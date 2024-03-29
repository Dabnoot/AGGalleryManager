package com.agcurations.aggallerymanager;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class ViewModel_CatalogAnalysis extends ViewModel {

    static final String EXTRA_ANALYSIS_TYPE = "com.agcurations.aggallerymanager.extra.EXTRA_ANALYSIS_TYPE";

    int iMediaCategory = -1;
    int iAnalysisType = -1;

    public static final int ANALYSIS_TYPE_MISSING_FILES = 1;
    public static final int ANALYSIS_TYPE_ORPHANED_FILES = 2;

}