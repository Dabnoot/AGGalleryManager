package com.agcurations.aggallerymanager;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Import_DeleteFiles extends Worker {

    public static final String TAG_WORKER_IMPORT_DELETEFILES = "com.agcurations.aggallermanager.tag_worker_import_deletefiles";

    ArrayList<String> galsUriFilesToDelete;
    String gsCallerActionResponseFilter;

    public Worker_Import_DeleteFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        GlobalClass globalClass = (GlobalClass) context;
        galsUriFilesToDelete = new ArrayList<>(globalClass.alsUriFilesToDelete);
        gsCallerActionResponseFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        int iProgressNumerator = 0;
        int iProgressDenominator = galsUriFilesToDelete.size();
        int iProgressBarValue;

        Uri uriSourceFile;

        String sLogLine;
        boolean bDisplayLogMessage;
        String sProgressBarText;
        for(String sUriFileToDelete: galsUriFilesToDelete) {
            bDisplayLogMessage = false;
            sLogLine = GlobalClass.FILE_DELETION_MESSAGE + sUriFileToDelete + "...";
            uriSourceFile = Uri.parse(sUriFileToDelete);

            String sMessage;

            if (GlobalClass.CheckIfFileExists(uriSourceFile)) {
                try {
                    if (DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriSourceFile)) {
                        sLogLine = sLogLine + "Success.";
                        sMessage = "deleted.";
                    } else {
                        sLogLine = sLogLine + "Failed.";
                        sMessage = "could not be deleted.";
                        bDisplayLogMessage = true;
                    }
                } catch (Exception e){
                    sMessage = "could not be deleted.";
                    sLogLine = sLogLine + e.getMessage();
                    bDisplayLogMessage = true;
                }
            } else {
                sMessage = "was not found.";
            }

            iProgressNumerator++;
            sProgressBarText = "File " + iProgressNumerator + " of " + iProgressDenominator + " " + sMessage;
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(bDisplayLogMessage, sLogLine,
                    true, iProgressBarValue,
                    true, sProgressBarText,
                    gsCallerActionResponseFilter);
        }

        globalClass.BroadcastProgress(false, GlobalClass.FILE_DELETION_OP_COMPLETE_MESSAGE,
                true, 100,
                false, "",
                gsCallerActionResponseFilter);


        return Result.success();
    }

}
