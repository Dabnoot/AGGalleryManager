package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Map;
import java.util.TreeMap;

public class Worker_User_Delete extends Worker {

    public static final String TAG_WORKER_USER_DELETE = "com.agcurations.aggallermanager.tag_worker_user_delete";

    public static final String USER_DELETE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.USER_DELETE_ACTION_RESPONSE";

    public static final String USER_DELETE_COMPLETE_NOTIFICATION_BOOLEAN = "USER_DELETE_COMPLETE_NOTIFICATION_BOOLEAN";

    String gsResponseActionFilter;

    String gsUserName;

    public Worker_User_Delete(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsResponseActionFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);

        gsUserName = getInputData().getString(GlobalClass.EXTRA_STRING_USERNAME);

    }

    @NonNull
    @Override
    public Result doWork() {

        if(gsUserName == null){
            return Result.failure(DataErrorMessage("User name passed to Worker_User_Delete is null."));
        }

        int iProgressNumerator = 0;
        int iProgressDenominator = 1;
        int iProgressBarValue;

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        //=============== IMPORTANT NOTE - THIS ROUTINE DOES NOT DELETE FILES. =====================
        //After any files that need to be removed (or not), execute final actions. File removal can
        // take time.

        //If there are no catalog items remaining to be deleted, look for tag items private to the
        // to-be-deleted-user. If there are private tag items to be deleted, delete those tags. If
        // the user name is on a tag, remove it.
        boolean bRewriteTagFileLoop = false;

        for(int iMediaCategory = 0; iMediaCategory <= 2; iMediaCategory++){
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(false, "",
                    false, iProgressBarValue,
                    true, "Checking for username '" + gsUserName + "' in "
                            + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " tags file...",
                    USER_DELETE_ACTION_RESPONSE);
            //Create a copy of the tag reference list so that the true list can be edited:
            TreeMap<Integer, ItemClass_Tag> tmTagReferenceListCopy = new TreeMap<>(GlobalClass.gtmCatalogTagReferenceLists.get(iMediaCategory));
            for(Map.Entry<Integer, ItemClass_Tag> tagEntry: tmTagReferenceListCopy.entrySet()){
                ItemClass_Tag ict = tagEntry.getValue();
                if(ict.alsTagApprovedUsers.size() == 1) {
                    if (ict.alsTagApprovedUsers.get(0).equals(gsUserName)) {
                        bRewriteTagFileLoop = true;
                        //Remove the tag from the reference list:
                        GlobalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).remove(tagEntry.getKey());
                    }
                } else {
                    //If there is more than one username, attempt to remove the user name from the
                    // tag. Note - coding guidance says there's no point in checking if the username
                    // exists first.
                    if(ict.alsTagApprovedUsers.remove(gsUserName)) {
                        //If the name was found and removed, set the rewrite flag:
                        bRewriteTagFileLoop = true;
                    }

                }
            }
            if(bRewriteTagFileLoop){
                bRewriteTagFileLoop = false; //Reset back to false state for next loop.
                //Re-write the tag file. No need to post a progress message before as the tag files are quite small and should not take much time.
                GlobalClass.WriteTagDataFile(iMediaCategory);
                //If there are no private catalog items, no private tags, and the user name has been removed
                // from all tag records for this media category, remove the user from any shared catalog item records:
                globalClass.BroadcastProgress(false, "",
                        false, 0,
                        true,
                        "Updating " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " catalog items based on tags...",
                        USER_DELETE_ACTION_RESPONSE);

                //Call a worker to go through this media category data file and recalc the maturity
                //  rating and assigned users:
                Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                Data dataRecalcCatalogItemsMaturityAndUsers = new Data.Builder()
                        .putString(GlobalClass.EXTRA_CALLER_ID, "Worker_User_Delete:doWork()")
                        .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                        .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, iMediaCategory)
                        .build();
                OneTimeWorkRequest otwrRecalcCatalogItemsMaturityAndUsers = new OneTimeWorkRequest.Builder(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.class)
                        .setInputData(dataRecalcCatalogItemsMaturityAndUsers)
                        .addTag(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.TAG_WORKER_CATALOG_RECALC_APPROVED_USERS) //To allow finding the worker later.
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueue(otwrRecalcCatalogItemsMaturityAndUsers);

            }
            iProgressNumerator++;
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(false, "",
                    false, iProgressBarValue,
                    false, "",
                    USER_DELETE_ACTION_RESPONSE);
        }

        globalClass.BroadcastProgress(false, "",
                true, 100,
                true, "Tag file processing complete.",
                USER_DELETE_ACTION_RESPONSE);


        //If the to-be-deleted user does not have any private catalog items, private tag items,
        // and is not an approved user to any tag or catalog item, wipe the user from memory and
        // the user data file.

        ItemClass_User icuUserToBeRemoved = null;
        for(ItemClass_User icu: GlobalClass.galicu_Users){
            if(icu.sUserName.equals(gsUserName)){
                icuUserToBeRemoved = icu;
                break;
            }
        }
        GlobalClass.galicu_Users.remove(icuUserToBeRemoved);
        GlobalClass.WriteUserDataFile();

        //Send a broadcast notifying that the user delete operation is complete:
        Intent broadcastIntent_UserDeleteResponse = new Intent();
        broadcastIntent_UserDeleteResponse.putExtra(USER_DELETE_COMPLETE_NOTIFICATION_BOOLEAN, true);
        broadcastIntent_UserDeleteResponse.setAction(USER_DELETE_ACTION_RESPONSE);
        broadcastIntent_UserDeleteResponse.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_UserDeleteResponse);

        return Result.success();
    }


    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_User_Delete:" + sRoutine, sMessage);
    }


}
