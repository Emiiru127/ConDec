package com.example.condec;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ReadTermsConditionsActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageButton btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_read_conditions);

        this.btnBack = findViewById(R.id.btnTermsBack);
        this.btnBack.setOnClickListener(this);

    }

    private void goBack(){
        Intent intent = new Intent(ReadTermsConditionsActivity.this, SettingsActivity.class);
        startActivity(intent);
        finish();
    }

    public void onBackPressed() {

        if (false){

            super.onBackPressed();

        }
    }

    @Override
    public void onClick(View view) {

        if (this.btnBack == view){

            goBack();

        }

    }
}
