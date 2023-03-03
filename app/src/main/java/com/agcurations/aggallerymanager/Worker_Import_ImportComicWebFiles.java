package com.agcurations.aggallerymanager;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Import_ImportComicWebFiles extends Worker {

    public static final String TAG_WORKER_IMPORT_IMPORTCOMICWEBFILES = "com.agcurations.aggallermanager.tag_worker_import_importcomicwebfiles";

    String gsIntentActionFilter;

    public Worker_Import_ImportComicWebFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsIntentActionFilter = getInputData().getString(GlobalClass.EXTRA_STRING_INTENT_ACTION_FILTER);

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

        //sIntentActionFilter is used to send out the broadcast responses.

        ArrayList<ItemClass_File> alFileList = globalClass.galImportFileList;

        long lProgressNumerator = 0L;
        long lProgressDenominator = alFileList.get(0).iComicPages;
        int iProgressBarValue = 0;

        ItemClass_CatalogItem ci = new ItemClass_CatalogItem();

        //Create the comic folder.
        ci.sItemID = globalClass.getNewCatalogRecordID(GlobalClass.MEDIA_CATEGORY_COMICS);
        ci.sFolder_Name = ci.sItemID;
        ci.iMediaCategory = ItemClass_CatalogItem.MEDIA_CATEGORY_COMICS;


        Uri uriDestinationFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].toString(), ci.sFolder_Name);


        if (!GlobalClass.CheckIfFileExists(uriDestinationFolder)) {
            uriDestinationFolder = GlobalClass.CreateDirectory(uriDestinationFolder);

            if (uriDestinationFolder == null) {
                //Unable to create directory
                String sMessage = "Unable to create destination folder " +  ci.sFolder_Name + " at: "
                        + GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS] + "\n";
                globalClass.BroadcastProgress(true, sMessage,
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        gsIntentActionFilter);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;
                return Result.failure();
            } else {
                globalClass.BroadcastProgress(true, "Destination folder created: " + uriDestinationFolder + "\n",
                        false, iProgressBarValue,
                        false, "",
                        gsIntentActionFilter);
            }
        } else {
            globalClass.BroadcastProgress(true, "Destination folder verified: " + uriDestinationFolder + "\n",
                    true, iProgressBarValue,
                    false, "",
                    gsIntentActionFilter);
        }

        //Create a timestamp to be used to create the data record:
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        ci.dDatetime_Last_Viewed_by_User = dTimeStamp;
        ci.dDatetime_Import = dTimeStamp;

        //Populate Catalog Item data fields:
        ci.sSource            = alFileList.get(0).sURL;
        ci.sTitle             = alFileList.get(0).sTitle;
        ci.lSize              = alFileList.get(0).lSizeBytes;
        ci.sComicParodies     = alFileList.get(0).sComicParodies;
        ci.sComicCharacters   = alFileList.get(0).sComicCharacters;
        ci.sComicArtists      = alFileList.get(0).sComicArtists;
        ci.sComicGroups       = alFileList.get(0).sComicGroups;
        ci.sComicLanguages    = alFileList.get(0).sComicLanguages;
        ci.sComicCategories   = alFileList.get(0).sComicCategories;
        ci.iComicPages        = alFileList.get(0).iComicPages;
        ci.iComic_Max_Page_ID = alFileList.get(0).iComicPages;
        ci.sTags = GlobalClass.formDelimitedString(alFileList.get(0).aliProspectiveTags, ",");
        ci.aliTags = new ArrayList<>(alFileList.get(0).aliProspectiveTags);
        //Inform program of a need to update the tags histogram:
        globalClass.gbTagHistogramRequiresUpdate[ci.iMediaCategory] = true;

        //The below call should add the record to both the catalog contents file
        //  and memory. Create the record in the system before downloading the files for the event that
        //  the download is interrupted:
        try {
            globalClass.CatalogDataFile_CreateNewRecord(ci);
        } catch (Exception e) {
            e.printStackTrace();
            globalClass.gbImportExecutionRunning = false;
            globalClass.gbImportExecutionFinished = true;
            return Result.failure();
        }


        if(alFileList.size() > 0){
            //If there are image addresses to attempt to download...

            //NOTE: Android has DownloadIdleService which is reponsible for cleaning up stale or orphan downloads.
            //  I have witnessed disappearance of downloaded files. This service seems to be deleting comic files.
            //See article at: https://www.vvse.com/blog/blog/2020/01/06/android-10-automatically-deletes-downloaded-files/

            try {

                ArrayList<Long> allDownloadIDs = new ArrayList<>();
                DownloadManager downloadManager;

                downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
                if(downloadManager == null){
                    globalClass.BroadcastProgress(true, "Could not get download manager.",
                            false, iProgressBarValue,
                            true, "Halted.",
                            gsIntentActionFilter);
                    globalClass.gbImportExecutionRunning = false;
                    globalClass.gbImportExecutionFinished = true;
                    return Result.failure();
                }


                //Download the files:
                String sDownloadManagerDownloadFolder = "";
                for(ItemClass_File icf: alFileList) {

                    String sNewFilename = ci.sItemID + "_" + icf.sFileOrFolderName;
                    //ci.iPostProcessingCode = ItemClass_CatalogItem.POST_PROCESSING_COMIC_DLM_MOVE; //Tell the app to move the files after download to avoid DLM-automated deletion.
                    //Above item no longer required as DownloadManager will not download to the final location on the SD Card, only to the emulated folder. However,
                    //  leave this item here in the even that testing without an SD Card reveals that we want to use this again.
                    String sJumbledNewFileName = GlobalClass.JumbleFileName(sNewFilename);

                    if(ci.sFilename.equals("")){ //On first loop, configure the coverpage by setting the catalog item file name:
                        ci.sFilename = sJumbledNewFileName;
                        ci.sThumbnail_File = sJumbledNewFileName;
                        //Update the catalog record with the filename and thumbnail image:
                        globalClass.CatalogDataFile_UpdateRecord(ci);
                    }



                    Uri uriNewFile = GlobalClass.FormChildUri(uriDestinationFolder.toString(), sJumbledNewFileName);

                    if(uriNewFile == null) {

                        globalClass.BroadcastProgress(true, "Initiating download of file: " + icf.sURL + "...",
                                false, iProgressBarValue,
                                true, "Submitting download requests...",
                                gsIntentActionFilter);

                        //Use the download manager to download the file:
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(icf.sURL));
                        /*request.setTitle("AG Gallery+ File Download: " + "Comic ID " + ci.sItemID)
                                .setDescription("Comic ID " + ci.sItemID + "; " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS])
                                //.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                                //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                                .setMimeType("application/octet-stream")
                                .setDestinationUri(Uri.fromFile(fNewFile));*/
                        //The above method no longer works as of Android 11 API level 30, One UI version 3.1.

                        String sDownloadFolderRelativePath;
                        sDownloadFolderRelativePath = File.separator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_COMICS] +
                                File.separator + ci.sFolder_Name;
                        File fExternalFilesDir = getApplicationContext().getExternalFilesDir(null);
                        if(fExternalFilesDir != null) {
                            sDownloadManagerDownloadFolder = fExternalFilesDir.getAbsolutePath() +
                                sDownloadFolderRelativePath;
                        } else {
                            globalClass.BroadcastProgress(true, "Could not identify external files dir.",
                                    false, iProgressBarValue,
                                    true, "Halted.",
                                    gsIntentActionFilter);
                            globalClass.gbImportExecutionRunning = false;
                            globalClass.gbImportExecutionFinished = true;
                            return Result.failure();
                        }
                        request.setTitle("AGGallery+ Download " + (lProgressNumerator + 1) + " of " + lProgressDenominator + " ComicID " + ci.sItemID)
                                .setDescription("Comic ID " + ci.sItemID + "; " + icf.sURL)
                                //.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                                //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                                .setMimeType("application/octet-stream")
                                .setDestinationInExternalFilesDir(getApplicationContext(), sDownloadFolderRelativePath, sJumbledNewFileName);

                        long lDownloadID = downloadManager.enqueue(request);
                        allDownloadIDs.add(lDownloadID); //Record the download ID so that we can check to see if it is completed via the worker.

                    }



                    lProgressNumerator++;

                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    globalClass.BroadcastProgress(true, "\n",
                            true, iProgressBarValue,
                            false, "",
                            gsIntentActionFilter);


                }
                //Success downloading files.

                //Start a worker to move the downloaded files:


                long[] lDownloadIDs = new long[allDownloadIDs.size()];
                for(int i = 0; i < allDownloadIDs.size(); i++){
                    lDownloadIDs[i] = allDownloadIDs.get(i);
                }

                //Build-out data to send to the worker:
                String sCallerID = "Worker_Import_ImportComicWebFiles:doWork()";
                Data dataDownloadPostProcessor = new Data.Builder()
                        .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                        .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                        .putString(Worker_DownloadPostProcessing.KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS, sDownloadManagerDownloadFolder)
                        .putString(Worker_DownloadPostProcessing.KEY_ARG_WORKING_FOLDER_NAME, ci.sFolder_Name)
                        .putInt(Worker_DownloadPostProcessing.KEY_ARG_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_COMICS)
                        .putLongArray(Worker_DownloadPostProcessing.KEY_ARG_DOWNLOAD_IDS, lDownloadIDs)
                        .putString(Worker_DownloadPostProcessing.KEY_ARG_ITEM_ID, ci.sItemID)
                        .build();
                OneTimeWorkRequest otwrDownloadPostProcessor = new OneTimeWorkRequest.Builder(Worker_DownloadPostProcessing.class)
                        .setInputData(dataDownloadPostProcessor)
                        .addTag(Worker_DownloadPostProcessing.WORKER_TAG_DOWNLOAD_POST_PROCESSING) //To allow finding the worker later.
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueue(otwrDownloadPostProcessor);









                globalClass.BroadcastProgress(true, "Operation complete.\nSome files may continue to download in the background. Comic will be available when downloads complete.",
                        true, iProgressBarValue,
                        false, "",
                        gsIntentActionFilter);

            } catch (Exception e) {
                if(e.getMessage() != null) {
                    Log.e("Error: ", e.getMessage());
                }
                globalClass.BroadcastProgress(true, "Problem encountered:\n" + e.getMessage(),
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        gsIntentActionFilter);
            }

        }

        if(ci.iSpecialFlag != ItemClass_CatalogItem.FLAG_NO_CODE){
            //Update the catalog file to note the post-processing code:
            globalClass.CatalogDataFile_UpdateRecord(ci);
        }

        /*//Put processed catalog item in globalclass to allow easier pass-back of catalog item:
        globalClass.gci_ImportComicWebItem = ci;*/



        globalClass.gbImportExecutionRunning = false;
        globalClass.gbImportExecutionFinished = true;
        return Result.success();
    }

}
