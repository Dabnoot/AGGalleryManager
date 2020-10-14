package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImportActivity extends AppCompatActivity {

    public static ViewPager2 ViewPager2_Import;
    FragmentImportViewPagerAdapter importViewPagerFragmentAdapter;
    private ArrayList<Fragment> arrayList_ImportFragments = new ArrayList<>();

    //Fragment page indexes:
    public static final int FRAGMENT_IMPORT_0_ID_MEDIA_CATEGORY = 0;
    public static final int FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION = 1;
    public static final int FRAGMENT_IMPORT_2_ID_SELECT_ITEMS = 2;
    public static final int FRAGMENT_IMPORT_3_ID_SELECT_TAGS = 3;

    public static int FRAGMENT_COUNT = 4;

    //=================================================
    //User selection global variables:

    //FragmentImport_0_MediaCategory
    //Radiobutton selections:
    public final int IMPORT_MEDIA_CATEGORY_VIDEOS = 0;
    public final int IMPORT_MEDIA_CATEGORY_IMAGES = 1;
    public final int IMPORT_MEDIA_CATEGORY_COMICS = 2;
    public int giImportMediaCategory; //Selected Category

    //FragmentImport_1_StorageLocation
    public static Uri guriImportTreeURI; //Uri of selected base folder holding files/folders to be imported.

    //FragmentImport_2_SelectItems
    public static FileListCustomAdapter fileListCustomAdapter;

    //FragmentImport_3_SelectTags
    public static String[] sDefaultTags; //Default tags from which user may select.
    public static ArrayList<String> alsImportTags = new ArrayList<>();  //Tags to apply to the import.

    //=================================================


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        ViewPager2_Import = findViewById(R.id.viewPager_Import);

        // add Fragments in your ViewPagerFragmentAdapter class
        arrayList_ImportFragments.add(new FragmentImport_0_MediaCategory());
        arrayList_ImportFragments.add(new FragmentImport_1_StorageLocation());
        arrayList_ImportFragments.add(new FragmentImport_2_SelectItems());
        arrayList_ImportFragments.add(new FragmentImport_3_SelectTags());

        importViewPagerFragmentAdapter = new FragmentImportViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        // set Orientation in your ViewPager2
        ViewPager2_Import.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        ViewPager2_Import.setAdapter(importViewPagerFragmentAdapter);

        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        ViewPager2_Import.setUserInputEnabled(false);

        //myViewPager2.setPageTransformer(new MarginPageTransformer(1500));
    }

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
            giImportMediaCategory = IMPORT_MEDIA_CATEGORY_VIDEOS;
        } else if (rbImages.isChecked()){
            giImportMediaCategory = IMPORT_MEDIA_CATEGORY_IMAGES;
        } else {
            giImportMediaCategory = IMPORT_MEDIA_CATEGORY_COMICS;
        }

        //Go to the import folder selection fragment:
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_1_ID_STORAGE_LOCATION);
    }

    public void buttonNextClick_StorageLocation(View v){
        //Go to the import folder selection fragment:
        ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_2_ID_SELECT_ITEMS);
    }

    public void buttonNextClick_ItemSelectComplete(View v){
        if (giImportMediaCategory == IMPORT_MEDIA_CATEGORY_VIDEOS) {

            //Create an array of tag strings from GlobalClass:
            List<String> alsTags = new ArrayList<String>();
            for (Map.Entry<Integer, String> entry : GlobalClass.gtmAllUniqueCatalogVideoTags.entrySet()){
                alsTags.add(entry.getValue());
            }
            String[] sTags = (String[]) alsTags.toArray(new String[0]);

            sDefaultTags = sTags; //getResources().getStringArray(R.array.default_video_tags);
            ViewPager2_Import.setCurrentItem(FRAGMENT_IMPORT_3_ID_SELECT_TAGS);
        }

    }

    public void buttonNextClick_TagSelectComplete(View v){

    }


    //================================================
    //  Data Structures
    //================================================
    //======================================================================================
    //===== File ListView Adapter ==========================================================
    //======================================================================================

    //References:
    //  https://thecodeprogram.com/build-your-own-file-selector-screen-on-android

    public static class fileModel {
        final public Uri uri;
        final public String type; //folder or file
        final public String name;
        final public String path;
        final public long size;
        final public Date dateLastModified;
        public Boolean isChecked;

        public fileModel(String _type, String _name ,String _path, long _size, Date _dateLastModified, Boolean _isChecked)
        {
            this.uri = null;
            this.type = _type;
            this.name = _name;
            this.path = _path;
            this.size = _size;
            this.dateLastModified = _dateLastModified;
            this.isChecked = _isChecked;
        }

        //Initialize with Uri instead of path:
        public fileModel(String _type, String _name , Uri _uri, long _size, Date _dateLastModified, Boolean _isChecked)
        {
            this.uri = _uri;
            this.type = _type;
            this.name = _name;
            this.path = "";
            this.size = _size;
            this.dateLastModified = _dateLastModified;
            this.isChecked = _isChecked;
        }
    }

    public static class FileListCustomAdapter extends ArrayAdapter<fileModel> {

        final private ArrayList<fileModel> alFileList;
        private ArrayList<fileModel> alFileListDisplay;
        private  boolean bSelectAllSelected = false;

        public FileListCustomAdapter(Context context, int textViewResourceId, ArrayList<fileModel> xmlList) {
            super(context, textViewResourceId, xmlList);
            this.alFileList = new ArrayList<>(xmlList);
            this.alFileListDisplay = new ArrayList<>(xmlList);
            SortByFileNameAsc();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                //My custom list item design is here
                row = inflater.inflate(R.layout.listview_import_storageitem, parent, false);
            }

            CheckBox cbStorageItemSelect =  row.findViewById(R.id.checkBox_StorageItemSelect);
            ImageView ivFileType =  row.findViewById(R.id.imageView_StorageItemIcon);
            TextView tvFileName =  row.findViewById(R.id.textView_FileName);
            TextView tvModifiedDate = row.findViewById(R.id.textView_DateModified);

            tvFileName.setText(this.alFileListDisplay.get(position).name);
            DateFormat dfDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.getDefault() );
            String sdf = dfDateFormat.format(this.alFileListDisplay.get(position).dateLastModified);
            tvModifiedDate.setText(sdf);

            //set the image type if folder or file
            if(this.alFileListDisplay.get(position).type.equals("folder")) {
                ivFileType.setImageResource(R.drawable.baseline_folder_white_18dp);
            } else {
                ivFileType.setImageResource(R.drawable.baseline_file_white_18dp);
            }

            if(alFileListDisplay.get(position).isChecked ){
                cbStorageItemSelect.setChecked(true);
            } else {
                cbStorageItemSelect.setChecked(false);
            }

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
            for(fileModel fm:alFileListDisplay){
                fm.isChecked = bSelectAllSelected;
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
        private static class FileNameAscComparator implements Comparator<fileModel> {
            public int compare(fileModel fm1, fileModel fm2) {
                String FileName1 = fm1.name.toUpperCase();
                String FileName2 = fm2.name.toUpperCase();

                //ascending order
                return FileName1.compareTo(FileName2);
            }
        }

        //Sort by file name descending:
        private static class FileNameDescComparator implements Comparator<fileModel> {
            public int compare(fileModel fm1, fileModel fm2) {
                String FileName1 = fm1.name.toUpperCase();
                String FileName2 = fm2.name.toUpperCase();

                //descending order
                return FileName2.compareTo(FileName1);
            }
        }

        //Sort by file modified date ascending:
        private static class FileModifiedDateAscComparator implements Comparator<fileModel> {

            public int compare(fileModel fm1, fileModel fm2) {
                Date FileDate1 = fm1.dateLastModified;
                Date FileDate2 = fm2.dateLastModified;

                //ascending order
                return FileDate1.compareTo(FileDate2);
            }
        }

        //Sort by file modified date descending:
        private static class FileModifiedDateDescComparator implements Comparator<fileModel> {

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