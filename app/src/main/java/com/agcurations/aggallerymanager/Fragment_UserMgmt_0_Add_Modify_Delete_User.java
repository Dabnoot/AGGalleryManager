package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Fragment_UserMgmt_0_Add_Modify_Delete_User extends Fragment {

    private String mParam2;

    public Fragment_UserMgmt_0_Add_Modify_Delete_User() {
        // Required empty public constructor
    }

    public static Fragment_UserMgmt_0_Add_Modify_Delete_User newInstance(String param1, String param2) {
        return new Fragment_UserMgmt_0_Add_Modify_Delete_User();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_mgmt_0_add_modify_delete_user, container, false);
    }
}