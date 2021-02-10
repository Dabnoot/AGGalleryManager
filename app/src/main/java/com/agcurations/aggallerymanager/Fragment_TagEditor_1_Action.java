package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_TagEditor_1_Action#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_TagEditor_1_Action extends Fragment {

    public Fragment_TagEditor_1_Action() {
        // Required empty public constructor
    }

    public static Fragment_TagEditor_1_Action newInstance() {
        return new Fragment_TagEditor_1_Action();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tag_editor_1_action, container, false);
    }
}