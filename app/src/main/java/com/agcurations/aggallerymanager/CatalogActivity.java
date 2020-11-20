package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CatalogActivity extends AppCompatActivity {

    //Global Constants:
    private static final String LOG_TAG = "CatalogActivity";

    //Global Variables:
    private GlobalClass globalClass;
    private int giMediaCategory;
    private RecyclerView.Adapter<RecyclerViewCatalogAdapter.ViewHolder> gRecyclerViewCatalogAdapter;
    private final boolean gbDebugTouch = false;
    RecyclerView gRecyclerView;
    private boolean gbRecyclerViewFiltered;
    private String gsFilterText;
    private boolean gbCatalogTagsRestrictionsOn;
    Spinner gspSpinnerSort;
    //private int giRecyclerViewDefaultSortBySetting;
    private int giRecyclerViewSortBySetting;
    private boolean gbRecyclerViewSortAscending = true;

    private Menu ActivityMenu;


    private final int[] giDataRecordIDIndexes = {
            GlobalClass.VIDEO_ID_INDEX,
            GlobalClass.IMAGE_ID_INDEX,
            GlobalClass.COMIC_ID_INDEX};

    private final int[] giDataRecordDateTimeImportIndexes = {
            GlobalClass.VIDEO_DATETIME_IMPORT_INDEX,
            GlobalClass.IMAGE_DATETIME_IMPORT_INDEX,
            GlobalClass.COMIC_DATETIME_IMPORT_INDEX};

    private final int[] giDataRecordDateTimeViewedIndexes = {
            GlobalClass.VIDEO_DATETIME_LAST_VIEWED_BY_USER_INDEX,
            GlobalClass.IMAGE_DATETIME_LAST_VIEWED_BY_USER_INDEX,
            GlobalClass.COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX};

    private final int[] giDataRecordTagsIndexes = {
            GlobalClass.VIDEO_TAGS_INDEX,
            GlobalClass.IMAGE_TAGS_INDEX,
            GlobalClass.COMIC_TAGS_INDEX};

    private final int[] giDataRecordFolderIndexes = {
            GlobalClass.VIDEO_FOLDER_NAME_INDEX,
            GlobalClass.IMAGE_FOLDER_NAME_INDEX,
            GlobalClass.COMIC_FOLDER_NAME_INDEX}; //The record index to find the item's folder.

    private final int[] giDataRecordRecylerViewImageIndexes = {
            GlobalClass.VIDEO_FILENAME_INDEX,
            GlobalClass.IMAGE_FILENAME_INDEX,
            GlobalClass.COMIC_THUMBNAIL_FILE_INDEX};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.MainTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        Intent intent = getIntent();
        giMediaCategory = intent.getIntExtra("MEDIA_CATEGORY", -1);

        if(globalClass.ObfuscationOn) {
            setTitle(globalClass.getObfuscatedProgramName());
        } else {
            setTitle(globalClass.sNonObfustatedProgramName[giMediaCategory]);
        }

        //Update TextView to show 0 catalog items if applicable:
        notifyZeroCatalogItemsIfApplicable();

        //Get tag restrictions preferences:
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> ssCatalogTagsRestricted = sharedPreferences.getStringSet("multi_select_list_restricted_tags", null);
        //Attempt to match the restricted tag text from the preferences to the Tag ID:
        if(ssCatalogTagsRestricted != null) {
            String s;
            for (String sRestrictedTag : ssCatalogTagsRestricted) {
                for (Map.Entry<Integer, String[]> entry : globalClass.gtmCatalogTagReferenceLists.get(giMediaCategory).entrySet()) {
                    s = entry.getValue()[GlobalClass.TAG_NAME_INDEX];
                    if (sRestrictedTag.equals(s)) {
                        //If the restricted tag has been found, assign it to the restricted tags TreeMap:
                        globalClass.gtmCatalogTagsRestricted.get(giMediaCategory).put(entry.getKey(), entry.getValue()[GlobalClass.TAG_NAME_INDEX]);
                    }
                }
            }
        }

        gbCatalogTagsRestrictionsOn = sharedPreferences.getBoolean("hide_restricted_tags", false);

        gRecyclerView = findViewById(R.id.RecyclerView_CatalogItems);
        configure_RecyclerViewCatalogItems();

        gsFilterText = "";

        if(globalClass.connectivityManager == null){
            globalClass.registerNetworkCallback();
            //This lets us check globalClass.isNetworkConnected to see if we are connected to the
            //network;
        }

        //See additional initialization in onCreateOptionsMenu().
    }

    public void notifyZeroCatalogItemsIfApplicable(){

        //Update TextView to show 0 items if applicable:
        TextView tvCatalogStatus = findViewById(R.id.textView_CatalogStatus);
        if (globalClass.gtmCatalogLists.get(giMediaCategory).size() == 0 ) {
            tvCatalogStatus.setVisibility(View.VISIBLE);
            String s = "Catalog contains 0 items.";
            tvCatalogStatus.setText(s);
        } else {
            tvCatalogStatus.setVisibility(View.INVISIBLE);
        }

    }

    public static int SPINNER_ITEM_IMPORT_DATE = 0;
    public static int SPINNER_ITEM_LAST_VIEWED_DATE = 1;
    String[] gsSpinnerItems={"Import Date","Last Read Date"};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        ActivityMenu = menu;

        getMenuInflater().inflate(R.menu.catalog_action_bar, menu);

        //Set the restricted tags lock icon as appropriate:
        MenuItem restrictedItem = menu.findItem(R.id.icon_tags_restricted);
        if(gbCatalogTagsRestrictionsOn){
            restrictedItem.setIcon(R.drawable.baseline_lock_white_18dp);
        } else {
            restrictedItem.setIcon(R.drawable.baseline_lock_open_white_18dp);
        }

        // Initialise menu item search bar with id and take its object
        //https://www.geeksforgeeks.org/android-searchview-with-example/
        MenuItem searchViewItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) searchViewItem.getActionView();
        // attach setOnQueryTextListener to search view defined above
        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    // Override onQueryTextSubmit method
                    @Override
                    public boolean onQueryTextSubmit(String sQuery)
                    {
                        gsFilterText = sQuery;
                        populate_RecyclerViewCatalogItems();
                        Toast.makeText(getApplicationContext(), "Showing " + gRecyclerViewCatalogAdapter.getItemCount() + " items.", Toast.LENGTH_SHORT).show();
                        gbRecyclerViewFiltered = true;
                        return false;
                    }
                    @Override
                    public boolean onQueryTextChange(String sQuery) {
                        gsFilterText = sQuery;
                        return false;
                    }
                });
        //Set a listener for the "cancel search" button:
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                if(gbRecyclerViewFiltered) {
                    populate_RecyclerViewCatalogItems();
                    Toast.makeText(getApplicationContext(), "Showing " + gRecyclerViewCatalogAdapter.getItemCount() + " items.", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        //Configure the 'Sort by' selection spinner:
        MenuItem miSpinnerSort = menu.findItem(R.id.spinner_sort);
        gspSpinnerSort =(Spinner) miSpinnerSort.getActionView();
        //wrap the items in the Adapter
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this, R.layout.catalog_action_bar_spinner_item, gsSpinnerItems);
        //assign adapter to the Spinner
        gspSpinnerSort.setAdapter(adapter);

        //Initialize the spinner position:
        //This is here because when onResume hits when the activity is first created,
        //  the Spinner does not yet exist.
        if(giRecyclerViewSortBySetting == giDataRecordDateTimeImportIndexes[giMediaCategory]){
            gspSpinnerSort.setSelection(SPINNER_ITEM_IMPORT_DATE);
            //When sorting by import date, sort Descending by default (newest first):
            SetSortIconToDescending();
        } else if(giRecyclerViewSortBySetting == giDataRecordDateTimeViewedIndexes[giMediaCategory]){
            gspSpinnerSort.setSelection(SPINNER_ITEM_LAST_VIEWED_DATE);
            //When sorting by last viewed, sort Ascending by default:
            SetSortIconToAscending();
        }

        //Continue with configuring the spinner:
        gspSpinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == SPINNER_ITEM_IMPORT_DATE) {
                    giRecyclerViewSortBySetting = giDataRecordDateTimeImportIndexes[giMediaCategory];
                } else if(position == SPINNER_ITEM_LAST_VIEWED_DATE) {
                    giRecyclerViewSortBySetting = giDataRecordDateTimeViewedIndexes[giMediaCategory];
                }
                populate_RecyclerViewCatalogItems();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // no need to code here
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Display a message showing the name of the item selected.
        int itemID = item.getItemId();

        if(itemID == R.id.icon_tags_restricted){
            if(gbCatalogTagsRestrictionsOn){
                //If restrictions are on, ask for pin code before unlocking.
                Intent intentPinCodeAccessSettings = new Intent(this, PinCodePopup.class);
                startActivityForResult(intentPinCodeAccessSettings, PinCodePopup.START_ACTIVITY_FOR_RESULT_UNLOCK_RESTRICTED_TAGS);
            } else {
                //If restrictions are off...
                //Turn on restrictions, hide items, set icon to show lock symbol
                gbCatalogTagsRestrictionsOn = true;
                SetRestrictedIconToLock();
                //Repopulate the catalog list:
                //populate_RecyclerViewCatalogItems(globalClass.gtmCatalogLists.get(giMediaCategory));
                populate_RecyclerViewCatalogItems();
                Toast.makeText(getApplicationContext(), "Showing " + gRecyclerViewCatalogAdapter.getItemCount() + " items.", Toast.LENGTH_SHORT).show();
            }

        } else if(itemID == R.id.icon_sort_order){
            if( gbRecyclerViewSortAscending) {
                SetSortIconToDescending();
            } else {
                SetSortIconToAscending();
            }
            populate_RecyclerViewCatalogItems();

        } else if(itemID == R.id.menu_FlipView){
            FlipObfuscation();

        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        try {
            super.onActivityResult(requestCode, resultCode, resultData);

            if(requestCode == PinCodePopup.START_ACTIVITY_FOR_RESULT_UNLOCK_RESTRICTED_TAGS){
                if(resultCode == RESULT_OK){
                    //Show catalog items with restricted tags.
                    //Change the lock icon to 'unlocked':
                    SetRestrictedIconToUnlock();
                    //Set the flag:
                    gbCatalogTagsRestrictionsOn = false;
                    //Repopulate the catalog list:
                    populate_RecyclerViewCatalogItems();
                    Toast.makeText(getApplicationContext(), "Showing " + gRecyclerViewCatalogAdapter.getItemCount() + " items.", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception ex) {
            Context context = getApplicationContext();
            Toast.makeText(context, ex.toString(),
                    Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, ex.toString());
        }

    }


    private void SetRestrictedIconToLock(){
        MenuItem item = ActivityMenu.findItem(R.id.icon_tags_restricted);
        item.setIcon(R.drawable.baseline_lock_white_18dp);
    }

    private void SetRestrictedIconToUnlock(){
        MenuItem item = ActivityMenu.findItem(R.id.icon_tags_restricted);
        item.setIcon(R.drawable.baseline_lock_open_white_18dp);
    }

    private void SetSortIconToAscending(){
        MenuItem item = ActivityMenu.findItem(R.id.icon_sort_order);
        item.setIcon(R.drawable.baseline_sort_ascending_white_18dp);
        gbRecyclerViewSortAscending = true;
    }

    private void SetSortIconToDescending(){
        MenuItem item = ActivityMenu.findItem(R.id.icon_sort_order);
        item.setIcon(R.drawable.baseline_sort_descending_white_18dp);
        gbRecyclerViewSortAscending = false;
    }



    //=====================================================================================
    //===== RecyclerView Code =================================================================
    //=====================================================================================

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
            gridLayoutManager = new GridLayoutManager(this, 4, RecyclerView.VERTICAL, false);
            gRecyclerView.setLayoutManager(gridLayoutManager);
        } else {
            // In portrait
            // use a linear layout manager
            layoutManager = new LinearLayoutManager(this);
            gRecyclerView.setLayoutManager(layoutManager);
        }

    }

    public class RecyclerViewCatalogAdapter extends RecyclerView.Adapter<RecyclerViewCatalogAdapter.ViewHolder> {

        private final TreeMap<Integer, String[]> treeMap;
        private final Integer[] mapKeys;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView ivThumbnail;
            public final TextView tvThumbnailText;
            public final TextView tvDetails;

            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.ImageView_Thumbnail);
                tvThumbnailText = v.findViewById(R.id.editText_Title);
                tvDetails = v.findViewById(R.id.TextView_Details);
            }
        }

        public RecyclerViewCatalogAdapter(TreeMap<Integer, String[]> data) {
            this.treeMap = data;
            mapKeys = treeMap.keySet().toArray(new Integer[getCount()]);
        }

        public int getCount() {
            return treeMap.size();
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public RecyclerViewCatalogAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                        int viewType) {
            // create a new view
            View v;

            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS){
                    v = inflater.inflate(R.layout.recycler_catalog_grid_videos, parent, false);
                } else {
                    v = inflater.inflate(R.layout.recycler_catalog_grid, parent, false);
                }
            } else {
                v = inflater.inflate(R.layout.recycler_catalog_row, parent, false);
            }

            return new RecyclerViewCatalogAdapter.ViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull RecyclerViewCatalogAdapter.ViewHolder holder, final int position) {
            // - get element from your data set at this position
            // - replace the contents of the view with that element

            //Get the data for the row:
            String[] sFields;
            sFields = treeMap.get(mapKeys[position]);
            if (sFields == null) {
                sFields = new String[GlobalClass.CatalogRecordFields[giMediaCategory].length]; //To prevent possible null pointer exception later.
            }
            final String[] sFields_final = sFields;


            if(globalClass.ObfuscationOn) {

                //Get the obfuscation image index:
                int i = (position % globalClass.getObfuscationImageCount());
                //Get the obfuscation image resource ID:
                int iObfuscatorResourceID = globalClass.getObfuscationImage(i);

                Bitmap bmObfuscator = BitmapFactory.decodeResource(getResources(), iObfuscatorResourceID);
                holder.ivThumbnail.setImageBitmap(bmObfuscator);
                holder.tvThumbnailText.setText(globalClass.getObfuscationImageText(i));
            } else {

                //Load the non-obfuscated image into the RecyclerView ViewHolder:
                String sThumbnailFilePath = globalClass.gfCatalogFolders[giMediaCategory].getAbsolutePath() + File.separator
                        + sFields[giDataRecordFolderIndexes[giMediaCategory]] + File.separator
                        + sFields[giDataRecordRecylerViewImageIndexes[giMediaCategory]];
                File fThumbnail = new File(sThumbnailFilePath);

                if (fThumbnail.exists()) {
                    Glide.with(getApplicationContext()).load(fThumbnail).into(holder.ivThumbnail);
                }

                String sThumbnailText = "";
                switch(giMediaCategory){
                    case GlobalClass.MEDIA_CATEGORY_VIDEOS:
                        sThumbnailText = globalClass.JumbleFileName(sFields[GlobalClass.VIDEO_FILENAME_INDEX]) + ", " +
                                sFields[GlobalClass.VIDEO_DURATION_TEXT_INDEX];
                        break;
                    case GlobalClass.MEDIA_CATEGORY_IMAGES:
                        sThumbnailText = globalClass.JumbleFileName(sFields[GlobalClass.IMAGE_FILENAME_INDEX]) + ", " +
                                sFields[GlobalClass.IMAGE_TAGS_INDEX];
                        break;
                    case GlobalClass.MEDIA_CATEGORY_COMICS:
                        sThumbnailText = sFields[GlobalClass.COMIC_NAME_INDEX];
                        //sThumbnailText = GlobalClass.ConvertDoubleTimeStampToString(sFields[GlobalClass.COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX]);
                        break;
                }
                holder.tvThumbnailText.setText(sThumbnailText);

            }


            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(gbDebugTouch) Toast.makeText(getApplicationContext(),"Click Item Number " + position, Toast.LENGTH_LONG).show();


                    if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                        StartVideoPlayerActivity(treeMap, Integer.parseInt(sFields_final[GlobalClass.VIDEO_ID_INDEX]));

                    } else if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                        StartComicViewerActivity(sFields_final);

                    }
                }
            });

            holder.ivThumbnail.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(gbDebugTouch) Toast.makeText(getApplicationContext(), "Long press detected", Toast.LENGTH_SHORT).show();
                    Obfuscate();
                    return true;// returning true instead of false, works for me
                }
            });

        }

        // Return the size of the data set (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return treeMap.size();
        }

    }

    public void populate_RecyclerViewCatalogItems(){

        //Apply the sort field.
        //Copy over only items that match a filter, if applied.
        //Copy over only non-restricted catalog items, if necessary.
        //Sort the TreeMap.

        //Create new TreeMap to presort the catalog items:
        TreeMap<String, String[]> treeMapPreSort; //String = field being sorted, String = Catalog item data
        treeMapPreSort = new TreeMap<>();

        //Format the restriction tag set so that we can process it:
        StringBuilder sbRestrictedTags = new StringBuilder();
        for(Map.Entry<Integer, String> entry: globalClass.gtmCatalogTagsRestricted.get(giMediaCategory).entrySet()){
            sbRestrictedTags.append(entry.getValue());
            sbRestrictedTags.append(",");
        }
        //Split restricted tags into array:
        String[] sTagsArray = new String[]{};
        if(sbRestrictedTags.toString().contains(",")) {
            //If there is at least 1 restricted tag...
            sTagsArray = sbRestrictedTags.toString().split(",");
        }

        //Populate the Key field of the preSort TreeMap with SortBy field data, filtered and restricted if necessary:
        String[] sCatalogListRecord;
        String sKey;
        for (Map.Entry<Integer, String[]>
                entry : globalClass.gtmCatalogLists.get(giMediaCategory).entrySet()) {
            sCatalogListRecord = entry.getValue();

            //Create a unique key to identify the record in the TreeMap, which includes
            // the SortBy field. TreeMap automatically sorts by the Key field.
            sKey = sCatalogListRecord[giRecyclerViewSortBySetting];
            sKey = sKey + sCatalogListRecord[giDataRecordIDIndexes[giMediaCategory]];

            //Apply a filter if requested - build a string out of the records contents, and if a
            //  filter is to be applied, check for a match. If no match, don't add the record to
            //  the TreeMap destined for the RecyclerView:
            boolean bIsFilterMatch = true;
            if(!gsFilterText.equals("")) {
                StringBuilder sbRecordText;
                String sFilterText_LowerCase = gsFilterText.toLowerCase();
                String sKey_RecordText;

                sbRecordText = new StringBuilder();
                for (int i = 0; i < GlobalClass.CatalogRecordFields[giMediaCategory].length; i++) {
                    sbRecordText.append(sCatalogListRecord[i]);
                }
                sKey_RecordText = sbRecordText.toString().toLowerCase();

                if (!sKey_RecordText.contains(sFilterText_LowerCase)) {
                    bIsFilterMatch = false;
                }
            }

            //Check to see if the record needs to be skipped due to restriction settings:
            boolean bIsRestricted = false;
            if(gbCatalogTagsRestrictionsOn &&
                    (sTagsArray.length > 0)) {
                //If restricted tags exist...
                //Look for restricted tags in the incoming record:
                for (String s : sTagsArray) {
                    if (sCatalogListRecord[giDataRecordTagsIndexes[giMediaCategory]].contains(s.trim())) {
                        bIsRestricted = true;
                    }
                }
            }

            if(bIsFilterMatch && !bIsRestricted){
                treeMapPreSort.put(sKey, sCatalogListRecord);
            }

        }

        //TreeMap presort will auto-sort itself.

        //Clean up the key, apply a reverse sort order, if applicable:
        TreeMap<Integer, String[]> tmNewOrderCatalogList = new TreeMap<>();
        int iRID, iIterator;
        if(gbRecyclerViewSortAscending){
            iRID = 0;
            iIterator = 1;
        } else {
            iRID = treeMapPreSort.size();
            iIterator = -1;
        }

        for (Map.Entry<String, String[]>
                entry : treeMapPreSort.entrySet()) {
            sCatalogListRecord = entry.getValue();
            tmNewOrderCatalogList.put(iRID, sCatalogListRecord);
            iRID += iIterator;
        }


        //Apply the new TreeMap to the RecyclerView:
        gRecyclerViewCatalogAdapter = new RecyclerViewCatalogAdapter(tmNewOrderCatalogList);
        gRecyclerView.setAdapter(gRecyclerViewCatalogAdapter);


    }


    //=====================================================================================
    //===== Player/Viewer Code =================================================================
    //=====================================================================================

    public final static String RECYCLERVIEW_VIDEO_TREEMAP_FILTERED = "RECYCLERVIEW_VIDEO_TREEMAP_FILTERED";
    public final static String RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_KEY = "RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_KEY";
    private void StartVideoPlayerActivity(TreeMap<Integer, String[]> treeMap, int iKey) {
        //Key is the TreeMap Key for the selected video.

        Intent intentVideoPlayer = new Intent(this, VideoPlayerActivityFullScreen.class);
        intentVideoPlayer.putExtra(RECYCLERVIEW_VIDEO_TREEMAP_FILTERED, treeMap);
        intentVideoPlayer.putExtra(RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_KEY, iKey);
        startActivity(intentVideoPlayer);
    }

    public void StartComicViewerActivity(String[] sFields){

        Intent intentComicViewer = new Intent(this, ComicDetailsActivity.class);

        intentComicViewer.putExtra(ComicDetailsActivity.EXTRA_COMIC_FIELDS_STRING, sFields);

        startActivity(intentComicViewer);
    }


    //=====================================================================================
    //===== Obfuscation Code ==============================================================
    //=====================================================================================

    @Override
    public void onResume(){
        super.onResume();

        //Apply a resort.
        //If a comic was just deleted, or a comic was just read, the view needs to be updated.
        //  This includes removing a comic, or moving a comic to the end of the RecyclerView.

        //Change spinner position if we have just come in from an import operation.
        //  The user will want to see that the item they just imported has made it into the
        //  catalog.
        if(globalClass.gbJustImported[giMediaCategory]) {
            //Set sort by to "import_datetime"
            giRecyclerViewSortBySetting = giDataRecordDateTimeImportIndexes[giMediaCategory];
            globalClass.gbJustImported[giMediaCategory] = false;
        } else {
            //Set sort by to "viewed_datetime"
            giRecyclerViewSortBySetting = giDataRecordDateTimeViewedIndexes[giMediaCategory];
        }

        populate_RecyclerViewCatalogItems();
        Toast.makeText(getApplicationContext(), "Showing " + gRecyclerViewCatalogAdapter.getItemCount() + " items.", Toast.LENGTH_SHORT).show();

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
        setTitle(globalClass.getObfuscatedProgramName());
        //Update the RecyclerView:
        gRecyclerViewCatalogAdapter.notifyDataSetChanged();
    }

    public void RemoveObfuscation(){
        //Remove obfuscation:
        setTitle(globalClass.sNonObfustatedProgramName[giMediaCategory]);
        //Update the RecyclerView:
        gRecyclerViewCatalogAdapter.notifyDataSetChanged();
    }



}