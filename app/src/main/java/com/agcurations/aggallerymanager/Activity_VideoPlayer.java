package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class Activity_VideoPlayer extends AppCompatActivity {

    private TreeMap<Integer, ItemClass_CatalogItem> treeMapRecyclerViewCatItems;
    private Integer giKey;

    private long glCurrentVideoPosition = 1;
    private final int VIDEO_PLAYBACK_STATE_PAUSED = 0;
    private final int VIDEO_PLAYBACK_STATE_PLAYING = 1;
    private int giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
    private static final String PLAYBACK_TIME = "play_time";

    private DrawerLayout gDrawerLayout;
    private ImageView gImageView_GifViewer;

    TreeMap<Integer, ItemClass_M3U8_TS_Entry> gtmM3U8_TS_File_Sequence; //Not used as of Sept. 29, 2023, but leaving in place for potential video-editor or other utility.
    ImageView gImageView_VideoFrameImage;
    Button gButton_GetVideoFrameImage;
    Bitmap gBitmapVideoFrame = null;
    public static final String COPY_PIXEL_VIDEO_PLAYER_FRAME_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.COPY_PIXEL_VIDEO_PLAYER_FRAME_RESPONSE";

    //ExoPlayer is used for playback of local M3U8 files:
    private ExoPlayer gExoPlayer;
    private PlayerView gplayerView_ExoVideoPlayer;

    private Fragment_ItemDetails gFragment_itemDetails;

    private boolean gbAutoHideControlsDueToSwipe = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    @OptIn(markerClass = {UnstableApi.class, UnstableApi.class}) //todo: remove
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        //Create a response receiver to listen for catalog file writes, etc, to display progress to user.
        gProgressResponseReceiver = new ProgressResponseReceiver();
        gProgressResponseReceiver.progressBar_ProgressIndicator = findViewById(R.id.progressBar_Progress);
        gProgressResponseReceiver.textView_ProgressText = findViewById(R.id.textView_ProgressBarText);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);
        filter.addAction(GlobalClass.BROADCAST_WRITE_CATALOG_FILE);
        filter.addAction(COPY_PIXEL_VIDEO_PLAYER_FRAME_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(gProgressResponseReceiver, filter);


        gplayerView_ExoVideoPlayer = findViewById(R.id.playerView_ExoVideoPlayer);
        gImageView_GifViewer = findViewById(R.id.imageView_GifViewer);
        gDrawerLayout = findViewById(R.id.drawer_layout);
        gDrawerLayout.openDrawer(GravityCompat.START); //Start the drawer open so that the user knows it's there.
        //Configure a runnable to close the drawer after a timeout.
        gDrawerLayout.postDelayed(this::closeDrawer, 1500);

        //Get the treeMap and the key identifying the treeMap data to use for the first video to show:
        Intent intentVideoPlayer = this.getIntent();
        treeMapRecyclerViewCatItems = GlobalClass.gtmCatalogViewerDisplayTreeMap;

        String sVideoID = intentVideoPlayer.getStringExtra(Activity_CatalogViewer.RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_ID);

        //Get the TreeMap key associated with the Video ID provided:
        giKey = 0;
        for (Map.Entry<Integer, ItemClass_CatalogItem>
                entry : treeMapRecyclerViewCatItems.entrySet()) {
            if(entry.getValue().sItemID.equals(sVideoID)) {
                giKey = entry.getKey();
            }
        }

        if (savedInstanceState != null) {
            glCurrentVideoPosition = savedInstanceState.getLong(PLAYBACK_TIME);
        }


        //Set a touch listener to the VideoView so that the user can pause video and obfuscate with a
        //  double-tap, as well as swipe to go to the next video:
        final GestureDetector gdVideoView = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){

            //Here is the method for double tap
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                super.onLongPress(e);
            }

            @Override
            public boolean onDoubleTapEvent(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return super.onDown(e);
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                return super.onSingleTapConfirmed(e);
            }

            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
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
                if(!gplayerView_ExoVideoPlayer.isControllerFullyVisible()) {
                    gbAutoHideControlsDueToSwipe = true; //The player will automatically show the controls. Automatically hide them again.
                }
                if(treeMapRecyclerViewCatItems.containsKey(iTempKey)) {
                    StopPlayback();
                    giKey--;
                    glCurrentVideoPosition = 1; //If moving to another item, set play position to start.
                    initializePlayer();
                }
            }

            public void onSwipeLeft() {
                int iTempKey = giKey + 1;
                if(!gplayerView_ExoVideoPlayer.isControllerFullyVisible()) {
                    gbAutoHideControlsDueToSwipe = true; //The player will automatically show the controls. Automatically hide them again.
                }
                if(treeMapRecyclerViewCatItems.containsKey(iTempKey)) {
                    StopPlayback();
                    giKey++;
                    glCurrentVideoPosition = 1; //If moving to another item, set play position to start.
                    initializePlayer();
                }
            }

            public void onSwipeUp() {

            }

            public void onSwipeDown() {
            }

        });


        //Prepare the Gif image viewer to accept swipe to go to next or previous file:
        //Set a touch listener to the VideoView so that the user can pause video and obfuscate with a
        //  double-tap, as well as swipe to go to the next video:
        final GestureDetector gdImageView = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){

            //Here is the method for double tap
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                super.onLongPress(e);
            }

            @Override
            public boolean onDoubleTapEvent(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                return super.onSingleTapConfirmed(e);
            }

            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
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

        });

        gImageView_GifViewer.setOnTouchListener((v, event) -> gdImageView.onTouchEvent(event));

        //Create the ExoPlayer.
        gExoPlayer = new ExoPlayer.Builder(this).build();

        gExoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);

        gExoPlayer.addListener(new Player.Listener() {

            @Override
            public void onEvents(@NonNull Player player,@NonNull  Player.Events events) {
                Player.Listener.super.onEvents(player, events);
                if(!player.isLoading()){
                    int i = player.getPlaybackState();
                    if(i == Player.STATE_READY && gbAutoHideControlsDueToSwipe){
                        gplayerView_ExoVideoPlayer.hideController();
                        gbAutoHideControlsDueToSwipe = false;
                    }
                }

            }
        });

        gplayerView_ExoVideoPlayer.setPlayer(gExoPlayer);

        gplayerView_ExoVideoPlayer.setOnTouchListener((v, event) -> gdVideoView.onTouchEvent(event));

        gplayerView_ExoVideoPlayer.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            if(visibility == PlayerView.INVISIBLE ||
                visibility == PlayerView.GONE){
                //This is here to hide these particular views because there is a timeout on the controls that needs to be caught.
                gButton_GetVideoFrameImage.setVisibility(View.INVISIBLE);
                gImageView_VideoFrameImage.setVisibility(View.INVISIBLE);
            } else if (visibility == PlayerView.VISIBLE) {
                gButton_GetVideoFrameImage.setVisibility(View.VISIBLE);
                gImageView_VideoFrameImage.setVisibility(View.VISIBLE);
            }


        });

        gImageView_VideoFrameImage = findViewById(R.id.imageView_VideoFrameImage);
        gButton_GetVideoFrameImage = findViewById(R.id.button_GetVideoFrameImage);
        gButton_GetVideoFrameImage.setOnClickListener(v -> {
            SurfaceView surfaceView = (SurfaceView) gplayerView_ExoVideoPlayer.getVideoSurfaceView();

            if(surfaceView != null) {
                final Bitmap bitmap = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);
                PixelCopy.request(surfaceView, bitmap, (int result) -> {
                    if (result != PixelCopy.SUCCESS) {
                        return;
                    }
                    gBitmapVideoFrame = bitmap;
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(COPY_PIXEL_VIDEO_PLAYER_FRAME_ACTION_RESPONSE);
                    broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    broadcastIntent.putExtra(ProgressResponseReceiver.EXTRA_VIDEO_FRAME_SCREENSHOT_UPDATE, true);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
                }, new Handler(Looper.getMainLooper()));
            }

        });

        //Hide the system bar:
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Configure the behavior of the hidden system bars.
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars());
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());

        initializePlayer();
    }

    @Override
    public void onDestroy() {
        if(getApplicationContext() != null) {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(gProgressResponseReceiver);
        }
        super.onDestroy();
    }

    private void PausePlayback(){

        glCurrentVideoPosition = gExoPlayer.getCurrentPosition();
        if (gExoPlayer.isPlaying()) {
            giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
        } else {
            giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
        }
        gExoPlayer.pause();

    }

    private void StopPlayback(){

        glCurrentVideoPosition = gExoPlayer.getCurrentPosition();
        if (gExoPlayer.isPlaying()) {
            giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
        } else {
            giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
        }
        gExoPlayer.stop();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            gExoPlayer.seekTo(glCurrentVideoPosition);
            if (giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING) {
                gExoPlayer.setPlayWhenReady(true);
            }
            gExoPlayer.pause();
        }

    }

    @Override
    protected void onPause() {
        if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            PausePlayback();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            StopPlayback();
        }

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            glCurrentVideoPosition = gExoPlayer.getCurrentPosition();
            outState.putLong(PLAYBACK_TIME, glCurrentVideoPosition);
        }

    }

    //==============================================================================================
    //  Video-affecting routines
    //==============================================================================================

    private void initializePlayer() {

        gtmM3U8_TS_File_Sequence = null;

        if(treeMapRecyclerViewCatItems.containsKey(giKey)) {
            ItemClass_CatalogItem ci;
            ci = treeMapRecyclerViewCatItems.get(giKey);
            if (ci != null) {
                String sFileName = ci.sFilename;

                if(ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8){
                    setTitle(sFileName); //Filename of the m3u8 file will not be jumbled in order to allow the media player to understand what it's receiving.
                } else {
                    setTitle(GlobalClass.JumbleFileName(sFileName));
                }

                //Populate the item details fragment:
                if(gFragment_itemDetails == null) {
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    gFragment_itemDetails = new Fragment_ItemDetails();

                    Bundle args = new Bundle();
                    args.putSerializable(Fragment_ItemDetails.CATALOG_ITEM, ci); //NOTE!!!!! ci passed here gets marshalled as a reference, not a copy.
                    //Read more here: https://stackoverflow.com/questions/44698863/bundle-putserializable-serializing-reference-not-value
                    args.putBoolean(Fragment_ItemDetails.HISTOGRAM_FREEZE, true); //Don't xref histogram data as the user selects tags - the user is assigning tags, not filtering on xref.
                    gFragment_itemDetails.setArguments(args);
                    fragmentTransaction.replace(R.id.fragment_Item_Details, gFragment_itemDetails);
                    fragmentTransaction.commit();
                } else {
                    gFragment_itemDetails.initData(ci);
                }

                //Create a time stamp for "last viewed" and update the catalog record and record in memory:
                ci.dDatetime_Last_Viewed_by_User = GlobalClass.GetTimeStampDouble();

                /*Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                String sCatalogRecord = GlobalClass.getCatalogRecordString(ci);
                Data dataCatalogUpdateItem = new Data.Builder()
                        .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_VideoPlayer:initializePlayer()")
                        .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                        .putString(GlobalClass.EXTRA_CATALOG_ITEM, sCatalogRecord)
                        .build();
                OneTimeWorkRequest otwrCatalogUpdateItem = new OneTimeWorkRequest.Builder(Worker_Catalog_UpdateItem.class)
                        .setInputData(dataCatalogUpdateItem)
                        .addTag(Worker_Catalog_UpdateItem.TAG_WORKER_CATALOG_UPDATEITEM) //To allow finding the worker later.
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueue(otwrCatalogUpdateItem);*/
                //Above: No longer update the catalog file with last read data. This data to be housed in a different file.

                //Determine if this is a gif file, which the VideoView will not play:
                boolean bFileIsGif = GlobalClass.JumbleFileName(sFileName).contains(".gif");

                String sFileUri = GlobalClass.gsUriAppRootPrefix
                        + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[ci.iMediaCategory]
                        + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                        + GlobalClass.gsFileSeparator + ci.sFilename;
                Uri uriFileUri = Uri.parse(sFileUri);

                if (bFileIsGif || (GlobalClass.giSelectedCatalogMediaCategory != GlobalClass.MEDIA_CATEGORY_VIDEOS)) {
                    //This will cause the load of gif image or other images.

                    Glide.with(getApplicationContext()).load(uriFileUri).into(gImageView_GifViewer);

                    if (!gImageView_GifViewer.isShown()) {
                        gImageView_GifViewer.setVisibility(View.VISIBLE);
                        gplayerView_ExoVideoPlayer.setVisibility(View.INVISIBLE);
                    }
                } else if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                    gImageView_GifViewer.setVisibility(View.INVISIBLE);
                    gplayerView_ExoVideoPlayer.setVisibility(View.VISIBLE);

                    String sUriM3U8_Filename = GlobalClass.gsUriAppRootPrefix
                            + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                            + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                            + GlobalClass.gsFileSeparator + ci.sFilename;
                    Uri uriM3U8_File = Uri.parse(sUriM3U8_Filename);

                    MediaItem mediaItem = MediaItem.fromUri(uriM3U8_File);
                    gExoPlayer.setMediaItem(mediaItem);
                    gExoPlayer.prepare();
                    if (giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING) {
                        gExoPlayer.setPlayWhenReady(true);
                    }

                    /*todo: Read the M3U8 file and confirm that the text is using an up-to-date path
                      aligned with the current storage*/

                } else {
                    //Last case is if this is a non-M3U8 video.
                    gImageView_GifViewer.setVisibility(View.INVISIBLE);
                    gplayerView_ExoVideoPlayer.setVisibility(View.VISIBLE);
                    MediaItem mediaItem = MediaItem.fromUri(uriFileUri);
                    gExoPlayer.setMediaItem(mediaItem);
                    gExoPlayer.prepare();

                    if(glCurrentVideoPosition != 1){
                        gExoPlayer.seekTo(glCurrentVideoPosition);
                    }
                    if (giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING) {
                        gExoPlayer.setPlayWhenReady(true);
                    }
                }

            } //End if our catalog item is not null.

        } //End if the catalog item was found in the passed-in list.

    }

    private Uri getM3U8CurrentTSFile(){
        long lContentPosition = gExoPlayer.getContentPosition(); //returns milliseconds.
        Log.d("Video frame calc", "Current position is: " + lContentPosition + " ms.");
        ItemClass_CatalogItem ci;
        ci = treeMapRecyclerViewCatItems.get(giKey);

        Uri uriMediaUri = null;
        if(ci != null) {
            if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                //Get the m3u8 file and determine which transport stream (TS) file is relevant to the
                //  current playback time.
                if (gtmM3U8_TS_File_Sequence == null) {
                    createM3U8Sequencing();
                }
                if (gtmM3U8_TS_File_Sequence != null) {
                    if (gtmM3U8_TS_File_Sequence.size() > 0) {
                        //Determine the TS file at the content position:
                        long lCummulativeDuration = 0L;
                        ItemClass_M3U8_TS_Entry itemClass_m3U8_ts_entry_last = gtmM3U8_TS_File_Sequence.get(0);
                        if (itemClass_m3U8_ts_entry_last != null) {
                            long lNextCummulativeDuration = (long) (itemClass_m3U8_ts_entry_last.fDuration * 1000f);
                            for (int i = 1; i < gtmM3U8_TS_File_Sequence.size(); i++) {
                                if (lNextCummulativeDuration >= lContentPosition) {
                                    uriMediaUri = Uri.parse(itemClass_m3U8_ts_entry_last.sUri);
                                    Log.d("Video frame calc", "Transport Stream File " + i + ", " + itemClass_m3U8_ts_entry_last.sUri);
                                    break;
                                }
                                lCummulativeDuration = lNextCummulativeDuration;
                                itemClass_m3U8_ts_entry_last = gtmM3U8_TS_File_Sequence.get(i);
                                if (itemClass_m3U8_ts_entry_last != null) {
                                    lNextCummulativeDuration += (long) (itemClass_m3U8_ts_entry_last.fDuration * 1000L);
                                }
                            }
                        }
                        lContentPosition -= lCummulativeDuration;
                        Log.d("Video frame calc", "Content position: " + lContentPosition + "ms.");
                    }

                }
            } else {
                String sFileUri = GlobalClass.gsUriAppRootPrefix
                        + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[ci.iMediaCategory]
                        + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                        + GlobalClass.gsFileSeparator + ci.sFilename;
                uriMediaUri = Uri.parse(sFileUri);
            }
        }
        return uriMediaUri;
    }

    private void createM3U8Sequencing(){
        //Read the M3U8 file and sequence the transport stream entries:
        gtmM3U8_TS_File_Sequence = new TreeMap<>();

        if(treeMapRecyclerViewCatItems.containsKey(giKey)) {
            ItemClass_CatalogItem ci;
            ci = treeMapRecyclerViewCatItems.get(giKey);
            if (ci != null) {
                String sFileNameBase = ci.sFilename.substring(0, ci.sFilename.lastIndexOf("."));
                String sFileNameExt = ci.sFilename.substring(ci.sFilename.lastIndexOf(".") + 1);
                //Determine the name of the SAF-adapted M3U8 file:
                String sM3U8_SAF_FileName = sFileNameBase + GlobalClass.gsSAF_Adapted_M3U8_Suffix + "." + sFileNameExt;
                String sUriM3U8 = GlobalClass.gsUriAppRootPrefix
                        + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                        + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                        + GlobalClass.gsFileSeparator + sM3U8_SAF_FileName;
                Uri uriM3U8 = Uri.parse(sUriM3U8);

                if(GlobalClass.CheckIfFileExists(uriM3U8)){
                    try {

                        InputStream isM3U8 = GlobalClass.gcrContentResolver.openInputStream(uriM3U8);
                        if(isM3U8 != null) {
                            byte[] byteM3U8File = GlobalClass.readAllBytes(isM3U8);
                            isM3U8.close();
                            String sM3U8File = new String(byteM3U8File);
                            String[] sM3U8FileRecords = sM3U8File.split("\n");

                            float fTargetDuration = -1;
                            int iSequence = 0;
                            ItemClass_M3U8_TS_Entry itemClass_m3U8_ts_entry = null;
                            for (String sLine : sM3U8FileRecords) {

                                if(sLine.startsWith("#EXT-X-TARGETDURATION:")){
                                    //Target duration
                                    String sTemp = sLine.replace("#EXT-X-TARGETDURATION:", "");
                                    sTemp = sTemp.trim();
                                    try{
                                        fTargetDuration = Float.parseFloat(sTemp);
                                    } catch (Exception ignored){
                                        //Could not determine target duration.
                                    }

                                } else if (sLine.startsWith("#EXTINF:")) {
                                    //#EXTINF:<DURATION> [<KEY>="<VALUE>"]*,<TITLE>
                                    //Uri string
                                    //Get transport stream (TS) metadata:
                                    itemClass_m3U8_ts_entry = new ItemClass_M3U8_TS_Entry();
                                    String sMetadata = sLine;
                                    sMetadata = sMetadata.replace("#EXTINF:","");
                                    sMetadata = sMetadata.trim();
                                    String[] sTemp = sMetadata.split(" ");
                                    if(sTemp.length > 0){
                                        String sTemp2 = sTemp[0].replace(",","");
                                        String sDuration = sTemp2.trim();
                                        float fDuration;
                                        try{
                                            fDuration = Float.parseFloat(sDuration);
                                            itemClass_m3U8_ts_entry.fDuration = fDuration;
                                        } catch (Exception e){
                                            //Could not determine duration from metadata. Opportunity to use default duration if stated at the top of the file.
                                            itemClass_m3U8_ts_entry.fDuration = fTargetDuration;
                                        }
                                    }

                                } else if (!sLine.startsWith("#") && sLine.endsWith("st")) {
                                    if(itemClass_m3U8_ts_entry != null) {
                                        itemClass_m3U8_ts_entry.sUri = sLine;
                                        gtmM3U8_TS_File_Sequence.put(iSequence, itemClass_m3U8_ts_entry);
                                        iSequence++;
                                        itemClass_m3U8_ts_entry = null;
                                    }
                                } //End if for types of lines in the M3U8 file.

                            } //End loop processing data from teh M3U8 file.

                        } //End if the M3U8 file was found.

                    } catch (Exception e){
                        String sMessage = "" + e.getMessage();
                        Log.d("M3U8 File Processing", sMessage);
                    }

                }
            }
        }

    } //End createM3U8Sequencing().

    public void closeDrawer(){
        gDrawerLayout.closeDrawer(GravityCompat.START);
    }

    //==============================================================================================
    //==============================================================================================
    //==============================================================================================

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Activity_VideoPlayer:" + sRoutine, sMessage);
    }

    ProgressResponseReceiver gProgressResponseReceiver;

    public class ProgressResponseReceiver extends BroadcastReceiver {

        public static final String EXTRA_VIDEO_FRAME_SCREENSHOT_UPDATE  = "com.agcurations.aggallerymanager.extra.FRAME_SCREENSHOT_UPDATE";

        public ProgressBar progressBar_ProgressIndicator;
        public TextView textView_ProgressText;

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {
                String sStatusMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_STATUS_MESSAGE);
                if(sStatusMessage != null){
                    Toast.makeText(context, sStatusMessage, Toast.LENGTH_LONG).show();
                }
            }

            //Check to see if this is a response to update log or progress bar:
            boolean 	bUpdatePercentComplete;
            boolean 	bUpdateProgressBarText;

            //Get booleans from the intent telling us what to update:
            bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
            bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

            if(progressBar_ProgressIndicator != null && textView_ProgressText != null) {
                if (bUpdatePercentComplete) {
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if (progressBar_ProgressIndicator != null) {
                        progressBar_ProgressIndicator.setProgress(iAmountComplete);
                    }
                    assert progressBar_ProgressIndicator != null;
                    if (iAmountComplete == 100) {
                        progressBar_ProgressIndicator.setVisibility(View.INVISIBLE);
                        textView_ProgressText.setVisibility(View.INVISIBLE);
                    } else {
                        progressBar_ProgressIndicator.setVisibility(View.VISIBLE);
                        textView_ProgressText.setVisibility(View.VISIBLE);
                    }

                }
                if (bUpdateProgressBarText) {
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if (textView_ProgressText != null) {
                        textView_ProgressText.setText(sProgressBarText);
                    }
                }
            }

            boolean bUpdateVideoFrameScreenshot = intent.getBooleanExtra(EXTRA_VIDEO_FRAME_SCREENSHOT_UPDATE,false);
            if(bUpdateVideoFrameScreenshot) {
                if(gImageView_VideoFrameImage != null) {
                    if (gBitmapVideoFrame != null) {
                        gImageView_VideoFrameImage.setImageBitmap(gBitmapVideoFrame);
                        float fHWRatio = gBitmapVideoFrame.getHeight() / (float) gBitmapVideoFrame.getWidth();
                        int iVideoFrameImageWidth = gImageView_VideoFrameImage.getLayoutParams().width;
                        gImageView_VideoFrameImage.getLayoutParams().height = (int) (iVideoFrameImageWidth * fHWRatio);
                        gImageView_VideoFrameImage.requestLayout();

                        //Save the Bitmap as a png file:
                        ItemClass_CatalogItem ci = treeMapRecyclerViewCatItems.get(giKey);
                        if (ci != null) {
                            String sThumbnailFilename;
                            if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                                //If this is an m3u8 video style catalog item, configure the path to the file to use as the thumbnail.
                                sThumbnailFilename = GlobalClass.JumbleFileName("Thumbnail.png");
                            } else {
                                sThumbnailFilename = "lianbmuhT_" + ci.sFilename.substring(0, ci.sFilename.lastIndexOf("."))
                                        + ".gnp";
                            }
                            String sThumbnailFolder = GlobalClass.gsUriAppRootPrefix
                                            + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[ci.iMediaCategory]
                                            + GlobalClass.gsFileSeparator + ci.sFolderRelativePath;

                            try {
                                Uri uriNewThumbnailFile = Uri.parse(sThumbnailFolder
                                        + GlobalClass.gsFileSeparator + sThumbnailFilename);
                                if(!GlobalClass.CheckIfFileExists(uriNewThumbnailFile)) {
                                    //If the file does not exist, create it:
                                    Uri uriThumbnailFolder = Uri.parse(sThumbnailFolder);
                                    uriNewThumbnailFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriThumbnailFolder, GlobalClass.BASE_TYPE_TEXT, sThumbnailFilename);
                                }
                                if(uriNewThumbnailFile != null) {
                                    OutputStream osNewThumbnailFile;
                                    osNewThumbnailFile = GlobalClass.gcrContentResolver.openOutputStream(uriNewThumbnailFile, "wt"); //Mode wt = write and truncate. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
                                    if (osNewThumbnailFile != null) {
                                        //Write the data to the file:
                                        gBitmapVideoFrame.compress(Bitmap.CompressFormat.PNG, 100, osNewThumbnailFile); // PNG is a lossless format, the compression factor (100) is ignored
                                        osNewThumbnailFile.close();
                                        ci.sThumbnail_File = sThumbnailFilename;
                                        GlobalClass.gsRefreshCatalogViewerThumbnail = ci.sItemID; //Used to cause thumbnail file refresh.
                                        //Update in true memory, not just the copy passed to this activity:
                                        ItemClass_CatalogItem icci = GlobalClass.gtmCatalogLists.get(ci.iMediaCategory).get(ci.sItemID);
                                        if(icci != null){
                                            icci.sThumbnail_File = sThumbnailFilename;
                                        }
                                        //Update the catalog file:
                                        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                                        String sCatalogRecord = GlobalClass.getCatalogRecordString(ci);
                                        Data dataCatalogUpdateItem = new Data.Builder()
                                                .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_VideoPlayer:ProgressResponseReceiver()")
                                                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                                                .putString(GlobalClass.EXTRA_CATALOG_ITEM, sCatalogRecord)
                                                .build();
                                        OneTimeWorkRequest otwrCatalogUpdateItem = new OneTimeWorkRequest.Builder(Worker_Catalog_UpdateItem.class)
                                                .setInputData(dataCatalogUpdateItem)
                                                .addTag(Worker_Catalog_UpdateItem.TAG_WORKER_CATALOG_UPDATEITEM) //To allow finding the worker later.
                                                .build();
                                        WorkManager.getInstance(getApplicationContext()).enqueue(otwrCatalogUpdateItem);


                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }

            }

        }
    }

}