package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;


public class Service_Main extends IntentService {

    // IntentService Actions:
    private static final String ACTION_LOAD_APP_DATA = "com.agcurations.aggallerymanager.action.LAD";
    //Parameters:
    public static final String EXTRA_BOOL_DATA_LOAD_PROBLEM = "com.agcurations.aggallerymanager.extra.BDLP";
    public static final String EXTRA_STRING_DATA_LOAD_PROBLEM = "com.agcurations.aggallerymanager.extra.SDLP";

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


    @Override
    protected void onHandleIntent(Intent intent) {
        globalClass = (GlobalClass) getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LOAD_APP_DATA.equals(action)) {
                handleActionLoadAppData();
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
                globalClass.gfCatalogContentsFiles[i] = new File(globalClass.gfCatalogFolders[i].getAbsolutePath()
                        + File.separator + "CatalogContents.dat");
                //Identify the Logs folder for the catalog:
                globalClass.gfCatalogLogsFolders[i] = new File(globalClass.gfCatalogFolders[i]
                        + File.separator + "Logs");
                obtainFolderStructureItem(globalClass.gfCatalogLogsFolders[i]);
                //Identify the tags file for the catalog:
                globalClass.gfCatalogTagsFiles[i] = new File(globalClass.gfCatalogFolders[i].getAbsolutePath()
                        + File.separator + "Tags.dat");
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



        //Change the video file names to match with their actual names in storage (non-jumbled):
        //(Otherwise the actual file name shows up in CatalogContents.dat because the video files
        //  are renamed on import)
        //globalClass.CatalogDataFile_UpdateAllRecords_UnJumbleVideoFileName();

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

    }

/*    private void fixComicPageCount(){
        ArrayList<ItemClass_CatalogItem> alsCatalogItemsToUpdate = new ArrayList<>();
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()){
            if(tmEntry.getValue().iComicPages == 0) {
                tmEntry.getValue().iComicPages = tmEntry.getValue().iComic_File_Count;
                tmEntry.getValue().iComic_Max_Page_ID = tmEntry.getValue().iComic_File_Count;
                alsCatalogItemsToUpdate.add(tmEntry.getValue());
            }
        }

        for(ItemClass_CatalogItem ci: alsCatalogItemsToUpdate) {
            globalClass.CatalogDataFile_UpdateRecord(ci);
        }
    }*/


    /*private void fixComicCoverPageAssignments(){
        ArrayList<ItemClass_CatalogItem> alsCatalogItemsToUpdate = new ArrayList<>();
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()){
            String sComicFolder = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS] +
                    File.separator + tmEntry.getValue().sFolder_Name;
            File fComicFolder = new File(sComicFolder);
            String sCoverFilePath = sComicFolder + File.separator + tmEntry.getValue().sFilename;
            File fCoverFile = new File(sCoverFilePath);
            if(!fCoverFile.exists()){
                File[] fComicFiles = fComicFolder.listFiles();
                if(fComicFiles != null) {
                    for (File fComicFile : fComicFiles) {
                        if(fComicFile.getName().contains("100_egaP") || fComicFile.getName().contains("1000_egaP")){
                            tmEntry.getValue().sFilename = fComicFile.getName();
                            tmEntry.getValue().sThumbnail_File = fComicFile.getName();
                            alsCatalogItemsToUpdate.add(tmEntry.getValue());
                            break;
                        }
                    }
                }
            }
        }

        for(ItemClass_CatalogItem ci: alsCatalogItemsToUpdate){
            globalClass.CatalogDataFile_UpdateRecord(ci);
        }
    }*/


