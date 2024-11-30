package com.example.condec;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.condec.Classes.AppBlockAdapter;

import java.util.List;
import java.util.Set;

public class AppSelectionActivity extends AppCompatActivity {

    private RecyclerView appsRecyclerView;
    private AppBlockAdapter appsAdapter;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_app_selection);

        appsRecyclerView = findViewById(R.id.appsRecyclerView);
        saveButton = findViewById(R.id.saveButton);

        appsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installedApps = getInstalledApps(pm);

        appsAdapter = new AppBlockAdapter(installedApps, pm, this);
        appsRecyclerView.setAdapter(appsAdapter);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // You can return to the previous activity if needed
                setResult(RESULT_OK);
                Bundle result = new Bundle();
                getSupportFragmentManager().setFragmentResult("appSelection", result);
                finish();
            }
        });
    }

    private List<ApplicationInfo> getInstalledApps(PackageManager pm) {
        return pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Trigger the result to notify the fragment
        Bundle result = new Bundle();
        getSupportFragmentManager().setFragmentResult("appSelection", result);
    }

}