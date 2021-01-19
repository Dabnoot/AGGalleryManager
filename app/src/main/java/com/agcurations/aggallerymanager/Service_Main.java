package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.ClipData;
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

import javax.xml.parsers.FactoryConfigurationError;

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
                        if (entry.getValue().TagID.equals(iRestrictedTag)) {
                            //If the restricted tag has been found, mark it as restricted:
                            entry.getValue().isRestricted = true;
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
            globalClass.gtmCatalogLists.add(readNewCatalogFileToCatalogItems(iMediaCategory));
        }

        //Re-write the files in the new format:
        /*for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){
            createNewCatalogItemsFiles(iMediaCategory);
        }*/




    }

    private void obtainFolderStructureItem(File file){
        if(!file.exists()){
            if(!file.mkdir()){
                problemNotificationConfig("Could not create item at " + file.getAbsolutePath());
            }
        }
    }


    private TreeMap<String, ItemClass_CatalogItem> readNewCatalogFileToCatalogItems(int iMediaCategory){

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
                            ItemClass_CatalogItem ciFirst = new ItemClass_CatalogItem();
                            String[] sTemp = GlobalClass.getCatalogRecordString(ciFirst);
                            fwCatalogContentsFile.append(sTemp[0]); //Write the header.
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


    private void createNewCatalogItemsFiles(int iMediaCategory){

        File fCatalogFolder = globalClass.gfCatalogFolders[iMediaCategory];
        File fCatalogContentsFile = globalClass.gfCatalogContentsFiles[iMediaCategory];


        FileWriter fwCatalogContentsFile = null;
        try {
            fwCatalogContentsFile = new FileWriter(fCatalogContentsFile, false);

            TreeMap<String, ItemClass_CatalogItem> tmCatalogItems = globalClass.gtmCatalogLists.get(iMediaCategory);

            boolean bFirstRecord = true;
            for(Map.Entry<String, ItemClass_CatalogItem> entry: tmCatalogItems.entrySet()) {

                //Convert the ItemClass_CatalogItem to String:
                String[] sRecordLinePrep = GlobalClass.getCatalogRecordString(entry.getValue());

                //Write the activity_comic_details_header line to the file:
                if(bFirstRecord) {
                    bFirstRecord = false;
                    fwCatalogContentsFile.append(sRecordLinePrep[0]); //Write the header.
                    fwCatalogContentsFile.append("\n");
                }
                fwCatalogContentsFile.append(sRecordLinePrep[2]); //Write the line prepared for file output.
                fwCatalogContentsFile.append("\n");

            }

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

    }



    private TreeMap<String, ItemClass_CatalogItem> readCatalogFileToCatalogItems(int iMediaCategory){

        File fCatalogFolder = globalClass.gfCatalogFolders[iMediaCategory];
        File fCatalogContentsFile = globalClass.gfCatalogContentsFiles[iMediaCategory];
        File fLogsFolder = globalClass.gfCatalogLogsFolders[iMediaCategory];
        String[] sRecordFields = GlobalClass.CatalogRecordFields[iMediaCategory];

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
                            fwCatalogContentsFile.append(sRecordFields[0]);
                            for(int i = 1; i < sRecordFields.length; i++) {
                                fwCatalogContentsFile.append("\t");
                                fwCatalogContentsFile.append(sRecordFields[i]);
                            }
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
            //if(fCatalogContentsFile.exists()){

            //Process any modifications to the CatalogContentsFile:
            //Catalog_data_file_add_field();

            //}

            //Look for the Logs folder. If it does not exist, create it.
            if(!fLogsFolder.exists()) {
                if(!fLogsFolder.mkdirs()){
                    problemNotificationConfig("Could not create log folder at" + fLogsFolder.getAbsolutePath());
                }
            }

            //Build the internal list of entries:
            TreeMap<Integer, String[]> tmCatalogListings = new TreeMap<>();

            //Read the list of entries and populate the catalog array:
            BufferedReader brReader;
            try {
                brReader = new BufferedReader(new FileReader(fCatalogContentsFile.getAbsolutePath()));
                brReader.readLine(); //The first line is the header. Skip this line.
                String sLine = brReader.readLine();
                String[] sFields;
                int iRID = 0;
                while (sLine != null) {
                    //Split the line read from the contents file with the delimiter of TAB:
                    sFields = sLine.split("\t",-1);
                    //De-jumble the data:
                    String[] sFields2 = new String[sFields.length];
                    for(int i = 0; i < sFields.length; i++){
                        if(i == GlobalClass.iNoJumbleFileNameIndex[iMediaCategory]){
                            sFields2[i] = sFields[i];
                        } else {
                            sFields2[i] = GlobalClass.JumbleStorageText(sFields[i]);
                        }
                    }

                    tmCatalogListings.put(iRID, sFields2);

                    // read next line
                    sLine = brReader.readLine();
                    iRID++;
                }
                brReader.close();

            } catch (IOException e) {
                problemNotificationConfig("Trouble reading CatalogContents.dat at" + fCatalogContentsFile.getAbsolutePath());
            }



            TreeMap<String, ItemClass_CatalogItem> tmCatalogItems = new TreeMap<>();
            ItemClass_CatalogItem ci;
            for(Map.Entry<Integer, String[]> entry: tmCatalogListings.entrySet()) {
                ci = new ItemClass_CatalogItem();
                ci.iMediaCategory = iMediaCategory;
                if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                    ci.sItemID                        = entry.getValue()[GlobalClass.VIDEO_ID_INDEX];                             //Video ID
                    ci.sFilename                      = entry.getValue()[GlobalClass.VIDEO_FILENAME_INDEX];
                    ci.lSize                          = Long.parseLong(entry.getValue()[GlobalClass.VIDEO_SIZE_MB_INDEX]) * 1024 * 1024;
                    ci.sDuration_Text                 = entry.getValue()[GlobalClass.VIDEO_DURATION_TEXT_INDEX];                  //Duration of video text
                    ci.lDuration_Milliseconds         = Long.parseLong(entry.getValue()[GlobalClass.VIDEO_DURATION_MILLISECONDS_INDEX]);          //Duration of video in Milliseconds
                    ci.sFolder_Name                   = entry.getValue()[GlobalClass.VIDEO_FOLDER_NAME_INDEX];                    //Name of the folder holding the video
                    ci.sTags                          = entry.getValue()[GlobalClass.VIDEO_TAGS_INDEX];                           //Tags given to the video
                    ci.sCast                          = entry.getValue()[GlobalClass.VIDEO_CAST_INDEX];
                    ci.sSource                        = entry.getValue()[GlobalClass.VIDEO_SOURCE_INDEX];                         //Website, if relevant
                    ci.dDatetime_Last_Viewed_by_User  = Double.parseDouble(entry.getValue()[GlobalClass.VIDEO_DATETIME_LAST_VIEWED_BY_USER_INDEX]);   //Date of last read by user. Used for sorting if desired
                    ci.dDatetime_Import               = Double.parseDouble(entry.getValue()[GlobalClass.VIDEO_DATETIME_IMPORT_INDEX]);               //Date of import. Used for sorting if desired
                    String[] sResolution              = entry.getValue()[GlobalClass.VIDEO_RESOLUTION_INDEX].split("x"); //Width x Height
                    if(sResolution.length > 1){
                        ci.iWidth = Integer.parseInt(sResolution[0]);
                        ci.iHeight = Integer.parseInt(sResolution[1]);
                    }
                } else if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                    ci.sItemID                        = entry.getValue()[GlobalClass.IMAGE_ID_INDEX];                             //Image ID
                    ci.sFilename                      = entry.getValue()[GlobalClass.IMAGE_FILENAME_INDEX];
                    ci.lSize                          = Long.parseLong(entry.getValue()[GlobalClass.IMAGE_SIZE_KB_INDEX]);
                    ci.sFolder_Name                   = entry.getValue()[GlobalClass.IMAGE_FOLDER_NAME_INDEX];                    //Name of the folder holding the imGE
                    ci.sTags                          = entry.getValue()[GlobalClass.IMAGE_TAGS_INDEX];                           //Tags given to the Image
                    ci.sCast                          = entry.getValue()[GlobalClass.IMAGE_CAST_INDEX];
                    ci.sSource                        = entry.getValue()[GlobalClass.IMAGE_SOURCE_INDEX];                         //Website, if relevant
                    ci.dDatetime_Last_Viewed_by_User  = Double.parseDouble(entry.getValue()[GlobalClass.IMAGE_DATETIME_LAST_VIEWED_BY_USER_INDEX]);   //Date of last read by user. Used for sorting if desired
                    ci.dDatetime_Import               = Double.parseDouble(entry.getValue()[GlobalClass.IMAGE_DATETIME_IMPORT_INDEX]);                //Date of import. Used for sorting if desired
                    String[] sResolution              = entry.getValue()[GlobalClass.IMAGE_DIMENSIONS_INDEX].split("x"); //Width x Height
                    if(sResolution.length > 1){
                        ci.iWidth = Integer.parseInt(sResolution[0]);
                        ci.iHeight = Integer.parseInt(sResolution[1]);
                    }
                } else if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                    ci.sItemID                        = entry.getValue()[GlobalClass.COMIC_ID_INDEX];                    //Comic ID
                    ci.sComicName                     = entry.getValue()[GlobalClass.COMIC_NAME_INDEX];                  //Comic Name
                    ci.iComic_File_Count              = Integer.parseInt(entry.getValue()[GlobalClass.COMIC_FILE_COUNT_INDEX]);            //Files included with the comic
                    ci.iComic_Max_Page_ID             = Integer.parseInt(entry.getValue()[GlobalClass.COMIC_MAX_PAGE_ID_INDEX]);           //Max page ID extracted from file names
                    ci.sComic_Missing_Pages           = entry.getValue()[GlobalClass.COMIC_MISSING_PAGES_INDEX];         //String of comma-delimited missing page numbers
                    ci.lSize                          = Long.parseLong(entry.getValue()[GlobalClass.COMIC_SIZE_KB_INDEX]);               //Total size of all files in the comic
                    ci.sFolder_Name                   = entry.getValue()[GlobalClass.COMIC_FOLDER_NAME_INDEX];           //Name of the folder holding the comic pages
                    ci.sFilename                      = entry.getValue()[GlobalClass.COMIC_THUMBNAIL_FILE_INDEX];        //Name of the file used as the thumbnail for the comic
                    ci.sThumbnail_File                = entry.getValue()[GlobalClass.COMIC_THUMBNAIL_FILE_INDEX];        //Name of the file used as the thumbnail for the comic
                    ci.sComicParodies                 = entry.getValue()[GlobalClass.COMIC_PARODIES_INDEX];
                    ci.sComicCharacters               = entry.getValue()[GlobalClass.COMIC_CHARACTERS_INDEX];
                    ci.sTags                          = entry.getValue()[GlobalClass.COMIC_TAGS_INDEX];                 //Tags given to the comic
                    ci.sComicArtists                  = entry.getValue()[GlobalClass.COMIC_ARTISTS_INDEX];
                    ci.sComicGroups                   = entry.getValue()[GlobalClass.COMIC_GROUPS_INDEX];
                    ci.sComicLanguages                = entry.getValue()[GlobalClass.COMIC_LANGUAGES_INDEX];            //Language(s) found in the comic
                    ci.sComicCategories               = entry.getValue()[GlobalClass.COMIC_CATEGORIES_INDEX];
                    ci.iComicPages                    = Integer.parseInt(entry.getValue()[GlobalClass.COMIC_PAGES_INDEX]);                //Total number of pages as defined at the comic source
                    ci.sSource                        = entry.getValue()[GlobalClass.COMIC_SOURCE_INDEX];                     //nHentai.net, other source, etc.
                    ci.dDatetime_Last_Viewed_by_User  = Double.parseDouble(entry.getValue()[GlobalClass.COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX]); //Date of last read by user. Used for sorting if desired
                    ci.dDatetime_Import               = Double.parseDouble(entry.getValue()[GlobalClass.COMIC_DATETIME_IMPORT_INDEX]);            //Date of import. Used for sorting if desired
                    ci.bComic_Online_Data_Acquired = !entry.getValue()[GlobalClass.COMIC_ONLINE_DATA_ACQUIRED_INDEX].equals(GlobalClass.COMIC_ONLINE_DATA_ACQUIRED_NO);

                }
                tmCatalogItems.put(ci.sItemID,ci);
            }


            //Return the data read from the file:
            return tmCatalogItems;

        }

    }


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
