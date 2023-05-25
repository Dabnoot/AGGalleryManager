package com.agcurations.aggallerymanager;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Map;

public class Worker_Catalog_RecalcCatalogItemsMaturityAndUsers extends Worker {

    public static final String TAG_WORKER_CATALOG_RECALC_APPROVED_USERS = "com.agcurations.aggallermanager.TAG_WORKER_CATALOG_RECALC_APPROVED_USERS";

    public static final String WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.WORKER_CATALOG_RECALC_APPROVED_USERS_RESPONSE";

    int giMediaCategory;


    public Worker_Catalog_RecalcCatalogItemsMaturityAndUsers(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);

    }

    @NonNull
    @Override
    public Result doWork() {

        if(giMediaCategory == -1){
            return Result.failure(DataErrorMessage("No media category passed to worker for" +
                    " recalc of catalog items approved users and maturity rating."));
        }

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        int iProgressDenominator;
        int iProgressNumerator = 0;
        int iProgressBarValue;
        globalClass.BroadcastProgress(false, "",
                true, 0,
                true, "Recalculating catalog item approved users...",
                WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);

        String sMessage;

        iProgressDenominator = GlobalClass.gtmCatalogLists.get(giMediaCategory).size();

        globalClass.BroadcastProgress(false, "",
                true, 0,
                true, "Recalculating " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog item approved users...",
                WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);

        for(Map.Entry<String, ItemClass_CatalogItem> icciCatalogItem: GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()){
            icciCatalogItem.getValue().alsApprovedUsers = GlobalClass.getApprovedUsersForTagGrouping(icciCatalogItem.getValue().aliTags, giMediaCategory); //This also takes into account the maturity rating of the tags.
            icciCatalogItem.getValue().iMaturityRating = GlobalClass.getHighestTagMaturityRating(icciCatalogItem.getValue().aliTags, giMediaCategory);

            iProgressNumerator++;
            if(iProgressNumerator % 100 == 0) {
                iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                globalClass.BroadcastProgress(false, "",
                        true, iProgressBarValue,
                        false,"",
                        WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);
            }
        }

        globalClass.BroadcastProgress(false, "",
                true, 100,
                true, "Writing " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog file...",
                WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);

        GlobalClass.CatalogDataFile_UpdateCatalogFile(giMediaCategory);

        globalClass.BroadcastProgress(false, "",
                true, 100,
                true, "Recalculation and update of catalog file record's approved-users completed.",
                WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);

        return Result.success();
    }


    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Catalog_RecalcCatalogItemsApprovedUsers:" + sRoutine, sMessage);
    }

    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }

}
