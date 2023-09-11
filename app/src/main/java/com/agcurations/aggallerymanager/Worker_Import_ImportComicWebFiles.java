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

    public static final String IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE";

    public Worker_Import_ImportComicWebFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

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

        String sMessage;

        ArrayList<ItemClass_File> alFileList = globalClass.galImportFileList;

        long lProgressNumerator = 0L;
        long lProgressDenominator = alFileList.get(0).iComicPages;
        int iProgressBarValue = 0;

        ItemClass_CatalogItem ciNew = new ItemClass_CatalogItem();

        //Create the comic folder.
        ciNew.sItemID = GlobalClass.getNewCatalogRecordID();
        ciNew.sFolderRelativePath = alFileList.get(0).sDestinationFolder + GlobalClass.gsFileSeparator + ciNew.sItemID;
        ciNew.iMediaCategory = ItemClass_CatalogItem.MEDIA_CATEGORY_COMICS;


        Uri uriDestinationFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].toString(), ciNew.sFolderRelativePath);
        String sUserFriendlyDestinationFolder = GlobalClass.cleanHTMLCodedCharacters(uriDestinationFolder.toString());

        if (!GlobalClass.CheckIfFileExists(uriDestinationFolder)) {

            try {
                uriDestinationFolder = GlobalClass.CreateDirectory(uriDestinationFolder);
            } catch (Exception e){
                sMessage = "Could not locate parent directory of destination folder in order to create destination folder. Destination folder: " + sUserFriendlyDestinationFolder;
                LogThis("doWork()", sMessage, e.getMessage());
                uriDestinationFolder = null;
            }

            if (uriDestinationFolder == null) {
                //Unable to create directory
                sMessage = "Unable to create destination folder " +  ciNew.sFolderRelativePath + " at: "
                        + GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS] + "\n";
                globalClass.BroadcastProgress(true, sMessage,
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;
                return Result.failure();
            } else {
                globalClass.BroadcastProgress(true, "Destination folder created: " + sUserFriendlyDestinationFolder + "\n",
                        false, iProgressBarValue,
                        false, "",
                        IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
            }
        } else {
            globalClass.BroadcastProgress(true, "Destination folder verified: " + sUserFriendlyDestinationFolder + "\n",
                    true, iProgressBarValue,
                    false, "",
                    IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
        }

        //Create a timestamp to be used to create the data record:
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        ciNew.dDatetime_Last_Viewed_by_User = dTimeStamp;
        ciNew.dDatetime_Import = dTimeStamp;

        //Populate Catalog Item data fields:
        ciNew.sSource            = alFileList.get(0).sURL;
        ciNew.sTitle             = alFileList.get(0).sTitle;
        ciNew.lSize              = alFileList.get(0).lSizeBytes;
        ciNew.sComicParodies     = alFileList.get(0).sComicParodies;
        ciNew.sComicCharacters   = alFileList.get(0).sComicCharacters;
        ciNew.sComicArtists      = alFileList.get(0).sComicArtists;
        ciNew.sComicGroups       = alFileList.get(0).sComicGroups;
        ciNew.sComicLanguages    = alFileList.get(0).sComicLanguages;
        ciNew.sComicCategories   = alFileList.get(0).sComicCategories;
        ciNew.iComicPages        = alFileList.get(0).iComicPages;
        ciNew.iComic_Max_Page_ID = alFileList.get(0).iComicPages;
        ciNew.sTags = GlobalClass.formDelimitedString(alFileList.get(0).aliProspectiveTags, ",");
        ciNew.aliTags = new ArrayList<>(alFileList.get(0).aliProspectiveTags);
        ciNew.iMaturityRating = GlobalClass.getHighestTagMaturityRating(ciNew.aliTags, GlobalClass.MEDIA_CATEGORY_COMICS);
        //ciNew.alsApprovedUsers.add(globalClass.gicuCurrentUser.sUserName);
        ciNew.alsApprovedUsers = GlobalClass.getApprovedUsersForTagGrouping(ciNew.aliTags, ciNew.iMediaCategory);
        //Inform program of a need to update the tags histogram:
        globalClass.gbTagHistogramRequiresUpdate[ciNew.iMediaCategory] = true;
        ciNew.sGroupID = alFileList.get(0).sGroupID; //todo: This line added during rough-draft implementation of catalog item grouping. Comic grouping not really thought-out, but putting it here might make things magically work later as a bonus.

        //The below call should add the record to both the catalog contents file
        //  and memory. Create the record in the system before downloading the files for the event that
        //  the download is interrupted:
        try {
            globalClass.CatalogDataFile_CreateNewRecord(ciNew);
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
                            IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
                    globalClass.gbImportExecutionRunning = false;
                    globalClass.gbImportExecutionFinished = true;
                    return Result.failure();
                }


                //Download the files:
                String sDownloadManagerDownloadFolder = "";
                for(ItemClass_File icf: alFileList) {

                    String sNewFilename = icf.sFileOrFolderName;
                    //ciNew.iPostProcessingCode = ItemClass_CatalogItem.POST_PROCESSING_COMIC_DLM_MOVE; //Tell the app to move the files after download to avoid DLM-automated deletion.
                    //Above item no longer required as DownloadManager will not download to the final location on the SD Card, only to the emulated folder. However,
                    //  leave this item here in the even that testing without an SD Card reveals that we want to use this again.
                    String sJumbledNewFileName = GlobalClass.JumbleFileName(sNewFilename);

                    if(ciNew.sFilename.equals("")){ //On first loop, configure the coverpage by setting the catalog item file name:
                        ciNew.sFilename = sJumbledNewFileName;
                        ciNew.sThumbnail_File = sJumbledNewFileName;
                        //Update the catalog record with the filename and thumbnail image:
                        globalClass.CatalogDataFile_UpdateRecord(ciNew);
                    }



                    Uri uriNewFile = GlobalClass.FormChildUri(uriDestinationFolder.toString(), sJumbledNewFileName);

                    if(uriNewFile != null) {

                        globalClass.BroadcastProgress(true, "Initiating download of file: " + icf.sURL + "...",
                                false, iProgressBarValue,
                                true, "Submitting download requests...",
                                IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);

                        //Use the download manager to download the file:
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(icf.sURL));
                        /*request.setTitle("AG Gallery+ File Download: " + "Comic ID " + ciNew.sItemID)
                                .setDescription("Comic ID " + ciNew.sItemID + "; " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS])
                                //.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                                //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                                .setMimeType("application/octet-stream")
                                .setDestinationUri(Uri.fromFile(fNewFile));*/
                        //The above method no longer works as of Android 11 API level 30, One UI version 3.1.

                        String sDownloadFolderRelativePath;
                        sDownloadFolderRelativePath = File.separator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_COMICS] +
                                File.separator + ciNew.sFolderRelativePath;
                        File fExternalFilesDir = getApplicationContext().getExternalFilesDir(null);
                        if(fExternalFilesDir != null) {
                            sDownloadManagerDownloadFolder = fExternalFilesDir.getAbsolutePath() +
                                sDownloadFolderRelativePath;
                        } else {
                            globalClass.BroadcastProgress(true, "Could not identify external files dir.",
                                    false, iProgressBarValue,
                                    true, "Halted.",
                                    IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
                            globalClass.gbImportExecutionRunning = false;
                            globalClass.gbImportExecutionFinished = true;
                            return Result.failure();
                        }
                        request.setTitle("AGGallery+ Download " + (lProgressNumerator + 1) + " of " + lProgressDenominator + " ComicID " + ciNew.sItemID)
                                .setDescription("Comic ID " + ciNew.sItemID + "; " + icf.sURL)
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
                            IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);


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
                        .putString(Worker_DownloadPostProcessing.KEY_ARG_RELATIVE_PATH_TO_FOLDER, ciNew.sFolderRelativePath)
                        .putInt(Worker_DownloadPostProcessing.KEY_ARG_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_COMICS)
                        .putLongArray(Worker_DownloadPostProcessing.KEY_ARG_DOWNLOAD_IDS, lDownloadIDs)
                        .putString(Worker_DownloadPostProcessing.KEY_ARG_ITEM_ID, ciNew.sItemID)
                        .build();
                OneTimeWorkRequest otwrDownloadPostProcessor = new OneTimeWorkRequest.Builder(Worker_DownloadPostProcessing.class)
                        .setInputData(dataDownloadPostProcessor)
                        .addTag(Worker_DownloadPostProcessing.WORKER_TAG_DOWNLOAD_POST_PROCESSING) //To allow finding the worker later.
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueue(otwrDownloadPostProcessor);









                globalClass.BroadcastProgress(true, "Operation complete.\nSome files may continue to download in the background. Comic will be available when downloads complete.",
                        true, iProgressBarValue,
                        false, "",
                        IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);

            } catch (Exception e) {
                if(e.getMessage() != null) {
                    Log.e("Error: ", e.getMessage());
                }
                globalClass.BroadcastProgress(true, "Problem encountered:\n" + e.getMessage(),
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
            }

        }

        if(ciNew.iSpecialFlag != ItemClass_CatalogItem.FLAG_NO_CODE){
            //Update the catalog file to note the post-processing code:
            globalClass.CatalogDataFile_UpdateRecord(ciNew);
        }

        /*//Put processed catalog item in globalclass to allow easier pass-back of catalog item:
        globalClass.gci_ImportComicWebItem = ciNew;*/



        globalClass.gbImportExecutionRunning = false;
        globalClass.gbImportExecutionFinished = true;
        return Result.success();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Import_ImportComicWebFiles:" + sRoutine, sMessage);
    }

}
