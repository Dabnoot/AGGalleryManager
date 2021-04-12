package com.agcurations.aggallerymanager;



import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

import androidx.annotation.NonNull;

public class GlobalClass extends Application {
    //GlobalClass built using this guide:
    //https://androidexample.com/Global_Variable_Or_Application_Context_Variable_-_Android_Example/index.php?view=article_discription&aid=114&aaid=136

    //Each section, Videos, Pictures, Comics, has its own folder, catalog file, tags file, and log folder.

    //Global Variables:

    public String gsPin = "";

    public File gfAppFolder;

    public static final int MEDIA_CATEGORY_VIDEOS = 0;
    public static final int MEDIA_CATEGORY_IMAGES = 1;
    public static final int MEDIA_CATEGORY_COMICS = 2;

    public int giSelectedCatalogMediaCategory;

    public final File[] gfCatalogFolders = new File[3];
    public final File[] gfCatalogLogsFolders = new File[3];
    public final File[] gfCatalogContentsFiles = new File[3];
    public final File[] gfCatalogTagsFiles = new File[3];
    //Video tag variables:
    public final List<TreeMap<String, ItemClass_Tag>> gtmCatalogTagReferenceLists = new ArrayList<>();
    public final List<TreeMap<String, ItemClass_CatalogItem>> gtmCatalogLists = new ArrayList<>();
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
    public boolean gbCatalogViewerRefresh = false; //Used when data is edited.
    //End catalog viewer variables.

    public static final String gsNHComicCoverPageFilter = "^\\d{1,7}_Cover.+"; //A regex filter for getting the cover file for a NHComicDownloader file set.
    //public static final String gsNHComicPageFilter = "^\\d{1,7}_Page.+"; //A regex filter for getting the cover file for a NHComicDownloader file set.

    public static final String[]  gsRestrictedTagsPreferenceNames = new String[]{
            "multi_select_list_videos_restricted_tags",
            "multi_select_list_images_restricted_tags",
            "multi_select_list_comics_restricted_tags"};

    public static final String gsPinPreference = "preferences_pin";

    public static final String gsUnsortedFolderName = "etc";  //Used when imports are placed in a folder based on their assigned tags.


    ArrayList<ItemClass_File> galImportFileList; //Used to pass a large list of files to import to the import service.
    //  This is done because the list of files can exceed the intent extra transaction size limit.

    //=====================================================================================
    //===== Background Service Tracking Variables =========================================
    //=====================================================================================
    //These vars not in a ViewModel as a service can continue to run after an activity is destroyed.

