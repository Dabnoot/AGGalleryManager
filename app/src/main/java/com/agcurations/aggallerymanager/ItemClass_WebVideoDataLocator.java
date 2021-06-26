package com.agcurations.aggallerymanager;

import android.content.ClipData;

import java.util.ArrayList;

public class ItemClass_WebVideoDataLocator {

    public final String sHostnameRegEx;
    public boolean bHostNameMatchFound = false; //This tells Service_Import that this is the particular item in use.

    public String sHTML;

    //Data items can be located via either string search pattern or sXPathExpression.

    public ItemClass_WebVideoDataLocator(String _sHostnameRegEx){
        sHostnameRegEx = _sHostnameRegEx;
    }

    ArrayList<ItemClass_VideoDownloadSearchKey> alVideoDownloadSearchKeys;



}
