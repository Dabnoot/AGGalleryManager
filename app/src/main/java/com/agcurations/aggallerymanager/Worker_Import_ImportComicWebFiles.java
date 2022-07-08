package com.agcurations.aggallerymanager;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.UUID;

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
                .putInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[globalClass.giSelectedCatalogMediaCategory],
                        GlobalClass.SORT_BY_DATETIME_IMPORTED)
                .putBoolean(GlobalClass.gsCatalogViewerPreferenceNameSortAscending[globalClass.giSelectedCatalogMediaCategory],
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


        File fDestination = new File(
                globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath() + File.separator +
                        ci.sFolder_Name);


        if (!fDestination.exists()) {
            if (!fDestination.mkdir()) {
                //Unable to create directory
                globalClass.BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        gsIntentActionFilter);
                return Result.failure();
            } else {
                globalClass.BroadcastProgress(true, "Destination folder created: " + fDestination.getPath() + "\n",
                        false, iProgressBarValue,
                        false, "",
                        gsIntentActionFilter);
            }
        } else {
            globalClass.BroadcastProgress(true, "Destination folder verified: " + fDestination.getPath() + "\n",
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

        //The below call should add the record to both the catalog contents file
        //  and memory. Create the record in the system before downloading the files for the event that
        //  the download is interrupted:
        globalClass.CatalogDataFile_CreateNewRecord(ci);


        if(alFileList.size() > 0){
            //If there are image addresses to attempt to download...

            //NOTE: Android has DownloadIdleService which is reponsible for cleaning up stale or orphan downloads.
            //  I have witnessed disappearance of downloaded files. This service seems to be deleting comic files.
            //See article at: https://www.vvse.com/blog/blog/2020/01/06/android-10-automatically-deletes-downloaded-files/

            InputStream input = null;
            OutputStream output = null;
            try {

                String sComicDownloadFolder = "";
                ArrayList<Long> allDownloadIDs = new ArrayList<>();
                DownloadManager downloadManager = null;
                if(globalClass.gbUseDownloadManager){
                    downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
                }
                //Download the files:
                int FILE_DOWNLOAD_ADDRESS = 0;
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


                    if(!globalClass.gbUseDownloadManager) {
                        String sNewFullPathFilename = fDestination.getPath() +
                                File.separator + GlobalClass.gsDLTempFolderName + File.separator + //Use DL folder name to allow move to a different folder after download.
                                sJumbledNewFileName;
                        File fNewFile = new File(sNewFullPathFilename);

                        if(!fNewFile.exists()) {

                            // Output stream
                            output = new FileOutputStream(fNewFile.getPath());

                            byte[] data = new byte[1024];

                            globalClass.BroadcastProgress(true, "Downloading: " + icf.sURL + "...",
                                    false, iProgressBarValue,
                                    true, "Downloading files...",
                                    gsIntentActionFilter);

                            URL url = new URL(icf.sURL);
                            URLConnection connection = url.openConnection();
                            connection.connect();

                            // download the file
                            input = new BufferedInputStream(url.openStream(), 8192);

                            int count;
                            while ((count = input.read(data)) != -1) {

                                // writing data to file
                                output.write(data, 0, count);
                            }

                            // flushing output
                            output.flush();

                            // closing streams
                            output.close();
                            input.close();

                        }




                    } else { //If bUseDownloadManager....

                        String sNewFullPathFilename = fDestination.getPath() + File.separator +
                                sJumbledNewFileName;
                        File fNewFile = new File(sNewFullPathFilename);

                        if(!fNewFile.exists()) {

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
                            sComicDownloadFolder = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() +
                                    sDownloadFolderRelativePath;
                            request.setTitle("AG Gallery+ File Download: " + "Comic ID " + ci.sItemID)
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

                    } //End if bUseDownloadManager.

                    lProgressNumerator++;

                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    globalClass.BroadcastProgress(true, "\n",
                            true, iProgressBarValue,
                            false, "",
                            gsIntentActionFilter);


                }
                //Success downloading files.

                //Start a worker to move the downloaded files if using DownloadManager:
                if(globalClass.gbUseDownloadManager){

                    long[] lDownloadIDs = new long[allDownloadIDs.size()];
                    for(int i = 0; i < allDownloadIDs.size(); i++){
                        lDownloadIDs[i] = allDownloadIDs.get(i);
                    }

                    //todo: mimic video downloadID logic. See video download routine.

                    //Build-out data to send to the worker:
                    Data dataComicDownloadPostProcessor = new Data.Builder()
                            .putString(Worker_ComicPostProcessing.KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS, sComicDownloadFolder)
                            .putString(Worker_ComicPostProcessing.KEY_ARG_WORKING_FOLDER, fDestination.getAbsolutePath())
                            .putLongArray(Worker_ComicPostProcessing.KEY_ARG_DOWNLOAD_IDS, lDownloadIDs)
                            .putString(Worker_ComicPostProcessing.KEY_ARG_ITEM_ID, ci.sItemID)
                            .build();
                    OneTimeWorkRequest otwrComicDownloadPostProcessor = new OneTimeWorkRequest.Builder(Worker_ComicPostProcessing.class)
                            .setInputData(dataComicDownloadPostProcessor)
                            .addTag(Worker_ComicPostProcessing.WORKER_COMIC_POST_PROCESSING_TAG) //To allow finding the worker later.
                            .build();
                    UUID UUIDWorkID = otwrComicDownloadPostProcessor.getId();
                    WorkManager.getInstance(getApplicationContext()).enqueue(otwrComicDownloadPostProcessor);



                }





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
            } finally {
                try {
                    if(output != null) {
                        output.close();
                    }
                    if(input != null) {
                        input.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
