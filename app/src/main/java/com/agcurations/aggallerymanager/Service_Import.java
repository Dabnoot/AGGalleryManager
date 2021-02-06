package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Service_Import extends IntentService {

    private static final String ACTION_GET_DIRECTORY_CONTENTS = "com.agcurations.aggallerymanager.action.GET_DIRECTORY_CONTENTS";
    private static final String ACTION_IMPORT_FILES = "com.agcurations.aggallerymanager.action.IMPORT_FILES";
    private static final String ACTION_IMPORT_COMICS = "com.agcurations.aggallerymanager.action.IMPORT_COMICS";
    private static final String ACTION_GET_COMIC_DETAILS_ONLINE = "com.agcurations.aggallerymanager.action.GET_COMIC_DETAILS_ONLINE";
    private static final String ACTION_IMPORT_COMIC_WEB_FILES = "com.agcurations.aggallerymanager.action.IMPORT_COMIC_WEB_FILES";

    private static final String EXTRA_IMPORT_TREE_URI = "com.agcurations.aggallerymanager.extra.IMPORT_TREE_URI";
    private static final String EXTRA_MEDIA_CATEGORY = "com.agcurations.aggallerymanager.extra.MEDIA_CATEGORY";
    private static final String EXTRA_FILES_OR_FOLDERS = "com.agcurations.aggallerymanager.extra.EXTRA_FILES_OR_FOLDERS";

    private static final String EXTRA_COMIC_IMPORT_SOURCE = "com.agcurations.aggallerymanager.extra.COMIC_IMPORT_SOURCE";

    private static final String EXTRA_IMPORT_FILES_FILELIST = "com.agcurations.aggallerymanager.extra.IMPORT_FILES_FILELIST";
    private static final String EXTRA_IMPORT_FILES_MOVE_OR_COPY = "com.agcurations.aggallerymanager.extra.IMPORT_FILES_MOVE_OR_COPY";

    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";

    public static final String EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_DIRECTORY_CONTENTS_RESPONSE"; //Used to flag in a listener that this is or is not a response to dir call.
    public static final String EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_DIRECTORY_CONTENTS_RESPONSE"; //ArrayList of response data

    public static final String EXTRA_STRING_WEB_ADDRESS = "com.agcurations.aggallerymanager.extra.STRING_WEB_ADDRESS";
    public static final String COMIC_DETAILS_SUCCESS = "COMIC_DETAILS_SUCCESS";
    public static final String COMIC_DETAILS_ERROR_MESSAGE = "COMIC_DETAILS_ERROR_MESSAGE";
    public static final String COMIC_CATALOG_ITEM = "COMIC_CATALOG_ITEM";

    public static final String RECEIVER_STORAGE_LOCATION = "com.agcurations.aggallerymanager.extra.RECEIVER_STORAGE_LOCATION";
    public static final String RECEIVER_EXECUTE_IMPORT = "com.agcurations.aggallerymanager.extra.RECEIVER_EXECUTE_IMPORT";
    public static final String RECEIVER_ACTIVITY_IMPORT = "com.agcurations.aggallerymanager.extra.RECEIVER_ACTIVITY_IMPORT";

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

    public static void startActionImportFiles(Context context,
                                              ArrayList<ItemClass_File> alImportFileList,
                                              int iMoveOrCopy,
                                              int iMediaCategory) {
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_FILES);
        intent.putExtra(EXTRA_IMPORT_FILES_FILELIST, alImportFileList);
        intent.putExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy);
        intent.putExtra(EXTRA_MEDIA_CATEGORY, iMediaCategory);
        context.startService(intent);
    }

    public static void startActionImportNHComicsFiles(Context context,
                                                      ArrayList<ItemClass_File> alImportFileList,
                                                      int iMoveOrCopy,
                                                      int iComicImportSource) {
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_COMICS);
        intent.putExtra(EXTRA_IMPORT_FILES_FILELIST, alImportFileList);
        intent.putExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy);
        intent.putExtra(EXTRA_COMIC_IMPORT_SOURCE, iComicImportSource);
        context.startService(intent);
    }

    public static void startActionImportNHComicsDetails(Context context,
                                                       String sAddress){
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_GET_COMIC_DETAILS_ONLINE);
        intent.putExtra(EXTRA_STRING_WEB_ADDRESS, sAddress);
        context.startService(intent);
    }

    public static void startActionImportComicWebFiles(Context context,
                                                        ItemClass_CatalogItem ci){
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_IMPORT_COMIC_WEB_FILES);
        intent.putExtra(COMIC_CATALOG_ITEM, ci);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_GET_DIRECTORY_CONTENTS.equals(action)) {
                final String sImportTreeUri = intent.getStringExtra(EXTRA_IMPORT_TREE_URI);
                Uri uriImportTreeUri = Uri.parse(sImportTreeUri);
                final int iMediaCategory = intent.getIntExtra(EXTRA_MEDIA_CATEGORY,-1);
                final int iFilesOrFolders = intent.getIntExtra(EXTRA_FILES_OR_FOLDERS, FILES_ONLY);
                final int iComicImportSource = intent.getIntExtra(EXTRA_COMIC_IMPORT_SOURCE, -1);
                handleAction_GetDirectoryContents(uriImportTreeUri, iMediaCategory, iFilesOrFolders, iComicImportSource);

            } else if (ACTION_IMPORT_FILES.equals(action)) {
                final ArrayList<ItemClass_File> alFileList = (ArrayList<ItemClass_File>) intent.getSerializableExtra(EXTRA_IMPORT_FILES_FILELIST);
                final int iMoveOrCopy = intent.getIntExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
                final int iMediaCategory = intent.getIntExtra(EXTRA_MEDIA_CATEGORY, -1);
                if(alFileList==null) return;
                handleAction_startActionImportFiles(
                        alFileList,
                        iMoveOrCopy,
                        iMediaCategory);

            } else if (ACTION_IMPORT_COMICS.equals(action)) {
                final ArrayList<ItemClass_File> alFileList = (ArrayList<ItemClass_File>) intent.getSerializableExtra(EXTRA_IMPORT_FILES_FILELIST);
                final int iMoveOrCopy = intent.getIntExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
                final int iComicImportSource = intent.getIntExtra(EXTRA_COMIC_IMPORT_SOURCE, -1);
                if(alFileList==null) return;
                handleAction_startActionImportNHComics(
                        alFileList,
                        iMoveOrCopy
                );

            } else if (ACTION_GET_COMIC_DETAILS_ONLINE.equals(action)) {
                final String sAddress = intent.getStringExtra(EXTRA_STRING_WEB_ADDRESS);
                handleAction_startActionGetComicDetailsOnline(sAddress);

            } else if (ACTION_IMPORT_COMIC_WEB_FILES.equals(action)) {
                final ItemClass_CatalogItem ci = (ItemClass_CatalogItem) intent.getSerializableExtra(COMIC_CATALOG_ITEM);
                try {
                    handleAction_startActionImportComicWebFiles(ci);
                } catch (IOException e) {
                    e.printStackTrace();
                    problemNotificationConfig(e.getMessage(), RECEIVER_EXECUTE_IMPORT);  //todo: make sure that this is properly handled in Execute_Import.
                }

            }
        }
    }



    //==============================================================================================
    //===== Service Content ========================================================================
    //==============================================================================================



    public static final int FOLDERS_ONLY = 0;
    public static final int FILES_ONLY = 1;

    private void handleAction_GetDirectoryContents(Uri uriImportTreeUri, int iMediaCategory, int iFilesOrFolders, int iComicImportSource) {
        if(Activity_Import.guriImportTreeURI != null){
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

                    long lProgressNumerator = 0L;
                    long lProgressDenominator;
                    int iProgressBarValue = 0;

                    //Calculate total number of files for a progress bar:
                    lProgressDenominator = cImport.getCount();

                    if((iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) &&
                            (iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER)) {
                        //If we are importing comics by the folder, calculate a new progressbar denominator.
                        //Go through the folders in the users' chosen directory and count the files.
                        //  Each will be processed, and add a tick to the progressbar.

                        cImport.moveToPosition(-1);
                        while (cImport.moveToNext()) {
                            String sSubFolderDocID = cImport.getString(0);
                            String sSubFolderName = cImport.getString(1); //For debugging.
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
                        }
                    }

                    BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "0/" + lProgressDenominator,
                            RECEIVER_STORAGE_LOCATION);


                    MediaMetadataRetriever mediaMetadataRetriever;
                    mediaMetadataRetriever = new MediaMetadataRetriever();

                    int iTemp = cImport.getCount();

                    cImport.moveToPosition(-1);
                    while (cImport.moveToNext()) {

                        //Update progress bar:
                        //Update progress right away in order to avoid instances in which a loop is skipped.
                        lProgressNumerator++;
                        iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                        BroadcastProgress(false, "",
                                true, iProgressBarValue,
                                true, lProgressNumerator + "/" + lProgressDenominator,
                                RECEIVER_STORAGE_LOCATION);


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


                        long lFileSize = 0;
                        String fileExtension = "";
                        //If the file is video, get the duration so that the file list can be sorted by duration if requested.
                        long lDurationInMilliseconds = -1L;
                        String sWidth = "";  //We are not doing math with the width and height. Therefore no need to convert to int.
                        String sHeight = "";

                        //Get a Uri for this individual document:
                        final Uri docUri = DocumentsContract.buildDocumentUriUsingTree(childrenUri, docId);

                        //Get date last modified:
                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.setTimeInMillis(lLastModified);
                        Date dateLastModified = cal.getTime();


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
                                    lDurationInMilliseconds = Long.parseLong(time);
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
                                            problemNotificationConfig(e.getMessage() + "\n" + docName, RECEIVER_STORAGE_LOCATION);
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
                                //If the user is importing comics by the folder, not necessarily NHComics...








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
                                        iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                                        BroadcastProgress(false, "",
                                                true, iProgressBarValue,
                                                true, lProgressNumerator + "/" + lProgressDenominator,
                                                RECEIVER_STORAGE_LOCATION);

                                        //Analyze the file item.

                                        //If this file is a folder, skip to the next item:
                                        final String sComicPageMimeType = cComicPages.getString(2);
                                        boolean bComicPageIsDirectory;
                                        bComicPageIsDirectory = (sComicPageMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR));
                                        int iComicPageItemFileType = (bComicPageIsDirectory) ? ItemClass_File.TYPE_FOLDER : ItemClass_File.TYPE_FILE;
                                        if((iComicPageItemFileType != ItemClass_File.TYPE_FILE) || !sComicPageMimeType.contains("image")){
                                            continue;
                                        }


                                        //Build a ItemClass_File item for this file:
                                        final String sComicPageFilename = cComicPages.getString(1);
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
                                        Date dateComicPageLastModified = cal.getTime();
                                        icf_ComicPage.dateLastModified = dateComicPageLastModified;

                                        //Record the file extension:
                                        fileExtension = sComicPageFilename.contains(".") ? sComicPageFilename.substring(sComicPageFilename.lastIndexOf(".")) : "";
                                        icf_ComicPage.sExtension = fileExtension;

                                        //Record the file size:
                                        final String sComicPageFileSize = cComicPages.getString(4);
                                        lFileSize = Long.parseLong(sComicPageFileSize); //size in Bytes
                                        icf_ComicPage.lSizeBytes = lFileSize;

                                        //Get the image dimensions:
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
                                                    String sTest = sComicPageFilename.substring(iDigitBlock[0], iDigitBlock[1]);
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
                                    String sUri = docUri.toString();
                                    icf_ComicFolderItem.sUri = sUri;

                                    icf_ComicFolderItem.dateLastModified = dateLastModified;

                                    //All of the comic pages in this directory have been added to an
                                    //  arraylist of file items. Now look for a page numbering pattern:

                                    //Verify that all pages have the same quantity of number blocks (telling where the page number might be in the filename):
                                    boolean bQtyNumBlocksOk = false;
                                    int iNumBlocks = -1;
                                    for(ItemClass_File file: alicf_ComicFiles){
                                        if(iNumBlocks == -1){  //If this is the first set of number blocks we are testing, record the count.
                                            iNumBlocks = file.aliNumberBlocks.size();
                                            bQtyNumBlocksOk = true;
                                        } else {
                                            if(iNumBlocks != file.aliNumberBlocks.size()){  //Compare the count of number blocks.
                                                bQtyNumBlocksOk = false;
                                                //Report a problem with this file:
                                                problemNotificationConfig(
                                                        "Problem identifying page number for comic in folder \"" +
                                                                docName + "\", file \"" + file.sFileOrFolderName +
                                                                "\". Import will default to the less reliable alphabetized page sequencing for this comic.", RECEIVER_STORAGE_LOCATION);
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
                                        int iAcceptedNumberBlock = -1;
                                        for(int iNumberBlock = 0; iNumberBlock < iNumBlocks; iNumberBlock++) {
                                            tmComicFilesPageNumCheck = new TreeMap<>(); //Reset the file ordering.

                                            boolean bPossibleNumberBlockCandidate = true;
                                            for (ItemClass_File icfComicPage : alicf_ComicFiles) {
                                                //Attempt to get the integer value from this filename using the current number block:
                                                String sPageID  = icfComicPage.sFileOrFolderName.substring(icfComicPage.aliNumberBlocks.get(iNumberBlock)[0], icfComicPage.aliNumberBlocks.get(iNumberBlock)[1]);
                                                Integer iPageID = Integer.parseInt(sPageID);
                                                //Check to see if this page number already exists in the TreeMap:
                                                if(tmComicFilesPageNumCheck.containsKey(iPageID)){
                                                    //If the page number already exists in the TreeMap, this cannot be the \
                                                    //  page number. Exit the loop and attempt to test the next number block;
                                                    bPossibleNumberBlockCandidate = false;
                                                    break;
                                                }
                                                tmComicFilesPageNumCheck.put(iPageID, icfComicPage.sUri);
                                            }
                                            if(bPossibleNumberBlockCandidate){
                                                //If all of the found possible page IDs for this comic are unique,
                                                //  now test the pages for order:
                                                int iOrder = 1;
                                                //tmComicFilesPageNumCheck should have the pages sorted by the key, the page number based
                                                //  on the current number block in the filename.
                                                for(Map.Entry<Integer, String> tmPageEntry: tmComicFilesPageNumCheck.entrySet()){
                                                    int iThisPageID = tmPageEntry.getKey();
                                                    if(iThisPageID > 9999){
                                                        //This app is not designed to work with pages greater than 9999.
                                                        //  The comic page viewer will not properly sort the 10,000th page
                                                        //  alphabetically. Just assume there are no comics out there with more than 9999 pages.
                                                        break; //Go investigate the next number block.
                                                    }
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
                problemNotificationConfig(e.getMessage(), RECEIVER_STORAGE_LOCATION);
                return;
            }

            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, true);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE, alFileList);

            broadcastIntent_GetDirectoryContentsResponse.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
            broadcastIntent_GetDirectoryContentsResponse.addCategory(Intent.CATEGORY_DEFAULT);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(RECEIVER_STRING, RECEIVER_ACTIVITY_IMPORT);
            //sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);
        }
    }

    private void handleAction_startActionImportFiles(ArrayList<ItemClass_File> alFileList, int iMoveOrCopy, int iMediaCategory) {


        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;
        long lLoopBytesRead;


        ContentResolver contentResolver = getApplicationContext().getContentResolver();

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        //Calculate total size of all files to import:
        for(ItemClass_File fi: alFileList){
            lTotalImportSize = lTotalImportSize + fi.lSizeBytes;
        }
        lProgressDenominator = lTotalImportSize;

        //Find the next record ID:
        int iNextRecordId = 0;
        int iThisId;
        for (Map.Entry<String, ItemClass_CatalogItem> entry : globalClass.gtmCatalogLists.get(iMediaCategory).entrySet()) {
            iThisId = Integer.parseInt(entry.getValue().sItemID);
            if (iThisId >= iNextRecordId) iNextRecordId = iThisId + 1;
        }
        //New record ID identified.

        //Loop and import files:
        for(ItemClass_File fileItem: alFileList) {

            if(fileItem.sDestinationFolder.equals("")) {
                fileItem.sDestinationFolder = GlobalClass.gsUnsortedFolderName;
            }

            File fDestination = new File(
                    globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                            fileItem.sDestinationFolder);

            if (!fDestination.exists()) {
                if (!fDestination.mkdir()) {
                    //Unable to create directory
                    BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            true, "Operation halted.",
                            RECEIVER_EXECUTE_IMPORT);
                    return;
                } else {
                    BroadcastProgress(true, "Destination folder created: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            false, "",
                            RECEIVER_EXECUTE_IMPORT);
                }
            } else {
                BroadcastProgress(true, "Destination folder verified: " + fDestination.getPath() + "\n",
                        true, iProgressBarValue,
                        false, "",
                        RECEIVER_EXECUTE_IMPORT);
            }

            Uri uriSourceFile;
            String sLogLine;
            InputStream inputStream;
            OutputStream outputStream;

            uriSourceFile = Uri.parse(fileItem.sUri);
            DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
            if (dfSource == null) continue;
            if (dfSource.getName() == null) continue;

            try {
                //Write next behavior to the screen log:
                sLogLine = "Attempting ";
                if (iMoveOrCopy == ViewModel_ImportActivity.IMPORT_METHOD_MOVE) {
                    sLogLine = sLogLine + "move ";
                } else {
                    sLogLine = sLogLine + "copy ";
                }
                sLogLine = sLogLine + "of file " + fileItem.sFileOrFolderName + " to destination...";
                BroadcastProgress(true, sLogLine,
                        false, iProgressBarValue,
                        true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                        RECEIVER_EXECUTE_IMPORT);

                //Reverse the text on the file so that the file does not get picked off by a search tool:
                String sFileName = GlobalClass.JumbleFileName(dfSource.getName());
                File fDestinationFolder = new File(fDestination.getPath());

                boolean bCopyViaStream = false;
                /*String sDestinationFolder = "content://com.android.externalstorage.documents/tree/0000-0000%3AAndroid%2Fdata%2Fcom.agcurations.aggallerymanager%2Ffiles%2FVideos%2F7";
                Uri uriDestinationFolder = Uri.parse(sDestinationFolder);
                URI uri1 = URI.create(sDestinationFolder);



                //First, attempt to transfer the document using DocumentsContract:
                Uri uriTransferredDocument = null;
                DocumentFile dfParentFile = dfSource.getParentFile();

                //if(dfParentFile != null) {
                    if (iMoveOrCopy == ViewModel_ImportActivity.IMPORT_METHOD_MOVE) {
                        //Attempt to move the document using DocumentsContract.moveDocument:
                        uriTransferredDocument = DocumentsContract.moveDocument(
                                getContentResolver(),
                                dfSource.getUri(),
                                dfSource.getUri(),
                                uriDestinationFolder);
                    } else if (iMoveOrCopy == ViewModel_ImportActivity.IMPORT_METHOD_COPY) {
                        //Attempt to move the document using DocumentsContract.moveDocument:
                        uriTransferredDocument = DocumentsContract.copyDocument(
                                getContentResolver(),
                                dfSource.getUri(),
                                uriDestinationFolder);
                    }
                //}
                if(uriTransferredDocument == null){
                    bCopyViaStream = true; //If the document move failed... copy via stream.
                } else {
                    //If DocumentsContract transfer succeeded, rename the file to the Jumbled filename:
                    DocumentFile dfMovedFile = DocumentFile.fromSingleUri(getApplicationContext(), uriTransferredDocument);
                    if(!dfMovedFile.renameTo(sFileName)) {
                        BroadcastProgress(true, "Problem renaming transferred file.",
                                false, iProgressBarValue,
                                false, "",
                                RECEIVER_EXECUTE_IMPORT);
                    }
                    lProgressNumerator += fileItem.sizeBytes;
                }*/
                bCopyViaStream = true;

                //If DocumentsContract transferred failed, try to copy via stream:
                if(bCopyViaStream) {
                    inputStream = contentResolver.openInputStream(dfSource.getUri());

                    outputStream = new FileOutputStream(fDestinationFolder.getPath() + File.separator + sFileName);
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
                                    RECEIVER_EXECUTE_IMPORT);
                        }
                    }
                    outputStream.flush();
                    outputStream.close();
                }

                if (iMoveOrCopy == ViewModel_ImportActivity.IMPORT_METHOD_MOVE) {
                    if(bCopyViaStream) {
                        sLogLine = "Copy success.";
                        if (!dfSource.delete()) {
                            sLogLine = sLogLine + "\nCould not delete source file after copy (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                        } else {
                            sLogLine = sLogLine + "\nSuccess deleting source file after copy.\n";
                        }
                    } else {
                        sLogLine = "Move success.";
                    }
                } else {
                    sLogLine = "Copy success.";
                }


                //Update the progress bar for the file copy:
                iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                BroadcastProgress(true, sLogLine,
                        false, iProgressBarValue,
                        true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                        RECEIVER_EXECUTE_IMPORT);

                //This file has now been copied.
                //Next add the data to the catalog file and memory:

                //Create a timestamp to be used to create the data record:
                Double dTimeStamp = GlobalClass.GetTimeStampFloat();

                ItemClass_CatalogItem ciNew = new ItemClass_CatalogItem();
                ciNew.iMediaCategory = iMediaCategory;
                ciNew.sItemID = String.valueOf(iNextRecordId);
                ciNew.sFilename = sFileName;
                ciNew.lSize = fileItem.lSizeBytes;
                ciNew.lDuration_Milliseconds = fileItem.lVideoTimeInMilliseconds;
                ciNew.sDuration_Text = fileItem.sVideoTimeText;
                ciNew.iWidth = Integer.parseInt(fileItem.sWidth);
                ciNew.iHeight = Integer.parseInt(fileItem.sHeight);
                ciNew.sFolder_Name = fileItem.sDestinationFolder;
                ciNew.sTags = GlobalClass.formDelimitedString(fileItem.aliProspectiveTags, ",");
                ciNew.dDatetime_Last_Viewed_by_User = dTimeStamp;
                ciNew.dDatetime_Import = dTimeStamp;

                //The below call should add the record to both the catalog contents file
                //  and memory:
                globalClass.CatalogDataFile_CreateNewRecord(ciNew);


                iNextRecordId += 1; //Identify the next record ID to assign.


            } catch (Exception e) {
                BroadcastProgress(true, "Problem with copy/move operation.\n" + e.getMessage(),
                        false, iProgressBarValue,
                        false, "",
                        RECEIVER_EXECUTE_IMPORT);
            }


        }

        //Modify viewer settings to show the newly-imported files:
        globalClass.giCatalogViewerSortBySetting[iMediaCategory] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        globalClass.gbCatalogViewerSortAscending[iMediaCategory] = false;

        BroadcastProgress(true, "Operation complete.",
                true, iProgressBarValue,
                true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                RECEIVER_EXECUTE_IMPORT);

    }


    //====== Comic Routines ========================================================================

    private void handleAction_startActionImportNHComics(ArrayList<ItemClass_File> alFileList, int iMoveOrCopy) {


        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;
        long lLoopBytesRead;


        ContentResolver contentResolver = getApplicationContext().getContentResolver();

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        //Calculate total size of all files to import:
        for(ItemClass_File fi: alFileList){
            lTotalImportSize = lTotalImportSize + fi.lSizeBytes;
        }
        lProgressDenominator = lTotalImportSize;

        //Find the next record ID:
        int iNextRecordId = 0;
        int iThisId;
        for (Map.Entry<String, ItemClass_CatalogItem> entry : globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()) {
            iThisId = Integer.parseInt(entry.getValue().sItemID);
            if (iThisId >= iNextRecordId) iNextRecordId = iThisId + 1;
        }
        //New record ID identified.

        TreeMap<String, String[]> tmNHComicIDs = new TreeMap<>(); //Map NH_Comic_Downloader ComicID to a record ID/folder and also grab the comic title.

        //If NH_Comic_Downloaded, loop and find all of the comic IDs:
        for(ItemClass_File fileItem: alFileList) {
            if(fileItem.sFileOrFolderName.matches(GlobalClass.gsNHComicCoverPageFilter)){
                String sComicID = GetNHComicID(fileItem.sFileOrFolderName);
                lProgressDenominator = lProgressDenominator - fileItem.lSizeBytes; //We don't copy over the cover page.
                String sRecordID = iNextRecordId + "";
                iNextRecordId++;
                String sComicName = GetNHComicNameFromCoverFile(fileItem.sFileOrFolderName);
                String sComicTags = GlobalClass.formDelimitedString(fileItem.aliProspectiveTags, ",");
                if(tmNHComicIDs.containsKey(sComicID)){
                    //If this is merely a duplicate comic selected during the import, not if it already exists in the catalog.
                    //If it already exists in the catalog, it is on the user to resolve.
                    problemNotificationConfig("Skipping Comic ID " + sComicID + ". Duplicate comic.", RECEIVER_EXECUTE_IMPORT);
                } else {
                    tmNHComicIDs.put(sComicID, new String[]{sRecordID, sComicName, sComicTags});
                }
            }
        }

        for(Map.Entry<String, String[]> tmEntryNHComic: tmNHComicIDs.entrySet()) {
            //Loop and import files:

            String sDestinationFolder = tmEntryNHComic.getValue()[0]; //The individual comic folder is the comic ID.

            File fDestination = new File(
                    globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath() + File.separator +
                            sDestinationFolder);

            if (!fDestination.exists()) {
                if (!fDestination.mkdir()) {
                    //Unable to create directory
                    BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            true, "Operation halted.",
                            RECEIVER_EXECUTE_IMPORT);
                    return;
                } else {
                    BroadcastProgress(true, "Destination folder created: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            false, "",
                            RECEIVER_EXECUTE_IMPORT);
                }
            } else {
                BroadcastProgress(true, "Destination folder verified: " + fDestination.getPath() + "\n",
                        true, iProgressBarValue,
                        false, "",
                        RECEIVER_EXECUTE_IMPORT);
            }

            //Prepare the data record:
            ItemClass_CatalogItem ciNewComic = new ItemClass_CatalogItem();
            ciNewComic.iMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
            ciNewComic.sItemID = tmEntryNHComic.getValue()[0]; //The individual comic folder is the comic ID.
            ciNewComic.sComicName = tmEntryNHComic.getValue()[1]; //Get the name.
            ciNewComic.sTags = tmEntryNHComic.getValue()[2]; //Get the tags.
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
                        ciNewComic.iComic_File_Count++;
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
                            sLogLine = "Attempting ";
                            if (iMoveOrCopy == ViewModel_ImportActivity.IMPORT_METHOD_MOVE) {
                                sLogLine = sLogLine + "move ";
                            } else {
                                sLogLine = sLogLine + "copy ";
                            }
                            sLogLine = sLogLine + "of file " + fileItem.sFileOrFolderName + " to destination...";
                            BroadcastProgress(true, sLogLine,
                                    false, iProgressBarValue,
                                    true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                                    RECEIVER_EXECUTE_IMPORT);

                            if (dfSource == null) continue;
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
                                            RECEIVER_EXECUTE_IMPORT);
                                }
                            }
                            outputStream.flush();
                            outputStream.close();
                            sLogLine = "Copy success.\n";
                            iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                            BroadcastProgress(true, sLogLine,
                                    false, iProgressBarValue,
                                    true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                                    RECEIVER_EXECUTE_IMPORT);

                            //This file has now been copied.

                            //Delete the source file if 'Move' specified:
                            boolean bUpdateLogOneMoreTime = false;
                            if (iMoveOrCopy == ViewModel_ImportActivity.IMPORT_METHOD_MOVE) {
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
                                    RECEIVER_EXECUTE_IMPORT);


                        } catch (Exception e) {
                            BroadcastProgress(true, "Problem with copy/move operation.\n\n" + e.getMessage(),
                                    false, iProgressBarValue,
                                    false, "",
                                    RECEIVER_EXECUTE_IMPORT);
                        }

                    } else {
                        //NHComic page is a duplicate.
                        problemNotificationConfig("File " + fileItem.sFileOrFolderName + " appears to be a duplicate. Skipping import of this file.\n", RECEIVER_EXECUTE_IMPORT);
                    }

                } //End if match with Page regex containing ComicID.

            } //End NHComic Import Loop.

            //Next add the data to the catalog file and memory:
            //The below call should add the record to both the catalog contents file
            //  and memory:
            globalClass.CatalogDataFile_CreateNewRecord(ciNewComic);

            //Delete the cover page from source folder if required:
            if(icfCoverPageFile != null) {
                if (iMoveOrCopy == ViewModel_ImportActivity.IMPORT_METHOD_MOVE) {
                    Uri uriSourceFile;
                    String sLogLine;

                    uriSourceFile = Uri.parse(icfCoverPageFile.sUri);
                    DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);

                    if (!dfSource.delete()) {
                        sLogLine = "Could not delete cover page duplicate file (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                    } else {
                        sLogLine = "Success deleting cover page duplicate file.\n";
                    }
                    BroadcastProgress(true, sLogLine,
                            true, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            RECEIVER_EXECUTE_IMPORT);
                }
            }






        } //End NHComics (plural) Import Loop.





        //Modify viewer settings to show the newly-imported files:
        globalClass.giCatalogViewerSortBySetting[GlobalClass.MEDIA_CATEGORY_COMICS] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        globalClass.gbCatalogViewerSortAscending[GlobalClass.MEDIA_CATEGORY_COMICS] = false;

        BroadcastProgress(true, "Operation complete.\n",
                true, iProgressBarValue,
                true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                RECEIVER_EXECUTE_IMPORT);

    }

    private void handleAction_startActionGetComicDetailsOnline(String sAddress){

        //Broadcast a message to be picked-up by the Import Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Fragment_Import_5a_WebConfirmation.ImportDataServiceResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
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

        boolean bMissingComicPagesAcquired = false;

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

            String sHTML = a.toString();
            sHTML = sHTML.replaceAll("tag-container field-name ", "tag-container field-name");

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
            TagNode node = pageParser.clean(sHTML);


            //Attempt to get the comic title from the WebPage html:
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

            //Get the first thumbnail image for import preview:
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
            String sNHComicID = ci.sSource;
            sNHComicID = sNHComicID.substring(0, sNHComicID.lastIndexOf("/"));
            sNHComicID = sNHComicID.substring(sNHComicID.lastIndexOf("/") + 1);
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
                    if(ci.lSize > -1) { //-1 if we do this once and find out that the data is not in the header.
                        URL urlPage = new URL(sNHImageDownloadAddress);
                        URLConnection connection = url.openConnection();
                        //connection.connect();
                        connection.setRequestProperty("Accept-Encoding", "identity");
                        ci.lSize += connection.getContentLength(); //Returns -1 if content size is not in the header.
                    }
                }
            }
            if(alsImageNameData.size() > 0){
                //If there are image addresses to attempt to download...
                ci.alsComicPageURLsAndDestFileNames = alsImageNameData;
            }




        } catch(Exception e){
            String sMsg = e.getMessage();
            broadcastIntent.putExtra(COMIC_DETAILS_SUCCESS, false);
            broadcastIntent.putExtra(COMIC_DETAILS_ERROR_MESSAGE, sMsg);
            sendBroadcast(broadcastIntent);
            return;
        }


        ci.sComicName = sReturnData[COMIC_DETAILS_TITLE_INDEX];
        ci.sComicParodies = sReturnData[COMIC_DETAILS_PARODIES_DATA_INDEX];
        ci.sComicCharacters = sReturnData[COMIC_DETAILS_CHARACTERS_DATA_INDEX];
        ci.sTags = sReturnData[COMIC_DETAILS_TAGS_DATA_INDEX]; //NOTE: THESE ARE TEXTUAL TAGS, NOT TAG IDS.
        ci.sComicArtists = sReturnData[COMIC_DETAILS_ARTISTS_DATA_INDEX];
        ci.sComicGroups = sReturnData[COMIC_DETAILS_GROUPS_DATA_INDEX];
        ci.sComicLanguages = sReturnData[COMIC_DETAILS_LANGUAGES_DATA_INDEX];
        ci.sComicCategories = sReturnData[COMIC_DETAILS_CATEGORIES_DATA_INDEX];
        ci.iComicPages = Integer.parseInt(sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX]);


        //Apply booleans to the intent to tell the receiver success, data available,
        //  and set the data where appropriate:
        broadcastIntent.putExtra(COMIC_DETAILS_SUCCESS, true);
        broadcastIntent.putExtra(COMIC_CATALOG_ITEM, ci);

        //Log.d("Comics", "Finished downloading from " + ci.sSource);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }

    private void handleAction_startActionImportComicWebFiles(ItemClass_CatalogItem ci) throws IOException {

        long lProgressNumerator = 0L;
        long lProgressDenominator = ci.iComicPages;
        int iProgressBarValue = 0;

        ContentResolver contentResolver = getApplicationContext().getContentResolver();

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        //Determine the next Comic Catalog ID:
        //Find the next record ID:
        int iNextRecordId = 0;
        int iThisId;
        for (Map.Entry<String, ItemClass_CatalogItem> entry : globalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()) {
            iThisId = Integer.parseInt(entry.getValue().sItemID);
            if (iThisId >= iNextRecordId) iNextRecordId = iThisId + 1;
        }


        //Create the comic folder.
        ci.sItemID = String.valueOf(iNextRecordId);
        ci.sFolder_Name = ci.sItemID;

        File fDestination = new File(
                globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath() + File.separator +
                        ci.sFolder_Name);


        if (!fDestination.exists()) {
            if (!fDestination.mkdir()) {
                //Unable to create directory
                BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        RECEIVER_EXECUTE_IMPORT);
                return;
            } else {
                BroadcastProgress(true, "Destination folder created: " + fDestination.getPath() + "\n",
                        false, iProgressBarValue,
                        false, "",
                        RECEIVER_EXECUTE_IMPORT);
            }
        } else {
            BroadcastProgress(true, "Destination folder verified: " + fDestination.getPath() + "\n",
                    true, iProgressBarValue,
                    false, "",
                    RECEIVER_EXECUTE_IMPORT);
        }

        if(ci.alsComicPageURLsAndDestFileNames.size() > 0){
            //If there are image addresses to attempt to download...
            InputStream input = null;
            OutputStream output = null;
            try {

                //Download the files:
                for(String[] sData: ci.alsComicPageURLsAndDestFileNames) {

                    String sNewFilename = ci.sItemID + "_" +sData[1];
                    String sJumbledNewFileName = GlobalClass.JumbleFileName(sNewFilename);
                    String sNewFullPathFilename = fDestination.getPath() +
                            File.separator +
                            sJumbledNewFileName;

                    if(ci.sFilename.equals("")){
                        ci.sFilename = sJumbledNewFileName;
                        ci.sThumbnail_File = sJumbledNewFileName;
                    }

                    File fNewFile = new File(sNewFullPathFilename);

                    if(!fNewFile.exists()) {
                        // Output stream
                        output = new FileOutputStream(fNewFile.getPath());

                        byte[] data = new byte[1024];

                        BroadcastProgress(true, "Downloading: " + sData[0] + "...",
                                false, iProgressBarValue,
                                true, "Downloading files...",
                                RECEIVER_EXECUTE_IMPORT);

                        URL url = new URL(sData[0]);
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

                        lProgressNumerator++;

                        iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                        BroadcastProgress(true, " Complete.\n",
                                true, iProgressBarValue,
                                false, "",
                                RECEIVER_EXECUTE_IMPORT);
                    }



                }
                //Success downloading files.

                //Convert textual tags to numeric tags:
                //Form the tag integer array:
                String[] sTags = ci.sTags.split(", ");
                ArrayList<Integer> aliTags = new ArrayList<>();
                for(String sTag: sTags){
                    aliTags.add(globalClass.getTagIDFromText(sTag, GlobalClass.MEDIA_CATEGORY_COMICS));
                }
                //Look for any tags that could not be found:
                for(int i = 0; i < aliTags.size() - 1; i++){
                    if(aliTags.get(i) == -1){
                        //Create the tag:
                        if(!sTags[i].equals("")) {
                            int iTag = globalClass.TagDataFile_CreateNewRecord(sTags[i], GlobalClass.MEDIA_CATEGORY_COMICS);
                            if(iTag != -1){
                                aliTags.add(i, iTag); //Replace the -1 with the new TagID.
                            }
                        }
                    }
                }
                ci.sTags = GlobalClass.formDelimitedString(aliTags, ",");



                //Create a timestamp to be used to create the data record:
                Double dTimeStamp = GlobalClass.GetTimeStampFloat();
                ci.dDatetime_Last_Viewed_by_User = dTimeStamp;
                ci.dDatetime_Import = dTimeStamp;

                //The below call should add the record to both the catalog contents file
                //  and memory:
                globalClass.CatalogDataFile_CreateNewRecord(ci);




                BroadcastProgress(true, "Operation complete.",
                        true, iProgressBarValue,
                        false, "",
                        RECEIVER_EXECUTE_IMPORT);

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
                BroadcastProgress(true, "Problem encountered:\n" + e.getMessage(),
                        false, iProgressBarValue,
                        true, "Operation halted.",
                        RECEIVER_EXECUTE_IMPORT);
            } finally {
                if(output != null) {
                    output.close();
                }
                if(input != null) {
                    input.close();
                }
            }

        }

        //Modify viewer settings to show the newly-imported files:
        globalClass.giCatalogViewerSortBySetting[GlobalClass.MEDIA_CATEGORY_COMICS] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        globalClass.gbCatalogViewerSortAscending[GlobalClass.MEDIA_CATEGORY_COMICS] = false;

    }



    //==============================================================================================
    //===== Import Utilities =======================================================================
    //==============================================================================================

    public static String GetNHComicID(String sFileName){
        boolean bIsValidComicPage = true;
        int iComicIDDigitCount = 0;

        int iComicID = -1;
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

    //==============================================================================================
    //===== Service Communication Utilities ========================================================
    //==============================================================================================


    void problemNotificationConfig(String sMessage, String sTarget){
        Intent broadcastIntent_Problem = new Intent();
        broadcastIntent_Problem.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent_Problem.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent_Problem.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Problem.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        broadcastIntent_Problem.putExtra(RECEIVER_STRING, sTarget);
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
                                  String sReceiver){

        //Broadcast a message to be picked-up by the Import Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        broadcastIntent.putExtra(UPDATE_LOG_BOOLEAN, bUpdateLog);
        broadcastIntent.putExtra(LOG_LINE_STRING, sLogLine);
        broadcastIntent.putExtra(UPDATE_PERCENT_COMPLETE_BOOLEAN, bUpdatePercentComplete);
        broadcastIntent.putExtra(PERCENT_COMPLETE_INT, iAmountComplete);
        broadcastIntent.putExtra(UPDATE_PROGRESS_BAR_TEXT_BOOLEAN, bUpdateProgressBarText);
        broadcastIntent.putExtra(PROGRESS_BAR_TEXT_STRING, sProgressBarText);
        broadcastIntent.putExtra(RECEIVER_STRING, sReceiver);

        //sendBroadcast(broadcastIntent);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }
}