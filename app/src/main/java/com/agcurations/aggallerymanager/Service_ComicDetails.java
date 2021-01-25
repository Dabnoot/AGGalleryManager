package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.TagNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.Nullable;

import org.htmlcleaner.HtmlCleaner;

public class Service_ComicDetails extends IntentService {

    //Global Constants
    public static final String COMIC_CATALOG_ITEM = "COMIC_CATALOG_ITEM";

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
    public static final int COMIC_DETAILS_ERROR_MSG_INDEX = 9;

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
        int i = 0;
        for(Integer iTag: aliTags){
            if(iTag == -1){
                //Create the tag:
                if(!sTags[i].equals("")) {
                    iTag = globalClass.TagDataFile_CreateNewRecord(sTags[i], GlobalClass.MEDIA_CATEGORY_COMICS);
                    if(iTag != -1){
                        aliTags.add(i, iTag);
                    }
                }
            }
            i++;
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
        String sTagIDsConcat = GlobalClass.formDelimitedString(aliCombinedTags, ",");
        ci.sTags = sTagIDsConcat;

        ci.sComicArtists = sReturnData[COMIC_DETAILS_ARTISTS_DATA_INDEX];
        ci.sComicGroups = sReturnData[COMIC_DETAILS_GROUPS_DATA_INDEX];
        ci.sComicLanguages = sReturnData[COMIC_DETAILS_LANGUAGES_DATA_INDEX];
        ci.sComicCategories = sReturnData[COMIC_DETAILS_CATEGORIES_DATA_INDEX];
        ci.iComicPages = Integer.parseInt(sReturnData[COMIC_DETAILS_PAGES_DATA_INDEX]);

        //Apply booleans to the intent to tell the receiver success, data available,
        //  and set the data where appropriate:

        broadcastIntent.putExtra(COMIC_DETAILS_SUCCESS, true);
        broadcastIntent.putExtra(COMIC_CATALOG_ITEM, ci);





        sendBroadcast(broadcastIntent);

    }

}
