package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_TagEditor_4_DeleteTag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_TagEditor_4_DeleteTag extends Fragment {

    private GlobalClass globalClass;
    private ViewModel_TagEditor viewModelTagEditor;

    ListViewTagsAdapter gListViewTagsAdapter;

    TagEditorServiceResponseReceiver tagEditorServiceResponseReceiver;

    Button button_DeleteTag = null;

    public Fragment_TagEditor_4_DeleteTag() {
        // Required empty public constructor
    }

    public static Fragment_TagEditor_4_DeleteTag newInstance() {
        return new Fragment_TagEditor_4_DeleteTag();
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Calling Application class (see application tag in AndroidManifest.xml)
        if(getActivity() != null) {
            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }
        //Instantiate the ViewModel sharing data between fragments:
        viewModelTagEditor = new ViewModelProvider(getActivity()).get(ViewModel_TagEditor.class);

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(TagEditorServiceResponseReceiver.TAG_EDITOR_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        tagEditorServiceResponseReceiver = new TagEditorServiceResponseReceiver();
        //registerReceiver(tagEditorServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(tagEditorServiceResponseReceiver,filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tag_editor_4_delete_tag, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        button_DeleteTag = getView().findViewById(R.id.button_DeleteTag);

    }

    @Override
    public void onResume() {
        super.onResume();
        initComponents();
    }


    @Override
    public void onDestroy() {
        //unregisterReceiver(tagEditorServiceResponseReceiver);
        if(getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(tagEditorServiceResponseReceiver);
        }
        super.onDestroy();
    }

    public void initComponents() {

        if (getView() == null) {
            return;
        }

        if(getActivity() == null){
            return;
        }

        RefreshTagListView();

        if(getView() != null) {
            Button button_DeleteTag = getView().findViewById(R.id.button_DeleteTag);
            button_DeleteTag.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    button_DeleteTag_Click(v);
                }
            });
        }

        if(getView() != null) {
            Button button_Finish = getView().findViewById(R.id.button_Finish);
            button_Finish.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(getActivity() != null) {
                        ((Activity_TagEditor) getActivity()).callForFinish();
                    }
                }
            });
        }
    }

    private void RefreshTagListView(){
        //Populate the listView:
        if (getView() == null) {
            return;
        }
        final ListView listView_TagDelete = getView().findViewById(R.id.listView_TagDelete);

        // Create the adapter for the ListView, and set the ListView adapter:
        if(getActivity() == null){
            return;
        }

        ArrayList<ItemClass_Tag> alict_TagItems = new ArrayList<>();

        //Go through the tags treeMap and put the ListView together:
        for (Map.Entry<Integer, ItemClass_Tag> tmEntryTagReferenceItem : globalClass.gtmApprovedCatalogTagReferenceLists.get(viewModelTagEditor.iTagEditorMediaCategory).entrySet()) {
            alict_TagItems.add(tmEntryTagReferenceItem.getValue());
        }

        //Alphabetize the tags:
        TreeMap<String, ItemClass_Tag> tmICT = new TreeMap<>();
        for(ItemClass_Tag ict: alict_TagItems){
            tmICT.put(ict.sTagText.toLowerCase(Locale.ROOT), ict);
        }
        alict_TagItems.clear();
        for(Map.Entry<String, ItemClass_Tag> tmEntry : tmICT.entrySet()){
            alict_TagItems.add(tmEntry.getValue());
        }

        gListViewTagsAdapter = new ListViewTagsAdapter(getActivity().getApplicationContext(), alict_TagItems);
        listView_TagDelete.setAdapter(gListViewTagsAdapter);
        listView_TagDelete.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if(getView() != null) {
            Button button_DeleteTag = getView().findViewById(R.id.button_DeleteTag);
            button_DeleteTag.setEnabled(false);
        }
    }


    private void button_DeleteTag_Click(View v){
        if (getView() == null) {
            return;
        }

        String sConfirmationMessage = "This action will delete the tag from the database. The tag will be" +
                " removed from all catalog items. The user permissions and maturity rating for each" +
                " catalog item previously holding the tag will be recalculated. If this tag has a" +
                " high maturity rating and all other tags applied to the catalog item have a lower" +
                " maturity, the lower maturity rating will be applied to the catalog item.\n" +
                "If there are no remaining tags to the catalog item, the default maturity will be applied. This could result in" +
                " mature content or content 'currently private to the current user' being exposed to" +
                " inappropriate users, or in the case of this tag being the last tag and of a lower" +
                " maturity than the default, the catalog item may \"disappear\" from view of some" +
                " low maturity users as the higher default maturity is applied. Use the filter feature" +
                " of the Catalog Viewer to determine the content to which this tag has been applied.\n" +
                "Confirm tag deletion: ";
        sConfirmationMessage = sConfirmationMessage + Objects.requireNonNull(gListViewTagsAdapter.getItem(gListViewTagsAdapter.iTagItemSelected)).sTagText;

        if (getActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustomStyle);
        builder.setTitle("Delete Tag");
        builder.setMessage(sConfirmationMessage);
        //builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                DeleteTag();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog adConfirmationDialog = builder.create();
        adConfirmationDialog.show();
        //adConfirmationDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.YELLOW);
    }

    private void DeleteTag(){
        if(getContext() == null) return;
        if(gListViewTagsAdapter == null) return;
        if(gListViewTagsAdapter.getItem(gListViewTagsAdapter.iTagItemSelected) == null) return;
        Toast.makeText(getContext(), "Deleting tag...", Toast.LENGTH_SHORT).show();
        String sTagRecord = GlobalClass.getTagRecordString(Objects.requireNonNull(gListViewTagsAdapter.getItem(gListViewTagsAdapter.iTagItemSelected)));
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataDeleteTag = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Fragment_TagEditor_4_DeleteTag:DeleteTag()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, viewModelTagEditor.iTagEditorMediaCategory)
                .putString(GlobalClass.EXTRA_TAG_TO_BE_DELETED, sTagRecord)
                .build();
        OneTimeWorkRequest otwrDeleteTag = new OneTimeWorkRequest.Builder(Worker_Tags_DeleteTag.class)
                .setInputData(dataDeleteTag)
                .addTag(Worker_Tags_DeleteTag.TAG_WORKER_TAGS_DELETETAG) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(getContext()).enqueue(otwrDeleteTag);

        //todo: Update user permissions and maturity ratings for catalog items that had this tag.
        viewModelTagEditor.bTagDeleted = true;
    }



    public class TagEditorServiceResponseReceiver extends BroadcastReceiver {
        public static final String TAG_EDITOR_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_TAG_EDITOR_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {
            //Errors are checked for in Activity_TagEditor.
            //Check to see if this is a message indicating that a tag deletion is complete:
            boolean bTagDeleteComplete = intent.getBooleanExtra(GlobalClass.EXTRA_TAG_DELETE_COMPLETE,false);
            if(bTagDeleteComplete){
                RefreshTagListView();
                Toast.makeText(context, "Tag deletion complete.", Toast.LENGTH_LONG).show();
            }



        }
    }


    public class ListViewTagsAdapter extends ArrayAdapter<ItemClass_Tag> {

        int iTagItemSelected = -1;


        public ListViewTagsAdapter(Context context, ArrayList<ItemClass_Tag> tagItems) {
            super(context, 0, tagItems);
        }

        @Override @NonNull
        public View getView(final int position, View v, @NonNull ViewGroup parent) {
            // Get the data item for this position
            final ItemClass_Tag tagItem = getItem(position);
            if(tagItem == null){
                return v;
            }
            // Check if an existing view is being reused, otherwise inflate the view
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(R.layout.listview_tag_item_select_tags_fragment, parent, false);
            }
            // Lookup view for data population
            final CheckedTextView checkedTextView_TagText = v.findViewById(R.id.checkedTextView_TagText);
            // Populate the data into the template view using the data object
            String s = tagItem.sTagText;
            checkedTextView_TagText.setText(s);


            //Set the selection state (needed as views are recycled).
            if(getActivity() != null) {
                if (tagItem.bIsChecked) {
                    checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                } else {
                    checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorUnfilledUnselected));
                }
                checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
            }

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View viewClicked) {
                    //Handle changing the checked state:
                    if(iTagItemSelected > -1){
                        //If a tag item is already selected, make the adjustments:
                        ItemClass_Tag tagItemPrevSelected = getItem(iTagItemSelected);
                        if(tagItemPrevSelected != null && (iTagItemSelected != position)){
                            tagItemPrevSelected.bIsChecked = false;
                        }
                        iTagItemSelected = -1;
                    }

                    ItemClass_Tag tagItem_Clicked = getItem(position);

                    if(tagItem_Clicked != null) {
                        tagItem_Clicked.bIsChecked = !tagItem_Clicked.bIsChecked;
                        if (tagItem_Clicked.bIsChecked) {
                            iTagItemSelected = position;
                            if(button_DeleteTag != null){
                                button_DeleteTag.setEnabled(true);
                            }
                        } else {
                            if(button_DeleteTag != null){
                                button_DeleteTag.setEnabled(false);
                            }
                        }
                    }

                    //Tell the listView adapter to redraw everything, thus "unselecting" any previously selected item:
                    notifyDataSetChanged();
                }
            });

            // Return the completed view to render on screen
            return v;
        }


    }





}