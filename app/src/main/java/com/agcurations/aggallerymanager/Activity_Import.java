package com.agcurations.aggallerymanager;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public class Activity_Import extends AppCompatActivity {
    private GlobalClass globalClass;

    public ViewPager2 ViewPager2_Import;
    FragmentImportViewPagerAdapter importViewPagerFragmentAdapter;

    //Fragment page indexes:
    public static final int FRAGMENT_IMPORT_0_ID_MEDIA_CATEGORY = 0;
    public static final int FRAGMENT_IMPORT_0A_ID_VIDEO_SOURCE = 1;
    public static final int FRAGMENT_IMPORT_0B_ID_COMIC_SOURCE = 2;
    public static final int FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION = 3;
    public static final int FRAGMENT_IMPORT_1A_ID_VIDEO_WEB_DETECT = 4;
    public static final int FRAGMENT_IMPORT_1B_ID_WEB_ADDRESS = 5;
    public static final int FRAGMENT_IMPORT_2_ID_SELECT_ITEMS = 6;
    public static final int FRAGMENT_IMPORT_2A_ID_SELECT_DETECTED_WEB_VIDEO_ITEM = 7;
    public static final int FRAGMENT_IMPORT_3_ID_SELECT_TAGS = 8;
    public static final int FRAGMENT_IMPORT_3A_ITEM_DOWNLOAD_TAG_IMPORT = 9;
    public static final int FRAGMENT_IMPORT_4_ID_IMPORT_METHOD = 10;
    public static final int FRAGMENT_IMPORT_5_ID_CONFIRMATION = 11;
    public static final int FRAGMENT_IMPORT_2B_SELECT_SINGLE_WEB_COMIC = 12;
    public static final int FRAGMENT_IMPORT_6_ID_EXECUTE_IMPORT = 13;

    public static final int FRAGMENT_COUNT = 14;

    //=================================================
    //User selection global variables:

    //FragmentImport_1_StorageLocation
    public static Uri guriImportTreeURI; //Uri of selected base folder holding files/folders to be imported.

    //FragmentImport_2_SelectItems
    public static int SelectItemsListViewWidth = 1000;  //Expands the listView items to the width of the listview.
    public FileListCustomAdapter fileListCustomAdapter; //For the file selector.
    public VideoDownloadListCustomAdapter videoDownloadListCustomAdapter; //For the video download selector.
    public static final String PREVIEW_FILE_ITEMS = "PREVIEW_FILE_ITEMS";
    public static final String PREVIEW_FILE_ITEMS_POSITION = "PREVIEW_FILE_ITEMS_POSITION";
    public static final String IMPORT_SESSION_TAGS_IN_USE = "IMPORT_SESSION_TAGS_IN_USE";
    public static final String TAG_SELECTION_RESULT_BUNDLE = "TAG_SELECTION_RESULT_BUNDLE";
    public static final String MEDIA_CATEGORY = "MEDIA_CATEGORY";

    public static final String EXTRA_INT_MEDIA_CATEGORY = "EXTRA_INT_MEDIA_CATEGORY";
    //If the import routine is being started from somewhere other than
    //generic menu item, it must be in an area applicable to a particular media type.

    //FragmentImport_3_SelectTags
    public static ViewModel_Fragment_SelectTags viewModelTags; //Used for applying tags globally to an entire import selection.
    public static ViewModel_ImportActivity viewModelImportActivity; //Used to transfer data between fragments.

    //FragmentImport_4_ImportMethod


    //=================================================

    static MediaMetadataRetriever mediaMetadataRetriever;

    static Stack<Integer> stackFragmentOrder;
    private static int giStartingFragment;

    boolean gbWriteApplicationLog = false;
    String gsApplicationLogFilePath = "";

    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        setTitle("Import");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if(gbWriteApplicationLog){
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        if(globalClass.giSelectedCatalogMediaCategory == null){
            ApplicationLogWriter("Selected media category is null. Returning to Main Activity.");
            finish();
            return;
        }

        ViewPager2_Import = findViewById(R.id.viewPager_Import);

        importViewPagerFragmentAdapter = new FragmentImportViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        // set Orientation in your ViewPager2
        ViewPager2_Import.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        ViewPager2_Import.setAdapter(importViewPagerFragmentAdapter);

        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        ViewPager2_Import.setUserInputEnabled(false);

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();
        //registerReceiver(importDataServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(importDataServiceResponseReceiver,filter);

        mediaMetadataRetriever = new MediaMetadataRetriever();

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        viewModelTags = new ViewModelProvider(this).get(ViewModel_Fragment_SelectTags.class);

        //Instantiate the ViewModel sharing data between fragments:
        viewModelImportActivity = new ViewModelProvider(this).get(ViewModel_ImportActivity.class);

        stackFragmentOrder = new Stack<>();


        if(globalClass.gbImportFolderAnalysisRunning && !globalClass.gbImportFolderAnalysisFinished){
            //If a folder analysis operation has been started and is not finished, go to the storage
            // location fragment which should show the analysis progress.
            giStartingFragment = FRAGMENT_IMPORT_0_ID_MEDIA_CATEGORY;
            stackFragmentOrder.push(giStartingFragment); //DO allow user to go back to media select.
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION, false);
            stackFragmentOrder.push(FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION);

        } else if(globalClass.gbImportExecutionRunning && !globalClass.gbImportExecutionFinished){
            //If an import operation has been started and is not finished, go to the execute
            //  fragment which will show the user the log.
            giStartingFragment = FRAGMENT_IMPORT_6_ID_EXECUTE_IMPORT;
            ViewPager2_Import.setCurrentItem(giStartingFragment, false);
            stackFragmentOrder.push(giStartingFragment);

        } else if(globalClass.gbImportComicWebAnalysisRunning && !globalClass.gbImportComicWebAnalysisFinished){
            //If a comic web analysis operation has been started and is not finished, go to the appropriate
            // fragment.
            giStartingFragment = FRAGMENT_IMPORT_2B_SELECT_SINGLE_WEB_COMIC;
            ViewPager2_Import.setCurrentItem(giStartingFragment, false);
            stackFragmentOrder.push(giStartingFragment);

        } else {

            giStartingFragment = FRAGMENT_IMPORT_0_ID_MEDIA_CATEGORY;
            //Check to see if this activity has been started by an activity desiring mods to a
            //  particular media category:
            Intent iStartingIntent = getIntent();
            if(iStartingIntent != null){
                int iMediaCategory = iStartingIntent.getIntExtra(EXTRA_INT_MEDIA_CATEGORY, -1);
                if(iMediaCategory != -1){
                    viewModelImportActivity.iImportMediaCategory = iMediaCategory;

                    if(iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                        giStartingFragment = FRAGMENT_IMPORT_0A_ID_VIDEO_SOURCE;
                    } else if(iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                        giStartingFragment = FRAGMENT_IMPORT_0B_ID_COMIC_SOURCE;
                    } else {
                        giStartingFragment = FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION;
                    }
                    gotoMediaCategorySelectedFragment(iMediaCategory);
                } else {
                    stackFragmentOrder.push(giStartingFragment);
                }
            }

        }

    }

    private void ApplicationLogWriter(String sMessage){
        if(gbWriteApplicationLog){
            try {
                File fLog = new File(gsApplicationLogFilePath);
                FileWriter fwLogFile = new FileWriter(fLog, true);
                fwLogFile.write(GlobalClass.GetTimeStampReadReady() + ": " + this.getLocalClassName() + ", " + sMessage + "\n");
                fwLogFile.close();
            } catch (Exception e) {
                Log.d("Log FileWriter", e.getMessage());
            }
        }

    }

    @Override
    protected void onDestroy() {
        mediaMetadataRetriever.release();
        //unregisterReceiver(importDataServiceResponseReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(importDataServiceResponseReceiver);
        super.onDestroy();
    }

    @SuppressWarnings("unchecked")
    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String IMPORT_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_IMPORT_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            //Get boolean indicating that an error may have occurred:
            boolean bError = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_PROBLEM,false);

            if (bError) {
                String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to request to get directory contents:
                boolean bGetDirContentsResponse = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, false);
                if (bGetDirContentsResponse) {
                    ArrayList<ItemClass_File> alFileList = (ArrayList<ItemClass_File>) intent.getSerializableExtra(Service_Import.EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE);
                    fileListCustomAdapter = new FileListCustomAdapter(getApplicationContext(), R.id.listView_FolderContents, alFileList);
                }

                //Check to see if this is a response to request to get video downloads from html:
                boolean bGetVideoDownloadsResponse = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE, false);
                if (bGetVideoDownloadsResponse) {
                    ArrayList<ItemClass_File> alicf_VideoDownloadFileItems = (ArrayList<ItemClass_File>) intent.getSerializableExtra(Service_Import.EXTRA_AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE);
                    videoDownloadListCustomAdapter = new VideoDownloadListCustomAdapter(getApplicationContext(), R.id.listView_FolderContents, alicf_VideoDownloadFileItems);
                }

            }


        }
    }




    ActivityResultLauncher<Intent> garlGetTagsForImportItems = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == RESULT_OK){
                        Intent data = result.getData();
                        if(data != null) {
                            Bundle b = data.getBundleExtra(TAG_SELECTION_RESULT_BUNDLE);
                            if (b == null) return;
                            ItemClass_File[] fileItems = (ItemClass_File[]) b.getSerializable(PREVIEW_FILE_ITEMS);
                            if (fileItems == null) return;
                            //ArrayList<Integer> aliTagIDs;
                            //aliTagIDs = b.getIntegerArrayList(TAG_SELECTION_TAG_IDS);
                            //Apply the change to the fileListCustomAdapter:
                            //fileListCustomAdapter.updateFileItemTags(fileItems[0].uri, aliTagIDs);
                            fileListCustomAdapter.updateFileItemDetails(fileItems);
                        }
                    }
                }
            });



    //======================================================
    //======  FRAGMENT NAVIGATION ROUTINES  ================
    //======================================================



    @Override
    public void onBackPressed() {

        if(stackFragmentOrder.empty()){
            gotoFinish();
        } else {
            //Go back through the fragments in the order by which we progressed.
            //  Some user selections will cause a fragment to be skipped, and we don't want
            //  to go back to those skipped fragments, hence the use of a Stack, and pop().
            int iCurrentFragment = ViewPager2_Import.getCurrentItem();
            int iPrevFragment = stackFragmentOrder.pop();
            if((iCurrentFragment == iPrevFragment) && (iCurrentFragment == giStartingFragment)){
                gotoFinish();
                return;
            }

            if(iCurrentFragment == iPrevFragment){
                //To handle interesting behavior about how the stack is built.
                iPrevFragment = stackFragmentOrder.peek();
            }
            ViewPager2_Import.setCurrentItem(iPrevFragment, false);

            if(iPrevFragment == giStartingFragment){
                //There is no item to push '0' onto the fragment order stack. Do it here:
                stackFragmentOrder.push(giStartingFragment);
            }
        }
    }

    public void buttonNextClick_MediaCategorySelected(View v){
        RadioButton radioButton_ImportVideos = findViewById(R.id.radioButton_ImportVideos);
        RadioButton radioButton_ImportImages = findViewById(R.id.radioButton_ImportImages);
        //RadioButton rbComics = findViewById(R.id.radioButton_ImportComics);

        int iNewImportMediaCatagory;
        if (radioButton_ImportVideos.isChecked()){
            iNewImportMediaCatagory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        } else if (radioButton_ImportImages.isChecked()){
            iNewImportMediaCatagory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        } else {
            iNewImportMediaCatagory = GlobalClass.MEDIA_CATEGORY_COMICS;
        }

        gotoMediaCategorySelectedFragment(iNewImportMediaCatagory);

    }

    public void gotoMediaCategorySelectedFragment(int iNewImportMediaCatagory){

        if(iNewImportMediaCatagory != viewModelImportActivity.iImportMediaCategory) {
            viewModelImportActivity.bImportCategoryChange = true; //Force user to select new import folder (in the event that they backtracked).
            globalClass.gbImportExecutionStarted = false;
            if(globalClass.gbImportFolderAnalysisRunning){
                globalClass.gbImportFolderAnalysisStop = true;
            }
            viewModelImportActivity.iImportMediaCategory = iNewImportMediaCatagory;
        }

        //Go to the import folder selection fragment:
        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_0A_ID_VIDEO_SOURCE, false); //Prompt user to select video source.
        } else if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_0B_ID_COMIC_SOURCE, false); //Prompt user to select comic source.
        } else {
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION, false);
        }
        stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());
    }

    public void buttonNextClick_ComicSourceSelected(View v){
        RadioButton radioButton_ComicSourceFolder = findViewById(R.id.radioButton_ComicSourceFolder);

        int iNewComicSource;

        if (radioButton_ComicSourceFolder.isChecked()){
            iNewComicSource = ViewModel_ImportActivity.COMIC_SOURCE_FOLDER;
        } else {
            iNewComicSource = ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE;
        }

        if(iNewComicSource != viewModelImportActivity.iComicImportSource){
            viewModelImportActivity.bImportCategoryChange = true;
            viewModelImportActivity.iComicImportSource = iNewComicSource;
        }


        //Go to the import folder selection fragment:
        if(viewModelImportActivity.iComicImportSource != ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE) {
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION, false);
        } else { //Allow user to import web address of a comic to import.
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_1B_ID_WEB_ADDRESS, false);
        }
        stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());
    }

    public void buttonNextClick_VideoSourceSelected(View v){
        RadioButton radioButton_VideoSourceFolder = findViewById(R.id.radioButton_VideoSourceFolder);
        //RadioButton radioButton_VideoSourceWebpage = findViewById(R.id.radioButton_VideoSourceWebpage);

        int iNewVideoSource;

        if (radioButton_VideoSourceFolder.isChecked()){
            iNewVideoSource = ViewModel_ImportActivity.VIDEO_SOURCE_FOLDER;
        } else {
            iNewVideoSource = ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE;
        }

        if(iNewVideoSource != viewModelImportActivity.iVideoImportSource){
            viewModelImportActivity.bImportCategoryChange = true;
            viewModelImportActivity.iVideoImportSource = iNewVideoSource;
        }

        //Go to the import folder selection fragment:
        if(viewModelImportActivity.iVideoImportSource != ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE) {
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION, false);
        } else { //Allow user to specify web address of a video to import.
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_1A_ID_VIDEO_WEB_DETECT, false);
        }
        stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());
    }

    public void buttonNextClick_StorageImportFromLocation(View v){
        //Go to the import folder selection fragment:
        if(viewModelImportActivity.iImportMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS) {
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_2_ID_SELECT_ITEMS, false);
        } else {
            if(viewModelImportActivity.iComicImportSource != ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE) {
                ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_2_ID_SELECT_ITEMS, false);
            }
        }
        stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());
    }

    public void buttonNextClick_VideoWebDetect(View v){
        //Go to the video download selection fragment:
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_2A_ID_SELECT_DETECTED_WEB_VIDEO_ITEM, false);
        stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());
    }

    public void buttonNextClick_WebAddress(View v){
        //Done with entering the WebAddress
        if(!viewModelImportActivity.sWebAddress.equals("")) {

            globalClass.gbImportComicWebAnalysisStarted = true;
            globalClass.gbImportComicWebAnalysisFinished = false;
            giStartingFragment = FRAGMENT_IMPORT_2B_SELECT_SINGLE_WEB_COMIC; //Don't allow user to go back.
            ViewPager2_Import.setCurrentItem(giStartingFragment, false);
            stackFragmentOrder.clear();
            stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());

        } else {
            Toast.makeText(getApplicationContext(), "The provided web address is currently incompatible with this version of the app." , Toast.LENGTH_SHORT).show();
        }
    }

    public void buttonNextClick_ItemSelectComplete(View v){
        if((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS
        && viewModelImportActivity.iVideoImportSource == ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE)
        || (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS
                && viewModelImportActivity.iVideoImportSource == ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE)){
            //If we are importing a video or comic from the web, allow the user to confirm import
            // of tags that don't already exist in the list of tags.
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_3A_ITEM_DOWNLOAD_TAG_IMPORT, false);
        } else {
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_3_ID_SELECT_TAGS, false);
        }
        stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());
    }

    public void buttonNextClick_TagImportSelectionComplete(View v){
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_3_ID_SELECT_TAGS, false);
        stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());
    }

    public void buttonNextClick_TagSelectComplete(View v){

        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS
                && viewModelImportActivity.iVideoImportSource == ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE){
            //If we are importing a video from the web, go to import confirm.
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_5_ID_CONFIRMATION, false);
        } else {
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_4_ID_IMPORT_METHOD, false);
        }

    }

    public void buttonNextClick_ImportMethodComplete(View v){
        RadioButton rbMove = findViewById(R.id.radioButton_MoveFiles);

        if (rbMove.isChecked()){
            viewModelImportActivity.iImportMethod = ViewModel_ImportActivity.IMPORT_METHOD_MOVE;
        } else{
            viewModelImportActivity.iImportMethod = ViewModel_ImportActivity.IMPORT_METHOD_COPY;
        }

        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_5_ID_CONFIRMATION, false);
        stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());
    }

    public void buttonNextClick_ImportConfirm(View v){
        globalClass.gbImportExecutionStarted = true;
        globalClass.gbImportExecutionFinished = false;
        giStartingFragment = FRAGMENT_IMPORT_6_ID_EXECUTE_IMPORT; //Don't allow user to go back.
        ViewPager2_Import.setCurrentItem(giStartingFragment, false);
        stackFragmentOrder.clear();
        stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());
    }

    public void buttonNextClick_ImportFinish(View v){
        gotoFinish();
    }

    public void buttonClick_ImportRestart(View v) {
        stackFragmentOrder.empty();
        stackFragmentOrder.push(0);
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_0_ID_MEDIA_CATEGORY, false);
        viewModelImportActivity.bImportCategoryChange = true; //This forces re-select of import dir.
    }

    public void buttonClick_Cancel(View v){
        if(globalClass.gbImportFolderAnalysisRunning){
            globalClass.gbImportFolderAnalysisStop = true;
        }
        gotoFinish();
    }

    public void gotoFinish(){
        //Code any pre-finish operations here.
        finish();
    }
    
    //================================================
    //  Adapters
    //================================================

    public class FileListCustomAdapter extends ArrayAdapter<ItemClass_File> {

        final public ArrayList<ItemClass_File> alFileItems;
        private ArrayList<ItemClass_File> alFileItemsDisplay;
        private boolean bSelectAllSelected = false;
        Context contextFromCaller;

        public FileListCustomAdapter(Context context, int textViewResourceId, ArrayList<ItemClass_File> alfi) {
            super(context, textViewResourceId, alfi);
            contextFromCaller = context;
            alFileItems = new ArrayList<>(alfi);

            if (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_NH_COMIC_DOWNLOADER) {
                    //If importing comics and importing NHComicDownloader files as the source, filter on the cover pages:
                    alFileItemsDisplay = new ArrayList<>(); //initialize.
                    applyFilter(GlobalClass.gsNHComicCoverPageFilter);
                } else if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                    //If importing comics and importing folder files as the source, filter on the folder items:
                    alFileItemsDisplay = new ArrayList<>(); //initialize.
                    applyFilterByType(ItemClass_File.TYPE_FOLDER);
                }

            } else {
                alFileItemsDisplay = new ArrayList<>(alfi);
            }

            SortByFileNameAsc();
        }

        @Override
        @NonNull
        public View getView(final int position, View v, @NonNull ViewGroup parent) {
            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                //My custom list item design is here
                row = inflater.inflate(R.layout.listview_fileitem_selectable, parent, false);
            }

            CheckBox cbStorageItemSelect =  row.findViewById(R.id.checkBox_StorageItemSelect);
            ImageView ivFileType =  row.findViewById(R.id.imageView_StorageItemIcon);
            TextView tvLine1 =  row.findViewById(R.id.textView_Line1);
            TextView tvLine2 = row.findViewById(R.id.textView_Line2);
            TextView tvLine3 = row.findViewById(R.id.textView_Line3);

            tvLine1.setText(alFileItemsDisplay.get(position).sFileOrFolderName);
            DateFormat dfDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.getDefault() );
            String sLine2 = dfDateFormat.format(alFileItemsDisplay.get(position).dateLastModified);


            //If type is video or gif, get the duration:
            boolean bIsVideoOrGif = (alFileItemsDisplay.get(position).sMimeType.startsWith("video")) ||
                    (alFileItemsDisplay.get(position).sExtension.contentEquals(".gif")) ||
                    (alFileItemsDisplay.get(position).sMimeType.equals("application/octet-stream") && alFileItemsDisplay.get(position).sExtension.equals(".mp4"));
            if(bIsVideoOrGif) {
                long durationInMilliseconds = alFileItemsDisplay.get(position).lVideoTimeInMilliseconds;
                try {
                    if (durationInMilliseconds == -1L) { //If the time has not already been determined for the video file...
                        if (alFileItemsDisplay.get(position).sMimeType.startsWith("video")) {
                            Uri docUri = Uri.parse(alFileItemsDisplay.get(position).sUri);
                            mediaMetadataRetriever.setDataSource(getApplicationContext(), docUri);
                            String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            durationInMilliseconds = Long.parseLong(time);
                        } else { //if it's not a video file, check to see if it's a gif:
                            if (alFileItemsDisplay.get(position).sExtension.contentEquals(".gif")) {
                                //Get the duration of the gif image:
                                Uri docUri = Uri.parse(alFileItemsDisplay.get(position).sUri);
                                Context activityContext = getApplicationContext();
                                pl.droidsonroids.gif.GifDrawable gd = new pl.droidsonroids.gif.GifDrawable(activityContext.getContentResolver(), docUri);
                                durationInMilliseconds = gd.getDuration();
                            }
                        }
                        if (durationInMilliseconds != -1L) { //If time is now defined, get the text form of the time:
                            alFileItemsDisplay.get(position).lVideoTimeInMilliseconds = durationInMilliseconds;
                        }
                    }
                    if (durationInMilliseconds > -1L) {
                        alFileItemsDisplay.get(position).sVideoTimeText = GlobalClass.getDurationTextFromMilliseconds(durationInMilliseconds);
                    }

                    if (alFileItemsDisplay.get(position).sVideoTimeText.length() > 0) {
                        //If the video time text has been defined, recall and display the time:
                        sLine2 = sLine2 + "\tDuration: " + alFileItemsDisplay.get(position).sVideoTimeText;
                    }

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage() + "; File: " + alFileItemsDisplay.get(position).sFileOrFolderName, Toast.LENGTH_LONG).show();
                }
            }

            tvLine2.setText(sLine2);

            //Get tag text to apply to list item if tags are assigned to the item:
            StringBuilder sbTags = new StringBuilder();
            sbTags.append("Tags: ");
            ArrayList<Integer> aliPreConfirmedTagIDs = alFileItemsDisplay.get(position).aliProspectiveTags;

            //Confirm tag exists before displaying (user can delete tags using preview->tagEditor)
            boolean bTagsChanged = false;
            ArrayList<Integer> aliConfirmedTagIDs = new ArrayList<>();
            for(int iTagID: aliPreConfirmedTagIDs){
                if(!globalClass.TagIDExists(iTagID, viewModelImportActivity.iImportMediaCategory)){
                    bTagsChanged = true;
                } else {
                    aliConfirmedTagIDs.add(iTagID);
                }
            }
            if(bTagsChanged){
                alFileItemsDisplay.get(position).aliProspectiveTags = aliConfirmedTagIDs;
            }

            ArrayList<Integer> aliTagIDs = alFileItemsDisplay.get(position).aliProspectiveTags;

            if(aliTagIDs != null){
                if(aliTagIDs.size() > 0) {
                    sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(0), viewModelImportActivity.iImportMediaCategory));
                    for (int i = 1; i < aliTagIDs.size(); i++) {
                        sbTags.append(", ");
                        sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(i), viewModelImportActivity.iImportMediaCategory));
                    }
                }
            }
            tvLine3.setText(sbTags.toString());

            //set the image type if folder or file
            if(alFileItemsDisplay.get(position).iTypeFileFolderURL == ItemClass_File.TYPE_FOLDER) {
                //Get the Uri of the file and create/display a thumbnail:
                String sUri = alFileItemsDisplay.get(position).sUriThumbnailFile;
                Uri uri = Uri.parse(sUri);
                Glide.with(getContext()).
                        load(uri).
                        into(ivFileType);
            } else {
                //Get the Uri of the file and create/display a thumbnail:
                String sUri = alFileItemsDisplay.get(position).sUri;
                Uri uri = Uri.parse(sUri);
                Glide.with(getContext()).
                        load(uri).
                        into(ivFileType);
            }


            cbStorageItemSelect.setChecked(alFileItemsDisplay.get(position).bIsChecked);

            //Expand the width of the listItem to the width of the ListView.
            //  This makes it so that the listItem responds to the click even when
            //  the click is off of the text.
            row.setMinimumWidth(SelectItemsListViewWidth);

            //Set the onClickListener for the row to toggle the checkbox:
            row.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    CheckBox checkBox_StorageItemSelect = view.findViewById(R.id.checkBox_StorageItemSelect);
                    boolean bNewCheckedState = !checkBox_StorageItemSelect.isChecked();
                    checkBox_StorageItemSelect.setChecked(bNewCheckedState);
                    alFileItemsDisplay.get(position).bIsChecked = bNewCheckedState;
                    toggleItemChecked(position, bNewCheckedState);

                }
            });


            //Set the onClickListener for the checkbox to toggle the checkbox:
            CheckBox checkBox_StorageItemSelect = row.findViewById(R.id.checkBox_StorageItemSelect);
            checkBox_StorageItemSelect.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    CheckBox checkBox_StorageItemSelect = (CheckBox) view;
                    boolean bNewCheckedState = checkBox_StorageItemSelect.isChecked();
                    alFileItemsDisplay.get(position).bIsChecked = bNewCheckedState;
                    toggleItemChecked(position, bNewCheckedState);

                }
            });

            //Code the button to delete a file in the ListView:
            Button button_Delete = row.findViewById(R.id.button_Delete);
            button_Delete.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(Activity_Import.this, R.style.AlertDialogCustomStyle);
                    builder.setTitle("Delete Item");
                    builder.setMessage("Are you sure you want to delete this item?\n" + alFileItemsDisplay.get(position).sFileOrFolderName);
                    //builder.setIcon(R.drawable.ic_launcher);
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            if(viewModelImportActivity.iImportMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS) {
                                Uri uriSourceFile;
                                uriSourceFile = Uri.parse(alFileItemsDisplay.get(position).sUri);
                                DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);

                                if (dfSource != null) {
                                    String sMessage;
                                    if (!dfSource.delete()) {
                                        sMessage = "Could not delete file.";
                                    } else {
                                        sMessage = "File deleted.";
                                    }
                                    Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_LONG).show();
                                }
                                //Find the item in the alFileItems list and delete it:
                                ItemClass_File fiSelected = alFileItemsDisplay.get(position);
                                ItemClass_File fiSource;
                                for (int i = 0; i < alFileItems.size(); i++) {
                                    fiSource = alFileItems.get(i);
                                    if (fiSelected.sFileOrFolderName.equals(fiSource.sFileOrFolderName)) {
                                        alFileItems.remove(i);
                                        break;
                                    }
                                }
                                alFileItemsDisplay.remove(position);
                                notifyDataSetChanged();
                            } else {
                                if(viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_NH_COMIC_DOWNLOADER) {

                                    String sComicID = Service_Import.GetNHComicID(alFileItemsDisplay.get(position).sFileOrFolderName);
                                    int iFailedDeletions = 0;
                                    int iTotalDeletions = 0;

                                    ItemClass_File fiSource;
                                    for (int i = 0; i < alFileItems.size(); i++) {
                                        fiSource = alFileItems.get(i);
                                        if(fiSource.sFileOrFolderName.startsWith(sComicID)){
                                            Uri uriSourceFile = Uri.parse(fiSource.sUri);
                                            DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);
                                            if (dfSource != null) {
                                                if (dfSource.delete()) {
                                                    iTotalDeletions++;
                                                } else {
                                                    iFailedDeletions++;
                                                }
                                            }
                                            alFileItems.remove(i);
                                        }
                                    }
                                    String sMessage;
                                    if(iFailedDeletions == 0) {
                                        sMessage = "Comic deleted: " + alFileItemsDisplay.get(position).sFileOrFolderName;
                                    } else {
                                        sMessage = "Failed to delete " + iFailedDeletions + "/" + iTotalDeletions + " files belonging to the comic.";
                                    }
                                    alFileItemsDisplay.remove(position);
                                    Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                                    notifyDataSetChanged();

                                } else if(viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {

                                    String sComicParentUri = alFileItemsDisplay.get(position).sUri;

                                    ArrayList<String> alsUriFilesToDelete = new ArrayList<>();
                                    ItemClass_File fiSource;

                                    //Mark comic page files for deletion, and remove from alFileItems:
                                    String sMessage;
                                    for (int i = 0; i < alFileItems.size(); i++) {
                                        fiSource = alFileItems.get(i);
                                        if(fiSource.sUriParent.equals(sComicParentUri)){
                                            alsUriFilesToDelete.add(fiSource.sUri);
                                            alFileItems.remove(i);
                                        }
                                    }

                                    //Mark comic folder for deletion and remove from alFileItemsDisplay:
                                    alsUriFilesToDelete.add(alFileItemsDisplay.get(position).sUri);
                                    alFileItemsDisplay.remove(position);

                                    //Refresh the listView:
                                    notifyDataSetChanged();

                                    //Start the file delete service:
                                    Service_Import.startActionDeleteFiles(getApplicationContext(), alsUriFilesToDelete,
                                            Fragment_Import_2_SelectItems.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_SELECT_ITEMS_RESPONSE);

                                }
                            }
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });

            //If the file item is video mimeType, set the preview button visibility to visible:
            Button button_MediaPreview = row.findViewById(R.id.button_MediaPreview);
            boolean bItemIsVideo = alFileItemsDisplay.get(position).sMimeType.startsWith("video")  ||
                    (alFileItemsDisplay.get(position).sMimeType.equals("application/octet-stream") &&
                            alFileItemsDisplay.get(position).sExtension.equals(".mp4"));//https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid)

            //button_MediaPreview.setVisibility(Button.VISIBLE);
            button_MediaPreview.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    //Start the preview popup activity:
                    Intent intentPreviewPopup;
                    intentPreviewPopup = new Intent(Activity_Import.this, Activity_ImportFilePreview.class);


                    Bundle b = new Bundle();
                    b.putInt(MEDIA_CATEGORY,
                            viewModelImportActivity.iImportMediaCategory); //viewModel not intended
                    // to be used between Activities. Therefore, pass media category via bundle in
                    // intent.

                    //Form a list of tags in use by the selected items. There may be a tag that has just been applied
                    //  that is not currently used by any tags in the catalog. Such a tag would not get picked up by the
                    //  IN-USE function in globalClass, and get listed in the IN-USE section of the tag selector.
                    TreeMap<String, ItemClass_Tag> tmImportSessionTagsInUse = new TreeMap<>();
                    for(ItemClass_File fi: alFileItems){ //Loop through file items in this listView.
                        if(fi.bIsChecked){               //If the user has selected this fileItem...
                            for(Integer iTagID: fi.aliProspectiveTags){  //loop through the prospectiveTags and add them to the non-duplicate TreeMap.
                                String sTagText = globalClass.getTagTextFromID(iTagID, viewModelImportActivity.iImportMediaCategory);
                                tmImportSessionTagsInUse.put(sTagText,new ItemClass_Tag(iTagID, sTagText));
                            }
                        }
                    }
                    //Add the treeMap to the bundle to send to the preview:
                    b.putSerializable(IMPORT_SESSION_TAGS_IN_USE, tmImportSessionTagsInUse);

                    ItemClass_File[] fileItems;
                    if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                            viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {

                        //Send all of the video and image file items that are shown to the preview, and tell position.
                        //  That way the user can swipe to the next video or image and apply tags to that one as well.
                        ItemClass_File[] icf = new ItemClass_File[alFileItemsDisplay.size()];
                        int i = 0;
                        for(ItemClass_File icfSource: alFileItemsDisplay){
                            icf[i] = icfSource;
                            i++;
                        }
                        fileItems = icf;

                        b.putInt(PREVIEW_FILE_ITEMS_POSITION, position);

                    } else { //If comic...
                        //If this is a comic, put together all of the page fileItems for the preview.
                        ItemClass_File[] icfComicFiles = new ItemClass_File[]{};

                        if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_NH_COMIC_DOWNLOADER) {
                            //final public ArrayList<ItemClass_File> alFileItems;
                            //private ArrayList<ItemClass_File> alFileItemsDisplay;

                            //Sort the files for this comic by putting them into a TreeMap:
                            TreeMap<String, ItemClass_File> tmFiles = new TreeMap<>();
                            String sComicID = Service_Import.GetNHComicID(alFileItemsDisplay.get(position).sFileOrFolderName);
                            String sComicIDCandidate;
                            for (ItemClass_File icf : alFileItems) {
                                sComicIDCandidate = Service_Import.GetNHComicID(icf.sFileOrFolderName);
                                if (sComicIDCandidate.equals(sComicID)) {
                                    tmFiles.put(icf.sFileOrFolderName, icf);
                                }
                            }

                            ItemClass_File[] icf = new ItemClass_File[tmFiles.size()];
                            int i = 0;
                            for(Map.Entry<String, ItemClass_File> entry: tmFiles.entrySet()){
                                icf[i] = entry.getValue();
                                i++;
                            }

                            icfComicFiles = icf;

                        } else if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                            //final public ArrayList<ItemClass_File> alFileItems;
                            //private ArrayList<ItemClass_File> alFileItemsDisplay;

                            //Sort the files for this comic by putting them into a TreeMap:
                            TreeMap<String, ItemClass_File> tmFiles = new TreeMap<>();
                            String sParentComic = alFileItemsDisplay.get(position).sUri;
                            for (ItemClass_File icf : alFileItems) {
                                if (icf.sUriParent.equals(sParentComic)) {
                                    tmFiles.put(icf.sFileOrFolderName, icf);
                                }
                            }
                            //Put the files into a standard array:
                            ItemClass_File[] icf = new ItemClass_File[tmFiles.size()];
                            int i = 0;
                            for(Map.Entry<String, ItemClass_File> entry: tmFiles.entrySet()){
                                icf[i] = entry.getValue();
                                i++;
                            }

                            icfComicFiles = icf;

                        }

                        fileItems = icfComicFiles;
                    }


                    b.putSerializable(PREVIEW_FILE_ITEMS, fileItems);
                    intentPreviewPopup.putExtras(b);


                    garlGetTagsForImportItems.launch(intentPreviewPopup);



                }
            });


            return row;
        }


        private void toggleItemChecked(int iFileItemsDisplayPosition, boolean bNewCheckedState){


            if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                //If this is a comic, need to select all of the files that are part of that comic.
                if(viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_NH_COMIC_DOWNLOADER) {
                    //If the user is importing comic pages downloaded by the NHComicDownloader, find
                    // all files with the comic ID and apply the checked state:
                    String sNHComicID = Service_Import.GetNHComicID(alFileItemsDisplay.get(iFileItemsDisplayPosition).sFileOrFolderName);
                    String sNHComicFilter = sNHComicID + ".+";
                    for (ItemClass_File icf : alFileItems) {
                        if (icf.sFileOrFolderName.matches(sNHComicFilter)) {
                            icf.bIsChecked = bNewCheckedState;
                        }
                    }
                } else if(viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                    //If the user is importing comic pages by folder, find
                    // all files with the comic parent Uri assigned and apply the checked state:
                    String sUriParent = alFileItemsDisplay.get(iFileItemsDisplayPosition).sUri;
                    for (ItemClass_File icf : alFileItems) {
                        if (icf.sUriParent.equals(sUriParent)) {
                            icf.bIsChecked = bNewCheckedState;
                        }
                    }
                }
            } else {
                //Find the item that is checked/unchecked in alFileList and apply the property.
                //  The user will have clicked an item in alFileListDisplay, not alFileList.
                //  alFileListDisplay may be a subset of alFileList.
                for(ItemClass_File icf: alFileItems){
                    if(icf.sFileOrFolderName.equals(alFileItemsDisplay.get(iFileItemsDisplayPosition).sFileOrFolderName)){
                        icf.bIsChecked = bNewCheckedState;
                        break; //Break, as only one item should match.
                    }
                }
            }

            if(!bNewCheckedState){
                recalcButtonNext();
            } else {
                Button button_ItemSelectComplete = findViewById(R.id.button_ItemSelectComplete);
                if(button_ItemSelectComplete != null){
                    button_ItemSelectComplete.setEnabled(true);
                }
            }

            
        }
        
        public void recalcButtonNext(){
            boolean bEnableNextButton = false;
            for(ItemClass_File fi: alFileItems){
                if(fi.bIsChecked){
                    bEnableNextButton = true;
                    break;
                }
            }
            Button button_ItemSelectComplete = findViewById(R.id.button_ItemSelectComplete);
            if(button_ItemSelectComplete != null){
                button_ItemSelectComplete.setEnabled(bEnableNextButton);
            }

        }




        public void updateFileItemDetails(ItemClass_File[] icfIncomingFIs){
            boolean bFoundAndUpdated = false;
            //Find the items to apply individualized tags.
            //This routine is not designed to apply the same tags to multiple items.


            for(ItemClass_File icfIncoming: icfIncomingFIs) {
                if(icfIncoming.bDataUpdateFlag) {
                    for (ItemClass_File icfUpdate : alFileItems) {
                        if (icfUpdate.sUri.contentEquals(icfIncoming.sUri)) {
                            icfUpdate.aliProspectiveTags = icfIncoming.aliProspectiveTags;
                            icfUpdate.iGrade = icfIncoming.iGrade;
                            icfUpdate.bIsChecked = icfIncoming.bIsChecked;
                            bFoundAndUpdated = true;
                            break;
                        }
                    }
                }
            }

            if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                    //If we are importing comics from folders, update the comic parent item with the selected tags.
                    if(icfIncomingFIs.length > 0) {
                        String sParentComic = icfIncomingFIs[0].sUriParent;
                        for (ItemClass_File icfUpdate : alFileItems) {
                            if (icfUpdate.sUri.equals(sParentComic)) {
                                icfUpdate.aliProspectiveTags = icfIncomingFIs[0].aliProspectiveTags;
                                icfUpdate.iGrade = icfIncomingFIs[0].iGrade;
                                icfUpdate.bIsChecked = true;
                            }
                        }
                    }
                }
            }

            recalcButtonNext();

            if (bFoundAndUpdated) {
                notifyDataSetChanged();
            }

        }




        //To prevent data resetting when scrolled
        @Override
        public int getCount() {
            return alFileItemsDisplay.size();
        }

        @Override
        public ItemClass_File getItem(int position) {
            return alFileItemsDisplay.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void toggleSelectAll(){
            bSelectAllSelected = !bSelectAllSelected;

            if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                //If we are working with comics, then the items on display are only the cover pages.
                //  We need to apply "Select All" to all items including the items which are not displayed.
                for(ItemClass_File fiDisplayed: alFileItems){
                    fiDisplayed.bIsChecked = bSelectAllSelected;
                    //Translate the selected item state to alFileList:
                    for(ItemClass_File fi: alFileItems){
                        if(fi.sFileOrFolderName.contentEquals(fiDisplayed.sFileOrFolderName)){
                            fi.bIsChecked = bSelectAllSelected;
                            break;
                        }
                    }
                }
            } else {
                for(ItemClass_File fiDisplayed: alFileItemsDisplay){
                    fiDisplayed.bIsChecked = bSelectAllSelected;
                    //Translate the selected item state to alFileList:
                    for(ItemClass_File fi: alFileItems){
                        if(fi.sFileOrFolderName.contentEquals(fiDisplayed.sFileOrFolderName)){
                            fi.bIsChecked = bSelectAllSelected;
                            break;
                        }
                    }
                }
            }



            Button button_ItemSelectComplete = findViewById(R.id.button_ItemSelectComplete);
            if(button_ItemSelectComplete != null){
                button_ItemSelectComplete.setEnabled(bSelectAllSelected);
            }
        }


        public void applySearch(String sSearch){
            alFileItemsDisplay.clear();
            for(ItemClass_File fi : alFileItems){
                if(fi.sFileOrFolderName.contains(sSearch)){
                    alFileItemsDisplay.add(fi);
                }
            }
        }

        public void applyFilter(String sFilter){
            alFileItemsDisplay.clear();
            for(ItemClass_File fi : alFileItems){
                if(fi.sFileOrFolderName.matches(sFilter)){
                    alFileItemsDisplay.add(fi);
                }
            }
        }

        public void applyFilterByType(int iTypeFileOrFolder){
            alFileItemsDisplay.clear();
            for(ItemClass_File fi : alFileItems){
                if(fi.iTypeFileFolderURL ==iTypeFileOrFolder){
                    alFileItemsDisplay.add(fi);
                }
            }
        }

        public void removeSearch(){
            alFileItemsDisplay = alFileItems;
        }

        //Comparators
        //Allow sort by file name or modified date

        //Sort by file name ascending:
        private class FileNameAscComparator implements Comparator<ItemClass_File> {
            public int compare(ItemClass_File fi1, ItemClass_File fi2) {
                String FileName1 = fi1.sFileOrFolderName.toUpperCase();
                String FileName2 = fi2.sFileOrFolderName.toUpperCase();

                //ascending order
                return FileName1.compareTo(FileName2);
            }
        }

        //Sort by file name descending:
        private class FileNameDescComparator implements Comparator<ItemClass_File> {
            public int compare(ItemClass_File fi1, ItemClass_File fi2) {
                String FileName1 = fi1.sFileOrFolderName.toUpperCase();
                String FileName2 = fi2.sFileOrFolderName.toUpperCase();

                //descending order
                return FileName2.compareTo(FileName1);
            }
        }

        //Sort by file modified date ascending:
        private class FileModifiedDateAscComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fi1, ItemClass_File fi2) {
                Date FileDate1 = fi1.dateLastModified;
                Date FileDate2 = fi2.dateLastModified;

                //ascending order
                return FileDate1.compareTo(FileDate2);
            }
        }

        //Sort by file modified date descending:
        private class FileModifiedDateDescComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fi1, ItemClass_File fi2) {
                Date FileDate1 = fi1.dateLastModified;
                Date FileDate2 = fi2.dateLastModified;

                //descending order
                return FileDate2.compareTo(FileDate1);
            }
        }

        //Sort by file modified date ascending:
        private class FileDurationAscComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fi1, ItemClass_File fi2) {
                Long FileDuration1 = fi1.lVideoTimeInMilliseconds;
                Long FileDuration2 = fi2.lVideoTimeInMilliseconds;

                //ascending order
                return FileDuration1.compareTo(FileDuration2);
            }
        }

        //Sort by file modified date descending:
        private class FileDurationDescComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fi1, ItemClass_File fi2) {
                Long FileDuration1 = fi1.lVideoTimeInMilliseconds;
                Long FileDuration2 = fi2.lVideoTimeInMilliseconds;

                //descending order
                return FileDuration2.compareTo(FileDuration1);
            }
        }


        public int iCurrentSortMethod;
        private final int SORT_METHOD_FILENAME_ASC = 1;
        private final int SORT_METHOD_FILENAME_DESC = 2;
        private final int SORT_METHOD_MODIFIED_DATE_ASC = 3;
        private final int SORT_METHOD_MODIFIED_DATE_DESC = 4;
        private final int SORT_METHOD_DURATION_ASC = 5;
        private final int SORT_METHOD_DURATION_DESC = 6;
        public void SortByFileNameAsc(){
            Collections.sort(alFileItemsDisplay, new FileListCustomAdapter.FileNameAscComparator());
            iCurrentSortMethod = SORT_METHOD_FILENAME_ASC;
        }
        public void SortByFileNameDesc(){
            Collections.sort(alFileItemsDisplay, new FileListCustomAdapter.FileNameDescComparator());
            iCurrentSortMethod = SORT_METHOD_FILENAME_DESC;
        }
        public void SortByDateModifiedAsc(){
            Collections.sort(alFileItemsDisplay, new FileListCustomAdapter.FileModifiedDateAscComparator());
            iCurrentSortMethod = SORT_METHOD_MODIFIED_DATE_ASC;
        }
        public void SortByDateModifiedDesc(){
            Collections.sort(alFileItemsDisplay, new FileListCustomAdapter.FileModifiedDateDescComparator());
            iCurrentSortMethod = SORT_METHOD_MODIFIED_DATE_DESC;
        }
        public void SortByDurationAsc(){
            Collections.sort(alFileItemsDisplay, new FileListCustomAdapter.FileDurationAscComparator());
            iCurrentSortMethod = SORT_METHOD_DURATION_ASC;
        }
        public void SortByDurationDesc(){
            Collections.sort(alFileItemsDisplay, new FileListCustomAdapter.FileDurationDescComparator());
            iCurrentSortMethod = SORT_METHOD_DURATION_DESC;
        }
        public boolean reverseSort(){
            //Return true if new sort order is ascending, false if descending.
            boolean bSortOrderIsAscending = true;
            switch (iCurrentSortMethod){
                case SORT_METHOD_FILENAME_ASC:
                    SortByFileNameDesc();
                    bSortOrderIsAscending =  false;
                    break;
                case SORT_METHOD_FILENAME_DESC:
                    SortByFileNameAsc();
                    bSortOrderIsAscending =  true;
                    break;
                case SORT_METHOD_MODIFIED_DATE_ASC:
                    SortByDateModifiedDesc();
                    bSortOrderIsAscending =  false;
                    break;
                case SORT_METHOD_MODIFIED_DATE_DESC:
                    SortByDateModifiedAsc();
                    bSortOrderIsAscending =  true;
                    break;
                case SORT_METHOD_DURATION_ASC:
                    SortByDurationDesc();
                    bSortOrderIsAscending =  false;
                    break;
                case SORT_METHOD_DURATION_DESC:
                    SortByDurationAsc();
                    bSortOrderIsAscending =  true;
                    break;
            }
            return bSortOrderIsAscending;
        }

    }

    public class VideoDownloadListCustomAdapter extends ArrayAdapter<ItemClass_File> {
        //This class for displaying to user video files found in html.

        final public ArrayList<ItemClass_File> alFileItems;
        Context contextFromCaller;

        public VideoDownloadListCustomAdapter(Context context, int textViewResourceId, ArrayList<ItemClass_File> alfi) {
            super(context, textViewResourceId, alfi);
            contextFromCaller = context;
            alFileItems = new ArrayList<>(alfi);
        }

        @Override
        @NonNull
        public View getView(final int position, View v, @NonNull ViewGroup parent) {
            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                //My custom list item design is here
                row = inflater.inflate(R.layout.listview_fileitem_selectable, parent, false);
            }

            CheckBox cbStorageItemSelect =  row.findViewById(R.id.checkBox_StorageItemSelect);
            ImageView ivFileType =  row.findViewById(R.id.imageView_StorageItemIcon);
            TextView tvLine1 =  row.findViewById(R.id.textView_Line1);
            TextView tvLine2 = row.findViewById(R.id.textView_Line2);
            TextView tvLine3 = row.findViewById(R.id.textView_Line3);

            tvLine1.setText(alFileItems.get(position).sFileOrFolderName);

            String sLine2 = GlobalClass.CleanStorageSize(alFileItems.get(position).lSizeBytes, "");

            if (alFileItems.get(position).iTypeFileFolderURL == ItemClass_File.TYPE_M3U8) {
                if (alFileItems.get(position).ic_M3U8 != null) {
                    sLine2 = sLine2 + ". Resolution: " + alFileItems.get(position).ic_M3U8.sResolution + ".";
                    if (alFileItems.get(position).ic_M3U8.als_TSDownloads != null) {
                        sLine2 = sLine2 + " " + alFileItems.get(position).ic_M3U8.als_TSDownloads.size() +
                                " MPEG-2 files to be downloaded and concatenated.";
                    } else {
                        sLine2 = "Possible corrupted download information.";
                    }
                } else {
                    sLine2 = "Possible corrupted download information.";
                }
            }
            tvLine2.setText(sLine2);

            tvLine3.setVisibility(View.INVISIBLE);

            //Display a thumbnail:
            String sURLThumbnail = alFileItems.get(position).sURLThumbnail;
            if(!sURLThumbnail.equals("")) {
                Glide.with(getContext()).
                        load(sURLThumbnail).
                        into(ivFileType);
            }



            cbStorageItemSelect.setChecked(alFileItems.get(position).bIsChecked);

            //Expand the width of the listItem to the width of the ListView.
            //  This makes it so that the listItem responds to the click even when
            //  the click is off of the text.
            row.setMinimumWidth(SelectItemsListViewWidth);

            //Set the onClickListener for the row to toggle the checkbox:
            row.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    CheckBox checkBox_StorageItemSelect = view.findViewById(R.id.checkBox_StorageItemSelect);
                    boolean bNewCheckedState = !checkBox_StorageItemSelect.isChecked();
                    checkBox_StorageItemSelect.setChecked(bNewCheckedState);
                    alFileItems.get(position).bIsChecked = bNewCheckedState;
                    toggleItemChecked(position, bNewCheckedState);

                }
            });


            //Set the onClickListener for the checkbox to toggle the checkbox:
            CheckBox checkBox_StorageItemSelect = row.findViewById(R.id.checkBox_StorageItemSelect);
            checkBox_StorageItemSelect.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    CheckBox checkBox_StorageItemSelect = (CheckBox) view;
                    boolean bNewCheckedState = checkBox_StorageItemSelect.isChecked();
                    alFileItems.get(position).bIsChecked = bNewCheckedState;
                    toggleItemChecked(position, bNewCheckedState);

                }
            });


            //If the file item is video mimeType, set the preview button visibility to visible:
            Button button_MediaPreview = row.findViewById(R.id.button_MediaPreview);
            boolean bItemIsVideo = alFileItems.get(position).sMimeType.startsWith("video")  ||
                    (alFileItems.get(position).sMimeType.equals("application/octet-stream") &&
                            alFileItems.get(position).sExtension.equals(".mp4"));//https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid)

            button_MediaPreview.setVisibility(View.INVISIBLE); //For now, disable preview of web-detected video.

            //button_MediaPreview.setVisibility(Button.VISIBLE);
            button_MediaPreview.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    //Start the preview popup activity:
                    Intent intentPreviewPopup;
                    intentPreviewPopup = new Intent(Activity_Import.this, Activity_ImportFilePreview.class);


                    Bundle b = new Bundle();
                    b.putInt(MEDIA_CATEGORY,
                            viewModelImportActivity.iImportMediaCategory); //viewModel not intended
                    // to be used between Activities. Therefore, pass media category via bundle in
                    // intent.

                    //Form a list of tags in use by the selected items. There may be a tag that has just been applied
                    //  that is not currently used by any tags in the catalog. Such a tag would not get picked up by the
                    //  IN-USE function in globalClass, and get listed in the IN-USE section of the tag selector.
                    TreeMap<String, ItemClass_Tag> tmImportSessionTagsInUse = new TreeMap<>();
                    for(ItemClass_File fi: alFileItems){ //Loop through file items in this listView.
                        if(fi.bIsChecked){               //If the user has selected this fileItem...
                            for(Integer iTagID: fi.aliProspectiveTags){  //loop through the prospectiveTags and add them to the non-duplicate TreeMap.
                                String sTagText = globalClass.getTagTextFromID(iTagID, viewModelImportActivity.iImportMediaCategory);
                                tmImportSessionTagsInUse.put(sTagText,new ItemClass_Tag(iTagID, sTagText));
                            }
                        }
                    }
                    //Add the treeMap to the bundle to send to the preview:
                    b.putSerializable(IMPORT_SESSION_TAGS_IN_USE, tmImportSessionTagsInUse);

                    ItemClass_File[] fileItems;


                    //Send all of the video and image file items that are shown to the preview, and tell position.
                    //  That way the user can swipe to the next video or image and apply tags to that one as well.
                    ItemClass_File[] icf = new ItemClass_File[alFileItems.size()];
                    int i = 0;
                    for(ItemClass_File icfSource: alFileItems){
                        icf[i] = icfSource;
                        i++;
                    }
                    fileItems = icf;

                    b.putInt(PREVIEW_FILE_ITEMS_POSITION, position);
                    b.putSerializable(PREVIEW_FILE_ITEMS, fileItems);
                    intentPreviewPopup.putExtras(b);
                    garlGetTagsForImportItems.launch(intentPreviewPopup);

                }
            });

            //Hide the delete button since this is a download (delete doesn't make sense):
            Button button_Delete = row.findViewById(R.id.button_Delete);
            if(button_Delete != null){
                button_Delete.setVisibility(View.INVISIBLE);
            }


            return row;
        }


        private void toggleItemChecked(int iFileItemsDisplayPosition, boolean bNewCheckedState){

            for(ItemClass_File icf: alFileItems){
                icf.bIsChecked = false;  //Reset all items to not-checked state as only one item gets checked.
            }

            //Apply the checked-state to the item:
            alFileItems.get(iFileItemsDisplayPosition).bIsChecked = bNewCheckedState;

            //Enable/disable next button:
            recalcButtonNext();

            notifyDataSetChanged(); //Update the checkboxes for all items.
        }

        public void recalcButtonNext(){
            boolean bEnableNextButton = false;
            for(ItemClass_File fi: alFileItems){
                if(fi.bIsChecked){
                    bEnableNextButton = true;
                    break;
                }
            }
            Button button_ItemSelectComplete = findViewById(R.id.button_ItemSelectComplete);
            if(button_ItemSelectComplete != null){
                button_ItemSelectComplete.setEnabled(bEnableNextButton);
            }

        }




        public void updateFileItemDetails(ItemClass_File[] icfIncomingFIs){
            boolean bFoundAndUpdated = false;
            //Find the items to apply individualized tags.
            //This routine is not designed to apply the same tags to multiple items.


            for(ItemClass_File icfIncoming: icfIncomingFIs) {
                if(icfIncoming.bDataUpdateFlag) {
                    for (ItemClass_File icfUpdate : alFileItems) {
                        if (icfUpdate.sUri.contentEquals(icfIncoming.sUri)) {
                            icfUpdate.aliProspectiveTags = icfIncoming.aliProspectiveTags;
                            icfUpdate.iGrade = icfIncoming.iGrade;
                            icfUpdate.bIsChecked = true;
                            bFoundAndUpdated = true;
                            break;
                        }
                    }
                }
            }

            if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                    //If we are importing comics from folders, update the comic parent item with the selected tags.
                    if(icfIncomingFIs.length > 0) {
                        String sParentComic = icfIncomingFIs[0].sUriParent;
                        for (ItemClass_File icfUpdate : alFileItems) {
                            if (icfUpdate.sUri.equals(sParentComic)) {
                                icfUpdate.aliProspectiveTags = icfIncomingFIs[0].aliProspectiveTags;
                                icfUpdate.iGrade = icfIncomingFIs[0].iGrade;
                                icfUpdate.bIsChecked = true;
                            }
                        }
                    }
                }
            }

            recalcButtonNext();

            if (bFoundAndUpdated) {
                notifyDataSetChanged();
            }

        }




        //To prevent data resetting when scrolled
        @Override
        public int getCount() {
            return alFileItems.size();
        }

        @Override
        public ItemClass_File getItem(int position) {
            return alFileItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }







    }


    public static class FragmentImportViewPagerAdapter extends FragmentStateAdapter {

        public FragmentImportViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                //case FRAGMENT_IMPORT_0_ID_MEDIA_CATEGORY:
                //    return new Fragment_Import_0_MediaCategory();
                case FRAGMENT_IMPORT_0A_ID_VIDEO_SOURCE:
                    return new Fragment_Import_0a_VideoSource();
                case FRAGMENT_IMPORT_0B_ID_COMIC_SOURCE:
                    return new Fragment_Import_0b_ComicSource();
                case FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION:
                    return new Fragment_Import_1_StorageLocation();
                case FRAGMENT_IMPORT_1A_ID_VIDEO_WEB_DETECT:
                    return new Fragment_Import_1a_VideoWebDetect();
                case FRAGMENT_IMPORT_1B_ID_WEB_ADDRESS:
                    return new Fragment_Import_1b_ComicWebDetect();
                case FRAGMENT_IMPORT_2_ID_SELECT_ITEMS:
                    return new Fragment_Import_2_SelectItems();
                case FRAGMENT_IMPORT_2A_ID_SELECT_DETECTED_WEB_VIDEO_ITEM:
                    return new Fragment_Import_2a_SelectDetectedWebVideo();
                case FRAGMENT_IMPORT_2B_SELECT_SINGLE_WEB_COMIC:
                    return new Fragment_Import_2b_SelectSingleWebComic();
                case FRAGMENT_IMPORT_3_ID_SELECT_TAGS:
                    return new Fragment_Import_3_SelectTags();
                case FRAGMENT_IMPORT_3A_ITEM_DOWNLOAD_TAG_IMPORT:
                    return new Fragment_Import_3a_ItemDownloadTagImport();
                case FRAGMENT_IMPORT_4_ID_IMPORT_METHOD:
                    return new Fragment_Import_4_CopyOrMoveFiles();
                case FRAGMENT_IMPORT_5_ID_CONFIRMATION:
                    return new Fragment_Import_5_Confirmation();

                case FRAGMENT_IMPORT_6_ID_EXECUTE_IMPORT:
                    return new Fragment_Import_6_ExecuteImport();
                default:
                    return new Fragment_Import_0_MediaCategory();
            }
        }

        @Override
        public int getItemCount() {
            return Activity_Import.FRAGMENT_COUNT;
        }

    }


}