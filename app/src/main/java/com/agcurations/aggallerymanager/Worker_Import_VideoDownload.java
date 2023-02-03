package com.agcurations.aggallerymanager;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.exoplayer2.util.MimeTypes;

import org.w3c.dom.Document;

public class Worker_Import_VideoDownload extends Worker {

    public static final String TAG_WORKER_IMPORT_VIDEODOWNLOAD = "com.agcurations.aggallermanager.tag_worker_import_videodownload";

    String gsAddress;
    
    public Worker_Import_VideoDownload(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsAddress = getInputData().getString(GlobalClass.EXTRA_STRING_WEB_ADDRESS);
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

        String sIntentActionFilter = Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE;

        int iMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;

        globalClass.gbUseFFMPEGToMerge = sharedPreferences.getBoolean(GlobalClass.PREF_USE_FFMPEG_TO_MERGE_VIDEO_STREAMS, false);

        ArrayList<ItemClass_File> alFileList = globalClass.galImportFileList;
        ItemClass_File icfDownloadItem = alFileList.get(0);
        //Use file count as progress denominator:
        if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
            //If this is a single download file, only 1 file needs to be downloaded.
            lProgressDenominator = 1;
        } else {
            //If this is an M3U8 download, a set of files must be downloaded.
            lProgressDenominator = (long) icfDownloadItem.ic_M3U8.als_TSDownloads.size();
        }

        //Find the next record ID:
        String sNextRecordId = globalClass.getNewCatalogRecordID(GlobalClass.MEDIA_CATEGORY_VIDEOS);

        if(icfDownloadItem.sDestinationFolder.equals("")) {
            icfDownloadItem.sDestinationFolder = GlobalClass.gsUnsortedFolderName;
        }

        DocumentFile dfDestination = globalClass.gdfCatalogFolders[iMediaCategory].findFile(icfDownloadItem.sDestinationFolder);

        String sMessage;

