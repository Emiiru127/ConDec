package com.example.condec;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
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
import android.widget.Switch;
import android.widget.TextView;

import com.example.condec.Classes.WebsiteBlockAdapter;
import com.example.condec.Database.BlockedURLRepository;
import com.example.condec.Database.UserBlockedUrl;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WebsiteBlockingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WebsiteBlockingFragment extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private BlockedURLRepository repository;
    private WebsiteBlockAdapter adapter;
    private RecyclerView recyclerView;

    private Switch switchWebsiteBlock;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Button btnAddWebsite;

    private static final int VPN_REQUEST_CODE = 0x0F;

    public WebsiteBlockingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment WebsiteBlockingFragment.
     */
    // TODO: Rename and change types and number of parameters
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_website_blocking, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.btnAddWebsite = getView().findViewById(R.id.btnAddWebsite);
        this.btnAddWebsite.setOnClickListener(this);

        this.switchWebsiteBlock = getView().findViewById(R.id.switchWebsiteBlock);

        Log.d("Condec Database", "DATABASE DATA:");

        BlockedURLRepository blockedURLRepository = new BlockedURLRepository(getActivity().getApplication());

        LiveData<List<String>> blockedUrls = blockedURLRepository.getAllBlockedUrls();

        blockedUrls.observe(getViewLifecycleOwner(), urls -> {
            // Update UI with the list of URLs

            for (String url : urls){

                Log.d("Condec Database", url);

            }
        });

        boolean isServiceRunning = isServiceRunning(CondecVPNService.class);
        switchWebsiteBlock.setChecked(isServiceRunning);

        switchWebsiteBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {

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

// Observe the LiveData
        repository.getUserBlockedUrls().observe(getViewLifecycleOwner(), new Observer<List<UserBlockedUrl>>() {
            @Override
            public void onChanged(List<UserBlockedUrl> userBlockedUrls) {
                // Initialize adapter with the list of URLs and context
                adapter = new WebsiteBlockAdapter(userBlockedUrls, repository, getContext());
                recyclerView.setAdapter(adapter);
            }
        });

    }
    private void stopVpnService() {
        // Stop the VPN service if it is running
        Intent serviceIntent = new Intent(getActivity(), CondecVPNService.class);
        getActivity().stopService(serviceIntent);

        // Revoke the VPN connection using a valid context
        Context context = getActivity(); // Ensure we have a valid context
        if (context != null) {
            Intent revokeIntent = VpnService.prepare(context);  // Use the valid context
            if (revokeIntent != null) {
                startActivityForResult(revokeIntent, VPN_REQUEST_CODE);
            } else {
                Log.d("VPN", "VPN successfully revoked");
            }
        } else {
            Log.e("VPN", "Context is null, cannot revoke VPN");
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

        DialogTip dialog = new DialogTip();
        dialog.show(requireActivity().getSupportFragmentManager(), "BlockedAppsInfoDialog");

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            Intent intent = new Intent(getActivity(), CondecVPNService.class);
            getActivity().startService(intent);
        }
    }

    private void showAddUrlDialog() {
        // Inflate the custom dialog view
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
                addUrlToDatabase(url);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void addUrlToDatabase(String url) {
        // Perform the database operation
            UserBlockedUrl userBlockedUrl = new UserBlockedUrl(url);
            repository.insertUserBlockedUrl(userBlockedUrl);
    }

    private void startVpnService(){

        Intent intent = CondecVPNService.prepare(getActivity());
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            onActivityResult(VPN_REQUEST_CODE, getActivity().RESULT_OK, null);
        }

    }

    @Override
    public void onClick(View view) {

        if (this.btnAddWebsite == view){

            showAddUrlDialog();

        }

    }
}