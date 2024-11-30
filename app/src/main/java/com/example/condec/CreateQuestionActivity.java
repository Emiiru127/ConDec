package com.example.condec;

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
import android.widget.Toast;

public class CreateQuestionActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText editTxtQuestion;
    private EditText editTxtAnswer;
    private EditText editTxtReAnswer;

    private Button btnConfirmCreateQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_question);

        this.editTxtQuestion = findViewById(R.id.editTxtCreateQuestion);
        this.editTxtAnswer = findViewById(R.id.editTxtCreateAnswer);
        this.editTxtReAnswer = findViewById(R.id.editTxtCreateReAnswer);
        this.btnConfirmCreateQuestion = findViewById(R.id.btnConfirmCreateQuestion);

        this.btnConfirmCreateQuestion.setOnClickListener(this);

        this.editTxtQuestion.addTextChangedListener(new TextWatcher() {
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

        this.editTxtReAnswer.addTextChangedListener(new TextWatcher() {
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

    private void saveBackupPassword(){

        String enteredQuestion = this.editTxtQuestion.getText().toString().trim();
        String enteredPassword = this.editTxtAnswer.getText().toString().trim();

        boolean hasPassword = true;
        SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = condecPreferences.edit();
        editor.putBoolean("hasExistingBackupPassword", hasPassword);
        editor.putString("savedQuestion", enteredQuestion);
        editor.putString("savedBackupPassword", enteredPassword);
        editor.apply();

        Intent intent = new Intent(CreateQuestionActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

    }

    private void checkPassword(){

        String enteredPin = this.editTxtAnswer.getText().toString().trim();
        String reEnteredPin = this.editTxtReAnswer.getText().toString().trim();

        if (enteredPin.equals(reEnteredPin)){

            Toast.makeText(CreateQuestionActivity.this, "VALID: Backup Password", Toast.LENGTH_SHORT).show();
            saveBackupPassword();

        }
        else {

            Toast.makeText(CreateQuestionActivity.this, "INVALID BACKUP PASSWORD", Toast.LENGTH_SHORT).show();

        }

    }

    private void update(){

        if (this.editTxtQuestion.getText().length() != 0 && checkString(this.editTxtQuestion.getText().toString().trim())){

            enableConfirm();

        }
        else{

            disableConfirm();

        }

        if (this.editTxtAnswer.getText().length() != 0 && checkString(this.editTxtAnswer.getText().toString().trim())){

            enableConfirm();

        }
        else{

            disableConfirm();

        }

        if (this.editTxtReAnswer.getText().length() != 0 && checkString(this.editTxtReAnswer.getText().toString().trim())){

            enableConfirm();

        }
        else{

            disableConfirm();

        }

    }

    private void enableConfirm(){

        this.btnConfirmCreateQuestion.setEnabled(true);
        this.btnConfirmCreateQuestion.setBackgroundColor(getColor(R.color.green));
        this.btnConfirmCreateQuestion.setTextColor(getColor(R.color.white));

    }

    private void disableConfirm(){

        this.btnConfirmCreateQuestion.setEnabled(false);
        this.btnConfirmCreateQuestion.setBackgroundColor(getColor(R.color.gray));
        this.btnConfirmCreateQuestion.setTextColor(getColor(R.color.black_main_background));

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

        if (view == this.btnConfirmCreateQuestion){

            checkPassword();

        }

    }
}