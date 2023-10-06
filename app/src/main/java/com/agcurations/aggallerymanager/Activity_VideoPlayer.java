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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

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

    private boolean gbPlayingM3U8;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    @OptIn(markerClass = {UnstableApi.class, UnstableApi.class})
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

        globalClass = (GlobalClass) getApplicationContext();

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

        MediaController gMediaController = new MediaController(this);
        gMediaController.addOnUnhandledKeyEventListener((view, keyEvent) -> {
            //Handle BACK button
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                onBackPressed();
                return true;
            }
            return false;
        });



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

                //The other player does not trigger onSingleTapConfirmed without
                // this item true.
                return !gbPlayingM3U8;
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                toggle();
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
                toggle();
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

        gplayerView_ExoVideoPlayer.setPlayer(gExoPlayer);

        gplayerView_ExoVideoPlayer.setOnTouchListener((v, event) -> gdVideoView.onTouchEvent(event));

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

        
        if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            bControlsAreVisible = true;
            if (gButton_GetVideoFrameImage != null) {
                gButton_GetVideoFrameImage.setVisibility(View.VISIBLE);
            }
            if (gImageView_VideoFrameImage != null) {
                gImageView_VideoFrameImage.setVisibility(View.VISIBLE);
            }
        }

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
        String sMessage;

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
                    gFragment_itemDetails.setArguments(args);
                    fragmentTransaction.replace(R.id.fragment_Item_Details, gFragment_itemDetails);
                    fragmentTransaction.commit();
                } else {
                    gFragment_itemDetails.initData(ci);
                }

                //Create a time stamp for "last viewed" and update the catalog record and record in memory:
                ci.dDatetime_Last_Viewed_by_User = GlobalClass.GetTimeStampDouble();

                Double dTimeStamp = GlobalClass.GetTimeStampDouble();
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
                WorkManager.getInstance(getApplicationContext()).enqueue(otwrCatalogUpdateItem);


                //Determine if this is a gif file, which the VideoView will not play:
                boolean bFileIsGif = GlobalClass.JumbleFileName(sFileName).contains(".gif");

                String sFileUri = GlobalClass.gsUriAppRootPrefix
                        + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[ci.iMediaCategory]
                        + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                        + GlobalClass.gsFileSeparator + ci.sFilename;
                Uri uriFileUri = Uri.parse(sFileUri);

                if (bFileIsGif || (GlobalClass.giSelectedCatalogMediaCategory != GlobalClass.MEDIA_CATEGORY_VIDEOS)) {
                    //This will cause the load of gif image or other images.
                    gbPlayingM3U8 = false;

                    Glide.with(getApplicationContext()).load(uriFileUri).into(gImageView_GifViewer);

                    if (!gImageView_GifViewer.isShown()) {
                        gImageView_GifViewer.setVisibility(View.VISIBLE);
                        gplayerView_ExoVideoPlayer.setVisibility(View.INVISIBLE);
                    }
                } else if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                    gbPlayingM3U8 = true;
                    gImageView_GifViewer.setVisibility(View.INVISIBLE);
                    gplayerView_ExoVideoPlayer.setVisibility(View.VISIBLE);

                    //Find the M3U8 file:
                    Uri uriM3U8;

                    String sM3U8Uri = GlobalClass.gsUriAppRootPrefix
                            + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                            + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                            + GlobalClass.gsFileSeparator + ci.sFilename;

                    uriM3U8 = Uri.parse(sM3U8Uri);



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
                        String sParentFolderUri = GlobalClass.gsUriAppRootPrefix
                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                                + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                                + GlobalClass.gsFileSeparator + sNewFileName;
                        Uri uriParentFolderUri = Uri.parse(sParentFolderUri);

                        //Read the M3U8 file and sequence the transport stream entries:
                        TreeMap<Integer, String> tmFileSequence = new TreeMap<>();

                        try {
                            Uri uriNewM3U8 = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriParentFolderUri, GlobalClass.BASE_TYPE_TEXT, sNewFileName);
                            if(uriNewM3U8 == null){
                                LogThis("inintializePlayer", "Could not create new M3U8 file.", null);
                                return;
                            }
                            InputStream isM3U8 = GlobalClass.gcrContentResolver.openInputStream(uriM3U8);
                            OutputStream osM3U8New = GlobalClass.gcrContentResolver.openOutputStream(uriNewM3U8);
                            if (osM3U8New == null) {
                                LogThis("inintializePlayer", "Could not open output stream to new M3U8 file.", null);
                                return;
                            }

                            if (isM3U8 != null) {
                                BufferedReader brM3U8 = new BufferedReader(new InputStreamReader(isM3U8));
                                StringBuilder sbM3U8New = new StringBuilder();
                                String sLine = brM3U8.readLine();
                                StringBuilder sbUriString = new StringBuilder();
                                while (sLine != null) {
                                    //#EXTINF:<DURATION> [<KEY>="<VALUE>"]*,<TITLE>
                                    //Uri string
                                    if (!sLine.startsWith("#") && sLine.endsWith("st")) {
                                        //Form the uri string:
                                        sbUriString.append(GlobalClass.gsUriAppRootPrefix)
                                                .append(GlobalClass.gsFileSeparator).append(GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS])
                                                .append(GlobalClass.gsFileSeparator).append(ci.sFolderRelativePath)

                                                .append(GlobalClass.gsFileSeparator).append(sLine);
                                        sLine = sbUriString.toString();
                                        sbUriString.setLength(0);
                                        tmFileSequence.put(iSequence, sLine);
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
                                String sSegmentFileUri = tmFileSequence.get(i);
                                if (sSegmentFileUri != null) {
                                    Uri uriSegmentFile = Uri.parse(sSegmentFileUri);
                                    MediaItem mediaItem = MediaItem.fromUri(uriSegmentFile);
                                    lMediaItems.add(mediaItem);

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

                        MediaItem mediaItem;
                        //todo: Create a flow chart to map this logic.

                        //Check to see if the SAF-aligned M3U8 file exists, and if not, create it.
                        //todo: what to do if the user has transferred files to another device? This file will need to be recreated.
                        // todo: Perhaps check to see if the file exists. If it exists, test the first valid file URI link and see if it is good.
                        // todo:   if not, recreate the file.

                        if(!ci.sFilename.contains(".")){
                            sMessage = "Problem with filename '" + ci.sFilename + "': cannot determine extension.";
                            Log.d("Activity_VideoPlayer:initializePlayer", sMessage);
                            return;
                        }

                        String sFileNameBase = ci.sFilename.substring(0, ci.sFilename.lastIndexOf("."));
                        String sFileNameExt = ci.sFilename.substring(ci.sFilename.lastIndexOf(".") + 1);

                        //Determine the name of the SAF-adapted M3U8 file:
                        String sM3U8_SAF_FileName = sFileNameBase + "_SAF_Adapted" + "." + sFileNameExt;

                        //Check to see indexing of the files for the app is complete. If not, return.
                        //If the global file indexing is complete, use fast-lookup:
                        String sUriM3U8_SAF = GlobalClass.gsUriAppRootPrefix
                                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                                + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                                + GlobalClass.gsFileSeparator + sM3U8_SAF_FileName;
                        Uri uriM3U8_SAF = Uri.parse(sUriM3U8_SAF);

                        boolean bM3U8_SAF_File_Exists = false;
                        try{
                            InputStream isM3U8 = GlobalClass.gcrContentResolver.openInputStream(uriM3U8_SAF);
                            if(isM3U8 != null){
                                bM3U8_SAF_File_Exists = true;
                                //Read-in one path to make sure it is accurate:
                                //byte[] byteM3U8File = isM3U8.readAllBytes();
                                byte[] byteM3U8File = GlobalClass.readAllBytes(isM3U8);
                                isM3U8.close();
                                String sM3U8File = new String(byteM3U8File);
                                String[] sM3U8FileRecords = sM3U8File.split("\n");
                                StringBuilder sbM3U8New = new StringBuilder();
                                String sLine;
                                for (String sM3U8FileRecord : sM3U8FileRecords) {
                                    sLine = sM3U8FileRecord;
                                    if (!sM3U8FileRecord.startsWith("#") && sM3U8FileRecord.endsWith("st")) {
                                        //sLine should now have a Uri path name.
                                        Uri uriTest = Uri.parse(sLine);
                                        try {
                                            InputStream isTest = GlobalClass.gcrContentResolver.openInputStream(uriTest);
                                            if(isTest != null){
                                                isTest.close();
                                            }
                                        } catch (Exception e){
                                            //If the segment file in the M3U8 file does not exist, delete the adapted M3U8 file so that it can be re-written.
                                            isM3U8.close();
                                            if(!DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriM3U8_SAF)){
                                                LogThis("initializePlayer()", "Android Storage Access Framework-adapted M3U8 file contained a \n" +
                                                        "reference to a segment file whose Uri does not point to a file. The adapted M3U8 file must be re-written\n" +
                                                        "but it could not be deleted.", null);
                                            } else {
                                                bM3U8_SAF_File_Exists = false;
                                            }
                                            break;
                                        }

                                    }
                                    sbM3U8New.append(sLine);
                                    sbM3U8New.append("\n");
                                }
                                isM3U8.close();
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
                                        + GlobalClass.gsFileSeparator + ci.sFolderRelativePath;
                                Uri uriParent = Uri.parse(sParentUri);
                                //With the parent folder Uri identified, create the M3U8_SAF text file at that location:
                                uriM3U8_SAF = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriParent, GlobalClass.BASE_TYPE_TEXT, sM3U8_SAF_FileName);
                                if(uriM3U8_SAF == null){
                                    sMessage = "Problem creating updated M3U8 file with SAF paths with parent uri " + sParentUri;
                                    Log.d("Activity_VideoPlayer:initializePlayer", sMessage);
                                    return;
                                }

                                ArrayList<String> alsExistingFileNames = GlobalClass.GetDirectoryFileNames(uriParent);

                                //With the new file created but empty, copy over the contents of the existing M3U8 file but replace the video segment files
                                //  with the SAF Uri strings:
                                InputStream isM3U8 = GlobalClass.gcrContentResolver.openInputStream(uriM3U8);
                                OutputStream osM3U8New = GlobalClass.gcrContentResolver.openOutputStream(uriM3U8_SAF);
                                if(osM3U8New == null){
                                    return;
                                }

                                if (isM3U8 != null) {
                                    //byte[] byteM3U8File = isM3U8.readAllBytes();
                                    byte[] byteM3U8File = GlobalClass.readAllBytes(isM3U8);
                                    isM3U8.close();
                                    String sM3U8File = new String(byteM3U8File);
                                    String[] sM3U8FileRecords = sM3U8File.split("\n");
                                    StringBuilder sbM3U8New = new StringBuilder();
                                    String sLine;
                                    boolean bMissingRecords = false;
                                    for (String sM3U8FileRecord : sM3U8FileRecords) {
                                        sLine = sM3U8FileRecord;
                                        //todo: make sure all of the video segment files exist or the video player will not play at all.
                                        if (!sM3U8FileRecord.startsWith("#") && sM3U8FileRecord.endsWith("st")) {
                                            //sLine should have a file name.
                                            if(alsExistingFileNames.contains(sLine)) {
                                                sLine = GlobalClass.gsUriAppRootPrefix
                                                        + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[GlobalClass.MEDIA_CATEGORY_VIDEOS]
                                                        + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                                                        + GlobalClass.gsFileSeparator + sLine;
                                            } else {
                                                sLine = "#Missing file: " + sLine;
                                                bMissingRecords = true;
                                            }
                                        }
                                        sbM3U8New.append(sLine);
                                        sbM3U8New.append("\n");
                                    }

                                    String sData = sbM3U8New.toString();
                                    osM3U8New.write(sData.getBytes(StandardCharsets.UTF_8));
                                    osM3U8New.flush();
                                    osM3U8New.close();
                                    if(bMissingRecords){
                                        Toast.makeText(getApplicationContext(), "This video file appears to be missing video segment files. Perhaps retry download.", Toast.LENGTH_SHORT).show();
                                        ci.iAllVideoSegmentFilesDetected = ItemClass_CatalogItem.VIDEO_SEGMENT_FILES_KNOWN_INCOMPLETE;
                                        globalClass.CatalogDataFile_UpdateRecord(ci); //Write the status to the database.
                                    }
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
                    //Last case is if this is a non-M3U8 video.
                    gbPlayingM3U8 = false;
                    gImageView_GifViewer.setVisibility(View.INVISIBLE);
                    gplayerView_ExoVideoPlayer.setVisibility(View.VISIBLE);
                    MediaItem mediaItem = MediaItem.fromUri(uriFileUri);
                    gExoPlayer.setMediaItem(mediaItem);
                    gExoPlayer.prepare();
                    //gExoPlayer.setPlayWhenReady(true);

                    gExoPlayer.seekTo(glCurrentVideoPosition);
                    if (giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING) {
                        gExoPlayer.setPlayWhenReady(true);
                    }
                    gExoPlayer.pause();
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
                String sM3U8_SAF_FileName = sFileNameBase + "_SAF_Adapted" + "." + sFileNameExt;
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

    private boolean bControlsAreVisible;

    private void toggle() {
        if (bControlsAreVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI
        bControlsAreVisible = false;

        if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            if (gButton_GetVideoFrameImage != null) {
                gButton_GetVideoFrameImage.setVisibility(View.INVISIBLE);
            }
            if (gImageView_VideoFrameImage != null) {
                gImageView_VideoFrameImage.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void show() {
        bControlsAreVisible = true;

        if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            if (gButton_GetVideoFrameImage != null) {
                gButton_GetVideoFrameImage.setVisibility(View.VISIBLE);
            }
            if (gImageView_VideoFrameImage != null) {
                gImageView_VideoFrameImage.setVisibility(View.VISIBLE);
            }
        }

    }


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