package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;


public class Service_WebPageTabs {

    public static final String IMPORT_REQUEST_FROM_INTERNAL_BROWSER = "com.agcurations.aggallerymanager.importurl";

    public static void startAction_GetWebPageTabData(Context context) {
        OneTimeWorkRequest otwrGetWebPageTabData = new OneTimeWorkRequest.Builder(Worker_Browser_GetWebPageTabData.class)
                .build();
        WorkManager.getInstance(context).enqueue(otwrGetWebPageTabData);
    }

    public static void startAction_WriteWebPageTabData(Context context, String sCallerID) {

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataBrowserWriteWebPageTabData = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrWriteWebPageTabData = new OneTimeWorkRequest.Builder(Worker_Browser_WriteWebPageTabData.class)
                .setInputData(dataBrowserWriteWebPageTabData)
                .addTag(Worker_Browser_WriteWebPageTabData.TAG_WORKER_BROWSER_WRITEWEBPAGETABDATA) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrWriteWebPageTabData);
    }

    public static void startAction_GetWebpageTitleFavicon(Context context, ItemClass_WebPageTabData itemClass_webPageTabData){

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataBrowserGetWebPagePreview = new Data.Builder()
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_TABID, itemClass_webPageTabData.sTabID)
                .putString(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_ADDRESS, itemClass_webPageTabData.sAddress)
                .build();
        OneTimeWorkRequest otwrGetWebPagePreview = new OneTimeWorkRequest.Builder(Worker_Browser_GetWebPagePreview.class)
                .setInputData(dataBrowserGetWebPagePreview)
                .addTag(Worker_Browser_GetWebPagePreview.TAG_WORKER_BROWSER_GETWEBPAGEPREVIEW) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrGetWebPagePreview);
    }








}