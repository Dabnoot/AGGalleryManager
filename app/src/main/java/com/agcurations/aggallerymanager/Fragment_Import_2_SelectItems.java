package com.agcurations.aggallerymanager;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_Import_2_SelectItems#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_Import_2_SelectItems extends Fragment {

    public static ViewModel_ImportActivity viewModelImportActivity; //Used to transfer data between fragments.


    public Fragment_Import_2_SelectItems() {
        // Required empty public constructor
    }

    public static Fragment_Import_2_SelectItems newInstance() {
        return new Fragment_Import_2_SelectItems();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Instantiate the ViewModel sharing data between fragments:
        if(getActivity() != null) {
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_2_select_items, container, false);

    }


    Parcelable ListViewState;
    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() != null) {
            getActivity().setTitle("Import - Select Items");
        }

        if(ListViewState != null) {
            if(getView() == null){
                return;
            }
            ListView listView_FolderContents = getView().findViewById(R.id.listView_FolderContents);
            listView_FolderContents.onRestoreInstanceState(ListViewState);
        }

        if(ListViewState == null || viewModelImportActivity.bUpdateImportSelectList){
            initComponents();
        }

    }

    @Override
    public void onPause() {
        if(getView() == null){
            return;
        }
        ListView listView_FolderContents = getView().findViewById(R.id.listView_FolderContents);
        ListViewState = listView_FolderContents.onSaveInstanceState();

        //Get all of the selected items and assign them to the viewModel:
        //  Sort the items before assigning them to the viewModel:
        TreeMap<String, ItemClass_File> tmFileItemsSort = new TreeMap<>();

        if(getActivity() != null) {
            for (ItemClass_File fi : ((Activity_Import) getActivity()).fileListCustomAdapter.alFileItems) {
                if (fi.bIsChecked) {
                    tmFileItemsSort.put(fi.sUri, fi); //Sort by Uri rather than file name because comic folders might contain the same file names.

                }
            }
            ArrayList<ItemClass_File> alfi = new ArrayList<>();
            for (Map.Entry<String, ItemClass_File> tmEntry : tmFileItemsSort.entrySet()) {
                if (tmEntry.getValue().bIsChecked) {
                    alfi.add(tmEntry.getValue());
                }
            }
            viewModelImportActivity.alfiConfirmedFileImports = alfi;
        }
        super.onPause();


    }

    public void initComponents(){
        viewModelImportActivity.bUpdateImportSelectList = false;
        if(getView() == null){
            return;
        }

        /*Long lStartTime = 0L;
        Long lEndTime = 0L;

        lStartTime = System.nanoTime();*/
        //Set the contents of the ListView:
        final ListView listView_FolderContents = getView().findViewById(R.id.listView_FolderContents);
        if(listView_FolderContents != null) {
            Activity_Import.SelectItemsListViewWidth = listView_FolderContents.getWidth();
            if(getActivity()==null){
                return;
            }
            listView_FolderContents.setAdapter(((Activity_Import) getActivity()).fileListCustomAdapter);
        }

        /*lEndTime = System.nanoTime();
        Log.d("********Time used to read folder","*******Time used to assign adapter: " + TimeUnit.MILLISECONDS.convert(lEndTime - lStartTime, TimeUnit.NANOSECONDS));
        lStartTime = System.nanoTime();*/

        //Configure the "Select All" checkbox:
        CheckBox checkBox_SelectAllStorageItems = getView().findViewById(R.id.checkBox_SelectAllStorageItems);
        checkBox_SelectAllStorageItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getActivity()==null){
                    return;
                }
                ((Activity_Import) getActivity()).fileListCustomAdapter.toggleSelectAll();
                if(listView_FolderContents != null) {
                    listView_FolderContents.setAdapter(((Activity_Import) getActivity()).fileListCustomAdapter);
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
                    ((Activity_Import) getActivity()).fileListCustomAdapter.applySearch(sSearch);
                    return true;
                }
                return false;
            }
        });

        //Configure the "Sort by" selection Spinner:
        final int SPINNER_ITEM_FILE_NAME = 0;
        final int SPINNER_ITEM_MODIFIED_DATE = 1;
        final int SPINNER_ITEM_DURATION = 2;
        String[] sSpinnerItems;
        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS){
            sSpinnerItems = new String[]{"Filename","Modified Date", "Duration"};
        } else {
            sSpinnerItems = new String[]{"Filename","Modified Date"};
        }
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

                if(getActivity()==null){
                    return;
                }
                if(position == SPINNER_ITEM_FILE_NAME){
                    if(((Activity_Import) getActivity()).fileListCustomAdapter != null) {
                        ((Activity_Import) getActivity()).fileListCustomAdapter.SortByFileNameAsc();
                        ((Activity_Import) getActivity()).fileListCustomAdapter.notifyDataSetChanged();
                    }
                } else if(position == SPINNER_ITEM_MODIFIED_DATE) {
                    if(((Activity_Import) getActivity()).fileListCustomAdapter != null) {
                        ((Activity_Import) getActivity()).fileListCustomAdapter.SortByDateModifiedAsc();
                        ((Activity_Import) getActivity()).fileListCustomAdapter.notifyDataSetChanged();
                    }
                } else if(position == SPINNER_ITEM_DURATION) {
                    if(((Activity_Import) getActivity()).fileListCustomAdapter != null) {
                        ((Activity_Import) getActivity()).fileListCustomAdapter.SortByDurationAsc();
                        ((Activity_Import) getActivity()).fileListCustomAdapter.notifyDataSetChanged();
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
                if(getActivity()==null){
                    return;
                }
                boolean bNewSortOrderIsAscending;
                bNewSortOrderIsAscending = ((Activity_Import) getActivity()).fileListCustomAdapter.reverseSort();
                ((Activity_Import) getActivity()).fileListCustomAdapter.notifyDataSetChanged();
                if(bNewSortOrderIsAscending){
                    ((ImageView)view).setImageResource(R.drawable.baseline_sort_ascending_white_18dp);
                } else {
                    ((ImageView)view).setImageResource(R.drawable.baseline_sort_descending_white_18dp);
                }

            }
        });
        /*lEndTime = System.nanoTime();
        Log.d("********Time used to read folder","*******Time used to do everything else: " + TimeUnit.MILLISECONDS.convert(lEndTime - lStartTime, TimeUnit.NANOSECONDS));*/

        ((Activity_Import) getActivity()).fileListCustomAdapter.recalcButtonNext();

    }





}