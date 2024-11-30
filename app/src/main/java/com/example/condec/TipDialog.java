package com.example.condec;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class TipDialog extends DialogFragment {

    private String title;
    private String message;
    private PermissionDialogListener listener;

    private boolean isPermission;

    public TipDialog(String title, String message) {
        this.title = title;
        this.message = message;
        this.listener = null;
        this.isPermission = false;
    }

    public TipDialog(String title, String message, PermissionDialogListener listener) {
        this.title = title;
        this.message = message;
        this.listener = listener;
        this.isPermission = true;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog_info, null);

        TextView txtViewTitle = view.findViewById(R.id.dialog_title);
        TextView txtViewMessage = view.findViewById(R.id.dialog_message);

        txtViewTitle.setText(this.title);
        txtViewMessage.setText(this.message);

        builder.setView(view);

        // Find the back button and set its click listener
        Button backButton = view.findViewById(R.id.btnDialogBack);

        if (isPermission == true){

            backButton.setText("OK");

        }

        backButton.setOnClickListener(v -> {
            if (listener != null) {

                listener.onDialogConfirmed();  // Notify listener when the dialog is dismissed with back button
            }
            dismiss();
        });

        return builder.create();
    }

    public interface PermissionDialogListener {
        void onDialogConfirmed();
    }
}