    /*private void fixComicPageIDs(){
        ArrayList<ItemClass_CatalogItem> alsCatalogItemsToUpdate = new ArrayList<>();
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()){
            String sComicFolder = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS] +
                    File.separator + tmEntry.getValue().sFolder_Name;
            File fComicFolder = new File(sComicFolder);
            File[] fComicFiles = fComicFolder.listFiles();
            boolean bThumbnailFilenameReset = false;
            if(fComicFiles != null) {
                for (File fComicFile : fComicFiles) {
                    String sFileName = GlobalClass.JumbleFileName(fComicFile.getName());
                    if(sFileName.startsWith(tmEntry.getValue().sItemID)){
                        break;
                    }
                    //If the filename does not have the comicID (but rather has the NHComicID),
                    //  rename the file to have the comicID, and reset the thumbnail file.
                    String sNewFileName = sFileName.substring(sFileName.indexOf("_"));
                    sNewFileName = tmEntry.getValue().sItemID + sNewFileName;
                    sNewFileName = GlobalClass.JumbleFileName(sNewFileName);
                    if(fComicFile.getName().contains("100_egaP") || fComicFile.getName().contains("1000_egaP")){
                        bThumbnailFilenameReset = true;
                        tmEntry.getValue().sFilename = sNewFileName;
                        tmEntry.getValue().sThumbnail_File = sNewFileName;
                        alsCatalogItemsToUpdate.add(tmEntry.getValue());
                    }
                    File fDestinationFileName = new File(sComicFolder +
                            File.separator +
                            sNewFileName);
                    if(!fComicFile.renameTo(fDestinationFileName)){
                        Log.d("Comics","Trouble renaming file: " + fComicFile.getPath());
                    }

                }
            }

        }

        for(ItemClass_CatalogItem ci: alsCatalogItemsToUpdate) {
            globalClass.CatalogDataFile_UpdateRecord(ci);
        }

    }*/

    /*private void analyzeComicsReportMissingPages(){
        int icount = 0;
        for(Map.Entry<String, ItemClass_CatalogItem> ciEntry: globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()){
           ciEntry.setValue(globalClass.analyzeComicReportMissingPages(ciEntry.getValue()));

           if(!ciEntry.getValue().sComic_Missing_Pages.equals("") || ciEntry.getValue().iComic_File_Count == 0){
                   if(globalClass.isNetworkConnected) {
                       Intent intentGetComicDetails;
                       intentGetComicDetails = new Intent(this, Service_ComicDetails.class);
                       intentGetComicDetails.putExtra(Service_ComicDetails.COMIC_CATALOG_ITEM, ciEntry.getValue());
                       startService(intentGetComicDetails);
                   }
               icount++;
           }
        }
        Log.d("Comics", icount + " with missing pages.");
    }*/



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
        File fLogsFolder = globalClass.gfCatalogLogsFolders[iMediaCategory];

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

            //Look for the Logs folder. If it does not exist, create it.
            if(!fLogsFolder.exists()) {
                if(!fLogsFolder.mkdirs()){
                    problemNotificationConfig("Could not create log folder at" + fLogsFolder.getAbsolutePath());
                }
            }

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



    /*private void updateCatalogRecords(int iMediaCategory){

        //Loop through each comic entry, attempt to rename the folder to ComicID, then update the
        //  catalog file and memory record:

        ItemClass_CatalogItem ciComic;
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: globalClass.gtmCatalogLists.get(iMediaCategory).entrySet()){

            ciComic = tmEntry.getValue();
            //ciComic.sSource = "https:/nhentai.net/g/" + ciComic.sItemID + "/";
            globalClass.CatalogDataFile_UpdateRecord(ciComic);

        }

    }*/


