package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


public class Fragment_Import_1c_ComicWebDetect extends Fragment {

    GlobalClass globalClass;

    private EditText gEditText_WebAddress;
    private Button gButton_Go;
    private Button gButton_Detect;
    private TextView gTextView_StatusInstructions;
    private WebView gWebView;
    private Button gButton_Next;

    public static ViewModel_ImportActivity viewModelImportActivity;

    public final MutableLiveData<String> gmLiveDataStringHTML = new MutableLiveData<>(); //Used to assist with move of HTML data out of the webview.

    private ResponseReceiver_ComicWebDetect responseReceiver_ComicWebDetect;

    private final String gsUnknownAddress = "UNKNOWN_ADDRESS";
    private ArrayList<String> galsRequestedResources;
    private final boolean gbWebSiteCheck = false;

    boolean gbEnableAutoDetect = false; //Used in combination with global setting.

    public Fragment_Import_1c_ComicWebDetect() {
        // Required empty public constructor
    }

    public static Fragment_Import_1c_ComicWebDetect newInstance() {
        return new Fragment_Import_1c_ComicWebDetect();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);

            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(Worker_Import_ComicAnalyzeHTML.WEB_COMIC_ANALYSIS_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        responseReceiver_ComicWebDetect = new ResponseReceiver_ComicWebDetect();

        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(responseReceiver_ComicWebDetect, filter);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_1c_comic_web_detect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getView() != null) {
            gEditText_WebAddress  = getView().findViewById(R.id.editText_WebAddress);
            gButton_Go = getView().findViewById(R.id.button_Go);
            gButton_Detect = getView().findViewById(R.id.button_Detect);
            gTextView_StatusInstructions = getView().findViewById((R.id.textView_StatusInstructions));
            gWebView = getView().findViewById(R.id.webView);
            gButton_Next = getView().findViewById(R.id.button_NextStep);

        }

        SetTextStatusMessage("Enter an address and click 'Go'.");

        //Configure the WebView:
        gWebView.setBackgroundColor(Color.BLACK);
        WebSettings webSettings = gWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        GlobalClass.ConfigureWebSettings(webSettings);

        //Add a JavaScript interface to get the HTML from the WebView:
        gWebView.addJavascriptInterface(new MyJavaScriptInterfaceGetHTML(), "HtmlViewer");
        //Add a JavaScript interface to get the element upon which the user has clicked (for testing):
        gWebView.addJavascriptInterface(new MyClickJsToAndroid(), "my"); //todo: clarify the coding. Who names a routine 'my'??

        gWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                gWebView.evaluateJavascript(addMyClickCallBackJs(),null);
                gButton_Detect.setEnabled(true);
                gEditText_WebAddress.setText(url);

                //Save the cookie to assist with download if required:
                //String sCookie = CookieManager.getInstance().getCookie(url);
                //Evaluate if an address matches a pattern:
                boolean bWebComicDataLocatorNotFound = true;
                for(ItemClass_WebComicDataLocator icWCDL: viewModelImportActivity.alWebComicDataLocators) {
                    String sNonExplicitAddress = icWCDL.sHostnameRegEx;
                    String sRegexExpression = sNonExplicitAddress.replace("%", "");
                    if (url.matches(sRegexExpression)) {
                        bWebComicDataLocatorNotFound = false;
                        //icWCDL.sCookie = sCookie;
                        //GlobalClass.gsCookie = sCookie;
                        break;
                    }
                }
                if(bWebComicDataLocatorNotFound){
                    //Set the "unknown" WebComicDataLocator as "the one".
                    for(ItemClass_WebComicDataLocator icWCDL: viewModelImportActivity.alWebComicDataLocators) {
                        if (icWCDL.sHostnameRegEx.matches(gsUnknownAddress)) {
                            //icWCDL.sCookie = sCookie;
                            //GlobalClass.gsCookie = sCookie;
                            break;
                        }
                    }
                }

