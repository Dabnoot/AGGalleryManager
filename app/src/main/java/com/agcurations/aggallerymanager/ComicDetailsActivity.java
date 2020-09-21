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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

public class ComicDetailsActivity extends AppCompatActivity {
    //Global constants


    //Global Variables:

    private GlobalClass globalClass;

    private String[] gsComicFields;
    private TreeMap<Integer, String> gtmComicPages;



    private MenuItem gmiGetOnlineData;
    private MenuItem gmiSaveDetails;

    private ComicDetailsActivity.ComicDetailsResponseReceiver gComicDetailsResponseReceiver;

    private boolean gbComicDetailsTitleUpdateAvailable = false;
    private boolean gbComicDetailsParodiesDataUpdateAvailable = false;
    private boolean gbComicDetailsCharactersDataUpdateAvailable = false;
    private boolean gbComicDetailsTagsDataUpdateAvailable = false;
    private boolean gbComicDetailsArtistsDataUpdateAvailable = false;
    private boolean gbComicDetailsGroupsDataUpdateAvailable = false;
    private boolean gbComicDetailsLanguagesDataUpdateAvailable = false;
    private boolean gbComicDetailsCategoriesDataUpdateAvailable = false;
    private boolean gbComicDetailsPagesDataUpdateAvailable = false;

    private RecyclerViewComicPagesAdapter gRecyclerViewComicPagesAdapter;


    private boolean gbDebugTouch = false;
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

        gsComicFields = globalClass.gvSelectedComic; //Don't bother with using the intent to pass this data.
        if( gsComicFields == null) return;

        String sComicFolder_AbsolutePath = GlobalClass.gvfComicsFolder.getAbsolutePath();
        String sComicFolderPath;
        sComicFolderPath = sComicFolder_AbsolutePath + File.separator
                + gsComicFields[GlobalClass.COMIC_FOLDER_NAME_INDEX];

        //Load the full path to each comic page into tmComicPages:
        File fComicFolder = new File(sComicFolderPath);
        gtmComicPages = new TreeMap<>();
        if(fComicFolder.exists()){
            File[] fComicPages = fComicFolder.listFiles();
            if(fComicPages != null) {
                for (int i = 0; i < fComicPages.length; i++) {
                    gtmComicPages.put(i, fComicPages[i].getAbsolutePath());
                }
            }
        }

        populate_RecyclerViewComicPages();

        if(globalClass.ObfuscationOn) {
            Obfuscate();
        } else {
            RemoveObfuscation();
        }

        IntentFilter filter = new IntentFilter(ComicDetailsResponseReceiver.COMIC_DETAILS_DATA_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        gComicDetailsResponseReceiver = new ComicDetailsActivity.ComicDetailsResponseReceiver();
        registerReceiver(gComicDetailsResponseReceiver, filter);

        //See additional initialization in onCreateOptionsMenu().
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comic_details_menu, menu);
        gmiGetOnlineData = menu.findItem(R.id.menu_GetOnlineData);
        gmiSaveDetails = menu.findItem(R.id.menu_SaveDetails);

