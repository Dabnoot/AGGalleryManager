package com.agcurations.aggallerymanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;

import java.util.List;

public class Fragment_CatalogAnalysis_1_AnalysisType extends Fragment {

    RangeSlider gRangeSlider_MaxResultCount;
    TextView gTextView_MaxResultsCount;

    public static Fragment_CatalogAnalysis_1_AnalysisType newInstance() {
        return new Fragment_CatalogAnalysis_1_AnalysisType();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog_analysis_1_analysis_type, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getView() == null){
            return;
        }

        //Configure the "max result count" rangeslider:
        gRangeSlider_MaxResultCount = getView().findViewById(R.id.rangeSlider_MaxResultCount);
        //Set default value:
        gRangeSlider_MaxResultCount.setValueTo((float) 5);
        gRangeSlider_MaxResultCount.setStepSize((float) 1);
        gRangeSlider_MaxResultCount.setValues(3.0f);
        gRangeSlider_MaxResultCount.setLabelFormatter(new LabelFormatter() {
            @NonNull
            @Override
            public String getFormattedValue(float v) {
                return getMaxResultCountSelectionString(v);
            }
        });
        gTextView_MaxResultsCount = getView().findViewById(R.id.textView_MaxResultsCount);

        gRangeSlider_MaxResultCount.addOnChangeListener(new RangeSlider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull RangeSlider rangeSlider, float v, boolean b) {
                if(gTextView_MaxResultsCount != null){
                    String sLabel = getMaxResultCountSelectionString(v);
                    gTextView_MaxResultsCount.setText(sLabel);
                }

                GlobalClass.giCatalog_Analysis_Approx_Max_Results = getMaxResultCountSelectionInt(v);

            }
        });



    }

    private String getMaxResultCountSelectionString(float v){
        String sLabel = "";

        switch ((int)v){
            case 0:
                sLabel = "10";
                break;
            case 1:
                sLabel = "20";
                break;
            case 2:
                sLabel = "50";
                break;
            case 3:
                sLabel = "100";
                break;
            case 4:
                sLabel = "200";
                break;
            case 5:
                sLabel = "All Results";
                break;
        }

        return sLabel;
    }

    private int getMaxResultCountSelectionInt(float v){
        int iValue = 100;

        switch ((int)v){
            case 0:
                iValue = 10;
                break;
            case 1:
                iValue = 20;
                break;
            case 2:
                iValue = 50;
                break;
            case 4:
                iValue = 200;
                break;
            case 5:
                iValue = -1;
                break;
        }

        return iValue;
    }



}