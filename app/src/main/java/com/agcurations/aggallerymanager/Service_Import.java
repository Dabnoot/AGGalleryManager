package com.agcurations.aggallerymanager;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class Service_Import {

    public static void startActionGetDirectoryContents(Context context, Uri uriImportTreeUri, int iMediaCategory, int iFilesOrFolders, int iComicImportSource) {

        String sCallerID = "Service_Import:startActionGetDirectoryContents()";
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

    public static void startActionGetHoldingFolderDirectoryContents(Context context) {

        String sCallerID = "Service_Import:startActionGetHoldingFolderDirectoryContents()";
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataGetHoldingFolderDirectoryContents = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrGetHoldingFolderDirectoryContents = new OneTimeWorkRequest.Builder(Worker_Import_GetHoldingFolderDirectoryContents.class)
                .setInputData(dataGetHoldingFolderDirectoryContents)
                .addTag(Worker_Import_GetHoldingFolderDirectoryContents.TAG_WORKER_IMPORT_GETHOLDINGFOLDERDIRECTORYCONTENTS) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrGetHoldingFolderDirectoryContents);
    }

    /**
     *
     * @param context           Context
     * @param iMoveOrCopy       Specify move or copy
     * @param iMediaCategory    Media category
     */
    public static void startActionImportFiles(Context context, int iMoveOrCopy, int iMediaCategory) {

        String sCallerID = "Service_Import:startActionImportFiles()";
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

    public static void startActionImportComicFolders(Context context, int iMoveOrCopy, int iComicImportSource) {

        String sCallerID = "Service_Import:startActionImportComicFolders()";
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

    public static void startActionComicAnalyzeHTML(Context context){
        String sCallerID = "Service_Import:startActionComicAnalyzeHTML()";
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
        String sCallerID = "Service_Import:startActionImportComicWebFiles()";
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
        String sCallerID = "Service_Import:startActionVideoAnalyzeHTML()";
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
        String sCallerID = "Service_Import:startActionVideoDownload()";
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

        String sCallerID = "Service_Import:startActionVideoDownload()";
        GlobalClass globalClass = (GlobalClass) context;
        //Create a copy of alsUriFilesToDelete in globalClass in order to pass the data without exceeding memory for the transfer.
        //  Create a copy in case the user starts a new import and has more files to delete. The Worker should make yet another copy
        //  of this file list in case the listing in globalClass is overwritten before operation completion.
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