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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;


public class Fragment_WebPageTab extends Fragment {

    GlobalClass globalClass;

    private VideoEnabledWebView gWebView;
    private WebViewClient gWebViewClient = null;

    private TextInputEditText gEditText_Address;

    public String gsWebAddress = "";
    public String gsMatchingCatalogItemID = "";

    ArrayList<String> gals_ResourceRequests;

    boolean gbWriteApplicationLog = false;
    String gsApplicationLogFilePath = "";

    public static ViewModel_Browser viewModel_browser; //Used to transfer data between fragments.

    Activity_Browser.HandlerOpenLinkInNewTab handlerOpenLinkInNewTab;

    private int giThisFragmentHashCode = 0;

    public RelativeLayout gRelativeLayout_WebViewNavigation;

    private ImageButton gImageButton_Back;
    private ImageButton gImageButton_Forward;

    ImageButton gImageButton_ImportContent;

    private ArrayList<ItemClass_WebComicDataLocator> galWebComicDataLocators;
    private boolean gbWebpageAnalysisRetry = true; //This is to re-trigger an analysis in the event that a page did not load fully before "onPageFinished" was triggered.

    private String gsCustomDownloadPrompt = "";
    private final int CUSTOM_DOWNLOAD_OPTION_NO_TO_HALT = 1;
    private final int CUSTOM_DOWNLOAD_OPTION_YES_IMPORT_TO_COLLECTION = 3;
    private int giCustomDownloadOptions = CUSTOM_DOWNLOAD_OPTION_NO_TO_HALT;
    private String gsPageHTML = "";
    private LinearProgressIndicator gLinearProgressIndicator_DLInspection;

    private int giMediaCategory = -1;

