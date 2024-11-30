package com.example.condec;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.condec.Classes.AppUsageAdapter;
import com.example.condec.Classes.AppUsageInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AppUsageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AppUsageFragment extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AppUsageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AppUsageFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AppUsageFragment newInstance(String param1, String param2) {
        AppUsageFragment fragment = new AppUsageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView recyclerView;
    private AppUsageAdapter adapter;

    private ImageButton btnTipAppUsage;

    private List<AppUsageInfo> appUsageList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        if (!hasUsageStatsPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.appUsages);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        this.btnTipAppUsage = view.findViewById(R.id.btnTipAppUsage);
        this.btnTipAppUsage.setOnClickListener(this);

        appUsageList = new ArrayList<>();
        adapter = new AppUsageAdapter(appUsageList);
        recyclerView.setAdapter(adapter);

        loadAppUsageData();

    }

    private void loadAppUsageData() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getContext().getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        // Query usage stats for the current day only
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, startTime, endTime);
        if (usageStatsList == null || usageStatsList.isEmpty()) {
            Toast.makeText(getContext(), "No usage data available", Toast.LENGTH_SHORT).show();
            return;
        }

        List<AppUsageInfo> tempAppUsageList = new ArrayList<>();
        Set<String> addedPackages = new HashSet<>();
        String myPackageName = getContext().getPackageName();
        PackageManager pm = getContext().getPackageManager();

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = pm.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY);
        String defaultLauncherPackage = resolveInfo.activityInfo.packageName;

        for (UsageStats usageStats : usageStatsList) {
            String packageName = usageStats.getPackageName();
            long usageTime = usageStats.getTotalTimeInForeground();
            long lastTimeUsed = usageStats.getLastTimeUsed();

            if (usageTime > 0 || lastTimeUsed > 0) {
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

                    if (packageName.equals(myPackageName) || packageName.equals(defaultLauncherPackage)) {
                        continue;
                    }

                    if (isSystemApp(appInfo) && !isAllowedSystemApp(packageName)) {
                        continue;
                    }

                    // Ensure no duplicate entries
                    if (!addedPackages.contains(packageName)) {
                        String appName = pm.getApplicationLabel(appInfo).toString();
                        Drawable appIcon = pm.getApplicationIcon(appInfo);
                        tempAppUsageList.add(new AppUsageInfo(packageName, appName, appIcon, usageTime, lastTimeUsed));
                        addedPackages.add(packageName);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        tempAppUsageList.sort((app1, app2) -> Long.compare(app2.getLastTimeUsed(), app1.getLastTimeUsed()));

        appUsageList.clear();
        int limit = 20;
        appUsageList.addAll(tempAppUsageList.subList(0, Math.min(tempAppUsageList.size(), limit)));

        adapter.notifyDataSetChanged();
    }


    private boolean isSystemApp(ApplicationInfo appInfo) {
        return ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) && ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0);
    }

    private boolean isAllowedSystemApp(String packageName) {
        return packageName.equals("com.android.vending") // Play Store
                || packageName.equals("com.google.android.youtube") // YouTube
                || packageName.equals("com.android.chrome"); // Your own app
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getActivity().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getActivity().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasUsageStatsPermission()) {
            loadAppUsageData();
        } else {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_app_usage, container, false);
    }

    private void showTip(){

        TipDialog dialog = new TipDialog("App Usage", "This feature displays the list of apps your child has recently used, along with the amount of time spent on each app.");
        dialog.show(requireActivity().getSupportFragmentManager(), "BlockedWebsitesInfoDialog");

    }

    @Override
    public void onClick(View view) {

        if (this.btnTipAppUsage == view){

            showTip();

        }

    }
}