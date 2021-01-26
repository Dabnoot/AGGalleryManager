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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

    private ItemClass_CatalogItem gciCatalogItem;
    private TreeMap<Integer, String> gtmComicPages;

    private MenuItem gmiGetOnlineData;
    private MenuItem gmiSaveDetails;

    public static final String EXTRA_CATALOG_ITEM = "CATALOG_ITEM";

    private Activity_ComicDetails.ComicDetailsResponseReceiver gComicDetailsResponseReceiver;

    private RecyclerViewComicPagesAdapter gRecyclerViewComicPagesAdapter;

    private final boolean gbDebugTouch = false;
    private boolean gbAutoAcquireData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_details);

        getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorDarkDarkOrange));
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        bar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.colorDarkDarkOrange)));
        this.getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.startup_screen_background));


        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        Intent intent = getIntent();
        gciCatalogItem = (ItemClass_CatalogItem) intent.getSerializableExtra(EXTRA_CATALOG_ITEM);

        if( gciCatalogItem == null) return;

        String sComicFolder_AbsolutePath = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath();
        String sComicFolderPath;
        sComicFolderPath = sComicFolder_AbsolutePath + File.separator
                + gciCatalogItem.sFolder_Name;

        //Load the full path to each comic page into tmComicPages:
        File fComicFolder = new File(sComicFolderPath);
        TreeMap<String, String> tmSortByFileName = new TreeMap<>();
        if(fComicFolder.exists()){
            File[] fComicPages = fComicFolder.listFiles();
            if(fComicPages != null) {
                for (File fComicPage : fComicPages) {
                    tmSortByFileName.put(GlobalClass.JumbleFileName(fComicPage.getName()), fComicPage.getAbsolutePath());
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

        IntentFilter filter = new IntentFilter(ComicDetailsResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        gComicDetailsResponseReceiver = new Activity_ComicDetails.ComicDetailsResponseReceiver();
        registerReceiver(gComicDetailsResponseReceiver, filter);

        //See additional initialization in onCreateOptionsMenu().
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comic_details_menu, menu);
        gmiGetOnlineData = menu.findItem(R.id.menu_GetOnlineData);
        gmiSaveDetails = menu.findItem(R.id.menu_SaveDetails);

        if(globalClass.bAutoDownloadOn) {
            if (!gciCatalogItem.bComic_Online_Data_Acquired) {
                //If there is no tag data, automatically go out and try to get it.
                gbAutoAcquireData = true;
                SyncOnlineData();
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_FlipView) {
            FlipObfuscation();
            return true;
        } else if (item.getItemId() == R.id.menu_GetOnlineData) {
            SyncOnlineData();
            return true;
        } else if (item.getItemId() == R.id.menu_SaveDetails) {
            SaveDetails(gciCatalogItem);
            return true;
        } else if (item.getItemId() == R.id.menu_DeleteComic) {
            DeleteComicPrompt();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void DeleteComicPrompt(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

            public final TextView tvComicSource;
            public final TextView tvParodies;
            public final TextView tvCharacters;
            public final TextView tvTags;
            public final TextView tvArtists;
            public final TextView tvGroups;
            public final TextView tvLanguages;
            public final TextView tvCategories;
            public final TextView tvPages;
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
            public final TextView tvLabelComicID;

            public final Button button_Delete;

            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.imageView_Thumbnail);
                tvThumbnailText = v.findViewById(R.id.textView_Title);

                tvComicSource = v.findViewById(R.id.textView_ComicSource);
                tvParodies = v.findViewById(R.id.textView_Parodies);
                tvCharacters = v.findViewById(R.id.textView_Characters);
                tvTags = v.findViewById(R.id.textView_Tags);
                tvArtists = v.findViewById(R.id.textView_Artists);
                tvGroups = v.findViewById(R.id.textView_Groups);
                tvLanguages = v.findViewById(R.id.textView_Languages);
                tvCategories = v.findViewById(R.id.textView_Categories);
                tvPages = v.findViewById(R.id.textView_Pages);
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
                    holder.tvLabelComicID.setVisibility(View.INVISIBLE);
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
                    holder.tvComicID.setVisibility(View.VISIBLE);

                    holder.tvLabelComicSource.setVisibility(View.VISIBLE);
                    holder.tvLabelParodies .setVisibility(View.VISIBLE);
                    holder.tvLabelTags.setVisibility(View.VISIBLE);
                    holder.tvLabelArtists.setVisibility(View.VISIBLE);
                    holder.tvLabelLanguages.setVisibility(View.VISIBLE);
                    holder.tvLabelCategories.setVisibility(View.VISIBLE);
                    holder.tvLabelPages.setVisibility(View.VISIBLE);
                    holder.tvLabelComicID.setVisibility(View.VISIBLE);

                    sThumbnailText = gciCatalogItem.sComicName;
                    holder.tvComicSource.setText(gciCatalogItem.sSource);
                    holder.tvParodies.setText(gciCatalogItem.sComicParodies);
                    holder.tvCharacters.setText(gciCatalogItem.sComicCharacters);
                    StringBuilder sbTags = new StringBuilder();
                    if(!gciCatalogItem.sTags.equals("")) {
                        String[] sTagIDs = gciCatalogItem.sTags.split(",");
                        for (String sTagID : sTagIDs) {
                            sbTags.append(globalClass.getTagTextFromID(Integer.parseInt(sTagID), GlobalClass.MEDIA_CATEGORY_COMICS));
                            sbTags.append(", ");
                        }
                        String sTagTextAggregate = sbTags.toString();
                        if (sTagTextAggregate.contains(",")) {
                            sTagTextAggregate = sTagTextAggregate.substring(0, sTagTextAggregate.lastIndexOf(", "));
                        }
                        holder.tvTags.setText(sTagTextAggregate);
                    }
                    holder.tvArtists.setText(gciCatalogItem.sComicArtists);
                    holder.tvGroups.setText(gciCatalogItem.sComicGroups);
                    holder.tvLanguages.setText(gciCatalogItem.sComicLanguages);
                    holder.tvCategories.setText(gciCatalogItem.sComicCategories);
                    String sPages = "" + gciCatalogItem.iComicPages;
                    holder.tvPages.setText(sPages);
                    holder.tvComicID.setText(gciCatalogItem.sItemID);
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
        Intent intentComicViewer = new Intent(this, Activity_SeriesImageViewer.class);

        intentComicViewer.putExtra(Activity_SeriesImageViewer.EXTRA_CATALOG_ITEM, gciCatalogItem);
        intentComicViewer.putExtra(Activity_SeriesImageViewer.EXTRA_COMIC_PAGE_START, iComicPage);

        startActivity(intentComicViewer);
    }

    //=====================================================================================
    //===== Obfuscation Code =================================================================
    //=====================================================================================

    @Override
    public void onResume(){
        super.onResume();
        if(globalClass.ObfuscationOn) {
            //Obfuscate data:
            Obfuscate();
        } else {
            //Remove obfuscation:
            RemoveObfuscation();
        }


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
        setTitle(gciCatalogItem.sComicName);

        //Update the RecyclerView:
        gRecyclerViewComicPagesAdapter.notifyDataSetChanged();
    }

    //=====================================================================================
    //===== Data Update Code =================================================================
    //=====================================================================================


    public void SyncOnlineData(){

        if(globalClass.isNetworkConnected) {

            Intent intentGetComicDetails;

            intentGetComicDetails = new Intent(this, Service_ComicDetails.class);
            intentGetComicDetails.putExtra(Service_ComicDetails.COMIC_CATALOG_ITEM, gciCatalogItem);

            gmiGetOnlineData.setEnabled(false);

            Toast.makeText(getApplicationContext(), "Getting online data...", Toast.LENGTH_LONG).show();

            startService(intentGetComicDetails);
        } else {
            Toast.makeText(getApplicationContext(), "No network connected.", Toast.LENGTH_LONG).show();
        }
    }

    public class ComicDetailsResponseReceiver extends BroadcastReceiver {
        public static final String COMIC_DETAILS_DATA_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_COMIC_DETAILS_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean bComicDetailsDataServiceSuccess;
            bComicDetailsDataServiceSuccess = intent.getBooleanExtra(Service_ComicDetails.COMIC_DETAILS_SUCCESS,
                                                         false);

            String sErrorMessage;
            if(bComicDetailsDataServiceSuccess) {


                gciCatalogItem = (ItemClass_CatalogItem) intent.getSerializableExtra(Service_ComicDetails.COMIC_CATALOG_ITEM);
                gciCatalogItem.bComic_Online_Data_Acquired = true;
                gmiSaveDetails.setEnabled(true);

                //Update the title bar:
                if(!globalClass.ObfuscationOn) {
                    //(only if not obfuscated)
                    RemoveObfuscation();
                }

                //Update the RecyclerView:
                gRecyclerViewComicPagesAdapter.notifyDataSetChanged();

                if(gbAutoAcquireData){
                    Toast.makeText(getApplicationContext(), "Online data acquired. Auto saving...", Toast.LENGTH_LONG).show();
                    SaveDetails(gciCatalogItem);
                } else {
                    Toast.makeText(getApplicationContext(), "Online data acquired. Don't forget to save.", Toast.LENGTH_LONG).show();
                }



            } else {
                sErrorMessage = intent.getStringExtra(Service_ComicDetails.COMIC_DETAILS_ERROR_MESSAGE);
                Toast.makeText(getApplicationContext(), "Error getting data online.\n" + sErrorMessage, Toast.LENGTH_LONG).show();
            }

            gmiGetOnlineData.setEnabled(true);

        }
    }


    public void SaveDetails(ItemClass_CatalogItem ci){

        
        //Update the catalog file:
        globalClass.CatalogDataFile_UpdateRecord(ci);

        gmiSaveDetails.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Data saved.", Toast.LENGTH_LONG).show();

    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(gComicDetailsResponseReceiver);
        super.onDestroy();
    }





}