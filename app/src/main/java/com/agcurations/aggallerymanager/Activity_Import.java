package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import java.util.TreeMap;

public class Activity_Import extends AppCompatActivity {
    private GlobalClass globalClass;

    public ViewPager2 ViewPager2_Import;
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
    public static ViewModel_Fragment_SelectTags viewModelTags; //Used for applying tags globally to an entire import selection.
    public static ViewModel_ImportActivity viewModelImportActivity; //Used to transfer data between fragments.

    //FragmentImport_4_ImportMethod
    public static final int IMPORT_METHOD_MOVE = 0;
    public static final int IMPORT_METHOD_COPY = 1;

    //=================================================

    static MediaMetadataRetriever mediaMetadataRetriever;

    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        setTitle("Import");

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        ViewPager2_Import = findViewById(R.id.viewPager_Import);

        importViewPagerFragmentAdapter = new FragmentImportViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        // set Orientation in your ViewPager2
        ViewPager2_Import.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        ViewPager2_Import.setAdapter(importViewPagerFragmentAdapter);

        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        ViewPager2_Import.setUserInputEnabled(false);

        //myViewPager2.setPageTransformer(new MarginPageTransformer(1500)); todo

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
            bError = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to request to get directory contents:
                boolean bGetDirContentsResponse = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE, false);
                if(bGetDirContentsResponse) {
                    ArrayList<ItemClass_File> alFileList = (ArrayList<ItemClass_File>) intent.getSerializableExtra(Service_Import.EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE);
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
                Bundle b = data.getBundleExtra(Activity_VideoPreview.TAG_SELECTION_RESULT_BUNDLE);
                if(b == null) return;
                ItemClass_File fileItem = (ItemClass_File) b.getSerializable(Activity_VideoPreview.FILE_ITEM);
               if(fileItem == null) return;
                ArrayList<Integer> aliTagIDs;
                aliTagIDs = b.getIntegerArrayList(Activity_VideoPreview.TAG_SELECTION_TAG_IDS);
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

    public void buttonNextClick_MediaCategorySelected(View v){
        RadioButton rbVideos = findViewById(R.id.radioButton_ImportVideos);
        RadioButton rbImages = findViewById(R.id.radioButton_ImportImages);
        //RadioButton rbComics = findViewById(R.id.radioButton_ImportComics);



        if (rbVideos.isChecked()){
            viewModelImportActivity.iImportMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        } else if (rbImages.isChecked()){
            viewModelImportActivity.iImportMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        } else {
            viewModelImportActivity.iImportMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
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
            viewModelImportActivity.iImportMethod = IMPORT_METHOD_MOVE;
        } else{
            viewModelImportActivity.iImportMethod = IMPORT_METHOD_COPY;
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
    //  Adapters
    //================================================

    public class FileListCustomAdapter extends ArrayAdapter<ItemClass_File> {

        final public ArrayList<ItemClass_File> alFileItems;
        private ArrayList<ItemClass_File> alFileItemsDisplay;
        private boolean bSelectAllSelected = false;

        public FileListCustomAdapter(Context context, int textViewResourceId, ArrayList<ItemClass_File> alfi) {
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
                        if (alFileItemsDisplay.get(position).extension.contentEquals(".gif")) {
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
                    sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(0), viewModelImportActivity.iImportMediaCategory));
                    for (int i = 1; i < aliTagIDs.size(); i++) {
                        sbTags.append(", ");
                        sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(i), viewModelImportActivity.iImportMediaCategory));
                    }
                }
            }
            tvLine3.setText(sbTags.toString());

            //set the image type if folder or file
            if(alFileItemsDisplay.get(position).type.equals("folder")) {
                ivFileType.setImageResource(R.drawable.baseline_folder_white_18dp);
            } else {
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
                    for(ItemClass_File fm: alFileItems){
                        if(fm.name.contentEquals(alFileItemsDisplay.get(position).name)){
                            fm.isChecked = bNewCheckedState;
                            break;
                        }
                    }
                }
            });
            //Expand the width of the listItem to the width of the ListView.
            //  This makes it so that the listItem responds to the click even when
            //  the click is off of the text.
            row.setMinimumWidth(SelectItemsListViewWidth);

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

                    for(ItemClass_File fm: alFileItems){
                        if(fm.name.contentEquals(alFileItemsDisplay.get(position).name)){
                            fm.isChecked = bNewCheckedState;
                            break;
                        }
                    }

                }
            });

            //Code the button to delete a file in the ListView:
            Button button_Delete = row.findViewById(R.id.button_Delete);
            button_Delete.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(Activity_Import.this);
                    builder.setTitle("Delete Item");
                    builder.setMessage("Are you sure you want to delete this item?\n" + alFileItemsDisplay.get(position).name);
                    //builder.setIcon(R.drawable.ic_launcher);
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            Uri uriSourceFile;
                            uriSourceFile = Uri.parse(alFileItemsDisplay.get(position).uri);
                            DocumentFile dfSource = DocumentFile.fromSingleUri(getApplicationContext(), uriSourceFile);

                            if(dfSource != null) {
                                String sMessage;
                                if (!dfSource.delete()) {
                                    sMessage = "Could not delete file.";
                                } else {
                                    sMessage = "File deleted.";
                                }
                                Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_LONG).show();
                            }
                            //Find the item in the alFileItems list and delete it:
                            ItemClass_File fmSelected = alFileItemsDisplay.get(position);
                            ItemClass_File fmSource;
                            for(int i = 0; i < alFileItems.size(); i++){
                                fmSource = alFileItems.get(i);
                                if(fmSelected.name.equals(fmSource.name)){
                                    alFileItems.remove(i);
                                    break;
                                }
                            }
                            alFileItemsDisplay.remove(position);
                            notifyDataSetChanged();
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
            Button button_VideoPreview = row.findViewById(R.id.button_VideoPreview);
            if(alFileItemsDisplay.get(position).mimeType.startsWith("video")  ||
                    (alFileItemsDisplay.get(position).mimeType.equals("application/octet-stream") &&
                            alFileItemsDisplay.get(position).extension.equals(".mp4"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid)
                button_VideoPreview.setVisibility(Button.VISIBLE);
                button_VideoPreview.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        //Start the video preview popup activity:
                        Intent intentVideoPreviewPopup = new Intent(Activity_Import.this, Activity_VideoPreview.class);
                        Bundle b = new Bundle();

                        //Form a list of tags in use by the selected items. There may be a tag that has just been applied
                        //  that is not currently used by any tags in the catalog. Such a tag would not get picked up by the
                        //  IN-USE function in globalClass, and get listed in the IN-USE section of the tag selector. The
                        //  user would otherwise be
                        TreeMap<String, ItemClass_Tag> tmImportSessionTagsInUse = new TreeMap<>();
                        for(ItemClass_File fm: alFileItems){ //Loop through file items in this listView.
                            if(fm.isChecked){               //If the user has selected this fileItem...
                                for(Integer iTagID: fm.prospectiveTags){  //loop through the prospectiveTags and add them to the non-duplicate TreeMap.
                                    String sTagText = globalClass.getTagTextFromID(iTagID, viewModelImportActivity.iImportMediaCategory);
                                    tmImportSessionTagsInUse.put(sTagText,new ItemClass_Tag(iTagID, sTagText));
                                }
                            }
                        }
                        //Add the treeMap to the bundle to send to the preview:
                        b.putSerializable(Activity_VideoPreview.IMPORT_SESSION_TAGS_IN_USE, tmImportSessionTagsInUse);

                        ItemClass_File fileItem = alFileItemsDisplay.get(position);
                        b.putSerializable(Activity_VideoPreview.FILE_ITEM, alFileItemsDisplay.get(position));
                        intentVideoPreviewPopup.putExtras(b);
                        intentVideoPreviewPopup.putExtra(Activity_VideoPreview.VIDEO_FILE_DURATION_MILLISECONDS_LONG, alFileItemsDisplay.get(position).videoTimeInMilliseconds);
                        startActivityForResult(intentVideoPreviewPopup, GET_TAGS_FOR_IMPORT_ITEM);
                    }
                });
            } else {
                button_VideoPreview.setVisibility(Button.INVISIBLE);
            }

            return row;
        }

        public void applyTagsToItem(String sFileUri, ArrayList<Integer> aliTagIDs){
            boolean bFoundAndUpdated = false;
            //Find the item to apply tags:
            for(ItemClass_File fm: alFileItems){
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
        public ItemClass_File getItem(int position) {
            return alFileItemsDisplay.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void toggleSelectAll(){
            bSelectAllSelected = !bSelectAllSelected;
            for(ItemClass_File fmDisplayed: alFileItemsDisplay){
                fmDisplayed.isChecked = bSelectAllSelected;
                //Translate the selected item state to alFileList:
                for(ItemClass_File fm: alFileItems){
                    if(fm.name.contentEquals(fmDisplayed.name)){
                        fm.isChecked = bSelectAllSelected;
                        break;
                    }
                }
            }



        }

        public void applySearch(String sSearch){
            alFileItemsDisplay.clear();
            for(ItemClass_File fm : alFileItems){
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
        private class FileNameAscComparator implements Comparator<ItemClass_File> {
            public int compare(ItemClass_File fm1, ItemClass_File fm2) {
                String FileName1 = fm1.name.toUpperCase();
                String FileName2 = fm2.name.toUpperCase();

                //ascending order
                return FileName1.compareTo(FileName2);
            }
        }

        //Sort by file name descending:
        private class FileNameDescComparator implements Comparator<ItemClass_File> {
            public int compare(ItemClass_File fm1, ItemClass_File fm2) {
                String FileName1 = fm1.name.toUpperCase();
                String FileName2 = fm2.name.toUpperCase();

                //descending order
                return FileName2.compareTo(FileName1);
            }
        }

        //Sort by file modified date ascending:
        private class FileModifiedDateAscComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fm1, ItemClass_File fm2) {
                Date FileDate1 = fm1.dateLastModified;
                Date FileDate2 = fm2.dateLastModified;

                //ascending order
                return FileDate1.compareTo(FileDate2);
            }
        }

        //Sort by file modified date descending:
        private class FileModifiedDateDescComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fm1, ItemClass_File fm2) {
                Date FileDate1 = fm1.dateLastModified;
                Date FileDate2 = fm2.dateLastModified;

                //descending order
                return FileDate2.compareTo(FileDate1);
            }
        }

        //Sort by file modified date ascending:
        private class FileDurationAscComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fm1, ItemClass_File fm2) {
                Long FileDuration1 = fm1.videoTimeInMilliseconds;
                Long FileDuration2 = fm2.videoTimeInMilliseconds;

                //ascending order
                return FileDuration1.compareTo(FileDuration2);
            }
        }

        //Sort by file modified date descending:
        private class FileDurationDescComparator implements Comparator<ItemClass_File> {

            public int compare(ItemClass_File fm1, ItemClass_File fm2) {
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
                case FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION:
                    return new Fragment_Import_1_StorageLocation();
                case FRAGMENT_IMPORT_2_ID_SELECT_ITEMS:
                    return new Fragment_Import_2_SelectItems();
                case FRAGMENT_IMPORT_3_ID_SELECT_TAGS:
                    return new Fragment_Import_3_SelectTags();
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