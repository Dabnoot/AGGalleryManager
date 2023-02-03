package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Browser_GetWebPageTabData extends Worker {

    public static final String RESULT_TYPE_WEB_PAGE_TAB_DATA_ACQUIRED = "com.agcurations.webbrowser.result.WEB_PAGE_TAB_DATA_ACQUIRED";

    public Worker_Browser_GetWebPageTabData(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        //Get the webpage tab data file path:
        DocumentFile dfWebPageTabDataFile = globalClass.gdfWebpageTabDataFile;
        if(dfWebPageTabDataFile == null) return Result.failure();

        //Debugging helper section:
        boolean bTestingCloseOfTabs = false;
        if(bTestingCloseOfTabs){
            boolean bFormReferenceTabFile = false;
            DocumentFile dfReferenceFile = globalClass.gdfBrowserDataFolder.findFile("WebPageTabDataRef.dat");
            if(dfReferenceFile != null) {
                if (bFormReferenceTabFile) {
                    //Create a reference tab file:
                    try {
                        DocumentsContract.copyDocument(GlobalClass.gcrContentResolver, dfReferenceFile.getUri(), globalClass.gdfBrowserDataFolder.getUri());
                    } catch (Exception e) {
                        String sMessage = e.getMessage();
                        Log.d("Browser testing", sMessage);
                    }
                }
                //Copy the reference file of open tabs so that I don't have to keep opening them.
                try {
                    DocumentsContract.copyDocument(GlobalClass.gcrContentResolver, dfReferenceFile.getUri(), globalClass.gdfBrowserDataFolder.getUri());
                } catch (Exception e) {
                    String sMessage = e.getMessage();
                }
            }
        }

        //If the file does not exist, return.
        if(!dfWebPageTabDataFile.exists()) return Result.failure();

        //Read the file into memory.
        try {
            InputStream isWebPageTabDataFile = GlobalClass.gcrContentResolver.openInputStream(dfWebPageTabDataFile.getUri());
            if(isWebPageTabDataFile != null) {
                BufferedReader brReader;
                brReader = new BufferedReader(new InputStreamReader(isWebPageTabDataFile));
                brReader.readLine(); //Skip read of the file header.

                if (globalClass.gal_WebPages == null) {
                    globalClass.gal_WebPages = new ArrayList<>();
                } else {
                    globalClass.gal_WebPages.clear();
                }

                String sLine = brReader.readLine();
                while (sLine != null) {
                    ItemClass_WebPageTabData icwptd_DataRecordFromFile;
                    icwptd_DataRecordFromFile = GlobalClass.ConvertStringToWebPageTabData(sLine);
                    globalClass.gal_WebPages.add(icwptd_DataRecordFromFile);
                    sLine = brReader.readLine();
                }
                brReader.close();
                isWebPageTabDataFile.close();
            }
        } catch (Exception e) {
            globalClass.problemNotificationConfig( "Problem reading tab records from file: " + e.getMessage() + "\nSelect 'clear' from Settings->Browser.",
                    Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        }


        //Broadcast a message to be picked-up by the WebPage Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(GlobalClass.EXTRA_RESULT_TYPE, RESULT_TYPE_WEB_PAGE_TAB_DATA_ACQUIRED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);



        return Result.success();
    }





}
