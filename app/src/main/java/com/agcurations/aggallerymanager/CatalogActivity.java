package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
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
    private boolean gbCatalogTagsRestrictionsOn;
    Spinner gspSpinnerSort;
    private int giRecyclerViewDefaultSortBySetting;
    private boolean gbRecyclerViewSortAscending = true;

    private int[] giDataRecordIDIndexes = {
            GlobalClass.VIDEO_ID_INDEX,
            GlobalClass.IMAGE_ID_INDEX,
            GlobalClass.COMIC_ID_INDEX};

    private int[] giDataRecordDateTimeImportIndexes = {
            GlobalClass.VIDEO_DATETIME_IMPORT_INDEX,
            GlobalClass.IMAGE_DATETIME_IMPORT_INDEX,
            GlobalClass.COMIC_DATETIME_IMPORT_INDEX};

    private int[] giDataRecordDateTimeViewedIndexes = {
            GlobalClass.VIDEO_DATETIME_LAST_VIEWED_BY_USER_INDEX,
            GlobalClass.IMAGE_DATETIME_LAST_VIEWED_BY_USER_INDEX,
            GlobalClass.COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX};

    private int[] giDataRecordTagsIndexes = {
            GlobalClass.VIDEO_TAGS_INDEX,
            GlobalClass.IMAGE_TAGS_INDEX,
            GlobalClass.COMIC_TAGS_INDEX};

    private int[] giDataRecordFolderIndexes = {
            GlobalClass.VIDEO_FOLDER_NAME_INDEX,
            GlobalClass.IMAGE_FOLDER_NAME_INDEX,
            GlobalClass.COMIC_FOLDER_NAME_INDEX}; //The record index to find the item's folder.

    private int[] giDataRecordRecylerViewImageIndexes = {
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



        gbCatalogTagsRestrictionsOn = sharedPreferences.getBoolean("hide_restricted_tags", false);


        gRecyclerView = findViewById(R.id.RecyclerView_CatalogItems);
        configure_RecyclerViewCatalogItems();

        giRecyclerViewDefaultSortBySetting = giDataRecordDateTimeViewedIndexes[giMediaCategory];

        SetCatalogSortOrderDefault(); //This routine also populates the RecyclerView Adapter.
        //gspSpinnerSort.setOnItemSelected will call SetItemSortOrderDefault after menu load.


        if(globalClass.connectivityManager == null){
            globalClass.registerNetworkCallback();
            //This lets us check globalClass.isNetworkConnected to see if we are connected to the
            //network;
        }

        gRecyclerViewCatalogAdapter.notifyDataSetChanged();

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
    public static int SPINNER_ITEM_LAST_READ_DATE = 1;
    String[] gsSpinnerItems={"Import Date","Last Read Date"};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {


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
                    public boolean onQueryTextSubmit(String query)
                    {
                        AssignCatalogFilter(true, query);
                        gbRecyclerViewFiltered = true;
                        return false;
                    }
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                });
        //Set a listener for the "cancel search" button:
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                if(gbRecyclerViewFiltered) {
                    SetCatalogSortOrderDefault();
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

        //Change spinner position if we have just come in from an import operation.
        //  The user will want to see that the item they just imported has made it into the
        //  catalog.
        if(globalClass.gbJustImported[giMediaCategory]) {
            //Set sort by to "import_datetime"
            giRecyclerViewDefaultSortBySetting = giDataRecordDateTimeImportIndexes[giMediaCategory];
            //Set the sort order to reverse so that the newest appears at the top:
            if( gbRecyclerViewSortAscending) {
                gbRecyclerViewSortAscending = false;
            }
            gspSpinnerSort.setSelection(SPINNER_ITEM_IMPORT_DATE);
            //Get a reference to the sort order icon:
            MenuItem miSortOrder = menu.findItem(R.id.icon_sort_order);
            miSortOrder.setIcon(R.drawable.baseline_sort_descending_white_18dp);
            globalClass.gbJustImported[giMediaCategory] = false;
        }

        //Continue with configuring the spinner:
        gspSpinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == SPINNER_ITEM_IMPORT_DATE) {
                    giRecyclerViewDefaultSortBySetting = giDataRecordDateTimeImportIndexes[giMediaCategory];
                } else if(position == SPINNER_ITEM_LAST_READ_DATE) {
                    giRecyclerViewDefaultSortBySetting = giDataRecordDateTimeViewedIndexes[giMediaCategory];
                }
                SetCatalogSortOrderDefault();
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
        //Toast.makeText(this, "Selected Item: " +item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {

            case R.id.icon_tags_restricted:
                gbCatalogTagsRestrictionsOn = !gbCatalogTagsRestrictionsOn;
                if(gbCatalogTagsRestrictionsOn){
                    item.setIcon(R.drawable.baseline_lock_white_18dp);
                } else {
                    item.setIcon(R.drawable.baseline_lock_open_white_18dp);
                }
                //Repopulate the catalog list:
                populate_RecyclerViewCatalogItems(globalClass.gtmCatalogLists.get(giMediaCategory));
                return true;

            case R.id.icon_sort_order:
                if( gbRecyclerViewSortAscending) {
                    item.setIcon(R.drawable.baseline_sort_descending_white_18dp);
                } else {
                    item.setIcon(R.drawable.baseline_sort_ascending_white_18dp);
                }
                gbRecyclerViewSortAscending = !gbRecyclerViewSortAscending;
                ApplyRecyclerViewSortOrder();
                return true;

            case R.id.menu_FlipView:
                FlipObfuscation();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        //https://developer.android.com/training/data-storage/shared/documents-files
        try {
            super.onActivityResult(requestCode, resultCode, resultData);

        } catch (Exception ex) {
            Context context = getApplicationContext();
            Toast.makeText(context, ex.toString(),
                    Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, ex.toString());
        }


    }

    public class CatalogDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String CATALOG_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_CATALOG_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            /*boolean bCatalogDataChange;
            boolean bError;

            //Get boolean indicating data acquisition was successful:
            bCatalogDataChange = intent.getBooleanExtra(CatalogDataService.EXTRA_BOOL_CATALOG_DATA_CHANGE,false);
            if( bCatalogDataChange) {
                //Update TextView to show 0 comics if applicable:
                notifyZeroComicsIfApplicable();
                gRecyclerViewComicsAdapter.notifyDataSetChanged();
            }

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(CatalogDataService.EXTRA_BOOL_DATA_IMPORT_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(CatalogDataService.EXTRA_STRING_DATA_IMPORT_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            }*/


        }
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
                ivThumbnail = v.findViewById(R.id.ImageView_Thumbnail); //todo
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
                    v = inflater.inflate(R.layout.recycler_catalog_grid_videos, parent, false); //todo
                } else {
                    v = inflater.inflate(R.layout.recycler_catalog_grid, parent, false); //todo
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
                        break;
                }
                holder.tvThumbnailText.setText(sThumbnailText);

            }


            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(gbDebugTouch) Toast.makeText(getApplicationContext(),"Click Item Number " + position, Toast.LENGTH_LONG).show();
                    if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
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

            /*if(holder.tvComicDetails != null) {
                //The landscape version (GridView) does not have a "Comic Details" TextView, so
                //  don't try to set it if this object is null.
                String s = "Comic ID: " + sFields[GlobalClass.COMIC_ID_INDEX];
                holder.tvComicDetails.setText(s);
            }*/
        }

        // Return the size of the data set (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return treeMap.size();
        }

    }

    public void populate_RecyclerViewCatalogItems(TreeMap<Integer, String[]> tmCatalogList){
        gRecyclerViewCatalogAdapter = new RecyclerViewCatalogAdapter(FilterOutRestrictedItems(tmCatalogList));
        gRecyclerView.setAdapter(gRecyclerViewCatalogAdapter);
        Toast.makeText(this, "Showing " + gRecyclerViewCatalogAdapter.getItemCount() + " items.", Toast.LENGTH_SHORT).show();
    }

    public void SetCatalogSortOrderDefault(){
        ChangeSortField(giRecyclerViewDefaultSortBySetting); //TODO, return to default sort.
        gbRecyclerViewFiltered = false;  //Removes filtering.
    }


    public TreeMap<Integer, String[]> FilterOutRestrictedItems(TreeMap<Integer, String[]> tmIncoming){
        TreeMap<Integer, String[]> tmOutgoing = new TreeMap<>();
        boolean bNoData = true;
        if(gbCatalogTagsRestrictionsOn){
            //Format the restriction tag set so that we can process it:
            StringBuilder sbRestrictedTags = new StringBuilder();
            for(Map.Entry<Integer, String> entry: globalClass.gtmCatalogTagsRestricted.get(giMediaCategory).entrySet()){
                sbRestrictedTags.append(entry.getValue());
                sbRestrictedTags.append(",");
            }

            if(sbRestrictedTags.toString().contains(",")) {
                //If there is at least 1 restricted tag...
                //Split restricted tags into array:
                String[] sTagsArray = sbRestrictedTags.toString().split(",");

                if (sTagsArray.length > 0) { //If restricted tags exist...
                    //Look for restricted tags in the incoming treeMap and transfer the entry if
                    //  none are found:
                    String[] sFields;
                    int i = 0;
                    for (Map.Entry<Integer, String[]>
                            entry : tmIncoming.entrySet()) {
                        sFields = entry.getValue();
                        boolean bHasRestrictedTag = false;
                        for (String s : sTagsArray) {

                            if (sFields[giDataRecordTagsIndexes[giMediaCategory]].contains(s.trim())) {
                                bHasRestrictedTag = true;
                                break;
                            }
                        }
                        if (!bHasRestrictedTag) {
                            tmOutgoing.put(i, sFields);
                            i++;
                        }
                    }
                    bNoData = false;

                }
            }
        }
        if(bNoData) {
            return tmIncoming;
        } else {
            return tmOutgoing;
        }


    }



    public void ChangeSortField(int iField){

        //Create new TreeMap to presort the catalog items:
        TreeMap<String, String[]> treeMapPreSort; //String = field being sorted, String = Catalog item data
        treeMapPreSort = new TreeMap<>();

        //Get existing data and load elements into the presorter:
        TreeMap<Integer, String[]> tmCatalogList;
        tmCatalogList = globalClass.gtmCatalogLists.get(giMediaCategory);
        String[] sCatalogListRecord;
        String sKey;

        //If the user has selected 'Sort by last read date', get the oldest read date and apply
        //  that date plus one day to any item that has a "zero" for the last read date.
        String sTemp;
        double dDateTimeValue = 0d;
        double dTemp = 0d;
        if(iField == giDataRecordDateTimeViewedIndexes[giMediaCategory]) {
            for (Map.Entry<Integer, String[]>
                    entry : tmCatalogList.entrySet()) {
                sCatalogListRecord = entry.getValue();
                sTemp = sCatalogListRecord[iField];
                try {
                    dTemp = Double.parseDouble(sTemp);
                }catch (Exception e){
                    String s = e.getMessage();
                    Toast.makeText(this, "Trouble with sort by datetime read by user.\n" + s, Toast.LENGTH_LONG).show();
                }
                if (dTemp < dDateTimeValue) dDateTimeValue = dTemp;
            }
        }  //if sort by last read datetime, finished getting oldest date.



        for (Map.Entry<Integer, String[]>
                entry : tmCatalogList.entrySet()) {
            sCatalogListRecord = entry.getValue();
            //Append the ItemID to the key field to ensure that the key is always unique.
            //  The user could choose to sort by "LAST_READ_DATE", and the LAST_READ_DATE
            //  could be 0, duplicated. There cannot be duplicate keys.
            //  The user might also decide to sort by # of pages, for which there might
            //  be duplicates.
            sKey = sCatalogListRecord[iField];
            if(iField == giDataRecordDateTimeViewedIndexes[giMediaCategory]) {
                if (Double.parseDouble(sKey) == 0d){
                    dTemp = dDateTimeValue - 1.0d;
                    sKey = Double.toString(dTemp);
                }
            }
            sKey = sKey + sCatalogListRecord[giDataRecordIDIndexes[giMediaCategory]];
            treeMapPreSort.put(sKey, sCatalogListRecord);
        }

        //Review the sort (for debugging purposes):
        /*for (Map.Entry<String, String[]>
                entry : treeMapPreSort.entrySet()) {
            sCatalogListRecord = entry.getValue();
            sKey = entry.getKey();
            Log.d("CatalogActivity", "ChangeComicSortOrder: " +
                    sCatalogListRecord[GlobalClass.COMIC_ID_INDEX] + ": " +
                    sKey + ", " +
                    sCatalogListRecord[GlobalClass.COMIC_DATETIME_LAST_READ_BY_USER]);
        }*/


        //Treemap presort will auto-sort itself.

        //Delete everything out of the old TreeMap, and re-populate it with the new sort order:
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

        //Re-populate the RecyclerVeiw adapter and tell the RecyclerView to update itself:
        populate_RecyclerViewCatalogItems(tmNewOrderCatalogList);
    }

    public void ApplyRecyclerViewSortOrder(){

        //Create new TreeMap to presort the catalog items:
        TreeMap<Integer, String[]> treeMapNewSortOrder; //String = field being sorted, String = Catalog item data
        treeMapNewSortOrder = new TreeMap<>();

        //Get existing data and load elements into the presorter:
        TreeMap<Integer, String[]> tmCatalogList;
        tmCatalogList = globalClass.gtmCatalogLists.get(giMediaCategory);
        String[] sCatalogListRecord;
        Integer iKey, iIterator;

        if(gbRecyclerViewSortAscending){
            iKey = 0;
            iIterator = 1;
        } else {
            iKey = tmCatalogList.size();
            iIterator = -1;
        }


        //Reverse the sort of the catalog:
        for (Map.Entry<Integer, String[]>
                entry : tmCatalogList.entrySet()) {
            sCatalogListRecord = entry.getValue();
            treeMapNewSortOrder.put(iKey, sCatalogListRecord);
            iKey = iKey + iIterator;
        }

        //Re-populate the RecyclerVeiw adapter and tell the RecyclerView to update itself:
        populate_RecyclerViewCatalogItems(treeMapNewSortOrder);
    }

    public void AssignCatalogFilter(boolean bFilterOn, String sFilterText){

        if(bFilterOn){
            //Create new TreeMap to presort the Catalog items:
            TreeMap<Integer, String[]> treeMapFiltered;
            treeMapFiltered = new TreeMap<>();

            //Get existing data and load elements into the presorter:
            TreeMap<Integer, String[]> tmCatalogList;
            tmCatalogList = globalClass.gtmCatalogLists.get(giMediaCategory);
            String[] sCatalogListRecord;
            StringBuilder sbKey;
            int iRID = 0;
            String sFilterText_LowerCase = sFilterText.toLowerCase();
            String sKey_LowerCase;
            for (Map.Entry<Integer, String[]>
                    entry : tmCatalogList.entrySet()) {
                sCatalogListRecord = entry.getValue();
                sbKey = new StringBuilder();
                for(int i = 0; i < GlobalClass.CatalogRecordFields[giMediaCategory].length; i++){
                    sbKey.append(sCatalogListRecord[i]);
                }
                sKey_LowerCase = sbKey.toString().toLowerCase();

                if(sKey_LowerCase.contains(sFilterText_LowerCase)){
                    treeMapFiltered.put(iRID, sCatalogListRecord);
                    iRID++;
                }
            }
            populate_RecyclerViewCatalogItems(treeMapFiltered);

        } else {

            SetCatalogSortOrderDefault();

        }
    }

    //=====================================================================================
    //===== Comic Viewer Code =================================================================
    //=====================================================================================

    public void StartComicViewerActivity(String[] sFields){

        Intent intentComicViewer = new Intent(this, ComicDetailsActivity.class);

        //intentComicViewer.putExtra(ComicDetailsActivity.COMIC_FIELDS_STRING,sFields);
        globalClass.gsSelectedComic = sFields; //Don't bother with using the intent to pass this data.

        startActivity(intentComicViewer);
    }


    //=====================================================================================
    //===== Obfuscation Code ==============================================================
    //=====================================================================================

    @Override
    public void onResume(){
        super.onResume();

        //If we are returning from ComicDetailsActivity after deleting the comic,
        //  return the sort:
        if(globalClass.gbComicJustDeleted){
            SetCatalogSortOrderDefault();
            globalClass.gbComicJustDeleted = false;
        }

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

    //=====================================================================================
    //===== Local Utilities ===============================================================
    //=====================================================================================

    public Object MediaCategoryReturn(Object[] Options){
        return Options[giMediaCategory];
    }

}