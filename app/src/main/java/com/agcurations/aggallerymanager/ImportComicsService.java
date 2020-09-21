package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ImportComicsService extends IntentService {

    //Global Constants:
    public static final String IMPORT_METHOD = "IMPORT_METHOD";
    public static final int IMPORT_METHOD_FOLDER = 0;
    public static final int IMPORT_METHOD_FILE = 1;
    public static final String IMPORT_URI = "IMPORT_URI";

    public static final String UPDATE_LOG_BOOLEAN = "UPDATE_LOG_BOOLEAN";
    public static final String LOG_LINE_STRING = "LOG_LINE_STRING";
    public static final String UPDATE_PERCENT_COMPLETE_BOOLEAN = "UPDATE_PERCENT_COMPLETE_BOOLEAN";
    public static final String PERCENT_COMPLETE_INT = "PERCENT_COMPLETE_INT";
    public static final String UPDATE_STAGE_INDICATION_BOOLEAN = "UPDATE_STAGE_INDICATION_BOOLEAN";
    public static final String STAGE_STRING = "STAGE_STRING";

    //Global Variables:
    Uri guriImportUri;
    private FileWriter gfwImportLogFile;
    private boolean bLogWriterErrorReported = false;

    private GlobalClass globalClass;

    public ImportComicsService() {
        super("ImportComicsService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        globalClass = (GlobalClass) getApplicationContext();

        //Create a log file to track the import:
        String sTemp = globalClass.GetTimeStampFileSafe() + "_ImportLog.txt";
        File fImportLog = new File(GlobalClass.gvfComicLogsFolder + File.separator + sTemp);
        try {
            gfwImportLogFile = new FileWriter(fImportLog, true);
        } catch (Exception e){
            String s = "Problem creating log file to record Import.\n" + e.getMessage();
            BroadcastProgress(true, s,
                    false, 0,
                    false, "");

        }

        //Get the Uri for the import object from the intent:
        String sUriString = intent.getStringExtra(IMPORT_URI);
        guriImportUri = Uri.parse(sUriString);


        //Begin the import processing:
        if(intent.getIntExtra(IMPORT_METHOD,-1) == IMPORT_METHOD_FOLDER){
            Import_Operation_Process_File_List();
        //} else {
            //Import a zip file.

        }

        try {
            gfwImportLogFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        BroadcastProgress(true, "Import complete.",
                true, 100,
                true, "Complete.");

    }


    public void BroadcastProgress(boolean bUpdateLog, String sLogLine,
                                  boolean bUpdatePercentComplete, int iAmountComplete,
                                  boolean bUpdateStageIndication, String sStage){

        //Broadcast a message to be picked-up by the Import Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ImportComicsActivity.ImportResponseReceiver.IMPORT_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        broadcastIntent.putExtra(UPDATE_LOG_BOOLEAN, bUpdateLog);
        broadcastIntent.putExtra(LOG_LINE_STRING, sLogLine);
        broadcastIntent.putExtra(UPDATE_PERCENT_COMPLETE_BOOLEAN, bUpdatePercentComplete);
        broadcastIntent.putExtra(PERCENT_COMPLETE_INT, iAmountComplete);
        broadcastIntent.putExtra(UPDATE_STAGE_INDICATION_BOOLEAN, bUpdateStageIndication);
        broadcastIntent.putExtra(STAGE_STRING, sStage);

        sendBroadcast(broadcastIntent);

    }


    public void WriteLogLine(String sLine, boolean bBroadcastLogLine){
        String s = "";
        try {
            //Create a timestamp for the log line:
            s = globalClass.GetTimeStampReadReady() + ": " + sLine + "\n";
            gfwImportLogFile.append(s);

        } catch (Exception e){
            if(!bLogWriterErrorReported) { //Don't keep repeating the error for each time we attempt to write a line during operations.
                s = "Problem writing to log file to record Import.\n" + e.getMessage();
                bBroadcastLogLine = true;
                bLogWriterErrorReported = true;
            }
        }

        //Broadcast the log update back to the ImportComicsActivity?
        if(bBroadcastLogLine){
            BroadcastProgress(true, s,
                    false, 0,
                    false, "");
        }


    }

    public void Import_Operation_Process_File_List(){
        TreeMap<Integer, String[]> tmCatalogComicList = globalClass.gvtmCatalogComicList;




        //================================
        //=== Get filenames from folder.
        //================================

        //https://stackoverflow.com/questions/41096332/issues-traversing-through-directory-hierarchy-with-android-storage-access-framew
        ContentResolver contentResolver = this.getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(guriImportUri, DocumentsContract.getTreeDocumentId(guriImportUri));
        WriteLogLine( "Analyzing folder: " + childrenUri.getPath(), true );

        // Keep track of our directory hierarchy
        List<Uri> dirNodes = new LinkedList<>();
        dirNodes.add(childrenUri);

        childrenUri = dirNodes.remove(0); // get the item from top
        String sSortOrder = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC"; //Sort does not appear to work.
        Cursor cImport = contentResolver.query(childrenUri, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, sSortOrder);

        if(cImport == null){
            return;
        }

        int iTotalFilesToCopy = 0;
        int iProgressCounter;
        int iProgressDenominator;
        int iProgressBarValue;
        String sStageName;

        try {

            // Create an array to map image file names to cursor index:
            //https://stackoverflow.com/questions/27402861/creating-an-array-that-stores-strings-and-integers-in-java/27403133
            // The TreeMap  is naturally sorted
            TreeMap<String, Integer> tmImportSourceFileList = new TreeMap<>();

            //Provide user with progress update:
            iProgressCounter = 0;
            iProgressBarValue = 0;
            iProgressDenominator = cImport.getCount();
            BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, "Obtaining file names...");
            sStageName = "Folder Content Evaluation";

            WriteLogLine("Selected folder includes " + iProgressDenominator + " files.", true);

            while (cImport.moveToNext()) {
                final String docId = cImport.getString(0);
                final String mime = cImport.getString(2);
                if (mime.contains("image")) {
                    tmImportSourceFileList.put(docId.substring(4), cImport.getPosition());
                }
                iProgressCounter++;
                if(iProgressCounter % 5 == 0){//Update after every 5th page processed
                    iProgressBarValue = Math.round((iProgressCounter / (float) iProgressDenominator) * 100);
                    BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            false, "");
                }
            }
            WriteLogLine("Found " + tmImportSourceFileList.size() + " image files.",true);
            BroadcastProgress(false, "",
                    true, 100,
                    true, sStageName + " completed.");


            //==========================================
            //=== Examine files. Create list of comics.
            //==========================================

            TreeMap<Integer, String[]> tmImportComicList = new TreeMap<>();
            TreeMap<String, Integer> tmImportSourceFileDuplicateList = new TreeMap<>();
            // using for-each loop for
            // iteration over TreeMap.entrySet()
            //Prefix convention: "######_Cover*.<ext>"
            //Prefix convention: "######_Page*.<ext>"
            String sAbsolutePath;
            String sFileName;
            int iComicID;
            String sComicName = "";
            int iFileCount = 0;
            int iPageID = -1;
            int iLPPageID = -1;
            String sMissingPages = "";
            int iLPComicID = 0;
            boolean bDuplicateInCatalog = false;
            String[] sImportComicListRecord = new String[GlobalClass.ComicRecordFields.length + 1];
            int DUPLICATE_IN_CATALOG_INDEX = GlobalClass.ComicRecordFields.length; //Recall, index is 0-based.


            long lFileSize;
            long lComicSize = 0; //The storage size of a comic in KB.

            //Iterate over the found image files and look for files that match the
            //  prefix convention. Create a list of comics for import, and a list of duplicate
            //  comics for removal from the selected folder.

            //Provide user with progress update:
            iProgressCounter = 0;
            iProgressBarValue = 0;
            iProgressDenominator = tmImportSourceFileList.size();
            BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, "Analyzing image files...");
            sStageName = "Image File Analysis";

            for (Map.Entry<String, Integer>
                    entry : tmImportSourceFileList.entrySet()) {
                //Get the path to the file:
                sAbsolutePath = entry.getKey();
                //Get the filename of the file:
                File f = new File(sAbsolutePath);
                sFileName = f.getName();

                lFileSize = f.length() / 1024; //Keep the file size in KB

                //B
                //Does the file name match the comic ID prefix convention?
                if (sFileName.matches("^\\d{1,7}_(Cover|Page).+")) { //Starts with 1 to 7 digits followed by an underscore

                    //C
                    //Check to see if this is a duplicate file:
                    if (isPageDuplicate(sAbsolutePath)) {
                        //If it is a duplicate page, add the file to the "duplicate files list" for possible deletion at a later stage.
                        tmImportSourceFileDuplicateList.put(entry.getKey(), entry.getValue());
                        WriteLogLine("Found duplicate page: " + entry.getKey(),true);

                    } else {
                        //Get the comic ID:
                        iComicID = GetComicID(sFileName);

                        //Temporarily get the page ID here so that we don't have to do it twice later.
                        if (!isCoverFile(sFileName)) {
                            // It is not the cover page, so it should have a page ID.
                            int i;
                            i = sFileName.lastIndexOf(".");
                            iPageID = Integer.parseInt(sFileName.substring(i - 3, i));
                        }

                        //D
                        if (!(iComicID == iLPComicID)) {
                            //New comic detected. Clear the fields.
                            sImportComicListRecord = new String[GlobalClass.ComicRecordFields.length + 1];
                            for (int i = 0; i < GlobalClass.ComicRecordFields.length; i++){
                                //Make sure that no fields are null.
                                //  This would become a problem later when we start adding records to the contents file.
                                sImportComicListRecord[i]="";
                            }
                                    //The record is +1 longer so that we can put a flag for "duplicate comic" status.
                            sImportComicListRecord[GlobalClass.COMIC_ID_INDEX] = String.valueOf(iComicID);
                            sComicName = "";
                            iTotalFilesToCopy += iFileCount;
                            iFileCount = 1;
                            sMissingPages = "";
                            bDuplicateInCatalog = false;

                            //Record the size of the comic thus far (we will not copy the cover page):
                            lComicSize = 0;

                            //G
                            //Attempt to obtain the comic name. The comic name page should always be the first comic:
                            if (isCoverFile(sFileName)) {
                                //If file is marked "Cover" and there is enough length to the filename to include the cover name.
                                sComicName = GetComicNameFromCoverFile(sFileName);
                                sImportComicListRecord[GlobalClass.COMIC_NAME_INDEX] = sComicName; //Comic name field
                                sImportComicListRecord[GlobalClass.COMIC_FOLDER_NAME_INDEX] =
                                        iComicID + "_" + sComicName; //Set the folder name
                                //Or, the path might end up too long and the folder would have to be renamed.
                                iPageID = 0;
                            } else {
                                //If there is no cover file (which would contain the comic name, and would be the first file)
                                sImportComicListRecord[GlobalClass.COMIC_NAME_INDEX] = ""; //Comic name field
                                sImportComicListRecord[GlobalClass.COMIC_FOLDER_NAME_INDEX] = String.valueOf(iComicID); //Folder name
                                sImportComicListRecord[GlobalClass.COMIC_THUMBNAIL_FILE_INDEX] = sFileName; //Set the thumbnail file.
                            }

                            //Apply an import timestamp:
                            Double dTimeStamp = globalClass.GetTimeStampFloat();
                            String sDateTime = dTimeStamp.toString();
                            sImportComicListRecord[GlobalClass.COMIC_DATETIME_IMPORT_INDEX] = sDateTime;
                            sImportComicListRecord[GlobalClass.COMIC_SOURCE_INDEX] = "nHentai.net";
                            //Must provide a value for the last read by user or there will be an error
                            //  during interpretation during user-selected sort:
                            sImportComicListRecord[GlobalClass.COMIC_DATETIME_LAST_READ_BY_USER_INDEX] = "0";

                            WriteLogLine("Found new comic: Comic ID: " + iComicID + ", Comic Name: " + sComicName + ".",true);

                            //J
                            //if the catalog contains the ComicID, mark as a duplicate:
                            for (Map.Entry<Integer, String[]>
                                    CatalogEntry : tmCatalogComicList.entrySet()) {
                                String sEntryComicID = CatalogEntry.getValue()[GlobalClass.COMIC_ID_INDEX];
                                if( Integer.parseInt(sEntryComicID) == iComicID){
                                //if (tmCatalogComicList.containsKey(iComicID)) {
                                    //if the catalog contains the ComicID, mark as a duplicate

                                    bDuplicateInCatalog = true;
                                    WriteLogLine("Found catalog comic duplicate: Comic ID: " + iComicID + ", Comic Name: " + sComicName + ".", true);

                                }
                            }

                            iLPPageID = iPageID;
                            iLPComicID = iComicID;

                        } else {

                            //F
                            //Check to is if this is NOT the next consecutive page. If not, build a "missing pages string".
                            if (iPageID != (iLPPageID + 1)) {
                                StringBuilder sb = new StringBuilder();

                                for (int i = iLPPageID + 1; i < iPageID; i++) {
                                    if(sMissingPages.length() == 0){
                                        sb.append(i);
                                    } else {
                                        sb.append(",");
                                        sb.append(+ i);
                                    }
                                    WriteLogLine("Comic is missing page: " + i + " from Comic ID " + iComicID + ", Comic Name " + sComicName + ".",true);
                                }
                                sMissingPages = sb.toString();

                            }

                            if(sImportComicListRecord[GlobalClass.COMIC_THUMBNAIL_FILE_INDEX].equals("")) {
                                //If the comic thumbnail page has not yet been set,
                                //  set the comic thumbnail to the first file (set it only once in our loop)
                                sImportComicListRecord[GlobalClass.COMIC_THUMBNAIL_FILE_INDEX] = sFileName; //Set the thumbnail file.
                            }
                            lComicSize += lFileSize;
                            iLPPageID = iPageID;
                            iFileCount++;
                        }

                        //Build the comic record:

                        sImportComicListRecord[GlobalClass.COMIC_FILE_COUNT_INDEX] = String.valueOf(iFileCount);
                        sImportComicListRecord[GlobalClass.COMIC_MAX_PAGE_ID_INDEX] = String.valueOf(iPageID);
                        sImportComicListRecord[GlobalClass.COMIC_MISSING_PAGES_INDEX] = sMissingPages;
                        sImportComicListRecord[GlobalClass.COMIC_SIZE_KB_INDEX] = String.valueOf(lComicSize);
                        sImportComicListRecord[DUPLICATE_IN_CATALOG_INDEX] = String.valueOf(bDuplicateInCatalog);


                        //Put the comic record into the tree map object (it's basically a set of records):
                        tmImportComicList.put(iComicID, sImportComicListRecord);


                    }//if not a duplicate file
                }//if file matches prefix convention

                //Provide user with progress update:
                iProgressCounter++;
                if(iProgressCounter % 5 == 0){  //Update after every 5th page processed
                    iProgressBarValue = Math.round((iProgressCounter / (float) iProgressDenominator) * 100);
                    BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            false, "");
                }

            }//Loop while there is still an image file to examine in the import folder.

            WriteLogLine("Found " + tmImportComicList.size() + " comics.",true);
            WriteLogLine("Found " + tmImportSourceFileDuplicateList.size() + " duplicate pages.",true);

            BroadcastProgress(false, "",
                    true, 100,
                    true, sStageName + " completed.");

            //===============================================
            //=== Remove duplicate pages from import folder.
            //===============================================

            //Remove duplicate pages now to reduce processing later.




            File fDeleteMe;
            if (tmImportSourceFileDuplicateList.size() > 0){
                WriteLogLine("Removing duplicate files from import folder.",true);

                //Provide user with progress update:
                iProgressCounter = 0;
                iProgressBarValue = 0;
                iProgressDenominator = tmImportSourceFileDuplicateList.size();
                BroadcastProgress(false, "",
                        true, iProgressBarValue,
                        true, "Removing duplicate pages...");
                sStageName = "Duplicate File Removal";

                for (Map.Entry<String, Integer>
                        entry : tmImportSourceFileDuplicateList.entrySet()) {
                    sAbsolutePath = entry.getKey();
                    fDeleteMe = new File (sAbsolutePath);
                    WriteLogLine("Deleting file " + sAbsolutePath,true);
                    if(!fDeleteMe.delete()) {
                        WriteLogLine("Could not delete duplicate file: " + fDeleteMe.getAbsolutePath(),true);
                    } else {
                        iTotalFilesToCopy--;
                    }

                    //Provide user with progress update:
                    iProgressCounter++;
                    if(iProgressCounter % 5 == 0) {  //Update every 5 deletions
                        iProgressBarValue = Math.round((iProgressCounter / (float) iProgressDenominator) * 100);
                        BroadcastProgress(false, "",
                                true, iProgressBarValue,
                                false, "");
                    }
                }

                BroadcastProgress(false, "",
                        true, 100,
                        true, sStageName + " completed.");

            }



            //===============================================================================
            //=== Delete comics that already exist in catalog; calculate space requirement.
            //===============================================================================

            //I
            //Are there comics to import?
            if (tmImportComicList.size() > 0) {
                //Calculate the amount of space required by the comic move, as well as delete catalog duplicate comics:
                long lSpaceRequiredKB = 0;

                //Provide user with progress update:
                iProgressCounter = 0;
                iProgressBarValue = 0;
                iProgressDenominator = tmImportComicList.size();
                BroadcastProgress(false, "",
                        true, iProgressBarValue,
                        true, "Calculating size / removing catalog duplicates...");
                sStageName = "Space Requirements / Duplicate Comic Removal";

                for (Map.Entry<Integer, String[]>
                        entry : tmImportComicList.entrySet()) {
                    sImportComicListRecord = entry.getValue();
                    if(!Boolean.parseBoolean(sImportComicListRecord[DUPLICATE_IN_CATALOG_INDEX])) { //Don't count the space required if the comic already exists in the catalog.
                        lSpaceRequiredKB += Long.parseLong(sImportComicListRecord[GlobalClass.COMIC_SIZE_KB_INDEX]);
                    } else {
                        //Delete duplicate catalog comics from the import folder:
                        for (Map.Entry<String, Integer>
                                entryImportFile : tmImportSourceFileList.entrySet()) {
                            //Get the path to the file:
                            sAbsolutePath = entryImportFile.getKey();
                            //Get the filename of the file:
                            File f = new File(sAbsolutePath);
                            sFileName = f.getName();

                            iComicID = GetComicID(sFileName);

                            if(iComicID == entry.getKey()){
                                fDeleteMe = new File (sAbsolutePath);
                                WriteLogLine("Deleting comic files from import folder which are already in the catalog: " + sAbsolutePath,true);
                                if(!fDeleteMe.delete()) {
                                    WriteLogLine("Could not delete duplicate file: " + fDeleteMe.getAbsolutePath(),true);
                                } else {
                                    iTotalFilesToCopy--;
                                }
                            }
                        }
                    }
                    //Provide user with progress update:
                    iProgressCounter++;
                    //Update with every comic ID processed:
                    iProgressBarValue = Math.round((iProgressCounter / (float) iProgressDenominator) * 100);
                    BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            false, "");

                }
                WriteLogLine("Size required for file move: " + lSpaceRequiredKB + " KB.",true);
                long lSizeKB;
                lSizeKB = globalClass.AvailableStorageSpace(this, 1);

                BroadcastProgress(false, "",
                        true, 100,
                        true, sStageName + " completed.");

                //===============================================================================
                //=== Create comic folders in catalog, move files, and record data.
                //===============================================================================


                //M
                if (lSizeKB > lSpaceRequiredKB){
                    //Begin loop for comic folder creation and execution of file moves:
                    File fSourceFile;
                    File fDestinationFile;
                    File fCatalogContentsFile = GlobalClass.gvfComicCatalogContentsFile;
                    FileWriter fwCatalogContentsFile = null;

                    //Provide user with progress update:
                    iProgressCounter = 0;
                    iProgressBarValue = 0;
                    iProgressDenominator = iTotalFilesToCopy;
                    BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "Creating folders and moving files...");
                    sStageName = "Folder Creation and File Move";

                    try {
                        fwCatalogContentsFile = new FileWriter(fCatalogContentsFile, true);


                        StringBuilder sbCatalogContentRecord;
                        String sFolderName;
                        int iPageComicID;
                        File fComicDestination;
                        for (Map.Entry<Integer, String[]>
                                entry : tmImportComicList.entrySet()) {
                            sImportComicListRecord = entry.getValue();

                            //Pull data from the record for easy access:
                            iComicID = Integer.parseInt(sImportComicListRecord[GlobalClass.COMIC_ID_INDEX]); //Could also use iComicID = entry.getKey();.
                            iFileCount = Integer.parseInt(sImportComicListRecord[GlobalClass.COMIC_FILE_COUNT_INDEX]); //Used to reduce file count on deletion of cover page.
                            sFolderName = sImportComicListRecord[GlobalClass.COMIC_FOLDER_NAME_INDEX];
                            bDuplicateInCatalog = Boolean.parseBoolean(sImportComicListRecord[DUPLICATE_IN_CATALOG_INDEX]);



                            if(!bDuplicateInCatalog) { //Don't execute if the comic already exists in the catalog.

                                //Create the comic folder:
                                fComicDestination = new File(GlobalClass.gvfComicsFolder,sFolderName);

                                if(!fComicDestination.mkdirs()){
                                    WriteLogLine( "Could not create folder for comic " + iComicID,true);
                                } else {

                                    //Find all files that match the comic ID we are processing, and move them to the new comic folder:
                                    for (Map.Entry<String, Integer>
                                            entryImportFile : tmImportSourceFileList.entrySet()) {
                                        //Get the path to the file:
                                        sAbsolutePath = entryImportFile.getKey();
                                        //Get the filename of the file:
                                        fSourceFile = new File(sAbsolutePath);
                                        sFileName = fSourceFile.getName();

                                        iPageComicID = GetComicID(sFileName);

                                        if (iPageComicID == iComicID) {
                                            WriteLogLine("Importing file into catalog: " + sAbsolutePath,true);
                                            fDestinationFile = new File(GlobalClass.gvfComicsFolder.getAbsolutePath() + File.separator +
                                                    sFolderName + File.separator + sFileName);
                                            WriteLogLine("Moving file " + sFileName + " to " + fDestinationFile.getAbsolutePath(),true);
                                            try {

                                                java.nio.file.Files.move(fSourceFile.toPath(), fDestinationFile.toPath());
                                                if (isCoverFile(sFileName)) {
                                                    //If this is the cover page file, delete it from the destination
                                                    //  directory. We do this after the move because it is more likely
                                                    //  that there will be a security exception with accessing/moving
                                                    //  files from the source directory, and the cover page will be the
                                                    //  first file touched.
                                                    if(!fDestinationFile.delete()) {
                                                        WriteLogLine("Could not delete comic cover file: " + fDestinationFile.getAbsolutePath(),true);
                                                    } else {
                                                        //Update the file count for this comic.
                                                        iFileCount--;
                                                        sImportComicListRecord[GlobalClass.COMIC_FILE_COUNT_INDEX] = Integer.toString(iFileCount);


                                                    }
                                                }

                                                //Provide user with progress update:
                                                iProgressCounter++;
                                                //Update with every comic page processed:
                                                iProgressBarValue = Math.round((iProgressCounter / (float) iProgressDenominator) * 100);
                                                BroadcastProgress(false, "",
                                                        true, iProgressBarValue,
                                                        false, "");


                                            } catch (Exception e) {
                                                WriteLogLine("Problem during file move.\n" + e.getMessage(),true);
                                            }

                                        }
                                    }
                                    //Write a record to the CatalogContentsFile:
                                    sbCatalogContentRecord = new StringBuilder (sImportComicListRecord[0]);
                                    for (int i = 1; i < GlobalClass.ComicRecordFields.length; i++){
                                        sbCatalogContentRecord.append("\t");
                                        sbCatalogContentRecord.append(sImportComicListRecord[i]);
                                    }

                                    sbCatalogContentRecord.append("\n");
                                    fwCatalogContentsFile.append(sbCatalogContentRecord.toString());

                                    //Add the record to the global memory memory treemap:
                                    globalClass.gvtmCatalogComicList.put(
                                            globalClass.gvtmCatalogComicList.size(),
                                            sImportComicListRecord);
                                }

                            }



                        }

                        //If we made it here, the import was successful.

                        //Inform the rest of the program that we have just completed a comic import.
                        // This will affect comic default sort so that the user can see the newly-
                        // imported comic first:
                        globalClass.gbComicJustImported = true;

                    } catch (Exception e){
                        WriteLogLine("Problem during file move.\n" + e.getMessage(),true);
                    } finally {
                        try {
                            if (fwCatalogContentsFile != null) {
                                fwCatalogContentsFile.flush();
                                fwCatalogContentsFile.close();
                            }
                        } catch (IOException e) {
                            WriteLogLine("Problem during CatalogContentsFile flush/close.\n" + e.getMessage(),true);
                        }
                    }

                    BroadcastProgress(false, "",
                            true, 100,
                            true, sStageName + " completed.");

                } else {
                    WriteLogLine( "Insufficient space, " + lSizeKB + " KB, in storage to move " + lSpaceRequiredKB + " KB of comic files.",true);
                }

            } else {
                WriteLogLine( "No comics detected for import.",true);
            }
        } catch (Exception e) {
            WriteLogLine( "Exception during import. Import aborted. " + e.getMessage(),true);
        } finally {
            closeQuietly(cImport);
            WriteLogLine( "Comic import operation complete.",true);
        }
    }

    private static boolean isCoverFile(String sFileName){
        return (sFileName.matches("^\\d{5,6}_Cover.+") && sFileName.length() > 17);
    }
    private static String GetComicNameFromCoverFile(String sFileName){

        int iComicIDDigitCount = 6;
        if (sFileName.matches("^\\d{5}_Cover.+")){
            iComicIDDigitCount = 5;
        }
        return sFileName.substring(7 + iComicIDDigitCount,sFileName.length()-4);
    }
    private static Integer GetComicID(String sFileName){
        boolean bIsValidComicPage = true;
        int iComicIDDigitCount = 0;
        String sComicID;
        int iComicID = -1;
        if (sFileName.matches("^\\d{7}_(Cover|Page).+")){
            iComicIDDigitCount = 7;
        } else if (sFileName.matches("^\\d{6}_(Cover|Page).+")){
            iComicIDDigitCount = 6;
        } else if (sFileName.matches("^\\d{5}_(Cover|Page).+")) {
            iComicIDDigitCount = 5;
        } else if (sFileName.matches("^\\d{4}_(Cover|Page).+")) {
            iComicIDDigitCount = 4;
        } else if (sFileName.matches("^\\d{3}_(Cover|Page).+")) {
            iComicIDDigitCount = 3;
        } else if (sFileName.matches("^\\d{2}_(Cover|Page).+")) {
            iComicIDDigitCount = 2;
        } else if (sFileName.matches("^\\d_(Cover|Page).+")) {
            iComicIDDigitCount = 1;
        } else {
            bIsValidComicPage = false;
        }
        if(bIsValidComicPage) {
            sComicID = sFileName.substring(0, iComicIDDigitCount);
            iComicID = Integer.parseInt(sComicID);
        }
        return iComicID;
    }

    private static boolean isPageDuplicate(String sAbsolutePath){

        File f = new File(sAbsolutePath);
        String sFileName = f.getName();

        boolean bEndsWithNumberInParenthesis;
        bEndsWithNumberInParenthesis = sFileName.matches("^\\d{5,6}_.+\\(\\d{1,2}\\)\\.\\w{3,4}$");

        if (bEndsWithNumberInParenthesis) {
            String sOriginalFileAbsolutePath;

            sOriginalFileAbsolutePath = sAbsolutePath.substring(0,sAbsolutePath.lastIndexOf("(")) + sAbsolutePath.substring(sAbsolutePath.lastIndexOf("."));
            File fFileCheck = new File(sOriginalFileAbsolutePath);

            return fFileCheck.exists();


        }

        return false;

    }


    // Util method to close a closeable
    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception ignore) {
                // ignore exception
            }
        }
    }




}










