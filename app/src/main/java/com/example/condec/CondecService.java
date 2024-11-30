package com.example.condec;

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class CondecService extends Service {

    public final IBinder binder = new LocalBinder();
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;

    private ImageReader imageReader;

    private SurfaceView surfaceView;

    private int resultCode;
    private Intent data;


    public class LocalBinder extends Binder {
        CondecService getService() {
            // Return this instance of MyBoundService so clients can call public methods
            return CondecService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    }

    public static Intent newIntent(Context context, int resultCode, Intent data){

        Intent intent = new Intent(context, CondecService.class);
        intent.putExtra("CAPTURE_CODE", resultCode);
        intent.putExtra("CAPTURE_DATA", data);
        return intent;

    }

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel("1", "Test", NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(this, "1")
                .setContentTitle("MEDIA PROJECTOR IS IN FORCE")
                .setContentText("Media Projector is running")
                .setSmallIcon(R.mipmap.ic_launcher_round);

        return builder.build();
    }

    public void startProjection(int resultCode, Intent data) {

        System.out.println("resultCode: " + resultCode);
        System.out.println("data: " + data);
        System.out.println("mediaProjectionManager: " + (mediaProjectionManager != null));
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        System.out.println("MediaProjector: " + mediaProjection);
        System.out.println("MediaProjector Initialized: " + (mediaProjection != null));
        if (mediaProjection != null) {
            createVirtualDisplay();
            Toast.makeText(CondecService.this, "Media Projection Initialized", Toast.LENGTH_SHORT).show();
            System.out.println("MediaProjection Initialized: " + (this.mediaProjection != null));
        }
    }

    private void createVirtualDisplay() {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;

        System.out.println("METRICS:");
        System.out.println("Width: " + screenWidth);
        System.out.println("Height: " + screenHeight);
        System.out.println("screenDensity: " + screenDensity);

        this.imageReader  = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);

        virtualDisplay = mediaProjection.createVirtualDisplay("MediaProjector",
                screenWidth, screenHeight, screenDensity, VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                this.imageReader.getSurface(), null, null);
        if (virtualDisplay != null) {
            Log.i("VirtualDisplay", "Virtual Display Created");
            System.out.println("Virtual Display Created: " + (this.virtualDisplay != null));

        }

        /*imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    Bitmap bitmap = imageToBitmap(image);
                    if (bitmap != null) {


                            System.out.println("YAHALLO CAPTURING!!!");

                    }
                    image.close();
                }
            }
        }, null);*/
    }

    public void setSurfaceView(SurfaceView surfaceView){
        System.out.println("Adding Surface");
        this.surfaceView = surfaceView;
        this.virtualDisplay.setSurface(this.surfaceView.getHolder().getSurface());
        System.out.println("Added Surface");
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification notification = createNotification();
        startForeground(1, notification);

        this.resultCode = intent.getIntExtra("CAPTURE_CODE", 0);
        this.data = intent.getParcelableExtra("CAPTURE_DATA");


        System.out.println("MediaProjection Initialized: " + (this.mediaProjection != null));
        startProjection(this.resultCode, this.data);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

        return binder;

    }

}