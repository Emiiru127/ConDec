package com.example.condec;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
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

                Log.d("Condec Parental", "Message length: " + parts.length);

                // Decide which method to call based on the number of parts in the message
                if (parts.length == 2) {
                    Log.d("Condec Parental", "Getting Service Statuses");
                    // Call the first handleClient method for data exchange
                    handleClientData(clientSocket, parts);
                } else if (parts.length >= 3) {
                    Log.d("Condec Parental", "Command Recieved");
                    // Call the second handleClientCommand method for command handling

                    if (!parts[2].equals("SLEEP_CONTROL") && parts.length == 3){

                        handleClientCommand(clientSocket, parts);

                    }
                    else if (parts[2].equals("SLEEP_CONTROL") && parts.length == 3){

                        handleClientSleepData(clientSocket, parts);

                    } else if (parts.length == 4) {

                        handleClientSleepCommand(clientSocket, parts);

                    }

                }
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
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
    private void reScheduleSleepTime(){

        Calendar startTimeCalendar = Calendar.getInstance();
        Calendar endTimeCalendar = Calendar.getInstance();

        PendingIntent startPendingIntent;
        PendingIntent stopPendingIntent;

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Load stored times or set defaults
        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        long startMillis = sharedPreferences.getLong("sleepStartTime", -1);
        long endMillis = sharedPreferences.getLong("sleepEndTime", -1);

        boolean useTimeOn = sharedPreferences.getBoolean("sleepUseTimeOn", false);
        boolean manualOn = sharedPreferences.getBoolean("sleepManualOn", false);

        long currentTime = System.currentTimeMillis();
        long startTime = startTimeCalendar.getTimeInMillis();
        long endTime = endTimeCalendar.getTimeInMillis();

        startTimeCalendar.setTimeInMillis(startMillis);
        endTimeCalendar.setTimeInMillis(endMillis);

        Intent startIntent = new Intent(this, CondecSleepService.class);
        startIntent.setAction("START_SERVICE");
        startPendingIntent = PendingIntent.getForegroundService(
                this, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, CondecSleepService.class);
        stopIntent.setAction("STOP_SERVICE");
        stopPendingIntent = PendingIntent.getForegroundService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Cancel existing alarms before rescheduling
        if (alarmManager != null) {
            alarmManager.cancel(startPendingIntent);
            alarmManager.cancel(stopPendingIntent);
        }


        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTimeCalendar.getTimeInMillis(), startPendingIntent);

        // Schedule the service stop
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeCalendar.getTimeInMillis(), stopPendingIntent);

        // Handle current state: should we start or stop the service now?
        if (currentTime >= startTime && currentTime < endTime) {
            startForegroundService(new Intent(this, CondecSleepService.class));
        } else {
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

        switch (command) {
            case "START_DETECTION":
                Intent requestIntent = new Intent(this, RequestDetectionPermission.class);
                requestIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(requestIntent);
                showToast("Remotely Started Detection Service");
                break;
            case "STOP_DETECTION":
                stopService(new Intent(this, CondecDetectionService.class));
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