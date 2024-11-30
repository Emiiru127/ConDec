package com.example.condec.Classes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.condec.R;

import java.util.List;

public class ParentalAppUsageAdapter extends RecyclerView.Adapter<ParentalAppUsageAdapter.ViewHolder> {

    private List<ParentalAppUsageInfo> appUsageList;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView appIcon;
        public TextView appName;
        public TextView appUsageTime;
        public TextView appLastTimeUsed;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appUsageTime = itemView.findViewById(R.id.app_usage_time);
            appLastTimeUsed = itemView.findViewById(R.id.app_last_time_used);
        }
    }

    public ParentalAppUsageAdapter(List<ParentalAppUsageInfo> appUsageList) {
        this.appUsageList = appUsageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_usage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParentalAppUsageInfo parentalAppUsageInfo = appUsageList.get(position);
        holder.appName.setText(parentalAppUsageInfo.getAppName());
        holder.appIcon.setImageDrawable(parentalAppUsageInfo.getAppIcon());
        holder.appUsageTime.setText(formatUsageTime(parentalAppUsageInfo.getUsageTime()));
        holder.appLastTimeUsed.setText(formatRelativeTime(parentalAppUsageInfo.getLastTimeUsed()));
    }

    @Override
    public int getItemCount() {
        return appUsageList.size();
    }

    private String formatUsageTime(long timeInMillis) {
        long minutes = (timeInMillis / 1000) / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        return String.format("%dh %02dm", hours, minutes);
    }

    private String formatRelativeTime(long lastTimeUsed) {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - lastTimeUsed;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "just now";
        }
    }

}
