package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

public class TagEditorActivity extends AppCompatActivity {
    private GlobalClass globalClass;

    public ViewPager2 ViewPager2_TagEditor;
    private FragmentTagEditorViewPagerAdapter fragmentTagEditorViewPagerAdapter;
    private TagEditorViewModel tagEditorViewModel;

    //Fragment page indexes:
    public static final int FRAGMENT_TAG_EDITOR_0_ID_MEDIA_CATEGORY = 0;
    public static final int FRAGMENT_TAG_EDITOR_1_ID_ACTION = 1; //Add/Delete tags
    public static final int FRAGMENT_TAG_EDITOR_2_ID_EDIT_TAG = 2; //Edit existing tag
    public static final int FRAGMENT_TAG_EDITOR_3_ID_MERGE_TAG = 3; //Merge tag
    public static final int FRAGMENT_TAG_EDITOR_4_ID_CONFIRM = 4; //Confirmation
    public static final int FRAGMENT_COUNT = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_editor);
        setTitle("Tag Editor");

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        ViewPager2_TagEditor = findViewById(R.id.tag_editor_activity);

        //Instantiate the ViewModel sharing data between fragments:
        tagEditorViewModel = new ViewModelProvider(this).get(TagEditorViewModel.class);

    }



    //================================================
    //  Adapters
    //================================================


    public static class FragmentTagEditorViewPagerAdapter extends FragmentStateAdapter {

        public FragmentTagEditorViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case FRAGMENT_TAG_EDITOR_0_ID_MEDIA_CATEGORY:
                    return new ImportFragment_0_MediaCategory();
                case FRAGMENT_TAG_EDITOR_1_ID_ACTION:
                    return new ImportFragment_1_StorageLocation();
                case FRAGMENT_TAG_EDITOR_2_ID_EDIT_TAG:
                    return new ImportFragment_2_SelectItems();
                case FRAGMENT_TAG_EDITOR_3_ID_MERGE_TAG:
                    return new ImportFragment_3_SelectTags();
                case FRAGMENT_TAG_EDITOR_4_ID_CONFIRM:
                    return new ImportFragment_4_ImportMethod();
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return FRAGMENT_COUNT;
        }

    }

}