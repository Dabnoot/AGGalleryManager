package com.agcurations.aggallerymanager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
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
                .putInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[globalClass.giSelectedCatalogMediaCategory],
                        GlobalClass.SORT_BY_DATETIME_IMPORTED)
                .putBoolean(GlobalClass.gsCatalogViewerPreferenceNameSortAscending[globalClass.giSelectedCatalogMediaCategory],
                        false)
                .apply();

        long lProgressNumerator = 0L;
        long lProgressDenominator;
        int iProgressBarValue = 0;
        long lTotalImportSize = 0L;
        long lLoopBytesRead;


        ContentResolver contentResolver = getApplicationContext().getContentResolver();

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

        for(Map.Entry<String, String[]> tmEntryComic: tmComics.entrySet()) {
            //Create a folder and import files for this comic:

            String sDestinationFolder = tmEntryComic.getValue()[INDEX_RECORD_ID]; //The individual destination comic folder name is the comic ID.

            File fDestination = new File(
                    globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath() + File.separator +
                            sDestinationFolder);

            if (!fDestination.exists()) {
                if (!fDestination.mkdir()) {
                    //Unable to create directory
                    globalClass.BroadcastProgress(true, "Unable to create destination folder at: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            true, "Operation halted.",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    globalClass.gbImportExecutionRunning = false;
                    globalClass.gbImportExecutionFinished = true;
                    return Result.failure();
                } else {
                    globalClass.BroadcastProgress(true, "Destination folder created: " + fDestination.getPath() + "\n",
                            false, iProgressBarValue,
                            false, "",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                }
            } else {
                globalClass.BroadcastProgress(true, "Destination folder verified: " + fDestination.getPath() + "\n",
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
                DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);

                if(fileItem.sFileOrFolderName.equals(GlobalClass.STRING_COMIC_XML_FILENAME)){
                    if (giMoveOrCopy == GlobalClass.MOVE) {
                        if(dfSource != null) {
                            if (!dfSource.delete()) {
                                sLogLine = "Could not delete xml file from source folder.\n";
                            } else {
                                sLogLine = "Success deleting xml file from source folder.\n";
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

                InputStream inputStream;
                OutputStream outputStream;

                try {
                    //Write next behavior to the screen log:
                    sLogLine = "Attempting " + GlobalClass.gsMoveOrCopy[giMoveOrCopy].toLowerCase();
                    sLogLine = sLogLine + " of file " + fileItem.sFileOrFolderName + " to destination...";
                    globalClass.BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                    if (dfSource == null) {
                        globalClass.BroadcastProgress(true, "Problem with copy/move operation of file " + fileItem.sFileOrFolderName,
                                false, iProgressBarValue,
                                false, "",
                                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                        lProgressNumerator += fileItem.lSizeBytes;
                        continue;
                    }
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
                            globalClass.BroadcastProgress(false, "",
                                    true, iProgressBarValue,
                                    true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                        }
                    }
                    outputStream.flush();
                    outputStream.close();
                    sLogLine = "Copy success.\n";
                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    globalClass.BroadcastProgress(true, sLogLine,
                            false, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

                    //This file has now been copied.

                    //Delete the source file if 'Move' specified:
                    boolean bUpdateLogOneMoreTime = false;
                    if (giMoveOrCopy == GlobalClass.MOVE) {
                        bUpdateLogOneMoreTime = true;
                        if (!dfSource.delete()) {
                            sLogLine = "Could not delete source file after copy (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                        } else {
                            sLogLine = "Success deleting source file after copy.\n";
                        }
                    }

                    iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);

                    globalClass.BroadcastProgress(bUpdateLogOneMoreTime, sLogLine,
                            true, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);


                } catch (Exception e) {
                    globalClass.BroadcastProgress(true, "Problem with copy/move operation.\n\n" + e.getMessage(),
                            false, iProgressBarValue,
                            false, "",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                }


            } //End comic page import Loop.

            //Update the record with the file count, size, etc:
            globalClass.CatalogDataFile_UpdateRecord(ciNewComic);


            //Delete the comic folder from source directory if required:
            if (giMoveOrCopy == GlobalClass.MOVE) {
                Uri uriComicSourceFolder; //This is not the folder that the user selected, but rather
                //a comic folder within that folder.
                String sLogLine;
                uriComicSourceFolder = Uri.parse(tmEntryComic.getKey());
                DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriComicSourceFolder);
                sLogLine = "Could not delete folder for comic '" + tmEntryComic.getValue()[INDEX_COMIC_NAME] + "'.\n";
                if(dfSource != null) {
                    if (dfSource.delete()) {
                        sLogLine = "Success deleting folder for comic '" + tmEntryComic.getValue()[INDEX_COMIC_NAME] + "'.\n";
                    }
                }
                globalClass.BroadcastProgress(true, sLogLine,
                        true, iProgressBarValue,
                        true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
            }

        } //End NHComics (plural) Import Loop.

        //Modify viewer settings to show the newly-imported files:
        /*globalClass.giCatalogViewerSortBySetting[GlobalClass.MEDIA_CATEGORY_COMICS] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        globalClass.gbCatalogViewerSortAscending[GlobalClass.MEDIA_CATEGORY_COMICS] = false;*/ //Handled in onHandleIntent.

        globalClass.BroadcastProgress(true, "Operation complete.\n",
                true, iProgressBarValue,
                true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);


        globalClass.gbImportExecutionRunning = false;
        globalClass.gbImportExecutionFinished = true;

        return Result.success();
    }

}
