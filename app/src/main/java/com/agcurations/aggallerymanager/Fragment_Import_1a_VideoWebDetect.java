package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.Editable;
import android.text.TextWatcher;
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

import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8;
import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TITLE;


public class Fragment_Import_1a_VideoWebDetect extends Fragment {

    GlobalClass globalClass;

    private EditText gEditText_WebAddress;
    private Button gbutton_PasteAddress;
    private Button gButton_Go;
    private Button gButton_Detect;
    private TextView gTextView_StatusInstructions;
    private WebView gWebView;
    private Button gButton_Next;

    public static ViewModel_ImportActivity viewModelImportActivity;

    public final MutableLiveData<String> gmLiveDataStringHTML = new MutableLiveData<>(); //Used to assist with move of HTML data out of the webview.

    private ImportDataServiceResponseReceiver importDataServiceResponseReceiver;


    public Fragment_Import_1a_VideoWebDetect() {
        // Required empty public constructor
    }

    public static Fragment_Import_1a_VideoWebDetect newInstance(String param1, String param2) {
        return new Fragment_Import_1a_VideoWebDetect();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(ImportDataServiceResponseReceiver.IMPORT_RESPONSE_VIDEO_WEB_DETECT);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();

        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(importDataServiceResponseReceiver, filter);
        }

        //Instantiate the ViewModel sharing data between fragments:
        if(getActivity() != null) {
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
        }

