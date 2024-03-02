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
import java.util.Map;
import java.util.TreeMap;

public class Worker_Catalog_Analysis extends Worker {

    public static final String TAG_WORKER_CATALOG_VERIFICATION = "com.agcurations.aggallermanager.tag_worker_catalog_verification";

    public static final String CATALOG_VERIFICATION_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_VERIFICATION_ACTION_RESPONSE";

    int giMediaCategory;
    int giAnalysisType;

    public Worker_Catalog_Analysis(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
        giAnalysisType = getInputData().getInt(ViewModel_CatalogAnalysis.EXTRA_ANALYSIS_TYPE, -1);

    }

    @NonNull
    @Override
    public Result doWork() {

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

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
                TreeMap<String, ItemClass_CatalogItem> tmCatalogItemsMissing = new TreeMap<>();
                for (Map.Entry<String, ItemClass_CatalogItem> entry : GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()) {

                    ItemClass_CatalogItem icci = entry.getValue();

                    iProgressNumerator++;
                    iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                    globalClass.BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "Verifying item " + iProgressNumerator + "/" + iProgressDenominator + "...",
                            CATALOG_VERIFICATION_ACTION_RESPONSE);


                    String sCatalogItemUri = "";
                    if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                        if (icci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                            //A folder containing files related to this M3U8:
                            sCatalogItemUri = GlobalClass.gsUriAppRootPrefix
                                    + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                    + GlobalClass.gsFileSeparator + icci.sFolderRelativePath;
                        } else {
                            //A single file:
                            sCatalogItemUri = GlobalClass.gsUriAppRootPrefix
                                    + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                    + GlobalClass.gsFileSeparator + icci.sFolderRelativePath
                                    + GlobalClass.gsFileSeparator + icci.sFilename;
                        }
                    } else if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                        //A single file:
                        sCatalogItemUri = GlobalClass.gsUriAppRootPrefix
                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                + GlobalClass.gsFileSeparator + icci.sFolderRelativePath
                                + GlobalClass.gsFileSeparator + icci.sFilename;
                    } else if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                        //A folder containing files related to this comic:
                        sCatalogItemUri = GlobalClass.gsUriAppRootPrefix
                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                + GlobalClass.gsFileSeparator + icci.sFolderRelativePath;
                    }
                    Uri uriCatalogItemUri = Uri.parse(sCatalogItemUri);

                    if (!GlobalClass.CheckIfFileExists(uriCatalogItemUri)) {
                        boolean bMarkItemAsMissing = true;
                        sMessage = "Item with ID " + icci.sItemID + " not found. Expected to be found at the following location:\n"
                                + GlobalClass.cleanHTMLCodedCharacters(sCatalogItemUri) + "\n";
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
                            tmCatalogItemsMissing.put(entry.getKey(), entry.getValue());
                        }


                    }

                    if (GlobalClass.aiCatalogVerificationRunning.get() == GlobalClass.STOP_REQUESTED) {
                        sMessage = "'STOP' command received from user.\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);
                        break;
                    }

                }

                sMessage = "Scanned " + iProgressNumerator + "/" + iProgressDenominator + " items in the " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog.\n";
                if (tmCatalogItemsMissing.size() == 0) {
                    sMessage = sMessage + "Of the " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog items that were scanned, no missing media identified.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                } else {
                    sMessage = sMessage + "Of the " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog items that were scanned, " + tmCatalogItemsMissing.size() +
                            " catalog items were found to have missing media. Those missing media include the following IDs: \n";
                    StringBuilder sbMissingMediaCatIDs = new StringBuilder();
                    for (Map.Entry<String, ItemClass_CatalogItem> entry : tmCatalogItemsMissing.entrySet()) {
                        sbMissingMediaCatIDs.append(entry.getKey()).append("\n");
                    }
                    sMessage = sMessage + sbMissingMediaCatIDs;
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                }

                /*if (bTrimMissingCatalogItems) {
                    //Remove from memory and update the catalog file any catalog items whose files were not found:
                    for (Map.Entry<String, ItemClass_CatalogItem> entry : tmCatalogItemsMissing.entrySet()) {
                        gtmCatalogLists.get(giMediaCategory).remove(entry.getKey());
                    }
                    CatalogDataFile_UpdateCatalogFile(giMediaCategory, "Updating catalog after content trim...");
                }*/

                //End search for catalog items with missing files.

            } else {
                //Analyze orphaned files:

                if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                    //Look for orphaned files:
                    sMessage = "Looking for orphaned files...\n";
                    LogThis("doWork()", sMessage, null);

                    iProgressDenominator = GlobalClass.gtmCatalogLists.get(giMediaCategory).size();

                    //Start with a listing of all folders in the selected media folder:
                    ArrayList<String> alsFolderNamesInUse = GlobalClass.GetDirectorySubfolderNames(GlobalClass.gUriCatalogFolders[giMediaCategory]);

                    TreeMap<String, String> tmCatalogRelativePaths = new TreeMap<>();
                    //Prepare fast lookup:
                    for (Map.Entry<String, ItemClass_CatalogItem> entry : GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()) {
                        if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                            String sRelativePath = entry.getValue().sFolderRelativePath +
                                    GlobalClass.gsFileSeparator + entry.getValue().sFilename;
                            tmCatalogRelativePaths.put(sRelativePath, entry.getValue().sFilename);
                        }
                    }

                    ArrayList<ItemClass_File> alOrphanedFileList = new ArrayList<>();

                    for (String sFolderName : alsFolderNamesInUse) {

                        sMessage = "Reviewing folder: " + sFolderName + "...\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);

                        if (GlobalClass.aiCatalogVerificationRunning.get() == GlobalClass.STOP_REQUESTED) {
                            sMessage = "'STOP' command received from user.\n";
                            SendLogLine(sMessage);
                            sbLogLines.append(sMessage);
                            break;
                        }

                        if (sFolderName.equals(GlobalClass.gsImageDownloadHoldingFolderName)) {
                            continue;
                        }

                        //Assemble a Uri for the folder:
                        String sFolderUri = GlobalClass.gUriCatalogFolders[giMediaCategory] +
                                GlobalClass.gsFileSeparator + sFolderName;
                        Uri uriFolderUri = Uri.parse(sFolderUri);

                        if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {

                            ArrayList<ItemClass_File> alicf_FileItemsInFolder = GlobalClass.GetDirectoryFileNamesData(uriFolderUri);
                            if(alicf_FileItemsInFolder != null) {
                                int iFileItemCountOrphanedInThisFolder = 0;
                                int iFileItemCountOrphanedInThisFolderButNameMatched = 0;
                                for (ItemClass_File icf_FileItem : alicf_FileItemsInFolder) {
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
                                            true, "Verifying item " + iProgressNumerator + " of " + iProgressDenominator + " known catalog items...",
                                            CATALOG_VERIFICATION_ACTION_RESPONSE);

                                    //Look for the folder and filename combination in memory:
                                    String sItemRelativePath = sFolderName +
                                            GlobalClass.gsFileSeparator + icf_FileItem.sFileOrFolderName;
                                    if (!tmCatalogRelativePaths.containsKey(sItemRelativePath)) {
                                        //Item is not identified in memory. Note the occurrence:
                                        sMessage = "Item not found in catalog: " + GlobalClass.gsCatalogFolderNames[giMediaCategory] +
                                                "/" + GlobalClass.cleanHTMLCodedCharacters(sItemRelativePath) + "\n";
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);

                                        //create the file model and initialize:
                                        String sfileExtension = icf_FileItem.sFileOrFolderName.contains(".") ? icf_FileItem.sFileOrFolderName.substring(icf_FileItem.sFileOrFolderName.lastIndexOf(".")) : "";
                                        if (!sfileExtension.matches(".+")) {
                                            icf_FileItem.sExtension = "";
                                        } else {
                                            icf_FileItem.sExtension = sfileExtension;
                                        }

                                        icf_FileItem.sUri = sFolderUri +
                                                GlobalClass.gsFileSeparator + icf_FileItem.sFileOrFolderName;
                                        icf_FileItem.lVideoTimeInMilliseconds = 0;

                                        icf_FileItem.bMetadataDetected = false;
                                        String sWidth = "";  //We are not doing math with the width and height. Therefore no need to convert to int.
                                        String sHeight = "";
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

                                        icf_FileItem.sWidth = sWidth;
                                        icf_FileItem.sHeight = sHeight;

                                        iFileItemCountOrphanedInThisFolder++;

                                        //Check to see if there is an exact filename match in the catalog:
                                        if(tmCatalogRelativePaths.containsValue(icf_FileItem.sFileOrFolderName)){
                                            int iFileNameMatchCount = 0;
                                            for(Map.Entry<String, String> entryCatalogRelativePath: tmCatalogRelativePaths.entrySet()){
                                                if(entryCatalogRelativePath.getValue().equals(icf_FileItem.sFileOrFolderName)){
                                                    iFileNameMatchCount++;
                                                    iFileItemCountOrphanedInThisFolderButNameMatched++;
                                                }
                                            }
                                            icf_FileItem.iFileNameDuplicationCount = iFileNameMatchCount;
                                        }

                                        //Add the ItemClass_File to the ArrayList:
                                        alOrphanedFileList.add(icf_FileItem);

                                    } // End if item not identified in memory.

                                } // End looping through files in folder.

                                if((iFileItemCountOrphanedInThisFolder == iFileItemCountOrphanedInThisFolderButNameMatched) &&
                                        (iFileItemCountOrphanedInThisFolder == alicf_FileItemsInFolder.size())){
                                    sMessage = "\tAll items in this folder appear to have the same names as certain items in the " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog.\n\n";
                                    SendLogLine(sMessage);
                                    sbLogLines.append(sMessage);
                                } else {
                                    if(iFileItemCountOrphanedInThisFolder > 0) {
                                        String[] sPluarality = {"were", "files"};
                                        if(iFileItemCountOrphanedInThisFolder == 1){
                                            sPluarality = new String[]{"was", "file"};
                                        }
                                        sMessage = "\tThere " + sPluarality[0] + " " + iFileItemCountOrphanedInThisFolder + " orphaned " + sPluarality[1] + " in this folder out of " + alicf_FileItemsInFolder.size() + " files.\n" +
                                                "\t" + iFileItemCountOrphanedInThisFolderButNameMatched + " of those orphaned files matched a file name of an item in the " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog.\n\n";
                                        SendLogLine(sMessage);
                                        sbLogLines.append(sMessage);
                                    }
                                }
                            } //End if arraylist is not null.
                        } //End if verifying images.
                        if(alOrphanedFileList.size() > GlobalClass.CATALOG_ANALYSIS_APPROX_MAX_RESULTS) break;
                    } //End looping through catalog subfolders.

                    sMessage = "\nOrphaned file analysis observed " + iProgressNumerator + " file items for " + iProgressDenominator + " catalog items.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                    sMessage = "A total of " + alOrphanedFileList.size() + " files were not identified as belonging to a catalog item.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                    if(alOrphanedFileList.size() > GlobalClass.CATALOG_ANALYSIS_APPROX_MAX_RESULTS){
                        sMessage = "Analysis result limit set to " + GlobalClass.CATALOG_ANALYSIS_APPROX_MAX_RESULTS + ", but continues until the end of a folder is reached.\n" +
                                  "For more results, resolve the existing orphaned files and run the analysis again.\n";
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
            } //End analysis of orphaned files.

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
