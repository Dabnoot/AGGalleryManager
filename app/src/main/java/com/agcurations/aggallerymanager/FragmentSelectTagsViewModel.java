package com.agcurations.aggallerymanager;

import java.util.ArrayList;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FragmentSelectTagsViewModel extends ViewModel {
    // TODO: Implement the ViewModel
    public final ArrayList<TagItem> alTagsAll = new ArrayList<>();  //All possible tags //todo: Exclude restricted tags?
    public ArrayList<TagItem> alTagsInUse = new ArrayList<>(); //Tags currently in-use //todo: Exclude restricted tags?
    public final MutableLiveData<ArrayList<TagItem>> altiTagsSelected =
            new MutableLiveData<>(); //Tags selected by the user, or already applied to a selected item.
    public final MutableLiveData<TagItem> tiTagItemRemoved =
            new MutableLiveData<>(); //Used to notify that a tag has been removed by the user.
                                    // Needed when bulk assigning during import. As some items
                                    //   may have been individually assigned multiple tags
    public final MutableLiveData<TagItem> tiTagItemAdded =
            new MutableLiveData<>(); //Used to notify that a tag has been added by the user.
                                    // Needed when bulk assigning during import. As some items
                                    //   may have been individually assigned multiple tags.

    public void setSelectedTags(ArrayList<TagItem> altiNew){

        //Find which tag has been added or removed, and set tiTagItemRemoved or tiTagItemAdded:
        if((altiNew != null) && (altiTagsSelected.getValue() != null)) {
            //If the size of the list has decreased by 1:
            int iNewSize = altiNew.size() - altiTagsSelected.getValue().size();
            if (iNewSize == -1) {
                tiTagItemAdded.setValue(null);
                //Search for the tag item that was removed:
                boolean bTagFound;
                for(TagItem tiOld : altiTagsSelected.getValue()){
                    bTagFound = false;
                    for (TagItem tiIncoming : altiNew) {
                        if(tiIncoming.TagID == tiOld.TagID){
                            bTagFound = true;
                            break;
                        }
                    }
                    if(!bTagFound){
                        //We have found the deleted tag in altiTagsSelected.
                        //  Record the deleted tag item:
                        tiTagItemRemoved.setValue(tiOld);
                        break;
                    }
                }
            } else if (iNewSize == 1){
                tiTagItemRemoved.setValue(null);
                //Search for the tag item that was added:
                boolean bTagFound;
                for (TagItem tiIncoming : altiNew) {
                    bTagFound = false;
                    for(TagItem tiOld : altiTagsSelected.getValue()){
                        if(tiIncoming.TagID == tiOld.TagID){
                            bTagFound = true;
                            break;
                        }
                    }
                    if(!bTagFound){
                        //We have found the added tag in altiTagsSelected.
                        //  Record the deleted tag item:
                        tiTagItemAdded.setValue(tiIncoming);
                        break;
                    }
                }
            }
        } else if(altiNew != null){
            //This is the situation in which this is the first tag added, and altiTagsSelected
            //  .getValue returns null because nothing is stored yet.
            //Check to see if only 1 tag has been added, and if so, update tiTagItemAdded:
            tiTagItemRemoved.setValue(null);
            if(altiNew.size() == 1){
                tiTagItemAdded.setValue(altiNew.get(0));
            }
        }


        altiTagsSelected.setValue(altiNew);
    }


    /*public MutableLiveData<ArrayList<TagItem>> getSelectedTags() {
        return altiTagsSelected;
    }*/

}