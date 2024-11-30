package com.example.condec;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.condec.Classes.AppBlockAdapter;

import java.util.ArrayList;
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
        List<ApplicationInfo> userApps = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String defaultLauncher = resolveInfo.activityInfo.packageName;

        for (ApplicationInfo app : installedApps) {
            if (pm.getApplicationIcon(app).getConstantState() != pm.getDefaultActivityIcon().getConstantState()) {
                if (!app.packageName.startsWith("com.android.overlay") ||
                        !app.packageName.startsWith("com.android.service") ||
                        !app.packageName.startsWith("com.android.carrier") ||
                        !app.packageName.startsWith("com.android.cts") ||
                        !app.packageName.startsWith("com.android.provider") ||
                        !app.packageName.startsWith("com.android.server") ||
                        !app.packageName.startsWith(defaultLauncher)) {

                    userApps.add(app);
                }
            }
        }

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                appsAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                appsAdapter.getFilter().filter(newText);
                return false;
            }
        });

        Button selectAllButton = findViewById(R.id.selectAllButton);
        Button deselectAllButton = findViewById(R.id.deselectAllButton);

        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (ApplicationInfo app : userApps) {
                    appsAdapter.getSelectedApps().add(app.packageName);
                }
                appsAdapter.notifyDataSetChanged(); // This will toggle all switches on
            }
        });

        deselectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appsAdapter.getSelectedApps().clear();
                appsAdapter.notifyDataSetChanged(); // This will toggle all switches off
            }
        });
        appsAdapter = new AppBlockAdapter(userApps, pm, this);
        appsRecyclerView.setAdapter(appsAdapter);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                // Add any data you need to pass back
                setResult(Activity.RESULT_OK, resultIntent);
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