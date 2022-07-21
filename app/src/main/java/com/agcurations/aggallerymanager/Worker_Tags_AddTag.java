package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Tags_AddTag extends Worker {

    public static final String TAG_WORKER_TAGS_ADDTAG = "com.agcurations.aggallermanager.tag_worker_tags_addtag";

    String gsResponseActionFilter = Fragment_Import_3a_ItemDownloadTagImport.AddTagsServiceResponseReceiver.ADD_TAGS_SERVICE_EXECUTE_RESPONSE;
    int giMediaCategory;
    ArrayList<String> alsTags;

    public Worker_Tags_AddTag(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
        String[] sTags = getInputData().getStringArray(GlobalClass.EXTRA_ARRAYLIST_STRING_TAGS_TO_ADD);
        alsTags = new ArrayList<>();
        if (sTags != null) {
            Collections.addAll(alsTags, sTags);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        ArrayList<ItemClass_Tag> itemClass_tags = null;

        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        itemClass_tags = globalClass.TagDataFile_CreateNewRecords(alsTags, giMediaCategory);

        //Broadcast the completion of this task:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(gsResponseActionFilter);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        if(itemClass_tags != null) {
            broadcastIntent.putExtra(GlobalClass.EXTRA_ARRAYLIST_ITEMCLASSTAGS_ADDED_TAGS, itemClass_tags);
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        return Result.success();
    }



}
