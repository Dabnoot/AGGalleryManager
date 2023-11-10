package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
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

        String sMessage;

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;

        Intent broadcastIntent_GetDirectoryContentsResponse = new Intent();
        ArrayList<ItemClass_File> alFileList = new ArrayList<>();
        try {

            //Get data about the files:
            //TreeMap<String, String[]> tmHoldingFolderRecordData = new TreeMap<>();
            int MEDIA_FILE_NAME_INDEX = 0;
            int MEDIA_FILE_EXTENSION_INDEX = 1;
            int MEDIA_FILE_MIME_TYPE_INDEX = 2;
            int MEDIA_FILE_LAST_MODIFIED_INDEX = 3;
            int MEDIA_FILE_SIZE_INDEX = 4;
            int iFieldCount = 5;

            TreeMap<String, String[]> tmHoldingFolderRecordData_AllFiles = new TreeMap<>();

            final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(GlobalClass.gUriImageDownloadHoldingFolder,
                    DocumentsContract.getDocumentId(GlobalClass.gUriImageDownloadHoldingFolder));
            Cursor c;
            try {
                c = GlobalClass.gcrContentResolver.query(childrenUri, new String[] {
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        DocumentsContract.Document.COLUMN_SIZE}, null, null, null);
                if(c != null) {
                    while (c.moveToNext()) {
                        String sMimeType = c.getString( 1);
                        if(!sMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                            //Get data regarding the file:
                            String[] sDataRecord = new String[iFieldCount];
                            String sFileName = c.getString(0);
                            String sFileLastModified = c.getString(2);
                            String sFileSize = c.getString(3);
                            sDataRecord[MEDIA_FILE_NAME_INDEX] = sFileName;
                            sDataRecord[MEDIA_FILE_EXTENSION_INDEX] = "";
                            sDataRecord[MEDIA_FILE_MIME_TYPE_INDEX] = sMimeType;
                            sDataRecord[MEDIA_FILE_LAST_MODIFIED_INDEX] = sFileLastModified;
                            sDataRecord[MEDIA_FILE_SIZE_INDEX] = sFileSize;
                            tmHoldingFolderRecordData_AllFiles.put(sFileName, sDataRecord);
                        }
                    }
                    c.close();
                }
            } catch (Exception e) {
                LogThis("doWork()", "Problem querying folder and processing file listings.", e.getMessage());
            }



            //1. List all media files with associated .dat files:
            ArrayList<String> alsMediaFilesWithDatFiles = new ArrayList<>();
            for(Map.Entry<String, String[]> entry: tmHoldingFolderRecordData_AllFiles.entrySet()){
                String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(entry.getKey());
                if(sBaseAndExtension.length == 2) {
                    if (sBaseAndExtension[1].equals("tad")) { //.dat file obfuscated. .dat file would be something like 1egamI.gpj.tad.
                        if(tmHoldingFolderRecordData_AllFiles.containsKey(sBaseAndExtension[0])){ //sBaseAndExtension[0] will be the media file name AND its extension... such as 1egamI.gpj.
                            //If the media file exists, add it to the list of media files with matching .dat files:
                            alsMediaFilesWithDatFiles.add(sBaseAndExtension[0]);
                        }/* else {
                        //If this is a .dat file without a media file... do nothing for now. Todo: Consider deleting the .dat file?
                        //  This case would be if the download failed for some reason, or perhaps the media file was moved
                        //  or was imported and the .dat file deletion failed.
                        //  Suggest starting the "delete files" worker if orphaned .dat files are found.
                    }*/
                    }
                }
            }
            //2. Look to see if there are any files without metadata files.
            ArrayList<String> alsNoMetadataFileMediaFiles = new ArrayList<>();
            for(Map.Entry<String, String[]> entry: tmHoldingFolderRecordData_AllFiles.entrySet()){
                String sFileName = entry.getKey();
                String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sFileName);
                if(sBaseAndExtension.length == 2) {
                    if (!sBaseAndExtension[1].equals("tad")) {
                        //If this is not a .dat file then it is a media file. Check if this media file had a corresponding .dat file.
                        if(!alsMediaFilesWithDatFiles.contains(sFileName)){
                            //If there was no associated .dat file found for this media file in the previous operation, record this file.
                            alsNoMetadataFileMediaFiles.add(sFileName);
                        }
                    }
                }

            }

            //3. Check metadata files for user. If the user is specified and matches the current user,
            //     include this entry in the file count for potential imports.
            TreeMap<String, String> tmFilenamesAndUsers = new TreeMap<>();
            for(String sMediaFileName: alsMediaFilesWithDatFiles) {
                String sMetaDataFileName = sMediaFileName + ".tad";
                String sMetadataFileUri = GlobalClass.FormChildUriString(GlobalClass.gUriImageDownloadHoldingFolder.toString(), sMetaDataFileName);
                try {
                    //Read a metadata file.
                    Uri uriMetadataFile = Uri.parse(sMetadataFileUri);
                    InputStream isImageMetadataFile = GlobalClass.gcrContentResolver.openInputStream(uriMetadataFile);
                    if (isImageMetadataFile == null) {
                        sMessage = "Could not open metadata file for analysis: " + sMetadataFileUri;
                        Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                        continue;
                    }
                    BufferedReader brReader;
                    brReader = new BufferedReader(new InputStreamReader(isImageMetadataFile));
                    int iLine = 0;
                    for (String sLine = brReader.readLine(); sLine != null && iLine < 3; sLine = brReader.readLine()) {
                        switch (iLine) {
                            case 0:
                            case 1:
                                //Do nothing
                                break;
                            case 2:
                                tmFilenamesAndUsers.put(sMediaFileName, sLine);
                                break;
                        }
                        iLine++;
                    }

                    brReader.close();
                    isImageMetadataFile.close();

                    if(!tmFilenamesAndUsers.containsKey(sMediaFileName)){
                        //If a username was not found for this entry, add the filename but exclude a user.
                        tmFilenamesAndUsers.put(sMediaFileName, "");
                    }

                } catch (Exception e) {
                    sMessage = "Could not open metadata file for analysis: " + sMetadataFileUri + "\n" + e.getMessage();
                    Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                }
            }
            //tmFilenamesAndUsers now contains a list of all media filenames and the user as derived from an associated metadata file.


            //4. Combine file items that had metadata indicating that they are intended for the current
            //     user with any file items that were missing metadata files.
            ArrayList<String> alsApprovedFiles = new ArrayList<>();
            for(Map.Entry<String, String> entry: tmFilenamesAndUsers.entrySet()){
                if(entry.getValue().equals(GlobalClass.gicuCurrentUser.sUserName) ||
                        entry.getValue().equals("")){
                    alsApprovedFiles.add(entry.getKey());
                }
            }
            alsApprovedFiles.addAll(alsNoMetadataFileMediaFiles);

            //5. Refine the treemap containing files so that it only includes the approved files.
            TreeMap<String, String[]> tmHoldingFolderRecordData_ApprovedFiles = new TreeMap<>();
            for(String sApprovedFile: alsApprovedFiles){
                tmHoldingFolderRecordData_ApprovedFiles.put(sApprovedFile, tmHoldingFolderRecordData_AllFiles.get(sApprovedFile));
            }

            //All media files in holding folder now located that are approved for the current user
            //  or have no defined user or have no associated metadata file.





            if(tmHoldingFolderRecordData_ApprovedFiles.size() == 0){
                GlobalClass.gbImportFolderAnalysisRunning = false;
                GlobalClass.gbImportFolderAnalysisFinished = true;
                sMessage = "No files found in the holding folder.";
                globalClass.gsbImportFolderAnalysisLog.append(sMessage);
                globalClass.problemNotificationConfig(sMessage, Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
                return Result.success();
            }

            //Calculate total number of files for a progress bar:
            lProgressDenominator = tmHoldingFolderRecordData_ApprovedFiles.size();

            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, "0/" + lProgressDenominator,
                    Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);


            //Process the holding folder entries:
            for(Map.Entry<String, String[]> HoldingFolderEntry: tmHoldingFolderRecordData_ApprovedFiles.entrySet()){

                if(GlobalClass.gbImportFolderAnalysisStop){
                    break;
                }

                //Check to see if it is a video or an image and if we are looking for videos or images.
                String sMimeType = HoldingFolderEntry.getValue()[MEDIA_FILE_MIME_TYPE_INDEX];
                String sMediaFileExtension = HoldingFolderEntry.getValue()[MEDIA_FILE_EXTENSION_INDEX];
                String sMediaFileName = HoldingFolderEntry.getValue()[MEDIA_FILE_NAME_INDEX];

                if (sMimeType.startsWith("video") ||
                        (sMimeType.equals("application/octet-stream") && sMediaFileExtension.equals(".4pm"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid
                    //If not a file that we want to analyze...
                    continue; //If requesting images and mimeType is video or the file is text, go to next loop.
                    //Choosing to classify gifs as images as they are generally very limited in size.
                    //Leave it up to the user to decide whether to import gifs from the holding folder to the videos catalog.
                }

                long lMediaFileLastModified = Long.parseLong(HoldingFolderEntry.getValue()[MEDIA_FILE_LAST_MODIFIED_INDEX]);// //milliseconds since January 1, 1970 00:00:00.0 UTC.
                long lMediaFileSize = Long.parseLong(HoldingFolderEntry.getValue()[MEDIA_FILE_SIZE_INDEX]);

                //Update progress bar:
                //Update progress right away in order to avoid instances in which a loop is skipped.
                lProgressNumerator++;
                iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 1000);
                globalClass.BroadcastProgress(false, "",
                        true, iProgressBarValue,
                        true, lProgressNumerator + "/" + lProgressDenominator,
                        Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

                String sWidth;  //We are not doing math with the width and height. Therefore no need to convert to int.
                String sHeight;

                //Get date last modified:
                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                cal.setTimeInMillis(lMediaFileLastModified);
                Date dateLastModified = cal.getTime();

                String sMediaFileUri = GlobalClass.FormChildUriString(GlobalClass.gUriImageDownloadHoldingFolder.toString(), HoldingFolderEntry.getValue()[MEDIA_FILE_NAME_INDEX]);

                //Get the width and height of the image:
                try {
                    BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                    onlyBoundsOptions.inJustDecodeBounds = true;

                    Uri uriMediaFile = Uri.parse(sMediaFileUri);
                    InputStream isImageFile = GlobalClass.gcrContentResolver.openInputStream(uriMediaFile);
                    if(isImageFile == null){
                        sMessage = "Could not open image file for analysis: " + sMediaFileName;
                        LogThis("doWork", sMessage, null);
                        continue;
                    }
                    BitmapFactory.decodeStream(isImageFile, null, onlyBoundsOptions);
                    sHeight = "" + onlyBoundsOptions.outHeight;
                    sWidth = "" + onlyBoundsOptions.outWidth;
                    isImageFile.close();

                } catch (Exception e) {
                    continue; //Skip the rest of this loop.
                }

                //create the file model and initialize:
                ItemClass_File icfFileItem = new ItemClass_File(ItemClass_File.TYPE_IMAGE_FROM_HOLDING_FOLDER, sMediaFileName);
                icfFileItem.sExtension = sMediaFileExtension;
                icfFileItem.lSizeBytes = lMediaFileSize;
                icfFileItem.dateLastModified = dateLastModified;
                icfFileItem.sWidth = sWidth;
                icfFileItem.sHeight = sHeight;
                icfFileItem.sUri = sMediaFileUri;
                icfFileItem.sUriParent = GlobalClass.gUriImageDownloadHoldingFolder.toString();
                icfFileItem.sMimeType = sMimeType;
                //Get the URL data from the associated metadata file, if it exists:

                String sMetaDataFileUri = GlobalClass.FormChildUriString(GlobalClass.gUriImageDownloadHoldingFolder.toString(), HoldingFolderEntry.getValue()[MEDIA_FILE_NAME_INDEX] + ".tad");
                Uri uriMetaDataFileUri = Uri.parse(sMetaDataFileUri);
                if(GlobalClass.CheckIfFileExists(uriMetaDataFileUri)) {
                    try {
                        //Read a metadata file.
                        Uri uriMetaDataFile = Uri.parse(sMetaDataFileUri);
                        InputStream isImageMetadataFile = GlobalClass.gcrContentResolver.openInputStream(uriMetaDataFile);
                        if(isImageMetadataFile == null){
                            sMessage = "Could not open metadata file for analysis: " + sMetaDataFileUri;
                            LogThis("doWork()", sMessage, null);
                            continue;
                        }
                        BufferedReader brReader;
                        brReader = new BufferedReader(new InputStreamReader(isImageMetadataFile));
                        int iLine = 0;
                        for (String sLine = brReader.readLine(); sLine != null && iLine < 3; sLine = brReader.readLine()) {
                            switch (iLine) {
                                case 0:
                                    icfFileItem.sURL = sLine;
                                    break;
                                case 1:
                                    icfFileItem.sTitle = sLine;  //This is the original file name. Coordinate wtih VideoEnabledWebView.java.
                                    break;
                                case 2:
                                    icfFileItem.sUserName = sLine;
                                    break;
                            }
                            iLine++;
                        }

                        brReader.close();
                        isImageMetadataFile.close();
                    } catch (Exception e) {
                        sMessage = "Could not open metadata file for analysis: " + sMetaDataFileUri;
                        LogThis("doWork()", sMessage, e.getMessage());
                    }
                }

                //Add the ItemClass_File to the ArrayList:
                if(icfFileItem.sUserName.equals(GlobalClass.gicuCurrentUser.sUserName) || icfFileItem.sUserName.equals("")) {
                    //If the file item has metadata that indicates that this item is intended for the current user or if
                    //  there was no metadata that defined a user, add the file to the list to be included in the search results.
                    alFileList.add(icfFileItem);
                }

            } //End loop going through the folder that the user selected.


        }catch (Exception e){
            GlobalClass.gbImportFolderAnalysisRunning = false;
            GlobalClass.gbImportFolderAnalysisFinished = true;
            sMessage = "Problem during Worker_Import_GetHoldingFolderDirectoryContents: " + e.getMessage();
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
        GlobalClass.gbImportFolderAnalysisFinished = true;
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, lProgressNumerator + "/" + lProgressDenominator,
                Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

        GlobalClass.gbImportFolderAnalysisRunning = false;
        if(GlobalClass.gbImportFolderAnalysisStop) {
            GlobalClass.gbImportFolderAnalysisStop = false;
        }

        return Result.success();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Import_GetHoldingFolderDirectoryContents:" + sRoutine, sMessage);
    }

}
