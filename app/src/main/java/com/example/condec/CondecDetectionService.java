package com.example.condec;

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
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
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.snackbar.Snackbar;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Timer;
import java.util.TimerTask;

public class CondecDetectionService extends Service {

    private int requestCodeCaptureScreen = 1;

    public final IBinder binder = new LocalBinder();
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;

    private Notification notification;

    private ImageReader imageReader;
    private Interpreter tflite;
    private Handler handler;
    private Timer timer;

    private int resultCode;
    private Intent data;
    private Image latestImage;


    public class LocalBinder extends Binder {
        CondecDetectionService getService() {
            // Return this instance of MyBoundService so clients can call public methods
            return CondecDetectionService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        try {
            tflite = new Interpreter(loadModelFile(this, "ai models/detect.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        HandlerThread handlerThread = new HandlerThread("InferenceThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public static Intent newIntent(Context context, int resultCode, Intent data) {

        Intent intent = new Intent(context, CondecDetectionService.class);
        intent.putExtra("CAPTURE_CODE", resultCode);
        intent.putExtra("CAPTURE_DATA", data);
        return intent;

    }

    private Notification createNotification(String message) {
        NotificationChannel channel = new NotificationChannel("1", "condec", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = null;

        if (message != null) {

            builder = new Notification.Builder(this, "1")
                    .setContentTitle("CONDEC Warning Detection")
                    .setContentText(message)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setPriority(Notification.PRIORITY_HIGH) // Use high priority for chat notifications
                    .setAutoCancel(false);

        } else {

            builder = new Notification.Builder(this, "1")
                    .setContentTitle("CONDEC Warning Detection")
                    .setContentText("Warning Detection is running")
                    .setSmallIcon(R.mipmap.ic_launcher_round);

        }

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
            Toast.makeText(CondecDetectionService.this, "Media Projection Initialized", Toast.LENGTH_SHORT).show();
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

        this.imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);

        virtualDisplay = mediaProjection.createVirtualDisplay("MediaProjector",
                screenWidth, screenHeight, screenDensity, VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                this.imageReader.getSurface(), null, null);
        if (virtualDisplay != null) {
            Log.i("VirtualDisplay", "Virtual Display Created");
            System.out.println("Virtual Display Created: " + (this.virtualDisplay != null));

        }

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    if (latestImage != null) {
                        latestImage.close();
                    }
                    latestImage = image;
                } else {
                    System.out.println("Image from ImageReader is null");
                }
            }
        }, handler);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (latestImage != null) {
                            System.out.println("TESTING AI");
                            try {
                                processImage(latestImage);

                            } catch (Exception e) {

                                System.out.println("TESTING AI ERROR: " + e);
                            }
                        } else {
                            System.out.println("Latest image is null");
                        }
                    }
                });
            }
        }, 0, 1000); // Run every 3 seconds

    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void processImage(Image image) {
        Bitmap bitmap = convertImageToBitmap(image);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 320, 320, true);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        float[][] output = new float[1][10]; // Adjust based on your model's output

        // Run the model
        tflite.run(inputBuffer, output);

        // Process the model output
        processModelOutput(output);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int INPUT_SIZE = 320; // Model's expected input size
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat((((val >> 16) & 0xFF) - 127.5f) / 127.5f);
                byteBuffer.putFloat((((val >> 8) & 0xFF) - 127.5f) / 127.5f);
                byteBuffer.putFloat(((val & 0xFF) - 127.5f) / 127.5f);
            }
        }
        return byteBuffer;
    }

    private Bitmap convertImageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        Bitmap bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
        buffer.rewind();
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    private void processModelOutput(float[][] output) {

        System.out.println("AI RESULTS: ");

        for (int i = 0; i < output.length; i++) {
            for (int j = 0; j < output[i].length; j++) {

                System.out.println("AI RESULTS DATA LAYER " + (j + 1) + " : " + output[i][j]);

            }
        }

        //System.out.println("FINAL AI RESULTS: " + output[0][9]);

        float[] probabilities = output[0]; // Assuming output is a 2D array

        // Find the highest probability
        float maxProbability = 0.0f;
        for (float probability : probabilities) {
            if (probability > maxProbability) {
                maxProbability = probability;
            }
        }

        // Convert to percentage
        int percentage = Math.round(maxProbability * 100);
        System.out.println("FINAL AI RESULTS (Percentage): " + percentage + "%");

        //Toast.makeText(CondecDetectionService.this, ("AI RESULTS: " + percentage + "%"), Toast.LENGTH_SHORT).show();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, createNotification("AI RESULTS: " + percentage + "%"));

    }

    private void stopProjection() {

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        this.notification = createNotification(null);
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

    @Override
    public boolean onUnbind(Intent intent) {
        // Handle service unbinding
       /* stopProjection();
        stopSelf();*/ // Optionally stop the service when unbound
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the AI processing timer
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Release the latest image if it exists
        if (latestImage != null) {
            latestImage.close();
            latestImage = null;
        }

        // Close the ImageReader
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        // Stop and release the MediaProjection
        stopProjection();

        // Release the TensorFlow Lite interpreter
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }

        // Optionally, stop the handler thread if it was created just for AI processing
        if (handler != null) {
            handler.getLooper().quitSafely();
            handler = null;
        }
    }

}