package com.example.condec;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.condec.Classes.ParentalAppUsageAdapter;
import com.example.condec.Classes.ParentalAppUsageInfo;

import java.util.List;

public class ParentalAppUsageActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageButton btnParentalAppUsageBack;
    private TextView txtDeviceAppUsage;
    private RecyclerView rvParentalAppUsages;

    private List<ParentalAppUsageInfo> appUsageList;
    private ParentalAppUsageAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_parental_app_usage);

        // Initialize views
        this.btnParentalAppUsageBack = findViewById(R.id.btnParentalAppUsageBack);
        this.txtDeviceAppUsage = findViewById(R.id.txtDeviceAppUsage);
        this.rvParentalAppUsages = findViewById(R.id.rvParentalAppUsages);

        // Retrieve the app usage data from the Intent
        appUsageList = getIntent().getParcelableArrayListExtra("appUsageList");

        // Initialize and set up the RecyclerView with the existing adapter
        adapter = new ParentalAppUsageAdapter(appUsageList); // Assuming AppUsageAdapter is your existing adapter
        rvParentalAppUsages.setLayoutManager(new LinearLayoutManager(this));
        rvParentalAppUsages.setAdapter(adapter);

        // Handle back button click
        btnParentalAppUsageBack.setOnClickListener(this);
    }

    private void goBack(){

        finish();

    }

    @Override
    public void onClick(View view) {

        if(this.btnParentalAppUsageBack == view){

            goBack();

        }

    }
}
