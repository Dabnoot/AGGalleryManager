package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class Fragment_Import_0b_ComicSource extends Fragment {


    public Fragment_Import_0b_ComicSource() {
        // Required empty public constructor
    }

    public static Fragment_Import_0b_ComicSource newInstance() {
        return new Fragment_Import_0b_ComicSource();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_0b_comic_source, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() != null) {
            getActivity().setTitle("Import");
        }
    }
}