package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences condecPreferences;

    private boolean hasLoaded = false;

    private boolean hasAgreed;
    private boolean hasPassword;
    private boolean hasBackupPassword;
    private boolean hasAllowedCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_loading_screen);

        this.condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        getPrefData();
        hasLoaded = getIntent().getBooleanExtra("hasLoaded", false);

        if (hasLoaded == false){

            hasLoaded = true;

            final Handler handler = new Handler();
            final Runnable r = new Runnable() {
                public void run() {

                    initialize();

                }
            };

            handler.postDelayed(r, 3000);

        }
        else {

            initialize();

        }

    }

    private void initialize(){

        if (this.hasAgreed == false){

            Intent intent = new Intent(MainActivity.this, StartingPageActivity.class);
            intent.putExtra("hasLoaded", hasLoaded);
            startActivity(intent);
            finish();

        }
        else {

            if (this.hasPassword == false){

                Intent intent = new Intent(MainActivity.this, CreatePinActivity.class);
                intent.putExtra("hasLoaded", hasLoaded);
                startActivity(intent);
                finish();

            }
            else if (this.hasBackupPassword == false){

                Intent intent = new Intent(MainActivity.this, CreateQuestionActivity.class);
                intent.putExtra("hasLoaded", hasLoaded);
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
                intent.putExtra("hasLoaded", hasLoaded);
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