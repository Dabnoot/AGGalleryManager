package com.agcurations.aggallerymanager;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import androidx.annotation.NonNull;
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

public class Worker_VideoPostProcessing extends Worker {

    //Define string used to identify this worker type:
    public static final String WORKER_VIDEO_POST_PROCESSING_TAG = "WORKER_VIDEO_POST_PROCESSING_TAG";

    //public static final String PROGRESS = "PROGRESS";
    //public static final String FILENAME = "FILENAME";
    public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";

    //int iSavedNumerator = -1;

    //=========================
    // Define keys for arguments passed to this worker:
    public static final String KEY_ARG_DOWNLOAD_TYPE_SINGLE_OR_M3U8 = "KEY_ARG_DOWNLOAD_TYPE_SINGLE_OR_M3U8";
    public static final String KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS = "KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS";
    public static final String KEY_ARG_EXPECTED_DOWNLOAD_FILE_COUNT = "KEY_ARG_EXPECTED_DOWNLOAD_FILE_COUNT";
    public static final String KEY_ARG_VIDEO_OUTPUT_FILENAME = "KEY_ARG_VIDEO_OUTPUT_FILENAME";

    //=========================
    int giDownloadTypeSingleOrM3U8 = 0;
    public static final int DOWNLOAD_TYPE_SINGLE = 1;
    public static final int DOWNLOAD_TYPE_M3U8 = 2;
    String gsPathToMonitorForDownloads;                  //Location to monitor for download file(s)
    int giExpectedDownloadFileCount;                     //Expected count of downloaded files
    String gsVideoOutputFilename;                        //Name of output file

    //=========================

    public Worker_VideoPostProcessing(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters) {
        super(appContext, workerParameters);
        /*// Set initial progress to 0
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());*/
        giDownloadTypeSingleOrM3U8 = getInputData().getInt(KEY_ARG_DOWNLOAD_TYPE_SINGLE_OR_M3U8, DOWNLOAD_TYPE_SINGLE);
        gsPathToMonitorForDownloads = getInputData().getString(KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS);
        giExpectedDownloadFileCount = getInputData().getInt(KEY_ARG_EXPECTED_DOWNLOAD_FILE_COUNT,0);
        gsVideoOutputFilename = getInputData().getString(KEY_ARG_VIDEO_OUTPUT_FILENAME);
    }

