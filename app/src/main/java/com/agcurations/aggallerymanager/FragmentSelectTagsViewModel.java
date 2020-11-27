package com.agcurations.aggallerymanager;

import android.nfc.Tag;

import java.util.ArrayList;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FragmentSelectTagsViewModel extends ViewModel {
    // TODO: Implement the ViewModel
    public ArrayList<TagItem> alTagsAll = new ArrayList<>();  //All possible tags //todo: Exclude restricted tags?
    public ArrayList<TagItem> alTagsInUse = new ArrayList<>(); //Tags currently in-use //todo: Exclude restricted tags?
    public final MutableLiveData<ArrayList<TagItem>> alTagsSelected = new MutableLiveData<>(); //Tags selected by the user, or already applied to a selected item.

    /*public void saveSelectedTags(ArrayList<TagItem> alts){
        alTagsSelected.setValue(alts);
    }

    public MutableLiveData<ArrayList<TagItem>> getSelectedTags() {
        return alTagsSelected;
    }*/

}