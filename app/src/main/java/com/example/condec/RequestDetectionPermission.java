package com.example.condec;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.window.OnBackInvokedDispatcher;

public class RequestDetectionPermission extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdminReceiver = new ComponentName(this, AdminReceiver.class);

        if (dpm.isDeviceOwnerApp(getPackageName())) {
            dpm.setLockTaskPackages(deviceAdminReceiver, new String[]{getPackageName()});
            startLockTask();
        }

        // Request Media Projection permission
        requestMediaProjectionPermission();
    }

    private void requestMediaProjectionPermission() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                // Stop Lock Task Mode after permission is granted
                stopLockTask();

                // Start your service with the granted permission
                Intent serviceIntent = CondecDetectionService.newIntent(this, resultCode, data);
                startForegroundService(serviceIntent);
                Intent backIntent = new Intent(this, EnterPinActivity.class);
                startActivity(backIntent);
                Intent homeIntent = new Intent("com.example.ACTION_GO_HOME");
                sendBroadcast(homeIntent);
                CondecSecurityService.isRequestPermissionActivityRunning = false;
                Intent broadcastIntent = new Intent("com.example.condec.SECURITY_RESTART_SERVICE_CHECKER");
                sendBroadcast(broadcastIntent);
                finish(); // Close the activity
            } else {
                // User denied the permission, prompt again
                requestMediaProjectionPermission();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        CondecSecurityService.isRequestPermissionActivityRunning = false;
        Intent broadcastIntent = new Intent("com.example.condec.SECURITY_RESTART_SERVICE_CHECKER");
        sendBroadcast(broadcastIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CondecSecurityService.isRequestPermissionActivityRunning = false;
        Intent broadcastIntent = new Intent("com.example.condec.SECURITY_RESTART_SERVICE_CHECKER");
        sendBroadcast(broadcastIntent);

    }

    @Override
    public void onBackPressed() {

        if (false){

            super.onBackPressed();

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK ||
                keyCode == KeyEvent.KEYCODE_HOME ||
                keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            // Prevent default behavior
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}