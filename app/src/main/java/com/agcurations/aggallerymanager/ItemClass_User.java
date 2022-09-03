package com.agcurations.aggallerymanager;

import android.graphics.Color;

import java.io.Serializable;

public class ItemClass_User implements Serializable {

    public String sUserName = "";
    public int iPin = -1;
    public int iUserIconColor;
    public boolean bAdmin = false;
    public int iMaturityLevel = 0;

    public ItemClass_User() {
    }

    public void setUserIconColor(int Alpha, int Red, int Green, int Blue){
        iUserIconColor = Color.argb(Alpha, Red, Green, Blue);
    }

}