    @NonNull
    @Override
    public Result doWork() {

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
        if( giExpectedDownloadFileCount == 0){
            String sFailureMessage = "No expected file download count provided.";
            return Result.failure(DataErrorMessage(sFailureMessage));
        }

        //Establish the name of the temporary output folder for the video concatenation result,
        // and create the folder:
        String sOutputFolder = "Output";
        String sOutputFolderPath = fThisDownloadFolder + File.separator + sOutputFolder;
        File fOutputFolder = new File(sOutputFolderPath);
        int iOutputFolderRetryIterator = 1;
        while(fOutputFolder.exists()){
            sOutputFolder = "Output_" + iOutputFolderRetryIterator;
            iOutputFolderRetryIterator++;
            sOutputFolderPath = fThisDownloadFolder + File.separator + sOutputFolder;
            fOutputFolder = new File(sOutputFolderPath);
        }
        if(!fOutputFolder.mkdir()){
            String sFailureMessage = "Could not create folder at " + fOutputFolder.getAbsolutePath();
            return Result.failure(DataErrorMessage(sFailureMessage));
        }

        /*//https://developer.android.com/topic/libraries/architecture/workmanager/how-to/intermediate-progress
        try {
            // Doing work.
            int iDenominator = 120;
            int iNumerator;
            if(iSavedNumerator == -1){
                iNumerator = 0;
            } else {
                iNumerator = iSavedNumerator;
            }

            while( iNumerator < iDenominator) {
                Thread.sleep(1000);
                int iProgress = (int) ((iNumerator / (float) iDenominator) * 100);
                //Build data for any observers to take note:
                Data data = new Data.Builder()
                        .putInt(PROGRESS,iProgress)
                        .putString(FILENAME, sVideoOutputFilename)
                        .build();
                setProgressAsync(data);
                iNumerator++;
                iSavedNumerator = iNumerator;
            }
        } catch (InterruptedException exception) {
            // ... handle exception
        }
        // Set progress to 100 after you are done doing your work.
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());*/

        //Monitor the location for file downloads' completion:
        int iElapsedWaitTime = 0;
        int iWaitDuration = 5000; //milliseconds
        boolean bFileDownloadsComplete = false;
        File[] fDownloadedFiles = null;
        while( (iElapsedWaitTime < GlobalClass.VIDEO_DOWNLOAD_WAIT_TIMEOUT) && !bFileDownloadsComplete) {
            try {
                Thread.sleep(iWaitDuration);
                iElapsedWaitTime += iWaitDuration;
                fDownloadedFiles = fThisDownloadFolder.listFiles();
                if(fDownloadedFiles == null){
                    break;
                }
                int iFileCount = 0;
                for(File f:fDownloadedFiles){
                    if(f.isFile()){
                        iFileCount++;
                    }
                }
                if(iFileCount == giExpectedDownloadFileCount){
                    bFileDownloadsComplete = true;
                }


            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        if(bFileDownloadsComplete) {
            if(giDownloadTypeSingleOrM3U8 == DOWNLOAD_TYPE_M3U8) {
                //Process the files.

                //Create a file listing the files which are to be concatenated:
                String sFFMPEGInputFilename = "FFMPEGInputFileName.txt";
                String sFFMPEGInputFilePath = sOutputFolderPath + File.separator + sFFMPEGInputFilename;
                File fFFMPEGInputFile = new File(sFFMPEGInputFilePath);

                StringBuilder sbBuffer = new StringBuilder();
                for (File f : fDownloadedFiles) {
                    sbBuffer.append("file '");
                    sbBuffer.append(f.getAbsolutePath());
                    sbBuffer.append("'\n");
                }

                //Write the data to the file:
                try {
                    FileWriter fwFFMPEGInputFile = null;
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
                String sCommand = "-f concat -safe 0 -i " + sFFMPEGInputFilePath + " -c copy " + sOutputFolderPath + File.separator + gsVideoOutputFilename;
                FFmpegKit.executeAsync(sCommand, new ExecuteCallback() {

                    @Override
                    public void apply(Session session) {
                        // CALLED WHEN SESSION IS EXECUTED
                        SessionState state = session.getState();
                        ReturnCode returnCode = session.getReturnCode();

                        //broadcastLogMessage(String.format("\nFFmpeg process exited with state %s and return code %s.\n", state, returnCode) + "\n");

                        String sMessage = session.getFailStackTrace();
                        if(sMessage != null) {
                            //broadcastLogMessage(sMessage + "\n");
                        }
                    }
                }, new LogCallback() {

                    @Override
                    public void apply(com.arthenica.ffmpegkit.Log log) {
                        // CALLED WHEN SESSION PRINTS LOGS
                        String sMessage = log.getMessage();
                        //broadcastLogMessage(sMessage);
                    }
                }, new StatisticsCallback() {

                    @Override
                    public void apply(Statistics statistics) {
                        // CALLED WHEN SESSION GENERATES STATISTICS
                        String sMessage = "File size: " + String.valueOf(statistics.getSize());
                        //broadcastLogMessage(sMessage + "\n");
                    }
                });

                //Delete the downloaded files, FFMPEG input file:


                //Move the output file to the temporary download folder (for the main program to "find":


            }
            return Result.success();
        } else {
            String sFailureMessage = "Download wait timeout exceeded. Wait time " + iElapsedWaitTime + " milliseconds.";
            return Result.failure(DataErrorMessage(sFailureMessage));
        }


    }


    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(FAILURE_MESSAGE, sMessage)
                .build();
    }


}
