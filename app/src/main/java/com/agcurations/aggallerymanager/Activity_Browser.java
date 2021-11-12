package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebIconDatabase;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;

public class Activity_Browser extends AppCompatActivity {

    TabLayout tabLayout_WebTabs;
    ViewPager2 viewPager2_WebPages;
    FragmentViewPagerAdapter viewPagerFragmentAdapter;

    int giFragmentCount = 0;

    GlobalClass globalClass;

    WebPageTabDataServiceResponseReceiver webPageTabDataServiceResponseReceiver;

    boolean bTabsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        getSupportActionBar().hide();

        globalClass = (GlobalClass) getApplicationContext();

        globalClass.gal_WebPages = new ArrayList<>();

        viewPager2_WebPages = findViewById(R.id.viewPager2_WebPages);

        viewPagerFragmentAdapter = new FragmentViewPagerAdapter(getSupportFragmentManager(), getLifecycle(), getApplicationContext());
        // set Orientation in your ViewPager2
        viewPager2_WebPages.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        viewPager2_WebPages.setAdapter(viewPagerFragmentAdapter);


        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        viewPager2_WebPages.setUserInputEnabled(false);

        tabLayout_WebTabs = findViewById(R.id.tabLayout_WebTabs);

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
                if(bTabsLoaded) {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    sharedPreferences.edit()
                            .putInt(GlobalClass.PREF_WEB_TAB_PREV_FOCUS_INDEX, tab.getPosition())
                            .apply();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                /*int iTabNumber = tab.getPosition() + 1;
                Toast.makeText(getApplicationContext(), "tabID " + iTabNumber + " unselected.", Toast.LENGTH_SHORT).show();*/
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                /*int iTabNumber = tab.getPosition() + 1;
                Toast.makeText(getApplicationContext(), "tabID " + iTabNumber + " reselected.", Toast.LENGTH_SHORT).show();*/
            }
        });



        ImageButton imageButton_AddTab = findViewById(R.id.imageButton_AddTab);
        imageButton_AddTab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                giFragmentCount++;
                ItemClass_WebPageTabData icwptd = new ItemClass_WebPageTabData();
                icwptd.iTabIndex = giFragmentCount;
                icwptd.sTabID = GlobalClass.GetTimeStampFileSafe();
                icwptd.alsAddressHistory = new ArrayList<>();
                globalClass.gal_WebPages.add(icwptd); //This action must be done before createFragment (cannot be in SetWebPageData due to race condition)
                viewPagerFragmentAdapter.createFragment(giFragmentCount);   //Call CreateFragment before SetWebPageTabData to get Hash code. SetWebPageTabData will update
                //  globalClass.gal_Webpages, which will wipe the Hash code from memory.
                viewPagerFragmentAdapter.notifyDataSetChanged();
                InitializeTabAppearance();

                Service_WebPageTabs.startAction_SetWebPageTabData(getApplicationContext(), icwptd);

                viewPager2_WebPages.setCurrentItem(icwptd.iTabIndex, false);
            }
        });


        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(WebPageTabDataServiceResponseReceiver.WEB_PAGE_TAB_DATA_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        webPageTabDataServiceResponseReceiver = new WebPageTabDataServiceResponseReceiver();
        //registerReceiver(importDataServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(webPageTabDataServiceResponseReceiver,filter);

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


    }

    //==============================================================================================
    //======= Lifecycle Functions ==================================================================
    //==============================================================================================

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(webPageTabDataServiceResponseReceiver);
        super.onDestroy();
    }

    //==============================================================================================
    //======= Other Functions ======================================================================
    //==============================================================================================

    public void InitializeTabAppearance(){
        //This only updates the tab notch.
        for(int i =0; i<tabLayout_WebTabs.getTabCount(); i++)
        {
            String sTitle = globalClass.gal_WebPages.get(i).sTabTitle;
            if(sTitle.equals("")){
                sTitle = "New Tab";
            }
            if(sTitle.length() > 15){
                sTitle = sTitle.substring(0,15) + "...";
            }

            RelativeLayout relativeLayout_custom_tab = (RelativeLayout)
                    LayoutInflater.from(getApplicationContext())
                            .inflate(R.layout.custom_tab, null);
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

                    giFragmentCount--;
                    int iTabIndex = globalClass.gal_WebPages.get(iPosition).iTabIndex;
                    for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages){
                        if(icwptd.iTabIndex == iTabIndex){

                            icwptd.iTabIndex = -1;

                            //Delete the favicon file, if it was recorded:
                            String sFaviconFilename = icwptd.sFaviconFilename;
                            if(sFaviconFilename != null){
                                if(!sFaviconFilename.equals("")){
                                    Service_WebPageTabs.startAction_DeleteFaviconFile(getApplicationContext(), sFaviconFilename);
                                }
                            }

                        } else if(icwptd.iTabIndex > iTabIndex){
                            icwptd.iTabIndex--;
                        }
                    }

                    if(iTabIndex <= globalClass.gal_WebPages.size()) {
                        globalClass.gal_WebPages.remove(iTabIndex - 1);
                    } else {
                        //If something wierd has happened and the index is beyond the array, remove
                        //  only the end item.
                        globalClass.gal_WebPages.remove(globalClass.gal_WebPages.size() - 1);
                    }

                    //Update the tab notch views:
                    InitializeTabAppearance();

                    //Record the new web page tab lineup to the file:
                    Service_WebPageTabs.startAction_RemoveWebPageTabData(getApplicationContext());
                }
            });

            tabLayout_WebTabs.getTabAt(i).setCustomView(relativeLayout_custom_tab);

        }
    }

    public void updateSingleTabNotch(ItemClass_WebPageTabData itemClass_webPageTabData, Bitmap bitmap_favicon){

        TabLayout.Tab tab = tabLayout_WebTabs.getTabAt(itemClass_webPageTabData.iTabIndex - 1);
        if(tab != null) {
            View view = tab.getCustomView();
            if(view != null) {
                TextView textView_TabText = view.findViewById(R.id.text);
                if (textView_TabText != null) {
                    String sTitle = itemClass_webPageTabData.sTabTitle;
                    if (sTitle.equals("")) {
                        sTitle = "New Tab";
                    }
                    textView_TabText.setText(sTitle);
                }
                ImageView imageView_WebPageIcon = view.findViewById(R.id.imageView_WebPageIcon);
                if(imageView_WebPageIcon != null && bitmap_favicon != null){
                    imageView_WebPageIcon.setImageResource(0);
                    imageView_WebPageIcon.setImageBitmap(bitmap_favicon);
                }
            }
        }
    }

    public void updateAllTabNotches(){

        for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages) {

            TabLayout.Tab tab = tabLayout_WebTabs.getTabAt(icwptd.iTabIndex - 1);
            if (tab != null) {
                View view = tab.getCustomView();
                if (view != null) {
                    TextView textView_TabText = view.findViewById(R.id.text);
                    if (textView_TabText != null) {
                        String sTitle = icwptd.sTabTitle;
                        if (sTitle.equals("")) {
                            sTitle = "New Tab";
                        }
                        textView_TabText.setText(sTitle);
                    }
                }
            }
        }
    }


    //==============================================================================================
    //======= Adapters ======================================================================
    //==============================================================================================

    public static class FragmentViewPagerAdapter extends FragmentStateAdapter {

        GlobalClass globalClass;
        ArrayList<Fragment_WebPageTab> alFragment_WebPages;
        int iFragmentCount = 0;

        public FragmentViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, Context applicationContext) {
            super(fragmentManager, lifecycle);
            globalClass = (GlobalClass) applicationContext;
            alFragment_WebPages = new ArrayList<>();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if(position > iFragmentCount){
                iFragmentCount++;
                Fragment_WebPageTab fwp = new Fragment_WebPageTab(iFragmentCount);
                alFragment_WebPages.add(fwp);

                //Add the hashCode of the new fragment to the WebPageTabData for tracking:
                for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages){
                    if(icwptd.iTabIndex == position){
                        icwptd.iTabFragmentHashID = fwp.hashCode();
                        break;
                    }
                }

                return fwp;
            } else {
                return alFragment_WebPages.get(position);
            }
        }




        @Override
        public int getItemCount() {
            return iFragmentCount;
        }

        @Override
        public long getItemId(int position) {
            //return super.getItemId(position);
            return alFragment_WebPages.get(position).hashCode(); //Required for correct page removal.
        }

        @Override
        public boolean containsItem(long itemId) {
            /*return super.containsItem(itemId);*/
            return alFragment_WebPages.contains(itemId);  //Required for correct page removal.
        }

        public void removeItem(int iPosition){
            alFragment_WebPages.remove(iPosition);
            iFragmentCount--;
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
                        for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages) {
                            giFragmentCount++;
                            viewPagerFragmentAdapter.createFragment(giFragmentCount);
                        }
                        viewPagerFragmentAdapter.notifyDataSetChanged();
                        InitializeTabAppearance();

                        //Go to the tab last having the user focus:
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        int iTabofLastFocus = sharedPreferences.getInt(GlobalClass.PREF_WEB_TAB_PREV_FOCUS_INDEX, 0);
                        viewPager2_WebPages.setCurrentItem(iTabofLastFocus, false);

                        bTabsLoaded = true;

                    } else if (sResultType.equals(Service_WebPageTabs.RESULT_TYPE_WEB_PAGE_TAB_CLOSED)){

                    }
                }


            }


        }
    }


}

