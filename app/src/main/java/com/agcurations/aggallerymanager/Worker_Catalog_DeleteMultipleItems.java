package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

public class Worker_Catalog_DeleteMultipleItems extends Worker {

    public static final String TAG_WORKER_CATALOG_DELETE_MULTIPLE_ITEMS = "com.agcurations.aggallermanager.tag_worker_catalog_delete_multiple_items";

    public static final String DELETE_MULTIPLE_ITEMS_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.DELETE_MULTIPLE_ITEMS_ACTION_RESPONSE";

    String gsResponseActionFilter;
    ArrayList<ItemClass_CatalogItem> galci_CatalogItemsToDelete;

    Uri gUriDataFile;

    String gsUserName = "";

    public Worker_Catalog_DeleteMultipleItems(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsResponseActionFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);

        gsUserName = getInputData().getString(GlobalClass.EXTRA_STRING_USERNAME);

        //Get the name of the data file containing the list of catalog items to be deleted. This
        // file should written by the subroutine initiating this worker:
        String sDataFileUriString = getInputData().getString(GlobalClass.EXTRA_DATA_FILE_URI_STRING);

        //Read the data file to get a list of the catalog items:
        gUriDataFile = Uri.parse(sDataFileUriString);

    }

    @NonNull
    @Override
    public Result doWork() {

        String sMessage;
        int iProgressNumerator = 0;
        int iProgressDenominator = 1;
        int iProgressBarValue = 0;

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        if (!GlobalClass.CheckIfFileExists(gUriDataFile)) {
            return Result.success();

        } else {

            globalClass.BroadcastProgress(false, "",
                    false, iProgressBarValue,
                    true, "Reading data for removal of catalog data and records....",
                    DELETE_MULTIPLE_ITEMS_ACTION_RESPONSE);

            //Read the list of entries and populate the catalog array:
            InputStream isCatalogReader = null;

            galci_CatalogItemsToDelete = new ArrayList<>();

            try {

                isCatalogReader = GlobalClass.gcrContentResolver.openInputStream(gUriDataFile);

                String sCatalogRecordData;
                String[] sCatalogRecords = null;

                if(isCatalogReader != null) {

                    byte[] bytesCatalogData = GlobalClass.readAllBytes(isCatalogReader);

                    isCatalogReader.close();

                    sCatalogRecordData = new String(bytesCatalogData);
                    sCatalogRecords = sCatalogRecordData.split("\n");

                    ItemClass_CatalogItem ci;
                    String sLine;
                    for (String sCatalogRecord : sCatalogRecords) {
                        if (sCatalogRecord.equals("")) {
                            continue;
                        }
                        ci = GlobalClass.ConvertStringToCatalogItem(sCatalogRecord);
                        galci_CatalogItemsToDelete.add(ci);
                    }
                }

                iProgressDenominator = galci_CatalogItemsToDelete.size();

            } catch (IOException e) {
                sMessage = "Trouble reading data file at: " + GlobalClass.GetFileName(gUriDataFile);
                LogThis("doWork()", sMessage, null);
                return Result.failure(DataErrorMessage(sMessage));
            } finally {
                if (isCatalogReader != null) {
                    try {
                        isCatalogReader.close();
                    } catch (Exception e) {
                        sMessage = "Problem during Catalog Contents file reader close:\n" + GlobalClass.GetFileName(gUriDataFile) + "\n\n" + e.getMessage();
                        LogThis("doWork()", sMessage, null);
                    }
                }
            }
        }

        if(galci_CatalogItemsToDelete.size() == 0){
            return Result.success();
        }

        //We should now have a listing of all catalog items to be deleted.

        //Delete the item record from the CatalogContentsFile:

        boolean bAllFilesDeleted = true;

        ArrayList<ItemClass_CatalogItem> alicci_VideoCatalogItemsToDelete = new ArrayList<>();
        ArrayList<ItemClass_CatalogItem> alicci_ImageCatalogItemsToDelete = new ArrayList<>();
        ArrayList<ItemClass_CatalogItem> alicci_ComicCatalogItemsToDelete = new ArrayList<>();

        //Delete the file(s):
        for(ItemClass_CatalogItem ciToDelete: galci_CatalogItemsToDelete) {
            iProgressNumerator++;
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(false, "",
                    false, iProgressBarValue,
                    true, "Deleting data for catalog item " + iProgressNumerator + "\\" + iProgressDenominator,
                    DELETE_MULTIPLE_ITEMS_ACTION_RESPONSE);

            boolean bSuccess = true;
            //Break out listing of items by media category here, for a non-repeat of this loop, to
            //  facilitate a fast-update of the individual catalog data files:
            if(ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS){
                alicci_VideoCatalogItemsToDelete.add(ciToDelete);
            } else if(ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES){
                alicci_ImageCatalogItemsToDelete.add(ciToDelete);
            } else if(ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                alicci_ComicCatalogItemsToDelete.add(ciToDelete);
            }

            Uri uriItemFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[ciToDelete.iMediaCategory], ciToDelete.sFolder_Name);

            if (!GlobalClass.CheckIfFileExists(uriItemFolder)) {
                //Could not find folder holding item to be deleted. Item must have failed to be integrated or deleted via
                //  some other method. Proceed with removing data from catalog.
                bSuccess = globalClass.deleteItemFromCatalogFile(ciToDelete, gsResponseActionFilter);
            } else {

                boolean bSingleFileDeleteAndFileNotFound = false;

                if (!ciToDelete.sSource.startsWith("http")) {
                    //If the source is not from the web...
                    Uri uriFileToBeDeleted = GlobalClass.FormChildUri(uriItemFolder, ciToDelete.sFilename);
                    if (GlobalClass.CheckIfFileExists(uriFileToBeDeleted)) {
                        try {
                            if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriFileToBeDeleted)) {
                                globalClass.problemNotificationConfig("Could not delete file.", gsResponseActionFilter);
                                bSuccess = false;
                            }
                        } catch (FileNotFoundException e) {
                            globalClass.problemNotificationConfig("Could not delete file.", gsResponseActionFilter);
                        }
                    } else {
                        globalClass.problemNotificationConfig("Could not find file at this location: " + uriItemFolder + ".", gsResponseActionFilter);
                        bSuccess = false;
                        bSingleFileDeleteAndFileNotFound = true; //Used to provide easy delete for a single item.
                    }
                } else {
                    //If the source is from the web...
                    //Delete the temporary download folders, etc.
                    //Delete any working folders:

                    if (ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                        if (ciToDelete.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {

                            Uri uriVideoWorkingFolder = GlobalClass.FormChildUri(uriItemFolder, ciToDelete.sItemID);
                            if (GlobalClass.CheckIfFileExists(uriVideoWorkingFolder)) {

                                //Delete folder:
                                try {
                                    if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriVideoWorkingFolder)) {
                                        Log.d("File Deletion", "Unable to delete folder " + uriVideoWorkingFolder + ".");
                                    }
                                } catch (FileNotFoundException e) {
                                    Log.d("File Deletion", "Unable to delete folder " + uriVideoWorkingFolder + ".");
                                }

                                //}

                            }
                        }

                        //Check to see if the download folder exists as well and delete if it does:
                        //Delete the download folder to which downloadManager downloaded the files:
                        String sDownloadFolderRelativePath;
                        sDownloadFolderRelativePath = File.separator + GlobalClass.gsCatalogFolderNames[ciToDelete.iMediaCategory] +
                                File.separator + ciToDelete.sFolder_Name +
                                File.separator + ciToDelete.sItemID;
                        if (getApplicationContext().getExternalFilesDir(null) != null) {
                            String sExternalFilesDir = Objects.requireNonNull(getApplicationContext().getExternalFilesDir(null)).getAbsolutePath();
                            String sItemDownloadFolder = sExternalFilesDir +
                                    sDownloadFolderRelativePath;
                            File fItemDownloadFolder = new File(sItemDownloadFolder);
                            if (fItemDownloadFolder.exists()) {

                                File[] fItemDownloadFolderContents = fItemDownloadFolder.listFiles();
                                if (fItemDownloadFolderContents != null) {
                                    for (File f1 : fItemDownloadFolderContents) {
                                        if (!f1.delete()) {
                                            Log.d("File Deletion", "Unable to delete file " + f1.getAbsolutePath() + ".");
                                        }
                                    }
                                } //End attempt to delete download folder contents for this video.

                                if (!fItemDownloadFolder.delete()) {
                                    Log.d("File Deletion", "Unable to delete folder " + fItemDownloadFolder.getAbsolutePath());
                                }

                                //Delete the category folder on the temp storage if it is empty:
                                String sDownloadCategoryFolderRelativePath;
                                sDownloadCategoryFolderRelativePath = sExternalFilesDir +
                                        File.separator + GlobalClass.gsCatalogFolderNames[ciToDelete.iMediaCategory] +
                                        File.separator + ciToDelete.sFolder_Name;
                                File fCatFolder = new File(sDownloadCategoryFolderRelativePath);
                                if (fCatFolder.exists()) {
                                    File[] fCatFolderContents = fCatFolder.listFiles();
                                    if (fCatFolderContents != null) {
                                        if (fCatFolderContents.length == 0) {
                                            if (!fCatFolder.delete()) {
                                                Log.d("File Deletion", "Unable to delete folder " + fCatFolder.getAbsolutePath() + ".");
                                            }
                                        }
                                    }
                                }
                            }

                        }
                        //End if the catalog item sourced as a download was a video item.
                    } else if (ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {

                        //Delete the single file...
                        Uri uriFileToBeDeleted = GlobalClass.FormChildUri(uriItemFolder, ciToDelete.sFilename);
                        if (GlobalClass.CheckIfFileExists(uriFileToBeDeleted)) {
                            try {
                                if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriFileToBeDeleted)) {
                                    globalClass.problemNotificationConfig("Could not delete file.", gsResponseActionFilter);
                                    bSuccess = false;
                                }
                            } catch (FileNotFoundException e) {
                                globalClass.problemNotificationConfig("Could not delete file.", gsResponseActionFilter);
                                bSuccess = false;
                            }
                        } else {
                            globalClass.problemNotificationConfig("Could not find file at this location: " + uriItemFolder + ".", gsResponseActionFilter);
                            bSuccess = false;
                            bSingleFileDeleteAndFileNotFound = true; //Used to provide easy delete for a single item.
                        }

                        //Check to see if the download folder exists as well and delete if it does:
                        //Delete the download folder to which downloadManager downloaded the files:
                        String sDownloadFolderRelativePath;
                        sDownloadFolderRelativePath = File.separator + GlobalClass.gsCatalogFolderNames[ciToDelete.iMediaCategory] +
                                File.separator + ciToDelete.sFolder_Name;
                        if (getApplicationContext().getExternalFilesDir(null) != null) {
                            String sExternalFilesDir = Objects.requireNonNull(getApplicationContext().getExternalFilesDir(null)).getAbsolutePath();
                            String sItemDownloadFolder = sExternalFilesDir +
                                    sDownloadFolderRelativePath;
                            File fItemDownloadFolder = new File(sItemDownloadFolder);
                            if (fItemDownloadFolder.exists()) {
                                File[] fItemDownloadFolderContents = fItemDownloadFolder.listFiles();
                                if (fItemDownloadFolderContents != null) {
                                    if (fItemDownloadFolderContents.length == 0) {
                                        if (!fItemDownloadFolder.delete()) {
                                            Log.d("File Deletion", "Unable to delete folder " + fItemDownloadFolder.getAbsolutePath() + " which was tested to be empty.");
                                        }
                                    }
                                }

                                //Delete the category folder on the temp storage if it is empty:
                                String sDownloadCategoryFolderRelativePath;
                                sDownloadCategoryFolderRelativePath = sExternalFilesDir +
                                        File.separator + GlobalClass.gsCatalogFolderNames[ciToDelete.iMediaCategory];
                                File fCatFolder = new File(sDownloadCategoryFolderRelativePath);
                                if (fCatFolder.exists()) {
                                    File[] fCatFolderContents = fCatFolder.listFiles();
                                    if (fCatFolderContents != null) {
                                        if (fCatFolderContents.length == 0) {
                                            if (!fCatFolder.delete()) {
                                                Log.d("File Deletion", "Unable to delete folder " + fCatFolder.getAbsolutePath() + " which was tested to be empty.");
                                            }
                                        }
                                    }
                                }
                            }

                        }

                        //End if the catalog item sourced as a download was an image item.
                    } else if (ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                        //Delete folder:
                        try {
                            if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriItemFolder)) {
                                Log.d("File Deletion", "Unable to delete folder " + uriItemFolder + ".");
                            }
                        } catch (FileNotFoundException e) {
                            Log.d("File Deletion", "Unable to delete folder " + uriItemFolder + ".");
                        }
                    }


                    //End if the catalog item was sourced as a download.

                }

                Uri uriLogFolder = GlobalClass.gUriLogsFolder;
                if (GlobalClass.CheckIfFileExists(uriLogFolder)) {
                    ArrayList<String> alsLogFiles = GlobalClass.GetDirectoryFileNames(uriLogFolder);

                    for (String sLogFileName : alsLogFiles) {

                        if (sLogFileName.startsWith(ciToDelete.sItemID)) {
                            Uri uriLogFileToDelete = GlobalClass.FormChildUri(uriLogFolder, sLogFileName);
                            try {
                                if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriLogFileToDelete)) {
                                    globalClass.problemNotificationConfig("Could not delete log file associated with this item: " + sLogFileName + ".", gsResponseActionFilter);
                                }
                            } catch (FileNotFoundException e) {
                                globalClass.problemNotificationConfig("Could not delete log file associated with this item: " + sLogFileName + ".", gsResponseActionFilter);
                            }
                        }

                    }

                }

                if (bSuccess) {

                    //Delete the folder if the folder is now empty:
                    if (GlobalClass.IsDirEmpty(uriItemFolder)) {
                        try {
                            if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriItemFolder)) {
                                globalClass.problemNotificationConfig("Folder holding this item is empty, but could not delete folder. Folder name: " + uriItemFolder + ".", gsResponseActionFilter);
                            }
                        } catch (FileNotFoundException e) {
                            globalClass.problemNotificationConfig("Folder holding this item is empty, but could not delete folder. Folder name: " + uriItemFolder + ".", gsResponseActionFilter);
                        }
                    }


                    //Now delete the item record from the Catalog File:
                    bSuccess = globalClass.deleteItemFromCatalogFile(ciToDelete, gsResponseActionFilter);

                    //End if for continuing after successful file deletion.
                } else {
                    //Check to see if this is a single video file or single image file. If so, deletion
                    //  failue most likely due to unable to find file. If this is true, delete the catalog
                    //  item.
                    if (ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES ||
                            (ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS &&
                                    (ciToDelete.iSpecialFlag == ItemClass_CatalogItem.FLAG_NO_CODE || ciToDelete.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_DLM_SINGLE))) {
                        if (bSingleFileDeleteAndFileNotFound) {
                            bSuccess = globalClass.deleteItemFromCatalogFile(ciToDelete, gsResponseActionFilter);
                        }
                    }
                }
            }
            bAllFilesDeleted &= bSuccess;
        }

        if(!bAllFilesDeleted){
            sMessage = "Unable to delete all files.";
            LogThis("doWork()", sMessage, null);
            return Result.failure(DataErrorMessage(sMessage));
        }

        //Remove items from the catalog file(s):
        boolean bUpdatedAllCatalogFiles = true;
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){
            boolean bSuccess = false;
            if(!GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].get()){
                try {
                    Thread.sleep(250);
                } catch (Exception e){
                    sMessage = "Error while waiting for catalog file to be available. " + e.getMessage();
                    LogThis("doWork()", sMessage, null);
                    bSuccess = false;
                    bUpdatedAllCatalogFiles &= bSuccess;
                    continue;
                }
            }
            GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(false);

            //Update this catalog file:
            bSuccess = GlobalClass.CatalogDataFile_UpdateCatalogFile(iMediaCategory);

            bUpdatedAllCatalogFiles &= bSuccess;
            GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(true);
        }

        if(!bUpdatedAllCatalogFiles){
            sMessage = "Unable to update all catalog data files.";
            LogThis("doWork()", sMessage, null);
        } else {
            //todo: Prompt user to restart the program to complete the user deletion?
        }

        if(!gsUserName.equals("")) {
            //If this multi-delete is related to the deletion of a user,
            //  start user deletion worker:
            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            Data dataDeleteUser = new Data.Builder()
                    .putString(GlobalClass.EXTRA_CALLER_ID, "Worker_Catalog_DeleteMultipleItems:doWork()")
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                    .putString(GlobalClass.EXTRA_STRING_USERNAME, gsUserName)
                    .build();
            OneTimeWorkRequest otwrUserDelete = new OneTimeWorkRequest.Builder(Worker_User_Delete.class)
                    .setInputData(dataDeleteUser)
                    .addTag(Worker_User_Delete.TAG_WORKER_USER_DELETE) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(otwrUserDelete);


        }


        //Broadcast the result of the delete item action:
        Intent broadcastIntent_DeleteCatalogItemResponse = new Intent();
        broadcastIntent_DeleteCatalogItemResponse.putExtra(GlobalClass.EXTRA_BOOL_DELETE_ITEM, true);
        broadcastIntent_DeleteCatalogItemResponse.putExtra(GlobalClass.EXTRA_BOOL_DELETE_ITEM_RESULT, bUpdatedAllCatalogFiles);
        broadcastIntent_DeleteCatalogItemResponse.setAction(Activity_CatalogViewer.CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
        broadcastIntent_DeleteCatalogItemResponse.addCategory(Intent.CATEGORY_DEFAULT);
        //sendBroadcast(broadcastIntent_DeleteCatalogItemResponse);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_DeleteCatalogItemResponse);

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
        Log.d("Worker_Catalog_DeleteMultipleItems:" + sRoutine, sMessage);
    }


}