                if(gbEnableAutoDetect && GlobalClass.gbComicAutoDetect && !bWebComicDataLocatorNotFound){
                    initiateDetection();
                } else {
                    SetTextStatusMessage("Click 'Detect' once the comic summary page has loaded with all image thumbnails.");
                }
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {



                String sURL = request.getUrl().toString();
                //Check to see if the request is for an image file.
                if(gbWebSiteCheck){
                    //Record requested resources for review later if requested during development.
                    galsRequestedResources.add(sURL);
                }
                if(sURL.contains("jpg") || sURL.contains("jpeg") || sURL.contains("png")){
                    if(getActivity() != null) {
                        if (globalClass == null) {
                            globalClass = (GlobalClass) getActivity().getApplicationContext();
                        }

//                        if (globalClass.galWebVideoDataLocators != null) {
//                            for (int i = 0; i < globalClass.galWebVideoDataLocators.size(); i++) {
//                                if (globalClass.galWebVideoDataLocators.get(i).bHostNameMatchFound) {
//                                    //Create a new VDSK, add this M3U8 or MP4 "match", tell it that the match is found.
//                                    ItemClass_VideoDownloadSearchKey vdsk;
//                                    if(sURL.contains("m3u8")) {
//                                        vdsk = new ItemClass_VideoDownloadSearchKey(VIDEO_DOWNLOAD_M3U8, null);
//                                        vdsk.sSearchStringMatchContent = sURL;
//                                        vdsk.bMatchFound = true;
//                                        //Add the VDSK to the WebDataLocator instance:
//                                        globalClass.galWebVideoDataLocators.get(i).alVideoDownloadSearchKeys.add(vdsk);
//                                    }
//                                    if(sURL.contains("mp4")) {
//                                        //Contains MP4. It could be a URL with both mp4 and m3U8 in the file name, so include both for investigation if that is the case.
//                                        vdsk = new ItemClass_VideoDownloadSearchKey(VIDEO_DOWNLOAD_LINK, "", "");
//                                        vdsk.sSearchStringMatchContent = sURL;
//                                        vdsk.bMatchFound = true;
//                                        //Add the VDSK to the WebDataLocator instance:
//                                        globalClass.galWebVideoDataLocators.get(i).alVideoDownloadSearchKeys.add(vdsk);
//                                    }
//
//                                    break;
//                                }
//                            }
//                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);


            }
        });

