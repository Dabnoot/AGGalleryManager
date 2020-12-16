package com.agcurations.aggallerymanager;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class Activity_VideoPreview extends AppCompatActivity {
    public static final String FILE_URI_STRING = "FILE_URI_STRING";
    public static final String FILE_ITEM = "FILE_ITEM";

    public static final String TAG_SELECTION_RESULT_BUNDLE = "TAG_SELECTION_RESULT_BUNDLE";
    public static final String TAG_SELECTION_TAG_IDS = "TAG_SELECTION_TAG_IDS";

    public static final String VIDEO_FILE_DURATION_MILLISECONDS_LONG = "VIDEO_FILE_DURATION_MILLISECONDS_LONG";

    //private String gsUriVideoFile;

    private ItemClass_File gFileItem;

    VideoView gVideoView;
    MediaController gMediaController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getSupportActionBar().hide();

        //Source: https://www.youtube.com/watch?v=fn5OlqQuOCk
        setContentView(R.layout.activity_video_preview);

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
                    sb.append(tagItems.get(0).TagText);
                    for (int i = 1; i < tagItems.size(); i++) {
                        sb.append(", ");
                        sb.append(tagItems.get(i).TagText);
                    }
                }
                TextView tv = findViewById(R.id.textView_VideoPopupSelectedTags);
                if(tv != null){
                    tv.setText(sb.toString());
                }

                //Get the tag IDs to pass back to the calling activity:
                ArrayList<Integer> aliTagIDs = new ArrayList<>();
                for(ItemClass_Tag ti : tagItems){
                    aliTagIDs.add(ti.TagID);
                }

                //Prepare a result to send back to the calling activity:
                Intent data = new Intent();
                Bundle b = new Bundle();
                //Put back the file URI string so that the file can be located:
                b.putSerializable(FILE_ITEM, gFileItem);
                b.putIntegerArrayList(TAG_SELECTION_TAG_IDS, aliTagIDs);
                data.putExtra(TAG_SELECTION_RESULT_BUNDLE, b);
                setResult(RESULT_OK, data);

            }
        };
        mViewModel.altiTagsSelected.observe(this, selectedTagsObserver);

        Bundle b = getIntent().getExtras();
        if(b != null) {

            gFileItem = (ItemClass_File) b.getSerializable(FILE_ITEM);

            //Start the tag selection fragment:
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment_SelectTags fst = new Fragment_SelectTags();
            Bundle args = new Bundle();
            args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_VIDEOS);
            args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, gFileItem.prospectiveTags);
            fst.setArguments(args);
            ft.replace(R.id.child_fragment_tag_selector, fst);
            ft.commit();

            //Init the tags list if there are tags already assigned to this item:
            //Get the text of the tags and display:
            if(gFileItem.prospectiveTags != null) {
                if (gFileItem.prospectiveTags.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Tags: ");
                    GlobalClass globalClass;
                    globalClass = (GlobalClass) getApplicationContext();
                    sb.append(globalClass.getTagTextFromID(gFileItem.prospectiveTags.get(0), GlobalClass.MEDIA_CATEGORY_VIDEOS));
                    for (int i = 1; i < gFileItem.prospectiveTags.size(); i++) {
                        sb.append(", ");
                        sb.append(globalClass.getTagTextFromID(gFileItem.prospectiveTags.get(i), GlobalClass.MEDIA_CATEGORY_VIDEOS));
                    }
                    TextView tv = findViewById(R.id.textView_VideoPopupSelectedTags);
                    if (tv != null) {
                        tv.setText(sb.toString());
                    }
                }
            }



            //Build the preview:
            long lVideoDuration = gFileItem.videoTimeInMilliseconds;

            if(lVideoDuration < 0L){
                //If there is no video length, exit this activity.
                finish();
            }


            gVideoView = findViewById(R.id.videoView_VideoPlayerPreview);

            //Configure the media controller:
            gMediaController = new MediaController(this);
            gMediaController.setMediaPlayer(gVideoView);
            gVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    gMediaController.setAnchorView(gVideoView);
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
            gVideoView.setMediaController(gMediaController);
            Uri uriVideoFile = Uri.parse(gFileItem.uri);
            gVideoView.setVideoURI(uriVideoFile);
            gVideoView.seekTo(1);
            gVideoView.setZOrderOnTop(true);
            gVideoView.start();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        gVideoView.seekTo(1);
        gVideoView.start();

    }

    @Override
    protected void onStop() {
        super.onStop();
        gVideoView.stopPlayback();
    }



}
