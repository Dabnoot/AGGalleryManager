package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.InputStream;
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

public class Worker_Import_ImportFiles extends Worker {

    public static final String TAG_WORKER_IMPORT_IMPORTFILES = "com.agcurations.aggallermanager.tag_worker_import_importfiles";

    public static final String IMPORT_FILES_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.IMPORT_FILES_ACTION_RESPONSE";
    
    int giMoveOrCopy;
    int giMediaCategory;

    public Worker_Import_ImportFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        giMoveOrCopy = getInputData().getInt(GlobalClass.EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
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

        int iFileCountProgressNumerator = 0;
        int iFileCountProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;

        int iMetadata_File_Count = 0;

        ArrayList<ItemClass_File> alFileList = (ArrayList<ItemClass_File>)globalClass.galImportFileList.clone();

        //Calculate total size of all files to import:
        for(ItemClass_File fi: alFileList){
            lTotalImportSize = lTotalImportSize + fi.lSizeBytes;
        }

        iFileCountProgressDenominator = alFileList.size();

        String sMessage;
        String sLogLine;

        StringBuilder sbJobFileRecords = new StringBuilder();

        ArrayList<ItemClass_CatalogItem> alci_NewCatalogItems = new ArrayList<>();

        MediaMetadataRetriever mediaMetadataRetriever;
        mediaMetadataRetriever = new MediaMetadataRetriever();

        globalClass.BroadcastProgress(true, "Preparing data for job file.\n",
                false, iProgressBarValue,
                true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                IMPORT_FILES_ACTION_RESPONSE);

        //Loop and examine list of files to be imported, build a buffer of records to write to a job file,
        // and create catalog item records:
        for(ItemClass_File fileItem: alFileList) {
            //todo: update progress bar and progress bar text here rather than multiple places throughout.
            if(fileItem.bMarkedForDeletion){

                Uri uriFileItem = Uri.parse(fileItem.sUri);
                if(!GlobalClass.CheckIfFileExists(uriFileItem)){
                    sMessage = "Could not locate source file from uri: " + uriFileItem;
                    Log.d("Worker_Import_ImportFiles", sMessage);
                    continue;
                }

                //Write next behavior to the screen log:
                sLogLine = fileItem.sFileOrFolderName + " will be deleted.\n";
                iFileCountProgressNumerator++;
                iProgressBarValue = Math.round((iFileCountProgressNumerator / (float) iFileCountProgressDenominator) * 100);
                globalClass.BroadcastProgress(true, sLogLine,
                        true, iProgressBarValue,
                        true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                        IMPORT_FILES_ACTION_RESPONSE);
                String sLine = Worker_LocalFileTransfer.CreateJobFileRecord(
                        uriFileItem.toString(),
                        fileItem.sDestinationFolder,
                        fileItem.sFileOrFolderName,
                        fileItem.lSizeBytes,
                        true,
                        false);                 //Item marked for deletion?
                sbJobFileRecords.append(sLine);

                if(fileItem.iTypeFileFolderURL == ItemClass_File.TYPE_IMAGE_FROM_HOLDING_FOLDER){
                    //If this file is in the image download holding folder, mark the metadata file for deletion as well.
                    String sFileName = GlobalClass.GetFileName(uriFileItem);
                    String sMetadataFileName = sFileName + ".tad"; //The file will have two extensions.

                    Uri uriMetadataFile = GlobalClass.FormChildUri(GlobalClass.gUriImageDownloadHoldingFolder.toString(), sMetadataFileName);
                    if (!GlobalClass.CheckIfFileExists(uriMetadataFile)) {
                        sMessage = "Could not locate metadata file in location " + GlobalClass.gUriImageDownloadHoldingFolder;
                        Log.d("Worker_Import_ImportFiles", sMessage);
                        continue;
                    }

                    sLine = Worker_LocalFileTransfer.CreateJobFileRecord(
                            uriMetadataFile.toString(),
                            fileItem.sDestinationFolder,
                            fileItem.sFileOrFolderName,
                            fileItem.lMetadataFileSizeBytes,
                            true,
                            true);

                    sbJobFileRecords.append(sLine);

                    //iMetadata_File_Count++; //Need to include metadata files in the file count to be processed.
                    //  Don't include metadata files in the file count to be processed as they can confuse the user.
                }

                continue; //jump to next item in import list.
            } //End if item is marked for deletion.

            if(fileItem.sDestinationFolder.equals("")) {
                fileItem.sDestinationFolder = GlobalClass.gsUnsortedFolderName;
            }

            Uri uriFileItemSource = Uri.parse(fileItem.sUri);
            if(!GlobalClass.CheckIfFileExists(uriFileItemSource)){
                sMessage = "Could not locate source file from uri: " + uriFileItemSource;
                Log.d("Worker_Import_ImportFiles", sMessage);
                globalClass.BroadcastProgress(true, "Problem with source reference for " + fileItem.sFileOrFolderName + "\n",
                        false, iProgressBarValue,
                        false, "",
                        IMPORT_FILES_ACTION_RESPONSE);
                continue;
            }



            try {

                ItemClass_CatalogItem ciNew = new ItemClass_CatalogItem();
                ciNew.sItemID = GlobalClass.getNewCatalogRecordID();

                //Reverse the text on the file so that the file does not get picked off by a search tool:
                //Isolate the incoming file name:
                String sFileName = GlobalClass.GetFileName(uriFileItemSource);
                //Make sure the file name is not too long. If it is too long, shorten it. "Too long" is arbitrary here.
                String sTempFileName = sFileName;
                if(sTempFileName.length() > 50){
                    //Limit the length of the filename:
                    String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sTempFileName);
                    if(sBaseAndExtension.length == 2) {
                        sTempFileName = sBaseAndExtension[0].substring(0, 50 - sBaseAndExtension[1].length());
                        sTempFileName = sTempFileName + "." + sBaseAndExtension[1];
                    }
                }
                //Create unique filename, then jumble:
                if(fileItem.iTypeFileFolderURL == ItemClass_File.TYPE_IMAGE_FROM_HOLDING_FOLDER){
                    //File name will already be jumbled, as this program place the file in the holding folder.
                    sFileName = GlobalClass.getUniqueFileName(GlobalClass.GetParentUri(uriFileItemSource), sTempFileName, false);
                } else {
                    sFileName = GlobalClass.getUniqueFileName(GlobalClass.GetParentUri(uriFileItemSource), sTempFileName, true);
                }

                //Write next behavior to the screen log:
                sLogLine = fileItem.sFileOrFolderName + " will be imported.";
                iFileCountProgressNumerator++;
                iProgressBarValue = Math.round((iFileCountProgressNumerator / (float) iFileCountProgressDenominator) * 100);
                globalClass.BroadcastProgress(true, sLogLine,
                        true, iProgressBarValue,
                        true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                        IMPORT_FILES_ACTION_RESPONSE);

                String sLine = Worker_LocalFileTransfer.CreateJobFileRecord(
                        uriFileItemSource.toString(),
                        fileItem.sDestinationFolder,
                        sFileName,
                        fileItem.lSizeBytes,
                        false,
                        false);

                sbJobFileRecords.append(sLine);


                //Next add the data to the catalog file and memory:

                //Create a timestamp to be used to create the data record:
                Double dTimeStamp = GlobalClass.GetTimeStampDouble();

                ciNew.iMediaCategory = giMediaCategory;
                ciNew.sFilename = sFileName;
                ciNew.lSize = fileItem.lSizeBytes;

                if(!fileItem.bMetadataDetected){
                    //If the metadata for the file was not previously acquired during the folder examination, attempt to get the metadata here:

                    globalClass.BroadcastProgress(true, " Obtaining metadata for file... ",
                            false, iProgressBarValue,
                            false, "",
                            IMPORT_FILES_ACTION_RESPONSE);

                    if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {

                        if (fileItem.sMimeType.startsWith("video") ||
                                (fileItem.sMimeType.equals("application/octet-stream") && fileItem.sExtension.equals(".mp4"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid
                            try {
                                mediaMetadataRetriever.setDataSource(getApplicationContext(), Uri.parse(fileItem.sUri));
                            } catch (Exception e) {
                                globalClass.BroadcastProgress(true, "Could not get metadata for video.\n" + e.getMessage(),
                                        false, iProgressBarValue,
                                        false, "",
                                        IMPORT_FILES_ACTION_RESPONSE);
                            }
                            fileItem.sWidth = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                            fileItem.sHeight = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                            String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            if (time != null) {
                                fileItem.lVideoTimeInMilliseconds = Long.parseLong(time);
                            }
                        } else { //if it's not a video file, check to see if it's a gif:
                            if (fileItem.sExtension.equals(".gif")) {
                                //Get the duration of the gif image:
                                Context activityContext = getApplicationContext();
                                try {
                                    pl.droidsonroids.gif.GifDrawable gd = new pl.droidsonroids.gif.GifDrawable(activityContext.getContentResolver(), Uri.parse(fileItem.sUri));
                                    fileItem.lVideoTimeInMilliseconds = gd.getDuration();
                                    fileItem.sWidth = "" + gd.getIntrinsicWidth();
                                    fileItem.sHeight = "" + gd.getIntrinsicHeight();
                                } catch (Exception e) {
                                    globalClass.BroadcastProgress(true, "Could not get metadata for gif.\n" + e.getMessage(),
                                            false, iProgressBarValue,
                                            false, "",
                                            IMPORT_FILES_ACTION_RESPONSE);
                                }
                            }
                        }
                    } else if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                        //Get the width and height of the image:
                        try {
                            InputStream input = getApplicationContext().getContentResolver().openInputStream(Uri.parse(fileItem.sUri));
                            if (input != null) {
                                BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                                onlyBoundsOptions.inJustDecodeBounds = true;
                                BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
                                input.close();
                                fileItem.sWidth = "" + onlyBoundsOptions.outWidth;
                                fileItem.sHeight = "" + onlyBoundsOptions.outHeight;
                            }

                        } catch (Exception e) {
                            globalClass.BroadcastProgress(true, "Could not get metadata for image.\n" + e.getMessage(),
                                    false, iProgressBarValue,
                                    false, "",
                                    IMPORT_FILES_ACTION_RESPONSE);
                        }
                    }
                    globalClass.BroadcastProgress(true, "Metadata seek complete.\n",
                            false, iProgressBarValue,
                            false, "",
                            IMPORT_FILES_ACTION_RESPONSE);
                }

                ciNew.lDuration_Milliseconds = fileItem.lVideoTimeInMilliseconds;
                ciNew.sDuration_Text = fileItem.sVideoTimeText;
                if(!fileItem.sWidth.equals("") && !fileItem.sHeight.equals("")) {
                    ciNew.iWidth = Integer.parseInt(fileItem.sWidth);
                    ciNew.iHeight = Integer.parseInt(fileItem.sHeight);
                }
                ciNew.sFolderRelativePath = fileItem.sDestinationFolder;
                ciNew.sSource = ItemClass_CatalogItem.FOLDER_SOURCE;
                ciNew.sTags = GlobalClass.formDelimitedString(fileItem.aliProspectiveTags, ",");
                ciNew.aliTags = new ArrayList<>(fileItem.aliProspectiveTags);
                ciNew.iMaturityRating = GlobalClass.getHighestTagMaturityRating(ciNew.aliTags, ciNew.iMediaCategory);
                //ciNew.alsApprovedUsers.add(globalClass.gicuCurrentUser.sUserName);
                ciNew.alsApprovedUsers = GlobalClass.getApprovedUsersForTagGrouping(ciNew.aliTags, ciNew.iMediaCategory);
                ciNew.dDatetime_Last_Viewed_by_User = dTimeStamp;
                ciNew.dDatetime_Import = dTimeStamp;
                ciNew.iGrade = fileItem.iGrade;
                ciNew.sTitle = "" + fileItem.sTitle; //If this is a file from the holding folder, it may have an original, unshortened file name.
                ciNew.sGroupID = fileItem.sGroupID;

                if(!fileItem.sURL.equals("")){
                    ciNew.sSource = fileItem.sURL;
                    //Prepare to delete any metadata file that might exist associated with this file.
                    // This is different from the "delete metadata file" in an earlier section of
                    // this code. The earlier is related to when the user has merely decided to
                    // delete a media item, not to import it.

                    Uri uriMetadataFile = Activity_Import.getHoldingFolderItemMetadataFileUri(uriFileItemSource);
                    String sImageMetadataUri = uriMetadataFile.toString();
                    if(!sImageMetadataUri.equals("")) {
                        sLine = Worker_LocalFileTransfer.CreateJobFileRecord(
                                sImageMetadataUri,
                                fileItem.sDestinationFolder,
                                fileItem.sFileOrFolderName,
                                fileItem.lMetadataFileSizeBytes,
                                true,
                                true);

                        sbJobFileRecords.append(sLine);

                        //iMetadata_File_Count++; //Need to include metadata files in the file count to be processed.
                        //  Don't include metadata files in the file count to be processed as they can confuse the user.
                    }
                }

                //Check ensure that the record does not have any illegal character sequences that would mess with the data file:
                ciNew = GlobalClass.validateCatalogItemData(ciNew);
                if(ciNew == null){
                    //If we are here, validateCatalogItemData found a critical error, such as illegal character;
                    globalClass.BroadcastProgress(true, "Critical error with formation of data record. " +
                                    "File import and record creation aborted for this item. " +
                                    "Ensure user name or applied tag does not contain an illegal character.",
                            false, 0,
                            false, "",
                            IMPORT_FILES_ACTION_RESPONSE);
                    continue; //Skip to the next import item.
                }
                if(ciNew.bIllegalDataFound){
                    globalClass.BroadcastProgress(true, ciNew.sIllegalDataNarrative,
                            false, 0,
                            false, "",
                            IMPORT_FILES_ACTION_RESPONSE);
                }
                alci_NewCatalogItems.add(ciNew);


            } catch (Exception e) {
                globalClass.BroadcastProgress(true, "Problem with copy/move operation. Operation complete.\n" + e.getMessage(),
                        false, iProgressBarValue,
                        false, "",
                        IMPORT_FILES_ACTION_RESPONSE);
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
            sLogLine = "\nPreparing job file. ";
            globalClass.BroadcastProgress(true, sLogLine,
                    false, iProgressBarValue,
                    true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                    IMPORT_FILES_ACTION_RESPONSE);

            //Create a file with a listing of the files to be copied/moved:
            Uri uriJobFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, GlobalClass.gUriJobFilesFolder, GlobalClass.BASE_TYPE_TEXT, sJobFileName);
            if(uriJobFile == null){
                sMessage = "Could not create job file.";
                LogThis("doWork()", sMessage, null);
                return Result.failure(DataErrorMessage(sMessage));
            }
            OutputStream osJobFile = GlobalClass.gcrContentResolver.openOutputStream(uriJobFile, "wt");
            if(osJobFile == null){
                sMessage = "Could not open output stream to job file.";
                LogThis("doWork()", sMessage, null);
                return Result.failure(DataErrorMessage(sMessage));
            }
            BufferedWriter bwJobFile = new BufferedWriter(new OutputStreamWriter(osJobFile));
            //Write the data header:
            String sConfig = "MediaCategory:" + GlobalClass.gsCatalogFolderNames[giMediaCategory] + "\t"
                    + "MoveOrCopy:" + sMoveOrCopy + "\t"
                    + "TotalSize:" + lTotalImportSize + "\t"
                    + "FileCount:" + (alFileList.size() + iMetadata_File_Count) + "\n";
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
                    IMPORT_FILES_ACTION_RESPONSE);
            globalClass.gbImportExecutionRunning = false;
            globalClass.gbImportExecutionFinished = true;
            return Result.failure();
        }
        //Write next behavior to the screen log:
        sLogLine = "\nStarting worker to process job file. This 'job file creation worker' will end and 'job file process worker' will continue in the background.\n"
                + "Files will appear in the catalog as the worker progresses.\n"
                + "Refresh the catalog viewer (exit/re-enter, change sort direction) to view newly-added files.\n";
        globalClass.BroadcastProgress(true, sLogLine,
                false, iProgressBarValue,
                true, iFileCountProgressNumerator + "/" + iFileCountProgressDenominator + " files written to job file",
                IMPORT_FILES_ACTION_RESPONSE);

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

        globalClass.BroadcastProgress(true, "Job file creation worker operation complete.\n\n",
                false, iProgressBarValue,
                false, "",
                IMPORT_FILES_ACTION_RESPONSE);


        globalClass.gbImportExecutionRunning = false;
        globalClass.gbImportExecutionFinished = true;
        return Result.success();
    }

    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Import_ImportFiles:" + sRoutine, sMessage);
    }

}
