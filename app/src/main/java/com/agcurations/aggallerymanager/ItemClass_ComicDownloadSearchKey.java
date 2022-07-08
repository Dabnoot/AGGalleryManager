package com.agcurations.aggallerymanager;

import java.util.ArrayList;

public class ItemClass_ComicDownloadSearchKey {
    //This class to define search items to use to assist with finding data in an html file.
    //  Used to find a string by first locating a match, and copy data to a second matched string position.

    public static final String COMIC_DETAILS_TITLE = "COMIC_DETAILS_TITLE";
    public static final String COMIC_THUMBNAIL = "COMIC_THUMBNAIL";
    public static final String COMIC_DETAILS_PARODIES_DATA = "COMIC_DETAILS_PARODIES_DATA";
    public static final String COMIC_DETAILS_CHARACTERS_DATA = "COMIC_DETAILS_CHARACTERS_DATA";
    public static final String COMIC_DETAILS_TAGS_DATA = "COMIC_DETAILS_TAGS_DATA";
    public static final String COMIC_DETAILS_ARTISTS_DATA = "COMIC_DETAILS_ARTISTS_DATA";
    public static final String COMIC_DETAILS_GROUPS_DATA = "COMIC_DETAILS_GROUPS_DATA";
    public static final String COMIC_DETAILS_LANGUAGES_DATA = "COMIC_DETAILS_LANGUAGES_DATA";
    public static final String COMIC_DETAILS_CATEGORIES_DATA = "COMIC_DETAILS_CATEGORIES_DATA";
    public static final String COMIC_DETAILS_PAGES_DATA = "COMIC_DETAILS_PAGES_DATA";

    public final String sDataType; //Use the above types - download_title, link, image file, etc.

    public final String sSearchStartString;
    public final String sSearchEndString;
    public final String sSXPathExpression;

    public boolean bMatchFound = false;
    public String sSearchStringMatchContent = "";

    public long lFileSize = 0; //Used if download link.
    public boolean bErrorWithLink = false; //Used if download link.
    public String sErrorMessage = ""; //Used if download link.

    public ItemClass_ComicDownloadSearchKey(String sDataType,
                                            String sSearchStartString,
                                            String sSearchEndString)
    {
        this.sDataType = sDataType;
        this.sSearchStartString = sSearchStartString;
        this.sSearchEndString = sSearchEndString;
        this.sSXPathExpression = null;
    }

    public ItemClass_ComicDownloadSearchKey(String sDataType,
                                            String sSXPathExpression)
    {
        this.sDataType = sDataType;
        this.sSearchStartString = null;
        this.sSearchEndString = null;
        this.sSXPathExpression = sSXPathExpression;
    }

}
