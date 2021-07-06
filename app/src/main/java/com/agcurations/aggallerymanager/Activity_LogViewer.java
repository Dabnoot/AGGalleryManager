package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Stack;

public class Activity_LogViewer extends AppCompatActivity {

    GlobalClass globalClass;

    private ViewModel_Fragment_LogViewer viewModel_fragment_logViewer;

    public ViewPager2 viewPager_LogViewer;

    public static final int FRAGMENT_LOG_VIEWER_0_FILE_LIST = 0; //View list of log files
    public static final int FRAGMENT_LOG_VIEWER_1_VIEW = 1; //View a log

    public static final int FRAGMENT_COUNT = 2;

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

    }

    @Override
    public void onBackPressed() {

        if(stackFragmentOrder.empty()){
            finish();
        } else {
            //Go back through the fragments in the order by which we progressed.
            //  Some user selections will cause a fragment to be skipped, and we don't want
            //  to go back to those skipped fragments, hence the use of a Stack, and pop().
            int iCurrentFragment = viewPager_LogViewer.getCurrentItem();
            int iPrevFragment = stackFragmentOrder.pop();
            if((iCurrentFragment == iPrevFragment) && (iCurrentFragment == giStartingFragment)){
                finish();
                return;
            }
            if(iCurrentFragment == iPrevFragment){
                //To handle interesting behavior about how the stack is built.
                iPrevFragment = stackFragmentOrder.peek();
            }
            viewPager_LogViewer.setCurrentItem(iPrevFragment, false);

            if(iPrevFragment == giStartingFragment){
                //Go home:
                stackFragmentOrder.push(giStartingFragment);
            }
        }
    }

    public void gotoViewer(){

        //Go to the import folder selection fragment:
        viewPager_LogViewer.setCurrentItem(FRAGMENT_LOG_VIEWER_1_VIEW, false);
        stackFragmentOrder.push(viewPager_LogViewer.getCurrentItem());

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