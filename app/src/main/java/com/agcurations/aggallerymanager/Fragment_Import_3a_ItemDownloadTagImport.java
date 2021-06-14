package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;


public class Fragment_Import_3a_ItemDownloadTagImport extends Fragment {

    public static ViewModel_ImportActivity viewModelImportActivity;

    private int giSelectItemsListViewWidth;

    private Button gbutton_ImportTags;

    AddTagsServiceResponseReceiver addTagsServiceResponseReceiver;

    public Fragment_Import_3a_ItemDownloadTagImport() {
        // Required empty public constructor
    }

    public static Fragment_Import_3a_ItemDownloadTagImport newInstance() {
        return new Fragment_Import_3a_ItemDownloadTagImport();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Instantiate the ViewModel sharing data between fragments:
        if(getActivity() != null) {
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
        }

        //Configure a response receiver to listen for response from Service_TagEditor when adding tags:
        IntentFilter filter = new IntentFilter(AddTagsServiceResponseReceiver.ADD_TAGS_SERVICE_EXECUTE_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        addTagsServiceResponseReceiver = new AddTagsServiceResponseReceiver();
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(addTagsServiceResponseReceiver, filter);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_3a_item_download_tag_import, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gbutton_ImportTags = getView().findViewById(R.id.button_ImportTags);
    }

    @Override
    public void onResume() {
        super.onResume();
        initComponents();
    }

    public void initComponents() {

        PopulateUnidentifiedTagsListView();
        RefreshExistingTagsListView();

    }

    private void RefreshExistingTagsListView(){
        //Populate the tag listView:
        if(getView() == null){
            return;
        }
        final ListView listView_TagViewer = getView().findViewById(R.id.listView_TagViewer);

        GlobalClass globalClass = (GlobalClass) getActivity().getApplicationContext();
        ArrayList<String> alsTags = new ArrayList<>();
        for(Map.Entry<String, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_VIDEOS).entrySet()){
            alsTags.add(entry.getValue().sTagText);
        }

        String[] sTemp = new String[alsTags.size()];
        sTemp = alsTags.toArray(sTemp);
        if(getActivity() == null){
            return;
        }
        ArrayAdapter<String> aasTags = new ArrayAdapter<>(getActivity(), R.layout.listview_tageditor_tagtext, sTemp);
        listView_TagViewer.setAdapter(aasTags);

    }

    private void PopulateUnidentifiedTagsListView(){
        //Populate the new tags listView:
        if(getView() == null){
            return;
        }
        final ListView listView_UnidentifiedTags = getView().findViewById(R.id.listView_UnidentifiedTags);

        //Get the list of supposed tags which do not exist in the tags catalog:
        if(viewModelImportActivity.alfiConfirmedFileImports == null){
            return;
        }
        if(viewModelImportActivity.alfiConfirmedFileImports.size() == 0){
            return;
        }
        //Grab all prospective unidentified tags from all to-be-imported file items:
        ArrayList<String> alsProspectiveUnidentifiedTags = new ArrayList<>();
        for(ItemClass_File icf: viewModelImportActivity.alfiConfirmedFileImports) {
            alsProspectiveUnidentifiedTags.addAll(icf.alsUnidentifiedTags);
        }
        //Weed-out any duplicates:
        TreeMap<String, String> tmProspectiveUTsNoDuplicates = new TreeMap<>();
        for(String sPT: alsProspectiveUnidentifiedTags){
            tmProspectiveUTsNoDuplicates.put(sPT, sPT);
        }
        //Re-form the prospective tags without duplicates:
        alsProspectiveUnidentifiedTags.clear();
        for(Map.Entry<String, String> entry: tmProspectiveUTsNoDuplicates.entrySet()){
            alsProspectiveUnidentifiedTags.add(entry.getKey());
        }

        //Recalculate the list of tags that do not exist:
        GlobalClass globalClass = (GlobalClass) getActivity().getApplicationContext();
        ArrayList<String> alsConfirmedUnidentifiedTags = new ArrayList<>();
        for(String sTag: alsProspectiveUnidentifiedTags){
            String sIncomingTagCleaned = sTag.toLowerCase().trim();
            boolean bTagFound = false;
            for(Map.Entry<String, ItemClass_Tag> TagEntry: globalClass.gtmCatalogTagReferenceLists.get(viewModelImportActivity.iImportMediaCategory).entrySet()){
                String sExistingTagCleaned = TagEntry.getKey().toLowerCase().trim();
                if(sExistingTagCleaned.equals(sIncomingTagCleaned)){
                    bTagFound = true;
                    break;
                }
            }
            if(!bTagFound){
                alsConfirmedUnidentifiedTags.add(sTag.trim());
            }
        }


        giSelectItemsListViewWidth = listView_UnidentifiedTags.getWidth();

        //Create the adapter and assign to the listView:
        TagImportCustomAdapter aasTags = new TagImportCustomAdapter(getActivity(), R.layout.listview_tageditor_tagtext, alsConfirmedUnidentifiedTags);
        listView_UnidentifiedTags.setAdapter(aasTags);

    }




