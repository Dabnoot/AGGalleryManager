package com.agcurations.aggallerymanager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Import_GetDirectoryContents extends Worker {

    public static final String TAG_WORKER_IMPORT_GETDIRECTORYCONTENTS = "com.agcurations.aggallermanager.tag_worker_import_getdirectorycontents";

    Uri guriImportTreeUri;
    int giMediaCategory;
    int giFilesOrFolders;
    int giComicImportSource;

    public Worker_Import_GetDirectoryContents(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        String sImportTreeUri = getInputData().getString(GlobalClass.EXTRA_IMPORT_TREE_URI);
        guriImportTreeUri = Uri.parse(sImportTreeUri);
        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
        giFilesOrFolders = getInputData().getInt(GlobalClass.EXTRA_FILES_OR_FOLDERS, -1);
        giComicImportSource = getInputData().getInt(GlobalClass.EXTRA_COMIC_IMPORT_SOURCE, -1);
    }

    @NonNull
    @Override
    public Result doWork() {

        if(Activity_Import.guriImportTreeURI != null){
            GlobalClass globalClass = (GlobalClass) getApplicationContext();

            long lProgressNumerator = 0L;
            long lProgressDenominator = 0L;
            int iProgressBarValue = 0;

            Intent broadcastIntent_GetDirectoryContentsResponse = new Intent();
            ArrayList<ItemClass_File> alFileList = new ArrayList<>();
            try {

                //Get data about the files from the UriTree:
                ContentResolver contentResolver = getApplicationContext().getContentResolver();
                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(guriImportTreeUri, DocumentsContract.getTreeDocumentId(guriImportTreeUri));
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

                if(cImport != null) {





                    //Calculate total number of files for a progress bar:
                    lProgressDenominator = cImport.getCount();

                    if((giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) &&
                            (giComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER)) {
                        //If we are importing comics by the folder, calculate a new progressbar denominator.
                        //Go through the folders in the users' chosen directory and count the files.
                        //  Each will be processed, and add a tick to the progressbar.

                        long lProgressDenominatorQuick = lProgressDenominator;

                        cImport.moveToPosition(-1);
                        while (cImport.moveToNext() && !globalClass.gbImportFolderAnalysisStop) {
                            String sSubFolderDocID = cImport.getString(0);
                            String sSubFolderMimeType = cImport.getString(2);

                            boolean bSubFolderItemIsDirectory;
                            bSubFolderItemIsDirectory = (sSubFolderMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR));
                            if (bSubFolderItemIsDirectory) { //Only process directories/folders.
                                Uri uriSubfiles = DocumentsContract.buildChildDocumentsUriUsingTree(guriImportTreeUri, sSubFolderDocID);
                                List<Uri> luriSubfiles = new LinkedList<>();
                                luriSubfiles.add(uriSubfiles);
                                uriSubfiles = luriSubfiles.remove(0); // get the item from top
                                sSortOrder = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC"; //Sort does not appear to work.
                                Cursor cSubfiles = contentResolver.query(uriSubfiles,
                                        new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                                DocumentsContract.Document.COLUMN_MIME_TYPE},
                                        null,
                                        null,
                                        sSortOrder);

                                if (cSubfiles != null) { //If the query came back with results...
                                    lProgressDenominator += cSubfiles.getCount(); //Count the files in the folder.
                                    cSubfiles.close(); //Close the query.
                                }

                            }

                            lProgressNumerator++;
                            iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominatorQuick) * 1000);
                            globalClass.BroadcastProgress(false, "",
                                    true, iProgressBarValue,
                                    true, lProgressNumerator + "/" + lProgressDenominatorQuick,
                                    Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

                        }
                    }

                    lProgressNumerator = 0L;

                    globalClass.BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "0/" + lProgressDenominator,
                            Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);


                    MediaMetadataRetriever mediaMetadataRetriever;
                    mediaMetadataRetriever = new MediaMetadataRetriever();

                    cImport.moveToPosition(-1);
                    while (cImport.moveToNext() && !globalClass.gbImportFolderAnalysisStop) {

                        //Update progress bar:
                        //Update progress right away in order to avoid instances in which a loop is skipped.
                        lProgressNumerator++;
                        iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 1000);
                        globalClass.BroadcastProgress(false, "",
                                true, iProgressBarValue,
                                true, lProgressNumerator + "/" + lProgressDenominator,
                                Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);


                        final String docId = cImport.getString(0);
                        final String docName = cImport.getString(1);
                        final String mimeType = cImport.getString(2);
                        final long lLastModified = cImport.getLong(3); //milliseconds since January 1, 1970 00:00:00.0 UTC.
                        final String sFileSize = cImport.getString(4);

                        boolean isDirectory;
                        isDirectory = (mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR));
                        int iTypeFileOrFolder = (isDirectory) ? ItemClass_File.TYPE_FOLDER : ItemClass_File.TYPE_FILE;

                        if ((giFilesOrFolders == GlobalClass.FILES_ONLY) && (isDirectory)) {
                            continue; //skip the rest of the for loop for this item.
                        } else if ((giFilesOrFolders == GlobalClass.FOLDERS_ONLY) && (!isDirectory)) {
                            continue; //skip the rest of the for loop for this item.
                        }


                        long lFileSize;
                        String fileExtension;
                        //If the file is video, get the duration so that the file list can be sorted by duration if requested.
                        long lDurationInMilliseconds = -1L;
                        String sWidth = "";  //We are not doing math with the width and height. Therefore no need to convert to int.
                        String sHeight = "";

                        //Get a Uri for this individual document:
                        final Uri docUri = DocumentsContract.buildDocumentUriUsingTree(childrenUri, docId);

                        /*GlobalClass globalClass = (GlobalClass) getApplicationContext();
                        File fStorageDir = Environment.getExternalStorageDirectory();

                        problemNotificationConfig(
                                docUri.toString(), RECEIVER_STORAGE_LOCATION);

                        File fTestDir = null;
                        if(docUri.toString().startsWith("content://com.android.externalstorage.documents/tree/0000-0000")) {
                            fTestDir = new File("/storage/0000-0000/FireMimi/1558833241126.webm");
                            if (fTestDir.exists()) {
                                Log.d("Test", "Dir exists.");
                            } else {
                                Log.d("Test", "Dir doest not exist.");
                            }
                            return;
                        }
                        if(fTestDir == null){
                            return;
                        }*/



                        //Get date last modified:
                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.setTimeInMillis(lLastModified);
                        Date dateLastModified = cal.getTime();

                        //Data for comic from xml if available:
                        String sTitle = "";
                        String sURL = "";
                        String sPageCount = "";
                        String sArtist = "";
                        String sParody = "";
                        ArrayList<Integer> aliRecognizedTags = new ArrayList<>();
                        ArrayList<String> alsUnidentifiedTags = new ArrayList<>();

                        if (!isDirectory) {

                            //Record the file extension:
                            fileExtension = docName.contains(".") ? docName.substring(docName.lastIndexOf(".")) : "";
                            //If the file extension does not match the file extension regex, skip the remainder of the loop.
                            if (!fileExtension.matches(".+")) {
                                continue;  //skip the rest of the loop if the file extension does not match.
                            }

                            //Determine the file size:
                            lFileSize = Long.parseLong(sFileSize); //size in Bytes

                            //If this is a file, check to see if it is a video or an image and if we are looking for videos or images.
                            if (mimeType.startsWith("video") || fileExtension.equals(".gif") ||
                                    (mimeType.equals("application/octet-stream") && fileExtension.equals(".mp4"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid
                                //If video...
                                if ((giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES)
                                        || (giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS)) {
                                    continue; //If requesting images or comics, and mimeType is video or the file a gif, go to next loop.
                                }
                            } else {
                                //If not video...
                                if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                                    continue; //If requesting videos, and mimeType is not video nor is the file a gif, go to next loop.
                                }
                            }

                            if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS && GlobalClass.bVideoDeepDirectoryContentFileAnalysis) {
                                if (mimeType.startsWith("video") ||
                                        (mimeType.equals("application/octet-stream") && fileExtension.equals(".mp4"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid
                                    try {
                                        mediaMetadataRetriever.setDataSource(getApplicationContext(), docUri);
                                    } catch (Exception e) {
                                        //problemNotificationConfig(e.getMessage() + "\n" + docName, RECEIVER_STORAGE_LOCATION);
                                        continue; //Skip the rest of this loop.
                                    }
                                    sWidth = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                                    sHeight = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                                    String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                                    if(time != null) {
                                        lDurationInMilliseconds = Long.parseLong(time);
                                    }
                                } else { //if it's not a video file, check to see if it's a gif:
                                    if (fileExtension.equals(".gif")) {
                                        //Get the duration of the gif image:
                                        Context activityContext = getApplicationContext();
                                        try {
                                            pl.droidsonroids.gif.GifDrawable gd = new pl.droidsonroids.gif.GifDrawable(activityContext.getContentResolver(), docUri);
                                            lDurationInMilliseconds = gd.getDuration();
                                            sWidth = "" + gd.getIntrinsicWidth();
                                            sHeight = "" + gd.getIntrinsicHeight();
                                        } catch (Exception e) {
                                            globalClass.problemNotificationConfig(e.getMessage() + "\n" + docName, Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
                                            continue; //Skip the rest of this loop.
                                        }
                                    }
                                }
                            } else if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                                //Get the width and height of the image:
                                try {
                                    InputStream input = getApplicationContext().getContentResolver().openInputStream(docUri);
                                    if (input != null) {
                                        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                                        onlyBoundsOptions.inJustDecodeBounds = true;
                                        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
                                        input.close();
                                        sWidth = "" + onlyBoundsOptions.outWidth;
                                        sHeight = "" + onlyBoundsOptions.outHeight;
                                    }

                                } catch (Exception e) {
                                    continue; //Skip the rest of this loop.
                                }
                            }



                            //Convert the file Uri to string. Uri's don't transport well as intent extras.
                            String sUri = docUri.toString();

                            //create the file model and initialize:
                            ItemClass_File icfFileItem = new ItemClass_File(iTypeFileOrFolder, docName);
                            icfFileItem.sExtension = fileExtension;
                            icfFileItem.lSizeBytes = lFileSize;
                            icfFileItem.dateLastModified = dateLastModified;
                            icfFileItem.sWidth = sWidth;
                            icfFileItem.sHeight = sHeight;
                            icfFileItem.sUri = sUri;
                            icfFileItem.sMimeType = mimeType;
                            icfFileItem.lVideoTimeInMilliseconds = lDurationInMilliseconds;

                            //Add the ItemClass_File to the ArrayList:
                            alFileList.add(icfFileItem);




                        } else {
                            //File item IS a directory...
                            if((giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) &&
                                    (giComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER)){
                                //If the user is getting directory contents for importing comics by the folder...








                                //Get a list of files in the folder:
                                //Uri uriComicPages = DocumentsContract.buildChildDocumentsUriUsingTree(docUri, DocumentsContract.getTreeDocumentId(docUri));
                                Uri uriComicPages = DocumentsContract.buildChildDocumentsUriUsingTree(guriImportTreeUri, docId);
                                List<Uri> luriComicPages = new LinkedList<>();
                                luriComicPages.add(uriComicPages);
                                uriComicPages = luriComicPages.remove(0); // get the item from top
                                sSortOrder = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC"; //Sort does not appear to work.
                                Cursor cComicPages = contentResolver.query(uriComicPages,
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

                                if(cComicPages != null) {

                                    //Anaylyze potential comic pages in this subfolder:

                                    ArrayList<ItemClass_File> alicf_ComicFiles = new ArrayList<>();

                                    while (cComicPages.moveToNext()) {

                                        //Write progress here since some of the lower steps will cause the loop to skip processing:
                                        lProgressNumerator++;
                                        iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 1000);
                                        globalClass.BroadcastProgress(false, "",
                                                true, iProgressBarValue,
                                                true, lProgressNumerator + "/" + lProgressDenominator,
                                                Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

                                        //Analyze the file item.

                                        //If this file is a folder, skip to the next item:
                                        final String sComicPageFilename = cComicPages.getString(1);
                                        final String sComicPageMimeType = cComicPages.getString(2);
                                        boolean bComicPageIsDirectory;
                                        bComicPageIsDirectory = (sComicPageMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR));
                                        int iComicPageItemFileType = (bComicPageIsDirectory) ? ItemClass_File.TYPE_FOLDER : ItemClass_File.TYPE_FILE;
                                        if((iComicPageItemFileType != ItemClass_File.TYPE_FILE) ||
                                                (!sComicPageMimeType.contains("image") && !sComicPageFilename.equals(GlobalClass.STRING_COMIC_XML_FILENAME))){
                                            continue;
                                        }


                                        //Build a ItemClass_File item for this file:

                                        ItemClass_File icf_ComicPage = new ItemClass_File(iComicPageItemFileType, sComicPageFilename);

                                        //Get a Uri for this individual document:
                                        final String sComicPageDocId = cComicPages.getString(0);
                                        final Uri uriComicPageUri = DocumentsContract.buildDocumentUriUsingTree(childrenUri, sComicPageDocId);
                                        icf_ComicPage.sUri = uriComicPageUri.toString();

                                        //Set the mime type:
                                        icf_ComicPage.sMimeType = sComicPageMimeType;

                                        //Get date last modified:
                                        long lComicPageLastModified = cComicPages.getLong(3); //milliseconds since January 1, 1970 00:00:00.0 UTC.

                                        cal.setTimeInMillis(lComicPageLastModified);
                                        icf_ComicPage.dateLastModified = cal.getTime();

                                        //Record the file extension:
                                        fileExtension = sComicPageFilename.contains(".") ? sComicPageFilename.substring(sComicPageFilename.lastIndexOf(".")) : "";
                                        icf_ComicPage.sExtension = fileExtension;

                                        //Record the file size:
                                        final String sComicPageFileSize = cComicPages.getString(4);
                                        lFileSize = Long.parseLong(sComicPageFileSize); //size in Bytes
                                        icf_ComicPage.lSizeBytes = lFileSize;

                                        //Get the image dimensions:
                                        if(icf_ComicPage.sMimeType.contains("image")) {
                                            try {
                                                InputStream input = getApplicationContext().getContentResolver().openInputStream(uriComicPageUri);
                                                if (input != null) {
                                                    BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                                                    onlyBoundsOptions.inJustDecodeBounds = true;
                                                    BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
                                                    input.close();
                                                    sWidth = "" + onlyBoundsOptions.outWidth;
                                                    sHeight = "" + onlyBoundsOptions.outHeight;
                                                }

                                            } catch (Exception e) {
                                                continue; //Skip the rest of this loop.
                                            }
                                            icf_ComicPage.sWidth = sWidth;
                                            icf_ComicPage.sHeight = sHeight;
                                        } else {
                                            //Check to see if this is an xml file.
                                            //If this is an xml file, it likely contains comic details.

                                            if(sComicPageFilename.equals(GlobalClass.STRING_COMIC_XML_FILENAME)) {
                                                //Get Document Builder
                                                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                                                DocumentBuilder builder = factory.newDocumentBuilder();
                                                //Build Document
                                                InputStream inputStream = contentResolver.openInputStream(uriComicPageUri);
                                                Document docComicXML = builder.parse(inputStream);
                                                inputStream.close();

                                                sTitle = getXMLNodeText(docComicXML, "ComicTitle");
                                                sURL = getXMLNodeText(docComicXML, "URL");
                                                sPageCount = getXMLNodeText(docComicXML, "PageCount");
                                                sArtist = getXMLNodeText(docComicXML, "Artist");
                                                sParody = getXMLNodeText(docComicXML, "Parody");

                                                NodeList nlTemp;
                                                ArrayList<String> alsTags = new ArrayList<>();
                                                nlTemp = docComicXML.getElementsByTagName("Tag");
                                                for(int i = 0; i < nlTemp.getLength(); i++) {
                                                    alsTags.add(nlTemp.item(i).getTextContent());
                                                }
                                                //Pre-process tags. Identify tags that already exist, and create a list of new tags for
                                                //  the user to approve - don't automatically add new tags to the system (I've encountered
                                                //  garbage tags, tags that already exist in another form, and tags that the user might
                                                //  not want to add.
                                                for(String sTag: alsTags){
                                                    String sIncomingTagCleaned = sTag.toLowerCase().trim();
                                                    boolean bTagFound = false;
                                                    for(Map.Entry<String, ItemClass_Tag> TagEntry: globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()){
                                                        String sExistingTagCleaned = TagEntry.getKey().toLowerCase().trim();
                                                        if(sExistingTagCleaned.equals(sIncomingTagCleaned)){
                                                            bTagFound = true;
                                                            aliRecognizedTags.add(TagEntry.getValue().iTagID);
                                                            break;
                                                        }
                                                    }
                                                    if(!bTagFound){
                                                        alsUnidentifiedTags.add(sTag.trim());
                                                    }
                                                }

                                            } //End if this is an xml file containing comic data.

                                        } //End if this is/not an image file.

                                        //Set the parent folder Uri for various uses later:
                                        icf_ComicPage.sUriParent = docUri.toString();

                                        //Now determine opportunities to decypher comic page number:
                                        ArrayList<Integer[]> aliNumberBlocksSingleFile = new ArrayList<>();
                                        boolean bDigitBlockOn = false;
                                        Integer[] iDigitBlock = {0,0};
                                        for(int i = 0; i < sComicPageFilename.length(); i++){
                                            if(Character.isDigit(sComicPageFilename.charAt(i))){
                                                if(!bDigitBlockOn){
                                                    iDigitBlock[0] = i;
                                                }
                                                bDigitBlockOn = true;
                                            } else {
                                                if(bDigitBlockOn){
                                                    iDigitBlock[1] = i;
                                                    aliNumberBlocksSingleFile.add(iDigitBlock);
                                                    iDigitBlock = new Integer[]{0,0};
                                                }
                                                bDigitBlockOn = false;
                                            }
                                        }
                                        //Blocks of digits now acquired. Assign to the file item:
                                        icf_ComicPage.aliNumberBlocks = aliNumberBlocksSingleFile;


                                        //Add this file item to the list for report back to caller:
                                        alicf_ComicFiles.add(icf_ComicPage);


                                    }
                                    cComicPages.close();

                                    //Create a file item to track this comic folder:
                                    ItemClass_File icf_ComicFolderItem = new ItemClass_File(ItemClass_File.TYPE_FOLDER, docName);
                                    icf_ComicFolderItem.sUri = docUri.toString();

                                    icf_ComicFolderItem.dateLastModified = dateLastModified;

                                    //Transfer data acquired from an XML file (no worries if no XML file contained the data):
                                    icf_ComicFolderItem.sTitle = sTitle;
                                    icf_ComicFolderItem.sURL = sURL;
                                    icf_ComicFolderItem.sPageCount = sPageCount;
                                    icf_ComicFolderItem.sComicArtists = sArtist;
                                    icf_ComicFolderItem.sComicParodies = sParody;
                                    icf_ComicFolderItem.aliRecognizedTags = aliRecognizedTags;
                                    icf_ComicFolderItem.aliProspectiveTags = new ArrayList<>(aliRecognizedTags); //Don't copy the pointer.
                                    icf_ComicFolderItem.alsUnidentifiedTags = alsUnidentifiedTags;

                                    //All of the comic pages in this directory have been added to an
                                    //  arraylist of file items. Now look for a page numbering pattern:

                                    //Verify that all pages have the same quantity of number blocks (telling where the page number might be in the filename):
                                    boolean bQtyNumBlocksOk = false;
                                    int iNumBlocks = -1;
                                    for(ItemClass_File file: alicf_ComicFiles){
                                        if(file.sFileOrFolderName.equals(GlobalClass.STRING_COMIC_XML_FILENAME)){
                                            continue; //Don't try to find a page number on this file.
                                        }
                                        if(iNumBlocks == -1){  //If this is the first set of number blocks we are testing, record the count.
                                            iNumBlocks = file.aliNumberBlocks.size();
                                            bQtyNumBlocksOk = true;
                                        } else {
                                            if(iNumBlocks != file.aliNumberBlocks.size()){  //Compare the count of number blocks.
                                                bQtyNumBlocksOk = false;
                                                //Report a problem with this file:
                                                String sMessage = "Problem identifying page number for comic in folder \"" +
                                                        docName + "\", file \"" + file.sFileOrFolderName +
                                                        "\". Note that the system uses alphabetization to sort comic pages.\n";
                                                globalClass.gsbImportFolderAnalysisLog.append(sMessage);
                                                globalClass.problemNotificationConfig(sMessage, Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
                                            }
                                        }
                                    }
                                    if(!bQtyNumBlocksOk){
                                        //If we need to stop analyzing the comic page order due to a numbering problem...
                                        icf_ComicFolderItem.iNumberBlockStatus = ItemClass_File.COMIC_PAGE_ID_BLOCK_COUNT_INCONSISTENT;
                                        //Assign the thumbnail file to the first file in the list.
                                        if(alicf_ComicFiles.size() > 0) {
                                            icf_ComicFolderItem.sUriThumbnailFile = alicf_ComicFiles.get(0).sUri;
                                        }
                                    } else {
                                        //Attempt to identify the number block representing the page ID for the set of image files in the folder:
                                        TreeMap<Integer, String> tmComicFilesPageNumCheck;
                                        for(int iNumberBlock = 0; iNumberBlock < iNumBlocks; iNumberBlock++) {
                                            tmComicFilesPageNumCheck = new TreeMap<>(); //Reset the file ordering.

                                            boolean bPossibleNumberBlockCandidate = true;
                                            for (ItemClass_File icfComicPage : alicf_ComicFiles) {
                                                if(icfComicPage.sFileOrFolderName.equals(GlobalClass.STRING_COMIC_XML_FILENAME)){
                                                    continue; //Don't try to find a page number on this file.
                                                }
                                                //Attempt to get the integer value from this filename using the current number block:
                                                String sPageID  = icfComicPage.sFileOrFolderName.substring(icfComicPage.aliNumberBlocks.get(iNumberBlock)[0], icfComicPage.aliNumberBlocks.get(iNumberBlock)[1]);
                                                if(sPageID.length() > 4){
                                                    sPageID = sPageID.substring(sPageID.length() - 4);  //Filename might be merely a datetime stamp.
                                                }
                                                if(sPageID.length() <= 4) {
                                                    //This app is not designed to work with pages greater than 9999.
                                                    //  The comic page viewer will not properly sort the 10,000th page
                                                    //  alphabetically. Just assume there are no comics out there with more than 9999 pages.

                                                    Integer iPageID = Integer.parseInt(sPageID);
                                                    //Check to see if this page number already exists in the TreeMap:
                                                    if (tmComicFilesPageNumCheck.containsKey(iPageID)) {
                                                        //If the page number already exists in the TreeMap, this cannot be the \
                                                        //  page number. Exit the loop and attempt to test the next number block;
                                                        bPossibleNumberBlockCandidate = false;
                                                        break;
                                                    }
                                                    tmComicFilesPageNumCheck.put(iPageID, icfComicPage.sUri);
                                                }
                                            }
                                            if(bPossibleNumberBlockCandidate){
                                                //If all of the found possible page IDs for this comic are unique,
                                                //  now test the pages for order:
                                                int iOrder = 1;
                                                //tmComicFilesPageNumCheck should have the pages sorted by the key, the page number based
                                                //  on the current number block in the filename.
                                                for(Map.Entry<Integer, String> tmPageEntry: tmComicFilesPageNumCheck.entrySet()){
                                                    int iThisPageID = tmPageEntry.getKey();
                                                    if(iThisPageID > (iOrder + GlobalClass.iComicFolderImportMaxPageSkip)){
                                                        //If the current pageID is is greater than the max skip page value (default,
                                                        // or possibly set by user in a later version of this program, go examine the next ID block.
                                                        icf_ComicFolderItem.iNumberBlockStatus = ItemClass_File.COMIC_PAGE_ID_BLOCK_COUNT_SKIPS_PAGES;
                                                        icf_ComicFolderItem.sUriThumbnailFile = ""; //Reset the possible thumbnail file.
                                                        break;
                                                    } else {
                                                        iOrder = iThisPageID;
                                                    }
                                                    if(icf_ComicFolderItem.sUriThumbnailFile.equals("")){
                                                        //Assign the thumbnail file to the first file in the list.
                                                        icf_ComicFolderItem.sUriThumbnailFile = tmPageEntry.getValue();
                                                    }

                                                }

                                                if(icf_ComicFolderItem.iNumberBlockStatus == ItemClass_File.COMIC_PAGE_ID_BLOCK_COUNT_OK){
                                                    //This is likely a number block ok for measuring the comic pages.
                                                    //Set the number block index so that this number block is used by the import process.
                                                    icf_ComicFolderItem.iNumberBlockPageIDIndex = iNumberBlock;
                                                    break; //Quit the loop looking at the number blocks.
                                                }


                                            }

                                        } //End loop looking for number block identifying the page ID in the set of comic pages.

                                        if(icf_ComicFolderItem.sUriThumbnailFile.equals("")){
                                            //If we still have not decided what the thumbnail will be,
                                            //  Set it to the first file when sorting alphabetically:
                                            TreeMap<String, String> tmAlphabetizedComicFiles = new TreeMap<>();
                                            for(ItemClass_File file: alicf_ComicFiles){
                                                tmAlphabetizedComicFiles.put(file.sFileOrFolderName,file.sUri);
                                            }
                                            Map.Entry<String, String> meFirst = tmAlphabetizedComicFiles.firstEntry();
                                            icf_ComicFolderItem.sUriThumbnailFile = meFirst.getValue();

                                        }


                                    } //End if all comic pages in this comic have the same quantity of number blocks in the file name.

                                    //Code to deliver the comic files and the comic folder FileItem:
                                    if(alicf_ComicFiles.size() > 0) {
                                        alFileList.add(icf_ComicFolderItem);
                                        alFileList.addAll(alicf_ComicFiles);
                                    }

                                } //End if comic folder has comic files.

                            } //End if user is importing comics and has selected to import from folder(s).

                        } //End if "isDirectory".


                    } //End loop going through the folder that the user selected.

                    cImport.close();
                    mediaMetadataRetriever.release();

                } //End if "there are items in the folder that the user selected.

            }catch (Exception e){
                //todo: This does not appear to make it to the import activity:
                String sMessage = "Problem during handleAction_GetDirectoryContents: " + e.getMessage();
                globalClass.gsbImportFolderAnalysisLog.append(sMessage);
                globalClass.problemNotificationConfig(e.getMessage(), Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
                return Result.failure();
            }

            broadcastIntent_GetDirectoryContentsResponse.putExtra(GlobalClass.EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, true);
            broadcastIntent_GetDirectoryContentsResponse.putExtra(GlobalClass.EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE, alFileList);

            //Send broadcast to the Import Activity:
            broadcastIntent_GetDirectoryContentsResponse.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
            broadcastIntent_GetDirectoryContentsResponse.addCategory(Intent.CATEGORY_DEFAULT);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);

            //Set finished and broadcast so that the fragment knows that we are done.
            globalClass.gbImportFolderAnalysisFinished = true;
            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, lProgressNumerator + "/" + lProgressDenominator,
                    Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);

            globalClass.gbImportFolderAnalysisRunning = false;
            if(globalClass.gbImportFolderAnalysisStop) {
                globalClass.gbImportFolderAnalysisStop = false;
            }

        }




        return Result.success();
    }

    private String getXMLNodeText(Document docXML, String sNodeTagName){
        //This routine used to simplify coding in handleAction_GetDirectoryContents for Comic XML read.
        NodeList nlTemp;
        String sReturnData = "";
        nlTemp = docXML.getElementsByTagName(sNodeTagName);
        if (nlTemp.getLength() > 0){
            sReturnData = nlTemp.item(0).getTextContent();
        }
        return sReturnData;
    }

}
