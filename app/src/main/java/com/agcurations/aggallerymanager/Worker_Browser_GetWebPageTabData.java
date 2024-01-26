package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
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
        Uri uriWebPageTabDataFile = GlobalClass.gUriWebpageTabDataFile;
        if(uriWebPageTabDataFile == null){
            return Result.failure(DataErrorMessage("No Uri defined for web page data file."));
        }

        //Debugging helper section:
        boolean bTestingCloseOfTabs = false;
        if(bTestingCloseOfTabs){
            boolean bFormReferenceTabFile = false;
            Uri uriReferenceFile = GlobalClass.FormChildUri(GlobalClass.gUriBrowserDataFolder, "WebPageTabDataRef.dat");
            if(uriReferenceFile != null) {
                if (bFormReferenceTabFile) {
                    //Create a reference tab file:
                    try {
                        DocumentsContract.copyDocument(GlobalClass.gcrContentResolver, uriReferenceFile, GlobalClass.gUriBrowserDataFolder);
                    } catch (Exception e) {
                        String sMessage = e.getMessage();
                        Log.d("Browser testing", sMessage);
                    }
                }
                //Copy the reference file of open tabs so that I don't have to keep opening them.
                try {
                    DocumentsContract.copyDocument(GlobalClass.gcrContentResolver, uriReferenceFile, GlobalClass.gUriBrowserDataFolder);
                } catch (Exception e) {
                    LogThis("doWork()", "Trouble copying browser data reference file to browser data folder.", e.getMessage());
                }
            }
        }

        //If the file does not exist, return.
        if(!GlobalClass.CheckIfFileExists(uriWebPageTabDataFile)) {
            return Result.failure(DataErrorMessage("Web page data file does not exist."));
        }

        //Read the file into memory.
        try {
            InputStream isWebPageTabDataFile = GlobalClass.gcrContentResolver.openInputStream(uriWebPageTabDataFile);
            if(isWebPageTabDataFile != null) {
                BufferedReader brReader;
                brReader = new BufferedReader(new InputStreamReader(isWebPageTabDataFile));
                brReader.readLine(); //Skip read of the file header.

                if (GlobalClass.gal_WebPagesForCurrentUser == null) {
                    GlobalClass.gal_WebPagesForCurrentUser = new ArrayList<>();
                } else {
                    GlobalClass.gal_WebPagesForCurrentUser.clear();
                }

                if (GlobalClass.gal_WebPagesForOtherUsers == null) {
                    GlobalClass.gal_WebPagesForOtherUsers = new ArrayList<>();
                } else {
                    GlobalClass.gal_WebPagesForOtherUsers.clear();
                }

                String sLine = brReader.readLine();
                while (sLine != null) {
                    ItemClass_WebPageTabData icwptd_DataRecordFromFile;
                    icwptd_DataRecordFromFile = GlobalClass.ConvertStringToWebPageTabData(sLine);
                    if(GlobalClass.gicuCurrentUser != null) {
                        if (icwptd_DataRecordFromFile.sUserName.equals(GlobalClass.gicuCurrentUser.sUserName)) {
                            GlobalClass.gal_WebPagesForCurrentUser.add(icwptd_DataRecordFromFile);
                        } else {
                            GlobalClass.gal_WebPagesForOtherUsers.add(icwptd_DataRecordFromFile);
                        }
                    } else {
                        GlobalClass.gal_WebPagesForOtherUsers.add(icwptd_DataRecordFromFile);
                    }
                    sLine = brReader.readLine();
                }
                brReader.close();
                isWebPageTabDataFile.close();

                if(GlobalClass.gicuCurrentUser == null && GlobalClass.gal_WebPagesForOtherUsers.size() > 0){
                    //Notify that a user is not signed-in. The user may have forgotten.
                    globalClass.problemNotificationConfig( "Current User: Default",
                            Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
                }
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

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Browser_GetWebPageTabData:" + sRoutine, sMessage);
    }

    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }


}
