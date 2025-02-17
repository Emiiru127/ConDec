package com.example.condec;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.condec.Database.BlockedURLRepository;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CondecVPNService extends VpnService {

    private static final String TAG = "Condec Vpn Service";
    public static final String ACTION_STOP_VPN = "com.example.condec.STOP_VPN";
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;

    // List of domains to block
    private List<String> blockedDomains = new ArrayList<>();

    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecVPNServiceStatus", true);
        editor.apply();

        executorService = Executors.newSingleThreadExecutor();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_STOP_VPN.equals(intent.getAction())) {
            stopVpn();
            return START_NOT_STICKY;
        }

        if (vpnThread != null && vpnThread.isAlive()) {
            vpnThread.interrupt();
        }

        fetchBlockedDomainsAndStartVpn();
        return START_STICKY;
    }

    private void fetchBlockedDomainsAndStartVpn() {

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                BlockedURLRepository blockedURLRepository = new BlockedURLRepository(getApplication());
                List<String> urls = blockedURLRepository.getAllBlockedUrlsSync();

                if (urls != null) {
                    blockedDomains.clear();
                    blockedDomains.addAll(urls);

                    Log.d(TAG, "Blocked Domains Updated:");
                    for (String url : blockedDomains) {
                        Log.d(TAG, url);
                    }

                    startVpnService();
                }
            }
        });
    }

    private void startVpnService() {
        vpnThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<InetAddress> blockedIps = new ArrayList<>();
                    for (String domain : blockedDomains) {
                        blockedIps.addAll(resolveDomainToIps(domain));
                    }

                    if (blockedIps.isEmpty()) {
                        Log.e(TAG, "No IPs found for domains");
                        stopSelf();
                        return;
                    }

                    Builder builder = new Builder();
                    builder.addAddress("10.0.0.2", 24);

                    for (InetAddress ip : blockedIps) {
                        String ipAddress = ip.getHostAddress();
                        try {

                            if (isValidIPv4Address(ipAddress)) {
                                Log.i(TAG, "Blocking IP: " + ipAddress);
                                builder.addRoute(ipAddress, 32);
                            } else {
                                Log.w(TAG, "Invalid IP address format: " + ipAddress);
                            }

                        }catch (Exception e){

                            Log.w(TAG, "ERROR: Invalid IP address format: " + ipAddress);

                        }
                    }

                    vpnInterface = builder.establish();
                    Log.d(TAG, "VPN Interface established");

                    FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
                    FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());

                    byte[] packet = new byte[32767];
                    int length;
                    while ((length = in.read(packet)) > 0) {
                        out.write(packet, 0, length);
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Error in VPN thread", e);
                    stopSelf();
                }
            }
        });
        vpnThread.start();

    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "VPN is On Destroy");
        stopVpn();
        Log.d(TAG, "VPN is On Destroy LAst");

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecVPNServiceStatus", false);
        editor.apply();

        super.onDestroy();
    }

    private void stopVpn() {

        if (vpnThread != null && vpnThread.isAlive()) {
            vpnThread.interrupt();
            try {
                vpnThread.join(); // Wait for the thread to finish
                Log.d(TAG, "VPN thread stopped successfully");
            } catch (InterruptedException e) {
                Log.e(TAG, "Error waiting for VPN thread to stop", e);
            }
        }

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
                Log.d(TAG, "VPN interface closed successfully");
            } catch (IOException e) {
                Log.e(TAG, "Error closing VPN interface", e);
            }
        }

        vpnInterface = null;
        vpnThread = null;

        stopSelf();

        Log.d(TAG, "VPN service stopped");
    }

    public static String extractDomain(String input) {
        try {

            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                input = "http://" + input;
            }

            URL url = new URL(input);

            String host = url.getHost();

            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            if (!host.contains(".")) {
                throw new IllegalArgumentException("Input is not a valid domain or URL.");
            }

            return host;
        } catch (Exception e) {

            System.err.println("Invalid input: " + input);
            return null;
        }
    }


    private List<InetAddress> resolveDomainToIps(String domain) {
        List<InetAddress> ipAddresses = new ArrayList<>();
        try {
            InetAddress[] addresses = InetAddress.getAllByName(domain);
            for (InetAddress address : addresses) {
                ipAddresses.add(address);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resolving domain: " + domain, e);
        }
        return ipAddresses;
    }

    private boolean isValidIPv4Address(String ip) {
        return ip != null && ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$");
    }

    private boolean isValidIPv6Address(String ip) {
        return ip != null && ip.matches("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    }

}