package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_CatalogViewer_SortAndFilterDisplayed extends Worker {

    public static final String TAG_WORKER_CATALOGVIEWER_SORTANDFILTERDISPLAYED = "com.agcurations.aggallermanager.tag_worker_catalogviewer_sortandfilterdisplayed";

    public static final String CATALOG_SORT_AND_FILTER_DISP_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_SORT_AND_FILTER_DISP_ACTION_RESPONSE";

    public Worker_CatalogViewer_SortAndFilterDisplayed(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        //First look for any download items requiring post-processing:
        //todo: Below item removed as this should now be fully executed by the download post-processor worker
        //globalClass.ExecuteDownloadManagerPostProcessing();

        //Apply the sort field.
        //Copy over only items that match a filter, if applied.
        //Copy over only non-restricted catalog items, if necessary.
        //Sort the TreeMap.

        //Create new TreeMap to presort the catalog items:
        TreeMap<String, ItemClass_CatalogItem> treeMapPreSort; //String = field being sorted, String = Catalog item data
        treeMapPreSort = new TreeMap<>();

        //Populate the Key field of the preSort TreeMap with SortBy field data, filtered and restricted if necessary:
        int iProgressNumerator = 1;
        int iProgressDenominator = GlobalClass.gtmCatalogLists.get(GlobalClass.giSelectedCatalogMediaCategory).size() + 1;
        int iProgressBarValue;

        String sKey;

        //Prepare resolution or page count filter bounds if necessary:
        //Do bounds calcs before the for loop to reduce undue burden.
        boolean bResolutionOrPageCountFilterApplicable = false;
        boolean bVideoDurationFilterApplicable = false;
        Integer iResMin = null;
        Integer iResMax = null;
        int iPageMin = 0;
        int iPageMax = GlobalClass.giMaxComicPageCount;
        long lDurationMin = Math.max(0, GlobalClass.glMinVideoDurationMSSelected);
        long lDurationMax = GlobalClass.glMaxVideoDurationMSSelected;
        switch (GlobalClass.giSelectedCatalogMediaCategory){
            case GlobalClass.MEDIA_CATEGORY_VIDEOS:

                if(GlobalClass.glMinVideoDurationMSSelected > -1 || GlobalClass.glMaxVideoDurationMSSelected > -1){
                    bVideoDurationFilterApplicable = true;
                    if(lDurationMax == -1){
                        lDurationMax = GlobalClass.glMaxVideoDurationMS;
                    }
                }

                if(GlobalClass.giMinVideoResolutionSelected == -1 && GlobalClass.giMaxVideoResolutionSelected == -1){
                    break;
                } else {
                    bResolutionOrPageCountFilterApplicable = true;
                    iResMin = Math.max(0, GlobalClass.giMinVideoResolutionSelected);
                    iResMax = GlobalClass.giMaxVideoResolutionSelected;
                    if(iResMax == -1){
                        iResMax = GlobalClass.gtmVideoResolutions.size() - 1;
                    }
                    iResMin = GlobalClass.gtmVideoResolutions.get(iResMin);
                    iResMax = GlobalClass.gtmVideoResolutions.get(iResMax);
                    if(iResMin == null || iResMax == null){
                        bResolutionOrPageCountFilterApplicable = false;
                        break;
                    }
                }
                break;
            case GlobalClass.MEDIA_CATEGORY_IMAGES:
                if(GlobalClass.giMinImageMegaPixelsSelected == -1 && GlobalClass.giMaxImageMegaPixelsSelected == -1){
                    break;
                } else {
                    bResolutionOrPageCountFilterApplicable = true;
                    iResMin = Math.max(0, GlobalClass.giMinImageMegaPixelsSelected);
                    iResMax = GlobalClass.giMaxImageMegaPixelsSelected;
                    if(iResMax == -1){
                        iResMax = GlobalClass.giMaxImageMegaPixels;
                    }
                }
                break;
            case GlobalClass.MEDIA_CATEGORY_COMICS:
                if(GlobalClass.giMinComicPageCountSelected == -1 && GlobalClass.giMaxComicPageCountSelected == -1){
                    break;
                } else {
                    bResolutionOrPageCountFilterApplicable = true;
                    iPageMin = Math.max(0, GlobalClass.giMinComicPageCountSelected);
                    iPageMax = GlobalClass.giMaxComicPageCountSelected;
                    if(iPageMax == -1){
                        iPageMax = GlobalClass.giMaxComicPageCount;
                    }
                }
                break;
        }


        boolean bSearchMatchApplicable = false;


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

            sKey = "";
            //Create a unique key to identify the record in the TreeMap, which includes
            // the SortBy field. TreeMap automatically sorts by the Key field.
            if(GlobalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_LAST_VIEWED){
                sKey = entry.getValue().dDatetime_Last_Viewed_by_User.toString();
            } else if(GlobalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_IMPORTED){
                sKey = entry.getValue().dDatetime_Import.toString();
            }
            sKey = sKey + entry.getValue().sItemID;


            //Apply a search if requested - build a string out of the records contents, and if a
            //  search is to be applied, check for a match. If no match, don't add the record to
            //  the TreeMap destined for the RecyclerView:
            boolean bSearchMatch = false;
            String sSearchInText_LowerCase = GlobalClass.gsCatalogViewerSearchInText[GlobalClass.giSelectedCatalogMediaCategory].toLowerCase();
            if(!sSearchInText_LowerCase.equals("")) {
                bSearchMatchApplicable = true;
                //Append all of the field data and search the resulting
                //  string for a filter match:
                String sKey_RecordText =
                        entry.getValue().sTitle + " " +
                                entry.getValue().sComicArtists + " " +
                                entry.getValue().sComicCharacters + " " +
                                entry.getValue().sComicParodies + " " +
                                entry.getValue().sItemID + " " +
                                entry.getValue().sFilename + " " +
                                GlobalClass.JumbleFileName(entry.getValue().sFilename);
                sKey_RecordText = sKey_RecordText.toLowerCase();
                if (sKey_RecordText.contains(sSearchInText_LowerCase)) {
                    bSearchMatch = true;
                }
            }

            //Apply a filter if requested, and if a
            //  filter is to be applied, check for a match. If no match, don't add the record to
            //  the TreeMap destined for the RecyclerView:
            boolean bFilterByApplicable = false;
            boolean bIsFilterByMatch = false;
            if(GlobalClass.giCatalogViewerFilterBySelection[GlobalClass.giSelectedCatalogMediaCategory] != GlobalClass.FILTER_BY_NO_SELECTION) {
                bFilterByApplicable = true;

                switch (GlobalClass.giCatalogViewerFilterBySelection[GlobalClass.giSelectedCatalogMediaCategory]) {
                    case GlobalClass.FILTER_BY_WEBSOURCE:
                        if (entry.getValue().sSource.startsWith("http")) {
                            bIsFilterByMatch = true;
                        }
                        break;
                    case GlobalClass.FILTER_BY_FOLDERSOURCE:
                        if (entry.getValue().sSource.equals("") ||
                                entry.getValue().sSource.equals(ItemClass_CatalogItem.FOLDER_SOURCE)) {
                            bIsFilterByMatch = true;
                        }
                        break;
                    case GlobalClass.FILTER_BY_NOTAGS:
                        if (entry.getValue().aliTags.size() == 0) {
                            bIsFilterByMatch = true;
                        }
                        break;
                    case GlobalClass.FILTER_BY_ITEMPROBLEM:
                        if ((entry.getValue().iAllVideoSegmentFilesDetected == ItemClass_CatalogItem.VIDEO_SEGMENT_FILES_KNOWN_INCOMPLETE)
                            || (!entry.getValue().sComic_Missing_Pages.equals(""))) {
                            bIsFilterByMatch = true;
                        }
                        break;

                }

            }

            //Filter on resolution or page count, as applicable:
            boolean bResolutionOrPageCountMatch = false;
            if(bResolutionOrPageCountFilterApplicable) {
                switch (GlobalClass.giSelectedCatalogMediaCategory) {
                    case GlobalClass.MEDIA_CATEGORY_VIDEOS:
                        if (iResMin == null || iResMax == null) {
                            bResolutionOrPageCountMatch = true;
                            break;
                        }
                        if ((entry.getValue().iHeight >= iResMin)
                                && entry.getValue().iHeight <= iResMax) {
                            bResolutionOrPageCountMatch = true;
                        }
                        break;
                    case GlobalClass.MEDIA_CATEGORY_IMAGES:
                        if (iResMin == null || iResMax == null) {
                            bResolutionOrPageCountMatch = true;
                            break;
                        }
                        int iHeight = entry.getValue().iHeight;
                        int iWidth = entry.getValue().iWidth;
                        int iMegaPixels = (iHeight * iWidth) / 1000000; //todo: carry float here.
                        if ((iMegaPixels >= iResMin)
                                && iMegaPixels <= iResMax) {
                            bResolutionOrPageCountMatch = true;
                        }
                        break;
                    case GlobalClass.MEDIA_CATEGORY_COMICS:
                        int iPageCount = entry.getValue().iComicPages;
                        if ((iPageCount >= iPageMin)
                                && iPageCount <= iPageMax) {
                            bResolutionOrPageCountMatch = true;
                        }
                        break;
                }
            }

            //Filter on video duration, as applicable:
            boolean bVideoDurationMatch = false;
            if(bVideoDurationFilterApplicable){
                long lVideoDuration = entry.getValue().lDuration_Milliseconds;
                if((lVideoDuration >= lDurationMin)
                        && lVideoDuration <= lDurationMax){
                    bVideoDurationMatch = true;
                }
            }

            //Filter by maturity, if selected by user:
            boolean bUserSelectedMaturityInBounds = false;
            if(entry.getValue().iMaturityRating >= GlobalClass.giMinContentMaturityFilter &&
                    entry.getValue().iMaturityRating <= GlobalClass.giMaxContentMaturityFilter){
                bUserSelectedMaturityInBounds = true;
            }


            boolean bTagMatchApplicable = false;
            boolean bTagsMatch = false;
            //Check to see if the user wants to filter by tag:
            if(GlobalClass.galtsiCatalogViewerFilterTags != null){
                if(GlobalClass.galtsiCatalogViewerFilterTags.get(GlobalClass.giSelectedCatalogMediaCategory).size() > 0){
                    bTagMatchApplicable = true;
                    if(entry.getValue().aliTags.containsAll(GlobalClass.galtsiCatalogViewerFilterTags.get(GlobalClass.giSelectedCatalogMediaCategory))){
                        bTagsMatch = true;
                    }
                }

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

            boolean bGroupIDFilterApplicable;
            boolean bGroupIDMatch = false;
            //If the global group ID filter for this media category has data, turn on this filter:
            bGroupIDFilterApplicable = !GlobalClass.gsCatalogViewerSearchByGroupID[GlobalClass.giSelectedCatalogMediaCategory].equals("");
            if(bGroupIDFilterApplicable){
                if(entry.getValue().sGroupID.equals(GlobalClass.gsCatalogViewerSearchByGroupID[GlobalClass.giSelectedCatalogMediaCategory])){
                    bGroupIDMatch = true;
                    entry.getValue().bSearchByGroupID = true;
                }
            } else {
                if(entry.getValue().bSearchByGroupID){
                    entry.getValue().bSearchByGroupID = false;
                }
            }


            if(!bIsRestricted && bApprovedForThisUser){
                boolean bIsMatch = true;

                if(bSearchMatchApplicable){
                    bIsMatch = bSearchMatch;
                }
                if(bFilterByApplicable){
                    bIsMatch = bIsMatch && bIsFilterByMatch;
                }
                if(bResolutionOrPageCountFilterApplicable){
                    bIsMatch = bIsMatch && bResolutionOrPageCountMatch;
                }
                if(bVideoDurationFilterApplicable){
                    bIsMatch = bIsMatch && bVideoDurationMatch;
                }
                if(bTagMatchApplicable){
                    bIsMatch = bIsMatch && bTagsMatch;
                }
                if(bGroupIDFilterApplicable){
                    bIsMatch = bIsMatch && bGroupIDMatch;
                }

                bIsMatch = bIsMatch && bUserSelectedMaturityInBounds;

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
                    CATALOG_SORT_AND_FILTER_DISP_ACTION_RESPONSE);

        }

        if(!bSearchMatchApplicable) {
            if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) { //Only do this for comics at this time.
                //Hide all but first of group items, so long as the user is not explictly searching for text:
                ArrayList<String> alsUniqueGroupIDs = new ArrayList<>();

                for (Map.Entry<String, ItemClass_CatalogItem>
                        entry : treeMapPreSort.entrySet()) {
                    if (!entry.getValue().sGroupID.equals("")) {
                        if (!alsUniqueGroupIDs.contains(entry.getValue().sGroupID)) {
                            alsUniqueGroupIDs.add(entry.getValue().sGroupID);
                        }
                    }
                }
                //All group IDs identified.
                ArrayList<String> alsGroupMembersToHide = new ArrayList<>();
                for (String sGroupID : alsUniqueGroupIDs) {
                    double dLastItemDateTime = 0;
                    String sLastItemID = "";
                    for (Map.Entry<String, ItemClass_CatalogItem>
                            entry : treeMapPreSort.entrySet()) {
                        if (entry.getValue().sGroupID.equals(sGroupID)) {
                            if (sLastItemID.equals("")) {
                                //If this is the first time we have found a member of this group, initialize.
                                sLastItemID = entry.getValue().sItemID;
                                dLastItemDateTime = entry.getValue().dDatetime_Import;
                            } else {
                                //If this is not the first time we have found a member of this group...

                                if (dLastItemDateTime < entry.getValue().dDatetime_Import) {
                                    // If this item is newer than the last group item identified,
                                    // mark it to be excluded from the sort results.
                                    alsGroupMembersToHide.add(entry.getKey());
                                } else {
                                    //Else this item is older than the last group item identified,
                                    // mark it as the latest to compare to and mark the one we were
                                    // previously comparing against to be excluded from the sort results.
                                    alsGroupMembersToHide.add(sLastItemID);
                                    sLastItemID = entry.getValue().sItemID;
                                    dLastItemDateTime = entry.getValue().dDatetime_Import;
                                }
                            }
                        }
                    }
                }
                //Hide identified group items which are not the first of their group:
                for (String sItemID : alsGroupMembersToHide) {
                    treeMapPreSort.remove(sItemID);
                }
            }
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
        broadcastIntent_SortAndFilterCatalogDisplayResponse.setAction(CATALOG_SORT_AND_FILTER_DISP_ACTION_RESPONSE);
        broadcastIntent_SortAndFilterCatalogDisplayResponse.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_SortAndFilterCatalogDisplayResponse);

        return Result.success();
    }



}
