package com.example.condec.Classes;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.condec.R;

public class WebsiteBlocked extends RecyclerView.ViewHolder {
    private TextView urlTextView;
    private Button removeButton;

    public WebsiteBlocked(View itemView) {
        super(itemView);
        urlTextView = itemView.findViewById(R.id.text_view_url);
        removeButton = itemView.findViewById(R.id.button_remove);
    }
}
