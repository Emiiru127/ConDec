package com.example.condec.Utils;

import com.example.condec.CreatePinActivity;
import com.example.condec.R;

public class DualPinController {

    private CreatePinActivity createPinActivity;

    private PinView pinView1;
    private PinView pinView2;
    private NumpadView numpadView;

    private boolean isDone;

    public DualPinController(CreatePinActivity createPinActivity, PinView pinView1, PinView pinView2, NumpadView numpadView){

        this.createPinActivity = createPinActivity;
        this.pinView1 = pinView1;
        this.pinView2 = pinView2;
        this.numpadView = numpadView;

        this.numpadView.setController(this);

    }

    public String getEnteredData(){

        String enteredData = "";

        for (int i = 0; i < this.pinView1.getPinData().length; i++){

            enteredData += this.pinView1.getPinData()[i];

        }

        return enteredData;

    }

    public String getReEnteredData(){

        String reEnteredData = "";

        for (int i = 0; i < this.pinView2.getPinData().length; i++){

            reEnteredData += this.pinView2.getPinData()[i];

        }

        return reEnteredData;

    }

    public void recieveData(String data){

        if ((!pinView1.isFull() || data.equals("X")) && pinView2.isEmpty()){

            this.pinView1.setBackgroundColor(this.createPinActivity.getDrawable(R.drawable.layout_round_highlight));
            this.pinView2.setBackgroundColor(this.createPinActivity.getDrawable(R.drawable.layout_round_main_background));

            if (data.equals("X")){

                this.pinView1.removePin();

            }
            else {

                this.pinView1.addPin(data);

            }

        }
        else if ((!pinView2.isFull() || data.equals("X")) && pinView1.isFull()) {

            this.pinView1.setBackgroundColor(this.createPinActivity.getDrawable(R.drawable.layout_round_main_background));
            this.pinView2.setBackgroundColor(this.createPinActivity.getDrawable(R.drawable.layout_round_highlight));

            if (data.equals("X")){

                this.pinView2.removePin();

            }
            else {

                this.pinView2.addPin(data);

            }

        }

        if (pinView1.isFull()){

            this.pinView1.setBackgroundColor(this.createPinActivity.getDrawable(R.drawable.layout_round_main_background));
            this.pinView2.setBackgroundColor(this.createPinActivity.getDrawable(R.drawable.layout_round_highlight));

        }

        if (pinView1.isFull() && pinView2.isFull()){

            this.isDone = true;
            this.createPinActivity.update();

        }
        else {

            this.isDone = false;
            this.createPinActivity.update();

        }

    }

    public boolean isDone() {
        return isDone;
    }
}
