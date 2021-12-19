package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


public class Service_WebPageTabs extends IntentService {

    private static final String ACTION_SET_WEBPAGE_TAB_DATA = "com.agcurations.webbrowser.action.set_webpage_tab_data";
    private static final String ACTION_GET_WEBPAGE_TAB_DATA = "com.agcurations.webbrowser.action.get_webpage_tab_data";
    private static final String ACTION_REMOVE_WEBPAGE_TAB_DATA = "com.agcurations.webbrowser.action.remove_webpage_tab_data";
    private static final String ACTION_GET_WEBPAGE_TITLE_FAVICON = "com.agcurations.webbrowser.action.get_webpage_title_favicon";

    private static final String EXTRA_WEBPAGE_TAB_DATA = "com.agcurations.webbrowser.extra.WEBPAGE_TAB_DATA";
    public static final String EXTRA_WEBPAGE_TAB_DATA_TABID = "com.agcurations.webbrowser.extra.WEBPAGE_TAB_DATA_TABID";

    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";

    public static final String EXTRA_RESULT_TYPE = "com.agcurations.webbrowser.extra.RESULT_TYPE";
    public static final String RESULT_TYPE_WEB_PAGE_TAB_DATA_ACQUIRED = "com.agcurations.webbrowser.result.WEB_PAGE_TAB_DATA_ACQUIRED";
    public static final String RESULT_TYPE_WEB_PAGE_TAB_CLOSED = "com.agcurations.webbrowser.result.WEB_PAGE_TAB_CLOSED";
    public static final String RESULT_TYPE_WEB_PAGE_TITLE_AND_FAVICON_ACQUIRED = "com.agcurations.webbrowser.result.WEB_PAGE_TITLE_AND_FAVICON_ACQUIRED";

    public static final String IMPORT_REQUEST_FROM_INTERNAL_BROWSER = "com.agcurations.aggallerymanager.importurl";



    public Service_WebPageTabs() {
        super("Service_WebPages");
    }


    public static void startAction_GetWebPageTabData(Context context) {
        OneTimeWorkRequest otwrGetWebPageTabData = new OneTimeWorkRequest.Builder(Worker_Browser_GetWebPageTabData.class)
                .build();
        WorkManager.getInstance(context).enqueue(otwrGetWebPageTabData);
    }

    public static void startAction_WriteWebPageTabData(Context context) {
        OneTimeWorkRequest otwrWriteWebPageTabData = new OneTimeWorkRequest.Builder(Worker_Browser_WriteWebPageTabData.class)
                .build();
        WorkManager.getInstance(context).enqueue(otwrWriteWebPageTabData);
    }

