package com.example.condec.Classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;

import com.example.condec.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppBlockAdapter extends RecyclerView.Adapter<AppBlockAdapter.AppViewHolder> implements Filterable {

    private List<ApplicationInfo> appList;

    private List<ApplicationInfo> appListFull; // Copy of the full list for filtering
    private PackageManager packageManager;
    private Set<String> selectedApps;
    private SharedPreferences sharedPreferences;

    public AppBlockAdapter(List<ApplicationInfo> appList, PackageManager packageManager, Context context) {
        this.appList = appList;
        this.packageManager = packageManager;
        this.appListFull = new ArrayList<>(appList); // Initialize the full list
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
        ApplicationInfo appInfo = appList.get(position);
        holder.appName.setText(appInfo.loadLabel(packageManager));
        holder.appIcon.setImageDrawable(appInfo.loadIcon(packageManager));

        // Set the switch state based on whether the app is selected
        holder.appSwitch.setChecked(selectedApps.contains(appInfo.packageName));

        holder.appSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedApps.add(appInfo.packageName);
            } else {
                selectedApps.remove(appInfo.packageName);
            }
            saveSelectedApps(); // Save the state if needed
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

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<ApplicationInfo> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(appListFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (ApplicationInfo app : appListFull) {
                        if (app.loadLabel(packageManager).toString().toLowerCase().contains(filterPattern)) {
                            filteredList.add(app);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                appList.clear();
                appList.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
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
