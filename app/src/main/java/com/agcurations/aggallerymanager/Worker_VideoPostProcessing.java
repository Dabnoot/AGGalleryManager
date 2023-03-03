package com.agcurations.aggallerymanager;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/*import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.LogCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.SessionState;
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;*/
import com.google.android.exoplayer2.util.MimeTypes;

public class Worker_VideoPostProcessing extends Worker {

    //Define string used to identify this worker type:
    public static final String WORKER_VIDEO_POST_PROCESSING_TAG = "WORKER_VIDEO_POST_PROCESSING_TAG";

    public static final String VIDEO_DLID_AND_SEQUENCE_FILE_NAME = "DLID_And_Sequence.txt";

    DownloadManager downloadManager;

    //=========================
    // Define keys for arguments passed to this worker:
    public static final String KEY_ARG_DOWNLOAD_TYPE_SINGLE_M3U8_M3U8LOCAL = "KEY_ARG_DOWNLOAD_TYPE_SINGLE_M3U8_M3U8LOCAL";
    public static final String KEY_ARG_URI_PATH_TO_MONITOR_FOR_DOWNLOADS = "KEY_ARG_URI_PATH_TO_MONITOR_FOR_DOWNLOADS";
    public static final String KEY_ARG_URI_PATH_TO_WORKING_FOLDER = "KEY_ARG_URI_PATH_TO_WORKING_FOLDER";
    //public static final String KEY_ARG_FILENAME_SEQUENCE = "KEY_ARG_FILENAME_SEQUENCE";
    public static final String KEY_ARG_VIDEO_OUTPUT_FILENAME = "KEY_ARG_VIDEO_OUTPUT_FILENAME";
    public static final String KEY_ARG_VIDEO_TOTAL_FILE_SIZE = "KEY_ARG_VIDEO_TOTAL_FILE_SIZE";
    public static final String KEY_ARG_DOWNLOAD_IDS = "KEY_ARG_DOWNLOAD_IDS";
    public static final String KEY_ARG_ITEM_ID = "KEY_ARG_ITEM_ID";

    //=========================
    int giDownloadTypeSingleOrM3U8;
    public static final int DOWNLOAD_TYPE_SINGLE = 1;
    public static final int DOWNLOAD_TYPE_M3U8 = 2;
    String gsUriPathToMonitorForDownloads;
    DocumentFile gdfFolderToMonitorForDownloads;                 //Location to monitor for download file(s)
    String gsUriPathToWorkingFolder;
    DocumentFile gdfWorkingFolder;                             //Location on external SD Card (big storage)
    int giExpectedDownloadFileCount;                    //Expected count of downloaded files
    String[] gsFilenameSequence;
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
        giDownloadTypeSingleOrM3U8 = getInputData().getInt(KEY_ARG_DOWNLOAD_TYPE_SINGLE_M3U8_M3U8LOCAL, DOWNLOAD_TYPE_SINGLE);

        gsUriPathToMonitorForDownloads = getInputData().getString(KEY_ARG_URI_PATH_TO_MONITOR_FOR_DOWNLOADS);
        gdfFolderToMonitorForDownloads = DocumentFile.fromSingleUri(getApplicationContext(), Uri.parse(gsUriPathToMonitorForDownloads));

        gsUriPathToWorkingFolder = getInputData().getString((KEY_ARG_URI_PATH_TO_WORKING_FOLDER));
        gdfWorkingFolder =  DocumentFile.fromSingleUri(getApplicationContext(), Uri.parse(gsUriPathToWorkingFolder));

        glTotalFileSize = getInputData().getLong(KEY_ARG_VIDEO_TOTAL_FILE_SIZE,0);

        gsItemID = getInputData().getString(KEY_ARG_ITEM_ID);
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String sMessage;

        if(!globalClass.CheckIfFileExists(GlobalClass.gUriLogsFolder)){
            sMessage = "Logs folder missing. Restarting app should create the folder.";
            return Result.failure(DataErrorMessage(sMessage));
        }

        //Put the log file in the logs folder:
        ContentResolver contentResolver = GlobalClass.gcrContentResolver;

