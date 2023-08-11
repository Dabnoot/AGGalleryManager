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
import java.util.Map;
import java.util.TreeMap;

public class Worker_Catalog_Verification extends Worker {

    public static final String TAG_WORKER_CATALOG_VERIFICATION = "com.agcurations.aggallermanager.tag_worker_catalog_verification";

    public static final String CATALOG_VERIFICATION_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_VERIFICATION_ACTION_RESPONSE";

    int giMediaCategory;

    public Worker_Catalog_Verification(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);

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

                if(GlobalClass.aiCatalogVerificationRunning.get() == GlobalClass.STOP_REQUESTED){
                    sMessage = "'STOP' command received from user.\n";
                    SendLogLine(sMessage);
                    sbLogLines.append(sMessage);
                    break;
                }

            }

            sMessage = "Scanned " + iProgressNumerator + "/" + iProgressDenominator + " items in the " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog.\n";
            if(tmCatalogItemsMissing.size() == 0){
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
