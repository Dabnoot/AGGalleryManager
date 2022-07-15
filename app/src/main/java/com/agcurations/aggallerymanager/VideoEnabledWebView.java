package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.ListenableWorker;

/**
 * https://github.com/cprcrack/VideoEnabledWebView
 * This class serves as a WebView to be used in conjunction with a VideoEnabledWebChromeClient.
 * It makes possible:
 * - To detect the HTML5 video ended event so that the VideoEnabledWebChromeClient can exit full-screen.
 *
 * Important notes:
 * - Javascript is enabled by default and must not be disabled with getSettings().setJavaScriptEnabled(false).
 * - setWebChromeClient() must be called before any loadData(), loadDataWithBaseURL() or loadUrl() method.
 *
 * @author Cristian Perez (http://cpr.name)
 *
 */
public class VideoEnabledWebView extends WebView
{
    WebView webView;
    Context gcContext;

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
                        if(sURL == null){
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
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(gsNodeData_src));

                        Context cApplicationContext = ((Activity_Browser) gcContext).getApplicationContext();

                        final GlobalClass globalClass = (GlobalClass) cApplicationContext;
                        String sDownloadFolderRelativePath = globalClass.gsImageDownloadHoldingFolderTempRPath; //Android will DL to internal storage only.

                        String sFileNameRaw = gsNodeData_src;
                        if(sFileNameRaw.contains("/")){
                            sFileNameRaw = gsNodeData_src.substring(gsNodeData_src.lastIndexOf("/") + 1);
                        }
                        String sFileName = Service_Import.cleanFileNameViaTrim(sFileNameRaw);