        if(getActivity() != null) {
            return;
        }
        globalClass = (GlobalClass) getActivity().getApplicationContext();




    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_1a_video_web_detect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getView() != null) {
            gbutton_PasteAddress = getView().findViewById(R.id.button_PasteAddress);
            gEditText_WebAddress  = getView().findViewById(R.id.editText_WebAddress);
            gButton_Go = getView().findViewById(R.id.button_Go);
            gButton_Detect = getView().findViewById(R.id.button_Detect);
            gTextView_StatusInstructions = getView().findViewById((R.id.textView_StatusInstructions));
            gWebView = getView().findViewById(R.id.webView);
            gButton_Next = getView().findViewById(R.id.button_NextStep);

        }

        SetTextStatusMessage("Enter an address and click 'Go'.");

        //Configure "Paste Address" button:
        if(gbutton_PasteAddress != null){

            gbutton_PasteAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(getContext() != null) {
                        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = clipboard.getPrimaryClip();
                        if(clipData != null) {
                            ClipData.Item clipData_Item = clipData.getItemAt(0);
                            if (clipData_Item != null) {
                                String sClipString = clipData_Item.getText().toString();
                                if (!sClipString.equals("")) {
                                    if(getView() != null) {
                                        EditText editText_WebAddress = getView().findViewById(R.id.editText_WebAddress);
                                        if (editText_WebAddress != null) {
                                            editText_WebAddress.setText(sClipString);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });

        }
        //End configuration of "Paste Address" button.



        //Configure the WebView:
        gWebView.setBackgroundColor(Color.BLACK);
        WebSettings webSettings = gWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        gWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HtmlViewer");

        gWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                gButton_Detect.setEnabled(true);
                SetTextStatusMessage("Click 'Detect'. If expected video does not appear in the results, try playing and pausing the video first.");
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String sURL = request.getUrl().toString();
                //Check to see if the request is for an m3u8 file.
                if(sURL.contains("m3u8")){
                    if(getActivity() != null) {
                        String sNonExplicitAddress = "^h%ttps:\\/\\/w%ww\\.p%ornh%ub\\.c%om\\/v%iew_v%ideo.p%hp(.*)"; //Don't allow b-o-t-s to easily find hard-coded addresses.
                        String sNHRegexExpression = sNonExplicitAddress.replace("%", "");
                        ItemClass_WebVideoDataLocator icWebDataLocator = null;
                        if (globalClass == null) {
                            globalClass = (GlobalClass) getActivity().getApplicationContext();
                        }

                        if (globalClass.galWebVideoDataLocators != null) {
                            for (ItemClass_WebVideoDataLocator icWVDL : globalClass.galWebVideoDataLocators) {
                                if (icWVDL.bHostNameMatchFound) {
                                    icWebDataLocator = icWVDL;
                                    break;
                                }
                            }
                            if (icWebDataLocator != null) {
                                if (icWebDataLocator.sHostnameRegEx.equals(sNHRegexExpression)) {
                                    for (ItemClass_VideoDownloadSearchKey vdsk : icWebDataLocator.alVideoDownloadSearchKeys) {
                                        if (vdsk.sDataType.equals(VIDEO_DOWNLOAD_M3U8)) {
                                            if(!vdsk.bMatchFound) { //There are multiple m3u8 files. Only record the first, master m3u8 file.
                                                vdsk.bMatchFound = true;
                                                vdsk.sSearchStringMatchContent = sURL;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);


            }
        });


        //String sTestAddress = "https://www.xnxx.com/video-uzl4597/bangbros_-_18_year_old_cutie_vanessa_phoenix_taking_big_dick_in_her_small_pussy";
        //String sTestAddress = "";
        //gEditText_WebAddress.setText(sTestAddress);

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
                gWebView.loadUrl(sWebAddress);
            }
        });

        gButton_Detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gWebView.loadUrl("javascript:HtmlViewer.showHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
            }
        });

    } //End onViewCreated

    @Override
    public void onResume() {
        super.onResume();

        final Observer<String> observerStringHTML = new Observer<String>() {
            @Override
            public void onChanged(String sHTML) {
                //Enter here when an assigned String is changed.
                //In particular, we enter here when a web page has finished loading.
                if(getActivity() != null) {

                    //Locate the WebVideoDataLocator in-use, and assign the HTML:
                    if(globalClass == null){
                        globalClass = (GlobalClass) getActivity().getApplicationContext();
                    }
                    if(viewModelImportActivity.sWebAddress.equals("")){
                        RecordWebAddress(); //We usually end up here during testing because I have
                                            //  pre-loaded the editText with data, so onChanged for the editText
                                            //  never fires.
                    }
                    for(ItemClass_WebVideoDataLocator icWVDL: globalClass.galWebVideoDataLocators) {
                        if (icWVDL.bHostNameMatchFound) {
                            icWVDL.sHTML = sHTML;
                            break;
                        }
                    }

                    SetTextStatusMessage("Analyzing HTML...");
                    Service_Import.startActionVideoAnalyzeHTML(getContext());

                }
            }
        };

        if(getActivity() != null) {
            gmLiveDataStringHTML.observe(getActivity(), observerStringHTML); //When the HTML code changes...
        }

        if(gbutton_PasteAddress != null) {
            if (getContext() != null) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = clipboard.getPrimaryClip();
                if (clipData != null) {
                    ClipData.Item clipData_Item = clipData.getItemAt(0);
                    if (clipData_Item != null) {
                        String sClipString = clipData_Item.getText().toString();
                        gbutton_PasteAddress.setEnabled(!sClipString.equals(""));
                    }
                }
            }
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
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(importDataServiceResponseReceiver);
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

            InitializeWebDataLocators();

            //Evaluate if an address matches a pattern:
            for(ItemClass_WebVideoDataLocator icWVDL: globalClass.galWebVideoDataLocators) {
                String sNonExplicitAddress = icWVDL.sHostnameRegEx;
                String sRegexExpression = sNonExplicitAddress.replace("%", "");
                if (sAddressCandidate.matches(sRegexExpression)) {
                    bAddressOK = true;
                    icWVDL.bHostNameMatchFound = true;
                    break;
                }
            }

        }
        if(bAddressOK) {
            viewModelImportActivity.sWebAddress = sAddressCandidate;
        } else {
            viewModelImportActivity.sWebAddress = "";
        }
    }

    private void InitializeWebDataLocators(){
        //Re-Populate video data locator structure (clears any previously-found data):
        String sNonExplicitAddress = "^h%ttps:\\/\\/w%ww\\.x%nxx\\.c%om\\/v%ideo(.*)"; //Don't allow b-o-t-s to easily find hard-coded addresses.
        String sNHRegexExpression = sNonExplicitAddress.replace("%","");
        ItemClass_WebVideoDataLocator itemClass_webVideoDataLocator = new ItemClass_WebVideoDataLocator(sNHRegexExpression);  //Re-create the data locator, clearing-out any found data.
        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys = new ArrayList<>();

        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                new ItemClass_VideoDownloadSearchKey(
                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TITLE,
                        "html5player.setVideoTitle('","');"));

        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                new ItemClass_VideoDownloadSearchKey(
                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TAGS,
                        "//div[@class='metadata-row video-tags']//a/text()"));

        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                new ItemClass_VideoDownloadSearchKey(
                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_THUMBNAIL,
                        "//div[@class='video-pic']//@src"));

        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                new ItemClass_VideoDownloadSearchKey(
                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_LINK,
                        "html5player.setVideoUrlLow('","');"));

        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                new ItemClass_VideoDownloadSearchKey(
                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_LINK,
                        "html5player.setVideoUrlHigh('","');"));

        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                new ItemClass_VideoDownloadSearchKey(
                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8,
                        "html5player.setVideoHLS('","');"));

        if(globalClass == null) {
            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }
        globalClass.galWebVideoDataLocators = new ArrayList<>();
        globalClass.galWebVideoDataLocators.add(itemClass_webVideoDataLocator);

        //Next webpage data locator:
        sNonExplicitAddress = "^h%ttps:\\/\\/w%ww\\.p%ornh%ub\\.c%om\\/v%iew_v%ideo.p%hp(.*)"; //Don't allow b-o-t-s to easily find hard-coded addresses.
        sNHRegexExpression = sNonExplicitAddress.replace("%","");
        itemClass_webVideoDataLocator = new ItemClass_WebVideoDataLocator(sNHRegexExpression);  //Re-create the data locator, clearing-out any found data.
        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys = new ArrayList<>();

        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                new ItemClass_VideoDownloadSearchKey(
                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TITLE,
                        "//span[@class='inlineFree']//text()"));

        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                new ItemClass_VideoDownloadSearchKey(
                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TAGS,
                        "//div[@class='tags']/a[@class='item js-mxp']/text()"));

        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                new ItemClass_VideoDownloadSearchKey(
                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_THUMBNAIL,
                        "//div[@class='mgp_videoPoster']//@src"));

        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                new ItemClass_VideoDownloadSearchKey(
                        ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8,
                        null)); //P.H.u.b. m3u8 needs to be picked up by a request listener
        //while playing the video.



        globalClass.galWebVideoDataLocators.add(itemClass_webVideoDataLocator);
    }


    private void SetTextStatusMessage(String sMessage){
        if(gTextView_StatusInstructions != null){
            gTextView_StatusInstructions.setText(sMessage);
        }
    }

    class MyJavaScriptInterface {

        @JavascriptInterface
        public void showHTML(String html) {
            gmLiveDataStringHTML.postValue(html);
        }

    }


    //======================================================================
    //========================= Receiver ===================================

    @SuppressWarnings("unchecked")
    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String IMPORT_RESPONSE_VIDEO_WEB_DETECT = "com.agcurations.aggallerymanager.intent.action.IMPORT_RESPONSE_VIDEO_WEB_DETECT";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;
            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
                SetTextStatusMessage(sMessage);
            } else {

                //Check to see if this is a response to update log or progress bar:
                boolean 	bUpdateLog;
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdateLog = intent.getBooleanExtra(Service_Import.UPDATE_LOG_BOOLEAN,false);
                /*bUpdatePercentComplete = intent.getBooleanExtra(Service_Import.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(Service_Import.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);*/

                if(bUpdateLog){
                    String sLogLine;
                    sLogLine = intent.getStringExtra(Service_Import.LOG_LINE_STRING);
                    if(sLogLine != null) {
                        //Present the text to the user:
                        SetTextStatusMessage(sLogLine);
                        //Check to see if the operation is complete:
                        if (sLogLine.contains("Click 'Next' to continue.")) {
                            gButton_Next.setEnabled(true);
                        } else {
                            gButton_Next.setEnabled(false);
                        }
                    }
                }
                /*if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(Service_Import.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_ImportProgress != null) {
                        gProgressBar_ImportProgress.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(Service_Import.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_ImportProgressBarText != null) {
                        gTextView_ImportProgressBarText.setText(sProgressBarText);
                    }
                }*/

            }






        }
    }


}