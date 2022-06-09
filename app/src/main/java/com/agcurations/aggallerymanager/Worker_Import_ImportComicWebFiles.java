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

    ItemClass_CatalogItem gci;
    String gsIntentActionFilter;

    public Worker_Import_ImportComicWebFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        gci = GlobalClass.Copy_ItemClass_CatalogItem(globalClass.catalogItem_ImportComicWebFiles);
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

        long lProgressNumerator = 0L;
        long lProgressDenominator = gci.iComicPages;
        int iProgressBarValue = 0;

        boolean bUpdateExistingComic = false;
        //Create the comic folder.
        if(!gci.sItemID.equals("")) { //There is a case in which this routine is called to re-download comic files. In that case, don't recreate the item ID.
            return Result.failure(); //Don't execute this anymore - I think it is no longer used.
            /*bUpdateExistingComic = true;
            //If we are updating an existing comic, get the download file list:
            ArrayList<String[]> alsURLs;
            alsURLs = handleAction_startActionGetComicDetailsOnline(ci.sSource, sIntentActionFilter);
            ci.alsDownloadURLsAndDestFileNames = alsURLs;*/
        }

        //Create the comic folder.
        if(!bUpdateExistingComic) {
            gci.sItemID = globalClass.getNewCatalogRecordID(GlobalClass.MEDIA_CATEGORY_COMICS);
            gci.sFolder_Name = gci.sItemID;
        }

        File fDestination = new File(
                globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath() + File.separator +
                        gci.sFolder_Name);


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
        gci.dDatetime_Last_Viewed_by_User = dTimeStamp;
        gci.dDatetime_Import = dTimeStamp;

        if(!bUpdateExistingComic) { //If this is an update, don't update the tags and don't create a new catalog record.
            //Convert textual tags to numeric tags:
            //Form the tag integer array:
            String[] sTags = gci.sTags.split(", ");
            ArrayList<Integer> aliTags = new ArrayList<>();
            for (String sTag : sTags) {
                aliTags.add(globalClass.getTagIDFromText(sTag, GlobalClass.MEDIA_CATEGORY_COMICS));
            }
            //Look for any tags that could not be found:
            ArrayList<String> alsNewTags = new ArrayList<>();
            for(int i = 0; i < aliTags.size(); i++){
                if(aliTags.get(i) == -1){
                    //Prepare a list of strings representing the new tags that must be created:
                    if(!sTags[i].equals("")) {
                        alsNewTags.add(sTags[i]);
                    }
                }
            }
            if(alsNewTags.size() > 0) {
                //Create the missing tags:
                ArrayList<ItemClass_Tag> alictNewTags = globalClass.TagDataFile_CreateNewRecords(alsNewTags, GlobalClass.MEDIA_CATEGORY_COMICS);
                //Insert the new tag IDs into aliTags:
                int k = 0;
                for (int i = 0; i < aliTags.size(); i++) {
                    if (aliTags.get(i) == -1) {
                        if (!sTags[i].equals("")) {
                            if(k < alictNewTags.size()) {
                                aliTags.set(i, alictNewTags.get(k).iTagID);
                                k++;
                            }
                        }
                    }
                }
            }

            gci.sTags = GlobalClass.formDelimitedString(aliTags, ",");

            //The below call should add the record to both the catalog contents file
            //  and memory. Create the record in the system before downloading the files for the event that
            //  the download is interrupted:
            globalClass.CatalogDataFile_CreateNewRecord(gci);
        }

        if(gci.alsDownloadURLsAndDestFileNames.size() > 0){
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
                int FILE_NAME_AND_EXTENSION = 1;
                for(String[] sURLAndFileName: gci.alsDownloadURLsAndDestFileNames) {

                    String sNewFilename = gci.sItemID + "_" + sURLAndFileName[FILE_NAME_AND_EXTENSION];
                    //ci.iPostProcessingCode = ItemClass_CatalogItem.POST_PROCESSING_COMIC_DLM_MOVE; //Tell the app to move the files after download to avoid DLM-automated deletion.
                    //Above item no longer required as DownloadManager will not download to the final location on the SD Card, only to the emulated folder. However,
                    //  leave this item here in the even that testing without an SD Card reveals that we want to use this again.
                    String sJumbledNewFileName = GlobalClass.JumbleFileName(sNewFilename);

                    if(gci.sFilename.equals("")){
                        gci.sFilename = sJumbledNewFileName;
                        gci.sThumbnail_File = sJumbledNewFileName;
                        //Update the catalog record with the filename and thumbnail image:
                        globalClass.CatalogDataFile_UpdateRecord(gci);
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

                            globalClass.BroadcastProgress(true, "Downloading: " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS] + "...",
                                    false, iProgressBarValue,
                                    true, "Downloading files...",
                                    gsIntentActionFilter);

                            URL url = new URL(sURLAndFileName[FILE_DOWNLOAD_ADDRESS]);
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

                            globalClass.BroadcastProgress(true, "Initiating download of file: " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS] + "...",
                                    false, iProgressBarValue,
                                    true, "Submitting download requests...",
                                    gsIntentActionFilter);

                            //Use the download manager to download the file:
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sURLAndFileName[FILE_DOWNLOAD_ADDRESS]));
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
                                    File.separator + gci.sFolder_Name;
                            sComicDownloadFolder = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() +
                                    sDownloadFolderRelativePath;
                            request.setTitle("AG Gallery+ File Download: " + "Comic ID " + gci.sItemID)
                                    .setDescription("Comic ID " + gci.sItemID + "; " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS])
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
                            .putString(Worker_ComicPostProcessing.KEY_ARG_ITEM_ID, gci.sItemID)
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

        if(gci.iSpecialFlag != ItemClass_CatalogItem.FLAG_NO_CODE){
            //Update the catalog file to note the post-processing code:
            globalClass.CatalogDataFile_UpdateRecord(gci);
        }

        //Put processed catalog item in globalclass to allow easier pass-back of catalog item:
        globalClass.gci_ImportComicWebItem = gci;



        globalClass.gbImportExecutionRunning = false;
        globalClass.gbImportExecutionFinished = true;
        return Result.success();
    }

}
