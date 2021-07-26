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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import java.util.Stack;


public class Activity_TagEditor extends AppCompatActivity {

    public ViewPager2 ViewPager2_TagEditor;
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

    public static final String NEW_TAGS = "NEW_TAGS";  //Send back any new tags to the calling activity.
    public static final String EXTRA_BUNDLE_TAG_EDITOR_NEW_TAGS_RESULT = "BUNDLE_TAG_EDITOR_NEW_TAGS_RESULT";
    public static final String EXTRA_BOOL_TAG_RENAMED = "EXTRA_BOOL_TAG_RENAMED";
    public static final String EXTRA_BOOL_TAG_DELETED = "EXTRA_BOOL_TAG_DELETED";

    TagEditorServiceResponseReceiver tagEditorServiceResponseReceiver;

    static Stack<Integer> stackFragmentOrder;
    private static int giStartingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_editor);
        setTitle("Tag Editor");

        ViewPager2_TagEditor = findViewById(R.id.viewPager_TagEditor);
        // set Orientation in your ViewPager2
        ViewPager2_TagEditor.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        //Set adapter for ViewPager2:
        ViewPagerAdapter_TagEditor viewPagerAdapter_tagEditor = new ViewPagerAdapter_TagEditor(getSupportFragmentManager(), getLifecycle());
        ViewPager2_TagEditor.setAdapter(viewPagerAdapter_tagEditor);
        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        ViewPager2_TagEditor.setUserInputEnabled(false);




        //Instantiate the ViewModel sharing data between fragments:
        viewModelTagEditor = new ViewModelProvider(this).get(ViewModel_TagEditor.class);

        stackFragmentOrder = new Stack<>();
        giStartingFragment = FRAGMENT_TAG_EDITOR_0_ID_MEDIA_CATEGORY;

        //Check to see if this activity has been started by an activity desiring mods to a
        //  particular media category set of tags:
        Intent iStartingIntent = getIntent();
        if(iStartingIntent != null){
            int iMediaCategory = iStartingIntent.getIntExtra(EXTRA_INT_MEDIA_CATEGORY, -1);
            if(iMediaCategory != -1){
                viewModelTagEditor.iTagEditorMediaCategory = iMediaCategory;
                //Go to the import folder selection fragment:
                ViewPager2_TagEditor.setCurrentItem(FRAGMENT_TAG_EDITOR_1_ID_ACTION, false);
                giStartingFragment = FRAGMENT_TAG_EDITOR_1_ID_ACTION;
            }
        }

        stackFragmentOrder.push(giStartingFragment);

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(TagEditorServiceResponseReceiver.TAG_EDITOR_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        tagEditorServiceResponseReceiver = new TagEditorServiceResponseReceiver();
        //registerReceiver(tagEditorServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(tagEditorServiceResponseReceiver,filter);

    }

    public void callForFinish(){

        Intent data = new Intent();

        if(viewModelTagEditor.bTagAdded) {
            //Send any new tags back to the calling activity so that
            // the calling activity is aware of the new tags created by the user.
            // The calling activity may want to automatically select these
            // new tags.
            Bundle b = new Bundle();
            b.putSerializable(Activity_TagEditor.NEW_TAGS, viewModelTagEditor.alNewTags);
            data.putExtra(Activity_TagEditor.EXTRA_BUNDLE_TAG_EDITOR_NEW_TAGS_RESULT, b);
        }
        if(viewModelTagEditor.bTagRenamed) {
            //Send back data to the caller that a tag has been renamed.
            //  If the user reached this stage while viewing a catalog item, the cat
            //  item may need to have tags reloaded.
            data.putExtra(Activity_TagEditor.EXTRA_BOOL_TAG_RENAMED, true);
        }
        if(viewModelTagEditor.bTagDeleted) {
            //Send back data to the caller that a tag has been deleted.
            //  If the user reached this stage while viewing a catalog item, the cat
            //  item may need to have its file reloaded.
            data.putExtra(Activity_TagEditor.EXTRA_BOOL_TAG_DELETED, true);
        }
        setResult(Activity.RESULT_OK, data);

        finish();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(tagEditorServiceResponseReceiver);
        super.onDestroy();
    }

    //======================================================
    //======  FRAGMENT NAVIGATION ROUTINES  ================
    //======================================================


    @Override
    public void onBackPressed() {

        if(stackFragmentOrder.empty()){
            finish();
        } else {
            //Go back through the fragments in the order by which we progressed.
            //  Some user selections will cause a fragment to be skipped, and we don't want
            //  to go back to those skipped fragments, hence the use of a Stack, and pop().
            int iCurrentFragment = ViewPager2_TagEditor.getCurrentItem();
            int iPrevFragment = stackFragmentOrder.pop();
            if((iCurrentFragment == iPrevFragment) && (iCurrentFragment == giStartingFragment)){
                finish();
                return;
            }
            if(iCurrentFragment == iPrevFragment){
                //To handle interesting behavior about how the stack is built.
                iPrevFragment = stackFragmentOrder.peek();
            }
            ViewPager2_TagEditor.setCurrentItem(iPrevFragment, false);

            if(iPrevFragment == giStartingFragment){
                //Go home:
                stackFragmentOrder.push(giStartingFragment);
            }
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
        ViewPager2_TagEditor.setCurrentItem(FRAGMENT_TAG_EDITOR_1_ID_ACTION, false);
        stackFragmentOrder.push(ViewPager2_TagEditor.getCurrentItem());
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

        stackFragmentOrder.push(ViewPager2_TagEditor.getCurrentItem());
    }

    public void buttonClick_Cancel(View v){
        finish();
    }


    public class TagEditorServiceResponseReceiver extends BroadcastReceiver {
        public static final String TAG_EDITOR_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_TAG_EDITOR_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_TagEditor.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            }

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
                //case FRAGMENT_TAG_EDITOR_0_ID_MEDIA_CATEGORY:
                //    return new Fragment_TagEditor_0_MediaCategory();
                case FRAGMENT_TAG_EDITOR_1_ID_ACTION:
                    return new Fragment_TagEditor_1_Action();
                case FRAGMENT_TAG_EDITOR_2_ID_ADD_TAG:
                    return new Fragment_TagEditor_2_AddTag();
                case FRAGMENT_TAG_EDITOR_3_ID_EDIT_TAG:
                    return new Fragment_TagEditor_3_EditTag();
                case FRAGMENT_TAG_EDITOR_4_ID_DELETE_TAG:
                    return new Fragment_TagEditor_4_DeleteTag();
                default:
                    return new Fragment_TagEditor_0_MediaCategory();
            }
        }

        @Override
        public int getItemCount() {
            return FRAGMENT_COUNT;
        }

    }

}