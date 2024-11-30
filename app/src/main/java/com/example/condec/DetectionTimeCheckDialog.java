package com.example.condec;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DetectionTimeCheckDialog extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawableResource(R.color.blue_main_background);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("The ConDec detection service has been running for 2 hours. Do you want to stop it?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent passwordIntent = new Intent(DetectionTimeCheckDialog.this, PasswordPromptActivity.class);
                        passwordIntent.putExtra("isForResult", true);
                        startActivityForResult(passwordIntent, 1);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent homeIntent = new Intent("com.example.ACTION_GO_HOME");
                        sendBroadcast(homeIntent);
                        finish();
                    }
                })
                .setCancelable(false)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.blue_main_background));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.blue_main_background));

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            boolean passwordCorrect = data.getBooleanExtra("password_correct", false);
            if (passwordCorrect) {
                finish();
            }
        }
        finish();
    }

}
