package com.agcurations.aggallerymanager;

import java.util.ArrayList;

public class ItemClass_WebComicDataLocator {

    public String sShortName;
    public final String sHostnameRegEx;
    public boolean bHostNameMatchFound = false; //This tells Service_Import that this is the particular item in use.

    public String sHTML;
    //public String sCookie; //Cookie data sometimes needed when requesting resource download, according to some sources.

    //Data items can be located via either string search pattern or sXPathExpression.

    public ItemClass_WebComicDataLocator(String _sHostnameRegEx){
        sHostnameRegEx = _sHostnameRegEx;
    }

    ArrayList<ItemClass_ComicDownloadSearchKey> alComicDownloadSearchKeys;



}
