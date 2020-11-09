package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.annotation.NonNull;

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

public class ComicsCatalogActivity extends AppCompatActivity {

    //Global Constants:
    private static final String LOG_TAG = "ComicsCatalogActivity";

    //Global Variables:
    private GlobalClass globalClass;
    private RecyclerView.Adapter<RecyclerViewComicsAdapter.ViewHolder> gRecyclerViewComicsAdapter;
    private final boolean gbDebugTouch = false;
    RecyclerView gRecyclerView;
    private boolean gbRecyclerViewFiltered;
    private boolean gbComicRestrictionsOn;
    Spinner gspSpinnerSort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.ComicsTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_catalog);

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        if(globalClass.ObfuscationOn) {
            setTitle(globalClass.getObfuscatedProgramName());
        } else {
            setTitle(globalClass.sNonObfustatedProgramName[GlobalClass.MEDIA_CATEGORY_COMICS]);
        }


        //Update TextView to show 0 comics if applicable:
        notifyZeroComicsIfApplicable();

        //Get comic restrictions preferences:
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> sssComicTagsRestricted = sharedPreferences.getStringSet("multi_select_list_restricted_tags", null);
        //Attempt to match the restricted tag text from the preferences to the Tag ID:
        String s;
        for(String sRestrictedTag: sssComicTagsRestricted) {
            for (Map.Entry<Integer, String[]> entry : globalClass.gtmComicTagReferenceList.entrySet()) {
                s = entry.getValue()[GlobalClass.TAG_NAME_INDEX];
                if(sRestrictedTag.equals(s)){
                    //If the restricted tag has been found, assign it to the restricted tags TreeMap:
                    globalClass.gtmComicTagsRestricted.put(entry.getKey(), entry.getValue()[GlobalClass.TAG_NAME_INDEX]);
                }
            }
        }


        gbComicRestrictionsOn = sharedPreferences.getBoolean("hide_restricted_tags", false);


        gRecyclerView = findViewById(R.id.RecyclerView_ComicsCatalog);
        configure_RecyclerViewComicsCatalog();
        SetComicSortOrderDefault(); //This routine also populates the RecyclerView Adapter.
        //gspSpinnerSort.setOnItemSelected will call SetComicSortOrderDefault after menu load.


        if(globalClass.connectivityManager == null){
            globalClass.registerNetworkCallback();
              //This lets us check globalClass.isNetworkConnected to see if we are connected to the
                //network;
        }

        gRecyclerViewComicsAdapter.notifyDataSetChanged();

        //See additional initialization in onCreateOptionsMenu().
    }

    public void notifyZeroComicsIfApplicable(){

        //Update TextView to show 0 comics if applicable:
        TextView tvCatalogStatus = findViewById(R.id.textView_CatalogStatus);
        if (globalClass.gtmCatalogComicList.size() == 0 ) {
            tvCatalogStatus.setVisibility(View.VISIBLE);
            String s = "Catalog contains 0 comics.";
            tvCatalogStatus.setText(s);
        } else {
            tvCatalogStatus.setVisibility(View.INVISIBLE);
        }

    }

    public static int SPINNER_ITEM_MISSING_TAGS = 0;
    public static int SPINNER_ITEM_IMPORT_DATE = 1;
    public static int SPINNER_ITEM_LAST_READ_DATE = 2;
    String[] gsSpinnerItems={"Missing tags","Import Date","Last Read Date"};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {


        getMenuInflater().inflate(R.menu.comics_catalog_action_bar, menu);

        //Set the restricted comics lock icon as appropriate:
        MenuItem restrictedItem = menu.findItem(R.id.icon_comics_restricted);
        if(gbComicRestrictionsOn){
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
                        ComicsCatalogFilter(true, query);
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
                    SetComicSortOrderDefault();
                }
                return false;
            }
        });

        //Configure the 'Sort by' selection spinner:
        MenuItem miSpinnerSort = menu.findItem(R.id.spinner_sort);
        gspSpinnerSort =(Spinner) miSpinnerSort.getActionView();
        //wrap the items in the Adapter
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this, R.layout.comics_action_bar_spinner_item, gsSpinnerItems);
        //assign adapter to the Spinner
        gspSpinnerSort.setAdapter(adapter);

        //Change spinner position if we have just returned from an import operation.
        //  The user will want to see the comic that they just imported has made it into the
        //  catalog.
        if(globalClass.gbComicJustImported) {
            //Set sort by comic import datetime
            globalClass.giComicDefaultSortBySetting = GlobalClass.COMIC_DATETIME_IMPORT_INDEX;
            //Set the sort order to reverse:
            if( globalClass.gbComicSortAscending) {
                globalClass.gbComicSortAscending = false;
            }
            gspSpinnerSort.setSelection(SPINNER_ITEM_IMPORT_DATE);
            //Get a reference to the sort order icon:
            MenuItem miSortOrder = menu.findItem(R.id.icon_sort_order);
            miSortOrder.setIcon(R.drawable.baseline_sort_descending_white_18dp);
            globalClass.gbComicJustImported = false;
        }

        //Continue with configuring the spinner:
        gspSpinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == SPINNER_ITEM_MISSING_TAGS){
                    globalClass.giComicDefaultSortBySetting = GlobalClass.COMIC_TAGS_INDEX;
                } else if(position == SPINNER_ITEM_IMPORT_DATE) {
                    globalClass.giComicDefaultSortBySetting = GlobalClass.COMIC_DATETIME_IMPORT_INDEX;
                } else if(position == SPINNER_ITEM_LAST_READ_DATE) {
                    globalClass.giComicDefaultSortBySetting = GlobalClass.COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX;
                }
                SetComicSortOrderDefault();
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

            case R.id.icon_comics_restricted:
                gbComicRestrictionsOn = !gbComicRestrictionsOn;
                if(gbComicRestrictionsOn){
                    item.setIcon(R.drawable.baseline_lock_white_18dp);
                } else {
                    item.setIcon(R.drawable.baseline_lock_open_white_18dp);
                }
                //Repopulate the catalog comics list:
                populate_RecyclerViewComicsCatalog(globalClass.gtmCatalogComicList);
                return true;

            case R.id.icon_sort_order:
                if( globalClass.gbComicSortAscending) {
                    item.setIcon(R.drawable.baseline_sort_descending_white_18dp);
                    globalClass.gbComicSortAscending = false;
                } else {
                    item.setIcon(R.drawable.baseline_sort_ascending_white_18dp);
                    globalClass.gbComicSortAscending = true;
                }
                ApplyComicSortOrder();
                return true;

            case R.id.menu_import:
                Intent intentImport = new Intent(this, ImportComicsActivity_obsolete.class);
                startActivity(intentImport);
                return true;

            case R.id.menu_export:
                Intent intentExport = new Intent(this, ExportComicsActivity.class);
                startActivity(intentExport);
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

    public static class CCDataServiceResponseReceiver extends BroadcastReceiver {
        //CCDataService = Comics Catalog Data Service
        //public static final String CC_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_CC_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            /*boolean bCatalogDataChange;
            boolean bError;

            //Get boolean indicating data acquisition was successful:
            bCatalogDataChange = intent.getBooleanExtra(ComicsCatalogDataService.EXTRA_BOOL_CATALOG_DATA_CHANGE,false);
            if( bCatalogDataChange) {
                //Update TextView to show 0 comics if applicable:
                notifyZeroComicsIfApplicable();
                gRecyclerViewComicsAdapter.notifyDataSetChanged();
            }

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(ComicsCatalogDataService.EXTRA_BOOL_DATA_IMPORT_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(ComicsCatalogDataService.EXTRA_STRING_DATA_IMPORT_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            }*/


        }
    }

    //=====================================================================================
    //===== RecyclerView Code =================================================================
    //=====================================================================================

    public void configure_RecyclerViewComicsCatalog(){

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

    public class RecyclerViewComicsAdapter extends RecyclerView.Adapter<RecyclerViewComicsAdapter.ViewHolder> {

        private final TreeMap<Integer, String[]> treeMap;
        private final Integer[] mapKeys;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView ivThumbnail;
            public final TextView tvThumbnailText;
            public final TextView tvComicDetails;

            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.ImageView_Thumbnail);
                tvThumbnailText = v.findViewById(R.id.editText_ComicTitle);
                tvComicDetails = v.findViewById(R.id.TextView_ComicDetails);
            }
        }

        public RecyclerViewComicsAdapter(TreeMap<Integer, String[]> data) {
            this.treeMap = data;
            mapKeys = treeMap.keySet().toArray(new Integer[getCount()]);
        }

        public int getCount() {
            return treeMap.size();
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public RecyclerViewComicsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                       int viewType) {
            // create a new view
            View v;

            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                v = inflater.inflate(R.layout.recycler_comics_grid, parent, false);
            } else {
                v = inflater.inflate(R.layout.recycler_comics_row, parent, false);
            }

            return new ViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, final int position) {
            // - get element from your data set at this position
            // - replace the contents of the view with that element

            //Get the data for the row:
            String[] sFields;
            sFields = treeMap.get(mapKeys[position]);
            if (sFields == null) {
                sFields = new String[GlobalClass.ComicRecordFields.length]; //To prevent possible null pointer exception later.
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
                String sThumbnailFilePath = globalClass.gfComicsFolder.getAbsolutePath()
                        + File.separator
                        + sFields[GlobalClass.COMIC_FOLDER_NAME_INDEX] + File.separator
                        + sFields[GlobalClass.COMIC_THUMBNAIL_FILE_INDEX];
                File fThumbnail = new File(sThumbnailFilePath);

                if (fThumbnail.exists()) {
                    Glide.with(getApplicationContext()).load(fThumbnail).into(holder.ivThumbnail);
                }

                holder.tvThumbnailText.setText(sFields[GlobalClass.COMIC_NAME_INDEX]);

            }


            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(gbDebugTouch) Toast.makeText(getApplicationContext(),"Click Item Number " + position, Toast.LENGTH_LONG).show();
                    StartComicViewerActivity(sFields_final);
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

            if(holder.tvComicDetails != null) {
                //The landscape version (GridView) does not have a "Comic Details" TextView, so
                //  don't try to set it if this object is null.
                String s = "Comic ID: " + sFields[GlobalClass.COMIC_ID_INDEX];
                holder.tvComicDetails.setText(s);
            }
        }

        // Return the size of the data set (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return treeMap.size();
        }

    }

    public void populate_RecyclerViewComicsCatalog(TreeMap<Integer, String[]> tmCatalogComicList){
        gRecyclerViewComicsAdapter = new RecyclerViewComicsAdapter(FilterOutRestrictedComics(tmCatalogComicList));
        gRecyclerView.setAdapter(gRecyclerViewComicsAdapter);
        Toast.makeText(this, "Showing " + gRecyclerViewComicsAdapter.getItemCount() + " comics.", Toast.LENGTH_SHORT).show();
    }

    public void SetComicSortOrderDefault(){
        ChangeComicSortField(globalClass.giComicDefaultSortBySetting); //TODO, return to default sort.
        gbRecyclerViewFiltered = false;  //Removes filtering.
    }


    public TreeMap<Integer, String[]> FilterOutRestrictedComics(TreeMap<Integer, String[]> tmIncoming){
        TreeMap<Integer, String[]> tmOutgoing = new TreeMap<>();
        boolean bNoData = true;
        if(gbComicRestrictionsOn){
            //Format the restriction tag set so that we can process it:
            StringBuilder sbRestrictedTags = new StringBuilder();
            for(Map.Entry<Integer, String> entry: globalClass.gtmComicTagsRestricted.entrySet()){
                sbRestrictedTags.append(entry.getValue());
                sbRestrictedTags.append(",");
            }
            //Split restricted tags into array:
            String[] sTagsArray = sbRestrictedTags.toString().split(",");


            if(sTagsArray.length > 0) { //If restricted tags exist...



                 //Look for restricted tags in the incoming treeMap and transfer the entry if
                //  none are found:
                String[] sFields;
                int i = 0;
                for (Map.Entry<Integer, String[]>
                        entry : tmIncoming.entrySet()) {
                    sFields = entry.getValue();
                    boolean bHasRestrictedTag = false;
                    for (String s : sTagsArray) {

                        if (sFields[GlobalClass.COMIC_TAGS_INDEX].contains(s.trim())) {
                            bHasRestrictedTag = true;
                            break;
                        }
                    }
                    if(!bHasRestrictedTag){
                        tmOutgoing.put(i, sFields);
                        i++;
                    }
                }
                bNoData = false;
            }

        }
        if(bNoData) {
            return tmIncoming;
        } else {
            return tmOutgoing;
        }


    }



    public void ChangeComicSortField(int iField){

        //Create new TreeMap to presort the comics:
        TreeMap<String, String[]> treeMapPreSort; //String = field being sorted, String = Comic data
        treeMapPreSort = new TreeMap<>();

        //Get existing data and load elements into the presorter:
        TreeMap<Integer, String[]> tmCatalogComicList;
        tmCatalogComicList = globalClass.gtmCatalogComicList;
        String[] sComicListRecord;
        String sKey;

        //If the user has selected 'Sort by last read date', get the oldest read date and apply
        //  that date plus one day to any comic that has a "zero" for the last read date.
        String sTemp;
        double dDateTimeValue = 0d;
        double dTemp = 0d;
        if(iField == GlobalClass.COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX) {
            for (Map.Entry<Integer, String[]>
                    entry : tmCatalogComicList.entrySet()) {
                sComicListRecord = entry.getValue();
                sTemp = sComicListRecord[iField];
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
                entry : tmCatalogComicList.entrySet()) {
            sComicListRecord = entry.getValue();
            //Append the ComicID to the key field to ensure that the key is always unique.
            //  The user could choose to sort by "LAST_READ_DATE", and the LAST_READ_DATE
            //  could be 0, duplicated. There cannot be duplicate keys.
            //  The user might also decide to sort by # of pages, for which there might
            //  be duplicates.
            sKey = sComicListRecord[iField];
            if(iField == GlobalClass.COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX) {
                if (Double.parseDouble(sKey) == 0d){
                    dTemp = dDateTimeValue - 1.0d;
                    sKey = Double.toString(dTemp);
                }
            }
            sKey = sKey + sComicListRecord[GlobalClass.COMIC_ID_INDEX];
            treeMapPreSort.put(sKey, sComicListRecord);
        }

        //Review the sort (for debugging purposes):
        /*for (Map.Entry<String, String[]>
                entry : treeMapPreSort.entrySet()) {
            sComicListRecord = entry.getValue();
            sKey = entry.getKey();
            Log.d("ComicsCatalogActivity", "ChangeComicSortOrder: " +
                    sComicListRecord[GlobalClass.COMIC_ID_INDEX] + ": " +
                    sKey + ", " +
                    sComicListRecord[GlobalClass.COMIC_DATETIME_LAST_READ_BY_USER]);
        }*/


        //Treemap presort will auto-sort itself.

        //Delete everything out of the old TreeMap, and re-populate it with the new sort order:
        TreeMap<Integer, String[]> tmNewOrderCatalogComicList = new TreeMap<>();
        int iComicRID, iIterator;
        if(globalClass.gbComicSortAscending){
            iComicRID = 0;
            iIterator = 1;
        } else {
            iComicRID = treeMapPreSort.size();
            iIterator = -1;
        }

        for (Map.Entry<String, String[]>
                entry : treeMapPreSort.entrySet()) {
            sComicListRecord = entry.getValue();
            tmNewOrderCatalogComicList.put(iComicRID, sComicListRecord);
            iComicRID += iIterator;
        }

        //Re-populate the RecyclerVeiw adapter and tell the RecyclerView to update itself:
        populate_RecyclerViewComicsCatalog(tmNewOrderCatalogComicList);
    }

    public void ApplyComicSortOrder(){

        //Create new TreeMap to presort the comics:
        TreeMap<Integer, String[]> treeMapNewSortOrder; //String = field being sorted, String = Comic data
        treeMapNewSortOrder = new TreeMap<>();

        //Get existing data and load elements into the presorter:
        TreeMap<Integer, String[]> tmCatalogComicList;
        tmCatalogComicList = globalClass.gtmCatalogComicList;
        String[] sComicListRecord;
        Integer iKey, iIterator;

        if(globalClass.gbComicSortAscending){
            iKey = 0;
            iIterator = 1;
        } else {
            iKey = tmCatalogComicList.size();
            iIterator = -1;
        }


        //Reverse the sort of the catalog:
        for (Map.Entry<Integer, String[]>
                entry : tmCatalogComicList.entrySet()) {
            sComicListRecord = entry.getValue();
            treeMapNewSortOrder.put(iKey, sComicListRecord);
            iKey = iKey + iIterator;
        }

        //Re-populate the RecyclerVeiw adapter and tell the RecyclerView to update itself:
        populate_RecyclerViewComicsCatalog(treeMapNewSortOrder);
    }

    public void ComicsCatalogFilter(boolean bFilterOn, String sFilterText){

        if(bFilterOn){
            //Create new TreeMap to presort the comics:
            TreeMap<Integer, String[]> treeMapFiltered;
            treeMapFiltered = new TreeMap<>();

            //Get existing data and load elements into the presorter:
            TreeMap<Integer, String[]> tmCatalogComicList;
            tmCatalogComicList = globalClass.gtmCatalogComicList;
            String[] sComicListRecord;
            StringBuilder sbKey;
            int iComicRID = 0;
            String sFilterText_LowerCase = sFilterText.toLowerCase();
            String sKey_LowerCase;
            for (Map.Entry<Integer, String[]>
                    entry : tmCatalogComicList.entrySet()) {
                sComicListRecord = entry.getValue();
                sbKey = new StringBuilder();
                sbKey.append(sComicListRecord[GlobalClass.COMIC_ID_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_NAME_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_PARODIES_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_CHARACTERS_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_TAGS_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_ARTISTS_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_GROUPS_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_CATEGORIES_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_SOURCE_INDEX]);
                sKey_LowerCase = sbKey.toString().toLowerCase();

                if(sKey_LowerCase.contains(sFilterText_LowerCase)){
                     treeMapFiltered.put(iComicRID, sComicListRecord);
                     iComicRID++;
                 }
            }
            populate_RecyclerViewComicsCatalog(treeMapFiltered);

        } else {

            SetComicSortOrderDefault();

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
    //===== Obfuscation Code =================================================================
    //=====================================================================================

    @Override
    public void onResume(){
        super.onResume();

        //If we are returning from ComicDetailsActivity after deleting the comic,
        //  return the sort:
        if(globalClass.gbComicJustDeleted){
            SetComicSortOrderDefault();
            globalClass.gbComicJustDeleted = false;
        }

        //If a comic has just been imported, there are certain actions that take place to sort the
        //  comics so that the user sees most recent imports. These actions cannot take place here,
        //  but rather take place in the menu inflation routine, as the menu icons are changed
        //  to indicate the new sort order.


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
        gRecyclerViewComicsAdapter.notifyDataSetChanged();
    }

    public void RemoveObfuscation(){
        //Remove obfuscation:
        setTitle(globalClass.sNonObfustatedProgramName[GlobalClass.MEDIA_CATEGORY_COMICS]);
        //Update the RecyclerView:
        gRecyclerViewComicsAdapter.notifyDataSetChanged();
    }













}