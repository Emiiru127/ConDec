package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ParentalControlActivity extends AppCompatActivity implements View .OnClickListener{

    private ImageButton btnBackParental;
    private TextView txtViewTargetDeviceName;

    private Switch switchDetection;
    private Switch switchAppBlocking;
    private Switch switchWebsiteBlocking;
    private CardView cardViewSleepMode;
    private CardView cardViewAppUsage;
    private CardView cardViewScreenView;
    private CardView cardViewDisplayMessage;
    private CardView cardViewForceGoBack;
    private CardView cardViewForceGoHome;
    private CardView cardViewPrevention;
    private TextView txtPreventionStatus;
    private CondecParentalService parentalService;

    private ParentalControlActivity parentalControlActivity;
    private boolean isBound = false;

    private boolean isPreventing;

    private String deviceName;
    private NsdServiceInfo deviceInfo;

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

    public ParentalControlActivity(){

        this.parentalControlActivity = this;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_parental_controls);

        this.btnBackParental = findViewById(R.id.btnBackParental);
        this.btnBackParental.setOnClickListener(this);

        this.txtViewTargetDeviceName = findViewById(R.id.txtDeviceAppUsage);

        this.switchDetection = findViewById(R.id.switchWarning);
        this.switchAppBlocking = findViewById(R.id.switchBlockingApps);
        this.switchWebsiteBlocking = findViewById(R.id.switchBlockingWebsites);
        this.cardViewSleepMode = findViewById(R.id.cardViewSleepMode);
        this.cardViewAppUsage = findViewById(R.id.cardViewAppUsage);
        this.cardViewScreenView = findViewById(R.id.cardViewScreenView);
        this.cardViewDisplayMessage = findViewById(R.id.cardViewDisplayMessage);
        this.cardViewForceGoBack = findViewById(R.id.cardViewGoBack);
        this.cardViewForceGoHome = findViewById(R.id.cardViewGoHome);
        this.cardViewPrevention = findViewById(R.id.cardViewPrevention);
        this.txtPreventionStatus = findViewById(R.id.txtPreventionStatus);

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
        this.isPreventing = Boolean.parseBoolean((receivedData.get(5)).split(":")[1]);

        this.txtViewTargetDeviceName.setText(deviceName);

        this.switchDetection.setChecked(isDetecting);
        this.switchAppBlocking.setChecked(isAppBlocking);
        this.switchWebsiteBlocking.setChecked(isWebsiteBlocking);

        updatePreventionText();

        this.switchDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                activateDetectionToDevice();
            } else {
                deactivateDetectionToDevice();
            }
        });
        this.switchAppBlocking.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                activateAppBlockingToDevice();
            } else {
                deactivateAppBlockingToDevice();
            }
        });
        this.switchWebsiteBlocking.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                activateWebsiteBlockingToDevice();
            } else {
                deactivateWebsiteBlockingToDevice();
            }
        });
        this.cardViewSleepMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openSleepSettingsOfDevice();

            }
        });
        this.cardViewAppUsage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                requestAppUsageOfDevice();

            }
        });
        this.cardViewScreenView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                requestViewScreen();

            }
        });
        this.cardViewDisplayMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showSendMessageDialog();

            }
        });
        this.cardViewForceGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                requestGoBack();

            }
        });
        this.cardViewForceGoHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                requestGoHome();
            }
        });
        this.cardViewPrevention.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isPreventing == true){

                    ConfirmationDialog confirmationDialog = new ConfirmationDialog("Disable Prevention Security?",
                            "WARNING:\nDisabling this feature will allow the user of that device to access the settings" +
                                    " without a password.",
                            parentalControlActivity);
                    confirmationDialog.show(getSupportFragmentManager(), "ConfirmationDialog");

                }
                else {

                    activateSecurityToDevice();

                }

            }
        });
    }

    public void showSleepSettings(List<String> sleepData){

        try {
            SleepControlDialog sleepControlDialog = new SleepControlDialog(this, sleepData);
            sleepControlDialog.show(getSupportFragmentManager(), "Parental Sleep Settings");
        }
       catch (Exception e){

            Log.d("Condec Parental Control", "ERROR: " + e);

       }

    }

    public void updatePreventionText(){

        if (this.isPreventing){

            this.txtPreventionStatus.setText("Active");
            this.txtPreventionStatus.setTextColor(getColor(R.color.green));

        }
        else {

            this.txtPreventionStatus.setText("Inactive");
            this.txtPreventionStatus.setTextColor(getColor(R.color.red));

        }

    }

    public void sendSleepCommandToDevice(String command, String input){

        parentalService.sendSleepCommandToDevice(deviceInfo, command, input);

    }

    public void activateDetectionToDevice(){

        parentalService.sendCommandToDevice(deviceInfo, "START_DETECTION");

    }
    public void deactivateDetectionToDevice(){

        parentalService.sendCommandToDevice(deviceInfo, "STOP_DETECTION");

    }

    public void activateAppBlockingToDevice(){

        parentalService.sendCommandToDevice(deviceInfo, "START_APP_BLOCKING");

    }
    public void deactivateAppBlockingToDevice(){

        parentalService.sendCommandToDevice(deviceInfo, "STOP_APP_BLOCKING");

    }

    public void activateWebsiteBlockingToDevice(){

        parentalService.sendCommandToDevice(deviceInfo, "START_WEBSITE_BLOCKING");

    }
    public void deactivateWebsiteBlockingToDevice(){

        parentalService.sendCommandToDevice(deviceInfo, "STOP_WEBSITE_BLOCKING");

    }

    public void openSleepSettingsOfDevice(){

        parentalService.requestSleepData(deviceInfo, parentalControlActivity);

    }
    public void requestAppUsageOfDevice(){

        parentalService.requestAppUsageData(deviceInfo, parentalControlActivity);

    }

    public void requestViewScreen(){

        parentalService.requestViewScreen(deviceInfo, parentalControlActivity);

    }

    public void requestDisplayMessage(String message){

        SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        String deviceName = condecPreferences.getString("deviceName", null);

        parentalService.sendCommandToDevice(deviceInfo, "DISPLAY_MESSAGE|" + deviceName + "|" + message);

    }

    public void requestGoBack(){

        parentalService.sendCommandToDevice(deviceInfo, "GO_BACK");

    }

    public void requestGoHome(){

        parentalService.sendCommandToDevice(deviceInfo, "GO_HOME");

    }

    public void activateSecurityToDevice(){

        parentalService.sendCommandToDevice(deviceInfo, "START_SECURITY");
        this.isPreventing = true;
        updatePreventionText();

    }
    public void deactivateSecurityToDevice(){

        parentalService.sendCommandToDevice(deviceInfo, "STOP_SECURITY");
        this.isPreventing = false;
        updatePreventionText();

    }

    public String getCurrentDeviceTarget(){

        return this.deviceName;

    }

    private void showSendMessageDialog() {

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.layout_request_string, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        TextView txtView = dialogView.findViewById(R.id.txtViewText);
        EditText editTxtInput = dialogView.findViewById(R.id.editTxtInput);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAdd = dialogView.findViewById(R.id.btnDone);

        txtView.setText("Enter the message to display on device:");
        editTxtInput.setHint("Enter message");
        btnAdd.setText("Send");

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String message = editTxtInput.getText().toString().trim();
            if (!message.isEmpty()) {
                requestDisplayMessage(message);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void showMessageDialog(String title, String message){

        TipDialog dialog = new TipDialog(title, message);
        dialog.show(getSupportFragmentManager(), "ParentalDialog");

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