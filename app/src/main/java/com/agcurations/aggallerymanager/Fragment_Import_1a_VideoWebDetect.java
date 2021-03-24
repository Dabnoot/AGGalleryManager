package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.icu.text.AlphabeticIndex;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class Fragment_Import_1a_VideoWebDetect extends Fragment {

    private EditText gEditText_WebAddress;
    private Button gButton_Go;
    private Button gButton_Detect;
    private TextView gTextView_ShortStatus;
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

        Button button_PasteAddress = null;
        EditText editText_WebAddress = null;

        if(getView() != null) {
            button_PasteAddress = getView().findViewById(R.id.button_PasteAddress);
            gEditText_WebAddress  = getView().findViewById(R.id.editText_WebAddress);
            gButton_Go = getView().findViewById(R.id.button_Go);
            gButton_Detect = getView().findViewById(R.id.button_Detect);
            gTextView_ShortStatus = getView().findViewById((R.id.textView_ShortStatus));
            gWebView = getView().findViewById(R.id.webView);
            gButton_Next = getView().findViewById(R.id.button_NextStep);

        }

        //Configure "Paste Address" button:
        if(button_PasteAddress != null){
            if(getContext() != null) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = clipboard.getPrimaryClip();
                if(clipData != null) {
                    ClipData.Item clipData_Item = clipData.getItemAt(0);
                    if (clipData_Item != null) {
                        String sClipString = clipData_Item.getText().toString();
                        button_PasteAddress.setEnabled(!sClipString.equals(""));
                    }
                }
            }


            button_PasteAddress.setOnClickListener(new View.OnClickListener() {
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
                SetTextStatusMessage("Play video and click 'Detect'.");
            }
        });


        String sTestAddress = "https://www.xnxx.com/video-10olx73e/jay_s_pov_-_i_had_a_threesome_with_two_hot_blonde_teens";
        gEditText_WebAddress.setText(sTestAddress);


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
                if(getActivity() != null) {

                    //Search the HTML for data using "keys" that have been defined by previous
                    //  investigation of webpage data. These keys might need to change if the
                    //  website changes the HTML.
                    GlobalClass globalClass = (GlobalClass) getActivity().getApplicationContext();

                    int iMatchCount = 0;

                    //Clear any data from a previous search:
                    for (ItemClass_VideoDownloadSearchKey vdsk :globalClass.galVideoDownloadSearchKeys){
                        if(vdsk.bMatchFound){
                            vdsk.sSearchStringMatchContent = "";
                            vdsk.bMatchFound = false;
                        }
                    }

                    SetTextStatusMessage("Searching webpage for target data...");
                    for (ItemClass_VideoDownloadSearchKey vdsk :globalClass.galVideoDownloadSearchKeys){

                        int iStart;
                        int iEnd;

                        //We want the data between vdsk.sSearchStartString and vdsk.sSearchEndString.
                        iStart = sHTML.indexOf(vdsk.sSearchStartString);
                        if(iStart > -1){
                            iStart += vdsk.sSearchStartString.length();
                            iEnd = sHTML.indexOf(vdsk.sSearchEndString, iStart);
                            if(iEnd > -1) {
                                //We want the data between iStart and iEnd.
                                String sTemp;
                                sTemp = sHTML.substring(iStart, iEnd);
                                if(sTemp.length() > 0) {
                                    vdsk.bMatchFound = true;
                                    vdsk.sSearchStringMatchContent = sTemp;
                                    iMatchCount++;
                                }

                            }
                        }
                    } //End loop searching for data within the HTML

                    if(gButton_Next != null){
                        if(iMatchCount > 0) {
                            gButton_Next.setEnabled(true);
                            SetTextStatusMessage("Data located. Select 'Next' to continue.");
                            Service_Import.startActionGetVideoTags(getContext(), sHTML, "//div[@class='metadata-row video-tags']//a/text()");
                        } else {
                            SetTextStatusMessage("No target data found within this webpage.");
                            gButton_Next.setEnabled(false);
                        }
                    }


                }
            }
        };

        if(getActivity() != null) {
            gmLiveDataStringHTML.observe(getActivity(), observerStringHTML); //When the HTML code changes...
        }


    }

    @Override
    public void onDestroy() {
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(importDataServiceResponseReceiver);
        }
        super.onDestroy();
    }




    private void SetTextStatusMessage(String sMessage){
        if(gTextView_ShortStatus != null){
            gTextView_ShortStatus.setText(sMessage);
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

    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String IMPORT_RESPONSE_VIDEO_WEB_DETECT = "com.agcurations.aggallerymanager.intent.action.IMPORT_RESPONSE_VIDEO_WEB_DETECT";

        @Override
        public void onReceive(Context context, Intent intent) {



            //Get boolean indicating that an error may have occurred:
            boolean bError = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_PROBLEM,false);
            if(!bError) {
                ArrayList<String> alsTags;
                alsTags = intent.getStringArrayListExtra(Service_Import.VIDEO_WEB_DATA_TAGS);
                if(alsTags != null) {
                    ItemClass_CatalogItem ci = new ItemClass_CatalogItem();
                    StringBuilder sbTags = new StringBuilder();
                    for(int i = 0; i < alsTags.size(); i++){
                        sbTags.append(alsTags.get(i));
                        if(i < alsTags.size() - 1) {
                            sbTags.append(", ");
                        }
                    }
                    ci.sTags = sbTags.toString();  //Note that these are textual tags and not numeric identifiers.
                    viewModelImportActivity.ci = ci;
                }
            } else {
                String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);

            }

        }
    }


}