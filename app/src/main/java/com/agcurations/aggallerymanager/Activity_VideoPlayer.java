package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class Activity_VideoPlayer extends AppCompatActivity {

    private GlobalClass globalClass;

    private TreeMap<Integer, ItemClass_CatalogItem> treeMapRecyclerViewVideos;
    private Integer giKey;

    private int giCurrentVideoPosition = 1;
    private final int VIDEO_PLAYBACK_STATE_PAUSED = 0;
    private final int VIDEO_PLAYBACK_STATE_PLAYING = 1;
    private int giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
    private static final String PLAYBACK_TIME = "play_time";

    private DrawerLayout gDrawerLayout;
    private VideoView gVideoView_VideoPlayer;
    private ImageView gImageView_GifViewer;
    private MediaController gMediaController;

    //ExoPlayer is used for playback of local M3U8 files:
    private SimpleExoPlayer gSimpleExoPlayer;
    private PlayerView gplayerView_ExoVideoPlayer;
    private PlayerControlView gPlayerControlView_ExoPlayerControls;

    private Fragment_ItemDetails gFragment_itemDetails;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        mVisible = true;
        gVideoView_VideoPlayer = findViewById(R.id.videoView_VideoPlayer);
        gplayerView_ExoVideoPlayer = findViewById(R.id.playerView_ExoVideoPlayer);
        gPlayerControlView_ExoPlayerControls = findViewById(R.id.playerControlView_ExoPlayerControls);
        gImageView_GifViewer = findViewById(R.id.imageView_GifViewer);
        gDrawerLayout = findViewById(R.id.drawer_layout);



        globalClass = (GlobalClass) getApplicationContext();

        //Get the treeMap and the key identifying the treeMap data to use for the first video to show:
        Intent intentVideoPlayer = this.getIntent();
        /*HashMap<Integer, ItemClass_CatalogItem> hashMapTemp = (HashMap<Integer, ItemClass_CatalogItem>)
                intentVideoPlayer.getSerializableExtra(Activity_CatalogViewer.RECYCLERVIEW_VIDEO_TREEMAP_FILTERED);
        treeMapRecyclerViewVideos = new TreeMap<>();
        if(hashMapTemp == null){
            finish();
            return;
        }
        treeMapRecyclerViewVideos.putAll(hashMapTemp);*/
        treeMapRecyclerViewVideos = globalClass.gtmCatalogViewerDisplayTreeMap;

        int iVideoID = intentVideoPlayer.getIntExtra(Activity_CatalogViewer.RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_ID,0);

        //Get the TreeMap key associated with the Video ID provided:
        giKey = 0;
        for (Map.Entry<Integer, ItemClass_CatalogItem>
                entry : treeMapRecyclerViewVideos.entrySet()) {
            if(entry.getValue().sItemID.equals(Integer.toString(iVideoID))) {
                giKey = entry.getKey();
            }
        }

        if (savedInstanceState != null) {
            giCurrentVideoPosition = savedInstanceState.getInt(PLAYBACK_TIME);
        }

        gMediaController = new MediaController(this);
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

        gMediaController.setMediaPlayer(gVideoView_VideoPlayer);
        gVideoView_VideoPlayer.setMediaController(gMediaController);

        //Set a touch listener to the VideoView so that the user can pause video and obfuscate with a
        //  double-tap, as well as swipe to go to the next video:
        final GestureDetector gdVideoView = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){

            //Here is the method for double tap
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                gVideoView_VideoPlayer.pause();
                ImageButton ImageButton_ObfuscationImage = findViewById(R.id.ImageButton_ObfuscationImage);
                ImageButton_ObfuscationImage.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Double tap detected. Obfuscating...", Toast.LENGTH_SHORT).show();
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

                if (gbAutoHide) {
                    //Delay hide while the user is interacting with controls
                    delayedHide();
                }

                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggle();
                if (mVisible && gbAutoHide) {
                    delayedHide();
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

//            public void onSwipeTop() {
//            }

            /*public void onSwipeBottom() {
            }*/

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
                Toast.makeText(getApplicationContext(), "Double tap detected. Obfuscating...", Toast.LENGTH_SHORT).show();
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
                if (mVisible && gbAutoHide) {
                    delayedHide();
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

//            public void onSwipeTop() {
//            }

            /*public void onSwipeBottom() {
            }*/

        });

        gImageView_GifViewer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gdImageView.onTouchEvent(event);
            }
        });

        if(globalClass.giSelectedCatalogMediaCategory != GlobalClass.MEDIA_CATEGORY_VIDEOS){
            gbAutoHide = true;
        }

        //Instantiate the ViewModel tracking tag data from the tag selector fragment.
        //  We need to observe and track any request from the TagEditor to reload the file.
        //  This would occur if the user deletes the tag folder holding the file, in which case
        //  the file would be moved.
        ViewModel_Fragment_SelectTags viewModel_fragment_selectTags = new ViewModelProvider(this).get(ViewModel_Fragment_SelectTags.class);
        //React to if the TagEditor is called and TagEditor requests that we reload the file:
        final Observer<Boolean> observerTagDeleted = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean bTagDeleted) {
                if(bTagDeleted) {
                    //If a tag is deleted, the video file may have been moved.
                    //  On import, items are copied into folders matching the first tag selected by the user.
                    //  If that 'primary tag' needs to be deleted at the users request, then the file gets moved
                    //  to its next tag folder. If the user merely reassigns a first tag, thus reordering or removing all
                    //  assigned tags, the file does not get moved. Only on tag delete would a file be moved.
                    initializePlayer();
                    gVideoView_VideoPlayer.seekTo(giCurrentVideoPosition);
                    gVideoView_VideoPlayer.start();
                }
            }
        };
        viewModel_fragment_selectTags.bTagDeleted.observe(this, observerTagDeleted);

        gSimpleExoPlayer = new SimpleExoPlayer.Builder(this).build();
        gplayerView_ExoVideoPlayer.setPlayer(gSimpleExoPlayer);
        gPlayerControlView_ExoPlayerControls.setPlayer(gSimpleExoPlayer);

        initializePlayer();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            gVideoView_VideoPlayer.seekTo(giCurrentVideoPosition);
            if (giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING) {
                gVideoView_VideoPlayer.start();
            }
        }
    }

    @Override
    protected void onPause() {
        giCurrentVideoPosition = gVideoView_VideoPlayer.getCurrentPosition();
        if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            giCurrentVideoPosition = gVideoView_VideoPlayer.getCurrentPosition();
            if (gVideoView_VideoPlayer.isPlaying()) {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
            } else {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
            }
            gVideoView_VideoPlayer.pause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            gVideoView_VideoPlayer.stopPlayback();
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            outState.putInt(PLAYBACK_TIME, gVideoView_VideoPlayer.getCurrentPosition());
        }
    }

    //==============================================================================================
    //  Video-affecting routines
    //==============================================================================================

    private boolean bFileIsGif;

    private void initializePlayer() {

        int iMediaCategory = globalClass.giSelectedCatalogMediaCategory;
        Uri gMediaUri = null;
        if(treeMapRecyclerViewVideos.containsKey(giKey)) {
            ItemClass_CatalogItem ci;
            ci = treeMapRecyclerViewVideos.get(giKey);
            if (ci != null) {
                String sFileName = ci.sFilename;
                String sFilePath = globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                        ci.sFolder_Name + File.separator +
                        sFileName;

                setTitle(GlobalClass.JumbleFileName(sFileName));

                //Populate the item details fragment:
                if(gFragment_itemDetails == null) {
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    gFragment_itemDetails = new Fragment_ItemDetails();

                    Bundle args = new Bundle();
                    args.putSerializable(Fragment_ItemDetails.CATALOG_ITEM, ci); //NOTE!!!!! ci passed here gets marshalled as a reference, not a copy.
                    //Read more here: https://stackoverflow.com/questions/44698863/bundle-putserializable-serializing-reference-not-value
                    gFragment_itemDetails.setArguments(args);
                    fragmentTransaction.replace(R.id.fragment_Item_Details, gFragment_itemDetails);
                    fragmentTransaction.commit();
                } else {
                    gFragment_itemDetails.initData(ci);
                }

                //Create a time stamp for "last viewed" and update the catalog record and record in memory:
                ci.dDatetime_Last_Viewed_by_User = GlobalClass.GetTimeStampFloat();

                Service_CatalogViewer.startActionUpdateCatalogItem(this, ci);

                gMediaUri = Uri.parse(sFilePath);

                //Determine if this is a gif file, which the VideoView will not play:
                bFileIsGif = GlobalClass.JumbleFileName(sFileName).contains(".gif");

                if(bFileIsGif || (globalClass.giSelectedCatalogMediaCategory != GlobalClass.MEDIA_CATEGORY_VIDEOS)){
                    if(gMediaUri != null) {
                        if (gMediaUri.getPath() != null) {
                            File fGif = new File(gMediaUri.getPath());
                            Glide.with(getApplicationContext()).load(fGif).into(gImageView_GifViewer);
                        }
                    }
                    if(!gImageView_GifViewer.isShown()) {
                        gImageView_GifViewer.setVisibility(View.VISIBLE);
                        gVideoView_VideoPlayer.setZOrderOnTop(false);
                        gVideoView_VideoPlayer.setVisibility(View.INVISIBLE);
                        gplayerView_ExoVideoPlayer.setVisibility(View.INVISIBLE);
                        gPlayerControlView_ExoPlayerControls.setVisibility(View.INVISIBLE);
                    }
                } else if(ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_PROCESSING_M3U8_LOCAL) {
                    gImageView_GifViewer.setVisibility(View.INVISIBLE);
                    gVideoView_VideoPlayer.setVisibility(View.INVISIBLE);
                    gplayerView_ExoVideoPlayer.setVisibility(View.VISIBLE);
                    gPlayerControlView_ExoPlayerControls.setVisibility(View.VISIBLE);
                    String sVideoPath = globalClass.gfCatalogFolders[iMediaCategory] +
                            File.separator + ci.sFolder_Name +
                            File.separator + ci.sItemID +
                            File.separator + ci.sFilename;
                    File fTemp = new File(sVideoPath);
                    if(fTemp.exists()) {
                        MediaItem mediaItem = MediaItem.fromUri(sVideoPath);
                        gSimpleExoPlayer.setMediaItem(mediaItem);
                        gSimpleExoPlayer.prepare();

                        gSimpleExoPlayer.play();
                    } else {
                        Toast.makeText(this, "Cannot find M3U8 file.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    gImageView_GifViewer.setVisibility(View.INVISIBLE);
                    gplayerView_ExoVideoPlayer.setVisibility(View.INVISIBLE);
                    gPlayerControlView_ExoPlayerControls.setVisibility(View.INVISIBLE);
                    gVideoView_VideoPlayer.setVisibility(View.VISIBLE);
                    gVideoView_VideoPlayer.setVideoURI(gMediaUri);

                    if(gVideoView_VideoPlayer.getDuration() > giCurrentVideoPosition){
                        giCurrentVideoPosition = 1;
                    }
                    gVideoView_VideoPlayer.seekTo(giCurrentVideoPosition);
                    gVideoView_VideoPlayer.start();
                }

            } //End if our catalog item is not null.

        } //End if the catalog item was found in the passed-in list.

    }

    public void HideObfuscationImageButton(View v){
        v.setVisibility(View.INVISIBLE);
    }


    public void closeDrawer(){
        gDrawerLayout.closeDrawer(GravityCompat.START);
    }

    //==============================================================================================
    //==============================================================================================
    //==============================================================================================
    // Full-Screen Activity Functions (Auto-Created)

    /*
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static boolean gbAutoHide = false;  //Turn off auto-hide for videos. When the user is dragging the
    //   seek bar, the background navigation bar remains, but the MediaController relocates. This causes
    //   uncomfortable user interaction.

    /*
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
            gDrawerLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        }
    };

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            if(!bFileIsGif && (globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS)){
                gMediaController.show();
            }
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
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, 500);
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

        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Clear system visibility settings:
        gDrawerLayout.setSystemUiVisibility(0);

        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

     /*
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide() {
        //Use with caution. It makes trouble for the user when combined with a mediaController.
        //  Above note from AGGalleryManager author, WRC.
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, Activity_VideoPlayer.AUTO_HIDE_DELAY_MILLIS);
    }

}