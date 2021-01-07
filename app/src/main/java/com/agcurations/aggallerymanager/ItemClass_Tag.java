package com.agcurations.aggallerymanager;

import java.io.Serializable;

public class ItemClass_Tag implements Serializable {

    public final Integer TagID;
    public String TagText;
    public Boolean isRestricted = false;

    //Create a variable to be used to preserve the order in which items are selected.
    //  This is needed because the first tag may be used for special purposes.
    public int iSelectionOrder;
    //Used for Tag ListViews in selecting tags:
    public Boolean isChecked = false;

    public ItemClass_Tag(int _TagID, String _TagText) {
        this.TagID = _TagID;
        this.TagText = _TagText;
    }
}
