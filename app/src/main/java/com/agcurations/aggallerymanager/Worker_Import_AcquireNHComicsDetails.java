package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Import_AcquireNHComicsDetails extends Worker {

    public static final String TAG_WORKER_IMPORT_ACQUIRENHCOMICDETAILS = "com.agcurations.aggallermanager.tag_worker_import_acquirenhcomicdetails";

    String gsAddress;
    String gsIntentActionFilter;

    public Worker_Import_AcquireNHComicsDetails(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsAddress = getInputData().getString(GlobalClass.EXTRA_STRING_WEB_ADDRESS);
        gsIntentActionFilter = getInputData().getString(GlobalClass.EXTRA_STRING_INTENT_ACTION_FILTER);
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        //Broadcast a message to be picked-up by the caller:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(gsIntentActionFilter);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        ItemClass_CatalogItem ci = new ItemClass_CatalogItem();
        ci.iMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        ci.sSource = gsAddress;

        //We don't grab the title from one of the html data blocks on nHentai.net.
        final String[] gsDataBlockIDs = new String[]{
                "Parodies:",
                "Characters:",
                "Tags:",
                "Artists:",
                "Groups:",
                "Languages:",
                "Categories:",
                "Pages:",
                "Uploaded:"}; //We ignore the upload date data, but still include it.

        int j = gsDataBlockIDs.length + 1;
        String[] sReturnData = new String[j];
        //First array element is for comic title.
        //Elements 1-8 are data block results.
        //Last array element is for error message.
        for(int i = 0; i < j; i++){
            sReturnData[i] = "";
        }

        String sComicTitle = "";

        final int COMIC_DETAILS_TITLE_INDEX = 0;
        final int COMIC_DETAILS_PARODIES_DATA_INDEX = 1;
        final int COMIC_DETAILS_CHARACTERS_DATA_INDEX = 2;
        final int COMIC_DETAILS_TAGS_DATA_INDEX = 3;
        final int COMIC_DETAILS_ARTISTS_DATA_INDEX = 4;
        final int COMIC_DETAILS_GROUPS_DATA_INDEX = 5;
        final int COMIC_DETAILS_LANGUAGES_DATA_INDEX = 6;
        final int COMIC_DETAILS_CATEGORIES_DATA_INDEX = 7;
        final int COMIC_DETAILS_PAGES_DATA_INDEX = 8;

        try {
            //Get the data from the WebPage:
            BroadcastProgress_ComicDetails("Getting data from " + ci.sSource + "\n", gsIntentActionFilter);
            URL url = new URL(ci.sSource);
            URLConnection conn = url.openConnection();
            conn.setDoInput(true);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder a = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                a.append(inputLine);
            }
            in.close();
            BroadcastProgress_ComicDetails("\nData acquired. Begin data processing...\n", gsIntentActionFilter);

            String sHTML = a.toString();
            sHTML = sHTML.replaceAll("tag-container field-name ", "tag-container field-name");

            //Note: DocumentBuilderFactory.newInstance().newDocumentBuilder().parse....
            //  does not work well to parse this html. Modern html interpreters accommodate
            //  certain "liberties" in the code. That parse routine is meant for tight XML.
            //  HtmlCleaner does a good job processing the html in a manner similar to modern
            //  browsers.
            //Clean up the HTML:
            BroadcastProgress_ComicDetails("Cleaning up html.\n", gsIntentActionFilter);
            HtmlCleaner pageParser = new HtmlCleaner();
            CleanerProperties props = pageParser.getProperties();
            props.setAllowHtmlInsideAttributes(true);
            props.setAllowMultiWordAttributes(true);
            props.setRecognizeUnicodeChars(true);
            props.setOmitComments(true);
            TagNode node = pageParser.clean(sHTML);


            //Attempt to get the comic title from the WebPage html:
            BroadcastProgress_ComicDetails("Looking for comic title.\n", gsIntentActionFilter);
            String sxPathExpression;
            sxPathExpression = globalClass.snHentai_Comic_Title_xPathExpression;
            //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
            Object[] objsTagNodeTitle = node.evaluateXPath(sxPathExpression);
            //Check to see if we found anything:
            if (objsTagNodeTitle != null && objsTagNodeTitle.length > 0) {
                //If we found something, assign it to a string:
                sComicTitle = ((TagNode) objsTagNodeTitle[0]).getText().toString();
            }

            sReturnData[COMIC_DETAILS_TITLE_INDEX] = sComicTitle;

            //Attempt to determine the inclusion of "parodies", "characters", "tags", etc
            //  in the info blocks:
            BroadcastProgress_ComicDetails("Looking for comic data info blocks (parodies, characters, tags, etc).\n", gsIntentActionFilter);
            sxPathExpression = globalClass.snHentai_Comic_Data_Blocks_xPE;
            //Use an xPathExpression (similar to RegEx) to look for the data in the html/xml:
            //TCFN = 'tag-container field-name' html class used by nHentai web pages.
            Object[] objsTagNodesTCFNs = node.evaluateXPath(sxPathExpression);
            String sData = "";
            //Check to see if we found anything:
            if (objsTagNodesTCFNs != null && objsTagNodesTCFNs.length > 0) {
                //If we found something, assign it to a string:
                sData = ((TagNode) objsTagNodesTCFNs[0]).getText().toString();
            }

            //Replace spacing with tabs and reduce the tab count.
            sData = sData.replaceAll(" {2}","\t");
            sData = sData.replaceAll("\t\t","\t");
            sData = sData.replaceAll("\t\t","\t");
            sData = sData.replaceAll("\t\t","\t");
            sData = sData.replaceAll("^\t",""); //Get rid of any leading tab character.

            String[] sDataBreakout = sData.split("\t");


            //Process each named data block. Data blocks are parodies, characters, tags, etc.
            for(int i = 0; i < gsDataBlockIDs.length - 1; i++) {
                //gsDataBlockIDs.length - 1 ====> We are ignoring the last data block, "Uploaded:", the upload date.
                int iterator = -1; //Determine where in the sequence of objects the current data block will appear.
                for (int k = 0; k < sDataBreakout.length - 1; k++) {
                    //Find the DataBreakout index (k) that contains the DataBlock identifier (not the data):
                    if (sDataBreakout[k].contains(gsDataBlockIDs[i])) {

                        if (sDataBreakout[k + 1].contains(gsDataBlockIDs[i + 1])) {
                            //If we are here, then it means that there was no data between the current
                            //  data block and the next data block. Skip gathering the data for this
                            //  data block.
                            continue;
                        } else {
                            iterator = k + 1;
                        }

                        break;
                    }
                }
                if (iterator > 0) {
                    sData = sDataBreakout[iterator];
                    if (!sDataBreakout[iterator - 1].contains("Pages:")) { //Don't clean-out numbers if we are expecting numbers.
                        //Get rid of "tag count" data. This is data unique to nHentai that
                        //  shows the number of times that the tag has been applied.
                        sData = sData.replaceAll("\\d{4}K", "\t");
                        sData = sData.replaceAll("\\d{3}K", "\t");
                        sData = sData.replaceAll("\\d{2}K", "\t");
                        sData = sData.replaceAll("\\dK", "\t");
                        sData = sData.replaceAll("\\d{4}", "\t");
                        sData = sData.replaceAll("\\d{3}", "\t");
                        sData = sData.replaceAll("\\d{2}", "\t");
                        sData = sData.replaceAll("\\d", "\t");
                    }
                    //Reformat the data:
                    String[] sItems = sData.split("\t");
                    StringBuilder sbData = new StringBuilder();
                    sbData.append(sItems[0]);
                    for (int m = 1; m < sItems.length; m++) {
                        sbData.append(", ");
                        sbData.append(sItems[m]);
                    }
                    sReturnData[i + 1] = sbData.toString();
                }
            }

            ci.sTitle = sReturnData[COMIC_DETAILS_TITLE_INDEX];
            ci.sComicParodies = sReturnData[COMIC_DETAILS_PARODIES_DATA_INDEX];
            ci.sComicCharacters = sReturnData[COMIC_DETAILS_CHARACTERS_DATA_INDEX];
            ci.sTags = sReturnData[COMIC_DETAILS_TAGS_DATA_INDEX]; //NOTE: THESE ARE TEXTUAL TAGS, NOT TAG IDS.
            ci.sComicArtists = sReturnData[COMIC_DETAILS_ARTISTS_DATA_INDEX];
            ci.sComicGroups = sReturnData[COMIC_DETAILS_GROUPS_DATA_INDEX];
            ci.sComicLanguages = sReturnData[COMIC_DETAILS_LANGUAGES_DATA_INDEX];
            ci.sComicCategories = sReturnData[COMIC_DETAILS_CATEGORIES_DATA_INDEX];
            if(!sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX].equals("")) {
                ci.iComicPages = Integer.parseInt(sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX]);
            }

            //Get the first thumbnail image for import preview:
            BroadcastProgress_ComicDetails("Looking for cover page thumbnail.\n", gsIntentActionFilter);
            sxPathExpression = globalClass.snHentai_Comic_Cover_Thumb_xPE;
            //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
            Object[] objsTagNodeThumbnails = node.evaluateXPath(sxPathExpression);
            //Check to see if we found anything:
            String sThumbnailImageAddress;
            if (objsTagNodeThumbnails != null && objsTagNodeThumbnails.length > 0) {
                sThumbnailImageAddress = ((TagNode) objsTagNodeThumbnails[0]).getAttributeByName("data-src");
                if (sThumbnailImageAddress.length() > 0) {
                    //ci.sComicThumbnailURL = sThumbnailImageAddress;
                }
            }

            //Decypher the rest of the comic page image URLs to be used in a later step of the import:
            BroadcastProgress_ComicDetails("Looking for listing of comic pages.\n", gsIntentActionFilter);
            sxPathExpression = globalClass.snHentai_Comic_Page_Thumbs_xPE;
            objsTagNodeThumbnails = node.evaluateXPath(sxPathExpression);
            //Check to see if we found anything:
            String sImageAddressTemplate;
            String sGalleryID = "";
            TreeMap<Integer, String> tmFileIndexImageExtension = new TreeMap<>();
            if (objsTagNodeThumbnails != null && objsTagNodeThumbnails.length > 0) {
                //Get the gallery ID. This is not the same as the NH comic ID.
                // Example: "https://t.nhentai.net/galleries/645538/1t.png"
                sImageAddressTemplate = ((TagNode) objsTagNodeThumbnails[0]).getAttributeByName("data-src");
                if (sImageAddressTemplate.length() > 0) {
                    sGalleryID = sImageAddressTemplate.substring(0, sImageAddressTemplate.lastIndexOf("/"));
                    sGalleryID = sGalleryID.substring(sGalleryID.lastIndexOf("/") + 1);
                }
                //Get the thumbnail image names, which will reveal the file extension of the full images:
                for (Object objsTagNodeThumbnail : objsTagNodeThumbnails) {
                    String sImageAddress = ((TagNode) objsTagNodeThumbnail).getAttributeByName("data-src");
                    BroadcastProgress_ComicDetails(sImageAddress + "\n", gsIntentActionFilter); //Broadcast progress
                    String sImageFilename = sImageAddress.substring(sImageAddress.lastIndexOf("/") + 1);
                    sImageFilename = sImageFilename.replace("t", ""); //Get rid of the 't', presummably for "thumbnail".
                    String[] sSplit = sImageFilename.split("\\.");
                    if (sSplit.length == 2) {
                        try {
                            Integer iPageNumber = Integer.parseInt(sSplit[0]);
                            tmFileIndexImageExtension.put(iPageNumber, sSplit[1]);
                        } catch (Exception ignored) {
                        }
                    }
                }

            }
            ArrayList<String[]> alsImageNameData = new ArrayList<>();
            int iFileSizeLoopCount = 0;
            boolean bGetOnlineSize = true;
            long lProjectedComicSize;

            if (sGalleryID.length() > 0) {
                for(Map.Entry<Integer, String> tmEntryPageNumImageExt: tmFileIndexImageExtension.entrySet()) {
                    //Build the suspected URL for the image:
                    String sNHImageDownloadAddress = "https://i.nhentai.net/galleries/" +
                            sGalleryID + "/" +
                            tmEntryPageNumImageExt.getKey() + "." +
                            tmEntryPageNumImageExt.getValue();
                    //Build a filename to save the file to in the catalog:
                    String sPageStringForFilename = String.format(Locale.getDefault(),"%04d", tmEntryPageNumImageExt.getKey());
                    String sNewFilename = "Page_" + sPageStringForFilename + "." + tmEntryPageNumImageExt.getValue();
                    String[] sTemp = {sNHImageDownloadAddress, sNewFilename};
                    alsImageNameData.add(sTemp);

                    //Get the size of the image and add it to the total size of the comic:
                    if(bGetOnlineSize) {
                        URL urlPage = new URL(sNHImageDownloadAddress);
                        BroadcastProgress_ComicDetails("Getting file size data for " + sNHImageDownloadAddress + "\n", gsIntentActionFilter); //Broadcast progress
                        //URLConnection connection = urlPage.openConnection();
                        HttpURLConnection connection = (HttpURLConnection) urlPage.openConnection();
                        connection.setRequestProperty("Accept-Encoding", "identity");
                        ci.lSize += connection.getContentLength(); //Returns -1 if content size is not in the header.
                        if(ci.lSize == -1){
                            bGetOnlineSize = false;
                        }
                        iFileSizeLoopCount++;
                        if(iFileSizeLoopCount == 5){  //Use a sample set of images to project the size of the comic.
                            lProjectedComicSize = ci.lSize / iFileSizeLoopCount;
                            lProjectedComicSize *= ci.iComicPages;
                            ci.lSize = lProjectedComicSize;
                            BroadcastProgress_ComicDetails("Projecting size of comic to " + ci.iComicPages + " pages... " + ci.lSize + " bytes." + "\n", gsIntentActionFilter);
                            bGetOnlineSize = false;
                        }
                        connection.disconnect();
                    }
                }
            }
            if(alsImageNameData.size() > 0){
                //If there are image addresses to attempt to download...
                //ci.alsDownloadURLsAndDestFileNames = alsImageNameData;
            }
            BroadcastProgress_ComicDetails("Finished analyzing web data.\n", gsIntentActionFilter);



        } catch(Exception e){
            String sMsg = e.getMessage();
            BroadcastProgress_ComicDetails("Problem collecting comic data from address. " + sMsg + "\n", gsIntentActionFilter);
            broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
            broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMsg);
            //sendBroadcast(broadcastIntent);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

            globalClass.gbImportComicWebAnalysisRunning = false;
            globalClass.gbImportComicWebAnalysisFinished = true;
            return Result.failure();
        }


        ci.bComic_Online_Data_Acquired = true;

        if(ci.iComicPages > 0) {
            //Put potential catalog item in globalclass to handle reset of activity if user leaves
            //  activity or rotates the screen:
            //globalClass.gci_ImportComicWebItem = ci;

            //Broadcast a message to be picked up by the Import fragment to refresh the views:
            broadcastIntent.putExtra(GlobalClass.COMIC_DETAILS_SUCCESS, true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        } else {
            BroadcastProgress_ComicDetails("No comic pages found.\n", gsIntentActionFilter);
        }





        globalClass.gbImportComicWebAnalysisRunning = false;
        globalClass.gbImportComicWebAnalysisFinished = true;
        return Result.success();
    }

    public void BroadcastProgress_ComicDetails(String sLogLine, String sIntentActionFilter){
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        globalClass.BroadcastProgress(true, sLogLine,
                false, 0,
                false, "",
                sIntentActionFilter);
        //Fragment_Import_5a_WebComicConfirmation.ImportDataServiceResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
    }

}
