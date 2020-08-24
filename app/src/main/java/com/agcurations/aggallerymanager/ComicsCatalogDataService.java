package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
            } /*else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }*/
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


        File fCatalogComicsFolder;
        File fCatalogContentsFile;
        File fLogsFolder;

        String sExternalStorageState;
        sExternalStorageState = Environment.getExternalStorageState();
        if (sExternalStorageState.equals(Environment.MEDIA_MOUNTED) ){

            // Get the NHComicManager directory that's inside the app-specific directory on
            // external storage, or create it.
            boolean bFolderOk = false ;

            File[] fAvailableDirs = getExternalFilesDirs(null);
            if (fAvailableDirs.length == 2) {
                //Create the folder on the likely SDCard:
                fCatalogComicsFolder = new File(fAvailableDirs[1] + File.separator + "Comics");
            }else{
                //Create the folder on the likely Internal storage.
                fCatalogComicsFolder = new File(fAvailableDirs[1] + File.separator + "Comics");
            }

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

                //Set the global variable holding the catalog comics folder:
                globalClass.setCatalogComicsFolder(fCatalogComicsFolder);

                //Look for the catalog status file:
                fCatalogContentsFile = new File(fCatalogComicsFolder.getAbsolutePath() + File.separator + "CatalogContents.dat");
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
                if(fCatalogContentsFile.exists()){
                    //If the catalog contents file exists, set the global variable:
                    globalClass.setCatalogContentsFile(fCatalogContentsFile);

                    /*//Process any modifications to the CatalogContentsFile:
                    String[] sNewFields = new String[]{
                            "COMIC_SOURCE",
                            "COMIC_DATETIME_LAST_READ_BY_USER",
                            "COMIC_DATETIME_IMPORT"
                    };
                    Catalog_data_file_add_fields(sNewFields,1);
*/
                    /*int[] iFields = new int[]{
                            GlobalClass.COMIC_DATETIME_IMPORT
                    };

                    String[] sUpdateData = new String[]{
                            "0"
                    };
                    globalClass.CatalogDataFile_UpdateAllRecords(iFields,sUpdateData);*/

                }

                //Look for the Logs folder. If it does not exist, create it.
                fLogsFolder = new File(fCatalogComicsFolder + File.separator + "Logs");
                if(!fLogsFolder.exists()) {
                    if(!fLogsFolder.mkdirs()){
                        problemNotificationConfig("Could not create log folder at" + fLogsFolder.getAbsolutePath());
                    }
                }
                if(fLogsFolder.exists()) {
                    globalClass.setLogsFolder(fLogsFolder);
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
                globalClass.setCatalogComicList(tmCatalogComicList);

                broadcastIntent_LoadComicCatalogResponse.putExtra(EXTRA_BOOL_CATALOG_DATA_CHANGE, true);

            }

        } else {
            problemNotificationConfig("No SD card mounted.");
        }


        //Broadcast a message to be picked-up by the Import Activity:
        sendBroadcast(broadcastIntent_LoadComicCatalogResponse);
    }

    void problemNotificationConfig(String sMessage){
        broadcastIntent_LoadComicCatalogResponse.putExtra(EXTRA_BOOL_DATA_IMPORT_PROBLEM, true);
        broadcastIntent_LoadComicCatalogResponse.putExtra(EXTRA_STRING_DATA_IMPORT_PROBLEM, sMessage);

    }

}
