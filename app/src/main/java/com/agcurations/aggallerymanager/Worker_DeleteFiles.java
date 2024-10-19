package com.agcurations.aggallerymanager;

import android.app.Notification;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.widget.Toast;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_DeleteFiles extends Worker {
    //This worker is for the generic deletion of files, such as log files.

    public static final String TAG_WORKER_DELETEFILES = "com.agcurations.aggallermanager.tag_worker_deletefiles";
    public static final String DELETE_FILES_RESPONSE = "com.agcurations.aggallerymanager.intent.action.DELETE_FILES_RESPONSE";

    public static final String FILE_DELETION_OP_COMPLETE_MESSAGE = "File deletion complete.";

    public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";

    //=========================
    // Define keys for arguments passed to this worker:
    public static final String KEY_ARG_JOB_REQUEST_DATETIME = "KEY_ARG_JOB_REQUEST_DATETIME";

    public static final String JOB_PROGRESS = "JOB_PROGRESS";

    public static final String JOB_DATETIME = "JOB_DATETIME";

    //=========================
    String gsJobRequestDateTime;    //Date/Time of job request for logging purposes.
    int giProgressNumerator = 0;
    int giProgressDenominator = 0;

    GlobalClass globalClass;
    int giNotificationID;
    Notification gNotification;
    NotificationCompat.Builder gNotificationBuilder;

    /**
     *
     * @param context           General context
     * @param workerParams      Worker parameters. Set KEY_ARG_JOB_REQUEST_DATETIME, and KEY_ARG_JOB_FILE.
     * This worker is for the generic deletion of files, such as log files.
     * Write job file with a header and job records.
     */
    public Worker_DeleteFiles(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        // Set initial progress to 0
        setProgressAsync(new Data.Builder().putInt(JOB_PROGRESS, 0).build());

        gsJobRequestDateTime = getInputData().getString(KEY_ARG_JOB_REQUEST_DATETIME);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public Result doWork() {

        Data dataProgress;

        globalClass = (GlobalClass) getApplicationContext();

        GlobalClass.gabGeneralFileDeletionRunning.set(true);

        //Get a copy of the list of log files from GlobalClass:
        ArrayList<ItemClass_File> alicf_FilesToDelete = (ArrayList<ItemClass_File>) GlobalClass.galicf_FilesToDeleteDataTransfer.clone();
        GlobalClass.galicf_FilesToDeleteDataTransfer.clear();

        //Count the files to be deleted:
        for(ItemClass_File icf: alicf_FilesToDelete){
            if(icf.bIsChecked){
                giProgressDenominator++;
            }
        }

        String sMessage;

        try {

            //Prepare a notification for the notification bar:
            String sNotificationTitle = "File deletion " + " job " + gsJobRequestDateTime + ".";
            String sNotificationText = giProgressNumerator + "/" + giProgressDenominator + " files deleted.";
            gNotificationBuilder = new NotificationCompat.Builder(getApplicationContext(), GlobalClass.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.copy)
                    .setContentTitle(sNotificationTitle)
                    .setContentText(sNotificationText)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOnlyAlertOnce(true) //Alert once and then update the notification silently.
                    .setOngoing(false) //Prevents the user from swiping it off the notification area.
                    .setProgress(100, 0, false);
            giNotificationID = GlobalClass.iNotificationID;
            GlobalClass.iNotificationID++;
            gNotification = gNotificationBuilder.build();
            GlobalClass.notificationManager.notify(giNotificationID, gNotification);

            //Build progress data associated with this worker:
            dataProgress = new Data.Builder()
                    .putString(JOB_PROGRESS, sNotificationText)
                    .putString(JOB_DATETIME, gsJobRequestDateTime)
                    .build();
            setProgressAsync(dataProgress);

            globalClass.BroadcastProgress(true, "Background worker processing files...\n\n",
                    true, 0,
                    true, sNotificationText,
                    DELETE_FILES_RESPONSE);

            globalClass.BroadcastProgress(false, "",
                    false, 0,
                    true, "Files processed: " + giProgressNumerator + "/" + giProgressDenominator,
                    DELETE_FILES_RESPONSE);

            String sLogLine;

            for(ItemClass_File icf: alicf_FilesToDelete){

                if (GlobalClass.gabGeneralFileDeletionCancel.get()) {
                    sMessage = "File deletion halted. " + giProgressNumerator + "/" + giProgressDenominator + " files processed.";
                    CloseNotification(sMessage);
                    GlobalClass.gabGeneralFileDeletionCancel.set(false);
                    GlobalClass.gabGeneralFileDeletionRunning.set(false);
                    globalClass.BroadcastProgress(true, sMessage,
                            false, 0,
                            false, "",
                            DELETE_FILES_RESPONSE);
                    return Result.success();
                }

                if(icf.bIsChecked) { //If file item marked for deletion...

                    //Get a user-friendly version of the source file path + filename:
                    String sSourceFileUri = GlobalClass.FormChildUri(icf.sUriParent, icf.sFileOrFolderName).toString();
                    Uri uriSourceFile = Uri.parse(sSourceFileUri);
                    String sUserFriendlySourceFileUri = sSourceFileUri;
                    for (Map.Entry<String, String> entryStorageDef : GlobalClass.gtmStorageDeviceNames.entrySet()) {
                        String sKey = entryStorageDef.getKey();
                        if (sKey.contains("/")) {
                            sKey = sKey.substring(sKey.lastIndexOf("/"));
                            sKey = sKey.replace("/", "");
                        }
                        if (sUserFriendlySourceFileUri.contains(sKey)) {
                            //Replace the cryptic storage location text with something the user is more likely to understand:
                            sUserFriendlySourceFileUri = sUserFriendlySourceFileUri.replace(sKey, entryStorageDef.getValue());
                            break;
                        }
                    }
                    if (sUserFriendlySourceFileUri.contains("/")) {
                        sUserFriendlySourceFileUri = sUserFriendlySourceFileUri.substring(sUserFriendlySourceFileUri.lastIndexOf("/"));
                        sUserFriendlySourceFileUri = sUserFriendlySourceFileUri.replace("/", "");
                    }
                    sUserFriendlySourceFileUri = URLDecoder.decode(sUserFriendlySourceFileUri, StandardCharsets.UTF_8.toString());
                    if (sUserFriendlySourceFileUri.contains(":")) {
                        sUserFriendlySourceFileUri = sUserFriendlySourceFileUri.replace(":", "://");
                    }


                    //If this source item is marked for deletion (no move or copy op to be performed), delete the source file:
                    boolean bDeleteSuccess;

                    bDeleteSuccess = DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriSourceFile);

                    sLogLine = "Deleting File: " + sUserFriendlySourceFileUri + " ...";
                    globalClass.BroadcastProgress(true, sLogLine,
                            false, 0,
                            false, "",
                            DELETE_FILES_RESPONSE);

                    if (!bDeleteSuccess) {
                        sLogLine = "File delete failed.\n";
                    } else {
                        sLogLine = "Success.\n";
                    }

                    giProgressNumerator++;
                    int iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    sNotificationText = giProgressNumerator + "/" + giProgressDenominator + " files processed.";
                    globalClass.BroadcastProgress(true, sLogLine + "\n",
                            true, iProgressBarValue,
                            true, sNotificationText,
                            DELETE_FILES_RESPONSE);

                    //Update the notification on the notification bar:
                    gNotificationBuilder.setContentText(sNotificationText)
                            .setProgress(100, iProgressBarValue, false);
                    gNotification = gNotificationBuilder.build();
                    GlobalClass.notificationManager.notify(giNotificationID, gNotification);

                    //Update the progress data associated with this worker:
                    dataProgress = new Data.Builder()
                            .putString(JOB_PROGRESS, sNotificationText)
                            .putString(JOB_DATETIME, gsJobRequestDateTime)
                            .build();
                    setProgressAsync(dataProgress);

                }

            }

            CloseNotification(FILE_DELETION_OP_COMPLETE_MESSAGE);

            sLogLine = "Operation complete. " + giProgressNumerator + "/" + giProgressDenominator + " files processed.";
            globalClass.BroadcastProgress(true, sLogLine + "\n",
                    false, 0,
                    true, sNotificationText,
                    DELETE_FILES_RESPONSE);

            globalClass.BroadcastProgress(false, FILE_DELETION_OP_COMPLETE_MESSAGE,
                    true, 100,
                    false, "",
                    DELETE_FILES_RESPONSE);

            GlobalClass.gabGeneralFileDeletionRunning.set(false);

            return Result.success();

        } catch (Exception e){
            sMessage = e.getMessage();
            if(sMessage == null){
                sMessage = "Null message";
            }
            sMessage = "File deletion halted. " + sMessage + "\n" + giProgressNumerator + "/" + giProgressDenominator + " files processed.";

            CloseNotification(sMessage);
            globalClass.BroadcastProgress(true, sMessage + "\n",
                    false, 0,
                    false, "",
                    DELETE_FILES_RESPONSE);

            dataProgress = new Data.Builder()
                    .putString(JOB_PROGRESS, sMessage)
                    .putString(JOB_DATETIME, gsJobRequestDateTime)
                    .putString(FAILURE_MESSAGE, sMessage)
                    .build();

            GlobalClass.gabGeneralFileDeletionRunning.set(false);

            return Result.failure(dataProgress);
        }


    }

    private void CloseNotification(String sMessage){
        gNotificationBuilder.setOngoing(false) //Let the user remove the notification from the notification bar.
                .setProgress(0, 0,false); //Remove the progress bar from the notification.
        gNotification = gNotificationBuilder.build();
        GlobalClass.notificationManager.notify(giNotificationID, gNotification);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Run your task here
                Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }


}
