package com.example.condec;

import static com.example.condec.MainMenuActivity.REQUEST_CODE_VPN;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.condec.Classes.WebsiteBlockAdapter;
import com.example.condec.Database.BlockedURLRepository;
import com.example.condec.Database.UserBlockedUrl;

import java.net.URL;
import java.util.List;

public class WebsiteBlockingFragment extends Fragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private BlockedURLRepository repository;
    private WebsiteBlockAdapter adapter;
    private RecyclerView recyclerView;
    private SharedPreferences condecPreferences;

    private Switch switchWebsiteBlock;

    private String mParam1;
    private String mParam2;


    private ImageButton btnTipWebsiteBlock;
    private Button btnAddWebsite;


    public WebsiteBlockingFragment() {

    }

    public static WebsiteBlockingFragment newInstance(String param1, String param2) {
        WebsiteBlockingFragment fragment = new WebsiteBlockingFragment();
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
        repository = new BlockedURLRepository(requireActivity().getApplication());
        this.condecPreferences = getActivity().getSharedPreferences("condecPref", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_website_blocking, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.btnTipWebsiteBlock = getView().findViewById(R.id.btnTipWebsiteBlock);
        this.btnTipWebsiteBlock.setOnClickListener(this);

        this.btnAddWebsite = getView().findViewById(R.id.btnAddWebsite);
        this.btnAddWebsite.setOnClickListener(this);

        this.switchWebsiteBlock = getView().findViewById(R.id.switchWebsiteBlock);

        Log.d("Condec Database", "DATABASE DATA:");

        BlockedURLRepository blockedURLRepository = new BlockedURLRepository(getActivity().getApplication());

        LiveData<List<String>> blockedUrls = blockedURLRepository.getAllBlockedUrls();

        blockedUrls.observe(getViewLifecycleOwner(), urls -> {

            for (String url : urls){

                Log.d("Condec Database", url);

            }
        });

        boolean isServiceRunning = isServiceRunning(CondecVPNService.class);
        switchWebsiteBlock.setChecked(isServiceRunning);

        switchWebsiteBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {

           if (!isVPNPermissionGranted()){

               requestVPNPermission();
               return;

           }

           if (isChecked) {
               startVpnService();
           } else {
               Log.e("Website Fragment", "Stopping VPN");
               stopVpnService();
           }

        });

        recyclerView = view.findViewById(R.id.recylcerWebsiteBlocked);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        repository = new BlockedURLRepository(requireActivity().getApplication());

        repository.getUserBlockedUrls().observe(getViewLifecycleOwner(), new Observer<List<UserBlockedUrl>>() {
            @Override
            public void onChanged(List<UserBlockedUrl> userBlockedUrls) {
                adapter = new WebsiteBlockAdapter(userBlockedUrls, repository, getContext());
                recyclerView.setAdapter(adapter);
            }
        });

    }
    private void stopVpnService() {

        Log.d("Condec Security", "VPN SERVICE WAS MANUALLY TURNED OFF");

        SharedPreferences.Editor editor =  this.condecPreferences.edit();
        editor.putBoolean("isVPNServiceManuallyOff", true);
        editor.apply();

        Intent broadcastIntent = new Intent("com.example.condec.UPDATE_SECURITY_FLAGS_VPN_OFF");
        getActivity().sendBroadcast(broadcastIntent);

        boolean isVPNServiceManuallyOff = this.condecPreferences.getBoolean("isVPNServiceManuallyOff", true);
        Log.d("Condec Security", "VPN SERVICE WAS MANUALLY: " + isVPNServiceManuallyOff);

        Intent intent = new Intent(getActivity(), CondecVPNService.class);
        intent.setAction(CondecVPNService.ACTION_STOP_VPN);
        getActivity().startService(intent);
    }

    private boolean isVPNPermissionGranted() {
        return CondecVPNService.prepare(getActivity()) == null;
    }

    private void requestVPNPermission() {
        TipDialog tipDialog = new TipDialog("VPN Permission",
                "This app requires VPN permission to secure your network connection.",
                () -> {
                    Intent vpnIntent = CondecVPNService.prepare(getActivity());
                    if (vpnIntent != null) {
                        startActivityForResult(vpnIntent, REQUEST_CODE_VPN);
                    } else {

                        requestVPNPermission();

                    }
                });
        tipDialog.show(getActivity().getSupportFragmentManager(), "VPNPermissionDialog");
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        if (requestCode == REQUEST_CODE_VPN) {
            if (isVPNPermissionGranted()) {
                startVpnService();
            } else {
                Toast.makeText(getActivity(), "VPN permission is required.", Toast.LENGTH_SHORT).show();
                requestVPNPermission();
            }
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

    private void showMessage(String title, String message, String buttonText){

        TipDialog dialog = new TipDialog(title, message, buttonText);
        dialog.show(requireActivity().getSupportFragmentManager(), "BlockedWebsitesDialog");

    }

    private void showTip(){

        TipDialog dialog = new TipDialog("Blocked Websites", "Restrict access to certain websites on your device. Blocked sites are automatically inaccessible.");
        dialog.show(requireActivity().getSupportFragmentManager(), "BlockedWebsitesInfoDialog");

    }

    private void showAddUrlDialog() {

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.layout_request_string, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        TextView txtView = dialogView.findViewById(R.id.txtViewText);
        EditText editTxtInput = dialogView.findViewById(R.id.editTxtInput);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAdd = dialogView.findViewById(R.id.btnDone);

        txtView.setText("Enter the URL of the Website:");
        editTxtInput.setHint("Enter URL");
        btnAdd.setText("Add Website");

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String url = editTxtInput.getText().toString().trim();
            if (!url.isEmpty()) {

                dialog.dismiss();
                String newURL = extractDomain(url);
                Log.d("Website Fragment", "Extracted URL: " + newURL);

                if (newURL == null){

                    Log.d("Website Fragment", "Invalid URL: " + newURL);
                    showMessage("Invalid URL", "The URL you provided is not a valid domain or URL.", "Ok");
                    return;

                }
                Log.d("Website Fragment", "Added: " + newURL + " to Blocked Database");
                addUrlToDatabase(newURL);

            }
        });

        dialog.show();
    }

    private void addUrlToDatabase(String url) {

        UserBlockedUrl userBlockedUrl = new UserBlockedUrl(url);
        repository.insertUserBlockedUrl(userBlockedUrl);

        if (isServiceRunning(CondecVPNService.class)){

            restartWebsiteBlocking();

        }

    }

    private void startVpnService(){

        Log.d("Condec Security", "VPN SERVICE WAS MANUALLY TURNED OFF");

        SharedPreferences.Editor editor =  this.condecPreferences.edit();
        editor.putBoolean("isVPNServiceManuallyOff", false);
        editor.apply();

        Intent broadcastIntent = new Intent("com.example.condec.UPDATE_SECURITY_FLAGS_VPN_ON");
        getActivity().sendBroadcast(broadcastIntent);

        boolean isVPNServiceManuallyOff = this.condecPreferences.getBoolean("isVPNServiceManuallyOff", true);
        Log.d("Condec Security", "VPN SERVICE WAS MANUALLY: " + isVPNServiceManuallyOff);

        Intent intent = new Intent(getActivity(), CondecVPNService.class);
        getActivity().startService(intent);

    }

    private static String extractDomain(String input) {
        try {

            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                input = "http://" + input;
            }

            URL url = new URL(input);

            String host = url.getHost();

            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            if (!host.contains(".")) {
                throw new IllegalArgumentException("Input is not a valid domain or URL.");
            }

            return host;
        } catch (Exception e) {

            System.err.println("Invalid input: " + input);
            return null;
        }
    }

    private void restartWebsiteBlocking(){

        stopVpnService();
        startVpnService();

    }

    @Override
    public void onClick(View view) {

        if (this.btnTipWebsiteBlock == view){

            showTip();
            return;

        }

        if (this.btnAddWebsite == view){

            showAddUrlDialog();

        }

    }
}