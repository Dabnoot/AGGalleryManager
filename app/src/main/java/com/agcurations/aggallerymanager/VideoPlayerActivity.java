package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;

public class VideoPlayerActivity extends AppCompatActivity {

    private GlobalClass globalClass;

    private VideoView gVideoView;

    private TreeMap<Integer, String[]> treeMapRecyclerViewVideos;
    private Integer giKey;

    private int giCurrentPosition = 0;
    private static final String PLAYBACK_TIME = "play_time";

    //This guide was used: https://developer.android.com/codelabs/advanced-android-training-video-view#2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        globalClass = (GlobalClass) getApplicationContext();

        //Get the treeMap and the key identifying the treeMap data to use for the first video to show:
        Intent intentVideoPlayer = this.getIntent();
        HashMap<Integer, String[]> hashMapTemp = (HashMap<Integer, String[]>)
                intentVideoPlayer.getSerializableExtra(CatalogActivity.RECYCLERVIEW_VIDEO_TREEMAP_FILTERED);
        treeMapRecyclerViewVideos = new TreeMap<Integer, String[]>();
        if(hashMapTemp == null){
            //todo: Add message to user as to what went wrong.
            finish();
        }
        treeMapRecyclerViewVideos.putAll(hashMapTemp);
        giKey = intentVideoPlayer.getIntExtra(CatalogActivity.RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_KEY,0);

        if (savedInstanceState != null) {
            giCurrentPosition = savedInstanceState.getInt(PLAYBACK_TIME);
        }

        gVideoView = findViewById(R.id.videoview_VideoPlayer);
        MediaController controller = new MediaController(this);
        controller.setMediaPlayer(gVideoView);
        gVideoView.setMediaController(controller);

        initializePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();

        releasePlayer();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(PLAYBACK_TIME, gVideoView.getCurrentPosition());
    }


    private Uri getMedia() {

        String[] sFields = treeMapRecyclerViewVideos.get(giKey);
        if(sFields != null) {
            String sVideoPath = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath() + File.separator +
                    sFields[GlobalClass.VIDEO_FOLDER_NAME_INDEX] + File.separator +
                    sFields[GlobalClass.VIDEO_FILENAME_INDEX];
            return Uri.parse(sVideoPath);
        }

        return null;
    }

    private void initializePlayer() {
        Uri uriVideo = getMedia();
        gVideoView.setVideoURI(uriVideo);
        if (giCurrentPosition > 0) {
            gVideoView.seekTo(giCurrentPosition);
        } else {
            // Skipping to 1 shows the first frame of the video.
            gVideoView.seekTo(1);
        }
        gVideoView.start();
    }

    private void releasePlayer() {
        gVideoView.stopPlayback();
    }

}