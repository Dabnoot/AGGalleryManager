package com.agcurations.aggallerymanager;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
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


public class Fragment_Import_2a_SelectDetectedWebVideo extends Fragment {

    public static ViewModel_ImportActivity viewModelImportActivity;

    public Fragment_Import_2a_SelectDetectedWebVideo() {
        // Required empty public constructor
    }

    public static Fragment_Import_2a_SelectDetectedWebVideo newInstance() {
        return new Fragment_Import_2a_SelectDetectedWebVideo();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_2a_select_detected_web_video, container, false);
    }

    Parcelable ListViewState;
    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() == null || getView() == null) {
            return;
        }

        getActivity().setTitle("Import - Select Download Item");
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();

        if(ListViewState != null) {
            ListView listView_VideoDownloadItems = getView().findViewById(R.id.listView_VideoDownloadItems);
            listView_VideoDownloadItems.onRestoreInstanceState(ListViewState);
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
        ListView listView_VideoDownloadItems = getView().findViewById(R.id.listView_VideoDownloadItems);
        ListViewState = listView_VideoDownloadItems.onSaveInstanceState();

        //Get any single selected item and assign it to the viewModel:
        if(getActivity() != null) {
            ArrayList<ItemClass_File> alfi = new ArrayList<>();
            for (ItemClass_File fi : ((Activity_Import) getActivity()).videoDownloadListCustomAdapter.alFileItems) {
                if (fi.bIsChecked) {
                    alfi.add(fi);
                    break;
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
        //Set the contents of the ListView:
        final ListView listView_VideoDownloadItems = getView().findViewById(R.id.listView_VideoDownloadItems);
        if(listView_VideoDownloadItems != null) {
            Activity_Import.SelectItemsListViewWidth = listView_VideoDownloadItems.getWidth();
            if(getActivity()==null){
                return;
            }
            listView_VideoDownloadItems.setAdapter(((Activity_Import) getActivity()).videoDownloadListCustomAdapter);
        }

        ((Activity_Import) getActivity()).videoDownloadListCustomAdapter.recalcButtonNext();

    }



}