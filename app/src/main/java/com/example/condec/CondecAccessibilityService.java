package com.example.condec;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.condec.Utils.AccessibilityUtils;

public class CondecAccessibilityService extends AccessibilityService {

    private int screenHeight;
    private int screenWidth;
    private BroadcastReceiver swipeAndBackReceiver;

    private BroadcastReceiver goBackReceiver;
    private BroadcastReceiver goHomeReceiver;
    private boolean isSwippingAndBacking = false;
    private boolean isBacking = false;
    private boolean isSwipping = false;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

        Log.d("CondecAccessibilityService", "Accessibility Service Connected");
        getScreenDimensions();

        swipeAndBackReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.ACTION_SWIPE_AND_BACK".equals(intent.getAction())) {
                    performSwipeAndBack();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.ACTION_SWIPE_AND_BACK");
        registerReceiver(swipeAndBackReceiver, filter);

        goBackReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.ACTION_GO_BACK".equals(intent.getAction())) {
                    goBack();
                }
            }
        };

        goHomeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.ACTION_GO_HOME".equals(intent.getAction())) {
                    goHome();
                }
            }
        };

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("com.example.ACTION_GO_HOME");
        registerReceiver(goHomeReceiver, filter2);

        IntentFilter filter3 = new IntentFilter();
        filter3.addAction("com.example.ACTION_GO_BACK");
        registerReceiver(goBackReceiver, filter3);
    }

    @Override
    public void onInterrupt() {
        Log.d("CondecAccessibilityService", "Service Interrupted");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d("CondecAccessibilityService", "Received Accessibility Event: " + event.toString());
    }

    private void swipeDown() {

        if(this.isSwipping){

            return;

        }

        this.isSwipping = true;
        Log.d("CondecAccessibilityService", "Attempting to Swipe Down1");
        try {
            Log.d("CondecAccessibilityService", "Screen Width: " + screenWidth + ", Screen Height: " + screenHeight);
            if (screenWidth == 0 || screenHeight == 0) {
                Log.e("CondecAccessibilityService", "Screen dimensions are not initialized.");
                return;
            }

            dispatchGesture(AccessibilityUtils.getSwipeUpGesture(this.screenWidth, this.screenHeight), null, null);
        }
        catch (Exception e){

            Log.d("CondecAccessibilityService", "Swipe ERROR: " + e);

        }
    }

    private void pressBackButtonWithDelay() {

        if(this.isBacking){

            return;

        }

        this.isBacking = true;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d("CondecAccessibilityService", "Attempting to press back button");
            boolean result = performGlobalAction(GLOBAL_ACTION_BACK);

            if (!result) {
                Log.e("CondecAccessibilityService", "Failed to press back button");
            }
        }, 750);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            this.isSwipping = false;
            this.isBacking = false;
            this.isSwippingAndBacking = false;

        }, 2000);


    }

    public void performSwipeAndBack() {
        Log.d("CondecAccessibilityService", "Performing Swipe Down and Pressing Back Button");

        if(this.isSwippingAndBacking == false && this.isSwipping == false && this.isBacking == false){

            this.isSwippingAndBacking = true;

        }

        // Perform swipe first
        if(isSwippingAndBacking == true){

            swipeDown();
            pressBackButtonWithDelay();

        }

    }

    public void goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }
    public void goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private void getScreenDimensions() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            screenWidth = displayMetrics.widthPixels;
            screenHeight = displayMetrics.heightPixels;

            Log.d("CondecAccessibilityService", "Screen Width: " + screenWidth + ", Screen Height: " + screenHeight);
        } else {
            Log.e("CondecAccessibilityService", "Unable to get WindowManager service");
        }
    }

}
