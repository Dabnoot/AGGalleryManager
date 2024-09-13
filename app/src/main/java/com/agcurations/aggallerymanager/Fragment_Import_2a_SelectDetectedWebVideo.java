package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


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
        return inflater.inflate(R.layout.fragment_import_2a_select_detected_web_item, container, false);
    }

    Parcelable ListViewState;
    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() == null || getView() == null) {
            return;
        }

        if(ListViewState != null) {
            ListView listView_VideoDownloadItems = getView().findViewById(R.id.listView_WebDownloadItems);
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
        ListView listView_VideoDownloadItems = getView().findViewById(R.id.listView_WebDownloadItems);
        ListViewState = listView_VideoDownloadItems.onSaveInstanceState();

        super.onPause();

    }

    public void initComponents(){
        viewModelImportActivity.bUpdateImportSelectList = false;
        if(getView() == null){
            return;
        }
        //Set the contents of the ListView:
        final ListView listView_WebDownloadItems = getView().findViewById(R.id.listView_WebDownloadItems);
        if(listView_WebDownloadItems != null) {
            Activity_Import.SelectItemsListViewWidth = listView_WebDownloadItems.getWidth();
            if(getActivity()==null){
                return;
            }
            listView_WebDownloadItems.setAdapter(((Activity_Import) getActivity()).videoDownloadListCustomAdapter);
        }

        if(getActivity() != null) {
            ((Activity_Import) getActivity()).videoDownloadListCustomAdapter.recalcButtonNext();
        }

    }



}