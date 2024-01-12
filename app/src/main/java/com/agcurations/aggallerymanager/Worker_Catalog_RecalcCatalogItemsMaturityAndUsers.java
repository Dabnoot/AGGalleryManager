package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Map;

public class Worker_Catalog_RecalcCatalogItemsMaturityAndUsers extends Worker {

    public static final String TAG_WORKER_CATALOG_RECALC_APPROVED_USERS = "com.agcurations.aggallermanager.TAG_WORKER_CATALOG_RECALC_APPROVED_USERS";

    public static final String WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.WORKER_CATALOG_RECALC_APPROVED_USERS_RESPONSE";

    public static final String EXTRA_CATALOG_RECALC_COMPLETE = "com.agcurations.aggallerymanager.extra.CATALOG_RECALC_COMPLETE";

    int giMediaCategoriesToProcessBitSet;


    public Worker_Catalog_RecalcCatalogItemsMaturityAndUsers(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        giMediaCategoriesToProcessBitSet = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY_BIT_SET, -1);
        //binary    int     description
        //--------------------------
        //  001     1       just videos
        //  010     2       just images
        //  100     4       just comics
        //  011     3       v & i
        //  101     5       v & c
        //  110     6       i & c
        //  111     7       v, i, & c
    }

    @NonNull
    @Override
    public Result doWork() {

        GlobalClass.gabDataLoaded.set(false); //Don't let the user get into any catalog until processing is complete.

        if(giMediaCategoriesToProcessBitSet == -1){
            return Result.failure(DataErrorMessage("No media category passed to worker for" +
                    " recalc of catalog items approved users and maturity rating."));
        }

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        int iProgressDenominator = 0;
        int iProgressNumerator = 0;
        int iProgressBarValue;

        int[] iMediaCategoryBits = {1, 2, 4};
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++) {
            if((giMediaCategoriesToProcessBitSet & iMediaCategoryBits[iMediaCategory]) == iMediaCategoryBits[iMediaCategory]) {
                iProgressDenominator += GlobalClass.gtmCatalogLists.get(iMediaCategory).size();
            }
        }

        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++) {
            if ((giMediaCategoriesToProcessBitSet & iMediaCategoryBits[iMediaCategory]) == iMediaCategoryBits[iMediaCategory]) {
                globalClass.BroadcastProgress(false, "",
                        true, 0,
                        true, "Recalculating " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " catalog item approved users...",
                        WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);

                for (Map.Entry<String, ItemClass_CatalogItem> icciCatalogItem : GlobalClass.gtmCatalogLists.get(iMediaCategory).entrySet()) {
                    icciCatalogItem.getValue().alsApprovedUsers = GlobalClass.getApprovedUsersForTagGrouping(icciCatalogItem.getValue().aliTags, iMediaCategory); //This also takes into account the maturity rating of the tags.
                    icciCatalogItem.getValue().iMaturityRating = GlobalClass.getHighestTagMaturityRating(icciCatalogItem.getValue().aliTags, iMediaCategory);

                    iProgressNumerator++;
                    if (iProgressNumerator % 100 == 0) {
                        iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                        globalClass.BroadcastProgress(false, "",
                                true, iProgressBarValue,
                                false, "",
                                WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);
                    }
                }

                globalClass.BroadcastProgress(false, "",
                        true, 100,
                        true, "Writing " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " catalog file...",
                        WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);

                globalClass.CatalogDataFile_UpdateCatalogFile(iMediaCategory, "Applying permissions to " + GlobalClass.gsCatalogFolderNames[iMediaCategory] +
                        " catalog records...");
            }
        }
        globalClass.BroadcastProgress(false, "",
                true, 100,
                true, "Recalculation and update of catalog file record's approved-users completed.",
                WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);

        //Send a broadcast that this process is complete.
        Intent broadcastIntent_NotifyCatalogRecalcComplete = new Intent();
        broadcastIntent_NotifyCatalogRecalcComplete.putExtra(EXTRA_CATALOG_RECALC_COMPLETE, true);
        broadcastIntent_NotifyCatalogRecalcComplete.setAction(WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);
        broadcastIntent_NotifyCatalogRecalcComplete.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_NotifyCatalogRecalcComplete);

        GlobalClass.gabDataLoaded.set(true); //Allow the user back into catalog viewers.

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
