package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class Activity_ImportImageComicPreview extends AppCompatActivity {




    private ItemClass_File[] gFileItems;

    private int giFileItemIndex;
    private int giMaxFileItemIndex;
    private static final String IMAGE_PREVIEW_INDEX = "image_preview_index";

    private ImageView gImagePreview;

    private int giMediaCategory;

    private Fragment_SelectTags fragment_selectTags;

    @SuppressLint("ClickableViewAccessibility") //For the onTouch for the imageView.
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(this.getSupportActionBar() != null) {
            this.getSupportActionBar().hide();
        }

        //Source: https://www.youtube.com/watch?v=fn5OlqQuOCk
        setContentView(R.layout.activity_import_image_comic_preview);

        if (savedInstanceState != null) {
            giFileItemIndex = savedInstanceState.getInt(IMAGE_PREVIEW_INDEX);
        }

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        ViewModel_Fragment_SelectTags mViewModel = new ViewModelProvider(this).get(ViewModel_Fragment_SelectTags.class);
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
                TextView tv = findViewById(R.id.textView_ImagePopupSelectedTags);
                if(tv != null){
                    tv.setText(sb.toString());
                }

                //Get the tag IDs to pass back to the calling activity:
                ArrayList<Integer> aliTagIDs = new ArrayList<>();
                for(ItemClass_Tag ti : tagItems){
                    aliTagIDs.add(ti.iTagID);
                }

                //If the media type is Comics, tags are applied to each
                //  file item.
                if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                    for(ItemClass_File icf: gFileItems){
                        icf.aliProspectiveTags = aliTagIDs;
                        icf.bPreviewTagUpdate = true;
                    }

                } else {
                    gFileItems[giFileItemIndex].aliProspectiveTags = aliTagIDs;
                    gFileItems[giFileItemIndex].bPreviewTagUpdate = true;
                }
                //Prepare a result to send back to the calling activity:
                Intent data = new Intent();
                Bundle b = new Bundle();
                //Put back the file URI string so that the file can be located:
                b.putSerializable(Activity_Import.PREVIEW_FILE_ITEMS, gFileItems);
                //b.putIntegerArrayList(Activity_Import.TAG_SELECTION_TAG_IDS, aliTagIDs);
                data.putExtra(Activity_Import.TAG_SELECTION_RESULT_BUNDLE, b);
                setResult(RESULT_OK, data);

            }
        };
        mViewModel.altiTagsSelected.observe(this, selectedTagsObserver);

        Bundle b = getIntent().getExtras();
        if(b != null) {

            gFileItems = (ItemClass_File[]) b.getSerializable(Activity_Import.PREVIEW_FILE_ITEMS);
            giMaxFileItemIndex = gFileItems.length - 1;
            giFileItemIndex = b.getInt(Activity_Import.PREVIEW_FILE_ITEMS_POSITION, 0);
            giMediaCategory = b.getInt(Activity_Import.MEDIA_CATEGORY, 1); //Default 1 because videos has its own preview activity.

            HashMap<String , ItemClass_Tag> hashMapTemp = (HashMap<String , ItemClass_Tag>) b.getSerializable(Activity_Import.IMPORT_SESSION_TAGS_IN_USE);
            TreeMap<String, ItemClass_Tag> tmImportSessionTagsInUse = null;
            if(hashMapTemp != null){
                tmImportSessionTagsInUse = new TreeMap<>(hashMapTemp);
            }

            //Start the tag selection fragment:
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            fragment_selectTags = new Fragment_SelectTags();
            Bundle args = new Bundle();
            args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, giMediaCategory);
            args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, gFileItems[giFileItemIndex].aliProspectiveTags);
            if(tmImportSessionTagsInUse != null){
                if(tmImportSessionTagsInUse.size() > 0){
                    //During import preview of other items, the user may have selected tags that
                    // have not yet been used in the catalog. These new items will not show up in
                    //  Fragment_SelectTags IN-USE tag tab, as that function only queries what is
                    //  already in the catalog. Send the list of tags that have been selected by the
                    //  user for other selected items to the tags fragment:
                    args.putSerializable(Fragment_SelectTags.IMPORT_SESSION_TAGS_IN_USE, tmImportSessionTagsInUse);
                }
            }
            fragment_selectTags.setArguments(args);
            ft.replace(R.id.child_fragment_tag_selector, fragment_selectTags);
            ft.commit();

        }


        //Prepare the Gif image viewer to accept swipe to go to next or previous file:
        //Set a touch listener to the ImageView so that the user can swipe to go to the next image:
        final GestureDetector gdImageView = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                /*ImageButton ImageButton_ObfuscationImage = findViewById(R.id.ImageButton_ObfuscationImage);
                ImageButton_ObfuscationImage.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Double tap detected. Obfuscating...", Toast.LENGTH_SHORT).show();*/
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return super.onSingleTapConfirmed(e);
            }

            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight();
                            } else {
                                onSwipeLeft();
                            }
                            result = true;
                        }
                    }
                    else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        /*if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }*/
                        result = true;
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }

            public void onSwipeRight() {
                int iTempKey = giFileItemIndex - 1;

                iTempKey = Math.max(0, iTempKey);
                if(iTempKey != giFileItemIndex) {
                    giFileItemIndex = iTempKey;
                    initializeImage();
                }
            }

            public void onSwipeLeft() {
                int iTempKey = giFileItemIndex + 1;

                iTempKey = Math.min(giMaxFileItemIndex, iTempKey);
                if(iTempKey != giFileItemIndex) {
                    giFileItemIndex = iTempKey;
                    initializeImage();
                }
            }

            /*public void onSwipeTop() {
            }*/

            /*public void onSwipeBottom() {
            }*/

        });

        gImagePreview = findViewById(R.id.imageView_ImagePreview);
        gImagePreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gdImageView.onTouchEvent(event);
            }

        });

        initializeImage();

    }





    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(IMAGE_PREVIEW_INDEX, giFileItemIndex);
    }

    private void initializeImage(){
        Glide.with(getApplicationContext()).load(gFileItems[giFileItemIndex].sUri).into(gImagePreview);

        TextView textView_FileName = findViewById(R.id.textView_FileName);
        textView_FileName.setText(gFileItems[giFileItemIndex].sFileOrFolderName);

        //Init the tags list if there are tags already assigned to this item:
        //Get the text of the tags and display:
        if(gFileItems[giFileItemIndex].aliProspectiveTags != null) {
            TextView textView_ImagePopupSelectedTags = findViewById(R.id.textView_ImagePopupSelectedTags);
            StringBuilder sbTags = new StringBuilder();
            sbTags.append("Tags: ");

            int iFileItemTagsIndex = 0; //If the media type is Comics, tags are applied to the first
                                        //  file item only.
            if(giMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS){
                iFileItemTagsIndex = giFileItemIndex;
            }

            if (gFileItems[iFileItemTagsIndex].aliProspectiveTags.size() > 0) {

                GlobalClass globalClass;
                globalClass = (GlobalClass) getApplicationContext();

                sbTags.append(globalClass.getTagTextFromID(gFileItems[iFileItemTagsIndex].aliProspectiveTags.get(0), giMediaCategory));
                for (int i = 1; i < gFileItems[iFileItemTagsIndex].aliProspectiveTags.size(); i++) {
                    sbTags.append(", ");
                    sbTags.append(globalClass.getTagTextFromID(gFileItems[iFileItemTagsIndex].aliProspectiveTags.get(i), giMediaCategory));
                }

            }
            if (textView_ImagePopupSelectedTags != null) {
                textView_ImagePopupSelectedTags.setText(sbTags.toString());
            }
            if(giMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS) { //Don't worry about resetting if it's a comic. Tags are same for every page.
                fragment_selectTags.resetTagListViewData(gFileItems[iFileItemTagsIndex].aliProspectiveTags);
            }
        }




    }




}
