package com.example.condec;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainMenuActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int REQUEST_CODE_DRAW_OVERLAY = 5469;
    private static final int REQUEST_CODE_USAGE_ACCESS = 5470;
    private static final int REQUEST_CODE_VPN = 5471;

    private static final int ACCESSIBILITY_REQUEST_CODE = 5472;

    private static final int REQUEST_CODE_BATTERY_OPTIMIZATION = 5473;
    private SharedPreferences condecPreferences;

    //Device admin
    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;

    //UI

    private SurfaceView surfaceView;
    private ImageView imgViewRename;
    private TextView txtViewRename;

    private ImageButton btnSettings;

    private MaterialButton btnSleep;

    private FrameLayout frameFeatures;
    private MaterialButton btnFeatureWarningDetection;
    private MaterialButton btnFeatureAppBlocking;
    private MaterialButton btnFeatureWebsiteBlocking;
    private MaterialButton btnFeatureAppUsage;

    private MaterialButton[] btnFeatures;

    private CondecParentalService condecParentalService;
    private CondecSecurityService condecSecurityService;
    private CondecSleepService condecSleepService;

    private CondecDetectionService condecDetectionService;
    boolean mBound = false;

    private boolean isToggleSleep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main_menu);

        this.condecPreferences = getSharedPreferences("condecPref", Context.MODE_PRIVATE);

        initializeUI();

        updateFrameFeatures("Warning Detection");

        checkAndRequestPermissions();
        checkSleepService();

        boolean isInitializationDone = condecPreferences.getBoolean("isInitializationDone", false);

        if (isInitializationDone == false){

            initialization();

        }

    }

    private void initialization(){

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> userApps = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String defaultLauncher = resolveInfo.activityInfo.packageName;

        // Add packages to include
        Set<String> includePackages = new HashSet<>();
        includePackages.add("com.android.vending"); // Play Store
        includePackages.add("com.android.chrome");  // Chrome
        includePackages.add("com.google.android.youtube"); // YouTube

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
        boolean isInitializationDone = sharedPreferences.getBoolean("isInitializationDone", false);

        // On first launch, select all apps by default and save them as blocked apps
        previouslySelectedApps = new HashSet<>();
        for (ApplicationInfo app : userApps) {
            previouslySelectedApps.add(app.packageName); // Select all user apps on first launch
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("blockedApps", previouslySelectedApps);
        editor.putBoolean("isInitializationDone", true); // Mark initialization as done
        editor.apply();


    }

   /* private void requestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY);
        }

        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_USAGE_ACCESS);
        }
    }*/

    private void checkAdminPermission(){

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminName = new ComponentName(this, AdminReceiver.class);

        if (!mDPM.isAdminActive(mAdminName)) {
            Intent intent = new Intent(MainMenuActivity.this, RequestAdminPermission.class);
            intent.putExtra("hasLoaded", getIntent().getBooleanExtra("hasLoaded", false));
            startActivity(intent);
            finish();
        }

    }

    private void checkBatteryPermission(){

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            // Battery optimizations are disabled for this app
        } else {
            // Battery optimizations are enabled, prompt the user to disable it
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

    }


    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + CondecAccessibilityService.class.getCanonicalName();

        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                colonSplitter.setString(settingValue);
                while (colonSplitter.hasNext()) {
                    String componentName = colonSplitter.next();
                    if (componentName.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void startRequiredServices() {

        // If the device name is set, start the required services
        if (!isServiceRunning(CondecParentalService.class)) {
            Intent serviceIntent = new Intent(this, CondecParentalService.class);
            startForegroundService(serviceIntent);
        }
        if (!isServiceRunning(CondecSecurityService.class)) {
            Intent serviceIntent = new Intent(this, CondecSecurityService.class);
            startForegroundService(serviceIntent);
        }

    }

    public void checkSleepService(){

        this.isToggleSleep = isServiceRunning(CondecSleepService.class);
        update();

    }

    // Check and request all necessary permissions
    private void checkAndRequestPermissions() {
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminName = new ComponentName(this, AdminReceiver.class);
        String deviceName = condecPreferences.getString("deviceName", null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!mDPM.isAdminActive(mAdminName)) {
                requestAdminPermission();
            }
            else if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
            } else if (!isUsageAccessGranted()) {
                requestUsageAccessPermission();
            } else if (!isVPNPermissionGranted()) {
                requestVPNPermission();
            } else if (!isAccessibilityServiceEnabled()) {
                requestAccessibilityPermission();
            } else if (!isBatteryOptimizationIgnored()) {
                requestBatteryOptimizationPermission();
            }
            else if (deviceName == null || deviceName.isEmpty()) {
                // If the device name is not set, prompt the user to set it
                showMandatoryRenameDeviceDialog();
            }
            else {

                startRequiredServices();

            }

        } else {
            if (!isVPNPermissionGranted()) {
                requestVPNPermission();
            } else {
                requestAdminPermission();
            }
        }
    }


    private boolean isBatteryOptimizationIgnored() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(getPackageName());
    }
    // Continuously request Accessibility Permission until granted
    private void requestAccessibilityPermission() {
        TipDialog tipDialog = new TipDialog("Accessibility Permission",
                "This app requires accessibility permission to monitor and control app activities.",
                () -> {
                    if (!isAccessibilityServiceEnabled()) {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivityForResult(intent, ACCESSIBILITY_REQUEST_CODE);
                    }
                });
        tipDialog.show(getSupportFragmentManager(), "AccessibilityPermissionDialog");
    }

    private void requestAdminPermission() {
        TipDialog tipDialog = new TipDialog("Device Admin Permission",
                "This app requires device admin privileges to perform specific tasks securely.",
                () -> {
                    mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                    mAdminName = new ComponentName(this, AdminReceiver.class);

                    if (!mDPM.isAdminActive(mAdminName)) {
                        Intent intent = new Intent(MainMenuActivity.this, RequestAdminPermission.class);
                        startActivity(intent);
                    }
                });
        tipDialog.show(getSupportFragmentManager(), "AdminPermissionDialog");
    }

    private void requestBatteryOptimizationPermission() {
        TipDialog tipDialog = new TipDialog("Battery Optimization",
                "This app needs to ignore battery optimizations to function correctly in the background.",
                () -> {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION);
                    }
                });
        tipDialog.show(getSupportFragmentManager(), "BatteryOptimizationPermissionDialog");
    }



    /*
    private void requestCapturePermission(){

        System.out.println("REQUESTING MEDIA PROJECTION PERMISSION");
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, REQUEST_CAPTURE_CODE);


    }*/
    private void requestOverlayPermission() {
        TipDialog tipDialog = new TipDialog("Overlay Permission", "This app requires overlay permission to display content on top of other apps.",
                () -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY);
                });
        tipDialog.show(getSupportFragmentManager(), "OverlayPermissionDialog");
    }

    private void requestUsageAccessPermission() {
        TipDialog tipDialog = new TipDialog("Usage Access Permission", "This app requires usage access permission to monitor your app usage.",
                () -> {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_USAGE_ACCESS);
                });
        tipDialog.show(getSupportFragmentManager(), "UsageAccessPermissionDialog");
    }

    private void requestVPNPermission() {
        TipDialog tipDialog = new TipDialog("VPN Permission",
                "This app requires VPN permission to secure your network connection.",
                () -> {
                    Intent vpnIntent = CondecVPNService.prepare(this);
                    if (vpnIntent != null) {
                        startActivityForResult(vpnIntent, REQUEST_CODE_VPN);
                    } else {
                        // VPN permission is already granted
                        startRequiredServices();
                    }
                });
        tipDialog.show(getSupportFragmentManager(), "VPNPermissionDialog");
    }
    private boolean isUsageAccessGranted() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isVPNPermissionGranted() {
        return CondecVPNService.prepare(this) == null;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DRAW_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "Overlay permission is required.", Toast.LENGTH_SHORT).show();
                requestOverlayPermission();
            }
        } else if (requestCode == REQUEST_CODE_USAGE_ACCESS) {
            if (isUsageAccessGranted()) {
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "Usage access permission is required.", Toast.LENGTH_SHORT).show();
                requestUsageAccessPermission();
            }
        } else if (requestCode == REQUEST_CODE_VPN) {
            if (isVPNPermissionGranted()) {
                Intent vpnIntent = new Intent(this, CondecVPNService.class);
                this.startService(vpnIntent);
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "VPN permission is required.", Toast.LENGTH_SHORT).show();
                requestVPNPermission();
            }
        }
        else if (requestCode == ACCESSIBILITY_REQUEST_CODE) {
            if (isAccessibilityServiceEnabled()) {
                // Accessibility service enabled, proceed with starting services
                checkAndRequestPermissions();

            } else {
                // Accessibility not enabled, ask again
                checkAndRequestPermissions();
            }
        }
        else if (requestCode == REQUEST_CODE_BATTERY_OPTIMIZATION) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "Battery optimization permission is required.", Toast.LENGTH_SHORT).show();
                requestBatteryOptimizationPermission();
            }
        }
    }

    private void initializeUI(){

        this.imgViewRename = findViewById(R.id.imgViewRename);
        this.txtViewRename = findViewById(R.id.txtViewRename);

        this.btnSettings = findViewById(R.id.btnSettings);

        this.btnSleep = findViewById(R.id.btnSleep);

        this.frameFeatures = findViewById(R.id.frameFeatures);

        this.btnFeatureWarningDetection = findViewById(R.id.btnWarningDetection);
        this.btnFeatureAppBlocking = findViewById(R.id.btnAppBlocking);
        this.btnFeatureWebsiteBlocking = findViewById(R.id.btnWebsiteBlocking);
        this.btnFeatureAppUsage = findViewById(R.id.btnAppUsage);

        this.btnFeatures = new MaterialButton[4];
        this.btnFeatures[0] = this.btnFeatureWarningDetection;
        this.btnFeatures[1] = this.btnFeatureAppBlocking;
        this.btnFeatures[2] = this.btnFeatureWebsiteBlocking;
        this.btnFeatures[3] = this.btnFeatureAppUsage;

        this.imgViewRename.setOnClickListener(this);
        this.txtViewRename.setOnClickListener(this);

        this.btnSleep.setOnClickListener(this);

        this.btnSettings.setOnClickListener(this);

        this.btnFeatureWarningDetection.setOnClickListener(this);
        this.btnFeatureAppBlocking.setOnClickListener(this);
        this.btnFeatureWebsiteBlocking.setOnClickListener(this);
        this.btnFeatureAppUsage.setOnClickListener(this);

        //this.surfaceView = findViewById(R.id.testingSf);

        update();
        refreshDeviceName();



    }

    private void updateFrameFeatures(String selectedFeature){

        int selected = -1;

        switch (selectedFeature){

            case ("Warning Detection"):
                selectFeature(new WarningDetectionFragment());
                selected = 0;
                break;

            case ("App Blocking"):
                selectFeature(new AppBlockingFragment());
                selected = 1;
                break;

            case ("Website Blocking"):
                selectFeature(new WebsiteBlockingFragment());
                selected = 2;
                break;

            case ("App Usage"):
                selectFeature(new AppUsageFragment());
                selected = 3;
                break;

        }

        for (int i = 0; i < this.btnFeatures.length; i++){

            if (selected == i){

                this.btnFeatures[i].setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.icon_selected)));
                this.btnFeatures[i].setTextColor(getResources().getColor(R.color.icon_selected));

            }
            else {

                this.btnFeatures[i].setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.icon_not_selected)));
                this.btnFeatures[i].setTextColor(getResources().getColor(R.color.icon_not_selected));

            }

        }

    }

    private void selectFeature(Fragment fragment){

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameFeatures, fragment);
        fragmentTransaction.commit();

    }

