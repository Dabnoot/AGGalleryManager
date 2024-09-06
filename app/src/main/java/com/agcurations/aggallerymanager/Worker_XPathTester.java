package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class Worker_XPathTester extends Worker {

    public static final String TAG_WORKER_XPATH_TESTER = "com.agcurations.aggallermanager.tag_worker_xpath_tester";

    public static final String WORKER_XPATH_TESTER_RESPONSE = "com.agcurations.aggallerymanager.intent.action.WORKER_XPATH_TESTER_RESPONSE";

    public static final String EXTRA_STRING_XPATH_EXPRESSION = "com.agcurations.aggallerymanager.extra.STRING_XPATH_EXPRESSION";

    private String gsXPathExpression = "";

    public Worker_XPathTester(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        gsXPathExpression = workerParams.getInputData().getString(EXTRA_STRING_XPATH_EXPRESSION);

    }


    @NonNull
    @Override
    public Result doWork() {

        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        if(!globalClass.WaitForObjectReady(GlobalClass.gabHTMLHolderAvailable, 2)){
            String sMessage;
            sMessage = "Memory not ready. Please retry.";
            globalClass.problemNotificationConfig(sMessage, WORKER_XPATH_TESTER_RESPONSE);
            Data data = new Data.Builder().putString("FAILURE_REASON", sMessage).build();
            return Result.failure(data);
        }
        GlobalClass.gabHTMLHolderAvailable.set(false);
        String sHTML = GlobalClass.sWebPageHTML;
        GlobalClass.sWebPageHTML = "";
        GlobalClass.gabHTMLHolderAvailable.set(true);


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
            node = pageParser.clean(sHTML);
        } catch (Exception e){
            String sMessage = "\nProblem with HTML parser. Try again?\n" + e.getMessage();
            globalClass.problemNotificationConfig(sMessage, WORKER_XPATH_TESTER_RESPONSE);
            Data data = new Data.Builder().putString("FAILURE_REASON", sMessage).build();
            return Result.failure(data);
        }
        //For acquiring clean html for use with xPathExpression testing tool at https://www.freeformatter.com/xpath-tester.html:
        String sCleanHTML= "<" + node.getName() + ">" + pageParser.getInnerHtml(node) + "</" + node.getName() + ">";

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(WORKER_XPATH_TESTER_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        BroadcastProgress_LogOutput("\n[EVALUATING XPATH EXPRESSION]");
        String sxPathExpression;
        sxPathExpression = "//a[@class='link-pri link-hover']";
        try {
            //Use an xPathExpression (similar to RegEx) to look for the comic title in the html/xml:
            Object[] objsTagNodeTitle = node.evaluateXPath(gsXPathExpression);
            //Check to see if we found anything:
            StringBuilder sbData = new StringBuilder();
            String sTemp;
            if (objsTagNodeTitle != null && objsTagNodeTitle.length > 0) {
                //If we found something, assign it to a string:
                for(int i = 0; i < objsTagNodeTitle.length; i++){
                    sTemp = "[Unknown Object Type]";
                    if(objsTagNodeTitle[i].getClass().equals(String.class)){
                        sTemp = (String) objsTagNodeTitle[i];
                    } else if (objsTagNodeTitle[i].getClass().equals(TagNode.class)) {
                        sTemp = ((TagNode) objsTagNodeTitle[i]).getText().toString();
                    }
                    sTemp = GlobalClass.cleanHTMLCodedCharacters(sTemp); //This will clear out '\n', so do this now rather than later.
                    sbData.append(sTemp);
                    if(i < objsTagNodeTitle.length - 1){
                        sbData.append("\n");
                    }
                }
            }
            BroadcastProgress_LogOutput("\n[DATA RESULTS]\n" + sbData);

        } catch (Exception e) {
            String sMessage = "\nProblem collecting comic data from address. " + e.getMessage();
            BroadcastProgress_LogOutput(sMessage);
            broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
            broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
            //sendBroadcast(broadcastIntent);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

            Data data = new Data.Builder().putString("FAILURE_REASON", sMessage).build();
            return Result.failure(data);
        }
        BroadcastProgress_LogOutput("\n[ANALYSIS COMPLETE]");
        return Result.success();
    }

    public void BroadcastProgress_LogOutput(String sLogLine){
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        globalClass.BroadcastProgress(true, sLogLine,
                false, 0,
                false, "",
                WORKER_XPATH_TESTER_RESPONSE);
    }
}
