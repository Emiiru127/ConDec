package com.example.condec;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.List;

public class SleepControlDialog extends DialogFragment {

    private Button btnSetStartTime;
    private Button btnSetEndTime;
    private Switch switchUseTime;
    private Switch switchManual;

    private SharedPreferences sharedPreferences;
    private AlarmManager alarmManager;
    private PendingIntent startPendingIntent, stopPendingIntent;

    private Calendar startTimeCalendar = Calendar.getInstance();
    private Calendar endTimeCalendar = Calendar.getInstance();

    private boolean isParentalControl;
    private boolean isTrigger;
    private MainMenuActivity mainMenuActivity;

    private ParentalControlActivity parentalControlActivity;

    private List<String> sleepData;

    public  SleepControlDialog(){

        this.isTrigger = false;
        this.isParentalControl = false;

    }

    public SleepControlDialog(ParentalControlActivity parentalControlActivity, List<String> sleepData){

        this.isTrigger = false;
        this.isParentalControl = true;

        this.parentalControlActivity = parentalControlActivity;
        this.sleepData = sleepData;

    }

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

        if (this.isParentalControl) {
            initializeParentalUI();
        } else {
            initializeStandardUI();
        }

        // Set initial text based on switch states
        updateSwitchText();

        return builder.create();
    }

    private void initializeParentalUI() {
        boolean useTimeOn = Boolean.parseBoolean((this.sleepData.get(1)).split(":")[1]);
        boolean manualOn = Boolean.parseBoolean((this.sleepData.get(2)).split(":")[1]);

        long startMillis = Long.parseLong((this.sleepData.get(3)).split(":")[1]);
        long endMillis = Long.parseLong((this.sleepData.get(4)).split(":")[1]);

        // Set the switch states
        switchUseTime.setChecked(useTimeOn);
        switchManual.setChecked(manualOn);

        startTimeCalendar.setTimeInMillis(startMillis);
        endTimeCalendar.setTimeInMillis(endMillis);

        // Update button texts
        btnSetStartTime.setText(formatTime(startTimeCalendar));
        btnSetEndTime.setText(formatTime(endTimeCalendar));

        // Set button click listeners for time settings
        btnSetStartTime.setOnClickListener(v -> showTimePickerDialog(true));
        btnSetEndTime.setOnClickListener(v -> showTimePickerDialog(false));

        // Handle switch toggles
        switchUseTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchManual.setChecked(false);
            this.parentalControlActivity.sendSleepCommandToDevice(isChecked ? "TIMED_BASED" : "CANCEL_SCHEDULED_SLEEP", "true");
            checkAndStopService();
            updateSwitchText();
        });

        switchManual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchUseTime.setChecked(false);
            this.parentalControlActivity.sendSleepCommandToDevice(isChecked ? "SLEEP_OVERRIDE" : "CANCEL_SCHEDULED_SLEEP", "true");
            checkAndStopService();
            updateSwitchText();
        });
    }

    private void initializeStandardUI() {
        alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

        sharedPreferences = getActivity().getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        long startMillis = sharedPreferences.getLong("sleepStartTime", -1);
        long endMillis = sharedPreferences.getLong("sleepEndTime", -1);

        boolean useTimeOn = sharedPreferences.getBoolean("sleepUseTimeOn", false);
        boolean manualOn = sharedPreferences.getBoolean("sleepManualOn", false);

        switchUseTime.setChecked(useTimeOn);
        switchManual.setChecked(manualOn);

        if (startMillis == -1 || endMillis == -1) {
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
            startTimeCalendar.setTimeInMillis(startMillis);
            endTimeCalendar.setTimeInMillis(endMillis);
        }

        btnSetStartTime.setText(formatTime(startTimeCalendar));
        btnSetEndTime.setText(formatTime(endTimeCalendar));

        btnSetStartTime.setOnClickListener(v -> showTimePickerDialog(true));
        btnSetEndTime.setOnClickListener(v -> showTimePickerDialog(false));

        switchUseTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchManual.setChecked(false);
            scheduleService();
            checkAndStopService();
            updateSwitchText();
        });

        switchManual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchUseTime.setChecked(false);
            if (isChecked) {
                startServiceManually();
            } else {
                stopServiceManually();
                if (!switchUseTime.isChecked()) {
                    cancelScheduledService();
                }
            }
            checkAndStopService();
            updateSwitchText();
        });
    }

    // Check if both switches are off and stop the service
    private void checkAndStopService() {
        if (!switchUseTime.isChecked() && !switchManual.isChecked()) {
            Log.d("SleepControlDialog", "Both switches are OFF. Stopping service.");
            stopServiceManually();
        }
    }

    private void updateSwitchText() {
        // Update the switch texts based on the state
        switchUseTime.setText(switchUseTime.isChecked() ? "ON" : "OFF");
        switchManual.setText(switchManual.isChecked() ? "ON" : "OFF");
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
        Calendar calendar = isStartTime ? startTimeCalendar : endTimeCalendar;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getActivity(),
                (view, hourOfDay, selectedMinute) -> {
                    // Set the calendar to today's date to avoid date misalignment
                    Calendar today = Calendar.getInstance();
                    calendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, today.get(Calendar.MONTH));
                    calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));

                    // Update the time
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);

                    // Update the button text
                    String time = formatTime(calendar);
                    if (isStartTime) {
                        btnSetStartTime.setText(time);
                        startTimeCalendar = calendar;

                        if (isParentalControl == true){

                            this.parentalControlActivity.sendSleepCommandToDevice("SET_SLEEP_START_TIME", Long.toString(startTimeCalendar.getTimeInMillis()));

                        }

                    } else {
                        btnSetEndTime.setText(time);
                        endTimeCalendar = calendar;

                        if (isParentalControl == true){

                            this.parentalControlActivity.sendSleepCommandToDevice("SET_SLEEP_END_TIME", Long.toString(endTimeCalendar.getTimeInMillis()));

                        }

                    }

                    if (isParentalControl == false){

                        // Save the new time in SharedPreferences
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        if (isStartTime) {
                            editor.putLong("sleepStartTime", startTimeCalendar.getTimeInMillis());
                        } else {
                            editor.putLong("sleepEndTime", endTimeCalendar.getTimeInMillis());
                        }
                        editor.apply();

                        // Reschedule service with updated times
                        if (switchUseTime.isChecked()) {
                            scheduleService();
                        }

                    }
                },
                hour, minute, false
        );

        timePickerDialog.show();
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleService() {

        if (startTimeCalendar == null || endTimeCalendar == null) {
            Toast.makeText(getActivity(), "Please set both start and end times.", Toast.LENGTH_SHORT).show();
            return;
        }

        long currentTime = System.currentTimeMillis();
        long startTime = startTimeCalendar.getTimeInMillis();
        long endTime = endTimeCalendar.getTimeInMillis();

        // If end time is earlier than start time, add a day to the end time
        if (endTime <= startTime) {
            endTimeCalendar.add(Calendar.DAY_OF_MONTH, 1);
            endTime = endTimeCalendar.getTimeInMillis();
            Log.d("SleepControlDialog", "End time adjusted to the next day: " + endTimeCalendar.getTime());
        }

        // Print times in human-readable format
        Log.d("SleepControlDialog", "Start time (human-readable): " + startTimeCalendar.getTime());
        Log.d("SleepControlDialog", "End time (human-readable): " + endTimeCalendar.getTime());
        Log.d("SleepControlDialog", "Current time (human-readable): " + Calendar.getInstance().getTime());

        Intent startIntent = new Intent(getActivity(), CondecSleepService.class);
        startIntent.setAction("START_SERVICE");
        startPendingIntent = PendingIntent.getForegroundService(
                getActivity(), 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(getActivity(), CondecSleepService.class);
        stopIntent.setAction("STOP_SERVICE");
        stopPendingIntent = PendingIntent.getForegroundService(
                getActivity(), 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Cancel existing alarms before rescheduling
        if (alarmManager != null) {
            alarmManager.cancel(startPendingIntent);
            alarmManager.cancel(stopPendingIntent);
        }

        // Schedule the service start and stop
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, startPendingIntent);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, stopPendingIntent);

        // Check if service should be started immediately
        Log.d("SleepControlDialog", "Checking if service should start or stop.");
        if (currentTime >= startTime && currentTime < endTime) {
            Log.d("SleepControlDialog", "Starting service manually.");
            startServiceManually();
        } else {
            Log.d("SleepControlDialog", "Stopping service manually.");
            stopServiceManually();
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

    public void setTrigger(MainMenuActivity mainMenuActivity){

        this.isTrigger = true;
        this.mainMenuActivity = mainMenuActivity;

    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if(this.isTrigger){

            this.mainMenuActivity.checkSleepService();

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