    private boolean gbFaviconAddressFound = false;

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
        filter.addAction(Worker_Import_ImportComicWebFiles.IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
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

    Context gContext;
    @Override
    public void onAttach(@NonNull Context context) {
        gContext = context;
        super.onAttach(context);
    }

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

        gLinearProgressIndicator_DLInspection = getView().findViewById(R.id.linearProgressIndicator_DLInspection);

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

        gWebChromeClient.setOnFaviconReceived(new VideoEnabledWebChromeClient.FaviconReceivedCallback() {
            @Override
            public void faviconReceived(Bitmap icon) {
                //Though I have coded this, I choose not to use it because it appears to load an inferior
                //  icon compared to what might be found on some webpage's link to favicon.ico.
            }
        });

        gWebChromeClient.setOnTitleReceived(new VideoEnabledWebChromeClient.TitleReceivedCallback() {
            @Override
            public void titleReceived(String sTitle) {
                Activity_Browser activity_browser = (Activity_Browser) getActivity();
                if(activity_browser != null){
                    activity_browser.resetSingleTabNotchTitle(giThisFragmentHashCode, sTitle);
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
        gWebView.addJavascriptInterface(new JavaScriptInterface_Custom(), "Custom_Android_Interface");

        gRelativeLayout_WebViewNavigation = getView().findViewById(R.id.relativeLayout_WebViewNavigation);

        View.OnTouchListener view_OnTouchListener = new View.OnTouchListener() {

            private final float fInitialGarbage = -10000000;
            float fLPY = fInitialGarbage;
            float fLPDeltaY = fInitialGarbage;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();

                int action = event.getActionMasked();

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

                    fLPY = event.getY();
                    if(
                            ((fDeltaY < 0 && fLPDeltaY > 0) || (fDeltaY > 0 && fLPDeltaY < 0))){
                        //Abort jitter mode induced by the movement of the view produced by this code when the top bars are height-adjusted at slow speed.
                        fLPDeltaY = fDeltaY;
                        return true;
                    }

                    fLPDeltaY = fDeltaY;

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
                String sRequestedAddress = textView.getText().toString();
                if (!sRequestedAddress.startsWith("http")) {
                    sRequestedAddress = "https://" + sRequestedAddress;
                    textView.setText(sRequestedAddress);
                }
                gEditText_Address.clearFocus();

                String sRequestedDomain = getDomainFromAddress(sRequestedAddress);
                if(!gsWebAddress.startsWith(sRequestedDomain)){
                    gbFaviconAddressFound = false;
                    //Reset the favicon and tab title while we wait for the webpage to load:
                    Activity_Browser activity_browser = (Activity_Browser) getActivity();
                    if(activity_browser != null) {
                        String sTitle = getDomainShortName(sRequestedDomain);
                        activity_browser.resetSingleTabNotchFavicon(giThisFragmentHashCode, sTitle);
                    }
                }
                gsWebAddress = sRequestedAddress;
                gWebView.loadUrl(sRequestedAddress);

                if (getActivity() != null) {
                    //Hide the keyboard. EditText ActionGo attribute does not hide the keyboard.
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(viewFragment.getWindowToken(), 0);
                }
                return true;
            }
        });


        gImageButton_ImportContent = getView().findViewById(R.id.imageButton_ImportContent);
        if (gImageButton_ImportContent != null) {
            gImageButton_ImportContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Paste the current URL to the internal clipboard:

                    GlobalClass.gsBrowserAddressClipboard = gsWebAddress;

                    GlobalClass.giSelectedCatalogMediaCategory = giMediaCategory; //Sometimes know the media category if the site was properly detected.

                    if(gsCustomDownloadPrompt.equals("")) {
                        //Send the user to the Import Activity:
                        Intent intentImportGuided = new Intent(getActivity(), Activity_Import.class);
                        startActivity(intentImportGuided);

                    } else {
                        //Else, if there is a custom prompt, show it to the user and then perform their need:
                        AlertDialog.Builder builder = new AlertDialog.Builder(gContext, R.style.AlertDialogCustomStyle);
                        builder.setTitle("Import");
                        builder.setMessage(gsCustomDownloadPrompt);

                        builder.setPositiveButton("Yes", (dialog, id) -> {
                            dialog.dismiss();
                            if(GlobalClass.gabImportExecutionRunning.get()) {
                                Toast.makeText(gContext, "An import operation is currently in progress. Please wait for completion and reinitiate this import.", Toast.LENGTH_SHORT).show();
                            } else {

                                if (giCustomDownloadOptions == CUSTOM_DOWNLOAD_OPTION_YES_IMPORT_TO_COLLECTION) {
                                    //The user has been prompted that this item is believed to be a member to a collection,
                                    //  and they clicked 'Yes' to import it to be a member of that collection.
                                    //  Determine the associated WebComicDataLocator:
                                    for (ItemClass_WebComicDataLocator icWCDL : galWebComicDataLocators) {
                                        if (icWCDL.sAddress.equals(gsWebAddress) && icWCDL.alicf_ComicDownloadFileItems != null) {

                                            initiateComicGroupItemImport(icWCDL);

                                            break;

                                        } //End if icWCDL.sAddress.equals(gsWebAddress).

                                    } //End loop throu galWebComicDataLocators.

                                } else {

                                    //If the user is here due to clicking 'Yes' despite an item already existing in the catalog,
                                    //  send the user to the Import Activity:
                                    Intent intentImportGuided = new Intent(getActivity(), Activity_Import.class);
                                    startActivity(intentImportGuided);
                                }

                            } //End if/else structure for (GlobalClass.gabImportExecutionRunning.get()).
                        });
                        builder.setNegativeButton("No", (dialog, id) -> {
                            dialog.dismiss();
                            if(giCustomDownloadOptions == CUSTOM_DOWNLOAD_OPTION_YES_IMPORT_TO_COLLECTION) {
                                //If the user has clicked "No" to option of importing a collection item to the catalog,
                                // send the user to the Import Activity (otherwise they will have clicked outside the alert dialog, thus cancelling):
                                Intent intentImportGuided = new Intent(getActivity(), Activity_Import.class);
                                startActivity(intentImportGuided);
                            }
                        });

                        AlertDialog adConfirmationDialog = builder.create();
                        adConfirmationDialog.show();

                    } //End 'if there is a custom download prompt set'. This is for recognized catalog items.

                } //End onClick for the Import button.

            }); //End gImageButton_ImportContent.setOnClickListener.

        } //End if (gImageButton_ImportContent != null).

