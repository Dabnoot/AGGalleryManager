package com.agcurations.aggallerymanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class VideoPreviewPopup_wTags extends AppCompatActivity {
    public static final String FILE_URI_STRING = "FILE_URI_STRING";
    public static final String VIDEO_FILE_DURATION_MILLISECONDS_LONG = "VIDEO_FILE_DURATION_MILLISECONDS_LONG";

    public static final String TAG_SELECTION_RESULT_BUNDLE = "TAG_SELECTION_RESULT_BUNDLE";
    public static final String TAG_SELECTION_TAG_IDS = "TAG_SELECTION_TAG_IDS";

    private FragmentSelectTagsViewModel mViewModel;

    private String gsUriVideoFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getSupportActionBar().hide();

        //Source: https://www.youtube.com/watch?v=fn5OlqQuOCk
        setContentView(R.layout.video_preview_popup_wtags);

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        mViewModel = new ViewModelProvider(this).get(FragmentSelectTagsViewModel.class);
        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<TagItem>> selectedTagsObserver = new Observer<ArrayList<TagItem>>() {
            @Override
            public void onChanged(ArrayList<TagItem> tagItems) {

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
                for(TagItem ti : tagItems){
                    aliTagIDs.add(ti.TagID);
                }

                //Prepare a result to send back to the calling activity:
                Intent data = new Intent();
                Bundle b = new Bundle();
                //Put back the file URI string so that the file can be located:
                b.putCharSequence(FILE_URI_STRING, gsUriVideoFile);
                b.putIntegerArrayList(TAG_SELECTION_TAG_IDS, aliTagIDs);
                data.putExtra(TAG_SELECTION_RESULT_BUNDLE, b);
                setResult(RESULT_OK, data);

            }
        };
        mViewModel.alTagsSelected.observe(this, selectedTagsObserver);

        //Start the tag selection fragment:
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment_SelectTags fst = new Fragment_SelectTags();
        Bundle args = new Bundle();
        args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, ImportActivity.giImportMediaCategory);
        fst.setArguments(args);
        ft.replace(R.id.child_fragment_tag_selector, fst);
        ft.commit();


        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        float fSize = 0.8f;

        getWindow().setLayout((int)(width * fSize), (int)(height * fSize));

        Bundle b = getIntent().getExtras();

        if(b != null) {

            //Get the video URI:
            gsUriVideoFile = b.getString(FILE_URI_STRING);
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
            Uri uriVideoFile = Uri.parse(gsUriVideoFile);
            retriever.setDataSource(ImportActivity.getContextOfActivity(), uriVideoFile);

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
