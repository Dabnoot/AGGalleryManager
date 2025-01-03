package com.agcurations.aggallerymanager;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Activity_CatalogGroupViewer extends AppCompatActivity {

    //This is an activity to allow the user to view all comics that belong to a group.

    public static final String CATALOG_FILTER_EXTRA_STRING_GROUP_NAME = "com.agcurations.aggallerymanager.intent.extra.CATALOG_FILTER_EXTRA_STRING_GROUP_NAME";

    GlobalClass globalClass;
    RecyclerView gRecyclerView;
    int giRecyclerViewLastSelectedPosition = -1;

    Toast toastLastToastMessage;

    LinearProgressIndicator gProgressIndicator_GeneralPurpose;
    TextView gTextView_ProgressBarText;

    private CatalogGroupViewerReceiver catalogGroupViewerReceiver;

    RecyclerViewCatalogGroupAdapter gRecyclerViewCatalogGroupAdapter;

    LinearLayout gLinearLayout_GroupingModeNotifier;
    TextView gTextView_GroupIDClipboardLabel;
    TextView gTextView_GroupIDClipboard;
    ImageButton gImageButton_ClearGroupingClipboard;

    int giGroupControlImageButtonWidth;

    String gsGroupID;

    boolean gbThumbnails_Visible = false;
    Resources gResources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_catalog_group_viewer);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        Intent intent = getIntent();

        //Get the item ID:
        gsGroupID = intent.getStringExtra(Worker_CatalogViewer_SortAndFilterGroup.CATALOG_FILTER_EXTRA_STRING_GROUP_ID);

        //Set the title:
        String sMaterialToolbarText = "";
        if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
            sMaterialToolbarText = "Comic Group: ";
            sMaterialToolbarText = sMaterialToolbarText + intent.getStringExtra(CATALOG_FILTER_EXTRA_STRING_GROUP_NAME);
        } else if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS){
            sMaterialToolbarText = "Video Group Viewer";
        } else if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES){
            sMaterialToolbarText = "Image Group Viewer";
        }
        MaterialToolbar materialToolbar_TopAppBar = findViewById(R.id.materialToolbar_TopAppBar);
        materialToolbar_TopAppBar.setTitle(sMaterialToolbarText);
        materialToolbar_TopAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if((item.getItemId() == R.id.icon_ChangeView)){
                    gbThumbnails_Visible = !gbThumbnails_Visible;
                    updateVisibleRecyclerItems();
                    return true;
                }
                return false;
            }
        });

        gResources = getResources();

        if( gsGroupID == null) return;

        globalClass = (GlobalClass) getApplicationContext();

        gRecyclerView = findViewById(R.id.recyclerView_ComicChapters);

        gRecyclerView.setHasFixedSize(true);
        // use this setting to improve performance if you know that changes in content do not
        // change the layout size of the RecyclerView

        RecyclerView.LayoutManager layoutManager;
        layoutManager = new LinearLayoutManager(this);
        gRecyclerView.setLayoutManager(layoutManager);


        //Configure a response receiver to listen for updates from the workers:
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_DeleteItem.CATALOG_DELETE_ITEM_ACTION_RESPONSE);
        filter.addAction(Worker_CatalogViewer_SortAndFilterGroup.CATALOG_SORT_AND_FILTER_GROUP_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportComicFolders.IMPORT_COMIC_FOLDERS_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportFiles.IMPORT_FILES_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportComicWebFiles.IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
        filter.addAction(Worker_Import_VideoDownload.IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
        filter.addAction(Worker_LocalFileTransfer.IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
        filter.addAction(Worker_Catalog_BackupCatalogDBFiles.CATALOG_DATA_FILE_BACKUP_ACTION_RESPONSE);
        filter.addAction(Worker_Catalog_DeleteMultipleItems.DELETE_MULTIPLE_ITEMS_ACTION_RESPONSE);
        filter.addAction(GlobalClass.BROADCAST_WRITE_CATALOG_FILE);
        filter.addAction(Worker_User_Delete.USER_DELETE_ACTION_RESPONSE);
        filter.addAction(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);
        filter.addAction(Worker_DownloadPostProcessing.DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);
        filter.addAction(GlobalClass.BROADCAST_CATALOG_FILES_MAINTENANCE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        catalogGroupViewerReceiver = new CatalogGroupViewerReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(catalogGroupViewerReceiver,filter);

        gProgressIndicator_GeneralPurpose = findViewById(R.id.progressIndicator_GeneralPurpose);
        gTextView_ProgressBarText = findViewById(R.id.textView_ProgressBarText);

        populate_RecyclerViewCatalogGroupItems();

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(catalogGroupViewerReceiver);
        super.onDestroy();
    }

    public class RecyclerViewCatalogGroupAdapter extends RecyclerView.Adapter<RecyclerViewCatalogGroupAdapter.ViewHolder> {

        private final TreeMap<Integer, ItemClass_CatalogItem> treeMap;
        private final Integer[] mapKeys;

        /**
         * Provide a reference to the type of views that you are using
         * (custom ViewHolder)
         */
        public class ViewHolder extends RecyclerView.ViewHolder {

            // each data item is just a string in this case
            public final RelativeLayout relativeLayout_Row;
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
                relativeLayout_Row = v.findViewById(R.id.relativeLayout_Row);
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

            public void StartAction(ItemClass_CatalogItem icci){

                giRecyclerViewLastSelectedPosition = this.getAbsoluteAdapterPosition(); //To allow scroll back to this position if the user edits the item and RecyclerView refreshes.
                //https://stackoverflow.com/questions/34942840/lint-error-do-not-treat-position-as-fixed-only-use-immediately

                if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                    StartVideoPlayerActivity(treeMap, icci.sItemID);

                } else if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                    //Temporarily set the image catalog to use the video player activity to display images until the
                    // SeriesImageViewer activity is genericized (was previously comic page viewer):
                    StartVideoPlayerActivity(treeMap, icci.sItemID);

                } else if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                    StartComicViewerActivity(icci);
                }

            }
        }

        /**
         * Initialize the dataset of the Adapter
         *
         * @param data Treemap containing sequence and catalog item.
         * by RecyclerView
         */
        public RecyclerViewCatalogGroupAdapter(TreeMap<Integer, ItemClass_CatalogItem> data) {
            this.treeMap = data;
            mapKeys = treeMap.keySet().toArray(new Integer[getCount()]);
        }

        public int getCount() {
            return treeMap.size();
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

            // Create a new view, which defines the UI of the list item
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.recycler_catalog_row, viewGroup, false);

            return new ViewHolder(view);
        }



        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {

            StopWatch stopWatch = new StopWatch(false); //enable/disable essentially turns the usage of this item on/off.
            stopWatch.Start();
            String sWatchMessageBase = "Activity_CatalogGroupViewer:RecyclerViewCatalogAdapter:onBindViewHolder:";
            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Getting catalog item data from treemap. ");

            //Get the data for the row:
            ItemClass_CatalogItem ci;
            ci = treeMap.get(mapKeys[position]);
            final ItemClass_CatalogItem ci_final = ci;
            assert ci_final != null;

            String sItemName = "";

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Catalog item data gathered from treemap. ");

            //Load the non-obfuscated image into the RecyclerView ViewHolder:

            if(gbThumbnails_Visible) {

                ViewGroup.LayoutParams layoutParams = viewHolder.imageView_Thumbnail.getLayoutParams();
                int iPixels =  (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                200,
                                gResources.getDisplayMetrics());
                layoutParams.width = iPixels;
                layoutParams.height = MATCH_PARENT;
                viewHolder.imageView_Thumbnail.setLayoutParams(layoutParams);

                Uri uriThumbnailUri;
                boolean bThumbnailQuickLookupSuccess = true;

                String sFileName = ci.sThumbnail_File;
                if (sFileName.equals("")) {
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

                if (GlobalClass.gbUseCatalogItemThumbnailDeepSearch) {
                    //Check to see if the thumbnail source is where it is supposed to be. If it is not
                    //  there, check for other related happenings that might identify the location.
                    //  This can add a little more tha 1/100th of a second to processing the thumbnail,
                    //  and in testing resulted in a stutter of the recyclerView.
                    bThumbnailQuickLookupSuccess = GlobalClass.CheckIfFileExists(uriThumbnailUri);
                    stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Thumbnail file verification complete.");
                }

                if (!bThumbnailQuickLookupSuccess) {
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
                                            if (viewHolder.textView_CatalogItemNotification != null) {
                                                //Notify the user that post-processing is incomplete:
                                                viewHolder.textView_CatalogItemNotification.setVisibility(View.VISIBLE);
                                                String sMessage = "Item pending post-processing...";
                                                viewHolder.textView_CatalogItemNotification.setText(sMessage);
                                            }
                                        }

                                    }  //End if we had to look for a .ts file to serve as a thumbnail file.
                                } //End if unable to find video working folder DocumentFile.
                            } //End if unable to find video tag folder DocumentFile.
                        } //End if video is m3u8 style.
                    } else {
                        if (viewHolder.textView_CatalogItemNotification != null) { //Default to turn off text notification for this video item.
                            viewHolder.textView_CatalogItemNotification.setVisibility(View.INVISIBLE);
                        }
                    }
                    if (uriThumbnailUri != null) {
                        if (!GlobalClass.CheckIfFileExists(uriThumbnailUri)) {
                            uriThumbnailUri = null;
                        }
                    }
                    stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "First thumbnail lookup didn't work. Second thumbnail lookup attempt complete.");
                }


                if (uriThumbnailUri != null) {
                    if (!GlobalClass.gsRefreshCatalogViewerThumbnail.equals(ci.sItemID)) {
                        //If a command to refresh a thumbnail is "" or not equal to a specified item,
                        // let Glide load the image using whatever disk strategy it has been using.
                        Glide.with(getApplicationContext())
                                .load(uriThumbnailUri)
                                .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                                .into(viewHolder.imageView_Thumbnail);
                    } else {
                        //Ignore cache. Used when the user updates a thumbnail.
                        Glide.with(getApplicationContext())
                                .load(uriThumbnailUri)
                                .diskCacheStrategy(DiskCacheStrategy.NONE) //This will only affect this one call.
                                .skipMemoryCache(true)
                                .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                                .into(viewHolder.imageView_Thumbnail);

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
                    if (GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                        //Check to see if the comic thumbnail was merely deleted such in the case if it were renamed or a duplicate, and if so select the next file (alphabetically) to be the thumbnail.
                        Uri uriComicFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].toString(), ci.sFolderRelativePath);


                        //Load the full path to each comic page into tmComicPages (sorts files):
                        TreeMap<String, String> tmSortByFileName = new TreeMap<>();
                        if (uriComicFolder != null) {
                            ArrayList<String> sComicPages = GlobalClass.GetDirectoryFileNames(uriComicFolder);
                            if (sComicPages.size() > 0) {
                                for (String sComicPage : sComicPages) {
                                    tmSortByFileName.put(GlobalClass.JumbleFileName(sComicPage), GlobalClass.FormChildUriString(uriComicFolder.toString(), sComicPage)); //de-jumble to get proper alphabetization.
                                }
                            }
                            //Assign the existing file to be the new thumbnail file:
                            if (tmSortByFileName.size() > 0) {
                                Map.Entry<String, String> mapNewComicThumbnail = tmSortByFileName.firstEntry();
                                if (mapNewComicThumbnail != null) {
                                    ci.sFilename = GlobalClass.JumbleFileName(mapNewComicThumbnail.getKey()); //re-jumble to get actual file name.
                                    uriThumbnailUri = Uri.parse(mapNewComicThumbnail.getValue());
                                    bFoundMissingComicThumbnail = true;
                                }
                            }
                        }

                    }

                    if (bFoundMissingComicThumbnail) {
                        Glide.with(getApplicationContext())
                                .load(uriThumbnailUri)
                                .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                                .into(viewHolder.imageView_Thumbnail);
                        globalClass.CatalogDataFile_UpdateRecord(ci); //update the record with the new thumbnail file name.
                    } else {
                        Glide.with(getApplicationContext())
                                .load(R.drawable.baseline_image_white_18dp_wpagepad)
                                .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                                .into(viewHolder.imageView_Thumbnail);
                    }
                }
                stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Thumbnail image loaded. ");
            } else {

                ViewGroup.LayoutParams layoutParams = viewHolder.imageView_Thumbnail.getLayoutParams();
                layoutParams.width = 0;
                layoutParams.height = 0;
                viewHolder.imageView_Thumbnail.setLayoutParams(layoutParams);

            }

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
                    if (!ci.sComicVolume.equals("")) {
                        sThumbnailText = sThumbnailText + ", Volume " + ci.sComicVolume;
                    }
                    if (!ci.sComicChapter.equals("")) {
                        sThumbnailText = sThumbnailText + ", Chapter " + ci.sComicChapter;
                    }

                    if(!ci.sComicChapterSubtitle.equals("")){
                        sThumbnailText = sThumbnailText + " - " + ci.sComicChapterSubtitle;
                    }

                    break;
            }

            if(sThumbnailText.length() > 100){
                sThumbnailText = sThumbnailText.substring(0, 100) + "...";
            }

            viewHolder.textView_Title.setText(sThumbnailText);

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Thumbnail text set. ");


            viewHolder.relativeLayout_Row.setOnClickListener(view -> {
                viewHolder.StartAction(ci_final);
            });
            viewHolder.imageView_Thumbnail.setOnClickListener(v -> {
                viewHolder.StartAction(ci_final);
            });
            viewHolder.textView_Title.setOnClickListener(view -> {
                viewHolder.StartAction(ci_final);
            });
            viewHolder.textView_Details.setOnClickListener(view -> {
                viewHolder.StartAction(ci_final);
            });
            viewHolder.imageView_Attention.setOnClickListener(v -> {
                viewHolder.StartAction(ci_final);
            });
            viewHolder.textView_AttentionNote.setOnClickListener(view -> {
                viewHolder.StartAction(ci_final);
            });

            boolean bAttention_Visible = false;
            if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                if (!ci.sComic_Missing_Pages.equals("")) {
                    bAttention_Visible = true;
                    String sAttentionNote = "Missing pages: " + ci.sComic_Missing_Pages;
                    viewHolder.textView_AttentionNote.setText(sAttentionNote);
                }
            } else if(GlobalClass.giSelectedCatalogMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                if (ci.lDuration_Milliseconds < 0) {
                    //Duration is <0 only when the source is from an online stream (M3U8), and suggests
                    //  that there was an error in the FFMPEG concatenation activity
                    bAttention_Visible = true;
                    String sAttentionNote = "Possible incomplete stream download.";
                    viewHolder.textView_AttentionNote.setText(sAttentionNote);
                }
                if (ci.iAllVideoSegmentFilesDetected == ItemClass_CatalogItem.VIDEO_SEGMENT_FILES_KNOWN_INCOMPLETE){
                    bAttention_Visible = true;
                    String sAttentionNote = "Incomplete stream download.";
                    viewHolder.textView_AttentionNote.setText(sAttentionNote);
                }

            }
            if(bAttention_Visible){
                viewHolder.imageView_Attention.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams layoutParams = viewHolder.imageView_Attention.getLayoutParams();
                layoutParams.height = WRAP_CONTENT;
                viewHolder.imageView_Attention.setLayoutParams(layoutParams);
                viewHolder.textView_AttentionNote.setVisibility(View.VISIBLE);
            } else {
                viewHolder.imageView_Attention.setVisibility(View.INVISIBLE);
                ViewGroup.LayoutParams layoutParams = viewHolder.imageView_Attention.getLayoutParams();
                layoutParams.height = 0;
                viewHolder.imageView_Attention.setLayoutParams(layoutParams);
                viewHolder.textView_AttentionNote.setVisibility(View.INVISIBLE);

            }


            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Catalog item attention icon and text configured.");

            if(viewHolder.button_Delete != null) {

                final String sItemNameToDelete = sItemName;
                viewHolder.button_Delete.setOnClickListener( (view) -> {

                    //Present confirmation that the user wishes to delete this item.
                    String sConfirmationMessage = "Confirm item deletion: " + sItemNameToDelete;

                    AlertDialog.Builder builder = new AlertDialog.Builder(Activity_CatalogGroupViewer.this, R.style.AlertDialogCustomStyle); //getApplicationContext & getBaseContext would not work here for some reason.
                    builder.setTitle("Delete Item");
                    builder.setMessage(sConfirmationMessage);
                    //builder.setIcon(R.drawable.ic_launcher);
                    builder.setPositiveButton("Yes", (DialogInterface dialogInterface, int i) -> {
                        dialogInterface.dismiss();
                        Toast.makeText(getApplicationContext(), "Deleting item...", Toast.LENGTH_LONG).show();

                        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                        String sCatalogRecord = GlobalClass.getCatalogRecordString(ci_final);
                        Data dataCatalogDeleteItem = new Data.Builder()
                                .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_CatalogGroupViewer:btnDelete.OnClickListener.OnClick")
                                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                                .putString(GlobalClass.EXTRA_CATALOG_ITEM, sCatalogRecord)
                                .build();
                        OneTimeWorkRequest otwrCatalogDeleteItem = new OneTimeWorkRequest.Builder(Worker_Catalog_DeleteItem.class)
                                .setInputData(dataCatalogDeleteItem)
                                .addTag(Worker_Catalog_DeleteItem.TAG_WORKER_CATALOG_DELETEITEM) //To allow finding the worker later.
                                .build();
                        WorkManager.getInstance(getApplicationContext()).enqueue(otwrCatalogDeleteItem);
                    });
                    builder.setNegativeButton("No", (DialogInterface dialogInterface, int i) -> {
                        dialogInterface.dismiss();
                    });
                    AlertDialog adConfirmationDialog = builder.create();
                    adConfirmationDialog.show();

                });

            }

            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Catalog item delete button configured.");

            if(viewHolder.linearLayout_GroupingControls != null &&
                    viewHolder.imageButton_OpenGroupingControls != null &&
                    viewHolder.textView_GroupID != null &&
                    viewHolder.imageButton_GroupIDNew != null &&
                    viewHolder.imageButton_GroupIDCopy!= null &&
                    viewHolder.imageButton_GroupIDPaste!= null &&
                    viewHolder.imageButton_GroupIDRemove!= null) {
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
                        viewHolder.imageButton_GroupIDNew,
                        viewHolder.imageButton_GroupIDCopy,
                        viewHolder.imageButton_GroupIDPaste,
                        viewHolder.imageButton_GroupIDRemove,
                        viewHolder.imageButton_GroupIDFilter,
                        viewHolder.imageButton_CloseGroupingControls
                };
                TextView[] tvGroupingTextViews = new TextView[]{
                        viewHolder.textView_LabelGroupID,
                        viewHolder.textView_GroupID
                };


                if(ci.bShowGroupingControls || !GlobalClass.gsGroupIDClip.equals("")){
                    //If the user has opened the grouping controls for this item or if the user
                    //  has copied a GroupID to the internal clipboard, show the grouping controls.
                    viewHolder.linearLayout_GroupingControls.setVisibility(View.VISIBLE);
                    ViewGroup.LayoutParams layoutParams = viewHolder.linearLayout_GroupingControls.getLayoutParams();
                    layoutParams.height = WRAP_CONTENT;
                    viewHolder.linearLayout_GroupingControls.setLayoutParams(layoutParams);
                    viewHolder.imageButton_OpenGroupingControls.setVisibility(View.INVISIBLE);

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
                            viewHolder.linearLayout_GroupingControls,
                            ibGroupingControls,
                            tvGroupingTextViews);
                } else {
                    viewHolder.linearLayout_GroupingControls.setVisibility(View.INVISIBLE);
                    ViewGroup.LayoutParams layoutParams = viewHolder.linearLayout_GroupingControls.getLayoutParams();
                    layoutParams.height = 0;
                    viewHolder.linearLayout_GroupingControls.setLayoutParams(layoutParams);
                    viewHolder.imageButton_OpenGroupingControls.setVisibility(View.VISIBLE);
                }

                if(!ci.sGroupID.equals("")){
                    if(!GlobalClass.gsCatalogViewerSearchByGroupID[GlobalClass.giSelectedCatalogMediaCategory].equals("")) {
                        //If a filter is on for a given group ID, all of the shown items should be items belonging to a group
                        //  with the filter icon showing.
                        if (ci.sGroupID.equals(GlobalClass.gsCatalogViewerSearchByGroupID[GlobalClass.giSelectedCatalogMediaCategory])) {
                            //ci.bSearchByGroupID = true;
                            viewHolder.imageButton_GroupIDFilter.setBackgroundColor(ci.iGroupingControlHighlight);
                            viewHolder.imageButton_GroupIDFilter.setColorFilter(ci.iGroupingControlHighlightContrastColor);
                        }
                    } else {
                        //ci.bSearchByGroupID = false;
                    }
                }

                viewHolder.imageButton_OpenGroupingControls.setOnClickListener(new View.OnClickListener() {
                    //This is the button that the user clicks to show the grouping controls
                    @Override
                    public void onClick(View v) {
                        viewHolder.linearLayout_GroupingControls.setVisibility(View.VISIBLE);
                        viewHolder.imageButton_OpenGroupingControls.setVisibility(View.INVISIBLE);
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
                    viewHolder.textView_GroupID.setText("----");
                    setGroupControlSize(viewHolder.imageButton_GroupIDCopy, 0);
                    setGroupControlSize(viewHolder.imageButton_GroupIDFilter, 0);
                    setGroupControlSize(viewHolder.imageButton_GroupIDRemove, 0);
                } else {
                    viewHolder.textView_GroupID.setText(ci.sGroupID);
                    setGroupControlSize(viewHolder.imageButton_GroupIDCopy, giGroupControlImageButtonWidth);
                    setGroupControlSize(viewHolder.imageButton_GroupIDFilter, giGroupControlImageButtonWidth);
                    setGroupControlSize(viewHolder.imageButton_GroupIDRemove, giGroupControlImageButtonWidth);
                }

                viewHolder.imageButton_GroupIDNew.setOnClickListener(v -> {
                    ci.sGroupID = GlobalClass.getNewGroupID();
                    int[] iColors = GlobalClass.calculateGroupingControlsColors(ci.sGroupID);
                    ci.iGroupingControlsColor = iColors[0];
                    ci.iGroupingControlsContrastColor = iColors[1];
                    ci.iGroupingControlHighlight = iColors[2];
                    ci.iGroupingControlHighlightContrastColor = iColors[3];
                    viewHolder.textView_GroupID.setText(ci.sGroupID);
                    setGroupControlSize(viewHolder.imageButton_GroupIDCopy, giGroupControlImageButtonWidth);
                    setGroupControlSize(viewHolder.imageButton_GroupIDFilter, giGroupControlImageButtonWidth);
                    setGroupControlSize(viewHolder.imageButton_GroupIDRemove, giGroupControlImageButtonWidth);
                    GlobalClass.applyGroupingControlsColor(
                            ci,
                            viewHolder.linearLayout_GroupingControls,
                            ibGroupingControls,
                            tvGroupingTextViews);
                    Toast.makeText(getApplicationContext(), "New group ID generated.", Toast.LENGTH_SHORT).show();
                    globalClass.CatalogDataFile_UpdateCatalogFile(ci.iMediaCategory, "Saving...");
                });

                viewHolder.imageButton_GroupIDCopy.setOnClickListener(v -> {
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

                viewHolder.imageButton_GroupIDPaste.setOnClickListener(v -> {
                    if (!GlobalClass.gsGroupIDClip.equals("")) {
                        ci.sGroupID = GlobalClass.gsGroupIDClip;
                        int[] iColors = GlobalClass.calculateGroupingControlsColors(ci.sGroupID);
                        ci.iGroupingControlsColor = iColors[0];
                        ci.iGroupingControlsContrastColor = iColors[1];
                        ci.iGroupingControlHighlight = iColors[2];
                        ci.iGroupingControlHighlightContrastColor = iColors[3];
                        viewHolder.textView_GroupID.setText(GlobalClass.gsGroupIDClip);
                        setGroupControlSize(viewHolder.imageButton_GroupIDCopy, giGroupControlImageButtonWidth);
                        setGroupControlSize(viewHolder.imageButton_GroupIDFilter, giGroupControlImageButtonWidth);
                        setGroupControlSize(viewHolder.imageButton_GroupIDRemove, giGroupControlImageButtonWidth);
                        GlobalClass.applyGroupingControlsColor(
                                ci,
                                viewHolder.linearLayout_GroupingControls,
                                ibGroupingControls,
                                tvGroupingTextViews);
                        Toast.makeText(getApplicationContext(), "Group ID pasted.", Toast.LENGTH_SHORT).show();
                        globalClass.CatalogDataFile_UpdateCatalogFile(ci.iMediaCategory, "Saving...");
                    }
                });

                viewHolder.imageButton_GroupIDRemove.setOnClickListener(v -> {
                    String sConfirmationMessage = "Are you sure you want to remove assigned group ID?";

                    AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext(), R.style.AlertDialogCustomStyle);
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
                                viewHolder.linearLayout_GroupingControls,
                                ibGroupingControls,
                                tvGroupingTextViews);
                        viewHolder.textView_GroupID.setText("----");
                        setGroupControlSize(viewHolder.imageButton_GroupIDCopy, 0);
                        setGroupControlSize(viewHolder.imageButton_GroupIDFilter, 0);
                        setGroupControlSize(viewHolder.imageButton_GroupIDRemove, 0);
                        Toast.makeText(getApplicationContext(), "Group ID removed.", Toast.LENGTH_SHORT).show();
                        globalClass.CatalogDataFile_UpdateCatalogFile(ci.iMediaCategory, "Saving...");
                    });
                    builder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());
                    AlertDialog adConfirmationDialog = builder.create();
                    adConfirmationDialog.show();
                });

                viewHolder.imageButton_GroupIDFilter.setOnClickListener(v -> {
                    if(ci.bSearchByGroupID){ //Technically it is a search, but we are using the filter icon.
                        //Filter is on, turn it off.
                        viewHolder.imageButton_GroupIDFilter.setBackgroundColor(ci.iGroupingControlsColor);
                        viewHolder.imageButton_GroupIDFilter.setColorFilter(ci.iGroupingControlsContrastColor);
                        ci.bSearchByGroupID = false;
                        viewHolder.imageButton_GroupIDFilter.setImageResource(R.drawable.baseline_filter_alt_24);
                        GlobalClass.gsCatalogViewerSearchByGroupID[ci.iMediaCategory] = "";
                        //Todo: quickly search for any items in the viewable area that are of the same group and change their filter icon color.
                    } else {
                        //Filter is off, turn it on.
                        viewHolder.imageButton_GroupIDFilter.setBackgroundColor(ci.iGroupingControlHighlight);
                        viewHolder.imageButton_GroupIDFilter.setColorFilter(ci.iGroupingControlHighlightContrastColor);
                        ci.bSearchByGroupID = true;
                        viewHolder.imageButton_GroupIDFilter.setImageResource(R.drawable.baseline_filter_alt_off_24);
                        GlobalClass.gsCatalogViewerSearchByGroupID[ci.iMediaCategory] = ci.sGroupID;
                        //Todo: quickly search for any items in the viewable area that are of the same group and change their filter icon color.
                    }
                    populate_RecyclerViewCatalogGroupItems(); //This will cause a set all of the shown items' ci.bSearchByGroupID members.
                });

                viewHolder.imageButton_CloseGroupingControls.setOnClickListener(v -> {
                    viewHolder.linearLayout_GroupingControls.setVisibility(View.INVISIBLE);
                    viewHolder.imageButton_OpenGroupingControls.setVisibility(View.VISIBLE);
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

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return treeMap.size();
        }

        public void setGroupControlSize(ImageButton imageButton, int iSize){
            ViewGroup.LayoutParams params = imageButton.getLayoutParams();
            params.width = iSize;
            imageButton.setLayoutParams(params);
        }



        public void applyGroupingControlsColor(ItemClass_CatalogItem ci,
                                               LinearLayout linearLayout_GroupingControls,
                                               ImageButton[] imageButtons,
                                               TextView[] textViews){

            linearLayout_GroupingControls.setBackground(new ColorDrawable(ci.iGroupingControlsColor));

            //Set colors for foreground controls:
            for(ImageButton imageButton: imageButtons){
                imageButton.setColorFilter(ci.iGroupingControlsContrastColor);
            }
            for(TextView textView: textViews){
                textView.setTextColor(ci.iGroupingControlsContrastColor);
            }

        }
    }


    public class CatalogGroupViewerReceiver extends BroadcastReceiver {

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
                        populate_RecyclerViewCatalogGroupItems(); //Refresh the catalog recycler view.
                    } else {
                        Toast.makeText(getApplicationContext(),"Could not successfully delete item.", Toast.LENGTH_LONG).show();
                    }
                }

                //Check to see if this is a response to request to SortAndFilterCatalogDisplay:
                boolean bRefreshCatalogDisplay = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_REFRESH_CATALOG_DISPLAY, false);
                if(bRefreshCatalogDisplay) {
                    //Catalog sort is complete.

                    if(gProgressIndicator_GeneralPurpose != null) {
                        gProgressIndicator_GeneralPurpose.setProgress(100);
                    }
                    if(gTextView_ProgressBarText != null) {
                        String s = "100%";
                        gTextView_ProgressBarText.setText(s);
                    }

                    //Apply the new TreeMap to the RecyclerView:
                    gRecyclerViewCatalogGroupAdapter = new RecyclerViewCatalogGroupAdapter(GlobalClass.gtmCatalogViewerDisplayTreeMap);
                    gRecyclerView.setAdapter(gRecyclerViewCatalogGroupAdapter);
                    gRecyclerViewCatalogGroupAdapter.notifyDataSetChanged();
                    if(giRecyclerViewLastSelectedPosition > -1){
                        gRecyclerView.scrollToPosition(giRecyclerViewLastSelectedPosition); //Scroll RecyclerView back to the last item selected by the user, due to refresh.
                        giRecyclerViewLastSelectedPosition = -1;
                    }
                    if(toastLastToastMessage != null){
                        toastLastToastMessage.cancel();
                    }
                    if(gProgressIndicator_GeneralPurpose != null && gTextView_ProgressBarText != null){
                        gProgressIndicator_GeneralPurpose.setVisibility(View.INVISIBLE);
                        gTextView_ProgressBarText.setVisibility(View.INVISIBLE);
                    }

                    int iItemCount = gRecyclerViewCatalogGroupAdapter.getItemCount();
                    String sNoun = "item";
                    if(iItemCount != 1){
                        sNoun += "s";
                    }
                    toastLastToastMessage = Toast.makeText(getApplicationContext(), "Showing " + gRecyclerViewCatalogGroupAdapter.getItemCount() + " " + sNoun + ".", Toast.LENGTH_SHORT);
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
                    if(gProgressIndicator_GeneralPurpose != null) {
                        gProgressIndicator_GeneralPurpose.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_ProgressBarText != null) {
                        gTextView_ProgressBarText.setText(sProgressBarText);
                    }
                }

            } //End if not an error message.

        } //End onReceive.

    } //End CatalogViewerServiceResponseReceiver.


    public void populate_RecyclerViewCatalogGroupItems(){
        GlobalClass.gbCatalogViewerRefresh = false;
        if(gProgressIndicator_GeneralPurpose != null && gTextView_ProgressBarText != null){
            gProgressIndicator_GeneralPurpose.setVisibility(View.VISIBLE);
            gTextView_ProgressBarText.setVisibility(View.VISIBLE);
        }

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataSortAndFilterGroup = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_CatalogGroupViewer:populate_RecyclerViewCatalogGroupItems()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putString(Worker_CatalogViewer_SortAndFilterGroup.CATALOG_FILTER_EXTRA_STRING_GROUP_ID, gsGroupID)
                .build();
        OneTimeWorkRequest otwrSortAndFilterGroup = new OneTimeWorkRequest.Builder(Worker_CatalogViewer_SortAndFilterGroup.class)
                .setInputData(dataSortAndFilterGroup)
                .addTag(Worker_CatalogViewer_SortAndFilterGroup.TAG_WORKER_CATALOGVIEWER_SORTANDFILTERGROUP) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(otwrSortAndFilterGroup);

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
        if(!ci.sGroupID.equals("")) {
            intentComicViewer = new Intent(this, Activity_ComicDetails.class);
        } else {
            intentComicViewer = new Intent(this, Activity_CatalogGroupViewer.class);
        }
        intentComicViewer.putExtra(GlobalClass.EXTRA_CATALOG_ITEM_ID, ci.sItemID); //Pass item ID and load record from file. To accommodate comic detail edit.

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
                gRecyclerViewCatalogGroupAdapter.notifyItemChanged(i);
            }
        }
    }


}