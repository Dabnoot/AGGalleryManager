package com.agcurations.aggallerymanager;

import android.app.Notification;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
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
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_LocalFileTransfer extends Worker {

    //Define string used to identify this worker type:
    public static final String WORKER_LOCAL_FILE_TRANSFER_TAG = "WORKER_LOCAL_FILE_TRANSFER_TAG";

    public static final String IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE";

    public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";

    //=========================
    // Define keys for arguments passed to this worker:
    public static final String KEY_ARG_JOB_REQUEST_DATETIME = "KEY_ARG_JOB_REQUEST_DATETIME";
    public static final String KEY_ARG_JOB_FILE = "KEY_ARG_JOB_FILE";

    public static final String JOB_PROGRESS = "JOB_PROGRESS";
    public static final String JOB_BYTES_PROCESSED = "JOB_BYTES_PROCESSED";
    public static final String JOB_BYTES_TOTAL = "JOB_BYTES_TOTAL";

    public static final String JOB_DATETIME = "JOB_DATETIME";

    public static final int HEADER_FIELD_INDEX_MOVE_OR_COPY = 1;
    public static final int HEADER_FIELD_INDEX_MEDIA_CATEGORY = 0;
    public static final int HEADER_FIELD_INDEX_TOTAL_SIZE = 2; //Includes total size of all files to process, even files to be deleted.
    public static final int HEADER_FIELD_INDEX_FILE_COUNT = 3;
    public static final int HEADER_FIELD_COUNT = 4;

    public static final int RECORD_FIELD_INDEX_SOURCE_FILE_URI_INDEX = 0;
    public static final int RECORD_FIELD_INDEX_DESTINATION_FOLDER = 1;
    public static final int RECORD_FIELD_INDEX_DESTINATION_FILENAME = 2;
    public static final int RECORD_FIELD_INDEX_SOURCE_FILE_SIZE_BYTES = 3;
    public static final int RECORD_FIELD_INDEX_SOURCE_FILE_DELETE_ONLY = 4; //If the user is not importing this item and has marked it for deletion.
    public static final int RECORD_FIELD_INDEX_METADATA_FILE_FLAG = 5;
    public static final int RECORD_FIELD_COUNT = 6; //Number of fields to expect per record in a job file.


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

    BufferedWriter gbwLogFile;
    OutputStream gosLogFile;
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

        if(!GlobalClass.CheckIfFileExists(GlobalClass.gUriLogsFolder)){
            sMessage = "Logs folder missing. Restarting app should create the folder.";
            GlobalClass.gabImportExecutionRunning.set(false);
            GlobalClass.gabImportExecutionFinished.set(true);
            return Result.failure(DataErrorMessage(sMessage));

        }

        //Put the log file in the logs folder:
        String sLogFileName = gsJobRequestDateTime + "_" + GlobalClass.GetTimeStampFileSafe() + "_LocalFileTransfer_WorkerLog.txt";
        Uri uriLogFile;
        try {
            uriLogFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, GlobalClass.gUriLogsFolder, GlobalClass.BASE_TYPE_TEXT, sLogFileName);
        } catch (FileNotFoundException e) {
            sMessage = "Could not create log file at location " + GlobalClass.gUriLogsFolder + ".";
            GlobalClass.gabImportExecutionRunning.set(false);
            GlobalClass.gabImportExecutionFinished.set(true);
            return Result.failure(DataErrorMessage(sMessage));
        }
        if(uriLogFile == null){
            sMessage = "Could not create log file at location " + GlobalClass.gUriLogsFolder + ".";
            GlobalClass.gabImportExecutionRunning.set(false);
            GlobalClass.gabImportExecutionFinished.set(true);
            return Result.failure(DataErrorMessage(sMessage));
        }
        try { //Required for the log file.
            gosLogFile = GlobalClass.gcrContentResolver.openOutputStream(uriLogFile, "wt");
            if(gosLogFile == null){
                sMessage = "Could not open output stream to log file at location " + GlobalClass.gUriLogsFolder + ".";
                GlobalClass.gabImportExecutionRunning.set(false);
                GlobalClass.gabImportExecutionFinished.set(true);
                return Result.failure(DataErrorMessage(sMessage));
            }
            gbwLogFile = new BufferedWriter(new OutputStreamWriter(gosLogFile));

            //Validate data
            if (gsJobFile.equals("")) {
                sMessage = "No job file name provided. This is the file telling the worker what files to copy, and where to place them.";
                gbwLogFile.write(sMessage + "\n");
                gbwLogFile.flush();
                gbwLogFile.close();
                gosLogFile.flush();
                gosLogFile.close();
                globalClass.BroadcastProgress(true, sMessage,
                        false, 0,
                        false, "",
                        IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                GlobalClass.gabImportExecutionRunning.set(false);
                GlobalClass.gabImportExecutionFinished.set(true);
                return Result.failure(DataErrorMessage(sMessage));
            }

            Uri uriJobFile = GlobalClass.FormChildUri(GlobalClass.gUriJobFilesFolder.toString(), gsJobFile);

            if (GlobalClass.CheckIfFileExists(uriJobFile)) {

                boolean bProblemWithFileTransfer = false;

                //Get data from file:
                //File Format:
                // ALL FILENAMES MUST BE UNIQUE! Otherwise this routine will think that the copy has
                // already taken place and will either skip the copy or, in the case of a move op,
                //  will delete the source file.

                InputStream isJobFile = GlobalClass.gcrContentResolver.openInputStream(uriJobFile);
                if(isJobFile == null){
                    sMessage = "Unable to open job file for reading. This is the file telling the worker what files to copy, and where to place them.";
                    gbwLogFile.write(sMessage + "\n");
                    gbwLogFile.flush();
                    gbwLogFile.close();
                    gosLogFile.flush();
                    gosLogFile.close();
                    globalClass.BroadcastProgress(true, sMessage,
                            false, 0,
                            false, "",
                            IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                    GlobalClass.gabImportExecutionRunning.set(false);
                    GlobalClass.gabImportExecutionFinished.set(true);
                    return Result.failure(DataErrorMessage(sMessage));
                }

                BufferedReader brReader;
                try { //Required for the job file.
                    brReader = new BufferedReader(new InputStreamReader(isJobFile));

                    int iMediaCategory;
                    int iMoveOrCopy;

                    //Read the header and get the media category and specified move/copy behavior:
                    String sConfig = brReader.readLine();
                    try {  //A Try/Catch to simplify error messaging with examination of the job file header.
                        String[] sData = sConfig.split("\t");
                        if (sData.length != HEADER_FIELD_COUNT) {
                            throw new Exception("Data header missing data in job file.");
                        }

                        //Determine media category:
                        String[] sMediaCategoryData = sData[HEADER_FIELD_INDEX_MEDIA_CATEGORY].split(":");
                        if (sMediaCategoryData.length != 2) {
                            throw new Exception("Data header missing data for media category in job file.");
                        }
                        if (sMediaCategoryData[1].equals(GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_COMICS])){
                            iMediaCategory = 2; //Comics
                            //Destination may be a combination of subfolder/comicfolder.
                        } else if(sMediaCategoryData[1].equals(GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS])){
                            iMediaCategory = 0; //Videos
                        } else {
                            iMediaCategory = 1; //default to Images.
                        }

                        //Determine move or copy:
                        String[] sMoveOrCopyData = sData[HEADER_FIELD_INDEX_MOVE_OR_COPY].split(":");
                        if (sMoveOrCopyData.length != 2) {
                            throw new Exception("Data header missing data for 'move or copy behavior selection' in job file.");
                        }
                        if(sMoveOrCopyData[1].equals(GlobalClass.gsMoveOrCopy[GlobalClass.MOVE])){
                            iMoveOrCopy = GlobalClass.MOVE;
                        } else {
                            iMoveOrCopy = GlobalClass.COPY;
                        }

                        //Read total size:
                        String[] sTotalSizeData = sData[HEADER_FIELD_INDEX_TOTAL_SIZE].split(":");
                        if (sTotalSizeData.length != 2) {
                            throw new Exception("Data header missing data for 'total file transfer size' in job file.");
                        }
                        glProgressDenominator = Long.parseLong(sTotalSizeData[1]);

                        //Read file count:
                        String[] sFileCountData = sData[HEADER_FIELD_INDEX_FILE_COUNT].split(":");
                        if (sFileCountData.length != 2) {
                            throw new Exception("Data header missing data for 'file count' in job file.");
                        }
                        giFileCount = Integer.parseInt(sFileCountData[1]);

                    } catch (Exception e){
                        sMessage = e.getMessage();
                        gbwLogFile.write(sMessage + "\n");
                        gbwLogFile.flush();
                        gbwLogFile.close();
                        gosLogFile.flush();
                        gosLogFile.close();
                        globalClass.BroadcastProgress(true, sMessage,
                                false, 0,
                                false, "",
                                IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                        GlobalClass.gabImportExecutionRunning.set(false);
                        GlobalClass.gabImportExecutionFinished.set(true);
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
                    giNotificationID = GlobalClass.iNotificationID;
                    GlobalClass.iNotificationID++;
                    gNotification = gNotificationBuilder.build();
                    GlobalClass.notificationManager.notify(giNotificationID, gNotification);

                    //Build progress data associated with this worker:
                    dataProgress = new Data.Builder()
                            .putLong(JOB_BYTES_PROCESSED, glProgressNumerator)
                            .putLong(JOB_BYTES_TOTAL, glProgressDenominator)
                            .putString(JOB_DATETIME, gsJobRequestDateTime)
                            .build();
                    setProgressAsync(dataProgress);

                    int iProgressBarValue = Math.round((glProgressNumerator / (float) glProgressDenominator) * 100);
                    globalClass.BroadcastProgress(true, "Background worker processing files...\n",
                            true, iProgressBarValue,
                            true, sNotificationText,
                            IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);

                    //Read the data records in the job file and move/copy files:
                    do {
                        String sLine = brReader.readLine();
                        if(sLine == null){
                            break;
                        }
                        globalClass.BroadcastProgress(false, "",
                                false, 0,
                                true, "Files processed: " + giFilesProcessed + "/" + giFileCount,
                                IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);

                        if (!sLine.equals("")) {
                            String[] sJobFileRecordFields = sLine.split("\t");
                            if (sJobFileRecordFields.length == RECORD_FIELD_COUNT) {

                                //Get a user-friendly version of the source file path + filename:
                                String sSourceFileUri = sJobFileRecordFields[RECORD_FIELD_INDEX_SOURCE_FILE_URI_INDEX];
                                Uri uriSourceFile = Uri.parse(sSourceFileUri);
                                String sUserFriendlySourceFileUri = GlobalClass.GetUserFriendlyStorageName(sSourceFileUri);


                                long lFileSize = Long.parseLong(sJobFileRecordFields[RECORD_FIELD_INDEX_SOURCE_FILE_SIZE_BYTES]);

                                boolean bMetadataFile = Boolean.parseBoolean((sJobFileRecordFields[RECORD_FIELD_INDEX_METADATA_FILE_FLAG]));

                                if(!bMetadataFile) {
                                    giFilesProcessed++; //Don't update the file count progression for a metadata file. It will likely confuse the user.
                                                        // If they are deleting one file, we don't want it to show that 2 files were deleted.
                                }

                                //Check if source file exists:
                                if (!GlobalClass.CheckIfFileExists(uriSourceFile)) {

                                    //If the source does not exist and was marked for transfer (not marked for deletion without transfer), assume that the file has already been transferred.
                                    //todo: make sure that the file exists in the destination, and if not, provide an error message.
                                    glProgressNumerator = glProgressNumerator + lFileSize;

                                    sMessage = "Could not locate source file " + uriSourceFile;
                                    gbwLogFile.write(sMessage + "\n");
                                    globalClass.BroadcastProgress(true, sMessage,
                                            false, 0,
                                            false, "",
                                            IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                                    bProblemWithFileTransfer = true;
                                    continue; //Skip to the end of the loop and read the next line in the job file.
                                }

                                boolean bMarkedForDeletion = Boolean.parseBoolean(sJobFileRecordFields[RECORD_FIELD_INDEX_SOURCE_FILE_DELETE_ONLY]);



                                String sLogLine;

                                if(bMarkedForDeletion) {
                                    //If this source item is marked for deletion (no move or copy op to be performed), delete the source file:
                                    boolean bDeleteSuccess;

                                    glProgressNumerator = glProgressNumerator +  lFileSize;
                                    bDeleteSuccess = DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriSourceFile);

                                    UpdateProgressOutput();

                                    String sSpecialFileMessage = " ";
                                    if(bMetadataFile){
                                        //Give a special notification to the user if this is a
                                        // metadata file - user might be confused as to what's going on.
                                        sSpecialFileMessage = " metadata ";
                                    }
                                    if (!bDeleteSuccess) {
                                        sLogLine = "Could not delete" + sSpecialFileMessage + "file marked for deletion: " + sUserFriendlySourceFileUri;
                                    } else {
                                        sLogLine = "Success deleting" + sSpecialFileMessage + "file marked for deletion: " + sUserFriendlySourceFileUri;
                                    }
                                    gbwLogFile.write(sLogLine + "\n");
                                    globalClass.BroadcastProgress(true, sLogLine + "\n",
                                            false, 0,
                                            false, "",
                                            IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);

                                } else {
                                    //If this item is not merely marked for deletion (this could be a move op, which would involve a deletion)...

                                    String sDestinationFileName = sJobFileRecordFields[RECORD_FIELD_INDEX_DESTINATION_FILENAME];

                                    //todo: will there be a problem here if there is a file and a directory of the same name?

                                    String sDestinationRelativePath = sJobFileRecordFields[RECORD_FIELD_INDEX_DESTINATION_FOLDER];
                                    sDestinationRelativePath = sDestinationRelativePath.replace("%2F", GlobalClass.gsFileSeparator);
                                    if(sDestinationRelativePath.contains(GlobalClass.gsFileSeparator)){
                                        //Ensure that the multiple folders of "relative path" exist:
                                        String[] sRelativePathFolders = sDestinationRelativePath.split(GlobalClass.gsFileSeparator);
                                        for(int i = 0; i < sRelativePathFolders.length - 1; i++){
                                            //Don't analyze the last folder on this string. It is only the intermediary folders that we are worried about.
                                            //Problem pops up for local-storage comic import. It would pop up for an m3u8 import if such a folder of files were on local storage,
                                            //  but that's not typically how an m3u8 import is ancipicated to happen in this program.
                                            Uri uriDestinationFolderParentA = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[iMediaCategory], sRelativePathFolders[i]);
                                            if(!GlobalClass.CheckIfFileExists(uriDestinationFolderParentA)){
                                                GlobalClass.CreateDirectory(uriDestinationFolderParentA);

                                                if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).size() > 0){
                                                    //todo: untested.
                                                    //Build an entry for CatalogAnalysis index if necessary:
                                                    //If a folder was created, and the user has recently been engage with catalog anaylsis, add the new folder to the catalog analysis index:
                                                    String sKey = sRelativePathFolders[i];
                                                    ItemClass_File icf = new ItemClass_File(ItemClass_File.TYPE_FOLDER, sRelativePathFolders[i]);
                                                    icf.sUri = uriDestinationFolderParentA.toString();
                                                    icf.sUriParent = GlobalClass.GetParentUri(uriDestinationFolderParentA.toString()); //Among other things, used to determine if pages belong to a comic or an M3U8 playlist.
                                                    icf.sMimeType = "";
                                                    icf.dateLastModified = new Date();
                                                    icf.lSizeBytes = 0;
                                                    icf.sMediaFolderRelativePath = sRelativePathFolders[i];
                                                    GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).put(sKey, icf);
                                                }

                                            }
                                        }
                                    }

                                    String sDestinationFolderName = sJobFileRecordFields[RECORD_FIELD_INDEX_DESTINATION_FOLDER];
                                    Uri uriDestinationFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[iMediaCategory], sDestinationFolderName);
                                    if(!GlobalClass.CheckIfFileExists(uriDestinationFolder)){
                                        uriDestinationFolder = GlobalClass.CreateDirectory(uriDestinationFolder);
                                        if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).size() > 0){
                                            //todo: untested.
                                            //Build an entry for CatalogAnalysis index if necessary:
                                            //If a folder was created, and the user has recently been engage with catalog anaylsis, add the new folder to the catalog analysis index:
                                            String sKey = sDestinationFolderName;
                                            ItemClass_File icf = new ItemClass_File(ItemClass_File.TYPE_FOLDER, sDestinationFolderName);
                                            icf.sUri = uriDestinationFolder.toString();
                                            icf.sUriParent = GlobalClass.GetParentUri(uriDestinationFolder.toString()); //Among other things, used to determine if pages belong to a comic or an M3U8 playlist.
                                            icf.sMimeType = "";
                                            icf.dateLastModified = new Date();
                                            icf.lSizeBytes = 0;
                                            icf.sMediaFolderRelativePath = sDestinationFolderName;
                                            GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).put(sKey, icf);
                                        }
                                    }

                                    String sUserFriendlyDestinationFolderUri = GlobalClass.GetUserFriendlyStorageName(uriDestinationFolder.toString());

                                    if(uriDestinationFolder == null){
                                        sMessage = "Could not create destination folder \"" + sJobFileRecordFields[RECORD_FIELD_INDEX_DESTINATION_FOLDER] + "\" for file \""
                                                + sUserFriendlyDestinationFolderUri + "\", line " + giFilesProcessed + ": " + uriJobFile;
                                        gbwLogFile.write(sMessage + "\n");
                                        globalClass.BroadcastProgress(true, sMessage,
                                                false, 0,
                                                false, "",
                                                IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                                        bProblemWithFileTransfer = true;
                                        continue; //Skip to the end of the loop and read the next line in the job file.
                                    }
                                    //Destination folder exists or has been created successfully.

                                    Uri uriDestinationFile = GlobalClass.FormChildUri(uriDestinationFolder, sDestinationFileName);

                                    sLogLine = "\n" + GlobalClass.gsMoveOrCopy[iMoveOrCopy + 2]
                                            + " file " + sUserFriendlySourceFileUri + " to " + sUserFriendlyDestinationFolderUri + ".\n";
                                    gbwLogFile.write(sLogLine + "\n");
                                    globalClass.BroadcastProgress(true, sLogLine,
                                            false, 0,
                                            false, "",
                                            IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);

                                    if (GlobalClass.CheckIfFileExists(uriDestinationFile)) {
                                        //The file copy has already been executed by a previous instance of this requested worker.
                                        //If the operation was a move operation, we are here only because the source file still
                                        //  exists. Attempt to delete the source file.
                                        if (iMoveOrCopy == GlobalClass.MOVE) {
                                            boolean bDeleteSuccess = DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriSourceFile);

                                            if (!bDeleteSuccess) {
                                                sMessage = "Source file copied, but could not delete source file as part of a 'move' operation. File \""
                                                        + sUserFriendlySourceFileUri + "\", job file line " + giFilesProcessed + " in job file " + uriJobFile + ".";
                                                gbwLogFile.write(sMessage + "\n");
                                                globalClass.BroadcastProgress(true, sMessage + "\n",
                                                        false, 0,
                                                        false, "",
                                                        IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                                                bProblemWithFileTransfer = true;
                                            }
                                        }
                                        glProgressNumerator = glProgressNumerator + lFileSize;
                                        continue; //Skip to the end of the loop and read the next line in the job file.
                                    }
                                    //If we are here, then the destination file does not exist.

                                    // Execute the copy or move operation:

                                    //Copy the document first. DocumentsContract.Copy and .Move are finicky and don't always do the job.
                                    Uri uriOutputFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriDestinationFolder, "none", sDestinationFileName);
                                    if(uriOutputFile != null) {
                                        InputStream isSourceFile = null;
                                        OutputStream osDestinationFile = null;

                                        try {
                                            isSourceFile = GlobalClass.gcrContentResolver.openInputStream(uriSourceFile);
                                            osDestinationFile = GlobalClass.gcrContentResolver.openOutputStream(uriOutputFile);

                                            if (isSourceFile != null && osDestinationFile != null) {
                                                byte[] bucket = new byte[32 * 1024];
                                                int bytesRead = 0;
                                                long lTotalBytes = 0;
                                                int iLoopCount = 1;
                                                while (bytesRead != -1) {
                                                    bytesRead = isSourceFile.read(bucket); //-1, 0, or more
                                                    if (bytesRead > 0) {
                                                        osDestinationFile.write(bucket, 0, bytesRead);
                                                        glProgressNumerator = glProgressNumerator + bytesRead;
                                                        lTotalBytes = lTotalBytes + bytesRead;
                                                        if(iLoopCount % 10 == 0) {
                                                            //Update bytes transferred every 10 loops:
                                                            UpdateProgressOutput();
                                                        }
                                                    }
                                                    iLoopCount++;
                                                }
                                                UpdateProgressOutput();

                                                //Build an entry for CatalogAnalysis index if necessary:
                                                if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).size() > 0){
                                                    //todo: untested.
                                                    //Create an entry in the CatalogAnalysis index for the file added to the data storage.
                                                    String sKey = sDestinationFolderName + GlobalClass.gsFileSeparator + sDestinationFileName;
                                                    ItemClass_File icf = new ItemClass_File(ItemClass_File.TYPE_FILE, sDestinationFileName);
                                                    icf.sUri = uriOutputFile.toString();
                                                    icf.sUriParent = GlobalClass.GetParentUri(uriOutputFile.toString()); //Among other things, used to determine if pages belong to a comic or an M3U8 playlist.
                                                    icf.sMimeType = "";
                                                    icf.dateLastModified = new Date();
                                                    icf.lSizeBytes = lTotalBytes;
                                                    // The key consists of icf.sMediaFolderRelativePath + GlobalClass.gsFileSeparator + icf.sFileOrFolderName
                                                    icf.sMediaFolderRelativePath = sDestinationFolderName;
                                                    GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).put(sKey, icf);
                                                }

                                            }




                                        } catch (Exception e) {
                                            sMessage = "Source file could not be copied. File \""
                                                    + sUserFriendlySourceFileUri + "\", job file line " + giFilesProcessed + " in job file " + uriJobFile + ".";
                                            gbwLogFile.write(sMessage + "\n");
                                            globalClass.BroadcastProgress(true, sMessage + "\n",
                                                    false, 0,
                                                    false, "",
                                                    IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                                            bProblemWithFileTransfer = true;
                                            glProgressNumerator = glProgressNumerator + lFileSize;
                                            continue;

                                        } finally {
                                            if (isSourceFile != null)
                                                isSourceFile.close();
                                            if (osDestinationFile != null)
                                                osDestinationFile.close();
                                        }
                                    } else {
                                        sMessage = "Source file could not be copied. File \""
                                                + sUserFriendlySourceFileUri + "\", job file line " + giFilesProcessed + " in job file " + uriJobFile + ".";
                                        gbwLogFile.write(sMessage + "\n");
                                        globalClass.BroadcastProgress(true, sMessage + "\n",
                                                false, 0,
                                                false, "",
                                                IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                                        bProblemWithFileTransfer = true;
                                        glProgressNumerator = glProgressNumerator + lFileSize;
                                        continue;
                                    }

                                    if(iMoveOrCopy == GlobalClass.MOVE){
                                        //Notify the user of the pending delete operation:
                                        sLogLine = "Deleting source file as part of move operation...";
                                        gbwLogFile.write(sLogLine + "\n");
                                        globalClass.BroadcastProgress(true, sLogLine,
                                                false, 0,
                                                false, "",
                                                IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);

                                        //Attempt the delete operation:
                                        if(!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriSourceFile)){
                                            sMessage = "Source file could not be deleted after copy to complete move operation. File \""
                                                    + sUserFriendlyDestinationFolderUri + "\", job file line " + giFilesProcessed + " in job file " + uriJobFile + ".";
                                            gbwLogFile.write(sMessage + "\n");
                                            globalClass.BroadcastProgress(true, sMessage + "\n",
                                                    false, 0,
                                                    false, "",
                                                    IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                                            bProblemWithFileTransfer = true;
                                            continue;
                                        }
                                    }

                                    sLogLine = "Success.";
                                    gbwLogFile.write(sLogLine + "\n");
                                    globalClass.BroadcastProgress(true, sLogLine + "\n",
                                            false, 0,
                                            false, "",
                                            IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);

                                }
                                dataProgress = UpdateProgressOutput();

                            } else {
                                sMessage = "Data missing while reading job file, line " + giFilesProcessed + ": " + uriJobFile;
                                gbwLogFile.write(sMessage + "\n");
                                gbwLogFile.flush();
                                gbwLogFile.close();
                                gosLogFile.flush();
                                gosLogFile.close();
                                globalClass.BroadcastProgress(true, sMessage,
                                        false, 0,
                                        false, "",
                                        IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                                CloseNotification();
                                GlobalClass.gabImportExecutionRunning.set(false);
                                GlobalClass.gabImportExecutionFinished.set(true);
                                return Result.failure(DataErrorMessage(sMessage));
                            }
                        }
                        globalClass.BroadcastProgress(false, "",
                                false, 0,
                                true, "Files processed: " + giFilesProcessed + "/" + giFileCount,
                                IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);

                    } while (true);
                    brReader.close();
                    isJobFile.close();

                    CloseNotification();

                } catch (IOException e) {
                    sMessage = "Problem reading job file: " + uriJobFile;
                    gbwLogFile.write(sMessage + "\n");
                    gbwLogFile.flush();
                    gbwLogFile.close();
                    gosLogFile.flush();
                    gosLogFile.close();
                    globalClass.BroadcastProgress(true, sMessage,
                            false, 0,
                            false, "",
                            IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                    GlobalClass.gabImportExecutionRunning.set(false);
                    GlobalClass.gabImportExecutionFinished.set(true);
                    return Result.failure(DataErrorMessage(sMessage));
                }

                //Delete the job file if there were no problems:
                if(!bProblemWithFileTransfer){
                    if(!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriJobFile)){
                        sMessage = "Worker finished processing job but could not delete job file: " + uriJobFile;
                        gbwLogFile.write(sMessage + "\n");
                        globalClass.BroadcastProgress(true, sMessage + "\n",
                                false, 0,
                                false, "",
                                IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                    }
                }

            } else {
                sMessage = "Job file does not exist: " + gsJobFile + " at location " + GlobalClass.gUriJobFilesFolder;
                gbwLogFile.write(sMessage + "\n");
                gbwLogFile.flush();
                gbwLogFile.close();
                gosLogFile.flush();
                gosLogFile.close();
                globalClass.BroadcastProgress(true, sMessage,
                        false, 0,
                        false, "",
                        IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
                GlobalClass.gabImportExecutionRunning.set(false);
                GlobalClass.gabImportExecutionFinished.set(true);
                return Result.failure(DataErrorMessage(sMessage));
            }

            gbwLogFile.flush();
            gbwLogFile.close();
            gosLogFile.flush();
            gosLogFile.close();

            GlobalClass.gabImportExecutionRunning.set(false);
            GlobalClass.gabImportExecutionFinished.set(true);
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
                    IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
            GlobalClass.gabImportExecutionRunning.set(false);
            GlobalClass.gabImportExecutionFinished.set(true);
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
        GlobalClass.notificationManager.notify(giNotificationID, gNotification);


        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                false, "",
                IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);

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
        GlobalClass.notificationManager.notify(giNotificationID, gNotification);

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
        GlobalClass.notificationManager.notify(giNotificationID, gNotification);

        try {
            gbwLogFile.write(sMessage + "\n");

        }catch (Exception e){
            //Do nothing at this point.
        }

        super.onStopped();
    }

    /**
     * Builds a string to be written to the job file. This routine serves to ensure that the proper
     * arguments, types, and usage are correct; this routine is to reduce programmer error.
     *
     * @param sSourceUri                        A Uri for the source file.
     * @param sDestinationFolderName            Name of the folder to hold this file. The header specifying
     *                                          the Catalog type (Videos, Pictures, Comics) is to include
     *                                          this folder name.
     * @param sDestinationFileOrFolderName      The destination file name.
     * @param lSizeInBytes                      The size of the file for progress display to user.
     * @param bMarkForDeletion                  A flag to mark if this file should be deleted rather than copied
     * @param bMetadataFile                     A flag to indicate that this is a metadata file. Used to notify user during logging of the file deletion.
     *                                          or moved.
     * @return sJobFileRecord                   A string is returned to be written to the job file.
     */
    public static String CreateJobFileRecord(String sSourceUri,
                                              String sDestinationFolderName,
                                              String sDestinationFileOrFolderName,
                                              long lSizeInBytes,
                                              boolean bMarkForDeletion,
                                              boolean bMetadataFile){

        return sSourceUri + "\t" +
                sDestinationFolderName + "\t" +
                sDestinationFileOrFolderName + "\t" +
                lSizeInBytes + "\t" +
                bMarkForDeletion + "\t" +
                bMetadataFile + "\n";
    }


}
