package com.example.condec;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class ReRequestAdminPermission extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(context, AdminReceiver.class);

        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            Intent activateDeviceAdmin = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            activateDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            activateDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This app requires device admin permissions to function correctly.");
            activateDeviceAdmin.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activateDeviceAdmin);
        }
    }
}
