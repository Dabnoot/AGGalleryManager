package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class Activity_ComicDetails extends AppCompatActivity {
    //Global Variables:
    private GlobalClass globalClass;

    private String gsComicItemID = "";
    private ItemClass_CatalogItem gciCatalogItem;
    private TreeMap<Integer, String> gtmComicPages;

    public static final String EXTRA_CATALOG_ITEM_ID = "com.agcurations.aggallerymanager.extra.CATALOG_ITEM_ID";

    private Activity_ComicDetails.ComicDetailsResponseReceiver gComicDetailsResponseReceiver;

    private RecyclerViewComicPagesAdapter gRecyclerViewComicPagesAdapter;

    private final boolean gbDebugTouch = false;

    TextView gtextView_ComicDetailsLog;

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
        gsComicItemID = intent.getStringExtra(EXTRA_CATALOG_ITEM_ID);



        if( gsComicItemID == null) return;

        gtextView_ComicDetailsLog = findViewById(R.id.textView_ComicDetailsLog);
        gtextView_ComicDetailsLog.setMovementMethod(new ScrollingMovementMethod());

        loadComicPageData();


        IntentFilter filter = new IntentFilter(ComicDetailsResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        gComicDetailsResponseReceiver = new Activity_ComicDetails.ComicDetailsResponseReceiver();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(gComicDetailsResponseReceiver, filter);

        //See additional initialization in onCreateOptionsMenu().
    }

    private void loadComicPageData(){
        //This was put in place to handle the scenario of missing file download completion - when missing files are downloaded.

        //Look-up the item and grab a copy:
        if (!gsComicItemID.equals("")) {
            for (Map.Entry<String, ItemClass_CatalogItem>
                    entry : globalClass.gtmCatalogLists.get(globalClass.giSelectedCatalogMediaCategory).entrySet()) {
                if(gsComicItemID.equals(entry.getKey())){
                    gciCatalogItem = entry.getValue();
                    break;
                }
            }
        }

        String sComicFolder_AbsolutePath = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath();
        String sComicFolderPath;
        sComicFolderPath = sComicFolder_AbsolutePath + File.separator
                + gciCatalogItem.sFolder_Name;
        if(gciCatalogItem.iSpecialFlag == ItemClass_CatalogItem.FLAG_PROCESSING_COMIC_DLM_MOVE){
            //If this is a downloaded comic and the files from DownloadManager have not been moved as
            //  part of download post-processing, look in the [comic]\download folder for the files:
            sComicFolderPath = sComicFolder_AbsolutePath + File.separator
                    + gciCatalogItem.sFolder_Name + File.separator
                    + GlobalClass.gsDLTempFolderName + File.separator;
        }

        //Load the full path to each comic page into tmComicPages:
        if(gciCatalogItem.iSpecialFlag == ItemClass_CatalogItem.FLAG_PROCESSING_COMIC_DLM_MOVE){
            //If this is a downloaded comic and the files from DownloadManager have not been moved as
            //  part of download post-processing, look in the [comic]\download folder for the files:
            sComicFolderPath = sComicFolder_AbsolutePath + File.separator
                    + gciCatalogItem.sFolder_Name + File.separator
                    + GlobalClass.gsDLTempFolderName + File.separator;
        }
        File fComicFolder = new File(sComicFolderPath);
        TreeMap<String, String> tmSortByFileName = new TreeMap<>();
        if(fComicFolder.exists()){
            File[] fComicPages = fComicFolder.listFiles();
            if(fComicPages != null) {
                for (File fComicPage : fComicPages) {
                    if(fComicPage.isFile()) {
                        tmSortByFileName.put(GlobalClass.JumbleFileName(fComicPage.getName()), fComicPage.getAbsolutePath());
                    }
                }
            }

            if(tmSortByFileName.size() == 0) {
                gtextView_ComicDetailsLog.setVisibility(View.VISIBLE);
                String sMessage = "No comic files found in folder at: " + fComicFolder.getAbsolutePath() + "\n";
                sMessage = sMessage  + "Comic source: " + gciCatalogItem.sSource + "\n";
                gtextView_ComicDetailsLog.setText(sMessage);
                gtextView_ComicDetailsLog.bringToFront();
            } else {
                gtextView_ComicDetailsLog.setVisibility(View.INVISIBLE);
                gtextView_ComicDetailsLog.setText("");
                gciCatalogItem.iFile_Count = fComicPages.length; //update the comic file count. Files may have been downloaded, deleted, etc.

                if(!gciCatalogItem.sComic_Missing_Pages.equals("")) {
                    String sMissingPages = gciCatalogItem.sComic_Missing_Pages;
                    //Check to see if this comic is missing any pages:
                    gciCatalogItem = globalClass.analyzeComicReportMissingPages(gciCatalogItem);
                    if(!sMissingPages.equals(gciCatalogItem.sComic_Missing_Pages)){
                        //Update the catalog file with the new record of missing pages:
                        globalClass.CatalogDataFile_UpdateRecord(gciCatalogItem);
                    }
                    globalClass.gbCatalogViewerRefresh = true;
                }
            }
        }



        gtmComicPages = new TreeMap<>();
        int i = 0;
        for(Map.Entry<String, String> tmFiles: tmSortByFileName.entrySet()){
            gtmComicPages.put(i, tmFiles.getValue());
            i++;
        }



        populate_RecyclerViewComicPages();

        if(globalClass.ObfuscationOn) {
            Obfuscate();
        } else {
            RemoveObfuscation();
        }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comic_details_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_FlipView) {
            FlipObfuscation();
            return true;
        } else if (item.getItemId() == R.id.menu_DeleteComic) {
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
        gtmComicPages = new TreeMap<>();
        gRecyclerViewComicPagesAdapter.notifyDataSetChanged();
        globalClass.gbCatalogViewerRefresh = true;
        if(globalClass.ComicCatalog_DeleteComic(gciCatalogItem)) {
            //If comic deletion successful, close the activity. Otherwise remain open so that
            //  the user can view the toast message.
            finish();
        }
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

            View header = LayoutInflater.from(this).inflate(
                    R.layout.activity_comic_details_header, recyclerView, false);
            header.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext(), "Header clicked.",
                            Toast.LENGTH_SHORT).show();
                }
            });



        } else {
            // In portrait
            // use a linear layout manager
            layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
        }


        gRecyclerViewComicPagesAdapter = new Activity_ComicDetails.RecyclerViewComicPagesAdapter(gtmComicPages);
        recyclerView.setAdapter(gRecyclerViewComicPagesAdapter);

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return gRecyclerViewComicPagesAdapter.isHeader(position) ? gridLayoutManager.getSpanCount() : 1;
            }
        });


    }

    public class RecyclerViewComicPagesAdapter extends RecyclerView.Adapter<Activity_ComicDetails.RecyclerViewComicPagesAdapter.ViewHolder> {

        //http://blog.sqisland.com/2014/12/recyclerview-grid-with-header.html

        private final TreeMap<Integer, String> treeMap;

        private static final int ITEM_VIEW_TYPE_HEADER = 0;
        private static final int ITEM_VIEW_TYPE_ITEM = 1;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView ivThumbnail;
            public final TextView tvThumbnailText;

            public final ImageView imageView_EditComicDetails;

            public final TextView tvComicSource;
            public final TextView tvParodies;
            public final TextView tvCharacters;
            public final TextView tvTags;
            public final TextView tvArtists;
            public final TextView tvGroups;
            public final TextView tvLanguages;
            public final TextView tvCategories;
            public final TextView tvPages;
            public final TextView textView_FileCount;
            public final TextView textView_MissingPages;
            public final TextView tvComicID;

            public final TextView tvLabelComicSource;
            public final TextView tvLabelParodies;
            public final TextView tvLabelCharacters;
            public final TextView tvLabelTags;
            public final TextView tvLabelArtists;
            public final TextView tvLabelGroups;
            public final TextView tvLabelLanguages;
            public final TextView tvLabelCategories;
            public final TextView tvLabelPages;
            public final TextView textView_LabelFileCount;
            public final TextView textView_LabelMissingPages;
            public final TextView tvLabelComicID;

            public final Button button_Delete;

            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.imageView_Thumbnail);
                tvThumbnailText = v.findViewById(R.id.textView_Title);

                imageView_EditComicDetails = v.findViewById(R.id.imageView_EditComicDetails);

                tvComicSource = v.findViewById(R.id.textView_ComicSource);
                tvParodies = v.findViewById(R.id.textView_Parodies);
                tvCharacters = v.findViewById(R.id.textView_Characters);
                tvTags = v.findViewById(R.id.textView_Tags);
                tvArtists = v.findViewById(R.id.textView_Artists);
                tvGroups = v.findViewById(R.id.textView_Groups);
                tvLanguages = v.findViewById(R.id.textView_Languages);
                tvCategories = v.findViewById(R.id.textView_Categories);
                tvPages = v.findViewById(R.id.textView_Pages);
                textView_FileCount = v.findViewById(R.id.textView_FileCount);
                textView_MissingPages = v.findViewById(R.id.textView_MissingPages);
                tvComicID = v.findViewById(R.id.textView_ComicID);

                tvLabelComicSource = v.findViewById(R.id.textView_LabelComicSource);
                tvLabelParodies  = v.findViewById(R.id.textView_LabelParodies);
                tvLabelCharacters  = v.findViewById(R.id.textView_LabelCharacters);
                tvLabelTags = v.findViewById(R.id.textView_LabelTags);
                tvLabelArtists = v.findViewById(R.id.textView_LabelArtists);
                tvLabelGroups  = v.findViewById(R.id.textView_LabelGroups);
                tvLabelLanguages = v.findViewById(R.id.textView_LabelLanguages);
                tvLabelCategories = v.findViewById(R.id.textView_LabelCategories);
                tvLabelPages = v.findViewById(R.id.textView_LabelPages);
                textView_LabelFileCount = v.findViewById(R.id.textView_LabelFileCount);
                textView_LabelMissingPages = v.findViewById(R.id.textView_LabelMissingPages);
                tvLabelComicID = v.findViewById(R.id.textView_LabelComicID);

                button_Delete = v.findViewById(R.id.button_Delete);
            }
        }

        public RecyclerViewComicPagesAdapter(TreeMap<Integer, String> data) {
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

            if (globalClass.ObfuscationOn) {

                if (isHeader(position)) {
                    holder.tvThumbnailText.setVisibility(View.INVISIBLE);
                    holder.tvComicSource.setVisibility(View.INVISIBLE);
                    holder.tvParodies.setVisibility(View.INVISIBLE);
                    holder.tvCharacters.setVisibility(View.INVISIBLE);
                    holder.tvTags.setVisibility(View.INVISIBLE);
                    holder.tvArtists.setVisibility(View.INVISIBLE);
                    holder.tvGroups.setVisibility(View.INVISIBLE);
                    holder.tvLanguages.setVisibility(View.INVISIBLE);
                    holder.tvCategories.setVisibility(View.INVISIBLE);
                    holder.tvPages.setVisibility(View.INVISIBLE);
                    holder.textView_FileCount.setVisibility(View.INVISIBLE);
                    holder.textView_MissingPages.setVisibility(View.INVISIBLE);
                    holder.tvComicID.setVisibility(View.INVISIBLE);

                    holder.tvLabelComicSource.setVisibility(View.INVISIBLE);
                    holder.tvLabelParodies.setVisibility(View.INVISIBLE);
                    holder.tvLabelCharacters.setVisibility(View.INVISIBLE);
                    holder.tvLabelTags.setVisibility(View.INVISIBLE);
                    holder.tvLabelArtists.setVisibility(View.INVISIBLE);
                    holder.tvLabelGroups.setVisibility(View.INVISIBLE);
                    holder.tvLabelLanguages.setVisibility(View.INVISIBLE);
                    holder.tvLabelCategories.setVisibility(View.INVISIBLE);
                    holder.tvLabelPages.setVisibility(View.INVISIBLE);
                    holder.textView_LabelFileCount.setVisibility(View.INVISIBLE);
                    holder.textView_LabelMissingPages.setVisibility(View.INVISIBLE);
                    holder.tvLabelComicID.setVisibility(View.INVISIBLE);

                    holder.imageView_EditComicDetails.setVisibility(View.INVISIBLE);
                }


                //Get the obfuscation image index:
                int i = (position % globalClass.getObfuscationImageCount());
                //Get the obfuscation image resource ID:
                int iObfuscatorResourceID = globalClass.getObfuscationImage(i);

                Bitmap bmObfuscator = BitmapFactory.decodeResource(getResources(), iObfuscatorResourceID);
                holder.ivThumbnail.setImageBitmap(bmObfuscator);
                sThumbnailText = globalClass.getObfuscationImageText(i);
            } else {

                //Load the non-obfuscated data into the RecyclerView ViewHolder:

                if (isHeader(position)) {

                    holder.tvThumbnailText.setVisibility(View.VISIBLE);
                    holder.tvComicSource.setVisibility(View.VISIBLE);
                    holder.tvParodies.setVisibility(View.VISIBLE);
                    holder.tvTags.setVisibility(View.VISIBLE);
                    holder.tvArtists.setVisibility(View.VISIBLE);
                    holder.tvLanguages.setVisibility(View.VISIBLE);
                    holder.tvCategories.setVisibility(View.VISIBLE);
                    holder.tvPages.setVisibility(View.VISIBLE);
                    holder.textView_FileCount.setVisibility(View.VISIBLE);
                    if(!gciCatalogItem.sComic_Missing_Pages.equals("")) {
                        holder.textView_MissingPages.setVisibility(View.VISIBLE);
                    }
                    holder.tvComicID.setVisibility(View.VISIBLE);

                    holder.tvLabelComicSource.setVisibility(View.VISIBLE);
                    holder.tvLabelParodies .setVisibility(View.VISIBLE);
                    holder.tvLabelTags.setVisibility(View.VISIBLE);
                    holder.tvLabelArtists.setVisibility(View.VISIBLE);
                    holder.tvLabelLanguages.setVisibility(View.VISIBLE);
                    holder.tvLabelCategories.setVisibility(View.VISIBLE);
                    holder.tvLabelPages.setVisibility(View.VISIBLE);
                    holder.textView_LabelFileCount.setVisibility(View.VISIBLE);
                    if(!gciCatalogItem.sComic_Missing_Pages.equals("")) {
                        holder.textView_LabelMissingPages.setVisibility(View.VISIBLE);
                    }
                    holder.tvLabelComicID.setVisibility(View.VISIBLE);

                    holder.imageView_EditComicDetails.setVisibility(View.VISIBLE);

                    sThumbnailText = gciCatalogItem.sTitle;
                    holder.tvComicSource.setText(gciCatalogItem.sSource);
                    holder.tvParodies.setText(gciCatalogItem.sComicParodies);
                    holder.tvCharacters.setText(gciCatalogItem.sComicCharacters);

                    String sTagText = globalClass.getTagTextsFromTagIDsString(gciCatalogItem.sTags, gciCatalogItem.iMediaCategory);
                    holder.tvTags.setText(sTagText);

                    holder.tvArtists.setText(gciCatalogItem.sComicArtists);
                    holder.tvGroups.setText(gciCatalogItem.sComicGroups);
                    holder.tvLanguages.setText(gciCatalogItem.sComicLanguages);
                    holder.tvCategories.setText(gciCatalogItem.sComicCategories);
                    String sPages = "" + gciCatalogItem.iComicPages;
                    holder.tvPages.setText(sPages);
                    String sFileCount = "" + gciCatalogItem.iFile_Count;
                    holder.textView_FileCount.setText(sFileCount);
                    if(!gciCatalogItem.sComic_Missing_Pages.equals("")){
                        String sMissingPages = gciCatalogItem.sComic_Missing_Pages;
                        if(sMissingPages.length() > 10){
                            sMissingPages = sMissingPages.substring(0,10) + "...";
                        }
                        holder.textView_MissingPages.setText(sMissingPages);
                    }
                    holder.tvComicID.setText(gciCatalogItem.sItemID);

                    holder.imageView_EditComicDetails.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intentComicDetailsEditor = new Intent(getApplicationContext(), Activity_ComicDetailsEditor.class);
                            intentComicDetailsEditor.putExtra(Activity_ComicDetailsEditor.EXTRA_COMIC_CATALOG_ITEM, gciCatalogItem);
                            startActivity(intentComicDetailsEditor);

                        }
                    });

                } else {
                    sThumbnailText = "Page " + (position + 1) + " of " + getItemCount();  //Position is 0-based.
                }


                String sThumbnailFilePath = gtmComicPages.get(position);
                if (sThumbnailFilePath != null) {
                    File fThumbnail = new File(sThumbnailFilePath);
                    if (fThumbnail.exists()) {
                        Glide.with(getApplicationContext()).load(fThumbnail).into(holder.ivThumbnail);
                    }
                }

            }

            holder.tvThumbnailText.setText(sThumbnailText);


            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gbDebugTouch)
                        Toast.makeText(getApplicationContext(), "Click Item Number " + position, Toast.LENGTH_LONG).show();
                    StartComicViewerActivity(position);
                }
            });

            holder.ivThumbnail.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (gbDebugTouch)
                        Toast.makeText(getApplicationContext(), "Long press detected", Toast.LENGTH_SHORT).show();
                    Obfuscate();
                    return true;// returning true instead of false, works for me
                }
            });

            if(holder.button_Delete != null){
                holder.button_Delete.setVisibility(View.INVISIBLE);
            }


        }


    }


    //=====================================================================================
    //===== Comic Viewer Code =================================================================
    //=====================================================================================

    public void StartComicViewerActivity(int iComicPage){
        Intent intentComicViewer = new Intent(this, Activity_ComicViewer.class);

        intentComicViewer.putExtra(Activity_ComicViewer.EXTRA_CATALOG_ITEM, gciCatalogItem);
        intentComicViewer.putExtra(Activity_ComicViewer.EXTRA_COMIC_PAGE_START, iComicPage);

        startActivity(intentComicViewer);
    }

    //=====================================================================================
    //===== Obfuscation Code =================================================================
    //=====================================================================================

    @Override
    public void onResume(){
        super.onResume();

        loadComicPageData();

/*        if(globalClass.ObfuscationOn) {
            //Obfuscate data:
            Obfuscate();
        } else {
            //Remove obfuscation:
            RemoveObfuscation();
        }*/


    }

    public void FlipObfuscation() {
        //This routine primarily accessed via the toggle option on the menu.
        globalClass.ObfuscationOn = !globalClass.ObfuscationOn;
        if(globalClass.ObfuscationOn) {
            //Obfuscate data:
            Obfuscate();
        } else {
            //Remove obfuscation:
            RemoveObfuscation();
        }
    }

    public void Obfuscate() {
        //This routine is separate because it can be activated
        // by either a long-press or the toggle option on the menu.
        if(!globalClass.ObfuscationOn) {
            globalClass.ObfuscationOn = true;
        }
        setTitle(globalClass.getObfuscationCategoryName());

        //Update the RecyclerView:
        gRecyclerViewComicPagesAdapter.notifyDataSetChanged();
    }

    public void RemoveObfuscation(){
        //Remove obfuscation:
        setTitle(gciCatalogItem.sTitle);

        //Update the RecyclerView:
        gRecyclerViewComicPagesAdapter.notifyDataSetChanged();
    }

    //=====================================================================================
    //===== Data Update Code =================================================================
    //=====================================================================================

    public class ComicDetailsResponseReceiver extends BroadcastReceiver {
        public static final String COMIC_DETAILS_DATA_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.COMIC_DETAILS_DATA_ACTION_RESPONSE";

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