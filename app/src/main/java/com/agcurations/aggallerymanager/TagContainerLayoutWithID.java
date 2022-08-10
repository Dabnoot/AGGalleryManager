package com.agcurations.aggallerymanager;

import android.content.Context;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

import co.lujun.androidtagview.TagContainerLayout;

public class TagContainerLayoutWithID extends TagContainerLayout {

    public TagContainerLayoutWithID(Context context) {
        super(context);
    }

    public TagContainerLayoutWithID(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TagContainerLayoutWithID(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    ArrayList<ItemClass_Tag> galict_TagsWithIDs;

    public void setTags(ArrayList<ItemClass_Tag> tags) {
        galict_TagsWithIDs = new ArrayList<>(tags);

        List<String> lsTags = new ArrayList<>();

        for(ItemClass_Tag ict: tags){
            lsTags.add(ict.sTagText);
        }
        super.setTags(lsTags);
    }

    public ItemClass_Tag getTagItem(int position){

        if(galict_TagsWithIDs != null){
            if(position < galict_TagsWithIDs.size()){
                return galict_TagsWithIDs.get(position);
            }
        }
        return null;
    }




}
