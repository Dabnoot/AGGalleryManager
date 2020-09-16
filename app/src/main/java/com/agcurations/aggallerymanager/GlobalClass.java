package com.agcurations.aggallerymanager;



import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import androidx.annotation.Nullable;

public class GlobalClass extends Application {
    //GlobalClass built using this guide:
    //https://androidexample.com/Global_Variable_Or_Application_Context_Variable_-_Android_Example/index.php?view=article_discription&aid=114&aaid=136

    //Global Variables:

    public String gsPin = "";

    public boolean gbSkipComicCatalogReload;

    public static File gvfAppFolder;
    public static File gvfAppConfigFile;
    public static File gvfComicsFolder;
    public static File gvfComicLogsFolder;
    public static File gvfComicCatalogContentsFile;
    public static File gvfComicTagsFile;

    TreeMap<Integer, String[]> gvtmCatalogComicList = new TreeMap<>();

    public boolean gvbComicRestrictionsOn = false;

    public int gviComicDefaultSortBySetting = COMIC_TAGS_INDEX;
    public boolean gvbComicSortAscending = true;

    public boolean gbComicJustImported = false;

    public String[] gvSelectedComic;

    public SortedSet<String> gssAllUniqueCatalogComicTags = new TreeSet<>();
    public Set<String> gssComicTagsRestricted = new TreeSet<>();

    public void SendToast(Context context, String sMessage){
        Toast.makeText(context, sMessage, Toast.LENGTH_SHORT).show();
    }

    public void initFolderAndFileStructure(){

        String sExternalStorageState;
        sExternalStorageState = Environment.getExternalStorageState();
        if (sExternalStorageState.equals(Environment.MEDIA_MOUNTED) ) {

            File[] fAvailableDirs = getExternalFilesDirs(null);
            if (fAvailableDirs.length == 2) {
                gvfAppFolder = fAvailableDirs[1];
            } else {
                gvfAppFolder = fAvailableDirs[0];
            }

            gvfComicsFolder = new File(gvfAppFolder
                    + File.separator + "Comics");

            gvfComicCatalogContentsFile = new File(gvfComicsFolder.getAbsolutePath()
                    + File.separator + "CatalogContents.dat");

            gvfComicLogsFolder = new File(gvfComicsFolder
                    + File.separator + "Logs");

            gvfComicTagsFile = new File(gvfComicsFolder.getAbsolutePath()
                    + File.separator + "ComicTags.dat");

            gvfAppConfigFile = new File(gvfAppFolder.getAbsolutePath()
                    + File.separator + "AppConfig.dat");

        }


    }


    public void readPin(Context context){
        //Attempt to read a pin number set by the user:

        if (!gvfAppConfigFile.exists()){
            try {
                if(!gvfAppConfigFile.createNewFile()) {
                    SendToast(context, "Could not create AppConfig.dat at " + gvfAppConfigFile.getAbsolutePath());
                }
            }catch (IOException e){
                SendToast(context,"Could not create AppConfig.dat at " + gvfAppConfigFile.getAbsolutePath());
            }
        } else {

            //Read the AppConfig data. This file, at the time of design, was only intended to
            //  hold 1 piece of data - a pin/password set by the user to unlock certain settings.
            //  Specifically, settings for restricted tags, and turning the restriction on and off.
            BufferedReader brReader;
            String sLine = "";
            try {
                brReader = new BufferedReader(new FileReader(gvfAppConfigFile.getAbsolutePath()));
                sLine = brReader.readLine();
                brReader.close();
            } catch (IOException e) {
                SendToast(context,"Trouble reading AppConfig.dat at" + gvfAppConfigFile.getAbsolutePath());
            }

            //Set the global variable holding the pin:
            if(sLine == null){
                gsPin = "";
            } else {
                gsPin = sLine;
            }
        }





    }


