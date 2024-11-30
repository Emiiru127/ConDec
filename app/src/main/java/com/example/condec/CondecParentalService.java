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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.condec.Classes.ParentalAppUsageInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CondecParentalService extends Service {

    private String localDeviceName;
    private final IBinder binder = new LocalBinder();
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.RegistrationListener registrationListener;
    private List<NsdServiceInfo> discoveredDevices = new ArrayList<>();
    private ServerSocket serverSocket;
    private int serverPort = 12345;
    private boolean isRunning = true;
    private final ExecutorService executorService = Executors.newCachedThreadPool(); // ExecutorService for background tasks

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public CondecParentalService getService() {
            return CondecParentalService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, createNotification());

        // Retrieve the custom device name from SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        localDeviceName = sharedPref.getString("deviceName", "My Device");

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("condecParentalServiceStatus", true);
        editor.apply();

        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        registerService();  // Register and start discovery

        // Start server to listen for incoming connections
        new Thread(this::startServer).start();

        // Listen for changes in the device name
        // Listen for changes in the device name

        sharedPref.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if (key.equals("deviceName")) {
                String newDeviceName = sharedPreferences.getString("deviceName", "My Device");
                if (!newDeviceName.equals(localDeviceName)) {
                    localDeviceName = newDeviceName;
                    restartService(); // Restart the service to apply new device name
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        executorService.shutdown(); // Shut down the ExecutorService
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopDiscovery();

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
                .setContentTitle("Device Discovery Running")
                .setContentText("Searching for devices on the local network")
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
        unregisterService(); // Unregister current service
        stopDiscovery();     // Stop discovering services
        // Add delay here if necessary to ensure full reset
        registerService();   // Register with the new name
        refreshDiscoveredDevices(); // Refresh and notify about the updated list
    }

    private void unregisterService() {
        if (registrationListener != null) {
            nsdManager.unregisterService(registrationListener);
            registrationListener = null; // Clear reference to avoid reuse
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
                executorService.execute(() -> handleClient(clientSocket)); // Use executor for client handling
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

            // Read the incoming message from Device A
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = in.readLine();

            if (message != null) {
                String[] parts = message.split(":");

                Log.d("CondecParentalService", "Message length: " + parts.length);

                // Decide which method to call based on the number of parts in the message
                if (parts.length == 2) {
                    Log.d("CondecParentalService", "Getting Service Statuses");
                    // Call the first handleClient method for data exchange
                    handleClientData(clientSocket, parts);
                } else if (parts.length >= 3) {
                    Log.d("CondecParentalService", "Command Recieved");
                    // Call the second handleClientCommand method for command handling

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
        calendar.set(Calendar.HOUR_OF_DAY, 0); // Start of the current day
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

        // Now send this app usage data to the target device
        return appUsageList;
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        return ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) && ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0);
    }

    private boolean isAllowedSystemApp(String packageName) {
        return packageName.equals("com.android.vending") // Play Store
                || packageName.equals("com.google.android.youtube") // YouTube
                || packageName.equals("com.android.chrome"); // Chrome
    }

    private void handleRequestAppUsageData(Socket clientSocket) {
            try {
                // Assuming you already have a socket connection to the target device
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                List<ParentalAppUsageInfo> appUsageList = collectAppUsageData();

                // Send each app's data
                for (ParentalAppUsageInfo parentalAppUsageInfo : appUsageList) {
                    String dataToSend = String.format("%s|%d|%d|%s",
                            parentalAppUsageInfo.getAppName(),
                            parentalAppUsageInfo.getUsageTime(),
                            parentalAppUsageInfo.getLastTimeUsed(),
                            encodeIconToBase64(parentalAppUsageInfo.getAppIcon())  // Convert icon to Base64 string
                    );
                    Log.d("CondecParentalService", "App Data Sent: " + dataToSend);
                    out.println(dataToSend);
                }

                out.println("END_OF_APP_USAGE_DATA");  // Mark the end of the data
                out.flush();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    // Helper method to convert Drawable to Base64 string
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

                // Send the request for app usage data
                out.println(localDeviceName + ":" + deviceInfo.getServiceName() + ":" + "APP_USAGE_DATA");
                Log.d("CondecParentalService", "REQUESTING APP USAGE DATA");
                // Listen for data from the target device
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                List<String> receivedData = new ArrayList<>();
                String line;
                String temp = "";
                while ((line = in.readLine()) != null) {
                    Log.d("CondecParentalService", "App Data Received: " + line);
                    temp += line;
                    if (line.equals("END_OF_APP_USAGE_DATA")) {
                        break;  // End of data marker
                    }
                    if(line.isEmpty()){

                        receivedData.add(temp);
                        temp = "";

                    }

                }


                for (String data : receivedData) {
                    Log.d("CondecParentalService", data);
                }

                // Handle the received app usage data (e.g., start a new activity)
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

        // Sort the appUsageList by lastTimeUsed in descending order
        appUsageList.sort((app1, app2) -> Long.compare(app2.getLastTimeUsed(), app1.getLastTimeUsed()));

        // Proceed to use appUsageList, e.g., pass it to the activity or process it
        Log.d("CondecParentalService", "App usage data processed: " + appUsageList.size() + " apps.");

        // Pass the sorted list to the activity (for example)
        Intent intent = new Intent(parentalControlActivity, ParentalAppUsageActivity.class);
        intent.putExtra("deviceName", parentalControlActivity.getCurrentDeviceTarget());
        intent.putParcelableArrayListExtra("appUsageList", (ArrayList<ParentalAppUsageInfo>) appUsageList);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // Helper method to decode Base64 string to Drawable
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

                Log.d("Condec Parental", "Received Request from: " + senderDeviceName);

                Log.d("Condec Parental", "sending deviceName: " + deviceName);
                Log.d("Condec Parental", "sending isDetecting: " + isDetecting);
                Log.d("Condec Parental", "sending isAppBlocking: " + isAppBlocking);
                Log.d("Condec Parental", "sending isWebsiteBlocking: " + isWebsiteBlocking);
                Log.d("Condec Parental", "sending isSleeping: " + isSleeping);

                String[] dataToSend = {
                        "DeviceName:" + deviceName,
                        "Detection:" + isDetecting,
                        "BlockingApp:" + isAppBlocking,
                        "BlockingWebsite:" + isWebsiteBlocking,
                        "Sleeping:" + isSleeping,
                };  // Example array of strings
                for (String data : dataToSend) {
                    out.println(data);  // Send each data item
                }
                out.println("END_OF_DATA");  // Send a marker to indicate the end of the data
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
                };  // Example array of strings
                for (String data : dataToSend) {
                    out.println(data);  // Send each data item
                }
                out.println("END_OF_SLEEP_DATA");  // Send a marker to indicate the end of the data
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
                handleSleepCommand(command, parts[3]); // Handle the received command
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
            if (localDeviceName.equals(targetDeviceName)) {
                Log.d("Condec Parental", "Authentication: Accepted " + localDeviceName + " and " + targetDeviceName + " are equal");
                handleCommand(command); // Handle the received command
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
                showToast("Unknown Sleep Command: " + command);
                break;
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void reScheduleSleepTime() {

        Calendar startTimeCalendar = Calendar.getInstance();
        Calendar endTimeCalendar = Calendar.getInstance();

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Load stored times
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

        // Cancel existing alarms before rescheduling
        if (alarmManager != null) {
            alarmManager.cancel(startPendingIntent);
            alarmManager.cancel(stopPendingIntent);
        }

        // Schedule start and stop services
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, startPendingIntent);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, stopPendingIntent);

        // Check the current time
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

        // Schedule the service to start at the same time every day
        Intent startIntent = new Intent(this, CondecSleepService.class);
        startPendingIntent = PendingIntent.getService(this, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Schedule the service to stop at the same time every day
        Intent stopIntent = new Intent(this, CondecSleepService.class);
        stopIntent.setAction("STOP_SERVICE");
        stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        // Cancel the scheduled service
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

        switch (command) {
            case "START_DETECTION":
                Intent requestIntent = new Intent(this, RequestDetectionPermission.class);
                requestIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(requestIntent);

                editor.putBoolean("isDetectionServiceManuallyOff", true);
                editor.apply();

                Intent broadcastDetectionOnIntent = new Intent("com.example.condec.UPDATE_SECURITY_FLAGS_DETECTION_OFF");
                sendBroadcast(broadcastDetectionOnIntent);
                showToast("Remotely Started Detection Service");
                break;
            case "STOP_DETECTION":
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
            default:
                showToast("Unknown Command: " + command);
                break;
        }
    }

    private void handleIncomingMessage(String message) {
        String[] parts = message.split(":");
        Log.d("CondecServer", "Handle Has MESSAGE: " + message);
        Log.d("CondecServer", "parts: " + parts.length);
        if (parts.length == 2) {
            String senderDeviceName = parts[0];
            String targetDeviceName = parts[1];

            Log.d("CondecServer", "sender: " + senderDeviceName);
            Log.d("CondecServer", "target: " + targetDeviceName);
            Log.d("CondecServer", "local: " + localDeviceName);
            Log.d("CondecServer", "judgement: " + localDeviceName.equals(targetDeviceName));
            if (localDeviceName.equals(targetDeviceName)) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(CondecParentalService.this,
                                senderDeviceName + " was calling this device. This device successfully received the call.",
                                Toast.LENGTH_LONG).show()
                );
            }
        }
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

                // Now listen for data from Device B
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                List<String> receivedData = new ArrayList<>();
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("END_OF_DATA")) {
                        break;  // End of data marker
                    }
                    receivedData.add(line);
                }

                Log.d("Condec Parental", "DATA RECEIVED From : " + (receivedData.get(0)).split(":")[1]);

                for (String data : receivedData){

                    Log.d("Condec Parental", data);

                }

                // Handle the received data (e.g., start a new activity)
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

                // Now listen for data from Device B
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

                // Handle the received data (e.g., start a new activity)
                handleReceivedSleepData(receivedData, parentalControlActivity);

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleReceivedData(List<String> data) {
        // Example: Start a new activity and pass the data
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
        stopDiscovery(); // Stop any ongoing discovery
        startDiscovery(); // Restart discovery with the latest settings
        notifyDevicesChanged(); // Broadcast the updated device list
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
            // Remove any existing entries with the same service name but different address/port
            discoveredDevices.removeIf(device -> device.getServiceName().equals(resolvedServiceInfo.getServiceName()));

            // Add the new entry
            discoveredDevices.add(resolvedServiceInfo);
            Log.d("NSD Condec", "Added resolved device to the list.");
        } else {
            Log.e("NSD Condec", "Resolved service has invalid host or port. Device ignored.");
        }

        cleanUpDiscoveredDevices(); // Clean up invalid entries
    }

    private void cleanUpDiscoveredDevices() {
        Iterator<NsdServiceInfo> iterator = discoveredDevices.iterator();
        while (iterator.hasNext()) {
            NsdServiceInfo device = iterator.next();
            // Check for invalid devices or outdated device names
            if (device.getHost() == null || device.getPort() <= 0) {
                Log.d("NSD Condec", "Removing invalid or outdated device: " + device.getServiceName());
                iterator.remove();
            }
        }
        notifyDevicesChanged(); // Notify the UI to update with the cleaned list
    }

    public List<NsdServiceInfo> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices); // Ensure the list is correctly populated
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
            stopDiscovery(); // Stop any ongoing discovery
        }
        initializeDiscoveryListener(); // Reinitialize listener
        nsdManager.discoverServices("_condec._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void stopDiscovery() {
        if (discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            discoveryListener = null; // Clear reference to avoid reuse
        }
    }
}