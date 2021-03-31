package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class Fragment_Import_2a_SelectDetectedWebVideo extends Fragment {

    public static ViewModel_ImportActivity viewModelImportActivity;

    public Fragment_Import_2a_SelectDetectedWebVideo() {
        // Required empty public constructor
    }

    public static Fragment_Import_2a_SelectDetectedWebVideo newInstance() {
        return new Fragment_Import_2a_SelectDetectedWebVideo();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Instantiate the ViewModel sharing data between fragments:
        if(getActivity() != null) {
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_2a_select_detected_web_video, container, false);
    }
}