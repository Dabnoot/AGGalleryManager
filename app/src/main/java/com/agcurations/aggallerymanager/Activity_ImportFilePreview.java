package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.google.android.material.slider.RangeSlider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class Activity_ImportFilePreview extends AppCompatActivity {

    private ArrayList<ItemClass_File> galFileItems;
    private int giFileItemIndex;
    private int giFileItemLastIndex;    //Used to automatically move the user to the next item if they
                                        //  hit the 'mark for deletion' checkbox.
    private int giMaxFileItemIndex;
    private static final String IMAGE_PREVIEW_INDEX = "image_preview_index";

    private Fragment_SelectTags gFragment_selectTags; //Used to reset tags when swiping to the next file.

    private int giMediaCategory;

    private boolean gbLookForFileAdjacencies = false;

    VideoView gVideoView_VideoPlayer;
    MediaController gMediaController;

    private ImageView gImagePreview;

    private int giCurrentVideoPosition = 1;
    private final int VIDEO_PLAYBACK_STATE_PAUSED = 0;
    private final int VIDEO_PLAYBACK_STATE_PLAYING = 1;
    private int giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PAUSED;
    private static final String PLAYBACK_TIME = "play_time";

    ArrayList<Integer> galiLastAssignedTags;
    boolean gbFreezeLastAssignedReset = false;
    boolean gbPastingTags = false;

    ImportFilePreviewResponseReceiver importFilePreviewResponseReceiver;
    RelativeLayout gRelativeLayout_Adjacency_Analysis_Progress;
    ProgressBar gProgressBar_AnalysisProgress;
    TextView gTextView_AnalysisProgressBarText;
    RelativeLayout gRelativeLayout_Adjacencies;
    RecyclerView gRecyclerView_Adjacencies;

    TextView gTextView_GroupID;
    ImageButton gImageButton_GroupIDNew;
    ImageButton gImageButton_GroupIDCopy;
    ImageButton gImageButton_GroupIDPaste;
    ImageButton gImageButton_GroupIDRemove;

    Button gButton_ShowAdjacencies;
    TextView gTextView_AdjacencyCount			;
    TextView gTextView_FileNameMatchCount		;
    TextView gTextView_DateModifiedMatchCount	;
    TextView gTextView_ResolutionMatchCount		;
    TextView gTextView_DurationMatchCount		;

    Context gContextWindow;

    @SuppressLint("ClickableViewAccessibility") //For the onTouch for the imageView.
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gContextWindow = this;

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
        final Observer<ArrayList<ItemClass_Tag>> selectedTagsObserver = tagItems -> {

            //Get the text of the tags and display:
            StringBuilder sb = new StringBuilder();
            sb.append("Tags: ");
            String sMaturityRatingText = "";
            if (tagItems.size() > 0) {
                sb.append(tagItems.get(0).sTagText);
                int iGreatestMaturityRating = GlobalClass.giDefaultUserMaturityRating;
                if(tagItems.get(0).iMaturityRating > iGreatestMaturityRating){
                    iGreatestMaturityRating = tagItems.get(0).iMaturityRating;
                }
                for (int i = 1; i < tagItems.size(); i++) {
                    sb.append(", ");
                    sb.append(tagItems.get(i).sTagText);
                    if(tagItems.get(i).iMaturityRating > iGreatestMaturityRating){
                        iGreatestMaturityRating = tagItems.get(i).iMaturityRating;
                    }
                }
                sMaturityRatingText += AdapterMaturityRatings.MATURITY_RATINGS[iGreatestMaturityRating][AdapterMaturityRatings.MATURITY_RATING_CODE_INDEX];
                sMaturityRatingText += " - ";
                String sMatRatDesc = AdapterMaturityRatings.MATURITY_RATINGS[iGreatestMaturityRating][AdapterMaturityRatings.MATURITY_RATING_NAME_INDEX];
                int iMaxTextLength = 75;
                sMaturityRatingText += sMatRatDesc.substring(0, Math.min(iMaxTextLength, sMatRatDesc.length()));
                if(iMaxTextLength < sMatRatDesc.length()) {
                    sMaturityRatingText += "...";
                }
            }
            TextView textView_SelectedTags = findViewById(R.id.textView_SelectedTags);
            if (textView_SelectedTags != null) {
                textView_SelectedTags.setText(sb.toString());
            }
            TextView textView_MaturityRating = findViewById(R.id.textView_MaturityRating);
            if(textView_MaturityRating != null) {
                textView_MaturityRating.setText(sMaturityRatingText);
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
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    //If this is the result of a tag pasting operation, automatically move to the next/previous item.
                    //Do this with a slight delay to allow the graphics to update so that the user can see
                    //  that the tag selections were applied.
                    if (giFileItemIndex > giFileItemLastIndex) {
                        iterateToGreaterIndexedItem();
                    } else if (giFileItemIndex < giFileItemLastIndex) {
                        iterateToLesserIndexedItem();
                    }
                }, 500);


            }

            updateAdjacencies();

            //Set a result to send back to the calling activity (this is also done on checkbox click):
            setResult(RESULT_OK);

        };
        viewModel_fragment_selectTags.altiTagsSelected.observe(this, selectedTagsObserver);

        gButton_ShowAdjacencies = findViewById(R.id.button_ShowAdjacencies);

        Bundle b = getIntent().getExtras();
        if(b != null) {
            GlobalClass globalClass = (GlobalClass) getApplicationContext(); 
            galFileItems = globalClass.galPreviewFileList;
            giMaxFileItemIndex = galFileItems.size() - 1;
            giFileItemIndex = b.getInt(Activity_Import.PREVIEW_FILE_ITEMS_POSITION, 0);
            giFileItemLastIndex = giFileItemIndex;
            giMediaCategory = b.getInt(Activity_Import.MEDIA_CATEGORY, 0);

            gbLookForFileAdjacencies = b.getBoolean(Activity_Import.IMPORT_ALIGN_ADJACENCIES, false);
            if(gbLookForFileAdjacencies){
                gButton_ShowAdjacencies.setEnabled(false);
            } else {
                gButton_ShowAdjacencies.setEnabled(true);
            }

            //Start the tag selection fragment:
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            gFragment_selectTags = new Fragment_SelectTags();
            Bundle args = new Bundle();
            args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, giMediaCategory);
            args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, galFileItems.get(giFileItemIndex).aliProspectiveTags);

            gFragment_selectTags.setArguments(args);
            ft.replace(R.id.child_fragment_tag_selector, gFragment_selectTags);
            ft.commit();

            gFragment_selectTags.gbHistogramFreeze = true; //Don't xref histogram data as the user selects tags - the user is assigning tags, not filtering on xref.

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
                    float fsx1 = gImagePreview.getX();
                    float fsx2 = gImagePreview.getWidth();
                    float fXMidPoint = fsx1 + (fsx2 / 2f);
                    float fTouchDeadband = fsx2 * .50f;
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


            RelativeLayout relativeLayout_VideoView = findViewById(R.id.relativeLayout_VideoView);
            relativeLayout_VideoView.setOnTouchListener((v, event) -> gestureDetector_SwipeForNextFile.onTouchEvent(event));


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
                gVideoView_VideoPlayer.setOnPreparedListener(mp -> gMediaController.setAnchorView(gVideoView_VideoPlayer));
                gMediaController.addOnUnhandledKeyEventListener((view, keyEvent) -> {
                    //Handle BACK button
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        onBackPressed();
                        return true;
                    }
                    return false;
                });
                gVideoView_VideoPlayer.setMediaController(gMediaController);

            } else {

                gImagePreview.bringToFront();

            }

        }


        //Add a response receiver to listen for responses from the adjacency analyzer worker.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_Adjaceny_Analyzer.CATALOG_ADJAN_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importFilePreviewResponseReceiver = new ImportFilePreviewResponseReceiver();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(importFilePreviewResponseReceiver, filter);

        gRelativeLayout_Adjacencies = findViewById(R.id.relativeLayout_Adjacencies);

        gTextView_AdjacencyCount			= findViewById(R.id.textView_AdjacencyCount			);
        gTextView_FileNameMatchCount		= findViewById(R.id.textView_FileNameMatchCount		);
        gTextView_DateModifiedMatchCount	= findViewById(R.id.textView_DateModifiedMatchCount	);
        gTextView_ResolutionMatchCount		= findViewById(R.id.textView_ResolutionMatchCount	);
        gTextView_DurationMatchCount		= findViewById(R.id.textView_DurationMatchCount		);

        gButton_ShowAdjacencies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gRelativeLayout_Adjacencies.setVisibility(View.VISIBLE);
                gbLookForFileAdjacencies = true;
                view.setEnabled(false);
                updateAdjacencies();
            }
        });

        ImageButton imageButton_CloseAdjacencies = findViewById(R.id.imageButton_CloseAdjacencies);
        imageButton_CloseAdjacencies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gRelativeLayout_Adjacencies.setVisibility(View.INVISIBLE);
                gbLookForFileAdjacencies = false;
                gButton_ShowAdjacencies.setEnabled(true);
            }
        });

        //Configure the maturity filter rangeslider:
        RangeSlider rangeSlider_MaturityFilter = findViewById(R.id.rangeSlider_MaturityFilter);
        //Set max available maturity to the max allowed to the user:
        if(GlobalClass.gicuCurrentUser != null) {
            rangeSlider_MaturityFilter.setValueTo((float) GlobalClass.gicuCurrentUser.iMaturityLevel);
        } else {
            rangeSlider_MaturityFilter.setValueTo((float) GlobalClass.giDefaultUserMaturityRating);
        }
        rangeSlider_MaturityFilter.setStepSize((float) 1);
        //Set the current selected maturity window max to the default maturity rating:
        rangeSlider_MaturityFilter.setValues((float) GlobalClass.giMinContentMaturityFilter, (float) GlobalClass.giMaxContentMaturityFilter);

        rangeSlider_MaturityFilter.setLabelFormatter(value -> AdapterMaturityRatings.MATURITY_RATINGS[(int)value][0] + " - " + AdapterMaturityRatings.MATURITY_RATINGS[(int)value][1]);
        rangeSlider_MaturityFilter.addOnChangeListener(new RangeSlider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                //Todo: Need to ensure that this routine only gets called when the user is done sliding.
                List<Float> lfSliderValues = slider.getValues();
                if(lfSliderValues.size() == 2){
                    int iMinTemp = lfSliderValues.get(0).intValue();
                    int iMaxTemp = lfSliderValues.get(1).intValue();
                    if(iMinTemp != GlobalClass.giMinContentMaturityFilter ||
                            iMaxTemp != GlobalClass.giMaxContentMaturityFilter) {
                        GlobalClass.giMinContentMaturityFilter = lfSliderValues.get(0).intValue();
                        GlobalClass.giMaxContentMaturityFilter = lfSliderValues.get(1).intValue();
                        gbLookForFileAdjacencies = true;
                        updateAdjacencies();
                    }
                }
            }
        });

        if(!gbLookForFileAdjacencies){
            gRelativeLayout_Adjacencies.setVisibility(View.INVISIBLE);
            gButton_ShowAdjacencies.setEnabled(true);
        }

        initializeFile();

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

        checkBox_ImportItem.setOnClickListener(view -> {
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
        });
        TextView textView_LabelImport = findViewById(R.id.textView_LabelImport);
        textView_LabelImport.setOnClickListener(view -> {
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
        });
        LinearLayout linearLayout_ImportIndication = findViewById(R.id.linearLayout_ImportIndication);
        linearLayout_ImportIndication.setOnClickListener(view -> {
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
        });

        checkBox_ImportItem.setChecked(galFileItems.get(giFileItemIndex).bIsChecked);
        CheckboxImportColorSwitch(galFileItems.get(giFileItemIndex).bIsChecked);


        checkBox_MarkForDeletion.setOnClickListener(view -> {
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
        });
        TextView textView_LabelMarkForDeletion = findViewById(R.id.textView_LabelMarkForDeletion);
        textView_LabelMarkForDeletion.setOnClickListener(view -> {
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
        });

        checkBox_MarkForDeletion.setChecked(galFileItems.get(giFileItemIndex).bMarkedForDeletion);
        CheckboxMarkForDeletionColorSwitch(galFileItems.get(giFileItemIndex).bMarkedForDeletion);


        recalcGroupButtonVisibilities();


        TextView textView_FileName = findViewById(R.id.textView_FileName);
        String sFileNameTextLine;// = galFileItems.get(giFileItemIndex).sFileOrFolderName;
        sFileNameTextLine = GlobalClass.cleanHTMLCodedCharacters(galFileItems.get(giFileItemIndex).sUri);
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
                gFragment_selectTags.resetTagListViewData(galFileItems.get(iFileItemTagsIndex).aliProspectiveTags);
            }

            //Show the sequence number of this item:
            TextView textView_ImportItemNumberOfNumber = findViewById(R.id.textView_ImportItemNumberOfNumber);
            String sTemp = (giFileItemIndex + 1) + "/" + (giMaxFileItemIndex + 1);
            textView_ImportItemNumberOfNumber.setText(sTemp);
        }

        ImageButton imageButton_PasteLastTags = findViewById(R.id.imageButton_PasteLastTags);
        if (imageButton_PasteLastTags != null) {
            imageButton_PasteLastTags.setOnClickListener(v -> copyLastTagSelection());


        }

        TextView textView_LabelCopyLastTagSelection = findViewById(R.id.textView_LabelCopyLastTagSelection);
        if(textView_LabelCopyLastTagSelection != null){
            textView_LabelCopyLastTagSelection.setOnClickListener(v -> copyLastTagSelection());
        }

        updateAdjacencies();

    }

    private void recalcGroupButtonVisibilities(){
        boolean bHasGroupID = !galFileItems.get(giFileItemIndex).sGroupID.equals("");
        gTextView_GroupID = findViewById(R.id.textView_GroupID);
        gImageButton_GroupIDNew = findViewById(R.id.imageButton_GroupIDNew);
        gImageButton_GroupIDCopy = findViewById(R.id.imageButton_GroupIDCopy);
        gImageButton_GroupIDPaste = findViewById(R.id.imageButton_GroupIDPaste);
        gImageButton_GroupIDRemove = findViewById(R.id.imageButton_GroupIDRemove);

        if(bHasGroupID){
            gTextView_GroupID.setText(galFileItems.get(giFileItemIndex).sGroupID);
            int[] iColors = Activity_CatalogViewer.calculateGroupingControlsColors(galFileItems.get(giFileItemIndex).sGroupID);
            gTextView_GroupID.setBackgroundColor(iColors[0]);
            gTextView_GroupID.setTextColor(iColors[1]);
            //Show the Group ID Copy icon, but only if the Group ID is not already on the internal clipboard:
            if(galFileItems.get(giFileItemIndex).sGroupID.equals(GlobalClass.gsGroupIDClip)){
                gImageButton_GroupIDCopy.setVisibility(View.INVISIBLE);
            } else {
                gImageButton_GroupIDCopy.setVisibility(View.VISIBLE);
            }
            gImageButton_GroupIDRemove.setVisibility(View.VISIBLE);
        } else {
            gTextView_GroupID.setText("----");
            gTextView_GroupID.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlack));
            gTextView_GroupID.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorTextColor));
            gImageButton_GroupIDCopy.setVisibility(View.INVISIBLE);
            gImageButton_GroupIDRemove.setVisibility(View.INVISIBLE);
        }

        //If the internal clipboard has a group ID, show the paste icon, but only if it is not the
        // group ID for the current item:
        if(GlobalClass.gsGroupIDClip.equals("")) {
            gImageButton_GroupIDPaste.setVisibility(View.INVISIBLE);
        } else {
            if(galFileItems.get(giFileItemIndex).sGroupID.equals(GlobalClass.gsGroupIDClip)){
                gImageButton_GroupIDPaste.setVisibility(View.INVISIBLE);
            } else {
                gImageButton_GroupIDPaste.setVisibility(View.VISIBLE);
            }
        }

        gImageButton_GroupIDNew.setOnClickListener(v -> {
            galFileItems.get(giFileItemIndex).sGroupID = GlobalClass.getNewGroupID();
            gImageButton_GroupIDRemove.setVisibility(View.VISIBLE);
            gTextView_GroupID.setText(galFileItems.get(giFileItemIndex).sGroupID);
            GlobalClass.gsGroupIDClip = galFileItems.get(giFileItemIndex).sGroupID;
            gImageButton_GroupIDPaste.setVisibility(View.INVISIBLE);
            GlobalClass.gbClearGroupIDAtImportClose = true;
            int[] iColors = Activity_CatalogViewer.calculateGroupingControlsColors(galFileItems.get(giFileItemIndex).sGroupID);
            gTextView_GroupID.setBackgroundColor(iColors[0]);
            gTextView_GroupID.setTextColor(iColors[1]);
            Toast.makeText(getApplicationContext(), "Group ID copied.", Toast.LENGTH_SHORT).show();
        });

        gImageButton_GroupIDCopy.setOnClickListener(v -> {
            GlobalClass.gsGroupIDClip = galFileItems.get(giFileItemIndex).sGroupID;
            gImageButton_GroupIDPaste.setVisibility(View.INVISIBLE);
            gImageButton_GroupIDCopy.setVisibility(View.INVISIBLE);
            GlobalClass.gbClearGroupIDAtImportClose = true;
            Toast.makeText(getApplicationContext(), "Group ID copied.", Toast.LENGTH_SHORT).show();
        });

        gImageButton_GroupIDPaste.setOnClickListener(v -> {
            if(!GlobalClass.gsGroupIDClip.equals("")){
                galFileItems.get(giFileItemIndex).sGroupID = GlobalClass.gsGroupIDClip;
                gImageButton_GroupIDRemove.setVisibility(View.VISIBLE);
                gImageButton_GroupIDPaste.setVisibility(View.INVISIBLE);
                gImageButton_GroupIDCopy.setVisibility(View.INVISIBLE);
                gTextView_GroupID.setText(GlobalClass.gsGroupIDClip);
                int[] iColors = Activity_CatalogViewer.calculateGroupingControlsColors(galFileItems.get(giFileItemIndex).sGroupID);
                gTextView_GroupID.setBackgroundColor(iColors[0]);
                gTextView_GroupID.setTextColor(iColors[1]);
            }
        });

        gImageButton_GroupIDRemove.setOnClickListener(v -> {
            galFileItems.get(giFileItemIndex).sGroupID = "";
            gImageButton_GroupIDCopy.setVisibility(View.INVISIBLE);
            gImageButton_GroupIDRemove.setVisibility(View.INVISIBLE);
            gTextView_GroupID.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlack));
            gTextView_GroupID.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorTextColor));
            gTextView_GroupID.setText("----");
        });
    }


    private void updateAdjacencies(){
        if(gbLookForFileAdjacencies){
            gRelativeLayout_Adjacencies.setVisibility(View.VISIBLE);

            gRecyclerView_Adjacencies = findViewById(R.id.recyclerView_Adjacencies);

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext(),
                    LinearLayoutManager.HORIZONTAL, false);
            gRecyclerView_Adjacencies.setLayoutManager(linearLayoutManager);

            gRelativeLayout_Adjacency_Analysis_Progress = findViewById(R.id.relativeLayout_Adjacency_Analysis_Progress);

            gProgressBar_AnalysisProgress = findViewById(R.id.progressBar_AnalysisProgress);
            gProgressBar_AnalysisProgress.setMax(100);
            gTextView_AnalysisProgressBarText = findViewById(R.id.textView_AnalysisProgressBarText);

            //Before starting the adjacency analyzer, clear the adjacency RecyclerView so that
            //  the user is not stuck looking at old results while the worker does its job:
            GlobalClass.gtmCatalogAdjacencyAnalysisTreeMap = new TreeMap<>();
            RecyclerViewCatalogAdjacencyAdapter gRecyclerViewCatalogAdapter = new RecyclerViewCatalogAdjacencyAdapter(GlobalClass.gtmCatalogAdjacencyAnalysisTreeMap);
            gRecyclerView_Adjacencies.setAdapter(gRecyclerViewCatalogAdapter);

            //Start the adjacency analyzer:
            int[] iarray = new int[galFileItems.get(giFileItemIndex).aliProspectiveTags.size()];
            for(int i = 0; i < galFileItems.get(giFileItemIndex).aliProspectiveTags.size(); i++){
                iarray[i] = galFileItems.get(giFileItemIndex).aliProspectiveTags.get(i);
            }
            int iHeight = -1;
            int iWidth = -1;
            try {
                iHeight = Integer.parseInt(galFileItems.get(giFileItemIndex).sHeight);
                iWidth = Integer.parseInt(galFileItems.get(giFileItemIndex).sWidth);
            } catch (Exception ignored){}
            double dDateLastModified = -1d;
            if(galFileItems.get(giFileItemIndex).dateLastModified != null){
                dDateLastModified = GlobalClass.GetTimeStampDouble(galFileItems.get(giFileItemIndex).dateLastModified);
            }
            String sCallerID = "Activity_ImportFilePreview.ImportFilePreviewResponseReceiver.onReceive()";
            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            Data dataStartAdjacencyAnalyzer = new Data.Builder()
                    .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)

                    .putString(Worker_Catalog_Adjaceny_Analyzer.EXTRA_STRING_FILENAME, galFileItems.get(giFileItemIndex).sFileOrFolderName)
                    .putString(Worker_Catalog_Adjaceny_Analyzer.EXTRA_STRING_FILENAME_FILTER, "")
                    .putInt(Worker_Catalog_Adjaceny_Analyzer.EXTRA_INT_HEIGHT, iHeight)
                    .putInt(Worker_Catalog_Adjaceny_Analyzer.EXTRA_INT_WIDTH, iWidth)
                    .putLong(Worker_Catalog_Adjaceny_Analyzer.EXTRA_LONG_DURATION, galFileItems.get(giFileItemIndex).lVideoTimeInMilliseconds)
                    .putDouble(Worker_Catalog_Adjaceny_Analyzer.EXTRA_DOUBLE_FILE_MODIFIED_DATE, dDateLastModified)
                    .putLong(Worker_Catalog_Adjaceny_Analyzer.EXTRA_LONG_FILE_SIZE, galFileItems.get(giFileItemIndex).lSizeBytes)
                    .putIntArray(Worker_Catalog_Adjaceny_Analyzer.EXTRA_ARRAY_INT_TAGS, iarray)

                    .build();
            OneTimeWorkRequest otwrStartAdjacencyAnalyzer = new OneTimeWorkRequest.Builder(Worker_Catalog_Adjaceny_Analyzer.class)
                    .setInputData(dataStartAdjacencyAnalyzer)
                    .addTag(Worker_Catalog_Adjaceny_Analyzer.TAG_WORKER_CATALOG_ADJACENCY_ANALYZER) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(otwrStartAdjacencyAnalyzer);

        /*} else {
            gRelativeLayout_Adjacencies.setVisibility(View.INVISIBLE);
            gButton_ShowAdjacencies.setEnabled(true);*/
        }
    }



    private void copyLastTagSelection(){
        if(galiLastAssignedTags != null){
            //If the user is pasting tags, set a flag to move to the next item automatically.
            gbPastingTags = true;
            gFragment_selectTags.gListViewTagsAdapter.selectTagsByIDs(galiLastAssignedTags);
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

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(importFilePreviewResponseReceiver);
        super.onDestroy();
    }


    public class ImportFilePreviewResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            //Get boolean indicating that an error may have occurred:
            boolean bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);

            if (bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {
                //Check to see if this is a response to update progress bar:
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdatePercentComplete || bUpdateProgressBarText){
                    gRelativeLayout_Adjacency_Analysis_Progress.setVisibility(View.VISIBLE);
                }

                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);

                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> {
                        //Do something after 100ms
                        if(gProgressBar_AnalysisProgress != null) {
                            gProgressBar_AnalysisProgress.setProgress(iAmountComplete);
                        }
                    }, 100);
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_AnalysisProgressBarText != null) {
                        gTextView_AnalysisProgressBarText.setText(sProgressBarText);
                    }
                }

                //Check to see if this is a response indicating adjacencies analysis is complete:
                boolean bAdjacencyAnalyzerComplete = intent.getBooleanExtra(Worker_Catalog_Adjaceny_Analyzer.CATALOG_ADJAN_EXTRA_BOOL_COMPLETE, false);
                if (bAdjacencyAnalyzerComplete) {
                    gRelativeLayout_Adjacency_Analysis_Progress.setVisibility(View.INVISIBLE);
                    if(GlobalClass.gtmCatalogAdjacencyAnalysisTreeMap.size() == 0){
                        //gRelativeLayout_Adjacencies.setVisibility(View.INVISIBLE);
                        String sMessage = "No adjacencies found. If this is unexpected, understand that this function" +
                                " will not compare against catalog items private to other users, and filters" +
                                " against user-selected maturity settings and tags. If no resolution data appears to" +
                                " be available for the base resource, ensure initial analysis of folder items is set" +
                                " to include resolution data to improve matches.";
                        GlobalClass.ShowMessage(gContextWindow, "Adjacency Analysis", sMessage);
                        gbLookForFileAdjacencies = false; //Allow the user to click the button to start looking at adjacencies again.
                        gButton_ShowAdjacencies.setEnabled(true);
                    } else {
                        //Initiate the RecyclerView:
                        gRelativeLayout_Adjacencies.setVisibility(View.VISIBLE);
                        RecyclerViewCatalogAdjacencyAdapter gRecyclerViewCatalogAdapter = new RecyclerViewCatalogAdjacencyAdapter(GlobalClass.gtmCatalogAdjacencyAnalysisTreeMap);
                        gRecyclerView_Adjacencies.setAdapter(gRecyclerViewCatalogAdapter);

                        //Populate statistics for the adjacencies:
                        int iMatchTotal						 = intent.getIntExtra(Worker_Catalog_Adjaceny_Analyzer.CATALOG_ADJAN_EXTRA_INT_MAT_TOTAL, 0);
                        int iMatchCountOnFileName			 = intent.getIntExtra(Worker_Catalog_Adjaceny_Analyzer.CATALOG_ADJAN_EXTRA_INT_MAT_FNAME, 0);
                        int iMatchCountOnModifiedDateWindow	 = intent.getIntExtra(Worker_Catalog_Adjaceny_Analyzer.CATALOG_ADJAN_EXTRA_INT_MAT_MDATE, 0);
                        int iMatchCountOnResolution			 = intent.getIntExtra(Worker_Catalog_Adjaceny_Analyzer.CATALOG_ADJAN_EXTRA_INT_MAT_RES  , 0);
                        int iMatchCountOnDuration			 = intent.getIntExtra(Worker_Catalog_Adjaceny_Analyzer.CATALOG_ADJAN_EXTRA_INT_MAT_DUR  , 0);

                        if (    (gTextView_AdjacencyCount			!= null) &&
                                (gTextView_FileNameMatchCount		!= null) &&
                                (gTextView_DateModifiedMatchCount	!= null) &&
                                (gTextView_ResolutionMatchCount	    != null) &&
                                (gTextView_DurationMatchCount		!= null)){
                            String sTemp = "" + iMatchTotal;
                            gTextView_AdjacencyCount.setText         (sTemp);
                            sTemp = "" + iMatchCountOnFileName;
                            gTextView_FileNameMatchCount.setText     (sTemp);
                            sTemp = "" + iMatchCountOnModifiedDateWindow;
                            gTextView_DateModifiedMatchCount.setText (sTemp);
                            if(galFileItems.get(giFileItemIndex).sHeight.equals("")){
                                sTemp = "No res data avail.";
                            } else {
                                sTemp = "" + iMatchCountOnResolution;
                            }
                            gTextView_ResolutionMatchCount.setText(sTemp);
                            sTemp = "" + iMatchCountOnDuration;
                            gTextView_DurationMatchCount.setText     (sTemp);

                        }


                    }
                }

            }

        }
    }




    //The below RecyclerView is only for finding item adjacencies. That is, items that are similar to the prospective import image:
    public class RecyclerViewCatalogAdjacencyAdapter extends RecyclerView.Adapter<RecyclerViewCatalogAdjacencyAdapter.ViewHolder> {

        private final TreeMap<Integer, ItemClass_CatalogItem> treeMap;
        private final Integer[] mapKeys;

        ViewGroup vgParent;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView ivThumbnail;
            public final TextView tvThumbnailText;

            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.imageView_Thumbnail);
                tvThumbnailText = v.findViewById(R.id.textView_Title);
            }
        }

        public RecyclerViewCatalogAdjacencyAdapter(TreeMap<Integer, ItemClass_CatalogItem> data) {
            this.treeMap = data;
            mapKeys = treeMap.keySet().toArray(new Integer[getCount()]);
        }

        public int getCount() {
            return treeMap.size();
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public RecyclerViewCatalogAdjacencyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                                               int viewType) {
            // create a new view
            View v;
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            v = inflater.inflate(R.layout.recycler_catalog_adjacencies_grid, parent, false);

            vgParent = parent;

            return new RecyclerViewCatalogAdjacencyAdapter.ViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull RecyclerViewCatalogAdjacencyAdapter.ViewHolder holder, final int position) {
            // - get element from your data set at this position
            // - replace the contents of the view with that element

            //Get the data for the row:
            ItemClass_CatalogItem ci;
            ci = treeMap.get(mapKeys[position]);
            final ItemClass_CatalogItem ci_final = ci;
            assert ci_final != null;

            String sItemName;


            //Load the non-obfuscated image into the RecyclerView ViewHolder:

            Uri uriThumbnailUri;
            boolean bThumbnailQuickLookupSuccess = true;

            String sFileName = ci.sThumbnail_File;
            if(sFileName.equals("")){
                sFileName = ci.sFilename;
            }
            String sPath = GlobalClass.gsCatalogFolderNames[ci.iMediaCategory]
                    + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                    + GlobalClass.gsFileSeparator + sFileName;
            if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                //If this is an m3u8 video style catalog item, configure the path to the file to use as the thumbnail.
                sPath = GlobalClass.gsCatalogFolderNames[ci.iMediaCategory]
                        + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                        + GlobalClass.gsFileSeparator + ci.sThumbnail_File; //ci.sFilename will be the m3u8 file name in this case.
            }
            String sThumbnailUri = GlobalClass.gsUriAppRootPrefix
                    + GlobalClass.gsFileSeparator + sPath;
            uriThumbnailUri = Uri.parse(sThumbnailUri);


            if(GlobalClass.gbUseCatalogItemThumbnailDeepSearch) {
                //Check to see if the thumbnail source is where it is supposed to be. If it is not
                //  there, check for other related happenings that might identify the location.
                //  This can add a little more tha 1/100th of a second to processing the thumbnail,
                //  and in testing resulted in a stutter of the recyclerView.
                bThumbnailQuickLookupSuccess = GlobalClass.CheckIfFileExists(uriThumbnailUri);
            }


            if(!bThumbnailQuickLookupSuccess) {
                Uri uriCatalogItemFolder;
                uriCatalogItemFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.giSelectedCatalogMediaCategory].toString(), ci.sFolderRelativePath);

                if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS &&
                        ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_COMIC_DLM_MOVE) {
                    //If this is a comic, and the files from DownloadManager have not been moved as
                    //  part of download post-processing, look in the [comic]\download folder for the files:
                    if (uriCatalogItemFolder != null) {
                        Uri uriDLTempFolder = GlobalClass.FormChildUri(uriCatalogItemFolder.toString(), GlobalClass.gsDLTempFolderName);
                        if (uriDLTempFolder != null) {
                            uriThumbnailUri = GlobalClass.FormChildUri(uriDLTempFolder.toString(), ci.sFilename);
                        }
                    }
                }
                if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                    if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_DLM_CONCAT) {
                        //We are not doing anything with this item.
                        uriThumbnailUri = null;
                    } else if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                        //If this is a local M3U8, locate the downloaded thumbnail image or first video to present as thumbnail.
                        Uri uriVideoTagFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].toString(), ci.sFolderRelativePath);

                        if (uriVideoTagFolder != null) {
                            Uri uriVideoWorkingFolder = GlobalClass.FormChildUri(uriVideoTagFolder.toString(), ci.sItemID);

                            if (uriVideoWorkingFolder != null) {
                                Uri uriDownloadedThumbnailFile = GlobalClass.FormChildUri(uriVideoWorkingFolder.toString(), ci.sThumbnail_File);

                                if (uriDownloadedThumbnailFile != null) { //isDir if ci.sThum=="".
                                    uriThumbnailUri = uriDownloadedThumbnailFile;
                                } else {
                                    //If there is no downloaded thumbnail file, find the first .ts file and use that for the thumbnail:
                                    Uri uriM3U8File = GlobalClass.FormChildUri(uriVideoWorkingFolder.toString(), ci.sFilename);
                                    if (uriM3U8File != null) {
                                        try {
                                            InputStream isM3U8File = GlobalClass.gcrContentResolver.openInputStream(uriM3U8File);
                                            if (isM3U8File != null) {
                                                BufferedReader brReader;
                                                brReader = new BufferedReader(new InputStreamReader(isM3U8File));
                                                String sLine = brReader.readLine();
                                                while (sLine != null) {
                                                    if (!sLine.startsWith("#") && sLine.contains(".st")) {
                                                        Uri uriThumbnailFileCandidate = GlobalClass.FormChildUri(uriVideoWorkingFolder.toString(), sLine);
                                                        if (uriThumbnailFileCandidate != null) {
                                                            uriThumbnailUri = uriThumbnailFileCandidate;
                                                            break;
                                                        }
                                                    }
                                                    // read next line
                                                    sLine = brReader.readLine();
                                                }
                                                brReader.close();
                                                isM3U8File.close();
                                            }

                                        } catch (Exception e) {
                                            //Probably a file IO exception.
                                        }
                                    }


                                }  //End if we had to look for a .ts file to serve as a thumbnail file.
                            } //End if unable to find video working folder DocumentFile.
                        } //End if unable to find video tag folder DocumentFile.
                    } //End if video is m3u8 style.

                }
                if(uriThumbnailUri != null) {
                    if (!GlobalClass.CheckIfFileExists(uriThumbnailUri)) {
                        uriThumbnailUri = null;
                    }
                }
            }


            if(uriThumbnailUri != null) {
                Glide.with(getApplicationContext())
                        .load(uriThumbnailUri)
                        .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                        .into(holder.ivThumbnail);
            } else {
                //Special behavior if this is a comic.
                boolean bFoundMissingComicThumbnail = false;
                if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                    //Check to see if the comic thumbnail was merely deleted such in the case if it were renamed or a duplicate, and if so select the next file (alphabetically) to be the thumbnail.
                    Uri uriComicFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].toString(), ci.sFolderRelativePath);


                    //Load the full path to each comic page into tmComicPages (sorts files):
                    TreeMap<String, String> tmSortByFileName = new TreeMap<>();
                    if(uriComicFolder != null){
                        ArrayList<String> sComicPages = GlobalClass.GetDirectoryFileNames(uriComicFolder);
                        if(sComicPages.size() > 0) {
                            for (String sComicPage : sComicPages) {
                                tmSortByFileName.put(GlobalClass.JumbleFileName(sComicPage), GlobalClass.FormChildUriString(uriComicFolder.toString(), sComicPage)); //de-jumble to get proper alphabetization.
                            }
                        }
                        //Assign the existing file to be the new thumbnail file:
                        if(tmSortByFileName.size() > 0) {
                            Map.Entry<String, String> mapNewComicThumbnail = tmSortByFileName.firstEntry();
                            if(mapNewComicThumbnail != null) {
                                ci.sFilename = GlobalClass.JumbleFileName(mapNewComicThumbnail.getKey()); //re-jumble to get actual file name.
                                uriThumbnailUri = Uri.parse(mapNewComicThumbnail.getValue());
                                bFoundMissingComicThumbnail = true;
                            }
                        }
                    }

                }

                if(bFoundMissingComicThumbnail){
                    Glide.with(getApplicationContext())
                            .load(uriThumbnailUri)
                            .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                            .into(holder.ivThumbnail);
                } else {
                    Glide.with(getApplicationContext())
                            .load(R.drawable.baseline_image_white_18dp_wpagepad)
                            .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                            .into(holder.ivThumbnail);
                }
            }

            String sThumbnailText = "";
            switch (GlobalClass.giSelectedCatalogMediaCategory) {
                case GlobalClass.MEDIA_CATEGORY_VIDEOS:
                    String sTemp = ci.sFilename;
                    sItemName = GlobalClass.JumbleFileName(sTemp);
                    if(!ci.sTitle.equals("")){
                        sItemName = ci.sTitle;
                    }
                    sThumbnailText = sItemName;
                    if(!ci.sDuration_Text.equals("")){
                        sThumbnailText = sThumbnailText  + ", " + ci.sDuration_Text;
                    }
                    break;
                case GlobalClass.MEDIA_CATEGORY_IMAGES:
                    sItemName = GlobalClass.JumbleFileName(ci.sFilename);
                    sThumbnailText = sItemName;
                    break;
                case GlobalClass.MEDIA_CATEGORY_COMICS:
                    sItemName = ci.sTitle;
                    sThumbnailText = sItemName;
                    break;
            }

            if(sThumbnailText.length() > 100){
                sThumbnailText = sThumbnailText.substring(0, 100) + "...";
            }

            holder.tvThumbnailText.setText(sThumbnailText);
            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Create popup context menu with options for the user.
                    PopupMenu popupAdjacencyItemOptions = new PopupMenu(vgParent.getContext(), v);
                    if(ci.sGroupID.equals("")){
                        popupAdjacencyItemOptions.getMenuInflater().inflate(R.menu.adjacency_action_menu_nogroup, popupAdjacencyItemOptions.getMenu());
                    } else {
                        popupAdjacencyItemOptions.getMenuInflater().inflate(R.menu.adjacency_action_menu, popupAdjacencyItemOptions.getMenu());
                    }
                    popupAdjacencyItemOptions.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int itemId = item.getItemId();

                            if (itemId == R.id.menu_ApplyTagsFromItem) {
                                //Apply the tags associated with this catalog item to the potential import item.
                                gFragment_selectTags.gListViewTagsAdapter.selectTagsByIDs(ci.aliTags);
                                return true;

                            } else if (itemId == R.id.menu_CopyFileName) {
                                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("Filename", ci.sFilename);
                                clipboard.setPrimaryClip(clip);
                                return true;

                            } else if (itemId == R.id.menu_CopyGroupID) {
                                GlobalClass.gsGroupIDClip = ci.sGroupID;
                                recalcGroupButtonVisibilities();
                                return true;

                            } else { // (itemId == R.id.menu_Magnify) {

                                return true;

                            }

                        }
                    });

                    popupAdjacencyItemOptions.show();

























                } //End context menu popup for if the user clicks on the adjacency item.
            });


        }

        // Return the size of the data set (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return treeMap.size();
        }

    }


}
