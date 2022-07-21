package com.agcurations.aggallerymanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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
        globalClass = (GlobalClass) getApplicationContext();
        String sExternalStorageState;
        sExternalStorageState = Environment.getExternalStorageState();
        if (sExternalStorageState.equals(Environment.MEDIA_MOUNTED) ) {

            File[] fAvailableDirs = gContext.getExternalFilesDirs(null);
            if (fAvailableDirs.length == 2) {
                globalClass.gfAppFolder = fAvailableDirs[1];
            } else {
                globalClass.gfAppFolder = fAvailableDirs[0];
            }

            //Create Logs folder if it does not exist:
            String sLogsFolderPath = globalClass.gfAppFolder + File.separator + "Logs";
            globalClass.gfLogsFolder = new File(sLogsFolderPath);
            if(!globalClass.gfLogsFolder.exists()){
                if(!globalClass.gfLogsFolder.mkdir()){
                    globalClass.problemNotificationConfig("Could not create logs folder.",
                            Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
                }
            }

            //Create Jobs folder if it does not exist:
            String sJobFilesFolderPath = globalClass.gfAppFolder + File.separator + "JobFiles";
            globalClass.gfJobFilesFolder = new File(sJobFilesFolderPath);
            if(!globalClass.gfJobFilesFolder.exists()){
                if(!globalClass.gfJobFilesFolder.mkdir()){
                    problemNotificationConfig("Could not create JobFiles folder.");
                }
            }

            //Create Backup folder if it does not exist:
            String sBackupFolderPath = globalClass.gfAppFolder + File.separator + "Backup";
            File fBackupFolderPath = new File(sBackupFolderPath);
            if(!fBackupFolderPath.exists()){
                if(!fBackupFolderPath.mkdir()){
                    problemNotificationConfig("Could not create backup folder.");
                }
            }

            //Create image downloading temp holding folder if it does not exist:

            globalClass.gsImageDownloadHoldingFolderTempRPath = File.separator +
                    GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_IMAGES] +
                    File.separator + "HoldingTemp";
            String sImageDownloadHoldingFolderTemp = globalClass.gfAppFolder +
                    globalClass.gsImageDownloadHoldingFolderTempRPath;
            globalClass.gfImageDownloadHoldingFolderTemp = new File(sImageDownloadHoldingFolderTemp);
            if(!globalClass.gfImageDownloadHoldingFolderTemp.exists()){
                if(!globalClass.gfImageDownloadHoldingFolderTemp.mkdir()){
                    problemNotificationConfig("Could not create image download temp holding folder.");
                }
            }

            //Create image downloading holding folder if it does not exist:
            String sImageDownloadHoldingFolder = globalClass.gfAppFolder + File.separator +
                    GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_IMAGES] +
                    File.separator + "Holding";
            globalClass.gfImageDownloadHoldingFolder = new File(sImageDownloadHoldingFolder);
            if(!globalClass.gfImageDownloadHoldingFolder.exists()){
                if(!globalClass.gfImageDownloadHoldingFolder.mkdir()){
                    problemNotificationConfig("Could not create image download holding folder.");
                }
            }

            /*File[] files = globalClass.gfAppFolder.listFiles();
            if(files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        if (f.getName().contains("9999")) {
                            f.delete();
                        }
                    }
                }
            }*/

            //Create folder to hold Browser data:
            String sBrowserDataFolderName = globalClass.gfAppFolder.getPath() + File.separator + "BrowserData";
            globalClass.gfBrowserDataFolder = new File(sBrowserDataFolderName);
            if(!globalClass.gfBrowserDataFolder.exists()){
                if(!globalClass.gfBrowserDataFolder.mkdir()){
                    problemNotificationConfig("Could not create BrowserData folder.");
                }
            }

            //Get file to hold web page tab data:
            String sWebPageTabDataFilePath = globalClass.gfBrowserDataFolder.getPath() + File.separator + "WebpageTabData.dat";
            globalClass.gfWebpageTabDataFile = new File(sWebPageTabDataFilePath);

            //Create Webpage Favicon folder if it does not exist:
            String sWebpageFaviconBitmapFolder = globalClass.gfBrowserDataFolder + File.separator + "TempFavicon";
            globalClass.gfWebpageFaviconBitmapFolder = new File(sWebpageFaviconBitmapFolder);
            if(!globalClass.gfWebpageFaviconBitmapFolder.exists()){
                if(!globalClass.gfWebpageFaviconBitmapFolder.mkdir()){
                    problemNotificationConfig("Could not create TempFavicon folder.");
                }
            }

            //Save the application-wide log filename to a preference so that it can be pulled if GlobalClass resets.
            //  This can occur if Android closed the application, but saves the last Activity and the user returns.
            //  We want to record the log location so that data can be written to it.
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            sharedPreferences.edit()
                    .putString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, sLogsFolderPath + File.separator + GlobalClass.gsApplicationLogName)
                    .apply();

            //Catalog Folder Structure:
            for(int i = 0; i < 3; i++){
                globalClass.gfCatalogFolders[i] = new File(globalClass.gfAppFolder + File.separator + GlobalClass.gsCatalogFolderNames[i]);
                obtainFolderStructureItem(globalClass.gfCatalogFolders[i]);
                //Identify the CatalogContents.dat file:
                /*globalClass.gfCatalogContentsFiles[i] = new File(globalClass.gfCatalogFolders[i].getAbsolutePath()
                        + File.separator + "CatalogContents.dat");*/
                globalClass.gfCatalogContentsFiles[i] = new File(globalClass.gfAppFolder + File.separator
                        + GlobalClass.gsCatalogFolderNames[i] + "_CatalogContents.dat");


                //Identify the Logs folder for the catalog:
                /*globalClass.gfCatalogLogsFolders[i] = new File(globalClass.gfCatalogFolders[i]
                        + File.separator + "Logs");*/


                //obtainFolderStructureItem(globalClass.gfCatalogLogsFolders[i]);

                //Identify the tags file for the catalog:
                /*globalClass.gfCatalogTagsFiles[i] = new File(globalClass.gfCatalogFolders[i].getAbsolutePath()
                        + File.separator + "Tags.dat");*/
                globalClass.gfCatalogTagsFiles[i] = new File(globalClass.gfAppFolder + File.separator
                        + GlobalClass.gsCatalogFolderNames[i] + "_Tags.dat");
            }

            //Attempt to read a pin number set by the user:
            globalClass.gsPin = sharedPreferences.getString(GlobalClass.gsPinPreference, "");

        }

        /*GlobalClass.CatalogDataFile_UpdateAllRecords_TimeStamps(
                globalClass.gfCatalogContentsFiles[GlobalClass.MEDIA_CATEGORY_COMICS]);*/

        //Fix the tags files so that the tag ID is also jumbled to be in alignment with the storage
        //  method of the catalog files:
        /*for(int i = 0; i < 3; i++){
            globalClass.TagsFile_UpdateAllRecords_JumbleTagID(i);
        }*/


        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){
            globalClass.gtmCatalogTagReferenceLists.add(globalClass.InitTagData(iMediaCategory));

            //Get tag restrictions preferences:
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(gContext);
            Set<String> ssCatalogTagsRestricted = sharedPreferences.getStringSet(GlobalClass.gsRestrictedTagsPreferenceNames[iMediaCategory], null);
            //Attempt to match the restricted tag text IDs from the preferences to the Tag ID:
            if(ssCatalogTagsRestricted != null) {
                for (String sRestrictedTag : ssCatalogTagsRestricted) {
                    Integer iRestrictedTag = Integer.parseInt(sRestrictedTag);
                    for (Map.Entry<String, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                        if (entry.getValue().iTagID.equals(iRestrictedTag)) {
                            //If the restricted tag has been found, mark it as restricted:
                            entry.getValue().bIsRestricted = true;
                        }
                    }
                }
            }
        }

        //Read the catalog list files into memory:
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){
            globalClass.gtmCatalogLists.add(readCatalogFileToCatalogItems(iMediaCategory));
        }

        if(globalClass.connectivityManager == null){
            globalClass.registerNetworkCallback();
            //This lets us check globalClass.isNetworkConnected to see if we are connected to the
            //network;
        }

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


        globalClass.ExecuteDownloadManagerPostProcessing();

        //globalClass.CatalogDataFile_AddNewField();

        //VerifyVideoFilesIntegrity();

        LogFilesMaintenance();
        return Result.success();
    }

    private void obtainFolderStructureItem(File file){
        if(!file.exists()){
            if(!file.mkdir()){
                problemNotificationConfig("Could not create item at " + file.getAbsolutePath());

            }
        }
    }

    private TreeMap<String, ItemClass_CatalogItem> readCatalogFileToCatalogItems(int iMediaCategory){

        File fCatalogFolder = globalClass.gfCatalogFolders[iMediaCategory];
        File fCatalogContentsFile = globalClass.gfCatalogContentsFiles[iMediaCategory];
        //File fLogsFolder = globalClass.gfCatalogLogsFolders[iMediaCategory];

        boolean bFolderOk = false ;
        if(!fCatalogFolder.exists()) {
            if (fCatalogFolder.mkdirs()) {
                bFolderOk = true;
            }
        }else{
            bFolderOk = true;
        }

        if (!bFolderOk) {
            problemNotificationConfig("Could not create catalog data folder " + fCatalogFolder.getPath() + ".");
            return null;
        } else {

            if (!fCatalogContentsFile.exists()){
                try {
                    if(fCatalogContentsFile.createNewFile()) {
                        FileWriter fwCatalogContentsFile = null;
                        try {
                            fwCatalogContentsFile = new FileWriter(fCatalogContentsFile, true);

                            //Write the activity_comic_details_header line to the file:
                            fwCatalogContentsFile.append(globalClass.getCatalogHeader()); //Write the header.
                            fwCatalogContentsFile.append("\n");

                        } catch (Exception e) {
                            problemNotificationConfig("Problem during Catalog Contents File write:\n" + fCatalogContentsFile.getPath() + "\n\n" + e.getMessage());
                        } finally {
                            try {
                                if (fwCatalogContentsFile != null) {
                                    fwCatalogContentsFile.flush();
                                    fwCatalogContentsFile.close();
                                }
                            } catch (IOException e) {
                                problemNotificationConfig("Problem during Catalog Contents File flush/close:\n" + fCatalogContentsFile.getPath() + "\n\n" + e.getMessage());

                            }
                        }
                    } else {
                        problemNotificationConfig("Could not create CatalogContents.dat at" + fCatalogContentsFile.getAbsolutePath());
                    }
                }catch (IOException e){
                    problemNotificationConfig("Problem creating CatalogContents.dat at" + fCatalogContentsFile.getAbsolutePath());

                }
            }

            /*//Look for the Logs folder. If it does not exist, create it.
            if(!fLogsFolder.exists()) {
                if(!fLogsFolder.mkdirs()){
                    problemNotificationConfig("Could not create log folder at" + fLogsFolder.getAbsolutePath());
                }
            }*/

            //Build the internal list of entries:
            TreeMap<String, ItemClass_CatalogItem> tmCatalogItems = new TreeMap<>();

            //Read the list of entries and populate the catalog array:
            BufferedReader brReader;
            try {
                brReader = new BufferedReader(new FileReader(fCatalogContentsFile.getAbsolutePath()));
                brReader.readLine(); //The first line is the header. Skip this line.
                String sLine = brReader.readLine();

                ItemClass_CatalogItem ci;
                while (sLine != null) {
                    ci = GlobalClass.ConvertStringToCatalogItem(sLine);
                    tmCatalogItems.put(ci.sItemID, ci);

                    // read next line
                    sLine = brReader.readLine();
                }
                brReader.close();

            } catch (IOException e) {
                problemNotificationConfig("Trouble reading CatalogContents.dat at" + fCatalogContentsFile.getAbsolutePath());
            }





            //Return the data read from the file:
            return tmCatalogItems;

        }

    }

    private void VerifyVideoFilesIntegrity(){
        boolean bUpdateVideoCatalogFile = false;
        int iItemsWithIssuesCounter = 0;
        FileWriter fwLogFile = null;
        String sLogFilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                File.separator + GlobalClass.GetTimeStampFileSafe() + "_VideoFilesCheck.txt";
        File fLog = new File(sLogFilePath);
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).entrySet()){
            ItemClass_CatalogItem ci = tmEntry.getValue();
            if((ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8)
                    && ((ci.iAllVideoSegmentFilesDetected == ItemClass_CatalogItem.VIDEO_SEGMENT_FILES_UNDETERMINED)
                    || (ci.iAllVideoSegmentFilesDetected == ItemClass_CatalogItem.VIDEO_SEGMENT_FILES_KNOWN_INCOMPLETE))){
                //If this is an M3U8 file check to see if all of the segment files are in place.
                String sMessage = "Examining M3U8 video ID " + tmEntry.getKey();
                Log.d("VideoFilesCheck", sMessage);
                String sM3U8FilePath = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS] +
                        File.separator + ci.sFolder_Name +
                        File.separator + ci.sItemID +
                        File.separator + ci.sFilename;
                File fM3U8File = new File(sM3U8FilePath);
                if(fM3U8File.exists()) {
                    //Get data from file:
                    BufferedReader brReader;
                    try {
                        brReader = new BufferedReader(new FileReader(fM3U8File.getAbsolutePath()));
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
                        String sM3U8FolderPath = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS] +
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
                                                if(fwLogFile == null){
                                                    fwLogFile = new FileWriter(fLog, true);
                                                }
                                                sMessage = "Video ID " + tmEntry.getKey() + " is missing one or more video segment files.";
                                                fwLogFile.write(sMessage + "\n");
                                                fwLogFile.flush();
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
                if (fwLogFile != null) {
                    fwLogFile.write(sMessage + "\n");
                    fwLogFile.flush();
                }
            } catch (Exception e){
                sMessage = e.getMessage() + "";
                Log.d("VideoFilesCheck", sMessage);
            }
        }

    }

    private void LogFilesMaintenance(){
        String sLogFilePath = globalClass.gfLogsFolder.getAbsolutePath();
        File fLogsFile = new File(sLogFilePath);
        if(fLogsFile.exists()){
            File[] fLogsFiles = fLogsFile.listFiles();
            if(fLogsFiles != null){
                if (fLogsFiles.length > 0) {
                    //Go through the logs files and automatically delete files that are there for a period greater than the
                    //  "LogFilesHoldDuration".
                    LocalDate ldNow = LocalDate.now();

                    for(File fLogFile: fLogsFiles) {
                        Date dModifiedDate = new Date(fLogFile.lastModified());
                        LocalDate ldModifiedDate = dModifiedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        long lDaysBetween = Period.between(ldModifiedDate, ldNow).getDays();
                        //long lDaysBetween = Duration.between(ldModifiedDate, ldNow).toDays();
                        if(lDaysBetween > GlobalClass.giLogFileKeepDurationInDays){
                            if(!fLogFile.delete()){
                                Log.d("Log File Maintenance", "Could not delete log file " + fLogFile.getName());
                            }
                        }

                    }

                }
            }
        }



    }


    void problemNotificationConfig(String sMessage){
        globalClass.problemNotificationConfig(sMessage,
                Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
    }

}
