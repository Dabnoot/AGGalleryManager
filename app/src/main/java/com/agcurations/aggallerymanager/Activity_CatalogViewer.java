package com.agcurations.aggallerymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

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

    RecyclerViewCatalogAdapter gRecyclerViewCatalogAdapter;

    LinearLayout gLinearLayout_GroupingModeNotifier;
    TextView gTextView_GroupIDClipboardLabel;
    TextView gTextView_GroupIDClipboard;
    ImageButton gImageButton_ClearGroupingClipboard;

    int giGroupControlImageButtonWidth;

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
        GlobalClass.giCatalogViewerSortBySetting[GlobalClass.giSelectedCatalogMediaCategory] =
                sharedPreferences.getInt(GlobalClass.gsCatalogViewerPreferenceNameSortBy[GlobalClass.giSelectedCatalogMediaCategory], GlobalClass.SORT_BY_DATETIME_LAST_VIEWED);
        GlobalClass.gbCatalogViewerSortAscending[GlobalClass.giSelectedCatalogMediaCategory] =
                sharedPreferences.getBoolean(GlobalClass.gsCatalogViewerPreferenceNameSortAscending[GlobalClass.giSelectedCatalogMediaCategory], true);

        ApplicationLogWriter("Configuring RecyclerView");

        gRecyclerView = findViewById(R.id.RecyclerView_CatalogItems);

        gRecyclerView.setHasFixedSize(true);
            // use this setting to improve performance if you know that changes in content do not
            // change the layout size of the RecyclerView

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

        gLinearLayout_GroupingModeNotifier = findViewById(R.id.linearLayout_GroupingModeNotifier);
        gTextView_GroupIDClipboardLabel = findViewById(R.id.textView_GroupIDClipboardLabel);
        gTextView_GroupIDClipboard = findViewById(R.id.textView_GroupIDClipboard);
        gImageButton_ClearGroupingClipboard = findViewById(R.id.imageButton_ClearGroupingClipboard);
        gImageButton_ClearGroupingClipboard.setOnClickListener(v -> {
            GlobalClass.gsGroupIDClip = "";
            updateVisibleRecyclerItems();
            gLinearLayout_GroupingModeNotifier.setVisibility(View.INVISIBLE);
        });

        ApplicationLogWriter("Creating ResponseReceiver");

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_DeleteItem.CATALOG_DELETE_ITEM_ACTION_RESPONSE);
        filter.addAction(Worker_CatalogViewer_SortAndFilterDisplayed.CATALOG_SORT_AND_FILTER_DISP_ACTION_RESPONSE);

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
        //Configure a runnable to close the drawer after a timeout.
        drawer_layout_sort.postDelayed(() ->
                drawer_layout_sort.closeDrawer(GravityCompat.START), 1500);

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
        //Configure a runnable to close the drawer after a timeout.
        drawer_layout_data.postDelayed(() ->
                drawer_layout_data.closeDrawer(GravityCompat.END), 1500);

        populate_RecyclerViewCatalogItems();

        float factor = getResources().getDisplayMetrics().density;
        giGroupControlImageButtonWidth = (int)(40 * factor);


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
            if (GlobalClass.gtmCatalogLists.get(GlobalClass.giSelectedCatalogMediaCategory).size() == 0) {
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

        if(GlobalClass.gbCatalogViewerRefresh){
            //Typically enter here if data has been edited.
            populate_RecyclerViewCatalogItems();
        } else if (!GlobalClass.gsRefreshCatalogViewerThumbnail.equals("")) {
            //Cause an update of the thumbnail for the specified item.
            gRecyclerViewCatalogAdapter.updateItem(GlobalClass.gsRefreshCatalogViewerThumbnail);
        }

    }

    public void CloseSortDrawer(){
        final DrawerLayout drawer_layout_sort = findViewById(R.id.drawer_layout_sort);
        drawer_layout_sort.closeDrawer(GravityCompat.START);
    }


    public class CatalogViewerServiceResponseReceiver extends BroadcastReceiver {

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
                        globalClass.updateTagHistogramsIfRequired(); //todo: Moved here after refactor of comic delete logic. Examine if this is appropriate.
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
                    gRecyclerViewCatalogAdapter = new RecyclerViewCatalogAdapter(GlobalClass.gtmCatalogViewerDisplayTreeMap);
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

                    int iItemCount = gRecyclerViewCatalogAdapter.getItemCount();
                    String sNoun = "item";
                    if(iItemCount != 1){
                        sNoun += "s";
                    }
                    toastLastToastMessage = Toast.makeText(getApplicationContext(), "Showing " + gRecyclerViewCatalogAdapter.getItemCount() + " " + sNoun + ".", Toast.LENGTH_SHORT);
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

    public class RecyclerViewCatalogAdapter extends RecyclerView.Adapter<RecyclerViewCatalogAdapter.ViewHolder> {

        private final TreeMap<Integer, ItemClass_CatalogItem> treeMap;
        private final Integer[] mapKeys;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView imageView_Thumbnail;
            public final ImageView imageView_Attention;
            public final TextView textView_AttentionNote;
            public final Button button_Delete;
            public final ImageButton imageButton_OpenGroupingControls;
            public final TextView textView_Title;
            public final TextView textView_Details;
            public final TextView textView_CatalogItemNotification;
            LinearLayout linearLayout_GroupingControls;
            TextView textView_LabelGroupID;
            TextView textView_GroupID;
            ImageButton imageButton_GroupIDNew;
            ImageButton imageButton_GroupIDCopy;
            ImageButton imageButton_GroupIDPaste;
            ImageButton imageButton_GroupIDRemove;
            ImageButton imageButton_GroupIDFilter;
            ImageButton imageButton_CloseGroupingControls;

            public ViewHolder(View v) {
                super(v);
                imageView_Thumbnail = v.findViewById(R.id.imageView_Thumbnail);
                imageView_Attention = v.findViewById(R.id.imageView_Attention);
                textView_AttentionNote = v.findViewById(R.id.textView_AttentionNote);
                button_Delete = v.findViewById(R.id.button_Delete);
                textView_Title = v.findViewById(R.id.textView_Title);
                textView_Details = v.findViewById(R.id.textView_Details);
                textView_CatalogItemNotification = v.findViewById(R.id.textView_CatalogItemNotification);

                imageButton_OpenGroupingControls = v.findViewById(R.id.imageButton_OpenGroupingControls);
                linearLayout_GroupingControls = v.findViewById(R.id.linearLayout_GroupingControls);
                textView_LabelGroupID = v.findViewById(R.id.textView_LabelGroupID);
                textView_GroupID = v.findViewById(R.id.textView_GroupID);
                imageButton_GroupIDNew = v.findViewById(R.id.imageButton_GroupIDNew);
                imageButton_GroupIDCopy = v.findViewById(R.id.imageButton_GroupIDCopy);
                imageButton_GroupIDPaste = v.findViewById(R.id.imageButton_GroupIDPaste);
                imageButton_GroupIDRemove = v.findViewById(R.id.imageButton_GroupIDRemove);
                imageButton_GroupIDFilter = v.findViewById(R.id.imageButton_GroupIDFilter);
                imageButton_CloseGroupingControls = v.findViewById(R.id.imageButton_CloseGroupingControls);
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

            StopWatch stopWatch = new StopWatch(false); //enable/disable essentially turns the usage of this item on/off.
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
            boolean bThumbnailQuickLookupSuccess = true;

            String sFileName = ci.sThumbnail_File;
            if(sFileName.equals("")){
                sFileName = ci.sFilename;
            }
            String sPath = GlobalClass.gsCatalogFolderNames[ci.iMediaCategory]
                    + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                    + GlobalClass.gsFileSeparator + sFileName;
            if (ci.iSpecialFlag == ItemClass_CatalogItem.FLAG_VIDEO_M3U8) {
                //If this is an m3u8 video style catalog item, configure the path to the file to use as the thumbnail.
                sPath = GlobalClass.gsCatalogFolderNames[ci.iMediaCategory]
                        + GlobalClass.gsFileSeparator + ci.sFolderRelativePath
                        + GlobalClass.gsFileSeparator + ci.sThumbnail_File; //ci.sFilename will be the m3u8 file name in this case.
            }
            String sThumbnailUri = GlobalClass.gsUriAppRootPrefix
                    + GlobalClass.gsFileSeparator + sPath;
            uriThumbnailUri = Uri.parse(sThumbnailUri);

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Thumbnail Uri development complete.");

            if(GlobalClass.gbUseCatalogItemThumbnailDeepSearch) {
                //Check to see if the thumbnail source is where it is supposed to be. If it is not
                //  there, check for other related happenings that might identify the location.
                //  This can add a little more tha 1/100th of a second to processing the thumbnail,
                //  and in testing resulted in a stutter of the recyclerView.
                bThumbnailQuickLookupSuccess = GlobalClass.CheckIfFileExists(uriThumbnailUri);
                stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Thumbnail file verification complete.");
            }

            if(!bThumbnailQuickLookupSuccess) {
                Uri uriCatalogItemFolder;
                uriCatalogItemFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.giSelectedCatalogMediaCategory].toString(), ci.sFolderRelativePath);

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
                        Uri uriVideoTagFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_VIDEOS].toString(), ci.sFolderRelativePath);

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
                stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "First thumbnail lookup didn't work. Second thumbnail lookup attempt complete.");
            }


            if(uriThumbnailUri != null) {
                if(!GlobalClass.gsRefreshCatalogViewerThumbnail.equals(ci.sItemID)) {
                    //If a command to refresh a thumbnail is "" or not equal to a specified item,
                    // let Glide load the image using whatever disk strategy it has been using.
                    Glide.with(getApplicationContext())
                            .load(uriThumbnailUri)
                            .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                            .into(holder.imageView_Thumbnail);
                } else {
                    //Ignore cache. Used when the user updates a thumbnail.
                    Glide.with(getApplicationContext())
                            .load(uriThumbnailUri)
                            .diskCacheStrategy(DiskCacheStrategy.NONE ) //This will only affect this one call.
                            .skipMemoryCache(true)
                            .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                            .into(holder.imageView_Thumbnail);

                    //IF Glide is misbehaving, provide the option to clear the cache and memory:
                    //https://bumptech.github.io/glide/doc/caching.html#cache-configuration
                    //Glide.get(context).clearMemory();
                    //Glide.get(applicationContext).clearDiskCache();

                    GlobalClass.gsRefreshCatalogViewerThumbnail = "";
                }
                stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Thumbnail load complete. ");
            } else {
                //Special behavior if this is a comic.
                boolean bFoundMissingComicThumbnail = false;
                if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                    //Check to see if the comic thumbnail was merely deleted such in the case if it were renamed or a duplicate, and if so select the next file (alphabetically) to be the thumbnail.
                    Uri uriComicFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].toString(), ci.sFolderRelativePath);


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
                            .into(holder.imageView_Thumbnail);
                    globalClass.CatalogDataFile_UpdateRecord(ci); //update the record with the new thumbnail file name.
                } else {
                    Glide.with(getApplicationContext())
                            .load(R.drawable.baseline_image_white_18dp_wpagepad)
                            .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                            .into(holder.imageView_Thumbnail);
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
                    break;
                case GlobalClass.MEDIA_CATEGORY_IMAGES:
                    sItemName = GlobalClass.JumbleFileName(ci.sFilename);
                    sThumbnailText = sItemName;
                    break;
                case GlobalClass.MEDIA_CATEGORY_COMICS:
                    sItemName = ci.sTitle;
                    sThumbnailText = sItemName;
                    if(!ci.sComicVolume.equals("") || !ci.sComicChapter.equals("")) {
                        sThumbnailText = sThumbnailText + ", ";
                        if (!ci.sComicVolume.equals("")) {
                            sThumbnailText = sThumbnailText + "Volume " + ci.sComicVolume;
                            if (!ci.sComicChapter.equals("")) {
                                sThumbnailText = sThumbnailText + ", ";
                            }
                        }
                        if (!ci.sComicChapter.equals("")) {
                            sThumbnailText = sThumbnailText + "Chapter " + ci.sComicChapter;
                        }
                    }
                    if(!ci.sComicChapterSubtitle.equals("")){
                        sThumbnailText = sThumbnailText + " - " + ci.sComicChapterSubtitle;
                    }

                    break;
            }

            if(sThumbnailText.length() > 100){
                sThumbnailText = sThumbnailText.substring(0, 100) + "...";
            }

            holder.textView_Title.setText(sThumbnailText);

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Thumbnail text set. ");



            holder.imageView_Thumbnail.setOnClickListener(v -> {
                giRecyclerViewLastSelectedPosition = holder.getAbsoluteAdapterPosition(); //To allow scroll back to this position if the user edits the item and RecyclerView refreshes.
                //https://stackoverflow.com/questions/34942840/lint-error-do-not-treat-position-as-fixed-only-use-immediately
                if (gbDebugTouch){
                    Toast.makeText(getApplicationContext(), "Click Item Number " + holder.getAbsoluteAdapterPosition(), Toast.LENGTH_LONG).show();
                }

                if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                    StartVideoPlayerActivity(treeMap, ci_final.sItemID);

                } else if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                    //Temporarily set the image catalog to use the video player activity to display images until the
                    // SeriesImageViewer activity is genericized (was previously comic page viewer):
                    StartVideoPlayerActivity(treeMap, ci_final.sItemID);

                } else if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                    StartComicViewerActivity(ci_final);
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

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Catalog item attention icon and text configured.");

            if(holder.button_Delete != null) {

                final String sItemNameToDelete = sItemName;
                holder.button_Delete.setOnClickListener(view -> {
                    //Present confirmation that the user wishes to delete this item.
                    String sConfirmationMessage = "Confirm item deletion: " + sItemNameToDelete;

                    AlertDialog.Builder builder = new AlertDialog.Builder(Activity_CatalogViewer.this, R.style.AlertDialogCustomStyle);
                    builder.setTitle("Delete Item");
                    builder.setMessage(sConfirmationMessage);
                    //builder.setIcon(R.drawable.ic_launcher);
                    builder.setPositiveButton("Yes", (dialog, id) -> {
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Deleting item...", Toast.LENGTH_LONG).show();

                        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                        String sCatalogRecord = GlobalClass.getCatalogRecordString(ci_final);
                        Data dataCatalogDeleteItem = new Data.Builder()
                                .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_CatalogViewer:btnDelete.OnClickListener.OnClick")
                                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                                .putString(GlobalClass.EXTRA_CATALOG_ITEM, sCatalogRecord)
                                .build();
                        OneTimeWorkRequest otwrCatalogDeleteItem = new OneTimeWorkRequest.Builder(Worker_Catalog_DeleteItem.class)
                                .setInputData(dataCatalogDeleteItem)
                                .addTag(Worker_Catalog_DeleteItem.TAG_WORKER_CATALOG_DELETEITEM) //To allow finding the worker later.
                                .build();
                        WorkManager.getInstance(getApplicationContext()).enqueue(otwrCatalogDeleteItem);
                    });
                    builder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());
                    AlertDialog adConfirmationDialog = builder.create();
                    adConfirmationDialog.show();


                });
            }

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Catalog item delete button configured.");

            if(holder.linearLayout_GroupingControls != null &&
                    holder.imageButton_OpenGroupingControls != null &&
                    holder.textView_GroupID != null &&
                    holder.imageButton_GroupIDNew != null &&
                    holder.imageButton_GroupIDCopy!= null &&
                    holder.imageButton_GroupIDPaste!= null &&
                    holder.imageButton_GroupIDRemove!= null) {
                //Todo: Map the logic for this section and reorganize. Showing the controls, partial controls,
                //   applying coloring, is complicated logic.
                //Controls for:
                //  -Group ID Shown
                //  -Group ID new button
                //  -Group ID copy button
                //  -Group ID paste button
                //  -Group ID remove button
                //  -Group ID filter button
                //  -Group ID clear filter button

                //Logic:
                /*
                * If the user has clicked the group icon, show group control panel for the item.
                * If the user has copied a group ID, show group control panel for all items.
                *
                *
                * */

                ImageButton[] ibGroupingControls = new ImageButton[]{
                        holder.imageButton_GroupIDNew,
                        holder.imageButton_GroupIDCopy,
                        holder.imageButton_GroupIDPaste,
                        holder.imageButton_GroupIDRemove,
                        holder.imageButton_GroupIDFilter,
                        holder.imageButton_CloseGroupingControls
                };
                TextView[] tvGroupingTextViews = new TextView[]{
                        holder.textView_LabelGroupID,
                        holder.textView_GroupID
                };


                if(ci.bShowGroupingControls || !GlobalClass.gsGroupIDClip.equals("")){
                    //If the user has opened the grouping controls for this item or if the user
                    //  has copied a GroupID to the internal clipboard, show the grouping controls.
                    holder.linearLayout_GroupingControls.setVisibility(View.VISIBLE);
                    holder.imageButton_OpenGroupingControls.setVisibility(View.INVISIBLE);

                    //If controls are shown, you must calc the colors every time otherwise it will recycle colors.
                    if(!ci.bColorsCalculated) {
                        if (!ci.sGroupID.equals("")) {
                            int[] iColors = GlobalClass.calculateGroupingControlsColors(ci.sGroupID);
                            ci.iGroupingControlsColor = iColors[0];
                            ci.iGroupingControlsContrastColor = iColors[1];
                            ci.iGroupingControlHighlight = iColors[2];
                            ci.iGroupingControlHighlightContrastColor = iColors[3];
                            ci.bColorsCalculated = true;
                        } else {
                            //Specify "no-group" colors:
                            ci.iGroupingControlsColor = ContextCompat.getColor(getApplicationContext(), R.color.colorBlack);
                            ci.iGroupingControlsContrastColor = ContextCompat.getColor(getApplicationContext(), R.color.colorTextColor);
                            ci.iGroupingControlHighlight = 0;   //Does not matter without an assigned group ID - used to indicate that the filter is on.
                            ci.iGroupingControlHighlightContrastColor = 0;
                        }
                    }
                    GlobalClass.applyGroupingControlsColor(
                            ci,
                            holder.linearLayout_GroupingControls,
                            ibGroupingControls,
                            tvGroupingTextViews);
                } else {
                    holder.linearLayout_GroupingControls.setVisibility(View.INVISIBLE);
                    holder.imageButton_OpenGroupingControls.setVisibility(View.VISIBLE);
                }

                if(!ci.sGroupID.equals("")){
                    if(!GlobalClass.gsCatalogViewerSearchByGroupID[GlobalClass.giSelectedCatalogMediaCategory].equals("")) {
                        //If a filter is on for a given group ID, all of the shown items should be items belonging to a group
                        //  with the filter icon showing.
                        if (ci.sGroupID.equals(GlobalClass.gsCatalogViewerSearchByGroupID[GlobalClass.giSelectedCatalogMediaCategory])) {
                            //ci.bSearchByGroupID = true;
                            holder.imageButton_GroupIDFilter.setBackgroundColor(ci.iGroupingControlHighlight);
                            holder.imageButton_GroupIDFilter.setColorFilter(ci.iGroupingControlHighlightContrastColor);
                        }
                    } else {
                        //ci.bSearchByGroupID = false;
                    }
                }

                holder.imageButton_OpenGroupingControls.setOnClickListener(new View.OnClickListener() {
                    //This is the button that the user clicks to show the grouping controls
                    @Override
                    public void onClick(View v) {
                        holder.linearLayout_GroupingControls.setVisibility(View.VISIBLE);
                        holder.imageButton_OpenGroupingControls.setVisibility(View.INVISIBLE);
                        ci.bShowGroupingControls = true;
                        //Open the grouping controls for all other items with the same group ID:
                        boolean bOtherGroupItemsFound = false;
                        for(Map.Entry<Integer, ItemClass_CatalogItem> entry: treeMap.entrySet()){
                            ItemClass_CatalogItem icci = entry.getValue();
                            if(!icci.sGroupID.equals("")){
                                if(icci.sGroupID.equals(ci.sGroupID)){
                                    icci.bShowGroupingControls = true;
                                    bOtherGroupItemsFound = true;
                                }
                            }
                        }
                        if(bOtherGroupItemsFound){
                            updateVisibleRecyclerItems();
                        }
                    }
                });


                if (ci.sGroupID.equals("")) {
                    holder.textView_GroupID.setText("----");
                    setGroupControlSize(holder.imageButton_GroupIDCopy, 0);
                    setGroupControlSize(holder.imageButton_GroupIDFilter, 0);
                    setGroupControlSize(holder.imageButton_GroupIDRemove, 0);
                } else {
                    holder.textView_GroupID.setText(ci.sGroupID);
                    setGroupControlSize(holder.imageButton_GroupIDCopy, giGroupControlImageButtonWidth);
                    setGroupControlSize(holder.imageButton_GroupIDFilter, giGroupControlImageButtonWidth);
                    setGroupControlSize(holder.imageButton_GroupIDRemove, giGroupControlImageButtonWidth);
                }

                holder.imageButton_GroupIDNew.setOnClickListener(v -> {
                    ci.sGroupID = GlobalClass.getNewGroupID();
                    int[] iColors = GlobalClass.calculateGroupingControlsColors(ci.sGroupID);
                    ci.iGroupingControlsColor = iColors[0];
                    ci.iGroupingControlsContrastColor = iColors[1];
                    ci.iGroupingControlHighlight = iColors[2];
                    ci.iGroupingControlHighlightContrastColor = iColors[3];
                    holder.textView_GroupID.setText(ci.sGroupID);
                    setGroupControlSize(holder.imageButton_GroupIDCopy, giGroupControlImageButtonWidth);
                    setGroupControlSize(holder.imageButton_GroupIDFilter, giGroupControlImageButtonWidth);
                    setGroupControlSize(holder.imageButton_GroupIDRemove, giGroupControlImageButtonWidth);
                    GlobalClass.applyGroupingControlsColor(
                            ci,
                            holder.linearLayout_GroupingControls,
                            ibGroupingControls,
                            tvGroupingTextViews);
                    Toast.makeText(getApplicationContext(), "New group ID generated.", Toast.LENGTH_SHORT).show();
                    globalClass.CatalogDataFile_UpdateCatalogFile(ci.iMediaCategory, "Saving...");
                });

                holder.imageButton_GroupIDCopy.setOnClickListener(v -> {
                    boolean bGroupControlsAlreadyOpen = !GlobalClass.gsGroupIDClip.equals("");
                    GlobalClass.gsGroupIDClip = ci.sGroupID;
                    gLinearLayout_GroupingModeNotifier.setVisibility(View.VISIBLE);

                    //Set the colors of the grouping mode notifier to match the calculated colors from the group ID:
                    int[] iColors = GlobalClass.calculateGroupingControlsColors(ci.sGroupID);
                    gTextView_GroupIDClipboardLabel.setTextColor(iColors[1]);
                    gTextView_GroupIDClipboard.setTextColor(iColors[1]);
                    //Change the color of the 'close' icon for proper contrast:
                    Drawable drawable_Baseline_Close_24 = AppCompatResources.getDrawable(getApplicationContext(), R.drawable.baseline_close_24);
                    if(drawable_Baseline_Close_24 != null) {
                        Drawable drawable = drawable_Baseline_Close_24.mutate();
                        drawable.setColorFilter(new PorterDuffColorFilter(iColors[1], PorterDuff.Mode.SRC_IN));
                        gImageButton_ClearGroupingClipboard.setImageDrawable(drawable);
                    }
                    //Set the grouping mode notifier background and border colors:
                    GradientDrawable drawable = (GradientDrawable)gLinearLayout_GroupingModeNotifier.getBackground();
                    //drawable.mutate(); // only change this instance of the xml, not all components using this xml
                    drawable.setStroke(globalClass.ConvertDPtoPX(1), iColors[1]); // set stroke width and stroke color
                    drawable.setColor(iColors[0]); //Don't use .setTint for this, as it will override the stroke (border).

                    gTextView_GroupIDClipboard.setText(GlobalClass.gsGroupIDClip);

                    if(!bGroupControlsAlreadyOpen){
                        updateVisibleRecyclerItems();
                    }

                    //Toast.makeText(getApplicationContext(), "Group ID copied.", Toast.LENGTH_SHORT).show();
                });

                holder.imageButton_GroupIDPaste.setOnClickListener(v -> {
                    if (!GlobalClass.gsGroupIDClip.equals("")) {
                        ci.sGroupID = GlobalClass.gsGroupIDClip;
                        int[] iColors = GlobalClass.calculateGroupingControlsColors(ci.sGroupID);
                        ci.iGroupingControlsColor = iColors[0];
                        ci.iGroupingControlsContrastColor = iColors[1];
                        ci.iGroupingControlHighlight = iColors[2];
                        ci.iGroupingControlHighlightContrastColor = iColors[3];
                        holder.textView_GroupID.setText(GlobalClass.gsGroupIDClip);
                        setGroupControlSize(holder.imageButton_GroupIDCopy, giGroupControlImageButtonWidth);
                        setGroupControlSize(holder.imageButton_GroupIDFilter, giGroupControlImageButtonWidth);
                        setGroupControlSize(holder.imageButton_GroupIDRemove, giGroupControlImageButtonWidth);
                        GlobalClass.applyGroupingControlsColor(
                                ci,
                                holder.linearLayout_GroupingControls,
                                ibGroupingControls,
                                tvGroupingTextViews);
                        Toast.makeText(getApplicationContext(), "Group ID pasted.", Toast.LENGTH_SHORT).show();
                        globalClass.CatalogDataFile_UpdateCatalogFile(ci.iMediaCategory, "Saving...");
                    }
                });

                holder.imageButton_GroupIDRemove.setOnClickListener(v -> {

                    String sConfirmationMessage = "Are you sure you want to remove assigned group ID?";

                    AlertDialog.Builder builder = new AlertDialog.Builder(Activity_CatalogViewer.this, R.style.AlertDialogCustomStyle);
                    builder.setTitle("Remove Group Assignment");
                    builder.setMessage(sConfirmationMessage);
                    builder.setPositiveButton("Yes", (dialog, id) -> {
                        dialog.dismiss();
                        ci.sGroupID = "";
                        ci.iGroupingControlsColor = ContextCompat.getColor(getApplicationContext(), R.color.colorBlack);
                        ci.iGroupingControlsContrastColor = ContextCompat.getColor(getApplicationContext(), R.color.colorTextColor);
                        ci.iGroupingControlHighlight = 0;   //Does not matter without an assigned group ID - used to indicate that the filter is on.
                        ci.iGroupingControlHighlightContrastColor = 0;
                        GlobalClass.applyGroupingControlsColor(
                                ci,
                                holder.linearLayout_GroupingControls,
                                ibGroupingControls,
                                tvGroupingTextViews);
                        holder.textView_GroupID.setText("----");
                        setGroupControlSize(holder.imageButton_GroupIDCopy, 0);
                        setGroupControlSize(holder.imageButton_GroupIDFilter, 0);
                        setGroupControlSize(holder.imageButton_GroupIDRemove, 0);
                        Toast.makeText(getApplicationContext(), "Group ID removed.", Toast.LENGTH_SHORT).show();
                        globalClass.CatalogDataFile_UpdateCatalogFile(ci.iMediaCategory, "Saving...");

                    });
                    builder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());
                    AlertDialog adConfirmationDialog = builder.create();
                    adConfirmationDialog.show();
                });

                holder.imageButton_GroupIDFilter.setOnClickListener(v -> {
                    if(ci.bSearchByGroupID){ //Technically it is a search, but we are using the filter icon.
                        //Filter is on, turn it off.
                        holder.imageButton_GroupIDFilter.setBackgroundColor(ci.iGroupingControlsColor);
                        holder.imageButton_GroupIDFilter.setColorFilter(ci.iGroupingControlsContrastColor);
                        ci.bSearchByGroupID = false;
                        holder.imageButton_GroupIDFilter.setImageResource(R.drawable.baseline_filter_alt_24);
                        GlobalClass.gsCatalogViewerSearchByGroupID[ci.iMediaCategory] = "";
                        //Todo: quickly search for any items in the viewable area that are of the same group and change their filter icon color.
                    } else {
                        //Filter is off, turn it on.
                        holder.imageButton_GroupIDFilter.setBackgroundColor(ci.iGroupingControlHighlight);
                        holder.imageButton_GroupIDFilter.setColorFilter(ci.iGroupingControlHighlightContrastColor);
                        ci.bSearchByGroupID = true;
                        holder.imageButton_GroupIDFilter.setImageResource(R.drawable.baseline_filter_alt_off_24);
                        GlobalClass.gsCatalogViewerSearchByGroupID[ci.iMediaCategory] = ci.sGroupID;
                        //Todo: quickly search for any items in the viewable area that are of the same group and change their filter icon color.
                    }
                    populate_RecyclerViewCatalogItems(); //This will cause a set all of the shown items' ci.bSearchByGroupID members.
                });

                holder.imageButton_CloseGroupingControls.setOnClickListener(v -> {
                    holder.linearLayout_GroupingControls.setVisibility(View.INVISIBLE);
                    holder.imageButton_OpenGroupingControls.setVisibility(View.VISIBLE);
                    ci.bShowGroupingControls = false;
                    if(!ci.sGroupID.equals("")){
                        //If this item has a group ID, then if it is open it is likely that there are other items
                        //  of the same group that are showing their group controls. If the user
                        //  is hiding this item's grouping controls, then they likely want the other group
                        //  items' controls hidden as well. Hide them.
                        boolean bOtherGroupItemsFound = false;
                        for(Map.Entry<Integer, ItemClass_CatalogItem> entry: treeMap.entrySet()){
                            ItemClass_CatalogItem icci = entry.getValue();
                            if(!icci.sGroupID.equals("")){
                                if(icci.sGroupID.equals(ci.sGroupID)){
                                    icci.bShowGroupingControls = false;
                                    bOtherGroupItemsFound = true;
                                }
                            }
                        }
                        if(bOtherGroupItemsFound){
                            updateVisibleRecyclerItems();
                        }
                    }
                });

            }

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Catalog item grouping controls configured.");

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "onBindViewHolder finished. ");
            stopWatch.Stop();
            stopWatch.Reset();
        }

        // Return the size of the data set (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return treeMap.size();
        }

        public void setGroupControlSize(ImageButton imageButton, int iSize){
            ViewGroup.LayoutParams params = imageButton.getLayoutParams();
            params.width = iSize;
            imageButton.setLayoutParams(params);
        }

        public void updateItem(String sItemID){
            //Created for enabling the update of thumbnail image created by user action in Activity_VideoPlayer.
            try {
                for (int i = 0; i <= mapKeys.length; i++) {
                    //TreeMap<Integer, ItemClass_CatalogItem>
                    ItemClass_CatalogItem icci = treeMap.get(mapKeys[i]);
                    if(icci != null) {
                        if (icci.sItemID.equals(sItemID)) {
                            gRecyclerViewCatalogAdapter.notifyItemChanged(i);
                            return;
                        }
                    }
                }
            } catch (Exception e){
                String sMessage = "Trouble finding item ID " + sItemID + ".\n" +
                        "Error: " + e.getMessage();
                Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(getApplicationContext(), "Could not find item ID " + sItemID, Toast.LENGTH_SHORT).show();
        }


    }




    public void populate_RecyclerViewCatalogItems(){
        GlobalClass.gbCatalogViewerRefresh = false;
        if(gProgressBar_CatalogSortProgress != null && gTextView_CatalogSortProgressBarText != null){
            gProgressBar_CatalogSortProgress.setVisibility(View.VISIBLE);
            gTextView_CatalogSortProgressBarText.setVisibility(View.VISIBLE);
        }

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataSortAndFilterCatalogDisplay = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_CatalogViewer:populate_RecyclerViewCatalogItems()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrSortAndFilterCatalogDisplay = new OneTimeWorkRequest.Builder(Worker_CatalogViewer_SortAndFilterDisplayed.class)
                .setInputData(dataSortAndFilterCatalogDisplay)
                .addTag(Worker_CatalogViewer_SortAndFilterDisplayed.TAG_WORKER_CATALOGVIEWER_SORTANDFILTERDISPLAYED) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(otwrSortAndFilterCatalogDisplay);

    }


    //=====================================================================================
    //===== Player/Viewer Code =================================================================
    //=====================================================================================

    public final static String RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_ID = "RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_KEY";
    private void StartVideoPlayerActivity(TreeMap<Integer, ItemClass_CatalogItem> treeMap, String sVideoID) {
        //Key is the TreeMap Key for the selected video.

        //A timestamp for last viewed is handled within the video player. This is because the
        //  user can swipe left or right in the player to play an adjacent video. The adjacent video
        //  needs its last viewed timestamp to be updated as well.

        //Start the video player:
        Intent intentVideoPlayer = new Intent(this, Activity_VideoPlayer.class);
        GlobalClass.gtmCatalogViewerDisplayTreeMap = treeMap;
        intentVideoPlayer.putExtra(RECYCLERVIEW_VIDEO_TREEMAP_SELECTED_VIDEO_ID, sVideoID);
        if(toastLastToastMessage != null){
            toastLastToastMessage.cancel(); //Hide any toast message that might be shown.
        }
        Toast.makeText(getApplicationContext(), "Opening...", Toast.LENGTH_LONG).show();
        startActivity(intentVideoPlayer);
    }

    private void StartComicViewerActivity(ItemClass_CatalogItem ci){

        //Record the COMIC_DATETIME_LAST_READ_BY_USER:
        ci.dDatetime_Last_Viewed_by_User = GlobalClass.GetTimeStampDouble();

        //globalClass.CatalogDataFile_UpdateRecord(ci); //No longer update the catalog file record with the date of last read. This data to be moved to another file.

        Intent intentComicViewer;
        if(ci.sGroupID.equals("")) {
            intentComicViewer = new Intent(this, Activity_ComicDetails.class);
            intentComicViewer.putExtra(GlobalClass.EXTRA_CATALOG_ITEM_ID, ci.sItemID); //Pass item ID and load record from file. To accommodate comic detail edit.
        } else {
            intentComicViewer = new Intent(this, Activity_CatalogGroupViewer.class);
            intentComicViewer.putExtra(Worker_CatalogViewer_SortAndFilterGroup.CATALOG_FILTER_EXTRA_STRING_GROUP_ID, ci.sGroupID);
            intentComicViewer.putExtra(Activity_CatalogGroupViewer.CATALOG_FILTER_EXTRA_STRING_GROUP_NAME, ci.sTitle);
        }


        if(toastLastToastMessage != null){
            toastLastToastMessage.cancel(); //Hide any toast message that might be shown.
        }

        startActivity(intentComicViewer);
    }



    private void updateVisibleRecyclerItems() {
        RecyclerView.LayoutManager layoutManager = gRecyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            int first = linearLayoutManager.findFirstVisibleItemPosition();
            int last = linearLayoutManager.findLastVisibleItemPosition();
            for (int i = first; i <= last; i++) {
                gRecyclerViewCatalogAdapter.notifyItemChanged(i);
            }
        }
    }


}