package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class Fragment_WebPageTab extends Fragment {

    GlobalClass globalClass;

    private VideoEnabledWebView gWebView;
    private WebViewClient gWebViewClient = null;

    private EditText gEditText_Address;

    public String gsWebAddress = "";

    ArrayList<String> gals_ResourceRequests;

    boolean gbWriteApplicationLog = false;
    String gsApplicationLogFilePath = "";

    public static ViewModel_Browser viewModel_browser; //Used to transfer data between fragments.

    Activity_Browser.HandlerOpenLinkInNewTab handlerOpenLinkInNewTab;

    private int giThisFragmentHashCode = 0;

    public RelativeLayout gRelativeLayout_WebViewNavigation;

    private ImageButton gImageButton_Back;
    private ImageButton gImageButton_Forward;

    public Fragment_WebPageTab() {
        //Empty constructor
    }

    public Fragment_WebPageTab(String sURL) {
        gsWebAddress = sURL;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() == null) {
            return;
        }

        giThisFragmentHashCode = this.hashCode();

        viewModel_browser = new ViewModelProvider(getActivity()).get(ViewModel_Browser.class);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if (gbWriteApplicationLog) {
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }
        ApplicationLogWriter("OnCreate Start, getting application context.");

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        gals_ResourceRequests = new ArrayList<>();

        ConfigureHTMLWatcher();

        ApplicationLogWriter("OnCreate End.");
    }

    ViewGroup gVGContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        gVGContainer = container;
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_web_page, container, false);
    }

    int iWebViewNavigationHeight_Original;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ApplicationLogWriter("OnViewCreated start.");
        if (getActivity() == null || getView() == null) {
            return;
        }

        gWebView = getView().findViewById(R.id.videoEnabledWebView_tabWebView);
        gEditText_Address = getView().findViewById(R.id.editText_Address);

        // Initialize the VideoEnabledWebChromeClient and set event handlers
        View nonVideoLayout = getActivity().findViewById(R.id.nonVideoLayout); // Your own view, read class comments
        ViewGroup videoLayout = getActivity().findViewById(R.id.videoLayout); // Your own view, read class comments
        //noinspection all
        View loadingView = getLayoutInflater().inflate(R.layout.view_loading_video, null); // Your own view, read class comments

        if(giThisFragmentHashCode == 0){
            giThisFragmentHashCode = this.hashCode();
        }
        VideoEnabledWebChromeClient gWebChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, loadingView, gWebView);
        gWebChromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback() {
            @Override
            public void toggledFullscreen(boolean fullscreen) {
                if (getActivity() == null) {
                    return;
                }
                // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:
                WindowInsetsController insetsController = getActivity().getWindow().getInsetsController();
                if (insetsController != null) {
                    if (fullscreen) {
                        insetsController.hide(WindowInsets.Type.systemBars());
                        getActivity().getWindow().setNavigationBarColor(Color.TRANSPARENT);

                    } else {
                        insetsController.show(WindowInsets.Type.systemBars());
                        getActivity().getWindow().setNavigationBarColor(getResources().getColor(R.color.colorNavigationBar, getActivity().getTheme()));

                    }
                }

            }
        });


        //Configure the WebView:
        gWebView.setBackgroundColor(Color.BLACK);
        WebSettings webSettings = gWebView.getSettings();
        //webSettings.setJavaScriptEnabled(true); No longer required with VideoEnabledWebView and VideoEnabledWebChromeClient.
        webSettings.setDomStorageEnabled(true); //Required to load all graphics on some webpages.

        gWebView.setOpenLinkInNewTabHandler(handlerOpenLinkInNewTab);


        gWebView.setWebChromeClient(gWebChromeClient);

        if(gWebViewClient == null){

            gWebViewClient = getNewWebViewClient();
        }
        gWebView.setWebViewClient(gWebViewClient);
        gWebView.addJavascriptInterface(new JavaScriptInterfaceGetHTML(), "HtmlViewer");

        gRelativeLayout_WebViewNavigation = getView().findViewById(R.id.relativeLayout_WebViewNavigation);

        View.OnTouchListener view_OnTouchListener = new View.OnTouchListener() {

            private final float fInitialGarbage = -10000000;
            float fLPY = fInitialGarbage;
            float fLPDeltaY = fInitialGarbage;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();

                int action = event.getActionMasked();
                String[] sActionEnum = new String[]{
                        "ACTION_DOWN",
                        "ACTION_UP",
                        "ACTION_MOVE",
                        "ACTION_CANCEL",
                        "ACTION_OUTSIDE",
                        "ACTION_POINTER_DOWN",
                        "ACTION_POINTER_UP",
                        "ACTION_HOVER_MOVE",
                        "ACTION_SCROLL",
                        "ACTION_HOVER_ENTER",
                        "ACTION_HOVER_EXIT",
                        "ACTION_BUTTON_PRESS",
                        "ACTION_BUTTON_RELEASE",
                        "ACTION_UNKNOWN"
                };
                /*int[] p = {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.ACTION_CANCEL,
                        MotionEvent.ACTION_OUTSIDE,
                        MotionEvent.ACTION_POINTER_DOWN,
                        MotionEvent.ACTION_POINTER_UP,
                        MotionEvent.ACTION_HOVER_MOVE,
                        MotionEvent.ACTION_SCROLL,
                        MotionEvent.ACTION_HOVER_ENTER,
                        MotionEvent.ACTION_HOVER_EXIT,
                        MotionEvent.ACTION_BUTTON_PRESS,
                        MotionEvent.ACTION_BUTTON_RELEASE
                };*/

                boolean bDebugTopBarSlide = false;
                if(bDebugTopBarSlide) Log.d("onTouch:", sActionEnum[action]);



                if( action == MotionEvent.ACTION_MOVE) {

                    int iHistorySize = event.getHistorySize();
                    float fDeltaY;
                    if(iHistorySize == 0){
                        if(fLPY != fInitialGarbage){
                            fDeltaY = fLPY - event.getY();
                        } else {
                            fDeltaY = 0;
                        }

                    } else {
                        fDeltaY = event.getHistoricalY(0, 0) - event.getY(0);

                    }
                    if(bDebugTopBarSlide) Log.d("ACTION_MOVE", "DeltaY: " + fDeltaY + " fLPDeltaY: " + fLPDeltaY);

                    fLPY = event.getY();
                    if(
                            ((fDeltaY < 0 && fLPDeltaY > 0) || (fDeltaY > 0 && fLPDeltaY < 0))){
                        //Abort jitter mode induced by the movement of the view produced by this code when the top bars are height-adjusted at slow speed.
                        fLPDeltaY = fDeltaY;
                        return true;
                    }

                    fLPDeltaY = fDeltaY;

                    if(bDebugTopBarSlide) Log.d("ACTION_MOVE", "DeltaY: " + fDeltaY + " History: " + event.getHistorySize() + " Y: " + event.getY());

                    Activity_Browser activity_browser = (Activity_Browser) getActivity();
                    int iBrowserTopBarHeight_Current = 0;
                    if (activity_browser != null) {
                        if (activity_browser.giBrowserTopBarHeight_Original == 0) {
                            activity_browser.giBrowserTopBarHeight_Original = activity_browser.gRelativeLayout_BrowserTopBar.getHeight();
                        }
                        iBrowserTopBarHeight_Current = activity_browser.gRelativeLayout_BrowserTopBar.getHeight();
                    }
                    if (iWebViewNavigationHeight_Original == 0) {
                        iWebViewNavigationHeight_Original = gRelativeLayout_WebViewNavigation.getHeight();
                    }

                    int iWebViewNavigationHeight_Current = gRelativeLayout_WebViewNavigation.getHeight();

                    int iWebViewNavigationHeight_New;
                    int iBrowserTopBarHeight_New;
                    float fMovementMultiplier = 2.1f; //The bars don't appear to get out of
                                                            // the way fast enough.


                    if (fDeltaY > 0f) { //User is scrolling down
                        if (iBrowserTopBarHeight_Current > 0) {
                            //Start hiding the tab bar:
                            iBrowserTopBarHeight_New = iBrowserTopBarHeight_Current - (int) (fDeltaY * fMovementMultiplier);
                            iBrowserTopBarHeight_New = Math.max(0, iBrowserTopBarHeight_New);
                            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) activity_browser.gRelativeLayout_BrowserTopBar.getLayoutParams();
                            rlp.height = iBrowserTopBarHeight_New;
                            activity_browser.gRelativeLayout_BrowserTopBar.setLayoutParams(rlp);
                            return true; //Do not pass this touch event to the lower-level views.
                        } else if (iWebViewNavigationHeight_Current > 0f) {
                            //Start hiding the address bar
                            iWebViewNavigationHeight_New = iWebViewNavigationHeight_Current - (int) (fDeltaY * fMovementMultiplier);
                            iWebViewNavigationHeight_New = Math.max(0, iWebViewNavigationHeight_New);
                            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) gRelativeLayout_WebViewNavigation.getLayoutParams();
                            rlp.height = iWebViewNavigationHeight_New;
                            gRelativeLayout_WebViewNavigation.setLayoutParams(rlp);
                            return true; //Do not pass this touch event to the lower-level views.
                        } else {
                            //Max scroll reached - user is scrolling down and the top bar and address bar are fully hidden.
                            return false;
                        }
                    } else if (fDeltaY < 0) { //User is scrolling up

                        if (iWebViewNavigationHeight_Current < iWebViewNavigationHeight_Original) {
                            iWebViewNavigationHeight_New = iWebViewNavigationHeight_Current - (int) (fDeltaY * fMovementMultiplier);
                            iWebViewNavigationHeight_New = Math.min(iWebViewNavigationHeight_Original, iWebViewNavigationHeight_New);
                            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) gRelativeLayout_WebViewNavigation.getLayoutParams();
                            rlp.height = iWebViewNavigationHeight_New;
                            gRelativeLayout_WebViewNavigation.setLayoutParams(rlp);
                            return true; //Do not pass this touch event to the lower-level views.
                        } else if ((activity_browser != null) && (iBrowserTopBarHeight_Current < activity_browser.giBrowserTopBarHeight_Original)) {
                            //Start showing the tab bar:
                            iBrowserTopBarHeight_New = iBrowserTopBarHeight_Current - (int) (fDeltaY * fMovementMultiplier);
                            iBrowserTopBarHeight_New = Math.min(activity_browser.giBrowserTopBarHeight_Original, iBrowserTopBarHeight_New);
                            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) activity_browser.gRelativeLayout_BrowserTopBar.getLayoutParams();
                            rlp.height = iBrowserTopBarHeight_New;
                            activity_browser.gRelativeLayout_BrowserTopBar.setLayoutParams(rlp);
                            return true; //Do not pass this touch event to the lower-level views.
                        } else {
                            //Max scroll reached - user is scrolling up and the top bar and address bar are fully visible.
                            return false;
                        }
                    } else {
                        return true;
                    }


                }

                return false;  //return false to indicate that we have not handled the touch and to pass it on to child views, etc.

            }

        };
        gWebView.setOnTouchListener(view_OnTouchListener);

        final View viewFragment = view;
        gEditText_Address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                //Log.d("test", "onEditorAction: KeyEvent.Keycode = " + keyEvent.getKeyCode());
                String sWebAddress = textView.getText().toString();
                if (!sWebAddress.startsWith("http")) {
                    sWebAddress = "https://" + sWebAddress;
                    textView.setText(sWebAddress);
                }
                gEditText_Address.clearFocus();
                gsWebAddress = sWebAddress;
                gWebView.loadUrl(sWebAddress);

                if (getActivity() != null) {
                    //Hide the keyboard. EditText ActionGo attribute does not hide the keyboard.
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(viewFragment.getWindowToken(), 0);
                }
                return true;
            }
        });


        ImageButton imageButton_ClearText = getView().findViewById(R.id.imageButton_ClearText);
        if (imageButton_ClearText != null) {
            imageButton_ClearText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gEditText_Address.setText("");

                    //Check to see if there is a valid address on the clipboard. If so, check to see
                    // if the page is on any of the other fragments. If not, enter it into
                    //  the EditText, clear it from the clipboard, and navigate to the page.
                    String sPossibleAddress = "";
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    if(getContext() != null) {
                        ClipData clipData = clipboard.getPrimaryClip();
                        if(clipData != null) {
                            ClipData.Item clipData_Item = clipData.getItemAt(0);
                            if (clipData_Item != null) {
                                sPossibleAddress = clipData_Item.getText().toString();
                            }
                        }
                    }
                    if(sPossibleAddress.startsWith("http")){
                        boolean bAddressInUse = false;
                        for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages){
                            if(sPossibleAddress.equals(icwptd.sAddress)){
                                bAddressInUse = true;
                                break;
                            }
                        }
                        if(!bAddressInUse){
                            //clipboard.clearPrimaryClip();
                            gEditText_Address.setText(sPossibleAddress);
                            gsWebAddress = sPossibleAddress;
                            gWebView.loadUrl(sPossibleAddress);
                        }
                    }

                }
            });
        }

        ImageButton imageButton_ImportContent = getView().findViewById(R.id.imageButton_ImportContent);
        if (imageButton_ImportContent != null) {
            imageButton_ImportContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Paste the current URL to the clipboard:
                    if(getContext() == null){
                        return;
                    }
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText(Service_Browser.IMPORT_REQUEST_FROM_INTERNAL_BROWSER, gsWebAddress);
                    clipboard.setPrimaryClip(clipData);

                    //Send the user to the Import Activity:
                    globalClass.giSelectedCatalogMediaCategory = -1; //Don't know the type of media selected.
                    Intent intentImportGuided = new Intent(getActivity(), Activity_Import.class);
                    startActivity(intentImportGuided);
                }
            });
        }

        gImageButton_Back = getView().findViewById(R.id.imageButton_Back);
        if (gImageButton_Back != null) {
            gImageButton_Back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*if (gWebView.canGoBack()) {
                        gWebView.goBack();
                    }*/
                    int i = getWebPageTabDataIndex();
                    if(i >= 0){
                        int iStackBackHistorySize = globalClass.gal_WebPages.get(i).stackBackHistory.size();
                        if(iStackBackHistorySize > 1) {
                            //The top of the back stack should be equal to the currenly displayed webpage.
                            String sCurrentAddress = globalClass.gal_WebPages.get(i).stackBackHistory.pop();
                            globalClass.gal_WebPages.get(i).stackForwardHistory.push(sCurrentAddress);
                            //Show the forward button as enabled:
                            ForwardButtonEnable();
                            if(iStackBackHistorySize == 2){
                                //If we are now at the top of the stack, show the back button as disabled:
                                BackButtonDisable();
                            }
                            String sBackURL = globalClass.gal_WebPages.get(i).stackBackHistory.peek();
                            gWebView.loadUrl(sBackURL);
                            //onPageStarted will handle setting sAddress variables.
                            //don't worry about writing the updated back and finish stacks to the file here.
                            // It will get written after the page finishes loading.
                        } else {
                            //Show the back button as disabled:
                            BackButtonDisable();
                        }
                    }
                }
            });

        }

        gImageButton_Forward = getView().findViewById(R.id.imageButton_Forward);
        if (gImageButton_Forward != null) {
            gImageButton_Forward.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*if (gWebView.canGoForward()) {
                        gWebView.goForward();
                    }*/
                    int i = getWebPageTabDataIndex();
                    if(i >= 0){
                        if(globalClass.gal_WebPages.get(i).stackForwardHistory.size() > 0) {
                            String sForwardURL = globalClass.gal_WebPages.get(i).stackForwardHistory.pop();
                            globalClass.gal_WebPages.get(i).stackBackHistory.push(sForwardURL);
                            //Show the back button as enabled:
                            BackButtonEnable();

                            gWebView.loadUrl(sForwardURL);
                            //onPageStarted will handle setting sAddress variables.
                        } else {
                            //Show the forward button as disabled:
                            ForwardButtonDisable();
                        }
                        //don't worry about writing the updated back and finish stacks to the file here.
                        // It will get written after the page finishes loading.
                    }
                }
            });

        }



        ApplicationLogWriter("OnViewCreated end.");
    }

    private void BackButtonEnable(){
        gImageButton_Back.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_browser_fragment_button_enabled, getActivity().getTheme())));
    }
    private void BackButtonDisable(){
        gImageButton_Back.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.tab_backcolor_selected, getActivity().getTheme())));
    }
    private void ForwardButtonEnable(){
        gImageButton_Forward.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_browser_fragment_button_enabled, getActivity().getTheme())));
    }
    private void ForwardButtonDisable(){
        gImageButton_Forward.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.tab_backcolor_selected, getActivity().getTheme())));
    }

    private void ApplicationLogWriter(String sMessage) {
        if (gbWriteApplicationLog) {
            try {
                File fLog = new File(gsApplicationLogFilePath);
                FileWriter fwLogFile = new FileWriter(fLog, true);
                fwLogFile.write(GlobalClass.GetTimeStampReadReady() + ": " + "Fragment_WebPageTab" + ", " + sMessage + "\n");
                fwLogFile.close();
            } catch (Exception e) {
                if(e.getMessage() == null){
                    return;
                }
                Log.d("Log FileWriter", e.getMessage());
            }
        }

    }


    private void InitializeData() {
        ApplicationLogWriter("InitializeData start.");


        //Find the associated WebPageTabData:
        giThisFragmentHashCode = this.hashCode();

        //If the tab is not currently selected, don't load:
        Activity_Browser activity_browser = (Activity_Browser) getActivity();
        if(activity_browser == null) return;
        int iSelectedTab = activity_browser.tabLayout_WebTabs.getSelectedTabPosition();
        int iSelectedTabHashID = globalClass.gal_WebPages.get(iSelectedTab).iTabFragmentHashID;


        //Load data and webpage:
        for (ItemClass_WebPageTabData icwptd : globalClass.gal_WebPages) {
            if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
                if (icwptd.sAddress != null) {
                    if (!icwptd.sAddress.equals("")) {
                        String sAddress = icwptd.sAddress;
                        gEditText_Address.setText(sAddress);
                        gsWebAddress = sAddress;
                        gWebView.gsTabID = icwptd.sTabID;

                        if(icwptd.stackBackHistory.size() > 1){
                            BackButtonEnable(); //The top of the stack is the current address.
                        }
                        if(icwptd.stackForwardHistory.size() > 0){
                            ForwardButtonEnable();
                        }

                        if(iSelectedTabHashID == giThisFragmentHashCode){
                            gWebView.loadUrl(sAddress);
                        }
                    }
                }
                break;
            }
        }

        ApplicationLogWriter("InitializeData end.");
    }

    private int getWebPageTabDataIndex(){
        for (int i = 0; i < globalClass.gal_WebPages.size(); i++) {
            ItemClass_WebPageTabData icwptd = globalClass.gal_WebPages.get(i);
            if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
                return i;
            }
        }
        return -1;
    }


    @Override
    public void onResume() {
        super.onResume();
        ApplicationLogWriter("onResume start.");

        String sLoadedAddress = gWebView.getUrl();
        if (sLoadedAddress == null && !gsWebAddress.equals("")) {
            //The webview holds no url, but out internal string shows a url should be loaded.
            InitializeData();
        }
        ApplicationLogWriter("onResume end.");
    }

    @Override
    public void onDestroy() {
        destroyWebView();
        super.onDestroy();
    }

    public void destroyWebView() {
        //This routine added because Activity_Browser was crashing.
        //  Partially taken from https://stackoverflow.com/questions/17418503/destroy-webview-in-android.

        // Make sure you remove the WebView from its parent view before doing anything.
        if(gVGContainer != null) {
            gVGContainer.removeAllViews();
        }

        gWebView.clearHistory();

        // NOTE: clears RAM cache, if you pass true, it will also clear the disk cache.
        // Probably not a great idea to pass true if you have other WebViews still alive.
        gWebView.clearCache(true); //todo: Is this preventing recall of login between tabs?

        // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
        gWebView.loadUrl("about:blank");

        gWebView.onPause();
        gWebView.removeAllViews();

        // NOTE: This can occasionally cause a segfault below API 17 (4.2)
        gWebView.destroy();

        // Null out the reference so that you don't end up re-using it.
        gWebView = null;
    }

    private WebViewClient getNewWebViewClient(){
        return new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //Trigger update of the favicon address....
                gWebView.loadUrl("javascript:HtmlViewer.showHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');"); //This will trigger an observable, which may complete after the code below.

                String sTitle = view.getTitle();
                for (int i = 0; i < globalClass.gal_WebPages.size(); i++) {
                    ItemClass_WebPageTabData icwptd = globalClass.gal_WebPages.get(i);
                    if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
                        icwptd.sTabTitle = sTitle;
                        if(icwptd.stackBackHistory.size() > 0){
                            String sTopofBackStack = icwptd.stackBackHistory.peek();
                            if(!sTopofBackStack.equals(url)){
                                icwptd.stackBackHistory.push(url);
                                //Show the back button as enabled:
                                gImageButton_Back.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_browser_fragment_button_enabled, getActivity().getTheme())));
                            }
                        } else {
                            icwptd.stackBackHistory.push(url);
                        }

                        Activity_Browser activity_browser = (Activity_Browser) getActivity();
                        if (activity_browser != null) {
                            //Update memory and page storage file.
                            //Service_WebPageTabs.startAction_SetWebPageTabData(getContext(), icwptd);
                            globalClass.gal_WebPages.set(i, icwptd);
                            Service_Browser.startAction_WriteWebPageTabData(getContext(), "Fragment_WebPageTab: getNewWebViewClient.onPageFinished()");

                        }
                        break;
                    }
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                gEditText_Address.setText(url);

                //Update the recorded webpage history for this tab:
                //Find the associated WebPageTabData:
                for (int i = 0; i < globalClass.gal_WebPages.size(); i++) {
                    ItemClass_WebPageTabData icwptd = globalClass.gal_WebPages.get(i);
                    if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
                            /*if (icwptd.sAddress == null) {
                                icwptd.sAddress = new ArrayList<>();
                            }
                            //Add url to address history list for this tab, but first make sure that
                            //  we are not merely re-loading the current address:
                            int iAddressCount = icwptd.sAddress.size();
                            boolean bSkipSet = false;
                            if (iAddressCount > 0) {
                                String sCurrentAddress = icwptd.sAddress.get(iAddressCount - 1);
                                if (!url.equals(sCurrentAddress)) {
                                    icwptd.sAddress.add(url);
                                } else {
                                    bSkipSet = true;
                                }
                            } else {
                                icwptd.sAddress.add(url);
                            }*/
                        boolean bSkipSet = false;
                        if (icwptd.sAddress != null) {
                            if (!icwptd.sAddress.equals(url)) {
                                icwptd.sAddress = url;
                                gsWebAddress = url;
                            } else {
                                bSkipSet = true;
                            }
                        } else {
                            icwptd.sAddress = url;
                        }
                        if (!bSkipSet) {
                            if(getActivity() == null){
                                return;
                            }
                            //Service_WebPageTabs.startAction_SetWebPageTabData(getActivity().getApplicationContext(), icwptd);

                            //Update memory and page storage file.
                            //Service_WebPageTabs.startAction_SetWebPageTabData(getContext(), icwptd);
                            globalClass.gal_WebPages.set(i, icwptd);
                            Service_Browser.startAction_WriteWebPageTabData(getContext(), "Fragment_WebPageTab: getNewWebViewClient.onPageStarted()");


                        }
                        break;
                    }
                }
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                try {
                    gals_ResourceRequests.add(request.getUrl().toString());
                } catch (Exception e) {
                    //Sometimes we get an ArrayIndexOutofBoundsException item here.
                    //  Just ignore it. gals_ResourceRequests is just for searching through to find
                    //  images loaded, videos played, etc.
                }
                return super.shouldInterceptRequest(view, request);

            }
        };

    }

    //========== Class to get the html from the webview =======================
    private final MutableLiveData<String> gmLiveDataStringHTML = new MutableLiveData<>(); //Used to assist with move of HTML data out of the webview.
    class JavaScriptInterfaceGetHTML {

        @JavascriptInterface
        public void showHTML(String html) { //THIS ITEM IS USED. IGNORE THE WARNING.
            gmLiveDataStringHTML.postValue(html);
        }

    }

    private void ConfigureHTMLWatcher(){

        final Observer<String> observerStringHTML = new Observer<String>() {
            @Override
            public void onChanged(String sHTML) {
                //Enter here when an assigned String is changed.
                //In particular, we enter here when a web page has finished loading.

                //Find the favicon address:
                boolean bFaviconAddressFound = false;
                int iFoundLinkLocationStart = 0;
                int iFoundLinkLocationEnd;
                String sFaviconAddress = "";
                do {
                    iFoundLinkLocationStart = sHTML.indexOf("<link", iFoundLinkLocationStart + 1);
                    if (iFoundLinkLocationStart < 0) continue;
                    iFoundLinkLocationEnd = sHTML.indexOf(">", iFoundLinkLocationStart);
                    if (iFoundLinkLocationEnd < 0) continue;
                    String sLinkBlock = sHTML.substring(iFoundLinkLocationStart, iFoundLinkLocationEnd);
                    int iFoundRelLocationStart = sLinkBlock.indexOf("rel");
                    if (iFoundRelLocationStart < 0) continue;
                    int iFirstStringQuote = sLinkBlock.indexOf("\"", iFoundRelLocationStart);
                    if (iFirstStringQuote < 0) continue;
                    int iEndStringQuote = sLinkBlock.indexOf("\"", iFirstStringQuote + 1);
                    if (iEndStringQuote < 0) continue;
                    String sRel = sLinkBlock.substring(iFirstStringQuote + 1, iEndStringQuote);

                    if(sRel.equals("icon")) {
                        int iFoundHrefLocationStart = sLinkBlock.indexOf("href");
                        if (iFoundHrefLocationStart < 0) continue;
                        iFirstStringQuote = sLinkBlock.indexOf("\"", iFoundHrefLocationStart);
                        if (iFirstStringQuote < 0) continue;
                        iEndStringQuote = sLinkBlock.indexOf("\"", iFirstStringQuote + 1);
                        if (iEndStringQuote < 0) continue;
                        sFaviconAddress = sLinkBlock.substring(iFirstStringQuote + 1, iEndStringQuote);
                        bFaviconAddressFound = true;
                    }

                } while (iFoundLinkLocationStart > 0 && !bFaviconAddressFound);
                if(bFaviconAddressFound){

                    //Handle the case where the favicon listed address is relative to the host, and not a full address:
                    if(!sFaviconAddress.startsWith("http")) {
                        try {
                            URL url = new URL(gsWebAddress);
                            String sHostPrefix = gsWebAddress.substring(0,gsWebAddress.indexOf("/"));
                            String sHost = sHostPrefix + "//" + url.getHost();
                            if(sFaviconAddress.startsWith("/")){
                                sFaviconAddress = sHost + sFaviconAddress;
                            } else {
                                sFaviconAddress = sHost + "/" + sFaviconAddress;
                            }
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }

                    }

                } else {
                    //If the favicon was not found using the textual searches, try looking for it at the host level:
                    try {
                        URL url = new URL(gsWebAddress);
                        String sHostPrefix = gsWebAddress.substring(0,gsWebAddress.indexOf("/"));
                        String sHost = sHostPrefix + "//" + url.getHost();
                        sFaviconAddress = sHost + "/favicon.ico";
                        //Address is to be passed to Glide, so
                        // let Glide deal with it for now.
                        bFaviconAddressFound = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(bFaviconAddressFound){
                    Activity_Browser activity_browser = (Activity_Browser) getActivity();
                    if(activity_browser != null) {
                        //Update the favicon Address in the WebPageTabData:
                        for (int i = 0; i < globalClass.gal_WebPages.size(); i++) {
                            ItemClass_WebPageTabData icwptd = globalClass.gal_WebPages.get(i);
                            if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
                                icwptd.sFaviconAddress = sFaviconAddress;
                                globalClass.gal_WebPages.set(i, icwptd);
                                Service_Browser.startAction_WriteWebPageTabData(getContext(), "Fragment_WebPageTab: ConfigureHTMLWtacher.Observer.onChanged()");
                                break;
                            }
                        }
                        activity_browser.updateSingleTabNotchFavicon(giThisFragmentHashCode); //Update the tab label.

                    }
                }

            }
        };

        if(getActivity() != null) {
            gmLiveDataStringHTML.observe(getActivity(), observerStringHTML); //When the HTML code changes...
        }

    }



}



