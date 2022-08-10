package com.agcurations.aggallerymanager;

import android.content.Context;

import co.lujun.androidtagview.TagView;

public class TagViewWithID extends TagView {
    //This class created to enable carrying of a TagID with each TagView instance.
    public int mTagID = -1; //Initialize to what is intended to denote "unassigned".

    public TagViewWithID(Context context, String text) {
        super(context, text);
    }

    public TagViewWithID(Context context, String text, int defaultImageID) {
        super(context, text, defaultImageID);
    }

    public TagViewWithID(Context context, int iTagID, String text) {
        super(context, text);
        mTagID = iTagID;
    }

}
