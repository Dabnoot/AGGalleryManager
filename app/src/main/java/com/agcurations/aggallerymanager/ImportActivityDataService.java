package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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
    private static final String EXTRA_IMPORT_FILES_ARRAY = "com.agcurations.aggallerymanager.extra.IMPORT_FILES_ARRAY";
    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";
    public static final String EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_DIRECTORY_CONTENTS_RESPONSE"; //Used to flag in a listener that this is or is not a response to dir call.
    public static final String EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_DIRECTORY_CONTENTS_RESPONSE"; //ArrayList of response data

    public ImportActivityDataService() {
        super("ImportActivityDataService");
    }

    /**
     * Starts this service to perform actions with the given parameters. If
     * the service is already performing a task this action will be queued.
     */
    public static void startActionGetDirectoryContents(Context context, Uri uriImportTreeUri) {
        Intent intent = new Intent(context, ImportActivityDataService.class);
        intent.setAction(ACTION_GET_DIRECTORY_CONTENTS);
        String sImportTreeUri = uriImportTreeUri.toString();
        intent.putExtra(EXTRA_IMPORT_TREE_URI, sImportTreeUri);
        context.startService(intent);
    }

    /**
     * Starts this service to perform actions with the given parameters. If
     * the service is already performing a task this action will be queued.
     */
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, ImportActivityDataService.class);
        intent.setAction(ACTION_IMPORT_FILES);
        intent.putExtra(EXTRA_IMPORT_FILES_ARRAY, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_GET_DIRECTORY_CONTENTS.equals(action)) {
                final String sImportTreeUri = intent.getStringExtra(EXTRA_IMPORT_TREE_URI);
                Uri uriImportTreeUri = Uri.parse(sImportTreeUri);
                handleAction_GetDirectoryContents(uriImportTreeUri);

            } else if (ACTION_IMPORT_FILES.equals(action)) {
                final String param2 = intent.getStringExtra(EXTRA_IMPORT_FILES_ARRAY);
                handleActionBaz(param2);
            }
        }
    }

    Intent broadcastIntent_GetDirectoryContentsResponse = new Intent();; //Make global to allow for problem notification string extras.
    void problemNotificationConfig(String sMessage){
        broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_STRING_PROBLEM, sMessage);

    }

    private void handleAction_GetDirectoryContents(Uri uriImportTreeUri) {
        if(ImportActivity.guriImportTreeURI != null){

            ArrayList<ImportActivity.fileModel> alFileList = readFolderContent(uriImportTreeUri, ".+", FILES_ONLY);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, true);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE, alFileList);

            /*ImportActivity.fileModel2 fm2;
            Date date = new Date();
            fm2 = new ImportActivity.fileModel2("test", "test", "test", 0L, date, false);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE, fm2);*/

            broadcastIntent_GetDirectoryContentsResponse.setAction(ImportActivity.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
            broadcastIntent_GetDirectoryContentsResponse.addCategory(Intent.CATEGORY_DEFAULT);
            sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);
        }
    }


    private void handleActionBaz(String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }





    int FOLDERS_ONLY = 0;
    int FILES_ONLY = 1;
    //int BOTH_FOLDERS_AND_FILES = 3;
    public ArrayList<ImportActivity.fileModel> readFolderContent(String _path, String sFileExtensionRegEx, int iSelectFoldersFilesOrBoth) {

        ArrayList<ImportActivity.fileModel> alFileList = new ArrayList<>();

        File directory = new File(_path);
        File[] files = directory.listFiles();

        if(files == null){
            return null;
        }

        //Gather attributes from files:
        for (File value : files) {
            String fileType = value.isDirectory() ? "folder" : "file";
            if ((iSelectFoldersFilesOrBoth == FILES_ONLY) && (value.isDirectory())) {
                continue; //skip the rest of the for loop for this item.
            } else if ((iSelectFoldersFilesOrBoth == FOLDERS_ONLY) && (!value.isDirectory())) {
                continue; //skip the rest of the for loop for this item.
            }
            String fileName = value.getName();
            String filePath = value.getPath();
            long fileSize = value.length() / 1024; //size in kB.
            Date dateLastModified = new Date(value.lastModified());
            String fileExtention = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf(".")) : "";

            if (!fileExtention.matches(sFileExtensionRegEx)) {
                continue;  //skip the rest of the loop if the file extension does not match.
            }

            //create the file model and initialize:
            ImportActivity.fileModel file = new ImportActivity.fileModel(fileType, fileName, filePath, fileSize, dateLastModified, false);

            alFileList.add(file); // add the file models to the ArrayList.
        }

        return alFileList;
    }

    //Use a Uri instead of a path to get folder contents:
    public ArrayList<ImportActivity.fileModel> readFolderContent(Uri uriFolder, String sFileExtensionRegEx, int iSelectFoldersFilesOrBoth) {

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

        ArrayList<ImportActivity.fileModel> alFileList = new ArrayList<>();

        while (cImport.moveToNext()) {
            final String docId = cImport.getString(0);
            final String docName = cImport.getString(1);
            final String mime = cImport.getString(2);
            final long lLastModified = cImport.getLong(3); //milliseconds since January 1, 1970 00:00:00.0 UTC.
            final String sFileSize = cImport.getString(4);

            boolean isDirectory;
            isDirectory = (mime.equals(DocumentsContract.Document.MIME_TYPE_DIR));
            String fileType = (isDirectory) ? "folder" : "file";

            if ((iSelectFoldersFilesOrBoth == FILES_ONLY) && (isDirectory)) {
                continue; //skip the rest of the for loop for this item.
            } else if ((iSelectFoldersFilesOrBoth == FOLDERS_ONLY) && (!isDirectory)) {
                continue; //skip the rest of the for loop for this item.
            }

            //Get a Uri for this individual document:
            final Uri docUri = DocumentsContract.buildDocumentUriUsingTree(childrenUri,docId);

            long lFileSize = Long.parseLong(sFileSize) / 1024; //size in kB


            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(lLastModified);
            Date dateLastModified = cal.getTime();

            String fileExtension = docName.contains(".") ? docName.substring(docName.lastIndexOf(".")) : "";

            if (!fileExtension.matches(sFileExtensionRegEx)) {
                continue;  //skip the rest of the loop if the file extension does not match.
            }

            String sUri = docUri.toString();

            //create the file model and initialize:
            ImportActivity.fileModel file = new ImportActivity.fileModel(fileType, docName, lFileSize, dateLastModified, false, sUri);

            alFileList.add(file); // add the file models to the ArrayList.

        }
        cImport.close();

        return alFileList;
    }





}