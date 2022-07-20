package com.agcurations.aggallerymanager;

import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


public class Service_CatalogViewer {

    public static void startActionDeleteCatalogItem(Context context, ItemClass_CatalogItem ciToDelete, String sCallerID, String sResponseActionFilter) {
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        String sCatalogRecord = GlobalClass.getCatalogRecordString(ciToDelete);
        Data dataCatalogDeleteItem = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(GlobalClass.EXTRA_CATALOG_ITEM, sCatalogRecord)
                .putString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER, sResponseActionFilter)
                .build();
        OneTimeWorkRequest otwrCatalogDeleteItem = new OneTimeWorkRequest.Builder(Worker_Catalog_DeleteItem.class)
                .setInputData(dataCatalogDeleteItem)
                .addTag(Worker_Catalog_DeleteItem.TAG_WORKER_CATALOG_DELETEITEM) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrCatalogDeleteItem);

    }

    public static void startActionUpdateCatalogItem(Context context, ItemClass_CatalogItem ciToUpdate, String sCallerID) {
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        String sCatalogRecord = GlobalClass.getCatalogRecordString(ciToUpdate);
        Data dataCatalogUpdateItem = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(GlobalClass.EXTRA_CATALOG_ITEM, sCatalogRecord)
                .build();
        OneTimeWorkRequest otwrCatalogUpdateItem = new OneTimeWorkRequest.Builder(Worker_Catalog_UpdateItem.class)
                .setInputData(dataCatalogUpdateItem)
                .addTag(Worker_Catalog_UpdateItem.TAG_WORKER_CATALOG_UPDATEITEM) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrCatalogUpdateItem);
    }

    public static void startActionSortAndFilterCatalogDisplay(Context context, String sCallerID) {
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataSortAndFilterCatalogDisplay = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrSortAndFilterCatalogDisplay = new OneTimeWorkRequest.Builder(Worker_CatalogViewer_SortAndFilterDisplayed.class)
                .setInputData(dataSortAndFilterCatalogDisplay)
                .addTag(Worker_CatalogViewer_SortAndFilterDisplayed.TAG_WORKER_CATALOGVIEWER_SORTANDFILTERDISPLAYED) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrSortAndFilterCatalogDisplay);
    }

}