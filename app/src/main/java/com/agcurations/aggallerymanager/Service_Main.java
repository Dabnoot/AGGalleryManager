package com.agcurations.aggallerymanager;

import android.content.Context;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


public class Service_Main {


    public static void startActionLoadData(Context context) {

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataCatalogLoadData = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_Main:onCreate()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrCatalogLoadData = new OneTimeWorkRequest.Builder(Worker_Catalog_LoadData.class)
                .setInputData(dataCatalogLoadData)
                .addTag(Worker_Catalog_LoadData.TAG_WORKER_CATALOG_LOADDATA) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrCatalogLoadData);
    }

    public static void startActionCatalogBackup(Context context) {

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataCatalogBackupCatalogDBFiles = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_Main:onCreate()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrCatalogBackupCatalogDBFiles = new OneTimeWorkRequest.Builder(Worker_Catalog_BackupCatalogDBFiles.class)
                .setInputData(dataCatalogBackupCatalogDBFiles)
                .addTag(Worker_Catalog_BackupCatalogDBFiles.TAG_WORKER_CATALOG_BACKUPCATALOGDBFILES) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrCatalogBackupCatalogDBFiles);
    }

}
