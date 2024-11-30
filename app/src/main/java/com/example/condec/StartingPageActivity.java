package com.example.condec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartingPageActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_starting_page);

        this.btnGetStarted = findViewById(R.id.btnGetStarted);
        this.btnGetStarted.setOnClickListener(this);

    }

    private void getStarted(){

        Intent intent = new Intent(StartingPageActivity.this, TermsAndConditionsActivity.class);
        intent.putExtra("hasLoaded", getIntent().getBooleanExtra("hasLoaded", false));
        startActivity(intent);
        finish();

    }

    @Override
    public void onClick(View view) {

        if (this.btnGetStarted == view){

            getStarted();

        }

    }
}