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
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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

import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_LINK;
import static com.agcurations.aggallerymanager.ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8;


public class Fragment_Import_1a_VideoWebDetect extends Fragment {

    boolean bDebug = false;

    GlobalClass globalClass;

    private EditText gEditText_WebAddress;
    private Button gbutton_PasteAddress;
    private Button gButton_Go;
    private Button gButton_Detect;
    private TextView gTextView_StatusInstructions;
    private VideoEnabledWebView gWebView;
    private Button gButton_Next;

    public static ViewModel_ImportActivity viewModelImportActivity;

    public final MutableLiveData<String> gmLiveDataStringHTML = new MutableLiveData<>(); //Used to assist with move of HTML data out of the webview.

    private ImportDataServiceResponseReceiver importDataServiceResponseReceiver;

    private final String gsUnknownAddress = "UNKNOWN_ADDRESS";
    private ArrayList<String> galsRequestedResources;
    private final boolean gbWebSiteCheck = false;

    public Fragment_Import_1a_VideoWebDetect() {
        // Required empty public constructor
    }

    public static Fragment_Import_1a_VideoWebDetect newInstance(String param1, String param2) {
        return new Fragment_Import_1a_VideoWebDetect();
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
        IntentFilter filter = new IntentFilter(ImportDataServiceResponseReceiver.IMPORT_RESPONSE_VIDEO_WEB_DETECT);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();

        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(importDataServiceResponseReceiver, filter);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_1a_video_web_detect, container, false);
    }

    private int iVideoStreamSenseCount = 0;
    private int iMP4SenseCount = 0;
    private int iWebmSenseCount = 0;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getView() != null) {
            gbutton_PasteAddress = getView().findViewById(R.id.button_PasteAddress);
            gEditText_WebAddress  = getView().findViewById(R.id.editText_WebAddress);
            gButton_Go = getView().findViewById(R.id.button_Go);
            gButton_Detect = getView().findViewById(R.id.button_Detect);
            gTextView_StatusInstructions = getView().findViewById((R.id.textView_StatusInstructions));
            gWebView = getView().findViewById(R.id.videoEnabledWebView_webView);
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

        // Initialize the VideoEnabledWebChromeClient and set event handlers
        if(getActivity() == null){
            return;
        }
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
        //webSettings.setJavaScriptEnabled(true);
        GlobalClass.ConfigureWebSettings(webSettings);

        gWebView.setWebChromeClient(gWebChromeClient);

        //Add a JavaScript interface to get the HTML from the WebView:
        gWebView.addJavascriptInterface(new MyJavaScriptInterfaceGetHTML(), "HtmlViewer");
        //Add a JavaScript interface to get the element upon which the user has clicked (for testing):
        gWebView.addJavascriptInterface(new MyClickJsToAndroid(), "my"); //todo: clarify the coding. Who names a routine 'my'??

        gWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                gWebView.evaluateJavascript(addMyClickCallBackJs(),null);
                gButton_Detect.setEnabled(true);
                String sCurrentWebAddressText = gEditText_WebAddress.getText().toString();
                if(!sCurrentWebAddressText.equals(url)) {
                    //Had to put this if statement in because some of the pages request the m3u8 BEFORE the
                    //  page even finishes loading. Thus the change of the text triggers a rest of the
                    //  vdsk structures and that m3u8 capture is lost.
                    gEditText_WebAddress.setText(url);
                }
                SetTextStatusMessage("Click 'Detect'. If expected video does not appear in the results, try playing and pausing the video first.");
            }

            boolean bMyTurn = true;

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

                if(!bMyTurn){
                    try{
                        Thread.sleep(20);
                    } catch (Exception e){
                        if(bDebug) Log.d("shouldInterceptRequest", "Problem sleeping.");
                    }
                }
                bMyTurn = false;


                String sURL = request.getUrl().toString();
                //Check to see if the request is for an m3u8 or mp4 file.
                if(gbWebSiteCheck){
                    //Record requested resources for review later if requested during development.
                    galsRequestedResources.add(sURL);
                }
                //if(bDebug) Log.d("shouldInterceptRequest", "Resource request: " + sURL);
                if((sURL.contains("m3u8") || sURL.contains("mp4") || sURL.contains("webm")) && !globalClass.gbWorkerVideoAnalysisInProgress
                        && !(globalClass.gbOptionSilenceActiveStreamListening && sURL.contains(".ts"))){

                    //Enter here if we detect a valid file
                    // but not if we are currently processing detected items
                    // and not if the user has selected to not listed for video segments.


                    String sAnnounce = "";
                    if(sURL.contains(".ts")){
                        // ".ts" is likely just the m3u8 downloading segments to a video player.
                        sAnnounce = "Possible video segment address intercepted. ";
                    }


                    if(getActivity() != null) {
                        if (globalClass == null) {
                            globalClass = (GlobalClass) getActivity().getApplicationContext();
                        }

                        boolean bLinkIsUnique = true;

                        if (globalClass.galWebVideoDataLocators != null) {
                            for (int i = 0; i < globalClass.galWebVideoDataLocators.size(); i++) {
                                if (globalClass.galWebVideoDataLocators.get(i).bHostNameMatchFound) {

                                    //Make sure this link is not a duplicate:
                                    for(ItemClass_VideoDownloadSearchKey icvdsk: globalClass.galWebVideoDataLocators.get(i).alVideoDownloadSearchKeys){
                                        if(icvdsk.sSearchStringMatchContent.equals(sURL)){
                                            bLinkIsUnique = false;
                                            break;
                                        }
                                    }
                                    if(!bLinkIsUnique){
                                        break;
                                    }

                                    //Create a new VDSK, add this M3U8 or MP4 "match", tell it that the match is found.
                                    ItemClass_VideoDownloadSearchKey vdsk;
                                    boolean bIsM3U8 = false;
                                    boolean bIsMP4 = false;
                                    boolean bIsWebm = false;
                                    if(sURL.contains(".m3u8") && sURL.contains(".mp4")) {
                                        //It could be a URL with both mp4 and m3U8 in the file name.
                                        //Inconclusive case.
                                        Log.d("shouldInterceptRequest", "Found both .m3u8 and .mp4 in same URL.");
                                        int iFirstBound = sURL.lastIndexOf("/") + 1;
                                        if(iFirstBound > 0){
                                            String sSub = sURL.substring(iFirstBound);
                                            int iQuestionMarkLoc = sSub.lastIndexOf("?");
                                            int iM3U8Loc;
                                            int iMP4Loc;
                                            if (iQuestionMarkLoc > 0) {
                                                //Solution cannot be past a question mark, if it exists. Generally stuff after that is not related to a file location.
                                                sSub = sSub.substring(0, iQuestionMarkLoc);
                                            }
                                            iM3U8Loc = sSub.lastIndexOf(".m3u8");
                                            iMP4Loc = sSub.lastIndexOf(".mp4");
                                            if(iM3U8Loc > iMP4Loc){
                                                bIsM3U8 = true;
                                                Log.d("shouldInterceptRequest", "Believed to be M3U8.");
                                            } else if(iMP4Loc > iM3U8Loc){
                                                bIsMP4 = true;
                                                Log.d("shouldInterceptRequest", "Believed to be MP4.");
                                            } else {
                                                Log.d("shouldInterceptRequest", "Could not determine if M3U8 or MP4.");
                                            }

                                        }
                                    } else if(sURL.contains("m3u8")) {
                                        bIsM3U8 = true;
                                    } else if(sURL.contains(".mp4")) {
                                        //Contains MP4.
                                        bIsMP4 = true;
                                    } else if(sURL.contains("webm")){
                                        bIsWebm = true;
                                    }
                                    if(bIsM3U8){
                                        vdsk = new ItemClass_VideoDownloadSearchKey(VIDEO_DOWNLOAD_M3U8, null);
                                        vdsk.sSearchStringMatchContent = sURL;
                                        vdsk.bMatchFound = true;
                                        //Add the VDSK to the WebDataLocator instance:
                                        globalClass.galWebVideoDataLocators.get(i).alVideoDownloadSearchKeys.add(vdsk);
                                    } else if(bIsMP4 || bIsWebm){
                                        vdsk = new ItemClass_VideoDownloadSearchKey(VIDEO_DOWNLOAD_LINK, "", "");
                                        vdsk.sSearchStringMatchContent = sURL;
                                        vdsk.bMatchFound = true;
                                        //Add the VDSK to the WebDataLocator instance:
                                        if(bDebug) Log.d("shouldInterceptRequest", "Created item as VIDEO_DOWNLOAD_LINK at index " + globalClass.galWebVideoDataLocators.get(i).alVideoDownloadSearchKeys.size());
                                        globalClass.galWebVideoDataLocators.get(i).alVideoDownloadSearchKeys.add(vdsk);
                                    }

                                    break;
                                }
                            }
                        }

                        if(bLinkIsUnique) {
                            if (sURL.contains("m3u8")) {
                                iVideoStreamSenseCount++;
                                sAnnounce = sAnnounce + "Possible video stream (" + iVideoStreamSenseCount + ") address intercepted. ";
                                //Make a call to set textbox color indicator (since we are not on the UI thread):
                                Intent broadcastIntent_VideoWebDetectResponse = new Intent();
                                broadcastIntent_VideoWebDetectResponse.putExtra("M3U8_DETECTED", true);
                                broadcastIntent_VideoWebDetectResponse.setAction(ImportDataServiceResponseReceiver.IMPORT_RESPONSE_VIDEO_WEB_DETECT);
                                broadcastIntent_VideoWebDetectResponse.addCategory(Intent.CATEGORY_DEFAULT);
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent_VideoWebDetectResponse);
                            }
                            if (sURL.contains("mp4")) {
                                iMP4SenseCount++;
                                sAnnounce = sAnnounce + "Possible video mp4 (" + iMP4SenseCount + ") address intercepted.";
                            }
                            if (sURL.contains("webm")) {
                                iWebmSenseCount++;
                                sAnnounce = sAnnounce + "Possible video webm (" + iWebmSenseCount + ") address intercepted.";
                            }
                            if (!sAnnounce.equals("")) {
                                Intent broadcastIntent_VideoWebDetectResponse = new Intent();
                                broadcastIntent_VideoWebDetectResponse.putExtra(GlobalClass.UPDATE_LOG_BOOLEAN, true);
                                broadcastIntent_VideoWebDetectResponse.putExtra(GlobalClass.LOG_LINE_STRING, sAnnounce);
                                broadcastIntent_VideoWebDetectResponse.setAction(ImportDataServiceResponseReceiver.IMPORT_RESPONSE_VIDEO_WEB_DETECT);
                                broadcastIntent_VideoWebDetectResponse.addCategory(Intent.CATEGORY_DEFAULT);
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent_VideoWebDetectResponse);
                            }
                        }
                    }
                }
                bMyTurn = true;
                return super.shouldInterceptRequest(view, request);


            }
        });


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
                galsRequestedResources = new ArrayList<>();
                gWebView.loadUrl(sWebAddress);
            }
        });

        gButton_Detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(gbWebSiteCheck) {
                    StringBuilder sb = new StringBuilder();
                    for (String s : galsRequestedResources) {
                        sb.append(s).append("\n");
                    }
                    Log.d("Fragment_Import_1a_VideoWebDetect", sb.toString());
                }

                gWebView.loadUrl("javascript:HtmlViewer.showHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
            }
        });

        //Check to see if we got here because the user wants to import something that they found on the internal
        // browser:
        if (getContext() != null) {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData != null) {
                String sClipLabel = clipData.getDescription().getLabel().toString();
                if(sClipLabel.equals(GlobalClass.IMPORT_REQUEST_FROM_INTERNAL_BROWSER)){
                    ClipData.Item clipItem = clipData.getItemAt(0);
                    if(clipItem != null){
                        if(clipItem.getText() != null){
                            if(getActivity() == null) return;
                            String sWebAddress = clipItem.coerceToText(getActivity().getApplicationContext()).toString();
                            gEditText_WebAddress.setText(sWebAddress);
                            SetTextStatusMessage("Loading webpage...");
                            galsRequestedResources = new ArrayList<>();
                            gWebView.loadUrl(sWebAddress);
                            clipboard.clearPrimaryClip();
                        }
                    }

                }
            }
        }

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
                    String sTitle = gWebView.getTitle();
                    for(ItemClass_WebVideoDataLocator icWVDL: globalClass.galWebVideoDataLocators) {
                        if (icWVDL.bHostNameMatchFound) {
                            icWVDL.sHTML = sHTML;
                            icWVDL.sWebPageTitle = sTitle;
                            break;
                        }
                    }

                    SetTextStatusMessage("Analyzing HTML...");
                    globalClass.gbWorkerVideoAnalysisInProgress = false; //Force capability to start
                                                    // analysis (even if a worker is in progress).
                    if(getContext() == null) return;
                    String sCallerID = "Service_Import:startActionVideoAnalyzeHTML()";
                    Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                    Data dataVideoAnalyzeHTML = new Data.Builder()
                            .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                            .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                            .build();
                    OneTimeWorkRequest otwrVideoAnalyzeHTML = new OneTimeWorkRequest.Builder(Worker_Import_VideoAnalyzeHTML.class)
                            .setInputData(dataVideoAnalyzeHTML)
                            .addTag(Worker_Import_VideoAnalyzeHTML.TAG_WORKER_IMPORT_VIDEOANALYZEHTML) //To allow finding the worker later.
                            .build();
                    WorkManager.getInstance(getContext()).enqueue(otwrVideoAnalyzeHTML);

                }
            }
        };

        if(getActivity() != null) {
            gmLiveDataStringHTML.observe(getActivity(), observerStringHTML); //When the HTML code changes...

            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }

        if(gbutton_PasteAddress != null) {
            if (getContext() != null) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = clipboard.getPrimaryClip();
                if (clipData != null) {
                    ClipData.Item clipData_Item = clipData.getItemAt(0);
                    if (clipData_Item != null) {
                        if(clipData_Item.getText() != null) {
                            try {
                                String sClipString = (String) clipData_Item.coerceToText(getActivity().getApplicationContext());
                                gbutton_PasteAddress.setEnabled(!sClipString.equals(""));
                            } catch (Exception e){
                                Log.d("Problem", e.getMessage());
                            }
                        }
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
            boolean bWebVideoDataLocatorNotFound = true;
            for(ItemClass_WebVideoDataLocator icWVDL: globalClass.galWebVideoDataLocators) {
                String sNonExplicitAddress = icWVDL.sHostnameRegEx;
                String sRegexExpression = sNonExplicitAddress.replace("%", "");
                if (sAddressCandidate.matches(sRegexExpression)) {
                    bWebVideoDataLocatorNotFound = false;
                    bAddressOK = true;
                    icWVDL.bHostNameMatchFound = true;
                    break;
                }
            }
            if(bWebVideoDataLocatorNotFound){
                //Set the "unknown" WebVideoDataLocator as "the one".
                for(ItemClass_WebVideoDataLocator icWVDL: globalClass.galWebVideoDataLocators) {
                    if (icWVDL.sHostnameRegEx.matches(gsUnknownAddress)) {
                        bAddressOK = true;
                        icWVDL.bHostNameMatchFound = true;
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

    private void InitializeWebDataLocators(){
        //Re-Populate video data locator structure (clears any previously-found data):

        globalClass.galWebVideoDataLocators = new ArrayList<>();
        ItemClass_WebVideoDataLocator itemClass_webVideoDataLocator;

        itemClass_webVideoDataLocator =
                FormWebVideoDataLocator("^h%ttps:\\/\\/w%ww\\.x%nxx\\.c%om\\/v%ideo(.*)",
                        new String[][]{
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TITLE, "html5player.setVideoTitle('", "');"},
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TAGS, "//div[@class='metadata-row video-tags']//a/text()"},
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_THUMBNAIL, "//div[@class='video-pic']//@src"},
                                {VIDEO_DOWNLOAD_LINK, "html5player.setVideoUrlLow('","');"},
                                {VIDEO_DOWNLOAD_LINK, "html5player.setVideoUrlHigh('","');"},
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8, "html5player.setVideoHLS('","');"}
                        });
        globalClass.galWebVideoDataLocators.add(itemClass_webVideoDataLocator);

        itemClass_webVideoDataLocator =
                FormWebVideoDataLocator("^h%ttps:\\/\\/w%ww\\.p%ornh%ub\\.c%om\\/v%iew_v%ideo.p%hp(.*)",
                        new String[][]{
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TITLE, "//span[@class='inlineFree']//text()"},
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TAGS, "//div[@class='tags']/a[@class='item js-mxp']/text()"},
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_THUMBNAIL, "//div[@class='mgp_videoPoster']//@src"},
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8, null}//PpHhuubb m3u8 needs to be picked up by a request listener
                        });
        globalClass.galWebVideoDataLocators.add(itemClass_webVideoDataLocator);

        itemClass_webVideoDataLocator =
                FormWebVideoDataLocator("^h%ttps:\\/\\/x%ham%ster\\.c%om\\/videos(.*)",
                        new String[][]{
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TITLE, "//h1/text()"},
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_TAGS, "//a[@class='categories-container__item']/text()"},
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_THUMBNAIL, "//div[@class='xp-preload-image']/@style"},
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8, "//link[@rel='preload'][@as='fetch']/@href"},
                                {ItemClass_VideoDownloadSearchKey.VIDEO_DOWNLOAD_M3U8, null} //Allow listen for played video.
                        });
        globalClass.galWebVideoDataLocators.add(itemClass_webVideoDataLocator);


        //Create a WebVideoDataLocator for unknown webpages:
        itemClass_webVideoDataLocator =
                FormWebVideoDataLocator(gsUnknownAddress, new String[][]{});
        globalClass.galWebVideoDataLocators.add(itemClass_webVideoDataLocator);

        /*itemClass_webVideoDataLocator =
                FormWebVideoDataLocator(,
                        new String[][]{
                                {},
                                {},
                                {},
                                {},
                                {},
                                {}
                        });
        globalClass.galWebVideoDataLocators.add(itemClass_webVideoDataLocator);*/


        globalClass.galWebVideoDataLocators.add(itemClass_webVideoDataLocator);
    }


    private ItemClass_WebVideoDataLocator FormWebVideoDataLocator(String sNonExplicitAddress, String[][] sSearchKeys){
        //Include parenthesis in sNonExplicitAddress to obscure the web address so that searchboottss cannot find it.
        String sNHRegexExpression = sNonExplicitAddress.replace("%","");
        ItemClass_WebVideoDataLocator itemClass_webVideoDataLocator = new ItemClass_WebVideoDataLocator(sNHRegexExpression);  //Re-create the data locator, clearing-out any found data.
        itemClass_webVideoDataLocator.alVideoDownloadSearchKeys = new ArrayList<>();

        for(String[] sFields: sSearchKeys){
            if(sFields.length == 2) {
                //SxPathExpression Search Key
                itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                        new ItemClass_VideoDownloadSearchKey(
                                sFields[0], sFields[1]));
            } else if (sFields.length == 3){
                //Text Search Key
                itemClass_webVideoDataLocator.alVideoDownloadSearchKeys.add(
                        new ItemClass_VideoDownloadSearchKey(
                                sFields[0], sFields[1], sFields[2]));
            }
        }
        return itemClass_webVideoDataLocator;
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
            if(bDebug) Log.d("WebViewClick", "myClick-> " + idOrClass);
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

    @SuppressWarnings("unchecked")
    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String IMPORT_RESPONSE_VIDEO_WEB_DETECT = "com.agcurations.aggallerymanager.intent.action.IMPORT_RESPONSE_VIDEO_WEB_DETECT";

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
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdateLog = intent.getBooleanExtra(GlobalClass.UPDATE_LOG_BOOLEAN,false);

                if(bUpdateLog){
                    String sLogLine;
                    sLogLine = intent.getStringExtra(GlobalClass.LOG_LINE_STRING);
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


            }



            boolean bUpdateStatusColor = intent.getBooleanExtra("M3U8_DETECTED",false);
            if(bUpdateStatusColor){
                gTextView_StatusInstructions.setBackgroundColor(0xAA00AA00);
            }



        }
    }


}