        if(gEditText_WebAddress != null){

            gEditText_WebAddress.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    RecordWebAddress();
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });


        }


        gButton_Go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sWebAddress = gEditText_WebAddress.getText().toString();
                SetTextStatusMessage("Loading webpage...");
                galsRequestedResources = new ArrayList<>();
                gWebView.loadUrl(sWebAddress);
            }
        });

        gButton_Detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initiateDetection();
            }
        });

        //Check to see if we got here because the user wants to import something that they found on the internal
        // browser:
        if(!GlobalClass.gsBrowserAddressClipboard.equals("")){
            gEditText_WebAddress.setText(GlobalClass.gsBrowserAddressClipboard);
            SetTextStatusMessage("Loading webpage...");
            galsRequestedResources = new ArrayList<>();
            gWebView.loadUrl(GlobalClass.gsBrowserAddressClipboard);
            GlobalClass.gsBrowserAddressClipboard = "";
            gbEnableAutoDetect = true; //If we are here and pasting in an address automatically, go ahead and do the detect automatically.
        }

    } //End onViewCreated

    public void initiateDetection(){
        if(gbWebSiteCheck) {
            StringBuilder sb = new StringBuilder();
            for (String s : galsRequestedResources) {
                sb.append(s).append("\n");
            }
            String s = sb.toString();
            Log.d("Resources Requested", s);
        }
        gWebView.loadUrl("javascript:HtmlViewer.showHTML" +
                "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
    }

    @Override
    public void onResume() {
        super.onResume();

        final Observer<String> observerStringHTML = new Observer<>() {
            @Override
            public void onChanged(String sHTML) {
                //Enter here when an assigned String is changed.
                //In particular, we enter here when a web page has finished loading.
                if (getActivity() != null) {

                    //Locate the WebComicDataLocator in-use, and assign the HTML:
                    if (globalClass == null) {
                        globalClass = (GlobalClass) getActivity().getApplicationContext();
                    }
                    if (viewModelImportActivity.sWebAddress.equals("")) {
                        RecordWebAddress(); //We usually end up here during testing because I have
                        //  pre-loaded the editText with data, so onChanged for the editText
                        //  never fires.
                    }
                    for (ItemClass_WebComicDataLocator icWCDL : viewModelImportActivity.alWebComicDataLocators) {
                        if (icWCDL.bHostNameMatchFound) {
                            icWCDL.sHTML = sHTML;
                            break;
                        }
                    }

                    String sDataRecordKey = GlobalClass.getNewCatalogRecordID(); //Not actually getting a new catalog item ID, just using it to generate a unique ID for data tagging.
                    if (!globalClass.WaitForObjectReady(GlobalClass.gabComicWebAnalysDataTMAvailable, 1)) {
                        Toast.makeText(getContext(), "Web data transfer unavailble.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    GlobalClass.gabComicWebAnalysDataTMAvailable.set(false);
                    //Add data to a feeder for the worker. Data must be transfered. Storing it in a static, ungrowing global is unsafe,
                    //  depending on how fast the system might attempt to do it.
                    GlobalClass.gtmComicWebDataLocators.put(sDataRecordKey, new ArrayList<>(viewModelImportActivity.alWebComicDataLocators));
                    GlobalClass.gabComicWebAnalysDataTMAvailable.set(true);

                    if (getContext() == null) return;
                    SetTextStatusMessage("Analyzing HTML...");
                    String sCallerID = "Service_Import:startActionComicAnalyzeHTML()";
                    Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                    Data dataComicAnalyzeHTML = new Data.Builder()
                            .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                            .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                            .putString(Worker_Import_ComicAnalyzeHTML.EXTRA_STRING_WEB_DATA_LOCATOR_AL_KEY, sDataRecordKey)
                            .build();
                    OneTimeWorkRequest otwrComicAnalyzeHTML = new OneTimeWorkRequest.Builder(Worker_Import_ComicAnalyzeHTML.class)
                            .setInputData(dataComicAnalyzeHTML)
                            .addTag(Worker_Import_ComicAnalyzeHTML.TAG_WORKER_IMPORT_COMIC_ANALYZE_HTML) //To allow finding the worker later.
                            .build();
                    WorkManager.getInstance(getContext()).enqueue(otwrComicAnalyzeHTML);

                }
            }
        };

        if(getActivity() != null) {
            gmLiveDataStringHTML.observe(getActivity(), observerStringHTML); //When the HTML code changes...
        }

    }

    @Override
    public void onPause() {
        if(viewModelImportActivity.sWebAddress.equals("")) {
            RecordWebAddress();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(responseReceiver_ComicWebDetect);
        }
        super.onDestroy();
    }


    private void RecordWebAddress(){

        if(getActivity() == null){
            return;
        }

        boolean bAddressOK = false;
        String sAddressCandidate = gEditText_WebAddress.getText().toString();
        if(sAddressCandidate.length() > 0) {

            viewModelImportActivity.alWebComicDataLocators = GlobalClass.getComicWebDataKeys();

            //Evaluate if an address matches a pattern:
            boolean bWebComicDataLocatorNotFound = true;
            for(ItemClass_WebComicDataLocator icWCDL: viewModelImportActivity.alWebComicDataLocators) {
                String sNonExplicitAddress = icWCDL.sHostnameRegEx;
                String sRegexExpression = sNonExplicitAddress.replace("%", "");
                if (sAddressCandidate.matches(sRegexExpression)) {
                    bWebComicDataLocatorNotFound = false;
                    bAddressOK = true;
                    icWCDL.bHostNameMatchFound = true;
                    icWCDL.sAddress = sAddressCandidate;
                    break;
                }
            }
            if(bWebComicDataLocatorNotFound){
                //Set the "unknown" WebComicDataLocator as "the one".
                for(ItemClass_WebComicDataLocator icWCDL: viewModelImportActivity.alWebComicDataLocators) {
                    if (icWCDL.sHostnameRegEx.matches(gsUnknownAddress)) {
                        bAddressOK = true;
                        icWCDL.bHostNameMatchFound = true;
                        icWCDL.sAddress = sAddressCandidate;
                        break;
                    }
                }
            }

        }
        if(bAddressOK) {
            viewModelImportActivity.sWebAddress = sAddressCandidate;
        } else {
            viewModelImportActivity.sWebAddress = "";
        }
    }




    private void SetTextStatusMessage(String sMessage){
        if(gTextView_StatusInstructions != null){
            gTextView_StatusInstructions.setText(sMessage);
        }
    }
    //=========================================================================

    //========== Class to get the html from the webview =======================

    class MyJavaScriptInterfaceGetHTML {

        @JavascriptInterface
        public void showHTML(String html) {
            gmLiveDataStringHTML.postValue(html);
        }

    }
    //=========================================================================

    //========== Routines to get the element clicked by the user ==============


    class MyClickJsToAndroid extends Object{
        @JavascriptInterface
        public void myClick(String idOrClass) {
            Log.d("WebViewClick", "myClick-> " + idOrClass);
        }
    }

    public static String addMyClickCallBackJs() {
        String js = "javascript:";
        js += "function myClick(event){" +
                "if(event.target.className == null){my.myClick(event.target.id)}" +
                "else{my.myClick(event.target.className)}}";
        js += "document.addEventListener(\"click\",myClick,true);";
        return js;
    }

    //=========================================================================




    //=========================================================================
    //========================= Receiver ======================================

    public class ResponseReceiver_ComicWebDetect extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;
            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
                SetTextStatusMessage(sMessage);
            } else {

                //Check to see if this is a response to update log or progress bar:
                boolean 	bUpdateLog;

                //Get booleans from the intent telling us what to update:
                bUpdateLog = intent.getBooleanExtra(GlobalClass.UPDATE_LOG_BOOLEAN,false);

                if(bUpdateLog){
                    String sLogLine;
                    sLogLine = intent.getStringExtra(GlobalClass.LOG_LINE_STRING);
                    if(sLogLine != null) {
                        //Present the text to the user:
                        SetTextStatusMessage(sLogLine);
                        //Check to see if the operation is complete:
                        if (sLogLine.contains("Click 'Next' to continue.")) { //todo: Add an actual flag variable.
                            gButton_Next.setEnabled(true);
                        } else {
                            gButton_Next.setEnabled(false);
                        }
                    }
                }


            }






        }
    }


}