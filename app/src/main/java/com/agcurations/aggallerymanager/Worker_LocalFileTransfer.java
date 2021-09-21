package com.agcurations.aggallerymanager;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;
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
    public static final String KEY_ARG_MEDIA_CATEGORY = "KEY_ARG_MEDIA_CATEGORY";
    public static final String KEY_ARG_COPY_OR_MOVE = "KEY_ARG_COPY_OR_MOVE";
    public static final String KEY_ARG_TOTAL_IMPORT_SIZE_BYTES = "KEY_ARG_TOTAL_IMPORT_SIZE_BYTES";

    public static final String WORKER_PROGRESS = "WORKER_PROGRESS";
    public static final String WORKER_BYTES_PROCESSED = "WORKER_BYTES_PROCESSED";

    public static final int LOCAL_FILE_TRANSFER_MOVE = 0;
    public static final int LOCAL_FILE_TRANSFER_COPY = 1;

    //=========================
    String gsJobRequestDateTime;    //Date/Time of job request for logging purposes.
    String gsJobFile;               //Name of file containing a list of files to transfer
    int giMediaCategory;            //Media category for base folder determination
    int giCopyOrMove;               //Copy the files or move the files?
    long glTotalImportSize;         //Collective file size of the import operation, for reporting progress.

    //=========================

    public Worker_LocalFileTransfer(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters) {
        super(appContext, workerParameters);
        // Set initial progress to 0
        setProgressAsync(new Data.Builder().putInt(WORKER_PROGRESS, 0).build());

        gsJobRequestDateTime = getInputData().getString(KEY_ARG_JOB_REQUEST_DATETIME);
        gsJobFile = getInputData().getString(KEY_ARG_JOB_FILE);
        giCopyOrMove = getInputData().getInt(KEY_ARG_COPY_OR_MOVE, LOCAL_FILE_TRANSFER_COPY);
        giMediaCategory = getInputData().getInt(KEY_ARG_MEDIA_CATEGORY, -1);
        glTotalImportSize = getInputData().getLong(KEY_ARG_TOTAL_IMPORT_SIZE_BYTES, -1);
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        String sMessage;

        if(!globalClass.gfLogsFolder.exists()){
            sMessage = "Logs folder missing. Restarting app should create the folder.";
            return Result.failure(DataErrorMessage(sMessage));
        }

        //Put the log file in the logs folder:
        String sLogFilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                File.separator + gsJobRequestDateTime + "_" + GlobalClass.GetTimeStampFileSafe() + "_LocalFileTransfer_WorkerLog.txt";
        File fLog = new File(sLogFilePath);
        FileWriter fwLogFile;
        try {
            fwLogFile = new FileWriter(fLog, true);

            //Validate data
            if (gsJobFile.equals("")) {
                sMessage = "No job file name provided. This is the file telling the worker what files to copy, and where to place them.";
                fwLogFile.write(sMessage + "\n");
                fwLogFile.close();
                return Result.failure(DataErrorMessage(sMessage));
            }

            if (giMediaCategory == -1) {
                sMessage = "Media category missing in data transfer. Cannot determine where to store files.";
                fwLogFile.write(sMessage + "\n");
                fwLogFile.close();
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

                long lProgressNumerator = 0L;
                long lProgressDenominator = 1L;
                int iProgressBarValue = 0;
                long lTotalImportSize = 0L;
                long lLoopBytesRead;

                BufferedReader brReader;
                try {
                    brReader = new BufferedReader(new FileReader(fJobFile.getAbsolutePath()));

                    int j = 0;
                    do {
                        String sLine = brReader.readLine();
                        if(sLine == null){
                            break;
                        }

                        j++;
                        if (!sLine.equals("")) {
                            String[] sTemp = sLine.split("\t");
                            if (sTemp.length == 4) {

                                String sDestinationFolder = globalClass.gfCatalogFolders[giMediaCategory].getAbsolutePath() + File.separator + sTemp[DESTINATION_FOLDER];
                                String sDestinationFileName = sTemp[DESTINATION_FILENAME];

                                File fDestinationFolder = new File(sDestinationFolder);
                                if (!fDestinationFolder.exists()) {
                                    if (!fDestinationFolder.mkdir()) {
                                        sMessage = "Could not create destination folder \"" + sDestinationFolder + "\" for file \"" + sDestinationFileName + "\", line " + j + ": " + sJobFilePath;
                                        fwLogFile.write(sMessage + "\n");
                                        bProblemWithFileTransfer = true;
                                        continue; //Skip to the end of the loop and read the next line in the job file.
                                    }
                                }
                                //Destination folder exists or has been created successfully.

                                //Check if source file exists:
                                String sSourceFileUri = sTemp[SOURCE_FILE_URI_INDEX];
                                Uri uriSourceFile = Uri.parse(sSourceFileUri);
                                DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
                                if (dfSource == null) {
                                    continue;
                                }
                                if (dfSource.getName() == null) {
                                    continue;
                                }

                                if (dfSource.exists()) {
                                    String sDestinationFileFullPath = sDestinationFolder + File.separator + sDestinationFileName;
                                    File fDestinationFile = new File(sDestinationFileFullPath);
                                    if (fDestinationFile.exists()) {
                                        //The file copy has already been executed by a previous instance of this requested worker.
                                        //If the operation was a move operation, we are here only because the source file still
                                        //  exists. Attempt to delete the source file.
                                        if (giCopyOrMove == LOCAL_FILE_TRANSFER_MOVE) {
                                            if (!dfSource.delete()) {
                                                sMessage = "Source file copied, but could not delete source file, " + dfSource.getName() + ", as part of a 'move' operation. File \"" + dfSource.getName() + "\", job file line " + j + " in job file " + sJobFilePath;
                                                fwLogFile.write(sMessage + "\n");
                                                bProblemWithFileTransfer = true;
                                            }
                                        }
                                        continue; //Skip to the end of the loop and read the next line in the job file.
                                    }
                                    //Destination file does not exist.

                                    // Execute the copy or move operation:
                                    String sLogLine;
                                    if (giCopyOrMove == LOCAL_FILE_TRANSFER_MOVE) {
                                        sLogLine = "Moving ";
                                    } else {
                                        sLogLine = "Copying ";
                                    }
                                    sLogLine = sLogLine + " file " + dfSource.getName() + " to " + fDestinationFile.getPath() + ".";
                                    fwLogFile.write(sLogLine + "\n");
                                    ContentResolver contentResolver = getApplicationContext().getContentResolver();
                                    InputStream inputStream = contentResolver.openInputStream(dfSource.getUri());

                                    OutputStream outputStream = new FileOutputStream(fDestinationFile.getPath());
                                    int iLoopCount = 0;
                                    byte[] buffer = new byte[100000];
                                    if (inputStream == null) {
                                        continue;
                                    }
                                    while ((lLoopBytesRead = inputStream.read(buffer, 0, buffer.length)) >= 0) {
                                        outputStream.write(buffer, 0, buffer.length);
                                        lProgressNumerator += lLoopBytesRead;
                                        iLoopCount++;
                                        if (iLoopCount % 10 == 0) {
                                            //Send update every 10 loops:
                                            iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                                        }
                                    }
                                    outputStream.flush();
                                    outputStream.close();

                                    sLogLine = " Success.\n";
                                    fwLogFile.write(sLogLine + "\n");

                                    if (giCopyOrMove == LOCAL_FILE_TRANSFER_MOVE) {
                                        if (!dfSource.delete()) {
                                            sLogLine = "Could not delete source file, " + dfSource.getName() + ", after copy (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                                            fwLogFile.write(sLogLine + "\n");
                                        } else {
                                            sLogLine = "Success deleting source file, " + dfSource.getName() + ", after copy (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                                            fwLogFile.write(sLogLine + "\n");
                                        }
                                    }

                                }

                            } else {
                                sMessage = "Data missing while reading job file, line " + j + ": " + sJobFilePath;
                                fwLogFile.write(sMessage + "\n");
                                fwLogFile.close();
                                return Result.failure(DataErrorMessage(sMessage));
                            }
                        }
                    } while (true);
                    brReader.close();

                } catch (IOException e) {
                    sMessage = "Problem reading job file: " + sJobFilePath;
                    fwLogFile.write(sMessage + "\n");
                    fwLogFile.close();
                    return Result.failure(DataErrorMessage(sMessage));
                }

                //Delete the job file if there were no problems:
                if(!bProblemWithFileTransfer){
                    if(!fJobFile.delete()){
                        sMessage = "Worker finished processing job but could not delete job file: " + sJobFilePath;
                        fwLogFile.write(sMessage + "\n");
                    }
                }

            } else {
                sMessage = "Job file does not exist: " + sJobFilePath;
                fwLogFile.write(sMessage + "\n");
                fwLogFile.close();
                return Result.failure(DataErrorMessage(sMessage));
            }

            fwLogFile.close();
            return Result.success();

        } catch (Exception e){
            sMessage = e.getMessage();
            if(sMessage == null){
                sMessage = "Null message";
            }
            Log.d("Job Worker", sMessage) ;
            return Result.failure(DataErrorMessage(sMessage));
        }



    }



    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(FAILURE_MESSAGE, sMessage)
                .build();
    }

}
