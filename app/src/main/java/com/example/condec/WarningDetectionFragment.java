package com.example.condec;

import static android.app.Activity.RESULT_OK;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.condec.Database.BlockedURLRepository;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WarningDetectionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WarningDetectionFragment extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static final int REQUEST_CAPTURE_CODE = 1;
    private MediaProjectionManager mediaProjectionManager;
    private CondecDetectionService condecDetectionService;
    private SharedPreferences condecPreferences;

    private ImageButton btnTipWarning;
    private ImageView imgViewDetectionServiceStatus;
    private Button btnDetectionServiceStatus;
    private TextView txtDetectionServiceStatus;

    private boolean isServiceActive = false;

    boolean isBinded = false;
    boolean hasAllowedScreenCapture = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CondecDetectionService.LocalBinder binder = (CondecDetectionService.LocalBinder) service;
            condecDetectionService = binder.getService();

            isBinded = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            isBinded = false;

        }
    };
    public WarningDetectionFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment WarningDetectionFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WarningDetectionFragment newInstance(String param1, String param2) {
        WarningDetectionFragment fragment = new WarningDetectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        this.condecPreferences = getActivity().getSharedPreferences("condecPref", Context.MODE_PRIVATE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_warning_detection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.btnTipWarning = view.findViewById(R.id.btnTipWarning);

        this.btnDetectionServiceStatus = view.findViewById(R.id.btnDetectionStatus);
        this.imgViewDetectionServiceStatus = view.findViewById(R.id.imgViewDetectionStatus);
        this.txtDetectionServiceStatus = view.findViewById(R.id.txtDetectionStatus);

        this.btnTipWarning.setOnClickListener(this);

        this.btnDetectionServiceStatus.setOnClickListener(this);
        this.imgViewDetectionServiceStatus.setOnClickListener(this);

        checkAndBindService(); // Check if service is already running and bind to it

    }

    private void update(){

        if (isServiceRunning(CondecDetectionService.class) == true){

            this.imgViewDetectionServiceStatus.setImageResource(R.drawable.power_on_button_icon);
            this.btnDetectionServiceStatus.setText("On");
            this.btnDetectionServiceStatus.setBackgroundColor(getActivity().getColor(R.color.green));
            this.txtDetectionServiceStatus.setText("Click to Off");

        }
        else {

            this.imgViewDetectionServiceStatus.setImageResource(R.drawable.power_off_button_icon);
            this.btnDetectionServiceStatus.setText("Off");
            this.btnDetectionServiceStatus.setBackgroundColor(getActivity().getColor(R.color.red));
            this.txtDetectionServiceStatus.setText("Click to On");

        }

    }

    private void requestCapturePermission(){

        //hasAllowedScreenCapture = this.condecPreferences.getBoolean("hasAllowedScreenCapture", false);
        //hasAllowedScreenCapture = false;
        if (hasAllowedScreenCapture == false){

            System.out.println("REQUESTING MEDIA PROJECTION PERMISSION");
            mediaProjectionManager = (MediaProjectionManager) getActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(permissionIntent, REQUEST_CAPTURE_CODE);


        }

    }

    private void toggleService(){

        if (!isServiceRunning(CondecDetectionService.class)){

            requestCapturePermission();

        }
        else {

            stopCondecService();

        }

        update();
/*
        SharedPreferences.Editor editor = this.condecPreferences.edit();
        editor.putBoolean("isSystemActive", this.isSystemActive);
        editor.apply();*/

    }

    private void startService(int screenCaptureResultCode, Intent screenCaptureIntent)  {

        //boolean hasAllowedScreenCapture = this.condecPreferences.getBoolean("hasAllowedScreenCapture", false);

        if (screenCaptureResultCode == RESULT_OK){

            this.isServiceActive = true;

            // int screenCaptureResultCode = this.condecPreferences.getInt("screenCaptureResultCode", 0);
            //String serializedScreenCaptureIntent = this.condecPreferences.getString("savedScreenCaptureIntent", null);

            Intent serviceIntent = CondecDetectionService.newIntent(getActivity(), screenCaptureResultCode, screenCaptureIntent);
            getActivity().startForegroundService(serviceIntent);
            getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        }
        else {

            System.out.println("NO PERMISSION");

        }
        update();
    }

    private void stopCondecService()  {

        if (isBinded == true){

            getActivity().unbindService(this.serviceConnection);
            this.hasAllowedScreenCapture = false;
            this.isServiceActive = false;
            this.isBinded = false;

            Intent serviceIntent = new Intent(getActivity(), CondecDetectionService.class);
            getActivity().stopService(serviceIntent);

        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_CODE) {
            if (resultCode == getActivity().RESULT_OK) {
                // Get the MediaProjection
                this.hasAllowedScreenCapture = true;
                boolean hasAllowedScreenCapture = false; // FORCED CODE
                int screenCaptureResultCode = resultCode;
                String serializedIntent = data.toUri(Intent.URI_INTENT_SCHEME);

                startService(resultCode, data);

                SharedPreferences.Editor editor = condecPreferences.edit();
                editor.putBoolean("hasAllowedScreenCapture", hasAllowedScreenCapture);
                editor.putInt("screenCaptureResultCode", screenCaptureResultCode);
                editor.putString("savedScreenCaptureIntent", serializedIntent);
                editor.apply();

                // Continue with using the mediaProjection object
            } else {
                // User denied permission
                this.hasAllowedScreenCapture = false;
            }
        }
    }

    private void checkAndBindService() {
        if (isServiceRunning(CondecDetectionService.class)) {
            Intent serviceIntent = new Intent(getActivity(), CondecDetectionService.class);
            getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

            this.isServiceActive = true;
            update();

        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void showTip(){

        DialogTip dialog = new DialogTip("Warning Detection", "This feature allows you to enable or disable the detection of sensitive content warnings on your child’s social media.");
        dialog.show(requireActivity().getSupportFragmentManager(), "BlockedWebsitesInfoDialog");

    }

    @Override
    public void onClick(View view) {

        if (this.btnTipWarning == view){

            showTip();
            return;

        }
        else if (this.btnDetectionServiceStatus == view || this.imgViewDetectionServiceStatus == view){

            toggleService();

        }

    }

}