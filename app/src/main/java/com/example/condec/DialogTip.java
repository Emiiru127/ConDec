package com.example.condec;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class DialogTip extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate the custom layout
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog_info, null);


        // Set the custom layout
        builder.setView(view);

        // Find views from the dialog layout
        Button backButton = view.findViewById(R.id.btnDialogBack);

        // Set button click listener
        backButton.setOnClickListener(v -> dismiss());

        return builder.create();
    }

}
