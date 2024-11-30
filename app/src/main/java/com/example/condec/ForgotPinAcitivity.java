package com.example.condec;

import static androidx.core.widget.TextViewKt.addTextChangedListener;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ForgotPinAcitivity extends AppCompatActivity implements View.OnClickListener {

    private TextView txtBackupQuestion;

    private EditText editTxtAnswer;

    private Button btnConfirm;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pin);

        SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        this.txtBackupQuestion = findViewById(R.id.txtForgotPinQuestion);
        this.editTxtAnswer = findViewById(R.id.editTxtForgotPinAnswer);
        this.btnConfirm = findViewById(R.id.btnConfirmForgotPin);
        this.btnBack = findViewById(R.id.btnBackForgotPin);

        this.txtBackupQuestion.setText(condecPreferences.getString("savedQuestion", null));

        this.btnConfirm.setOnClickListener(this);
        this.btnBack.setOnClickListener(this);

        this.editTxtAnswer.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler(Looper.getMainLooper());
            private Runnable workRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for this example
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed for this example
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(workRunnable);
                workRunnable = () -> {
                    // User has stopped typing. Perform your action here
                    performAction(s.toString());
                };
                handler.postDelayed(workRunnable, 500); // Set delay as per your need
            }

            private void performAction(String text) {
                // This method is called when we assume user has finished typing.
                update();
            }
        });

    }

    private void login(){

        Intent intent = new Intent(ForgotPinAcitivity.this, MainMenuActivity.class);
        startActivity(intent);
        finish();

    }

    private void checkAnswer(){

        SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        String enteredPin = this.editTxtAnswer.getText().toString().trim();
        String validAnswer = condecPreferences.getString("savedBackupPassword", null);

        if (enteredPin.equals(validAnswer)){

            Toast.makeText(ForgotPinAcitivity.this, "VALID: Backup Password", Toast.LENGTH_SHORT).show();
            login();

        }
        else {

            Toast.makeText(ForgotPinAcitivity.this, "INVALID BACKUP PASSWORD", Toast.LENGTH_SHORT).show();

        }

    }

    private void goBack(){

        Intent intent = new Intent(ForgotPinAcitivity.this, EnterPinActivity.class);
        startActivity(intent);
        finish();

    }

    private void update(){

        if (this.editTxtAnswer.getText().length() != 0 && checkString(this.editTxtAnswer.getText().toString().trim())){

            enableConfirm();

        }
        else{

            disableConfirm();

        }

    }

    private void enableConfirm(){

        this.btnConfirm.setEnabled(true);
        this.btnConfirm.setBackgroundColor(getColor(R.color.green));
        this.btnConfirm.setTextColor(getColor(R.color.white));

    }

    private void disableConfirm(){

        this.btnConfirm.setEnabled(false);
        this.btnConfirm.setBackgroundColor(getColor(R.color.gray));
        this.btnConfirm.setTextColor(getColor(R.color.black_main_background));

    }

    private boolean checkString(String data){

        boolean check = false;

        for(int i = 0; i < data.length(); i++){

            if (data.charAt(i) == ' '){

                check = false;

            }
            else {

                check = true;
                break;

            }

        }

        return  check;

    }

    @Override
    public void onClick(View view) {

        if (this.btnConfirm == view){

            checkAnswer();

        }
        if (this.btnBack == view){

            goBack();

        }

    }
}