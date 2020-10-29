package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;


public class MainActivityDataService extends IntentService {

    // IntentService Actions:
    private static final String ACTION_LOAD_APP_DATA = "com.agcurations.aggallerymanager.action.LAD";
    //Parameters:
    public static final String EXTRA_BOOL_DATA_LOAD_PROBLEM = "com.agcurations.aggallerymanager.extra.BDLP";
    public static final String EXTRA_STRING_DATA_LOAD_PROBLEM = "com.agcurations.aggallerymanager.extra.SDLP";

    //Global Variables:
    private GlobalClass globalClass;


    public MainActivityDataService() {
        super("MainActivityDataService");
    }


    public static void startActionLoadData(Context context) {
        Intent intent = new Intent(context, MainActivityDataService.class);
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



    Intent broadcastIntent_LoadAppDataResponse = new Intent();; //Make global to allow for problem notification string extras.
    private void handleActionLoadAppData() {

        String sExternalStorageState;
        sExternalStorageState = Environment.getExternalStorageState();
        if (sExternalStorageState.equals(Environment.MEDIA_MOUNTED) ) {

            File[] fAvailableDirs = getExternalFilesDirs(null);
            if (fAvailableDirs.length == 2) {
                GlobalClass.gfAppFolder = fAvailableDirs[1];
            } else {
                GlobalClass.gfAppFolder = fAvailableDirs[0];
            }



            //--------------------------------------------------------------------------------
            //Videos Folder Structure:
            GlobalClass.gfVideosFolder = new File(GlobalClass.gfAppFolder
                    + File.separator + "Videos");
            obtainFolderStructureItem(GlobalClass.gfVideosFolder);

            GlobalClass.gfVideoCatalogContentsFile = new File(GlobalClass.gfVideosFolder.getAbsolutePath()
                    + File.separator + "CatalogContents.dat");

            GlobalClass.gfVideoLogsFolder = new File(GlobalClass.gfVideosFolder
                    + File.separator + "Logs");
            obtainFolderStructureItem(GlobalClass.gfVideoLogsFolder);

            GlobalClass.gfVideoTagsFile = new File(GlobalClass.gfVideosFolder.getAbsolutePath()
                    + File.separator + "VideoTags.dat");
            //--------------------------------------------------------------------------------

            //--------------------------------------------------------------------------------
            //Comics Folder Structure:
            GlobalClass.gfComicsFolder = new File(GlobalClass.gfAppFolder
                    + File.separator + "Comics");
            obtainFolderStructureItem(GlobalClass.gfComicsFolder);

            GlobalClass.gfComicCatalogContentsFile = new File(GlobalClass.gfComicsFolder.getAbsolutePath()
                    + File.separator + "CatalogContents.dat");

            GlobalClass.gfComicLogsFolder = new File(GlobalClass.gfComicsFolder
                    + File.separator + "Logs");
            obtainFolderStructureItem(GlobalClass.gfComicLogsFolder);

            GlobalClass.gfComicTagsFile = new File(GlobalClass.gfComicsFolder.getAbsolutePath()
                    + File.separator + "ComicTags.dat");
            //--------------------------------------------------------------------------------

            //Attempt to read a pin number set by the user:
            GlobalClass.gfAppConfigFile = new File(GlobalClass.gfAppFolder.getAbsolutePath()
                    + File.separator + "AppConfig.dat");
            if (!GlobalClass.gfAppConfigFile.exists()) {
                try {
                    if (!GlobalClass.gfAppConfigFile.createNewFile()) {
                        problemNotificationConfig("Could not create AppConfig.dat at " + GlobalClass.gfAppConfigFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    problemNotificationConfig("Could not create AppConfig.dat at " + GlobalClass.gfAppConfigFile.getAbsolutePath());
                }
            } else {

                //Read the AppConfig data. This file, at the time of design, was only intended to
                //  hold 1 piece of data - a pin/password set by the user to unlock certain settings.
                //  Specifically, settings for restricted tags, and turning the restriction on and off.
                BufferedReader brReader;
                String sLine = "";
                try {
                    brReader = new BufferedReader(new FileReader(GlobalClass.gfAppConfigFile.getAbsolutePath()));
                    sLine = brReader.readLine();
                    brReader.close();
                } catch (IOException e) {
                    problemNotificationConfig("Trouble reading AppConfig.dat at" + GlobalClass.gfAppConfigFile.getAbsolutePath());
                }

                //Set the global variable holding the pin:
                if (sLine == null) {
                    GlobalClass.gsPin = "";
                } else {
                    GlobalClass.gsPin = sLine;
                }
            }
        }

        //Attempt to read or create the video tags file:
        GlobalClass.gtmVideoTagReferenceList =
                InitTagData(GlobalClass.gfVideoTagsFile,
                        getResources().getStringArray(R.array.default_video_tags));

        //Attempt to read or create the comic tags file:
        GlobalClass.gtmComicTagReferenceList =
                InitTagData(GlobalClass.gfComicTagsFile,
                        getResources().getStringArray(R.array.default_comic_tags));


        //readComicsCatalogFile();


        GlobalClass.gtmCatalogComicList = readCatalogFile(
                GlobalClass.gfComicsFolder,
                GlobalClass.gfComicCatalogContentsFile,
                GlobalClass.gfComicLogsFolder,
                GlobalClass.ComicRecordFields);



    }

    private void obtainFolderStructureItem(File file){
        if(!file.exists()){
            if(!file.mkdir()){
                problemNotificationConfig("Could not create item at " + file.getAbsolutePath());
            }
        }
    }

    private TreeMap<Integer, String[]> InitTagData(File fTagsFile, String[] sDefaultTags ){
        TreeMap<Integer, String[]> tmTags = new TreeMap<>();
        if(fTagsFile.exists()) {
            //Get Tags from file:
            BufferedReader brReader;
            try {

                brReader = new BufferedReader(new FileReader(fTagsFile.getAbsolutePath()));
                brReader.readLine();//First line is the header, skip it.
                String sLine = brReader.readLine();

                while(sLine != null) {
                    String[] sFields;
                    sFields = sLine.split(",");
                    Integer iTagID = Integer.parseInt(sFields[0]);

                    //Reverse the text of each field (except the TagID):
                    for(int j = 1; j < sFields.length; j++){
                        sFields[j] = GlobalClass.JumbleStorageText(sFields[j]);
                    }


                    tmTags.put(iTagID, sFields); //Yes, even though the TagID is the key, value sFields also includes the tagID.
                    sLine = brReader.readLine();
                }

                brReader.close();

            } catch (IOException e) {
                problemNotificationConfig("Trouble reading tags file at" + fTagsFile.getAbsolutePath());
            }
        } else { //If the tags file does not exist, create it and populate it with default values:
            try {
                if(fTagsFile.createNewFile()) {
                    try {
                        FileWriter fwTagsFile = new FileWriter(fTagsFile, false);
                        Integer i = 0;
                        //Write the header record:
                        fwTagsFile.write(GlobalClass.TagRecordFields[0]);
                        for(int j = 1; j < GlobalClass.TagRecordFields.length; j++){
                            fwTagsFile.write("," + GlobalClass.TagRecordFields[j]);
                        }
                        fwTagsFile.write("\n");

                        //Write data records:
                        for (String sEntry : sDefaultTags) {
                            if(!sEntry.equals("")) {
                                fwTagsFile.write(i.toString() + "," + GlobalClass.JumbleStorageText(sEntry) + "\n");
                                String[] s = new String[]{i.toString(), sEntry};
                                tmTags.put(i, s);
                                i++;
                            }
                        }

                        //Close the tags file:
                        fwTagsFile.flush();
                        fwTagsFile.close();

                    } catch (IOException e) {
                        problemNotificationConfig( "Trouble writing file at "
                                + fTagsFile.getAbsolutePath());
                    }
                } else {
                    problemNotificationConfig( "Could not create file at "
                            + fTagsFile.getAbsolutePath());
                }
            }catch (IOException e){
                problemNotificationConfig("Could not create file at "
                        + fTagsFile.getAbsolutePath());
            }
        }

        return tmTags;
    }

    private TreeMap<Integer, String[]> readCatalogFile(File fCatalogFolder, File fCatalogContentsFile, File fLogsFolder, String[] sRecordFields){

        boolean bFolderOk = false ;
        if(!fCatalogFolder.exists()) {
            if (fCatalogFolder.mkdirs()) {
                bFolderOk = true;
            }
        }else{
            bFolderOk = true;
        }

        if (!bFolderOk) {
            problemNotificationConfig("Could not create catalog data folder 'Comics'.");
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
                        sFields2[i] = GlobalClass.JumbleStorageText(sFields[i]);
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

            //Return the data read from the file:
            return tmCatalogListings;

        }

    }


    public void Catalog_data_file_add_field() {
        //Add the new field to GlobalClass.ComicRecordFields before running this routine.
        //  This will affect the creation of the dat file header.

        int iToVersion = 2; //This causes the routine to update the .dat file only once.

        File fCatalogContentsFile = GlobalClass.gfComicCatalogContentsFile;

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
            File fCatalogComicsFolder = GlobalClass.gfComicsFolder;
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

    public void Catalog_data_file_jumble_fields() {
        //Jumble the fields.
        //  This will affect the creation of the dat file header.

        int iToVersion = 3; //This causes the routine to update the .dat file only once.

        File fCatalogContentsFile = GlobalClass.gfComicCatalogContentsFile;

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
            File fCatalogComicsFolder = GlobalClass.gfComicsFolder;
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
                                fwNewCatalogContentsFile.append(GlobalClass.JumbleStorageText(sFields[0]));
                                for(int i = 1; i < sFields.length; i++){
                                    fwNewCatalogContentsFile.append("\t");
                                    fwNewCatalogContentsFile.append(GlobalClass.JumbleStorageText(sFields[i]));
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

    void problemNotificationConfig(String sMessage){
        broadcastIntent_LoadAppDataResponse.putExtra(EXTRA_BOOL_DATA_LOAD_PROBLEM, true);
        broadcastIntent_LoadAppDataResponse.putExtra(EXTRA_STRING_DATA_LOAD_PROBLEM, sMessage);

    }




}
