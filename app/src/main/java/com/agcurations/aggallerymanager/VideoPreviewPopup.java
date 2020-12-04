package com.agcurations.aggallerymanager;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import androidx.annotation.Nullable;

public class VideoPreviewPopup extends Activity {
    public static final String FILE_URI_STRING = "FILE_URI_STRING";
    public static final String FILE_ITEM = "FILE_ITEM";
    public static final String VIDEO_FILE_DURATION_MILLISECONDS_LONG = "VIDEO_FILE_DURATION_MILLISECONDS_LONG";
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Source: https://www.youtube.com/watch?v=fn5OlqQuOCk
        setContentView(R.layout.video_preview_popup);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        float fSize = 0.5f;

        getWindow().setLayout((int)(width * fSize), (int)(height * fSize));

        Bundle b = getIntent().getExtras();

        if(b != null) {
            
            
            String sUriVideoFile = b.getString(FILE_URI_STRING);
            long lVideoDuration = b.getLong(VIDEO_FILE_DURATION_MILLISECONDS_LONG,0);

            if(lVideoDuration < 0L){
                //If there is no video length, exit this activity.
                finish();
            }

            int iLengthOfLoopInSeconds = 10;
            int iImageChangeFrequencyHz = 1;
            int iFrames = iLengthOfLoopInSeconds * iImageChangeFrequencyHz;
            long lSampleFrequencyMicroSeconds = (lVideoDuration * 1000) / iFrames;

            // 2 Hz = 2x/sec = 500 ms. 1/2 = .5; .5 * 1000 = 500.
            //10 Hz = 10/sec = 100 ms. 1/10 = .1; .1 * 1000 = 100.
            int iFrameDurationMilliseconds = (int)(1.0 / iImageChangeFrequencyHz * 1000);


            Bitmap[] bitmapFrames = new Bitmap[iFrames];

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            // Set data source to retriever.
            Uri uriVideoFile = Uri.parse(sUriVideoFile);
            retriever.setDataSource(getApplicationContext(), uriVideoFile);

            //Get frame bitmaps:
            for(int i = 0; i < iFrames; i++) {
                // Get a frame in Bitmap by specifying time.
                // Be aware that the parameter must be in "microseconds", not milliseconds.
                bitmapFrames[i] = retriever.getFrameAtTime(i * lSampleFrequencyMicroSeconds);
            }
            retriever.release();

            //Create the animation:
            AnimationDrawable animationDrawable = new AnimationDrawable();
            for(int i = 0; i < iFrames; i++) {
                Drawable d = new BitmapDrawable(getResources(), bitmapFrames[i]);
                animationDrawable.addFrame(d, iFrameDurationMilliseconds);
            }
            ImageView imageView_Video = findViewById(R.id.imageView_Video);
            imageView_Video.setImageDrawable(animationDrawable);
            animationDrawable.setOneShot(false);
            animationDrawable.start();

        }
    }

}