        String sLogFileName = gsItemID + "_" + GlobalClass.GetTimeStampFileSafe() + "_Video_WorkerLog.txt";
        Uri uriLogFile = null;
        try {
            uriLogFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, GlobalClass.gUriLogsFolder, MimeTypes.BASE_TYPE_TEXT, sLogFileName);
        } catch (FileNotFoundException e) {
            Toast.makeText(getApplicationContext(), "Could not create log file to monitor video post processing.", Toast.LENGTH_SHORT).show();
            return Result.failure();
        }
        if(uriLogFile == null){
            Toast.makeText(getApplicationContext(), "Could not create log file to monitor video post processing.", Toast.LENGTH_SHORT).show();
            return Result.failure();
        }
        BufferedWriter bwLogFile; //Use a buffered writer to write the log file to avoid slowing performance
                                  //  due to frequent file write operations.

        try {
            OutputStream osLogFile;
            osLogFile = contentResolver.openOutputStream(uriLogFile, "wt");
            if(osLogFile == null){
                Toast.makeText(getApplicationContext(), "Could not write log file to monitor video post processing.", Toast.LENGTH_SHORT).show();
                return Result.failure();
            }
            bwLogFile = new BufferedWriter(new OutputStreamWriter(osLogFile));

            boolean bDownloadFolderTrouble;
            bDownloadFolderTrouble = (gdfFolderToMonitorForDownloads == null);
            if (!bDownloadFolderTrouble) {
                bDownloadFolderTrouble = !gdfFolderToMonitorForDownloads.exists();
            }
            if (!bDownloadFolderTrouble) {
                sMessage = "Folder to monitor for downloads does not exist: " + gsUriPathToMonitorForDownloads;
                LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                return Result.failure(DataErrorMessage(sMessage));
            }

            boolean bWorkingFolderTrouble;
            bWorkingFolderTrouble = (gdfWorkingFolder == null);
            if(!bWorkingFolderTrouble){
                bWorkingFolderTrouble = !gdfWorkingFolder.exists();
            }
            if (bWorkingFolderTrouble) {
                sMessage = "Trouble with identification of download working folder: " + gsUriPathToWorkingFolder;
                LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                return Result.failure(DataErrorMessage(sMessage));
            }

            DocumentFile dfFileSequenceFile = gdfWorkingFolder.findFile(VIDEO_DLID_AND_SEQUENCE_FILE_NAME);
            if(dfFileSequenceFile == null){
                sMessage = "Could not find download file sequence file: " + VIDEO_DLID_AND_SEQUENCE_FILE_NAME;
                LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                return Result.failure(DataErrorMessage(sMessage));
            }
            if (dfFileSequenceFile.exists()) {
                //Get data from file:
                BufferedReader brReader;
                try {
                    InputStream isFileSequenceFile = GlobalClass.gcrContentResolver.openInputStream(dfFileSequenceFile.getUri());
                    if(isFileSequenceFile == null){
                        sMessage = "Could not open download file sequence file for reading: " + VIDEO_DLID_AND_SEQUENCE_FILE_NAME;
                        LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                        return Result.failure(DataErrorMessage(sMessage));
                    }

                    brReader = new BufferedReader(new InputStreamReader(isFileSequenceFile));
                    String sLine = brReader.readLine();
                    ArrayList<Long> allDownloadIDs = new ArrayList<>();
                    ArrayList<String> alsDownloadFileSequence = new ArrayList<>();
                    int j = 0;
                    while (sLine != null) {
                        j++;
                        if (!sLine.equals("")) {
                            String[] sTemp = sLine.split("\t");
                            if (sTemp.length == 2) {
                                long lTemp;
                                try {
                                    lTemp = Long.parseLong(sTemp[0]);
                                    allDownloadIDs.add(lTemp);
                                    alsDownloadFileSequence.add(sTemp[1]);
                                } catch (Exception e) {
                                    sMessage = "Could not parse long while reading file sequence file, line " + j + ".";
                                    LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                                    return Result.failure(DataErrorMessage(sMessage));
                                }

                            } else {
                                sMessage = "Data missing while reading file sequence file, line " + j + ".";
                                LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                                return Result.failure(DataErrorMessage(sMessage));
                            }
                        }
                        sLine = brReader.readLine();
                    }
                    brReader.close();
                    isFileSequenceFile.close();

                    gsFilenameSequence = new String[alsDownloadFileSequence.size()];
                    gsFilenameSequence = alsDownloadFileSequence.toArray(gsFilenameSequence);
                    giExpectedDownloadFileCount = gsFilenameSequence.length;
                    glDownloadIDs = new long[allDownloadIDs.size()];
                    for (int l = 0; l < allDownloadIDs.size(); l++) {
                        glDownloadIDs[l] = allDownloadIDs.get(l);
                    }

                } catch (IOException e) {
                    sMessage = "Problem reading file sequence file.";
                    LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                    return Result.failure(DataErrorMessage(sMessage));
                }

            }


            if (giExpectedDownloadFileCount == 0) {
                sMessage = "No expected file download count provided.";
                LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                return Result.failure(DataErrorMessage(sMessage));
            }






            //Establish the name of the temporary output folder for the video concatenation result,
            // and create the folder:
            String sOutputFolder = "Final";
            DocumentFile dfOutputFolder = gdfWorkingFolder.findFile(sOutputFolder);
            int iOutputFolderRetryIterator = 1;
            while (dfOutputFolder != null) {
                sOutputFolder = "Final_" + iOutputFolderRetryIterator;
                iOutputFolderRetryIterator++;
                dfOutputFolder = gdfWorkingFolder.findFile(sOutputFolder);
            }

            bwLogFile.write("Creating output folder: " + sOutputFolder + "\n");
            bwLogFile.flush();
            dfOutputFolder = gdfWorkingFolder.createDirectory(sOutputFolder);
            if (dfOutputFolder == null) {
                sMessage = "Could not create output folder " + sOutputFolder;
                LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                return Result.failure(DataErrorMessage(sMessage));
            }
            bwLogFile.write("Success." + "\n");
            bwLogFile.flush();


            //Monitor the location for file downloads' completion:
            int iElapsedWaitTime = 0;
            int iWaitDuration = 5000; //milliseconds
            boolean bFileDownloadsComplete = false;
            boolean bDownloadProblem = false;
            boolean bPaused = false;
            DocumentFile[] dfDownloadedFiles;
            String sDownloadFailedReason = "";
            String sDownloadPausedReason = "";

            ArrayList<Long> alRemainingDownloadIDs = new ArrayList<>();
            for (long glDownloadID : glDownloadIDs) {
                alRemainingDownloadIDs.add(glDownloadID);
            }

            sMessage = "Waiting for download(s) to complete, a maximum of " + (GlobalClass.DOWNLOAD_WAIT_TIMEOUT / 1000) + " seconds.";
            bwLogFile.write(sMessage + "\n");
            bwLogFile.flush();
            while ((iElapsedWaitTime < GlobalClass.DOWNLOAD_WAIT_TIMEOUT) && !bFileDownloadsComplete && !bDownloadProblem) {

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
                bwLogFile.write(".");
                bwLogFile.flush();

                //Query for remaining downloads:
                long[] lRemainingDownloadIDs = new long[alRemainingDownloadIDs.size()];
                for(int i = 0; i < alRemainingDownloadIDs.size(); i++){
                    lRemainingDownloadIDs[i] = alRemainingDownloadIDs.get(i);
                }
                DownloadManager.Query dmQuery = new DownloadManager.Query();
                dmQuery.setFilterById(lRemainingDownloadIDs);
                downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
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
                                bwLogFile.write(sMessage + "\n\n");
                                bwLogFile.flush();
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
                                bwLogFile.write(sMessage + "\n\n");
                                bwLogFile.flush();

                                break;
                            case DownloadManager.STATUS_PENDING:
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
                                Uri uriLocalURI = Uri.parse(sLocalURI);
                                DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriLocalURI);
                                if(dfSource == null){
                                    sMessage = "Could not determine/find document file from " + sLocalURI;
                                    LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                                    return Result.failure(DataErrorMessage(sMessage));
                                }
                                String sFileName = dfSource.getName();
                                bwLogFile.write("Download completed: " + sFileName);
                                if (dfSource.exists()) {
                                    if(dfSource.getParentFile() == null){
                                        sMessage = "Could not determine source file parent documentfile.";
                                        LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                                        return Result.failure(DataErrorMessage(sMessage));
                                    }
                                    Uri uriMovedFile = DocumentsContract.moveDocument(contentResolver, dfSource.getUri(), dfSource.getParentFile().getUri(), gdfWorkingFolder.getUri());
                                    if(uriMovedFile == null){
                                        sMessage = "Failed to move file " + sFileName + " from the download source folder to the working folder.";
                                        LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                                        return Result.failure(DataErrorMessage(sMessage));
                                    }


                                    /*String sDestination = dfWorkingFolder + File.separator + sFileName;
                                    DocumentFile dfDestination = new File(sDestination);
                                    //Move the file to the working folder:
                                    if (!dfDestination.exists()) {
                                        try {
                                            InputStream inputStream;
                                            OutputStream outputStream;
                                            inputStream = new FileInputStream(dfSource.getPath());
                                            outputStream = new FileOutputStream(dfDestination.getPath());
                                            byte[] buffer = new byte[100000];
                                            while ((inputStream.read(buffer, 0, buffer.length)) >= 0) {
                                                outputStream.write(buffer, 0, buffer.length);
                                            }
                                            outputStream.flush();
                                            outputStream.close();
                                            bwLogFile.write(" Copied to working folder.");
                                            if (!dfSource.delete()) {
                                                sMessage = "Could not delete source file after copy. Source: " + dfSource.getAbsolutePath();
                                                bwLogFile.write("Download monitoring: " + sMessage + "\n");
                                            } else {
                                                bwLogFile.write(" Source file deleted.");
                                                if(!alRemainingDownloadIDs.remove(lDownloadID)){
                                                    sMessage = "Could not remove download ID " + lDownloadID + " from download monitoring. This is a non-critical issue.";
                                                    bwLogFile.write("Download monitoring: " + sMessage + "\n");
                                                }
                                            }
                                        } catch (Exception e) {
                                            sMessage = dfSource.getPath() + "\n" + e.getMessage();
                                            bwLogFile.write("Stream copy exception: " + sMessage + "\n");
                                        }
                                    } //End if !FDestination.exists. If it does exist, we have already copied the file over.*/
                                } else { //End if dfSource.exists. If it does not exist, we probably already moved it.
                                    bwLogFile.write(" Source file does not exist (already moved?).");
                                }
                                bwLogFile.write("\n");
                                bwLogFile.flush();

                                break;
                        }
                    } while (cursor.moveToNext() && bFileDownloadsComplete && !bDownloadProblem); //End loop through download query results.


                } //End if cursor has a record.

            } //End loop waiting for download completion.
            if (bFileDownloadsComplete) {
                bwLogFile.write("\nAll downloads reported as completed." + "\n");
            } else {
                bwLogFile.write("\nA download may have failed." + "\n");
                if (iElapsedWaitTime >= GlobalClass.DOWNLOAD_WAIT_TIMEOUT) {
                    sMessage = "Download elapsed time exceeds timeout of " + (GlobalClass.DOWNLOAD_WAIT_TIMEOUT / 1000) + " seconds.";
                    bwLogFile.write(sMessage + "\n");
                }
            }
            bwLogFile.flush();

            //Downloads should be complete and moved out of the source folder.
            if(!dfFileSequenceFile.delete()){
                sMessage = "Could not delete download ID and sequence file.";
                bwLogFile.write(sMessage + "\n");
            }


            //Delete the download folder to which downloadManager downloaded the files:
            bwLogFile.write("Attempting delete of download folder:" + gdfFolderToMonitorForDownloads.getUri() + "\n");
            if (gdfFolderToMonitorForDownloads.exists()) {
                //Preserve a link to the download folder's parent folder (category folder) so that
                //  we can check to see if it is empty.
                //  This may be like an vestigial item in that the program was coded to create "category" folders
                //  to attempt to sort files. The category was the first tag selected, or "no tag".
                //  This is a category folder that is created in the internal downloads folder used by
                //  this program.
                DocumentFile dfCategoryFolder = gdfFolderToMonitorForDownloads.getParentFile();
                if (!gdfFolderToMonitorForDownloads.delete()) {
                    sMessage = "Could not delete download folder: " + gdfFolderToMonitorForDownloads.getUri();
                    bwLogFile.write(sMessage + "\n");
                } else {
                    bwLogFile.write("Success." + "\n");
                    bwLogFile.flush();
                    //Attempt to remove the category folder from the download location if it is empty:

                    if (dfCategoryFolder != null) {
                        if (dfCategoryFolder.listFiles().length == 0) {
                            bwLogFile.write("Attempting to delete category folder from download location.\n");
                            bwLogFile.flush();
                            if (!dfCategoryFolder.delete()) {
                                //Unable to remove stub folder.
                                sMessage = "Could not delete temp category folder: " + dfCategoryFolder.getUri();
                                bwLogFile.write(sMessage + "\n");
                            } else {
                                bwLogFile.write("Success." + "\n");
                            }

                        }
                    }
                }
            }
            bwLogFile.flush();



            dfDownloadedFiles = gdfWorkingFolder.listFiles();
            if (dfDownloadedFiles.length < 2) {
                sMessage = "Downloaded file(s) missing from working folder: " + gdfWorkingFolder.getUri();
                LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                return Result.failure(DataErrorMessage(sMessage));
            }

            if (giDownloadTypeSingleOrM3U8 == DOWNLOAD_TYPE_SINGLE) {
                //Move the file to the output folder to get captured by the main program:
                DocumentFile dfInputFile = null; //There should be but 1 file in the working folder for DOWNLOAD_TYPE_SINGLE.
                for (DocumentFile f : dfDownloadedFiles) {
                    if (f.isFile()) {
                        dfInputFile = f;
                        break;
                    }
                }

                if(dfInputFile == null){
                    sMessage = "Could not find downloaded file in expected location: " + gdfWorkingFolder.getUri();
                    LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                    return Result.failure(DataErrorMessage(sMessage));
                }

                bwLogFile.write("Moving downloaded file to output folder: " + dfInputFile.getUri() + "\n");
                Uri uriMovedDocumentUri = DocumentsContract.moveDocument(contentResolver, dfInputFile.getUri(), gdfWorkingFolder.getUri(), dfOutputFolder.getUri());
                if(uriMovedDocumentUri == null){
                    sMessage = "Could not move downloaded file to output folder from: " + dfInputFile.getUri();
                    LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                    return Result.failure(DataErrorMessage(sMessage));
                }

                //todo: clean up this garbage:
                /*DocumentFile fOutputFile = new File(sFinalOutputPath);
                if (dfInputFile != null) {
                    bwLogFile.write("Moving downloaded file to output folder: " + dfInputFile.getUri() + "\n");
                    if (!dfInputFile.renameTo(fOutputFile)) {
                        sMessage = "Could not move downloaded file to output folder: " + dfInputFile.getAbsolutePath();
                        LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                        return Result.failure(DataErrorMessage(sMessage));
                    }
                } else {
                    sMessage = "Download file missing from folder: " + fThisWorkingFolder.getAbsolutePath();
                    LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                    return Result.failure(DataErrorMessage(sMessage));
                }*/
                bwLogFile.write("Success." + "\n");
                bwLogFile.flush();

            }
            bwLogFile.flush();
            bwLogFile.close();
            osLogFile.flush();
            osLogFile.close();
            return Result.success();

        } catch (Exception e){
            sMessage = e.getMessage();
            if(sMessage == null){
                sMessage = "Null message";
            }
            Log.d("Video Worker", sMessage) ;
            return Result.failure(DataErrorMessage(sMessage));
        }





    }



    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }

    private void LogReturnWithFailure(String sMessage, OutputStream osLogFile, BufferedWriter bwLogFile){
        try {
            bwLogFile.write(sMessage + "\n");
            bwLogFile.flush();
            bwLogFile.close();
            osLogFile.flush();
            osLogFile.close();
        }catch (Exception e){
            if(e.getMessage() != null) {
                Log.d("Log File Issue", e.getMessage());
            } else {
                Log.d("Log File Issue", "Problem writing log file during video post processing.");
            }
        }
    }

}
