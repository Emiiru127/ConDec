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
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;

import com.example.condec.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppBlockAdapter extends RecyclerView.Adapter<AppBlockAdapter.AppViewHolder> implements Filterable {

    private List<ApplicationInfo> appList;
    private List<ApplicationInfo> appListFiltered;
    private Map<String, Boolean> appSelectionState = new HashMap<>();
    private Set<String> selectedApps;
    private PackageManager packageManager;

    public AppBlockAdapter(List<ApplicationInfo> appList, PackageManager packageManager, Set<String> previouslySelectedApps) {
        this.appList = appList;
        this.packageManager = packageManager;
        this.appListFiltered = new ArrayList<>(appList);

        if (previouslySelectedApps == null) {
            // If no apps were previously selected, block all apps by default
            this.selectedApps = new HashSet<>();
            for (ApplicationInfo app : appList) {
                selectedApps.add(app.packageName);
            }
        } else {
            this.selectedApps = new HashSet<>(previouslySelectedApps);
        }

        // Initialize appSelectionState based on selectedApps
        for (ApplicationInfo app : appList) {
            appSelectionState.put(app.packageName, selectedApps.contains(app.packageName));
        }
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_blocking, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        ApplicationInfo appInfo = appListFiltered.get(position);

        holder.appIcon.setImageDrawable(appInfo.loadIcon(packageManager));
        holder.appName.setText(appInfo.loadLabel(packageManager));

        // Detach listener before setting checked state
        holder.appSwitch.setOnCheckedChangeListener(null);

        // Set switch state based on appSelectionState map
        holder.appSwitch.setChecked(appSelectionState.getOrDefault(appInfo.packageName, false));

        // Reattach listener after setting the checked state
        holder.appSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appSelectionState.put(appInfo.packageName, isChecked);

            if (isChecked) {
                selectedApps.add(appInfo.packageName);
            } else {
                selectedApps.remove(appInfo.packageName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint.toString().toLowerCase();

                if (query.isEmpty()) {
                    appListFiltered = new ArrayList<>(appList);
                } else {
                    List<ApplicationInfo> filteredList = new ArrayList<>();
                    for (ApplicationInfo app : appList) {
                        if (app.loadLabel(packageManager).toString().toLowerCase().contains(query)) {
                            filteredList.add(app);
                        }
                    }
                    appListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = appListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                appListFiltered = (List<ApplicationInfo>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public void selectAll() {
        for (ApplicationInfo app : appListFiltered) {
            appSelectionState.put(app.packageName, true);
            selectedApps.add(app.packageName);
        }
        notifyDataSetChanged();
    }

    public void deselectAll() {
        for (ApplicationInfo app : appListFiltered) {
            appSelectionState.put(app.packageName, false);
            selectedApps.remove(app.packageName);
        }
        notifyDataSetChanged();
    }

    public Set<String> getSelectedApps() {
        return selectedApps;
    }

    public Map<String, Boolean> getAppSelectionState() {
        return appSelectionState;
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        Switch appSwitch;

        public AppViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appSwitch = itemView.findViewById(R.id.appSwitch);
        }
    }
}

