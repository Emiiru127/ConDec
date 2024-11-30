package com.example.condec;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("condecPref", MODE_PRIVATE);
            boolean isFeatureOn = prefs.getBoolean("DetectionFeatureEnabled", false);
            if (isFeatureOn) {
                Intent serviceIntent = new Intent(context, CondecDetectionService.class);
                context.startForegroundService(serviceIntent);
            }
        }
    }

}
