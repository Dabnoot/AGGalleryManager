package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Activity_CatalogViewer extends AppCompatActivity {


    //Global Variables:
    private GlobalClass globalClass;
    private final boolean gbDebugTouch = false;
    RecyclerView gRecyclerView;
    int giRecyclerViewLastSelectedPosition = -1;

    Toast toastLastToastMessage;

    ProgressBar gProgressBar_CatalogSortProgress;
    TextView gTextView_CatalogSortProgressBarText;

    private CatalogViewerServiceResponseReceiver catalogViewerServiceResponseReceiver;

    boolean gbWriteApplicationLog = false;
    String gsApplicationLogFilePath = "";

    private Fragment_CatalogSort gFragment_CatalogSort;
    private Fragment_CatalogDataEditor gFragment_CatalogDataEditor;

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

        if(GlobalClass.giSelectedCatalogMediaCategory == null){
            ApplicationLogWriter("Selected media category is null. Returning to Main Activity.");
            finish();
            return;
        }

        ApplicationLogWriter("OnCreate Start");

        ApplicationLogWriter("Notifying zero catalog items if applicable");
        //Update TextView to show 0 catalog items if applicable:
        notifyZeroCatalogItemsIfApplicable();

        ApplicationLogWriter("Obtaining preferences");

        //Pull sort-by and sort-order from preferences (recall user's last selection). Note that this item is modified by the import process so that the user can
        //  see the item that they last imported.
        globalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory] =
                sharedPreferences.getInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[GlobalClass.giSelectedCatalogMediaCategory], GlobalClass.SORT_BY_DATETIME_LAST_VIEWED);
        globalClass.gbCatalogViewerSortAscending[GlobalClass.giSelectedCatalogMediaCategory] =
                sharedPreferences.getBoolean(GlobalClass.gsCatalogViewerPreferenceNameSortAscending[GlobalClass.giSelectedCatalogMediaCategory], true);

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
            args.putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, GlobalClass.giSelectedCatalogMediaCategory);
            gFragment_CatalogSort.setArguments(args);
            fragmentTransaction.replace(R.id.fragment_Catalog_Sort, gFragment_CatalogSort);
            fragmentTransaction.commit();
        }

        final DrawerLayout drawer_layout_sort = findViewById(R.id.drawer_layout_sort);
        drawer_layout_sort.openDrawer(GravityCompat.START); //Start the drawer open so that the user knows it's there.
        drawer_layout_sort.postDelayed(new Runnable() { //Configure a runnable to close the drawer after a timeout.
            @Override
            public void run() {
                drawer_layout_sort.closeDrawer(GravityCompat.START);
            }
        }, 1500);

        //Populate the CatalogDataEditor fragment:
        if(gFragment_CatalogDataEditor == null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            gFragment_CatalogDataEditor = new Fragment_CatalogDataEditor();

            Bundle args = new Bundle();
            args.putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, GlobalClass.giSelectedCatalogMediaCategory);
            gFragment_CatalogDataEditor.setArguments(args);
            fragmentTransaction.replace(R.id.fragment_Catalog_Data_Editor, gFragment_CatalogDataEditor);
            fragmentTransaction.commit();
        }

        final DrawerLayout drawer_layout_data = findViewById(R.id.drawer_layout_data);
        drawer_layout_data.openDrawer(GravityCompat.END); //Start the drawer open so that the user knows it's there.
        drawer_layout_data.postDelayed(new Runnable() { //Configure a runnable to close the drawer after a timeout.
            @Override
            public void run() {
                drawer_layout_data.closeDrawer(GravityCompat.END);
            }
        }, 1500);

        populate_RecyclerViewCatalogItems();


    ApplicationLogWriter("OnCreate End");

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(catalogViewerServiceResponseReceiver);
        super.onDestroy();
    }

    public void notifyZeroCatalogItemsIfApplicable(){

        //Update TextView to show 0 items if applicable:
        TextView textView_CatalogStatus = findViewById(R.id.textView_CatalogStatus);
        try {
            if (globalClass.gtmCatalogLists.get(GlobalClass.giSelectedCatalogMediaCategory).size() == 0) {
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
        }

    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        //Display a message showing the name of the item selected.
        return super.onOptionsItemSelected(item);
    }

    public void CloseSortDrawer(){
        final DrawerLayout drawer_layout_sort = findViewById(R.id.drawer_layout_sort);
        drawer_layout_sort.closeDrawer(GravityCompat.START);
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
                    RecyclerViewCatalogAdapter gRecyclerViewCatalogAdapter = new RecyclerViewCatalogAdapter(globalClass.gtmCatalogViewerDisplayTreeMap);
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
                if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS){
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

            StopWatch stopWatch = new StopWatch(false);
            stopWatch.Start();
            String sWatchMessageBase = "Activity_CatalogViewer:RecyclerViewCatalogAdapter:onBindViewHolder:";
            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Getting catalog item data from treemap. ");

            //Get the data for the row:
            ItemClass_CatalogItem ci;
            ci = treeMap.get(mapKeys[position]);
            final ItemClass_CatalogItem ci_final = ci;
            assert ci_final != null;

            String sItemName = "";

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Catalog item data gathered from treemap. ");

            //Load the non-obfuscated image into the RecyclerView ViewHolder:

            Uri uriThumbnailUri;
            boolean bThumbnailQuickLookupSuccess;

            String sFileName = ci.sThumbnail_File;
            if(sFileName.equals("")){
                sFileName = ci.sFilename;
            }
            String sPath = GlobalClass.gsCatalogFolderNames[ci.iMediaCategory]
                    + GlobalClass.gsFileSeparator + ci.sFolder_Name
                    + GlobalClass.gsFileSeparator + sFileName;
            if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                //If this is an m3u8 video style catalog item, configure the path to the file to use as the thumbnail.
                sPath = GlobalClass.gsCatalogFolderNames[ci.iMediaCategory]
                        + GlobalClass.gsFileSeparator + ci.sFolder_Name
                        + GlobalClass.gsFileSeparator + ci.sItemID
                        + GlobalClass.gsFileSeparator + ci.sThumbnail_File; //ci.sFilename will be the m3u8 file name in this case.
            }
            String sThumbnailUri = GlobalClass.gsUriAppRootPrefix
                    + GlobalClass.gsFileSeparator + sPath;
            uriThumbnailUri = Uri.parse(sThumbnailUri);

            bThumbnailQuickLookupSuccess = GlobalClass.CheckIfFileExists(uriThumbnailUri);

            if(!bThumbnailQuickLookupSuccess) {
                Uri uriCatalogItemFolder;
                uriCatalogItemFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.giSelectedCatalogMediaCategory].toString(), ci.sFolder_Name);

                if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS &&
                        ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_COMIC_DLM_MOVE) {
                    //If this is a comic, and the files from DownloadManager have not been moved as
                    //  part of download post-processing, look in the [comic]\download folder for the files:
                    if (uriCatalogItemFolder != null) {
                        Uri uriDLTempFolder = GlobalClass.FormChildUri(uriCatalogItemFolder.toString(), GlobalClass.gsDLTempFolderName);
                        if (uriDLTempFolder != null) {
                            uriThumbnailUri = GlobalClass.FormChildUri(uriDLTempFolder.toString(), ci.sFilename);
                        }
                    }
                }
                if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                    if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_DLM_CONCAT) {
                        //We are not doing anything with this item.
                        uriThumbnailUri = null;
                    } else if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                        //If this is a local M3U8, locate the downloaded thumbnail image or first video to present as thumbnail.
                        Uri uriVideoTagFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].toString(), ci.sFolder_Name);

                        if (uriVideoTagFolder != null) {
                            Uri uriVideoWorkingFolder = GlobalClass.FormChildUri(uriVideoTagFolder.toString(), ci.sItemID);

                            if (uriVideoWorkingFolder != null) {
                                Uri uriDownloadedThumbnailFile = GlobalClass.FormChildUri(uriVideoWorkingFolder.toString(), ci.sThumbnail_File);

                                if (uriDownloadedThumbnailFile != null) { //isDir if ci.sThum=="".
                                    uriThumbnailUri = uriDownloadedThumbnailFile;
                                } else {
                                    //If there is no downloaded thumbnail file, find the first .ts file and use that for the thumbnail:
                                    boolean bVideoFileFound = false;
                                    Uri uriM3U8File = GlobalClass.FormChildUri(uriVideoWorkingFolder.toString(), ci.sFilename);
                                    if (uriM3U8File != null) {
                                        try {
                                            InputStream isM3U8File = GlobalClass.gcrContentResolver.openInputStream(uriM3U8File);
                                            if (isM3U8File != null) {
                                                BufferedReader brReader;
                                                brReader = new BufferedReader(new InputStreamReader(isM3U8File));
                                                String sLine = brReader.readLine();
                                                while (sLine != null) {
                                                    if (!sLine.startsWith("#") && sLine.contains(".st")) {
                                                        Uri uriThumbnailFileCandidate = GlobalClass.FormChildUri(uriVideoWorkingFolder.toString(), sLine);
                                                        if (uriThumbnailFileCandidate != null) {
                                                            uriThumbnailUri = uriThumbnailFileCandidate;
                                                            bVideoFileFound = true;
                                                            break;
                                                        }
                                                    }
                                                    // read next line
                                                    sLine = brReader.readLine();
                                                }
                                                brReader.close();
                                                isM3U8File.close();
                                            }

                                        } catch (Exception e) {
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

                                }  //End if we had to look for a .ts file to serve as a thumbnail file.
                            } //End if unable to find video working folder DocumentFile.
                        } //End if unable to find video tag folder DocumentFile.
                    } //End if video is m3u8 style.
                } else {
                    if (holder.textView_CatalogItemNotification != null) { //Default to turn off text notification for this video item.
                        holder.textView_CatalogItemNotification.setVisibility(View.INVISIBLE);
                    }
                }
                if(uriThumbnailUri != null) {
                    if (!GlobalClass.CheckIfFileExists(uriThumbnailUri)) {
                        uriThumbnailUri = null;
                    }
                }
            }


            if(uriThumbnailUri != null) {
                Glide.with(getApplicationContext())
                        .load(uriThumbnailUri)
                        .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                        .into(holder.ivThumbnail);
                stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Thumbnail load complete. ");
            } else {
                //Special behavior if this is a comic.
                boolean bFoundMissingComicThumbnail = false;
                if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                    //Check to see if the comic thumbnail was merely deleted such in the case if it were renamed or a duplicate, and if so select the next file (alphabetically) to be the thumbnail.
                    Uri uriComicFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].toString(), ci.sFolder_Name);


                    //Load the full path to each comic page into tmComicPages (sorts files):
                    TreeMap<String, String> tmSortByFileName = new TreeMap<>();
                    if(uriComicFolder != null){
                        ArrayList<String> sComicPages = GlobalClass.GetDirectoryFileNames(uriComicFolder);
                        if(sComicPages.size() > 0) {
                            for (String sComicPage : sComicPages) {
                                tmSortByFileName.put(GlobalClass.JumbleFileName(sComicPage), GlobalClass.FormChildUriString(uriComicFolder.toString(), sComicPage)); //de-jumble to get proper alphabetization.
                            }
                        }
                        //Assign the existing file to be the new thumbnail file:
                        if(tmSortByFileName.size() > 0) {
                            Map.Entry<String, String> mapNewComicThumbnail = tmSortByFileName.firstEntry();
                            if(mapNewComicThumbnail != null) {
                                ci.sFilename = GlobalClass.JumbleFileName(mapNewComicThumbnail.getKey()); //re-jumble to get actual file name.
                                uriThumbnailUri = Uri.parse(mapNewComicThumbnail.getValue());
                                bFoundMissingComicThumbnail = true;
                            }
                        }
                    }

                }

                if(bFoundMissingComicThumbnail){
                    Glide.with(getApplicationContext())
                            .load(uriThumbnailUri)
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
            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Thumbnail image loaded. ");

            String sThumbnailText = "";
            switch (GlobalClass.giSelectedCatalogMediaCategory) {
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

            if(sThumbnailText.length() > 100){
                sThumbnailText = sThumbnailText.substring(0, 100) + "...";
            }

            holder.tvThumbnailText.setText(sThumbnailText);

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Thumbnail text set. ");

            if (holder.btnDelete != null) {
                holder.btnDelete.setVisibility(View.VISIBLE);
            }


            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    giRecyclerViewLastSelectedPosition = position; //To allow scroll back to this position if the user edits the item and RecyclerView refreshes.
                    if (gbDebugTouch)
                        Toast.makeText(getApplicationContext(), "Click Item Number " + position, Toast.LENGTH_LONG).show();

                    if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                        StartVideoPlayerActivity(treeMap, Integer.parseInt(ci_final.sItemID));

                    } else if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                        //Temporarily set the image catalog to use the video player activity to display images until the
                        // SeriesImageViewer activity is genericized (was previously comic page viewer):
                        StartVideoPlayerActivity(treeMap, Integer.parseInt(ci_final.sItemID));

                    } else if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                        StartComicViewerActivity(ci_final);
                    }
                }
            });

            if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                if (ci.sComic_Missing_Pages.equals("")) {
                    holder.imageView_Attention.setVisibility(View.INVISIBLE);
                    holder.textView_AttentionNote.setVisibility(View.INVISIBLE);
                } else {
                    holder.imageView_Attention.setVisibility(View.VISIBLE);
                    holder.textView_AttentionNote.setVisibility(View.VISIBLE);
                    String sAttentionNote = "Missing pages: " + ci.sComic_Missing_Pages;
                    holder.textView_AttentionNote.setText(sAttentionNote);
                }
            } else if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
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
                if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                    holder.btnDelete.setVisibility(View.INVISIBLE);
                } else {
                    holder.btnDelete.setVisibility(View.VISIBLE);
                }

                final String sItemNameToDelete = sItemName;
                holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Present confirmation that the user wishes to delete this item.
                        if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
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
            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "onBindViewHolder finished. ");
            stopWatch.Stop();
            stopWatch.Reset();
        }

        // Return the size of the data set (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return treeMap.size();
        }

    }

    public void populate_RecyclerViewCatalogItems(){
        globalClass.gbCatalogViewerRefresh = false;
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





}