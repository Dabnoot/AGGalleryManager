package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class Activity_Browser extends AppCompatActivity {

    public static final String EXTRA_STRING_WEB_ADDRESS_REQUEST = "com.agcurations.aggallermanager.extra_string_web_address_request";
    private String gsStartupAddressRequest = null;

    public TabLayout tabLayout_WebTabs;
    ViewPager2 gViewPager2_WebPages;
    FragmentViewPagerAdapter viewPagerFragmentAdapter;

    RelativeLayout gRelativeLayout_Progress;

    GlobalClass globalClass;

    BroadcastReceiver_ActivityBrowser webPageTabDataServiceResponseReceiver;

    boolean bTabsLoaded = false;

    boolean gbWriteApplicationLog = false;
    String gsApplicationLogFilePath = "";

    public static ViewModel_Browser viewModel_browser; //Used to transfer data between fragments.

    public int giBrowserTopBarHeight_Original;
    public RelativeLayout gRelativeLayout_BrowserTopBar;

    private String[] gsNewTabSequenceHelper;

    LinearProgressIndicator gProgressIndicator_Progress;
    TextView gTextView_ProgressBarText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        //Instantiate the ViewModel sharing data between fragments:
        viewModel_browser = new ViewModelProvider(this).get(ViewModel_Browser.class);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        //Hide the Status Bar
        WindowInsetsController insetsController = getWindow().getInsetsController();
        if(insetsController != null) {
            insetsController.hide(WindowInsets.Type.statusBars());
        }
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if(gbWriteApplicationLog){
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }

        gProgressIndicator_Progress = findViewById(R.id.progressIndicator_Progress);
        gProgressIndicator_Progress.setMax(100);
        gTextView_ProgressBarText = findViewById(R.id.textView_ProgressBarText);
        if(GlobalClass.gUriDataFolder == null){
            //No storage location has been specified. Tabs will not be loaded.
            gProgressIndicator_Progress.setVisibility(View.INVISIBLE);
            gTextView_ProgressBarText.setVisibility(View.INVISIBLE);
            Toast.makeText(getApplicationContext(),
                    "No data folder selected." +
                            " A storage location may be selected from the Settings menu.",
                    Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(),
                            "Browser will not be able to save open tabs or load previously" +
                            " opened tabs.",
                    Toast.LENGTH_LONG).show();
        }

        try {
            ApplicationLogWriter("OnCreate Start, getting application context.");

            globalClass = (GlobalClass) getApplicationContext();

            GlobalClass.gal_WebPagesForCurrentUser = new ArrayList<>();

            gRelativeLayout_Progress = findViewById(R.id.relativeLayout_Progress);

            gViewPager2_WebPages = findViewById(R.id.viewPager2_WebPages);
            //Set the number of pages that should be retained to either side of the current page
            // in the view hierarchy in an idle state. Pages beyond this limit will be recreated
            // from the adapter when needed.:
            gViewPager2_WebPages.setOffscreenPageLimit(1);

            ApplicationLogWriter("Getting new FragmentViewPagerAdapter.");
            viewPagerFragmentAdapter = new FragmentViewPagerAdapter(getSupportFragmentManager(), getLifecycle(), getApplicationContext());

            // set Orientation in your ViewPager2
            gViewPager2_WebPages.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

            gViewPager2_WebPages.setAdapter(viewPagerFragmentAdapter);
            ApplicationLogWriter("FragmentViewPagerAdapter assigned to ViewPager.");

            //Stop the user from swiping left and right on the ViewPager (control with Next button):
            gViewPager2_WebPages.setUserInputEnabled(false);

            gViewPager2_WebPages.setPageTransformer(new ViewPager2.PageTransformer() {
                @Override
                public void transformPage(@NonNull View page, float position) {

                }
            });

            tabLayout_WebTabs = findViewById(R.id.tabLayout_WebTabs);

            gRelativeLayout_BrowserTopBar = findViewById(R.id.relativeLayout_BrowserTopBar); //Referenced for scrolling the TopBar out of view during WebView scrolldown.

            //Configure a TabLayoutMediator to synchronize the TabLayout and the ViewPager2.
            //AutoRefresh tells the system to recreate all the tabs of the tabLayout if notifyDataSetChanged is called to the viewPager adapter.
            TabLayoutMediator tlm = new TabLayoutMediator(
                    tabLayout_WebTabs,
                    gViewPager2_WebPages,
                    true,
                    false,
                    new TabLayoutMediator.TabConfigurationStrategy() {
                        @Override
                        public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                            tab.setText("New tab");
                        }
                    }
            );
            tlm.attach();



            tabLayout_WebTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    //Record the tab selected by the user so we can go back to that tab when the activity is restarted:
                    if (bTabsLoaded) {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String sPreferenceNamePrefix = GlobalClass.gicuCurrentUser.sUserName;
                        String sPrefWebTabPrevFocusIndexByUser = GlobalClass.PREF_WEB_TAB_PREV_FOCUS_INDEX_PREFIX + sPreferenceNamePrefix;
                        sharedPreferences.edit()
                                .putInt(sPrefWebTabPrevFocusIndexByUser, tab.getPosition())
                                .apply();
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    gsNewTabSequenceHelper = null; //Clear the helper, new tab creation order reset.
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }

            });


            ImageButton imageButton_AddTab = findViewById(R.id.imageButton_AddTab);
            imageButton_AddTab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    int iTabCount = viewPagerFragmentAdapter.getItemCount();
                    if(iTabCount >= GlobalClass.giMaxTabCount) {
                        String sMessage = "Arbitrary tab limit reached at " + iTabCount + " tabs. Close a tab to open a new tab.";
                        Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    gsNewTabSequenceHelper = null; //Clear the helper, new tab creation order reset.
                    Toast.makeText(getApplicationContext(), "New tab (" + (iTabCount + 1) + ")...", Toast.LENGTH_SHORT).show();  //Add one because we are imminently opening a new tab.
                    CreateNewTab("");
                }
            });

            //Configure a response receiver to listen for updates from the Data Service:
            IntentFilter filter = new IntentFilter();
            filter.addCategory(Intent.CATEGORY_DEFAULT);
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
            filter.addAction(Worker_Catalog_Analysis.CATALOG_ANALYSIS_ACTION_RESPONSE);
            filter.addAction(Worker_Browser_GetWebPagePreview.WORKER_BROWSER_GET_WEB_PAGE_PREVIEW_MESSAGE);
            filter.addAction(Worker_Browser_GetWebPageTabData.WORKER_BROWSER_GET_WEB_TAB_DATA_MESSAGE);
            filter.addAction(Worker_Browser_WriteWebPageTabData.WORKER_BROWSER_WRITE_WEB_PAGE_TAB_DATA_MESSAGE);
            webPageTabDataServiceResponseReceiver = new BroadcastReceiver_ActivityBrowser();
            //registerReceiver(importDataServiceResponseReceiver, filter);
            LocalBroadcastManager.getInstance(this).registerReceiver(webPageTabDataServiceResponseReceiver, filter);


            startAction_GetWebPageTabData();

            Intent intentStartingIntent = getIntent();
            if(intentStartingIntent != null){
                String sRequestedAddress = intentStartingIntent.getStringExtra(EXTRA_STRING_WEB_ADDRESS_REQUEST);
                if(sRequestedAddress != null){
                    gsStartupAddressRequest = sRequestedAddress;
                }
            }

            /*Button button_testBrowser = findViewById(R.id.button_testBrowser);
            if(button_testBrowser != null){
                button_testBrowser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        viewPager2_WebPages.setCurrentItem(1,false);
                    }
                });
            }*/

            ApplicationLogWriter("onCreate end.");
        } catch (Exception e){
            ApplicationLogWriter(e.getMessage());
        }

    }




    public void CreateNewTab(String sAddress){
        ItemClass_WebPageTabData icwptd = new ItemClass_WebPageTabData();
        icwptd.sTabID = GlobalClass.GetTimeStampFileSafe();
        icwptd.sUserName = GlobalClass.gicuCurrentUser.sUserName;

        //icwptd.sAddress = new ArrayList<>();
        if(sAddress != null){
            if(!sAddress.equals("")) {
                //icwptd.sAddress.add(sAddress);
                icwptd.sAddress = sAddress;
            }
        }
        int iNewTabPosition;

        //If a tab is to be inserted, not appended (only case is context menu->open link in new tab):
        if(gsNewTabSequenceHelper != null){
            String sNewTabPostion = gsNewTabSequenceHelper[1];
            iNewTabPosition = Integer.parseInt(sNewTabPostion);
            GlobalClass.gal_WebPagesForCurrentUser.add(iNewTabPosition, icwptd); //This action must be done before createFragment (cannot be in SetWebPageData due to race condition)
            viewPagerFragmentAdapter.insertFragment(iNewTabPosition, icwptd.sAddress);   //Call CreateFragment before SetWebPageTabData to get Hash code. SetWebPageTabData will update globalClass.galWebPages, which will wipe the Hash code from memory.
            viewPagerFragmentAdapter.notifyDataSetChanged();
            InitializeTabAppearance();

            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            Data dataBrowserGetWebPagePreview = new Data.Builder()
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                    .putString(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_TABID, icwptd.sTabID)
                    .putString(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_ADDRESS, icwptd.sAddress)
                    .build();
            OneTimeWorkRequest otwrGetWebPagePreview = new OneTimeWorkRequest.Builder(Worker_Browser_GetWebPagePreview.class)
                    .setInputData(dataBrowserGetWebPagePreview)
                    .addTag(Worker_Browser_GetWebPagePreview.TAG_WORKER_BROWSER_GETWEBPAGEPREVIEW) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(otwrGetWebPagePreview);

        } else {
            iNewTabPosition = viewPagerFragmentAdapter.getItemCount(); //Put the tab at the end.
            GlobalClass.gal_WebPagesForCurrentUser.add(iNewTabPosition, icwptd); //This action must be done before createFragment (cannot be in SetWebPageData due to race condition)
            viewPagerFragmentAdapter.createFragment(iNewTabPosition);   //Call CreateFragment before SetWebPageTabData to get Hash code. SetWebPageTabData will update globalClass.galWebPages, which will wipe the Hash code from memory.
            viewPagerFragmentAdapter.notifyDataSetChanged();
            InitializeTabAppearance();
            //Service_WebPageTabs.startAction_SetWebPageTabData(getApplicationContext(), icwptd);
            //Update stored data:
            Activity_Browser.startAction_WriteWebPageTabData(getApplicationContext(), "Activity_Browser: CreateNewTab()");
            gViewPager2_WebPages.setCurrentItem(iNewTabPosition, false);
        }

    }


    class HandlerOpenLinkInNewTab extends Handler {
        public HandlerOpenLinkInNewTab(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            int iTabCount = viewPagerFragmentAdapter.getItemCount();
            int iTabWarningCount = (int) Math.floor((double) GlobalClass.giMaxTabCount * .90);
            if(iTabCount >= GlobalClass.giMaxTabCount) {
                String sMessage = "Arbitrary tab limit reached at " + iTabCount + " tabs.\nClose a tab to open a new tab.";
                Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                return;
            } else if(iTabCount >= iTabWarningCount) {
                String sMessage = (iTabCount + 1) + " tabs loaded."; //Add one because we are imminently opening a new tab.
                Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
            }

            String sURL = msg.getData().getString("url");
            String sTabID = msg.getData().getString("tabID"); //The TabID for the tab calling for the opening of a link in a new tab.
            //Open the new tab immediately after the current tab, or after a new prior tab.
            int iNewTabPosition;
            if(gsNewTabSequenceHelper != null){
                String sLastNewTabPostion = gsNewTabSequenceHelper[1];
                int iLastNewTabPosition = Integer.parseInt(sLastNewTabPostion);
                iNewTabPosition = iLastNewTabPosition + 1;
            } else {
                //Determine current tab position:
                int iCurrentTabPosition = tabLayout_WebTabs.getSelectedTabPosition();
                iNewTabPosition = iCurrentTabPosition + 1;
            }
            String sNewTabPosition = String.valueOf(iNewTabPosition);
            gsNewTabSequenceHelper = new String[]{sTabID, sNewTabPosition};  //Initialize helper with location for new tab.
            CreateNewTab(sURL);
        }
    }

    private void ApplicationLogWriter(String sMessage){
        if(gbWriteApplicationLog){
            try {
                File fLog = new File(gsApplicationLogFilePath);
                FileWriter fwLogFile = new FileWriter(fLog, true);
                fwLogFile.write(GlobalClass.GetTimeStampReadReady() + ": " + this.getLocalClassName() + ", " + sMessage + "\n");
                fwLogFile.close();
            } catch (Exception e) {
                Log.d("Log FileWriter", "" + e.getMessage());
            }
        }

    }

    //==============================================================================================
    //======= Lifecycle Functions ==================================================================
    //==============================================================================================

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(webPageTabDataServiceResponseReceiver);
        super.onDestroy();
    }


    /*@Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if(gbWriteApplicationLog){
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }
        ApplicationLogWriter("onStart.");
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if(gbWriteApplicationLog){
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }
        ApplicationLogWriter("onResume.");
    }

    /*@Override
    protected void onPause() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if(gbWriteApplicationLog){
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }
        ApplicationLogWriter("onPause.");
        super.onPause();
    }*/

    @Override
    protected void onRestart() {
        super.onRestart();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if(gbWriteApplicationLog){
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }
        if(!bTabsLoaded) {
            startAction_GetWebPageTabData();
        }
        ApplicationLogWriter("onRestart.");
    }



    /*@Override
    protected void onStop() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if(gbWriteApplicationLog){
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }
        super.onStop();
    }*/


    //==============================================================================================
    //======= Other Functions ======================================================================
    //==============================================================================================
    int giTabWidth = -1;
    int giWindowWidth = -1;
    public void InitializeTabAppearance(){
        //This only updates the tab notch.
        ApplicationLogWriter("InitializeTabAppearance start.");
        for(int i =0; i<tabLayout_WebTabs.getTabCount(); i++)
        {
            if(tabLayout_WebTabs.getTabCount() > GlobalClass.gal_WebPagesForCurrentUser.size()){
                this.finish(); //Close this activity if the sizes are out-of-sync.
                return;
            }
            String sTitle = GlobalClass.gal_WebPagesForCurrentUser.get(i).sTabTitle;
            if(sTitle.equals("")){
                sTitle = "New Tab";
            }

            RelativeLayout relativeLayout_custom_tab = (RelativeLayout)
                    LayoutInflater.from(getApplicationContext())
                            .inflate(R.layout.custom_tab, gRelativeLayout_BrowserTopBar, false);


            ImageView imageView_Favicon = relativeLayout_custom_tab.findViewById(R.id.imageView_Favicon);
            if(imageView_Favicon != null) {
                String sFaviconAddress = GlobalClass.gal_WebPagesForCurrentUser.get(i).sFaviconAddress;
                if(!sFaviconAddress.equals("")){
                    Glide.with(this)
                            .load(sFaviconAddress)
                            .into(imageView_Favicon);
                }
            }

            TextView textView_TabText = relativeLayout_custom_tab.findViewById(R.id.text);
            textView_TabText.setText(sTitle);

            ImageButton imageButton_Close = relativeLayout_custom_tab.findViewById(R.id.imageButton_Close);
            final int iPosition = i;
            imageButton_Close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Tab closing...
                    Toast.makeText(getApplicationContext(), "Closing tab...", Toast.LENGTH_SHORT).show();
                    //If the tab being closed is not the tab which has focus, make sure that the
                    //  focus tab retains focus.
                    int iFocusPosition = gViewPager2_WebPages.getCurrentItem();
                    int iMaxPosition = iFocusPosition;
                    if(gViewPager2_WebPages.getAdapter() != null){
                        iMaxPosition = gViewPager2_WebPages.getAdapter().getItemCount() - 1;
                    }
                    //Make sure tab focus remains on the correct tab:
                    if(iFocusPosition != iPosition){
                        if(iFocusPosition < iPosition){
                            gViewPager2_WebPages.setCurrentItem(iFocusPosition, false);
                        } else {
                            gViewPager2_WebPages.setCurrentItem(iFocusPosition - 1, false);
                        }
                    } else {
                        //Select the tab that was before the one being closed, if such a tab exists:
                        if(iFocusPosition > 0 && iFocusPosition != iMaxPosition){
                            gViewPager2_WebPages.setCurrentItem(iFocusPosition - 1, false);
                        }
                    }

                    //Perform operations to remove the tab:
                    viewPagerFragmentAdapter.removeItem(iPosition);

                    GlobalClass.gal_WebPagesForCurrentUser.remove(iPosition);

                    //Update the tab notch views:
                    InitializeTabAppearance();

                    //Record the new web page tab lineup to the file:
                    Activity_Browser.startAction_WriteWebPageTabData(getApplicationContext(), "Activity_Browser: imageButton_Close.onClick()");

                    //Wake the newly-visible tab:
                    int iSelectedTab = tabLayout_WebTabs.getSelectedTabPosition();
                    if(iSelectedTab > -1) {
                        viewPagerFragmentAdapter.alFragment_WebPages.get(iSelectedTab).onResume();
                    }

                }
            });
            TabLayout.Tab tt = tabLayout_WebTabs.getTabAt(i);
            if(tt != null) {
                tt.setCustomView(relativeLayout_custom_tab);
            }
        }
        //Scroll to center the selected tab:
        int iSelectedTabPosition = tabLayout_WebTabs.getSelectedTabPosition();
        if(giTabWidth < 0){
            giTabWidth = (int) convertDpToPx(getApplicationContext(), 200); //200dp default
        }
        if(giWindowWidth < 0){
            giWindowWidth = getResources().getDisplayMetrics().widthPixels;
        }
        int iScrollXMax = tabLayout_WebTabs.getTabCount() * giTabWidth - giWindowWidth;
        iScrollXMax = Math.max(0, iScrollXMax); //Don't let the max scroll go negative.
        int iDistanceToStartOfSelectedTab = giTabWidth * iSelectedTabPosition;
        int iScrollXToCenterTabInWindow = iDistanceToStartOfSelectedTab + (giTabWidth / 2) - (giWindowWidth / 2);
        int iScrollX = Math.min(Math.max(0, iScrollXToCenterTabInWindow), iScrollXMax);
        tabLayout_WebTabs.setScrollX(iScrollX);
        ApplicationLogWriter("InitializeTabAppearance end.");
    }


    /**
     * Call to update a tab notch title only.
     * @param iHashCode Hash code identifying a tab.
     * @param sTitle Title to display on the tab notch.
     */
    public void resetSingleTabNotchTitle(int iHashCode, String sTitle){
        int iTabIndex = -1;
        //Find the tab index matching the supplied HashCode.
        for(int i = 0; i < GlobalClass.gal_WebPagesForCurrentUser.size(); i++){
            if(iHashCode == GlobalClass.gal_WebPagesForCurrentUser.get(i).iTabFragmentHashID){
                iTabIndex = i;
                break;
            }
        }
        if(iTabIndex == -1){
            return;
        }

        TabLayout.Tab tab = tabLayout_WebTabs.getTabAt(iTabIndex);
        if(tab != null) {
            View view = tab.getCustomView();
            if(view != null) {
                TextView textView_TabText = view.findViewById(R.id.text);
                if (textView_TabText != null) {
                    textView_TabText.setText(sTitle);
                }
            }
        }
    }

    /**
     * Call to update a tab notch with a loading icon and temporary title.
     * @param iHashCode Hash code identifying a tab.
     * @param sTitle Title to display on the tab notch.
     */
    public void resetSingleTabNotchFavicon(int iHashCode, String sTitle){

        int iTabIndex = -1;
        //Find the tab index matching the supplied HashCode.
        for(int i = 0; i < GlobalClass.gal_WebPagesForCurrentUser.size(); i++){
            if(iHashCode == GlobalClass.gal_WebPagesForCurrentUser.get(i).iTabFragmentHashID){
                iTabIndex = i;
                break;
            }
        }
        if(iTabIndex == -1){
            return;
        }

        TabLayout.Tab tab = tabLayout_WebTabs.getTabAt(iTabIndex);
        if(tab != null) {
            View view = tab.getCustomView();
            if(view != null) {
                TextView textView_TabText = view.findViewById(R.id.text);
                if (textView_TabText != null) {
                    textView_TabText.setText(sTitle);
                }

                ImageView imageView_Favicon = view.findViewById(R.id.imageView_Favicon);
                if(imageView_Favicon != null) {
                    imageView_Favicon.setImageResource(R.drawable.new_tab_icon);
                }
            }
        }

    }

    /**
     * Updates a tab notch with data found in the corresponding tab instance identified by iHashCode.
     * @param iHashCode Hash code identifying a tab.
     */
    public void updateSingleTabNotch(int iHashCode){
        //Update tab title and favicon.
        ApplicationLogWriter("updateSingleTabNotchFavicon start.");
        int iTabIndex = -1;
        //Find the tab index matching the supplied HashCode.
        for(int i = 0; i < GlobalClass.gal_WebPagesForCurrentUser.size(); i++){
            if(iHashCode == GlobalClass.gal_WebPagesForCurrentUser.get(i).iTabFragmentHashID){
                iTabIndex = i;
                break;
            }
        }
        if(iTabIndex == -1){
            return;
        }

        TabLayout.Tab tab = tabLayout_WebTabs.getTabAt(iTabIndex);
        if(tab != null) {
            View view = tab.getCustomView();
            if(view != null) {
                TextView textView_TabText = view.findViewById(R.id.text);
                if (textView_TabText != null) {
                    String sTitle = GlobalClass.gal_WebPagesForCurrentUser.get(iTabIndex).sTabTitle;
                    if (sTitle.equals("")) {
                        sTitle = "New Tab";
                    }
                    textView_TabText.setText(sTitle);
                }

                ImageView imageView_Favicon = view.findViewById(R.id.imageView_Favicon);
                if(imageView_Favicon != null) {
                    String sFaviconAddress = GlobalClass.gal_WebPagesForCurrentUser.get(iTabIndex).sFaviconAddress;
                    if(!sFaviconAddress.equals("")){
                        Glide.with(this)
                                .load(sFaviconAddress)
                                .into(imageView_Favicon);
                    }
                }
            }
        }
        ApplicationLogWriter("updateSingleTabNotchFavicon end.");
    }


    //==============================================================================================
    //======= Adapters ======================================================================
    //==============================================================================================

    public class FragmentViewPagerAdapter extends FragmentStateAdapter {

        GlobalClass globalClass;
        ArrayList<Fragment_WebPageTab> alFragment_WebPages;

        public FragmentViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, Context applicationContext) {
            super(fragmentManager, lifecycle);
            globalClass = (GlobalClass) applicationContext;
            alFragment_WebPages = new ArrayList<>();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            //'position' is zero-based. The routine is called twice for each fragment. Once by the coder's code to create a fragment, again by the FragmentStateAdapter to load and run the fragment.
            if(position > alFragment_WebPages.size() - 1){

                Fragment_WebPageTab fwp = new Fragment_WebPageTab();

                fwp.handlerOpenLinkInNewTab = new HandlerOpenLinkInNewTab(Looper.getMainLooper());

                alFragment_WebPages.add(fwp);

                //Add the hashCode of the new fragment to the WebPageTabData for tracking.
                //  WebPageTabData must be added before this createFragment routine is called.
                GlobalClass.gal_WebPagesForCurrentUser.get(position).iTabFragmentHashID = fwp.hashCode();

                return fwp;
            } else {
                //If the FragmentStateAdapter is calling for a recreation of an existing fragment,
                //  return the fragment from the array:
                return alFragment_WebPages.get(position);
            }
        }

        public void insertFragment(int index, String sURL) {
            //InsertFragment is only used for inserting a new tab from a long-press on a hyperlink in the fragment's webView.
            Fragment_WebPageTab fwp = new Fragment_WebPageTab(sURL);
            fwp.handlerOpenLinkInNewTab = new HandlerOpenLinkInNewTab(Looper.getMainLooper());
            alFragment_WebPages.add(index, fwp);
            //Add the hashCode of the new fragment to the WebPageTabData for tracking.
            //  WebPageTabData must be added before this createFragment routine is called.
            GlobalClass.gal_WebPagesForCurrentUser.get(index).iTabFragmentHashID = fwp.hashCode();
        }




        @Override
        public int getItemCount() {
            return alFragment_WebPages.size();
        }

        @Override
        public long getItemId(int position) {
            //return super.getItemId(position);
            int iHashCode = 0;
            Fragment_WebPageTab fragment_webPageTab = alFragment_WebPages.get(position);
            if(fragment_webPageTab != null){
                iHashCode = fragment_webPageTab.hashCode();
            }
            return iHashCode; //Required for correct page removal.
        }

        @Override
        public boolean containsItem(long itemId) {
            //return super.containsItem(itemId);
            boolean bItemFound = false;
            for(Fragment_WebPageTab fwpt: alFragment_WebPages){
                long lHashCode = fwpt.hashCode();
                if(itemId == lHashCode){
                    bItemFound = true;
                    break;
                }
            }

            return bItemFound;  //Required for correct page removal.
        }

        public void removeItem(int iPosition){
            alFragment_WebPages.remove(iPosition);
            notifyDataSetChanged();
        }


    }

    //==============================================================================================
    //======= Broadcast Receivers===================================================================
    //==============================================================================================

    public class BroadcastReceiver_ActivityBrowser extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            //Get boolean indicating that an error may have occurred:
            boolean bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);

            if (bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                String sResultType = intent.getStringExtra(GlobalClass.EXTRA_RESULT_TYPE_WEB_PAGE_TAB_MESSAGE);
                if(sResultType != null){
                    if(sResultType.equals(Worker_Browser_GetWebPageTabData.RESULT_TYPE_WEB_PAGE_TAB_DATA_ACQUIRED)){

                        //Initialize the tabs:

                        //Lookup the last tab of focus:
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String sPreferenceNamePrefix = GlobalClass.gicuCurrentUser.sUserName;
                        String sPrefWebTabPrevFocusIndexByUser = GlobalClass.PREF_WEB_TAB_PREV_FOCUS_INDEX_PREFIX + sPreferenceNamePrefix;
                        int iTabofLastFocus = sharedPreferences.getInt(sPrefWebTabPrevFocusIndexByUser, 0);

                        //Also check to see if this activity was requested
                        // to be opened by a link the user clicked on an existing catalog item:
                        if(gsStartupAddressRequest != null){
                            ItemClass_WebPageTabData icwptd = new ItemClass_WebPageTabData();
                            icwptd.sTabID = GlobalClass.GetTimeStampFileSafe();
                            icwptd.sUserName = GlobalClass.gicuCurrentUser.sUserName;
                            icwptd.sAddress = gsStartupAddressRequest;
                            int iNewTabPosition = GlobalClass.gal_WebPagesForCurrentUser.size();
                            GlobalClass.gal_WebPagesForCurrentUser.add(iNewTabPosition, icwptd);
                            iTabofLastFocus = iNewTabPosition;
                            gsStartupAddressRequest = null;
                        }

                        int iTabCount = GlobalClass.gal_WebPagesForCurrentUser.size();
                        for(int i = 0; i < iTabCount; i++){
                            //NO PROGRESS BAR TO BE IMPLEMENTED HERE.
                            //  This onReceive blocks the UI thread, and so no progress bar drawing
                            //  will occur.
                            String sAddress = GlobalClass.gal_WebPagesForCurrentUser.get(i).sAddress;
                            if(!sAddress.equals("")) {
                                viewPagerFragmentAdapter.insertFragment(i, GlobalClass.gal_WebPagesForCurrentUser.get(i).sAddress);
                            } else {
                                viewPagerFragmentAdapter.createFragment(i);
                            }
                        }

                        viewPagerFragmentAdapter.notifyDataSetChanged();
                        InitializeTabAppearance();

                        //Go to the tab last having the user focus:
                        gViewPager2_WebPages.setCurrentItem(iTabofLastFocus, false);

                        //Shrink the progressbar:
                        if(gRelativeLayout_Progress != null){
                            ViewGroup.LayoutParams layoutParams = gRelativeLayout_Progress.getLayoutParams();
                            layoutParams.height = 0;
                            gRelativeLayout_Progress.setLayoutParams(layoutParams);
                        }

                        bTabsLoaded = true;

                        int iTabWarningCount = (int) Math.floor((double) GlobalClass.giMaxTabCount * .90);
                        if(iTabCount >= iTabWarningCount) {
                            String sMessage = iTabCount + " tabs loaded.";
                            Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                        }

                    } else if (sResultType.equals(Worker_Browser_GetWebPagePreview.RESULT_TYPE_WEB_PAGE_TITLE_AND_FAVICON_ACQUIRED)){
                        String sTabID = intent.getStringExtra(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_TABID);
                        if(sTabID != null) {
                            //Find the hash for the fragment with the matching tab ID:
                            int iHashCode = 0;
                            int iTabID = -1;
                            for (int i = 0; i < GlobalClass.gal_WebPagesForCurrentUser.size(); i++) {
                                if (sTabID.equals(GlobalClass.gal_WebPagesForCurrentUser.get(i).sTabID)) {
                                    iHashCode = GlobalClass.gal_WebPagesForCurrentUser.get(i).iTabFragmentHashID;
                                    iTabID = i;
                                    break;
                                }
                            }
                            //Update memory:
                            String sTitle = intent.getStringExtra(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_TITLE);
                            String sFaviconAddress = intent.getStringExtra(GlobalClass.EXTRA_WEBPAGE_TAB_DATA_FAVICON_ADDRESS);
                            GlobalClass.gal_WebPagesForCurrentUser.get(iTabID).sTabTitle = sTitle;
                            GlobalClass.gal_WebPagesForCurrentUser.get(iTabID).sFaviconAddress = sFaviconAddress;

                            //Write data to storage file:
                            Activity_Browser.startAction_WriteWebPageTabData(getApplicationContext(), "Activity_Browser: WebPageTabDataServiceResponseReceiver().TITLE_AND_FAVICON_ACQUIRED");

                            //Update views:
                            if(iHashCode != 0) {
                                updateSingleTabNotch(iHashCode);
                            }
                        }


                    }

                }//If result type != null

                //Check to see if this is a response to update log or progress bar:
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(gProgressIndicator_Progress != null && gTextView_ProgressBarText != null) {
                    if (bUpdatePercentComplete) {
                        int iAmountComplete;
                        iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                        if (gProgressIndicator_Progress != null) {
                            gProgressIndicator_Progress.setProgress(iAmountComplete);
                        }
                        if (iAmountComplete == 100) {
                            //Shrink the progressbar:
                            if(gRelativeLayout_Progress != null){
                                ViewGroup.LayoutParams layoutParams = gRelativeLayout_Progress.getLayoutParams();
                                layoutParams.height = 0;
                                gRelativeLayout_Progress.setLayoutParams(layoutParams);
                            }

                        } else {
                            //Expand the progressbar:
                            if(gRelativeLayout_Progress != null){
                                ViewGroup.LayoutParams layoutParams = gRelativeLayout_Progress.getLayoutParams();
                                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                gRelativeLayout_Progress.setLayoutParams(layoutParams);
                            }
                        }

                    }
                    if (bUpdateProgressBarText) {
                        String sProgressBarText;
                        sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                        if (gTextView_ProgressBarText != null) {
                            gTextView_ProgressBarText.setText(sProgressBarText);
                        }
                    }
                }
            }


        }
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public float convertDpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }


    public void startAction_GetWebPageTabData() {
        OneTimeWorkRequest otwrGetWebPageTabData = new OneTimeWorkRequest.Builder(Worker_Browser_GetWebPageTabData.class)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(otwrGetWebPageTabData);
    }

    public static void startAction_WriteWebPageTabData(Context context, String sCallerID) {

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataBrowserWriteWebPageTabData = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrWriteWebPageTabData = new OneTimeWorkRequest.Builder(Worker_Browser_WriteWebPageTabData.class)
                .setInputData(dataBrowserWriteWebPageTabData)
                .addTag(Worker_Browser_WriteWebPageTabData.TAG_WORKER_BROWSER_WRITEWEBPAGETABDATA) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrWriteWebPageTabData);
    }

}

