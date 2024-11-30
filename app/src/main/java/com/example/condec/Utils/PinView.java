package com.example.condec.Utils;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PinView {

    private TextView[] pins;
    private LinearLayout background;

    private int pinCount;
    private int currentPin;

    private boolean isFull;
    private boolean isEmpty;

    private char pinCharacter;

    private String[] pinData;

    public PinView(char pinCharacter){

        this.isFull = false;
        this.isEmpty = true;
        this.currentPin = 0;
        this.pinCount = 0;
        this.pinCharacter = pinCharacter;

    }

    public void addPin(String data){

        if (!this.isFull && (this.currentPin >= 0 && this.currentPin <= this.pinCount)){

            this.pins[this.currentPin].setText(Character.toString(this.pinCharacter));
            this.pinData[this.currentPin] = data;
            this.currentPin++;
            setPinCursor();

            if (this.currentPin == this.pinCount){

                this.isFull = true;

            }
            if(this.currentPin != 0){

                this.isEmpty = false;

            }

        }

    }

    public void setPinCursor(){

        if (!this.isFull && (this.currentPin >= 0 && this.currentPin < this.pinCount)){

            this.pins[this.currentPin].setText("_");

        }


    }

    public void removePinCursor(){

        if (!this.isFull && (this.currentPin >= 0 && this.currentPin < this.pinCount)){

            this.pins[this.currentPin].setText(" ");

        }

    }

    public void removePin(){

        if (this.currentPin > 0 && this.currentPin <= this.pinCount){

            this.pins[this.currentPin - 1].setText(" ");
            this.pinData[this.currentPin - 1] = null;
            removePinCursor();

            if (isFull){

                this.pins[this.currentPin - 1].setText("_");

            }

            this.currentPin--;
            setPinCursor();


            if (this.currentPin >= 0 && this.currentPin <= this.pinCount){

                this.isFull = false;

            }

            if (this.currentPin == 0){

                this.isEmpty = true;

            }

        }

    }

    public void setPins(TextView[] pins){

        this.pins = pins;
        this.pinCount = this.pins.length;

        for(int i = 0; i < this.pins.length; i++){

            this.pins[i].setText(" ");

        }

        this.pinData = new String[this.pins.length];

    }

    public void setPinViewBackground(LinearLayout layout){
        this.background = layout;
    }

    public void setBackgroundColor(Drawable color){
        this.background.setBackground(color);
    }

    public boolean isFull(){
        return this.isFull;
    }

    public boolean isEmpty(){
        return this.isEmpty;
    }

    public String[] getPinData(){
        return this.pinData;
    }

}
