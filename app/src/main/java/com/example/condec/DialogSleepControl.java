package com.example.condec;

import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class DialogSleepControl extends DialogFragment {

    private Button btnSetStartTime;
    private Button btnSetEndTime;
    private Switch switchUseTime;
    private Switch switchManual;

    private SharedPreferences sharedPreferences;
    private AlarmManager alarmManager;
    private PendingIntent startPendingIntent, stopPendingIntent;

    private Calendar startTimeCalendar = Calendar.getInstance();
    private Calendar endTimeCalendar = Calendar.getInstance();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate the custom layout
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_sleep_controls, null);

        builder.setView(view);

        // Find views from the dialog layout
        this.btnSetStartTime = view.findViewById(R.id.btnSetStartTime);
        this.btnSetEndTime = view.findViewById(R.id.btnSetEndTime);
        this.switchUseTime = view.findViewById(R.id.switchUseTime);
        this.switchManual = view.findViewById(R.id.switchManual);
        Button doneButton = view.findViewById(R.id.btnDoneSleep);

        // Set button click listener for Done
        doneButton.setOnClickListener(v -> dismiss());

        // Initialize the AlarmManager
        alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

        // Load stored times or set defaults
        sharedPreferences = getActivity().getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        long startMillis = sharedPreferences.getLong("sleepStartTime", -1);
        long endMillis = sharedPreferences.getLong("sleepEndTime", -1);

        boolean useTimeOn = sharedPreferences.getBoolean("sleepUseTimeOn", false);
        boolean manualOn = sharedPreferences.getBoolean("sleepManualOn", false);

        // Set the switch states
        switchUseTime.setChecked(useTimeOn);
        switchManual.setChecked(manualOn);

        if (startMillis == -1 || endMillis == -1) {
            // Set default times (10 PM to 7 AM)
            startTimeCalendar.set(Calendar.HOUR_OF_DAY, 22);
            startTimeCalendar.set(Calendar.MINUTE, 0);
            startTimeCalendar.set(Calendar.SECOND, 0);
            startTimeCalendar.set(Calendar.MILLISECOND, 0);

            endTimeCalendar.set(Calendar.HOUR_OF_DAY, 7);
            endTimeCalendar.set(Calendar.MINUTE, 0);
            endTimeCalendar.set(Calendar.SECOND, 0);
            endTimeCalendar.set(Calendar.MILLISECOND, 0);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("sleepStartTime", startTimeCalendar.getTimeInMillis());
            editor.putLong("sleepEndTime", endTimeCalendar.getTimeInMillis());
            editor.apply();

        } else {
            // Load saved times
            startTimeCalendar.setTimeInMillis(startMillis);
            endTimeCalendar.setTimeInMillis(endMillis);
        }

        // Update button texts
        btnSetStartTime.setText(formatTime(startTimeCalendar));
        btnSetEndTime.setText(formatTime(endTimeCalendar));

        // Set button click listeners for time settings
        btnSetStartTime.setOnClickListener(v -> showTimePickerDialog(true));
        btnSetEndTime.setOnClickListener(v -> showTimePickerDialog(false));

        // Handle switch toggles
        switchUseTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchManual.setChecked(false);
                scheduleService();
            } else {
                if (!switchManual.isChecked()) {
                    cancelScheduledService();
                }
            }
            saveSwitchStates();
        });

        switchManual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchUseTime.setChecked(false);
                startServiceManually();
            } else {
                stopServiceManually();
                if (!switchUseTime.isChecked()) {
                    cancelScheduledService();
                }
            }
            saveSwitchStates();
        });

        return builder.create();
    }

    private void saveSwitchStates() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("sleepUseTimeOn", switchUseTime.isChecked());
        editor.putBoolean("sleepManualOn", switchManual.isChecked());
        editor.apply();
    }
    private String formatTime(Calendar calendar) {
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return String.format("%02d:%02d %s",
                (hourOfDay % 12 == 0 ? 12 : hourOfDay % 12),
                minute,
                (hourOfDay >= 12) ? "PM" : "AM");
    }

    private void showTimePickerDialog(boolean isStartTime) {
        // Get the current time or the previously set time from the respective Calendar
        Calendar calendar = isStartTime ? startTimeCalendar : endTimeCalendar;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Create and show a TimePickerDialog with the current hour and minute
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getActivity(),
                (view, hourOfDay, selectedMinute) -> {
                    // Update the corresponding calendar with the selected time
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    calendar.set(Calendar.SECOND, 0); // Reset seconds to 0 for consistency

                    // Update the button text with the formatted time
                    String time = String.format("%02d:%02d %s",
                            (hourOfDay % 12 == 0 ? 12 : hourOfDay % 12),
                            selectedMinute,
                            (hourOfDay >= 12) ? "PM" : "AM");

                    if (isStartTime) {
                        btnSetStartTime.setText(time);
                        startTimeCalendar = calendar;
                    } else {
                        btnSetEndTime.setText(time);
                        endTimeCalendar = calendar;
                    }

                    // Save the time in SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if (isStartTime) {
                        editor.putLong("sleepStartTime", startTimeCalendar.getTimeInMillis());
                    } else {
                        editor.putLong("sleepEndTime", endTimeCalendar.getTimeInMillis());
                    }
                    editor.apply();
                },
                hour,
                minute,
                false // Set to true if you want a 24-hour time format
        );

        timePickerDialog.show();
    }

    private void scheduleService() {
        if (startTimeCalendar == null || endTimeCalendar == null) {
            Toast.makeText(getActivity(), "Please set both start and end times.", Toast.LENGTH_SHORT).show();
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Schedule the service to start
        Intent startIntent = new Intent(getActivity(), CondecSleepService.class);
        startPendingIntent = PendingIntent.getService(getActivity(), 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Schedule the service to stop
        Intent stopIntent = new Intent(getActivity(), CondecSleepService.class);
        stopIntent.setAction("STOP_SERVICE");
        stopPendingIntent = PendingIntent.getService(getActivity(), 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Log.d("DialogSleepControl", "Start time in millis: " + startTimeCalendar.getTimeInMillis());
        Log.d("DialogSleepControl", "End time in millis: " + endTimeCalendar.getTimeInMillis());
        Log.d("DialogSleepControl", "Current time: " + currentTime);

        // Handle case where end time is earlier than start time (next day)
        if (endTimeCalendar.getTimeInMillis() < startTimeCalendar.getTimeInMillis()) {
            endTimeCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTimeCalendar.getTimeInMillis(), startPendingIntent);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeCalendar.getTimeInMillis(), stopPendingIntent);

        // Handle case where end time is earlier than current time
        if (endTimeCalendar.getTimeInMillis() < currentTime) {
            stopServiceManually();
        } else if (currentTime > startTimeCalendar.getTimeInMillis()) {
            startServiceManually();
        }
    }

    private void cancelScheduledService() {
        // Cancel the scheduled service
        if (alarmManager != null) {
            if (startPendingIntent != null) {
                Log.d("DialogSleepControl", "Cancelling scheduled start service.");
                alarmManager.cancel(startPendingIntent);
            }
            if (stopPendingIntent != null) {
                Log.d("DialogSleepControl", "Cancelling scheduled stop service.");
                alarmManager.cancel(stopPendingIntent);
            }
        }
    }

    private void startServiceManually() {
        // Start the service immediately
        Log.d("DialogSleepControl", "Starting service manually.");
        Intent startIntent = new Intent(getActivity(), CondecSleepService.class);
        getActivity().startForegroundService(startIntent);
    }

    private void stopServiceManually() {
        // Stop the service immediately
        Log.d("DialogSleepControl", "Stopping service manually.");
        Intent stopIntent = new Intent(getActivity(), CondecSleepService.class);
        getActivity().stopService(stopIntent);
    }
}