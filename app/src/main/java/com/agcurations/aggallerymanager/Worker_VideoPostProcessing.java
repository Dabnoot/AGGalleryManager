package com.agcurations.aggallerymanager;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.core.content.res.TypedArrayUtils;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.LogCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.SessionState;
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;

import static com.agcurations.aggallerymanager.Worker_ComicPostProcessing.KEY_ARG_DOWNLOAD_IDS;

public class Worker_VideoPostProcessing extends Worker {

    //Define string used to identify this worker type:
    public static final String WORKER_VIDEO_POST_PROCESSING_TAG = "WORKER_VIDEO_POST_PROCESSING_TAG";

    //public static final String PROGRESS = "PROGRESS";
    //public static final String FILENAME = "FILENAME";
    public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";

    public static final String VIDEO_FILE_SEQUENCE_FILE_NAME = "Video_FileSequence.txt";

    DownloadManager downloadManager;

    //=========================
    // Define keys for arguments passed to this worker:
    public static final String KEY_ARG_DOWNLOAD_TYPE_SINGLE_OR_M3U8 = "KEY_ARG_DOWNLOAD_TYPE_SINGLE_OR_M3U8";
    public static final String KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS = "KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS";
    public static final String KEY_ARG_WORKING_FOLDER = "KEY_ARG_WORKING_FOLDER";
    //public static final String KEY_ARG_FILENAME_SEQUENCE = "KEY_ARG_FILENAME_SEQUENCE";
    public static final String KEY_ARG_VIDEO_OUTPUT_FILENAME = "KEY_ARG_VIDEO_OUTPUT_FILENAME";
    public static final String KEY_ARG_VIDEO_TOTAL_FILE_SIZE = "KEY_ARG_VIDEO_TOTAL_FILE_SIZE";
    //public static final String KEY_ARG_DOWNLOAD_IDS = "KEY_ARG_DOWNLOAD_IDS";
    public static final String KEY_ARG_ITEM_ID = "KEY_ARG_ITEM_ID";

    //=========================
    int giDownloadTypeSingleOrM3U8;
    public static final int DOWNLOAD_TYPE_SINGLE = 1;
    public static final int DOWNLOAD_TYPE_M3U8 = 2;
    String gsPathToMonitorForDownloads;                 //Location to monitor for download file(s)
    String gsWorkingFolder;                             //Location on external SD Card (big storage)
    int giExpectedDownloadFileCount;                    //Expected count of downloaded files
    String[] gsFilenameSequence;
    String gsVideoOutputFilename;                       //Name of output file
    long glTotalFileSize;
    long[] glDownloadIDs;                               //Used to determine if a download ID in DownloadManager has status 'completed'.
    String gsItemID;

    //=========================

