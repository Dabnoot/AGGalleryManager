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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Worker_Catalog_Analysis extends Worker {

    public static final String TAG_WORKER_CATALOG_VERIFICATION = "com.agcurations.aggallermanager.tag_worker_catalog_verification";

    public static final String CATALOG_VERIFICATION_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_VERIFICATION_ACTION_RESPONSE";

    GlobalClass globalClass;

    int giMediaCategory;
    int giAnalysisType;

    TreeMap<String, ItemClass_CatalogItem> gtmCatalogItemsMissing;

    public Worker_Catalog_Analysis(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
        giAnalysisType = getInputData().getInt(ViewModel_CatalogAnalysis.EXTRA_ANALYSIS_TYPE, -1);

    }

    @NonNull
    @Override
    public Result doWork() {

        globalClass = (GlobalClass) getApplicationContext();

        String sMessage;
        if(giMediaCategory == -1){
            sMessage = "No catalog specified.";
            LogThis("doWork()", sMessage, null);
            GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.STOPPED);
            return Result.failure();
        }

        if(GlobalClass.aiCatalogVerificationRunning.get() == GlobalClass.RUNNING){
            sMessage = "Stopping Catalog Verification...\n";
            LogThis("doWork()", sMessage, null);
            GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.STOP_REQUESTED);
            return Result.success();
        }
        GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.RUNNING);


        int iProgressNumerator = 0;
        int iProgressDenominator = GlobalClass.gtmCatalogLists.get(giMediaCategory).size();
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
        try { //Required for the log file.
            osLogFile = GlobalClass.gcrContentResolver.openOutputStream(uriLogFile, "wt");
            if (osLogFile == null) {
                GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.STOPPED);
                return Result.failure();
            }
            bwLogFile = new BufferedWriter(new OutputStreamWriter(osLogFile));


            if(giAnalysisType == ViewModel_CatalogAnalysis.ANALYSIS_TYPE_MISSING_FILES) {
                gtmCatalogItemsMissing = new TreeMap<>();
                int iLongestID = 0;
                for (Map.Entry<String, ItemClass_CatalogItem> entry : GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()) {

                    ItemClass_CatalogItem icci = entry.getValue();

                    iProgressNumerator++;
                    iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                    globalClass.BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "Verifying item " + iProgressNumerator + "/" + iProgressDenominator,
                            CATALOG_VERIFICATION_ACTION_RESPONSE);


                    String sCatalogItemUri = "";
                    String sItemRelativePathUserFriendly = "";
                    if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                        if (icci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                            //A folder containing files related to this M3U8:
                            sCatalogItemUri = GlobalClass.gsUriAppRootPrefix
                                    + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                    + GlobalClass.gsFileSeparator + icci.sFolderRelativePath;
                            sItemRelativePathUserFriendly = GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                    + GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator +  icci.sFolderRelativePath);
                        } else {
                            //A single file:
                            sCatalogItemUri = GlobalClass.gsUriAppRootPrefix
                                    + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                    + GlobalClass.gsFileSeparator + icci.sFolderRelativePath
                                    + GlobalClass.gsFileSeparator + icci.sFilename;
                            sItemRelativePathUserFriendly = GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                    + GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator +  icci.sFolderRelativePath + GlobalClass.gsFileSeparator)
                                    + icci.sFilename;
                        }
                    } else if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                        //A single file:
                        sCatalogItemUri = GlobalClass.gsUriAppRootPrefix
                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                + GlobalClass.gsFileSeparator + icci.sFolderRelativePath
                                + GlobalClass.gsFileSeparator + icci.sFilename;
                        sItemRelativePathUserFriendly = GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                + GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator +  icci.sFolderRelativePath + GlobalClass.gsFileSeparator)
                                + icci.sFilename;
                    } else if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                        //A folder containing files related to this comic:
                        sCatalogItemUri = GlobalClass.gsUriAppRootPrefix
                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                + GlobalClass.gsFileSeparator + icci.sFolderRelativePath;
                        sItemRelativePathUserFriendly = GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                +GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator +  icci.sFolderRelativePath);
                    }
                    Uri uriCatalogItemUri = Uri.parse(sCatalogItemUri);

                    if (!GlobalClass.CheckIfFileExists(uriCatalogItemUri)) {
                        boolean bMarkItemAsMissing = true;
                        int iMissingItemCount = gtmCatalogItemsMissing.size() + 1;

                        sMessage = iMissingItemCount + ". Item with ID " + icci.sItemID + " not found. Expected at location: " +
                                sItemRelativePathUserFriendly + "\n";

                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);
                        Log.d("AGGalleryManager", sMessage);

                        if(GlobalClass.bAllowCheckAndMoveOfComicFolders) {
                            //Temporary code related to a data crash and recovery operation:
                            //Check if item exists at one level above, and if so, attempt to move it:
                            if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                                if (icci.sFolderRelativePath.contains(GlobalClass.gsFileSeparator)) {
                                    sMessage = "Checking to see if the item folder exists one level up in the heirarchy...\n";
                                    SendLogLine(sMessage);
                                    sbLogLines.append(sMessage);
                                    Log.d("AGGalleryManager", sMessage);

                                    //Get the suspected parent folder Uri to ensure that it exists, too:
                                    String sSuspectedParentFolderUri = GlobalClass.gsUriAppRootPrefix
                                            + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory];
                                    Uri uriSuspectedParentFolderUri = Uri.parse(sSuspectedParentFolderUri);

                                    //Get the suspected actual item folder Uri:
                                    String sItemFolderName = icci.sFolderRelativePath.substring(icci.sFolderRelativePath.lastIndexOf(GlobalClass.gsFileSeparator));
                                    String sSuspectedActualUri = GlobalClass.gsUriAppRootPrefix
                                            + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                            + GlobalClass.gsFileSeparator + sItemFolderName;
                                    Uri uriSuspectedActualUri = Uri.parse(sSuspectedActualUri);

                                    if (GlobalClass.CheckIfFileExists(uriSuspectedParentFolderUri)) {
                                        sMessage = "Parent folder confirmation successful...\n";
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);
                                        Log.d("AGGalleryManager", sMessage);
                                        if (GlobalClass.CheckIfFileExists(uriSuspectedActualUri)) {
                                            sMessage = "Item folder was found one level up in the heirarchy. Attempting to move the folder to the expected location...\n";
                                            SendLogLine(sMessage);
                                            sbLogLines.append(sMessage);
                                            Log.d("AGGalleryManager", sMessage);

                                            //Form the target parent Uri (the parent folder to which the item folder will be moved):
                                            String sTargetParentFolderName = icci.sFolderRelativePath.substring(0, icci.sFolderRelativePath.lastIndexOf(GlobalClass.gsFileSeparator));
                                            String sTargetParentFolderUri = GlobalClass.gsUriAppRootPrefix
                                                    + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                                    + GlobalClass.gsFileSeparator + sTargetParentFolderName;
                                            Uri uriTargetParentFolderUri = Uri.parse(sTargetParentFolderUri);

                                            boolean bExpectedParentFolderOk = true;
                                            if (!GlobalClass.CheckIfFileExists(uriTargetParentFolderUri)) {
                                                //If the folder does not exist, create it.
                                                try {
                                                    uriTargetParentFolderUri = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriSuspectedParentFolderUri, DocumentsContract.Document.MIME_TYPE_DIR, sTargetParentFolderName);
                                                } catch (Exception e) {
                                                    bExpectedParentFolderOk = false;
                                                    sMessage = "Expected location's parent folder was not found and could not create the folder.\n";
                                                    SendLogLine(sMessage);
                                                    sbLogLines.append(sMessage);
                                                    Log.d("AGGalleryManager", sMessage);
                                                }
                                                if (uriTargetParentFolderUri == null) {
                                                    bExpectedParentFolderOk = false;
                                                }

                                            }

                                            if (bExpectedParentFolderOk) {
                                                boolean bMoveSuccess = false;
                                                try {
                                                    Uri uriResult = DocumentsContract.moveDocument(
                                                            GlobalClass.gcrContentResolver,
                                                            uriSuspectedActualUri,
                                                            uriSuspectedParentFolderUri,
                                                            uriTargetParentFolderUri);
                                                    if (uriResult != null) {
                                                        bMoveSuccess = true;
                                                        bMarkItemAsMissing = false;
                                                    }
                                                } catch (Exception e) {
                                                    sMessage = "Error encountered during move attempt: " + e.getMessage() + "\n";
                                                    SendLogLine(sMessage);
                                                    sbLogLines.append(sMessage);
                                                    Log.d("AGGalleryManager", sMessage);
                                                }
                                                if (bMoveSuccess) {
                                                    sMessage = "Item folder was successfully moved to the expected location.\n\n";
                                                    SendLogLine(sMessage);
                                                    sbLogLines.append(sMessage);
                                                    Log.d("AGGalleryManager", sMessage);
                                                }
                                            }


                                        } else {
                                            sMessage = "Item folder does not exist one level up in the heirarchy.\n\n";
                                            SendLogLine(sMessage);
                                            sbLogLines.append(sMessage);
                                            Log.d("AGGalleryManager", sMessage);
                                        }
                                    }
                                }
                            }
                        }

                        if(bMarkItemAsMissing){
                            gtmCatalogItemsMissing.put(entry.getKey(), entry.getValue());
                        }


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
                    sMessage = sMessage + " " + gtmCatalogItemsMissing.size() + " catalog items were found to have missing media.\n\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                }

                //End search for catalog items with missing files.

                //If there were any missing items, search this program's data storage directory dedicated to the media type
                //  for the missing media:
                if(gtmCatalogItemsMissing.size() > 0) {
                    //Analyze Orphaned Files to see if any are a match.
                    sMessage = "===========================================================\n";
                    sMessage = sMessage + "=========== ANALYZING ORPHANED FILES FOR MATCHES ===========\n\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                }
            }

            if((giAnalysisType == ViewModel_CatalogAnalysis.ANALYSIS_TYPE_ORPHANED_FILES)
                || (giAnalysisType == ViewModel_CatalogAnalysis.ANALYSIS_TYPE_MISSING_FILES && gtmCatalogItemsMissing.size() > 0)) {
                //Analyze orphaned files if we are here for that, or
                // if we are analyzing missing files and the missing file count is > 0.

                //Analyze orphaned files:


                if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                        giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                    //Look for orphaned files:
                    sMessage = "Indexing folders...\n";
                    LogThis("doWork()", sMessage, null);

                    //Start with a listing of all folders in the selected media folder:
                    ArrayList<String> alsFolderNamesInUse = GlobalClass.GetDirectorySubfolderNames(GlobalClass.gUriCatalogFolders[giMediaCategory]);

                    //Get an array containing all of the file items found in the selected media storage (ie. Videos, Images)
                    ArrayList<ArrayList<ItemClass_File>> alAllFileItemsInMediaFolder = new ArrayList<>();
                    iProgressNumerator = 0;
                    iProgressDenominator = alsFolderNamesInUse.size();
                    int iTotalFiles = 0;
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
                                CATALOG_VERIFICATION_ACTION_RESPONSE);


                        //Assemble a Uri for the folder:
                        String sFolderUri = GlobalClass.gUriCatalogFolders[giMediaCategory] +
                                GlobalClass.gsFileSeparator + sFolderName;
                        Uri uriFolderUri = Uri.parse(sFolderUri);

                        //Get all files in the folder:
                        ArrayList<ItemClass_File> alicf_FileItemsInFolder = GlobalClass.GetDirectoryFileNamesData(uriFolderUri);

                        //Look for items in this folder that are m3u8 folders (subfolder storage of multiple related files):
                        ArrayList<String> alsSubfolders = GlobalClass.GetDirectorySubfolderNames(uriFolderUri);
                        if(alsSubfolders.size() > 0){
                            int iSubFolderCount = 0;
                            for(String sSubfolderName: alsSubfolders){

                                iSubFolderCount++;
                                globalClass.BroadcastProgress(false, "",
                                        true, iProgressBarValue,
                                        true, "Indexing folder " + iProgressNumerator +
                                                " [subfolder " + iSubFolderCount + "/" + alsSubfolders.size() + "] of " + iProgressDenominator,
                                        CATALOG_VERIFICATION_ACTION_RESPONSE);

                                //For each folder, check to see if it has a .m3u8 file:
                                String sSubfolderUri = GlobalClass.gUriCatalogFolders[giMediaCategory] +
                                        GlobalClass.gsFileSeparator + sFolderName +
                                        GlobalClass.gsFileSeparator + sSubfolderName;
                                Uri uriSubfolderUri = Uri.parse(sSubfolderUri);
                                ArrayList<ItemClass_File> alicf_SubfolderItems = GlobalClass.GetDirectoryFileNamesData(uriSubfolderUri);
                                if(alicf_SubfolderItems.size() > 0){
                                    for(ItemClass_File icf: alicf_SubfolderItems){
                                        if(icf.sFileOrFolderName.endsWith("m3u8")){
                                            //Add this .m3u8 item to be considered as an item:
                                            ItemClass_File icf_Temp = new ItemClass_File(ItemClass_File.TYPE_M3U8, icf.sFileOrFolderName);
                                            icf_Temp.sMediaFolderRelativePath = sFolderName + GlobalClass.gsFileSeparator + sSubfolderName;
                                            alicf_FileItemsInFolder.add(icf_Temp);
                                        }
                                    }
                                }
                            }
                        }

                        if(alicf_FileItemsInFolder.size() > 0) {
                            for (ItemClass_File icf : alicf_FileItemsInFolder) {
                                if(icf.iTypeFileFolderURL != ItemClass_File.TYPE_M3U8) {
                                    icf.sMediaFolderRelativePath = sFolderName;
                                }
                            }
                            iTotalFiles += alicf_FileItemsInFolder.size();
                            alAllFileItemsInMediaFolder.add(alicf_FileItemsInFolder);
                        }



                    }

                    //Prepare fast lookup of files identified in the catalog database file (currently in memory):
                    Set<String> setCatalogRelativePaths = new HashSet<>();
                    for (Map.Entry<String, ItemClass_CatalogItem> entry : GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()) {
                        String sRelativePath = entry.getValue().sFolderRelativePath +
                                GlobalClass.gsFileSeparator + entry.getValue().sFilename;
                        if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                            if (entry.getValue().iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                                //An M3U8 item may need special treatment.
                                sRelativePath = entry.getValue().sFolderRelativePath +
                                        GlobalClass.gsFileSeparator + entry.getValue().sFilename;
                            }
                        }

                        setCatalogRelativePaths.add(sRelativePath);
                    }

                    ArrayList<ItemClass_File> alOrphanedFileList = new ArrayList<>();

                    //Loop through all of the folders in memory in the media category storage location,
                    // then loop through all of the file items (and m3u8 file items) to see if there
                    // is a match in the catalog:
                    iProgressNumerator = 0;
                    iProgressDenominator = iTotalFiles;
                    int iTotalOrphanedFileCount = 0;
                    for(ArrayList<ItemClass_File> alFilesInFolder: alAllFileItemsInMediaFolder){
                        //Loop through all of the files stored in those subfolders:

                        String sFolderName = alFilesInFolder.get(0).sMediaFolderRelativePath;
                        sMessage = "\nReviewing folder: " + sFolderName + "...\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);

                        String sFolderUri;

                        int iFileItemCountOrphanedInThisFolder = 0;
                        int iFileItemCountOrphanedInThisFolderButNameMatched = 0;
                        int iFileItemCountOrphanedMissingMatch = 0;
                        for(ItemClass_File icf_FileItem: alFilesInFolder){ //These are the files in the folders in storage.

                            if (GlobalClass.aiCatalogVerificationRunning.get() == GlobalClass.STOP_REQUESTED) {
                                sMessage = "'STOP' command received from user.\n";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);
                                break;
                            }

                            iProgressNumerator++;
                            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                            globalClass.BroadcastProgress(false, "",
                                    true, iProgressBarValue,
                                    true, "Verifying item " + iProgressNumerator + " of " + iProgressDenominator + " files",
                                    CATALOG_VERIFICATION_ACTION_RESPONSE);

                            //Look for the folder and filename combination in memory:
                            String sItemRelativePath = icf_FileItem.sMediaFolderRelativePath +
                                    GlobalClass.gsFileSeparator + icf_FileItem.sFileOrFolderName; //This will include a .m3u8 file path and file name, if applicable.

                            boolean bIsM3U8 = false;
                            if(icf_FileItem.sFileOrFolderName.endsWith("m3u8")){
                                bIsM3U8 = true;
                            }

                            if (!setCatalogRelativePaths.contains(sItemRelativePath)) {

                                //Item is not identified in memory. Record the occurence so that the
                                //  user can review them in the next step. This requires creating an
                                //  array of file items to pass to a preview activity.

                                iTotalOrphanedFileCount++;

                                sMessage = "\n" + iTotalOrphanedFileCount + ".\tItem not found in catalog:\t" +
                                        GlobalClass.gsCatalogFolderNames[giMediaCategory] +
                                        GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + sItemRelativePath) + "\n";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);


                                //Create a file item for greater analysis outside of this worker:
                                //Initialize file item data:
                                //Determine file extension:
                                String sfileExtension = icf_FileItem.sFileOrFolderName.contains(".") ? icf_FileItem.sFileOrFolderName.substring(icf_FileItem.sFileOrFolderName.lastIndexOf(".")) : "";
                                if (!sfileExtension.matches(".+")) {
                                    icf_FileItem.sExtension = "";
                                } else {
                                    icf_FileItem.sExtension = sfileExtension;
                                }

                                icf_FileItem.sUri = GlobalClass.gUriCatalogFolders[giMediaCategory] +
                                        GlobalClass.gsFileSeparator + sItemRelativePath; //This will include a .m3u8 file path and file name, if applicable.

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

                                    } catch (Exception ignored) {
                                    }
                                }

                                icf_FileItem.sWidth = sWidth;
                                icf_FileItem.sHeight = sHeight;

                                iFileItemCountOrphanedInThisFolder++;

                                //Check to see if this filename matches an item in the catalog that is missing it's media:
                                boolean bOrphanedFileAssociatedWithMissingCatItem = false;
                                Set<String> ssCatItemsMissingMediaFinds = new HashSet<>();
                                if(gtmCatalogItemsMissing.size() > 0){
                                    //Compare to catalog items missing items.
                                    for(Map.Entry<String, ItemClass_CatalogItem> entry: gtmCatalogItemsMissing.entrySet()){
                                        if(entry.getValue().sFilename.equals(icf_FileItem.sFileOrFolderName)){
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
                                            if(icf_FileItem.alsOrphanAssociatedCatalogItemIDs == null){
                                                icf_FileItem.alsOrphanAssociatedCatalogItemIDs = new ArrayList<>();
                                            }
                                            icf_FileItem.alsOrphanAssociatedCatalogItemIDs.add(entry.getKey());
                                        }
                                    }
                                }
                                if(!bOrphanedFileAssociatedWithMissingCatItem){
                                    sMessage = "\t\t\tThis orphaned file is not associated with any catalog item missing its media.\n";/* +
                                               "\t\t\t\tThe catalog item may have been deleted with a failed file-delete operation,\n" +
                                               "\t\t\t\ta backup of the catalog database file may have occurred which does not have\n" +
                                               "\t\t\t\trecord of a recently-imported item, or a user or other program may have \n" +
                                               "\t\t\t\tplaced a file in the location.\n";*/
                                    SendLogLine(sMessage);
                                    sbLogLines.append(sMessage);
                                }

                                //Check to see if there is an exact filename match in the catalog (including items that are NOT missing their media):
                                int iFileNameMatchCount = 0;
                                boolean bFoundMatch = false;
                                ArrayList<String> alsFileNameMatchPaths = new ArrayList<>();
                                for (Map.Entry<String, ItemClass_CatalogItem> entry : GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()) {
                                    if(entry.getValue().sFilename.equals(icf_FileItem.sFileOrFolderName)){
                                        iFileNameMatchCount++;
                                        if(!bFoundMatch) {
                                            iFileItemCountOrphanedInThisFolderButNameMatched++;
                                            icf_FileItem.bOrphanAssociatedWithCatalogItem = true;
                                            bFoundMatch = true;
                                        }
                                        if(ssCatItemsMissingMediaFinds.contains(entry.getKey())) {
                                            //We have already notified the user that the orphaned file matches this catalog items' media, and that
                                            //  this particular catalog item is in fact missing it's media at it's intended location.
                                            //  Don't record this item for display to the user a second time.
                                            continue;
                                        }
                                        if(icf_FileItem.alsOrphanAssociatedCatalogItemIDs == null){
                                            icf_FileItem.alsOrphanAssociatedCatalogItemIDs = new ArrayList<>();
                                        }
                                        icf_FileItem.alsOrphanAssociatedCatalogItemIDs.add(entry.getKey());
                                        String sMatchPath = entry.getValue().sFolderRelativePath + GlobalClass.gsFileSeparator + entry.getValue().sFilename;
                                        alsFileNameMatchPaths.add(sMatchPath);
                                    }
                                }
                                icf_FileItem.iFileNameDuplicationCount = iFileNameMatchCount;
                                if(bFoundMatch){
                                    //Provide a message to the user that the file name was matched with one or more catalog file items:
                                    for(String sMatchPath: alsFileNameMatchPaths){
                                        sMessage = "\t\t\tFile name found for a catalog item at location:\t" +
                                                GlobalClass.gsCatalogFolderNames[giMediaCategory] +
                                                GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + sMatchPath) + "\n";
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);
                                    }
                                }


                                //Add the ItemClass_File to the ArrayList:
                                alOrphanedFileList.add(icf_FileItem);

                            } // End if item not identified in memory.

                        } // End looping through files in folder.

                        //If there were orphaned files in this folder, display a summary for the folder:
                        sMessage = "\n------------------------------------------------------\n" +
                                     "----------   Folder Summary   ------------------------\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);

                        if(iFileItemCountOrphanedInThisFolder > 0) {
                            sMessage = "\nThere were " + iFileItemCountOrphanedInThisFolder + " orphaned files in this folder out of " + alFilesInFolder.size() + " files.\n";
                            SendLogLine(sMessage);
                            sbLogLines.append(sMessage);

                            if(iFileItemCountOrphanedInThisFolderButNameMatched > 0){
                                sMessage = "Some of the file names matched file names with catalog items. " + iFileItemCountOrphanedInThisFolderButNameMatched + " matches were found.\n";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);
                            }

                            if(iFileItemCountOrphanedMissingMatch > 0){
                                sMessage = "\t\t" + iFileItemCountOrphanedMissingMatch + " of the orphaned files matched catalog items' missing media file names.\n";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);
                            } else {
                                sMessage = "None of these orphaned file names matched file names with catalog items which are missing their media.\n";
                                SendLogLine(sMessage);
                                sbLogLines.append(sMessage);
                            }
                        }

                        if(alOrphanedFileList.size() > GlobalClass.CATALOG_ANALYSIS_APPROX_MAX_RESULTS) break;

                    } //End looping through folders

                    sMessage = "\n------------------------------------------------------\n" +
                                 "----------   Orphaned File Analysis Summary   --------\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);

                    sMessage = "\nOrphaned file analysis tested " + iProgressNumerator + " file items of " + iProgressDenominator + " total files.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                    sMessage = "A total of " + alOrphanedFileList.size() + " orphaned files were found.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);


                    if(alOrphanedFileList.size() > GlobalClass.CATALOG_ANALYSIS_APPROX_MAX_RESULTS){
                        sMessage = "\nAnalysis result limit set to " + GlobalClass.CATALOG_ANALYSIS_APPROX_MAX_RESULTS + ", but continues until the end of a folder is reached.\n" +
                                "For more results, resolve the existing orphaned files and run the analysis again.\n\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);
                    }


                    // =============================================================================
                    // ============================ System repair ==================================
                    // =============================================================================

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

                    //Broadcast a message with the list of files so that they can be presented to the user:
                    Intent broadcastIntent_GetDirectoryContentsResponse = new Intent();
                    broadcastIntent_GetDirectoryContentsResponse.putExtra(GlobalClass.EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, true);
                    broadcastIntent_GetDirectoryContentsResponse.putExtra(GlobalClass.EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE, alOrphanedFileList);
                    broadcastIntent_GetDirectoryContentsResponse.setAction(CATALOG_VERIFICATION_ACTION_RESPONSE);
                    broadcastIntent_GetDirectoryContentsResponse.addCategory(Intent.CATEGORY_DEFAULT);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);

                }



            }

            bwLogFile.write(sbLogLines.toString());
            bwLogFile.flush();
            bwLogFile.close();
            osLogFile.flush();
            osLogFile.close();

        } catch (Exception ignored){

        }



        sMessage = "Catalog Verification complete.";
        LogThis("doWork()", sMessage, null);
        GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.FINISHED);
        globalClass.BroadcastProgress(false, "",
                true, 100,
                true, "Complete.",
                CATALOG_VERIFICATION_ACTION_RESPONSE);
        return Result.success();
    }



    private void SendLogLine(String sLogLine){
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(CATALOG_VERIFICATION_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(GlobalClass.LOG_LINE_STRING, sLogLine);
        broadcastIntent.putExtra(GlobalClass.UPDATE_LOG_BOOLEAN, true);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        globalClass.BroadcastProgress(true, sMessage,
                false, 0,
                false, "",
                CATALOG_VERIFICATION_ACTION_RESPONSE);
        Log.d("Worker_Catalog_Verification:" + sRoutine, sMessage);
    }




}
