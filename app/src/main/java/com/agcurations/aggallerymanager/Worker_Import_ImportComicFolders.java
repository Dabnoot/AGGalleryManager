package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.exoplayer2.util.MimeTypes;

public class Worker_Import_ImportComicFolders extends Worker {

    public static final String TAG_WORKER_IMPORT_IMPORTCOMICFOLDERS = "com.agcurations.aggallermanager.tag_worker_import_importcomicfolders";

    int giMoveOrCopy;

    public Worker_Import_ImportComicFolders(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        giMoveOrCopy = getInputData().getInt(GlobalClass.EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public Result doWork() {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        //Set the flags to tell the catalogViewer to view the imported files first:
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sharedPreferences.edit()
                .putInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[GlobalClass.giSelectedCatalogMediaCategory],
                        GlobalClass.SORT_BY_DATETIME_IMPORTED)
                .putBoolean(GlobalClass.gsCatalogViewerPreferenceNameSortAscending[GlobalClass.giSelectedCatalogMediaCategory],
                        false)
                .apply();

        long lByteProgressNumerator = 0L;
        long lByteProgressDenominator;
        int iFileCountProgressNumerator = 0;
        int iFileCountProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;

        ArrayList<ItemClass_File> alFileList = (ArrayList<ItemClass_File>)globalClass.galImportFileList.clone();

        //Calculate total size of all files to import:
        for(ItemClass_File fi: alFileList){
            lTotalImportSize = lTotalImportSize + fi.lSizeBytes;
        }
        lByteProgressDenominator = lTotalImportSize;
        iFileCountProgressDenominator = alFileList.size();

        String sMessage;
        String sLogLine;

        StringBuilder sbJobFileRecords = new StringBuilder();

        ArrayList<ItemClass_CatalogItem> alci_NewCatalogItems = new ArrayList<>();

        MediaMetadataRetriever mediaMetadataRetriever;
        mediaMetadataRetriever = new MediaMetadataRetriever();

        globalClass.BroadcastProgress(true, "Preparing data for job file.\n",
                false, iProgressBarValue,
                true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

        //Loop and examine list of files to be imported, build a buffer of records to write to a job file,
        // and create catalog item records:

        TreeMap<String, String[]> tmComics = new TreeMap<>(); //Map Comic to a record ID/folder and also grab the comic title.

        //This is Comic Folder Import. Loop and find all of the comic parent Uris:
        int INDEX_RECORD_ID = 0;
        int INDEX_COMIC_NAME = 1;
        int INDEX_COMIC_TAGS = 2;
        int INDEX_COMIC_GRADE = 3;
        int INDEX_COMIC_SOURCE = 4;
        int INDEX_COMIC_PARODY = 5;
        int INDEX_COMIC_ARTIST = 6;
        int INDEX_MARKED_FOR_DELETION = 7;
        for(ItemClass_File fileItem: alFileList) {
            if(fileItem.iTypeFileFolderURL == ItemClass_File.TYPE_FOLDER){
                String sUriParent = fileItem.sUri;
                String sRecordID = globalClass.getNewCatalogRecordID(GlobalClass.MEDIA_CATEGORY_COMICS);
                String sComicName = fileItem.sFileOrFolderName;
                if(!fileItem.sTitle.equals("")){
                    sComicName = fileItem.sTitle;
                }
                String sSource = ItemClass_CatalogItem.FOLDER_SOURCE;
                if(!fileItem.sURL.equals("")){
                    sSource = fileItem.sURL;
                }
                String sParody = "";
                if(!fileItem.sComicParodies.equals("")){
                    sParody = fileItem.sComicParodies;
                }
                String sArtist = "";
                if(!fileItem.sComicArtists.equals("")){
                    sArtist = fileItem.sComicArtists;
                }
                String sComicTags = GlobalClass.formDelimitedString(fileItem.aliProspectiveTags, ",");
                int iGrade = fileItem.iGrade;
                Boolean bMarkedForDeletion = fileItem.bMarkedForDeletion;
                tmComics.put(sUriParent, new String[]{
                        sRecordID,
                        sComicName,
                        sComicTags,
                        String.valueOf(iGrade),
                        sSource,
                        sParody,
                        sArtist,
                        bMarkedForDeletion.toString()});
            }
        }
        //Comic folders identified.


        for(Map.Entry<String, String[]> tmEntryComic: tmComics.entrySet()) {

            //Find comic files belonging to this comic folder and put them in a tree map for sorting.
            TreeMap<String, ItemClass_File> tmComicFiles = new TreeMap<>();
            for (ItemClass_File fileItem : alFileList) {
                if (fileItem.sUriParent.equals(tmEntryComic.getKey())) {
                    tmComicFiles.put(fileItem.sFileOrFolderName, fileItem);
                }
            }

            String sComicFolderName = tmEntryComic.getValue()[INDEX_COMIC_NAME];

            if(Boolean.parseBoolean(tmEntryComic.getValue()[INDEX_MARKED_FOR_DELETION])){
                //If this comic is marked for deletion, create job file records that will tell
                //  the local file transfer worker to delete the files:
                for (Map.Entry<String, ItemClass_File> entryComicFile : tmComicFiles.entrySet()) {
                    String sLine = Worker_LocalFileTransfer.CreateJobFileRecord(
                            entryComicFile.getValue().sUri,
                            "",
                            "",
                            entryComicFile.getValue().lSizeBytes,
                            true);                 //Item marked for deletion?
                    sbJobFileRecords.append(sLine);

                    //Write progress to the screen log:
                    sLogLine = sComicFolderName + "\\" + entryComicFile.getValue().sFileOrFolderName + " marked for deletion.\n";
                    iFileCountProgressNumerator++;
                    iProgressBarValue = Math.round((lByteProgressNumerator / (float) lByteProgressDenominator) * 100);
                    globalClass.BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                }
                //Write a record to tell the local file transfer worker to delete the comic source
                // folder as well:
                String sLine = Worker_LocalFileTransfer.CreateJobFileRecord(
                        tmEntryComic.getKey(),
                        "",
                        "",
                        0,
                        true);                 //Item marked for deletion?
                sbJobFileRecords.append(sLine);

                //Write progress to the screen log:
                sLogLine = sComicFolderName + " folder marked for deletion.\n";
                iFileCountProgressNumerator++;
                iProgressBarValue = Math.round((lByteProgressNumerator / (float) lByteProgressDenominator) * 100);
                globalClass.BroadcastProgress(true, sLogLine,
                        false, iProgressBarValue,
                        true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                continue; //Don't process any more code for this comic, move to the next comic.
            }

            //If we are here, this comic is not marked for deletion from the source.

            //Determine the folder which will be created to hold this comic:
            String sDestinationFolder = tmEntryComic.getValue()[INDEX_RECORD_ID]; //The individual destination comic folder name is the comic ID.

            //Prepare the data record:
            ItemClass_CatalogItem ciNewComic = new ItemClass_CatalogItem();

            //Comic page job file records creation. All of the file items in this loop belong to
            // the comic identified by the outer loop.
            for (Map.Entry<String, ItemClass_File> entryComicFile : tmComicFiles.entrySet()) {
                ItemClass_File fileItem = entryComicFile.getValue();


                if(fileItem.sFileOrFolderName.equals(GlobalClass.STRING_COMIC_XML_FILENAME)){
                    //The xml file in this case is created by a custom tool that downloads comics
                    // from ukkaF (string [reversed] to avoid buhtig crawlers). This file was
                    //  examined during the import folder examination process and is no longer
                    //  needed to complete the import. If this is a move operation, mark the file
                    //  for deletion.
                    if (giMoveOrCopy == GlobalClass.MOVE) {
                        String sLine = Worker_LocalFileTransfer.CreateJobFileRecord(
                                fileItem.sUri,
                                "",
                                "",
                                fileItem.lSizeBytes,
                                true);                 //Item marked for deletion?
                        sbJobFileRecords.append(sLine);
                    }
                    //Write progress to the screen log:
                    sLogLine = sComicFolderName + "\\" + fileItem.sFileOrFolderName + " will be deleted - XML file no longer needed.\n";
                    iFileCountProgressNumerator++;
                    iProgressBarValue = Math.round((lByteProgressNumerator / (float) lByteProgressDenominator) * 100);
                    globalClass.BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    continue; //Don't do anything else related to the comic import with this XML file.
                }

                ciNewComic.iFile_Count++;
                ciNewComic.iComicPages++;
                ciNewComic.iComic_Max_Page_ID++;

                String sSourceFileName = fileItem.sFileOrFolderName;
                String sDestinationFileName = GlobalClass.JumbleFileName(sSourceFileName);
                if (ciNewComic.sFilename.equals("")) {
                    //Set the Thumbnail file to the first page:
                    ciNewComic.sFilename = sDestinationFileName;
                    ciNewComic.sThumbnail_File = sDestinationFileName;
                }
                ciNewComic.lSize += fileItem.lSizeBytes;

                //Write instruction to the job file buffer to import this comic page to the comic
                //  folder:
                String sLine = Worker_LocalFileTransfer.CreateJobFileRecord(
                        fileItem.sUri,
                        sDestinationFolder,
                        sDestinationFileName,
                        fileItem.lSizeBytes,
                        false);
                sbJobFileRecords.append(sLine);

                //Write progress to the screen log:
                sLogLine = sComicFolderName + "\\" + fileItem.sFileOrFolderName + " will be imported.\n";
                iFileCountProgressNumerator++;
                iProgressBarValue = Math.round((lByteProgressNumerator / (float) lByteProgressDenominator) * 100);
                globalClass.BroadcastProgress(true, sLogLine,
                        false, iProgressBarValue,
                        true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

            } //End comic page collation Loop.

            //If this is a move operation, mark the source folder for deletion, as the local file
            // import worker will not do it:
            if (giMoveOrCopy == GlobalClass.MOVE) {
                String sLine = Worker_LocalFileTransfer.CreateJobFileRecord(
                        tmEntryComic.getKey(),
                        "",
                        "",
                        0,
                        true);                 //Item marked for deletion?
                sbJobFileRecords.append(sLine);
            }

            //Build out the catalog record for this comic:
            ciNewComic.iMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
            ciNewComic.sItemID = tmEntryComic.getValue()[INDEX_RECORD_ID]; //The individual comic folder is the comic ID.
            ciNewComic.sTitle = sComicFolderName;
            ciNewComic.sTags = tmEntryComic.getValue()[INDEX_COMIC_TAGS]; //Get the tags.
            ciNewComic.aliTags = GlobalClass.getTagIDsFromTagIDString(ciNewComic.sTags);
            ciNewComic.iGrade = Integer.parseInt(tmEntryComic.getValue()[INDEX_COMIC_GRADE]); //Get the grade.
            ciNewComic.sFolder_Name = sDestinationFolder;
            //Create a timestamp to be used to create the data record:
            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            ciNewComic.dDatetime_Last_Viewed_by_User = dTimeStamp;
            ciNewComic.dDatetime_Import = dTimeStamp;
            ciNewComic.sSource = tmEntryComic.getValue()[INDEX_COMIC_SOURCE];
            ciNewComic.sComicParodies = tmEntryComic.getValue()[INDEX_COMIC_PARODY];
            ciNewComic.sComicArtists = tmEntryComic.getValue()[INDEX_COMIC_ARTIST];

            //Add comic catalog record to arraylist for adding to the comics catalog once all
            // potential new comics are examined, job file is ready to go, etc:
            alci_NewCatalogItems.add(ciNewComic);

        } //End NHComics (plural) Import Loop.

        //The below call will add the records to both the catalog contents file
        //  and memory. Do this before copying or moving files so that the files
        //  don't become "lost" inside the program storage:
        globalClass.CatalogDataFile_CreateNewRecords(alci_NewCatalogItems);


        //Prepare the job file from a StringBuilder configured in the loop above:
        String sJobDateTime = GlobalClass.GetTimeStampFileSafe();
        String sJobFileName = "Job_" + sJobDateTime + ".txt";

        try {

            //Inform user of preparation of worker:
            String sMoveOrCopy = GlobalClass.gsMoveOrCopy[giMoveOrCopy];
            sLogLine = "\n\nPreparing job file. ";
            globalClass.BroadcastProgress(true, sLogLine,
                    false, iProgressBarValue,
                    true, "File " + iFileCountProgressNumerator + "/" + iFileCountProgressDenominator,
                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

            //Create a file with a listing of the files to be copied/moved:
            Uri uriJobFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, GlobalClass.gUriJobFilesFolder, MimeTypes.BASE_TYPE_TEXT, sJobFileName);
            if(uriJobFile == null){
                sMessage = "Could not create job file.";
                LogThis("doWork()", sMessage, null);
                return Result.failure(DataErrorMessage(sMessage));
            }
            OutputStream osJobFile = GlobalClass.gcrContentResolver.openOutputStream(uriJobFile, "wt");
            if(osJobFile == null){
                sMessage = "Could not open output stream to job file.";
                LogThis("doWork()", sMessage, null);
                return Result.failure(DataErrorMessage(sMessage));
            }
            BufferedWriter bwJobFile = new BufferedWriter(new OutputStreamWriter(osJobFile));
            //Write the data header:
            String sConfig = "MediaCategory:" + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_COMICS] + "\t"
                    + "MoveOrCopy:" + sMoveOrCopy + "\t"
                    + "TotalSize:" + lTotalImportSize + "\t"
                    + "FileCount:" + alFileList.size() + "\n";
            bwJobFile.write(sConfig);
            bwJobFile.write(sbJobFileRecords.toString());
            bwJobFile.flush();
            bwJobFile.close();
            osJobFile.flush();
            osJobFile.close();



        } catch (Exception e){
            globalClass.BroadcastProgress(true, "Problem with writing the job file.\n" + e.getMessage(),
                    false, iProgressBarValue,
                    false, "",
                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            globalClass.gbImportExecutionRunning = false;
            globalClass.gbImportExecutionFinished = true;
            return Result.failure();
        }
        //Write next behavior to the screen log:
        sLogLine = "\nStarting worker to process job file. This 'job file creation worker' will end and 'job file process worker' will continue in the background.\n"
                + "Files will appear in the catalog as the worker progresses.\n"
                + "Refresh the catalog viewer (exit/re-enter, change sort direction) to view newly-added files.\n";
        globalClass.BroadcastProgress(true, sLogLine,
                false, iProgressBarValue,
                true, iFileCountProgressNumerator + "/" + iFileCountProgressDenominator + " files written to job file",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

        //Build-out data to send to the worker:
        Data dataLocalFileTransfer = new Data.Builder()
                .putString(Worker_LocalFileTransfer.KEY_ARG_JOB_REQUEST_DATETIME, sJobDateTime)
                .putString(Worker_LocalFileTransfer.KEY_ARG_JOB_FILE, sJobFileName)
                .build();
        OneTimeWorkRequest otwrLocalFileTransfer = new OneTimeWorkRequest.Builder(Worker_LocalFileTransfer.class)
                .setInputData(dataLocalFileTransfer)
                .addTag(Worker_LocalFileTransfer.WORKER_LOCAL_FILE_TRANSFER_TAG) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(otwrLocalFileTransfer);

        globalClass.BroadcastProgress(true, "Job file creation worker operation complete.\n\n",
                false, iProgressBarValue,
                false, "",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);


        globalClass.gbImportExecutionRunning = false;
        globalClass.gbImportExecutionFinished = true;
        return Result.success();
    }

    private Data DataErrorMessage(String sMessage){
        return new Data.Builder()
                .putString(GlobalClass.FAILURE_MESSAGE, sMessage)
                .build();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Import_ImportComicFolders:" + sRoutine, sMessage);
    }

}
