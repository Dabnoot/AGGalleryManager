package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import androidx.documentfile.provider.DocumentFile;
import androidx.loader.content.CursorLoader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.provider.DocumentsContract.getDocumentId;

public class Service_Import extends IntentService {

    private static final String ACTION_GET_DIRECTORY_CONTENTS = "com.agcurations.aggallerymanager.action.GET_DIRECTORY_CONTENTS";
    private static final String ACTION_IMPORT_FILES = "com.agcurations.aggallerymanager.action.IMPORT_FILES";
    private static final String ACTION_IMPORT_COMICS = "com.agcurations.aggallerymanager.action.IMPORT_COMICS";

    private static final String EXTRA_IMPORT_TREE_URI = "com.agcurations.aggallerymanager.extra.IMPORT_TREE_URI";
    private static final String EXTRA_MEDIA_CATEGORY = "com.agcurations.aggallerymanager.extra.MEDIA_CATEGORY";

    private static final String EXTRA_COMIC_IMPORT_SOURCE = "com.agcurations.aggallerymanager.extra.COMIC_IMPORT_SOURCE";

    private static final String EXTRA_IMPORT_FILES_FILELIST = "com.agcurations.aggallerymanager.extra.IMPORT_FILES_FILELIST";
    private static final String EXTRA_IMPORT_FILES_MOVE_OR_COPY = "com.agcurations.aggallerymanager.extra.IMPORT_FILES_MOVE_OR_COPY";

    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";

    public static final String EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_DIRECTORY_CONTENTS_RESPONSE"; //Used to flag in a listener that this is or is not a response to dir call.
    public static final String EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_DIRECTORY_CONTENTS_RESPONSE"; //ArrayList of response data

    public static final String RECEIVER_STORAGE_LOCATION = "com.agcurations.aggallerymanager.extra.RECEIVER_STORAGE_LOCATION";
    public static final String RECEIVER_EXECUTE_IMPORT = "com.agcurations.aggallerymanager.extra.RECEIVER_EXECUTE_IMPORT";

    public Service_Import() {
        super("ImportActivityDataService");
    }

