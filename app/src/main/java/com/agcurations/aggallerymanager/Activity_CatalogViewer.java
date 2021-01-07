package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class Activity_CatalogViewer extends AppCompatActivity {

    //Global Constants:
    private static final String LOG_TAG = "CatalogActivity";

    //Global Variables:
    private GlobalClass globalClass;
    //private int giMediaCategory;
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


    private CatalogViewerServiceResponseReceiver catalogViewerServiceResponseReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.MainTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        //Intent intent = getIntent();
        //giMediaCategory = intent.getIntExtra("MEDIA_CATEGORY", -1);

        if(globalClass.ObfuscationOn) {
            setTitle(globalClass.getObfuscatedProgramName());
        } else {
            setTitle(globalClass.sNonObfuscatedProgramName[globalClass.giSelectedCatalogMediaCategory]);
        }

        //Update TextView to show 0 catalog items if applicable:
        notifyZeroCatalogItemsIfApplicable();


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //Get tag restrictions preferences:
        /*Set<String> ssCatalogTagsRestricted = sharedPreferences.getStringSet("multi_select_list_restricted_tags", null);
        //Attempt to match the restricted tag text from the preferences to the Tag ID:
        if(ssCatalogTagsRestricted != null) {
            String s;
            for (String sRestrictedTag : ssCatalogTagsRestricted) {
                for (Map.Entry<String, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(globalClass.giSelectedCatalogMediaCategory).entrySet()) {
                    s = entry.getValue().TagText;
                    if (sRestrictedTag.equals(s)) {
                        //If the restricted tag has been found, assign it to the restricted tags TreeMap:
                        globalClass.gtmCatalogTagsRestricted.get(globalClass.giSelectedCatalogMediaCategory)
                                .put(entry.getValue().TagID, entry.getValue().TagText);
                    }
                }
            }
        }*/

        gbCatalogTagsRestrictionsOn = sharedPreferences.getBoolean("hide_restricted_tags", false);

        gRecyclerView = findViewById(R.id.RecyclerView_CatalogItems);
        configure_RecyclerViewCatalogItems();

        gsFilterText = "";

        if(globalClass.connectivityManager == null){
            globalClass.registerNetworkCallback();
            //This lets us check globalClass.isNetworkConnected to see if we are connected to the
            //network;
        }



        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        catalogViewerServiceResponseReceiver = new CatalogViewerServiceResponseReceiver();
        //registerReceiver(importDataServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(catalogViewerServiceResponseReceiver,filter);


        //See additional initialization in onCreateOptionsMenu().
    }

    @Override
    protected void onDestroy() {
        //unregisterReceiver(importDataServiceResponseReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(catalogViewerServiceResponseReceiver);
        super.onDestroy();
    }

    public void notifyZeroCatalogItemsIfApplicable(){

        //Update TextView to show 0 items if applicable:
        TextView tvCatalogStatus = findViewById(R.id.textView_CatalogStatus);
        if (globalClass.gtmCatalogLists.get(globalClass.giSelectedCatalogMediaCategory).size() == 0 ) {
            tvCatalogStatus.setVisibility(View.VISIBLE);
            String s = "Catalog contains 0 items.";
            tvCatalogStatus.setText(s);
        } else {
            tvCatalogStatus.setVisibility(View.INVISIBLE);
        }

    }

    public static final int SPINNER_ITEM_IMPORT_DATE = 0;
    public static final int SPINNER_ITEM_LAST_VIEWED_DATE = 1;
    final String[] gsSpinnerItems={"Import Date","Last Read Date"};

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
        if(giRecyclerViewSortBySetting == GlobalClass.giDataRecordDateTimeImportIndexes[globalClass.giSelectedCatalogMediaCategory]){
            gspSpinnerSort.setSelection(SPINNER_ITEM_IMPORT_DATE);
            //When sorting by import date, sort Descending by default (newest first):
            SetSortIconToDescending();
        } else if(giRecyclerViewSortBySetting == GlobalClass.giDataRecordDateTimeViewedIndexes[globalClass.giSelectedCatalogMediaCategory]){
            gspSpinnerSort.setSelection(SPINNER_ITEM_LAST_VIEWED_DATE);
            //When sorting by last viewed, sort Ascending by default:
            SetSortIconToAscending();
        }

        //Continue with configuring the spinner:
        gspSpinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == SPINNER_ITEM_IMPORT_DATE) {
                    giRecyclerViewSortBySetting = GlobalClass.giDataRecordDateTimeImportIndexes[globalClass.giSelectedCatalogMediaCategory];
                } else if(position == SPINNER_ITEM_LAST_VIEWED_DATE) {
                    giRecyclerViewSortBySetting = GlobalClass.giDataRecordDateTimeViewedIndexes[globalClass.giSelectedCatalogMediaCategory];
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

    private Parcelable recyclerViewState;
    @Override
    protected void onPause() {
        //Attempt to save the state, ie scroll position, of the recyclerView:
        recyclerViewState = gRecyclerView.getLayoutManager().onSaveInstanceState();
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        //Attempt to restore the state, ie scroll position, of the recyclerView:
        gRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);//restore


        //Apply a resort.
        //If a comic was just deleted, or a comic was just read, the view needs to be updated.
        //  This includes removing a comic, or moving a comic to the end of the RecyclerView.

        //Change spinner position if we have just come in from an import operation.
        //  The user will want to see that the item they just imported has made it into the
        //  catalog.
        if(globalClass.gbJustImported[globalClass.giSelectedCatalogMediaCategory]) {
            //Set sort by to "import_datetime"
            giRecyclerViewSortBySetting = GlobalClass.giDataRecordDateTimeImportIndexes[globalClass.giSelectedCatalogMediaCategory];
            globalClass.gbJustImported[globalClass.giSelectedCatalogMediaCategory] = false;
            //Set the spinner:
            if(gspSpinnerSort != null) {
                gspSpinnerSort.setSelection(SPINNER_ITEM_IMPORT_DATE);
            }
        } else {
            //Set sort by to "viewed_datetime"
            giRecyclerViewSortBySetting = GlobalClass.giDataRecordDateTimeViewedIndexes[globalClass.giSelectedCatalogMediaCategory];
            if(gspSpinnerSort != null) {
                gspSpinnerSort.setSelection(SPINNER_ITEM_LAST_VIEWED_DATE);
            }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Display a message showing the name of the item selected.
        int itemID = item.getItemId();

        if(itemID == R.id.icon_tags_restricted){
            if(gbCatalogTagsRestrictionsOn){
                //If restrictions are on, ask for pin code before unlocking.
                Intent intentPinCodeAccessSettings = new Intent(this, Activity_PinCodePopup.class);
                startActivityForResult(intentPinCodeAccessSettings, Activity_PinCodePopup.START_ACTIVITY_FOR_RESULT_UNLOCK_RESTRICTED_TAGS);
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

            if(requestCode == Activity_PinCodePopup.START_ACTIVITY_FOR_RESULT_UNLOCK_RESTRICTED_TAGS){
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

    public class CatalogViewerServiceResponseReceiver extends BroadcastReceiver {
        public static final String CATALOG_VIEWER_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_CATALOG_VIEWER_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_CatalogViewer.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_CatalogViewer.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to request to delete an item:
                boolean bIsDeleteItemResponse = intent.getBooleanExtra(Service_CatalogViewer.EXTRA_BOOL_DELETE_ITEM, false);
                if(bIsDeleteItemResponse) {
                    boolean bDeleteItemResult = intent.getBooleanExtra(Service_CatalogViewer.EXTRA_BOOL_DELETE_ITEM_RESULT, false);
                    if (bDeleteItemResult) {
                        populate_RecyclerViewCatalogItems(); //Refresh the catalog recycler view.
                    } else {
                        Toast.makeText(getApplicationContext(),"Could not successfully delete item.", Toast.LENGTH_LONG).show();
                    }
                }

            }

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
        private final Context context;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView ivThumbnail;
            public final Button btnDelete;
            public final TextView tvThumbnailText;
            public final TextView tvDetails;

            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.imageView_Thumbnail);
                btnDelete = v.findViewById(R.id.button_Delete);
                tvThumbnailText = v.findViewById(R.id.textView_Title);
                tvDetails = v.findViewById(R.id.textView_Details);
            }
        }

        public RecyclerViewCatalogAdapter(TreeMap<Integer, String[]> data, Context _context) {
            this.treeMap = data;
            mapKeys = treeMap.keySet().toArray(new Integer[getCount()]);
            context = _context;
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
                if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS){
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
                sFields = new String[GlobalClass.CatalogRecordFields[globalClass.giSelectedCatalogMediaCategory].length]; //To prevent possible null pointer exception later.
            }
            final String[] sFields_final = sFields;

            String sItemName = "";

            if(globalClass.ObfuscationOn) {

                //Get the obfuscation image index:
                int i = (position % globalClass.getObfuscationImageCount());
                //Get the obfuscation image resource ID:
                int iObfuscatorResourceID = globalClass.getObfuscationImage(i);

                Bitmap bmObfuscator = BitmapFactory.decodeResource(getResources(), iObfuscatorResourceID);
                holder.ivThumbnail.setImageBitmap(bmObfuscator);
                holder.tvThumbnailText.setText(globalClass.getObfuscationImageText(i));

                if(holder.btnDelete != null){
                    //Don't allow delete during obfuscation.
                    holder.btnDelete.setVisibility(View.INVISIBLE);
                }
            } else {

                //Load the non-obfuscated image into the RecyclerView ViewHolder:
                String sThumbnailFilePath = globalClass.gfCatalogFolders[globalClass.giSelectedCatalogMediaCategory].getAbsolutePath() + File.separator
                        + sFields[GlobalClass.giDataRecordFolderIndexes[globalClass.giSelectedCatalogMediaCategory]] + File.separator
                        + sFields[GlobalClass.giDataRecordRecyclerViewImageIndexes[globalClass.giSelectedCatalogMediaCategory]];
                File fThumbnail = new File(sThumbnailFilePath);

                if (fThumbnail.exists()) {
                    Glide.with(getApplicationContext()).load(fThumbnail).into(holder.ivThumbnail);
                }

                String sThumbnailText = "";
                switch(globalClass.giSelectedCatalogMediaCategory){
                    case GlobalClass.MEDIA_CATEGORY_VIDEOS:
                        String sTemp = sFields[GlobalClass.VIDEO_FILENAME_INDEX];
                        sItemName = GlobalClass.JumbleFileName(sTemp);
                        sThumbnailText = sItemName + ", " +
                                sFields[GlobalClass.VIDEO_DURATION_TEXT_INDEX];
                        break;
                    case GlobalClass.MEDIA_CATEGORY_IMAGES:
                        sItemName = GlobalClass.JumbleFileName(sFields[GlobalClass.IMAGE_FILENAME_INDEX]);
                        sThumbnailText = sItemName + ", " +
                                sFields[GlobalClass.IMAGE_TAGS_INDEX];
                        break;
                    case GlobalClass.MEDIA_CATEGORY_COMICS:
                        sItemName = sFields[GlobalClass.COMIC_NAME_INDEX];
                        sThumbnailText = sItemName;
                        //sThumbnailText = GlobalClass.ConvertDoubleTimeStampToString(sFields[GlobalClass.COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX]);
                        break;
                }
                holder.tvThumbnailText.setText(sThumbnailText);

                if(holder.btnDelete != null){
                    holder.btnDelete.setVisibility(View.VISIBLE);
                }
            }


            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(gbDebugTouch) Toast.makeText(getApplicationContext(),"Click Item Number " + position, Toast.LENGTH_LONG).show();

                    if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                        StartVideoPlayerActivity(treeMap, Integer.parseInt(sFields_final[GlobalClass.VIDEO_ID_INDEX]));

                    } else if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
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


            if(holder.btnDelete != null) {
                final String sItemNameToDelete = sItemName;
                final String sItemID = sFields[GlobalClass.giDataRecordIDIndexes[globalClass.giSelectedCatalogMediaCategory]];
                holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Present confirmation that the user wishes to delete this item.
                        if (globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                            //Please use comic details view to delete comic.
                            Toast.makeText(getApplicationContext(), "Select comic and delete from comic preview.", Toast.LENGTH_SHORT).show();
                        }
                        String sConfirmationMessage = "Confirm item deletion: " + sItemNameToDelete;

                        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogConfirmation);
                        builder.setTitle("Delete Item");
                        builder.setMessage(sConfirmationMessage);
                        //builder.setIcon(R.drawable.ic_launcher);
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                Service_CatalogViewer.startActionDeleteCatalogItem(getApplicationContext(), sItemID, globalClass.giSelectedCatalogMediaCategory);
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog adConfirmationDialog = builder.create();
                        adConfirmationDialog.show();


                    }
                });
            }

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

        //Populate the Key field of the preSort TreeMap with SortBy field data, filtered and restricted if necessary:
        String[] sCatalogListRecord;
        String sKey;
        for (Map.Entry<Integer, String[]>
                entry : globalClass.gtmCatalogLists.get(globalClass.giSelectedCatalogMediaCategory).entrySet()) {
            sCatalogListRecord = entry.getValue();

            //Create a unique key to identify the record in the TreeMap, which includes
            // the SortBy field. TreeMap automatically sorts by the Key field.
            sKey = sCatalogListRecord[giRecyclerViewSortBySetting];
            sKey = sKey + sCatalogListRecord[GlobalClass.giDataRecordIDIndexes[globalClass.giSelectedCatalogMediaCategory]];

            //Apply a filter if requested - build a string out of the records contents, and if a
            //  filter is to be applied, check for a match. If no match, don't add the record to
            //  the TreeMap destined for the RecyclerView:
            boolean bIsFilterMatch = true;
            if(!gsFilterText.equals("")) {
                StringBuilder sbRecordText;
                String sFilterText_LowerCase = gsFilterText.toLowerCase();
                String sKey_RecordText;

                //Loop through all of the field data, append it together, and search the resulting
                //  string for a filter match:
                sbRecordText = new StringBuilder();
                for (int i = 0; i < GlobalClass.CatalogRecordFields[globalClass.giSelectedCatalogMediaCategory].length; i++) {

                    if(i == GlobalClass.giDataRecordTagsIndexes[globalClass.giSelectedCatalogMediaCategory]){
                        //if the field is the tags field, translate the tags to text:
                        StringBuilder sbTags = new StringBuilder();
                        String[] sTagIDs = sCatalogListRecord[i].split(",");
                        for(String sTagID : sTagIDs){
                            sbTags.append(globalClass.getTagTextFromID(Integer.parseInt(sTagID), globalClass.giSelectedCatalogMediaCategory));
                            sbTags.append(", ");
                        }
                        sbRecordText.append(sbTags.toString());
                    } else if (i == GlobalClass.giDataRecordFileNameIndexes[globalClass.giSelectedCatalogMediaCategory]){
                        //if the field is a jumbled filename, unjumble it for the search:
                        String sFileName = GlobalClass.JumbleFileName(sCatalogListRecord[i]);
                        sbRecordText.append(sFileName);
                    } else {
                        sbRecordText.append(sCatalogListRecord[i]);
                    }
                }
                sKey_RecordText = sbRecordText.toString().toLowerCase();

                if (!sKey_RecordText.contains(sFilterText_LowerCase)) {
                    bIsFilterMatch = false;
                }
            }

            //Check to see if the record needs to be skipped due to restriction settings:
            boolean bIsRestricted = false;
            if(gbCatalogTagsRestrictionsOn) {
                String sRecordTags = sCatalogListRecord[GlobalClass.giDataRecordTagsIndexes[globalClass.giSelectedCatalogMediaCategory]];
                String[] saRecordTags = sRecordTags.split(",");
                for (String s : saRecordTags) {
                    //if list of restricted tags contains this particular record tag, mark as restricted item:
                    Integer iTagID = Integer.parseInt(s);
                    ItemClass_Tag ict  = globalClass.gtmCatalogTagReferenceLists.get(globalClass.giSelectedCatalogMediaCategory).get(globalClass.getTagTextFromID(iTagID,globalClass.giSelectedCatalogMediaCategory));
                    if(ict != null) {
                        if (ict.isRestricted) {
                            bIsRestricted = true;
                            break;
                        }
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
        gRecyclerViewCatalogAdapter = new RecyclerViewCatalogAdapter(tmNewOrderCatalogList, this);
        gRecyclerView.setAdapter(gRecyclerViewCatalogAdapter);
        gRecyclerViewCatalogAdapter.notifyDataSetChanged();

    }


    //=====================================================================================
    //===== Player/Viewer Code =================================================================
    //=====================================================================================

    public final static String RECYCLERVIEW_VIDEO_TREEMAP_FILTERED = "RECYCLERVIEW_VIDEO_TREEMAP_FILTERED";
    public final static String RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_ID = "RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_KEY";
    private void StartVideoPlayerActivity(TreeMap<Integer, String[]> treeMap, Integer iVideoID) {
        //Key is the TreeMap Key for the selected video.

        //A timestamp for last viewed is handled within the video player. This is because the
        //  user can swipe left or right in the player to play an adjacent video. The adjacent video
        //  needs its last viewed timestamp to be updated as well.

        //Start the video player:
        Intent intentVideoPlayer = new Intent(this, Activity_VideoPlayer.class);
        intentVideoPlayer.putExtra(RECYCLERVIEW_VIDEO_TREEMAP_FILTERED, treeMap);
        intentVideoPlayer.putExtra(RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_ID, iVideoID);
        startActivity(intentVideoPlayer);
    }

    public void StartComicViewerActivity(String[] sFields){

        //Record the COMIC_DATETIME_LAST_READ_BY_USER:
        Double dTimeStamp = GlobalClass.GetTimeStampFloat();
        String[] sDateTime = new String[]{dTimeStamp.toString()};
        int[] iFields = new int[]{GlobalClass.COMIC_DATETIME_LAST_VIEWED_BY_USER_INDEX};
        globalClass.CatalogDataFile_UpdateRecord(
                sFields[GlobalClass.COMIC_ID_INDEX],
                iFields,
                sDateTime,
                GlobalClass.MEDIA_CATEGORY_COMICS);


        Intent intentComicViewer = new Intent(this, Activity_ComicDetails.class);

        intentComicViewer.putExtra(Activity_ComicDetails.EXTRA_COMIC_FIELDS_STRING, sFields);

        startActivity(intentComicViewer);
    }


    //=====================================================================================
    //===== Obfuscation Code ==============================================================
    //=====================================================================================


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
        setTitle(globalClass.sNonObfuscatedProgramName[globalClass.giSelectedCatalogMediaCategory]);
        //Update the RecyclerView:
        gRecyclerViewCatalogAdapter.notifyDataSetChanged();
    }



}