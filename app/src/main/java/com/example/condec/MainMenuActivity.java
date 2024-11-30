package com.example.condec;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

public class MainMenuActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int REQUEST_CODE_DRAW_OVERLAY = 5469;
    private static final int REQUEST_CODE_USAGE_ACCESS = 5470;

    private SharedPreferences condecPreferences;

    //Device admin
    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;

    //UI

    private ImageButton btnSettings;

    private FrameLayout frameFeatures;
    private MaterialButton btnFeatureWarningDetection;
    private MaterialButton btnFeatureAppBlocking;
    private MaterialButton btnFeatureWebsiteBlocking;
    private MaterialButton btnFeatureAppUsage;

    private MaterialButton[] btnFeatures;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main_menu);

        this.condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        initializeUI();

        updateFrameFeatures("Warning Detection");

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminName = new ComponentName(this, AdminReceiver.class);

        if (!mDPM.isAdminActive(mAdminName)) {

            Intent intent = new Intent(MainMenuActivity.this, RequestAdminPermission.class);
            intent.putExtra("hasLoaded", getIntent().getBooleanExtra("hasLoaded", false));
            startActivity(intent);
            finish();

        }

       /* AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.noteOpNoThrow(AppOpsManager.OPSTR_WRITE_SETTINGS, .(), context.getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            // Permission is granted
        } else {
            // Permission is denied
            throw new SecurityException("App does not have the required permissions");
        }*/

        //checkAndRequestPermissions();
        //checkPermissions();
    }

   /* private void requestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY);
        }

        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_USAGE_ACCESS);
        }
    }*/

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkOverlayPermission();
            checkUsageAccessPermission();
        }
    }

    private void checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY);
        }
    }

    private void checkUsageAccessPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_USAGE_ACCESS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DRAW_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                Log.d("OverlayPermission", "Permission granted");
            } else {
                Log.e("OverlayPermission", "Permission denied");
            }
        } else if (requestCode == REQUEST_CODE_USAGE_ACCESS) {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
            if (mode == AppOpsManager.MODE_ALLOWED) {
                Log.d("UsageAccessPermission", "Permission granted");
            } else {
                Log.e("UsageAccessPermission", "Permission denied");
            }
        }
    }

    private void initializeUI(){

        this.btnSettings = findViewById(R.id.btnSettings);

        this.frameFeatures = findViewById(R.id.frameFeatures);

        this.btnFeatureWarningDetection = findViewById(R.id.btnWarningDetection);
        this.btnFeatureAppBlocking = findViewById(R.id.btnAppBlocking);
        this.btnFeatureWebsiteBlocking = findViewById(R.id.btnWebsiteBlocking);
        this.btnFeatureAppUsage = findViewById(R.id.btnAppUsage);

        this.btnFeatures = new MaterialButton[4];
        this.btnFeatures[0] = this.btnFeatureWarningDetection;
        this.btnFeatures[1] = this.btnFeatureAppBlocking;
        this.btnFeatures[2] = this.btnFeatureWebsiteBlocking;
        this.btnFeatures[3] = this.btnFeatureAppUsage;

        this.btnSettings.setOnClickListener(this);

        this.btnFeatureWarningDetection.setOnClickListener(this);
        this.btnFeatureAppBlocking.setOnClickListener(this);
        this.btnFeatureWebsiteBlocking.setOnClickListener(this);
        this.btnFeatureAppUsage.setOnClickListener(this);


    }

    private void updateFrameFeatures(String selectedFeature){

        int selected = -1;

        switch (selectedFeature){

            case ("Warning Detection"):
                selectFeature(new WarningDetectionFragment());
                selected = 0;
                break;

            case ("App Blocking"):
                selectFeature(new AppBlockingFragment());
                selected = 1;
                break;

            case ("Website Blocking"):
                selectFeature(new WebsiteBlockingFragment());
                selected = 2;
                break;

            case ("App Usage"):
                selectFeature(new AppUsageFragment());
                selected = 3;
                break;

        }

        for (int i = 0; i < this.btnFeatures.length; i++){

            if (selected == i){

                this.btnFeatures[i].setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.icon_selected)));
                this.btnFeatures[i].setTextColor(getResources().getColor(R.color.icon_selected));

            }
            else {

                this.btnFeatures[i].setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.icon_not_selected)));
                this.btnFeatures[i].setTextColor(getResources().getColor(R.color.icon_not_selected));

            }

        }

    }

    private void selectFeature(Fragment fragment){

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameFeatures, fragment);
        fragmentTransaction.commit();

    }

/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DRAW_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                // Overlay permission granted
            } else {
                // Overlay permission denied
            }
        } else if (requestCode == REQUEST_CODE_USAGE_ACCESS) {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
            if (mode == AppOpsManager.MODE_ALLOWED) {
                // Usage access permission granted
            } else {
                // Usage access permission denied
            }
        }
    }*/

/*
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_CODE) {
            if (resultCode == RESULT_OK) {
                // Get the MediaProjection
                this.hasAllowedScreenCapture = true;
                boolean hasAllowedScreenCapture = false; // FORCED CODE
                int screenCaptureResultCode = resultCode;
                String serializedIntent = data.toUri(Intent.URI_INTENT_SCHEME);

                startService(resultCode, data);


                SharedPreferences.Editor editor = condecPreferences.edit();
                editor.putBoolean("hasAllowedScreenCapture", hasAllowedScreenCapture);
                editor.putInt("screenCaptureResultCode", screenCaptureResultCode);
                editor.putString("savedScreenCaptureIntent", serializedIntent);
                editor.apply();

                // Continue with using the mediaProjection object
            } else {
                // User denied permission
                this.hasAllowedScreenCapture = false;
            }
        }
    }
*/

    @Override
    public void onBackPressed() {

        if (false){

            super.onBackPressed();

        }
    }

    @Override
    public void onClick(View view) {

        if (this.btnSettings == view){

            Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();

        }
        if (this.btnFeatureWarningDetection == view){

            updateFrameFeatures("Warning Detection");
            return;
        }
        if (this.btnFeatureAppBlocking == view){

            updateFrameFeatures("App Blocking");
            return;

        }if (this.btnFeatureWebsiteBlocking == view){

            updateFrameFeatures("Website Blocking");
            return;
        }
        if (this.btnFeatureAppUsage == view){

            updateFrameFeatures("App Usage");
            return;

        }

    }

}