package com.agcurations.aggallerymanager;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;


public class Fragment_CatalogSort extends Fragment {

    GlobalClass globalClass;

    int giMediaCategory;

    TreeSet<Integer> gtsiSelectedTagIDs;

    ViewModel_Fragment_SelectTags gViewModel_fragment_selectTags;

    private Fragment_SelectTags gFragment_selectTags;

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
        final Observer<ArrayList<ItemClass_Tag>> observerSelectedTags = GetNewTagObserver();
        gViewModel_fragment_selectTags.altiTagsSelected.observe(getViewLifecycleOwner(), observerSelectedTags);




        if (getView() != null) {
            //Configure the APPLY button listener:
            final Button button_Apply = getView().findViewById(R.id.button_Apply);
            if (button_Apply != null) {
                button_Apply.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //On Apply filter....
                        //Apply any text filters to the filter hold in globalClass:
                        EditText editText_Keywords = requireView().findViewById(R.id.editText_Keywords);
                        if (editText_Keywords != null) {
                            String sKeywords = editText_Keywords.getText().toString();
                            globalClass.gsCatalogViewerFilterText[globalClass.giSelectedCatalogMediaCategory] = sKeywords;
                        }

                        //Apply any tag filters to the filter hold in globalClass:
                        if(globalClass.galtsiCatalogViewerFilterTags == null){
                            globalClass.galtsiCatalogViewerFilterTags = new ArrayList<>();
                            globalClass.galtsiCatalogViewerFilterTags.add(new TreeSet<Integer>()); //Videos
                            globalClass.galtsiCatalogViewerFilterTags.add(new TreeSet<Integer>()); //Images
                            globalClass.galtsiCatalogViewerFilterTags.add(new TreeSet<Integer>()); //Comics
                        }
                        if(gtsiSelectedTagIDs != null){
                            for(Integer iTagID: gtsiSelectedTagIDs){
                                globalClass.galtsiCatalogViewerFilterTags.get(giMediaCategory).add(iTagID);
                            }
                        } else {
                            globalClass.galtsiCatalogViewerFilterTags.get(giMediaCategory).clear();
                        }

                        //Process the filter settings:
                        ((Activity_CatalogViewer) requireActivity()).populate_RecyclerViewCatalogItems();
                        globalClass.gbCatalogViewerFiltered[globalClass.giSelectedCatalogMediaCategory] = true;

                        button_Apply.setEnabled(false);

                        if (getActivity() != null) {
                            ((Activity_CatalogViewer) getActivity()).closeDrawer();
                        }

                    }
                });

                //Listen to when the Keywords EditText field is modified to enable the Apply button:
                EditText editText_Keywords = requireView().findViewById(R.id.editText_Keywords);
                if (editText_Keywords != null) {
                    editText_Keywords.addTextChangedListener(new TextWatcher() {
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
                    editText_Keywords.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if(!hasFocus){
                                //Hide the soft keyboard if the editText no longer has focus:
                                InputMethodManager imm =  (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            }
                        }
                    });
                }
            }
        }

        initData();

    }

    private Observer<ArrayList<ItemClass_Tag>> GetNewTagObserver() {
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
        final Observer<ArrayList<ItemClass_Tag>> observerSelectedTags = GetNewTagObserver();

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