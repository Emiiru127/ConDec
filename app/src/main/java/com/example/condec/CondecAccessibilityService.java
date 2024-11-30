package com.example.condec;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class CondecAccessibilityService extends AccessibilityService {

    private int screenHeight;
    private int screenWidth;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("CondecAccessibilityService", "Accessibility Service Connected");
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
        Log.d("CondecAccessibilityService", "Attempting to Swipe Down1");
        try {
            Log.d("CondecAccessibilityService", "Screen Width: " + screenWidth + ", Screen Height: " + screenHeight);
            if (screenWidth == 0 || screenHeight == 0) {
                Log.e("CondecAccessibilityService", "Screen dimensions are not initialized.");
                return;
            }

            // Define the path for swipe down gesture
            Path swipePath = new Path();
            swipePath.moveTo(screenWidth / 2, screenHeight * 0.25f);
            swipePath.lineTo(screenWidth / 2, screenHeight * 0.75f);

            // Create the gesture stroke (duration of 500ms)
            GestureDescription.StrokeDescription strokeDescription = new GestureDescription.StrokeDescription(swipePath, 0, 500);
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(strokeDescription);

            // Dispatch the gesture
            boolean result = dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d("CondecAccessibilityService", "Swipe down completed");

                    // After swipe completes, press back button
                    pressBackButtonWithDelay();
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.d("CondecAccessibilityService", "Swipe down cancelled");
                }
            }, null);

            if (!result) {
                Log.e("CondecAccessibilityService", "Failed to dispatch swipe down gesture");
            }
        } catch (Exception e) {
            Log.e("CondecAccessibilityService", "ERROR: " + e);
        }
    }

    private void pressBackButtonWithDelay() {
        // Post a delayed action on the main thread to allow the swipe to complete
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d("CondecAccessibilityService", "Attempting to press back button");
            boolean result = performGlobalAction(GLOBAL_ACTION_BACK);

            if (!result) {
                Log.e("CondecAccessibilityService", "Failed to press back button");
            }
        }, 500); // 500ms delay to allow the swipe to complete before pressing back
    }

    public void performSwipeAndBack() {
        Log.d("CondecAccessibilityService", "Performing Swipe Down and Pressing Back Button");

        // Perform swipe first
        swipeDown();

        pressBackButtonWithDelay();
    }

    // If you want to scroll using ACTION_SCROLL_BACKWARD
    public void scrollDown() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            AccessibilityNodeInfo scrollableNode = findScrollableNode(rootNode);
            if (scrollableNode != null) {
                scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            }
        }
    }

    private AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo node) {
        if (node.isScrollable()) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo scrollableNode = findScrollableNode(child);
                if (scrollableNode != null) {
                    return scrollableNode;
                }
            }
        }
        return null;
    }

    public void setScreenDimensions(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }
}
