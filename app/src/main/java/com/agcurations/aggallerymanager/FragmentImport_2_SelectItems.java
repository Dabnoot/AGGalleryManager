package com.agcurations.aggallerymanager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

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
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentImport_2_SelectItems#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentImport_2_SelectItems extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public FragmentImport_2_SelectItems() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentImport_2_SelectItems.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentImport_2_SelectItems newInstance(String param1, String param2) {
        FragmentImport_2_SelectItems fragment = new FragmentImport_2_SelectItems();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_2_select_items, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
/*        if(ImportActivity.ViewPager2_Import.getCurrentItem() != ImportActivity.FRAGMENT_IMPORT_2_INDEX_SELECT_ITEMS){
            //There will be a problem
            return;
        }*/
    }

    @Override
    public void onResume() {
        super.onResume();
        initComponents();
    }

    public void initComponents(){

        if(getView() == null){
            return;
        }

        //Long lStartTime = 0L;
        //Long lEndTime = 0L;

        //Set the contents of the ListView:
        //TextView textView_Label_Selected_Folder = findViewById(R.id.textView_Selected_Import_Folder);
        //String sFolder = textView_Label_Selected_Folder.getText().toString();
        //if(!sFolder.equals("")) {
        final ListView listView_FolderContents = getView().findViewById(R.id.listView_FolderContents);
        if(ImportActivity.guriImportTreeURI != null){
            //ArrayList<fileModel> alFileList = readFolderContent(sFolder, ".+", FILES_ONLY);
            //lStartTime = System.nanoTime();
            ArrayList<ImportActivity.fileModel> alFileList = readFolderContent(ImportActivity.guriImportTreeURI, ".+", FILES_ONLY);
            //lEndTime = System.nanoTime();
            //Log.d("********Time used to read folder","*******Time used to read folder: " + TimeUnit.MILLISECONDS.convert(lEndTime - lStartTime, TimeUnit.NANOSECONDS));
            //lStartTime = System.nanoTime();
            if(getActivity() == null){
                return;
            }
            ImportActivity.fileListCustomAdapter = new ImportActivity.FileListCustomAdapter(getActivity().getApplicationContext(), R.id.listView_FolderContents, alFileList);

            if(listView_FolderContents != null) {
                listView_FolderContents.setAdapter(ImportActivity.fileListCustomAdapter);
            }
        }

        //Configure the "Select All" checkbox:
        CheckBox checkBox_SelectAllStorageItems = getView().findViewById(R.id.checkBox_SelectAllStorageItems);
        checkBox_SelectAllStorageItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImportActivity.fileListCustomAdapter.toggleSelectAll();
                if(listView_FolderContents != null) {
                    listView_FolderContents.setAdapter(ImportActivity.fileListCustomAdapter);
                }
            }
        });

        //Configure the "Search" editText:
        final EditText editText_Search = getView().findViewById(R.id.editText_Search);
        editText_Search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) { //When the user clicks the magnifying glass:
                    //Close the keyboard:
                    textView.clearFocus();
                    if(getActivity() == null){
                        return false;
                    }
                    InputMethodManager imm =  (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText_Search.getWindowToken(), 0);
                    //Apply the search:
                    String sSearch = textView.getText().toString();
                    ImportActivity.fileListCustomAdapter.applySearch(sSearch);
                    return true;
                }
                return false;
            }
        });

        //Configure the "Sort by" selection Spinner:
        final int SPINNER_ITEM_FILE_NAME = 0;
        final int SPINNER_ITEM_MODIFIED_DATE = 1;
        String[] sSpinnerItems={"Filename","Modified Date"};
        Spinner spinner_SortBy = getView().findViewById(R.id.spinner_SortBy);
        //wrap the items in the Adapter
        if(getActivity() == null){
            return;
        }
        ArrayAdapter<String> adapter=new ArrayAdapter<>(getActivity().getApplicationContext(), R.layout.activity_import_comics_spinner_item, sSpinnerItems);
        //assign adapter to the Spinner
        spinner_SortBy.setAdapter(adapter);

        spinner_SortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                if(position == SPINNER_ITEM_FILE_NAME){
                    if(ImportActivity.fileListCustomAdapter != null) {
                        ImportActivity.fileListCustomAdapter.SortByFileNameAsc();
                        ImportActivity.fileListCustomAdapter.notifyDataSetChanged();
                    }
                } else if(position == SPINNER_ITEM_MODIFIED_DATE) {
                    if(ImportActivity.fileListCustomAdapter != null) {
                        ImportActivity.fileListCustomAdapter.SortByDateModifiedAsc();
                        ImportActivity.fileListCustomAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // no code needed here
            }

        });

        //Configure the sort order imageView to respond to click:
        ImageView imageView_SortOrder = getView().findViewById(R.id.imageView_SortOrder);
        imageView_SortOrder.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                boolean bNewSortOrderIsAscending;
                bNewSortOrderIsAscending = ImportActivity.fileListCustomAdapter.reverseSort();
                ImportActivity.fileListCustomAdapter.notifyDataSetChanged();
                if(bNewSortOrderIsAscending){
                    ((ImageView)view).setImageResource(R.drawable.baseline_sort_ascending_white_18dp);
                } else {
                    ((ImageView)view).setImageResource(R.drawable.baseline_sort_descending_white_18dp);
                }

            }
        });
        //lEndTime = System.nanoTime();
        //Log.d("********Time used to read folder","*******Time used to do everything else: " + TimeUnit.MILLISECONDS.convert(lEndTime - lStartTime, TimeUnit.NANOSECONDS));
    }

    int FOLDERS_ONLY = 0;
    int FILES_ONLY = 1;
    //int BOTH_FOLDERS_AND_FILES = 3;
    public ArrayList<ImportActivity.fileModel> readFolderContent(String _path, String sFileExtensionRegEx, int iSelectFoldersFilesOrBoth) {

        ArrayList<ImportActivity.fileModel> alFileList = new ArrayList<>();

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
            long fileSize = value.length() / 1024; //size in kB.
            Date dateLastModified = new Date(value.lastModified());
            String fileExtention = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf(".")) : "";

            if (!fileExtention.matches(sFileExtensionRegEx)) {
                continue;  //skip the rest of the loop if the file extension does not match.
            }

            //create the file model and initialize:
            ImportActivity.fileModel file = new ImportActivity.fileModel(fileType, fileName, filePath, fileSize, dateLastModified, false);

            alFileList.add(file); // add the file models to the ArrayList.
        }

        return alFileList;
    }

    //Use a Uri instead of a path to get folder contents:
    public ArrayList<ImportActivity.fileModel> readFolderContent(Uri uriFolder, String sFileExtensionRegEx, int iSelectFoldersFilesOrBoth) {

        //Get data about the files from the UriTree:
        if(getActivity() == null){
            return null;
        }
        ContentResolver contentResolver = getActivity().getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriFolder, DocumentsContract.getTreeDocumentId(uriFolder));
        List<Uri> dirNodes = new LinkedList<>();
        dirNodes.add(childrenUri);
        childrenUri = dirNodes.remove(0); // get the item from top
        String sSortOrder = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC"; //Sort does not appear to work.
        Cursor cImport = contentResolver.query(childrenUri,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        DocumentsContract.Document.COLUMN_SIZE,
                        DocumentsContract.Document.COLUMN_SUMMARY,
                        DocumentsContract.Document.COLUMN_FLAGS,
                        DocumentsContract.Document.COLUMN_ICON},
                null,
                null,
                sSortOrder);
        if(cImport == null){
            return null;
        }

        ArrayList<ImportActivity.fileModel> alFileList = new ArrayList<>();

        while (cImport.moveToNext()) {
            final String docId = cImport.getString(0);
            final String docName = cImport.getString(1);
            final String mime = cImport.getString(2);
            final long lLastModified = cImport.getLong(3); //milliseconds since January 1, 1970 00:00:00.0 UTC.
            final String sFileSize = cImport.getString(4);

            boolean isDirectory;
            isDirectory = (mime.equals(DocumentsContract.Document.MIME_TYPE_DIR));
            String fileType = (isDirectory) ? "folder" : "file";

            if ((iSelectFoldersFilesOrBoth == FILES_ONLY) && (isDirectory)) {
                continue; //skip the rest of the for loop for this item.
            } else if ((iSelectFoldersFilesOrBoth == FOLDERS_ONLY) && (!isDirectory)) {
                continue; //skip the rest of the for loop for this item.
            }

            //Get a Uri for this individual document:
            final Uri docUri = DocumentsContract.buildDocumentUriUsingTree(childrenUri,docId);

            long lFileSize = Long.parseLong(sFileSize) / 1024; //size in kB


            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(lLastModified);
            Date dateLastModified = cal.getTime();

            String fileExtension = docName.contains(".") ? docName.substring(docName.lastIndexOf(".")) : "";

            if (!fileExtension.matches(sFileExtensionRegEx)) {
                continue;  //skip the rest of the loop if the file extension does not match.
            }

            //create the file model and initialize:
            ImportActivity.fileModel file = new ImportActivity.fileModel(fileType, docName, docUri, lFileSize, dateLastModified, false);

            alFileList.add(file); // add the file models to the ArrayList.

        }
        cImport.close();

        return alFileList;
    }



}