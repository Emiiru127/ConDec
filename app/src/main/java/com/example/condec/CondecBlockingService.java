package com.example.condec;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CondecBlockingService extends Service {

    private boolean isLockActivityRunning = false;
    private Handler handler = new Handler();
    private SharedPreferences sharedPreferences;
    private Set<String> blockedApps;
    private Map<String, Long> appLastSeenTime = new HashMap<>();
    private Map<String, Boolean> authenticatedApps = new HashMap<>();
    private String lastAuthenticatedApp = null;
    private String lastForegroundPackage = null;

    private BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.condec.UNLOCK_APP".equals(intent.getAction())) {
                String packageName = intent.getStringExtra("PACKAGE_NAME");
                if (packageName != null) {
                    onPasswordCorrect(packageName);
                }
            } else if ("com.example.condec.RESET_LOCK_STATE".equals(intent.getAction())) {
                resetLockState();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        blockedApps = sharedPreferences.getStringSet("blockedApps", new HashSet<>());

        authenticatedApps.clear();

        IntentFilter filter = new IntentFilter("com.example.condec.UNLOCK_APP");
        filter.addAction("com.example.condec.RESET_LOCK_STATE");
        registerReceiver(unlockReceiver, filter);

        startForegroundService();
        handler.postDelayed(runnable, 500); // Delay to start checking
    }

    private void startForegroundService() {
        String channelId = "condec_app_lock_channel";
        String channelName = "Condec App Lock Service";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Condec App Lock")
                .setContentText("Monitoring blocked apps.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(1, notification);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            checkRunningApps();
            handler.postDelayed(this, 500);
        }
    };

    private void checkRunningApps() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);

        if (appList != null && !appList.isEmpty()) {
            UsageStats recentStats = null;
            for (UsageStats stats : appList) {
                if (recentStats == null || stats.getLastTimeUsed() > recentStats.getLastTimeUsed()) {
                    recentStats = stats;
                }
            }
            if (recentStats != null) {
                String currentPackageName = recentStats.getPackageName();

                if (currentPackageName.equals(getPackageName())) {
                    return; // Skip self
                }

                // Update the last seen time for the current app
                appLastSeenTime.put(currentPackageName, time);
                System.out.println("appLastSeenTime: " + appLastSeenTime);
                System.out.println("lastForegroundPackage: " + lastForegroundPackage);
                System.out.println("currentPackageName: " + currentPackageName);
                // Check if the previously foreground app has been exited
                if (lastForegroundPackage != null && !lastForegroundPackage.equals(currentPackageName)) {
                    if (blockedApps.contains(lastForegroundPackage)) {
                        // Reset authentication for the previous foreground app
                        System.out.println("Removed: " + lastForegroundPackage);
                        authenticatedApps.remove(lastForegroundPackage);
                    }
                }

                // Update last foreground package
                lastForegroundPackage = currentPackageName;

                if (blockedApps.contains(currentPackageName)) {
                    Boolean isAuthenticated = authenticatedApps.get(currentPackageName);

                    if (isAuthenticated == null || !isAuthenticated) {
                        if (!isLockActivityRunning) {
                            isLockActivityRunning = true;
                            Intent lockIntent = new Intent(this, PasswordPromptActivity.class);
                            lockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            lockIntent.putExtra("PACKAGE_NAME", currentPackageName);
                            startActivity(lockIntent);
                        }
                    }
                }

            }
        }
    }

    public void onPasswordCorrect(String packageName) {
        if (packageName != null && blockedApps.contains(packageName)) {
            authenticatedApps.put(packageName, true);
            lastAuthenticatedApp = packageName;
            resetLockState();
        } else {
            System.out.println("Package name is null or not blocked. Authentication failed.");
        }
    }

    public void resetLockState() {
        isLockActivityRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(unlockReceiver);
        handler.removeCallbacks(runnable);
        stopForeground(true);
        authenticatedApps.clear();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}