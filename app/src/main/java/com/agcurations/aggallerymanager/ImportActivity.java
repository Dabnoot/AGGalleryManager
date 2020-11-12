package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AppCompatActivity;
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

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ImportActivity extends AppCompatActivity {
    private GlobalClass globalClass;

    public static ViewPager2 ViewPager2_Import;
    FragmentImportViewPagerAdapter importViewPagerFragmentAdapter;

    //Fragment page indexes:
    public static final int FRAGMENT_IMPORT_0_ID_MEDIA_CATEGORY = 0;
    public static final int FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION = 1;
    public static final int FRAGMENT_IMPORT_2_ID_SELECT_ITEMS = 2;
    public static final int FRAGMENT_IMPORT_3_ID_SELECT_TAGS = 3;
    public static final int FRAGMENT_IMPORT_4_ID_IMPORT_METHOD = 4;
    public static final int FRAGMENT_IMPORT_5_ID_CONFIRMATION = 5;
    public static final int FRAGMENT_IMPORT_6_ID_EXECUTE_IMPORT = 6;

    public static int FRAGMENT_COUNT = 7;

    //=================================================
    //User selection global variables:

    //FragmentImport_0_MediaCategory
    //Radiobutton selections:
    public static int giImportMediaCategory; //Selected Category
    public static String gsMediaCategoryFolderName = "";

    //FragmentImport_1_StorageLocation
    public static Uri guriImportTreeURI; //Uri of selected base folder holding files/folders to be imported.

    //FragmentImport_2_SelectItems
    public static int SelectItemsListViewWidth = 1000;
    public static FileListCustomAdapter fileListCustomAdapter;

    //FragmentImport_3_SelectTags
    public static String[] sDefaultTags; //Default tags from which user may select.
    public static ArrayList<String> alsImportTags = new ArrayList<>();  //Tags to apply to the import.
    public static String gsImportDestinationFolder;

    //FragmentImport_4_ImportMethod
    public static int IMPORT_METHOD_MOVE = 0;
    public static int IMPORT_METHOD_COPY = 1;
    public static int giImportMethod; //Selected import method.

    //=================================================

    static MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

    // a static variable to get a reference of our activity context
    public static Context contextOfActivity;
    public static Context getContextOfActivity(){
        return contextOfActivity;
    }



    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_activity);

        contextOfActivity = this;

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
        registerReceiver(importDataServiceResponseReceiver, filter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaMetadataRetriever.release();
        unregisterReceiver(importDataServiceResponseReceiver);
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
                    ArrayList<ImportActivity.fileModel> alFileList = (ArrayList<ImportActivity.fileModel>) intent.getSerializableExtra(ImportActivityDataService.EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE);
                    fileListCustomAdapter = new FileListCustomAdapter(getContextOfActivity(), R.id.listView_FolderContents, alFileList);
                }

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
            giImportMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
            gsMediaCategoryFolderName =  "Videos";
        } else if (rbImages.isChecked()){
            giImportMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
            gsMediaCategoryFolderName = "Images";
        } else {
            giImportMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
            gsMediaCategoryFolderName = "Comics";
        }

        //Go to the import folder selection fragment:
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION);
    }

    public void buttonNextClick_StorageLocation(View v){
        //Go to the import folder selection fragment:
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_2_ID_SELECT_ITEMS);
    }

    public void buttonNextClick_ItemSelectComplete(View v){

        //Create an array of tag strings from GlobalClass:
        List<String> alsTags = new ArrayList<String>();
        for (Map.Entry<Integer, String[]> entry : globalClass.gtmCatalogTagReferenceLists.get(giImportMediaCategory).entrySet()){
            alsTags.add(entry.getValue()[GlobalClass.TAG_NAME_INDEX]);
        }
        String[] sTags = (String[]) alsTags.toArray(new String[0]);

        sDefaultTags = sTags; //getResources().getStringArray(R.array.default_video_tags);
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_3_ID_SELECT_TAGS);


    }

    public void buttonNextClick_TagSelectComplete(View v){
        Integer iFolderID;
        //Determine the folder into which the contents will be placed:
        for (Map.Entry<Integer, String[]> entry : globalClass.gtmCatalogTagReferenceLists.get(giImportMediaCategory).entrySet()){
            //Get the key value associated with the text tag:
            if(alsImportTags.get(0).contentEquals(entry.getValue()[GlobalClass.TAG_NAME_INDEX])){
                gsImportDestinationFolder = Integer.toString(entry.getKey());
                break;
            }
        }


        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_4_ID_IMPORT_METHOD);
    }

    public void buttonNextClick_ImportMethodComplete(View v){
        RadioButton rbMove = findViewById(R.id.radioButton_MoveFiles);

        if (rbMove.isChecked()){
            giImportMethod = IMPORT_METHOD_MOVE;
        } else{
            giImportMethod = IMPORT_METHOD_COPY;
        }

        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_5_ID_CONFIRMATION);
    }

    public void buttonNextClick_ImportConfirm(View v){

        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_6_ID_EXECUTE_IMPORT);

        //Begin the import:
        StringBuilder sb = new StringBuilder();
        sb.append(alsImportTags.get(0));
        for(int i = 1; i < alsImportTags.size(); i++){
            sb.append(",");
            sb.append(alsImportTags.get(i));
        }
        String sTags = sb.toString();

        //Create list of files to import:
        ArrayList<fileModel> alImportFileList = new ArrayList<>();
        for(fileModel fm: fileListCustomAdapter.alFileList){
            if(fm.isChecked){
                alImportFileList.add(fm);
            }
        }

        String sDestinationPath = gsImportDestinationFolder;

        //Initiate the file import via ImportActivityDataService:
        ImportActivityDataService.startActionImportFiles(getContextOfActivity(),
                sDestinationPath,
                alImportFileList,
                sTags,
                giImportMediaCategory,
                giImportMethod);

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

    public static class fileModel implements Serializable {

        final public String type; //folder or file
        final public String name;
        final public String extension;
        final public long sizeBytes;
        final public Date dateLastModified;
        public Boolean isChecked;
        final public String uri;
        final public String mimeType;
        public long videoTimeInMilliseconds;

        public String videoTimeText;

        public fileModel(String _type,
                         String _name,
                         String _extension,
                         long _sizeBytes,
                         Date _dateLastModified,
                         Boolean _isChecked,
                         String _uri,
                         String _mime,
                         long _videoTimeInMilliseconds)
        {
            this.uri = _uri;
            this.type = _type;
            this.name = _name;
            this.extension = _extension;
            this.sizeBytes = _sizeBytes;
            this.dateLastModified = _dateLastModified;
            this.isChecked = _isChecked;
            this.mimeType = _mime;
            this.videoTimeInMilliseconds = _videoTimeInMilliseconds;
            this.videoTimeText = "";
        }
    }

    public static String getDurationTextFromMilliseconds(long lMilliseconds){
        String sDurationText;
        sDurationText = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(lMilliseconds),
                TimeUnit.MILLISECONDS.toMinutes(lMilliseconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(lMilliseconds)),
                TimeUnit.MILLISECONDS.toSeconds(lMilliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(lMilliseconds)));
        return sDurationText;
    }

    public class FileListCustomAdapter extends ArrayAdapter<fileModel> {

        final public ArrayList<fileModel> alFileList;
        private ArrayList<fileModel> alFileListDisplay;
        private  boolean bSelectAllSelected = false;

        public FileListCustomAdapter(Context context, int textViewResourceId, ArrayList<fileModel> xmlList) {
            super(context, textViewResourceId, xmlList);
            alFileList = new ArrayList<>(xmlList);
            alFileListDisplay = new ArrayList<>(xmlList);
            SortByFileNameAsc();
        }

        @Override
        public View getView(final int position, View v, ViewGroup parent) {
            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                //My custom list item design is here
                row = inflater.inflate(R.layout.listview_fileitem, parent, false);
            }

            CheckBox cbStorageItemSelect =  row.findViewById(R.id.checkBox_StorageItemSelect);
            ImageView ivFileType =  row.findViewById(R.id.imageView_StorageItemIcon);
            TextView tvLine1 =  row.findViewById(R.id.textView_Line1);
            TextView tvLine2 = row.findViewById(R.id.textView_Line2);

            tvLine1.setText(alFileListDisplay.get(position).name);
            DateFormat dfDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.getDefault() );
            String sLine2 = dfDateFormat.format(alFileListDisplay.get(position).dateLastModified);





            //If type is video or gif, get the duration:
            long durationInMilliseconds = -1L;
            //If mimeType is video or gif, get the duration:
            try {
                if(alFileListDisplay.get(position).videoTimeInMilliseconds == -1L) { //If the time has not already been determined for the video file...
                    if (alFileListDisplay.get(position).mimeType.startsWith("video")) {
                        Uri docUri = Uri.parse(alFileListDisplay.get(position).uri);
                        mediaMetadataRetriever.setDataSource(getContextOfActivity(), docUri);
                        String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        durationInMilliseconds = Long.parseLong(time);
                    } else { //if it's not a video file, check to see if it's a gif:
                        if (alFileListDisplay.get(position).extension.contentEquals("gif")) {
                            //Get the duration of the gif image:
                            Uri docUri = Uri.parse(alFileListDisplay.get(position).uri);
                            Context activityContext = getContextOfActivity();
                            pl.droidsonroids.gif.GifDrawable gd = new pl.droidsonroids.gif.GifDrawable(activityContext.getContentResolver(), docUri);
                            durationInMilliseconds = gd.getDuration();
                        }
                    }
                    if(durationInMilliseconds != -1L) { //If time is now defined, get the text form of the time:
                        alFileListDisplay.get(position).videoTimeText = getDurationTextFromMilliseconds(durationInMilliseconds);
                        alFileListDisplay.get(position).videoTimeInMilliseconds = durationInMilliseconds;
                    }
                }

                if(alFileListDisplay.get(position).videoTimeText.length() > 0){
                    //If the video time text has been defined, recall and display the time:
                    sLine2 = sLine2 + "\tDuration: " + alFileListDisplay.get(position).videoTimeText;
                }

            }catch (Exception e){
                Context activityContext = ImportActivity.getContextOfActivity();
                Toast.makeText(activityContext, e.getMessage() + "; File: " + alFileListDisplay.get(position).name, Toast.LENGTH_LONG).show();
            }

            tvLine2.setText(sLine2);



            //set the image type if folder or file
            if(alFileListDisplay.get(position).type.equals("folder")) {
                ivFileType.setImageResource(R.drawable.baseline_folder_white_18dp);
            } else {
                //ivFileType.setImageResource(R.drawable.baseline_file_white_18dp);

                //Get the Uri of the file and create/display a thumbnail:
                String sUri = alFileListDisplay.get(position).uri;
                Uri uri = Uri.parse(sUri);
                /*Glide.with(getContext()).
                        load(uri).
                        thumbnail(1.0f).
                        into(ivFileType);*/
                Glide.with(getContext()).
                        load(uri).
                        into(ivFileType);
            }




            if(alFileListDisplay.get(position).isChecked ){
                cbStorageItemSelect.setChecked(true);
            } else {
                cbStorageItemSelect.setChecked(false);
            }


            //Set the onClickListener for the row to toggle the checkbox:
            row.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    CheckBox checkBox_StorageItemSelect = view.findViewById(R.id.checkBox_StorageItemSelect);
                    boolean bNewCheckedState = !checkBox_StorageItemSelect.isChecked();
                    checkBox_StorageItemSelect.setChecked(bNewCheckedState);

                    //Find the item that is checked/unchecked in alFileList and apply the property.
                    //  The user will have clicked an item in alFileListDisplay, not alFileList.
                    //  alFileListDisplay may be a subset of alFileList.

                    alFileListDisplay.get(position).isChecked = bNewCheckedState;

                    for(fileModel fm: alFileList){
                        if(fm.name.contentEquals(alFileListDisplay.get(position).name)){
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
                    alFileListDisplay.get(position).isChecked = bNewCheckedState;

                    for(fileModel fm: alFileList){
                        if(fm.name.contentEquals(alFileListDisplay.get(position).name)){
                            fm.isChecked = bNewCheckedState;
                            break;
                        }
                    }

                }
            });

            //If the file item is video mimeType, set the preview button visibility to visible:
            Button button_VideoPreview = row.findViewById(R.id.button_VideoPreview);
            if(alFileListDisplay.get(position).mimeType.startsWith("video")){
                button_VideoPreview.setVisibility(Button.VISIBLE);
                button_VideoPreview.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        //Start the video preview popup activity:
                        Intent intentVideoPreviewPopup = new Intent(ImportActivity.this, VideoPreviewPopup.class);
                        Bundle b = new Bundle();
                        b.putCharSequence(VideoPreviewPopup.FILE_URI_STRING, alFileListDisplay.get(position).uri);
                        intentVideoPreviewPopup.putExtras(b);
                        intentVideoPreviewPopup.putExtra(VideoPreviewPopup.VIDEO_FILE_DURATION_MILLISECONDS_LONG, alFileListDisplay.get(position).videoTimeInMilliseconds);
                        startActivity(intentVideoPreviewPopup);
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

        //To prevent data resetting when scrolled
        @Override
        public int getCount() {
            return alFileListDisplay.size();
        }

        @Override
        public fileModel getItem(int position) {
            return alFileListDisplay.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void toggleSelectAll(){
            bSelectAllSelected = !bSelectAllSelected;
            for(fileModel fmDisplayed:alFileListDisplay){
                fmDisplayed.isChecked = bSelectAllSelected;
                //Translate the selected item state to alFileList:
                for(fileModel fm: alFileList){
                    if(fm.name.contentEquals(fmDisplayed.name)){
                        fm.isChecked = bSelectAllSelected;
                        break;
                    }
                }
            }



        }

        public void applySearch(String sSearch){
            alFileListDisplay.clear();
            for(fileModel fm : alFileList){
                if(fm.name.contains(sSearch)){
                    alFileListDisplay.add(fm);
                }
            }
        }

        public void removeSearch(){
            alFileListDisplay = alFileList;
        }

        //Comparators
        //Allow sort by file name or modified date

        //Sort by file name ascending:
        private class FileNameAscComparator implements Comparator<fileModel> {
            public int compare(fileModel fm1, fileModel fm2) {
                String FileName1 = fm1.name.toUpperCase();
                String FileName2 = fm2.name.toUpperCase();

                //ascending order
                return FileName1.compareTo(FileName2);
            }
        }

        //Sort by file name descending:
        private class FileNameDescComparator implements Comparator<fileModel> {
            public int compare(fileModel fm1, fileModel fm2) {
                String FileName1 = fm1.name.toUpperCase();
                String FileName2 = fm2.name.toUpperCase();

                //descending order
                return FileName2.compareTo(FileName1);
            }
        }

        //Sort by file modified date ascending:
        private class FileModifiedDateAscComparator implements Comparator<fileModel> {

            public int compare(fileModel fm1, fileModel fm2) {
                Date FileDate1 = fm1.dateLastModified;
                Date FileDate2 = fm2.dateLastModified;

                //ascending order
                return FileDate1.compareTo(FileDate2);
            }
        }

        //Sort by file modified date descending:
        private class FileModifiedDateDescComparator implements Comparator<fileModel> {

            public int compare(fileModel fm1, fileModel fm2) {
                Date FileDate1 = fm1.dateLastModified;
                Date FileDate2 = fm2.dateLastModified;

                //descending order
                return FileDate2.compareTo(FileDate1);
            }
        }

        private int iCurrentSortMethod;
        private final int SORT_METHOD_FILENAME_ASC = 1;
        private final int SORT_METHOD_FILENAME_DESC = 2;
        private final int SORT_METHOD_MODIFIED_DATE_ASC = 3;
        private final int SORT_METHOD_MODIFIED_DATE_DESC = 4;
        public void SortByFileNameAsc(){
            Collections.sort(alFileListDisplay, new FileListCustomAdapter.FileNameAscComparator());
            iCurrentSortMethod = SORT_METHOD_FILENAME_ASC;
        }
        public void SortByFileNameDesc(){
            Collections.sort(alFileListDisplay, new FileListCustomAdapter.FileNameDescComparator());
            iCurrentSortMethod = SORT_METHOD_FILENAME_DESC;
        }
        public void SortByDateModifiedAsc(){
            Collections.sort(alFileListDisplay, new FileListCustomAdapter.FileModifiedDateAscComparator());
            iCurrentSortMethod = SORT_METHOD_MODIFIED_DATE_ASC;
        }
        public void SortByDateModifiedDesc(){
            Collections.sort(alFileListDisplay, new FileListCustomAdapter.FileModifiedDateDescComparator());
            iCurrentSortMethod = SORT_METHOD_MODIFIED_DATE_DESC;
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
            }
            return bSortOrderIsAscending;
        }

    }

}