package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class ItemClass_VideoDownloadSearchKey implements Serializable {
    //This class to define search items to use to assist with finding data in an html file.
    //  Used to find a string by first locating a match, and copy data to a second matched string position.

    public final String sHostnameInfo;

    public static final String VIDEO_DOWNLOAD_TITLE = "VIDEO_DOWNLOAD_TITLE";
    public static final String VIDEO_DOWNLOAD_LINK = "VIDEO_DOWNLOAD_LINK";
    public static final String VIDEO_DOWNLOAD_M3U8 = "VIDEO_DOWNLOAD_M3U8";

    public final String sDataType; //Use the above types - download_title, link, m3u8, etc.

    public final String sSearchStartString;
    public final String sSearchEndString;

    public boolean bMatchFound = false;
    public String sSearchStringMatchContent = "";
    public ArrayList<String> alsM3U8Lines;

    public long lFileSize = 0; //Used if download link.
    public boolean bErrorWithLink = false; //Used if download link.
    public String sErrorMessage = ""; //Used if download link.

    public ItemClass_VideoDownloadSearchKey(String _sHostnameInfo,
                                            String _sDataType,
                                            String _sSearchStartString,
                                            String _sSearchEndString)
    {
        this.sHostnameInfo = _sHostnameInfo;
        this.sDataType = _sDataType;
        this.sSearchStartString = _sSearchStartString;
        this.sSearchEndString = _sSearchEndString;
    }

}