    //==================================
    //===== ListView Adapter ===========
    //==================================

    public class TagImportCustomAdapter extends ArrayAdapter<String> {
        //This class for displaying to user video files found in html.

        final public ArrayList<tagCandidate> alTagCandidates;
        Context contextFromCaller;

        public TagImportCustomAdapter(Context context, int textViewResourceId, ArrayList<String> altc) {
            super(context, textViewResourceId, altc);
            contextFromCaller = context;

            alTagCandidates = new ArrayList<>();

            for(String sTagText: altc){
                alTagCandidates.add(new tagCandidate(sTagText));
            }
            recalcImportButton();

        }

        @Override
        @NonNull
        public View getView(final int position, View v, @NonNull ViewGroup parent) {
            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                //My custom list item design is here
                row = inflater.inflate(R.layout.listview_selectable_1line, parent, false);
            }

            CheckBox cbStorageItemSelect =  row.findViewById(R.id.checkBox_StorageItemSelect);
            TextView tvLine1 =  row.findViewById(R.id.textView_Line1);

            tvLine1.setText(alTagCandidates.get(position).sTagText);


            cbStorageItemSelect.setChecked(alTagCandidates.get(position).bIsChecked);

            //Expand the width of the listItem to the width of the ListView.
            //  This makes it so that the listItem responds to the click even when
            //  the click is off of the text.
            row.setMinimumWidth(giSelectItemsListViewWidth);

            //Set the onClickListener for the row to toggle the checkbox:
            row.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    CheckBox checkBox_StorageItemSelect = view.findViewById(R.id.checkBox_StorageItemSelect);
                    boolean bNewCheckedState = !checkBox_StorageItemSelect.isChecked();
                    checkBox_StorageItemSelect.setChecked(bNewCheckedState);
                    alTagCandidates.get(position).bIsChecked = bNewCheckedState;
                    recalcImportButton();
                }
            });

            //Set the onClickListener for the checkbox to toggle the checkbox:
            CheckBox checkBox_StorageItemSelect = row.findViewById(R.id.checkBox_StorageItemSelect);
            checkBox_StorageItemSelect.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    CheckBox checkBox_StorageItemSelect = (CheckBox) view;
                    alTagCandidates.get(position).bIsChecked = checkBox_StorageItemSelect.isChecked();
                    recalcImportButton();
                }
            });



            return row;
        }

        public void recalcImportButton(){
            boolean bEnabled = false;
            for(tagCandidate tc: alTagCandidates){
                if(tc.bIsChecked){
                    bEnabled = true;
                    break;
                }
            }
            if(gbutton_ImportTags != null) {
                gbutton_ImportTags.setEnabled(bEnabled);
            }

        }

        //To prevent data resetting when scrolled
        @Override
        public int getCount() {
            return alTagCandidates.size();
        }

        @Override
        public String getItem(int position) {
            return alTagCandidates.get(position).sTagText;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

    }

    public static class tagCandidate{
        String sTagText = "";
        boolean bIsChecked = false;

        public tagCandidate(String sTT){
            sTagText = sTT;
        }

    }


    //=============================================
    //======= Response Receiver ===================
    //=============================================

    public  class AddTagsServiceResponseReceiver extends BroadcastReceiver {
        public static final String ADD_TAGS_SERVICE_EXECUTE_RESPONSE = "com.agcurations.aggallerymanager.intent.action.ADD_TAGS_SERVICE_EXECUTE_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;
            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_TagEditor.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_TagEditor.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to tag addition:
                ItemClass_Tag[] itemClass_tags;

                itemClass_tags = (ItemClass_Tag[]) intent.getSerializableExtra(Service_TagEditor.EXTRA_ARRAYLIST_ITEMCLASSTAGS_ADDED_TAGS);

                if(itemClass_tags != null){
                    //Tag addition complete. Update listViews.
                    initComponents(); //This will also recalc the Import button enabled state.
                }


            }

        }
    }




}