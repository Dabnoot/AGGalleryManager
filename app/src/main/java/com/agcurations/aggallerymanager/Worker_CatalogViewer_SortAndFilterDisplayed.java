package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;

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
        globalClass.ExecuteDownloadManagerPostProcessing();

        //Apply the sort field.
        //Copy over only items that match a filter, if applied.
        //Copy over only non-restricted catalog items, if necessary.
        //Sort the TreeMap.

        //Create new TreeMap to presort the catalog items:
        TreeMap<String, ItemClass_CatalogItem> treeMapPreSort; //String = field being sorted, String = Catalog item data
        treeMapPreSort = new TreeMap<>();

        //Populate the Key field of the preSort TreeMap with SortBy field data, filtered and restricted if necessary:
        int iProgressNumerator = 1;
        int iProgressDenominator = globalClass.gtmCatalogLists.get(globalClass.giSelectedCatalogMediaCategory).size() + 1;
        int iProgressBarValue;

        String sKey;
        for (Map.Entry<String, ItemClass_CatalogItem>
                entry : globalClass.gtmCatalogLists.get(globalClass.giSelectedCatalogMediaCategory).entrySet()) {
            sKey = "";
            //Create a unique key to identify the record in the TreeMap, which includes
            // the SortBy field. TreeMap automatically sorts by the Key field.
            if(globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_LAST_VIEWED){
                sKey = entry.getValue().dDatetime_Last_Viewed_by_User.toString();
            } else if(globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_IMPORTED){
                sKey = entry.getValue().dDatetime_Import.toString();
            }
            sKey = sKey + entry.getValue().sItemID;


            //Apply a filter if requested - build a string out of the records contents, and if a
            //  filter is to be applied, check for a match. If no match, don't add the record to
            //  the TreeMap destined for the RecyclerView:
            boolean bIsFilterMatch = true;
            if(!globalClass.gsCatalogViewerFilterText[globalClass.giSelectedCatalogMediaCategory].equals("")) {
                String sFilterText_LowerCase = globalClass.gsCatalogViewerFilterText[globalClass.giSelectedCatalogMediaCategory].toLowerCase();
                String sKey_RecordText;

                //Append all of the field data and search the resulting
                //  string for a filter match:
                sKey_RecordText = globalClass.getCatalogRecordSearchString(entry.getValue());
                sKey_RecordText = sKey_RecordText.toLowerCase();

                if (!sKey_RecordText.contains(sFilterText_LowerCase)) {
                    bIsFilterMatch = false;
                }
            }

            //Check to see if the record needs to be skipped due to restriction settings:
            boolean bIsRestricted = false;
            if(globalClass.gbCatalogViewerTagsRestrictionsOn) {
                String sRecordTags = entry.getValue().sTags;
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
                        ItemClass_Tag ict = globalClass.gtmCatalogTagReferenceLists.get(globalClass.giSelectedCatalogMediaCategory).get(globalClass.getTagTextFromID(iTagID, globalClass.giSelectedCatalogMediaCategory));
                        if (ict != null) {
                            if (ict.bIsRestricted) {
                                bIsRestricted = true;
                                break;
                            }
                        }
                    }
                }
            }

            if(bIsFilterMatch && !bIsRestricted){
                treeMapPreSort.put(sKey, entry.getValue());
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
        if(globalClass.gbCatalogViewerSortAscending[globalClass.giSelectedCatalogMediaCategory]){
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