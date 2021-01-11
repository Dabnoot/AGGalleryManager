package com.agcurations.aggallerymanager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

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

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public Fragment_TagEditor_4_DeleteTag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Fragment_TagEditor_3_EditDeleteTag.
     */
    // TODO: Rename and change types and number of parameters
    public static Fragment_TagEditor_4_DeleteTag newInstance(String param1, String param2) {
        Fragment_TagEditor_4_DeleteTag fragment = new Fragment_TagEditor_4_DeleteTag();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // TODO: Rename and change types of parameters
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Calling Application class (see application tag in AndroidManifest.xml)
        if(getActivity() != null) {
            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }
        //Instantiate the ViewModel sharing data between fragments:
        viewModelTagEditor = new ViewModelProvider(this).get(ViewModel_TagEditor.class);

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
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
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
        for (Map.Entry<String, ItemClass_Tag> tmEntryTagReferenceItem : globalClass.gtmCatalogTagReferenceLists.get(viewModelTagEditor.iTagEditorMediaCategory).entrySet()) {
            alict_TagItems.add(tmEntryTagReferenceItem.getValue());
        }

        gListViewTagsAdapter = new ListViewTagsAdapter(getActivity().getApplicationContext(), alict_TagItems);
        listView_TagDelete.setAdapter(gListViewTagsAdapter);
        listView_TagDelete.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }


    private void button_DeleteTag_Click(View v){
        if (getView() == null) {
            return;
        }
        ListView listView_TagDelete = getView().findViewById(R.id.listView_TagDelete);
        String sConfirmationMessage = "Confirm tag geletion: ";
        sConfirmationMessage = sConfirmationMessage + Objects.requireNonNull(gListViewTagsAdapter.getItem(gListViewTagsAdapter.iTagItemSelected)).TagText;

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
        Service_TagEditor.startActionDeleteTag(getActivity(),
                gListViewTagsAdapter.getItem(gListViewTagsAdapter.iTagItemSelected),
                viewModelTagEditor.iTagEditorMediaCategory);
    }



    public class TagEditorServiceResponseReceiver extends BroadcastReceiver {
        public static final String TAG_EDITOR_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_TAG_EDITOR_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_TagEditor.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {
                //Check to see if this is a message indicating that a tag deletion is complete:
                boolean bTagDeleteComplete = intent.getBooleanExtra(Service_TagEditor.EXTRA_TAG_DELETE_COMPLETE,false);
                if(bTagDeleteComplete){
                    RefreshTagListView();
                    Toast.makeText(context, "Tag deletion complete.", Toast.LENGTH_LONG).show();
                }

            }

        }
    }


    public class ListViewTagsAdapter extends ArrayAdapter<ItemClass_Tag> {

        int iTagItemSelected = -1;


        public ListViewTagsAdapter(Context context, ArrayList<ItemClass_Tag> tagItems) {
            super(context, 0, tagItems);
        }

        @Override
        public View getView(final int position, View v, ViewGroup parent) {
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
            String s = tagItem.TagText;
            checkedTextView_TagText.setText(s);


            //Set the selection state (needed as views are recycled).
            if(getActivity() != null) {
                if (tagItem.isChecked) {
                    checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentImportBackgroundHighlight2));
                } else {
                    checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentImportBackground));
                }
                checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorGrey1));
            }

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View viewClicked) {
                    //Handle changing the checked state:
                    if(iTagItemSelected > -1){
                        //If a tag item is already selected, make the adjustments:
                        ItemClass_Tag tagItemPrevSelected = getItem(iTagItemSelected);
                        if(tagItemPrevSelected != null && (iTagItemSelected != position)){
                            tagItemPrevSelected.isChecked = false;
                        }
                        iTagItemSelected = -1;
                    }

                    ItemClass_Tag tagItem_Clicked = getItem(position);

                    if(tagItem_Clicked != null) {
                        tagItem_Clicked.isChecked = !tagItem_Clicked.isChecked;
                        if (tagItem_Clicked.isChecked) {
                            iTagItemSelected = position;
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