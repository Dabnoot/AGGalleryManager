package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_ItemDetails#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_ItemDetails extends Fragment {

    public final static String CATALOG_ITEM = "CATALOG_ITEM";
    public final static String HISTOGRAM_FREEZE = "HISTOGRAM_FREEZE";

    private ItemClass_CatalogItem gciCatalogItem;

    GlobalClass globalClass;

    /*int[] giGradeImageViews;*/

    String gsNewTagIDs;
    String gsPreviousTagIDs;
    /*int giNewGrade;
    int giPreviousGrade;*/

    ViewModel_Fragment_SelectTags gViewModel_fragment_selectTags;

    public Fragment_SelectTags gFragment_selectTags;

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

        boolean bHistogramFreeze = false;

        if(args != null) {
            gciCatalogItem = (ItemClass_CatalogItem) args.getSerializable(CATALOG_ITEM); //NOTE!!!! This is passed to this fragment by reference.
                                    //Read more here: https://stackoverflow.com/questions/44698863/bundle-putserializable-serializing-reference-not-value
            bHistogramFreeze = args.getBoolean(HISTOGRAM_FREEZE);
            if(gciCatalogItem == null){
                return;
            }
            gsNewTagIDs = gciCatalogItem.sTags;
            gsPreviousTagIDs = gsNewTagIDs;
            /*giNewGrade = gciCatalogItem.iGrade;
            giPreviousGrade = giNewGrade;*/
        } else {
            gciCatalogItem = new ItemClass_CatalogItem(); //todo: This fragment serves no purpose if the catalog item is not received.
        }
        if(getActivity() == null){
            return;
        }
        globalClass = (GlobalClass) getActivity().getApplicationContext();

        /*//Set on-click listener for grade:
        giGradeImageViews = new int[]{
                R.id.imageView_Grade1,
                R.id.imageView_Grade2,
                R.id.imageView_Grade3,
                R.id.imageView_Grade4,
                R.id.imageView_Grade5};
        ImageView[] imageView_GradeArray = new ImageView[5];
        boolean bGradeIVsOK = true;
        if(getView() == null){
            return;
        }
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
        }*/

        //displayGrade();

        UpdateTextViews();

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
        gFragment_selectTags = new Fragment_SelectTags();
        Bundle fragment_selectTags_args = new Bundle();
        fragment_selectTags_args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, gciCatalogItem.iMediaCategory);
        fragment_selectTags_args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, aliTags);
        gFragment_selectTags.setArguments(fragment_selectTags_args);
        fragmentTransaction.replace(R.id.child_fragment_tag_selector, gFragment_selectTags);
        fragmentTransaction.commit();

        if(bHistogramFreeze){
            gFragment_selectTags.gbHistogramFreeze = true;
        }

        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<ItemClass_Tag>> observerSelectedTags = GetNewTagObserver();

        gViewModel_fragment_selectTags.altiTagsSelected.observe(getViewLifecycleOwner(), observerSelectedTags);

        /*//React to if the TagEditor is called and TagEditor requests that we reload tags:
        final Observer<Boolean> observerReloadTags = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean bReloadTags) {
                //Populate the tags fragment:
                //Get tags for the item:
                ArrayList<Integer> aliTags = GlobalClass.getIntegerArrayFromString(gciCatalogItem.sTags, ",");
                ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();
                for(int i = 0; i < aliTags.size(); i++){
                    alTagItems.add(i, new ItemClass_Tag(aliTags.get(i), globalClass.getTagTextFromID(aliTags.get(i), gciCatalogItem.iMediaCategory)));
                }
                gViewModel_fragment_selectTags.altiTagsSelected.setValue(alTagItems);
                FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
                Fragment_SelectTags fragment_selectTags = new Fragment_SelectTags();
                Bundle fragment_selectTags_args = new Bundle();
                fragment_selectTags_args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, gciCatalogItem.iMediaCategory);
                fragment_selectTags_args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, aliTags);
                fragment_selectTags.setArguments(fragment_selectTags_args);
                fragmentTransaction.replace(R.id.child_fragment_tag_selector, fragment_selectTags);
                fragmentTransaction.commit();
            }
        };
        gViewModel_fragment_selectTags.bTagEditorRequestsReloadTags.observe(getViewLifecycleOwner(), observerReloadTags);*/


        //Configure the SAVE button listener:
        final Button button_Save = getView().findViewById(R.id.button_Save);
        if(button_Save != null){
            button_Save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!gciCatalogItem.sTags.equals(gsNewTagIDs)) {
                        gciCatalogItem.sTags = gsNewTagIDs;
                        gciCatalogItem.aliTags = GlobalClass.getTagIDsFromTagIDString(gsNewTagIDs);
                        //Recalc maturity rating and approved users for the item:
                        gciCatalogItem.iMaturityRating = GlobalClass.getHighestTagMaturityRating(gciCatalogItem.aliTags, gciCatalogItem.iMediaCategory);
                        gciCatalogItem.alsApprovedUsers = GlobalClass.getApprovedUsersForTagGrouping(gciCatalogItem.aliTags, gciCatalogItem.iMediaCategory);
                        //Inform program of a need to update the tags histogram:
                        globalClass.gbTagHistogramRequiresUpdate[gciCatalogItem.iMediaCategory] = true;
                    }
                    /*gciCatalogItem.iGrade = giNewGrade;
                    gsPreviousTagIDs = gsNewTagIDs;
                    giPreviousGrade = giNewGrade;*/
                    Toast.makeText(getContext(), "Saving data...", Toast.LENGTH_SHORT).show();
                    button_Save.setEnabled(false);
                        //Because the item assigned to gciCatalogItem was passed-in by reference,
                        //  this changes the CatalogItem set in the calling activity, too.
                    globalClass.CatalogDataFile_UpdateRecord(gciCatalogItem);
                        //Write the modifications to file, and update official memory.
                    Toast.makeText(getContext(), "Data saved.", Toast.LENGTH_SHORT).show();

                    if(getActivity() != null) {
                        ((Activity_VideoPlayer) getActivity()).closeDrawer();
                    }

                }
            });
        }


    }

    private Observer<ArrayList<ItemClass_Tag>> GetNewTagObserver() {
        return new Observer<ArrayList<ItemClass_Tag>>() {
            @Override
            public void onChanged(ArrayList<ItemClass_Tag> tagItems) {

                //Get the text of the tags and display:
                StringBuilder sb = new StringBuilder();
                sb.append("Tags: ");
                if (tagItems.size() > 0) {
                    sb.append(tagItems.get(0).sTagText);
                    for (int i = 1; i < tagItems.size(); i++) {
                        sb.append(", ");
                        sb.append(tagItems.get(i).sTagText);
                    }
                }
                if (getView() != null) {
                    TextView textView_Tags = getView().findViewById(R.id.textView_Tags);
                    if (textView_Tags != null) {
                        textView_Tags.setText(sb.toString());
                    }
                }

                //Get the tag IDs:
                ArrayList<Integer> aliTagIDs = new ArrayList<>();
                for (ItemClass_Tag ti : tagItems) {
                    aliTagIDs.add(ti.iTagID);
                }

                gsNewTagIDs = GlobalClass.formDelimitedString(aliTagIDs, ",");

                //gsNewTagIDs will be sorted. Sort gsPreviousTagIDs so that a proper comparison can
                //  be made.
                String sPreviousTagIDsTemp = SortTagIDString(gsPreviousTagIDs);
                String sNewTagIDsTemp = SortTagIDString(gsNewTagIDs);

                int iMaturityRating = GlobalClass.getHighestTagMaturityRating(aliTagIDs, gciCatalogItem.iMediaCategory);
                updateMaturityText(iMaturityRating);

                ArrayList<String> alsApprovedUsers = GlobalClass.getApprovedUsersForTagGrouping(aliTagIDs, gciCatalogItem.iMediaCategory);
                updateApprovedUsersText(alsApprovedUsers);


                if (!sNewTagIDsTemp.equals(sPreviousTagIDsTemp)) {
                    //Enable the Save button:
                    enableSave();
                }
            }
        };
    }

    private String SortTagIDString(String sTagIDStringInput){
        String sOutputTags = sTagIDStringInput;
        String[] sInputTags = sTagIDStringInput.split(",");
        if(sInputTags.length > 0){
            if(!(sInputTags.length == 1 && sInputTags[0].equals(""))) {
                TreeMap<Integer, Integer> tmSortInputTags = new TreeMap<>();
                for (String sTagID : sInputTags) {
                    Integer iTagID = Integer.parseInt(sTagID);
                    tmSortInputTags.put(iTagID, iTagID);
                }
                StringBuilder sbOutputTags = new StringBuilder();
                for (Map.Entry<Integer, Integer> tmEntry : tmSortInputTags.entrySet()) {
                    sbOutputTags.append(tmEntry.getKey());
                    sbOutputTags.append(",");
                }
                String sTempOutputTags = sbOutputTags.toString();
                if (sTempOutputTags.length() > 0) {
                    sTempOutputTags = sTempOutputTags.substring(0, sTempOutputTags.length() - 1);
                    sOutputTags = sTempOutputTags;
                }
            }
        }
        return sOutputTags;

    }


    public void initData(ItemClass_CatalogItem ci){

        gciCatalogItem = ci;
        gsNewTagIDs = gciCatalogItem.sTags;
        gsPreviousTagIDs = gsNewTagIDs;
        /*giNewGrade = gciCatalogItem.iGrade;
        giPreviousGrade = giNewGrade;
        displayGrade();*/

        UpdateTextViews();

        //Get tags for the item:
        ArrayList<Integer> aliTags = GlobalClass.getIntegerArrayFromString(gciCatalogItem.sTags, ",");

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        if(getActivity() == null){
            return;
        }
        gViewModel_fragment_selectTags = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_SelectTags.class);

        gViewModel_fragment_selectTags.altiTagsSelected.removeObservers(getViewLifecycleOwner());

        ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();
        for(int i = 0; i < aliTags.size(); i++){
            alTagItems.add(i, new ItemClass_Tag(aliTags.get(i), globalClass.getTagTextFromID(aliTags.get(i), gciCatalogItem.iMediaCategory)));
        }
        gViewModel_fragment_selectTags.altiTagsSelected.setValue(alTagItems);

        //Re-populate the tags fragment:
        if(gFragment_selectTags != null) {
            gFragment_selectTags.resetTagListViewData(aliTags);
        }

        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<ItemClass_Tag>> observerSelectedTags = GetNewTagObserver();

        gViewModel_fragment_selectTags.altiTagsSelected.observe(getViewLifecycleOwner(), observerSelectedTags);

        Button button_Save = getView().findViewById(R.id.button_Save);
        button_Save.setEnabled(false); //When init data, no reason to save.

    }

    private void UpdateTextViews(){
        if(getView() == null){
            return;
        }

        TextView textView_Title = getView().findViewById(R.id.textView_Title);
        if(textView_Title != null){
            String sTitle = "Title: " + gciCatalogItem.sTitle;
            textView_Title.setText(sTitle);
        }

        TextView textView_FileName = getView().findViewById(R.id.textView_FileName);
        if(textView_FileName != null){
            String sFilename = "File name: " + gciCatalogItem.sFilename;
            textView_FileName.setText(sFilename);
        }

        TextView textView_StorageLocation = getView().findViewById(R.id.textView_StorageLocation);
        if(textView_StorageLocation != null){
            String sStorageLocation = "Storage Location: " + GlobalClass.gsCatalogFolderNames[gciCatalogItem.iMediaCategory] + File.separator + GlobalClass.cleanHTMLCodedCharacters(gciCatalogItem.sFolderRelativePath);
            textView_StorageLocation.setText(sStorageLocation);
        }

        TextView textView_Dimensions = getView().findViewById(R.id.textView_Dimensions);
        if(textView_Dimensions != null){
            String sDimensions = "Dimensions: " + gciCatalogItem.iWidth + " x " + gciCatalogItem.iHeight;
            textView_Dimensions.setText(sDimensions);
        }

        TextView textView_Source = getView().findViewById(R.id.textView_Source);
        if(textView_Source != null){
            String sSource = "Source: " + gciCatalogItem.sSource;
            textView_Source.setText(sSource);
        }

        updateMaturityText(gciCatalogItem.iMaturityRating);

        updateApprovedUsersText(gciCatalogItem.alsApprovedUsers);


        TextView textView_Tags = getView().findViewById(R.id.textView_Tags);
        if(textView_Tags != null){
            String sTagText = "Tags: ";
            sTagText += globalClass.getTagTextsFromTagIDsString(gciCatalogItem.sTags, gciCatalogItem.iMediaCategory);
            textView_Tags.setText(sTagText);
        }
    }

    private void updateMaturityText(int iMaturityRating){
        if(getView() == null) return;
        TextView textView_MaturityRating = getView().findViewById(R.id.textView_MaturityRating);
        if(textView_MaturityRating != null){
            String sMaturityRatingText = "Maturity Rating: ";

            sMaturityRatingText += AdapterMaturityRatings.MATURITY_RATINGS[iMaturityRating][AdapterMaturityRatings.MATURITY_RATING_CODE_INDEX];
            sMaturityRatingText += " - ";
            String sMatRatDesc = AdapterMaturityRatings.MATURITY_RATINGS[iMaturityRating][AdapterMaturityRatings.MATURITY_RATING_NAME_INDEX];
            boolean bLengthLimit = false;
            if(bLengthLimit) {
                int iMaxTextLength = 75;
                sMaturityRatingText += sMatRatDesc.substring(0, Math.min(iMaxTextLength, sMatRatDesc.length()));
                if (iMaxTextLength < sMatRatDesc.length()) {
                    sMaturityRatingText += "...";
                }
            } else {
                sMaturityRatingText += sMatRatDesc;
            }
            textView_MaturityRating.setText(sMaturityRatingText);
        }
    }

    private void updateApprovedUsersText(ArrayList<String> alsApprovedUsers){
        if(getView() == null) return;
        TextView textView_ApprovedUsers = getView().findViewById(R.id.textView_ApprovedUsers);
        if(textView_ApprovedUsers != null){
            StringBuilder sbApprovedUsersText = new StringBuilder();
            sbApprovedUsersText.append("Approved Users: ");
            if(alsApprovedUsers.size() > 0) {
                for (int i = 0; i < alsApprovedUsers.size(); i++) {
                    sbApprovedUsersText.append(alsApprovedUsers.get(i));
                    if (i < (alsApprovedUsers.size() - 1)) {
                        sbApprovedUsersText.append(", ");
                    }
                }
            } else {
                sbApprovedUsersText.append("[Unspecified]");
            }
            textView_ApprovedUsers.setText(sbApprovedUsersText);
        }
    }


    /*private void displayGrade(){
        if(getView() == null){
            return;
        }
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

    }*/

    private void enableSave(){
        if(getView() == null){
            return;
        }
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
            /*giNewGrade = iGrade;
            displayGrade();*/
            enableSave();
        }
    }


}