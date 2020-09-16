package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

public class ImportComicsGuidedActivity extends AppCompatActivity {

    public static final int MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE = 2002;
    public static final int REQUEST_CODE_GET_IMPORT_FOLDER = 2004;
    public static final int REQUEST_CODE_GET_IMPORT_FILE = 2005;

    private static final String LOG_TAG = "ImportComicsGuidedActivity";

    //The pager widget, which handles animation and allows swiping horizontally to access previous
    //  and next wizard steps:
    private ViewPager2 viewPager;

    private FileListCustomAdapter fileListCustomAdapter;

    private Stack<String[]> stackImportSteps = new Stack<>();

    private GlobalClass globalClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_comics_guided_activity);

        globalClass = (GlobalClass) getApplicationContext();

        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.viewPager);

        ArrayList<Integer> ai = new ArrayList<>();
        ai.add(1);
        ai.add(2);
        ai.add(3);
        ai.add(4);

        //The pager adapter, which provides the pages to the view pager widget.
        ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(ai);
        viewPager.setAdapter(pagerAdapter);
        //Stop the user from swiping left and right on the ViewPager (control with Next button):
        viewPager.setUserInputEnabled(false);


    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            stackImportSteps.pop();
            String[] sPreviousStepData = stackImportSteps.pop();
            int iPreviousPage = Integer.parseInt(sPreviousStepData[0]);
            viewPager.setCurrentItem(iPreviousPage);
        }
    }


    public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewHolder> {

        ArrayList<Integer> ai;
        int[] layouts = {R.layout.activity_import_comics_guided_step_0,
                         R.layout.activity_import_comics_guided_step_1_1,
                         R.layout.activity_import_comics_guided_step_1_2,
                         R.layout.activity_import_comics_guided_step_2_1};

        //Declare integers to track pages:
        private static final int STEP_0_SelectImportSource = 0;
        private static final int STEP_1_1_Select_Folder = 1;
        private static final int STEP_1_2_Select_Folder_Items = 2;
        private static final int STEP_2_1_EnterWebAddress = 3;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {

            public final RadioButton radioButton_ImportComicsFromStorage;

            public final Button button_SelectFolder;
            public final TextView textView_Label_Selected_Folder;
            public final TextView textView_Selected_Import_Folder;

            public CheckBox checkBox_SelectAllStorageItems;
            public Spinner spinner_SortBy;
            public ImageView imageView_SortOrder;
            public EditText editText_Search;
            public ListView listView_FolderContents;

            public final Button button_Next;


            public ViewHolder(View v) {
                super(v);
                radioButton_ImportComicsFromStorage = v.findViewById(R.id.radioButton_ImportComicsFromStorage);

                button_SelectFolder = v.findViewById(R.id.button_SelectFolder);
                textView_Label_Selected_Folder = v.findViewById(R.id.textView_Label_Selected_Folder);
                textView_Selected_Import_Folder = v.findViewById(R.id.textView_Selected_Import_Folder);

                checkBox_SelectAllStorageItems = v.findViewById(R.id.checkBox_SelectAllStorageItems);
                spinner_SortBy = v.findViewById(R.id.spinner_SortBy);
                imageView_SortOrder = v.findViewById(R.id.imageView_SortOrder);
                editText_Search = v.findViewById(R.id.editText_Search);
                listView_FolderContents = v.findViewById(R.id.listView_FolderContents);

                button_Next = v.findViewById(R.id.button_NextStep);

            }
        }

        public ViewPagerAdapter(ArrayList<Integer> data) {
            this.ai = data;
        }


        @Override
        public int getItemViewType(int position) {
            if(position == 0) {
                return STEP_0_SelectImportSource;
            } else if(position == 1) {
                return STEP_1_1_Select_Folder;
            } else if(position == 2) {
                return STEP_1_2_Select_Folder_Items;
            } else {
                return STEP_2_1_EnterWebAddress;
            }

        }


        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            // create a new view
            View v;

            v = inflater.inflate(layouts[viewType], parent, false);


            return new ViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(final ImportComicsGuidedActivity.ViewPagerAdapter.ViewHolder holder, final int position) {
            // - get element from your data set at this position
            // - replace the contents of the view with that element

            //If this is the first view, configure the next button to perform actions:
            if(position == STEP_0_SelectImportSource) {
                //Push step 0 onto the step stack:
                stackImportSteps.push(new String[]{Integer.toString(0)});
                holder.button_Next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String sData;
                        int iNextStep;
                        if(holder.radioButton_ImportComicsFromStorage.isChecked()){
                            sData = "";
                            iNextStep = STEP_1_1_Select_Folder;
                        } else {
                            sData = "";
                            iNextStep = STEP_2_1_EnterWebAddress;
                        }
                        stackImportSteps.push(new String[]{Integer.toString(iNextStep), sData});
                        Toast.makeText(getApplicationContext(), "Click detected", Toast.LENGTH_SHORT).show();
                        viewPager.setCurrentItem(iNextStep);
                    }
                });
            }

            if(position == STEP_1_1_Select_Folder){
                //Push step onto the step stack:
                stackImportSteps.push(new String[]{Integer.toString(STEP_1_1_Select_Folder)});

                holder.button_SelectFolder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Allow the user to choose a directory using the system's file picker.
                        Intent intent_GetImportFromFolder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                        // Provide write access to files and sub-directories in the user-selected directory:
                        intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        //Start the activity:
                        startActivityForResult(intent_GetImportFromFolder, REQUEST_CODE_GET_IMPORT_FOLDER);
                    }
                });


                holder.button_Next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int iNextStep;
                        iNextStep = STEP_1_2_Select_Folder_Items;
                        stackImportSteps.push(new String[]{Integer.toString(iNextStep), "test"});

                        notifyDataSetChanged(); //make sure we call to refresh the ListView on the next page.

                        viewPager.setCurrentItem(iNextStep);
                    }
                });



            }

            if(position == STEP_1_2_Select_Folder_Items){ //Configure views on the file selector page:
                //Push step onto the step stack:
                stackImportSteps.push(new String[]{Integer.toString(STEP_1_2_Select_Folder_Items)});

                //Set the contents of the ListView:
                TextView textView_Label_Selected_Folder = findViewById(R.id.textView_Selected_Import_Folder);
                String sFolder = textView_Label_Selected_Folder.getText().toString();
                if(!sFolder.equals("")) {
                    ArrayList<fileModel> alFileList = fnc_readFolderContent(sFolder, ".+", FILES_ONLY);
                    fileListCustomAdapter = new FileListCustomAdapter(getApplicationContext(), R.id.listView_FolderContents, alFileList);

                    if(holder.listView_FolderContents != null) {
                        holder.listView_FolderContents.setAdapter(fileListCustomAdapter);
                    }
                }

                //Configure the "Select All" checkbox:
                holder.checkBox_SelectAllStorageItems.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fileListCustomAdapter.toggleSelectAll();
                        holder.listView_FolderContents.setAdapter(fileListCustomAdapter);
                    }
                });

                //Configure the "Search" editText:
                holder.editText_Search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) { //When the user clicks the magnifying glass:
                            //Close the keyboard:
                            textView.clearFocus();
                            InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(holder.editText_Search.getWindowToken(), 0);
                            //Apply the search:
                            String sSearch = textView.getText().toString();
                            fileListCustomAdapter.applySearch(sSearch);
                            return true;
                        }
                        return false;
                    }
                });

                //Configure the "Sort by" selection Spinner:
                final int SPINNER_ITEM_FILE_NAME = 0;
                final int SPINNER_ITEM_MODIFIED_DATE = 1;
                String[] sSpinnerItems={"Filename","Modified Date"};
                Spinner spinner_SortBy = holder.spinner_SortBy;
                //wrap the items in the Adapter
                ArrayAdapter<String> adapter=new ArrayAdapter<>(getApplicationContext(), R.layout.activity_import_comics_spinner_item, sSpinnerItems);
                //assign adapter to the Spinner
                spinner_SortBy.setAdapter(adapter);

                spinner_SortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        // your code here
                        if(position == SPINNER_ITEM_FILE_NAME){
                            fileListCustomAdapter.SortByFileNameAsc();
                            fileListCustomAdapter.notifyDataSetChanged();
                        } else if(position == SPINNER_ITEM_MODIFIED_DATE) {
                            fileListCustomAdapter.SortByDateModifiedAsc();
                            fileListCustomAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                        // no code needed here
                    }

                });

                //Configure the sort order imageView to respond to click:
                holder.imageView_SortOrder.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View view) {
                        boolean bNewSortOrderIsAscending;
                        bNewSortOrderIsAscending = fileListCustomAdapter.reverseSort();
                        fileListCustomAdapter.notifyDataSetChanged();
                        if(bNewSortOrderIsAscending){
                            ((ImageView)view).setImageResource(R.drawable.baseline_sort_ascending_white_18dp);
                        } else {
                            ((ImageView)view).setImageResource(R.drawable.baseline_sort_descending_white_18dp);
                        }

                    }
                });


            } //End config/response of STEP_1_2_Select_Folder_Items



        }

        @Override
        public int getItemCount() {
            return ai.size();
        }


    } //End ViewPagerAdapter


    //======================================================================================
    //===== File ListView Adapter ==========================================================
    //======================================================================================

    //References:
    //  https://thecodeprogram.com/build-your-own-file-selector-screen-on-android

    public static class fileModel {
        final public String type; //folder or file
        final public String name;
        final public String path;
        final public long size;
        final public Date dateLastModified;
        public Boolean isChecked;

        public fileModel(String _type, String _name ,String _path, long _size, Date _dateLastModified, Boolean _isChecked)
        {
            this.type = _type;
            this.name = _name;
            this.path = _path;
            this.size = _size;
            this.dateLastModified = _dateLastModified;
            this.isChecked = _isChecked;
        }
    }

    int FOLDERS_ONLY = 0;
    int FILES_ONLY = 1;
    //int BOTH_FOLDERS_AND_FILES = 3;
    public ArrayList<fileModel> fnc_readFolderContent(String _path, String sFileExtensionRegEx, int iSelectFoldersFilesOrBoth) {

        ArrayList<fileModel> alFileList = new ArrayList<>();

        File directory = new File(_path);
        File[] files = directory.listFiles();

        if(files == null){
            return null;
        }

        //Gather attributes from files:
        for (File value : files) {
            String fileType = value.isDirectory() ? "folder" : "file";
            if ((iSelectFoldersFilesOrBoth == FILES_ONLY) && (value.isDirectory())) {
                continue; //skip the rest of the for loop for this item.
            } else if ((iSelectFoldersFilesOrBoth == FOLDERS_ONLY) && (!value.isDirectory())) {
                continue; //skip the rest of the for loop for this item.
            }
            String fileName = value.getName();
            String filePath = value.getPath();
            long fileSize = value.length();
            Date dateLastModified = new Date(value.lastModified());
            String fileExtention = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf(".")) : "";

            if (!fileExtention.matches(sFileExtensionRegEx)) {
                continue;  //skip the rest of the loop if the file extension does not match.
            }

            //create the file model and initialize:
            fileModel file = new fileModel(fileType, fileName, filePath, fileSize, dateLastModified, false);

            alFileList.add(file); // add the file models to the Arraylist to return for the outer function
        }

        return alFileList;
    }

    private static class FileListCustomAdapter extends ArrayAdapter<fileModel> {

        final private ArrayList<fileModel> alFileList;
        private ArrayList<fileModel> alFileListDisplay;
        private  boolean bSelectAllSelected = false;
        private  boolean bSearched = false;

        public FileListCustomAdapter(Context context, int textViewResourceId, ArrayList<fileModel> xmlList) {
            super(context, textViewResourceId, xmlList);
            this.alFileList = new ArrayList<fileModel>(xmlList);
            this.alFileListDisplay = new ArrayList<fileModel>(xmlList);
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
            bSearched = true;
        }

        public void removeSearch(){
            alFileListDisplay = alFileList;
            bSearched = false;
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
        };

        //Sort by file modified date ascending:
        private static class FileModifiedDateAscComparator implements Comparator<fileModel> {

            public int compare(fileModel fm1, fileModel fm2) {
                Date FileDate1 = fm1.dateLastModified;
                Date FileDate2 = fm2.dateLastModified;

                //ascending order
                return FileDate1.compareTo(FileDate2);
            }
        };

        //Sort by file modified date descending:
        private static class FileModifiedDateDescComparator implements Comparator<fileModel> {

            public int compare(fileModel fm1, fileModel fm2) {
                Date FileDate1 = fm1.dateLastModified;
                Date FileDate2 = fm2.dateLastModified;

                //descending order
                return FileDate2.compareTo(FileDate1);
            }
        };

        private int iCurrentSortMethod;
        private final int SORT_METHOD_FILENAME_ASC = 1;
        private final int SORT_METHOD_FILENAME_DESC = 2;
        private final int SORT_METHOD_MODIFIED_DATE_ASC = 3;
        private final int SORT_METHOD_MODIFIED_DATE_DESC = 4;
        public void SortByFileNameAsc(){
            Collections.sort(alFileListDisplay, new FileNameAscComparator());
            iCurrentSortMethod = SORT_METHOD_FILENAME_ASC;
        }
        public void SortByFileNameDesc(){
            Collections.sort(alFileListDisplay, new FileNameDescComparator());
            iCurrentSortMethod = SORT_METHOD_FILENAME_DESC;
        }
        public void SortByDateModifiedAsc(){
            Collections.sort(alFileListDisplay, new FileModifiedDateAscComparator());
            iCurrentSortMethod = SORT_METHOD_MODIFIED_DATE_ASC;
        }
        public void SortByDateModifiedDesc(){
            Collections.sort(alFileListDisplay, new FileModifiedDateDescComparator());
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







    public void selectImportFile(View v){
        //Make sure there is space for copying
        long lSize;
        lSize = globalClass.AvailableStorageSpace(this, 1);

        //150 MB to begin import operation. This is about the size of 250 pages.
        //It is not uncommon for a comic listing to be a set of issues totaling neat 250 pages.
        boolean bEnoughSize = false;

        if (lSize >= 1024) {
            //contains at least 1 MB.
            lSize /= 1024;
            //size now in MB units.
            if (lSize > 150){
                bEnoughSize = true;
            }
        }

        //A
        if (!bEnoughSize) {
            Toast.makeText(this, "Storage space too full to begin import.(need > 150 MB).", Toast.LENGTH_LONG).show();
            return;
        }


        RadioButton radioButton_ImportFolder;
        radioButton_ImportFolder = findViewById(R.id.radioButton_ImportFolder);
        if(radioButton_ImportFolder.isChecked()) {

            //A minimum space to get started with the import has been confirmed.
            //https://developer.android.com/training/data-storage/shared/documents-files
            // Allow the user to choose a directory using the system's file picker.
            Intent intent_GetImportFromFolder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

            // Provide write access to files and sub-directories in the user-selected directory:
            intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            //Start the activity:
            startActivityForResult(intent_GetImportFromFolder, REQUEST_CODE_GET_IMPORT_FOLDER);
        } else {
            // Allow the user to choose a backup zip file using the system's file picker.
            Intent intent_GetExportSaveAsFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            intent_GetExportSaveAsFile.addCategory(Intent.CATEGORY_OPENABLE);
            intent_GetExportSaveAsFile.setType("application/zip");

            //Start the activity:
            startActivityForResult(intent_GetExportSaveAsFile, REQUEST_CODE_GET_IMPORT_FILE);

        }

    }

    @Override
    //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        //https://developer.android.com/training/data-storage/shared/documents-files
        try {
            super.onActivityResult(requestCode, resultCode, resultData);


            //If this is an EXPORT operation, and the data is not NULL,
            // look for permissions before executing operations.

            if (((requestCode == REQUEST_CODE_GET_IMPORT_FOLDER && resultCode == Activity.RESULT_OK)
                    || (requestCode == REQUEST_CODE_GET_IMPORT_FILE && resultCode == Activity.RESULT_OK))
                    && (resultData != null)){
                //Check to make sure that we have read/write permission in the selected folder.
                //If we don't have permission, request it.
                if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) ||
                        (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED)) {

                    // Permission is not granted
                    // Should we show an explanation?
                    if ((ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                            (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE))) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        Toast.makeText(this, "Permission required for read/write operation.", Toast.LENGTH_LONG).show();
                    } else {
                        // No explanation needed; request the permission
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                    //} else {
                    // Permission has already been granted
                }


                //The above code checked for permission, and if not granted, requested it.
                //  Check one more time to see if the permission was granted:

                if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED)) {
                    //If we now have permission...
                    //The result data contains a URI for the document or directory that
                    //the user selected.

                    //Put the import Uri into the intent (this could represent a folder OR a file:

                    Uri uriTree = resultData.getData();
                    String path = GlobalClass.FileUtil.getFullPathFromTreeUri(uriTree,this);

                    assert uriTree != null;
                    assert path != null;
                    if(path.matches("/") && uriTree.toString().contains("providers.downloads")) {
                        //Downloads Provider selected.
                        //  Get the path of the Downloads folder:
                        Uri uriProvider = resultData.getData();
                        ContentResolver contentResolver = this.getContentResolver();
                        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriProvider, DocumentsContract.getTreeDocumentId(uriProvider));

                        // Keep track of our directory hierarchy
                        List<Uri> dirNodes = new LinkedList<>();
                        dirNodes.add(childrenUri);

                        childrenUri = dirNodes.remove(0); // get the item from top
                        String sSortOrder = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC"; //Sort does not appear to work.
                        Cursor cImport = contentResolver.query(childrenUri, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, sSortOrder);

                        if (cImport == null) {
                            return;
                        }

                        //int iItemCount = cImport.getCount();
                        while (cImport.moveToNext()) {
                            final String docId = cImport.getString(0);
                            if(docId.contains("Download")){
                                path = docId.substring(docId.indexOf("/"),docId.lastIndexOf("/"));
                                break;
                            }
                        }
                        cImport.close();

                    }

                    TextView textView_Selected_Import_Folder = findViewById(R.id.textView_Selected_Import_Folder);
                    textView_Selected_Import_Folder.setText(path);
                    TextView textView_Label_Selected_Folder = findViewById(R.id.textView_Label_Selected_Folder);
                    textView_Label_Selected_Folder.setVisibility(View.VISIBLE);
                    textView_Selected_Import_Folder.setVisibility(View.VISIBLE);


                }

            }
        } catch (Exception ex) {
            Context context = getApplicationContext();
            Toast.makeText(context, ex.toString(),
                    Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, ex.toString());
        }

    }



}