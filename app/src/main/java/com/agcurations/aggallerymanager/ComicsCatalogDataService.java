package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class ComicsCatalogDataService extends IntentService {

    // IntentService Actions:
    private static final String ACTION_LOAD_COMICS_CATALOG = "com.agcurations.aggallerymanager.action.LCC";
    //Parameters:
    public static final String EXTRA_BOOL_CATALOG_DATA_CHANGE = "com.agcurations.aggallerymanager.extra.BCDC";
    public static final String EXTRA_BOOL_DATA_IMPORT_PROBLEM = "com.agcurations.aggallerymanager.extra.BDIP";
    public static final String EXTRA_STRING_DATA_IMPORT_PROBLEM = "com.agcurations.aggallerymanager.extra.SDIP";

    //Global Variables:
    private GlobalClass globalClass;


    public ComicsCatalogDataService() {
        super("ComicCatalogDataService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     */
    public static void startActionLoadComicsCatalog(Context context) {
        Intent intent = new Intent(context, ComicsCatalogDataService.class);
        intent.setAction(ACTION_LOAD_COMICS_CATALOG);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        globalClass = (GlobalClass) getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LOAD_COMICS_CATALOG.equals(action)) {
                handleActionLoadComicsCatalog();
            }
        }
    }

    /**
     * Handle action LoadComicsCatalog in the provided background thread with the provided
     * parameters.
     */
    Intent broadcastIntent_LoadComicCatalogResponse; //Make global to allow for problem notification string extras.
    private void handleActionLoadComicsCatalog() {

        //Create a Broadcast intent to be sent at the end of the routine by the Import Activity:
        broadcastIntent_LoadComicCatalogResponse = new Intent();
        broadcastIntent_LoadComicCatalogResponse.setAction(ComicsCatalogActivity.CCDataServiceResponseReceiver.CCDATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent_LoadComicCatalogResponse.addCategory(Intent.CATEGORY_DEFAULT);


        File fCatalogComicsFolder = GlobalClass.gfComicsFolder;
        File fCatalogContentsFile = GlobalClass.gvfComicCatalogContentsFile;
        File fLogsFolder = GlobalClass.gfComicLogsFolder;


        boolean bFolderOk = false ;
        if(!fCatalogComicsFolder.exists()) {
            if (fCatalogComicsFolder.mkdirs()) {
                bFolderOk = true;
            }
        }else{
            bFolderOk = true;
        }

        if (!bFolderOk) {
            problemNotificationConfig("Could not create catalog data folder 'Comics'.");
        } else {

            if (!fCatalogContentsFile.exists()){
                try {
                    if(fCatalogContentsFile.createNewFile()) {
                        FileWriter fwCatalogContentsFile = null;
                        try {
                            fwCatalogContentsFile = new FileWriter(fCatalogContentsFile, true);

                            //Write the activity_comic_details_header line to the file:
                            fwCatalogContentsFile.append(GlobalClass.ComicRecordFields[0]);
                            for(int i = 1; i < GlobalClass.ComicRecordFields.length; i++) {
                                fwCatalogContentsFile.append("\t");
                                fwCatalogContentsFile.append("GlobalClass.ComicRecordFields[i]");
                            }
                            fwCatalogContentsFile.append("\n");

                        } catch (Exception e) {
                            problemNotificationConfig("Problem during CatalogContentsFile write.\n" + e.getMessage());

                        } finally {
                            try {
                                if (fwCatalogContentsFile != null) {
                                    fwCatalogContentsFile.flush();
                                    fwCatalogContentsFile.close();
                                }
                            } catch (IOException e) {
                                problemNotificationConfig("Problem during CatalogContentsFile flush/close.\n" + e.getMessage());

                            }
                        }
                    } else {
                        problemNotificationConfig("Could not create CatalogContents.dat at" + fCatalogComicsFolder.getAbsolutePath());
                    }
                }catch (IOException e){
                    problemNotificationConfig("Could not create CatalogContents.dat at" + fCatalogComicsFolder.getAbsolutePath());

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

            //Build the internal list of comics:
            TreeMap<Integer, String[]> tmCatalogComicList = new TreeMap<>();

            //Read the list of comics and populate the catalog array:
            BufferedReader brReader;
            try {
                brReader = new BufferedReader(new FileReader(fCatalogContentsFile.getAbsolutePath()));
                brReader.readLine(); //The first line is the activity_comic_details_header. Skip this line.
                String sLine = brReader.readLine();
                String[] sFields;
                int iComicRID = 0;
                while (sLine != null) {
                    //Split the line read from the contents file with the delimiter of TAB:
                    sFields = sLine.split("\t",-1);
                    tmCatalogComicList.put(iComicRID, sFields);

                    // read next line
                    sLine = brReader.readLine();
                    iComicRID++;
                }
                brReader.close();

            } catch (IOException e) {
                problemNotificationConfig("Trouble reading CatalogContents.dat at" + fCatalogComicsFolder.getAbsolutePath());
            }

            //Set the global variable holding the comic list:
            globalClass.gtmCatalogComicList = tmCatalogComicList;

            broadcastIntent_LoadComicCatalogResponse.putExtra(EXTRA_BOOL_CATALOG_DATA_CHANGE, true);


        }

        //Broadcast a message to be picked-up by the Import Activity:
        sendBroadcast(broadcastIntent_LoadComicCatalogResponse);
    }

    void problemNotificationConfig(String sMessage){
        broadcastIntent_LoadComicCatalogResponse.putExtra(EXTRA_BOOL_DATA_IMPORT_PROBLEM, true);
        broadcastIntent_LoadComicCatalogResponse.putExtra(EXTRA_STRING_DATA_IMPORT_PROBLEM, sMessage);

    }


    public void Catalog_data_file_add_field() {
        //Add the new field to GlobalClass.ComicRecordFields before running this routine.
        //  This will affect the creation of the dat file header.

        int iToVersion = 2; //This causes the routine to update the .dat file only once.

        File fCatalogContentsFile = GlobalClass.gvfComicCatalogContentsFile;

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


}
