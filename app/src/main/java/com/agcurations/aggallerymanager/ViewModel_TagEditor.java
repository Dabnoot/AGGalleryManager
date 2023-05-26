package com.agcurations.aggallerymanager;

import java.util.ArrayList;

import androidx.lifecycle.ViewModel;

public class ViewModel_TagEditor extends ViewModel {
    public int iTagEditorMediaCategory;

    public ArrayList<ItemClass_Tag> alNewTags;
    public boolean bTagAdded = false;
    public boolean bTagDataUpdated = false;
    public boolean bTagDeleted = false;

    public int iTagAddOrEditMode = 0;
    public final static int TAG_ADD_MODE = 1;
    public final static int TAG_EDIT_MODE = 2;

}
