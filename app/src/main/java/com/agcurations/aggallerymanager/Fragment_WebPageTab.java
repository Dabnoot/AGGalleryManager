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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;


public class Fragment_WebPageTab extends Fragment {

    GlobalClass globalClass;

    public static final String ARG_WEBPAGE_INDEX = "WEBPAGE_INDEX";

    public int giWebPageIndex;

    private VideoEnabledWebView gWebView;
    private VideoEnabledWebChromeClient webChromeClient;

    private EditText gEditText_Address;

    public Fragment_WebPageTab(int iWebPageIndex) {
        giWebPageIndex = iWebPageIndex;
    }

    public String gsWebAddress = "";

    boolean gbInitialized = false;

    ArrayList<String> gals_ResourceRequests;

    boolean gbWriteApplicationLog = false;
    String gsApplicationLogFilePath = "";

    public static ViewModel_Browser viewModel_browser; //Used to transfer data between fragments.

    public Fragment_WebPageTab() {
        //Empty constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() == null){
            //todo: write to log "[fragment] no activity object found".
            return;
        }

        viewModel_browser = new ViewModelProvider(getActivity()).get(ViewModel_Browser.class);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if(gbWriteApplicationLog){
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }
        ApplicationLogWriter("OnCreate Start, getting application context.");

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        if (getArguments() != null) {
            giWebPageIndex = getArguments().getInt(ARG_WEBPAGE_INDEX, -1);
        }

        gals_ResourceRequests = new ArrayList<>();
        ApplicationLogWriter("OnCreate End.");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_web_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ApplicationLogWriter("OnViewCreated start.");
        if(getView() == null){
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
        webChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, loadingView, gWebView) // See all available constructors...
        {
            // Subscribe to standard events, such as onProgressChanged()...
            @Override
            public void onProgressChanged(WebView view, int progress)
            {
                // Your code...
            }
        };
        webChromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback()
        {
            @Override
            public void toggledFullscreen(boolean fullscreen)
            {
                // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:
                WindowInsetsController insetsController = getActivity().getWindow().getInsetsController();
                if(insetsController != null) {
                    if (fullscreen) {

                        /*WindowManager.LayoutParams attrs = getActivity().getWindow().getAttributes();
                        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                        attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                        getActivity().getWindow().setAttributes(attrs);
                        //noinspection all
                        getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);*/

                        insetsController.hide(WindowInsets.Type.systemBars());
                        getActivity().getWindow().setNavigationBarColor(Color.TRANSPARENT);

                    } else {
                        /*WindowManager.LayoutParams attrs = getActivity().getWindow().getAttributes();
                        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                        attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                        getActivity().getWindow().setAttributes(attrs);
                        //noinspection all
                        getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);*/

                        insetsController.show(WindowInsets.Type.systemBars());
                        getActivity().getWindow().setNavigationBarColor(getResources().getColor(R.color.colorNavigationBar, getActivity().getTheme()));

                    }
                }

            }
        });


        //Configure the WebView:
        gWebView.setBackgroundColor(Color.BLACK);
        //WebSettings webSettings = gWebView.getSettings(); No longer required with VideoEnabledWebView and VideoEnabledWebChromeClient.
        //webSettings.setJavaScriptEnabled(true); No longer required with VideoEnabledWebView and VideoEnabledWebChromeClient.
        //webSettings.setDomStorageEnabled(true); No longer required with VideoEnabledWebView and VideoEnabledWebChromeClient.

        final Fragment fParent = this;





        gWebView.setWebChromeClient(webChromeClient);


        gWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                String sTitle = view.getTitle();
                for (ItemClass_WebPageTabData icwptd : globalClass.gal_WebPages) {
                    int ihashCode = fParent.hashCode();
                    if (ihashCode == icwptd.iTabFragmentHashID) {
                        icwptd.sTabTitle = sTitle;
                        Bitmap bitmap_favicon = gWebView.getFavicon();
                        Activity_Browser activity_browser = (Activity_Browser) getActivity();
                        if (activity_browser != null) {
                            activity_browser.updateSingleTabNotch(icwptd, bitmap_favicon); //Update the tab label.
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
                    int ihashCode = fParent.hashCode();
                    if (ihashCode == icwptd.iTabFragmentHashID) {
                        if (icwptd.alsAddressHistory == null) {
                            icwptd.alsAddressHistory = new ArrayList<>();
                        }
                        //Add url to address history list for this tab, but first make sure that
                        //  we are not merely re-loading the current address:
                        int iAddressCount = icwptd.alsAddressHistory.size();
                        boolean bSkipSet = false;
                        if (iAddressCount > 0) {
                            String sCurrentAddress = icwptd.alsAddressHistory.get(iAddressCount - 1);
                            if (!url.equals(sCurrentAddress)) {
                                icwptd.alsAddressHistory.add(url);
                            } else {
                                bSkipSet = true;
                            }
                        } else {
                            icwptd.alsAddressHistory.add(url);
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
                gals_ResourceRequests.add(request.getUrl().toString());
                return super.shouldInterceptRequest(view, request);
            }
        });


        final View viewFragment = view;
        gEditText_Address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                //Log.d("test", "onEditorAction: KeyEvent.Keycode = " + keyEvent.getKeyCode());
                String sWebAddress = textView.getText().toString();
                if(!sWebAddress.startsWith("http")){
                    sWebAddress = "https://" + sWebAddress;
                    textView.setText(sWebAddress);
                }
                gEditText_Address.clearFocus();
                gsWebAddress = sWebAddress;
                gWebView.loadUrl(sWebAddress);

                if(getActivity() != null) {
                    //Hide the keyboard. EditText ActionGo attribute does not hide the keyboard.
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(viewFragment.getWindowToken(), 0);
                }
                return true;
            }
        });

        String sLoadedAddress = gWebView.getUrl();
        if(sLoadedAddress == null && !gsWebAddress.equals("")) {
            InitializeData();
        }

        ImageButton imageButton_ClearText = getView().findViewById(R.id.imageButton_ClearText);
        if(imageButton_ClearText != null){
            imageButton_ClearText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gEditText_Address.setText("");
                }
            });
        }

        ImageButton imageButton_ImportContent = getView().findViewById(R.id.imageButton_ImportContent);
        if(imageButton_ImportContent != null){
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
        ApplicationLogWriter("OnViewCreated end.");
    }

    private void ApplicationLogWriter(String sMessage){
        if(gbWriteApplicationLog){
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



    private void InitializeData(){
        ApplicationLogWriter("InitializeData start.");
        //Find the associated WebPageTabData and enter that data into the view:
        for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages){
            int ihashCode = this.hashCode();
            if(ihashCode == icwptd.iTabFragmentHashID){
                if(icwptd.alsAddressHistory != null){
                    int iAddressCount = icwptd.alsAddressHistory.size();
                    if(iAddressCount > 0){
                        String sCurrentAddress = icwptd.alsAddressHistory.get(iAddressCount - 1);
                        gEditText_Address.setText(sCurrentAddress);
                        gsWebAddress = sCurrentAddress;
                        gWebView.loadUrl(sCurrentAddress);
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
        if(sLoadedAddress == null && gsWebAddress.equals("")) {
            InitializeData();
        }
        ApplicationLogWriter("onResume end.");
    }





}
