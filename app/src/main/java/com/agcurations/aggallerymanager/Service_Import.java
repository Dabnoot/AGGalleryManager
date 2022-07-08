package com.agcurations.aggallerymanager;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.text.Html;
import android.util.Log;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TAGS;
import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_THUMBNAIL;
import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TITLE;
import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_LINK;
import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8;

public class Service_Import extends IntentService {

    private static final String ACTION_GET_DIRECTORY_CONTENTS = "com.agcurations.aggallerymanager.action.GET_DIRECTORY_CONTENTS";
    private static final String ACTION_IMPORT_FILES = "com.agcurations.aggallerymanager.action.IMPORT_FILES";
    private static final String ACTION_IMPORT_NHCOMICS = "com.agcurations.aggallerymanager.action.IMPORT_COMICS";
    private static final String ACTION_GET_COMIC_DETAILS_ONLINE = "com.agcurations.aggallerymanager.action.GET_COMIC_DETAILS_ONLINE";
    private static final String ACTION_IMPORT_COMIC_WEB_FILES = "com.agcurations.aggallerymanager.action.IMPORT_COMIC_WEB_FILES";
    private static final String ACTION_IMPORT_COMIC_FOLDERS = "com.agcurations.aggallerymanager.action.IMPORT_COMIC_FOLDERS";
    private static final String ACTION_VIDEO_ANALYZE_HTML = "com.agcurations.aggallerymanager.action.ACTION_VIDEO_ANALYZE_HTML";
    private static final String ACTION_IMPORT_VIDEO_WEB_FILES = "com.agcurations.aggallerymanager.action.ACTION_IMPORT_VIDEO_WEB_FILES";
    private static final String ACTION_DELETE_FILES = "com.agcurations.aggallerymanager.action.DELETE_FILES";

    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";

    public static final String EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_DIRECTORY_CONTENTS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_DIRECTORY_CONTENTS_RESPONSE"; //ArrayList of response data

    public static final String EXTRA_BOOL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE"; //ArrayList of response data

    public static final String EXTRA_STRING_INTENT_ACTION_FILTER = "com.agcurations.aggallerymanager.extra.STRING_INTENT_ACTION_FILTER";

    public Service_Import() {
        super("ImportActivityDataService");
    }


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


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            String sIntentActionFilter = intent.getStringExtra(EXTRA_STRING_INTENT_ACTION_FILTER); //used to send broadcasts to proper receivers.

            GlobalClass globalClass = (GlobalClass) getApplicationContext();

            /*if(ACTION_IMPORT_FILES.equals(action) ||
                    ACTION_IMPORT_COMIC_FOLDERS.equals(action) ||
                    ACTION_IMPORT_NHCOMICS.equals(action) ||
                    ACTION_IMPORT_COMIC_WEB_FILES.equals(action) ||
                    ACTION_IMPORT_VIDEO_WEB_FILES.equals(action)){

                //Set the flags to tell the catalogViewer to view the imported files first:
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                sharedPreferences.edit()
                        .putInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[globalClass.giSelectedCatalogMediaCategory],
                                GlobalClass.SORT_BY_DATETIME_IMPORTED)
                        .putBoolean(GlobalClass.gsCatalogViewerPreferenceNameSortAscending[globalClass.giSelectedCatalogMediaCategory],
                                false)
                        .apply();
            }*/

