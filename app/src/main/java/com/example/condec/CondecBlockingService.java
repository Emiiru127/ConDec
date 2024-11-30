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
    private Map<String, Boolean> authenticatedApps = new HashMap<>(); // Add this map to track authenticated apps

    private BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resetLockState();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        blockedApps = sharedPreferences.getStringSet("blockedApps", new HashSet<>());

        IntentFilter filter = new IntentFilter("com.example.condec.UNLOCK_APP");
        registerReceiver(unlockReceiver, filter);

        // Start the service as a foreground service
        startForegroundService();

        handler.postDelayed(runnable, 1000);
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
                .setSmallIcon(R.mipmap.ic_launcher) // Use your own app icon here
                .build();

        startForeground(1, notification);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            checkRunningApps();
            handler.postDelayed(this, 1000);
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
                System.out.println("CHECKING RUNNING APPS: " + currentPackageName);

                // Avoid locking the app itself
                if (currentPackageName.equals("com.example.condec")) {
                    System.out.println("Skipping lock for self.");
                    return;
                }

                // Check if the detected app is in the blocked list
                if (blockedApps.contains(currentPackageName)) {
                    if (!isLockActivityRunning) {
                        // Check if the app is already authenticated
                        Boolean isAuthenticated = authenticatedApps.get(currentPackageName);
                        if (isAuthenticated == null || !isAuthenticated) {
                            isLockActivityRunning = true;
                            Intent lockIntent = new Intent(this, PasswordPromptActivity.class);
                            lockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            lockIntent.putExtra("packageName", currentPackageName); // Pass the package name to the prompt activity
                            startActivity(lockIntent);
                        }
                    }
                } else {
                    isLockActivityRunning = false;
                }
            }
        }
    }

    public void onPasswordCorrect(String packageName) {
        authenticatedApps.put(packageName, true); // Mark the app as authenticated
        resetLockState(); // Reset the lock state to allow the app to open without showing the prompt
    }

    public void resetLockState() {
        isLockActivityRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister the broadcast receiver
        unregisterReceiver(unlockReceiver);

        // Stop the handler and its runnable
        handler.removeCallbacks(runnable);

        // Stop the foreground service
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
