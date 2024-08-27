package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Worker_CatalogViewer_SortAndFilterGroup extends Worker {

    public static final String TAG_WORKER_CATALOGVIEWER_SORTANDFILTERGROUP = "com.agcurations.aggallermanager.tag_worker_catalogviewer_sortandfiltergroup";

    public static final String CATALOG_SORT_AND_FILTER_GROUP_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_SORT_AND_FILTER_GROUP_ACTION_RESPONSE";

    public static final String CATALOG_FILTER_EXTRA_STRING_GROUP_ID = "com.agcurations.aggallerymanager.intent.extra.CATALOG_FILTER_EXTRA_STRING_GROUP_ID";

    String gsGroupID;

    public Worker_CatalogViewer_SortAndFilterGroup(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        gsGroupID = workerParams.getInputData().getString(Worker_CatalogViewer_SortAndFilterGroup.CATALOG_FILTER_EXTRA_STRING_GROUP_ID);

    }

    @NonNull
    @Override
    public Result doWork() {
        //Ensure that group items are appropriate for the user. A particular group item could have a
        // private tag, or an elevated rating.

        //Create new TreeMap to presort the catalog items:
        TreeMap<String, ItemClass_CatalogItem> treeMapPreSort; //String = field being sorted, String = Catalog item data
        treeMapPreSort = new TreeMap<>();

        //Populate the Key field of the preSort TreeMap with SortBy field data, filtered and restricted if necessary:
        int iProgressNumerator = 1;
        int iProgressDenominator = GlobalClass.gtmCatalogLists.get(GlobalClass.giSelectedCatalogMediaCategory).size() + 1;
        int iProgressBarValue;

        String sKey;

        for (Map.Entry<String, ItemClass_CatalogItem>
                entry : GlobalClass.gtmCatalogLists.get(GlobalClass.giSelectedCatalogMediaCategory).entrySet()) {

            if(!GlobalClass.gsCatalogViewerSortBySharedWithUser.equals("")){
                //The user has requested a sort including only catalog items that are shared with a
                // specified user. This user could be theirself. If the ApprovedUsers list does not
                // contain the requested user name, skip the rest of the loop.
                String sRequestedUserNameShare = GlobalClass.gsCatalogViewerSortBySharedWithUser;
                if(!entry.getValue().alsApprovedUsers.contains(sRequestedUserNameShare)){
                    continue;
                }
            }

            sKey = entry.getValue().dDatetime_Import.toString() + "_" + entry.getValue().sItemID;

            //Filter by maturity, if selected by user:
            boolean bUserSelectedMaturityInBounds = false;
            if(entry.getValue().iMaturityRating >= GlobalClass.giMinContentMaturityFilter &&
                    entry.getValue().iMaturityRating <= GlobalClass.giMaxContentMaturityFilter){
                bUserSelectedMaturityInBounds = true;
            }

            //Check to see if the record needs to be skipped due to restriction settings:
            boolean bIsRestricted = false;

            //Filter by absolute maturity rating - user cannot see content beyond their user rating:
            int iMaturityRating = entry.getValue().iMaturityRating;
            if (GlobalClass.gicuCurrentUser != null) {
                if (iMaturityRating > GlobalClass.gicuCurrentUser.iMaturityLevel) {
                    bIsRestricted = true;
                }
            } else {
                if (iMaturityRating > GlobalClass.giDefaultUserMaturityRating) {
                    bIsRestricted = true;
                }
            }
            boolean bApprovedForThisUser = false;
            ArrayList<String> alsAssignedUsers = entry.getValue().alsApprovedUsers;
            if(alsAssignedUsers.size() > 0){
                if(GlobalClass.gicuCurrentUser != null) {
                    for (String sAssignedUser : alsAssignedUsers) {
                        if (GlobalClass.gicuCurrentUser.sUserName.equals(sAssignedUser)) {
                            bApprovedForThisUser = true;
                            break;
                        }
                    }
                }
            } else {
                bApprovedForThisUser = true;
            }


            boolean bGroupIDMatch = false;
            if(entry.getValue().sGroupID.equals(gsGroupID)){
                bGroupIDMatch = true;
                entry.getValue().bSearchByGroupID = true;
            }

            if(!bIsRestricted && bApprovedForThisUser){
                boolean bIsMatch;

                bIsMatch = bGroupIDMatch && bUserSelectedMaturityInBounds;

                if(bIsMatch){
                    treeMapPreSort.put(sKey, entry.getValue());
                }

            }


            iProgressNumerator++;

            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            GlobalClass globalClass;
            globalClass = (GlobalClass) getApplicationContext();
            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, iProgressBarValue + "%",
                    CATALOG_SORT_AND_FILTER_GROUP_ACTION_RESPONSE);

        }

        //TreeMap presort will auto-sort itself.

        //Clean up the key, apply a reverse sort order, if applicable:
        TreeMap<Integer, ItemClass_CatalogItem> tmNewOrderCatalogList = new TreeMap<>();
        int iRID, iIterator;
        if(GlobalClass.gbCatalogViewerSortAscending[GlobalClass.giSelectedCatalogMediaCategory]){
            iRID = 0;
            iIterator = 1;
        } else {
            iRID = treeMapPreSort.size();
            iIterator = -1;
        }

        /* //No need to refresh the progress here - it is pretty quick.
        iProgressNumerator = 0;
        iProgressDenominator = treeMapPreSort.size();*/
        for (Map.Entry<String, ItemClass_CatalogItem>
                entry : treeMapPreSort.entrySet()) {
            tmNewOrderCatalogList.put(iRID, entry.getValue());
            iRID += iIterator;
            /* //No need to show progress here - it is pretty quick.
            iProgressNumerator++;
            if(iProgressNumerator % 2 == 0) {
                iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                BroadcastProgress(true, iProgressBarValue,
                        true, iProgressNumerator + "/" + iProgressDenominator);
            }*/
        }

        GlobalClass.gtmCatalogViewerDisplayTreeMap = tmNewOrderCatalogList;

        //Broadcast the ready state of the SortAndFilterCatalogDisplay operation:
        Intent broadcastIntent_SortAndFilterCatalogDisplayResponse = new Intent();
        broadcastIntent_SortAndFilterCatalogDisplayResponse.putExtra(GlobalClass.EXTRA_BOOL_REFRESH_CATALOG_DISPLAY, true);
        broadcastIntent_SortAndFilterCatalogDisplayResponse.setAction(CATALOG_SORT_AND_FILTER_GROUP_ACTION_RESPONSE);
        broadcastIntent_SortAndFilterCatalogDisplayResponse.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_SortAndFilterCatalogDisplayResponse);

        return Result.success();
    }



}
