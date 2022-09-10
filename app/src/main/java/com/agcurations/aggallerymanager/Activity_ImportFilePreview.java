package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class Activity_ImportFilePreview extends AppCompatActivity {

    private ArrayList<ItemClass_File> galFileItems;
    private int giFileItemIndex;
    private int giFileItemLastIndex;    //Used to automatically move the user to the next item if they
                                        //  hit the 'mark for deletion' checkbox.
    private int giMaxFileItemIndex;
    private static final String IMAGE_PREVIEW_INDEX = "image_preview_index";

    private Fragment_SelectTags fragment_selectTags; //Used to reset tags when swiping to the next file.

    private int giMediaCategory;

    VideoView gVideoView_VideoPlayer;
    MediaController gMediaController;

    private ImageView gImagePreview;

    private int giCurrentVideoPosition = 1;
    private final int VIDEO_PLAYBACK_STATE_PAUSED = 0;
    private final int VIDEO_PLAYBACK_STATE_PLAYING = 1;
    private int giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
    private static final String PLAYBACK_TIME = "play_time";

    int[] giGradeImageViews;

    ArrayList<Integer> galiLastAssignedTags;
    boolean gbFreezeLastAssignedReset = false;
    boolean gbPastingTags = false;

    @SuppressLint("ClickableViewAccessibility") //For the onTouch for the imageView.
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(this.getSupportActionBar() != null) {
            this.getSupportActionBar().hide();
        }

        //Source: https://www.youtube.com/watch?v=fn5OlqQuOCk
        setContentView(R.layout.activity_import_file_preview);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        if (savedInstanceState != null) {
            giFileItemIndex = savedInstanceState.getInt(IMAGE_PREVIEW_INDEX);
            giFileItemLastIndex = giFileItemIndex;
            giCurrentVideoPosition = savedInstanceState.getInt(PLAYBACK_TIME);
        }

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        ViewModel_Fragment_SelectTags viewModel_fragment_selectTags = new ViewModelProvider(this).get(ViewModel_Fragment_SelectTags.class);
        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<ItemClass_Tag>> selectedTagsObserver = new Observer<ArrayList<ItemClass_Tag>>() {
            @Override
            public void onChanged(ArrayList<ItemClass_Tag> tagItems) {

                //Get the text of the tags and display:
                StringBuilder sb = new StringBuilder();
                sb.append("Tags: ");
                if (tagItems.size() > 0) {
                    sb.append(tagItems.get(0).sTagText);
                    for (int i = 1; i < tagItems.size(); i++) {
                        sb.append(", ");
                        sb.append(tagItems.get(i).sTagText);
                    }
                }
                TextView textView_SelectedTags = findViewById(R.id.textView_SelectedTags);
                if (textView_SelectedTags != null) {
                    textView_SelectedTags.setText(sb.toString());
                }

                //Get the tag IDs to pass back to the calling activity:
                ArrayList<Integer> aliTagIDs = new ArrayList<>();
                for (ItemClass_Tag ti : tagItems) {
                    aliTagIDs.add(ti.iTagID);
                }

                boolean bSetCheckedDisplay = false;
                //If the media type is Comics, tags are applied to each
                //  file item.
                if (giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                    boolean bSetChecked = aliTagIDs.size() > galFileItems.get(0).aliProspectiveTags.size();
                    for (ItemClass_File icf : galFileItems) {
                        icf.aliProspectiveTags = aliTagIDs;
                        icf.bDataUpdateFlag = true;
                        if (bSetChecked) {
                            icf.bIsChecked = true;  //Only set if a tag has been added.
                            icf.bMarkedForDeletion = false;
                            bSetCheckedDisplay = true;
                        }
                    }

                } else {
                    if (aliTagIDs.size() > galFileItems.get(giFileItemIndex).aliProspectiveTags.size()) {
                        galFileItems.get(giFileItemIndex).bIsChecked = true; //Only set if a tag has been added.
                        galFileItems.get(giFileItemIndex).bMarkedForDeletion = false;
                        bSetCheckedDisplay = true;
                    }
                    galFileItems.get(giFileItemIndex).aliProspectiveTags = aliTagIDs;
                    galFileItems.get(giFileItemIndex).bDataUpdateFlag = true;

                    if (!gbFreezeLastAssignedReset) {
                        galiLastAssignedTags = new ArrayList<>(aliTagIDs);
                    } else {
                        //Data protection in place due to initialization.
                        gbFreezeLastAssignedReset = false; //Unfreeze data protection.
                    }

                }

                if (bSetCheckedDisplay) {
                    CheckBox checkBox_ImportItem = findViewById(R.id.checkBox_ImportItem);
                    checkBox_ImportItem.setChecked(true);
                    CheckboxImportColorSwitch(true);
                    CheckBox checkBox_MarkForDeletion = findViewById(R.id.checkBox_MarkForDeletion);
                    checkBox_MarkForDeletion.setChecked(false);
                    CheckboxMarkForDeletionColorSwitch(false);
                }

                if (gbPastingTags) {
                    gbPastingTags = false;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //If this is the result of a tag pasting operation, automatically move to the next/previous item.
                            //Do this with a slight delay to allow the graphics to update so that the user can see
                            //  that the tag selections were applied.
                            if (giFileItemIndex > giFileItemLastIndex) {
                                iterateToGreaterIndexedItem();
                            } else if (giFileItemIndex < giFileItemLastIndex) {
                                iterateToLesserIndexedItem();
                            }
                        }
                    }, 500);


                }

                //Set a result to send back to the calling activity (this is also done on checkbox click):
                setResult(RESULT_OK);

            }
        };
        viewModel_fragment_selectTags.altiTagsSelected.observe(this, selectedTagsObserver);



        Bundle b = getIntent().getExtras();
        if(b != null) {
            GlobalClass globalClass = (GlobalClass) getApplicationContext(); 
            galFileItems = globalClass.galPreviewFileList;
            giMaxFileItemIndex = galFileItems.size() - 1;
            giFileItemIndex = b.getInt(Activity_Import.PREVIEW_FILE_ITEMS_POSITION, 0);
            giFileItemLastIndex = giFileItemIndex;
            giMediaCategory = b.getInt(Activity_Import.MEDIA_CATEGORY, 0);

            //Start the tag selection fragment:
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            fragment_selectTags = new Fragment_SelectTags();
            Bundle args = new Bundle();
            args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, giMediaCategory);
            args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, galFileItems.get(giFileItemIndex).aliProspectiveTags);

            fragment_selectTags.setArguments(args);
            ft.replace(R.id.child_fragment_tag_selector, fragment_selectTags);
            ft.commit();

            //Init the tags list if there are tags already assigned to this item:
            //Get the text of the tags and display:
            if(galFileItems.get(giFileItemIndex).aliProspectiveTags != null) {
                if (galFileItems.get(giFileItemIndex).aliProspectiveTags.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Tags: ");
                    sb.append(globalClass.getTagTextFromID(galFileItems.get(giFileItemIndex).aliProspectiveTags.get(0), GlobalClass.MEDIA_CATEGORY_VIDEOS));
                    for (int i = 1; i < galFileItems.get(giFileItemIndex).aliProspectiveTags.size(); i++) {
                        sb.append(", ");
                        sb.append(globalClass.getTagTextFromID(galFileItems.get(giFileItemIndex).aliProspectiveTags.get(i), GlobalClass.MEDIA_CATEGORY_VIDEOS));
                    }
                    TextView textView_SelectedTags = findViewById(R.id.textView_SelectedTags);
                    if (textView_SelectedTags != null) {
                        textView_SelectedTags.setText(sb.toString());
                    }
                }
            }

            gVideoView_VideoPlayer = findViewById(R.id.videoView_VideoPlayerPreview);
            gImagePreview = findViewById(R.id.imageView_ImagePreview);

            //Prepare a touch listener accept swipe to go to next or previous file:
            final GestureDetector gestureDetector_SwipeForNextFile = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){


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
                    float fsx1 = gImagePreview.getX();
                    float fsx2 = gImagePreview.getWidth();
                    float fXMidPoint = fsx1 + (fsx2 / 2f);
                    float fTouchDeadband = fsx2 * .10f;
                    float fNavigateBackTapXLocation = fXMidPoint - fTouchDeadband;
                    float fNavigateNextTapXLocation = fXMidPoint + fTouchDeadband;
                    float fTapXLocation = e.getRawX();
                    if(fTapXLocation < fNavigateBackTapXLocation){
                        iterateToLesserIndexedItem();
                    } else if (fTapXLocation > fNavigateNextTapXLocation){
                        iterateToGreaterIndexedItem();
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
                    iterateToLesserIndexedItem();
                }

                public void onSwipeLeft() {
                    iterateToGreaterIndexedItem();
                }

            /*public void onSwipeTop() {
            }*/

            /*public void onSwipeBottom() {
            }*/

            });


            if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                long lVideoDuration = galFileItems.get(giFileItemIndex).lVideoTimeInMilliseconds;
                if (lVideoDuration < 0L) {
                    //If there is no video length, exit this activity.
                    finish();
                }
                gVideoView_VideoPlayer.bringToFront();

                //Configure the media controller:
                gMediaController = new MediaController(this);
                gMediaController.setMediaPlayer(gVideoView_VideoPlayer);
                gVideoView_VideoPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        gMediaController.setAnchorView(gVideoView_VideoPlayer);
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
                gVideoView_VideoPlayer.setMediaController(gMediaController);

                gVideoView_VideoPlayer.setOnTouchListener(new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                            if (!gMediaController.isShowing()) {
                                gMediaController.show(5000);
                            }
                        }
                        return gestureDetector_SwipeForNextFile.onTouchEvent(event);
                    }

                });


            } else {

                gImagePreview.bringToFront();
                gImagePreview.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return gestureDetector_SwipeForNextFile.onTouchEvent(event);
                    }
                });
            }

        }

        //Set on-click listener for grade:
        /*giGradeImageViews = new int[]{
                R.id.imageView_Grade1,
                R.id.imageView_Grade2,
                R.id.imageView_Grade3,
                R.id.imageView_Grade4,
                R.id.imageView_Grade5};
        ImageView[] imageView_GradeArray = new ImageView[5];
        boolean bGradeIVsOK = true;
        for(int i = 0; i < giGradeImageViews.length; i++){
            imageView_GradeArray[i] = findViewById(giGradeImageViews[i]);
            if(imageView_GradeArray[i] == null){
                bGradeIVsOK = false;
            }
        }
        if (bGradeIVsOK){
            for(int i = 0; i < giGradeImageViews.length; i++) {
                imageView_GradeArray[i].setOnClickListener(new gradeOnClickListener(i + 1));
            }
        }*/

        initializeFile();
        //displayGrade();

    }

    private void iterateToLesserIndexedItem(){
        int iTempKey = giFileItemIndex - 1;

        iTempKey = Math.max(0, iTempKey);
        if(iTempKey != giFileItemIndex) {
            giFileItemLastIndex = giFileItemIndex;
            giFileItemIndex = iTempKey;
            initializeFile();
        }
    }
    private void iterateToGreaterIndexedItem(){
        int iTempKey = giFileItemIndex + 1;

        iTempKey = Math.min(giMaxFileItemIndex, iTempKey);
        if(iTempKey != giFileItemIndex) {
            giFileItemLastIndex = giFileItemIndex;
            giFileItemIndex = iTempKey;
            initializeFile();
        }
    }


    private void initializeFile(){
        if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {

            if(galFileItems.get(giFileItemIndex).iTypeFileFolderURL == ItemClass_File.TYPE_FILE) {
                Uri uriVideoFile = Uri.parse(galFileItems.get(giFileItemIndex).sUri);
                gVideoView_VideoPlayer.setVideoURI(uriVideoFile);
            } else if (galFileItems.get(giFileItemIndex).iTypeFileFolderURL == ItemClass_File.TYPE_URL) {
                gVideoView_VideoPlayer.setVideoPath(galFileItems.get(giFileItemIndex).sURLVideoLink);
            }
            // Skipping to 1 shows the first frame of the video.
            gVideoView_VideoPlayer.seekTo(1);
            giCurrentVideoPosition = 1;
            gVideoView_VideoPlayer.start();

            giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
        } else {
            if( !galFileItems.get(giFileItemIndex).sUri.equals("")) {
                Glide.with(getApplicationContext()).load(galFileItems.get(giFileItemIndex).sUri).into(gImagePreview);
            } else {
                Glide.with(getApplicationContext()).load(galFileItems.get(giFileItemIndex).sURL).into(gImagePreview);
            }
        }

        final CheckBox checkBox_ImportItem = findViewById(R.id.checkBox_ImportItem);
        final CheckBox checkBox_MarkForDeletion = findViewById(R.id.checkBox_MarkForDeletion);

        checkBox_ImportItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                galFileItems.get(giFileItemIndex).bIsChecked = ((CheckBox)view).isChecked();
                galFileItems.get(giFileItemIndex).bDataUpdateFlag = true;
                CheckboxImportColorSwitch(galFileItems.get(giFileItemIndex).bIsChecked);

                if(galFileItems.get(giFileItemIndex).bIsChecked && galFileItems.get(giFileItemIndex).bMarkedForDeletion){
                    galFileItems.get(giFileItemIndex).bMarkedForDeletion = false;
                    checkBox_MarkForDeletion.setChecked(false);
                    CheckboxMarkForDeletionColorSwitch(false);
                }

                //Update result to send back to the calling activity (this is also done on tag change):
                setResult(RESULT_OK);
            }
        });
        TextView textView_LabelImport = findViewById(R.id.textView_LabelImport);
        textView_LabelImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check/uncheck the checkbox.
                checkBox_ImportItem.setChecked(!checkBox_ImportItem.isChecked());
                galFileItems.get(giFileItemIndex).bIsChecked = checkBox_ImportItem.isChecked();
                galFileItems.get(giFileItemIndex).bDataUpdateFlag = true;
                CheckboxImportColorSwitch(galFileItems.get(giFileItemIndex).bIsChecked);

                if(galFileItems.get(giFileItemIndex).bIsChecked && galFileItems.get(giFileItemIndex).bMarkedForDeletion){
                    galFileItems.get(giFileItemIndex).bMarkedForDeletion = false;
                    checkBox_MarkForDeletion.setChecked(false);
                    CheckboxMarkForDeletionColorSwitch(false);
                }

                //Update result to send back to the calling activity (this is also done on tag change):
                setResult(RESULT_OK);
            }
        });
        LinearLayout linearLayout_ImportIndication = findViewById(R.id.linearLayout_ImportIndication);
        linearLayout_ImportIndication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check/uncheck the checkbox.
                checkBox_ImportItem.setChecked(!checkBox_ImportItem.isChecked());
                galFileItems.get(giFileItemIndex).bIsChecked = checkBox_ImportItem.isChecked();
                galFileItems.get(giFileItemIndex).bDataUpdateFlag = true;
                CheckboxImportColorSwitch(galFileItems.get(giFileItemIndex).bIsChecked);

                if(galFileItems.get(giFileItemIndex).bIsChecked && galFileItems.get(giFileItemIndex).bMarkedForDeletion){
                    galFileItems.get(giFileItemIndex).bMarkedForDeletion = false;
                    checkBox_MarkForDeletion.setChecked(false);
                    CheckboxMarkForDeletionColorSwitch(false);
                    //todo: tighten repeat coding.
                }

                //Update result to send back to the calling activity (this is also done on tag change):
                setResult(RESULT_OK);
            }
        });

        checkBox_ImportItem.setChecked(galFileItems.get(giFileItemIndex).bIsChecked);
        CheckboxImportColorSwitch(galFileItems.get(giFileItemIndex).bIsChecked);


        checkBox_MarkForDeletion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                galFileItems.get(giFileItemIndex).bMarkedForDeletion = ((CheckBox)view).isChecked();
                CheckboxMarkForDeletionColorSwitch(galFileItems.get(giFileItemIndex).bMarkedForDeletion);
                galFileItems.get(giFileItemIndex).bDataUpdateFlag = true;

                if(galFileItems.get(giFileItemIndex).bIsChecked && galFileItems.get(giFileItemIndex).bMarkedForDeletion){
                    galFileItems.get(giFileItemIndex).bIsChecked = false;
                    checkBox_ImportItem.setChecked(false);
                    CheckboxImportColorSwitch(false);
                }

                if(((CheckBox)view).isChecked()){
                    //If the user has marked this item for deletion, move to the next item automatically.
                    if(giFileItemIndex > giFileItemLastIndex){
                        iterateToGreaterIndexedItem();
                    } else if(giFileItemIndex < giFileItemLastIndex) {
                        iterateToLesserIndexedItem();
                    }
                }

                //Update result to send back to the calling activity (this is also done on tag change):
                setResult(RESULT_OK);
            }
        });
        TextView textView_LabelMarkForDeletion = findViewById(R.id.textView_LabelMarkForDeletion);
        textView_LabelMarkForDeletion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check/uncheck the checkbox.
                checkBox_MarkForDeletion.setChecked(!checkBox_MarkForDeletion.isChecked());
                galFileItems.get(giFileItemIndex).bMarkedForDeletion = checkBox_MarkForDeletion.isChecked();
                galFileItems.get(giFileItemIndex).bDataUpdateFlag = true;
                CheckboxMarkForDeletionColorSwitch(galFileItems.get(giFileItemIndex).bMarkedForDeletion);

                if(galFileItems.get(giFileItemIndex).bIsChecked && galFileItems.get(giFileItemIndex).bMarkedForDeletion){
                    galFileItems.get(giFileItemIndex).bIsChecked = false;
                    checkBox_ImportItem.setChecked(false);
                    CheckboxImportColorSwitch(false);
                }

                if(checkBox_MarkForDeletion.isChecked()){
                    //If the user has marked this item for deletion, move to the next item automatically.
                    if(giFileItemIndex > giFileItemLastIndex){
                        iterateToGreaterIndexedItem();
                    } else if(giFileItemIndex < giFileItemLastIndex) {
                        iterateToLesserIndexedItem();
                    }
                }

                //Update result to send back to the calling activity (this is also done on tag change):
                setResult(RESULT_OK);
            }
        });

        checkBox_MarkForDeletion.setChecked(galFileItems.get(giFileItemIndex).bMarkedForDeletion);
        CheckboxMarkForDeletionColorSwitch(galFileItems.get(giFileItemIndex).bMarkedForDeletion);

        TextView textView_FileName = findViewById(R.id.textView_FileName);
        String sFileNameTextLine = galFileItems.get(giFileItemIndex).sFileOrFolderName;
        if(!galFileItems.get(giFileItemIndex).sHeight.equals("")){ //Add resolution data to display if available:
            sFileNameTextLine = sFileNameTextLine + "\n" + galFileItems.get(giFileItemIndex).sWidth + "x" + galFileItems.get(giFileItemIndex).sHeight;
        }
        if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES){
            //If category is images, include megapixels.
            try {
                double dWidth = Double.parseDouble(galFileItems.get(giFileItemIndex).sWidth);
                double dHeight = Double.parseDouble(galFileItems.get(giFileItemIndex).sHeight);
                double dMegapixels = (dWidth * dHeight) / 1048576; //2^20 pixels per megapixel.
                sFileNameTextLine = sFileNameTextLine + " " + String.format(Locale.getDefault(), "%.1f", dMegapixels) + "MP";
            } catch (Exception e){
                //Do nothing. Just a textual ommision.
            }
        }
        textView_FileName.setText(sFileNameTextLine);

        //Init the tags list if there are tags already assigned to this item:
        //Get the text of the tags and display:
        if(galFileItems.get(giFileItemIndex).aliProspectiveTags != null) {
            TextView textView_SelectedTags = findViewById(R.id.textView_SelectedTags);
            StringBuilder sbTags = new StringBuilder();
            sbTags.append("Tags: ");

            int iFileItemTagsIndex = 0; //If the media type is Comics, tags are applied to the first
            //  file item only.
            if(giMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS){
                iFileItemTagsIndex = giFileItemIndex;
            }

            if (galFileItems.get(iFileItemTagsIndex).aliProspectiveTags.size() > 0) {

                GlobalClass globalClass;
                globalClass = (GlobalClass) getApplicationContext();

                ArrayList<Integer> aliConfirmedProspectiveTags = new ArrayList<>(); //Confirm all tags exist as user may have deleted a tag.
                for(Integer iTagID: galFileItems.get(iFileItemTagsIndex).aliProspectiveTags){
                    if(globalClass.TagIDExists(iTagID, giMediaCategory)){
                        aliConfirmedProspectiveTags.add(iTagID);
                    }
                }
                galFileItems.get(iFileItemTagsIndex).aliProspectiveTags = aliConfirmedProspectiveTags;

                //Update the Tag text listing on the preview display:
                sbTags.append(globalClass.getTagTextFromID(galFileItems.get(iFileItemTagsIndex).aliProspectiveTags.get(0), giMediaCategory));
                for (int i = 1; i < galFileItems.get(iFileItemTagsIndex).aliProspectiveTags.size(); i++) {
                    sbTags.append(", ");
                    sbTags.append(globalClass.getTagTextFromID(galFileItems.get(iFileItemTagsIndex).aliProspectiveTags.get(i), giMediaCategory));
                }

            }
            if (textView_SelectedTags != null) {
                textView_SelectedTags.setText(sbTags.toString());
            }

            if(giMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS) { //Don't worry about resetting if it's a comic. Tags are same for every page.
                gbFreezeLastAssignedReset = true; //Don't let the data observer reset the "lastAssignedTags" arrayList.
                fragment_selectTags.resetTagListViewData(galFileItems.get(iFileItemTagsIndex).aliProspectiveTags);
            }

            //displayGrade(); //Update the displayed grade

            //Show the sequence number of this item:
            TextView textView_ImportItemNumberOfNumber = findViewById(R.id.textView_ImportItemNumberOfNumber);
            String sTemp = (giFileItemIndex + 1) + "/" + (giMaxFileItemIndex + 1);
            textView_ImportItemNumberOfNumber.setText(sTemp);
        }

        ImageButton imageButton_PasteLastTags = findViewById(R.id.imageButton_PasteLastTags);
        if (imageButton_PasteLastTags != null) {
            imageButton_PasteLastTags.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyLastTagSelection();
                }
            });


        }

        TextView textView_LabelCopyLastTagSelection = findViewById(R.id.textView_LabelCopyLastTagSelection);
        if(textView_LabelCopyLastTagSelection != null){
            textView_LabelCopyLastTagSelection.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyLastTagSelection();
                }
            });
        }

    }


    private void copyLastTagSelection(){
        if(galiLastAssignedTags != null){
            //If the user is pasting tags, set a flag to move to the next item automatically.
            gbPastingTags = true;
            fragment_selectTags.gListViewTagsAdapter.selectTagsByIDs(galiLastAssignedTags);
        }

    }



    private void CheckboxImportColorSwitch(boolean bChecked){
        LinearLayout linearLayout_ImportIndication = findViewById(R.id.linearLayout_ImportIndication);
        if(bChecked) {
            linearLayout_ImportIndication.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorActionBar));
        } else {
            linearLayout_ImportIndication.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlack));
        }

    }

    private void CheckboxMarkForDeletionColorSwitch(boolean bChecked){
        LinearLayout linearLayout_MarkForDeletion = findViewById(R.id.linearLayout_MarkForDeletion);
        if(bChecked) {
            linearLayout_MarkForDeletion.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorActionBar));
        } else {
            linearLayout_MarkForDeletion.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlack));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            gVideoView_VideoPlayer.seekTo(giCurrentVideoPosition);
            if (giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING) {
                gVideoView_VideoPlayer.start();
            }
        }
    }

    @Override
    protected void onPause() {
        if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
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
        super.onStop();
        if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            gVideoView_VideoPlayer.stopPlayback();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            outState.putInt(PLAYBACK_TIME, gVideoView_VideoPlayer.getCurrentPosition());
        }
        outState.putInt(IMAGE_PREVIEW_INDEX, giFileItemIndex);
    }

    private void displayGrade(){
        //Show the rating:
        ImageView[] imageView_GradeArray = new ImageView[5];
        boolean bGradeIVsOK = true;
        for(int i = 0; i < giGradeImageViews.length; i++){
            imageView_GradeArray[i] = findViewById(giGradeImageViews[i]);
            if(imageView_GradeArray[i] == null){
                bGradeIVsOK = false;
            }
        }
        if (bGradeIVsOK){
            Drawable drawable_SolidStar = ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_grade_white_18dp, null);
            Drawable drawable_EmptyStar = ResourcesCompat.getDrawable(getResources(), R.drawable.outline_grade_white_18dp, null);
            for(int i = 0; i < galFileItems.get(giFileItemIndex).iGrade; i++) {
                imageView_GradeArray[i].setImageDrawable(drawable_SolidStar);
            }
            for(int i = galFileItems.get(giFileItemIndex).iGrade; i < giGradeImageViews.length; i++) {
                imageView_GradeArray[i].setImageDrawable(drawable_EmptyStar);
            }
        }

    }

    private class gradeOnClickListener implements View.OnClickListener{

        int iGrade;

        public gradeOnClickListener(int iGrade){
            this.iGrade = iGrade;
        }

        @Override
        public void onClick(View view) {
            galFileItems.get(giFileItemIndex).iGrade = iGrade;
            displayGrade();
        }
    }


}
