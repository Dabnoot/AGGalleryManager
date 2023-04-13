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

    String gsIntentActionFilter;

    public Worker_CatalogViewer_SortAndFilterDisplayed(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsIntentActionFilter = Fragment_Import_1c_ComicWebDetect.ImportDataServiceResponseReceiver.IMPORT_RESPONSE_COMIC_WEB_DETECT;
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

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
        int iProgressDenominator = globalClass.gtmCatalogLists.get(GlobalClass.giSelectedCatalogMediaCategory).size() + 1;
        int iProgressBarValue;

        String sKey;

        //Prepare resolution or page count filter bounds if necessary:
        //Do bounds calcs before the for loop to reduce undue burden.
        boolean bResolutionOrPageCountFilterApplicable = false;
        boolean bVideoDurationFilterApplicable = false;
        Integer iResMin = null;
        Integer iResMax = null;
        int iPageMin = 0;
        int iPageMax = globalClass.giMaxComicPageCount;
        long lDurationMin = Math.max(0, globalClass.glMinVideoDurationMSSelected);
        long lDurationMax = globalClass.glMaxVideoDurationMSSelected;
        switch (GlobalClass.giSelectedCatalogMediaCategory){
            case GlobalClass.MEDIA_CATEGORY_VIDEOS:

                if(globalClass.glMinVideoDurationMSSelected > -1 || globalClass.glMaxVideoDurationMSSelected > -1){
                    bVideoDurationFilterApplicable = true;
                    if(lDurationMax == -1){
                        lDurationMax = globalClass.glMaxVideoDurationMS;
                    }
                }

                if(globalClass.giMinVideoResolutionSelected == -1 && globalClass.giMaxVideoResolutionSelected == -1){
                    break;
                } else {
                    bResolutionOrPageCountFilterApplicable = true;
                    iResMin = Math.max(0, globalClass.giMinVideoResolutionSelected);
                    iResMax = globalClass.giMaxVideoResolutionSelected;
                    if(iResMax == -1){
                        iResMax = globalClass.gtmVideoResolutions.size() - 1;
                    }
                    iResMin = globalClass.gtmVideoResolutions.get(iResMin);
                    iResMax = globalClass.gtmVideoResolutions.get(iResMax);
                    if(iResMin == null || iResMax == null){
                        bResolutionOrPageCountFilterApplicable = false;
                        break;
                    }
                }
                break;
            case GlobalClass.MEDIA_CATEGORY_IMAGES:
                if(globalClass.giMinImageMegaPixelsSelected == -1 && globalClass.giMaxImageMegaPixelsSelected == -1){
                    break;
                } else {
                    bResolutionOrPageCountFilterApplicable = true;
                    iResMin = Math.max(0, globalClass.giMinImageMegaPixelsSelected);
                    iResMax = globalClass.giMaxImageMegaPixelsSelected;
                    if(iResMax == -1){
                        iResMax = globalClass.giMaxImageMegaPixels;
                    }
                }
                break;
            case GlobalClass.MEDIA_CATEGORY_COMICS:
                if(globalClass.giMinComicPageCountSelected == -1 && globalClass.giMaxComicPageCountSelected == -1){
                    break;
                } else {
                    bResolutionOrPageCountFilterApplicable = true;
                    iPageMin = Math.max(0, globalClass.giMinComicPageCountSelected);
                    iPageMax = globalClass.giMaxComicPageCountSelected;
                    if(iPageMax == -1){
                        iPageMax = globalClass.giMaxComicPageCount;
                    }
                }
                break;
        }





        for (Map.Entry<String, ItemClass_CatalogItem>
                entry : globalClass.gtmCatalogLists.get(GlobalClass.giSelectedCatalogMediaCategory).entrySet()) {
            sKey = "";
            //Create a unique key to identify the record in the TreeMap, which includes
            // the SortBy field. TreeMap automatically sorts by the Key field.
            if(globalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_LAST_VIEWED){
                sKey = entry.getValue().dDatetime_Last_Viewed_by_User.toString();
            } else if(globalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_IMPORTED){
                sKey = entry.getValue().dDatetime_Import.toString();
            }
            sKey = sKey + entry.getValue().sItemID;


            //Apply a search if requested - build a string out of the records contents, and if a
            //  search is to be applied, check for a match. If no match, don't add the record to
            //  the TreeMap destined for the RecyclerView:
            boolean bSearchMatchApplicable = false;
            boolean bSearchMatch = false;
            if(globalClass.giCatalogViewerSearchInSelection[GlobalClass.giSelectedCatalogMediaCategory] != GlobalClass.SEARCH_IN_NO_SELECTION) {
                bSearchMatchApplicable = true;

                String sSearchInText_LowerCase = globalClass.gsCatalogViewerSearchInText[GlobalClass.giSelectedCatalogMediaCategory].toLowerCase();
                String sKey_RecordText = "";
                switch (globalClass.giCatalogViewerSearchInSelection[GlobalClass.giSelectedCatalogMediaCategory]) {
                    case GlobalClass.SEARCH_IN_TITLE:
                        sKey_RecordText = entry.getValue().sTitle;
                        break;
                    case GlobalClass.SEARCH_IN_ARTIST:
                        sKey_RecordText = entry.getValue().sComicArtists;
                        break;
                    case GlobalClass.SEARCH_IN_CHARACTERS:
                        sKey_RecordText = entry.getValue().sComicCharacters;
                        break;
                    case GlobalClass.SEARCH_IN_PARODIES:
                        sKey_RecordText = entry.getValue().sComicParodies;
                        break;
                    case GlobalClass.SEARCH_IN_ITEMID:
                        sKey_RecordText = entry.getValue().sItemID;
                        break;
                }
                sKey_RecordText = sKey_RecordText.toLowerCase();

                //Append all of the field data and search the resulting
                //  string for a filter match:

                if (sKey_RecordText.contains(sSearchInText_LowerCase)) {
                    bSearchMatch = true;
                }
            }

            //Apply a filter if requested, and if a
            //  filter is to be applied, check for a match. If no match, don't add the record to
            //  the TreeMap destined for the RecyclerView:
            boolean bFilterByApplicable = false;
            boolean bIsFilterByMatch = false;
            if(globalClass.giCatalogViewerFilterBySelection[GlobalClass.giSelectedCatalogMediaCategory] != GlobalClass.FILTER_BY_NO_SELECTION) {
                bFilterByApplicable = true;

                switch (globalClass.giCatalogViewerFilterBySelection[GlobalClass.giSelectedCatalogMediaCategory]) {
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


            boolean bTagMatchApplicable = false;
            boolean bTagsMatch = false;
            //Check to see if the user wants to filter by tag:
            if(globalClass.galtsiCatalogViewerFilterTags != null){
                if(globalClass.galtsiCatalogViewerFilterTags.get(GlobalClass.giSelectedCatalogMediaCategory).size() > 0){
                    bTagMatchApplicable = true;
                    if(entry.getValue().aliTags.containsAll(globalClass.galtsiCatalogViewerFilterTags.get(GlobalClass.giSelectedCatalogMediaCategory))){
                        bTagsMatch = true;
                    }
                }

            }

            //Check to see if the record needs to be skipped due to restriction settings:
            boolean bIsRestricted = false;

            /*String sRecordTags = entry.getValue().sTags;
            if(sRecordTags.length() > 0) {
                String[] saRecordTags = sRecordTags.split(",");
                for (String s : saRecordTags) {
                    //if list of restricted tags contains this particular record tag, mark as restricted item:
                    int iTagID;
                    //String sErrorMessage;
                    try {
                        iTagID = Integer.parseInt(s);
                    } catch (Exception e){
                        //sErrorMessage = e.getMessage();
                        continue;
                    }
                    ItemClass_Tag ict = globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.giSelectedCatalogMediaCategory).get(iTagID);
                    if (ict != null) {
                        if (globalClass.gicuCurrentUser != null) {
                            if (ict.iMaturityRating > globalClass.gicuCurrentUser.iMaturityLevel) {
                                bIsRestricted = true;
                                break;
                            }
                        } else {
                            if (ict.iMaturityRating > globalClass.giDefaultUserMaturityRating) {
                                bIsRestricted = true;
                                break;
                            }
                        }
                    }
                }
            }*/
            int iMaturityRating = entry.getValue().iMaturityRating;
            if (globalClass.gicuCurrentUser != null) {
                if (iMaturityRating > globalClass.gicuCurrentUser.iMaturityLevel) {
                    bIsRestricted = true;
                }
            } else {
                if (iMaturityRating > globalClass.giDefaultUserMaturityRating) {
                    bIsRestricted = true;
                }
            }
            boolean bApprovedForThisUser = false;
            ArrayList<String> alsAssignedUsers = entry.getValue().alsApprovedUsers;
            if(alsAssignedUsers.size() > 0){
                for(String sAssignedUser: alsAssignedUsers){
                    if(globalClass.gicuCurrentUser.sUserName.equals(sAssignedUser)){
                        bApprovedForThisUser = true;
                        break;
                    }
                }
            } else {
                bApprovedForThisUser = true;
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

                if(bIsMatch){
                    treeMapPreSort.put(sKey, entry.getValue());
                }

            }


            iProgressNumerator++;

            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, iProgressBarValue + "%",
                    Activity_CatalogViewer.CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);

        }

        //TreeMap presort will auto-sort itself.

        //Clean up the key, apply a reverse sort order, if applicable:
        TreeMap<Integer, ItemClass_CatalogItem> tmNewOrderCatalogList = new TreeMap<>();
        int iRID, iIterator;
        if(globalClass.gbCatalogViewerSortAscending[GlobalClass.giSelectedCatalogMediaCategory]){
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

        globalClass.gtmCatalogViewerDisplayTreeMap = tmNewOrderCatalogList;

        //Broadcast the ready state of the SortAndFilterCatalogDisplay operation:
        Intent broadcastIntent_SortAndFilterCatalogDisplayResponse = new Intent();
        broadcastIntent_SortAndFilterCatalogDisplayResponse.putExtra(GlobalClass.EXTRA_BOOL_REFRESH_CATALOG_DISPLAY, true);
        broadcastIntent_SortAndFilterCatalogDisplayResponse.setAction(Activity_CatalogViewer.CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
        broadcastIntent_SortAndFilterCatalogDisplayResponse.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_SortAndFilterCatalogDisplayResponse);

        return Result.success();
    }



}
