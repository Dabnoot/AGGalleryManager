package com.agcurations.aggallerymanager;

import java.util.ArrayList;

public class ItemClass_WebComicDataLocator {

    public String sShortName;
    public final String sHostnameRegEx;
    public boolean bHostNameMatchFound = false; //This tells Worker_Import_ComicAnalyzeHTML that this is the particular item in use.
    public boolean bSeriesFlag = false; //This tells Worker_Import_ComicAnalyzeHTML that the user is looking to import a listed comic series.
    public String sAddress = "";

    public String sHTML;
    //public String sCookie; //Cookie data sometimes needed when requesting resource download, according to some sources.

    //Data items can be located via either string search pattern or sXPathExpression.

    public ItemClass_WebComicDataLocator(String _sHostnameRegEx){
        sHostnameRegEx = _sHostnameRegEx;
    }

    ArrayList<ItemClass_ComicDownloadSearchKey> alComicDownloadSearchKeys;



}
