package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Xml;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.TagNode;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import androidx.annotation.Nullable;

import org.htmlcleaner.HtmlCleaner;

public class ComicDetailsDataService extends IntentService {

    //Global Constants
    public static final String COMIC_DETAILS_COMIC_ID = "COMIC_DETAILS_COMIC_ID";

    public static final String COMIC_DETAILS_COMIC_TITLE_ACQUIRED = "COMIC_DETAILS_COMIC_TITLE_ACQUIRED";
    public static final String COMIC_DETAILS_COMIC_TITLE = "COMIC_DETAILS_COMIC_TITLE";
    public static final String COMIC_DETAILS_PARODIES_DATA_ACQUIRED = "COMIC_DETAILS_PARODIES_DATA_ACQUIRED";
    public static final String COMIC_DETAILS_PARODIES_DATA = "COMIC_DETAILS_PARODIES_DATA";
    public static final String COMIC_DETAILS_CHARACTERS_DATA_ACQUIRED = "COMIC_DETAILS_CHARACTERS_DATA_ACQUIRED";
    public static final String COMIC_DETAILS_CHARACTERS_DATA = "COMIC_DETAILS_CHARACTERS_DATA";
    public static final String COMIC_DETAILS_TAGS_DATA_ACQUIRED = "COMIC_DETAILS_TAGS_DATA_ACQUIRED";
    public static final String COMIC_DETAILS_TAGS_DATA = "COMIC_DETAILS_TAGS_DATA";
    public static final String COMIC_DETAILS_ARTISTS_DATA_ACQUIRED = "COMIC_DETAILS_ARTISTS_DATA_ACQUIRED";
    public static final String COMIC_DETAILS_ARTISTS_DATA = "COMIC_DETAILS_ARTISTS_DATA";
    public static final String COMIC_DETAILS_GROUPS_DATA_ACQUIRED = "COMIC_DETAILS_GROUPS_DATA_ACQUIRED";
    public static final String COMIC_DETAILS_GROUPS_DATA = "COMIC_DETAILS_GROUPS_DATA";
    public static final String COMIC_DETAILS_LANGUAGES_DATA_ACQUIRED = "COMIC_DETAILS_LANGUAGES_DATA_ACQUIRED";
    public static final String COMIC_DETAILS_LANGUAGES_DATA = "COMIC_DETAILS_LANGUAGES_DATA";
    public static final String COMIC_DETAILS_CATEGORIES_DATA_ACQUIRED = "COMIC_DETAILS_CATEGORIES_DATA_ACQUIRED";
    public static final String COMIC_DETAILS_CATEGORIES_DATA = "COMIC_DETAILS_CATEGORIES_DATA";
    public static final String COMIC_DETAILS_PAGES_DATA_ACQUIRED = "COMIC_DETAILS_PAGES_DATA_ACQUIRED";
    public static final String COMIC_DETAILS_PAGES_DATA = "COMIC_DETAILS_PAGES_DATA";

    public static final String COMIC_DETAILS_SUCCESS = "COMIC_DETAILS_SUCCESS";
    public static final String COMIC_DETAILS_ERROR_MESSAGE = "COMIC_DETAILS_ERROR_MESSAGE";

    //Global Variables
    private GlobalClass globalClass;

    private String[] gsDataBlockIDs = new String[]{
            "Parodies:",
            "Characters:",
            "Tags:",
            "Artists:",
            "Groups:",
            "Languages:",
            "Categories:",
            "Pages:",
            "Uploaded:"}; //We ignore the upload date data, but still include it.

    private String[] gsComicDetailsDataBooleans = new String[]{
            COMIC_DETAILS_PARODIES_DATA_ACQUIRED,
            COMIC_DETAILS_CHARACTERS_DATA_ACQUIRED,
            COMIC_DETAILS_TAGS_DATA_ACQUIRED,
            COMIC_DETAILS_ARTISTS_DATA_ACQUIRED,
            COMIC_DETAILS_GROUPS_DATA_ACQUIRED,
            COMIC_DETAILS_LANGUAGES_DATA_ACQUIRED,
            COMIC_DETAILS_CATEGORIES_DATA_ACQUIRED,
            COMIC_DETAILS_PAGES_DATA_ACQUIRED};

