package com.agcurations.aggallerymanager;



import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class GlobalClass extends Application {
    //GlobalClass built using this guide:
    //https://androidexample.com/Global_Variable_Or_Application_Context_Variable_-_Android_Example/index.php?view=article_discription&aid=114&aaid=136

    //Each section, Videos, Pictures, Comics, has its own folder, catalog file, tags file, and log folder.

    //Global Variables:

    public String gsPin = "";

    public File gfAppFolder;
    //public File gfAppConfigFile;

    public static final int MEDIA_CATEGORY_VIDEOS = 0;
    public static final int MEDIA_CATEGORY_IMAGES = 1;
    public static final int MEDIA_CATEGORY_COMICS = 2;

    public int giSelectedCatalogMediaCategory;

    public final File[] gfCatalogFolders = new File[3];
    public final File[] gfCatalogLogsFolders = new File[3];
    public final File[] gfCatalogContentsFiles = new File[3];
    public final int giCatalogFileVersion = 5;
    public final File[] gfCatalogTagsFiles = new File[3];
    //Video tag variables:
    public final int giTagFileVersion = 1;
    public final List<TreeMap<String, ItemClass_Tag>> gtmCatalogTagReferenceLists = new ArrayList<>();
    public final List<TreeMap<String, ItemClass_CatalogItem>> gtmCatalogLists = new ArrayList<>();
    public final boolean[] gbJustImported = {false, false, false};
    public static final String[] gsCatalogFolderNames = {"Videos", "Images", "Comics"};

    //Activity_CatalogViewer variables shared with Service_CatalogViewer:
    public TreeMap<Integer, ItemClass_CatalogItem> gtmCatalogViewerDisplayTreeMap;
    public static final int SORT_BY_DATETIME_LAST_VIEWED = 0;
    public static final int SORT_BY_DATETIME_IMPORTED = 1;
    public int[] giCatalogViewerSortBySetting = {SORT_BY_DATETIME_LAST_VIEWED, SORT_BY_DATETIME_LAST_VIEWED, SORT_BY_DATETIME_LAST_VIEWED};
    public boolean[] gbCatalogViewerSortAscending = {true, true, true};
    public boolean[] gbCatalogViewerFiltered = {false, false, false};
    public String[] gsCatalogViewerFilterText = {"", "", ""};
    public boolean gbCatalogViewerTagsRestrictionsOn;
    //End catalog viewer variables.

    public static final String gsNHComicCoverPageFilter = "^\\d{1,7}_Cover.+"; //A regex filter for getting the cover file for a NHComicDownloader file set.
    public static final String gsNHComicPageFilter = "^\\d{1,7}_Page.+"; //A regex filter for getting the cover file for a NHComicDownloader file set.

    public static final String[][] CatalogRecordFields = new String[][]{
            {"VIDEO_ID",
                    "VIDEO_FILENAME",
                    "SIZE_MB",
                    "DURATION_MILLISECONDS",
                    "DURATION_TEXT",
                    "RESOLUTION",
                    "FOLDER_NAME",
                    "TAGS",
                    "CAST",
                    "SOURCE",
                    "DATETIME_LAST_VIEWED_BY_USER",
                    "DATETIME_IMPORT"},
            {"IMAGE_ID",
                    "IMAGE_FILENAME",
                    "SIZE_KB",
                    "DIMENSIONS",
                    "FOLDER_NAME",
                    "TAGS",
                    "CAST",
                    "SOURCE",
                    "DATETIME_LAST_VIEWED_BY_USER",
                    "DATETIME_IMPORT"},
            {"COMIC_ID",
                    "COMIC_NAME",
                    "FILE_COUNT",
                    "MAX_PAGE_ID",
                    "MISSING_PAGES",
                    "COMIC_SIZE_KB",
                    "FOLDER_NAME",
                    "THUMBNAIL_FILE",
                    "PARODIES",
                    "CHARACTERS",
                    "TAGS",
                    "ARTISTS",
                    "GROUPS",
                    "LANGUAGES",
                    "CATEGORIES",
                    "PAGES",
                    "SOURCE",
                    "DATETIME_LAST_READ_BY_USER",
                    "DATETIME_IMPORT",
                    "ONLINE_DATA_ACQUIRED"}};



//=====================================================================================
    //===== Videos Variables Section ======================================================
    //=====================================================================================

    public static final int VIDEO_ID_INDEX = 0;                             //Video ID
    public static final int VIDEO_FILENAME_INDEX = 1;
    public static final int VIDEO_SIZE_MB_INDEX = 2;
    public static final int VIDEO_DURATION_MILLISECONDS_INDEX = 3;          //Duration of video in Milliseconds
    public static final int VIDEO_DURATION_TEXT_INDEX = 4;                  //Duration of video text
    public static final int VIDEO_RESOLUTION_INDEX = 5;                     //Width x Height
    public static final int VIDEO_FOLDER_NAME_INDEX = 6;                    //Name of the folder holding the video
    public static final int VIDEO_TAGS_INDEX = 7;                           //Tags given to the video
    public static final int VIDEO_CAST_INDEX = 8;
    public static final int VIDEO_SOURCE_INDEX = 9;                         //Website, if relevant
    public static final int VIDEO_DATETIME_LAST_VIEWED_BY_USER_INDEX = 10;   //Date of last read by user. Used for sorting if desired
    public static final int VIDEO_DATETIME_IMPORT_INDEX = 11;               //Date of import. Used for sorting if desired



    //=====================================================================================
    //===== Images Variables Section ======================================================
    //=====================================================================================

    public static final int IMAGE_ID_INDEX = 0;                             //Image ID
    public static final int IMAGE_FILENAME_INDEX = 1;
    public static final int IMAGE_SIZE_KB_INDEX = 2;
    public static final int IMAGE_DIMENSIONS_INDEX = 3;
    public static final int IMAGE_FOLDER_NAME_INDEX = 4;                    //Name of the folder holding the imGE
    public static final int IMAGE_TAGS_INDEX = 5;                           //Tags given to the Image
    public static final int IMAGE_CAST_INDEX = 6;
    public static final int IMAGE_SOURCE_INDEX = 7;                         //Website, if relevant
    public static final int IMAGE_DATETIME_LAST_VIEWED_BY_USER_INDEX = 8;   //Date of last read by user. Used for sorting if desired
    public static final int IMAGE_DATETIME_IMPORT_INDEX = 9;                //Date of import. Used for sorting if desired

    //=====================================================================================
    //===== Comics Variables Section ======================================================
    //=====================================================================================

    public static final int COMIC_ID_INDEX = 0;                    //Comic ID
    public static final int COMIC_NAME_INDEX = 1;                  //Comic Name
    public static final int COMIC_FILE_COUNT_INDEX = 2;            //Files included with the comic
    public static final int COMIC_MAX_PAGE_ID_INDEX = 3;           //Max page ID extracted from file names
    public static final int COMIC_MISSING_PAGES_INDEX = 4;         //String of comma-delimited missing page numbers
    public static final int COMIC_SIZE_KB_INDEX = 5;               //Total size of all files in the comic
    public static final int COMIC_FOLDER_NAME_INDEX = 6;           //Name of the folder holding the comic pages
    public static final int COMIC_THUMBNAIL_FILE_INDEX = 7;        //Name of the file used as the thumbnail for the comic
    public static final int COMIC_PARODIES_INDEX = 8;
    public static final int COMIC_CHARACTERS_INDEX = 9;
    public static final int COMIC_TAGS_INDEX = 10;                 //Tags given to the comic
    public static final int COMIC_ARTISTS_INDEX = 11;
    public static final int COMIC_GROUPS_INDEX = 12;
    public static final int COMIC_LANGUAGES_INDEX = 13;            //Language(s) found in the comic
    public static final int COMIC_CATEGORIES_INDEX = 14;
    public static final int COMIC_PAGES_INDEX = 15;                //Total number of pages as defined at the comic source
    public static final int COMIC_SOURCE_INDEX = 16;                     //nHentai.net, other source, etc.
    public static final int COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX = 17; //Date of last read by user. Used for sorting if desired
    public static final int COMIC_DATETIME_IMPORT_INDEX = 18;            //Date of import. Used for sorting if desired
    public static final int COMIC_ONLINE_DATA_ACQUIRED_INDEX = 19;

    public static final String COMIC_ONLINE_DATA_ACQUIRED_NO = "No";
    public static final String COMIC_ONLINE_DATA_ACQUIRED_YES = "Yes";


    //Lookup tables
    public static final int[] giDataRecordIDIndexes = {
            VIDEO_ID_INDEX,
            IMAGE_ID_INDEX,
            COMIC_ID_INDEX};

    public static final int[] giDataRecordFileNameIndexes = {
            VIDEO_FILENAME_INDEX,
            IMAGE_FILENAME_INDEX,
            -1};// -1, there is no descriptive comic file name.

    public static final int[] giDataRecordDateTimeImportIndexes = {
            VIDEO_DATETIME_IMPORT_INDEX,
            IMAGE_DATETIME_IMPORT_INDEX,
            COMIC_DATETIME_IMPORT_INDEX};

    public static final int[] giDataRecordDateTimeViewedIndexes = {
            VIDEO_DATETIME_LAST_VIEWED_BY_USER_INDEX,
            IMAGE_DATETIME_LAST_VIEWED_BY_USER_INDEX,
            COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX};

    public static final int[] giDataRecordTagsIndexes = {
            VIDEO_TAGS_INDEX,
            IMAGE_TAGS_INDEX,
            COMIC_TAGS_INDEX};

    public static final int[] giDataRecordFolderIndexes = {
            VIDEO_FOLDER_NAME_INDEX,
            IMAGE_FOLDER_NAME_INDEX,
            COMIC_FOLDER_NAME_INDEX}; //The record index to find the item's folder.

    public static final int[] giDataRecordRecyclerViewImageIndexes = {
            VIDEO_FILENAME_INDEX,
            IMAGE_FILENAME_INDEX,
            COMIC_THUMBNAIL_FILE_INDEX};

    public static final String[]  gsRestrictedTagsPreferenceNames = new String[]{
            "multi_select_list_videos_restricted_tags",
            "multi_select_list_images_restricted_tags",
            "multi_select_list_comics_restricted_tags"};

    public static final String gsPinPreference = "preferences_pin";

    public static final String gsUnsortedFolderName = "etc";  //Used when imports are placed in a folder based on their assigned tags.

    //todo: get rid of these variables as they are handled as arrayList items above:
    public File gfComicsFolder;
    public File gfComicLogsFolder;
    public File gfComicCatalogContentsFile;
    public final TreeMap<Integer, String[]> gtmCatalogComicList = new TreeMap<>();
    public boolean gbComicJustImported = false;



    //Each tags file has the same fields:
    public static final int TAG_ID_INDEX = 0;                    //Tag ID
    public static final int TAG_NAME_INDEX = 1;                  //Tag Name
    public static final int TAG_DESCRIPTION_INDEX = 3;           //Tag Description
    public final String[] TagRecordFields = new String[]{
            "TAG_ID",
            "TAG_NAME",
            "DESCRIPTION"};


    public void SendToast(Context context, String sMessage){
        Toast.makeText(context, sMessage, Toast.LENGTH_SHORT).show();
    }

    //=====================================================================================
    //===== Network Monitoring ============================================================
    //=====================================================================================
    public boolean isNetworkConnected = false;
    public ConnectivityManager connectivityManager;
    // Network Check
    public void registerNetworkCallback() {
        try {
            connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
                                                                   @Override
                                                                   public void onAvailable(Network network) {
                                                                       isNetworkConnected = true; // Global Static Variable
                                                                   }
                                                                   @Override
                                                                   public void onLost(Network network) {
                                                                       isNetworkConnected = false; // Global Static Variable
                                                                   }
                                                               }

            );
            isNetworkConnected = false;
        }catch (Exception e){
            isNetworkConnected = false;
        }
    }

    //User-set option:
    public final boolean bAutoDownloadOn = true; //By default, auto-download details is on.
      //This uses little network resource because we are just getting html data.


    //=====================================================================================
    //===== Utilities =====================================================================
    //=====================================================================================

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        try {
            if(activity.getCurrentFocus() != null) {
                inputMethodManager.hideSoftInputFromWindow(
                        activity.getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e){
            //Most likely null pointer if keyboard is not shown.
        }
    }

    public long AvailableStorageSpace(Context c, Integer iDevice) {
        //Returns space available in kB.
        long freeBytesExternal = 0;
        File[] fAvailableDirs = c.getExternalFilesDirs(null);
        if (fAvailableDirs.length >= iDevice) {
            //Examine the likely SDCard:
            freeBytesExternal = new File(fAvailableDirs[iDevice].toString()).getFreeSpace();
        } else {
            Toast.makeText(c, "Storage device " + iDevice + " not found.", Toast.LENGTH_LONG).show();
        }

        return freeBytesExternal;
    }

    static final String gsDatePatternFileSafe = "yyyyMMdd_HHmmss";
    static final String gsDatePatternNumSort = "yyyyMMdd.HHmmss";
    static final String gsDatePatternReadReady = "yyyy-MM-dd HH:mm:ss";
    static DateTimeFormatter gdtfDateFormatter;

    public String GetTimeStampFileSafe(){
        //For putting a timestamp on a file name. Observant of illegal characters.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternFileSafe);
        return gdtfDateFormatter.format(LocalDateTime.now());
    }

    public static Double GetTimeStampFloat(){
        //Get an easily-comparable time stamp.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternNumSort);
        String sTimeStamp = gdtfDateFormatter.format(LocalDateTime.now());
        return Double.parseDouble(sTimeStamp);
    }

    public String GetTimeStampReadReady(){
        //Get an easily readable time stamp.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternReadReady);
        return gdtfDateFormatter.format(LocalDateTime.now());
    }

    public static String JumbleStorageText(String sSourceText){
        if(sSourceText.equals("")){
            return "";
        }
        //Render the text unsearchable so that no scanning system can pick up explicit tags.
        String sFinalText;
        StringBuilder sbReverse = new StringBuilder();
        sbReverse.append(sSourceText);
        sFinalText = sbReverse.reverse().toString();

        return sFinalText;
    }

    public static String JumbleStorageText(int i) {
        return JumbleStorageText(Integer.toString(i));
    }

    public static String JumbleStorageText(double d) {
        return JumbleStorageText(Double.toString(d));
    }

    public static String JumbleStorageText(long l) {
        return JumbleStorageText(Long.toString(l));
    }

    public static String JumbleStorageText(boolean b) {
        return JumbleStorageText(Boolean.toString(b));
    }

    public static String JumbleFileName(String sFileName){
        if(sFileName.equals("")){
            return "";
        }
        //Reverse the text on the file so that the file does not get picked off by a search tool:
        StringBuilder sFileNameExtJumble = new StringBuilder();
        sFileNameExtJumble.append(sFileName.substring(sFileName.lastIndexOf(".") + 1));
        StringBuilder sFileNameBody = new StringBuilder();
        sFileNameBody.append(sFileName.substring(0,sFileName.lastIndexOf(".")));
        return sFileNameBody.reverse().toString() + "." + sFileNameExtJumble.reverse().toString();
    }

    //=====================================================================================
    //===== Catalog Subroutines Section ===================================================
    //=====================================================================================

    public static String[] getCatalogRecordString(ItemClass_CatalogItem ci){
        // Return value:
        // [0] - Header
        // [1] - Human-readable for searching text
        // [2] - Meant to write to a file

        String sHeader = "";
        sHeader = sHeader + "MediaCategory";                         //Video, image, or comic.
        sHeader = sHeader + "\t" + "ItemID";                         //Video, image, comic id
        sHeader = sHeader + "\t" + "Filename";                       //Video or image filename
        sHeader = sHeader + "\t" + "Folder_Name";                    //Name of the folder holding the video, image, or comic pages
        sHeader = sHeader + "\t" + "Thumbnail_File";                 //Name of the file used as the thumbnail for a video or comic
        sHeader = sHeader + "\t" + "Datetime_Import";                //Date of import. Used for sorting if desired
        sHeader = sHeader + "\t" + "Datetime_Last_Viewed_by_User";   //Date of last read by user. Used for sorting if desired
        sHeader = sHeader + "\t" + "Tags";                           //Tags given to the video, image, or comic
        sHeader = sHeader + "\t" + "Height";                         //Video or image dimension/resolution
        sHeader = sHeader + "\t" + "Width";                          //Video or image dimension/resolution
        sHeader = sHeader + "\t" + "Duration_Milliseconds";          //Duration of video in milliseconds
        sHeader = sHeader + "\t" + "Duration_Text";                  //Duration of video text in 00:00:00 format
        sHeader = sHeader + "\t" + "Resolution";                     //Resolution for sorting at user request
        sHeader = sHeader + "\t" + "Size";                           //Size of video, image, or size of all files in the comic, in Bytes
        sHeader = sHeader + "\t" + "Cast";                           //For videos and images

        //Comic-related variables:
        sHeader = sHeader + "\t" + "ComicArtists";                   //Common comic tag category
        sHeader = sHeader + "\t" + "ComicCategories";                //Common comic tag category
        sHeader = sHeader + "\t" + "ComicCharacters";                //Common comic tag category
        sHeader = sHeader + "\t" + "ComicGroups";                    //Common comic tag category
        sHeader = sHeader + "\t" + "ComicLanguages";                 //Language(s) found in the comic
        sHeader = sHeader + "\t" + "ComicParodies";                  //Common comic tag category
        sHeader = sHeader + "\t" + "ComicName";                      //Comic name
        sHeader = sHeader + "\t" + "ComicPages";                     //Total number of pages as defined at the comic source
        sHeader = sHeader + "\t" + "Comic_Max_Page_ID";              //Max comic page id extracted from file names
        sHeader = sHeader + "\t" + "Comic_Missing_Pages";            //Missing page numbers
        sHeader = sHeader + "\t" + "Comic_File_Count";               //Files included with the comic. Can be used for egrity check.
        sHeader = sHeader + "\t" + "Comic_Online_Data_Acquired";     //Typically used to gather tag data from an online comic source, if automatic.
        sHeader = sHeader + "\t" + "Comic_Source";

        String sReadableData = ""; //To be used for textual searches
        sReadableData = sReadableData + ci.iMediaCategory;                         //Video, image, or comic.
        sReadableData = sReadableData + "\t" + ci.sItemID;                         //Video, image, comic id
        sReadableData = sReadableData + "\t" + JumbleFileName(ci.sFilename);       //Video or image filename. Filename used by storage is obfuscated. De-jumble to make readable.
        sReadableData = sReadableData + "\t" + ci.sFolder_Name;                    //Name of the folder holding the video, image, or comic pages
        sReadableData = sReadableData + "\t" + JumbleFileName(ci.sThumbnail_File); //Name of the file used as the thumbnail for a video or comic
        sReadableData = sReadableData + "\t" + ci.dDatetime_Import;                //Date of import. Used for sorting if desired
        sReadableData = sReadableData + "\t" + ci.dDatetime_Last_Viewed_by_User;   //Date of last read by user. Used for sorting if desired
        sReadableData = sReadableData + "\t" + ci.sTags;                           //Tags given to the video, image, or comic
        sReadableData = sReadableData + "\t" + ci.iHeight;                         //Video or image dimension/resolution
        sReadableData = sReadableData + "\t" + ci.iWidth;                          //Video or image dimension/resolution
        sReadableData = sReadableData + "\t" + ci.lDuration_Milliseconds;          //Duration of video in milliseconds
        sReadableData = sReadableData + "\t" + ci.sDuration_Text;                  //Duration of video text in 00:00:00 format
        sReadableData = sReadableData + "\t" + ci.sResolution;                     //Resolution for sorting at user request
        sReadableData = sReadableData + "\t" + ci.lSize;                           //Size of video, image, or size of all files in the comic, in Bytes
        sReadableData = sReadableData + "\t" + ci.sCast;                           //For videos and images

        //Comic-related variables:
        sReadableData = sReadableData + "\t" + ci.sComicArtists;                   //Common comic tag category
        sReadableData = sReadableData + "\t" + ci.sComicCategories;                //Common comic tag category
        sReadableData = sReadableData + "\t" + ci.sComicCharacters;                //Common comic tag category
        sReadableData = sReadableData + "\t" + ci.sComicGroups;                    //Common comic tag category
        sReadableData = sReadableData + "\t" + ci.sComicLanguages;                 //Language(s) found in the comic
        sReadableData = sReadableData + "\t" + ci.sComicParodies;                  //Common comic tag category
        sReadableData = sReadableData + "\t" + ci.sComicName;                      //Comic name
        sReadableData = sReadableData + "\t" + ci.iComicPages;                     //Total number of pages as defined at the comic source
        sReadableData = sReadableData + "\t" + ci.iComic_Max_Page_ID;              //Max comic page id extracted from file names
        sReadableData = sReadableData + "\t" + ci.sComic_Missing_Pages;            //Missing page numbers
        sReadableData = sReadableData + "\t" + ci.iComic_File_Count;               //Files included with the comic. Can be used for egrity check.
        sReadableData = sReadableData + "\t" + ci.bComic_Online_Data_Acquired;     //Typically used to gather tag data from an online comic source, if automatic.
        sReadableData = sReadableData + "\t" + ci.sSource;                   //Website, if relevant. ended for comics.

        String sRecord = "";  //To be used when writing the catalog file.
        sRecord = sRecord + ci.iMediaCategory;                                            //Video, image, or comic.
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sItemID);                         //Video, image, comic id
        sRecord = sRecord + "\t" + ci.sFilename;                                          //Video or image filename
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sFolder_Name);                    //Name of the folder holding the video, image, or comic pages
        sRecord = sRecord + "\t" + ci.sThumbnail_File;                                    //Name of the file used as the thumbnail for a video or comic
        sRecord = sRecord + "\t" + JumbleStorageText(ci.dDatetime_Import);                //Date of import. Used for sorting if desired
        sRecord = sRecord + "\t" + JumbleStorageText(ci.dDatetime_Last_Viewed_by_User);   //Date of last read by user. Used for sorting if desired
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sTags);                           //Tags given to the video, image, or comic
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iHeight);                         //Video or image dimension/resolution
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iWidth);                          //Video or image dimension/resolution
        sRecord = sRecord + "\t" + JumbleStorageText(ci.lDuration_Milliseconds);          //Duration of video in milliseconds
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sDuration_Text);                  //Duration of video text in 00:00:00 format
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sResolution);                     //Resolution for sorting at user request
        sRecord = sRecord + "\t" + JumbleStorageText(ci.lSize);                           //Size of video, image, or size of all files in the comic, in Bytes
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sCast);                           //For videos and images

        //Comic-related variables:
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicArtists);                   //Common comic tag category
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicCategories);                //Common comic tag category
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicCharacters);                //Common comic tag category
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicGroups);                    //Common comic tag category
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicLanguages);                 //Language(s) found in the comic
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicParodies);                  //Common comic tag category
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicName);                      //Comic name
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iComicPages);                     //Total number of pages as defined at the comic source
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iComic_Max_Page_ID);              //Max comic page id extracted from file names
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComic_Missing_Pages);            //Missing page numbers
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iComic_File_Count);               //Files included with the comic. Can be used for egrity check.
        sRecord = sRecord + "\t" + JumbleStorageText(ci.bComic_Online_Data_Acquired);     //Typically used to gather tag data from an online comic source, if automatic.
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sSource);                   //Website, if relevant. ended for comics.
        
        return new String[]{sHeader,sReadableData,sRecord};
    }
    
    public static ItemClass_CatalogItem ConvertStringToCatalogItem(String[] sRecord){
        //Designed for interpretting a line as read from a catalog file.
        ItemClass_CatalogItem ci =  new ItemClass_CatalogItem();
        ci.iMediaCategory = Integer.parseInt(sRecord[0]);                                            //Video, image, or comic.
        ci.sItemID = JumbleStorageText(sRecord[1]);                         //Video, image, comic id
        ci.sFilename = sRecord[2];                                          //Video or image filename
        ci.sFolder_Name = JumbleStorageText(sRecord[3]);                    //Name of the folder holding the video, image, or comic pages
        ci.sThumbnail_File = sRecord[4];                                    //Name of the file used as the thumbnail for a video or comic
        ci.dDatetime_Import = Double.parseDouble(JumbleStorageText(sRecord[5]));                //Date of import. Used for sorting if desired
        ci.dDatetime_Last_Viewed_by_User = Double.parseDouble(JumbleStorageText(sRecord[6]));   //Date of last read by user. Used for sorting if desired
        ci.sTags = JumbleStorageText(sRecord[7]);                           //Tags given to the video, image, or comic
        ci.iHeight = Integer.parseInt(JumbleStorageText(sRecord[8]));                         //Video or image dimension/resolution
        ci.iWidth = Integer.parseInt(JumbleStorageText(sRecord[9]));                          //Video or image dimension/resolution
        ci.lDuration_Milliseconds = Long.parseLong(JumbleStorageText(sRecord[10]));          //Duration of video in milliseconds
        ci.sDuration_Text = JumbleStorageText(sRecord[11]);                  //Duration of video text in 00:00:00 format
        ci.sResolution = JumbleStorageText(sRecord[12]);                     //Resolution for sorting at user request
        ci.lSize = Long.parseLong(JumbleStorageText(sRecord[13]));                           //Size of video, image, or size of all files in the comic, in Bytes
        ci.sCast = JumbleStorageText(sRecord[14]);                           //For videos and images

        //Comic-related variables:
        ci.sComicArtists = JumbleStorageText(sRecord[15]);                   //Common comic tag category
        ci.sComicCategories = JumbleStorageText(sRecord[16]);                //Common comic tag category
        ci.sComicCharacters = JumbleStorageText(sRecord[17]);                //Common comic tag category
        ci.sComicGroups = JumbleStorageText(sRecord[18]);                    //Common comic tag category
        ci.sComicLanguages = JumbleStorageText(sRecord[19]);                 //Language(s = sRecord[0] found in the comic
        ci.sComicParodies = JumbleStorageText(sRecord[20]);                  //Common comic tag category
        ci.sComicName = JumbleStorageText(sRecord[21]);                      //Comic name
        ci.iComicPages = Integer.parseInt(JumbleStorageText(sRecord[22]));                     //Total number of pages as defined at the comic source
        ci.iComic_Max_Page_ID = Integer.parseInt(JumbleStorageText(sRecord[23]));              //Max comic page id extracted from file names
        ci.sComic_Missing_Pages = JumbleStorageText(sRecord[24]);            //Missing page numbers
        ci.iComic_File_Count = Integer.parseInt(JumbleStorageText(sRecord[25]));               //Files included with the comic. Can be used for egrity check.
        ci.bComic_Online_Data_Acquired = Boolean.parseBoolean(JumbleStorageText(sRecord[26]));     //Typically used to gather tag data from an online comic source, if automatic.
        if(sRecord.length == 28) { //String.split will not give the last item if it is an empty string.
            ci.sSource = JumbleStorageText(sRecord[27]);                   //Website, if relevant. ended for comics.
        }
        return ci;
    }

    public static ItemClass_CatalogItem ConvertStringToCatalogItem(String sRecord){
        String[] sRecord2 =  sRecord.split("\t");
        return ConvertStringToCatalogItem(sRecord2);
    }

    public void CatalogDataFile_CreateNewRecord(ItemClass_CatalogItem ci){

        File fCatalogContentsFile = gfCatalogContentsFiles[ci.iMediaCategory];

        TreeMap<String, ItemClass_CatalogItem> tmCatalogRecords = gtmCatalogLists.get(ci.iMediaCategory);

        try {

            //Add the details to the TreeMap:
            tmCatalogRecords.put(ci.sItemID, ci);

            String sLine = getCatalogRecordString(ci)[2];
            
            //Write the data to the file:
            FileWriter fwNewCatalogContentsFile = new FileWriter(fCatalogContentsFile, true);
            fwNewCatalogContentsFile.write(sLine);
            fwNewCatalogContentsFile.write("\n");
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + fCatalogContentsFile.getPath() + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    public void CatalogDataFile_UpdateRecord(ItemClass_CatalogItem ci) {

        File fCatalogContentsFile = gfCatalogContentsFiles[ci.iMediaCategory];
        TreeMap<String, ItemClass_CatalogItem> tmCatalogRecords = gtmCatalogLists.get(ci.iMediaCategory);

        try {

            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fCatalogContentsFile.getAbsolutePath()));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");

            String[] sFields;
            String sLine = brReader.readLine();
            ItemClass_CatalogItem ciFromFile;
            while (sLine != null) {
                ciFromFile = ConvertStringToCatalogItem(sLine);

                //Check to see if this record is the one that we want to update:
                if (ciFromFile.sItemID.equals(ci.sItemID)) {
                    sLine = getCatalogRecordString(ci)[2];

                    //Now update the record in the treeMap:
                    tmCatalogRecords.put(ci.sItemID,ci);

                }

                //Write the current record to the buffer:
                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            //Write the data to the file:
            FileWriter fwNewCatalogContentsFile = new FileWriter(fCatalogContentsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + fCatalogContentsFile.getPath() + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public boolean ComicCatalog_DeleteComic(ItemClass_CatalogItem ci) {

        //Delete the comic record from the CatalogContentsFile:

        try {

            String  sComicFolderPath = gfCatalogFolders[MEDIA_CATEGORY_COMICS].getPath() + File.separator
                    + ci.sFolder_Name;

            File fComicFolderToBeDeleted = new File(sComicFolderPath);
            if(fComicFolderToBeDeleted.exists() && fComicFolderToBeDeleted.isDirectory()){
                try{
                    //First, the directory must be empty to delete. So delete all files in folder:
                    String[] sChildFiles = fComicFolderToBeDeleted.list();
                    if(sChildFiles != null) {
                        for (String sChildFile : sChildFiles) {
                            File fChildFile = new File(fComicFolderToBeDeleted, sChildFile);
                            if(!fChildFile.delete()){
                                Toast.makeText(this, "Could not delete file of comic ID " + ci.sItemID + ":\n" +
                                                sChildFile,
                                                Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    if(!fComicFolderToBeDeleted.delete()){
                        Toast.makeText(this, "Could not delete folder of comic ID " + ci.sItemID + ".",
                                Toast.LENGTH_LONG).show();
                        return false;
                    }
                } catch (Exception e){
                    Toast.makeText(this, "Could not delete folder of comic ID " + ci.sItemID + ".",
                            Toast.LENGTH_LONG).show();
                    return false;
                }
            } else {
                Toast.makeText(this, "Could not find folder of to-be-deleted comic ID " + ci.sItemID + ".",
                        Toast.LENGTH_LONG).show();
            }

            //Now attempt to delete the comic record from the CatalogContentsFile:
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(gfCatalogContentsFiles[MEDIA_CATEGORY_COMICS].getAbsolutePath()));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");

            String[] sFields;
            String sLine = brReader.readLine();

            ItemClass_CatalogItem ciFromFile;

            while (sLine != null) {

                ciFromFile = ConvertStringToCatalogItem(sLine);

                //Check to see if this record is the one that we want to update:
                if (!ciFromFile.sItemID.equals(ci.sItemID)) {
                    //If the line is not the comic we are trying to delete, transfer it over:
                    sbBuffer.append(sLine);
                    sbBuffer.append("\n");
                }

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            //Re-write the CatalogContentsFile without the deleted comic's data record:
            FileWriter fwNewCatalogContentsFile = new FileWriter(gfCatalogContentsFiles[MEDIA_CATEGORY_COMICS], false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();

            //Now update memory to no longer include the comic:
            gtmCatalogLists.get(MEDIA_CATEGORY_COMICS).remove(ci.sItemID);
            Toast.makeText(this, "Comic ID " + ci.sItemID + " removed from catalog.",
                    Toast.LENGTH_LONG).show();
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public boolean CatalogDataFile_UpdateAllRecords(File fCatalogContentsFile, int[] iFieldIDs, String[] sFieldUpdateData) {
        //This routine used to update a single field across all records in a catalog file.
        //  Used primarily during development of the software.
        //  CatalogDataFile_UpdateAllRecords = Update field(s) to a common value.
        try {
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fCatalogContentsFile.getAbsolutePath()));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                int j = 0; //To track requested field updates.
                sFields = sLine.split("\t",-1);

                StringBuilder sb = new StringBuilder();
                if (iFieldIDs[j] == 0) {
                    //If the caller wishes to update field 0...
                    sb.append(sFieldUpdateData[j]);
                    j++;
                } else {
                    sb.append(sFields[0]);
                }
                for (int i = 1; i < sFields.length; i++) {
                    sb.append("\t");
                    if(j < iFieldIDs.length) {
                        if (iFieldIDs[j] == i) {
                            //If the caller wishes to update field i...
                            sb.append(sFieldUpdateData[j]);
                            j++;
                        } else {
                            sb.append(sFields[i]);
                        }
                    } else {
                        sb.append(sFields[i]);
                    }
                }
                sLine = sb.toString();

                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            FileWriter fwNewCatalogContentsFile = new FileWriter(fCatalogContentsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public void CatalogDataFile_UpdateAllRecords_SwitchTagTextToIDs(int iMediaCategory) {
        //This routine used to update all record's tags field to integer IDs for the tags.
        //  Used during development of the software.

        try {

            int[] iTagsField = {VIDEO_TAGS_INDEX, IMAGE_TAGS_INDEX, COMIC_TAGS_INDEX};

            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(gfCatalogContentsFiles[iMediaCategory].getAbsolutePath()));
            String sHeader = brReader.readLine();
            //Determine the file version, and don't update it if it is up-to-date:
            String[] sHeaderFields = sHeader.split("\t");
            String sVersionField = sHeaderFields[sHeaderFields.length - 1];
            String[] sVersionFields = sVersionField.split("\\.");
            if(Integer.parseInt(sVersionFields[1]) >= giCatalogFileVersion){
                return;
            }
            //Re-form the header line with the new file version:
            StringBuilder sbNewHeaderLine = new StringBuilder();
            for(int i = 0; i < sHeaderFields.length - 1; i++){
                sbNewHeaderLine.append(sHeaderFields[i]);
                sbNewHeaderLine.append("\t");
            }
            sbNewHeaderLine.append("FILEVERSION.");
            sbNewHeaderLine.append(giCatalogFileVersion); //Append the current file version number.
            sbBuffer.append(sbNewHeaderLine);
            sbBuffer.append("\n"); //Append newline character.

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                sFields = sLine.split("\t",-1);

                StringBuilder sb = new StringBuilder();
                sb.append(sFields[0]); //Append the first field, which would not be the tags field.
                for (int i = 1; i < sFields.length; i++) {
                    sb.append("\t");

                    if (i == iTagsField[iMediaCategory]) {
                        //This is the tags field. Convert the tag text to integers.
                        String[] sTagsText = sFields[i].split(",");
                        StringBuilder sbTagsInt = new StringBuilder();
                        for(String sTag: sTagsText){
                            Integer iTagID = getTagIDFromText(JumbleStorageText(sTag.trim()), iMediaCategory);
                            if(iTagID < 0){ //Tag Text not found.
                                iTagID = 9999;
                            }
                            sbTagsInt.append(iTagID.toString());
                            sbTagsInt.append(",");
                        }
                        String sTagsInt = sbTagsInt.toString();
                        //Remove trailing comma:
                        if(sTagsInt.contains(",")) {
                            sTagsInt = sTagsInt.substring(0, sTagsInt.lastIndexOf(","));
                        }
                        sb.append(JumbleStorageText(sTagsInt));
                    } else {
                        sb.append(sFields[i]);
                    }


                }
                sLine = sb.toString();

                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            FileWriter fwNewCatalogContentsFile = new FileWriter(gfCatalogContentsFiles[iMediaCategory], false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static boolean CatalogDataFile_UpdateAllRecords_TimeStamps(File fCatalogContentsFile) {
        //This routine used to update a single field across all record in a catalog file.
        //  Used primarily during development of the software.

        try {
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fCatalogContentsFile.getAbsolutePath()));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");
            //finished header processing.

            //Apply an import timestamp:
            double dTimeStamp = GetTimeStampFloat();
            String sDateTime;


            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                dTimeStamp += 0.000100; //Increment the timestamp by one minute.
                sDateTime = Double.toString(dTimeStamp);
                sFields = sLine.split("\t",-1);

                StringBuilder sb = new StringBuilder();
                sb.append(sFields[0]);

                for (int i = 1; i < sFields.length; i++) {
                    sb.append("\t");
                    if (i == COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX) {
                        //If this is the field to update...
                        sb.append( JumbleStorageText(sDateTime));
                    } else {
                        sb.append(sFields[i]);
                    }

                }
                sLine = sb.toString();

                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            FileWriter fwNewCatalogContentsFile = new FileWriter(fCatalogContentsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void CatalogDataFile_UpdateAllRecords_UnJumbleVideoFileName() {
        //This routine used to update all video records such that the file name matches the
        //  storage file name.

        try {

            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(gfCatalogContentsFiles[MEDIA_CATEGORY_VIDEOS].getAbsolutePath()));
            String sHeader = brReader.readLine();
            //Determine the file version, and don't update it if it is up-to-date:
            String[] sHeaderFields = sHeader.split("\t");
            String sVersionField = sHeaderFields[sHeaderFields.length - 1];
            String[] sVersionFields = sVersionField.split("\\.");
            if(Integer.parseInt(sVersionFields[1]) >= giCatalogFileVersion){
                return;
            }
            //Re-form the header line with the new file version:
            StringBuilder sbNewHeaderLine = new StringBuilder();
            for(int i = 0; i < sHeaderFields.length - 1; i++){
                sbNewHeaderLine.append(sHeaderFields[i]);
                sbNewHeaderLine.append("\t");
            }
            sbNewHeaderLine.append("FILEVERSION.");
            sbNewHeaderLine.append(giCatalogFileVersion); //Append the current file version number.
            sbBuffer.append(sbNewHeaderLine);
            sbBuffer.append("\n"); //Append newline character.

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                sFields = sLine.split("\t",-1);

                StringBuilder sb = new StringBuilder();
                sb.append(sFields[0]); //Append the first field, which would not be the tags field.
                for (int i = 1; i < sFields.length; i++) {
                    sb.append("\t");

                    if (i == VIDEO_FILENAME_INDEX) {
                        //This is the filename field. Reverse the jumble:
                        sb.append(JumbleStorageText(sFields[i]));
                    } else {
                        sb.append(sFields[i]);
                    }

                }
                sLine = sb.toString();

                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            FileWriter fwNewCatalogContentsFile = new FileWriter(gfCatalogContentsFiles[MEDIA_CATEGORY_VIDEOS], false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //=====================================================================================
    //===== Tag Subroutines Section ===================================================
    //=====================================================================================

    public TreeMap<String, ItemClass_Tag> InitTagData(int iMediaCategory){
        TreeMap<String, ItemClass_Tag> tmTags = new TreeMap<>();

        File fTagsFile = gfCatalogTagsFiles[iMediaCategory];

        //Get tag reference lists:
        int[] iTagStringArrayResources = {R.array.default_video_tags, R.array.default_video_tags, R.array.default_comic_tags};
        String[] sDefaultTags = getResources().getStringArray(iTagStringArrayResources[iMediaCategory]);


        if(fTagsFile.exists()) {
            //Get Tags from file:
            BufferedReader brReader;
            try {

                brReader = new BufferedReader(new FileReader(fTagsFile.getAbsolutePath()));
                brReader.readLine();//First line is the header, skip it.
                String sLine = brReader.readLine();

                while(sLine != null) {
                    String[] sFields;
                    sFields = sLine.split("\t");

                    //Reverse the text of each field:
                    for(int j = 0; j < sFields.length; j++) {
                        sFields[j] = JumbleStorageText(sFields[j]);
                    }

                    ItemClass_Tag ict;
                    String sTagName;
                    if(sFields.length < 2){
                        //The tag is blank.
                        sTagName = "";
                    } else {
                        sTagName = sFields[TAG_NAME_INDEX];
                    }
                    ict = new ItemClass_Tag(Integer.parseInt(sFields[TAG_ID_INDEX]), sTagName);
                    tmTags.put(sTagName, ict);
                    sLine = brReader.readLine();
                }

                brReader.close();

            } catch (IOException e) {
                Toast.makeText(this, "Trouble reading tags file at\n" + fTagsFile.getAbsolutePath() + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else { //If the tags file does not exist, create it and populate it with default values:
            try {
                if(fTagsFile.createNewFile()) {
                    try {
                        FileWriter fwTagsFile = new FileWriter(fTagsFile, false);
                        int i = 0;
                        //Write the header record:
                        fwTagsFile.write(TagRecordFields[0]);
                        for(int j = 1; j < TagRecordFields.length; j++){
                            fwTagsFile.write("\t" + TagRecordFields[j]);
                        }
                        fwTagsFile.write("\n");

                        //Write data records:
                        for (String sEntry : sDefaultTags) {
                            if(!sEntry.equals("")) {
                                fwTagsFile.write(JumbleStorageText(Integer.toString(i)) + "\t" + JumbleStorageText(sEntry) + "\n");
                                ItemClass_Tag ict = new ItemClass_Tag(i, sEntry);
                                tmTags.put(sEntry, ict);
                                i++;
                            }
                        }

                        //Close the tags file:
                        fwTagsFile.flush();
                        fwTagsFile.close();

                    } catch (IOException e) {
                        Toast.makeText(this, "Trouble writing file at\n" + fTagsFile.getAbsolutePath() + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Could not create file at\n" + fTagsFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            }catch (IOException e){
                Toast.makeText(this, "Could not create file at\n" + fTagsFile.getAbsolutePath() + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        return tmTags;
    }

    public int TagDataFile_CreateNewRecord(String sTagName, int iMediaCategory){

        File fTagsFile = gfCatalogTagsFiles[iMediaCategory];
        int iNextRecordId = -1;

        try {

            int iThisId;
            if(gtmCatalogTagReferenceLists.get(iMediaCategory).size() > 0) {
                for (Map.Entry<String, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                    iThisId = entry.getValue().TagID;
                    if (iThisId >= iNextRecordId) iNextRecordId = iThisId + 1;
                    if (entry.getValue().TagText.toLowerCase().equals(sTagName.toLowerCase())) {
                        //If the tag already exists, abort adding a new tag.
                        return -1;
                    }
                }
            } else {
                iNextRecordId = 0;
            }
            //New record ID identified.

            ItemClass_Tag ictNewTag = new ItemClass_Tag(iNextRecordId, sTagName);
            gtmCatalogTagReferenceLists.get(iMediaCategory).put(sTagName, ictNewTag);

            //Add the new record to the catalog file:
            String sLine = getTagRecordString(ictNewTag);
            FileWriter fwNewTagsFile = new FileWriter(fTagsFile, true);
            fwNewTagsFile.write(sLine);
            fwNewTagsFile.write("\n");
            fwNewTagsFile.flush();
            fwNewTagsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating Tags.dat.\n" + fTagsFile.getPath() + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return iNextRecordId;

    }

    public boolean TagDataFile_UpdateRecord(
            String sTagID,
            String sData,
            int iMediaCategory) {

        int[] iFieldIDs = {TAG_NAME_INDEX};
        String[] sFieldUpdateData = new String[]{sData};

        File fTagsFile = gfCatalogTagsFiles[iMediaCategory];

        try {

            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fTagsFile.getAbsolutePath()));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                int j = 0; //To track requested field updates.

                sFields = sLine.split("\t",-1);
                //De-jumble the data read from the file:
                String[] sFields2 = new String[sFields.length];
                for(int i = 0; i < sFields.length; i++){
                    sFields2[i] = JumbleStorageText(sFields[i]);
                }
                sFields = sFields2;

                //Check to see if this record is the one that we want to update:
                if (sFields[giDataRecordIDIndexes[iMediaCategory]].equals(sTagID)) {
                    StringBuilder sb = new StringBuilder();

                    if (iFieldIDs[j] == 0) {
                        //If the caller wishes to update field 0...
                        sb.append(sFieldUpdateData[j]);
                        j++;
                    } else {
                        sb.append(sFields[0]);
                    }

                    for (int i = 1; i < sFields.length; i++) {
                        sb.append("\t");
                        if(j < iFieldIDs.length) {
                            if (iFieldIDs[j] == i) {
                                //If the caller wishes to update field i...
                                sb.append(sFieldUpdateData[j]);
                                j++;
                            } else {
                                sb.append(sFields[i]);
                            }
                        } else {
                            sb.append(sFields[i]);
                        }
                    }

                    sLine = sb.toString();

                    //Now update the record in the treeMap:
                    sFields = sLine.split("\t",-1);
                    int iKey = -1;


                    String sKey = "";
                    for (Map.Entry<String, ItemClass_Tag>
                            TagEntry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                        if( TagEntry.getValue().TagID.equals(Integer.parseInt(sTagID))){
                            sKey = TagEntry.getKey();
                            break;
                        }
                    }

                    //Delete the pre-Existing tag item from the reference list because it hold the previous tag name as the
                    //  key value:
                    gtmCatalogTagReferenceLists.get(iMediaCategory).remove(sKey);
                    ItemClass_Tag ictNew = new ItemClass_Tag(Integer.parseInt(sTagID), sData);
                    gtmCatalogTagReferenceLists.get(iMediaCategory).put(sData, ictNew);

                    //Jumble the fields in preparation for writing to file:
                    sFields2 = sLine.split("\t",-1);
                    StringBuilder sbJumble = new StringBuilder();
                    sbJumble.append(JumbleStorageText(sFields2[0]));
                    for(int i = 1; i < sFields.length; i++){
                        sbJumble.append("\t");
                        sbJumble.append(JumbleStorageText(sFields2[i]));
                    }
                    sLine = sbJumble.toString();

                }
                //Write the current record to the buffer:
                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            //Write the data to the file:
            FileWriter fwNewCatalogContentsFile = new FileWriter(fTagsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating Tags.dat.\n" + fTagsFile.getPath() + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public String getTagRecordString(ItemClass_Tag ict){

        String sTagRecord =
                JumbleStorageText(ict.TagID.toString()) + "\t" +
                JumbleStorageText(ict.TagText);
        return sTagRecord;
    }

    public void TagsFile_UpdateAllRecords_JumbleTagID(int iMediaCategory) {
        //This routine used to update all tag records such that the tag ID is jumbled.

        try {

            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(gfCatalogTagsFiles[iMediaCategory].getAbsolutePath()));
            String sHeader = brReader.readLine();
            //Determine the file version, and don't update it if it is up-to-date:
            String[] sHeaderFields = sHeader.split("\t");
            /*String sVersionField = sHeaderFields[sHeaderFields.length - 1];
            String[] sVersionFields = sVersionField.split("\\.");
            if(Integer.parseInt(sVersionFields[1]) >= giCatalogFileVersion){
                return;
            }*/
            //Re-form the header line with the new file version:
            String sNewHeaderLine;
            StringBuilder sbNewHeaderLine = new StringBuilder();
            for(int i = 0; i < sHeaderFields.length - 1; i++){
                sbNewHeaderLine.append(sHeaderFields[i]);
                sbNewHeaderLine.append("\t");
            }
            sbNewHeaderLine.append("FILEVERSION.");
            sbNewHeaderLine.append(giTagFileVersion); //Append the current file version number.
            sbBuffer.append(sbNewHeaderLine);
            sbBuffer.append("\n"); //Append newline character.

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                sFields = sLine.split("\t",-1);

                StringBuilder sb = new StringBuilder();
                sb.append(JumbleStorageText(sFields[0]));
                for (int i = 1; i < sFields.length; i++) {
                    sb.append("\t");
                    sb.append(sFields[i]);
                }
                sLine = sb.toString();

                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            FileWriter fwNewCatalogContentsFile = new FileWriter(gfCatalogTagsFiles[iMediaCategory], false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating Tags.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public TreeMap<String, ItemClass_Tag> GetCatalogTagsInUse(Integer iMediaCategory){

        int[] iTagsField = {VIDEO_TAGS_INDEX, IMAGE_TAGS_INDEX, COMIC_TAGS_INDEX};


        SortedSet<String> ssTemp = new TreeSet<>();//todo: refactor.
        for(Map.Entry<String, ItemClass_CatalogItem>
                CatalogEntry : gtmCatalogLists.get(iMediaCategory).entrySet()) {
            //Sort the strings:
            String[] sTempArray = CatalogEntry.getValue().sTags.split(",");
            for (String s : sTempArray) {
                if(s.length() > 0) { //Zero-length string will cause problem for parseInt used later.
                    ssTemp.add(s.trim()); //This will not allow the addition of duplicate tags.
                }
            }
        }

        TreeMap<String, ItemClass_Tag> tmTagsInUse = new TreeMap<>();
        if(ssTemp.size() > 0) {
            //Format the strings:
            ArrayList<Integer> aliTagsInUse = new ArrayList<>();
            Iterator<String> isIterator = ssTemp.iterator();
            aliTagsInUse.add(Integer.parseInt(isIterator.next()));
            while (isIterator.hasNext()) {
                aliTagsInUse.add(Integer.parseInt(isIterator.next()));
            }

            for (Integer iTagID : aliTagsInUse) {
                String sTagText = getTagTextFromID(iTagID, iMediaCategory);
                ItemClass_Tag ict = new ItemClass_Tag(iTagID, sTagText);
                ItemClass_Tag ictTemp = gtmCatalogTagReferenceLists.get(iMediaCategory).get(ict.TagText);
                if (ictTemp != null) {
                    ict.isRestricted = ictTemp.isRestricted;
                }
                tmTagsInUse.put(sTagText, ict);
            }
        }

        return tmTagsInUse;
    }

    public String getTagTextFromID(Integer iTagID, Integer iMediaCategory){
        String sTagText = "[Tag ID " + iTagID + " not found]";

        for(Map.Entry<String, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            Integer iRefTag = entry.getValue().TagID;
            if(iRefTag.equals(iTagID)){
                sTagText = entry.getValue().TagText;
                break;
            }
        }


        return sTagText;
    }

    public ArrayList<String> getTagTextsFromIDs(ArrayList<Integer> ali, int iMediaCategory){
        ArrayList<String> als = new ArrayList<>();
        for(Integer i : ali){
            als.add(getTagTextFromID(i, iMediaCategory));
        }
        return als;
    }

    public Integer getTagIDFromText(String sTagText, Integer iMediaCategory){
        int iKey = -1;
        for(Map.Entry<String, ItemClass_Tag> entry: gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            if(entry.getValue().TagText.equalsIgnoreCase(sTagText)){
                iKey = entry.getValue().TagID;
                break;
            }
        }
        return iKey;
    }



    //=====================================================================================
    //===== Other Subroutines Section ===================================================
    //=====================================================================================

    public static String formDelimitedString(ArrayList<Integer> ali, String sDelimiter){
        //Used by preferences for storing integer string representing restricted tags.
        //Used to save tag IDs to catalog file.
        String sReturn;
        StringBuilder sb = new StringBuilder();
        for(Integer i : ali){
            sb.append(i.toString());
            sb.append(sDelimiter);
        }
        sReturn = sb.toString();
        if(sReturn.contains(sDelimiter)) {
            //Clear the trailing delimiter:
            sReturn = sReturn.substring(0, sReturn.lastIndexOf(sDelimiter));
        }
        return sReturn;
    }

    public static ArrayList<Integer> getIntegerArrayFromString(String sTags, String sDelimiter){
        ArrayList<Integer> ali = new ArrayList<>();

        if(sTags != null){
            if(sTags.length() > 0) {
                String[] sa = sTags.split(sDelimiter);
                for (String s : sa) {
                    ali.add(Integer.parseInt(s));
                }
            }
        }
        return ali;
    }

    public static final String STORAGE_SIZE_NO_PREFERENCE = "";
    public static final String STORAGE_SIZE_BYTES = "B";       //Must match sSizeSuffix chars by the manner of which CleanStorageSize is used.
    public static final String STORAGE_SIZE_KILOBYTES = "KB";  //Must match sSizeSuffix chars by the manner of which CleanStorageSize is used.
    public static final String STORAGE_SIZE_MEGABYTES = "MB";  //Must match sSizeSuffix chars by the manner of which CleanStorageSize is used.
    public static final String STORAGE_SIZE_GIGABYTES = "GB";  //Must match sSizeSuffix chars by the manner of which CleanStorageSize is used.
    public static String CleanStorageSize(Long lStorageSize, String sStorageSizePreference){
        //Returns a string of size to 2 significant figures plus units of B, KB, MB, or GB.

        String sSizeSuffix = " B";
        if( sStorageSizePreference.equals(STORAGE_SIZE_NO_PREFERENCE)) {
            if (lStorageSize > 1000) {
                lStorageSize /= 1024;
                sSizeSuffix = " KB";
            }
            if (lStorageSize > 1000) {
                lStorageSize /= 1024;
                sSizeSuffix = " MB";
            }
            if (lStorageSize > 1000) {
                lStorageSize /= 1024;
                sSizeSuffix = " GB";
            }
        } else {
            switch (sStorageSizePreference){
                case STORAGE_SIZE_GIGABYTES:
                    lStorageSize = lStorageSize / (long)Math.pow(1024, 3);
                    sSizeSuffix = " GB";
                    break;

                case STORAGE_SIZE_MEGABYTES:
                    lStorageSize = lStorageSize / (long)Math.pow(1024, 2);
                    sSizeSuffix = " MB";
                    break;

                case STORAGE_SIZE_KILOBYTES:
                    lStorageSize /= 1024;
                    sSizeSuffix = " KB";

                case STORAGE_SIZE_BYTES:
                    break;
            }
        }

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);

        return decimalFormat.format(lStorageSize) + sSizeSuffix;
    }

    public static String getDurationTextFromMilliseconds(long lMilliseconds){
        String sDurationText;
        sDurationText = String.format(Locale.getDefault(),"%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(lMilliseconds),
                TimeUnit.MILLISECONDS.toMinutes(lMilliseconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(lMilliseconds)),
                TimeUnit.MILLISECONDS.toSeconds(lMilliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(lMilliseconds)));
        return sDurationText;
    }



    //=====================================================================================
    //===== Obfuscation Section ===========================================================
    //=====================================================================================


    public boolean ObfuscationOn = false;
    public int iObfuscationIndex;
    //public int OBFUSCATION_SUBJECT_VIDEOGAMES = 0;
    public final int OBFUSCATION_SUBJECT_QUALITY = 1;
    public final int iObfuscationSubjectSelection = OBFUSCATION_SUBJECT_QUALITY;

    final int[][] iImageList = new int[][]{
            {
                    R.drawable.ovg_death_stranding,
                    R.drawable.ovg_doom_eternal,
                    R.drawable.ovg_fallout_new_vegas,
                    R.drawable.ovg_horizon_zero_dawn,
                    R.drawable.ovg_resident_evil_2
            },
            {
                    R.drawable.oq_cost_of_poor_quality,
                    R.drawable.oq_five_why,
                    R.drawable.oq_ishikawa_diagram,
                    R.drawable.oq_ishikawa_diagram2,
                    R.drawable.oq_kanban_method_principles,
                    R.drawable.oq_kanban_method,
                    R.drawable.oq_mccalls_quality_factors
            }
    };
    final String[][] sObfuscationCategoryNames = new String[][]{
            {
                    "Top 10 PS4 Games 2015",
                    "Top 10 PS4 Games 2016",
                    "Top 10 PS4 Games 2017",
                    "Top 10 PS4 Games 2018",
                    "Top 10 PS4 Games 2019"
            },
            {
                    "Cost of Poor Quality",
                    "Five Why Diagram",
                    "Ishikawa Diagram 1",
                    "Ishikawa Diagram 2",
                    "Kanban Method Principles",
                    "Kanban Method",
                    "McCall's Quality Factors"
            }
    };
    final String[] sObfuscatedProgramNames = new String[]{
            "Top Titles",
            "Quality Operations"
    };
    final String[] sNonObfuscatedProgramName = new String[]{"Videos Catalog", "Images Catalog", "Comics Catalog"};

    public int getObfuscationImageCount(){
        return iImageList[iObfuscationSubjectSelection].length;
    }

    public int getObfuscationImage(int index) {
        if(index >= iImageList[iObfuscationSubjectSelection].length - 1){
            index = 0;
        }
        return iImageList[iObfuscationSubjectSelection][index];
    }

    public String getObfuscationImageText(int index){
        return sObfuscationCategoryNames[iObfuscationSubjectSelection][index];
    }

    public String getObfuscationCategoryName(){
        return sObfuscationCategoryNames[iObfuscationSubjectSelection][iObfuscationIndex];
    }

    public String getObfuscatedProgramName() {
        return sObfuscatedProgramNames[iObfuscationSubjectSelection];
    }

    //End obfuscation section.

    //=====================================================================================
    //===== Comic Page Viewer Activity Options =====================================================
    //=====================================================================================
    //CPV = "Comic Page Viewer"
    public final float bCPV_MaxScale = 4.0f; //Max zoom.
    public final boolean bCPV_AllowZoomJump = true;
    public final float fCPV_ZoomJumpOutThreshold = 100.0f;
    public final float fCPV_ZoomJumpInThreshold = -200.0f;

    //When image is zoomed, options for pan speed:
    public final boolean bCPV_PanAcceleration = true;
    public static final int CPV_PAN_SPEED_SCALED = 1; //Pan based on zoom level.
    public static final int CPV_PAN_SPEED_FIXED = 2;  //Pan based on user-selected speed.
    public final int iCPV_PanSpeedMethod = CPV_PAN_SPEED_SCALED;
    public final float fCPV_VerticalPanScalar = 1.5f;
    public final float fCPV_HorizontalPanScalar = 1.5f;

    //=====================================================================================
    //===== Comic Details Activity Options =====================================================
    //=====================================================================================

    //If comic source is nHentai, these strings enable searching the nHentai web page for tag data:
    //public String snHentai_Default_Comic_Address_Prefix = "https://nhentai.net/g/";
    public final String snHentai_Comic_Address_Prefix = "https://nhentai.net/g/";
    //public String snHentai_Default_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    public final String snHentai_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    //public String snHentai_Default_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";
    public final String snHentai_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";

    //=====================================================================================
    //===== Import Options ================================================================
    //=====================================================================================

    //A flag to turn on/off video file duration analysis:
    public final static boolean bVideoDeepDirectoryContentFileAnalysis = true;
        //This flag allows the program to analyze video duration prior to import to allow
        //  sorting video files by duration in the file listView presented to the user. The user
        //  selects which files to import, and may wish to sort by video duration. If the user
        //  does this without this option turned on, the sort will be inaccurate.
        //  This deep analysis takes longer.


}

