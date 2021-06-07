package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class Fragment_Import_3a_ItemDownloadTagImport extends Fragment {



    public Fragment_Import_3a_ItemDownloadTagImport() {
        // Required empty public constructor
    }

    public static Fragment_Import_3a_ItemDownloadTagImport newInstance() {
        Fragment_Import_3a_ItemDownloadTagImport fragment = new Fragment_Import_3a_ItemDownloadTagImport();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_3a_item_download_tag_import, container, false);
    }
}