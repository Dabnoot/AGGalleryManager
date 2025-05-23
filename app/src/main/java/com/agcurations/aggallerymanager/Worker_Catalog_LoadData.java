package com.agcurations.aggallerymanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Catalog_LoadData extends Worker {

    public static final String TAG_WORKER_CATALOG_LOADDATA = "com.agcurations.aggallermanager.tag_worker_catalog_loaddata";

    public static final String CATALOG_LOAD_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_LOAD_ACTION_RESPONSE";

    public static final String CATALOG_LOAD_COMPLETE_NOTIFICATION_BOOLEAN = "CATALOG_LOAD_COMPLETE_NOTIFICATION_BOOLEAN";


    String gsResponseActionFilter;
    Context gContext;
    GlobalClass globalClass;

    public Worker_Catalog_LoadData(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gContext = context;
        gsResponseActionFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);


    }

    @NonNull
    @Override
    public Result doWork() {

        StopWatch stopWatch = new StopWatch(false);

        globalClass = (GlobalClass) getApplicationContext();

        GlobalClass.galtsiCatalogViewerFilterTags = new ArrayList<>();
        GlobalClass.galtsiCatalogViewerFilterTags.add(new TreeSet<>()); //Videos
        GlobalClass.galtsiCatalogViewerFilterTags.add(new TreeSet<>()); //Images
        GlobalClass.galtsiCatalogViewerFilterTags.add(new TreeSet<>()); //Comics

        if(GlobalClass.giLoadingState == GlobalClass.LOADING_STATE_STARTED
            || GlobalClass.giLoadingState == GlobalClass.LOADING_STATE_FINISHED){
            return Result.failure();
        }
        GlobalClass.giLoadingState = GlobalClass.LOADING_STATE_STARTED;

        GlobalClass.gsUriAppRootPrefix = GlobalClass.gUriDataFolder.toString();

        //Create Logs folder if it does not exist:
        GlobalClass.gUriLogsFolder = initSubfolder(GlobalClass.gUriDataFolder, "Logs", "Could not create logs folder.");

        //Create Jobs folder if it does not exist:
        GlobalClass.gUriJobFilesFolder = initSubfolder(GlobalClass.gUriDataFolder, "JobFiles", "Could not create JobFiles folder.");

        //Create Backup folder if it does not exist:
        GlobalClass.gUriBackupFolder = initSubfolder(GlobalClass.gUriDataFolder, "Backup", "Could not create backup folder.");

        //Catalog Folder Structure:
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){
            GlobalClass.gUriCatalogFolders[iMediaCategory] = initSubfolder(GlobalClass.gUriDataFolder, GlobalClass.gsCatalogFolderNames[iMediaCategory], "Could not create " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " folder.");

            //Catalog/tag data files:
            if(GlobalClass.gUriCatalogFolders[iMediaCategory] != null){
                //Identify/create the CatalogContents.dat file:
                String sFileName = GlobalClass.gsCatalogFolderNames[iMediaCategory] + "_CatalogContents.dat";
                GlobalClass.gUriCatalogContentsFiles[iMediaCategory] = getDataFileOrCreateIt(GlobalClass.gUriDataFolder, sFileName, "Could not create file " + sFileName + ".");

                //Identify the tags file for the catalog:
                sFileName = GlobalClass.gsCatalogFolderNames[iMediaCategory] + "_Tags.dat";
                GlobalClass.gUriCatalogTagsFiles[iMediaCategory] = getDataFileOrCreateIt(GlobalClass.gUriDataFolder, sFileName, "Could not create file " + sFileName + ".");
            }
        }

        //Detect storage devices to determine if one is SD card:
        ArrayList<Storage> alsStorages = getStorages(getApplicationContext());
        GlobalClass.gtmStorageDeviceNames = new TreeMap<>();
        for(Storage storage: alsStorages){
            GlobalClass.gtmStorageDeviceNames.put(storage.getPath(), storage.sName);
        }

        //Create image downloading temp holding folder if it does not exist:
        //This is to temporarily hold downloaded images. We immediately move them so that the downloadManager
        //  cannot find them and delete them as part of a general "cleanup" thing that it does. It automatically
        //  deletes files that have not been used in a while. Since this is an archiving program,
        //  that behavior is not helpful.
        stopWatch.Reset();
        File[] fAvailableDirs = gContext.getExternalFilesDirs(null);
        if (fAvailableDirs.length >= 1) {
            GlobalClass.gfDownloadExternalStorageFolder = fAvailableDirs[0];
        }
        GlobalClass.gsImageDownloadHoldingFolderTempRPath = File.separator +
                GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_IMAGES] +
                File.separator + GlobalClass.gsImageDownloadHoldingTempFolderName;
        String sDLExternalTempCatFolder = GlobalClass.gfDownloadExternalStorageFolder + File.separator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_IMAGES];
        File fDLExternalTempCatFolder = new File(sDLExternalTempCatFolder);
        if(!fDLExternalTempCatFolder.exists()){
            if(!fDLExternalTempCatFolder.mkdir()){
                String sMessage = "Trouble with find/create file: " + sDLExternalTempCatFolder;
                Log.d("Worker_Catalog_LoadData:getDataFileOrCreateIt", sMessage);
            }
        }
        GlobalClass.gfImageDownloadHoldingFolderTemp = initSubfolder(fDLExternalTempCatFolder,GlobalClass.gsImageDownloadHoldingTempFolderName, "Could not create image download temp holding folder.");
        stopWatch.PostDebugLogAndRestart("Internal folders built/verified with duration ");

        //Create image downloading holding folder if it does not exist:
        //  The user will import these files into the catalog at their leisure.
        GlobalClass.gUriImageDownloadHoldingFolder = initSubfolder(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_IMAGES],GlobalClass.gsImageDownloadHoldingFolderName, "Could not create image download holding folder.");


        //Create folder to hold Browser data:
        GlobalClass.gUriBrowserDataFolder = initSubfolder(GlobalClass.gUriDataFolder, "BrowserData", "Could not create BrowserData folder.");

        //Get file to hold web page tab data:
        GlobalClass.gUriWebpageTabDataFile = getDataFileOrCreateIt(GlobalClass.gUriBrowserDataFolder, GlobalClass.gsBrowserDataFile, "Could not create file WebpageTabData.dat.");

        stopWatch.PostDebugLogAndRestart("External folders built/verified with duration ");

        //Save the application-wide log filename to a preference so that it can be pulled if GlobalClass resets.
        //  This can occur if Android closed the application, but saves the last Activity and the user returns.
        //  We want to record the log location so that data can be written to it.
        GlobalClass.gUriAppLogFile = getDataFileOrCreateIt(GlobalClass.gUriLogsFolder, GlobalClass.gsApplicationLogName, "Could not create Application Log File.");
        //If file size is too big, recreate it:


        /*GlobalClass.CatalogDataFile_UpdateAllRecords_TimeStamps(
                globalClass.gfCatalogContentsFiles[GlobalClass.MEDIA_CATEGORY_COMICS]);*/

        //Fix the tags files so that the tag ID is also jumbled to be in alignment with the storage
        //  method of the catalog files:
        /*for(int i = 0; i < 3; i++){
            globalClass.TagsFile_UpdateAllRecords_JumbleTagID(i);
        }*/

        //Initialize arrays used by catalog analysis here. They are checked during import/delete operations.
        //  Initializing them here saves lines of code in other locations.
        GlobalClass.gtmicf_AllFileItemsInMediaFolder.add(new TreeMap<>()); //Videos
        GlobalClass.gtmicf_AllFileItemsInMediaFolder.add(new TreeMap<>()); //Images
        GlobalClass.gtmicf_AllFileItemsInMediaFolder.add(new TreeMap<>()); //Comics

        //Read tags:
        int iProgressDenominator = 10;
        int iProgressNumerator = 0;
        int iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, "Reading Tags",
                CATALOG_LOAD_ACTION_RESPONSE);

        stopWatch.Reset();
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){
            GlobalClass.gtmCatalogTagReferenceLists.add(InitTagData(iMediaCategory));

            /*//Get tag restrictions preferences:
            //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(gContext);
            Set<String> ssCatalogTagsRestricted = sharedPreferences.getStringSet(GlobalClass.gsRestrictedTagsPreferenceNames[iMediaCategory], null);
            //Attempt to match the restricted tag text IDs from the preferences to the Tag ID:
            if(ssCatalogTagsRestricted != null) {
                for (String sRestrictedTag : ssCatalogTagsRestricted) {
                    Integer iRestrictedTag = Integer.parseInt(sRestrictedTag);
                    for (Map.Entry<Integer, ItemClass_Tag> entry : globalClass.gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                        if (entry.getValue().iTagID.equals(iRestrictedTag)) {
                            //If the restricted tag has been found, mark it as restricted:
                            entry.getValue().bIsRestricted = true;
                        }
                    }
                }
            }*/
            iProgressNumerator++;
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    false, "Reading Tags",
                    CATALOG_LOAD_ACTION_RESPONSE);
        }
        GlobalClass.gabTagsLoaded.set(true);
        globalClass.populateApprovedTags();
        stopWatch.PostDebugLogAndRestart("Tag data initialized/read with duration ");

        //Configure video resolution options:
        //Prepare to list possible item resolutions:
        GlobalClass.gtmVideoResolutions = new TreeMap<>();
        GlobalClass.gtmVideoResolutions.put(0,  240);
        GlobalClass.gtmVideoResolutions.put(1,  360);
        GlobalClass.gtmVideoResolutions.put(2,  480);
        GlobalClass.gtmVideoResolutions.put(3,  720);
        GlobalClass.gtmVideoResolutions.put(4, 1080);
        GlobalClass.gtmVideoResolutions.put(5, 2160);

        iProgressNumerator++;
        iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, "Reading Catalogs",
                CATALOG_LOAD_ACTION_RESPONSE);

        //Read the catalog list files into memory:
        stopWatch.Reset();
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){

            stopWatch.Start();

            Uri uriCatalogFolder = GlobalClass.gUriCatalogFolders[iMediaCategory];
            Uri uriCatalogContentsFile = GlobalClass.gUriCatalogContentsFiles[iMediaCategory];

            if (!GlobalClass.CheckIfFileExists(uriCatalogFolder)) {
                problemNotificationConfig("Catalog data folder does not exist: " + uriCatalogFolder + ".");
                return Result.failure();
            } else {
                stopWatch.PostDebugLogAndRestart("Catalog folder existence confirmed at duration ");
                if(uriCatalogContentsFile == null){
                    return Result.failure();
                }
                TreeMap<String, ItemClass_CatalogItem> tmCatalogItems = new TreeMap<>();

                Long lCatalogContentsFileSize = globalClass.GetFileSize(uriCatalogContentsFile.toString());
                stopWatch.PostDebugLogAndRestart("Catalog contents file size gathered after duration ");

                if(lCatalogContentsFileSize == 0) {

                    OutputStream osCatalogContentsFile = null;
                    try {
                        osCatalogContentsFile = GlobalClass.gcrContentResolver.openOutputStream(uriCatalogContentsFile, "wa"); //Mode wa = write-append. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
                        if(osCatalogContentsFile == null){
                            throw new Exception();
                        }
                        //Write the activity_comic_details_header line to the file:
                        osCatalogContentsFile.write(GlobalClass.getCatalogHeader().getBytes()); //Write the header.
                        osCatalogContentsFile.write("\n".getBytes());

                    } catch (Exception e) {
                        problemNotificationConfig("Problem during Catalog Contents File write:\n" + GlobalClass.GetFileName(uriCatalogContentsFile) + "\n\n" + e.getMessage());
                    } finally {
                        try {
                            if(osCatalogContentsFile != null){
                                osCatalogContentsFile.flush();
                                osCatalogContentsFile.close();
                            }
                        } catch (IOException e) {
                            problemNotificationConfig("Problem during Catalog Contents File flush/close:\n" + GlobalClass.GetFileName(uriCatalogContentsFile) + "\n\n" + e.getMessage());

                        }
                    }
                } else {

                    //Build the internal list of entries.
                    //Read the list of entries and populate the catalog array:
                    //BufferedReader brReader;
                    InputStream isCatalogReader = null;
                    try {

                        //Wait for the catalog file to become available:
                        if(!globalClass.IsCatalogFileAvailability(iMediaCategory)){
                            return Result.failure();
                        }
                        GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(false);

                        isCatalogReader = GlobalClass.gcrContentResolver.openInputStream(uriCatalogContentsFile);
                        stopWatch.PostDebugLogAndRestart("Opening InputStream to catalog file completed at duration ");

                        String sCatalogRecordData;
                        String[] sCatalogRecords = null;

                        if(isCatalogReader != null) {

                            //Don't use InputStream.readAllBytes() if using a version of Java less than 9.
                            //byte[] bytesCatalogData = isCatalogReader.readAllBytes(); //Read all data at once as this is fastest.
                            byte[] bytesCatalogData = GlobalClass.readAllBytes(isCatalogReader);

                            isCatalogReader.close();

                            stopWatch.PostDebugLogAndRestart("All bytes read from catalog file at duration ");
                            sCatalogRecordData = new String(bytesCatalogData);
                            sCatalogRecords = sCatalogRecordData.split("\n");

                            stopWatch.PostDebugLogAndRestart("Catalog file bytes processed at duration ");
                        }

                        GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(true);

                        if(sCatalogRecords == null){
                            continue; //Loop to the next catalog file.
                        }

                        int iProgressNumeratorCatReadLoop = 0;
                        int iProgressDenominatorCatReadLoop = sCatalogRecords.length;
                        iProgressBarValue = Math.round((iProgressNumeratorCatReadLoop / (float) iProgressDenominatorCatReadLoop) * 100);
                        globalClass.BroadcastProgress(false, "",
                                true, iProgressBarValue,
                                true, "Reading data for " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " catalog...",
                                CATALOG_LOAD_ACTION_RESPONSE);


                        ItemClass_CatalogItem ci;
                        String sLine;

                        //Get the version of the catalog file:
                        int iCatalogFileVersion = GlobalClass.giCatalogFileVersion;
                        sLine = sCatalogRecords[0];
                        boolean bUpdateCatalogFileToLatestVersion = false;
                        String[] sHeaderFields = sLine.split("\t");
                        if(sHeaderFields.length > 0) {
                            String sCatalogFileVersionBulk = sHeaderFields[sHeaderFields.length - 1];
                            String[] sCatalogFileVersionBulk2 = sCatalogFileVersionBulk.split(":");

                            if (sCatalogFileVersionBulk2.length == 2) {
                                try {
                                    iCatalogFileVersion = Integer.parseInt(sCatalogFileVersionBulk2[1]);
                                } catch (Exception ignored) {
                                    //Could not parse catalog file version.
                                }
                                if (iCatalogFileVersion != GlobalClass.giCatalogFileVersion) {
                                    bUpdateCatalogFileToLatestVersion = true;
                                }
                            }
                        }

                        for(int i = 1; i < sCatalogRecords.length; i++){
                            sLine = sCatalogRecords[i];
                            if(sLine.equals("")){
                                continue;
                            }
                            ci = GlobalClass.ConvertStringToCatalogItem(sLine, iCatalogFileVersion);

                            if(ci.iSpecialFlag ==  ItemClass_CatalogItem.FLAG_VIDEO_M3U8){
                                if(ci.sThumbnail_File.equals("")){
                                    Log.d("Worker_Catalog_LoadData:readCatalogFileToCatalogItems()", "============= M3U8 FILE DETECTED WITH MISSING THUMBNAIL DEFINITION ============");
                                    Log.d("Worker_Catalog_LoadData:readCatalogFileToCatalogItems()", "============= Come and fix it. ============");
                                }
                            }

                            tmCatalogItems.put(ci.sItemID, ci);

                            //Calculate amounts for use in the Sort/Filter capabilities of the comic viewer:
                            if (ci.lDuration_Milliseconds > GlobalClass.glMaxVideoDurationMS) {
                                GlobalClass.glMaxVideoDurationMS = ci.lDuration_Milliseconds; //For the filter range slider.
                            }
                            if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                                int iMegaPixels = (ci.iHeight * ci.iWidth) / 1000000;
                                if (GlobalClass.giMinImageMegaPixels == -1) {
                                    GlobalClass.giMinImageMegaPixels = iMegaPixels;
                                }
                                if (iMegaPixels < GlobalClass.giMinImageMegaPixels) {
                                    GlobalClass.giMinImageMegaPixels = iMegaPixels;
                                }
                                if (iMegaPixels > GlobalClass.giMaxImageMegaPixels) {
                                    GlobalClass.giMaxImageMegaPixels = iMegaPixels;
                                }
                            } else if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                                int iPageCount = ci.iComicPages;
                                if (GlobalClass.giMinComicPageCount == -1) {
                                    GlobalClass.giMinComicPageCount = iPageCount;
                                }
                                if (iPageCount < GlobalClass.giMinComicPageCount) {
                                    GlobalClass.giMinComicPageCount = iPageCount;
                                }
                                if (iPageCount > GlobalClass.giMaxComicPageCount) {
                                    GlobalClass.giMaxComicPageCount = iPageCount;
                                }
                            }

                            iProgressNumeratorCatReadLoop++;
                            if(iProgressNumeratorCatReadLoop % 100 == 0) {
                                iProgressBarValue = Math.round((iProgressNumeratorCatReadLoop / (float) iProgressDenominatorCatReadLoop) * 100);
                                globalClass.BroadcastProgress(false, "",
                                        true, iProgressBarValue,
                                        true, "Reading data for " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " catalog...",
                                        CATALOG_LOAD_ACTION_RESPONSE);
                            }
                        }

                        //Add the data read from the file to memory:
                        GlobalClass.gtmCatalogLists.add(tmCatalogItems);

                        GlobalClass.gbTagHistogramRequiresUpdate[iMediaCategory] = false;

                        if(bUpdateCatalogFileToLatestVersion){
                            String sMessage = "Updating " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " catalog file to the latest version.";
                            globalClass.CatalogDataFile_UpdateCatalogFile(iMediaCategory, sMessage);
                        }

                    } catch (IOException e) {
                        problemNotificationConfig("Trouble reading CatalogContents.dat: " + GlobalClass.GetFileName(uriCatalogFolder));
                    } finally {
                        if (isCatalogReader != null) {
                            try {
                                isCatalogReader.close();
                            } catch (Exception e) {
                                problemNotificationConfig("Problem during Catalog Contents file reader close:\n" + GlobalClass.GetFileName(uriCatalogFolder) + "\n\n" + e.getMessage());
                            }
                        }
                    }
                    stopWatch.PostDebugLogAndRestart("Processing catalog contents records completed at duration ");
                } //End read of catalog file.


            } //End if catalog media type folder (videos, images, comics) exists

            iProgressNumerator++;
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    false, "Reading Catalogs",
                    CATALOG_LOAD_ACTION_RESPONSE);
            stopWatch.PostDebugLogAndRestart("Catalog data for " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " initialized/read with duration ");

        } //End media categor loop for read of catalogs.

        stopWatch.PostDebugLogAndRestart("Catalog data initialized/read with duration ");


        if(globalClass.connectivityManager == null){
            globalClass.registerNetworkCallback();
            //This lets us check globalClass.isNetworkConnected to see if we are connected to the
            //network;
        }
        stopWatch.PostDebugLogAndRestart("Internet connectivity initialized with duration ");

        //Create the notification channel to be used to display notifications to the user (to notify
        //  the user when things happen with WorkManager when the user has left the app, such as
        //  during file downloads):
        globalClass.notificationChannel = new NotificationChannel(
                GlobalClass.NOTIFICATION_CHANNEL_ID,
                GlobalClass.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);
        globalClass.notificationChannel.setDescription(GlobalClass.NOTIFICATION_CHANNEL_DESCRIPTION);
        globalClass.notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        globalClass.notificationManager = gContext.getSystemService(NotificationManager.class);
        globalClass.notificationManager.createNotificationChannel(globalClass.notificationChannel);
        stopWatch.PostDebugLogAndRestart("Notification channel initialized with duration ");

        iProgressNumerator++;
        iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, "Analyzing storage folder availability...",
                CATALOG_LOAD_ACTION_RESPONSE);

        //Examine catgalog storage folders (quick process):
        for(int iMediaCateogry = 0; iMediaCateogry < 3; iMediaCateogry++) {
            GlobalClass.getAGGMStorageFolderAvailability(iMediaCateogry);
        }


        iProgressNumerator++;
        /*iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, "Updating tags files",
                Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
        globalClass.TagDataFileAddNewField();*/

        //GlobalClass.UpdateAllCatalogItemBasedOnTags();

        /*int iUpdateCount = 0;
        for(int iMediaCateogry = 0; iMediaCateogry < 2; iMediaCateogry++) {
            for (Map.Entry<String, ItemClass_CatalogItem> entry : GlobalClass.gtmCatalogLists.get(iMediaCateogry).entrySet()) {
                String sFileNameOriginal = entry.getValue().sFilename;
                String sFileNameModified =  sFileNameOriginal.trim();
                //String sItemID = entry.getValue().sItemID;
                if(!sFileNameModified.equals(sFileNameOriginal)){
                    entry.getValue().sFilename = sFileNameModified;
                    iUpdateCount++;
                }
            }
        }
        String sMessage = "Updated " + iUpdateCount + " items.";
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, sMessage,
                CATALOG_LOAD_ACTION_RESPONSE);
        globalClass.CatalogDataFile_UpdateCatalogFiles("Re-writing catalog files...");*/

        //VerifyVideoFilesIntegrity();

        //investigateLongFileNames();

        //fixM3U8InternalFilePaths();

        //globalClass.CatalogDataFile_UpdateCatalogFiles("Updating data...");

        //globalClass.verifyCatalogItemsExist(GlobalClass.MEDIA_CATEGORY_VIDEOS, false);

        //globalClass.rectifyRogueComicCatalogItemLocations();

        //globalClass.refolderizeImages();

        //globalClass.deJumbleOrphanedFiles(GlobalClass.MEDIA_CATEGORY_VIDEOS);

        iProgressNumerator++;
        iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, "Data Load Complete",
                CATALOG_LOAD_ACTION_RESPONSE);


        LogFilesMaintenance();
        stopWatch.PostDebugLogAndRestart("Log file maintenance completed with duration ");
        GlobalClass.giLoadingState = GlobalClass.LOADING_STATE_FINISHED;

        //Send a broadcast notifying that the data load operation is complete:
        Intent broadcastIntent_CatalogLoadResponse = new Intent();
        broadcastIntent_CatalogLoadResponse.putExtra(CATALOG_LOAD_COMPLETE_NOTIFICATION_BOOLEAN, true);
        broadcastIntent_CatalogLoadResponse.setAction(CATALOG_LOAD_ACTION_RESPONSE);
        broadcastIntent_CatalogLoadResponse.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_CatalogLoadResponse);

        return Result.success();
    }


    private Uri initSubfolder(Uri uriParentFolder, String sFolderName, String sFailMessage) {

        String sUriChildFolder = uriParentFolder.toString()
                + GlobalClass.gsFileSeparator + sFolderName;
        Uri uriChildFolder = Uri.parse(sUriChildFolder);

        if (GlobalClass.CheckIfFileExists(uriChildFolder)) {
            return uriChildFolder;
        } else {
            try {
                return DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriParentFolder, DocumentsContract.Document.MIME_TYPE_DIR, sFolderName);
            } catch (Exception e){
                return null;
            }

        }


    }

    private File initSubfolder(File fParentFolder, String sFolderName, String sFailMessage){

        File fSubFolder = null;

        try {
            fSubFolder = new File(fParentFolder.getAbsolutePath() + File.separator + sFolderName);

            if (!fSubFolder.exists()) {
                if(!fSubFolder.mkdir()){
                    problemNotificationConfig(sFailMessage);
                }
            }
        } catch (Exception e){
            String sMessage = "Trouble with find/create folder: " + sFolderName;
            Log.d("Worker_Catalog_LoadData:initSubfolder", sMessage);
        }
        return fSubFolder;

    }

    private Uri getDataFileOrCreateIt(Uri uriParentFolder, String sFileName, String sFailMessage){
        Uri uriDataFile = null;

        if(GlobalClass.CheckIfFileExists(uriParentFolder, sFileName)){
            uriDataFile = GlobalClass.FormChildUri(uriParentFolder.toString(), sFileName);
        } else {
            try {
                uriDataFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriParentFolder, GlobalClass.BASE_TYPE_TEXT, sFileName);
                if (uriDataFile == null) {
                    problemNotificationConfig(sFailMessage);
                }
            } catch (Exception e) {
                String sMessage = "Trouble with find/create file: " + sFileName;
                Log.d("Worker_Catalog_LoadData:getDataFileOrCreateIt", sMessage);
            }
        }
        return uriDataFile;
    }


    public static class Storage extends File {

        public static final int INTERNAL_STORAGE = 1;
        public static final int SD_CARD = 2;
        public static final int USB_DRIVE = 3;

        public String sName;
        public int iType;

        public Storage(String path, String sName, int iType) {
            super(path);
            this.sName = sName;
            this.iType = iType;
        }
    }

    public static ArrayList<Storage> getStorages(Context context) {
        //https://stackoverflow.com/questions/41719986/get-all-storages-and-devices-with-their-names-android
        ArrayList<Storage> storages = new ArrayList<>();

        // Internal storage
        storages.add(new Storage(Environment.getExternalStorageDirectory().getPath(),
                "Internal Storage", Storage.INTERNAL_STORAGE));

        // SD Cards
        ArrayList<File> extStorages = new ArrayList<>(Arrays.asList(context.getExternalFilesDirs(null)));
        extStorages.remove(0); // Remove internal storage
        String secondaryStoragePath = System.getenv("SECONDARY_STORAGE");
        for (int i = 0; i < extStorages.size(); i++) {
            String path = extStorages.get(i).getPath().split("/Android")[0];
            if (Environment.isExternalStorageRemovable(extStorages.get(i)) || secondaryStoragePath != null && secondaryStoragePath.contains(path)) {
                String name = "SD Card" + (i == 0 ? "" : " " + (i + 1));
                storages.add(new Storage(path, name, Storage.SD_CARD));
            }
        }

        // USB Drives
        ArrayList<String> drives = new ArrayList<>();
        String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
        String s = "";
        try {
            final Process process = new ProcessBuilder().command("mount")
                    .redirectErrorStream(true).start();
            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte[] buffer = new byte[1024];
            StringBuilder sb = new StringBuilder();
            while (is.read(buffer) != -1) {
                sb.append(new String(buffer));
            }
            s = sb.toString();
            is.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        final String[] lines = s.split("\n");
        for (String line : lines) {
            if (!line.toLowerCase(Locale.US).contains("asec") && line.matches(reg)) {
                String[] parts = line.split(" ");
                for (String path : parts) {
                    if (path.startsWith(File.separator) && !path.toLowerCase(Locale.US).contains("vold")) {
                        drives.add(path);
                    }
                }
            }
        }

        // Remove SD Cards from found drives (already found)
        ArrayList<String> ids = new ArrayList<>();
        for (Storage st : storages) {
            String[] parts = st.getPath().split(File.separator);
            ids.add(parts[parts.length-1]);
        }
        for (int i = drives.size() - 1; i >= 0; i--) {
            String[] parts = drives.get(i).split(File.separator);
            String id = parts[parts.length-1];
            if (ids.contains(id)) drives.remove(i);
        }

        // Get USB Drive name
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        Collection<UsbDevice> dList = usbManager.getDeviceList().values();
        ArrayList<UsbDevice> deviceList = new ArrayList<>(dList);
        for (int i = 0; i < deviceList.size(); i++) {
            storages.add(new Storage(drives.get(i), deviceList.get(i).getProductName(), Storage.USB_DRIVE));
        }

        return storages;
    }


    public TreeMap<Integer, ItemClass_Tag> InitTagData(int iMediaCategory){
        //TreeMap<String, ItemClass_Tag> tmTags = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        TreeMap<Integer, ItemClass_Tag> tmTags = new TreeMap<>();

        Uri uriTagsFile = GlobalClass.gUriCatalogTagsFiles[iMediaCategory];
        if(globalClass.GetFileSize(uriTagsFile.toString()) == 0 ) {
            try {
                OutputStream osTagsFile = GlobalClass.gcrContentResolver.openOutputStream(uriTagsFile, "wt");
                if (osTagsFile == null) {
                    problemNotificationConfig("Trouble writing file at\n" + uriTagsFile);
                    return null;
                }

                //Write the header record:
                osTagsFile.write(GlobalClass.getTagFileHeader().getBytes(StandardCharsets.UTF_8));
                osTagsFile.write("\n".getBytes(StandardCharsets.UTF_8));

                //Close the tags file:
                osTagsFile.flush();
                osTagsFile.close();

            } catch (IOException e) {
                problemNotificationConfig("Trouble writing file at\n" + uriTagsFile + "\n\n" + e.getMessage());
            }
        } else {


            //Get Tags from file:
            BufferedReader brReader;
            InputStream isTagsFile = null;
            try {
                isTagsFile = GlobalClass.gcrContentResolver.openInputStream(uriTagsFile);
                if (isTagsFile == null) {
                    return null;
                }
                brReader = new BufferedReader(new InputStreamReader(isTagsFile));
                brReader.readLine();//First line is the header, skip it.
                String sLine = brReader.readLine();

                while(sLine != null) {

                    ItemClass_Tag ict = GlobalClass.ConvertFileLineToTagItem(sLine);
                    tmTags.put(ict.iTagID, ict);

                    sLine = brReader.readLine();
                }

                brReader.close();

            } catch (IOException e) {
                problemNotificationConfig("Trouble reading tags file at\n" + uriTagsFile + "\n\n" + e.getMessage());
            } finally {
                if(isTagsFile != null){
                    try {
                        isTagsFile.close();
                    } catch (Exception e){
                        Log.d("Worker_Catalog_LoadData:InitTagData", "Could not close tags file.");
                    }
                }
            }
        }

        return tmTags;
    }


    private void LogFilesMaintenance(){

        //todo: is it faster to use DocumentFile or DocumentContract here?
        DocumentFile dfLogsFolder = DocumentFile.fromTreeUri(getApplicationContext(), GlobalClass.gUriLogsFolder);
        if(dfLogsFolder != null){
            if(GlobalClass.CheckIfFileExists(GlobalClass.gUriLogsFolder)){
                DocumentFile[] dfLogFiles = dfLogsFolder.listFiles();
                if(dfLogFiles.length > 0){
                    LocalDate ldNow = LocalDate.now();

                    for(DocumentFile dfLogFile: dfLogFiles) {
                        Date dModifiedDate = new Date(dfLogFile.lastModified());
                        LocalDate ldModifiedDate = dModifiedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        long lDaysBetween = Period.between(ldModifiedDate, ldNow).getDays();
                        //long lDaysBetween = Duration.between(ldModifiedDate, ldNow).toDays();
                        if(lDaysBetween > GlobalClass.giLogFileKeepDurationInDays){
                            if(!dfLogFile.delete()){
                                Log.d("Log File Maintenance", "Could not delete log file " + dfLogFile.getName());
                            }
                        }

                    }
                }
            }
        }






    }

    private int investigateLongFileNames(){
        //Some items stored with long filenames due to using data from web source as file name.
        //  Some file names are so long that they are incompatible with the PC-based OS.
        int iLongestReasonablePath = 0;
        TreeMap<Integer, Integer> tmLengthHistogram = new TreeMap<>();
        for(int i = 0; i < 261; i++){
            tmLengthHistogram.put(i, 0);
        }


        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){

            for(Map.Entry<String, ItemClass_CatalogItem> entry: GlobalClass.gtmCatalogLists.get(iMediaCategory).entrySet()){
                //Determine the full path to the file:
                ItemClass_CatalogItem ci = entry.getValue();
                String sFilePath = GlobalClass.gUriCatalogFolders[iMediaCategory].getPath() + File.separator
                        + ci.sFolderRelativePath + File.separator
                        + ci.sFilename;

                if(ci.sFilename.equals("")){
                    continue;
                }

                if(sFilePath.length() >= 260){
                    continue;
                }

                if(sFilePath.length() > iLongestReasonablePath){
                    iLongestReasonablePath = sFilePath.length();
                }

                Integer iValue = tmLengthHistogram.get(sFilePath.length());
                if(iValue == null) iValue = 0;
                iValue++;
                tmLengthHistogram.put(sFilePath.length(), iValue);

            }

        }

        return iLongestReasonablePath;
    }

    private void fixM3U8InternalFilePaths(){



    }








    void problemNotificationConfig(String sMessage){
        globalClass.problemNotificationConfig(sMessage,
                CATALOG_LOAD_ACTION_RESPONSE);
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Catalog_LoadData:" + sRoutine, sMessage);
    }

}
