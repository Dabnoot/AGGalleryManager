package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;

public class ItemClass_Tag implements Serializable {

    public final Integer iTagID;
    @SuppressWarnings("CanBeFinal")
    public String sTagText;
    public String sTagDescription = "";
    public Boolean bIsRestricted = false;
    public int iMaturityRating = AdapterMaturityRatings.MATURITY_RATING_RP;

    public ArrayList<String> alsTagApprovedUsers = null;
    //A list of users to whom this tag, and items carrying this tag, is restricted. Other users
    //  will not be able to see this tag nor the items to which it has been assigned.

    //Create a variable to be used to preserve the order in which items are selected.
    //  This is needed because the first tag may be used for special purposes.
    public int iSelectionOrder;
    //Used for Tag ListViews in selecting tags:
    public Boolean bIsChecked = false;

    int iHistogramCount = 0;            //To count the number of times this tag is used.

    public boolean bIllegalDataFound = false; //Used during tag creation or editing.
    public String sIllegalDataNarrative = "";

    public ItemClass_Tag(int _iTagID, @NonNull String _sTagText) {
        this.iTagID = _iTagID;
        this.sTagText = _sTagText;
    }
}
