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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
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
    public static final int FRAGMENT_IMPORT_3B_COMIC_TAG_IMPORT = 10;
    public static final int FRAGMENT_IMPORT_4_ID_IMPORT_METHOD = 11;
    public static final int FRAGMENT_IMPORT_5_ID_CONFIRMATION = 12;
    public static final int FRAGMENT_IMPORT_2B_SELECT_SINGLE_WEB_COMIC = 13;
    public static final int FRAGMENT_IMPORT_6_ID_EXECUTE_IMPORT = 14;

    public static final int FRAGMENT_COUNT = 15;

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

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

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
        if(mediaMetadataRetriever != null) {
            mediaMetadataRetriever.release();
        }

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
                //This ActivityResultLauncher gathers results of tag selection from a preview operation.
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == RESULT_OK){
                        fileListCustomAdapter.updateFileItemDetails(globalClass.galPreviewFileList);
                        globalClass.galPreviewFileList = null; //Release memory.
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

    public void buttonClick_ComicTagImportReturn(View v){
        fileListCustomAdapter.recalcUnidentifiedTags();
        onBackPressed();
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

        globalClass.giSelectedCatalogMediaCategory = iNewImportMediaCatagory;
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
        RadioButton radioButton_VideoSourceWebpage = findViewById(R.id.radioButton_VideoSourceWebpage);
        //RadioButton radioButton_VideoSourceFolder = findViewById(R.id.radioButton_VideoSourceFolder);

        int iNewVideoSource;

        if (radioButton_VideoSourceWebpage.isChecked()){
            iNewVideoSource = ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE;
        } else {
            iNewVideoSource = ViewModel_ImportActivity.VIDEO_SOURCE_FOLDER;
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
            viewModelImportActivity.iImportMethod = GlobalClass.MOVE;
        } else{
            viewModelImportActivity.iImportMethod = GlobalClass.COPY;
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

        private final int iHideTagImportButtonHeightPixels;
        private final int iShowTagImportButtonHeightPixels;

        public FileListCustomAdapter(Context context, int textViewResourceId, ArrayList<ItemClass_File> alfi) {
            super(context, textViewResourceId, alfi);
            contextFromCaller = context;
            alFileItems = new ArrayList<>(alfi);

            int iHideTagImportButtonHeightDP = 67;
            iHideTagImportButtonHeightPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iHideTagImportButtonHeightDP, getResources().getDisplayMetrics());
            int iShowTagImportButtonHeightDP = 67 + 50;
            iShowTagImportButtonHeightPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iShowTagImportButtonHeightDP, getResources().getDisplayMetrics());

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
            RelativeLayout relativeLayout_AdaptiveButtons = row.findViewById(R.id.relativeLayout_AdaptiveButtons);
            Button button_TagImport = row.findViewById(R.id.button_TagImport);

            String sLine1 = alFileItemsDisplay.get(position).sFileOrFolderName;
            if(!alFileItemsDisplay.get(position).sHeight.equals("")){ //Add resolution data to display if available:
                sLine1 = sLine1 + " " + alFileItemsDisplay.get(position).sWidth + "x" + alFileItemsDisplay.get(position).sHeight;
            }
            if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES){
                //If category is images, include megapixels.
                try {
                    double dWidth = Double.parseDouble(alFileItemsDisplay.get(position).sWidth);
                    double dHeight = Double.parseDouble(alFileItemsDisplay.get(position).sHeight);
                    double dMegapixels = (dWidth * dHeight) / 1048576; //2^20 pixels per megapixel.
                    sLine1 = sLine1 + " " + String.format(Locale.getDefault(), "%.2f", dMegapixels) + "MP";
                } catch (Exception e){
                    //Do nothing. Just a textual ommision.
                }
            }
            tvLine1.setText(sLine1);
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

            if(!alFileItemsDisplay.get(position).sParody.equals("")){
                sbTags.append("Parody: ");
                sbTags.append(alFileItemsDisplay.get(position).sParody);
                sbTags.append("\n");
            }
            if(!alFileItemsDisplay.get(position).sArtist.equals("")){
                sbTags.append("Artist: ");
                sbTags.append(alFileItemsDisplay.get(position).sArtist);
                sbTags.append("\n");
            }

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

            //Add unidentified tags to the row if they exist. This happens during import of comic with tags specified in an XML file.
            if(alFileItemsDisplay.get(position).alsUnidentifiedTags != null) {
                ArrayList<String> alsUnidentifiedTags = alFileItemsDisplay.get(position).alsUnidentifiedTags;
                //Prepare to check/adjust the view capability of the "Tag Import" button:
                ViewGroup.LayoutParams params = relativeLayout_AdaptiveButtons.getLayoutParams();
                if (alsUnidentifiedTags.size() > 0) {
                    sbTags.append("\n[Tags not in dictionary: ");
                    boolean bFirstOne = true;
                    for (String sUnidentifiedTag : alsUnidentifiedTags) {
                        if (bFirstOne) {
                            sbTags.append(sUnidentifiedTag);
                            bFirstOne = false;
                        } else {
                            String sTemp = ", " + sUnidentifiedTag;
                            sbTags.append(sTemp);
                        }
                    }
                    sbTags.append("]");

                    //Show the "Tag Import" button on the row:
                    params.height = iShowTagImportButtonHeightPixels;
                    relativeLayout_AdaptiveButtons.setLayoutParams(params);

                    //Set the action for the "Tag Import" button:
                    button_TagImport.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            viewModelImportActivity.alsUnidentifiedTags = alFileItemsDisplay.get(position).alsUnidentifiedTags;

                            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_3B_COMIC_TAG_IMPORT, false);
                            stackFragmentOrder.push(ViewPager2_Import.getCurrentItem());

                        }
                    });

                } else {
                    //Make sure the "Tag Import" button is not shown: //todo: is this used anymore?
                    if (params.height != iHideTagImportButtonHeightPixels) {
                        //If it is shown, hide it by shrinking the LinearLayout:
                        params.height = iHideTagImportButtonHeightPixels;
                        relativeLayout_AdaptiveButtons.setLayoutParams(params);
                    }
                }
            }
            String sLine3 = sbTags.toString();
            if(alFileItemsDisplay.get(position).bMarkedForDeletion){
                sLine3 = "Marked for deletion.";
            }
            tvLine3.setText(sLine3);

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

            final Button button_Delete = row.findViewById(R.id.button_Delete);

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
                    if(bNewCheckedState){
                        alFileItemsDisplay.get(position).bMarkedForDeletion = false;
                        button_Delete.setPressed(false);
                    }
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
                    if(bNewCheckedState){
                        alFileItemsDisplay.get(position).bMarkedForDeletion = false;
                        button_Delete.setPressed(false);
                    }
                    toggleItemChecked(position, bNewCheckedState);

                }
            });

            //Code the button to delete a file in the ListView:

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
                            //if the user has selected not to delete this item, unmark it for deletion if it is marked such.
                            if(alFileItemsDisplay.get(position).bMarkedForDeletion){
                                alFileItemsDisplay.get(position).bMarkedForDeletion = false;
                                for(ItemClass_File icf: alFileItems){
                                    if(icf.sFileOrFolderName.equals(alFileItemsDisplay.get(position).sFileOrFolderName)){
                                        icf.bMarkedForDeletion = false;
                                        break; //Break, as only one item should match.
                                    }
                                }
                            }
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });

            if(alFileItemsDisplay.get(position).bMarkedForDeletion){
                button_Delete.setPressed(true);
            }


            Button button_MediaPreview = row.findViewById(R.id.button_MediaPreview);
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

                    ArrayList<ItemClass_File> alPreviewFileList = new ArrayList<>();
                    if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                            viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {

                        //Send all of the video and image file items that are shown to the preview, and tell position.
                        //  That way the user can swipe to the next video or image and apply tags to that one as well.
                        alPreviewFileList = alFileItemsDisplay;
                        b.putInt(PREVIEW_FILE_ITEMS_POSITION, position);

                    } else { //If comic...
                        //If this is a comic, put together all of the page fileItems for the preview.

                        if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_NH_COMIC_DOWNLOADER) {

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

                            for(Map.Entry<String, ItemClass_File> entry: tmFiles.entrySet()){
                                alPreviewFileList.add(entry.getValue()); //todo: simplify?
                            }


                        } else if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {

                            //Sort the files for this comic by putting them into a TreeMap:
                            TreeMap<String, ItemClass_File> tmFiles = new TreeMap<>();
                            String sParentComic = alFileItemsDisplay.get(position).sUri;
                            for (ItemClass_File icf : alFileItems) {
                                if (icf.sUriParent.equals(sParentComic)) {
                                    tmFiles.put(icf.sFileOrFolderName, icf);
                                }
                            }
                            //Put the files into a standard array:
                            for(Map.Entry<String, ItemClass_File> entry: tmFiles.entrySet()){
                                alPreviewFileList.add(entry.getValue()); //todo: simplify?
                            }
                            //Put the tags into the first item in the file array. This is only
                            // for comics. The item selected by the user is a "folder" item and is not
                            // transferred to preview, but this is the item holding the tags.
                            if(alPreviewFileList.size() > 0){
                                alPreviewFileList.get(0).aliProspectiveTags = alFileItemsDisplay.get(position).aliProspectiveTags; //todo: simplify?
                            }


                        }

                    }

                    globalClass.galPreviewFileList = alPreviewFileList;

                    intentPreviewPopup.putExtras(b); //Just the int index for whichever item on display the user selected.

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
                            if(bNewCheckedState){
                                icf.bMarkedForDeletion = false;
                            }
                        }
                    }
                } else if(viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                    //If the user is importing comic pages by folder, find
                    // all files with the comic parent Uri assigned and apply the checked state:
                    String sUriParent = alFileItemsDisplay.get(iFileItemsDisplayPosition).sUri;
                    for (ItemClass_File icf : alFileItems) {
                        if (icf.sUriParent.equals(sUriParent)) {
                            icf.bIsChecked = bNewCheckedState;
                            if(bNewCheckedState){
                                icf.bMarkedForDeletion = false;
                            }
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
                        if(bNewCheckedState){
                            icf.bMarkedForDeletion = false;
                        }
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




        public void updateFileItemDetails(ArrayList<ItemClass_File> alicfIncomingFIs){
            boolean bFoundAndUpdated = false;
            //Find the items to apply individualized tags.
            //This routine is not designed to apply the same tags to multiple items.

            for(ItemClass_File icfIncoming: alicfIncomingFIs) {
                if(icfIncoming.bDataUpdateFlag) {
                    for (ItemClass_File icfUpdate : alFileItems) {
                        if (icfUpdate.sUri.contentEquals(icfIncoming.sUri)) {
                            icfUpdate.aliProspectiveTags = icfIncoming.aliProspectiveTags;
                            icfUpdate.iGrade = icfIncoming.iGrade;
                            icfUpdate.bIsChecked = icfIncoming.bIsChecked;
                            icfUpdate.bMarkedForDeletion = icfIncoming.bMarkedForDeletion;
                            bFoundAndUpdated = true;
                            break;
                        }
                    }
                }
            }

            if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                    //If we are importing comics from folders, update the comic parent item with the selected tags.
                    if(alicfIncomingFIs.size() > 0) {
                        String sParentComic = alicfIncomingFIs.get(0).sUriParent;
                        for (ItemClass_File icfUpdate : alFileItems) {
                            if (icfUpdate.sUri.equals(sParentComic)) {
                                icfUpdate.aliProspectiveTags = alicfIncomingFIs.get(0).aliProspectiveTags;
                                icfUpdate.iGrade = alicfIncomingFIs.get(0).iGrade;
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

        //Sort by file resolution ascending:
        private class FileResolutionAscComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fi1, ItemClass_File fi2) {
                try {
                    int iHeight1 = Integer.parseInt(fi1.sHeight);
                    int iWidth1 = Integer.parseInt(fi1.sWidth);
                    int iResolution1 = iHeight1 * iWidth1;

                    int iHeight2 = Integer.parseInt(fi2.sHeight);
                    int iWidth2 = Integer.parseInt(fi2.sWidth);
                    int iResolution2 = iHeight2 * iWidth2;

                    //ascending order
                    return iResolution1 - iResolution2;
                } catch (Exception e){
                    //Likely parse error on resolution
                    //todo: Notify user that sorting may be inaccurate.
                    return 0;
                }
            }
        }

        //Sort by file resolution descending:
        private class FileResolutionDescComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fi1, ItemClass_File fi2) {
                try {
                    int iHeight1 = Integer.parseInt(fi1.sHeight);
                    int iWidth1 = Integer.parseInt(fi1.sWidth);
                    int iResolution1 = iHeight1 * iWidth1;

                    int iHeight2 = Integer.parseInt(fi2.sHeight);
                    int iWidth2 = Integer.parseInt(fi2.sWidth);
                    int iResolution2 = iHeight2 * iWidth2;

                    //descending order
                    return iResolution2 - iResolution1;
                } catch (Exception e){
                    //Likely parse error on resolution
                    //todo: Notify user that sorting may be inaccurate.
                    return 0;
                }
            }
        }


        //Sort by video duration ascending:
        private class FileDurationAscComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fi1, ItemClass_File fi2) {
                Long FileDuration1 = fi1.lVideoTimeInMilliseconds;
                Long FileDuration2 = fi2.lVideoTimeInMilliseconds;

                //ascending order
                return FileDuration1.compareTo(FileDuration2);
            }
        }

        //Sort by video duration descending:
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
        private final int SORT_METHOD_RESOLUTION_ASC = 5;
        private final int SORT_METHOD_RESOLUTION_DESC = 6;
        private final int SORT_METHOD_DURATION_ASC = 7;
        private final int SORT_METHOD_DURATION_DESC = 8;
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
        public void SortByResolutionAsc(){
            Collections.sort(alFileItemsDisplay, new FileListCustomAdapter.FileResolutionAscComparator());
            iCurrentSortMethod = SORT_METHOD_RESOLUTION_ASC;
        }
        public void SortByResolutionDesc(){
            Collections.sort(alFileItemsDisplay, new FileListCustomAdapter.FileResolutionDescComparator());
            iCurrentSortMethod = SORT_METHOD_RESOLUTION_DESC;
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
                case SORT_METHOD_RESOLUTION_ASC:
                    SortByResolutionDesc();
                    bSortOrderIsAscending =  false;
                    break;
                case SORT_METHOD_RESOLUTION_DESC:
                    SortByResolutionAsc();
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


        public void recalcUnidentifiedTags(){
            //For import of comic. If the comic comes with an xml file containing a list of tags,
            //  some of those tags may not be in the catalog. This function is called if the
            //  user imports some of those tags. This refreshes the file item listing to show
            //  tags that are now imported.

            for(ItemClass_File fi: alFileItems){

                //If this item has unidentified tags:
                if (fi.alsUnidentifiedTags != null) {
                    if (fi.alsUnidentifiedTags.size() > 0) {

                        //Pre-process tags. Identify tags that already exist, and create a list of new tags for
                        //  the user to approve - don't automatically add new tags to the system (I've encountered
                        //  garbage tags, tags that already exist in another form, and tags that the user might
                        //  not want to add.
                        ArrayList<String> alsNewUnidentifiedTags = new ArrayList<>();
                        for(String sTag: fi.alsUnidentifiedTags){
                            String sIncomingTagCleaned = sTag.toLowerCase().trim();
                            boolean bTagFound = false;
                            for(Map.Entry<String, ItemClass_Tag> TagEntry: globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()){
                                String sExistingTagCleaned = TagEntry.getKey().toLowerCase().trim();
                                if(sExistingTagCleaned.equals(sIncomingTagCleaned)){
                                    bTagFound = true;
                                    fi.aliRecognizedTags.add(TagEntry.getValue().iTagID);
                                    fi.aliProspectiveTags.add(TagEntry.getValue().iTagID);
                                    break;
                                }
                            }
                            if(!bTagFound){
                                alsNewUnidentifiedTags.add(sTag.trim());
                            }
                        }
                        fi.alsUnidentifiedTags = alsNewUnidentifiedTags;

                    }
                }

                //Transfer the re-processed tags to the display listing:
                for(ItemClass_File fiDisplayed: alFileItemsDisplay){
                    if(fi.sFileOrFolderName.contentEquals(fiDisplayed.sFileOrFolderName)){
                        fiDisplayed.aliProspectiveTags = fi.aliProspectiveTags;
                        fiDisplayed.alsUnidentifiedTags = fi.alsUnidentifiedTags;
                        fiDisplayed.aliRecognizedTags = fi.aliRecognizedTags;
                        break;
                    }
                }
            }

            //Refresh the records shown on the listview:
            notifyDataSetChanged();

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




        public void updateFileItemDetails(ArrayList<ItemClass_File> alicfIncomingFIs){
            boolean bFoundAndUpdated = false;
            //Find the items to apply individualized tags.
            //This routine is not designed to apply the same tags to multiple items.


            for(ItemClass_File icfIncoming: alicfIncomingFIs) {
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
                    if(alicfIncomingFIs.size() > 0) {
                        String sParentComic = alicfIncomingFIs.get(0).sUriParent;
                        for (ItemClass_File icfUpdate : alFileItems) {
                            if (icfUpdate.sUri.equals(sParentComic)) {
                                icfUpdate.aliProspectiveTags = alicfIncomingFIs.get(0).aliProspectiveTags;
                                icfUpdate.iGrade = alicfIncomingFIs.get(0).iGrade;
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
                case FRAGMENT_IMPORT_3B_COMIC_TAG_IMPORT:
                    return new Fragment_Import_3b_ComicTagImport();
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