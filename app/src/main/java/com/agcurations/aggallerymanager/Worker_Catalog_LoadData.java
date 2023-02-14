package com.agcurations.aggallerymanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.exoplayer2.util.MimeTypes;

public class Worker_Catalog_LoadData extends Worker {

    public static final String TAG_WORKER_CATALOG_LOADDATA = "com.agcurations.aggallermanager.tag_worker_catalog_loaddata";

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

        if(globalClass.giLoadingState == GlobalClass.LOADING_STATE_STARTED
            || globalClass.giLoadingState == GlobalClass.LOADING_STATE_FINISHED){
            return Result.failure();
        }
        globalClass.giLoadingState = GlobalClass.LOADING_STATE_STARTED;



        //Create Logs folder if it does not exist:
        globalClass.gdfLogsFolder = initSubfolder(GlobalClass.gdfDataFolder, "Logs", "Could not create logs folder.");

        //Create Jobs folder if it does not exist:
        globalClass.gdfJobFilesFolder = initSubfolder(GlobalClass.gdfDataFolder, "JobFiles", "Could not create JobFiles folder.");

        //Create Backup folder if it does not exist:
        globalClass.gdfBackupFolder = initSubfolder(GlobalClass.gdfDataFolder, "Backup", "Could not create backup folder.");

        //Catalog Folder Structure:
        for(int i = 0; i < 3; i++){
            globalClass.gdfCatalogFolders[i] = initSubfolder(GlobalClass.gdfDataFolder, GlobalClass.gsCatalogFolderNames[i], "Could not create " + GlobalClass.gsCatalogFolderNames[i] + " folder.");

            //Catalog/tag data files:
            if(globalClass.gdfCatalogFolders[i] != null){
                //Identify/create the CatalogContents.dat file:
                String sFileName = GlobalClass.gsCatalogFolderNames[i] + "_CatalogContents.dat";
                globalClass.gdfCatalogContentsFiles[i] = getDataFileOrCreateIt(GlobalClass.gdfDataFolder, sFileName, "Could not create file " + sFileName + ".");

                //Identify the tags file for the catalog:
                sFileName = GlobalClass.gsCatalogFolderNames[i] + "_Tags.dat";
                globalClass.gdfCatalogTagsFiles[i] = getDataFileOrCreateIt(GlobalClass.gdfDataFolder, sFileName, "Could not create file " + sFileName + ".");
            }
        }


        //With a threshold of items initialized, build a list of all document files for fast look-up:
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataCatalogBuildDocumentUriList = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_Main:onCreate()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrCatalogBuildDocumentUriList = new OneTimeWorkRequest.Builder(Worker_Catalog_BuildDocumentUriList.class)
                .setInputData(dataCatalogBuildDocumentUriList)
                .addTag(Worker_Catalog_BuildDocumentUriList.TAG_WORKER_BUILD_URI_LIST) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(otwrCatalogBuildDocumentUriList);


