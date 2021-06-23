package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class Service_CatalogViewer extends IntentService {

    // Action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_DELETE_CATALOG_ITEM = "com.agcurations.aggallerymanager.action.delete_catalog_item";
    private static final String ACTION_UPDATE_CATALOG_ITEM = "com.agcurations.aggallerymanager.action.update_catalog_item";
    private static final String ACTION_SORT_AND_FILTER_CATALOG_DISPLAY = "com.agcurations.aggallerymanager.action.sort_and_filter_catalog_display";

    private static final String EXTRA_CATALOG_ITEM = "com.agcurations.aggallerymanager.extra.catalog_item";

    public Service_CatalogViewer() {
        super("Service_CatalogViewer");
    }

    public static void startActionDeleteCatalogItem(Context context, ItemClass_CatalogItem ciToDelete) {
        Intent intent = new Intent(context, Service_CatalogViewer.class);
        intent.setAction(ACTION_DELETE_CATALOG_ITEM);
        intent.putExtra(EXTRA_CATALOG_ITEM, ciToDelete);
        context.startService(intent);
    }

    public static void startActionUpdateCatalogItem(Context context, ItemClass_CatalogItem ciToUpdate) {
        Intent intent = new Intent(context, Service_CatalogViewer.class);
        intent.setAction(ACTION_UPDATE_CATALOG_ITEM);
        intent.putExtra(EXTRA_CATALOG_ITEM, ciToUpdate);
        context.startService(intent);
    }

    public static void startActionSortAndFilterCatalogDisplay(Context context) {
        Intent intent = new Intent(context, Service_CatalogViewer.class);
        intent.setAction(ACTION_SORT_AND_FILTER_CATALOG_DISPLAY);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DELETE_CATALOG_ITEM.equals(action)) {
                final ItemClass_CatalogItem ciToDelete = (ItemClass_CatalogItem) intent.getSerializableExtra(EXTRA_CATALOG_ITEM);
                handleActionDeleteCatalogItem(ciToDelete);
            } else if (ACTION_UPDATE_CATALOG_ITEM.equals(action)) {
                final ItemClass_CatalogItem ciToUpdate = (ItemClass_CatalogItem) intent.getSerializableExtra(EXTRA_CATALOG_ITEM);
                handleActionUpdateCatalogItem(ciToUpdate);
            } else if (ACTION_SORT_AND_FILTER_CATALOG_DISPLAY.equals(action)) {
                handleActionSortAndFilterCatalogDisplay();
            }
        }
    }


    /**
     * Handle action DeleteCatalogItem in the provided background thread with the provided
     * parameters.
     */
    public static final String EXTRA_BOOL_DELETE_ITEM = "com.agcurations.aggallerymanager.extra.delete_item";
    public static final String EXTRA_BOOL_DELETE_ITEM_RESULT = "com.agcurations.aggallerymanager.extra.delete_item_result";
    private void handleActionDeleteCatalogItem(ItemClass_CatalogItem ci) {

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        //Delete the item record from the CatalogContentsFile:
        boolean bSuccess = true;
        try {

            //Delete the file:

            //Get a path to the file to delete:
            String sCatalogFolderPath = globalClass.gfCatalogFolders[ci.iMediaCategory].getPath();
            String sItemFolderName = ci.sFolder_Name;
            String sItemFileName = ci.sFilename;

            String sFileFolder = sCatalogFolderPath + File.separator +
                    sItemFolderName;
            String sFullPath = sFileFolder + File.separator +
                    sItemFileName;
            File fFileToBeDeleted = new File(sFullPath);

            if(ci.iPostProcessingCode == ItemClass_CatalogItem.POST_PROCESSING_VIDEO_DLM_CONCAT ||
               ci.iPostProcessingCode == ItemClass_CatalogItem.POST_PROCESSING_VIDEO_DLM_SINGLE){
                //Delete the temporary download folders, etc.
                String sVideoDestinationFolder = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath() +
                        File.separator + ci.sFolder_Name;
                String sVideoDownloadFolder = sVideoDestinationFolder + File.separator + ci.sItemID;
                File fVideoDownloadFolder = new File(sVideoDownloadFolder);
                if(fVideoDownloadFolder.exists()){
                    File[] fVideoDownloadFolderListing = fVideoDownloadFolder.listFiles();
                    ArrayList<File> alfOutputFolders = new ArrayList<>();
                    if(fVideoDownloadFolderListing != null) {
                        for (File f : fVideoDownloadFolderListing) {
                            //Locate the output folder
                            if (f.isDirectory()) {
                                alfOutputFolders.add(f); //The worker could potentially create multiple output folders if it is re-run.
                            }
                        }
                        //Go through the output folders and delete contents:
                        for (File f2 : alfOutputFolders) {
                            File[] f2_Contents = f2.listFiles();
                            if (f2_Contents != null) {
                                for (File f3 : f2_Contents) {
                                    if(!f3.delete()){
                                        Log.d("File Deletion", "Unable to delete file " + f3.getAbsolutePath());
                                    }
                                }
                            }
                        }
                        //Delete download folder contents:
                        for (File f4 : fVideoDownloadFolderListing) {
                            if(!f4.delete()){
                                Log.d("File Deletion", "Unable to delete file or folder " + f4.getAbsolutePath());
                            }
                        }
                        //Delete download folder:
                        if(!fVideoDownloadFolder.delete()){
                            Log.d("File Deletion", "Unable to delete folder " + fVideoDownloadFolder.getAbsolutePath());
                        }

                    }
                }






            } else {

                if (fFileToBeDeleted.exists()) {
                    if (!fFileToBeDeleted.delete()) {
                        problemNotificationConfig("Could not delete file.");
                        bSuccess = false;
                    }
                } else {
                    problemNotificationConfig("Could not find file at this location: " + sFullPath);
                    bSuccess = false;
                }
            }
            if(bSuccess) {

                //Delete the folder if the folder is now empty:
                File fFolder = new File(sFileFolder);
                String[] sFilesRemaining = fFolder.list();
                if(sFilesRemaining != null) {
                    if (sFilesRemaining.length == 0) {
                        if (!fFolder.delete()) {
                            problemNotificationConfig("Folder holding this item is empty, but could not delete folder. Folder name: " + sFileFolder);
                        }
                    }
                }

                //Now delete the item record from the Catalog File:
                StringBuilder sbBuffer = new StringBuilder();
                BufferedReader brReader;
                brReader = new BufferedReader(new FileReader(globalClass.gfCatalogContentsFiles[ci.iMediaCategory].getAbsolutePath()));
                sbBuffer.append(brReader.readLine());
                sbBuffer.append("\n");

                String sLine = brReader.readLine();
                bSuccess = false;
                ItemClass_CatalogItem ciFromFile;
                while (sLine != null) {
                    ciFromFile = GlobalClass.ConvertStringToCatalogItem(sLine);
                    if (!(ciFromFile.sItemID.equals(ci.sItemID))) {
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
                    problemNotificationConfig("Could not locate item data record (ID: " +
                            GlobalClass.JumbleStorageText(ci.sItemID) +
                            ") in CatalogContents.dat.\n" +
                            globalClass.gfCatalogContentsFiles[ci.iMediaCategory]);

                }

                //Re-write the CatalogContentsFile without the deleted comic's data record:
                FileWriter fwNewCatalogContentsFile = new FileWriter(globalClass.gfCatalogContentsFiles[ci.iMediaCategory], false);
                fwNewCatalogContentsFile.write(sbBuffer.toString());
                fwNewCatalogContentsFile.flush();
                fwNewCatalogContentsFile.close();


                //Now update memory to no longer include the item:
                globalClass.gtmCatalogLists.get(ci.iMediaCategory).remove(ci.sItemID);

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

    private void handleActionUpdateCatalogItem(ItemClass_CatalogItem ciToUpdate) {
        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();
        globalClass.CatalogDataFile_UpdateRecord(ciToUpdate);
    }

    public static final String EXTRA_BOOL_REFRESH_CATALOG_DISPLAY = "com.agcurations.aggallerymanager.extra.refresh_catalog_display";
    private void handleActionSortAndFilterCatalogDisplay(){

        GlobalClass globalClass;
        globalClass = (GlobalClass) getApplicationContext();

        //First look for any download items requiring post-processing:
        globalClass.ExecuteDownloadManagerPostProcessing();

        //Apply the sort field.
        //Copy over only items that match a filter, if applied.
        //Copy over only non-restricted catalog items, if necessary.
        //Sort the TreeMap.

        //Create new TreeMap to presort the catalog items:
        TreeMap<String, ItemClass_CatalogItem> treeMapPreSort; //String = field being sorted, String = Catalog item data
        treeMapPreSort = new TreeMap<>();

        //Populate the Key field of the preSort TreeMap with SortBy field data, filtered and restricted if necessary:
        int iProgressNumerator = 1;
        int iProgressDenominator = globalClass.gtmCatalogLists.get(globalClass.giSelectedCatalogMediaCategory).size() + 1;
        int iProgressBarValue;

        String sKey;
        for (Map.Entry<String, ItemClass_CatalogItem>
                entry : globalClass.gtmCatalogLists.get(globalClass.giSelectedCatalogMediaCategory).entrySet()) {
            sKey = "";
            //Create a unique key to identify the record in the TreeMap, which includes
            // the SortBy field. TreeMap automatically sorts by the Key field.
            if(globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_LAST_VIEWED){
                sKey = entry.getValue().dDatetime_Last_Viewed_by_User.toString();
            } else if(globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_IMPORTED){
                sKey = entry.getValue().dDatetime_Import.toString();
            }
            sKey = sKey + entry.getValue().sItemID;


            //Apply a filter if requested - build a string out of the records contents, and if a
            //  filter is to be applied, check for a match. If no match, don't add the record to
            //  the TreeMap destined for the RecyclerView:
            boolean bIsFilterMatch = true;
            if(!globalClass.gsCatalogViewerFilterText[globalClass.giSelectedCatalogMediaCategory].equals("")) {
                String sFilterText_LowerCase = globalClass.gsCatalogViewerFilterText[globalClass.giSelectedCatalogMediaCategory].toLowerCase();
                String sKey_RecordText;

                //Append all of the field data and search the resulting
                //  string for a filter match:
                sKey_RecordText = globalClass.getCatalogRecordSearchString(entry.getValue());
                sKey_RecordText = sKey_RecordText.toLowerCase();

                if (!sKey_RecordText.contains(sFilterText_LowerCase)) {
                    bIsFilterMatch = false;
                }
            }

            //Check to see if the record needs to be skipped due to restriction settings:
            boolean bIsRestricted = false;
            if(globalClass.gbCatalogViewerTagsRestrictionsOn) {
                String sRecordTags = entry.getValue().sTags;
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
                        ItemClass_Tag ict = globalClass.gtmCatalogTagReferenceLists.get(globalClass.giSelectedCatalogMediaCategory).get(globalClass.getTagTextFromID(iTagID, globalClass.giSelectedCatalogMediaCategory));
                        if (ict != null) {
                            if (ict.bIsRestricted) {
                                bIsRestricted = true;
                                break;
                            }
                        }
                    }
                }
            }

            if(bIsFilterMatch && !bIsRestricted){
                treeMapPreSort.put(sKey, entry.getValue());
            }

            iProgressNumerator++;
            if(iProgressNumerator % 1 == 0) {
                iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                BroadcastProgress(true, iProgressBarValue,
                        true, iProgressBarValue + "%");
            }
        }

        //TreeMap presort will auto-sort itself.

        //Clean up the key, apply a reverse sort order, if applicable:
        TreeMap<Integer, ItemClass_CatalogItem> tmNewOrderCatalogList = new TreeMap<>();
        int iRID, iIterator;
        if(globalClass.gbCatalogViewerSortAscending[globalClass.giSelectedCatalogMediaCategory]){
            iRID = 0;
            iIterator = 1;
        } else {
            iRID = treeMapPreSort.size();
            iIterator = -1;
        }

        /* //No need to refresh the progress here - it is pretty quick.
        iProgressNumerator = 0;
        iProgressDenominator = treeMapPreSort.size();*/
        for (Map.Entry<String, ItemClass_CatalogItem>
                entry : treeMapPreSort.entrySet()) {
            tmNewOrderCatalogList.put(iRID, entry.getValue());
            iRID += iIterator;
            /* //No need to show progress here - it is pretty quick.
            iProgressNumerator++;
            if(iProgressNumerator % 2 == 0) {
                iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                BroadcastProgress(true, iProgressBarValue,
                        true, iProgressNumerator + "/" + iProgressDenominator);
            }*/
        }

        globalClass.gtmCatalogViewerDisplayTreeMap = tmNewOrderCatalogList;

        //Broadcast the ready state of the SortAndFilterCatalogDisplay operation:
        Intent broadcastIntent_SortAndFilterCatalogDisplayResponse = new Intent();
        broadcastIntent_SortAndFilterCatalogDisplayResponse.putExtra(EXTRA_BOOL_REFRESH_CATALOG_DISPLAY, true);
        broadcastIntent_SortAndFilterCatalogDisplayResponse.setAction(Activity_CatalogViewer.CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
        broadcastIntent_SortAndFilterCatalogDisplayResponse.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_SortAndFilterCatalogDisplayResponse);


    }


    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";

    void problemNotificationConfig(String sMessage){
        Intent broadcastIntent_Problem = new Intent();
        broadcastIntent_Problem.setAction(Activity_CatalogViewer.CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
        broadcastIntent_Problem.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent_Problem.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Problem.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Problem);
    }

    public static final String UPDATE_PERCENT_COMPLETE_BOOLEAN = "UPDATE_PERCENT_COMPLETE_BOOLEAN";
    public static final String PERCENT_COMPLETE_INT = "PERCENT_COMPLETE_INT";
    public static final String UPDATE_PROGRESS_BAR_TEXT_BOOLEAN = "UPDATE_PROGRESS_BAR_TEXT_BOOLEAN";
    public static final String PROGRESS_BAR_TEXT_STRING = "PROGRESS_BAR_TEXT_STRING";

    public void BroadcastProgress(boolean bUpdatePercentComplete, int iAmountComplete,
                                  boolean bUpdateProgressBarText, String sProgressBarText){

        //Broadcast a message to be picked-up by the Import Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_CatalogViewer.CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        broadcastIntent.putExtra(UPDATE_PERCENT_COMPLETE_BOOLEAN, bUpdatePercentComplete);
        broadcastIntent.putExtra(PERCENT_COMPLETE_INT, iAmountComplete);
        broadcastIntent.putExtra(UPDATE_PROGRESS_BAR_TEXT_BOOLEAN, bUpdateProgressBarText);
        broadcastIntent.putExtra(PROGRESS_BAR_TEXT_STRING, sProgressBarText);

        //sendBroadcast(broadcastIntent);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }


}