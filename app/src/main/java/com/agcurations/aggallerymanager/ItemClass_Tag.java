package com.agcurations.aggallerymanager;

public class ItemClass_Tag {

    public final Integer TagID;
    public final String TagText;
    public Boolean isRestricted = false;

    //Create a variable to be used to preserve the order in which items are selected.
    //  This is needed because the first tag may be used for special purposes.
    public int iSelectionOrder;
    //Used for Tag ListViews in selecting tags:
    public Boolean isChecked;

    public ItemClass_Tag(int _TagID, String _TagText) {
        this.TagID = _TagID;
        this.TagText = _TagText;
    }
}
