package com.agcurations.aggallerymanager;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Catalog_Maintenance extends Worker {

    public static final String CATALOG_MAINTENANCE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_MAINTENANCE_ACTION_RESPONSE";

    static final String EXTRA_MAINTENANCE_TYPE = "com.agcurations.aggallerymanager.extra.EXTRA_MAINTENANCE_TYPE";

    int giMaintenanceType = -1;

    public Worker_Catalog_Maintenance(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        giMaintenanceType = getInputData().getInt(EXTRA_MAINTENANCE_TYPE, -1);

    }

    @NonNull
    @Override
    public Result doWork() {

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        String sMessage;


        //todo: report the time it took to complete the operation.

        sMessage = "Catalog Maintenance complete.";
        LogThis("doWork()", sMessage, null);
        GlobalClass.aiCatalogUpdateRunning.set(GlobalClass.FINISHED);
        globalClass.BroadcastProgress(false, "",
                true, 100,
                true, "Complete.",
                CATALOG_MAINTENANCE_ACTION_RESPONSE);

        return Result.success();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        globalClass.BroadcastProgress(true, sMessage,
                false, 0,
                false, "",
                CATALOG_MAINTENANCE_ACTION_RESPONSE);
        Log.d("Worker_Catalog_Verification:" + sRoutine, sMessage);
    }
}