        if(globalClass.bAutoDownloadOn) {
            if (gsComicFields[GlobalClass.COMIC_ONLINE_DATA_ACQUIRED_INDEX].equals(GlobalClass.COMIC_ONLINE_DATA_ACQUIRED_NO)) {
                //If there is no tag data, automatically go out and try to get it.
                gbAutoAcquireData = true;
                SyncOnlineData();
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Display a message showing the name of the item selected.
        //Toast.makeText(this, "Selected Item: " +item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {
            case R.id.menu_FlipView:
                FlipObfuscation();
                return true;
            case R.id.menu_GetOnlineData:
                SyncOnlineData();
                return true;
            case R.id.menu_SaveDetails:
                SaveDetails();
                return true;
            case R.id.menu_DeleteComic:
                DeleteComicPrompt();
                return true;
            default:
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
        if(globalClass.ComicCatalog_DeleteComic(gsComicFields[GlobalClass.COMIC_ID_INDEX])) {
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


        gRecyclerViewComicPagesAdapter = new ComicDetailsActivity.RecyclerViewComicPagesAdapter(gtmComicPages);
        recyclerView.setAdapter(gRecyclerViewComicPagesAdapter);

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return gRecyclerViewComicPagesAdapter.isHeader(position) ? gridLayoutManager.getSpanCount() : 1;
            }
        });


    }

    public class RecyclerViewComicPagesAdapter extends RecyclerView.Adapter<ComicDetailsActivity.RecyclerViewComicPagesAdapter.ViewHolder> {

        //http://blog.sqisland.com/2014/12/recyclerview-grid-with-header.html

        private final TreeMap<Integer, String> treeMap;
        private final Integer[] mapKeys;

        private static final int ITEM_VIEW_TYPE_HEADER = 0;
        private static final int ITEM_VIEW_TYPE_ITEM = 1;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView ivThumbnail;
            public final TextView tvThumbnailText;

            public final TextView tvComicID;
            public final TextView tvParodies;
            public final TextView tvCharacters;
            public final TextView tvTags;
            public final TextView tvArtists;
            public final TextView tvGroups;
            public final TextView tvLanguages;
            public final TextView tvCategories;
            public final TextView tvPages;

            public final TextView tvLabelComicID;
            public final TextView tvLabelParodies;
            public final TextView tvLabelCharacters;
            public final TextView tvLabelTags;
            public final TextView tvLabelArtists;
            public final TextView tvLabelGroups;
            public final TextView tvLabelLanguages;
            public final TextView tvLabelCategories;
            public final TextView tvLabelPages;


            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.ImageView_Thumbnail);
                tvThumbnailText = v.findViewById(R.id.editText_ComicTitle);

                tvComicID = v.findViewById(R.id.editText_ComicSource);
                tvParodies = v.findViewById(R.id.editText_Parodies);
                tvCharacters = v.findViewById(R.id.editText_Characters);
                tvTags = v.findViewById(R.id.editText_Tags);
                tvArtists = v.findViewById(R.id.editText_Artists);
                tvGroups = v.findViewById(R.id.editText_Groups);
                tvLanguages = v.findViewById(R.id.editText_Languages);
                tvCategories = v.findViewById(R.id.editText_Categories);
                tvPages = v.findViewById(R.id.textView_Pages);

                tvLabelComicID = v.findViewById(R.id.textView_LabelComicSource);
                tvLabelParodies  = v.findViewById(R.id.textView_LabelParodies);
                tvLabelCharacters  = v.findViewById(R.id.textView_LabelCharacters);
                tvLabelTags = v.findViewById(R.id.textView_LabelTags);
                tvLabelArtists = v.findViewById(R.id.textView_LabelArtists);
                tvLabelGroups  = v.findViewById(R.id.textView_LabelGroups);
                tvLabelLanguages = v.findViewById(R.id.textView_LabelLanguages);
                tvLabelCategories = v.findViewById(R.id.textView_LabelCategories);
                tvLabelPages = v.findViewById(R.id.textView_LabelPages);


            }
        }

        public RecyclerViewComicPagesAdapter(TreeMap<Integer, String> data) {
            this.treeMap = data;
            mapKeys = treeMap.keySet().toArray(new Integer[getCount()]);
        }

        public int getCount() {
            return treeMap.size();
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
        public ComicDetailsActivity.RecyclerViewComicPagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                                             int viewType) {


            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            if (viewType == ITEM_VIEW_TYPE_HEADER) {
                View headerView = inflater.inflate(R.layout.activity_comic_details_header, parent, false);
                return new ComicDetailsActivity.RecyclerViewComicPagesAdapter.ViewHolder(headerView);
            }

            // create a new view
            View v;

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                v = inflater.inflate(R.layout.recycler_comics_grid, parent, false);
            } else {
                v = inflater.inflate(R.layout.recycler_comics_row, parent, false);
            }

            return new ComicDetailsActivity.RecyclerViewComicPagesAdapter.ViewHolder(v);
        }







        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ComicDetailsActivity.RecyclerViewComicPagesAdapter.ViewHolder holder, final int position) {
            // - get element from your data set at this position
            // - replace the contents of the view with that element




            String sThumbnailText;

            if (globalClass.ObfuscationOn) {

                if (isHeader(position)) {
                    holder.tvThumbnailText.setVisibility(View.INVISIBLE);
                    holder.tvComicID.setVisibility(View.INVISIBLE);
                    holder.tvParodies.setVisibility(View.INVISIBLE);
                    holder.tvCharacters.setVisibility(View.INVISIBLE);
                    holder.tvTags.setVisibility(View.INVISIBLE);
                    holder.tvArtists.setVisibility(View.INVISIBLE);
                    holder.tvGroups.setVisibility(View.INVISIBLE);
                    holder.tvLanguages.setVisibility(View.INVISIBLE);
                    holder.tvCategories.setVisibility(View.INVISIBLE);
                    holder.tvPages.setVisibility(View.INVISIBLE);

                    holder.tvLabelComicID.setVisibility(View.INVISIBLE);
                    holder.tvLabelParodies.setVisibility(View.INVISIBLE);
                    holder.tvLabelCharacters.setVisibility(View.INVISIBLE);
                    holder.tvLabelTags.setVisibility(View.INVISIBLE);
                    holder.tvLabelArtists.setVisibility(View.INVISIBLE);
                    holder.tvLabelGroups.setVisibility(View.INVISIBLE);
                    holder.tvLabelLanguages.setVisibility(View.INVISIBLE);
                    holder.tvLabelCategories.setVisibility(View.INVISIBLE);
                    holder.tvLabelPages.setVisibility(View.INVISIBLE);
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
                    holder.tvComicID.setVisibility(View.VISIBLE);
                    holder.tvParodies.setVisibility(View.VISIBLE);
                    holder.tvTags.setVisibility(View.VISIBLE);
                    holder.tvArtists.setVisibility(View.VISIBLE);
                    holder.tvLanguages.setVisibility(View.VISIBLE);
                    holder.tvCategories.setVisibility(View.VISIBLE);
                    holder.tvPages.setVisibility(View.VISIBLE);

                    holder.tvLabelComicID.setVisibility(View.VISIBLE);
                    holder.tvLabelParodies .setVisibility(View.VISIBLE);
                    holder.tvLabelTags.setVisibility(View.VISIBLE);
                    holder.tvLabelArtists.setVisibility(View.VISIBLE);
                    holder.tvLabelLanguages.setVisibility(View.VISIBLE);
                    holder.tvLabelCategories.setVisibility(View.VISIBLE);
                    holder.tvLabelPages.setVisibility(View.VISIBLE);

                    sThumbnailText = gsComicFields[GlobalClass.COMIC_NAME_INDEX];
                    holder.tvComicID.setText(gsComicFields[GlobalClass.COMIC_ID_INDEX]);
                    holder.tvParodies.setText(gsComicFields[GlobalClass.COMIC_PARODIES_INDEX]);
                    holder.tvCharacters.setText(gsComicFields[GlobalClass.COMIC_CHARACTERS_INDEX]);
                    holder.tvTags.setText(gsComicFields[GlobalClass.COMIC_TAGS_INDEX]);
                    holder.tvArtists.setText(gsComicFields[GlobalClass.COMIC_ARTISTS_INDEX]);
                    holder.tvGroups.setText(gsComicFields[GlobalClass.COMIC_GROUPS_INDEX]);
                    holder.tvLanguages.setText(gsComicFields[GlobalClass.COMIC_LANGUAGES_INDEX]);
                    holder.tvCategories.setText(gsComicFields[GlobalClass.COMIC_CATEGORIES_INDEX]);
                    holder.tvPages.setText(gsComicFields[GlobalClass.COMIC_PAGES_INDEX]);
                } else {
                    sThumbnailText = String.format("Page %d of %d", position + 1, getItemCount());  //Position is 0-based.
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

        }


    }


    //=====================================================================================
    //===== Comic Viewer Code =================================================================
    //=====================================================================================

    public void StartComicViewerActivity(int iComicPage){
        Intent intentComicViewer = new Intent(this, ComicPageViewerActivity.class);

        //intentComicViewer.putExtra(ComicPageViewerActivity.COMIC_FIELDS_STRING, gsComicFields);
        intentComicViewer.putExtra(ComicPageViewerActivity.COMIC_PAGE_START, iComicPage);

        //Record the COMIC_DATETIME_LAST_READ_BY_USER:
        Double dTimeStamp = globalClass.GetTimeStampFloat();
        String[] sDateTime = new String[]{dTimeStamp.toString()};
        int[] iFields = new int[]{GlobalClass.COMIC_DATETIME_LAST_READ_BY_USER_INDEX};
        globalClass.ComicCatalogDataFile_UpdateRecord(
                gsComicFields[GlobalClass.COMIC_ID_INDEX],
                iFields,
                sDateTime);

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
        setTitle(gsComicFields[GlobalClass.COMIC_NAME_INDEX]);

        //Update the RecyclerView:
        gRecyclerViewComicPagesAdapter.notifyDataSetChanged();
    }

    //=====================================================================================
    //===== Data Update Code =================================================================
    //=====================================================================================


    public void SyncOnlineData(){

        if(GlobalClass.isNetworkConnected) {

            Intent intentGetComicDetails;

            intentGetComicDetails = new Intent(this, ComicDetailsDataService.class);
            intentGetComicDetails.putExtra(ComicDetailsDataService.COMIC_DETAILS_COMIC_ID,
                    gsComicFields[GlobalClass.COMIC_ID_INDEX]);

            gmiGetOnlineData.setEnabled(false);

            Toast.makeText(getApplicationContext(), "Getting online data...", Toast.LENGTH_LONG).show();

            startService(intentGetComicDetails);
        } else {
            Toast.makeText(getApplicationContext(), "No network connected.", Toast.LENGTH_LONG).show();
        }
    }

    public class ComicDetailsResponseReceiver extends BroadcastReceiver {
        public static final String COMIC_DETAILS_DATA_ACTION_RESPONSE = "com.dabnoot.intent.action.FROM_COMIC_DETAILS_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean bComicDetailsDataServiceSuccess;
            bComicDetailsDataServiceSuccess = intent.getBooleanExtra(ComicDetailsDataService.COMIC_DETAILS_SUCCESS,
                                                         false);

            String sErrorMessage;
            if(bComicDetailsDataServiceSuccess) {
                if(intent.getBooleanExtra(ComicDetailsDataService.COMIC_DETAILS_COMIC_TITLE_ACQUIRED,false)){
                    gbComicDetailsTitleUpdateAvailable = true;
                    gsComicFields[GlobalClass.COMIC_NAME_INDEX] = intent.getStringExtra(ComicDetailsDataService.COMIC_DETAILS_COMIC_TITLE);
                }

                if(intent.getBooleanExtra(ComicDetailsDataService.COMIC_DETAILS_PARODIES_DATA_ACQUIRED,false)){
                    gbComicDetailsParodiesDataUpdateAvailable = true;
                    gsComicFields[GlobalClass.COMIC_PARODIES_INDEX] = intent.getStringExtra(ComicDetailsDataService.COMIC_DETAILS_PARODIES_DATA);
                }

                if(intent.getBooleanExtra(ComicDetailsDataService.COMIC_DETAILS_CHARACTERS_DATA_ACQUIRED,false)){
                    gbComicDetailsCharactersDataUpdateAvailable = true;
                    gsComicFields[GlobalClass.COMIC_CHARACTERS_INDEX] = intent.getStringExtra(ComicDetailsDataService.COMIC_DETAILS_CHARACTERS_DATA);
                }

                if(intent.getBooleanExtra(ComicDetailsDataService.COMIC_DETAILS_TAGS_DATA_ACQUIRED,false)){
                    gbComicDetailsTagsDataUpdateAvailable = true;
                    gsComicFields[GlobalClass.COMIC_TAGS_INDEX] = intent.getStringExtra(ComicDetailsDataService.COMIC_DETAILS_TAGS_DATA);
                }

                if(intent.getBooleanExtra(ComicDetailsDataService.COMIC_DETAILS_ARTISTS_DATA_ACQUIRED,false)){
                    gbComicDetailsArtistsDataUpdateAvailable = true;
                    gsComicFields[GlobalClass.COMIC_ARTISTS_INDEX] = intent.getStringExtra(ComicDetailsDataService.COMIC_DETAILS_ARTISTS_DATA);
                }

                if(intent.getBooleanExtra(ComicDetailsDataService.COMIC_DETAILS_GROUPS_DATA_ACQUIRED,false)){
                    gbComicDetailsGroupsDataUpdateAvailable = true;
                    gsComicFields[GlobalClass.COMIC_GROUPS_INDEX] = intent.getStringExtra(ComicDetailsDataService.COMIC_DETAILS_GROUPS_DATA);
                }

                if(intent.getBooleanExtra(ComicDetailsDataService.COMIC_DETAILS_LANGUAGES_DATA_ACQUIRED,false)){
                    gbComicDetailsLanguagesDataUpdateAvailable = true;
                    gsComicFields[GlobalClass.COMIC_LANGUAGES_INDEX] = intent.getStringExtra(ComicDetailsDataService.COMIC_DETAILS_LANGUAGES_DATA);
                }

                if(intent.getBooleanExtra(ComicDetailsDataService.COMIC_DETAILS_CATEGORIES_DATA_ACQUIRED,false)){
                    gbComicDetailsCategoriesDataUpdateAvailable = true;
                    gsComicFields[GlobalClass.COMIC_CATEGORIES_INDEX] = intent.getStringExtra(ComicDetailsDataService.COMIC_DETAILS_CATEGORIES_DATA);
                }

                if(intent.getBooleanExtra(ComicDetailsDataService.COMIC_DETAILS_PAGES_DATA_ACQUIRED,false)){
                    gbComicDetailsPagesDataUpdateAvailable = true;
                    gsComicFields[GlobalClass.COMIC_PAGES_INDEX] = intent.getStringExtra(ComicDetailsDataService.COMIC_DETAILS_PAGES_DATA);
                }

                gmiSaveDetails.setEnabled(true);

                //Update the RecyclerView:
                gRecyclerViewComicPagesAdapter.notifyDataSetChanged();
                if(gbAutoAcquireData){
                    Toast.makeText(getApplicationContext(), "Online data acquired. Auto saving...", Toast.LENGTH_LONG).show();
                    SaveDetails();
                } else {
                    Toast.makeText(getApplicationContext(), "Online data acquired. Don't forget to save.", Toast.LENGTH_LONG).show();
                }
            } else {
                sErrorMessage = intent.getStringExtra(ComicDetailsDataService.COMIC_DETAILS_ERROR_MESSAGE);
                Toast.makeText(getApplicationContext(), "Error getting data online.\n" + sErrorMessage, Toast.LENGTH_LONG).show();
            }

            gmiGetOnlineData.setEnabled(true);

        }
    }


    public void SaveDetails(){

        boolean[] bFieldUpdateBooleans = new boolean[]{
                gbComicDetailsTitleUpdateAvailable,
                gbComicDetailsParodiesDataUpdateAvailable,
                gbComicDetailsCharactersDataUpdateAvailable,
                gbComicDetailsTagsDataUpdateAvailable,
                gbComicDetailsArtistsDataUpdateAvailable,
                gbComicDetailsGroupsDataUpdateAvailable,
                gbComicDetailsLanguagesDataUpdateAvailable,
                gbComicDetailsCategoriesDataUpdateAvailable,
                gbComicDetailsPagesDataUpdateAvailable
        };

        String[] sFieldUpdateText = new String[]{
                gsComicFields[GlobalClass.COMIC_NAME_INDEX],
                gsComicFields[GlobalClass.COMIC_PARODIES_INDEX],
                gsComicFields[GlobalClass.COMIC_CHARACTERS_INDEX],
                gsComicFields[GlobalClass.COMIC_TAGS_INDEX],
                gsComicFields[GlobalClass.COMIC_ARTISTS_INDEX],
                gsComicFields[GlobalClass.COMIC_GROUPS_INDEX],
                gsComicFields[GlobalClass.COMIC_LANGUAGES_INDEX],
                gsComicFields[GlobalClass.COMIC_CATEGORIES_INDEX],
                gsComicFields[GlobalClass.COMIC_PAGES_INDEX]
        };

        int[] iPossibleFieldIDs = new int[]{
                GlobalClass.COMIC_NAME_INDEX,
                GlobalClass.COMIC_PARODIES_INDEX,
                GlobalClass.COMIC_CHARACTERS_INDEX,
                GlobalClass.COMIC_TAGS_INDEX,
                GlobalClass.COMIC_ARTISTS_INDEX,
                GlobalClass.COMIC_GROUPS_INDEX,
                GlobalClass.COMIC_LANGUAGES_INDEX,
                GlobalClass.COMIC_CATEGORIES_INDEX,
                GlobalClass.COMIC_PAGES_INDEX
        };

        ArrayList<Integer> iFieldIDs = new ArrayList<>();
        ArrayList<String> sFieldUpdateData = new ArrayList<>();

        for(int i = 0; i< bFieldUpdateBooleans.length; i++){
            if(bFieldUpdateBooleans[i]){
                iFieldIDs.add(iPossibleFieldIDs[i]);
                sFieldUpdateData.add(sFieldUpdateText[i]);
            }
        }

        int[] iTemp = new int[iFieldIDs.size()];
        String[] sTemp = new String[sFieldUpdateData.size()];
        for(int i = 0; i < iFieldIDs.size(); i++){
            iTemp[i] = iFieldIDs.get(i);
            sTemp[i] = sFieldUpdateData.get(i);
        }

        globalClass.ComicCatalogDataFile_UpdateRecord(gsComicFields[GlobalClass.COMIC_ID_INDEX],
                iTemp, sTemp);

        gmiSaveDetails.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Data saved.", Toast.LENGTH_LONG).show();

    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(gComicDetailsResponseReceiver);
        super.onDestroy();
    }





}