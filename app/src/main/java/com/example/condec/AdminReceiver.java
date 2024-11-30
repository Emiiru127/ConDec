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

        // Show a custom message and launch the PasswordActivity
       /* Intent passwordIntent = new Intent(context, PasswordPromptActivity.class);
        passwordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, passwordIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return "Please provide the password to disable this admin.";
        } else {
            // For older versions, just return the message
            return "Please provide the password to disable this admin.";
        }*/


        //requirePassword(context);
        Toast.makeText(context, "You are disabling Condec's Admin Permission", Toast.LENGTH_SHORT).show();


        return "Disabling this admin will cause security issues.";

    }

    private void requirePassword(Context context){

        Intent intent = new Intent(context, PasswordPromptActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        //super.onDisabled(context, intent);
        Toast.makeText(context, "Device Admin disabled", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Toast.makeText(context, "Device Admin enabled", Toast.LENGTH_SHORT).show();
    }

}

