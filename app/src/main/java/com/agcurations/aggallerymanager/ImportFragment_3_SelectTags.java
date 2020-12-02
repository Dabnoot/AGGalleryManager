package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImportFragment_3_SelectTags#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImportFragment_3_SelectTags extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ImportFragment_3_SelectTags() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentImport_3_VideoApplyTags.
     */
    // TODO: Rename and change types and number of parameters
    public static ImportFragment_3_SelectTags newInstance(String param1, String param2) {
        ImportFragment_3_SelectTags fragment = new ImportFragment_3_SelectTags();
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
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.import_fragment_3_select_tags, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        initComponents();
    }

    public void initComponents() {

        if (getView() == null) {
            return;
        }

        if(getActivity() == null){
            return;
        }

        //Populate the listview:
        final ListView listView_FilesToImport = getView().findViewById(R.id.listView_FilesToImport);
        if(listView_FilesToImport != null) {
            listView_FilesToImport.setAdapter(ImportActivity.fileListCustomAdapter);
        }



        //Start the tag selection fragment:
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        Fragment_SelectTags fst = new Fragment_SelectTags();
        Bundle args = new Bundle();
        args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, ImportActivity.giImportMediaCategory);
        fst.setArguments(args);
        ft.replace(R.id.child_fragment_tag_selector, fst);
        ft.commit();

        //React to changes in the selected tag data in the ViewModel initiated in ImportActivity:
        final Observer<ArrayList<TagItem>> selectedTagsObserver = new Observer<ArrayList<TagItem>>() {
            @Override
            public void onChanged(ArrayList<TagItem> tagItems) {

                //Get the text of the tags and display:
                StringBuilder sb = new StringBuilder();
                if(tagItems.size() > 0) {
                    sb.append(tagItems.get(0).TagText);
                    for (int i = 1; i < tagItems.size(); i++) {
                        sb.append(", ");
                        sb.append(tagItems.get(i).TagText);
                    }
                }
                //Display the tags:
                TextView tv = getView().findViewById(R.id.textView_ImportTags);
                if(tv != null){
                    tv.setText(sb.toString());
                }

                //Apply the selected tags to individual items:


            }
        };
        ImportActivity.viewModelTags.altiTagsSelected.observe(this, selectedTagsObserver);

    }

}