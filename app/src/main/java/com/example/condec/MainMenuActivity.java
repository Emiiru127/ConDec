package com.example.condec;

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainMenuActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;
    private MediaProjectionManager mediaProjectionManager;

    private SurfaceView surfaceView;
    private Surface surface;

    private MediaProjector mediaProjector;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    boolean mBound = false;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MediaProjector.LocalBinder binder = (MediaProjector.LocalBinder) service;
            mediaProjector = binder.getService();

            mediaProjector.setSurface(surface);


            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;
            int screenDensity = metrics.densityDpi;

            mediaProjection = mediaProjector.getMediaProjection();

            System.out.println("YAHALLO0");
            System.out.println("Before:" + surface);
            System.out.println("MediaProjection Initialized: " + (mediaProjection != null));


            virtualDisplay = mediaProjection.createVirtualDisplay("MediaProjector",
                    screenWidth, screenHeight, screenDensity, VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                    surface, null, null);
            System.out.println("Test Projector2:" + surface);


            System.out.println("YAHALLO3");

            System.out.println("STARTED PROJECTING");

            System.out.println("MediaProjection Initialized: " + (mediaProjection != null));
            System.out.println("Virtual Display Created: " + (virtualDisplay != null));
            System.out.println("Surface Ready: " + (surface != null && surface.isValid()));

            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        this.surfaceView = findViewById(R.id.vDisplay);

        this.surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            public void surfaceCreated(SurfaceHolder holder) {
                // The surface is ready to be used.
                surface = holder.getSurface();
            }


            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // Surface size or format has changed. Depending on your setup, you may need to handle this.
            }


            public void surfaceDestroyed(SurfaceHolder holder) {
                // Surface is no longer available.
                surface = null;
            }
        });

        this.surface = this.surfaceView.getHolder().getSurface();



/*
        System.out.println("TEST1");
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        System.out.println("TEST1.5");
        Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, REQUEST_CODE);
        System.out.println("TEST2");

*/

    }

    private void startProjection(int resultCode, Intent data){

        Intent serviceIntent = MediaProjector.newIntent(this, resultCode, data);

        //startForegroundService(serviceIntent);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Get the MediaProjection

                startProjection(resultCode, data);
                // Continue with using the mediaProjection object
            } else {
                // User denied permission
            }
        }
    }

}