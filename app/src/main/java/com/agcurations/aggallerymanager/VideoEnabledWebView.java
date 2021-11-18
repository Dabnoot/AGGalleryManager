package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    }

    @SuppressWarnings("unused")
    public VideoEnabledWebView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        addedJavascriptInterface = false;
    }

    @SuppressWarnings("unused")
    public VideoEnabledWebView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        addedJavascriptInterface = false;
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            //saveNodeData();
        }
        return super.onTouchEvent(event);
    }

    public void saveNodeData(){
        //Called in advance to get ready for a potential long-press in which the user wants
        //  to view the link behind the item they are clicking.
        HREFHandler hrefHandler = new HREFHandler();
        Message msg = hrefHandler.obtainMessage();
        this.requestFocusNodeHref(msg); //It will take a moment for the msg to get processed.

    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        saveNodeData();
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

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        super.onCreateContextMenu(menu);

        final HitTestResult result = getHitTestResult();

        MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                // do the menu action
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData;
                switch (item.getItemId()){
                    case ID_OPEN_LINK_NEW_TAB:
                        Message msg = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("url", gsNodeData_url);
                        msg.setData(bundle);
                        OpenLinkInNewTabHandler.dispatchMessage(msg);
                        break;
                    case ID_COPY_LINK_ADDRESS:
                        clipData = ClipData.newPlainText("", gsNodeData_url);
                        clipboard.setPrimaryClip(clipData);
                        break;
                    case ID_COPY_LINK_TEXT:
                        clipData = ClipData.newPlainText("", gsNodeData_title);
                        clipboard.setPrimaryClip(clipData);
                        break;
                    default: //ID_DOWNLOAD_IMAGE.
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

        } else if (result.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
            // Menu options for a hyperlink.
            menu.add(0, ID_OPEN_LINK_NEW_TAB, 0, "Open in new tab").setOnMenuItemClickListener(handler);
            menu.add(0, ID_COPY_LINK_ADDRESS, 0, "Copy link address").setOnMenuItemClickListener(handler);
            menu.add(0, ID_COPY_LINK_TEXT, 0, "Copy link text").setOnMenuItemClickListener(handler);
        }

        /*menu.add(0, ID_OPEN_LINK_NEW_TAB, 0, "Open in new tab").setOnMenuItemClickListener(handler);
        menu.add(0, ID_COPY_LINK_ADDRESS, 0, "Copy link address").setOnMenuItemClickListener(handler);
        menu.add(0, ID_COPY_LINK_TEXT, 0, "Copy link text").setOnMenuItemClickListener(handler);
        menu.add(0, ID_DOWNLOAD_IMAGE, 0, "Download image").setOnMenuItemClickListener(handler);*/


    }

    Handler OpenLinkInNewTabHandler;
    public void setOpenLinkInNewTabHandler(Handler handler){
        OpenLinkInNewTabHandler = handler;
    }



}
