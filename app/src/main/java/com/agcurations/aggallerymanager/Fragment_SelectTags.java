package com.agcurations.aggallerymanager;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
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

    ArrayList<ItemClass_Tag> galNewTags; //Used in conjunction with the TagEditor.
    // If the user creates new tags from this fragment, select those tags in the list upon return.

    TagContainerLayoutWithID gTagContainerLayout_SuggestedTags;

    boolean bOptionViewOnly = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_tags, container, false);
    }


    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
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
            if(galiPreselectedTags == null){
                galiPreselectedTags = new ArrayList<>();
            }
        }

        if (getView() == null) {
            return;
        }

        if(getActivity() == null){
            return;
        }

        //Process button_login:
        Button button_login = getView().findViewById(R.id.button_login);
        button_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(globalClass.gbGuestMode){
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
                                globalClass.gbGuestMode = false;
                                globalClass.gbCatalogViewerRefresh = true;
                                triggerParentActivityUserCheck();
                                setUserColor(globalClass.USER_COLOR_ADMIN);

                                initListViewData();
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
                    globalClass.gbGuestMode = true;
                    globalClass.gbCatalogViewerRefresh = true;
                    triggerParentActivityUserCheck();
                    setUserColor(globalClass.USER_COLOR_GUEST);
                    initListViewData();
                    Toast.makeText(getActivity(), "Showing " + gListViewTagsAdapter.getCount() + " tags.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        recalcUserColor();

        Button button_UncheckTags = getView().findViewById(R.id.button_UncheckTags);



        //Configure the button to start the tag editor:
        if (getView() == null) {
            return;
        }
        Button button_TagEditor = getView().findViewById(R.id.button_TagEditor);

        if(bOptionViewOnly) {
            //Hide the UncheckTags button:
            button_UncheckTags.setVisibility(View.INVISIBLE);

            //Shrink down the TagEditor button space allocation since it is not to be used in this mode:
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) button_TagEditor.getLayoutParams();
            layoutParams.height = 0;
            layoutParams.setMargins(0, 0, 0, 0);
            button_TagEditor.setLayoutParams(layoutParams);

        } else {
            button_UncheckTags.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gListViewTagsAdapter.uncheckAll();
                    }
            });

            button_TagEditor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Ask for pin code in order to allow access to the Tag Editor:


                    if (getActivity() == null) {
                        return;
                    }

                    if (globalClass.gbGuestMode) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustomStyle);

                        // set the custom layout
                        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_layout_pin_code, null);
                        builder.setView(customLayout);

                        final AlertDialog adConfirmationDialog = builder.create();

                        //Code action for the Cancel button:
                        Button button_PinCodeCancel = customLayout.findViewById(R.id.button_PinCodeCancel);
                        button_PinCodeCancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                adConfirmationDialog.dismiss();
                            }
                        });

                        //Code action for the OK button:
                        Button button_PinCodeOK = customLayout.findViewById(R.id.button_PinCodeOK);
                        button_PinCodeOK.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                                String sPinEntered = editText_DialogInput.getText().toString();

                                if (sPinEntered.equals(globalClass.gsPin)) {
                                    globalClass.gbGuestMode = false;
                                    globalClass.gbCatalogViewerRefresh = true;
                                    triggerParentActivityUserCheck();
                                    setUserColor(globalClass.USER_COLOR_ADMIN);

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
                    } else {
                        //If we are in Admin mode:
                        Intent intentTagEditor = new Intent(getActivity(), Activity_TagEditor.class);
                        intentTagEditor.putExtra(Activity_TagEditor.EXTRA_INT_MEDIA_CATEGORY, viewModel_fragment_selectTags.iMediaCategory);
                        garlGetResultFromTagEditor.launch(intentTagEditor);
                    }
                }
            });


            //Create adapations for "suggested tags":
            //Update a maintained list of suggested tags:
            //React to changes in the selected tag data in the ViewModel:
            final Observer<ArrayList<ItemClass_Tag>> selectedTagsObserverForTagSuggestions = new Observer<ArrayList<ItemClass_Tag>>() {
                @Override
                public void onChanged(ArrayList<ItemClass_Tag> tagItems) {

                    //Update the suggested tags list in the viewmodel:
                    ArrayList<ItemClass_Tag> alictSuggestions = viewModel_fragment_selectTags.altiTagSuggestions.getValue();
                    ArrayList<ItemClass_Tag> alictNewSuggestions = viewModel_fragment_selectTags.altiTagSuggestions.getValue();
                    if (alictSuggestions == null) {
                        alictSuggestions = new ArrayList<>();
                        alictNewSuggestions = new ArrayList<>();
                    }
                    for (ItemClass_Tag ict : tagItems) {
                        boolean bNotInList = true;
                        for (ItemClass_Tag ictSuggestion : alictSuggestions) {
                            if (ict.iTagID.equals(ictSuggestion.iTagID)) {
                                bNotInList = false;
                                break;
                            }
                        }
                        if (bNotInList) {
                            alictNewSuggestions.add(ict);
                        }
                    }
                    viewModel_fragment_selectTags.altiTagSuggestions.postValue(alictNewSuggestions);

                }
            };
            viewModel_fragment_selectTags.altiTagsSelected.observe(getActivity(), selectedTagsObserverForTagSuggestions);

            //Maintain the display of suggested tags:
            gTagContainerLayout_SuggestedTags = getView().findViewById(R.id.tagContainerLayout_SuggestedTags);
            gTagContainerLayout_SuggestedTags.setOnTagClickListener(new TagViewWithID.OnTagClickListener() {
                @Override
                public void onTagClick(int position, String text) {

                    ItemClass_Tag ict = gTagContainerLayout_SuggestedTags.getTagItem(position);
                    //gListViewTagsAdapter.selectTagByName(text);
                    ArrayList<Integer> aliTagIDs = new ArrayList<>();
                    aliTagIDs.add(ict.iTagID);
                    gListViewTagsAdapter.selectTagsByIDs(aliTagIDs);
                 }

                @Override
                public void onTagLongClick(int position, String text) {

                }

                @Override
                public void onSelectedTagDrag(int position, String text) {

                }

                @Override
                public void onTagCrossClick(int position) {
                    //Remove the item from suggested tags:
                    //String sTagText = gTagContainerLayout_SuggestedTags.getTagText(position);
                    ItemClass_Tag ictTagToRemove = gTagContainerLayout_SuggestedTags.getTagItem(position);
                    //Update the suggested tags list in the viewmodel:
                    ArrayList<ItemClass_Tag> alictSuggestions = viewModel_fragment_selectTags.altiTagSuggestions.getValue();
                    ArrayList<ItemClass_Tag> alictNewSuggestions = new ArrayList<>();
                    for(ItemClass_Tag ict: alictSuggestions){
                        if(!ict.iTagID.equals(ictTagToRemove.iTagID)){
                            alictNewSuggestions.add(ict);
                        }
                    }
                    viewModel_fragment_selectTags.altiTagSuggestions.postValue(alictNewSuggestions);

                }
            });

            //Watch for changes in suggested tags:
            final Observer<ArrayList<ItemClass_Tag>> suggestedTagsObserver = new Observer<ArrayList<ItemClass_Tag>>() {
                @Override
                public void onChanged(ArrayList<ItemClass_Tag> tagItems) {
                    updateSuggestedTagDisplay(tagItems);
                }
            };
            viewModel_fragment_selectTags.altiTagSuggestions.observe(getActivity(), suggestedTagsObserver);
        }




    }

    private void setUserColor(int iColor){
        Drawable drawable = AppCompatResources.getDrawable(getActivity().getApplicationContext(), R.drawable.login).mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(iColor, PorterDuff.Mode.SRC_IN));
        Button button_login = getView().findViewById(R.id.button_login);
        button_login.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
    }

    public void recalcUserColor(){
        if (!globalClass.gbGuestMode) {
            setUserColor(globalClass.USER_COLOR_ADMIN);
        } else {
            setUserColor(globalClass.USER_COLOR_GUEST);
        }
    }

    private void triggerParentActivityUserCheck(){
        Integer iUserChangedToggle = viewModel_fragment_selectTags.mldiUserChangedToggle.getValue();
        if(iUserChangedToggle == null){
            iUserChangedToggle = 0;
        }
        iUserChangedToggle++;
        viewModel_fragment_selectTags.mldiUserChangedToggle.setValue(iUserChangedToggle);
    }

    private void updateSuggestedTagDisplay(ArrayList<ItemClass_Tag> alTagSuggestions){
        //Get the text of the tags and display tag suggestions:

        if(alTagSuggestions == null){
            return;
        }

        //Go through the selected tags and only display suggested tags that are not selected:
        ArrayList<ItemClass_Tag> alictTagSelections = viewModel_fragment_selectTags.altiTagsSelected.getValue();

        if(alictTagSelections == null){
            return;
        }

        ArrayList<ItemClass_Tag> alictValidSuggestions = new ArrayList<>();
        for(ItemClass_Tag ictSuggestion: alTagSuggestions){
            boolean bSuggestedTagNotSelected = true;
            for(ItemClass_Tag ictSelection: alictTagSelections){
                if(ictSelection.iTagID.equals(ictSuggestion.iTagID)){
                    bSuggestedTagNotSelected = false;
                    break;
                }
            }
            if(bSuggestedTagNotSelected){
                alictValidSuggestions.add(ictSuggestion);
            }
        }

        //Sort the valid suggestions:
        TreeMap<String, ItemClass_Tag> tmValidSuggestions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for(ItemClass_Tag ictSuggestion: alictValidSuggestions){
            tmValidSuggestions.put(ictSuggestion.sTagText, ictSuggestion);
        }
        //Copy over:
        alictValidSuggestions.clear();
        for(Map.Entry<String, ItemClass_Tag> entry: tmValidSuggestions.entrySet()){
            alictValidSuggestions.add(entry.getValue());
        }

        gTagContainerLayout_SuggestedTags.setTags(alictValidSuggestions);



    }

    private void InitializeSuggestedTagDisplay(){
        //Go through the selected tags and only display suggested tags that are not selected:
        ArrayList<ItemClass_Tag> alict_TagSuggestions = viewModel_fragment_selectTags.altiTagSuggestions.getValue();
        if(alict_TagSuggestions == null){
            return;
        }
        ArrayList<ItemClass_Tag> alictValidSuggestions = new ArrayList<>();
        for(ItemClass_Tag ictSuggestion: alict_TagSuggestions){
            boolean bSuggestedTagNotSelected = true;
            if(galiPreselectedTags != null) {
                for (int iPreSelectedTagID : galiPreselectedTags) {
                    if (iPreSelectedTagID == ictSuggestion.iTagID) {
                        bSuggestedTagNotSelected = false;
                        break;
                    }
                }
            }
            if(bSuggestedTagNotSelected){
                alictValidSuggestions.add(ictSuggestion);
            }
        }
        gTagContainerLayout_SuggestedTags.setTags(alictValidSuggestions);

    }




    @Override
    public void onResume() {
        super.onResume();
        recalcUserColor();
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

        TreeMap<Integer, ItemClass_Tag> tmTagPool = globalClass.gtmCatalogTagReferenceLists.get(viewModel_fragment_selectTags.iMediaCategory);

        ArrayList<ItemClass_Tag> alict_SelectedTagsViewModelReset = new ArrayList<>();

        //Go through the tags treeMap and put the ListView together:
        for (Map.Entry<Integer, ItemClass_Tag> tmEntryTagReferenceItem : tmTagPool.entrySet()) {

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

            ItemClass_Tag ictNew = new ItemClass_Tag(
                    tmEntryTagReferenceItem.getValue().iTagID,
                    tmEntryTagReferenceItem.getValue().sTagText);
            ictNew.bIsChecked = bIsChecked;
            ictNew.iSelectionOrder = iSelectionOrder;
            ictNew.iHistogramCount = tmEntryTagReferenceItem.getValue().iHistogramCount;
            ictNew.bIsRestricted = tmEntryTagReferenceItem.getValue().bIsRestricted;

            if(!(globalClass.gbGuestMode && ictNew.bIsRestricted)) {
                //Don't add the tag if TagRestrictions are on and this is a restricted tag.
                viewModel_fragment_selectTags.alTagsAll.add(ictNew);
                if(ictNew.bIsChecked){
                    alict_SelectedTagsViewModelReset.add(ictNew);
                }
            }

        }

        //Reset the selected tags stored in the viewmodel:
        viewModel_fragment_selectTags.setSelectedTags(alict_SelectedTagsViewModelReset);

        galNewTags = null; //Mark galNewTags as empty.

        // Create the adapter for the ListView, and set the ListView adapter:
        if(getActivity() == null){
            return;
        }
        gListViewTagsAdapter = new ListViewTagsAdapter(getActivity().getApplicationContext(), viewModel_fragment_selectTags.alTagsAll, iPreSelectedTagsCount);
        listView_ImportTagSelection.setAdapter(gListViewTagsAdapter);
        listView_ImportTagSelection.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        //Initialize suggested tags by sending an updated list of what is selected:
        InitializeSuggestedTagDisplay();


    }

    @SuppressWarnings("unchecked")
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

        ArrayList<ItemClass_Tag> alictTagItems; //Contains all tag items passed to the listviewTagsAdapter.
        TreeMap<String, ItemClass_Tag> tmTagItemsDisplay;
        TreeMap<Integer, String> tmTagItemsDisplaySequence;

        public ListViewTagsAdapter(Context context, ArrayList<ItemClass_Tag> tagItems, int iPreselectedCount) {
            super(context, 0, tagItems);
            iOrderIterator = iPreselectedCount;
            alictTagItems = tagItems;
            tmTagItemsDisplay = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            tmTagItemsDisplaySequence = new TreeMap<>();

            if (viewModel_fragment_selectTags.bShowModeXrefTagUse) {
                //When bShowModeXrefTagUse is activated, we are using Fragment_SelectTags
                //  for filtering. In this case, only show tags which are used, that is,
                //  has a frequency count greater than zero.
                updateXrefTagsHistogram();
            } else {
                for (ItemClass_Tag ict : alictTagItems) {
                    String sCompoundID = ict.sTagText.toLowerCase() + ":" + ict.iTagID;
                    tmTagItemsDisplay.put(sCompoundID, ict);
                }
            }

            tmTagItemsDisplaySequence = new TreeMap<>();
            int iSequence = 0;
            for(Map.Entry<String, ItemClass_Tag> entry: tmTagItemsDisplay.entrySet()){
                //Configure a tree map to map display item position to the display item key:
                tmTagItemsDisplaySequence.put(iSequence, entry.getKey());
                iSequence++;
            }

        }

        public void uncheckAll(){

            for(Map.Entry<String, ItemClass_Tag> entry: tmTagItemsDisplay.entrySet()){
                entry.getValue().bIsChecked = false;
            }
            if(galiPreselectedTags != null){
                galiPreselectedTags.clear();
            }
            ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();
            viewModel_fragment_selectTags.setSelectedTags(alTagItems);

            updateXrefTagsHistogram();

            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, View v, @NonNull ViewGroup parent) {
            // Get the data item for this position

            String sCompoundID = tmTagItemsDisplaySequence.get(position);
            final ItemClass_Tag tagItem = tmTagItemsDisplay.get(sCompoundID);

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
            //Attempt to get usage data for the tag:
            int iTagUseCount;
            iTagUseCount = tagItem.iHistogramCount;
            if(iTagUseCount != 0) {
                s = s + " (" + iTagUseCount + ")";
            }
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
                    TreeMap<Integer, ItemClass_Tag> tmSelectedItems = new TreeMap<>();
                    for(Map.Entry<String, ItemClass_Tag> entry: tmTagItemsDisplay.entrySet()){
                        if (entry.getValue().bIsChecked) {
                            tmSelectedItems.put(entry.getValue().iSelectionOrder, entry.getValue());
                        }
                    }

                    //Put the sorted TreeList items into an ArrayList and transfer to the ViewModel:
                    ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();

                    for (Map.Entry<Integer, ItemClass_Tag> entry : tmSelectedItems.entrySet()) {
                        alTagItems.add(entry.getValue());
                    }

                    viewModel_fragment_selectTags.setSelectedTags(alTagItems);

                    if(viewModel_fragment_selectTags.bShowModeXrefTagUse) {
                        updateXrefTagsHistogram();
                        notifyDataSetChanged();//todo: should this be moved outside of this if-statement?
                    }

                }
            });


            // Return the completed view to render on screen
            return v;
        }


        public void selectTagsByIDs(ArrayList<Integer> aliTagsToSelect){
            boolean bNotifyDataSetChanged = false;
            for(Integer iIncomingTagID: aliTagsToSelect) {
                //Go and look to make sure that the tag is not already selected:
                boolean bTagAlreadySelected = false;
                for (int iTagID : galiPreselectedTags) {
                    if (iIncomingTagID == iTagID) {
                        bTagAlreadySelected = true;
                        break;
                    }
                }

                if (!bTagAlreadySelected) {
                    //Find the tagItem in the list:


                    for (Map.Entry<String, ItemClass_Tag> entry: tmTagItemsDisplay.entrySet()){
                        if (entry.getValue().iTagID.equals(iIncomingTagID)) {
                            //Select the tagItem:
                            iOrderIterator++;
                            entry.getValue().iSelectionOrder = iOrderIterator; //Set the index for the order in which this item was selected.
                            entry.getValue().bIsChecked = true;
                            galiPreselectedTags.add(entry.getValue().iTagID);
                            bNotifyDataSetChanged = true;
                            break;
                        }
                    }

                }
            }


            if (bNotifyDataSetChanged) {
                //Reform the tags string listing all of the selected tags:

                //Iterate through all of the items in this ArrayAdapter, gathering the items,
                //  and using a TreeMap to automatically sort the items by selection order:
                TreeMap<Integer, ItemClass_Tag> tmSelectedItems = new TreeMap<>();

                for (Map.Entry<String, ItemClass_Tag> entry: tmTagItemsDisplay.entrySet()){
                    if (entry.getValue().bIsChecked) {
                        tmSelectedItems.put(entry.getValue().iSelectionOrder, entry.getValue());
                    }
                }

                //Put the sorted TreeList items into an ArrayList and transfer to the ViewModel:
                ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();

                for (Map.Entry<Integer, ItemClass_Tag>
                        entry : tmSelectedItems.entrySet()) {
                    alTagItems.add(entry.getValue());
                }

                viewModel_fragment_selectTags.setSelectedTags(alTagItems);

                notifyDataSetChanged();

                if (viewModel_fragment_selectTags.bShowModeXrefTagUse) {
                    updateXrefTagsHistogram();
                }

            }
        }

        private void updateXrefTagsHistogram(){
            //recalc cross-referenced tag histogram with the newly checked/unchecked item:
            TreeMap<Integer, ItemClass_Tag> tmXrefTagHistogram =
                    globalClass.getXrefTagHistogram(viewModel_fragment_selectTags.iMediaCategory, galiPreselectedTags, globalClass.gbGuestMode);

            //Mark tags selected as appropriate:
            for(int iTagID: galiPreselectedTags){
                tmXrefTagHistogram.get(iTagID).bIsChecked = true;
            }

            //Sort items by tag name:
            tmTagItemsDisplay.clear();
            for(Map.Entry<Integer, ItemClass_Tag> entry: tmXrefTagHistogram.entrySet()){
                String sCompoundID = entry.getValue().sTagText + ":" + entry.getValue().iTagID;
                tmTagItemsDisplay.put(sCompoundID, entry.getValue());
            }

            tmTagItemsDisplaySequence.clear();
            int iSequence = 0;
            for(Map.Entry<String, ItemClass_Tag> entry: tmTagItemsDisplay.entrySet()){
                //Configure a tree map to map display item position to the display item key:
                tmTagItemsDisplaySequence.put(iSequence, entry.getKey());
                iSequence++;
            }

        }

        @Override
        public int getCount() {
            if(viewModel_fragment_selectTags.bShowModeXrefTagUse){
                return tmTagItemsDisplay.size();
            }

            return super.getCount();
        }
    }




}