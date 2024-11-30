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
import java.util.ArrayList;
import java.util.List;

public class CondecVPNService extends VpnService {

    private static final String TAG = "Condec Vpn Service";

    private boolean isVpnRunning = false;
    private boolean stopVpnThread = false; // Control variable for VPN thread

    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;

    // List of domains to block
    private List<String> blockedDomains = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecVPNServiceStatus", true);
        editor.apply();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isVpnRunning) {
            Log.d(TAG, "VPN is already running, not starting again");
            return START_STICKY;
        }

        fetchBlockedDomainsAndStartVpn();
        return START_STICKY;
    }

    private void fetchBlockedDomainsAndStartVpn() {
        // Use AsyncTask to run the database query on a background thread
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voids) {
                BlockedURLRepository blockedURLRepository = new BlockedURLRepository(getApplication());
                return blockedURLRepository.getAllBlockedUrlsSync(); // Synchronous call
            }

            @Override
            protected void onPostExecute(List<String> urls) {
                if (urls != null) {
                    blockedDomains.clear();
                    blockedDomains.addAll(urls);

                    Log.d(TAG, "Blocked Domains Updated:");
                    for (String url : blockedDomains) {
                        Log.d(TAG, url);
                    }

                    // Start VPN service after domains are updated
                    startVpnService();
                }
            }
        }.execute();
    }

    private void startVpnService() {
        if (isVpnRunning) {
            Log.d(TAG, "VPN is already running, skipping start");
            return; // Don't start the VPN again if it's already running
        }

        if (vpnThread != null) {
            vpnThread.interrupt();
        }
        stopVpnThread = false; // Reset the control flag
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
                        if (isValidIPv4Address(ipAddress)) {
                            Log.i(TAG, "Blocking IP: " + ipAddress);
                            builder.addRoute(ipAddress, 32);
                        } else {
                            Log.w(TAG, "Invalid IP address format: " + ipAddress);
                        }
                    }

                    vpnInterface = builder.establish();

                    FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
                    FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());

                    byte[] packet = new byte[32767];
                    int length;
                    while (!stopVpnThread && (length = in.read(packet)) > 0) {
                        out.write(packet, 0, length);
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Error in VPN thread", e);
                    stopSelf();
                }
            }
        });

        vpnThread.start();
        isVpnRunning = true; // Set the flag to true when VPN starts
    }

    @Override
    public void onDestroy() {

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("condecVPNServiceStatus", false);
        editor.apply();

        // Properly clean up and stop the VPN
        Log.d(TAG, "VPN is On Destroy");
        stopVpn();

        // Stop the VPN interface
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "VPN is On Destroy LAst");

        super.onDestroy();
    }

    private void stopVpn() {
        if (!isVpnRunning) {
            Log.d(TAG, "VPN is already stopped");
            return;
        }

        stopVpnThread = true; // Set the flag to stop the thread

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
                Log.d(TAG, "VPN interface closed");
            } catch (IOException e) {
                Log.e(TAG, "Error closing VPN interface", e);
            }
            vpnInterface = null;
        }

        if (vpnThread != null) {
            try {
                vpnThread.join(); // Wait for the thread to finish
            } catch (InterruptedException e) {
                Log.e(TAG, "Error joining VPN thread", e);
            }
            vpnThread = null;
        }

        stopForeground(true);
        stopSelf();

        isVpnRunning = false; // Update the flag
        Log.d(TAG, "VPN service stopped");
    }

    // Method to resolve the domain to its IP addresses
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

    // Validate if the IP address is in valid IPv4 format
    private boolean isValidIPv4Address(String ip) {
        return ip != null && ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$");
    }

    // Validate if the IP address is in valid IPv6 format
    private boolean isValidIPv6Address(String ip) {
        return ip != null && ip.matches("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    }

}