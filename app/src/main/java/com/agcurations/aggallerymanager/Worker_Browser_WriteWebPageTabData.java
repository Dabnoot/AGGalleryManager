package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static com.agcurations.aggallerymanager.Service_WebPageTabs.EXTRA_RESULT_TYPE;
import static com.agcurations.aggallerymanager.Service_WebPageTabs.RESULT_TYPE_WEB_PAGE_TAB_DATA_ACQUIRED;

public class Worker_Browser_WriteWebPageTabData extends Worker {

    public static final String TAG_WORKER_BROWSER_WRITEWEBPAGETABDATA = "com.agcurations.aggallermanager.tag_worker_browser_writewebpagetabdata";
    public static final String EXTRA_CALLER_ID = "com.agcurations.aggallermanager.string_caller_id";
    public static final String EXTRA_CALLER_TIMESTAMP = "com.agcurations.aggallermanager.long_caller_timestamp";

    String gsCallerID;
    Double gdCallerTimeStamp;

    public Worker_Browser_WriteWebPageTabData(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsCallerID = getInputData().getString(EXTRA_CALLER_ID);
        gdCallerTimeStamp = getInputData().getDouble(EXTRA_CALLER_TIMESTAMP, -1);
    }

    @NonNull
    @Override
    public Result doWork() {

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        //If caller timestamp is old, the Android system may have tried to restart the worker.
        //If so, finish this worker as failed and abandon.
        Double dCurrentTimeStamp = GlobalClass.GetTimeStampDouble(); //yyyyMMdd.HHmmss
        double dTimeDiff = dCurrentTimeStamp - gdCallerTimeStamp;
        double dOneSecond = 0.000001;
        double dTimeOut = 5.0 * dOneSecond;
        if(gdCallerTimeStamp < 0 ||
           dTimeDiff > dTimeOut){
            return Result.failure();
        }

        //Make sure it is this routine's turn to update data:
        String thisHash = UUID.randomUUID().toString();
        GlobalClass.queueWebPageTabDataFileWriteRequests.add(thisHash);
        int iLoopSleepMS = 20;
        int iMaxLoops = GlobalClass.giMaxDelayForWriteRequestMS / iLoopSleepMS;
        int i = 0;
        while (!GlobalClass.queueWebPageTabDataFileWriteRequests.peek().equals(thisHash) && i < iMaxLoops){
            try {
                Thread.sleep(iLoopSleepMS);
            } catch (Exception e){

            }
            i++;
        }


        //Update the webpage tab data file:
        File fWebPageTabDataFile = globalClass.gfWebpageTabDataFile;
        if(fWebPageTabDataFile == null) return Result.failure();

        //Re-write the data file completely because all of the indexes have changed:
        try {

            StringBuilder sbBuffer = new StringBuilder();

            String sHeader = GlobalClass.getWebPageTabDataFileHeader(); //Get updated header.
            sbBuffer.append(sHeader);
            sbBuffer.append("\n");

            for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages){
                String sLine = GlobalClass.ConvertWebPageTabDataToString(icwptd);
                sbBuffer.append(sLine);
                sbBuffer.append("\n");
            }

            //Write the data to the file:
            FileWriter fwNewWebPageStorageFile = new FileWriter(fWebPageTabDataFile, false);
            String sBuffer = sbBuffer.toString();
            if(sBuffer == null || sBuffer.equals("")){
                sBuffer = "No tabs present due to call from " + gsCallerID + ".";
            }
            fwNewWebPageStorageFile.write(sbBuffer.toString());
            fwNewWebPageStorageFile.flush();
            fwNewWebPageStorageFile.close();

        } catch (Exception e) {
            GlobalClass.problemNotificationConfig( e.getMessage(),
                    Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE,
                    getApplicationContext());
        }

        //Broadcast a message to be picked-up by the WebPage Activity:
        /*Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(EXTRA_RESULT_TYPE, RESULT_TYPE_WEB_PAGE_TAB_CLOSED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);*/

        GlobalClass.queueWebPageTabDataFileWriteRequests.remove();


        return Result.success();
    }





}
