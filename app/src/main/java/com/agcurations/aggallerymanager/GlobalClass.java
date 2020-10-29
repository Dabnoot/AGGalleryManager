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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

public class GlobalClass extends Application {
    //GlobalClass built using this guide:
    //https://androidexample.com/Global_Variable_Or_Application_Context_Variable_-_Android_Example/index.php?view=article_discription&aid=114&aaid=136

    //Global Variables:

    public static String gsPin = "";

    public static File gfAppFolder;
    public static File gfAppConfigFile;

    public static File gfVideosFolder;
    public static File gfVideoLogsFolder;
    public static File gfVideoCatalogContentsFile;
    public static File gfVideoTagsFile;
    //Video tag variables:
    public static TreeMap<Integer, String[]> gtmVideoTagReferenceList = new TreeMap<>();
    public static TreeMap<Integer, String> gtmVideoTagsRestricted = new TreeMap<>(); //Key: TagID, Value: TagName
    public static TreeMap<Integer, String[]> gtmCatalogVideoList = new TreeMap<>();

    public static File gfComicsFolder;
    public static File gfComicLogsFolder;
    public static File gfComicCatalogContentsFile;
    public static File gfComicTagsFile;
    //Process comic tags in same manner as video and picture tags.
    //  However, they are not interchangeable. The fictional nature of comics could cause problems
    //  if the tags were applied to real videos or pictures.
    public static TreeMap<Integer, String[]> gtmComicTagReferenceList = new TreeMap<>();
    public static TreeMap<Integer, String> gtmComicTagsRestricted = new TreeMap<>(); //Key: TagID, Value: TagName
    public static TreeMap<Integer, String[]> gtmCatalogComicList = new TreeMap<>();
    public boolean gbComicRestrictionsOn = false;
    public int giComicDefaultSortBySetting = COMIC_TAGS_INDEX;
    public boolean gbComicSortAscending = true;
    public boolean gbComicJustImported = false;
    public String[] gsSelectedComic;

    //Each tags file has the same fields:
    public static final int TAG_ID_INDEX = 0;                    //Tag ID
    public static final int TAG_NAME_INDEX = 1;                  //Tag Name
    public static final int TAG_DESCRIPTION_INDEX = 3;           //Tag Description
    public static final String[] TagRecordFields = new String[]{
            "TAG_ID",
            "TAG_NAME",
            "DESCRIPTION"};





    public void SendToast(Context context, String sMessage){
        Toast.makeText(context, sMessage, Toast.LENGTH_SHORT).show();
    }

    //=====================================================================================
    //===== Network Monitoring ============================================================
    //=====================================================================================
    public static boolean isNetworkConnected = false;
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


    public static String JumbleStorageText(String sSourceText){
        //Render the text unsearchable so that no scanning system can pick up explicit tags.
        String sFinalText;
        StringBuilder sbReverse = new StringBuilder();
        sbReverse.append(sSourceText);
        sFinalText = sbReverse.reverse().toString();

        return sFinalText;
    }

    //=====================================================================================
    //===== Videos Section ===============================================================
    //=====================================================================================

    public static final int VIDEO_ID_INDEX = 0;                             //Video ID
    public static final int VIDEO_FILENAME_INDEX = 1;
    public static final int VIDEO_SIZE_MB_INDEX = 2;
    public static final int VIDEO_DURATION_INDEX = 3;                       //Duration of video
    public static final int VIDEO_RESOLUTION_INDEX = 4;
    public static final int VIDEO_FOLDER_NAME_INDEX = 5;                    //Name of the folder holding the video
    public static final int VIDEO_TAGS_INDEX = 6;                           //Tags given to the video
    public static final int VIDEO_CAST_INDEX = 7;
    public static final int VIDEO_SOURCE_INDEX = 8;                         //Website, if relevant
    public static final int VIDEO_DATETIME_LAST_VIEWED_BY_USER_INDEX = 9;   //Date of last read by user. Used for sorting if desired
    public static final int VIDEO_DATETIME_IMPORT_INDEX = 10;               //Date of import. Used for sorting if desired

    public static final String[] VideoRecordFields = new String[]{
            "VIDEO_ID",
            "VIDEO_FILENAME",
            "SIZE_MB",
            "DURATION",
            "RESOLUTION",
            "FOLDER_NAME",
            "TAGS",
            "CAST",
            "SOURCE",
            "DATETIME_LAST_VIEWED_BY_USER",
            "DATETIME_IMPORT"};


