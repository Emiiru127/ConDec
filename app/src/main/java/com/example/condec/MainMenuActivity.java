package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

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
    private Surface surface;
    boolean isBinded = false;
    boolean hasAllowedScreenCapture = false;

    //UI

    private FrameLayout frameFeatures;
    private MaterialButton btnFeatureWarningDetection;
    private MaterialButton btnFeatureAppBlocking;
    private MaterialButton btnFeatureWebsiteBlocking;
    private MaterialButton btnFeatureAppUsage;

    private MaterialButton[] btnFeatures;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CondecService.LocalBinder binder = (CondecService.LocalBinder) service;
            condecService = binder.getService();

            condecService.setSurface(surface);
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
        setContentView(R.layout.layout_main_menu);

        this.condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        this.imgViewServiceStatus = findViewById(R.id.imgViewStatus);
        this.btnServiceStatus = findViewById(R.id.btnSystemStatus);
        this.txtServiceStatus = findViewById(R.id.txtSystemStatus);

        this.imgViewServiceStatus.setOnClickListener(this);
        this.btnServiceStatus.setOnClickListener(this);

        this.isServiceActive = false;//this.condecPreferences.getBoolean("isSystemActive", false);

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

        this.btnFeatureWarningDetection.setOnClickListener(this);
        this.btnFeatureAppBlocking.setOnClickListener(this);
        this.btnFeatureWebsiteBlocking.setOnClickListener(this);
        this.btnFeatureAppUsage.setOnClickListener(this);

        this.surfaceView = findViewById(R.id.screenView);
        this.surfaceView.setZOrderOnTop(true);
        this.surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surface = holder.getSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surface = null;
            }
        });

        update();
        updateFrameFeatures("Warning Detection");

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (condecService != null){

            //this.condecService.setSurface(this.surface);

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

    private void toggleService(){

        if (this.isServiceActive == false){

            requestCapturePermission();

        }
        else if(this.isServiceActive == true){

            stopCondecService();
            this.isServiceActive = false;
            clearSurface();

        }
/*
        SharedPreferences.Editor editor = this.condecPreferences.edit();
        editor.putBoolean("isSystemActive", this.isSystemActive);
        editor.apply();*/

    }

    private void clearSurface() {

        SurfaceHolder surfaceHolder = this.surfaceView.getHolder();
        Surface surface = this.surfaceView.getHolder().getSurface();

        if (surface != null && surface.isValid()) {
            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK);
                }
            }
            catch (IllegalArgumentException exception){

                System.out.println("Ilegal Argument Error");

            }
            finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void requestCapturePermission(){

        //hasAllowedScreenCapture = this.condecPreferences.getBoolean("hasAllowedScreenCapture", false);
        //hasAllowedScreenCapture = false;
        if (hasAllowedScreenCapture == false){

            System.out.println("REQUESTING MEDIA PROJECTION PERMISSION");
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(permissionIntent, REQUEST_CAPTURE_CODE);


        }

    }

    private void startService(int screenCaptureResultCode, Intent screenCaptureIntent)  {

        //boolean hasAllowedScreenCapture = this.condecPreferences.getBoolean("hasAllowedScreenCapture", false);

        if (hasAllowedScreenCapture == true){

            this.isServiceActive = true;
            update();

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

    private void stopCondecService()  {

        if (isBinded == true){

            unbindService(this.serviceConnection);
            this.hasAllowedScreenCapture = false;
            this.isServiceActive = false;
            this.isBinded = false;

            Intent serviceIntent = new Intent(this, CondecService.class);
            stopService(serviceIntent);

        }

    }

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

    @Override
    public void onClick(View view) {

        if (this.btnServiceStatus == view || this.imgViewServiceStatus == view){

            toggleService();
            update();

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