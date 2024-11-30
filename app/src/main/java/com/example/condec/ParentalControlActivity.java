package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

public class ParentalControlActivity extends AppCompatActivity implements View .OnClickListener{

    private ImageButton btnBackParental;
    private TextView txtViewTargetDeviceName;

    private Switch switchDetection;
    private Switch switchAppBlocking;
    private Switch switchWebsiteBlocking;
    private CardView cardViewSleepMode;
    private CondecParentalService parentalService;
    private boolean isBound = false;

    String deviceName;
    NsdServiceInfo deviceInfo;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CondecParentalService.LocalBinder binder = (CondecParentalService.LocalBinder) service;
            parentalService = binder.getService();
            deviceInfo = parentalService.getDeviceInfoByName(deviceName);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_parental_controls);

        this.btnBackParental = findViewById(R.id.btnBackParental);
        this.btnBackParental.setOnClickListener(this);

        this.txtViewTargetDeviceName = findViewById(R.id.txtTargetDeviceName);

        this.switchDetection = findViewById(R.id.switchWarning);
        this.switchAppBlocking = findViewById(R.id.switchBlockingApps);
        this.switchWebsiteBlocking = findViewById(R.id.switchBlockingWebsites);
        this.cardViewSleepMode = findViewById(R.id.cardViewSleepMode);

        ArrayList<String> receivedData = getIntent().getStringArrayListExtra("receivedData");

        Log.d("Condec Parental", "Parental Control DATA RECEIVED:");

        for (String data : receivedData){

            Log.d("Condec Parental", data);

        }

        Intent intent = new Intent(this, CondecParentalService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        deviceName = (receivedData.get(0)).split(":")[1];
        boolean isDetecting = Boolean.parseBoolean((receivedData.get(1)).split(":")[1]);
        boolean isAppBlocking = Boolean.parseBoolean((receivedData.get(2)).split(":")[1]);
        boolean isWebsiteBlocking = Boolean.parseBoolean((receivedData.get(3)).split(":")[1]);
        boolean isSleeping = Boolean.parseBoolean((receivedData.get(4)).split(":")[1]);



        this.txtViewTargetDeviceName.setText(deviceName);

        this.switchDetection.setChecked(isDetecting);
        this.switchAppBlocking.setChecked(isAppBlocking);
        this.switchWebsiteBlocking.setChecked(isWebsiteBlocking);
        //this.switchSleep.setChecked(isSleeping);

        switchDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            if (isChecked) {
                // Start the service
                parentalService.sendCommandToDevice(deviceInfo, "START_DETECTION");
            } else {
                parentalService.sendCommandToDevice(deviceInfo, "STOP_DETECTION");
            }
        });
        switchAppBlocking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            if (isChecked) {
                // Start the service
                parentalService.sendCommandToDevice(deviceInfo, "START_APP_BLOCKING");
            } else {
                parentalService.sendCommandToDevice(deviceInfo, "STOP_APP_BLOCKING");
            }
        });
        switchWebsiteBlocking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            if (isChecked) {
                // Start the service
                parentalService.sendCommandToDevice(deviceInfo, "START_WEBSITE_BLOCKING");
            } else {
                parentalService.sendCommandToDevice(deviceInfo, "STOP_WEBSITE_BLOCKING");
            }
        });
        cardViewSleepMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                parentalService.sendCommandToDevice(deviceInfo, "TOGGLE_SLEEP");

            }
        });
    }

    @Override
    public void onClick(View view) {

        if (this.btnBackParental == view){

            Intent intent = new Intent(ParentalControlActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();

        }

    }
}