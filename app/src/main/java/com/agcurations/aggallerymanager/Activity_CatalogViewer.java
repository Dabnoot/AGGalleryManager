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

import android.annotation.SuppressLint;
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
import android.os.Handler;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.TreeMap;

public class Activity_CatalogViewer extends AppCompatActivity {


    //Global Variables:
    private GlobalClass globalClass;
    private RecyclerViewCatalogAdapter gRecyclerViewCatalogAdapter;
    private final boolean gbDebugTouch = false;
    RecyclerView gRecyclerView;

    Spinner gspSpinnerSort;

    Toast toastLastToastMessage;

    ProgressBar gProgressBar_CatalogSortProgress;
    TextView gTextView_CatalogSortProgressBarText;

    private Menu ActivityMenu;

    private CatalogViewerServiceResponseReceiver catalogViewerServiceResponseReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.MainTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog_viewer);

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

        globalClass.gbCatalogViewerTagsRestrictionsOn = sharedPreferences.getBoolean("hide_restricted_tags", false);

        gRecyclerView = findViewById(R.id.RecyclerView_CatalogItems);
        configure_RecyclerViewCatalogItems();

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        catalogViewerServiceResponseReceiver = new CatalogViewerServiceResponseReceiver();
        //registerReceiver(importDataServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(catalogViewerServiceResponseReceiver,filter);

        gProgressBar_CatalogSortProgress = findViewById(R.id.progressBar_CatalogSortProgress);
        gTextView_CatalogSortProgressBarText = findViewById(R.id.textView_CatalogSortProgressBarText);


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
        if(globalClass.gbCatalogViewerTagsRestrictionsOn){
            restrictedItem.setIcon(R.drawable.baseline_lock_white_18dp);
        } else {
            restrictedItem.setIcon(R.drawable.baseline_lock_open_white_18dp);
        }

        // Initialise menu item search bar with id and take its object
        //https://www.geeksforgeeks.org/android-searchview-with-example/
        MenuItem searchViewItem = menu.findItem(R.id.search_bar);
        final SearchView searchView = (SearchView) searchViewItem.getActionView();


        if(!globalClass.ObfuscationOn) {
            //If not obfuscated, and there is an active search filter, apply the text and show the filter:
            if(globalClass.gbCatalogViewerFiltered[globalClass.giSelectedCatalogMediaCategory]){
                searchView.setQuery(globalClass.gsCatalogViewerFilterText[globalClass.giSelectedCatalogMediaCategory], false);
                searchView.setIconified(false);
                searchView.clearFocus();
            }
        }



        // attach setOnQueryTextListener to search view defined above
        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    // Override onQueryTextSubmit method
                    @Override
                    public boolean onQueryTextSubmit(String sQuery)
                    {
                        globalClass.gsCatalogViewerFilterText[globalClass.giSelectedCatalogMediaCategory] = sQuery;
                        populate_RecyclerViewCatalogItems();
                        globalClass.gbCatalogViewerFiltered[globalClass.giSelectedCatalogMediaCategory] = true;
                        searchView.clearFocus();
                        return false;
                    }
                    @Override
                    public boolean onQueryTextChange(String sQuery) {
                        return false;
                    }
                });
        //Set a listener for the "cancel search" button:
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                globalClass.gsCatalogViewerFilterText[globalClass.giSelectedCatalogMediaCategory] = "";
                globalClass.gbCatalogViewerFiltered[globalClass.giSelectedCatalogMediaCategory] = false;
                populate_RecyclerViewCatalogItems();
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
        if(globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_IMPORTED){
            gspSpinnerSort.setSelection(SPINNER_ITEM_IMPORT_DATE);
        } else if(globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] == GlobalClass.SORT_BY_DATETIME_LAST_VIEWED){
            gspSpinnerSort.setSelection(SPINNER_ITEM_LAST_VIEWED_DATE);
        }

        if(globalClass.gbCatalogViewerSortAscending[globalClass.giSelectedCatalogMediaCategory]){
            SetSortIconToAscending();
        } else {
            SetSortIconToDescending();
        }

        //Continue with configuring the spinner:
        gspSpinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == SPINNER_ITEM_IMPORT_DATE) {
                    //globalClass.giCatalogViewerSortBySetting = GlobalClass.giDataRecordDateTimeImportIndexes[globalClass.giSelectedCatalogMediaCategory];
                    globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] = GlobalClass.SORT_BY_DATETIME_IMPORTED;
                } else if(position == SPINNER_ITEM_LAST_VIEWED_DATE) {
                    //globalClass.giCatalogViewerSortBySetting = GlobalClass.giDataRecordDateTimeViewedIndexes[globalClass.giSelectedCatalogMediaCategory];
                    globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] = GlobalClass.SORT_BY_DATETIME_LAST_VIEWED;
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
        if(gRecyclerView.getLayoutManager() != null) {
            recyclerViewState = gRecyclerView.getLayoutManager().onSaveInstanceState();
        }

        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        //Attempt to restore the state, ie scroll position, of the recyclerView:
        if(gRecyclerView.getLayoutManager() != null) {
            gRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);//restore
        }

        //populate_RecyclerViewCatalogItems();
        //no need to call pop() here because the initialization of position of gspSpinnerSort calls it.

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
            if(globalClass.gbCatalogViewerTagsRestrictionsOn){
                //If restrictions are on, ask for pin code before unlocking.
                //Intent intentPinCodeAccessSettings = new Intent(this, Activity_PinCodePopup.class);
                //startActivityForResult(intentPinCodeAccessSettings, Activity_PinCodePopup.START_ACTIVITY_FOR_RESULT_UNLOCK_RESTRICTED_TAGS);

                if(!globalClass.gsPin.equals("")) { //If the user has specified a pin in the settings...
                    //Ask for the pin before revealing restricted tags.
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustomStyle);

                    // set the custom layout
                    final View customLayout = getLayoutInflater().inflate(R.layout.dialog_layout_pin_code, null);
                    builder.setView(customLayout);

                    final AlertDialog adConfirmationDialog = builder.create();

                    //Code action for the Cancel button:
                    Button button_PinCodeCancel = customLayout.findViewById(R.id.button_PinCodeCancel);
                    button_PinCodeCancel.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            adConfirmationDialog.dismiss();
                        }
                    });

                    //Code action for the OK button:
                    Button button_PinCodeOK = customLayout.findViewById(R.id.button_PinCodeOK);
                    button_PinCodeOK.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                            String sPinEntered = editText_DialogInput.getText().toString();

                            if(sPinEntered.equals(globalClass.gsPin)){
                                unlockRestrictedTags();
                            } else {
                                Toast.makeText(getApplicationContext(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                            }

                            adConfirmationDialog.dismiss();
                        }
                    });

                    adConfirmationDialog.show();
                } else {
                    //If the user has NOT specified a pin in the settings...
                    //Go ahead and reveal the restricted tags.
                    unlockRestrictedTags();
                }



            } else {
                //If restrictions are off...
                //Turn on restrictions, hide items, set icon to show lock symbol
                globalClass.gbCatalogViewerTagsRestrictionsOn = true;
                SetRestrictedIconToLock();
                //Repopulate the catalog list:
                populate_RecyclerViewCatalogItems();
            }

        } else if(itemID == R.id.icon_sort_order){
            if(globalClass.gbCatalogViewerSortAscending[globalClass.giSelectedCatalogMediaCategory]) {
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

    private void unlockRestrictedTags(){
        //Show catalog items with restricted tags.
        //Change the lock icon to 'unlocked':
        SetRestrictedIconToUnlock();
        //Set the flag:
        globalClass.gbCatalogViewerTagsRestrictionsOn = false;
        //Repopulate the catalog list:
        populate_RecyclerViewCatalogItems();
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

                //Check to see if this is a response to request to SortAndFilterCatalogDisplay:
                boolean bRefreshCatalogDisplay = intent.getBooleanExtra(Service_CatalogViewer.EXTRA_BOOL_REFRESH_CATALOG_DISPLAY, false);
                if(bRefreshCatalogDisplay) {
                    //Apply the new TreeMap to the RecyclerView:
                    gRecyclerViewCatalogAdapter = new RecyclerViewCatalogAdapter(globalClass.gtmCatalogViewerDisplayTreeMap, getApplicationContext());
                    gRecyclerView.setAdapter(gRecyclerViewCatalogAdapter);
                    gRecyclerViewCatalogAdapter.notifyDataSetChanged();
                    if(toastLastToastMessage != null){
                        toastLastToastMessage.cancel();
                    }
                    if(gProgressBar_CatalogSortProgress != null && gTextView_CatalogSortProgressBarText != null){
                        gProgressBar_CatalogSortProgress.setVisibility(View.INVISIBLE);
                        gTextView_CatalogSortProgressBarText.setVisibility(View.INVISIBLE);
                    }
                    toastLastToastMessage = Toast.makeText(getApplicationContext(), "Showing " + gRecyclerViewCatalogAdapter.getItemCount() + " items.", Toast.LENGTH_SHORT);
                    toastLastToastMessage.show();
                }

                //Check to see if this is a response to update log or progress bar:
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdatePercentComplete = intent.getBooleanExtra(Service_Import.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(Service_Import.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(Service_Import.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_CatalogSortProgress != null) {
                        gProgressBar_CatalogSortProgress.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(Service_Import.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_CatalogSortProgressBarText != null) {
                        gTextView_CatalogSortProgressBarText.setText(sProgressBarText);
                    }
                }

            } //End if not an error message.

        } //End onReceive.

    } //End CatalogViewerServiceResponseReceiver.

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
        globalClass.gbCatalogViewerSortAscending[globalClass.giSelectedCatalogMediaCategory] = true;
    }

    private void SetSortIconToDescending(){
        MenuItem item = ActivityMenu.findItem(R.id.icon_sort_order);
        item.setIcon(R.drawable.baseline_sort_descending_white_18dp);
        globalClass.gbCatalogViewerSortAscending[globalClass.giSelectedCatalogMediaCategory] = false;
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

        private final TreeMap<Integer, ItemClass_CatalogItem> treeMap;
        private final Integer[] mapKeys;
        private final Context context;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView ivThumbnail;
            public final ImageView imageView_Attention;
            public final Button btnDelete;
            public final TextView tvThumbnailText;
            public final TextView tvDetails;

            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.imageView_Thumbnail);
                imageView_Attention = v.findViewById(R.id.imageView_Attention);
                btnDelete = v.findViewById(R.id.button_Delete);
                tvThumbnailText = v.findViewById(R.id.textView_Title);
                tvDetails = v.findViewById(R.id.textView_Details);
            }
        }

        public RecyclerViewCatalogAdapter(TreeMap<Integer, ItemClass_CatalogItem> data, Context _context) {
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
            ItemClass_CatalogItem ci;
            ci = treeMap.get(mapKeys[position]);
            final ItemClass_CatalogItem ci_final = ci;

            String sItemName = "";

            if (globalClass.ObfuscationOn) {

                //Get the obfuscation image index:
                int i = (position % globalClass.getObfuscationImageCount());
                //Get the obfuscation image resource ID:
                int iObfuscatorResourceID = globalClass.getObfuscationImage(i);

                Bitmap bmObfuscator = BitmapFactory.decodeResource(getResources(), iObfuscatorResourceID);
                holder.ivThumbnail.setImageBitmap(bmObfuscator);
                holder.tvThumbnailText.setText(globalClass.getObfuscationImageText(i));

                if (holder.btnDelete != null) {
                    //Don't allow delete during obfuscation.
                    holder.btnDelete.setVisibility(View.INVISIBLE);
                }
            } else {

                //Load the non-obfuscated image into the RecyclerView ViewHolder:
                String sThumbnailFilePath = globalClass.gfCatalogFolders[globalClass.giSelectedCatalogMediaCategory].getAbsolutePath() + File.separator
                        + ci.sFolder_Name + File.separator
                        + ci.sFilename;


                File fThumbnail = new File(sThumbnailFilePath);

                if (fThumbnail.exists()) {
                    Glide.with(getApplicationContext()).load(fThumbnail).into(holder.ivThumbnail);
                }

                String sThumbnailText = "";
                switch (globalClass.giSelectedCatalogMediaCategory) {
                    case GlobalClass.MEDIA_CATEGORY_VIDEOS:
                        String sTemp = ci.sFilename;
                        sItemName = GlobalClass.JumbleFileName(sTemp);
                        sThumbnailText = sItemName + ", " +
                                ci.sDuration_Text;
                        break;
                    case GlobalClass.MEDIA_CATEGORY_IMAGES:
                        sItemName = GlobalClass.JumbleFileName(ci.sFilename);
                        sThumbnailText = sItemName;
                        break;
                    case GlobalClass.MEDIA_CATEGORY_COMICS:
                        sItemName = ci.sComicName;
                        sThumbnailText = sItemName;
                        break;
                }
                holder.tvThumbnailText.setText(sThumbnailText);

                if (holder.btnDelete != null) {
                    holder.btnDelete.setVisibility(View.VISIBLE);
                }
            }


            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gbDebugTouch)
                        Toast.makeText(getApplicationContext(), "Click Item Number " + position, Toast.LENGTH_LONG).show();

                    if (globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                        StartVideoPlayerActivity(treeMap, Integer.parseInt(ci_final.sItemID));

                    } else if (globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                        //Temporarily set the image catalog to use the video player activity to display images until the
                        // SeriesImageViewer activity is genericized (was previously comic page viewer):
                        StartVideoPlayerActivity(treeMap, Integer.parseInt(ci_final.sItemID));

                    } else if (globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                        StartComicViewerActivity(ci_final);

                    }
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


            if (ci.sComic_Missing_Pages.equals("")) {
                holder.imageView_Attention.setVisibility(View.INVISIBLE);
            } else {
                holder.imageView_Attention.setVisibility(View.VISIBLE);
            }

            if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                holder.btnDelete.setVisibility(View.INVISIBLE);
            } else {
                holder.btnDelete.setVisibility(View.VISIBLE);
            }

            if(holder.btnDelete != null) {
                final String sItemNameToDelete = sItemName;
                holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Present confirmation that the user wishes to delete this item.
                        if (globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                            //Please use comic details view to delete comic.
                            Toast.makeText(getApplicationContext(), "Select comic and delete from comic preview.", Toast.LENGTH_SHORT).show();
                        }
                        String sConfirmationMessage = "Confirm item deletion: " + sItemNameToDelete;

                        AlertDialog.Builder builder = new AlertDialog.Builder(Activity_CatalogViewer.this, R.style.AlertDialogCustomStyle);
                        builder.setTitle("Delete Item");
                        builder.setMessage(sConfirmationMessage);
                        //builder.setIcon(R.drawable.ic_launcher);
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                Service_CatalogViewer.startActionDeleteCatalogItem(getApplicationContext(), ci_final);
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

    private int popCalls = 0;
    public void populate_RecyclerViewCatalogItems(){

        popCalls++; //This line just to allow catch of the debugger here. Call after is optimized away.
        if(gProgressBar_CatalogSortProgress != null && gTextView_CatalogSortProgressBarText != null){
            gProgressBar_CatalogSortProgress.setVisibility(View.VISIBLE);
            gTextView_CatalogSortProgressBarText.setVisibility(View.VISIBLE);
        }
        Service_CatalogViewer.startActionSortAndFilterCatalogDisplay(this);

    }


    //=====================================================================================
    //===== Player/Viewer Code =================================================================
    //=====================================================================================

    public final static String RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_ID = "RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_KEY";
    private void StartVideoPlayerActivity(TreeMap<Integer, ItemClass_CatalogItem> treeMap, Integer iVideoID) {
        //Key is the TreeMap Key for the selected video.

        //A timestamp for last viewed is handled within the video player. This is because the
        //  user can swipe left or right in the player to play an adjacent video. The adjacent video
        //  needs its last viewed timestamp to be updated as well.

        //Start the video player:
        Intent intentVideoPlayer = new Intent(this, Activity_VideoPlayer.class);
        //intentVideoPlayer.putExtra(RECYCLERVIEW_VIDEO_TREEMAP_FILTERED, treeMap);
        globalClass.gtmCatalogViewerDisplayTreeMap = treeMap;
        intentVideoPlayer.putExtra(RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_ID, iVideoID);
        if(toastLastToastMessage != null){
            toastLastToastMessage.cancel(); //Hide any toast message that might be shown.
        }
        startActivity(intentVideoPlayer);
    }

    public void StartComicViewerActivity(ItemClass_CatalogItem ci){

        //Record the COMIC_DATETIME_LAST_READ_BY_USER:
        ci.dDatetime_Last_Viewed_by_User = GlobalClass.GetTimeStampFloat();

        globalClass.CatalogDataFile_UpdateRecord(ci);


        Intent intentComicViewer = new Intent(this, Activity_ComicDetails.class);

        intentComicViewer.putExtra(Activity_ComicDetails.EXTRA_CATALOG_ITEM, ci);

        if(toastLastToastMessage != null){
            toastLastToastMessage.cancel(); //Hide any toast message that might be shown.
        }

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
        //gRecyclerViewCatalogAdapter.notifyDataSetChanged();
    }



}