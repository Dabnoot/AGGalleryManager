package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_LINK;
import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8;
import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TAGS;
import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_THUMBNAIL;
import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TITLE;

public class Worker_Import_VideoAnalyzeHTML extends Worker {

    public static final String TAG_WORKER_IMPORT_VIDEOANALYZEHTML = "com.agcurations.aggallermanager.tag_worker_import_videoanalyzehtml";

    public Worker_Import_VideoAnalyzeHTML(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        String sIntentActionFilter = Fragment_Import_1a_VideoWebDetect.ImportDataServiceResponseReceiver.IMPORT_RESPONSE_VIDEO_WEB_DETECT;

        globalClass.BroadcastProgress(true, "Searching webpage for target data...",
                false, 0,
                false, "",
                sIntentActionFilter);

        ItemClass_WebVideoDataLocator icWebDataLocator = null;
        for(ItemClass_WebVideoDataLocator icWVDL: globalClass.galWebVideoDataLocators){
            if(icWVDL.bHostNameMatchFound){
                icWebDataLocator = icWVDL;
                break;
            }
        }
        if(icWebDataLocator == null){
            globalClass.BroadcastProgress(true, "This webpage is incompatible at this time.",
                    false, 0,
                    false, "",
                    sIntentActionFilter);
            return Result.failure();
        }



        //Perform textual search for data as specified by vdsks:
        for (ItemClass_VideoDownloadSearchKey vdsk : icWebDataLocator.alVideoDownloadSearchKeys){

            if(vdsk.bMatchFound){
                //If the data was applied as a result of WebView ResourceRequest monitoring, skip
                //  this loop.
                continue;
            }

            int iStart;
            int iEnd;

            if(vdsk.sSearchStartString != null && vdsk.sSearchEndString != null) {
                //We want the data between vdsk.sSearchStartString and vdsk.sSearchEndString.
                iStart = icWebDataLocator.sHTML.indexOf(vdsk.sSearchStartString);
                if (iStart > -1) {
                    iStart += vdsk.sSearchStartString.length();
                    iEnd = icWebDataLocator.sHTML.indexOf(vdsk.sSearchEndString, iStart);
                    if (iEnd > -1) {
                        //We want the data between iStart and iEnd.
                        String sTemp;
                        sTemp = icWebDataLocator.sHTML.substring(iStart, iEnd);
                        if (sTemp.length() > 0) {
                            vdsk.bMatchFound = true;
                            vdsk.sSearchStringMatchContent = sTemp;
                        }

                    }
                }
            }
        } //End loop searching for data (textual search) within the HTML

        //Custom textual searches for hard-coded mp4 files:
        String sNonExplicitAddress = "^h%ttps:\\/\\/w%ww\\.p%ornh%ub\\.c%om\\/v%iew_v%ideo.p%hp(.*)"; //Don't allow b-o-t-s to easily find hard-coded addresses in this source.
        String sNHRegexExpression = sNonExplicitAddress.replace("%","");
        if(icWebDataLocator.sHostnameRegEx.equals(sNHRegexExpression)){
            //Special behavior for this website.
            //Finding the mp4 contiguous (non-M3U8) video download data for this particular site requires significant
            //parsing.
            try {
                String sS = "qualityItems";
                String sE = ";";
                int iS = icWebDataLocator.sHTML.indexOf(sS);
                int iE = icWebDataLocator.sHTML.indexOf(sE, iS);
                String Sub1 = icWebDataLocator.sHTML.substring(iS, iE);
                sS = "[{";
                sE = "}]";
                iS = Sub1.indexOf(sS);
                iE = Sub1.indexOf(sE, iS);
                String Sub2 = Sub1.substring(iS, iE);
                String Sub3 = Sub2.replace("},{", ";");
                Sub3 = Sub3.replace("[{", "");
                String[] sVDOptions = Sub3.split(";");

                int iRecordNumber = icWebDataLocator.alVideoDownloadSearchKeys.size();
                for (String sVDORecords : sVDOptions) {
                    String[] sTemp = sVDORecords.split(",");
                    if (sTemp.length >= 5) {
                        //0: "id":"quality240p"
                        //1: "text":"240p"
                        //2: "url":"https:\/\/ev.phncdn.com\/videos\/201812\/31\/199453331\/240P_400K_199453331.mp4?validfrom=1624843943&validto=1624851143&rate=500k&burst=1400k&ipa=71.179.233.44&hash=MpIVLM9Yr5qK32YnDVbWG81%2Fhaw%3D"
                        //3: "upgrade":0
                        //4: "active":0
                        int iHTTPLocation = sTemp[2].indexOf("http");
                        if (iHTTPLocation >= 0) {
                            String sURL = sTemp[2].substring(sTemp[2].indexOf("http"));
                            sURL = sURL.replace("\"", "");
                            sURL = sURL.replace("\\", "");
                            icWebDataLocator.alVideoDownloadSearchKeys.add(
                                    new ItemClass_VideoDownloadSearchKey(
                                            ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_LINK,
                                            "", ""));
                            icWebDataLocator.alVideoDownloadSearchKeys.get(iRecordNumber).sSearchStringMatchContent = sURL;
                            icWebDataLocator.alVideoDownloadSearchKeys.get(iRecordNumber).bMatchFound = true; //Fake it.

                            iRecordNumber++;
                        }

                    }
                }
            } catch (Exception e){
                //Catch crashes when substring location markers includes -1 because a string match was not found.
            }

        }
        //End custom textual searches for hard-coded mp4 files.


        globalClass.BroadcastProgress(true, "Textual searches complete. Parsing HTML...",
                false, 0,
                false, "",
                sIntentActionFilter);



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
        TagNode node = null;
        try {
            node = pageParser.clean(icWebDataLocator.sHTML);
        } catch (Exception e){
            String sMessage = "Problem with HTML parser. Try again?\n" + e.getMessage();
            globalClass.problemNotificationConfig(sMessage, sIntentActionFilter);
            return Result.failure();
        }
        //For acquiring clean html for use with xPathExpression testing tool at https://www.freeformatter.com/xpath-tester.html:
        String sCleanHTML= "<" + node.getName() + ">" + pageParser.getInnerHtml(node) + "</" + node.getName() + ">";

        pageParser = null;

        boolean bProblem = false;





        String sTitle = "";
        String sURLThumbnail = "";
        ArrayList<String> alsUnidentifiedTags = new ArrayList<>();
        ArrayList<Integer> aliIdentifiedTags = new ArrayList<>();


        //Assemble a list of FileItems (ItemClass_File) listing the potential downloads.
        //  Include filename, download address, tags, and, if available, file size, resolution, and duration:
        ArrayList<ItemClass_File> alicf_VideoDownloadFileItems = new ArrayList<>();


        //Analyze data based on the Video Download Search Keys (VDSKs). Each VDSK can be either a
        // textual search key or an XPath Expression, similar to RegEx, but for XML/HTML.

        for (ItemClass_VideoDownloadSearchKey vdsk : icWebDataLocator.alVideoDownloadSearchKeys){

            switch (vdsk.sDataType) {

                case VIDEO_DOWNLOAD_TITLE:

                    //Check to see if the title was found via a textual search:
                    if (vdsk.bMatchFound) {
                        sTitle = GlobalClass.cleanHTMLCodedCharacters(vdsk.sSearchStringMatchContent); //Convert any html coded characters
                        if(!sTitle.equals("")){
                            break;
                        }
                    }

                    //If not found via textual search, attempt to get the video title from the webpage via XPath:
                    String sXPathExpressionTitleLocator = null;
                    if(vdsk.sSXPathExpression != null){
                        sXPathExpressionTitleLocator = vdsk.sSXPathExpression;
                    } else {
                        break;
                    }

                    try {
                        //Use an xPathExpression (similar to RegEx) to look for the video title in the html/xml:
                        Object[] objsTitle = node.evaluateXPath(sXPathExpressionTitleLocator);
                        //Check to see if we found anything:
                        if (objsTitle != null && objsTitle.length > 0) {
                            //If we found something, assign it to a string:
                            for (Object oTitle : objsTitle) {
                                sTitle = oTitle.toString();
                                if(!sTitle.equals("")){
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        bProblem = true;
                        globalClass.problemNotificationConfig(e.getMessage(), sIntentActionFilter);
                    }

                    //End search for title.
                    break;


                case VIDEO_DOWNLOAD_THUMBNAIL:
                    //Attempt to get the thumbnail address from the Webpage html:
                    globalClass.BroadcastProgress(true, "Looking for thumbnail address...",
                            false, 0,
                            false, "",
                            sIntentActionFilter);

                    //Check to see if the thumbnail URL was found via a textual search:
                    if (vdsk.bMatchFound) {
                        sURLThumbnail = GlobalClass.cleanHTMLCodedCharacters(vdsk.sSearchStringMatchContent); //Convert any html coded characters
                        if(!sURLThumbnail.equals("")){
                            break;
                        }
                    }

                    //If the thumbnail was not found via textual search, attempt XPath expression:
                    String sXPathExpressionThumbnailLocator = null;
                    if(vdsk.sSXPathExpression != null) {
                        sXPathExpressionThumbnailLocator = vdsk.sSXPathExpression;
                    } else {
                        break;
                    }
                    try {
                        //Use an xPathExpression (similar to RegEx) to look for tag data in the html/xml:
                        Object[] objsThumbnail = node.evaluateXPath(sXPathExpressionThumbnailLocator);
                        //Check to see if we found anything:
                        if (objsThumbnail != null && objsThumbnail.length > 0) {
                            //If we found something, assign it to a string:
                            for (Object oTags : objsThumbnail) {
                                sURLThumbnail = oTags.toString();
                                if (!sURLThumbnail.equals("")) {
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        bProblem = true;
                        globalClass.problemNotificationConfig(e.getMessage(), sIntentActionFilter);
                    }
                    //End thumbnail URL standard search.

                    //Special behavior for x.hamm.stter thumbnail URL:
                    sNonExplicitAddress = "^h%ttps:\\/\\/x%ham%ster\\.c%om\\/videos(.*)"; //Don't allow b-o-t-s to easily find hard-coded addresses in this source.
                    sNHRegexExpression = sNonExplicitAddress.replace("%","");
                    if(icWebDataLocator.sHostnameRegEx.equals(sNHRegexExpression)) {
                        //Special behavior for this website.
                        //The XPath expression finds a string like:
                        //style="background:image: url(https://thumb:lvlt.xhcdn.com/a/Gb79mZGgKJ7vdtiUf6MF8Q/010/869/987/1280x720.c.jpg.v1547829300);"
                        try {
                            String sS = "url(";
                            String sE = ");";
                            int iE = sURLThumbnail.indexOf(sE);
                            int iS = sURLThumbnail.indexOf(sS) + sS.length();
                            sURLThumbnail = sURLThumbnail.substring(iS, iE);
                        } catch (Exception e) {
                            //Catch crashes when substring location markers includes -1 because a string match was not found.
                        }
                    } //end special behavior for hamm, regarding thumbnail image.
                    break;


                case VIDEO_DOWNLOAD_TAGS:
                    //Attempt to get the tags from the WebPage html:
                    globalClass.BroadcastProgress(true, "Looking for video category tags...",
                            false, 0,
                            false, "",
                            sIntentActionFilter);

                    //Several tags, so likely only use XPath Expression to find them. Break from pattern used
                    //  to search for title-string and thumbnail-URL sections above.
                    String sXPathExpressionTagsLocator = null;
                    if(vdsk.sSXPathExpression != null){
                        sXPathExpressionTagsLocator = vdsk.sSXPathExpression;
                    }
                    ArrayList<String> alsTags = new ArrayList<>();
                    if(sXPathExpressionTagsLocator != null) {
                        try {
                            //Use an xPathExpression (similar to RegEx) to look for tag data in the html/xml:
                            Object[] objsTags = node.evaluateXPath(sXPathExpressionTagsLocator);
                            //Check to see if we found anything:
                            if (objsTags != null && objsTags.length > 0) {
                                //If we found something, assign it to a string:
                                for (Object oTags : objsTags) {
                                    alsTags.add(oTags.toString());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            bProblem = true;
                            globalClass.problemNotificationConfig(e.getMessage(), sIntentActionFilter);
                        }
                    }
                    //Pre-process tags. Identify tags that already exist, and create a list of new tags for
                    //  the user to approve - don't automatically add new tags to the system (I've encountered
                    //  garbage tags, tags that already exist in another form, and tags that the user might
                    //  not want to add.

                    for(String sTag: alsTags){
                        String sIncomingTagCleaned = sTag.toLowerCase().trim();
                        boolean bTagFound = false;
                        for(Map.Entry<Integer, ItemClass_Tag> TagEntry: globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).entrySet()){
                            String sExistingTagCleaned = TagEntry.getValue().sTagText.toLowerCase().trim();
                            if(sExistingTagCleaned.equals(sIncomingTagCleaned)){
                                bTagFound = true;
                                aliIdentifiedTags.add(TagEntry.getValue().iTagID);
                                break;
                            }
                        }
                        if(!bTagFound){
                            alsUnidentifiedTags.add(sTag.trim());
                        }
                    }
                    break;


                case VIDEO_DOWNLOAD_LINK:
                    globalClass.BroadcastProgress(true, "Analyzing video link. " + vdsk.sSearchStringMatchContent,
                            false, 0,
                            false, "",
                            sIntentActionFilter);
                    try {
                        URL urlVideoLink = null;
                        if(vdsk.bMatchFound) {
                            //If using textual-search found data:
                            urlVideoLink = new URL(vdsk.sSearchStringMatchContent);
                        } else {

                            //If this video link was not found via textual search, attempt to get the video title from the webpage via XPath:
                            String sXPathExpressionVideoLinkLocator = vdsk.sSXPathExpression;
                            if(sXPathExpressionVideoLinkLocator == null || sXPathExpressionVideoLinkLocator.equals("")){
                                break;
                            }
                            try {
                                //Use an xPathExpression (similar to RegEx) to look for a video link in the html/xml:
                                Object[] objsVideoLink = node.evaluateXPath(sXPathExpressionVideoLinkLocator);
                                //Check to see if we found anything:
                                if (objsVideoLink != null && objsVideoLink.length > 0) {
                                    //If we found something, assign it to a string:
                                    for (Object oVideoLink : objsVideoLink) {
                                        String sVideoLink = oVideoLink.toString();
                                        if(!sVideoLink.equals("")){
                                            urlVideoLink = new URL(sVideoLink);
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                break;
                            }
                        } //End search for video link URL.

                        if(urlVideoLink == null){
                            //If we still have not found related data, continue to the next vdsk.
                            break;
                        }



                        //Locate a file name, likely in between the last '/' and either a '?' or length of string.
                        String sTempFilename;
                        sTempFilename = vdsk.sSearchStringMatchContent;
                        int iStartLocation = Math.max(sTempFilename.lastIndexOf("/") + 1, 0);
                        int iEndLocation;
                        int iSpecialEndCharLocation = sTempFilename.lastIndexOf("?");
                        if (iSpecialEndCharLocation > 0) {
                            iEndLocation = iSpecialEndCharLocation;
                        } else {
                            iEndLocation = sTempFilename.length();
                        }
                        sTempFilename = sTempFilename.substring(iStartLocation, iEndLocation);
                        ItemClass_File icf = new ItemClass_File(ItemClass_File.TYPE_URL, sTempFilename);
                        icf.sURLVideoLink = vdsk.sSearchStringMatchContent;

                        HttpURLConnection connection = (HttpURLConnection) urlVideoLink.openConnection();
                        connection.setRequestProperty("Accept-Encoding", "identity");
                        vdsk.lFileSize = connection.getContentLength(); //Returns -1 if content size is not in the header.
                        icf.lSizeBytes = vdsk.lFileSize;
                        connection.disconnect();

                        if(sTitle.equals("")){
                            sTitle = icWebDataLocator.sWebPageTitle;
                        }
                        icf.sTitle = sTitle;
                        icf.sURLThumbnail = sURLThumbnail;
                        icf.alsUnidentifiedTags = alsUnidentifiedTags; //Assign textual string of tags. Will digest and convert/import new tags if user chooses to continue import.
                        icf.aliRecognizedTags = aliIdentifiedTags; //todo: redundant?
                        icf.aliProspectiveTags = aliIdentifiedTags;

                        alicf_VideoDownloadFileItems.add(icf);

                    } catch (Exception e) {
                        vdsk.bErrorWithLink = true;
                        vdsk.sErrorMessage = e.getMessage();
                    }

                    break;



                case VIDEO_DOWNLOAD_M3U8:
                    globalClass.BroadcastProgress(true, "Analyzing video stream. " + vdsk.sSearchStringMatchContent,
                            false, 0,
                            false, "",
                            sIntentActionFilter);
                    try {

                        String sURLM3U8VideoLink = "";
                        URL urlM3U8VideoLink = null;
                        if(vdsk.bMatchFound) {
                            //If using textual-search found data:
                            sURLM3U8VideoLink = vdsk.sSearchStringMatchContent;
                            urlM3U8VideoLink = new URL(sURLM3U8VideoLink);
                        } else {

                            //Attempt to get the video title from the webpage via XPath:
                            String sXPathExpressionVideoLinkLocator = vdsk.sSXPathExpression;
                            if(sXPathExpressionVideoLinkLocator == null || sXPathExpressionVideoLinkLocator.equals("")){
                                break;
                            }
                            try {
                                //Use an xPathExpression (similar to RegEx) to look for a video link in the html/xml:
                                Object[] objsVideoLink = node.evaluateXPath(sXPathExpressionVideoLinkLocator);
                                //Check to see if we found anything:
                                if (objsVideoLink != null && objsVideoLink.length > 0) {
                                    //If we found something, assign it to a string:
                                    for (Object oVideoLink : objsVideoLink) {
                                        sURLM3U8VideoLink = oVideoLink.toString();
                                        if(!sURLM3U8VideoLink.equals("")){
                                            sURLM3U8VideoLink = GlobalClass.cleanHTMLCodedCharacters(sURLM3U8VideoLink);
                                            urlM3U8VideoLink = new URL(sURLM3U8VideoLink);
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                break;
                            }
                        } //End search for video link URL.

                        if(urlM3U8VideoLink == null){
                            //If we still have not found related data, continue to the next vdsk.
                            break;
                        }

                        URLConnection connection = urlM3U8VideoLink.openConnection();
                        connection.connect();

                        // download the file
                        InputStream inputStream = new BufferedInputStream(urlM3U8VideoLink.openStream(), 1024 * 8);


                        StringBuilder sbM3U8Content = new StringBuilder();
                        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                        for (String line; (line = r.readLine()) != null; ) {
                            sbM3U8Content.append(line).append('\n');
                        }
                        inputStream.close();









                        /*
                        HLS: Http Live Streaming
                        https://www.toptal.com/apple/introduction-to-http-live-streaming-hls#:~:text=What%20is%20an%20M3U8%20file,used%20to%20define%20media%20streams.

                        Below is an example of an M3U8 "master" file. It points to other M3U8 files with video "chunks".

                        M3U comments begin with the '#' character, unless ended with a semicolon.
                        My comments begin with '!'.
                        Example M3U8 data:

                        #EXTM3U               !Header. Must be first line. Required. Standard.
                        #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=763904,RESOLUTION=854x480,NAME="480p"  !Optional. This example gives some data about the stream.
                        hls-480p-fe738.m3u8?e=1616648203&l=0&h=8f2321bd696368a1795a61fc978a0dbb
                        #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1327104,RESOLUTION=1280x720,NAME="720p"
                        hls-720p-02c5c.m3u8?e=1616648203&l=0&h=8f2321bd696368a1795a61fc978a0dbbf
                        #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=2760704,RESOLUTION=1920x1080,NAME="1080p"
                        hls-1080p-cffd4.m3u8?e=1616648203&l=0&h=8f2321bd696368a1795a61fc978a0dbb
                        #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=423936,RESOLUTION=640x360,NAME="360p"
                        hls-360p-f6185.m3u8?e=1616648203&l=0&h=8f2321bd696368a1795a61fc978a0dbb
                        #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=155648,RESOLUTION=444x250,NAME="250p"
                        hls-250p-2b29e.m3u8?e=1616648203&l=0&h=8f2321bd696368a1795a61fc978a0dbb
                         */

                        if(globalClass.gbLogM3U8Files) {
                            //Write the m3u8 master file to the logs folder for debugging purposes:
                            String sShortFileName = sURLM3U8VideoLink.substring(sURLM3U8VideoLink.lastIndexOf("/") + 1);
                            sShortFileName = Service_Import.cleanFileNameViaTrim(sShortFileName);
                            String sM3U8FilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                                    File.separator + GlobalClass.GetTimeStampFileSafe() + "_" + sShortFileName + ".txt";
                            File fM3U8 = new File(sM3U8FilePath);
                            FileWriter fwM3U8File = new FileWriter(fM3U8, true);
                            fwM3U8File.write("#" + sURLM3U8VideoLink + "\n");
                            fwM3U8File.write(sbM3U8Content.toString());
                            fwM3U8File.flush();
                            fwM3U8File.close();
                        }


                        //Locate the M3U8 items:
                        //Go through the M3U8 master list and identify the .M3U8 files.
                        String sM3U8_MasterData = sbM3U8Content.toString();
                        String[] sM3U8_MasterDataLines = sM3U8_MasterData.split("\n");
                        ArrayList<ItemClass_M3U8> al_M3U8 = new ArrayList<>();
                        boolean bReadingM3U8Item = false;
                        ItemClass_M3U8 icM3U8 = null;
                        for (String sLine : sM3U8_MasterDataLines) {
                            if (sLine.startsWith("#EXT-X-STREAM-INF")) {
                                bReadingM3U8Item = true;
                                icM3U8 = new ItemClass_M3U8();
                                icM3U8.sBaseURL = sURLM3U8VideoLink.substring(0, sURLM3U8VideoLink.lastIndexOf("/"));
                                String[] sTemp1 = sLine.split(":");
                                if (sTemp1.length > 1) {
                                    String[] sTemp2 = sTemp1[1].split(",");
                                    boolean bCODECFlag = false;
                                    for (String sDataItem : sTemp2) {
                                        String[] sDataSplit = sDataItem.split("=");
                                        if (sDataSplit.length > 1) {
                                            switch (sDataSplit[0]) {
                                                case "PROGRAM-ID":
                                                    icM3U8.sProgramID = sDataSplit[1];
                                                    bCODECFlag = false;
                                                    break;
                                                case "BANDWIDTH":
                                                    icM3U8.sBandwidth = sDataSplit[1];
                                                    bCODECFlag = false;
                                                    break;
                                                case "RESOLUTION":
                                                    icM3U8.sResolution = sDataSplit[1];
                                                    bCODECFlag = false;
                                                    break;
                                                case "NAME":
                                                    icM3U8.sName = sDataSplit[1];
                                                    bCODECFlag = false;
                                                    break;
                                                case "CODECS":
                                                    icM3U8.sCODECs = sDataSplit[1];
                                                    bCODECFlag = true;
                                                    break;

                                            }
                                        } else if (bCODECFlag){
                                            //CODECs may be comma-separated, and so there will be no equals-sign
                                            //  creating an array of 2 items. If the CODEC item was just
                                            //  processed, and this item is singular, it is almost
                                            //  definitely another CODEC definition. Process accordingly:
                                            if(!icM3U8.sCODECs.equals("")){
                                                icM3U8.sCODECs = icM3U8.sCODECs + "," + sDataSplit[0];
                                            }
                                        }
                                    }
                                }
                            } else if (bReadingM3U8Item) {
                                bReadingM3U8Item = false;
                                if (icM3U8 != null) {
                                    if(sLine.contains("/")){
                                        int iHostStart = icM3U8.sBaseURL.indexOf("//");
                                        String sHTTPPrefix = icM3U8.sBaseURL.substring(0, iHostStart);
                                        String sHost = icM3U8.sBaseURL.substring(iHostStart + 2);
                                        int iHostEnd = sHost.indexOf("/");
                                        sHost = sHost.substring(0, iHostEnd);
                                        sHost = sHTTPPrefix + "//" + sHost;
                                        String sPath = sLine.substring(0, sLine.lastIndexOf("/"));
                                        icM3U8.sBaseURL = sHost + sPath;
                                        sLine = sLine.substring(sLine.lastIndexOf("/") + 1);
                                    }
                                    icM3U8.sFileName = sLine;
                                    al_M3U8.add(icM3U8);
                                }
                            }
                        }


                        //Example of an M3U8 item file containing TS file entries:
                        /*
                        #EXTM3U
                        #EXT-X-VERSION:3
                        #EXT-X-TARGETDURATION:10
                        #EXT-X-MEDIA-SEQUENCE:0
                        #EXTINF:10.010011,
                        hls-480p-fe7380.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                        #EXTINF:10.010011,
                        hls-480p-fe7381.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                        #EXTINF:10.010011,
                        hls-480p-fe7382.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                        #EXTINF:10.010011,
                        hls-480p-fe73880.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                        #EXTINF:9.743067,
                        hls-480p-fe73881.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                        #EXT-X-ENDLIST
                        e7dff1bc
                        #EXTINF:10.010011,
                        hls-480p-fe73871.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                        #EXTINF:10.010011,
                        hls-480p-fe73872.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                        #EXTINF:10.010011,
                        hls-480p-fe73877.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
                        #EXTIN
                        */

                        //Evaluate the M3U8 files and create a list of the .ts files in each M3U8 item:
                        for (ItemClass_M3U8 icM3U8_entry : al_M3U8) {
                            String sUrl;
                            sUrl = icM3U8_entry.sBaseURL + "/" + icM3U8_entry.sFileName;

                            URL url = new URL(sUrl);
                            connection = url.openConnection();
                            connection.connect();

                            // download the M3U8 text file:
                            inputStream = new BufferedInputStream(url.openStream(), 1024 * 8);
                            r = new BufferedReader(new InputStreamReader(inputStream));
                            StringBuilder total = new StringBuilder();
                            for (String line; (line = r.readLine()) != null; ) {
                                total.append(line).append('\n');
                            }
                            String sM3U8Content = total.toString();
                            inputStream.close();

                            String sShortFileName = Service_Import.cleanFileNameViaTrim(icM3U8_entry.sFileName);
                            if(globalClass.gbLogM3U8Files) {
                                //Write the m3u8 file to the logs folder for debugging purposes:

                                String sM3U8FilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                                        File.separator + GlobalClass.GetTimeStampFileSafe() + "_" + sShortFileName + ".txt";
                                File fM3U8 = new File(sM3U8FilePath);
                                FileWriter fwM3U8File = new FileWriter(fM3U8, true);
                                fwM3U8File.write(sM3U8Content);
                                fwM3U8File.flush();
                                fwM3U8File.close();
                            }

                            /*final String sM3U8InterprettedFilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                                    File.separator + GlobalClass.GetTimeStampFileSafe() + "_" + sShortFileName + "_interpretted.txt";
                            final File fM3U8Interpretted = new File(sM3U8InterprettedFilePath);
                            FileWriter fwM3U8InterprettedFile;
                            fwM3U8InterprettedFile = new FileWriter(fM3U8Interpretted, true);*/


                            //Evaluate lines in the M3U8 file to check if a .ts file name and add it to the arraylist if so:
                            String[] sLines = sM3U8Content.split("\n");
                            float fDurationInSeconds = 0.0f;
                            for (String sLine : sLines) {

                                if (!sLine.startsWith("#") && sLine.contains(".ts")) {// && sLine.startsWith("hls")) {
                                    if (icM3U8_entry.als_TSDownloads == null) {
                                        icM3U8_entry.als_TSDownloads = new ArrayList<>();
                                    }
                                    icM3U8_entry.als_TSDownloads.add(sLine); //Add our detected TS download address to the M3U8 item.
                                    //fwM3U8InterprettedFile.write(sLine + "\n");
                                } else if (sLine.contains("ENDLIST")) {
                                    break;
                                } else if (sLine.contains("EXTINF")){
                                    //Try to pull out duration:
                                    String[] sData1 = sLine.split(":");
                                    if(sData1.length > 1){
                                        String sData2 = sData1[1];
                                        sData2 = sData2.replace(",","");
                                        //sData2 should now have the duration of the ts file.
                                        try{
                                            float fTemp = Float.parseFloat(sData2);
                                            fDurationInSeconds = fDurationInSeconds + fTemp;
                                        } catch (Exception e){
                                            Log.d("M3U8", "String to float conversion error. Cannot convert: " + sData2);
                                        }
                                    }
                                }

                            }
                            /*fwM3U8InterprettedFile.flush();
                            fwM3U8InterprettedFile.close();*/

                            icM3U8_entry.fDurationInSeconds = fDurationInSeconds; //Load the duration now for confirmation of video concatenation completion later.

                        }


                        /*//Test download of TS files:
                        File fDestinationFolder = new File(
                                globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath()
                                        + File.separator + "Test");
                        if(!fDestinationFolder.exists()){

                            fDestinationFolder.mkdir();

                            DownloadManager downloadManager = null;

                            downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

                            String sDownloadName;
                            for(int i = 0; i < alals_TSDownloads.get(0).size(); i++){
                                sDownloadName = alals_TSDownloads.get(0).get(i);
                                //Use the download manager to download the file:
                                String sAbbrevFileName = sDownloadName.substring(0,sDownloadName.lastIndexOf("?"));
                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(al_M3U8.get(0).sBaseURL + "/" + sDownloadName));
                                File fDestinationFile = new File(fDestinationFolder.getPath() + File.separator + sAbbrevFileName);
                                request.setTitle("AG Gallery+ File Download : " + sAbbrevFileName)
                                        .setDescription(sAbbrevFileName)
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                                        //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                                        .setMimeType("application/octet-stream")
                                        .setDestinationUri(Uri.fromFile(fDestinationFile));
                                downloadManager.enqueue(request);
                            }
                        }*/

                        //Obtain size of each TS file set of downloads:
                        //Loop through the M3U8 entries, such as video @ 240x320, video @ 640x480, @720p, @1080p, etc:

                        for (ItemClass_M3U8 icM3U8_entry : al_M3U8) {
                            //Loop through the TS downloads for each of the M3U8 entries and accumulate the file sizes:

                            int iFileSizeLoopCount = 0;
                            int iDataAcquiredLoopCount = 0;
                            long lFileSizeAccumulator = 0;

                            for (String sTSDownloadAddress : icM3U8_entry.als_TSDownloads) {


                                String sFilename = icM3U8_entry.sBaseURL + "/" + icM3U8_entry.sFileName;
                                if(icM3U8_entry.sFileName.startsWith("/")){
                                    sFilename = icM3U8_entry.sHost + icM3U8_entry.sFileName;
                                } else {
                                    sFilename = icM3U8_entry.sBaseURL + "/" + icM3U8_entry.sFileName;
                                }

                                URL urlPage = new URL(icM3U8_entry.sBaseURL + "/" + sTSDownloadAddress);

                                globalClass.BroadcastProgress(true, "Getting file size data for video stream: " + sFilename + "\n",
                                        false, 0,
                                        false, "",
                                        sIntentActionFilter); //Broadcast progress

                                HttpURLConnection httpURLConnection = (HttpURLConnection) urlPage.openConnection();
                                httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
                                long lSingleTSFileDownloadSize = httpURLConnection.getContentLength(); //Returns -1 if content size is not in the header.
                                httpURLConnection.disconnect();
                                iFileSizeLoopCount++;

                                if (lSingleTSFileDownloadSize > 0) {
                                    iDataAcquiredLoopCount++;
                                    lFileSizeAccumulator = lFileSizeAccumulator + lSingleTSFileDownloadSize;
                                }
                                if (iDataAcquiredLoopCount == 5 || iFileSizeLoopCount == 10) {
                                    //A set of the TS files will be representative of all of the TS file sizes for the given video.
                                    icM3U8_entry.lTotalTSFileSetSize = (lFileSizeAccumulator / iDataAcquiredLoopCount) * icM3U8_entry.als_TSDownloads.size();
                                    break;
                                }
                            }
                            //Create a file item to record the results:
                            String sFilename = Service_Import.cleanFileNameViaTrim(icM3U8_entry.sFileName);
                            ItemClass_File icf = new ItemClass_File(ItemClass_File.TYPE_M3U8, sFilename);
                            icf.ic_M3U8 = icM3U8_entry;
                            icf.lSizeBytes = icM3U8_entry.lTotalTSFileSetSize;
                            icf.sURLThumbnail = sURLThumbnail;
                            icf.alsUnidentifiedTags = alsUnidentifiedTags; //Assign textual string of tags. Will digest and convert/import new tags if user chooses to continue import.
                            icf.aliRecognizedTags = aliIdentifiedTags; //todo: redundant?
                            icf.aliProspectiveTags = aliIdentifiedTags;
                            if(sTitle.equals("")){
                                sTitle = icWebDataLocator.sWebPageTitle;
                            }
                            icf.sTitle = sTitle;
                            alicf_VideoDownloadFileItems.add(icf); //Add item to list of file items to return;

                        }
                        //Finished obtaining sizes of the TS file sets.

                    } catch (Exception e) {
                        vdsk.bErrorWithLink = true;
                        vdsk.sErrorMessage = e.getMessage();
                        if(vdsk.sErrorMessage.startsWith("http")){
                            //Error will be meaningless to the user. Don't present a message.
                            break;
                        }
                        if(vdsk.sErrorMessage.length() > 50){
                            vdsk.sErrorMessage = vdsk.sErrorMessage.substring(0, 50) + "...";
                        }
                        globalClass.problemNotificationConfig(vdsk.sErrorMessage, sIntentActionFilter);
                    }
                    break;
            }


        } //End loop searching for data within the HTML

        //Broadcast a message to be picked-up by the VideoWebDetect fragment:
        /*Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(sIntentActionFilter);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        if(!bProblem) {
            broadcastIntent.putExtra(EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, true);
            broadcastIntent.putExtra(EXTRA_AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE, alicf_VideoDownloadFileItems);
        } else {
            broadcastIntent.putExtra(EXTRA_BOOL_PROBLEM, bProblem);
            broadcastIntent.putExtra(EXTRA_STRING_PROBLEM, sProblemMessage);
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);*/

        globalClass.BroadcastProgress(true, "HTML examination complete. Click 'Next' to continue.",
                false, 0,
                false, "",
                sIntentActionFilter);

        if(!bProblem) {
            //Also send a broadcast to Activity Import to capture the download items in an array adapter:
            Intent broadcastIntent_VideoWebDetectResponse = new Intent();
            broadcastIntent_VideoWebDetectResponse.putExtra(GlobalClass.EXTRA_BOOL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE, true);
            broadcastIntent_VideoWebDetectResponse.putExtra(GlobalClass.EXTRA_AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE, alicf_VideoDownloadFileItems);
            broadcastIntent_VideoWebDetectResponse.setAction(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
            broadcastIntent_VideoWebDetectResponse.addCategory(Intent.CATEGORY_DEFAULT);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_VideoWebDetectResponse);
        }


        return Result.success();
    }

}
