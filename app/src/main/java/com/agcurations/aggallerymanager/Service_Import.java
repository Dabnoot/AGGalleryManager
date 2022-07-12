package com.agcurations.aggallerymanager;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class Service_Import {


    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";



    public static void startActionGetDirectoryContents(Context context, Uri uriImportTreeUri, int iMediaCategory, int iFilesOrFolders, int iComicImportSource) {
        /*Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_GET_DIRECTORY_CONTENTS);
        String sImportTreeUri = uriImportTreeUri.toString();
        intent.putExtra(EXTRA_IMPORT_TREE_URI, sImportTreeUri);
        intent.putExtra(EXTRA_MEDIA_CATEGORY, iMediaCategory);
        intent.putExtra(EXTRA_FILES_OR_FOLDERS, iFilesOrFolders);
        intent.putExtra(EXTRA_COMIC_IMPORT_SOURCE, iComicImportSource);
        context.startService(intent);*/
        startAction_GetDirectoryContents(context, uriImportTreeUri, iMediaCategory, iFilesOrFolders, iComicImportSource, "Service_Import:startActionGetDirectoryContents()");
    }

    public static void startAction_GetDirectoryContents(Context context, Uri uriImportTreeUri, int iMediaCategory, int iFilesOrFolders, int iComicImportSource, String sCallerID) {
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        String sImportTreeUri = uriImportTreeUri.toString();
        Data dataGetDirectoryContents = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(GlobalClass.EXTRA_IMPORT_TREE_URI, sImportTreeUri)
                .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, iMediaCategory)
                .putInt(GlobalClass.EXTRA_FILES_OR_FOLDERS, iFilesOrFolders)
                .putInt(GlobalClass.EXTRA_COMIC_IMPORT_SOURCE, iComicImportSource)
                .build();
        OneTimeWorkRequest otwrGetDirectoryContents = new OneTimeWorkRequest.Builder(Worker_Import_GetDirectoryContents.class)
                .setInputData(dataGetDirectoryContents)
                .addTag(Worker_Import_GetDirectoryContents.TAG_WORKER_IMPORT_GETDIRECTORYCONTENTS) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrGetDirectoryContents);
    }

    public static void startActionImportFiles(Context context, int iMoveOrCopy, int iMediaCategory) {
        /*Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_FILES);
        intent.putExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy);
        intent.putExtra(EXTRA_MEDIA_CATEGORY, iMediaCategory);
        context.startService(intent);*/
        startAction_ImportFiles(context, iMoveOrCopy, iMediaCategory, "Service_Import:startActionImportFiles()");
    }

    public static void startAction_ImportFiles(Context context, int iMoveOrCopy, int iMediaCategory, String sCallerID) {
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataImportFiles = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putInt(GlobalClass.EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy)
                .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, iMediaCategory)
                .build();
        OneTimeWorkRequest otwrImportFiles = new OneTimeWorkRequest.Builder(Worker_Import_ImportFiles.class)
                .setInputData(dataImportFiles)
                .addTag(Worker_Import_ImportFiles.TAG_WORKER_IMPORT_IMPORTFILES) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrImportFiles);
    }

    public static void startActionImportNHComicsFiles(Context context, int iMoveOrCopy, int iComicImportSource) {
        /*Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_NHCOMICS);
        intent.putExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy);
        intent.putExtra(EXTRA_COMIC_IMPORT_SOURCE, iComicImportSource);
        context.startService(intent);*/
        startAction_ImportNHComicsFiles(context, iMoveOrCopy, iComicImportSource, "Service_Import:startActionImportNHComicsFiles()");
    }

    public static void startAction_ImportNHComicsFiles(Context context, int iMoveOrCopy, int iComicImportSource, String sCallerID) {
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataImportNHComicsFiles = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putInt(GlobalClass.EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy)
                .putInt(GlobalClass.EXTRA_COMIC_IMPORT_SOURCE, iComicImportSource)
                .build();
        OneTimeWorkRequest otwrImportNHComicsFiles = new OneTimeWorkRequest.Builder(Worker_Import_ImportNHComicsFiles.class)
                .setInputData(dataImportNHComicsFiles)
                .addTag(Worker_Import_ImportNHComicsFiles.TAG_WORKER_IMPORT_IMPORTNHCOMICSFILES) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrImportNHComicsFiles);
    }

    public static void startActionImportComicFolders(Context context, int iMoveOrCopy, int iComicImportSource) {
        /*Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_COMIC_FOLDERS);
        intent.putExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy);
        intent.putExtra(EXTRA_COMIC_IMPORT_SOURCE, iComicImportSource);
        context.startService(intent);*/
        startAction_ImportComicFolders(context, iMoveOrCopy, iComicImportSource, "Service_Import:startActionImportComicFolders()");
    }

    public static void startAction_ImportComicFolders(Context context, int iMoveOrCopy, int iComicImportSource, String sCallerID) {
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataImportComicFolders = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putInt(GlobalClass.EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy)
                .putInt(GlobalClass.EXTRA_COMIC_IMPORT_SOURCE, iComicImportSource)
                .build();
        OneTimeWorkRequest otwrImportComicFolders = new OneTimeWorkRequest.Builder(Worker_Import_ImportComicFolders.class)
                .setInputData(dataImportComicFolders)
                .addTag(Worker_Import_ImportComicFolders.TAG_WORKER_IMPORT_IMPORTCOMICFOLDERS) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrImportComicFolders);
    }

    public static void startActionAcquireNHComicsDetails(Context context, String sAddress, String sIntentActionFilter){
        /*Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_GET_COMIC_DETAILS_ONLINE);
        intent.putExtra(EXTRA_STRING_WEB_ADDRESS, sAddress);
        intent.putExtra(EXTRA_STRING_INTENT_ACTION_FILTER, sIntentActionFilter);
        context.startService(intent);*/
        startAction_AcquireNHComicsDetails(context, sAddress, sIntentActionFilter, "Service_Import:startActionAcquireNHComicsDetails()");
    }

    public static void startAction_AcquireNHComicsDetails(Context context, String sAddress, String sIntentActionFilter, String sCallerID){
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataAcquireNHComicsDetails = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(GlobalClass.EXTRA_STRING_WEB_ADDRESS, sAddress)
                .putString(GlobalClass.EXTRA_STRING_INTENT_ACTION_FILTER, sIntentActionFilter)
                .build();
        OneTimeWorkRequest otwrAcquireNHComicsDetails = new OneTimeWorkRequest.Builder(Worker_Import_AcquireNHComicsDetails.class)
                .setInputData(dataAcquireNHComicsDetails)
                .addTag(Worker_Import_AcquireNHComicsDetails.TAG_WORKER_IMPORT_ACQUIRENHCOMICDETAILS) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrAcquireNHComicsDetails);
    }

    public static void startActionComicAnalyzeHTML(Context context){
        /*Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_VIDEO_ANALYZE_HTML);
        context.startService(intent);*/
        startAction_ComicAnalyzeHTML(context, "Service_Import:startActionComicAnalyzeHTML()");
    }

    public static void startAction_ComicAnalyzeHTML(Context context, String sCallerID){
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataComicAnalyzeHTML = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrComicAnalyzeHTML = new OneTimeWorkRequest.Builder(Worker_Import_ComicAnalyzeHTML.class)
                .setInputData(dataComicAnalyzeHTML)
                .addTag(Worker_Import_ComicAnalyzeHTML.TAG_WORKER_IMPORT_COMICANALYZEHTML) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrComicAnalyzeHTML);
    }

    public static void startActionImportComicWebFiles(Context context, String sIntentActionFilter){
        /*Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_COMIC_WEB_FILES);
        intent.putExtra(COMIC_CATALOG_ITEM, ci);
        intent.putExtra(EXTRA_STRING_INTENT_ACTION_FILTER, sIntentActionFilter);
        context.startService(intent);*/
        startAction_ImportComicWebFiles(context, sIntentActionFilter, "Service_Import:startActionImportComicWebFiles()");
    }

    public static void startAction_ImportComicWebFiles(Context context, String sIntentActionFilter, String sCallerID){
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataImportComicWebFiles = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(GlobalClass.EXTRA_STRING_INTENT_ACTION_FILTER, sIntentActionFilter)
                .build();
        OneTimeWorkRequest otwrImportComicWebFiles = new OneTimeWorkRequest.Builder(Worker_Import_ImportComicWebFiles.class)
                .setInputData(dataImportComicWebFiles)
                .addTag(Worker_Import_ImportComicWebFiles.TAG_WORKER_IMPORT_IMPORTCOMICWEBFILES) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrImportComicWebFiles);
    }

    public static void startActionVideoAnalyzeHTML(Context context){
        /*Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_VIDEO_ANALYZE_HTML);
        context.startService(intent);*/
        startAction_VideoAnalyzeHTML(context, "Service_Import:startActionVideoAnalyzeHTML()");
    }

    public static void startAction_VideoAnalyzeHTML(Context context, String sCallerID){
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataVideoAnalyzeHTML = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrVideoAnalyzeHTML = new OneTimeWorkRequest.Builder(Worker_Import_VideoAnalyzeHTML.class)
                .setInputData(dataVideoAnalyzeHTML)
                .addTag(Worker_Import_VideoAnalyzeHTML.TAG_WORKER_IMPORT_VIDEOANALYZEHTML) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrVideoAnalyzeHTML);
    }

    public static void startActionVideoDownload(Context context, String sWebPageAddress){
        /*Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_VIDEO_WEB_FILES);
        intent.putExtra(EXTRA_STRING_WEB_ADDRESS, sWebPageAddress);
        context.startService(intent);*/
        startAction_VideoDownload(context, sWebPageAddress, "Service_Import:startActionVideoDownload()");
    }

    public static void startAction_VideoDownload(Context context, String sWebPageAddress, String sCallerID){
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataVideoDownload = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(GlobalClass.EXTRA_STRING_WEB_ADDRESS, sWebPageAddress)
                .build();
        OneTimeWorkRequest otwrVideoDownload = new OneTimeWorkRequest.Builder(Worker_Import_VideoDownload.class)
                .setInputData(dataVideoDownload)
                .addTag(Worker_Import_VideoDownload.TAG_WORKER_IMPORT_VIDEODOWNLOAD) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrVideoDownload);
    }

    public static void startActionDeleteFiles(Context context, ArrayList<String> alsUriFilesToDelete, String sCallerActionResponseFilter){
        /*Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_DELETE_FILES);
        intent.putExtra(EXTRA_URI_STRING_ARRAY_FILES_TO_DELETE, alsUriFilesToDelete);
        intent.putExtra(EXTRA_CALLER_ACTION_RESPONSE_FILTER, sCallerActionResponseFilter);
        context.startService(intent);*/
        startAction_DeleteFiles(context, alsUriFilesToDelete, sCallerActionResponseFilter, "Service_Import:startActionVideoDownload()");
    }

    public static void startAction_DeleteFiles(Context context, ArrayList<String> alsUriFilesToDelete, String sCallerActionResponseFilter, String sCallerID){

        //Create a copy of alsUriFilesToDelete in globalClass in order to pass the data without exceeding memory for the transfer.
        //  Create a copy in case the user starts a new import and has more files to delete. The Worker should make yet another copy
        //  of this file list in case the listing in globalClass is overwritten before operation completion.
        GlobalClass globalClass = (GlobalClass) context;
        globalClass.alsUriFilesToDelete = new ArrayList<>(alsUriFilesToDelete);

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataDeleteFiles = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER, sCallerActionResponseFilter)
                .build();
        OneTimeWorkRequest otwrDeleteFiles = new OneTimeWorkRequest.Builder(Worker_Import_DeleteFiles.class)
                .setInputData(dataDeleteFiles)
                .addTag(Worker_Import_DeleteFiles.TAG_WORKER_IMPORT_DELETEFILES) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrDeleteFiles);
    }


    //==============================================================================================
    //===== Import Utilities =======================================================================
    //==============================================================================================

    public static String GetNHComicID(String sFileName){
        boolean bIsValidComicPage = true;
        int iComicIDDigitCount = 0;

        if (sFileName.matches("^\\d{7}_(Cover|Page).+")){
            iComicIDDigitCount = 7;
        } else if (sFileName.matches("^\\d{6}_(Cover|Page).+")){
            iComicIDDigitCount = 6;
        } else if (sFileName.matches("^\\d{5}_(Cover|Page).+")) {
            iComicIDDigitCount = 5;
        } else if (sFileName.matches("^\\d{4}_(Cover|Page).+")) {
            iComicIDDigitCount = 4;
        } else if (sFileName.matches("^\\d{3}_(Cover|Page).+")) {
            iComicIDDigitCount = 3;
        } else if (sFileName.matches("^\\d{2}_(Cover|Page).+")) {
            iComicIDDigitCount = 2;
        } else if (sFileName.matches("^\\d_(Cover|Page).+")) {
            iComicIDDigitCount = 1;
        } else {
            bIsValidComicPage = false;
        }
        String sComicID = "";
        if(bIsValidComicPage) {
            sComicID = sFileName.substring(0, iComicIDDigitCount);
        }
        return sComicID;
    }

    public static String GetNHComicNameFromCoverFile(String sFileName){
        if (sFileName.matches(GlobalClass.gsNHComicCoverPageFilter)){
            int iComicIDDigitCount = GetNHComicID(sFileName).length();
            return sFileName.substring(7 + iComicIDDigitCount,sFileName.length()-4); //'7' for the word "_Cover".
        }

       return "";
    }

    public static String cleanFileNameViaTrim(String sFilename){
        //Use when expecting the begining of the filename to be ok, but trailing data may have illegal chars.
        //Useful when file is a download from a URL.
        //Example:
        //hls-480p-fe73881.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
        String sOutput = sFilename;
        String[] sReservedChars = {
            "|",
            "\\",
            "?",
            "*",
            "<",
            "\"",
            ":",
            ">",
            "+",
            "[",
            "]",
            "'"};
        for(String sReservedChar: sReservedChars) {
            int iReservedCharLocation = sOutput.indexOf(sReservedChar);
            if( iReservedCharLocation > 0){
                sOutput = sOutput.substring(0, iReservedCharLocation);
            }

        }

        return sOutput;
    }



}