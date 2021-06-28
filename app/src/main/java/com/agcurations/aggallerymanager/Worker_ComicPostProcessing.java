package com.agcurations.aggallerymanager;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.LogCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.SessionState;
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_ComicPostProcessing extends Worker {

    //Define string used to identify this worker type:
    public static final String WORKER_COMIC_POST_PROCESSING_TAG = "WORKER_COMIC_POST_PROCESSING_TAG";

    //public static final String PROGRESS = "PROGRESS";
    //public static final String FILENAME = "FILENAME";
    public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";

    DownloadManager downloadManager;

    //=========================
    // Define keys for arguments passed to this worker:
    public static final String KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS = "KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS";
    public static final String KEY_ARG_WORKING_FOLDER = "KEY_ARG_WORKING_FOLDER";
    public static final String KEY_ARG_DOWNLOAD_IDS = "KEY_ARG_DOWNLOAD_IDS";
    public static final String KEY_ARG_ITEM_ID = "KEY_ARG_ITEM_ID";

    //=========================
    String gsPathToMonitorForDownloads;                 //Location to monitor for download file(s)
    long[] glDownloadIDs;                               //Used to determine if a download ID in DownloadManager has status 'completed'.
    String gsItemID;
    String gsWorkingFolder;

    //=========================

    public Worker_ComicPostProcessing(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters) {
        super(appContext, workerParameters);
        /*// Set initial progress to 0
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());*/
        gsPathToMonitorForDownloads = getInputData().getString(KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS);
        glDownloadIDs = getInputData().getLongArray(KEY_ARG_DOWNLOAD_IDS);
        gsItemID = getInputData().getString(KEY_ARG_ITEM_ID);
        gsWorkingFolder = getInputData().getString(KEY_ARG_WORKING_FOLDER);
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        if(!globalClass.gfLogsFolder.exists()){
            String sFailureMessage = "Logs folder missing. Restarting app should create the folder.";
            return Result.failure(DataErrorMessage(sFailureMessage));
        }

        //Put the log file in the logs folder:
        final String sLogFilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                File.separator + GlobalClass.GetTimeStampFileSafe() + "_Comic_" + gsItemID + "_FFMPEGLog.txt";
        final File fLog = new File(sLogFilePath);


        //Validate data
        if( gsPathToMonitorForDownloads.equals("")){
            String sFailureMessage = "No path to monitor for downloads provided.";
            return Result.failure(DataErrorMessage(sFailureMessage));
        }

        File fThisDownloadFolder = new File(gsPathToMonitorForDownloads);
        if(!fThisDownloadFolder.exists()){
            String sFailureMessage = "Folder to monitor for downloads does not exist: " + gsPathToMonitorForDownloads;
            return Result.failure(DataErrorMessage(sFailureMessage));
        }

        /*if(!gsVideoOutputFilename.equals("")){
            return Result.failure(DataErrorMessage("Killing Worker."));
        }*/

        //Monitor the location for file downloads' completion:
        int iElapsedWaitTime = 0;
        int iWaitDuration = 5000; //milliseconds
        boolean bFileDownloadsComplete = false;
        boolean bDownloadProblem = false;
        boolean bPaused = false;
        String sDownloadFailedReason = "";
        String sDownloadPausedReason = "";


        while( (iElapsedWaitTime < GlobalClass.DOWNLOAD_WAIT_TIMEOUT) && !bFileDownloadsComplete && !bDownloadProblem) {

            try {
                Thread.sleep(iWaitDuration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!bPaused) {
                iElapsedWaitTime += iWaitDuration;
            } else {
                iElapsedWaitTime += (int) (iWaitDuration/ 10.0); //Wait longer if a download is paused.
            }


            DownloadManager.Query dmQuery = new DownloadManager.Query();
            dmQuery.setFilterById(glDownloadIDs);
            downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor cursor = downloadManager.query(dmQuery);

            if(cursor.moveToFirst()) {
                do {
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);
                    int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(columnReason);
                    int iLocalURIIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    String sLocalURI = cursor.getString(iLocalURIIndex);

                    bDownloadProblem = false;
                    bPaused = false;
                    bFileDownloadsComplete = false;

                    switch (status) {
                        case DownloadManager.STATUS_FAILED:
                            bDownloadProblem = true;
                            switch (reason) {
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
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            bPaused = true;
                            switch (reason) {
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
                            }                            break;
                        case DownloadManager.STATUS_PENDING:
                            //No action.
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            //No action.
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            bFileDownloadsComplete = true;

                            //As of Android version 11, API level 30, One UI 3.1, the DownloadManager
                            //  will only store files in the onboard storage, or something like that.
                            //  Move those files over to the SD Card before processing.
                            sLocalURI = sLocalURI.replace("file://", "");
                            File fSource = new File(sLocalURI);
                            if(fSource.exists()) {
                                String sFileName = fSource.getName();
                                String sDestination = gsWorkingFolder + File.separator + sFileName;
                                File fDestination = new File(sDestination);
                                //Move the file to the working folder:
                                if (!fDestination.exists()) {
                                    try {
                                        InputStream inputStream;
                                        OutputStream outputStream;
                                        inputStream = new FileInputStream(fSource.getPath());
                                        outputStream = new FileOutputStream(fDestination.getPath());
                                        byte[] buffer = new byte[100000];
                                        while ((inputStream.read(buffer, 0, buffer.length)) >= 0) {
                                            outputStream.write(buffer, 0, buffer.length);
                                        }
                                        outputStream.flush();
                                        outputStream.close();

                                        if (!fSource.delete()) {
                                            String sMessage = "Could not delete source file after copy. Source: " + fSource.getAbsolutePath();
                                            FileWriter fwLogFile;
                                            fwLogFile = new FileWriter(fLog, true);
                                            fwLogFile.write("DownloadManager monitoring message: " + sMessage + "\n");
                                            fwLogFile.flush();
                                            fwLogFile.close();
                                        }
                                    } catch (Exception e) {
                                        try {
                                            String sMessage = fSource.getPath() + "\n" + e.getMessage();
                                            FileWriter fwLogFile;
                                            fwLogFile = new FileWriter(fLog, true);
                                            fwLogFile.write("Stream copy exception: " + sMessage + "\n");
                                            fwLogFile.flush();
                                            fwLogFile.close();
                                        } catch(Exception e2){
                                            String sMessageI = "";
                                            if(e.getMessage() != null) {
                                                sMessageI = e.getMessage();
                                            }
                                            Log.d("Stream copy exception", sMessageI);
                                            String sMessageII = "";
                                            if(e2.getMessage() != null) {
                                                sMessageII = e2.getMessage();
                                            }
                                            Log.d("Log write exception", sMessageII);
                                        }
                                    }
                                } //End if !FDestination.exists. If it does exist, we have already copied the file over.
                            } //End if fSource.exists. If it does not exist, we probably already moved it.



                            break;
                    }
                } while (cursor.moveToNext() && bFileDownloadsComplete && !bDownloadProblem); //End loop through download query results.



            } //End if cursor has a record.

        } //End loop waiting for download completion.

        //Downloads should be complete and moved out of the source folder.



        //Delete the download folder to which downloadManager downloaded the files:
        if(fThisDownloadFolder.exists()){
            if(!fThisDownloadFolder.delete()){
                try {
                    String sMessage = "Could not delete download folder: " + fThisDownloadFolder.getAbsolutePath();
                    FileWriter fwLogFile;
                    fwLogFile = new FileWriter(fLog, true);
                    fwLogFile.write(sMessage + "\n");
                    fwLogFile.flush();
                    fwLogFile.close();
                } catch(Exception e){
                    String sMessage2 = "";
                    if(e.getMessage() != null) {
                        sMessage2 = e.getMessage();
                    }
                    Log.d("Log write exception", sMessage2);
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

}
