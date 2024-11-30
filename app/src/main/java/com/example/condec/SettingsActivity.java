package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.condec.Classes.DeviceAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView rvDevices;
    private DeviceAdapter deviceAdapter;
    private List<String> deviceList = new ArrayList<>();
    private CondecParentalService condecParentalService;
    private boolean isBound = false;
    private SharedPreferences condecPreferences;

    // Map to track old names to new names
    private Map<String, String> deviceNameMap = new HashMap<>();

    // ServiceConnection for interacting with the CondecMainService
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CondecParentalService.LocalBinder binder = (CondecParentalService.LocalBinder) service;
            condecParentalService = binder.getService();
            isBound = true;

            // Update device list on service connection
            updateDeviceList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private final BroadcastReceiver deviceDiscoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String deviceName = intent.getStringExtra("deviceName");
            if (deviceName != null && !deviceList.contains(deviceName)) {
                deviceList.add(deviceName);
                deviceAdapter.notifyDataSetChanged();
            }
        }
    };

    private final BroadcastReceiver deviceListChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDeviceList(); // Refresh the list when the broadcast is received
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);

        rvDevices = findViewById(R.id.rv_devices);
        deviceAdapter = new DeviceAdapter(deviceList, this::onDeviceClicked);
        rvDevices.setAdapter(deviceAdapter);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));

        condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        // Bind to the service
        Intent serviceIntent = new Intent(this, CondecParentalService.class);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        // Set up click listener for the back button
        ImageButton btnBack = findViewById(R.id.btnSettingsBack);
        btnBack.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.example.condec.DEVICE_DISCOVERED");
        registerReceiver(deviceDiscoveryReceiver, filter);

        IntentFilter listChangeFilter = new IntentFilter("com.example.condec.DEVICE_LIST_CHANGED");
        registerReceiver(deviceListChangedReceiver, listChangeFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(deviceDiscoveryReceiver);
        unregisterReceiver(deviceListChangedReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    // Handle device click
    private void onDeviceClicked(String deviceName) {
        if (condecParentalService != null) {
            NsdServiceInfo targetDeviceInfo = condecParentalService.getDeviceInfoByName(deviceName);
            if (targetDeviceInfo != null) {
                condecParentalService.sendCallToDevice(targetDeviceInfo);
            } else {
                Toast.makeText(this, "Device not found: " + deviceName, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnSettingsBack) {
            Intent intent = new Intent(SettingsActivity.this, MainMenuActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // Update device list by fetching it from the service and handling renaming
    private void updateDeviceList() {
        if (condecParentalService != null) {
            List<NsdServiceInfo> discoveredDevices = condecParentalService.getDiscoveredDevices();

            // Clear the existing list before updating
            deviceList.clear();

            // Iterate through the discovered devices
            for (NsdServiceInfo device : discoveredDevices) {
                String deviceName = device.getServiceName();
                String host = device.getHost() != null ? device.getHost().getHostAddress() : null;
                int port = device.getPort();

                // Ensure valid devices and not self-device
                if (host != null && port != 0 && !isSelfDevice(device)) {
                    // Check if the device name has changed
                    String oldDeviceName = deviceNameMap.get(host);  // Assuming host is used as a unique key

                    if (oldDeviceName != null && !oldDeviceName.equals(deviceName)) {
                        removeOldDeviceFromList(oldDeviceName);
                    }

                    // Update the map and the list
                    deviceNameMap.put(host, deviceName);
                    deviceList.add(deviceName);
                } else {
                    Log.d("CondecSender", "Skipping invalid device: " + deviceName + " (host: " + host + ", port: " + port + ")");
                }
            }
            Log.d("CondecSender", "Devices:");
            // Notify the RecyclerView adapter to refresh the list
            deviceAdapter.notifyDataSetChanged();
            for(String device : deviceList){

                Log.d("CondecSender", device);

            }
        }
    }

    // Method to remove the old device from the list
    private void removeOldDeviceFromList(String oldDeviceName) {
        if (deviceList.contains(oldDeviceName)) {
            deviceList.remove(oldDeviceName);
        }
    }

    // Helper method to check if the device is the current device
    private boolean isSelfDevice(NsdServiceInfo device) {
        String selfDeviceName = getSelfDeviceName();
        return selfDeviceName.equals(device.getServiceName());
    }

    private String getSelfDeviceName() {
        return condecPreferences.getString("deviceName", "My Device");
    }
}