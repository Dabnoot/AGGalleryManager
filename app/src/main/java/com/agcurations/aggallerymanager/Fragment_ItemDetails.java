package com.agcurations.aggallerymanager;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_ItemDetails#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_ItemDetails extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public final static String CATALOG_ITEM = "CATALOG_ITEM";

    // TODO: Rename and change types of parameters
    private ItemClass_CatalogItem gciCatalogItem;

    GlobalClass globalClass;

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
            gciCatalogItem = (ItemClass_CatalogItem) args.getSerializable(CATALOG_ITEM);
        } else {
            gciCatalogItem = new ItemClass_CatalogItem(); //todo: This fragment serves no purpose if the catalog item is not received.
        }

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        //Show the rating:
        ImageView imageView_Grade1 = getView().findViewById(R.id.imageView_Grade1);
        ImageView imageView_Grade2 = getView().findViewById(R.id.imageView_Grade2);
        ImageView imageView_Grade3 = getView().findViewById(R.id.imageView_Grade3);
        ImageView imageView_Grade4 = getView().findViewById(R.id.imageView_Grade4);
        ImageView imageView_Grade5 = getView().findViewById(R.id.imageView_Grade5);
        if(imageView_Grade1 != null &&
                imageView_Grade2 != null &&
                imageView_Grade3 != null &&
                imageView_Grade4 != null &&
                imageView_Grade5 != null){

            ImageView[] GradeArray = {imageView_Grade1, imageView_Grade2, imageView_Grade3, imageView_Grade4, imageView_Grade5};
            Drawable drawable_SolidStar = ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_grade_white_18dp, null);
            for(int i = 0; i < gciCatalogItem.iRating && i < 5; i++){
                //Glide.with(getContext()).load(R.drawable.baseline_grade_white_18dp).into(GradeArray[i]);
                GradeArray[i].setImageDrawable(drawable_SolidStar);
            }
        }


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


        //Populate the tags fragment:
        ArrayList<Integer> aliTags = GlobalClass.getIntegerArrayFromString(gciCatalogItem.sTags, ",");
        //Start the tag selection fragment:
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment_SelectTags fragment_selectTags = new Fragment_SelectTags();
        Bundle fragment_selectTags_args = new Bundle();
        fragment_selectTags_args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, gciCatalogItem.iMediaCategory);
        fragment_selectTags_args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, aliTags);
        fragment_selectTags.setArguments(fragment_selectTags_args);
        fragmentTransaction.replace(R.id.child_fragment_tag_selector, fragment_selectTags);
        fragmentTransaction.commit();


    }
}