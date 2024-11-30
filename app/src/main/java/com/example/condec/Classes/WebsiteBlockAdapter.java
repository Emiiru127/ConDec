package com.example.condec.Classes;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.condec.CondecVPNService;
import com.example.condec.Database.BlockedURLRepository;
import com.example.condec.Database.UserBlockedUrl;
import com.example.condec.R;

import java.util.List;

public class WebsiteBlockAdapter extends RecyclerView.Adapter<WebsiteBlockAdapter.WebsiteBlockedViewHolder> {

    private List<UserBlockedUrl> userBlockedUrls;
    private BlockedURLRepository repository;
    private Context context;

    public WebsiteBlockAdapter(List<UserBlockedUrl> userBlockedUrls, BlockedURLRepository repository, Context context) {
        this.userBlockedUrls = userBlockedUrls;
        this.repository = repository;
        this.context = context;
    }

    @NonNull
    @Override
    public WebsiteBlockedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_blocked_url, parent, false);
        return new WebsiteBlockedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WebsiteBlockedViewHolder holder, int position) {
        UserBlockedUrl userBlockedUrl = userBlockedUrls.get(position);
        holder.urlTextView.setText(userBlockedUrl.getUrl());

        holder.removeButton.setOnClickListener(v -> {
            removeUrl(userBlockedUrl, position, holder);
        });
    }

    @Override
    public int getItemCount() {
        return userBlockedUrls.size();
    }

    private void removeUrl(UserBlockedUrl userBlockedUrl, int position, WebsiteBlockedViewHolder holder) {
        new Thread(() -> {
            repository.removeUserBlockedUrl(userBlockedUrl);
            userBlockedUrls.remove(position);

            ((Activity) context).runOnUiThread(() -> notifyItemRemoved(position));

            if (isServiceRunning(CondecVPNService.class)){

                Intent stopIntent = new Intent(context, CondecVPNService.class);
                stopIntent.setAction(CondecVPNService.ACTION_STOP_VPN);
                context.startService(stopIntent);
                Intent startIntent = new Intent(context, CondecVPNService.class);
                context.startService(startIntent);

            }

        }).start();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static class WebsiteBlockedViewHolder extends RecyclerView.ViewHolder {
        TextView urlTextView;
        ImageButton removeButton;

        public WebsiteBlockedViewHolder(View itemView) {
            super(itemView);
            urlTextView = itemView.findViewById(R.id.text_view_url);
            removeButton = itemView.findViewById(R.id.button_remove);
        }
    }
}
