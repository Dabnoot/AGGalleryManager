package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.WorkManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Stack;

public class Activity_CatalogAnalysis extends AppCompatActivity {

    public static final String EXTRA_BOOL_IMPORT_ORPHANED_FILES = "com.agcurations.aggallerymanager.intent.extra.BOOL_IMPORT_ORPHANED_FILES";

    //Fragment page indexes:
    public static final int FRAGMENT_CAT_ANALYSIS_0_ID_MEDIA_CATEGORY = 0;
    public static final int FRAGMENT_CAT_ANALYSIS_1_ANALYSIS_TYPE = 1;
    public static final int FRAGMENT_CAT_ANALYSIS_2_PERFORM_ANALYSIS = 2;
    public static final int FRAGMENT_CAT_ANALYSIS_3_TRIM_OR_IMPORT = 3;
    public static final int FRAGMENT_CAT_ANALYSIS_3A_M3U8_PROCESS_SELECTION = 4;
    public static final int FRAGMENT_CAT_ANALYSIS_4_IMPORT_FILTER = 5;
    public static final int FRAGMENT_COUNT = 6;

    public ViewPager2 ViewPager2_CatalogAnalysis;
    FragmentCatalogAnalysisViewPagerAdapter catalogAnalysisViewPagerAdapter;

    public static ViewModel_CatalogAnalysis viewModel_catalogAnalysis; //Used to transfer data between fragments.

    static Stack<Integer> stackFragmentOrder;
    private static int giStartingFragment;

    private CatalogAnalysisResponseReceiver catalogAnalysisResponseReceiver;

    public static ArrayList<ItemClass_File> galicf_CatalogAnalysisFileItems;
    public static ArrayList<String> gals_CatalogItemsMissingMedia;
    public static int giOrphansWOMatch;
    public static int giOrphansWMatchWMedia;
    public static int giOrphansWMatchWOMedia;
    public static ArrayList<String> gals_M3U8_CatItems_Missing_SAF_Playlist = new ArrayList<>();
    public static ArrayList<String> gals_M3U8_CatItems_Misaligned_Paths = new ArrayList<>();
    public static ArrayList<String> gals_M3U8_CatItems_Missing_Segments = new ArrayList<>();

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

        //Configure a response receiver to listen for data response from Worker_Catalog_Analysis:
        IntentFilter filter = new IntentFilter(Worker_Catalog_Analysis.CATALOG_ANALYSIS_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        catalogAnalysisResponseReceiver = new CatalogAnalysisResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(catalogAnalysisResponseReceiver,filter);

    }

    @Override
    protected void onDestroy() {

        LocalBroadcastManager.getInstance(this).unregisterReceiver(catalogAnalysisResponseReceiver);

        super.onDestroy();
    }

    //======================================================
    //======  FRAGMENT NAVIGATION ROUTINES  ================
    //======================================================



    @Override
    public void onBackPressed() {

        if (stackFragmentOrder.empty()) {
            gotoFinish();
        } else {
            //Go back through the fragments in the order by which we progressed.
            //  Some user selections will cause a fragment to be skipped, and we don't want
            //  to go back to those skipped fragments, hence the use of a Stack, and pop().
            int iCurrentFragment = ViewPager2_CatalogAnalysis.getCurrentItem();
            int iPrevFragment = stackFragmentOrder.pop();
            if ((iCurrentFragment == iPrevFragment) && (iCurrentFragment == giStartingFragment)) {
                gotoFinish();
                return;
            }

            if (iCurrentFragment == iPrevFragment) {
                //To handle interesting behavior about how the stack is built.
                iPrevFragment = stackFragmentOrder.peek();
            }
            ViewPager2_CatalogAnalysis.setCurrentItem(iPrevFragment, false);

            if (iPrevFragment == giStartingFragment) {
                //There is no item to push '0' onto the fragment order stack. Do it here:
                stackFragmentOrder.push(giStartingFragment);
            }
        }
    }

    public void buttonClick_Back(View v){
        onBackPressed();
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
        RadioButton radioButton_OrphanedFileIdentification = findViewById(R.id.radioButton_OrphanedFileIdentification);

        if (radioButton_MissingFileIdentification.isChecked()){
            viewModel_catalogAnalysis.iAnalysisType = Worker_Catalog_Analysis.ANALYSIS_TYPE_MISSING_FILES;
        } else if (radioButton_OrphanedFileIdentification.isChecked()) {
            viewModel_catalogAnalysis.iAnalysisType = Worker_Catalog_Analysis.ANALYSIS_TYPE_ORPHANED_FILES;
        } else {
            viewModel_catalogAnalysis.iAnalysisType = Worker_Catalog_Analysis.ANALYSIS_TYPE_M3U8;
        }
        GlobalClass.aiCatalogVerificationRunning.set(GlobalClass.START_REQUESTED);
        ViewPager2_CatalogAnalysis.setCurrentItem(FRAGMENT_CAT_ANALYSIS_2_PERFORM_ANALYSIS, false);

    }

