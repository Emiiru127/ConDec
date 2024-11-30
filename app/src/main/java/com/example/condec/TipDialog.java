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

public class TipDialog extends DialogFragment {

    private String title;
    private String message;
    private String buttonText;
    private PermissionDialogListener listener;

    private MainMenuActivity mainMenuActivity;

    private boolean isPermission;

    private boolean isPermitting;

    public TipDialog(String title, String message, String buttonText) {
        this.title = title;
        this.message = message;
        this.buttonText = buttonText;
        this.listener = null;
        this.isPermission = false;
        this.isPermitting = false;
    }

    public TipDialog(String title, String message) {
        this.title = title;
        this.message = message;
        this.buttonText = null;
        this.listener = null;
        this.isPermission = false;
        this.isPermitting = false;
    }

    public TipDialog(String title, String message, PermissionDialogListener listener) {
        this.title = title;
        this.message = message;
        this.buttonText = null;
        this.listener = listener;
        this.isPermission = true;
        this.isPermitting = false;
    }

    public TipDialog(String title, String message, PermissionDialogListener listener, MainMenuActivity mainMenuActivity) {
        this.title = title;
        this.message = message;
        this.buttonText = null;
        this.listener = listener;
        this.mainMenuActivity = mainMenuActivity;
        this.isPermission = true;
        this.isPermitting = false;
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

        Button backButton = view.findViewById(R.id.btnDialogBack);

        if (isPermission == true){

            backButton.setText("OK");

        }
        else if (this.buttonText != null){

            backButton.setText(buttonText);

        }

        backButton.setOnClickListener(v -> {
            if (listener != null) {

                this.isPermitting = true;
                listener.onDialogConfirmed();
            }
            dismiss();
        });

        return builder.create();
    }

    public interface PermissionDialogListener {
        void onDialogConfirmed();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (this.isPermission == true && this.mainMenuActivity != null){

            if(this.isPermitting != true){

                this.mainMenuActivity.checkAndRequestPermissions();

            }

        }

    }
}
