package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.TagNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import androidx.annotation.Nullable;

import org.htmlcleaner.HtmlCleaner;

public class Service_ComicDetails extends IntentService {

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

    private final String[] gsComicDetailsDataBooleans = new String[]{
            COMIC_DETAILS_PARODIES_DATA_ACQUIRED,
            COMIC_DETAILS_CHARACTERS_DATA_ACQUIRED,
            COMIC_DETAILS_TAGS_DATA_ACQUIRED,
            COMIC_DETAILS_ARTISTS_DATA_ACQUIRED,
            COMIC_DETAILS_GROUPS_DATA_ACQUIRED,
            COMIC_DETAILS_LANGUAGES_DATA_ACQUIRED,
            COMIC_DETAILS_CATEGORIES_DATA_ACQUIRED,
            COMIC_DETAILS_PAGES_DATA_ACQUIRED};

    private final String[] gsComicDetailsDataContentMarkers = new String[]{
            COMIC_DETAILS_PARODIES_DATA,
            COMIC_DETAILS_CHARACTERS_DATA,
            COMIC_DETAILS_TAGS_DATA,
            COMIC_DETAILS_ARTISTS_DATA,
            COMIC_DETAILS_GROUPS_DATA,
            COMIC_DETAILS_LANGUAGES_DATA,
            COMIC_DETAILS_CATEGORIES_DATA,
            COMIC_DETAILS_PAGES_DATA};




    public Service_ComicDetails() { super("ComicDetailsDataService"); }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        globalClass = (GlobalClass) getApplicationContext();

        assert intent != null;
        String sComicID = intent.getStringExtra(COMIC_DETAILS_COMIC_ID);

        String[] sComicData =  getOnlineComicDetails(sComicID);


        //Broadcast a message to be picked-up by the Import Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_ComicDetails.ComicDetailsResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);


        //Apply booleans to the intent to tell the receiver if there is data available,
        //  and set the data where appropriate:

        //Set to return the title data:
        int i = 0;
        if(sComicData[i].length()>0) {
            broadcastIntent.putExtra(COMIC_DETAILS_COMIC_TITLE_ACQUIRED, true);
            broadcastIntent.putExtra(COMIC_DETAILS_COMIC_TITLE, sComicData[i]);
        } else {
            broadcastIntent.putExtra(COMIC_DETAILS_COMIC_TITLE_ACQUIRED, false);
        }

        //set to return the datablock data:
        for(i = 1; i < gsComicDetailsDataContentMarkers.length + 1; i++){
            if(sComicData[i].length()>0) {
                broadcastIntent.putExtra(gsComicDetailsDataBooleans[i - 1], true);
                broadcastIntent.putExtra(gsComicDetailsDataContentMarkers[i - 1], sComicData[i]);
            } else {
                broadcastIntent.putExtra(gsComicDetailsDataBooleans[i - 1], false);
            }
        }

        //set to return any error message that arose:
        if(sComicData[i].length()>0) {  //'i' iterates to the next entry at exit from For loop, above.
            broadcastIntent.putExtra(COMIC_DETAILS_SUCCESS, false);
            broadcastIntent.putExtra(COMIC_DETAILS_COMIC_TITLE, sComicData[i]);
        } else {
            broadcastIntent.putExtra(COMIC_DETAILS_SUCCESS, true);
        }

        sendBroadcast(broadcastIntent);
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
    public static final int COMIC_DETAILS_ERROR_MSG_INDEX = 9;

    public String[] getOnlineComicDetails(String sComicID){

        int j = gsComicDetailsDataContentMarkers.length + 2;
        String[] sReturnData = new String[j];
        //First array element is for comic title.
        //Elements 1-8 are data block results.
        //Last array element is for error message.
        for(int i = 0; i < j; i++){
            sReturnData[i] = "";
        }


        String sComicTitle = "";

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
        } catch(Exception e){
            String sMsg = e.getMessage();
            sReturnData[COMIC_DETAILS_ERROR_MSG_INDEX] =  sMsg;
        }

        return sReturnData;

    }

}
