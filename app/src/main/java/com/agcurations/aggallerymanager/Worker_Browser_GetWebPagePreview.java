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

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Browser_GetWebPagePreview extends Worker {

    public static final String TAG_WORKER_BROWSER_GETWEBPAGEPREVIEW = "com.agcurations.aggallermanager.tag_worker_browser_getwebpagepreview";
    public static final String RESULT_TYPE_WEB_PAGE_TITLE_AND_FAVICON_ACQUIRED = "com.agcurations.webbrowser.result.WEB_PAGE_TITLE_AND_FAVICON_ACQUIRED";


    private String gsTabID = "";
    private String gsAddress = "";

    Double gdCallerTimeStamp;
    public Worker_Browser_GetWebPagePreview(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        gdCallerTimeStamp = getInputData().getDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, -1);
        gsTabID = getInputData().getString(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_TABID);
        gsAddress = getInputData().getString(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_ADDRESS);
    }

    @NonNull
    @Override
    public Result doWork() {

        //If caller timestamp is old, the Android system may have tried to restart the worker.
        //If so, finish this worker as failed and abandon.
        Double dCurrentTimeStamp = GlobalClass.GetTimeStampDouble(); //yyyyMMdd.HHmmss
        double dTimeDiff = dCurrentTimeStamp - gdCallerTimeStamp;
        double dOneSecond = 0.000001;
        double dTimeOut = 5.0 * dOneSecond;
        if(gdCallerTimeStamp < 0 ||
                dTimeDiff > dTimeOut){
            return Result.failure();
        }

        String sTitle = "";
        String sFaviconAddress = "";
        boolean bFaviconAddressFound = false;

        //Get the html from the webpage:
        try {
            URL url = new URL(gsAddress);
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
                return Result.failure();
            }
            //For acquiring clean html for use with xPathExpression testing tool at https://www.freeformatter.com/xpath-tester.html:
            String sCleanHTML= "<" + node.getName() + ">" + pageParser.getInnerHtml(node) + "</" + node.getName() + ">";

            //Attempt to locate the title and favicon address:

            String sXPathExpressionTitleLocator = "//title/text()";
            String sXPathExpressionFaviconLocator = "//link[@rel='icon']/@href";

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

                String sAddress = gsAddress;
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

            } catch (Exception e) {
                String sMessage = e.getMessage();
            }




        } catch (Exception e){
            String sMessage = e.getMessage();
            return Result.failure();
        }

        if(bFaviconAddressFound) {
            //Broadcast a message to be picked-up by the WebPage Activity:
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            broadcastIntent.putExtra(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_TABID, gsTabID);
            broadcastIntent.putExtra(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_TITLE, sTitle);
            broadcastIntent.putExtra(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_FAVICON_ADDRESS, sFaviconAddress);
            broadcastIntent.putExtra(GlobalClass.EXTRA_RESULT_TYPE, RESULT_TYPE_WEB_PAGE_TITLE_AND_FAVICON_ACQUIRED);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        }

        return Result.success();
    }





}
