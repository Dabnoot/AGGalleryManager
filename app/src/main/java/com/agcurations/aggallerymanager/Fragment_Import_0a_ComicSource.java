package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_Import_0a_ComicSource#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_Import_0a_ComicSource extends Fragment {


    public Fragment_Import_0a_ComicSource() {
        // Required empty public constructor
    }

    public static Fragment_Import_0a_ComicSource newInstance() {
        return new Fragment_Import_0a_ComicSource();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_0a_comic_source, container, false);
    }

}