package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.exoplayer2.util.MimeTypes;

public class Worker_Catalog_BackupCatalogDBFiles extends Worker {

    public static final String TAG_WORKER_CATALOG_BACKUPCATALOGDBFILES = "com.agcurations.aggallermanager.tag_worker_catalog_backupcatalogdbfiles";

    String gsResponseActionFilter;
    GlobalClass globalClass;

    public Worker_Catalog_BackupCatalogDBFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsResponseActionFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);


    }

    @NonNull
    @Override
    public Result doWork() {
        globalClass = (GlobalClass) getApplicationContext();

        Intent broadcastIntent = new Intent();
        boolean bProblem = false;


        int iProgressDenominator;
        int iProgressNumerator;
        int iProgressBarValue;

        Uri uriBackupFolder = GlobalClass.FormChildUri(GlobalClass.gUriDataFolder, "Backup");

        //Backup the catalog text files:
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){
            iProgressNumerator = 0;
            iProgressDenominator = GlobalClass.gtmCatalogLists.get(iMediaCategory).size();
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, "Backing up catalog data files...",
                    Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);

            StringBuilder sbBuffer = new StringBuilder();
            boolean bHeaderWritten = false;
            for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: GlobalClass.gtmCatalogLists.get(iMediaCategory).entrySet()){

                if(!bHeaderWritten) {
                    sbBuffer.append(GlobalClass.getCatalogHeader()); //Append the header.
                    sbBuffer.append("\n");
                    bHeaderWritten = true;
                }

                sbBuffer.append(GlobalClass.getCatalogRecordString(tmEntry.getValue())); //Append the data.
                sbBuffer.append("\n");

                iProgressNumerator++;
                if(iProgressNumerator % 100 == 0) {
                    iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                    globalClass.BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "Backing up " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " catalog data file.",
                            Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
                }
            }

            try {
                //Write the catalog file:
                String sDateTimeStamp = GlobalClass.GetTimeStampFileSafe();

                String sFileName = GlobalClass.gsCatalogFolderNames[iMediaCategory] + "_CatalogContents_" + sDateTimeStamp + ".dat";
                Uri uriBackupFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriBackupFolder, MimeTypes.BASE_TYPE_TEXT, sFileName);
                if(uriBackupFile != null){
                    OutputStream osBackupFile = GlobalClass.gcrContentResolver.openOutputStream(uriBackupFile);
                    if(osBackupFile != null) {
                        osBackupFile.write(sbBuffer.toString().getBytes(StandardCharsets.UTF_8));
                        osBackupFile.flush();
                        osBackupFile.close();
                    }
                }

            } catch (Exception e) {
                String sMessage = "Problem creating backup of CatalogContents.dat.\n" + e.getMessage();
                LogThis("doWork()", sMessage, null);
                broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                bProblem = true;
            }
        }

        //Backup the tag files:
        for(int i = 0; i < 3; i++){
            StringBuilder sbBuffer = new StringBuilder();
            boolean bHeaderWritten = false;
            for(Map.Entry<Integer, ItemClass_Tag> tmEntry: GlobalClass.gtmCatalogTagReferenceLists.get(i).entrySet()){

                if(!bHeaderWritten) {
                    sbBuffer.append(GlobalClass.getTagFileHeader()); //Append the header.
                    sbBuffer.append("\n");
                    bHeaderWritten = true;
                }

                sbBuffer.append(GlobalClass.getTagRecordString(tmEntry.getValue())); //Append the data.
                sbBuffer.append("\n");
            }

            try {
                //Write the tag file:
                String sDateTimeStamp = GlobalClass.GetTimeStampFileSafe();

                String sFileName = GlobalClass.gsCatalogFolderNames[i] + "_Tags_" + sDateTimeStamp + ".dat";
                Uri uriBackupFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriBackupFolder, MimeTypes.BASE_TYPE_TEXT, sFileName);
                if(uriBackupFile != null){
                    OutputStream osBackupFile = GlobalClass.gcrContentResolver.openOutputStream(uriBackupFile);
                    if(osBackupFile != null) {
                        osBackupFile.write(sbBuffer.toString().getBytes(StandardCharsets.UTF_8));
                        osBackupFile.flush();
                        osBackupFile.close();
                    }
                }

            } catch (Exception e) {
                String sMessage = "Problem creating backup of Tags.dat.\n" + e.getMessage();
                broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                bProblem = true;
            }
        }
        if(!bProblem) {
            String sMessage = "Database files backup completed.";
            broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_STATUS_MESSAGE, sMessage);
        }

        //Send broadcast to the Main Activity:
        broadcastIntent.setAction(Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

        globalClass.BroadcastProgress(false, "",
                true, 100,
                true, "Catalog Backup Complete",
                Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);

        return Result.success();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Catalog_BackupCatalogDBFiles:" + sRoutine, sMessage);
    }

}
