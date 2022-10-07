package com.agcurations.aggallerymanager;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_LocalFileTransfer extends Worker {

    //Define string used to identify this worker type:
    public static final String WORKER_LOCAL_FILE_TRANSFER_TAG = "WORKER_LOCAL_FILE_TRANSFER_TAG";

    public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";

    //=========================
    // Define keys for arguments passed to this worker:
    public static final String KEY_ARG_JOB_REQUEST_DATETIME = "KEY_ARG_JOB_REQUEST_DATETIME";
    public static final String KEY_ARG_JOB_FILE = "KEY_ARG_JOB_FILE";
    //public static final String KEY_ARG_MEDIA_CATEGORY = "KEY_ARG_MEDIA_CATEGORY";
    //public static final String KEY_ARG_COPY_OR_MOVE = "KEY_ARG_COPY_OR_MOVE";
    //public static final String KEY_ARG_TOTAL_IMPORT_SIZE_BYTES = "KEY_ARG_TOTAL_IMPORT_SIZE_BYTES";

    public static final String JOB_PROGRESS = "JOB_PROGRESS";
    public static final String JOB_BYTES_PROCESSED = "JOB_BYTES_PROCESSED";
    public static final String JOB_BYTES_TOTAL = "JOB_BYTES_TOTAL";

    public static final String JOB_DATETIME = "JOB_DATETIME";

    //public static final int LOCAL_FILE_TRANSFER_MOVE = 0;
    //public static final int LOCAL_FILE_TRANSFER_COPY = 1;

    //=========================
    String gsJobRequestDateTime;    //Date/Time of job request for logging purposes.
    String gsJobFile;               //Name of file containing a list of files to transfer
    long glProgressNumerator = 0L;
    long glProgressDenominator;
    int giFileCount;
    int giFilesProcessed;
    String gsMoveCopyPastTense;

    GlobalClass globalClass;
    int giNotificationID;
    Notification gNotification;
    NotificationCompat.Builder gNotificationBuilder;

    FileWriter gfwLogFile;
    //=========================

    public Worker_LocalFileTransfer(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters) {
        super(appContext, workerParameters);
        // Set initial progress to 0
        setProgressAsync(new Data.Builder().putInt(JOB_PROGRESS, 0).build());

        gsJobRequestDateTime = getInputData().getString(KEY_ARG_JOB_REQUEST_DATETIME);
        gsJobFile = getInputData().getString(KEY_ARG_JOB_FILE);
    }

    @NonNull
    @Override
    public Result doWork() {

        Data dataProgress;

        globalClass = (GlobalClass) getApplicationContext();

        String sMessage;

        if(!globalClass.gfLogsFolder.exists()){
            sMessage = "Logs folder missing. Restarting app should create the folder.";
            globalClass.gbImportExecutionRunning = false;
            globalClass.gbImportExecutionFinished = true;
            return Result.failure(DataErrorMessage(sMessage));

        }

        //Put the log file in the logs folder:
        String sLogFilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                File.separator + gsJobRequestDateTime + "_" + GlobalClass.GetTimeStampFileSafe() + "_LocalFileTransfer_WorkerLog.txt";
        File fLog = new File(sLogFilePath);

        try { //Required for the log file.

            gfwLogFile = new FileWriter(fLog, true);

            //Validate data
            if (gsJobFile.equals("")) {
                sMessage = "No job file name provided. This is the file telling the worker what files to copy, and where to place them.";
                gfwLogFile.write(sMessage + "\n");
                gfwLogFile.close();
                globalClass.BroadcastProgress(true, sMessage,
                        false, 0,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;
                return Result.failure(DataErrorMessage(sMessage));
            }

            String sJobFilePath = globalClass.gfJobFilesFolder.getAbsolutePath() +
                    File.separator + gsJobFile;
            File fJobFile = new File(sJobFilePath);

            if (fJobFile.exists()) {

                boolean bProblemWithFileTransfer = false;

                //Get data from file:
                //File Format:
                //Source File Uri/tDestination Folder/tDestination Filename
                // ALL FILENAMES MUST BE UNIQUE! Otherwise this routine will think that the copy has
                // already taken place and will either skip the copy or, in the case of a move op,
                //  will delete the source file.
                int SOURCE_FILE_URI_INDEX = 0;
                int DESTINATION_FOLDER = 1;
                int DESTINATION_FILENAME = 2;
                int SOURCE_FILE_SIZE_BYTES = 3;
                int SOURCE_FILE_DELETE_ONLY = 4; //If the user is not importing this item and has marked it for deletion.

                long lLoopBytesRead;

                BufferedReader brReader;
                try { //Required for the job file.
                    brReader = new BufferedReader(new FileReader(fJobFile.getAbsolutePath()));

                    int iMediaCategory;
                    int iMoveOrCopy;

                    //Read the header and get the media category and specified move/copy behavior:
                    String sConfig = brReader.readLine();
                    try {  //A Try/Catch to simplify error messaging with examination of the job file header.
                        String[] sData = sConfig.split("\t");
                        if (sData.length != 4) {
                            throw new Exception("Data header missing data in job file.");
                        }
                    /*
                    "MediaCategory:1\tMoveOrCopy:0\tTotalSize:999999"
                    * */
                        //Determine media category:
                        String[] sMediaCategoryData = sData[0].split(":");
                        if (sMediaCategoryData.length != 2) {
                            throw new Exception("Data header missing data for media category in job file.");
                        }
                        if (sMediaCategoryData[1].equals(GlobalClass.gsCatalogFolderNames[2])){
                            iMediaCategory = 2; //Comics only if specifically coded to do so. This worker not designed for this at origin.
                        } else if(sMediaCategoryData[1].equals(GlobalClass.gsCatalogFolderNames[0])){
                            iMediaCategory = 0; //Videos
                        } else {
                            iMediaCategory = 1; //default to Images.
                        }

                        //Determine move or copy:
                        String[] sMoveOrCopyData = sData[1].split(":");
                        if (sMoveOrCopyData.length != 2) {
                            throw new Exception("Data header missing data for 'move or copy behavior selection' in job file.");
                        }
                        if(sMoveOrCopyData[1].equals(GlobalClass.gsMoveOrCopy[GlobalClass.MOVE])){
                            iMoveOrCopy = GlobalClass.MOVE;
                        } else {
                            iMoveOrCopy = GlobalClass.COPY;
                        }

                        //Read total size:
                        String[] sTotalSizeData = sData[2].split(":");
                        if (sTotalSizeData.length != 2) {
                            throw new Exception("Data header missing data for 'total file transfer size' in job file.");
                        }
                        glProgressDenominator = Long.parseLong(sTotalSizeData[1]);

                        //Read file count:
                        String[] sFileCountData = sData[3].split(":");
                        if (sFileCountData.length != 2) {
                            throw new Exception("Data header missing data for 'file count' in job file.");
                        }
                        giFileCount = Integer.parseInt(sFileCountData[1]);

                    } catch (Exception e){
                        sMessage = e.getMessage();
                        gfwLogFile.write(sMessage + "\n");
                        gfwLogFile.close();
                        globalClass.BroadcastProgress(true, sMessage,
                                false, 0,
                                false, "",
                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                        globalClass.gbImportExecutionRunning = false;
                        globalClass.gbImportExecutionFinished = true;
                        return Result.failure(DataErrorMessage(sMessage));
                    }

                    //Prepare a notification for the notification bar:
                    String sNotificationTitle = "File " + GlobalClass.gsMoveOrCopy[iMoveOrCopy].toLowerCase() + " job " + gsJobRequestDateTime + ".";
                    gsMoveCopyPastTense = (iMoveOrCopy == GlobalClass.MOVE) ?  "moved" : "copied";
                    String sNotificationText = giFilesProcessed + "/" + giFileCount + " files " + gsMoveCopyPastTense + ".";
                    gNotificationBuilder = new NotificationCompat.Builder(getApplicationContext(), GlobalClass.NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(R.drawable.copy)
                            .setContentTitle(sNotificationTitle)
                            .setContentText(sNotificationText)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setOnlyAlertOnce(true) //Alert once and then update the notification silently.
                            .setOngoing(false) //Prevents the user from swiping it off the notification area.
                            .setProgress(100, 0, false);
                    giNotificationID = globalClass.iNotificationID;
                    globalClass.iNotificationID++;
                    gNotification = gNotificationBuilder.build();
                    globalClass.notificationManager.notify(giNotificationID, gNotification);

                    //Build progress data associated with this worker:
                    dataProgress = new Data.Builder()
                            .putLong(JOB_BYTES_PROCESSED, glProgressNumerator)
                            .putLong(JOB_BYTES_TOTAL, glProgressDenominator)
                            .putString(JOB_DATETIME, gsJobRequestDateTime)
                            .build();
                    setProgressAsync(dataProgress);

                    int iProgressBarValue = Math.round((glProgressNumerator / (float) glProgressDenominator) * 100);
                    globalClass.BroadcastProgress(true, "Background working processing files...",
                            true, iProgressBarValue,
                            true, sNotificationText,
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                    //Read the data records in the job file and move/copy files:
                    do {
                        String sLine = brReader.readLine();
                        if(sLine == null){
                            break;
                        }
                        globalClass.BroadcastProgress(false, "",
                                false, 0,
                                true, giFilesProcessed + "/" + giFileCount,
                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                        giFilesProcessed++;
                        if (!sLine.equals("")) {
                            String[] sTemp = sLine.split("\t");
                            if (sTemp.length == 5) {

                                String sDestinationFolder = globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator + sTemp[DESTINATION_FOLDER];
                                String sDestinationFileName = sTemp[DESTINATION_FILENAME];
                                long lFileSize = Long.parseLong(sTemp[SOURCE_FILE_SIZE_BYTES]);

                                //Check if source file exists:
                                String sSourceFileUri = sTemp[SOURCE_FILE_URI_INDEX];
                                Uri uriSourceFile = Uri.parse(sSourceFileUri);
                                if(uriSourceFile == null){
                                    continue;
                                }
                                DocumentFile dfSource = null;
                                File fSourceFile = null;
                                boolean bUseFileRatherThanUri = false;
                                boolean bTryAlternate = false;
                                try {
                                    dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
                                    if (dfSource == null) {
                                        continue;
                                    }
                                    if (dfSource.getName() == null) {
                                        bTryAlternate = true;
                                    }
                                } catch (Exception e){
                                    bTryAlternate = true;
                                }
                                if(bTryAlternate){
                                    //If there was a problem here, then it might not be a Uri but rather a file address to the holding folder. Test.
                                    fSourceFile = new File(Objects.requireNonNull(uriSourceFile.getPath()));
                                    if(fSourceFile.exists()){
                                        bUseFileRatherThanUri = true;
                                    } else {
                                        continue;
                                    }
                                }

                                boolean bMarkedForDeletion = Boolean.parseBoolean(sTemp[SOURCE_FILE_DELETE_ONLY]);

                                String sLogLine;

                                String sFileName = "";
                                if(bUseFileRatherThanUri){
                                    sFileName = fSourceFile.getName();
                                } else {
                                    sFileName = dfSource.getName();
                                }

                                if(bMarkedForDeletion) {
                                    //If this source item is marked for deletion (no move or copy op to be performed), delete the source file:
                                    boolean bDeleteSuccess;
                                    if(bUseFileRatherThanUri){
                                        glProgressNumerator = glProgressNumerator + fSourceFile.length();
                                        bDeleteSuccess = fSourceFile.delete();
                                    } else {
                                        glProgressNumerator = glProgressNumerator + dfSource.length();
                                        bDeleteSuccess = dfSource.delete();
                                    }
                                    UpdateProgressOutput();

                                    if (!bDeleteSuccess) {
                                        sLogLine = "Could not delete file marked for deletion: " + sFileName;
                                    } else {
                                        sLogLine = "Success deleting file marked for deletion: " + sFileName;
                                    }
                                    gfwLogFile.write(sLogLine + "\n");
                                    globalClass.BroadcastProgress(true, sLogLine + "\n",
                                            false, 0,
                                            false, "",
                                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                                } else {
                                    //If this item is not merely marked for deletion...

                                    File fDestinationFolder = new File(sDestinationFolder);
                                    if (!fDestinationFolder.exists()) {
                                        if (!fDestinationFolder.mkdir()) {
                                            sMessage = "Could not create destination folder \"" + sDestinationFolder + "\" for file \""
                                                    + sDestinationFileName + "\", line " + giFilesProcessed + ": " + sJobFilePath;
                                            gfwLogFile.write(sMessage + "\n");
                                            globalClass.BroadcastProgress(true, sMessage,
                                                    false, 0,
                                                    false, "",
                                                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                                            bProblemWithFileTransfer = true;
                                            continue; //Skip to the end of the loop and read the next line in the job file.
                                        }
                                    }
                                    //Destination folder exists or has been created successfully.

                                    boolean bFileExists;
                                    if(bUseFileRatherThanUri){
                                        bFileExists = fSourceFile.exists();
                                    } else {
                                        bFileExists = dfSource.exists();
                                    }

                                    if (bFileExists) {
                                        String sDestinationFileFullPath = sDestinationFolder + File.separator + sDestinationFileName;
                                        File fDestinationFile = new File(sDestinationFileFullPath);
                                        if (fDestinationFile.exists()) {
                                            //The file copy has already been executed by a previous instance of this requested worker.
                                            //If the operation was a move operation, we are here only because the source file still
                                            //  exists. Attempt to delete the source file.
                                            if (iMoveOrCopy == GlobalClass.MOVE) {
                                                boolean bDeleteSuccess;
                                                if(bUseFileRatherThanUri){
                                                    bDeleteSuccess = fSourceFile.delete();
                                                } else {
                                                    bDeleteSuccess = dfSource.delete();
                                                }

                                                if (!bDeleteSuccess) {
                                                    sMessage = "Source file copied, but could not delete source file as part of a 'move' operation. File \""
                                                            + dfSource.getName() + "\", job file line " + giFilesProcessed + " in job file " + sJobFilePath + ".";
                                                    gfwLogFile.write(sMessage + "\n");
                                                    globalClass.BroadcastProgress(true, sMessage + "\n",
                                                            false, 0,
                                                            false, "",
                                                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                                                    bProblemWithFileTransfer = true;
                                                }
                                            }
                                            glProgressNumerator = glProgressNumerator + lFileSize;
                                            continue; //Skip to the end of the loop and read the next line in the job file.
                                        }
                                        //Destination file does not exist.

                                        // Execute the copy or move operation:


                                        sLogLine = GlobalClass.gsMoveOrCopy[iMoveOrCopy + 1]
                                                + " file " + sFileName + " to " + fDestinationFile.getPath() + ".";
                                        gfwLogFile.write(sLogLine + "\n");
                                        globalClass.BroadcastProgress(true, sLogLine,
                                                false, 0,
                                                false, "",
                                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);


                                        ContentResolver contentResolver = getApplicationContext().getContentResolver();
                                        //InputStream inputStream = contentResolver.openInputStream(dfSource.getUri());
                                        FileInputStream inputStream;
                                        if(bUseFileRatherThanUri){
                                            inputStream = new FileInputStream(fSourceFile);
                                        } else {
                                            inputStream = (FileInputStream) contentResolver.openInputStream(dfSource.getUri());
                                        }

                                        OutputStream outputStream = new FileOutputStream(fDestinationFile.getPath());
                                        int iLoopCount = 0;
                                        byte[] buffer = new byte[100000];
                                        if (inputStream == null) {
                                            continue;
                                        }
                                        while ((lLoopBytesRead = inputStream.read(buffer, 0, buffer.length)) >= 0) {
                                            outputStream.write(buffer, 0, buffer.length);
                                            glProgressNumerator += lLoopBytesRead;
                                            iLoopCount++;
                                            if (iLoopCount % 10 == 0) {
                                                //Send update every 10 loops:
                                                UpdateProgressOutput();
                                            }

                                        }
                                        outputStream.flush();
                                        outputStream.close();

                                        sLogLine = " Success.";
                                        gfwLogFile.write(sLogLine + "\n");
                                        globalClass.BroadcastProgress(true, sLogLine + "\n",
                                                false, 0,
                                                false, "",
                                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                                        if (iMoveOrCopy == GlobalClass.MOVE) {
                                            boolean bDeleteSuccess;
                                            if(bUseFileRatherThanUri){
                                                bDeleteSuccess = fSourceFile.delete();
                                            } else {
                                                bDeleteSuccess = dfSource.delete();
                                            }
                                            if (!bDeleteSuccess) {
                                                sLogLine = "Could not delete source file after copy: " + sFileName;
                                            } else {
                                                sLogLine = "Success deleting source file after copy: " + sFileName;
                                            }
                                            gfwLogFile.write(sLogLine + "\n");
                                            globalClass.BroadcastProgress(true, sLogLine + "\n",
                                                    false, 0,
                                                    false, "",
                                                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                                        }

                                    } else {
                                        //If the source does not exist and was marked for transfer (not marked for deletion without transfer), assume that the file has already been transferred.
                                        //todo: make sure that the file exists in the destination, and if not, provide an error message.
                                        glProgressNumerator = glProgressNumerator + lFileSize;
                                    }

                                }
                                dataProgress = UpdateProgressOutput();

                            } else {
                                sMessage = "Data missing while reading job file, line " + giFilesProcessed + ": " + sJobFilePath;
                                gfwLogFile.write(sMessage + "\n");
                                gfwLogFile.close();
                                globalClass.BroadcastProgress(true, sMessage,
                                        false, 0,
                                        false, "",
                                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                                CloseNotification();
                                globalClass.gbImportExecutionRunning = false;
                                globalClass.gbImportExecutionFinished = true;
                                return Result.failure(DataErrorMessage(sMessage));
                            }
                        }
                        globalClass.BroadcastProgress(false, "",
                                false, 0,
                                true, giFilesProcessed + "/" + giFileCount,
                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                    } while (true);
                    brReader.close();

                    CloseNotification();

                } catch (IOException e) {
                    sMessage = "Problem reading job file: " + sJobFilePath;
                    gfwLogFile.write(sMessage + "\n");
                    gfwLogFile.close();
                    globalClass.BroadcastProgress(true, sMessage,
                            false, 0,
                            false, "",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    globalClass.gbImportExecutionRunning = false;
                    globalClass.gbImportExecutionFinished = true;
                    return Result.failure(DataErrorMessage(sMessage));
                }

                //Delete the job file if there were no problems:
                if(!bProblemWithFileTransfer){
                    if(!fJobFile.delete()){
                        sMessage = "Worker finished processing job but could not delete job file: " + sJobFilePath;
                        gfwLogFile.write(sMessage + "\n");
                        globalClass.BroadcastProgress(true, sMessage + "\n",
                                false, 0,
                                false, "",
                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    }
                }

            } else {
                sMessage = "Job file does not exist: " + sJobFilePath;
                gfwLogFile.write(sMessage + "\n");
                gfwLogFile.close();
                globalClass.BroadcastProgress(true, sMessage,
                        false, 0,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;
                return Result.failure(DataErrorMessage(sMessage));
            }

            gfwLogFile.close();

            globalClass.gbImportExecutionRunning = false;
            globalClass.gbImportExecutionFinished = true;
            return Result.success(dataProgress);

        } catch (Exception e){
            sMessage = e.getMessage();
            if(sMessage == null){
                sMessage = "Null message";
            }
            Log.d("Job Worker", sMessage) ;
            globalClass.BroadcastProgress(true, sMessage + "\n",
                    false, 0,
                    false, "",
                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            globalClass.gbImportExecutionRunning = false;
            globalClass.gbImportExecutionFinished = true;
            return Result.failure(DataErrorMessage(sMessage));
        }



    }

    private Data UpdateProgressOutput(){

        int iProgressBarValue = Math.round((glProgressNumerator / (float) glProgressDenominator) * 100);


        //Update the notification on the notification bar:
        //String sNotificationText = giFilesProcessed + "/" + giFileCount + " files " + gsMoveCopyPastTense + ".";
        String sNotificationText = giFilesProcessed + "/" + giFileCount + " files processed.";
        gNotificationBuilder.setContentText(sNotificationText)
                .setProgress(100, iProgressBarValue, false);
        gNotification = gNotificationBuilder.build();
        globalClass.notificationManager.notify(giNotificationID, gNotification);


        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                false, "",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

        //Update the progress data associated with this worker:
        Data dataProgress = new Data.Builder()
                .putLong(JOB_BYTES_PROCESSED, glProgressNumerator)
                .putLong(JOB_BYTES_TOTAL, glProgressDenominator)
                .putString(JOB_DATETIME, gsJobRequestDateTime)
                .build();
        setProgressAsync(dataProgress);

        return dataProgress;
    }


    private void CloseNotification(){
        gNotificationBuilder.setOngoing(false) //Let the user remove the notification from the notification bar.
                            .setProgress(0, 0,false); //Remove the progress bar from the notification.
        gNotification = gNotificationBuilder.build();
        globalClass.notificationManager.notify(giNotificationID, gNotification);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Run your task here
                Toast.makeText(getApplicationContext(), "File transfer complete.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Data DataErrorMessage(String sMessage){

        return new Data.Builder()
                .putLong(JOB_BYTES_PROCESSED, glProgressNumerator)
                .putLong(JOB_BYTES_TOTAL, glProgressDenominator)
                .putString(JOB_DATETIME, gsJobRequestDateTime)
                .putString(FAILURE_MESSAGE, sMessage)
                .build();
    }

    @Override
    public void onStopped() {
        //The worker is stopping. May be that the job is done, or that the user has commanded
        // it, or that the Android OS has commanded it.

        String sNotificationText = giFilesProcessed + "/" + giFileCount + " files processed.";
        String sMessage = "Worker stopped.";

        if(giFilesProcessed != giFileCount){
            sNotificationText = sNotificationText + " Operation paused.";
            sMessage = sMessage + " Files processed: " + giFilesProcessed + "/" + giFileCount;
            //How do we want to record the state of this incomplete worker?
            //  Preference?
            //  File?
            //Should I construct a manager to monitor the state of jobs and workers?

        } else {
            gNotificationBuilder.setProgress(0, 0,false); //Remove the progress bar from the notification.
            sMessage = sMessage + " File processing complete.";
        }
        gNotificationBuilder.setOngoing(false) //Let the user remove the notification from the notification bar.
                .setContentText(sNotificationText);

        gNotification = gNotificationBuilder.build();
        globalClass.notificationManager.notify(giNotificationID, gNotification);

        try {
            gfwLogFile.write(sMessage + "\n");

        }catch (Exception e){
            //Do nothing at this point.
        }

        super.onStopped();
    }

}
