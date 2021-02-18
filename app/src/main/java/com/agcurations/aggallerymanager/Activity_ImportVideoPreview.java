package com.agcurations.aggallerymanager;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class Activity_ImportVideoPreview extends AppCompatActivity {

    private ItemClass_File gFileItem;

    VideoView gVideoView_VideoPlayer;
    MediaController gMediaController;

    private int giCurrentVideoPosition = 1;
    private final int VIDEO_PLAYBACK_STATE_PAUSED = 0;
    private final int VIDEO_PLAYBACK_STATE_PLAYING = 1;
    private int giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
    private static final String PLAYBACK_TIME = "play_time";

    int[] giGradeImageViews;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(this.getSupportActionBar() != null) {
            this.getSupportActionBar().hide();
        }

        //Source: https://www.youtube.com/watch?v=fn5OlqQuOCk
        setContentView(R.layout.activity_import_video_preview);

        if (savedInstanceState != null) {
            giCurrentVideoPosition = savedInstanceState.getInt(PLAYBACK_TIME);
        }

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        ViewModel_Fragment_SelectTags viewModel_fragment_selectTags = new ViewModelProvider(this).get(ViewModel_Fragment_SelectTags.class);
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
                TextView tv = findViewById(R.id.textView_VideoPopupSelectedTags);
                if(tv != null){
                    tv.setText(sb.toString());
                }

                //Get the tag IDs to pass back to the calling activity:
                ArrayList<Integer> aliTagIDs = new ArrayList<>();
                for(ItemClass_Tag ti : tagItems){
                    aliTagIDs.add(ti.iTagID);
                }
                gFileItem.aliProspectiveTags = aliTagIDs;
                gFileItem.bPreviewTagUpdate = true;

                //Prepare a result to send back to the calling activity:
                Intent data = new Intent();
                Bundle b = new Bundle();
                //Put back the file URI string so that the file can be located:
                ItemClass_File[] icFileItems = new ItemClass_File[]{gFileItem};
                b.putSerializable(Activity_Import.PREVIEW_FILE_ITEMS, icFileItems);
                //b.putIntegerArrayList(Activity_Import.TAG_SELECTION_TAG_IDS, aliTagIDs);
                data.putExtra(Activity_Import.TAG_SELECTION_RESULT_BUNDLE, b);
                setResult(RESULT_OK, data);

            }
        };
        viewModel_fragment_selectTags.altiTagsSelected.observe(this, selectedTagsObserver);




        Bundle b = getIntent().getExtras();
        if(b != null) {

            ItemClass_File[] icFileItems;
            icFileItems = (ItemClass_File[]) b.getSerializable(Activity_Import.PREVIEW_FILE_ITEMS);
            gFileItem = icFileItems[0]; //Video preview will only have 1 file item.

            HashMap<String , ItemClass_Tag> hashMapTemp = (HashMap<String , ItemClass_Tag>) b.getSerializable(Activity_Import.IMPORT_SESSION_TAGS_IN_USE);
            TreeMap<String, ItemClass_Tag> tmImportSessionTagsInUse = null;
            if(hashMapTemp != null){
                tmImportSessionTagsInUse = new TreeMap<>(hashMapTemp);
            }

            //Start the tag selection fragment:
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment_SelectTags fst = new Fragment_SelectTags();
            Bundle args = new Bundle();
            args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_VIDEOS);
            args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, gFileItem.aliProspectiveTags);
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
            fst.setArguments(args);
            ft.replace(R.id.child_fragment_tag_selector, fst);
            ft.commit();

            //Init the tags list if there are tags already assigned to this item:
            //Get the text of the tags and display:
            if(gFileItem.aliProspectiveTags != null) {
                if (gFileItem.aliProspectiveTags.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Tags: ");
                    GlobalClass globalClass;
                    globalClass = (GlobalClass) getApplicationContext();
                    sb.append(globalClass.getTagTextFromID(gFileItem.aliProspectiveTags.get(0), GlobalClass.MEDIA_CATEGORY_VIDEOS));
                    for (int i = 1; i < gFileItem.aliProspectiveTags.size(); i++) {
                        sb.append(", ");
                        sb.append(globalClass.getTagTextFromID(gFileItem.aliProspectiveTags.get(i), GlobalClass.MEDIA_CATEGORY_VIDEOS));
                    }
                    TextView textView_VideoPopupSelectedTags = findViewById(R.id.textView_VideoPopupSelectedTags);
                    if (textView_VideoPopupSelectedTags != null) {
                        textView_VideoPopupSelectedTags.setText(sb.toString());
                    }
                }
            }

            long lVideoDuration = gFileItem.lVideoTimeInMilliseconds;

            if(lVideoDuration < 0L){
                //If there is no video length, exit this activity.
                finish();
            }

            gVideoView_VideoPlayer = findViewById(R.id.videoView_VideoPlayerPreview);

            //Configure the media controller:
            gMediaController = new MediaController(this);
            gMediaController.setMediaPlayer(gVideoView_VideoPlayer);
            gVideoView_VideoPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    gMediaController.setAnchorView(gVideoView_VideoPlayer);
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                gMediaController.addOnUnhandledKeyEventListener(new View.OnUnhandledKeyEventListener() {
                    @Override
                    public boolean onUnhandledKeyEvent(View view, KeyEvent keyEvent) {
                        //Handle BACK button
                        if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                            onBackPressed();
                            return true;
                        }
                        return false;
                    }
                });
            }
            gVideoView_VideoPlayer.setMediaController(gMediaController);
            Uri uriVideoFile = Uri.parse(gFileItem.sUri);
            gVideoView_VideoPlayer.setVideoURI(uriVideoFile);
            if (giCurrentVideoPosition > 0) {
                gVideoView_VideoPlayer.seekTo(giCurrentVideoPosition);
            } else {
                // Skipping to 1 shows the first frame of the video.
                gVideoView_VideoPlayer.seekTo(1);
            }
            gVideoView_VideoPlayer.setZOrderOnTop(true);
            gVideoView_VideoPlayer.start();
        }

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
            imageView_GradeArray[i] = findViewById(giGradeImageViews[i]);
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

    }

    @Override
    protected void onResume() {
        super.onResume();
        gVideoView_VideoPlayer.seekTo(giCurrentVideoPosition);
        if(giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING){
            gVideoView_VideoPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        giCurrentVideoPosition = gVideoView_VideoPlayer.getCurrentPosition();
        if(gVideoView_VideoPlayer.isPlaying()){
            giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
        } else {
            giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
        }
        gVideoView_VideoPlayer.pause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        gVideoView_VideoPlayer.stopPlayback();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(PLAYBACK_TIME, gVideoView_VideoPlayer.getCurrentPosition());
    }

    private void displayGrade(){
        //Show the rating:
        ImageView[] imageView_GradeArray = new ImageView[5];
        boolean bGradeIVsOK = true;
        for(int i = 0; i < giGradeImageViews.length; i++){
            imageView_GradeArray[i] = findViewById(giGradeImageViews[i]);
            if(imageView_GradeArray[i] == null){
                bGradeIVsOK = false;
            }
        }
        if (bGradeIVsOK){
            Drawable drawable_SolidStar = ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_grade_white_18dp, null);
            Drawable drawable_EmptyStar = ResourcesCompat.getDrawable(getResources(), R.drawable.outline_grade_white_18dp, null);
            for(int i = 0; i < gFileItem.iGrade; i++) {
                imageView_GradeArray[i].setImageDrawable(drawable_SolidStar);
            }
            for(int i = gFileItem.iGrade; i < giGradeImageViews.length; i++) {
                imageView_GradeArray[i].setImageDrawable(drawable_EmptyStar);
            }
        }

    }

    private class gradeOnClickListener implements View.OnClickListener{

        int iGrade;

        public gradeOnClickListener(int iGrade){
            this.iGrade = iGrade;
        }

        @Override
        public void onClick(View view) {
            gFileItem.iGrade = iGrade;
            displayGrade();
        }
    }

}
