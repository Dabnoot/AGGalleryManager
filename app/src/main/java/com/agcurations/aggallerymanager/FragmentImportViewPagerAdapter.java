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
            case ImportActivity.FRAGMENT_IMPORT_0_ID_MEDIA_CATEGORY:
                return new FragmentImport_0_MediaCategory();
            case ImportActivity.FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION:
                return new FragmentImport_1_StorageLocation();
            case ImportActivity.FRAGMENT_IMPORT_2_ID_SELECT_ITEMS:
                return new FragmentImport_2_SelectItems();
            case ImportActivity.FRAGMENT_IMPORT_3_ID_SELECT_TAGS:
                return new FragmentImport_3_SelectTags();
            case ImportActivity.FRAGMENT_IMPORT_4_ID_IMPORT_METHOD:
                return new FragmentImport_4_ImportMethod();
            case ImportActivity.FRAGMENT_IMPORT_5_ID_CONFIRMATION:
                return new FragmentImport_5_Confirmation();

        }
        return null;
    }

    @Override
    public int getItemCount() {
        return ImportActivity.FRAGMENT_COUNT;
    }

}