package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;


public class Fragment_CatalogSort extends Fragment {

    GlobalClass globalClass;

    int giMediaCategory;

    TreeSet<Integer> gtsiSelectedTagIDs;

    ViewModel_Fragment_SelectTags gViewModel_fragment_selectTags;

    public Fragment_SelectTags gFragment_selectTags;


    final int DROPDOWN_SORTBY_ITEM_IMPORT_DATE = 0;
    final int DROPDOWN_SORTBY_ITEM_LAST_VIEWED_DATE = 1;
    final String[] gsDropDownSortByItems ={"Import Date","Last Read Date"};

    String[] gsSharedWithUsersNameArray = {""};

    boolean gbCatalogViewerSortAscending;

    final String[] gsFilterByItems = {
            "",
            "Web sources only",
            "Folder sources only",
            "Items with no tags",
            "Items with error"};


    public Fragment_CatalogSort() {
        // Required empty public constructor
    }

    public static Fragment_CatalogSort newInstance() {
        return new Fragment_CatalogSort();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_catalog_sort, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Get the Media Category argument passed to this fragment:
        Bundle args = getArguments();

        if(args != null) {
            giMediaCategory = args.getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
        }
        if(getActivity() == null){
            return;
        }
        globalClass = (GlobalClass) getActivity().getApplicationContext();

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        gViewModel_fragment_selectTags = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_SelectTags.class);

        gViewModel_fragment_selectTags.altiTagsSelected.removeObservers(getViewLifecycleOwner());