    public void buttonNextClick_AnalysisComplete(View v){

        if(viewModel_catalogAnalysis.iAnalysisType != Worker_Catalog_Analysis.ANALYSIS_TYPE_M3U8) {
            //If we are here, then catalog items missing their media or orphaned files were found.
            if (gals_CatalogItemsMissingMedia == null) {
                //If there are no items to trim, go to the fragment that will lead the user to the import activity:
                ViewPager2_CatalogAnalysis.setCurrentItem(FRAGMENT_CAT_ANALYSIS_4_IMPORT_FILTER, false);
            } else if (galicf_CatalogAnalysisFileItems == null) {
                //If there are catalog items missing media, but no orphaned files...

                //todo: How to display for review catalog items missing media?

            } else {
                //If there are both catalog items missing media and orphaned files...
                ViewPager2_CatalogAnalysis.setCurrentItem(FRAGMENT_CAT_ANALYSIS_3_TRIM_OR_IMPORT, false);
            }
        } else {
            //M3U8 analysis
            ViewPager2_CatalogAnalysis.setCurrentItem(FRAGMENT_CAT_ANALYSIS_3A_M3U8_PROCESS_SELECTION, false);
        }

    }

    public void buttonNextClick_TrimOrImportSelected(View v){

        RadioButton radioButton_ReviewCatalogItemsMissingMedia = findViewById(R.id.radioButton_ReviewCatalogItemsMissingMedia);
        if(radioButton_ReviewCatalogItemsMissingMedia.isChecked()){
            //Start the fragment associated with reviewing catalog items with missing media:
            //todo: How to display for review catalog items missing media?

        } else {
            //Start the fragment associated with filtering orphaned files for potential import:
            ViewPager2_CatalogAnalysis.setCurrentItem(FRAGMENT_CAT_ANALYSIS_4_IMPORT_FILTER, false);
        }

    }

    public void buttonNextClick_M3U8ProcessSelect(View v){

        RadioButton radioButton_UpdateM3U8sToSAF = findViewById(R.id.radioButton_UpdateM3U8sToSAF);
        RadioButton radioButton_UpdateM3U8sBaseStorageString = findViewById(R.id.radioButton_UpdateM3U8sBaseStorageString);
        RadioButton radioButton_ReviewM3U8MissingSegments = findViewById(R.id.radioButton_ReviewM3U8MissingSegments);
        RadioButton radioButton_UpdateM3U8MissingSegments = findViewById(R.id.radioButton_UpdateM3U8MissingSegments);

        if(radioButton_UpdateM3U8sToSAF.isChecked()){

        } else if (radioButton_UpdateM3U8sBaseStorageString.isChecked()) {

        } else if (radioButton_ReviewM3U8MissingSegments.isChecked()) {

        } else if (radioButton_UpdateM3U8MissingSegments.isChecked()) {

        }

    }

    public void buttonNextClick_ImportFilterSelected(View v){

        //Filter the list of orphan file items based on the user's selection, and then start the
        //  import activity:

        AppCompatCheckBox acCheckBox_ReviewOrphansWOMatch = findViewById(R.id.acCheckBox_ReviewOrphansWOMatch);
        AppCompatCheckBox acCheckBox_ReviewOrphansWMatchWMedia = findViewById(R.id.acCheckBox_ReviewOrphansWMatchWMedia);
        AppCompatCheckBox acCheckBox_ReviewOrphansWMatchWOMedia = findViewById(R.id.acCheckBox_ReviewOrphansWMatchWOMedia);

        boolean bReviewOrphansWOMatch = acCheckBox_ReviewOrphansWOMatch.isChecked();
        boolean bReviewOrphansWMatchWMedia = acCheckBox_ReviewOrphansWMatchWMedia.isChecked();
        boolean bReviewOrphansWMatchWOMedia = acCheckBox_ReviewOrphansWMatchWOMedia.isChecked();

        ArrayList<ItemClass_File> alicf_FilteredForImportReview = new ArrayList<>();
        ArrayList<ItemClass_File> alicf_DownFiltered1 = new ArrayList<>();
        for(ItemClass_File icf: galicf_CatalogAnalysisFileItems){
            if(bReviewOrphansWOMatch && !icf.bOrphanAssociatedWithCatalogItem){
                alicf_FilteredForImportReview.add(icf);
            } else {
                alicf_DownFiltered1.add(icf);
            }
        }
        ArrayList<ItemClass_File> alicf_DownFiltered2 = new ArrayList<>();
        for(ItemClass_File icf: alicf_DownFiltered1){
            if(bReviewOrphansWMatchWMedia && icf.bOrphanAssociatedWithCatalogItem && !icf.bOrphanAssociatedCatalogItemIsMissingMedia){
                alicf_FilteredForImportReview.add(icf);
            } else {
                alicf_DownFiltered2.add(icf);
            }
        }
        for(ItemClass_File icf: alicf_DownFiltered2){
            if(bReviewOrphansWMatchWOMedia && icf.bOrphanAssociatedWithCatalogItem && icf.bOrphanAssociatedCatalogItemIsMissingMedia){
                alicf_FilteredForImportReview.add(icf);
            }
        }

        GlobalClass.galf_Orphaned_Files = alicf_FilteredForImportReview;

        GlobalClass.giSelectedCatalogMediaCategory = viewModel_catalogAnalysis.iMediaCategory;
        Intent intentImportGuided = new Intent(getApplicationContext(), Activity_Import.class);
        intentImportGuided.putExtra(EXTRA_BOOL_IMPORT_ORPHANED_FILES, true);

        startActivity(intentImportGuided);
        finish();
    }

