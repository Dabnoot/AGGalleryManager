package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ParseException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Worker_DownloadPostProcessing  extends Worker {
    //This routine moves downloaded files from a temporary download folder to a more permanent
    //  designated location. The Android Download Manager will remove downloaded files from a device
    //  in an attempt to free space. This routine avoids that.
    //  Android also only allows downloads to an internal folder. This routine moves from internal,
    //  app-associated storage to a designated storage location defined by the user through the
    //  first-run behavior when configuring the storage location for the files.

    //Define string used to identify this worker type:
    public static final String WORKER_TAG_DOWNLOAD_POST_PROCESSING = "WORKER_TAG_DOWNLOAD_POST_PROCESSING";

    public static final String DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE";

    public static final String EXTRA_STRING_DOWNLOAD_POST_PROCESSING_ID = "com.agcurations.aggallermanager.extra_string_download_post_processing_id";

    public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";

    final static boolean DEBUG = false;

    //=========================
    // Define keys for arguments passed to this worker:
    public static final String KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS = "KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS";
    public static final String KEY_ARG_RELATIVE_PATH_TO_FOLDER = "KEY_ARG_RELATIVE_PATH_TO_FOLDER";
    public static final String KEY_ARG_MEDIA_CATEGORY = "KEY_ARG_MEDIA_CATEGORY";
    public static final String KEY_ARG_VIDEO_TYPE_SINGLE_M3U8 = "KEY_ARG_VIDEO_TYPE_SINGLE_M3U8";
    public static final String KEY_ARG_DOWNLOAD_IDS = "KEY_ARG_DOWNLOAD_IDS";
    public static final String KEY_ARG_ITEM_ID = "KEY_ARG_ITEM_ID";

    //=========================

    //public static final int DOWNLOAD_TYPE_SINGLE = 1;
    public static final int DOWNLOAD_TYPE_M3U8 = 2;

    String gsDLPostProcessingID = "";

    GlobalClass globalClass;

    String gsPathToMonitorForDownloads;                 //Location to monitor for download file(s)
    long[] glDownloadIDs;                               //Used to determine if a download ID in DownloadManager has status 'completed'.
    String gsItemID;
    String gsRelativeFolder;
    Uri uriDestinationFolder = null;
    boolean gbExpectDifferentFilesSameNames = false;
    int giMediaCategory;
    int giSingleOrM3U8 = -1;

    OutputStream gosLogFile = null;
    BufferedWriter gbwLogFile = null;
    boolean gbLogInUse = false;

    //=========================

    public Worker_DownloadPostProcessing(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {

        super(context, workerParams);

        globalClass = (GlobalClass) getApplicationContext();

        gsDLPostProcessingID = getInputData().getString(EXTRA_STRING_DOWNLOAD_POST_PROCESSING_ID);

        //Get path to monitor for downloads for the purpose of deleting a download folder when
        //  empty:
        gsPathToMonitorForDownloads = getInputData().getString(KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS);

        //Get the array of download IDs to query from the DownloadManager:
        glDownloadIDs = getInputData().getLongArray(KEY_ARG_DOWNLOAD_IDS);

        //Get a catalog item ID. Not required or valid for downloads related to holding folder (images only).
        gsItemID = getInputData().getString(KEY_ARG_ITEM_ID);

        //Get the media category for the download.
        giMediaCategory = getInputData().getInt(KEY_ARG_MEDIA_CATEGORY, -1);

        gsRelativeFolder = getInputData().getString(KEY_ARG_RELATIVE_PATH_TO_FOLDER); //If this is a comic, it will be the comic ID. If it is a video, it will be a sub folder.
        if(gsRelativeFolder != null){
            uriDestinationFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[giMediaCategory], gsRelativeFolder);
        } else {
            LogThis("Worker_DownloadPostProcessing Constructor:", "Relative path data not passed to worker.", null);
        }

        if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES){
            gsItemID = "Image"; //gsItemID is used in log name generation
            gbExpectDifferentFilesSameNames = true; //Some image files that are downloaded could
            // have the same name. Such as 001.jpg. Need to create a unique destination file name if
            // such a file already exists in the destination folder.
        }
    }

    @NonNull
    @Override
    public Result doWork() {

        boolean bDebug = DEBUG;

        String sMessage;

        long lFileCountProgressNumerator = 0L;
        long lFileCountProgressDenominator;
        int iFileCountProgressBarValue = 0;
        int iSizeProgressBarValue = 0;
        int iProgressBarValue;
        TreeMap<Long, DownloadItem> tmDLSizes = new TreeMap<>();
        long lSizeProgressNumerator;
        long lSizeProgressDenominator;

        if(!GlobalClass.CheckIfFileExists(GlobalClass.gUriLogsFolder)){
            sMessage = "Logs folder missing. Restarting app should create the folder.";
            return Result.failure(DataErrorMessage(sMessage));
        }

        //Prepare log file:
        String sLogFileName = GlobalClass.GetTimeStampFileSafe() + "_DLTransferLog_" + gsItemID + ".txt";

        TreeMap<String, ItemClass_File> tmicf_ItemsToAddToFileIndex = new TreeMap<>();
        String sThumbnailUri = "";

        try {
            if(bDebug) {
                Uri uriLog = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, GlobalClass.gUriLogsFolder, GlobalClass.BASE_TYPE_TEXT, sLogFileName);
                if(uriLog == null){
                    bDebug = false;
                } else {
                    gosLogFile = GlobalClass.gcrContentResolver.openOutputStream(uriLog, "wt");
                    gbwLogFile = new BufferedWriter(new OutputStreamWriter(gosLogFile));
                    gbLogInUse = true;
                }
            }

            //Validate data
            if(glDownloadIDs.length == 0){
                sMessage = "Download ID(s) missing.";
                return Result.failure(LogReturnWithFailure(sMessage));
            }
            lFileCountProgressDenominator = glDownloadIDs.length;

            Set<Long> setDownloadIDs = new TreeSet<>();
            for(long l: glDownloadIDs){
                setDownloadIDs.add(l);
                DownloadItem dli = new DownloadItem();
                dli.lDownloadID = l;
                tmDLSizes.put(l, dli);
            }

            if( gsPathToMonitorForDownloads.equals("")){
                sMessage = "No path to monitor for downloads provided.";
                return Result.failure(LogReturnWithFailure(sMessage));
            }

            File fThisDownloadFolder = new File(gsPathToMonitorForDownloads);
            if(!fThisDownloadFolder.exists()){
                sMessage = "Folder to monitor for downloads does not exist: " + gsPathToMonitorForDownloads;
                return Result.failure(DataErrorMessage(sMessage));
            }

            //Monitor the location for file downloads' completion:
            int iElapsedWaitTime = 0;
            int iWaitDuration = 500; //milliseconds
            boolean bPaused = false;
            String sDownloadFailedReason = "";
            String sDownloadPausedReason = "";


            sMessage = "Waiting for download to complete, a maximum of " + (GlobalClass.DOWNLOAD_WAIT_TIMEOUT / 1000) + " seconds.";
            if(bDebug) gbwLogFile.write(sMessage + "\n");
            if(bDebug) gbwLogFile.flush();

            DownloadManager downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);

            float fPercentConcernTimeElapsed;

            globalClass.BroadcastProgress(true, "\nBegin monitoring for downloads...\n",
                    true, 0,
                    true, lFileCountProgressNumerator + "/" + lFileCountProgressDenominator + " downloads completed.",
                    DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);

            boolean bDLAwaitingRetry = false;

            double dWaitTimeStart = GlobalClass.GetTimeStampDouble();

            //Query for remaining downloads:
            //Create array of download IDs to query:
            long[] lDownloadIDs = new long[setDownloadIDs.size()];
            lDownloadIDs = setDownloadIDs.stream().mapToLong(Long::longValue).toArray();

            DownloadManager.Query dmQuery = new DownloadManager.Query();
            dmQuery.setFilterById(lDownloadIDs);

            boolean bRecalcSizeProgress;

            long lDefaultDownloadSize;
            int iDLCountWithData;


            while ((iElapsedWaitTime < GlobalClass.DOWNLOAD_WAIT_TIMEOUT) && setDownloadIDs.size() > 0) {

                try {
                    Thread.sleep(iWaitDuration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!bPaused) {
                    iElapsedWaitTime += iWaitDuration;
                } else {
                    iElapsedWaitTime += (int) (iWaitDuration / 10.0); //Wait longer if a download is paused.
                }
                if(bDebug) gbwLogFile.write(".");
                if(bDebug) gbwLogFile.flush();

                //Prepare DL Size Defaults:
                lDefaultDownloadSize = 0;
                iDLCountWithData = 0;
                boolean bDefaultDLSizeFirstApplicationComplete = false;
                for( Map.Entry<Long, DownloadItem> entry: tmDLSizes.entrySet()){
                    if(entry.getValue().iDownloadStatus != DownloadManager.STATUS_PENDING){
                        lDefaultDownloadSize += entry.getValue().lBytesDownloadTotalSize;
                        iDLCountWithData++;
                    }
                }
                if(iDLCountWithData > 0) {
                    lDefaultDownloadSize /= iDLCountWithData;
                }
                for( Map.Entry<Long, DownloadItem> entry: tmDLSizes.entrySet()){
                    if(entry.getValue().iDownloadStatus == DownloadManager.STATUS_PENDING){
                        entry.getValue().lBytesDownloadTotalSize = lDefaultDownloadSize;
                    }
                }


                //Execute the query for download data:
                Cursor cursor = downloadManager.query(dmQuery);

                if(cursor.moveToFirst()) {
                    do {
                        bRecalcSizeProgress = false;
                        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = cursor.getInt(columnIndex);
                        int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                        int iReasonID = cursor.getInt(columnReason);
                        int iLocalURIIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        String sLocalURI = cursor.getString(iLocalURIIndex);
                        int iDownloadURI = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
                        String sDownloadURI = cursor.getString(iDownloadURI);
                        int iDownloadID = cursor.getColumnIndex((DownloadManager.COLUMN_ID));
                        long lDownloadID = cursor.getLong(iDownloadID);
                        int iBytes_Downloaded_Column = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        String sBytes_Downloaded = "";
                        if(iBytes_Downloaded_Column > -1){
                            sBytes_Downloaded = cursor.getString(iBytes_Downloaded_Column);
                        }
                        int iBytes_Total_Size_Column = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES); //NOTE: This only holds total size once a download has started transferring data.
                        String sBytes_Total_Size = "";
                        if(iBytes_Total_Size_Column > -1) {
                            sBytes_Total_Size = cursor.getString(iBytes_Total_Size_Column);
                        }

                        if(tmDLSizes.containsKey(lDownloadID)) { //preventing null warnings
                            DownloadItem dli = tmDLSizes.get(lDownloadID);
                            if(dli != null) {
                                if (!sBytes_Total_Size.equals("")) {
                                    try {
                                        long lLastSize = dli.lBytesDownloaded;
                                        dli.lBytesDownloadTotalSize = Long.parseLong(sBytes_Total_Size);
                                        dli.lBytesDownloaded = Long.parseLong(sBytes_Downloaded);
                                        if(lLastSize != dli.lBytesDownloaded) {
                                            bRecalcSizeProgress = true;
                                        }

                                        if(!bDefaultDLSizeFirstApplicationComplete){
                                            //If this is the first download provided with a size, adjust all of the downloads' defaults to
                                            //  avoid an erroneous 100%. Use this first item's DL size as the initial default value.
                                            lDefaultDownloadSize = dli.lBytesDownloadTotalSize;
                                            for( Map.Entry<Long, DownloadItem> entry: tmDLSizes.entrySet()){
                                                if(entry.getValue().iDownloadStatus == DownloadManager.STATUS_PENDING){
                                                    entry.getValue().lBytesDownloadTotalSize = lDefaultDownloadSize;
                                                }
                                            }
                                            bDefaultDLSizeFirstApplicationComplete = true;
                                        }

                                    } catch (ParseException pe) {
                                        pe.printStackTrace();
                                    }
                                }
                            }
                        }
                        if(bRecalcSizeProgress){
                            //Recalc progress:
                            lSizeProgressNumerator = 0;
                            lSizeProgressDenominator = 0;
                            for (Map.Entry<Long, DownloadItem> entry : tmDLSizes.entrySet()) {
                                lSizeProgressNumerator += entry.getValue().lBytesDownloaded;
                                lSizeProgressDenominator += entry.getValue().lBytesDownloadTotalSize;
                            }
                            iSizeProgressBarValue = Math.round((lSizeProgressNumerator / (float) lSizeProgressDenominator) * 100);
                        }



                        bPaused = false;

                        switch (status) {
                            case DownloadManager.STATUS_FAILED:
                                switch (iReasonID) {
                                    case DownloadManager.ERROR_CANNOT_RESUME:
                                        sDownloadFailedReason = "ERROR_CANNOT_RESUME";
                                        break;
                                    case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                                        sDownloadFailedReason = "ERROR_DEVICE_NOT_FOUND";
                                        break;
                                    case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                                        sDownloadFailedReason = "ERROR_FILE_ALREADY_EXISTS";
                                        break;
                                    case DownloadManager.ERROR_FILE_ERROR:
                                        sDownloadFailedReason = "ERROR_FILE_ERROR";
                                        break;
                                    case DownloadManager.ERROR_HTTP_DATA_ERROR:
                                        sDownloadFailedReason = "ERROR_HTTP_DATA_ERROR";
                                        break;
                                    case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                                        sDownloadFailedReason = "ERROR_INSUFFICIENT_SPACE";
                                        break;
                                    case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                                        sDownloadFailedReason = "ERROR_TOO_MANY_REDIRECTS";
                                        break;
                                    case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                                        sDownloadFailedReason = "ERROR_UNHANDLED_HTTP_CODE";
                                        break;
                                    case DownloadManager.ERROR_UNKNOWN:
                                        sDownloadFailedReason = "ERROR_UNKNOWN";
                                        break;
                                    case 403:
                                        sDownloadFailedReason = "Web server is not reachable or access to the server has been denied.";
                                        break;
                                    case 404:
                                        sDownloadFailedReason = "File not found.";
                                        break;
                                }
                                sMessage = "\nThere was a problem with a download.";
                                sMessage = sMessage + "\n" + "Download: " + sDownloadURI;
                                sMessage = sMessage + "\n" + "Reason ID: " + iReasonID;
                                sMessage = sMessage + "\n" + "Reason text: " + sDownloadFailedReason;
                                lFileCountProgressNumerator++;
                                iFileCountProgressBarValue = Math.round((lFileCountProgressNumerator / (float) lFileCountProgressDenominator) * 100);
                                iProgressBarValue = 0;
                                if(iFileCountProgressBarValue == 0 && iSizeProgressBarValue > 0){
                                    iProgressBarValue = iSizeProgressBarValue;
                                } else if (iFileCountProgressBarValue >= iSizeProgressBarValue){
                                    iProgressBarValue = iSizeProgressBarValue;
                                }
                                iProgressBarValue = iSizeProgressBarValue;
                                if(iFileCountProgressBarValue == 100){
                                    iProgressBarValue = iFileCountProgressBarValue;
                                }
                                bRecalcSizeProgress = false;
                                globalClass.BroadcastProgress(true, sMessage + "\n",
                                        true, iProgressBarValue,
                                        true, lFileCountProgressNumerator + "/" + lFileCountProgressDenominator + " downloads accounted.",
                                        DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);
                                if(bDebug) gbwLogFile.write(sMessage + "\n\n");
                                if(bDebug) gbwLogFile.flush();

                                setDownloadIDs.remove(lDownloadID);

                                break;
                            case DownloadManager.STATUS_PAUSED:
                                bPaused = true;
                                switch (iReasonID) {
                                    case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                                        sDownloadPausedReason = "PAUSED_QUEUED_FOR_WIFI";
                                        break;
                                    case DownloadManager.PAUSED_UNKNOWN:
                                        sDownloadPausedReason = "PAUSED_UNKNOWN";
                                        break;
                                    case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                                        sDownloadPausedReason = "PAUSED_WAITING_FOR_NETWORK";
                                        break;
                                    case DownloadManager.PAUSED_WAITING_TO_RETRY:
                                        sDownloadPausedReason = "PAUSED_WAITING_TO_RETRY";
                                        break;
                                }
                                sMessage = "\n" + "Download paused: " + sDownloadURI;
                                sMessage = sMessage + "\n" + "Reason ID: " + iReasonID;
                                sMessage = sMessage + "\n" + "Reason text: " + sDownloadPausedReason;
                                bDLAwaitingRetry = true;
                                if(bDebug) gbwLogFile.write(sMessage + "\n\n");
                                if(bDebug) gbwLogFile.flush();

                                break;
                            case DownloadManager.STATUS_PENDING:
                                //No action.
                                //break;
                            case DownloadManager.STATUS_RUNNING:
                                //No action.
                                break;
                            case DownloadManager.STATUS_SUCCESSFUL:

                                //As of Android version 11, API level 30, One UI 3.1, the DownloadManager
                                //  will only store files in the onboard storage, or something like that.
                                //  Move those files over to the SD Card before processing.

                                sLocalURI = sLocalURI.replace("file://", "");
                                sLocalURI = URLDecoder.decode(sLocalURI, StandardCharsets.UTF_8.toString());
                                File fSource = new File(sLocalURI);

                                if(fSource.exists()) {
                                    String sFileName = fSource.getName();
                                    if(bDebug) gbwLogFile.write("Download completed: " + sFileName);



                                    //Ensure the destination filename is unique:
                                    if(gbExpectDifferentFilesSameNames){
                                        //  In this case, create a new filename with a
                                        //  filename modifier if there is a file of the same name:
                                        ArrayList<String> sExistingFilesInDestinationFolder = GlobalClass.GetDirectoryFileNames(uriDestinationFolder);
                                        if(sExistingFilesInDestinationFolder.size() > 0) {
                                            String sNew = sFileName;
                                            if(sNew.length() > 50){
                                                //Limit the length of the filename:
                                                String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sNew);
                                                if(sBaseAndExtension.length == 2) {
                                                    sNew = sBaseAndExtension[0].substring(0, 50 - sBaseAndExtension[1].length());
                                                    sNew = sNew + "." + sBaseAndExtension[1];
                                                }
                                            }
                                            sFileName = GlobalClass.getUniqueFileName(GlobalClass.gUriImageDownloadHoldingFolder, sNew, false);
                                        }
                                    } //else {
                                        //There should be no file of the same name in the destination.
                                        //  Check to see if such a file exists, and if so, don't
                                        //  execute a copy, merely delete the source if it is a
                                        //  move operation.

                                    //}
                                    Uri uriDestinationFile = GlobalClass.FormChildUri(uriDestinationFolder, sFileName);
                                    String sUserFriendlyDestinationFile = GlobalClass.cleanHTMLCodedCharacters(uriDestinationFile.toString());

                                    //Move the file to the destination folder:
                                    if (!GlobalClass.CheckIfFileExists(uriDestinationFile)) {
                                        //If the destination file does not exist...
                                        uriDestinationFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriDestinationFolder, GlobalClass.BASE_TYPE_TEXT, sFileName);
                                        if(uriDestinationFile != null) {
                                            try {
                                                InputStream inputStream;
                                                OutputStream osDestinationFile = GlobalClass.gcrContentResolver.openOutputStream(uriDestinationFile, "wt");
                                                if (osDestinationFile == null) {
                                                    sMessage = "Problem opening output stream.";
                                                    Log.d("CopyFile", sMessage);
                                                    if (bDebug) gbwLogFile.write(sMessage);
                                                    continue;
                                                }
                                                inputStream = Files.newInputStream(Paths.get(fSource.getPath()));
                                                byte[] buffer = new byte[100000];
                                                long lSize = 0;
                                                while ((inputStream.read(buffer, 0, buffer.length)) >= 0) {
                                                    osDestinationFile.write(buffer, 0, buffer.length);
                                                    lSize += buffer.length;
                                                }
                                                inputStream.close();
                                                osDestinationFile.flush();
                                                osDestinationFile.close();
                                                if (bDebug) gbwLogFile.write(" Copied to working folder.");

                                                //Update file indexing:
                                                //This is used in catalog analysis.
                                                // The treemap variable "gtmicf_AllFileItemsInMediaFolder" is an array list of 3 treemaps. Each
                                                // treemap key consists of
                                                // icf.sMediaFolderRelativePath + GlobalClass.gsFileSeparator + icf.sFileOrFolderName.
                                                // icf.sMediaFolderRelativePath excludes the media type folder. Media type folder is identified by the media type array index related to the TreeMap index.
                                                // Only update if the size of the tree is > 0, as it must be initiated by the Catalog Analysis worker.
                                                if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).size() > 0){


                                                    String sKey = gsRelativeFolder + GlobalClass.gsFileSeparator + sFileName;
                                                    ItemClass_File icf = new ItemClass_File(ItemClass_File.TYPE_FILE, sFileName);

                                                    //Get the mime type, date modified, file size:
                                                    icf.sUri = uriDestinationFile.toString();
                                                    icf.sUriParent = uriDestinationFolder.toString(); //Among other things, used to determine if pages belong to a comic or an M3U8 playlist.
                                                    icf.sMimeType = "";

                                                    icf.dateLastModified = new Date();

                                                    icf.lSizeBytes = lSize;

                                                    icf.sMediaFolderRelativePath = gsRelativeFolder;
                                                    if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                                                        if (giSingleOrM3U8 == DOWNLOAD_TYPE_M3U8) {
                                                            icf.iTypeFileFolderURL = ItemClass_File.TYPE_M3U8;
                                                        }
                                                    }

                                                    icf.sUriThumbnailFile = sThumbnailUri;

                                                    if (sThumbnailUri.equals("") &&
                                                            (icf.sFileOrFolderName.endsWith(".jpg") || icf.sFileOrFolderName.endsWith(".gpj") ||
                                                            icf.sFileOrFolderName.endsWith(".png") || icf.sFileOrFolderName.endsWith(".gnp"))) {
                                                        sThumbnailUri = icf.sUri;
                                                    }

                                                    tmicf_ItemsToAddToFileIndex.put(sKey, icf);

                                                }


                                            } catch (Exception e) {
                                                sMessage = fSource.getPath() + "\n" + e.getMessage();
                                                if (bDebug) gbwLogFile.write("Stream copy exception: " + sMessage + "\n");
                                            }
                                        }
                                    } //End if !FDestination.exists. If it does exist, we have already copied the file over.

                                    if (!fSource.delete()) {
                                        sMessage = "Could not delete source file after copy. Source: " + fSource.getAbsolutePath();
                                        if(bDebug) gbwLogFile.write("Download monitoring: " + sMessage + "\n");
                                    } else {
                                        if(bDebug) gbwLogFile.write(" Source file successfully deleted.");
                                    }

                                    lFileCountProgressNumerator++;
                                    iFileCountProgressBarValue = Math.round((lFileCountProgressNumerator / (float) lFileCountProgressDenominator) * 100);
                                    iProgressBarValue = 0;
                                    if(iFileCountProgressBarValue == 0 && iSizeProgressBarValue > 0){
                                        iProgressBarValue = iSizeProgressBarValue;
                                    } else if (iFileCountProgressBarValue >= iSizeProgressBarValue){
                                        iProgressBarValue = iSizeProgressBarValue;
                                    }
                                    iProgressBarValue = iSizeProgressBarValue;
                                    if(iFileCountProgressBarValue == 100){
                                        iProgressBarValue = iFileCountProgressBarValue;
                                    }
                                    bRecalcSizeProgress = false;
                                    globalClass.BroadcastProgress(true, "\nDownload completed and file placed at URI: \n" + sUserFriendlyDestinationFile + "\n",
                                            true, iProgressBarValue,
                                            true, lFileCountProgressNumerator + "/" + lFileCountProgressDenominator + " downloads completed.",
                                            DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);

                                } //End if fSource.exists. If it does not exist, we probably already moved it.

                                if(bDebug) gbwLogFile.write("\n");
                                if(bDebug) gbwLogFile.flush();

                                setDownloadIDs.remove(lDownloadID);

                                break; //end Case DownloadManager.STATUS_SUCCESSFUL.
                        }

                        long lConcernTime = 5 * 1000; //5 seconds in milliseconds.
                        double dWaitTimeCurrent = GlobalClass.GetTimeStampDouble();       //00000001000000000
                        double dTotalWaitTimeMS = (dWaitTimeCurrent - dWaitTimeStart) * 1000000000;//yyyyMMdd.HHmmssSSS
                        float fTemp = (float) dTotalWaitTimeMS;
                        fPercentConcernTimeElapsed = (fTemp / (float) lConcernTime) * 100;

                        if(fPercentConcernTimeElapsed > 50.0 && bDLAwaitingRetry){

                            //Calculate user-friendly time elapsed:
                            int iMSHours = 60 * 60 * 1000; //Milliseconds in an hour.
                            int iMSMinutes = 60 * 1000; //Milliseconds in a minute.
                            int iMSSeconds = 1000; //Milliseconds in a second.
                            int iNetTime = (int) dTotalWaitTimeMS; //iElapsedWaitTime;
                            int iHours = iNetTime / iMSHours;
                            iNetTime = iNetTime - iHours * iMSHours;
                            int iMinutes = iNetTime / iMSMinutes;
                            iNetTime = iNetTime - iMinutes * iMSMinutes;
                            int iSeconds = iNetTime / iMSSeconds;
                            String sTempFormat = "%02d:%02d:%02d";
                            @SuppressLint("DefaultLocale") String sElapsedTime = String.format(sTempFormat, iHours, iMinutes, iSeconds);

                            String sMessage2 = lFileCountProgressNumerator + "/" + lFileCountProgressDenominator + " downloads completed. " + "One or more downloads awaiting retry. Time elapsed: " + sElapsedTime;
                            iProgressBarValue = 0;
                            if(iFileCountProgressBarValue == 0 && iSizeProgressBarValue > 0){
                                iProgressBarValue = iSizeProgressBarValue;
                            } else if (iFileCountProgressBarValue >= iSizeProgressBarValue){
                                iProgressBarValue = iSizeProgressBarValue;
                            }
                            iProgressBarValue = iSizeProgressBarValue;
                            if(iFileCountProgressBarValue == 100){
                                iProgressBarValue = iFileCountProgressBarValue;
                            }
                            bRecalcSizeProgress = false;
                            globalClass.BroadcastProgress(true, "\n" + sMessage2,
                                    true, iProgressBarValue,
                                    true, sMessage2,
                                    DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);
                        }

                        if(bRecalcSizeProgress){
                            //If progress was recalculated based on changed bytes downloaded, and the progress was not updated at a prior step
                            //  based on file state, update it now:
                            iProgressBarValue = 0;
                            if(iFileCountProgressBarValue == 0 && iSizeProgressBarValue > 0){
                                iProgressBarValue = iSizeProgressBarValue;
                            } else if (iFileCountProgressBarValue >= iSizeProgressBarValue){
                                iProgressBarValue = iSizeProgressBarValue;
                            }
                            iProgressBarValue = iSizeProgressBarValue;
                            if(iFileCountProgressBarValue == 100){
                                iProgressBarValue = iFileCountProgressBarValue;
                            }
                            globalClass.BroadcastProgress(false, "",
                                    true, iProgressBarValue,
                                    false, "",
                                    DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);

                        }


                    } while (cursor.moveToNext() && setDownloadIDs.size() > 0); //End loop through download query results.

                } //End if cursor has a record.

                cursor.close();

            } //End loop waiting for download completion.

            //Downloads should be complete and moved out of the source folder.

            //Delete the download folder to which downloadManager downloaded the files:
            if(fThisDownloadFolder.exists()){
                File[] fDownloadFolderContents = fThisDownloadFolder.listFiles();
                if(fDownloadFolderContents != null) {
                    if (fDownloadFolderContents.length == 0) {
                        if (!fThisDownloadFolder.delete()) {
                            sMessage = "Could not delete download folder: " + fThisDownloadFolder.getAbsolutePath();
                            gbwLogFile.write(sMessage);
                            gbwLogFile.flush();
                        }
                    }
                }
            }

            if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).size() > 0){
                //If we are tracking a file index (likely due to catalog analysis activities)\
                //If this is a video or comic download, apply any additional attributes to the
                //  data intended for the file index:
                if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                        giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                    for (Map.Entry<String, ItemClass_File> entry : tmicf_ItemsToAddToFileIndex.entrySet()) {
                        if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                            if (giSingleOrM3U8 == DOWNLOAD_TYPE_M3U8) {
                                entry.getValue().iTypeFileFolderURL = ItemClass_File.TYPE_M3U8;
                            }
                        }
                        if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                                giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                            if (!sThumbnailUri.equals("")) {
                                entry.getValue().sUriThumbnailFile = sThumbnailUri;
                            }
                        }
                    }
                }
                GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(giMediaCategory).putAll(tmicf_ItemsToAddToFileIndex);
            }

            if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS){
                if(giSingleOrM3U8 == DOWNLOAD_TYPE_M3U8){
                    //Delete the temporary download folder parent if empty, which would be a tags folder.
                    String sPathParent = fThisDownloadFolder.getParent();
                    if(sPathParent != null) {
                        File fParent = new File(sPathParent);
                        if(fParent.exists()){
                            File[] fListing = fParent.listFiles();
                            if(fListing != null) {
                                if (fListing.length == 0) {
                                    if(!fParent.delete()){
                                        sMessage = "Could not delete download parent folder: " + fParent.getAbsolutePath();
                                        gbwLogFile.write(sMessage);
                                        gbwLogFile.flush();
                                    }
                                }
                            }
                        }
                    }

                } else {
                    //Delete the working folder holding the DLID_And_Sequence.txt file:



                }
            }

            //Toast.makeText(getApplicationContext(), "File download and transfer complete.", Toast.LENGTH_SHORT).show();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // Run your task here
                    Toast.makeText(getApplicationContext(), "File download and transfer complete.", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e){
            sMessage = e.getMessage();
            if(sMessage == null){
                sMessage = "Null message";
            }
            Log.d("Download Folder Transfer", sMessage) ;
        } finally {
            if(bDebug){
                try {
                    gbwLogFile.flush();
                    gbwLogFile.close();
                    gosLogFile.flush();
                    gosLogFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }


        if(gsDLPostProcessingID != null){
            Intent broadcastIntent_DLPostProcessing = new Intent();
            broadcastIntent_DLPostProcessing.putExtra(EXTRA_STRING_DOWNLOAD_POST_PROCESSING_ID, gsDLPostProcessingID);
            broadcastIntent_DLPostProcessing.setAction(DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);
            broadcastIntent_DLPostProcessing.addCategory(Intent.CATEGORY_DEFAULT);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_DLPostProcessing);
        }


        return Result.success();
    }

    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(FAILURE_MESSAGE, sMessage)
                .build();
    }

    private Data LogReturnWithFailure(String sMessage){
        if(gbLogInUse) {
            try {
                gbwLogFile.write(sMessage + "\n");
                gbwLogFile.flush();
                gbwLogFile.close();
                gosLogFile.flush();
                gosLogFile.close();
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.d("Log File Issue", e.getMessage());
                } else {
                    Log.d("Log File Issue", "Problem writing log file during video post processing.");
                }
            }
        }
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Catalog_LoadData:" + sRoutine, sMessage);
    }

    private class DownloadItem {
        long lDownloadID = 0;
        int iDownloadStatus = DownloadManager.STATUS_PENDING;
        long lBytesDownloaded = 0;
        long lBytesDownloadTotalSize = 0;
    }

}
