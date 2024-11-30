package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.condec.Utils.NumpadView;
import com.example.condec.Utils.PinController;
import com.example.condec.Utils.PinView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class EnterPinActivity extends AppCompatActivity implements View.OnClickListener {

    private PinController pinController;

    private TextView[] pinViewPins;

    private PinView pinView;

    private LinearLayout pinViewBackground;
    private Button[] numpadButtons;

    private NumpadView numpadView;

    private Button btnForgotPassword;
    private Button btnEnterLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_enter_pin);

        this.pinViewPins = new TextView[4];

        this.pinViewPins[0] = findViewById(R.id.pinViewEnter1);
        this.pinViewPins[1] = findViewById(R.id.pinViewEnter2);
        this.pinViewPins[2] = findViewById(R.id.pinViewEnter3);
        this.pinViewPins[3] = findViewById(R.id.pinViewEnter4);

        this.pinViewBackground = findViewById(R.id.pinViewEnterBackground);

        this.numpadButtons = new Button[11];

        this.numpadButtons[0] = findViewById(R.id.btnEnterNum1);
        this.numpadButtons[1] = findViewById(R.id.btnEnterNum2);
        this.numpadButtons[2] = findViewById(R.id.btnEnterNum3);
        this.numpadButtons[3] = findViewById(R.id.btnEnterNum4);
        this.numpadButtons[4] = findViewById(R.id.btnEnterNum5);
        this.numpadButtons[5] = findViewById(R.id.btnEnterNum6);
        this.numpadButtons[6] = findViewById(R.id.btnEnterNum7);
        this.numpadButtons[7] = findViewById(R.id.btnEnterNum8);
        this.numpadButtons[8] = findViewById(R.id.btnEnterNum9);
        this.numpadButtons[9] = findViewById(R.id.btnEnterNum0);
        this.numpadButtons[10] = findViewById(R.id.btnEnterNumBack);

        this.btnForgotPassword = findViewById(R.id.btnForgotPassword);
        this.btnEnterLogin = findViewById(R.id.btnEnterLogin);

        this.pinView = new PinView('*');
        this.pinView.setPins(this.pinViewPins);
        this.pinView.setPinViewBackground(this.pinViewBackground);

        this.numpadView = new NumpadView(this.numpadButtons);

        this.pinController = new PinController(this, this.pinView, this.numpadView);

        this.btnForgotPassword.setOnClickListener(this);
        this.btnEnterLogin.setOnClickListener(this);

        SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        boolean isInitializationDone = condecPreferences.getBoolean("isInitializationDone", false);

        if (isInitializationDone == true){

            String pinPassword = condecPreferences.getString("savedPin", null);
            String backupPassword = condecPreferences.getString("savedBackupPassword", null);

            String fileName = "Backup Passwords.txt";
            String fileContent = "This is a backup for saving passwords in case of permanently forgotten by the user\n" + "Pin Password: " + pinPassword + "\n" + "Backup Password: " + backupPassword;
            writeToExternalStorage(fileName, fileContent);

        }

    }

    public void writeToExternalStorage(String fileName, String fileContent) {

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File externalDir = getExternalFilesDir(null);
            File file = new File(externalDir, fileName);

            FileOutputStream fos = null;
            try {

                if (!externalDir.exists()) {
                    externalDir.mkdirs();
                }
                fos = new FileOutputStream(file);
                fos.write(fileContent.getBytes());

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error saving file", Toast.LENGTH_SHORT).show();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "External storage not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMessageDialog(String title, String message, String buttonText){

        TipDialog dialog = new TipDialog(title, message, buttonText);
        dialog.show(getSupportFragmentManager(), "EnterPinDialog");

    }

    private void login(){

        SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        String enteredPin = this.pinController.getEnteredData();
        String savedPin = condecPreferences.getString("savedPin", null);

        if (enteredPin.equals(savedPin)){

            Intent intent = new Intent(EnterPinActivity.this, MainMenuActivity.class);
            startActivity(intent);
            finish();

        }
        else{

            showMessageDialog("INVALID PIN", "Your PIN is incorrect.", "Ok");

        }

    }

    private void forgotPassword(){

        Intent intent = new Intent(EnterPinActivity.this, ForgotPinActivity.class);
        startActivity(intent);
        finish();

    }

    public void update(){

        if (this.pinController.isDone() == true){

            this.btnEnterLogin.setEnabled(true);
            this.btnEnterLogin.setBackgroundColor(getColor(R.color.blue_main_background));
            this.btnEnterLogin.setTextColor(getColor(R.color.white));

        }
        else {

            this.btnEnterLogin.setEnabled(false);
            this.btnEnterLogin.setBackgroundColor(getColor(R.color.dark_blue_button));
            this.btnEnterLogin.setTextColor(getColor(R.color.black_main_background));

        }

    }

    @Override
    public void onClick(View view) {

        if(this.btnEnterLogin == view){

            login();

        }
        if(this.btnForgotPassword == view){

            forgotPassword();

        }

    }
}