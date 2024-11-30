package com.example.condec;

import static androidx.core.app.ActivityCompat.startActivityForResult;

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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CondecSecurityService extends Service {

    private static final int REQUEST_CODE_VPN = 5471;
    private SharedPreferences condecPreferences;
    private boolean isLockActivityRunning = false;
    private Handler handlerAppCheck = new Handler(); // For app checking every 500ms
    private Handler handlerServiceCheck = new Handler(); // For service checking every 5000ms
    private String settingsPackage = "com.android.settings";

    private String packageInstaller = "com.android.packageinstaller";
    private String lastForegroundPackage = null;
    private Map<String, Boolean> authenticatedApps = new HashMap<>();

    private Boolean isDetectionServiceManualOff = true;

    public static boolean isRequestPermissionActivityRunning = false; // Track the state

    private boolean isDetectionServiceManuallyOff = true;
    private boolean isVPNServiceManuallyOff = true;

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
    private BroadcastReceiver updateFlagsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.condec.UPDATE_SECURITY_FLAGS_DETECTION_ON".equals(intent.getAction())) {
                Log.d("Condec Security", "RECEIVED REQUEST TO CHECK FLAGS");
                isDetectionServiceManuallyOff = false;
                stopRunnableServiceCheck();
                checkFlags();
                handlerServiceCheck.postDelayed(runnableServiceCheck, 5000);  // Restart service check
            }
            else if ("com.example.condec.UPDATE_SECURITY_FLAGS_DETECTION_OFF".equals(intent.getAction())) {
                Log.d("Condec Security", "RECEIVED REQUEST TO CHECK FLAGS");
                isDetectionServiceManuallyOff = true;
                stopRunnableServiceCheck();
                checkFlags();
                handlerServiceCheck.postDelayed(runnableServiceCheck, 5000);  // Restart service check
            }
            else if ("com.example.condec.UPDATE_SECURITY_FLAGS_VPN_ON".equals(intent.getAction())) {
                Log.d("Condec Security", "RECEIVED REQUEST TO CHECK FLAGS");
                isVPNServiceManuallyOff = false;
                stopRunnableServiceCheck();
                checkFlags();
                handlerServiceCheck.postDelayed(runnableServiceCheck, 5000);  // Restart service check
            }
            else if ("com.example.condec.UPDATE_SECURITY_FLAGS_VPN_OFF".equals(intent.getAction())) {
                Log.d("Condec Security", "RECEIVED REQUEST TO CHECK FLAGS");
                isVPNServiceManuallyOff = true;
                stopRunnableServiceCheck();
                checkFlags();
                handlerServiceCheck.postDelayed(runnableServiceCheck, 5000);  // Restart service check
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        this.condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =   this.condecPreferences.edit();
        editor.putBoolean("condecSecurityServiceStatus", true);
        editor.apply();

        IntentFilter filter2 = new IntentFilter("com.example.condec.UPDATE_SECURITY_FLAGS_DETECTION_ON");
        registerReceiver(updateFlagsReceiver, filter2);
        IntentFilter filter3 = new IntentFilter("com.example.condec.UPDATE_SECURITY_FLAGS_DETECTION_OFF");
        registerReceiver(updateFlagsReceiver, filter3);
        IntentFilter filter4 = new IntentFilter("com.example.condec.UPDATE_SECURITY_FLAGS_VPN_ON");
        registerReceiver(updateFlagsReceiver, filter4);
        IntentFilter filter5 = new IntentFilter("com.example.condec.UPDATE_SECURITY_FLAGS_VPN_OFF");
        registerReceiver(updateFlagsReceiver, filter5);

        IntentFilter filter = new IntentFilter("com.example.condec.UNLOCK_APP");
        registerReceiver(unlockReceiver, filter);
        Log.d("Condec Security", "Condec Security Running");
        startForegroundService();

        // Start both Runnables
        handlerAppCheck.postDelayed(runnableAppCheck, 500); // 500ms app check
        handlerServiceCheck.postDelayed(runnableServiceCheck, 5000); // 5000ms service check

        this.isDetectionServiceManualOff = this.condecPreferences.getBoolean("isDetectionServiceManualOff", true);
        this.isVPNServiceManuallyOff = this.condecPreferences.getBoolean("isVPNServiceManuallyOff", true);

        checkFlags();
    }

    private void stopRunnableServiceCheck() {
        handlerServiceCheck.removeCallbacks(runnableServiceCheck); // Remove any callbacks to stop the thread
    }
    private void checkFlags(){

        Log.d("Condec Security", "CHECKING FLAGS:");
        this.condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        //this.isDetectionServiceManuallyOff = this.condecPreferences.getBoolean("isDetectionServiceManuallyOff", true);
        Log.d("Condec Security", "CHECKING DETECTION FLAGS: " + this.isDetectionServiceManuallyOff);
        Log.d("Condec Security", "CHECKING VPN FLAGS: " + this.isVPNServiceManuallyOff);


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

    // Runnable for checking running apps every 500ms
    private Runnable runnableAppCheck = new Runnable() {
        @Override
        public void run() {
            checkRunningApps();
            handlerAppCheck.postDelayed(this, 500); // Re-run every 500ms
        }
    };

    // Runnable for checking services every 5000ms
    private Runnable runnableServiceCheck = new Runnable() {
        @Override
        public void run() {

            checkOtherServices();
            handlerServiceCheck.postDelayed(this, 5000); // Re-run every 5000ms
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
                if (settingsPackage.equals(currentPackageName) || packageInstaller.equals(currentPackageName)) {
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

    private void checkOtherServices() {

        Log.d("Condec Security", "CHECKING SERVICES:");
        // Check Service 1


        Log.d("Condec Security", "CHECKING DETECTION SERVICE:");
        Log.d("Condec Security", "Manual: " + isDetectionServiceManuallyOff);
        Log.d("Condec Security", "Service Status: " + isServiceRunning(CondecDetectionService.class));
        Log.d("Condec Security", "JUDGEMENT: " + (isServiceRunning(CondecDetectionService.class) == false && isDetectionServiceManuallyOff == false));
        if (isServiceRunning(CondecDetectionService.class) == false && isDetectionServiceManuallyOff == false) {
            if (!isRequestPermissionActivityRunning) { // Only start if not running
                isRequestPermissionActivityRunning = true;
                Log.d("Condec Security", "DETECTION SERVICE WAS NOT MANUALLY OFF, RESTARTING DETECTION SERVICE");
                Intent requestIntent = new Intent(this, RequestDetectionPermission.class);
                requestIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(requestIntent);
                Log.d("Condec Security", "STARTING DETECTION SERVICE:");
            }
        }

        Log.d("Condec Security", "CHECKING VPN SERVICE:");
        Log.d("Condec Security", "Manual: " + isVPNServiceManuallyOff);
        Log.d("Condec Security", "Service Status: " + isServiceRunning(CondecVPNService.class));
        Log.d("Condec Security", "JUDGEMENT: " + (isServiceRunning(CondecVPNService.class) == false && isVPNServiceManuallyOff == false));
        if (isServiceRunning(CondecVPNService.class) == false && isVPNServiceManuallyOff == false) {

            Log.d("Condec Security", "VPN SERVICE WAS NOT MANUALLY OFF, RESTARTING VPN SERVICE");
            Intent intent = new Intent(this, CondecVPNService.class);
            startService(intent);
            Log.d("Condec Security", "STARTING VPN SERVICE:");

        }

    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecSecurityServiceStatus", false);
        editor.apply();

        unregisterReceiver(unlockReceiver);
        handlerAppCheck.removeCallbacks(runnableAppCheck);
        handlerServiceCheck.removeCallbacks(runnableServiceCheck);
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