    public Worker_VideoPostProcessing(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters) {
        super(appContext, workerParameters);
        /*// Set initial progress to 0
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());*/
        giDownloadTypeSingleOrM3U8 = getInputData().getInt(KEY_ARG_DOWNLOAD_TYPE_SINGLE_OR_M3U8, DOWNLOAD_TYPE_SINGLE);
        gsPathToMonitorForDownloads = getInputData().getString(KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS);
        gsWorkingFolder = getInputData().getString((KEY_ARG_WORKING_FOLDER));
        gsVideoOutputFilename = getInputData().getString(KEY_ARG_VIDEO_OUTPUT_FILENAME);
        glTotalFileSize = getInputData().getLong(KEY_ARG_VIDEO_TOTAL_FILE_SIZE,0);

        gsItemID = getInputData().getString(KEY_ARG_ITEM_ID);
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
                File.separator + GlobalClass.GetTimeStampFileSafe() + "_Video_" + gsItemID + "_FFMPEGLog.txt";
        final File fLog = new File(sLogFilePath);


        //Validate data
        if( gsVideoOutputFilename.equals("")){
            String sFailureMessage = "No output file name provided.";
            return Result.failure(DataErrorMessage(sFailureMessage));
        }
        if( gsPathToMonitorForDownloads.equals("")){
            String sFailureMessage = "No path to monitor for downloads provided.";
            return Result.failure(DataErrorMessage(sFailureMessage));
        }

        File fThisDownloadFolder = new File(gsPathToMonitorForDownloads);
        if(!fThisDownloadFolder.exists()){
            String sFailureMessage = "Folder to monitor for downloads does not exist: " + gsPathToMonitorForDownloads;
            return Result.failure(DataErrorMessage(sFailureMessage));
        }

        if( gsWorkingFolder.equals("")){
            String sFailureMessage = "No working folder provided.";
            return Result.failure(DataErrorMessage(sFailureMessage));
        }
        File fThisWorkingFolder = new File(gsWorkingFolder);
        if(!fThisWorkingFolder.exists()){
            String sFailureMessage = "Working folder does not exist: " + gsWorkingFolder;
            return Result.failure(DataErrorMessage(sFailureMessage));
        }


        final String sFileSequenceFilePath = gsWorkingFolder +
                File.separator + VIDEO_FILE_SEQUENCE_FILE_NAME;
        final File fFileSequenceFile = new File(sFileSequenceFilePath);

        if(fFileSequenceFile.exists()) {
            //Get data from file:
            BufferedReader brReader;
            try {
                brReader = new BufferedReader(new FileReader(fFileSequenceFile.getAbsolutePath()));
                brReader.readLine();//First line is the header, skip it.
                String sLine = brReader.readLine();
                ArrayList<Long> allDownloadIDs = new ArrayList<>();
                ArrayList<String> alsDownloadFileSequence = new ArrayList<>();
                int j = 0;
                while (sLine != null) {
                    j++;
                    if(!sLine.equals("")){
                        String[] sTemp = sLine.split("\t");
                        if(sTemp.length == 2){
                            long lTemp;
                            try {
                                lTemp = Long.parseLong(sTemp[0]);
                                allDownloadIDs.add(lTemp);
                                alsDownloadFileSequence.add(sTemp[1]);
                            } catch (Exception e){
                                String sFailureMessage = "Could not parse long while reading file sequence file, line " + j + ": " + sFileSequenceFilePath;
                                return Result.failure(DataErrorMessage(sFailureMessage));
                            }

                        } else {
                            String sFailureMessage = "Data missing while reading file sequence file, line " + j + ": " + sFileSequenceFilePath;
                            return Result.failure(DataErrorMessage(sFailureMessage));
                        }
                    }
                    sLine = brReader.readLine();
                }
                brReader.close();

                gsFilenameSequence = new String[alsDownloadFileSequence.size()];
                gsFilenameSequence = alsDownloadFileSequence.toArray(gsFilenameSequence);
                giExpectedDownloadFileCount = gsFilenameSequence.length;
                glDownloadIDs = new long[allDownloadIDs.size()];
                for(int l = 0; l < allDownloadIDs.size(); l++) {
                    glDownloadIDs[l] = allDownloadIDs.get(l);
                }

            } catch (IOException e) {
                String sFailureMessage = "Problem reading file sequence file: " + sFileSequenceFilePath;
                return Result.failure(DataErrorMessage(sFailureMessage));
            }


        } else {
            String sFailureMessage = "File sequence file does not exist: " + sFileSequenceFilePath;
            return Result.failure(DataErrorMessage(sFailureMessage));
        }













        if( giExpectedDownloadFileCount == 0){
            String sFailureMessage = "No expected file download count provided.";
            return Result.failure(DataErrorMessage(sFailureMessage));
        }




        /*if(!gsVideoOutputFilename.equals("")){
            return Result.failure(DataErrorMessage("Killing Worker."));
        }*/

        //Establish the name of the temporary output folder for the video concatenation result,
        // and create the folder:
        String sOutputFolder = "Output";
        String sOutputFolderPath = fThisWorkingFolder + File.separator + sOutputFolder;
        File fOutputFolder = new File(sOutputFolderPath);
        int iOutputFolderRetryIterator = 1;
        while(fOutputFolder.exists()){
            sOutputFolder = "Output_" + iOutputFolderRetryIterator;
            iOutputFolderRetryIterator++;
            sOutputFolderPath = fThisWorkingFolder + File.separator + sOutputFolder;
            fOutputFolder = new File(sOutputFolderPath);
        }

        if (!fOutputFolder.mkdir()) {
            String sFailureMessage = "Could not create folder at " + fOutputFolder.getAbsolutePath();
            return Result.failure(DataErrorMessage(sFailureMessage));
        }

        //Monitor the location for file downloads' completion:
        int iElapsedWaitTime = 0;
        int iWaitDuration = 5000; //milliseconds
        boolean bFileDownloadsComplete = false;
        boolean bDownloadProblem = false;
        boolean bPaused = false;
        File[] fDownloadedFiles;
        String sDownloadFailedReason = "";
        String sDownloadPausedReason = "";

        String sDownloadFolderToClean = "";

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
                            sDownloadFolderToClean = sLocalURI;
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

        //Attempt to remove the category folder from the download location if it is empty:
        if(!sDownloadFolderToClean.equals("")){
            sDownloadFolderToClean = sDownloadFolderToClean.substring(0, sDownloadFolderToClean.lastIndexOf(File.separator));
            sDownloadFolderToClean = sDownloadFolderToClean.substring(0, sDownloadFolderToClean.lastIndexOf(File.separator));
            File fDownloadFolderToClean = new File(sDownloadFolderToClean);
            File[] fTemp = fDownloadFolderToClean.listFiles();
            if(fTemp != null) {
                if (fTemp.length == 0) {
                    if (!fDownloadFolderToClean.delete()) {
                        //Unable to remove stub folder.
                        try {
                            String sMessage = "Could not delete temp category folder: " + fDownloadFolderToClean.getAbsolutePath();
                            FileWriter fwLogFile;
                            fwLogFile = new FileWriter(fLog, true);
                            fwLogFile.write(sMessage + "\n");
                            fwLogFile.flush();
                            fwLogFile.close();
                        } catch (Exception e) {
                            String sMessage2 = "";
                            if (e.getMessage() != null) {
                                sMessage2 = e.getMessage();
                            }
                            Log.d("Log write exception", sMessage2);
                        }
                    }
                }
            }

        }




        final String sFinalOutputPath = sOutputFolderPath + File.separator + GlobalClass.JumbleFileName(gsVideoOutputFilename);
        if(bFileDownloadsComplete) {
            fDownloadedFiles = fThisWorkingFolder.listFiles();
            if(fDownloadedFiles == null){
                String sFailureMessage = "Downloaded file(s) missing from folder: " + gsWorkingFolder;
                return Result.failure(DataErrorMessage(sFailureMessage));
            } else if (fDownloadedFiles.length < 2) {
                String sFailureMessage = "Downloaded file(s) missing from folder: " + gsWorkingFolder;
                return Result.failure(DataErrorMessage(sFailureMessage));
            }
            if(giDownloadTypeSingleOrM3U8 == DOWNLOAD_TYPE_SINGLE) {
                //Move the file to the output folder to get captured by the main program:
                File fInputFile = null; //There should be but 1 file in the download folder for DOWNLOAD_TYPE_SINGLE. Index 0 is the folder.
                for(File f: fDownloadedFiles){
                    if(f.isFile()){
                        fInputFile = f;
                    }
                }

                File fOutputFile = new File(sFinalOutputPath);
                if(fInputFile != null) {
                    if (!fInputFile.renameTo(fOutputFile)) {
                        String sFailureMessage = "Could not move downloaded file to output folder: " + fInputFile.getAbsolutePath();
                        return Result.failure(DataErrorMessage(sFailureMessage));
                    }
                } else {
                    String sFailureMessage = "Download file missing from folder: " + fThisWorkingFolder.getAbsolutePath();
                    return Result.failure(DataErrorMessage(sFailureMessage));
                }

            } else {
            //if(giDownloadTypeSingleOrM3U8 == DOWNLOAD_TYPE_M3U8) {
                //Process the files.

                //Create a file listing the files which are to be concatenated:
                String sFFMPEGInputFilename = "FFMPEGInputFileName.txt";
                String sFFMPEGInputFilePath = sOutputFolderPath + File.separator + sFFMPEGInputFilename;
                File fFFMPEGInputFile = new File(sFFMPEGInputFilePath);

                //Sort the file names:
                TreeMap<Integer, String> tmDownloadedFiles = new TreeMap<>();
                for(File f: fDownloadedFiles){
                    for(int j = 0; j < gsFilenameSequence.length; j++){
                        if(f.isFile()){
                            if(!f.getName().contains(VIDEO_FILE_SEQUENCE_FILE_NAME)) {
                                String sFilename = f.getName();
                                if (sFilename.contains(gsFilenameSequence[j])) {
                                    tmDownloadedFiles.put(j, f.getAbsolutePath());
                                    break;
                                }
                            }
                        }
                    }
                }

                StringBuilder sbBuffer = new StringBuilder();
                for (Map.Entry<Integer, String> entry: tmDownloadedFiles.entrySet()) {
                    sbBuffer.append("file '");
                    sbBuffer.append(entry.getValue());
                    sbBuffer.append("'\n");
                }

                //Write the data to the file:
                try {
                    FileWriter fwFFMPEGInputFile;
                    fwFFMPEGInputFile = new FileWriter(fFFMPEGInputFile, false);
                    fwFFMPEGInputFile.write(sbBuffer.toString());
                    fwFFMPEGInputFile.flush();
                    fwFFMPEGInputFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    String sFailureMessage = "Unable to create FFMPEG input file: " + fFFMPEGInputFile.getAbsolutePath();
                    return Result.failure(DataErrorMessage(sFailureMessage));
                }

                //Execute the FFMPEG concatenation:
                final String sFFMPEGOutputFilePath = sOutputFolderPath + File.separator + gsVideoOutputFilename;

                String sCommand = "-f concat -safe 0 -i " + sFFMPEGInputFilePath + " -c copy \"" + sFFMPEGOutputFilePath + "\"";
                FFmpegKit.executeAsync(sCommand, new ExecuteCallback() {

                    @Override
                    public void apply(Session session) {
                        // CALLED WHEN SESSION IS EXECUTED
                        SessionState state = session.getState();
                        ReturnCode returnCode = session.getReturnCode();
                        //Write the data to the log file:
                        try {
                            FileWriter fwLogFile;
                            fwLogFile = new FileWriter(fLog, true);
                            fwLogFile.write(String.format("\nExec message: FFmpeg process exited with state %s and return code %s.\n", state, returnCode) + "\n");

                            if(ReturnCode.isSuccess(returnCode)) {
                                //Attempt to move the output file:
                                File fFFMPEGOutputFile = new File(sFFMPEGOutputFilePath);
                                File fFinalOutputFile = new File(sFinalOutputPath);
                                if (!fFFMPEGOutputFile.renameTo(fFinalOutputFile)) {
                                    String sFailureMessage = "Exec message: Could not rename FFMPEG output file to final file name: " + fFFMPEGOutputFile.getAbsolutePath() + " => " + fFinalOutputFile.getAbsolutePath();
                                    fwLogFile.write(sFailureMessage + "\n");

                                }
                            }
                            String sMessage = session.getFailStackTrace();
                            if(sMessage != null) {
                                fwLogFile.write("Exec message: " + sMessage + "\n");
                            }

                            fwLogFile.flush();
                            fwLogFile.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, new LogCallback() {

                    @Override
                    public void apply(com.arthenica.ffmpegkit.Log log) {
                        // CALLED WHEN SESSION PRINTS LOGS
                        String sMessage = log.getMessage();

                        //Write the data to the log file and rename the output file so that the main application can find it:
                        try {
                            FileWriter fwLogFile;
                            fwLogFile = new FileWriter(fLog, true);
                            fwLogFile.write("Log message: " + sMessage + "\n");
                            fwLogFile.flush();
                            fwLogFile.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }, new StatisticsCallback() {

                    @Override
                    public void apply(Statistics statistics) {
                        // CALLED WHEN SESSION GENERATES STATISTICS
                        String sMessage = "File size: " + statistics.getSize();

                        //Write the data to the log file:
                        try {
                            FileWriter fwLogFile;
                            fwLogFile = new FileWriter(fLog, true);
                            fwLogFile.write("Stat message: " + sMessage + "\n");
                            fwLogFile.flush();
                            fwLogFile.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }



                    }
                });

                //Execute no further processing as the FFMPEG call is asynchronous.


            }
            return Result.success();
        } else {

            //Write a failure message to the log:
            FileWriter fwLogFile;
            String sFailureMessage = "";
            if( iElapsedWaitTime >= GlobalClass.DOWNLOAD_WAIT_TIMEOUT) {
                sFailureMessage = "Download elapsed time exceeds timeout of " + GlobalClass.DOWNLOAD_WAIT_TIMEOUT + " milliseconds.";
            } else if (bDownloadProblem) {
                sFailureMessage = "There was a problem with a download. " + sDownloadFailedReason;
            }

            try {
                fwLogFile = new FileWriter(fLog, true);
                fwLogFile.write(sFailureMessage + "\n");
                fwLogFile.flush();
                fwLogFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return Result.failure(DataErrorMessage(sFailureMessage));
        }

    }



    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(FAILURE_MESSAGE, sMessage)
                .build();
    }

}
