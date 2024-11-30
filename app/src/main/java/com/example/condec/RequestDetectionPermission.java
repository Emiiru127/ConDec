package com.example.condec;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class RequestDetectionPermission extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            SharedPreferences prefs = getSharedPreferences("condecPref", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            if (resultCode == RESULT_OK && data != null) {
                editor.putBoolean("permission_granted", true);
                editor.putInt("CAPTURE_CODE", resultCode);
                editor.putString("CAPTURE_DATA", data.toUri(Intent.URI_INTENT_SCHEME));
                editor.apply();
            } else {
                editor.putBoolean("permission_granted", false);
                editor.apply();
            }
            finish();
        }
    }
}
