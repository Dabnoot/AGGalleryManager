package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;


public class Fragment_WebPageTab extends Fragment {

    GlobalClass globalClass;

    private VideoEnabledWebView gWebView;
    private WebViewClient gWebViewClient = null;

    private TextInputEditText gEditText_Address;

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

    private ArrayList<ItemClass_WebComicDataLocator> galWebComicDataLocators;

    private String gsCustomDownloadPrompt = "";
    private final int CUSTOM_DOWNLOAD_OPTION_NO_TO_HALT = 1;
    private final int CUSTOM_DOWNLOAD_OPTION_YES_NO = 2;
    private int giCustomDownloadOptions = CUSTOM_DOWNLOAD_OPTION_NO_TO_HALT;

    private int giMediaCategory = -1;

    ResponseReceiver_WebPageTab responseReceiver_WebPageTab;

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

        //Configure a response receiver for this web page tab to capture data from workers, specifically the comic web detect worker
        //  to accelerate detection and import of comics that are part of a collection.
        responseReceiver_WebPageTab = new ResponseReceiver_WebPageTab();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Import_ComicAnalyzeHTML.WEB_COMIC_ANALYSIS_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(responseReceiver_WebPageTab,filter);

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

        if(giThisFragmentHashCode == 0){
            giThisFragmentHashCode = this.hashCode();
        }

        // Initialize the VideoEnabledWebChromeClient and set event handlers
        View nonVideoLayout = getActivity().findViewById(R.id.nonVideoLayout); // Your own view, read class comments
        ViewGroup videoLayout = getActivity().findViewById(R.id.videoLayout); // Your own view, read class comments
        //noinspection all
        View loadingView = getLayoutInflater().inflate(R.layout.view_loading_video, null); // Your own view, read class comments

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
        GlobalClass.ConfigureWebSettings(webSettings);

        if(GlobalClass.giWebViewSettings_UserAgentString.equals("")) {
            GlobalClass.giWebViewSettings_UserAgentString = webSettings.getUserAgentString();
        }

        gWebView.gbEnableNewTabLinkage = true; //Allow a context menu popup that contains "Open link in new tab."

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


