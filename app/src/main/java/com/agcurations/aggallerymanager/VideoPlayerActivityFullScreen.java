package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VideoPlayerActivityFullScreen extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    //====================================================================================================
    //====================================================================================================
    //====================================================================================================

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

        setContentView(R.layout.activity_video_player_full_screen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

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
        MediaController mediaController = new MediaController(this);

        mediaController.setMediaPlayer(gVideoView);
        gVideoView.setMediaController(mediaController);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        FrameLayout.LayoutParams lpp = new FrameLayout.LayoutParams(dm.widthPixels, 170);
        lpp.gravity= Gravity.BOTTOM;
        Resources resources = this.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int iNavigationBarSizeInPixels = 100;
        if (resourceId > 0) {
            iNavigationBarSizeInPixels = resources.getDimensionPixelSize(resourceId);
        }
        lpp.setMargins(0,0,0,iNavigationBarSizeInPixels);
        mediaController.setLayoutParams(lpp);

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












    //==============================================================================================
    //==============================================================================================
    //==============================================================================================



    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        //mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        //        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}