package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class Service_CatalogViewer extends IntentService {

    // Action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_DELETE_CATALOG_ITEM = "com.agcurations.aggallerymanager.action.delete_catalog_item";

    private static final String EXTRA_ITEM_ID = "com.agcurations.aggallerymanager.extra.item_id";
    private static final String EXTRA_MEDIA_CATEGORY = "com.agcurations.aggallerymanager.extra.media_category";

    public Service_CatalogViewer() {
        super("Service_CatalogViewer");
    }

    public static void startActionDeleteCatalogItem(Context context, String sItemID, int iMediaCategory) {
        Intent intent = new Intent(context, Service_CatalogViewer.class);
        intent.setAction(ACTION_DELETE_CATALOG_ITEM);
        intent.putExtra(EXTRA_ITEM_ID, sItemID);
        intent.putExtra(EXTRA_MEDIA_CATEGORY, iMediaCategory);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DELETE_CATALOG_ITEM.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_ITEM_ID);
                final int param2 = intent.getIntExtra(EXTRA_MEDIA_CATEGORY, 0);
                handleActionDeleteCatalogItem(param1, param2);
            }
        }
    }


    /**
     * Handle action DeleteCatalogItem in the provided background thread with the provided
     * parameters.
     */
    public static final String EXTRA_BOOL_DELETE_ITEM = "com.agcurations.aggallerymanager.extra.delete_item";
    public static final String EXTRA_BOOL_DELETE_ITEM_RESULT = "com.agcurations.aggallerymanager.extra.delete_item_result";
    private void handleActionDeleteCatalogItem(String sItemID, int iMediaCategory) {

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        //Delete the item record from the CatalogContentsFile:
        boolean bSuccess = true;
        try {

            //Delete the file:

            //Get a path to the file to delete:
            String sCatalogFolderPath = globalClass.gfCatalogFolders[iMediaCategory].getPath();
            String sItemFolderName = "";
            String sItemFileName = "";
            for (Map.Entry<Integer, String[]>
                    CatalogEntry : globalClass.gtmCatalogLists.get(iMediaCategory).entrySet()) {
                String[] sFields = CatalogEntry.getValue();
                if( sFields[GlobalClass.giDataRecordIDIndexes[iMediaCategory]].contains(sItemID)){
                    sItemFolderName = sFields[GlobalClass.giDataRecordFolderIndexes[iMediaCategory]];
                    sItemFileName = sFields[GlobalClass.giDataRecordFileNameIndexes[iMediaCategory]];
                    break;
                }
            }
            String sFileFolder = sCatalogFolderPath + File.separator +
                    sItemFolderName;
            String sFullPath = sFileFolder + File.separator +
                    sItemFileName;
            File fFileToBeDeleted = new File(sFullPath);
            if(fFileToBeDeleted.exists()){
                if(!fFileToBeDeleted.delete()){
                    problemNotificationConfig("Could not delete file.");
                    bSuccess = false;
                }
            } else {
                problemNotificationConfig("Could not find file at this location: " + sFullPath);
                bSuccess = false;
            }

            if(bSuccess) {

                //Delete the folder if the folder is now empty:
                File fFolder = new File(sFileFolder);
                if (fFolder.list().length == 0) {
                    if (!fFolder.delete()) {
                        problemNotificationConfig("Folder holding this item is empty, but could not delete folder. Folder name: " + sFileFolder);
                    }
                }

                //Now delete the item record from the Catalog File:
                StringBuilder sbBuffer = new StringBuilder();
                BufferedReader brReader;
                brReader = new BufferedReader(new FileReader(globalClass.gfCatalogContentsFiles[iMediaCategory].getAbsolutePath()));
                sbBuffer.append(brReader.readLine());
                sbBuffer.append("\n");

                String[] sFields;
                String sLine = brReader.readLine();
                bSuccess = false;
                while (sLine != null) {
                    sFields = sLine.split("\t", -1);
                    if (!(GlobalClass.JumbleStorageText(sFields[GlobalClass.giDataRecordIDIndexes[iMediaCategory]]).equals(sItemID))) {
                        //If the line is not the comic we are trying to delete, transfer it over:
                        sbBuffer.append(sLine);
                        sbBuffer.append("\n");
                    } else {
                        //Item record is located and we are skipping copying it into the buffer (thus deleting it).
                        bSuccess = true;
                    }

                    // read next line
                    sLine = brReader.readLine();
                }
                brReader.close();

                if(!bSuccess){
                    problemNotificationConfig("Could not located item data record (ID: " +
                            GlobalClass.JumbleStorageText(sItemID) +
                            ") in CatalogContents.dat.\n" +
                            globalClass.gfCatalogContentsFiles[iMediaCategory]);

                }

                //Re-write the CatalogContentsFile without the deleted comic's data record:
                FileWriter fwNewCatalogContentsFile = new FileWriter(globalClass.gfCatalogContentsFiles[iMediaCategory], false);
                fwNewCatalogContentsFile.write(sbBuffer.toString());
                fwNewCatalogContentsFile.flush();
                fwNewCatalogContentsFile.close();


                //Now update memory to no longer include the item:
                int iKey = -1;
                for (Map.Entry<Integer, String[]>
                        CatalogEntry : globalClass.gtmCatalogLists.get(iMediaCategory).entrySet()) {
                    String sEntryID = CatalogEntry.getValue()[GlobalClass.giDataRecordIDIndexes[iMediaCategory]];
                    if (sEntryID.contains(sItemID)) {
                        iKey = CatalogEntry.getKey();
                        break;
                    }
                }
                if (iKey >= 0) {
                    globalClass.gtmCatalogLists.get(iMediaCategory).remove(iKey);
                } else {
                    problemNotificationConfig("Could not locate item to be deleted in memory.");
                    //This message should only appear if I am having trouble with coding.
                }

            } //End if for continuing after successful file deletion.

        } catch (Exception e) {
            problemNotificationConfig("Problem updating CatalogContents.dat.\n" + e.getMessage());
            bSuccess = false;
        }

        //Broadcast the result of the delete item action:
        Intent broadcastIntent_DeleteCatalogItemResponse = new Intent();
        broadcastIntent_DeleteCatalogItemResponse.putExtra(EXTRA_BOOL_DELETE_ITEM, true);
        broadcastIntent_DeleteCatalogItemResponse.putExtra(EXTRA_BOOL_DELETE_ITEM_RESULT, bSuccess);
        broadcastIntent_DeleteCatalogItemResponse.setAction(Activity_CatalogViewer.CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
        broadcastIntent_DeleteCatalogItemResponse.addCategory(Intent.CATEGORY_DEFAULT);
        //sendBroadcast(broadcastIntent_DeleteCatalogItemResponse);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_DeleteCatalogItemResponse);


    }


    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";

    void problemNotificationConfig(String sMessage){
        Intent broadcastIntent_Problem = new Intent();
        broadcastIntent_Problem.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent_Problem.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent_Problem.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Problem.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Problem);
    }


}