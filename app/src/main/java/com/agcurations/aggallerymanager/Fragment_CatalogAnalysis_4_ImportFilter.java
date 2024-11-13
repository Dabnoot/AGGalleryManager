package com.agcurations.aggallerymanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;

public class Fragment_CatalogAnalysis_4_ImportFilter extends Fragment {

    public static Fragment_CatalogAnalysis_4_ImportFilter newInstance() {
        return new Fragment_CatalogAnalysis_4_ImportFilter();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog_analysis_4_import_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getView() == null){
            return;
        }

        AppCompatCheckBox acCheckBox_ReviewOrphansWOMatch = getView().findViewById(R.id.acCheckBox_ReviewOrphansWOMatch);
        AppCompatCheckBox acCheckBox_ReviewOrphansWMatchWOMedia = getView().findViewById(R.id.acCheckBox_ReviewOrphansWMatchWOMedia);
        AppCompatCheckBox acCheckBox_ReviewOrphansWMatchWMedia = getView().findViewById(R.id.acCheckBox_ReviewOrphansWMatchWMedia);

        String sOrphansWOMatch_Label = "Without catalog matches (" + Activity_CatalogAnalysis.giOrphansWOMatch + " items)";
        String sOrphansWMatchWOMedia_Label = "With catalog matches missing their media (" + Activity_CatalogAnalysis.giOrphansWMatchWOMedia + " items)";
        String sOrphansWMatchWMedia_Label = "With catalog matches not missing media (" + Activity_CatalogAnalysis.giOrphansWMatchWMedia + " items)\n" +
                                            "These are orphaned files duplicating files already in catalog storage.";

        acCheckBox_ReviewOrphansWOMatch.setText(sOrphansWOMatch_Label);
        acCheckBox_ReviewOrphansWMatchWOMedia.setText(sOrphansWMatchWOMedia_Label);
        acCheckBox_ReviewOrphansWMatchWMedia.setText(sOrphansWMatchWMedia_Label);

    }
}