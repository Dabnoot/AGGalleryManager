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

public class Worker_Catalog_Adjaceny_Analyzer extends Worker {

    public static final String TAG_WORKER_CATALOG_ADJACENCY_ANALYZER = "com.agcurations.aggallermanager.tag_worker_catalog_adjacency_analyzer";

    public static final String CATALOG_ADJACENCY_ANALYZER_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_ADJACENCY_ANALYZER_RESPONSE";
    public static final String CATALOG_ADJACENCY_ANALYZER_EXTRA_BOOL_COMPLETE = "com.agcurations.aggallerymanager.intent.extra.CATALOG_ADJACENCY_ANALYZER_EXTRA_BOOL_COMPLETE";

    public static final String EXTRA_STRING_FILENAME = "com.agcurations.aggallerymanager.extra.string_filename";
    public static final String EXTRA_STRING_FILENAME_FILTER = "com.agcurations.aggallerymanager.extra.string_filename_filter";
    public static final String EXTRA_INT_HEIGHT = "com.agcurations.aggallerymanager.extra.int_height";
    public static final String EXTRA_INT_WIDTH = "com.agcurations.aggallerymanager.extra.int_width";
    public static final String EXTRA_LONG_DURATION = "com.agcurations.aggallerymanager.extra.long_duration";
    public static final String EXTRA_LONG_FILE_SIZE = "com.agcurations.aggallerymanager.extra.long_file_size";
    public static final String EXTRA_DOUBLE_FILE_MODIFIED_DATE = "com.agcurations.aggallerymanager.extra.double_file_modified_date";
    public static final String EXTRA_ARRAY_INT_TAGS = "com.agcurations.aggallerymanager.extra.array_int_tags";


    String gsIntentActionFilter;

    public String gsIncomingData_FileName;
    public String gsIncomingData_FileNameFilter;
    public int giIncomingData_Height;
    public int giIncomingData_Width;
    public long glIncomingData_Duration_Milliseconds;
    public long glIncomingData_FileSize;
    public Double gdIncomingDate_File_Modified_Date;
    public ArrayList<Integer> gali_IncomingData_Tags = null;


