package com.agcurations.aggallerymanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Fragment_CatalogAnalysis_3a_M3U8_Process_Select extends Fragment {

    public static Fragment_CatalogAnalysis_3a_M3U8_Process_Select newInstance() {
        return new Fragment_CatalogAnalysis_3a_M3U8_Process_Select();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog_analysis_3a_m3u8_process_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getView() == null){
            return;
        }


        RadioButton radioButton_UpdateM3U8sToSAF = getView().findViewById(R.id.radioButton_UpdateM3U8sToSAF);
        if(Activity_CatalogAnalysis.gals_M3U8_CatItems_Missing_SAF_Playlist != null) {
            int iItemCount = Activity_CatalogAnalysis.gals_M3U8_CatItems_Missing_SAF_Playlist.size();
            String sLabel = "Update M3U8 playlists to use full base storage paths (" + iItemCount + " items)";
            radioButton_UpdateM3U8sToSAF.setText(sLabel);
        }

        RadioButton radioButton_UpdateM3U8sBaseStorageString = getView().findViewById(R.id.radioButton_UpdateM3U8sBaseStorageString);
        if(Activity_CatalogAnalysis.gals_M3U8_CatItems_Misaligned_Paths != null) {
            int iItemCount = Activity_CatalogAnalysis.gals_M3U8_CatItems_Misaligned_Paths.size();
            String sLabel = "Update M3U8 playlists to use up-to-date base storage paths (" + iItemCount + " items)";
            radioButton_UpdateM3U8sBaseStorageString.setText(sLabel);
        }

        RadioButton radioButton_ReviewM3U8MissingSegments = getView().findViewById(R.id.radioButton_ReviewM3U8MissingSegments);
        if(Activity_CatalogAnalysis.gals_M3U8_CatItems_Missing_Segments != null) {
            int iItemCount = Activity_CatalogAnalysis.gals_M3U8_CatItems_Missing_Segments.size();
            String sLabel = "Review M3U8 playlists that are missing segment files (" + iItemCount + " M3U8 items)";
            radioButton_ReviewM3U8MissingSegments.setText(sLabel);
        }

        RadioButton radioButton_UpdateM3U8MissingSegments = getView().findViewById(R.id.radioButton_UpdateM3U8MissingSegments);
        if(Activity_CatalogAnalysis.gals_M3U8_CatItems_Missing_Segments != null) {
            int iItemCount = Activity_CatalogAnalysis.gals_M3U8_CatItems_Missing_Segments.size();
            String sLabel = "Update M3U8 playlists to exclude missing segment files (" + iItemCount + " M3U8 items)";
            radioButton_UpdateM3U8MissingSegments.setText(sLabel);
        }



    }
}