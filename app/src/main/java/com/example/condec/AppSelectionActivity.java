package com.example.condec;

import android.app.Activity;
import android.content.Context;
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
import java.util.HashSet;
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

        Set<String> includePackages = new HashSet<>();
        includePackages.add("com.android.vending");
        includePackages.add("com.android.chrome");
        includePackages.add("com.google.android.youtube");

        for (ApplicationInfo app : installedApps) {
            if (!app.packageName.equals(getPackageName())
                    && !app.packageName.equals(defaultLauncher)
                    && (pm.getApplicationIcon(app).getConstantState() != pm.getDefaultActivityIcon().getConstantState())
                    && ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 || includePackages.contains(app.packageName))) {
                userApps.add(app);
            }
        }

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        Set<String> previouslySelectedApps = sharedPreferences.getStringSet("blockedApps", new HashSet<>());

        appsAdapter = new AppBlockAdapter(userApps, pm, previouslySelectedApps);
        appsRecyclerView.setAdapter(appsAdapter);

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

        selectAllButton.setOnClickListener(v -> appsAdapter.selectAll());

        deselectAllButton.setOnClickListener(v -> appsAdapter.deselectAll());

        saveButton.setOnClickListener(v -> {

            saveListed();

        });

    }

    private void saveListed(){

        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> selectedApps = appsAdapter.getSelectedApps();

        editor.putStringSet("blockedApps", selectedApps.isEmpty() ? new HashSet<>() : selectedApps);

        editor.apply();

        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_OK, resultIntent);
        finish();

    }

    private List<ApplicationInfo> getInstalledApps(PackageManager pm) {
        return pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Bundle result = new Bundle();
        getSupportFragmentManager().setFragmentResult("appSelection", result);
    }
}