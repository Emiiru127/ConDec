package com.example.condec;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AboutCondecActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageButton btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_about_condec);

        this.btnBack = findViewById(R.id.btnAboutCondecBack);
        this.btnBack.setOnClickListener(this);

    }

    private void goBack(){
        Intent intent = new Intent(AboutCondecActivity.this, SettingsActivity.class);
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
