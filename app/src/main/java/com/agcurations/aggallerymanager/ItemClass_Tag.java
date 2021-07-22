package com.agcurations.aggallerymanager;

import java.io.Serializable;

public class ItemClass_Tag implements Serializable {

    public final Integer iTagID;
    @SuppressWarnings("CanBeFinal")
    public String sTagText;
    public String sTagDescription = "";
    public Boolean bIsRestricted = false;
    public Boolean bIsDeleted = false;

    //Create a variable to be used to preserve the order in which items are selected.
    //  This is needed because the first tag may be used for special purposes.
    public int iSelectionOrder;
    //Used for Tag ListViews in selecting tags:
    public Boolean bIsChecked = false;

    public ItemClass_Tag(int _iTagID, String _sTagText) {
        this.iTagID = _iTagID;
        this.sTagText = _sTagText;
    }
}