    //Variables to control starting of import folder content analysis:
    // These variables prevent the system/user from starting another folder analysis until an
    // existing folder analysis operation is finished.
    //public boolean gbImportFolderAnalysisStarted = false; This item not needed for this fragment.
    public boolean gbImportFolderAnalysisRunning = false;
    public boolean gbImportFolderAnalysisStop = false;
    public boolean gbImportFolderAnalysisFinished = false;
    public StringBuilder gsbImportFolderAnalysisLog = new StringBuilder();
    public int giImportFolderAnalysisProgressBarPercent = 0;
    public String gsImportFolderAnalysisProgressBarText = "";
    public String gsImportFolderAnalysisSelectedFolder = "";
    //Variables to control starting of import execution:
    // These variables prevent the system/user from starting another import until an existing
    // import operation is finished.
    public boolean gbImportExecutionStarted = false;
    public boolean gbImportExecutionRunning = false;
    public boolean gbImportExecutionFinished = false;
    public StringBuilder gsbImportExecutionLog = new StringBuilder();
    public int giImportExecutionProgressBarPercent = 0;
    public String gsImportExecutionProgressBarText = "";
    //Variables to control starting of comic web address analysis:
    // These variables prevent the system/user from starting another analysis until an existing
    // operation is finished.
    public boolean gbImportComicWebAnalysisStarted = false;
    public boolean gbImportComicWebAnalysisRunning = false;
    public boolean gbImportComicWebAnalysisFinished = false;
    public StringBuilder gsbImportComicWebAnalysisLog = new StringBuilder();
    public ItemClass_CatalogItem gci_ImportComicWebItem;  //To capture a potential import.

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
                                                                   public void onAvailable(@NonNull Network network) {
                                                                       isNetworkConnected = true; // Global Static Variable
                                                                   }
                                                                   @Override
                                                                   public void onLost(@NonNull Network network) {
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

    static final String gsDatePatternNumSort = "yyyyMMdd.HHmmss";
    static DateTimeFormatter gdtfDateFormatter;

    public static Double GetTimeStampFloat(){
        //Get an easily-comparable time stamp.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternNumSort);
        String sTimeStamp = gdtfDateFormatter.format(LocalDateTime.now());
        return Double.parseDouble(sTimeStamp);
    }

    /*static final String gsDatePatternFileSafe = "yyyyMMdd_HHmmss";
    static final String gsDatePatternReadReady = "yyyy-MM-dd HH:mm:ss";
    public String GetTimeStampReadReady(){
        //Get an easily readable time stamp.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternReadReady);
        return gdtfDateFormatter.format(LocalDateTime.now());
    }*/

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
    public final int giCatalogFileVersion = 5;
    public String getCatalogHeader(){
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

        sHeader = sHeader + "\t" + "Grade";                          //Grade of the item, set by the user

        sHeader = sHeader + "\t" + "Version:" + giCatalogFileVersion;

        return sHeader;
    }

    public String getCatalogRecordSearchString(ItemClass_CatalogItem ci){

        String sReadableData = ""; //To be used for textual searches
        sReadableData = sReadableData + ci.iMediaCategory;                         //Video, image, or comic.
        sReadableData = sReadableData + "\t" + ci.sItemID;                         //Video, image, comic id
        sReadableData = sReadableData + "\t" + JumbleFileName(ci.sFilename);       //Video or image filename. Filename used by storage is obfuscated. De-jumble to make readable.
        sReadableData = sReadableData + "\t" + ci.sFolder_Name;                    //Name of the folder holding the video, image, or comic pages
        sReadableData = sReadableData + "\t" + JumbleFileName(ci.sThumbnail_File); //Name of the file used as the thumbnail for a video or comic
        sReadableData = sReadableData + "\t" + ci.dDatetime_Import;                //Date of import. Used for sorting if desired
        sReadableData = sReadableData + "\t" + ci.dDatetime_Last_Viewed_by_User;   //Date of last read by user. Used for sorting if desired

        String sTags = getTagTextsFromIDs(getIntegerArrayFromString(ci.sTags, ","), ci.iMediaCategory).toString();
        sReadableData = sReadableData + "\t" + sTags;                              //Tags given to the video, image, or comic

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
        sReadableData = sReadableData + "\t" + ci.sSource;                         //Website, if relevant. ended for comics.
        sReadableData = sReadableData + "\t" + ci.iGrade;                          //Grade.

        return sReadableData;
    }

    public String getCatalogRecordString(ItemClass_CatalogItem ci){

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
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sSource);                         //Website, if relevant. ended for comics.
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iGrade);                          //Grade.

