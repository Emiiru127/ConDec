package com.example.condec;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AdminReceiver extends DeviceAdminReceiver {

    @Nullable
    @Override
    public CharSequence onDisableRequested(@NonNull Context context, @NonNull Intent intent) {

        Toast.makeText(context, "You are disabling Condec's Admin Permission", Toast.LENGTH_SHORT).show();
        return "Disabling this admin will cause security issues.";

    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Toast.makeText(context, "Device Admin disabled", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Toast.makeText(context, "Device Admin enabled", Toast.LENGTH_SHORT).show();
    }

}

