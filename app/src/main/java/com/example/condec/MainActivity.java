package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences condecPreferences;

    private boolean hasAgreed;
    private boolean hasPassword;
    private boolean hasBackupPassword;
    private boolean hasAllowedCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        getPrefData();
        initialize();

    }

    private void initialize(){

        if (this.hasAgreed == false){

            Intent intent = new Intent(MainActivity.this, TermsAndConditionsActivity.class);
            startActivity(intent);
            finish();

        }
        else {

            if (this.hasPassword == false){

                Intent intent = new Intent(MainActivity.this, CreatePinActivity.class);
                startActivity(intent);
                finish();

            }
            else if (this.hasBackupPassword == false){

                Intent intent = new Intent(MainActivity.this, CreateQuestionActivity.class);
                startActivity(intent);
                finish();

            }
            /*else if (this.hasAllowedCapture == false){

                Intent intent = new Intent(MainActivity.this, RequestMediaProjectionPermission.class);
                startActivity(intent);
                finish();

            }*/
            else {

                Intent intent = new Intent(MainActivity.this, EnterPinActivity.class);
                startActivity(intent);
                finish();

            }

        }

    }

    private void getPrefData(){

        this.hasAgreed = this.condecPreferences.getBoolean("hasAgreedConditions", false);
        this.hasPassword = this.condecPreferences.getBoolean("hasExistingPassword", false);
        this.hasBackupPassword = this.condecPreferences.getBoolean("hasExistingBackupPassword", false);
        this.hasAllowedCapture = this.condecPreferences.getBoolean("hasAllowedScreenCapture", false);

    }

}