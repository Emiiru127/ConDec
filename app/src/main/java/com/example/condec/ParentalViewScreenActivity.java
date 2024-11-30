package com.example.condec;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ParentalViewScreenActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView imgViewScreen;
    private CondecParentalService parentalService;
    private ImageButton btnBackViewScreen;
    private boolean isBound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable updateImageTask = new Runnable() {
        @Override
        public void run() {
            if (isBound && parentalService != null) {
                Bitmap latestImage = parentalService.getLatestImage(); // Directly get Bitmap
                if (latestImage != null) {
                    imgViewScreen.setImageBitmap(latestImage); // Display the Bitmap
                    Log.d("ParentalViewScreen", "Image updated successfully.");
                } else {
                    Log.d("ParentalViewScreen", "No image available from parental service.");
                }
            }
            handler.postDelayed(this, 1000); // Refresh every second
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CondecParentalService.LocalBinder binder = (CondecParentalService.LocalBinder) service;
            parentalService = binder.getService();
            isBound = true;
            handler.post(updateImageTask); // Start updating the image
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            handler.removeCallbacks(updateImageTask); // Stop updates
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_parental_view_screen);

        imgViewScreen = findViewById(R.id.imgViewScreen);
        btnBackViewScreen = findViewById(R.id.btnBackViewScreen);
        btnBackViewScreen.setOnClickListener(this);

        Intent intent = new Intent(this, CondecParentalService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacks(updateImageTask);
    }

    @Override
    public void onClick(View view) {
        if (view == btnBackViewScreen) {
            finish();
        }
    }
}
