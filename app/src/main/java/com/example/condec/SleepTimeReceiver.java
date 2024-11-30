package com.example.condec;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

public class SleepTimeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        boolean alarmSetForToday = preferences.getBoolean("alarmSetForToday", false);

        if (!alarmSetForToday) {
            scheduleService(context);

            preferences.edit().putBoolean("alarmSetForToday", true).apply();

            Log.d("SleepTimeReceiver", "Alarms scheduled for the day.");
        }

        scheduleMidnightChecker(context);
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleService(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        SharedPreferences preferences = context.getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        long startTime = preferences.getLong("sleepStartTime", -1);
        long endTime = preferences.getLong("sleepEndTime", -1);

        if (startTime == -1 || endTime == -1) {
            Log.e("SleepTimeReceiver", "Start or end time not set.");
            return;
        }

        Intent startIntent = new Intent(context, CondecSleepService.class);
        startIntent.setAction("START_SERVICE");
        PendingIntent startPendingIntent = PendingIntent.getForegroundService(context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(context, CondecSleepService.class);
        stopIntent.setAction("STOP_SERVICE");
        PendingIntent stopPendingIntent = PendingIntent.getForegroundService(context, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, startPendingIntent);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, stopPendingIntent);

        Log.d("SleepTimeReceiver", "Service scheduled between: " + startTime + " and " + endTime);
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleMidnightChecker(Context context) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent midnightIntent = new Intent(context, SleepTimeReceiver.class);
        PendingIntent midnightPendingIntent = PendingIntent.getBroadcast(context, 0, midnightIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar midnightCalendar = Calendar.getInstance();
        midnightCalendar.set(Calendar.HOUR_OF_DAY, 0);
        midnightCalendar.set(Calendar.MINUTE, 0);
        midnightCalendar.set(Calendar.SECOND, 0);
        midnightCalendar.add(Calendar.DAY_OF_MONTH, 1);

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnightCalendar.getTimeInMillis(), midnightPendingIntent);

        Log.d("SleepTimeReceiver", "Midnight checker scheduled for: " + midnightCalendar.getTime());
    }
}

