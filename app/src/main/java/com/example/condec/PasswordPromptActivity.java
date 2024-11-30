package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.condec.Utils.NumpadView;
import com.example.condec.Utils.PinController;
import com.example.condec.Utils.PinView;

import java.util.HashSet;
import java.util.Set;

public class PasswordPromptActivity extends AppCompatActivity implements View.OnClickListener {

    private PinController pinController;

    private TextView[] pinViewPins;

    private PinView pinView;

    private LinearLayout pinViewBackground;
    private Button[] numpadButtons;

    private NumpadView numpadView;
    private Button btnPinEnter;

    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;
    private SharedPreferences condecPreferences;
    private String correctPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_password_prompt);

        this.pinViewPins = new TextView[4];

        this.pinViewPins[0] = findViewById(R.id.pinViewPinEnter1);
        this.pinViewPins[1] = findViewById(R.id.pinViewPinEnter2);
        this.pinViewPins[2] = findViewById(R.id.pinViewPinEnter3);
        this.pinViewPins[3] = findViewById(R.id.pinViewPinEnter4);

        this.pinViewBackground = findViewById(R.id.pinViewPinEnterBackground);

        this.numpadButtons = new Button[11];

        this.numpadButtons[0] = findViewById(R.id.btnPinEnterNum1);
        this.numpadButtons[1] = findViewById(R.id.btnPinEnterNum2);
        this.numpadButtons[2] = findViewById(R.id.btnPinEnterNum3);
        this.numpadButtons[3] = findViewById(R.id.btnPinEnterNum4);
        this.numpadButtons[4] = findViewById(R.id.btnPinEnterNum5);
        this.numpadButtons[5] = findViewById(R.id.btnPinEnterNum6);
        this.numpadButtons[6] = findViewById(R.id.btnPinEnterNum7);
        this.numpadButtons[7] = findViewById(R.id.btnPinEnterNum8);
        this.numpadButtons[8] = findViewById(R.id.btnPinEnterNum9);
        this.numpadButtons[9] = findViewById(R.id.btnPinEnterNum0);
        this.numpadButtons[10] = findViewById(R.id.btnPinEnterNumBack);

        this.btnPinEnter = findViewById(R.id.btnPinEnter);

        this.pinView = new PinView('*');
        this.pinView.setPins(this.pinViewPins);
        this.pinView.setPinViewBackground(this.pinViewBackground);

        this.numpadView = new NumpadView(this.numpadButtons);

        this.pinController = new PinController(this, this.pinView, this.numpadView);

        this.btnPinEnter.setOnClickListener(this);

        condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        correctPassword = condecPreferences.getString("savedPin", null);

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminName = new ComponentName(this, AdminReceiver.class);

    }

    private void checkPassword(){

        String enteredPassword = this.pinController.getEnteredData();

        if (correctPassword.equals(enteredPassword)) {
            // Correct password, allow access and finish the activity
            String currentPackageName = getIntent().getStringExtra("PACKAGE_NAME");
            if (currentPackageName != null) {
                Intent intent = new Intent("com.example.condec.UNLOCK_APP");
                intent.putExtra("PACKAGE_NAME", currentPackageName);
                sendBroadcast(intent); // Broadcast the unlock event
            }

            finish();
        } else {
            // Incorrect password, show a message
            Toast.makeText(PasswordPromptActivity.this, "Incorrect password. Try again.", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if the app should still be locked
        String currentPackageName = getIntent().getStringExtra("PACKAGE_NAME");
        SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        Set<String> blockedApps = condecPreferences.getStringSet("blockedApps", new HashSet<>());

        if (blockedApps.contains(currentPackageName)) {
            // Show the password prompt again
            if (!isFinishing()) {
                // Activity is still active, do nothing
            } else {
                // Re-launch the password prompt
                finish();
                Intent intent = new Intent(this, PasswordPromptActivity.class);
                intent.putExtra("PACKAGE_NAME", currentPackageName);
                startActivity(intent);
            }
        }
    }


    public void update(){

        if (this.pinController.isDone() == true){

            this.btnPinEnter.setEnabled(true);
            this.btnPinEnter.setBackgroundColor(getColor(R.color.blue_main_background));
            this.btnPinEnter.setTextColor(getColor(R.color.white));

        }
        else {

            this.btnPinEnter.setEnabled(false);
            this.btnPinEnter.setBackgroundColor(getColor(R.color.dark_blue_button));
            this.btnPinEnter.setTextColor(getColor(R.color.black_main_background));

        }

    }

    @Override
    public void onClick(View view) {

        if(this.btnPinEnter == view){

            checkPassword();

        }

    }

    @Override
    public void onBackPressed() {

        if (false){

            super.onBackPressed();

        }
    }

}