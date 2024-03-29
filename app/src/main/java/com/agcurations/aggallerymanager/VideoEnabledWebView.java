package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


/**
 * <a href="https://github.com/cprcrack/VideoEnabledWebView">...</a>
 * This class serves as a WebView to be used in conjunction with a VideoEnabledWebChromeClient.
 * It makes possible:
 * - To detect the HTML5 video ended event so that the VideoEnabledWebChromeClient can exit full-screen.
 *
 * Important notes:
 * - Javascript is enabled by default and must not be disabled with getSettings().setJavaScriptEnabled(false).
 * - setWebChromeClient() must be called before any loadData(), loadDataWithBaseURL() or loadUrl() method.
 *
 * @author Cristian Perez (<a href="http://cpr.name">...</a>)
 *
 */
public class VideoEnabledWebView extends WebView
{
    WebView webView;
    Context gcContext;

    boolean gbEnableNewTabLinkage = false;

    public class JavascriptInterface
    {
        @android.webkit.JavascriptInterface @SuppressWarnings("unused")
        public void notifyVideoEnd() // Must match Javascript interface method of VideoEnabledWebChromeClient
        {
            Log.d("___", "GOT IT");
            // This code is not executed in the UI thread, so we must force that to happen
            new Handler(Looper.getMainLooper()).post(new Runnable()
            {
                @Override
                public void run()
                {
                    if (videoEnabledWebChromeClient != null)
                    {
                        videoEnabledWebChromeClient.onHideCustomView();
                    }
                }
            });
        }
    }

    private VideoEnabledWebChromeClient videoEnabledWebChromeClient;
    private boolean addedJavascriptInterface;

    @SuppressWarnings("unused")
    public VideoEnabledWebView(Context context)
    {
        super(context);
        addedJavascriptInterface = false;
        webView = this;
        gcContext = context;
    }