        if (dfDestination == null) {
            dfDestination = globalClass.gdfCatalogFolders[iMediaCategory].createDirectory(icfDownloadItem.sDestinationFolder);
            if (dfDestination == null) {
                //Unable to create directory
                sMessage = "Unable to create destination folder " +
                        icfDownloadItem.sDestinationFolder + " at: "
                        + globalClass.gdfCatalogFolders[iMediaCategory].getUri() + "\n";
                globalClass.BroadcastProgress(true, sMessage,
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                return Result.failure();
            } else {
                globalClass.BroadcastProgress(true, "Destination folder created: " + dfDestination.getUri() + "\n",
                        false, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            }
        } else {
            globalClass.BroadcastProgress(true, "Destination folder verified: " + dfDestination.getUri() + "\n",
                    true, iProgressBarValue,
                    false, "",
                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
        }

        //Create a folder to serve as a working folder:
        DocumentFile dfWorkingFolder = dfDestination.findFile(sNextRecordId);

        //Create the temporary download folder (within the destination folder):
        if (dfWorkingFolder == null) {
            dfWorkingFolder = dfDestination.createDirectory(sNextRecordId);
            if (dfWorkingFolder == null) {
                //Unable to create directory
                sMessage = "Unable to create working folder " +
                        sNextRecordId + " at: "
                        + dfDestination.getUri() + "\n";
                globalClass.BroadcastProgress(true, sMessage,
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        sIntentActionFilter);
                return Result.failure();
            } else {
                globalClass.BroadcastProgress(true, "Destination folder created: " + dfWorkingFolder.getUri() + "\n",
                        false, iProgressBarValue,
                        false, "",
                        sIntentActionFilter);
            }
        } else {
            globalClass.BroadcastProgress(true, "Destination folder verified: " + dfWorkingFolder.getUri() + "\n",
                    true, iProgressBarValue,
                    false, "",
                    sIntentActionFilter);
        }

        // With the download folder successfully created, record the catalog item:
        // The below call should add the record to both the catalog contents file
        //  and memory:
        //Next create a new catalog item data structure:
        ItemClass_CatalogItem ciNew = new ItemClass_CatalogItem();
        ciNew.iMediaCategory = iMediaCategory;
        ciNew.sItemID = sNextRecordId;
        ciNew.lSize = icfDownloadItem.lSizeBytes;
        ciNew.lDuration_Milliseconds = icfDownloadItem.lVideoTimeInMilliseconds;
        ciNew.sDuration_Text = icfDownloadItem.sVideoTimeText;
        if(!icfDownloadItem.sWidth.equals("") && !icfDownloadItem.sHeight.equals("")) {
            ciNew.iWidth = Integer.parseInt(icfDownloadItem.sWidth);
            ciNew.iHeight = Integer.parseInt(icfDownloadItem.sHeight);
        }
        ciNew.sFolder_Name = icfDownloadItem.sDestinationFolder;
        ciNew.sTags = GlobalClass.formDelimitedString(icfDownloadItem.aliProspectiveTags, ",");
        ciNew.aliTags = new ArrayList<>(icfDownloadItem.aliProspectiveTags);
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        ciNew.dDatetime_Last_Viewed_by_User = dTimeStamp;
        ciNew.dDatetime_Import = dTimeStamp;
        ciNew.iGrade = icfDownloadItem.iGrade;
        ciNew.sSource = gsAddress;
        ciNew.sTitle = icfDownloadItem.sTitle;
        //ciNew.alsDownloadURLsAndDestFileNames = new ArrayList<>();
        ArrayList<String[]> alsDownloadURLsAndDestFileNames = new ArrayList<>();
        if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
            ciNew.iSpecialFlag = ItemClass_CatalogItem.FLAG_VIDEO_DLM_SINGLE;
            ciNew.iFile_Count = 1;
            ciNew.sVideoLink = icfDownloadItem.sURLVideoLink;
            ciNew.sFilename = GlobalClass.JumbleFileName(icfDownloadItem.sFileOrFolderName);
        } else {
            //M3U8. Mark post-processing to concat videos and move the result.
            if(globalClass.gbUseFFMPEGToMerge) {
                ciNew.iSpecialFlag = ItemClass_CatalogItem.FLAG_VIDEO_DLM_CONCAT;
                //Form a name for the concatenated video file:
                String sTempFilename = icfDownloadItem.ic_M3U8.sFileName;
                sTempFilename = Service_Import.cleanFileNameViaTrim(sTempFilename); //Remove special characters.
                sTempFilename = ciNew.sItemID + "_" + sTempFilename; //Add item ID to create a unique filename.
                sTempFilename = sTempFilename.substring(0,sTempFilename.lastIndexOf(".")); //Remove extension (probably .m3u8).
                if(globalClass.gbUseFFMPEGConvertToMP4) {
                    sTempFilename = sTempFilename + ".mp4"; //Add appropriate extension.
                } else {
                    sTempFilename = sTempFilename + ".ts"; //Add appropriate extension.
                }
                ciNew.sFilename = GlobalClass.JumbleFileName(sTempFilename);
                ciNew.iFile_Count = 1; //There will only be 1 file after concatenation.
            } else {
                ciNew.iSpecialFlag = ItemClass_CatalogItem.FLAG_VIDEO_M3U8;
                //Form a name for the M3U8 file:
                String sTempFilename = icfDownloadItem.ic_M3U8.sFileName;
                sTempFilename = Service_Import.cleanFileNameViaTrim(sTempFilename); //Remove special characters.
                sTempFilename = ciNew.sItemID + "_" + sTempFilename; //Add item ID to create a unique filename.
                ciNew.sFilename = sTempFilename; //Do not jumble the M3U8 file. Exoplayer will not recognize.
                ciNew.iFile_Count = icfDownloadItem.ic_M3U8.als_TSDownloads.size(); //Record the file count.

                //Configure thumbnail file for M3U8:
                String sThumbnailURL = icfDownloadItem.sURLThumbnail;
                try{
                    String sThumbnailFileName = sThumbnailURL.substring(sThumbnailURL.lastIndexOf("/") + 1);
                    sThumbnailFileName = Service_Import.cleanFileNameViaTrim(sThumbnailFileName);
                    ciNew.sThumbnail_File = GlobalClass.JumbleFileName(sThumbnailFileName);
                    alsDownloadURLsAndDestFileNames.add(new String[]{sThumbnailURL, ciNew.sThumbnail_File});
                } catch (Exception e){
                    globalClass.problemNotificationConfig("Could not get thumbnail image.", sIntentActionFilter);
                }
            }

            ciNew.sVideoLink = icfDownloadItem.ic_M3U8.sBaseURL + "/" + icfDownloadItem.ic_M3U8.sFileName;
            ciNew.lDuration_Milliseconds = (long) (icfDownloadItem.ic_M3U8.fDurationInSeconds * 1000); //Load the duration now for confirmation of video concatenation completion later.

        }

        try {
            globalClass.CatalogDataFile_CreateNewRecord(ciNew);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Map-out the files to be downloaded with destination file names:

        if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
            //If this is a single download file, only 1 file needs to be downloaded.
            String sDownloadAddress = icfDownloadItem.sURLVideoLink;
            String sFileName = icfDownloadItem.sFileOrFolderName;

            if(sFileName.length() > 50){
                //Limit the length of the filename:
                String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sFileName);
                if(sBaseAndExtension.length == 2) {
                    sFileName = sBaseAndExtension[0].substring(0, 50 - sBaseAndExtension[1].length());
                    sFileName = sFileName + "." + sBaseAndExtension[1];
                }
            }

            alsDownloadURLsAndDestFileNames.add(new String[]{sDownloadAddress, sFileName});

        } else {
            //If this is an M3U8 download, a set of files must be downloaded.
            for(String sFileName: icfDownloadItem.ic_M3U8.als_TSDownloads){
                String sDownloadAddress;
                if(sFileName.startsWith("http")){
                    sDownloadAddress = sFileName;
                } else {
                    sDownloadAddress = icfDownloadItem.ic_M3U8.sBaseURL + "/" + sFileName;
                }
                if(sFileName.contains("/")){
                    sFileName = sFileName.substring(sFileName.lastIndexOf("/") + 1);
                }
                String sNewFilename = ciNew.sItemID + "_" + Service_Import.cleanFileNameViaTrim(sFileName);  //the 'save-to' filename cannot have special chars or downloadManager will not download the file.

                if(sNewFilename.length() > 50){
                    //Limit the length of the filename:
                    String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sNewFilename);
                    if(sBaseAndExtension.length == 2) {
                        sNewFilename = sBaseAndExtension[0].substring(0, 50 - sBaseAndExtension[1].length());
                        sNewFilename = sNewFilename + "." + sBaseAndExtension[1];
                    }
                }

                if(ciNew.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8){
                    //If we will be holding the .ts files in storage as part of a local M3U8 configuration,
                    // jumble the .ts filenames:
                    sNewFilename = GlobalClass.JumbleFileName(sNewFilename);
                }
                alsDownloadURLsAndDestFileNames.add(new String[]{sDownloadAddress, sNewFilename});
            }
        }



