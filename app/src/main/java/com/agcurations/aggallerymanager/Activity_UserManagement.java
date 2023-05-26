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
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;

import java.util.Stack;

public class Activity_UserManagement extends AppCompatActivity {

    public ViewPager2 viewPager2_UserManagement;
    FragmentUserMgmtViewPagerAdapter fragmentUserMgmtViewPagerAdapter;

    private ViewModel_UserManagement viewModelUserManagement;

    //Fragment page indexes:
    public static final int FRAGMENT_USERMGMT_0_ADD_MODIFY_DELETE_USER = 0;
    public static final int FRAGMENT_USERMGMT_1_ADD_USER = 1;
    public static final int FRAGMENT_USERMGMT_3_DELETE_USER = 2;

    public static final int FRAGMENT_COUNT = 3;

    static Stack<Integer> stackFragmentOrder;
    private static int giStartingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setTitle("User Management");

        viewPager2_UserManagement = findViewById(R.id.viewPager2_UserManagement);

        //Instantiate the ViewModel sharing data between fragments:
        viewModelUserManagement = new ViewModelProvider(this).get(ViewModel_UserManagement.class);

        fragmentUserMgmtViewPagerAdapter = new FragmentUserMgmtViewPagerAdapter(getSupportFragmentManager(), getLifecycle());

        viewPager2_UserManagement.setAdapter(fragmentUserMgmtViewPagerAdapter);

        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        viewPager2_UserManagement.setUserInputEnabled(false);

        viewPager2_UserManagement.setCurrentItem(FRAGMENT_USERMGMT_0_ADD_MODIFY_DELETE_USER, false);

        stackFragmentOrder = new Stack<>();
        giStartingFragment = FRAGMENT_USERMGMT_0_ADD_MODIFY_DELETE_USER;
        stackFragmentOrder.push(giStartingFragment);
    }

    @Override
    protected void onDestroy() {

        //Mark user items as unchecked so that they don't appear pre-selected in any user list:
        for(ItemClass_User icu: GlobalClass.galicu_Users){
            icu.bIsChecked = false;
        }

        super.onDestroy();
    }

    //======================================================
    //======  FRAGMENT NAVIGATION ROUTINES  ================
    //======================================================


    @Override
    public void onBackPressed() {

        if(stackFragmentOrder.empty()){
            finish();
        } else {
            //Go back through the fragments in the order by which we progressed.
            //  Some user selections will cause a fragment to be skipped, and we don't want
            //  to go back to those skipped fragments, hence the use of a Stack, and pop().
            int iCurrentFragment = viewPager2_UserManagement.getCurrentItem();
            int iPrevFragment = stackFragmentOrder.pop();
            if((iCurrentFragment == iPrevFragment) && (iCurrentFragment == giStartingFragment)){
                finish();
                return;
            }
            if(iCurrentFragment == iPrevFragment){
                //To handle interesting behavior about how the stack is built.
                iPrevFragment = stackFragmentOrder.peek();
            }
            viewPager2_UserManagement.setCurrentItem(iPrevFragment, false);

            if(iPrevFragment == giStartingFragment){
                //Go home:
                stackFragmentOrder.push(giStartingFragment);
            }
        }
    }

    public void buttonNextClick_UserOperationSelected(View v){
        RadioButton radioButton_AddUser = findViewById(R.id.radioButton_AddUser);
        RadioButton radioButton_ModifyUser = findViewById(R.id.radioButton_ModifyUser);

        if (radioButton_AddUser.isChecked()){
            viewModelUserManagement.iUserAddOrEditMode = ViewModel_UserManagement.USER_ADD_MODE;
            viewPager2_UserManagement.setCurrentItem(FRAGMENT_USERMGMT_1_ADD_USER, false);
        } else if (radioButton_ModifyUser.isChecked()){
            viewModelUserManagement.iUserAddOrEditMode = ViewModel_UserManagement.USER_EDIT_MODE;
            viewPager2_UserManagement.setCurrentItem(FRAGMENT_USERMGMT_1_ADD_USER, false); //Add user fragment doubles as "modify user".
        } else {
            viewPager2_UserManagement.setCurrentItem(FRAGMENT_USERMGMT_3_DELETE_USER, false);
        }
    }


    public void buttonClick_Cancel(View v){
        finish();
    }




    //=============================================================================================
    //================= ADAPTERS SECTION ==========================================================
    //=============================================================================================

    public static class FragmentUserMgmtViewPagerAdapter extends FragmentStateAdapter {

        public FragmentUserMgmtViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case FRAGMENT_USERMGMT_1_ADD_USER:
                    return new Fragment_UserMgmt_1_Add_User();
                case FRAGMENT_USERMGMT_3_DELETE_USER:
                    return new Fragment_UserMgmt_3_Delete_User();
                default:
                    return new Fragment_UserMgmt_0_Add_Modify_Delete_User();
            }
        }

        @Override
        public int getItemCount() {
            return FRAGMENT_COUNT;
        }

    }




}