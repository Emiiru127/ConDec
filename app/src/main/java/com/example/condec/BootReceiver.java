package com.example.condec;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("condecPref", MODE_PRIVATE);

            Log.d("Condec Boot Receiver", "Starting Services:");

            boolean securityServiceStatus = prefs.getBoolean("condecSecurityServiceStatus", false);
            Log.d("Condec Boot Receiver", "Checking SecurityServiceStatus: " + securityServiceStatus);

            if (securityServiceStatus) {
                Log.d("Condec Boot Receiver", "Starting Service: Security Service");
                Intent serviceIntent = new Intent(context, CondecSecurityService.class);
                context.startForegroundService(serviceIntent);
            }

            boolean parentalServiceStatus = prefs.getBoolean("condecParentalServiceStatus", false);
            Log.d("Condec Boot Receiver", "Checking ParentalServiceStatus: " + parentalServiceStatus);

            if (parentalServiceStatus) {
                Log.d("Condec Boot Receiver", "Starting Service: Parental Service");
                Intent serviceIntent = new Intent(context, CondecParentalService.class);
                context.startForegroundService(serviceIntent);
            }

            boolean sleepServiceStatus = prefs.getBoolean("condecSleepServiceStatus", false);
            Log.d("Condec Boot Receiver", "Checking SleepServiceStatus: " + sleepServiceStatus);


            if (sleepServiceStatus) {
                Log.d("Condec Boot Receiver", "Starting Service: Sleep Service");
                Intent serviceIntent = new Intent(context, CondecSleepService.class);
                context.startForegroundService(serviceIntent);
            }

            // Trigger the SleepTimeReceiver to reschedule alarms at boot
            Log.d("Condec Boot Receiver", "Triggering SleepTimeReceiver to reschedule alarms.");
            Intent sleepReceiverIntent = new Intent(context, SleepTimeReceiver.class);
            context.sendBroadcast(sleepReceiverIntent);

            boolean detectionServiceStatus = prefs.getBoolean("condecDetectionServiceStatus", false);
            Log.d("Condec Boot Receiver", "Checking DetectionServiceStatus: " + detectionServiceStatus);

            if (detectionServiceStatus) {
                Log.d("Condec Boot Receiver", "Starting Service: Detection Service");
                Intent requestIntent = new Intent(context, RequestDetectionPermission.class);
                requestIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(requestIntent);
            }

            boolean blockingServiceStatus = prefs.getBoolean("condecBlockingServiceStatus", false);
            Log.d("Condec Boot Receiver", "Checking BlockingServiceStatus: " + blockingServiceStatus);

            if (blockingServiceStatus) {
                Log.d("Condec Boot Receiver", "Starting Service: Blocking Service");
                Intent serviceIntent = new Intent(context, CondecBlockingService.class);
                context.startForegroundService(serviceIntent);
            }

            boolean vpnServiceStatus = prefs.getBoolean("condecVPNServiceStatus", false);
            Log.d("Condec Boot Receiver", "Checking VPNServiceStatus: " + vpnServiceStatus);

            if (vpnServiceStatus) {
                Log.d("Condec Boot Receiver", "Starting Service: VPN Service");
                Intent serviceIntent = new Intent(context, CondecVPNService.class);
                context.startForegroundService(serviceIntent);
            }

        }
    }

}
