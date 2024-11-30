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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CondecSleepService extends Service {

    private boolean isLockActivityRunning = false;
    private Handler handler = new Handler();
    private SharedPreferences sharedPreferences;
    private Set<String> allowedApps;
    private Map<String, Boolean> authenticatedApps = new HashMap<>();
    private String lastForegroundPackage = null;

    private BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.condec.UNLOCK_APP".equals(intent.getAction())) {
                String packageName = intent.getStringExtra("PACKAGE_NAME");
                if (packageName != null) {
                    authenticatedApps.put(packageName, true);
                    isLockActivityRunning = false;
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_SERVICE".equals(action)) {
                // Handle service start
                startForegroundService();
                handler.postDelayed(runnable, 500);
                return START_STICKY;
            } else if ("STOP_SERVICE".equals(action)) {
                // Handle service stop
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        // Service logic here

        startForegroundService();
        handler.postDelayed(runnable, 500);

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecSleepServiceStatus", true);
        editor.apply();

        allowedApps = new HashSet<>();
        allowedApps.add("com.example.condec");  // Your app's package name
        allowedApps.add("com.android.settings"); // Allow settings

        // Allow the home screen (launcher)
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String defaultLauncher = resolveInfo.activityInfo.packageName;
        allowedApps.add(defaultLauncher);

        // Allow default dialer app
        Intent dialerIntent = new Intent(Intent.ACTION_DIAL);
        ResolveInfo dialerInfo = getPackageManager().resolveActivity(dialerIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (dialerInfo != null) {
            String dialerPackage = dialerInfo.activityInfo.packageName;
            allowedApps.add(dialerPackage);
        }

        // Allow default SMS app
        String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this);
        if (defaultSmsPackage != null) {
            allowedApps.add(defaultSmsPackage);
        }

        // Allow Contacts app
        Intent contactsIntent = new Intent(Intent.ACTION_VIEW);
        contactsIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        ResolveInfo contactsInfo = getPackageManager().resolveActivity(contactsIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (contactsInfo != null) {
            String contactsPackage = contactsInfo.activityInfo.packageName;
            allowedApps.add(contactsPackage);
        }

        IntentFilter filter = new IntentFilter("com.example.condec.UNLOCK_APP");
        registerReceiver(unlockReceiver, filter);

    }

    private void startForegroundService() {
        String channelId = "sleep_mode_service_channel";
        String channelName = "Sleep Mode Service";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("ConDec is Running")
                .setContentText("ConDec is running on background")
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

                // Reset authentication for the previous foreground app if switching
                if (lastForegroundPackage != null && !lastForegroundPackage.equals(currentPackageName)) {
                    authenticatedApps.remove(lastForegroundPackage);
                    isLockActivityRunning = false;  // Ensure lock screen can show again
                }

                lastForegroundPackage = currentPackageName; // Update last foreground package

                // If the current app is not allowed and not authenticated, show the lock screen
                if (!allowedApps.contains(currentPackageName)) {
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
        unregisterReceiver(unlockReceiver);
        handler.removeCallbacks(runnable);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecSleepServiceStatus", false);
        editor.apply();

        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
