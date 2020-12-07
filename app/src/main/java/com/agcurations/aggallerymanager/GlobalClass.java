package com.agcurations.aggallerymanager;



import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    public File gfAppConfigFile;

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
    public final List<TreeMap<Integer, String[]>> gtmCatalogTagReferenceLists = new ArrayList<>();
    public final List<TreeMap<Integer, String>> gtmCatalogTagsRestricted = new ArrayList<>(); //Key: TagID, Value: TagName
    public final List<TreeMap<Integer, String[]>> gtmCatalogLists = new ArrayList<>();
    public final boolean[] gbJustImported = {false, false, false};
    public static final String[] gsCatalogFolderNames = {"Videos", "Images", "Comics"};

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

    public static String ConvertDoubleTimeStampToString(String sTimeStamp){
        double dTimeStamp = Double.parseDouble(sTimeStamp);
        int iYear = (int) (dTimeStamp / 10000);
        int iMonth = (int) ((dTimeStamp - iYear * 10000) / 100 );
        int iDay = (int) ((dTimeStamp - iYear * 10000 - iMonth * 100));
        int iHour = (int) ((dTimeStamp - iYear * 10000 - iMonth * 100 - iDay) * 100);
        int iMinute = (int) ((((dTimeStamp - iYear * 10000 - iMonth * 100 - iDay) * 100) - iHour) * 100);
        int iSecond = (int) ((((((dTimeStamp - iYear * 10000 - iMonth * 100 - iDay) * 100) - iHour) * 100) - iMinute) * 100);

        return iYear + "-" +
                String.format("%02d", iMonth) + "-" +
                String.format("%02d", iDay) + " " +
                String.format("%02d", iHour) + ":" +
                String.format("%02d", iMinute) + ":" +
                String.format("%02d", iSecond);
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

    static final int[] iNoJumbleFileNameIndex =
            {GlobalClass.VIDEO_FILENAME_INDEX, GlobalClass.IMAGE_FILENAME_INDEX, -1};
    //Filenames that may contain descriptive information are jumbled at import, and
    //  the catalog file should maintain that jumbled name, since the file is ascii.


    //=====================================================================================
    //===== Catalog Subroutines Section ===================================================
    //=====================================================================================

    public void CatalogDataFile_CreateNewRecord(
            int iRecordID,
            String[] sFieldData,
            int iMediaCategory){

        File fCatalogContentsFile = gfCatalogContentsFiles[iMediaCategory];
        TreeMap<Integer, String[]> tmCatalogRecords = gtmCatalogLists.get(iMediaCategory);

        try {
            //Add the details to the TreeMap:
            tmCatalogRecords.put(iRecordID, sFieldData);

            //Add the new record to the catalog file:
            StringBuilder sbLine = new StringBuilder();
            sbLine.append(JumbleStorageText(sFieldData[0]));
            for(int i = 1; i < sFieldData.length; i++){
                sbLine.append("\t");
                if(i == iNoJumbleFileNameIndex[iMediaCategory]){
                    //Don't jumble the filename, as it was jumbled on import. The catalog file is
                    //  ascii, so any descriptive information in the filename would then be readable.
                    sbLine.append(sFieldData[i]);
                } else {
                    sbLine.append(JumbleStorageText(sFieldData[i]));
                }
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
            String sNewHeaderLine;
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
                int j = 0; //To track requested field updates.
                sFields = sLine.split("\t",-1);

                StringBuilder sb = new StringBuilder();
                sb.append(sFields[0]);

                for (int i = 1; i < sFields.length; i++) {
                    sb.append("\t");
                    if (i == COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX) {
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
            String sNewHeaderLine;
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

    public ArrayList<String> GetTagsInUse(Integer iMediaCategory){

        int[] iTagsField = {VIDEO_TAGS_INDEX, IMAGE_TAGS_INDEX, COMIC_TAGS_INDEX};

        ArrayList<String> alsTagsInUse = new ArrayList<>();

        SortedSet<String> ssTemp = new TreeSet<>();
        for(Map.Entry<Integer, String[]>
                CatalogEntry : gtmCatalogLists.get(iMediaCategory).entrySet()) {
            //Sort the strings:
            String[] sTempArray = CatalogEntry.getValue()[iTagsField[iMediaCategory]].split(",");
            for (String s : sTempArray) {
                ssTemp.add(s.trim()); //This will not allow the addition of duplicate tags.
            }
        }

        //Format the strings:
        StringBuilder sb = new StringBuilder();
        Iterator<String> isIterator = ssTemp.iterator();
        sb.append(isIterator.next());
        while(isIterator.hasNext()){
            alsTagsInUse.add(isIterator.next());
        }

        return alsTagsInUse;
    }

    public String getTagTextFromID(Integer iTagID, Integer iMediaCategory){
        String sTagText = "[Tag ID " + iTagID + " not found]";

        String[] sTagData = gtmCatalogTagReferenceLists.get(iMediaCategory).get(iTagID);
        if (sTagData != null) {
            sTagText = sTagData[TAG_NAME_INDEX];
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
        Integer iKey = -1;
        String[] sFields;
        for(Map.Entry<Integer, String[]> entry: gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            sFields = entry.getValue();
            if(sFields[TAG_NAME_INDEX].equals(sTagText)){
                iKey = entry.getKey();
                break;
            }
        }
        return iKey;
    }

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
            String[] sa = sTags.split(sDelimiter);
            for(String s : sa){
                ali.add(Integer.parseInt(s));
            }
        }
        return ali;
    }


    public static String CleanStorageSize(Long lStorageSize){
        //Returns a string of size to 2 significant figures plus units of B, KB, MB, or GB.

        String sSizeSuffix = " B";
        if(lStorageSize > 1000) {
            lStorageSize /= 1024;
            sSizeSuffix = " KB";
        }
        if(lStorageSize > 1000) {
            lStorageSize /= 1024;
            sSizeSuffix = " MB";
        }
        if(lStorageSize > 1000) {
            lStorageSize /= 1024;
            sSizeSuffix = " GB";
        }

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);

        return decimalFormat.format(lStorageSize) + sSizeSuffix;
    }

    public static String getDurationTextFromMilliseconds(long lMilliseconds){
        String sDurationText;
        sDurationText = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(lMilliseconds),
                TimeUnit.MILLISECONDS.toMinutes(lMilliseconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(lMilliseconds)),
                TimeUnit.MILLISECONDS.toSeconds(lMilliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(lMilliseconds)));
        return sDurationText;
    }


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
                    if(sChildFiles != null) {
                        for (String sChildFile : sChildFiles) {
                            new File(fComicFolderToBeDeleted, sChildFile).delete();
                        }
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



}

