package com.agcurations.aggallerymanager;

import java.util.ArrayList;

import androidx.lifecycle.ViewModel;

public class ViewModel_TagEditor extends ViewModel {
    public int iTagEditorMediaCategory;

    public ArrayList<ItemClass_Tag> alNewTags;
    public boolean bTagAdded = false;
    public boolean bTagRenamed = false;
    public boolean bTagDeleted = false;
}
