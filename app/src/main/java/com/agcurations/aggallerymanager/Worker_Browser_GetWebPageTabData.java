package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static com.agcurations.aggallerymanager.Service_WebPageTabs.EXTRA_RESULT_TYPE;
import static com.agcurations.aggallerymanager.Service_WebPageTabs.RESULT_TYPE_WEB_PAGE_TAB_DATA_ACQUIRED;

public class Worker_Browser_GetWebPageTabData extends Worker {

    public Worker_Browser_GetWebPageTabData(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        //Get the webpage tab data file path:
        File fWebPageTabDataFile = globalClass.gfWebpageTabDataFile;
        if(fWebPageTabDataFile == null) return Result.failure();

        //Debugging helper section:
        boolean bTestingCloseOfTabs = false;
        if(bTestingCloseOfTabs){
            boolean bFormReferenceTabFile = false;
            File fReferenceFile = new File(globalClass.gfBrowserDataFolder.getPath() + File.separator + "WebPageTabDataRef.dat");
            if(bFormReferenceTabFile){
                //Create a reference tab file:
                try {
                    Files.copy(fWebPageTabDataFile.toPath(), fReferenceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e){
                    String sMessage = e.getMessage();
                }
            }
            //Copy the reference file of open tabs so that I don't have to keep opening them.
            try {
                Files.copy(fReferenceFile.toPath(), fWebPageTabDataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e){
                String sMessage = e.getMessage();
            }

        }

        //If the file does not exist, return.
        if(!fWebPageTabDataFile.exists()) return Result.failure();

        //Read the file into memory.
        try {

            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fWebPageTabDataFile.getAbsolutePath()));

            brReader.readLine(); //Skip read of the file header.

            if(globalClass.gal_WebPages == null){
                globalClass.gal_WebPages = new ArrayList<>();
            } else {
                globalClass.gal_WebPages.clear();
            }

            String sLine = brReader.readLine();
            while (sLine != null) {

                ItemClass_WebPageTabData icwptd_DataRecordFromFile;
                icwptd_DataRecordFromFile = GlobalClass.ConvertStringToWebPageTabData(sLine);
                globalClass.gal_WebPages.add(icwptd_DataRecordFromFile);

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();


        } catch (Exception e) {
            GlobalClass.problemNotificationConfig( "Problem reading tab records from file: " + e.getMessage() + "\nSelect 'clear' from Settings->Browser.",
                    Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE,
                    getApplicationContext());
        }


        //Broadcast a message to be picked-up by the WebPage Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(EXTRA_RESULT_TYPE, RESULT_TYPE_WEB_PAGE_TAB_DATA_ACQUIRED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);



        return Result.success();
    }





}
