package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;


public class Activity_TagEditor extends AppCompatActivity {
    private GlobalClass globalClass;

    public ViewPager2 ViewPager2_TagEditor;
    private ViewPagerAdapter_TagEditor viewPagerAdapter_tagEditor;
    private ViewModel_TagEditor viewModelTagEditor;

    //Fragment page indexes:
    public static final int FRAGMENT_TAG_EDITOR_0_ID_MEDIA_CATEGORY = 0;
    public static final int FRAGMENT_TAG_EDITOR_1_ID_ACTION = 1; //Choose action to perform on tags
    public static final int FRAGMENT_TAG_EDITOR_2_ID_ADD_TAG = 2; //Add a new tag
    public static final int FRAGMENT_TAG_EDITOR_3_ID_EDIT_TAG = 3; //Edit a tag
    public static final int FRAGMENT_TAG_EDITOR_4_ID_DELETE_TAG = 4; //Delete a tag

    public static final int FRAGMENT_COUNT = 5;

    public static final String EXTRA_INT_MEDIA_CATEGORY = "EXTRA_INT_MEDIA_CATEGORY";
                                 //If the tag editor is being started from somewhere other than
                                 // Main activity, it must be in an area applicable to a particular media type.

//    TagEditorServiceResponseReceiver tagEditorServiceResponseReceiver;

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


        /*//Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(TagEditorServiceResponseReceiver.TAG_EDITOR_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        tagEditorServiceResponseReceiver = new TagEditorServiceResponseReceiver();
        //registerReceiver(tagEditorServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(tagEditorServiceResponseReceiver,filter);*/

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

    @Override
    protected void onDestroy() {
        //unregisterReceiver(tagEditorServiceResponseReceiver);
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(tagEditorServiceResponseReceiver);
        super.onDestroy();
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
        RadioButton rbEditTags = findViewById(R.id.radioButton_EditTags);
        //RadioButton rbMergeTags = findViewById(R.id.radioButton_MergeTags);

        if (rbAddTags.isChecked()){
            ViewPager2_TagEditor.setCurrentItem(FRAGMENT_TAG_EDITOR_2_ID_ADD_TAG, false);
        } else if (rbEditTags.isChecked()){
            ViewPager2_TagEditor.setCurrentItem(FRAGMENT_TAG_EDITOR_3_ID_EDIT_TAG, false);
        } else {
            ViewPager2_TagEditor.setCurrentItem(FRAGMENT_TAG_EDITOR_4_ID_DELETE_TAG, false);
        }
    }


    /*public class TagEditorServiceResponseReceiver extends BroadcastReceiver {
        public static final String TAG_EDITOR_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_TAG_EDITOR_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_TagEditor.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {
                //Perform actions
                int i = 0;

            }

        }
    }*/

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
                case FRAGMENT_TAG_EDITOR_3_ID_EDIT_TAG:
                    return new Fragment_TagEditor_3_EditTag();
                case FRAGMENT_TAG_EDITOR_4_ID_DELETE_TAG:
                    return new Fragment_TagEditor_4_DeleteTag();

            }
            return null;
        }

        @Override
        public int getItemCount() {
            return FRAGMENT_COUNT;
        }

    }

}