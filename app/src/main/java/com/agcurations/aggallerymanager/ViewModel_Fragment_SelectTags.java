package com.agcurations.aggallerymanager;

import java.util.ArrayList;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ViewModel_Fragment_SelectTags extends ViewModel {
    // TODO: Implement the ViewModel
    public final ArrayList<ItemClass_Tag> alTagsAll = new ArrayList<>();  //All possible tags
    public final MutableLiveData<ArrayList<ItemClass_Tag>> altiTagsSelected =
            new MutableLiveData<>(); //Tags selected by the user, or already applied to a selected item.
    public final MutableLiveData<ItemClass_Tag> tiTagItemRemoved =
            new MutableLiveData<>(); //Used to notify that a tag has been removed by the user.
                                    // Needed when bulk assigning during import. As some items
                                    //   may have been individually assigned multiple tags
    public final MutableLiveData<ItemClass_Tag> tiTagItemAdded =
            new MutableLiveData<>(); //Used to notify that a tag has been added by the user.
                                    // Needed when bulk assigning during import. As some items
                                    //   may have been individually assigned multiple tags.
    public int iMediaCategory = -1;
    public int iTabLayoutListingSelection = 0;

    public void setSelectedTags(ArrayList<ItemClass_Tag> altiNew){

        //Find which tag has been added or removed, and set tiTagItemRemoved or tiTagItemAdded:
        if((altiNew != null) && (altiTagsSelected.getValue() != null)) {
            //If the size of the list has decreased by 1:
            int iNewSize = altiNew.size() - altiTagsSelected.getValue().size();
            if (iNewSize == -1) {
                tiTagItemAdded.setValue(null);
                //Search for the tag item that was removed:
                boolean bTagFound;
                for(ItemClass_Tag tiOld : altiTagsSelected.getValue()){
                    bTagFound = false;
                    for (ItemClass_Tag tiIncoming : altiNew) {
                        if(tiIncoming.TagID.equals(tiOld.TagID)){
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
                for (ItemClass_Tag tiIncoming : altiNew) {
                    bTagFound = false;
                    for(ItemClass_Tag tiOld : altiTagsSelected.getValue()){
                        if(tiIncoming.TagID.equals(tiOld.TagID)){
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