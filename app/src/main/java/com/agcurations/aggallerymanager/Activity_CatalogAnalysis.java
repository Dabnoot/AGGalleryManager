package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.WorkManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;

import java.util.Stack;

public class Activity_CatalogAnalysis extends AppCompatActivity {

    //Fragment page indexes:
    public static final int FRAGMENT_CAT_ANALYSIS_0_ID_MEDIA_CATEGORY = 0;
    public static final int FRAGMENT_CAT_ANALYSIS_1_ANALYSIS_TYPE = 1;
    public static final int FRAGMENT_COUNT = 2;

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

    public void gotoFinish(){
        //Code any pre-finish operations here.
        //Kill any file indexing workers that might be running:
        WorkManager.getInstance(getApplicationContext())
                .cancelAllWorkByTag(Worker_Import_GetHoldingFolderDirectoryContents.TAG_WORKER_IMPORT_GETHOLDINGFOLDERDIRECTORYCONTENTS);
        WorkManager.getInstance(getApplicationContext())
                .cancelAllWorkByTag(Worker_Import_GetDirectoryContents.TAG_WORKER_IMPORT_GETDIRECTORYCONTENTS);

        finish();
    }

    public void buttonNextClick_MediaCategorySelected(View v){
        RadioButton radioButton_ImportVideos = findViewById(R.id.radioButton_ImportVideos);
        RadioButton radioButton_ImportImages = findViewById(R.id.radioButton_ImportImages);

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

        int iNewImportMediaCatagory;
        if (radioButton_MissingFileIdentification.isChecked()){
            viewModel_catalogAnalysis.iAnalysisType = ViewModel_CatalogAnalysis.ANALYSIS_TYPE_MISSING_FILES;
        } else {
            viewModel_catalogAnalysis.iAnalysisType = ViewModel_CatalogAnalysis.ANALYSIS_TYPE_ORPHANED_FILES;
        }

        //ViewPager2_CatalogAnalysis.setCurrentItem(FRAGMENT_CAT_ANALYSIS_1_ANALYSIS_TYPE, false);

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

                default:
                    return new Fragment_CatalogAnalysis_0_MediaCategory();
            }
        }

        @Override
        public int getItemCount() {
            return Activity_Import.FRAGMENT_COUNT;
        }

    }

    //==========================================================
    //======  END FRAGMENT NAVIGATION ROUTINES  ================
    //==========================================================
    //==========================================================


}