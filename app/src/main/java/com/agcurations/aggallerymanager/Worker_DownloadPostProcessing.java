package com.agcurations.aggallerymanager;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.exoplayer2.util.MimeTypes;

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


    GlobalClass globalClass;

    String gsPathToMonitorForDownloads;                 //Location to monitor for download file(s)
    long[] glDownloadIDs;                               //Used to determine if a download ID in DownloadManager has status 'completed'.
    String gsItemID;
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

        //Get path to monitor for downloads for the purpose of deleting a download folder when
        //  empty:
        gsPathToMonitorForDownloads = getInputData().getString(KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS);

        //Get the array of download IDs to query from the DownloadManager:
        glDownloadIDs = getInputData().getLongArray(KEY_ARG_DOWNLOAD_IDS);

        //Get a catalog item ID. Not required or valid for downloads related to holding folder (images only).
        gsItemID = getInputData().getString(KEY_ARG_ITEM_ID);

        //Get the media category for the download.
        giMediaCategory = getInputData().getInt(KEY_ARG_MEDIA_CATEGORY, -1);

        String sRelativePath = getInputData().getString(KEY_ARG_RELATIVE_PATH_TO_FOLDER); //If this is a comic, it will be the comic ID. If it is a video, it will be the tag folder.
        if(sRelativePath != null){
            uriDestinationFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[giMediaCategory], sRelativePath);
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

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;

        if(!GlobalClass.CheckIfFileExists(GlobalClass.gUriLogsFolder)){
            sMessage = "Logs folder missing. Restarting app should create the folder.";
            return Result.failure(DataErrorMessage(sMessage));
        }

        //Prepare log file:
        String sLogFileName = GlobalClass.GetTimeStampFileSafe() + "_DLTransferLog_" + gsItemID + ".txt";

        try {
            if(bDebug) {
                Uri uriLog = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, GlobalClass.gUriLogsFolder, MimeTypes.BASE_TYPE_TEXT, sLogFileName);
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
            lProgressDenominator = glDownloadIDs.length;

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
            boolean bFileDownloadsComplete = false;
            boolean bDownloadProblem = false;
            boolean bPaused = false;
            String sDownloadFailedReason = "";
            String sDownloadPausedReason = "";


            sMessage = "Waiting for download to complete, a maximum of " + (GlobalClass.DOWNLOAD_WAIT_TIMEOUT / 1000) + " seconds.";
            if(bDebug) gbwLogFile.write(sMessage + "\n");
            if(bDebug) gbwLogFile.flush();

            DownloadManager downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);

            globalClass.BroadcastProgress(true, "\nBegin monitoring for downloads...\n",
                    true, 0,
                    true, lProgressNumerator + "/" + lProgressDenominator + " downloads completed.",
                    DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);

            while ((iElapsedWaitTime < GlobalClass.DOWNLOAD_WAIT_TIMEOUT) && !bFileDownloadsComplete && !bDownloadProblem) {

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

                //Query for remaining downloads:

                DownloadManager.Query dmQuery = new DownloadManager.Query();
                dmQuery.setFilterById(glDownloadIDs);

                Cursor cursor = downloadManager.query(dmQuery);

                if(cursor.moveToFirst()) {
                    do {
                        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = cursor.getInt(columnIndex);
                        int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                        int iReasonID = cursor.getInt(columnReason);
                        int iLocalURIIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        String sLocalURI = cursor.getString(iLocalURIIndex);
                        int iDownloadURI = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
                        String sDownloadURI = cursor.getString(iDownloadURI);

                        bDownloadProblem = false;
                        bPaused = false;
                        bFileDownloadsComplete = false;

                        switch (status) {
                            case DownloadManager.STATUS_FAILED:
                                bDownloadProblem = true;
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
                                }
                                sMessage = "\nThere was a problem with a download.";
                                sMessage = sMessage + "\n" + "Download: " + sDownloadURI;
                                sMessage = sMessage + "\n" + "Reason ID: " + iReasonID;
                                sMessage = sMessage + "\n" + "Reason text: " + sDownloadFailedReason;
                                if(bDebug) gbwLogFile.write(sMessage + "\n\n");
                                if(bDebug) gbwLogFile.flush();
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
                                bFileDownloadsComplete = true;

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
                                    String sUserFriendlyDestinationFolder = GlobalClass.cleanHTMLCodedCharacters(uriDestinationFolder.toString());

                                    //Move the file to the destination folder:
                                    if (!GlobalClass.CheckIfFileExists(uriDestinationFile)) {
                                        //If the destination file does not exist...
                                        uriDestinationFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriDestinationFolder, MimeTypes.BASE_TYPE_TEXT, sFileName);
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
                                                while ((inputStream.read(buffer, 0, buffer.length)) >= 0) {
                                                    osDestinationFile.write(buffer, 0, buffer.length);
                                                }
                                                osDestinationFile.flush();
                                                osDestinationFile.close();
                                                if (bDebug) gbwLogFile.write(" Copied to working folder.");

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

                                    lProgressNumerator++;
                                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                                    globalClass.BroadcastProgress(true, "\nDownload completed and file placed at URI: \n" + sUserFriendlyDestinationFolder + "\n",
                                            true, iProgressBarValue,
                                            true, lProgressNumerator + "/" + lProgressDenominator + " downloads completed.",
                                            DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);

                                } //End if fSource.exists. If it does not exist, we probably already moved it.

                                if(bDebug) gbwLogFile.write("\n");
                                if(bDebug) gbwLogFile.flush();

                                break; //end Case DownloadManager.STATUS_SUCCESSFUL.
                        }
                    } while (cursor.moveToNext() && bFileDownloadsComplete && !bDownloadProblem); //End loop through download query results.



                } //End if cursor has a record.

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

}