        //Create image downloading temp holding folder if it does not exist:
        //This is to temporarily hold downloaded images. We immediately move them so that the downloadManager
        //  cannot find them and delete them as part of a general "cleanup" thing that it does. It automatically
        //  deletes files that have not been used in a while. Since this is an archiving program,
        //  that behavior is not helpful.
        stopWatch.Reset();
        File[] fAvailableDirs = gContext.getExternalFilesDirs(null);
        if (fAvailableDirs.length >= 1) {
            globalClass.gfDownloadExternalStorageFolder = fAvailableDirs[0];
        }
        globalClass.gsImageDownloadHoldingFolderTempRPath = File.separator +
                GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_IMAGES] +
                File.separator + "HoldingTemp";
        String sDLExternalTempCatFolder = globalClass.gfDownloadExternalStorageFolder + File.separator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_IMAGES];
        File fDLExternalTempCatFolder = new File(sDLExternalTempCatFolder);
        if(!fDLExternalTempCatFolder.exists()){
            if(!fDLExternalTempCatFolder.mkdir()){
                String sMessage = "Trouble with find/create file: " + sDLExternalTempCatFolder;
                Log.d("Worker_Catalog_LoadData:getDataFileOrCreateIt", sMessage);
            }
        }
        globalClass.gfImageDownloadHoldingFolderTemp = initSubfolder(fDLExternalTempCatFolder,"HoldingTemp", "Could not create image download temp holding folder.");
        stopWatch.PostDebugLogAndRestart("Internal folders built/verified with duration ");

        //Create image downloading holding folder if it does not exist:
        //  The user will import these files into the catalog at their leisure.
        globalClass.gdfImageDownloadHoldingFolder = initSubfolder(globalClass.gdfCatalogFolders[GlobalClass.MEDIA_CATEGORY_IMAGES],"Holding", "Could not create image download holding folder.");


        //Create folder to hold Browser data:
        globalClass.gdfBrowserDataFolder = initSubfolder(GlobalClass.gdfDataFolder, "BrowserData", "Could not create BrowserData folder.");

        //Get file to hold web page tab data:
        globalClass.gdfWebpageTabDataFile = getDataFileOrCreateIt(globalClass.gdfBrowserDataFolder, "WebpageTabData.dat", "Could not create file WebpageTabData.dat.");

        //Create Webpage Favicon folder if it does not exist:
        globalClass.gdfWebpageFaviconBitmapFolder = initSubfolder(GlobalClass.gdfDataFolder, "TempFavicon", "Could not create TempFavicon folder.");

        stopWatch.PostDebugLogAndRestart("External folders built/verified with duration ");

        //Save the application-wide log filename to a preference so that it can be pulled if GlobalClass resets.
        //  This can occur if Android closed the application, but saves the last Activity and the user returns.
        //  We want to record the log location so that data can be written to it.
        String sLogsFolderUri = globalClass.gdfLogsFolder.getUri().toString();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sharedPreferences.edit()
                .putString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, sLogsFolderUri + File.separator + GlobalClass.gsApplicationLogName) //todo: This is likely not appropriate with the switch from File to DocumentFile.
                .apply();

        /*GlobalClass.CatalogDataFile_UpdateAllRecords_TimeStamps(
                globalClass.gfCatalogContentsFiles[GlobalClass.MEDIA_CATEGORY_COMICS]);*/

        //Fix the tags files so that the tag ID is also jumbled to be in alignment with the storage
        //  method of the catalog files:
        /*for(int i = 0; i < 3; i++){
            globalClass.TagsFile_UpdateAllRecords_JumbleTagID(i);
        }*/

        int iProgressDenominator = 10;
        int iProgressNumerator = 0;
        int iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, "Reading Tags",
                Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);

        stopWatch.Reset();
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){
            globalClass.gtmCatalogTagReferenceLists.add(InitTagData(iMediaCategory));

            //Get tag restrictions preferences:
            //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(gContext);
            Set<String> ssCatalogTagsRestricted = sharedPreferences.getStringSet(GlobalClass.gsRestrictedTagsPreferenceNames[iMediaCategory], null);
            //Attempt to match the restricted tag text IDs from the preferences to the Tag ID:
            if(ssCatalogTagsRestricted != null) {
                for (String sRestrictedTag : ssCatalogTagsRestricted) {
                    Integer iRestrictedTag = Integer.parseInt(sRestrictedTag);
                    for (Map.Entry<Integer, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                        if (entry.getValue().iTagID.equals(iRestrictedTag)) {
                            //If the restricted tag has been found, mark it as restricted:
                            entry.getValue().bIsRestricted = true;
                        }
                    }
                }
            }
            iProgressNumerator++;
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    false, "Reading Tags",
                    Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
        }
        stopWatch.PostDebugLogAndRestart("Tag data initialized/read with duration ");

        //Configure video resolution options:
        //Prepare to list possible item resolutions:
        globalClass.gtmVideoResolutions = new TreeMap<>();
        globalClass.gtmVideoResolutions.put(0,  240);
        globalClass.gtmVideoResolutions.put(1,  360);
        globalClass.gtmVideoResolutions.put(2,  480);
        globalClass.gtmVideoResolutions.put(3,  720);
        globalClass.gtmVideoResolutions.put(4, 1080);
        globalClass.gtmVideoResolutions.put(5, 2160);

        //Prep tag histograms:
        //globalClass.galtmTagHistogram = new ArrayList<>();
        //globalClass.galtmTagHistogram.add(new TreeMap<Integer, Integer>()); //Videos
        //globalClass.galtmTagHistogram.add(new TreeMap<Integer, Integer>()); //Images
        //globalClass.galtmTagHistogram.add(new TreeMap<Integer, Integer>()); //Comics

        iProgressNumerator++;
        iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, "Reading Catalogs",
                Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);

        //Read the catalog list files into memory:
        stopWatch.Reset();
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){

            globalClass.gtmCatalogLists.add(readCatalogFileToCatalogItems(iMediaCategory));
            iProgressNumerator++;
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    false, "Reading Catalogs",
                    Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
            stopWatch.PostDebugLogAndRestart("Catalog data for " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " initialized/read with duration ");
        }
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
                true, "Post Processing",
                Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
        //todo: Below item removed as this should now be fully executed by the download post-processor worker
        //globalClass.ExecuteDownloadManagerPostProcessing();

        iProgressNumerator++;
        /*iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, "Updating tags files",
                Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
        globalClass.TagDataFileAddNewField();*/

        //globalClass.CatalogDataFile_AddNewField();

        //VerifyVideoFilesIntegrity();

        //investigateLongFileNames();

        //fixM3U8InternalFilePaths();

        //todo: Create test case here.
        //todo: Use the storage access framework to create a text file of name Bananna.
        //todo: Create a directory of the name Bananna.
        //todo: Use DocumentFile to retrieve DocumentFile of name Bananna.
        //todo: Test if the returned Document file is the text file or the directory.
        //todo: Attempt to retreive the DocumentFile for the other item that was not returned.
        //Todo: This is in response to Worker_LocalFileTransfer todo item near line 360.


        iProgressNumerator++;
        iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
        globalClass.BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, "Data Load Complete",
                Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);


        LogFilesMaintenance();
        stopWatch.PostDebugLogAndRestart("Log file maintenance completed with duration ");
        globalClass.giLoadingState = GlobalClass.LOADING_STATE_FINISHED;

        return Result.success();
    }

    private DocumentFile initSubfolder(DocumentFile dfFolder, String sFolderName, String sFailMessage){

        DocumentFile dfSubFolder = null;

        try {
            dfSubFolder = dfFolder.findFile(sFolderName);
            if (dfSubFolder == null) {
                dfSubFolder = dfFolder.createDirectory(sFolderName);
            }
            if (dfSubFolder == null) {
                problemNotificationConfig(sFailMessage);
            }
        } catch (Exception e){
            String sMessage = "Trouble with find/create file: " + sFolderName;
            Log.d("Worker_Catalog_LoadData:getDataFileOrCreateIt", sMessage);
        }
        return dfSubFolder;

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
            Log.d("Worker_Catalog_LoadData:getDataFileOrCreateIt", sMessage);
        }
        return fSubFolder;

    }

    private DocumentFile getDataFileOrCreateIt(DocumentFile dfFolder, String sFileName, String sFailMessage){
        DocumentFile dfDataFile = null;
        try {
            dfDataFile = dfFolder.findFile(sFileName);

            if (dfDataFile == null) {
                dfDataFile = dfFolder.createFile(MimeTypes.BASE_TYPE_TEXT, sFileName);
            }
            if (dfDataFile == null) {
                problemNotificationConfig(sFailMessage);
            }
        } catch (Exception e){
            String sMessage = "Trouble with find/create file: " + sFileName;
            Log.d("Worker_Catalog_LoadData:getDataFileOrCreateIt", sMessage);
        }
        return dfDataFile;
    }


    static public void initDataFolder(Uri treeUri, Context context){
        DocumentFile dfSelectedFolder = DocumentFile.fromTreeUri(context, treeUri);

        if(dfSelectedFolder == null){
            return;
        }

        String sUserFriendlyFolderPath = dfSelectedFolder.getUri().getPath();
        //Do what you gotta do to reflect back to the user a friendly path that they
        //  selected:
        //https://www.dev2qa.com/how-to-get-real-file-path-from-android-uri/
        Uri uriSelectedFolder = dfSelectedFolder.getUri();

        String uriAuthority = uriSelectedFolder.getAuthority();
        boolean isExternalStoreDoc = "com.android.externalstorage.documents".equals(uriAuthority);
        if(isExternalStoreDoc) {
            //Detect storage devices to determine if one is SD card:
            ArrayList<Storage> alsStorages = getStorages(context);

            String documentId = DocumentsContract.getDocumentId(uriSelectedFolder);

            String[] idArr = documentId.split(":");
            if(idArr.length == 2)
            {
                String type = idArr[0];
                String realDocId = idArr[1];

                String sStoragePrefix = "";
                if("primary".equalsIgnoreCase(type))
                {
                    sStoragePrefix = "Internal storage";
                } else {
                    for (Storage storage : alsStorages) {
                        if (storage.getName().contains(type)) {
                            sStoragePrefix = storage.sName;
                            break;
                        }
                    }
                }
                sUserFriendlyFolderPath = sStoragePrefix + "/" + realDocId;

            }
        }
        //With the user having specified a folder, identify/create the data folder within:
        Uri parentFolderUri = dfSelectedFolder.getUri();
        DocumentFile parentFolder = DocumentFile.fromTreeUri(context, parentFolderUri);
        if(parentFolder == null){
            return;
        }
        DocumentFile dfDataFolder = parentFolder.findFile(GlobalClass.gsDataFolderBaseName);
        if(dfDataFolder == null) {
            boolean bWeAreInTheGalleryFolder = false;
            if(parentFolder.getName() != null) {
                if (parentFolder.getName().equals(GlobalClass.gsDataFolderBaseName)){
                    bWeAreInTheGalleryFolder = true;
                }
            }
            if(!bWeAreInTheGalleryFolder) {
                dfDataFolder = parentFolder.createDirectory(GlobalClass.gsDataFolderBaseName);
            } else {
                dfDataFolder = parentFolder;
            }
        }

        sUserFriendlyFolderPath = sUserFriendlyFolderPath + File.separator + GlobalClass.gsDataFolderBaseName;

        if(dfDataFolder == null){
            Toast.makeText(context, "Unable to create working folder in selected directory.", Toast.LENGTH_LONG).show();
        } else {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            String sDataStorageLocationURI = dfDataFolder.getUri().toString();
            sharedPreferences.edit()
                    .putString(GlobalClass.gsPreferenceName_DataStorageLocation, sDataStorageLocationURI)
                    .apply();
            sharedPreferences.edit()
                    .putString(GlobalClass.gsPreferenceName_DataStorageLocationUF, sUserFriendlyFolderPath)
                    .apply();

            GlobalClass.gdfDataFolder = dfDataFolder;

            //Start the worker found in Worker_Catalog_LoadData:
            Service_Main.startActionLoadData(context);
        }
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
        ArrayList<File> extStorages = new ArrayList<>();
        extStorages.addAll(Arrays.asList(context.getExternalFilesDirs(null)));
        extStorages.remove(0); // Remove internal storage
        String secondaryStoragePath = System.getenv("SECONDARY_STORAGE");
        for (int i = 0; i < extStorages.size(); i++) {
            String path = extStorages.get(i).getPath().split("/Android")[0];
            if (Environment.isExternalStorageRemovable(extStorages.get(i)) || secondaryStoragePath != null && secondaryStoragePath.contains(path)) {
                String name = "SD Card" + (i == 0 ? "" : " " + String.valueOf(i+1));
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
            while (is.read(buffer) != -1) {
                s += new String(buffer);
            }
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
        ArrayList<UsbDevice> deviceList = new ArrayList<>();
        deviceList.addAll(dList);
        for (int i = 0; i < deviceList.size(); i++) {
            storages.add(new Storage(drives.get(i), deviceList.get(i).getProductName(), Storage.USB_DRIVE));
        }

        return storages;
    }


    private void obtainFolderStructureItem(File file){
        if(!file.exists()){
            if(!file.mkdir()){
                problemNotificationConfig("Could not create item at " + file.getAbsolutePath());

            }
        }
    }

    private TreeMap<String, ItemClass_CatalogItem> readCatalogFileToCatalogItems(int iMediaCategory){

        StopWatch stopWatch = new StopWatch(false);
        stopWatch.Start();

        DocumentFile dfCatalogFolder = globalClass.gdfCatalogFolders[iMediaCategory];
        DocumentFile dfCatalogContentsFile = globalClass.gdfCatalogContentsFiles[iMediaCategory];

        if (!dfCatalogFolder.exists()) {
            problemNotificationConfig("Catalog data folder does not exist: " + dfCatalogFolder.getName() + ".");
            return null;
        } else {
            stopWatch.PostDebugLogAndRestart("Catalog folder existence confirmed at duration ");
            if(dfCatalogContentsFile == null){
                return null;
            }
            TreeMap<String, ItemClass_CatalogItem> tmCatalogItems = new TreeMap<>();

            long lCatalogContentsFileSize = getDocumentFileSize(dfCatalogContentsFile);
            stopWatch.PostDebugLogAndRestart("Catalog contents file size gathered after duration ");

            if(lCatalogContentsFileSize == 0) {

                OutputStream osCatalogContentsFile = null;
                try {
                    osCatalogContentsFile = GlobalClass.gcrContentResolver.openOutputStream(dfCatalogContentsFile.getUri(), "wa"); //Mode wa = write-append. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
                    if(osCatalogContentsFile == null){
                        throw new Exception();
                    }
                    //Write the activity_comic_details_header line to the file:
                    osCatalogContentsFile.write(globalClass.getCatalogHeader().getBytes()); //Write the header.
                    osCatalogContentsFile.write("\n".getBytes());

                } catch (Exception e) {
                    problemNotificationConfig("Problem during Catalog Contents File write:\n" + dfCatalogContentsFile.getName() + "\n\n" + e.getMessage());
                } finally {
                    try {
                        if(osCatalogContentsFile != null){
                            osCatalogContentsFile.flush();
                            osCatalogContentsFile.close();
                        }
                    } catch (IOException e) {
                        problemNotificationConfig("Problem during Catalog Contents File flush/close:\n" + dfCatalogContentsFile.getName() + "\n\n" + e.getMessage());

                    }
                }
            } else {

                //Build the internal list of entries.
                //Read the list of entries and populate the catalog array:
                //BufferedReader brReader;
                InputStream isCatalogReader = null;
                try {

                    isCatalogReader = GlobalClass.gcrContentResolver.openInputStream(dfCatalogContentsFile.getUri());
                    stopWatch.PostDebugLogAndRestart("Opening InputStream to catalog file completed at duration ");

                    String sCatalogRecordData;
                    String[] sCatalogRecords = null;

                    if(isCatalogReader != null) {

                        byte[] bytesCatalogData = isCatalogReader.readAllBytes(); //Read all data at once as this is fastest.
                        isCatalogReader.close();

                        stopWatch.PostDebugLogAndRestart("All bytes read from catalog file at duration ");
                        sCatalogRecordData = new String(bytesCatalogData);
                        sCatalogRecords = sCatalogRecordData.split("\n");

                        stopWatch.PostDebugLogAndRestart("Catalog file bytes processed at duration ");
                    }
                    if(sCatalogRecords == null){
                        return tmCatalogItems;
                    }


                    ItemClass_CatalogItem ci;
                    String sLine;
                    for(int i = 1; i < sCatalogRecords.length; i++){
                        sLine = sCatalogRecords[i];
                        if(sLine.equals("")){
                            continue;
                        }
                        ci = GlobalClass.ConvertStringToCatalogItem(sLine);


                        if(ci.iSpecialFlag ==  ItemClass_CatalogItem.FLAG_VIDEO_M3U8){
                            if(ci.sThumbnail_File.equals("")){
                                Log.d("Worker_Catalog_LoadData:readCatalogFileToCatalogItems()", "============= M3U8 FILE DETECTED WITH MISSING THUMBNAIL DEFINITION ============");
                                Log.d("Worker_Catalog_LoadData:readCatalogFileToCatalogItems()", "============= Come and fix it. ============");
                                //Commented code below was previously used to detect if there was a missing thumbnail definition.
                                //  Last used on 2/14/2023.
                                /*iM3U8VideosWithoutThumbnailFile++;
                                DocumentFile dfVideosFolder = globalClass.gdfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS];
                                if(dfVideosFolder != null){
                                    DocumentFile dfVideoTagFolder = dfVideosFolder.findFile(ci.sFolder_Name);
                                    if(dfVideoTagFolder != null){
                                        DocumentFile dfItemFolder = dfVideoTagFolder.findFile(ci.sItemID);
                                        if(dfItemFolder != null){
                                            DocumentFile dfM3U8File = dfItemFolder.findFile(ci.sFilename);
                                            if(dfM3U8File != null){
                                                InputStream isM3U8File = GlobalClass.gcrContentResolver.openInputStream(dfM3U8File.getUri());
                                                if (isM3U8File != null) {
                                                    BufferedReader brReader;
                                                    brReader = new BufferedReader(new InputStreamReader(isM3U8File));
                                                    String sLine2 = brReader.readLine();

                                                    //Get a listing of all of the files in this folder:
                                                    final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(dfItemFolder.getUri(),
                                                            DocumentsContract.getDocumentId(dfItemFolder.getUri()));

                                                    Cursor c = null;
                                                    try {
                                                        ArrayList<String> alsFileNames = new ArrayList<>();
                                                        c = GlobalClass.gcrContentResolver.query(childrenUri, new String[]{
                                                                DocumentsContract.Document.COLUMN_DISPLAY_NAME
                                                        }, null, null, null);
                                                        if (c != null) {
                                                            while (c.moveToNext()) {
                                                                final String sDocumentName = c.getString(0);
                                                                alsFileNames.add(sDocumentName);
                                                            }
                                                        }
                                                        while (sLine2 != null) {
                                                            if (!sLine2.startsWith("#") && sLine2.contains(".st")) {
                                                                if(alsFileNames.contains(sLine2)){
                                                                    ci.sThumbnail_File = sLine2;
                                                                    break;
                                                                }
                                                            }
                                                            // read next line
                                                            sLine2 = brReader.readLine();
                                                        }
                                                        brReader.close();
                                                        isM3U8File.close();

                                                    } catch (Exception e) {
                                                        Log.w("Worker_Catalog_LoadData:readCatalogFileToCatalogItems()", "Failed query: " + e);
                                                    } finally {
                                                        if (c != null) {
                                                            c.close();
                                                        }
                                                    }






                                                }
                                            }
                                        }
                                    }
                                }
*/
                            }
                        }



                        tmCatalogItems.put(ci.sItemID, ci);

                        //Calculate amounts for use in the Sort/Filter capabilities of the comic viewer:
                        if (ci.lDuration_Milliseconds > globalClass.glMaxVideoDurationMS) {
                            globalClass.glMaxVideoDurationMS = ci.lDuration_Milliseconds; //For the filter range slider.
                        }
                        if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                            int iMegaPixels = (ci.iHeight * ci.iWidth) / 1000000;
                            if (globalClass.giMinImageMegaPixels == -1) {
                                globalClass.giMinImageMegaPixels = iMegaPixels;
                            }
                            if (iMegaPixels < globalClass.giMinImageMegaPixels) {
                                globalClass.giMinImageMegaPixels = iMegaPixels;
                            }
                            if (iMegaPixels > globalClass.giMaxImageMegaPixels) {
                                globalClass.giMaxImageMegaPixels = iMegaPixels;
                            }
                        } else if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                            int iPageCount = ci.iComicPages;
                            if (globalClass.giMinComicPageCount == -1) {
                                globalClass.giMinComicPageCount = iPageCount;
                            }
                            if (iPageCount < globalClass.giMinComicPageCount) {
                                globalClass.giMinComicPageCount = iPageCount;
                            }
                            if (iPageCount > globalClass.giMaxComicPageCount) {
                                globalClass.giMaxComicPageCount = iPageCount;
                            }
                        }

                        //Update the tags histogram. As of 7/29/2022, this is used to show the user
                        //  how many tags are in use while they select tags to perform a tag filter.
                        for (int iCatalogItemTagID : ci.aliTags) {
                            if (globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).get(iCatalogItemTagID) != null) {
                                Objects.requireNonNull(globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).get(iCatalogItemTagID)).iHistogramCount++;
                            }
                        }

                    }


                } catch (IOException e) {
                    problemNotificationConfig("Trouble reading CatalogContents.dat: " + dfCatalogFolder.getName());
                } finally {
                    if (isCatalogReader != null) {
                        try {
                            isCatalogReader.close();
                        } catch (Exception e) {
                            problemNotificationConfig("Problem during Catalog Contents file reader close:\n" + dfCatalogFolder.getName() + "\n\n" + e.getMessage());
                        }
                    }
                }
                stopWatch.PostDebugLogAndRestart("Processing catalog contents records and cross-referencing tags completed at duration ");
            }
            globalClass.gbTagHistogramRequiresUpdate[iMediaCategory] = false;


            //Return the data read from the file:
            return tmCatalogItems;

        }

    }

    public TreeMap<Integer, ItemClass_Tag> InitTagData(int iMediaCategory){
        //TreeMap<String, ItemClass_Tag> tmTags = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        TreeMap<Integer, ItemClass_Tag> tmTags = new TreeMap<>();

        DocumentFile dfTagsFile = globalClass.gdfCatalogTagsFiles[iMediaCategory];
        if(getDocumentFileSize(dfTagsFile) == 0 ) {
            try {
                OutputStream osTagsFile = GlobalClass.gcrContentResolver.openOutputStream(dfTagsFile.getUri(), "wt");
                if (osTagsFile == null) {
                    problemNotificationConfig("Trouble writing file at\n" + dfTagsFile.getUri());
                    return null;
                }

                //Write the header record:
                osTagsFile.write(GlobalClass.getTagFileHeader().getBytes(StandardCharsets.UTF_8));
                osTagsFile.write("\n".getBytes(StandardCharsets.UTF_8));

                //Close the tags file:
                osTagsFile.flush();
                osTagsFile.close();

            } catch (IOException e) {
                problemNotificationConfig("Trouble writing file at\n" + dfTagsFile.getUri() + "\n\n" + e.getMessage());
            }
        } else {


            //Get Tags from file:
            BufferedReader brReader;
            InputStream isTagsFile = null;
            try {
                isTagsFile = GlobalClass.gcrContentResolver.openInputStream(dfTagsFile.getUri());
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
                problemNotificationConfig("Trouble reading tags file at\n" + dfTagsFile.getUri() + "\n\n" + e.getMessage());
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


    private long getDocumentFileSize(DocumentFile documentFile){

        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        Cursor returnCursor = contentResolver.query(documentFile.getUri(), null, null, null, null);
        if(returnCursor == null){
            return -1;
        }
        /*
         * Get the column indexes of the data in the Cursor,
         * move to the first row in the Cursor, get the data,
         * and display it.
         */
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        long lSize = returnCursor.getLong(sizeIndex);
        returnCursor.close();
        return lSize;
    }

    private void VerifyVideoFilesIntegrity(){
        ContentResolver contentResolver = GlobalClass.gcrContentResolver;

        boolean bUpdateVideoCatalogFile = false;
        int iItemsWithIssuesCounter = 0;

        OutputStream osLogFile = null;
        BufferedWriter bwLogFile = null;

        String sLogFileName = GlobalClass.GetTimeStampFileSafe() + "_VideoFilesCheck.txt";
        DocumentFile dfLog = globalClass.gdfLogsFolder.createFile(MimeTypes.BASE_TYPE_TEXT, sLogFileName);
        if(dfLog == null){
            problemNotificationConfig("Problem creating log file during video file integrity check.");
            return;
        }

        for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).entrySet()){
            ItemClass_CatalogItem ci = tmEntry.getValue();
            if((ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8)
                    && ((ci.iAllVideoSegmentFilesDetected == ItemClass_CatalogItem.VIDEO_SEGMENT_FILES_UNDETERMINED)
                    || (ci.iAllVideoSegmentFilesDetected == ItemClass_CatalogItem.VIDEO_SEGMENT_FILES_KNOWN_INCOMPLETE))){
                //If this is an M3U8 file check to see if all of the segment files are in place.
                String sMessage = "Examining M3U8 video ID " + tmEntry.getKey();
                Log.d("VideoFilesCheck", sMessage);
                String sM3U8FilePath = globalClass.gdfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS] +
                        File.separator + ci.sFolder_Name +
                        File.separator + ci.sItemID +
                        File.separator + ci.sFilename;
                DocumentFile dfVideoSubfolder = globalClass.gdfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].findFile(ci.sFolder_Name);
                if(dfVideoSubfolder == null){
                    continue;
                }
                DocumentFile dfItemFolder = dfVideoSubfolder.findFile(ci.sItemID);
                if(dfItemFolder == null){
                    continue;
                }
                DocumentFile dfM3U8File = dfItemFolder.findFile(ci.sFilename);
                if(dfM3U8File != null) {
                    //Get data from file:
                    InputStream isM3U8 = null;
                    BufferedReader brReader;
                    try {
                        isM3U8 = contentResolver.openInputStream(dfM3U8File.getUri());
                        brReader = new BufferedReader(new InputStreamReader(isM3U8));
                        String sLine = brReader.readLine();
                        ArrayList<String> alsVideoSequenceFilePaths = new ArrayList<>();

                        while (sLine != null) {
                            if (sLine.startsWith("/")) {
                                alsVideoSequenceFilePaths.add(sLine);
                            }
                            sLine = brReader.readLine();
                        }
                        brReader.close();

                        ArrayList<String> alsVideoFileSequenceFileNames = new ArrayList<>();
                        for(String sFilePath: alsVideoSequenceFilePaths){
                            int iLastIndexOfForwardSlash = sFilePath.lastIndexOf("/") + 1;
                            if(iLastIndexOfForwardSlash > 0 && iLastIndexOfForwardSlash < sFilePath.length()) {
                                String sFileNameListing = sFilePath.substring(sFilePath.lastIndexOf("/") + 1);
                                alsVideoFileSequenceFileNames.add(sFileNameListing);
                            }
                        }



                        //Get a listing of the files in the folder:
                        String sM3U8FolderPath = globalClass.gdfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS] +
                                File.separator + ci.sFolder_Name +
                                File.separator + ci.sItemID;
                        File fM3U8Folder = new File(sM3U8FolderPath);
                        //Compare file names to see if all files in the listing exist:
                        boolean bAllFilesFound = false;
                        if(alsVideoSequenceFilePaths.size() == 0){
                            bAllFilesFound = true;
                        } else {
                            if (fM3U8Folder.exists()) {
                                File[] fFiles = fM3U8Folder.listFiles();
                                if (fFiles != null) {
                                    if(fFiles.length > 0) {
                                        ArrayList<String> alsFilesInFolder = new ArrayList<>();
                                        for (File fFile : fFiles) {
                                            alsFilesInFolder.add(fFile.getName());
                                        }

                                        bAllFilesFound = true;
                                        for (String sM3U8ListedFile : alsVideoFileSequenceFileNames) {
                                            if (!alsFilesInFolder.contains(sM3U8ListedFile)) {
                                                bAllFilesFound = false;
                                                if(bwLogFile == null){
                                                    try {
                                                        osLogFile = contentResolver.openOutputStream(dfLog.getUri(), "wa"); //Mode wa = write-append. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
                                                        if(osLogFile == null){
                                                            throw new Exception();
                                                        }
                                                        bwLogFile = new BufferedWriter(new OutputStreamWriter(osLogFile));
                                                    } catch (Exception e) {
                                                        problemNotificationConfig("Problem during log file write:\n" + dfLog.getName() + "\n\n" + e.getMessage());
                                                    }
                                                }
                                                if(bwLogFile != null) {
                                                    sMessage = "Video ID " + tmEntry.getKey() + " is missing one or more video segment files.";
                                                    bwLogFile.write(sMessage + "\n");
                                                    bwLogFile.flush();
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if(bAllFilesFound){
                            ci.iAllVideoSegmentFilesDetected = ItemClass_CatalogItem.VIDEO_SEGMENT_FILES_KNOWN_COMPLETE;
                            bUpdateVideoCatalogFile = true;
                        } else {
                            ci.iAllVideoSegmentFilesDetected = ItemClass_CatalogItem.VIDEO_SEGMENT_FILES_KNOWN_INCOMPLETE;
                            bUpdateVideoCatalogFile = true;
                            iItemsWithIssuesCounter++;
                        }

                    } catch (IOException e) {
                        sMessage = e.getMessage() + "";
                        Log.d("VideoFilesCheck", sMessage);
                    } finally {
                        if(isM3U8 != null){
                            try {
                                isM3U8.close();
                            } catch (Exception e){
                                Log.d("VideoFilesCheck", "Could not close M3U8 during integrity check.");
                            }
                        }
                    }

                } else {
                    sMessage = "Cannot find M3U8 file: " + sM3U8FilePath;
                    Log.d("VideoFilesCheck", sMessage);
                }




            }

        } //End for loop going through Catalog items.

        if(bUpdateVideoCatalogFile){
            globalClass.WriteCatalogDataFile(GlobalClass.MEDIA_CATEGORY_VIDEOS);
            String sMessage = "Finished M3U8 video integrity check. " +
                    iItemsWithIssuesCounter + " video items have missing files.";

            //Toast.makeText(this, sMessage , Toast.LENGTH_SHORT).show();

            try {
                if (bwLogFile != null) {
                    bwLogFile.write(sMessage + "\n");
                    bwLogFile.flush();
                }
            } catch (Exception e){
                sMessage = e.getMessage() + "";
                Log.d("VideoFilesCheck", sMessage);
            }
        }

        try {
            if(osLogFile != null){
                osLogFile.flush();
                osLogFile.close();
            }
        } catch (IOException e) {
            problemNotificationConfig("Problem during log file flush/close:\n" + dfLog.getName() + "\n\n" + e.getMessage());

        }

    }

    private void LogFilesMaintenance(){

        DocumentFile[] dfLogFiles;
        if(globalClass.gdfLogsFolder != null){
            if(globalClass.gdfLogsFolder.exists()){
                dfLogFiles = globalClass.gdfLogsFolder.listFiles();
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

    private void investigateLongFileNames(){
        //Some items stored with long filenames due to using data from web source as file name.
        //  Some file names are so long that they are incompatible with the PC-based OS.
        int iLongestReasonablePath = 0;
        TreeMap<Integer, Integer> tmLengthHistogram = new TreeMap<>();
        for(int i = 0; i < 261; i++){
            tmLengthHistogram.put(i, 0);
        }


        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){

            for(Map.Entry<String, ItemClass_CatalogItem> entry: globalClass.gtmCatalogLists.get(iMediaCategory).entrySet()){
                //Determine the full path to the file:
                ItemClass_CatalogItem ci = entry.getValue();
                String sFilePath = globalClass.gdfCatalogFolders[iMediaCategory].getUri().getPath() + File.separator
                        + ci.sFolder_Name + File.separator
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
                iValue++;
                tmLengthHistogram.put(sFilePath.length(), iValue);

            }

        }

        String sMessage = "" + iLongestReasonablePath;

    }

    private void fixM3U8InternalFilePaths(){
        //I had coded the M3U8 text files to have full paths to segment files, but it should be
        //  relative. The segment files can't be found after copying all of the data over to an
        //  upgraded SD card because the Android storage device name changes in the path.

        for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).entrySet()){
            ItemClass_CatalogItem ci = tmEntry.getValue();
            if(ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8){
                //If this is an M3U8 file catalog item, locate the M3U8 file and update file paths.
                String sMessage = "Examining M3U8 video ID " + tmEntry.getKey();
                Log.d("VideoFilesCheck", sMessage);
                String sM3U8FilePath = globalClass.gdfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS] +
                        File.separator + ci.sFolder_Name +
                        File.separator + ci.sItemID +
                        File.separator + ci.sFilename;
                File fM3U8File = new File(sM3U8FilePath);
                if(fM3U8File.exists()) {
                    StringBuilder sbBuffer = new StringBuilder();
                    //Get data from file:
                    BufferedReader brReader;
                    try {
                        brReader = new BufferedReader(new FileReader(fM3U8File.getAbsolutePath()));
                        String sLine = brReader.readLine();

                        while (sLine != null) {
                            if (sLine.contains("/storage/3966-3438")) {
                                sLine = sLine.substring(sLine.lastIndexOf("/") + 1);
                            }
                            sbBuffer.append(sLine);
                            sbBuffer.append("\n");
                            sLine = brReader.readLine();
                        }
                        brReader.close();

                        //Re-write the CatalogContentsFile without the deleted item's data record:
                        FileWriter fwNewM3U8File = new FileWriter(fM3U8File, false);
                        fwNewM3U8File.write(sbBuffer.toString());
                        fwNewM3U8File.flush();
                        fwNewM3U8File.close();

                    } catch (IOException e) {
                        sMessage = e.getMessage() + "";
                        Log.d("VideoFilesCheck", sMessage);
                    }

                } else {
                    sMessage = "Cannot find M3U8 file: " + sM3U8FilePath;
                    Log.d("VideoFilesCheck", sMessage);
                }




            }

        } //End for loop going through Catalog items.


    }

    void problemNotificationConfig(String sMessage){
        globalClass.problemNotificationConfig(sMessage,
                Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
    }

}
