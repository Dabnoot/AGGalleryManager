package com.agcurations.aggallerymanager;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Browser_ImageDownloadToHoldingFolder extends Worker {

    public static final String TAG_WORKER_BROWSER_IMAGEDOWNLOADTOHOLDINGFOLDER = "com.agcurations.aggallermanager.tag_worker_browser_imagedownloadtoholdingfolder";

    long lDownloadID;

    public Worker_Browser_ImageDownloadToHoldingFolder(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        lDownloadID = getInputData().getLong(GlobalClass.EXTRA_LONG_DOWNLOAD_ID, -1);
    }

    @NonNull
    @Override
    public Result doWork() {

        if(lDownloadID == -1){
            return Result.failure();
        }

        //Monitor the location for file downloads' completion:
        int iElapsedWaitTime = 0;
        int iWaitDuration = 500; //milliseconds
        boolean bFileDownloadComplete = false;
        boolean bDownloadProblem = false;
        boolean bPaused = false;
        String sMessage;
        String sDownloadFailedReason = "";
        String sDownloadPausedReason = "";
        boolean bDebug = false;

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        String sLogFilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                File.separator + GlobalClass.GetTimeStampFileSafe() + "_ImageDLTransfer_WorkerLog.txt";
        File fLog = new File(sLogFilePath);
        FileWriter fwLogFile = null;
        try {

            if(bDebug) fwLogFile = new FileWriter(fLog, true);

            sMessage = "Waiting for download to complete, a maximum of " + (GlobalClass.DOWNLOAD_WAIT_TIMEOUT / 1000) + " seconds.";
            if(bDebug) fwLogFile.write(sMessage + "\n");
            if(bDebug) fwLogFile.flush();
            while ((iElapsedWaitTime < GlobalClass.DOWNLOAD_WAIT_TIMEOUT) && !bFileDownloadComplete && !bDownloadProblem) {

                try {
                    Thread.sleep(iWaitDuration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!bPaused) {
                    iElapsedWaitTime += iWaitDuration;
                } else {
                    iElapsedWaitTime += (int) (iWaitDuration / 10.0); //Wait longer if a download is paused.
                }
                if(bDebug) fwLogFile.write(".");
                if(bDebug) fwLogFile.flush();

                //Query for remaining downloads:

                DownloadManager.Query dmQuery = new DownloadManager.Query();
                dmQuery.setFilterById(lDownloadID);
                final DownloadManager downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
                Cursor cursor = downloadManager.query(dmQuery);

                if (cursor.moveToFirst()) {
                    do {
                        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = cursor.getInt(columnIndex);
                        int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                        int iReasonID = cursor.getInt(columnReason);
                        int iLocalURIIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        String sLocalURI = cursor.getString(iLocalURIIndex);
                        int iDownloadURI = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
                        String sDownloadURI = cursor.getString(iDownloadURI);
                        int iDownloadID = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                        long lDownloadID = cursor.getLong(iDownloadID);

                        bDownloadProblem = false;
                        bPaused = false;
                        bFileDownloadComplete = false;

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
                                if(bDebug) fwLogFile.write(sMessage + "\n\n");
                                if(bDebug) fwLogFile.flush();
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
                                if(bDebug) fwLogFile.write(sMessage + "\n\n");
                                if(bDebug) fwLogFile.flush();

                                break;
                            case DownloadManager.STATUS_PENDING:
                                //No action.
                                break;
                            case DownloadManager.STATUS_RUNNING:
                                //No action.
                                break;
                            case DownloadManager.STATUS_SUCCESSFUL:
                                bFileDownloadComplete = true;

                                //As of Android version 11, API level 30, One UI 3.1, the DownloadManager
                                //  will only store files in the onboard storage, or something like that.
                                //  Move those files over to the SD Card before processing.
                                sLocalURI = sLocalURI.replace("file://", "");
                                sLocalURI = URLDecoder.decode(sLocalURI, StandardCharsets.UTF_8.toString());
                                File fSource = new File(sLocalURI);
                                String sFileName = fSource.getName();
                                if(bDebug) fwLogFile.write("Download completed: " + sFileName);
                                if (fSource.exists()) {
                                    //Determine the destination filename:
                                    File[] fDLHoldingFiles = globalClass.gfImageDownloadHoldingFolder.listFiles();
                                    if(fDLHoldingFiles != null) {
                                        if(fDLHoldingFiles.length > 0) {
                                            String sNew = sFileName;
                                            if(sNew.length() > 50){
                                                //Limit the length of the filename:
                                                String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sNew);
                                                if(sBaseAndExtension.length == 2) {
                                                    sNew = sBaseAndExtension[0].substring(0, 50 - sBaseAndExtension[1].length());
                                                    sNew = sNew + "." + sBaseAndExtension[1];
                                                }
                                            }


                                            boolean bMatchFoundInExistingHoldingFiles;
                                            int iIterator = 0;
                                            do {
                                                bMatchFoundInExistingHoldingFiles = false;
                                                for (File fExisting : fDLHoldingFiles) {
                                                    if (sNew.contentEquals(fExisting.getName())) {
                                                        bMatchFoundInExistingHoldingFiles = true;
                                                        break;
                                                    }
                                                }
                                                if (bMatchFoundInExistingHoldingFiles) {
                                                    iIterator += 1;
                                                    String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sFileName);
                                                    sNew = sBaseAndExtension[0] + "_"  + String.format(Locale.getDefault(), "%04d", iIterator);
                                                    if(sBaseAndExtension.length == 2) {
                                                        sNew = sNew + "." + sBaseAndExtension[1];
                                                    }
                                                }
                                            } while (bMatchFoundInExistingHoldingFiles);
                                            sFileName = sNew;
                                        }
                                    }
                                    String sDestination = globalClass.gfImageDownloadHoldingFolder.getAbsolutePath() + File.separator + sFileName;
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
                                            if(bDebug) fwLogFile.write(" Copied to working folder.");
                                            if (!fSource.delete()) {
                                                sMessage = "Could not delete source file after copy. Source: " + fSource.getAbsolutePath();
                                                if(bDebug) fwLogFile.write("Download monitoring: " + sMessage + "\n");
                                            } else {
                                                if(bDebug) fwLogFile.write(" Source file deleted.");
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
                                        } catch (Exception e) {
                                            sMessage = fSource.getPath() + "\n" + e.getMessage();
                                            if(bDebug) fwLogFile.write("Stream copy exception: " + sMessage + "\n");
                                        }
                                    } //End if !FDestination.exists. If it does exist, we have already copied the file over.
                                } else { //End if fSource.exists. If it does not exist, we probably already moved it.
                                    if(bDebug) fwLogFile.write(" Source file does not exist (already moved?).");
                                }
                                if(bDebug) fwLogFile.write("\n");
                                if(bDebug) fwLogFile.flush();

                                break;
                        }
                    } while (cursor.moveToNext() && bFileDownloadComplete && !bDownloadProblem); //End loop through download query results.


                } //End if cursor has a record.

            } //End loop waiting for download completion.

        } catch (Exception e){
            sMessage = e.getMessage();
            if(sMessage == null){
                sMessage = "Null message";
            }
            Log.d("Image Download Transfer", sMessage) ;
        }


        return Result.success();
    }

}
