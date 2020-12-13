package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Activity_VideoPlayerFullScreen extends AppCompatActivity {

    private GlobalClass globalClass;

    private TreeMap<Integer, String[]> treeMapRecyclerViewVideos;
    private Integer giKey;

    private int giCurrentPosition = 0;
    private static final String PLAYBACK_TIME = "play_time";

    private RelativeLayout gRelativeLayout_VideoPlayer;
    private VideoView gVideoView_VideoPlayer;
    private ImageView gImageView_GifViewer;
    private MediaController gMediaController;

    private Uri gMediaUri;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player_full_screen);

        mVisible = true;
        //mControlsView = findViewById(R.id.fullscreen_content_controls);
        gVideoView_VideoPlayer = findViewById(R.id.videoView_VideoPlayer);
        gImageView_GifViewer = findViewById(R.id.imageView_GifViewer);
        gRelativeLayout_VideoPlayer = findViewById(R.id.frameLayout_VideoPlayer);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        globalClass = (GlobalClass) getApplicationContext();

        //Get the treeMap and the key identifying the treeMap data to use for the first video to show:
        Intent intentVideoPlayer = this.getIntent();
        HashMap<Integer, String[]> hashMapTemp = (HashMap<Integer, String[]>)
                intentVideoPlayer.getSerializableExtra(Activity_CatalogViewer.RECYCLERVIEW_VIDEO_TREEMAP_FILTERED);
        treeMapRecyclerViewVideos = new TreeMap<>();
        if(hashMapTemp == null){
            finish();
            return;
        }
        treeMapRecyclerViewVideos.putAll(hashMapTemp);
        int iVideoID = intentVideoPlayer.getIntExtra(Activity_CatalogViewer.RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_ID,0);

        //Get the TreeMap key associated with the Video ID provided:
        giKey = 0;
        for (Map.Entry<Integer, String[]>
                entry : treeMapRecyclerViewVideos.entrySet()) {
            if(entry.getValue()[GlobalClass.VIDEO_ID_INDEX].equals(Integer.toString(iVideoID))) {
                giKey = entry.getKey();
            }
        }

        if (savedInstanceState != null) {
            giCurrentPosition = savedInstanceState.getInt(PLAYBACK_TIME);
        }

        //gVideoView = findViewById(R.id.videoView_VideoPlayer);
        gMediaController = new MediaController(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            gMediaController.addOnUnhandledKeyEventListener(new View.OnUnhandledKeyEventListener() {
                @Override
                public boolean onUnhandledKeyEvent(View view, KeyEvent keyEvent) {
                    //Handle BACK button
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        //mediaController.hide(); //Hide mediaController,according to your needs, you can also called here onBackPressed() or finish()
                        onBackPressed();
                        return true;
                    }

                    return false;
                }
            });
        }

        gMediaController.setMediaPlayer(gVideoView_VideoPlayer);
        gVideoView_VideoPlayer.setMediaController(gMediaController);
        //mediaController.setAnchorView(findViewById(R.id.videoView_VideoPlayer));

        //Set a touch listener to the VideoView so that the user can pause video and obfuscate with a
        //  double-tap, as well as swipe to go to the next video:
        final GestureDetector gdVideoView = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){

            //Here is the method for double tap
            @Override
            public boolean onDoubleTap(MotionEvent e) {

                gVideoView_VideoPlayer.pause();
                ImageButton ImageButton_ObfuscationImage = findViewById(R.id.ImageButton_ObfuscationImage);
                ImageButton_ObfuscationImage.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Double tap detected.", Toast.LENGTH_SHORT).show();

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
                /*if(mediaController.isShowing()){
                    mediaController.hide();
                } else {
                    mediaController.show();
                }*/
                toggle();
                if (mVisible && AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
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
                        if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                        result = true;
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }

            public void onSwipeRight() {
                int iTempKey = giKey - 1;
                if(treeMapRecyclerViewVideos.containsKey(iTempKey)) {
                    gVideoView_VideoPlayer.stopPlayback();
                    giKey--;
                    initializePlayer();
                }
            }

            public void onSwipeLeft() {
                int iTempKey = giKey + 1;
                if(treeMapRecyclerViewVideos.containsKey(iTempKey)) {
                    gVideoView_VideoPlayer.stopPlayback();
                    giKey++;
                    initializePlayer();
                }
            }

            public void onSwipeTop() {
            }

            public void onSwipeBottom() {
            }

        });

        gVideoView_VideoPlayer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gdVideoView.onTouchEvent(event);
            }
        });

        gVideoView_VideoPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });


        //Prepare the Gif image viewer to accept swipe to go to next or previous file:
        //Set a touch listener to the VideoView so that the user can pause video and obfuscate with a
        //  double-tap, as well as swipe to go to the next video:
        final GestureDetector gdImageView = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){

            //Here is the method for double tap
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                ImageButton ImageButton_ObfuscationImage = findViewById(R.id.ImageButton_ObfuscationImage);
                ImageButton_ObfuscationImage.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Double tap detected.", Toast.LENGTH_SHORT).show();
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
                toggle();
                if (mVisible && AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
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
                        if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                        result = true;
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }

            public void onSwipeRight() {
                int iTempKey = giKey - 1;
                if(treeMapRecyclerViewVideos.containsKey(iTempKey)) {
                    giKey--;
                    initializePlayer();
                }
            }

            public void onSwipeLeft() {
                int iTempKey = giKey + 1;
                if(treeMapRecyclerViewVideos.containsKey(iTempKey)) {
                    giKey++;
                    initializePlayer();
                }
            }

            public void onSwipeTop() {
            }

            public void onSwipeBottom() {
            }

        });

        gImageView_GifViewer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gdImageView.onTouchEvent(event);
            }
        });


        initializePlayer();
    }


    @Override
    protected void onStop() {
        super.onStop();

        releasePlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(getApplicationContext(), "Swipe up to access Switch, Home, and Back buttons.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(PLAYBACK_TIME, gVideoView_VideoPlayer.getCurrentPosition());
    }

    //==============================================================================================
    //  Video-affecting routines
    //==============================================================================================

    private boolean bFileIsGif;
    private Uri getMedia() {

        if(treeMapRecyclerViewVideos.containsKey(giKey)) {
            String[] sFields = treeMapRecyclerViewVideos.get(giKey);
            if (sFields != null) {
                String sVideoPath = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath() + File.separator +
                        sFields[GlobalClass.VIDEO_FOLDER_NAME_INDEX] + File.separator +
                        sFields[GlobalClass.VIDEO_FILENAME_INDEX];
                //Determine if this is a gif file, which the VideoView will not play:
                setTitle(GlobalClass.JumbleFileName(sFields[GlobalClass.VIDEO_FILENAME_INDEX]));
                bFileIsGif = GlobalClass.JumbleFileName(sFields[GlobalClass.VIDEO_FILENAME_INDEX]).contains(".gif");
                return Uri.parse(sVideoPath);
            }
        }
        return null;
    }




    private void initializePlayer() {
        gMediaUri = getMedia();
        if(bFileIsGif){
            File fGif = new File(gMediaUri.getPath());
            Glide.with(getApplicationContext()).load(fGif).into(gImageView_GifViewer);
            gImageView_GifViewer.setVisibility(View.VISIBLE);
            gVideoView_VideoPlayer.setZOrderOnTop(false);
            gVideoView_VideoPlayer.setVisibility(View.INVISIBLE);
        } else {
            gImageView_GifViewer.setVisibility(View.INVISIBLE);
            gVideoView_VideoPlayer.setVisibility(View.VISIBLE);
            gVideoView_VideoPlayer.setVideoURI(gMediaUri);
            if (giCurrentPosition > 0) {
                gVideoView_VideoPlayer.seekTo(giCurrentPosition);
            } else {
                // Skipping to 1 shows the first frame of the video.
                gVideoView_VideoPlayer.seekTo(1);
            }
            gVideoView_VideoPlayer.setZOrderOnTop(true);

            gVideoView_VideoPlayer.setLayoutParams(getLayoutParamsBarsInVisible(gMediaUri));
            gVideoView_VideoPlayer.invalidate();

            gVideoView_VideoPlayer.start();
        }
    }

    private ViewGroup.LayoutParams getLayoutParamsBarsVisible(Uri uriMedia){
        //Get the aspect ratio:
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(this, uriMedia);
        int iVideoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int iVideoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        retriever.close();
        retriever.release();

        //Set gVideoView size:
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int iStatBarHeight = getStatusBarHeight();
        //int iNavBarHeight = getNavigationBarHeight();
        int iActionBarHeight = getActionBarHeight();
        int y = dm.heightPixels;
        y -= iStatBarHeight;
        //y -= iNavBarHeight;
        y -= iActionBarHeight;
        //int x = dm.widthPixels;
        int x = (int) ((float) iVideoWidth / iVideoHeight * y);

        RelativeLayout.LayoutParams videoviewlp = new RelativeLayout.LayoutParams(x, y);
        videoviewlp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        videoviewlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        videoviewlp.setMargins(0,iStatBarHeight,0,0);

        return  videoviewlp;
    }

    private ViewGroup.LayoutParams getLayoutParamsBarsInVisible(Uri uriMedia){
        //Get the aspect ratio:
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(this, uriMedia);
        int iVideoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int iVideoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        retriever.close();
        retriever.release();

        //Set gVideoView size:
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        //int iStatBarHeight = getStatusBarHeight();
        //int iNavBarHeight = getNavigationBarHeight();
        //int iActionBarHeight = getActionBarHeight();
        int y = dm.heightPixels;
        //y -= iStatBarHeight;
        //y -= iNavBarHeight;
        //y -= iActionBarHeight;
        //int x = dm.widthPixels;
        int x = (int) ((float) iVideoWidth / iVideoHeight * y);

        RelativeLayout.LayoutParams videoviewlp = new RelativeLayout.LayoutParams(x, y);
        videoviewlp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        videoviewlp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        videoviewlp.setMargins(0,0,0,0);

        return  videoviewlp;
    }



    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /*public int getNavigationBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }*/

    public int getActionBarHeight() {
        int result = 0;
        // Calculate ActionBar height
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
            result = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }
        return result;
    }



    private void releasePlayer() {
        gVideoView_VideoPlayer.stopPlayback();
    }









    public void HideObfuscationImageButton(View v){
        v.setVisibility(View.INVISIBLE);
    }


    //==============================================================================================
    //==============================================================================================
    //==============================================================================================
    // Full-Screen Activity Functions (Auto-Created)

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
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            gRelativeLayout_VideoPlayer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            if(!bFileIsGif){
                gVideoView_VideoPlayer.setLayoutParams(getLayoutParamsBarsInVisible(gMediaUri));
                gVideoView_VideoPlayer.invalidate();
            }


            gMediaController.hide();


        }
    };
    //private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            //mControlsView.setVisibility(View.VISIBLE);
            if(!bFileIsGif){
                gVideoView_VideoPlayer.setLayoutParams(getLayoutParamsBarsVisible(gMediaUri));
                gVideoView_VideoPlayer.invalidate();
            }
            gMediaController.show();



        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };



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
        //mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        gRelativeLayout_VideoPlayer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
               | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
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

}