    /*public void Comic_Catalog_data_file_add_field() {
        //Add the new field to GlobalClass.ComicRecordFields before running this routine.
        //  This will affect the creation of the dat file header.

        int iToVersion = 2; //This causes the routine to update the .dat file only once.

        File fCatalogContentsFile = globalClass.gfComicCatalogContentsFile;

        try {
            //Read the list of comics and populate the catalog array:
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fCatalogContentsFile.getAbsolutePath()));

            //Get the version of the current .dat file.
            String sLine = brReader.readLine();
            String[] sFields = sLine.split("\t");
            String[] sVersionData = sFields[sFields.length - 1].split(".");
            int iFromVersion = 0;
            if (sVersionData.length == 2) {
                iFromVersion = Integer.parseInt(sVersionData[1]);
            }
            //Quit this routine if the version of the .dat file to be written
            //  is the same or older:
            if (iToVersion <= iFromVersion) {
                brReader.close();
                return;
            }

            //Create the new catalog contents file:
            File fCatalogComicsFolder = globalClass.gfComicsFolder;
            File fNewCatalogContentsFile;

            //Create a new catalog status file:
            fNewCatalogContentsFile = new File(fCatalogComicsFolder.getAbsolutePath() + File.separator + "CatalogContents_new.dat");

            if (!fNewCatalogContentsFile.exists()) {
                try {
                    if (fNewCatalogContentsFile.createNewFile()) {
                        FileWriter fwNewCatalogContentsFile;
                        try {
                            fwNewCatalogContentsFile = new FileWriter(fNewCatalogContentsFile, true);

                            //Write the activity_comic_details_header line to the file:
                            fwNewCatalogContentsFile.append(GlobalClass.ComicRecordFields[0]);
                            for (int i = 1; i < GlobalClass.ComicRecordFields.length; i++) {
                                fwNewCatalogContentsFile.append("\t");
                                fwNewCatalogContentsFile.append(GlobalClass.ComicRecordFields[i]);
                            }

                            fwNewCatalogContentsFile.append("\t");
                            fwNewCatalogContentsFile.append("DataFileVersion."); //DataFileVersion.[version number]
                            fwNewCatalogContentsFile.append(Integer.toString(iToVersion));
                            fwNewCatalogContentsFile.append("\n");

                            //Write lines from the original .dat file to the new .dat file:
                            sLine = brReader.readLine();
                            while (sLine != null) {
                                fwNewCatalogContentsFile.append(sLine);
                                fwNewCatalogContentsFile.append("\t");

                                //Write field initial value here:
                                sFields = sLine.split("\t",-1);
                                if(sFields[GlobalClass.COMIC_TAGS_INDEX].equals("")){ //If TAGS data does not exist
                                    fwNewCatalogContentsFile.append("No");  //Initial value "online data acquired = no"
                                } else {                                    //else if TAGS data DOES exist...
                                    fwNewCatalogContentsFile.append("Yes"); //Initial value "online data acquired = yes"
                                }

                                //Close the data row:
                                fwNewCatalogContentsFile.append("\n");
                                // read next line
                                sLine = brReader.readLine();
                            }
                            brReader.close();

                            fwNewCatalogContentsFile.flush();
                            fwNewCatalogContentsFile.close();

                            File fRenameCurrentDatFile = new File(fCatalogComicsFolder.getAbsolutePath() + File.separator + "CatalogContents_v" + iFromVersion + "_bak.dat");
                            if (!fRenameCurrentDatFile.exists()) {
                                if (!fCatalogContentsFile.renameTo(fRenameCurrentDatFile)) {
                                    problemNotificationConfig("Could not rename CatalogContentsFile.");
                                } else {
                                    if (!fNewCatalogContentsFile.renameTo(fCatalogContentsFile)) {
                                        problemNotificationConfig("Could not rename new CatalogContentsFile.");
                                    }
                                }
                            }

                        } catch (Exception e) {
                            problemNotificationConfig("Problem during CatalogContentsFile re-write.\n" + e.getMessage());
                        }
                    } else {
                        problemNotificationConfig("Could not write new CatalogContents.dat at" + fNewCatalogContentsFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    problemNotificationConfig("Could not create new CatalogContents.dat at" + fNewCatalogContentsFile.getAbsolutePath());
                }
            }
        } catch (Exception e){
            problemNotificationConfig("Could not open CatalogContents.dat at" + fCatalogContentsFile.getAbsolutePath()+ "\n" + e.getMessage());
        }
    }
*/