        ImageButton imageButton_ImportContent = getView().findViewById(R.id.imageButton_ImportContent);
        if (imageButton_ImportContent != null) {
            imageButton_ImportContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Paste the current URL to the internal clipboard:
                    if(getContext() == null){
                        return;
                    }

                    GlobalClass.gsBrowserAddressClipboard = gsWebAddress;

                    GlobalClass.giSelectedCatalogMediaCategory = giMediaCategory; //Sometimes know the media category if the site was properly detected.

                    if(gsCustomDownloadPrompt.equals("")) {
                        //Send the user to the Import Activity:
                        Intent intentImportGuided = new Intent(getActivity(), Activity_Import.class);
                        startActivity(intentImportGuided);

                    } else {
                        //Else, if there is a custom prompt, show it to the user and then perform their need:
                        Context context = getContext();
                        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogCustomStyle);
                        builder.setTitle("Import");
                        builder.setMessage(gsCustomDownloadPrompt);

                        builder.setPositiveButton("Yes", (dialog, id) -> {
                            dialog.dismiss();
                            Toast.makeText(context, "No action", Toast.LENGTH_SHORT).show();
                        });
                        builder.setNegativeButton("No", (dialog, id) -> {
                            dialog.dismiss();
                            if(giCustomDownloadOptions != CUSTOM_DOWNLOAD_OPTION_NO_TO_HALT) {
                                //Send the user to the Import Activity:
                                Intent intentImportGuided = new Intent(getActivity(), Activity_Import.class);
                                startActivity(intentImportGuided);
                            }
                        });

                        AlertDialog adConfirmationDialog = builder.create();
                        adConfirmationDialog.show();
                    }

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
                        int iStackBackHistorySize = GlobalClass.gal_WebPagesForCurrentUser.get(i).stackBackHistory.size();
                        if(iStackBackHistorySize > 1) {
                            //The top of the back stack should be equal to the currenly displayed webpage.
                            String sCurrentAddress = GlobalClass.gal_WebPagesForCurrentUser.get(i).stackBackHistory.pop();
                            GlobalClass.gal_WebPagesForCurrentUser.get(i).stackForwardHistory.push(sCurrentAddress);
                            //Show the forward button as enabled:
                            ForwardButtonEnable();
                            if(iStackBackHistorySize == 2){
                                //If we are now at the top of the stack, show the back button as disabled:
                                BackButtonDisable();
                            }
                            String sBackURL = GlobalClass.gal_WebPagesForCurrentUser.get(i).stackBackHistory.peek();
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
                        if(GlobalClass.gal_WebPagesForCurrentUser.get(i).stackForwardHistory.size() > 0) {
                            String sForwardURL = GlobalClass.gal_WebPagesForCurrentUser.get(i).stackForwardHistory.pop();
                            GlobalClass.gal_WebPagesForCurrentUser.get(i).stackBackHistory.push(sForwardURL);
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
        if(getActivity() == null) return;
        gImageButton_Back.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_browser_fragment_button_enabled, getActivity().getTheme())));
    }
    private void BackButtonDisable(){
        if(getActivity() == null) return;
        gImageButton_Back.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.tab_backcolor_selected, getActivity().getTheme())));
    }
    private void ForwardButtonEnable(){
        if(getActivity() == null) return;
        gImageButton_Forward.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_browser_fragment_button_enabled, getActivity().getTheme())));
    }
    private void ForwardButtonDisable(){
        if(getActivity() == null) return;
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
        int iSelectedTabHashID = GlobalClass.gal_WebPagesForCurrentUser.get(iSelectedTab).iTabFragmentHashID;


        //Load data and webpage:
        for (ItemClass_WebPageTabData icwptd : GlobalClass.gal_WebPagesForCurrentUser) {
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
        for (int i = 0; i < GlobalClass.gal_WebPagesForCurrentUser.size(); i++) {
            ItemClass_WebPageTabData icwptd = GlobalClass.gal_WebPagesForCurrentUser.get(i);
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
        LocalBroadcastManager.getInstance(requireActivity().getApplicationContext()).unregisterReceiver(responseReceiver_WebPageTab);
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
            public void onPageFinished(WebView view, String sAddress) {

                //Get cookie. Sometimes needed when requesting a resource download according to some sources.
                //String sCookie = CookieManager.getInstance().getCookie(sAddress);
                //GlobalClass.gsCookie = sCookie;

                //Trigger update of the favicon address....
                gWebView.loadUrl("javascript:HtmlViewer.showHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');"); //This will trigger an observable, which may complete after the code below.

                String sTitle = "";
                String sTemp = view.getTitle();
                if(sTemp != null){
                    sTitle = sTemp;
                }

                //================================================================
                // ===== CLEAN ANY DATA TO ENSURE NO CORRUPTION OF DATA FILE =====
                //================================================================

                //If more data eventually requires verification, see coding for verification in other
                // data sets, such as in GlobalClass.validateCatalogItemData().

                //Go through all of the "illegal" strings/characters:
                for(String[] sIllegalStringSet: GlobalClass.gsIllegalRecordStrings) {
                    boolean bIllegalString = sTitle.contains(sIllegalStringSet[GlobalClass.CHECKABLE]);
                    if(bIllegalString) {
                        sTitle = sTitle.replace(sIllegalStringSet[GlobalClass.CHECKABLE],"");
                    }
                }

                //================================================================
                //================================================================
                //================================================================


                //Configure back/forward buttons:
                for (int i = 0; i < GlobalClass.gal_WebPagesForCurrentUser.size(); i++) {
                    ItemClass_WebPageTabData icwptd = GlobalClass.gal_WebPagesForCurrentUser.get(i);
                    if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
                        icwptd.sTabTitle = sTitle;
                        if(icwptd.stackBackHistory.size() > 0){
                            String sTopofBackStack = icwptd.stackBackHistory.peek();
                            if(!sTopofBackStack.equals(sAddress)){
                                icwptd.stackBackHistory.push(sAddress);
                                //Show the back button as enabled:
                                if(getActivity() == null) return;
                                gImageButton_Back.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_browser_fragment_button_enabled, getActivity().getTheme())));
                            }
                        } else {
                            icwptd.stackBackHistory.push(sAddress);
                        }

                        Activity_Browser activity_browser = (Activity_Browser) getActivity();
                        if (activity_browser != null) {
                            //Update memory and page storage file.
                            //Service_WebPageTabs.startAction_SetWebPageTabData(getContext(), icwptd);
                            GlobalClass.gal_WebPagesForCurrentUser.set(i, icwptd);
                            Activity_Browser.startAction_WriteWebPageTabData(getContext(), "Fragment_WebPageTab: getNewWebViewClient.onPageFinished()");

                        }
                        break;
                    }
                }


                //========================
                //== Pre-import checks
                //===========

                gsCustomDownloadPrompt = "";
                giCustomDownloadOptions = CUSTOM_DOWNLOAD_OPTION_NO_TO_HALT;
                giMediaCategory = -1;

                //Check to see if this is a comic site and if the user has downloaded a related comic.
                //  If so, prep to allow the user to download and group the comic.
                galWebComicDataLocators = GlobalClass.getComicWebDataKeys();
                //Evaluate if an address matches a pattern:
                ItemClass_WebComicDataLocator icWCDL_Match = null;
                for(ItemClass_WebComicDataLocator icWCDL: galWebComicDataLocators) {
                    String sNonExplicitAddress = icWCDL.sHostnameRegEx;
                    String sRegexExpression = sNonExplicitAddress.replace("%", "");
                    if (sAddress.matches(sRegexExpression)) {
                        icWCDL.bHostNameMatchFound = true;
                        icWCDL.sAddress = sAddress;  //Passing this item here so that this icWCDL can be passed to a processing worker,
                                                    //  and those results passed back and identified by the address.
                        icWCDL_Match = icWCDL;
                        break;
                    }
                }
                if(icWCDL_Match == null){
                    //If no match was found, return.
                    return;
                }

                //If we are here, this is a comic import site.
                giMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;

                String sComicSeriesIDStartString = icWCDL_Match.sComicSeriesIDStartString.replace("%", ""); //Remove obfuscating chars.

                //Get the ID of the comic from this website:
                String sComicSeriesID = "";
                if (!sComicSeriesIDStartString.equals("")) {
                    //If data has been provided internally to search for a series...
                    if(sAddress.startsWith(sComicSeriesIDStartString)){
                        //If this item appears to be a potential comic series entry. Check the ID.
                        sComicSeriesID = sAddress.substring(sComicSeriesIDStartString.length());
                        sComicSeriesID = sComicSeriesID.substring(0, sComicSeriesID.indexOf("/"));
                    }
                }

                //Look to see if this item is already located in the catalog..
                //  Move this check to a worker if it takes too long.
                boolean bItemInCatalog = false;
                String sMatchingCatalogItemID = "";
                String sCatalogItem_Address;
                String sCatalogItemWebComicID = "";
                for (Map.Entry<String, ItemClass_CatalogItem>
                        entry : GlobalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()) {

                    if (!entry.getValue().alsApprovedUsers.contains(GlobalClass.gicuCurrentUser.sUserName)) {
                        //Don't notify the user that there are existing, matching catalog items if those items
                        //  are not approved for this user.
                        continue;
                    }

                    sCatalogItem_Address = entry.getValue().sSource;
                    if(sCatalogItem_Address.equals(sAddress)){      //Check every item to ensure no exact match.
                        bItemInCatalog = true;
                    }
                    //Determine if this address is part of a collection:
                    if(sMatchingCatalogItemID.equals("")) { //Only find a matching comic series item once to grab group ID and tags.
                        if (!sComicSeriesID.equals("")) {
                            //If data has been provided internally to search for a series...
                            if (sCatalogItem_Address.startsWith(sComicSeriesIDStartString)) {
                                //If this item appears to be a potential comic series entry. Get the catalog item ID. It doesn't have to match the chapter, just any member of the collection.
                                sCatalogItemWebComicID = sCatalogItem_Address.substring(sComicSeriesIDStartString.length());
                                sCatalogItemWebComicID = sCatalogItemWebComicID.substring(0, sCatalogItemWebComicID.indexOf("/"));
                                if (sComicSeriesID.equals(sCatalogItemWebComicID)) {
                                    sMatchingCatalogItemID = entry.getKey();
                                }
                            }
                        }
                    }
                }

                if(bItemInCatalog){
                    gsCustomDownloadPrompt = "This comic exists in the catalog. Would you like to proceed with the import activity?";
                    giCustomDownloadOptions = CUSTOM_DOWNLOAD_OPTION_NO_TO_HALT;
                    return;
                }

                if(!sMatchingCatalogItemID.equals("")){

                    gsCustomDownloadPrompt = "This comic has been identified as belonging to a collection in the catalog.\n" +
                            " Title: " + Objects.requireNonNull(GlobalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).get(sMatchingCatalogItemID)).sTitle + "\n" +
                            " Would you like to add this item to the collection, applying the same group ID and tags?";
                    giCustomDownloadOptions = CUSTOM_DOWNLOAD_OPTION_YES_NO;
                }




            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                gEditText_Address.setText(url);

                //Update the recorded webpage history for this tab:
                //Find the associated WebPageTabData:
                for (int i = 0; i < GlobalClass.gal_WebPagesForCurrentUser.size(); i++) {
                    ItemClass_WebPageTabData icwptd = GlobalClass.gal_WebPagesForCurrentUser.get(i);
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
                            GlobalClass.gal_WebPagesForCurrentUser.set(i, icwptd);
                            Activity_Browser.startAction_WriteWebPageTabData(getContext(), "Fragment_WebPageTab: getNewWebViewClient.onPageStarted()");


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

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                //Give the user a message if they are leaving the host area, such as for an ad redirect:


                //Determine the base host address:
                String sHostAddress = "";
                int iCharIndexOfHostStart = gsWebAddress.indexOf("://");
                if(iCharIndexOfHostStart > 0){
                    iCharIndexOfHostStart += 3;
                    int iCharIndexOfHostEnd = gsWebAddress.indexOf("/", iCharIndexOfHostStart);
                    if(iCharIndexOfHostEnd > 0){
                        sHostAddress = gsWebAddress.substring(0,iCharIndexOfHostEnd);
                    }
                }

                //Determine if navigation should continue regardless:
                String sRequestedAddress = request.getUrl().toString();
                if(sHostAddress.equals("") ||
                        sRequestedAddress.startsWith(sHostAddress) ||
                        sRequestedAddress.equals("https://www.google.com")){
                    return super.shouldOverrideUrlLoading(view, request);
                }

                //Determine the host for the new page for reporting to the user:
                String sNewHostAddress = "";
                iCharIndexOfHostStart = sRequestedAddress.indexOf("://");
                if(iCharIndexOfHostStart > 0){
                    iCharIndexOfHostStart += 3;
                    int iCharIndexOfHostEnd = sRequestedAddress.indexOf("/", iCharIndexOfHostStart);
                    if(iCharIndexOfHostEnd > 0){
                        sNewHostAddress = sRequestedAddress.substring(0,iCharIndexOfHostEnd);
                    }
                }

                String sConfirmationMessage = "Web page tab is attempting to navigate to a new URL. Would you like to allow this navigation?\nNew host: " + sNewHostAddress;
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext(), R.style.AlertDialogCustomStyle);
                builder.setTitle("Tab Navigation");
                builder.setMessage(sConfirmationMessage);
                builder.setPositiveButton("Yes", (dialog, id) -> {
                    dialog.dismiss();
                    view.loadUrl(request.getUrl().toString());
                    //super.shouldOverrideUrlLoading(view, request);

                });
                builder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());
                AlertDialog adConfirmationDialog = builder.create();
                adConfirmationDialog.show();
                return true;
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
                        for (int i = 0; i < GlobalClass.gal_WebPagesForCurrentUser.size(); i++) {
                            ItemClass_WebPageTabData icwptd = GlobalClass.gal_WebPagesForCurrentUser.get(i);
                            if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
                                icwptd.sFaviconAddress = sFaviconAddress;
                                GlobalClass.gal_WebPagesForCurrentUser.set(i, icwptd);
                                Activity_Browser.startAction_WriteWebPageTabData(getContext(), "Fragment_WebPageTab: ConfigureHTMLWtacher.Observer.onChanged()");
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


    @SuppressWarnings("unchecked")
    public class ResponseReceiver_WebPageTab extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            //Get boolean indicating that an error may have occurred:
            boolean bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);

            if (bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to request to get comic downloads from html:
                boolean bGetComicDownloadsResponse = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_GET_WEB_COMIC_ANALYSIS_RESPONSE, false);
                if (bGetComicDownloadsResponse) {
                    ArrayList<ItemClass_File> alicf_ComicDownloadFileItems = (ArrayList<ItemClass_File>) intent.getSerializableExtra(GlobalClass.EXTRA_AL_GET_WEB_COMIC_ANALYSIS_RESPONSE);


                }

            }


        }
    }


}



