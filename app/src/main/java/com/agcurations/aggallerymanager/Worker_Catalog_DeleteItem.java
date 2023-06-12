package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Catalog_DeleteItem extends Worker {

    public static final String TAG_WORKER_CATALOG_DELETEITEM = "com.agcurations.aggallermanager.tag_worker_catalog_deleteitem";

    public static final String CATALOG_DELETE_ITEM_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_DELETE_ITEM_ACTION_RESPONSE";
    
    ItemClass_CatalogItem ciToDelete;

    public Worker_Catalog_DeleteItem(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        String sCatalogItemToDelete = getInputData().getString(GlobalClass.EXTRA_CATALOG_ITEM);
        if(sCatalogItemToDelete != null) {
            ciToDelete = GlobalClass.ConvertStringToCatalogItem(sCatalogItemToDelete);
        }
    }

    @NonNull
    @Override
    public Result doWork() {

        if(GlobalClass.gUriCatalogFolders[0] == null){
            //todo: Examine uninitialized case. This typically would occur when I was debugging this
            // routine, stopped the program, and the program restarted and attempted to run the
            // worker again.
            return Result.failure();
        }
        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        //Delete the item record from the CatalogContentsFile:
        boolean bSuccess = true;

        //Delete the file(s):

        Uri uriItemFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[ciToDelete.iMediaCategory], ciToDelete.sFolder_Name);

        if(!GlobalClass.CheckIfFileExists(uriItemFolder)){
            //Could not find folder holding item to be deleted. Item must have failed to be integrated or deleted via
            //  some other method. Proceed with removing data from catalog.
            bSuccess = globalClass.deleteItemFromCatalogFile(ciToDelete, CATALOG_DELETE_ITEM_ACTION_RESPONSE);
        } else {
            boolean bSingleFileDeleteAndFileNotFound = false;
            if (ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                // If the user is attempting to delete a comic, merely delete the comic folder:
                try {
                    if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriItemFolder)) {
                        Log.d("File Deletion", "Unable to delete folder " + uriItemFolder + ".");
                    }
                } catch (FileNotFoundException e) {
                    Log.d("File Deletion", "Unable to delete folder " + uriItemFolder + ".");
                }
            } else {

                if (!ciToDelete.sSource.startsWith("http")) {
                    //If the source is not from the web...
                    Uri uriFileToBeDeleted = GlobalClass.FormChildUri(uriItemFolder, ciToDelete.sFilename);
                    if (GlobalClass.CheckIfFileExists(uriFileToBeDeleted)) {
                        try {
                            if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriFileToBeDeleted)) {
                                globalClass.problemNotificationConfig("Could not delete file.", CATALOG_DELETE_ITEM_ACTION_RESPONSE);
                                bSuccess = false;
                            }
                        } catch (FileNotFoundException e) {
                            globalClass.problemNotificationConfig("Could not delete file.", CATALOG_DELETE_ITEM_ACTION_RESPONSE);
                        }
                    } else {
                        globalClass.problemNotificationConfig("Could not find file at this location: " + uriItemFolder + ".", CATALOG_DELETE_ITEM_ACTION_RESPONSE);
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
                                    globalClass.problemNotificationConfig("Could not delete file.", CATALOG_DELETE_ITEM_ACTION_RESPONSE);
                                    bSuccess = false;
                                }
                            } catch (FileNotFoundException e) {
                                globalClass.problemNotificationConfig("Could not delete file.", CATALOG_DELETE_ITEM_ACTION_RESPONSE);
                                bSuccess = false;
                            }
                        } else {
                            globalClass.problemNotificationConfig("Could not find file at this location: " + uriItemFolder + ".", CATALOG_DELETE_ITEM_ACTION_RESPONSE);
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

                    } //End if the catalog item sourced as a download was an image item.

                } //End if the catalog item was sourced as a download.

            } //End if the catalog item is not a comic.

            Uri uriLogFolder = GlobalClass.gUriLogsFolder;
            if (GlobalClass.CheckIfFileExists(uriLogFolder)) {
                ArrayList<String> alsLogFiles = GlobalClass.GetDirectoryFileNames(uriLogFolder);

                for (String sLogFileName : alsLogFiles) {

                    if(sLogFileName.startsWith(ciToDelete.sItemID)) {
                        Uri uriLogFileToDelete = GlobalClass.FormChildUri(uriLogFolder, sLogFileName);
                        try {
                            if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriLogFileToDelete)) {
                                globalClass.problemNotificationConfig("Could not delete log file associated with this item: " + sLogFileName + ".", CATALOG_DELETE_ITEM_ACTION_RESPONSE);
                            }
                        } catch (FileNotFoundException e) {
                            globalClass.problemNotificationConfig("Could not delete log file associated with this item: " + sLogFileName + ".", CATALOG_DELETE_ITEM_ACTION_RESPONSE);
                        }
                    }

                }
            }

            if (bSuccess) {
                if (ciToDelete.iMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS) { //Comic folder should already be deleted.
                    //Delete the folder if the folder is now empty:
                    if (GlobalClass.IsDirEmpty(uriItemFolder)) {
                        try {
                            if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriItemFolder)) {
                                globalClass.problemNotificationConfig("Folder holding this item is empty, but could not delete folder. Folder name: " + uriItemFolder + ".", CATALOG_DELETE_ITEM_ACTION_RESPONSE);
                            }
                        } catch (FileNotFoundException e) {
                            globalClass.problemNotificationConfig("Folder holding this item is empty, but could not delete folder. Folder name: " + uriItemFolder + ".", CATALOG_DELETE_ITEM_ACTION_RESPONSE);
                        }
                    }
                }
                //Now delete the item record from the Catalog File:
                bSuccess = globalClass.deleteItemFromCatalogFile(ciToDelete, CATALOG_DELETE_ITEM_ACTION_RESPONSE); //This item takes 4-5 seconds??

                //End if for continuing after successful file deletion.
            } else {
                //Check to see if this is a single video file or single image file. If so, deletion
                //  failue most likely due to unable to find file. If this is true, delete the catalog
                //  item.
                if (ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES ||
                        (ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS &&
                                (ciToDelete.iSpecialFlag == ItemClass_CatalogItem.FLAG_NO_CODE || ciToDelete.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_DLM_SINGLE))) {
                    if (bSingleFileDeleteAndFileNotFound) {
                        bSuccess = globalClass.deleteItemFromCatalogFile(ciToDelete, CATALOG_DELETE_ITEM_ACTION_RESPONSE);
                    }
                }
            }
        }


        //Broadcast the result of the delete item action:
        Intent broadcastIntent_DeleteCatalogItemResponse = new Intent();
        broadcastIntent_DeleteCatalogItemResponse.putExtra(GlobalClass.EXTRA_BOOL_DELETE_ITEM, true);
        broadcastIntent_DeleteCatalogItemResponse.putExtra(GlobalClass.EXTRA_BOOL_DELETE_ITEM_RESULT, bSuccess);
        broadcastIntent_DeleteCatalogItemResponse.setAction(CATALOG_DELETE_ITEM_ACTION_RESPONSE);
        broadcastIntent_DeleteCatalogItemResponse.addCategory(Intent.CATEGORY_DEFAULT);
        //sendBroadcast(broadcastIntent_DeleteCatalogItemResponse);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_DeleteCatalogItemResponse);

        return Result.success();
    }



}
