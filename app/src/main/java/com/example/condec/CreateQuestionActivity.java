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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class CreateQuestionActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText editTxtQuestion;
    private EditText editTxtAnswer;
    private EditText editTxtReAnswer;

    private Button btnConfirmCreateQuestion;

    private boolean forChanging = false;

    private ImageButton btnCreateQuestionBack;
    private TextView txtViewQuestionTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_create_question);

        this.editTxtQuestion = findViewById(R.id.editTxtCreateQuestion);
        this.editTxtAnswer = findViewById(R.id.editTxtCreateAnswer);
        this.editTxtReAnswer = findViewById(R.id.editTxtCreateReAnswer);
        this.btnConfirmCreateQuestion = findViewById(R.id.btnConfirmCreateQuestion);

        this.btnConfirmCreateQuestion.setOnClickListener(this);

        this.forChanging = getIntent().getBooleanExtra("forChanging", false);

        this.btnCreateQuestionBack = findViewById(R.id.btnCreateQuestionBack);
        this.txtViewQuestionTitle = findViewById(R.id.txtViewQuestionTitle);

        this.btnCreateQuestionBack.setOnClickListener(this);

        if (this.forChanging){

            this.btnCreateQuestionBack.setVisibility(View.VISIBLE);
            this.txtViewQuestionTitle.setText("Change\nBackup Password");

            SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
            String oldQuestion = condecPreferences.getString("savedQuestion", null);
            String oldAnswer = condecPreferences.getString("savedBackupPassword", null);

            this.editTxtQuestion.setText(oldQuestion);
            this.editTxtAnswer.setText(oldAnswer);

        }

        this.editTxtQuestion.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler(Looper.getMainLooper());
            private Runnable workRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(workRunnable);
                workRunnable = () -> {

                    performAction(s.toString());
                };
                handler.postDelayed(workRunnable, 500);
            }

            private void performAction(String text) {

                update();
            }
        });

        this.editTxtAnswer.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler(Looper.getMainLooper());
            private Runnable workRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(workRunnable);
                workRunnable = () -> {

                    performAction(s.toString());
                };
                handler.postDelayed(workRunnable, 500);
            }

            private void performAction(String text) {

                update();
            }
        });

        this.editTxtReAnswer.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler(Looper.getMainLooper());
            private Runnable workRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(workRunnable);
                workRunnable = () -> {

                    performAction(s.toString());
                };
                handler.postDelayed(workRunnable, 500);
            }

            private void performAction(String text) {

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

        if (this.forChanging == false){

            Intent intent = new Intent(CreateQuestionActivity.this, MainActivity.class);
            intent.putExtra("hasLoaded", getIntent().getBooleanExtra("hasLoaded", false));
            startActivity(intent);
            finish();

        }
        else {

            Intent intent = new Intent(CreateQuestionActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();

        }

    }

    private void checkPassword(){

        String enteredPin = this.editTxtAnswer.getText().toString().trim();
        String reEnteredPin = this.editTxtReAnswer.getText().toString().trim();

        if (checkString(this.editTxtQuestion.getText().toString().trim()) == false){

            Toast.makeText(CreateQuestionActivity.this, "INVALID Question", Toast.LENGTH_SHORT).show();
            return;

        }

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
        this.btnConfirmCreateQuestion.setBackgroundColor(getColor(R.color.blue_main_background));
        this.btnConfirmCreateQuestion.setTextColor(getColor(R.color.white));

    }

    private void disableConfirm(){

        this.btnConfirmCreateQuestion.setEnabled(false);
        this.btnConfirmCreateQuestion.setBackgroundColor(getColor(R.color.dark_blue_button));
        this.btnConfirmCreateQuestion.setTextColor(getColor(R.color.black_main_background));

    }

    private void goToSettings(){
        Intent intent = new Intent(CreateQuestionActivity.this, SettingsActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean checkString(String data){

        boolean check = false;

        for(int i = 0; i < data.length(); i++){

            if (data.charAt(i) != ' '){

                check = true;
                break;

            }

        }

        return  check;

    }

    private boolean shouldAllowBack(){

        return  false;

    }

    @Override
    public void onBackPressed() {
        if (shouldAllowBack()) {
            super.onBackPressed();
        } else {

        }
    }

    @Override
    public void onClick(View view) {

        if (view == this.btnCreateQuestionBack){

            goToSettings();

        }

        if (view == this.btnConfirmCreateQuestion){

            checkPassword();

        }

    }
}