package com.agcurations.aggallerymanager;

import android.content.Context;

import java.util.ArrayList;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


public class Service_TagEditor {

    public static void startActionDeleteTag(Context context, ItemClass_Tag ict_TagToDelete, int iMediaCategory) {

        String sTagRecord = GlobalClass.getTagRecordString(ict_TagToDelete);
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataDeleteTag = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Fragment_TagEditor_4_DeleteTag:DeleteTag()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, iMediaCategory)
                .putString(GlobalClass.EXTRA_TAG_TO_BE_DELETED, sTagRecord)
                .build();
        OneTimeWorkRequest otwrDeleteTag = new OneTimeWorkRequest.Builder(Worker_Tags_DeleteTag.class)
                .setInputData(dataDeleteTag)
                .addTag(Worker_Tags_DeleteTag.TAG_WORKER_TAGS_DELETETAG) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrDeleteTag);
    }

    public static void startActionAddTags(Context context, ArrayList<String> alsTags, int iMediaCategory) {

        String[] sTags = new String[alsTags.size()];
        int i = 0;
        for(String sTag: alsTags){
            sTags[i] = sTag;
            i++;
        }

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataAddTag = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Fragment_TagEditor_4_DeleteTag:DeleteTag()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, iMediaCategory)
                .putStringArray(GlobalClass.EXTRA_ARRAYLIST_STRING_TAGS_TO_ADD, sTags)
                .build();
        OneTimeWorkRequest otwrAddTag = new OneTimeWorkRequest.Builder(Worker_Tags_AddTag.class)
                .setInputData(dataAddTag)
                .addTag(Worker_Tags_AddTag.TAG_WORKER_TAGS_ADDTAG) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrAddTag);

    }

}