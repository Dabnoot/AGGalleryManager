package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Catalog_DeleteItem extends Worker {

    public static final String TAG_WORKER_CATALOG_DELETEITEM = "com.agcurations.aggallermanager.tag_worker_catalog_deleteitem";

    String gsResponseActionFilter;
    ItemClass_CatalogItem ciToDelete;

    public Worker_Catalog_DeleteItem(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsResponseActionFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);

        String sCatalogItemToDelete = getInputData().getString(GlobalClass.EXTRA_CATALOG_ITEM);
        if(sCatalogItemToDelete != null) {
            ciToDelete = GlobalClass.ConvertStringToCatalogItem(sCatalogItemToDelete);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        //Delete the item record from the CatalogContentsFile:
        boolean bSuccess = true;

        //Delete the file:

        //Get a path to the file to delete:
        String sCatalogFolderPath = globalClass.gfCatalogFolders[ciToDelete.iMediaCategory].getPath();
        String sItemFolderName = ciToDelete.sFolder_Name;
        String sItemFileName = ciToDelete.sFilename;

        String sFileFolder = sCatalogFolderPath + File.separator +
                sItemFolderName;
        String sFullPath = sFileFolder + File.separator +
                sItemFileName;
        File fFileToBeDeleted = new File(sFullPath);

        boolean bSingleFileDeleteAndFileNotFound = false;

        if(ciToDelete.sSource.startsWith("http")){
            //Delete the temporary download folders, etc.
            //Delete any working folders:
            String sVideoDestinationFolder = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath() +
                    File.separator + ciToDelete.sFolder_Name;
            String sVideoWorkingFolder = sVideoDestinationFolder + File.separator + ciToDelete.sItemID;
            File fVideoWorkingFolder = new File(sVideoWorkingFolder);
            if(fVideoWorkingFolder.exists()){
                File[] fVideoWorkingFolderListing = fVideoWorkingFolder.listFiles();
                ArrayList<File> alfOutputFolders = new ArrayList<>();
                if(fVideoWorkingFolderListing != null) {
                    for (File f : fVideoWorkingFolderListing) {
                        //Locate the output folder
                        if (f.isDirectory()) {
                            alfOutputFolders.add(f); //The worker could potentially create multiple output folders if it is re-run.
                        }
                    }
                    //Go through the output folders and delete contents:
                    for (File f2 : alfOutputFolders) {
                        File[] f2_Contents = f2.listFiles();
                        if (f2_Contents != null) {
                            for (File f3 : f2_Contents) {
                                if(!f3.delete()){
                                    Log.d("File Deletion", "Unable to delete file " + f3.getAbsolutePath() + ".");
                                }
                            }
                        }
                    }
                    //Delete download folder contents:
                    for (File f4 : fVideoWorkingFolderListing) {
                        if(!f4.delete()){
                            Log.d("File Deletion", "Unable to delete file or folder " + f4.getAbsolutePath() + ".");
                        }
                    }
                    //Delete download folder:
                    if(!fVideoWorkingFolder.delete()){
                        Log.d("File Deletion", "Unable to delete folder " + fVideoWorkingFolder.getAbsolutePath() + ".");
                    }

                }

            } //End if VideoWorkingFolderExists

            //Check to see if the download folder exists as well and delete if it does:
            //Delete the download folder to which downloadManager downloaded the files:
            String sDownloadFolderRelativePath;
            sDownloadFolderRelativePath = File.separator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS] +
                    File.separator + ciToDelete.sFolder_Name +
                    File.separator + ciToDelete.sItemID;
            if(getApplicationContext().getExternalFilesDir(null) != null) {

                String sVideoDownloadFolder = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() +
                        sDownloadFolderRelativePath;
                File fVideoDownloadFolder = new File(sVideoDownloadFolder);
                if (fVideoDownloadFolder.exists()) {

                    File[] fVideoDownloadFolderContents = fVideoDownloadFolder.listFiles();
                    if(fVideoDownloadFolderContents != null){
                        for(File f1:fVideoDownloadFolderContents){
                            if(!f1.delete()){
                                Log.d("File Deletion", "Unable to delete file " + f1.getAbsolutePath() + ".");
                            }
                        }
                    } //End attempt to delete download folder contents for this video.

                    if (!fVideoDownloadFolder.delete()) {
                        Log.d("File Deletion", "Unable to delete folder " + fVideoDownloadFolder.getAbsolutePath());
                    }

                    //Delete the category folder on the temp storage if it is empty:
                    String sDownloadCategoryFolderRelativePath;
                    sDownloadCategoryFolderRelativePath = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() +
                            File.separator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS] +
                            File.separator + ciToDelete.sFolder_Name;
                    File fCatFolder = new File(sDownloadCategoryFolderRelativePath);
                    if(fCatFolder.exists()){
                        File[] fCatFolderContents = fCatFolder.listFiles();
                        if(fCatFolderContents != null){
                            if(fCatFolderContents.length == 0){
                                if (!fCatFolder.delete()) {
                                    Log.d("File Deletion", "Unable to delete folder " + fCatFolder.getAbsolutePath() + ".");
                                }
                            }
                        }
                    }
                }

            }

            //End if the catalog item was sourced as a download.

        } else {

            if (fFileToBeDeleted.exists()) {
                if (!fFileToBeDeleted.delete()) {
                    globalClass.problemNotificationConfig("Could not delete file.", gsResponseActionFilter);
                    bSuccess = false;
                }
            } else {
                globalClass.problemNotificationConfig("Could not find file at this location: " + sFullPath + ".", gsResponseActionFilter);
                bSuccess = false;
                bSingleFileDeleteAndFileNotFound = true; //Used to provide easy delete for a single item.
            }
        }

        File fLogFolder = globalClass.gfLogsFolder;
        if(fLogFolder.exists()){
            File[] fLogFiles = fLogFolder.listFiles();
            if(fLogFiles != null){
                for(File f: fLogFiles){
                    if(f.isFile() && f.getName().startsWith(ciToDelete.sItemID)){
                        if(!f.delete()){
                            globalClass.problemNotificationConfig("Could not delete log file associated with this item: " + f.getName() + ".", gsResponseActionFilter);
                        }
                    }
                }
            }
        }

        if(bSuccess) {

            //Delete the folder if the folder is now empty:
            File fFolder = new File(sFileFolder);
            String[] sFilesRemaining = fFolder.list();
            if(sFilesRemaining != null) {
                if (sFilesRemaining.length == 0) {
                    if (!fFolder.delete()) {
                        globalClass.problemNotificationConfig("Folder holding this item is empty, but could not delete folder. Folder name: " + sFileFolder + ".", gsResponseActionFilter);
                    }
                }
            }

            //Now delete the item record from the Catalog File:
            bSuccess = globalClass.deleteItemFromCatalogFile(ciToDelete, gsResponseActionFilter);

            //End if for continuing after successful file deletion.
        } else {
            //Check to see if this is a single video file or single image file. If so, deletion
            //  failue most likely due to unable to find file. If this is true, delete the catalog
            //  item.
            if(ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES ||
                    (ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS &&
                            (ciToDelete.iSpecialFlag == ItemClass_CatalogItem.FLAG_NO_CODE || ciToDelete.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_DLM_SINGLE))){
                if(bSingleFileDeleteAndFileNotFound){

                    bSuccess = globalClass.deleteItemFromCatalogFile(ciToDelete, gsResponseActionFilter);

                }
            }
        }



        //Broadcast the result of the delete item action:
        Intent broadcastIntent_DeleteCatalogItemResponse = new Intent();
        broadcastIntent_DeleteCatalogItemResponse.putExtra(GlobalClass.EXTRA_BOOL_DELETE_ITEM, true);
        broadcastIntent_DeleteCatalogItemResponse.putExtra(GlobalClass.EXTRA_BOOL_DELETE_ITEM_RESULT, bSuccess);
        broadcastIntent_DeleteCatalogItemResponse.setAction(Activity_CatalogViewer.CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
        broadcastIntent_DeleteCatalogItemResponse.addCategory(Intent.CATEGORY_DEFAULT);
        //sendBroadcast(broadcastIntent_DeleteCatalogItemResponse);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_DeleteCatalogItemResponse);

        return Result.success();
    }



}
