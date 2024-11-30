package com.example.condec;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class DetectionTimeCheckDialog extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new AlertDialog.Builder(this)
                .setTitle("Service Running for 2 Hours")
                .setMessage("The service has been running for 2 hours. Do you want to stop it?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Start PasswordPromptActivity to validate password
                        Intent passwordIntent = new Intent(DetectionTimeCheckDialog.this, PasswordPromptActivity.class);
                        passwordIntent.putExtra("isForResult", true);
                        startActivityForResult(passwordIntent, 1);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Just dismiss the dialog and keep the service running
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            boolean passwordCorrect = data.getBooleanExtra("password_correct", false);
            if (passwordCorrect) {
                // Broadcast to stop the service if the password is correct
                finish();

            }
        }
        finish(); // Close the activity
    }

}
