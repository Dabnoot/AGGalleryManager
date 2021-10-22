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

    private static final String EXTRA_IMPORT_TREE_URI = "com.agcurations.aggallerymanager.extra.IMPORT_TREE_URI";
    private static final String EXTRA_MEDIA_CATEGORY = "com.agcurations.aggallerymanager.extra.MEDIA_CATEGORY";
    private static final String EXTRA_FILES_OR_FOLDERS = "com.agcurations.aggallerymanager.extra.EXTRA_FILES_OR_FOLDERS";

    private static final String EXTRA_COMIC_IMPORT_SOURCE = "com.agcurations.aggallerymanager.extra.COMIC_IMPORT_SOURCE";

    private static final String EXTRA_IMPORT_FILES_MOVE_OR_COPY = "com.agcurations.aggallerymanager.extra.IMPORT_FILES_MOVE_OR_COPY";

    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";

    public static final String EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_DIRECTORY_CONTENTS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_DIRECTORY_CONTENTS_RESPONSE"; //ArrayList of response data

    public static final String EXTRA_BOOL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE"; //ArrayList of response data

    public static final String EXTRA_STRING_WEB_ADDRESS = "com.agcurations.aggallerymanager.extra.STRING_WEB_ADDRESS";
    public static final String COMIC_DETAILS_SUCCESS = "COMIC_DETAILS_SUCCESS";
    public static final String COMIC_CATALOG_ITEM = "COMIC_CATALOG_ITEM";

    public static final String EXTRA_STRING_INTENT_ACTION_FILTER = "com.agcurations.aggallerymanager.extra.STRING_INTENT_ACTION_FILTER";

    public static final String EXTRA_URI_STRING_ARRAY_FILES_TO_DELETE = "com.agcurations.aggallerymanager.extra.URI_STRING_ARRAY_FILES_TO_DELETE";

    public static final String EXTRA_CALLER_ACTION_RESPONSE_FILTER = "com.agcurations.aggallerymanager.extra.EXTRA_CALLER_ACTION_RESPONSE_FILTER";

    public static final String FILE_DELETION_MESSAGE = "Deleting file: ";
    public static final String FILE_DELETION_OP_COMPLETE_MESSAGE = "File deletion operation complete.";

    public static final String STRING_COMIC_XML_FILENAME = "ComicData.xml";

    public Service_Import() {
        super("ImportActivityDataService");
    }


    public static void startActionGetDirectoryContents(Context context, Uri uriImportTreeUri, int iMediaCategory, int iFilesOrFolders, int iComicImportSource) {
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_GET_DIRECTORY_CONTENTS);
        String sImportTreeUri = uriImportTreeUri.toString();
        intent.putExtra(EXTRA_IMPORT_TREE_URI, sImportTreeUri);
        intent.putExtra(EXTRA_MEDIA_CATEGORY, iMediaCategory);
        intent.putExtra(EXTRA_FILES_OR_FOLDERS, iFilesOrFolders);
        intent.putExtra(EXTRA_COMIC_IMPORT_SOURCE, iComicImportSource);
        context.startService(intent);
    }

    public static void startActionImportFiles(Context context, int iMoveOrCopy, int iMediaCategory) {
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_FILES);
        intent.putExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy);
        intent.putExtra(EXTRA_MEDIA_CATEGORY, iMediaCategory);
        context.startService(intent);
    }

    public static void startActionImportNHComicsFiles(Context context, int iMoveOrCopy, int iComicImportSource) {
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_NHCOMICS);
        intent.putExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy);
        intent.putExtra(EXTRA_COMIC_IMPORT_SOURCE, iComicImportSource);
        context.startService(intent);
    }

    public static void startActionImportComicFolders(Context context, int iMoveOrCopy, int iComicImportSource) {
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_COMIC_FOLDERS);
        intent.putExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy);
        intent.putExtra(EXTRA_COMIC_IMPORT_SOURCE, iComicImportSource);
        context.startService(intent);
    }

    public static void startActionAcquireNHComicsDetails(Context context, String sAddress, String sIntentActionFilter){
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_GET_COMIC_DETAILS_ONLINE);
        intent.putExtra(EXTRA_STRING_WEB_ADDRESS, sAddress);
        intent.putExtra(EXTRA_STRING_INTENT_ACTION_FILTER, sIntentActionFilter);
        context.startService(intent);
    }

    public static void startActionImportComicWebFiles(Context context, ItemClass_CatalogItem ci, String sIntentActionFilter){
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_COMIC_WEB_FILES);
        intent.putExtra(COMIC_CATALOG_ITEM, ci);
        intent.putExtra(EXTRA_STRING_INTENT_ACTION_FILTER, sIntentActionFilter);
        context.startService(intent);
    }

    public static void startActionVideoAnalyzeHTML(Context context){
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_VIDEO_ANALYZE_HTML);
        context.startService(intent);

    }

    public static void startActionVideoDownload(Context context, String sWebPageAddress){
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_VIDEO_WEB_FILES);
        intent.putExtra(EXTRA_STRING_WEB_ADDRESS, sWebPageAddress);
        context.startService(intent);
    }

    public static void startActionDeleteFiles(Context context, ArrayList<String> alsUriFilesToDelete, String sCallerActionResponseFilter){
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_DELETE_FILES);

        intent.putExtra(EXTRA_URI_STRING_ARRAY_FILES_TO_DELETE, alsUriFilesToDelete);
        intent.putExtra(EXTRA_CALLER_ACTION_RESPONSE_FILTER, sCallerActionResponseFilter);

        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            String sIntentActionFilter = intent.getStringExtra(EXTRA_STRING_INTENT_ACTION_FILTER); //used to send broadcasts to proper receivers.

            GlobalClass globalClass = (GlobalClass) getApplicationContext();

            if(ACTION_IMPORT_FILES.equals(action) ||
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
            }

            if (ACTION_GET_DIRECTORY_CONTENTS.equals(action)) {
                final String sImportTreeUri = intent.getStringExtra(EXTRA_IMPORT_TREE_URI);
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
                }
            } else if (ACTION_IMPORT_FILES.equals(action)) {
                final int iMoveOrCopy = intent.getIntExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
                final int iMediaCategory = intent.getIntExtra(EXTRA_MEDIA_CATEGORY, -1);
                handleAction_startActionImportFiles(iMoveOrCopy, iMediaCategory);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;
            } else if (ACTION_IMPORT_NHCOMICS.equals(action)) {
                final int iMoveOrCopy = intent.getIntExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
                handleAction_startActionImportNHComics(iMoveOrCopy);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;
            } else if (ACTION_IMPORT_COMIC_FOLDERS.equals(action)) {
                final int iMoveOrCopy = intent.getIntExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
                handleAction_startActionImportComicFolders(iMoveOrCopy);
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;
            } else if (ACTION_GET_COMIC_DETAILS_ONLINE.equals(action)) {
                final String sAddress = intent.getStringExtra(EXTRA_STRING_WEB_ADDRESS);
                handleAction_startActionGetComicDetailsOnline(sAddress, sIntentActionFilter);
                globalClass.gbImportComicWebAnalysisRunning = false;
                globalClass.gbImportComicWebAnalysisFinished = true;
            } else if (ACTION_IMPORT_COMIC_WEB_FILES.equals(action)) {
                final ItemClass_CatalogItem ci = (ItemClass_CatalogItem) intent.getSerializableExtra(COMIC_CATALOG_ITEM);
                if(ci == null) return;
                try {
                    handleAction_startActionImportComicWebFiles(ci, sIntentActionFilter);
                } catch (IOException e) {
                    e.printStackTrace();
                    problemNotificationConfig(e.getMessage(), sIntentActionFilter);  //todo: make sure that this is properly handled in Execute_Import.
                }
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;

            } else if (ACTION_VIDEO_ANALYZE_HTML.equals(action)) {
                try{
                    handleAction_startActionVideoAnalyzeHTML();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (ACTION_IMPORT_VIDEO_WEB_FILES.equals(action)) {

                final String sWebAddress = intent.getStringExtra(EXTRA_STRING_WEB_ADDRESS);
                try{
                    handleAction_startActionVideoDownload(sWebAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;

            } else if (ACTION_DELETE_FILES.equals(action)){

                ArrayList<String>  alsUriFTD = intent.getStringArrayListExtra(EXTRA_URI_STRING_ARRAY_FILES_TO_DELETE);
                final String sCallerActionResponseFilter = intent.getStringExtra(EXTRA_CALLER_ACTION_RESPONSE_FILTER);

                if(alsUriFTD == null){
                    alsUriFTD = new ArrayList<>(); //This round-about method to get rid of a warning that "alsUriFilesToDelete might be null."
                }
                final ArrayList<String> alsUriFilesToDelete = alsUriFTD;
                handleAction_startActionDeleteFiles(alsUriFilesToDelete, sCallerActionResponseFilter);
            }
        }
    }



    //==============================================================================================
    //===== Service Content ========================================================================
    //==============================================================================================



    public static final int FOLDERS_ONLY = 0;
    public static final int FILES_ONLY = 1;

    //todo: Separate comic folder analysis from GetDirectoryContents.
    private void handleAction_GetDirectoryContents(Uri uriImportTreeUri, int iMediaCategory, int iFilesOrFolders, int iComicImportSource) {
        if(Activity_Import.guriImportTreeURI != null){
            GlobalClass globalClass = (GlobalClass) getApplicationContext();

            long lProgressNumerator = 0L;
            long lProgressDenominator = 0L;
            int iProgressBarValue = 0;

            Intent broadcastIntent_GetDirectoryContentsResponse = new Intent();
            ArrayList<ItemClass_File> alFileList = new ArrayList<>();
            try {

                //Get data about the files from the UriTree:
                ContentResolver contentResolver = getApplicationContext().getContentResolver();
                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriImportTreeUri, DocumentsContract.getTreeDocumentId(uriImportTreeUri));
                List<Uri> dirNodes = new LinkedList<>();
                dirNodes.add(childrenUri);
                childrenUri = dirNodes.remove(0); // get the item from top
                String sSortOrder = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC"; //Sort does not appear to work.
                Cursor cImport = contentResolver.query(childrenUri,
                        new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                DocumentsContract.Document.COLUMN_MIME_TYPE,
                                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                                DocumentsContract.Document.COLUMN_SIZE,
                                DocumentsContract.Document.COLUMN_SUMMARY,
                                DocumentsContract.Document.COLUMN_FLAGS,
                                DocumentsContract.Document.COLUMN_ICON},
                        null,
                        null,
                        sSortOrder);

                if(cImport != null) {





                    //Calculate total number of files for a progress bar:
                    lProgressDenominator = cImport.getCount();

                    if((iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) &&
                            (iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER)) {
                        //If we are importing comics by the folder, calculate a new progressbar denominator.
                        //Go through the folders in the users' chosen directory and count the files.
                        //  Each will be processed, and add a tick to the progressbar.

                        long lProgressDenominatorQuick = lProgressDenominator;

                        cImport.moveToPosition(-1);
                        while (cImport.moveToNext() && !globalClass.gbImportFolderAnalysisStop) {
                            String sSubFolderDocID = cImport.getString(0);
                            String sSubFolderMimeType = cImport.getString(2);

                            boolean bSubFolderItemIsDirectory;
                            bSubFolderItemIsDirectory = (sSubFolderMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR));
                            if (bSubFolderItemIsDirectory) { //Only process directories/folders.
                                Uri uriSubfiles = DocumentsContract.buildChildDocumentsUriUsingTree(uriImportTreeUri, sSubFolderDocID);
                                List<Uri> luriSubfiles = new LinkedList<>();
                                luriSubfiles.add(uriSubfiles);
                                uriSubfiles = luriSubfiles.remove(0); // get the item from top
                                sSortOrder = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC"; //Sort does not appear to work.
                                Cursor cSubfiles = contentResolver.query(uriSubfiles,
                                        new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                                DocumentsContract.Document.COLUMN_MIME_TYPE},
                                        null,
                                        null,
                                        sSortOrder);

                                if (cSubfiles != null) { //If the query came back with results...
                                    lProgressDenominator += cSubfiles.getCount(); //Count the files in the folder.
                                    cSubfiles.close(); //Close the query.
                                }

                            }

                            lProgressNumerator++;
                            iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominatorQuick) * 1000);
                            BroadcastProgress(false, "",
                                    true, iProgressBarValue,
                                    true, lProgressNumerator + "/" + lProgressDenominatorQuick,
                                    Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

                        }
                    }

                    lProgressNumerator = 0L;

                    BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "0/" + lProgressDenominator,
                            Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);


                    MediaMetadataRetriever mediaMetadataRetriever;
                    mediaMetadataRetriever = new MediaMetadataRetriever();

                    cImport.moveToPosition(-1);
                    while (cImport.moveToNext() && !globalClass.gbImportFolderAnalysisStop) {

                        //Update progress bar:
                        //Update progress right away in order to avoid instances in which a loop is skipped.
                        lProgressNumerator++;
                        iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 1000);
                        BroadcastProgress(false, "",
                                true, iProgressBarValue,
                                true, lProgressNumerator + "/" + lProgressDenominator,
                                Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);


                        final String docId = cImport.getString(0);
                        final String docName = cImport.getString(1);
                        final String mimeType = cImport.getString(2);
                        final long lLastModified = cImport.getLong(3); //milliseconds since January 1, 1970 00:00:00.0 UTC.
                        final String sFileSize = cImport.getString(4);

                        boolean isDirectory;
                        isDirectory = (mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR));
                        int iTypeFileOrFolder = (isDirectory) ? ItemClass_File.TYPE_FOLDER : ItemClass_File.TYPE_FILE;

                        if ((iFilesOrFolders == FILES_ONLY) && (isDirectory)) {
                            continue; //skip the rest of the for loop for this item.
                        } else if ((iFilesOrFolders == FOLDERS_ONLY) && (!isDirectory)) {
                            continue; //skip the rest of the for loop for this item.
                        }


                        long lFileSize;
                        String fileExtension;
                        //If the file is video, get the duration so that the file list can be sorted by duration if requested.
                        long lDurationInMilliseconds = -1L;
                        String sWidth = "";  //We are not doing math with the width and height. Therefore no need to convert to int.
                        String sHeight = "";

                        //Get a Uri for this individual document:
                        final Uri docUri = DocumentsContract.buildDocumentUriUsingTree(childrenUri, docId);

                        /*GlobalClass globalClass = (GlobalClass) getApplicationContext();
                        File fStorageDir = Environment.getExternalStorageDirectory();

                        problemNotificationConfig(
                                docUri.toString(), RECEIVER_STORAGE_LOCATION);

                        File fTestDir = null;
                        if(docUri.toString().startsWith("content://com.android.externalstorage.documents/tree/0000-0000")) {
                            fTestDir = new File("/storage/0000-0000/FireMimi/1558833241126.webm");
                            if (fTestDir.exists()) {
                                Log.d("Test", "Dir exists.");
                            } else {
                                Log.d("Test", "Dir doest not exist.");
                            }
                            return;
                        }
                        if(fTestDir == null){
                            return;
                        }*/



                        //Get date last modified:
                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.setTimeInMillis(lLastModified);
                        Date dateLastModified = cal.getTime();

                        //Data for comic from xml if available:
                        String sTitle = "";
                        String sURL = "";
                        String sPageCount = "";
                        String sArtist = "";
                        String sParody = "";
                        ArrayList<Integer> aliRecognizedTags = new ArrayList<>();
                        ArrayList<String> alsUnidentifiedTags = new ArrayList<>();

                        if (!isDirectory) {

                            //Record the file extension:
                            fileExtension = docName.contains(".") ? docName.substring(docName.lastIndexOf(".")) : "";
                            //If the file extension does not match the file extension regex, skip the remainder of the loop.
                            if (!fileExtension.matches(".+")) {
                                continue;  //skip the rest of the loop if the file extension does not match.
                            }

                            //Determine the file size:
                            lFileSize = Long.parseLong(sFileSize); //size in Bytes

                            //If this is a file, check to see if it is a video or an image and if we are looking for videos or images.
                            if (mimeType.startsWith("video") || fileExtension.equals(".gif") ||
                                    (mimeType.equals("application/octet-stream") && fileExtension.equals(".mp4"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid
                                //If video...
                                if ((iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES)
                                        || (iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS)) {
                                    continue; //If requesting images or comics, and mimeType is video or the file a gif, go to next loop.
                                }
                            } else {
                                //If not video...
                                if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                                    continue; //If requesting videos, and mimeType is not video nor is the file a gif, go to next loop.
                                }
                            }

                            if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS && GlobalClass.bVideoDeepDirectoryContentFileAnalysis) {
                                if (mimeType.startsWith("video") ||
                                        (mimeType.equals("application/octet-stream") && fileExtension.equals(".mp4"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid
                                    try {
                                        mediaMetadataRetriever.setDataSource(getApplicationContext(), docUri);
                                    } catch (Exception e) {
                                        //problemNotificationConfig(e.getMessage() + "\n" + docName, RECEIVER_STORAGE_LOCATION);
                                        continue; //Skip the rest of this loop.
                                    }
                                    sWidth = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                                    sHeight = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                                    String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                                    if(time != null) {
                                        lDurationInMilliseconds = Long.parseLong(time);
                                    }
                                } else { //if it's not a video file, check to see if it's a gif:
                                    if (fileExtension.equals(".gif")) {
                                        //Get the duration of the gif image:
                                        Context activityContext = getApplicationContext();
                                        try {
                                            pl.droidsonroids.gif.GifDrawable gd = new pl.droidsonroids.gif.GifDrawable(activityContext.getContentResolver(), docUri);
                                            lDurationInMilliseconds = gd.getDuration();
                                            sWidth = "" + gd.getIntrinsicWidth();
                                            sHeight = "" + gd.getIntrinsicHeight();
                                        } catch (Exception e) {
                                            problemNotificationConfig(e.getMessage() + "\n" + docName, Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
                                            continue; //Skip the rest of this loop.
                                        }
                                    }
                                }
                            } else if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                                //Get the width and height of the image:
                                try {
                                    InputStream input = this.getContentResolver().openInputStream(docUri);
                                    if (input != null) {
                                        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                                        onlyBoundsOptions.inJustDecodeBounds = true;
                                        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
                                        input.close();
                                        sWidth = "" + onlyBoundsOptions.outWidth;
                                        sHeight = "" + onlyBoundsOptions.outHeight;
                                    }

                                } catch (Exception e) {
                                    continue; //Skip the rest of this loop.
                                }
                            }



                            //Convert the file Uri to string. Uri's don't transport well as intent extras.
                            String sUri = docUri.toString();

                            //create the file model and initialize:
                            ItemClass_File icfFileItem = new ItemClass_File(iTypeFileOrFolder, docName);
                            icfFileItem.sExtension = fileExtension;
                            icfFileItem.lSizeBytes = lFileSize;
                            icfFileItem.dateLastModified = dateLastModified;
                            icfFileItem.sWidth = sWidth;
                            icfFileItem.sHeight = sHeight;
                            icfFileItem.sUri = sUri;
                            icfFileItem.sMimeType = mimeType;
                            icfFileItem.lVideoTimeInMilliseconds = lDurationInMilliseconds;

                            //Add the ItemClass_File to the ArrayList:
                            alFileList.add(icfFileItem);




                        } else {
                            //File item IS a directory...
                            if((iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) &&
                                    (iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER)){
                                //If the user is getting directory contents for importing comics by the folder...








                                //Get a list of files in the folder:
                                //Uri uriComicPages = DocumentsContract.buildChildDocumentsUriUsingTree(docUri, DocumentsContract.getTreeDocumentId(docUri));
                                Uri uriComicPages = DocumentsContract.buildChildDocumentsUriUsingTree(uriImportTreeUri, docId);
                                List<Uri> luriComicPages = new LinkedList<>();
                                luriComicPages.add(uriComicPages);
                                uriComicPages = luriComicPages.remove(0); // get the item from top
                                sSortOrder = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC"; //Sort does not appear to work.
                                Cursor cComicPages = contentResolver.query(uriComicPages,
                                        new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                                DocumentsContract.Document.COLUMN_MIME_TYPE,
                                                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                                                DocumentsContract.Document.COLUMN_SIZE,
                                                DocumentsContract.Document.COLUMN_SUMMARY,
                                                DocumentsContract.Document.COLUMN_FLAGS,
                                                DocumentsContract.Document.COLUMN_ICON},
                                        null,
                                        null,
                                        sSortOrder);

                                if(cComicPages != null) {

                                    //Anaylyze potential comic pages in this subfolder:

                                    ArrayList<ItemClass_File> alicf_ComicFiles = new ArrayList<>();

                                    while (cComicPages.moveToNext()) {

                                        //Write progress here since some of the lower steps will cause the loop to skip processing:
                                        lProgressNumerator++;
                                        iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 1000);
                                        BroadcastProgress(false, "",
                                                true, iProgressBarValue,
                                                true, lProgressNumerator + "/" + lProgressDenominator,
                                                Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

                                        //Analyze the file item.

                                        //If this file is a folder, skip to the next item:
                                        final String sComicPageFilename = cComicPages.getString(1);
                                        final String sComicPageMimeType = cComicPages.getString(2);
                                        boolean bComicPageIsDirectory;
                                        bComicPageIsDirectory = (sComicPageMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR));
                                        int iComicPageItemFileType = (bComicPageIsDirectory) ? ItemClass_File.TYPE_FOLDER : ItemClass_File.TYPE_FILE;
                                        if((iComicPageItemFileType != ItemClass_File.TYPE_FILE) ||
                                                (!sComicPageMimeType.contains("image") && !sComicPageFilename.equals(STRING_COMIC_XML_FILENAME))){
                                            continue;
                                        }


                                        //Build a ItemClass_File item for this file:

                                        ItemClass_File icf_ComicPage = new ItemClass_File(iComicPageItemFileType, sComicPageFilename);

                                        //Get a Uri for this individual document:
                                        final String sComicPageDocId = cComicPages.getString(0);
                                        final Uri uriComicPageUri = DocumentsContract.buildDocumentUriUsingTree(childrenUri, sComicPageDocId);
                                        icf_ComicPage.sUri = uriComicPageUri.toString();

                                        //Set the mime type:
                                        icf_ComicPage.sMimeType = sComicPageMimeType;

                                        //Get date last modified:
                                        long lComicPageLastModified = cComicPages.getLong(3); //milliseconds since January 1, 1970 00:00:00.0 UTC.

                                        cal.setTimeInMillis(lComicPageLastModified);
                                        icf_ComicPage.dateLastModified = cal.getTime();

                                        //Record the file extension:
                                        fileExtension = sComicPageFilename.contains(".") ? sComicPageFilename.substring(sComicPageFilename.lastIndexOf(".")) : "";
                                        icf_ComicPage.sExtension = fileExtension;

                                        //Record the file size:
                                        final String sComicPageFileSize = cComicPages.getString(4);
                                        lFileSize = Long.parseLong(sComicPageFileSize); //size in Bytes
                                        icf_ComicPage.lSizeBytes = lFileSize;

                                        //Get the image dimensions:
                                        if(icf_ComicPage.sMimeType.contains("image")) {
                                            try {
                                                InputStream input = this.getContentResolver().openInputStream(uriComicPageUri);
                                                if (input != null) {
                                                    BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                                                    onlyBoundsOptions.inJustDecodeBounds = true;
                                                    BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
                                                    input.close();
                                                    sWidth = "" + onlyBoundsOptions.outWidth;
                                                    sHeight = "" + onlyBoundsOptions.outHeight;
                                                }

                                            } catch (Exception e) {
                                                continue; //Skip the rest of this loop.
                                            }
                                            icf_ComicPage.sWidth = sWidth;
                                            icf_ComicPage.sHeight = sHeight;
                                        } else {
                                            //Check to see if this is an xml file.
                                            //If this is an xml file, it likely contains comic details.

                                            if(sComicPageFilename.equals(STRING_COMIC_XML_FILENAME)) {
                                                //Get Document Builder
                                                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                                                DocumentBuilder builder = factory.newDocumentBuilder();
                                                //Build Document
                                                InputStream inputStream = contentResolver.openInputStream(uriComicPageUri);
                                                Document docComicXML = builder.parse(inputStream);
                                                inputStream.close();

                                                sTitle = getXMLNodeText(docComicXML, "ComicTitle");
                                                sURL = getXMLNodeText(docComicXML, "URL");
                                                sPageCount = getXMLNodeText(docComicXML, "PageCount");
                                                sArtist = getXMLNodeText(docComicXML, "Artist");
                                                sParody = getXMLNodeText(docComicXML, "Parody");

                                                NodeList nlTemp;
                                                ArrayList<String> alsTags = new ArrayList<>();
                                                nlTemp = docComicXML.getElementsByTagName("Tag");
                                                for(int i = 0; i < nlTemp.getLength(); i++) {
                                                    alsTags.add(nlTemp.item(i).getTextContent());
                                                }
                                                //Pre-process tags. Identify tags that already exist, and create a list of new tags for
                                                //  the user to approve - don't automatically add new tags to the system (I've encountered
                                                //  garbage tags, tags that already exist in another form, and tags that the user might
                                                //  not want to add.
                                                for(String sTag: alsTags){
                                                    String sIncomingTagCleaned = sTag.toLowerCase().trim();
                                                    boolean bTagFound = false;
                                                    for(Map.Entry<String, ItemClass_Tag> TagEntry: globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()){
                                                        String sExistingTagCleaned = TagEntry.getKey().toLowerCase().trim();
                                                        if(sExistingTagCleaned.equals(sIncomingTagCleaned)){
                                                            bTagFound = true;
                                                            aliRecognizedTags.add(TagEntry.getValue().iTagID);
                                                            break;
                                                        }
                                                    }
                                                    if(!bTagFound){
                                                        alsUnidentifiedTags.add(sTag.trim());
                                                    }
                                                }

                                            } //End if this is an xml file containing comic data.

                                        } //End if this is/not an image file.

                                        //Set the parent folder Uri for various uses later:
                                        icf_ComicPage.sUriParent = docUri.toString();

                                        //Now determine opportunities to decypher comic page number:
                                        ArrayList<Integer[]> aliNumberBlocksSingleFile = new ArrayList<>();
                                        boolean bDigitBlockOn = false;
                                        Integer[] iDigitBlock = {0,0};
                                        for(int i = 0; i < sComicPageFilename.length(); i++){
                                            if(Character.isDigit(sComicPageFilename.charAt(i))){
                                                if(!bDigitBlockOn){
                                                    iDigitBlock[0] = i;
                                                }
                                                bDigitBlockOn = true;
                                            } else {
                                                if(bDigitBlockOn){
                                                    iDigitBlock[1] = i;
                                                    aliNumberBlocksSingleFile.add(iDigitBlock);
                                                    iDigitBlock = new Integer[]{0,0};
                                                }
                                                bDigitBlockOn = false;
                                            }
                                        }
                                        //Blocks of digits now acquired. Assign to the file item:
                                        icf_ComicPage.aliNumberBlocks = aliNumberBlocksSingleFile;


                                        //Add this file item to the list for report back to caller:
                                        alicf_ComicFiles.add(icf_ComicPage);


                                    }
                                    cComicPages.close();

                                    //Create a file item to track this comic folder:
                                    ItemClass_File icf_ComicFolderItem = new ItemClass_File(ItemClass_File.TYPE_FOLDER, docName);
                                    icf_ComicFolderItem.sUri = docUri.toString();

                                    icf_ComicFolderItem.dateLastModified = dateLastModified;

                                    //Transfer data acquired from an XML file (no worries if no XML file contained the data):
                                    icf_ComicFolderItem.sTitle = sTitle;
                                    icf_ComicFolderItem.sURL = sURL;
                                    icf_ComicFolderItem.sPageCount = sPageCount;
                                    icf_ComicFolderItem.sArtist = sArtist;
                                    icf_ComicFolderItem.sParody = sParody;
                                    icf_ComicFolderItem.aliRecognizedTags = aliRecognizedTags;
                                    icf_ComicFolderItem.aliProspectiveTags = new ArrayList<>(aliRecognizedTags); //Don't copy the pointer.
                                    icf_ComicFolderItem.alsUnidentifiedTags = alsUnidentifiedTags;

                                    //All of the comic pages in this directory have been added to an
                                    //  arraylist of file items. Now look for a page numbering pattern:

                                    //Verify that all pages have the same quantity of number blocks (telling where the page number might be in the filename):
                                    boolean bQtyNumBlocksOk = false;
                                    int iNumBlocks = -1;
                                    for(ItemClass_File file: alicf_ComicFiles){
                                        if(file.sFileOrFolderName.equals(STRING_COMIC_XML_FILENAME)){
                                            continue; //Don't try to find a page number on this file.
                                        }
                                        if(iNumBlocks == -1){  //If this is the first set of number blocks we are testing, record the count.
                                            iNumBlocks = file.aliNumberBlocks.size();
                                            bQtyNumBlocksOk = true;
                                        } else {
                                            if(iNumBlocks != file.aliNumberBlocks.size()){  //Compare the count of number blocks.
                                                bQtyNumBlocksOk = false;
                                                //Report a problem with this file:
                                                String sMessage = "Problem identifying page number for comic in folder \"" +
                                                        docName + "\", file \"" + file.sFileOrFolderName +
                                                        "\". Note that the system uses alphabetization to sort comic pages.\n";
                                                globalClass.gsbImportFolderAnalysisLog.append(sMessage);
                                                problemNotificationConfig(sMessage, Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
                                            }
                                        }
                                    }
                                    if(!bQtyNumBlocksOk){
                                        //If we need to stop analyzing the comic page order due to a numbering problem...
                                        icf_ComicFolderItem.iNumberBlockStatus = ItemClass_File.COMIC_PAGE_ID_BLOCK_COUNT_INCONSISTENT;
                                        //Assign the thumbnail file to the first file in the list.
                                        if(alicf_ComicFiles.size() > 0) {
                                            icf_ComicFolderItem.sUriThumbnailFile = alicf_ComicFiles.get(0).sUri;
                                        }
                                    } else {
                                        //Attempt to identify the number block representing the page ID for the set of image files in the folder:
                                        TreeMap<Integer, String> tmComicFilesPageNumCheck;
                                        for(int iNumberBlock = 0; iNumberBlock < iNumBlocks; iNumberBlock++) {
                                            tmComicFilesPageNumCheck = new TreeMap<>(); //Reset the file ordering.

                                            boolean bPossibleNumberBlockCandidate = true;
                                            for (ItemClass_File icfComicPage : alicf_ComicFiles) {
                                                if(icfComicPage.sFileOrFolderName.equals(STRING_COMIC_XML_FILENAME)){
                                                    continue; //Don't try to find a page number on this file.
                                                }
                                                //Attempt to get the integer value from this filename using the current number block:
                                                String sPageID  = icfComicPage.sFileOrFolderName.substring(icfComicPage.aliNumberBlocks.get(iNumberBlock)[0], icfComicPage.aliNumberBlocks.get(iNumberBlock)[1]);
                                                if(sPageID.length() > 4){
                                                    sPageID = sPageID.substring(sPageID.length() - 4);  //Filename might be merely a datetime stamp.
                                                }
                                                if(sPageID.length() <= 4) {
                                                    //This app is not designed to work with pages greater than 9999.
                                                    //  The comic page viewer will not properly sort the 10,000th page
                                                    //  alphabetically. Just assume there are no comics out there with more than 9999 pages.

                                                    Integer iPageID = Integer.parseInt(sPageID);
                                                    //Check to see if this page number already exists in the TreeMap:
                                                    if (tmComicFilesPageNumCheck.containsKey(iPageID)) {
                                                        //If the page number already exists in the TreeMap, this cannot be the \
                                                        //  page number. Exit the loop and attempt to test the next number block;
                                                        bPossibleNumberBlockCandidate = false;
                                                        break;
                                                    }
                                                    tmComicFilesPageNumCheck.put(iPageID, icfComicPage.sUri);
                                                }
                                            }
                                            if(bPossibleNumberBlockCandidate){
                                                //If all of the found possible page IDs for this comic are unique,
                                                //  now test the pages for order:
                                                int iOrder = 1;
                                                //tmComicFilesPageNumCheck should have the pages sorted by the key, the page number based
                                                //  on the current number block in the filename.
                                                for(Map.Entry<Integer, String> tmPageEntry: tmComicFilesPageNumCheck.entrySet()){
                                                    int iThisPageID = tmPageEntry.getKey();
                                                    if(iThisPageID > (iOrder + GlobalClass.iComicFolderImportMaxPageSkip)){
                                                        //If the current pageID is is greater than the max skip page value (default,
                                                        // or possibly set by user in a later version of this program, go examine the next ID block.
                                                        icf_ComicFolderItem.iNumberBlockStatus = ItemClass_File.COMIC_PAGE_ID_BLOCK_COUNT_SKIPS_PAGES;
                                                        icf_ComicFolderItem.sUriThumbnailFile = ""; //Reset the possible thumbnail file.
                                                        break;
                                                    } else {
                                                        iOrder = iThisPageID;
                                                    }
                                                    if(icf_ComicFolderItem.sUriThumbnailFile.equals("")){
                                                        //Assign the thumbnail file to the first file in the list.
                                                        icf_ComicFolderItem.sUriThumbnailFile = tmPageEntry.getValue();
                                                    }

                                                }

                                                if(icf_ComicFolderItem.iNumberBlockStatus == ItemClass_File.COMIC_PAGE_ID_BLOCK_COUNT_OK){
                                                    //This is likely a number block ok for measuring the comic pages.
                                                    //Set the number block index so that this number block is used by the import process.
                                                    icf_ComicFolderItem.iNumberBlockPageIDIndex = iNumberBlock;
                                                    break; //Quit the loop looking at the number blocks.
                                                }


                                            }

                                        } //End loop looking for number block identifying the page ID in the set of comic pages.

                                        if(icf_ComicFolderItem.sUriThumbnailFile.equals("")){
                                            //If we still have not decided what the thumbnail will be,
                                            //  Set it to the first file when sorting alphabetically:
                                            TreeMap<String, String> tmAlphabetizedComicFiles = new TreeMap<>();
                                            for(ItemClass_File file: alicf_ComicFiles){
                                                tmAlphabetizedComicFiles.put(file.sFileOrFolderName,file.sUri);
                                            }
                                            Map.Entry<String, String> meFirst = tmAlphabetizedComicFiles.firstEntry();
                                            icf_ComicFolderItem.sUriThumbnailFile = meFirst.getValue();

                                        }


                                    } //End if all comic pages in this comic have the same quantity of number blocks in the file name.

                                    //Code to deliver the comic files and the comic folder FileItem:
                                    if(alicf_ComicFiles.size() > 0) {
                                        alFileList.add(icf_ComicFolderItem);
                                        alFileList.addAll(alicf_ComicFiles);
                                    }

                                } //End if comic folder has comic files.

                            } //End if user is importing comics and has selected to import from folder(s).

                        } //End if "isDirectory".


                    } //End loop going through the folder that the user selected.

                    cImport.close();
                    mediaMetadataRetriever.release();

                } //End if "there are items in the folder that the user selected.

            }catch (Exception e){
                //todo: This does not appear to make it to the import activity:
                String sMessage = "Problem during handleAction_GetDirectoryContents: " + e.getMessage();
                globalClass.gsbImportFolderAnalysisLog.append(sMessage);
                problemNotificationConfig(e.getMessage(), Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
                return;
            }

            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, true);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE, alFileList);

            //Send broadcast to the Import Activity:
            broadcastIntent_GetDirectoryContentsResponse.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
            broadcastIntent_GetDirectoryContentsResponse.addCategory(Intent.CATEGORY_DEFAULT);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);

            //Set finished and broadcast so that the fragment knows that we are done.
            globalClass.gbImportFolderAnalysisFinished = true;
            BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, lProgressNumerator + "/" + lProgressDenominator,
                    Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

        }
    }
    private String getXMLNodeText(Document docXML, String sNodeTagName){
        //This routine used to simplify coding in handleAction_GetDirectoryContents for Comic XML read.
        NodeList nlTemp;
        String sReturnData = "";
        nlTemp = docXML.getElementsByTagName(sNodeTagName);
        if (nlTemp.getLength() > 0){
            sReturnData = nlTemp.item(0).getTextContent();
        }
        return sReturnData;
    }

    private void handleAction_startActionImportFiles(int iMoveOrCopy, int iMediaCategory) {

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;
        long lLoopBytesRead;


        ContentResolver contentResolver = getApplicationContext().getContentResolver();

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        ArrayList<ItemClass_File> alFileList = globalClass.galImportFileList;

        //Calculate total size of all files to import:
        for(ItemClass_File fi: alFileList){
            lTotalImportSize = lTotalImportSize + fi.lSizeBytes;
        }
        lProgressDenominator = lTotalImportSize;

        String sLogLine;

        boolean bCopyViaWorker = true;

        StringBuilder sbJobFileRecords = new StringBuilder();

        ArrayList<String> alsVerifiedDestinationFolders = new ArrayList<>(); //Used to avoid re-confirm and additional messaging.

        //Loop and import files:
        for(ItemClass_File fileItem: alFileList) {

            if(fileItem.bMarkedForDeletion){
                Uri uriSourceFileToDelete;
                uriSourceFileToDelete = Uri.parse(fileItem.sUri);
                DocumentFile dfSourceToDelete = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFileToDelete);
                sLogLine = "Deleting file: " + dfSourceToDelete.getName();
                if (!dfSourceToDelete.delete()) {
                    sLogLine = sLogLine + "\nCould not delete source file.\n";
                }
                BroadcastProgress(true, sLogLine + "\n",
                        false, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                continue; //jump to next item in import list.
            }

            if(fileItem.sDestinationFolder.equals("")) {
                fileItem.sDestinationFolder = GlobalClass.gsUnsortedFolderName;
            }

            String sDestination = globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                    fileItem.sDestinationFolder;
            File fDestination = new File(sDestination);

            if( !alsVerifiedDestinationFolders.contains(sDestination)) {

                if (!fDestination.exists()) {
                    if (!fDestination.mkdir()) {
                        //Unable to create directory
                        BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                                false, iProgressBarValue,
                                true, "Operation halted.",
                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                        return;
                    } else {
                        alsVerifiedDestinationFolders.add(sDestination);
                        BroadcastProgress(true, "Destination folder created: " + fDestination.getPath() + "\n",
                                false, iProgressBarValue,
                                false, "",
                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    }
                } else {
                    alsVerifiedDestinationFolders.add(sDestination);
                    BroadcastProgress(true, "Destination folder verified: " + fDestination.getPath() + "\n",
                            true, iProgressBarValue,
                            false, "",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                }
            }
            Uri uriSourceFile;
            InputStream inputStream;
            OutputStream outputStream;

            uriSourceFile = Uri.parse(fileItem.sUri);
            DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
            boolean bProblemWithSourceFile = false;
            if (dfSource == null) {
                bProblemWithSourceFile = true;
            } else if (dfSource.getName() == null){
                bProblemWithSourceFile = true;
            }
            if(bProblemWithSourceFile){
                BroadcastProgress(true, "Problem with source reference for " + fileItem.sFileOrFolderName + "\n",
                        false, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                lProgressNumerator += fileItem.lSizeBytes;
                continue;
            }

            try {


                ItemClass_CatalogItem ciNew = new ItemClass_CatalogItem();
                ciNew.sItemID = globalClass.getNewCatalogRecordID(iMediaCategory);

                //Reverse the text on the file so that the file does not get picked off by a search tool:
                String sTempFileName = ciNew.sItemID + "_" + dfSource.getName(); //Create unique filename. Using ID will allow database error checking.
                String sFileName = GlobalClass.JumbleFileName(sTempFileName);
                File fDestinationFile = new File(fDestination.getPath() + File.separator + sFileName);

                if(bCopyViaWorker){

                    //Write next behavior to the screen log:
                    sLogLine = "Preparing data for job file " + fileItem.sFileOrFolderName + ".\n";
                    lProgressNumerator++;
                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, "File " + lProgressNumerator + "/" + lProgressDenominator,
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                    String sLine = dfSource.getUri() + "\t"
                            + fileItem.sDestinationFolder + "\t"
                            + fDestinationFile.getName() + "\t"
                            + fileItem.lSizeBytes + "\n";
                    sbJobFileRecords.append(sLine);


                } else {

                    //Write next behavior to the screen log:
                    sLogLine = GlobalClass.gsMoveOrCopy[iMoveOrCopy];
                    sLogLine = sLogLine + " file " + fileItem.sFileOrFolderName + " to destination...";
                    BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                    inputStream = contentResolver.openInputStream(dfSource.getUri());

                    outputStream = new FileOutputStream(fDestinationFile.getPath());
                    int iLoopCount = 0;
                    byte[] buffer = new byte[100000];
                    if (inputStream == null) continue;
                    while ((lLoopBytesRead = inputStream.read(buffer, 0, buffer.length)) >= 0) {
                        outputStream.write(buffer, 0, buffer.length);
                        lProgressNumerator += lLoopBytesRead;
                        iLoopCount++;
                        if (iLoopCount % 10 == 0) {
                            //Send update every 10 loops:
                            iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                            BroadcastProgress(false, "",
                                    true, iProgressBarValue,
                                    true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                        }
                    }
                    outputStream.flush();
                    outputStream.close();
                    //This file has now been copied.

                    sLogLine = "Success.\n";
                    if (iMoveOrCopy == GlobalClass.MOVE) {
                        if (!dfSource.delete()) {
                            sLogLine = "\nCould not delete source file after copy (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                        }
                    }

                    //Update the progress bar for the file move/copy:
                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                } //End else if copying via this routine (versus assigning to a worker).

                //Next add the data to the catalog file and memory:

                //Create a timestamp to be used to create the data record:
                Double dTimeStamp = GlobalClass.GetTimeStampFloat();

                ciNew.iMediaCategory = iMediaCategory;
                ciNew.sFilename = sFileName;
                ciNew.lSize = fileItem.lSizeBytes;
                ciNew.lDuration_Milliseconds = fileItem.lVideoTimeInMilliseconds;
                ciNew.sDuration_Text = fileItem.sVideoTimeText;
                if(!fileItem.sWidth.equals("") && !fileItem.sHeight.equals("")) {
                    ciNew.iWidth = Integer.parseInt(fileItem.sWidth);
                    ciNew.iHeight = Integer.parseInt(fileItem.sHeight);
                }
                ciNew.sFolder_Name = fileItem.sDestinationFolder;
                ciNew.sTags = GlobalClass.formDelimitedString(fileItem.aliProspectiveTags, ",");
                ciNew.dDatetime_Last_Viewed_by_User = dTimeStamp;
                ciNew.dDatetime_Import = dTimeStamp;
                ciNew.iGrade = fileItem.iGrade;

                //The below call should add the record to both the catalog contents file
                //  and memory:
                globalClass.CatalogDataFile_CreateNewRecord(ciNew);



            } catch (Exception e) {
                BroadcastProgress(true, "Problem with copy/move operation.\n" + e.getMessage(),
                        false, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            }


        } //End file processing loop.

        if(bCopyViaWorker){

            //Close the job file, written-to in the file loop above:
            String sJobDateTime = GlobalClass.GetTimeStampFileSafe();
            String sJobFileName = "Job_" + sJobDateTime + ".txt";

            try {

                //Inform user of preparation of worker:
                String sMoveOrCopy = GlobalClass.gsMoveOrCopy[iMoveOrCopy];
                sLogLine = "Using background worker for file " + sMoveOrCopy.toLowerCase() + " operations.\n"
                        + "Preparing job file.\n\n";
                lProgressDenominator = alFileList.size();
                BroadcastProgress(true, sLogLine,
                        false, iProgressBarValue,
                        true, "File " + lProgressNumerator + "/" + lProgressDenominator,
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                //Create a file with a listing of the files to be copied/moved:
                String sJobFilePath = globalClass.gfJobFilesFolder.getAbsolutePath() +
                        File.separator + sJobFileName;
                File fJobFile = new File(sJobFilePath);

                FileWriter fwJobFile = new FileWriter(fJobFile, true);
                //Write the data header:
                String sConfig = "MediaCategory:" + GlobalClass.gsCatalogFolderNames[iMediaCategory] + "\t"
                        + "MoveOrCopy:" + sMoveOrCopy + "\t"
                        + "TotalSize:" + lTotalImportSize + "\t"
                        + "FileCount:" + alFileList.size() + "\n";
                fwJobFile.write(sConfig);
                fwJobFile.write(sbJobFileRecords.toString());
                fwJobFile.flush();
                fwJobFile.close();



            } catch (Exception e){
                BroadcastProgress(true, "Problem with writing the job file.\n" + e.getMessage(),
                        false, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            }
            //Write next behavior to the screen log:
            sLogLine = "\nStarting worker to process job file.\n\n"
                    + "Files will appear in the catalog as the worker progresses.\n"
                    + "Refresh the catalog viewer (exit/re-enter, change sort direction) to view newly-added files.\n";
            BroadcastProgress(true, sLogLine,
                    false, iProgressBarValue,
                    true, lProgressNumerator + "/" + lProgressDenominator + " files written to job file",
                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

            //Build-out data to send to the worker:
            Data dataLocalFileTransfer = new Data.Builder()
                    .putString(Worker_LocalFileTransfer.KEY_ARG_JOB_REQUEST_DATETIME, sJobDateTime)
                    .putString(Worker_LocalFileTransfer.KEY_ARG_JOB_FILE, sJobFileName)
                    //.putInt(Worker_LocalFileTransfer.KEY_ARG_MEDIA_CATEGORY, iMediaCategory)
                    //.putInt(Worker_LocalFileTransfer.KEY_ARG_COPY_OR_MOVE, iMoveOrCopy)
                    //.putLong(Worker_LocalFileTransfer.KEY_ARG_TOTAL_IMPORT_SIZE_BYTES, lTotalImportSize)
                    .build();
            OneTimeWorkRequest otwrLocalFileTransfer = new OneTimeWorkRequest.Builder(Worker_LocalFileTransfer.class)
                    .setInputData(dataLocalFileTransfer)
                    .addTag(Worker_LocalFileTransfer.WORKER_LOCAL_FILE_TRANSFER_TAG) //To allow finding the worker later.
                    .build();
            UUID UUIDWorkID = otwrLocalFileTransfer.getId();
            WorkManager.getInstance(getApplicationContext()).enqueue(otwrLocalFileTransfer);

        }

        BroadcastProgress(true, "Operation complete.",
                false, iProgressBarValue,
                false, "",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

    }


    //====== Comic Routines ========================================================================

    private void handleAction_startActionImportNHComics(int iMoveOrCopy) {


        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;
        long lLoopBytesRead;


        ContentResolver contentResolver = getApplicationContext().getContentResolver();

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        ArrayList<ItemClass_File> alFileList = globalClass.galImportFileList;

        //Calculate total size of all files to import:
        for(ItemClass_File fi: alFileList){
            lTotalImportSize = lTotalImportSize + fi.lSizeBytes;
        }
        lProgressDenominator = lTotalImportSize;


        TreeMap<String, String[]> tmNHComicIDs = new TreeMap<>(); //Map NH_Comic_Downloader ComicID to a record ID/folder and also grab the comic title.

        int INDEX_RECORD_ID = 0;
        int INDEX_COMIC_NAME = 1;
        int INDEX_COMIC_TAGS = 2;
        int INDEX_COMIC_GRADE = 3;
        //If NH_Comic_Downloaded, loop and find all of the comic IDs:
        for(ItemClass_File fileItem: alFileList) {
            if(fileItem.sFileOrFolderName.matches(GlobalClass.gsNHComicCoverPageFilter)){
                String sComicID = GetNHComicID(fileItem.sFileOrFolderName);
                lProgressDenominator = lProgressDenominator - fileItem.lSizeBytes; //We don't copy over the cover page.
                String sRecordID = globalClass.getNewCatalogRecordID(GlobalClass.MEDIA_CATEGORY_COMICS);
                String sComicName = GetNHComicNameFromCoverFile(fileItem.sFileOrFolderName);
                String sComicTags = GlobalClass.formDelimitedString(fileItem.aliProspectiveTags, ",");
                int iGrade = fileItem.iGrade;
                if(tmNHComicIDs.containsKey(sComicID)){
                    //If this is merely a duplicate comic selected during the import, not if it already exists in the catalog.
                    //If it already exists in the catalog, it is on the user to resolve.
                    problemNotificationConfig("Skipping Comic ID " + sComicID + ". Duplicate comic.", Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                } else {
                    tmNHComicIDs.put(sComicID, new String[]{sRecordID, sComicName, sComicTags, String.valueOf(iGrade)});
                }
            }
        }

        for(Map.Entry<String, String[]> tmEntryNHComic: tmNHComicIDs.entrySet()) {
            //Loop and import files:

            String sDestinationFolder = tmEntryNHComic.getValue()[INDEX_RECORD_ID]; //The individual comic folder is the comic ID.

            File fDestination = new File(
                    globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath() + File.separator +
                            sDestinationFolder);

            if (!fDestination.exists()) {
                if (!fDestination.mkdir()) {
                    //Unable to create directory
                    BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            true, "Operation halted.",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    return;
                } else {
                    BroadcastProgress(true, "Destination folder created: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            false, "",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                }
            } else {
                BroadcastProgress(true, "Destination folder verified: " + fDestination.getPath() + "\n",
                        true, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            }

            //Prepare the data record:
            ItemClass_CatalogItem ciNewComic = new ItemClass_CatalogItem();
            ciNewComic.iMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
            ciNewComic.sItemID = tmEntryNHComic.getValue()[INDEX_RECORD_ID]; //The individual comic folder is the comic ID.
            ciNewComic.sTitle = tmEntryNHComic.getValue()[INDEX_COMIC_NAME]; //Get the name.
            ciNewComic.sTags = tmEntryNHComic.getValue()[INDEX_COMIC_TAGS]; //Get the tags.
            ciNewComic.iGrade = Integer.parseInt(tmEntryNHComic.getValue()[INDEX_COMIC_GRADE]); //Get the grade. Either default or selected by user.
            ciNewComic.sFolder_Name = sDestinationFolder;
            //Create a timestamp to be used to create the data record:
            Double dTimeStamp = GlobalClass.GetTimeStampFloat();
            ciNewComic.dDatetime_Last_Viewed_by_User = dTimeStamp;
            ciNewComic.dDatetime_Import = dTimeStamp;
            ciNewComic.sSource = "https:/nhentai.net/g/" + tmEntryNHComic.getKey() + "/";

            ItemClass_File icfCoverPageFile = null;

            for (ItemClass_File fileItem : alFileList) {

                if(fileItem.sFileOrFolderName.matches("^" + tmEntryNHComic.getKey() + "_Cover.+")) {
                    //Preserve the cover page file item so that it can be deleted at the after the
                    // other files are imported. No particular reason for saving this until the end.
                    icfCoverPageFile = fileItem;

                }

                if(fileItem.sFileOrFolderName.matches("^" + tmEntryNHComic.getKey() + "_Page.+")) {

                    //Make sure that it is not a duplicate page.
                    if (!isPageDuplicate(fileItem.sFileOrFolderName)) {
                        ciNewComic.iFile_Count++;
                        String sNewFilename = fileItem.sFileOrFolderName;
                        sNewFilename = sNewFilename.substring(sNewFilename.indexOf("_")); //Get rid of the NH comic ID.
                        sNewFilename = ciNewComic.sItemID + sNewFilename; //Add on the sequenced comic record ID.
                        sNewFilename = GlobalClass.JumbleFileName(sNewFilename);
                        if (fileItem.sFileOrFolderName.contains("_Page_001")) {
                            //Set the Thumbnail file to the first page (which is a duplicate of the
                            // cover page but of a different name):
                            ciNewComic.sFilename = sNewFilename;
                            ciNewComic.sThumbnail_File = sNewFilename;
                        }
                        ciNewComic.lSize += fileItem.lSizeBytes;

                        Uri uriSourceFile;
                        String sLogLine;
                        InputStream inputStream;
                        OutputStream outputStream;

                        uriSourceFile = Uri.parse(fileItem.sUri);
                        DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
                        try {
                            //Write next behavior to the screen log:
                            sLogLine = "Attempting " + GlobalClass.gsMoveOrCopy[iMoveOrCopy].toLowerCase();
                            sLogLine = sLogLine + " of file " + fileItem.sFileOrFolderName + " to destination...";
                            BroadcastProgress(true, sLogLine,
                                    false, iProgressBarValue,
                                    true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                            if (dfSource == null) {
                                BroadcastProgress(true, "Problem with copy/move operation of file " + fileItem.sFileOrFolderName,
                                        false, iProgressBarValue,
                                        false, "",
                                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                                lProgressNumerator += fileItem.lSizeBytes;
                                continue;
                            }
                            inputStream = contentResolver.openInputStream(dfSource.getUri());

                            //Reverse the text on the file so that the file does not get picked off by a search tool:
                            if (dfSource.getName() == null) continue;

                            outputStream = new FileOutputStream(fDestination.getPath() + File.separator + sNewFilename);
                            int iLoopCount = 0;
                            byte[] buffer = new byte[100000];
                            if (inputStream == null) continue;
                            while ((lLoopBytesRead = inputStream.read(buffer, 0, buffer.length)) >= 0) {
                                outputStream.write(buffer, 0, buffer.length);
                                lProgressNumerator += lLoopBytesRead;
                                iLoopCount++;
                                if (iLoopCount % 10 == 0) {
                                    //Send update every 10 loops:
                                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                                    BroadcastProgress(false, "",
                                            true, iProgressBarValue,
                                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                                }
                            }
                            outputStream.flush();
                            outputStream.close();
                            sLogLine = "Copy success.\n";
                            iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                            BroadcastProgress(true, sLogLine,
                                    false, iProgressBarValue,
                                    true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                            //This file has now been copied.

                            //Delete the source file if 'Move' specified:
                            boolean bUpdateLogOneMoreTime = false;
                            if (iMoveOrCopy == GlobalClass.MOVE) {
                                bUpdateLogOneMoreTime = true;
                                if (!dfSource.delete()) {
                                    sLogLine = "Could not delete source file after copy (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                                } else {
                                    sLogLine = "Success deleting source file after copy.\n";
                                }
                            }

                            iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);

                            BroadcastProgress(bUpdateLogOneMoreTime, sLogLine,
                                    true, iProgressBarValue,
                                    true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);


                        } catch (Exception e) {
                            BroadcastProgress(true, "Problem with copy/move operation.\n\n" + e.getMessage(),
                                    false, iProgressBarValue,
                                    false, "",
                                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                        }

                    } else {
                        //NHComic page is a duplicate.
                        problemNotificationConfig("File " + fileItem.sFileOrFolderName + " appears to be a duplicate. Skipping import of this file.\n", Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    }

                } //End if match with Page regex containing ComicID.

            } //End NHComic Import Loop.

            //Next add the data to the catalog file and memory:
            //The below call should add the record to both the catalog contents file
            //  and memory:
            globalClass.CatalogDataFile_CreateNewRecord(ciNewComic);

            //Delete the cover page from source folder if required:
            if(icfCoverPageFile != null) {
                if (iMoveOrCopy == GlobalClass.MOVE) {
                    Uri uriSourceFile;
                    String sLogLine;

                    uriSourceFile = Uri.parse(icfCoverPageFile.sUri);
                    DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
                    if(dfSource != null) {
                        if (!dfSource.delete()) {
                            sLogLine = "Could not delete cover page duplicate file (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                        } else {
                            sLogLine = "Success deleting cover page duplicate file.\n";
                        }
                    } else {
                        sLogLine = "Could not delete cover page duplicate file (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                    }
                    BroadcastProgress(true, sLogLine,
                            true, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                }
            }






        } //End NHComics (plural) Import Loop.





        //Modify viewer settings to show the newly-imported files:
        /*globalClass.giCatalogViewerSortBySetting[GlobalClass.MEDIA_CATEGORY_COMICS] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        globalClass.gbCatalogViewerSortAscending[GlobalClass.MEDIA_CATEGORY_COMICS] = false;*/ //Handled in onHandleIntent.

        BroadcastProgress(true, "Operation complete.\n",
                true, iProgressBarValue,
                true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

    }

    private void handleAction_startActionImportComicFolders(int iMoveOrCopy) {

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;
        long lLoopBytesRead;


        ContentResolver contentResolver = getApplicationContext().getContentResolver();

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        ArrayList<ItemClass_File> alFileList = globalClass.galImportFileList;

        //Calculate total size of all files to import:
        for(ItemClass_File fi: alFileList){
            lTotalImportSize = lTotalImportSize + fi.lSizeBytes;
        }
        lProgressDenominator = lTotalImportSize;

        TreeMap<String, String[]> tmComics = new TreeMap<>(); //Map Comic to a record ID/folder and also grab the comic title.

        //If comic folder import, loop and find all of the comic parent Uris:
        int INDEX_RECORD_ID = 0;
        int INDEX_COMIC_NAME = 1;
        int INDEX_COMIC_TAGS = 2;
        int INDEX_COMIC_GRADE = 3;
        int INDEX_COMIC_SOURCE = 4;
        int INDEX_COMIC_PARODY = 5;
        int INDEX_COMIC_ARTIST = 6;
        for(ItemClass_File fileItem: alFileList) {
            if(fileItem.iTypeFileFolderURL == ItemClass_File.TYPE_FOLDER){
                String sUriParent = fileItem.sUri;
                String sRecordID = globalClass.getNewCatalogRecordID(GlobalClass.MEDIA_CATEGORY_COMICS);
                String sComicName = fileItem.sFileOrFolderName;
                if(!fileItem.sTitle.equals("")){
                    sComicName = fileItem.sTitle;
                }
                String sSource = "Folder Import";
                if(!fileItem.sURL.equals("")){
                    sSource = fileItem.sURL;
                }
                String sParody = "";
                if(!fileItem.sParody.equals("")){
                    sParody = fileItem.sParody;
                }
                String sArtist = "";
                if(!fileItem.sArtist.equals("")){
                    sArtist = fileItem.sArtist;
                }
                String sComicTags = GlobalClass.formDelimitedString(fileItem.aliProspectiveTags, ",");
                int iGrade = fileItem.iGrade;
                tmComics.put(sUriParent, new String[]{
                        sRecordID,
                        sComicName,
                        sComicTags,
                        String.valueOf(iGrade),
                        sSource,
                        sParody,
                        sArtist});
            }
        }

        for(Map.Entry<String, String[]> tmEntryComic: tmComics.entrySet()) {
            //Create a folder and import files for this comic:

            String sDestinationFolder = tmEntryComic.getValue()[INDEX_RECORD_ID]; //The individual destination comic folder name is the comic ID.

            File fDestination = new File(
                    globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath() + File.separator +
                            sDestinationFolder);

            if (!fDestination.exists()) {
                if (!fDestination.mkdir()) {
                    //Unable to create directory
                    BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            true, "Operation halted.",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    return;
                } else {
                    BroadcastProgress(true, "Destination folder created: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            false, "",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                }
            } else {
                BroadcastProgress(true, "Destination folder verified: " + fDestination.getPath() + "\n",
                        true, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            }

            //Prepare the data record:
            ItemClass_CatalogItem ciNewComic = new ItemClass_CatalogItem();
            ciNewComic.iMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
            ciNewComic.sItemID = tmEntryComic.getValue()[INDEX_RECORD_ID]; //The individual comic folder is the comic ID.
            ciNewComic.sTitle = tmEntryComic.getValue()[INDEX_COMIC_NAME]; //Get the name.
            ciNewComic.sTags = tmEntryComic.getValue()[INDEX_COMIC_TAGS]; //Get the tags.
            ciNewComic.iGrade = Integer.parseInt(tmEntryComic.getValue()[INDEX_COMIC_GRADE]); //Get the grade.
            ciNewComic.sFolder_Name = sDestinationFolder;
            //Create a timestamp to be used to create the data record:
            Double dTimeStamp = GlobalClass.GetTimeStampFloat();
            ciNewComic.dDatetime_Last_Viewed_by_User = dTimeStamp;
            ciNewComic.dDatetime_Import = dTimeStamp;
            ciNewComic.sSource = tmEntryComic.getValue()[INDEX_COMIC_SOURCE];
            ciNewComic.sComicParodies = tmEntryComic.getValue()[INDEX_COMIC_PARODY];
            ciNewComic.sComicArtists = tmEntryComic.getValue()[INDEX_COMIC_ARTIST];

            //Find comic files belonging to this comic and put them in a tree map for sorting.
            TreeMap<String, ItemClass_File> tmComicFiles = new TreeMap<>();
            for (ItemClass_File fileItem : alFileList) {
                if (fileItem.sUriParent.matches(tmEntryComic.getKey())) {
                    tmComicFiles.put(fileItem.sFileOrFolderName, fileItem);
                }
            }


            for (Map.Entry<String, ItemClass_File> entryComicFile : tmComicFiles.entrySet()) {
                ItemClass_File fileItem = entryComicFile.getValue();

                String sLogLine;
                Uri uriSourceFile = Uri.parse(fileItem.sUri);
                DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);

                if(fileItem.sFileOrFolderName.equals(STRING_COMIC_XML_FILENAME)){
                    if (iMoveOrCopy == GlobalClass.MOVE) {
                        if(dfSource != null) {
                            if (!dfSource.delete()) {
                                sLogLine = "Could not delete xml file from source folder.\n";
                            } else {
                                sLogLine = "Success deleting xml file from source folder.\n";
                            }
                        } else {
                            sLogLine = "Could not delete xml file from source folder.\n";
                        }
                        BroadcastProgress(true, sLogLine,
                                false, 0,
                                false, "",
                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    }
                    continue; //If this is the xml file, take no action to copy it into our catalog.
                }

                ciNewComic.iFile_Count++;
                ciNewComic.iComicPages++;
                ciNewComic.iComic_Max_Page_ID++;

                String sNewFilename = fileItem.sFileOrFolderName;
                sNewFilename = GlobalClass.JumbleFileName(sNewFilename);
                if (ciNewComic.sFilename.equals("")) {
                    //Set the Thumbnail file to the first page:
                    ciNewComic.sFilename = sNewFilename;
                    ciNewComic.sThumbnail_File = sNewFilename;
                }
                ciNewComic.lSize += fileItem.lSizeBytes;

                InputStream inputStream;
                OutputStream outputStream;

                try {
                    //Write next behavior to the screen log:
                    sLogLine = "Attempting " + GlobalClass.gsMoveOrCopy[iMoveOrCopy].toLowerCase();
                    sLogLine = sLogLine + " of file " + fileItem.sFileOrFolderName + " to destination...";
                    BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                    if (dfSource == null) {
                        BroadcastProgress(true, "Problem with copy/move operation of file " + fileItem.sFileOrFolderName,
                                false, iProgressBarValue,
                                false, "",
                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                        lProgressNumerator += fileItem.lSizeBytes;
                        continue;
                    }
                    inputStream = contentResolver.openInputStream(dfSource.getUri());

                    //Reverse the text on the file so that the file does not get picked off by a search tool:
                    if (dfSource.getName() == null) continue;

                    outputStream = new FileOutputStream(fDestination.getPath() + File.separator + sNewFilename);
                    int iLoopCount = 0;
                    byte[] buffer = new byte[100000];
                    if (inputStream == null) continue;
                    while ((lLoopBytesRead = inputStream.read(buffer, 0, buffer.length)) >= 0) {
                        outputStream.write(buffer, 0, buffer.length);
                        lProgressNumerator += lLoopBytesRead;
                        iLoopCount++;
                        if (iLoopCount % 10 == 0) {
                            //Send update every 10 loops:
                            iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                            BroadcastProgress(false, "",
                                    true, iProgressBarValue,
                                    true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                        }
                    }
                    outputStream.flush();
                    outputStream.close();
                    sLogLine = "Copy success.\n";
                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                    //This file has now been copied.

                    //Delete the source file if 'Move' specified:
                    boolean bUpdateLogOneMoreTime = false;
                    if (iMoveOrCopy == GlobalClass.MOVE) {
                        bUpdateLogOneMoreTime = true;
                        if (!dfSource.delete()) {
                            sLogLine = "Could not delete source file after copy (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                        } else {
                            sLogLine = "Success deleting source file after copy.\n";
                        }
                    }

                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);

                    BroadcastProgress(bUpdateLogOneMoreTime, sLogLine,
                            true, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);


                } catch (Exception e) {
                    BroadcastProgress(true, "Problem with copy/move operation.\n\n" + e.getMessage(),
                            false, iProgressBarValue,
                            false, "",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                }


            } //End comic page import Loop.

            //Next add the data to the catalog file and memory:
            //The below call should add the record to both the catalog contents file
            //  and memory:
            globalClass.CatalogDataFile_CreateNewRecord(ciNewComic);

            //Delete the comic folder from source directory if required:
            if (iMoveOrCopy == GlobalClass.MOVE) {
                Uri uriComicSourceFolder; //This is not the folder that the user selected, but rather
                    //a comic folder within that folder.
                String sLogLine;
                uriComicSourceFolder = Uri.parse(tmEntryComic.getKey());
                DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriComicSourceFolder);
                sLogLine = "Could not delete folder for comic '" + tmEntryComic.getValue()[INDEX_COMIC_NAME] + "'.\n";
                if(dfSource != null) {
                    if (dfSource.delete()) {
                        sLogLine = "Success deleting folder for comic '" + tmEntryComic.getValue()[INDEX_COMIC_NAME] + "'.\n";
                    }
                }
                BroadcastProgress(true, sLogLine,
                        true, iProgressBarValue,
                        true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            }
            
        } //End NHComics (plural) Import Loop.
        
        //Modify viewer settings to show the newly-imported files:
        /*globalClass.giCatalogViewerSortBySetting[GlobalClass.MEDIA_CATEGORY_COMICS] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        globalClass.gbCatalogViewerSortAscending[GlobalClass.MEDIA_CATEGORY_COMICS] = false;*/ //Handled in onHandleIntent.

        BroadcastProgress(true, "Operation complete.\n",
                true, iProgressBarValue,
                true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

    }

    @Nullable
    private ArrayList<String[]> handleAction_startActionGetComicDetailsOnline(String sAddress, String sIntentActionFilter){
        //Optionally returns an arraylist of the comic image URLs. This is because sometimes the DownloadManager fails
        //  to download the file, and it has to be redone.

        //Broadcast a message to be picked-up by the caller:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(sIntentActionFilter);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        ItemClass_CatalogItem ci = new ItemClass_CatalogItem();
        ci.iMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        ci.sSource = sAddress;

        //We don't grab the title from one of the html data blocks on nHentai.net.
        final String[] gsDataBlockIDs = new String[]{
                "Parodies:",
                "Characters:",
                "Tags:",
                "Artists:",
                "Groups:",
                "Languages:",
                "Categories:",
                "Pages:",
                "Uploaded:"}; //We ignore the upload date data, but still include it.

        int j = gsDataBlockIDs.length + 1;
        String[] sReturnData = new String[j];
        //First array element is for comic title.
        //Elements 1-8 are data block results.
        //Last array element is for error message.
        for(int i = 0; i < j; i++){
            sReturnData[i] = "";
        }

        String sComicTitle = "";

        final int COMIC_DETAILS_TITLE_INDEX = 0;
        final int COMIC_DETAILS_PARODIES_DATA_INDEX = 1;
        final int COMIC_DETAILS_CHARACTERS_DATA_INDEX = 2;
        final int COMIC_DETAILS_TAGS_DATA_INDEX = 3;
        final int COMIC_DETAILS_ARTISTS_DATA_INDEX = 4;
        final int COMIC_DETAILS_GROUPS_DATA_INDEX = 5;
        final int COMIC_DETAILS_LANGUAGES_DATA_INDEX = 6;
        final int COMIC_DETAILS_CATEGORIES_DATA_INDEX = 7;
        final int COMIC_DETAILS_PAGES_DATA_INDEX = 8;

        try {
            //Get the data from the WebPage:
            BroadcastProgress_ComicDetails("Getting data from " + ci.sSource + "\n", sIntentActionFilter);
            URL url = new URL(ci.sSource);
            URLConnection conn = url.openConnection();
            conn.setDoInput(true);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder a = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                a.append(inputLine);
            }
            in.close();
            BroadcastProgress_ComicDetails("\nData acquired. Begin data processing...\n", sIntentActionFilter);

            String sHTML = a.toString();
            sHTML = sHTML.replaceAll("tag-container field-name ", "tag-container field-name");

            //Note: DocumentBuilderFactory.newInstance().newDocumentBuilder().parse....
            //  does not work well to parse this html. Modern html interpreters accommodate
            //  certain "liberties" in the code. That parse routine is meant for tight XML.
            //  HtmlCleaner does a good job processing the html in a manner similar to modern
            //  browsers.
            //Clean up the HTML:
            BroadcastProgress_ComicDetails("Cleaning up html.\n", sIntentActionFilter);
            HtmlCleaner pageParser = new HtmlCleaner();
            CleanerProperties props = pageParser.getProperties();
            props.setAllowHtmlInsideAttributes(true);
            props.setAllowMultiWordAttributes(true);
            props.setRecognizeUnicodeChars(true);
            props.setOmitComments(true);
            TagNode node = pageParser.clean(sHTML);


            //Attempt to get the comic title from the WebPage html:
            BroadcastProgress_ComicDetails("Looking for comic title.\n", sIntentActionFilter);
            String sxPathExpression;
            sxPathExpression = globalClass.snHentai_Comic_Title_xPathExpression;
            //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
            Object[] objsTagNodeTitle = node.evaluateXPath(sxPathExpression);
            //Check to see if we found anything:
            if (objsTagNodeTitle != null && objsTagNodeTitle.length > 0) {
                //If we found something, assign it to a string:
                sComicTitle = ((TagNode) objsTagNodeTitle[0]).getText().toString();
            }

            sReturnData[COMIC_DETAILS_TITLE_INDEX] = sComicTitle;

            //Attempt to determine the inclusion of "parodies", "characters", "tags", etc
            //  in the info blocks:
            BroadcastProgress_ComicDetails("Looking for comic data info blocks (parodies, characters, tags, etc).\n", sIntentActionFilter);
            sxPathExpression = globalClass.snHentai_Comic_Data_Blocks_xPE;
            //Use an xPathExpression (similar to RegEx) to look for the data in the html/xml:
            //TCFN = 'tag-container field-name' html class used by nHentai web pages.
            Object[] objsTagNodesTCFNs = node.evaluateXPath(sxPathExpression);
            String sData = "";
            //Check to see if we found anything:
            if (objsTagNodesTCFNs != null && objsTagNodesTCFNs.length > 0) {
                //If we found something, assign it to a string:
                sData = ((TagNode) objsTagNodesTCFNs[0]).getText().toString();
            }

            //Replace spacing with tabs and reduce the tab count.
            sData = sData.replaceAll(" {2}","\t");
            sData = sData.replaceAll("\t\t","\t");
            sData = sData.replaceAll("\t\t","\t");
            sData = sData.replaceAll("\t\t","\t");
            sData = sData.replaceAll("^\t",""); //Get rid of any leading tab character.

            String[] sDataBreakout = sData.split("\t");


            //Process each named data block. Data blocks are parodies, characters, tags, etc.
            for(int i = 0; i < gsDataBlockIDs.length - 1; i++) {
                //gsDataBlockIDs.length - 1 ====> We are ignoring the last data block, "Uploaded:", the upload date.
                int iterator = -1; //Determine where in the sequence of objects the current data block will appear.
                for (int k = 0; k < sDataBreakout.length - 1; k++) {
                    //Find the DataBreakout index (k) that contains the DataBlock identifier (not the data):
                    if (sDataBreakout[k].contains(gsDataBlockIDs[i])) {

                        if (sDataBreakout[k + 1].contains(gsDataBlockIDs[i + 1])) {
                            //If we are here, then it means that there was no data between the current
                            //  data block and the next data block. Skip gathering the data for this
                            //  data block.
                            continue;
                        } else {
                            iterator = k + 1;
                        }

                        break;
                    }
                }
                if (iterator > 0) {
                    sData = sDataBreakout[iterator];
                    if (!sDataBreakout[iterator - 1].contains("Pages:")) { //Don't clean-out numbers if we are expecting numbers.
                        //Get rid of "tag count" data. This is data unique to nHentai that
                        //  shows the number of times that the tag has been applied.
                        sData = sData.replaceAll("\\d{4}K", "\t");
                        sData = sData.replaceAll("\\d{3}K", "\t");
                        sData = sData.replaceAll("\\d{2}K", "\t");
                        sData = sData.replaceAll("\\dK", "\t");
                        sData = sData.replaceAll("\\d{4}", "\t");
                        sData = sData.replaceAll("\\d{3}", "\t");
                        sData = sData.replaceAll("\\d{2}", "\t");
                        sData = sData.replaceAll("\\d", "\t");
                    }
                    //Reformat the data:
                    String[] sItems = sData.split("\t");
                    StringBuilder sbData = new StringBuilder();
                    sbData.append(sItems[0]);
                    for (int m = 1; m < sItems.length; m++) {
                        sbData.append(", ");
                        sbData.append(sItems[m]);
                    }
                    sReturnData[i + 1] = sbData.toString();
                }
            }

            ci.sTitle = sReturnData[COMIC_DETAILS_TITLE_INDEX];
            ci.sComicParodies = sReturnData[COMIC_DETAILS_PARODIES_DATA_INDEX];
            ci.sComicCharacters = sReturnData[COMIC_DETAILS_CHARACTERS_DATA_INDEX];
            ci.sTags = sReturnData[COMIC_DETAILS_TAGS_DATA_INDEX]; //NOTE: THESE ARE TEXTUAL TAGS, NOT TAG IDS.
            ci.sComicArtists = sReturnData[COMIC_DETAILS_ARTISTS_DATA_INDEX];
            ci.sComicGroups = sReturnData[COMIC_DETAILS_GROUPS_DATA_INDEX];
            ci.sComicLanguages = sReturnData[COMIC_DETAILS_LANGUAGES_DATA_INDEX];
            ci.sComicCategories = sReturnData[COMIC_DETAILS_CATEGORIES_DATA_INDEX];
            if(!sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX].equals("")) {
                ci.iComicPages = Integer.parseInt(sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX]);
            }

            //Get the first thumbnail image for import preview:
            BroadcastProgress_ComicDetails("Looking for cover page thumbnail.\n", sIntentActionFilter);
            sxPathExpression = globalClass.snHentai_Comic_Cover_Thumb_xPE;
            //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
            Object[] objsTagNodeThumbnails = node.evaluateXPath(sxPathExpression);
            //Check to see if we found anything:
            String sThumbnailImageAddress;
            if (objsTagNodeThumbnails != null && objsTagNodeThumbnails.length > 0) {
                sThumbnailImageAddress = ((TagNode) objsTagNodeThumbnails[0]).getAttributeByName("data-src");
                if (sThumbnailImageAddress.length() > 0) {
                    ci.sComicThumbnailURL = sThumbnailImageAddress;
                }
            }

            //Decypher the rest of the comic page image URLs to be used in a later step of the import:
            BroadcastProgress_ComicDetails("Looking for listing of comic pages.\n", sIntentActionFilter);
            sxPathExpression = globalClass.snHentai_Comic_Page_Thumbs_xPE;
            objsTagNodeThumbnails = node.evaluateXPath(sxPathExpression);
            //Check to see if we found anything:
            String sImageAddressTemplate;
            String sGalleryID = "";
            TreeMap<Integer, String> tmFileIndexImageExtention = new TreeMap<>();
            if (objsTagNodeThumbnails != null && objsTagNodeThumbnails.length > 0) {
                //Get the gallery ID. This is not the same as the NH comic ID.
                // Example: "https://t.nhentai.net/galleries/645538/1t.png"
                sImageAddressTemplate = ((TagNode) objsTagNodeThumbnails[0]).getAttributeByName("data-src");
                if (sImageAddressTemplate.length() > 0) {
                    sGalleryID = sImageAddressTemplate.substring(0, sImageAddressTemplate.lastIndexOf("/"));
                    sGalleryID = sGalleryID.substring(sGalleryID.lastIndexOf("/") + 1);
                }
                //Get the thumbnail image names, which will reveal the file extension of the full images:
                for (Object objsTagNodeThumbnail : objsTagNodeThumbnails) {
                    String sImageAddress = ((TagNode) objsTagNodeThumbnail).getAttributeByName("data-src");
                    BroadcastProgress_ComicDetails(sImageAddress + "\n", sIntentActionFilter); //Broadcast progress
                    String sImageFilename = sImageAddress.substring(sImageAddress.lastIndexOf("/") + 1);
                    sImageFilename = sImageFilename.replace("t", ""); //Get rid of the 't', presummably for "thumbnail".
                    String[] sSplit = sImageFilename.split("\\.");
                    if (sSplit.length == 2) {
                        try {
                            Integer iPageNumber = Integer.parseInt(sSplit[0]);
                            tmFileIndexImageExtention.put(iPageNumber, sSplit[1]);
                        } catch (Exception ignored) {
                        }
                    }
                }

            }
            ArrayList<String[]> alsImageNameData = new ArrayList<>();
            int iFileSizeLoopCount = 0;
            boolean bGetOnlineSize = true;
            long lProjectedComicSize;

            if (sGalleryID.length() > 0) {
                for(Map.Entry<Integer, String> tmEntryPageNumImageExt: tmFileIndexImageExtention.entrySet()) {
                    //Build the suspected URL for the image:
                    String sNHImageDownloadAddress = "https://i.nhentai.net/galleries/" +
                            sGalleryID + "/" +
                            tmEntryPageNumImageExt.getKey() + "." +
                            tmEntryPageNumImageExt.getValue();
                    //Build a filename to save the file to in the catalog:
                    String sPageStringForFilename = String.format(Locale.getDefault(),"%04d", tmEntryPageNumImageExt.getKey());
                    String sNewFilename = "Page_" + sPageStringForFilename + "." + tmEntryPageNumImageExt.getValue();
                    String[] sTemp = {sNHImageDownloadAddress, sNewFilename};
                    alsImageNameData.add(sTemp);

                    //Get the size of the image and add it to the total size of the comic:
                    if(bGetOnlineSize) {
                        URL urlPage = new URL(sNHImageDownloadAddress);
                        BroadcastProgress_ComicDetails("Getting file size data for " + sNHImageDownloadAddress + "\n", sIntentActionFilter); //Broadcast progress
                        //URLConnection connection = urlPage.openConnection();
                        HttpURLConnection connection = (HttpURLConnection) urlPage.openConnection();
                        connection.setRequestProperty("Accept-Encoding", "identity");
                        ci.lSize += connection.getContentLength(); //Returns -1 if content size is not in the header.
                        if(ci.lSize == -1){
                            bGetOnlineSize = false;
                        }
                        iFileSizeLoopCount++;
                        if(iFileSizeLoopCount == 5){  //Use a sample set of images to project the size of the comic.
                            lProjectedComicSize = ci.lSize / iFileSizeLoopCount;
                            lProjectedComicSize *= ci.iComicPages;
                            ci.lSize = lProjectedComicSize;
                            BroadcastProgress_ComicDetails("Projecting size of comic to " + ci.iComicPages + " pages... " + ci.lSize + " bytes." + "\n", sIntentActionFilter);
                            bGetOnlineSize = false;
                        }
                        connection.disconnect();
                    }
                }
            }
            if(alsImageNameData.size() > 0){
                //If there are image addresses to attempt to download...
                ci.alsDownloadURLsAndDestFileNames = alsImageNameData;
            }
            BroadcastProgress_ComicDetails("Finished analyzing web data.\n", sIntentActionFilter);



        } catch(Exception e){
            String sMsg = e.getMessage();
            BroadcastProgress_ComicDetails("Problem collecting comic data from address. " + sMsg + "\n", sIntentActionFilter);
            broadcastIntent.putExtra(EXTRA_BOOL_PROBLEM, true);
            broadcastIntent.putExtra(EXTRA_STRING_PROBLEM, sMsg);
            sendBroadcast(broadcastIntent);

            return null;
        }


        ci.bComic_Online_Data_Acquired = true;

        if(ci.iComicPages > 0) {
            //Put potential catalog item in globalclass to handle reset of activity if user leaves
            //  activity or rotates the screen:
            globalClass.gci_ImportComicWebItem = ci;

            //Broadcast a message to be picked up by the Import fragment to refresh the views:
            broadcastIntent.putExtra(COMIC_DETAILS_SUCCESS, true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        } else {
            BroadcastProgress_ComicDetails("No comic pages found.\n", sIntentActionFilter);
        }

        return ci.alsDownloadURLsAndDestFileNames;
    }

    private void handleAction_startActionImportComicWebFiles(ItemClass_CatalogItem ci, String sIntentActionFilter) throws IOException {
        //sIntentActionFilter is used to send out the broadcast responses.

        long lProgressNumerator = 0L;
        long lProgressDenominator = ci.iComicPages;
        int iProgressBarValue = 0;

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        boolean bUpdateExistingComic = false;
        //Create the comic folder.
        if(!ci.sItemID.equals("")) { //There is a case in which this routine is called to re-download comic files. In that case, don't recreate the item ID.
            bUpdateExistingComic = true;
            //If we are updating an existing comic, get the download file list:
            ArrayList<String[]> alsURLs;
            alsURLs = handleAction_startActionGetComicDetailsOnline(ci.sSource, sIntentActionFilter);
            ci.alsDownloadURLsAndDestFileNames = alsURLs;
        }

        //Create the comic folder.
        if(!bUpdateExistingComic) {
            ci.sItemID = globalClass.getNewCatalogRecordID(GlobalClass.MEDIA_CATEGORY_COMICS);
            ci.sFolder_Name = ci.sItemID;
        }

        File fDestination = new File(
                globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath() + File.separator +
                        ci.sFolder_Name);


        if (!fDestination.exists()) {
            if (!fDestination.mkdir()) {
                //Unable to create directory
                BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        sIntentActionFilter);
                return;
            } else {
                BroadcastProgress(true, "Destination folder created: " + fDestination.getPath() + "\n",
                        false, iProgressBarValue,
                        false, "",
                        sIntentActionFilter);
            }
        } else {
            BroadcastProgress(true, "Destination folder verified: " + fDestination.getPath() + "\n",
                    true, iProgressBarValue,
                    false, "",
                    sIntentActionFilter);
        }

        //Create a timestamp to be used to create the data record:
        Double dTimeStamp = GlobalClass.GetTimeStampFloat();
        ci.dDatetime_Last_Viewed_by_User = dTimeStamp;
        ci.dDatetime_Import = dTimeStamp;

        if(!bUpdateExistingComic) { //If this is an update, don't update the tags and don't create a new catalog record.
            //Convert textual tags to numeric tags:
            //Form the tag integer array:
            String[] sTags = ci.sTags.split(", ");
            ArrayList<Integer> aliTags = new ArrayList<>();
            for (String sTag : sTags) {
                aliTags.add(globalClass.getTagIDFromText(sTag, GlobalClass.MEDIA_CATEGORY_COMICS));
            }
            //Look for any tags that could not be found:
            ArrayList<String> alsNewTags = new ArrayList<>();
            for(int i = 0; i < aliTags.size(); i++){
                if(aliTags.get(i) == -1){
                    //Prepare a list of strings representing the new tags that must be created:
                    if(!sTags[i].equals("")) {
                        alsNewTags.add(sTags[i]);
                    }
                }
            }
            if(alsNewTags.size() > 0) {
                //Create the missing tags:
                ArrayList<ItemClass_Tag> alictNewTags = globalClass.TagDataFile_CreateNewRecords(alsNewTags, GlobalClass.MEDIA_CATEGORY_COMICS);
                //Insert the new tag IDs into aliTags:
                int k = 0;
                for (int i = 0; i < aliTags.size(); i++) {
                    if (aliTags.get(i) == -1) {
                        if (!sTags[i].equals("")) {
                            if(k < alictNewTags.size()) {
                                aliTags.set(i, alictNewTags.get(k).iTagID);
                                k++;
                            }
                        }
                    }
                }
            }

            ci.sTags = GlobalClass.formDelimitedString(aliTags, ",");

            //The below call should add the record to both the catalog contents file
            //  and memory. Create the record in the system before downloading the files for the event that
            //  the download is interrupted:
            globalClass.CatalogDataFile_CreateNewRecord(ci);
        }

        if(ci.alsDownloadURLsAndDestFileNames.size() > 0){
            //If there are image addresses to attempt to download...

            //NOTE: Android has DownloadIdleService which is reponsible for cleaning up stale or orphan downloads.
            //  I have witnessed disappearance of downloaded files. This service seems to be deleting comic files.
            //See article at: https://www.vvse.com/blog/blog/2020/01/06/android-10-automatically-deletes-downloaded-files/

            InputStream input = null;
            OutputStream output = null;
            try {

                String sComicDownloadFolder = "";
                ArrayList<Long> allDownloadIDs = new ArrayList<>();
                DownloadManager downloadManager = null;
                if(globalClass.gbUseDownloadManager){
                    downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                }
                //Download the files:
                int FILE_DOWNLOAD_ADDRESS = 0;
                int FILE_NAME_AND_EXTENSION = 1;
                for(String[] sURLAndFileName: ci.alsDownloadURLsAndDestFileNames) {

                    String sNewFilename = ci.sItemID + "_" + sURLAndFileName[FILE_NAME_AND_EXTENSION];
                    //ci.iPostProcessingCode = ItemClass_CatalogItem.POST_PROCESSING_COMIC_DLM_MOVE; //Tell the app to move the files after download to avoid DLM-automated deletion.
                    //Above item no longer required as DownloadManager will not download to the final location on the SD Card, only to the emulated folder. However,
                    //  leave this item here in the even that testing without an SD Card reveals that we want to use this again.
                    String sJumbledNewFileName = GlobalClass.JumbleFileName(sNewFilename);

                    if(ci.sFilename.equals("")){
                        ci.sFilename = sJumbledNewFileName;
                        ci.sThumbnail_File = sJumbledNewFileName;
                        //Update the catalog record with the filename and thumbnail image:
                        globalClass.CatalogDataFile_UpdateRecord(ci);
                    }


                    if(!globalClass.gbUseDownloadManager) {
                        String sNewFullPathFilename = fDestination.getPath() +
                                File.separator + GlobalClass.gsDLTempFolderName + File.separator + //Use DL folder name to allow move to a different folder after download.
                                sJumbledNewFileName;
                        File fNewFile = new File(sNewFullPathFilename);

                        if(!fNewFile.exists()) {

                            // Output stream
                            output = new FileOutputStream(fNewFile.getPath());

                            byte[] data = new byte[1024];

                            BroadcastProgress(true, "Downloading: " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS] + "...",
                                    false, iProgressBarValue,
                                    true, "Downloading files...",
                                    sIntentActionFilter);

                            URL url = new URL(sURLAndFileName[FILE_DOWNLOAD_ADDRESS]);
                            URLConnection connection = url.openConnection();
                            connection.connect();

                            // download the file
                            input = new BufferedInputStream(url.openStream(), 8192);

                            int count;
                            while ((count = input.read(data)) != -1) {

                                // writing data to file
                                output.write(data, 0, count);
                            }

                            // flushing output
                            output.flush();

                            // closing streams
                            output.close();
                            input.close();

                        }




                    } else { //If bUseDownloadManager....

                        String sNewFullPathFilename = fDestination.getPath() + File.separator +
                                sJumbledNewFileName;
                        File fNewFile = new File(sNewFullPathFilename);

                        if(!fNewFile.exists()) {

                            BroadcastProgress(true, "Initiating download of file: " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS] + "...",
                                    false, iProgressBarValue,
                                    true, "Submitting download requests...",
                                    sIntentActionFilter);

                            //Use the download manager to download the file:
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sURLAndFileName[FILE_DOWNLOAD_ADDRESS]));
                            /*request.setTitle("AG Gallery+ File Download: " + "Comic ID " + ci.sItemID)
                                    .setDescription("Comic ID " + ci.sItemID + "; " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS])
                                    //.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                    //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                                    //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                                    .setMimeType("application/octet-stream")
                                    .setDestinationUri(Uri.fromFile(fNewFile));*/
                            //The above method no longer works as of Android 11 API level 30, One UI version 3.1.

                            String sDownloadFolderRelativePath;
                            sDownloadFolderRelativePath = File.separator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_COMICS] +
                                    File.separator + ci.sFolder_Name;
                            sComicDownloadFolder = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() +
                                    sDownloadFolderRelativePath;
                            request.setTitle("AG Gallery+ File Download: " + "Comic ID " + ci.sItemID)
                                    .setDescription("Comic ID " + ci.sItemID + "; " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS])
                                    //.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                    //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                                    //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                                    .setMimeType("application/octet-stream")
                                    .setDestinationInExternalFilesDir(getApplicationContext(), sDownloadFolderRelativePath, sJumbledNewFileName);

                            long lDownloadID = downloadManager.enqueue(request);
                            allDownloadIDs.add(lDownloadID); //Record the download ID so that we can check to see if it is completed via the worker.

                        }

                    } //End if bUseDownloadManager.

                    lProgressNumerator++;

                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    BroadcastProgress(true, "\n",
                            true, iProgressBarValue,
                            false, "",
                            sIntentActionFilter);


                }
                //Success downloading files.

                //Start a worker to move the downloaded files if using DownloadManager:
                if(globalClass.gbUseDownloadManager){

                    long[] lDownloadIDs = new long[allDownloadIDs.size()];
                    for(int i = 0; i < allDownloadIDs.size(); i++){
                        lDownloadIDs[i] = allDownloadIDs.get(i);
                    }

                    //Build-out data to send to the worker:
                    Data dataComicDownloadPostProcessor = new Data.Builder()
                            .putString(Worker_ComicPostProcessing.KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS, sComicDownloadFolder)
                            .putString(Worker_ComicPostProcessing.KEY_ARG_WORKING_FOLDER, fDestination.getAbsolutePath())
                            .putLongArray(Worker_ComicPostProcessing.KEY_ARG_DOWNLOAD_IDS, lDownloadIDs)
                            .putString(Worker_ComicPostProcessing.KEY_ARG_ITEM_ID, ci.sItemID)
                            .build();
                    OneTimeWorkRequest otwrComicDownloadPostProcessor = new OneTimeWorkRequest.Builder(Worker_ComicPostProcessing.class)
                            .setInputData(dataComicDownloadPostProcessor)
                            .addTag(Worker_ComicPostProcessing.WORKER_COMIC_POST_PROCESSING_TAG) //To allow finding the worker later.
                            .build();
                    UUID UUIDWorkID = otwrComicDownloadPostProcessor.getId();
                    WorkManager.getInstance(getApplicationContext()).enqueue(otwrComicDownloadPostProcessor);



                }





                BroadcastProgress(true, "Operation complete.\nSome files may continue to download in the background. Comic will be available when downloads complete.",
                        true, iProgressBarValue,
                        false, "",
                        sIntentActionFilter);

            } catch (Exception e) {
                if(e.getMessage() != null) {
                    Log.e("Error: ", e.getMessage());
                }
                BroadcastProgress(true, "Problem encountered:\n" + e.getMessage(),
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        sIntentActionFilter);
            } finally {
                if(output != null) {
                    output.close();
                }
                if(input != null) {
                    input.close();
                }
            }

        }

        if(ci.iSpecialFlag != ItemClass_CatalogItem.FLAG_NO_CODE){
            //Update the catalog file to note the post-processing code:
            globalClass.CatalogDataFile_UpdateRecord(ci);
        }

        //Put processed catalog item in globalclass to allow easier pass-back of catalog item:
        globalClass.gci_ImportComicWebItem = ci;

        //Modify viewer settings to show the newly-imported files:
        /*globalClass.giCatalogViewerSortBySetting[GlobalClass.MEDIA_CATEGORY_COMICS] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        globalClass.gbCatalogViewerSortAscending[GlobalClass.MEDIA_CATEGORY_COMICS] = false;*/ //Handled in onHandleIntent.

    }

    //====== Video Download Routines ===============================================================

    private void handleAction_startActionVideoAnalyzeHTML(){

        String sIntentActionFilter = Fragment_Import_1a_VideoWebDetect.ImportDataServiceResponseReceiver.IMPORT_RESPONSE_VIDEO_WEB_DETECT;

        BroadcastProgress(true, "Searching webpage for target data...",
                false, 0,
                false, "",
                sIntentActionFilter);

        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        ItemClass_WebVideoDataLocator icWebDataLocator = null;
        for(ItemClass_WebVideoDataLocator icWVDL: globalClass.galWebVideoDataLocators){
            if(icWVDL.bHostNameMatchFound){
                icWebDataLocator = icWVDL;
                break;
            }
        }
        if(icWebDataLocator == null){
            BroadcastProgress(true, "This webpage is incompatible at this time.",
                false, 0,
                false, "",
                sIntentActionFilter);
            return;
        }




        for (ItemClass_VideoDownloadSearchKey vdsk : icWebDataLocator.alVideoDownloadSearchKeys){

            int iStart;
            int iEnd;

            if(vdsk.sSearchStartString != null && vdsk.sSearchEndString != null) {
                //We want the data between vdsk.sSearchStartString and vdsk.sSearchEndString.
                iStart = icWebDataLocator.sHTML.indexOf(vdsk.sSearchStartString);
                if (iStart > -1) {
                    iStart += vdsk.sSearchStartString.length();
                    iEnd = icWebDataLocator.sHTML.indexOf(vdsk.sSearchEndString, iStart);
                    if (iEnd > -1) {
                        //We want the data between iStart and iEnd.
                        String sTemp;
                        sTemp = icWebDataLocator.sHTML.substring(iStart, iEnd);
                        if (sTemp.length() > 0) {
                            vdsk.bMatchFound = true;
                            vdsk.sSearchStringMatchContent = sTemp;
                        }

                    }
                }
            }
        } //End loop searching for data (textual search) within the HTML


        String sNonExplicitAddress = "^h%ttps:\\/\\/w%ww\\.p%ornh%ub\\.c%om\\/v%iew_v%ideo.p%hp(.*)"; //Don't allow b-o-t-s to easily find hard-coded addresses in this source.
        String sNHRegexExpression = sNonExplicitAddress.replace("%","");
        if(icWebDataLocator.sHostnameRegEx.equals(sNHRegexExpression)){
            //Special behavior for this website.
            //Finding the mp4 contiguous (non-M3U8) video download data for this particular site requires significant
            //parsing.

            String sS = "qualityItems";
            String sE = ";";
            int iS = icWebDataLocator.sHTML.indexOf(sS);
            int iE = icWebDataLocator.sHTML.indexOf(sE, iS);
            String Sub1 = icWebDataLocator.sHTML.substring(iS,iE);
            sS = "[{";
            sE = "}]";
            iS = Sub1.indexOf(sS);
            iE = Sub1.indexOf(sE, iS);
            String Sub2 = Sub1.substring(iS,iE);
            String Sub3 = Sub2.replace("},{", ";");
            Sub3 = Sub3.replace("[{","");
            String[] sVDOptions = Sub3.split(";");

            int iRecordNumber = icWebDataLocator.alVideoDownloadSearchKeys.size();
            for(String sVDORecords: sVDOptions){
                String[] sTemp = sVDORecords.split(",");
                if(sTemp.length >= 5){
                    //0: "id":"quality240p"
                    //1: "text":"240p"
                    //2: "url":"https:\/\/ev.phncdn.com\/videos\/201812\/31\/199453331\/240P_400K_199453331.mp4?validfrom=1624843943&validto=1624851143&rate=500k&burst=1400k&ipa=71.179.233.44&hash=MpIVLM9Yr5qK32YnDVbWG81%2Fhaw%3D"
                    //3: "upgrade":0
                    //4: "active":0
                    int iHTTPLocation = sTemp[2].indexOf("http");
                    if(iHTTPLocation >= 0) {
                        String sURL = sTemp[2].substring(sTemp[2].indexOf("http"));
                        sURL = sURL.replace("\"", "");
                        sURL = sURL.replace("\\", "");
                        icWebDataLocator.alVideoDownloadSearchKeys.add(
                                new ItemClass_VideoDownloadSearchKey(
                                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_LINK,
                                        "", ""));
                        icWebDataLocator.alVideoDownloadSearchKeys.get(iRecordNumber).sSearchStringMatchContent = sURL;
                        icWebDataLocator.alVideoDownloadSearchKeys.get(iRecordNumber).bMatchFound = true; //Fake it.

                        iRecordNumber++;
                    }

                }
            }

        }





        BroadcastProgress(true, "Textual searches complete. Performing XPath analysis...",
                false, 0,
                false, "",
                sIntentActionFilter);






        /*boolean bUseDownloadManager = false;
        if(icWebDataLocator.alVideoDownloadSearchKeys == null){
            //This just to get rid of the warning that bUseDownloadManager is always false.
            bUseDownloadManager = true;
        }
        if(!bUseDownloadManager) {
            try {
                // Output stream
                String sOutput = getExternalFilesDir(null) + File.separator + "test.txt";
                OutputStream output = new FileOutputStream(sOutput);

                byte[] data = new byte[1024];

                //Trying to download a blob:
                URL url = new URL("https://www.pornhub.com/7af454a8-af5f-49c0-8791-dab93c89983b");
                URLConnection connection = url.openConnection();
                connection.connect();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                int count;
                while ((count = input.read(data)) != -1) {

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
            }catch (Exception e){
                Log.d("test", e.getMessage());
            }
        }*/













        //Note: DocumentBuilderFactory.newInstance().newDocumentBuilder().parse....
        //  does not work well to parse this html. Modern html interpreters accommodate
        //  certain "liberties" in the code. That parse routine is meant for tight XML.
        //  HtmlCleaner does a good job processing the html in a manner similar to modern
        //  browsers.
        //Clean up the HTML:
        HtmlCleaner pageParser = new HtmlCleaner();
        CleanerProperties props = pageParser.getProperties();
        props.setAllowHtmlInsideAttributes(true);
        props.setAllowMultiWordAttributes(true);
        props.setRecognizeUnicodeChars(true);
        props.setOmitComments(true);
        TagNode node = null;
        try {
            node = pageParser.clean(icWebDataLocator.sHTML);
        } catch (Exception e){
            String sMessage = "Problem with HTML parser. Try again?\n" + e.getMessage();
            problemNotificationConfig(sMessage, sIntentActionFilter);
            return;
        }
        //For acquiring clean html for use with xPathExpression testing tool at https://www.freeformatter.com/xpath-tester.html:
        String sCleanHTML= "<" + node.getName() + ">" + pageParser.getInnerHtml(node) + "</" + node.getName() + ">";


        boolean bProblem = false;

        //Attempt to get the video title from the webpage via XPath:
        String sXPathExpressionTitleLocator = null;
        for (ItemClass_VideoDownloadSearchKey vdsk : icWebDataLocator.alVideoDownloadSearchKeys){
            if(vdsk.sDataType.equals(VIDEO_DOWNLOAD_TITLE)){
                if(vdsk.sSXPathExpression != null){
                    sXPathExpressionTitleLocator = vdsk.sSXPathExpression;
                }
            }
        }
        String sTitle = "";
        if(sXPathExpressionTitleLocator != null) {
            try {
                //Use an xPathExpression (similar to RegEx) to look for the video title in the html/xml:
                Object[] objsTitle = node.evaluateXPath(sXPathExpressionTitleLocator);
                //Check to see if we found anything:
                if (objsTitle != null && objsTitle.length > 0) {
                    //If we found something, assign it to a string:
                    for (Object oTitle : objsTitle) {
                        sTitle = oTitle.toString();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                bProblem = true;
                problemNotificationConfig(e.getMessage(), sIntentActionFilter);
            }
        }



        //Attempt to get the thumbnail address from the Webpage html:
        BroadcastProgress(true, "Looking for thumbnail address...",
                false, 0,
                false, "",
                sIntentActionFilter);

        String sXPathExpressionThumbnailLocator = null;
        for (ItemClass_VideoDownloadSearchKey vdsk : icWebDataLocator.alVideoDownloadSearchKeys){
            if(vdsk.sDataType.equals(ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_THUMBNAIL)){
                if(vdsk.sSXPathExpression != null){
                    sXPathExpressionThumbnailLocator = vdsk.sSXPathExpression;
                }
            }
        }
        String sURLThumbnail = "";
        if(sXPathExpressionThumbnailLocator != null) {
            try {
                //Use an xPathExpression (similar to RegEx) to look for tag data in the html/xml:
                Object[] objsThumbnail = node.evaluateXPath(sXPathExpressionThumbnailLocator);
                //Check to see if we found anything:
                if (objsThumbnail != null && objsThumbnail.length > 0) {
                    //If we found something, assign it to a string:
                    for (Object oTags : objsThumbnail) {
                        sURLThumbnail = oTags.toString();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                bProblem = true;
                problemNotificationConfig(e.getMessage(), sIntentActionFilter);
            }
        }

        //Attempt to get the tags from the WebPage html:
        BroadcastProgress(true, "Looking for video category tags...",
                false, 0,
                false, "",
                sIntentActionFilter);

        String sXPathExpressionTagsLocator = null;
        for (ItemClass_VideoDownloadSearchKey vdsk : icWebDataLocator.alVideoDownloadSearchKeys){
            if(vdsk.sDataType.equals(ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TAGS)){
                if(vdsk.sSXPathExpression != null){
                    sXPathExpressionTagsLocator = vdsk.sSXPathExpression;
                }
            }
        }
        ArrayList<String> alsTags = new ArrayList<>();
        if(sXPathExpressionTagsLocator != null) {
            try {
                //Use an xPathExpression (similar to RegEx) to look for tag data in the html/xml:
                Object[] objsTags = node.evaluateXPath(sXPathExpressionTagsLocator);
                //Check to see if we found anything:
                if (objsTags != null && objsTags.length > 0) {
                    //If we found something, assign it to a string:
                    for (Object oTags : objsTags) {
                        alsTags.add(oTags.toString());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                bProblem = true;
                problemNotificationConfig(e.getMessage(), sIntentActionFilter);
            }
        }
        //Pre-process tags. Identify tags that already exist, and create a list of new tags for
        //  the user to approve - don't automatically add new tags to the system (I've encountered
        //  garbage tags, tags that already exist in another form, and tags that the user might
        //  not want to add.
        ArrayList<String> alsUnidentifiedTags = new ArrayList<>();
        ArrayList<Integer> aliIdentifiedTags = new ArrayList<>();
        for(String sTag: alsTags){
            String sIncomingTagCleaned = sTag.toLowerCase().trim();
            boolean bTagFound = false;
            for(Map.Entry<String, ItemClass_Tag> TagEntry: globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).entrySet()){
                String sExistingTagCleaned = TagEntry.getKey().toLowerCase().trim();
                if(sExistingTagCleaned.equals(sIncomingTagCleaned)){
                    bTagFound = true;
                    aliIdentifiedTags.add(TagEntry.getValue().iTagID);
                    break;
                }
            }
            if(!bTagFound){
                alsUnidentifiedTags.add(sTag.trim());
            }
        }




        //Assemble a list of FileItems (ItemClass_File) listing the potential downloads.
        //  Include filename, download address, tags, and, if available, file size, resolution, and duration:
        ArrayList<ItemClass_File> alicf_VideoDownloadFileItems = new ArrayList<>();

        //First go through and grab the title so that the title can be passed into FileItems that are created next:
        for (ItemClass_VideoDownloadSearchKey vdsk : icWebDataLocator.alVideoDownloadSearchKeys) {
            if (vdsk.bMatchFound) {
                if (vdsk.sDataType.equals(VIDEO_DOWNLOAD_TITLE)) {
                    sTitle = cleanHTMLCodedCharacters(vdsk.sSearchStringMatchContent); //Convert any html coded characters
                }
            }
        }

        //Look for potential downloads' file sizes, and download any m3u8 text file if it exists:
        for (ItemClass_VideoDownloadSearchKey vdsk : icWebDataLocator.alVideoDownloadSearchKeys){
            if(vdsk.bMatchFound) {
                switch (vdsk.sDataType) {
                    case VIDEO_DOWNLOAD_TITLE:
                        //Do nothing (this is processed above)
                        break;
                    case VIDEO_DOWNLOAD_LINK:
                        BroadcastProgress(true, "Analyzing video link: " + vdsk.sSearchStringMatchContent,
                                false, 0,
                                false, "",
                                sIntentActionFilter);
                        try {
                            URL urlVideoLink = new URL(vdsk.sSearchStringMatchContent);

                            //Locate a file name, likely in between the last '/' and either a '?' or length of string.
                            String sTempFilename;
                            sTempFilename = vdsk.sSearchStringMatchContent;
                            int iStartLocation = Math.max(sTempFilename.lastIndexOf("/") + 1, 0);
                            int iEndLocation;
                            int iSpecialEndCharLocation = sTempFilename.lastIndexOf("?");
                            if (iSpecialEndCharLocation > 0) {
                                iEndLocation = iSpecialEndCharLocation;
                            } else {
                                iEndLocation = sTempFilename.length();
                            }
                            sTempFilename = sTempFilename.substring(iStartLocation, iEndLocation);
                            ItemClass_File icf = new ItemClass_File(ItemClass_File.TYPE_URL, sTempFilename);
                            icf.sURLVideoLink = vdsk.sSearchStringMatchContent;

                            HttpURLConnection connection = (HttpURLConnection) urlVideoLink.openConnection();
                            connection.setRequestProperty("Accept-Encoding", "identity");
                            vdsk.lFileSize = connection.getContentLength(); //Returns -1 if content size is not in the header.
                            icf.lSizeBytes = vdsk.lFileSize;
                            connection.disconnect();

                            icf.sTitle = sTitle;
                            icf.sURLThumbnail = sURLThumbnail;
                            icf.alsUnidentifiedTags = alsUnidentifiedTags; //Assign textual string of tags. Will digest and convert/import new tags if user chooses to continue import.
                            icf.aliRecognizedTags = aliIdentifiedTags; //todo: redundant?
                            icf.aliProspectiveTags = aliIdentifiedTags;

                            alicf_VideoDownloadFileItems.add(icf);

                        } catch (Exception e) {
                            vdsk.bErrorWithLink = true;
                            vdsk.sErrorMessage = e.getMessage();
                        }

                        break;
                    case VIDEO_DOWNLOAD_M3U8:
                        BroadcastProgress(true, "Analyzing video stream: " + vdsk.sSearchStringMatchContent,
                                false, 0,
                                false, "",
                                sIntentActionFilter);
                        try {
                            String sURL = vdsk.sSearchStringMatchContent;
                            URL url = new URL(vdsk.sSearchStringMatchContent);
                            URLConnection connection = url.openConnection();
                            connection.connect();

                            // download the file
                            InputStream inputStream = new BufferedInputStream(url.openStream(), 1024 * 8);


                            StringBuilder sbM3U8Content = new StringBuilder();
                            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                            for (String line; (line = r.readLine()) != null; ) {
                                sbM3U8Content.append(line).append('\n');
                            }
                            inputStream.close();









                            /*
                            HLS: Http Live Streaming
                            https://www.toptal.com/apple/introduction-to-http-live-streaming-hls#:~:text=What%20is%20an%20M3U8%20file,used%20to%20define%20media%20streams.

                            Below is an example of an M3U8 "master" file. It points to other M3U8 files with video "chunks".

                            M3U comments begin with the '#' character, unless ended with a semicolon.
                            My comments begin with '!'.
                            Example M3U8 data:

                            #EXTM3U               !Header. Must be first line. Required. Standard.
                            #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=763904,RESOLUTION=854x480,NAME="480p"  !Optional. This example gives some data about the stream.
                            hls-480p-fe738.m3u8?e=1616648203&l=0&h=8f2321bd696368a1795a61fc978a0dbb
                            #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1327104,RESOLUTION=1280x720,NAME="720p"
                            hls-720p-02c5c.m3u8?e=1616648203&l=0&h=8f2321bd696368a1795a61fc978a0dbbf
                            #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=2760704,RESOLUTION=1920x1080,NAME="1080p"
                            hls-1080p-cffd4.m3u8?e=1616648203&l=0&h=8f2321bd696368a1795a61fc978a0dbb
                            #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=423936,RESOLUTION=640x360,NAME="360p"
                            hls-360p-f6185.m3u8?e=1616648203&l=0&h=8f2321bd696368a1795a61fc978a0dbb
                            #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=155648,RESOLUTION=444x250,NAME="250p"
                            hls-250p-2b29e.m3u8?e=1616648203&l=0&h=8f2321bd696368a1795a61fc978a0dbb
                             */

                            if(globalClass.gbLogM3U8Files) {
                                //Write the m3u8 master file to the logs folder for debugging purposes:
                                String sShortFileName = sURL.substring(sURL.lastIndexOf("/") + 1);
                                sShortFileName = cleanFileNameViaTrim(sShortFileName);
                                String sM3U8FilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                                        File.separator + GlobalClass.GetTimeStampFileSafe() + "_" + sShortFileName + ".txt";
                                File fM3U8 = new File(sM3U8FilePath);
                                FileWriter fwM3U8File = new FileWriter(fM3U8, true);
                                fwM3U8File.write("#" + sURL + "\n");
                                fwM3U8File.write(sbM3U8Content.toString());
                                fwM3U8File.flush();
                                fwM3U8File.close();
                            }


                            //Locate the M3U8 items:
                            //Go through the M3U8 master list and identify the .M3U8 files.
                            String sM3U8_MasterData = sbM3U8Content.toString();
                            String[] sM3U8_MasterDataLines = sM3U8_MasterData.split("\n");
                            ArrayList<ItemClass_M3U8> al_M3U8 = new ArrayList<>();
                            boolean bReadingM3U8Item = false;
                            ItemClass_M3U8 icM3U8 = null;
                            for (String sLine : sM3U8_MasterDataLines) {
                                if (sLine.startsWith("#EXT-X-STREAM-INF")) {
                                    bReadingM3U8Item = true;
                                    icM3U8 = new ItemClass_M3U8();
                                    icM3U8.sBaseURL = vdsk.sSearchStringMatchContent.substring(0, vdsk.sSearchStringMatchContent.lastIndexOf("/"));
                                    String[] sTemp1 = sLine.split(":");
                                    if (sTemp1.length > 1) {
                                        String[] sTemp2 = sTemp1[1].split(",");
                                        for (String sDataItem : sTemp2) {
                                            String[] sDataSplit = sDataItem.split("=");
                                            if (sDataSplit.length > 1) {
                                                if (sDataSplit[0].equals("BANDWIDTH")) {
                                                    icM3U8.sBandwidth = sDataSplit[1];
                                                }
                                                if (sDataSplit[0].equals("RESOLUTION")) {
                                                    icM3U8.sResolution = sDataSplit[1];
                                                }
                                                if (sDataSplit[0].equals("NAME")) {
                                                    icM3U8.sName = sDataSplit[1];
                                                }
                                            }
                                        }
                                    }
                                } else if (bReadingM3U8Item) {
                                    bReadingM3U8Item = false;
                                    if (icM3U8 != null) {
                                        icM3U8.sFileName = sLine;
                                        al_M3U8.add(icM3U8);
                                    }
                                }
                            }


                            //Example of an M3U8 item file containing TS file entries:
                            /*
                            #EXTM3U
                            #EXT-X-VERSION:3
                            #EXT-X-TARGETDURATION:10
                            #EXT-X-MEDIA-SEQUENCE:0
                            #EXTINF:10.010011,
                            hls-480p-fe7380.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                            #EXTINF:10.010011,
                            hls-480p-fe7381.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                            #EXTINF:10.010011,
                            hls-480p-fe7382.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                            #EXTINF:10.010011,
                            hls-480p-fe73880.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                            #EXTINF:9.743067,
                            hls-480p-fe73881.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                            #EXT-X-ENDLIST
                            e7dff1bc
                            #EXTINF:10.010011,
                            hls-480p-fe73871.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                            #EXTINF:10.010011,
                            hls-480p-fe73872.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                            #EXTINF:10.010011,
                            hls-480p-fe73877.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                            #EXTIN
                            */

                            //Evaluate the M3U8 files and create a list of the .ts files in each M3U8 item:
                            for (ItemClass_M3U8 icM3U8_entry : al_M3U8) {
                                String sUrl = icM3U8_entry.sBaseURL + "/" + icM3U8_entry.sFileName;
                                url = new URL(sUrl);
                                connection = url.openConnection();
                                connection.connect();

                                // download the M3U8 text file:
                                inputStream = new BufferedInputStream(url.openStream(), 1024 * 8);
                                r = new BufferedReader(new InputStreamReader(inputStream));
                                StringBuilder total = new StringBuilder();
                                for (String line; (line = r.readLine()) != null; ) {
                                    total.append(line).append('\n');
                                }
                                String sM3U8Content = total.toString();
                                inputStream.close();

                                String sShortFileName = cleanFileNameViaTrim(icM3U8_entry.sFileName);
                                if(globalClass.gbLogM3U8Files) {
                                    //Write the m3u8 file to the logs folder for debugging purposes:

                                    String sM3U8FilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                                            File.separator + GlobalClass.GetTimeStampFileSafe() + "_" + sShortFileName + ".txt";
                                    File fM3U8 = new File(sM3U8FilePath);
                                    FileWriter fwM3U8File = new FileWriter(fM3U8, true);
                                    fwM3U8File.write(sM3U8Content);
                                    fwM3U8File.flush();
                                    fwM3U8File.close();
                                }

                                /*final String sM3U8InterprettedFilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                                        File.separator + GlobalClass.GetTimeStampFileSafe() + "_" + sShortFileName + "_interpretted.txt";
                                final File fM3U8Interpretted = new File(sM3U8InterprettedFilePath);
                                FileWriter fwM3U8InterprettedFile;
                                fwM3U8InterprettedFile = new FileWriter(fM3U8Interpretted, true);*/


                                //Evaluate lines in the M3U8 file to check if a .ts file name and add it to the arraylist if so:
                                String[] sLines = sM3U8Content.split("\n");
                                float fDurationInSeconds = 0.0f;
                                for (String sLine : sLines) {

                                    if (!sLine.startsWith("#") && sLine.contains(".ts")) {// && sLine.startsWith("hls")) {
                                        if (icM3U8_entry.als_TSDownloads == null) {
                                            icM3U8_entry.als_TSDownloads = new ArrayList<>();
                                        }
                                        icM3U8_entry.als_TSDownloads.add(sLine); //Add our detected TS download address to the M3U8 item.
                                        //fwM3U8InterprettedFile.write(sLine + "\n");
                                    } else if (sLine.contains("ENDLIST")) {
                                        break;
                                    } else if (sLine.contains("EXTINF")){
                                        //Try to pull out duration:
                                        String[] sData1 = sLine.split(":");
                                        if(sData1.length > 1){
                                            String sData2 = sData1[1];
                                            sData2 = sData2.replace(",","");
                                            //sData2 should now have the duration of the ts file.
                                            try{
                                                float fTemp = Float.parseFloat(sData2);
                                                fDurationInSeconds = fDurationInSeconds + fTemp;
                                            } catch (Exception e){
                                                Log.d("M3U8", "String to float conversion error. Cannot convert: " + sData2);
                                            }
                                        }
                                    }

                                }
                                /*fwM3U8InterprettedFile.flush();
                                fwM3U8InterprettedFile.close();*/

                                icM3U8_entry.fDurationInSeconds = fDurationInSeconds; //Load the duration now for confirmation of video concatenation completion later.

                            }


                            /*//Test download of TS files:
                            File fDestinationFolder = new File(
                                    globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath()
                                            + File.separator + "Test");
                            if(!fDestinationFolder.exists()){

                                fDestinationFolder.mkdir();

                                DownloadManager downloadManager = null;

                                downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

                                String sDownloadName;
                                for(int i = 0; i < alals_TSDownloads.get(0).size(); i++){
                                    sDownloadName = alals_TSDownloads.get(0).get(i);
                                    //Use the download manager to download the file:
                                    String sAbbrevFileName = sDownloadName.substring(0,sDownloadName.lastIndexOf("?"));
                                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(al_M3U8.get(0).sBaseURL + "/" + sDownloadName));
                                    File fDestinationFile = new File(fDestinationFolder.getPath() + File.separator + sAbbrevFileName);
                                    request.setTitle("AG Gallery+ File Download : " + sAbbrevFileName)
                                            .setDescription(sAbbrevFileName)
                                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                                            //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                                            .setMimeType("application/octet-stream")
                                            .setDestinationUri(Uri.fromFile(fDestinationFile));
                                    downloadManager.enqueue(request);
                                }
                            }*/

                            //Obtain size of each TS file set of downloads:
                            //Loop through the M3U8 entries, such as video @ 240x320, video @ 640x480, @720p, @1080p, etc:

                            for (ItemClass_M3U8 icM3U8_entry : al_M3U8) {
                                //Loop through the TS downloads for each of the M3U8 entries and accumulate the file sizes:

                                int iFileSizeLoopCount = 0;
                                int iDataAcquiredLoopCount = 0;
                                long lFileSizeAccumulator = 0;

                                for (String sTSDownloadAddress : icM3U8_entry.als_TSDownloads) {

                                    URL urlPage = new URL(icM3U8_entry.sBaseURL + "/" + sTSDownloadAddress);
                                    String sFilename = icM3U8_entry.sBaseURL + "/" + icM3U8_entry.sFileName;
                                    BroadcastProgress(true, "Getting file size data for video stream: " + sFilename + "\n",
                                        false, 0,
                                        false, "",
                                        sIntentActionFilter); //Broadcast progress

                                    HttpURLConnection httpURLConnection = (HttpURLConnection) urlPage.openConnection();
                                    httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
                                    long lSingleTSFileDownloadSize = httpURLConnection.getContentLength(); //Returns -1 if content size is not in the header.
                                    httpURLConnection.disconnect();
                                    iFileSizeLoopCount++;

                                    if (lSingleTSFileDownloadSize > 0) {
                                        iDataAcquiredLoopCount++;
                                        lFileSizeAccumulator = lFileSizeAccumulator + lSingleTSFileDownloadSize;
                                    }
                                    if (iDataAcquiredLoopCount == 5 || iFileSizeLoopCount == 10) {
                                        //A set of the TS files will be representative of all of the TS file sizes for the given video.
                                        icM3U8_entry.lTotalTSFileSetSize = (lFileSizeAccumulator / iDataAcquiredLoopCount) * icM3U8_entry.als_TSDownloads.size();
                                        break;
                                    }
                                }
                                //Create a file item to record the results:
                                String sFilename = cleanFileNameViaTrim(icM3U8_entry.sFileName);
                                ItemClass_File icf = new ItemClass_File(ItemClass_File.TYPE_M3U8, sFilename);
                                icf.ic_M3U8 = icM3U8_entry;
                                icf.lSizeBytes = icM3U8_entry.lTotalTSFileSetSize;
                                icf.sURLThumbnail = sURLThumbnail;
                                icf.alsUnidentifiedTags = alsUnidentifiedTags; //Assign textual string of tags. Will digest and convert/import new tags if user chooses to continue import.
                                icf.aliRecognizedTags = aliIdentifiedTags; //todo: redundant?
                                icf.aliProspectiveTags = aliIdentifiedTags;
                                icf.sTitle = sTitle;
                                alicf_VideoDownloadFileItems.add(icf); //Add item to list of file items to return;

                            }
                            //Finished obtaining sizes of the TS file sets.

                        } catch (Exception e) {
                            vdsk.bErrorWithLink = true;
                            vdsk.sErrorMessage = e.getMessage();
                            problemNotificationConfig(e.getMessage(), sIntentActionFilter);
                        }
                        break;
                }

            }
        } //End loop searching for data within the HTML

        //Broadcast a message to be picked-up by the VideoWebDetect fragment:
        /*Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(sIntentActionFilter);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        if(!bProblem) {
            broadcastIntent.putExtra(EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, true);
            broadcastIntent.putExtra(EXTRA_AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE, alicf_VideoDownloadFileItems);
        } else {
            broadcastIntent.putExtra(EXTRA_BOOL_PROBLEM, bProblem);
            broadcastIntent.putExtra(EXTRA_STRING_PROBLEM, sProblemMessage);
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);*/

        BroadcastProgress(true, "HTML examination complete. Click 'Next' to continue.",
                false, 0,
                false, "",
                sIntentActionFilter);

        if(!bProblem) {
            //Also send a broadcast to Activity Import to capture the download items in an array adapter:
            Intent broadcastIntent_VideoWebDetectResponse = new Intent();
            broadcastIntent_VideoWebDetectResponse.putExtra(EXTRA_BOOL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE, true);
            broadcastIntent_VideoWebDetectResponse.putExtra(EXTRA_AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE, alicf_VideoDownloadFileItems);
            broadcastIntent_VideoWebDetectResponse.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
            broadcastIntent_VideoWebDetectResponse.addCategory(Intent.CATEGORY_DEFAULT);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_VideoWebDetectResponse);
        }


    }

    private void handleAction_startActionVideoDownload(String sAddress){

        String sIntentActionFilter = Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE;

        int iMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        globalClass.gbUseFFMPEGToMerge = sharedPreferences.getBoolean(GlobalClass.PREF_USE_FFMPEG_TO_MERGE_VIDEO_STREAMS, false);

        ArrayList<ItemClass_File> alFileList = globalClass.galImportFileList;
        ItemClass_File icfDownloadItem = alFileList.get(0);
        //Use file count as progress denominator:
        if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
            //If this is a single download file, only 1 file needs to be downloaded.
            lProgressDenominator = 1;
        } else {
            //If this is an M3U8 download, a set of files must be downloaded.
            lProgressDenominator = (long) icfDownloadItem.ic_M3U8.als_TSDownloads.size();
        }

        //Find the next record ID:
        String sNextRecordId = globalClass.getNewCatalogRecordID(GlobalClass.MEDIA_CATEGORY_VIDEOS);

        if(icfDownloadItem.sDestinationFolder.equals("")) {
            icfDownloadItem.sDestinationFolder = GlobalClass.gsUnsortedFolderName;
        }

        File fDestination = new File(
                globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                        icfDownloadItem.sDestinationFolder);

        if (!fDestination.exists()) {
            if (!fDestination.mkdir()) {
                //Unable to create directory
                BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                return;
            } else {
                BroadcastProgress(true, "Destination folder created: " + fDestination.getPath() + "\n",
                        false, iProgressBarValue,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            }
        } else {
            BroadcastProgress(true, "Destination folder verified: " + fDestination.getPath() + "\n",
                    true, iProgressBarValue,
                    false, "",
                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
        }

        //Create a folder to serve as a working folder:
        File fWorkingFolder = new File(
                globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                        icfDownloadItem.sDestinationFolder + File.separator + sNextRecordId);

        //Create the temporary download folder (within the destination folder):
        if (!fWorkingFolder.exists()) {
            if (!fWorkingFolder.mkdir()) {
                //Unable to create directory
                BroadcastProgress(true, "Unable to create destination folder at: " + fWorkingFolder.getPath() + "\n",
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        sIntentActionFilter);
                return;
            } else {
                BroadcastProgress(true, "Destination folder created: " + fWorkingFolder.getPath() + "\n",
                        false, iProgressBarValue,
                        false, "",
                        sIntentActionFilter);
            }
        } else {
            BroadcastProgress(true, "Destination folder verified: " + fWorkingFolder.getPath() + "\n",
                    true, iProgressBarValue,
                    false, "",
                    sIntentActionFilter);
        }

        // With the download folder successfully created, record the catalog item:
        // The below call should add the record to both the catalog contents file
        //  and memory:
        //Next create a new catalog item data structure:
        ItemClass_CatalogItem ciNew = new ItemClass_CatalogItem();
        ciNew.iMediaCategory = iMediaCategory;
        ciNew.sItemID = sNextRecordId;
        ciNew.lSize = icfDownloadItem.lSizeBytes;
        ciNew.lDuration_Milliseconds = icfDownloadItem.lVideoTimeInMilliseconds;
        ciNew.sDuration_Text = icfDownloadItem.sVideoTimeText;
        if(!icfDownloadItem.sWidth.equals("") && !icfDownloadItem.sHeight.equals("")) {
            ciNew.iWidth = Integer.parseInt(icfDownloadItem.sWidth);
            ciNew.iHeight = Integer.parseInt(icfDownloadItem.sHeight);
        }
        ciNew.sFolder_Name = icfDownloadItem.sDestinationFolder;
        ciNew.sTags = GlobalClass.formDelimitedString(icfDownloadItem.aliProspectiveTags, ",");
        Double dTimeStamp = GlobalClass.GetTimeStampFloat();
        ciNew.dDatetime_Last_Viewed_by_User = dTimeStamp;
        ciNew.dDatetime_Import = dTimeStamp;
        ciNew.iGrade = icfDownloadItem.iGrade;
        ciNew.sSource = sAddress;
        ciNew.sTitle = icfDownloadItem.sTitle;
        ciNew.alsDownloadURLsAndDestFileNames = new ArrayList<>();
        if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
            ciNew.iSpecialFlag = ItemClass_CatalogItem.FLAG_PROCESSING_VIDEO_DLM_SINGLE;
            ciNew.iFile_Count = 1;
            ciNew.sVideoLink = icfDownloadItem.sURLVideoLink;
            ciNew.sFilename = GlobalClass.JumbleFileName(icfDownloadItem.sFileOrFolderName);
        } else {
            //M3U8. Mark post-processing to concat videos and move the result.
            if(globalClass.gbUseFFMPEGToMerge) {
                ciNew.iSpecialFlag = ItemClass_CatalogItem.FLAG_PROCESSING_VIDEO_DLM_CONCAT;
                //Form a name for the concatenated video file:
                String sTempFilename = icfDownloadItem.ic_M3U8.sFileName;
                sTempFilename = cleanFileNameViaTrim(sTempFilename); //Remove special characters.
                sTempFilename = ciNew.sItemID + "_" + sTempFilename; //Add item ID to create a unique filename.
                sTempFilename = sTempFilename.substring(0,sTempFilename.lastIndexOf(".")); //Remove extension (probably .m3u8).
                if(globalClass.gbUseFFMPEGConvertToMP4) {
                    sTempFilename = sTempFilename + ".mp4"; //Add appropriate extension.
                } else {
                    sTempFilename = sTempFilename + ".ts"; //Add appropriate extension.
                }
                ciNew.sFilename = GlobalClass.JumbleFileName(sTempFilename);
                ciNew.iFile_Count = 1; //There will only be 1 file after concatenation.
            } else {
                ciNew.iSpecialFlag = ItemClass_CatalogItem.FLAG_PROCESSING_M3U8_LOCAL;
                //Form a name for the M3U8 file:
                String sTempFilename = icfDownloadItem.ic_M3U8.sFileName;
                sTempFilename = cleanFileNameViaTrim(sTempFilename); //Remove special characters.
                sTempFilename = ciNew.sItemID + "_" + sTempFilename; //Add item ID to create a unique filename.
                ciNew.sFilename = sTempFilename; //Do not jumble the M3U8 file. Exoplayer will not recognize.
                ciNew.iFile_Count = icfDownloadItem.ic_M3U8.als_TSDownloads.size(); //Record the file count.

                //Configure thumbnail file for M3U8:
                String sThumbnailURL = icfDownloadItem.sURLThumbnail;
                try{
                    String sThumbnailFileName = sThumbnailURL.substring(sThumbnailURL.lastIndexOf("/") + 1);
                    sThumbnailFileName = cleanFileNameViaTrim(sThumbnailFileName);
                    ciNew.sThumbnail_File = GlobalClass.JumbleFileName(sThumbnailFileName);
                    ciNew.alsDownloadURLsAndDestFileNames.add(new String[]{sThumbnailURL, ciNew.sThumbnail_File});
                } catch (Exception e){
                    problemNotificationConfig("Could not get thumbnail image.", sIntentActionFilter);
                }
            }

            ciNew.sVideoLink = icfDownloadItem.ic_M3U8.sBaseURL + "/" + icfDownloadItem.ic_M3U8.sFileName;
            ciNew.lDuration_Milliseconds = (long) (icfDownloadItem.ic_M3U8.fDurationInSeconds * 1000); //Load the duration now for confirmation of video concatenation completion later.

        }

        globalClass.CatalogDataFile_CreateNewRecord(ciNew);

        //Map-out the files to be downloaded with destination file names:

        if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
            //If this is a single download file, only 1 file needs to be downloaded.
            String sDownloadAddress = icfDownloadItem.sURLVideoLink;
            String sFileName = icfDownloadItem.sFileOrFolderName;
            ciNew.alsDownloadURLsAndDestFileNames.add(new String[]{sDownloadAddress, sFileName});
        } else {
            //If this is an M3U8 download, a set of files must be downloaded.
            for(String sFileName: icfDownloadItem.ic_M3U8.als_TSDownloads){
                String sDownloadAddress = icfDownloadItem.ic_M3U8.sBaseURL + "/" + sFileName;
                String sNewFilename = ciNew.sItemID + "_" + cleanFileNameViaTrim(sFileName);  //the 'save-to' filename cannot have special chars or downloadManager will not download the file.
                if(ciNew.iSpecialFlag == ItemClass_CatalogItem.FLAG_PROCESSING_M3U8_LOCAL){
                    //If we will be holding the .ts files in storage as part of a local M3U8 configuration,
                    // jumble the .ts filenames:
                    sNewFilename = GlobalClass.JumbleFileName(sNewFilename);
                }
                ciNew.alsDownloadURLsAndDestFileNames.add(new String[]{sDownloadAddress, sNewFilename});
            }
        }



        //Initiate the download(s):

        //NOTE: Android has DownloadIdleService which is reponsible for cleaning up stale or orphan downloads.
        //  I have witnessed disappearance of downloaded files. This service seems to be deleting comic files.
        //See article at: https://www.vvse.com/blog/blog/2020/01/06/android-10-automatically-deletes-downloaded-files/

        try {

            DownloadManager downloadManager = null;
            downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            ArrayList<Long> allDownloadIDs = new ArrayList<>();

            //Download the file(s):
            int FILE_DOWNLOAD_ADDRESS = 0;
            int FILE_NAME_AND_EXTENSION = 1;

            String sVideoDownloadFolder = "";

            for(String[] sURLAndFileName: ciNew.alsDownloadURLsAndDestFileNames) {
                String sNewFullPathFilename = fWorkingFolder + File.separator + sURLAndFileName[FILE_NAME_AND_EXTENSION];
                //File name is not Jumbled for download as if it is a .ts file download of videos, FFMPEG will
                //  not understand what to do with the files if the extension is unrecognized.
                File fNewFile = new File(sNewFullPathFilename);
                /*//Debugging an issue...
                String s;
                try {
                    s = fNewFile.getCanonicalPath();  //Check to see if the file name and path is valid:
                } catch (Exception e){
                    sNewFullPathFilename = fTempDestination + File.separator + sURLAndFileName[FILE_NAME_AND_EXTENSION];
                    fNewFile = new File(sNewFullPathFilename);
                    try{
                        s = fNewFile.getCanonicalPath();
                    } catch (Exception e2){
                        Log.d("File exception", e2.getMessage());
                    }
                    Log.d("File exception", e.getMessage());
                }*/
                if(!fNewFile.exists()) {

                    BroadcastProgress(true, "Initiating download of file: " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS] + "...",
                            false, iProgressBarValue,
                            true, "Submitting download requests...",
                            sIntentActionFilter);

                    //Use the download manager to download the file:
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sURLAndFileName[FILE_DOWNLOAD_ADDRESS]));
                    /*request.setTitle("AG Gallery+ File Download: " + "Video ID " + ciNew.sItemID)
                            .setDescription("Video ID " + ciNew.sItemID + "; " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS])
                            //.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                            //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                            //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                            .setMimeType("application/octet-stream")
                            .setDestinationUri(Uri.fromFile(fNewFile));*/
                    //The above method no longer works as of Android 11 API level 30, One UI version 3.1.
                    String sDownloadFolderRelativePath;
                    sDownloadFolderRelativePath = File.separator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS] +
                                    File.separator + ciNew.sFolder_Name +
                                    File.separator + ciNew.sItemID;
                    sVideoDownloadFolder = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() +
                            sDownloadFolderRelativePath;
                    request.setTitle("AG Gallery+ File Download: " + "Video ID " + ciNew.sItemID)
                            .setDescription("Video ID " + ciNew.sItemID + "; " + sURLAndFileName[FILE_DOWNLOAD_ADDRESS])
                            //.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) //Make download notifications disappear when completed.
                            //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                            //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                            .setMimeType("application/octet-stream")
                            .setDestinationInExternalFilesDir(getApplicationContext(), sDownloadFolderRelativePath, fNewFile.getName());

                    long lDownloadID = downloadManager.enqueue(request);
                    allDownloadIDs.add(lDownloadID); //Record the download ID so that we can check to see if it is completed via the worker.


                    lProgressNumerator++;

                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    BroadcastProgress(true, "\n",
                            true, iProgressBarValue,
                            false, "",
                            sIntentActionFilter);
                }

            }
            //Success initiating file download(s).

            int iSingleOrM3U8;
            if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
                iSingleOrM3U8 = Worker_VideoPostProcessing.DOWNLOAD_TYPE_SINGLE;
            } else if(!globalClass.gbUseFFMPEGToMerge) {
                iSingleOrM3U8 = Worker_VideoPostProcessing.DOWNLOAD_TYPE_M3U8_LOCAL;
            } else {
                iSingleOrM3U8 = Worker_VideoPostProcessing.DOWNLOAD_TYPE_M3U8;
            }

            //If this is an M3U8 stream download and the user has elected not to concatenate via
            //  FFMPEG, download the M3U8 file as well:
            if((icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_M3U8) &&
                    (!globalClass.gbUseFFMPEGToMerge)){

                ItemClass_M3U8 icM3U8_entry = icfDownloadItem.ic_M3U8;

                String sUrl = icM3U8_entry.sBaseURL + "/" + icM3U8_entry.sFileName;
                URL url = new URL(sUrl);
                URLConnection connection = url.openConnection();
                connection.connect();

                // download the M3U8 text file:
                InputStream inputStream = new BufferedInputStream(url.openStream(), 1024 * 8);
                BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder total = new StringBuilder();
                for (String line; (line = r.readLine()) != null; ) {
                    total.append(line).append('\n');
                }
                String sM3U8Content = total.toString();
                inputStream.close();

                //Write the m3u8 file to the working folder:
                String sM3U8FilePath = fWorkingFolder.getAbsolutePath() +
                        File.separator + ciNew.sFilename;
                File fM3U8 = new File(sM3U8FilePath);
                FileWriter fwM3U8File = new FileWriter(fM3U8, true);

                String[] sLines = sM3U8Content.split("\n");
                for (String sLine : sLines) {
                    if (!sLine.startsWith("#") && sLine.contains(".ts")) {// && sLine.startsWith("hls")) {
                        String sTSShortFileName = ciNew.sItemID + "_" + cleanFileNameViaTrim(sLine);
                        String sJumbledTSShortFileName = GlobalClass.JumbleFileName(sTSShortFileName);
                        String sFullPathToTSFile = fWorkingFolder.getAbsolutePath() + File.separator + sJumbledTSShortFileName;
                        fwM3U8File.write(sFullPathToTSFile + "\n");
                    } else if (sLine.contains("ENDLIST")) {
                        fwM3U8File.write(sLine + "\n");
                        break;
                    } else {
                        fwM3U8File.write(sLine + "\n");
                    }

                }

                fwM3U8File.flush();
                fwM3U8File.close();

            }




            //Start Video Post-Processing Worker.
            //Testing WorkManager for video concatenation:
            //https://developer.android.com/topic/libraries/architecture/workmanager/advanced

            //Send:
            // Location to monitor
            // Single-file or M3U8 result (M3U8 => multiple .ts files)
            // Expected file count (needed for M3U8)
            // Name of file to create

            /*public static final String KEY_ARG_DOWNLOAD_TYPE_SINGLE_OR_M3U8 = "KEY_ARG_DOWNLOAD_TYPE_SINGLE_OR_M3U8";
            public static final String KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS = "KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS";
            public static final String KEY_ARG_EXPECTED_DOWNLOAD_FILE_COUNT = "KEY_ARG_EXPECTED_DOWNLOAD_FILE_COUNT";
            public static final String KEY_ARG_VIDEO_OUTPUT_FILENAME = "KEY_ARG_VIDEO_OUTPUT_FILENAME";*/


            String sVideoFinalDestinationFolder = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath() +
                    File.separator + ciNew.sFolder_Name;
            String sVideoWorkingFolder = sVideoFinalDestinationFolder + File.separator + ciNew.sItemID;
            long[] lDownloadIDs = new long[allDownloadIDs.size()];
            for(int i = 0; i < allDownloadIDs.size(); i++){
                lDownloadIDs[i] = allDownloadIDs.get(i);
            }
            //Prepare the file sequence so that an M3U8 sequence can be concatenated properly.
            //String[] sFilenameSequence = new String[ciNew.alsDownloadURLsAndDestFileNames.size()];
            //int l = 0;
            //A file sequence string array can be too big to pass to a worker, so write it to a file:
            final String sDLIDFileSequenceFilePath = sVideoWorkingFolder +
                    File.separator + Worker_VideoPostProcessing.VIDEO_DLID_AND_SEQUENCE_FILE_NAME;
            final File fDLIDFileSequenceFile = new File(sDLIDFileSequenceFilePath);
            FileWriter fwDLIDFileSequenceFile;
            fwDLIDFileSequenceFile = new FileWriter(fDLIDFileSequenceFile, true);
            for(int l = 0; l < ciNew.alsDownloadURLsAndDestFileNames.size(); l++) {
                String sLine = lDownloadIDs[l] + "\t" + ciNew.alsDownloadURLsAndDestFileNames.get(l)[FILE_NAME_AND_EXTENSION] + "\n";
                fwDLIDFileSequenceFile.write(sLine);
            }
            fwDLIDFileSequenceFile.flush();
            fwDLIDFileSequenceFile.close();
            /*for(String[] sURLAndFileName: ciNew.alsDownloadURLsAndDestFileNames) {
                sFilenameSequence[l] =  sURLAndFileName[FILE_NAME_AND_EXTENSION];
                l++;
            }*/
            //Build-out data to send to the worker:
            Data dataVideoPostProcessor = new Data.Builder()
                    .putInt(Worker_VideoPostProcessing.KEY_ARG_DOWNLOAD_TYPE_SINGLE_M3U8_M3U8LOCAL, iSingleOrM3U8)
                    .putString(Worker_VideoPostProcessing.KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS, sVideoDownloadFolder)
                    .putString(Worker_VideoPostProcessing.KEY_ARG_WORKING_FOLDER, sVideoWorkingFolder)
                    //.putStringArray(Worker_VideoPostProcessing.KEY_ARG_FILENAME_SEQUENCE, sFilenameSequence)
                    .putString(Worker_VideoPostProcessing.KEY_ARG_VIDEO_OUTPUT_FILENAME, GlobalClass.JumbleFileName(ciNew.sFilename)) //Double-jumble.
                    .putLong(Worker_VideoPostProcessing.KEY_ARG_VIDEO_TOTAL_FILE_SIZE, ciNew.lSize)
                    //.putLongArray(Worker_VideoPostProcessing.KEY_ARG_DOWNLOAD_IDS, lDownloadIDs)
                    .putString(Worker_VideoPostProcessing.KEY_ARG_ITEM_ID, ciNew.sItemID)
                    .build();
            OneTimeWorkRequest otwrVideoPostProcessor = new OneTimeWorkRequest.Builder(Worker_VideoPostProcessing.class)
                    .setInputData(dataVideoPostProcessor)
                    .addTag(Worker_VideoPostProcessing.WORKER_VIDEO_POST_PROCESSING_TAG) //To allow finding the worker later.
                    .build();
            UUID UUIDWorkID = otwrVideoPostProcessor.getId();
            WorkManager.getInstance(getApplicationContext()).enqueue(otwrVideoPostProcessor);

            String sMessage = "Operation complete.\n";
            if(icfDownloadItem.iTypeFileFolderURL == ItemClass_File.TYPE_URL){
                sMessage = sMessage + "The download will continue in the background and will be available once completed.\n";
            } else {
                sMessage = sMessage + "M3U8 video downloads will continue in the background and will be concatenated into a single video once completed.\n";
            }
            //Check to see if the user has applied a restricted tag so that an appropriate reminder
            //  message can be shown:
            boolean bRestrictedTagInUse = false;
            String sRecordTags = ciNew.sTags;
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
                    ItemClass_Tag ict = globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).get(globalClass.getTagTextFromID(iTagID, GlobalClass.MEDIA_CATEGORY_VIDEOS));
                    if (ict != null) {
                        if (ict.bIsRestricted) {
                            bRestrictedTagInUse = true;
                            break;
                        }
                    }
                }
            }
            if(bRestrictedTagInUse) {
                sMessage = sMessage + "The item will not be readily shown in the catalog due to the application of a restricted tag.\n";
            }

            BroadcastProgress(true, sMessage,
                    true, iProgressBarValue,
                    true, "All downloads started",
                    sIntentActionFilter);

        } catch (Exception e) {
            if(e.getMessage() != null) {
                Log.e("Error: ", e.getMessage());
            }
            BroadcastProgress(true, "Problem encountered:\n" + e.getMessage(),
                    false, iProgressBarValue,
                    true, "Operation halted.",
                    sIntentActionFilter);
        }

        //Modify viewer settings to show the newly-imported files:
        /*globalClass.giCatalogViewerSortBySetting[iMediaCategory] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        globalClass.gbCatalogViewerSortAscending[iMediaCategory] = false;*/ //Handled in onHandleIntent.

    }

    //====== Delete files (user can delete files while selecting imports) ==========================

    private void handleAction_startActionDeleteFiles( ArrayList<String> alsUriFilesToDelete, String sCallerActionResponseFilter){

        int iProgressNumerator = 0;
        int iProgressDenominator = alsUriFilesToDelete.size();
        int iProgressBarValue = 0;

        Uri uriSourceFile;
        DocumentFile dfSource;
        String sLogLine;
        boolean bDisplayLogMessage;
        String sProgressBarText;
        for(String sUriFileToDelete: alsUriFilesToDelete) {
            bDisplayLogMessage = false;
            sLogLine = FILE_DELETION_MESSAGE + sUriFileToDelete + "...";
            uriSourceFile = Uri.parse(sUriFileToDelete);
            dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);

            if (dfSource != null) {
                try {
                    if (dfSource.delete()) {
                        sLogLine = sLogLine + "Success.";
                    } else {
                        sLogLine = sLogLine + "Failed.";
                        bDisplayLogMessage = true;
                    }
                } catch (Exception e){
                  sLogLine = sLogLine + e.getMessage();
                    bDisplayLogMessage = true;
                }
            }

            iProgressNumerator++;
            sProgressBarText = "File " + iProgressNumerator + " of " + iProgressDenominator + " deleted.";
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            BroadcastProgress(bDisplayLogMessage, sLogLine,
                    true, iProgressBarValue,
                    true, sProgressBarText,
                    sCallerActionResponseFilter);
        }

        BroadcastProgress(false, FILE_DELETION_OP_COMPLETE_MESSAGE,
                true, 100,
                false, "",
                sCallerActionResponseFilter);

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

    private static boolean isPageDuplicate(String sAbsolutePath){

        File f = new File(sAbsolutePath);
        String sFileName = f.getName();

        boolean bEndsWithNumberInParenthesis;
        bEndsWithNumberInParenthesis = sFileName.matches("^\\d{1,7}_.+\\(\\d{1,2}\\)\\.\\w{3,4}$");

        if (bEndsWithNumberInParenthesis) {
            String sOriginalFileAbsolutePath;

            sOriginalFileAbsolutePath = sAbsolutePath.substring(0,sAbsolutePath.lastIndexOf("(")) + sAbsolutePath.substring(sAbsolutePath.lastIndexOf("."));
            File fFileCheck = new File(sOriginalFileAbsolutePath);

            return fFileCheck.exists();

        }
        return false;
    }

    public String cleanFileNameViaTrim(String sFilename){
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

    public String cleanFileNameViaReplace(String sFilename, String sReplaceChar){
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
            sOutput = sOutput.replace(sReservedChar, sReplaceChar);
        }

        return sOutput;
    }

    public static String cleanHTMLCodedCharacters(String sInput){

        String sOutput = Html.fromHtml(sInput,0).toString();
        return sOutput;

    }




    //==============================================================================================
    //===== Service Communication Utilities ========================================================
    //==============================================================================================


    void problemNotificationConfig(String sMessage, String sIntentActionFilter){
        Intent broadcastIntent_Problem = new Intent();
        broadcastIntent_Problem.setAction(sIntentActionFilter);
        broadcastIntent_Problem.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent_Problem.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Problem.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Problem);
    }

    public static final String UPDATE_LOG_BOOLEAN = "UPDATE_LOG_BOOLEAN";
    public static final String LOG_LINE_STRING = "LOG_LINE_STRING";
    public static final String UPDATE_PERCENT_COMPLETE_BOOLEAN = "UPDATE_PERCENT_COMPLETE_BOOLEAN";
    public static final String PERCENT_COMPLETE_INT = "PERCENT_COMPLETE_INT";
    public static final String UPDATE_PROGRESS_BAR_TEXT_BOOLEAN = "UPDATE_PROGRESS_BAR_TEXT_BOOLEAN";
    public static final String PROGRESS_BAR_TEXT_STRING = "PROGRESS_BAR_TEXT_STRING";
    public static final String RECEIVER_STRING = "RECEIVER_STRING";

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

    public void BroadcastProgress_ComicDetails(String sLogLine, String sIntentActionFilter){

        BroadcastProgress(true, sLogLine,
        false, 0,
        false, "",
                sIntentActionFilter);
                //Fragment_Import_5a_WebComicConfirmation.ImportDataServiceResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
    }

}