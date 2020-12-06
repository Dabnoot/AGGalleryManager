package com.agcurations.aggallerymanager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ImportActivity extends AppCompatActivity {
    private GlobalClass globalClass;

    public ViewPager2 ViewPager2_Import; //todo: Is this is what is driving all of my fragment components to be required static?
    FragmentImportViewPagerAdapter importViewPagerFragmentAdapter;

    //Fragment page indexes:
    public static final int FRAGMENT_IMPORT_0_ID_MEDIA_CATEGORY = 0;
    public static final int FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION = 1;
    public static final int FRAGMENT_IMPORT_2_ID_SELECT_ITEMS = 2;
    public static final int FRAGMENT_IMPORT_3_ID_SELECT_TAGS = 3;
    public static final int FRAGMENT_IMPORT_4_ID_IMPORT_METHOD = 4;
    public static final int FRAGMENT_IMPORT_5_ID_CONFIRMATION = 5;
    public static final int FRAGMENT_IMPORT_6_ID_EXECUTE_IMPORT = 6;

    public static final int FRAGMENT_COUNT = 7;

    //Constants for individualized tag application via adapter:
    public static final int GET_TAGS_FOR_IMPORT_ITEM = 1050;

    //=================================================
    //User selection global variables:

    //FragmentImport_1_StorageLocation
    public static Uri guriImportTreeURI; //Uri of selected base folder holding files/folders to be imported.

    //FragmentImport_2_SelectItems
    public static int SelectItemsListViewWidth = 1000;  //Expands the listView items to the width of the listview.
    public static FileListCustomAdapter fileListCustomAdapter; //For the file selector.


    //FragmentImport_3_SelectTags
    public static FragmentSelectTagsViewModel viewModelTags; //Used for applying tags globally to an entire import selection.
    public static ImportActivityViewModel importActivityViewModel; //Used to transfer data between fragments.

    //FragmentImport_4_ImportMethod
    public static final int IMPORT_METHOD_MOVE = 0;
    public static final int IMPORT_METHOD_COPY = 1;

    //=================================================

    static MediaMetadataRetriever mediaMetadataRetriever;

    // a static variable to get a reference of our activity context
    /*public static Context contextOfActivity;
    public static Context getContextOfActivity(){
        return contextOfActivity;
    }*/



    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_activity);
        setTitle("Import");

        //contextOfActivity = this;

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        ViewPager2_Import = findViewById(R.id.viewPager_Import);

        importViewPagerFragmentAdapter = new FragmentImportViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        // set Orientation in your ViewPager2
        ViewPager2_Import.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        ViewPager2_Import.setAdapter(importViewPagerFragmentAdapter);

        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        ViewPager2_Import.setUserInputEnabled(false);

        //myViewPager2.setPageTransformer(new MarginPageTransformer(1500));

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();
        //registerReceiver(importDataServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(importDataServiceResponseReceiver,filter);

        mediaMetadataRetriever = new MediaMetadataRetriever();

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        viewModelTags = new ViewModelProvider(this).get(FragmentSelectTagsViewModel.class);

        //Instantiate the ViewModel sharing data between fragments:
        importActivityViewModel = new ViewModelProvider(this).get(ImportActivityViewModel.class);

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

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(ImportActivityDataService.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(ImportActivityDataService.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to request to get directory contents:
                boolean bGetDirContentsResponse = intent.getBooleanExtra(ImportActivityDataService.EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, false);
                if(bGetDirContentsResponse) {
                    ArrayList<FileItem> alFileList = (ArrayList<FileItem>) intent.getSerializableExtra(ImportActivityDataService.EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE);
                    fileListCustomAdapter = new FileListCustomAdapter(getApplicationContext(), R.id.listView_FolderContents, alFileList);
                }

            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GET_TAGS_FOR_IMPORT_ITEM){
            if((resultCode == RESULT_OK) &&
                    (data != null)){
                Bundle b = data.getBundleExtra(VideoPreviewPopup_wTags.TAG_SELECTION_RESULT_BUNDLE);
                if(b == null) return;
                FileItem fileItem = (FileItem) b.getSerializable(VideoPreviewPopup_wTags.FILE_ITEM);
               if(fileItem == null) return;
                ArrayList<Integer> aliTagIDs;
                aliTagIDs = b.getIntegerArrayList(VideoPreviewPopup_wTags.TAG_SELECTION_TAG_IDS);
                //Apply the change to the fileListCustomAdapter:
                fileListCustomAdapter.applyTagsToItem(fileItem.uri, aliTagIDs);
            }
        }


    }

    //======================================================
    //======  FRAGMENT NAVIGATION ROUTINES  ================
    //======================================================


    @Override
    public void onBackPressed() {

        if (ViewPager2_Import.getCurrentItem() != 0) {
            ViewPager2_Import.setCurrentItem(ViewPager2_Import.getCurrentItem() - 1,false);
        }else{
            finish();
        }

    }

    public void buttonNextClick_MediaCategory(View v){
        RadioButton rbVideos = findViewById(R.id.radioButton_ImportVideos);
        RadioButton rbImages = findViewById(R.id.radioButton_ImportImages);
        //RadioButton rbComics = findViewById(R.id.radioButton_ImportComics);



        if (rbVideos.isChecked()){
            importActivityViewModel.iImportMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        } else if (rbImages.isChecked()){
            importActivityViewModel.iImportMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        } else {
            importActivityViewModel.iImportMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        }

        //Go to the import folder selection fragment:
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION);
    }

    public void buttonNextClick_StorageLocation(View v){
        //Go to the import folder selection fragment:
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_2_ID_SELECT_ITEMS);
    }

    public void buttonNextClick_ItemSelectComplete(View v){
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_3_ID_SELECT_TAGS);
    }

    public void buttonNextClick_TagSelectComplete(View v){
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_4_ID_IMPORT_METHOD);
    }

    public void buttonNextClick_ImportMethodComplete(View v){
        RadioButton rbMove = findViewById(R.id.radioButton_MoveFiles);

        if (rbMove.isChecked()){
            importActivityViewModel.iImportMethod = IMPORT_METHOD_MOVE;
        } else{
            importActivityViewModel.iImportMethod = IMPORT_METHOD_COPY;
        }

        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_5_ID_CONFIRMATION);
    }

    public void buttonNextClick_ImportConfirm(View v){
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_6_ID_EXECUTE_IMPORT);
    }

    public void buttonNextClick_ImportFinish(View v){
        finish();
    }

    //================================================
    //  Data Structures
    //================================================
    //======================================================================================
    //===== File ListView Adapter ==========================================================
    //======================================================================================

    //References:
    //  https://thecodeprogram.com/build-your-own-file-selector-screen-on-android





    public class FileListCustomAdapter extends ArrayAdapter<FileItem> {

        final public ArrayList<FileItem> alFileItems;
        private ArrayList<FileItem> alFileItemsDisplay;
        private boolean bSelectAllSelected = false;

        public FileListCustomAdapter(Context context, int textViewResourceId, ArrayList<FileItem> alfi) {
            super(context, textViewResourceId, alfi);
            alFileItems = new ArrayList<>(alfi);
            alFileItemsDisplay = new ArrayList<>(alfi);
            SortByFileNameAsc();
        }

        @Override
        public View getView(final int position, View v, ViewGroup parent) {
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

            tvLine1.setText(alFileItemsDisplay.get(position).name);
            DateFormat dfDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.getDefault() );
            String sLine2 = dfDateFormat.format(alFileItemsDisplay.get(position).dateLastModified);


            //If type is video or gif, get the duration:
            long durationInMilliseconds = alFileItemsDisplay.get(position).videoTimeInMilliseconds;
            //If mimeType is video or gif, get the duration:
            try {
                if(durationInMilliseconds == -1L) { //If the time has not already been determined for the video file...
                    if (alFileItemsDisplay.get(position).mimeType.startsWith("video")) {
                        Uri docUri = Uri.parse(alFileItemsDisplay.get(position).uri);
                        mediaMetadataRetriever.setDataSource(getApplicationContext(), docUri);
                        String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        durationInMilliseconds = Long.parseLong(time);
                    } else { //if it's not a video file, check to see if it's a gif:
                        if (alFileItemsDisplay.get(position).extension.contentEquals("gif")) {
                            //Get the duration of the gif image:
                            Uri docUri = Uri.parse(alFileItemsDisplay.get(position).uri);
                            Context activityContext = getApplicationContext();
                            pl.droidsonroids.gif.GifDrawable gd = new pl.droidsonroids.gif.GifDrawable(activityContext.getContentResolver(), docUri);
                            durationInMilliseconds = gd.getDuration();
                        }
                    }
                    if(durationInMilliseconds != -1L) { //If time is now defined, get the text form of the time:
                        alFileItemsDisplay.get(position).videoTimeInMilliseconds = durationInMilliseconds;
                    }
                }
                if(durationInMilliseconds > -1L){
                    alFileItemsDisplay.get(position).videoTimeText = GlobalClass.getDurationTextFromMilliseconds(durationInMilliseconds);
                }

                if(alFileItemsDisplay.get(position).videoTimeText.length() > 0){
                    //If the video time text has been defined, recall and display the time:
                    sLine2 = sLine2 + "\tDuration: " + alFileItemsDisplay.get(position).videoTimeText;
                }

            }catch (Exception e){
                Toast.makeText(getApplicationContext(), e.getMessage() + "; File: " + alFileItemsDisplay.get(position).name, Toast.LENGTH_LONG).show();
            }

            tvLine2.setText(sLine2);

            //Get tag text to apply to list item if tags are assigned to the item:
            StringBuilder sbTags = new StringBuilder();
            sbTags.append("Tags: ");
            ArrayList<Integer> aliTagIDs = alFileItemsDisplay.get(position).prospectiveTags;

            if(aliTagIDs != null){
                if(aliTagIDs.size() > 0) {
                    sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(0), importActivityViewModel.iImportMediaCategory));
                    for (int i = 1; i < aliTagIDs.size(); i++) {
                        sbTags.append(", ");
                        sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(i), importActivityViewModel.iImportMediaCategory));
                    }
                }
            }
            tvLine3.setText(sbTags.toString());

            //set the image type if folder or file
            if(alFileItemsDisplay.get(position).type.equals("folder")) {
                ivFileType.setImageResource(R.drawable.baseline_folder_white_18dp);
            } else {
                //ivFileType.setImageResource(R.drawable.baseline_file_white_18dp);

                //Get the Uri of the file and create/display a thumbnail:
                String sUri = alFileItemsDisplay.get(position).uri;
                Uri uri = Uri.parse(sUri);
                Glide.with(getContext()).
                        load(uri).
                        into(ivFileType);
            }


            cbStorageItemSelect.setChecked(alFileItemsDisplay.get(position).isChecked);


            //Set the onClickListener for the row to toggle the checkbox:
            row.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    CheckBox checkBox_StorageItemSelect = view.findViewById(R.id.checkBox_StorageItemSelect);
                    boolean bNewCheckedState = !checkBox_StorageItemSelect.isChecked();
                    checkBox_StorageItemSelect.setChecked(bNewCheckedState);
                    alFileItemsDisplay.get(position).isChecked = bNewCheckedState;

                    //Find the item that is checked/unchecked in alFileList and apply the property.
                    //  The user will have clicked an item in alFileListDisplay, not alFileList.
                    //  alFileListDisplay may be a subset of alFileList.
                    for(FileItem fm: alFileItems){
                        if(fm.name.contentEquals(alFileItemsDisplay.get(position).name)){
                            fm.isChecked = bNewCheckedState;
                            break;
                        }
                    }

                }
            });

            //Set the onClickListener for the checkbox to toggle the checkbox:
            CheckBox checkBox_StorageItemSelect = row.findViewById(R.id.checkBox_StorageItemSelect);
            checkBox_StorageItemSelect.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    CheckBox checkBox_StorageItemSelect = (CheckBox) view;
                    boolean bNewCheckedState = checkBox_StorageItemSelect.isChecked();

                    //Find the item that is checked/unchecked in alFileList and apply the property.
                    //  The user will have clicked an item in alFileListDisplay, not alFileList.
                    //  alFileListDisplay may be a subset of alFileList.
                    alFileItemsDisplay.get(position).isChecked = bNewCheckedState;

                    for(FileItem fm: alFileItems){
                        if(fm.name.contentEquals(alFileItemsDisplay.get(position).name)){
                            fm.isChecked = bNewCheckedState;
                            break;
                        }
                    }

                }
            });

            //If the file item is video mimeType, set the preview button visibility to visible:
            Button button_VideoPreview = row.findViewById(R.id.button_VideoPreview);
            if(alFileItemsDisplay.get(position).mimeType.startsWith("video")){
                button_VideoPreview.setVisibility(Button.VISIBLE);
                button_VideoPreview.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        //Start the video preview popup activity:
                        Intent intentVideoPreviewPopup = new Intent(ImportActivity.this, VideoPreviewPopup_wTags.class);
                        Bundle b = new Bundle();
                        b.putCharSequence(VideoPreviewPopup_wTags.FILE_URI_STRING, alFileItemsDisplay.get(position).uri);
                        FileItem fileItem = alFileItemsDisplay.get(position);
                        b.putSerializable(VideoPreviewPopup_wTags.FILE_ITEM, alFileItemsDisplay.get(position));
                        intentVideoPreviewPopup.putExtras(b);
                        intentVideoPreviewPopup.putExtra(VideoPreviewPopup.VIDEO_FILE_DURATION_MILLISECONDS_LONG, alFileItemsDisplay.get(position).videoTimeInMilliseconds);
                        startActivityForResult(intentVideoPreviewPopup, GET_TAGS_FOR_IMPORT_ITEM);
                    }
                });
            } else {
                button_VideoPreview.setVisibility(Button.INVISIBLE);
            }


            //Expand the width of the listItem to the width of the ListView.
            //  This makes it so that the listItem responds to the click even when
            //  the click is off of the text.
            row.setMinimumWidth(SelectItemsListViewWidth);

            return row;
        }

        public void applyTagsToItem(String sFileUri, ArrayList<Integer> aliTagIDs){
            boolean bFoundAndUpdated = false;
            //Find the item to apply tags:
            for(FileItem fm: alFileItems){
                if(fm.uri.contentEquals(sFileUri)){
                    fm.prospectiveTags = aliTagIDs;
                    fm.isChecked = true;
                    bFoundAndUpdated = true;
                    break;
                }
            }
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
        public FileItem getItem(int position) {
            return alFileItemsDisplay.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void toggleSelectAll(){
            bSelectAllSelected = !bSelectAllSelected;
            for(FileItem fmDisplayed: alFileItemsDisplay){
                fmDisplayed.isChecked = bSelectAllSelected;
                //Translate the selected item state to alFileList:
                for(FileItem fm: alFileItems){
                    if(fm.name.contentEquals(fmDisplayed.name)){
                        fm.isChecked = bSelectAllSelected;
                        break;
                    }
                }
            }



        }

        public void applySearch(String sSearch){
            alFileItemsDisplay.clear();
            for(FileItem fm : alFileItems){
                if(fm.name.contains(sSearch)){
                    alFileItemsDisplay.add(fm);
                }
            }
        }

        public void removeSearch(){
            alFileItemsDisplay = alFileItems;
        }

        //Comparators
        //Allow sort by file name or modified date

        //Sort by file name ascending:
        private class FileNameAscComparator implements Comparator<FileItem> {
            public int compare(FileItem fm1, FileItem fm2) {
                String FileName1 = fm1.name.toUpperCase();
                String FileName2 = fm2.name.toUpperCase();

                //ascending order
                return FileName1.compareTo(FileName2);
            }
        }

        //Sort by file name descending:
        private class FileNameDescComparator implements Comparator<FileItem> {
            public int compare(FileItem fm1, FileItem fm2) {
                String FileName1 = fm1.name.toUpperCase();
                String FileName2 = fm2.name.toUpperCase();

                //descending order
                return FileName2.compareTo(FileName1);
            }
        }

        //Sort by file modified date ascending:
        private class FileModifiedDateAscComparator implements Comparator<FileItem> {

            public int compare(FileItem fm1, FileItem fm2) {
                Date FileDate1 = fm1.dateLastModified;
                Date FileDate2 = fm2.dateLastModified;

                //ascending order
                return FileDate1.compareTo(FileDate2);
            }
        }

        //Sort by file modified date descending:
        private class FileModifiedDateDescComparator implements Comparator<FileItem> {

            public int compare(FileItem fm1, FileItem fm2) {
                Date FileDate1 = fm1.dateLastModified;
                Date FileDate2 = fm2.dateLastModified;

                //descending order
                return FileDate2.compareTo(FileDate1);
            }
        }

        //Sort by file modified date ascending:
        private class FileDurationAscComparator implements Comparator<FileItem> {

            public int compare(FileItem fm1, FileItem fm2) {
                Long FileDuration1 = fm1.videoTimeInMilliseconds;
                Long FileDuration2 = fm2.videoTimeInMilliseconds;

                //ascending order
                return FileDuration1.compareTo(FileDuration2);
            }
        }

        //Sort by file modified date descending:
        private class FileDurationDescComparator implements Comparator<FileItem> {

            public int compare(FileItem fm1, FileItem fm2) {
                Long FileDuration1 = fm1.videoTimeInMilliseconds;
                Long FileDuration2 = fm2.videoTimeInMilliseconds;

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

}