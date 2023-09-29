package com.agcurations.aggallerymanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Catalog_UpdateItem extends Worker {

    public static final String TAG_WORKER_CATALOG_UPDATEITEM = "com.agcurations.aggallermanager.tag_worker_catalog_updateitem";

    ItemClass_CatalogItem ciToUpdate;

    public Worker_Catalog_UpdateItem(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        String sCatalogItemToUpdate = getInputData().getString(GlobalClass.EXTRA_CATALOG_ITEM);
        if(sCatalogItemToUpdate != null) {
            ciToUpdate = GlobalClass.ConvertStringToCatalogItem(sCatalogItemToUpdate);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        if(ciToUpdate != null) {
            GlobalClass globalClass;
            globalClass = (GlobalClass) getApplicationContext();
            globalClass.CatalogDataFile_UpdateCatalogFile(
                    ciToUpdate.iMediaCategory,
                    "Updating " + GlobalClass.gsCatalogFolderNames[ciToUpdate.iMediaCategory] + " catalog...");
            return Result.success();
        } else {
            return Result.failure();
        }
    }



}
