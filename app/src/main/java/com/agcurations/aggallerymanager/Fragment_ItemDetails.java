package com.agcurations.aggallerymanager;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_ItemDetails#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_ItemDetails extends Fragment {

    public final static String CATALOG_ITEM = "CATALOG_ITEM";

    private ItemClass_CatalogItem gciCatalogItem;

    GlobalClass globalClass;

    int[] giGradeImageViews;

    String gsNewTagIDs;
    String gsPreviousTagIDs;
    int giNewGrade;
    int giPreviousGrade;

    ViewModel_Fragment_SelectTags gViewModel_fragment_selectTags;

    public Fragment_ItemDetails() {
        // Required empty public constructor
    }

    public static Fragment_ItemDetails newInstance() {
        return new Fragment_ItemDetails();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_item_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Get the Media Category argument passed to this fragment:
        Bundle args = getArguments();

        if(args != null) {
            gciCatalogItem = (ItemClass_CatalogItem) args.getSerializable(CATALOG_ITEM); //NOTE!!!! This is passed to this fragment by reference.
                                    //Read more here: https://stackoverflow.com/questions/44698863/bundle-putserializable-serializing-reference-not-value
            gsNewTagIDs = gciCatalogItem.sTags;
            gsPreviousTagIDs = gsNewTagIDs;
            giNewGrade = gciCatalogItem.iGrade;
            giPreviousGrade = giNewGrade;
        } else {
            gciCatalogItem = new ItemClass_CatalogItem(); //todo: This fragment serves no purpose if the catalog item is not received.
        }

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        //Set on-click listener for grade:
        giGradeImageViews = new int[]{
                R.id.imageView_Grade1,
                R.id.imageView_Grade2,
                R.id.imageView_Grade3,
                R.id.imageView_Grade4,
                R.id.imageView_Grade5};
        ImageView[] imageView_GradeArray = new ImageView[5];
        boolean bGradeIVsOK = true;
        for(int i = 0; i < giGradeImageViews.length; i++){
            imageView_GradeArray[i] = getView().findViewById(giGradeImageViews[i]);
            if(imageView_GradeArray[i] == null){
                bGradeIVsOK = false;
            }
        }
        if (bGradeIVsOK){
            for(int i = 0; i < giGradeImageViews.length; i++) {
                imageView_GradeArray[i].setOnClickListener(new gradeOnClickListener(i + 1));
            }
        }

        displayGrade();

        TextView textView_FileName = getView().findViewById(R.id.textView_FileName);
        if(textView_FileName != null){
            String sFilename = "File name: " + GlobalClass.JumbleFileName(gciCatalogItem.sFilename);
            textView_FileName.setText(sFilename);
        }

        TextView textView_Tags = getView().findViewById(R.id.textView_Tags);
        if(textView_Tags != null){
            String sTagText = "Tags: ";
            sTagText += sTagText = globalClass.getTagTextsFromTagIDsString(gciCatalogItem.sTags, gciCatalogItem.iMediaCategory);
            textView_Tags.setText(sTagText);
        }

        //Get tags for the item:
        ArrayList<Integer> aliTags = GlobalClass.getIntegerArrayFromString(gciCatalogItem.sTags, ",");

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        gViewModel_fragment_selectTags = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_SelectTags.class);

        gViewModel_fragment_selectTags.altiTagsSelected.removeObservers(getViewLifecycleOwner());

        ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();
        for(int i = 0; i < aliTags.size(); i++){
            alTagItems.add(i, new ItemClass_Tag(aliTags.get(i), globalClass.getTagTextFromID(aliTags.get(i), gciCatalogItem.iMediaCategory)));
        }
        gViewModel_fragment_selectTags.altiTagsSelected.setValue(alTagItems);

        //Populate the tags fragment:
        //Start the tag selection fragment:
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment_SelectTags fragment_selectTags = new Fragment_SelectTags();
        Bundle fragment_selectTags_args = new Bundle();
        fragment_selectTags_args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, gciCatalogItem.iMediaCategory);
        fragment_selectTags_args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, aliTags);
        fragment_selectTags.setArguments(fragment_selectTags_args);
        fragmentTransaction.replace(R.id.child_fragment_tag_selector, fragment_selectTags);
        fragmentTransaction.commit();



        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<ItemClass_Tag>> selectedTagsObserver = new Observer<ArrayList<ItemClass_Tag>>() {
            @Override
            public void onChanged(ArrayList<ItemClass_Tag> tagItems) {

                //Get the text of the tags and display:
                StringBuilder sb = new StringBuilder();
                sb.append("Tags: ");
                if(tagItems.size() > 0) {
                    sb.append(tagItems.get(0).sTagText);
                    for (int i = 1; i < tagItems.size(); i++) {
                        sb.append(", ");
                        sb.append(tagItems.get(i).sTagText);
                    }
                }
                TextView textView_Tags = getView().findViewById(R.id.textView_Tags);
                if(textView_Tags != null){
                    textView_Tags.setText(sb.toString());
                }

                //Get the tag IDs:
                ArrayList<Integer> aliTagIDs = new ArrayList<>();
                for(ItemClass_Tag ti : tagItems){
                    aliTagIDs.add(ti.iTagID);
                }

                gsNewTagIDs = GlobalClass.formDelimitedString(aliTagIDs,",");
                if(!gsNewTagIDs.equals(gsPreviousTagIDs)) {
                    //Enable the Save button:
                    enableSave();
                }
            }
        };

        gViewModel_fragment_selectTags.altiTagsSelected.observe(getViewLifecycleOwner(), selectedTagsObserver);

        //Configure the SAVE button listener:
        final Button button_Save = getView().findViewById(R.id.button_Save);
        if(button_Save != null){
            button_Save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gciCatalogItem.sTags = gsNewTagIDs;
                    gciCatalogItem.iGrade = giNewGrade;
                    gsPreviousTagIDs = gsNewTagIDs;
                    giPreviousGrade = giNewGrade;
                        //Because the item assigned to gciCatalogItem was passed-in by reference,
                        //  this changes the CatalogItem set in the calling activity, too.
                    globalClass.CatalogDataFile_UpdateRecord(gciCatalogItem);
                        //Write the modifications to file, and update official memory.
                    button_Save.setEnabled(false);
                    Toast.makeText(getContext(), "Data saved.", Toast.LENGTH_SHORT).show();

                    ((Activity_VideoPlayer) getActivity()).closeDrawer();


                }
            });
        }


    }


    private void displayGrade(){
        //Show the rating:
        ImageView[] imageView_GradeArray = new ImageView[5];
        boolean bGradeIVsOK = true;
        for(int i = 0; i < giGradeImageViews.length; i++){
            imageView_GradeArray[i] = getView().findViewById(giGradeImageViews[i]);
            if(imageView_GradeArray[i] == null){
                bGradeIVsOK = false;
            }
        }
        if (bGradeIVsOK){
            Drawable drawable_SolidStar = ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_grade_white_18dp, null);
            Drawable drawable_EmptyStar = ResourcesCompat.getDrawable(getResources(), R.drawable.outline_grade_white_18dp, null);
            for(int i = 0; i < giNewGrade; i++) {
                imageView_GradeArray[i].setImageDrawable(drawable_SolidStar);
            }
            for(int i = giNewGrade; i < giGradeImageViews.length; i++) {
                imageView_GradeArray[i].setImageDrawable(drawable_EmptyStar);
            }
        }

    }

    private void enableSave(){
        Button button_Save = getView().findViewById(R.id.button_Save);
        if(button_Save != null){
            button_Save.setEnabled(true);
        }
    }

    private class gradeOnClickListener implements View.OnClickListener{

        int iGrade;

        public gradeOnClickListener(int iGrade){
            this.iGrade = iGrade;
        }

        @Override
        public void onClick(View view) {
            giNewGrade = iGrade;
            displayGrade();
            enableSave();
        }
    }


}