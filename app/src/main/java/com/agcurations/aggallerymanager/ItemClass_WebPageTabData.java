package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.ArrayList;

public class ItemClass_WebPageTabData  implements Serializable {

    String sTabID = "";                          //ID is a date/time stamp - the date/time the user first navigated a tab to an address.
    int iTabIndex;                          //The order that the webpage appears in a given tab layout
    String sTabTitle = "";                       //Save the title so that the tab can be labeled.
    ArrayList<String> alsAddressHistory;    //Address history for this tab. Last-in is the current address viewed.
    int iTabFragmentHashID;                //Holds ID of tab related to this instance of ItemClass_WebPageTabData. Really needed when the view resets.

}