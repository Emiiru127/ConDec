package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainMenuActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int REQUEST_CAPTURE_CODE = 1;
    private MediaProjectionManager mediaProjectionManager;
    private CondecService condecService;


    private SharedPreferences condecPreferences;
    private ImageView imgViewServiceStatus;
    private Button btnServiceStatus;
    private TextView txtServiceStatus;

    private boolean isServiceActive = false;

    private SurfaceView surfaceView;
    boolean isBinded = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CondecService.LocalBinder binder = (CondecService.LocalBinder) service;
            condecService = binder.getService();

            condecService.setSurfaceView(surfaceView);

            isBinded = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            isBinded = false;

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        this.condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        this.imgViewServiceStatus = findViewById(R.id.imgViewStatus);
        this.btnServiceStatus = findViewById(R.id.btnSystemStatus);
        this.txtServiceStatus = findViewById(R.id.txtSystemStatus);

        this.imgViewServiceStatus.setOnClickListener(this);
        this.btnServiceStatus.setOnClickListener(this);

        this.isServiceActive = false;//this.condecPreferences.getBoolean("isSystemActive", false);

        this.surfaceView = findViewById(R.id.screenView);
        this.surfaceView.setZOrderOnTop(true);

        update();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (condecService != null){

            this.condecService.setSurfaceView(this.surfaceView);

        }

    }

    private void update(){

        if (this.isServiceActive){

            this.imgViewServiceStatus.setImageResource(R.drawable.power_on_button_icon);
            this.btnServiceStatus.setText("On");
            this.btnServiceStatus.setBackgroundColor(getColor(R.color.green));
            this.txtServiceStatus.setText("Click to Off");

        }
        else {

            this.imgViewServiceStatus.setImageResource(R.drawable.power_off_button_icon);
            this.btnServiceStatus.setText("Off");
            this.btnServiceStatus.setBackgroundColor(getColor(R.color.red));
            this.txtServiceStatus.setText("Click to On");

        }

    }

    private void toggleService(){

        this.isServiceActive = !this.isServiceActive;
/*
        if (this.isSystemActive == true){



        }

        SharedPreferences.Editor editor = this.condecPreferences.edit();
        editor.putBoolean("isSystemActive", this.isSystemActive);
        editor.apply();*/

    }

    private void requestCapturePermission(){

        boolean hasAllowedScreenCapture = this.condecPreferences.getBoolean("hasAllowedScreenCapture", false);
        hasAllowedScreenCapture = false;
        if (hasAllowedScreenCapture == false){

            System.out.println("REQUESTING MEDIA PROJECTION PERMISSION");
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(permissionIntent, REQUEST_CAPTURE_CODE);


        }

    }

    private void startService(int screenCaptureResultCode, Intent screenCaptureIntent)  {

        boolean hasAllowedScreenCapture = this.condecPreferences.getBoolean("hasAllowedScreenCapture", false);

        hasAllowedScreenCapture = false;
        if (hasAllowedScreenCapture == false){

           // int screenCaptureResultCode = this.condecPreferences.getInt("screenCaptureResultCode", 0);
            //String serializedScreenCaptureIntent = this.condecPreferences.getString("savedScreenCaptureIntent", null);

            Intent serviceIntent = CondecService.newIntent(this, screenCaptureResultCode, screenCaptureIntent);
            startForegroundService(serviceIntent);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        }
        else {

            System.out.println("NO PERMISSION");

        }

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_CODE) {
            if (resultCode == RESULT_OK) {
                // Get the MediaProjection

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
            }
        }
    }

    @Override
    public void onClick(View view) {

        if (this.btnServiceStatus == view || this.imgViewServiceStatus == view){

            this.isServiceActive = !this.isServiceActive;
            //startMainSystem();
            requestCapturePermission();
            update();

        }

    }

}