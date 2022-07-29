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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private Fragment_SelectTags gFragment_selectTags;


    final int SPINNER_SORTBY_ITEM_IMPORT_DATE = 0;
    final int SPINNER_SORTBY_ITEM_LAST_VIEWED_DATE = 1;
    final String[] gsSpinnerSortByItems ={"Import Date","Last Read Date"};

    boolean gbCatalogViewerSortAscending;

    int giSearchInSelections = GlobalClass.SEARCH_IN_NO_SELECTION;
    final String[] gsSpinnerSearchInItems = {
            "",
            "Title",
            "Artist",
            "Characters",
            "Parodies",
            "Item ID"};

    int giFilterBySelections = GlobalClass.FILTER_BY_NO_SELECTION;
    final String[] gsSpinnerFilterByItems = {
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
        gFragment_selectTags.setArguments(fragment_selectTags_args);
        fragmentTransaction.replace(R.id.child_fragment_tag_selector, gFragment_selectTags);
        fragmentTransaction.commit();


        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<ItemClass_Tag>> observerSelectedTags = getNewTagObserver();
        gViewModel_fragment_selectTags.altiTagsSelected.observe(getViewLifecycleOwner(), observerSelectedTags);




        if (getView() != null) {

            //Configure the 'Sort by' selection spinner:
            Spinner spinner_SortBy = getView().findViewById(R.id.spinner_SortBy);
            //wrap the items in the Adapter
            ArrayAdapter<String> adapterSortBy = new ArrayAdapter<>(getActivity(), R.layout.catalog_spinner_item_sort_search_filter, gsSpinnerSortByItems);
            //assign adapter to the Spinner
            spinner_SortBy.setAdapter(adapterSortBy);

            //Initialize the spinner position:
            //This is here because when onResume hits when the activity is first created,
            //  the Spinner does not yet exist.
            if(globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_IMPORTED){
                spinner_SortBy.setSelection(SPINNER_SORTBY_ITEM_IMPORT_DATE);
            } else if(globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_LAST_VIEWED){
                spinner_SortBy.setSelection(SPINNER_SORTBY_ITEM_LAST_VIEWED_DATE);
            }

            spinner_SortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                boolean bInitialized = false;
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if(bInitialized) {
                        enableApply();
                    }
                    bInitialized = true;
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // no need to code here
                }
            });

            //Initialize the SortOrder ImageButton:
            final ImageButton imageButton_SortOrder = getView().findViewById(R.id.imageButton_SortOrder);
            if(globalClass.gbCatalogViewerSortAscending[globalClass.giSelectedCatalogMediaCategory]){
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




            //Listen to when the Keywords EditText field is modified to enable the Apply button:
            EditText editText_SearchKeywords = requireView().findViewById(R.id.editText_SearchKeywords);
            if(!globalClass.gsCatalogViewerSearchInText[globalClass.giSelectedCatalogMediaCategory].equals("")){
                editText_SearchKeywords.setText(globalClass.gsCatalogViewerSearchInText[globalClass.giSelectedCatalogMediaCategory]);
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

            //Configure the "SearchIn" spinner:
            Spinner spinner_SearchIn = getView().findViewById(R.id.spinner_SearchIn);
            //wrap the items in the Adapter
            ArrayAdapter<String> adapterSearchIn = new ArrayAdapter<>(getActivity(), R.layout.catalog_spinner_item_sort_search_filter, gsSpinnerSearchInItems);
            //assign adapter to the Spinner
            spinner_SearchIn.setAdapter(adapterSearchIn);

            //Initialize the "SearchIn" spinner position:
            //This is here because when onResume hits when the activity is first created,
            //  the Spinner does not yet exist.
            int iSpinnerSelection = globalClass.giCatalogViewerSearchInSelection[globalClass.giSelectedCatalogMediaCategory];
            spinner_SearchIn.setSelection(iSpinnerSelection);

            spinner_SearchIn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                boolean bInitialized = false;
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if(bInitialized) {
                        enableApply();
                    }
                    bInitialized = true;
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // no need to code here
                }
            }); //End config of spinner_SearchIn.

            //Configure the "FilterBy" spinner:
            Spinner spinner_FilterBy = getView().findViewById(R.id.spinner_FilterBy);
            //wrap the items in the Adapter
            ArrayAdapter<String> adapterFilterBy = new ArrayAdapter<>(getActivity(), R.layout.catalog_spinner_item_sort_search_filter, gsSpinnerFilterByItems);
            //assign adapter to the Spinner
            spinner_FilterBy.setAdapter(adapterFilterBy);

            //Initialize the "FilterBy" spinner position:
            //This is here because when onResume hits when the activity is first created,
            //  the Spinner does not yet exist.
            iSpinnerSelection = globalClass.giCatalogViewerFilterBySelection[globalClass.giSelectedCatalogMediaCategory];
            spinner_FilterBy.setSelection(iSpinnerSelection);

            spinner_FilterBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                boolean bInitialized = false;
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if(bInitialized) {
                        enableApply();
                    }
                    bInitialized = true;
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // no need to code here
                }
            }); //End config of spinner_FilterBy.

            //Configure the resolution range slider:
            if (getView() != null) {
                RangeSlider rangeSlider_Resolution = getView().findViewById(R.id.rangeSlider_Resolution);
                if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                    //Configure the rangeSlider to have discrete steps:
                    float fMaxPositionCount = globalClass.gtmVideoResolutions.size() - 1;
                    rangeSlider_Resolution.setValueTo(fMaxPositionCount);
                    rangeSlider_Resolution.setValues(0.0F, fMaxPositionCount);
                    rangeSlider_Resolution.setStepSize(1.0f);
                    rangeSlider_Resolution.setLabelFormatter(new LabelFormatter() {
                        @NonNull
                        @Override
                        public String getFormattedValue(float value) {
                            int iValue = (int) value;
                            String sValue = globalClass.gtmVideoResolutions.get(iValue) + "p";
                            return sValue;
                        }
                    });
                    rangeSlider_Resolution.addOnChangeListener(new RangeSlider.OnChangeListener() {
                        @Override
                        public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                            enableApply();
                        }
                    });
                } else if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                    rangeSlider_Resolution.setValueTo(globalClass.giMaxImageMegaPixels);
                    rangeSlider_Resolution.setValues((float) globalClass.giMinImageMegaPixels,
                            (float) globalClass.giMaxImageMegaPixels);
                    rangeSlider_Resolution.setLabelFormatter(new LabelFormatter() {
                        @NonNull
                        @Override
                        public String getFormattedValue(float value) {
                            String sValue = String.format(Locale.getDefault(), "%01.1fMP", value);
                            return sValue;
                        }
                    });
                    rangeSlider_Resolution.addOnChangeListener(new RangeSlider.OnChangeListener() {
                        @Override
                        public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                            enableApply();
                        }
                    });
                } else if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                    TextView textView_labelResolution = getView().findViewById(R.id.textView_labelResolution);
                    if (textView_labelResolution != null) {
                        textView_labelResolution.setText("Comic page count:");
                    }
                    rangeSlider_Resolution.setValueTo(globalClass.giMaxComicPageCount);
                    rangeSlider_Resolution.setValues((float) globalClass.giMinComicPageCount,
                            (float) globalClass.giMaxComicPageCount);
                    rangeSlider_Resolution.setLabelFormatter(new LabelFormatter() {
                        @NonNull
                        @Override
                        public String getFormattedValue(float value) {
                            int iValue = (int) value;
                            String sValue = iValue + "";
                            return sValue;
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
                if(globalClass.giSelectedCatalogMediaCategory != GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                    //Hide the video duration RangeSlider if we are not viewing videos:
                    RelativeLayout relativeLayout_VideoDuration = getView().findViewById(R.id.relativeLayout_VideoDuration);
                    if (relativeLayout_VideoDuration != null) {
                        relativeLayout_VideoDuration.getLayoutParams().height = 0;
                        relativeLayout_VideoDuration.requestLayout();
                    }
                }

                RangeSlider rangeSlider_VideoDuration = getView().findViewById(R.id.rangeSlider_VideoDuration);
                float fMaxMilliseconds = globalClass.glMaxVideoDurationMS;
                rangeSlider_VideoDuration.setValueTo(fMaxMilliseconds);
                rangeSlider_VideoDuration.setValues(0.0F, fMaxMilliseconds);
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

            //Configure the APPLY button listener:
            final Button button_Apply = getView().findViewById(R.id.button_Apply);
            if (button_Apply != null) {
                button_Apply.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //On Apply filter....
                        apply();
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
        Spinner spinner_SortBy = getView().findViewById(R.id.spinner_SortBy);
        int iSpinnerSortByPosition = spinner_SortBy.getSelectedItemPosition();
        if(iSpinnerSortByPosition == SPINNER_SORTBY_ITEM_IMPORT_DATE) {
            //globalClass.giCatalogViewerSortBySetting = GlobalClass.giDataRecordDateTimeImportIndexes[globalClass.giSelectedCatalogMediaCategory];
            globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
        } else if(iSpinnerSortByPosition == SPINNER_SORTBY_ITEM_LAST_VIEWED_DATE) {
            //globalClass.giCatalogViewerSortBySetting = GlobalClass.giDataRecordDateTimeViewedIndexes[globalClass.giSelectedCatalogMediaCategory];
            globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] = GlobalClass.SORT_BY_DATETIME_LAST_VIEWED;
        }
        //Record the user's selected sort item:
        if(getActivity() != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            sharedPreferences.edit()
                    .putInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[globalClass.giSelectedCatalogMediaCategory],
                            globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory])
                    .apply();
        }

        //Read SortOrder:
        globalClass.gbCatalogViewerSortAscending[globalClass.giSelectedCatalogMediaCategory] = gbCatalogViewerSortAscending;


        //Apply any text search to the search hold in globalClass:
        Spinner spinner_SearchIn = getView().findViewById(R.id.spinner_SearchIn);
        int iSpinnerSearchInPosition = spinner_SearchIn.getSelectedItemPosition();
        globalClass.giCatalogViewerSearchInSelection[globalClass.giSelectedCatalogMediaCategory] = iSpinnerSearchInPosition;
        if (iSpinnerSearchInPosition != GlobalClass.SEARCH_IN_NO_SELECTION) {
            EditText editText_SearchKeywords = requireView().findViewById(R.id.editText_SearchKeywords);
            if (editText_SearchKeywords != null) {
                String sSearchKeywords = editText_SearchKeywords.getText().toString();
                globalClass.gsCatalogViewerSearchInText[globalClass.giSelectedCatalogMediaCategory] = sSearchKeywords;
            }
        } else {
            globalClass.gsCatalogViewerSearchInText[globalClass.giSelectedCatalogMediaCategory] = "";
        }

        //Read FilterBy Selection:
        Spinner spinner_FilterBy = getView().findViewById(R.id.spinner_FilterBy);
        int iSpinnerFilterByPosition = spinner_FilterBy.getSelectedItemPosition();
        globalClass.giCatalogViewerFilterBySelection[globalClass.giSelectedCatalogMediaCategory] = iSpinnerFilterByPosition;


        //Read Resolution/PageCount RangeSlider:
        RangeSlider rangeSlider_Resolution = getView().findViewById(R.id.rangeSlider_Resolution);
        List<Float> lfRangeSliderSelectedMinMaxValues = rangeSlider_Resolution.getValues();
        if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            if(lfRangeSliderSelectedMinMaxValues.get(0) == 0) {
                globalClass.giMinVideoResolutionSelected = -1;  //Mark as no value selected.
            } else {
                globalClass.giMinVideoResolutionSelected = lfRangeSliderSelectedMinMaxValues.get(0).intValue();
            }
            float fMaxPositionCount = globalClass.gtmVideoResolutions.size() - 1;
            if(lfRangeSliderSelectedMinMaxValues.get(1) == fMaxPositionCount) {
                globalClass.giMaxVideoResolutionSelected = -1;  //Mark as no value selected.
            } else {
                globalClass.giMaxVideoResolutionSelected = lfRangeSliderSelectedMinMaxValues.get(1).intValue();
            }
        } else if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
            if(lfRangeSliderSelectedMinMaxValues.get(0) == (float) globalClass.giMinImageMegaPixels) {
                globalClass.giMinImageMegaPixelsSelected = -1;  //Mark as no value selected.
            } else {
                globalClass.giMinImageMegaPixelsSelected = lfRangeSliderSelectedMinMaxValues.get(0).intValue();
            }
            if(lfRangeSliderSelectedMinMaxValues.get(1) == (float) globalClass.giMaxImageMegaPixels) {
                globalClass.giMaxImageMegaPixelsSelected = -1;  //Mark as no value selected.
            } else {
                globalClass.giMaxImageMegaPixelsSelected = lfRangeSliderSelectedMinMaxValues.get(1).intValue();
            }
        } else if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
            if(lfRangeSliderSelectedMinMaxValues.get(0) == (float) globalClass.giMinComicPageCount) {
                globalClass.giMinComicPageCountSelected = -1;  //Mark as no value selected.
            } else {
                globalClass.giMinComicPageCountSelected = lfRangeSliderSelectedMinMaxValues.get(0).intValue();
            }
            if(lfRangeSliderSelectedMinMaxValues.get(1) == (float) globalClass.giMaxComicPageCount) {
                globalClass.giMaxComicPageCountSelected = -1;  //Mark as no value selected.
            } else {
                globalClass.giMaxComicPageCountSelected = lfRangeSliderSelectedMinMaxValues.get(1).intValue();
            }
        }

        //Read Duration RangeSlider:
        RangeSlider rangeSlider_VideoDuration = getView().findViewById(R.id.rangeSlider_VideoDuration);
        lfRangeSliderSelectedMinMaxValues = rangeSlider_VideoDuration.getValues();
        if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            if (lfRangeSliderSelectedMinMaxValues.get(0) == 0.0f) {
                globalClass.glMinVideoDurationMSSelected = -1;
            } else {
                globalClass.glMinVideoDurationMSSelected = lfRangeSliderSelectedMinMaxValues.get(0).longValue();
            }
            if (lfRangeSliderSelectedMinMaxValues.get(1) == (float) globalClass.glMaxVideoDurationMS) {
                globalClass.glMaxVideoDurationMSSelected = -1;
            } else {
                globalClass.glMaxVideoDurationMSSelected = lfRangeSliderSelectedMinMaxValues.get(1).longValue();
            }
        }

        //Apply any tag filters to the filter hold in globalClass:
        if (globalClass.galtsiCatalogViewerFilterTags == null) {
            globalClass.galtsiCatalogViewerFilterTags = new ArrayList<>();
            globalClass.galtsiCatalogViewerFilterTags.add(new TreeSet<Integer>()); //Videos
            globalClass.galtsiCatalogViewerFilterTags.add(new TreeSet<Integer>()); //Images
            globalClass.galtsiCatalogViewerFilterTags.add(new TreeSet<Integer>()); //Comics
        }
        if (gtsiSelectedTagIDs != null) {
            for (Integer iTagID : gtsiSelectedTagIDs) {
                globalClass.galtsiCatalogViewerFilterTags.get(giMediaCategory).add(iTagID);
            }
        } else {
            globalClass.galtsiCatalogViewerFilterTags.get(giMediaCategory).clear();
        }

        //Process the filter settings:
        ((Activity_CatalogViewer) requireActivity()).populate_RecyclerViewCatalogItems();

        Button button_Apply = getView().findViewById(R.id.button_Apply);
        button_Apply.setEnabled(false);

        if (getActivity() != null) {
            ((Activity_CatalogViewer) getActivity()).closeDrawer();
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

            }
        };
    }



    public void initData(){

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        if(getActivity() == null){
            return;
        }
        gViewModel_fragment_selectTags = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_SelectTags.class);

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
        Button button_Apply = getView().findViewById(R.id.button_Apply);
        if(button_Apply != null){
            button_Apply.setEnabled(true);
        }
    }


}