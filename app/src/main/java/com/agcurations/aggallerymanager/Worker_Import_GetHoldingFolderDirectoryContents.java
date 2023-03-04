package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    private double gdTimeStamp;

    public Worker_Import_GetHoldingFolderDirectoryContents(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gdTimeStamp = getInputData().getDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, -1);
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
            TreeMap<String, String[]> tmHoldingFolderRecordData = new TreeMap<>();
            int MEDIA_FILE_NAME_INDEX = 0;
            int MEDIA_FILE_EXTENSION_INDEX = 1;
            int MEDIA_FILE_MIME_TYPE_INDEX = 2;
            int MEDIA_FILE_URI_STRING_INDEX = 3;
            int MEDIA_FILE_LAST_MODIFIED_INDEX = 4;
            int MEDIA_FILE_SIZE_INDEX = 5;
            int METADATA_FILE_URI_STRING_INDEX = 6;
            int iFieldCount = 7;

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
                            //todo: what if the media file name does not have an extension? I think the program may crash.
                            String sFileName = c.getString(0);
                            String sFileBaseName = sFileName.substring(0, sFileName.lastIndexOf("."));
                            sFileBaseName = URLDecoder.decode(sFileBaseName, StandardCharsets.UTF_8.toString()); //todo: this line may not be needed after a while due to
                                        //inclusion in VideoEnabledWebView. There was an issue with encoded filename as the downloader would clean the name up
                                        //  before creating the file, but the metadata text file that the program creates would retain those characters.
                                        //  Characters such as '%20' for 'SPACE' character.
                            String sFileExtension = sFileName.substring(sFileName.lastIndexOf("."));
                            if(sFileExtension.equals(".txt")){
                                //The base name of this file will include the media file name with the media file extension. Get the base file name again.
                                sFileBaseName = sFileBaseName.substring(0, sFileBaseName.lastIndexOf("."));

                                //Due to a legacy issue, the base file name that was retrieved from this metadata file may be too long. Download
                                //  manager will have shortened the name before completing the download.
                                //  todo:Remove this code after the holding folder is cleared and try test at rule 34 phael
                                int iFileNameMaxLength = 47; //Arbitrarily set because I don't know Download Manager's rules. Gave some buffer from what I have seen.
                                if(sFileBaseName.length() > iFileNameMaxLength){
                                    //Limit max length of file name or download manager will do it for you.
                                    int iAmountToTrim = sFileBaseName.length() - iFileNameMaxLength;
                                    sFileBaseName = sFileBaseName.substring(0, sFileBaseName.length() - iAmountToTrim);
                                }


                            }
                            String sFileLastModified = c.getString(2);
                            String sFileSize = c.getString(3);
                            if(!tmHoldingFolderRecordData.containsKey(sFileBaseName)){
                                //If we have not yet processed this FileBaseName, start it:
                                String[] sDataRecord = new String[iFieldCount];
                                if(sFileExtension.equals(".txt")){
                                    //Prepare the Metadata entry
                                    String sMetadataFileUri = GlobalClass.FormChildUriString(GlobalClass.gUriImageDownloadHoldingFolder.toString(), sFileName);
                                    sDataRecord[METADATA_FILE_URI_STRING_INDEX] = sMetadataFileUri;
                                } else {
                                    //Prepare the file entry
                                    sDataRecord[MEDIA_FILE_NAME_INDEX] = sFileName;
                                    sDataRecord[MEDIA_FILE_EXTENSION_INDEX] = sFileExtension;
                                    sDataRecord[MEDIA_FILE_MIME_TYPE_INDEX] = sMimeType;
                                    String sMediaFileUri = GlobalClass.FormChildUriString(GlobalClass.gUriImageDownloadHoldingFolder.toString(), sFileName);
                                    sDataRecord[MEDIA_FILE_URI_STRING_INDEX] = sMediaFileUri;
                                    sDataRecord[MEDIA_FILE_LAST_MODIFIED_INDEX] = sFileLastModified;
                                    sDataRecord[MEDIA_FILE_SIZE_INDEX] = sFileSize;
                                }
                                tmHoldingFolderRecordData.put(sFileBaseName, sDataRecord);
                            } else {
                                String[] sDataRecord = tmHoldingFolderRecordData.get(sFileBaseName);
                                if(sDataRecord == null){
                                    //This should never happen, but Android Studio complains that it could.
                                    sDataRecord = new String[5];
                                }
                                if(sFileExtension.equals(".txt")){
                                    //Prepare the Metadata entry
                                    String sMetadataFileUri = GlobalClass.FormChildUriString(GlobalClass.gUriImageDownloadHoldingFolder.toString(), sFileName);
                                    sDataRecord[METADATA_FILE_URI_STRING_INDEX] = sMetadataFileUri;
                                } else {
                                    //Prepare the file entry
                                    sDataRecord[MEDIA_FILE_NAME_INDEX] = sFileName;
                                    sDataRecord[MEDIA_FILE_EXTENSION_INDEX] = sFileExtension;
                                    sDataRecord[MEDIA_FILE_MIME_TYPE_INDEX] = sMimeType;
                                    String sFileMediaUri = GlobalClass.FormChildUriString(GlobalClass.gUriImageDownloadHoldingFolder.toString(), sFileName);
                                    sDataRecord[MEDIA_FILE_URI_STRING_INDEX] = sFileMediaUri;
                                    sDataRecord[MEDIA_FILE_LAST_MODIFIED_INDEX] = sFileLastModified;
                                    sDataRecord[MEDIA_FILE_SIZE_INDEX] = sFileSize;
                                }
                                tmHoldingFolderRecordData.replace(sFileBaseName, sDataRecord);
                            }

                        }
                    }
                    c.close();
                }
            } catch (Exception e) {
                LogThis("doWork()", "Problem querying folder and processing file listings.", e.getMessage());
            }



            if(tmHoldingFolderRecordData.size() == 0){
                GlobalClass.gbImportFolderAnalysisRunning = false;
                GlobalClass.gbImportFolderAnalysisFinished = true;
                String sMessage = "No files found in the holding folder.";
                globalClass.gsbImportFolderAnalysisLog.append(sMessage);
                globalClass.problemNotificationConfig(sMessage, Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
                return Result.success();
            }

            //Calculate total number of files for a progress bar:
            lProgressDenominator = tmHoldingFolderRecordData.size();

            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, "0/" + lProgressDenominator,
                    Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);


            //Process the holding folder entries:
            for(Map.Entry<String, String[]> HoldingFolderEntry: tmHoldingFolderRecordData.entrySet()){

                if(GlobalClass.gbImportFolderAnalysisStop){
                    break;
                }

                //Check to see if it is a video or an image and if we are looking for videos or images.
                String sMimeType = HoldingFolderEntry.getValue()[MEDIA_FILE_MIME_TYPE_INDEX];
                String sMediaFileExtension = HoldingFolderEntry.getValue()[MEDIA_FILE_EXTENSION_INDEX];
                String sMediaFileName = HoldingFolderEntry.getValue()[MEDIA_FILE_NAME_INDEX];

                String s;
                if(sMimeType == null){
                    s ="sad";
                }
                if (sMimeType.startsWith("video") ||
                        sMediaFileExtension.equals(".gif") ||
                        sMediaFileExtension.equals(".txt") ||
                        (sMimeType.equals("application/octet-stream") && sMediaFileExtension.equals(".mp4"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid
                    //If not a file that we want to analyze...
                    continue; //If requesting images and mimeType is video or the file a gif, go to next loop.
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


                //If the file is video, get the duration so that the file list can be sorted by duration if requested.
                long lDurationInMilliseconds = -1L;
                String sWidth = "";  //We are not doing math with the width and height. Therefore no need to convert to int.
                String sHeight = "";

                //Get date last modified:
                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                cal.setTimeInMillis(lMediaFileLastModified);
                Date dateLastModified = cal.getTime();

                //Get the width and height of the image:
                try {

                    BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                    onlyBoundsOptions.inJustDecodeBounds = true;

                    Uri uriMediaFile = Uri.parse(HoldingFolderEntry.getValue()[MEDIA_FILE_URI_STRING_INDEX]);
                    InputStream isImageFile = GlobalClass.gcrContentResolver.openInputStream(uriMediaFile);
                    if(isImageFile == null){
                        String sMessage = "Could not open image file for analysis: " + sMediaFileName;
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
                icfFileItem.sUri = HoldingFolderEntry.getValue()[MEDIA_FILE_URI_STRING_INDEX];
                icfFileItem.sUriParent = GlobalClass.gUriImageDownloadHoldingFolder.toString();
                icfFileItem.sMimeType = sMimeType;
                //Get the URL data from the associated metadata file, if it exists:

                String sMetaDataFileUri = HoldingFolderEntry.getValue()[METADATA_FILE_URI_STRING_INDEX];
                if(sMetaDataFileUri != null) {
                    try {
                        Uri uriMetaDataFile = Uri.parse(sMetaDataFileUri);
                        InputStream isImageMetadataFile = GlobalClass.gcrContentResolver.openInputStream(uriMetaDataFile);
                        if(isImageMetadataFile == null){
                            String sMessage = "Could not open metadata file for analysis: " + sMetaDataFileUri;
                            LogThis("doWork()", sMessage, null);
                            continue;
                        }
                        BufferedReader brReader;
                        brReader = new BufferedReader(new InputStreamReader(isImageMetadataFile));
                        icfFileItem.sURL = brReader.readLine();
                        icfFileItem.sTitle = brReader.readLine();
                        brReader.close();
                        isImageMetadataFile.close();
                    } catch (Exception e) {
                        String sMessage = "Could not open metadata file for analysis: " + sMetaDataFileUri;
                        LogThis("doWork()", sMessage, e.getMessage());
                    }
                }
                //Add the ItemClass_File to the ArrayList:
                //LogThis("doWork", "Added entry: " + icfFileItem.sUri, null);
                alFileList.add(icfFileItem);

            } //End loop going through the folder that the user selected.


        }catch (Exception e){
            GlobalClass.gbImportFolderAnalysisRunning = false;
            GlobalClass.gbImportFolderAnalysisFinished = true;
            String sMessage = "Problem during Worker_Import_GetHoldingFolderDirectoryContents: " + e.getMessage();
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

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Import_GetHoldingFolderDirectoryContents:" + sRoutine, sMessage);
    }

}
