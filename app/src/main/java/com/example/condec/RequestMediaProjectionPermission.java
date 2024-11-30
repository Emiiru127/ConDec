package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

public class RequestMediaProjectionPermission extends AppCompatActivity {

    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                // Permission granted, start capturing the screen


                boolean hasAllowedCapture = true;

                SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = condecPreferences.edit();
                editor.putBoolean("hasAllowedScreenCapture", hasAllowedCapture);
                editor.apply();

                Intent intent = new Intent(RequestMediaProjectionPermission.this, MainActivity.class);
                startActivity(intent);
                finish();

            } else {

                // Permission denied

                Intent intent = new Intent(RequestMediaProjectionPermission.this, MainActivity.class);
                startActivity(intent);
                finish();

            }
        }
    }

}