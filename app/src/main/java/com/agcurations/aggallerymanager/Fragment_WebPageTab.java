package com.agcurations.aggallerymanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;


public class Fragment_WebPageTab extends Fragment {

    GlobalClass globalClass;

    public static final String ARG_WEBPAGE_INDEX = "WEBPAGE_INDEX";

    private VideoEnabledWebView gWebView;
    private WebViewClient gWebViewClient = null;
    private VideoEnabledWebChromeClient gWebChromeClient;

    private EditText gEditText_Address;

    public Fragment_WebPageTab(int iWebPageIndex) {
    }

    public String gsWebAddress = "";

    boolean gbInitialized = false;

    ArrayList<String> gals_ResourceRequests;

    boolean gbWriteApplicationLog = false;
    String gsApplicationLogFilePath = "";

    public static ViewModel_Browser viewModel_browser; //Used to transfer data between fragments.

    Activity_Browser.HandlerOpenLinkInNewTab handlerOpenLinkInNewTab;

    private int giThisFragmentHashCode = 0;

    public Fragment_WebPageTab() {
        //Empty constructor
    }

    boolean gPreloadWebPage = false;

    public Fragment_WebPageTab(String sURL) {
        gsWebAddress = sURL;
        gPreloadWebPage = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() == null) {
            //todo: write to log "[fragment] no activity object found".
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

        if (gPreloadWebPage) { //Preload webpage to cache data for faster tab switching.
            gWebView = new VideoEnabledWebView(getActivity());
            gWebViewClient = getNewWebViewClient();
            gWebView.setWebViewClient(gWebViewClient);
            gWebView.addJavascriptInterface(new JavaScriptInterfaceGetHTML(), "HtmlViewer");
            WebChromeClient webChromeClient = new WebChromeClient();
            gWebView.setWebChromeClient(webChromeClient); //todo: is this still necessary?
            gWebView.loadUrl(gsWebAddress);
        }

        ApplicationLogWriter("OnCreate End.");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_web_page, container, false);
    }

    int iWebViewNavigationHeight_Original;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ApplicationLogWriter("OnViewCreated start.");
        if (getView() == null) {
            //todo: write to log no view found.
            return;
        }

        gWebView = (VideoEnabledWebView) getView().findViewById(R.id.videoEnabledWebView_tabWebView);
        gEditText_Address = getView().findViewById(R.id.editText_Address);

        // Initialize the VideoEnabledWebChromeClient and set event handlers
        View nonVideoLayout = getActivity().findViewById(R.id.nonVideoLayout); // Your own view, read class comments
        ViewGroup videoLayout = (ViewGroup) getActivity().findViewById(R.id.videoLayout); // Your own view, read class comments
        //noinspection all
        View loadingView = getLayoutInflater().inflate(R.layout.view_loading_video, null); // Your own view, read class comments

        if(giThisFragmentHashCode == 0){
            giThisFragmentHashCode = this.hashCode();
        }
        gWebChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, loadingView, gWebView);
        gWebChromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback() {
            @Override
            public void toggledFullscreen(boolean fullscreen) {
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

        final RelativeLayout relativeLayout_WebViewNavigation = getView().findViewById(R.id.relativeLayout_WebViewNavigation);


        final GestureDetector gestureDetector_WebView = new GestureDetector(getActivity(), new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            boolean bIgnoreMotionEvent = false; //Required because moving the views under the user's

            //  finger triggers the onScroll event.
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (bIgnoreMotionEvent) {
                    bIgnoreMotionEvent = false;
                    return true;
                }
                Activity_Browser activity_browser = (Activity_Browser) getActivity();
                int iBrowserTopBarHeight_Current = 0;
                if (activity_browser != null) {
                    if (activity_browser.giBrowserTopBarHeight_Original == 0) {
                        activity_browser.giBrowserTopBarHeight_Original = activity_browser.relativeLayout_BrowserTopBar.getHeight();
                    }
                    iBrowserTopBarHeight_Current = activity_browser.relativeLayout_BrowserTopBar.getHeight();
                }
                if (iWebViewNavigationHeight_Original == 0) {
                    iWebViewNavigationHeight_Original = relativeLayout_WebViewNavigation.getHeight();
                }

                int iWebViewNavigationHeight_Current = relativeLayout_WebViewNavigation.getHeight();

                int iWebViewNavigationHeight_New;
                int iBrowserTopBarHeight_New;
                float fMovementMultiplier = 2.1f; //The bars don't appear to get out of the way fast
                //  enough. Part of this is because of the inclusion
                //  of 'bIgnoreMotionEvent' causing a skip of every
                //  other event while scrolling.
                if (distanceY > 0) { //User is scrolling down
                    if (iBrowserTopBarHeight_Current > 0) {
                        //Start hiding the tab bar:
                        iBrowserTopBarHeight_New = iBrowserTopBarHeight_Current - (int) (distanceY * fMovementMultiplier);
                        iBrowserTopBarHeight_New = Math.max(0, iBrowserTopBarHeight_New);
                        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) activity_browser.relativeLayout_BrowserTopBar.getLayoutParams();
                        rlp.height = iBrowserTopBarHeight_New;
                        activity_browser.relativeLayout_BrowserTopBar.setLayoutParams(rlp);
                        bIgnoreMotionEvent = true;
                    } else if (iWebViewNavigationHeight_Current > 0) {
                        //Start hiding the address bar
                        iWebViewNavigationHeight_New = iWebViewNavigationHeight_Current - (int) (distanceY * fMovementMultiplier);
                        iWebViewNavigationHeight_New = Math.max(0, iWebViewNavigationHeight_New);
                        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) relativeLayout_WebViewNavigation.getLayoutParams();
                        rlp.height = iWebViewNavigationHeight_New;
                        relativeLayout_WebViewNavigation.setLayoutParams(rlp);
                        bIgnoreMotionEvent = true;
                    }
                } else if (distanceY < 0) { //User is scrolling up

                    if (iWebViewNavigationHeight_Current < iWebViewNavigationHeight_Original) {
                        iWebViewNavigationHeight_New = iWebViewNavigationHeight_Current - (int) (distanceY * fMovementMultiplier);
                        iWebViewNavigationHeight_New = Math.min(iWebViewNavigationHeight_Original, iWebViewNavigationHeight_New);
                        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) relativeLayout_WebViewNavigation.getLayoutParams();
                        rlp.height = iWebViewNavigationHeight_New;
                        relativeLayout_WebViewNavigation.setLayoutParams(rlp);
                        bIgnoreMotionEvent = true;
                    } else if ((activity_browser != null) && (iBrowserTopBarHeight_Current < activity_browser.giBrowserTopBarHeight_Original)) {
                        //Start showing the tab bar:
                        iBrowserTopBarHeight_New = iBrowserTopBarHeight_Current - (int) (distanceY * fMovementMultiplier);
                        iBrowserTopBarHeight_New = Math.min(activity_browser.giBrowserTopBarHeight_Original, iBrowserTopBarHeight_New);
                        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) activity_browser.relativeLayout_BrowserTopBar.getLayoutParams();
                        rlp.height = iBrowserTopBarHeight_New;
                        activity_browser.relativeLayout_BrowserTopBar.setLayoutParams(rlp);
                        bIgnoreMotionEvent = true;
                    }
                }

                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
        View.OnTouchListener view_OnTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();
                return gestureDetector_WebView.onTouchEvent(event);
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

        String sLoadedAddress = gWebView.getUrl();
        if (sLoadedAddress == null && !gsWebAddress.equals("")) {  //Logic for properly determining when to reload, such as due to screen rotation.
            InitializeData();
        }

        ImageButton imageButton_ClearText = getView().findViewById(R.id.imageButton_ClearText);
        if (imageButton_ClearText != null) {
            imageButton_ClearText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gEditText_Address.setText("");
                }
            });
        }

        ImageButton imageButton_ImportContent = getView().findViewById(R.id.imageButton_ImportContent);
        if (imageButton_ImportContent != null) {
            imageButton_ImportContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Paste the current URL to the clipboard:
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText(Service_WebPageTabs.IMPORT_REQUEST_FROM_INTERNAL_BROWSER, gsWebAddress);
                    clipboard.setPrimaryClip(clipData);

                    //Send the user to the Import Activity:
                    globalClass.giSelectedCatalogMediaCategory = -1; //Don't know the type of media selected.
                    Intent intentImportGuided = new Intent(getActivity(), Activity_Import.class);
                    startActivity(intentImportGuided);
                }
            });
        }

        ImageButton imageButton_Back = getView().findViewById(R.id.imageButton_Back);
        if (imageButton_Back != null) {
            imageButton_Back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (gWebView.canGoBack()) {
                        gWebView.goBack();
                    }
                }
            });

        }

        ImageButton imageButton_Forward = getView().findViewById(R.id.imageButton_Forward);
        if (imageButton_Forward != null) {
            imageButton_Forward.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (gWebView.canGoForward()) {
                        gWebView.goForward();
                    }
                }
            });

        }


        ApplicationLogWriter("OnViewCreated end.");
    }


    private void ApplicationLogWriter(String sMessage) {
        if (gbWriteApplicationLog) {
            try {
                File fLog = new File(gsApplicationLogFilePath);
                FileWriter fwLogFile = new FileWriter(fLog, true);
                fwLogFile.write(GlobalClass.GetTimeStampReadReady() + ": " + "Fragment_WebPageTab" + ", " + sMessage + "\n");
                fwLogFile.close();
            } catch (Exception e) {
                Log.d("Log FileWriter", e.getMessage());
            }
        }

    }


    private void InitializeData() {
        ApplicationLogWriter("InitializeData start.");
        //Find the associated WebPageTabData and enter that data into the view:
        giThisFragmentHashCode = this.hashCode();
        for (ItemClass_WebPageTabData icwptd : globalClass.gal_WebPages) {
            if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
                if (icwptd.sAddress != null) {
                    if (!icwptd.sAddress.equals("")) {
                        String sAddress = icwptd.sAddress;
                        gEditText_Address.setText(sAddress);
                        gsWebAddress = sAddress;
                        gWebView.loadUrl(sAddress);
                        gWebView.gsTabID = icwptd.sTabID;
                    }
                }
                break;
            }
        }
        gbInitialized = true;
        ApplicationLogWriter("InitializeData end.");
    }

    @Override
    public void onResume() {
        super.onResume();
        ApplicationLogWriter("onResume start.");

        String sLoadedAddress = gWebView.getUrl();
        if (sLoadedAddress == null && gsWebAddress.equals("")) {
            InitializeData();
        }
        ApplicationLogWriter("onResume end.");
    }


    private WebViewClient getNewWebViewClient(){
        WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //Trigger update of the favicon address....
                gWebView.loadUrl("javascript:HtmlViewer.showHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');"); //This will trigger an observable, which may complete after the code below.

                String sTitle = view.getTitle();
                for (ItemClass_WebPageTabData icwptd : globalClass.gal_WebPages) {
                    if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
                        icwptd.sTabTitle = sTitle;
                        Activity_Browser activity_browser = (Activity_Browser) getActivity();
                        if (activity_browser != null) {
                            //Update memory and page storage file.
                            Service_WebPageTabs.startAction_SetWebPageTabData(getContext(), icwptd);
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
                for (ItemClass_WebPageTabData icwptd : globalClass.gal_WebPages) {
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
                            Service_WebPageTabs.startAction_SetWebPageTabData(getActivity().getApplicationContext(), icwptd);
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

        return webViewClient;
    }

    //========== Class to get the html from the webview =======================
    private final MutableLiveData<String> gmLiveDataStringHTML = new MutableLiveData<>(); //Used to assist with move of HTML data out of the webview.
    class JavaScriptInterfaceGetHTML {

        @JavascriptInterface
        public void showHTML(String html) {
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
                    Activity_Browser activity_browser = (Activity_Browser) getActivity();
                    if(activity_browser != null) {
                        //Update the favicon Address in the WebPageTabData:
                        for (ItemClass_WebPageTabData icwptd : globalClass.gal_WebPages) {
                            if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
                                icwptd.sFaviconAddress = sFaviconAddress;
                                Service_WebPageTabs.startAction_SetWebPageTabData(getContext(), icwptd);
                                break;
                            }
                        }
                        activity_browser.updateSingleTabNotchFavicon(giThisFragmentHashCode, null); //Update the tab label.

                    }
                }
            }
        };

        if(getActivity() != null) {
            gmLiveDataStringHTML.observe(getActivity(), observerStringHTML); //When the HTML code changes...
        }

    }



}