    public Worker_Catalog_Adjaceny_Analyzer(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsIntentActionFilter = Fragment_Import_1c_ComicWebDetect.ImportDataServiceResponseReceiver.IMPORT_RESPONSE_COMIC_WEB_DETECT;

        gsIncomingData_FileName = workerParams.getInputData().getString(EXTRA_STRING_FILENAME);
        gsIncomingData_FileNameFilter = workerParams.getInputData().getString(EXTRA_STRING_FILENAME_FILTER);
        giIncomingData_Height = workerParams.getInputData().getInt(EXTRA_INT_HEIGHT, -1);
        giIncomingData_Width = workerParams.getInputData().getInt(EXTRA_INT_WIDTH, -1);
        glIncomingData_Duration_Milliseconds = workerParams.getInputData().getLong(EXTRA_LONG_DURATION, -1);
        gdIncomingDate_File_Modified_Date = workerParams.getInputData().getDouble(EXTRA_DOUBLE_FILE_MODIFIED_DATE, -1);
        glIncomingData_FileSize = workerParams.getInputData().getLong(EXTRA_LONG_FILE_SIZE, -1);

        int[] tempArray = workerParams.getInputData().getIntArray(EXTRA_ARRAY_INT_TAGS);
        if(tempArray != null) {
            gali_IncomingData_Tags = new ArrayList<>();
            for(int iElement: tempArray){
                gali_IncomingData_Tags.add(iElement);
            }
        }


    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();


        //Create new TreeMap to presort the catalog items:
        TreeMap<String, ItemClass_CatalogItem> treeMapPreSort; //String = field being sorted, String = Catalog item data
        treeMapPreSort = new TreeMap<>();

        //Populate the Key field of the preSort TreeMap with SortBy field data, filtered and restricted if necessary:
        int iProgressNumerator = 1;
        int iProgressDenominator = GlobalClass.gtmCatalogLists.get(GlobalClass.giSelectedCatalogMediaCategory).size() + 1;
        int iProgressBarValue = 0;

        for (Map.Entry<String, ItemClass_CatalogItem>
                entry : GlobalClass.gtmCatalogLists.get(GlobalClass.giSelectedCatalogMediaCategory).entrySet()) {

            if (!GlobalClass.gsCatalogViewerSortBySharedWithUser.equals("")) {
                //The user has requested a sort including only catalog items that are shared with a
                // specified user. This user could be theirself. If the ApprovedUsers list does not
                // contain the requested user name, skip the rest of the loop.
                String sRequestedUserNameShare = GlobalClass.gsCatalogViewerSortBySharedWithUser;
                if (!entry.getValue().alsApprovedUsers.contains(sRequestedUserNameShare)) {
                    continue;
                }
            }

            //Check to see if a filename filter is applicable:
            boolean bFilterByFileName = false;
            boolean bFileNameFilterMatch = false;
            if (gsIncomingData_FileNameFilter != null) {
                if (!gsIncomingData_FileNameFilter.equals("")) {
                    bFilterByFileName = true;
                    bFileNameFilterMatch = entry.getValue().sFilename.matches(gsIncomingData_FileNameFilter);
                } else {
                    if(gsIncomingData_FileName.equals(entry.getValue().sFilename)){
                        bFilterByFileName = true;
                        bFileNameFilterMatch = true;
                    }
                }
            }

            //Check to see if a file modified date is to be used:
            boolean bFilterByFileModifiedDateTime = false;
            boolean bDateTimeFilterMatch = false;
            /*if (gdIncomingDate_File_Modified_Date > 0) {
                bFilterByFileModifiedDateTime = true;
                double dDateTime_CatalogItemImport = entry.getValue().dDatetime_Import;
                double lDateDiff = Math.abs(dDateTime_CatalogItemImport - gdIncomingDate_File_Modified_Date);
                if (lDateDiff < 500.000100) {
                    //static final String gsDatePatternNumSort = "yyyyMMdd.HHmmss"; 0.000100 = 1 minute difference.
                    bDateTimeFilterMatch = true;
                }
            }*/


            //Filter on resolution or page count, as applicable:
            boolean bResolutionFilterApplicable = false;
            boolean bResolutionMatch = false;

            if (giIncomingData_Height > 0 && giIncomingData_Width > 0) {
                bResolutionFilterApplicable = true;
                switch (GlobalClass.giSelectedCatalogMediaCategory) {
                    case GlobalClass.MEDIA_CATEGORY_VIDEOS:
                        if (entry.getValue().iHeight == giIncomingData_Height && entry.getValue().iWidth == giIncomingData_Width) {
                            bResolutionMatch = true;
                        }
                        break;
                    case GlobalClass.MEDIA_CATEGORY_IMAGES:
                        if ((entry.getValue().iHeight == giIncomingData_Height && entry.getValue().iWidth == giIncomingData_Width) ||
                                (entry.getValue().iHeight == giIncomingData_Width && entry.getValue().iWidth == giIncomingData_Height)) {
                            bResolutionMatch = true;

                        }
                        break;
                }
            }
            if (bResolutionMatch) {
                //todo: remove. For debugging.
                //static final String gsDatePatternNumSort = "yyyyMMdd.HHmmss"; 0.000100 = 1 minute difference.
                double dDateTime_CatalogItemImport = entry.getValue().dDatetime_Import;
                double lDateDiff = Math.abs(dDateTime_CatalogItemImport - gdIncomingDate_File_Modified_Date);
                bDateTimeFilterMatch = true;
                ItemClass_CatalogItem icci = entry.getValue();
                String sFolderName = icci.sFolderRelativePath;
                String sFileName = icci.sFilename;
                int iTagCount = icci.aliTags.size();
            }

            //Filter on video duration, as applicable:
            boolean bVideoDurationFilterApplicable = false;
            boolean bVideoDurationMatch = false;
            /*if (glIncomingData_Duration_Milliseconds > 0) {
                bVideoDurationFilterApplicable = true;
                long lVideoDuration = entry.getValue().lDuration_Milliseconds;
                if (Math.abs(lVideoDuration - glIncomingData_Duration_Milliseconds) < 10000) {
                    bVideoDurationMatch = true;
                }
            }*/


            boolean bTagMatchApplicable = false;
            boolean bTagsMatch = false;
            //Check to see if the user wants to filter by tag:
            if (gali_IncomingData_Tags != null) {
                if (gali_IncomingData_Tags.size() > 0) {
                    bTagMatchApplicable = true;
                    if (entry.getValue().aliTags.containsAll(gali_IncomingData_Tags)) {
                        bTagsMatch = true;
                    }
                }
            }

            //Check to see if the record needs to be skipped due to restriction settings:
            boolean bIsRestricted = false;

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
            if (alsAssignedUsers.size() > 0) {
                if (GlobalClass.gicuCurrentUser != null) {
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

            if (!bIsRestricted && bApprovedForThisUser) {
                boolean bIsMatch = true;

                if (bFilterByFileName) {
                    bIsMatch = bFileNameFilterMatch;
                }
                if (bFilterByFileModifiedDateTime) {
                    bIsMatch = bIsMatch && bDateTimeFilterMatch;
                }
                if (bResolutionFilterApplicable) {
                    bIsMatch = bIsMatch && bResolutionMatch;
                }
                if (bVideoDurationFilterApplicable) {
                    bIsMatch = bIsMatch && bVideoDurationMatch;
                }
                if (bTagMatchApplicable) {
                    bIsMatch = bIsMatch && bTagsMatch;
                }

                if (bIsMatch) {
                    treeMapPreSort.put(entry.getKey(), entry.getValue());
                }

            }

            iProgressNumerator++;
            int iPrevProgressBarValue = iProgressBarValue;
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            if (iPrevProgressBarValue != iProgressBarValue) {
                globalClass.BroadcastProgress(false, "",
                        true, iProgressBarValue,
                        true, iProgressBarValue + "%",
                        CATALOG_ADJACENCY_ANALYZER_RESPONSE);
            }

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

        GlobalClass.gtmCatalogAdjacencyAnalysisTreeMap = tmNewOrderCatalogList;

        //Broadcast the ready state of the SortAndFilterCatalogDisplay operation:
        Intent broadcastIntent_CatalogAdjacencyAnalysisResponse = new Intent();
        broadcastIntent_CatalogAdjacencyAnalysisResponse.putExtra(CATALOG_ADJACENCY_ANALYZER_EXTRA_BOOL_COMPLETE, true);
        broadcastIntent_CatalogAdjacencyAnalysisResponse.setAction(CATALOG_ADJACENCY_ANALYZER_RESPONSE);
        broadcastIntent_CatalogAdjacencyAnalysisResponse.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_CatalogAdjacencyAnalysisResponse);

        return Result.success();
    }



}
