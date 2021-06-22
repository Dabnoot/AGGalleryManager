package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.ListenableWorker;


public class Service_Main extends IntentService {

    // IntentService Actions:
    private static final String ACTION_LOAD_APP_DATA = "com.agcurations.aggallerymanager.action.Load_App_Data";
    private static final String ACTION_CATALOG_BACKUP = "com.agcurations.aggallerymanager.action.Catalog_Backup";
    //Parameters:
    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.Bool_Problem";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.String_Problem";
    public static final String EXTRA_STRING_STATUS_MESSAGE = "com.agcurations.aggallerymanager.extra.String_Status_Message";
    //Global Variables:
    private GlobalClass globalClass;


    public Service_Main() {
        super("MainActivityDataService");
    }

    public static void startActionLoadData(Context context) {
        Intent intent = new Intent(context, Service_Main.class);
        intent.setAction(ACTION_LOAD_APP_DATA);
        context.startService(intent);
    }

    public static void startActionCatalogBackup(Context context) {
        Intent intent = new Intent(context, Service_Main.class);
        intent.setAction(ACTION_CATALOG_BACKUP);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        globalClass = (GlobalClass) getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LOAD_APP_DATA.equals(action)) {
                handleActionLoadAppData();
            } else if (ACTION_CATALOG_BACKUP.equals(action)){
                handleActionCatalogBackup();
            }
        }
    }


    private void handleActionLoadAppData() {

        String sExternalStorageState;
        sExternalStorageState = Environment.getExternalStorageState();
        if (sExternalStorageState.equals(Environment.MEDIA_MOUNTED) ) {

            File[] fAvailableDirs = getExternalFilesDirs(null);
            if (fAvailableDirs.length == 2) {
                globalClass.gfAppFolder = fAvailableDirs[1];
            } else {
                globalClass.gfAppFolder = fAvailableDirs[0];
            }

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
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
        globalClass.notificationManager = getSystemService(NotificationManager.class);
        globalClass.notificationManager.createNotificationChannel(globalClass.notificationChannel);


        ExecuteDownloadManagerPostProcessing();

        //globalClass.CatalogDataFile_AddNewField(); //Call when a new field is added.

    }

    private void handleActionCatalogBackup(){
        Intent broadcastIntent = new Intent();
        boolean bProblem = false;

        //Backup the catalog text files:
        for(int i = 0; i < 3; i++){
            StringBuilder sbBuffer = new StringBuilder();
            boolean bHeaderWritten = false;
            for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: globalClass.gtmCatalogLists.get(i).entrySet()){

                if(!bHeaderWritten) {
                    sbBuffer.append(globalClass.getCatalogHeader()); //Append the header.
                    sbBuffer.append("\n");
                    bHeaderWritten = true;
                }

                sbBuffer.append(globalClass.getCatalogRecordString(tmEntry.getValue())); //Append the data.
                sbBuffer.append("\n");
            }

            try {
                //Write the catalog file:
                String sDateTimeStamp = globalClass.GetTimeStampFileSafe();
                /*File fBackup = new File(globalClass.gfCatalogFolders[i].getAbsolutePath()
                        + File.separator + "CatalogContents_" + sDateTimeStamp + ".dat");*/
                File fBackup = new File(globalClass.gfAppFolder + File.separator
                        + GlobalClass.gsCatalogFolderNames[i] + "_CatalogContents_" + sDateTimeStamp + ".dat");

                FileWriter fwNewCatalogContentsFile = new FileWriter(fBackup, false);

                fwNewCatalogContentsFile.write(sbBuffer.toString());
                fwNewCatalogContentsFile.flush();
                fwNewCatalogContentsFile.close();

            } catch (Exception e) {
                String sMessage = "Problem creating backup of CatalogContents.dat.\n" + e.getMessage();
                broadcastIntent.putExtra(EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(EXTRA_STRING_PROBLEM, sMessage);
                bProblem = true;
            }
        }

        //Backup the tag files:
        for(int i = 0; i < 3; i++){
            StringBuilder sbBuffer = new StringBuilder();
            boolean bHeaderWritten = false;
            for(Map.Entry<String, ItemClass_Tag> tmEntry: globalClass.gtmCatalogTagReferenceLists.get(i).entrySet()){

                if(!bHeaderWritten) {
                    sbBuffer.append(GlobalClass.getTagFileHeader()); //Append the header.
                    sbBuffer.append("\n");
                    bHeaderWritten = true;
                }

                sbBuffer.append(globalClass.getTagRecordString(tmEntry.getValue())); //Append the data.
                sbBuffer.append("\n");
            }

            try {
                //Write the tag file:
                String sDateTimeStamp = globalClass.GetTimeStampFileSafe();
                /*File fBackup = new File(globalClass.gfCatalogFolders[i].getAbsolutePath()
                        + File.separator + "Tags_" + sDateTimeStamp + ".dat");*/
                File fBackup = new File(globalClass.gfAppFolder + File.separator
                        + GlobalClass.gsCatalogFolderNames[i] + "_Tags_" + sDateTimeStamp + ".dat");
                FileWriter fwNewTagsFile = new FileWriter(fBackup, false);

                fwNewTagsFile.write(sbBuffer.toString());
                fwNewTagsFile.flush();
                fwNewTagsFile.close();

            } catch (Exception e) {
                String sMessage = "Problem creating backup of Tags.dat.\n" + e.getMessage();
                broadcastIntent.putExtra(EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(EXTRA_STRING_PROBLEM, sMessage);
                bProblem = true;
            }
        }
        if(!bProblem) {
            String sMessage = "Database files backup completed.";
            broadcastIntent.putExtra(EXTRA_STRING_STATUS_MESSAGE, sMessage);
        }

        //Send broadcast to the Import Activity:
        broadcastIntent.setAction(Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }






    private void ExecuteDownloadManagerPostProcessing(){
        //DownloadIdleService will delete files after about a week. Rename downloaded files to prevent
        //  this from happening. This will need to occur for downloaded comics or

        ArrayList<ItemClass_CatalogItem> alsCatalogItemsToUpdate = new ArrayList<>();

        //Look for comic post-processing required:
        for(Map.Entry<String, ItemClass_CatalogItem> tmCatalogEntry: globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()) {
            ItemClass_CatalogItem ci = tmCatalogEntry.getValue();
            if (ci.iPostProcessingCode == ItemClass_CatalogItem.POST_PROCESSING_COMIC_DLM_MOVE) {
                //Check to see if all of the files have downloaded:
                String sComicItemFolderPath =
                        globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath()
                                + File.separator + ci.sFolder_Name;
                File fComicItemFolder = new File(sComicItemFolderPath);
                String sComicItemDLFolderPath = sComicItemFolderPath + File.separator + GlobalClass.gsDLTempFolderName;
                File fComicItemDLFolder = new File(sComicItemDLFolderPath);
                if (fComicItemDLFolder.exists()) {
                    File[] fComicDLFiles = fComicItemDLFolder.listFiles();
                    if (fComicDLFiles != null) {
                        if (fComicDLFiles.length == ci.iComicPages) {
                            //All of the files have been downloaded.
                            //Attempt to move the files:
                            boolean bMoveSuccessful = true;
                            for (File fDLFile : fComicDLFiles) {
                                String sFileName = fDLFile.getName();
                                File fDestination = new File(sComicItemFolderPath + File.separator + sFileName);
                                if (fDLFile.isFile()) {
                                    if (!fDLFile.renameTo(fDestination)) {
                                        Log.d("File move", "Cannot move file " + sFileName + " from " + fDLFile.getAbsolutePath() + " to " + fDestination.getAbsolutePath() + ".");
                                        bMoveSuccessful = false;
                                    }
                                }
                            }
                            if (bMoveSuccessful) {
                                //Delete the DL folder:
                                if (!fComicItemDLFolder.delete()) {
                                    Log.d("File move", "Could not delete " + fComicItemDLFolder.getAbsolutePath() + " folder.");
                                }
                                ci.iPostProcessingCode = ItemClass_CatalogItem.POST_PROCESSING_NONE;
                                alsCatalogItemsToUpdate.add(ci);
                            }
                        }
                    }
                } else {
                    Log.d("Post DLManager Ops", "DL folder not found for comic " + ci.sItemID);
                }
            }
        }
        //Look for video post-processing required:
        for(Map.Entry<String, ItemClass_CatalogItem> tmCatalogEntry: globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).entrySet()){
            ItemClass_CatalogItem ci = tmCatalogEntry.getValue();
            if((ci.iPostProcessingCode == ItemClass_CatalogItem.POST_PROCESSING_VIDEO_DLM_SINGLE) ||
                    (ci.iPostProcessingCode == ItemClass_CatalogItem.POST_PROCESSING_VIDEO_DLM_CONCAT)) {
                /*//Check for, and if it exists, move a single file.
                String sVideoDestinationFolder = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath() +
                        File.separator + ci.sFolder_Name;
                String sVideoDownloadFolder = sVideoDestinationFolder + File.separator + ci.sItemID;
                String sVideoDownloadedFile = sVideoDownloadFolder + File.separator + ci.sFilename;
                File fVideoDownloadedFile = new File(sVideoDownloadedFile);
                if(fVideoDownloadedFile.exists() && (fVideoDownloadedFile.length() >= ci.lSize)){
                    //Move the file:
                    String sOutputFileFinalDestination = sVideoDestinationFolder + File.separator + ci.sFilename;
                    File fOutputFileFinalDestination = new File(sOutputFileFinalDestination);
                    if (!fOutputFileFinalDestination.exists()) {
                        if(!fVideoDownloadedFile.renameTo(fOutputFileFinalDestination)){
                            Log.d("File Move", "Unable to move file from " + fVideoDownloadedFile.getAbsolutePath() + " to " + fOutputFileFinalDestination.getAbsolutePath());
                        } else {
                            //Move successful, delete the temp folder:
                            File fVideoDownloadFolder = new File(sVideoDownloadFolder);
                            if(!fVideoDownloadFolder.delete()){
                                Log.d("File Deletion", "Unable to delete folder " + fVideoDownloadFolder.getAbsolutePath());
                            }
                        }
                        //Update the catalog to indicate that this item has been moved.
                        ci.iPostProcessingCode = ItemClass_CatalogItem.POST_PROCESSING_NONE;
                        alsCatalogItemsToUpdate.add(ci);
                    }
                }


            } else if(ci.iPostProcessingCode == ItemClass_CatalogItem.POST_PROCESSING_VIDEO_DLM_CONCAT) {*/
                //Check to see if the concatenation (or single video file download) operation is complete:
                String sVideoDestinationFolder = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath() +
                        File.separator + ci.sFolder_Name;
                String sVideoDownloadFolder = sVideoDestinationFolder + File.separator + ci.sItemID;
                File fVideoDownloadFolder = new File(sVideoDownloadFolder);
                if(fVideoDownloadFolder.exists()){
                    File[] fVideoDownloadFolderListing = fVideoDownloadFolder.listFiles();
                    ArrayList<File> alfOutputFolders = new ArrayList<>();
                    if(fVideoDownloadFolderListing != null) {
                        for (File f : fVideoDownloadFolderListing) {
                            //Locate the output folder
                            if (f.isDirectory()) {
                                alfOutputFolders.add(f); //The worker could potentially create multiple output folders if it is re-run.
                            }
                        }
                        //Attempt to locate the output file of a concatenation operation:
                        for (File f : alfOutputFolders) {
                            String sOutputFileAbsolutePath = f.getAbsolutePath() + File.separator + ci.sFilename;
                            File fOutputFile = new File(sOutputFileAbsolutePath);
                            if (fOutputFile.exists()) {
                                //Concatenation is complete
                                //Move the file:
                                String sOutputFileFinalDestination = sVideoDestinationFolder + File.separator + ci.sFilename;
                                File fOutputFileFinalDestination = new File(sOutputFileFinalDestination);
                                if (!fOutputFileFinalDestination.exists()) {
                                    if(!fOutputFile.renameTo(fOutputFileFinalDestination)){
                                        Log.d("File Move", "Unable to move file from " + fOutputFile.getAbsolutePath() + " to " + fOutputFileFinalDestination.getAbsolutePath());
                                        break;
                                    }
                                }

                                //Delete output folder contents:
                                for (File f2 : alfOutputFolders) {
                                    File[] f2_Contents = f2.listFiles();
                                    if (f2_Contents != null) {
                                        for (File f3 : f2_Contents) {
                                            if(!f3.delete()){
                                                Log.d("File Deletion", "Unable to delete file " + f3.getAbsolutePath());
                                            }
                                        }
                                    }
                                }
                                //Delete download folder contents:
                                for (File f4 : fVideoDownloadFolderListing) {
                                    if(!f4.delete()){
                                        Log.d("File Deletion", "Unable to delete file or folder " + f4.getAbsolutePath());
                                    }
                                }
                                //Delete download folder:
                                if(!fVideoDownloadFolder.delete()){
                                    Log.d("File Deletion", "Unable to delete folder " + fVideoDownloadFolder.getAbsolutePath());
                                }

                                //Update catalog item to indicate no post-processing needed:
                                ci.iPostProcessingCode = ItemClass_CatalogItem.POST_PROCESSING_NONE;
                                alsCatalogItemsToUpdate.add(ci);
                                break; //Don't go through any more "output" folders in this temp download directory.
                            }
                        }
                    }
                }

            }
        }

        //Update any catalog records:
        for(ItemClass_CatalogItem ci: alsCatalogItemsToUpdate) {
            globalClass.CatalogDataFile_UpdateRecord(ci);
        }

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


    void problemNotificationConfig(String sMessage){
        Intent broadcastIntent_Notification = new Intent();
        broadcastIntent_Notification.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Notification.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Notification);
    }




}
