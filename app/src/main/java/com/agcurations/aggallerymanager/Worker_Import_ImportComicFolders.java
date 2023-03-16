package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Import_ImportComicFolders extends Worker {

    public static final String TAG_WORKER_IMPORT_IMPORTCOMICFOLDERS = "com.agcurations.aggallermanager.tag_worker_import_importcomicfolders";

    int giMoveOrCopy;
    int giComicImportSource;

    public Worker_Import_ImportComicFolders(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        giMoveOrCopy = getInputData().getInt(GlobalClass.EXTRA_IMPORT_FILES_MOVE_OR_COPY, -1);
        giComicImportSource = getInputData().getInt(GlobalClass.EXTRA_COMIC_IMPORT_SOURCE, -1);
    }

    @NonNull
    @Override
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

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;

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

        String sMessage;
        ArrayList<ItemClass_CatalogItem> alci_NewCatalogItemsToUpdate = new ArrayList<>();
        for(Map.Entry<String, String[]> tmEntryComic: tmComics.entrySet()) {
            //Create a folder and import files for this comic:

            String sDestinationFolder = tmEntryComic.getValue()[INDEX_RECORD_ID]; //The individual destination comic folder name is the comic ID.

            Uri uriDestination = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].toString(), sDestinationFolder);

            if (!GlobalClass.CheckIfFileExists(uriDestination)) {
                try {
                    uriDestination = GlobalClass.CreateDirectory(uriDestination);
                } catch (Exception e){
                    sMessage = "Could not locate parent directory of destination folder in order to create destination folder. Destination folder: " + sDestinationFolder;
                    LogThis("doWork()", sMessage, e.getMessage());
                    uriDestination = null;
                }
                if (uriDestination == null) {
                    //Unable to create directory
                    sMessage = "Unable to create destination folder " + sDestinationFolder + " at "
                            + GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS] + "\n";
                    globalClass.BroadcastProgress(true, sMessage,
                            false, iProgressBarValue,
                            true, "Operation halted.",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    globalClass.gbImportExecutionRunning = false;
                    globalClass.gbImportExecutionFinished = true;
                    return Result.failure();
                } else {
                    globalClass.BroadcastProgress(true, "Destination folder created: " + uriDestination + "\n",
                            false, iProgressBarValue,
                            false, "",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                }
            } else {
                globalClass.BroadcastProgress(true, "Destination folder verified: " + uriDestination + "\n",
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

            //The below call should add the record to both the catalog contents file
            //  and memory. Do this before copying or moving files so that the files
            //  don't become "lost" inside the program storage:
            try {
                globalClass.CatalogDataFile_CreateNewRecord(ciNewComic);
            } catch (Exception e) {
                e.printStackTrace();
                globalClass.gbImportExecutionRunning = false;
                globalClass.gbImportExecutionFinished = true;
                return Result.failure();
            }

            //Find comic files belonging to this comic and put them in a tree map for sorting.
            TreeMap<String, ItemClass_File> tmComicFiles = new TreeMap<>();
            for (ItemClass_File fileItem : alFileList) {
                if (fileItem.sUriParent.equals(tmEntryComic.getKey())) {
                    tmComicFiles.put(fileItem.sFileOrFolderName, fileItem);
                }
            }

            //Comic page import
            for (Map.Entry<String, ItemClass_File> entryComicFile : tmComicFiles.entrySet()) {
                ItemClass_File fileItem = entryComicFile.getValue();

                String sLogLine;
                Uri uriSourceFile = Uri.parse(fileItem.sUri);

                if(fileItem.sFileOrFolderName.equals(GlobalClass.STRING_COMIC_XML_FILENAME)){
                    if (giMoveOrCopy == GlobalClass.MOVE) {
                        if(GlobalClass.CheckIfFileExists(uriSourceFile)) {
                            try {
                                if (!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriSourceFile)) {
                                    sLogLine = "Could not delete xml file from source folder.\n";
                                } else {
                                    sLogLine = "Success deleting xml file from source folder.\n";
                                }
                            } catch (FileNotFoundException e) {
                                sLogLine = "Could not delete xml file from source folder.\n";
                            }
                        } else {
                            sLogLine = "Could not delete xml file from source folder.\n";
                        }
                        globalClass.BroadcastProgress(true, sLogLine,
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

                try {
                    //Write next behavior to the screen log:
                    sLogLine = "Attempting " + GlobalClass.gsMoveOrCopy[giMoveOrCopy].toLowerCase();
                    sLogLine = sLogLine + " of file " + fileItem.sFileOrFolderName + " to destination...";
                    globalClass.BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                    if (uriSourceFile == null) {
                        globalClass.BroadcastProgress(true, "Problem with copy/move operation of file " + fileItem.sFileOrFolderName,
                                false, iProgressBarValue,
                                false, "",
                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                        lProgressNumerator += fileItem.lSizeBytes;
                        continue;
                    }

                    //Execute move or copy:
                    Uri uriCopiedOrMovedDocument;
                    if(giMoveOrCopy == GlobalClass.MOVE){
                        //Move Operation
                        Uri uriSourceParent = GlobalClass.GetParentUri(uriSourceFile);
                        if(uriSourceParent == null){
                            sMessage = "Could note determine source parent.";
                            Log.d("Worker_Import_ComicFolders", sMessage);
                            continue;
                        }
                        uriCopiedOrMovedDocument = DocumentsContract.moveDocument(
                                GlobalClass.gcrContentResolver,
                                uriSourceFile,
                                uriSourceParent,
                                uriDestination);
                    } else {
                        //Copy operation
                        uriCopiedOrMovedDocument = DocumentsContract.copyDocument(
                                GlobalClass.gcrContentResolver,
                                uriSourceFile,
                                uriDestination);

                    }
                    //Rename the copied or moved document:
                    if(uriCopiedOrMovedDocument != null){
                        Uri uriCopiedOrMovedRenamedDocument = DocumentsContract.renameDocument(GlobalClass.gcrContentResolver, uriCopiedOrMovedDocument, sNewFilename);
                        if(uriCopiedOrMovedRenamedDocument == null) {
                            sMessage = "Trouble identifying copied/moved file: " + uriCopiedOrMovedDocument;
                            Log.d("Worker_Import_ComicFolders", sMessage);
                        }
                    } else {
                        sMessage = "Trouble copying or moving file: " + uriSourceFile;
                        Log.d("Worker_Import_ComicFolders", sMessage);
                    }




                } catch (Exception e) {
                    globalClass.BroadcastProgress(true, "Problem with copy/move operation.\n\n" + e.getMessage(),
                            false, iProgressBarValue,
                            false, "",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                }


            } //End comic page import Loop.

            //Mark record for update with the file count, size, etc:
            alci_NewCatalogItemsToUpdate.add(ciNewComic);


            //Delete the comic folder from source directory if required:
            if (giMoveOrCopy == GlobalClass.MOVE) {
                Uri uriComicSourceFolder; //This is not the folder that the user selected, but rather
                //a comic folder within that folder.
                String sLogLine;
                uriComicSourceFolder = Uri.parse(tmEntryComic.getKey());

                try {
                    if (DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriComicSourceFolder)) {
                        sLogLine = "Success deleting folder for comic '" + tmEntryComic.getValue()[INDEX_COMIC_NAME] + "'.\n";
                    } else {
                        sLogLine = "Could not delete folder for comic '" + tmEntryComic.getValue()[INDEX_COMIC_NAME] + "'.\n";
                    }
                } catch (FileNotFoundException e) {
                    sLogLine = "Could not delete folder for comic '" + tmEntryComic.getValue()[INDEX_COMIC_NAME] + "'.\n";
                }

                globalClass.BroadcastProgress(true, sLogLine,
                        true, iProgressBarValue,
                        true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            }

        } //End NHComics (plural) Import Loop.

        //Write data again to the CatalogContents file and memory with updated data regarding:
        // file counts, collection size, max page number, thumbnail filename, etc:
        globalClass.CatalogDataFile_UpdateRecords(alci_NewCatalogItemsToUpdate);


        globalClass.BroadcastProgress(true, "Operation complete.\n",
                true, iProgressBarValue,
                true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);


        globalClass.gbImportExecutionRunning = false;
        globalClass.gbImportExecutionFinished = true;

        return Result.success();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Import_ImportComicFolders:" + sRoutine, sMessage);
    }

}
