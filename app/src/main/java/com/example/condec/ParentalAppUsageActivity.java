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

        this.btnParentalAppUsageBack = findViewById(R.id.btnParentalAppUsageBack);
        this.txtDeviceAppUsage = findViewById(R.id.txtDeviceAppUsage);
        this.rvParentalAppUsages = findViewById(R.id.rvParentalAppUsages);

        String targetDevice = getIntent().getStringExtra("deviceName");

        this.txtDeviceAppUsage.setText(targetDevice + "'s App Usages");

        appUsageList = getIntent().getParcelableArrayListExtra("appUsageList");

        adapter = new ParentalAppUsageAdapter(appUsageList);
        rvParentalAppUsages.setLayoutManager(new LinearLayoutManager(this));
        rvParentalAppUsages.setAdapter(adapter);

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
