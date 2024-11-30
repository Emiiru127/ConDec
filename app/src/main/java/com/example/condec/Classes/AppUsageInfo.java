package com.example.condec.Classes;

import android.graphics.drawable.Drawable;

public class AppUsageInfo {

    private String packageName;
    private String appName;
    private Drawable appIcon;
    private long usageTime;
    private long lastTimeUsed;

    public AppUsageInfo(String packageName, String appName, Drawable appIcon, long usageTime, long lastTimeUsed) {
        this.packageName = packageName;
        this.appName = appName;
        this.appIcon = appIcon;
        this.usageTime = usageTime;
        this.lastTimeUsed = lastTimeUsed;
    }

    public String getPackageName() {
        return packageName;
    }
    public String getAppName() {
        return appName;
    }
    public Drawable getAppIcon() {
        return appIcon;
    }
    public long getUsageTime() {
        return usageTime;
    }
    public long getLastTimeUsed() {
        return lastTimeUsed;
    }

}
