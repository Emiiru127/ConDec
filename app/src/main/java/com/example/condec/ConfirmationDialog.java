package com.example.condec;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ConfirmationDialog extends DialogFragment {

    private String title;
    private String message;

    private ParentalControlActivity parentalControlActivity;

    private boolean isConfirmation;

    public ConfirmationDialog(String title, String message) {
        this.title = title;
        this.message = message;
        this.isConfirmation = false;
    }

    public ConfirmationDialog(String title, String message, ParentalControlActivity parentalControlActivity) {
        this.title = title;
        this.message = message;
        this.parentalControlActivity = parentalControlActivity;
        this.isConfirmation = true;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog_confirmation, null);

        TextView txtViewTitle = view.findViewById(R.id.dialog_confirmation_title);
        TextView txtViewMessage = view.findViewById(R.id.dialog_confirmation_message);

        txtViewTitle.setText(this.title);
        txtViewMessage.setText(this.message);

        builder.setView(view);

        Button btnYes = view.findViewById(R.id.btnConfirmationDialogYes);
        Button btnNo = view.findViewById(R.id.btnConfirmationDialogNo);

        btnYes.setOnClickListener(v -> {

            if (isConfirmation){

                this.parentalControlActivity.deactivateSecurityToDevice();

            }

            dismiss();

        });

        btnNo.setOnClickListener(v -> {

            dismiss();

        });

        return builder.create();
    }
}
