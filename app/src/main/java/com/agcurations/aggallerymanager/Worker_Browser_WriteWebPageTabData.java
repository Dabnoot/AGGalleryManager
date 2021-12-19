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

    public Worker_Browser_WriteWebPageTabData(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

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
