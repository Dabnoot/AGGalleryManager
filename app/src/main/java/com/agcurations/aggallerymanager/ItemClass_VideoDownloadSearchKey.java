package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class ItemClass_VideoDownloadSearchKey {
    //This class to define search items to use to assist with finding data in an html file.
    //  Used to find a string by first locating a match, and copy data to a second matched string position.

    public static final String VIDEO_DOWNLOAD_TITLE = "VIDEO_DOWNLOAD_TITLE";
    public static final String VIDEO_DOWNLOAD_TAGS = "VIDEO_DOWNLOAD_TAGS";
    public static final String VIDEO_DOWNLOAD_THUMBNAIL = "VIDEO_DOWNLOAD_THUMBNAIL";
    public static final String VIDEO_DOWNLOAD_LINK = "VIDEO_DOWNLOAD_LINK";
    public static final String VIDEO_DOWNLOAD_M3U8 = "VIDEO_DOWNLOAD_M3U8";

    public final String sDataType; //Use the above types - download_title, link, m3u8, etc.

    public final String sSearchStartString;
    public final String sSearchEndString;
    public final String sSXPathExpression;

    public boolean bMatchFound = false;
    public String sSearchStringMatchContent = "";

    public long lFileSize = 0; //Used if download link.
    public boolean bErrorWithLink = false; //Used if download link.
    public String sErrorMessage = ""; //Used if download link.

    public ItemClass_VideoDownloadSearchKey(String sDataType,
                                            String sSearchStartString,
                                            String sSearchEndString)
    {
        this.sDataType = sDataType;
        this.sSearchStartString = sSearchStartString;
        this.sSearchEndString = sSearchEndString;
        this.sSXPathExpression = null;
    }

    public ItemClass_VideoDownloadSearchKey(String sDataType,
                                            String sSXPathExpression)
    {
        this.sDataType = sDataType;
        this.sSearchStartString = null;
        this.sSearchEndString = null;
        this.sSXPathExpression = sSXPathExpression;
    }

}
