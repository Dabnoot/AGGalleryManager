package com.agcurations.aggallerymanager;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Fragment_SelectTags extends Fragment {

    GlobalClass globalClass;

    private ViewModel_Fragment_SelectTags viewModel_fragment_selectTags;

    public static Fragment_SelectTags newInstance() {
        return new Fragment_SelectTags();
    }

    public final static String MEDIA_CATEGORY = "MEDIA_CATEGORY";
    public final static String PRESELECTED_TAG_ITEMS = "PRESELECTED_TAG_ITEMS";

    private ArrayList<Integer> galiPreselectedTags;

    ListViewTagsAdapter gListViewTagsAdapter;

    private boolean gbCatalogTagsRestrictionsOn;

    ArrayList<ItemClass_Tag> galNewTags; //Used in conjunction with the TagEditor.
    // If the user creates new tags from this fragment, select those tags in the list upon return.

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_tags, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getActivity() == null) return;
        viewModel_fragment_selectTags = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_SelectTags.class);

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        //Get the Media Category argument passed to this fragment:
        Bundle args = getArguments();


        if(args == null) {
            viewModel_fragment_selectTags.iMediaCategory = 0;
            galiPreselectedTags = new ArrayList<>();
        } else {
            viewModel_fragment_selectTags.iMediaCategory = args.getInt(MEDIA_CATEGORY, 0);
            galiPreselectedTags = args.getIntegerArrayList(PRESELECTED_TAG_ITEMS);
        }

        if (getView() == null) {
            return;
        }

        if(getActivity() == null){
            return;
        }

        //Process button_tags_restricted:
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        gbCatalogTagsRestrictionsOn = sharedPreferences.getBoolean("hide_restricted_tags", false);
        Button button_tags_restricted = getView().findViewById(R.id.button_tags_restricted);
        //Set locked/unlocked icon:
        if(gbCatalogTagsRestrictionsOn){
            button_tags_restricted.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_white_18dp, 0, 0, 0);
        } else {
            button_tags_restricted.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_open_white_18dp, 0, 0, 0);
        }
        button_tags_restricted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(gbCatalogTagsRestrictionsOn){
                    //If restrictions are on, ask for pin code before unlocking.

                    if(getActivity() == null){
                        return;
                    }
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustomStyle);

                    // set the custom layout
                    final View customLayout = getLayoutInflater().inflate(R.layout.dialog_layout_pin_code, null);
                    builder.setView(customLayout);

                    final AlertDialog adConfirmationDialog = builder.create();

                    //Code action for the Cancel button:
                    Button button_PinCodeCancel = customLayout.findViewById(R.id.button_PinCodeCancel);
                    button_PinCodeCancel.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            adConfirmationDialog.dismiss();
                        }
                    });

                    //Code action for the OK button:
                    Button button_PinCodeOK = customLayout.findViewById(R.id.button_PinCodeOK);
                    button_PinCodeOK.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                            String sPinEntered = editText_DialogInput.getText().toString();

                            if(sPinEntered.equals(globalClass.gsPin)){
                                //Show catalog items with restricted tags.
                                gbCatalogTagsRestrictionsOn = false;
                                initListViewData();
                                //Change the lock icon to 'unlocked':
                                if(getView() != null) {
                                    Button button_tags_restricted = getView().findViewById(R.id.button_tags_restricted);
                                    button_tags_restricted.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_open_white_18dp, 0, 0, 0);
                                }
                                Toast.makeText(getActivity(), "Showing " + gListViewTagsAdapter.getCount() + " tags.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                            }

                            adConfirmationDialog.dismiss();
                        }
                    });

                    adConfirmationDialog.show();


                } else {
                    //If restrictions are off...
                    //Turn on restrictions, hide items, set icon to show lock symbol
                    gbCatalogTagsRestrictionsOn = true;
                    initListViewData();
                    ((Button)view).setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_white_18dp, 0, 0, 0);
                    Toast.makeText(getActivity(), "Showing " + gListViewTagsAdapter.getCount() + " tags.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button button_UncheckTags = getView().findViewById(R.id.button_UncheckTags);
        button_UncheckTags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gListViewTagsAdapter.uncheckAll();
            }
        });


        //Configure the button to start the tag editor:
        if (getView() == null) {
            return;
        }
        Button button_TagEditor = getView().findViewById(R.id.button_TagEditor);
        button_TagEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Ask for pin code in order to allow access to the Tag Editor:


                if (getActivity() == null) {
                    return;
                }
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustomStyle);

                // set the custom layout
                final View customLayout = getLayoutInflater().inflate(R.layout.dialog_layout_pin_code, null);
                builder.setView(customLayout);

                final AlertDialog adConfirmationDialog = builder.create();

                //Code action for the Cancel button:
                Button button_PinCodeCancel = customLayout.findViewById(R.id.button_PinCodeCancel);
                button_PinCodeCancel.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        adConfirmationDialog.dismiss();
                    }
                });

                //Code action for the OK button:
                Button button_PinCodeOK = customLayout.findViewById(R.id.button_PinCodeOK);
                button_PinCodeOK.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                        String sPinEntered = editText_DialogInput.getText().toString();

                        if(sPinEntered.equals(globalClass.gsPin)){
                            Intent intentTagEditor = new Intent(getActivity(), Activity_TagEditor.class);
                            intentTagEditor.putExtra(Activity_TagEditor.EXTRA_INT_MEDIA_CATEGORY, viewModel_fragment_selectTags.iMediaCategory);
                            garlGetResultFromTagEditor.launch(intentTagEditor);
                        } else {
                            Toast.makeText(getActivity(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                        }

                        adConfirmationDialog.dismiss();
                    }
                });

                adConfirmationDialog.show();

            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        initListViewData();
    }

    public void resetTagListViewData(ArrayList<Integer> aliPreselectedTags){
        //This is called by the host activity when the user goes to the next image or video.
        //  The user specifies the new tags and we "check mark" the tags that apply.
        galiPreselectedTags = aliPreselectedTags;
        initListViewData();
    }

    private void initListViewData(){
        ListView listView_ImportTagSelection;
        if(getView() != null) {
            listView_ImportTagSelection = getView().findViewById(R.id.listView_ImportTagSelection);
        } else {
            return;
        }

        //Get tags to put in the ListView:
        int iPreSelectedTagsCount = 0;
        int iPreSelectedTagIterator = 0;
        int iSelectionOrder;

        viewModel_fragment_selectTags.alTagsAll.clear();

        TreeMap<String, ItemClass_Tag> tmTagPool = globalClass.gtmCatalogTagReferenceLists.get(viewModel_fragment_selectTags.iMediaCategory);

        //Go through the tags treeMap and put the ListView together:
        for (Map.Entry<String, ItemClass_Tag> tmEntryTagReferenceItem : tmTagPool.entrySet()) {

            //Check to see if the list of preselected tags includes this tag from the reference list.
            //  If so, set the item as "checked":
            boolean bIsChecked = false;
            iSelectionOrder = 0;
            //Log.d("Tag identification", tmEntryTagReferenceItem.getKey());
            if (galiPreselectedTags != null) {
                iPreSelectedTagsCount = galiPreselectedTags.size();
                int iReferenceTagID = tmEntryTagReferenceItem.getValue().iTagID;
                for (int iPreSelectedTagID : galiPreselectedTags) {
                    if (iReferenceTagID == iPreSelectedTagID) {
                        bIsChecked = true;
                        iPreSelectedTagIterator++;
                        iSelectionOrder = galiPreselectedTags.indexOf(iPreSelectedTagID) + 1;
                        //iSelectionOrder = iPreSelectedTagIterator;
                        break;
                    }
                }
            }

            if (galNewTags != null) { //If the user used the TagEditor JUST NOW to add new tags,
                                        //  pre-select them:
                iPreSelectedTagsCount += galNewTags.size();
                int iReferenceTagID = tmEntryTagReferenceItem.getValue().iTagID;
                for (ItemClass_Tag iNewTagItem : galNewTags) {
                    if (iReferenceTagID == iNewTagItem.iTagID) {
                        bIsChecked = iNewTagItem.bIsChecked;
                        iNewTagItem.bIsChecked = false; //Only mark as selected once, as the user might not want this tag on the next item they view.
                        iPreSelectedTagIterator++;
                        iSelectionOrder = iPreSelectedTagIterator;
                        break;
                    }
                }
            }

            ItemClass_Tag tiNew = new ItemClass_Tag(
                    tmEntryTagReferenceItem.getValue().iTagID,
                    tmEntryTagReferenceItem.getValue().sTagText);
            tiNew.bIsChecked = bIsChecked;
            tiNew.iSelectionOrder = iSelectionOrder;
            tiNew.bIsRestricted = tmEntryTagReferenceItem.getValue().bIsRestricted;

            if(!(gbCatalogTagsRestrictionsOn && tiNew.bIsRestricted)) {
                //Don't add the tag if TagRestrictions are on and this is a restricted tag.
                viewModel_fragment_selectTags.alTagsAll.add(tiNew);
            }

        }

        // Create the adapter for the ListView, and set the ListView adapter:
        if(getActivity() == null){
            return;
        }
        gListViewTagsAdapter = new ListViewTagsAdapter(getActivity().getApplicationContext(), viewModel_fragment_selectTags.alTagsAll, iPreSelectedTagsCount);
        listView_ImportTagSelection.setAdapter(gListViewTagsAdapter);
        listView_ImportTagSelection.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    ActivityResultLauncher<Intent> garlGetResultFromTagEditor = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //Get result from TagEditor Activity:
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data == null) return;
                        Bundle b = data.getBundleExtra(Activity_TagEditor.EXTRA_BUNDLE_TAG_EDITOR_NEW_TAGS_RESULT);
                        if(b != null) {
                            //If new tags have been added...
                            ArrayList<ItemClass_Tag> altiNewTags = (ArrayList<ItemClass_Tag>) b.getSerializable(Activity_TagEditor.NEW_TAGS);
                            if (galNewTags == null) {
                                galNewTags = new ArrayList<>();
                            }
                            if (altiNewTags != null) {
                                //Set the new tag(s) to "selected" so that the user does not have to go through and find the tag that they just added:
                                for (ItemClass_Tag iNewTagItem : altiNewTags) {
                                    iNewTagItem.bIsChecked = true;
                                }
                                //Add the tags to galNewTags so that the fragment knows which tags to highlight:
                                galNewTags.addAll(altiNewTags);


                                //Also add the the tags to the ViewModel selected tags array so that whatever is watching
                                // that variable will properly update.
                                ArrayList<ItemClass_Tag> altiExistingSelectedTags = viewModel_fragment_selectTags.altiTagsSelected.getValue();
                                if(altiExistingSelectedTags == null){
                                    altiExistingSelectedTags = new ArrayList<>();
                                }
                                altiExistingSelectedTags.addAll(altiNewTags);
                                viewModel_fragment_selectTags.setSelectedTags(altiExistingSelectedTags);

                            }
                        }

                        //Reload catalog item. Tags may have been deleted or renamed. The file
                        //  may also have been moved if its tag folder was deleted.
                        boolean bTagRenamed = data.getBooleanExtra(Activity_TagEditor.EXTRA_BOOL_TAG_RENAMED, false);
                        boolean bTagDeleted = data.getBooleanExtra(Activity_TagEditor.EXTRA_BOOL_TAG_DELETED, false);
                        viewModel_fragment_selectTags.bTagDeleted.setValue(bTagDeleted);
                        if(bTagRenamed || bTagDeleted){
                            //Update selected tags held in the ViewModel for the event that the user renamed some of the
                            //  tags that the user had already selected:
                            ArrayList<ItemClass_Tag> altiExistingSelectedTags = viewModel_fragment_selectTags.altiTagsSelected.getValue();
                            if(altiExistingSelectedTags != null) {
                                ArrayList<ItemClass_Tag> altiUpdatedExistingSelectedTags = new ArrayList<>();
                                for (ItemClass_Tag ict : altiExistingSelectedTags) {
                                    if(globalClass.TagIDExists(ict.iTagID, viewModel_fragment_selectTags.iMediaCategory)) {
                                        ict.sTagText = globalClass.getTagTextFromID(ict.iTagID, viewModel_fragment_selectTags.iMediaCategory);
                                        altiUpdatedExistingSelectedTags.add(ict);
                                    }
                                }
                                viewModel_fragment_selectTags.altiTagsSelected.setValue(altiUpdatedExistingSelectedTags);
                            }

                        }
                    }
                }
            });  //End get result from TagEditor.


    public class ListViewTagsAdapter extends ArrayAdapter<ItemClass_Tag> {
        int iOrderIterator;

        public ListViewTagsAdapter(Context context, ArrayList<ItemClass_Tag> tagItems, int iPreselectedCount) {
            super(context, 0, tagItems);
            iOrderIterator = iPreselectedCount;
        }

        public void uncheckAll(){
            for(int i = 0; i < getCount(); i++){
                ItemClass_Tag tagItem = getItem(i);
                tagItem.bIsChecked = false;
            }
            if(galiPreselectedTags != null){
                galiPreselectedTags.clear();
            }
            ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();
            viewModel_fragment_selectTags.setSelectedTags(alTagItems);

            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
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
                public void onClick(View v) {
                    //Handle changing the checked state:
                    tagItem.bIsChecked = !tagItem.bIsChecked;
                    if(tagItem.bIsChecked){
                        iOrderIterator++;
                        tagItem.iSelectionOrder = iOrderIterator; //Set the index for the order in which this item was selected.
                        checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                        checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
                        if(galiPreselectedTags == null){
                            galiPreselectedTags = new ArrayList<>();
                        }
                        galiPreselectedTags.add(tagItem.iTagID);
                    } else {
                        //iOrderIterator--; Never decrease the order iterator, because user may unselect a middle item, thus creating duplicate order nums.
                        tagItem.iSelectionOrder = 0; //Remove the index showing the order in which this item was selected.
                        checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorUnfilledUnselected));
                        checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
                        //Remove the item from preselected tags, if it exists there:
                        if(galiPreselectedTags == null){
                            //Not a likely case, but just in case.
                            galiPreselectedTags = new ArrayList<>();
                        }
                        for(int i = 0; i < galiPreselectedTags.size(); i++) {
                            if(galiPreselectedTags.get(i).equals(tagItem.iTagID)){
                                galiPreselectedTags.remove(i);
                                break;
                            }
                        }
                    }

                    //Reform the tags string listing all of the selected tags:

                    //Iterate through all of the items in this ArrayAdapter, gathering the items,
                    //  and using a TreeMap to automatically sort the items by selection order:
                    int iItemCount = getCount();
                    TreeMap<Integer, ItemClass_Tag> tmSelectedItems = new TreeMap<>();
                    for(int i=0; i< iItemCount; i++){
                        if(getItem(i) != null){
                            ItemClass_Tag tagItem1 = getItem(i);
                            assert tagItem1 != null;
                            if(tagItem1.bIsChecked) {
                                tmSelectedItems.put(tagItem1.iSelectionOrder, tagItem1);
                            }
                        }
                    }

                    //Put the sorted TreeList items into an ArrayList and transfer to the ViewModel:
                    ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();

                    for (Map.Entry<Integer, ItemClass_Tag>
                            entry : tmSelectedItems.entrySet()) {
                        alTagItems.add(entry.getValue());
                    }

                    viewModel_fragment_selectTags.setSelectedTags(alTagItems);

                }
            });


            // Return the completed view to render on screen
            return v;
        }
    }

}