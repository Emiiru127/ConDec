package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.google.android.material.button.MaterialButton;

public class TermsAndConditionsActivity extends AppCompatActivity implements View.OnClickListener {

    private MaterialButton btnAccept;
    private MaterialButton btnDecline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_terms_and_conditions);

        this.btnAccept = findViewById(R.id.btnConfirmAgreement);
        this.btnDecline = findViewById(R.id.btnDeclineAgreement);

        this.btnAccept.setOnClickListener(this);
        this.btnDecline.setOnClickListener(this);

    }

    private void accept(){

        boolean hasAgreed = true;
        SharedPreferences condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = condecPreferences.edit();
        editor.putBoolean("hasAgreedConditions", hasAgreed);
        editor.apply();

        Intent intent = new Intent(TermsAndConditionsActivity.this, MainActivity.class);
        intent.putExtra("hasLoaded", getIntent().getBooleanExtra("hasLoaded", false));
        startActivity(intent);
        finish();

    }

    @Override
    public void onClick(View view) {

        if (this.btnAccept == view){

            accept();

        } else if (this.btnDecline == view) {

            return;

        }

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

}