package com.example.condec.Utils;

import android.view.View;
import android.widget.Button;

public class NumpadView implements View.OnClickListener {

    private PinController pinController;
    private DualPinController dualPinController;
    private Button[] buttons;

    public NumpadView(Button[] buttons){

        this.dualPinController = dualPinController;
        this.buttons = buttons;

        for (int i = 0; i < this.buttons.length; i++){

            this.buttons[i].setOnClickListener(this);

        }

    }

    public void setController(PinController pinController){

        this.pinController = pinController;

    }

    public void setController(DualPinController dualPinController){

        this.dualPinController = dualPinController;

    }

    public void sendData(Button button){

        if(this.pinController != null){

            this.pinController.recieveData(button.getText().toString());

        }

        if(this.dualPinController != null){

            this.dualPinController.recieveData(button.getText().toString());

        }

    }

    @Override
    public void onClick(View view) {

        for (int i = 0; i < this.buttons.length; i++){

            if (this.buttons[i] == view){

                sendData(this.buttons[i]);

            }

        }

    }
}