    @SuppressWarnings("unused")
    public VideoEnabledWebView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        addedJavascriptInterface = false;
        webView = this;
        gcContext = context; //Context is the hosting activity.
    }

    @SuppressWarnings("unused")
    public VideoEnabledWebView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        addedJavascriptInterface = false;
        webView = this;
        gcContext = context;
    }

    /**
     * Indicates if the video is being displayed using a custom view (typically full-screen)
     * @return true it the video is being displayed using a custom view (typically full-screen)
     */
    @SuppressWarnings("unused")
    public boolean isVideoFullscreen()
    {
        return videoEnabledWebChromeClient != null && videoEnabledWebChromeClient.isVideoFullscreen();
    }

    /**
     * Pass only a VideoEnabledWebChromeClient instance.
     */
    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public void setWebChromeClient(WebChromeClient client)
    {
        getSettings().setJavaScriptEnabled(true);

        if (client instanceof VideoEnabledWebChromeClient)
        {
            this.videoEnabledWebChromeClient = (VideoEnabledWebChromeClient) client;
        }

        super.setWebChromeClient(client);
    }

    @Override
    public void loadData(String data, String mimeType, String encoding)
    {
        addJavascriptInterface();
        super.loadData(data, mimeType, encoding);
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl)
    {
        addJavascriptInterface();
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    @Override
    public void loadUrl(String url)
    {
        addJavascriptInterface();
        super.loadUrl(url);
    }

    @Override
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders)
    {
        addJavascriptInterface();
        super.loadUrl(url, additionalHttpHeaders);
    }

    private void addJavascriptInterface()
    {
        if (!addedJavascriptInterface)
        {
            // Add javascript interface to be called when the video ends (must be done before page load)
            //noinspection all
            addJavascriptInterface(new JavascriptInterface(), "_VideoEnabledWebView"); // Must match Javascript interface name of VideoEnabledWebChromeClient

            addedJavascriptInterface = true;
        }
    }


    public static String gsNodeData_src;
    public static String gsNodeData_title;
    public static String gsNodeData_url;

    double dLastx;
    double dLasty;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //The webview must have an html node selected in order for us to get the node data.
        //  The touch even occurs before a node is selected, and the same for the long-press
        //  event. So we must ensure that the user has committed ACTION_DOWN in the same area
        //  at least twice before we get the node data and allow a context menu to appear related
        //  to that data.
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            double dDeltaX = Math.abs(event.getX() - dLastx);
            double dDeltaY = Math.abs(event.getY() - dLasty);
            double dTranslation = Math.sqrt(dDeltaX*dDeltaX + dDeltaY*dDeltaY);
            if(dTranslation > 50) {
                //If we are far away from the last selected location, we must be selecting a new
                //  node. Clear the node data so that we do not allow a context menu to appear.
                //  This ACTION_DOWN will select a node item and we will call to get the data on
                //  the next touch action.
                //  I had to create a flow chart to understand how to create this logic.
                gsNodeData_src = null;
                gsNodeData_title = null;
                gsNodeData_url = null;
                dLastx = event.getX();
                dLasty = event.getY();
            } else {
                //The user has committed ACTION_DOWN near a previously-touched location.
                //  Initiate call to get data related to a selected node.
                HREFHandler hrefHandler = new HREFHandler();
                Message msg = hrefHandler.obtainMessage();
                this.requestFocusNodeHref(msg); //It will take a moment for the msg to get processed.
            }
        }

        return super.onTouchEvent(event);
    }



    static class HREFHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            gsNodeData_src = msg.getData().getString("src");
            gsNodeData_title = msg.getData().getString("title");
            gsNodeData_url = msg.getData().getString("url");
        }
    }



    final int ID_OPEN_LINK_NEW_TAB = 7421; //Numbers are arbitrary.
    final int ID_COPY_LINK_ADDRESS = 7422;
    final int ID_COPY_LINK_TEXT = 7423;
    final int ID_DOWNLOAD_IMAGE = 7424;

    public String gsTabID = "";

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        super.onCreateContextMenu(menu);

        if((gsNodeData_src == null) && (gsNodeData_url == null)){
            return;
        }

        final HitTestResult result = getHitTestResult();


        //todo: Create your own dialog menu so that you can change the colors.
        //https://stackoverflow.com/questions/38872585/how-to-specify-the-background-color-of-the-context-menu



        MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                // do the menu action
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData;
                String sURL;
                switch (item.getItemId()){
                    case ID_OPEN_LINK_NEW_TAB:
                        Message msg = new Message();
                        Bundle bundle = new Bundle();
                        sURL = gsNodeData_url;
                        if (sURL == null) {
                            sURL = gsNodeData_src;
                        }
                        bundle.putString("url", sURL);
                        bundle.putString("tabID", gsTabID);
                        msg.setData(bundle);
                        OpenLinkInNewTabHandler.dispatchMessage(msg);
                        break;
                    case ID_COPY_LINK_ADDRESS:
                        sURL = gsNodeData_url;
                        if(sURL == null){
                            sURL = gsNodeData_src;
                        }
                        clipData = ClipData.newPlainText("", sURL);
                        clipboard.setPrimaryClip(clipData);
                        break;
                    case ID_COPY_LINK_TEXT:
                        clipData = ClipData.newPlainText("", gsNodeData_title);
                        clipboard.setPrimaryClip(clipData);
                        break;
                    case ID_DOWNLOAD_IMAGE:
                        //Use the download manager to download the file:
                        if(gsNodeData_src == null){
                            Toast.makeText(getContext(), "Problem identifying download. Try again.", Toast.LENGTH_SHORT).show();
                        }
                        Toast.makeText(getContext(), "Processing download.", Toast.LENGTH_SHORT).show();

                        Context cApplicationContext = gcContext.getApplicationContext();

                        String sFileNameRaw = gsNodeData_src;
                        if(sFileNameRaw.contains("/")){
                            sFileNameRaw = gsNodeData_src.substring(gsNodeData_src.lastIndexOf("/") + 1);
                        }
                        String sFileName = GlobalClass.cleanFileNameViaTrim(sFileNameRaw);

                        //Create a destination Uri for the file to be downloaded to, ensuring that the
                        //  file name is unique:
                        try {
                            sFileName = URLDecoder.decode(sFileName, StandardCharsets.UTF_8.toString());
                        } catch (Exception e){
                            Log.d("VideoEnabledWebView: onMenuItemClick()", "Trouble with URL Decoder.");
                        }
                        String sOriginalFileName = sFileName;
                        if(sFileName.length() > GlobalClass.AGGM_MAX_FILENAME_LENGTH){
                            //Limit max length of file name or download manager will do it for you.
                            String sFileBaseName = sFileName.substring(0, sFileName.lastIndexOf("."));
                            String sFileExtension = sFileName.substring(sFileName.lastIndexOf("."));
                            if(sFileExtension.length() > 5){
                                Log.d("VideoEnabledWebView: onMenuItemClick()", "File extension wierd. Not processing due to uncaptured case.");
                                return true;
                            }
                            int iAmountToTrim = sFileName.length() - GlobalClass.AGGM_MAX_FILENAME_LENGTH;
                            sFileBaseName = sFileBaseName.substring(0, sFileBaseName.length() - iAmountToTrim);
                            sFileBaseName = sFileBaseName.trim();
                            sFileName = sFileBaseName + sFileExtension;


                        }
                        //Two locations must be checked to confirm that the filename is unique. The file's final
                        //  destination is the Holding folder. The file's intermediate destination is
                        //  the App-internal storage directory, as an app can only download files to their
                        //  own storage location. This app will move the file from HoldingTemp to Holding
                        //  to prevent Android DL cleanup from removing "unused files". Therefore, the
                        //  file name must be unique to both locations.

                        String sFileNameCandidate = sFileName; //FileName is non-jumbled at this point.
                        String sMetadataFileName = "";

                        boolean bFileNamesAreUnique = false;
                        int iMaxIterations = 10000;
                        int iIteration = 0;
                        while(!bFileNamesAreUnique) {
                            iIteration++;
                            sFileNameCandidate = GlobalClass.getUniqueFileName(GlobalClass.gUriImageDownloadHoldingFolder, sFileNameCandidate, true);
                            sFileNameCandidate = GlobalClass.getUniqueFileNameAppInternalTempStorage(GlobalClass.gfImageDownloadHoldingFolderTemp, sFileNameCandidate, false);

                            //The metadata filename must also be available and have a base name of the media file:
                            sMetadataFileName = sFileNameCandidate + ".tad";  //.dat extension but jumbled.
                            sMetadataFileName = GlobalClass.getUniqueFileName(GlobalClass.gUriImageDownloadHoldingFolder, sMetadataFileName, false);
                            sMetadataFileName = GlobalClass.getUniqueFileNameAppInternalTempStorage(GlobalClass.gfImageDownloadHoldingFolderTemp, sMetadataFileName, false);

                            String sMetadataFileNameCompareString = sMetadataFileName.substring(0, sMetadataFileName.lastIndexOf("."));
                            if(sMetadataFileNameCompareString.equals(sFileNameCandidate)){
                                bFileNamesAreUnique = true;
                            } else {
                                sFileNameCandidate = sFileName + "_" + iIteration;
                            }
                            if(iIteration > iMaxIterations){
                                String sMessage = "Too many files of the same name in holding folder or holding folder temporary storage.";
                                Toast.makeText(getContext(), sMessage, Toast.LENGTH_SHORT).show();
                                return true;
                            }

                        }
                        sFileName = sFileNameCandidate;

                        String sDownloadFolderRelativePath = GlobalClass.gsImageDownloadHoldingFolderTempRPath; //Android will DL to internal storage only.
                        String sDownloadManagerDownloadFolder;
                        File fExternalFilesDir = cApplicationContext.getExternalFilesDir(null);
                        if(fExternalFilesDir != null) {
                            sDownloadManagerDownloadFolder = fExternalFilesDir.getAbsolutePath() +
                                    sDownloadFolderRelativePath;
                        } else {
                            String sMessage = "Could not identify external files dir.";
                            Toast.makeText(getContext(), sMessage, Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(gsNodeData_src));
                        //request.addRequestHeader("User-Agent", GlobalClass.giWebViewSettings_UserAgentString);
                        //request.addRequestHeader("cookie", GlobalClass.gsCookie);
                        request.setTitle("AGGallery+ Download Single Image")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) //Make download notifications disappear when completed.
                                //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                                //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                                .setMimeType("application/octet-stream")
                                .setDestinationInExternalFilesDir(cApplicationContext, sDownloadFolderRelativePath, sFileName);

                        DownloadManager downloadManager = (DownloadManager) cApplicationContext.getSystemService(Context.DOWNLOAD_SERVICE);
                        long lDownloadID = downloadManager.enqueue(request);
                        long[] lDownloadIDs = new long[1];
                        lDownloadIDs[0] = lDownloadID;

                        //Call a worker to monitor the download and move the file into the holding folder out of the DM's reach:
                        String sCallerID = "VideoEnabledWebView:onCreateContextMenu().MenuItem.OnMenuItemClickListener";
                        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                        Data dataDownloadPostProcessor = new Data.Builder()
                                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                                .putString(Worker_DownloadPostProcessing.KEY_ARG_PATH_TO_MONITOR_FOR_DOWNLOADS, sDownloadManagerDownloadFolder)
                                .putString(Worker_DownloadPostProcessing.KEY_ARG_RELATIVE_PATH_TO_FOLDER, "Holding") //Holding folder name.
                                .putInt(Worker_DownloadPostProcessing.KEY_ARG_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_IMAGES)
                                .putLongArray(Worker_DownloadPostProcessing.KEY_ARG_DOWNLOAD_IDS, lDownloadIDs)
                                .build();
                        OneTimeWorkRequest otwrDownloadPostProcessor = new OneTimeWorkRequest.Builder(Worker_DownloadPostProcessing.class)
                                .setInputData(dataDownloadPostProcessor)
                                .addTag(Worker_DownloadPostProcessing.WORKER_TAG_DOWNLOAD_POST_PROCESSING) //To allow finding the worker later.
                                .build();
                        WorkManager.getInstance(getContext()).enqueue(otwrDownloadPostProcessor);

                        //Write a text file of the same file name to record details of the origin of the file. This
                        //  text data file is to be used during the import process to add a bit of metadata.

                        Uri uriImageMetadataFile;
                        try {

                            uriImageMetadataFile = DocumentsContract.createDocument(
                                    GlobalClass.gcrContentResolver,
                                    GlobalClass.gUriImageDownloadHoldingFolder,
                                    GlobalClass.BASE_TYPE_TEXT, sMetadataFileName);
                        } catch (Exception e) {
                            Log.d("MenuItemClick", "Could not create metadata file for downloaded item.");
                            return true;
                        }
                        if(uriImageMetadataFile == null){
                            Log.d("MenuItemClick", "Could not create metadata file for downloaded item.");
                            return true;
                        }
                        try {
                            //Metadata file is read in routine Worker_Import_GetHoldingFolderDirectoryContents.java.
                            OutputStream osImageMetadataFile = GlobalClass.gcrContentResolver.openOutputStream(uriImageMetadataFile, "wt");
                            if(osImageMetadataFile == null){
                                Log.d("MenuItemClick", "Could not write metadata file for downloaded item.");
                                return true;
                            }
                            String sWebPageURL = webView.getUrl();
                            if(sWebPageURL == null){
                                Log.d("MenuItemClick", "No metadata to write to file for downloaded item.");
                                return true;
                            }

                            //...........
                            //Clean Metadata before attempting to write it to a file:
                            boolean bIllegalString = false;
                            boolean bIllegalDataFound = false;

                            //Line up a field name (for messaging to the user), and the field value.
                            String[][] sFieldsAndData = {
                                    {"WebPageURL"		    	,sWebPageURL			},
                                    {"OriginalFileName"		    ,sOriginalFileName	    }};

                            //Create a stringbuilder to provide a message to the user if illegal characters are found:
                            StringBuilder sbDataIssueNarrative = new StringBuilder();

                            //Search the fields for illegal characters, and if they are found, replace the illegal
                            //  characters and prepare a message for the user:
                            //Go through all of the "illegal" strings/characters:
                            for(String[] sIllegalStringSet: GlobalClass.gsIllegalRecordStrings) {
                                //Go through all of the data fields:
                                for(int i = 0; i < sFieldsAndData.length; i++){
                                    bIllegalString = sFieldsAndData[i][GlobalClass.CHECKABLE].contains(sIllegalStringSet[GlobalClass.CHECKABLE]);
                                    if(bIllegalString) {
                                        bIllegalDataFound = true;
                                        sbDataIssueNarrative.append("Illegal string sequence ").append(sIllegalStringSet[GlobalClass.PRINTABLE]).append(" found in ").append(sFieldsAndData[i][GlobalClass.PRINTABLE]).append(" field.\n");
                                        sbDataIssueNarrative.append("Original data: ").append(sFieldsAndData[i][GlobalClass.CHECKABLE]).append("\n");
                                        sFieldsAndData[i][GlobalClass.CHECKABLE] = sFieldsAndData[i][GlobalClass.CHECKABLE].replace(sIllegalStringSet[GlobalClass.CHECKABLE],"");
                                        sbDataIssueNarrative.append("Modified data: ").append(sFieldsAndData[i][GlobalClass.CHECKABLE]).append("\n\n");
                                        switch (i){
                                            case 0: sWebPageURL             = sFieldsAndData[i][1]; break;
                                            case 1: sOriginalFileName       = sFieldsAndData[i][1]; break;
                                        }
                                    }
                                }
                            }

                            if(bIllegalDataFound){
                                //The illegal data should have been corrected by the validation routine. Notify the user:
                                Toast.makeText(getContext(), sbDataIssueNarrative.toString(), Toast.LENGTH_LONG).show();
                            }
                            //Data for file-write should now be devoid of any characters that would corrupt the file.
                            //...........

                            String sb = sWebPageURL + "\n" +
                                    sOriginalFileName + "\n" +
                                    GlobalClass.gicuCurrentUser.sUserName + "\n";
                            osImageMetadataFile.write(sb.getBytes(StandardCharsets.UTF_8));

                            osImageMetadataFile.flush();
                            osImageMetadataFile.close();
                        } catch (Exception e) {
                            String sMessage = "" + e.getMessage();
                            Log.d("VideoEnabledWebView", sMessage);
                        }



                        break;


                    default:
                        //Do nothing at this time.
                }
                return true;
            }




        };



        String sTitle = gsNodeData_title;
        if( sTitle == null){
            sTitle = gsNodeData_url;
        }
        if( sTitle == null){
            sTitle = gsNodeData_src;
        }
        menu.setHeaderTitle(sTitle);

        if(gbEnableNewTabLinkage) {
            //If this is configured in such a way to allow the user to open new tabs...
            if (result.getType() == HitTestResult.IMAGE_TYPE ||
                    result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                // Menu options for an image.
                menu.add(0, ID_OPEN_LINK_NEW_TAB, 0, "Open in new tab").setOnMenuItemClickListener(handler);
                menu.add(0, ID_COPY_LINK_ADDRESS, 0, "Copy link address").setOnMenuItemClickListener(handler);
                menu.add(0, ID_DOWNLOAD_IMAGE, 0, "Download image to holding folder (visit Import to complete)").setOnMenuItemClickListener(handler);

            } else if (result.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
                // Menu options for a hyperlink.
                menu.add(0, ID_OPEN_LINK_NEW_TAB, 0, "Open in new tab").setOnMenuItemClickListener(handler);
                menu.add(0, ID_COPY_LINK_ADDRESS, 0, "Copy link address").setOnMenuItemClickListener(handler);
                if (gsNodeData_title != null) {
                    menu.add(0, ID_COPY_LINK_TEXT, 0, "Copy link text").setOnMenuItemClickListener(handler);
                }
            }
        } else {
            //If this video-enabled web view is not implemented in a manner to allow new tabs,
            //  don't give the option to 'Open link in new tab.'
            if (result.getType() == HitTestResult.IMAGE_TYPE ||
                    result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                // Menu options for an image.
                menu.add(0, ID_COPY_LINK_ADDRESS, 0, "Copy link address").setOnMenuItemClickListener(handler);
                menu.add(0, ID_DOWNLOAD_IMAGE, 0, "Download image to holding folder (visit Import to complete)").setOnMenuItemClickListener(handler);

            } else if (result.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
                // Menu options for a hyperlink.
                menu.add(0, ID_COPY_LINK_ADDRESS, 0, "Copy link address").setOnMenuItemClickListener(handler);
                if (gsNodeData_title != null) {
                    menu.add(0, ID_COPY_LINK_TEXT, 0, "Copy link text").setOnMenuItemClickListener(handler);
                }
            }
        }

    }



    Handler OpenLinkInNewTabHandler;
    public void setOpenLinkInNewTabHandler(Handler handler){
        OpenLinkInNewTabHandler = handler;
    }


}
