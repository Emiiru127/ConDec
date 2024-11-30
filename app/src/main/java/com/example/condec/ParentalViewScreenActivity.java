package com.example.condec;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ParentalViewScreenActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageButton btnBackViewScreen;
    private ImageView imgViewScreen;
    private CondecParentalService parentalService;

    private BroadcastReceiver imageUpdateReceiver;

    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CondecParentalService.LocalBinder binder = (CondecParentalService.LocalBinder) service;
            parentalService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_parental_view_screen);

        imgViewScreen = findViewById(R.id.imgViewScreen);
        Intent intent = new Intent(this, CondecParentalService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        this.btnBackViewScreen = findViewById(R.id.btnBackViewScreen);
        this.btnBackViewScreen.setOnClickListener(this);

        // Register BroadcastReceiver to update image
        imageUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] imageData = intent.getByteArrayExtra("image_data");
                if (imageData != null && imageData.length > 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                    if (!bitmap.equals(imgViewScreen.getDrawable())) { // Avoid unnecessary re-renders
                        imgViewScreen.setImageBitmap(bitmap);
                    }
                }
            }
        };

        // Register the receiver for image updates
        LocalBroadcastManager.getInstance(this).registerReceiver(
                imageUpdateReceiver, new IntentFilter("com.example.condec.IMAGE_UPDATE"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
        }

        // Notify CondecParentalService to stop image requests
        Intent stopRequestIntent = new Intent("com.example.condec.STOP_IMAGE_REQUESTS");
        LocalBroadcastManager.getInstance(this).sendBroadcast(stopRequestIntent);

        // Unregister the receiver to prevent memory leaks
        if (imageUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(imageUpdateReceiver);
        }

    }

    @Override
    public void onClick(View view) {
        if (view == this.btnBackViewScreen) {
            finish();
        }
    }
}
