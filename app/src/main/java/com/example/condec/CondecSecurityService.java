package com.example.condec;

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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CondecSecurityService extends Service {

    private boolean isLockActivityRunning = false;
    private Handler handler = new Handler();
    private String settingsPackage = "com.android.settings";
    private String lastForegroundPackage = null;
    private Map<String, Boolean> authenticatedApps = new HashMap<>();

    private BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.condec.UNLOCK_APP".equals(intent.getAction())) {
                String packageName = intent.getStringExtra("PACKAGE_NAME");
                if (packageName != null && packageName.equals(settingsPackage)) {
                    authenticatedApps.put(packageName, true); // Mark as authenticated
                    isLockActivityRunning = false;
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecSecurityServiceStatus", true);
        editor.apply();

        IntentFilter filter = new IntentFilter("com.example.condec.UNLOCK_APP");
        registerReceiver(unlockReceiver, filter);
        Log.d("Condec Security", "Condec Security Running");
        startForegroundService();
        handler.postDelayed(runnable, 500);
    }

    private void startForegroundService() {
        String channelId = "security_service_channel";
        String channelName = "Condec Security Service";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Condec Security Service")
                .setContentText("Preventing Uninstallation")
                .setSmallIcon(R.mipmap.ic_launcher_round)
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

                // Reset authentication if the app has changed
                if (lastForegroundPackage != null && !lastForegroundPackage.equals(currentPackageName)) {
                    authenticatedApps.remove(lastForegroundPackage);
                    isLockActivityRunning = false;
                }

                lastForegroundPackage = currentPackageName; // Update last foreground package

                // Check if the current app needs to be locked
                if (settingsPackage.equals(currentPackageName)) {
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecSecurityServiceStatus", false);
        editor.apply();

        unregisterReceiver(unlockReceiver);
        handler.removeCallbacks(runnable);
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
