package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Import_ComicAnalyzeHTML extends Worker {

    public static final String TAG_WORKER_IMPORT_COMICANALYZEHTML = "com.agcurations.aggallermanager.tag_worker_import_comicanalyzehtml";

    //String gsAddress;
    String gsIntentActionFilter;

    public Worker_Import_ComicAnalyzeHTML(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        //gsAddress = getInputData().getString(GlobalClass.EXTRA_STRING_WEB_ADDRESS);
        gsIntentActionFilter = Fragment_Import_1c_ComicWebDetect.ImportDataServiceResponseReceiver.IMPORT_RESPONSE_COMIC_WEB_DETECT;
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        globalClass.BroadcastProgress(true, "Searching webpage for target data...",
                false, 0,
                false, "",
                gsIntentActionFilter);

        ItemClass_WebComicDataLocator icWebDataLocator = null;
        for(ItemClass_WebComicDataLocator icWCDL: globalClass.galWebComicDataLocators){
            if(icWCDL.bHostNameMatchFound){
                icWebDataLocator = icWCDL;
                break;
            }
        }
        if(icWebDataLocator == null){
            globalClass.BroadcastProgress(true, "This webpage is incompatible at this time.",
                    false, 0,
                    false, "",
                    gsIntentActionFilter);
            return Result.failure();
        }

        //Perform textual search for data as specified by cdsks:
        for (ItemClass_ComicDownloadSearchKey cdsk : icWebDataLocator.alComicDownloadSearchKeys){

            if(cdsk.bMatchFound){
                //If the data was applied as a result of WebView ResourceRequest monitoring, skip
                //  this loop.
                continue;
            }

            int iStart;
            int iEnd;

            if(cdsk.sSearchStartString != null && cdsk.sSearchEndString != null) {
                //We want the data between cdsk.sSearchStartString and cdsk.sSearchEndString.
                iStart = icWebDataLocator.sHTML.indexOf(cdsk.sSearchStartString);
                if (iStart > -1) {
                    iStart += cdsk.sSearchStartString.length();
                    iEnd = icWebDataLocator.sHTML.indexOf(cdsk.sSearchEndString, iStart);
                    if (iEnd > -1) {
                        //We want the data between iStart and iEnd.
                        String sTemp;
                        sTemp = icWebDataLocator.sHTML.substring(iStart, iEnd);
                        if (sTemp.length() > 0) {
                            cdsk.bMatchFound = true;
                            cdsk.sSearchStringMatchContent = sTemp;
                        }

                    }
                }
            }
        } //End loop searching for data (textual search) within the HTML

        globalClass.BroadcastProgress(true, "Textual searches complete. Parsing HTML...",
                false, 0,
                false, "",
                gsIntentActionFilter);

        //Note: DocumentBuilderFactory.newInstance().newDocumentBuilder().parse....
        //  does not work well to parse this html. Modern html interpreters accommodate
        //  certain "liberties" in the code. That parse routine is meant for tight XML.
        //  HtmlCleaner does a good job processing the html in a manner similar to modern
        //  browsers.
        //Clean up the HTML:
        HtmlCleaner pageParser = new HtmlCleaner();
        CleanerProperties props = pageParser.getProperties();
        props.setAllowHtmlInsideAttributes(true);
        props.setAllowMultiWordAttributes(true);
        props.setRecognizeUnicodeChars(true);
        props.setOmitComments(true);
        TagNode node;
        try {
            String sHTML = icWebDataLocator.sHTML;
            sHTML = sHTML.replaceAll("tag-container field-name ", "tag-container field-name"); //Quick find&replace specific to a particular website. //TODO: specify in vdsk.
            node = pageParser.clean(sHTML);
        } catch (Exception e){
            String sMessage = "Problem with HTML parser. Try again?\n" + e.getMessage();
            globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
            return Result.failure();
        }
        //For acquiring clean html for use with xPathExpression testing tool at https://www.freeformatter.com/xpath-tester.html:
        String sCleanHTML= "<" + node.getName() + ">" + pageParser.getInnerHtml(node) + "</" + node.getName() + ">";

        boolean bProblem = false;

        ArrayList<ItemClass_File> alicf_ComicDownloadFileItems = new ArrayList<>();

        /////==============================================================
        /////==============================================================
        /////=======BELOW CODE FROM IMPORT NH COMIC DETAILS  ==============
        /////==============================================================
        /////==============================================================

        //Broadcast a message to be picked-up by the caller:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(gsIntentActionFilter);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        String sTitle;
        String sComicThumbnailURL = "";
        String sComicParodies;
        String sComicCharacters;
        String sRawTextTags;
        String sComicArtists;
        String sComicGroups;
        String sComicLanguages;
        String sComicCategories;
        int iComicPages = -1;
        long lAveragePageSize = 0;
        ArrayList<String[]> alsComicPageAndImageData = new ArrayList<>(); //This ArrayList contains page download address, Save-as filename, and thumbnail address.
        final int URL_INDEX = 0;       //For use with alsComicPageAndImageData String Array elements.
        final int FILENAME_INDEX = 1;  //For use with alsComicPageAndImageData String Array elements.
        final int THUMBNAIL_INDEX = 2; //For use with alsComicPageAndImageData String Array elements.
        int iThumbnailURLImageHeight = -1; //Used specifically for Comic Import Preview.
        int iThumbnailURLImageWidth = -1;  //Used specifically for Comic Import Preview.

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
            /*BroadcastProgress_ComicDetails("Getting data from " + ci.sSource + "\n", gsIntentActionFilter);
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
            TagNode node = pageParser.clean(sHTML);*/


            //Attempt to get the comic title from the WebPage html:
            BroadcastProgress_ComicDetails("Looking for comic title.", gsIntentActionFilter);
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
            BroadcastProgress_ComicDetails("Looking for comic data info blocks (parodies, characters, tags, etc).", gsIntentActionFilter);
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
            sData = sData.replaceAll("\n",""); //Get rid of any newline characters
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

            sTitle = sReturnData[COMIC_DETAILS_TITLE_INDEX];
            sComicParodies = sReturnData[COMIC_DETAILS_PARODIES_DATA_INDEX];
            sComicCharacters = sReturnData[COMIC_DETAILS_CHARACTERS_DATA_INDEX];
            sRawTextTags = sReturnData[COMIC_DETAILS_TAGS_DATA_INDEX]; //NOTE: THESE ARE TEXTUAL TAGS, NOT TAG IDS.
            sComicArtists = sReturnData[COMIC_DETAILS_ARTISTS_DATA_INDEX];
            sComicGroups = sReturnData[COMIC_DETAILS_GROUPS_DATA_INDEX];
            sComicLanguages = sReturnData[COMIC_DETAILS_LANGUAGES_DATA_INDEX];
            sComicCategories = sReturnData[COMIC_DETAILS_CATEGORIES_DATA_INDEX];
            if(!sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX].equals("")) {
                iComicPages = Integer.parseInt(sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX]);
            }

            //Get the first thumbnail image for import preview:
            BroadcastProgress_ComicDetails("Looking for cover page thumbnail.", gsIntentActionFilter);
            sxPathExpression = globalClass.snHentai_Comic_Cover_Thumb_xPE;
            //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
            Object[] objsTagNodeThumbnails = node.evaluateXPath(sxPathExpression);
            //Check to see if we found anything:
            String sThumbnailImageAddress;
            if (objsTagNodeThumbnails != null && objsTagNodeThumbnails.length > 0) {
                sThumbnailImageAddress = ((TagNode) objsTagNodeThumbnails[0]).getAttributeByName("data-src");
                if (sThumbnailImageAddress.length() > 0) {
                    sComicThumbnailURL = sThumbnailImageAddress;
                }
            }

            //Decypher the rest of the comic page image URLs to be used in a later step of the import:
            BroadcastProgress_ComicDetails("Looking for listing of comic pages.", gsIntentActionFilter);
            sxPathExpression = globalClass.snHentai_Comic_Page_Thumbs_xPE;
            objsTagNodeThumbnails = node.evaluateXPath(sxPathExpression);
            //Check to see if we found anything:
            String sImageAddressTemplate;
            String sGalleryID = "";
            TreeMap<Integer, String[]> tmFileIndexImageExtension = new TreeMap<>();
            final int iExtensionIndex = 0;
            final int iThumbnailURLIndex = 1;
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
                            tmFileIndexImageExtension.put(iPageNumber, new String[]{sSplit[1], sImageAddress}); //Put the thumbnail image address in with the file extension.
                        } catch (Exception ignored) {
                        }
                    }
                }

            }

            int iFileSizeLoopCount = 0;
            boolean bGetOnlineSize = true;
            long lProjectedComicSize;

            if (sGalleryID.length() > 0) {
                long lSize = 0;
                for(Map.Entry<Integer, String[]> tmEntryPageNumImageExt: tmFileIndexImageExtension.entrySet()) {
                    //Build the suspected URL for the image:
                    String sNHImageDownloadAddress = "https://i.nhentai.net/galleries/" +
                            sGalleryID + "/" +
                            tmEntryPageNumImageExt.getKey() + "." +
                            tmEntryPageNumImageExt.getValue()[iExtensionIndex];
                    //Build a filename to save the file to in the catalog:
                    String sPageStringForFilename = String.format(Locale.getDefault(),"%04d", tmEntryPageNumImageExt.getKey());
                    String sNewFilename = "Page_" + sPageStringForFilename + "." + tmEntryPageNumImageExt.getValue()[iExtensionIndex];
                    String[] sTemp = {sNHImageDownloadAddress, sNewFilename, tmEntryPageNumImageExt.getValue()[iThumbnailURLIndex]};
                    alsComicPageAndImageData.add(sTemp);

                    //Get the size of the image and add it to the total size of the comic:
                    if(bGetOnlineSize) {
                        URL urlPage = new URL(sNHImageDownloadAddress);
                        BroadcastProgress_ComicDetails("Getting file size data for " + sNHImageDownloadAddress, gsIntentActionFilter); //Broadcast progress
                        //URLConnection connection = urlPage.openConnection();
                        HttpURLConnection connection = (HttpURLConnection) urlPage.openConnection();
                        connection.setRequestProperty("Accept-Encoding", "identity");
                        lSize += connection.getContentLength(); //Returns -1 if content size is not in the header.
                        if(lSize == -1){
                            bGetOnlineSize = false;
                        }
                        iFileSizeLoopCount++;
                        if(iFileSizeLoopCount == 5){  //Use a sample set of images to project the size of the comic.

                            bGetOnlineSize = false;
                        }
                        connection.disconnect();
                    }
                }
                if(lSize > 0) {
                    lAveragePageSize = lSize / iFileSizeLoopCount;
                    lProjectedComicSize = lAveragePageSize * iComicPages;

                    String sCleanedProjectedSize = GlobalClass.CleanStorageSize(lProjectedComicSize, GlobalClass.STORAGE_SIZE_NO_PREFERENCE);
                    BroadcastProgress_ComicDetails("Average page size is " + lAveragePageSize + " bytes. Total comic size projected to be " + sCleanedProjectedSize + ".", gsIntentActionFilter);
                } else {

                }
            }
            if(alsComicPageAndImageData.size() > 0){
                //If there are image addresses to attempt to download...
                try {
                    //Attempt to get the thumbnail height for preview purposes. This helps to limit unused space in a gridview.
                    //  Have to get this data here as we are not on the main thread (network activities not allowed on main thread).
                    InputStream is = (InputStream) new URL(alsComicPageAndImageData.get(0)[THUMBNAIL_INDEX]).getContent();
                    Drawable d = Drawable.createFromStream(is, "src name");
                    int iIntrinsicHeight = d.getIntrinsicHeight();
                    int iMinimumHeight = d.getMinimumHeight();
                    iThumbnailURLImageHeight = Math.max(iIntrinsicHeight, iMinimumHeight);
                    int iIntrinsicWidth = d.getIntrinsicWidth();
                    int iMinimumWidth = d.getMinimumWidth();
                    iThumbnailURLImageWidth = Math.max(iIntrinsicWidth, iMinimumWidth);
                } catch (Exception e) {
                    String sMessage = e.getMessage();
                    Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                }

            }
            BroadcastProgress_ComicDetails("HTML examination complete. Click 'Next' to continue.", gsIntentActionFilter); //todo, merge with !bProblem


        } catch(Exception e){
            String sMsg = e.getMessage();
            BroadcastProgress_ComicDetails("Problem collecting comic data from address. " + sMsg, gsIntentActionFilter);
            broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
            broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMsg);
            //sendBroadcast(broadcastIntent);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

            globalClass.gbImportComicWebAnalysisRunning = false;
            globalClass.gbImportComicWebAnalysisFinished = true;
            return Result.failure();
        }

        if(iComicPages > 0) { //todo, merge with !bProblem
            //Broadcast a message to be picked up by the Import fragment to refresh the views:
            broadcastIntent.putExtra(GlobalClass.COMIC_DETAILS_SUCCESS, true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        } else {
            BroadcastProgress_ComicDetails("No comic pages found.", gsIntentActionFilter);
        }


        globalClass.gbImportComicWebAnalysisRunning = false;
        globalClass.gbImportComicWebAnalysisFinished = true;



        if(!bProblem) {
            //Create an array of ItemClass_File for each detected comic page.
            //  Put the metadata on each page, too, for the future expansion in which the comic detection
            //  picks up non-comic page data and the user "unselects" pages for import - must not
            //  put metadata on the first page only. Considered creating a separate class to hold the metadata,
            //  but the memory load of putting it on every page is low.
            //Process tags first.
            // Convert textual tags to numeric tags:
            // Form the tag integer array:
            // Identify new tags:
            String[] sTags = sRawTextTags.split(", ");
            ArrayList<Integer> aliTags = new ArrayList<>();
            for (String sTag : sTags) {
                int iTagID = globalClass.getTagIDFromText(sTag, GlobalClass.MEDIA_CATEGORY_COMICS);
                aliTags.add(iTagID);
            }
            //Look for any tags that could not be found:
            ArrayList<String> alsNewTags = new ArrayList<>();
            ArrayList<Integer> aliTagsPreExisting = new ArrayList<>();
            for(int i = 0; i < aliTags.size(); i++){
                if(aliTags.get(i) == -1){
                    //Prepare a list of strings representing the new tags that must be created:
                    if(!sTags[i].equals("")) {
                        alsNewTags.add(sTags[i]);
                    }
                } else {
                    aliTagsPreExisting.add(aliTags.get(i));
                }
            }
            for(String[] sComicPageURLs: alsComicPageAndImageData){
                ItemClass_File icf = new ItemClass_File  (ItemClass_File.TYPE_URL, sComicPageURLs[FILENAME_INDEX]);
                icf.sURL = sComicPageURLs[URL_INDEX];
                icf.sTitle = sTitle;
                icf.sURLThumbnail = sComicPageURLs[THUMBNAIL_INDEX];
                icf.bIsChecked = true; //For tag import, tags are attached to an item, and the item must be selected. As of 2022-07-07, we use a catalog item in globalclass to import comics, not these file items.
                icf.lSizeBytes = lAveragePageSize;

                icf.alsUnidentifiedTags = alsNewTags;
                icf.aliProspectiveTags = aliTagsPreExisting;
                icf.aliRecognizedTags = new ArrayList<>();
                icf.aliRecognizedTags.addAll(aliTagsPreExisting);

                icf.sComicParodies = sComicParodies;
                icf.sComicCharacters = sComicCharacters;
                icf.sComicArtists = sComicArtists;
                icf.sComicGroups = sComicGroups;
                icf.sComicLanguages = sComicLanguages;
                icf.sComicCategories = sComicCategories;
                icf.iComicPages = iComicPages;
                icf.iThumbnailURLImageHeight = iThumbnailURLImageHeight;
                icf.iThumbnailURLImageWidth = iThumbnailURLImageWidth;

                alicf_ComicDownloadFileItems.add(icf);
            }

            //Also send a broadcast to Activity Import to capture the download items in an array adapter:
            Intent broadcastIntent_ComicWebDetectResponse = new Intent();
            broadcastIntent_ComicWebDetectResponse.putExtra(GlobalClass.EXTRA_BOOL_GET_WEB_COMIC_ANALYSIS_RESPONSE, true);
            broadcastIntent_ComicWebDetectResponse.putExtra(GlobalClass.EXTRA_AL_GET_WEB_COMIC_ANALYSIS_RESPONSE, alicf_ComicDownloadFileItems);
            broadcastIntent_ComicWebDetectResponse.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
            broadcastIntent_ComicWebDetectResponse.addCategory(Intent.CATEGORY_DEFAULT);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_ComicWebDetectResponse);
        }




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
