package com.example.condec;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AppBlockingFragment extends Fragment implements View.OnClickListener {

    private static final int REQUEST_CODE_APP_SELECTION = 3;
    private final int REQUEST_CODE_OVERLAY_PERMISSION = 1;

    private LinearLayout blockedAppsContainer;
    private List<String> lockedApps;

    private ImageButton btnTipAppBlock;
    private Button btnManageBlockApp;

    private Switch switchAppBlock;
    private SharedPreferences sharedPreferences;
    private static final String SWITCH_STATE_KEY = "switch_state";

    public AppBlockingFragment() {

    }

    public static AppBlockingFragment newInstance(String param1, String param2) {
        AppBlockingFragment fragment = new AppBlockingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_blocking, container, false);

        blockedAppsContainer = view.findViewById(R.id.blockedAppsContainer);
        lockedApps = getLockedAppsFromPreferences();

        List<ApplicationInfo> installedApps = getInstalledApps();

        for (ApplicationInfo app : installedApps) {

            if (lockedApps.contains(app.packageName)) {
                createAppToggleView(app, blockedAppsContainer);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.btnTipAppBlock = view.findViewById(R.id.btnTipAppBlock);
        this.btnManageBlockApp = view.findViewById(R.id.btnManageBlockApp);

        this.btnTipAppBlock.setOnClickListener(this);
        this.btnManageBlockApp.setOnClickListener(this);

        this.switchAppBlock = view.findViewById(R.id.switchAppBlock);
        sharedPreferences = getActivity().getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(getActivity())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getActivity().getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            }
        }

        boolean isServiceRunning = isMyServiceRunning(CondecBlockingService.class);
        switchAppBlock.setChecked(isServiceRunning);

        switchAppBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SWITCH_STATE_KEY, isChecked);
            editor.apply();

            if (isChecked) {

                Intent serviceIntent = new Intent(getActivity(), CondecBlockingService.class);
                getActivity().startForegroundService(serviceIntent);


            } else {

                Intent serviceIntent = new Intent(getActivity(), CondecBlockingService.class);
                getActivity().stopService(serviceIntent);
            }
        });

    }

    private List<String> getLockedAppsFromPreferences() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        return new ArrayList<>(sharedPreferences.getStringSet("blockedApps", new HashSet<>()));
    }

    private List<ApplicationInfo> getInstalledApps() {
        PackageManager pm = getActivity().getPackageManager();
        return pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    private void createAppToggleView(ApplicationInfo app, ViewGroup parent) {
        View appView = LayoutInflater.from(getContext()).inflate(R.layout.item_app_blocking, parent, false);

        ImageView appIcon = appView.findViewById(R.id.appIcon);
        TextView appName = appView.findViewById(R.id.appName);
        Switch appToggle = appView.findViewById(R.id.appSwitch);

        PackageManager pm = getActivity().getPackageManager();
        appIcon.setImageDrawable(app.loadIcon(pm));
        appName.setText(app.loadLabel(pm));

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        boolean isLocked = sharedPreferences.getBoolean(app.packageName, false);
        appToggle.setChecked(isLocked);
        appToggle.setVisibility(View.INVISIBLE);

        appToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveLockedAppState(app.packageName, isChecked);
        });

        parent.addView(appView);
    }

    private void saveLockedAppState(String packageName, boolean isLocked) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(packageName, isLocked);
        editor.apply();
    }

    public void refresh() {

        lockedApps = getLockedAppsFromPreferences();
        blockedAppsContainer.removeAllViews();
        List<ApplicationInfo> installedApps = getInstalledApps();

        for (ApplicationInfo app : installedApps) {
            if (lockedApps.contains(app.packageName)) {
                createAppToggleView(app, blockedAppsContainer);
            }
        }

        if (isMyServiceRunning(CondecBlockingService.class)){

            Intent stopServiceIntent = new Intent(getActivity(), CondecBlockingService.class);
            getActivity().stopService(stopServiceIntent);
            Intent startServiceIntent = new Intent(getActivity(), CondecBlockingService.class);
            getActivity().startForegroundService(startServiceIntent);

        }
    }

    private void selectApp(){

        Intent intent = new Intent(getActivity(), AppSelectionActivity.class);
        startActivityForResult(intent, REQUEST_CODE_APP_SELECTION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_APP_SELECTION && resultCode == Activity.RESULT_OK) {
            refresh();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void showTip(){

        TipDialog dialog = new TipDialog("Blocked Apps", "Manage which apps can be accessed on your device. Blocked apps require a password or authentication to open.");
        dialog.show(requireActivity().getSupportFragmentManager(), "BlockedAppsInfoDialog");

    }

    @Override
    public void onClick(View view) {

        if (this.btnTipAppBlock == view){

            showTip();

        }

        if (this.btnManageBlockApp == view){

            selectApp();

        }

    }
}