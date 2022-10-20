package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class Fragment_CatalogDataEditor extends Fragment {



    public Fragment_CatalogDataEditor() {
        // Required empty public constructor
    }

    public static Fragment_CatalogDataEditor newInstance() {
        return new Fragment_CatalogDataEditor();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_catalog_data_editor, container, false);
    }
}