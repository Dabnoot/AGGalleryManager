package com.agcurations.aggallerymanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Fragment_CatalogAnalysis_3_TrimOrImport extends Fragment {

    public static Fragment_CatalogAnalysis_3_TrimOrImport newInstance() {
        return new Fragment_CatalogAnalysis_3_TrimOrImport();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog_analysis_3_trim_or_import, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getView() == null){
            return;
        }



        RadioButton radioButton_ReviewOrphanedFiles = getView().findViewById(R.id.radioButton_ReviewOrphanedFiles);

        int iOrphanedItemCount = Activity_CatalogAnalysis.giOrphansWOMatch +
                Activity_CatalogAnalysis.giOrphansWMatchWOMedia +
                Activity_CatalogAnalysis.giOrphansWMatchWMedia;
        String sOrphanLabel = "Review orphaned files (" + iOrphanedItemCount + " items)";
        radioButton_ReviewOrphanedFiles.setText(sOrphanLabel);

        RadioButton radioButton_ReviewCatalogItemsMissingMedia = getView().findViewById(R.id.radioButton_ReviewCatalogItemsMissingMedia);
        if(Activity_CatalogAnalysis.gals_CatalogItemsMissingMedia != null) {
            int iTrimCount = Activity_CatalogAnalysis.gals_CatalogItemsMissingMedia.size();
            String sTrimLabel = "Review catalog items that are missing assigned media (" + iTrimCount + " items)";
            radioButton_ReviewCatalogItemsMissingMedia.setText(sTrimLabel);
        }


    }
}