package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;


public class Fragment_Import_2b_SelectSingleWebComic extends Fragment {

    ViewModel_ImportActivity viewModelImportActivity;

    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;

    //ItemClass_CatalogItem gciCatalogItem;

    GlobalClass globalClass;

    public Fragment_Import_2b_SelectSingleWebComic() {
        // Required empty public constructor
    }

    public static Fragment_Import_2b_SelectSingleWebComic newInstance() {
        return new Fragment_Import_2b_SelectSingleWebComic();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);

            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(ImportDataServiceResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(importDataServiceResponseReceiver, filter);
        }

    }

    @Override
    public void onDestroy() {
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(importDataServiceResponseReceiver);
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_2b_select_single_web_comic, container, false);
    }


    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() != null) {
            getActivity().setTitle("Confirm Import");
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }
        initComponents();
    }

    public void initComponents(){

        //Init progress:
        if(getView() == null){
            return;
        }

        TextView textView_WebImportDetails = getView().findViewById(R.id.textView_WebImportDetails);
        textView_WebImportDetails.setMovementMethod(new ScrollingMovementMethod());

        if(globalClass.gbImportComicWebAnalysisStarted && !globalClass.gbImportComicWebAnalysisRunning) {
            if (globalClass.isNetworkConnected) {
                textView_WebImportDetails.setText("");
                globalClass.gsbImportComicWebAnalysisLog = new StringBuilder();
                globalClass.gbImportComicWebAnalysisStarted = false;
                globalClass.gbImportComicWebAnalysisRunning = true;//This prevents import from starting again
                // if the activity/fragment is restarted due to an orientation change, etc.
                Service_Import.startActionAcquireNHComicsDetails(getActivity(), viewModelImportActivity.sWebAddress, ImportDataServiceResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
            } else {
                Toast.makeText(getActivity(), "No network connected.", Toast.LENGTH_LONG).show();
            }
        }
        updateViews();
    }




    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String COMIC_DETAILS_DATA_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.COMIC_DETAILS_DATA_ACTION_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_PROBLEM, false);
            if (bError) {
                if(getView() != null) {
                    TextView textView_Title;
                    textView_Title = getView().findViewById(R.id.textView_Title);
                    TextView textView_WebImportDetails;
                    textView_WebImportDetails = getView().findViewById(R.id.textView_WebImportDetails);
                    TextView textView_WebImportDetailsLog;
                    textView_WebImportDetailsLog = getView().findViewById(R.id.textView_WebImportDetailsLog);
                    String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);
                    textView_Title.setVisibility(View.INVISIBLE);
                    textView_WebImportDetails.setVisibility((View.INVISIBLE));
                    textView_WebImportDetailsLog.setVisibility(View.VISIBLE);
                    textView_WebImportDetailsLog.append(sMessage);
                }
            } else {
                updateViews();

            } //End if/else error message/no error message.

        } //End onReceive.

    }

    private void updateViews(){
        if(getView() != null && getActivity() != null) {
            TextView textView_Title;
            textView_Title = getView().findViewById(R.id.textView_Title);
            TextView textView_WebImportDetails;
            textView_WebImportDetails = getView().findViewById(R.id.textView_WebImportDetails);
            TextView textView_WebImportDetailsLog; //For use while the web analysis is occurring.
            textView_WebImportDetailsLog = getView().findViewById(R.id.textView_WebImportDetailsLog);
            ImageView imageView_Thumbnail;
            imageView_Thumbnail = getView().findViewById(R.id.imageView_Thumbnail);


            if (globalClass.gbImportComicWebAnalysisFinished) {
                textView_WebImportDetailsLog.setText("");
                textView_WebImportDetailsLog.setVisibility(View.INVISIBLE);
                textView_Title.setVisibility(View.VISIBLE);
                textView_WebImportDetails.setVisibility((View.VISIBLE));

                textView_Title.setText(globalClass.gci_ImportComicWebItem.sTitle);
                String sComicDetails = "";
                if (!globalClass.gci_ImportComicWebItem.sSource.equals(""))
                    sComicDetails = sComicDetails + "Source: " + globalClass.gci_ImportComicWebItem.sSource + "\n\n";
                String sPages = "" + globalClass.gci_ImportComicWebItem.iComicPages;
                sComicDetails = sComicDetails + sPages + " pages." + "\n\n";

                //Display the required & available space:
                long lSize = globalClass.gci_ImportComicWebItem.lSize;
                String sRequiredSpaceSuffix = "";
                if (lSize == -1) { //-1 if the online connection header for each file did not contain size data.
                    lSize = 800 * 1024 * globalClass.gci_ImportComicWebItem.iComicPages; //Average page uses 800 KB.
                    sRequiredSpaceSuffix = " (estimated)";
                }
                String sRS = GlobalClass.CleanStorageSize(lSize, GlobalClass.STORAGE_SIZE_NO_PREFERENCE);
                String sRequiredSpace = "Required space: " +
                        sRS +
                        sRequiredSpaceSuffix +
                        ".";
                //Get the units from 'required space' to match with 'available space':
                String[] sData = sRS.split(" ");
                String sStorageUnit = GlobalClass.STORAGE_SIZE_NO_PREFERENCE;
                if (sData.length == 2) {
                    sStorageUnit = sData[1];
                }
                //Display the available space:
                long lAvailableStorageSpaceBytes;
                lAvailableStorageSpaceBytes = globalClass.AvailableStorageSpace(getActivity().getApplicationContext(), 1);
                String sAvailableStorageSpace = GlobalClass.CleanStorageSize(lAvailableStorageSpaceBytes, sStorageUnit);
                sComicDetails = sComicDetails + sRequiredSpace + " Available space: " + sAvailableStorageSpace + ".\n\n";

                if (!globalClass.gci_ImportComicWebItem.sComicParodies.equals(""))
                    sComicDetails = sComicDetails + "Parodies: " + globalClass.gci_ImportComicWebItem.sComicParodies + "\n\n";
                if (!globalClass.gci_ImportComicWebItem.sComicCharacters.equals(""))
                    sComicDetails = sComicDetails + "Characters: " + globalClass.gci_ImportComicWebItem.sComicCharacters + "\n\n";
                if (!globalClass.gci_ImportComicWebItem.sTags.equals(""))
                    sComicDetails = sComicDetails + "Tags: " + globalClass.gci_ImportComicWebItem.sTags + "\n\n";
                if (!globalClass.gci_ImportComicWebItem.sComicArtists.equals(""))
                    sComicDetails = sComicDetails + "Artists: " + globalClass.gci_ImportComicWebItem.sComicArtists + "\n\n";
                if (!globalClass.gci_ImportComicWebItem.sComicGroups.equals(""))
                    sComicDetails = sComicDetails + "Groups: " + globalClass.gci_ImportComicWebItem.sComicGroups + "\n\n";
                if (!globalClass.gci_ImportComicWebItem.sComicLanguages.equals(""))
                    sComicDetails = sComicDetails + "Languages: " + globalClass.gci_ImportComicWebItem.sComicLanguages + "\n\n";
                if (!globalClass.gci_ImportComicWebItem.sComicCategories.equals(""))
                    sComicDetails = sComicDetails + "Categories: " + globalClass.gci_ImportComicWebItem.sComicCategories;
                textView_WebImportDetails.setText(sComicDetails);

                //Load the thumbnail:
                RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                        .error(R.drawable.attention_32px_wpagepad);

                Glide.with(getActivity()).load(globalClass.gci_ImportComicWebItem.sComicThumbnailURL).apply(options).into(imageView_Thumbnail);
                imageView_Thumbnail.setVisibility(View.VISIBLE);

                if (getView() != null) {
                    Button button_ImportConfirmation = getView().findViewById(R.id.button_ImportConfirmation);
                    if (button_ImportConfirmation != null) {
                        button_ImportConfirmation.setEnabled(true);
                    }
                }
            } else {
                //If web analysis is not finished...
                imageView_Thumbnail.setImageDrawable(null);
                textView_Title.setVisibility(View.INVISIBLE);
                textView_WebImportDetails.setVisibility((View.INVISIBLE));
                textView_WebImportDetailsLog.setVisibility(View.VISIBLE);
                textView_WebImportDetailsLog.setText(globalClass.gsbImportComicWebAnalysisLog.toString());
            }
        }
    }



}