    public static void startAction_GetWebpageTitleFavicon(Context context, ItemClass_WebPageTabData itemClass_webPageTabData){
        Intent intent = new Intent(context, Service_WebPageTabs.class);
        intent.setAction(ACTION_GET_WEBPAGE_TITLE_FAVICON);
        intent.putExtra(EXTRA_WEBPAGE_TAB_DATA, itemClass_webPageTabData);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_GET_WEBPAGE_TITLE_FAVICON.equals(action)){
                final ItemClass_WebPageTabData itemClass_webPageTabData = (ItemClass_WebPageTabData) intent.getSerializableExtra(EXTRA_WEBPAGE_TAB_DATA);
                if(itemClass_webPageTabData == null) return;
                handleActionPreloadHTMLGetTitleFavicon(itemClass_webPageTabData);
            }
        }
    }


    private void handleActionSetWebPageTabData(ItemClass_WebPageTabData icwptd_DataToSet) {
        boolean b = false;
        if(b){
            return;
        }
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        //Make sure it is this routine's turn to update data:
        String thisHash = UUID.randomUUID().toString();
        GlobalClass.queueWebPageTabDataFileWriteRequests.add(thisHash);
        int iLoopSleepMS = 20;
        int iMaxLoops = GlobalClass.giMaxDelayForWriteRequestMS / iLoopSleepMS;
        int i = 0;
        while (!GlobalClass.queueWebPageTabDataFileWriteRequests.peek().equals(thisHash) && i < iMaxLoops){
            try {
                Thread.sleep(iLoopSleepMS);
            } catch (Exception e){

            }
            i++;
        }


        //Update memory:
        boolean bDataFound = false;
        for(i = 0; i < globalClass.gal_WebPages.size(); i++){
            if(globalClass.gal_WebPages.get(i).sTabID.equals(icwptd_DataToSet.sTabID)){
                globalClass.gal_WebPages.set(i, icwptd_DataToSet);
                bDataFound = true;
                break;
            }
        }
        if(!bDataFound){
            //This is a new record. Add it to the file:
            //This should not occur because should only occur on new Tab. New tab
            //  must create the data in memory to prevent a race condition with CreateFragment.
            //  I'm leaving it here just in case I am mistaken.
            globalClass.gal_WebPages.add(icwptd_DataToSet);
        }

        //Update the webpage tab data file:
        File fWebPageTabDataFile = globalClass.gfWebpageTabDataFile;
        if(fWebPageTabDataFile == null) return;

        try {

            StringBuilder sbBuffer = new StringBuilder();
            String sHeader = GlobalClass.getWebPageTabDataFileHeader(); //Get updated header.
            sbBuffer.append(sHeader);
            sbBuffer.append("\n");

            String sLine;
            for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages){
                sLine = GlobalClass.ConvertWebPageTabDataToString(icwptd) + "\n";
                sbBuffer.append(sLine);
            }

            //Write the data to the file:
            FileWriter fwNewWebPageStorageFile = new FileWriter(fWebPageTabDataFile, false);
            fwNewWebPageStorageFile.write(sbBuffer.toString());
            fwNewWebPageStorageFile.flush();
            fwNewWebPageStorageFile.close();

        } catch (Exception e) {
            problemNotificationConfig( e.getMessage(), Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        }

        GlobalClass.queueWebPageTabDataFileWriteRequests.remove();

    }


    private void handleActionRemoveWebPageTabData() {

    }

    private void handleActionPreloadHTMLGetTitleFavicon(ItemClass_WebPageTabData icwptd_DataToSet){

        //Get the html from the webpage:
        try {
            URL url = new URL(icwptd_DataToSet.sAddress);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String input;
            StringBuilder stringBuilder = new StringBuilder();
            while ((input = in.readLine()) != null) {
                stringBuilder.append(input);
            }
            in.close();
            String sHTML = stringBuilder.toString();

            //Process the HTML so that it can be parsed:

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
                node = pageParser.clean(sHTML);
            } catch (Exception e){
                String sMessage = e.getMessage();
                return;
            }
            //For acquiring clean html for use with xPathExpression testing tool at https://www.freeformatter.com/xpath-tester.html:
            String sCleanHTML= "<" + node.getName() + ">" + pageParser.getInnerHtml(node) + "</" + node.getName() + ">";

            //Attempt to locate the title and favicon address:

            String sXPathExpressionTitleLocator = "//title/text()";
            String sTitle = "";
            String sXPathExpressionFaviconLocator = "//link[@rel='icon']/@href";
            String sFaviconAddress = "";
            try {
                //Use an xPathExpression (similar to RegEx) to look for the title in the html/xml:
                Object[] objsTitleString = node.evaluateXPath(sXPathExpressionTitleLocator);
                //Check to see if we found anything:
                if (objsTitleString != null && objsTitleString.length > 0) {
                    //If we found something, assign it to a string:
                    for (Object oTitleString : objsTitleString) {
                        sTitle = oTitleString.toString();
                        if (!sTitle.equals("")) {
                            break;
                        }
                    }
                }
                icwptd_DataToSet.sTabTitle = sTitle;

                boolean bFaviconAddressFound = false;
                //Use an xPathExpression (similar to RegEx) to look for favicon in the html/xml:
                Object[] objsFaviconLink = node.evaluateXPath(sXPathExpressionFaviconLocator);
                //Check to see if we found anything:
                if (objsFaviconLink != null && objsFaviconLink.length > 0) {
                    //If we found something, assign it to a string:
                    for (Object oFaviconLink : objsFaviconLink) {
                        sFaviconAddress = oFaviconLink.toString();
                        if (!sFaviconAddress.equals("")) {
                            bFaviconAddressFound = true;
                            break;
                        }
                    }
                }

                String sAddress = icwptd_DataToSet.sAddress;
                if(bFaviconAddressFound) {
                    if (!sFaviconAddress.startsWith("http")) {

                        String sHostPrefix = sAddress.substring(0, sAddress.indexOf("/"));
                        String sHost = sHostPrefix + "//" + url.getHost();
                        if (sFaviconAddress.startsWith("/")) {
                            sFaviconAddress = sHost + sFaviconAddress;
                        } else {
                            sFaviconAddress = sHost + "/" + sFaviconAddress;
                        }
                    }
                } else {
                    //If the favicon was not found using the xPath expression searche, try looking
                    // for it at the host level:
                    String sHostPrefix = sAddress.substring(0,sAddress.indexOf("/"));
                    String sHost = sHostPrefix + "//" + url.getHost();
                    sFaviconAddress = sHost + "/favicon.ico";
                    //Check to see if the resource is found at the URL:
                    url = new URL(sFaviconAddress);
                    HttpURLConnection huc = (HttpURLConnection) url.openConnection();
                    int iResponseCode = huc.getResponseCode();
                    if(iResponseCode == HttpURLConnection.HTTP_OK){
                        bFaviconAddressFound = true;
                    }
                }
                if(bFaviconAddressFound) {
                    icwptd_DataToSet.sFaviconAddress = sFaviconAddress;
                }

            } catch (Exception e) {
                String sMessage = e.getMessage();
            }




        } catch (Exception e){
            String sMessage = e.getMessage();
            return;
        }

        handleActionSetWebPageTabData(icwptd_DataToSet);

        //Broadcast a message to be picked-up by the WebPage Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(EXTRA_WEBPAGE_TAB_DATA_TABID, icwptd_DataToSet.sTabID);
        broadcastIntent.putExtra(EXTRA_RESULT_TYPE, RESULT_TYPE_WEB_PAGE_TITLE_AND_FAVICON_ACQUIRED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }




//==================================================================================================
//=========  UTILITIES  ============================================================================
//==================================================================================================





    void problemNotificationConfig(String sMessage, String sIntentActionFilter){
        Intent broadcastIntent_Problem = new Intent();
        broadcastIntent_Problem.setAction(sIntentActionFilter);
        broadcastIntent_Problem.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent_Problem.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Problem.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Problem);
    }



}