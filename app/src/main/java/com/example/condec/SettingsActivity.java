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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.condec.Classes.DeviceAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnReadTermsConditions;
    private Button btnChangePin;
    private Button btnChangeBackup;
    private Button btnAboutCondec;
    private Button btnParentMode;

    private TextView txtviewAvailableDevices;
    private RecyclerView recycleViewDevices;

    private Switch switchParentMode;
    private DeviceAdapter deviceAdapter;
    private List<String> deviceList = new ArrayList<>();
    private CondecParentalService condecParentalService;
    private boolean isBound = false;
    private SharedPreferences condecPreferences;

    private Map<String, String> deviceNameMap = new HashMap<>();

    private Handler discoveryHandler = new Handler();
    private Runnable discoveryRunnable;
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CondecParentalService.LocalBinder binder = (CondecParentalService.LocalBinder) service;
            condecParentalService = binder.getService();
            isBound = true;

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
            updateDeviceList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);

        this.btnReadTermsConditions = findViewById(R.id.btnReadTermsConditions);
        this.btnChangePin = findViewById(R.id.btnChangePin);
        this.btnChangeBackup = findViewById(R.id.btnChangeBackup);
        this.btnAboutCondec = findViewById(R.id.btnAboutCondec);

        switchParentMode = findViewById(R.id.switchParentMode);
        btnParentMode = findViewById(R.id.btnParentMode);
        txtviewAvailableDevices = findViewById(R.id.txtviewAvailableDevices);
        recycleViewDevices = findViewById(R.id.recycleViewDevices);
        deviceAdapter = new DeviceAdapter(deviceList, this::onDeviceClicked);
        recycleViewDevices.setAdapter(deviceAdapter);
        recycleViewDevices.setLayoutManager(new LinearLayoutManager(this));

        condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        boolean isParentModeOn = condecPreferences.getBoolean("parentModeState", false);
        switchParentMode.setChecked(isParentModeOn);
        toggleParentModeUI(isParentModeOn);

        Intent serviceIntent = new Intent(this, CondecParentalService.class);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        ImageButton btnBack = findViewById(R.id.btnSettingsBack);
        btnBack.setOnClickListener(this);

        btnReadTermsConditions.setOnClickListener(this);
        btnChangePin.setOnClickListener(this);
        btnChangeBackup.setOnClickListener(this);
        btnAboutCondec.setOnClickListener(this);

        switchParentMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleParentModeUI(isChecked);
            SharedPreferences.Editor editor = condecPreferences.edit();
            editor.putBoolean("parentModeState", isChecked);
            editor.apply();
        });

        btnParentMode.setOnClickListener(view -> {
            boolean isChecked = !switchParentMode.isChecked();
            switchParentMode.setChecked(isChecked);

        });
        startDiscoveryRefresh();
    }

    private void toggleParentModeUI(boolean isChecked) {
        if (isChecked) {

            restartService();

            txtviewAvailableDevices.setVisibility(View.VISIBLE);
            recycleViewDevices.setVisibility(View.VISIBLE);
            switchParentMode.setText("ON");
            updateDeviceList();
        } else {
            txtviewAvailableDevices.setVisibility(View.GONE);
            recycleViewDevices.setVisibility(View.GONE);
            switchParentMode.setText("OFF");
            deviceList.clear();
            deviceAdapter.notifyDataSetChanged();
        }
    }

    private void restartService() {

        Intent stopServiceIntent = new Intent(this, CondecParentalService.class);
        stopService(stopServiceIntent);

        Intent startServiceIntent = new Intent(this, CondecParentalService.class);
        startService(startServiceIntent);

        bindService(startServiceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

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

    private void goBack(){
        Intent intent = new Intent(SettingsActivity.this, MainMenuActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToTermsConditions(){
        Intent intent = new Intent(SettingsActivity.this, ReadTermsConditionsActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToChangePin(){
        Intent intent = new Intent(SettingsActivity.this, CreatePinActivity.class);
        intent.putExtra("forChanging", true);
        startActivity(intent);
        finish();
    }

    private void goToChangeQuestion(){
        Intent intent = new Intent(SettingsActivity.this, CreateQuestionActivity.class);
        intent.putExtra("forChanging", true);
        startActivity(intent);
        finish();
    }

    private void goToAboutCondec(){
        Intent intent = new Intent(SettingsActivity.this, AboutCondecActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnSettingsBack) {
            goBack();
        }
        if (view.getId() == R.id.btnReadTermsConditions) {
            goToTermsConditions();
        }
        if (view.getId() == R.id.btnChangePin) {
            goToChangePin();
        }
        if (view.getId() == R.id.btnChangeBackup) {
            goToChangeQuestion();
        }
        if (view.getId() == R.id.btnAboutCondec) {
            goToAboutCondec();
        }
    }

    private void updateDeviceList() {
        if (condecParentalService != null) {
            List<NsdServiceInfo> discoveredDevices = condecParentalService.getDiscoveredDevices();

            deviceList.clear();

            for (NsdServiceInfo device : discoveredDevices) {
                String deviceName = device.getServiceName();
                String host = device.getHost() != null ? device.getHost().getHostAddress() : null;
                int port = device.getPort();

                if (host != null && port != 0 && !isSelfDevice(device)) {

                    String oldDeviceName = deviceNameMap.get(host);

                    if (oldDeviceName != null && !oldDeviceName.equals(deviceName)) {
                        removeOldDeviceFromList(oldDeviceName);
                    }

                    deviceNameMap.put(host, deviceName);
                    deviceList.add(deviceName);
                } else {
                    Log.d("CondecSender", "Skipping invalid device: " + deviceName + " (host: " + host + ", port: " + port + ")");
                }
            }
            Log.d("CondecSender", "Devices:");
            deviceAdapter.notifyDataSetChanged();
            for(String device : deviceList){

                Log.d("CondecSender", device);

            }
        }
    }

    private void startDiscoveryRefresh() {
        discoveryRunnable = new Runnable() {
            @Override
            public void run() {
                if (condecParentalService != null) {
                    Log.d("SettingsActivity", "Refreshing device discovery...");
                    condecParentalService.refreshDiscoveredDevices(); // This triggers discovery
                }
                discoveryHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        discoveryHandler.post(discoveryRunnable);
    }

    private void stopDiscoveryRefresh() {
        if (discoveryRunnable != null) {
            discoveryHandler.removeCallbacks(discoveryRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.example.condec.DEVICE_DISCOVERED");
        registerReceiver(deviceDiscoveryReceiver, filter);

        IntentFilter listChangeFilter = new IntentFilter("com.example.condec.DEVICE_LIST_CHANGED");
        registerReceiver(deviceListChangedReceiver, listChangeFilter);

        startDiscoveryRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(deviceDiscoveryReceiver);
        unregisterReceiver(deviceListChangedReceiver);

        stopDiscoveryRefresh();
    }

    public void onBackPressed() {

        if (false){

            super.onBackPressed();

        }
    }

    private void removeOldDeviceFromList(String oldDeviceName) {
        if (deviceList.contains(oldDeviceName)) {
            deviceList.remove(oldDeviceName);
        }
    }

    private boolean isSelfDevice(NsdServiceInfo device) {
        String selfDeviceName = getSelfDeviceName();
        return selfDeviceName.equals(device.getServiceName());
    }

    private String getSelfDeviceName() {
        return condecPreferences.getString("deviceName", "My Device");
    }
}