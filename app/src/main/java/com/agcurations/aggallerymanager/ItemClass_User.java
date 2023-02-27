package com.agcurations.aggallerymanager;

import android.graphics.Color;

import java.io.Serializable;

public class ItemClass_User implements Serializable {

    public String sUserName = "";
    public String sPin = ""; //Pin is a String, not an integer, because we do not perform math with a pin. Plus, don't want to deal with parseInt error possibility.
    public int iUserIconColor;
    public boolean bAdmin = false;
    public int iMaturityLevel = 0;
    public boolean bIsChecked = false; //For listView purposes.

    public ItemClass_User() {
    }

    public void setUserIconColor(int Alpha, int Red, int Green, int Blue){
        iUserIconColor = Color.argb(Alpha, Red, Green, Blue);
    }

}
