package com.agcurations.aggallerymanager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.Stack;

public class Activity_LogViewer extends AppCompatActivity {

    GlobalClass globalClass;

    private ViewModel_Fragment_LogViewer viewModel_fragment_logViewer;

    public ViewPager2 viewPager_LogViewer;

    public static final int FRAGMENT_LOG_VIEWER_0_FILE_LIST = 0; //View list of log files
    public static final int FRAGMENT_LOG_VIEWER_1_VIEW = 1; //View a log
    public static final int FRAGMENT_LOG_VIEWER_1A_EXECUTE_DELETE = 2;

    public static final int FRAGMENT_COUNT = 3;

    static Stack<Integer> stackFragmentOrder;
    private static int giStartingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        globalClass = (GlobalClass) getApplicationContext();

        setTitle("Log Viewer");

        viewPager_LogViewer = findViewById(R.id.viewPager_LogViewer);
        // set Orientation in your ViewPager2
        viewPager_LogViewer.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        //Set adapter for ViewPager2:
        ViewPagerAdapter_LogViewer viewPagerAdapter_LogViewer = new ViewPagerAdapter_LogViewer(getSupportFragmentManager(), getLifecycle());
        viewPager_LogViewer.setAdapter(viewPagerAdapter_LogViewer);
        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        viewPager_LogViewer.setUserInputEnabled(false);


        //Instantiate the ViewModel sharing data between fragments:
        viewModel_fragment_logViewer = new ViewModelProvider(this).get(ViewModel_Fragment_LogViewer.class);

        stackFragmentOrder = new Stack<>();
        giStartingFragment = FRAGMENT_LOG_VIEWER_0_FILE_LIST;
        stackFragmentOrder.push(giStartingFragment);
        if(GlobalClass.gabGeneralFileDeletionRunning.get()){
            stackFragmentOrder.push(FRAGMENT_LOG_VIEWER_1A_EXECUTE_DELETE);
            viewPager_LogViewer.setCurrentItem(FRAGMENT_LOG_VIEWER_1A_EXECUTE_DELETE, false);
        }

        boolean ENABLED = true;
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(ENABLED) {
            @Override
            public void handleOnBackPressed() {
                if (stackFragmentOrder.empty()) {
                    finish();
                } else {
                    //Go back through the fragments in the order by which we progressed.
                    //  Some user selections will cause a fragment to be skipped, and we don't want
                    //  to go back to those skipped fragments, hence the use of a Stack, and pop().
                    int iCurrentFragment = viewPager_LogViewer.getCurrentItem();
                    int iPrevFragment = stackFragmentOrder.pop();
                    if ((iCurrentFragment == iPrevFragment) && (iCurrentFragment == giStartingFragment)) {
                        finish();
                        return;
                    }
                    if (iCurrentFragment == iPrevFragment) {
                        //To handle interesting behavior about how the stack is built.
                        iPrevFragment = stackFragmentOrder.peek();
                    }
                    viewPager_LogViewer.setCurrentItem(iPrevFragment, false);

                    if (iPrevFragment == giStartingFragment) {
                        //Go home:
                        stackFragmentOrder.push(giStartingFragment);
                    }
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        GlobalClass.gabGeneralFileDeletionCancel.set(false);
        super.onDestroy();
    }

    public void gotoViewer(){

        //Go to the import folder selection fragment:
        viewPager_LogViewer.setCurrentItem(FRAGMENT_LOG_VIEWER_1_VIEW, false);
        stackFragmentOrder.push(viewPager_LogViewer.getCurrentItem());

    }

    //=======================
    //   Button Routines
    //=======================

    public void button_Delete(View v) {
        GlobalClass.galicf_FilesToDeleteDataTransfer = new ArrayList<>(viewModel_fragment_logViewer.alicf_LogFiles);
        GlobalClass.gabGeneralFileDeletionStart.set(true);
        viewPager_LogViewer.setCurrentItem(FRAGMENT_LOG_VIEWER_1A_EXECUTE_DELETE, false);
        stackFragmentOrder.push(viewPager_LogViewer.getCurrentItem());
    }

    public void button_Cancel(View v){
        GlobalClass.gabGeneralFileDeletionCancel.set(true);
    }

    public void button_Finish(View v){
        finish();
    }


    //======================
    //=====  Adapters =======
    //======================

    public static class ViewPagerAdapter_LogViewer extends FragmentStateAdapter {

        public ViewPagerAdapter_LogViewer(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case FRAGMENT_LOG_VIEWER_1_VIEW:
                    return new Fragment_LogViewer_1_View();
                case FRAGMENT_LOG_VIEWER_1A_EXECUTE_DELETE:
                    return new Fragment_LogViewer_1a_ExecuteDelete();
                default:
                    return new Fragment_LogViewer_0_FileList();
            }
        }

        @Override
        public int getItemCount() {
            return FRAGMENT_COUNT;
        }

    }



}