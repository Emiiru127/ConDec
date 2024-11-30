package com.example.condec;

import static android.app.Activity.RESULT_OK;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;

public class MediaProjector extends Service {

    public final IBinder binder = new LocalBinder();
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private Surface surface;

    private int resultCode;
    private Intent data;

    public class LocalBinder extends Binder {
        MediaProjector getService() {
            // Return this instance of MyBoundService so clients can call public methods
            return MediaProjector.this;
        }
    }

    @Override
    public void onCreate() {

        this.mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    }

    public static Intent newIntent(Context context, int resultCode, Intent data){

        Intent intent = new Intent(context, MediaProjector.class);
        intent.putExtra("CAPTURE_CODE", resultCode);
        intent.putExtra("CAPTURE_DATA", data);
        return intent;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification notification = createNotification();
        startForeground(1, notification);

        this.resultCode = intent.getIntExtra("CAPTURE_CODE", 0);
        this.data = intent.getParcelableExtra("CAPTURE_DATA");


        return super.onStartCommand(intent, flags, startId);
    }

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel("1", "ConDec", NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(this, "1")
                .setContentTitle("MEDIA PROJECTOR IS IN FORCE")
                .setContentText("Media Projector is running")
                .setSmallIcon(R.drawable.ic_launcher_foreground);

        return builder.build();
    }
/*
    public void startProjection(int resultCode, Intent data){

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;


    /*    System.out.println("YAHALLO0");
        System.out.println("Before:" + this.surface);


           System.out.println("YAHALLO1");*/
           //this.mediaProjection = this.mediaProjectionManager.getMediaProjection(resultCode, data);
           /*
           System.out.println("YAHALLO2");
           System.out.println("Test Projector1:" + this.surface);
           this. virtualDisplay = mediaProjection.createVirtualDisplay("MediaProjector",
                   screenWidth, screenHeight, screenDensity, VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                   this.surface, null, null);
           System.out.println("Test Projector2:" + this.surface);


           System.out.println("YAHALLO3");

           System.out.println("STARTED PROJECTING");

        System.out.println("MediaProjection Initialized: " + (this.mediaProjection != null));
        System.out.println("Virtual Display Created: " + (this.virtualDisplay != null));
        System.out.println("Surface Ready: " + (this.surface != null && this.surface.isValid()));
        */
/*
        if (this.surface == null){

            System.out.println("SURFACE NULL");

        }


    }
*/
    public void startProjection(int resultCode, Intent data) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection != null && surface != null && surface.isValid()) {
           // createVirtualDisplay();
            System.out.println("MediaProjection Initialized: " + (this.mediaProjection != null));
    }
}

    private void createVirtualDisplay() {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;

        virtualDisplay = mediaProjection.createVirtualDisplay("MediaProjector",
                screenWidth, screenHeight, screenDensity, VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                surface, null, null);
        if (virtualDisplay != null) {
            Log.i("VirtualDisplay", "Virtual Display Created");
            System.out.println("Virtual Display Created: " + (this.virtualDisplay != null));
            System.out.println("Surface Ready: " + (this.surface != null && this.surface.isValid()));
        }
    }

    public void setSurface(Surface surface){

        this.surface = surface;
        System.out.println("YAHALLO SURFACE");
        System.out.println("Surface Ready: " + (this.surface != null && this.surface.isValid()));

        if (this.resultCode == RESULT_OK && (this.surface != null && this.surface.isValid())) {
            startProjection(this.resultCode, this.data);
        }
    }

    public MediaProjection getMediaProjection(){

        return this.mediaProjection;

    }

    public VirtualDisplay getVirtualDisplay(){

        return this.virtualDisplay;

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

        return binder;
    }
}