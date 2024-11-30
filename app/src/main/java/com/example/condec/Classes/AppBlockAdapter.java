package com.example.condec.Classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;

import com.example.condec.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppBlockAdapter extends RecyclerView.Adapter<AppBlockAdapter.AppViewHolder> {

    private List<ApplicationInfo> appList;
    private PackageManager packageManager;
    private Set<String> selectedApps;
    private SharedPreferences sharedPreferences;

    public AppBlockAdapter(List<ApplicationInfo> appList, PackageManager packageManager, Context context) {
        this.appList = appList;
        this.packageManager = packageManager;
        this.sharedPreferences = context.getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        this.selectedApps = new HashSet<>(sharedPreferences.getStringSet("blockedApps", new HashSet<>()));
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_blocking, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        ApplicationInfo app = appList.get(position);
        holder.appIcon.setImageDrawable(app.loadIcon(packageManager));
        holder.appName.setText(app.loadLabel(packageManager));

        // Set the SwitchCompat state based on whether the app is in the selected list
        holder.appSwitch.setOnCheckedChangeListener(null);
        holder.appSwitch.setChecked(selectedApps.contains(app.packageName));

        holder.appSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedApps.add(app.packageName);
            } else {
                selectedApps.remove(app.packageName);
            }
            saveSelectedApps();
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    private void saveSelectedApps() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("blockedApps", selectedApps);
        editor.apply();
    }

    public Set<String> getSelectedApps() {
        return selectedApps;
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        SwitchCompat appSwitch;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appSwitch = itemView.findViewById(R.id.appSwitch);
        }
    }
}
