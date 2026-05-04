package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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


public class Fragment_Import_2e_ImportFilePreview extends Fragment {

    GlobalClass globalClass;
    private ViewModel_ImportActivity viewModelImportActivity;

    private ArrayList<ItemClass_File> galFileItems;
    private int giFileItemIndex;
    private int giFileItemLastIndex;    //Used to automatically move the user to the next item if they
    //  hit the 'mark for deletion' checkbox.
    private int giMaxFileItemIndex;
    private static final String IMAGE_PREVIEW_INDEX = "image_preview_index";

    private Fragment_SelectTags gFragment_selectTags; //Used to reset tags when swiping to the next file.

    private boolean gbLookForFileAdjacencies = false;

    //ExoPlayer is used for playback of local M3U8 files:
    private ExoPlayer gExoPlayer;

    private ImageView gImagePreview;

    private long glCurrentVideoPosition = 1;
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

    ImageButton gImageButton_NextItem;
    ImageButton gImageButton_PreviousItem;

    Button gButton_ShowAdjacencies;
    TextView gTextView_AdjacencyCount			;
    TextView gTextView_FileNameMatchCount		;
    TextView gTextView_DateModifiedMatchCount	;
    TextView gTextView_ResolutionMatchCount		;
    TextView gTextView_DurationMatchCount		;

    public Fragment_Import_2e_ImportFilePreview() {
        // Required empty public constructor
    }

    public static Fragment_Import_2e_ImportFilePreview newInstance() {
        return new Fragment_Import_2e_ImportFilePreview();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() == null){
            return;
        }

        //Instantiate the ViewModel sharing data between fragments:
        viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        if (savedInstanceState != null) {
            giFileItemIndex = savedInstanceState.getInt(IMAGE_PREVIEW_INDEX);
            giFileItemLastIndex = giFileItemIndex;
            glCurrentVideoPosition = savedInstanceState.getLong(PLAYBACK_TIME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_2e_import_file_preview, container, false);
    }

    private void iterateToLesserIndexedItem(){
        int iTempKey = giFileItemIndex - 1;

        iTempKey = Math.max(0, iTempKey);
        if(iTempKey != giFileItemIndex) {
            giFileItemLastIndex = giFileItemIndex;
            giFileItemIndex = iTempKey;
            setNextPrevButtonVisibilities();
            initializeFile();
        }

    }

    private void iterateToGreaterIndexedItem(){
        int iTempKey = giFileItemIndex + 1;

        iTempKey = Math.min(giMaxFileItemIndex, iTempKey);
        if(iTempKey != giFileItemIndex) {
            giFileItemLastIndex = giFileItemIndex;
            giFileItemIndex = iTempKey;
            setNextPrevButtonVisibilities();
            initializeFile();
        }

    }

    private void setNextPrevButtonVisibilities(){
        if(gImageButton_NextItem != null && gImageButton_PreviousItem != null) {
            if (giFileItemIndex == 0) {
                gImageButton_PreviousItem.setVisibility(View.INVISIBLE);
            } else {
                gImageButton_PreviousItem.setVisibility(View.VISIBLE);
            }
            if(giFileItemIndex == giMaxFileItemIndex){
                gImageButton_NextItem.setVisibility(View.INVISIBLE);
            } else {
                gImageButton_NextItem.setVisibility(View.VISIBLE);
            }
        }


    }