        //Initiate the download(s):

        //NOTE: Android has DownloadIdleService which is reponsible for cleaning up stale or orphan downloads.
        //  I have witnessed disappearance of downloaded files. This service seems to be deleting comic files.
        //See article at: https://www.vvse.com/blog/blog/2020/01/06/android-10-automatically-deletes-downloaded-files/

        try {

            DownloadManager downloadManager;
            downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);

            //ArrayList<Long> allDownloadIDs = new ArrayList<>();

            //Download the file(s):
            int FILE_DOWNLOAD_ADDRESS = 0;
            int FILE_NAME_AND_EXTENSION = 1;
            int DOWNLOAD_ID = 0;

            String sVideoDownloadFolder = "";
            ArrayList<String[]> alsDLIDsAndFileNames = new ArrayList<>();
            for(String[] sURLAndFileName: alsDownloadURLsAndDestFileNames) {
                String sNewFullPathFilename = dfWorkingFolder + File.separator + sURLAndFileName[FILE_NAME_AND_EXTENSION];
                //File name is not Jumbled for download as if it is a .ts file download of videos, FFMPEG will
                //  not understand what to do with the files if the extension is unrecognized.
                File fNewFile = new File(sNewFullPathFilename);
                /*//Debugging an issue...
                String s;
                try {
                    s = fNewFile.getCanonicalPath();  //Check to see if the file name and path is valid:
                } catch (Exception e){
                    sNewFullPathFilename = fTempDestination + File.separator + sURLAndFileName[FILE_NAME_AND_EXTENSION];
                    fNewFile = new File(sNewFullPathFilename);
                    try{
                        s = fNewFile.getCanonicalPath();
                    } catch (Exception e2){
                        Log.d("File exception", e2.getMessage());
                    }
                    Log.d("File exception", e.getMessage());
                }*/
                if(!fNewFile.exists()) {

                    globalClass.BroadcastProgress(true, "Initiating download of file: " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS] + "...",
                            false, iProgressBarValue,
                            true, "Submitting download requests...",
                            sIntentActionFilter);

                    //Use the download manager to download the file:
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sURLAndFileName[FILE_DOWNLOAD_ADDRESS]));
                    /*request.setTitle("AG Gallery+ File Download: " + "Video ID " + ciNew.sItemID)
                            .setDescription("Video ID " + ciNew.sItemID + "; " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS])
                            //.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                            //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                            //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                            .setMimeType("application/octet-stream")
                            .setDestinationUri(Uri.fromFile(fNewFile));*/
                    //The above method no longer works as of Android 11 API level 30, One UI version 3.1.
                    String sDownloadFolderRelativePath;
                    sDownloadFolderRelativePath = File.separator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS] +
                            File.separator + ciNew.sFolder_Name +
                            File.separator + ciNew.sItemID;
                    sVideoDownloadFolder = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() +
                            sDownloadFolderRelativePath;
                    request.setTitle("AGGallery+ Download " + (lProgressNumerator + 1) + " of " + lProgressDenominator + " VideoID " + ciNew.sItemID)
                            //.setDescription("Video ID " + ciNew.sItemID + "; " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS])
                            //.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) //Make download notifications disappear when completed.
                            //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                            //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                            .setMimeType("application/octet-stream")
                            .setDestinationInExternalFilesDir(getApplicationContext(), sDownloadFolderRelativePath, fNewFile.getName());

                    long lDownloadID = downloadManager.enqueue(request);
                    //todo: Check to make sure that the download is approved. Such as download source exists, and filename is
                    //  valid and not already taken.
                    //Record the download ID so that we can check to see if it is completed via the worker.
                    alsDLIDsAndFileNames.add(new String[]{
                            String.valueOf(lDownloadID),
                            sURLAndFileName[FILE_NAME_AND_EXTENSION]});

                    lProgressNumerator++;

                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    globalClass.BroadcastProgress(true, "\n",
                            true, iProgressBarValue,
                            false, "",
                            sIntentActionFilter);
                }

            }
            //Success initiating file download(s).

            int iSingleOrM3U8;
            if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
                iSingleOrM3U8 = Worker_VideoPostProcessing.DOWNLOAD_TYPE_SINGLE;
            } else if(!globalClass.gbUseFFMPEGToMerge) {
                iSingleOrM3U8 = Worker_VideoPostProcessing.DOWNLOAD_TYPE_M3U8_LOCAL;
            } else {
                iSingleOrM3U8 = Worker_VideoPostProcessing.DOWNLOAD_TYPE_M3U8;
            }

            //If this is an M3U8 stream download and the user has elected not to concatenate via
            //  FFMPEG, download the M3U8 file as well:
            if((icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_M3U8) &&
                    (!globalClass.gbUseFFMPEGToMerge)){

                ItemClass_M3U8 icM3U8_entry = icfDownloadItem.ic_M3U8;

                String sUrl = icM3U8_entry.sBaseURL + "/" + icM3U8_entry.sFileName;
                URL url = new URL(sUrl);
                URLConnection connection = url.openConnection();
                connection.connect();

                // download the M3U8 text file:
                InputStream inputStream = new BufferedInputStream(url.openStream(), 1024 * 8);
                BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder total = new StringBuilder();
                for (String line; (line = r.readLine()) != null; ) {
                    total.append(line).append('\n');
                }
                String sM3U8Content = total.toString();
                inputStream.close();

                //Write the m3u8 file to the working folder:
                DocumentFile dfM3U8 = dfWorkingFolder.createFile(MimeTypes.BASE_TYPE_TEXT, ciNew.sFilename);
                if(dfM3U8 == null){
                    sMessage = "Could not create M3U8 file.";
                    globalClass.BroadcastProgress(true, sMessage,
                            true, iProgressBarValue,
                            true, sMessage,
                            sIntentActionFilter);
                    return Result.failure();
                }
                OutputStream osM3U8File = GlobalClass.gcrContentResolver.openOutputStream(dfM3U8.getUri(), "wa");
                if(osM3U8File == null){
                    sMessage = "Could not write M3U8 file.";
                    globalClass.BroadcastProgress(true, sMessage,
                            true, iProgressBarValue,
                            true, sMessage,
                            sIntentActionFilter);
                    return Result.failure();
                }
                BufferedWriter bwM3U8File = new BufferedWriter(new OutputStreamWriter(osM3U8File));

                String[] sLines = sM3U8Content.split("\n");
                for (String sLine : sLines) {
                    if (!sLine.startsWith("#") && sLine.contains(".ts")) {// && sLine.startsWith("hls")) {
                        if(sLine.contains("/")){
                            sLine = sLine.substring(sLine.lastIndexOf("/") + 1);
                        }
                        String sTSShortFileName = ciNew.sItemID + "_" + Service_Import.cleanFileNameViaTrim(sLine);
                        String sJumbledTSShortFileName = GlobalClass.JumbleFileName(sTSShortFileName);
                        bwM3U8File.write(sJumbledTSShortFileName + "\n");
                    } else if (sLine.contains("ENDLIST")) {
                        bwM3U8File.write(sLine + "\n");
                        break;
                    } else {
                        bwM3U8File.write(sLine + "\n");
                    }

                }

                bwM3U8File.flush();
                bwM3U8File.close();
                osM3U8File.flush();
                osM3U8File.close();

            }




            //Start Video Post-Processing Worker.
            //Testing WorkManager for video concatenation:
            //https://developer.android.com/topic/libraries/architecture/workmanager/advanced

            //Send:
            // Location to monitor
            // Single-file or M3U8 result (M3U8 => multiple .ts files)
            // Expected file count (needed for M3U8)
            // Name of file to create

            /*public static final String KEY_ARG_DOWNLOAD_TYPE_SINGLE_OR_M3U8 = "KEY_ARG_DOWNLOAD_TYPE_SINGLE_OR_M3U8";
            public static final String KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS = "KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS";
            public static final String KEY_ARG_EXPECTED_DOWNLOAD_FILE_COUNT = "KEY_ARG_EXPECTED_DOWNLOAD_FILE_COUNT";
            public static final String KEY_ARG_VIDEO_OUTPUT_FILENAME = "KEY_ARG_VIDEO_OUTPUT_FILENAME";*/


            DocumentFile dfVideoFinalDestinationFolder = globalClass.gdfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].findFile(ciNew.sFolder_Name);
            if(dfVideoFinalDestinationFolder == null){
                sMessage = "Could not locate video final destination folder " + ciNew.sFolder_Name + " in " +
                        globalClass.gdfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getUri();
                return Result.failure(DataErrorMessage(sMessage));
            }
            DocumentFile dfVideoWorkingFolder = dfVideoFinalDestinationFolder.findFile(ciNew.sItemID);
            if(dfVideoWorkingFolder == null){
                sMessage = "Could not locate video working folder " + ciNew.sItemID + " in " +
                        dfVideoFinalDestinationFolder.getUri();
                return Result.failure(DataErrorMessage(sMessage));
            }
            //Prepare the file sequence so that an M3U8 sequence can be concatenated properly.
            //String[] sFilenameSequence = new String[ciNew.alsDownloadURLsAndDestFileNames.size()];
            //A file sequence string array can be too big to pass to a worker, so write it to a file:

            final DocumentFile dfDLIDFileSequenceFile = dfVideoWorkingFolder.createFile(MimeTypes.BASE_TYPE_TEXT,
                    Worker_VideoPostProcessing.VIDEO_DLID_AND_SEQUENCE_FILE_NAME);
            if(dfDLIDFileSequenceFile == null){
                sMessage = "Could not create file to record download ID sequencing: " + Worker_VideoPostProcessing.VIDEO_DLID_AND_SEQUENCE_FILE_NAME + " in " +
                        dfVideoWorkingFolder.getUri();
                return Result.failure(DataErrorMessage(sMessage));
            }
            OutputStream osDLIDFileSequenceFile = GlobalClass.gcrContentResolver.openOutputStream(dfDLIDFileSequenceFile.getUri(), "wt");
            if(osDLIDFileSequenceFile == null){
                sMessage = "Could not open output stream to file " + dfDLIDFileSequenceFile.getUri();
                return Result.failure(DataErrorMessage(sMessage));
            }
            BufferedWriter bwDLIDFileSequenceFile;
            bwDLIDFileSequenceFile = new BufferedWriter(new OutputStreamWriter(osDLIDFileSequenceFile));

            for(String[] sDLIDsAndFileNames: alsDLIDsAndFileNames){
                String sLine = sDLIDsAndFileNames[DOWNLOAD_ID] + "\t" + sDLIDsAndFileNames[FILE_NAME_AND_EXTENSION] + "\n";
                bwDLIDFileSequenceFile.write(sLine);
            }
            bwDLIDFileSequenceFile.flush();
            bwDLIDFileSequenceFile.close();
            osDLIDFileSequenceFile.flush();
            osDLIDFileSequenceFile.close();

            //Build-out data to send to the worker:
            Data dataVideoPostProcessor = new Data.Builder()
                    .putInt(Worker_VideoPostProcessing.KEY_ARG_DOWNLOAD_TYPE_SINGLE_M3U8_M3U8LOCAL, iSingleOrM3U8)
                    .putString(Worker_VideoPostProcessing.KEY_ARG_URI_PATH_TO_MONITOR_FOR_DOWNLOADS, sVideoDownloadFolder)
                    .putString(Worker_VideoPostProcessing.KEY_ARG_URI_PATH_TO_WORKING_FOLDER, dfVideoWorkingFolder.getUri().toString())
                    //.putStringArray(Worker_VideoPostProcessing.KEY_ARG_FILENAME_SEQUENCE, sFilenameSequence)
                    .putString(Worker_VideoPostProcessing.KEY_ARG_VIDEO_OUTPUT_FILENAME, GlobalClass.JumbleFileName(ciNew.sFilename)) //Double-jumble.
                    .putLong(Worker_VideoPostProcessing.KEY_ARG_VIDEO_TOTAL_FILE_SIZE, ciNew.lSize)
                    //.putLongArray(Worker_VideoPostProcessing.KEY_ARG_DOWNLOAD_IDS, lDownloadIDs)
                    .putString(Worker_VideoPostProcessing.KEY_ARG_ITEM_ID, ciNew.sItemID)
                    .build();
            OneTimeWorkRequest otwrVideoPostProcessor = new OneTimeWorkRequest.Builder(Worker_VideoPostProcessing.class)
                    .setInputData(dataVideoPostProcessor)
                    .addTag(Worker_VideoPostProcessing.WORKER_VIDEO_POST_PROCESSING_TAG) //To allow finding the worker later.
                    .build();
            UUID UUIDWorkID = otwrVideoPostProcessor.getId();
            WorkManager.getInstance(getApplicationContext()).enqueue(otwrVideoPostProcessor);

            sMessage = "Operation complete.\n";
            if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
                sMessage = sMessage + "The download will continue in the background and will be available once completed.\n";
            } else {
                sMessage = sMessage + "M3U8 video downloads will continue in the background and will be concatenated into a single video once completed.\n";
            }
            //Check to see if the user has applied a restricted tag so that an appropriate reminder
            //  message can be shown:
            boolean bRestrictedTagInUse = false;
            String sRecordTags = ciNew.sTags;
            if(sRecordTags.length() > 0) {
                String[] saRecordTags = sRecordTags.split(",");
                for (String s : saRecordTags) {
                    //if list of restricted tags contains this particular record tag, mark as restricted item:
                    int iTagID;
                    //String sErrorMessage;
                    try {
                        iTagID = Integer.parseInt(s);
                    } catch (Exception e){
                        //sErrorMessage = e.getMessage();
                        continue;
                    }
                    ItemClass_Tag ict = globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).get(iTagID);
                    if (ict != null) {
                        if (ict.bIsRestricted) {
                            bRestrictedTagInUse = true;
                            break;
                        }
                    }
                }
            }
            if(bRestrictedTagInUse) {
                sMessage = sMessage + "The item will not be readily shown in the catalog due to the application of a restricted tag.\n";
            }

            globalClass.BroadcastProgress(true, sMessage,
                    true, iProgressBarValue,
                    true, "All downloads started",
                    sIntentActionFilter);

        } catch (Exception e) {
            if(e.getMessage() != null) {
                Log.e("Error: ", e.getMessage());
            }
            globalClass.BroadcastProgress(true, "Problem encountered:\n" + e.getMessage(),
                    false, iProgressBarValue,
                    true, "Operation halted.",
                    sIntentActionFilter);
        }
        
        globalClass.gbImportExecutionRunning = false;
        globalClass.gbImportExecutionFinished = true;
        return Result.success();
    }

    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }



}
