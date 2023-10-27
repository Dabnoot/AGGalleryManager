package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.Stack;

public class ItemClass_WebPageTabData  implements Serializable {

    String sTabID = "";                     //ID is a date/time stamp - the date/time the user first navigated a tab to an address.
    String sTabTitle = "";                  //Save the title so that the tab can be labeled.
    String sAddress = "";                        //Last used address for this tab. Enables reload.
    int iTabFragmentHashID;                 //Holds ID of tab related to this instance of ItemClass_WebPageTabData. Really needed when the view resets.
    String sFaviconAddress = "";           //Holds address of favicon file for tab icon.
    Stack<String> stackBackHistory = new Stack<>();
    Stack<String> stackForwardHistory = new Stack<>();

    String sUserName = ""; //User name of the tab creator. Don't share tabs between users.

    public boolean bIllegalDataFound = false; //Used during import.
    public String sIllegalDataNarrative = "";

}