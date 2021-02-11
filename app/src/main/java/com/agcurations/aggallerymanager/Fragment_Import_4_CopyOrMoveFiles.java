package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_Import_4_CopyOrMoveFiles#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_Import_4_CopyOrMoveFiles extends Fragment {

    public Fragment_Import_4_CopyOrMoveFiles() {
        // Required empty public constructor
    }

    public static Fragment_Import_4_CopyOrMoveFiles newInstance() {
        return new Fragment_Import_4_CopyOrMoveFiles();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_4_import_method, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }
}