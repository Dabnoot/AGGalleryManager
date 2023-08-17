package com.agcurations.aggallerymanager;

import androidx.lifecycle.ViewModel;

public class ViewModel_CatalogAnalysis extends ViewModel {

    int iMediaCategory = -1;
    int iAnalysisType = -1;

    public static final int ANALYSIS_TYPE_MISSING_FILES = 1;
    public static final int ANALYSIS_TYPE_ORPHANED_FILES = 2;


}