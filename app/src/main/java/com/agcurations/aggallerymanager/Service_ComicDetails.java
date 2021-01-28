package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.TagNode;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.Nullable;

import org.htmlcleaner.HtmlCleaner;

public class Service_ComicDetails extends IntentService {

    //Global Constants
    public static final String COMIC_CATALOG_ITEM = "COMIC_CATALOG_ITEM";

    public static final String COMIC_DETAILS_SUCCESS = "COMIC_DETAILS_SUCCESS";
    public static final String COMIC_DETAILS_ERROR_MESSAGE = "COMIC_DETAILS_ERROR_MESSAGE";
    public static final String COMIC_MISSING_PAGES_ACQUIRED = "COMIC_MISSING_PAGES_ACQUIRED";

    //Global Variables
    private GlobalClass globalClass;

    //We don't grab the title from one of the html data blocks on nHentai.net.
    private final String[] gsDataBlockIDs = new String[]{
            "Parodies:",
            "Characters:",
            "Tags:",
            "Artists:",
            "Groups:",
            "Languages:",
            "Categories:",
            "Pages:",
            "Uploaded:"}; //We ignore the upload date data, but still include it.

    public Service_ComicDetails() { super("ComicDetailsDataService"); }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        globalClass = (GlobalClass) getApplicationContext();

        assert intent != null;
        ItemClass_CatalogItem ci = (ItemClass_CatalogItem) intent.getSerializableExtra(COMIC_CATALOG_ITEM);