    private String[] gsComicDetailsDataContentMarkers = new String[]{
            COMIC_DETAILS_PARODIES_DATA,
            COMIC_DETAILS_CHARACTERS_DATA,
            COMIC_DETAILS_TAGS_DATA,
            COMIC_DETAILS_ARTISTS_DATA,
            COMIC_DETAILS_GROUPS_DATA,
            COMIC_DETAILS_LANGUAGES_DATA,
            COMIC_DETAILS_CATEGORIES_DATA,
            COMIC_DETAILS_PAGES_DATA};


    public ComicDetailsDataService() { super("ComicDetailsDataService"); }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        globalClass = (GlobalClass) getApplicationContext();

        String sComicID = intent.getStringExtra(COMIC_DETAILS_COMIC_ID);
        String sComicTitle;


        //Broadcast a message to be picked-up by the Import Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ComicDetailsActivity.ComicDetailsResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        try {
            //Get the data from the WebPage:
            URL url = new URL(globalClass.snHentai_Comic_Address_Prefix + sComicID);
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
            String s="";
            boolean bDataAcquired = false;
            //Check to see if we found anything:
            if (objsTagNodeTitle != null && objsTagNodeTitle.length > 0) {
                //If we found something, assign it to a string:
                s = ((TagNode) objsTagNodeTitle[0]).getText().toString();
                bDataAcquired = true;
            }
            broadcastIntent.putExtra(COMIC_DETAILS_COMIC_TITLE_ACQUIRED, bDataAcquired);
            broadcastIntent.putExtra(COMIC_DETAILS_COMIC_TITLE, s);


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
            sData = sData.replaceAll("  ","\t");
            sData = sData.replaceAll("\t\t","\t");
            sData = sData.replaceAll("\t\t","\t");
            sData = sData.replaceAll("\t\t","\t");
            sData = sData.replaceAll("^\t",""); //Get rid of any leading tab character.

            String[] sDataBreakout = sData.split("\t");


            //Process each named data block. Data blocks are parodies, characters, tags, etc.
            for(int i = 0; i < gsDataBlockIDs.length - 1; i++) {
                //gsDataBlockIDs.length - 1 ====> We are ignoring the last data block, "Uploaded:", the upload date.
                sData = "";
                bDataAcquired = false;
                int iterator = -1; //Determine where in the sequence of objects the current data block will appear.
                for (int k = 0; k < sDataBreakout.length - 1; k++) {
                    //Find the DataBreakout index (k) that contains the DataBlock identifier (not the data):
                    if (sDataBreakout[k].contains(gsDataBlockIDs[i])) {

                            if (sDataBreakout[k + 1].contains(gsDataBlockIDs[i + 1])) {
                                //If we are here, then it means that there was no data between the current
                                //  data block and the next data block. Skip gathering the data for this
                                //  data block.
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
                        sData = sData.replaceAll("\\d{1}K", "\t");
                        sData = sData.replaceAll("\\d{4}", "\t");
                        sData = sData.replaceAll("\\d{3}", "\t");
                        sData = sData.replaceAll("\\d{2}", "\t");
                        sData = sData.replaceAll("\\d{1}", "\t");
                    }
                    //Reformat the data:
                    String[] sItems = sData.split("\t");
                    StringBuilder sbData = new StringBuilder();
                    sbData.append(sItems[0]);
                    for(int m = 1; m < sItems.length; m++){
                        sbData.append(", ");
                        sbData.append(sItems[m]);
                    }

                    bDataAcquired = true;
                    broadcastIntent.putExtra(gsComicDetailsDataContentMarkers[i], sbData.toString());
                }
                //Tell the broadcast listener that there IS or IS NOT data available for this data block:
                broadcastIntent.putExtra(gsComicDetailsDataBooleans[i], bDataAcquired);


            }

            //broadcastIntent.putExtra(COMIC_DETAILS_TAG_DATA, sTagData);
            broadcastIntent.putExtra(COMIC_DETAILS_SUCCESS, true);
        } catch(Exception e){
            String sMsg = e.getMessage();
            broadcastIntent.putExtra(COMIC_DETAILS_ERROR_MESSAGE, sMsg);
            broadcastIntent.putExtra(COMIC_DETAILS_SUCCESS, false);
        }

        sendBroadcast(broadcastIntent);
    }




}
