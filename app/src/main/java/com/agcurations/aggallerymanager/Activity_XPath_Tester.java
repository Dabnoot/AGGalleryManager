package com.agcurations.aggallerymanager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class Activity_XPath_Tester extends AppCompatActivity {

    GenericReceiver genericReceiver;

    ProgressBar gProgressBar_GeneralPurpose;
    TextView gTextView_ProgressBarText;

    EditText gTextInputEditText_WebAddress;
    EditText gTextInputEditText_XPathExpression;
    Button gButton_TestXPathString;
    ScrollView gScrollView_OutputLog;
    TextView gTextView_OutputLog;

    private WebView gWebView;

    public final MutableLiveData<String> gmLiveDataStringHTML = new MutableLiveData<>(); //Used to assist with move of HTML data out of the webview.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*EdgeToEdge.enable(this);*/
        setContentView(R.layout.activity_xpath_tester);
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });*/

        ActionBar AB = getSupportActionBar();
        if(AB != null) {
            AB.setTitle("XPath Tester");
        }

        //Configure a response receiver to listen for updates from the workers:
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_XPathTester.WORKER_XPATH_TESTER_RESPONSE);
        filter.addAction(Worker_Catalog_DeleteItem.CATALOG_DELETE_ITEM_ACTION_RESPONSE);
        filter.addAction(Worker_CatalogViewer_SortAndFilterGroup.CATALOG_SORT_AND_FILTER_GROUP_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportComicFolders.IMPORT_COMIC_FOLDERS_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportFiles.IMPORT_FILES_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportComicWebFiles.IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
        filter.addAction(Worker_Import_VideoDownload.IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
        filter.addAction(Worker_LocalFileTransfer.IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
        filter.addAction(Worker_Catalog_BackupCatalogDBFiles.CATALOG_DATA_FILE_BACKUP_ACTION_RESPONSE);
        filter.addAction(Worker_Catalog_DeleteMultipleItems.DELETE_MULTIPLE_ITEMS_ACTION_RESPONSE);
        filter.addAction(GlobalClass.BROADCAST_WRITE_CATALOG_FILE);
        filter.addAction(Worker_User_Delete.USER_DELETE_ACTION_RESPONSE);
        filter.addAction(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);
        filter.addAction(Worker_DownloadPostProcessing.DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);
        filter.addAction(GlobalClass.BROADCAST_CATALOG_FILES_MAINTENANCE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        genericReceiver = new GenericReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(genericReceiver,filter);

        gProgressBar_GeneralPurpose = findViewById(R.id.progressBar_GeneralPurpose);
        gTextView_ProgressBarText = findViewById(R.id.textView_ProgressBarText);

        gTextInputEditText_WebAddress = findViewById(R.id.textInputEditText_WebAddress);
        gTextInputEditText_WebAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                //Log.d("test", "onEditorAction: KeyEvent.Keycode = " + keyEvent.getKeyCode());
                String sWebAddress = textView.getText().toString();
                if (!sWebAddress.startsWith("http")) {
                    sWebAddress = "https://" + sWebAddress;
                    textView.setText(sWebAddress);
                }
                gTextInputEditText_WebAddress.clearFocus();
                gWebView.loadUrl(sWebAddress);

                //Hide the keyboard. EditText ActionGo attribute does not hide the keyboard.
                View view = getCurrentFocus();
                if(view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                return true;
            }
        });

        gTextInputEditText_XPathExpression = findViewById(R.id.textInputEditText_XPathExpression);




        gButton_TestXPathString = findViewById(R.id.button_TestXPathString);
        gScrollView_OutputLog = findViewById(R.id.scrollView_OutputLog);
        gTextView_OutputLog = findViewById(R.id.textView_OutputLog);

        gWebView = findViewById(R.id.videoEnabledWebView_webView);
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
                gButton_TestXPathString.setEnabled(true);
                gTextInputEditText_WebAddress.setText(url);
                String sMessage = "[Webpage load complete.]";
                gTextView_OutputLog.setText(sMessage);
            }

        });

        Button button_Go = findViewById(R.id.button_Go);
        button_Go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sWebAddress = gTextInputEditText_WebAddress.getText().toString();
                String sMessage = "[Loading webpage...]";
                gTextView_OutputLog.setText(sMessage);
                gButton_TestXPathString.setEnabled(false);
                gWebView.loadUrl(sWebAddress);
                //Hide the keyboard:
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                gTextInputEditText_XPathExpression.clearFocus();
            }
        });

        final Observer<String> observerStringHTML = new Observer<String>() {
            @Override
            public void onChanged(String sHTML) {
                //Enter here when an assigned String is changed.
                //In particular, we enter here when a web page has finished loading.

                GlobalClass globalClass = (GlobalClass) getApplicationContext();
                if(!globalClass.WaitForObjectReady(GlobalClass.gabHTMLHolderAvailable, 2)){
                    String sMessage;
                    sMessage = "Memory not ready. Please retry.";
                    Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                    return;
                }
                GlobalClass.gabHTMLHolderAvailable.set(false);
                GlobalClass.sWebPageHTML = sHTML;
                GlobalClass.gabHTMLHolderAvailable.set(true);

                String sXPathExpression = "";
                if(gTextInputEditText_XPathExpression != null){
                    sXPathExpression = gTextInputEditText_XPathExpression.getText().toString();
                }

                String sMessage = "[Analyzing HTML...]";
                gTextView_OutputLog.setText(sMessage);
                String sCallerID = "Activity_XPath_Tester:start_Action_HTML_XPath_Test()";
                Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                Data dataHTMLTestXPath = new Data.Builder()
                        .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                        .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                        .putString(Worker_XPathTester.EXTRA_STRING_XPATH_EXPRESSION, sXPathExpression)
                        .build();
                OneTimeWorkRequest otwrHTMLTestXPath = new OneTimeWorkRequest.Builder(Worker_XPathTester.class)
                        .setInputData(dataHTMLTestXPath)
                        .addTag(Worker_XPathTester.TAG_WORKER_XPATH_TESTER) //To allow finding the worker later.
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueue(otwrHTMLTestXPath);

            }
        };
        gmLiveDataStringHTML.observe(this, observerStringHTML); //When the HTML code changes...

        gButton_TestXPathString.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gWebView.loadUrl("javascript:HtmlViewer.showHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
                //Hide the keyboard:
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                gTextInputEditText_XPathExpression.clearFocus();

            }
        });
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(genericReceiver);
        super.onDestroy();
    }



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


    public class GenericReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to update log or progress bar:
                boolean 	bUpdateLog;
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdateLog = intent.getBooleanExtra(GlobalClass.UPDATE_LOG_BOOLEAN,false);
                bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdateLog){
                    String sLogLine;
                    sLogLine = intent.getStringExtra(GlobalClass.LOG_LINE_STRING);
                    if(sLogLine != null) {
                        if (gTextView_OutputLog != null) {
                            gTextView_OutputLog.append(sLogLine);
                            //Execute delayed scroll down since this broadcast listener is not on the UI thread:
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Do something after 100ms
                                    if(gScrollView_OutputLog != null){
                                        gScrollView_OutputLog.fullScroll(View.FOCUS_DOWN);
                                    }
                                }
                            }, 100);
                        }
                    }
                }
                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_GeneralPurpose != null) {
                        gProgressBar_GeneralPurpose.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_ProgressBarText != null) {
                        gTextView_ProgressBarText.setText(sProgressBarText);
                    }
                }

            } //End if not an error message.

        } //End onReceive.

    } //End GenericReceiver.


}