    private void initializeFile(){
        if(getView() == null){
            return;
        }
        if(getActivity() == null){
            return;
        }

        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {

            if(galFileItems.get(giFileItemIndex).iTypeFileFolderURL == ItemClass_File.TYPE_FILE) {
                Uri uriVideoFile = Uri.parse(galFileItems.get(giFileItemIndex).sUri);

                MediaItem mediaItem = MediaItem.fromUri(uriVideoFile);
                gExoPlayer.setMediaItem(mediaItem);
                gExoPlayer.prepare();

            } else if(galFileItems.get(giFileItemIndex).iTypeFileFolderURL == ItemClass_File.TYPE_FOLDER) {
                //If we are here, then this is an m3u8 folder passed by the CatalogAnalyzer and the user is checking to
                //  see if this orphaned file matches something in the database.
                //Look for the m3u8 file:
                String sMessage;
                if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(viewModelImportActivity.iImportMediaCategory) != null){
                    if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(viewModelImportActivity.iImportMediaCategory).size() > 0){
                        String sKeyPrefix = GlobalClass.GetRelativePathFromUriString(galFileItems.get(giFileItemIndex).sUri, GlobalClass.gUriDataFolder.toString());
                        for(Map.Entry<String, ItemClass_File> entry: GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(viewModelImportActivity.iImportMediaCategory).entrySet()){
                            if(entry.getKey().startsWith(sKeyPrefix)){
                                if(entry.getKey().endsWith("m3u8")){

                                    //Check to see if the m3u8 file needs to be corrected to have relative paths.
                                    //  If the user has transferred data to a new memory card, this is important.
                                    //Open the m3u8 file and ensure that it has the proper paths:
                                    String sM3U8_Uri = GlobalClass.FormChildUriString(GlobalClass.gUriCatalogFolders[viewModelImportActivity.iImportMediaCategory].toString(), entry.getKey());
                                    byte[] byteM3U8_File = null;
                                    Uri uriM3U8 = null;
                                    try{
                                        uriM3U8 = Uri.parse(sM3U8_Uri);
                                        InputStream isM3U8 = GlobalClass.gcrContentResolver.openInputStream(uriM3U8);
                                        if(isM3U8 == null){
                                            sMessage = "Could not open M3U8 file.";
                                            Toast.makeText(getActivity().getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                                        } else {
                                            byteM3U8_File = GlobalClass.readAllBytes(isM3U8);
                                            isM3U8.close();
                                        }

                                    } catch (Exception e){
                                        sMessage = "Problem identifying M3U8 file. " + e.getMessage();
                                        Toast.makeText(getActivity().getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                                    }

                                    if(byteM3U8_File != null) {
                                        //Read-in one path to make sure it is accurate.
                                        String sM3U8_File_Contents = new String(byteM3U8_File);
                                        String[] sM3U8_FileLines = sM3U8_File_Contents.split("\n");

                                        boolean bM3U8_File_Internal_Paths_UpToDate = Worker_Catalog_Analysis.M3U8FileRelativePathsUptoDate(sM3U8_FileLines);

                                        if(!bM3U8_File_Internal_Paths_UpToDate){
                                            //M3U8 file does not have up-to-date paths utilizing the current storage structure.
                                            // This could be caused by moving the database.

                                            //Update the file to the current base storage:
                                            try {
                                                String sParentFolder = GlobalClass.GetParentUri(sM3U8_Uri);
                                                if(Worker_Catalog_Analysis.UpdateM3U8FileRelativePaths(sM3U8_FileLines, sParentFolder, uriM3U8)){
                                                    sMessage = "M3U8 playlist file internal paths updated successfully.";
                                                    Toast.makeText(getActivity().getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                                                } else {
                                                    sMessage = "Could not open M3U8 playlist file to update file paths.";
                                                    Toast.makeText(getActivity().getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                                                    continue;
                                                }
                                            } catch (Exception e) {
                                                sMessage = "Problem processing and/or writing to updated M3U8 file: " + e.getMessage();
                                                Toast.makeText(getActivity().getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                    } //End if the M3U8 file bytes not null.

                                    if(uriM3U8 != null) {
                                        MediaItem mediaItem = MediaItem.fromUri(uriM3U8);
                                        gExoPlayer.setMediaItem(mediaItem);
                                        gExoPlayer.prepare();
                                        gExoPlayer.setPlayWhenReady(true);
                                    } else {
                                        sMessage = "Could not locate M3U8 file.";
                                        Toast.makeText(getActivity().getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }

            } else if (galFileItems.get(giFileItemIndex).iTypeFileFolderURL == ItemClass_File.TYPE_URL) {
                MediaItem mediaItem = MediaItem.fromUri(galFileItems.get(giFileItemIndex).sURLVideoLink);
                gExoPlayer.setMediaItem(mediaItem);
                gExoPlayer.prepare();
                gExoPlayer.setPlayWhenReady(true);

            }
            // Skipping to 1 shows the first frame of the video.
            gExoPlayer.seekTo(1);
            glCurrentVideoPosition = 1;

            giCurrentVideoPlaybackState = VIDEO_PLAYBACK_STATE_PLAYING;
        } else {
            if( !galFileItems.get(giFileItemIndex).sUri.equals("")) {
                Glide.with(getActivity().getApplicationContext()).load(galFileItems.get(giFileItemIndex).sUri).into(gImagePreview);
            } else {
                Glide.with(getActivity().getApplicationContext()).load(galFileItems.get(giFileItemIndex).sURL).into(gImagePreview);
            }
        }

        final CheckBox checkBox_ImportItem = getView().findViewById(R.id.checkBox_ImportItem);
        final CheckBox checkBox_MarkForDeletion = getView().findViewById(R.id.checkBox_MarkForDeletion);

        checkBox_ImportItem.setOnClickListener(view -> {
            galFileItems.get(giFileItemIndex).bIsChecked = ((CheckBox)view).isChecked();
            galFileItems.get(giFileItemIndex).bDataUpdateFlag = true;
            CheckboxImportColorSwitch(galFileItems.get(giFileItemIndex).bIsChecked);

            if(galFileItems.get(giFileItemIndex).bIsChecked && galFileItems.get(giFileItemIndex).bMarkedForDeletion){
                galFileItems.get(giFileItemIndex).bMarkedForDeletion = false;
                checkBox_MarkForDeletion.setChecked(false);
                CheckboxMarkForDeletionColorSwitch(false);
            }

        });
        TextView textView_LabelImport = getView().findViewById(R.id.textView_LabelImport);
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

        });
        LinearLayout linearLayout_ImportIndication = getView().findViewById(R.id.linearLayout_ImportIndication);
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

        });
        TextView textView_LabelMarkForDeletion = getView().findViewById(R.id.textView_LabelMarkForDeletion);
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

        });

        checkBox_MarkForDeletion.setChecked(galFileItems.get(giFileItemIndex).bMarkedForDeletion);
        CheckboxMarkForDeletionColorSwitch(galFileItems.get(giFileItemIndex).bMarkedForDeletion);


        recalcGroupButtonVisibilities();


        TextView textView_FileName = getView().findViewById(R.id.textView_FileName);
        String sFileNameTextLine;// = galFileItems.get(giFileItemIndex).sFileOrFolderName;
        sFileNameTextLine = GlobalClass.cleanHTMLCodedCharacters(galFileItems.get(giFileItemIndex).sUri);
        if(!galFileItems.get(giFileItemIndex).sHeight.equals("")){ //Add resolution data to display if available:
            sFileNameTextLine = sFileNameTextLine + "\n" + galFileItems.get(giFileItemIndex).sWidth + "x" + galFileItems.get(giFileItemIndex).sHeight;
        }
        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES){
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
            TextView textView_SelectedTags = getView().findViewById(R.id.textView_SelectedTags);
            StringBuilder sbTags = new StringBuilder();
            sbTags.append("Tags: ");

            int iFileItemTagsIndex = 0; //If the media type is Comics, tags are applied to the first
            //  file item only.
            if(viewModelImportActivity.iImportMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS){
                iFileItemTagsIndex = giFileItemIndex;
            }

            if (galFileItems.get(iFileItemTagsIndex).aliProspectiveTags.size() > 0) {

                ArrayList<Integer> aliConfirmedProspectiveTags = new ArrayList<>(); //Confirm all tags exist as user may have deleted a tag.
                for(Integer iTagID: galFileItems.get(iFileItemTagsIndex).aliProspectiveTags){
                    if(globalClass.TagIDExists(iTagID, viewModelImportActivity.iImportMediaCategory)){
                        aliConfirmedProspectiveTags.add(iTagID);
                    }
                }
                galFileItems.get(iFileItemTagsIndex).aliProspectiveTags = aliConfirmedProspectiveTags;

                //Update the Tag text listing on the preview display:
                sbTags.append(globalClass.getTagTextFromID(galFileItems.get(iFileItemTagsIndex).aliProspectiveTags.get(0), viewModelImportActivity.iImportMediaCategory));
                for (int i = 1; i < galFileItems.get(iFileItemTagsIndex).aliProspectiveTags.size(); i++) {
                    sbTags.append(", ");
                    sbTags.append(globalClass.getTagTextFromID(galFileItems.get(iFileItemTagsIndex).aliProspectiveTags.get(i), viewModelImportActivity.iImportMediaCategory));
                }

            }
            if (textView_SelectedTags != null) {
                textView_SelectedTags.setText(sbTags.toString());
            }

            if(viewModelImportActivity.iImportMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS) { //Don't worry about resetting if it's a comic. Tags are same for every page.
                gbFreezeLastAssignedReset = true; //Don't let the data observer reset the "lastAssignedTags" arrayList.
                gFragment_selectTags.resetTagListViewData(galFileItems.get(iFileItemTagsIndex).aliProspectiveTags);
            }

            //Show the sequence number of this item:
            TextView textView_ImportItemNumberOfNumber = getView().findViewById(R.id.textView_ImportItemNumberOfNumber);
            String sTemp = (giFileItemIndex + 1) + "/" + (giMaxFileItemIndex + 1);
            textView_ImportItemNumberOfNumber.setText(sTemp);
        }

        ImageButton imageButton_PasteLastTags = getView().findViewById(R.id.imageButton_PasteLastTags);
        if (imageButton_PasteLastTags != null) {
            imageButton_PasteLastTags.setOnClickListener(v -> copyLastTagSelection());


        }

        TextView textView_LabelCopyLastTagSelection = getView().findViewById(R.id.textView_LabelCopyLastTagSelection);
        if(textView_LabelCopyLastTagSelection != null){
            textView_LabelCopyLastTagSelection.setOnClickListener(v -> copyLastTagSelection());
        }

        updateAdjacencies();

    }

    private void recalcGroupButtonVisibilities(){
        if(getView() == null){
            return;
        }
        if(getActivity() == null){
            return;
        }
        boolean bHasGroupID = !galFileItems.get(giFileItemIndex).sGroupID.equals("");
        gTextView_GroupID = getView().findViewById(R.id.textView_GroupID);
        gImageButton_GroupIDNew = getView().findViewById(R.id.imageButton_GroupIDNew);
        gImageButton_GroupIDCopy = getView().findViewById(R.id.imageButton_GroupIDCopy);
        gImageButton_GroupIDPaste = getView().findViewById(R.id.imageButton_GroupIDPaste);
        gImageButton_GroupIDRemove = getView().findViewById(R.id.imageButton_GroupIDRemove);

        if(bHasGroupID){
            gTextView_GroupID.setText(galFileItems.get(giFileItemIndex).sGroupID);
            int[] iColors = GlobalClass.calculateGroupingControlsColors(galFileItems.get(giFileItemIndex).sGroupID);
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
            gTextView_GroupID.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBlack));
            gTextView_GroupID.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
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
            int[] iColors = GlobalClass.calculateGroupingControlsColors(galFileItems.get(giFileItemIndex).sGroupID);
            gTextView_GroupID.setBackgroundColor(iColors[0]);
            gTextView_GroupID.setTextColor(iColors[1]);
            Toast.makeText(getActivity().getApplicationContext(), "Group ID copied.", Toast.LENGTH_SHORT).show();
        });

        gImageButton_GroupIDCopy.setOnClickListener(v -> {
            GlobalClass.gsGroupIDClip = galFileItems.get(giFileItemIndex).sGroupID;
            gImageButton_GroupIDPaste.setVisibility(View.INVISIBLE);
            gImageButton_GroupIDCopy.setVisibility(View.INVISIBLE);
            GlobalClass.gbClearGroupIDAtImportClose = true;
            Toast.makeText(getActivity().getApplicationContext(), "Group ID copied.", Toast.LENGTH_SHORT).show();
        });

        gImageButton_GroupIDPaste.setOnClickListener(v -> {
            if(!GlobalClass.gsGroupIDClip.equals("")){
                galFileItems.get(giFileItemIndex).sGroupID = GlobalClass.gsGroupIDClip;
                gImageButton_GroupIDRemove.setVisibility(View.VISIBLE);
                gImageButton_GroupIDPaste.setVisibility(View.INVISIBLE);
                gImageButton_GroupIDCopy.setVisibility(View.INVISIBLE);
                gTextView_GroupID.setText(GlobalClass.gsGroupIDClip);
                int[] iColors = GlobalClass.calculateGroupingControlsColors(galFileItems.get(giFileItemIndex).sGroupID);
                gTextView_GroupID.setBackgroundColor(iColors[0]);
                gTextView_GroupID.setTextColor(iColors[1]);
            }
        });

        gImageButton_GroupIDRemove.setOnClickListener(v -> {
            galFileItems.get(giFileItemIndex).sGroupID = "";
            gImageButton_GroupIDCopy.setVisibility(View.INVISIBLE);
            gImageButton_GroupIDRemove.setVisibility(View.INVISIBLE);
            gTextView_GroupID.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBlack));
            gTextView_GroupID.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
            gTextView_GroupID.setText("----");
        });
    }


    private void updateAdjacencies(){
        if(getView() == null){
            return;
        }
        if(getActivity() == null){
            return;
        }
        if(gbLookForFileAdjacencies){
            gRelativeLayout_Adjacencies.setVisibility(View.VISIBLE);

            gRecyclerView_Adjacencies = getView().findViewById(R.id.recyclerView_Adjacencies);

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext(),
                    LinearLayoutManager.HORIZONTAL, false);
            gRecyclerView_Adjacencies.setLayoutManager(linearLayoutManager);

            gRelativeLayout_Adjacency_Analysis_Progress = getView().findViewById(R.id.relativeLayout_Adjacency_Analysis_Progress);

            gProgressBar_AnalysisProgress = getView().findViewById(R.id.progressBar_AnalysisProgress);
            gProgressBar_AnalysisProgress.setMax(100);
            gTextView_AnalysisProgressBarText = getView().findViewById(R.id.textView_AnalysisProgressBarText);

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
            String sCallerID = "Fragment_Import_2e_ImportFilePreview.ImportFilePreviewResponseReceiver.onReceive()";
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
            WorkManager.getInstance(getActivity().getApplicationContext()).enqueue(otwrStartAdjacencyAnalyzer);

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
        if(getView() == null){
            return;
        }
        if(getActivity() == null){
            return;
        }
        LinearLayout linearLayout_ImportIndication = getView().findViewById(R.id.linearLayout_ImportIndication);
        if(bChecked) {
            linearLayout_ImportIndication.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorActionBar));
        } else {
            linearLayout_ImportIndication.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBlack));
        }

    }

    private void CheckboxMarkForDeletionColorSwitch(boolean bChecked){
        if(getView() == null){
            return;
        }
        if(getActivity() == null){
            return;
        }
        LinearLayout linearLayout_MarkForDeletion = getView().findViewById(R.id.linearLayout_MarkForDeletion);
        if(bChecked) {
            linearLayout_MarkForDeletion.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorActionBar));
        } else {
            linearLayout_MarkForDeletion.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBlack));
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if(getActivity() == null){
            return;
        }
        if(getView() == null){
            return;
        }
        if(viewModelImportActivity == null){
            return;
        }

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        ViewModel_Fragment_SelectTags viewModel_fragment_selectTags = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_SelectTags.class);
        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<ItemClass_Tag>> selectedTagsObserver = tagItems -> {

            if(getView() == null){
                return;
            }

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
            TextView textView_SelectedTags = getView().findViewById(R.id.textView_SelectedTags);
            if (textView_SelectedTags != null) {
                textView_SelectedTags.setText(sb.toString());
            }
            TextView textView_MaturityRating = getView().findViewById(R.id.textView_MaturityRating);
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
            if (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
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
                CheckBox checkBox_ImportItem = getView().findViewById(R.id.checkBox_ImportItem);
                checkBox_ImportItem.setChecked(true);
                CheckboxImportColorSwitch(true);
                CheckBox checkBox_MarkForDeletion = getView().findViewById(R.id.checkBox_MarkForDeletion);
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

        };
        viewModel_fragment_selectTags.altiTagsSelected.observe(getActivity(), selectedTagsObserver);

        gButton_ShowAdjacencies = getView().findViewById(R.id.button_ShowAdjacencies);

        galFileItems = ((Activity_Import) getActivity()).fileListCustomAdapter.alFileItemsDisplay;
        //todo: Test the above during import of local-storage comics. Ensure that the tags are properly assigned to the imported item, as well as any
        //  group identifier.

        giMaxFileItemIndex = galFileItems.size() - 1;
        giFileItemIndex = viewModelImportActivity.iSelectedItemIndexForPreview;
        giFileItemLastIndex = giFileItemIndex;

        gbLookForFileAdjacencies = viewModelImportActivity.bImportingOrphanedFiles;
        if(gbLookForFileAdjacencies){
            gButton_ShowAdjacencies.setEnabled(false);
        } else {
            gButton_ShowAdjacencies.setEnabled(true);
        }

        //Start the tag selection fragment:
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        gFragment_selectTags = new Fragment_SelectTags();
        Bundle args = new Bundle();
        args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, viewModelImportActivity.iImportMediaCategory);
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
                TextView textView_SelectedTags = getView().findViewById(R.id.textView_SelectedTags);
                if (textView_SelectedTags != null) {
                    textView_SelectedTags.setText(sb.toString());
                }
            }
        }

        //Create the ExoPlayer.
        gExoPlayer = new ExoPlayer.Builder(getActivity().getApplicationContext()).build();
        gExoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        PlayerView gplayerView_ExoVideoPlayer = getView().findViewById(R.id.playerView_ExoVideoPlayer);
        gplayerView_ExoVideoPlayer.setPlayer(gExoPlayer);


        gImagePreview = getView().findViewById(R.id.imageView_ImagePreview);

        gImageButton_NextItem = getView().findViewById(R.id.imageButton_NextItem);
        if(gImageButton_NextItem != null){
            gImageButton_NextItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    iterateToGreaterIndexedItem();
                }
            });
        }

        gImageButton_PreviousItem = getView().findViewById(R.id.imageButton_PreviousItem);
        if(gImageButton_PreviousItem != null){
            gImageButton_PreviousItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    iterateToLesserIndexedItem();
                }
            });
        }

        setNextPrevButtonVisibilities();

        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            long lVideoDuration = galFileItems.get(giFileItemIndex).lVideoTimeInMilliseconds;
            if (lVideoDuration < 0L) {
                //If there is no video length, exit this activity.
                Toast.makeText(getContext(),"No video length.", Toast.LENGTH_SHORT).show();
            }
            gplayerView_ExoVideoPlayer.bringToFront();
            gplayerView_ExoVideoPlayer.setVisibility(View.VISIBLE);

        } else {

            gImagePreview.bringToFront();

        }



        //Add a response receiver to listen for responses from the adjacency analyzer worker.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_Adjaceny_Analyzer.CATALOG_ADJAN_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importFilePreviewResponseReceiver = new ImportFilePreviewResponseReceiver();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(importFilePreviewResponseReceiver, filter);

        gRelativeLayout_Adjacencies = getView().findViewById(R.id.relativeLayout_Adjacencies);

        gTextView_AdjacencyCount			= getView().findViewById(R.id.textView_AdjacencyCount			);
        gTextView_FileNameMatchCount		= getView().findViewById(R.id.textView_FileNameMatchCount		);
        gTextView_DateModifiedMatchCount	= getView().findViewById(R.id.textView_DateModifiedMatchCount	);
        gTextView_ResolutionMatchCount		= getView().findViewById(R.id.textView_ResolutionMatchCount	);
        gTextView_DurationMatchCount		= getView().findViewById(R.id.textView_DurationMatchCount		);

        gButton_ShowAdjacencies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gRelativeLayout_Adjacencies.setVisibility(View.VISIBLE);
                gbLookForFileAdjacencies = true;
                view.setEnabled(false);
                updateAdjacencies();
            }
        });

        ImageButton imageButton_CloseAdjacencies = getView().findViewById(R.id.imageButton_CloseAdjacencies);
        imageButton_CloseAdjacencies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gRelativeLayout_Adjacencies.setVisibility(View.INVISIBLE);
                gbLookForFileAdjacencies = false;
                gButton_ShowAdjacencies.setEnabled(true);
            }
        });

        //Configure the maturity filter rangeslider:
        RangeSlider rangeSlider_MaturityFilter = getView().findViewById(R.id.rangeSlider_MaturityFilter);
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

        if(viewModelImportActivity == null){
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
        }
        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            if(gExoPlayer != null) {
                gExoPlayer.seekTo(glCurrentVideoPosition);
                if (giCurrentVideoPlaybackState == VIDEO_PLAYBACK_STATE_PLAYING) {
                    gExoPlayer.play();
                }
            }
        }
    }

    @Override
    public void onPause() {
        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            gExoPlayer.stop(); //Need to tell it to stop rather than pause otherwise coming & going from the
                               // fragment multiple times causes a crash on "out-of-memory".
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            gExoPlayer.stop();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            outState.putLong(PLAYBACK_TIME, gExoPlayer.getCurrentPosition());
        }
        outState.putInt(IMAGE_PREVIEW_INDEX, giFileItemIndex);
    }

    @Override
    public void onDestroy() {
        if(getActivity() == null){
            return;
        }
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(importFilePreviewResponseReceiver);
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
                        GlobalClass.ShowMessage(getContext(), "Adjacency Analysis", sMessage);
                        gbLookForFileAdjacencies = false; //Don't automatically seek adjacencies again unless the user click the button.
                        gButton_ShowAdjacencies.setEnabled(true); //Allow the user to click the button to start looking at adjacencies again.

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
            if(getActivity() == null){
                return;
            }
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
                Glide.with(getActivity().getApplicationContext())
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
                    Glide.with(getActivity().getApplicationContext())
                            .load(uriThumbnailUri)
                            .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                            .into(holder.ivThumbnail);
                } else {
                    Glide.with(getActivity().getApplicationContext())
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

            sThumbnailText = sThumbnailText + "\n" +
                    "Location: " + GlobalClass.gsCatalogFolderNames[GlobalClass.giSelectedCatalogMediaCategory] +
                    GlobalClass.cleanHTMLCodedCharacters(GlobalClass.gsFileSeparator + ci.sFolderRelativePath);

            holder.tvThumbnailText.setText(sThumbnailText);
            final Uri uriThumbnailUriForMagnify = uriThumbnailUri;
            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Create popup context menu with options for the user.
                    PopupMenu popupMenuAdjacencyItemOptions = new PopupMenu(vgParent.getContext(), v);
                    popupMenuAdjacencyItemOptions.getMenuInflater().inflate(R.menu.adjacency_action_menu, popupMenuAdjacencyItemOptions.getMenu());
                    if(ci.sGroupID.equals("")){
                        popupMenuAdjacencyItemOptions.getMenu().removeItem(R.id.menu_CopyGroupID);
                    }
                    popupMenuAdjacencyItemOptions.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if(getView() == null){
                                return true;
                            }
                            int itemId = item.getItemId();

                            if (itemId == R.id.menu_ApplyTagsFromItem) {
                                //Apply the tags associated with this catalog item to the potential import item.
                                gFragment_selectTags.gListViewTagsAdapter.selectTagsByIDs(ci.aliTags);
                                return true;

                            } else if (itemId == R.id.menu_CopyFileName) {
                                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("Filename", ci.sFilename);
                                clipboard.setPrimaryClip(clip);
                                return true;

                            } else if (itemId == R.id.menu_CopyGroupID) {
                                GlobalClass.gsGroupIDClip = ci.sGroupID;
                                recalcGroupButtonVisibilities();
                                return true;

                            } else if (itemId == R.id.menu_Magnify) {

                                final RelativeLayout relativeLayout_SimgleImageMagnify = getView().findViewById(R.id.relativeLayout_SimgleImageMagnify);
                                relativeLayout_SimgleImageMagnify.setVisibility(View.VISIBLE);

                                AppCompatImageView acImageView_SingleImageMagnify = getView().findViewById(R.id.acImageView_SingleImageMagnify);
                                Glide.with(getActivity().getApplicationContext()).load(uriThumbnailUriForMagnify).into(acImageView_SingleImageMagnify);
                                acImageView_SingleImageMagnify.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        relativeLayout_SimgleImageMagnify.setVisibility(View.INVISIBLE);
                                    }
                                });

                                TextView textView_Fader = getView().findViewById(R.id.textView_Fader);
                                textView_Fader.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        relativeLayout_SimgleImageMagnify.setVisibility(View.INVISIBLE);
                                    }
                                });

                                return true;

                            } else if (itemId == R.id.menu_SideBySide) {

                                final RelativeLayout relativeLayout_MagnifyImageSideBySide = getView().findViewById(R.id.relativeLayout_MagnifyImageSideBySide);
                                relativeLayout_MagnifyImageSideBySide.setVisibility(View.VISIBLE);

                                AppCompatImageView acImageView_SideBySideImageA = getView().findViewById(R.id.acImageView_SideBySideImageA);
                                //Glide.with(getApplicationContext()).load(galFileItems.get(giFileItemIndex).sUri).into(acImageView_SideBySideImageA);
                                acImageView_SideBySideImageA.setImageURI(Uri.parse(galFileItems.get(giFileItemIndex).sUri));
                                acImageView_SideBySideImageA.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        relativeLayout_MagnifyImageSideBySide.setVisibility(View.INVISIBLE);
                                    }
                                });

                                AppCompatImageView acImageView_SideBySideImageB = getView().findViewById(R.id.acImageView_SideBySideImageB);
                                //Glide.with(getApplicationContext()).load(uriThumbnailUriForMagnify).into(acImageView_SideBySideImageB);
                                acImageView_SideBySideImageA.setImageURI(uriThumbnailUriForMagnify);
                                acImageView_SideBySideImageB.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        relativeLayout_MagnifyImageSideBySide.setVisibility(View.INVISIBLE);
                                    }
                                });

                                TextView textView_Fader2 = getView().findViewById(R.id.textView_Fader2);
                                textView_Fader2.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        relativeLayout_MagnifyImageSideBySide.setVisibility(View.INVISIBLE);
                                    }
                                });

                                return true;
                            } else {
                                return true;
                            }

                        }
                    });

                    popupMenuAdjacencyItemOptions.show();

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