        if(ci.sSource.startsWith("http")){
            getNHOnlineComicDetails(ci);
        }



    }






    //Return data order for getOnlineComicDetails:
    public static final int COMIC_DETAILS_TITLE_INDEX = 0;
    public static int COMIC_DETAILS_PARODIES_DATA_INDEX = 1;
    public static int COMIC_DETAILS_CHARACTERS_DATA_INDEX = 2;
    public static int COMIC_DETAILS_TAGS_DATA_INDEX = 3;
    public static int COMIC_DETAILS_ARTISTS_DATA_INDEX = 4;
    public static int COMIC_DETAILS_GROUPS_DATA_INDEX = 5;
    public static int COMIC_DETAILS_LANGUAGES_DATA_INDEX = 6;
    public static int COMIC_DETAILS_CATEGORIES_DATA_INDEX = 7;
    public static int COMIC_DETAILS_PAGES_DATA_INDEX = 8;
    //public static final int COMIC_DETAILS_ERROR_MSG_INDEX = 9;

    public void getNHOnlineComicDetails(ItemClass_CatalogItem ci){

        //Broadcast a message to be picked-up by the Import Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_ComicDetails.ComicDetailsResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        int j = gsDataBlockIDs.length + 1;
        String[] sReturnData = new String[j];
        //First array element is for comic title.
        //Elements 1-8 are data block results.
        //Last array element is for error message.
        for(int i = 0; i < j; i++){
            sReturnData[i] = "";
        }


        String sComicTitle = "";

        boolean bMissingComicPagesAcquired = false;

        try {
            //Get the data from the WebPage:

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

            String sHTML = a.toString();
            sHTML = sHTML.replaceAll("tag-container field-name ", "tag-container field-name");

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
            TagNode node = pageParser.clean(sHTML);


            //Attempt to get the comic title from the WebPage html:
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
                if (iterator > 0){
                    sData = sDataBreakout[iterator];
                    if(!sDataBreakout[iterator-1].contains("Pages:")) { //Don't clean-out numbers if we are expecting numbers.
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
                    for(int m = 1; m < sItems.length; m++){
                        sbData.append(", ");
                        sbData.append(sItems[m]);
                    }
                    sReturnData[i + 1] = sbData.toString();
                }
            }


            //Get any missing comic pages:
            if(!ci.sComic_Missing_Pages.equals("")) {
                //If the catalog item is missing pages, attempt to recover those pages:
                sxPathExpression = globalClass.snHentai_Comic_Page_Thumbs_xPE;
                //sxPathExpression = "//div[@class='thumb-container']//img[@class='lazyload']//@data-src";
                //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
                Object[] objsTagNodeThumbnails = node.evaluateXPath(sxPathExpression);
                //Check to see if we found anything:
                String sImageAddressTemplate;
                String sGalleryID = "";
                TreeMap<Integer, String> tmFileIndexImageExtention = new TreeMap<>();
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
                        String sImageFilename = sImageAddress.substring(sImageAddress.lastIndexOf("/") + 1);
                        sImageFilename = sImageFilename.replace("t", ""); //Get rid of the 't', presummably for "thumbnail".
                        String[] sSplit = sImageFilename.split("\\.");
                        if (sSplit.length == 2) {
                            try {
                                Integer iPageNumber = Integer.parseInt(sSplit[0]);
                                tmFileIndexImageExtention.put(iPageNumber, sSplit[1]);
                            } catch (Exception ignored) {
                            }
                        }
                    }

                }
                ArrayList<Integer> aliMissingPages = GlobalClass.getIntegerArrayFromString(ci.sComic_Missing_Pages, ",");
                ArrayList<String[]> alsImageNameData = new ArrayList<>();
                String sNHComicID = ci.sSource;
                sNHComicID = sNHComicID.substring(0, sNHComicID.lastIndexOf("/"));
                sNHComicID = sNHComicID.substring(sNHComicID.lastIndexOf("/") + 1);
                if (sGalleryID.length() > 0) {
                    for(Integer iMissingPage: aliMissingPages) {
                        if(tmFileIndexImageExtention.containsKey(iMissingPage)) {
                            String sNHImageDownloadAddress = "https://i.nhentai.net/galleries/" + sGalleryID + "/" + iMissingPage + "." + tmFileIndexImageExtention.get(iMissingPage);
                            String sPageStringForFilename = String.format(Locale.getDefault(),"%03d", iMissingPage);
                            String sNewFilename = sNHComicID + "_Page_" + sPageStringForFilename + "." + tmFileIndexImageExtention.get(iMissingPage);
                            String[] sTemp = {sNHImageDownloadAddress, sNewFilename};
                            alsImageNameData.add(sTemp);
                        }
                    }
                }
                if(alsImageNameData.size() > 0){
                    //If there are image addresses to attempt to download...
                    try {
                        for(String[] sImageNameData: alsImageNameData) {
                            url = new URL(sImageNameData[0]);
                            URLConnection connection = url.openConnection();
                            connection.connect();

                            // this will be useful so that you can show a tipical 0-100%
                            // progress bar
                            //int lenghtOfFile = connection.getContentLength();

                            // download the file
                            InputStream input = new BufferedInputStream(url.openStream(), 8192);

                            String sNewFilename = sImageNameData[1];

                            String sNewFullPathFilename = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS] +
                                    File.separator + ci.sFolder_Name + File.separator + GlobalClass.JumbleFileName(sNewFilename);
                            File fNewFile = new File(sNewFullPathFilename);
                            if(!fNewFile.exists()) {
                                // Output stream
                                OutputStream output = new FileOutputStream(fNewFile.getPath());

                                byte[] data = new byte[1024];

                                //long total = 0;
                                int count;
                                while ((count = input.read(data)) != -1) {
                                    //total += count;
                                    // publishing the progress....
                                    // After this onProgressUpdate will be called
                                    //publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                                    // writing data to file
                                    output.write(data, 0, count);
                                }

                                // flushing output
                                output.flush();

                                // closing streams
                                output.close();
                                input.close();

                            }
                        }
                        //Success downloading files.
                        bMissingComicPagesAcquired = true;
                        //Recalculate missing comic pages, file count, and max page ID for this comic:
                        ci = globalClass.analyzeComicReportMissingPages(ci);

                    } catch (Exception e) {
                        Log.e("Error: ", e.getMessage());
                    }


                }


            }






        } catch(Exception e){
            String sMsg = e.getMessage();
            broadcastIntent.putExtra(COMIC_DETAILS_SUCCESS, false);
            broadcastIntent.putExtra(COMIC_DETAILS_ERROR_MESSAGE, sMsg);
            sendBroadcast(broadcastIntent);
            return;
        }

        ci.sComicName = sReturnData[COMIC_DETAILS_TITLE_INDEX];
        ci.sComicParodies = sReturnData[COMIC_DETAILS_PARODIES_DATA_INDEX];
        ci.sComicCharacters = sReturnData[COMIC_DETAILS_CHARACTERS_DATA_INDEX];

        //Form the tag integer array:
        String[] sTags = sReturnData[COMIC_DETAILS_TAGS_DATA_INDEX].split(", ");
        ArrayList<Integer> aliTags = new ArrayList<>();
        for(String sTag: sTags){
            aliTags.add(globalClass.getTagIDFromText(sTag, GlobalClass.MEDIA_CATEGORY_COMICS));
        }
        //Look for any tags that could not be found:
        for(int i = 0; i < aliTags.size(); i++){
            if(aliTags.get(i) == -1){
                //Create the tag:
                if(!sTags[i].equals("")) {
                    int iTag = globalClass.TagDataFile_CreateNewRecord(sTags[i], GlobalClass.MEDIA_CATEGORY_COMICS);
                    if(iTag != -1){
                        aliTags.add(i, iTag);
                    }
                }
            }
        }
        //Combine the found tags with any tags already assigned to the comic:
        ArrayList<Integer> aliPreAssignedTagIDs = GlobalClass.getIntegerArrayFromString(ci.sTags, ",");
        //Combine, no duplicates:
        TreeMap<Integer, Integer> tmCombinedTags = new TreeMap<>();
        for(Integer iTag: aliTags){
            if(iTag != -1) { //Don't put any unresolved tags.
                tmCombinedTags.put(iTag, iTag);
            }
        }
        for(Integer iTag: aliPreAssignedTagIDs){
            tmCombinedTags.put(iTag, iTag);
        }
        ArrayList<Integer> aliCombinedTags = new ArrayList<>();
        for(Map.Entry<Integer, Integer> tmEntry: tmCombinedTags.entrySet()){
            aliCombinedTags.add(tmEntry.getKey());
        }
        ci.sTags = GlobalClass.formDelimitedString(aliCombinedTags, ",");

        ci.sComicArtists = sReturnData[COMIC_DETAILS_ARTISTS_DATA_INDEX];
        ci.sComicGroups = sReturnData[COMIC_DETAILS_GROUPS_DATA_INDEX];
        ci.sComicLanguages = sReturnData[COMIC_DETAILS_LANGUAGES_DATA_INDEX];
        ci.sComicCategories = sReturnData[COMIC_DETAILS_CATEGORIES_DATA_INDEX];
        ci.iComicPages = Integer.parseInt(sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX]);

        //Apply booleans to the intent to tell the receiver success, data available,
        //  and set the data where appropriate:


        broadcastIntent.putExtra(COMIC_DETAILS_SUCCESS, true);
        broadcastIntent.putExtra(COMIC_CATALOG_ITEM, ci);
        broadcastIntent.putExtra(COMIC_MISSING_PAGES_ACQUIRED, bMissingComicPagesAcquired);

        //Log.d("Comics", "Finished downloading from " + ci.sSource);

        sendBroadcast(broadcastIntent);

    }

}
