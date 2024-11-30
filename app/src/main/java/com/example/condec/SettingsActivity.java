package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageButton btnSettingsBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_settings);

        this.btnSettingsBack = findViewById(R.id.btnSettingsBack);

        this.btnSettingsBack.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {

        if (view == this.btnSettingsBack){

            Intent intent = new Intent(SettingsActivity.this, MainMenuActivity.class);
            startActivity(intent);
            finish();

        }

    }
}