package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.util.MimeTypes;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class Activity_VideoPlayer extends AppCompatActivity {

    private GlobalClass globalClass;

    private TreeMap<Integer, ItemClass_CatalogItem> treeMapRecyclerViewCatItems;
    private Integer giKey;

    private long glCurrentVideoPosition = 1;
    private final int VIDEO_PLAYBACK_STATE_PAUSED = 0;
    private final int VIDEO_PLAYBACK_STATE_PLAYING = 1;
    private int giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
    private static final String PLAYBACK_TIME = "play_time";

    private DrawerLayout gDrawerLayout;
    private VideoView gVideoView_VideoPlayer;
    private ImageView gImageView_GifViewer;
    private MediaController gMediaController;

    //ExoPlayer is used for playback of local M3U8 files:
    private ExoPlayer gExoPlayer;
    private StyledPlayerView gplayerView_ExoVideoPlayer;
    private StyledPlayerControlView gPlayerControlView_ExoPlayerControls;

    private Fragment_ItemDetails gFragment_itemDetails;

    private boolean gbPlayingM3U8;

    static boolean active = false; //Used to keep a runnable from doing stuff after the activity closes.

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        mVisible = true;
        gVideoView_VideoPlayer = findViewById(R.id.videoView_VideoPlayer);
        gplayerView_ExoVideoPlayer = findViewById(R.id.playerView_ExoVideoPlayer);
        gPlayerControlView_ExoPlayerControls = findViewById(R.id.playerControlView_ExoPlayerControls);
        gImageView_GifViewer = findViewById(R.id.imageView_GifViewer);
        gDrawerLayout = findViewById(R.id.drawer_layout);
        gDrawerLayout.openDrawer(GravityCompat.START); //Start the drawer open so that the user knows it's there.
        gDrawerLayout.postDelayed(new Runnable() { //Configure a runnable to close the drawer after a timeout.
            @Override
            public void run() {
                closeDrawer();
            }
        }, 1500);

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
        treeMapRecyclerViewCatItems = globalClass.gtmCatalogViewerDisplayTreeMap;

        int iVideoID = intentVideoPlayer.getIntExtra(Activity_CatalogViewer.RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_ID,0);

        //Get the TreeMap key associated with the Video ID provided:
        giKey = 0;
        for (Map.Entry<Integer, ItemClass_CatalogItem>
                entry : treeMapRecyclerViewCatItems.entrySet()) {
            if(entry.getValue().sItemID.equals(Integer.toString(iVideoID))) {
                giKey = entry.getKey();
            }
        }

        if (savedInstanceState != null) {
            glCurrentVideoPosition = savedInstanceState.getLong(PLAYBACK_TIME);
        }

        gMediaController = new MediaController(this);
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

        gMediaController.setMediaPlayer(gVideoView_VideoPlayer);
        gVideoView_VideoPlayer.setMediaController(gMediaController);

        //Set a touch listener to the VideoView so that the user can pause video and obfuscate with a
        //  double-tap, as well as swipe to go to the next video:
        final GestureDetector gdVideoView = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){

            //Here is the method for double tap
            @Override
            public boolean onDoubleTap(MotionEvent e) {
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

                if(gbPlayingM3U8) {
                    return false;
                } else {
                    return true; //The other player does not trigger onSingleTapConfirmed without
                    // this item true.
                }
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
                        if (diffY > 0) {
                            onSwipeUp();
                        } else {
                            onSwipeDown();
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
                if(treeMapRecyclerViewCatItems.containsKey(iTempKey)) {
                    StopPlayback();
                    giKey--;
                    glCurrentVideoPosition = 1; //If moving to another item, set play position to start.
                    initializePlayer();
                }
            }

            public void onSwipeLeft() {
                int iTempKey = giKey + 1;
                if(treeMapRecyclerViewCatItems.containsKey(iTempKey)) {
                    StopPlayback();
                    giKey++;
                    glCurrentVideoPosition = 1; //If moving to another item, set play position to start.
                    initializePlayer();
                }
            }

            public void onSwipeUp() {
                //The user is likely attempting to bring up the navigation bar.
                // If the navigation bar is shown, hide it.
                /*ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.show();
                }
                if(!bFileIsGif && (globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS)){
                    //Figure out which video player is active, and hide its media controls:
                    if (gbPlayingM3U8) {
                        gPlayerControlView_ExoPlayerControls.hide();
                    } else {
                        gMediaController.hide();
                    }

                }
                mVisible = false;*/
            }

            public void onSwipeDown() {
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
                if(treeMapRecyclerViewCatItems.containsKey(iTempKey)) {
                    giKey--;
                    glCurrentVideoPosition = 1; //If moving to another item, set play position to start.
                    initializePlayer();
                }
            }

            public void onSwipeLeft() {
                int iTempKey = giKey + 1;
                if(treeMapRecyclerViewCatItems.containsKey(iTempKey)) {
                    giKey++;
                    glCurrentVideoPosition = 1; //If moving to another item, set play position to start.
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
                    //  Update 11/14/2021 - I'm not sure if the above is still true. Will need to test again. (todo).
                    initializePlayer();
                    if(gbPlayingM3U8){
                        gExoPlayer.seekTo(glCurrentVideoPosition);
                        gExoPlayer.play();
                    } else {
                        gVideoView_VideoPlayer.seekTo((int) glCurrentVideoPosition);
                        gVideoView_VideoPlayer.start();
                    }
                }
            }
        };
        viewModel_fragment_selectTags.bTagDeleted.observe(this, observerTagDeleted);

        //Create the ExoPlayer.
        //The next few lines are specifically to control the buffering.
        //  In the switch to Android SAF, I had to code the M3U8 files to include full Uri strings
        //    to each video segment file. When the player started buffering, I believe that it
        //    overloaded the memory and froze the tablet. A hard reset was required.
        //    The case may be that higher-resolution video files may require the buffer duration
        //    to be reduced.
        DefaultRenderersFactory renderersFactory;
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(5000, 20000, 1500, 2000).build();
        @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
        renderersFactory = new DefaultRenderersFactory(this) .setExtensionRendererMode(extensionRendererMode);
        TrackSelector trackSelector = new DefaultTrackSelector(this);
        //End code addition to control buffer size.

        gExoPlayer = new ExoPlayer.Builder(this, renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build();

        gExoPlayer = new ExoPlayer.Builder(this).build();

        gplayerView_ExoVideoPlayer.setPlayer(gExoPlayer);
        gPlayerControlView_ExoPlayerControls.setPlayer(gExoPlayer);

        gplayerView_ExoVideoPlayer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
               return gdVideoView.onTouchEvent(event);
            }
        });

        initializePlayer();
    }

    private void PausePlayback(){

        if(gbPlayingM3U8) {
            glCurrentVideoPosition = gExoPlayer.getCurrentPosition();
            if (gExoPlayer.isPlaying()) {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
            } else {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
            }
            gExoPlayer.pause();
        } else {
            glCurrentVideoPosition = gVideoView_VideoPlayer.getCurrentPosition();
            if (gVideoView_VideoPlayer.isPlaying()) {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
            } else {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
            }
            gVideoView_VideoPlayer.pause();
        }
    }

    private void StopPlayback(){

        if(gbPlayingM3U8) {
            glCurrentVideoPosition = gExoPlayer.getCurrentPosition();
            if (gExoPlayer.isPlaying()) {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
            } else {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
            }
            gExoPlayer.stop();
        } else {
            glCurrentVideoPosition = gVideoView_VideoPlayer.getCurrentPosition();
            if (gVideoView_VideoPlayer.isPlaying()) {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
            } else {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
            }
            gVideoView_VideoPlayer.stopPlayback();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            /*gVideoView_VideoPlayer.seekTo((int) glCurrentVideoPosition);
            if (giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING) {
                gVideoView_VideoPlayer.start();
            }*/

            //Figure out which video player is active, and resume that object.
            if(gbPlayingM3U8) {
                gExoPlayer.seekTo(glCurrentVideoPosition);
                if (giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING) {
                    gExoPlayer.setPlayWhenReady(true);
                    //gSimpleExoPlayer.play();
                }
                gExoPlayer.pause();
            } else {
                gVideoView_VideoPlayer.seekTo((int) glCurrentVideoPosition);
                if (giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING) {
                    gVideoView_VideoPlayer.start();
                }
            }



        }
    }

    @Override
    protected void onPause() {
        /*glCurrentVideoPosition = gVideoView_VideoPlayer.getCurrentPosition();
        if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            glCurrentVideoPosition = gVideoView_VideoPlayer.getCurrentPosition();
            if (gVideoView_VideoPlayer.isPlaying()) {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
            } else {
                giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
            }
            gVideoView_VideoPlayer.pause();
        }*/

        //Figure out which video player is active, and pause that object.
        PausePlayback();
        super.onPause();
    }

    @Override
    protected void onStop() {
        /*if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            gVideoView_VideoPlayer.stopPlayback();
        }*/

        //Figure out which video player is active, and stop that object.
        StopPlayback();

        active = false;

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        /*if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            outState.putInt(PLAYBACK_TIME, gVideoView_VideoPlayer.getCurrentPosition());
        }*/

        //Figure out which video player is active, and save the position.
        if(gbPlayingM3U8) {
            glCurrentVideoPosition = gExoPlayer.getCurrentPosition();
        } else {
            glCurrentVideoPosition = gVideoView_VideoPlayer.getCurrentPosition();
        }

        outState.putLong(PLAYBACK_TIME, glCurrentVideoPosition);

    }

    //==============================================================================================
    //  Video-affecting routines
    //==============================================================================================

    private boolean bFileIsGif;

    private void initializePlayer() {
        String sMessage;
        int iMediaCategory = globalClass.giSelectedCatalogMediaCategory;

        if(treeMapRecyclerViewCatItems.containsKey(giKey)) {
            ItemClass_CatalogItem ci;
            ci = treeMapRecyclerViewCatItems.get(giKey);
            if (ci != null) {
                String sFileName = ci.sFilename;

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
                ci.dDatetime_Last_Viewed_by_User = GlobalClass.GetTimeStampDouble();

                Service_CatalogViewer.startActionUpdateCatalogItem(this, ci, "Activity_VideoPlayer:initializePlayer()");

                DocumentFile dfMediaFileFolder = globalClass.gdfCatalogFolders[iMediaCategory].findFile(ci.sFolder_Name);
                if(dfMediaFileFolder != null) {
                    if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                        dfMediaFileFolder = dfMediaFileFolder.findFile(ci.sItemID);
                    }
                    DocumentFile dfMediaFile = dfMediaFileFolder.findFile(ci.sFilename);

                    if(dfMediaFile != null) {

                        //Determine if this is a gif file, which the VideoView will not play:
                        bFileIsGif = GlobalClass.JumbleFileName(sFileName).contains(".gif");

                        if (bFileIsGif || (globalClass.giSelectedCatalogMediaCategory != GlobalClass.MEDIA_CATEGORY_VIDEOS)) {
                            gbPlayingM3U8 = false;

                            Glide.with(getApplicationContext()).load(dfMediaFile.getUri()).into(gImageView_GifViewer);

                            if (!gImageView_GifViewer.isShown()) {
                                gImageView_GifViewer.setVisibility(View.VISIBLE);
                                gVideoView_VideoPlayer.setZOrderOnTop(false);
                                gVideoView_VideoPlayer.setVisibility(View.INVISIBLE);
                                gplayerView_ExoVideoPlayer.setVisibility(View.INVISIBLE);
                                gPlayerControlView_ExoPlayerControls.setVisibility(View.INVISIBLE);
                            }
                        } else if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                            gbPlayingM3U8 = true;
                            gImageView_GifViewer.setVisibility(View.INVISIBLE);
                            gVideoView_VideoPlayer.setVisibility(View.INVISIBLE);
                            gplayerView_ExoVideoPlayer.setVisibility(View.VISIBLE);
                            gPlayerControlView_ExoPlayerControls.setVisibility(View.VISIBLE);

                            //Find the M3U8 file:
                            Uri uriM3U8;

                            String sRelativePathM3U8 = GlobalClass.gsUriAppRootPrefix
                                    + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                                    + GlobalClass.gsFileSeparator + ci.sFolder_Name
                                    + GlobalClass.gsFileSeparator + ci.sItemID
                                    + GlobalClass.gsFileSeparator + ci.sFilename;

                            uriM3U8 = Uri.parse(sRelativePathM3U8);

                            TreeMap<Integer, String> tmFileSequence = new TreeMap<>();

                            if(GlobalClass.gbOptionIndividualizeM3U8VideoSegmentPlayback) {
                                //If the option to individualize M3U8 video segment playback is selected,
                                //  create an array of the individual video segment files and feed
                                //  them into the ExoPlayer as a playlist.
                                //  There was an issue during coding and testing an SAF-adapted M3U8
                                //  in which the program would freeze the entire tablet causing the
                                //  need for a hard reset. If this happens again, a coder can change the
                                //  buffer amount (in onCreate), or configure this boolean to be
                                //  user-configurable.

                                List<MediaItem> lMediaItems = new ArrayList<>();
                                int iSequence = 0;
                                String[] sFileNameAndExtension = ci.sFilename.split("\\.");
                                if (sFileNameAndExtension.length != 2) {
                                    return;
                                }
                                String sNewFileName = sFileNameAndExtension[0] + "_new." + sFileNameAndExtension[1];
                                DocumentFile dfNewM3U8 = dfMediaFileFolder.createFile(MimeTypes.BASE_TYPE_TEXT, sNewFileName);


                                try {
                                    InputStream isM3U8 = GlobalClass.gcrContentResolver.openInputStream(uriM3U8);
                                    OutputStream osM3U8New = GlobalClass.gcrContentResolver.openOutputStream(dfNewM3U8.getUri());
                                    if (osM3U8New == null) {
                                        return;
                                    }

                                    if (isM3U8 != null) {
                                        BufferedReader brM3U8 = new BufferedReader(new InputStreamReader(isM3U8));
                                        StringBuilder sbM3U8New = new StringBuilder();
                                        String sLine = brM3U8.readLine();
                                        StringBuilder sbUriString = new StringBuilder();
                                        while (sLine != null) {
                                            if (!sLine.startsWith("#") && sLine.endsWith("st")) {
                                                //Form the uri string:
                                                sbUriString.append(GlobalClass.gsUriAppRootPrefix)
                                                        .append(GlobalClass.gsFileSeparator).append(GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS])
                                                        .append(GlobalClass.gsFileSeparator).append(ci.sFolder_Name)
                                                        .append(GlobalClass.gsFileSeparator).append(ci.sItemID)
                                                        .append(GlobalClass.gsFileSeparator).append(sLine);
                                                sLine = sbUriString.toString();
                                                sbUriString.setLength(0);
                                                iSequence++;

                                            }
                                            sbM3U8New.append(sLine);
                                            sbM3U8New.append("\n");
                                            sLine = brM3U8.readLine();
                                        }
                                        brM3U8.close();
                                        isM3U8.close();
                                        String sData = sbM3U8New.toString();
                                        osM3U8New.write(sData.getBytes(StandardCharsets.UTF_8));
                                        osM3U8New.flush();
                                        osM3U8New.close();
                                    }
                                } catch (Exception e) {
                                    sMessage = "Problem opening InputStream to M3U8 file: " + e.getMessage();
                                    Log.d("Activity_VideoPlayer:initializePlayer", sMessage);
                                }

                                try {
                                    for (int i = 0; i <= iSequence; i++) {
                                        String sFileNameSought = tmFileSequence.get(i);
                                        if (sFileNameSought != null) {
                                            dfMediaFile = dfMediaFileFolder.findFile(sFileNameSought);
                                            if (dfMediaFile != null) {
                                                Uri uriTest = dfMediaFile.getUri();
                                            /*AssetFileDescriptor assetFileDescriptor;
                                            try {
                                                assetFileDescriptor = GlobalClass.gcrContentResolver.openAssetFileDescriptor(uriTest, "r");
                                            } catch (Exception e){
                                                String sMessage = "" + e.getMessage();
                                                Log.d("Activity_VideoPlayer:initializePlayer()", sMessage);
                                            }*/
                                                MediaItem mediaItem = MediaItem.fromUri(uriTest);
                                                lMediaItems.add(mediaItem);
                                            }
                                        }

                                    }
                                } catch (Exception e) {
                                    sMessage = "" + e.getMessage();
                                    Log.d("Activity_VideoPlayer:initializePlayer()", sMessage);
                                }


                                gExoPlayer.setMediaItems(lMediaItems, false);
                                gExoPlayer.prepare();
                                gExoPlayer.setPlayWhenReady(true);

                            } else { //End if GlobalClass.gbOptionIndividualizeM3U8VideoSegmentPlayback
                                //If the option to individualize M3U8 video segment playback is not selected,
                                //  play an SAF-adapted M3U8 file. That is, a file with video listings
                                //  of Android Storage Access Framework Uris.

                                MediaItem mediaItem = null;
                                //todo: Create a flow chart to map this logic.

                                //Check to see if the SAF-aligned M3U8 file exists, and if not, create it.
                                //todo: what to do if the user has transferred files to another device? This file will need to be recreated.
                                // todo: Perhaps check to see if the file exists. If it exists, test the first valid file URI link and see if it is good.
                                // todo:   if not, recreate the file.
                                String[] sFileNameAndExtension = ci.sFilename.split("\\.");
                                if(sFileNameAndExtension.length != 2){
                                    sMessage = "Problem with filename: " + ci.sFilename;
                                    Log.d("Activity_VideoPlayer:initializePlayer", sMessage);
                                    return;
                                }
                                //Determine the name of the SAF-adapted M3U8 file:
                                String sM3U8_SAF_FileName = sFileNameAndExtension[0] + "_SAF_Adapted." + sFileNameAndExtension[1];

                                //Check to see indexing of the files for the app is complete. If not, return.
                                //If the global file indexing is complete, use fast-lookup:
                                String sUriM3U8_SAF = GlobalClass.gsUriAppRootPrefix
                                        + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                                        + GlobalClass.gsFileSeparator + ci.sFolder_Name
                                        + GlobalClass.gsFileSeparator + ci.sItemID
                                        + GlobalClass.gsFileSeparator + sM3U8_SAF_FileName;
                                Uri uriM3U8_SAF = Uri.parse(sUriM3U8_SAF);

                                boolean bM3U8_SAF_File_Exists = false;
                                try{
                                    InputStream is = GlobalClass.gcrContentResolver.openInputStream(uriM3U8_SAF);
                                    if(is != null){
                                        bM3U8_SAF_File_Exists = true;
                                        is.close();
                                    }
                                } catch (Exception e){
                                    LogThis("initializePlayer()", "Could not find SAF-adapted M3U8 file. This is a handled condition, non-error.", null);
                                }

                                if(!bM3U8_SAF_File_Exists){
                                    //The SAF-adapted M3U8 file does not exist. Create it.
                                    try {
                                        //DocumentsContract.createDocument requires a Uri to the parent folder. Find the Uri:
                                        String sParentUri = GlobalClass.gsUriAppRootPrefix
                                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                                                + GlobalClass.gsFileSeparator + ci.sFolder_Name
                                                + GlobalClass.gsFileSeparator + ci.sItemID;
                                        Uri uriParent = Uri.parse(sParentUri);
                                        //With the parent folder Uri identified, create the M3U8_SAF text file at that location:
                                        uriM3U8_SAF = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriParent, MimeTypes.BASE_TYPE_TEXT, sM3U8_SAF_FileName);
                                        if(uriM3U8_SAF == null){
                                            sMessage = "Problem creating updated M3U8 file with SAF paths with parent uri " + sParentUri;
                                            Log.d("Activity_VideoPlayer:initializePlayer", sMessage);
                                            return;
                                        }

                                        //With the new file created but empty, copy over the contents of the existing M3U8 file but replace the video segment files
                                        //  with the SAF Uri strings:
                                        InputStream isM3U8 = GlobalClass.gcrContentResolver.openInputStream(uriM3U8);
                                        OutputStream osM3U8New = GlobalClass.gcrContentResolver.openOutputStream(uriM3U8_SAF);
                                        if(osM3U8New == null){
                                            return;
                                        }

                                        if (isM3U8 != null) {
                                            byte[] byteM3U8File = isM3U8.readAllBytes();
                                            isM3U8.close();
                                            String sM3U8File = new String(byteM3U8File);
                                            String[] sM3U8FileRecords = sM3U8File.split("\n");
                                            StringBuilder sbM3U8New = new StringBuilder();
                                            String sLine;
                                            for (String sM3U8FileRecord : sM3U8FileRecords) {
                                                sLine = sM3U8FileRecord;
                                                if (!sM3U8FileRecord.startsWith("#") && sM3U8FileRecord.endsWith("st")) {
                                                    //sLine should have a file name.
                                                    String sVideoSegmentUri = GlobalClass.gsUriAppRootPrefix
                                                            + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                                                            + GlobalClass.gsFileSeparator + ci.sFolder_Name
                                                            + GlobalClass.gsFileSeparator + ci.sItemID
                                                            + GlobalClass.gsFileSeparator + sLine;
                                                    sLine = sVideoSegmentUri;
                                                }
                                                sbM3U8New.append(sLine);
                                                sbM3U8New.append("\n");
                                            }

                                            String sData = sbM3U8New.toString();
                                            osM3U8New.write(sData.getBytes(StandardCharsets.UTF_8));
                                            osM3U8New.flush();
                                            osM3U8New.close();
                                        }
                                    } catch (Exception e) {
                                        sMessage = "Problem opening InputStream to M3U8 file: " + e.getMessage();
                                        Log.d("Activity_VideoPlayer:initializePlayer", sMessage);
                                    }
                                } //End if the M3U8_SAF file does not exist.

                                //The Uri for the M3U8_SAF file should now be defined.
                                if(uriM3U8_SAF != null) {
                                    mediaItem = MediaItem.fromUri(uriM3U8_SAF);
                                    gExoPlayer.setMediaItem(mediaItem);
                                    gExoPlayer.prepare();
                                    gExoPlayer.setPlayWhenReady(true);
                                } else {
                                    sMessage = "Something went wrong while loading the M3U8 file.";
                                    Log.d("Activity_VideoPlayer:initializePlayer", sMessage);
                                }


                                //File indexing should be complete or we would have exited.
                                //M3U8_SAF file should now exist and be defined.

                            } //End if !gbOptionIndividualizeM3U8VideoSegmentPlayback


                        } else {
                            gbPlayingM3U8 = false;
                            gImageView_GifViewer.setVisibility(View.INVISIBLE);
                            gplayerView_ExoVideoPlayer.setVisibility(View.INVISIBLE);
                            gPlayerControlView_ExoPlayerControls.setVisibility(View.INVISIBLE);
                            gVideoView_VideoPlayer.setVisibility(View.VISIBLE);
                            gVideoView_VideoPlayer.setVideoURI(dfMediaFile.getUri());

                            if (gVideoView_VideoPlayer.getDuration() > glCurrentVideoPosition) {
                                glCurrentVideoPosition = 1;
                            }
                            gVideoView_VideoPlayer.seekTo((int) glCurrentVideoPosition);
                            gVideoView_VideoPlayer.start();
                        }
                    }
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
                if(gbPlayingM3U8){
                    //gplayerView_ExoVideoPlayer.setUseController(true);
                } else {
                    if(active) {
                        gMediaController.show(); //This mediaController needs this merely for how the touches
                        // are handled between the ExoPlayer and the VideoView.
                    }
                }
            }
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            if(active) {
                hide();
            }
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

        if(gbPlayingM3U8){
            //gplayerView_ExoVideoPlayer.setUseController(false);
        }

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

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Activity_VideoPlayer:" + sRoutine, sMessage);
    }

}