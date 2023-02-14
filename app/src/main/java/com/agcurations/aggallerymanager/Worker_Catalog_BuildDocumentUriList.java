package com.agcurations.aggallerymanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.exoplayer2.util.MimeTypes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public class Worker_Catalog_BuildDocumentUriList extends Worker {

    public static final String TAG_WORKER_BUILD_URI_LIST = "com.agcurations.aggallermanager.tag_worker_build_uri_list";

    String gsResponseActionFilter;
    Context gContext;
    GlobalClass globalClass;

    public Worker_Catalog_BuildDocumentUriList(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gContext = context;
        gsResponseActionFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);


    }

    @NonNull
    @Override
    public Result doWork() {

        globalClass = (GlobalClass) getApplicationContext();

        if(globalClass.giBuildingDocumentUriListState == GlobalClass.LOADING_STATE_STARTED
            || globalClass.giBuildingDocumentUriListState == GlobalClass.LOADING_STATE_FINISHED){
            return Result.failure();
        }
        globalClass.giBuildingDocumentUriListState = GlobalClass.LOADING_STATE_STARTED;

        globalClass.bFileLookupArrayLoaded.set(false);
        globalClass.bFolderLookupArrayLoaded.set(false);

        StopWatch stopWatch = new StopWatch(false);

        //Debug Experiment...
        //Get all URIs for all documents with an up-front query so that we don't have to deal with
        //  the lag later. Inspired by https://stackoverflow.com/questions/42186820/why-is-documentfile-so-slow-and-what-should-i-use-instead.
        globalClass.gtm_FileLookupArray = new TreeMap<>();
        globalClass.gtm_BaseFoldersLookupArray = new TreeMap<>();

        try {

            int[] iCatOrder = {1, 2, 0};
            for(int i = 0; i < 3; i++) {
                //Odd loop ordering - videos is the biggest and takes the longest to process.
                //  Look at images, comics, and then videos.
                Uri uriCatalogFolder = globalClass.gdfCatalogFolders[iCatOrder[i]].getUri();
                final String sRelativePath = GlobalClass.gsCatalogFolderNames[iCatOrder[i]];
                final ItemClass_DocFileData icdfd = new ItemClass_DocFileData();
                icdfd.sPath = "";
                icdfd.sFileName = GlobalClass.gsCatalogFolderNames[iCatOrder[i]];
                icdfd.sPath = icdfd.sFileName;
                icdfd.bIsFolder = true;
                icdfd.uri = uriCatalogFolder;
                icdfd.iMediaCategory = iCatOrder[i];
                icdfd.uriParentFolder = GlobalClass.gdfDataFolder.getUri();
                globalClass.gtm_FileLookupArray.put(sRelativePath, icdfd);
                globalClass.gtm_BaseFoldersLookupArray.put(sRelativePath, icdfd);
            }
            boolean bFreshItemsAdded = true; //Flag to keep digging in the tree for more.
            stopWatch.Start();
            boolean bVideosCatalogProcessed;
            boolean bImagesCatalogProcessed;
            boolean bComicsCatalogProcessed;
            while(bFreshItemsAdded){
                bFreshItemsAdded = false;
                TreeMap<String, ItemClass_DocFileData> gtm_NewAdditionsToFileLookupArray = new TreeMap<>();
                for(Map.Entry<String, ItemClass_DocFileData> entry: globalClass.gtm_FileLookupArray.entrySet()) {
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
                                    Log.d("Worker_Catalog_BuildDocumentUriList:doWork()", "Added item " + sRelativePath);
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

                globalClass.gtm_FileLookupArray.putAll(gtm_NewAdditionsToFileLookupArray);

            } //End while(there are new items to explore)

            stopWatch.PostDebugLogAndRestart("Built list of document Uris with duration ");
        } catch (Exception e){
            String sMessage = "Trouble getting file listings. " + e.getMessage();
            Log.d("Worker_Catalog_BuildDocumentUriList:doWork()", sMessage);
            globalClass.giBuildingDocumentUriListState = GlobalClass.LOADING_STATE_FINISHED;
            return Result.failure();
        }

        globalClass.bFileLookupArrayLoaded.set(true);
        globalClass.giBuildingDocumentUriListState = GlobalClass.LOADING_STATE_FINISHED;
        return Result.success();
    }


}
