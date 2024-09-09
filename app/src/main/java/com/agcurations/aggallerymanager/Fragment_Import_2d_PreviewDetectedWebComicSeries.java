package com.agcurations.aggallerymanager;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class Fragment_Import_2d_PreviewDetectedWebComicSeries extends Fragment {

    public static ViewModel_ImportActivity viewModelImportActivity;

    public Fragment_Import_2d_PreviewDetectedWebComicSeries() {
        // Required empty public constructor
    }

    public static Fragment_Import_2d_PreviewDetectedWebComicSeries newInstance() {
        return new Fragment_Import_2d_PreviewDetectedWebComicSeries();
    }

    RecyclerView gRecyclerView;

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
        return inflater.inflate(R.layout.fragment_import_2d_preview_detctd_web_comic_ser, container, false);
    }

    private Parcelable recyclerViewState;
    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() == null || getView() == null) {
            return;
        }

        getActivity().setTitle("Import - Review Detected Web Comic");
        if(((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }

        //Get the comic title and update on the layout:
        String sTitle = ((Activity_Import) getActivity()).recyclerViewComicPreviewAdapter.alFileItems.get(0).sTitle;
        TextView textView_ComicTitle = getView().findViewById(R.id.textView_ComicTitle);
        textView_ComicTitle.setText(sTitle);

        gRecyclerView = getView().findViewById(R.id.recyclerView_WebComicPagePreview);

        //Attempt to restore the state, ie scroll position, of the recyclerView:
        if(gRecyclerView.getLayoutManager() != null) {
            gRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);//restore
        }

        if(recyclerViewState == null || viewModelImportActivity.bUpdateImportSelectList){
            initComponents();
        }

    }


    @Override
    public void onPause() {
        if(getView() == null){
            return;
        }

        //Attempt to save the state, ie scroll position, of the recyclerView:
        if(gRecyclerView.getLayoutManager() != null) {
            recyclerViewState = gRecyclerView.getLayoutManager().onSaveInstanceState();
        }

        super.onPause();

    }

    public void initComponents(){
        configure_RecyclerViewCatalogItems();

        viewModelImportActivity.bUpdateImportSelectList = false;
        if(getView() == null){
            return;
        }

        Activity_Import activityImport = (Activity_Import) getActivity();
        if(activityImport != null) {
            gRecyclerView.setAdapter(activityImport.recyclerViewComicPreviewAdapter);
            activityImport.recyclerViewComicPreviewAdapter.notifyDataSetChanged();
        }
        //todo: above not needed?

        //((Activity_Import) getActivity()).recyclerViewCatalogAdapter.recalcButtonNext();

    }

    public void configure_RecyclerViewCatalogItems(){

        // use this setting to
        // improve performance if you know that changes
        // in content do not change the layout size
        // of the RecyclerView
        gRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager;
        GridLayoutManager gridLayoutManager;

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape
            // use a grid layout manager
            gridLayoutManager = new GridLayoutManager(getContext(), 4, RecyclerView.VERTICAL, false);
            gRecyclerView.setLayoutManager(gridLayoutManager);
            gRecyclerView.setItemViewCacheSize(0); //Had problems with resizing the grid imageview height to minimize dead space between grid items.
            // First round of recycles, items 5 and 6, would retain the original sizing. Believed to be due to cache size of 2.
        } else {
            // In portrait
            // use a linear layout manager
            layoutManager = new LinearLayoutManager(getContext());
            gRecyclerView.setLayoutManager(layoutManager);
        }

    }




}