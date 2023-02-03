package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
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

        //Delete the file(s):

        DocumentFile dfItemFolder = globalClass.gdfCatalogFolders[ciToDelete.iMediaCategory].findFile(ciToDelete.sFolder_Name);
        if(dfItemFolder == null){
            //Could not find folder holding item to be deleted. Item must have failed to be integrated or deleted via
            //  some other method. Proceed with removing data from catalog.
            bSuccess = globalClass.deleteItemFromCatalogFile(ciToDelete, gsResponseActionFilter);
        } else {

            boolean bSingleFileDeleteAndFileNotFound = false;

            if (!ciToDelete.sSource.startsWith("http")) {
                //If the source is not from the web...
                DocumentFile dfFileToBeDeleted = dfItemFolder.findFile(ciToDelete.sFilename);
                if (dfFileToBeDeleted != null) {
                    if (!dfFileToBeDeleted.delete()) {
                        globalClass.problemNotificationConfig("Could not delete file.", gsResponseActionFilter);
                        bSuccess = false;
                    }
                } else {
                    globalClass.problemNotificationConfig("Could not find file at this location: " + dfItemFolder.getUri() + ".", gsResponseActionFilter);
                    bSuccess = false;
                    bSingleFileDeleteAndFileNotFound = true; //Used to provide easy delete for a single item.
                }
            } else {
                //If the source is from the web...
                //Delete the temporary download folders, etc.
                //Delete any working folders:

                if(ciToDelete.iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {

                    DocumentFile dfVideoWorkingFolder = dfItemFolder.findFile(ciToDelete.sItemID);
                    if (dfVideoWorkingFolder != null) {
                        DocumentFile[] dfVideoWorkingFolderListing = dfVideoWorkingFolder.listFiles();
                        ArrayList<DocumentFile> aldfOutputFolders = new ArrayList<>();
                        if (dfVideoWorkingFolderListing.length > 0) {
                            for (DocumentFile df : dfVideoWorkingFolderListing) {
                                //Locate the output folder
                                if (df.isDirectory()) {
                                    aldfOutputFolders.add(df); //The worker could potentially create multiple output folders if it is re-run.
                                }
                            }
                            //Go through the output folders and delete contents:
                            for (DocumentFile df2 : aldfOutputFolders) {
                                DocumentFile[] df2_Contents = df2.listFiles();
                                if (df2_Contents.length > 0) {
                                    for (DocumentFile df3 : df2_Contents) {
                                        if (!df3.delete()) {
                                            Log.d("File Deletion", "Unable to delete file " + df3.getUri() + ".");
                                        }
                                    }
                                }
                            }
                            //Delete download folder contents:
                            for (DocumentFile df4 : dfVideoWorkingFolderListing) {
                                if (!df4.delete()) {
                                    Log.d("File Deletion", "Unable to delete file or folder " + df4.getUri() + ".");
                                }
                            }
                            //Delete download folder:
                            if (!dfVideoWorkingFolder.delete()) {
                                Log.d("File Deletion", "Unable to delete folder " + dfVideoWorkingFolder.getUri() + ".");
                            }

                        }

                    } //End if VideoWorkingFolderExists

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
                    DocumentFile dfFileToBeDeleted = dfItemFolder.findFile(ciToDelete.sFilename);
                    if (dfFileToBeDeleted != null) {
                        if (!dfFileToBeDeleted.delete()) {
                            globalClass.problemNotificationConfig("Could not delete file.", gsResponseActionFilter);
                            bSuccess = false;
                        }
                    } else {
                        globalClass.problemNotificationConfig("Could not find file at this location: " + dfItemFolder.getUri() + ".", gsResponseActionFilter);
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
                            if(fItemDownloadFolderContents != null) {
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
                    //Note - this is executed in GlobalClass routine ComicCatalog_DeleteComic.
                    //  It may be desireable to move that routine to this location.
                }


                //End if the catalog item was sourced as a download.

            }

            DocumentFile dfLogFolder = globalClass.gdfLogsFolder;
            if (dfLogFolder.exists()) {
                DocumentFile[] dfLogFiles = dfLogFolder.listFiles();

                for (DocumentFile df : dfLogFiles) {
                    if (df.isFile() && df.getName() != null) {
                        if(df.getName().startsWith(ciToDelete.sItemID)) {
                            if (!df.delete()) {
                                globalClass.problemNotificationConfig("Could not delete log file associated with this item: " + df.getName() + ".", gsResponseActionFilter);
                            }
                        }
                    }
                }

            }

            if (bSuccess) {

                //Delete the folder if the folder is now empty:

                DocumentFile[] dfFilesRemaining = dfItemFolder.listFiles();
                if (dfFilesRemaining.length == 0) {
                    if (!dfItemFolder.delete()) {
                        globalClass.problemNotificationConfig("Folder holding this item is empty, but could not delete folder. Folder name: " + dfItemFolder.getUri() + ".", gsResponseActionFilter);
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
