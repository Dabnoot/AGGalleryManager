package com.agcurations.aggallerymanager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Import_GetHoldingFolderDirectoryContents extends Worker {

    public static final String TAG_WORKER_IMPORT_GETHOLDINGFOLDERDIRECTORYCONTENTS = "com.agcurations.aggallermanager.tag_worker_import_getholdingfolderdirectorycontents";

    public Worker_Import_GetHoldingFolderDirectoryContents(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;

        Intent broadcastIntent_GetDirectoryContentsResponse = new Intent();
        ArrayList<ItemClass_File> alFileList = new ArrayList<>();
        try {

            //Get data about the files:
            File[] fImageHoldingFolderFiles = globalClass.gfImageDownloadHoldingFolder.listFiles();
            if(fImageHoldingFolderFiles == null){
                globalClass.gbImportFolderAnalysisRunning = false;
                globalClass.gbImportFolderAnalysisFinished = true;
                String sMessage = "No files found in the holding folder.";
                globalClass.gsbImportFolderAnalysisLog.append(sMessage);
                globalClass.problemNotificationConfig(sMessage, Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
                return Result.success();
            }

            //Calculate total number of files for a progress bar:
            lProgressDenominator = fImageHoldingFolderFiles.length;

            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, "0/" + lProgressDenominator,
                    Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

            MediaMetadataRetriever mediaMetadataRetriever;
            mediaMetadataRetriever = new MediaMetadataRetriever();

            int iIterator = 0;
            while ((iIterator < fImageHoldingFolderFiles.length) && !globalClass.gbImportFolderAnalysisStop) {

                //Update progress bar:
                //Update progress right away in order to avoid instances in which a loop is skipped.
                lProgressNumerator++;
                iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 1000);
                globalClass.BroadcastProgress(false, "",
                        true, iProgressBarValue,
                        true, lProgressNumerator + "/" + lProgressDenominator,
                        Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

                File fImport = fImageHoldingFolderFiles[iIterator];
                iIterator++;

                String docName = fImport.getName();
                String mimeType = getMimeType(docName);
                long lLastModified = fImport.lastModified();//cImport.getLong(3); //milliseconds since January 1, 1970 00:00:00.0 UTC.
                long lFileSize = fImport.length();

                if(mimeType == null){
                    mimeType = "";
                }

                boolean isDirectory;
                isDirectory = (mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR));

                if (isDirectory) {
                    continue; //skip the rest of the for loop for this item.
                }

                String fileExtension;
                //If the file is video, get the duration so that the file list can be sorted by duration if requested.
                long lDurationInMilliseconds = -1L;
                String sWidth = "";  //We are not doing math with the width and height. Therefore no need to convert to int.
                String sHeight = "";

                //Get date last modified:
                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                cal.setTimeInMillis(lLastModified);
                Date dateLastModified = cal.getTime();

                //Record the file extension:
                fileExtension = docName.contains(".") ? docName.substring(docName.lastIndexOf(".")) : "";
                //If the file extension does not match the file extension regex, skip the remainder of the loop.
                if (!fileExtension.matches(".+")) {
                    continue;  //skip the rest of the loop if the file extension does not match.
                }

                //If this is a file, check to see if it is a video or an image and if we are looking for videos or images.
                if (mimeType.startsWith("video") ||
                        fileExtension.equals(".gif") ||
                        fileExtension.equals(".txt") ||
                        (mimeType.equals("application/octet-stream") && fileExtension.equals(".mp4"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid
                    //If video...
                    continue; //If requesting images and mimeType is video or the file a gif, go to next loop.
                }


                //Get the width and height of the image:
                try {
                    //InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(docUri);
                    FileInputStream inputStream = new FileInputStream(fImport);
                    BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                    onlyBoundsOptions.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(inputStream, null, onlyBoundsOptions);
                    inputStream.close();
                    sWidth = "" + onlyBoundsOptions.outWidth;
                    sHeight = "" + onlyBoundsOptions.outHeight;

                } catch (Exception e) {
                    continue; //Skip the rest of this loop.
                }

                //create the file model and initialize:
                ItemClass_File icfFileItem = new ItemClass_File(ItemClass_File.TYPE_IMAGE_FROM_HOLDING_FOLDER, docName);
                icfFileItem.sExtension = fileExtension;
                icfFileItem.lSizeBytes = lFileSize;
                icfFileItem.dateLastModified = dateLastModified;
                icfFileItem.sWidth = sWidth;
                icfFileItem.sHeight = sHeight;
                icfFileItem.sUri = Uri.fromFile(fImport).toString();// fImport.getAbsolutePath();
                icfFileItem.sMimeType = mimeType;
                //Get the URL data from the associated metadata file, if it exists:
                String sImageMetadataFilePath = globalClass.gfImageDownloadHoldingFolder.getPath() +
                        File.separator + docName + ".txt"; //The file will have two extensions.
                File fImageMetadataFile = new File(sImageMetadataFilePath);
                if(fImageMetadataFile.exists()) {
                    try {
                        BufferedReader brReader;
                        brReader = new BufferedReader(new FileReader(fImageMetadataFile.getAbsolutePath()));
                        icfFileItem.sURL = brReader.readLine();
                        brReader.close();
                    } catch (Exception e) {
                        String sMessage = "" + e.getMessage();
                        Log.d("VideoEnabledWebView", sMessage);
                    }
                }
                //Add the ItemClass_File to the ArrayList:
                alFileList.add(icfFileItem);

            } //End loop going through the folder that the user selected.


            mediaMetadataRetriever.release();


        }catch (Exception e){
            globalClass.gbImportFolderAnalysisRunning = false;
            globalClass.gbImportFolderAnalysisFinished = true;
            String sMessage = "Problem during handleAction_GetHoldingFolderDirectoryContents: " + e.getMessage();
            globalClass.gsbImportFolderAnalysisLog.append(sMessage);
            globalClass.problemNotificationConfig(e.getMessage(), Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
            return Result.failure();
        }

        broadcastIntent_GetDirectoryContentsResponse.putExtra(GlobalClass.EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, true);
        broadcastIntent_GetDirectoryContentsResponse.putExtra(GlobalClass.EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE, alFileList);

        //Send broadcast to the Import Activity:
        broadcastIntent_GetDirectoryContentsResponse.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent_GetDirectoryContentsResponse.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);

        //Set finished and broadcast so that the fragment knows that we are done.
        globalClass.gbImportFolderAnalysisFinished = true;
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, lProgressNumerator + "/" + lProgressDenominator,
                Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

        globalClass.gbImportFolderAnalysisRunning = false;
        if(globalClass.gbImportFolderAnalysisStop) {
            globalClass.gbImportFolderAnalysisStop = false;
        }






        return Result.success();
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }


}