/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DRAW_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                // Overlay permission granted
            } else {
                // Overlay permission denied
            }
        } else if (requestCode == REQUEST_CODE_USAGE_ACCESS) {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
            if (mode == AppOpsManager.MODE_ALLOWED) {
                // Usage access permission granted
            } else {
                // Usage access permission denied
            }
        }
    }*/

/*
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_CODE) {
            if (resultCode == RESULT_OK) {
                // Get the MediaProjection
                this.hasAllowedScreenCapture = true;
                boolean hasAllowedScreenCapture = false; // FORCED CODE
                int screenCaptureResultCode = resultCode;
                String serializedIntent = data.toUri(Intent.URI_INTENT_SCHEME);

                startService(resultCode, data);


                SharedPreferences.Editor editor = condecPreferences.edit();
                editor.putBoolean("hasAllowedScreenCapture", hasAllowedScreenCapture);
                editor.putInt("screenCaptureResultCode", screenCaptureResultCode);
                editor.putString("savedScreenCaptureIntent", serializedIntent);
                editor.apply();

                // Continue with using the mediaProjection object
            } else {
                // User denied permission
                this.hasAllowedScreenCapture = false;
            }
        }
    }
*/

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void showRenameDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your name of this Device");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String userInput = input.getText().toString();
            // Handle the user input here
            renameDevice(userInput);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showMandatoryRenameDeviceDialog() {
        // Check if a device name is already saved
        String currentDeviceName = condecPreferences.getString("deviceName", "My Device");

        // If a device name is already set, start the services directly
        if (currentDeviceName != null && !currentDeviceName.isEmpty() && currentDeviceName != "My Device") {
            startRequiredServices();
            return;
        }

        // If no device name is set, show the mandatory dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your device's name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setCancelable(false); // Disable canceling the dialog

        builder.setPositiveButton("OK", (dialog, which) -> {
            String userInput = input.getText().toString().trim();
            if (!userInput.isEmpty()) {
                renameDevice(userInput);
                startRequiredServices(); // Start services after the name is provided
            } else {
                Toast.makeText(MainMenuActivity.this, "Device name cannot be empty!", Toast.LENGTH_SHORT).show();
                showMandatoryRenameDeviceDialog(); // Show the dialog again if the input is empty
            }
        });

        builder.show();
    }

    private void renameDevice(String name) {
        SharedPreferences.Editor editor = this.condecPreferences.edit();
        editor.putString("oldDeviceName", this.txtViewRename.toString());
        editor.putString("deviceName", name);
        editor.apply();
        Toast.makeText(MainMenuActivity.this, "Device Renamed: " + name, Toast.LENGTH_SHORT).show();
        refreshDeviceName();

        // Notify discovery process or UI to update the device list
        refreshDeviceListWithNewName();
        restartParentalService();

    }

    private void refreshDeviceListWithNewName() {
        // If the discovery process is ongoing, re-run or update the list
        if (condecParentalService != null) {
            condecParentalService.refreshDiscoveredDevices();
        }

        // Optionally, directly update the UI here if necessary
        // updateDeviceListInUI();
    }

    private void refreshDeviceName(){

        String name = this.condecPreferences.getString("deviceName", "My Device");
        this.txtViewRename.setText(name);

    }

    private void showSleepSettings(){

        SleepControlDialog sleepControlDialog = new SleepControlDialog();
        sleepControlDialog.setTrigger(this);
        sleepControlDialog.show(getSupportFragmentManager(), "Sleep Settings");

    }

    private void toggleSleepMode(){

        Intent intent = new Intent(MainMenuActivity.this, CondecSleepService.class);


        if (this.isToggleSleep == true){

            this.isToggleSleep = false;
            stopService(intent);


        }
        else if (this.isToggleSleep == false){

            this.isToggleSleep = true;
            startService(intent);


        }
        Log.d("Condec Sleep", "Sleep Mode Status: " + this.isToggleSleep);

        update();
    }

    private void restartParentalService(){

        // Stop the service
        Intent stopIntent = new Intent(this, CondecParentalService.class);
        stopService(stopIntent);

        // Start the service
        Intent startIntent = new Intent(this, CondecParentalService.class);
        startForegroundService(startIntent);

    }

    private void update(){

        if (this.isToggleSleep == true){

            this.btnSleep.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.sleep_icon_on)));
            this.btnSleep.setTextColor(getResources().getColor(R.color.sleep_icon_on));
            this.btnSleep.setText("Sleep On");
            Log.d("Condec Sleep", "Sleep Mode Icon: ON");
        }
        else if (this.isToggleSleep == false){

            this.btnSleep.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.sleep_icon_off)));
            this.btnSleep.setTextColor(getResources().getColor(R.color.sleep_icon_off));
            this.btnSleep.setText("Sleep Off");
            Log.d("Condec Sleep", "Sleep Mode Icon: OFF");

        }

    }

    @Override
    public void onBackPressed() {

        if (false){

            super.onBackPressed();

        }
    }

    @Override
    public void onClick(View view) {

        if (this.btnSettings == view){

            Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();

        }
        if (this.imgViewRename == view || this.txtViewRename == view){

            showRenameDeviceDialog();

        }
        if (this.btnSleep == view){

            showSleepSettings();
            return;
        }
        if (this.btnFeatureWarningDetection == view){

            updateFrameFeatures("Warning Detection");
            return;
        }
        if (this.btnFeatureAppBlocking == view){

            updateFrameFeatures("App Blocking");
            return;

        }if (this.btnFeatureWebsiteBlocking == view){

            updateFrameFeatures("Website Blocking");
            return;
        }
        if (this.btnFeatureAppUsage == view){

            updateFrameFeatures("App Usage");
            return;

        }

    }

}