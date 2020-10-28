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
        broadcastIntent_LoadComicCatalogResponse.setAction(MainActivity.CCDataServiceResponseReceiver.CCDATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent_LoadComicCatalogResponse.addCategory(Intent.CATEGORY_DEFAULT);


        //Broadcast a message to be picked-up by the Import Activity:
        sendBroadcast(broadcastIntent_LoadComicCatalogResponse);
    }

    void problemNotificationConfig(String sMessage){
        broadcastIntent_LoadComicCatalogResponse.putExtra(EXTRA_BOOL_DATA_IMPORT_PROBLEM, true);
        broadcastIntent_LoadComicCatalogResponse.putExtra(EXTRA_STRING_DATA_IMPORT_PROBLEM, sMessage);

    }





}