            if (ACTION_GET_DIRECTORY_CONTENTS.equals(action)) {
                /*final String sImportTreeUri = intent.getStringExtra(EXTRA_IMPORT_TREE_URI);
                Uri uriImportTreeUri = Uri.parse(sImportTreeUri);
                final int iMediaCategory = intent.getIntExtra(EXTRA_MEDIA_CATEGORY,-1);
                final int iFilesOrFolders = intent.getIntExtra(EXTRA_FILES_OR_FOLDERS, FILES_ONLY);
                final int iComicImportSource = intent.getIntExtra(EXTRA_COMIC_IMPORT_SOURCE, -1);
                handleAction_GetDirectoryContents(uriImportTreeUri, iMediaCategory, iFilesOrFolders, iComicImportSource);
                globalClass.gbImportFolderAnalysisRunning = false;
                if(globalClass.gbImportFolderAnalysisStop) {
                    globalClass.gbImportFolderAnalysisStop = false;
                //} else {
                    //Only set "finished" to true if it was not stopped intentionally.
                    //globalClass.gbImportFolderAnalysisFinished = true;   ---Set at the end of the GetDirectoryContents routine before the last broadcast.
                }*/
            } else if (ACTION_IMPORT_FILES.equals(action)) {
                /*final int iMoveOrCopy = intent.getIntExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
                final int iMediaCategory = intent.getIntExtra(EXTRA_MEDIA_CATEGORY, -1);
                handleAction_startActionImportFiles(iMoveOrCopy, iMediaCategory);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;*/
            } else if (ACTION_IMPORT_NHCOMICS.equals(action)) {
                /*final int iMoveOrCopy = intent.getIntExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
                handleAction_startActionImportNHComics(iMoveOrCopy);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;*/
            } else if (ACTION_IMPORT_COMIC_FOLDERS.equals(action)) {
                /*final int iMoveOrCopy = intent.getIntExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
                handleAction_startActionImportComicFolders(iMoveOrCopy);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;*/
            } else if (ACTION_GET_COMIC_DETAILS_ONLINE.equals(action)) {
                /*final String sAddress = intent.getStringExtra(EXTRA_STRING_WEB_ADDRESS);
                handleAction_startActionGetComicDetailsOnline(sAddress, sIntentActionFilter);
                globalClass.gbImportComicWebAnalysisRunning = false;
                globalClass.gbImportComicWebAnalysisFinished = true;*/
            } else if (ACTION_IMPORT_COMIC_WEB_FILES.equals(action)) {
                /*final ItemClass_CatalogItem ci = (ItemClass_CatalogItem) intent.getSerializableExtra(COMIC_CATALOG_ITEM);
                if(ci == null) return;
                try {
                    handleAction_startActionImportComicWebFiles(ci, sIntentActionFilter);
                } catch (IOException e) {
                    e.printStackTrace();
                    problemNotificationConfig(e.getMessage(), sIntentActionFilter);  //todo: make sure that this is properly handled in Execute_Import.
                }
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;*/

            } else if (ACTION_VIDEO_ANALYZE_HTML.equals(action)) {
                /*try{
                    handleAction_startActionVideoAnalyzeHTML();
                } catch (Exception e) {
                    e.printStackTrace();
                }*/

            } else if (ACTION_IMPORT_VIDEO_WEB_FILES.equals(action)) {

                /*final String sWebAddress = intent.getStringExtra(EXTRA_STRING_WEB_ADDRESS);
                try{
                    handleAction_startActionVideoDownload(sWebAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;*/

            } else if (ACTION_DELETE_FILES.equals(action)){

                /*ArrayList<String>  alsUriFTD = intent.getStringArrayListExtra(EXTRA_URI_STRING_ARRAY_FILES_TO_DELETE);
                final String sCallerActionResponseFilter = intent.getStringExtra(EXTRA_CALLER_ACTION_RESPONSE_FILTER);

                if(alsUriFTD == null){
                    alsUriFTD = new ArrayList<>(); //This round-about method to get rid of a warning that "alsUriFilesToDelete might be null."
                }
                final ArrayList<String> alsUriFilesToDelete = alsUriFTD;
                handleAction_startActionDeleteFiles(alsUriFilesToDelete, sCallerActionResponseFilter);*/
            }
        }
    }



    //==============================================================================================
    //===== Service Content ========================================================================
    //==============================================================================================

    public static final int FOLDERS_ONLY = 0;
    public static final int FILES_ONLY = 1;

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




    //==============================================================================================
    //===== Service Communication Utilities ========================================================
    //==============================================================================================




    public static final String UPDATE_LOG_BOOLEAN = "UPDATE_LOG_BOOLEAN";
    public static final String LOG_LINE_STRING = "LOG_LINE_STRING";
    public static final String UPDATE_PERCENT_COMPLETE_BOOLEAN = "UPDATE_PERCENT_COMPLETE_BOOLEAN";
    public static final String PERCENT_COMPLETE_INT = "PERCENT_COMPLETE_INT";
    public static final String UPDATE_PROGRESS_BAR_TEXT_BOOLEAN = "UPDATE_PROGRESS_BAR_TEXT_BOOLEAN";
    public static final String PROGRESS_BAR_TEXT_STRING = "PROGRESS_BAR_TEXT_STRING";

    public void BroadcastProgress(boolean bUpdateLog, String sLogLine,
                                  boolean bUpdatePercentComplete, int iAmountComplete,
                                  boolean bUpdateProgressBarText, String sProgressBarText,
                                  String sIntentActionFilter){

        //Preserve the log for the event of a screen rotation, or activity looses focus:
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        globalClass.gsbImportExecutionLog.append(sLogLine);
        
        if(sIntentActionFilter.equals(Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE)) {
            if (bUpdatePercentComplete) {
                globalClass.giImportExecutionProgressBarPercent = iAmountComplete;
            }
            if (bUpdateProgressBarText) {
                globalClass.gsImportExecutionProgressBarText = sProgressBarText;
            }
        }

        if(sIntentActionFilter.equals(
                Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE)) {
            if(bUpdatePercentComplete) {
                globalClass.giImportFolderAnalysisProgressBarPercent = iAmountComplete;
            }
            if(bUpdateProgressBarText){
                globalClass.gsImportFolderAnalysisProgressBarText = sProgressBarText;
            }
        }

        if(sIntentActionFilter.equals(
                Fragment_Import_2b_SelectSingleWebComic.ImportDataServiceResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE)){
            globalClass.gsbImportComicWebAnalysisLog.append(sLogLine);
        }


        //Broadcast a message to be picked-up by the Import Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(sIntentActionFilter);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        broadcastIntent.putExtra(UPDATE_LOG_BOOLEAN, bUpdateLog);
        broadcastIntent.putExtra(LOG_LINE_STRING, sLogLine);
        broadcastIntent.putExtra(UPDATE_PERCENT_COMPLETE_BOOLEAN, bUpdatePercentComplete);
        broadcastIntent.putExtra(PERCENT_COMPLETE_INT, iAmountComplete);
        broadcastIntent.putExtra(UPDATE_PROGRESS_BAR_TEXT_BOOLEAN, bUpdateProgressBarText);
        broadcastIntent.putExtra(PROGRESS_BAR_TEXT_STRING, sProgressBarText);

        //sendBroadcast(broadcastIntent);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }



}