package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class CreatePinActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText editTextEnterPin;
    private EditText editTextReEnterPin;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_pin);

        this.editTextEnterPin = findViewById(R.id.editTextEnterPin);
        this.editTextReEnterPin = findViewById(R.id.editTextReEnterPin);
        this.btnSave = findViewById(R.id.btnSave);

        this.btnSave.setOnClickListener(this);

    }

    private void savePassword(){

        String enteredPin = this.editTextEnterPin.getText().toString();

        boolean hasPassword = true;
        SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = condecPreferences.edit();
        editor.putBoolean("hasExistingPassword", hasPassword);
        editor.putString("savedPin", enteredPin);
        editor.apply();

        Intent intent = new Intent(CreatePinActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

    }

    private void checkPassword(){

        String enteredPin = this.editTextEnterPin.getText().toString();
        String reEnteredPin = this.editTextReEnterPin.getText().toString();

        if (enteredPin.equals(reEnteredPin)){

            if (!enteredPin.isEmpty() && !reEnteredPin.isEmpty()){

                Toast.makeText(CreatePinActivity.this, "VALID: PIN SAVED", Toast.LENGTH_SHORT).show();
                savePassword();

            }
            else {

                Toast.makeText(CreatePinActivity.this, "INVALID: Pin must not be blank", Toast.LENGTH_SHORT).show();

            }

        }
        else {

            Toast.makeText(CreatePinActivity.this, "INVALID: Pin Length not Equal", Toast.LENGTH_SHORT).show();

        }

    }

    @Override
    public void onClick(View view) {

        if (this.btnSave == view){

            checkPassword();

        }

    }

    private boolean shouldAllowBack(){

        return  false;

    }

    @Override
    public void onBackPressed() {
        if (shouldAllowBack()) { // true for allow back
            super.onBackPressed();
        } else {

        }
    }

}