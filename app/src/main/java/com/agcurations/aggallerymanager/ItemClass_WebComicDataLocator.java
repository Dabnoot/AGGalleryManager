package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.ArrayList;

public class ItemClass_WebComicDataLocator implements Serializable {

    public String sShortName;
    public final String sHostnameRegEx;
    public String sComicSeriesIDStartString = ""; //Used if the comic site has a standard tracking for comic series. Used to extract the ID.
    public boolean bHostNameMatchFound = false; //This tells Worker_Import_ComicAnalyzeHTML that this is the particular item in use.
    public boolean bSeriesSummaryWebpageFlag = false; //This tells Worker_Import_ComicAnalyzeHTML that the user is looking to analyze a page listing comics in a series.
    public boolean bRecognizedSeries = false; //Tells that this data is associated with a recognized comic series.
    public String sAddress = "";

    public String sHTML;
    //public String sCookie; //Cookie data sometimes needed when requesting resource download, according to some sources.

    //Data items can be located via either string search pattern or sXPathExpression.

    public ItemClass_WebComicDataLocator(String _sHostnameRegEx){
        sHostnameRegEx = _sHostnameRegEx;
    }

    ArrayList<ItemClass_ComicDownloadSearchKey> alComicDownloadSearchKeys;

    ArrayList<ItemClass_File> alicf_ComicDownloadFileItems = null; //Added explicitly to accommodate download of a known collection item via browser web page tab.


}
