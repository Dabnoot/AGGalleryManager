package com.agcurations.aggallerymanager;

import java.util.ArrayList;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class FragmentImportViewPagerAdapter extends FragmentStateAdapter {

    private ArrayList<Fragment> arrayList = new ArrayList<>();

    public FragmentImportViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new FragmentImport_0_MediaCategory();
            case 1:
                return new FragmentImport_1_StorageLocation();
            case 2:
                return new FragmentImport_2_SelectItems();

        }
        return null;
    }

    @Override
    public int getItemCount() {
        return 3;
    }

}