    /*public void Catalog_data_file_jumble_fields() {
        //Jumble the fields.
        //  This will affect the creation of the dat file header.

        int iToVersion = 3; //This causes the routine to update the .dat file only once.

        File fCatalogContentsFile = globalClass.gfComicCatalogContentsFile;

        try {
            //Read the list of comics and populate the catalog array:
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fCatalogContentsFile.getAbsolutePath()));

            //Get the version of the current .dat file.
            String sLine = brReader.readLine();
            String[] sFields = sLine.split("\t");
            String[] sVersionData = sFields[sFields.length - 1].split(".");
            int iFromVersion = 0;
            if (sVersionData.length == 2) {
                iFromVersion = Integer.parseInt(sVersionData[1]);
            }
            //Quit this routine if the version of the .dat file to be written
            //  is the same or older:
            if (iToVersion <= iFromVersion) {
                brReader.close();
                return;
            }

            //Create the new catalog contents file:
            File fCatalogComicsFolder = globalClass.gfComicsFolder;
            File fNewCatalogContentsFile;

            //Create a new catalog status file:
            fNewCatalogContentsFile = new File(fCatalogComicsFolder.getAbsolutePath() + File.separator + "CatalogContents_new.dat");

            if (!fNewCatalogContentsFile.exists()) {
                try {
                    if (fNewCatalogContentsFile.createNewFile()) {
                        FileWriter fwNewCatalogContentsFile;
                        try {
                            fwNewCatalogContentsFile = new FileWriter(fNewCatalogContentsFile, true);

                            //Write the activity_comic_details_header line to the file:
                            fwNewCatalogContentsFile.append(GlobalClass.ComicRecordFields[0]);
                            for (int i = 1; i < GlobalClass.ComicRecordFields.length; i++) {
                                fwNewCatalogContentsFile.append("\t");
                                fwNewCatalogContentsFile.append(GlobalClass.ComicRecordFields[i]);
                            }

                            fwNewCatalogContentsFile.append("\t");
                            fwNewCatalogContentsFile.append("DataFileVersion."); //DataFileVersion.[version number]
                            fwNewCatalogContentsFile.append(Integer.toString(iToVersion));
                            fwNewCatalogContentsFile.append("\n");

                            //Write lines from the original .dat file to the new .dat file:
                            sLine = brReader.readLine();
                            while (sLine != null) {
                                sFields = sLine.split("\t",-1);
                                //Write data to file:
                                fwNewCatalogContentsFile.append(globalClass.JumbleStorageText(sFields[0]));
                                for(int i = 1; i < sFields.length; i++){
                                    fwNewCatalogContentsFile.append("\t");
                                    fwNewCatalogContentsFile.append(globalClass.JumbleStorageText(sFields[i]));
                                }
                                //Close the data row:
                                fwNewCatalogContentsFile.append("\n");
                                // read next line
                                sLine = brReader.readLine();
                            }
                            brReader.close();

                            fwNewCatalogContentsFile.flush();
                            fwNewCatalogContentsFile.close();

                            File fRenameCurrentDatFile = new File(fCatalogComicsFolder.getAbsolutePath() + File.separator + "CatalogContents_v" + iFromVersion + "_bak.dat");
                            if (!fRenameCurrentDatFile.exists()) {
                                if (!fCatalogContentsFile.renameTo(fRenameCurrentDatFile)) {
                                    problemNotificationConfig("Could not rename CatalogContentsFile.");
                                } else {
                                    if (!fNewCatalogContentsFile.renameTo(fCatalogContentsFile)) {
                                        problemNotificationConfig("Could not rename new CatalogContentsFile.");
                                    }
                                }
                            }

                        } catch (Exception e) {
                            problemNotificationConfig("Problem during CatalogContentsFile re-write.\n" + e.getMessage());
                        }
                    } else {
                        problemNotificationConfig("Could not write new CatalogContents.dat at" + fNewCatalogContentsFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    problemNotificationConfig("Could not create new CatalogContents.dat at" + fNewCatalogContentsFile.getAbsolutePath());
                }
            }
        } catch (Exception e){
            problemNotificationConfig("Could not open CatalogContents.dat at" + fCatalogContentsFile.getAbsolutePath()+ "\n" + e.getMessage());
        }
    }
*/

    void problemNotificationConfig(String sMessage){
        Intent broadcastIntent_Notification = new Intent();
        broadcastIntent_Notification.putExtra(EXTRA_BOOL_DATA_LOAD_PROBLEM, true);
        broadcastIntent_Notification.putExtra(EXTRA_STRING_DATA_LOAD_PROBLEM, sMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Notification);
    }




}
