package com.example.condec;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.condec.Classes.ParentalAppUsageInfo;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CondecParentalService extends Service {

    public String localDeviceName;
    private final IBinder binder = new LocalBinder();
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.RegistrationListener registrationListener;
    private List<NsdServiceInfo> discoveredDevices = new ArrayList<>();
    private ServerSocket serverSocket;
    private int serverPort = 12345;
    private boolean isRunning = true;
    public final ExecutorService executorService = Executors.newCachedThreadPool();
    private WindowManager windowManager;
    private View dialogView;

    private Bitmap latestImage;

    private Handler imageRequestHandler;
    private Runnable imageRequestRunnable;
    private boolean isImageRequestActive = false;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public CondecParentalService getService() {
            return CondecParentalService.this;
        }
    }

    private CondecDetectionService condecDetectionService;

    private boolean isBound;

    private BroadcastReceiver detectionStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Intent bindIntent = new Intent(CondecParentalService.this, CondecDetectionService.class);
            bindService(bindIntent, connection, Context.BIND_AUTO_CREATE);
            Log.d("CondecParentalService", "Binding to Detection Service.");
        }
    };

    private BroadcastReceiver warningDetectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Condec Parental", "Received Warning Detection");
            if ("com.example.condec.SENSITIVE_WARNING_DETECTED".equals(intent.getAction())) {
                Log.d("Condec Parental", "SENSITIVE_WARNING_DETECTED");
                informWarningDetection();
            }
        }
    };

    private BroadcastReceiver unbindReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Unbind from the Detection Service if bound
            if (isBound) {
                unbindService(connection);
                isBound = false;
                Log.d("CondecParentalService", "Unbound from Detection Service on stop request.");
            }
        }
    };

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CondecDetectionService.LocalBinder binder = (CondecDetectionService.LocalBinder) service;
            condecDetectionService = binder.getService();
            isBound = true;
            Log.d("CondecParentalService", "Successfully bound to Detection Service.");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            Log.d("CondecParentalService", "Unbound from Detection Service.");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, createNotification());

        IntentFilter filter = new IntentFilter("com.example.condec.ACTION_DETECTION_STARTED");
        registerReceiver(detectionStartedReceiver, filter);

        IntentFilter detectionFilter = new IntentFilter("com.example.condec.SENSITIVE_WARNING_DETECTED");
        registerReceiver(warningDetectedReceiver, detectionFilter);

        IntentFilter unbindFilter = new IntentFilter("com.example.condec.ACTION_UNBIND_DETECTION");
        registerReceiver(unbindReceiver, unbindFilter);

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.condec.STOP_IMAGE_REQUESTS".equals(intent.getAction())) {
                    stopImageRequestLoop();
                    Log.d("CondecParentalService", "Image request loop stopped.");
                }
            }
        }, new IntentFilter("com.example.condec.STOP_IMAGE_REQUESTS"));

        SharedPreferences sharedPref = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        localDeviceName = sharedPref.getString("deviceName", "My Device");

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("condecParentalServiceStatus", true);
        editor.apply();

        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        registerService();

        new Thread(this::startServer).start();

        sharedPref.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if (key.equals("deviceName")) {
                String newDeviceName = sharedPreferences.getString("deviceName", "My Device");
                if (!newDeviceName.equals(localDeviceName)) {
                    localDeviceName = newDeviceName;
                    restartService();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        executorService.shutdown();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopDiscovery();

        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        unregisterReceiver(detectionStartedReceiver);
        unregisterReceiver(warningDetectedReceiver);
        unregisterReceiver(unbindReceiver);

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecParentalServiceStatus", false);
        editor.apply();

        super.onDestroy();
    }

    private Notification createNotification() {
        String channelId = "discovery_service_channel";
        String channelName = "Condec Main Service";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("ConDec is Running")
                .setContentText("ConDec is running on background")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    private void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                stopDiscovery();
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                stopDiscovery();
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d("NSD Condec", "Discovery started.");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d("NSD Condec", "Discovery stopped.");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                String serviceName = serviceInfo.getServiceName();
                Log.d("NSD Condec", "Service found: " + serviceInfo.getServiceName());
                Log.d("NSD Condec", "Host: " + serviceInfo.getHost());
                Log.d("NSD Condec", "Port: " + serviceInfo.getPort());

                if (serviceName.equals(localDeviceName)) {
                    return;
                }

                if (!discoveredDevices.contains(serviceInfo)) {
                    discoveredDevices.add(serviceInfo);
                    notifyDeviceDiscovered(serviceInfo);
                }

                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e("NSD Condec", "Resolve failed: " + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo resolvedServiceInfo) {
                        Log.d("NSD Condec", "Service resolved: " + resolvedServiceInfo.getServiceName());
                        handleServiceResolved(resolvedServiceInfo);
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                InetAddress lostHost = serviceInfo.getHost();
                int lostPort = serviceInfo.getPort();

                Log.d("NSD Condec", "Service lost: " + serviceInfo.getServiceName());
                Log.d("NSD Condec", "Lost Host: " + lostHost);
                Log.d("NSD Condec", "Lost Port: " + lostPort);

                cleanUpDiscoveredDevices();

            }
        };
    }

    private void restartService() {
        unregisterService();
        stopDiscovery();
        registerService();
        refreshDiscoveredDevices();
    }

    private void unregisterService() {
        if (registrationListener != null) {
            nsdManager.unregisterService(registrationListener);
            registrationListener = null;
        }
    }

    private void notifyDevicesChanged() {
        Intent intent = new Intent("com.example.condec.DEVICE_LIST_CHANGED");
        sendBroadcast(intent);
    }

    private void registerService() {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(localDeviceName);
        serviceInfo.setServiceType("_condec._tcp.");
        serviceInfo.setPort(serverPort);

        registrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                localDeviceName = NsdServiceInfo.getServiceName();
                startDiscovery();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e("NSD Condec", "Service registration failed with code: " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.d("NSD Condec", "Service unregistered.");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e("NSD Condec", "Service unregistration failed with code: " + errorCode);
            }
        };

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(serverPort);
            Log.d("CondecServer", "Server started and listening on port " + serverPort);
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                Log.d("CondecServer", "Client connected");
                executorService.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            Log.e("CondecServer", "Error starting server: " + e.getMessage());
        }
    }

    public NsdServiceInfo getDeviceInfoByName(String deviceName) {
        for (NsdServiceInfo deviceInfo : discoveredDevices) {
            if (deviceInfo.getServiceName().equals(deviceName)) {
                return deviceInfo;
            }
        }
        return null;
    }

    private void handleClient(Socket clientSocket) {
        try {
            Log.d("CondecServer", "Handling client");

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = in.readLine();

            if (message != null) {
                String[] parts = message.split(":");

                Log.d("CondecParentalService", "Message length: " + parts.length);

                if (parts.length == 2) {
                    Log.d("CondecParentalService", "Getting Service Statuses");

                    handleClientData(clientSocket, parts);
                } else if (parts.length >= 3) {
                    Log.d("CondecParentalService", "Command Recieved");

                    if (!parts[2].equals("SLEEP_CONTROL") && parts.length == 3){

                        handleClientCommand(clientSocket, parts);

                    }
                    else if (parts[2].equals("SLEEP_CONTROL") && parts.length == 3){

                        handleClientSleepData(clientSocket, parts);

                    } else if (parts.length == 4) {

                        handleClientSleepCommand(clientSocket, parts);

                    }

                    if (parts[2].equals("APP_USAGE_DATA")) {
                        Log.d("CondecParentalService", "SENDING APP USAGE DATA");
                        Log.d("CondecParentalService", "Handling App Usage Data");
                        handleRequestAppUsageData(clientSocket);
                    }

                }
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<ParentalAppUsageInfo> collectAppUsageData() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, startTime, endTime);
        if (usageStatsList == null || usageStatsList.isEmpty()) {
            Log.d("CondecParentalService", "No usage data available.");
            return null;
        }

        List<ParentalAppUsageInfo> appUsageList = new ArrayList<>();
        Set<String> addedPackages = new HashSet<>();
        PackageManager pm = getPackageManager();

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = pm.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY);
        String defaultLauncherPackage = resolveInfo.activityInfo.packageName;

        String myPackageName = getPackageName();

        for (UsageStats usageStats : usageStatsList) {
            String packageName = usageStats.getPackageName();
            long usageTime = usageStats.getTotalTimeInForeground();
            long lastTimeUsed = usageStats.getLastTimeUsed();

            if (usageTime > 0 || lastTimeUsed > 0) {
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

                    if (packageName.equals(myPackageName) || packageName.equals(defaultLauncherPackage)) {
                        continue;
                    }

                    if (isSystemApp(appInfo) && !isAllowedSystemApp(packageName)) {
                        continue;
                    }

                    if (!addedPackages.contains(packageName)) {
                        String appName = pm.getApplicationLabel(appInfo).toString();
                        Drawable appIcon = pm.getApplicationIcon(appInfo);
                        appUsageList.add(new ParentalAppUsageInfo(packageName, appName, appIcon, usageTime, lastTimeUsed));
                        addedPackages.add(packageName);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        Log.d("CondecParentalService", "Total apps found: " + appUsageList.size());

        return appUsageList;
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        return ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) && ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0);
    }

    private boolean isAllowedSystemApp(String packageName) {
        return packageName.equals("com.android.vending")
                || packageName.equals("com.google.android.youtube")
                || packageName.equals("com.android.chrome");
    }

    private void handleRequestAppUsageData(Socket clientSocket) {
            try {

                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                List<ParentalAppUsageInfo> appUsageList = collectAppUsageData();

                for (ParentalAppUsageInfo parentalAppUsageInfo : appUsageList) {
                    String dataToSend = String.format("%s|%d|%d|%s",
                            parentalAppUsageInfo.getAppName(),
                            parentalAppUsageInfo.getUsageTime(),
                            parentalAppUsageInfo.getLastTimeUsed(),
                            encodeIconToBase64(parentalAppUsageInfo.getAppIcon())
                    );
                    Log.d("CondecParentalService", "App Data Sent: " + dataToSend);
                    out.println(dataToSend);
                }

                out.println("END_OF_APP_USAGE_DATA");
                out.flush();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private String encodeIconToBase64(Drawable icon) {
        Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public void requestAppUsageData(NsdServiceInfo deviceInfo, ParentalControlActivity parentalControlActivity) {
        executorService.execute(() -> {
            try {
                Socket socket = new Socket(deviceInfo.getHost(), deviceInfo.getPort());
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                out.println(localDeviceName + ":" + deviceInfo.getServiceName() + ":" + "APP_USAGE_DATA");
                Log.d("CondecParentalService", "REQUESTING APP USAGE DATA");

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                List<String> receivedData = new ArrayList<>();
                String line;
                String temp = "";
                while ((line = in.readLine()) != null) {
                    Log.d("CondecParentalService", "App Data Received: " + line);
                    temp += line;
                    if (line.equals("END_OF_APP_USAGE_DATA")) {
                        break;
                    }
                    if(line.isEmpty()){

                        receivedData.add(temp);
                        temp = "";

                    }

                }


                for (String data : receivedData) {
                    Log.d("CondecParentalService", data);
                }

                handleReceivedAppUsageData(receivedData, parentalControlActivity);

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleReceivedAppUsageData(List<String> receivedData, ParentalControlActivity parentalControlActivity) {
        List<ParentalAppUsageInfo> appUsageList = new ArrayList<>();

        for (String data : receivedData) {
            if (data.equals("END_OF_APP_USAGE_DATA")) {
                break;
            }

            String[] parts = data.split("\\|");

            if (parts.length >= 4) {
                String appName = parts[0];
                long usageTime = Long.parseLong(parts[1]);
                long lastTimeUsed = Long.parseLong(parts[2]);
                Drawable appIcon = decodeBase64ToDrawable(parts[3]);

                appUsageList.add(new ParentalAppUsageInfo(null, appName, appIcon, usageTime, lastTimeUsed));
            } else {
                Log.e("CondecParentalService", "Invalid data received: " + data);
            }
        }

        appUsageList.sort((app1, app2) -> Long.compare(app2.getLastTimeUsed(), app1.getLastTimeUsed()));

        Log.d("CondecParentalService", "App usage data processed: " + appUsageList.size() + " apps.");

        Intent intent = new Intent(parentalControlActivity, ParentalAppUsageActivity.class);
        intent.putExtra("deviceName", parentalControlActivity.getCurrentDeviceTarget());
        intent.putParcelableArrayListExtra("appUsageList", (ArrayList<ParentalAppUsageInfo>) appUsageList);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private Drawable decodeBase64ToDrawable(String base64Icon) {
        try {
            byte[] decodedBytes = Base64.decode(base64Icon, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            return new BitmapDrawable(getResources(), bitmap);
        } catch (IllegalArgumentException e) {
            Log.e("CondecParentalService", "Base64 decoding error: " + e.getMessage());
            return null;
        }
    }

    private void handleClientData(Socket clientSocket, String[] parts) {
        try {
            String senderDeviceName = parts[0];
            String targetDeviceName = parts[1];

            if (localDeviceName.equals(targetDeviceName)) {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

                String deviceName = sharedPreferences.getString("deviceName", null);
                boolean isDetecting = isServiceRunning(CondecDetectionService.class);
                boolean isAppBlocking = isServiceRunning(CondecBlockingService.class);
                boolean isWebsiteBlocking = isServiceRunning(CondecVPNService.class);
                boolean isSleeping = isServiceRunning(CondecSleepService.class);
                boolean isPreventing = isServiceRunning(CondecSecurityService.class);

                Log.d("Condec Parental", "Received Request from: " + senderDeviceName);

                Log.d("Condec Parental", "sending deviceName: " + deviceName);
                Log.d("Condec Parental", "sending isDetecting: " + isDetecting);
                Log.d("Condec Parental", "sending isAppBlocking: " + isAppBlocking);
                Log.d("Condec Parental", "sending isWebsiteBlocking: " + isWebsiteBlocking);
                Log.d("Condec Parental", "sending isSleeping: " + isSleeping);
                Log.d("Condec Parental", "sending isPreventing: " + isPreventing);

                String[] dataToSend = {
                        "DeviceName:" + deviceName,
                        "Detection:" + isDetecting,
                        "BlockingApp:" + isAppBlocking,
                        "BlockingWebsite:" + isWebsiteBlocking,
                        "Sleeping:" + isSleeping,
                        "Prevention:" + isPreventing,
                };
                for (String data : dataToSend) {
                    out.println(data);
                }
                out.println("END_OF_DATA");
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSleepData(Socket clientSocket, String[] parts) {
        try {
            String senderDeviceName = parts[0];
            String targetDeviceName = parts[1];

            if (localDeviceName.equals(targetDeviceName)) {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

                String deviceName = sharedPreferences.getString("deviceName", null);
                boolean isTimeBased = sharedPreferences.getBoolean("sleepUseTimeOn", false);
                boolean isOverride = sharedPreferences.getBoolean("sleepManualOn", false);
                Long startTime = sharedPreferences.getLong("sleepStartTime", -1);
                Long endTime = sharedPreferences.getLong("sleepEndTime", -1);

                Log.d("Condec Parental", "Received Request from: " + senderDeviceName);

                Log.d("Condec Parental", "sending deviceName: " + deviceName);
                Log.d("Condec Parental", "sending Time Based: " + isTimeBased);
                Log.d("Condec Parental", "sending Override: " + isOverride);
                Log.d("Condec Parental", "sending Start Time: " + startTime);
                Log.d("Condec Parental", "sending End Time: " + endTime);

                String[] dataToSend = {
                        "DeviceName:" + deviceName,
                        "TimeBased:" + isTimeBased,
                        "Override:" + isOverride,
                        "StartTime:" + startTime,
                        "EndTime:" + endTime,
                };
                for (String data : dataToSend) {
                    out.println(data);
                }
                out.println("END_OF_SLEEP_DATA");
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSleepCommand(Socket clientSocket, String[] parts) {

        Log.d("Condec Parental", "Command Recieved in Method");

        try {
            String senderDeviceName = parts[0];
            String targetDeviceName = parts[1];
            String command = parts[2];
            Log.d("Condec Parental", "Authentication: between " + senderDeviceName + " and " + targetDeviceName);
            if (localDeviceName.equals(targetDeviceName)) {
                Log.d("Condec Parental", "Authentication: Accepted " + localDeviceName + " and " + targetDeviceName + " are equal");
                handleSleepCommand(command, parts[3]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClientCommand(Socket clientSocket, String[] parts) {

        Log.d("Condec Parental", "Command Recieved in Method");

        try {
            String senderDeviceName = parts[0];
            String targetDeviceName = parts[1];
            String command = parts[2];
            Log.d("Condec Parental", "Authentication: between " + senderDeviceName + " and " + targetDeviceName);
            if (localDeviceName.equals(targetDeviceName) && (targetDeviceName.equals("%PARENT%") == false)) {
                if ("CHECK_DETECTION_STATUS".equals(command)) {
                    // Check if Detection Service is active
                    boolean isDetectionActive = isServiceRunning(CondecDetectionService.class);
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                    if (isDetectionActive) {
                        Log.d("CondecParentalService", "Detection service is active, granting access.");
                        out.println("DETECTION_ACTIVE"); // Reply to Device A
                    } else {
                        out.println("DETECTION_INACTIVE");
                    }
                    out.flush();
                }
                else if ("START_IMAGE_STREAM".equals(command) && isServiceRunning(CondecDetectionService.class)) {
                    Log.d("CondecParentalService", "Starting image stream to " + senderDeviceName);
                    startImageStream(clientSocket, senderDeviceName);
                }
                Log.d("Condec Parental", "Authentication: Accepted " + localDeviceName + " and " + targetDeviceName + " are equal");
                handleCommand(command);
            } else if ((localDeviceName.equals(senderDeviceName) == false) && targetDeviceName.equals("%PARENT%")) {

                Log.d("Condec Parental", "Authentication To All Parents: Accepted " + localDeviceName + " and " + targetDeviceName + " are equal");
                SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
                boolean isParentModeActive = condecPreferences.getBoolean("parentModeState", false);

                if (isParentModeActive){

                    handleCommand(command);

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSleepCommand(String command, String input) {

        Log.d("Condec Parental", "Sleep Command Received: " + command);
        Log.d("Condec Parental", "Input Received: " + input);

        switch (command) {
            case "TIMED_BASED":
                setTimedBasedSleep(Boolean.parseBoolean(input));
                break;
            case "SLEEP_OVERRIDE":
                setOverriddenSleep(Boolean.parseBoolean(input));
                break;
            case "SET_SLEEP_START_TIME":
                setStartTimeSleep(Long.parseLong(input));
                break;
            case "SET_SLEEP_END_TIME":
                setEndTimeSleep(Long.parseLong(input));
                break;
            case "CANCEL_SCHEDULED_SLEEP":
                cancelScheduledSleepTime();
                break;
            case "RESCHEDULED_SLEEP":
                reScheduleSleepTime();
                break;
            default:
                Log.d("Condec Parental", "Unknown Sleep Command: " + command);
                //showToast("Unknown Sleep Command: " + command);
                break;
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void reScheduleSleepTime() {

        Calendar startTimeCalendar = Calendar.getInstance();
        Calendar endTimeCalendar = Calendar.getInstance();

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        long startMillis = sharedPreferences.getLong("sleepStartTime", -1);
        long endMillis = sharedPreferences.getLong("sleepEndTime", -1);

        startTimeCalendar.setTimeInMillis(startMillis);
        endTimeCalendar.setTimeInMillis(endMillis);

        long currentTime = System.currentTimeMillis();
        long startTime = startTimeCalendar.getTimeInMillis();
        long endTime = endTimeCalendar.getTimeInMillis();

        Intent startIntent = new Intent(this, CondecSleepService.class);
        startIntent.setAction("START_SERVICE");
        PendingIntent startPendingIntent = PendingIntent.getForegroundService(
                this, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, CondecSleepService.class);
        stopIntent.setAction("STOP_SERVICE");
        PendingIntent stopPendingIntent = PendingIntent.getForegroundService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(startPendingIntent);
            alarmManager.cancel(stopPendingIntent);
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, startPendingIntent);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, stopPendingIntent);

        if (currentTime >= startTime && currentTime < endTime) {
            Log.d("CondecSleepService", "Starting service immediately as current time is within the sleep window.");
            startForegroundService(new Intent(this, CondecSleepService.class));
        } else if (currentTime >= endTime) {
            Log.d("CondecSleepService", "Current time is past end time; not starting the service.");
            stopService(new Intent(this, CondecSleepService.class));
        }
    }

    private void cancelScheduledSleepTime(){

        PendingIntent startPendingIntent;
        PendingIntent stopPendingIntent;

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent startIntent = new Intent(this, CondecSleepService.class);
        startPendingIntent = PendingIntent.getService(this, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, CondecSleepService.class);
        stopIntent.setAction("STOP_SERVICE");
        stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            if (startPendingIntent != null) {

                alarmManager.cancel(startPendingIntent);
            }
            if (stopPendingIntent != null) {

                alarmManager.cancel(stopPendingIntent);
            }
        }

    }

    private void setTimedBasedSleep(boolean isTimedBased){

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("sleepUseTimeOn", isTimedBased);
        editor.apply();

        boolean isOverride = sharedPreferences.getBoolean("sleepManualOn", false);
        boolean isSleepServiceActive = isServiceRunning(CondecSleepService.class);

        if (isOverride == true || isSleepServiceActive == true){

            editor.putBoolean("sleepManualOn", false);
            stopService(new Intent(this, CondecSleepService.class));

        }

        reScheduleSleepTime();

    }

    private void setOverriddenSleep(boolean isOverridden){

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("sleepManualOn", isOverridden);
        editor.apply();

        boolean isTimedBased = sharedPreferences.getBoolean("sleepUseTimeOn", false);
        boolean isSleepServiceActive = isServiceRunning(CondecSleepService.class);

        if (isTimedBased == true){

            editor.putBoolean("sleepUseTimeOn", false);

        }

        cancelScheduledSleepTime();

        if (isOverridden == true && isSleepServiceActive == false){

            startForegroundService(new Intent(this, CondecSleepService.class));

        }
        else {

            stopService(new Intent(this, CondecSleepService.class));

        }

    }

    private void setStartTimeSleep(long milliseconds){

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putLong("sleepStartTime", milliseconds);
        editor.apply();

        boolean isTimedBased = sharedPreferences.getBoolean("sleepUseTimeOn", false);

        if(isTimedBased == true){

            reScheduleSleepTime();

        }
    }

    private void setEndTimeSleep(long milliseconds){

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putLong("sleepEndTime", milliseconds);
        editor.apply();

        boolean isTimedBased = sharedPreferences.getBoolean("sleepUseTimeOn", false);

        if(isTimedBased == true){

            reScheduleSleepTime();

        }

    }


    private void handleCommand(String command) {

        Log.d("Condec Parental", "Command Received:" + command);
        SharedPreferences sharedPref = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sharedPref.edit();

        int indexOfColonCommand = command.indexOf("|");
        String data = "";

        if (indexOfColonCommand != -1){

            String formattedCommand = command.substring(0, indexOfColonCommand);
            String formattedData = command.substring(indexOfColonCommand + 1, command.length());
            Log.d("Condec Parental", "Formatted Command:" + formattedCommand);
            Log.d("Condec Parental", "Formatted Data:" + formattedData);

            if(formattedCommand.equals("DISPLAY_MESSAGE") || formattedCommand.equals("WARNING_DETECTED")){

                command = formattedCommand;
                data = formattedData;

            }
        }

        switch (command) {
            case "START_DETECTION":
                Intent requestIntent = new Intent(this, RequestDetectionPermission.class);
                requestIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(requestIntent);

                editor.putBoolean("isDetectionServiceManuallyOff", false);
                editor.apply();

                Intent broadcastDetectionOnIntent = new Intent("com.example.condec.UPDATE_SECURITY_FLAGS_DETECTION_OFF");
                sendBroadcast(broadcastDetectionOnIntent);
                showToast("Remotely Started Detection Service");
                break;
            case "STOP_DETECTION":

                // Notify the Parental Service to unbind before stopping
                Intent intentUnbindDetection = new Intent("com.example.condec.ACTION_UNBIND_DETECTION");
                sendBroadcast(intentUnbindDetection);

                stopService(new Intent(this, CondecDetectionService.class));

                editor.putBoolean("isDetectionServiceManuallyOff", true);
                editor.apply();

                Intent broadcastDetectionOffIntent = new Intent("com.example.condec.UPDATE_SECURITY_FLAGS_DETECTION_OFF");
                sendBroadcast(broadcastDetectionOffIntent);
                showToast("Remotely Stopped Detection Service");
                break;
            case "START_APP_BLOCKING":
                startForegroundService(new Intent(this, CondecBlockingService.class));
                showToast("Remotely Started App Blocking Service");
                break;
            case "STOP_APP_BLOCKING":
                stopService(new Intent(this, CondecBlockingService.class));
                showToast("Remotely Stopped App Blocking Service");
                break;
            case "START_WEBSITE_BLOCKING":
                Intent intent = new Intent(this, CondecVPNService.class);
                startService(intent);
                showToast("Remotely Started Website Blocking Service");
                break;
            case "STOP_WEBSITE_BLOCKING":
                Intent serviceIntent = new Intent(this, CondecVPNService.class);
                serviceIntent.setAction(CondecVPNService.ACTION_STOP_VPN);
                startService(serviceIntent);
                showToast("Remotely Stopped Website Blocking Service");
                break;
            case "START_SLEEP":
                startForegroundService(new Intent(this, CondecSleepService.class));
                showToast("Remotely Started Sleep Service");
                break;
            case "STOP_SLEEP":
                stopService(new Intent(this, CondecSleepService.class));
                showToast("Remotely Stopped Sleep Service");
                break;
            case "TOGGLE_SLEEP":
                if (!isServiceRunning(CondecSleepService.class)){
                    startForegroundService(new Intent(this, CondecSleepService.class));
                    showToast("Remotely Started Sleep Service");
                }
                else {
                    stopService(new Intent(this, CondecSleepService.class));
                    showToast("Remotely Stopped Sleep Service");
                }
                break;
            case "REQUEST_SCREEN_CAPTURE":

                break;
            case "DISPLAY_MESSAGE":

                int indexOfColonParent = data.indexOf("|");
                String parent = "";
                String parentMessage = "";

                if (indexOfColonParent != -1){

                    String formattedParent = data.substring(0, indexOfColonParent);
                    String formattedMessage = data.substring(indexOfColonParent + 1, data.length());

                    Log.d("Condec Parental", "Formatted Parent:" + formattedParent);
                    Log.d("Condec Parental", "Formatted Message:" + formattedMessage);

                    parent = formattedParent;
                    parentMessage = formattedMessage;
                    displayMessageFromParent(parent, parentMessage);

                }

                break;
            case "GO_BACK":
                Intent intentBack = new Intent("com.example.ACTION_GO_BACK");
                sendBroadcast(intentBack);
                break;
            case "GO_HOME":
                Intent intentHome = new Intent("com.example.ACTION_GO_HOME");
                sendBroadcast(intentHome);
                break;
            case "START_SECURITY":
                startForegroundService(new Intent(this, CondecSecurityService.class));
                showToast("Remotely Started Security Service");
                break;
            case "STOP_SECURITY":
                stopService(new Intent(this, CondecSecurityService.class));
                showToast("Remotely Stopped Security Service");
                break;
            case "WARNING_DETECTED":
                Log.d("Condec Parental", "WARNING_DETECTED: from: " + data);
                displayDetectedMessage(data);
                break;

            default:
                Log.d("Condec Parental", "Unknown Command: " + command);
                //showToast("Unknown Command: " + command);
                break;
        }
    }

    private void informWarningDetection(){

        Log.d("Condec Parental", "informing Parents about " + localDeviceName);
        sendCommandToParent("WARNING_DETECTED|" + localDeviceName);

    }

    public void sendCommandToParent(String command) {
        executorService.execute(() -> {
            try {
                Log.d("Condec Parental", "Sending command to parents");
                Log.d("Condec Parental", "Command: " + command);
                for (NsdServiceInfo deviceInfo : discoveredDevices) {
                    Socket socket = new Socket(deviceInfo.getHost(), deviceInfo.getPort());
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    out.println(localDeviceName + ":" + "%PARENT%" + ":" + command);
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendCommandToDevice(NsdServiceInfo deviceInfo, String command) {
        executorService.execute(() -> {
            try {
                Log.d("Condec Parental", "deviceInfo: " + deviceInfo);
                Log.d("Condec Parental", "host: " + deviceInfo.getHost());
                Log.d("Condec Parental", "port: " + deviceInfo.getPort());
                Socket socket = new Socket(deviceInfo.getHost(), deviceInfo.getPort());
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println(localDeviceName + ":" + deviceInfo.getServiceName() + ":" + command);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendSleepCommandToDevice(NsdServiceInfo deviceInfo, String command, String input) {
        executorService.execute(() -> {
            try {
                Log.d("Condec Parental", "deviceInfo: " + deviceInfo);
                Log.d("Condec Parental", "host: " + deviceInfo.getHost());
                Log.d("Condec Parental", "port: " + deviceInfo.getPort());
                Socket socket = new Socket(deviceInfo.getHost(), deviceInfo.getPort());
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println(localDeviceName + ":" + deviceInfo.getServiceName() + ":" + command + ":" + input);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendCallToDevice(NsdServiceInfo deviceInfo) {
        executorService.execute(() -> {
            try {
                Socket socket = new Socket(deviceInfo.getHost(), deviceInfo.getPort());
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println(localDeviceName + ":" + deviceInfo.getServiceName());

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                List<String> receivedData = new ArrayList<>();
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("END_OF_DATA")) {
                        break;
                    }
                    receivedData.add(line);
                }

                Log.d("Condec Parental", "DATA RECEIVED From : " + (receivedData.get(0)).split(":")[1]);

                for (String data : receivedData){

                    Log.d("Condec Parental", data);

                }

                handleReceivedData(receivedData);

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void requestSleepData(NsdServiceInfo deviceInfo, ParentalControlActivity parentalControlActivity) {
        executorService.execute(() -> {
            try {
                Socket socket = new Socket(deviceInfo.getHost(), deviceInfo.getPort());
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println(localDeviceName + ":" + deviceInfo.getServiceName() + ":" + "SLEEP_CONTROL");

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                List<String> receivedData = new ArrayList<>();
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("END_OF_SLEEP_DATA")) {
                        break;  // End of data marker
                    }
                    receivedData.add(line);
                }

                Log.d("Condec Parental", "DATA RECEIVED From : " + (receivedData.get(0)).split(":")[1]);

                for (String data : receivedData){

                    Log.d("Condec Parental", data);

                }

                handleReceivedSleepData(receivedData, parentalControlActivity);

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleReceivedData(List<String> data) {
        Intent intent = new Intent(this, ParentalControlActivity.class);
        intent.putStringArrayListExtra("receivedData", new ArrayList<>(data));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void handleReceivedSleepData(List<String> data, ParentalControlActivity parentalControlActivity) {

        parentalControlActivity.showSleepSettings(data);

    }

    private void showToast(String message) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    public void refreshDiscoveredDevices() {
        stopDiscovery();
        startDiscovery();
        notifyDevicesChanged();
    }

    private void notifyDeviceDiscovered(NsdServiceInfo deviceInfo) {
        Intent intent = new Intent("com.example.condec.DEVICE_DISCOVERED");
        intent.putExtra("deviceName", deviceInfo.getServiceName());
        sendBroadcast(intent);
    }

    public void debugDiscoveredDevices() {
        for (NsdServiceInfo deviceInfo : discoveredDevices) {
            Log.d("CondecSender", "Device Info: " + deviceInfo);
            Log.d("CondecSender", "Host: " + deviceInfo.getHost());
            Log.d("CondecSender", "Port: " + deviceInfo.getPort());
        }
    }

    private void handleServiceResolved(NsdServiceInfo resolvedServiceInfo) {
        Log.d("NSD Condec", "Service resolved: " + resolvedServiceInfo.getServiceName());
        InetAddress resolvedHost = resolvedServiceInfo.getHost();
        int resolvedPort = resolvedServiceInfo.getPort();

        if (resolvedHost != null && resolvedPort > 0) {

            discoveredDevices.removeIf(device -> device.getServiceName().equals(resolvedServiceInfo.getServiceName()));

            discoveredDevices.add(resolvedServiceInfo);
            Log.d("NSD Condec", "Added resolved device to the list.");
        } else {
            Log.e("NSD Condec", "Resolved service has invalid host or port. Device ignored.");
        }

        cleanUpDiscoveredDevices();
    }

    private void cleanUpDiscoveredDevices() {
        Iterator<NsdServiceInfo> iterator = discoveredDevices.iterator();
        while (iterator.hasNext()) {
            NsdServiceInfo device = iterator.next();

            if (device.getHost() == null || device.getPort() <= 0) {
                Log.d("NSD Condec", "Removing invalid or outdated device: " + device.getServiceName());
                iterator.remove();
            }
        }
        notifyDevicesChanged();
    }

    public List<NsdServiceInfo> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startDiscovery() {
        if (discoveryListener != null) {
            stopDiscovery();
        }
        initializeDiscoveryListener();
        nsdManager.discoverServices("_condec._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void stopDiscovery() {
        if (discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            discoveryListener = null;
        }
    }

    private void displayDetectedMessage(String device) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (dialogView != null) {

                windowManager.removeView(dialogView);
                dialogView = null;
            }

            createAndShowDialog(
                    "Sensitive Warning Detected",
                    "A sensitive warning was detected from " + device
            );
        });
    }

    private void displayMessageFromParent(String parent, String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (dialogView != null) {

                windowManager.removeView(dialogView);
                dialogView = null;
            }

            createAndShowDialog(
                    "Message from " + parent + " (Parent)",
                    message
            );
        });
    }

    private void createAndShowDialog(String title, String message) {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        dialogView = inflater.inflate(R.layout.layout_dialog_info, null);

        TextView txtViewTitle = dialogView.findViewById(R.id.dialog_title);
        TextView txtViewMessage = dialogView.findViewById(R.id.dialog_message);

        Button okButton = dialogView.findViewById(R.id.btnDialogBack);

        txtViewTitle.setText(title);
        txtViewMessage.setText(message);
        okButton.setText("Ok");
        okButton.setTextColor(getColor(R.color.white));
        okButton.setBackgroundColor(getColor(R.color.blue_main_background));

        okButton.setOnClickListener(v -> {
            if (dialogView != null) {
                windowManager.removeView(dialogView);
                dialogView = null;
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.dimAmount = 0.6f;
        params.gravity = Gravity.CENTER;

        windowManager.addView(dialogView, params);
    }

    private void startImageStream(Socket clientSocket, String senderDeviceName) {
        try {
            if (condecDetectionService == null) {
                Log.e("CondecParentalService", "Detection service is not bound, cannot start image stream.");
                return;
            }

            Log.d("CondecParentalService", "Requesting latest image from Detection Service...");
            byte[] imageData = condecDetectionService.getLatestImageData();

            if (imageData != null) {
                Log.d("CondecParentalService", "Image data retrieved, sending to " + senderDeviceName);
                OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write(imageData);
                outputStream.flush();
                Log.d("CondecParentalService", "Image data sent successfully to " + senderDeviceName);
            } else {
                Log.w("CondecParentalService", "No image data available from Detection Service.");
            }
        } catch (IOException e) {
            Log.e("CondecParentalService", "Error streaming image to " + senderDeviceName, e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Log.e("CondecParentalService", "Error closing client socket after image stream", e);
            }
        }
    }

    public void startImageRequestLoop(NsdServiceInfo deviceInfo, ParentalControlActivity parentalControlActivity) {

        imageRequestHandler = new Handler(Looper.getMainLooper());

        imageRequestRunnable = new Runnable() {
            @Override
            public void run() {
                if (isImageRequestActive) {
                    requestImageStream(deviceInfo);
                    imageRequestHandler.postDelayed(this, 200); // Repeat every 200 ms
                }
            }
        };

        // Start the request loop
        isImageRequestActive = true;
        imageRequestHandler.post(imageRequestRunnable);
    }

    public void stopImageRequestLoop() {
        isImageRequestActive = false;
        if (imageRequestHandler != null && imageRequestRunnable != null) {
            imageRequestHandler.removeCallbacks(imageRequestRunnable);
        }
    }

    private void requestImageStream(NsdServiceInfo deviceInfo) {
        executorService.execute(() -> {
            try (Socket socket = new Socket(deviceInfo.getHost(), deviceInfo.getPort());
                 OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {

                socket.setReceiveBufferSize(64 * 1024);
                socket.setSendBufferSize(64 * 1024);

                PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)), true);
                writer.println(localDeviceName + ":" + deviceInfo.getServiceName() + ":START_IMAGE_STREAM");
                Log.d("CondecParentalService", "Sent image stream request to " + deviceInfo.getServiceName());

                ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    imageBuffer.write(buffer, 0, bytesRead);
                }

                byte[] imageData = imageBuffer.toByteArray();
                if (imageData.length > 0) {
                    Log.d("CondecParentalService", "Image data received, size: " + imageData.length + " bytes.");
                    latestImage = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                    Intent imageUpdateIntent = new Intent("com.example.condec.IMAGE_UPDATE");
                    imageUpdateIntent.putExtra("image_data", imageData);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(imageUpdateIntent);
                } else {
                    Log.w("CondecParentalService", "No image data received from " + deviceInfo.getServiceName());
                }
            } catch (IOException e) {
                Log.e("CondecParentalService", "Error during image streaming request", e);
            }
        });
    }
}