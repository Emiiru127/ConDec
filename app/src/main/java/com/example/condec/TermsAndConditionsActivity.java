package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class TermsAndConditionsActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private CheckBox chkAgreed;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_and_conditions);

        this.chkAgreed = findViewById(R.id.chkAgreed);
        this.btnConfirm = findViewById(R.id.btnConfirmAgreement);

        this.chkAgreed.setOnCheckedChangeListener(this);
        this.btnConfirm.setOnClickListener(this);

        update();

    }

    private void submit(){

        if (this.chkAgreed.isChecked() == true){

            boolean hasAgreed = true;
            SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = condecPreferences.edit();
            editor.putBoolean("hasAgreedConditions", hasAgreed);
            editor.apply();

            Intent intent = new Intent(TermsAndConditionsActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

        }

    }

    private void update(){

        if (this.chkAgreed.isChecked() == true){

            this.btnConfirm.setEnabled(true);
            this.btnConfirm.setBackgroundColor(getColor(R.color.green));
            this.btnConfirm.setTextColor(getColor(R.color.white));

        }
        else {

            this.btnConfirm.setEnabled(false);
            this.btnConfirm.setBackgroundColor(getColor(R.color.gray));
            this.btnConfirm.setTextColor(getColor(R.color.black_main_background));

        }

    }

    @Override
    public void onClick(View view) {

        if (this.btnConfirm == view){

            submit();

        }

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        if (this.chkAgreed == compoundButton){

            update();

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