    //=====================================================================================
    //===== Comics Section ================================================================
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
    public static final int COMIC_DATETIME_LAST_READ_BY_USER_INDEX = 17; //Date of last read by user. Used for sorting if desired
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

    public void ComicCatalogDataFile_UpdateRecord(String sComicID, int[] iFieldIDs, String[] sFieldUpdateData) {

        try {
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(gfComicCatalogContentsFile.getAbsolutePath()));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {
                int j = 0; //To track requested field updates.

                sFields = sLine.split("\t",-1);
                //De-jumble the data:
                String[] sFields2 = new String[sFields.length];
                for(int i = 0; i < sFields.length; i++){
                    sFields2[i] = GlobalClass.JumbleStorageText(sFields[i]);
                }
                sFields = sFields2;

                //Check to see if this record is the one that we want to update:
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

                    //Now update the record in the treemap:
                    sFields = sLine.split("\t",-1);
                    int iKey = -1;
                    for (Map.Entry<Integer, String[]>
                            CatalogEntry : gtmCatalogComicList.entrySet()) {
                        String sEntryComicID = CatalogEntry.getValue()[GlobalClass.COMIC_ID_INDEX];
                        if( sEntryComicID.contains(sFields[COMIC_ID_INDEX])){
                            iKey = CatalogEntry.getKey();
                            break;
                        }
                    }
                    if(iKey >= 0){
                        gtmCatalogComicList.put(iKey,sFields);
                    }

                    //Jumble the fields in preparation for writing to file:
                    sFields2 = sLine.split("\t",-1);
                    StringBuilder sbJumble = new StringBuilder();
                    sbJumble.append(GlobalClass.JumbleStorageText(sFields2[0]));
                    for(int i = 1; i < sFields.length; i++){
                        sbJumble.append("\t");
                        sbJumble.append(GlobalClass.JumbleStorageText(sFields2[i]));
                    }
                    sLine = sbJumble.toString();

                }
                //Write the current comic record to the buffer:
                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            //Write the data to the file:
            FileWriter fwNewCatalogContentsFile = new FileWriter(gfComicCatalogContentsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();
        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public boolean ComicCatalogDataFile_UpdateAllRecords(int[] iFieldIDs, String[] sFieldUpdateData) {

        try {
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(gfComicCatalogContentsFile.getAbsolutePath()));
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

            FileWriter fwNewCatalogContentsFile = new FileWriter(gfComicCatalogContentsFile, false);
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
                    CatalogEntry : gtmCatalogComicList.entrySet()) {
                String[] sFields = CatalogEntry.getValue();
                if( sFields[GlobalClass.COMIC_ID_INDEX].contains(sComicID)){
                    sComicFolderName = sFields[COMIC_FOLDER_NAME_INDEX];
                    break;
                }
            }

            String  sComicFolderPath = gfComicsFolder.getPath() + File.separator
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
            brReader = new BufferedReader(new FileReader(gfComicCatalogContentsFile.getAbsolutePath()));
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
            FileWriter fwNewCatalogContentsFile = new FileWriter(gfComicCatalogContentsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();


            //Now update memory to no longer include the comic:
            int iKey = -1;
            for (Map.Entry<Integer, String[]>
                    CatalogEntry : gtmCatalogComicList.entrySet()) {
                String sEntryComicID = CatalogEntry.getValue()[GlobalClass.COMIC_ID_INDEX];
                if( sEntryComicID.contains(sComicID)){
                    iKey = CatalogEntry.getKey();
                    break;
                }
            }
            if(iKey >= 0){
                gtmCatalogComicList.remove(iKey);
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

    //If comic source is nHentai, these strings enable searching the nHentai web page for tag data:
    //public String snHentai_Default_Comic_Address_Prefix = "https://nhentai.net/g/";
    public String snHentai_Comic_Address_Prefix = "https://nhentai.net/g/";
    //public String snHentai_Default_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    public String snHentai_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    //public String snHentai_Default_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";
    public String snHentai_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";




   /* public static final class FileUtil {
   This item commented-out on 2020-10-24.
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
    }*/


}

