package com.agcurations.aggallerymanager;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


public class Fragment_Import_2at_SelectDetectedThumbnail extends Fragment {

    public static ViewModel_ImportActivity viewModelImportActivity;

    public Fragment_Import_2at_SelectDetectedThumbnail() {
        // Required empty public constructor
    }

    public static Fragment_Import_2at_SelectDetectedThumbnail newInstance() {
        return new Fragment_Import_2at_SelectDetectedThumbnail();
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
        return inflater.inflate(R.layout.fragment_import_2at_select_detected_thumbnail, container, false);
    }

    Parcelable ListViewState;
    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() == null || getView() == null) {
            return;
        }

        if(ListViewState != null) {
            ListView listView_ThumbnailDownloadItems = getView().findViewById(R.id.listView_ThumbnailDownloadItems);
            listView_ThumbnailDownloadItems.onRestoreInstanceState(ListViewState);
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
        ListView listView_ThumbnailDownloadItems = getView().findViewById(R.id.listView_ThumbnailDownloadItems);
        ListViewState = listView_ThumbnailDownloadItems.onSaveInstanceState();

        super.onPause();

    }

    public void initComponents(){

        if(getView() == null){
            return;
        }
        //Set the contents of the ListView:
        final ListView listView_ThumbnailDownloadItems = getView().findViewById(R.id.listView_ThumbnailDownloadItems);
        if(listView_ThumbnailDownloadItems != null) {
            Activity_Import.SelectItemsListViewWidth = listView_ThumbnailDownloadItems.getWidth();
            if(getActivity()==null){
                return;
            }
            listView_ThumbnailDownloadItems.setAdapter(((Activity_Import) getActivity()).thumbnailListCustomAdapter);
        }

        if(getActivity() != null) {
            ((Activity_Import) getActivity()).thumbnailListCustomAdapter.recalcButtonNext();
        }

    }



}