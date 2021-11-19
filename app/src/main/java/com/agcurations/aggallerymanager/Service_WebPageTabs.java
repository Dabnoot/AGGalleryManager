package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class Service_WebPageTabs extends IntentService {

    private static final String ACTION_SET_WEBPAGE_TAB_DATA = "com.agcurations.webbrowsertest.action.set_webpage_tab_data";
    private static final String ACTION_GET_WEBPAGE_TAB_DATA = "com.agcurations.webbrowsertest.action.get_webpage_tab_data";
    private static final String ACTION_REMOVE_WEBPAGE_TAB_DATA = "com.agcurations.webbrowsertest.action.remove_webpage_tab_data";
    private static final String ACTION_DELETE_FAVICON_FILE = "com.agcurations.webbrowsertest.action.delete_favicon_file";

    private static final String EXTRA_WEBPAGE_TAB_DATA = "com.agcurations.webbrowsertest.extra.WEBPAGE_TAB_DATA";
    private static final String EXTRA_FAVICON_FILENAME = "com.agcurations.webbrowsertest.extra.FAVICON_FILENAME";

    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";

    public static final String EXTRA_RESULT_TYPE = "com.agcurations.webbrowsertest.extra.RESULT_TYPE";
    public static final String RESULT_TYPE_WEB_PAGE_TAB_DATA_ACQUIRED = "com.agcurations.webbrowsertest.result.WEB_PAGE_TAB_DATA_ACQUIRED";
    public static final String RESULT_TYPE_WEB_PAGE_TAB_CLOSED = "com.agcurations.webbrowsertest.result.WEB_PAGE_TAB_CLOSED";

    public static final String IMPORT_REQUEST_FROM_INTERNAL_BROWSER = "com.agcurations.aggallerymanager.importurl";
    public static final String OPEN_NEW_TAB_REQUEST = "com.agcurations.aggallerymanager.OPEN_NEW_TAB_REQUEST";

    public Service_WebPageTabs() {
        super("Service_WebPages");
    }


    public static void startAction_SetWebPageTabData(Context context, ItemClass_WebPageTabData itemClass_webPageTabData) {
        Intent intent = new Intent(context, Service_WebPageTabs.class);
        intent.setAction(ACTION_SET_WEBPAGE_TAB_DATA);
        intent.putExtra(EXTRA_WEBPAGE_TAB_DATA, itemClass_webPageTabData);
        context.startService(intent);
    }

    public static void startAction_GetWebPageTabData(Context context) {
        Intent intent = new Intent(context, Service_WebPageTabs.class);
        intent.setAction(ACTION_GET_WEBPAGE_TAB_DATA);
        context.startService(intent);
    }

    public static void startAction_RemoveWebPageTabData(Context context) {
        Intent intent = new Intent(context, Service_WebPageTabs.class);
        intent.setAction(ACTION_REMOVE_WEBPAGE_TAB_DATA);
        context.startService(intent);
    }

    public static void startAction_DeleteFaviconFile(Context context, String sFaviconFilename) {
        Intent intent = new Intent(context, Service_WebPageTabs.class);
        intent.setAction(ACTION_DELETE_FAVICON_FILE);
        intent.putExtra(EXTRA_FAVICON_FILENAME, sFaviconFilename);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SET_WEBPAGE_TAB_DATA.equals(action)) {
                final ItemClass_WebPageTabData itemClass_webPageTabData = (ItemClass_WebPageTabData) intent.getSerializableExtra(EXTRA_WEBPAGE_TAB_DATA);
                if(itemClass_webPageTabData == null) return;
                handleActionSetWebPageTabData(itemClass_webPageTabData);
            } else if (ACTION_GET_WEBPAGE_TAB_DATA.equals(action)) {
                handleActionGetWebPageTabData();
            } else if (ACTION_REMOVE_WEBPAGE_TAB_DATA.equals(action)) {
                handleActionRemoveWebPageTabData();
            } else if (ACTION_DELETE_FAVICON_FILE.equals(action)) {
                final String sFaviconFilename = intent.getStringExtra(EXTRA_FAVICON_FILENAME);
                if(sFaviconFilename == null) return;
                handleActionDeleteFaviconFile(sFaviconFilename);
            }
        }
    }


    private void handleActionSetWebPageTabData(ItemClass_WebPageTabData icwptd_DataToSet) {
        boolean b = false;
        if(b){
            return;
        }
        //Update memory:
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        boolean bDataFound = false;
        for(int i = 0; i < globalClass.gal_WebPages.size(); i++){
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
            String sHeader = getCatalogHeader(); //Get updated header.
            sbBuffer.append(sHeader);
            sbBuffer.append("\n");

            String sLine;
            for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages){
                sLine = ConvertWebPageTabDataToString(icwptd) + "\n";
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

    }


    private void handleActionGetWebPageTabData() {

        //Update memory:
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        //Update the webpage tab data file:
        File fWebPageTabDataFile = globalClass.gfWebpageTabDataFile;
        if(fWebPageTabDataFile == null) return;
        if(!fWebPageTabDataFile.exists()) return;


        boolean bTestingCloseOfTabs = false;
        if(bTestingCloseOfTabs){
            boolean bFormReferenceTabFile = false;
            File fReferenceFile = new File(globalClass.gfBrowserDataFolder.getPath() + File.separator + "WebPageTabDataRef.dat");
            if(bFormReferenceTabFile){
                //Create a reference tab file:
                try {
                    Files.copy(fWebPageTabDataFile.toPath(), fReferenceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e){
                    String sMessage = e.getMessage();
                }
            }
            //Copy the reference file of open tabs so that I don't have to keep opening them.
            try {
                Files.copy(fReferenceFile.toPath(), fWebPageTabDataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e){
                String sMessage = e.getMessage();
            }

        }




        try {

            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fWebPageTabDataFile.getAbsolutePath()));

            brReader.readLine(); //Skip read of the file header.

            if(globalClass.gal_WebPages == null){
                globalClass.gal_WebPages = new ArrayList<>();
            } else {
                globalClass.gal_WebPages.clear();
            }

            String sLine = brReader.readLine();
            while (sLine != null) {

                ItemClass_WebPageTabData icwptd_DataRecordFromFile;
                icwptd_DataRecordFromFile = ConvertStringToWebPageTabData(sLine);
                globalClass.gal_WebPages.add(icwptd_DataRecordFromFile);

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();


        } catch (Exception e) {
            problemNotificationConfig( "Problem reading tab records from file: " + e.getMessage(), Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
            if(fWebPageTabDataFile.exists()){
                fWebPageTabDataFile.delete();
            }
        }




        //Broadcast a message to be picked-up by the WebPage Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(EXTRA_RESULT_TYPE, RESULT_TYPE_WEB_PAGE_TAB_DATA_ACQUIRED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);



    }

    private void handleActionRemoveWebPageTabData() {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();

        //Update the webpage tab data file:
        File fWebPageTabDataFile = globalClass.gfWebpageTabDataFile;
        if(fWebPageTabDataFile == null) return;

        //Re-write the data file completely because all of the indexes have changed:
        try {

            StringBuilder sbBuffer = new StringBuilder();

            String sHeader = getCatalogHeader(); //Get updated header.
            sbBuffer.append(sHeader);
            sbBuffer.append("\n");

            for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages){
                String sLine = ConvertWebPageTabDataToString(icwptd);
                sbBuffer.append(sLine);
                sbBuffer.append("\n");
            }

            //Write the data to the file:
            FileWriter fwNewWebPageStorageFile = new FileWriter(fWebPageTabDataFile, false);
            fwNewWebPageStorageFile.write(sbBuffer.toString());
            fwNewWebPageStorageFile.flush();
            fwNewWebPageStorageFile.close();

        } catch (Exception e) {
            problemNotificationConfig( e.getMessage(), Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        }

        //Broadcast a message to be picked-up by the WebPage Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_Browser.WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(EXTRA_RESULT_TYPE, RESULT_TYPE_WEB_PAGE_TAB_CLOSED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }

    private void handleActionDeleteFaviconFile(String sFaviconFilename) {
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        String sFilePath = globalClass.gfWebpageFaviconBitmapFolder.getPath() + File.separator + sFaviconFilename;
        File fFaviconFile = new File(sFilePath);
        if(fFaviconFile.exists()){
            if(!fFaviconFile.delete()){
                //do nothing
                //todo.
            }
        }

    }


//==================================================================================================
//=========  UTILITIES  ============================================================================
//==================================================================================================



    public final int giWebPageTabDataFileVersion = 1;
    public String getCatalogHeader(){
        String sHeader = "";
        sHeader = sHeader + "ID";                       //Tab ID (unique).
        sHeader = sHeader + "\t" + "Index";             //Tab index in the tab layout.
        sHeader = sHeader + "\t" + "Title";             //Tab title (don't reload the page to get the title).
        sHeader = sHeader + "\t" + "AddressHistory";    //Address history for the tab.
        sHeader = sHeader + "\t" + "Favicon Filename";  //Filename of bitmap for tab icon.
        sHeader = sHeader + "\t" + "Version:" + giWebPageTabDataFileVersion;

        return sHeader;
    }

    public String ConvertWebPageTabDataToString(ItemClass_WebPageTabData wptd){

        String sRecord = "";  //To be used when writing the catalog file.
        sRecord = sRecord + GlobalClass.JumbleStorageText(wptd.sTabID);
        sRecord = sRecord + "\t" + wptd.iTabIndex;
        sRecord = sRecord + "\t" + GlobalClass.JumbleStorageText(wptd.sTabTitle);
        /*sRecord = sRecord + "\t" + "{";
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < wptd.sAddress.size(); i++){
            sb.append(GlobalClass.JumbleStorageText(wptd.sAddress.get(i)));
            if(i < (wptd.sAddress.size() - 1)){
                sb.append("%%"); //A double-percent is a symbol not allowed in a web address.
            }
        }
        sRecord = sRecord + sb.toString() + "%%" + "}";*/
        sRecord = sRecord + "\t" + GlobalClass.JumbleStorageText(wptd.sAddress);
        sRecord = sRecord + "\t" + wptd.sFaviconFilename;

        return sRecord;
    }

    public static ItemClass_WebPageTabData ConvertStringToWebPageTabData(String[] sRecord){
        //Designed for interpreting a line as read from the WebPageTabData file.
        ItemClass_WebPageTabData wptd =  new ItemClass_WebPageTabData();
        wptd.sTabID = GlobalClass.JumbleStorageText(sRecord[0]);
        wptd.iTabIndex = Integer.parseInt(sRecord[1]);
        wptd.sTabTitle = GlobalClass.JumbleStorageText(sRecord[2]);
        /*wptd.sAddress = new ArrayList<>();
        String sAddresses = sRecord[3];
        sAddresses = sAddresses.substring(1, sAddresses.length() - 1); //Remove '{' and '}'.
        String[] sAddressHistory = sAddresses.split("%%");
        for(int i = 0; i < sAddressHistory.length; i++){
            sAddressHistory[i] = GlobalClass.JumbleStorageText(sAddressHistory[i]);
        }
        wptd.sAddress.addAll(Arrays.asList(sAddressHistory));*/
        wptd.sAddress = GlobalClass.JumbleStorageText(sRecord[3]);


        if(sRecord.length >= 5) {
            //Favicon filename might be empty, and if it is the last item on the record,
            //  it will not be split-out via the split operation.
            wptd.sFaviconFilename = sRecord[4];
        }

        return wptd;
    }

    public static ItemClass_WebPageTabData ConvertStringToWebPageTabData(String sRecord){
        String[] sRecord2 =  sRecord.split("\t");
        return ConvertStringToWebPageTabData(sRecord2);
    }

    void problemNotificationConfig(String sMessage, String sIntentActionFilter){
        Intent broadcastIntent_Problem = new Intent();
        broadcastIntent_Problem.setAction(sIntentActionFilter);
        broadcastIntent_Problem.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent_Problem.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Problem.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Problem);
    }



}