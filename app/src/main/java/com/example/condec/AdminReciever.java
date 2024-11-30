package com.example.condec;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;

public class AdminReciever extends DeviceAdminReceiver {


    public AdminReciever(MainActivity mainActivity){
        // Request device admin permission
/*
        Intent REQUEST_ENABLE_DEVICE_ADMIN = mainActivity;
        ComponentName componentName = new ComponentName(mainActivity, DeviceAdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin to secure your app");
        startActivityForResult(intent, REQUEST_ENABLE_DEVICE_ADMIN);

*/
    }



}