        ImageButton imageButton_OpenWebPageTabMenu = getView().findViewById(R.id.imageButton_OpenWebPageTabMenu);
        if(imageButton_OpenWebPageTabMenu != null){
            imageButton_OpenWebPageTabMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenuImport = new PopupMenu(getContext(), view);
                    popupMenuImport.inflate(R.menu.web_page_menu);
                    popupMenuImport.getMenu().getItem(0).setChecked(GlobalClass.gbAutoDownloadGroupComics);
                    popupMenuImport.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            if(menuItem.getItemId() == R.id.icon_toggle_auto_group_item_import) {
                                GlobalClass.gbAutoDownloadGroupComics = !GlobalClass.gbAutoDownloadGroupComics;
                            }
                            if(menuItem.getItemId() == R.id.icon_retry_processing) {
                                gWebView.loadUrl("javascript:Custom_Android_Interface.showHTML" +
                                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');"); //This will trigger an observable, which may complete after the code below.
                            }
                            return false;
                        }
                    });
                    popupMenuImport.show();
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

                if(!gsWebAddress.equals(sAddress)){
                    Toast.makeText(gContext, "Start address not the same as finished address.", Toast.LENGTH_SHORT).show();
                }

                //Trigger get of html for processing.
                //Use a delay to start the processing to ensure everything is truly loaded.
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(gWebView != null) {
                            gWebView.loadUrl("javascript:Custom_Android_Interface.showHTML" +
                                    "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');"); //This will trigger an observable, which may complete after the code below.
                        }
                    }
                }, GlobalClass.giAutoProcessingWaitTimeMS);

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

            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                gals_ResourceRequests = new ArrayList<>();
                gbWebpageAnalysisRetry = true; //Allow a one-shot analysis retry if necessary.
                gEditText_Address.setText(url);


                //Check to see if the favicon needs to be updated:
                String sRequestedDomain = getDomainFromAddress(url);
                if(!gsWebAddress.startsWith(sRequestedDomain)){
                    gbFaviconAddressFound = false;
                    //Reset the favicon and tab title while we wait for the webpage to load:
                    Activity_Browser activity_browser = (Activity_Browser) getActivity();
                    if(activity_browser != null) {
                        String sTitle = getDomainShortName(sRequestedDomain);
                        activity_browser.resetSingleTabNotchFavicon(giThisFragmentHashCode, sTitle);
                    }
                }


                //Set color of the download icon to be grey:
                SetDownloadButtonColor(NORMAL);

                //Update the recorded webpage history for this tab:
                //Find the associated WebPageTabData:
                for (int i = 0; i < GlobalClass.gal_WebPagesForCurrentUser.size(); i++) {
                    ItemClass_WebPageTabData icwptd = GlobalClass.gal_WebPagesForCurrentUser.get(i);
                    if (giThisFragmentHashCode == icwptd.iTabFragmentHashID) {
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
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {

                //Give the user a message if they are leaving the host area, such as for an ad redirect:


                //Determine the base host address:
                String sHostAddress = getDomainFromAddress(gsWebAddress);

                //Determine if navigation should continue regardless:
                String sRequestedAddress = request.getUrl().toString();
                if(sHostAddress.equals("") ||
                        sRequestedAddress.startsWith(sHostAddress)){
                    if(sHostAddress.equals("")){
                        gbFaviconAddressFound = false; //If this is a new navigation, note no favicon yet.
                        //Reset the favicon and tab title while we wait for the webpage to load:
                        Activity_Browser activity_browser = (Activity_Browser) getActivity();
                        if(activity_browser != null) {
                            String sDomain = getDomainFromAddress(sRequestedAddress);
                            String sTitle = getDomainShortName(sDomain);
                            activity_browser.resetSingleTabNotchFavicon(giThisFragmentHashCode, sTitle);
                        }
                    }
                    return super.shouldOverrideUrlLoading(webView, request);
                }

                //Determine the host for the new page for reporting to the user:
                String sNewHostAddress = getDomainFromAddress(sRequestedAddress);;

                String sConfirmationMessage = "Web page tab is attempting to navigate to a new URL. Would you like to allow this navigation?\nNew host: " + sNewHostAddress;
                AlertDialog.Builder builder = new AlertDialog.Builder(webView.getContext(), R.style.AlertDialogCustomStyle);
                builder.setTitle("Tab Navigation");
                builder.setMessage(sConfirmationMessage);
                builder.setPositiveButton("Yes", (dialog, id) -> {
                    dialog.dismiss();
                    String sRequestedUrl = request.getUrl().toString();
                    webView.loadUrl(sRequestedUrl);
                    gbFaviconAddressFound = false;
                    //Reset the favicon and tab title while we wait for the webpage to load:
                    Activity_Browser activity_browser = (Activity_Browser) getActivity();
                    if(activity_browser != null) {
                        String sDomain = getDomainFromAddress(sRequestedUrl);
                        String sTitle = getDomainShortName(sDomain);
                        activity_browser.resetSingleTabNotchFavicon(giThisFragmentHashCode, sTitle);
                    }
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
    class JavaScriptInterface_Custom {

        @JavascriptInterface
        public void showHTML(String html) {
            gmLiveDataStringHTML.postValue(html);
        }

    }

    private void ConfigureHTMLWatcher(){

        final Observer<String> observerStringHTML = new Observer<>() {
            @Override
            public void onChanged(String sHTML) {
                //Enter here when an assigned String is changed.
                //In particular, we enter here when a web page has finished loading.

                gsPageHTML = sHTML;

                //Find the favicon address:
                int iFoundLinkLocationStart = 0;
                int iFoundLinkLocationEnd;
                String sFaviconAddress = "";

                if(!gbFaviconAddressFound){
                    String sDomain = getDomainFromAddress(gsWebAddress);
                    if(sDomain.endsWith("/")){
                       sDomain = sDomain.substring(0, sDomain.length() - 2);
                    }
                    int iFoundFavStringStart = sHTML.indexOf("favicon.ico", iFoundLinkLocationStart + 1);
                    if(iFoundFavStringStart > 0){
                        int iStartDQuote = 0;
                        //Locate the start of the link:
                        String sSubString;

                        for(iFoundLinkLocationStart = iFoundFavStringStart; iFoundLinkLocationStart > 0; iFoundLinkLocationStart--){
                            sSubString = sHTML.substring(iFoundLinkLocationStart, iFoundLinkLocationStart + 1);
                            if (sSubString.equals("\"")){
                                break;
                            }
                        }
                        iFoundLinkLocationEnd = sHTML.indexOf("\"", iFoundLinkLocationStart + 1);
                        if(iFoundLinkLocationEnd > 0){
                            sFaviconAddress = sHTML.substring(iFoundLinkLocationStart + 1, iFoundLinkLocationEnd);
                        }
                        if(sFaviconAddress.startsWith("/")){
                            sFaviconAddress = sFaviconAddress.substring(1);
                        }
                        try {
                            if(!sFaviconAddress.startsWith("http")){ //Sometimes the favicon address will be given relative to the domain.
                                                                     //  If this is the case, concatenate the two.
                                sFaviconAddress = sDomain + "/" + sFaviconAddress;
                            }
                        } catch (Exception ignored){

                        }
                    } else {
                        //If no "favicon.ico" entry was found, try the default...
                        sFaviconAddress = sDomain + "/" + "favicon.ico";
                    }
                    gbFaviconAddressFound = true;

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
                        activity_browser.updateSingleTabNotch(giThisFragmentHashCode); //Update the tab label.

                    }

                }


                //========================
                //== Pre-import checks
                //===========

                //Check to see if this page has been analyzed before in current memory, and if so,
                //  don't try to re-analyze.


                gsCustomDownloadPrompt = "";
                giCustomDownloadOptions = CUSTOM_DOWNLOAD_OPTION_NO_TO_HALT;
                gsMatchingCatalogItemID = "";
                giMediaCategory = -1;

                //Check to see if this is a comic site and if the user has downloaded a related comic.
                //  If so, prep to allow the user to download and group the comic.
                galWebComicDataLocators = GlobalClass.getComicWebDataKeys();
                //Evaluate if an address matches a pattern:
                ItemClass_WebComicDataLocator icWCDL_Match = null;
                for(ItemClass_WebComicDataLocator icWCDL: galWebComicDataLocators) {
                    String sNonExplicitAddress = icWCDL.sHostnameRegEx;
                    String sRegexExpression = sNonExplicitAddress.replace("%", "");
                    if (gsWebAddress.matches(sRegexExpression)) {
                        icWCDL.bHostNameMatchFound = true;
                        icWCDL.sAddress = gsWebAddress;  //Passing this item here so that this icWCDL can be passed to a processing worker,
                        //  and those results passed back and identified by the address.
                        icWCDL_Match = icWCDL;
                        break;
                    }
                }
                if(icWCDL_Match == null){
                    //If no matching domain alignment was found, return. That is, if this is an unrecognized website, return.
                    return;
                }

                if(gsPageHTML.equals("")){
                    return;
                }
                icWCDL_Match.sHTML = gsPageHTML;
                icWCDL_Match.dDateTimeStampDataLocated = GlobalClass.GetTimeStampDouble(); //Assign, as soon as possible, a date/time stamp to indicate when the html data was acquired.
                                                                                           // This relates to links that may expire within an unknown duration. Duration to expect expiration
                                                                                           //  should probably be user-configurable.

                //If we are here, this is a comic import site.
                giMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;

                //Get the ID of the comic SERIES from this website, if possible.
                //  Comic series' usually have a site address that includes an ID for the series.
                //Get a potential comic series ID here. Do it before checking for comic duplication.
                //  Do it before the comic duplication check, because comic duplication and
                //  series membership can be done at the same time.
                String sComicSeriesIDStartString = icWCDL_Match.sComicSeriesIDStartString.replace("%", ""); //Remove obfuscating chars.
                String sComicSeriesID = "";
                if (!sComicSeriesIDStartString.equals("")) {
                    //If data has been provided internally to search for a series...
                    if(gsWebAddress.startsWith(sComicSeriesIDStartString)){
                        //If we are here, then this website contains a comic that can be downloaded.
                        //  Set the download button color.
                        SetDownloadButtonColor(READY);
                        //If this item appears to be a potential comic series entry. Check the ID.
                        sComicSeriesID = gsWebAddress.substring(sComicSeriesIDStartString.length());
                        int iLastSlashIndex = sComicSeriesID.indexOf("/");
                        if(iLastSlashIndex > 0) {
                            sComicSeriesID = sComicSeriesID.substring(0, sComicSeriesID.indexOf("/"));
                        } else {
                            //We do not recognize this format webaddress, therefore leave the routine.
                            //  No match will be found within the catalog.
                            return;
                        }
                    }
                }

                //Look to see if this item is already located in the catalog..
                //  Move this check to a worker if it takes too long.
                boolean bItemInCatalog = false;
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
                    if(sCatalogItem_Address.equals(gsWebAddress)){      //Check every item to ensure no exact match.
                        bItemInCatalog = true;
                        //Data for this web page has already been acquired.
                        SetDownloadButtonColor(DUPLICATE);
                    }

                    //Determine if this address is part of a collection:
                    if(gsMatchingCatalogItemID.equals("")) { //Only find a matching comic series item once to grab group ID and tags.
                        if (!sComicSeriesID.equals("")) {
                            //If data has been provided internally to search for a series...
                            if (sCatalogItem_Address.startsWith(sComicSeriesIDStartString)) {
                                //If this item appears to be a potential comic series entry. Get the catalog item ID. It doesn't have to match the chapter, just any member of the collection.
                                sCatalogItemWebComicID = sCatalogItem_Address.substring(sComicSeriesIDStartString.length());
                                sCatalogItemWebComicID = sCatalogItemWebComicID.substring(0, sCatalogItemWebComicID.indexOf("/"));
                                if (sComicSeriesID.equals(sCatalogItemWebComicID)) {
                                    gsMatchingCatalogItemID = entry.getKey();
                                    icWCDL_Match.bRecognizedSeries = true;
                                }
                            }
                        }
                    }
                }

                if(bItemInCatalog){
                    gsCustomDownloadPrompt = "This comic exists in the catalog. Would you like to proceed with the import activity?";
                    giCustomDownloadOptions = CUSTOM_DOWNLOAD_OPTION_NO_TO_HALT;
                    SetDownloadButtonColor(DUPLICATE);
                    return;
                }

                //Reaching this point is very quick. ~0.06s with 1200 catalog items.
                if(!gsMatchingCatalogItemID.equals("")){

                    gsCustomDownloadPrompt = "This comic has been identified as belonging to a collection in the catalog.\n" +
                            "Title: " + Objects.requireNonNull(GlobalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).get(gsMatchingCatalogItemID)).sTitle + "\n" +
                            "Would you like to add this item to the collection, applying the same group ID and tags?";
                    giCustomDownloadOptions = CUSTOM_DOWNLOAD_OPTION_YES_IMPORT_TO_COLLECTION;
                }

                //At this point, the addressed item's address matches a domain and a given format
                // used to recognize downloadable comic candidates. Further, the item is not a
                // duplicate. The item may belong to a collection.

                //Fire off a worker to get the comic details:
                String sDataRecordKey = GlobalClass.getNewCatalogRecordID(); //Not actually getting a new catalog item ID, just using it to generate a unique ID for data tagging.
                if(!globalClass.WaitForObjectReady(GlobalClass.gabComicWebAnalysDataTMAvailable, 1)){
                    Toast.makeText(gContext, "Web data transfer unavailble.", Toast.LENGTH_SHORT).show();
                    return;
                }
                GlobalClass.gabComicWebAnalysDataTMAvailable.set(false);
                //Add data to a feeder for the worker. Data must be transfered. Storing it in a static, ungrowing global is unsafe,
                //  depending on how fast the system might attempt to do it.

                GlobalClass.gtmComicWebDataLocators.put(sDataRecordKey, icWCDL_Match);
                GlobalClass.gabComicWebAnalysDataTMAvailable.set(true);

                String sCallerID = "Fragment_WebPageTab:WebViewClient.onPageFinished()";
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
                WorkManager.getInstance(gContext).enqueue(otwrComicAnalyzeHTML);

                if( gLinearProgressIndicator_DLInspection != null){
                    gLinearProgressIndicator_DLInspection.setVisibility(View.VISIBLE);
                    gLinearProgressIndicator_DLInspection.setProgress(0);
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

                boolean bRecoverableAnalysisError = intent.getBooleanExtra(Worker_Import_ComicAnalyzeHTML.EXTRA_STRING_ANALYSIS_ERROR_RECOVERABLE, false);
                if(bRecoverableAnalysisError && gbWebpageAnalysisRetry){
                    gbWebpageAnalysisRetry = false;
                    gWebView.loadUrl("javascript:Custom_Android_Interface.showHTML" +
                            "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');"); //This will trigger an observable to check the background html.
                }

            } else {

                //Check to see if this is a response to request to get comic downloads from html:
                boolean bGetComicDownloadsResponse = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_GET_WEB_COMIC_ANALYSIS_RESPONSE, false);
                if (bGetComicDownloadsResponse) {

                    //Look to see if this is a progress message:


                    //Look to see if this is a response to a known item to this web tab:
                    String sDataRelatedURLAddress = intent.getStringExtra(GlobalClass.EXTRA_STRING_WEB_ADDRESS);
                    if(sDataRelatedURLAddress != null && galWebComicDataLocators != null) { //todo: why does galWebComicDataLocators sometimes appear as Null?
                        for (ItemClass_WebComicDataLocator icWCDL : galWebComicDataLocators) {
                            if (icWCDL.sAddress.matches(sDataRelatedURLAddress)) {
                                icWCDL.alicf_ComicDownloadFileItems = (ArrayList<ItemClass_File>) intent.getSerializableExtra(GlobalClass.EXTRA_AL_GET_WEB_COMIC_ANALYSIS_RESPONSE);
                                //Set color of the download icon to be blue:
                                SetDownloadButtonColor(RECOGNIZED);
                                //Toast.makeText(getContext(), "Success detecting matching comic and images.", Toast.LENGTH_SHORT).show();
                                gLinearProgressIndicator_DLInspection.setProgress(0);
                                gLinearProgressIndicator_DLInspection.setVisibility(View.INVISIBLE);

                                if(GlobalClass.gbAutoDownloadGroupComics && icWCDL.bRecognizedSeries){
                                    //If autodownload is on and this item is from a recognized series,
                                    //  initiate download. The series check is here because the system
                                    //  was recognizing comics from other sites that did not belong to a group and was just
                                    //  straight-up downloading them immediately.
                                    initiateComicGroupItemImport(icWCDL);
                                }


                                break;
                            }
                        }
                    }

                }

                boolean bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                if (bUpdatePercentComplete) {
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if(gLinearProgressIndicator_DLInspection != null) {
                        gLinearProgressIndicator_DLInspection.setProgress(iAmountComplete);
                        if (iAmountComplete == 100) {
                            gLinearProgressIndicator_DLInspection.setVisibility(View.INVISIBLE);
                        } else {
                            gLinearProgressIndicator_DLInspection.setVisibility(View.VISIBLE);
                        }
                    }
                }

                //Check to see if it is for this web address and
                // change the download icon color to indicate duplicate. This means that the user started an import from the
                // browser and it is now complete.
                boolean bNewCatItemCreated = intent.getBooleanExtra(Worker_Import_ImportComicWebFiles.EXTRA_BOOLEAN_NEW_CAT_ITEM_CREATED,false);
                if(bNewCatItemCreated) {
                    String sCatalogItem_Address;
                    for (Map.Entry<String, ItemClass_CatalogItem>
                            entry : GlobalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()) {

                        if (!entry.getValue().alsApprovedUsers.contains(GlobalClass.gicuCurrentUser.sUserName)) {
                            //Don't notify the user that there are existing, matching catalog items if those items
                            //  are not approved for this user.
                            continue;
                        }

                        sCatalogItem_Address = entry.getValue().sSource;
                        if (sCatalogItem_Address.equals(gsWebAddress)) {      //Check every item to ensure no exact match.
                            SetDownloadButtonColor(DUPLICATE);
                            break;
                        }
                    }
                }

            }


        }

    } //End Broadcast Receiver class definition.

    private final int NORMAL = 1;
    private final int READY = 2;      //Belongs to a domain to which this program is adapted for downloads
    private final int RECOGNIZED = 3; //Recognized as part of a group of comics, ready for download.
    private final int DUPLICATE = 4;  //Item exists in catalog.
    private void SetDownloadButtonColor(int iColor){
        int iColorInt;
        if(iColor == NORMAL) {
            iColorInt = ContextCompat.getColor(gContext, R.color.color_download_normal);
        } else if(iColor == READY){
            iColorInt = ContextCompat.getColor(gContext, R.color.color_download_ready);
        } else if(iColor == RECOGNIZED){
            iColorInt = ContextCompat.getColor(gContext, R.color.color_download_recognized);
        } else {
            iColorInt = ContextCompat.getColor(gContext, R.color.color_download_duplicate);
        }
        //Set color of the download icon to be grey:
        Drawable d1 = AppCompatResources.getDrawable(gContext, R.drawable.download);
        if (d1 == null) {
            return;
        }
        Drawable drawable = d1.mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(iColorInt, PorterDuff.Mode.SRC_IN));
        if (gImageButton_ImportContent != null) {
            gImageButton_ImportContent.setImageDrawable(drawable);
        }

    }

    private void initiateComicGroupItemImport(ItemClass_WebComicDataLocator icWCDL){
        //Set the destination for all file items:
        GlobalClass.AssignDestinationFolders(icWCDL.alicf_ComicDownloadFileItems, giMediaCategory);

        //Copy the group ID, tags and other data from the found catalog match:
        ItemClass_CatalogItem icci_Match = GlobalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).get(gsMatchingCatalogItemID);
        if (icci_Match != null && icWCDL.alicf_ComicDownloadFileItems.size() > 0) {
            ItemClass_File icf_zero = icWCDL.alicf_ComicDownloadFileItems.get(0);
            if (icf_zero.aliProspectiveTags != null) {
                if (icf_zero.aliProspectiveTags.size() == 0) {
                    icf_zero.aliProspectiveTags = icci_Match.aliTags;
                }
            }
            if (!icci_Match.sGroupID.equals("")) {
                icf_zero.sGroupID = icci_Match.sGroupID;
            } else {
                //If there is no group ID, create one and assign it.
                String sGroupID = GlobalClass.getNewGroupID();
                icci_Match.sGroupID = sGroupID;
                icf_zero.sGroupID = sGroupID;
                ItemClass_CatalogItem icci_Match2 = GlobalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).get(gsMatchingCatalogItemID);
                //todo: confirm that a new group ID sticks, and then delete the above line (assuming pass-by-reference works).
                Toast.makeText(gContext, "New group ID generated. New item will be grouped with matching catalog item.", Toast.LENGTH_SHORT).show();
                globalClass.CatalogDataFile_UpdateCatalogFile(giMediaCategory, "Saving...");
                //todo: confirm that a new group ID sticks.
            }
        }


        //Set the data needed by the worker:
        String sDataRecordKey = GlobalClass.getNewCatalogRecordID(); //Not actually getting a new catalog item ID, just using it to generate a unique ID for data tagging.
        if (!globalClass.WaitForObjectReady(GlobalClass.gabImportFileListTMAvailable, 1)) {
            Toast.makeText(getContext(), "Data transfer to worker unavailble. Operation halted.", Toast.LENGTH_SHORT).show();
            return;
        }
        GlobalClass.gabImportFileListTMAvailable.set(false);
        //Add data to a feeder for the worker. Data must be transfered. Storing it in a static, ungrowing global is unsafe,
        //  depending on how fast the system might attempt to do it.
        GlobalClass.gtmalImportFileList.put(sDataRecordKey, new ArrayList<>(icWCDL.alicf_ComicDownloadFileItems));//Transfer to globalClass to avoid transaction limit.
        GlobalClass.gabImportFileListTMAvailable.set(true);

        GlobalClass.gsbImportExecutionLog = new StringBuilder();
        GlobalClass.gabImportExecutionRunning.set(true);

        String sCallerID = "Fragment_WebPageTab.[ImportToCollection]";
        double dTimeStamp = GlobalClass.GetTimeStampDouble();
        //Start the webcomic import worker:
        Data dataImportComicWebFiles = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(Worker_Import_ImportComicWebFiles.EXTRA_STRING_IMPORT_FILES_LOCATOR_AL_KEY, sDataRecordKey)
                .putString(GlobalClass.EXTRA_STRING_WEB_ADDRESS, icWCDL.sAddress)
                .build();
        OneTimeWorkRequest otwrImportComicWebFiles = new OneTimeWorkRequest.Builder(Worker_Import_ImportComicWebFiles.class)
                .setInputData(dataImportComicWebFiles)
                .addTag(Worker_Import_ImportComicWebFiles.TAG_WORKER_IMPORT_IMPORTCOMICWEBFILES) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(gContext).enqueue(otwrImportComicWebFiles);
    }

    /**
     *
     * @param sAddress
     * @return String of host domain including http:// or https://.
     */
    private String getDomainFromAddress(String sAddress){
        String sDomain = "";
        int iCharIndexOfDomainStart = sAddress.indexOf("://");
        if(iCharIndexOfDomainStart > 0){
            iCharIndexOfDomainStart += 3;
            int iCharIndexOfDomainEnd = sAddress.indexOf("/", iCharIndexOfDomainStart);
            if(iCharIndexOfDomainEnd > 0){
                sDomain = sAddress.substring(0,iCharIndexOfDomainEnd);
            } else {
                sDomain = sAddress;
            }
        }
        return sDomain;
    }

    public String getDomainShortName(String sDomain){
        String sDomainShortName = "";

        if(!sDomain.equals("")){

            if(sDomain.contains("://")){
                sDomainShortName = sDomain.substring(sDomain.indexOf("://") + 3);
            }
            if(sDomainShortName.startsWith("www.")){
                sDomainShortName = sDomainShortName.substring(4);
            }
        }

        return  sDomainShortName;
    }



}



