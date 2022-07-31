package com.agcurations.aggallerymanager;

import java.util.ArrayList;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ViewModel_Fragment_SelectTags extends ViewModel {

    public final ArrayList<ItemClass_Tag> alTagsAll = new ArrayList<>();  //All possible tags

    public final MutableLiveData<ArrayList<ItemClass_Tag>> altiTagsSelected =
            new MutableLiveData<>(); //Tags selected by the user, or already applied to a selected item.

    public final MutableLiveData<ArrayList<ItemClass_Tag>> tiTagItemsRemoved =
            new MutableLiveData<>(); //Used to notify that a tag has been removed by the user.
                                    // Needed when bulk assigning during import. As some items
                                    //   may have been individually assigned multiple tags

    public final MutableLiveData<ItemClass_Tag> tiTagItemAdded =
            new MutableLiveData<>(); //Used to notify that a tag has been added by the user.
                                    // Needed when bulk assigning during import. As some items
                                    //   may have been individually assigned multiple tags.

    public final MutableLiveData<Boolean> bTagEditorRequestsReloadTags =
            new MutableLiveData<>();  //To enable notification of a viewer that it should reload tags.

    public final MutableLiveData<Boolean> bTagDeleted =
            new MutableLiveData<>();  //To enable notification of a viewer that it should reload
                                        // file. A Tag directory was deleted, and some files may'
                                        // have been moved.

    public int iMediaCategory = -1;

    public boolean bShowModeCompoundTagUse = false; //Set to true to show tags and frequencies that
                                                    // are in-use alongside tags that the user is
                                                    // selecting. For use with filtering.

    public final MutableLiveData<ArrayList<ItemClass_Tag>> altiTagSuggestions =
            new MutableLiveData<>(); //To popup recently used tags to simplify tagging by user.

    public void setSelectedTags(ArrayList<ItemClass_Tag> altiNewListOfSelectedTags){

        //Find which tag has been added or removed, and set tiTagItemRemoved or tiTagItemAdded:
        if((altiNewListOfSelectedTags != null) && (altiTagsSelected.getValue() != null)) {

            int iNewSize = altiNewListOfSelectedTags.size() - altiTagsSelected.getValue().size();

            if (iNewSize <= -1) { //If the size of the list has decreased:
                tiTagItemAdded.setValue(null);
                //Search for the tag items that were removed (multiple only happens when the user clears the list):
                ArrayList<ItemClass_Tag> altiUnselectedTags = new ArrayList<>();
                for(ItemClass_Tag ictTagPreviouslySelected : altiTagsSelected.getValue()){
                    boolean bThisItemNotFoundInNewList = true;
                    for (ItemClass_Tag tiIncoming : altiNewListOfSelectedTags) {
                        if(tiIncoming.iTagID.equals(ictTagPreviouslySelected.iTagID)){
                            bThisItemNotFoundInNewList = false;
                        }
                    }
                    if(bThisItemNotFoundInNewList){
                        altiUnselectedTags.add(ictTagPreviouslySelected);
                    }

                }
                if(altiUnselectedTags.size() > 0){
                    //We have found deleted tags in altiTagsSelected.
                    //  Record the deleted tag items:
                    tiTagItemsRemoved.setValue(altiUnselectedTags);
                }
            } else if (iNewSize == 1){
                tiTagItemsRemoved.setValue(null);
                //Search for the tag item that was added:
                boolean bTagFound;
                for (ItemClass_Tag tiIncoming : altiNewListOfSelectedTags) {
                    bTagFound = false;
                    for(ItemClass_Tag tiOld : altiTagsSelected.getValue()){
                        if(tiIncoming.iTagID.equals(tiOld.iTagID)){
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
        } else if(altiNewListOfSelectedTags != null){
            //This is the situation in which this is the first tag added, and altiTagsSelected
            //  .getValue returns null because nothing is stored yet.
            //Check to see if only 1 tag has been added, and if so, update tiTagItemAdded:
            tiTagItemsRemoved.setValue(null);
            if(altiNewListOfSelectedTags.size() == 1){
                tiTagItemAdded.setValue(altiNewListOfSelectedTags.get(0));
            }
        }


        altiTagsSelected.setValue(altiNewListOfSelectedTags);
    }


    /*public MutableLiveData<ArrayList<TagItem>> getSelectedTags() {
        return altiTagsSelected;
    }*/

}