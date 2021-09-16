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

    public static final String KEY_ARG_JOB_REQUEST_DATETIME = "KEY_ARG_JOB_REQUEST_DATETIME";

    private int giNotificationID;

    public Worker_TrackingTest(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters) {
        super(appContext, workerParameters);
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

        giNotificationID = globalClass.iNotificationID;
        globalClass.iNotificationID++;
        Notification notification = notificationBuilder.build();
        globalClass.notificationManager.notify(giNotificationID, notification);

        Data data = new Data.Builder()
                .putInt(WORKER_PROGRESS, 0)
                .putString(WORKER_ID, "Worker_ID_Test_Data")
                .build();
        setProgressAsync(data);

        int i = 0;
        while(i <= 100){
            try {
                Thread.currentThread();
                Thread.sleep(500);
            }catch (Exception e){
                Log.d("Test", "doWork: " + e.getMessage());
            }
            i+=10;
            notificationBuilder.setProgress(100, i, (i > 25 && i < 50));
            if(i == 100){
                notificationBuilder.setOngoing(false);
                // When done, update the notification one more time to remove the progress bar
                notificationBuilder.setContentText("Testing complete")
                        .setProgress(0,0,false);
            }
            notification = notificationBuilder.build();
            globalClass.notificationManager.notify(giNotificationID, notification);

            data = new Data.Builder()
                    .putInt(WORKER_PROGRESS, i)
                    .putString(WORKER_ID, "Worker_ID_Test_Data")
                    .build();
            setProgressAsync(data);

        }

        return Result.success();
    }


}