        return sRecord;
    }
    
    public static ItemClass_CatalogItem ConvertStringToCatalogItem(String[] sRecord){
        //Designed for interpretting a line as read from a catalog file.
        ItemClass_CatalogItem ci =  new ItemClass_CatalogItem();
        ci.iMediaCategory = Integer.parseInt(sRecord[0]);                   //Video, image, or comic.
        ci.sItemID = JumbleStorageText(sRecord[1]);                         //Video, image, comic id
        ci.sFilename = sRecord[2];                                          //Video or image filename
        ci.sFolder_Name = JumbleStorageText(sRecord[3]);                    //Name of the folder holding the video, image, or comic pages
        ci.sThumbnail_File = sRecord[4];                                    //Name of the file used as the thumbnail for a video or comic
        ci.dDatetime_Import = Double.parseDouble(JumbleStorageText(sRecord[5]));                //Date of import. Used for sorting if desired
        ci.dDatetime_Last_Viewed_by_User = Double.parseDouble(JumbleStorageText(sRecord[6]));   //Date of last read by user. Used for sorting if desired
        ci.sTags = JumbleStorageText(sRecord[7]);                           //Tags given to the video, image, or comic
        ci.iHeight = Integer.parseInt(JumbleStorageText(sRecord[8]));               //Video or image dimension/resolution
        ci.iWidth = Integer.parseInt(JumbleStorageText(sRecord[9]));                //Video or image dimension/resolution
        ci.lDuration_Milliseconds = Long.parseLong(JumbleStorageText(sRecord[10])); //Duration of video in milliseconds
        ci.sDuration_Text = JumbleStorageText(sRecord[11]);                 //Duration of video text in 00:00:00 format
        ci.sResolution = JumbleStorageText(sRecord[12]);                    //Resolution for sorting at user request
        ci.lSize = Long.parseLong(JumbleStorageText(sRecord[13]));          //Size of video, image, or size of all files in the comic, in Bytes
        ci.sCast = JumbleStorageText(sRecord[14]);                          //For videos and images

        //Comic-related variables:
        ci.sComicArtists = JumbleStorageText(sRecord[15]);                  //Common comic tag category
        ci.sComicCategories = JumbleStorageText(sRecord[16]);               //Common comic tag category
        ci.sComicCharacters = JumbleStorageText(sRecord[17]);               //Common comic tag category
        ci.sComicGroups = JumbleStorageText(sRecord[18]);                   //Common comic tag category
        ci.sComicLanguages = JumbleStorageText(sRecord[19]);                //Language(s = sRecord[0] found in the comic
        ci.sComicParodies = JumbleStorageText(sRecord[20]);                 //Common comic tag category
        ci.sComicName = JumbleStorageText(sRecord[21]);                     //Comic name
        ci.iComicPages = Integer.parseInt(JumbleStorageText(sRecord[22]));  //Total number of pages as defined at the comic source
        ci.iComic_Max_Page_ID = Integer.parseInt(JumbleStorageText(sRecord[23]));   //Max comic page id extracted from file names
        ci.sComic_Missing_Pages = JumbleStorageText(sRecord[24]);           //Missing page numbers
        ci.iComic_File_Count = Integer.parseInt(JumbleStorageText(sRecord[25]));    //Files included with the comic. Can be used for egrity check.
        ci.bComic_Online_Data_Acquired = Boolean.parseBoolean(JumbleStorageText(sRecord[26]));  //Typically used to gather tag data from an online comic source, if automatic.
        ci.sSource = JumbleStorageText(sRecord[27]);                        //Website, if relevant. ended for comics.
        if(sRecord.length == 29) { //String.split will not give the last item if it is an empty string.
            ci.iGrade = Integer.parseInt(JumbleStorageText(sRecord[28]));   //Grade, supplied by user.
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

            String sLine = getCatalogRecordString(ci);
            
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

            String sLine = brReader.readLine();
            ItemClass_CatalogItem ciFromFile;
            while (sLine != null) {
                ciFromFile = ConvertStringToCatalogItem(sLine);

                //Check to see if this record is the one that we want to update:
                if (ciFromFile.sItemID.equals(ci.sItemID)) {
                    sLine = getCatalogRecordString(ci);

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

    public void CatalogDataFile_AddNewField(){
        //Update getCatalogRecordString before calling this routine.
        //Update ConvertStringToCatalogItem after calling this routine.
        for(int i = 0; i < 3; i++){
            StringBuilder sbBuffer = new StringBuilder();
            boolean bHeaderWritten = false;
            for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: gtmCatalogLists.get(i).entrySet()){

                if(!bHeaderWritten) {
                    sbBuffer.append(getCatalogHeader()); //Append the header.
                    sbBuffer.append("\n");
                    bHeaderWritten = true;
                }

                sbBuffer.append(getCatalogRecordString(tmEntry.getValue())); //Append the data.
                sbBuffer.append("\n");
            }

            try {
                //Write the catalog file:
                FileWriter fwNewCatalogContentsFile = new FileWriter(gfCatalogContentsFiles[i], false);
                fwNewCatalogContentsFile.write(sbBuffer.toString());
                fwNewCatalogContentsFile.flush();
                fwNewCatalogContentsFile.close();

            } catch (Exception e) {
                Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }


    public ItemClass_CatalogItem analyzeComicReportMissingPages(ItemClass_CatalogItem ci){

        String sFolderName = ci.sFolder_Name;
        //Log.d("Comics", sFolderName);
        String sFolderPath = gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getPath() + File.separator + sFolderName;
        File fFolder = new File(sFolderPath);

        String sMessage;

        if(fFolder.exists()) {

            File[] fComicPages = fFolder.listFiles();

            if(fComicPages != null) {
                if (fComicPages.length == 0) {
                    sMessage = "Comic source \"" + ci.sSource + "\" folder exists, but is missing files.";
                    Log.d("Comics", sMessage);
                }
            }

            TreeMap<String, String> tmSortedFileNames = new TreeMap<>();

            for (File fComicPage : fComicPages) {
                String sFileName = GlobalClass.JumbleFileName(fComicPage.getName());
                tmSortedFileNames.put(sFileName, sFileName);
            }
            ArrayList<Integer> aliComicPageNumbers = new ArrayList<>();
            for (Map.Entry<String, String> tmEntry : tmSortedFileNames.entrySet()) {
                String sFileName = tmEntry.getKey();
                String sPageID = sFileName.substring(sFileName.lastIndexOf("_") + 1, sFileName.lastIndexOf("."));
                aliComicPageNumbers.add(Integer.parseInt(sPageID));
            }
            ArrayList<Integer> aliMissingPages = new ArrayList<>();
            int iExpectedPageID = 0;
            for (Integer iPageID : aliComicPageNumbers) {
                iExpectedPageID++;
                while (iPageID > iExpectedPageID) {
                    aliMissingPages.add(iExpectedPageID);
                    iExpectedPageID++;
                }
            }
            int iMaxPageID = iExpectedPageID;
            if(iExpectedPageID < ci.iComicPages){
                iExpectedPageID++;
                for(int i = iExpectedPageID; i <= ci.iComicPages; i++){
                    aliMissingPages.add(i);
                    iMaxPageID = i;
                }

            }
            ci.iComic_Max_Page_ID = iMaxPageID;
            ci.iComic_File_Count = aliComicPageNumbers.size();

            if(aliMissingPages.size() > 0) {
                String sMissingPages = GlobalClass.formDelimitedString(aliMissingPages, ",");
                ci.sComic_Missing_Pages = sMissingPages;
                sMessage = "Comic source \"" + ci.sSource + "\" missing page numbers: " + sMissingPages + ".";
                Log.d("Comics", sMessage);
            } else {
                ci.sComic_Missing_Pages = "";
            }

        } else {
            sMessage = "Comic source \"" + ci.sSource + "\" missing comic folder.";
            Log.d("Comics", sMessage);
        }
        return ci;
    }


    //=====================================================================================
    //===== Tag Subroutines Section ===================================================
    //=====================================================================================
    public static final int giTagFileVersion = 1;
    public static String getTagFileHeader(){
        return "TagID" +
                "\t" + "TagText" +
                "\t" + "TagDescription" +
                "\t" + "Version:" + giTagFileVersion;
    }

    public static ItemClass_Tag ConvertFileLineToTagItem(String[] sRecord){
        //Designed for interpretting a line as read from a tags file.
        ItemClass_Tag ict;
        if(sRecord.length >= 2) {
            ict = new ItemClass_Tag(Integer.parseInt(JumbleStorageText(sRecord[0])), JumbleStorageText(sRecord[1]));
        } else {
            ict = new ItemClass_Tag(Integer.parseInt(JumbleStorageText(sRecord[0])), "");
        }
        if(sRecord.length >2) {
            ict.sTagDescription = JumbleStorageText(sRecord[2]);
        }
        return ict;
    }

    public static ItemClass_Tag ConvertFileLineToTagItem(String sRecord){
        String[] sRecord2 =  sRecord.split("\t");
        return ConvertFileLineToTagItem(sRecord2);
    }

    public TreeMap<String, ItemClass_Tag> InitTagData(int iMediaCategory){
        TreeMap<String, ItemClass_Tag> tmTags = new TreeMap<>();

        File fTagsFile = gfCatalogTagsFiles[iMediaCategory];

        if(fTagsFile.exists()) {
            //Get Tags from file:
            BufferedReader brReader;
            try {

                brReader = new BufferedReader(new FileReader(fTagsFile.getAbsolutePath()));
                brReader.readLine();//First line is the header, skip it.
                String sLine = brReader.readLine();

                while(sLine != null) {

                    ItemClass_Tag ict = ConvertFileLineToTagItem(sLine);
                    tmTags.put(ict.sTagText, ict);

                    sLine = brReader.readLine();
                }

                brReader.close();

            } catch (IOException e) {
                Toast.makeText(this, "Trouble reading tags file at\n" + fTagsFile.getAbsolutePath() + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else { //If the tags file does not exist, create it:
            try {
                if(fTagsFile.createNewFile()) {
                    try {
                        FileWriter fwTagsFile = new FileWriter(fTagsFile, false);

                        //Write the header record:
                        fwTagsFile.write(getTagFileHeader());
                        fwTagsFile.write("\n");

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

    public ItemClass_Tag TagDataFile_CreateNewRecord(String sTagName, int iMediaCategory){

        File fTagsFile = gfCatalogTagsFiles[iMediaCategory];
        int iNextRecordId = -1;
        ItemClass_Tag ictNewTag = null;
        try {

            int iThisId;
            if(gtmCatalogTagReferenceLists.get(iMediaCategory).size() > 0) {
                for (Map.Entry<String, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                    iThisId = entry.getValue().iTagID;
                    if (iThisId >= iNextRecordId) iNextRecordId = iThisId + 1;
                    if (entry.getValue().sTagText.toLowerCase().equals(sTagName.toLowerCase())) {
                        //If the tag already exists, abort adding a new tag.
                        return null;
                    }
                }
            } else {
                iNextRecordId = 0;
            }
            //New record ID identified.

            ictNewTag = new ItemClass_Tag(iNextRecordId, sTagName);
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
        return ictNewTag;

    }

    public boolean TagDataFile_UpdateRecord(String sTagID, String sData, int iMediaCategory) {

        ItemClass_Tag ictIncoming = new ItemClass_Tag(Integer.parseInt(sTagID), sData);

        File fTagsFile = gfCatalogTagsFiles[iMediaCategory];

        try {

            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fTagsFile.getAbsolutePath()));
            sbBuffer.append(brReader.readLine());  //todo: Append header.
            sbBuffer.append("\n");

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {

                ItemClass_Tag ictFromFile = ConvertFileLineToTagItem(sLine);

                //Check to see if this record is the one that we want to update:
                if (ictFromFile.iTagID.equals(ictIncoming.iTagID)) {

                    sLine = getTagRecordString(ictIncoming);

                    //Now update the record in the treeMap:
                    gtmCatalogTagReferenceLists.get(iMediaCategory).remove(ictFromFile.sTagText);
                    gtmCatalogTagReferenceLists.get(iMediaCategory).put(ictIncoming.sTagText, ictIncoming);

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
        //For writing tags to a tags file.
        String sTagRecord =
                JumbleStorageText(ict.iTagID.toString()) + "\t" +
                JumbleStorageText(ict.sTagText) + "\t" +
                JumbleStorageText(ict.sTagDescription);
        return sTagRecord;
    }

    public String getTagTextsFromTagIDsString(String sTagIDs, int iMediaCategory){
        String sTagTexts = "";
        if(!sTagIDs.equals("")) {
            String[] sTagIDArray = sTagIDs.split(",");
            StringBuilder sbTags = new StringBuilder();
            for (String sTagID : sTagIDArray) {
                sbTags.append(getTagTextFromID(Integer.parseInt(sTagID), iMediaCategory));
                sbTags.append(", ");
            }
            sTagTexts = sbTags.toString();
            if (sTagTexts.contains(",")) {
                sTagTexts = sTagTexts.substring(0, sTagTexts.lastIndexOf(", "));
            }
        }
        return sTagTexts;
    }

    public TreeMap<String, ItemClass_Tag> GetCatalogTagsInUse(Integer iMediaCategory){

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
                ItemClass_Tag ictTemp = gtmCatalogTagReferenceLists.get(iMediaCategory).get(ict.sTagText);
                if (ictTemp != null) {
                    ict.bIsRestricted = ictTemp.bIsRestricted;
                }
                tmTagsInUse.put(sTagText, ict);
            }
        }

        return tmTagsInUse;
    }

    public String getTagTextFromID(Integer iTagID, Integer iMediaCategory){
        String sTagText = "[Tag ID " + iTagID + " not found]";

        //todo: instead of looping through items, use TreeMap.getValue or TreeMap.getKey if it exists.
        for(Map.Entry<String, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            Integer iRefTag = entry.getValue().iTagID;
            if(iRefTag.equals(iTagID)){
                sTagText = entry.getValue().sTagText;
                break;
            }
        }


        return sTagText;
    }

    public boolean TagIDExists(Integer iTagID, int iMediaCategory){

        for(Map.Entry<String, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            Integer iRefTag = entry.getValue().iTagID;
            if(iRefTag.equals(iTagID)){
                return true;
            }
        }

        return false;
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
            if(entry.getValue().sTagText.equalsIgnoreCase(sTagText)){
                iKey = entry.getValue().iTagID;
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
    //===== Import Options ================================================================
    //=====================================================================================

    //A flag to turn on/off video file duration analysis:
    public final static boolean bVideoDeepDirectoryContentFileAnalysis = true;
        //This flag allows the program to analyze video duration prior to import to allow
        //  sorting video files by duration in the file listView presented to the user. The user
        //  selects which files to import, and may wish to sort by video duration. If the user
        //  does this without this option turned on, the sort will be inaccurate.
        //  This deep analysis takes longer.

    public final static int iComicFolderImportMaxPageSkip = 2;
        //The program attempts to recognize page numbers in filenames during comic import via folder.
        //  The program has to recognize the page numbering pattern. This factor allows some leniency
        //  if the user is missing pages from their comic folder - perhaps a first page, or a middle
        //  page. There is no way to determine if end pages are missing. This feature is rather inert
        // and only gives a message to the user to indicate that pages might be missing.

    //nHentai comic import web html search strings (may change if the website changes)
    //If comic source is nHentai, these strings enable searching the nHentai web page for tag data:
    //public String snHentai_Default_Comic_Address_Prefix = "https://nhentai.net/g/";
    //public final String snHentai_Comic_Address_Prefix = "https://nhentai.net/g/";
    //public String snHentai_Default_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    public final String snHentai_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    //public String snHentai_Default_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";
    public final String snHentai_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";
    public final String snHentai_Comic_Cover_Thumb_xPE = "//div[@id='bigcontainer']//img[@class='lazyload']";
    public final String snHentai_Comic_Page_Thumbs_xPE = "//div[@class='thumb-container']//img[@class='lazyload']";

    //Video import web html search strings (may change if the website changes)
    //Create an array of keys that allow program to locate video links:
    String VIDEO_DOWNLOAD_TITLE = "VIDEO_DOWNLOAD_TITLE";
    String VIDEO_DOWNLOAD_LINK = "VIDEO_DOWNLOAD_LINK";
    String VIDEO_DOWNLOAD_M3U8 = "VIDEO_DOWNLOAD_M3U8";
    String[][] sHTML_VideoKeys = new String[][]{
            {"www.xnxx.com", VIDEO_DOWNLOAD_TITLE, "html5player.setVideoTitle('", "');"},
            {"www.xnxx.com", VIDEO_DOWNLOAD_LINK, "html5player.setVideoUrlLow('", "');"},
            {"www.xnxx.com", VIDEO_DOWNLOAD_LINK, "html5player.setVideoUrlHigh('", "');"},
            {"www.xnxx.com", VIDEO_DOWNLOAD_M3U8, "html5player.setVideoHLS('", "');"}
    };

    ArrayList<ItemClass_VideoDownloadSearchKey> galVideoDownloadSearchKeys = new ArrayList<ItemClass_VideoDownloadSearchKey>() {{

        add(new ItemClass_VideoDownloadSearchKey("www.xnxx.com",ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TITLE, "html5player.setVideoTitle('","');"));
        add(new ItemClass_VideoDownloadSearchKey("www.xnxx.com",ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_LINK, "html5player.setVideoUrlLow('","');"));
        add(new ItemClass_VideoDownloadSearchKey("www.xnxx.com",ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_LINK, "html5player.setVideoUrlHigh('","');"));
        add(new ItemClass_VideoDownloadSearchKey("www.xnxx.com",ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8, "html5player.setVideoHLS('","');"));
    }};


}