    //=====================================================================================
    //===== Start Network Monitoring Section ==============================================
    //=====================================================================================
    public static boolean isNetworkConnected = false;
    public ConnectivityManager connectivityManager;
    // Network Check
    public void registerNetworkCallback() {
        try {
            connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();

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
    //===== End Network Monitoring ============================================================
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

    String gsDatePatternFileSafe = "yyyyMMdd_HHmmss";
    String gsDatePatternNumSort = "yyyyMMdd.HHmmss";
    String gsDatePatternReadReady = "yyyy-MM-dd HH:mm:ss";
    DateTimeFormatter gdtfDateFormatter;

    public String GetTimeStampFileSafe(){
        //For putting a timestamp on a file name. Observant of illegal characters.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternFileSafe);
        return gdtfDateFormatter.format(LocalDateTime.now());
    }
    public Double GetTimeStampFloat(){
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
    public static final int COMIC_SOURCE = 16;                     //nHentai.net, other source, etc.
    public static final int COMIC_DATETIME_LAST_READ_BY_USER = 17; //Date of last read by user. Used for sorting if desired
    public static final int COMIC_DATETIME_IMPORT = 18;            //Date of import. Used for sorting if desired
    public static final int COMIC_ONLINE_DATA_ACQUIRED = 19;

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




    //=====================================================================================
    //===== Start Catalog.dat Data Modification Routine(S) ================================
    //=====================================================================================

    public boolean ComicCatalogDataFile_UpdateRecord(String sComicID, int[] iFieldIDs, String[] sFieldUpdateData) {
        File fCatalogContentsFile = gvfComicCatalogContentsFile;

        try {
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(gvfComicCatalogContentsFile.getAbsolutePath()));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                int j = 0; //To track requested field updates.
                sFields = sLine.split("\t",-1);
                if (sFields[COMIC_ID_INDEX].equals(sComicID)) {
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

                    //The comic record should now be updated in the file.
                    //Now update the record in the treemap:
                    sFields = sLine.split("\t",-1);
                    int iKey = -1;
                    for (Map.Entry<Integer, String[]>
                            CatalogEntry : gvtmCatalogComicList.entrySet()) {
                        String sEntryComicID = CatalogEntry.getValue()[GlobalClass.COMIC_ID_INDEX];
                        if( sEntryComicID.contains(sFields[COMIC_ID_INDEX])){
                            iKey = CatalogEntry.getKey();
                            break;
                        }
                    }
                    if(iKey >= 0){
                        gvtmCatalogComicList.put(iKey,sFields);
                    }



                }
                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            FileWriter fwNewCatalogContentsFile = new FileWriter(gvfComicCatalogContentsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public boolean ComicCatalogDataFile_UpdateAllRecords(int[] iFieldIDs, String[] sFieldUpdateData) {

        try {
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(gvfComicCatalogContentsFile.getAbsolutePath()));
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

            FileWriter fwNewCatalogContentsFile = new FileWriter(gvfComicCatalogContentsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }



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
                    CatalogEntry : gvtmCatalogComicList.entrySet()) {
                String[] sFields = CatalogEntry.getValue();
                if( sFields[GlobalClass.COMIC_ID_INDEX].contains(sComicID)){
                    sComicFolderName = sFields[COMIC_FOLDER_NAME_INDEX];
                    break;
                }
            }

            String  sComicFolderPath = gvfComicsFolder.getPath() + File.separator
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
            brReader = new BufferedReader(new FileReader(gvfComicCatalogContentsFile.getAbsolutePath()));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                sFields = sLine.split("\t",-1);
                if (!(sFields[COMIC_ID_INDEX].equals(sComicID))) {
                    //If the line is not the comic we are trying to delete, transfer it over:
                    sbBuffer.append(sLine);
                    sbBuffer.append("\n");

                }


                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            //Re-write the CatalogContentsFile without the deleted comic's data record:
            FileWriter fwNewCatalogContentsFile = new FileWriter(gvfComicCatalogContentsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();


            //Now update memory to no longer include the comic:
            int iKey = -1;
            for (Map.Entry<Integer, String[]>
                    CatalogEntry : gvtmCatalogComicList.entrySet()) {
                String sEntryComicID = CatalogEntry.getValue()[GlobalClass.COMIC_ID_INDEX];
                if( sEntryComicID.contains(sComicID)){
                    iKey = CatalogEntry.getKey();
                    break;
                }
            }
            if(iKey >= 0){
                gvtmCatalogComicList.remove(iKey);
            }

            gbComicJustDeleted = true;
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    //=====================================================================================
    //===== End Catalog.dat Data Modification Routine(S) ================================
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
    String sNonObfustatedProgramName = "Comic Catalog";

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

    public String getNonObfuscatedProgramName(){
        return sNonObfustatedProgramName;
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

    public String snHentai_Default_Comic_Address_Prefix = "https://nhentai.net/g/";
    public String snHentai_Comic_Address_Prefix = "https://nhentai.net/g/";

    public String snHentai_Default_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    public String snHentai_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";

    public String snHentai_Default_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";
    public String snHentai_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";




    public static final class FileUtil {
        static String TAG="TAG";
        private static final String PRIMARY_VOLUME_NAME = "primary";

        @Nullable
        public static String getFullPathFromTreeUri(@Nullable final Uri treeUri, Context con) {
            if (treeUri == null) return null;
            String volumePath = getVolumePath(getVolumeIdFromTreeUri(treeUri),con);
            if (volumePath == null) return File.separator;
            if (volumePath.endsWith(File.separator))
                volumePath = volumePath.substring(0, volumePath.length() - 1);

            String documentPath = getDocumentPathFromTreeUri(treeUri);
            if (documentPath.endsWith(File.separator))
                documentPath = documentPath.substring(0, documentPath.length() - 1);

            if (documentPath.length() > 0) {
                if (documentPath.startsWith(File.separator))
                    return volumePath + documentPath;
                else
                    return volumePath + File.separator + documentPath;
            }
            else return volumePath;
        }


        @SuppressLint("ObsoleteSdkInt")
        private static String getVolumePath(final String volumeId, Context context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null;
            try {
                StorageManager mStorageManager =
                        (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
                Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
                Method getUuid = storageVolumeClazz.getMethod("getUuid");
                Method getPath = storageVolumeClazz.getMethod("getPath");
                Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
                Object result = getVolumeList.invoke(mStorageManager);

                final int length = Array.getLength(result);
                for (int i = 0; i < length; i++) {
                    Object storageVolumeElement = Array.get(result, i);
                    String uuid = (String) getUuid.invoke(storageVolumeElement);
                    Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                    // primary volume?
                    if (primary && PRIMARY_VOLUME_NAME.equals(volumeId))
                        return (String) getPath.invoke(storageVolumeElement);

                    // other volumes?
                    if (uuid != null && uuid.equals(volumeId))
                        return (String) getPath.invoke(storageVolumeElement);
                }
                // not found.
                return null;
            } catch (Exception ex) {
                return null;
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private static String getVolumeIdFromTreeUri(final Uri treeUri) {
            final String docId = DocumentsContract.getTreeDocumentId(treeUri);
            final String[] split = docId.split(":");
            if (split.length > 0) return split[0];
            else return null;
        }


        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private static String getDocumentPathFromTreeUri(final Uri treeUri) {
            final String docId = DocumentsContract.getTreeDocumentId(treeUri);
            final String[] split = docId.split(":");
            if ((split.length >= 2) && (split[1] != null)) return split[1];
            else return File.separator;
        }
    }


}

