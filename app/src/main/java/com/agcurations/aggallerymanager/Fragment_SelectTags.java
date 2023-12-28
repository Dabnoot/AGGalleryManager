package com.agcurations.aggallerymanager;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.List;
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

    ChipGroup gChipGroup_SuggestedTags;



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

        Button button_UncheckTags = getView().findViewById(R.id.button_UncheckTags);
        button_UncheckTags.setOnClickListener(view1 -> gListViewTagsAdapter.uncheckAll());

        //Configure the button to start the tag editor:
        if (getView() == null) {
            return;
        }
        Button button_TagEdit = getView().findViewById(R.id.button_TagEdit);

        //Maintain the display of suggested tags:
        gChipGroup_SuggestedTags = getView().findViewById(R.id.chipGroup_SuggestedTags);

        if(bOptionViewOnly) {
            //Hide the UncheckTags button:
            button_UncheckTags.setVisibility(View.INVISIBLE);

            //Hide the TagEditor button:
            button_TagEdit.setVisibility(View.INVISIBLE);

            //Collapse the chipgroup showing recently-selected tags:
            RelativeLayout.LayoutParams rlParams = (RelativeLayout.LayoutParams) gChipGroup_SuggestedTags.getLayoutParams();
            rlParams.height = 0;
            gChipGroup_SuggestedTags.setLayoutParams(rlParams);

        } else if(viewModel_fragment_selectTags.bShowModeXrefTagUse) {
            //When bShowModeXrefTagUse is activated, we are using Fragment_SelectTags
            //  for filtering. In this case, only show tags which are used, that is,
            //  has a frequency count greater than zero. Don't give the option for the user to add
            //  tags via the tag editor.

            //Hide the TagEditor button:
            button_TagEdit.setVisibility(View.INVISIBLE);

            //Collapse the chipgroup showing recently-selected tags:
            RelativeLayout.LayoutParams rlParams = (RelativeLayout.LayoutParams) gChipGroup_SuggestedTags.getLayoutParams();
            rlParams.height = 0;
            gChipGroup_SuggestedTags.setLayoutParams(rlParams);

        } else {
            //User is applying tags.
            button_TagEdit.setOnClickListener(view12 -> {
                if (getActivity() == null) {
                    return;
                }

                //Launch the tag editor in a mode to allow retrieval of any result, such as new tag(s) created.
                Intent intentTagEditor = new Intent(getActivity(), Activity_TagEditor.class);
                intentTagEditor.putExtra(Activity_TagEditor.EXTRA_INT_MEDIA_CATEGORY, viewModel_fragment_selectTags.iMediaCategory);
                garlGetResultFromTagEditor.launch(intentTagEditor);

            });


            //Create adapations for "suggested tags":
            //Update a maintained list of suggested tags:
            //React to changes in the selected tag data in the ViewModel:
            final Observer<ArrayList<ItemClass_Tag>> selectedTagsObserver = selectedTags -> {

                //Update the suggested tags list in the viewmodel:
                ArrayList<ItemClass_Tag> alictSuggestions = viewModel_fragment_selectTags.altiTagSuggestions.getValue();
                ArrayList<ItemClass_Tag> alictNewSuggestions = viewModel_fragment_selectTags.altiTagSuggestions.getValue();
                if (alictSuggestions == null) {
                    alictSuggestions = new ArrayList<>();
                    alictNewSuggestions = new ArrayList<>();
                }
                for (ItemClass_Tag ict : selectedTags) {
                    boolean bNotInList = true;
                    for (ItemClass_Tag ictSuggestion : alictSuggestions) {
                        if (ict.iTagID.equals(ictSuggestion.iTagID)) {
                            bNotInList = false;
                            break;
                        }
                    }
                    if (bNotInList) {
                        if(alictNewSuggestions == null){
                            alictNewSuggestions = new ArrayList<>();
                        }
                        alictNewSuggestions.add(ict);
                    }
                }
                viewModel_fragment_selectTags.altiTagSuggestions.postValue(alictNewSuggestions);

            };
            viewModel_fragment_selectTags.altiTagsSelected.observe(getActivity(), selectedTagsObserver);

            //Watch for changes in suggested tags:
            //final Observer<ArrayList<ItemClass_Tag>> suggestedTagsObserver = this::updateSuggestedTagDisplay;
            final Observer<ArrayList<ItemClass_Tag>> suggestedTagsObserver = alTagSuggestions -> {

                //When a tag suggestion changes in the view model...
                // - User can select a tag in the main tag list, adding it to the item and also to suggested tags, but should not be displayed as a suggested tag,
                // - User can select a tag from the chip group (which displays the suggested tags), which should add it to the item and hide it from the chip group,
                // - User can X a tag from the chip group (which displays the suggested tags), which removes it from suggested tags.

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

                updateChips(alictValidSuggestions);
            };

            viewModel_fragment_selectTags.altiTagSuggestions.observe(getActivity(), suggestedTagsObserver);

        }

        if (getView() != null) {
            //Tag maturity view RangeSlider.
            //  This merely hides/shows tags based on user input.
            RangeSlider rangeSlider_TagMaturityWindow = getView().findViewById(R.id.rangeSlider_TagMaturityWindow);
            //Set max available maturity to the max allowed to the user:
            if(GlobalClass.gicuCurrentUser != null) {
                rangeSlider_TagMaturityWindow.setValueTo((float) GlobalClass.gicuCurrentUser.iMaturityLevel);
            } else {
                rangeSlider_TagMaturityWindow.setValueTo((float) GlobalClass.giDefaultUserMaturityRating);
            }
            //Set the current selected maturity window max to the default maturity rating:
            rangeSlider_TagMaturityWindow.setValues((float) GlobalClass.giMinTagMaturityFilter, (float) GlobalClass.giMaxTagMaturityFilter);

            rangeSlider_TagMaturityWindow.setLabelFormatter(value -> AdapterMaturityRatings.MATURITY_RATINGS[(int)value][0] + " - " + AdapterMaturityRatings.MATURITY_RATINGS[(int)value][1]);
            rangeSlider_TagMaturityWindow.addOnChangeListener((slider, value, fromUser) -> {
                List<Float> lfSliderValues = slider.getValues();
                if(lfSliderValues.size() == 2){
                    int iMinTemp = lfSliderValues.get(0).intValue();
                    int iMaxTemp = lfSliderValues.get(1).intValue();
                    if(iMinTemp != GlobalClass.giMinTagMaturityFilter ||
                        iMaxTemp != GlobalClass.giMaxTagMaturityFilter) {
                        GlobalClass.giMinTagMaturityFilter = lfSliderValues.get(0).intValue();
                        GlobalClass.giMaxTagMaturityFilter = lfSliderValues.get(1).intValue();
                    }
                    //Call routine to filter tags based on the updated maturity bounds:
                    initListViewData();
                }
            });
        } //End config of the tag maturity RangeSlider

    } //End onViewCreated.

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

        updateChips(alictValidSuggestions);

    }

    private void updateChips(ArrayList<ItemClass_Tag> alictValidSuggestions){

        for(ItemClass_Tag ict: alictValidSuggestions){
            //Don't add tags to the chip group from the 'valid suggestions' if the tag is already displayed:
            boolean bAddChip = true;
            for(int i = 0; i < gChipGroup_SuggestedTags.getChildCount(); i++){
                Chip chipExisting = (Chip) gChipGroup_SuggestedTags.getChildAt(i);
                if(chipExisting.getId() == ict.iTagID){
                    bAddChip = false;
                    break;
                }
            }
            if(bAddChip) {
                Chip chipNew = new Chip(getContext());
                chipNew.setText(ict.sTagText);
                chipNew.setId(ict.iTagID);
                chipNew.setOnClickListener((view) -> {
                    ArrayList<Integer> aliTagIDs = new ArrayList<>();
                    aliTagIDs.add(view.getId());
                    gListViewTagsAdapter.selectTagsByIDs(aliTagIDs);
                    gChipGroup_SuggestedTags.removeView(view); //Remove this chip from the group.
                });
                chipNew.setCloseIconVisible(true);
                chipNew.setOnCloseIconClickListener((view)->{
                    //Remove the item from suggested tags:
                    int iTagToRemove = view.getId();
                    //Update the suggested tags list in the viewmodel:
                    ArrayList<ItemClass_Tag> alictSuggestions = viewModel_fragment_selectTags.altiTagSuggestions.getValue();
                    ArrayList<ItemClass_Tag> alictNewSuggestions = new ArrayList<>();
                    if(alictSuggestions != null) {
                        for (ItemClass_Tag ictSuggestion : alictSuggestions) {
                            if (!ictSuggestion.iTagID.equals(iTagToRemove)) {
                                alictNewSuggestions.add(ictSuggestion);
                            }
                        }
                    }
                    gChipGroup_SuggestedTags.removeView(view); //Remove this chip from the group.
                    viewModel_fragment_selectTags.altiTagSuggestions.postValue(alictNewSuggestions);
                });
                gChipGroup_SuggestedTags.addView(chipNew);

                //Now sort the chips:
                TreeMap<String, Chip> tmChipsSorted = new TreeMap<>();
                for(int i=0; i < gChipGroup_SuggestedTags.getChildCount(); i++){
                    Chip chip = (Chip) gChipGroup_SuggestedTags.getChildAt(i);
                    tmChipsSorted.put(chip.getText().toString(), chip);
                }
                gChipGroup_SuggestedTags.removeAllViews();
                for(Map.Entry<String, Chip> entry: tmChipsSorted.entrySet()){
                    gChipGroup_SuggestedTags.addView(entry.getValue());
                }
            }
        }

    }




    @Override
    public void onResume() {
        super.onResume();
        //recalcUserColor();
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

        viewModel_fragment_selectTags.alTagsAll.clear();

        //Grab all tags that fall within the maturity range selected by the user:
        TreeMap<Integer, ItemClass_Tag> tmTagPool_PreMaturityFilter = GlobalClass.gtmApprovedCatalogTagReferenceLists.get(viewModel_fragment_selectTags.iMediaCategory);
        TreeMap<Integer, ItemClass_Tag> tmTagPool = new TreeMap<>();
        for(Map.Entry<Integer, ItemClass_Tag> entry: tmTagPool_PreMaturityFilter.entrySet()){
            if(entry.getValue().iMaturityRating >= GlobalClass.giMinTagMaturityFilter &&
                entry.getValue().iMaturityRating <= GlobalClass.giMaxTagMaturityFilter){
                tmTagPool.put(entry.getKey(), entry.getValue());
            }
        }

        ArrayList<ItemClass_Tag> alict_SelectedTagsViewModelReset = new ArrayList<>();

        //Go through the tags treeMap and put the ListView together:
        for (Map.Entry<Integer, ItemClass_Tag> tmEntryTagReferenceItem : tmTagPool.entrySet()) {

            //Check to see if the list of preselected tags includes this tag from the reference list.
            //  If so, set the item as "checked":
            boolean bIsChecked = false;

            if (galiPreselectedTags != null) {
                int iReferenceTagID = tmEntryTagReferenceItem.getValue().iTagID;
                for (int iPreSelectedTagID : galiPreselectedTags) {
                    if (iReferenceTagID == iPreSelectedTagID) {
                        bIsChecked = true;
                        break;
                    }
                }
            }

            if (galNewTags != null) { //If the user used the TagEditor JUST NOW to add new tags,
                //  pre-select them:
                int iReferenceTagID = tmEntryTagReferenceItem.getValue().iTagID;
                for (ItemClass_Tag iNewTagItem : galNewTags) {
                    if (iReferenceTagID == iNewTagItem.iTagID) {
                        bIsChecked = iNewTagItem.bIsChecked;
                        iNewTagItem.bIsChecked = false; //Only mark as selected once, as the user might not want this tag on the next item they view.
                        break;
                    }
                }
            }

            ItemClass_Tag ictNew = new ItemClass_Tag(
                    tmEntryTagReferenceItem.getValue().iTagID,
                    tmEntryTagReferenceItem.getValue().sTagText);
            ictNew.bIsChecked = bIsChecked;
            ictNew.iMaturityRating = tmEntryTagReferenceItem.getValue().iMaturityRating;
            ictNew.iHistogramCount = tmEntryTagReferenceItem.getValue().iHistogramCount;
            if(tmEntryTagReferenceItem.getValue().alsTagApprovedUsers == null){
                ictNew.alsTagApprovedUsers = new ArrayList<>();
            } else {
                ictNew.alsTagApprovedUsers = new ArrayList<>(tmEntryTagReferenceItem.getValue().alsTagApprovedUsers);
            }

            viewModel_fragment_selectTags.alTagsAll.add(ictNew);
            if (ictNew.bIsChecked) {
                alict_SelectedTagsViewModelReset.add(ictNew);
            }

        }

        //Reset the selected tags stored in the viewmodel:
        viewModel_fragment_selectTags.setSelectedTags(alict_SelectedTagsViewModelReset);

        galNewTags = null; //Mark galNewTags as empty.

        // Create the adapter for the ListView, and set the ListView adapter:
        if(getActivity() == null){
            return;
        }
        gListViewTagsAdapter = new ListViewTagsAdapter(getActivity().getApplicationContext(), viewModel_fragment_selectTags.alTagsAll);
        listView_ImportTagSelection.setAdapter(gListViewTagsAdapter);
        listView_ImportTagSelection.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        //Initialize suggested tags by sending an updated list of what is selected:
        InitializeSuggestedTagDisplay();


    }

    @SuppressWarnings("unchecked")
    ActivityResultLauncher<Intent> garlGetResultFromTagEditor = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //Get result from TagEditor Activity:
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data == null) return;
                        Bundle b = data.getBundleExtra(Activity_TagEditor.EXTRA_BUNDLE_TAG_EDITOR_NEW_TAGS_RESULT);
                        if (b != null) {
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
                                if (altiExistingSelectedTags == null) {
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
                        if (bTagRenamed || bTagDeleted) {
                            //Update selected tags held in the ViewModel for the event that the user renamed some of the
                            //  tags that the user had already selected:
                            ArrayList<ItemClass_Tag> altiExistingSelectedTags = viewModel_fragment_selectTags.altiTagsSelected.getValue();
                            if (altiExistingSelectedTags != null) {
                                ArrayList<ItemClass_Tag> altiUpdatedExistingSelectedTags = new ArrayList<>();
                                for (ItemClass_Tag ict : altiExistingSelectedTags) {
                                    if (globalClass.TagIDExists(ict.iTagID, viewModel_fragment_selectTags.iMediaCategory)) {
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

        ArrayList<ItemClass_Tag> alictTagItems; //Contains all tag items passed to the listviewTagsAdapter.
        TreeMap<String, ItemClass_Tag> tmTagItemsDisplay;
        TreeMap<Integer, String> tmTagItemsDisplaySequence;

        public ListViewTagsAdapter(Context context, ArrayList<ItemClass_Tag> tagItems) {
            super(context, 0, tagItems);
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
        public View getView(int position, View viewRow, @NonNull ViewGroup parent) {
            // Get the data item for this position

            String sCompoundID = tmTagItemsDisplaySequence.get(position);
            final ItemClass_Tag tagItem = tmTagItemsDisplay.get(sCompoundID);

            if(tagItem == null){
                return viewRow;
            }
            // Check if an existing view is being reused, otherwise inflate the view
            if (viewRow == null) {
                viewRow = LayoutInflater.from(getContext()).inflate(R.layout.listview_tag_item_select_tags_fragment, parent, false);
            }
            // Populate the tag text:
            final TextView textView_TagText = viewRow.findViewById(R.id.textView_TagText);
            textView_TagText.setText(tagItem.sTagText);

            TextView textView_HistogramCount = viewRow.findViewById(R.id.textView_HistogramCount);
            if(viewModel_fragment_selectTags.bShowModeXrefTagUse) {
                //Attempt to get usage (histogram) data for the tag:
                int iTagUseCount;
                iTagUseCount = tagItem.iHistogramCount;
                String s;
                if (iTagUseCount != 0) {
                    s = "(" + iTagUseCount + ")";
                    textView_HistogramCount.setText(s);
                    textView_HistogramCount.setVisibility(View.VISIBLE);
                } else {
                    textView_HistogramCount.setText("");
                    textView_HistogramCount.setVisibility(View.INVISIBLE);
                }
            } else {
                textView_HistogramCount.setText("");
                textView_HistogramCount.setVisibility(View.INVISIBLE);
            }

            //Set the maturity rating code text for the readout:
            TextView textView_Maturity = viewRow.findViewById(R.id.textView_Maturity);
            String sMaturityCode = AdapterMaturityRatings.MATURITY_RATINGS[tagItem.iMaturityRating][AdapterMaturityRatings.MATURITY_RATING_CODE_INDEX];
            sMaturityCode = "(" + sMaturityCode + ")";
            textView_Maturity.setText(sMaturityCode);

            TextView textView_Private = viewRow.findViewById(R.id.textView_Private);
            if(tagItem.alsTagApprovedUsers.size() == 1){
                textView_Private.setVisibility(View.VISIBLE);
                //textView_Private.getLayoutParams().width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                textView_Private.setText("(P)");
            } else {
                textView_Private.setVisibility(View.INVISIBLE);
                textView_Private.setText("");
            }

            //Set the selection state (needed as views are recycled).
            if(getActivity() != null) {
                if (tagItem.bIsChecked) {
                    textView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                } else {
                    textView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorUnfilledUnselected));
                }
                textView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
            }

            viewRow.setOnClickListener(view -> {
                //Handle changing the checked state:
                tagItem.bIsChecked = !tagItem.bIsChecked;
                if(tagItem.bIsChecked){
                    textView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                    textView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
                    if(galiPreselectedTags == null){
                        galiPreselectedTags = new ArrayList<>();
                    }
                    galiPreselectedTags.add(tagItem.iTagID);
                } else {
                    textView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorUnfilledUnselected));
                    textView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
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
                //  and using a TreeMap to automatically sort the items alphabetically:
                TreeMap<String, ItemClass_Tag> tmSelectedItems = new TreeMap<>();
                for(Map.Entry<String, ItemClass_Tag> entry: tmTagItemsDisplay.entrySet()){
                    if (entry.getValue().bIsChecked) {
                        tmSelectedItems.put(entry.getValue().sTagText, entry.getValue());
                    }
                }

                //Put the sorted TreeList items into an ArrayList and transfer to the ViewModel:
                ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();

                for (Map.Entry<String, ItemClass_Tag> entry : tmSelectedItems.entrySet()) {
                    alTagItems.add(entry.getValue());
                }

                viewModel_fragment_selectTags.setSelectedTags(alTagItems);

                if(viewModel_fragment_selectTags.bShowModeXrefTagUse) {
                    updateXrefTagsHistogram();
                    notifyDataSetChanged();//todo: should this be moved outside of this if-statement?
                }

            });


            // Return the completed view to render on screen
            return viewRow;
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
                //  and using a TreeMap to automatically sort the items alphabetically:
                TreeMap<String, ItemClass_Tag> tmSelectedItems = new TreeMap<>();

                for (Map.Entry<String, ItemClass_Tag> entry: tmTagItemsDisplay.entrySet()){
                    if (entry.getValue().bIsChecked) {
                        tmSelectedItems.put(entry.getValue().sTagText, entry.getValue());
                    }
                }

                //Put the sorted TreeList items into an ArrayList and transfer to the ViewModel:
                ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();

                for (Map.Entry<String, ItemClass_Tag> entry : tmSelectedItems.entrySet()) {
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
            TreeMap<Integer, ItemClass_Tag> tmXrefTagHistogram_PreMaturityFilter =
                    globalClass.getXrefTagHistogram(viewModel_fragment_selectTags.iMediaCategory, galiPreselectedTags);

            //Only show tags which are within the maturity display bounds selected by the user:
            TreeMap<Integer, ItemClass_Tag> tmXrefTagHistogram = new TreeMap<>();
            for(Map.Entry<Integer, ItemClass_Tag> entry: tmXrefTagHistogram_PreMaturityFilter.entrySet()){
                if(entry.getValue().iMaturityRating >= GlobalClass.giMinTagMaturityFilter &&
                        entry.getValue().iMaturityRating <= GlobalClass.giMaxTagMaturityFilter){
                    tmXrefTagHistogram.put(entry.getKey(), entry.getValue());
                }
            }

            //Mark tags selected as appropriate:
            for(int iTagID: galiPreselectedTags){
                if(tmXrefTagHistogram.get(iTagID) != null) {
                    tmXrefTagHistogram.get(iTagID).bIsChecked = true;
                }
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