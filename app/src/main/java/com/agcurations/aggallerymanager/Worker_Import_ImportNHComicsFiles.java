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

public class Worker_Import_ImportNHComicsFiles extends Worker {

    public static final String TAG_WORKER_IMPORT_IMPORTNHCOMICSFILES = "com.agcurations.aggallermanager.tag_worker_import_importnhcomicsfiles";

    int giMoveOrCopy;
    int giComicImportSource;

    public Worker_Import_ImportNHComicsFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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


        TreeMap<String, String[]> tmNHComicIDs = new TreeMap<>(); //Map NH_Comic_Downloader ComicID to a record ID/folder and also grab the comic title.

        int INDEX_RECORD_ID = 0;
        int INDEX_COMIC_NAME = 1;
        int INDEX_COMIC_TAGS = 2;
        int INDEX_COMIC_GRADE = 3;
        //If NH_Comic_Downloaded, loop and find all of the comic IDs:
        for(ItemClass_File fileItem: alFileList) {
            if(fileItem.sFileOrFolderName.matches(GlobalClass.gsNHComicCoverPageFilter)){
                String sComicID = Service_Import.GetNHComicID(fileItem.sFileOrFolderName);
                lProgressDenominator = lProgressDenominator - fileItem.lSizeBytes; //We don't copy over the cover page.
                String sRecordID = globalClass.getNewCatalogRecordID(GlobalClass.MEDIA_CATEGORY_COMICS);
                String sComicName = Service_Import.GetNHComicNameFromCoverFile(fileItem.sFileOrFolderName);
                String sComicTags = GlobalClass.formDelimitedString(fileItem.aliProspectiveTags, ",");
                int iGrade = fileItem.iGrade;
                if(tmNHComicIDs.containsKey(sComicID)){
                    //If this is merely a duplicate comic selected during the import, not if it already exists in the catalog.
                    //If it already exists in the catalog, it is on the user to resolve.
                    globalClass.problemNotificationConfig("Skipping Comic ID " + sComicID + ". Duplicate comic.", Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                } else {
                    tmNHComicIDs.put(sComicID, new String[]{sRecordID, sComicName, sComicTags, String.valueOf(iGrade)});
                }
            }
        }

        for(Map.Entry<String, String[]> tmEntryNHComic: tmNHComicIDs.entrySet()) {
            //Loop and import files:

            String sDestinationFolder = tmEntryNHComic.getValue()[INDEX_RECORD_ID]; //The individual comic folder is the comic ID.

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
            ciNewComic.sItemID = tmEntryNHComic.getValue()[INDEX_RECORD_ID]; //The individual comic folder is the comic ID.
            ciNewComic.sTitle = tmEntryNHComic.getValue()[INDEX_COMIC_NAME]; //Get the name.
            ciNewComic.sTags = tmEntryNHComic.getValue()[INDEX_COMIC_TAGS]; //Get the tags.
            ciNewComic.iGrade = Integer.parseInt(tmEntryNHComic.getValue()[INDEX_COMIC_GRADE]); //Get the grade. Either default or selected by user.
            ciNewComic.sFolder_Name = sDestinationFolder;
            //Create a timestamp to be used to create the data record:
            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            ciNewComic.dDatetime_Last_Viewed_by_User = dTimeStamp;
            ciNewComic.dDatetime_Import = dTimeStamp;
            ciNewComic.sSource = "https:/nhentai.net/g/" + tmEntryNHComic.getKey() + "/";

            ItemClass_File icfCoverPageFile = null;

            for (ItemClass_File fileItem : alFileList) {

                if(fileItem.sFileOrFolderName.matches("^" + tmEntryNHComic.getKey() + "_Cover.+")) {
                    //Preserve the cover page file item so that it can be deleted at the after the
                    // other files are imported. No particular reason for saving this until the end.
                    icfCoverPageFile = fileItem;

                }

                if(fileItem.sFileOrFolderName.matches("^" + tmEntryNHComic.getKey() + "_Page.+")) {

                    //Make sure that it is not a duplicate page.
                    if (!isPageDuplicate(fileItem.sFileOrFolderName)) {
                        ciNewComic.iFile_Count++;
                        String sNewFilename = fileItem.sFileOrFolderName;
                        sNewFilename = sNewFilename.substring(sNewFilename.indexOf("_")); //Get rid of the NH comic ID.
                        sNewFilename = ciNewComic.sItemID + sNewFilename; //Add on the sequenced comic record ID.
                        sNewFilename = GlobalClass.JumbleFileName(sNewFilename);
                        if (fileItem.sFileOrFolderName.contains("_Page_001")) {
                            //Set the Thumbnail file to the first page (which is a duplicate of the
                            // cover page but of a different name):
                            ciNewComic.sFilename = sNewFilename;
                            ciNewComic.sThumbnail_File = sNewFilename;
                        }
                        ciNewComic.lSize += fileItem.lSizeBytes;

                        Uri uriSourceFile;
                        String sLogLine;
                        InputStream inputStream;
                        OutputStream outputStream;

                        uriSourceFile = Uri.parse(fileItem.sUri);
                        DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
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

                    } else {
                        //NHComic page is a duplicate.
                        globalClass.problemNotificationConfig("File " + fileItem.sFileOrFolderName + " appears to be a duplicate. Skipping import of this file.\n", Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                    }

                } //End if match with Page regex containing ComicID.

            } //End NHComic Import Loop.

            //Next add the data to the catalog file and memory:
            //The below call should add the record to both the catalog contents file
            //  and memory:
            globalClass.CatalogDataFile_CreateNewRecord(ciNewComic);

            //Delete the cover page from source folder if required:
            if(icfCoverPageFile != null) {
                if (giMoveOrCopy == GlobalClass.MOVE) {
                    Uri uriSourceFile;
                    String sLogLine;

                    uriSourceFile = Uri.parse(icfCoverPageFile.sUri);
                    DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
                    if(dfSource != null) {
                        if (!dfSource.delete()) {
                            sLogLine = "Could not delete cover page duplicate file (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                        } else {
                            sLogLine = "Success deleting cover page duplicate file.\n";
                        }
                    } else {
                        sLogLine = "Could not delete cover page duplicate file (deletion is required step of 'move' operation, otherwise it is a 'copy' operation).\n";
                    }
                    globalClass.BroadcastProgress(true, sLogLine,
                            true, iProgressBarValue,
                            true, lProgressNumerator / 1024 + " / " + lProgressDenominator / 1024 + " KB",
                            Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                }
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
