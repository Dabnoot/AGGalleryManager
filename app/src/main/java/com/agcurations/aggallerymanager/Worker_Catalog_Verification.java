package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.exoplayer2.util.MimeTypes;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Worker_Catalog_Verification extends Worker {

    public static final String TAG_WORKER_CATALOG_VERIFICATION = "com.agcurations.aggallermanager.tag_worker_catalog_verification";

    public static final String CATALOG_VERIFICATION_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_VERIFICATION_ACTION_RESPONSE";

    int giMediaCategory;
    int giAnalysisType;

    public Worker_Catalog_Verification(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
        int iProgressBarValue = 0;

        String sAnalysisStartDateTime = GlobalClass.GetTimeStampFileSafe();
        BufferedWriter bwLogFile;
        OutputStream osLogFile;

        StringBuilder sbLogLines = new StringBuilder();

        String sLogFileName = sAnalysisStartDateTime + "_" + GlobalClass.gsCatalogFolderNames[giMediaCategory] + "CatalogVerification_.txt";
        Uri uriLogFile;
        try {
            uriLogFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, GlobalClass.gUriLogsFolder, MimeTypes.BASE_TYPE_TEXT, sLogFileName);
        } catch (FileNotFoundException e) {
            return Result.failure();
        }
        if(uriLogFile == null){
            return Result.failure();
        }
        try { //Required for the log file.
            osLogFile = GlobalClass.gcrContentResolver.openOutputStream(uriLogFile, "wt");
            if (osLogFile == null) {
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


                    String sUri = "";
                    if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                        if (icci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                            //A folder containing files related to this M3U8:
                            sUri = GlobalClass.gsUriAppRootPrefix
                                    + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                    + GlobalClass.gsFileSeparator + icci.sFolderRelativePath;
                        } else {
                            //A single file:
                            sUri = GlobalClass.gsUriAppRootPrefix
                                    + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                    + GlobalClass.gsFileSeparator + icci.sFolderRelativePath
                                    + GlobalClass.gsFileSeparator + icci.sFilename;
                        }
                    } else if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                        //A single file:
                        sUri = GlobalClass.gsUriAppRootPrefix
                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                + GlobalClass.gsFileSeparator + icci.sFolderRelativePath
                                + GlobalClass.gsFileSeparator + icci.sFilename;
                    } else if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                        //A folder containing files related to this comic:
                        sUri = GlobalClass.gsUriAppRootPrefix
                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[giMediaCategory]
                                + GlobalClass.gsFileSeparator + icci.sFolderRelativePath;
                    }
                    Uri uriItem = Uri.parse(sUri);

                    if (!GlobalClass.CheckIfFileExists(uriItem)) {
                        sMessage = "Item with ID " + icci.sItemID + " not found. Expected to be found in location "
                                + sUri + "\n";
                        SendLogLine(sMessage);
                        sbLogLines.append(sMessage);
                        tmCatalogItemsMissing.put(entry.getKey(), entry.getValue());
                        Log.d("AGGalleryManager", sMessage);
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
                    sMessage = sMessage + sbMissingMediaCatIDs.toString();
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

                    //Prepare fast lookup:
                    ArrayList<String> alsCatalogRelativePaths = new ArrayList<>();
                    for (Map.Entry<String, ItemClass_CatalogItem> entry : GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()) {
                        if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                            String sRelativePath = entry.getValue().sFolderRelativePath +
                                    GlobalClass.gsFileSeparator + entry.getValue().sFilename;
                            alsCatalogRelativePaths.add(sRelativePath);
                        }
                    }
                    ArrayList<String> alsOrphanedFiles = new ArrayList<>();
                    ArrayList<ItemClass_File> alOrphanedFileList = new ArrayList<>();

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

                        //Assemble a Uri for the folder:
                        String sFolderUri = GlobalClass.gUriCatalogFolders[giMediaCategory] +
                                GlobalClass.gsFileSeparator + sFolderName;
                        Uri uriFolderUri = Uri.parse(sFolderUri);

                        if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {

                            ArrayList<String> alsFileNamesInFolder = GlobalClass.GetDirectoryFileNames(uriFolderUri);
                            for (String sFileName : alsFileNamesInFolder) {
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
                                        GlobalClass.gsFileSeparator + sFileName;
                                if (!alsCatalogRelativePaths.contains(sItemRelativePath)) {
                                    //Item is not identified in memory. Note the occurrence:
                                    alsOrphanedFiles.add(sItemRelativePath);
                                    sMessage = "Item not found in catalog: " + GlobalClass.cleanHTMLCodedCharacters(sItemRelativePath) + "\n";
                                    SendLogLine(sMessage);
                                    sbLogLines.append(sMessage);

                                    //create the file model and initialize:
                                    ItemClass_File icfFileItem = new ItemClass_File(0, "");
                                    icfFileItem.sExtension = "";
                                    icfFileItem.lSizeBytes = 0;
                                    icfFileItem.dateLastModified = null;
                                    icfFileItem.bMetadataDetected = false;
                                    icfFileItem.sWidth = "";
                                    icfFileItem.sHeight = "";
                                    icfFileItem.sUri = "";
                                    icfFileItem.sMimeType = "";
                                    icfFileItem.lVideoTimeInMilliseconds = 0;

                                    //Add the ItemClass_File to the ArrayList:
                                    alOrphanedFileList.add(icfFileItem);

                                }

                            }
                        }

                    }

                    sMessage = "Orphaned file analysis observed " + iProgressNumerator + " file items for " + iProgressDenominator + " catalog items.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                    sMessage = "A total of " + alsOrphanedFiles.size() + " files were not identified as belonging to a catalog item.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);

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
        GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.STOPPED);
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
