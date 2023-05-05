package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;

public class Activity_UserManagement extends AppCompatActivity {

    public ViewPager2 viewPager2_UserManagement;
    FragmentUserMgmtViewPagerAdapter fragmentUserMgmtViewPagerAdapter;

    //Fragment page indexes:
    public static final int FRAGMENT_USERMGMT_0_ADD_MODIFY_DELETE_USER = 0;
    public static final int FRAGMENT_USERMGMT_1_ADD_USER = 1;
    public static final int FRAGMENT_USERMGMT_2_MODIFY_USER = 2;
    public static final int FRAGMENT_USERMGMT_3_DELETE_USER = 3;

    public static final int FRAGMENT_COUNT = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setTitle("User Management");

        viewPager2_UserManagement = findViewById(R.id.viewPager2_UserManagement);

        fragmentUserMgmtViewPagerAdapter = new FragmentUserMgmtViewPagerAdapter(getSupportFragmentManager(), getLifecycle());

        viewPager2_UserManagement.setAdapter(fragmentUserMgmtViewPagerAdapter);

        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        viewPager2_UserManagement.setUserInputEnabled(false);

        viewPager2_UserManagement.setCurrentItem(FRAGMENT_USERMGMT_0_ADD_MODIFY_DELETE_USER, false);
    }



    public void buttonNextClick_UserOperationSelected(View v){
        RadioButton radioButton_AddUser = findViewById(R.id.radioButton_AddUser);
        RadioButton radioButton_ModifyUser = findViewById(R.id.radioButton_ModifyUser);

        if (radioButton_AddUser.isChecked()){
            viewPager2_UserManagement.setCurrentItem(FRAGMENT_USERMGMT_1_ADD_USER, false);
        } else if (radioButton_ModifyUser.isChecked()){
            viewPager2_UserManagement.setCurrentItem(FRAGMENT_USERMGMT_2_MODIFY_USER, false);
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
                case FRAGMENT_USERMGMT_2_MODIFY_USER:
                    return new Fragment_UserMgmt_2_Modify_User();
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