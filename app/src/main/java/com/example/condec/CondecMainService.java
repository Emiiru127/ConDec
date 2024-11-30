package com.example.condec;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CondecMainService extends Service {

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
        public CondecMainService getService() {
            return CondecMainService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, createNotification());

        // Retrieve the custom device name from SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        localDeviceName = sharedPref.getString("deviceName", "My Device");

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
        // Unregister existing service and start discovery again with new name
        Log.w("NSD Condec", "Restart Service");
        unregisterService();
        stopDiscovery();
        registerService();
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
        serviceInfo.setServiceType("_http._tcp.");
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
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = in.readLine();
            Log.d("CondecServer", "Checking MESSAGE: " + message);
            if (message != null) {
                Log.d("CondecServer", "Has MESSAGE: " + message);
                handleIncomingMessage(message);
            }
            Log.d("CondecServer", "NO MESSAGE");
            clientSocket.close();
        } catch (IOException e) {
            Log.d("CondecServer", "ERROR: " + e);
            e.printStackTrace();
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
                        Toast.makeText(CondecMainService.this,
                                senderDeviceName + " was calling this device. This device successfully received the call.",
                                Toast.LENGTH_LONG).show()
                );
            }
        }
    }

    public void sendCallToDevice(NsdServiceInfo deviceInfo) {
        if (deviceInfo == null) {
            showToast("Device info is null");
            return;
        }

        Log.d("CondecSender", "DEBUGGING:");
        debugDiscoveredDevices();
        Log.d("CondecSender", "DEBUGGING:");

        Log.d("CondecSender", "deviceInfo: " + deviceInfo);
        Log.d("CondecSender", "deviceInfo: " + deviceInfo.getHost());
        Log.d("CondecSender", "port: " + deviceInfo.getPort());

        InetAddress host = deviceInfo.getHost();
        int port = deviceInfo.getPort();

        if (host == null || port <= 0) {
            showToast("Invalid device info. Host or port is null/invalid.");
            return;
        }

        String hostAddress = host.getHostAddress();

        executorService.execute(() -> {
            try {
                Socket socket = new Socket(hostAddress, port);
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println(localDeviceName + ":" + deviceInfo.getServiceName());
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                // Use Handler to post a Toast to the main thread
                new Handler(Looper.getMainLooper()).post(() ->
                        showToast("Failed to send call to device: " + e.getMessage())
                );
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
            if (device.getHost() == null || device.getPort() <= 0) {
                Log.d("NSD Condec", "Removing invalid device: " + device.getServiceName());
                iterator.remove();
            }
        }
        notifyDevicesChanged(); // Notify about the updated list
    }

    public List<NsdServiceInfo> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices); // Ensure the list is correctly populated
    }

    private void startDiscovery() {
        if (discoveryListener != null) {
            stopDiscovery(); // Stop any ongoing discovery
        }
        initializeDiscoveryListener(); // Reinitialize listener
        nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void stopDiscovery() {
        if (discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            discoveryListener = null; // Clear reference to avoid reuse
        }
    }
}