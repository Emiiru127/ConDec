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

public class EnterPinActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText editTextEnteredPin;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_enter_pin);

        this.editTextEnteredPin = findViewById(R.id.editTextLoginEnteredPin);
        this.btnLogin = findViewById(R.id.btnLogin);

        this.btnLogin.setOnClickListener(this);

    }

    private void login(){

        SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        String enteredPin = this.editTextEnteredPin.getText().toString();
        String savedPin = condecPreferences.getString("savedPin", null);

        if (enteredPin.equals(savedPin)){

            Intent intent = new Intent(EnterPinActivity.this, MainMenuActivity.class);
            startActivity(intent);
            finish();

        }
        else{

            Toast.makeText(EnterPinActivity.this, "INVALID PIN", Toast.LENGTH_SHORT).show();

        }

    }

    @Override
    public void onClick(View view) {

        if(this.btnLogin == view){

            login();

        }

    }
}