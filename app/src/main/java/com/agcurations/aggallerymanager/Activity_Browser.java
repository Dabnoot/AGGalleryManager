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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class Activity_Browser extends AppCompatActivity {

    public TabLayout tabLayout_WebTabs;
    ViewPager2 viewPager2_WebPages;
    FragmentViewPagerAdapter viewPagerFragmentAdapter;

    GlobalClass globalClass;

    WebPageTabDataServiceResponseReceiver webPageTabDataServiceResponseReceiver;

    boolean bTabsLoaded = false;

    boolean gbWriteApplicationLog = false;
    String gsApplicationLogFilePath = "";

    public static ViewModel_Browser viewModel_browser; //Used to transfer data between fragments.

    public int giBrowserTopBarHeight_Original;
    public RelativeLayout relativeLayout_BrowserTopBar;

    private String[] gsNewTabSequenceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        //Instantiate the ViewModel sharing data between fragments:
        viewModel_browser = new ViewModelProvider(this).get(ViewModel_Browser.class);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        //Hide the Action Bar
        getSupportActionBar().hide();
        //Hide the Status Bar
        WindowInsetsController insetsController = getWindow().getInsetsController();
        insetsController.hide(WindowInsets.Type.statusBars());
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if(gbWriteApplicationLog){
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }
        try {
            ApplicationLogWriter("OnCreate Start, getting application context.");

            globalClass = (GlobalClass) getApplicationContext();

            globalClass.gal_WebPages = new ArrayList<>();

            viewPager2_WebPages = findViewById(R.id.viewPager2_WebPages);
            //Set the number of pages that should be retained to either side of the current page
            // in the view hierarchy in an idle state. Pages beyond this limit will be recreated
            // from the adapter when needed.:
            viewPager2_WebPages.setOffscreenPageLimit(1);

            ApplicationLogWriter("Getting new FragmentViewPagerAdapter.");
            viewPagerFragmentAdapter = new FragmentViewPagerAdapter(getSupportFragmentManager(), getLifecycle(), getApplicationContext());

            // set Orientation in your ViewPager2
            viewPager2_WebPages.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

            viewPager2_WebPages.setAdapter(viewPagerFragmentAdapter);
            ApplicationLogWriter("FragmentViewPagerAdapter assigned to ViewPager.");

            //Stop the user from swiping left and right on the ViewPager (control with Next button):
            viewPager2_WebPages.setUserInputEnabled(false);

            viewPager2_WebPages.setPageTransformer(new ViewPager2.PageTransformer() {
                @Override
                public void transformPage(@NonNull View page, float position) {

                }
            });

            tabLayout_WebTabs = findViewById(R.id.tabLayout_WebTabs);

            relativeLayout_BrowserTopBar = findViewById(R.id.relativeLayout_BrowserTopBar); //Referenced for scrolling the TopBar out of view during WebView scrolldown.

            //Configure a TabLayoutMediator to synchronize the TabLayout and the ViewPager2.
            //AutoRefresh tells the system to recreate all the tabs of the tabLayout if notifyDataSetChanged is called to the viewPager adapter.
            TabLayoutMediator tlm = new TabLayoutMediator(
                    tabLayout_WebTabs,
                    viewPager2_WebPages,
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
                        sharedPreferences.edit()
                                .putInt(GlobalClass.PREF_WEB_TAB_PREV_FOCUS_INDEX, tab.getPosition())
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
                    gsNewTabSequenceHelper = null; //Clear the helper, new tab creation order reset.
                    CreateNewTab("");
                }
            });

            //Configure a response receiver to listen for updates from the Data Service:
            IntentFilter filter = new IntentFilter(WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            webPageTabDataServiceResponseReceiver = new WebPageTabDataServiceResponseReceiver();
            //registerReceiver(importDataServiceResponseReceiver, filter);
            LocalBroadcastManager.getInstance(this).registerReceiver(webPageTabDataServiceResponseReceiver, filter);


            Service_WebPageTabs.startAction_GetWebPageTabData(this);


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

        //icwptd.sAddress = new ArrayList<>();
        if(sAddress != null){
            if(!sAddress.equals("")) {
                //icwptd.sAddress.add(sAddress);
                icwptd.sAddress = sAddress;
            }
        }
        int iNewTabPosition;

        //If a tab is to be inserted, not appended
        if(gsNewTabSequenceHelper != null){
            String sNewTabPostion = gsNewTabSequenceHelper[1];
            iNewTabPosition = Integer.parseInt(sNewTabPostion);
            globalClass.gal_WebPages.add(iNewTabPosition, icwptd); //This action must be done before createFragment (cannot be in SetWebPageData due to race condition)
            viewPagerFragmentAdapter.insertFragment(iNewTabPosition, icwptd.sAddress);   //Call CreateFragment before SetWebPageTabData to get Hash code. SetWebPageTabData will update globalClass.galWebPages, which will wipe the Hash code from memory.
            viewPagerFragmentAdapter.notifyDataSetChanged();
            InitializeTabAppearance();
            //Service_WebPageTabs.startAction_SetWebPageTabData(getApplicationContext(), icwptd);
            Service_WebPageTabs.startAction_GetWebpageTitleFavicon(getApplicationContext(), icwptd); //This routine also calls the same routine as startAction_SetWebPageTabData.

        } else {
            iNewTabPosition = viewPagerFragmentAdapter.getItemCount(); //Put the tab at the end.
            globalClass.gal_WebPages.add(iNewTabPosition, icwptd); //This action must be done before createFragment (cannot be in SetWebPageData due to race condition)
            viewPagerFragmentAdapter.createFragment(iNewTabPosition);   //Call CreateFragment before SetWebPageTabData to get Hash code. SetWebPageTabData will update globalClass.galWebPages, which will wipe the Hash code from memory.
            viewPagerFragmentAdapter.notifyDataSetChanged();
            InitializeTabAppearance();
            Service_WebPageTabs.startAction_SetWebPageTabData(getApplicationContext(), icwptd);
            viewPager2_WebPages.setCurrentItem(iNewTabPosition, false);

        }

    }


    class HandlerOpenLinkInNewTab extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
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
                Log.d("Log FileWriter", e.getMessage());
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
            Service_WebPageTabs.startAction_GetWebPageTabData(this);
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
            if(tabLayout_WebTabs.getTabCount() > globalClass.gal_WebPages.size()){
                this.finish(); //Close this activity if the sizes are out-of-sync.
                return;
            }
            String sTitle = globalClass.gal_WebPages.get(i).sTabTitle;
            if(sTitle.equals("")){
                sTitle = "New Tab";
            }
            /*if(sTitle.length() > 15){
                sTitle = sTitle.substring(0,15) + "...";
            }*/

            RelativeLayout relativeLayout_custom_tab = (RelativeLayout)
                    LayoutInflater.from(getApplicationContext())
                            .inflate(R.layout.custom_tab, null);

            ImageView imageView_Favicon = relativeLayout_custom_tab.findViewById(R.id.imageView_Favicon);
            if(imageView_Favicon != null) {
                String sFaviconAddress = globalClass.gal_WebPages.get(i).sFaviconAddress;
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

                    //If the tab being closed is not the tab which has focus, make sure that the
                    //  focus tab retains focus.
                    int iFocusPosition = viewPager2_WebPages.getCurrentItem();
                    //Make sure tab focus remains on the correct tab:
                    if(iFocusPosition > iPosition){
                        viewPager2_WebPages.setCurrentItem(iFocusPosition - 1, false);
                    }

                    //Perform operations to remove the tab:
                    viewPagerFragmentAdapter.removeItem(iPosition);

                    globalClass.gal_WebPages.remove(iPosition);

                    //Update the tab notch views:
                    InitializeTabAppearance();

                    //Record the new web page tab lineup to the file:
                    Service_WebPageTabs.startAction_RemoveWebPageTabData(getApplicationContext());
                }
            });
            tabLayout_WebTabs.getTabAt(i).setCustomView(relativeLayout_custom_tab);
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

    public void updateSingleTabNotchTitle(int iHashCode){
        //Update tab title only.
        ApplicationLogWriter("updateSingleTabNotchTitle start.");
        int iTabIndex = -1;
        //Find the tab index matching the supplied HashCode.
        for(int i = 0; i < globalClass.gal_WebPages.size(); i++){
            if(iHashCode == globalClass.gal_WebPages.get(i).iTabFragmentHashID){
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
                    String sTitle = globalClass.gal_WebPages.get(iTabIndex).sTabTitle;
                    if (sTitle.equals("")) {
                        sTitle = "New Tab";
                    }
                    textView_TabText.setText(sTitle);
                }
            }
        }
        ApplicationLogWriter("updateSingleTabNotchTitle end.");
    }

    public void updateSingleTabNotchFavicon(int iHashCode, Bitmap bitmap_favicon){ //Todo: Combine with updateSingleTabNotchTitle, but with a flag?
        //Update tab title and favicon.
        ApplicationLogWriter("updateSingleTabNotchFavicon start.");
        int iTabIndex = -1;
        //Find the tab index matching the supplied HashCode.
        for(int i = 0; i < globalClass.gal_WebPages.size(); i++){
            if(iHashCode == globalClass.gal_WebPages.get(i).iTabFragmentHashID){
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
                    String sTitle = globalClass.gal_WebPages.get(iTabIndex).sTabTitle;
                    if (sTitle.equals("")) {
                        sTitle = "New Tab";
                    }
                    textView_TabText.setText(sTitle);
                }

                ImageView imageView_Favicon = view.findViewById(R.id.imageView_Favicon);
                if(imageView_Favicon != null) {
                    if( bitmap_favicon != null) {
                        imageView_Favicon.setImageResource(0);
                        imageView_Favicon.setImageBitmap(bitmap_favicon);
                    } else {
                        String sFaviconAddress = globalClass.gal_WebPages.get(iTabIndex).sFaviconAddress;
                        if(!sFaviconAddress.equals("")){
                            Glide.with(this)
                                    .load(sFaviconAddress)
                                    .into(imageView_Favicon);
                        }

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

                fwp.handlerOpenLinkInNewTab = new HandlerOpenLinkInNewTab();

                alFragment_WebPages.add(fwp);

                //Add the hashCode of the new fragment to the WebPageTabData for tracking.
                //  WebPageTabData must be added before this createFragment routine is called.
                globalClass.gal_WebPages.get(position).iTabFragmentHashID = fwp.hashCode();

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
            fwp.handlerOpenLinkInNewTab = new HandlerOpenLinkInNewTab();
            alFragment_WebPages.add(index, fwp);
            //Add the hashCode of the new fragment to the WebPageTabData for tracking.
            //  WebPageTabData must be added before this createFragment routine is called.
            globalClass.gal_WebPages.get(index).iTabFragmentHashID = fwp.hashCode();
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

    public class WebPageTabDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_WEB_PAGE_TAB_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            //Get boolean indicating that an error may have occurred:
            boolean bError = intent.getBooleanExtra(Service_WebPageTabs.EXTRA_BOOL_PROBLEM,false);

            if (bError) {
                String sMessage = intent.getStringExtra(Service_WebPageTabs.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                String sResultType = intent.getStringExtra(Service_WebPageTabs.EXTRA_RESULT_TYPE);
                if(sResultType != null){
                    if(sResultType.equals(Service_WebPageTabs.RESULT_TYPE_WEB_PAGE_TAB_DATA_ACQUIRED)){
                        //This should only run at Activity start.
                        //Initialize the tabs:
                        GlobalClass globalClass = (GlobalClass) getApplicationContext();
                        for(int i = 0; i < globalClass.gal_WebPages.size(); i++){
                            String sAddress = globalClass.gal_WebPages.get(i).sAddress;
                            if(!sAddress.equals("")) {
                                viewPagerFragmentAdapter.insertFragment(i, globalClass.gal_WebPages.get(i).sAddress);
                            } else {
                                viewPagerFragmentAdapter.createFragment(i);
                            }

                        }

                        viewPagerFragmentAdapter.notifyDataSetChanged();
                        InitializeTabAppearance();

                        //Go to the tab last having the user focus:
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        int iTabofLastFocus = sharedPreferences.getInt(GlobalClass.PREF_WEB_TAB_PREV_FOCUS_INDEX, 0);
                        viewPager2_WebPages.setCurrentItem(iTabofLastFocus, false);

                        bTabsLoaded = true;

                    } else if (sResultType.equals(Service_WebPageTabs.RESULT_TYPE_WEB_PAGE_TAB_CLOSED)){

                    } else if (sResultType.equals(Service_WebPageTabs.RESULT_TYPE_WEB_PAGE_TITLE_AND_FAVICON_ACQUIRED)){
                        String sTabID = intent.getStringExtra(Service_WebPageTabs.EXTRA_WEBPAGE_TAB_DATA_TABID);
                        if(sTabID != null) {
                            //Find the hash for the fragment with the matching tab ID:
                            int iHashCode = 0;
                            for (int i = 0; i < globalClass.gal_WebPages.size(); i++) {
                                if (sTabID.equals(globalClass.gal_WebPages.get(i).sTabID)) {
                                    iHashCode = globalClass.gal_WebPages.get(i).iTabFragmentHashID;
                                    break;
                                }
                            }
                            if(iHashCode != 0) {
                                updateSingleTabNotchFavicon(iHashCode, null);
                            }
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


}