        //Populate the tags fragment:
        //Start the tag selection fragment:
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        gFragment_selectTags = new Fragment_SelectTags();
        Bundle fragment_selectTags_args = new Bundle();
        fragment_selectTags_args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, giMediaCategory);
        //If there are any tags previously selected, such as user left the activity and then returned, restore them:
        if(GlobalClass.galtsiCatalogViewerFilterTags != null) {
            if (GlobalClass.galtsiCatalogViewerFilterTags.size() > 0) {
                if (GlobalClass.galtsiCatalogViewerFilterTags.get(giMediaCategory).size() > 0) {

                    //Quickly go through the list of pre-selected tags to see if there are any that
                    // don't have any matches. If there are such items, remove them from the list.
                    // The case is that the CatalogSort tag list does not present to the user any
                    // tags for selection that are not assigned to items. If the user was filtering
                    // by a particular tag and deleted the last item that was assigned to that tag,
                    // thus the tag should no longer be in the list, the tag will get stuck in the filter with it unable to
                    // be cleared without resetting the activity. This happens if the user exits the catalog
                    // viewer without unselecting the zero-item tag.
                    //First get a list of all of the tags in-use:
                    TreeMap<Integer, ItemClass_Tag> tmXrefTagHistogram;
                    tmXrefTagHistogram =
                                globalClass.getXrefTagHistogram(
                                        giMediaCategory,
                                        new ArrayList<>(),
                                        false);
                    //Create a place for tag IDs in the "pre-selected" list that are not in the histogram:
                    ArrayList<Integer> aliIDsToUnselect = new ArrayList<>();
                    //Check to see if there are any tags in the "pre-selected" list that need to be removed:
                    for(Integer iTagID: GlobalClass.galtsiCatalogViewerFilterTags.get(giMediaCategory)){
                        if(!tmXrefTagHistogram.containsKey(iTagID)){
                            //If the tag ID is not included, mark it for removal from the set.
                            //This should be a very rare occurrence.
                            aliIDsToUnselect.add(iTagID);
                        }
                    }
                    if (aliIDsToUnselect.size() > 0) {
                        for(Integer iTagToUnselect: aliIDsToUnselect){
                            GlobalClass.galtsiCatalogViewerFilterTags.get(giMediaCategory).remove(iTagToUnselect);
                        }
                    }
                    //Continue with the operation of initializing the fragment with pre-selected tags:
                    TreeSet<Integer> tsiTagIDs = GlobalClass.galtsiCatalogViewerFilterTags.get(giMediaCategory);
                    ArrayList<Integer> aliTagIDs = new ArrayList<>(tsiTagIDs);
                    fragment_selectTags_args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, aliTagIDs);
                }
            }
        }
        gFragment_selectTags.setArguments(fragment_selectTags_args);
        fragmentTransaction.replace(R.id.child_fragment_tag_selector, gFragment_selectTags);
        fragmentTransaction.commit();

        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<ItemClass_Tag>> observerSelectedTags = getNewTagObserver();
        gViewModel_fragment_selectTags.altiTagsSelected.observe(getViewLifecycleOwner(), observerSelectedTags);




        if (getView() != null) {

            //Configure the 'Sort by' "exposed dropdown menu":
            AutoCompleteTextView autoCompleteTextView_SortBy = getView().findViewById(R.id.autoCompleteTextView_SortBy);
            //wrap the items in the Adapter
            ArrayAdapter<String> adapterSortBy = new ArrayAdapter<>(getActivity(), R.layout.catalog_dropdown_item_sort_search_filter, gsDropDownSortByItems);
            //assign adapter to the dropdown menu:
            autoCompleteTextView_SortBy.setAdapter(adapterSortBy);

            //Initialize the drop-down menu position:
            if(GlobalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_IMPORTED){
                autoCompleteTextView_SortBy.setText(adapterSortBy.getItem(DROPDOWN_SORTBY_ITEM_IMPORT_DATE), false);
            } else if(GlobalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_LAST_VIEWED){
                autoCompleteTextView_SortBy.setText(adapterSortBy.getItem(DROPDOWN_SORTBY_ITEM_LAST_VIEWED_DATE), false);
            }

            autoCompleteTextView_SortBy.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    enableApply();
                }
            });

            //Initialize the SortOrder ImageButton:
            final ImageButton imageButton_SortOrder = getView().findViewById(R.id.imageButton_SortOrder);
            if(GlobalClass.gbCatalogViewerSortAscending[GlobalClass.giSelectedCatalogMediaCategory]){
                imageButton_SortOrder.setImageResource(R.drawable.baseline_sort_ascending_white_18dp);
                gbCatalogViewerSortAscending = true;
            } else {
                imageButton_SortOrder.setImageResource(R.drawable.baseline_sort_descending_white_18dp);
                gbCatalogViewerSortAscending = false;
            }
            imageButton_SortOrder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(gbCatalogViewerSortAscending){
                        gbCatalogViewerSortAscending = false;
                        imageButton_SortOrder.setImageResource(R.drawable.baseline_sort_descending_white_18dp);
                    } else {
                        gbCatalogViewerSortAscending = true;
                        imageButton_SortOrder.setImageResource(R.drawable.baseline_sort_ascending_white_18dp);
                    }
                    enableApply();
                }
            });



            //Configure the "Shared with User" filter:
            AutoCompleteTextView autoCompleteTextView_SharedWithUser = getView().findViewById(R.id.autoCompleteTextView_SharedWithUser);

            //wrap the items in the Adapter
            int iExtraUserNames = 1; //Used to populate extra selection fields
            gsSharedWithUsersNameArray = new String[GlobalClass.galicu_Users.size() + iExtraUserNames];
            gsSharedWithUsersNameArray[0] = ""; //The "Anyone" selection.
            for(int i = iExtraUserNames; i < GlobalClass.galicu_Users.size() + iExtraUserNames; i++){
                gsSharedWithUsersNameArray[i] = GlobalClass.galicu_Users.get(i - iExtraUserNames).sUserName;
            }
            ArrayAdapter<String> adapterSharedWithUser = new ArrayAdapter<>(getActivity(), R.layout.catalog_dropdown_item_sort_search_filter, gsSharedWithUsersNameArray);
            autoCompleteTextView_SharedWithUser.setAdapter(adapterSharedWithUser);

            //Initialize the data:
            for(int i = 0; i < GlobalClass.galicu_Users.size(); i++){
                if(GlobalClass.gsCatalogViewerSortBySharedWithUser.equals(gsSharedWithUsersNameArray[i])){
                    autoCompleteTextView_SharedWithUser.setText(adapterSharedWithUser.getItem(i), false);
                    break;
                }
            }

            autoCompleteTextView_SharedWithUser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    enableApply();
                }
            });





            //Listen to when the Keywords EditText field is modified to enable the Apply button:
            EditText editText_SearchKeywords = requireView().findViewById(R.id.editText_SearchKeywords);
            if(!GlobalClass.gsCatalogViewerSearchInText[GlobalClass.giSelectedCatalogMediaCategory].equals("")){
                editText_SearchKeywords.setText(GlobalClass.gsCatalogViewerSearchInText[GlobalClass.giSelectedCatalogMediaCategory]);
            }
            if (editText_SearchKeywords != null) {
                editText_SearchKeywords.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        enableApply();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
                editText_SearchKeywords.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if(!hasFocus){
                            //Hide the soft keyboard if the editText no longer has focus:
                            InputMethodManager imm =  (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                    }
                });
            } //end config of editText_Keywords.


            //Configure the "FilterBy" drop-down menu:
            AutoCompleteTextView autoCompleteTextView_FilterBy = getView().findViewById(R.id.autoCompleteTextView_FilterBy);
            //wrap the items in the Adapter
            ArrayAdapter<String> adapterFilterBy = new ArrayAdapter<>(getActivity(), R.layout.catalog_dropdown_item_sort_search_filter, gsFilterByItems);
            //assign adapter:
            autoCompleteTextView_FilterBy.setAdapter(adapterFilterBy);

            //Initialize the "FilterBy" text:
            String sFilterBySelection = gsFilterByItems[GlobalClass.giCatalogViewerFilterBySelection[GlobalClass.giSelectedCatalogMediaCategory]];
            autoCompleteTextView_FilterBy.setText(sFilterBySelection, false);

            autoCompleteTextView_FilterBy.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    enableApply();
                }
            });


            //Configure the resolution range slider:
            if (getView() != null) {
                RangeSlider rangeSlider_Resolution = getView().findViewById(R.id.rangeSlider_Resolution);
                if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                    //Configure the rangeSlider to have discrete steps:
                    float fMaxPositionCount = GlobalClass.gtmVideoResolutions.size() - 1;
                    rangeSlider_Resolution.setValueTo(Math.max(1.0F, fMaxPositionCount));
                    rangeSlider_Resolution.setValues(0.0F, Math.max(1.0F, fMaxPositionCount));
                    rangeSlider_Resolution.setStepSize(1.0f);
                    rangeSlider_Resolution.setLabelFormatter(new LabelFormatter() {
                        @NonNull
                        @Override
                        public String getFormattedValue(float value) {
                            int iValue = (int) value;
                            return GlobalClass.gtmVideoResolutions.get(iValue) + "p";
                        }
                    });
                    rangeSlider_Resolution.addOnChangeListener(new RangeSlider.OnChangeListener() {
                        @Override
                        public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                            enableApply();
                        }
                    });
                } else if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                    rangeSlider_Resolution.setValueTo(Math.max(1.0F, (float) GlobalClass.giMaxImageMegaPixels));
                    rangeSlider_Resolution.setValues(Math.max(0.0F, (float) GlobalClass.giMinImageMegaPixels),
                            Math.max(1.0F, (float) GlobalClass.giMaxImageMegaPixels));
                    rangeSlider_Resolution.setLabelFormatter(new LabelFormatter() {
                        @NonNull
                        @Override
                        public String getFormattedValue(float value) {
                            return String.format(Locale.getDefault(), "%01.1fMP", value);
                        }
                    });
                    rangeSlider_Resolution.addOnChangeListener(new RangeSlider.OnChangeListener() {
                        @Override
                        public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                            enableApply();
                        }
                    });
                } else if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                    TextView textView_labelResolution = getView().findViewById(R.id.textView_labelResolution);
                    if (textView_labelResolution != null) {
                        textView_labelResolution.setText("Comic page count:");
                    }
                    rangeSlider_Resolution.setValueTo(Math.max(1.0F, (float) GlobalClass.giMaxComicPageCount));
                    rangeSlider_Resolution.setValues(Math.max(0.0F, (float) GlobalClass.giMinComicPageCount),
                            Math.max(1.0F, (float) GlobalClass.giMaxComicPageCount));
                    rangeSlider_Resolution.setLabelFormatter(new LabelFormatter() {
                        @NonNull
                        @Override
                        public String getFormattedValue(float value) {
                            int iValue = (int) value;
                            return iValue + "";
                        }
                    });
                    rangeSlider_Resolution.addOnChangeListener(new RangeSlider.OnChangeListener() {
                        @Override
                        public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                            enableApply();
                        }
                    });
                }


            } //End config of the resolution RangeSlider

            //Configure the video duration range slider:
            if (getView() != null) {
                if(GlobalClass.giSelectedCatalogMediaCategory != GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                    //Hide the video duration RangeSlider if we are not viewing videos:
                    RelativeLayout relativeLayout_VideoDuration = getView().findViewById(R.id.relativeLayout_VideoDuration);
                    if (relativeLayout_VideoDuration != null) {
                        relativeLayout_VideoDuration.getLayoutParams().height = 0;
                        relativeLayout_VideoDuration.requestLayout();
                    }
                }

                RangeSlider rangeSlider_VideoDuration = getView().findViewById(R.id.rangeSlider_VideoDuration);
                float fMaxMilliseconds = GlobalClass.glMaxVideoDurationMS;
                rangeSlider_VideoDuration.setValueTo(Math.max(1.0F, fMaxMilliseconds));
                rangeSlider_VideoDuration.setValues(0.0F, Math.max(1.0F, fMaxMilliseconds));
                rangeSlider_VideoDuration.setLabelFormatter(new LabelFormatter() {
                    @NonNull
                    @Override
                    public String getFormattedValue(float value) {
                        long lHours = TimeUnit.MILLISECONDS.toHours((long) value);
                        long lMinutes = TimeUnit.MILLISECONDS.toMinutes((long) value) - lHours * 60;
                        long lSeconds = TimeUnit.MILLISECONDS.toSeconds((long) value) - lHours * 3600 - lMinutes * 60;
                        String sTime;
                        if(lHours == 0){
                            sTime = String.format(Locale.getDefault(),"%d:%02d", lMinutes, lSeconds);
                        } else {
                            sTime = String.format(Locale.getDefault(),"%d:%02d:%02d", lHours, lMinutes, lSeconds);
                        }
                        return sTime;
                    }
                });
                rangeSlider_VideoDuration.addOnChangeListener(new RangeSlider.OnChangeListener() {
                    @Override
                    public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                        enableApply();
                    }
                });
            } //End config of the video duration RangeSlider


            //Configure the maturity filter rangeslider:
            RangeSlider rangeSlider_MaturityFilter = getView().findViewById(R.id.rangeSlider_MaturityFilter);
            //Set max available maturity to the max allowed to the user:
            if(GlobalClass.gicuCurrentUser != null) {
                rangeSlider_MaturityFilter.setValueTo((float) GlobalClass.gicuCurrentUser.iMaturityLevel);
            } else {
                rangeSlider_MaturityFilter.setValueTo((float) GlobalClass.giDefaultUserMaturityRating);
            }
            rangeSlider_MaturityFilter.setStepSize((float) 1);
            //Set the current selected maturity window max to the default maturity rating:
            rangeSlider_MaturityFilter.setValues((float) GlobalClass.giMinContentMaturityFilter, (float) GlobalClass.giMaxContentMaturityFilter);

            rangeSlider_MaturityFilter.setLabelFormatter(value -> AdapterMaturityRatings.MATURITY_RATINGS[(int)value][0] + " - " + AdapterMaturityRatings.MATURITY_RATINGS[(int)value][1]);
            rangeSlider_MaturityFilter.addOnChangeListener((slider, value, fromUser) -> {
                enableApply();
            });




            //Configure the APPLY button listener:
            final Button button_Apply = getView().findViewById(R.id.button_Apply);
            if (button_Apply != null) {
                button_Apply.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //On Apply filter....
                        apply();

                        //Record that the user has used this feature. This count is recorded, and
                        //  is used to reduce impact of features used to train the user.
                        GlobalClass.giDrawerUseCntCatBrowser++;
                        if(getContext() != null) {
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                            String sPreferenceNamePrefix = GlobalClass.gicuCurrentUser.sUserName;
                            String sPrefName_DrawerUseCntCatBrowser = sPreferenceNamePrefix + GlobalClass.USR_DRAWER_USE_CNT_SUFX_CATALOG_BROWSER;
                            sharedPreferences.edit()
                                    .putInt(sPrefName_DrawerUseCntCatBrowser, GlobalClass.giDrawerUseCntCatBrowser)
                                    .apply();
                        }
                    }
                });
            } //end config of button_Apply.
        }

        initData();

    }


    private void apply(){
        if(getView() == null){
            return;
        }

        //Read SortBy:
        AutoCompleteTextView autoCompleteTextView_SortBy = getView().findViewById(R.id.autoCompleteTextView_SortBy);
        String sSortBySelection = autoCompleteTextView_SortBy.getText().toString();
        if(sSortBySelection.equals(gsDropDownSortByItems[DROPDOWN_SORTBY_ITEM_IMPORT_DATE])) {
            GlobalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        } else if(sSortBySelection.equals(gsDropDownSortByItems[DROPDOWN_SORTBY_ITEM_LAST_VIEWED_DATE])) {
            GlobalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory] = GlobalClass.SORT_BY_DATETIME_LAST_VIEWED;
        }
        //Record the user's selected sort item:
        if(getActivity() != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            sharedPreferences.edit()
                    .putInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[GlobalClass.giSelectedCatalogMediaCategory],
                            GlobalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory])
                    .apply();
        }

        //Read SortOrder:
        if(GlobalClass.gbCatalogViewerSortAscending[GlobalClass.giSelectedCatalogMediaCategory] != gbCatalogViewerSortAscending) {
            GlobalClass.gbCatalogViewerSortAscending[GlobalClass.giSelectedCatalogMediaCategory] = gbCatalogViewerSortAscending;
            //Record the user's selected sort order:
            if(getActivity() != null) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                sharedPreferences.edit()
                        .putBoolean(GlobalClass.gsCatalogViewerPreferenceNameSortAscending[GlobalClass.giSelectedCatalogMediaCategory],
                                GlobalClass.gbCatalogViewerSortAscending[GlobalClass.giSelectedCatalogMediaCategory])
                        .apply();
            }
        }

        //Read SharedWithUser selection:
        AutoCompleteTextView autoCompleteTextView_SharedWithUser = getView().findViewById(R.id.autoCompleteTextView_SharedWithUser);
        String sSharedUser = autoCompleteTextView_SharedWithUser.getText().toString();
        GlobalClass.gsCatalogViewerSortBySharedWithUser = sSharedUser;


        //Apply any text search to the search hold in globalClass:
        EditText editText_SearchKeywords = getView().findViewById(R.id.editText_SearchKeywords);
        if (editText_SearchKeywords != null) {
            String sSearchKeywords = editText_SearchKeywords.getText().toString();
            GlobalClass.gsCatalogViewerSearchInText[GlobalClass.giSelectedCatalogMediaCategory] = sSearchKeywords;
        } else {
            GlobalClass.gsCatalogViewerSearchInText[GlobalClass.giSelectedCatalogMediaCategory] = "";
        }

        //Read FilterBy Selection:
        AutoCompleteTextView autoCompleteTextView_FilterBy = getView().findViewById(R.id.autoCompleteTextView_FilterBy);
        String sFilterBySelection = autoCompleteTextView_FilterBy.getText().toString();
        for(int i = 0; i < 5; i++){
            if(sFilterBySelection.equals(gsFilterByItems[i])){
                GlobalClass.giCatalogViewerFilterBySelection[GlobalClass.giSelectedCatalogMediaCategory] = i;
                break;
            }
        }


        //Read Resolution/PageCount RangeSlider:
        RangeSlider rangeSlider_Resolution = getView().findViewById(R.id.rangeSlider_Resolution);
        List<Float> lfRangeSliderSelectedMinMaxValues = rangeSlider_Resolution.getValues();
        if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            if(lfRangeSliderSelectedMinMaxValues.get(0) == 0) {
                GlobalClass.giMinVideoResolutionSelected = -1;  //Mark as no value selected.
            } else {
                GlobalClass.giMinVideoResolutionSelected = lfRangeSliderSelectedMinMaxValues.get(0).intValue();
            }
            float fMaxPositionCount = GlobalClass.gtmVideoResolutions.size() - 1;
            if(lfRangeSliderSelectedMinMaxValues.get(1) == fMaxPositionCount) {
                GlobalClass.giMaxVideoResolutionSelected = -1;  //Mark as no value selected.
            } else {
                GlobalClass.giMaxVideoResolutionSelected = lfRangeSliderSelectedMinMaxValues.get(1).intValue();
            }
        } else if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
            if(lfRangeSliderSelectedMinMaxValues.get(0) == (float) GlobalClass.giMinImageMegaPixels) {
                GlobalClass.giMinImageMegaPixelsSelected = -1;  //Mark as no value selected.
            } else {
                GlobalClass.giMinImageMegaPixelsSelected = lfRangeSliderSelectedMinMaxValues.get(0).intValue();
            }
            if(lfRangeSliderSelectedMinMaxValues.get(1) == (float) GlobalClass.giMaxImageMegaPixels) {
                GlobalClass.giMaxImageMegaPixelsSelected = -1;  //Mark as no value selected.
            } else {
                GlobalClass.giMaxImageMegaPixelsSelected = lfRangeSliderSelectedMinMaxValues.get(1).intValue();
            }
        } else if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
            if(lfRangeSliderSelectedMinMaxValues.get(0) == (float) GlobalClass.giMinComicPageCount) {
                GlobalClass.giMinComicPageCountSelected = -1;  //Mark as no value selected.
            } else {
                GlobalClass.giMinComicPageCountSelected = lfRangeSliderSelectedMinMaxValues.get(0).intValue();
            }
            if(lfRangeSliderSelectedMinMaxValues.get(1) == (float) GlobalClass.giMaxComicPageCount) {
                GlobalClass.giMaxComicPageCountSelected = -1;  //Mark as no value selected.
            } else {
                GlobalClass.giMaxComicPageCountSelected = lfRangeSliderSelectedMinMaxValues.get(1).intValue();
            }
        }

        //Read Duration RangeSlider:
        RangeSlider rangeSlider_VideoDuration = getView().findViewById(R.id.rangeSlider_VideoDuration);
        lfRangeSliderSelectedMinMaxValues = rangeSlider_VideoDuration.getValues();
        if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            if (lfRangeSliderSelectedMinMaxValues.get(0) == 0.0f) {
                GlobalClass.glMinVideoDurationMSSelected = -1;
            } else {
                GlobalClass.glMinVideoDurationMSSelected = lfRangeSliderSelectedMinMaxValues.get(0).longValue();
            }
            if (lfRangeSliderSelectedMinMaxValues.get(1) == (float) GlobalClass.glMaxVideoDurationMS) {
                GlobalClass.glMaxVideoDurationMSSelected = -1;
            } else {
                GlobalClass.glMaxVideoDurationMSSelected = lfRangeSliderSelectedMinMaxValues.get(1).longValue();
            }
        }

        //Apply maturity filter if modified by user:
        RangeSlider rangeSlider_MaturityFilter = getView().findViewById(R.id.rangeSlider_MaturityFilter);
        List<Float> lfSliderValues = rangeSlider_MaturityFilter.getValues();
        if(lfSliderValues.size() == 2){
            int iMinTemp = lfSliderValues.get(0).intValue();
            int iMaxTemp = lfSliderValues.get(1).intValue();
            if(iMinTemp != GlobalClass.giMinContentMaturityFilter ||
                    iMaxTemp != GlobalClass.giMaxContentMaturityFilter) {
                GlobalClass.giMinContentMaturityFilter = lfSliderValues.get(0).intValue();
                GlobalClass.giMaxContentMaturityFilter = lfSliderValues.get(1).intValue();
            }
        }


        //Apply any tag filters to the filter hold in globalClass:
        GlobalClass.galtsiCatalogViewerFilterTags.get(giMediaCategory).clear();
        if (gtsiSelectedTagIDs != null) {
            if(gtsiSelectedTagIDs.size() > 0) {
                for (Integer iTagID : gtsiSelectedTagIDs) {
                    GlobalClass.galtsiCatalogViewerFilterTags.get(giMediaCategory).add(iTagID);
                }
            }
        }

        //Process the filter settings:
        ((Activity_CatalogViewer) requireActivity()).populate_RecyclerViewCatalogItems();

        Button button_Apply = getView().findViewById(R.id.button_Apply);
        button_Apply.setEnabled(false);

        if (getActivity() != null) {
            ((Activity_CatalogViewer) getActivity()).CloseSortDrawer();
        }
    }




    private Observer<ArrayList<ItemClass_Tag>> getNewTagObserver() {
        return new Observer<ArrayList<ItemClass_Tag>>() {
            @Override
            public void onChanged(ArrayList<ItemClass_Tag> tagItems) {

                //Get the tag IDs, sorted:
                TreeSet<Integer> tsiNewTagIDs = new TreeSet<>();
                for (ItemClass_Tag ti : tagItems) {
                    tsiNewTagIDs.add(ti.iTagID);
                }

                //Enable the Apply button is the user has selected new tags:
                if(gtsiSelectedTagIDs == null){
                    //If this is an initialization...
                    if(tsiNewTagIDs.size() > 0) {
                        //If there are already selected tag items to apply to a sort...
                        enableApply();
                    }
                } else {
                    //The user has modified tags for the sort, enable the Apply button:
                    enableApply();
                }
                gtsiSelectedTagIDs = new TreeSet<>(tsiNewTagIDs);

                //Update the textual display of selected tags:
                if(getView() != null){

                    StringBuilder sb = new StringBuilder();
                    if(tagItems.size() > 0) {
                        sb.append(tagItems.get(0).sTagText);
                        for (int i = 1; i < tagItems.size(); i++) {
                            sb.append(", ");
                            sb.append(tagItems.get(i).sTagText);
                        }
                    }
                    //Display the tags:
                    TextView textView_Tags = getView().findViewById(R.id.textView_Tags);
                    if (textView_Tags != null) {
                        textView_Tags.setText(sb.toString());
                    }



                }


            }
        };
    }



    public void initData(){

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        if(getActivity() == null){
            return;
        }
        gViewModel_fragment_selectTags = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_SelectTags.class);

        gViewModel_fragment_selectTags.bFilterOnXrefTags = true; //When the user selects a tag to filter on, update counts of other tag items to reflect those which share the selected tag(s).

        gViewModel_fragment_selectTags.altiTagsSelected.removeObservers(getViewLifecycleOwner());

        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<ItemClass_Tag>> observerSelectedTags = getNewTagObserver();

        gViewModel_fragment_selectTags.altiTagsSelected.observe(getViewLifecycleOwner(), observerSelectedTags);

        if(gViewModel_fragment_selectTags.altiTagsSelected.getValue() != null){
            //If there appears that there might be tags, as would be the case for a restart of the activity
            //  due to an orientation change, re-init the data in this fragment.
            ArrayList<Integer> aliTags = new ArrayList<>();
            for(ItemClass_Tag ict: gViewModel_fragment_selectTags.altiTagsSelected.getValue()){
                gtsiSelectedTagIDs.add(ict.iTagID);
                aliTags.add(ict.iTagID);
            }
            //Re-populate the tags fragment:
            if(gFragment_selectTags != null) {
                gFragment_selectTags.resetTagListViewData(aliTags);
            }
        }
        if (getView() != null) {
            Button button_Apply = getView().findViewById(R.id.button_Apply);
            button_Apply.setEnabled(false); //When init data, no reason to save.
        }


    }




    private void enableApply(){
        if(getView() == null){
            return;
        }

        //todo: Evaluate all criteria selected to ensure it has changed before enabling the Apply button.

        Button button_Apply = getView().findViewById(R.id.button_Apply);
        if(button_Apply != null){
            button_Apply.setEnabled(true);
        }
    }


}