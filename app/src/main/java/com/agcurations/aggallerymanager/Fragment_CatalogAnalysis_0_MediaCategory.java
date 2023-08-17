package com.agcurations.aggallerymanager;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Fragment_CatalogAnalysis_0_MediaCategory extends Fragment {

    private ViewModel_CatalogAnalysis mViewModel;

    public static Fragment_CatalogAnalysis_0_MediaCategory newInstance() {
        return new Fragment_CatalogAnalysis_0_MediaCategory();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ViewModel_CatalogAnalysis.class);
        // TODO: Use the ViewModel
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog_analysis_0_media_category, container, false);
    }

}