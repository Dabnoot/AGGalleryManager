package com.agcurations.aggallerymanager;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;


public class GlobalClass extends Application {
    //GlobalClass built using this guide:
    //https://androidexample.com/Global_Variable_Or_Application_Context_Variable_-_Android_Example/index.php?view=article_discription&aid=114&aaid=136

    //Each section, Videos, Pictures, Comics, has its own folder, catalog file, tags file, and log folder.

    //Global Variables:

    public static final String EXTRA_STRING_STATUS_MESSAGE = "com.agcurations.aggallerymanager.extra.String_Status_Message";

    NotificationChannel notificationChannel;
    NotificationManager notificationManager;
    public static final String NOTIFICATION_CHANNEL_ID = "com.agcurations.aggallerymanager.NOTICIFATION_CHANNEL";
    public static final String NOTIFICATION_CHANNEL_NAME = "Download progress & completion";
    public static final String NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications from AGGalleryManager, such as download progress or completion.";
    public int iNotificationID = 0;

    public String gsPin = "";

    public static Uri gUriDataFolder;
    public static String gsDataFolderBaseName = "AGGalleryManager";

    public static final int MEDIA_CATEGORY_VIDEOS = 0;
    public static final int MEDIA_CATEGORY_IMAGES = 1;
    public static final int MEDIA_CATEGORY_COMICS = 2;

    public static Integer giSelectedCatalogMediaCategory = null;

    public static TreeMap<String, String> gtmStorageDeviceNames;

    public static final Uri[] gUriCatalogFolders = new Uri[3];
    public static Uri gUriLogsFolder;
    public static Uri gUriBackupFolder;
    public static Uri gUriJobFilesFolder;
    public static Uri gUriBrowserDataFolder;
    public static Uri gUriWebpageTabDataFile;
    public static Uri gUriWebpageFaviconBitmapFolder;
    public static Uri gUriImageDownloadHoldingFolder; //Used to hold individual images downloaded by the user from the browser prior to import.
    public static File gfDownloadExternalStorageFolder;  //Destination root for DownloadManager Downloaded files. Android limits DL destination locations.
    public static File gfImageDownloadHoldingFolderTemp; //Used to hold download manager files temporarily, to be moved so that DLM can't find them for cleanup operations.
    public static String gsImageDownloadHoldingFolderTempRPath; //For coordinating file transfer from internal storage to SD card.
    public static final Uri[] gUriCatalogContentsFiles = new Uri[3];
    public static final Uri[] gUriCatalogTagsFiles = new Uri[3];

    public static ContentResolver gcrContentResolver;

    //Tag variables:
    public final List<TreeMap<Integer, ItemClass_Tag>> gtmCatalogTagReferenceLists = new ArrayList<>();
    public final List<TreeMap<String, ItemClass_CatalogItem>> gtmCatalogLists = new ArrayList<>();
    public static final String[] gsCatalogFolderNames = {"Videos", "Images", "Comics"};

    public int giLoadingState = LOADING_STATE_NOT_STARTED;
    public static final int LOADING_STATE_NOT_STARTED = 0;
    public static final int LOADING_STATE_STARTED = 1;
    public static final int LOADING_STATE_FINISHED = 2;

    //Activity_CatalogViewer variables shared with Service_CatalogViewer:
    public TreeMap<Integer, ItemClass_CatalogItem> gtmCatalogViewerDisplayTreeMap;
    public static final int SORT_BY_DATETIME_LAST_VIEWED = 0;
    public static final int SORT_BY_DATETIME_IMPORTED = 1;
    public int[] giCatalogViewerSortBySetting = {SORT_BY_DATETIME_LAST_VIEWED, SORT_BY_DATETIME_LAST_VIEWED, SORT_BY_DATETIME_LAST_VIEWED};
    public static final String[] gsCatalogViewerPreferenceNameSortBy = {"VIDEOS_SORT_BY", "IMAGES_SORT_BY", "COMICS_SORT_BY"};
    public static final String[] gsCatalogViewerPreferenceNameSortAscending = {"VIDEOS_SORT_ASCENDING", "IMAGES_SORT_ASCENDING", "COMICS_SORT_ASCENDING"};
    public boolean[] gbCatalogViewerSortAscending = {true, true, true};
    //Search and Filter Variables:
    public static final int SEARCH_IN_NO_SELECTION = 0;
    public static final int SEARCH_IN_TITLE = 1;
    public static final int SEARCH_IN_ARTIST = 2;
    public static final int SEARCH_IN_CHARACTERS = 3;
    public static final int SEARCH_IN_PARODIES = 4;
    public static final int SEARCH_IN_ITEMID = 5;
    public int[] giCatalogViewerSearchInSelection = {SEARCH_IN_NO_SELECTION, SEARCH_IN_NO_SELECTION, SEARCH_IN_NO_SELECTION};
    public String[] gsCatalogViewerSearchInText = {"", "", ""};
    public static final int FILTER_BY_NO_SELECTION = 0;
    public static final int FILTER_BY_WEBSOURCE = 1;
    public static final int FILTER_BY_FOLDERSOURCE = 2;
    public static final int FILTER_BY_NOTAGS = 3;
    public static final int FILTER_BY_ITEMPROBLEM = 4;
    public int[] giCatalogViewerFilterBySelection = {SEARCH_IN_NO_SELECTION, SEARCH_IN_NO_SELECTION, SEARCH_IN_NO_SELECTION};
    //Variables for the Sort & Filter resolution/pagecount RangeSlider:
    public TreeMap<Integer, Integer> gtmVideoResolutions;
    public int giMinVideoResolutionSelected = -1;
    public int giMaxVideoResolutionSelected = -1;
    public int giMinImageMegaPixels = -1;
    public int giMaxImageMegaPixels; //todo: carry float here.
    public int giMinImageMegaPixelsSelected = -1;//todo: carry float here.
    public int giMaxImageMegaPixelsSelected = -1;//todo: carry float here.
    public int giMinComicPageCount = -1;
    public int giMaxComicPageCount;
    public int giMinComicPageCountSelected = -1;
    public int giMaxComicPageCountSelected = -1;
    public long glMaxVideoDurationMS = 1; //For the filter range slider.
    public long glMinVideoDurationMSSelected = -1;
    public long glMaxVideoDurationMSSelected = -1;
    public ArrayList<TreeSet<Integer>> galtsiCatalogViewerFilterTags;
    //public boolean gbGuestMode;
    public boolean gbCatalogViewerRefresh = false; //Used when data is edited.
    //public ArrayList<TreeMap<Integer, Integer>> galtmTagHistogram;
    public boolean[] gbTagHistogramRequiresUpdate = {true, true, true};
    //End catalog viewer variables.

    public static final String gsUnsortedFolderName = "etc";  //Used when imports are placed in a folder based on their assigned tags.

    ArrayList<ItemClass_File> galImportFileList; //Used to pass a large list of files to import to the import service.
    ArrayList<ItemClass_File> galPreviewFileList; //Same as above, but for preview.
    //  This is done because the list of files can exceed the intent extra transaction size limit.

    public static final int MOVE = 0;
    public static final int COPY = 1;
    public static final String[] gsMoveOrCopy = {"Move", "Copy", "Moving", "Copying"};

    public boolean gbIsDarkModeOn = false;

    ArrayList<ItemClass_WebPageTabData> gal_WebPages;

    public boolean gbWorkerVideoAnalysisInProgress = false;

    public ItemClass_User gicuCurrentUser; //If null, routines will use the default maturity rating.
    public int giDefaultUserMaturityRating = AdapterMaturityRatings.MATURITY_RATING_M; //todo: Setting - add to settings

    //=====================================================================================
    //===== Background Service Tracking Variables =========================================
    //=====================================================================================
    //These vars not in a ViewModel as a service can continue to run after an activity is destroyed.

    //Variables to control starting of import folder content analysis:
    // These variables prevent the system/user from starting another folder analysis until an
    // existing folder analysis operation is finished.
    //public boolean gbImportFolderAnalysisStarted = false; This item not needed for this fragment.
    public static boolean gbImportFolderAnalysisRunning = false;
    public static boolean gbImportHoldingFolderAnalysisAutoStart = false;
    public static boolean gbImportFolderAnalysisStop = false;
    public static boolean gbImportFolderAnalysisFinished = false;
    public StringBuilder gsbImportFolderAnalysisLog = new StringBuilder();
    public int giImportFolderAnalysisProgressBarPercent = 0;
    public String gsImportFolderAnalysisProgressBarText = "";
    public String gsImportFolderAnalysisSelectedFolder = "";
    //Variables to control starting of import execution:
    // These variables prevent the system/user from starting another import until an existing
    // import operation is finished.
    public boolean gbImportExecutionStarted = false;
    public boolean gbImportExecutionRunning = false;
    public boolean gbImportExecutionFinished = false;
    public StringBuilder gsbImportExecutionLog = new StringBuilder();
    public int giImportExecutionProgressBarPercent = 0;
    public String gsImportExecutionProgressBarText = "";
    //Variables to control starting of comic web address analysis:
    // These variables prevent the system/user from starting another analysis until an existing
    // operation is finished.
    public boolean gbImportComicWebAnalysisRunning = false;
    public boolean gbImportComicWebAnalysisFinished = false;

    //The variable below is used to identify files that were acquired using the Android DownloadManager.
    //  The Android DownloadIdleService will automatically delete the files that this program downloads
    //  after about a week. This program must go through and find these files and rename them so that
    //  the service does not delete them.
    //  See https://www.vvse.com/blog/blog/2020/01/06/android-10-automatically-deletes-downloaded-files/.
    //  See https://android.googlesource.com/platform/packages/providers/DownloadProvider/+/master/src/com/android/providers/downloads/DownloadIdleService.java#109.
    //  See https://developer.android.com/reference/android/app/DownloadManager.Request.html#setVisibleInDownloadsUi(boolean).
    public static String gsDLTempFolderName = "DL";

    public static String gsApplicationLogName = "ApplicationLog.txt";

    public static final String EXTRA_CALLER_ID = "com.agcurations.aggallermanager.string_caller_id";
    public static final String EXTRA_CALLER_TIMESTAMP = "com.agcurations.aggallermanager.long_caller_timestamp";

    public ArrayList<ItemClass_User> galicu_Users;


