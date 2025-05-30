package com.agcurations.aggallerymanager;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Import_VideoDownload extends Worker {

    public static final String TAG_WORKER_IMPORT_VIDEODOWNLOAD = "com.agcurations.aggallermanager.tag_worker_import_videodownload";

    public static final String IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE";

    public static final String EXTRA_STRING_IMPORT_FILES_LOCATOR_AL_KEY = "com.agcurations.aggallermanager.extra_string_import_files_locator_al_key";
    
    public static final String VIDEO_DLID_AND_SEQUENCE_FILE_NAME = "DLID_And_Sequence.txt";
    public static final int DOWNLOAD_TYPE_SINGLE = 1;
    public static final int DOWNLOAD_TYPE_M3U8 = 2;

    String gsAddress;
    String gsDataLocatorKey;
    
    public Worker_Import_VideoDownload(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsAddress = getInputData().getString(GlobalClass.EXTRA_STRING_WEB_ADDRESS);
        gsDataLocatorKey = getInputData().getString(EXTRA_STRING_IMPORT_FILES_LOCATOR_AL_KEY);
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

        int iMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;

        //Get the data needed by this worker:
        if(gsDataLocatorKey == null){
            globalClass.BroadcastProgress(true, "Data transfer to Import Video Download worker incomplete: no data key.",
                    false, 0,
                    false, "",
                    IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
            return Result.failure();
        }
        if(!globalClass.WaitForObjectReady(GlobalClass.gabImportFileListTMAvailable, 1)){
            globalClass.BroadcastProgress(true, "Data transfer to Import Video Download worker incomplete: timeout.",
                    false, 0,
                    false, "",
                    IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
            return Result.failure();
        }

        GlobalClass.gabImportFileListTMAvailable.set(false);
        ArrayList<ItemClass_File> alFileList = null;
        if(GlobalClass.gtmalImportFileList.get(gsDataLocatorKey) != null) {
            alFileList = new ArrayList<>(Objects.requireNonNull(GlobalClass.gtmalImportFileList.get(gsDataLocatorKey)));
        }
        GlobalClass.gabImportFileListTMAvailable.set(true);

        if(alFileList == null){
            globalClass.BroadcastProgress(true, "Data transfer to Import Video Download worker incomplete: no data.",
                    false, 0,
                    false, "",
                    IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
            return Result.failure();
        }

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
        String sNextRecordId = GlobalClass.getNewCatalogRecordID();

        if(icfDownloadItem.sDestinationFolder.equals("")) {
            icfDownloadItem.sDestinationFolder = GlobalClass.gsUnsortedFolderName;
        }

        Uri uriSequenceFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[iMediaCategory].toString(), icfDownloadItem.sDestinationFolder);
        String sUserFriendlySequenceFolder = GlobalClass.cleanHTMLCodedCharacters(uriSequenceFolder.toString());

        String sMessage;

        if (!GlobalClass.CheckIfFileExists(uriSequenceFolder)) {
            uriSequenceFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[iMediaCategory].toString(), icfDownloadItem.sDestinationFolder);
            //todo: this should already be handled by a call to GlobalClass.getAGGMStorageFolderAvailability in Fragment_Import_5_Confirmation. Confirm.

            try {
                uriSequenceFolder = GlobalClass.CreateDirectory(uriSequenceFolder);

                //If we are creating a folder here, perform the below action if necessary.
                //If the user has been performing catalog analysis, that is, analyzing the catalog
                // in this session, update the analysis index to include this new folder.
                if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).size() > 0){
                    //If a folder was created record it
                    //  in the Catalog Analysis index variable if the index is in use.
                    String sKey = icfDownloadItem.sDestinationFolder;
                    ItemClass_File icfCollectionFolder = new ItemClass_File(ItemClass_File.TYPE_FOLDER, icfDownloadItem.sDestinationFolder);
                    icfCollectionFolder.sMimeType = DocumentsContract.Document.MIME_TYPE_DIR;
                    icfCollectionFolder.lSizeBytes = 0;
                    icfCollectionFolder.sUriParent = GlobalClass.gUriCatalogFolders[iMediaCategory].toString();
                    icfCollectionFolder.sMediaFolderRelativePath = icfDownloadItem.sDestinationFolder;
                    icfCollectionFolder.sUriThumbnailFile = "";
                    icfCollectionFolder.sUri = uriSequenceFolder.toString();

                    try {
                        Bundle bundle = DocumentsContract.getDocumentMetadata(GlobalClass.gcrContentResolver, uriSequenceFolder);
                        if(bundle != null) {
                            long lLastModified = bundle.getLong(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0);
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(lLastModified);
                            icfCollectionFolder.dateLastModified = cal.getTime();

                        }
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).put(sKey, icfCollectionFolder);

                }




            } catch (Exception e){
                sMessage = "Could not locate parent directory of destination folder in order to create destination folder. Destination folder: " + sUserFriendlySequenceFolder;
                LogThis("doWork()", sMessage, e.getMessage());
                uriSequenceFolder = null;
            }


            if (uriSequenceFolder == null) {
                //Unable to create directory
                sMessage = "Unable to create destination folder " +
                        icfDownloadItem.sDestinationFolder + " at: "
                        + GlobalClass.gUriCatalogFolders[iMediaCategory] + "\n";
                globalClass.BroadcastProgress(true, sMessage,
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
                GlobalClass.gabImportExecutionRunning.set(false);
                GlobalClass.gabImportExecutionFinished.set(true);
                return Result.failure();
            } else {
                globalClass.BroadcastProgress(true, "Destination folder created: " + sUserFriendlySequenceFolder + "\n",
                        false, iProgressBarValue,
                        false, "",
                        IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
            }
        } else {
            globalClass.BroadcastProgress(true, "Destination folder verified: " + sUserFriendlySequenceFolder + "\n",
                    true, iProgressBarValue,
                    false, "",
                    IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
        }

        Uri uriDestinationFolder = null;
        if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_M3U8) {
            //Create the destination folder:
            uriDestinationFolder = GlobalClass.FormChildUri(uriSequenceFolder.toString(), sNextRecordId);
            if (!GlobalClass.CheckIfFileExists(uriDestinationFolder)) {

                try {
                    uriDestinationFolder = GlobalClass.CreateDirectory(uriDestinationFolder);

                    //If we are creating a folder here, perform the below action if necessary.
                    //If the user has been performing catalog analysis, that is, analyzing the catalog
                    // in this session, update the analysis index to include this new folder.
                    if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).size() > 0){
                        //If a folder was created record it
                        //  in the Catalog Analysis index variable if the index is in use.
                        String sKey = icfDownloadItem.sDestinationFolder;
                        ItemClass_File icfCollectionFolder = new ItemClass_File(ItemClass_File.TYPE_FOLDER, icfDownloadItem.sDestinationFolder);
                        icfCollectionFolder.sMimeType = DocumentsContract.Document.MIME_TYPE_DIR;
                        icfCollectionFolder.lSizeBytes = 0;
                        icfCollectionFolder.sUriParent = GlobalClass.gUriCatalogFolders[iMediaCategory].toString();
                        icfCollectionFolder.sMediaFolderRelativePath = icfDownloadItem.sDestinationFolder;
                        icfCollectionFolder.sUriThumbnailFile = "";
                        icfCollectionFolder.sUri = uriSequenceFolder.toString();

                        try {
                            Bundle bundle = DocumentsContract.getDocumentMetadata(GlobalClass.gcrContentResolver, uriSequenceFolder);
                            if(bundle != null) {
                                long lLastModified = bundle.getLong(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0);
                                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                                cal.setTimeInMillis(lLastModified);
                                icfCollectionFolder.dateLastModified = cal.getTime();

                            }
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).put(sKey, icfCollectionFolder);

                    }
                    


                } catch (Exception e) {
                    sMessage = "Could not locate parent directory of destination folder in order to create working folder. Working folder: " + uriDestinationFolder;
                    LogThis("doWork()", sMessage, e.getMessage());
                    uriDestinationFolder = null;
                }

                if (uriDestinationFolder == null) {
                    //Unable to create directory
                    sMessage = "Unable to create working folder " +
                            sNextRecordId + " at: "
                            + sUserFriendlySequenceFolder + "\n";
                    globalClass.BroadcastProgress(true, sMessage,
                            false, iProgressBarValue,
                            true, "Operation halted.",
                            IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
                    GlobalClass.gabImportExecutionRunning.set(false);
                    GlobalClass.gabImportExecutionFinished.set(true);
                    return Result.failure();
                } else {
                    globalClass.BroadcastProgress(true, "Destination folder created: " + sUserFriendlySequenceFolder + "\n",
                            false, iProgressBarValue,
                            false, "",
                            IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
                }
            } else {
                //todo: What to do if the destination folder already exists?
                globalClass.BroadcastProgress(true, "Destination folder already exists (could be an issue): " + sUserFriendlySequenceFolder + "\n",
                        true, iProgressBarValue,
                        false, "",
                        IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
            }
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
        ciNew.sFolderRelativePath = icfDownloadItem.sDestinationFolder;
        ciNew.sTags = GlobalClass.formDelimitedString(icfDownloadItem.aliProspectiveTags, ",");
        ciNew.aliTags = new ArrayList<>(icfDownloadItem.aliProspectiveTags);
        ciNew.iMaturityRating = GlobalClass.getHighestTagMaturityRating(ciNew.aliTags, GlobalClass.MEDIA_CATEGORY_COMICS);
        //ciNew.alsApprovedUsers.add(globalClass.gicuCurrentUser.sUserName);
        ciNew.alsApprovedUsers = GlobalClass.getApprovedUsersForTagGrouping(ciNew.aliTags, ciNew.iMediaCategory);
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        ciNew.dDatetime_Last_Viewed_by_User = dTimeStamp;
        ciNew.dDatetime_Import = dTimeStamp;
        ciNew.iGrade = icfDownloadItem.iGrade;
        ciNew.sSource = gsAddress;
        ciNew.sTitle = icfDownloadItem.sTitle;
        ciNew.sGroupID = icfDownloadItem.sGroupID;

        ArrayList<String[]> alsDownloadURLsAndDestFileNames = new ArrayList<>();
        if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
            ciNew.iSpecialFlag = ItemClass_CatalogItem.FLAG_VIDEO_DLM_SINGLE;
            ciNew.iFile_Count = 1;
            ciNew.sVideoLink = icfDownloadItem.sURLVideoLink;
            ciNew.sFilename = GlobalClass.JumbleFileName(icfDownloadItem.sFileOrFolderName);
        } else {
            //M3U8.
            ciNew.iSpecialFlag = ItemClass_CatalogItem.FLAG_VIDEO_M3U8;
            ciNew.sFolderRelativePath = ciNew.sFolderRelativePath + GlobalClass.gsFileSeparator + ciNew.sItemID;
            //Form a name for the M3U8 file:
            String sTempFilename = icfDownloadItem.ic_M3U8.sFileName;
            sTempFilename = GlobalClass.cleanFileNameViaTrim(sTempFilename); //Remove special characters.
            ciNew.sFilename = sTempFilename; //Do not jumble the M3U8 file. Exoplayer will not recognize.
            ciNew.iFile_Count = icfDownloadItem.ic_M3U8.als_TSDownloads.size(); //Record the file count.

            //Configure thumbnail file for M3U8:
            if(!icfDownloadItem.sURLThumbnail.equals("")) {
                String sThumbnailURL = icfDownloadItem.sURLThumbnail;
                try {
                    String sThumbnailFileName = sThumbnailURL.substring(sThumbnailURL.lastIndexOf("/") + 1);
                    sThumbnailFileName = GlobalClass.cleanFileNameViaTrim(sThumbnailFileName);
                    ciNew.sThumbnail_File = GlobalClass.JumbleFileName(sThumbnailFileName);
                    alsDownloadURLsAndDestFileNames.add(new String[]{sThumbnailURL, ciNew.sThumbnail_File});
                    lProgressDenominator++; //The thumbnail file will be one additional file to download.
                } catch (Exception e) {
                    globalClass.problemNotificationConfig("Could not get thumbnail image.", IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
                }
            } else {
                globalClass.problemNotificationConfig("Could not find a thumbnail image.", IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
            }

            ciNew.sVideoLink = icfDownloadItem.ic_M3U8.sBaseURL + "/" + icfDownloadItem.ic_M3U8.sFileName;
            ciNew.lDuration_Milliseconds = (long) (icfDownloadItem.ic_M3U8.fDurationInSeconds * 1000); //Load the duration now for confirmation of video concatenation completion later.

        }

        try {
            //Check ensure that the record does not have any illegal character sequences that would mess with the data file:
            ItemClass_CatalogItem ciValidatedNew = GlobalClass.validateCatalogItemData(ciNew);
            if(ciValidatedNew.bIllegalDataFound){
                globalClass.BroadcastProgress(true, ciValidatedNew.sIllegalDataNarrative,
                        false, 0,
                        false, "",
                        IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
            }
            globalClass.CatalogDataFile_CreateNewRecord(ciValidatedNew);
        } catch (Exception e) {
            sMessage = "Problem with creating new record. " + e.getMessage();
            globalClass.BroadcastProgress(true, sMessage,
                    false, 0,
                    false, "",
                    IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
            return Result.failure();
        }

        //Map-out the files to be downloaded with destination file names:

        if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
            //If this is a single download file, only 1 file needs to be downloaded.
            String sDownloadAddress = icfDownloadItem.sURLVideoLink;
            String sFileName = icfDownloadItem.sFileOrFolderName;
            sFileName = GlobalClass.JumbleFileName(sFileName);

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

                String sNewFilename = GlobalClass.cleanFileNameViaTrim(sFileName);  //the 'save-to' filename cannot have special chars or downloadManager will not download the file.

                if(sNewFilename.length() > 50){ //todo: this length restriction does not appear to be applied to the formation of the m3u8 file, below.
                    //Limit the length of the filename:
                    String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sNewFilename);
                    if(sBaseAndExtension.length == 2) {
                        sNewFilename = sBaseAndExtension[0].substring(0, 50 - sBaseAndExtension[1].length());
                        sNewFilename = sNewFilename + "." + sBaseAndExtension[1];
                    }
                }

                if(ciNew.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8){
                    //If we will be holding the .ts/.m4s files in storage as part of a local M3U8 configuration,
                    // jumble the .ts/.m4s filenames:
                    sNewFilename = GlobalClass.JumbleFileName(sNewFilename);

                    if(ciNew.sThumbnail_File.equals("")){
                        //If there is no thumbnail file marked for download, specify the file name of the first .ts file
                        //  as the file name for the thumbnail file to be used by the catalog:
                        ciNew.sThumbnail_File = sNewFilename; //todo: Does this line actually get hit? And if so, is the data recorded with the creation of the data record?
                    }
                }



                alsDownloadURLsAndDestFileNames.add(new String[]{sDownloadAddress, sNewFilename});
            }

            if (!icfDownloadItem.ic_M3U8.sEXT_X_MAP.equals("")){
                //If an EXT_X_MAP has been defined, process needs here:
                //Pick-out the file name on the server for download:
                String sLine = icfDownloadItem.ic_M3U8.sEXT_X_MAP;
                int iFirstDQ = sLine.indexOf("\"");
                String sXMAP_FullPathFileOrg = sLine.substring(iFirstDQ + 1, sLine.length() - 1);
                String sXMAP_FileName = "";
                if(sXMAP_FullPathFileOrg.contains("/")){
                    String[] sPathFileSplit = sXMAP_FullPathFileOrg.split("/");
                    if(sPathFileSplit.length == 2){
                        sXMAP_FileName = sPathFileSplit[1];
                    }
                } else {
                    sXMAP_FileName = sXMAP_FullPathFileOrg;
                }
                String sDownloadAddress;
                if(sXMAP_FullPathFileOrg.startsWith("http")){
                    sDownloadAddress = sXMAP_FullPathFileOrg;
                } else {
                    sDownloadAddress = icfDownloadItem.ic_M3U8.sBaseURL + "/" + sXMAP_FullPathFileOrg;
                }

                String sXMAPShortFileName = GlobalClass.cleanFileNameViaTrim(sXMAP_FileName);
                String sJumbledXMAPShortFileName = GlobalClass.JumbleFileName(sXMAPShortFileName);

                alsDownloadURLsAndDestFileNames.add(new String[]{sDownloadAddress, sJumbledXMAPShortFileName});

            }


        }



        //Initiate the download(s):

        //NOTE: Android has DownloadIdleService which is reponsible for cleaning up stale or orphan downloads.
        //See article at: https://www.vvse.com/blog/blog/2020/01/06/android-10-automatically-deletes-downloaded-files/

        try {

            DownloadManager downloadManager;
            downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);

            ArrayList<Long> allDownloadIDs = new ArrayList<>();

            //Download the file(s):
            int FILE_DOWNLOAD_ADDRESS = 0;
            int FILE_NAME_AND_EXTENSION = 1;
            int DOWNLOAD_ID = 0;

            String sVideoDownloadFolder = "";
            ArrayList<String[]> alsDLIDsAndFileNames = new ArrayList<>();
            for(String[] sURLAndFileName: alsDownloadURLsAndDestFileNames) {

                globalClass.BroadcastProgress(true, "Initiating download of file: " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS] + "...",
                        false, iProgressBarValue,
                        true, "Submitting download requests...",
                        IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);

                //Use the download manager to download the file:
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sURLAndFileName[FILE_DOWNLOAD_ADDRESS]));
                String sDownloadFolderRelativePath;
                sDownloadFolderRelativePath = File.separator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS] +
                        File.separator + ciNew.sFolderRelativePath;

                //Resolve issue of %2F in the relative path (a file separator):
                String sDownloadMgrAcceptedFileSeparator = File.separator;
                String sUriFileSeparator = GlobalClass.gsFileSeparator;
                sDownloadFolderRelativePath = sDownloadFolderRelativePath.replace(sUriFileSeparator, sDownloadMgrAcceptedFileSeparator);

                File fExternalFilesDir = getApplicationContext().getExternalFilesDir(null);
                if(fExternalFilesDir != null) {
                    sVideoDownloadFolder = fExternalFilesDir.getAbsolutePath() + sDownloadFolderRelativePath;
                } else {
                    sMessage = "Could not identify external files dir.";
                    globalClass.BroadcastProgress(true, sMessage,
                            false, iProgressBarValue,
                            true, "Halted.",
                            IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
                    GlobalClass.gabImportExecutionRunning.set(false);
                    GlobalClass.gabImportExecutionFinished.set(true);
                    return Result.failure(DataErrorMessage(sMessage));
                }
                request.setTitle("AGGallery+ Download " + (lProgressNumerator + 1) + " of " + lProgressDenominator + " VideoID " + ciNew.sItemID)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) //Make download notifications disappear when completed.
                        //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                        //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                        .setMimeType("application/octet-stream")
                        .setDestinationInExternalFilesDir(getApplicationContext(), sDownloadFolderRelativePath, sURLAndFileName[FILE_NAME_AND_EXTENSION]);

                long lDownloadID = downloadManager.enqueue(request);
                allDownloadIDs.add(lDownloadID);
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
                        IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);


            }
            //Success initiating file download(s).

            int iSingleOrM3U8;
            if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
                iSingleOrM3U8 = DOWNLOAD_TYPE_SINGLE;
            } else {
                iSingleOrM3U8 = DOWNLOAD_TYPE_M3U8;
            }

            //If this is an M3U8 stream download and the user has elected not to concatenate via
            //  FFMPEG, download the M3U8 file as well:
            if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_M3U8) {

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
                assert uriDestinationFolder != null; //Destination folder Uri should not be null via above logic, but Android Studio warns about it nonetheless.
                Uri uriM3U8 = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriDestinationFolder, GlobalClass.BASE_TYPE_TEXT, ciNew.sFilename);
                if (uriM3U8 == null) {
                    sMessage = "Could not create M3U8 file.";
                    globalClass.BroadcastProgress(true, sMessage,
                            true, iProgressBarValue,
                            true, sMessage,
                            IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
                    GlobalClass.gabImportExecutionRunning.set(false);
                    GlobalClass.gabImportExecutionFinished.set(true);
                    return Result.failure();
                }
                OutputStream osM3U8File = GlobalClass.gcrContentResolver.openOutputStream(uriM3U8, "wt");
                if (osM3U8File == null) {
                    sMessage = "Could not write M3U8 file.";
                    globalClass.BroadcastProgress(true, sMessage,
                            true, iProgressBarValue,
                            true, sMessage,
                            IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
                    GlobalClass.gabImportExecutionRunning.set(false);
                    GlobalClass.gabImportExecutionFinished.set(true);
                    return Result.failure();
                }
                BufferedWriter bwM3U8File = new BufferedWriter(new OutputStreamWriter(osM3U8File));

                String[] sLines = sM3U8Content.split("\n");
                for (String sLine : sLines) {
                    if (!sLine.startsWith("#") && (sLine.contains(".ts") || sLine.contains(".m4s"))) {// && sLine.startsWith("hls")) {
                        if (sLine.contains("/")) {
                            sLine = sLine.substring(sLine.lastIndexOf("/") + 1);
                        }
                        String sTSShortFileName = GlobalClass.cleanFileNameViaTrim(sLine);
                        String sJumbledTSShortFileName = GlobalClass.JumbleFileName(sTSShortFileName);
                        //Add the path to the file name, or the video player will not be able to find the file:
                        String sFullPath = uriDestinationFolder + GlobalClass.gsFileSeparator + sJumbledTSShortFileName;
                        bwM3U8File.write(sFullPath + "\n");
                    } else if (sLine.contains("#EXT-X-MAP")){
                        //Break this line appart and re-form it, with a specific focus on correcting
                        //  any path item. Example: #EXT-X-MAP:URI="144p.av1.mp4/init-v1-a1.mp4"
                        int iFirstDQ = sLine.indexOf("\"");
                        String sXMAP_FullPathFileOrg = sLine.substring(iFirstDQ + 1, sLine.length() - 1);
                        String sXMAP_FileName = "";
                        if(sXMAP_FullPathFileOrg.contains("/")){
                            String[] sPathFileSplit = sXMAP_FullPathFileOrg.split("/");
                            if(sPathFileSplit.length == 2){
                                sXMAP_FileName = sPathFileSplit[1];
                            }
                        } else {
                            sXMAP_FileName = sXMAP_FullPathFileOrg;
                        }
                        String sXMAPShortFileName = GlobalClass.cleanFileNameViaTrim(sXMAP_FileName);
                        String sJumbledXMAPShortFileName = GlobalClass.JumbleFileName(sXMAPShortFileName);
                        //Add the path to the file name, or the video player will not be able to find the file:
                        String sXMAPFullPathNew = uriDestinationFolder + GlobalClass.gsFileSeparator + sJumbledXMAPShortFileName;
                        sLine = sLine.replace(sXMAP_FullPathFileOrg, sXMAPFullPathNew);

                        bwM3U8File.write(sLine + "\n");
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

                Uri uriVideoFinalDestinationFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].toString(), ciNew.sFolderRelativePath);
                if (uriVideoFinalDestinationFolder == null) {
                    sMessage = "Could not locate video final destination folder " + ciNew.sFolderRelativePath + " in " +
                            GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS];
                    GlobalClass.gabImportExecutionRunning.set(false);
                    GlobalClass.gabImportExecutionFinished.set(true);
                    return Result.failure(DataErrorMessage(sMessage));
                }

                //A file sequence string array can be too big to pass to a worker, so write it to a file:

                Uri uriDLIDFileSequenceFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriVideoFinalDestinationFolder, GlobalClass.BASE_TYPE_TEXT,
                        VIDEO_DLID_AND_SEQUENCE_FILE_NAME);
                if (uriDLIDFileSequenceFile == null) {
                    sMessage = "Could not create file to record download ID sequencing: " + VIDEO_DLID_AND_SEQUENCE_FILE_NAME + " in " +
                            uriVideoFinalDestinationFolder;
                    GlobalClass.gabImportExecutionRunning.set(false);
                    GlobalClass.gabImportExecutionFinished.set(true);
                    return Result.failure(DataErrorMessage(sMessage));
                }
                OutputStream osDLIDFileSequenceFile = GlobalClass.gcrContentResolver.openOutputStream(uriDLIDFileSequenceFile, "wt");
                if (osDLIDFileSequenceFile == null) {
                    sMessage = "Could not open output stream to file " + uriDLIDFileSequenceFile;
                    GlobalClass.gabImportExecutionRunning.set(false);
                    GlobalClass.gabImportExecutionFinished.set(true);
                    return Result.failure(DataErrorMessage(sMessage));
                }
                BufferedWriter bwDLIDFileSequenceFile;
                bwDLIDFileSequenceFile = new BufferedWriter(new OutputStreamWriter(osDLIDFileSequenceFile));

                for (String[] sDLIDsAndFileNames : alsDLIDsAndFileNames) {
                    String sLine = sDLIDsAndFileNames[DOWNLOAD_ID] + "\t" + sDLIDsAndFileNames[FILE_NAME_AND_EXTENSION] + "\n";
                    bwDLIDFileSequenceFile.write(sLine);
                }
                bwDLIDFileSequenceFile.flush();
                bwDLIDFileSequenceFile.close();
                osDLIDFileSequenceFile.flush();
                osDLIDFileSequenceFile.close();

            }

            long[] lDownloadIDs = new long[allDownloadIDs.size()];
            for(int i = 0; i < allDownloadIDs.size(); i++){
                lDownloadIDs[i] = allDownloadIDs.get(i);
            }

            String sCallerID = "Worker_Import_VideoDownload:doWork()";
            Data dataDownloadPostProcessor = new Data.Builder()
                    .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                    .putString(Worker_DownloadPostProcessing.KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS, sVideoDownloadFolder)
                    .putString(Worker_DownloadPostProcessing.KEY_ARG_RELATIVE_PATH_TO_FOLDER, ciNew.sFolderRelativePath)  //Videos/<Sequence folder>
                    .putInt(Worker_DownloadPostProcessing.KEY_ARG_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_VIDEOS)
                    .putInt(Worker_DownloadPostProcessing.KEY_ARG_VIDEO_TYPE_SINGLE_M3U8, iSingleOrM3U8) //Used to tell if it should search for subfolder.
                    .putString(Worker_DownloadPostProcessing.KEY_ARG_ITEM_ID, ciNew.sItemID) //Used if the type is M3U8 to find subfolder.
                    .putLongArray(Worker_DownloadPostProcessing.KEY_ARG_DOWNLOAD_IDS, lDownloadIDs)
                    .build();
            OneTimeWorkRequest otwrDownloadPostProcessor = new OneTimeWorkRequest.Builder(Worker_DownloadPostProcessing.class)
                    .setInputData(dataDownloadPostProcessor)
                    .addTag(Worker_DownloadPostProcessing.WORKER_TAG_DOWNLOAD_POST_PROCESSING) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(otwrDownloadPostProcessor);

            sMessage = "Operation complete.\n";
            sMessage = sMessage + "The download will continue in the background and will be available once completed.\n";

            //Check to see if the user has applied a restricted tag so that an appropriate reminder
            //  message can be shown:
            for(int iTagID: ciNew.aliTags){
                ItemClass_Tag ict = GlobalClass.gtmApprovedCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).get(iTagID);
                if(ict!=null) {
                    if (ict.iMaturityRating > GlobalClass.giDefaultUserMaturityRating) {
                        sMessage = sMessage + "The item might not be readily shown in the catalog due to the application " +
                                "of a tag with a maturity rating above the default view's rating. Make sure to set the view filter " +
                                "to include items of a higher maturity in order to view this item.\n";
                        break;
                    }
                }
            }

            globalClass.BroadcastProgress(true, sMessage,
                    true, iProgressBarValue,
                    true, "All downloads started",
                    IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);

        } catch (Exception e) {
            if(e.getMessage() != null) {
                Log.e("Error: ", e.getMessage());
            }
            globalClass.BroadcastProgress(true, "Problem encountered:\n" + e.getMessage(),
                    false, iProgressBarValue,
                    true, "Operation halted.",
                    IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
        }

        GlobalClass.gabImportExecutionRunning.set(false);
        GlobalClass.gabImportExecutionFinished.set(true);
        return Result.success();
    }

    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Import_VideoDownload:" + sRoutine, sMessage);
    }

}
