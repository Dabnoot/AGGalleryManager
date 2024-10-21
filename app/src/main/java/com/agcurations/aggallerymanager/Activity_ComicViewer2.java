package com.agcurations.aggallerymanager;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agcurations.aggallerymanager.databinding.ActivityComicViewer2Binding;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Activity_ComicViewer2 extends AppCompatActivity {

    public static final String EXTRA_COMIC_PAGE_START = "COMIC_PAGE_START";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        com.agcurations.aggallerymanager.databinding.ActivityComicViewer2Binding binding = ActivityComicViewer2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View mContentView = binding.relativeLayoutAll;
        if(mContentView.getWindowInsetsController() != null) {
            mContentView.getWindowInsetsController().hide(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        }

        // Initialize RecyclerView
        RecyclerView gRecyclerView_ComicViewer = findViewById(R.id.recyclerView_ComicPages);
        gRecyclerView_ComicViewer.setLayoutManager(new LinearLayoutManager(this));


        // Initialize data

        //Get the intent used to start this activity:
        Intent intentCaller = getIntent();
        //Get data from the intent:

        ItemClass_CatalogItem gciCatalogItem;
        gciCatalogItem = (ItemClass_CatalogItem) intentCaller.getSerializableExtra(GlobalClass.EXTRA_CATALOG_ITEM);

        int iStartPage = intentCaller.getIntExtra(Activity_ComicViewer2.EXTRA_COMIC_PAGE_START, 0);

        if( gciCatalogItem == null) return;

        String sComicFolderUri = GlobalClass.gsUriAppRootPrefix
                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[gciCatalogItem.iMediaCategory]
                + GlobalClass.gsFileSeparator + gciCatalogItem.sFolderRelativePath;

        //Load the full path to each comic page into tmComicPages:
        if (gciCatalogItem.iSpecialFlag == ItemClass_CatalogItem.FLAG_COMIC_DLM_MOVE) {
            //If this is a downloaded comic and the files from DownloadManager have not been moved as
            //  part of download post-processing, look in the [comic]\download folder for the files:
            sComicFolderUri = GlobalClass.gsUriAppRootPrefix
                    + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[gciCatalogItem.iMediaCategory]
                    + GlobalClass.gsFileSeparator + gciCatalogItem.sFolderRelativePath
                    + GlobalClass.gsFileSeparator + GlobalClass.gsDLTempFolderName;
        }



        Uri uriComicFolderUri = Uri.parse(sComicFolderUri);
        if(!GlobalClass.CheckIfFileExists(uriComicFolderUri)){
            Toast.makeText(getApplicationContext(), "Comic folder does not exist. Try deleting and re-import the comic.", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uriComicFilesChildUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriComicFolderUri,
                DocumentsContract.getDocumentId(uriComicFolderUri));
        Cursor cComicFiles = GlobalClass.gcrContentResolver.query(uriComicFilesChildUri,
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
                null);


        TreeMap<String, Uri> tmSortByFileName = new TreeMap<>();
        if(cComicFiles != null) {
            while(cComicFiles.moveToNext()){
                String sMimeType = cComicFiles.getString(2);
                if(sMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)){
                    continue; //Don't add any folders, if there might be one.
                }
                String sFileName = cComicFiles.getString(1);
                String sFileUri = sComicFolderUri
                        + GlobalClass.gsFileSeparator + sFileName;
                Uri uriFileUri = Uri.parse(sFileUri);
                tmSortByFileName.put(GlobalClass.JumbleFileName(sFileName), uriFileUri);
            }
            cComicFiles.close();
        }

        ArrayList<Uri> comicUris = new ArrayList<>();
        for(Map.Entry<String, Uri> entry: tmSortByFileName.entrySet()){
            comicUris.add(entry.getValue());
        }

        // Initialize adapter and set to RecyclerView
        ComicAdapter comicAdapter = new ComicAdapter(comicUris);
        gRecyclerView_ComicViewer.setAdapter(comicAdapter);

        if(iStartPage > 0) {
            //Navigate to a start page:
            RecyclerView.LayoutManager rvlm = gRecyclerView_ComicViewer.getLayoutManager();
            if(rvlm != null){
                rvlm.scrollToPosition(iStartPage);
            }
        }
    }


    public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {

        private final ArrayList<Uri> comicUris;

        public class ComicViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView_ComicPage;

            public ComicViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView_ComicPage = itemView.findViewById(R.id.imageView_ComicPage);
            }
        }

        public ComicAdapter(ArrayList<Uri> comicUris) {
            this.comicUris = comicUris;
        }

        @NonNull
        @Override
        public ComicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_comic_viewer2_page, parent, false);
            return new ComicViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ComicViewHolder holder, int position) {
            Uri comicUri = comicUris.get(position);

            Glide.with(getApplicationContext())
                    .load(comicUri)
                    .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                    .into(holder.imageView_ComicPage);
        }

        @Override
        public int getItemCount() {
            return comicUris.size();
        }


    }

}