    //=====================================================================================
    //===== Network Monitoring ============================================================
    //=====================================================================================
    public boolean isNetworkConnected = false;
    public ConnectivityManager connectivityManager;
    // Network Check
    public void registerNetworkCallback() {
        try {
            connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
                                                                   @Override
                                                                   public void onAvailable(@NonNull Network network) {
                                                                       isNetworkConnected = true; // Global Static Variable
                                                                   }
                                                                   @Override
                                                                   public void onLost(@NonNull Network network) {
                                                                       isNetworkConnected = false; // Global Static Variable
                                                                   }
                                                               }

            );
            isNetworkConnected = false;
        }catch (Exception e){
            isNetworkConnected = false;
        }
    }


    //=====================================================================================
    //===== Utilities =====================================================================
    //=====================================================================================

    public static String gsFileSeparator = null;
    public static String gsUriAppRootPrefix = null;

    public static Point getScreenWidth(@NonNull Activity activity) {
        Point pReturn = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets()
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            pReturn.x = windowMetrics.getBounds().width();// - insets.left - insets.right;
            pReturn.y = windowMetrics.getBounds().height();// - insets.top - insets.bottom;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            pReturn.y = displayMetrics.heightPixels;
            pReturn.x = displayMetrics.widthPixels;
        }
        return pReturn;
    }

    public int ConvertDPtoPX(int dp){
        Resources r = getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                r.getDisplayMetrics()
        );
    }

    /**
     * Creates a bitmap from the supplied view.
     *
     * @param view The view to get the bitmap.
     * @param width The width for the bitmap. Pass 0 to use view dimensions.
     * @param height The height for the bitmap. Pass 0 to use view dimensions.
     *
     * @return The bitmap from the supplied drawable.
     */
    public @NonNull Bitmap createBitmapFromView(@NonNull View view, int width, int height) {
        //https://dev.to/pranavpandey/android-create-bitmap-from-a-view-3lck
        //Pass 0 for height or width to use the current view dimensions.
        if (width > 0 && height > 0) {
            view.measure(View.MeasureSpec.makeMeasureSpec(ConvertDPtoPX(width), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(ConvertDPtoPX(height), View.MeasureSpec.EXACTLY));
        }
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(),
                view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable background = view.getBackground();

        if (background != null) {
            background.draw(canvas);
        }
        view.draw(canvas);

        return bitmap;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        try {
            if(activity.getCurrentFocus() != null) {
                inputMethodManager.hideSoftInputFromWindow(
                        activity.getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e){
            //Most likely null pointer if keyboard is not shown.
        }
    }

    public long AvailableStorageSpace(Context c, Integer iDevice) {
        //Returns space available in kB.
        long freeBytesExternal = 0;
        File[] fAvailableDirs = c.getExternalFilesDirs(null);
        if (fAvailableDirs.length >= iDevice) {
            //Examine the likely SDCard:
            freeBytesExternal = new File(fAvailableDirs[iDevice].toString()).getFreeSpace();
        } else {
            Toast.makeText(c, "Storage device " + iDevice + " not found.", Toast.LENGTH_LONG).show();
        }

        return freeBytesExternal;
    }

    static final String gsDatePatternNumSort = "yyyyMMdd.HHmmss";
    static DateTimeFormatter gdtfDateFormatter;

    public static Double GetTimeStampDouble(){
        //Get an easily-comparable time stamp.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternNumSort);
        String sTimeStamp = gdtfDateFormatter.format(LocalDateTime.now());
        return Double.parseDouble(sTimeStamp);
    }

    static final String gsDatePatternReadReady = "yyyy-MM-dd HH:mm:ss";
    public static String GetTimeStampReadReady(){
        //Get an easily readable time stamp.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternReadReady);
        return gdtfDateFormatter.format(LocalDateTime.now());
    }

    static final String gsDatePatternFileSafe = "yyyyMMdd_HHmmss";
    public static String GetTimeStampFileSafe(){
        //Get an easily readable time stamp.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternFileSafe);
        return gdtfDateFormatter.format(LocalDateTime.now());
    }

    public static String JumbleStorageText(String sSourceText){
        if(sSourceText.equals("")){
            return "";
        }
        //Render the text unsearchable so that no scanning system can pick up explicit tags.
        String sFinalText;
        StringBuilder sbReverse = new StringBuilder();
        sbReverse.append(sSourceText);
        sFinalText = sbReverse.reverse().toString();

        return sFinalText;
    }

    public static String JumbleStorageText(int i) {
        return JumbleStorageText(Integer.toString(i));
    }

    public static String JumbleStorageText(double d) {
        return JumbleStorageText(Double.toString(d));
    }

    public static String JumbleStorageText(long l) {
        return JumbleStorageText(Long.toString(l));
    }

    public static String JumbleStorageText(boolean b) {
        return JumbleStorageText(Boolean.toString(b));
    }

    public static String JumbleFileName(String sFileName){
        if(sFileName.equals("")){
            return "";
        }
        //Reverse the text on the file so that the file does not get picked off by a search tool:
        StringBuilder sFileNameExtJumble = new StringBuilder();
        sFileNameExtJumble.append(sFileName.substring(sFileName.lastIndexOf(".") + 1));
        StringBuilder sFileNameBody = new StringBuilder();
        sFileNameBody.append(sFileName.substring(0,sFileName.lastIndexOf(".")));
        return sFileNameBody.reverse().toString() + "." + sFileNameExtJumble.reverse().toString();
    }

    public static String[] SplitFileNameIntoBaseAndExtension(String sFileName){
        return sFileName.split("\\.(?=[^\\.]+$)");
    }

    public static ArrayList<String> GetDirectoryFileNames(Uri uriParent){
        //This routine does not return folder names!
        ArrayList<String> alsFileNames = new ArrayList<>();

        if(!GlobalClass.CheckIfFileExists(uriParent)){
            //The program will crash if the folder does not exist.
            return alsFileNames; //Let it behave as if there are no files in the folder.
        }
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriParent,
                DocumentsContract.getDocumentId(uriParent));
        Cursor c = null;
        try {
            c = gcrContentResolver.query(childrenUri, new String[] {
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
            if(c != null) {
                while (c.moveToNext()) {
                    final String sFileName = c.getString(0);
                    final String sMimeType = c.getString( 1);
                    if(!sMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                        alsFileNames.add(sFileName);
                    }
                }
                c.close();
            }
        } catch (Exception e) {
            Log.d("GlobalClass:GetDirFileNames()", "Problem querying folder.");
        }
        return alsFileNames;
    }
    public static ArrayList<String> GetDirectoryFileNames(String sUriParent){
        //This routine does not return folder names!
        Uri uriParent = Uri.parse(sUriParent);
        return GetDirectoryFileNames(uriParent);
    }

    public static boolean IsDirEmpty(Uri uriDirectory){
        boolean EMPTY = true;
        boolean NOT_EMPTY = false;
        if(!GlobalClass.CheckIfFileExists(uriDirectory)){
            return NOT_EMPTY;
        }
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriDirectory,
                DocumentsContract.getDocumentId(uriDirectory));
        Cursor c = null;
        try {
            c = gcrContentResolver.query(childrenUri, new String[] {
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
            if(c != null) {
                if (c.moveToNext()) {
                    c.close();
                    return NOT_EMPTY;
                }

            }
        } catch (Exception e) {
            Log.d("GlobalClass:IsDirEmpty()", "Problem querying folder.");
        }
        return EMPTY;
    }




    public static String FormChildUriString(String sUriParent, String sFileName){
        //todo: if this routine is only called by FormChildUri, get rid of it.
        return sUriParent + gsFileSeparator + sFileName;
    }

    public static Uri FormChildUri(String sUriParent, String sFileName){
        String sChildUri = FormChildUriString(sUriParent, sFileName);
        return Uri.parse(sChildUri);
    }

    public static Uri FormChildUri(Uri uriParent, String sFileName){
        return  FormChildUri(uriParent.toString(), sFileName);
    }

    public static boolean CheckIfFileExists(Uri uriParent, String sFileName){

        String sUriFile = uriParent.toString()
                + gsFileSeparator + sFileName;
        Uri uriFile = Uri.parse(sUriFile);

        return CheckIfFileExists(uriFile, uriParent);
    }

    public static boolean CheckIfFileExists(Uri uriFile){

        if(uriFile != null) {
            /*String sPossibleExtension = uriFile.toString();
            if(sPossibleExtension.length() > 3){
                if(sPossibleExtension.contains(".")){
                    sPossibleExtension = sPossibleExtension.substring(sPossibleExtension.lastIndexOf("."), sPossibleExtension.length());
                    if(sPossibleExtension.length() <= 4){
                        //Consider this to be a file.
                        //todo: confirmation that this is not a folder?
                        StopWatch stopWatch = new StopWatch(true);
                        stopWatch.Start();
                        String sWatchMessageBase = "Globalclass:CheckIfFileExists: ";
                        stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Checking if file exists with Inputstream. ");
                        try {
                            InputStream inputStream = gcrContentResolver.openInputStream(uriFile);
                            if(inputStream != null){
                                inputStream.close();
                            }
                            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Inputstream successfully opened and closed.");
                            return true;
                        } catch (Exception e) {
                            stopWatch.PostDebugLogAndRestart(sWatchMessageBase + "Inputstream exception. Uri must not be a file.");
                            return false;
                        }
                    }
                }
            }*/

            String sUriChild = uriFile.toString();
            String sUriParent = sUriChild.substring(0, sUriChild.lastIndexOf(gsFileSeparator));
            Uri uriParent = Uri.parse(sUriParent);
            return CheckIfFileExists(uriFile, uriParent);
        }
        return false;

    }

    public static boolean CheckIfFileExists(Uri uriFile, Uri uriParent) {
        try {
            return DocumentsContract.isChildDocument(gcrContentResolver, uriParent, uriFile);
        } catch (Exception e) {
            return false;
        }
    }

    public static String GetFileName(String sFileUri){
        //todo: Add exception signature and handle the exception in the callers.
        String sFileName = sFileUri.substring(sFileUri.lastIndexOf(gsFileSeparator) + gsFileSeparator.length());
        try {
            sFileName = URLDecoder.decode(sFileName, StandardCharsets.UTF_8.toString());
        } catch (Exception e){
            return sFileName;
        }
        return sFileName;
    }
    public static String GetFileName(Uri uriFileUri){
        return GetFileName(uriFileUri.toString());
    }

    public Long GetFileSize(String sUri){
        Cursor c = null;
        Uri uri = Uri.parse(sUri);
        Long lFileSize = null;
        try {
            c = gcrContentResolver.query(uri, new String[] {
                    DocumentsContract.Document.COLUMN_SIZE }, null, null, null);
            if(c != null) {
                while (c.moveToNext()) {
                    lFileSize = c.getLong(0);
                }
                c.close();
            }
        } catch (Exception e) {
            Log.d("GlobalClass:GetDirFileNames()", "Problem querying file.");
        }
        return lFileSize;


    }

    public static Uri CreateDirectory(Uri uriDesiredDirectory) throws FileNotFoundException {
        /*//https://stackoverflow.com/questions/57570369/android-saf-uri-of-a-folder-on-disk-from-documentscontract-gettreedocumentid
        String id = DocumentsContract.getTreeDocumentId(uriDesiredDirectory);
        Uri uriNewDirectory = DocumentsContract.buildChildDocumentsUriUsingTree(uriDesiredDirectory, id);*/
        Uri uriParent = GetParentUri(uriDesiredDirectory);
        String sDirName = GetFileName(uriDesiredDirectory);
        Uri uriNewDirectory;

                uriNewDirectory = DocumentsContract.createDocument(gcrContentResolver, uriParent, DocumentsContract.Document.MIME_TYPE_DIR, sDirName);

        return uriNewDirectory;

    }
    public static Uri CreateDirectory(String sDesiredDirectory) throws FileNotFoundException {
        return CreateDirectory(Uri.parse(sDesiredDirectory));
    }

    public static Uri GetParentUri(String sUriChild){
        String sUriParent = sUriChild.substring(0, sUriChild.lastIndexOf(gsFileSeparator));
        return Uri.parse(sUriParent);
    }

    public static Uri GetParentUri(Uri sUriChild){
        return GetParentUri(sUriChild.toString());
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        final int bufLen = 1024;
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                outputStream.write(buf, 0, readLen);

            return outputStream.toByteArray();
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            if (exception == null) inputStream.close();
            else try {
                inputStream.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
        }
    }

    //=====================================================================================
    //===== Catalog Subroutines Section ===================================================
    //=====================================================================================
    public static final String EXTRA_BOOL_DELETE_ITEM = "com.agcurations.aggallerymanager.extra.delete_item";
    public static final String EXTRA_BOOL_DELETE_ITEM_RESULT = "com.agcurations.aggallerymanager.extra.delete_item_result";
    public static final String EXTRA_BOOL_REFRESH_CATALOG_DISPLAY = "com.agcurations.aggallerymanager.extra.refresh_catalog_display";
    public static final String EXTRA_CATALOG_ITEM = "com.agcurations.aggallerymanager.extra.catalog_item";

    public final int giCatalogFileVersion = 6;
    public String getCatalogHeader(){
        String sHeader = "";
        sHeader = sHeader + "MediaCategory";                        //Video, image, or comic.
        sHeader = sHeader + "\t" + "ItemID";                        //Video, image, comic id
        sHeader = sHeader + "\t" + "Filename";                      //Video or image filename
        sHeader = sHeader + "\t" + "Folder_Name";                   //Name of the folder holding the video, image, or comic pages
        sHeader = sHeader + "\t" + "Thumbnail_File";                //Name of the file used as the thumbnail for a video or comic
        sHeader = sHeader + "\t" + "Datetime_Import";               //Date of import. Used for sorting if desired
        sHeader = sHeader + "\t" + "Datetime_Last_Viewed_by_User";  //Date of last read by user. Used for sorting if desired
        sHeader = sHeader + "\t" + "Tags";                          //Tags given to the video, image, or comic
        sHeader = sHeader + "\t" + "Height";                        //Video or image dimension/resolution
        sHeader = sHeader + "\t" + "Width";                         //Video or image dimension/resolution
        sHeader = sHeader + "\t" + "Duration_Milliseconds";         //Duration of video in milliseconds
        sHeader = sHeader + "\t" + "Duration_Text";                 //Duration of video text in 00:00:00 format
        sHeader = sHeader + "\t" + "Resolution";                    //Resolution for sorting at user request
        sHeader = sHeader + "\t" + "Size";                          //Size of video, image, or size of all files in the comic, in Bytes
        sHeader = sHeader + "\t" + "Cast";                          //For videos and images

        //Comic-related variables:
        sHeader = sHeader + "\t" + "ComicArtists";                  //Common comic tag category
        sHeader = sHeader + "\t" + "ComicCategories";               //Common comic tag category
        sHeader = sHeader + "\t" + "ComicCharacters";               //Common comic tag category
        sHeader = sHeader + "\t" + "ComicGroups";                   //Common comic tag category
        sHeader = sHeader + "\t" + "ComicLanguages";                //Language(s) found in the comic
        sHeader = sHeader + "\t" + "ComicParodies";                 //Common comic tag category
        sHeader = sHeader + "\t" + "Title";                         //Comic name or Video Title
        sHeader = sHeader + "\t" + "ComicPages";                    //Total number of pages as defined at the comic source
        sHeader = sHeader + "\t" + "Comic_Max_Page_ID";             //Max comic page id extracted from file names
        sHeader = sHeader + "\t" + "Comic_Missing_Pages";           //Missing page numbers
        sHeader = sHeader + "\t" + "File_Count";                    //Files included with the comic. Can be used for integrity check. Also used
                                                                    // for video M3U8 download completion check.
        sHeader = sHeader + "\t" + "Comic_Online_Data_Acquired";    //Typically used to gather tag data from an online comic source, if automatic.
        sHeader = sHeader + "\t" + "Comic_Source";

        sHeader = sHeader + "\t" + "Grade";                         //Grade of the item, set by the user
        sHeader = sHeader + "\t" + "PostProcessingCode";            //Code for required post-processing.
        /*sHeader = sHeader + "\t" + "Video_Link";                    //For video download from web page or M3U8 stream. Web address of page is
                                                                    //  stored in sAddress. There can be multiple video downloads and streams
                                                                    //  per web page, hence this field.*/
        sHeader = sHeader + "\t" + "M3U8IntegrityFlag";             //For verifying m3u8 segment-file-complex integrity.
        sHeader = sHeader + "\t" + "Maturity_Rating";               //Maturity rating either defined by the user or inherited from tags.
        sHeader = sHeader + "\t" + "Approved_Users";                //Approved user(s) either defined from the importing user, explicity by the user, or inhereted from tags.
        sHeader = sHeader + "\t" + "Version:" + giCatalogFileVersion;

        return sHeader;
    }

    public static String getCatalogRecordString(ItemClass_CatalogItem ci){

        String sRecord = "";  //To be used when writing the catalog file.
        sRecord = sRecord + ci.iMediaCategory;                                            //Video, image, or comic.
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sItemID);                         //Video, image, comic id
        sRecord = sRecord + "\t" + ci.sFilename;                                          //Video or image filename
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sFolder_Name);                    //Name of the folder holding the video, image, or comic pages
        sRecord = sRecord + "\t" + ci.sThumbnail_File;                                    //Name of the file used as the thumbnail for a video or comic
        sRecord = sRecord + "\t" + JumbleStorageText(ci.dDatetime_Import);                //Date of import. Used for sorting if desired
        sRecord = sRecord + "\t" + JumbleStorageText(ci.dDatetime_Last_Viewed_by_User);   //Date of last read by user. Used for sorting if desired
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sTags);                           //Tags given to the video, image, or comic
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iHeight);                         //Video or image dimension/resolution
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iWidth);                          //Video or image dimension/resolution
        sRecord = sRecord + "\t" + JumbleStorageText(ci.lDuration_Milliseconds);          //Duration of video in milliseconds
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sDuration_Text);                  //Duration of video text in 00:00:00 format
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sResolution);                     //Resolution for sorting at user request
        sRecord = sRecord + "\t" + JumbleStorageText(ci.lSize);                           //Size of video, image, or size of all files in the comic, in Bytes
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sCast);                           //For videos and images

        //Comic-related variables:
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicArtists);                   //Common comic tag category
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicCategories);                //Common comic tag category
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicCharacters);                //Common comic tag category
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicGroups);                    //Common comic tag category
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicLanguages);                 //Language(s) found in the comic
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComicParodies);                  //Common comic tag category
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sTitle);                          //Comic name
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iComicPages);                     //Total number of pages as defined at the comic source
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iComic_Max_Page_ID);              //Max comic page id extracted from file names
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sComic_Missing_Pages);            //Missing page numbers
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iFile_Count);                     //Files included with the comic. Can be used for integrity check. Also used
                                                                                          // for video M3U8 download completion check.
        sRecord = sRecord + "\t" + JumbleStorageText(ci.bComic_Online_Data_Acquired);     //Typically used to gather tag data from an online comic source, if automatic.
        sRecord = sRecord + "\t" + JumbleStorageText(ci.sSource);                         //Website, if relevant.
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iGrade);                          //Grade.
        sRecord = sRecord + "\t" + JumbleStorageText(ci.iSpecialFlag);                    //Code for required post-processing.
        sRecord = sRecord + "\t" + ci.iAllVideoSegmentFilesDetected;                      //For verifying m3u8 segment file complex integrity.

        //Append the Maturity Rating to the record:
        sRecord = sRecord + "\t" + ci.iMaturityRating;

        //Append the list of approved users for this item to the record:
        sRecord = sRecord + "\t" + "{";
        StringBuilder sb = new StringBuilder();
        int iUserListSize = ci.alsApprovedUsers.size();
        for(int i = 0; i < iUserListSize; i++){
            sb.append(GlobalClass.JumbleStorageText(ci.alsApprovedUsers.get(i)));
            if(i < (iUserListSize - 1)){
                sb.append("%%"); //A double-percent is a symbol not allowed in a web address.
            }
        }
        sRecord = sRecord + sb + "%%" + "}";


        return sRecord;
    }
    
    public static ItemClass_CatalogItem ConvertStringToCatalogItem(String[] sRecord){
        //Designed for interpretting a line as read from a catalog file.
        ItemClass_CatalogItem ci =  new ItemClass_CatalogItem();
        ci.iMediaCategory = Integer.parseInt(sRecord[0]);                               //Video, image, or comic.
        ci.sItemID = JumbleStorageText(sRecord[1]);                                     //Video, image, comic id
        ci.sFilename = sRecord[2];                                                      //Video or image filename
        ci.sFolder_Name = JumbleStorageText(sRecord[3]);                                //Name of the folder holding the video, image, or comic pages
        ci.sThumbnail_File = sRecord[4];                                                //Name of the file used as the thumbnail for a video or comic
        ci.dDatetime_Import = Double.parseDouble(JumbleStorageText(sRecord[5]));                //Date of import. Used for sorting if desired
        ci.dDatetime_Last_Viewed_by_User = Double.parseDouble(JumbleStorageText(sRecord[6]));   //Date of last read by user. Used for sorting if desired
        ci.sTags = JumbleStorageText(sRecord[7]);                                       //Tags given to the video, image, or comic
        ci.aliTags = getTagIDsFromTagIDString(JumbleStorageText(sRecord[7]));           //Should mirror sTags.
        ci.iHeight = Integer.parseInt(JumbleStorageText(sRecord[8]));                   //Video or image dimension/resolution
        ci.iWidth = Integer.parseInt(JumbleStorageText(sRecord[9]));                    //Video or image dimension/resolution
        ci.lDuration_Milliseconds = Long.parseLong(JumbleStorageText(sRecord[10]));     //Duration of video in milliseconds
        ci.sDuration_Text = JumbleStorageText(sRecord[11]);                             //Duration of video text in 00:00:00 format
        ci.sResolution = JumbleStorageText(sRecord[12]);                                //Resolution for sorting at user request
        ci.lSize = Long.parseLong(JumbleStorageText(sRecord[13]));                      //Size of video, image, or size of all files in the comic, in Bytes
        ci.sCast = JumbleStorageText(sRecord[14]);                                      //For videos and images

        //Comic-related variables:
        ci.sComicArtists = JumbleStorageText(sRecord[15]);                              //Common comic tag category
        ci.sComicCategories = JumbleStorageText(sRecord[16]);                           //Common comic tag category
        ci.sComicCharacters = JumbleStorageText(sRecord[17]);                           //Common comic tag category
        ci.sComicGroups = JumbleStorageText(sRecord[18]);                               //Common comic tag category
        ci.sComicLanguages = JumbleStorageText(sRecord[19]);                            //Language(s = sRecord[0] found in the comic
        ci.sComicParodies = JumbleStorageText(sRecord[20]);                             //Common comic tag category
        ci.sTitle = JumbleStorageText(sRecord[21]);                                 //Comic name
        ci.iComicPages = Integer.parseInt(JumbleStorageText(sRecord[22]));              //Total number of pages as defined at the comic source
        ci.iComic_Max_Page_ID = Integer.parseInt(JumbleStorageText(sRecord[23]));       //Max comic page id extracted from file names
        ci.sComic_Missing_Pages = JumbleStorageText(sRecord[24]);                       //Missing page numbers
        ci.iFile_Count = Integer.parseInt(JumbleStorageText(sRecord[25]));              //Files included with the comic. Can be used for integrity check. Also used
                                                                                        // for video M3U8 download completion check.
        ci.bComic_Online_Data_Acquired = Boolean.parseBoolean(JumbleStorageText(sRecord[26]));  //Typically used to gather tag data from an online comic source, if automatic.
        ci.sSource = JumbleStorageText(sRecord[27]);                                    //Website, if relevant. Originally for comics also used for video.
        ci.iGrade = Integer.parseInt(JumbleStorageText(sRecord[28]));               //Grade, supplied by user.
        ci.iSpecialFlag = Integer.parseInt(JumbleStorageText(sRecord[29]));  //Code for required post-processing.

        ci.iAllVideoSegmentFilesDetected = Integer.parseInt(JumbleStorageText(sRecord[30])); //For verifying m3u8 segment file complex integrity.

        ci.iMaturityRating = Integer.parseInt(sRecord[31]);

        //Get list of approved users:
        String sApprovedUsersRaw = sRecord[32]; //Array index is 0-based.
        sApprovedUsersRaw = sApprovedUsersRaw.substring(1, sApprovedUsersRaw.length() - 1); //Remove '{' and '}'.
        String[] sApprovedUsersArray = sApprovedUsersRaw.split("%%");
        for (int i = 0; i < sApprovedUsersArray.length; i++) {
            sApprovedUsersArray[i] = GlobalClass.JumbleStorageText(sApprovedUsersArray[i]);
        }
        ci.alsApprovedUsers.addAll(Arrays.asList(sApprovedUsersArray));


        return ci;
    }

    public static ItemClass_CatalogItem ConvertStringToCatalogItem(String sRecord){
        String[] sRecord2 =  sRecord.split("\t");
        return ConvertStringToCatalogItem(sRecord2);
    }

    public void CatalogDataFile_CreateNewRecord(ItemClass_CatalogItem ci) throws Exception {
        ArrayList<ItemClass_CatalogItem> alci_CatalogItems = new ArrayList<>();
        alci_CatalogItems.add(ci);
        CatalogDataFile_CreateNewRecords(alci_CatalogItems);
    }

    public void CatalogDataFile_CreateNewRecords(ArrayList<ItemClass_CatalogItem> alci_CatalogItems) {

        int iMediaCategory;

        if(alci_CatalogItems != null){
            if(alci_CatalogItems.size() > 0){
                iMediaCategory = alci_CatalogItems.get(0).iMediaCategory; //All items should have the same media category.
            } else {
                return;
            }
        } else {
            return;
        }

        gbTagHistogramRequiresUpdate[iMediaCategory] = true;

        TreeMap<String, ItemClass_CatalogItem> tmCatalogRecords = gtmCatalogLists.get(iMediaCategory);

        try {

            StringBuilder sbNewCatalogRecords = new StringBuilder();
            for(ItemClass_CatalogItem ci: alci_CatalogItems) {

                //Add the details to the TreeMap:
                tmCatalogRecords.put(ci.sItemID, ci);

                sbNewCatalogRecords.append(getCatalogRecordString(ci)).append("\n");

            }

            //Write the data to the file:
            OutputStream osNewCatalogContentsFile;

            osNewCatalogContentsFile = gcrContentResolver.openOutputStream(gUriCatalogContentsFiles[iMediaCategory], "wa"); //Mode wa = write-append. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
            if(osNewCatalogContentsFile == null){
                String sMessage = "Problem updating CatalogContents.dat.\n" + gUriCatalogContentsFiles[iMediaCategory];

                BroadcastProgress(true, sMessage,
                        false, 0,
                        false, "",
                        Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);
                return;
            }
            //Write the activity_comic_details_header line to the file:
            osNewCatalogContentsFile.write(sbNewCatalogRecords.toString().getBytes());
            osNewCatalogContentsFile.flush();
            osNewCatalogContentsFile.close();

        } catch (Exception e) {
            String sMessage = "Problem updating CatalogContents.dat.\n" + gUriCatalogContentsFiles[iMediaCategory] + "\n\n" + e.getMessage();

            BroadcastProgress(true, sMessage,
                    false, 0,
                    false, "",
                    Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE);

        }

        //Update the tags histogram:
        updateTagHistogramsIfRequired();

    }

    public void CatalogDataFile_UpdateRecord(ItemClass_CatalogItem ci) {
        ArrayList<ItemClass_CatalogItem> alci_CatalogItems = new ArrayList<>();
        alci_CatalogItems.add(ci);
        CatalogDataFile_UpdateRecords(alci_CatalogItems);
    }


    public void CatalogDataFile_UpdateRecords(ArrayList<ItemClass_CatalogItem> alci_CatalogItems) {
        int iMediaCategory;

        if(alci_CatalogItems != null){
            if(alci_CatalogItems.size() > 0){
                iMediaCategory = alci_CatalogItems.get(0).iMediaCategory; //All items should have the same media category.
            } else {
                return;
            }
        } else {
            return;
        }

        TreeMap<String, ItemClass_CatalogItem> tmCatalogRecords = gtmCatalogLists.get(iMediaCategory);

        try {
            InputStream isCatalogReader;
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            isCatalogReader = gcrContentResolver.openInputStream(gUriCatalogContentsFiles[iMediaCategory]);
            brReader = new BufferedReader(new InputStreamReader(isCatalogReader));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");

            String sLine = brReader.readLine();
            ItemClass_CatalogItem ciFromFile;
            while (sLine != null) {
                ciFromFile = ConvertStringToCatalogItem(sLine);

                //Check to see if this record is one of the records that we want to update:
                for (ItemClass_CatalogItem ciToUpdate: alci_CatalogItems) {
                    if (ciFromFile.sItemID.equals(ciToUpdate.sItemID)) {
                        sLine = getCatalogRecordString(ciToUpdate);

                        //Now update the record in the treeMap:
                        tmCatalogRecords.put(ciToUpdate.sItemID, ciToUpdate);
                    }
                }

                //Write the current record to the buffer:
                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            //Write the data to the file:
            OutputStream osNewCatalogContentsFile;

            osNewCatalogContentsFile = gcrContentResolver.openOutputStream(gUriCatalogContentsFiles[iMediaCategory], "wt"); //Mode w = write. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
            if(osNewCatalogContentsFile == null){
                throw new Exception();
            }
            //Write the activity_comic_details_header line to the file:
            osNewCatalogContentsFile.write(sbBuffer.toString().getBytes());
            osNewCatalogContentsFile.flush();
            osNewCatalogContentsFile.close();

            //Update the tags histogram if required:
            updateTagHistogramsIfRequired();
            if(isCatalogReader != null) {
                isCatalogReader.close();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + gUriCatalogContentsFiles[iMediaCategory] + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void WriteCatalogDataFile(int iMediaCategory) {

        StringBuilder sbBuffer = new StringBuilder();
        boolean bHeaderWritten = false;
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: gtmCatalogLists.get(iMediaCategory).entrySet()){

            if(!bHeaderWritten) {
                sbBuffer.append(getCatalogHeader()); //Append the header.
                sbBuffer.append("\n");
                bHeaderWritten = true;
            }

            sbBuffer.append(getCatalogRecordString(tmEntry.getValue())); //Append the data.
            sbBuffer.append("\n");
        }

        try {
            //Write the catalog file:

            OutputStream osNewCatalogContentsFile;

            osNewCatalogContentsFile = gcrContentResolver.openOutputStream(gUriCatalogContentsFiles[iMediaCategory], "wt"); //Mode w = write. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
            if(osNewCatalogContentsFile == null){
                throw new Exception();
            }
            osNewCatalogContentsFile.write(sbBuffer.toString().getBytes());
            osNewCatalogContentsFile.flush();
            osNewCatalogContentsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public boolean deleteItemFromCatalogFile(ItemClass_CatalogItem ci, String sIntentActionFilter){
        boolean bSuccess;

        gbTagHistogramRequiresUpdate[ci.iMediaCategory] = true;

        String sMessage;

        try {
            InputStream isCatalogReader;
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            isCatalogReader = gcrContentResolver.openInputStream(gUriCatalogContentsFiles[ci.iMediaCategory]);
            brReader = new BufferedReader(new InputStreamReader(isCatalogReader));
            sbBuffer.append(brReader.readLine());
            sbBuffer.append("\n");

            String sLine = brReader.readLine();
            bSuccess = false;
            ItemClass_CatalogItem ciFromFile;
            while (sLine != null) {
                ciFromFile = GlobalClass.ConvertStringToCatalogItem(sLine);
                if (!(ciFromFile.sItemID.equals(ci.sItemID))) {
                    //If the line is not the comic we are trying to delete, transfer it over:
                    sbBuffer.append(sLine);
                    sbBuffer.append("\n");
                } else {
                    //Item record is located and we are skipping copying it into the buffer (thus deleting it).
                    bSuccess = true;
                }

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            if(!bSuccess){
                sMessage = "Could not locate item data record (ID: " +
                        GlobalClass.JumbleStorageText(ci.sItemID) +
                        ") in CatalogContents.dat.\n" +
                        gUriCatalogContentsFiles[ci.iMediaCategory];
                problemNotificationConfig(sMessage, sIntentActionFilter);

            }

            //Re-write the CatalogContentsFile without the deleted item's data record:
            OutputStream osNewCatalogContentsFile = gcrContentResolver.openOutputStream(gUriCatalogContentsFiles[ci.iMediaCategory], "wt"); //Mode w = write. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
            if(osNewCatalogContentsFile == null){
                throw new Exception();
            }
            osNewCatalogContentsFile.write(sbBuffer.toString().getBytes());
            osNewCatalogContentsFile.flush();
            osNewCatalogContentsFile.close();


            //Now update memory to no longer include the item:
            gtmCatalogLists.get(ci.iMediaCategory).remove(ci.sItemID);

            //Update the tags histogram:
            updateTagHistogramsIfRequired();

        } catch (Exception e) {
            sMessage = "Problem updating CatalogContents.dat.\n" + e.getMessage();
            problemNotificationConfig(sMessage, sIntentActionFilter);
            bSuccess = false;
        }
        return bSuccess;
    }

    public void CatalogDataFile_AddNewField(){
        //Update getCatalogRecordString before calling this routine.
        //Update ConvertStringToCatalogItem after calling this routine.
        for(int i = 0; i < 3; i++){
            StringBuilder sbBuffer = new StringBuilder();
            boolean bHeaderWritten = false;
            for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: gtmCatalogLists.get(i).entrySet()){

                if(!bHeaderWritten) {
                    sbBuffer.append(getCatalogHeader()); //Append the header.
                    sbBuffer.append("\n");
                    bHeaderWritten = true;
                }

                sbBuffer.append(getCatalogRecordString(tmEntry.getValue())); //Append the data.
                sbBuffer.append("\n");
            }

            try {
                //Write the catalog file:
                OutputStream osNewCatalogContentsFile = gcrContentResolver.openOutputStream(gUriCatalogContentsFiles[i], "wt"); //Mode w = write. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
                if(osNewCatalogContentsFile == null){
                    throw new Exception();
                }
                osNewCatalogContentsFile.write(sbBuffer.toString().getBytes());
                osNewCatalogContentsFile.flush();
                osNewCatalogContentsFile.close();

            } catch (Exception e) {
                Toast.makeText(this, "Problem updating CatalogContents.dat.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    //Catalog backup handled in Service_Main.

    public ItemClass_CatalogItem analyzeComicReportMissingPages(ItemClass_CatalogItem ci){

        String sFolderName = ci.sFolder_Name;
        //Log.d("Comics", sFolderName);
        Uri uriComicFolder = FormChildUri(gUriCatalogFolders[MEDIA_CATEGORY_COMICS].toString(), sFolderName);


        if(uriComicFolder != null){
            if(CheckIfFileExists(uriComicFolder)){
                String sMessage;

                    ArrayList<String> alsComicPages = GetDirectoryFileNames(uriComicFolder);

                    if (alsComicPages.size() == 0) {
                        sMessage = "Comic source \"" + ci.sSource + "\" folder exists, but is missing files.";
                        Log.d("Comics", sMessage);
                    }

                    //Sort the file names. The on-disk file names should be jumbled and thus do not lend well to ordering.
                    TreeMap<String, String> tmSortedFileNames = new TreeMap<>();
                    for (String sComicPage : alsComicPages) {
                        String sUnJumbledFileName = GlobalClass.JumbleFileName(sComicPage);
                        tmSortedFileNames.put(sUnJumbledFileName, sComicPage);
                    }
                    ArrayList<Integer> aliComicPageNumbers = new ArrayList<>();
                    for (Map.Entry<String, String> tmEntry : tmSortedFileNames.entrySet()) {
                        String sFileName = tmEntry.getKey();
                        String sPageID = sFileName.substring(sFileName.lastIndexOf("_") + 1, sFileName.lastIndexOf("."));
                        aliComicPageNumbers.add(Integer.parseInt(sPageID));
                    }
                    ArrayList<Integer> aliMissingPages = new ArrayList<>();
                    int iExpectedPageID = 0;
                    for (Integer iPageID : aliComicPageNumbers) {
                        iExpectedPageID++;
                        while (iPageID > iExpectedPageID) {
                            aliMissingPages.add(iExpectedPageID);
                            iExpectedPageID++;
                        }
                    }
                    int iMaxPageID = iExpectedPageID;
                    if(iExpectedPageID < ci.iComicPages){
                        iExpectedPageID++;
                        for(int i = iExpectedPageID; i <= ci.iComicPages; i++){
                            aliMissingPages.add(i);
                            iMaxPageID = i;
                        }

                    }
                    ci.iComic_Max_Page_ID = iMaxPageID;
                    ci.iFile_Count = aliComicPageNumbers.size();

                    if(aliMissingPages.size() > 0) {
                        String sMissingPages = GlobalClass.formDelimitedString(aliMissingPages, ",");
                        ci.sComic_Missing_Pages = sMissingPages;
                        sMessage = "Comic source \"" + ci.sSource + "\" missing page numbers: " + sMissingPages + ".";
                        Log.d("Comics", sMessage);
                    } else {
                        ci.sComic_Missing_Pages = "";
                    }


            } else {
                String sMessage = "Comic source \"" + ci.sSource + "\" missing comic folder.";
                Log.d("Comics", sMessage);
            }
        }


        return ci;
    }

    public String getNewCatalogRecordID(int iMediaCategory){

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int iCurrentMaxID = sharedPreferences.getInt(gsCurrentMaxItemIDStoredPreference[iMediaCategory], 0);
        if(iCurrentMaxID == 0){
            //Make sure that there has not been an error with the preferences by double-checking with the catalog:
            int iThisId;
            for (Map.Entry<String, ItemClass_CatalogItem> entry : gtmCatalogLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()) {
                iThisId = Integer.parseInt(entry.getValue().sItemID);
                if (iThisId > iCurrentMaxID) iCurrentMaxID = iThisId;
            }
        }

        int iNewCatalogRecordID = iCurrentMaxID + 1;

        sharedPreferences.edit()
                .putInt(gsCurrentMaxItemIDStoredPreference[iMediaCategory], iNewCatalogRecordID)
                .apply();

        return String.valueOf(iNewCatalogRecordID);
    }

    //=====================================================================================
    //===== Tag Subroutines Section ===================================================
    //=====================================================================================
    public static final String EXTRA_TAG_TO_BE_DELETED = "com.agcurations.aggallerymanager.extra.TAG_TO_BE_DELETED";
    public static final String EXTRA_MEDIA_CATEGORY = "com.agcurations.aggallerymanager.extra.MEDIA_CATEGORY";
    public static final String EXTRA_TAG_DELETE_COMPLETE = "com.agcurations.aggallerymanager.extra.TAG_DELETE_COMPLETE";
    public static final String EXTRA_ARRAYLIST_STRING_TAGS_TO_ADD = "com.agcurations.aggallerymanager.extra.TAGS_TO_ADD";
    public static final String EXTRA_ARRAYLIST_ITEMCLASSTAGS_ADDED_TAGS = "com.agcurations.aggallerymanager.extra.ADDED_TAGS";

    public static final int giTagFileVersion = 1;
    public static String getTagFileHeader(){
        return
                "TagID" +
                "\t" + "TagText" +
                "\t" + "TagDescription" +
                "\t" + "TagAgeRating" +
                "\t" + "ApprovedUsers" +
                "\t" + "Version:" + giTagFileVersion;
    }

    public static ItemClass_Tag ConvertFileLineToTagItem(String[] sRecord){
        //Designed for interpretting a line as read from a tags file.
        ItemClass_Tag ict;
        ict = new ItemClass_Tag(Integer.parseInt(JumbleStorageText(sRecord[0])), JumbleStorageText(sRecord[1]));
        ict.sTagDescription = JumbleStorageText(sRecord[2]);
        try {
            ict.iMaturityRating = Integer.parseInt(sRecord[3]);
        } catch (Exception e){
            ict.iMaturityRating = AdapterMaturityRatings.MATURITY_RATING_X; //Default to highest restricted age rating.
            ict.sTagText = "00TagFault_" + ict.sTagText;
        }

        //Length is 1-based
        //Get the approved user list:
        String sApprovedUserRaw = sRecord[4]; //Array index is 0-based.
        sApprovedUserRaw = sApprovedUserRaw.substring(1, sApprovedUserRaw.length() - 1); //Remove '{' and '}'.
        String[] sApprovedUserArray = sApprovedUserRaw.split("%%");
        for (int i = 0; i < sApprovedUserArray.length; i++) {
            sApprovedUserArray[i] = GlobalClass.JumbleStorageText(sApprovedUserArray[i]);
        }
        ict.alsTagApprovedUsers = new ArrayList<>(Arrays.asList(sApprovedUserArray));

        return ict;
    }

    public static ItemClass_Tag ConvertFileLineToTagItem(String sRecord){
        String[] sRecord2 =  sRecord.split("\t");
        return ConvertFileLineToTagItem(sRecord2);
    }

    public ArrayList<ItemClass_Tag> TagDataFile_CreateNewRecords(ArrayList<String> sNewTagNames, int iMediaCategory){

        //Get the tags file:

        Uri uriTagsFile = gUriCatalogTagsFiles[iMediaCategory];


        int iNextRecordId = -1;

        //Create an ArrayList to store the new tags:
        ArrayList<ItemClass_Tag> ictNewTags = new ArrayList<>();
        ItemClass_Tag ictNewTag;

        //Find the greatest tag ID:
        if(gtmCatalogTagReferenceLists.get(iMediaCategory).size() > 0) {
            int iThisId;
            for (Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                iThisId = entry.getValue().iTagID;
                if (iThisId >= iNextRecordId){
                    iNextRecordId = iThisId + 1;
                }
            }
        } else {
            iNextRecordId = 0;
        }

        try {
            //Open the tags file write-mode append:

            OutputStream osNewTagsFile = gcrContentResolver.openOutputStream(uriTagsFile, "wa"); //Write-mode append.
            if(osNewTagsFile == null){
                return null;
            }
            StringBuilder sbNewTagsStringBuilder = new StringBuilder();

            for(String sNewTagName: sNewTagNames) {
                boolean bTagAlreadyExists = false;
                if (gtmCatalogTagReferenceLists.get(iMediaCategory).size() > 0) {
                    for (Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {

                        if (entry.getValue().sTagText.equalsIgnoreCase(sNewTagName)) {
                            //If the tag already exists, abort adding this tag. //todo: unless it is a user-private tag or age-rating is set differently.
                            bTagAlreadyExists = true;
                            break;
                        }
                    }
                    if(bTagAlreadyExists){
                        continue; //Skip and process the next tag.
                    }
                }


                ictNewTag = new ItemClass_Tag(iNextRecordId, sNewTagName);
                gtmCatalogTagReferenceLists.get(iMediaCategory).put(iNextRecordId, ictNewTag);

                //Prep for return of new tag items to the caller:
                ictNewTags.add(ictNewTag);

                //Add the new record to the catalog file:
                String sLine = getTagRecordString(ictNewTag);
                sbNewTagsStringBuilder.append(sLine);
                sbNewTagsStringBuilder.append("\n");
                iNextRecordId++;
            }

            osNewTagsFile.write(sbNewTagsStringBuilder.toString().getBytes(StandardCharsets.UTF_8));
            osNewTagsFile.flush();
            osNewTagsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating Tags.dat.\n" + uriTagsFile + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return ictNewTags;

    }

    public ItemClass_Tag TagDataFile_CreateNewRecord(ItemClass_Tag ictNewTag, int iMediaCategory){

        //Get the tags file:
        Uri uriTagsFile = gUriCatalogTagsFiles[iMediaCategory];

        int iNextRecordId = -1;

        //Find the greatest tag ID:
        if(gtmCatalogTagReferenceLists.get(iMediaCategory).size() > 0) {
            int iThisId;
            for (Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                iThisId = entry.getValue().iTagID;
                if (iThisId >= iNextRecordId){
                    iNextRecordId = iThisId + 1;
                }
            }
        } else {
            iNextRecordId = 0;
        }

        try {
            //Open the tags file write-mode append:
            OutputStream osNewTagsFile = gcrContentResolver.openOutputStream(uriTagsFile, "wa"); //Open file in write-append mode.
            if(osNewTagsFile == null){
                return null;
            }
            StringBuilder sbNewTagsStringBuilder = new StringBuilder();

            boolean bTagAlreadyExists = false;
            if (gtmCatalogTagReferenceLists.get(iMediaCategory).size() > 0) {
                for (Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {

                    if (entry.getValue().sTagText.toLowerCase().equals(ictNewTag.sTagText.toLowerCase())) {
                        //If the tag already exists, abort adding this tag. //todo: unless it is a user-private tag or age-rating is set differently.
                        bTagAlreadyExists = true;
                        break;
                    }
                }
            }

            if(!bTagAlreadyExists) {
                //Add the tag to memory
                ItemClass_Tag ictNewNewTag = new ItemClass_Tag(iNextRecordId, ictNewTag.sTagText); //Tag ID in the tag item is final. Must update here with "newnew" item.
                ictNewNewTag.sTagDescription = ictNewTag.sTagDescription;
                ictNewNewTag.iMaturityRating = ictNewTag.iMaturityRating;

                gtmCatalogTagReferenceLists.get(iMediaCategory).put(iNextRecordId, ictNewNewTag);

                //Add the new record to the catalog file:
                String sLine = getTagRecordString(ictNewNewTag);
                sbNewTagsStringBuilder.append(sLine);
                sbNewTagsStringBuilder.append("\n");
            } else {
                ictNewTag = null;
            }

            osNewTagsFile.write(sbNewTagsStringBuilder.toString().getBytes(StandardCharsets.UTF_8));
            osNewTagsFile.flush();
            osNewTagsFile.close();

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating Tags.dat.\n" + uriTagsFile + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return ictNewTag;

    }

    public boolean TagDataFile_UpdateRecord(ItemClass_Tag ict_TagToUpdate, int iMediaCategory) {

        Uri uriTagsFile = gUriCatalogTagsFiles[iMediaCategory];

        try {
            InputStream isTagsFile = gcrContentResolver.openInputStream(uriTagsFile);
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new InputStreamReader(isTagsFile));
            sbBuffer.append(getTagFileHeader());
            sbBuffer.append("\n");
            brReader.readLine(); //Read past the header.
            String sLine = brReader.readLine();
            while (sLine != null) {

                ItemClass_Tag ictFromFile = ConvertFileLineToTagItem(sLine);

                //Check to see if this record is the one that we want to update:
                if (ictFromFile.iTagID.equals(ict_TagToUpdate.iTagID)) {
                    sLine = getTagRecordString(ict_TagToUpdate);
                }
                //Write the current record to the buffer:
                sbBuffer.append(sLine);
                sbBuffer.append("\n");

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            //Write the data to the file:
            OutputStream osNewTagsFile = gcrContentResolver.openOutputStream(uriTagsFile, "wt"); //Open the file in overwrite mode.
            if (osNewTagsFile == null) {
                Toast.makeText(this, "Problem updating Tags.dat.\n" + uriTagsFile, Toast.LENGTH_LONG).show();
                return false;
            }
            osNewTagsFile.write(sbBuffer.toString().getBytes(StandardCharsets.UTF_8));
            osNewTagsFile.flush();
            osNewTagsFile.close();

            //Update memory:
            gtmCatalogTagReferenceLists.get(iMediaCategory).replace(ict_TagToUpdate.iTagID, ict_TagToUpdate);

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating Tags.dat.\n" + uriTagsFile + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static String getTagRecordString(ItemClass_Tag ict){
        //For writing tags to a tags file.

        String sTagRecord = "";

        sTagRecord = sTagRecord +
                JumbleStorageText(ict.iTagID.toString()) + "\t" +
                        JumbleStorageText(ict.sTagText) + "\t" +
                        JumbleStorageText(ict.sTagDescription) + "\t" +
                        ict.iMaturityRating;

        //If users are assigned to the tag, build the users' string:
        //Append the back-stack to the record:
        sTagRecord = sTagRecord + "\t" + "{";
        if(ict.alsTagApprovedUsers != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ict.alsTagApprovedUsers.size(); i++) {
                sb.append(GlobalClass.JumbleStorageText(ict.alsTagApprovedUsers.get(i)));
                if (i < (ict.alsTagApprovedUsers.size() - 1)) {
                    sb.append("%%"); //A double-percent is a symbol not allowed in a web address. Using this as an in-string delimiter.
                }
            }
            sTagRecord = sTagRecord + sb;
        }
        sTagRecord = sTagRecord + "%%" + "}";

        return sTagRecord;
    }

    public String getTagTextsFromTagIDsString(String sTagIDs, int iMediaCategory){
        String sTagTexts = "";
        if(!sTagIDs.equals("")) {
            String[] sTagIDArray = sTagIDs.split(",");
            StringBuilder sbTags = new StringBuilder();
            for (String sTagID : sTagIDArray) {
                sbTags.append(getTagTextFromID(Integer.parseInt(sTagID), iMediaCategory));
                sbTags.append(", ");
            }
            sTagTexts = sbTags.toString();
            if (sTagTexts.contains(",")) {
                sTagTexts = sTagTexts.substring(0, sTagTexts.lastIndexOf(", "));
            }
        }
        return sTagTexts;
    }

    public String getTagTextFromID(Integer iTagID, int iMediaCategory){
        String sTagText = "[Tag ID " + iTagID + " not found]";

        //todo: instead of looping through items, use TreeMap.getValue or TreeMap.getKey if it exists.
        for(Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            Integer iRefTag = entry.getValue().iTagID;
            if(iRefTag.equals(iTagID)){
                sTagText = entry.getValue().sTagText;
                break;
            }
        }


        return sTagText;
    }

    public static ArrayList<Integer> getTagIDsFromTagIDString(String sTagIDs){
        ArrayList<Integer> aliTagIDs = new ArrayList<>();
        String[] sTemp = sTagIDs.split(",");

        if(sTemp.length == 1 && sTemp[0].equals("")){
            return aliTagIDs;
        }

        for(String sTag: sTemp){
            int iTagID = Integer.parseInt(sTag);
            aliTagIDs.add(iTagID);
        }

        return  aliTagIDs;
    }

    public boolean TagIDExists(Integer iTagID, int iMediaCategory){

        for(Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            Integer iRefTag = entry.getValue().iTagID;
            if(iRefTag.equals(iTagID)){
                return true;
            }
        }

        return false;
    }

    public void TagDataFileAddNewField(){
        //Execute these steps to add a new field to the tags files.
        //  .01. Create a backup of the tags files.
        //  .02. Verify that the backup was successful by confirming the files exist and are not empty.
        //  .03. Update ItemClass_Tag.java to include the new data item, perhaps with an initial value.
        //  .04. Update "getTagFileHeader" to include the name of the new field.
        //  .05. Update "getTagRecordString" to include the new field data in the write operation.
        //  .06. Run this routine from Worker_Catalog_LoadData.java.
        //  .07. Verify the new field exists in the tags data files.
        //  .08. Comment-out code that ran this routine from Worker_Catalog_LoadData.java.
        //  .09. Update "ConvertFileLineToTagItem(String[] sRecord)" to interpret the new field in the data record and write to ItemClass_Tag.
        //  .10. Consider removing any initial value that was applied to the field in ItemClass_Tag.
        //  .11. Rebuild and install the app on the test device.
        //  .12. Verify all is well and commit code.

        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++) {

            Uri uriTagsFile = gUriCatalogTagsFiles[iMediaCategory];

            try {
                InputStream isTagsFile = gcrContentResolver.openInputStream(uriTagsFile);
                if(isTagsFile == null){
                    Toast.makeText(this, "Problem reading Tags.dat.\n" + uriTagsFile, Toast.LENGTH_LONG).show();
                    return;
                }
                BufferedReader brReader;
                brReader = new BufferedReader(new InputStreamReader(isTagsFile));

                StringBuilder sbBuffer = new StringBuilder();
                sbBuffer.append(getTagFileHeader());
                sbBuffer.append("\n");

                //Read a data record from the tags file:
                String sLine = brReader.readLine();
                if(sLine != null) {
                    sLine = brReader.readLine(); //Skip the first line - it's the header.
                }
                while (sLine != null) {

                    //Convert the tag data record to a tag class item:
                    ItemClass_Tag ictFromFile = ConvertFileLineToTagItem(sLine);

                    //Convert the tag class item back to a data record with the new field:
                    sLine = getTagRecordString(ictFromFile);

                    //Write the current record to the buffer:
                    sbBuffer.append(sLine);
                    sbBuffer.append("\n");

                    // read next data record from the tags file:
                    sLine = brReader.readLine();
                }
                brReader.close();

                isTagsFile.close();

                //Write the data to the file:
                OutputStream osNewTagsFile = gcrContentResolver.openOutputStream(uriTagsFile, "wt"); //Open the tags file in overwrite mode.
                if (osNewTagsFile == null) {
                    Toast.makeText(this, "Problem writing Tags.dat.\n" + uriTagsFile, Toast.LENGTH_LONG).show();
                    return;
                }
                osNewTagsFile.write(sbBuffer.toString().getBytes(StandardCharsets.UTF_8));
                osNewTagsFile.flush();
                osNewTagsFile.close();

            } catch (Exception e) {
                String sMessage =  "Problem updating Tags.dat.\n" + uriTagsFile + "\n" + e.getMessage();
                Log.d("TagDataFileAddNewField", sMessage);
            }
        }

    }

    public ArrayList<String> getTagTextsFromIDs(ArrayList<Integer> ali, int iMediaCategory){
        ArrayList<String> als = new ArrayList<>();
        for(Integer i : ali){
            als.add(getTagTextFromID(i, iMediaCategory));
        }
        return als;
    }

    public Integer getTagIDFromText(String sTagText, Integer iMediaCategory){
        int iKey = -1;
        for(Map.Entry<Integer, ItemClass_Tag> entry: gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            if(entry.getValue().sTagText.equalsIgnoreCase(sTagText)){
                iKey = entry.getValue().iTagID;
                break;
            }
        }
        return iKey;
    }

    public void updateTagHistogramsIfRequired(){

        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++) {
            if(gbTagHistogramRequiresUpdate[iMediaCategory]) {

                //Reset all of the tag counts back to zero for this media category:
                for(Map.Entry<Integer, ItemClass_Tag> entry: gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
                    entry.getValue().iHistogramCount = 0;
                }

                //Go through all items in the catalog and update tag counts:
                for(Map.Entry<String, ItemClass_CatalogItem> entry: gtmCatalogLists.get(iMediaCategory).entrySet()) {
                    ItemClass_CatalogItem ci = entry.getValue();
                    //Update the tags histogram. As of 7/29/2022, this is used to show the user
                    //  how many tags are in use while they select tags to perform a tag filter.
                    if(ci.aliTags == null){
                        ci.aliTags = new ArrayList<>(); //Just in case.
                    }

                    for (int iCatalogItemTagID : ci.aliTags) {
                        if(gtmCatalogTagReferenceLists.get(iMediaCategory).get(iCatalogItemTagID) != null) {
                            Objects.requireNonNull(gtmCatalogTagReferenceLists.get(iMediaCategory).get(iCatalogItemTagID)).iHistogramCount++;
                        }
                    }
                }


            }
        }
    }

    public TreeMap<Integer, ItemClass_Tag> getXrefTagHistogram(int iMediaCategory, ArrayList<Integer> aliTagIDs){
        //Get a histogram counting the tags that occur alongside tags found in aliTagIDs.
        //  Suppose the user selects tag ID 7, and wants to know what other tag IDs are frequently
        //  found alongside tag ID 7. This routine returns that list with frequency.
        //  This is used in filtering. The user will select a tag, an xref list will be returned.
        //  Tags with an occurrence of Zero will be hidden. Therefore, the user will be able to
        //  filter further by selecting additional tags.

        TreeMap<Integer, ItemClass_Tag> tmXrefTagHistogram = new TreeMap<>();

        ArrayList<Integer> aliRestrictedTagIDs = new ArrayList<>();
        for (Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
            //if (entry.getValue().bIsRestricted) {
            if(gicuCurrentUser != null) {
                if (gicuCurrentUser.iMaturityLevel < entry.getValue().iMaturityRating) {
                    aliRestrictedTagIDs.add(entry.getValue().iTagID);
                }
            } else {
                //If no user is selected or current user is somehow null, follow guidelines for
                //  default user maturity rating.
                if (entry.getValue().iMaturityRating <= giDefaultUserMaturityRating) {
                    aliRestrictedTagIDs.add(entry.getValue().iTagID);
                }
            }
        }

        //Go through each catalog item:
        for(Map.Entry<String, ItemClass_CatalogItem> entry: gtmCatalogLists.get(iMediaCategory).entrySet()) {
            ItemClass_CatalogItem ci = entry.getValue();

            //Check to see if all of the tags to be checked are in this catalog item:
            if(ci.aliTags.containsAll(aliTagIDs)){
                //Collect all of the tags that are associated with this catalog item and count them in the histogram to be returned.
                //  This includes counting the ones that are in aliTagIDs.
                //  But skip if this item contains a restricted tag and user is not approved to view restricted tags.
                ArrayList<Integer> aliRestrictedTest = new ArrayList<>(aliRestrictedTagIDs);
                aliRestrictedTest.retainAll(ci.aliTags);
                boolean bContainsRestrictedTag = aliRestrictedTest.size() > 0;
                if(bContainsRestrictedTag) {
                    //Don't add the tag if this catalog item contains a tag restricted from this user.
                    continue;
                }
                for (int iCatalogItemTagID : ci.aliTags) {
                    if(iCatalogItemTagID != -1) {
                        if (!tmXrefTagHistogram.containsKey(iCatalogItemTagID)) {
                            ItemClass_Tag ict = gtmCatalogTagReferenceLists.get(iMediaCategory).get(iCatalogItemTagID);
                            if(ict != null){
                                ict.iHistogramCount = 1;
                                tmXrefTagHistogram.put(iCatalogItemTagID, ict);
                            } else {
                                Log.d("getXrefTagHistogram","ICT is null.");
                            }
                        } else {
                            Objects.requireNonNull(tmXrefTagHistogram.get(iCatalogItemTagID)).iHistogramCount++;
                        }
                    }
                }
            }
        }

        return tmXrefTagHistogram;
    }

    public TreeMap<Integer, Integer> getInitTagHistogram(int iMediaCategory, boolean bCatalogTagsRestrictionsOn){
        TreeMap<Integer, Integer> tmCompoundTagHistogram = new TreeMap<>();

        ArrayList<Integer> aliRestrictedTagIDs = new ArrayList<>();
        for (Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
            if (entry.getValue().bIsRestricted) {
                aliRestrictedTagIDs.add(entry.getValue().iTagID);
            }
        }

        //Go through each catalog item:
        for(Map.Entry<String, ItemClass_CatalogItem> entry: gtmCatalogLists.get(iMediaCategory).entrySet()) {
            ItemClass_CatalogItem ci = entry.getValue();
            //Collect all of the tags that are associated with this catalog item and count them in the histogram to be returned.
            //  But skip if this item contains a restricted tag and user is not approved to view restricted tags.
            ArrayList<Integer> aliRestrictedTest = new ArrayList<>(aliRestrictedTagIDs);
            aliRestrictedTest.retainAll(ci.aliTags);
            boolean bContainsRestrictedTag = aliRestrictedTest.size() > 0;
            if(bCatalogTagsRestrictionsOn && bContainsRestrictedTag) {
                //Don't add the tag if TagRestrictions are on and this catalog item contains a restricted tag.
                continue;
            }
            for (int iCatalogItemTagID : ci.aliTags) {
                if (!tmCompoundTagHistogram.containsKey(iCatalogItemTagID)) {
                    tmCompoundTagHistogram.put(iCatalogItemTagID, 1);
                } else {
                    Integer iTagCountofID = tmCompoundTagHistogram.get(iCatalogItemTagID);
                    if (iTagCountofID != null) {
                        iTagCountofID++;
                        tmCompoundTagHistogram.put(iCatalogItemTagID, iTagCountofID);
                    }
                }
            }

        }

        return tmCompoundTagHistogram;
    }

    public int getLowestTagMaturityRating(ArrayList<ItemClass_Tag> alict_Tags){
        int iLowestTagMaturityRating = giDefaultUserMaturityRating;

        for(ItemClass_Tag ict: alict_Tags){
            if(iLowestTagMaturityRating < ict.iMaturityRating){
                iLowestTagMaturityRating = ict.iMaturityRating;
            }
        }

        return iLowestTagMaturityRating;
    }

    public int getLowestTagMaturityRating(ArrayList<Integer> ali_TagIDs, int iMediaCategory){

        ArrayList<ItemClass_Tag> alict_Tags = new ArrayList<>();
        for(Integer iTagID: ali_TagIDs){

            ItemClass_Tag ict = gtmCatalogTagReferenceLists.get(iMediaCategory).get(iTagID);
            if (ict != null) {
                alict_Tags.add(ict);
            }

        }

        return getLowestTagMaturityRating(alict_Tags);
    }

    public void UpdateAllCatalogItemMaturitiesBasedOnTags(){

        for(int i = 0; i < 3; i++){
            for(Map.Entry<String, ItemClass_CatalogItem> icciCatalogItem: gtmCatalogLists.get(i).entrySet()){
                int iLowestMaturityRating = giDefaultUserMaturityRating;
                for(int iTagID: icciCatalogItem.getValue().aliTags){
                    ItemClass_Tag ictReferenceTag = gtmCatalogTagReferenceLists.get(i).get(iTagID);
                    if(ictReferenceTag != null){
                        if(iLowestMaturityRating < ictReferenceTag.iMaturityRating){
                            iLowestMaturityRating = ictReferenceTag.iMaturityRating;
                        }
                    }
                }
                icciCatalogItem.getValue().iMaturityRating = iLowestMaturityRating;
            }
        }

        CatalogDataFile_AddNewField();

    }



    //==================================================================================================
    //=========  USER ACCOUNT DATA STORAGE  ============================================================
    //==================================================================================================


    public static String getUserAccountRecordString(ItemClass_User icu){
        String sUserRecord =
                        JumbleStorageText(icu.sUserName) + "\t" +
                        JumbleStorageText(icu.sPin) + "\t" +
                        JumbleStorageText(icu.iUserIconColor) + "\t" +
                        JumbleStorageText(icu.bAdmin) + "\t" +
                        JumbleStorageText(icu.iMaturityLevel);
        return sUserRecord;
    }

    public static ItemClass_User ConvertRecordStringToUserItem(String sRecord){
        ItemClass_User icu;
        icu = new ItemClass_User();
        String[] sRecordSplit =  sRecord.split("\t");
        icu.sUserName = JumbleStorageText(sRecordSplit[0]);
        icu.sPin = JumbleStorageText(sRecordSplit[1]);
        icu.iUserIconColor = Integer.parseInt(JumbleStorageText(sRecordSplit[2]));
        icu.bAdmin = Boolean.parseBoolean(JumbleStorageText(sRecordSplit[3]));
        icu.iMaturityLevel = Integer.parseInt(JumbleStorageText(sRecordSplit[4]));
        return icu;
    }


    //==================================================================================================
    //=========  BROWSER  ==============================================================================
    //==================================================================================================

    public static final String EXTRA_WEBPAGE_TAB_DATA_TABID = "com.agcurations.webbrowser.extra.WEBPAGE_TAB_DATA_TABID";
    public static final String EXTRA_WEBPAGE_TAB_DATA_ADDRESS = "com.agcurations.webbrowser.extra.WEBPAGE_TAB_DATA_ADDRESS";
    public static final String EXTRA_WEBPAGE_TAB_DATA_TITLE = "com.agcurations.webbrowser.extra.WEBPAGE_TAB_DATA_TITLE";
    public static final String EXTRA_WEBPAGE_TAB_DATA_FAVICON_ADDRESS = "com.agcurations.webbrowser.extra.WEBPAGE_TAB_DATA_FAVICON_ADDRESS";
    public static final String EXTRA_RESULT_TYPE = "com.agcurations.webbrowser.extra.RESULT_TYPE";

    public static final Queue<String> queueWebPageTabDataFileWriteRequests = new LinkedList<>();
    public static final int giMaxDelayForWriteRequestMS = 5000;

    public static final int giWebPageTabDataFileVersion = 1;
    public static String getWebPageTabDataFileHeader(){
        String sHeader = "";
        sHeader = sHeader + "ID";                       //Tab ID (unique).
        sHeader = sHeader + "\t" + "Title";             //Tab title (don't reload the page to get the title).
        sHeader = sHeader + "\t" + "Address";           //Current address for the tab.
        sHeader = sHeader + "\t" + "Favicon Filename";  //Filename of bitmap for tab icon.
        sHeader = sHeader + "\t" + "BackStack";         //Address back stack (history). Top item is the current address.
        sHeader = sHeader + "\t" + "ForwardStack";      //Address forward stack (forward-history)
        sHeader = sHeader + "\t" + "Version:" + giWebPageTabDataFileVersion;

        return sHeader;
    }

    public static String ConvertWebPageTabDataToString(ItemClass_WebPageTabData icwptd){

        String sRecord = "";  //To be used when writing the catalog file.
        sRecord = sRecord + GlobalClass.JumbleStorageText(icwptd.sTabID);
        sRecord = sRecord + "\t" + GlobalClass.JumbleStorageText(icwptd.sTabTitle);
        sRecord = sRecord + "\t" + GlobalClass.JumbleStorageText(icwptd.sAddress);
        sRecord = sRecord + "\t" + GlobalClass.JumbleStorageText(icwptd.sFaviconAddress);

        //Append the back-stack to the record:
        sRecord = sRecord + "\t" + "{";
        StringBuilder sb = new StringBuilder();
        int iBackStackSize = icwptd.stackBackHistory.size();
        for(int i = 0; i < iBackStackSize; i++){
            sb.append(GlobalClass.JumbleStorageText(icwptd.stackBackHistory.get(i)));
            if(i < (iBackStackSize - 1)){
                sb.append("%%"); //A double-percent is a symbol not allowed in a web address.
            }
        }
        sRecord = sRecord + sb + "%%" + "}";

        //Append the forward-stack to the record:
        sRecord = sRecord + "\t" + "{";
        sb = new StringBuilder();
        int iForwardStackSize = icwptd.stackForwardHistory.size();
        for(int i = 0; i < iForwardStackSize; i++){
            sb.append(GlobalClass.JumbleStorageText(icwptd.stackForwardHistory.get(i)));
            if(i < (iForwardStackSize - 1)){
                sb.append("%%"); //A double-percent is a symbol not allowed in a web address.
            }
        }
        sRecord = sRecord + sb + "%%" + "}";

        return sRecord;
    }

    public static ItemClass_WebPageTabData ConvertStringToWebPageTabData(String[] sRecord){
        //Designed for interpreting a line as read from the WebPageTabData file.
        ItemClass_WebPageTabData icwptd =  new ItemClass_WebPageTabData();
        icwptd.sTabID = GlobalClass.JumbleStorageText(sRecord[0]);
        icwptd.sTabTitle = GlobalClass.JumbleStorageText(sRecord[1]);
        icwptd.sAddress = GlobalClass.JumbleStorageText(sRecord[2]);


        if(sRecord.length > 3) { //Length is 1-based
            //Favicon filename might be empty, and if it is the last item on the record,
            //  it will not be split-out via the split operation.
            icwptd.sFaviconAddress = GlobalClass.JumbleStorageText(sRecord[3]); //Array index is 0-based.
        }

        if(sRecord.length > 4) { //Length is 1-based
            //Get the back-stack:
            String sBackStackRaw = sRecord[4]; //Array index is 0-based.
            sBackStackRaw = sBackStackRaw.substring(1, sBackStackRaw.length() - 1); //Remove '{' and '}'.
            String[] sBackStackArray = sBackStackRaw.split("%%");
            for (int i = 0; i < sBackStackArray.length; i++) {
                sBackStackArray[i] = GlobalClass.JumbleStorageText(sBackStackArray[i]);
            }
            icwptd.stackBackHistory.addAll(Arrays.asList(sBackStackArray));
        }

        if(sRecord.length > 5) { //Length is 1-based
            //Get the forward-stack:
            String sForwardStackRaw = sRecord[5]; //Array index is 0-based.
            sForwardStackRaw = sForwardStackRaw.substring(1, sForwardStackRaw.length() - 1); //Remove '{' and '}'.
            String[] sForwardStackArray = sForwardStackRaw.split("%%");
            for (int i = 0; i < sForwardStackArray.length; i++) {
                sForwardStackArray[i] = GlobalClass.JumbleStorageText(sForwardStackArray[i]);
            }
            icwptd.stackForwardHistory.addAll(Arrays.asList(sForwardStackArray));
        }



        return icwptd;
    }

    public static ItemClass_WebPageTabData ConvertStringToWebPageTabData(String sRecord){
        String[] sRecord2 =  sRecord.split("\t");
        //Split will ignore empty data and not return a full-sized array.
        //  Correcting array...
        int iRequiredFieldCount = 6;
        String[] sRecord3 = new String[iRequiredFieldCount];
        for(int i = 0; i < iRequiredFieldCount; i++){
            if(i < sRecord2.length){
                sRecord3[i] = sRecord2[i];
            } else {
                sRecord3[i] = "";
            }

        }
        return ConvertStringToWebPageTabData(sRecord3);
    }

    //====================================================================================
    //===== Worker/Download Management ==========================================================
    //====================================================================================



    //=====================================================================================
    //===== Other Subroutines Section ===================================================
    //=====================================================================================

    public static String getUniqueFileName(Uri uriParent, String sOriginalFileName, boolean bReturnJumbledFileName){
        //This routine is used to check to see if a file name is in-use, and if so, return an alternate.
        ArrayList<String> alsFileNamesInUse = GetDirectoryFileNames(uriParent);
        return getUniqueFileName(alsFileNamesInUse, sOriginalFileName, bReturnJumbledFileName);
    }
    public static String getUniqueFileName(File fileParent, String sOriginalFileName, boolean bReturnJumbledFileName) {
        //This routine is used to check to see if a file name is in-use, and if so, return an alternate.
        File[] fileFolderChildren = fileParent.listFiles();
        ArrayList<String> alsFileNamesInUse = new ArrayList<>();
        if (fileFolderChildren != null){
            for (File f : fileFolderChildren) {
                alsFileNamesInUse.add(f.getName());
            }
        }
        return getUniqueFileName(alsFileNamesInUse, sOriginalFileName, bReturnJumbledFileName);
    }

    public static String getUniqueFileName(ArrayList<String> alsFileNamesInUse, String sOriginalFileName, boolean bReturnJumbledFileName){
        //This routine is used to check to see if a file name is in-use, and if so, return an alternate.

        String sFinalFileName = sOriginalFileName;
        if(bReturnJumbledFileName) {
            sFinalFileName = JumbleFileName(sOriginalFileName);
        }

        int iOutputFolderRetryIterator = 0;
        while(alsFileNamesInUse.contains(sFinalFileName)){
            //If the file name is already in use in the output folder, the file name to be used
            //  must be changed by adding an iterator to the end.

            iOutputFolderRetryIterator++;
            String sFileNameWithoutExtension = sOriginalFileName.replaceFirst("[.][^.]+$","");
            String sNewFileName = sFileNameWithoutExtension + "_" + iOutputFolderRetryIterator;
            String sFileNameExtension = sOriginalFileName.substring(sOriginalFileName.lastIndexOf(".") + 1);
            sNewFileName = sNewFileName + "." + sFileNameExtension;

            if(bReturnJumbledFileName) {
                sNewFileName = JumbleFileName(sNewFileName);
            }

            sFinalFileName = sNewFileName;
        }

        return sFinalFileName;
    }




    public static String formDelimitedString(ArrayList<Integer> ali, String sDelimiter){
        //Used by preferences for storing integer string representing restricted tags.
        //Used to save tag IDs to catalog file.
        String sReturn;
        StringBuilder sb = new StringBuilder();
        for(Integer i : ali){
            sb.append(i.toString());
            sb.append(sDelimiter);
        }
        sReturn = sb.toString();
        if(sReturn.contains(sDelimiter)) {
            //Clear the trailing delimiter:
            sReturn = sReturn.substring(0, sReturn.lastIndexOf(sDelimiter));
        }
        return sReturn;
    }

    public static ArrayList<Integer> getIntegerArrayFromString(String sTags, String sDelimiter){
        ArrayList<Integer> ali = new ArrayList<>();

        if(sTags != null){
            if(sTags.length() > 0) {
                String[] sa = sTags.split(sDelimiter);
                for (String s : sa) {
                    ali.add(Integer.parseInt(s));
                }
            }
        }
        return ali;
    }

    public static final String STORAGE_SIZE_NO_PREFERENCE = "";
    public static final String STORAGE_SIZE_BYTES = "B";       //Must match sSizeSuffix chars by the manner of which CleanStorageSize is used.
    public static final String STORAGE_SIZE_KILOBYTES = "KB";  //Must match sSizeSuffix chars by the manner of which CleanStorageSize is used.
    public static final String STORAGE_SIZE_MEGABYTES = "MB";  //Must match sSizeSuffix chars by the manner of which CleanStorageSize is used.
    public static final String STORAGE_SIZE_GIGABYTES = "GB";  //Must match sSizeSuffix chars by the manner of which CleanStorageSize is used.
    public static String CleanStorageSize(Long lStorageSize, String sStorageSizePreference){
        //Returns a string of size to 2 significant figures plus units of B, KB, MB, or GB.

        String sSizeSuffix = " B";
        if( sStorageSizePreference.equals(STORAGE_SIZE_NO_PREFERENCE)) {
            if (lStorageSize > 1000) {
                lStorageSize /= 1024;
                sSizeSuffix = " KB";
            }
            if (lStorageSize > 1000) {
                lStorageSize /= 1024;
                sSizeSuffix = " MB";
            }
            if (lStorageSize > 1000) {
                lStorageSize /= 1024;
                sSizeSuffix = " GB";
            }
        } else {
            switch (sStorageSizePreference){
                case STORAGE_SIZE_GIGABYTES:
                    lStorageSize = lStorageSize / (long)Math.pow(1024, 3);
                    sSizeSuffix = " GB";
                    break;

                case STORAGE_SIZE_MEGABYTES:
                    lStorageSize = lStorageSize / (long)Math.pow(1024, 2);
                    sSizeSuffix = " MB";
                    break;

                case STORAGE_SIZE_KILOBYTES:
                    lStorageSize /= 1024;
                    sSizeSuffix = " KB";

                case STORAGE_SIZE_BYTES:
                    break;
            }
        }

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);

        return decimalFormat.format(lStorageSize) + sSizeSuffix;
    }

    public static String getDurationTextFromMilliseconds(long lMilliseconds){
        String sDurationText;
        sDurationText = String.format(Locale.getDefault(),"%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(lMilliseconds),
                TimeUnit.MILLISECONDS.toMinutes(lMilliseconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(lMilliseconds)),
                TimeUnit.MILLISECONDS.toSeconds(lMilliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(lMilliseconds)));
        return sDurationText;
    }

    public static final String EXTRA_BOOL_PROBLEM = "com.agcurations.aggallerymanager.extra.BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "com.agcurations.aggallerymanager.extra.STRING_PROBLEM";
    void problemNotificationConfig(String sMessage, String sIntentActionFilter){
        Intent broadcastIntent_Problem = new Intent();
        broadcastIntent_Problem.setAction(sIntentActionFilter);
        broadcastIntent_Problem.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent_Problem.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Problem.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Problem);
    }

    //=====================================================================================
    //===== Comic Page Viewer Activity Options =====================================================
    //=====================================================================================
    //CPV = "Comic Page Viewer"
    public final float bCPV_MaxScale = 4.0f; //Max zoom.
    public final boolean bCPV_AllowZoomJump = true;
    public final float fCPV_ZoomJumpOutThreshold = 100.0f;
    public final float fCPV_ZoomJumpInThreshold = -200.0f;

    //When image is zoomed, options for pan speed:
    public final boolean bCPV_PanAcceleration = true;
    public static final int CPV_PAN_SPEED_SCALED = 1; //Pan based on zoom level.
    public static final int CPV_PAN_SPEED_FIXED = 2;  //Pan based on user-selected speed.
    public final int iCPV_PanSpeedMethod = CPV_PAN_SPEED_SCALED;
    public final float fCPV_VerticalPanScalar = 1.5f;
    public final float fCPV_HorizontalPanScalar = 1.5f;

    public static final int COMIC_VIEWER_PAGE_MATCH_HEIGHT = 0;
    public static final int COMIC_VIEWER_PAGE_MATCH_WIDTH = 1;
    public static final int COMIC_VIEWER_PAGE_FIT_SCREEN = 2;
    public int giOptionComicViewerCoverPageStartZoomConfiguration = 0;
    public int giOptionComicViewerContentPageStartZoomConfiguration = 1;

    public boolean gbOptionComicViewerShowPageNumber = false;

    //==================================================================================================
    //=========  IMPORT  ==============================================================================
    //==================================================================================================

    static final String EXTRA_IMPORT_TREE_URI = "com.agcurations.aggallerymanager.extra.IMPORT_TREE_URI";
    static final String EXTRA_FILES_OR_FOLDERS = "com.agcurations.aggallerymanager.extra.EXTRA_FILES_OR_FOLDERS";

    static final String EXTRA_COMIC_IMPORT_SOURCE = "com.agcurations.aggallerymanager.extra.COMIC_IMPORT_SOURCE";

    static final String EXTRA_IMPORT_FILES_MOVE_OR_COPY = "com.agcurations.aggallerymanager.extra.IMPORT_FILES_MOVE_OR_COPY";

    public static final String EXTRA_BOOL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_DIRECTORY_CONTENTS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_DIRECTORY_CONTENTS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_DIRECTORY_CONTENTS_RESPONSE"; //ArrayList of response data

    public static final String EXTRA_BOOL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_VIDEO_DOWNLOAD_LISTINGS_RESPONSE"; //ArrayList of response data

    public static final String EXTRA_BOOL_GET_WEB_COMIC_ANALYSIS_RESPONSE = "com.agcurations.aggallerymanager.extra.BOOL_GET_WEB_COMIC_ANALYSIS_RESPONSE"; //Used to flag in a listener.
    public static final String EXTRA_AL_GET_WEB_COMIC_ANALYSIS_RESPONSE = "com.agcurations.aggallerymanager.extra.AL_GET_WEB_COMIC_ANALYSIS_RESPONSE"; //ArrayList of response data

    public static final String EXTRA_STRING_WEB_ADDRESS = "com.agcurations.aggallerymanager.extra.STRING_WEB_ADDRESS";
    //public static final String EXTRA_LONG_DOWNLOAD_ID = "com.agcurations.aggallerymanager.extra.LONG_DOWNLOAD_ID";
    public static final String COMIC_DETAILS_SUCCESS = "COMIC_DETAILS_SUCCESS";

    public static final String EXTRA_STRING_INTENT_ACTION_FILTER = "com.agcurations.aggallerymanager.extra.STRING_INTENT_ACTION_FILTER"; //todo: used for the same as EXTRA_CALLER_ACTION_RESPONSE_FILTER?

    public static final String EXTRA_CALLER_ACTION_RESPONSE_FILTER = "com.agcurations.aggallerymanager.extra.EXTRA_CALLER_ACTION_RESPONSE_FILTER";

    public static final String FILE_DELETION_MESSAGE = "Deleting file: ";
    public static final String FILE_DELETION_OP_COMPLETE_MESSAGE = "File deletion operation complete.";

    public static final String STRING_COMIC_XML_FILENAME = "ComicData.xml";

    public static final int FOLDERS_ONLY = 0;
    public static final int FILES_ONLY = 1;

    public static final String UPDATE_LOG_BOOLEAN = "UPDATE_LOG_BOOLEAN";
    public static final String LOG_LINE_STRING = "LOG_LINE_STRING";
    public static final String UPDATE_PERCENT_COMPLETE_BOOLEAN = "UPDATE_PERCENT_COMPLETE_BOOLEAN";
    public static final String PERCENT_COMPLETE_INT = "PERCENT_COMPLETE_INT";
    public static final String UPDATE_PROGRESS_BAR_TEXT_BOOLEAN = "UPDATE_PROGRESS_BAR_TEXT_BOOLEAN";
    public static final String PROGRESS_BAR_TEXT_STRING = "PROGRESS_BAR_TEXT_STRING";

    public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";

    public ArrayList<String> alsUriFilesToDelete;

    public static String cleanHTMLCodedCharacters(String sInput){

        return Html.fromHtml(sInput,0).toString();

    }

    public void BroadcastProgress(boolean bUpdateLog, String sLogLine,
                                  boolean bUpdatePercentComplete, int iAmountComplete,
                                  boolean bUpdateProgressBarText, String sProgressBarText,
                                  String sIntentActionFilter){

        //Preserve the log for the event of a screen rotation, or activity looses focus:
        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        globalClass.gsbImportExecutionLog.append(sLogLine);

        if(sIntentActionFilter.equals(Fragment_Import_6_ExecuteImport.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_EXECUTE_RESPONSE)) {
            if (bUpdatePercentComplete) {
                globalClass.giImportExecutionProgressBarPercent = iAmountComplete;
            }
            if (bUpdateProgressBarText) {
                globalClass.gsImportExecutionProgressBarText = sProgressBarText;
            }
        }

        if(sIntentActionFilter.equals(
                Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE)) {
            if(bUpdatePercentComplete) {
                globalClass.giImportFolderAnalysisProgressBarPercent = iAmountComplete;
            }
            if(bUpdateProgressBarText){
                globalClass.gsImportFolderAnalysisProgressBarText = sProgressBarText;
            }
        }

        //Broadcast a message to be picked-up by the Import Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(sIntentActionFilter);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        broadcastIntent.putExtra(UPDATE_LOG_BOOLEAN, bUpdateLog);
        broadcastIntent.putExtra(LOG_LINE_STRING, sLogLine);
        broadcastIntent.putExtra(UPDATE_PERCENT_COMPLETE_BOOLEAN, bUpdatePercentComplete);
        broadcastIntent.putExtra(PERCENT_COMPLETE_INT, iAmountComplete);
        broadcastIntent.putExtra(UPDATE_PROGRESS_BAR_TEXT_BOOLEAN, bUpdateProgressBarText);
        broadcastIntent.putExtra(PROGRESS_BAR_TEXT_STRING, sProgressBarText);

        //sendBroadcast(broadcastIntent);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }




    //=====================================================================================
    //===== Import Options ================================================================
    //=====================================================================================

    boolean gbUseDownloadManager = true;

    //A flag to turn on/off video file duration analysis:
    public final static boolean bVideoDeepDirectoryContentFileAnalysis = true;
        //This flag allows the program to analyze video duration prior to import to allow
        //  sorting video files by duration in the file listView presented to the user. The user
        //  selects which files to import, and may wish to sort by video duration. If the user
        //  does this without this option turned on, the sort will be inaccurate.
        //  This deep analysis takes longer.

    public final static int iComicFolderImportMaxPageSkip = 2;
        //The program attempts to recognize page numbers in filenames during comic import via folder.
        //  The program alerts user to possible missing pages. This factor allows some leniency
        //  if the user is missing pages from their comic folder - perhaps a first page, or a middle
        //  page. There is no way to determine if end pages are missing. This feature is rather inert
        // and only gives a message to the user to indicate that pages might be missing.

    //nHentai comic import web html search strings (may change if the website changes)
    //If comic source is nHentai, these strings enable searching the nHentai web page for tag data:
    //public String snHentai_Default_Comic_Address_Prefix = "https://nhentai.net/g/";
    //public final String snHentai_Comic_Address_Prefix = "https://nhentai.net/g/";
    //public String snHentai_Default_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    public final String snHentai_Comic_Title_xPathExpression = "//div[@id='info-block']//h1[@class='title']//span[@class='pretty']";
    //public String snHentai_Default_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";
    public final String snHentai_Comic_Data_Blocks_xPE = "//div[@class='tag-container field-name']/..";
    public final String snHentai_Comic_Cover_Thumb_xPE = "//div[@id='bigcontainer']//img[@class='lazyload']";
    public final String snHentai_Comic_Page_Thumbs_xPE = "//div[@class='thumb-container']//img[@class='lazyload']";

    //Video import web html search strings (may change if the website changes)

    boolean gbLogM3U8Files = false;     //Writes M3U8 files as they are read and analyzed as part of an
                                        //  import interpretive analysis.

    public final boolean gbOptionSilenceActiveStreamListening = true;
        //This option for video import from web source. During detection, if the user plays the
        //  the video a stream may continually request .ts files. This attempts to filter them out.
        //  However, it may block content that the user wants. Thus, allow it to be an option.


    //Create an array of keys that allow program to locate video links:
    ArrayList<ItemClass_WebVideoDataLocator> galWebVideoDataLocators;

    //Create an array of keys that allow program to locate image links:
    ArrayList<ItemClass_WebComicDataLocator> galWebComicDataLocators;

    public static final int DOWNLOAD_WAIT_TIMEOUT = 2 * 60 * 60 * 1000; //2 hours in milliseconds.

    //==============================================================================================
    //=========== Other Options ====================================================================

    public static int giLogFileKeepDurationInDays = 30;

    public static boolean gbOptionUserAutoLogin = false;

    public static boolean gbOptionIndividualizeM3U8VideoSegmentPlayback = false;
    //If the option to individualize M3U8 video segment playback is selected,
    //  create an array of the individual video segment files and feed
    //  them into the ExoPlayer as a playlist.
    //  There was an issue during coding and testing an SAF-adapted M3U8
    //  in which the program would freeze the entire tablet causing the
    //  need for a hard reset. If this happens again, a coder can change the
    //  buffer amount (in onCreate), or configure this boolean to be
    //  user-configurable.
    //  If the option to individualize M3U8 video segment playback is not selected,
    //  play an SAF-adapted M3U8 file. That is, a file with video listings
    //  of Android Storage Access Framework Uris.

    //==============================================================================================
    //=========== Preferences ======================================================================

    //Data storage location:
    public static final String gsPreference_DataStorageLocationUri = "com.agcurations.aggallerymanager.preferences.DataStorageLocation";
    //Data storage location, user-friendly for display purposes:
    public static final String gsPreference_DataStorageLocationUriUF = "com.agcurations.aggallerymanager.preferences.DataStorageLocationUF";

    public static final String gsPreferenceName_UserAccountData = "com.agcurations.aggallerymanager.preferences.UserAccountData";

    public static final String[]  gsRestrictedTagsPreferenceNames = new String[]{
            "multi_select_list_videos_restricted_tags",
            "multi_select_list_images_restricted_tags",
            "multi_select_list_comics_restricted_tags"};

    private static final String[] gsCurrentMaxItemIDStoredPreference = new String[]{
            "current_max_item_ID_videos",
            "current_max_item_ID_images",
            "current_max_item_ID_comics"
    };

    public static final String gsPinPreference = "preferences_pin";

    public static final String gsPreference_Import_IncludeGraphicsFileData = "com.agcurations.aggallerymanager.preferences.ImportIncludeGraphicsFileData";


    public static final String PREF_WEB_TAB_PREV_FOCUS_INDEX = "com.agcurations.aggallerymanager.preference.web_tab_prev_focus_index";

    public static String PREF_APPLICATION_LOG_PATH_FILENAME = "APPLICATION_LOG_PATH_FILENAME";
    public static String PREF_WRITE_APPLICATION_LOG_FILE = "WRITE_APPLICATION_LOG_FILE";

    public static String PREF_USE_FFMPEG_TO_MERGE_VIDEO_STREAMS = "USE_FFMPEG_TO_MERGE_VIDEO_STREAMS";

}

