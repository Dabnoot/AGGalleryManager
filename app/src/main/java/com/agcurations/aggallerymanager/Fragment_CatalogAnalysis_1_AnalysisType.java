package com.agcurations.aggallerymanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.RangeSlider;

import java.util.List;

public class Fragment_CatalogAnalysis_1_AnalysisType extends Fragment {

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

        //Configure the maturity filter rangeslider:
        RangeSlider rangeSlider_MaturityFilter = getView().findViewById(R.id.rangeSlider_MaturityFilter);
        //Set max available maturity to the max allowed to the user:
        if(GlobalClass.gicuCurrentUser != null) {
            rangeSlider_MaturityFilter.setValueTo((float) GlobalClass.gicuCurrentUser.iMaturityLevel);
        } else {
            rangeSlider_MaturityFilter.setValueTo((float) GlobalClass.giDefaultUserMaturityRating);
        }
        rangeSlider_MaturityFilter.setStepSize((float) 1);
        //Set the current selected maturity window max to the default maturity rating:
        rangeSlider_MaturityFilter.setValues((float) GlobalClass.giMinContentMaturityFilter, (float) GlobalClass.giMaxContentMaturityFilter);

        rangeSlider_MaturityFilter.setLabelFormatter(value -> AdapterMaturityRatings.MATURITY_RATINGS[(int)value][0] + " - " + AdapterMaturityRatings.MATURITY_RATINGS[(int)value][1]);
        rangeSlider_MaturityFilter.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> lfSliderValues = slider.getValues();
            if(lfSliderValues.size() == 2){
                int iMinTemp = lfSliderValues.get(0).intValue();
                int iMaxTemp = lfSliderValues.get(1).intValue();
                if(iMinTemp != GlobalClass.giMinContentMaturityFilter ||
                        iMaxTemp != GlobalClass.giMaxContentMaturityFilter) {
                    GlobalClass.giMinContentMaturityFilter = lfSliderValues.get(0).intValue();
                    GlobalClass.giMaxContentMaturityFilter = lfSliderValues.get(1).intValue();
                }
            }
        });
    }
}