package com.example.condec;

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
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
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CondecDetectionService extends Service {

    public final IBinder binder = new LocalBinder();
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;

    private Notification notification;

    private ImageReader imageReader;
    private Interpreter tflite;
    private Handler handler;
    private Timer timer;


    private boolean bypassThreshold = true;
    private int resultCode;
    private Intent data;
    private Image latestImage;

    private long serviceStartTime;
    private Handler handlerTimeCheck;
    private HandlerThread handlerThreadTimeCheck;
    private HandlerThread inferenceThread;
    private Handler inferenceHandler;

    private HandlerThread imageProcessingThread;
    private Handler imageProcessingHandler;
    private static final long TWO_HOURS_IN_MILLIS = 2 * 60 * 60 * 1000;

    private boolean isImageReaderActive = false;  // Flag for ImageReader state
    private boolean isMediaProjectionActive = false;  // Flag for MediaProjection state

    private ByteArrayOutputStream latestImageStream;

    private Map<String, Integer> appThresholds = new HashMap<String, Integer>() {{
        put("com.google.android.youtube", 85);
        put("com.facebook.katana", 90);
        put("com.zhiliaoapp.musically", 90);
        put("com.instagram.android", 90);
        put("com.twitter.android", 90);
    }};

    public class LocalBinder extends Binder {
        CondecDetectionService getService() {
            return CondecDetectionService.this;
        }
    }

    private BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.condec.STOP_SERVICE".equals(intent.getAction())) {
                stopProjection();
                stopForeground(true);
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Notify Parental Service that Detection Service has started
        Intent intent = new Intent("com.example.condec.ACTION_DETECTION_STARTED");
        sendBroadcast(intent);

        isImageReaderActive = true;
        isMediaProjectionActive = true;

        Log.d("CondecDetectionService", "Detection Service started.");

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecDetectionServiceStatus", true);
        editor.apply();

        this.mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        try {
            tflite = new Interpreter(loadModelFile(this, "ai models/detect.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        HandlerThread handlerThread = new HandlerThread("InferenceThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        IntentFilter filter = new IntentFilter("com.example.condec.STOP_SERVICE");
        registerReceiver(stopServiceReceiver, filter);

        serviceStartTime = System.currentTimeMillis();

        // Start the inference thread
        inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        inferenceHandler = new Handler(inferenceThread.getLooper());

        handlerThreadTimeCheck= new HandlerThread("CondecBackgroundThread");
        handlerThreadTimeCheck.start();

        handlerTimeCheck = new Handler(handlerThreadTimeCheck.getLooper());
        handlerTimeCheck.postDelayed(timeCheckRunnable, TWO_HOURS_IN_MILLIS);
    }

    private Runnable timeCheckRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - serviceStartTime) >= TWO_HOURS_IN_MILLIS) {
                showTimeCheckDialog();
            }

            handlerTimeCheck.postDelayed(this, TWO_HOURS_IN_MILLIS);
        }
    };

    private void showTimeCheckDialog() {
        Intent intent = new Intent(this, DetectionTimeCheckDialog.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
                    .setContentTitle("ConDec Warning Detection")
                    .setContentText(message)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setAutoCancel(false);

        } else {

            builder = new Notification.Builder(this, "1")
                    .setContentTitle("ConDec is Running")
                    .setContentText("ConDec is running on background")
                    .setSmallIcon(R.mipmap.ic_launcher_round);

        }

        return builder.build();
    }
    public void startProjection(int resultCode, Intent data) {

        System.out.println("resultCode: " + resultCode);
        System.out.println("data: " + data);
        System.out.println("mediaProjectionManager: " + (mediaProjectionManager != null));
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d("CondecDetectionService", "MediaProjection has stopped");
                stopService();
            }
        }, handler);
        System.out.println("MediaProjector: " + mediaProjection);
        System.out.println("MediaProjector Initialized: " + (mediaProjection != null));
        if (mediaProjection != null) {

            try {
                createVirtualDisplay();
            } catch (Exception e) {
                Log.e("YourService", "Error in service", e);
                // Optionally notify the user or handle the error
            }

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
        if (virtualDisplay == null || imageReader == null) {
            Log.d("CondecDetectionService", "Virtual Display or Image Reader is not initialized");
            return;
        }

        // Initialize HandlerThread
        if (imageProcessingThread == null) {
            imageProcessingThread = new HandlerThread("ImageProcessingThread");
            imageProcessingThread.start();
            imageProcessingHandler = new Handler(imageProcessingThread.getLooper());
        }

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                synchronized (CondecDetectionService.this) {
                    try {
                        // Close the previous image if it exists
                        if (latestImage != null) {
                            latestImage.close();
                        }

                        // Acquire the latest image
                        image = reader.acquireLatestImage();
                        if (image != null) {
                            Bitmap bitmap = convertImageToBitmapFull(image);
                            storeLatestImage(bitmap);
                            latestImage = image; // Assign the latest image
                        } else {
                            Log.d("CondecDetectionService", "Image from ImageReader is null");
                        }
                    } catch (Exception e) {
                        Log.e("CondecDetectionService", "Error acquiring image", e);
                    } finally {
                        // Ensure the acquired image is closed if not already closed
                        if (image != null && image != latestImage) {
                            image.close();
                        }
                    }
                }
            }
        }, imageProcessingHandler);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try{

                            if (latestImage != null && isImageReaderActive && isMediaProjectionActive) {
                                System.out.println("TESTING AI");
                                try {

                                    String currentApp = getForegroundAppPackageName();
                                    Log.d("CondecDetectionService", "Retrived App: " + currentApp);

                                    if (currentApp != null && appThresholds.containsKey(currentApp) || bypassThreshold) {

                                        processImage(latestImage, currentApp);

                                    }
                                    else {

                                        Log.d("CondecDetectionService", "Skipping AI detection for unmonitored app: " + currentApp);
                                        //if (bypassThreshold == true) notifyUser("AI RESULTS: NOT LISTED TO DETECT Current App: " + currentApp);

                                    }



                                } catch (Exception e) {

                                    System.out.println("TESTING AI ERROR: " + e);
                                }
                            } else {
                                System.out.println("Latest image is null");
                            }

                        }catch (Exception e){

                            System.out.println("TESTING AI ERROR: " + e);

                        }
                    }
                });
            }
        }, 0, 1000);

    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void processImage(Image image, String currentApp) {
        Bitmap bitmap = convertImageToBitmap(image);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        float[][] output = new float[1][10];
        runInference(inputBuffer, output, currentApp);

    }

    private void runInference(ByteBuffer inputData, float[][] outputData, String currentApp) {
        inferenceHandler.post(() -> {
            synchronized (this) {
                if (tflite != null && isImageReaderActive && isMediaProjectionActive) {
                    try {
                        tflite.run(inputData, outputData);
                        processModelOutput(outputData, currentApp);
                    } catch (Exception e) {
                        Log.e("CondecDetectionService", "Error during inference", e);
                    }
                } else {
                    Log.w("CondecDetectionService", "tflite is null, skipping inference");
                }
            }
        });
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int INPUT_SIZE = 640;
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

    public String getForegroundAppPackageName() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long currentTime = System.currentTimeMillis();
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 10, currentTime);

        if (appList == null || appList.isEmpty()) {
            return null;
        }

        UsageStats recentStats = null;
        for (UsageStats usageStats : appList) {
            if (recentStats == null || usageStats.getLastTimeUsed() > recentStats.getLastTimeUsed()) {
                recentStats = usageStats;
            }
        }

        if (recentStats != null) {
            return recentStats.getPackageName();
        }

        return null;
    }


    private void processModelOutput(float[][] output, String currentApp) {

        System.out.println("AI RESULTS: ");

        for (int i = 0; i < output.length; i++) {
            for (int j = 0; j < output[i].length; j++) {

                System.out.println("AI RESULTS DATA LAYER " + (j + 1) + " : " + output[i][j]);

            }
        }

        if (currentApp != null && appThresholds.containsKey(currentApp)  || bypassThreshold) {
            float[] probabilities = output[0];
            float maxProbability = 0.0f;

            for (float probability : probabilities) {
                if (probability > maxProbability) {
                    maxProbability = probability;
                }
            }

            int percentage = Math.round(maxProbability * 100);
            System.out.println("FINAL AI RESULTS (Percentage): " + percentage + "%, Current App: " + currentApp);
            //if (bypassThreshold == true) notifyUser("AI RESULTS: " + percentage + "%");
            //notifyUser("AI RESULTS: " + percentage + "%, Current App: " + currentApp);

            if (appThresholds.containsKey(currentApp)  || bypassThreshold) {
                Log.d("CondecDetectionService", "Current App: " + currentApp);
                int threshold = 0;

                if(bypassThreshold == false) threshold = appThresholds.get(currentApp);

                Log.d("CondecDetectionService", "Current App AI Result: " + percentage);
                Log.d("CondecDetectionService", "Current App AI Threshold: " + threshold);

                Log.d("CondecDetectionService", "AI Judgement Result: " + (percentage > threshold));

                if (bypassThreshold == true) threshold = 93;

                if (percentage > threshold) {
                    Log.d("CondecAccessabilityService", "AI Perform Swipe And Back");
                    Intent intent = new Intent("com.example.ACTION_SWIPE_AND_BACK");
                    sendBroadcast(intent);
                }
            }
        } else {

            Log.d("CondecDetectionService", "Current app is not monitored: " + currentApp);
        }

    }

    // Convert Image to Bitmap
    private Bitmap convertImageToBitmapFull(Image image) {
        if (image == null) {
            Log.e("CondecDetectionService", "Image is null, cannot convert to bitmap.");
            return null;
        }

        // Check if the Image format is compatible with direct Bitmap creation
        if (image.getFormat() == PixelFormat.RGBA_8888 || image.getFormat() == ImageFormat.YUV_420_888) {
            int width = image.getWidth();
            int height = image.getHeight();

            // Get the pixel data from the image planes
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            // Create a Bitmap with the pixel data
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            return bitmap;
        } else {
            Log.e("CondecDetectionService", "Unsupported image format: " + image.getFormat());
            return null;
        }
    }

    // Store the latest image as a byte array for easy access by ParentalService
    private void storeLatestImage(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e("CondecDetectionService", "Bitmap is null, cannot store image.");
            return;
        }

        latestImageStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, latestImageStream); // Compress for efficient transmission
        Log.d("CondecDetectionService", "Bitmap is not null, storing image.");
    }

    // Public method for ParentalService to retrieve the latest image data
    public byte[] getLatestImageData() {
        return (latestImageStream != null) ? latestImageStream.toByteArray() : null;
    }

    private void notifyUser(String message){

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(1, createNotification(message));


    }

    private void stopProjection() {

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (mediaProjection != null && isMediaProjectionActive) {
            mediaProjection.stop();
            mediaProjection = null;
            isMediaProjectionActive = false;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            Log.e("CondecDetectionService", "Received null Intent. Stopping service.");
            return START_NOT_STICKY;
        }

        this.notification = createNotification(null);
        startForeground(1, notification);

        this.resultCode = intent.getIntExtra("CAPTURE_CODE", 0);
        this.data = intent.getParcelableExtra("CAPTURE_DATA");


        System.out.println("MediaProjection Initialized: " + (this.mediaProjection != null));
        startProjection(this.resultCode, this.data);


        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

        return binder;

    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }

    private synchronized void stopService() {
        Log.d("CondecDetectionService", "Stopping CondecDetectionService");

        // Notify the Parental Service to unbind
        Intent unbindIntent = new Intent("com.example.condec.ACTION_UNBIND_DETECTION");
        sendBroadcast(unbindIntent);

        // Stop the screen projection
        stopProjection();

        // Synchronize access to latestImage to avoid concurrent modification
        synchronized (this) {

            if (latestImage != null) {
                latestImage.close();
                latestImage = null;
            }

            if (imageReader != null) {
                imageReader.close();
                isImageReaderActive = false;
                imageReader = null;
            }

            if (imageProcessingThread != null) {
                imageProcessingThread.quitSafely();
                imageProcessingThread = null;
                imageProcessingHandler = null;
            }

        }

        // Cancel any existing timer tasks
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Remove any scheduled time checks
        if (handlerTimeCheck != null) {
            handlerTimeCheck.removeCallbacks(timeCheckRunnable);
        }

        // Clean up the inference thread and interpreter safely
        if (inferenceThread != null) {
            try {
                inferenceThread.join(1000); // Add timeout for join to avoid blocking indefinitely
            } catch (InterruptedException e) {
                Log.e("CondecDetectionService", "Error joining inference thread", e);
            }
            inferenceThread.quitSafely();
            inferenceThread = null;
        }

        if (tflite != null) {
            tflite.close();
            tflite = null;
        }

        // Clean up the handler and its looper
        if (handler != null) {
            try {
                handler.getLooper().quitSafely();
            } catch (Exception e) {
                Log.e("CondecDetectionService", "Error quitting handler looper", e);
            }
            handler = null;
        }

        // Clean up the handler thread used for time checks
        if (handlerThreadTimeCheck != null) {
            try {
                handlerThreadTimeCheck.quitSafely();
                handlerThreadTimeCheck.join(1000); // Timeout for joining thread
            } catch (InterruptedException e) {
                Log.e("CondecDetectionService", "Error joining time check thread", e);
            }
            handlerThreadTimeCheck = null;
        }

        // Stop the foreground service
        stopForeground(true);

        // Delay stopping the service to allow for unbinding
        new Handler().postDelayed(this::stopSelf, 1000);  // 1 second delay
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister the stop service receiver
        unregisterReceiver(stopServiceReceiver);

        // Update service status in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecDetectionServiceStatus", false);
        editor.apply();

        stopService();
/*
        // Cancel the timer if it's running
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Remove scheduled time check callbacks
        handlerTimeCheck.removeCallbacks(timeCheckRunnable);

        // Clean up the image resources
        if (latestImage != null) {
            latestImage.close();
            latestImage = null;
        }

        if (imageReader != null && isImageReaderActive) {
            imageReader.close();
            imageReader = null;
            isImageReaderActive = false;
        }

        // Stop the screen projection
        stopProjection();

        // Clean up the TensorFlow Lite interpreter
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }

        // Clean up the inference thread and interpreter in onDestroy as well
        if (inferenceThread != null) {
            try {
                inferenceThread.join();
            } catch (InterruptedException e) {
                Log.e("CondecDetectionService", "Error joining inference thread", e);
            }
            inferenceThread.quitSafely();
            inferenceThread = null;
        }

        // Stop and clean up the handler and thread
        if (handler != null) {
            handler.getLooper().quitSafely();
            handler = null;
        }

        if (handlerThreadTimeCheck != null) {
            try {
                handlerThreadTimeCheck.quitSafely();
                handlerThreadTimeCheck.join();
            } catch (InterruptedException e) {
                Log.e("CondecDetectionService", "Error joining time check thread", e);
            }
            handlerThreadTimeCheck = null;
        }*/

    }
}