package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ImportActivityDataService extends IntentService {

    // TODO: Rename actions, choose action names that describe tasks that this
    private static final String ACTION_GET_DIRECTORY_CONTENTS = "com.agcurations.aggallerymanager.action.GET_DIRECTORY_CONTENTS";
    private static final String ACTION_IMPORT_FILES = "com.agcurations.aggallerymanager.action.IMPORT_FILES";

    private static final String EXTRA_IMPORT_TREE_URI = "com.agcurations.aggallerymanager.extra.IMPORT_TREE_URI";
    private static final String EXTRA_MEDIA_CATEGORY = "com.agcurations.aggallerymanager.extra.MEDIA_CATEGORY";

    private static final String EXTRA_IMPORT_FILES_FILELIST = "com.agcurations.aggallerymanager.extra.IMPORT_FILES_FILELIST";
    private static final String EXTRA_IMPORT_FILES_MOVE_OR_COPY = "com.agcurations.aggallerymanager.extra.IMPORT_FILES_MOVE_OR_COPY";

    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";

    public static final String EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_DIRECTORY_CONTENTS_RESPONSE"; //Used to flag in a listener that this is or is not a response to dir call.
    public static final String EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_DIRECTORY_CONTENTS_RESPONSE"; //ArrayList of response data

    public static final String RECEIVER_STORAGE_LOCATION = "com.agcurations.aggallerymanager.extra.RECEIVER_STORAGE_LOCATION";
    public static final String RECEIVER_EXECUTE_IMPORT = "com.agcurations.aggallerymanager.extra.RECEIVER_EXECUTE_IMPORT";

    public ImportActivityDataService() {
        super("ImportActivityDataService");
    }

    /**
     * Starts this service to perform actions with the given parameters. If
     * the service is already performing a task this action will be queued.
     */
    public static void startActionGetDirectoryContents(Context context, Uri uriImportTreeUri, int iMediaCategory) {
        Intent intent = new Intent(context, ImportActivityDataService.class);
        intent.setAction(ACTION_GET_DIRECTORY_CONTENTS);
        String sImportTreeUri = uriImportTreeUri.toString();
        intent.putExtra(EXTRA_IMPORT_TREE_URI, sImportTreeUri);
        intent.putExtra(EXTRA_MEDIA_CATEGORY, iMediaCategory);
        context.startService(intent);
    }

    public static void startActionImportFiles(Context context,
                                              ArrayList<FileItem> alImportFileList,
                                              int iMoveOrCopy,
                                              int iMediaCategory) {
        Intent intent = new Intent(context, ImportActivityDataService.class);
        intent.setAction(ACTION_IMPORT_FILES);
        intent.putExtra(EXTRA_IMPORT_FILES_FILELIST, alImportFileList);
        intent.putExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, iMoveOrCopy);
        intent.putExtra(EXTRA_MEDIA_CATEGORY, iMediaCategory);
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
                final ArrayList<FileItem> alFileList = (ArrayList<FileItem>) intent.getSerializableExtra(EXTRA_IMPORT_FILES_FILELIST);
                final int iMoveOrCopy = intent.getIntExtra(EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
                final int iMediaCategory = intent.getIntExtra(EXTRA_MEDIA_CATEGORY, -1);
                if(alFileList==null) return;
                handleAction_startActionImportDirectoryContents(
                        alFileList,
                        iMoveOrCopy,
                        iMediaCategory);
            }
        }
    }


    void problemNotificationConfig(String sMessage, String sReceiver){
        Intent broadcastIntent_Problem = new Intent();
        broadcastIntent_Problem.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent_Problem.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Problem.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        broadcastIntent_Problem.putExtra(RECEIVER_STRING, sReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Problem);
    }

    private void handleAction_GetDirectoryContents(Uri uriImportTreeUri, int iMediaCategory) {
        if(ImportActivity.guriImportTreeURI != null){
            Intent broadcastIntent_GetDirectoryContentsResponse = new Intent();
            ArrayList<FileItem> alFileList;
            try {
                alFileList = readFolderContent(uriImportTreeUri, ".+", iMediaCategory, FILES_ONLY);
            }catch (Exception e){
                problemNotificationConfig(e.getMessage(), RECEIVER_STORAGE_LOCATION);
                return;
            }
            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, true);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE, alFileList);

            broadcastIntent_GetDirectoryContentsResponse.setAction(ImportActivity.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
            broadcastIntent_GetDirectoryContentsResponse.addCategory(Intent.CATEGORY_DEFAULT);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(RECEIVER_STRING, RECEIVER_STORAGE_LOCATION);
            //sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);
        }
    }


    private void handleAction_startActionImportDirectoryContents(
            ArrayList<FileItem> alFileList,
            int iMoveOrCopy,
            int iMediaCategory) {


        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;
        long lLoopBytesRead;


        ContentResolver content = getApplicationContext().getContentResolver();

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        //Calculate total size of all files to import:
        for(FileItem fi: alFileList){
            lTotalImportSize = lTotalImportSize + fi.sizeBytes;
        }
        lProgressDenominator = lTotalImportSize;

        int[] iMediaIDs = {GlobalClass.VIDEO_ID_INDEX,
                            GlobalClass.IMAGE_ID_INDEX,
                            GlobalClass.COMIC_ID_INDEX};

        //Find the next record ID:
        int iNextRecordId = 0;
        int iThisId;
        for (Map.Entry<Integer, String[]> entry : globalClass.gtmCatalogLists.get(iMediaCategory).entrySet()) {
            iThisId = Integer.parseInt(entry.getValue()[iMediaIDs[iMediaCategory]]);
            if (iThisId >= iNextRecordId) iNextRecordId = iThisId + 1;
        }
        //New record ID identified.

        //Loop and import files:
        for(FileItem fileItem: alFileList) {

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
                try {
                    //Write next behavior to the screen log:
                    sLogLine = "Attempting ";
                    if (iMoveOrCopy == ImportActivity.IMPORT_METHOD_MOVE) {
                        sLogLine = sLogLine + "move ";
                    } else {
                        sLogLine = sLogLine + "copy ";
                    }
                    sLogLine = sLogLine + "of file " + fileItem.name + " to destination...";
                    BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            RECEIVER_EXECUTE_IMPORT);

                    if(dfSource == null) continue;
                    inputStream = content.openInputStream(dfSource.getUri());

                    //Reverse the text on the file so that the file does not get picked off by a search tool:
                    if(dfSource.getName()==null) continue;
                    String sFileName = globalClass.JumbleFileName(dfSource.getName());

                    outputStream = new FileOutputStream(fDestination.getPath() + File.separator + sFileName);
                    int iLoopCount = 0;
                    byte[] buffer = new byte[100000];
                    if(inputStream == null) continue;
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
                    sLogLine = "Copy success.";
                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            RECEIVER_EXECUTE_IMPORT);

                    //This file has now been copied.
                    //Next add the data to the catalog file and memory:

                    if (iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                        //Add record to catalog contents:
                    /*
                        "VIDEO_ID",
                        "VIDEO_FILENAME",
                        "SIZE_MB",
                        "DURATION_MILLISECONDS",
                        "DURATION_TEXT",
                        "RESOLUTION",
                        "FOLDER_NAME",
                        "TAGS",
                        "CAST",
                        "SOURCE",
                        "DATETIME_LAST_VIEWED_BY_USER",
                        "DATETIME_IMPORT"
                    */

                        //Create a timestamp to be used to create the data record:
                        Double dTimeStamp = GlobalClass.GetTimeStampFloat();
                        String sDateTime = dTimeStamp.toString();

                        String[] sFieldData = new String[]{
                                String.valueOf(iNextRecordId),
                                sFileName,
                                String.valueOf((fileItem.sizeBytes / 1024) / 1024),
                                String.valueOf(fileItem.videoTimeInMilliseconds),
                                String.valueOf(fileItem.videoTimeText),
                                "",
                                fileItem.destinationFolder,
                                GlobalClass.formDelimitedString(fileItem.prospectiveTags,","),
                                "",
                                "",
                                sDateTime,
                                sDateTime
                        };

                    /*public void CatalogDataFile_CreateNewRecord(
                            File fCatalogContentsFile,
                            int iRecordID,
                            TreeMap<Integer, String[]> tmCatalogRecords,
                            String[] sFieldData){*/

                        //The below call should add the record to both the catalog contents file
                        //  and memory:
                        globalClass.CatalogDataFile_CreateNewRecord(
                                globalClass.gfCatalogContentsFiles[iMediaCategory],
                                iNextRecordId,
                                globalClass.gtmCatalogLists.get(iMediaCategory),
                                sFieldData);

                    }
                    iNextRecordId += 1; //Identify the next record ID to assign.

                    if (iMoveOrCopy == ImportActivity.IMPORT_METHOD_MOVE) {
                        if (!dfSource.delete()) {
                            sLogLine = "Could not delete source file after copy (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).";
                        } else {
                            sLogLine = "Success deleting source file after copy.";
                        }
                    }

                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);

                    BroadcastProgress(true, sLogLine,
                            true, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            RECEIVER_EXECUTE_IMPORT);


                } catch (Exception e) {
                    BroadcastProgress(true, "Problem with copy/move operation.\n" + e.getMessage(),
                            false, iProgressBarValue,
                            false, "",
                            RECEIVER_EXECUTE_IMPORT);
                }

            }

        }
        BroadcastProgress(true, "Operation complete.",
                true, iProgressBarValue,
                true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                RECEIVER_EXECUTE_IMPORT);


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
        broadcastIntent.setAction(ImportActivity.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        broadcastIntent.putExtra(UPDATE_LOG_BOOLEAN, bUpdateLog);
        broadcastIntent.putExtra(LOG_LINE_STRING, sLogLine + "\n");
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

    //Use a Uri instead of a path to get folder contents:
    public ArrayList<FileItem> readFolderContent(Uri uriFolder, String sFileExtensionRegEx, int iMediaCategory, int iSelectFoldersFilesOrBoth) {
        //iMediaCategory: Specify media category. -1 ignore, 0 video, >0 images.






        //Get data about the files from the UriTree:
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriFolder, DocumentsContract.getTreeDocumentId(uriFolder));
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
        if(cImport == null){
            return null;
        }


        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;

        //Calculate total number of files for a progress bar:
        lProgressDenominator = cImport.getCount();
        BroadcastProgress(false, "",
                true, iProgressBarValue,
                true, "0/" + lProgressDenominator,
                RECEIVER_STORAGE_LOCATION);


        ArrayList<FileItem> alFileList = new ArrayList<>();

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
            String fileExtension = docName.contains(".") ? docName.substring(docName.lastIndexOf(".") + 1) : "";
            //If the file extension does not match the file extension regex, skip the remainder of the loop.
            if (!fileExtension.matches(sFileExtensionRegEx)) {
                continue;  //skip the rest of the loop if the file extension does not match.
            }

            if(!isDirectory) {
                //If this is a file, check to see if it is a video or an image and if we are looking for videos or images.
                if (mimeType.startsWith("video") || fileExtension.equals("gif")) {
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
            final Uri docUri = DocumentsContract.buildDocumentUriUsingTree(childrenUri,docId);

            //Determine the file size:
            long lFileSize = Long.parseLong(sFileSize); //size in Bytes

            //Get date last modified:
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(lLastModified);
            Date dateLastModified = cal.getTime();



            //Convert the file Uri to string. Uri's don't transport well as intent extras.
            String sUri = docUri.toString();

            //create the file model and initialize:
            //Don't detect the duration of video files here. It could take too much time.
            FileItem fileItem = new FileItem(fileType, docName, fileExtension, lFileSize, dateLastModified, false, sUri, mimeType, -1L);

            //Add the file model to the ArrayList:
            alFileList.add(fileItem);



        }
        cImport.close();

        return alFileList;
    }





}