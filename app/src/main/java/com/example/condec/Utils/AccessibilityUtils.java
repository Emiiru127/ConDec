package com.example.condec.Utils;

import android.accessibilityservice.GestureDescription;
import android.graphics.Path;

public class AccessibilityUtils {

    public static GestureDescription getSwipeUpGesture(int screenWidth, int screenHeight) {

        Path swipePath = new Path();
        swipePath.moveTo(screenWidth / 2, screenHeight * 0.85f);
        swipePath.lineTo(screenWidth / 2, screenHeight * 0.15f);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 100)); // Duration: 500ms
        return gestureBuilder.build();
    }
}
