package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Parcelable;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Fragment_Import_2_SelectItems extends Fragment {

    public static ViewModel_ImportActivity viewModelImportActivity; //Used to transfer data between fragments.

    ProgressBar gProgressBar_FileDeletionProgress;
    TextView gTextView_FileDeletionProgressBarText;
    TextView gtextView_FileDeletionDebugLog;

    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;

    public Fragment_Import_2_SelectItems() {
        // Required empty public constructor
    }

    public static Fragment_Import_2_SelectItems newInstance() {
        return new Fragment_Import_2_SelectItems();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);

        }

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_SELECT_ITEMS_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();
        //requireActivity().registerReceiver(importDataServiceResponseReceiver, filter);
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(importDataServiceResponseReceiver, filter);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_2_select_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getView() != null) {
            gProgressBar_FileDeletionProgress = getView().findViewById(R.id.progressBar_FileDeletionProgress);
            gProgressBar_FileDeletionProgress.setMax(100);
            gTextView_FileDeletionProgressBarText = getView().findViewById(R.id.textView_FileDeletionProgressBarText);

            gtextView_FileDeletionDebugLog = getView().findViewById(R.id.textView_FileDeletionDebugLog);
            if (gtextView_FileDeletionDebugLog != null) {
                gtextView_FileDeletionDebugLog.setMovementMethod(new ScrollingMovementMethod());
            }
        }

    }

    @Override
    public void onDestroy() {
        // unregister  like this
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(importDataServiceResponseReceiver);
        }
        super.onDestroy();
    }

    Parcelable ListViewState;
    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() == null || getView() == null) {
            return;
        }

        if(ListViewState != null) {
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
            ArrayList<ItemClass_File> alFileItems = ((Activity_Import) getActivity()).fileListCustomAdapter.alFileItems;
            for (ItemClass_File fi : alFileItems) {
                if (fi.bIsChecked || fi.bMarkedForDeletion){// && (viewModelImportActivity.iImportMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS))) {
                    tmFileItemsSort.put(fi.sUri, fi); //Sort by Uri rather than file name because comic folders might contain the same file names.

                }
            }
            ArrayList<ItemClass_File> alfi = new ArrayList<>();
            for (Map.Entry<String, ItemClass_File> tmEntry : tmFileItemsSort.entrySet()) {
                if (tmEntry.getValue().bIsChecked || tmEntry.getValue().bMarkedForDeletion){// && (viewModelImportActivity.iImportMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS))) {
                    alfi.add(tmEntry.getValue());
                }
            }
            viewModelImportActivity.alfiConfirmedFileImports = alfi;
        }
        super.onPause();


    }

    public void initComponents(){
        viewModelImportActivity.bUpdateImportSelectList = false;
        if(getActivity() == null || getView() == null){
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

        int iFileCount = ((Activity_Import) getActivity()).fileListCustomAdapter.getCount();
        String sCheckBoxText = "Select All " + iFileCount + " Items";
        checkBox_SelectAllStorageItems.setText(sCheckBoxText);
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

        //Configure the "Sort by" selection menu:
        ArrayList<String> alsSortBy = new ArrayList<>();
        int iNextPosition = 0;
        final int MENU_ITEM_FILE_NAME = iNextPosition;
        alsSortBy.add("Filename");

        iNextPosition++;
        final int MENU_ITEM_MODIFIED_DATE = iNextPosition;
        alsSortBy.add("Modified Date");

        iNextPosition++;
        final int MENU_ITEM_RESOLUTION = iNextPosition;
        alsSortBy.add("Resolution");

        int MENU_ITEM_DURATION_TEMP = -1; //Special treatment - "duration" is not applicable to images or comics.
        if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
            iNextPosition++;
            MENU_ITEM_DURATION_TEMP = iNextPosition;
            alsSortBy.add("Duration");
        }
        final int MENU_ITEM_DURATION = MENU_ITEM_DURATION_TEMP;

        int MENU_ITEM_ORPHAN_DUPLICATED_TEMP = -2; //Special treatment - "orphan duplicates" is only applicable to Catalog Analysis behaviors.
        if(viewModelImportActivity.bImportingOrphanedFiles) {
            iNextPosition++;
            MENU_ITEM_ORPHAN_DUPLICATED_TEMP = iNextPosition;
            alsSortBy.add("Orphaned Duplicate");
        }
        final int MENU_ITEM_ORPHAN_DUPLICATED = MENU_ITEM_ORPHAN_DUPLICATED_TEMP;

        String[] sSortByStringArray = new String[alsSortBy.size()];
        sSortByStringArray = alsSortBy.toArray(sSortByStringArray);

        AutoCompleteTextView autoCompleteTextView_SortBy = getView().findViewById(R.id.autoCompleteTextView_SortBy);
        ArrayAdapter<String> aasSortByAdapter=new ArrayAdapter<>(getActivity().getApplicationContext(), R.layout.dropdown_item_generic, sSortByStringArray);
        autoCompleteTextView_SortBy.setAdapter(aasSortByAdapter);

        autoCompleteTextView_SortBy.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            //Note: autoCompleteTextView_SortBy.setOnItemSelectedListener did not appear to fire in testing.
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int iPosition, long l) {
                if(getActivity()==null){
                    return;
                }
                if(iPosition == MENU_ITEM_FILE_NAME){
                    if(((Activity_Import) getActivity()).fileListCustomAdapter != null) {
                        ((Activity_Import) getActivity()).fileListCustomAdapter.SortByFileNameAsc();
                        ((Activity_Import) getActivity()).fileListCustomAdapter.notifyDataSetChanged();
                    }
                } else if(iPosition == MENU_ITEM_MODIFIED_DATE) {
                    if(((Activity_Import) getActivity()).fileListCustomAdapter != null) {
                        ((Activity_Import) getActivity()).fileListCustomAdapter.SortByDateModifiedAsc();
                        ((Activity_Import) getActivity()).fileListCustomAdapter.notifyDataSetChanged();
                    }
                } else if(iPosition == MENU_ITEM_RESOLUTION) {
                    if(((Activity_Import) getActivity()).fileListCustomAdapter != null) {
                        ((Activity_Import) getActivity()).fileListCustomAdapter.SortByResolutionAsc();
                        ((Activity_Import) getActivity()).fileListCustomAdapter.notifyDataSetChanged();
                    }
                } else if(iPosition == MENU_ITEM_DURATION) {
                    if(((Activity_Import) getActivity()).fileListCustomAdapter != null) {
                        ((Activity_Import) getActivity()).fileListCustomAdapter.SortByDurationAsc();
                        ((Activity_Import) getActivity()).fileListCustomAdapter.notifyDataSetChanged();
                    }
                }
                else if(iPosition == MENU_ITEM_ORPHAN_DUPLICATED) {
                    if(((Activity_Import) getActivity()).fileListCustomAdapter != null) {
                        ((Activity_Import) getActivity()).fileListCustomAdapter.SortByOrphanDuplicateAsc();
                        ((Activity_Import) getActivity()).fileListCustomAdapter.notifyDataSetChanged();
                    }
                }
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

        if(getActivity() != null) {
            if(((Activity_Import) getActivity()).fileListCustomAdapter != null){
                ((Activity_Import) getActivity()).fileListCustomAdapter.recalcButtonNext();
            }
        }

    }

    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String IMPORT_DATA_SERVICE_SELECT_ITEMS_RESPONSE = "com.agcurations.aggallerymanager.intent.action.IMPORT_DATA_SERVICE_SELECT_ITEMS_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                if(getView() != null) {
                    if (gtextView_FileDeletionDebugLog != null) {
                        gtextView_FileDeletionDebugLog.setVisibility(View.VISIBLE);
                        gtextView_FileDeletionDebugLog.append(sMessage);
                    } else {
                        Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
                    }
                }
            } else {

                //Check to see if this is a response to request to get directory contents:
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_FileDeletionProgressBarText != null) {
                        gTextView_FileDeletionProgressBarText.setText(sProgressBarText);
                        gTextView_FileDeletionProgressBarText.setVisibility(View.VISIBLE);
                    }
                }

                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_FileDeletionProgress != null) {
                        gProgressBar_FileDeletionProgress.setProgress(iAmountComplete);
                        gProgressBar_FileDeletionProgress.setVisibility(View.VISIBLE);

                    }
                    if(iAmountComplete == 100){
                        if(gProgressBar_FileDeletionProgress != null) {

                            gProgressBar_FileDeletionProgress.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    gProgressBar_FileDeletionProgress.setVisibility(View.INVISIBLE);
                                    if (gTextView_FileDeletionProgressBarText != null) {
                                        gTextView_FileDeletionProgressBarText.setVisibility(View.INVISIBLE);
                                    }
                                }
                            }, 3000);
                        }
                    }
                }


            }

        }
    }



}