    /**
     * Starts this service to perform actions with the given parameters. If
     * the service is already performing a task this action will be queued.
     */
    public static void startActionGetDirectoryContents(Context context, Uri uriImportTreeUri, int iMediaCategory) {
        Intent intent = new Intent(context, Service_Import.class);
        intent.setAction(ACTION_GET_DIRECTORY_CONTENTS);
        String sImportTreeUri = uriImportTreeUri.toString();
        intent.putExtra(EXTRA_IMPORT_TREE_URI, sImportTreeUri);
        intent.putExtra(EXTRA_MEDIA_CATEGORY, iMediaCategory);
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

    public static void startActionImportComics(Context context,
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

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_GET_DIRECTORY_CONTENTS.equals(action)) {
                final String sImportTreeUri = intent.getStringExtra(EXTRA_IMPORT_TREE_URI);
                Uri uriImportTreeUri = Uri.parse(sImportTreeUri);
                final int iMediaCategory = intent.getIntExtra(EXTRA_MEDIA_CATEGORY,-1);
                handleAction_GetDirectoryContents(uriImportTreeUri, iMediaCategory);

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
                handleAction_startActionImportComics(
                        alFileList,
                        iMoveOrCopy,
                        iComicImportSource);

            }
        }
    }


    void problemNotificationConfig(String sMessage){
        Intent broadcastIntent_Problem = new Intent();
        broadcastIntent_Problem.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent_Problem.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent_Problem.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Problem.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        broadcastIntent_Problem.putExtra(RECEIVER_STRING, Service_Import.RECEIVER_STORAGE_LOCATION);
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




    final int FOLDERS_ONLY = 0;
    final int FILES_ONLY = 1;

    private void handleAction_GetDirectoryContents(Uri uriImportTreeUri, int iMediaCategory) {
        if(Activity_Import.guriImportTreeURI != null){
            Intent broadcastIntent_GetDirectoryContentsResponse = new Intent();
            ArrayList<ItemClass_File> alFileList = new ArrayList<>();
            try {
                String sFileExtensionRegEx = ".+";
                int iSelectFoldersFilesOrBoth = FILES_ONLY;
                //iMediaCategory: Specify media category. -1 ignore, 0 video, >0 images.

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
                    BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "0/" + lProgressDenominator,
                            RECEIVER_STORAGE_LOCATION);


                    MediaMetadataRetriever mediaMetadataRetriever;
                    mediaMetadataRetriever = new MediaMetadataRetriever();

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
                        String fileType = (isDirectory) ? "folder" : "file";

                        if ((iSelectFoldersFilesOrBoth == FILES_ONLY) && (isDirectory)) {
                            continue; //skip the rest of the for loop for this item.
                        } else if ((iSelectFoldersFilesOrBoth == FOLDERS_ONLY) && (!isDirectory)) {
                            continue; //skip the rest of the for loop for this item.
                        }

                        //Record the file extension:
                        String fileExtension = docName.contains(".") ? docName.substring(docName.lastIndexOf(".")) : "";
                        //If the file extension does not match the file extension regex, skip the remainder of the loop.
                        if (!fileExtension.matches(sFileExtensionRegEx)) {
                            continue;  //skip the rest of the loop if the file extension does not match.
                        }

                        if (!isDirectory) {
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
                        }

                        //Get a Uri for this individual document:
                        final Uri docUri = DocumentsContract.buildDocumentUriUsingTree(childrenUri, docId);

                        //Determine the file size:
                        long lFileSize = Long.parseLong(sFileSize); //size in Bytes

                        //Get date last modified:
                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.setTimeInMillis(lLastModified);
                        Date dateLastModified = cal.getTime();


                        //If the file is video, get the duration so that the file list can be sorted by duration if requested.
                        long lDurationInMilliseconds = -1L;
                        String sWidth = "";  //We are not doing math with the width and height. Therefore no need to convert to int.
                        String sHeight = "";
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
                                        problemNotificationConfig(e.getMessage() + "\n" + docName);
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
                        //Don't detect the duration of video files here. It could take too much time.
                        ItemClass_File fileItem = new ItemClass_File(fileType, docName, fileExtension, lFileSize, dateLastModified, sWidth, sHeight, false, sUri, mimeType, lDurationInMilliseconds);

                        //Add the file model to the ArrayList:
                        alFileList.add(fileItem);


                    }
                    cImport.close();
                    mediaMetadataRetriever.release();

                }



            }catch (Exception e){
                problemNotificationConfig(e.getMessage());
                return;
            }
            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, true);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE, alFileList);

            broadcastIntent_GetDirectoryContentsResponse.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
            broadcastIntent_GetDirectoryContentsResponse.addCategory(Intent.CATEGORY_DEFAULT);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(RECEIVER_STRING, RECEIVER_STORAGE_LOCATION);
            //sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);
        }
    }






    private void handleAction_startActionImportFiles(
            ArrayList<ItemClass_File> alFileList,
            int iMoveOrCopy,
            int iMediaCategory) {


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
            lTotalImportSize = lTotalImportSize + fi.sizeBytes;
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

            if(fileItem.destinationFolder.equals("")) {
                fileItem.destinationFolder = GlobalClass.gsUnsortedFolderName;
            }

            File fDestination = new File(
                    globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                            fileItem.destinationFolder);

            BroadcastProgress(true, "Verifying destination folder " + fDestination.getPath(),
                    true, iProgressBarValue,
                    true, "Verifying destination folder...",
                    RECEIVER_EXECUTE_IMPORT);


            if (!fDestination.exists()) {
                if (!fDestination.mkdir()) {
                    //Unable to create directory
                    BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath(),
                            false, iProgressBarValue,
                            true, "Operation halted.",
                            RECEIVER_EXECUTE_IMPORT);
                    continue; //Skip to the end of the loop.
                }
            }

            if (fDestination.exists()) {
                BroadcastProgress(true, "Destination folder verified.",
                        false, iProgressBarValue,
                        false, "",
                        RECEIVER_EXECUTE_IMPORT);


                Uri uriSourceFile;
                String sLogLine;
                InputStream inputStream;
                OutputStream outputStream;

                uriSourceFile = Uri.parse(fileItem.uri);
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
                    sLogLine = sLogLine + "of file " + fileItem.name + " to destination...";
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
                    ciNew.lSize = fileItem.sizeBytes;
                    ciNew.lDuration_Milliseconds = fileItem.videoTimeInMilliseconds;
                    ciNew.sDuration_Text = fileItem.videoTimeText;
                    ciNew.iWidth = Integer.parseInt(fileItem.width);
                    ciNew.iHeight = Integer.parseInt(fileItem.height);
                    ciNew.sFolder_Name = fileItem.destinationFolder;
                    ciNew.sTags = GlobalClass.formDelimitedString(fileItem.prospectiveTags, ",");
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

        }

        //Modify viewer settings to show the newly-imported files:
        globalClass.giCatalogViewerSortBySetting[iMediaCategory] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        globalClass.gbCatalogViewerSortAscending[iMediaCategory] = false;

        BroadcastProgress(true, "Operation complete.",
                true, iProgressBarValue,
                true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                RECEIVER_EXECUTE_IMPORT);

    }



    private void handleAction_startActionImportComics(
            ArrayList<ItemClass_File> alFileList,
            int iMoveOrCopy,
            int iComicImportSource) {


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
            lTotalImportSize = lTotalImportSize + fi.sizeBytes;
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
        if(iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_NH_COMIC_DOWNLOADER){
            //If NH_Comic_Downloaded, loop and find all of the comic IDs:
            for(ItemClass_File fileItem: alFileList) {
                if(fileItem.name.matches(GlobalClass.gsNHComicCoverPageFilter)){
                    String sComicID = GetNHComicID(fileItem.name);
                    String sRecordID = iNextRecordId + "";
                    iNextRecordId++;
                    String sComicName = GetNHComicNameFromCoverFile(fileItem.name);
                    String sComicTags = GlobalClass.formDelimitedString(fileItem.prospectiveTags, ",");
                    if(tmNHComicIDs.containsKey(sComicID)){
                        //If this is merely a duplicate comic selected during the import, not if it already exists in the catalog.
                        //If it already exists in the catalog, it is on the user to resolve.
                        problemNotificationConfig("Skipping Comic ID " + sComicID + ". Duplicate comic.");
                    } else {
                        tmNHComicIDs.put(sComicID, new String[]{sRecordID, sComicName, sComicTags});
                    }
                }
            }
        }



        for(Map.Entry<String, String[]> tmEntryNHComic: tmNHComicIDs.entrySet()) {
            //Loop and import files:

            String sDestinationFolder = tmEntryNHComic.getValue()[0]; //The individual comic folder is the comic ID.

            File fDestination = new File(
                    globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath() + File.separator +
                            sDestinationFolder);

            BroadcastProgress(true, "Verifying destination folder " + fDestination.getPath() + "\n",
                    true, iProgressBarValue,
                    true, "Verifying destination folder...",
                    RECEIVER_EXECUTE_IMPORT);

            if (!fDestination.exists()) {
                if (!fDestination.mkdir()) {
                    //Unable to create directory
                    BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            true, "Operation halted.",
                            RECEIVER_EXECUTE_IMPORT);
                    continue; //Skip to the end of the loop.
                }
            }
            if (fDestination.exists()) { //May be unnecessay, but want to be sure.
                BroadcastProgress(true, "Destination folder verified.\n",
                        false, iProgressBarValue,
                        false, "",
                        RECEIVER_EXECUTE_IMPORT);
            } else {
                continue;
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

                if(fileItem.name.matches("^" + tmEntryNHComic.getKey() + "_Cover.+")) {
                    //Preserve the cover page file item so that it can be deleted at the after the
                    // other files are imported. No particular reason for saving this until the end.
                    icfCoverPageFile = fileItem;

                }

                if(fileItem.name.matches("^" + tmEntryNHComic.getKey() + "_Page.+")) {

                    //Make sure that it is not a duplicate page.
                    if (!isPageDuplicate(fileItem.name)) {
                        ciNewComic.iComic_File_Count++;
                        String sNewFilename = GlobalClass.JumbleFileName(fileItem.name);
                        if (fileItem.name.contains("_Page_001")) {
                            //Set the Thumbnail file to the first page (which is a duplicate of the
                            // cover page but of a different name):
                            ciNewComic.sFilename = sNewFilename;
                            ciNewComic.sThumbnail_File = sNewFilename;
                        }
                        ciNewComic.lSize += fileItem.sizeBytes;

                        Uri uriSourceFile;
                        String sLogLine;
                        InputStream inputStream;
                        OutputStream outputStream;

                        uriSourceFile = Uri.parse(fileItem.uri);
                        DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
                        try {
                            //Write next behavior to the screen log:
                            sLogLine = "Attempting ";
                            if (iMoveOrCopy == ViewModel_ImportActivity.IMPORT_METHOD_MOVE) {
                                sLogLine = sLogLine + "move ";
                            } else {
                                sLogLine = sLogLine + "copy ";
                            }
                            sLogLine = sLogLine + "of file " + fileItem.name + " to destination...";
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
                        problemNotificationConfig("File " + fileItem.name + " appears to be a duplicate. Skipping import of this file.\n");
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

                    uriSourceFile = Uri.parse(icfCoverPageFile.uri);
                    DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);

                    if (!dfSource.delete()) {
                        sLogLine = "Could not delete source file after copy (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                    } else {
                        sLogLine = "Success deleting source file after copy.\n";
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



}