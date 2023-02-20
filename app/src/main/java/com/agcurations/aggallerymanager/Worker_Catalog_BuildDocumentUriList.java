package com.agcurations.aggallerymanager;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.exoplayer2.util.MimeTypes;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Worker_Catalog_BuildDocumentUriList extends Worker {

    public static final String TAG_WORKER_BUILD_URI_LIST = "com.agcurations.aggallermanager.tag_worker_build_uri_list";

    int giMediaCategory;
    String gsResponseActionFilter;
    Context gContext;
    GlobalClass globalClass;

    public Worker_Catalog_BuildDocumentUriList(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gContext = context;
        gsResponseActionFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);

        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
    }

    @NonNull
    @Override
    public Result doWork() {

        globalClass = (GlobalClass) getApplicationContext();

        TreeMap<String, ItemClass_DocFileData> tm_FileLookupArray = new TreeMap<>();

        StopWatch stopWatch = new StopWatch(false);

        //Get all URIs for all documents with an up-front query so that we don't have to deal with
        //  the lag later. Inspired by https://stackoverflow.com/questions/42186820/why-is-documentfile-so-slow-and-what-should-i-use-instead.

        try {

            Uri uriCatalogFolder = globalClass.gdfCatalogFolders[giMediaCategory].getUri();
            final String sRelativePathBase = GlobalClass.gsCatalogFolderNames[giMediaCategory];
            final ItemClass_DocFileData icdfdBase = new ItemClass_DocFileData();
            icdfdBase.sPath = "";
            icdfdBase.sFileName = GlobalClass.gsCatalogFolderNames[giMediaCategory];
            icdfdBase.sPath = icdfdBase.sFileName;
            icdfdBase.bIsFolder = true;
            icdfdBase.uri = uriCatalogFolder;
            icdfdBase.iMediaCategory = giMediaCategory;
            icdfdBase.uriParentFolder = GlobalClass.gdfDataFolder.getUri();
            tm_FileLookupArray.put(sRelativePathBase, icdfdBase);

            boolean bFreshItemsAdded = true; //Flag to keep digging in the tree for more.
            stopWatch.Start();
            int iItemsFound = 0;
            int iProgressBarValue;
            while(bFreshItemsAdded){
                bFreshItemsAdded = false;
                TreeMap<String, ItemClass_DocFileData> gtm_NewAdditionsToFileLookupArray = new TreeMap<>();
                for(Map.Entry<String, ItemClass_DocFileData> entry: tm_FileLookupArray.entrySet()) {
                    ItemClass_DocFileData icdfd_Parent = entry.getValue();
                    if(!icdfd_Parent.bContentQueried) {
                        final Uri uriFolder = icdfd_Parent.uri;
                        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriFolder,
                                DocumentsContract.getDocumentId(uriFolder));
                        icdfd_Parent.bContentQueried = true;
                        Cursor c = null;
                        try {
                            c = GlobalClass.gcrContentResolver.query(childrenUri, new String[]{
                                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                    DocumentsContract.Document.COLUMN_FLAGS,
                                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                                    DocumentsContract.Document.COLUMN_SIZE
                            }, null, null, null);
                            if (c != null) {
                                if (c.getCount() > 0) {
                                    bFreshItemsAdded = true;
                                }
                                while (c.moveToNext()) {
                                    final String documentId = c.getString(0);
                                    final String sDocumentName = c.getString(1);
                                    final Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(uriFolder,
                                            documentId);
                                    final String sRelativePath = icdfd_Parent.sPath + File.separator + sDocumentName;
                                    final String sIsDir = c.getString(3);
                                    final ItemClass_DocFileData icdfd = new ItemClass_DocFileData();
                                    icdfd.sPath = sRelativePath;
                                    icdfd.sFileName = sDocumentName;
                                    icdfd.bIsFolder = sIsDir.equals(DocumentsContract.Document.MIME_TYPE_DIR);
                                    if(!icdfd.bIsFolder){
                                        icdfd.bContentQueried = true; //If it is not a folder, don't try to query for children later.
                                    }
                                    icdfd.uri = documentUri;
                                    icdfd.iMediaCategory = icdfd_Parent.iMediaCategory;
                                    icdfd.uriParentFolder = uriFolder;
                                    gtm_NewAdditionsToFileLookupArray.put(sRelativePath, icdfd);
                                    iItemsFound++;
                                    if(iItemsFound % 20 == 0){
                                        int iIndexedItems = GlobalClass.gatiFilesIndexed.addAndGet(iItemsFound);
                                        iItemsFound = 0;
                                        int iProgressNumerator = iIndexedItems;
                                        float fProgressDenominator = 1000;
                                        String sProgressDenominator = "?";
                                        if(GlobalClass.gfFileCountFromFileIndexHelper > 0){
                                            fProgressDenominator = GlobalClass.gfFileCountFromFileIndexHelper;
                                            sProgressDenominator = String.valueOf((int) fProgressDenominator);
                                        } else {
                                            iIndexedItems = iIndexedItems % 1000;
                                        }
                                        iProgressBarValue = Math.round((iIndexedItems / (float) fProgressDenominator) * 100);
                                        globalClass.BroadcastProgress(false, "",
                                                true, iProgressBarValue,
                                                true, "File Indexing " + iProgressNumerator + "/" + sProgressDenominator,
                                                Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);

                                    }
                                    //Log.d("Worker_Catalog_BuildDocumentUriList:doWork()", "Added item " + sRelativePath);
                                }
                            }

                        } catch (Exception e) {
                            Log.w("Worker_Catalog_BuildDocumentUriList:doWork()", "Failed query: " + e);
                        } finally {
                            if (c != null) {
                                c.close();
                            }
                        }

                    } //End if content has not been queried previously (also covers case of content being/not-being a folder).

                } //End for loop through all found items.

                tm_FileLookupArray.putAll(gtm_NewAdditionsToFileLookupArray);

            } //End while(there are new items to explore)
            String sMessage =  "Built list of document Uris for media category " + giMediaCategory + " with duration ";
            stopWatch.PostDebugLogAndRestart(sMessage); //Took 151.7s with 74,068 items in gtm_FileLookupArray on one thread.
            //When split into threads, took 11s for images, 48s for videos, 54s for comics,
            //  with a parallel timing of about 57s with my physical stopwatch.
            //  Timing improvement from 2:31 to 0:57, a savings of 1:34.
        } catch (Exception e){
            String sMessage = "Trouble getting file listings. " + e.getMessage();
            Log.d("Worker_Catalog_BuildDocumentUriList:doWork()", sMessage);

            return Result.failure();
        }



        //Write the found data to the master data holder.
        //Wait for the global file lookup treemap to become available, or wait for a timeout:
        int i = 0;
        int iMaxWaitTimeInSeconds = 20;
        int iMaxWaitTimeInMS = iMaxWaitTimeInSeconds * 1000;
        int iSleepAccumulator = 0;
        int iLoopFrequency = 20; //Hz
        int iSleepDurationMS = (int) ((float) 1/iLoopFrequency * 1000);
        while(iSleepAccumulator <= iMaxWaitTimeInMS){

            if(!GlobalClass.gabFileLookupArrayWriteBusy.get()){
                //If the lock is available, take it:
                GlobalClass.gabFileLookupArrayWriteBusy.set(true);
                stopWatch.PostDebugLogAndRestart("Waited for " + iSleepAccumulator + "ms for global file lookup array to become available with total duration since last message of ");
                //Initialize the global file lookup treemap if needed:
                if(GlobalClass.galtm_FileLookupTreeMap == null) {
                    GlobalClass.galtm_FileLookupTreeMap = new ArrayList<>();
                }
                //Add data to the global file lookup treemap:
                GlobalClass.galtm_FileLookupTreeMap.add(tm_FileLookupArray); //Took 2.1807E-5s to perform. Waaay faster than building one giant treemap.
                stopWatch.PostDebugLogAndRestart("Added items to lookup arraylist of treemaps with duration ");


                stopWatch.PostDebugLogAndRestart("Consolidated file indexes with duration ");
                //Release the lock on the global file lookup treemap:
                GlobalClass.gabFileLookupArrayWriteBusy.set(false);
                break;
            }
            try {
                Thread.sleep(iSleepDurationMS);
                if(iSleepAccumulator >= iMaxWaitTimeInMS){
                    //Timeout. Display message and leave.
                    LogThis("doWork", "Timeout waiting for FileLoopupArray to become available for writing. Data dropped from indexing.", null);
                    return Result.failure();
                }
            } catch (InterruptedException e) {
                LogThis("doWork", "Problem waiting for FileLookupArray. ", e.getMessage());
            }
            iSleepAccumulator += iSleepDurationMS;
        }



        //Check to see if all of the workers of this type are complete, and if so execute final actions:
        int iFileIndexingCompletionCounter = GlobalClass.gatiFileIndexingCompletionCounter.addAndGet(-1);
        if(iFileIndexingCompletionCounter <= 0){
            //If this is the last of these series of workers, complete final actions:
            int iFileCount = GlobalClass.getIndexedFileCount(); //Max size is Integer.MAX_VALUE. 2,147,483,647.

            GlobalClass.gatbFileLookupArrayLoaded.set(true); //Set flag to allow other routines to know that the FileLookupArray is complete.

            globalClass.BroadcastProgress(false, "",
                    true, 100,
                    true, "File Indexing Complete",
                    Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);

            //Create a text file listing the total file count. Do this in a text file rather than in a preference
            //  because some day may want to come back and add more data to guide the creation of
            //  more Worker_Catalog_BuildDocumentUriList instances for faster indexing.
            try {
                Uri uriFileIndexHelper = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, GlobalClass.gdfDataFolder.getUri(), MimeTypes.BASE_TYPE_TEXT, "FileIndexHelper.dat");
                if(uriFileIndexHelper != null){
                    OutputStream osFileIndexHelper = GlobalClass.gcrContentResolver.openOutputStream(uriFileIndexHelper);
                    if(osFileIndexHelper != null){
                        String sFileCount = iFileCount + "";
                        osFileIndexHelper.write(sFileCount.getBytes(StandardCharsets.UTF_8));
                        osFileIndexHelper.flush();
                        osFileIndexHelper.close();
                    }
                }
            }catch (Exception e){
                LogThis("doWork", "Unable to create index helper file.", e.getMessage());
            }
        }


        return Result.success();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Catalog_BuildDocumentUriList:" + sRoutine, sMessage);
    }




}
