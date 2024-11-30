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

    }

}