                        request.setTitle("AGGallery+ Download Single Image")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) //Make download notifications disappear when completed.
                                //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                                //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                                .setMimeType("application/octet-stream")
                                .setDestinationInExternalFilesDir(cApplicationContext, sDownloadFolderRelativePath, sFileName);

                        final DownloadManager downloadManager = (DownloadManager) cApplicationContext.getSystemService(Context.DOWNLOAD_SERVICE);
                        final long lDownloadID = downloadManager.enqueue(request);



                        final Handler handler1 = new Handler(Looper.getMainLooper());
                        handler1.post(new Runnable() {
                            @Override
                            public void run() {

                                //Monitor the location for file downloads' completion:
                                int iElapsedWaitTime = 0;
                                int iWaitDuration = 5000; //milliseconds
                                boolean bFileDownloadComplete = false;
                                boolean bDownloadProblem = false;
                                boolean bPaused = false;
                                String sMessage;
                                String sDownloadFailedReason = "";
                                String sDownloadPausedReason = "";
                                boolean bDebug = false;

                                String sLogFilePath = globalClass.gfLogsFolder.getAbsolutePath() +
                                        File.separator + GlobalClass.GetTimeStampFileSafe() + "_ImageDLTransfer_WorkerLog.txt";
                                File fLog = new File(sLogFilePath);
                                FileWriter fwLogFile = null;
                                try {

                                    if(bDebug) fwLogFile = new FileWriter(fLog, true);

                                    sMessage = "Waiting for download to complete, a maximum of " + (GlobalClass.DOWNLOAD_WAIT_TIMEOUT / 1000) + " seconds.";
                                    if(bDebug) fwLogFile.write(sMessage + "\n");
                                    if(bDebug) fwLogFile.flush();
                                    while ((iElapsedWaitTime < GlobalClass.DOWNLOAD_WAIT_TIMEOUT) && !bFileDownloadComplete && !bDownloadProblem) {

                                        try {
                                            Thread.sleep(iWaitDuration);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        if (!bPaused) {
                                            iElapsedWaitTime += iWaitDuration;
                                        } else {
                                            iElapsedWaitTime += (int) (iWaitDuration / 10.0); //Wait longer if a download is paused.
                                        }
                                        if(bDebug) fwLogFile.write(".");
                                        if(bDebug) fwLogFile.flush();

                                        //Query for remaining downloads:

                                        DownloadManager.Query dmQuery = new DownloadManager.Query();
                                        dmQuery.setFilterById(lDownloadID);
                                        Cursor cursor = downloadManager.query(dmQuery);

                                        if (cursor.moveToFirst()) {
                                            do {
                                                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                                                int status = cursor.getInt(columnIndex);
                                                int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                                                int iReasonID = cursor.getInt(columnReason);
                                                int iLocalURIIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                                                String sLocalURI = cursor.getString(iLocalURIIndex);
                                                int iDownloadURI = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
                                                String sDownloadURI = cursor.getString(iDownloadURI);
                                                int iDownloadID = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                                                long lDownloadID = cursor.getLong(iDownloadID);

                                                bDownloadProblem = false;
                                                bPaused = false;
                                                bFileDownloadComplete = false;

                                                switch (status) {
                                                    case DownloadManager.STATUS_FAILED:
                                                        bDownloadProblem = true;
                                                        switch (iReasonID) {
                                                            case DownloadManager.ERROR_CANNOT_RESUME:
                                                                sDownloadFailedReason = "ERROR_CANNOT_RESUME";
                                                                break;
                                                            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                                                                sDownloadFailedReason = "ERROR_DEVICE_NOT_FOUND";
                                                                break;
                                                            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                                                                sDownloadFailedReason = "ERROR_FILE_ALREADY_EXISTS";
                                                                break;
                                                            case DownloadManager.ERROR_FILE_ERROR:
                                                                sDownloadFailedReason = "ERROR_FILE_ERROR";
                                                                break;
                                                            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                                                                sDownloadFailedReason = "ERROR_HTTP_DATA_ERROR";
                                                                break;
                                                            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                                                                sDownloadFailedReason = "ERROR_INSUFFICIENT_SPACE";
                                                                break;
                                                            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                                                                sDownloadFailedReason = "ERROR_TOO_MANY_REDIRECTS";
                                                                break;
                                                            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                                                                sDownloadFailedReason = "ERROR_UNHANDLED_HTTP_CODE";
                                                                break;
                                                            case DownloadManager.ERROR_UNKNOWN:
                                                                sDownloadFailedReason = "ERROR_UNKNOWN";
                                                                break;
                                                        }
                                                        sMessage = "\nThere was a problem with a download.";
                                                        sMessage = sMessage + "\n" + "Download: " + sDownloadURI;
                                                        sMessage = sMessage + "\n" + "Reason ID: " + iReasonID;
                                                        sMessage = sMessage + "\n" + "Reason text: " + sDownloadFailedReason;
                                                        if(bDebug) fwLogFile.write(sMessage + "\n\n");
                                                        if(bDebug) fwLogFile.flush();
                                                        break;
                                                    case DownloadManager.STATUS_PAUSED:
                                                        bPaused = true;
                                                        switch (iReasonID) {
                                                            case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                                                                sDownloadPausedReason = "PAUSED_QUEUED_FOR_WIFI";
                                                                break;
                                                            case DownloadManager.PAUSED_UNKNOWN:
                                                                sDownloadPausedReason = "PAUSED_UNKNOWN";
                                                                break;
                                                            case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                                                                sDownloadPausedReason = "PAUSED_WAITING_FOR_NETWORK";
                                                                break;
                                                            case DownloadManager.PAUSED_WAITING_TO_RETRY:
                                                                sDownloadPausedReason = "PAUSED_WAITING_TO_RETRY";
                                                                break;
                                                        }
                                                        sMessage = "\n" + "Download paused: " + sDownloadURI;
                                                        sMessage = sMessage + "\n" + "Reason ID: " + iReasonID;
                                                        sMessage = sMessage + "\n" + "Reason text: " + sDownloadPausedReason;
                                                        if(bDebug) fwLogFile.write(sMessage + "\n\n");
                                                        if(bDebug) fwLogFile.flush();

                                                        break;
                                                    case DownloadManager.STATUS_PENDING:
                                                        //No action.
                                                        break;
                                                    case DownloadManager.STATUS_RUNNING:
                                                        //No action.
                                                        break;
                                                    case DownloadManager.STATUS_SUCCESSFUL:
                                                        bFileDownloadComplete = true;

                                                        //As of Android version 11, API level 30, One UI 3.1, the DownloadManager
                                                        //  will only store files in the onboard storage, or something like that.
                                                        //  Move those files over to the SD Card before processing.
                                                        sLocalURI = sLocalURI.replace("file://", "");
                                                        sLocalURI = URLDecoder.decode(sLocalURI, StandardCharsets.UTF_8.toString());
                                                        File fSource = new File(sLocalURI);
                                                        String sFileName = fSource.getName();
                                                        if(bDebug) fwLogFile.write("Download completed: " + sFileName);
                                                        if (fSource.exists()) {
                                                            //Determine the destination filename:
                                                            File[] fDLHoldingFiles = globalClass.gfImageDownloadHoldingFolder.listFiles();
                                                            if(fDLHoldingFiles != null) {
                                                                if(fDLHoldingFiles.length > 0) {
                                                                    String sNew = sFileName;
                                                                    boolean bMatchFoundInExistingHoldingFiles;
                                                                    int iIterator = 0;
                                                                    do {
                                                                        bMatchFoundInExistingHoldingFiles = false;
                                                                        for (File fExisting : fDLHoldingFiles) {
                                                                            if (sNew.contentEquals(fExisting.getName())) {
                                                                                bMatchFoundInExistingHoldingFiles = true;
                                                                                break;
                                                                            }
                                                                        }
                                                                        if (bMatchFoundInExistingHoldingFiles) {
                                                                            iIterator += 1;
                                                                            //https://stackoverflow.com/questions/4545937/java-splitting-the-filename-into-a-base-and-extension
                                                                            String[] tokens = sFileName.split("\\.(?=[^\\.]+$)");
                                                                            if(tokens.length == 2) {
                                                                                sNew = tokens[0] + "_"  + String.format(Locale.getDefault(), "%04d", iIterator);
                                                                                sNew = sNew + "." + tokens[1];
                                                                            } else {
                                                                                sNew = tokens[0];
                                                                            }
                                                                        }
                                                                    } while (bMatchFoundInExistingHoldingFiles);
                                                                    sFileName = sNew;
                                                                }
                                                            }
                                                            String sDestination = globalClass.gfImageDownloadHoldingFolder.getAbsolutePath() + File.separator + sFileName;
                                                            File fDestination = new File(sDestination);
                                                            //Move the file to the working folder:
                                                            if (!fDestination.exists()) {
                                                                try {
                                                                    InputStream inputStream;
                                                                    OutputStream outputStream;
                                                                    inputStream = new FileInputStream(fSource.getPath());
                                                                    outputStream = new FileOutputStream(fDestination.getPath());
                                                                    byte[] buffer = new byte[100000];
                                                                    while ((inputStream.read(buffer, 0, buffer.length)) >= 0) {
                                                                        outputStream.write(buffer, 0, buffer.length);
                                                                    }
                                                                    outputStream.flush();
                                                                    outputStream.close();
                                                                    if(bDebug) fwLogFile.write(" Copied to working folder.");
                                                                    if (!fSource.delete()) {
                                                                        sMessage = "Could not delete source file after copy. Source: " + fSource.getAbsolutePath();
                                                                        if(bDebug) fwLogFile.write("Download monitoring: " + sMessage + "\n");
                                                                    } else {
                                                                        if(bDebug) fwLogFile.write(" Source file deleted.");
                                                                    }
                                                                    Toast.makeText(getContext(), "File download and transfer complete.", Toast.LENGTH_SHORT).show();
                                                                } catch (Exception e) {
                                                                    sMessage = fSource.getPath() + "\n" + e.getMessage();
                                                                    if(bDebug) fwLogFile.write("Stream copy exception: " + sMessage + "\n");
                                                                }
                                                            } //End if !FDestination.exists. If it does exist, we have already copied the file over.
                                                        } else { //End if fSource.exists. If it does not exist, we probably already moved it.
                                                            if(bDebug) fwLogFile.write(" Source file does not exist (already moved?).");
                                                        }
                                                        if(bDebug) fwLogFile.write("\n");
                                                        if(bDebug) fwLogFile.flush();

                                                        break;
                                                }
                                            } while (cursor.moveToNext() && bFileDownloadComplete && !bDownloadProblem); //End loop through download query results.


                                        } //End if cursor has a record.

                                    } //End loop waiting for download completion.

                                } catch (Exception e){
                                    sMessage = e.getMessage();
                                    if(sMessage == null){
                                        sMessage = "Null message";
                                    }
                                    Log.d("Image Download Transfer", sMessage) ;
                                }

                            }
                        });


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
            if(gsNodeData_title != null) {
                menu.add(0, ID_COPY_LINK_TEXT, 0, "Copy link text").setOnMenuItemClickListener(handler);
            }
        }

    }



    Handler OpenLinkInNewTabHandler;
    public void setOpenLinkInNewTabHandler(Handler handler){
        OpenLinkInNewTabHandler = handler;
    }


}
