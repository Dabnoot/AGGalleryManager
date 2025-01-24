package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.provider.DocumentsContract;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.TreeMap;

public class Activity_ComicDetails extends AppCompatActivity {
    //Global Variables:
    private GlobalClass globalClass;

    private String gsComicItemID = "";
    private ItemClass_CatalogItem gciCatalogItem;
    private TreeMap<Integer, Uri> gtmComicPages;

    private Activity_ComicDetails.ComicDetailsResponseReceiver gComicDetailsResponseReceiver;

    private RecyclerViewComicPagesAdapter gRecyclerViewComicPagesAdapter;

    private final boolean gbDebugTouch = false;

    TextView gtextView_ComicDetailsLog;

    private boolean gbComicFilesNotFound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_details);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        Intent intent = getIntent();

        //Get the item ID:
        gsComicItemID = intent.getStringExtra(GlobalClass.EXTRA_CATALOG_ITEM_ID);

        if( gsComicItemID == null) return;

        gtextView_ComicDetailsLog = findViewById(R.id.textView_ComicDetailsLog);
        gtextView_ComicDetailsLog.setMovementMethod(new ScrollingMovementMethod());

        loadComicPageData();


        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_DeleteItem.CATALOG_DELETE_ITEM_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        gComicDetailsResponseReceiver = new Activity_ComicDetails.ComicDetailsResponseReceiver();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(gComicDetailsResponseReceiver, filter);

        //See additional initialization in onCreateOptionsMenu().
    }

    private void loadComicPageData(){


        //Look-up the item and grab a copy:
        if (!gsComicItemID.equals("")) {
            gciCatalogItem = GlobalClass.gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).get(gsComicItemID);
        }
        if(gciCatalogItem == null){
            Toast.makeText(getApplicationContext(), "Could not find comic in catalog.", Toast.LENGTH_SHORT).show();
            return;
        }

        String sComicFolderUri = GlobalClass.gsUriAppRootPrefix
                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[gciCatalogItem.iMediaCategory]
                + GlobalClass.gsFileSeparator + gciCatalogItem.sFolderRelativePath;

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

        TreeMap<String, String> tmSortByFileName = new TreeMap<>();
        if(cComicFiles != null) {
            while(cComicFiles.moveToNext()){
                String sMimeType = cComicFiles.getString(2);
                if(sMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)){
                    continue; //Don't add any folders, if there might be one.
                }
                String sFileName = cComicFiles.getString(1);
                tmSortByFileName.put(GlobalClass.JumbleFileName(sFileName), sFileName);
            }
            cComicFiles.close();
        }

        //Load the full path to each comic page into tmComicPages:
        if (tmSortByFileName.size() == 0) {
            gbComicFilesNotFound = true;
            tmSortByFileName.put("empty", "empty");
        }

        gtextView_ComicDetailsLog.setVisibility(View.INVISIBLE);
        gtextView_ComicDetailsLog.setText("");
        gciCatalogItem.iFile_Count = tmSortByFileName.size(); //update the comic file count. Files may have been downloaded, deleted, etc.

        if (!gciCatalogItem.sComic_Missing_Pages.equals("")) {
            String sMissingPages = gciCatalogItem.sComic_Missing_Pages;
            //Check to see if this comic is missing any pages:
            gciCatalogItem = globalClass.analyzeComicReportMissingPages(gciCatalogItem);
            if (!sMissingPages.equals(gciCatalogItem.sComic_Missing_Pages)) {
                //Update the catalog file with the new record of missing pages:
                globalClass.CatalogDataFile_UpdateRecord(gciCatalogItem);
            }
            GlobalClass.gbCatalogViewerRefresh = true;
        }



        gtmComicPages = new TreeMap<>();
        int i = 0;
        for (Map.Entry<String, String> tmFiles : tmSortByFileName.entrySet()) {
            String sFileUri = sComicFolderUri
                    + GlobalClass.gsFileSeparator + tmFiles.getValue();
            Uri uriFileUri = Uri.parse(sFileUri);
            if(i == 0 && !gbComicFilesNotFound){
                //Here we add the first file twice so that it shows up in the header entry and in the
                //  later, smaller recylerview grid. But not if there are no files found.
                gtmComicPages.put(i, uriFileUri);
                i++;
            }
            gtmComicPages.put(i, uriFileUri);
            i++;
        }


        populate_RecyclerViewComicPages();





    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comic_details_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_DeleteComic) {
            DeleteComicPrompt();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void DeleteComicPrompt(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustomStyle);
        builder.setTitle("Delete Comic");
        builder.setMessage("Are you sure you want to delete this comic?");
        //builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                DeleteComic();

            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    public void DeleteComic(){

        GlobalClass.gbCatalogViewerRefresh = true;
        Toast.makeText(getApplicationContext(), "Deleting comic...", Toast.LENGTH_SHORT).show();

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        String sCatalogRecord = GlobalClass.getCatalogRecordString(gciCatalogItem);
        Data dataCatalogDeleteItem = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_ComicDetails:DeleteComic()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(GlobalClass.EXTRA_CATALOG_ITEM, sCatalogRecord)
                .build();
        OneTimeWorkRequest otwrCatalogDeleteItem = new OneTimeWorkRequest.Builder(Worker_Catalog_DeleteItem.class)
                .setInputData(dataCatalogDeleteItem)
                .addTag(Worker_Catalog_DeleteItem.TAG_WORKER_CATALOG_DELETEITEM) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(otwrCatalogDeleteItem);
        GlobalClass.gbTagHistogramRequiresUpdate[gciCatalogItem.iMediaCategory] = true;

        finish();


    }



    //=====================================================================================
    //===== RecyclerView Code =================================================================
    //=====================================================================================

    public void populate_RecyclerViewComicPages(){

        RecyclerView recyclerView = findViewById(R.id.RecyclerView_ComicPages);
        // use this setting to
        // improve performance if you know that changes
        // in content do not change the layout size
        // of the RecyclerView
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager;
        // In landscape
        // use a grid layout manager
        final GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4, RecyclerView.VERTICAL, false);

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

            recyclerView.setLayoutManager(gridLayoutManager);

        } else {
            // In portrait
            // use a linear layout manager
            layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
        }


        gRecyclerViewComicPagesAdapter = new Activity_ComicDetails.RecyclerViewComicPagesAdapter(gtmComicPages);
        recyclerView.setAdapter(gRecyclerViewComicPagesAdapter);
        recyclerView.setItemViewCacheSize(8);

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return gRecyclerViewComicPagesAdapter.isHeader(position) ? gridLayoutManager.getSpanCount() : 1;
            }
        });


    }

    public class RecyclerViewComicPagesAdapter extends RecyclerView.Adapter<Activity_ComicDetails.RecyclerViewComicPagesAdapter.ViewHolder> {

        //http://blog.sqisland.com/2014/12/recyclerview-grid-with-header.html

        private final TreeMap<Integer, Uri> treeMap;

        private static final int ITEM_VIEW_TYPE_HEADER = 0;
        private static final int ITEM_VIEW_TYPE_ITEM = 1;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView imageView_Thumbnail;
            public final TextView textView_Title;

            public final ImageView imageView_EditComicDetails;

            public final TextView textView_ComicSource;
            public final TextView textView_Parodies;
            public final TextView textView_Characters;
            public final TextView textView_Tags;
            public final TextView textView_Artists;
            public final TextView textView_Groups;
            public final TextView textView_Languages;
            public final TextView textView_Categories;
            public final TextView textView_Pages;
            public final TextView textView_FileCount;
            public final TextView textView_MissingPages;
            public final TextView textView_ComicID;

            public final TextView textView_LabelComicSource;
            public final TextView textView_LabelParodies;
            public final TextView textView_LabelCharacters;
            public final TextView textView_LabelTags;
            public final TextView textView_LabelArtists;
            public final TextView textView_LabelGroups;
            public final TextView textView_LabelLanguages;
            public final TextView textView_LabelCategories;
            public final TextView textView_LabelPages;
            public final TextView textView_LabelFileCount;
            public final TextView textView_LabelMissingPages;
            public final TextView textView_LabelComicID;

            public final ImageButton button_Delete;
            public final ImageButton imageButton_OpenGroupingControls;

            public ViewHolder(View v) {
                super(v);
                imageView_Thumbnail = v.findViewById(R.id.imageView_Thumbnail);
                textView_Title = v.findViewById(R.id.textView_Title);

                imageView_EditComicDetails = v.findViewById(R.id.imageView_EditComicDetails);

                textView_ComicSource = v.findViewById(R.id.textView_ComicSource);
                textView_Parodies = v.findViewById(R.id.textView_Parodies);
                textView_Characters = v.findViewById(R.id.textView_Characters);
                textView_Tags = v.findViewById(R.id.textView_Tags);
                textView_Artists = v.findViewById(R.id.textView_Artists);
                textView_Groups = v.findViewById(R.id.textView_Groups);
                textView_Languages = v.findViewById(R.id.textView_Languages);
                textView_Categories = v.findViewById(R.id.textView_Categories);
                textView_Pages = v.findViewById(R.id.textView_Pages);
                textView_FileCount = v.findViewById(R.id.textView_FileCount);
                textView_MissingPages = v.findViewById(R.id.textView_MissingPages);
                textView_ComicID = v.findViewById(R.id.textView_ComicID);

                textView_LabelComicSource = v.findViewById(R.id.textView_LabelComicSource);
                textView_LabelParodies = v.findViewById(R.id.textView_LabelParodies);
                textView_LabelCharacters = v.findViewById(R.id.textView_LabelCharacters);
                textView_LabelTags = v.findViewById(R.id.textView_LabelTags);
                textView_LabelArtists = v.findViewById(R.id.textView_LabelArtists);
                textView_LabelGroups = v.findViewById(R.id.textView_LabelGroups);
                textView_LabelLanguages = v.findViewById(R.id.textView_LabelLanguages);
                textView_LabelCategories = v.findViewById(R.id.textView_LabelCategories);
                textView_LabelPages = v.findViewById(R.id.textView_LabelPages);
                textView_LabelFileCount = v.findViewById(R.id.textView_LabelFileCount);
                textView_LabelMissingPages = v.findViewById(R.id.textView_LabelMissingPages);
                textView_LabelComicID = v.findViewById(R.id.textView_LabelComicID);

                button_Delete = v.findViewById(R.id.button_Delete);
                imageButton_OpenGroupingControls = v.findViewById(R.id.imageButton_OpenGroupingControls);
            }
        }

        public RecyclerViewComicPagesAdapter(TreeMap<Integer, Uri> data) {
            this.treeMap = data;
        }


        //START HEADER-SPECIFIC ROUTINES
        @Override
        public int getItemCount() {
            return treeMap.size();
        }

        public boolean isHeader(int position) {
            return position == 0;
        }

        @Override
        public int getItemViewType(int position) {
            return isHeader(position) ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_ITEM;
        }
        //END HEADER-SPECIFIC ROUTINES

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public Activity_ComicDetails.RecyclerViewComicPagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                                                 int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            if (viewType == ITEM_VIEW_TYPE_HEADER) {
                View headerView = inflater.inflate(R.layout.activity_comic_details_header, parent, false);
                return new Activity_ComicDetails.RecyclerViewComicPagesAdapter.ViewHolder(headerView);
            }

            // create a new view
            View v;

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                v = inflater.inflate(R.layout.recycler_catalog_grid, parent, false);
            } else {
                v = inflater.inflate(R.layout.recycler_catalog_row, parent, false);
            }

            return new Activity_ComicDetails.RecyclerViewComicPagesAdapter.ViewHolder(v);
        }


        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull Activity_ComicDetails.RecyclerViewComicPagesAdapter.ViewHolder holder, final int position) {
            // - get element from your data set at this position
            // - replace the contents of the view with that element

            String sThumbnailText;

            if (isHeader(position)) {

                holder.textView_Title.setVisibility(View.VISIBLE);
                holder.textView_ComicSource.setVisibility(View.VISIBLE);
                holder.textView_Parodies.setVisibility(View.VISIBLE);
                holder.textView_Tags.setVisibility(View.VISIBLE);
                holder.textView_Artists.setVisibility(View.VISIBLE);
                holder.textView_Languages.setVisibility(View.VISIBLE);
                holder.textView_Categories.setVisibility(View.VISIBLE);
                holder.textView_Pages.setVisibility(View.VISIBLE);
                holder.textView_FileCount.setVisibility(View.VISIBLE);
                if (!gciCatalogItem.sComic_Missing_Pages.equals("")) {
                    holder.textView_MissingPages.setVisibility(View.VISIBLE);
                }
                holder.textView_ComicID.setVisibility(View.VISIBLE);

                holder.textView_LabelComicSource.setVisibility(View.VISIBLE);
                holder.textView_LabelParodies.setVisibility(View.VISIBLE);
                holder.textView_LabelTags.setVisibility(View.VISIBLE);
                holder.textView_LabelArtists.setVisibility(View.VISIBLE);
                holder.textView_LabelLanguages.setVisibility(View.VISIBLE);
                holder.textView_LabelCategories.setVisibility(View.VISIBLE);
                holder.textView_LabelPages.setVisibility(View.VISIBLE);
                holder.textView_LabelFileCount.setVisibility(View.VISIBLE);
                if (!gciCatalogItem.sComic_Missing_Pages.equals("")) {
                    holder.textView_LabelMissingPages.setVisibility(View.VISIBLE);
                }
                holder.textView_LabelComicID.setVisibility(View.VISIBLE);

                holder.imageView_EditComicDetails.setVisibility(View.VISIBLE);

                sThumbnailText = gciCatalogItem.sTitle;
                holder.textView_ComicSource.setText(gciCatalogItem.sSource);

                if(gciCatalogItem.sSource.startsWith("http")){
                    //If the source is an address, configure the link such that the user can click it and go to the webpage.
                    holder.textView_ComicSource.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intentBrowser = new Intent(getApplicationContext(), Activity_Browser.class);
                            intentBrowser.putExtra(Activity_Browser.EXTRA_STRING_WEB_ADDRESS_REQUEST, gciCatalogItem.sSource);
                            startActivity(intentBrowser);
                        }
                    });
                }

                holder.textView_Parodies.setText(gciCatalogItem.sComicParodies);
                holder.textView_Characters.setText(gciCatalogItem.sComicCharacters);

                String sTagText = globalClass.getTagTextsFromTagIDsString(gciCatalogItem.sTags, gciCatalogItem.iMediaCategory);
                holder.textView_Tags.setText(sTagText);

                holder.textView_Artists.setText(gciCatalogItem.sComicArtists);
                holder.textView_Groups.setText(gciCatalogItem.sComicGroups);
                holder.textView_Languages.setText(gciCatalogItem.sComicLanguages);
                holder.textView_Categories.setText(gciCatalogItem.sComicCategories);
                String sPages = "" + gciCatalogItem.iComicPages;
                holder.textView_Pages.setText(sPages);
                String sFileCount = "" + gciCatalogItem.iFile_Count;
                if(!gbComicFilesNotFound) {
                    holder.textView_FileCount.setText(sFileCount);
                } else {
                    holder.textView_FileCount.setText("0");
                }
                if (!gciCatalogItem.sComic_Missing_Pages.equals("")) {
                    String sMissingPages = gciCatalogItem.sComic_Missing_Pages;
                    if (sMissingPages.length() > 10) {
                        sMissingPages = sMissingPages.substring(0, 10) + "...";
                    }
                    holder.textView_MissingPages.setText(sMissingPages);
                }
                holder.textView_ComicID.setText(gciCatalogItem.sItemID);

                holder.imageView_EditComicDetails.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intentComicDetailsEditor = new Intent(getApplicationContext(), Activity_ComicDetailsEditor.class);
                        intentComicDetailsEditor.putExtra(Activity_ComicDetailsEditor.EXTRA_COMIC_CATALOG_ITEM, gciCatalogItem);
                        startActivity(intentComicDetailsEditor);
                    }
                });

            } else {
                sThumbnailText = "Page " + (position) + " of " + (getItemCount() - 1);  //Position is 0-based, but one extra item is added for the header.
                if (holder.imageButton_OpenGroupingControls != null) {
                    //Hide the grouping controls since this is not valid for individual pages of a comic.
                    holder.imageButton_OpenGroupingControls.setVisibility(View.INVISIBLE);
                }
            }

            Uri uriThumbnailFileName = treeMap.get(position);
            if(!gbComicFilesNotFound) {
                Glide.with(getApplicationContext()).load(uriThumbnailFileName).into(holder.imageView_Thumbnail);
            } else {
                Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_image_white_18dp_wpagepad, null);
                Glide.with(getApplicationContext()).load(drawable).into(holder.imageView_Thumbnail);
            }

            holder.textView_Title.setText(sThumbnailText);


            holder.imageView_Thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(gbComicFilesNotFound){
                        return;
                    }

                    if (gbDebugTouch)
                        Toast.makeText(getApplicationContext(), "Click Item Number " + position, Toast.LENGTH_SHORT).show();
                    int iCorrectedPosition = position;
                    if (iCorrectedPosition > 0) {
                        iCorrectedPosition = position - 1; //This is because we repeat the first image - once in the header, and again in the recyclerview grid.
                    }

                    StartComicViewerActivity(iCorrectedPosition);
                }
            });

            if (holder.button_Delete != null) {
                if (uriThumbnailFileName != null) {
                    final String sUriFileToDelete = uriThumbnailFileName.toString();
                    holder.button_Delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            //Present confirmation that the user wishes to delete this item.
                            String sConfirmationMessage = "Confirm file deletion: " + GlobalClass.cleanHTMLCodedCharacters(sUriFileToDelete);

                            AlertDialog.Builder builder = new AlertDialog.Builder(Activity_ComicDetails.this, R.style.AlertDialogCustomStyle);
                            builder.setTitle("Delete File");
                            builder.setMessage(sConfirmationMessage);
                            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @SuppressLint("NotifyDataSetChanged")
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    Toast.makeText(getApplicationContext(), "Deleting file...", Toast.LENGTH_SHORT).show();
                                    boolean bDeleteSuccess = false;
                                    try {
                                        Uri uriSourceFile = Uri.parse(sUriFileToDelete);
                                        bDeleteSuccess = DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, uriSourceFile);
                                    } catch (Exception e) {
                                        String sMessage = "Could not delete file. Message: " + e.getMessage();
                                        Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_LONG).show();
                                    }
                                    if (bDeleteSuccess) {
                                        Toast.makeText(getApplicationContext(), "File successfully deleted.", Toast.LENGTH_SHORT).show();
                                        //Trigger a reinitialization of the gridview?
                                        treeMap.remove(position);
                                        //Shift treeMap contents down:
                                        Uri uriTemp;
                                        for (int j = position; j < treeMap.size(); j++) {
                                            uriTemp = treeMap.get(j + 1);
                                            treeMap.put(j, uriTemp);
                                            treeMap.remove(j + 1);
                                        }
                                        gciCatalogItem.iFile_Count--;
                                        globalClass.CatalogDataFile_UpdateRecord(gciCatalogItem);
                                        if(position == 1){
                                            treeMap.put(0, treeMap.get(1)); //If the user deleted the first page, update the header slot.
                                        }
                                        //notifyItemRangeChanged(0, treeMap.size() - 1); //All items have changed because the "Page X of N" text has changed. However, this always appears to crash the app.
                                        notifyDataSetChanged();
                                    }
                                }
                            });
                            builder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());
                            AlertDialog adConfirmationDialog = builder.create();
                            adConfirmationDialog.show();
                        }
                    });
                }

            }


        }

    }

    //=====================================================================================
    //===== Comic Viewer Code =================================================================
    //=====================================================================================

    public void StartComicViewerActivity(int iComicPage){
        Intent intentComicViewer = new Intent(this, Activity_ComicViewer2.class);

        intentComicViewer.putExtra(GlobalClass.EXTRA_CATALOG_ITEM, gciCatalogItem);
        intentComicViewer.putExtra(Activity_ComicViewer2.EXTRA_COMIC_PAGE_START, iComicPage);

        startActivity(intentComicViewer);
    }

    //=====================================================================================
    //===== Obfuscation Code =================================================================
    //=====================================================================================

    @Override
    public void onResume(){
        super.onResume();

        loadComicPageData();

    }

    //=====================================================================================
    //===== Data Update Code =================================================================
    //=====================================================================================

    public class ComicDetailsResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;
            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to update log:
                boolean bUpdateLog;

                //Get booleans from the intent telling us what to update:
                bUpdateLog = intent.getBooleanExtra(GlobalClass.UPDATE_LOG_BOOLEAN,false);

                if(bUpdateLog){
                    String sLogLine;
                    sLogLine = intent.getStringExtra(GlobalClass.LOG_LINE_STRING);
                    if(sLogLine != null && gtextView_ComicDetailsLog != null) {

                        gtextView_ComicDetailsLog.append(sLogLine);

                    }

                }


            }

        }
    }



    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(gComicDetailsResponseReceiver);
        super.onDestroy();
    }





}