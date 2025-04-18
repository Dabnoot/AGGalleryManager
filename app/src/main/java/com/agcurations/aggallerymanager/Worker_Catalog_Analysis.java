package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Worker_Catalog_Analysis extends Worker {

    public static final String TAG_WORKER_CATALOG_VERIFICATION = "com.agcurations.aggallermanager.tag_worker_catalog_verification";

    static final String EXTRA_ANALYSIS_TYPE = "com.agcurations.aggallerymanager.extra.EXTRA_ANALYSIS_TYPE";
    public static final int ANALYSIS_TYPE_MISSING_FILES = 1;
    //public static final int ANALYSIS_TYPE_ORPHANED_FILES = 2;
    public static final int ANALYSIS_TYPE_M3U8 = 3;

    public static final String CATALOG_ANALYSIS_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_ANALYSIS_ACTION_RESPONSE";

    public static final String EXTRA_BOOL_GET_ARRAY_ORPHANED_FILEITEMS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_ARRAY_ORPHANED_FILEITEMS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_ARRAY_ORPHANED_FILEITEMS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_ARRAY_ORPHANED_FILEITEMS_RESPONSE"; //ArrayList of response data
    public static final String EXTRA_BOOL_GET_ARRAY_MISSING_CAT_ITEMS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_ARRAY_MISSING_CAT_ITEMS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_ARRAY_MISSING_CAT_ITEMS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_ARRAY_MISSING_CAT_ITEMS_RESPONSE"; //ArrayList of response data
    public static final String EXTRA_BOOL_GET_ARRAY_M3U8_INTERN_PATH_ITEMS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_ARRAY_M3U8_INTERN_PATH_ITEMS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_ARRAY_M3U8_INTERN_PATH_ITEMS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_ARRAY_M3U8_INTERN_PATH_ITEMS_RESPONSE"; //ArrayList of response data
    public static final String EXTRA_BOOL_GET_ARRAY_M3U8_MISSING_SEG_ITEMS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_ARRAY_M3U8_MISSING_SEG_ITEMS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_ARRAY_M3U8_MISSING_SEG_ITEMS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_ARRAY_M3U8_MISSING_SEG_ITEMS_RESPONSE"; //ArrayList of response data
    public static final String EXTRA_BOOL_CAT_ANALYSIS_NO_ITEMS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_CAT_ANALYSIS_NO_ITEMS_RESPONSE";

    GlobalClass globalClass;

    int giMediaCategory;
    int giAnalysisType;



    TreeMap<String, ItemClass_CatalogItem> gtmCatalogItemsMissing;

    public Worker_Catalog_Analysis(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
        giAnalysisType = getInputData().getInt(EXTRA_ANALYSIS_TYPE, -1);

    }

    @NonNull
    @Override
    public Result doWork() {

        globalClass = (GlobalClass) getApplicationContext();

        String sMessage;
        if(giMediaCategory == -1){
            sMessage = "No catalog specified.";
            SendLogLine(sMessage);
            GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.STOPPED);
            return Result.failure();
        }

        if(GlobalClass.aiCatalogVerificationRunning.get() == GlobalClass.RUNNING){
            //Don't let more than one of these workers run at the same time.
            return Result.success();
        }
        GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.RUNNING);

        int iProgressNumerator;
        int iProgressDenominator;
        int iProgressBarValue;

        String sAnalysisStartDateTime = GlobalClass.GetTimeStampFileSafe();
        BufferedWriter bwLogFile;
        OutputStream osLogFile;

        StringBuilder sbLogLines = new StringBuilder();

        String sLogFileName = sAnalysisStartDateTime + "_" + GlobalClass.gsCatalogFolderNames[giMediaCategory] + "CatalogVerification_.txt";
        Uri uriLogFile;
        try {
            uriLogFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, GlobalClass.gUriLogsFolder, GlobalClass.BASE_TYPE_TEXT, sLogFileName);
        } catch (FileNotFoundException e) {
            GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.STOPPED);
            return Result.failure();
        }
        if(uriLogFile == null){
            GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.STOPPED);
            return Result.failure();
        }

        ArrayList<ItemClass_File> alOrphanedFileList = new ArrayList<>();
        ArrayList<String> alsCatalogItemsMissing = new ArrayList<>();
        ArrayList<String> als_M3U8_CatItems_Missing_Playlist = new ArrayList<>();
        ArrayList<String> als_M3U8_CatItems_Missing_Segments = new ArrayList<>();


        try { //Required for the log file.
            osLogFile = GlobalClass.gcrContentResolver.openOutputStream(uriLogFile, "wt");
            if (osLogFile == null) {
                GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.STOPPED);
                return Result.failure();
            }
            bwLogFile = new BufferedWriter(new OutputStreamWriter(osLogFile));


            //All of the analysis in this routine require a listing of files in the selected media
            //  category. Create that listing here:

            //Start with a listing of all folders in the selected media folder:
            ArrayList<String> alsFolderNamesInUse = GlobalClass.GetDirectorySubfolderNames(GlobalClass.gUriCatalogFolders[giMediaCategory]);



            iProgressNumerator = 0;
            iProgressDenominator = alsFolderNamesInUse.size();

            StopWatch stopWatch = new StopWatch(false);

            if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.size() < 3){
                for(int i = 0; i< 3; i++){
                    GlobalClass.gtmicf_AllFileItemsInMediaFolder.add(new TreeMap<>());
                }
            }

            if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).size() == 0) {
                //Get an array containing all of the file items found in the selected media storage (ie. Videos, Images)
                stopWatch.Reset();
                stopWatch.PostDebugLogAndRestart("Performing indexing.");
                //If the directory index file does not exist, perform new indexing:
                sMessage = GlobalClass.gsCatalogFolderNames[giMediaCategory] + " directory index does not exist in memory. " +
                        "Indexing " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " directory now. User may exit this part of the app and return when complete or to check progress.\n";
                SendLogLine(sMessage);

                for (String sFolderName : alsFolderNamesInUse) {
                    if (GlobalClass.aiCatalogVerificationRunning.get() == GlobalClass.STOP_REQUESTED) {
                        sMessage = "'STOP' command received from user.\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);
                        break;
                    }

                    if (sFolderName.equals(GlobalClass.gsImageDownloadHoldingFolderName)) {
                        continue;
                    }

                    iProgressNumerator++;
                    iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                    globalClass.BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "Indexing folder " + iProgressNumerator + " of " + iProgressDenominator,
                            CATALOG_ANALYSIS_ACTION_RESPONSE);


                    //Assemble a Uri for the folder:
                    String sFolderUri = GlobalClass.gUriCatalogFolders[giMediaCategory] +
                            GlobalClass.gsFileSeparator + sFolderName;
                    Uri uriFolderUri = Uri.parse(sFolderUri);

                    //Get all files in the folder:
                    //todo: If this routine takes too long, consider simplifying the below query and just using strings.
                    ArrayList<ItemClass_File> alicf_FileItemsInFolder = GlobalClass.GetDirectoryFileNamesData(uriFolderUri);

                    //Look for items in this folder that are m3u8 folders (subfolder storage of multiple related files).
                    // All subfolders should exist only to hold a collection of files related to an m3u8 playlist.
                    ArrayList<String> alsM3U8Subfolders = GlobalClass.GetDirectorySubfolderNames(uriFolderUri);
                    if (alsM3U8Subfolders.size() > 0) {
                        int iSubFolderCount = 0;
                        for (String sSubfolderName : alsM3U8Subfolders) {

                            iSubFolderCount++;
                            globalClass.BroadcastProgress(false, "",
                                    true, iProgressBarValue,
                                    true, "Indexing folder " + iProgressNumerator +
                                            " [subfolder " + iSubFolderCount + "/" + alsM3U8Subfolders.size() + "] of " + iProgressDenominator,
                                    CATALOG_ANALYSIS_ACTION_RESPONSE);

                            //For each folder, check to see if it has a .m3u8 file:
                            String sSubfolderUri = GlobalClass.gUriCatalogFolders[giMediaCategory] +
                                    GlobalClass.gsFileSeparator + sFolderName +
                                    GlobalClass.gsFileSeparator + sSubfolderName;
                            Uri uriSubfolderUri = Uri.parse(sSubfolderUri);
                            ItemClass_File icfCollectionFolder = new ItemClass_File(ItemClass_File.TYPE_FOLDER, sSubfolderName);
                            icfCollectionFolder.sUri = sSubfolderUri;
                            icfCollectionFolder.sUriParent = sFolderUri;
                            icfCollectionFolder.sMediaFolderRelativePath = sFolderName;
                            ArrayList<ItemClass_File> alicf_M3U8FolderItems = GlobalClass.GetDirectoryFileNamesData(uriSubfolderUri);
                            if (alicf_M3U8FolderItems.size() > 0) {
                                //Locate a thumbnail file for use in item previews:
                                String sThumbnailFileName = "";
                                for (ItemClass_File icf : alicf_M3U8FolderItems) {
                                    if (icf.sFileOrFolderName.endsWith(".jpg") || icf.sFileOrFolderName.endsWith(".gpj") ||
                                            icf.sFileOrFolderName.endsWith(".png") || icf.sFileOrFolderName.endsWith(".gnp")) {
                                        sThumbnailFileName = icf.sFileOrFolderName;
                                    }
                                }
                                //Loop again through the files and add them to the file item collection. Assign a thumbnail
                                //  file to any M3U8 files found (there should only be one), and apply a thumbnail Uri.
                                //  This assists with any preview of the item that may occur later.
                                String sMediaFolderRelativePath = sFolderName + GlobalClass.gsFileSeparator + sSubfolderName;
                                String sThumbnailFileRelativePath = sMediaFolderRelativePath + GlobalClass.gsFileSeparator + sThumbnailFileName;
                                String sThumbnailUri = GlobalClass.FormChildUri(
                                                GlobalClass.gUriCatalogFolders[giMediaCategory].toString(),
                                                sThumbnailFileRelativePath)
                                        .toString();
                                //Add items from the M3U8 folder to the file item collection:
                                for (ItemClass_File icf : alicf_M3U8FolderItems) {

                                    if (icf.sFileOrFolderName.endsWith("m3u8")) {
                                        icf.sUriThumbnailFile = sThumbnailUri;
                                        icfCollectionFolder.sUriThumbnailFile = sThumbnailUri;
                                    }
                                    icf.iTypeFileFolderURL = ItemClass_File.TYPE_M3U8; //Mark all of the items in this folder to be associated with an m3u8 item. Used to assist with orphan analysis.
                                    icf.sMediaFolderRelativePath = sMediaFolderRelativePath;
                                    alicf_FileItemsInFolder.add(icf);
                                }

                            } //End if the M3U8 folder has items.

                            alicf_FileItemsInFolder.add(icfCollectionFolder);

                        } //End loop through potential M3U8 folders (which would be subfolders to the media subfolders)

                    } //End if there are M3U8 folders in this media subfolder.


                    if (alicf_FileItemsInFolder.size() > 0) {
                        for (ItemClass_File icf : alicf_FileItemsInFolder) {
                            if (icf.sMediaFolderRelativePath.equals("")) { //Ignore items that had a path set due to M3U8 membership.
                                icf.sMediaFolderRelativePath = sFolderName;
                            }
                            String sFileFullRelativePath = icf.sMediaFolderRelativePath + GlobalClass.gsFileSeparator + icf.sFileOrFolderName;
                            GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).put(sFileFullRelativePath, icf);
                        }
                    }

                } // End loop through all subfolders in the selected media directory.

                stopWatch.PostDebugLogAndRestart("Indexing complete.");

            }

            stopWatch.Stop();


            // There is now a treemap of all file items in the selected media directory.
            // The name of the treemap variable is "gtmicf_AllFileItemsInMediaFolder", and it is of type
            // TreeMap<String, ItemClass_File>.
            // The key consists of icf.sMediaFolderRelativePath + GlobalClass.gsFileSeparator + icf.sFileOrFolderName
            // boolean bFileExists = gtmicf_AllFileItemsInMediaFolder[giMediaCategory].containsKey(sFileCheckFullPath);


            gtmCatalogItemsMissing = new TreeMap<>();

            if(giAnalysisType == ANALYSIS_TYPE_MISSING_FILES) {

                iProgressNumerator = 0;
                iProgressDenominator = GlobalClass.gtmCatalogLists.get(giMediaCategory).size();

                for (Map.Entry<String, ItemClass_CatalogItem> entry : GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()) {

                    ItemClass_CatalogItem icci = entry.getValue();

                    iProgressNumerator++;
                    iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                    globalClass.BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "Verifying item " + iProgressNumerator + "/" + iProgressDenominator,
                            CATALOG_ANALYSIS_ACTION_RESPONSE);


                    //Determine if the item exists.
                    // For M3U8 videos, look for the m3u8 file.
                    // For comics, look for the cover page.
                    String sFileCheckFullPath = "";
                    String sItemRelativePathUserFriendly = "";
                    sFileCheckFullPath = icci.sFolderRelativePath + GlobalClass.gsFileSeparator + icci.sFilename;
                    sItemRelativePathUserFriendly = GlobalClass.cleanHTMLCodedCharacters(sFileCheckFullPath);
                    boolean bFileExists;
                    bFileExists = GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).containsKey(sFileCheckFullPath);

                    if (!bFileExists) {
                        //Record the catalog item ID that's missing its data:
                        gtmCatalogItemsMissing.put(entry.getKey(), entry.getValue());

                        int iMissingItemCount = gtmCatalogItemsMissing.size();
                        sMessage = iMissingItemCount + ". Item with ID " + icci.sItemID + " not found. Expected at location: " +
                                GlobalClass.gsCatalogFolderNames[giMediaCategory] + "/" + sItemRelativePathUserFriendly + "\n";

                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);
                        Log.d("AGGalleryManager", sMessage);

                    }

                    if (GlobalClass.aiCatalogVerificationRunning.get() == GlobalClass.STOP_REQUESTED) {
                        sMessage = "'STOP' command received from user.\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);
                        break;
                    }

                }

                sMessage = "\nScanned " + iProgressNumerator + "/" + iProgressDenominator + " items in the " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog.\n";
                if (gtmCatalogItemsMissing.size() == 0) {
                    sMessage = sMessage + "Of the " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog items that were scanned, no missing media identified.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                } else {
                    sMessage = sMessage + " Number of catalog items missing media: " + gtmCatalogItemsMissing.size() + "\n\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                }

                //End search for catalog items with missing files.

                //Display a message to indicate that analysis mode has changed to looking for
                //  orphaned items:

                //Analyze Orphaned Files to see if any are a match.
                sMessage =            "============================================================\n";
                sMessage = sMessage + "================== ANALYZING ORPHANED FILES ================\n\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);


                //Analyze orphaned files:


                if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                        giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                    //Look for orphaned files:

                    //Prepare fast lookup of files identified in the catalog database file (currently in memory):
                    Set<String> ssCatalogRelativeFullPaths = new HashSet<>();
                    for (Map.Entry<String, ItemClass_CatalogItem> entry : GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()) {
                        String sRelativeFullPath = entry.getValue().sFolderRelativePath +
                                GlobalClass.gsFileSeparator + entry.getValue().sFilename;
                        ssCatalogRelativeFullPaths.add(sRelativeFullPath);
                    }



                    //Loop through all of the folders in memory in the media category storage location,
                    // then loop through all of the file items (and m3u8 file items) to see if there
                    // is a match in the catalog:
                    iProgressNumerator = 0;
                    iProgressDenominator = GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).size();

                    int iFileItemCountInThisFolder = 0;
                    int iFileItemCountOrphanedInThisFolder = 0;
                    int iFileItemCountOrphanedInThisFolderButNameMatched = 0;
                    int iFileItemCountOrphanedInThisFolderButNameMatchedJumbled = 0;
                    int iFileItemCountOrphanedMissingMatch = 0;
                    int iFileItemCountOrphanedMissingMatchCounterJumbled = 0;

                    String sFirstPath;
                    String sCurrentFolder = "";

                    if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).size() > 0) {
                        if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).firstEntry() != null) {
                            if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).firstEntry().getValue() != null) {
                                ItemClass_File icf_Temp = GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).firstEntry().getValue();
                                if (icf_Temp != null) {
                                    sFirstPath = icf_Temp.sMediaFolderRelativePath;
                                    if (sFirstPath.contains(GlobalClass.gsFileSeparator)) {
                                        sCurrentFolder = sFirstPath.substring(0, sFirstPath.indexOf(GlobalClass.gsFileSeparator));
                                    } else {
                                        sCurrentFolder = sFirstPath; //Monitor which folder is being processed for better data reporting to the user.
                                    }
                                    sMessage = "\nReviewing folder: " + GlobalClass.gsCatalogFolderNames[giMediaCategory] +
                                            GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + sCurrentFolder) + "...\n";
                                    SendLogLine(sMessage);
                                    sbLogLines.append(sMessage);
                                }
                            }
                        }
                    }

                    String sNextPath;

                    boolean bFolderSummaryCompleted = false;

                    final boolean ORPHANED = true;
                    final boolean ASSIGNED_TO_CATALOG_ITEM = false;
                    TreeMap<String, Boolean> tmKnownSets = new TreeMap<>();    //Fast lookup of known missing sets of items
                                                                               // for M3U8 or Comic. Contains relative path
                                                                               // to pertinant folder representing the set,
                                                                               // and a bool for known missing.
                        //Regarding these "known sets" - we are looking for orphaned files. Sets
                    // of comics and m3u8 playlist files can represent a lot of items. As they are
                    // potentially repaired in a later step, all items need to be present for a
                    // potential move. Therefore, all of these orphaned subitems need to be tracked.
                    // If the item is a member of an orphaned comic or m3u8 playlist, add it to
                    // the appropriate collection.


                    ArrayList<String> alsRecognizedNonJumbledFileExtensions = GlobalClass.getListOfNonJumbledFileExtensions();

                    for(Map.Entry<String, ItemClass_File> entry_ItemInMediaFolder: GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).entrySet()){

                        //Watch for a stop command from the program:
                        if (GlobalClass.aiCatalogVerificationRunning.get() == GlobalClass.STOP_REQUESTED) {
                            sMessage = "'STOP' command received from user.\n";
                            SendLogLine(sMessage);
                            sbLogLines.append(sMessage);
                            break;
                        }

                        //Report progress:
                        iProgressNumerator++;
                        iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                        globalClass.BroadcastProgress(false, "",
                                true, iProgressBarValue,
                                true, "Verifying item " + iProgressNumerator + " of " + iProgressDenominator + " files",
                                CATALOG_ANALYSIS_ACTION_RESPONSE);

                        //Create a local variable to hold the current file item:
                        ItemClass_File icf_FileItem = entry_ItemInMediaFolder.getValue();

                        sNextPath = icf_FileItem.sMediaFolderRelativePath;
                        if(sNextPath.contains(GlobalClass.gsFileSeparator)){
                            sNextPath = sNextPath.substring(0, sNextPath.indexOf(GlobalClass.gsFileSeparator));
                        }

                        if(!sCurrentFolder.equals(sNextPath)){
                            //Print a summary for the last folder that was analyzed.

                            //If there were orphaned files in this folder, display a summary for the folder:
                            PrintFolderSummaryForOrphanedFiles(sbLogLines,
                                    iFileItemCountInThisFolder,
                                    iFileItemCountOrphanedInThisFolder,
                                    iFileItemCountOrphanedInThisFolderButNameMatched,
                                    iFileItemCountOrphanedInThisFolderButNameMatchedJumbled,
                                    iFileItemCountOrphanedMissingMatch,
                                    iFileItemCountOrphanedMissingMatchCounterJumbled,
                                    sCurrentFolder);

                            bFolderSummaryCompleted = true;
                            if(GlobalClass.giCatalog_Analysis_Approx_Max_Results > 0) { //If we are not processing unlimited results...
                                if (alOrphanedFileList.size() > GlobalClass.giCatalog_Analysis_Approx_Max_Results)
                                    break;
                            }
                            sCurrentFolder = sNextPath;
                            iFileItemCountInThisFolder = 0;
                            iFileItemCountOrphanedInThisFolder = 0;
                            iFileItemCountOrphanedInThisFolderButNameMatched = 0;
                            iFileItemCountOrphanedInThisFolderButNameMatchedJumbled = 0;

                            sMessage = "\nReviewing folder: " + GlobalClass.gsCatalogFolderNames[giMediaCategory] +
                                    GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + sCurrentFolder) + "...\n";
                            SendLogLine(sMessage);
                            sbLogLines.append(sMessage);

                            bFolderSummaryCompleted = false;
                        }


                        //Check to see if this file is orphaned:
                        if(!ssCatalogRelativeFullPaths.contains(entry_ItemInMediaFolder.getKey()) &&
                                icf_FileItem.iTypeFileFolderURL != ItemClass_File.TYPE_FOLDER) { //Process Diagram Node: 0500
                            //Don't test folders here, as ssCatalogRelativeFullPaths does not contain folder listings.
                            //  ssCatalogRelativeFullPaths only contains file names.


                            //Check to see if this file is part of a set of files, such as a comic
                            // or an M3U8 playlist. These supporting files will not be listed in the
                            // catalog, and thus we will be here even if the catalog item that the
                            // file supports is listed in the catalog.

                            if (icf_FileItem.iTypeFileFolderURL == ItemClass_File.TYPE_M3U8) {
                                //This file is associated with an m3u8 playlist.
                                //Locate the m3u8 file and determine if it is orphaned.
                                // If it is not, then skip the rest of the loop via continue.

                                if (!icf_FileItem.sFileOrFolderName.endsWith("m3u8")) {
                                    icf_FileItem.bSetSubItem = true;
                                } else {

                                    icf_FileItem.bSetHeadItem = true;
                                    //If this is a .msu8 file, go and find the folder item and set it as a subitem so the folder item
                                    // doesn't show up as an item for orphan analysis: //todo: this should never be hit if we do not evaluate folders, as indicated with code
                                    //  icf_FileItem.iTypeFileFolderURL != ItemClass_File.TYPE_FOLDER.
                                    String sParentFolder = icf_FileItem.sMediaFolderRelativePath;
                                    if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).containsKey(sParentFolder)){
                                        ItemClass_File icf_M3U8ParentFolder = GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).get(sParentFolder);
                                        icf_M3U8ParentFolder.bSetSubItem = true;
                                    }

                                }

                                //Perform a quick check to see if we have looked-up this file's parent
                                // previously:
                                String sRelativePath = icf_FileItem.sMediaFolderRelativePath;
                                if (tmKnownSets.containsKey(sRelativePath)) {
                                    boolean bSetIsOrphaned = tmKnownSets.get(sRelativePath);
                                    if (!bSetIsOrphaned) {
                                        continue;
                                    }
                                } else {
                                    boolean bSetIsOrphaned = ORPHANED;
                                    //Check to see if this comic or M3U8 member is part of the catalog or not:

                                    for(Map.Entry<String, ItemClass_File> entry_ItemInMediaFolder1: GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).entrySet()){
                                        String sFileFullRelativePath = entry_ItemInMediaFolder1.getKey();
                                        if (sFileFullRelativePath.startsWith(sRelativePath)) {
                                            //This String represents an item that's part of the comic / m3u8.
                                            if (sFileFullRelativePath.endsWith("m3u8")) {
                                                //This is the m3u8 member. Check to see if it is assigned
                                                //  to a catalog item.
                                                if (ssCatalogRelativeFullPaths.contains(sFileFullRelativePath)) {
                                                    bSetIsOrphaned = ASSIGNED_TO_CATALOG_ITEM;
                                                }
                                                tmKnownSets.put(sRelativePath, bSetIsOrphaned);
                                                //If the item is assigned to a catalog item, then it
                                                // is not orphaned and the rest of the greater analysis loop
                                                // for the item should be skipped.
                                                break;
                                            }
                                        }
                                    }
                                    if (!bSetIsOrphaned) {
                                        continue;
                                    }
                                }

                            } //End check if this is an orphaned m3u8 (loop continued if found not to be orphaned).

                            icf_FileItem.bIsOrphanedFile = true;

                            icf_FileItem.sUri = GlobalClass.gUriCatalogFolders[giMediaCategory] +
                                    GlobalClass.gsFileSeparator + entry_ItemInMediaFolder.getKey(); //This could include a .m3u8 file path and file name, if applicable.

                            if(!icf_FileItem.bSetSubItem) {
                                //If this file is not a subset item, such as a comic or M3U8 playlist supporting file, perform further analysis:

                                sMessage = "\n" + iFileItemCountOrphanedInThisFolder + ".\tItem not found in catalog:\t" +
                                        GlobalClass.gsCatalogFolderNames[giMediaCategory] +
                                        GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + entry_ItemInMediaFolder.getKey()) + "\n";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);


                                //Update the file item for greater analysis outside of this worker:
                                //Determine file extension:
                                String sfileExtension = icf_FileItem.sFileOrFolderName.contains(".") ? icf_FileItem.sFileOrFolderName.substring(icf_FileItem.sFileOrFolderName.lastIndexOf(".")) : "";
                                if (!sfileExtension.matches(".+")) {
                                    icf_FileItem.sExtension = "";
                                } else {
                                    icf_FileItem.sExtension = sfileExtension;
                                }

                                icf_FileItem.lVideoTimeInMilliseconds = 0;

                                icf_FileItem.bMetadataDetected = false;
                                String sWidth = "";  //We are not doing math with the width and height. Therefore no need to convert to int.
                                String sHeight = "";

                                if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                                    //Get the width and height of the image:
                                    try {
                                        Uri uriDocUri = Uri.parse(icf_FileItem.sUri);
                                        InputStream input = getApplicationContext().getContentResolver().openInputStream(uriDocUri);
                                        if (input != null) {
                                            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                                            onlyBoundsOptions.inJustDecodeBounds = true;
                                            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
                                            input.close();
                                            sWidth = "" + onlyBoundsOptions.outWidth;
                                            sHeight = "" + onlyBoundsOptions.outHeight;
                                            icf_FileItem.bMetadataDetected = true;
                                        }

                                    } catch (Exception e) {
                                        sMessage = "\t\t\tThere was a problem reading the image dimensions: " + e.getMessage();
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);
                                    }
                                } else if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                                    //todo: Get video duration and resolution/height/width


                                }

                                icf_FileItem.sWidth = sWidth;
                                icf_FileItem.sHeight = sHeight;

                                iFileItemCountOrphanedInThisFolder++;

                                String sAlternativeFileName = "";
                                if(!icf_FileItem.sExtension.equals("") && alsRecognizedNonJumbledFileExtensions.contains(icf_FileItem.sExtension)){
                                    //If this file name appears to be non-jumbled, consider an alternative, jumbled file name for comparison:
                                    sAlternativeFileName = GlobalClass.JumbleFileName(icf_FileItem.sFileOrFolderName);
                                }

                                Set<String> ssCatItemsMissingMediaFinds = new HashSet<>();
                                if (gtmCatalogItemsMissing != null) {
                                    //Check to see if this filename matches an item in the catalog that is missing it's media:
                                    boolean bOrphanedFileAssociatedWithMissingCatItem = false;

                                    if (gtmCatalogItemsMissing.size() > 0) {
                                        //Compare to catalog items missing items.
                                        for (Map.Entry<String, ItemClass_CatalogItem> entry : gtmCatalogItemsMissing.entrySet()) {
                                            if (entry.getValue().sFilename.equals(icf_FileItem.sFileOrFolderName) ||
                                                entry.getValue().sFilename.equals(sAlternativeFileName)) {

                                                sMessage = "\t\t\tA catalog item missing it's media is matched with this file. Expected location is: \n" +
                                                        "\t\t\t\t" + GlobalClass.gsCatalogFolderNames[giMediaCategory] +
                                                        GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + entry.getValue().sFolderRelativePath + GlobalClass.gsFileSeparator) +
                                                        entry.getValue().sFilename + "\n";
                                                SendLogLine(sMessage);
                                                sbLogLines.append(sMessage);
                                                bOrphanedFileAssociatedWithMissingCatItem = true;
                                                iFileItemCountOrphanedMissingMatch++;
                                                ssCatItemsMissingMediaFinds.add(entry.getKey());
                                                icf_FileItem.bOrphanAssociatedWithCatalogItem = true;
                                                icf_FileItem.bOrphanAssociatedCatalogItemIsMissingMedia = true;
                                                if(entry.getValue().sFilename.equals(sAlternativeFileName)){
                                                    iFileItemCountOrphanedMissingMatchCounterJumbled++;
                                                    icf_FileItem.bFileNameCounterToMediaItemJumbledName = true;
                                                }

                                                if (icf_FileItem.alsOrphanAssociatedCatalogItemIDs == null) {
                                                    icf_FileItem.alsOrphanAssociatedCatalogItemIDs = new ArrayList<>();
                                                }
                                                icf_FileItem.alsOrphanAssociatedCatalogItemIDs.add(entry.getKey());
                                            }
                                        }
                                    }
                                    if (!bOrphanedFileAssociatedWithMissingCatItem) {
                                        sMessage = "\t\t\tThis orphaned file is not associated with any catalog item missing its media.\n";/* +
                                           "\t\t\t\tThe catalog item may have been deleted with a failed file-delete operation,\n" +
                                           "\t\t\t\ta backup of the catalog database file may have occurred which does not have\n" +
                                           "\t\t\t\trecord of a recently-imported item, or a user or other program may have \n" +
                                           "\t\t\t\tplaced a file in the location.\n";*/
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);
                                    }
                                }

                                //Check to see if there is an exact filename match in the catalog (including items that are NOT missing their media):
                                int iFileNameMatchCount = 0;
                                boolean bFoundMatch = false;
                                ArrayList<String> alsFileNameMatchPaths = new ArrayList<>();
                                for (Map.Entry<String, ItemClass_CatalogItem> entry : GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()) {
                                    if (entry.getValue().sFilename.equals(icf_FileItem.sFileOrFolderName) ||
                                            entry.getValue().sFilename.equals(sAlternativeFileName)) {

                                        iFileNameMatchCount++;

                                        if(entry.getValue().sFilename.equals(sAlternativeFileName)) {
                                            iFileItemCountOrphanedInThisFolderButNameMatchedJumbled++;
                                            icf_FileItem.bFileNameCounterToMediaItemJumbledName = true;
                                        }

                                        if (!bFoundMatch) {
                                            iFileItemCountOrphanedInThisFolderButNameMatched++;
                                            icf_FileItem.bOrphanAssociatedWithCatalogItem = true;
                                            bFoundMatch = true;
                                        }
                                        if (icf_FileItem.sDestinationFolder.equals("")) {
                                            icf_FileItem.sDestinationFolder = entry.getValue().sFolderRelativePath; //Pre-select the first matching catalog item's folder.
                                        }

                                        if(iFileNameMatchCount == 1){
                                            //If a catalog item match was found, copy over the tags as prospective tags to allow better comparison.
                                            icf_FileItem.aliProspectiveTags = entry.getValue().aliTags;
                                        } else if(iFileNameMatchCount == 2){
                                            //If there are more than one matching catalog item file name, clear the prospective tags to avoid confusion.
                                            icf_FileItem.aliProspectiveTags = new ArrayList<>();
                                        }


                                        if (ssCatItemsMissingMediaFinds.contains(entry.getKey())) {
                                            //We have already notified the user that the orphaned file matches this catalog items' media, and that
                                            //  this particular catalog item is in fact missing it's media at it's intended location.
                                            //  Don't record this item for display to the user a second time.
                                            continue;
                                        }
                                        if (icf_FileItem.alsOrphanAssociatedCatalogItemIDs == null) {
                                            icf_FileItem.alsOrphanAssociatedCatalogItemIDs = new ArrayList<>();
                                        }
                                        icf_FileItem.alsOrphanAssociatedCatalogItemIDs.add(entry.getKey());
                                        String sMatchPath = entry.getValue().sFolderRelativePath + GlobalClass.gsFileSeparator + entry.getValue().sFilename;
                                        alsFileNameMatchPaths.add(sMatchPath);



                                    } //End if file name match found.

                                } //End loop through catalog items looking for file name match.

                                icf_FileItem.iFileNameDuplicationCount = iFileNameMatchCount;
                                if (bFoundMatch) {
                                    //Provide a message to the user that the file name was matched with one or more catalog file items:
                                    for (String sMatchPath : alsFileNameMatchPaths) {
                                        sMessage = "\t\t\tThis orphaned file name matches with file name for catalog item at location:\t" +
                                                GlobalClass.gsCatalogFolderNames[giMediaCategory] +
                                                GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + sMatchPath) + "\n";
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);
                                    }
                                }


                            } //End if this file item is NOT a member of a set, such as a comic of M3U8 playlist.

                            //Add the ItemClass_File to the ArrayList:
                            alOrphanedFileList.add(icf_FileItem);

                        } //End if file appears to be orphaned.

                        if(!icf_FileItem.bSetSubItem) {
                            iFileItemCountInThisFolder++; //Important to have this increment here.
                                //All of the m3u8 video files will be processed, and it gets confusing to
                                // the user to see that they have thousands of files more than they expected
                                // in an individual folder. There is a 'continue' in the statements above
                                // that will cause this increment to skip if the file is not to be processed.
                        }

                        if(GlobalClass.giCatalog_Analysis_Approx_Max_Results > 0) { //If we are not processing unlimited results...
                            if ((alOrphanedFileList.size() > GlobalClass.giCatalog_Analysis_Approx_Max_Results) &&
                                    (icf_FileItem.iTypeFileFolderURL != ItemClass_File.TYPE_M3U8)) {
                                //Break if results are beyond threshhold and we are not in the middle of analysing a set.
                                break;
                            }
                        }

                    } //End loop through all files.

                    if(!bFolderSummaryCompleted){
                        //If there were orphaned files in this folder, display a summary for the folder:
                        PrintFolderSummaryForOrphanedFiles(sbLogLines,
                                iFileItemCountInThisFolder,
                                iFileItemCountOrphanedInThisFolder,
                                iFileItemCountOrphanedInThisFolderButNameMatched,
                                iFileItemCountOrphanedInThisFolderButNameMatchedJumbled,
                                iFileItemCountOrphanedMissingMatch,
                                iFileItemCountOrphanedMissingMatchCounterJumbled,
                                sCurrentFolder);
                    }


                    //If there were orphaned sets, such as m3u8 or comics, go through and apply any attributes
                    // associated with the head item for that set to the other file items that are a
                    // member of that set:
                    for(Map.Entry<String, Boolean> tmKnownSet: tmKnownSets.entrySet()){
                        if(tmKnownSet.getValue() == ORPHANED){
                            boolean bOrphanedSetAssociatedWCatItem = false;
                            boolean bOrphanedSetAssociatedWCatItemMissingMedia = false;
                            String sDestinationFolder = "";
                            for(ItemClass_File icf_OrphanedFile: alOrphanedFileList){
                                if(icf_OrphanedFile.sMediaFolderRelativePath.equals(tmKnownSet.getKey())){
                                    if(icf_OrphanedFile.bSetHeadItem){
                                        bOrphanedSetAssociatedWCatItem = icf_OrphanedFile.bOrphanAssociatedWithCatalogItem;
                                        bOrphanedSetAssociatedWCatItemMissingMedia = icf_OrphanedFile.bOrphanAssociatedCatalogItemIsMissingMedia;
                                        sDestinationFolder = icf_OrphanedFile.sDestinationFolder;
                                        break;
                                    }
                                }
                            }
                            if(bOrphanedSetAssociatedWCatItem || bOrphanedSetAssociatedWCatItemMissingMedia){
                                for(ItemClass_File icf_OrphanedFile: alOrphanedFileList){
                                    if(icf_OrphanedFile.sMediaFolderRelativePath.equals(tmKnownSet.getKey())){
                                        if(!icf_OrphanedFile.bSetHeadItem){
                                            icf_OrphanedFile.bOrphanAssociatedWithCatalogItem = bOrphanedSetAssociatedWCatItem;
                                            icf_OrphanedFile.bOrphanAssociatedCatalogItemIsMissingMedia = bOrphanedSetAssociatedWCatItemMissingMedia;
                                            icf_OrphanedFile.sDestinationFolder = sDestinationFolder;
                                        }
                                    }
                                }
                            }

                        }
                    }


                    //todo: Change "files" verbage to "items", since M3U8s and comics encompass more than one file.

                    sMessage = "\n======================================================\n" +
                                 "----------   Orphaned File Analysis Summary   --------\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);

                    sMessage = "-\n";
                    sMessage = sMessage + "- Orphaned file analysis tested " + iProgressNumerator + " file items of " + iProgressDenominator + " total files.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                    int iOrphanedItemCount = 0;
                    for(ItemClass_File icf:alOrphanedFileList){
                        if(!icf.bSetSubItem){
                            iOrphanedItemCount++;
                        }
                    }

                    sMessage = "- A total of " + iOrphanedItemCount + " orphaned items were found.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);

                    //Orphaned files with catalog matches:
                    //Orphaned files with catalog matches, duplicating data:
                    //Orphaned files with "catalog matches that are missing their data":
                    int iOrphanedFilesWithCatalogMatches = 0;
                    int iOrphanedFilesWithCatalogMatchesDuplicateData = 0;
                    int iOrphanedFilesWithCatalogMatchesMissingData = 0;
                    for(ItemClass_File icf:alOrphanedFileList){
                        if(!icf.bSetSubItem){
                            iOrphanedFilesWithCatalogMatches++;
                            if(icf.bOrphanAssociatedWithCatalogItem){
                                if(!icf.bOrphanAssociatedCatalogItemIsMissingMedia){
                                    iOrphanedFilesWithCatalogMatchesDuplicateData++;
                                } else {
                                    iOrphanedFilesWithCatalogMatchesMissingData++;
                                }
                            }
                        } else {
                            Log.d("Worker_Catalog_Verification:" + "doWork()", "Odd orphaned listing - subitem."); //todo: remove if not applicable.
                        }
                    }

                    if(iOrphanedItemCount > 0){
                        sMessage = "- Of the orphaned files, " + iOrphanedFilesWithCatalogMatchesDuplicateData + " of those files are duplicates of files in proper storage for catalog items.\n";
                        sMessage = sMessage + "- Of the orphaned files, " + iOrphanedFilesWithCatalogMatchesMissingData + " of those files align with catalog items missing their data.\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);
                    }


                    if(GlobalClass.giCatalog_Analysis_Approx_Max_Results > 0) { //If we are not processing unlimited results...
                        if (iOrphanedItemCount > GlobalClass.giCatalog_Analysis_Approx_Max_Results) {
                            sMessage = "-\n";
                            sMessage = sMessage + "Analysis result limit set to " + GlobalClass.giCatalog_Analysis_Approx_Max_Results + ", but continues until the end of a folder is reached.\n" +
                                    "- For more results, resolve the existing orphaned files and run the analysis again.\n\n";
                            SendLogLine(sMessage);
                            sbLogLines.append(sMessage);
                        }
                    }

                    sMessage = "------------------------------------------------------\n\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);

                    // =============================================================================
                    // ============================ System repair ==================================
                    // =============================================================================
                    /*
                    //Trim database of missing catalog items:
                    if (GlobalClass.gbCatalogAnalysis_TrimMissingCatalogItems
                            && (GlobalClass.giCatalogAnalysis_TrimMissingCatalogItemLimit != 0) ) {

                        sMessage = "\n------------------------------------------------------\n" +
                                     "----------   System Repair - Catalog Trim   ----------\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);

                        sMessage = "\nTrimming catalog items with missing media in accordance with user settings.\n";
                        if(GlobalClass.giCatalogAnalysis_TrimMissingCatalogItemLimit > 0){
                            sMessage = sMessage + "Trim limit set to " + GlobalClass.giCatalogAnalysis_TrimMissingCatalogItemLimit + " items.\n";
                        } else {
                            sMessage = sMessage + "Trim limit configured to trim all applicable items.\n";
                        }
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);

                        //Remove from memory and update the catalog file any catalog items whose files were not found:
                        int iTrimCount = 0;
                        for (Map.Entry<String, ItemClass_CatalogItem> entry : gtmCatalogItemsMissing.entrySet()) {
                            iTrimCount++;
                            if((iTrimCount <= GlobalClass.giCatalogAnalysis_TrimMissingCatalogItemLimit)
                                || GlobalClass.giCatalogAnalysis_TrimMissingCatalogItemLimit == -1) {
                                sMessage = iTrimCount + ". Removing item ID " + entry.getKey() + "... ";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);
                                GlobalClass.gtmCatalogLists.get(giMediaCategory).remove(entry.getKey());
                                sMessage = "success.\n";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);
                            } else {
                                break;
                            }
                        }
                        sMessage = "\nCatalog trimming complete. Writing catalog file.\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);

                        globalClass.CatalogDataFile_UpdateCatalogFile(giMediaCategory, "Updating catalog after content trim...");

                        sMessage = "Catalog file write complete.\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);
                    }


                    //Move files/items related to orphaned files and catalog items missing their media:
                    if(GlobalClass.gbCatalogAnalysis_RepairOrphanedItems &&
                            (GlobalClass.giCatalogAnalysis_RepairOrphanedItemLimit != 0)){

                        sMessage = "\n------------------------------------------------------\n" +
                                     "------   System Repair - Orphaned Match Move   -------\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);

                        sMessage = "\nMoving orphaned files that have been matched with catalog items with missing media in accordance with user settings.\n";
                        if(GlobalClass.giCatalogAnalysis_RepairOrphanedItemLimit > 0){
                            sMessage = sMessage + "Repair limit set to " + GlobalClass.giCatalogAnalysis_RepairOrphanedItemLimit + " items.\n";
                        } else {
                            sMessage = sMessage + "Repair limit configured to repair all applicable items.\n";
                        }
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);

                        //Go through the list of orphaned file items and move files into position where necessary:
                        int iRepairCount = 0;
                        for (ItemClass_File icfOrphanedItem:  alOrphanedFileList) {
                            if(icfOrphanedItem.bOrphanAssociatedCatalogItemIsMissingMedia &&
                                    (icfOrphanedItem.alsOrphanAssociatedCatalogItemIDs.size() == 1) ) {

                                iRepairCount++;
                                if ((iRepairCount <= GlobalClass.giCatalogAnalysis_RepairOrphanedItemLimit)
                                        || GlobalClass.giCatalogAnalysis_RepairOrphanedItemLimit == -1) {

                                    String sItemID = icfOrphanedItem.alsOrphanAssociatedCatalogItemIDs.get(0);

                                    sMessage = iRepairCount + ". Repairing item ID " + sItemID + "... ";
                                    SendLogLine(sMessage);
                                    sbLogLines.append(sMessage);

                                    //Need:
                                    //  SourceDocumentUri
                                    //  SourceParentDocumentUri
                                    //  TargetParentUri

                                    //Get SourceDocumentUri:
                                    Uri uriSourceDocumentUri;
                                    if(icfOrphanedItem.iTypeFileFolderURL == ItemClass_File.TYPE_M3U8){
                                        //If this is an m3u8 item, then the Uri desired for the item is the parent folder
                                        // for the .m3u8 file item.
                                        try {
                                            uriSourceDocumentUri = GlobalClass.GetParentUri(icfOrphanedItem.sUri);
                                        } catch (Exception e){
                                            sMessage = "Could not form source Uri. Error message: " + e.getMessage();
                                            SendLogLine(sMessage);
                                            sbLogLines.append(sMessage);
                                            continue;
                                        }
                                    } else {
                                        uriSourceDocumentUri = Uri.parse(icfOrphanedItem.sUri);
                                    }

                                    //Get the SourceParentDocumentUri:
                                    Uri uriSourceParentDocumentUri;
                                    try {
                                        if (icfOrphanedItem.iTypeFileFolderURL == ItemClass_File.TYPE_M3U8) {
                                            //If this is an m3u8 item, then the Uri desired for the item is the parent folder
                                            // for the .m3u8 file item, and the source parent Uri is the parent to that parent.
                                            uriSourceParentDocumentUri = GlobalClass.GetParentUri(uriSourceDocumentUri);
                                        } else {
                                            uriSourceParentDocumentUri = GlobalClass.GetParentUri(uriSourceDocumentUri);
                                        }
                                    } catch (Exception e){
                                        sMessage = "Could not form source parent Uri. Error message: " + e.getMessage();
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);
                                        continue;
                                    }

                                    //Get the TargetParentUri:
                                    ItemClass_CatalogItem icci = GlobalClass.gtmCatalogLists.get(giMediaCategory).get(sItemID);
                                    String sFolderRelativePath;
                                    if(icfOrphanedItem.iTypeFileFolderURL == ItemClass_File.TYPE_M3U8) {
                                        //If this is an m3u8 item, then the Uri desired for the item is the parent folder
                                        // for the .m3u8 file item, and the source parent Uri is the parent to that parent.
                                        String[] sFolders = icci.sFolderRelativePath.split(GlobalClass.gsFileSeparator);
                                        if (sFolders.length <= 1) {
                                            //There should be at least 2 folders here, since the m3u8 content is held in a folder.
                                            continue;
                                        }
                                        StringBuilder sbParentFolder = new StringBuilder();
                                        for (int i = 0; i < sFolders.length - 1; i++) {
                                            //In the case of m3u8, we are ignoring the last folder on the relative path.
                                            sbParentFolder.append(sFolders[i]);
                                            if (i < sFolders.length - 2) {
                                                sbParentFolder.append(GlobalClass.gsFileSeparator);
                                            }
                                        }
                                        sFolderRelativePath = sbParentFolder.toString();
                                    } else {
                                        sFolderRelativePath = icci.sFolderRelativePath;
                                    }
                                    String sTargetParentDocumentUri = GlobalClass.gsUriAppRootPrefix
                                            + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                            + GlobalClass.gsFileSeparator + sFolderRelativePath;
                                    Uri uriTargetParentDocumentUri = Uri.parse(sTargetParentDocumentUri);

                                    //Verify target folder location:
                                    if (!GlobalClass.CheckIfFileExists(uriTargetParentDocumentUri)) {
                                        continue;
                                    }


                                    try {
                                        DocumentsContract.moveDocument(GlobalClass.gcrContentResolver, uriSourceDocumentUri, uriSourceParentDocumentUri, uriTargetParentDocumentUri);
                                        sMessage = "success.\n";
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);

                                    } catch (Exception e){
                                        sMessage = "Could not move item. Error message: " + e.getMessage();
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);
                                    }


                                } else {
                                    //If the orphaned file repair limit has been reached...
                                    break;
                                }

                            } //End if the orphaned file is associated with a catalog item missing its media.
                            if ((iRepairCount >= GlobalClass.giCatalogAnalysis_RepairOrphanedItemLimit) && GlobalClass.giCatalogAnalysis_RepairOrphanedItemLimit != -1) { break;}
                        } //End loop through orphaned file items.

                        sMessage = "\nFinished repairing orphaned file items.\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);

                    }
                    */
                    // ======================== End System Repair Section ==========================
                    // =============================================================================




                    if(gtmCatalogItemsMissing != null) {
                        for (Map.Entry<String, ItemClass_CatalogItem> entry : gtmCatalogItemsMissing.entrySet()) {
                            alsCatalogItemsMissing.add(entry.getKey());
                        }
                    }

                } // end if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                    //giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {



            } //end if(giAnalysisType == ANALYSIS_TYPE_MISSING_FILES).




            if((giAnalysisType == ANALYSIS_TYPE_M3U8)) {


                sMessage =            "============================================================\n";
                sMessage = sMessage + "============== ANALYZING M3U8 PLAYLIST FILES ===============\n\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);

                //Count M3U8 items to be processed:
                iProgressNumerator = 0;
                iProgressDenominator = 0;
                int iM3U8ProblemItemCount = 0;
                for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: GlobalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).entrySet()) {
                    ItemClass_CatalogItem ci = tmEntry.getValue();
                    if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                        iProgressDenominator++;
                    }
                }

                //Process all video catalog items, specifically the M3U8 items:
                boolean bWriteCatalogFile = false;
                int iM3U8_Files_Updated = 0;
                for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: GlobalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).entrySet()){

                    ItemClass_CatalogItem ci = tmEntry.getValue();

                    if(ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8){
                        iProgressNumerator++;
                        iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                        globalClass.BroadcastProgress(false, "",
                                true, iProgressBarValue,
                                true, "Verifying item " + iProgressNumerator + "/" + iProgressDenominator,
                                CATALOG_ANALYSIS_ACTION_RESPONSE);

                        //If this is an M3U8 file catalog item, locate the M3U8 file and update file paths.

                        String[] sFileNameAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(ci.sFilename);
                        if(sFileNameAndExtension.length != 2){
                            iM3U8ProblemItemCount++;
                            sMessage = "\n" + iM3U8ProblemItemCount + ". Catalog item ID " + ci.sItemID + " does not have a properly formatted file name. Skipping file. File name: " + ci.sFilename + "\n";
                            SendLogLine(sMessage);
                            sbLogLines.append(sMessage);
                            continue;
                        }

                        String sM3U8_FileName = ci.sFilename;

                        //Determine if the file exists before continuing:
                        // TreeMap<String, ItemClass_File>.
                        // The key consists of icf.sMediaFolderRelativePath + GlobalClass.gsFileSeparator + icf.sFileOrFolderName
                        // boolean bFileExists = gtmicf_AllFileItemsInMediaFolder[giMediaCategory].containsKey(sFileCheckFullPath);
                        String sRelativeFullPath = ci.sFolderRelativePath + GlobalClass.gsFileSeparator + sM3U8_FileName;
                        boolean bM3U8_File_Found = GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).containsKey(sRelativeFullPath);

                        String sUpdatedM3U8BasePath = GlobalClass.gsUriAppRootPrefix
                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                                + GlobalClass.gsFileSeparator + ci.sFolderRelativePath;

                        if (!bM3U8_File_Found) {
                            als_M3U8_CatItems_Missing_Playlist.add(tmEntry.getKey());
                            iM3U8ProblemItemCount++;
                            sMessage = "\n" + iM3U8ProblemItemCount + ". Could not find m3u8 file for Catalog item ID " + ci.sItemID + "\n";
                            sMessage = sMessage + "File expected at Videos" + GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + ci.sFolderRelativePath) + "\n";
                            SendLogLine(sMessage);
                            sbLogLines.append(sMessage);
                            continue;
                        }

                        //Open the m3u8 file and ensure that it has the proper paths:
                        String sM3U8_Uri = sUpdatedM3U8BasePath
                                + GlobalClass.gsFileSeparator + sM3U8_FileName;
                        byte[] byteM3U8_File = null;
                        Uri uriM3U8 = null;
                        try{
                            uriM3U8 = Uri.parse(sM3U8_Uri);
                            InputStream isM3U8 = GlobalClass.gcrContentResolver.openInputStream(uriM3U8);
                            if(isM3U8 == null){
                                iM3U8ProblemItemCount++;
                                sMessage = "\n" + iM3U8ProblemItemCount + ". Catalog item ID " + ci.sItemID + " does not have an m3u8 file.\n";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);

                            } else {
                                byteM3U8_File = GlobalClass.readAllBytes(isM3U8);
                                isM3U8.close();
                            }

                        } catch (Exception e){
                            iM3U8ProblemItemCount++;
                            sMessage = "\n" + iM3U8ProblemItemCount + ". Catalog item ID " + ci.sItemID + " had trouble opening and reading an m3u8 file.\n" + e.getMessage() + "\n";
                            SendLogLine(sMessage);
                            sbLogLines.append(sMessage);
                        }

                        if(byteM3U8_File != null) {
                            //Read-in one path to make sure it is accurate, and then read the rest of the records to
                            // ensure the segment files exist.
                            String sM3U8_File_Contents = new String(byteM3U8_File);
                            String[] sM3U8_FileLines = sM3U8_File_Contents.split("\n");

                            int iSegmentFileCount = 0;
                            int iSegmentFilesFound = 0;

                            boolean bM3U8_File_Internal_Paths_UpToDate = M3U8FileRelativePathsUptoDate(sM3U8_FileLines);

                            if(!bM3U8_File_Internal_Paths_UpToDate){
                                //M3U8 file does not have up-to-date paths utilizing the current storage structure.
                                // This could be caused by moving the database.
                                iM3U8ProblemItemCount++;
                                sMessage = "\n" + iM3U8ProblemItemCount + ". Catalog item ID " + ci.sItemID + " does not have an up-to-date m3u8 file aligned with the current base storage." +
                                        " This is could be caused by a move of the folder. Attempting to update the file...\n";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);

                                //Update the file to the current base storage:
                                try {
                                    if(UpdateM3U8FileRelativePaths(sM3U8_FileLines, sUpdatedM3U8BasePath, uriM3U8)){
                                        iM3U8_Files_Updated++;
                                        sMessage = "File updated successfully.\n";
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);
                                        bM3U8_File_Internal_Paths_UpToDate = true;
                                    } else {
                                        sMessage = "\nCould not open file for writing for update to current path.\n";
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);
                                        continue;
                                    }
                                } catch (Exception e) {
                                    sMessage = "Problem processing and/or writing to updated M3U8 file: " + e.getMessage() + "\n";
                                    SendLogLine(sMessage);
                                }
                            } else {
                                //Count the M3U8 segment files listed in the M3U8 file, and check to make sure those files exist:
                                for (String sM3U8_File_Line : sM3U8_FileLines) {
                                    if (!sM3U8_File_Line.startsWith("#") && sM3U8_File_Line.endsWith("st")) {
                                        //sLine should now have a Uri string to a ts file.
                                        iSegmentFileCount++;
                                        //Check to ensure that the segment file exists:
                                        String sTemp = GlobalClass.gsUriAppRootPrefix
                                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS];
                                        int iLengthOfPathToExclude = sTemp.length();
                                        sRelativeFullPath = sM3U8_File_Line.substring(iLengthOfPathToExclude);
                                        if (sRelativeFullPath.startsWith("%2F")) {
                                            sRelativeFullPath = sRelativeFullPath.substring(3);
                                        }
                                        if (GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).containsKey(sRelativeFullPath)) {
                                            //gtmicf_AllFileItemsInMediaFolder key => icf.sMediaFolderRelativePath + GlobalClass.gsFileSeparator + icf.sFileOrFolderName;
                                            //Segment file exists in the videos media folder.
                                            iSegmentFilesFound++;
                                        }

                                    } //End if this is an M3U8 segment file listing.

                                } //End loop through M3U8 file record lines.
                            }


                            if((iSegmentFileCount != iSegmentFilesFound) && bM3U8_File_Internal_Paths_UpToDate){
                                als_M3U8_CatItems_Missing_Segments.add(ci.sItemID);
                                sMessage = "\n" + iProgressNumerator + ". Catalog item ID " + ci.sItemID +
                                        " is missing some of the segment files. Found " + iSegmentFilesFound +
                                        " of " + iSegmentFileCount + " files.\n" + "Files expected in folder " +
                                        "Videos" + GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + ci.sFolderRelativePath) + "\n";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);
                            }

                        } //End if the M3U8 file bytes not null.

                    } //End if this is an M3U8.

                } //End for loop going through Catalog video items.


                //Print summary:

                sMessage = "\n======================================================\n" +
                             "----------   M3U8 Playlist Analysis Summary   --------\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);

                sMessage = "-\n";
                sMessage = sMessage + "- Analyzed " + iProgressNumerator + " M3U8 playlists out of " + iProgressDenominator + ".\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);
                sMessage = "- M3U8 playlist files updated to current storage path: " + iM3U8_Files_Updated + "\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);
                sMessage = "- M3U8 playlists with missing segment files: " + als_M3U8_CatItems_Missing_Segments.size() + "\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);
                sMessage = "-\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);


                sMessage = "------------------------------------------------------\n\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);






            } // End if((giAnalysisType == ANALYSIS_TYPE_M3U8)).

            bwLogFile.write(sbLogLines.toString());
            bwLogFile.flush();
            bwLogFile.close();
            osLogFile.flush();
            osLogFile.close();

        } catch (Exception e){
            sMessage = "\nException: " + e.getMessage() + "\n\n";
            SendLogLine(sMessage);
        }


        //Broadcast a message with the list of files so that they can be processed in next steps:
        Intent broadcastIntent_CatalogAnalysisResponse = new Intent();
        if(!alOrphanedFileList.isEmpty()) {
            broadcastIntent_CatalogAnalysisResponse.putExtra(EXTRA_BOOL_GET_ARRAY_ORPHANED_FILEITEMS_RESPONSE, true);
            broadcastIntent_CatalogAnalysisResponse.putExtra(EXTRA_AL_GET_ARRAY_ORPHANED_FILEITEMS_RESPONSE, alOrphanedFileList);
        }
        if(!alsCatalogItemsMissing.isEmpty()) {
            broadcastIntent_CatalogAnalysisResponse.putExtra(EXTRA_BOOL_GET_ARRAY_MISSING_CAT_ITEMS_RESPONSE, true);
            broadcastIntent_CatalogAnalysisResponse.putExtra(EXTRA_AL_GET_ARRAY_MISSING_CAT_ITEMS_RESPONSE, alsCatalogItemsMissing);
        }
        if(!als_M3U8_CatItems_Missing_Segments.isEmpty()) {
            broadcastIntent_CatalogAnalysisResponse.putExtra(EXTRA_BOOL_GET_ARRAY_M3U8_MISSING_SEG_ITEMS_RESPONSE, true);
            broadcastIntent_CatalogAnalysisResponse.putExtra(EXTRA_AL_GET_ARRAY_M3U8_MISSING_SEG_ITEMS_RESPONSE, als_M3U8_CatItems_Missing_Segments);
        }

        if(alsCatalogItemsMissing.isEmpty() &&
                alOrphanedFileList.isEmpty() &&
                als_M3U8_CatItems_Missing_Playlist.isEmpty() &&
                als_M3U8_CatItems_Missing_Segments.isEmpty()) {
            broadcastIntent_CatalogAnalysisResponse.putExtra(EXTRA_BOOL_CAT_ANALYSIS_NO_ITEMS_RESPONSE, true);
        }

        broadcastIntent_CatalogAnalysisResponse.setAction(CATALOG_ANALYSIS_ACTION_RESPONSE);
        broadcastIntent_CatalogAnalysisResponse.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_CatalogAnalysisResponse);




        //todo: report the time it took to complete the operation.



        sMessage = "Catalog Verification complete.";
        SendLogLine(sMessage);

        if(gsbLogBuffer.length() > 0) {
            //We are nearing completion, ensure that all data from the log is sent:
            //Because sending of this data is periodic, there may be some data not sent
            //  by the last call to SendLogLine() if the time did not perfectly align on a "tick".
            globalClass.BroadcastProgress(true, gsbLogBuffer.toString(),
                    false, 0,
                    false, "",
                    CATALOG_ANALYSIS_ACTION_RESPONSE);
        }

        GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.FINISHED);
        globalClass.BroadcastProgress(false, "",
                true, 100,
                true, "Complete.",
                CATALOG_ANALYSIS_ACTION_RESPONSE);
        return Result.success();
    }


    private void PrintFolderSummaryForOrphanedFiles(StringBuilder sbLogLines,
                                                    int iFileItemCountInFolder,
                                                    int iFileItemCountOrphanedInThisFolder,
                                                    int iFileItemCountOrphanedInThisFolderButNameMatched,
                                                    int iFileItemCountOrphanedInThisFolderButNameMatchedJumbled,
                                                    int iFileItemCountOrphanedMissingMatch,
                                                    int iFileItemCountOrphanedMissingMatchCounterJumbled,
                                                    String sCurrentFolder){
        //If there were orphaned files in this folder, display a summary for the folder:
        String sMessage;
        sMessage = "\n------------------------------------------------------\n" +
                     "----   Orphaned File Analysis - Folder Summary   -----\n" +
                     "------------------------------------------------------\n";
        SendLogLine(sMessage);
        sbLogLines.append(sMessage);

        sMessage = "-\n";
        sMessage = sMessage + "- Reviewed folder: " + GlobalClass.gsCatalogFolderNames[giMediaCategory] +
                GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + sCurrentFolder) + "\n";
        SendLogLine(sMessage);
        sbLogLines.append(sMessage);

        if(iFileItemCountOrphanedInThisFolder > 0) {
            sMessage = "-\n";
            sMessage = sMessage + "- There were " + iFileItemCountOrphanedInThisFolder + " orphaned files in this folder out of " + iFileItemCountInFolder + " files.\n";
            SendLogLine(sMessage);
            sbLogLines.append(sMessage);

            if(iFileItemCountOrphanedInThisFolderButNameMatched > 0){
                sMessage = "- Some of the file names matched file names with catalog items. " + iFileItemCountOrphanedInThisFolderButNameMatched + " matches were found.\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);
            }
            if(iFileItemCountOrphanedInThisFolderButNameMatchedJumbled > 0){
                sMessage = "- Some of the file names matched file names with catalog items if the file names were jumbled. " + iFileItemCountOrphanedInThisFolderButNameMatchedJumbled + " matches were found.\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);
            }

            if(iFileItemCountOrphanedMissingMatch > 0){
                sMessage = "- \t\t" + iFileItemCountOrphanedMissingMatch + " of the orphaned files matched catalog items' missing media file names.\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);
            } else {
                sMessage = "- None of these orphaned file names matched file names with catalog items which are missing their media.\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);
            }

            if(iFileItemCountOrphanedMissingMatchCounterJumbled > 0){
                sMessage = "- \t\t" + iFileItemCountOrphanedMissingMatchCounterJumbled + " of the orphaned files matched catalog items' missing media file names, if the file names were not Jumbled.\n";
                SendLogLine(sMessage);
                sbLogLines.append(sMessage);
            }
        }

        sMessage = "-\n";
        sMessage = sMessage + "------------------------------------------------------\n";
        SendLogLine(sMessage);
        sbLogLines.append(sMessage);

    }


    double gdLastLogDelivery = 0d;
    StringBuilder gsbLogBuffer = new StringBuilder();

    private void SendLogLine(String sLogLine){

        gsbLogBuffer.append(sLogLine);

        double dCurrentTime = GlobalClass.GetTimeStampDouble();
        double dtDiff = dCurrentTime - gdLastLogDelivery;
        float fDeliveryPeriodSeconds = 1.0f;
        double dSecConv = fDeliveryPeriodSeconds * .000001d;
        if(dtDiff > dSecConv) {
            globalClass.BroadcastProgress(true, gsbLogBuffer.toString(),
                    false, 0,
                    false, "",
                    CATALOG_ANALYSIS_ACTION_RESPONSE);
            gsbLogBuffer.setLength(0);
            gdLastLogDelivery = dCurrentTime;
        }
    }


    public static boolean M3U8FileRelativePathsUptoDate(String[] sM3U8_FileLines){
        for (String sM3U8_File_Line : sM3U8_FileLines) {
            if (!sM3U8_File_Line.startsWith("#") && sM3U8_File_Line.endsWith("st")) {
                //sLine should now have a Uri string to a ts file.
                if (sM3U8_File_Line.startsWith(GlobalClass.gsUriAppRootPrefix)) {
                    return true;
                } else {
                    return false;
                }
            } //End if this is an M3U8 segment file listing.
        } //End loop through M3U8 file record lines.
        return true; //If we are here, then the file likely has no valid records.
    }

    /**
     * @param sM3U8_FileLines String Array containing lines from the file to be examined, altered, & written.
     * @param sUpdatedM3U8BasePath A path similar to: GlobalClass.gsUriAppRootPrefix
     *             + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
     *             + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
     * @param uriM3U8 The Uri to the file to be written.
     *
     * @return Returns true if successful, returns false if it could not write the new file.
     * @throws Exception
     */
    public static boolean UpdateM3U8FileRelativePaths(String[] sM3U8_FileLines, String sUpdatedM3U8BasePath, Uri uriM3U8) throws Exception{
        //Update the file to the current base storage:

        StringBuilder sbM3U8 = new StringBuilder();
        String sLine;
        for (String sM3U8_File_Line : sM3U8_FileLines) {
            sLine = sM3U8_File_Line;
            if (!sM3U8_File_Line.startsWith("#") && sM3U8_File_Line.endsWith("st")) {
                //sLine should be a line which is a file name or a path + a file name.
                String sFileName;
                int iLastIndexOfFileDelimeter = sLine.lastIndexOf(GlobalClass.gsFileSeparator);
                if(iLastIndexOfFileDelimeter > 0){
                    sFileName = sLine.substring(sLine.lastIndexOf("/"));
                } else {
                    sFileName = sLine;
                }
                //Don't try to make sure that the file exists here. It is important that the file exists or the player
                //  might not play at all, but that is a task for another routine in order to save time.
                sLine = sUpdatedM3U8BasePath + GlobalClass.gsFileSeparator + sFileName;
            }
            sbM3U8.append(sLine);
            sbM3U8.append("\n");

        } //End loop through M3U8 file record lines.
        //Updated file formed in memory. Write the file:
        OutputStream osM3U8 = GlobalClass.gcrContentResolver.openOutputStream(uriM3U8);
        if (osM3U8 == null) {
            return false;
        }
        String sM3U8_Data = sbM3U8.toString();
        osM3U8.write(sM3U8_Data.getBytes(StandardCharsets.UTF_8));
        osM3U8.flush();
        osM3U8.close();

        return true;

    }

}
