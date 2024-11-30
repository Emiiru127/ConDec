package com.example.condec.Utils;

import com.example.condec.CreatePinActivity;
import com.example.condec.EnterPinActivity;
import com.example.condec.R;

public class PinController {

    private EnterPinActivity enterPinActivity;

    private PinView pinView;

    private NumpadView numpadView;

    private boolean isDone;

    public PinController(EnterPinActivity enterPinActivity, PinView pinView, NumpadView numpadView){

        this.enterPinActivity = enterPinActivity;
        this.pinView = pinView;
        this.numpadView = numpadView;

        this.numpadView.setController(this);

    }

    public String getEnteredData(){

        String enteredData = "";

        for (int i = 0; i < this.pinView.getPinData().length; i++){

            enteredData += this.pinView.getPinData()[i];

        }

        return enteredData;

    }

    public void recieveData(String data){

        if ((!pinView.isFull() || data.equals("X"))){

            if (data.equals("X")){

                this.pinView.removePin();

            }
            else {

                this.pinView.addPin(data);

            }

        }

        if (pinView.isFull()){

            this.isDone = true;
            this.enterPinActivity.update();

        }
        else {

            this.isDone = false;
            this.enterPinActivity.update();

        }

    }

    public boolean isDone() {
        return isDone;
    }

}
