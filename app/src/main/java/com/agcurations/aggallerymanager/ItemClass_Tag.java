package com.agcurations.aggallerymanager;

public class ItemClass_Tag {
    //Used for Tag ListViews
    public Boolean isChecked;
    public final int TagID;
    public final String TagText;

    //Create a variable to be used to preserve the order in which items are selected.
    //  This is needed because the first tag may be used for special purposes.
    public int SelectionOrder;

    public ItemClass_Tag(Boolean _isChecked, int _TagID, String _TagText, int _SelectionOrder) {
        this.isChecked = _isChecked;
        this.TagID = _TagID;
        this.TagText = _TagText;
        this.SelectionOrder = _SelectionOrder;
    }
}
