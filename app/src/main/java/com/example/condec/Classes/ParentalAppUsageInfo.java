package com.example.condec.Classes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayOutputStream;

public class ParentalAppUsageInfo implements Parcelable {

    private String packageName;
    private String appName;
    private Bitmap appIconBitmap;  // Use Bitmap instead of Drawable for Parcelable
    private long usageTime;
    private long lastTimeUsed;

    public ParentalAppUsageInfo(String packageName, String appName, Drawable appIcon, long usageTime, long lastTimeUsed) {
        this.packageName = packageName;
        this.appName = appName;
        this.appIconBitmap = drawableToBitmap(appIcon);  // Convert Drawable to Bitmap
        this.usageTime = usageTime;
        this.lastTimeUsed = lastTimeUsed;
    }

    // Convert Drawable to Bitmap
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    public String getPackageName() {
        return packageName;
    }
    public String getAppName() {
        return appName;
    }
    public Drawable getAppIcon() {
        return new BitmapDrawable(null, appIconBitmap);  // Convert Bitmap back to Drawable
    }
    public long getUsageTime() {
        return usageTime;
    }
    public long getLastTimeUsed() {
        return lastTimeUsed;
    }

    // Parcelable implementation
    protected ParentalAppUsageInfo(Parcel in) {
        packageName = in.readString();
        appName = in.readString();
        byte[] iconBytes = in.createByteArray();
        appIconBitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
        usageTime = in.readLong();
        lastTimeUsed = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeString(appName);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        appIconBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        dest.writeByteArray(stream.toByteArray());
        dest.writeLong(usageTime);
        dest.writeLong(lastTimeUsed);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ParentalAppUsageInfo> CREATOR = new Creator<ParentalAppUsageInfo>() {
        @Override
        public ParentalAppUsageInfo createFromParcel(Parcel in) {
            return new ParentalAppUsageInfo(in);
        }

        @Override
        public ParentalAppUsageInfo[] newArray(int size) {
            return new ParentalAppUsageInfo[size];
        }
    };
}