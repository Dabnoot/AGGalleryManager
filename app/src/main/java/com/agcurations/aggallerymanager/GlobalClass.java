package com.agcurations.aggallerymanager;



import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.Format;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GlobalClass extends Application {
    //GlobalClass built using this guide:
    //https://androidexample.com/Global_Variable_Or_Application_Context_Variable_-_Android_Example/index.php?view=article_discription&aid=114&aaid=136

    //Each section, Videos, Pictures, Comics, has its own folder, catalog file, tags file, and log folder.

    //Global Variables:

    public String gsPin = "";

    public File gfAppFolder;
    public File gfAppConfigFile;

    public static final int MEDIA_CATEGORY_VIDEOS = 0;
    public static final int MEDIA_CATEGORY_IMAGES = 1;
    public static final int MEDIA_CATEGORY_COMICS = 2;

    public File[] gfCatalogFolders = new File[3];
    public File[] gfCatalogLogsFolders = new File[3];
    public File[] gfCatalogContentsFiles = new File[3];
    public File[] gfCatalogTagsFiles = new File[3];
    //Video tag variables:
    public List<TreeMap<Integer, String[]>> gtmCatalogTagReferenceLists = new ArrayList<TreeMap<Integer, String[]>>();
    public List<TreeMap<Integer, String>> gtmCatalogTagsRestricted = new ArrayList<TreeMap<Integer, String>>(); //Key: TagID, Value: TagName
    public List<TreeMap<Integer, String[]>> gtmCatalogLists = new ArrayList<TreeMap<Integer, String[]>>();
    public boolean[] gbJustImported = {false, false, false};
    public String[] gsCatalogFolderNames = {"Videos", "Images", "Comics"};

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
                    "RESOLUTION",
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





    public File gfVideosFolder;
    public File gfVideoLogsFolder;
    public File gfVideoCatalogContentsFile;
    public File gfVideoTagsFile;
    //Video tag variables:
    public TreeMap<Integer, String[]> gtmVideoTagReferenceList = new TreeMap<>();
    public TreeMap<Integer, String> gtmVideoTagsRestricted = new TreeMap<>(); //Key: TagID, Value: TagName
    public TreeMap<Integer, String[]> gtmCatalogVideoList = new TreeMap<>();
    public boolean gbVideosJustImported = false;

    public File gfImagesFolder;
    public File gfImagesLogsFolder;
    public File gfImagesCatalogContentsFile;
    public File gfImagesTagsFile;
    //Video tag variables:
    public TreeMap<Integer, String[]> gtmImagesTagReferenceList = new TreeMap<>();
    public TreeMap<Integer, String> gtmImagesTagsRestricted = new TreeMap<>(); //Key: TagID, Value: TagName
    public TreeMap<Integer, String[]> gtmCatalogImagesList = new TreeMap<>();
    public boolean gbImagesJustImported = false;


    public File gfComicsFolder;
    public File gfComicLogsFolder;
    public File gfComicCatalogContentsFile;
    public File gfComicTagsFile;
    //Process comic tags in same manner as video and picture tags.
    //  However, they are not interchangeable. The fictional nature of comics could cause problems
    //  if the tags were applied to real videos or pictures.
    public TreeMap<Integer, String[]> gtmComicTagReferenceList = new TreeMap<>();
    public TreeMap<Integer, String> gtmComicTagsRestricted = new TreeMap<>(); //Key: TagID, Value: TagName
    public TreeMap<Integer, String[]> gtmCatalogComicList = new TreeMap<>();
    public int giComicDefaultSortBySetting = COMIC_TAGS_INDEX;
    public boolean gbComicSortAscending = true;
    public boolean gbComicJustImported = false;
    public String[] gsSelectedComic;

    public int giCatalogDefaultSortBySetting = COMIC_DATETIME_IMPORT_INDEX;
    public boolean gbCatalogSortAscending = true;


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
    public boolean bAutoDownloadOn = true; //By default, auto-download details is on.
      //This uses little network resource because we are just getting html data.


    //=====================================================================================
    //===== Utilities =====================================================================
    //=====================================================================================


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

        if (freeBytesExternal >= 1024) {
            //contains at least 1 KB.
            freeBytesExternal /= 1024;
        } else {
            freeBytesExternal = 0;
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

    public static String ConvertDoubleTimeStampToString(String sTimeStamp){
        double dTimeStamp = Double.parseDouble(sTimeStamp);
        int iYear = (int) (dTimeStamp / 10000);
        int iMonth = (int) ((dTimeStamp - iYear * 10000) / 100 );
        int iDay = (int) ((dTimeStamp - iYear * 10000 - iMonth * 100));
        int iHour = (int) ((dTimeStamp - iYear * 10000 - iMonth * 100 - iDay) * 100);
        int iMinute = (int) ((((dTimeStamp - iYear * 10000 - iMonth * 100 - iDay) * 100) - iHour) * 100);
        int iSecond = (int) ((((((dTimeStamp - iYear * 10000 - iMonth * 100 - iDay) * 100) - iHour) * 100) - iMinute) * 100);

        String sTimeStampFinal = iYear + "-" +
                String.format("%02d", iMonth) + "-" +
                String.format("%02d", iDay) + " " +
                String.format("%02d", iHour) + ":" +
                String.format("%02d", iMinute) + ":" +
                String.format("%02d", iSecond);

        return sTimeStampFinal;
    }


    public static String JumbleStorageText(String sSourceText){
        //Render the text unsearchable so that no scanning system can pick up explicit tags.
        String sFinalText;
        StringBuilder sbReverse = new StringBuilder();
        sbReverse.append(sSourceText);
        sFinalText = sbReverse.reverse().toString();

        return sFinalText;
    }

    public String JumbleFileName(String sFileName){
        //Reverse the text on the file so that the file does not get picked off by a search tool:
        StringBuilder sFileNameExtJumble = new StringBuilder();
        sFileNameExtJumble.append(sFileName.substring(sFileName.lastIndexOf(".") + 1));
        StringBuilder sFileNameBody = new StringBuilder();
        sFileNameBody.append(sFileName.substring(0,sFileName.lastIndexOf(".")));
        sFileName = sFileNameBody.reverse().toString() + "." + sFileNameExtJumble.reverse().toString();
        return sFileName;
    }


    //=====================================================================================
    //===== Catalog Subroutines Section ===================================================
    //=====================================================================================

    public void CatalogDataFile_CreateNewRecord(
            File fCatalogContentsFile,
            int iRecordID,
            TreeMap<Integer, String[]> tmCatalogRecords,
            String[] sFieldData){

        try {
            //Add the details to the TreeMap:
            tmCatalogRecords.put(iRecordID, sFieldData);

            //Add the new record to the catalog file:
            StringBuilder sbLine = new StringBuilder();
            sbLine.append(JumbleStorageText(sFieldData[0]));
            for(int i = 1; i < sFieldData.length; i++){
                sbLine.append("\t");
                sbLine.append(JumbleStorageText(sFieldData[i]));
            }
            //Write the data to the file:
            FileWriter fwNewCatalogContentsFile = new FileWriter(fCatalogContentsFile, true);
            fwNewCatalogContentsFile.write(sbLine.toString());
            fwNewCatalogContentsFile.write("\n");
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + fCatalogContentsFile.getPath() + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }


    public void CatalogDataFile_UpdateRecord(
            File fCatalogContentsFile,
            TreeMap<Integer, String[]> tmCatalogRecords,
            String sRecordID,
            int iRecordIDIndex,
            int[] iFieldIDs,
            String[] sFieldUpdateData) {

        //iRecordIDIndex is the index in the record at which the ID can be found.

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
                //De-jumble the data read from the file:
                String[] sFields2 = new String[sFields.length];
                for(int i = 0; i < sFields.length; i++){
                    sFields2[i] = JumbleStorageText(sFields[i]);
                }
                sFields = sFields2;

                //Check to see if this record is the one that we want to update:
                if (sFields[iRecordIDIndex].equals(sRecordID)) {
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
                    for (Map.Entry<Integer, String[]>
                            CatalogEntry : tmCatalogRecords.entrySet()) {
                        String sEntryRecordID = CatalogEntry.getValue()[iRecordIDIndex];
                        if( sEntryRecordID.contains(sFields[iRecordIDIndex])){
                            iKey = CatalogEntry.getKey();
                            break;
                        }
                    }
                    if(iKey >= 0){
                        tmCatalogRecords.put(iKey,sFields);
                    }

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
            FileWriter fwNewCatalogContentsFile = new FileWriter(fCatalogContentsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();
        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + fCatalogContentsFile.getPath() + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public boolean CatalogDataFile_UpdateAllRecords(File fCatalogContentsFile, int[] iFieldIDs, String[] sFieldUpdateData) {
        //This routine used to update a single field across all record in a catalog file.
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

            int iFieldIDToUpdate = COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX;
            //Apply an import timestamp:
            Double dTimeStamp = GetTimeStampFloat();
            String sDateTime = dTimeStamp.toString();


            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                dTimeStamp += 0.000100; //Increment the timestamp by one minute.
                sDateTime = dTimeStamp.toString();
                int j = 0; //To track requested field updates.
                sFields = sLine.split("\t",-1);

                StringBuilder sb = new StringBuilder();
                sb.append(sFields[0]);

                for (int i = 1; i < sFields.length; i++) {
                    sb.append("\t");
                    if (i == iFieldIDToUpdate) {
                        //If this is the field to update...
                        sb.append( JumbleStorageText(sDateTime));
                        j++;
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
            Toast.makeText(MainActivity.getContextOfActivity(), "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    //=====================================================================================
    //===== Videos Variables Section ======================================================
    //=====================================================================================

    public static final int VIDEO_ID_INDEX = 0;                             //Video ID
    public static final int VIDEO_FILENAME_INDEX = 1;
    public static final int VIDEO_SIZE_MB_INDEX = 2;
    public static final int VIDEO_DURATION_MILLISECONDS_INDEX = 3;          //Duration of video in Milliseconds
    public static final int VIDEO_DURATION_TEXT_INDEX = 4;                  //Duration of video text
    public static final int VIDEO_RESOLUTION_INDEX = 5;
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
    public static final int IMAGE_RESOLUTION_INDEX = 3;
    public static final int IMAGE_FOLDER_NAME_INDEX = 4;                    //Name of the folder holding the imGE
    public static final int IMAGE_TAGS_INDEX = 5;                           //Tags given to the Image
    public static final int IMAGE_CAST_INDEX = 6;
    public static final int IMAGE_SOURCE_INDEX = 7;                         //Website, if relevant
    public static final int IMAGE_DATETIME_LAST_VIEWED_BY_USER_INDEX = 8;   //Date of last read by user. Used for sorting if desired
    public static final int IMAGE_DATETIME_IMPORT_INDEX = 9;                //Date of import. Used for sorting if desired

    public static final String[] ImageRecordFields = new String[]{
            "IMAGE_ID",
            "IMAGE_FILENAME",
            "SIZE_KB",
            "RESOLUTION",
            "FOLDER_NAME",
            "TAGS",
            "CAST",
            "SOURCE",
            "DATETIME_LAST_VIEWED_BY_USER",
            "DATETIME_IMPORT"};

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

    public static final String[] ComicRecordFields = new String[]{
            "COMIC_ID",
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
            "ONLINE_DATA_ACQUIRED"};

    public static final String COMIC_ONLINE_DATA_ACQUIRED_NO = "No";
    public static final String COMIC_ONLINE_DATA_ACQUIRED_YES = "Yes";




    //=====================================================================================
    //===== Start Comic Catalog.dat Data Modification Routine(S) ================================
    //=====================================================================================





    public boolean gbComicJustDeleted = false;
    public boolean ComicCatalog_DeleteComic(String sComicID) {

        //Delete the comic record from the CatalogContentsFile:

        try {

            //Attempt to delete the selected comic folder first.
            //If that fails, abort the operation.
            String sComicFolderName = "";

            //Don't transfer the line over.
            //Get a path to the comic's folder for deletion in the next step:
            for (Map.Entry<Integer, String[]>
                    CatalogEntry : gtmCatalogLists.get(MEDIA_CATEGORY_COMICS).entrySet()) {
                String[] sFields = CatalogEntry.getValue();
                if( sFields[GlobalClass.COMIC_ID_INDEX].contains(sComicID)){
                    sComicFolderName = sFields[COMIC_FOLDER_NAME_INDEX];
                    break;
                }
            }

            String  sComicFolderPath = gfCatalogFolders[MEDIA_CATEGORY_COMICS].getPath() + File.separator
                    + sComicFolderName;

            File fComicFolderToBeDeleted = new File(sComicFolderPath);
            if(fComicFolderToBeDeleted.exists() && fComicFolderToBeDeleted.isDirectory()){
                try{
                    //First, the directory must be empty to delete. So delete all files in folder:
                    String[] sChildFiles = fComicFolderToBeDeleted.list();
                    for (int i = 0; i < sChildFiles.length; i++) {
                        new File(fComicFolderToBeDeleted, sChildFiles[i]).delete();
                    }

                    if(!fComicFolderToBeDeleted.delete()){
                        Toast.makeText(this, "Could not delete folder of comic ID " + sComicID + ".",
                                Toast.LENGTH_LONG).show();
                        return false;
                    }
                } catch (Exception e){
                    Toast.makeText(this, "Could not delete folder of comic ID " + sComicID + ".",
                            Toast.LENGTH_LONG).show();
                    return false;
                }
            } else {
                Toast.makeText(this, "Could not find folder of to-be-deleted comic ID " + sComicID + ".",
                        Toast.LENGTH_LONG).show();
                return false;
            }



            //Now attempt to delete the comic record from the CatalogContentsFile:
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(gfCatalogContentsFiles[MEDIA_CATEGORY_COMICS].getAbsolutePath()));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                sFields = sLine.split("\t",-1);
                if (!(JumbleStorageText(sFields[COMIC_ID_INDEX]).equals(sComicID))) {
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
            int iKey = -1;
            for (Map.Entry<Integer, String[]>
                    CatalogEntry : gtmCatalogLists.get(MEDIA_CATEGORY_COMICS).entrySet()) {
                String sEntryComicID = CatalogEntry.getValue()[GlobalClass.COMIC_ID_INDEX];
                if( sEntryComicID.contains(sComicID)){
                    iKey = CatalogEntry.getKey();
                    break;
                }
            }
            if(iKey >= 0){
                gtmCatalogLists.get(MEDIA_CATEGORY_COMICS).remove(iKey);
            }

            gbComicJustDeleted = true;
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    //=====================================================================================
    //===== End Comic Catalog.dat Data Modification Routine(S) ================================
    //=====================================================================================



    //Begin Obfuscation section:

    public boolean ObfuscationOn = false;
    public int iObfuscationIndex;
    //public int OBFUSCATION_SUBJECT_VIDEOGAMES = 0;
    public int OBFUSCATION_SUBJECT_QUALITY = 1;
    public int iObfuscationSubjectSelection = OBFUSCATION_SUBJECT_QUALITY;

    int[][] iImageList = new int[][]{
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
    String[][] sObfuscationCategoryNames = new String[][]{
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
    String[] sObfuscatedProgramNames = new String[]{
            "Top Titles",
            "Quality Operations"
    };
    String[] sNonObfustatedProgramName = new String[]{"Videos Catalog", "Images Catalog", "Comics Catalog"};

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
    public float bCPV_MaxScale = 4.0f; //Max zoom.
    public boolean bCPV_AllowZoomJump = true;
    public float fCPV_ZoomJumpOutThreshold = 100.0f;
    public float fCPV_ZoomJumpInThreshold = -200.0f;

    //When image is zoomed, options for pan speed:
    public boolean bCPV_PanAcceleration = true;
    public static final int CPV_PAN_SPEED_SCALED = 1; //Pan based on zoom level.
    public static final int CPV_PAN_SPEED_FIXED = 2;  //Pan based on user-selected speed.
    public int iCPV_PanSpeedMethod = CPV_PAN_SPEED_SCALED;
    public float fCPV_VerticalPanScalar = 1.5f;
    public float fCPV_HorizontalPanScalar = 1.5f;

    //=====================================================================================
    //===== Comic Details Activity Options =====================================================
    //=====================================================================================

    //If comic source is nHentai, these strings enable searching the nHentai web page for tag data:
    //public String snHentai_Default_Comic_Address_Prefix = "https://nhentai.net/g/";
    public String snHentai_Comic_Address_Prefix = "https://nhentai.net/g/";
    //public String snHentai_Default_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    public String snHentai_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    //public String snHentai_Default_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";
    public String snHentai_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";



}

