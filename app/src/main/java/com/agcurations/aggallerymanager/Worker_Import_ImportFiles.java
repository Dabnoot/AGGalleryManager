package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.exoplayer2.util.MimeTypes;

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
                .putInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[GlobalClass.giSelectedCatalogMediaCategory],
                        GlobalClass.SORT_BY_DATETIME_IMPORTED)
                .putBoolean(GlobalClass.gsCatalogViewerPreferenceNameSortAscending[GlobalClass.giSelectedCatalogMediaCategory],
                        false)
                .apply();

        long lByteProgressNumerator = 0L;
        long lByteProgressDenominator;
        int iFileCountProgressNumerator = 0;
        int iFileCountProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;

        ArrayList<ItemClass_File> alFileList = globalClass.galImportFileList;

        //Calculate total size of all files to import:
        for(ItemClass_File fi: alFileList){
            lTotalImportSize = lTotalImportSize + fi.lSizeBytes;
        }
        lByteProgressDenominator = lTotalImportSize;
        iFileCountProgressDenominator = alFileList.size();

        String sMessage;
        String sLogLine;

        StringBuilder sbJobFileRecords = new StringBuilder();

        ArrayList<ItemClass_CatalogItem> alci_NewCatalogItems = new ArrayList<>();

        //Loop and import files:
        for(ItemClass_File fileItem: alFileList) {
            //todo: update progress bar and progress bar text here rather than multiple places throughout.
            if(fileItem.bMarkedForDeletion){

                Uri uriFileItem = Uri.parse(fileItem.sUri);
                if(GlobalClass.CheckIfFileExists(uriFileItem)){
                    sMessage = "Could not locate source file from uri: " + uriFileItem;
                    Log.d("Worker_Import_ImportFiles", sMessage);
                    continue;
                }

                //Write next behavior to the screen log:
                sLogLine = "Preparing data for job file: Delete file " + fileItem.sFileOrFolderName + ".\n";
                iFileCountProgressNumerator++;
                lByteProgressNumerator += fileItem.lSizeBytes;
                iProgressBarValue = Math.round((lByteProgressNumerator / (float) lByteProgressDenominator) * 100);
                globalClass.BroadcastProgress(true, sLogLine,
                        false, iProgressBarValue,
                        true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                String sLine = sCreateJobFileRecord(
                        uriFileItem.toString(),
                        fileItem.sDestinationFolder,
                        fileItem.sFileOrFolderName,
                        fileItem.lSizeBytes,
                        true);                 //Item marked for deletion?
                sbJobFileRecords.append(sLine);

                if(fileItem.iTypeFileFolderURL == ItemClass_File.TYPE_IMAGE_FROM_HOLDING_FOLDER){
                    //If this file is in the image download holding folder, mark the metadata file for deletion as well.
                    String sFileName = GlobalClass.GetFileName(uriFileItem);
                    String sMetadataFileName = sFileName + ".txt"; //The file will have two extensions.

                    Uri uriMetadataFile = GlobalClass.FormChildUri(GlobalClass.gUriImageDownloadHoldingFolder.toString(), sMetadataFileName);
                    if (!GlobalClass.CheckIfFileExists(uriMetadataFile)) {
                        sMessage = "Could not locate metadata file in location " + GlobalClass.gUriImageDownloadHoldingFolder;
                        Log.d("Worker_Import_ImportFiles", sMessage);
                        continue;
                    }

                    sLine = sCreateJobFileRecord(
                            uriMetadataFile.toString(),
                            fileItem.sDestinationFolder,
                            fileItem.sFileOrFolderName,
                            fileItem.lSizeBytes,
                            true);

                    sbJobFileRecords.append(sLine);

                }

                continue; //jump to next item in import list.
            } //End if item is marked for deletion.

            if(fileItem.sDestinationFolder.equals("")) {
                fileItem.sDestinationFolder = GlobalClass.gsUnsortedFolderName;
            }

            String sFileName;
            String sImageMetadataUri = "";
            Uri uriFileItemSource = Uri.parse(fileItem.sUri);
            if(!GlobalClass.CheckIfFileExists(uriFileItemSource)){
                sMessage = "Could not locate source file from uri: " + uriFileItemSource;
                Log.d("Worker_Import_ImportFiles", sMessage);
                globalClass.BroadcastProgress(true, "Problem with source reference for " + fileItem.sFileOrFolderName + "\n",
                        false, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                lByteProgressNumerator += fileItem.lSizeBytes;
                continue;
            }
            sFileName = GlobalClass.GetFileName(uriFileItemSource);
            if(fileItem.iTypeFileFolderURL == ItemClass_File.TYPE_IMAGE_FROM_HOLDING_FOLDER){
                String sMetadataFileName = sFileName + ".txt"; //The file will have two extensions.
                Uri uriMetadataFile = GlobalClass.FormChildUri(GlobalClass.gUriImageDownloadHoldingFolder.toString(), sMetadataFileName);
                if (uriMetadataFile == null) {
                    sMessage = "Could not locate metadata file in location " + GlobalClass.gUriImageDownloadHoldingFolder;
                    Log.d("Worker_Import_ImportFiles", sMessage);
                    continue;
                }
                sImageMetadataUri = uriMetadataFile.toString();
            }


            try {


                ItemClass_CatalogItem ciNew = new ItemClass_CatalogItem();
                ciNew.sItemID = globalClass.getNewCatalogRecordID(giMediaCategory);

                //Reverse the text on the file so that the file does not get picked off by a search tool:
                String sTempFileName = ciNew.sItemID + "_" + sFileName; //Create unique filename. Using ID will allow database error checking.
                if(sTempFileName.length() > 50){
                    //Limit the length of the filename:
                    String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sTempFileName);
                    if(sBaseAndExtension.length == 2) {
                        sTempFileName = sBaseAndExtension[0].substring(0, 50 - sBaseAndExtension[1].length());
                        sTempFileName = sTempFileName + "." + sBaseAndExtension[1];
                    }
                }
                sFileName = GlobalClass.JumbleFileName(sTempFileName);


                //Write next behavior to the screen log:
                sLogLine = "Preparing data for job file: Import " + fileItem.sFileOrFolderName + ".\n";
                iFileCountProgressNumerator++;
                iProgressBarValue = Math.round((lByteProgressNumerator / (float) lByteProgressDenominator) * 100);
                globalClass.BroadcastProgress(true, sLogLine,
                        false, iProgressBarValue,
                        true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                String sLine = sCreateJobFileRecord(
                        uriFileItemSource.toString(),
                        fileItem.sDestinationFolder,
                        sFileName,
                        fileItem.lSizeBytes,
                        false);

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
                ciNew.sTitle = "" + fileItem.sTitle; //If this is a file from the holding folder, it may have an original, unshortened file name.

                if(!fileItem.sURL.equals("")){
                    ciNew.sSource = fileItem.sURL;
                    //Prepare to delete any metadata file that might exist associated with this file.
                    // This is different from the "delete metadata file" in an earlier section of
                    // this code. The earlier is related to when the user has merely decided to
                    // delete a media item, not to import it.
                    if(!sImageMetadataUri.equals("")) {
                        sLine = sCreateJobFileRecord(
                                sImageMetadataUri,
                                fileItem.sDestinationFolder,
                                fileItem.sFileOrFolderName,
                                100, //Size should be quite small.
                                true);

                        sbJobFileRecords.append(sLine);
                    }
                }

                alci_NewCatalogItems.add(ciNew);


            } catch (Exception e) {
                globalClass.BroadcastProgress(true, "Problem with copy/move operation.\n" + e.getMessage(),
                        false, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;
                return Result.failure();
            }


        } //End fileItem processing loop.

        //Create catalog records in both the catalog contents file and memory:
        globalClass.CatalogDataFile_CreateNewRecords(alci_NewCatalogItems);


        //Prepare the job file from a StringBuilder configured in the loop above:
        String sJobDateTime = GlobalClass.GetTimeStampFileSafe();
        String sJobFileName = "Job_" + sJobDateTime + ".txt";

        try {

            //Inform user of preparation of worker:
            String sMoveOrCopy = GlobalClass.gsMoveOrCopy[giMoveOrCopy];
            sLogLine = "Using background worker for file " + sMoveOrCopy.toLowerCase() + " operations.\n"
                    + "Preparing job file.\n\n";
            globalClass.BroadcastProgress(true, sLogLine,
                    false, iProgressBarValue,
                    true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

            //Create a file with a listing of the files to be copied/moved:
            Uri uriJobFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, GlobalClass.gUriJobFilesFolder, MimeTypes.BASE_TYPE_TEXT, sJobFileName);
            if(uriJobFile == null){
                sMessage = "Could not create job file.";
                Log.d("Worker_Import_ImportFiles", sMessage);
                return Result.failure(DataErrorMessage(sMessage));
            }
            OutputStream osJobFile = GlobalClass.gcrContentResolver.openOutputStream(uriJobFile, "wt");
            if(osJobFile == null){
                sMessage = "Could not open output stream to job file.";
                Log.d("Worker_Import_ImportFiles", sMessage);
                return Result.failure(DataErrorMessage(sMessage));
            }
            BufferedWriter bwJobFile = new BufferedWriter(new OutputStreamWriter(osJobFile));
            //Write the data header:
            String sConfig = "MediaCategory:" + GlobalClass.gsCatalogFolderNames[giMediaCategory] + "\t"
                    + "MoveOrCopy:" + sMoveOrCopy + "\t"
                    + "TotalSize:" + lTotalImportSize + "\t"
                    + "FileCount:" + alFileList.size() + "\n";
            bwJobFile.write(sConfig);
            bwJobFile.write(sbJobFileRecords.toString());
            bwJobFile.flush();
            bwJobFile.close();
            osJobFile.flush();
            osJobFile.close();



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
                true, iFileCountProgressNumerator + "/" + iFileCountProgressDenominator + " files written to job file",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

        //Build-out data to send to the worker:
        Data dataLocalFileTransfer = new Data.Builder()
                .putString(Worker_LocalFileTransfer.KEY_ARG_JOB_REQUEST_DATETIME, sJobDateTime)
                .putString(Worker_LocalFileTransfer.KEY_ARG_JOB_FILE, sJobFileName)
                .build();
        OneTimeWorkRequest otwrLocalFileTransfer = new OneTimeWorkRequest.Builder(Worker_LocalFileTransfer.class)
                .setInputData(dataLocalFileTransfer)
                .addTag(Worker_LocalFileTransfer.WORKER_LOCAL_FILE_TRANSFER_TAG) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(otwrLocalFileTransfer);

        globalClass.BroadcastProgress(true, "Operation complete.\n",
                false, iProgressBarValue,
                false, "",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);


        globalClass.gbImportExecutionRunning = false;
        globalClass.gbImportExecutionFinished = true;
        return Result.success();
    }


    /**
     * Builds a string to be written to the job file. This routine serves to ensure that the proper
     * arguments, types, and usage are correct; this routine is to reduce programmer error.
     *
     * @param sSourceUri                A Uri for the source file.
     * @param sDestinationFolderName    Name of the folder to hold this file. The header specifying
     *                                  the Catalog type (Videos, Pictures, Comics) is to include
     *                                  this folder name.
     * @param sFileOrFolderName         The destination file name.
     * @param lSizeInBytes              The size of the file for progress display to user.
     * @param bMarkForDeletion          A flag to mark if this file should be deleted rather than copied
     *                                  or moved.
     * @return sJobFileRecord           A string is returned to be written to the job file.
     */
    private String sCreateJobFileRecord(String sSourceUri,
                                        String sDestinationFolderName,
                                        String sFileOrFolderName,
                                        long lSizeInBytes,
                                        boolean bMarkForDeletion){

        return sSourceUri + "\t" +
        sDestinationFolderName + "\t" +
        sFileOrFolderName + "\t" +
        lSizeInBytes + "\t" +
        bMarkForDeletion + "\n";
    }

    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }

}
