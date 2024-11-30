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
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.condec.Classes.DeviceAdapter;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView rvDevices;
    private DeviceAdapter deviceAdapter;
    private List<String> deviceList = new ArrayList<>();
    private CondecMainService condecMainService;
    private boolean isBound = false;

    // ServiceConnection for interacting with the CondecMainService
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CondecMainService.LocalBinder binder = (CondecMainService.LocalBinder) service;
            condecMainService = binder.getService();
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
            // Refresh the device list
            updateDeviceList();
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

        // Bind to the service
        Intent serviceIntent = new Intent(this, CondecMainService.class);
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
        if (condecMainService != null) {
            NsdServiceInfo targetDeviceInfo = condecMainService.getDeviceInfoByName(deviceName);
            if (targetDeviceInfo != null) {
                condecMainService.sendCallToDevice(targetDeviceInfo); // Notify the service to send a call to the tapped device
            } else {
                Toast.makeText(this, "Device not found: " + deviceName, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        // Handle back button
        if (view.getId() == R.id.btnSettingsBack) {
            Intent intent = new Intent(SettingsActivity.this, MainMenuActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // Update device list by fetching it from the service
    private void updateDeviceList() {
        if (condecMainService != null) {
            deviceList.clear();
            List<NsdServiceInfo> discoveredDevices = condecMainService.getDiscoveredDevices();
            for (NsdServiceInfo deviceInfo : discoveredDevices) {
                deviceList.add(deviceInfo.getServiceName()); // Add the device name to the list
            }
            deviceAdapter.notifyDataSetChanged();
        }
    }
}