    public void buttonNextClick_M3U8PerformUpdateNext(View v){


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
                case FRAGMENT_CAT_ANALYSIS_3_TRIM_OR_IMPORT:
                    return new Fragment_CatalogAnalysis_3_TrimOrImport();
                case FRAGMENT_CAT_ANALYSIS_3A_M3U8_PROCESS_SELECTION:
                    return new Fragment_CatalogAnalysis_3a_M3U8_Process_Select();
                case FRAGMENT_CAT_ANALYSIS_4_IMPORT_FILTER:
                    return new Fragment_CatalogAnalysis_4_ImportFilter();
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

    @SuppressWarnings("unchecked")
    public static class CatalogAnalysisResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            //Get boolean indicating that an error may have occurred:
            boolean bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);

            if (bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response from the Catalog Analysis worker:
                boolean bGetCatalogAnalysisFileItemsResponse = intent.getBooleanExtra(Worker_Catalog_Analysis.EXTRA_BOOL_GET_ARRAY_ORPHANED_FILEITEMS_RESPONSE, false);
                if (bGetCatalogAnalysisFileItemsResponse) {
                    galicf_CatalogAnalysisFileItems = (ArrayList<ItemClass_File>) intent.getSerializableExtra(Worker_Catalog_Analysis.EXTRA_AL_GET_ARRAY_ORPHANED_FILEITEMS_RESPONSE);

                    //Count items:
                    giOrphansWOMatch = 0;
                    giOrphansWMatchWMedia = 0;
                    giOrphansWMatchWOMedia = 0;
                    if(galicf_CatalogAnalysisFileItems != null) {
                        for (ItemClass_File icf : galicf_CatalogAnalysisFileItems) {
                            if (icf.bOrphanAssociatedWithCatalogItem) {
                                if (icf.bOrphanAssociatedCatalogItemIsMissingMedia) {
                                    giOrphansWMatchWOMedia++;
                                } else {
                                    giOrphansWMatchWMedia++;
                                }
                            } else {
                                giOrphansWOMatch++;
                            }
                        }
                    }
                }
                boolean bGetCatalogAnalysisMissingItemsResponse = intent.getBooleanExtra(Worker_Catalog_Analysis.EXTRA_BOOL_GET_ARRAY_MISSING_CAT_ITEMS_RESPONSE, false);
                if (bGetCatalogAnalysisMissingItemsResponse) {
                    gals_CatalogItemsMissingMedia = (ArrayList<String>) intent.getSerializableExtra(Worker_Catalog_Analysis.EXTRA_AL_GET_ARRAY_MISSING_CAT_ITEMS_RESPONSE);
                }


                boolean bGetCatalogAnalysisM3U8MisalignedPathsItemsResponse = intent.getBooleanExtra(Worker_Catalog_Analysis.EXTRA_BOOL_GET_ARRAY_M3U8_INTERN_PATH_ITEMS_RESPONSE, false);
                if (bGetCatalogAnalysisM3U8MisalignedPathsItemsResponse) {
                    gals_M3U8_CatItems_Misaligned_Paths = (ArrayList<String>) intent.getSerializableExtra(Worker_Catalog_Analysis.EXTRA_AL_GET_ARRAY_M3U8_INTERN_PATH_ITEMS_RESPONSE);
                }
                boolean bGetCatalogAnalysisM3U8MissingSegFilesResponse = intent.getBooleanExtra(Worker_Catalog_Analysis.EXTRA_BOOL_GET_ARRAY_M3U8_MISSING_SEG_ITEMS_RESPONSE, false);
                if (bGetCatalogAnalysisM3U8MissingSegFilesResponse) {
                    gals_M3U8_CatItems_Missing_Segments = (ArrayList<String>) intent.getSerializableExtra(Worker_Catalog_Analysis.EXTRA_AL_GET_ARRAY_M3U8_MISSING_SEG_ITEMS_RESPONSE);
                }



                boolean bGetCatalogAnalysisNoItemsResponse = intent.getBooleanExtra(Worker_Catalog_Analysis.EXTRA_BOOL_CAT_ANALYSIS_NO_ITEMS_RESPONSE, false);
                if(bGetCatalogAnalysisNoItemsResponse){
                    //todo: Catalog Analysis has completed, but no catalog items missing media or orphaned files were found.
                }








            }


        }
    }

}