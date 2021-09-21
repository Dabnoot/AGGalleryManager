package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

import java.util.Stack;

public class Activity_WorkerConsole extends AppCompatActivity {

    GlobalClass globalClass;

    private ViewModel_Fragment_WorkerConsole viewModel_fragment_workerConsole;

    public ViewPager2 viewPager_WorkerViewer;

    public static final int FRAGMENT_WORKER_CONSOLE_0_WORKER_LIST = 0; //View list of workers and their status.
    public static final int FRAGMENT_WORKER_CONSOLE_1_WORKER_DETAILS = 1; //View details regarding a worker, such as job file and log.

    public static final int FRAGMENT_COUNT = 2;

    static Stack<Integer> stackFragmentOrder;
    private static int giStartingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_console);

        globalClass = (GlobalClass) getApplicationContext();

        setTitle("Worker Console");

        viewPager_WorkerViewer = findViewById(R.id.viewPager_WorkerViewer);
        // set Orientation in your ViewPager2
        viewPager_WorkerViewer.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        //Set adapter for ViewPager2:
        ViewPagerAdapter_WorkerViewer viewPagerAdapter_workerViewer = new ViewPagerAdapter_WorkerViewer(getSupportFragmentManager(), getLifecycle());
        viewPager_WorkerViewer.setAdapter(viewPagerAdapter_workerViewer);
        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        viewPager_WorkerViewer.setUserInputEnabled(false);


        //Instantiate the ViewModel sharing data between fragments:
        viewModel_fragment_workerConsole = new ViewModelProvider(this).get(ViewModel_Fragment_WorkerConsole.class);

        stackFragmentOrder = new Stack<>();
        giStartingFragment = FRAGMENT_WORKER_CONSOLE_0_WORKER_LIST;

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
            int iCurrentFragment = viewPager_WorkerViewer.getCurrentItem();
            int iPrevFragment = stackFragmentOrder.pop();
            if((iCurrentFragment == iPrevFragment) && (iCurrentFragment == giStartingFragment)){
                finish();
                return;
            }
            if(iCurrentFragment == iPrevFragment){
                //To handle interesting behavior about how the stack is built.
                iPrevFragment = stackFragmentOrder.peek();
            }
            viewPager_WorkerViewer.setCurrentItem(iPrevFragment, false);

            if(iPrevFragment == giStartingFragment){
                //Go home:
                stackFragmentOrder.push(giStartingFragment);
            }
        }
    }

    public void gotoWorkerDetails(){

        viewPager_WorkerViewer.setCurrentItem(FRAGMENT_WORKER_CONSOLE_1_WORKER_DETAILS, false);
        stackFragmentOrder.push(viewPager_WorkerViewer.getCurrentItem());

    }


    //======================
    //=====  Adapters =======
    //======================

    public static class ViewPagerAdapter_WorkerViewer extends FragmentStateAdapter {

        public ViewPagerAdapter_WorkerViewer(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case FRAGMENT_WORKER_CONSOLE_1_WORKER_DETAILS:
                    return new Fragment_WorkerConsole_1_Worker_Details();
                default:
                    return new Fragment_WorkerConsole_0_WorkerList();
            }
        }

        @Override
        public int getItemCount() {
            return FRAGMENT_COUNT;
        }

    }
}