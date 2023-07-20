package com.agcurations.aggallerymanager;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Browser_WriteWebPageTabData extends Worker {

    public static final String TAG_WORKER_BROWSER_WRITEWEBPAGETABDATA = "com.agcurations.aggallermanager.tag_worker_browser_writewebpagetabdata";


    String gsCallerID;
    Double gdCallerTimeStamp;

    final boolean bDebug = false;

    public Worker_Browser_WriteWebPageTabData(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsCallerID = getInputData().getString(GlobalClass.EXTRA_CALLER_ID);
        gdCallerTimeStamp = getInputData().getDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, -1);
    }

    @NonNull
    @Override
    public Result doWork() {

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

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        //Make sure it is this routine's turn to update data:
        String thisHash = UUID.randomUUID().toString();
        GlobalClass.queueWebPageTabDataFileWriteRequests.add(thisHash);
        int iLoopSleepMS = 20;
        int iMaxLoops = GlobalClass.giMaxDelayForWriteRequestMS / iLoopSleepMS;
        int i = 0;
        while (!GlobalClass.queueWebPageTabDataFileWriteRequests.peek().equals(thisHash) && i < iMaxLoops) {
            try {
                Thread.sleep(iLoopSleepMS);
            } catch (Exception e) {
                LogThis("doWork()", "Problem with call to sleep.", e.getMessage());
            }
            i++;
        }



        //Update the webpage tab data file:
        Uri uriWebPageTabDataFile = GlobalClass.gUriWebpageTabDataFile;
        if(!GlobalClass.CheckIfFileExists(uriWebPageTabDataFile)){
            return Result.failure();
        }

        //Re-write the data file completely because all of the indexes have changed:
        try {

            String sMessage;

            StringBuilder sbBuffer = new StringBuilder();

            String sHeader = GlobalClass.getWebPageTabDataFileHeader(); //Get updated header.
            sbBuffer.append(sHeader);
            sbBuffer.append("\n");

            //Combine list of web pages for current user with list of web pages for other users:

            for(ItemClass_WebPageTabData icwptd: GlobalClass.gal_WebPagesForCurrentUser){
                String sLine = GlobalClass.ConvertWebPageTabDataToString(icwptd);
                sbBuffer.append(sLine);
                sbBuffer.append("\n");
            }

            ArrayList<String> alsCurrentUserNames = new ArrayList<>();
            for(ItemClass_User icu: GlobalClass.galicu_Users){
                alsCurrentUserNames.add(icu.sUserName);
            }

            for(ItemClass_WebPageTabData icwptd: GlobalClass.gal_WebPagesForOtherUsers){
                //Check to make sure that user still exists (automatically handle case of deleted user):
                if(alsCurrentUserNames.contains(icwptd.sUserName)) {
                    String sLine = GlobalClass.ConvertWebPageTabDataToString(icwptd);
                    sbBuffer.append(sLine);
                    sbBuffer.append("\n");
                }
            }

            //Write the data to the file:
            OutputStream osNewWebPageStorageFile = GlobalClass.gcrContentResolver.openOutputStream(uriWebPageTabDataFile, "wt");
            if(osNewWebPageStorageFile == null){
                sMessage = "Could not open output stream to webpage tab data file.";
                return Result.failure(DataErrorMessage(sMessage));
            }
            BufferedWriter bwNewWebPageStorageFile = new BufferedWriter(new OutputStreamWriter(osNewWebPageStorageFile));
            String sBuffer = sbBuffer.toString();
            if(sBuffer.equals("")){
                sBuffer = "No tabs present due to call from " + gsCallerID + ".";
            }
            bwNewWebPageStorageFile.write(sBuffer);
            if(bDebug) Log.d("Worker_Browser_WriteWebPageTabData", "Writing Data: " + sBuffer);
            bwNewWebPageStorageFile.flush();
            bwNewWebPageStorageFile.close();
            osNewWebPageStorageFile.flush();
            osNewWebPageStorageFile.close();

        } catch (Exception e) {
            globalClass.problemNotificationConfig( e.getMessage(),
                    Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        }

        GlobalClass.queueWebPageTabDataFileWriteRequests.remove();


        return Result.success();
    }

    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Browser_WriteWebPageTabData:" + sRoutine, sMessage);
    }

}
