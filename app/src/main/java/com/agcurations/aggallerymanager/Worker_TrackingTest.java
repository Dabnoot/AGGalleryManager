package com.agcurations.aggallerymanager;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_TrackingTest extends Worker {

    //Define string used to identify this worker type:
    public static final String WORKER_TRACKING_TEST_TAG = "WORKER_TRACKING_TEST_TAG";

    public static final String WORKER_PROGRESS = "WORKER_PROGRESS";
    public static final String WORKER_ID = "WORKER_ID";
    public static final String WORKER_BYTES_PROCESSED = "WORKER_BYTES_PROCESSED";
    public static final String WORKER_BYTES_TOTAL = "WORKER_BYTES_TOTAL";

    public static final String KEY_ARG_JOB_REQUEST_DATETIME = "KEY_ARG_JOB_REQUEST_DATETIME";
    public static final String KEY_ARG_TOTAL_BYTES = "KEY_ARG_TOTAL_BYTES";

    private String gsJobRequestDateTime;
    private long glTotalBytes;

    public Worker_TrackingTest(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters) {
        super(appContext, workerParameters);

        gsJobRequestDateTime = getInputData().getString(KEY_ARG_JOB_REQUEST_DATETIME);
        glTotalBytes = getInputData().getLong(KEY_ARG_TOTAL_BYTES, 100);
    }


    @NonNull
    @Override
    public Result doWork() {

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), GlobalClass.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_arrow_circle_down_24)
                .setContentTitle("Notification Bar Test")
                .setContentText("Testing progress bar")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setProgress(100, 0, false);

        int iNotificationID = globalClass.iNotificationID;
        globalClass.iNotificationID++;
        Notification notification = notificationBuilder.build();
        globalClass.notificationManager.notify(iNotificationID, notification);

        Data data = new Data.Builder()
                .putInt(WORKER_PROGRESS, 0)
                .putString(WORKER_ID, gsJobRequestDateTime)
                .build();
        setProgressAsync(data);

        int iProgressBarValue;
        long lProgressNumerator = 0;
        long lProgressDenominator = glTotalBytes;
        while(lProgressNumerator <= glTotalBytes){
            try {
                Thread.currentThread();
                Thread.sleep(500);
            }catch (Exception e){
                Log.d("Test", "doWork: " + e.getMessage());
            }

            lProgressNumerator++; //Accumulate bytes transferred.

            iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
            notificationBuilder.setProgress(100, iProgressBarValue, (iProgressBarValue > 25 && iProgressBarValue < 50));

            if(lProgressNumerator == lProgressDenominator){
                notificationBuilder.setOngoing(false);
                // When done, update the notification one more time to remove the progress bar
                notificationBuilder.setContentText("Testing complete")
                        .setProgress(0,0,false);
            }
            notification = notificationBuilder.build();
            globalClass.notificationManager.notify(iNotificationID, notification);

            data = new Data.Builder()
                    .putLong(WORKER_BYTES_PROCESSED, lProgressNumerator)
                    .putLong(WORKER_BYTES_TOTAL, glTotalBytes)
                    .putString(WORKER_ID, gsJobRequestDateTime)
                    .build();
            setProgressAsync(data);

        }

        return Result.success();
    }


}
