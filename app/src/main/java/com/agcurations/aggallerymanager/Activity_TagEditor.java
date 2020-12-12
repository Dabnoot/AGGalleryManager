package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.RadioButton;

public class Activity_TagEditor extends AppCompatActivity {
    private GlobalClass globalClass;

    public ViewPager2 ViewPager2_TagEditor;
    private ViewPagerAdapter_TagEditor viewPagerAdapter_tagEditor;
    private ViewModel_TagEditor viewModelTagEditor;

    //Fragment page indexes:
    public static final int FRAGMENT_TAG_EDITOR_0_ID_MEDIA_CATEGORY = 0;
    public static final int FRAGMENT_TAG_EDITOR_1_ID_ACTION = 1; //Choose action to perform on tags
    public static final int FRAGMENT_TAG_EDITOR_2_ID_ADD_TAG = 2; //Add a new tag
    public static final int FRAGMENT_TAG_EDITOR_3_ID_EDIT_DELETE_TAG = 3; //Edit or delete a tag
    public static final int FRAGMENT_TAG_EDITOR_4_ID_MERGE_TAGS = 4; //Merge tags
    public static final int FRAGMENT_TAG_EDITOR_5_ID_CONFIRM = 5; //Confirmation (for delete and merge operations)
    public static final int FRAGMENT_TAG_EDITOR_5_ID_EXECUTE = 6; //Execute (for delete and merge operations)
    public static final int FRAGMENT_COUNT = 3;

    public static final String EXTRA_INT_MEDIA_CATEGORY = "EXTRA_INT_MEDIA_CATEGORY";
                                 //If the tag editor is being started from somewhere other than
                                 // Main activity, it must be in an area applicable to a particular media type.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_editor);
        setTitle("Tag Editor");

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        ViewPager2_TagEditor = findViewById(R.id.viewPager_TagEditor);
        // set Orientation in your ViewPager2
        ViewPager2_TagEditor.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        //Set adapter for ViewPager2:
        viewPagerAdapter_tagEditor = new ViewPagerAdapter_TagEditor(getSupportFragmentManager(), getLifecycle());
        ViewPager2_TagEditor.setAdapter(viewPagerAdapter_tagEditor);
        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        ViewPager2_TagEditor.setUserInputEnabled(false);




        //Instantiate the ViewModel sharing data between fragments:
        viewModelTagEditor = new ViewModelProvider(this).get(ViewModel_TagEditor.class);

        //Check to see if this activity has been started by an activity desiring mods to a
        //  particular media category set of tags:
        Intent iStartingIntent = getIntent();
        if(iStartingIntent != null){
            int iMediaCategory = iStartingIntent.getIntExtra(EXTRA_INT_MEDIA_CATEGORY, -1);
            if(iMediaCategory != -1){
                viewModelTagEditor.iTagEditorMediaCategory = iMediaCategory;
                //Go to the import folder selection fragment:
                ViewPager2_TagEditor.setCurrentItem(FRAGMENT_TAG_EDITOR_1_ID_ACTION);
            }
        }

    }

    //======================================================
    //======  FRAGMENT NAVIGATION ROUTINES  ================
    //======================================================


    @Override
    public void onBackPressed() {

        if (ViewPager2_TagEditor.getCurrentItem() != 0) {
            ViewPager2_TagEditor.setCurrentItem(ViewPager2_TagEditor.getCurrentItem() - 1,false);
        }else{
            finish();
        }

    }

    public void buttonNextClick_MediaCategorySelected(View v){
        RadioButton rbVideos = findViewById(R.id.radioButton_VideoTags);
        RadioButton rbImages = findViewById(R.id.radioButton_ImageTags);
        //RadioButton rbComics = findViewById(R.id.radioButton_ComicTags);

        if (rbVideos.isChecked()){
            viewModelTagEditor.iTagEditorMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        } else if (rbImages.isChecked()){
            viewModelTagEditor.iTagEditorMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        } else {
            viewModelTagEditor.iTagEditorMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        }

        //Go to the import folder selection fragment:
        ViewPager2_TagEditor.setCurrentItem(FRAGMENT_TAG_EDITOR_1_ID_ACTION);
    }

    public void buttonNextClick_TagActionSelected(View v){
        RadioButton rbAddTags = findViewById(R.id.radioButton_AddTags);
        RadioButton rbEditDeleteTags = findViewById(R.id.radioButton_EditDeleteTags);
        //RadioButton rbMergeTags = findViewById(R.id.radioButton_MergeTags);

        if (rbAddTags.isChecked()){
            ViewPager2_TagEditor.setCurrentItem(FRAGMENT_TAG_EDITOR_2_ID_ADD_TAG);
        } else if (rbEditDeleteTags.isChecked()){
            ViewPager2_TagEditor.setCurrentItem(FRAGMENT_TAG_EDITOR_3_ID_EDIT_DELETE_TAG);
        } else {
            ViewPager2_TagEditor.setCurrentItem(FRAGMENT_TAG_EDITOR_4_ID_MERGE_TAGS);
        }
    }



    //================================================
    //  Adapters
    //================================================


    public static class ViewPagerAdapter_TagEditor extends FragmentStateAdapter {

        public ViewPagerAdapter_TagEditor(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case FRAGMENT_TAG_EDITOR_0_ID_MEDIA_CATEGORY:
                    return new Fragment_TagEditor_0_MediaCategory();
                case FRAGMENT_TAG_EDITOR_1_ID_ACTION:
                    return new Fragment_TagEditor_1_Action();
                case FRAGMENT_TAG_EDITOR_2_ID_ADD_TAG:
                    return new Fragment_TagEditor_2_AddTag();
                case FRAGMENT_TAG_EDITOR_3_ID_EDIT_DELETE_TAG:
                    return null;
                case FRAGMENT_TAG_EDITOR_4_ID_MERGE_TAGS:
                    return null;
                case FRAGMENT_TAG_EDITOR_5_ID_CONFIRM:
                    return null;
                case FRAGMENT_TAG_EDITOR_5_ID_EXECUTE:
                    return null;
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return FRAGMENT_COUNT;
        }

    }

}