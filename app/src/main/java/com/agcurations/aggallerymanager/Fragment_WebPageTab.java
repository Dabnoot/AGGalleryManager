package com.agcurations.aggallerymanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;


public class Fragment_WebPageTab extends Fragment {

    GlobalClass globalClass;

    public static final String ARG_WEBPAGE_INDEX = "WEBPAGE_INDEX";

    public int giWebPageIndex;

    private WebView gWebView;
    private EditText gEditText_Address;

    public Fragment_WebPageTab(int iWebPageIndex) {
        giWebPageIndex = iWebPageIndex;
    }

    private static final String gsDefaultAddressString = "Search or type web address";

    public String gsWebAddress = "";

    boolean gbInitialized = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() == null){
            //todo: write to log "[fragment] no activity object found".
            return;
        }

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        if (getArguments() != null) {
            giWebPageIndex = getArguments().getInt(ARG_WEBPAGE_INDEX, -1);
        }
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

        if(getView() == null){
            //todo: write to log no view found.
            return;
        }

        gWebView = getView().findViewById(R.id.webView_tabWebView);
        gEditText_Address = getView().findViewById(R.id.editText_Address);

        //Configure the WebView:
        gWebView.setBackgroundColor(Color.BLACK);
        WebSettings webSettings = gWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        final Fragment fParent = this;

        gWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                String sTitle = view.getTitle();
                for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages){
                    int ihashCode = fParent.hashCode();
                    if(ihashCode == icwptd.iTabFragmentHashID){
                        icwptd.sTabTitle = sTitle;
                        Activity_Browser activity_browser = (Activity_Browser) getActivity();
                        if(activity_browser != null){
                            activity_browser.updateSingleTabNotch(icwptd); //Update the tab label.
                            Service_WebPageTabs.startAction_SetWebPageTabData(getContext(), icwptd);
                        }
                        break;
                    }
                }
            };

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                gEditText_Address.setText(url);

                //Update the recorded webpage history for this tab:
                //Find the associated WebPageTabData:
                for(ItemClass_WebPageTabData icwptd: globalClass.gal_WebPages){
                    int ihashCode = fParent.hashCode();
                    if(ihashCode == icwptd.iTabFragmentHashID){
                        if(icwptd.alsAddressHistory == null) {
                            icwptd.alsAddressHistory = new ArrayList<>();
                        }
                        //Add url to address history list for this tab, but first make sure that
                        //  we are not merely re-loading the current address:
                        int iAddressCount = icwptd.alsAddressHistory.size();
                        boolean bSkipSet = false;
                        if(iAddressCount > 0) {
                            String sCurrentAddress = icwptd.alsAddressHistory.get(iAddressCount - 1);
                            if (!url.equals(sCurrentAddress)) {
                                icwptd.alsAddressHistory.add(url);
                            } else {
                                bSkipSet = true;
                            }
                        } else {
                            icwptd.alsAddressHistory.add(url);
                        }
                        if(!bSkipSet) {
                            Service_WebPageTabs.startAction_SetWebPageTabData(getActivity().getApplicationContext(), icwptd);
                        }
                        break;
                    }
                }
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
    }

    private void InitializeData(){
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

    }

    @Override
    public void onResume() {
        super.onResume();
        String sLoadedAddress = gWebView.getUrl();
        if(sLoadedAddress == null && gsWebAddress.equals("")) {
            InitializeData();
        }
    }





}
