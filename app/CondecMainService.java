import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

public class CondecMainService extends Service {

    private final IBinder binder = new LocalBinder();
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private List<String> discoveredDevices = new ArrayList<>();

    // Binder class to allow the activity to bind to the service
    public class LocalBinder extends Binder {
        CondecMainService getService() {
            return CondecMainService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        initializeDiscoveryListener();
        startDiscovery();
    }

    @Override
    public void onDestroy() {
        stopDiscovery();
        super.onDestroy();
    }

    // Initialize the mDNS discovery listener
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
                // Discovery started
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                // Discovery stopped
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                // When a service is found, add it to the list
                String serviceName = serviceInfo.getServiceName();
                if (!discoveredDevices.contains(serviceName)) {
                    discoveredDevices.add(serviceName);
                    notifyDeviceDiscovered(serviceName);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                // Handle service lost if necessary
            }
        };
    }

    // Start mDNS discovery
    public void startDiscovery() {
        nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    // Stop mDNS discovery
    public void stopDiscovery() {
        if (nsdManager != null && discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener);
        }
    }

    // Notify the activity that a new device was discovered
    public List<String> getDiscoveredDevices() {
        return discoveredDevices;
    }

    // This method can be used by the activity to listen to new devices discovered
    private void notifyDeviceDiscovered(String deviceName) {
        // Notify bound activities, if any (via broadcast, callback, etc.)
        // You could implement a broadcast receiver or a callback system here
    }
}