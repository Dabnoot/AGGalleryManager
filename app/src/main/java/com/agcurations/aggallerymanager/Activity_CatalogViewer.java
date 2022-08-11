package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
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
import android.view.WindowManager;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeMap;

public class Activity_CatalogViewer extends AppCompatActivity {


    //Global Variables:
    private GlobalClass globalClass;
    private RecyclerViewCatalogAdapter gRecyclerViewCatalogAdapter;
    private final boolean gbDebugTouch = false;
    RecyclerView gRecyclerView;
    int giRecyclerViewLastSelectedPosition = -1;

    Spinner gspSpinnerSort;

    Toast toastLastToastMessage;

    ProgressBar gProgressBar_CatalogSortProgress;
    TextView gTextView_CatalogSortProgressBarText;

    private Menu ActivityMenu;

    private CatalogViewerServiceResponseReceiver catalogViewerServiceResponseReceiver;

    boolean gbWriteApplicationLog = false;
    String gsApplicationLogFilePath = "";

    private DrawerLayout gDrawerLayout;
    private Fragment_CatalogSort gFragment_CatalogSort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.MainTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog_viewer);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        gbWriteApplicationLog = sharedPreferences.getBoolean(GlobalClass.PREF_WRITE_APPLICATION_LOG_FILE, false);
        if(gbWriteApplicationLog){
            gsApplicationLogFilePath = sharedPreferences.getString(GlobalClass.PREF_APPLICATION_LOG_PATH_FILENAME, "");
        }

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        if(globalClass.giSelectedCatalogMediaCategory == null){
            ApplicationLogWriter("Selected media category is null. Returning to Main Activity.");
            finish();
            return;
        }

        ApplicationLogWriter("OnCreate Start");

        ApplicationLogWriter("Notifying zero catalog items if applicable");
        //Update TextView to show 0 catalog items if applicable:
        notifyZeroCatalogItemsIfApplicable();

        ApplicationLogWriter("Obtaining preferences");

        globalClass.gbCatalogViewerTagsRestrictionsOn = sharedPreferences.getBoolean("hide_restricted_tags", false);

        //Pull sort-by and sort-order from preferences (recall user's last selection). Note that this item is modified by the import process so that the user can
        //  see the item that they last imported.
        globalClass.giCatalogViewerSortBySetting[globalClass.giSelectedCatalogMediaCategory] =
                sharedPreferences.getInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[globalClass.giSelectedCatalogMediaCategory], GlobalClass.SORT_BY_DATETIME_LAST_VIEWED);
        globalClass.gbCatalogViewerSortAscending[globalClass.giSelectedCatalogMediaCategory] =
                sharedPreferences.getBoolean(GlobalClass.gsCatalogViewerPreferenceNameSortAscending[globalClass.giSelectedCatalogMediaCategory], true);

        ApplicationLogWriter("Configuring RecyclerView");

        gRecyclerView = findViewById(R.id.RecyclerView_CatalogItems);
        configure_RecyclerViewCatalogItems();

        ApplicationLogWriter("Creating ResponseReceiver");

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        catalogViewerServiceResponseReceiver = new CatalogViewerServiceResponseReceiver();
        //registerReceiver(importDataServiceResponseReceiver, filter);

        ApplicationLogWriter("Registering ResponseReceiver");

        LocalBroadcastManager.getInstance(this).registerReceiver(catalogViewerServiceResponseReceiver,filter);

        gProgressBar_CatalogSortProgress = findViewById(R.id.progressBar_CatalogSortProgress);
        gTextView_CatalogSortProgressBarText = findViewById(R.id.textView_CatalogSortProgressBarText);


        //Populate the CatalogSort fragment:
        if(gFragment_CatalogSort == null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            gFragment_CatalogSort = new Fragment_CatalogSort();

            Bundle args = new Bundle();
            args.putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, globalClass.giSelectedCatalogMediaCategory);
            gFragment_CatalogSort.setArguments(args);
            fragmentTransaction.replace(R.id.fragment_Catalog_Sort, gFragment_CatalogSort);
            fragmentTransaction.commit();
        }

        gDrawerLayout = findViewById(R.id.drawer_layout);
        gDrawerLayout.openDrawer(GravityCompat.START); //Start the drawer open so that the user knows it's there.
        gDrawerLayout.postDelayed(new Runnable() { //Configure a runnable to close the drawer after a timeout.
            @Override
            public void run() {
                closeDrawer();
            }
        }, 1500);

        ApplicationLogWriter("OnCreate End");

        //See additional initialization in onCreateOptionsMenu().
    }


    private void ApplicationLogWriter(String sMessage){
        if(gbWriteApplicationLog){
            try {
                File fLog = new File(gsApplicationLogFilePath);
                FileWriter fwLogFile = new FileWriter(fLog, true);
                fwLogFile.write(GlobalClass.GetTimeStampReadReady() + ": " + this.getLocalClassName() + ", " + sMessage + "\n");
                fwLogFile.close();
            } catch (Exception e) {
                sMessage = e.getMessage() + "";
                Log.d("Log FileWriter", sMessage);
            }
        }

    }

    @Override
    protected void onDestroy() {
        //unregisterReceiver(importDataServiceResponseReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(catalogViewerServiceResponseReceiver);
        super.onDestroy();
    }

    public void notifyZeroCatalogItemsIfApplicable(){

        //Update TextView to show 0 items if applicable:
        TextView textView_CatalogStatus = findViewById(R.id.textView_CatalogStatus);
        try {
            if (globalClass.gtmCatalogLists.get(globalClass.giSelectedCatalogMediaCategory).size() == 0) {
                textView_CatalogStatus.setVisibility(View.VISIBLE);
                String s = "Catalog contains 0 items.";
                textView_CatalogStatus.setText(s);
            } else {
                textView_CatalogStatus.setVisibility(View.INVISIBLE);
            }
        }catch (Exception e){
            ApplicationLogWriter(e.getMessage());
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

        populate_RecyclerViewCatalogItems();

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

        if(globalClass.gbCatalogViewerRefresh){
            //Typically enter here if data has been edited.
            populate_RecyclerViewCatalogItems();
            globalClass.gbCatalogViewerRefresh = false;

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
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to request to delete an item:
                boolean bIsDeleteItemResponse = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_DELETE_ITEM, false);
                if(bIsDeleteItemResponse) {
                    boolean bDeleteItemResult = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_DELETE_ITEM_RESULT, false);
                    if (bDeleteItemResult) {
                        populate_RecyclerViewCatalogItems(); //Refresh the catalog recycler view.
                    } else {
                        Toast.makeText(getApplicationContext(),"Could not successfully delete item.", Toast.LENGTH_LONG).show();
                    }
                }

                //Check to see if this is a response to request to SortAndFilterCatalogDisplay:
                boolean bRefreshCatalogDisplay = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_REFRESH_CATALOG_DISPLAY, false);
                if(bRefreshCatalogDisplay) {
                    //Catalog sort is complete.

                    if(gProgressBar_CatalogSortProgress != null) {
                        gProgressBar_CatalogSortProgress.setProgress(100);
                    }
                    if(gTextView_CatalogSortProgressBarText != null) {
                        String s = "100%";
                        gTextView_CatalogSortProgressBarText.setText(s);
                    }

                    //Apply the new TreeMap to the RecyclerView:
                    gRecyclerViewCatalogAdapter = new RecyclerViewCatalogAdapter(globalClass.gtmCatalogViewerDisplayTreeMap);
                    gRecyclerView.setAdapter(gRecyclerViewCatalogAdapter);
                    gRecyclerViewCatalogAdapter.notifyDataSetChanged();
                    if(giRecyclerViewLastSelectedPosition > -1){
                        gRecyclerView.scrollToPosition(giRecyclerViewLastSelectedPosition); //Scroll RecyclerView back to the last item selected by the user, due to refresh.
                        giRecyclerViewLastSelectedPosition = -1;
                    }
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
                bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_CatalogSortProgress != null) {
                        gProgressBar_CatalogSortProgress.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
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

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView ivThumbnail;
            public final ImageView imageView_Attention;
            public final TextView textView_AttentionNote;
            public final Button btnDelete;
            public final TextView tvThumbnailText;
            public final TextView tvDetails;
            public final TextView textView_CatalogItemNotification;

            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.imageView_Thumbnail);
                imageView_Attention = v.findViewById(R.id.imageView_Attention);
                textView_AttentionNote = v.findViewById(R.id.textView_AttentionNote);
                btnDelete = v.findViewById(R.id.button_Delete);
                tvThumbnailText = v.findViewById(R.id.textView_Title);
                tvDetails = v.findViewById(R.id.textView_Details);
                textView_CatalogItemNotification = v.findViewById(R.id.textView_CatalogItemNotification);
            }
        }

        public RecyclerViewCatalogAdapter(TreeMap<Integer, ItemClass_CatalogItem> data) {
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
            assert ci_final != null;

            String sItemName = "";



            //Load the non-obfuscated image into the RecyclerView ViewHolder:
            String sThumbnailFilePath = globalClass.gfCatalogFolders[globalClass.giSelectedCatalogMediaCategory].getAbsolutePath() + File.separator
                    + ci.sFolder_Name + File.separator
                    + ci.sFilename;
            if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS &&
                ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_COMIC_DLM_MOVE){
                //If this is a comic, and the files from DownloadManager have not been moved as
                //  part of download post-processing, look in the [comic]\download folder for the files:
                sThumbnailFilePath = globalClass.gfCatalogFolders[globalClass.giSelectedCatalogMediaCategory].getAbsolutePath() + File.separator
                        + ci.sFolder_Name + File.separator
                        + GlobalClass.gsDLTempFolderName + File.separator
                        + ci.sFilename;
            }
            if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_DLM_CONCAT) {
                    //If this is a video and the post-processing is incomplete...
                    //Every sort operation will attempt to relocate the file. However, we can
                    // look in the output folder for a result.
                    boolean bVideoFileFound = false;
                    String sVideoDestinationFolder = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath() +
                            File.separator + ci.sFolder_Name;
                    String sVideoWorkingFolder = sVideoDestinationFolder + File.separator + ci.sItemID;
                    File fVideoWorkingFolder = new File(sVideoWorkingFolder);
                    if (fVideoWorkingFolder.exists()) {
                        File[] fVideoDownloadFolderListing = fVideoWorkingFolder.listFiles();
                        ArrayList<File> alfOutputFolders = new ArrayList<>();
                        if (fVideoDownloadFolderListing != null) {
                            for (File f : fVideoDownloadFolderListing) {
                                //Locate the output folder
                                if (f.isDirectory()) {
                                    alfOutputFolders.add(f); //The worker could potentially create multiple output folders if it is re-run.
                                }
                            }
                            //Attempt to locate the output file of a concatenation operation:
                            for (File f : alfOutputFolders) {
                                String sOutputFileAbsolutePath = f.getAbsolutePath() + File.separator + ci.sFilename;
                                File fOutputFile = new File(sOutputFileAbsolutePath);
                                if (fOutputFile.exists()) {
                                    //Post-processing is complete but the output file has not yet been moved. Grab it for the thumbnail:
                                    sThumbnailFilePath = fOutputFile.getAbsolutePath();
                                    bVideoFileFound = true;
                                    break; //Don't go through any more "output" folders in this temp download directory.
                                }
                            }

                        }
                    }
                    if (!bVideoFileFound) {
                        if (holder.textView_CatalogItemNotification != null) {
                            //Notify the user that post-processing is incomplete:
                            holder.textView_CatalogItemNotification.setVisibility(View.VISIBLE);
                            String sMessage = "Item pending post-processing...";
                            holder.textView_CatalogItemNotification.setText(sMessage);
                        }
                    }


                } else if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                        //If this is a local M3U8, locate the downloaded thumbnail image or first video to present as thumbnail.
                    String sVideoDestinationFolder = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].getAbsolutePath() +
                            File.separator + ci.sFolder_Name;
                    String sVideoWorkingFolder = sVideoDestinationFolder + File.separator + ci.sItemID;
                    String sDownloadedThumbnailPath = sVideoWorkingFolder + File.separator + ci.sThumbnail_File;
                    File fDownloadedThumbnail = new File(sDownloadedThumbnailPath);
                    if (fDownloadedThumbnail.exists() && !fDownloadedThumbnail.isDirectory()) { //isDir if ci.sThum=="".
                        sThumbnailFilePath = sDownloadedThumbnailPath;
                    } else {
                        //If there is no downloaded thumbnail file, find the first .ts file and use that for the thumbnail:
                        boolean bVideoFileFound = false;
                        String sM3U8File = sVideoWorkingFolder + File.separator + ci.sFilename;
                        File fM3U8File = new File(sM3U8File);
                        if(fM3U8File.exists()) {
                            try {
                                BufferedReader brReader;
                                brReader = new BufferedReader(new FileReader(sM3U8File));
                                String sLine = brReader.readLine();
                                while (sLine != null) {
                                    if (!sLine.startsWith("#") && sLine.contains(".st")) {
                                        sThumbnailFilePath = sLine;
                                        File fThumbnail = new File(sThumbnailFilePath);
                                        if(fThumbnail.exists()) {
                                            bVideoFileFound = true;
                                            break;
                                        }
                                    }
                                    // read next line
                                    sLine = brReader.readLine();
                                }
                                brReader.close();

                            } catch (Exception e){
                                //Probably a file IO exception.
                                bVideoFileFound = false; //redundant, but don't want special behavior.
                            }
                        }
                        if (!bVideoFileFound) {
                            if (holder.textView_CatalogItemNotification != null) {
                                //Notify the user that post-processing is incomplete:
                                holder.textView_CatalogItemNotification.setVisibility(View.VISIBLE);
                                String sMessage = "Item pending post-processing...";
                                holder.textView_CatalogItemNotification.setText(sMessage);
                            }
                        }

                    }
                }
            } else {
                if (holder.textView_CatalogItemNotification != null) { //Default to turn off text notification for this video item.
                    holder.textView_CatalogItemNotification.setVisibility(View.INVISIBLE);
                }
            }


            File fThumbnail = new File(sThumbnailFilePath);

            if(fThumbnail.exists()) {
                Glide.with(getApplicationContext())
                        .load(fThumbnail)
                        .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                        .into(holder.ivThumbnail);
            } else {
                //Special behavior if this is a comic.
                boolean bFoundMissingComicThumbnail = false;
                if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                    //Check to see if the comic thumbnail was merely deleted such in the case if it were renamed or a duplicate, and if so select the next file (alphabetically) to be the thumbnail.
                    String sComicFolder_AbsolutePath = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getAbsolutePath();
                    String sComicFolderPath;
                    sComicFolderPath = sComicFolder_AbsolutePath + File.separator
                            + ci.sFolder_Name;

                    //Load the full path to each comic page into tmComicPages (sorts files):
                    File fComicFolder = new File(sComicFolderPath);
                    TreeMap<String, String> tmSortByFileName = new TreeMap<>();
                    if(fComicFolder.exists()){
                        File[] fComicPages = fComicFolder.listFiles();
                        if(fComicPages != null) {
                            for (File fComicPage : fComicPages) {
                                if(fComicPage.isFile()) {
                                    tmSortByFileName.put(GlobalClass.JumbleFileName(fComicPage.getName()), fComicPage.getAbsolutePath()); //de-jumble to get proper alphabetization.
                                }
                            }
                        }
                        //Assign the existing file to be the new thumbnail file:
                        if(tmSortByFileName.size() > 0) {
                            ci.sFilename = GlobalClass.JumbleFileName(Objects.requireNonNull(tmSortByFileName.firstEntry()).getKey()); //re-jumble to get actual file name.
                            bFoundMissingComicThumbnail = true;
                        }
                    }

                }

                if(bFoundMissingComicThumbnail){
                    Glide.with(getApplicationContext())
                            .load(fThumbnail)
                            .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                            .into(holder.ivThumbnail);
                    globalClass.CatalogDataFile_UpdateRecord(ci); //update the record with the new thumbnail file name.
                } else {
                    Glide.with(getApplicationContext())
                            .load(R.drawable.baseline_image_white_18dp_wpagepad)
                            .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                            .into(holder.ivThumbnail);
                }
            }

            String sThumbnailText = "";
            switch (globalClass.giSelectedCatalogMediaCategory) {
                case GlobalClass.MEDIA_CATEGORY_VIDEOS:
                    String sTemp = ci.sFilename;
                    sItemName = GlobalClass.JumbleFileName(sTemp);
                    if(!ci.sTitle.equals("")){
                        sItemName = ci.sTitle;
                    }
                    sThumbnailText = sItemName;
                    if(!ci.sDuration_Text.equals("")){
                        sThumbnailText = sThumbnailText  + ", " + ci.sDuration_Text;
                    }
                    sThumbnailText = ci.sItemID + " " + sThumbnailText; //For debugging.
                    break;
                case GlobalClass.MEDIA_CATEGORY_IMAGES:
                    sItemName = GlobalClass.JumbleFileName(ci.sFilename);
                    sThumbnailText = sItemName;
                    break;
                case GlobalClass.MEDIA_CATEGORY_COMICS:
                    sItemName = ci.sTitle;
                    sThumbnailText = sItemName;
                    break;
            }

            holder.tvThumbnailText.setText(sThumbnailText);

            if (holder.btnDelete != null) {
                holder.btnDelete.setVisibility(View.VISIBLE);
            }


            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    giRecyclerViewLastSelectedPosition = position; //To allow scroll back to this position if the user edits the item and RecyclerView refreshes.
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

            if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                if (ci.sComic_Missing_Pages.equals("")) {
                    holder.imageView_Attention.setVisibility(View.INVISIBLE);
                    holder.textView_AttentionNote.setVisibility(View.INVISIBLE);
                } else {
                    holder.imageView_Attention.setVisibility(View.VISIBLE);
                    holder.textView_AttentionNote.setVisibility(View.VISIBLE);
                    String sAttentionNote = "Missing pages: " + ci.sComic_Missing_Pages;
                    holder.textView_AttentionNote.setText(sAttentionNote);
                }
            } else if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                if (ci.lDuration_Milliseconds >= 0) {
                    holder.imageView_Attention.setVisibility(View.INVISIBLE);
                    holder.textView_AttentionNote.setVisibility(View.INVISIBLE);
                } else {
                    //Duration is <0 only when the source is from an online stream (M3U8), and suggests
                    //  that there was an error in the FFMPEG concatenation activity
                    holder.imageView_Attention.setVisibility(View.VISIBLE);
                    holder.textView_AttentionNote.setVisibility(View.VISIBLE);
                    String sAttentionNote = "Possible incomplete stream download.";
                    holder.textView_AttentionNote.setText(sAttentionNote);
                }
                if (ci.iAllVideoSegmentFilesDetected == ItemClass_CatalogItem.VIDEO_SEGMENT_FILES_KNOWN_INCOMPLETE){
                    holder.imageView_Attention.setVisibility(View.VISIBLE);
                    holder.textView_AttentionNote.setVisibility(View.VISIBLE);
                    String sAttentionNote = "Incomplete stream download.";
                    holder.textView_AttentionNote.setText(sAttentionNote);
                }

            }

            if(holder.btnDelete != null) {
                if(globalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                    holder.btnDelete.setVisibility(View.INVISIBLE);
                } else {
                    holder.btnDelete.setVisibility(View.VISIBLE);
                }

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
                                Service_CatalogViewer.startActionDeleteCatalogItem(
                                        getApplicationContext(), ci_final,
                                        "Activity_CatalogViewer:RecyclerViewCatalogAdapter.onBindViewHolder.btnDelete.OnClick",
                                        Activity_CatalogViewer.CatalogViewerServiceResponseReceiver.CATALOG_VIEWER_SERVICE_ACTION_RESPONSE);
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

        if(gProgressBar_CatalogSortProgress != null && gTextView_CatalogSortProgressBarText != null){
            gProgressBar_CatalogSortProgress.setVisibility(View.VISIBLE);
            gTextView_CatalogSortProgressBarText.setVisibility(View.VISIBLE);
        }
        Service_CatalogViewer.startActionSortAndFilterCatalogDisplay(this, "Activity_CatalogViewer:populate_RecyclerViewCatalogItems()");

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
        ci.dDatetime_Last_Viewed_by_User = GlobalClass.GetTimeStampDouble();

        globalClass.CatalogDataFile_UpdateRecord(ci);


        Intent intentComicViewer = new Intent(this, Activity_ComicDetails.class);

        intentComicViewer.putExtra(Activity_ComicDetails.EXTRA_CATALOG_ITEM_ID, ci.sItemID); //Pass item ID and load record from file. To accommodate comic detail edit.

        if(toastLastToastMessage != null){
            toastLastToastMessage.cancel(); //Hide any toast message that might be shown.
        }

        startActivity(intentComicViewer);
    }


    public void closeDrawer(){
        gDrawerLayout.closeDrawer(GravityCompat.START);
    }


}