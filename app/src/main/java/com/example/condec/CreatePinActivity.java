package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.condec.Utils.DualPinController;
import com.example.condec.Utils.NumpadView;
import com.example.condec.Utils.PinView;


public class CreatePinActivity extends AppCompatActivity implements View.OnClickListener {

    private PinView pinView1;
    private PinView pinView2;
    private NumpadView numpadView;
    private DualPinController dualPinController;

    private TextView[] pinView1Pins;
    private TextView[] pinView2Pins;
    private LinearLayout pinViewBackground1;
    private LinearLayout pinViewBackground2;
    private Button[] numpadButtons;
    private Button btnConfirmCreatePin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_pin);

        this.pinView1Pins = new TextView[4];

        this.pinView1Pins[0] = findViewById(R.id.pinView1Create1);
        this.pinView1Pins[1] = findViewById(R.id.pinView1Create2);
        this.pinView1Pins[2] = findViewById(R.id.pinView1Create3);
        this.pinView1Pins[3] = findViewById(R.id.pinView1Create4);

        this.pinView2Pins = new TextView[4];

        this.pinView2Pins[0] = findViewById(R.id.pinView2Create1);
        this.pinView2Pins[1] = findViewById(R.id.pinView2Create2);
        this.pinView2Pins[2] = findViewById(R.id.pinView2Create3);
        this.pinView2Pins[3] = findViewById(R.id.pinView2Create4);

        this.pinViewBackground1 = findViewById(R.id.pinView1CreateBackground);
        this.pinViewBackground2 = findViewById(R.id.pinView2CreateBackground);

        this.numpadButtons = new Button[11];

        this.numpadButtons[0] = findViewById(R.id.btnCreateNum1);
        this.numpadButtons[1] = findViewById(R.id.btnCreateNum2);
        this.numpadButtons[2] = findViewById(R.id.btnCreateNum3);
        this.numpadButtons[3] = findViewById(R.id.btnCreateNum4);
        this.numpadButtons[4] = findViewById(R.id.btnCreateNum5);
        this.numpadButtons[5] = findViewById(R.id.btnCreateNum6);
        this.numpadButtons[6] = findViewById(R.id.btnCreateNum7);
        this.numpadButtons[7] = findViewById(R.id.btnCreateNum8);
        this.numpadButtons[8] = findViewById(R.id.btnCreateNum9);
        this.numpadButtons[9] = findViewById(R.id.btnCreateNum0);
        this.numpadButtons[10] = findViewById(R.id.btnCreateNumBack);

        this.btnConfirmCreatePin = findViewById(R.id.btnConfirmCreatePin);
        this.btnConfirmCreatePin.setOnClickListener(this);

        this.pinView1 = new PinView('*');
        this.pinView1.setPins(this.pinView1Pins);
        this.pinView1.setPinViewBackground(this.pinViewBackground1);

        this.pinView2 = new PinView('*');
        this.pinView2.setPins(this.pinView2Pins);
        this.pinView2.setPinViewBackground(this.pinViewBackground2);

        this.numpadView = new NumpadView(numpadButtons);
        this.dualPinController = new DualPinController(this, this.pinView1, this.pinView2, this.numpadView);

    }

    private void savePassword(){

        String enteredPin = this.dualPinController.getEnteredData();

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

        String enteredPin = this.dualPinController.getEnteredData();
        String reEnteredPin = this.dualPinController.getReEnteredData();

        if (enteredPin.equals(reEnteredPin)){

            Toast.makeText(CreatePinActivity.this, "VALID: PIN SAVED", Toast.LENGTH_SHORT).show();
            savePassword();

        }
        else {

            Toast.makeText(CreatePinActivity.this, "INVALID PIN", Toast.LENGTH_SHORT).show();

        }

    }

    public void update(){

        if (this.dualPinController.isDone() == true){

            this.btnConfirmCreatePin.setEnabled(true);
            this.btnConfirmCreatePin.setBackgroundColor(getColor(R.color.green));
            this.btnConfirmCreatePin.setTextColor(getColor(R.color.white));

        }
        else {

            this.btnConfirmCreatePin.setEnabled(false);
            this.btnConfirmCreatePin.setBackgroundColor(getColor(R.color.gray));
            this.btnConfirmCreatePin.setTextColor(getColor(R.color.black_main_background));

        }

    }

    @Override
    public void onClick(View view) {

        if (this.btnConfirmCreatePin == view){

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