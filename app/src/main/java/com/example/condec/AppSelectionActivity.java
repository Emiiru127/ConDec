package com.example.condec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.condec.Classes.AppBlockAdapter;
import com.example.condec.Database.BlockedURLRepository;
import com.example.condec.Database.UserBlockedUrl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppSelectionActivity extends AppCompatActivity {

    private RecyclerView appsRecyclerView;
    private AppBlockAdapter appsAdapter;
    private Button saveButton;

    private BlockedURLRepository repository;

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

        this.repository = new BlockedURLRepository(getApplication());

    }

    private void saveListed() {
        SharedPreferences sharedPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> selectedApps = appsAdapter.getSelectedApps();

        editor.putStringSet("blockedApps", selectedApps.isEmpty() ? new HashSet<>() : selectedApps);
        editor.apply();

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installedApps = getInstalledApps(pm);
        Set<String> installedAppPackages = new HashSet<>();

        for (ApplicationInfo app : installedApps) {
            if (shouldIncludeApp(app, pm)) {
                installedAppPackages.add(app.packageName);
            }
        }

        Set<String> deselectedApps = new HashSet<>(installedAppPackages);
        deselectedApps.removeAll(selectedApps);

        List<String> urlsToRemove = generateUrlsFromAppLabels(deselectedApps);

        new Thread(() -> {
            List<String> userBlockedUrls = repository.userBlockedUrlDao.getAllUrlsSync(); // Fetch only user-blocked URLs
            Log.d("AppSelection", "User Blocked URLs in DB: " + userBlockedUrls);

            for (String urlToRemove : urlsToRemove) {
                if (userBlockedUrls.contains(urlToRemove)) {
                    Log.d("AppSelection", "Removing URL by deleteByUrl: " + urlToRemove);
                    repository.executorService.execute(() -> repository.userBlockedUrlDao.deleteByUrl(urlToRemove));
                } else {
                    Log.d("AppSelection", "URL not found in DB: " + urlToRemove);
                }
            }

            List<String> urlsToAdd = generateUrlsFromAppLabels(selectedApps);
            urlsToAdd.removeAll(userBlockedUrls); // Avoid re-adding already present URLs

            runOnUiThread(() -> {
                if (urlsToAdd.isEmpty() && urlsToRemove.isEmpty()) {

                    Log.d("AppSelection", "No URLs to add or remove; finishing activity.");
                    finishWithResult();
                } else {

                    showAddUrlsDialog(urlsToAdd);
                }
            });
        }).start();
    }


    // Helper method to filter which apps should be included in the installed app list
    private boolean shouldIncludeApp(ApplicationInfo app, PackageManager pm) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String defaultLauncher = resolveInfo.activityInfo.packageName;

        Set<String> includePackages = new HashSet<>();
        includePackages.add("com.android.vending");
        includePackages.add("com.android.chrome");
        includePackages.add("com.google.android.youtube");

        return !app.packageName.equals(getPackageName()) &&
                !app.packageName.equals(defaultLauncher) &&
                (pm.getApplicationIcon(app).getConstantState() != pm.getDefaultActivityIcon().getConstantState()) &&
                ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 || includePackages.contains(app.packageName));
    }

    private List<String> generateUrlsFromAppLabels(Set<String> appPackages) {
        PackageManager pm = getPackageManager();
        List<String> urls = new ArrayList<>();

        for (String packageName : appPackages) {
            try {

                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

                String appLabel = pm.getApplicationLabel(appInfo).toString().toLowerCase().replace(" ", "");

                String url = appLabel + ".com";
                urls.add(url);
            } catch (PackageManager.NameNotFoundException e) {

                Log.e("AppSelection", "Error getting app info for: " + packageName, e);
            }
        }
        return urls;
    }


    private void showAddUrlsDialog(List<String> generatedUrls) {
        if (generatedUrls.isEmpty()) {
            Log.d("AppSelection", "No new URLs to add; finishing activity.");
            finishWithResult();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_add_url_to_website, null);

        TextView urlsList = dialogView.findViewById(R.id.urlsList);
        StringBuilder urlsBuilder = new StringBuilder();
        for (String url : generatedUrls) {
            urlsBuilder.append(url).append("\n");
        }
        urlsList.setText(urlsBuilder.toString());

        AlertDialog urlDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Yes", (dialog, which) -> {

                    List<UserBlockedUrl> userBlockedUrls = new ArrayList<>();
                    for (String url : generatedUrls) {
                        userBlockedUrls.add(new UserBlockedUrl(url));
                    }
                    repository.insertUserBlockedUrls(userBlockedUrls.toArray(new UserBlockedUrl[0]));
                    finishWithResult();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    Log.d("AppSelection", "User chose not to add URLs.");
                    finishWithResult();
                })
                .show();

        urlDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.blue_main_background));
        urlDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.blue_main_background));
    }



    private void finishWithResult() {
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