package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.WorkManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.Stack;

public class Activity_CatalogAnalysis extends AppCompatActivity {

    public static final String EXTRA_BOOL_IMPORT_ORPHANED_FILES = "com.agcurations.aggallerymanager.intent.extra.BOOL_IMPORT_ORPHANED_FILES";

    //Fragment page indexes:
    public static final int FRAGMENT_CAT_ANALYSIS_0_ID_MEDIA_CATEGORY = 0;
    public static final int FRAGMENT_CAT_ANALYSIS_1_ANALYSIS_TYPE = 1;
    public static final int FRAGMENT_CAT_ANALYSIS_2_PERFORM_ANALYSIS = 2;
    public static final int FRAGMENT_COUNT = 3;

    public ViewPager2 ViewPager2_CatalogAnalysis;
    FragmentCatalogAnalysisViewPagerAdapter catalogAnalysisViewPagerAdapter;

    public static ViewModel_CatalogAnalysis viewModel_catalogAnalysis; //Used to transfer data between fragments.

    static Stack<Integer> stackFragmentOrder;
    private static int giStartingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog_analysis);

        setTitle("Catalog Analysis");

        ViewPager2_CatalogAnalysis = findViewById(R.id.viewPager_CatalogAnalysis);

        catalogAnalysisViewPagerAdapter = new FragmentCatalogAnalysisViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        // set Orientation in your ViewPager2
        ViewPager2_CatalogAnalysis.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        ViewPager2_CatalogAnalysis.setAdapter(catalogAnalysisViewPagerAdapter);

        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        ViewPager2_CatalogAnalysis.setUserInputEnabled(false);

        //Instantiate the ViewModel sharing data between fragments:
        viewModel_catalogAnalysis = new ViewModelProvider(this).get(ViewModel_CatalogAnalysis.class);

        stackFragmentOrder = new Stack<>();
        giStartingFragment = FRAGMENT_CAT_ANALYSIS_0_ID_MEDIA_CATEGORY;

    }

    //======================================================
    //======  FRAGMENT NAVIGATION ROUTINES  ================
    //======================================================



    @Override
    public void onBackPressed() {

        if(stackFragmentOrder.empty()){
            gotoFinish();
        } else {
            //Go back through the fragments in the order by which we progressed.
            //  Some user selections will cause a fragment to be skipped, and we don't want
            //  to go back to those skipped fragments, hence the use of a Stack, and pop().
            int iCurrentFragment = ViewPager2_CatalogAnalysis.getCurrentItem();
            int iPrevFragment = stackFragmentOrder.pop();
            if((iCurrentFragment == iPrevFragment) && (iCurrentFragment == giStartingFragment)){
                gotoFinish();
                return;
            }

            if(iCurrentFragment == iPrevFragment){
                //To handle interesting behavior about how the stack is built.
                iPrevFragment = stackFragmentOrder.peek();
            }
            ViewPager2_CatalogAnalysis.setCurrentItem(iPrevFragment, false);

            if(iPrevFragment == giStartingFragment){
                //There is no item to push '0' onto the fragment order stack. Do it here:
                stackFragmentOrder.push(giStartingFragment);
            }
        }
    }

    public void buttonClick_Cancel(View v){
        gotoFinish();
    }

    public void buttonNextClick_AnalysisFinish(View v) {
        gotoFinish();
    }

    public void gotoFinish(){
        //Code any pre-finish operations here.
        //Kill any file indexing workers that might be running:
        WorkManager.getInstance(getApplicationContext())
                .cancelAllWorkByTag(Worker_Catalog_Analysis.TAG_WORKER_CATALOG_VERIFICATION);
        GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.STOPPED);

        finish();
    }

    public void buttonNextClick_MediaCategorySelected(View v){
        RadioButton radioButton_ImportVideos = findViewById(R.id.radioButton_AnalyzeVideos);
        RadioButton radioButton_ImportImages = findViewById(R.id.radioButton_AnalyzeImages);

        int iNewImportMediaCatagory;
        if (radioButton_ImportVideos.isChecked()){
            iNewImportMediaCatagory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        } else if (radioButton_ImportImages.isChecked()){
            iNewImportMediaCatagory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        } else {
            iNewImportMediaCatagory = GlobalClass.MEDIA_CATEGORY_COMICS;
        }

        viewModel_catalogAnalysis.iMediaCategory = iNewImportMediaCatagory;

        ViewPager2_CatalogAnalysis.setCurrentItem(FRAGMENT_CAT_ANALYSIS_1_ANALYSIS_TYPE, false);

    }

    public void buttonNextClick_AnalysisTypeSelected(View v){
        RadioButton radioButton_MissingFileIdentification = findViewById(R.id.radioButton_MissingFileIdentification);

        if (radioButton_MissingFileIdentification.isChecked()){
            viewModel_catalogAnalysis.iAnalysisType = ViewModel_CatalogAnalysis.ANALYSIS_TYPE_MISSING_FILES;
        } else {
            viewModel_catalogAnalysis.iAnalysisType = ViewModel_CatalogAnalysis.ANALYSIS_TYPE_ORPHANED_FILES;
        }
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.START_REQUESTED);
        ViewPager2_CatalogAnalysis.setCurrentItem(FRAGMENT_CAT_ANALYSIS_2_PERFORM_ANALYSIS, false);

    }

    public void buttonNextClick_AnalysisImportSelect(View v){

        Intent intentImportGuided = new Intent(getApplicationContext(), Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, viewModel_catalogAnalysis.iMediaCategory); //todo: Redundant?
        intentImportGuided.putExtra(EXTRA_BOOL_IMPORT_ORPHANED_FILES, true);
        GlobalClass.galf_Orphaned_Files = new ArrayList<>(viewModel_catalogAnalysis.alFileList);
        GlobalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;//todo: Redundant?
        startActivity(intentImportGuided);
        finish();
    }

    public static class FragmentCatalogAnalysisViewPagerAdapter extends FragmentStateAdapter {

        public FragmentCatalogAnalysisViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case FRAGMENT_CAT_ANALYSIS_1_ANALYSIS_TYPE:
                    return new Fragment_CatalogAnalysis_1_AnalysisType();
                case FRAGMENT_CAT_ANALYSIS_2_PERFORM_ANALYSIS:
                    return new Fragment_CatalogAnalysis_2_PerformAnalysis();
                default:
                    return new Fragment_CatalogAnalysis_0_MediaCategory();
            }
        }

        @Override
        public int getItemCount() {
            return Activity_CatalogAnalysis.FRAGMENT_COUNT;
        }

    }

    //==========================================================
    //======  END FRAGMENT NAVIGATION ROUTINES  ================
    //==========================================================
    //==========================================================


}