package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Tags_DeleteTag extends Worker {

    public static final String TAG_WORKER_TAGS_DELETETAG = "com.agcurations.aggallermanager.tag_worker_tags_deletetag";

    public static final String DELETE_TAGS_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.DELETE_TAGS_ACTION_RESPONSE";

    String gsResponseActionFilter;
    int giMediaCategory;
    ItemClass_Tag gict_TagToDelete;
    Context gContext;

    public Worker_Tags_DeleteTag(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gContext = context;
        gsResponseActionFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);
        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
        String sTagToDelete = getInputData().getString(GlobalClass.EXTRA_TAG_TO_BE_DELETED);
        if(sTagToDelete != null){
            gict_TagToDelete = GlobalClass.ConvertFileLineToTagItem(sTagToDelete);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass;
        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        String sMessage;
        int iProgressNumerator = 0;
        int iProgressDenominator = 3;
        int iProgressBarValue;

        boolean bUpdateCatalogFile = false;

        //Loop through all catalog items and look for items that contain the tag to delete:
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntryCatalogRecord : GlobalClass.gtmCatalogLists.get(giMediaCategory).entrySet()){
            iProgressNumerator++;
            if(iProgressNumerator % 100 == 0) {
                iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                globalClass.BroadcastProgress(false, "",
                        true, iProgressBarValue,
                        true, "Examining " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog...",
                        DELETE_TAGS_ACTION_RESPONSE);
            }
            String sTags = tmEntryCatalogRecord.getValue().sTags;
            ArrayList<Integer> aliTags = GlobalClass.getIntegerArrayFromString(sTags, ",");

            if(aliTags.contains(gict_TagToDelete.iTagID)){
                //If this catalog item contains the tag...
                //Delete the tag from the record.

                bUpdateCatalogFile = true;

                //Form the new Tag string:
                ArrayList<Integer> aliNewTags = new ArrayList<>();
                for (Integer iTagID : aliTags) {
                    if (!iTagID.equals(gict_TagToDelete.iTagID)) {
                        aliNewTags.add(iTagID);
                    }
                }
                tmEntryCatalogRecord.getValue().sTags = GlobalClass.formDelimitedString(aliNewTags, ",");
                tmEntryCatalogRecord.getValue().aliTags = new ArrayList<>(aliNewTags);
                //Recalculate permissions and approved users for this catalog item given the new tag
                // set:
                tmEntryCatalogRecord.getValue().iMaturityRating =
                        GlobalClass.getHighestTagMaturityRating(tmEntryCatalogRecord.getValue().aliTags, giMediaCategory);
                tmEntryCatalogRecord.getValue().alsApprovedUsers =
                        GlobalClass.getApprovedUsersForTagGrouping(tmEntryCatalogRecord.getValue().aliTags, giMediaCategory);
                //Catalog records are updated in memory by these memory reference operations.

            } //End if the record contains the tag

        } //End for loop through catalog.

        if(bUpdateCatalogFile) {
            //Update the catalog file:
            globalClass.BroadcastProgress(false, "",
                    true, 0,
                    true, "Writing " + GlobalClass.gsCatalogFolderNames[giMediaCategory] + " catalog file...",
                    DELETE_TAGS_ACTION_RESPONSE);

            globalClass.CatalogDataFile_UpdateCatalogFile(giMediaCategory, "Removing tag from catalog records...");

            //Inform program of a need to update the tags histogram:
            globalClass.gbTagHistogramRequiresUpdate[giMediaCategory] = true;
        }

        //Remove the tag from memory:
        GlobalClass.gtmCatalogTagReferenceLists.get(giMediaCategory).remove(gict_TagToDelete.iTagID);
        GlobalClass.gtmApprovedCatalogTagReferenceLists.get(giMediaCategory).remove(gict_TagToDelete.iTagID);


        //Update the tag file:
        if(!GlobalClass.WriteTagDataFile(giMediaCategory)){
            sMessage = "Problem updating Tags data file.";
            globalClass.problemNotificationConfig(sMessage, DELETE_TAGS_ACTION_RESPONSE);
            return Result.failure();
        }

        globalClass.BroadcastProgress(false, "",
                true, 100,
                true, "Tag deletion complete.",
                DELETE_TAGS_ACTION_RESPONSE);

        //Send a broadcast that this process is complete.
        Intent broadcastIntent_NotifyTagDeleteComplete = new Intent();
        broadcastIntent_NotifyTagDeleteComplete.putExtra(GlobalClass.EXTRA_TAG_DELETE_COMPLETE, true);
        broadcastIntent_NotifyTagDeleteComplete.setAction(DELETE_TAGS_ACTION_RESPONSE);
        broadcastIntent_NotifyTagDeleteComplete.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_NotifyTagDeleteComplete);

        return Result.success();
    }



}
