package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_Import_0_MediaCategory#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_Import_0_MediaCategory extends Fragment {

    public Fragment_Import_0_MediaCategory() {
        // Required empty public constructor
    }

    public static Fragment_Import_0_MediaCategory newInstance() {
        return new Fragment_Import_0_MediaCategory();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            //viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_0_media_category, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() != null) {
            getActivity().setTitle("Import");
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }
    }


}