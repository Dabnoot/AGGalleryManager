package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Import_ComicAnalyzeHTML extends Worker {

    public static final String TAG_WORKER_IMPORT_COMIC_ANALYZE_HTML = "com.agcurations.aggallermanager.tag_worker_import_comic_analyze_html";

    public static final String WEB_COMIC_ANALYSIS_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.WEB_COMIC_ANALYSIS_ACTION_RESPONSE";

    public static final String EXTRA_STRING_WEB_DATA_LOCATOR_AL_KEY = "com.agcurations.aggallermanager.extra_string_web_data_locator_al_key";

    public static final String EXTRA_STRING_ANALYSIS_ERROR_RECOVERABLE = "com.agcurations.aggallerymanager.intent.action.EXTRA_STRING_ANALYSIS_ERROR_RECOVERABLE";

    String gsDataRecordKey;

    GlobalClass globalClass;

    public Worker_Import_ComicAnalyzeHTML(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsDataRecordKey = getInputData().getString(EXTRA_STRING_WEB_DATA_LOCATOR_AL_KEY);
    }

    @NonNull
    @Override
    public Result doWork() {
        globalClass = (GlobalClass) getApplicationContext();

        String sMessage;

        int giProgressNumerator = 0;
        int iFileSizeMaxLoopCount = 2;
        int giProgressDenominator = 8 + iFileSizeMaxLoopCount;
        int iProgressBarValue;



        //Get the data needed by this worker:
        if(gsDataRecordKey == null){
            globalClass.BroadcastProgress(true, "Data transfer to Comic Analysis worker incomplete: no data key.",
                    false, 0,
                    false, "",
                    WEB_COMIC_ANALYSIS_ACTION_RESPONSE);
            return Result.failure();
        }
        if(!globalClass.WaitForObjectReady(GlobalClass.gabComicWebAnalysDataTMAvailable, 1)){
            globalClass.BroadcastProgress(true, "Data transfer to Comic Analysis worker incomplete: timeout.",
                    false, 0,
                    false, "",
                    WEB_COMIC_ANALYSIS_ACTION_RESPONSE);
            return Result.failure();
        }

        GlobalClass.gabComicWebAnalysDataTMAvailable.set(false);
        if(GlobalClass.gtmComicWebDataLocators.get(gsDataRecordKey) == null) {
            globalClass.BroadcastProgress(true, "Data transfer to Comic Analysis worker incomplete: no data.",
                    false, 0,
                    false, "",
                    WEB_COMIC_ANALYSIS_ACTION_RESPONSE);
            GlobalClass.gabComicWebAnalysDataTMAvailable.set(true);
            return Result.failure();
        }
        ArrayList<ItemClass_WebComicDataLocator> alWebComicDataLocators = new ArrayList<>(Objects.requireNonNull(GlobalClass.gtmComicWebDataLocators.get(gsDataRecordKey)));
        GlobalClass.gtmComicWebDataLocators.remove(gsDataRecordKey);
        GlobalClass.gabComicWebAnalysDataTMAvailable.set(true);


        globalClass.BroadcastProgress(true, "Searching webpage for target data...",
                false, 0,
                false, "",
                WEB_COMIC_ANALYSIS_ACTION_RESPONSE);

        ItemClass_WebComicDataLocator icWebDataLocator = null;

        for (ItemClass_WebComicDataLocator icWCDL: alWebComicDataLocators){
            if(icWCDL.bHostNameMatchFound){
                icWebDataLocator = icWCDL;
                break;
            }
        }
        if(icWebDataLocator == null){
            globalClass.BroadcastProgress(true, "This webpage is incompatible at this time.",
                    false, 0,
                    false, "",
                    WEB_COMIC_ANALYSIS_ACTION_RESPONSE);
            return Result.failure();
        }
        giProgressNumerator++;
        iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
        BroadcastProgress_ComicDetails("", iProgressBarValue);

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
                WEB_COMIC_ANALYSIS_ACTION_RESPONSE);

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
            sMessage = "Problem with HTML parser. Try again?\n" + e.getMessage();
            globalClass.problemNotificationConfig(sMessage, WEB_COMIC_ANALYSIS_ACTION_RESPONSE);
            return Result.failure();
        }
        //For acquiring clean html for use with xPathExpression testing tool at https://www.freeformatter.com/xpath-tester.html:
        String sCleanHTML= "<" + node.getName() + ">" + pageParser.getInnerHtml(node) + "</" + node.getName() + ">";

        giProgressNumerator++;
        iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
        BroadcastProgress_ComicDetails("", iProgressBarValue);

        boolean bProblem = false;

        ArrayList<ItemClass_File> alicf_ComicDownloadFileItems = new ArrayList<>();

        //Broadcast a message to be picked-up by the caller:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(WEB_COMIC_ANALYSIS_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        String sTitle = "";
        String sComicDescription = "";
        String sComicParodies = "";
        String sComicCharacters = "";
        String sRawTextTags = "";
        String sComicArtists = "";
        String sComicGroups = "";
        String sComicLanguages = "";
        String sComicCategories = "";
        String sComicVolume = "";
        String sComicChapter = "";
        String sComicChapterSubtitle = "";
        int iComicPages = -1;
        long lAveragePageSize = 0;
        ArrayList<String[]> alsComicPageAndImageData = new ArrayList<>(); //This ArrayList contains page download address, Save-as filename, and thumbnail address.
        final int URL_INDEX = 0;       //For use with alsComicPageAndImageData String Array elements.
        final int FILENAME_INDEX = 1;  //For use with alsComicPageAndImageData String Array elements.
        final int THUMBNAIL_INDEX = 2; //For use with alsComicPageAndImageData String Array elements.
        int iThumbnailURLImageHeight = -1; //Used specifically for Comic Import Preview.
        int iThumbnailURLImageWidth = -1;  //Used specifically for Comic Import Preview.


        //Create an array structure assisting with data identification.
        //We don't grab the title from one of the html data blocks on nH.net. Data blocks
        // originally configured to align with nH.net.
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
        Arrays.fill(sReturnData, "");

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

        //ProgressNumerator should equal "2" here.

        if(icWebDataLocator.sShortName.equals("nH")) {
            /////==============================================================
            /////==============================================================
            /////=======BELOW CODE FROM IMPORT NH COMIC DETAILS  ==============
            /////==============================================================
            /////==============================================================

            //nH comic import web html search strings (may change if the website changes)
            //If comic source is nH, these strings enable searching the nH web page for tag data:
            String snH_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
            String snH_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";
            String snH_Comic_Cover_Thumb_xPE = "//div[@id='bigcontainer']//img[@class='lazyload']";
            String snH_Comic_Page_Thumbs_xPE = "//div[@class='thumb-container']//img[@class='lazyload']";

            try {
                //===
                //== Get comic title ===
                //===
                BroadcastProgress_ComicDetails("Looking for comic title...", -1);
                String sxPathExpression;
                sxPathExpression = snH_Comic_Title_xPathExpression;
                //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
                Object[] objsTagNodeTitle = node.evaluateXPath(sxPathExpression);
                //Check to see if we found anything:
                if (objsTagNodeTitle != null && objsTagNodeTitle.length > 0) {
                    //If we found something, assign it to a string:
                    sComicTitle = ((TagNode) objsTagNodeTitle[0]).getText().toString();
                }
                sReturnData[COMIC_DETAILS_TITLE_INDEX] = sComicTitle;

                giProgressNumerator++;
                iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                BroadcastProgress_ComicDetails("", iProgressBarValue);

                //===
                //== Get comic metadata ===
                //===

                //Attempt to determine the inclusion of "parodies", "characters", "tags", etc
                //  in the info blocks:
                BroadcastProgress_ComicDetails("Looking for comic data info blocks (parodies, characters, tags, etc)...", -1);
                sxPathExpression = snH_Comic_Data_Blocks_xPE;
                //Use an xPathExpression (similar to RegEx) to look for the data in the html/xml:
                //TCFN = 'tag-container field-name' html class used by n%Hen%tai web pages.
                Object[] objsTagNodesTCFNs = node.evaluateXPath(sxPathExpression);
                String sData = "";
                //Check to see if we found anything:
                if (objsTagNodesTCFNs != null && objsTagNodesTCFNs.length > 0) {
                    //If we found something, assign it to a string:
                    sData = ((TagNode) objsTagNodesTCFNs[0]).getText().toString();
                }

                //Replace spacing with tabs and reduce the tab count.
                sData = sData.replaceAll(" {2}", "\t");
                sData = sData.replaceAll("\t\t", "\t");
                sData = sData.replaceAll("\t\t", "\t");
                sData = sData.replaceAll("\t\t", "\t");
                sData = sData.replaceAll("^\t", ""); //Get rid of any leading tab character.
                sData = sData.replaceAll("\n", ""); //Get rid of any newline characters
                String[] sDataBreakout = sData.split("\t");

                giProgressNumerator++;
                iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                BroadcastProgress_ComicDetails("", iProgressBarValue);

                //Process each named data block. Data blocks are parodies, characters, tags, etc.
                for (int i = 0; i < gsDataBlockIDs.length - 1; i++) {
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
                            //Get rid of "tag count" data. This is data unique to n%Hen%tai that
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
                if (!sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX].equals("")) {
                    iComicPages = Integer.parseInt(sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX]);
                }

                //===
                //== Get comic page images ===
                //===

                //Decypher the rest of the comic page image URLs to be used in a later step of the import:
                BroadcastProgress_ComicDetails("Looking for listing of comic pages...", -1);
                sxPathExpression = snH_Comic_Page_Thumbs_xPE;
                Object[] objsTagNodeThumbnails = node.evaluateXPath(sxPathExpression);
                //Check to see if we found anything:
                String sImageAddressTemplate;
                String sGalleryID = "";
                TreeMap<Integer, String[]> tmFileIndexImageExtension = new TreeMap<>();
                final int EXTENTION_INDEX = 0;
                final int THUMBNAIL_URL_INDEX = 1;
                if (objsTagNodeThumbnails != null && objsTagNodeThumbnails.length > 0) {
                    //Get the gallery ID. This is not the same as the NH comic ID.
                    // Example: "https://t.nh&&entai.net/galleries/645538/1t.png"
                    sImageAddressTemplate = ((TagNode) objsTagNodeThumbnails[0]).getAttributeByName("data-src");
                    if (sImageAddressTemplate.length() > 0) {
                        sGalleryID = sImageAddressTemplate.substring(0, sImageAddressTemplate.lastIndexOf("/"));
                        sGalleryID = sGalleryID.substring(sGalleryID.lastIndexOf("/") + 1);
                    }
                    //Get the thumbnail image names, which will reveal the file extension of the full images (sometimes the image will be jpg, sometimes png, etc):
                    //The thumbnail images from this particular website reveal the file names of the full-size images, which will
                    //  be downloaded from a slightly different address and filename, hence the convoluted processing below.
                    for (Object objsTagNodeThumbnail : objsTagNodeThumbnails) {
                        String sImageAddress = ((TagNode) objsTagNodeThumbnail).getAttributeByName("data-src");
                        BroadcastProgress_ComicDetails(sImageAddress + "\n", -1); //Broadcast progress
                        String sImageFilename = sImageAddress.substring(sImageAddress.lastIndexOf("/") + 1);
                        sImageFilename = sImageFilename.replace("t", ""); //Get rid of the 't', presummably for "thumbnail".
                        String[] sSplit = sImageFilename.split("\\.");
                        if (sSplit.length == 2) {
                            try {
                                Integer iPageNumber = Integer.parseInt(sSplit[0]);
                                String[] sTemp = new String[2];
                                sTemp[EXTENTION_INDEX] = sSplit[1];
                                sTemp[THUMBNAIL_URL_INDEX] = sImageAddress;
                                tmFileIndexImageExtension.put(iPageNumber, sTemp);//Put the thumbnail image address in with the file extension.
                            } catch (Exception ignored) {
                            }
                        }
                    }

                }

                giProgressNumerator++;
                iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                BroadcastProgress_ComicDetails("", iProgressBarValue);

                if(tmFileIndexImageExtension.size() == 0){
                    sMessage = "Problem identifying comic page images on this webpage.";
                    BroadcastProgress_ComicDetails(sMessage, 100);
                    broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                    broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

                    GlobalClass.gabImportComicWebAnalysisRunning.set(false);
                    GlobalClass.gabImportComicWebAnalysisFinished.set(true);
                    return Result.failure();
                }
                //Page addresses should now be acquired.

                //ProgressNumerator should equal "5" here.

                //===
                //== Estimate comic size ===
                //===

                int iFileSizeLoopCount = 0;
                boolean bGetOnlineSize = true;
                long lProjectedComicSize;

                long lSize = 0;
                for (Map.Entry<Integer, String[]> tmEntryPageNumImageExt : tmFileIndexImageExtension.entrySet()) {
                    //Build the suspected URL for the image:
                    String sTemp1 = "h%ttps://i.n%hen%tai.net/galleries/";
                    String sExplicitAddress = sTemp1.replace("%","");
                    String sImageDownloadAddress = sExplicitAddress +
                            sGalleryID + "/" +
                            tmEntryPageNumImageExt.getKey() + "." +
                            tmEntryPageNumImageExt.getValue()[EXTENTION_INDEX];
                    //Build a filename to save the file to in the catalog:
                    String sPageStringForFilename = String.format(Locale.getDefault(), "%04d", tmEntryPageNumImageExt.getKey());
                    String sNewFilename = "Page_" + sPageStringForFilename + "." + tmEntryPageNumImageExt.getValue()[EXTENTION_INDEX];
                    String[] sTemp = {sImageDownloadAddress, sNewFilename, tmEntryPageNumImageExt.getValue()[THUMBNAIL_URL_INDEX]};
                    alsComicPageAndImageData.add(sTemp);

                    //Get the size of the image and add it to the total size of the comic:
                    if (bGetOnlineSize) {
                        URL urlPage = new URL(sImageDownloadAddress);
                        BroadcastProgress_ComicDetails("Getting file size data for " + sImageDownloadAddress, -1); //Broadcast progress
                        //URLConnection connection = urlPage.openConnection();
                        try{
                            HttpURLConnection connection = (HttpURLConnection) urlPage.openConnection();
                            connection.setRequestProperty("Accept-Encoding", "identity");
                            connection.setConnectTimeout(5000);
                            lSize += connection.getContentLength(); //Returns -1 if content size is not in the header.
                            if (lSize == -1) {
                                bGetOnlineSize = false;
                            }

                            iFileSizeLoopCount++;

                            giProgressNumerator++;
                            iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                            BroadcastProgress_ComicDetails("", iProgressBarValue);

                            if (iFileSizeLoopCount == iFileSizeMaxLoopCount) {  //Use a sample set of images to project the size of the comic.
                                //  Larger loop creates a longer delay before the user can move on
                                //  to the next step of an import process.
                                bGetOnlineSize = false;
                            }
                            connection.disconnect();
                        } catch (java.net.SocketTimeoutException e) {
                            sMessage = "Could not get image size due to timeout. " + e.getMessage();
                            BroadcastProgress_ComicDetails(sMessage, -1);
                        }
                    }
                }

                //ProgressNumerator should equal 5 + iFileSizeMaxLoopCount here.

                giProgressNumerator++;
                iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                BroadcastProgress_ComicDetails("", iProgressBarValue);

                if (lSize > 0) {
                    lAveragePageSize = lSize / iFileSizeLoopCount;
                    lProjectedComicSize = lAveragePageSize * iComicPages;

                    String sCleanedProjectedSize = GlobalClass.CleanStorageSize(lProjectedComicSize, GlobalClass.STORAGE_SIZE_NO_PREFERENCE);
                    BroadcastProgress_ComicDetails("Average page size is " + lAveragePageSize + " bytes. Total comic size projected to be " + sCleanedProjectedSize + ".", -1);
                } else {

                }

                if (alsComicPageAndImageData.size() > 0) {
                    //If there are image addresses to attempt to download...
                    try {
                        //Attempt to get the thumbnail height for preview purposes. This helps to limit unused space in a gridview.
                        //  Have to get this data here as we are not on the main thread (network activities not allowed on main thread).
                        InputStream is = (InputStream) new URL(alsComicPageAndImageData.get(0)[THUMBNAIL_INDEX]).getContent();
                        Drawable d = Drawable.createFromStream(is, "src name");
                        if(d != null) {
                            int iIntrinsicHeight = d.getIntrinsicHeight();
                            int iMinimumHeight = d.getMinimumHeight();
                            iThumbnailURLImageHeight = Math.max(iIntrinsicHeight, iMinimumHeight);
                            int iIntrinsicWidth = d.getIntrinsicWidth();
                            int iMinimumWidth = d.getMinimumWidth();
                            iThumbnailURLImageWidth = Math.max(iIntrinsicWidth, iMinimumWidth);
                        }
                    } catch (Exception e) {
                        sMessage = "Could not get image height/width data. " + e.getMessage();
                        BroadcastProgress_ComicDetails(sMessage, -1);
                    }

                }
                giProgressNumerator++;
                iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                BroadcastProgress_ComicDetails("", iProgressBarValue);

            } catch (Exception e) {
                sMessage = e.getMessage();
                BroadcastProgress_ComicDetails("Problem collecting comic data from address. " + sMessage, 100);
                broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                //sendBroadcast(broadcastIntent);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

                GlobalClass.gabImportComicWebAnalysisRunning.set(false);
                GlobalClass.gabImportComicWebAnalysisFinished.set(true);
                return Result.failure();
            }

            //ProgressNumerator should equal 7 + iFileSizeMaxLoopCount here.

        } else if(icWebDataLocator.sShortName.equals("MP")) {
            /////==============================================================
            /////==============================================================
            /////=======BELOW CODE FROM IMPORT MP (M.A.N.G.A.P.A.R.K.) COMIC DETAILS  ==============
            /////==============================================================
            /////==============================================================

            //ProgressNumerator should equal 2 here.

            try {

                if(icWebDataLocator.bSeriesFlag){
                    //The user is considering importing a series listing from this webpage.
                    //Get title:
                    BroadcastProgress_ComicDetails("Looking for comic title...", -1);
                    String sxPathExpression;
                    sxPathExpression = "//a[@class='link link-hover']";
                    //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
                    {
                        Object[] objsTagNodeTitle = node.evaluateXPath(sxPathExpression);
                        //Check to see if we found anything:
                        if (objsTagNodeTitle != null && objsTagNodeTitle.length > 0) {
                            //If we found something, assign it to a string:
                            sTitle = ((TagNode) objsTagNodeTitle[0]).getText().toString();
                        }
                    }
                    sTitle = GlobalClass.cleanHTMLCodedCharacters(sTitle);

                    giProgressNumerator++;
                    iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    BroadcastProgress_ComicDetails("", iProgressBarValue);

                    //Get description:
                    BroadcastProgress_ComicDetails("Looking for comic description...", -1);
                    sxPathExpression = "//body/div/main/div[1]/div[2]/div[4]/div/div[1]/div[1]/react-island/div/div[1]";
                    //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
                    {
                        Object[] objsTagNodeDescription = node.evaluateXPath(sxPathExpression);
                        //Check to see if we found anything:
                        if (objsTagNodeDescription != null && objsTagNodeDescription.length > 0) {
                            //If we found something, assign it to a string:
                            sComicDescription = ((TagNode) objsTagNodeDescription[0]).getText().toString();
                        }
                    }
                    sComicDescription = GlobalClass.cleanHTMLCodedCharacters(sComicDescription);
                    giProgressNumerator++;
                    iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    BroadcastProgress_ComicDetails("", iProgressBarValue);

                    //Get the chapter listing:
                    sxPathExpression = "//a[@class='link-hover link-primary visited:text-accent']";
                    Object[] objsTagNodeChapters = node.evaluateXPath(sxPathExpression);
                    ArrayList<String> alsChapterListing = new ArrayList<>();
                    if (objsTagNodeChapters != null && objsTagNodeChapters.length > 0) {
                        //If we found something, assign it to a string:
                        String sTemp;
                        for (Object obj : objsTagNodeChapters) {
                            sTemp = ((TagNode) obj).getText().toString();
                            sTemp = GlobalClass.cleanHTMLCodedCharacters(sTemp);
                            alsChapterListing.add(sTemp);
                        }
                    }
                    giProgressNumerator++;
                    iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    BroadcastProgress_ComicDetails("", iProgressBarValue);

                    //Get the chapter link listing:
                    String sHostAddress = "";
                    int iCharIndexOfHostStart = icWebDataLocator.sAddress.indexOf("://");
                    if(iCharIndexOfHostStart > 0){
                        iCharIndexOfHostStart += 3;
                        int iCharIndexOfHostEnd = icWebDataLocator.sAddress.indexOf("/", iCharIndexOfHostStart);
                        if(iCharIndexOfHostEnd > 0){
                            sHostAddress = icWebDataLocator.sAddress.substring(0,iCharIndexOfHostEnd);
                        }
                    }
                    sxPathExpression = "//a[@class='link-hover link-primary visited:text-accent']/@href";
                    Object[] objsTagNodeChapterLinks = node.evaluateXPath(sxPathExpression);
                    ArrayList<String> alsChapterLinkListing = new ArrayList<>();
                    if (objsTagNodeChapterLinks != null && objsTagNodeChapterLinks.length > 0) {
                        //If we found something, assign it to a string:
                        String sTemp;
                        for (Object obj : objsTagNodeChapterLinks) {
                            sTemp = (String) obj;
                            sTemp = GlobalClass.cleanHTMLCodedCharacters(sTemp);
                            sTemp = sHostAddress + sTemp;
                            alsChapterLinkListing.add(sTemp);
                        }
                    }
                    giProgressNumerator++;
                    iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    BroadcastProgress_ComicDetails("", iProgressBarValue);

                    //Verify that there are the same number of chapter listings and links:
                    if(alsChapterListing.size() != alsChapterLinkListing.size()){
                        sMessage = "Trouble identifying same count comic chapters and links to chapters.";
                        BroadcastProgress_ComicDetails("\n" + sMessage, 100); //Broadcast progress
                        Data data = new Data.Builder().putString("FAILURE_REASON", sMessage).build();
                        return Result.failure(data);
                    }

                    //Wait for shared data array to become available. It should be available, but
                    //  since this is a worker in another thread, must be careful.
                    if(!globalClass.WaitForObjectReady(GlobalClass.gabComicSeriesArrayAvailable, 2)) {
                        sMessage = "Wait time for ComicSeriesArray to become available exceeded. Worker terminated.";
                        BroadcastProgress_ComicDetails("\n" + sMessage, 100); //Broadcast progress
                        Data data = new Data.Builder().putString("FAILURE_REASON", sMessage).build();
                        return Result.failure(data);
                    }
                    giProgressNumerator++;
                    iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    BroadcastProgress_ComicDetails("", iProgressBarValue);

                    //Set marker to prevent any parallel worker or Activity to not touch the shared
                    // data array:
                    GlobalClass.gabComicSeriesArrayAvailable.set(false);
                    //Build the array of comic series entries:
                    GlobalClass.galicf_ComicSeriesEntries = new ArrayList<>();

                    //ProgressNumerator should equal 7 here.
                    giProgressNumerator = 7 + iFileSizeMaxLoopCount; //To even out with other branches to use same denominator for progress calcs.
                    //ProgressNumerator should equal 8 + iFileSizeMaxLoopCount here.

                    //Set marker to allow other worker or Activity to work with the shared data array:
                    GlobalClass.gabComicSeriesArrayAvailable.set(true);

                } else {

                    //ProgressNumerator should equal 2 here.

                    //===
                    //== Get comic metadata ===
                    //===

                    //Attempt to get the comic title from the WebPage html:
                    BroadcastProgress_ComicDetails("Looking for comic title...", -1);
                    String sxPathExpression;
                    sxPathExpression = "//a[@class='link-pri link-hover']";
                    //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
                    Object[] objsTagNodeTitle = node.evaluateXPath(sxPathExpression);
                    //Check to see if we found anything:
                    if (objsTagNodeTitle != null && objsTagNodeTitle.length > 0) {
                        //If we found something, assign it to a string:
                        sTitle = ((TagNode) objsTagNodeTitle[0]).getText().toString();
                    }
                    sTitle = GlobalClass.cleanHTMLCodedCharacters(sTitle);

                    giProgressNumerator++;
                    iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    BroadcastProgress_ComicDetails("", iProgressBarValue);

                    //Look for comic Chapter:
                    sxPathExpression = "//div[@class='text-base-content comic-detail space-y-2']/h6/a[@class='link-primary link-hover']/span[@class='opacity-80']";
                    Object[] objsTagNodeChapter = node.evaluateXPath(sxPathExpression);
                    //Check to see if we found anything:
                    if (objsTagNodeChapter != null && objsTagNodeChapter.length > 0) {
                        //If we found something, assign it to a string:
                        sComicChapter = ((TagNode) objsTagNodeChapter[0]).getText().toString();
                    }
                    sComicChapter = GlobalClass.cleanHTMLCodedCharacters(sComicChapter);

                    giProgressNumerator++;
                    iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    BroadcastProgress_ComicDetails("", iProgressBarValue);

                    sxPathExpression = "//div[@class='text-base-content comic-detail space-y-2']/h6/a[@class='link-primary link-hover']/span[@class='opacity-50']";
                    Object[] objsTagNodeSubtitle = node.evaluateXPath(sxPathExpression);
                    //Check to see if we found anything:
                    String[] sTrimStrings = {
                            "^:",
                            "^-",
                            "^_",
                            "^ ",
                            " $"
                    };
                    if (objsTagNodeSubtitle != null && objsTagNodeSubtitle.length > 0) {
                        //If we found something, assign it to a string:
                        sComicChapterSubtitle = ((TagNode) objsTagNodeSubtitle[0]).getText().toString();
                        sComicChapterSubtitle = GlobalClass.cleanHTMLCodedCharacters(sComicChapterSubtitle);
                        int iLengthDiff;
                        do {
                            iLengthDiff = sComicChapterSubtitle.length();
                            for (String sTrimString : sTrimStrings) {
                                sComicChapterSubtitle = sComicChapterSubtitle.replaceAll(sTrimString, "");
                            }
                            iLengthDiff = iLengthDiff - sComicChapterSubtitle.length();
                        } while (iLengthDiff > 0);
                    }

                    giProgressNumerator++;
                    iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    BroadcastProgress_ComicDetails("", iProgressBarValue);

                    //For this site, volume, chapter, and chapter subtitle can sometimes appear combined.
                    //Volume, Chapter, and Chapter Subtitle can all appear in the "Chapter" element.
                    //Chapter Subtitle can appear in the subtitle element, but is sometimes ommited.

                    String[] sVolumeAndChapter = ExtractVolumeAndChapter(sComicChapter);
                    sComicVolume = sVolumeAndChapter[0];
                    sComicChapter = sVolumeAndChapter[1];


                    //===
                    //== Get comic page images ===
                    //===

                    //Decypher the comic page image URLs to be used in a later step of the import:
                    BroadcastProgress_ComicDetails("Looking for listing of comic pages...", -1);
                    sxPathExpression = "//img[@class='w-full h-full']/@src";
                    Object[] objsTagNodePageImageAddresses = node.evaluateXPath(sxPathExpression);
                    //Check to see if we found anything:
                    TreeMap<Integer, String> tmFileIndexAndAddress = new TreeMap<>(); //Store page data.
                    if (objsTagNodePageImageAddresses != null && objsTagNodePageImageAddresses.length > 0) {
                        Integer iPageNumber = 0;
                        for (Object objTagNotePageImageAddress : objsTagNodePageImageAddresses) {
                            String sImageAddress = (String) objTagNotePageImageAddress;
                            BroadcastProgress_ComicDetails(sImageAddress + "\n", -1); //Broadcast progress
                            iPageNumber++;
                            tmFileIndexAndAddress.put(iPageNumber, sImageAddress);//Put the thumbnail image address in with the file extension.
                        }
                    }

                    if (tmFileIndexAndAddress.size() == 0) {
                        sMessage = "Problem identifying comic page images on this webpage.";
                        BroadcastProgress_ComicDetails(sMessage, 100);
                        broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                        broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                        broadcastIntent.putExtra(EXTRA_STRING_ANALYSIS_ERROR_RECOVERABLE, true);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

                        GlobalClass.gabImportComicWebAnalysisRunning.set(false);
                        GlobalClass.gabImportComicWebAnalysisFinished.set(true);
                        return Result.failure();
                    }
                    //Page addresses should now be acquired.

                    giProgressNumerator++;
                    iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    BroadcastProgress_ComicDetails("", iProgressBarValue);

                    //ProgressNumerator should equal 6 here.

                    //===
                    //== Estimate comic size ===
                    //===

                    int iFileSizeLoopCount = 0;
                    boolean bGetOnlineSize = true;
                    long lProjectedComicSize;

                    long lSize = 0;
                    for (Map.Entry<Integer, String> tmEntryPageNumImageExt : tmFileIndexAndAddress.entrySet()) {
                        String sImageAddress = tmEntryPageNumImageExt.getValue();
                        //Build a filename to save the file to in the catalog:
                        String sImageFilename = sImageAddress.substring(sImageAddress.lastIndexOf("/") + 1);
                        if (sImageFilename.contains("?")) {
                            sImageFilename = sImageAddress.substring(0, sImageAddress.lastIndexOf("?"));
                        }
                        String[] sFileNameAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sImageFilename);
                        String sExtension = "jpg"; //Assume jpeg.
                        if (sFileNameAndExtension.length == 2) {
                            sExtension = sFileNameAndExtension[1];
                        }
                        String sPageStringForFilename = String.format(Locale.getDefault(), "%04d", tmEntryPageNumImageExt.getKey());
                        String sNewFilename = "Page_" + sPageStringForFilename + "." + sExtension;
                        String[] sTemp = {sImageAddress, sNewFilename, sImageAddress}; //Image address, file name, Thumbnail address. Excuse the redundancy.
                        alsComicPageAndImageData.add(sTemp);

                        //Get the size of the image and add it to the total size of the comic:
                        if (bGetOnlineSize) {
                            URL urlPage = new URL(sImageAddress);
                            BroadcastProgress_ComicDetails("Getting file size data for " + sImageAddress, -1); //Broadcast progress
                            try {
                                //URLConnection connection = urlPage.openConnection();
                                HttpURLConnection connection = (HttpURLConnection) urlPage.openConnection();
                                connection.setRequestProperty("Accept-Encoding", "identity");
                                connection.setConnectTimeout(5000);
                                //connection.setRequestProperty("cookie", icWebDataLocator.sCookie); //todo: remove if not needed. Added on 2/1/2024 for testing with different sites.

                                lSize += connection.getContentLength(); //Returns -1 if content size is not in the header.
                                if (lSize == -1) {
                                    bGetOnlineSize = false;
                                }

                                iFileSizeLoopCount++;

                                giProgressNumerator++;
                                iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                                BroadcastProgress_ComicDetails("", iProgressBarValue);

                                if (iFileSizeLoopCount == iFileSizeMaxLoopCount) {  //Use a sample set of images to project the size of the comic.
                                    //  Larger loop creates a longer delay before the user can move on
                                    //  to the next step of an import process.
                                    bGetOnlineSize = false;
                                }
                                connection.disconnect();
                            } catch (java.net.SocketTimeoutException e) {
                                sMessage = "Could not get image size due to timeout. " + e.getMessage();
                                BroadcastProgress_ComicDetails(sMessage, -1);
                            }
                        }
                    }

                    //ProgressNumerator should equal 6 + iFileSizeMaxLoopCount here.

                    giProgressNumerator++;
                    iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
                    BroadcastProgress_ComicDetails("", iProgressBarValue);
                    //ProgressNumerator should equal 7 + iFileSizeMaxLoopCount here.

                    iComicPages = tmFileIndexAndAddress.size();
                    if (lSize > 0) {
                        lAveragePageSize = lSize / iFileSizeLoopCount;
                        lProjectedComicSize = lAveragePageSize * iComicPages;

                        String sCleanedProjectedSize = GlobalClass.CleanStorageSize(lProjectedComicSize, GlobalClass.STORAGE_SIZE_NO_PREFERENCE);
                        BroadcastProgress_ComicDetails("Average page size is " + lAveragePageSize + " bytes. Total comic size projected to be " + sCleanedProjectedSize + ".", -1);
                    } else {

                    }


                }

            } catch (Exception e) {
                sMessage = e.getMessage();
                BroadcastProgress_ComicDetails("Problem collecting comic data from address. " + sMessage, 100);
                broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                //sendBroadcast(broadcastIntent);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

                GlobalClass.gabImportComicWebAnalysisRunning.set(false);
                GlobalClass.gabImportComicWebAnalysisFinished.set(true);
                return Result.failure();
            }


        }

        /*if(iComicPages > 0) { //todo, merge with !bProblem
            //Broadcast a message to be picked up by the Import fragment to refresh the views:
            broadcastIntent.putExtra(GlobalClass.COMIC_DETAILS_SUCCESS, true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        } else {
            BroadcastProgress_ComicDetails("No comic pages found.", -1);
        }*/

        GlobalClass.gabImportComicWebAnalysisRunning.set(false);
        GlobalClass.gabImportComicWebAnalysisFinished.set(true);

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
            for (int i = 0; i < aliTags.size(); i++){
                if(aliTags.get(i) == -1){
                    //Prepare a list of strings representing the new tags that must be created:

                    if(!sTags[i].equals("")) {
                        //Make sure the tag does not contain any illegal characters that could be written to the tags file and corrupt the file:
                        //Focus on tag name only - a description of the tag is not added via this routine.

                        //Search the fields for illegal characters, and if they are found, replace the illegal
                        //  characters.
                        //Go through all of the "illegal" strings/characters:
                        for (String[] sIllegalStringSet: GlobalClass.gsIllegalRecordStrings) {
                            if(sTags[i].contains(sIllegalStringSet[GlobalClass.CHECKABLE])) {
                                sTags[i] = sTags[i].replace(sIllegalStringSet[GlobalClass.CHECKABLE],"");
                            }
                        }

                        alsNewTags.add(sTags[i]);
                    }
                } else {
                    aliTagsPreExisting.add(aliTags.get(i));
                }
            }
            for (String[] sComicPageURLs: alsComicPageAndImageData){
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
                icf.sComicVolume = sComicVolume;
                icf.sComicChapter = sComicChapter;
                icf.sComicChapterSubtitle = sComicChapterSubtitle;
                icf.iComicPages = iComicPages;
                icf.iThumbnailURLImageHeight = iThumbnailURLImageHeight;
                icf.iThumbnailURLImageWidth = iThumbnailURLImageWidth;

                //Verify that the icf does not contain any illegal characters:
                ItemClass_File icf_CleanedData = GlobalClass.validateFileItemData(icf);
                alicf_ComicDownloadFileItems.add(icf_CleanedData);
            }

            //Also send a broadcast to Activity Import to capture the download items in an array adapter:
            Intent broadcastIntent_ComicWebDetectResponse = new Intent();
            broadcastIntent_ComicWebDetectResponse.putExtra(GlobalClass.EXTRA_BOOL_GET_WEB_COMIC_ANALYSIS_RESPONSE, true);
            broadcastIntent_ComicWebDetectResponse.putExtra(GlobalClass.EXTRA_AL_GET_WEB_COMIC_ANALYSIS_RESPONSE, alicf_ComicDownloadFileItems);
            broadcastIntent_ComicWebDetectResponse.putExtra(GlobalClass.EXTRA_STRING_WEB_ADDRESS, icWebDataLocator.sAddress); //Place this here so that the result is recognized by the receiver. Could be concurrent analysis' ongoing.
            broadcastIntent_ComicWebDetectResponse.setAction(WEB_COMIC_ANALYSIS_ACTION_RESPONSE);
            broadcastIntent_ComicWebDetectResponse.addCategory(Intent.CATEGORY_DEFAULT);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_ComicWebDetectResponse);

        }

        giProgressNumerator++;
        iProgressBarValue = Math.round((giProgressNumerator / (float) giProgressDenominator) * 100);
        BroadcastProgress_ComicDetails("HTML examination complete. Click 'Next' to continue.", iProgressBarValue);
        //ProgressNumerator should equal 8 + iFileSizeMaxLoopCount here.


        return Result.success();
    }

    public static String[] ExtractVolumeAndChapter(String sDataString){
        String[] sVolumeAndChapter = {"", ""};

        String[] sIndicatorsHasVolume = {
                "Volume",
                "Vol",
                "V"}; //Order of indicators is significant.
        StringBuilder sbVolumeNumber = new StringBuilder();
        for (String sVolumeIndicator: sIndicatorsHasVolume){
            if(sDataString.contains(sVolumeIndicator)){
                int iStartingIndex = sDataString.indexOf(sVolumeIndicator);
                boolean bNumberStarted = false;
                boolean bNumberEnded = false;
                int iCharIndex = iStartingIndex;
                while(!bNumberEnded && iCharIndex < sDataString.length()){
                    String sTestChar = sDataString.substring(iCharIndex, iCharIndex + 1);
                    if(bNumberStarted && sTestChar.equals(".")){
                        sbVolumeNumber.append(".");
                    } else {
                        try {
                            int iNumber = Integer.parseInt(sTestChar);
                            sbVolumeNumber.append(iNumber);
                            bNumberStarted = true;
                        } catch (Exception ignored) {
                            if(bNumberStarted){
                                bNumberEnded = true;
                            }
                        }
                    }
                    iCharIndex++;
                }
                break;
            }
        }
        sVolumeAndChapter[0] = sbVolumeNumber.toString();

        String[] sIndicatorsHasChapter = {
                "Chapter",
                "Chapt",
                "Chap",
                "Cha",
                "Ch",
                "C"}; //Order of indicators is significant.
        StringBuilder sbChapterNumber = new StringBuilder();
        for (String sChapterIndicator: sIndicatorsHasChapter){
            if(sDataString.contains(sChapterIndicator)){
                int iStartingIndex = sDataString.indexOf(sChapterIndicator);
                boolean bNumberStarted = false;
                boolean bNumberEnded = false;
                int iCharIndex = iStartingIndex;
                while(!bNumberEnded && iCharIndex < sDataString.length()){
                    String sTestChar = sDataString.substring(iCharIndex, iCharIndex + 1);
                    if(bNumberStarted && sTestChar.equals(".")){
                        sbChapterNumber.append(".");
                    } else {
                        try {
                            int iNumber = Integer.parseInt(sTestChar);
                            sbChapterNumber.append(iNumber);
                            bNumberStarted = true;
                        } catch (Exception ignored) {
                            if(bNumberStarted){
                                bNumberEnded = true;
                            }
                        }
                    }
                    iCharIndex++;
                }
                break;
            }
        }
        sVolumeAndChapter[1] = sbChapterNumber.toString();

        return sVolumeAndChapter;
    }

    private void BroadcastProgress_ComicDetails(String sLogLine, int iPercentComplete){

        boolean bUpdateLog = !sLogLine.equals("");
        boolean bUpdatePercentComplete = iPercentComplete >= 0;

        globalClass.BroadcastProgress(bUpdateLog, sLogLine,
                bUpdatePercentComplete, iPercentComplete,
                false, "",
                WEB_COMIC_ANALYSIS_ACTION_RESPONSE);
    }

}
