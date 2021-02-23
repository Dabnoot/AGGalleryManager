package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;


public class Fragment_Import_5a_WebConfirmation extends Fragment {

    ViewModel_ImportActivity viewModelImportActivity;

    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;

    ItemClass_CatalogItem gciCatalogItem;

    GlobalClass globalClass;

    public Fragment_Import_5a_WebConfirmation() {
        // Required empty public constructor
    }

    public static Fragment_Import_5a_WebConfirmation newInstance() {
        return new Fragment_Import_5a_WebConfirmation();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        if(getActivity() != null) {
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
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
        return inflater.inflate(R.layout.fragment_import_5a_web_confirmation, container, false);
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Confirm Import");
        initComponents();
    }

    public void initComponents(){

        //Init progress:
        if(getView() == null){
            return;
        }

        TextView textView_WebImportDetails = getView().findViewById(R.id.textView_WebImportDetails);
        textView_WebImportDetails.setMovementMethod(new ScrollingMovementMethod());


        if(viewModelImportActivity.bWebAddressChanged) {
            if (globalClass.isNetworkConnected) {
                Service_Import.startActionAcquireNHComicsDetails(getActivity(), viewModelImportActivity.sWebAddress);
                viewModelImportActivity.bWebAddressChanged = false;
            } else {
                Toast.makeText(getActivity(), "No network connected.", Toast.LENGTH_LONG).show();
            }
        }

    }




    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String COMIC_DETAILS_DATA_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.COMIC_DETAILS_DATA_ACTION_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;
            TextView textView_Title;
            textView_Title = getView().findViewById(R.id.textView_Title);
            TextView textView_WebImportDetails;
            textView_WebImportDetails = getView().findViewById(R.id.textView_WebImportDetails);
            TextView textView_WebImportDetailsLog;
            textView_WebImportDetailsLog = getView().findViewById(R.id.textView_WebImportDetailsLog);


            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);
                textView_Title.setVisibility(View.INVISIBLE);
                textView_WebImportDetails.setVisibility((View.INVISIBLE));
                textView_WebImportDetailsLog.setVisibility(View.VISIBLE);
                textView_WebImportDetailsLog.append(sMessage);

            } else {

                String sLogMessage;
                sLogMessage = intent.getStringExtra(Service_Import.COMIC_DETAILS_LOG_MESSAGE);
                if(sLogMessage != null){
                    textView_Title.setVisibility(View.INVISIBLE);
                    textView_WebImportDetails.setVisibility((View.INVISIBLE));
                    textView_WebImportDetailsLog.setVisibility(View.VISIBLE);
                    textView_WebImportDetailsLog.append(sLogMessage);
                    return;
                }
                textView_WebImportDetailsLog.setText("");
                textView_WebImportDetailsLog.setVisibility(View.INVISIBLE);
                textView_Title.setVisibility(View.VISIBLE);
                textView_WebImportDetails.setVisibility((View.VISIBLE));

                boolean bComicDetailsDataServiceSuccess;
                bComicDetailsDataServiceSuccess = intent.getBooleanExtra(Service_Import.COMIC_DETAILS_SUCCESS,
                        false);

                String sErrorMessage;
                if(bComicDetailsDataServiceSuccess) {

                    gciCatalogItem = (ItemClass_CatalogItem) intent.getSerializableExtra(Service_Import.COMIC_CATALOG_ITEM);
                    gciCatalogItem.bComic_Online_Data_Acquired = true;

                    Toast.makeText(getActivity(), "Online data acquired.", Toast.LENGTH_SHORT).show();

                    ImageView imageView_Thumbnail;
                    imageView_Thumbnail = getView().findViewById(R.id.imageView_Thumbnail);

                    if(textView_Title != null) {
                        textView_Title.setText(gciCatalogItem.sComicName);
                    }
                    String sComicDetails = "";
                    if(!gciCatalogItem.sSource.equals("")) sComicDetails = sComicDetails + "Source: " + gciCatalogItem.sSource + "\n\n";
                    String sPages = "" + gciCatalogItem.iComicPages;
                    sComicDetails = sComicDetails + sPages + " pages." + "\n\n";

                    //Display the required & available space:
                    long lSize = gciCatalogItem.lSize;
                    String sRequiredSpaceSuffix = "";
                    if(lSize == -1){ //-1 if the online connection header for each file did not contain size data.
                        lSize = 800 * 1024 * gciCatalogItem.iComicPages; //Average page uses 800 KB.
                        sRequiredSpaceSuffix = " (estimated)";
                    }
                    String sRS = GlobalClass.CleanStorageSize(lSize, GlobalClass.STORAGE_SIZE_NO_PREFERENCE);
                    String sRequiredSpace = "Required space: " +
                            sRS +
                            sRequiredSpaceSuffix +
                            ".";
                    //Get the units from 'required space' to match with 'available space':
                    String sData[] = sRS.split(" ");
                    String sStorageUnit = GlobalClass.STORAGE_SIZE_NO_PREFERENCE;
                    if(sData.length == 2) {
                        sStorageUnit = sData[1];
                    }
                    //Display the available space:
                    long lAvailableStorageSpaceBytes;
                    lAvailableStorageSpaceBytes = globalClass.AvailableStorageSpace(getActivity().getApplicationContext(), 1);
                    String sAvailableStorageSpace = GlobalClass.CleanStorageSize(lAvailableStorageSpaceBytes, sStorageUnit);
                    sComicDetails = sComicDetails + sRequiredSpace + " Available space: " + sAvailableStorageSpace + ".\n\n";

                    if(!gciCatalogItem.sComicParodies.equals("")) sComicDetails = sComicDetails + "Parodies: " + gciCatalogItem.sComicParodies + "\n\n";
                    if(!gciCatalogItem.sComicCharacters.equals("")) sComicDetails = sComicDetails + "Characters: " + gciCatalogItem.sComicCharacters + "\n\n";
                    if(!gciCatalogItem.sTags.equals("")) sComicDetails = sComicDetails + "Tags: " + gciCatalogItem.sTags + "\n\n";
                    if(!gciCatalogItem.sComicArtists.equals("")) sComicDetails = sComicDetails + "Artists: " + gciCatalogItem.sComicArtists + "\n\n";
                    if(!gciCatalogItem.sComicGroups.equals("")) sComicDetails = sComicDetails + "Groups: " + gciCatalogItem.sComicGroups + "\n\n";
                    if(!gciCatalogItem.sComicLanguages.equals("")) sComicDetails = sComicDetails + "Languages: " + gciCatalogItem.sComicLanguages + "\n\n";
                    if(!gciCatalogItem.sComicCategories.equals("")) sComicDetails = sComicDetails + "Categories: " + gciCatalogItem.sComicCategories;
                    if(textView_WebImportDetails != null){
                        textView_WebImportDetails.setText(sComicDetails);
                    }

                    //Load the thumbnail:
                    RequestOptions options = new RequestOptions()
                            .centerCrop()
                            .placeholder(R.drawable.baseline_image_white_18dp)
                            .error(R.drawable.attention_32px);

                    Glide.with(getActivity()).load(gciCatalogItem.sComicThumbnailURL).apply(options).into(imageView_Thumbnail);

                    //Preserve this CatalogItem data for other import operations:
                    viewModelImportActivity.ci = gciCatalogItem;

                } //End successful comic data retrieval behavior.

            } //End if/else error message/no error message.

        } //End onReceive.

    }



}