package com.agcurations.aggallerymanager;

import java.io.Serializable;

public class ItemClass_Tag implements Serializable {

    public final Integer iTagID;
    @SuppressWarnings("CanBeFinal")
    public String sTagText;
    public String sTagDescription = "";
    public Boolean bIsRestricted = false;
    public int iTagAgeRating = AdapterTagMaturityRatings.TAG_AGE_RATING_RP;

    //Create a variable to be used to preserve the order in which items are selected.
    //  This is needed because the first tag may be used for special purposes.
    public int iSelectionOrder;
    //Used for Tag ListViews in selecting tags:
    public Boolean bIsChecked = false;

    int iHistogramCount = 0;            //To count the number of times this tag is used.
    int iHistogramCountXref = 0;        //Not for global use. Used to facilitate counting the
                                        //  number of times this tag is used alongside other tags.

    public ItemClass_Tag(int _iTagID, String _sTagText) {
        this.iTagID = _iTagID;
        this.sTagText = _sTagText;
    }
}
