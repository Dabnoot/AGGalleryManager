package com.agcurations.aggallerymanager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Import_ImportFiles extends Worker {

    public static final String TAG_WORKER_IMPORT_IMPORTFILES = "com.agcurations.aggallermanager.tag_worker_import_importfiles";

    int giMoveOrCopy;
    int giMediaCategory;

    public Worker_Import_ImportFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        giMoveOrCopy = getInputData().getInt(GlobalClass.EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        //Set the flags to tell the catalogViewer to view the imported files first:
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sharedPreferences.edit()
                .putInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[globalClass.giSelectedCatalogMediaCategory],
                        GlobalClass.SORT_BY_DATETIME_IMPORTED)
                .putBoolean(GlobalClass.gsCatalogViewerPreferenceNameSortAscending[globalClass.giSelectedCatalogMediaCategory],
                        false)
                .apply();

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;

        ArrayList<ItemClass_File> alFileList = globalClass.galImportFileList;

        //Calculate total size of all files to import:
        for(ItemClass_File fi: alFileList){
            lTotalImportSize = lTotalImportSize + fi.lSizeBytes;
        }
        lProgressDenominator = lTotalImportSize;

        String sLogLine;

        StringBuilder sbJobFileRecords = new StringBuilder();

        //Loop and import files:
        for(ItemClass_File fileItem: alFileList) {

            if(fileItem.bMarkedForDeletion){

                String sFileSource = "";
                if(fileItem.iTypeFileFolderURL == ItemClass_File.TYPE_IMAGE_FROM_HOLDING_FOLDER){
                    Uri uriTemp = Uri.parse(fileItem.sUri);
                    sFileSource = uriTemp.getPath();
                } else {
                    Uri uriSourceFileToDelete;
                    uriSourceFileToDelete = Uri.parse(fileItem.sUri);
                    DocumentFile dfSourceToDelete = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFileToDelete);
                    sFileSource = dfSourceToDelete.getUri().toString();
                }
                //Write next behavior to the screen log:
                sLogLine = "Preparing data for job file: Delete file " + fileItem.sFileOrFolderName + ".\n";
                lProgressNumerator++;
                iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                globalClass.BroadcastProgress(true, sLogLine,
                        false, iProgressBarValue,
                        true, "File " + lProgressNumerator + "/" + lProgressDenominator,
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                String sLine = sFileSource + "\t"
                        + fileItem.sDestinationFolder + "\t"
                        + fileItem.sFileOrFolderName + "\t"
                        + fileItem.lSizeBytes + "\t"
                        + true + "\n";                 //Item marked for deletion?
                sbJobFileRecords.append(sLine);

                continue; //jump to next item in import list.
            } //End if item is marked for deletion.

            if(fileItem.sDestinationFolder.equals("")) {
                fileItem.sDestinationFolder = GlobalClass.gsUnsortedFolderName;
            }

            String sFileName = "";
            String sUriOrPath = "";
            if(fileItem.iTypeFileFolderURL == ItemClass_File.TYPE_IMAGE_FROM_HOLDING_FOLDER){
                Uri uriTemp = Uri.parse(fileItem.sUri);
                File fSource = new File(uriTemp.getPath());
                sFileName = fSource.getName();
                sUriOrPath = fSource.getAbsolutePath();
            } else {
                Uri uriSourceFile = Uri.parse(fileItem.sUri);
                DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
                boolean bProblemWithSourceFile = false;
                if (dfSource == null) {
                    bProblemWithSourceFile = true;
                } else if (dfSource.getName() == null){
                    bProblemWithSourceFile = true;
                }
                if(bProblemWithSourceFile){
                    globalClass.BroadcastProgress(true, "Problem with source reference for " + fileItem.sFileOrFolderName + "\n",
                            false, iProgressBarValue,
                            false, "",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    lProgressNumerator += fileItem.lSizeBytes;
                    continue;
                }
                sFileName = dfSource.getName();
                sUriOrPath = dfSource.getUri().toString();
            }


            try {


                ItemClass_CatalogItem ciNew = new ItemClass_CatalogItem();
                ciNew.sItemID = globalClass.getNewCatalogRecordID(giMediaCategory);

                //Reverse the text on the file so that the file does not get picked off by a search tool:
                String sTempFileName = ciNew.sItemID + "_" + sFileName; //Create unique filename. Using ID will allow database error checking.
                sFileName = GlobalClass.JumbleFileName(sTempFileName);


                //Write next behavior to the screen log:
                sLogLine = "Preparing data for job file: Import " + fileItem.sFileOrFolderName + ".\n";
                lProgressNumerator++;
                iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                globalClass.BroadcastProgress(true, sLogLine,
                        false, iProgressBarValue,
                        true, "File " + lProgressNumerator + "/" + lProgressDenominator,
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                String sLine = sUriOrPath + "\t"
                        + fileItem.sDestinationFolder + "\t"
                        + sFileName + "\t"
                        + fileItem.lSizeBytes + "\t"
                        + false + "\n";                 //Item marked for deletion?
                sbJobFileRecords.append(sLine);


                //Next add the data to the catalog file and memory:

                //Create a timestamp to be used to create the data record:
                Double dTimeStamp = GlobalClass.GetTimeStampDouble();

                ciNew.iMediaCategory = giMediaCategory;
                ciNew.sFilename = sFileName;
                ciNew.lSize = fileItem.lSizeBytes;
                ciNew.lDuration_Milliseconds = fileItem.lVideoTimeInMilliseconds;
                ciNew.sDuration_Text = fileItem.sVideoTimeText;
                if(!fileItem.sWidth.equals("") && !fileItem.sHeight.equals("")) {
                    ciNew.iWidth = Integer.parseInt(fileItem.sWidth);
                    ciNew.iHeight = Integer.parseInt(fileItem.sHeight);
                }
                ciNew.sFolder_Name = fileItem.sDestinationFolder;
                ciNew.sSource = ItemClass_CatalogItem.FOLDER_SOURCE;
                ciNew.sTags = GlobalClass.formDelimitedString(fileItem.aliProspectiveTags, ",");
                ciNew.aliTags = new ArrayList<>(fileItem.aliProspectiveTags);
                ciNew.dDatetime_Last_Viewed_by_User = dTimeStamp;
                ciNew.dDatetime_Import = dTimeStamp;
                ciNew.iGrade = fileItem.iGrade;

                //The below call should add the record to both the catalog contents file
                //  and memory:
                globalClass.CatalogDataFile_CreateNewRecord(ciNew);



            } catch (Exception e) {
                globalClass.BroadcastProgress(true, "Problem with copy/move operation.\n" + e.getMessage(),
                        false, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;
                return Result.failure();
            }


        } //End file processing loop.

        //Close the job file, written-to in the file loop above:
        String sJobDateTime = GlobalClass.GetTimeStampFileSafe();
        String sJobFileName = "Job_" + sJobDateTime + ".txt";

        try {

            //Inform user of preparation of worker:
            String sMoveOrCopy = GlobalClass.gsMoveOrCopy[giMoveOrCopy];
            sLogLine = "Using background worker for file " + sMoveOrCopy.toLowerCase() + " operations.\n"
                    + "Preparing job file.\n\n";
            lProgressDenominator = alFileList.size();
            globalClass.BroadcastProgress(true, sLogLine,
                    false, iProgressBarValue,
                    true, "File " + lProgressNumerator + "/" + lProgressDenominator,
                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

            //Create a file with a listing of the files to be copied/moved:
            String sJobFilePath = globalClass.gfJobFilesFolder.getAbsolutePath() +
                    File.separator + sJobFileName;
            File fJobFile = new File(sJobFilePath);

            FileWriter fwJobFile = new FileWriter(fJobFile, true);
            //Write the data header:
            String sConfig = "MediaCategory:" + GlobalClass.gsCatalogFolderNames[giMediaCategory] + "\t"
                    + "MoveOrCopy:" + sMoveOrCopy + "\t"
                    + "TotalSize:" + lTotalImportSize + "\t"
                    + "FileCount:" + alFileList.size() + "\n";
            fwJobFile.write(sConfig);
            fwJobFile.write(sbJobFileRecords.toString());
            fwJobFile.flush();
            fwJobFile.close();



        } catch (Exception e){
            globalClass.BroadcastProgress(true, "Problem with writing the job file.\n" + e.getMessage(),
                    false, iProgressBarValue,
                    false, "",
                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            globalClass.gbImportExecutionRunning = false;
            globalClass.gbImportExecutionFinished = true;
            return Result.failure();
        }
        //Write next behavior to the screen log:
        sLogLine = "\nStarting worker to process job file.\n\n"
                + "Files will appear in the catalog as the worker progresses.\n"
                + "Refresh the catalog viewer (exit/re-enter, change sort direction) to view newly-added files.\n";
        globalClass.BroadcastProgress(true, sLogLine,
                false, iProgressBarValue,
                true, lProgressNumerator + "/" + lProgressDenominator + " files written to job file",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

        //Build-out data to send to the worker:
        Data dataLocalFileTransfer = new Data.Builder()
                .putString(Worker_LocalFileTransfer.KEY_ARG_JOB_REQUEST_DATETIME, sJobDateTime)
                .putString(Worker_LocalFileTransfer.KEY_ARG_JOB_FILE, sJobFileName)
                //.putInt(Worker_LocalFileTransfer.KEY_ARG_MEDIA_CATEGORY, iMediaCategory)
                //.putInt(Worker_LocalFileTransfer.KEY_ARG_COPY_OR_MOVE, iMoveOrCopy)
                //.putLong(Worker_LocalFileTransfer.KEY_ARG_TOTAL_IMPORT_SIZE_BYTES, lTotalImportSize)
                .build();
        OneTimeWorkRequest otwrLocalFileTransfer = new OneTimeWorkRequest.Builder(Worker_LocalFileTransfer.class)
                .setInputData(dataLocalFileTransfer)
                .addTag(Worker_LocalFileTransfer.WORKER_LOCAL_FILE_TRANSFER_TAG) //To allow finding the worker later.
                .build();
        UUID UUIDWorkID = otwrLocalFileTransfer.getId();
        WorkManager.getInstance(getApplicationContext()).enqueue(otwrLocalFileTransfer);

        globalClass.BroadcastProgress(true, "Operation complete.\n",
                false, iProgressBarValue,
                false, "",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);


        globalClass.gbImportExecutionRunning = false;
        globalClass.gbImportExecutionFinished = true;
        return Result.success();
    }

}
