package com.example.condec;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class AppInstallationReceiver extends BroadcastReceiver {

    private boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
            // Get the package name of the newly installed app
            Uri data = intent.getData();
            if (data != null) {
                String packageName = data.getSchemeSpecificPart();

                SharedPreferences sharedPreferences = context.getSharedPreferences("condecPref", Context.MODE_PRIVATE);
                Set<String> previouslySelectedApps = sharedPreferences.getStringSet("blockedApps", new HashSet<>());
                boolean isInitializationDone = sharedPreferences.getBoolean("isInitializationDone", false);

                // On first launch, select all apps by default and save them as blocked apps
                previouslySelectedApps.add(packageName);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putStringSet("blockedApps", previouslySelectedApps);
                editor.putBoolean("isInitializationDone", true); // Mark initialization as done
                editor.apply();

                if (isServiceRunning(CondecBlockingService.class, context)){

                    context.stopService(new Intent(context, CondecBlockingService.class));
                    context.startForegroundService(new Intent(context, CondecBlockingService.class));

                }

                Log.d("AppInstallationReceiver", "New app installed: " + packageName);
                // Handle the package name (e.g., save or display it)
            }
        }
    }
}
