package com.agcurations.aggallerymanager;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowMetrics;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;

import com.google.common.io.BaseEncoding;


public class GlobalClass extends Application {
    //GlobalClass built using this guide:
    //https://androidexample.com/Global_Variable_Or_Application_Context_Variable_-_Android_Example/index.php?view=article_discription&aid=114&aaid=136

    //Each section, Videos, Pictures, Comics, has its own folder, catalog file, tags file, and log folder.

    //Global Variables:

    public static final String EXTRA_STRING_STATUS_MESSAGE = "com.agcurations.aggallerymanager.extra.String_Status_Message";

    public static NotificationChannel notificationChannel;
    public static NotificationManager notificationManager;
    public static final String NOTIFICATION_CHANNEL_ID = "com.agcurations.aggallerymanager.NOTICIFATION_CHANNEL";
    public static final String NOTIFICATION_CHANNEL_NAME = "Download progress & completion";
    public static final String NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications from AGGalleryManager, such as download progress or completion.";
    public static int iNotificationID = 0;

    public static String gsPin = "";

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
    public static Uri gUriAppLogFile;
    public static final String gsBrowserDataFile = "WebpageTabData.dat";
    public static Uri gUriBrowserDataFolder;
    public static Uri gUriWebpageTabDataFile;
    public static final String gsImageDownloadHoldingFolderName = "Holding";
    public static final String gsImageDownloadHoldingTempFolderName = "Holding";
    public static Uri gUriImageDownloadHoldingFolder; //Used to hold individual images downloaded by the user from the browser prior to import.
    public static File gfDownloadExternalStorageFolder;  //Destination root for DownloadManager Downloaded files. Android limits DL destination locations.
    public static File gfImageDownloadHoldingFolderTemp; //Used to hold download manager files temporarily, to be moved so that DLM can't find them for cleanup operations.
    public static String gsImageDownloadHoldingFolderTempRPath; //For coordinating file transfer from internal storage to SD card.
    public static AtomicBoolean[] gAB_CatalogFileAvailable = {new AtomicBoolean(true), new AtomicBoolean(true), new AtomicBoolean(true)};
    public static final Uri[] gUriCatalogContentsFiles = new Uri[3];
    public static final Uri[] gUriCatalogTagsFiles = new Uri[3];

    //Start Catalog Anaylsis globals
    public static List<TreeMap<String, ItemClass_File>> gtmicf_AllFileItemsInMediaFolder = new ArrayList<>(); //Used for catalog analysis
    // The treemap variable "gtmicf_AllFileItemsInMediaFolder" is an array list of 3 treemaps. Each
    // treemap key consists of
    // icf.sMediaFolderRelativePath + GlobalClass.gsFileSeparator + icf.sFileOrFolderName.
    // icf.sMediaFolderRelativePath excludes the media type folder. Media type folder is identified by the media type array index.
    //End Catalog Analysis globals
    public static ArrayList<ItemClass_File> galf_Orphaned_Files;

    public static ContentResolver gcrContentResolver;

    public static int PRINTABLE = 0;
    public static int CHECKABLE = 1;
    public static final String[][] gsIllegalRecordStrings = { //{Printable notification, actual illegal string/character}
            {"%%", "%%"},               //"%%" is an illegal character sequence in html addresses. It is used to deliminate back/forward history of browser tabs.
            {"newline", "\n"},          //Newline will break the .dat files as records are deliminated by newline.
            {"carriage return", "\r"},  //Carriage return alone may break the .dat files as records are deliminated by newline.
            {"tab", "\t"}};             //Tab is used to separate fields in the .dat files.

    //Tag variables:
    public static final List<TreeMap<Integer, ItemClass_Tag>> gtmCatalogTagReferenceLists = new ArrayList<>();
    public static final List<TreeMap<Integer, ItemClass_Tag>> gtmApprovedCatalogTagReferenceLists = new ArrayList<>();
    public static AtomicBoolean gabTagsLoaded = new AtomicBoolean(false);
    public static AtomicBoolean gabDataLoaded = new AtomicBoolean(false);
    public static final List<TreeMap<String, ItemClass_CatalogItem>> gtmCatalogLists = new ArrayList<>();
    public static final String[] gsCatalogFolderNames = {"Videos", "Images", "Comics"};

    public static final int LOADING_STATE_NOT_STARTED = 0;
    public static final int LOADING_STATE_STARTED = 1;
    public static final int LOADING_STATE_FINISHED = 2;
    public static int giLoadingState = LOADING_STATE_NOT_STARTED;



    //Activity_CatalogViewer variables:
    public static TreeMap<Integer, ItemClass_CatalogItem> gtmCatalogViewerDisplayTreeMap;
    public static TreeMap<Integer, ItemClass_CatalogItem> gtmCatalogAdjacencyAnalysisTreeMap;
    public static final int SORT_BY_DATETIME_LAST_VIEWED = 0;
    public static final int SORT_BY_DATETIME_IMPORTED = 1;
    public static int[] giCatalogViewerSortBySetting = {SORT_BY_DATETIME_LAST_VIEWED, SORT_BY_DATETIME_LAST_VIEWED, SORT_BY_DATETIME_LAST_VIEWED};
    public static String gsCatalogViewerSortBySharedWithUser = "";
    public static final String[] gsCatalogViewerPreferenceNameSortBy = {"VIDEOS_SORT_BY", "IMAGES_SORT_BY", "COMICS_SORT_BY"};
    public static final String[] gsCatalogViewerPreferenceNameSortAscending = {"VIDEOS_SORT_ASCENDING", "IMAGES_SORT_ASCENDING", "COMICS_SORT_ASCENDING"};
    public static boolean[] gbCatalogViewerSortAscending = {true, true, true};
    //Search and Filter Variables:
    public static String[] gsCatalogViewerSearchInText = {"", "", ""};
    public static final int FILTER_BY_NO_SELECTION = 0;
    public static final int FILTER_BY_WEBSOURCE = 1;
    public static final int FILTER_BY_FOLDERSOURCE = 2;
    public static final int FILTER_BY_NOTAGS = 3;
    public static final int FILTER_BY_ITEMPROBLEM = 4;
    public static String[] gsCatalogViewerSearchByGroupID = {"", "", ""};
    public static int[] giCatalogViewerFilterBySelection = {FILTER_BY_NO_SELECTION, FILTER_BY_NO_SELECTION, FILTER_BY_NO_SELECTION};
    //Variables for the Sort & Filter resolution/pagecount RangeSlider:
    public static TreeMap<Integer, Integer> gtmVideoResolutions;
    public static int giMinVideoResolutionSelected = -1;
    public static int giMaxVideoResolutionSelected = -1;
    public static int giMinImageMegaPixels = -1;
    public static int giMaxImageMegaPixels; //todo: carry float here.
    public static int giMinImageMegaPixelsSelected = -1;//todo: carry float here.
    public static int giMaxImageMegaPixelsSelected = -1;//todo: carry float here.
    public static int giMinComicPageCount = -1;
    public static int giMaxComicPageCount;
    public static int giMinComicPageCountSelected = -1;
    public static int giMaxComicPageCountSelected = -1;
    public static long glMaxVideoDurationMS = 1; //For the filter range slider.
    public static long glMinVideoDurationMSSelected = -1;
    public static long glMaxVideoDurationMSSelected = -1;
    public static ArrayList<TreeSet<Integer>> galtsiCatalogViewerFilterTags;
    //public boolean gbGuestMode;
    public static boolean gbCatalogViewerRefresh = false; //Used when data is edited.
    //public ArrayList<TreeMap<Integer, Integer>> galtmTagHistogram;
    public static boolean[] gbTagHistogramRequiresUpdate = {true, true, true};
    //End catalog viewer variables.

    public static final String gsSAF_Adapted_M3U8_Suffix = "_SAF_Adapted";

    public static final String gsUnsortedFolderName = "etc";  //Todo: this folder should not be used anymore. Was used when a tag was not assigned to an item.

    public static AtomicBoolean gabImportFileListTMAvailable = new AtomicBoolean(true);
    public static TreeMap<String, ArrayList<ItemClass_File>> gtmalImportFileList = new TreeMap<>(); //Used to pass a large list of files to import to the import service.
    public static ArrayList<ItemClass_File> galPreviewFileList; //Same as above, but for preview.
    //  This is done because the list of files can exceed the intent extra transaction size limit.

    public static final int MOVE = 0;
    public static final int COPY = 1;
    public static final String[] gsMoveOrCopy = {"Move", "Copy", "Moving", "Copying"};

    public static boolean gbIsDarkModeOn = false;

    public static ArrayList<ItemClass_WebPageTabData> gal_WebPagesForCurrentUser;
    public static ArrayList<ItemClass_WebPageTabData> gal_WebPagesForOtherUsers;

    public static boolean gbWorkerVideoAnalysisInProgress = false;

    public static String gsUserDataFileName = "Data.dat";
    public static Uri gUriUserDataFile;
    public static ArrayList<ItemClass_User> galicu_Users;
    public static ItemClass_User gicuCurrentUser; //If null, routines will use the default maturity rating.
    public static int giDefaultUserMaturityRating = AdapterMaturityRatings.MATURITY_RATING_M; //todo: Setting - add to settings
    public static int giMinTagMaturityFilter = 0; //To filter tags based on maturity, configured by the current user.
    public static int giMaxTagMaturityFilter = giDefaultUserMaturityRating; //To filter tags based on maturity, configured by the current user.
    public static int giMinContentMaturityFilter = 0; //To filter content based on maturity, configured by the current user.
    public static int giMaxContentMaturityFilter = giDefaultUserMaturityRating; //To filter content based on maturity, configured by the current user.

    public static String gsRefreshCatalogViewerThumbnail = ""; //Used to refresh thumbnail.

    public static final int AGGM_MAX_FILENAME_LENGTH = 46; //For characters up to the '.' extension divider. Primarily needed to avoid Android Download Manager Truncation faults.

    //=====================================================================================
    //===== Background Service Tracking Variables =========================================
    //=====================================================================================
    //These vars not in a ViewModel as a service can continue to run after an activity is destroyed.

    //Variables to control starting of import folder content analysis:
    // These variables prevent the system/user from starting another folder analysis until an
    // existing folder analysis operation is finished.
    //public boolean gbImportFolderAnalysisStarted = false; This item not needed for this fragment.
    public static AtomicBoolean gabImportFolderAnalysisRunning = new AtomicBoolean(false);
    public static AtomicBoolean gabImportHoldingFolderAnalysisAutoStart = new AtomicBoolean(false);
    public static AtomicBoolean gabImportFolderAnalysisStop = new AtomicBoolean(false);
    public static AtomicBoolean gabImportFolderAnalysisFinished = new AtomicBoolean(false);
    public static StringBuilder gsbImportFolderAnalysisLog = new StringBuilder();
    public static int giImportFolderAnalysisProgressBarPercent = 0;
    public static String gsImportFolderAnalysisProgressBarText = "";
    public static String gsImportFolderAnalysisSelectedFolder = "";
    //Variables to control starting of import execution:
    // These variables prevent the system/user from starting another import until an existing
    // import operation is finished.
    public static AtomicBoolean gabImportExecutionStarted = new AtomicBoolean(false);
    public static AtomicBoolean gabImportExecutionRunning = new AtomicBoolean(false);
    public static AtomicBoolean gabImportExecutionFinished = new AtomicBoolean(false);
    public static StringBuilder gsbImportExecutionLog = new StringBuilder();
    public static int giImportExecutionProgressBarPercent = 0;
    public static String gsImportExecutionProgressBarText = "";

    //Variables related to catalog analysis:
    public static StringBuilder gsbCatalogAnalysis_ExecutionLog = new StringBuilder();
    public static int giCatalogAnalysis_ProgressBarPercent = 0;
    public static String gsCatalogAnalysis_ProgressBarText = "";
    public static StringBuilder gsbUpdateExecutionLog = new StringBuilder();
    public static int giUpdateExecutionProgressBarPercent = 0;
    public static String gsUpdateExecutionProgressBarText = "";
    public static int giCatalog_Analysis_Approx_Max_Results = 100;

    //Variables to control starting of comic web address analysis:
    // These variables prevent the system/user from starting another analysis until an existing
    // operation is finished.
    public static AtomicBoolean gabComicWebAnalysDataTMAvailable = new AtomicBoolean(true);
    public static TreeMap<String, ItemClass_WebComicDataLocator> gtmComicWebDataLocators = new TreeMap<>();
    public static AtomicBoolean gabImportComicWebAnalysisRunning = new AtomicBoolean(false);
    public static AtomicBoolean gabImportComicWebAnalysisFinished = new AtomicBoolean(false);



    //==============================
    // Global variables for working with the File Deletion Utility internal to this program:
    public static AtomicBoolean gabGeneralFileDeletionStart = new AtomicBoolean(false);
    public static AtomicBoolean gabGeneralFileDeletionRunning = new AtomicBoolean(false);
    public static AtomicBoolean gabGeneralFileDeletionCancel = new AtomicBoolean(false);
    public static ArrayList<ItemClass_File> galicf_FilesToDeleteDataTransfer = new ArrayList<>();
    public static StringBuilder gsbDeleteFilesExecutionLog = new StringBuilder();
    public static int giDeleteFilesExecutionProgressBarPercent = 0;
    public static String gsDeleteFilesExecutionProgressBarText = "";
    //==============================

    //The variable below is used to identify files that were acquired using the Android DownloadManager.
    //  The Android DownloadIdleService will automatically delete the files that this program downloads
    //  after about a week. This program must go through and find these files and rename them so that
    //  the service does not delete them.
    //  See https://www.vvse.com/blog/blog/2020/01/06/android-10-automatically-deletes-downloaded-files/.
    //  See https://android.googlesource.com/platform/packages/providers/DownloadProvider/+/master/src/com/android/providers/downloads/DownloadIdleService.java#109.
    //  See https://developer.android.com/reference/android/app/DownloadManager.Request.html#setVisibleInDownloadsUi(boolean).
    public static String gsDLTempFolderName = "DL";

    public static String gsApplicationLogName = "ApplicationLog.txt";
    public static AtomicBoolean gAB_ApplicationLogFileAvailable = new AtomicBoolean(true);

    public static final String EXTRA_CALLER_ID = "com.agcurations.aggallermanager.string_caller_id";
    public static final String EXTRA_CALLER_TIMESTAMP = "com.agcurations.aggallermanager.long_caller_timestamp";

    public static String gsGroupIDClip = "";
    public static boolean gbClearGroupIDAtImportClose = false;

    //=====================================================================================
    //===== MIME TYPES ====================================================================
    //=====================================================================================

    public static final String BASE_TYPE_TEXT = "text";

    //=====================================================================================
    //===== Network Monitoring ============================================================
    //=====================================================================================
    public boolean isNetworkConnected = false;
    public static ConnectivityManager connectivityManager;
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
        WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
        pReturn.x = windowMetrics.getBounds().width();// - insets.left - insets.right;
        pReturn.y = windowMetrics.getBounds().height();// - insets.top - insets.bottom;
        return pReturn;
    }

    public int ConvertDPtoPX(int dp){
        //Pixel
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

    public static int[] calculateGroupingControlsColors(String sUUID){
        int[] iReturnColors = new int[4];
        String[] sColors = new String[]{sUUID.substring(0, 2),
                sUUID.substring(2,4),
                sUUID.substring(4,6)};
        byte[] bytes;
        byte[] byteColors = new byte[3];
        for(int i = 0; i < 3; i++){
            bytes = BaseEncoding.base16().decode(sColors[i].toUpperCase());
            byteColors[i] = bytes[0];
        }
        //Bytes for colors acquired.

        //Convert to scale 0-255:
        int[] iColors = new int[3];
        for(int i = 0; i < 3; i++){
            if(byteColors[i] < 0){
                iColors[i] = byteColors[i] + 256;
            } else {
                iColors[i] = byteColors[i];
            }
        }

        //Normalize the colors:
        float[] fColorsNormalized = new float[3];
        for(int i = 0; i < 3; i++){
            fColorsNormalized[i] = iColors[i] / 255f;
        }

        //Determine the min and max values:
        float fMin = Math.min(fColorsNormalized[0], Math.min(fColorsNormalized[1], fColorsNormalized[2] ));
        float fMax = Math.max(fColorsNormalized[0], Math.max(fColorsNormalized[1], fColorsNormalized[2] ));

            /*
            The Hue formula is depending on what RGB color channel is the max value. The three different formulas are:
            If Red is max, then Hue = (G-B)/(max-min)
            If Green is max, then Hue = 2.0 + (B-R)/(max-min)
            If Blue is max, then Hue = 4.0 + (R-G)/(max-min)
            */
        float fR = fColorsNormalized[0];
        float fG = fColorsNormalized[1];
        float fB = fColorsNormalized[2];
        float fHue;
        float fDelta = fMax - fMin;
        if(fR > fG && fR > fB) {
            //Red is max
            fHue = 60 * ( ( (fG - fB) / fDelta) % 6);
        } else if(fG > fR && fG > fB) {
            //Green is Max
            fHue = 60 * (2f + (fB - fR) / fDelta);
        } else {
            //Blue is Max
            fHue = 60 * (4f + (fR - fG) / fDelta);
        }
        if(fHue < 0){
            fHue += 360;
        } else if (fHue > 360) {
            fHue -= 360;
        }


        //Ignore potential to derive the luminance from the original RGB and set the luminance hard
        // so that the coloring is not too dim:
        //* When 0 ≤ L ≤ 1:
        float fLum = .50f;

        //Ignore potential to derive the saturation from the original RGB values and set the
        //  saturation so that it is not ugly:
        //* When 0 ≤ S ≤ 1:
        float fSat = 1.0f;

        //Get color for controls' background:
        String sBackgroundColor = getRGBString(fHue, fSat, fLum);
        //Get colors for foreground controls:
        int iContrastColor = getContrastColor(Color.parseColor(sBackgroundColor));
        iReturnColors[0] = Color.parseColor(sBackgroundColor); //iGroupingControlsColor
        iReturnColors[1] = iContrastColor; //iGroupingControlsContrastColor

        //Get color for control highlight (specifically for when the 'filter by group ID' feature is activated):
        float fGroupingControlHighlight = fHue + 180;
        if(fGroupingControlHighlight > 360){
            fGroupingControlHighlight -= 360;
        }
        String sGCHColor = getRGBString(fGroupingControlHighlight, fSat, fLum);
        int iGCHContrastColor = getContrastColor(Color.parseColor(sGCHColor));
        iReturnColors[2] = Color.parseColor(sGCHColor); //iGroupingControlsHighlightColor
        iReturnColors[3] = iGCHContrastColor; //iGroupingControlsHighlightContrastColor

        return iReturnColors;
    }

    private static String getRGBString(float fHue, float fSat, float fLum){
        //Calculate RGB:
            /*https://www.rapidtables.com/convert/color/hsl-to-rgb.html
            * When 0 ≤ H < 360, 0 ≤ S ≤ 1 and 0 ≤ L ≤ 1:
                C = (1 - |2L - 1|) × S
                X = C × (1 - |(H / 60°) mod 2 - 1|)
                m = L - C/2
                (R,G,B) = ((R'+m)×255, (G'+m)×255,(B'+m)×255)
            * */
        float fC, fX, fm;
        fC = (1 - Math.abs(2*fLum - 1)) * fSat;
        fX = fC * (1 - Math.abs((fHue / 60) % 2 - 1));
        fm = fLum - fC / 2;
        float[] fP = new float[3];
        if( fHue >= 0 && fHue < 60){
            fP = new float[]{fC, fX, 0};
        } else if( fHue >= 60 && fHue < 120){
            fP = new float[]{fX, fC, 0};
        } else if( fHue >= 120 && fHue < 180){
            fP = new float[]{0, fC, fX};
        } else if( fHue >= 180 && fHue < 240){
            fP = new float[]{0, fX, fC};
        } else if( fHue >= 240 && fHue < 300){
            fP = new float[]{fX, 0, fC};
        } else if( fHue >= 300 && fHue < 360){
            fP = new float[]{fC, 0, fX};
        }
        int[] iColors = new int[]{(int)((fP[0]+fm)*255),
                (int)((fP[1]+fm)*255),
                (int)((fP[2]+fm)*255)};

        //Convert the values to byte:
        //Convert to scale 0 to 127 : -128 to 0:
        byte[] byteColors = new byte[3];
        for(int i = 0; i < 3; i++){
            if(iColors[i] >= 128){
                iColors[i] = iColors[i] - 256;
            }
            byteColors[i] = (byte) iColors[i];
        }

        //Form the color scheme:
        StringBuilder sbColorString = new StringBuilder();
        sbColorString.append("#");
        for(int i = 0; i < 3; i++){
            String sColor = String.format("%02X", (short) byteColors[i]);
            if(sColor.length() > 2){
                sColor = sColor.substring(2);
            }
            sbColorString.append(sColor);
        }

        return sbColorString.toString();
    }

    @ColorInt
    public static int getContrastColor(@ColorInt int color) {
        //https://stackoverflow.com/questions/1855884/determine-font-color-based-on-background-color
        // Counting the perceptive luminance - human eye favors green color...
        double a = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return a < 0.5 ? Color.BLACK : Color.WHITE;
    }

    public static void applyGroupingControlsColor(ItemClass_CatalogItem ci,
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

    static final String gsDatePatternNumSort = "yyyyMMdd.HHmmssSSS";
        //This one has milliseconds because the import of files can come in multiple files faster
        // than 1 second. In this case, sorting by date imported can give out-of-intended-sequence
        // results.
    static DateTimeFormatter gdtfDateFormatter;

    public static Double GetTimeStampDouble(){
        //Get an easily-comparable time stamp.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternNumSort);
        String sTimeStamp = gdtfDateFormatter.format(LocalDateTime.now());
        return Double.parseDouble(sTimeStamp);
    }

    public static Double GetTimeStampDouble(Date dateInput){
        //Get an easily-comparable time stamp.
        gdtfDateFormatter = DateTimeFormatter.ofPattern(gsDatePatternNumSort)
                .withZone( ZoneId.systemDefault() );
        Instant instant = dateInput.toInstant();
        String sTimeStamp = gdtfDateFormatter.format(instant);
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

    /**
     * Splits a file name into its base name and extension.
     *
     * @param sFileName                         A filename of form [base name].[extension]
     *
     * @return String[]                         A String array of 2 elements: File base name & file extension. The extension excludes the "." delimiter.
     */
    public static String[] SplitFileNameIntoBaseAndExtension(String sFileName){
        //The returned extension excludes the last "." in the full file name.
        String sBaseName = sFileName.substring(0,sFileName.lastIndexOf("."));
        String sExtension = sFileName.substring(sFileName.lastIndexOf(".") + 1);
        return new String[]{sBaseName, sExtension};
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
        Cursor cursor = null;
        try {
            cursor = gcrContentResolver.query(childrenUri, new String[] {
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
            if(cursor != null) {
                while (cursor.moveToNext()) {
                    final String sFileName = cursor.getString(0);
                    final String sMimeType = cursor.getString( 1);
                    if(!sMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                        alsFileNames.add(sFileName);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.d("GlobalClass:GetDirectoryFileNames()", "Problem querying folder.");
        }
        return alsFileNames;
    }
    public static ArrayList<String> GetDirectoryFileNames(String sUriParent){
        //This routine does not return folder names!
        Uri uriParent = Uri.parse(sUriParent);
        return GetDirectoryFileNames(uriParent);
    }
    @NonNull
    public static ArrayList<ItemClass_File> GetDirectoryFileNamesData(Uri uriParent){
        //This routine does not return folder names!
        ArrayList<ItemClass_File> alicf_Files = new ArrayList<>();

        String sUriParent = uriParent.toString();

        if(!GlobalClass.CheckIfFileExists(uriParent)){
            //The program will crash if the folder does not exist.
            return null;
        }
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriParent,
                DocumentsContract.getDocumentId(uriParent));
        Cursor cursor = null;
        try {
            cursor = gcrContentResolver.query(childrenUri, new String[] {
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE}, null, null, null);
            if(cursor != null) {
                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                while (cursor.moveToNext()) {

                    final String sMimeType = cursor.getString( 1);

                    if(!sMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                        final String sFileName = cursor.getString(0);
                        final long lLastModified = cursor.getLong(2); //milliseconds since January 1, 1970 00:00:00.0 UTC.
                        final String sFileSize = cursor.getString(3);

                        ItemClass_File icf = new ItemClass_File(ItemClass_File.TYPE_FILE, sFileName);

                        icf.sMimeType = sMimeType;

                        cal.setTimeInMillis(lLastModified);
                        icf.dateLastModified = cal.getTime();

                        icf.lSizeBytes = Long.parseLong(sFileSize);
                        icf.sUriParent = sUriParent; //Among other things, used to determine if pages belong to a comic or an M3U8 playlist.
                        alicf_Files.add(icf);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.d("GlobalClass:GetDirectoryFileNamesData()", "Problem querying folder.");
        }
        return alicf_Files;
    }
    public static ArrayList<ItemClass_File> GetDirectoryFileNamesData(String sUriParent){
        //This routine does not return folder names!
        Uri uriParent = Uri.parse(sUriParent);
        return GetDirectoryFileNamesData(uriParent);
    }

    @NonNull
    public static ArrayList<String> GetDirectorySubfolderNames(Uri uriParent){
        //This routine does not return file names!
        ArrayList<String> alsFolderNames = new ArrayList<>();

        if(!GlobalClass.CheckIfFileExists(uriParent)){
            //The program will crash if the folder does not exist.
            return alsFolderNames; //Let it behave as if there are no folders in the folder.
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
                    final String sItemName = c.getString(0);
                    final String sMimeType = c.getString( 1);
                    if(sMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                        alsFolderNames.add(sItemName);
                    }
                }
                c.close();
            }
        } catch (Exception e) {
            Log.d("GlobalClass:GetDirectorySubfolderNames()", "Problem querying folder.");
        }
        return alsFolderNames;
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
                c.close();
            }
        } catch (Exception e) {
            Log.d("GlobalClass:IsDirEmpty()", "Problem querying folder.");
        }
        return EMPTY;
    }


    public static String GetUserFriendlyStorageName(String sStoragePath) throws UnsupportedEncodingException {

        for(Map.Entry<String, String> entryStorageDef: GlobalClass.gtmStorageDeviceNames.entrySet()){
            String sKey = entryStorageDef.getKey();
            if(sKey.contains("/")){
                sKey = sKey.substring(sKey.lastIndexOf("/"));
                sKey = sKey.replace("/", "");
            }
            if(sStoragePath.contains(sKey)){
                //Replace the cryptic storage location text with something the user is more likely to understand:
                sStoragePath = sStoragePath.replace(sKey, entryStorageDef.getValue());
                break;
            }
        }
        if(sStoragePath.contains("/")) {
            sStoragePath = sStoragePath.substring(sStoragePath.lastIndexOf("/"));
            sStoragePath = sStoragePath.replace("/", "");
        }

        sStoragePath = URLDecoder.decode(sStoragePath, StandardCharsets.UTF_8.toString());

        if(sStoragePath.contains(":")){
            sStoragePath = sStoragePath.replace(":", "://");
        }

        return sStoragePath;
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

    public static String GetParentUri(String sUriChild){
        String sUriParent = sUriChild.substring(0, sUriChild.lastIndexOf(gsFileSeparator));
        return sUriParent;
    }

    public static Uri GetParentUri(Uri uriChild){
        return Uri.parse(GetParentUri(uriChild.toString()));
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

    public static String cleanFileNameViaTrim(String sFilename){
        //Use when expecting the begining of the filename to be ok, but trailing data may have illegal chars.
        //Useful when file is a download from a URL.
        //Example:
        //hls-480p-fe73881.ts?e=1622764694&l=0&h=1d7be9f2e81e3a2a2d1e8d3be7dff1bc
        String sOutput = sFilename;
        String[] sReservedChars = {
                "|",
                "\\",
                "?",
                "*",
                "<",
                "\"",
                ":",
                ">",
                "+",
                "[",
                "]",
                "'"};
        for(String sReservedChar: sReservedChars) {
            int iReservedCharLocation = sOutput.indexOf(sReservedChar);
            if( iReservedCharLocation > 0){
                sOutput = sOutput.substring(0, iReservedCharLocation);
            }

        }

        return sOutput;
    }

    public static void ShowMessage(Context context, String sTitle, String sMessage){
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogCustomStyle);
        if(sTitle != null){
            if(sTitle != ""){
                builder.setTitle(sTitle);
            }
        }
        if(sMessage != null){
            if(sMessage != ""){
                builder.setMessage(sMessage);
            }
        }
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     *
     * @param abIsReady AtomicBoolean to monitor for readiness. Routine returns when true.
     * @param iMaxWaitTimeMinutes How long to wait before a "failure".
     * @return Returns true if ready. Returns false if max wait time exceeded.
     */
    public boolean WaitForObjectReady(AtomicBoolean abIsReady, int iMaxWaitTimeMinutes){
        int iMaxWaitTimeSeconds = iMaxWaitTimeMinutes * 60;
        long lMaxWaitTimeMS = iMaxWaitTimeSeconds * 1000L;
        long lCummulativeWaitTimeMS = 0;
        int iWaitTimeInvervalMS = 250;
        while(!abIsReady.get() && lCummulativeWaitTimeMS < lMaxWaitTimeMS){
            try {
                Thread.sleep(iWaitTimeInvervalMS);
                lCummulativeWaitTimeMS += iWaitTimeInvervalMS;
            } catch (Exception ignored){
                return false;
            }
        }
        if(lCummulativeWaitTimeMS >= lMaxWaitTimeMS){
            return false;
        }
        return true;
    }


    //=====================================================================================
    //===== Catalog Subroutines Section ===================================================
    //=====================================================================================
    public static final String EXTRA_BOOL_DELETE_ITEM = "com.agcurations.aggallerymanager.extra.delete_item";
    public static final String EXTRA_BOOL_DELETE_ITEM_RESULT = "com.agcurations.aggallerymanager.extra.delete_item_result";
    public static final String EXTRA_BOOL_REFRESH_CATALOG_DISPLAY = "com.agcurations.aggallerymanager.extra.refresh_catalog_display";
    public static final String EXTRA_CATALOG_ITEM = "com.agcurations.aggallerymanager.extra.catalog_item";
    public static final String EXTRA_CATALOG_ITEM_ID = "com.agcurations.aggallerymanager.extra.catalog_item_id";
    public static final String EXTRA_DATA_FILE_URI_STRING = "com.agcurations.aggallerymanager.extra.data_file_uri_string";

    public static final int giCatalogFileVersion = 7;
    public static String getCatalogHeader(){
        String sHeader = "";
        sHeader = sHeader + "MediaCategory";                        //Video, image, or comic.
        sHeader = sHeader + "\t" + "ItemID";                        //Video, image, comic id
        sHeader = sHeader + "\t" + "GroupID";                       //Group ID
        sHeader = sHeader + "\t" + "Group Sequence";                //Sequence number within a Group
        sHeader = sHeader + "\t" + "Filename";                      //Video or image filename
        sHeader = sHeader + "\t" + "Folder Relative Path";          //Name of the folder holding the video, image, or comic pages
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
        sHeader = sHeader + "\t" + "Comic Volume";                  //Comic name or Video Title
        sHeader = sHeader + "\t" + "Comic Chapter";                 //Comic name or Video Title
        sHeader = sHeader + "\t" + "Comic Subtitle";                //Comic name or Video Title
        sHeader = sHeader + "\t" + "ComicPages";                    //Total number of pages as defined at the comic source
        sHeader = sHeader + "\t" + "Comic_Max_Page_ID";             //Max comic page id extracted from file names
        sHeader = sHeader + "\t" + "Comic_Missing_Pages";           //Missing page numbers
        sHeader = sHeader + "\t" + "File_Count";                    //Files included with the comic. Can be used for integrity check. Also used
                                                                    // for video M3U8 download completion check.
        sHeader = sHeader + "\t" + "Comic_Online_Data_Acquired";    //Typically used to gather tag data from an online comic source, if automatic.
        sHeader = sHeader + "\t" + "Source";

        sHeader = sHeader + "\t" + "Grade";                         //Grade of the item, set by the user
        sHeader = sHeader + "\t" + "PostProcessingCode";            //Code for required post-processing.
        /*sHeader = sHeader + "\t" + "Video_Link";                    //For video download from web page or M3U8 stream. Web address of page is
                                                                    //  stored in sAddress. There can be multiple video downloads and streams
                                                                    //  per web page, hence this field.*/
        sHeader = sHeader + "\t" + "M3U8 Integrity Flag";           //
        sHeader = sHeader + "\t" + "Maturity_Rating";               //Maturity rating either defined by the user or inherited from tags.
        sHeader = sHeader + "\t" + "Approved_Users";                //Approved user(s) either defined from the importing user, explicity by the user, or inhereted from tags.
        sHeader = sHeader + "\t" + "Version:" + giCatalogFileVersion;

        return sHeader;
    }

    public static String getCatalogRecordString(ItemClass_CatalogItem ci){
        StringBuilder sbRecord = new StringBuilder();
        return getCatalogRecordString(ci, sbRecord);
    }

    public static String getCatalogRecordString(ItemClass_CatalogItem ci, StringBuilder sbRecord){

        //StringBuilder sbRecord = new StringBuilder();  //To be used when writing the catalog file.
        sbRecord.append(ci.iMediaCategory)                                          //Video, image, or comic.
                .append("\t").append(JumbleStorageText(ci.sItemID))                         //Video, image, comic id
                .append("\t").append(JumbleStorageText(ci.sGroupID))                        //Group ID to identify explict related items related much more closely than generic tags.
                .append("\t").append(JumbleStorageText(ci.iGroupSequence))                  //Sequence of item within a group
                .append("\t").append(ci.sFilename)                                          //Video or image filename. The file name is jumbled at the moment in which it was written to storage.
                .append("\t").append(JumbleStorageText(ci.sFolderRelativePath))             //Relative path of the folder holding the video, image, or comic pages, relative to the catalog folder.
                .append("\t").append(ci.sThumbnail_File)                                    //Name of the file used as the thumbnail for a video or comic
                .append("\t").append(JumbleStorageText(ci.dDatetime_Import))                //Date of import. Used for sorting if desired
                .append("\t").append(JumbleStorageText(ci.dDatetime_Last_Viewed_by_User))   //Date of last read by user. Used for sorting if desired
                .append("\t").append(JumbleStorageText(ci.sTags))                           //Tags given to the video, image, or comic
                .append("\t").append(JumbleStorageText(ci.iHeight))                         //Video or image dimension/resolution
                .append("\t").append(JumbleStorageText(ci.iWidth))                          //Video or image dimension/resolution
                .append("\t").append(JumbleStorageText(ci.lDuration_Milliseconds))          //Duration of video in milliseconds
                .append("\t").append(JumbleStorageText(ci.sDuration_Text))                  //Duration of video text in 00:00:00 format
                .append("\t").append(JumbleStorageText(ci.sResolution))                     //Resolution for sorting at user request
                .append("\t").append(JumbleStorageText(ci.lSize))                           //Size of video, image, or size of all files in the comic, in Bytes
                .append("\t").append(JumbleStorageText(ci.sCast))                           //For videos and images

        //Comic-related variables:
                .append("\t").append(JumbleStorageText(ci.sComicArtists))                   //Common comic tag category
                .append("\t").append(JumbleStorageText(ci.sComicCategories))                //Common comic tag category
                .append("\t").append(JumbleStorageText(ci.sComicCharacters))                //Common comic tag category
                .append("\t").append(JumbleStorageText(ci.sComicGroups))                    //Common comic tag category
                .append("\t").append(JumbleStorageText(ci.sComicLanguages))                 //Language(s) found in the comic
                .append("\t").append(JumbleStorageText(ci.sComicParodies))                  //Common comic tag category
                .append("\t").append(JumbleStorageText(ci.sTitle))                          //Comic name
                .append("\t").append(JumbleStorageText(ci.sComicVolume))                    //Comic "book number" or volume string
                .append("\t").append(JumbleStorageText(ci.sComicChapter))                   //Comic chapter string
                .append("\t").append(JumbleStorageText(ci.sComicChapterSubtitle))           //Comic chapter subtitle
                .append("\t").append(JumbleStorageText(ci.iComicPages))                     //Total number of pages as defined at the comic source
                .append("\t").append(JumbleStorageText(ci.iComic_Max_Page_ID))              //Max comic page id extracted from file names
                .append("\t").append(JumbleStorageText(ci.sComic_Missing_Pages))            //Missing page numbers
                .append("\t").append(JumbleStorageText(ci.iFile_Count))                     //Files included with the comic. Can be used for integrity check. Also used
        // for video M3U8 download completion check.
                .append("\t").append(JumbleStorageText(ci.bComic_Online_Data_Acquired))     //Typically used to gather tag data from an online comic source, if automatic.
                .append("\t").append(JumbleStorageText(ci.sSource))                         //Website, if relevant.
                .append("\t").append(JumbleStorageText(ci.iGrade))                          //Grade.
                .append("\t").append(JumbleStorageText(ci.iSpecialFlag))                    //Code for required post-processing.
                .append("\t").append(ci.iAllVideoSegmentFilesDetected)                      //For verifying m3u8 segment file complex integrity.

        //Append the Maturity Rating to the record:
                .append("\t").append(ci.iMaturityRating)

        //Append the list of approved users for this item to the record:
                .append("\t").append("{");
        StringBuilder sb = new StringBuilder();
        int iUserListSize = ci.alsApprovedUsers.size();
        for(int i = 0; i < iUserListSize; i++){
            sb.append(GlobalClass.JumbleStorageText(ci.alsApprovedUsers.get(i)));
            if(i < (iUserListSize - 1)){
                sb.append("%%"); //A double-percent is a symbol not allowed in a web address.
            }
        }
        sbRecord.append(sb).append("%%}");

        return sbRecord.toString();
    }


    public static ItemClass_CatalogItem ConvertStringToCatalogItem(String[] sRecord, int iCatalogFileVersion){
        //Designed for interpretting a line as read from a catalog file.
        ItemClass_CatalogItem ci =  new ItemClass_CatalogItem();
        int iFieldIndex = 0; // Allows insertion of a field in the middle of the sequence
        ci.iMediaCategory = Integer.parseInt(sRecord[iFieldIndex++]);                               //Video, image, or comic.
        ci.sItemID = JumbleStorageText(sRecord[iFieldIndex++]);                                     //Video, image, comic id
        ci.sGroupID = JumbleStorageText(sRecord[iFieldIndex++]);                                    //Group ID to identify explict related items related much more closely than generic tags.
        ci.iGroupSequence = Integer.parseInt(JumbleStorageText(sRecord[iFieldIndex++]));                               //Sequence of item within a group
        ci.sFilename = sRecord[iFieldIndex++];                                                      //Video or image filename
        ci.sFolderRelativePath = JumbleStorageText(sRecord[iFieldIndex++]);                                //Relative path of the folder holding the video, image, or comic pages, relative to the catalog folder.
        ci.sThumbnail_File = sRecord[iFieldIndex++];                                                //Name of the file used as the thumbnail for a video or comic
        ci.dDatetime_Import = Double.parseDouble(JumbleStorageText(sRecord[iFieldIndex++]));                //Date of import. Used for sorting if desired
        ci.dDatetime_Last_Viewed_by_User = Double.parseDouble(JumbleStorageText(sRecord[iFieldIndex++]));   //Date of last read by user. Used for sorting if desired
        ci.sTags = JumbleStorageText(sRecord[iFieldIndex++]);                                       //Tags given to the video, image, or comic
        ci.aliTags = getTagIDsFromTagIDString(ci.sTags);                                            //Should mirror sTags.
        ci.iHeight = Integer.parseInt(JumbleStorageText(sRecord[iFieldIndex++]));                   //Video or image dimension/resolution
        ci.iWidth = Integer.parseInt(JumbleStorageText(sRecord[iFieldIndex++]));                    //Video or image dimension/resolution
        ci.lDuration_Milliseconds = Long.parseLong(JumbleStorageText(sRecord[iFieldIndex++]));      //Duration of video in milliseconds
        ci.sDuration_Text = JumbleStorageText(sRecord[iFieldIndex++]);                              //Duration of video text in 00:00:00 format
        ci.sResolution = JumbleStorageText(sRecord[iFieldIndex++]);                                 //Resolution for sorting at user request
        ci.lSize = Long.parseLong(JumbleStorageText(sRecord[iFieldIndex++]));                       //Size of video, image, or size of all files in the comic, in Bytes
        ci.sCast = JumbleStorageText(sRecord[iFieldIndex++]);                                       //For videos and images

        //Comic-related variables:
        ci.sComicArtists = JumbleStorageText(sRecord[iFieldIndex++]);                               //Common comic tag category
        ci.sComicCategories = JumbleStorageText(sRecord[iFieldIndex++]);                            //Common comic tag category
        ci.sComicCharacters = JumbleStorageText(sRecord[iFieldIndex++]);                            //Common comic tag category
        ci.sComicGroups = JumbleStorageText(sRecord[iFieldIndex++]);                                //Common comic tag category
        ci.sComicLanguages = JumbleStorageText(sRecord[iFieldIndex++]);                             //Language(s = sRecord[0] found in the comic
        ci.sComicParodies = JumbleStorageText(sRecord[iFieldIndex++]);                              //Common comic tag category
        ci.sTitle = JumbleStorageText(sRecord[iFieldIndex++]);                                      //Comic name
        if(iCatalogFileVersion > 5) {
            ci.sComicVolume = JumbleStorageText(sRecord[iFieldIndex++]);                                //Comic "book number" or volume string
            ci.sComicChapter = JumbleStorageText(sRecord[iFieldIndex++]);                               //Comic chapter string
            ci.sComicChapterSubtitle = JumbleStorageText(sRecord[iFieldIndex++]);                       //Comic chapter subtitle
        }
        ci.iComicPages = Integer.parseInt(JumbleStorageText(sRecord[iFieldIndex++]));               //Total number of pages as defined at the comic source
        ci.iComic_Max_Page_ID = Integer.parseInt(JumbleStorageText(sRecord[iFieldIndex++]));        //Max comic page id extracted from file names
        ci.sComic_Missing_Pages = JumbleStorageText(sRecord[iFieldIndex++]);                        //Missing page numbers
        ci.iFile_Count = Integer.parseInt(JumbleStorageText(sRecord[iFieldIndex++]));               //Files included with the comic. Can be used for integrity check. Also used
                                                                                                    // for video M3U8 download completion check.
        ci.bComic_Online_Data_Acquired = Boolean.parseBoolean(JumbleStorageText(sRecord[iFieldIndex++]));  //Typically used to gather tag data from an online comic source, if automatic.
        ci.sSource = JumbleStorageText(sRecord[iFieldIndex++]);                                     //Website, if relevant. Originally for comics also used for video.
        ci.iGrade = Integer.parseInt(JumbleStorageText(sRecord[iFieldIndex++]));                    //Grade, supplied by user.
        ci.iSpecialFlag = Integer.parseInt(JumbleStorageText(sRecord[iFieldIndex++]));              //Code for required post-processing.

        ci.iAllVideoSegmentFilesDetected = Integer.parseInt(JumbleStorageText(sRecord[iFieldIndex++])); //For verifying m3u8 segment file complex integrity.

        ci.iMaturityRating = Integer.parseInt(sRecord[iFieldIndex++]);

        //Get list of approved users:
        String sApprovedUsersRaw = sRecord[iFieldIndex];
        sApprovedUsersRaw = sApprovedUsersRaw.substring(1, sApprovedUsersRaw.length() - 1); //Remove '{' and '}'.
        String[] sApprovedUsersArray = sApprovedUsersRaw.split("%%");
        for (int i = 0; i < sApprovedUsersArray.length; i++) {
            sApprovedUsersArray[i] = GlobalClass.JumbleStorageText(sApprovedUsersArray[i]);
        }
        ci.alsApprovedUsers.addAll(Arrays.asList(sApprovedUsersArray));


        return ci;
    }

    public static ItemClass_CatalogItem ConvertStringToCatalogItem(String sRecord, int iCatalogFileVersion){
        String[] sRecord2 =  sRecord.split("\t");
        return ConvertStringToCatalogItem(sRecord2, iCatalogFileVersion);
    }

    public static ItemClass_CatalogItem ConvertStringToCatalogItem(String sRecord){
        String[] sRecord2 =  sRecord.split("\t");
        return ConvertStringToCatalogItem(sRecord2, giCatalogFileVersion);
    }


    public void CatalogDataFile_CreateNewRecord(ItemClass_CatalogItem ci) throws Exception {
        ArrayList<ItemClass_CatalogItem> alci_CatalogItems = new ArrayList<>();
        alci_CatalogItems.add(ci);
        CatalogDataFile_CreateNewRecords(alci_CatalogItems);
    }

    public static final String CATALOG_CREATE_NEW_RECORDS_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_CREATE_NEW_RECORDS_ACTION_RESPONSE";

    /**
     *
     * @param iMediaCategory    Media category for the catalog file to be read/written.
     * @return                  True = No other processes are using the file as checked by
     *                                 GlobalClass.gAB_CatalogFileAvailable.
     *                          False = The file is not available after waiting for a set time
     *                                  duration for the file to become available.
     */
    public boolean IsCatalogFileAvailability(int iMediaCategory){
        //Checks to see if a particular catalog file is available, and if not, wait for it to become available.
        //  If the file does not become available within a time limit, return an indication that the
        //  file is not available.
        int iMaxWaitTimeMinutes = 5;
        int iMaxWaitTimeSeconds = iMaxWaitTimeMinutes * 60;
        long lMaxWaitTimeMS = iMaxWaitTimeSeconds * 1000;
        long lCummulativeWaitTimeMS = 0;
        int iWaitTimeInvervalMS = 250;
        while(!GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].get() && lCummulativeWaitTimeMS < lMaxWaitTimeMS){
            try {
                Thread.sleep(iWaitTimeInvervalMS);
                lCummulativeWaitTimeMS += iWaitTimeInvervalMS;
            } catch (Exception ignored){
                return false;
            }
        }
        if(lCummulativeWaitTimeMS >= lMaxWaitTimeMS){
            return false;
        }
        return true;
    }

    public static boolean IsAppLogFileAvailabile(){
        //Checks to see if the application log file is available, and if not, wait for it to become available.
        //  If the file does not become available within a time limit, return an indication that the
        //  file is not available.
        int iMaxWaitTimeMinutes = 5;
        int iMaxWaitTimeSeconds = iMaxWaitTimeMinutes * 60;
        long lMaxWaitTimeMS = iMaxWaitTimeSeconds * 1000;
        long lCummulativeWaitTimeMS = 0;
        int iWaitTimeInvervalMS = 250;
        while(!GlobalClass.gAB_ApplicationLogFileAvailable.get() && lCummulativeWaitTimeMS < lMaxWaitTimeMS){
            try {
                Thread.sleep(iWaitTimeInvervalMS);
                lCummulativeWaitTimeMS += iWaitTimeInvervalMS;
            } catch (Exception ignored){
                return false;
            }
        }
        if(lCummulativeWaitTimeMS >= lMaxWaitTimeMS){
            return false;
        }
        return true;
    }

    public void CatalogDataFile_CreateNewRecords(ArrayList<ItemClass_CatalogItem> alci_CatalogItems) {

        String sMessage;

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

        //Wait for the catalog file to become available:
        if(!IsCatalogFileAvailability(iMediaCategory)){
            return;
        }
        GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(false);

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
                sMessage = "Problem updating CatalogContents.dat.\n" + gUriCatalogContentsFiles[iMediaCategory];

                BroadcastProgress(true, sMessage,
                        false, 0,
                        false, "",
                        CATALOG_CREATE_NEW_RECORDS_ACTION_RESPONSE);

                //Set the catalog file to "available":
                GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(true);
                return;
            }
            //Write the data to the file:
            osNewCatalogContentsFile.write(sbNewCatalogRecords.toString().getBytes());
            osNewCatalogContentsFile.flush();
            osNewCatalogContentsFile.close();

        } catch (Exception e) {
            sMessage = "Problem updating CatalogContents.dat.\n" + gUriCatalogContentsFiles[iMediaCategory] + "\n\n" + e.getMessage();

            BroadcastProgress(true, sMessage,
                    false, 0,
                    false, "",
                    CATALOG_CREATE_NEW_RECORDS_ACTION_RESPONSE);

            //Set the catalog file to "available":
            GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(true);
            return;
        }

        //Set the catalog file to "available":
        GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(true);

        //Update the tags histogram:
        updateTagHistogramsIfRequired();

    }

    public void CatalogDataFile_UpdateRecord(ItemClass_CatalogItem ci) {
        ArrayList<ItemClass_CatalogItem> alci_CatalogItems = new ArrayList<>();
        alci_CatalogItems.add(ci);
        CatalogDataFile_UpdateRecords(alci_CatalogItems);
    }


    public String CatalogDataFile_UpdateRecords(ArrayList<ItemClass_CatalogItem> alci_CatalogItems) {
        //todo: get rid of this routine in favor of CatalogDataFile_UpdateCatalogFile

        String sMessage;

        int iMediaCategory;

        if(alci_CatalogItems != null){
            if(alci_CatalogItems.size() > 0){
                int iTempMediaCategory = -1;
                for(ItemClass_CatalogItem icci: alci_CatalogItems){
                    if(iTempMediaCategory == -1){
                        iTempMediaCategory = icci.iMediaCategory; //All items should have the same media category.
                    } else if (icci.iMediaCategory != iTempMediaCategory){
                        return "Records for update do not have the same media category.";
                    }
                }
                iMediaCategory = iTempMediaCategory;
            } else {
                return "No records passed for update.";
            }
        } else {
            return "No records passed for update.";
        }

        //Wait for the catalog file to become available:
        if(!IsCatalogFileAvailability(iMediaCategory)){
            return "Catalog file is being used by another process.";
        }
        GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(false);

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
                ciFromFile = ConvertStringToCatalogItem(sLine, giCatalogFileVersion);

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

            osNewCatalogContentsFile = gcrContentResolver.openOutputStream(gUriCatalogContentsFiles[iMediaCategory], "wt"); //Mode wt = write and truncate. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
            if(osNewCatalogContentsFile == null){
                //Set the catalog file to "available":
                GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(true);
                return "Issue with openning output stream to catalog file.";
            }
            //Write the data to the file:
            osNewCatalogContentsFile.write(sbBuffer.toString().getBytes());
            osNewCatalogContentsFile.flush();
            osNewCatalogContentsFile.close();

            //Update the tags histogram if required:
            updateTagHistogramsIfRequired();
            if(isCatalogReader != null) {
                isCatalogReader.close();
            }
        } catch (Exception e) {
            sMessage = "Problem updating CatalogContents.dat.\n" + gUriCatalogContentsFiles[iMediaCategory] + "\n\n" + e.getMessage();
            Toast.makeText(this, sMessage, Toast.LENGTH_LONG).show();
            //Set the catalog file to "available":
            GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(true);
            return sMessage;
        }
        GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(true);
        return "";
    }

    public boolean deleteItemFromCatalogFile(ItemClass_CatalogItem ci, String sIntentActionFilter){
        boolean bSuccess = true;

        gbTagHistogramRequiresUpdate[ci.iMediaCategory] = true;

        String sMessage;

        try {


            //Update memory to no longer include the item:
            gtmCatalogLists.get(ci.iMediaCategory).remove(ci.sItemID);

            //Update the catalog file from memory:
            CatalogDataFile_UpdateCatalogFile(ci.iMediaCategory, "Deleting item from catalog database file...");

            //Update the tags histogram:
            updateTagHistogramsIfRequired();

        } catch (Exception e) {
            sMessage = "Problem updating CatalogContents.dat.\n" + e.getMessage();
            problemNotificationConfig(sMessage, sIntentActionFilter);
            bSuccess = false;
        }
        return bSuccess;
    }

    public String CatalogDataFile_UpdateCatalogFiles(String sSpecialProgressMessage){
        //If calling this routine to add a new field:
        //  Update getCatalogRecordString before calling this routine.
        //  Update ConvertStringToCatalogItem after calling this routine.
        String sResult = "";
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){
            sResult = CatalogDataFile_UpdateCatalogFile(iMediaCategory, sSpecialProgressMessage);
        }
        return sResult;
    }

    public static final String BROADCAST_WRITE_CATALOG_FILE = "com.agcurations.aggallerymanager.intent.action.WRITE_CATALOG_FILE";
    public String CatalogDataFile_UpdateCatalogFile(int iMediaCategory, String sSpecialProgressMessage){
        //If calling this routine to add a new field:
        //  Update getCatalogRecordString before calling this routine.
        //  Update ConvertStringToCatalogItem after calling this routine.
        //  Update the validateCatalogItemData routine to ensure no illegal characters appear in the data file.

        String sProgressMessage = "Writing " + gsCatalogFolderNames[iMediaCategory] + " catalog file...";
        if(!sSpecialProgressMessage.equals("")){
            sProgressMessage = sSpecialProgressMessage;
        }

        String sMessage;
        int iProgressNumerator = 0;
        int iProgressDenominator = gtmCatalogLists.get(iMediaCategory).size();
        int iProgressBarValue;

        StringBuilder sbBuffer = new StringBuilder();
        StringBuilder sbRecord = new StringBuilder(); //This is used in an attempt to increase the speed
                                                        // at which a catalog's records are turned into a text string.
                                                        // There may be other opportunities to increase the file-write speed.

        sbBuffer.append(getCatalogHeader()); //Append the header.
        sbBuffer.append("\n");

        for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: gtmCatalogLists.get(iMediaCategory).entrySet()){

            sbBuffer.append(getCatalogRecordString(tmEntry.getValue(), sbRecord)); //Append the data.
            sbRecord.setLength(0);
            sbBuffer.append("\n");

            iProgressNumerator++;
            if (iProgressNumerator % 100 == 0) {
                iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                BroadcastProgress(false, "",
                        true, iProgressBarValue,
                        true, sProgressMessage,
                        BROADCAST_WRITE_CATALOG_FILE);
            }
        }

        //Wait for the catalog file to become available:
        if(!IsCatalogFileAvailability(iMediaCategory)){
            return "Catalog file is being used by another process.";
        }
        GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(false);

        try {
            //Write the catalog file:
            OutputStream osNewCatalogContentsFile = gcrContentResolver.openOutputStream(gUriCatalogContentsFiles[iMediaCategory], "wt"); //Mode w = write. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
            if(osNewCatalogContentsFile == null){
                throw new Exception();
            }
            osNewCatalogContentsFile.write(sbBuffer.toString().getBytes());
            osNewCatalogContentsFile.flush();
            osNewCatalogContentsFile.close();

        } catch (Exception e) {
            sMessage = "" + e.getMessage();
            //Set the catalog file to "available":
            GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(true);
            return sMessage;
        }

        //Issue 100% as the final message:
        BroadcastProgress(false, "",
                true, 100,
                false, sProgressMessage,
                BROADCAST_WRITE_CATALOG_FILE);

        GlobalClass.gAB_CatalogFileAvailable[iMediaCategory].set(true);
        return "";
    }


    /**
     *
     * @param icci      ItemClass_CatalogItem with String content to be validated to not include
     *                  characters such as 'newline' which would corrupt the data file.
     * @return          Returns the same contents as the pass-in ItemClass_CatalogItem. If any
     *                  Strings were found to contain an illegal character, that illegal character
     *                  will have been removed and a flag and message text set in the
     *                  ItemClass_CatalogItem fields 'bIllegalDataFound' & 'sIllegalDataNarrative'.
     *                  Will never return null.
     */
    static public ItemClass_CatalogItem validateCatalogItemData(@NonNull ItemClass_CatalogItem icci){

        //There are some characters and or character sequences that are not allowed in the
        //  catalog file.
        //  -Double percent symbol. This is used to indicate start of text.
        //  -Newline character.
        //  -Carriage return character.

        //Check all string fields to ensure that there are no illegal strings.
        boolean bIllegalString;

        //Note: It would be nice to create an array of objects representing the Strings so that
        //  an array can be formed and the array can be processed in such a way that a modification
        //  can be done on the icci memory location, but this is not possible. Any changes to those
        //  strings do not occur on the memory locations in icci. It is possible to change the
        //  variable type in ItemClass_CatalogItem, but that would require sweeping modifications
        //  across the application and lots of testing to ensure that unintended changes do not
        //  occur.

        /*Object[][] oExaminationItems = {
                {"", ((Object) icci.sTags					)},
                {"", ((Object) icci.sFilename				)},
                {"", ((Object) icci.sFolderRelativePath		)},
                {"", ((Object) icci.sCast					)},
                {"", ((Object) icci.sDuration_Text			)},
                {"", ((Object) icci.sResolution				)},
                {"", ((Object) icci.sThumbnail_File			)},
                {"", ((Object) icci.sComicArtists			)},
                {"", ((Object) icci.sComicCategories		)},
                {"", ((Object) icci.sComicCharacters		)},
                {"", ((Object) icci.sComicGroups			)},
                {"", ((Object) icci.sComicLanguages			)},
                {"", ((Object) icci.sComicParodies			)},
                {"", ((Object) icci.sTitle					)},
                {"", ((Object) icci.sComic_Missing_Pages	)},
                {"", ((Object) icci.sSource					)},
                {"", ((Object) icci.sVideoLink				)}};*/

        String[][] sFieldsAndData = {
                {"Tags"						,icci.sTags						},
                {"Filename"					,icci.sFilename					},
                {"FolderRelativePath"		,icci.sFolderRelativePath		},
                {"Cast"						,icci.sCast						},
                {"Duration_Text"			,icci.sDuration_Text			},
                {"Resolution"				,icci.sResolution				},
                {"Thumbnail_File"			,icci.sThumbnail_File			},
                {"ComicArtists"				,icci.sComicArtists				},
                {"ComicCategories"			,icci.sComicCategories			},
                {"ComicCharacters"			,icci.sComicCharacters			},
                {"ComicGroups"				,icci.sComicGroups				},
                {"ComicLanguages"			,icci.sComicLanguages			},
                {"ComicParodies"			,icci.sComicParodies			},
                {"Title"					,icci.sTitle					},
                {"Comic_Missing_Pages"		,icci.sComic_Missing_Pages		},
                {"Source"					,icci.sSource					},
                {"VideoLink"				,icci.sVideoLink				}};

        StringBuilder sbDataIssueNarrative = new StringBuilder();

        for(String[] sIllegalStringSet: gsIllegalRecordStrings) {
            for(int i = 0; i < sFieldsAndData.length; i++){
                bIllegalString = sFieldsAndData[i][CHECKABLE].contains(sIllegalStringSet[CHECKABLE]);
                if(bIllegalString) {
                    icci.bIllegalDataFound = true;
                    sbDataIssueNarrative.append("Illegal string sequence ").append(sIllegalStringSet[PRINTABLE]).append(" found in ").append(sFieldsAndData[i][PRINTABLE]).append(" field.\n");
                    sbDataIssueNarrative.append("Original data: ").append(sFieldsAndData[i][CHECKABLE]).append("\n");
                    sFieldsAndData[i][CHECKABLE] = sFieldsAndData[i][CHECKABLE].replace(sIllegalStringSet[CHECKABLE],"");
                    sbDataIssueNarrative.append("Modified data: ").append(sFieldsAndData[i][CHECKABLE]).append("\n\n");
                    switch (i){
                        case 0: icci.sTags                   = sFieldsAndData[i][1]; break;
                        case 1: icci.sFilename               = sFieldsAndData[i][1]; break;
                        case 2: icci.sFolderRelativePath     = sFieldsAndData[i][1]; break;
                        case 3: icci.sCast                   = sFieldsAndData[i][1]; break;
                        case 4: icci.sDuration_Text          = sFieldsAndData[i][1]; break;
                        case 5: icci.sResolution             = sFieldsAndData[i][1]; break;
                        case 6: icci.sThumbnail_File         = sFieldsAndData[i][1]; break;
                        case 7: icci.sComicArtists           = sFieldsAndData[i][1]; break;
                        case 8: icci.sComicCategories        = sFieldsAndData[i][1]; break;
                        case 9: icci.sComicCharacters        = sFieldsAndData[i][1]; break;
                        case 10: icci.sComicGroups           = sFieldsAndData[i][1]; break;
                        case 11: icci.sComicLanguages        = sFieldsAndData[i][1]; break;
                        case 12: icci.sComicParodies         = sFieldsAndData[i][1]; break;
                        case 13: icci.sTitle                 = sFieldsAndData[i][1]; break;
                        case 14: icci.sComic_Missing_Pages   = sFieldsAndData[i][1]; break;
                        case 15: icci.sSource                = sFieldsAndData[i][1]; break;
                        case 16: icci.sVideoLink             = sFieldsAndData[i][1]; break;
                    }
                }
            }
        }

        if(icci.bIllegalDataFound){
            icci.sIllegalDataNarrative = sbDataIssueNarrative.toString();
        }

        return icci;
    }

    static public ItemClass_File validateFileItemData(ItemClass_File icf){

        //There are some characters and or character sequences that are not allowed in the
        //  data file. Introductions of these characters must be elimnated at the source to allow
        //  user to be informed of data modification, etc.

        //Check all string fields to ensure that there are no illegal strings.
        boolean bIllegalString;

        String[][] sFieldsAndData = {
                {"Extension"		        ,icf.sExtension		        },
                {"Width"					,icf.sWidth					},
                {"Height"			        ,icf.sHeight			    },
                {"Uri"				        ,icf.sUri				    },
                {"MimeType"			        ,icf.sMimeType			    },
                {"DestinationFolder"		,icf.sDestinationFolder		},
                {"VideoTimeText"			,icf.sVideoTimeText			},
                {"UriParent"			    ,icf.sUriParent			    },
                {"UriThumbnailFile"		    ,icf.sUriThumbnailFile		},
                {"URL"			            ,icf.sURL			        },
                {"PageCount"			    ,icf.sPageCount			    },
                {"ComicArtists"			    ,icf.sComicArtists			},
                {"ComicParodies"		    ,icf.sComicParodies		    },
                {"ComicCategories"			,icf.sComicCategories		},
                {"ComicCharacters"			,icf.sComicCharacters		},
                {"ComicGroups"				,icf.sComicGroups		    },
                {"ComicLanguages"			,icf.sComicLanguages		},
                {"URLVideoLink"			    ,icf.sURLVideoLink			},
                {"URLThumbnail"		        ,icf.sURLThumbnail			},
                {"GroupID"				    ,icf.sGroupID				}};

        StringBuilder sbDataIssueNarrative = new StringBuilder();

        for(String[] sIllegalStringSet: gsIllegalRecordStrings) {
            for(int i = 0; i < sFieldsAndData.length; i++){
                bIllegalString = sFieldsAndData[i][CHECKABLE].contains(sIllegalStringSet[CHECKABLE]);
                if(bIllegalString) {
                    icf.bIllegalDataFound = true;
                    sbDataIssueNarrative.append("Illegal string sequence ").append(sIllegalStringSet[PRINTABLE]).append(" found in ").append(sFieldsAndData[i][PRINTABLE]).append(" field.\n");
                    sbDataIssueNarrative.append("Original data: ").append(sFieldsAndData[i][CHECKABLE]).append("\n");
                    sFieldsAndData[i][CHECKABLE] = sFieldsAndData[i][CHECKABLE].replace(sIllegalStringSet[CHECKABLE],"");
                    sbDataIssueNarrative.append("Modified data: ").append(sFieldsAndData[i][CHECKABLE]).append("\n\n");
                    switch (i){
                        case 0:  icf.sExtension				= sFieldsAndData[i][1]; break;
                        case 1:  icf.sWidth					= sFieldsAndData[i][1]; break;
                        case 2:  icf.sHeight				= sFieldsAndData[i][1]; break;
                        case 3:  icf.sUri					= sFieldsAndData[i][1]; break;
                        case 4:  icf.sMimeType				= sFieldsAndData[i][1]; break;
                        case 5:  icf.sDestinationFolder		= sFieldsAndData[i][1]; break;
                        case 6:  icf.sVideoTimeText			= sFieldsAndData[i][1]; break;
                        case 7:  icf.sUriParent				= sFieldsAndData[i][1]; break;
                        case 8:  icf.sUriThumbnailFile		= sFieldsAndData[i][1]; break;
                        case 9:  icf.sURL					= sFieldsAndData[i][1]; break;
                        case 10: icf.sPageCount				= sFieldsAndData[i][1]; break;
                        case 11: icf.sComicArtists			= sFieldsAndData[i][1]; break;
                        case 12: icf.sComicParodies			= sFieldsAndData[i][1]; break;
                        case 13: icf.sComicCategories		= sFieldsAndData[i][1]; break;
                        case 14: icf.sComicCharacters		= sFieldsAndData[i][1]; break;
                        case 15: icf.sComicGroups			= sFieldsAndData[i][1]; break;
                        case 16: icf.sComicLanguages		= sFieldsAndData[i][1]; break;
                        case 17: icf.sURLVideoLink			= sFieldsAndData[i][1]; break;
                        case 18: icf.sURLThumbnail			= sFieldsAndData[i][1]; break;
                        case 19: icf.sGroupID				= sFieldsAndData[i][1]; break;
                    }
                }
            }
        }

        //Verify user names are acceptable.
        for (String[] sIllegalStringSet : gsIllegalRecordStrings) {
            for (int i = 0; i < icf.alsUnidentifiedTags.size(); i++) {
                String sUnidentifiedTag = icf.alsUnidentifiedTags.get(i);
                bIllegalString = sUnidentifiedTag.contains(sIllegalStringSet[CHECKABLE]);
                if (bIllegalString) {
                    //If there is a tag containing an illegal character, correct it.
                    icf.bIllegalDataFound = true;
                    sbDataIssueNarrative.append("Illegal string sequence ").append(sIllegalStringSet[PRINTABLE]).append(" found in unidentified tag list:\n");
                    sbDataIssueNarrative.append("Original data: ").append(sUnidentifiedTag).append("\n");
                    icf.alsUnidentifiedTags.set(i,sUnidentifiedTag.replace(sIllegalStringSet[CHECKABLE], ""));
                    sbDataIssueNarrative.append("Modified data: ").append(icf.alsUnidentifiedTags.get(i)).append("\n\n");
                    break;
                }
            }
        }

        if(icf.bIllegalDataFound){
            icf.sIllegalDataNarrative = sbDataIssueNarrative.toString();
        }

        return icf;
    }

    /**
     * Removes a record of a file item stored in this program media folder from the index
     * used by the CatalogAnalysis worker. This is only part of CatalogAnalysis and does
     * not affect storage.
     * @param iMediaCategory - the media category of the affected index
     * @param sUri - a String of the Uri to the file listing to be removed from the index
     */
    public static boolean RemoveItemFromAnalysisIndex(int iMediaCategory, String sUri){
        //Remove the item from the CatalogAnalysis index if in use:
        //Update file indexing:
        //This is used in catalog analysis.
        // The treemap variable "gtmicf_AllFileItemsInMediaFolder" is an array list of 3 treemaps. Each
        // treemap key consists of
        // icf.sMediaFolderRelativePath + GlobalClass.gsFileSeparator + icf.sFileOrFolderName.
        // icf.sMediaFolderRelativePath excludes the media type folder. Media type folder is identified by the media type array index related to the TreeMap index.
        // Only update if the size of the tree is > 0, as it must be initiated by the Catalog Analysis worker.

        if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).size() > 0){
            String sKey = GetRelativePathFromUriString(sUri, gUriDataFolder.toString());
            if(!sKey.equals("")) {
                if (GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).containsKey(sKey)) {
                    GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).remove(sKey);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a path to a file or folder relative to a media storage folder associated with this program.
     * Relative path = subfolder + GlobalClass.gsFileSeparator + filename.
     * It does not include the media class folder, such as "Videos", "Images", or "Comics".
     * @param sUri - A string representing a Uri path.
     * @param sUriDataFolder - A string representing the base of the Uri to be removed.
     * @return - Returns the relative path, such as "1013/2e5d2p4/index.m3u8" or "1013/046256.mbew"
     */
    public static String GetRelativePathFromUriString(String sUri, String sUriDataFolder){
        String sBase = sUri;
        //sBase = cleanHTMLCodedCharacters(sBase);
        //sUriDataFolder = cleanHTMLCodedCharacters(sUriDataFolder);
        String sRelativePath = "";
        if(sBase.contains(sUriDataFolder)){
            sRelativePath = sBase.substring(sUriDataFolder.length());
        }
        //There should be two encoded slashes at the front of sRelativePath.
        int iSlashCharsCount = gsFileSeparator.length();
        if(sRelativePath.contains(gsCatalogFolderNames[MEDIA_CATEGORY_VIDEOS])){
            sRelativePath = sRelativePath.substring(gsCatalogFolderNames[MEDIA_CATEGORY_VIDEOS].length() + 2 * iSlashCharsCount); //Plus 2 to get rid of slashes.
        } else if(sRelativePath.contains(gsCatalogFolderNames[MEDIA_CATEGORY_IMAGES])){
            sRelativePath = sRelativePath.substring(gsCatalogFolderNames[MEDIA_CATEGORY_IMAGES].length() + 2 * iSlashCharsCount);
        } else if(sRelativePath.contains(gsCatalogFolderNames[MEDIA_CATEGORY_COMICS])){
            sRelativePath = sRelativePath.substring(gsCatalogFolderNames[MEDIA_CATEGORY_COMICS].length() + 2 * iSlashCharsCount);
        }

        return sRelativePath;
    }




    //Catalog backup handled in Service_Main.

    public ItemClass_CatalogItem analyzeComicReportMissingPages(ItemClass_CatalogItem ci){

        String sFolderName = ci.sFolderRelativePath;
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

    public static String getNewCatalogRecordID(){
        //Generate a randomUUID hash for a record ID. 50% possibility of a collision if every person
        //  on earth owned 600 million UUIDs.
        //This is not done in a sequence as users may wish to combine their catalogs or share data.

        //Catalog record ID must be unique and compact as it is in some cases used as the file or folder name for an item.
        //  This is due to simple accomodation for future ability to merge catalogs with catalogs held on
        //  other devices.
        //todo: Work catalog merge system to obviate need for UUID-generated file/folder names.

        String sUUID = UUID.randomUUID().toString(); //36 chars. Get random UUID, returns hex string, includes dashes.

        //Compact the string of hex characters into a string of Base32 that includes filename-safe characters only (both Windows and Linux environments):
        String sUUID_noDash = sUUID.replace("-", ""); //Get rid of dashes. 36 chars -> 32 chars

        String sUUID_noDashUpper = sUUID_noDash.toUpperCase();          //Take to uppercase for conversion from representation of hex nibbles in chars to compacted chars.
        // 0000 0000    =   a byte (8 bits) is a char represented by 2 nibbles (4 bits each).
        // Each nibble is represented by a Hex character in the sUUID string, but the string does not use nibbles,
        // a string uses chars. So each hex character is represented by 8 bits in the string.
        // One nibble is wasted for each char representation in the string. There is opportunity to
        // compact by converting to base 32, with file-safe characters.
        byte[] bytes = BaseEncoding.base16().decode(sUUID_noDashUpper); //Convert the hex nibbles to chars (bytes).
        //String sTest = BaseEncoding.base16().encode(bytes);             //Verify capability to convert from chars back to the hex nibbles.
        //String sBase32UUIDEncoded = Base32.encodeOriginal(bytes);           //Encode the chars to base32.
        //byte[] bytes_decoded = Base32.decode(sBase32UUIDEncoded);           //Test return from base32 to chars.
        //String sUUID_noDashUpper_returned = BaseEncoding.base16().encode(bytes_decoded); //Verify capability to convert chars back to hex nibbles.
        //int iLenOrig = sUUID.length(); //36 chars
        //int iLenEncoded = sBase32UUIDEncoded.length(); //26 chars

        return Base32.encodeOriginal(bytes); //26 chars
    }

    public static String getNewGroupID(){
        //Generate a randomUUID hash for a record ID. 50% possibility of a collision if every person
        //  on earth owned 600 million UUIDs.
        //This is not done in a sequence as users may wish to combine their catalogs or share data.

        return UUID.randomUUID().toString(); //36 chars. Get random UUID, returns hex string, includes dashes.
    }


    //=====================================================================================
    //===== Tag Subroutines Section ===================================================
    //=====================================================================================
    public static final String EXTRA_TAG_TO_BE_DELETED = "com.agcurations.aggallerymanager.extra.TAG_TO_BE_DELETED";
    public static final String EXTRA_MEDIA_CATEGORY = "com.agcurations.aggallerymanager.extra.MEDIA_CATEGORY";
    public static final String EXTRA_MEDIA_CATEGORY_BIT_SET = "com.agcurations.aggallerymanager.extra.MEDIA_CATEGORY_BIT_SET";
    public static final String EXTRA_TAG_DELETE_COMPLETE = "com.agcurations.aggallerymanager.extra.TAG_DELETE_COMPLETE";
    public static final String EXTRA_ARRAYLIST_STRING_TAGS_TO_ADD = "com.agcurations.aggallerymanager.extra.TAGS_TO_ADD";
    public static final String EXTRA_ARRAYLIST_ITEMCLASSTAGS_ADDED_TAGS = "com.agcurations.aggallerymanager.extra.ADDED_TAGS";

    public static final int giTagFileVersion = 1;
    public static String getTagFileHeader(){
        return
                "TagID" +
                "\t" + "TagText" +
                "\t" + "TagDescription" +
                "\t" + "TagMaturityRating" +
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

    public static int getNewTagID(int iMediaCategory){
        int iNextRecordId = -1; //Don't try to use UUID to create tag IDs. Some catalog items can
        // have a couple of dozen tags, and the UUIDs are long: 1026d7dc93aa-44c8-bed4-6b48-a9e8dbb9
        // 36 characters in the above example. At the time of this writing, a typical catalog record
        // is 380 characters with 3 tags. File size increase would be about 20% or more.
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


        return iNextRecordId;
    }

    public static ArrayList<Integer> ImportNewTags(ArrayList<String> alsNewTagNames, int iMediaCategory){

        //Any searches related to avoiding tag text/maturity/user combinations that are already in
        // the tag DB files must have already been completed.

        int iNextRecordId = getNewTagID(iMediaCategory);

        //Prepare tag items and records:
        ArrayList<ItemClass_Tag> alict_NewTags = new ArrayList<>();
        ArrayList<String> als_NewTagRecords = new ArrayList<>();
        ArrayList<Integer> als_NewTagIDs = new ArrayList<>();

        for(String sNewTagName: alsNewTagNames) {
            ItemClass_Tag ictNewTag;
            ictNewTag = new ItemClass_Tag(iNextRecordId, sNewTagName);
            iNextRecordId++;

            //Use the user's maturity level:
            ictNewTag.iMaturityRating = gicuCurrentUser.iMaturityLevel;

            //Restrict the tag to only this user:
            ictNewTag.alsTagApprovedUsers = new ArrayList<>();
            ictNewTag.alsTagApprovedUsers.add(gicuCurrentUser.sUserName);

            //Add the tag item to the arraylist:
            alict_NewTags.add(ictNewTag);

            //Get a tag record to write to the catalog file and add it to the arraylist:
            String sTagRecord = getTagRecordString(ictNewTag);
            als_NewTagRecords.add(sTagRecord);
        }

        //Get the tags file:
        Uri uriTagsFile = gUriCatalogTagsFiles[iMediaCategory];
        try {
            //Open the tags file write-mode append.
            OutputStream osNewTagsFile = gcrContentResolver.openOutputStream(uriTagsFile, "wa"); //Open file in write-append mode.
            if(osNewTagsFile == null){
                return null;
            }
            StringBuilder sbNewTagsStringBuilder = new StringBuilder();

            //Add the new record to the catalog file:
            for(String sLine: als_NewTagRecords) {
                sbNewTagsStringBuilder.append(sLine);
                sbNewTagsStringBuilder.append("\n");
            }

            osNewTagsFile.write(sbNewTagsStringBuilder.toString().getBytes(StandardCharsets.UTF_8));
            osNewTagsFile.flush();
            osNewTagsFile.close();

        } catch (Exception e) {
            return null;
        }

        //If the database write was successful, add the tag items to working memory:
        for(ItemClass_Tag ictNewTag: alict_NewTags) {
            gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).put(ictNewTag.iTagID, ictNewTag);
            gtmCatalogTagReferenceLists.get(iMediaCategory).put(ictNewTag.iTagID, ictNewTag);
            als_NewTagIDs.add(ictNewTag.iTagID);
        }

        return als_NewTagIDs;

    }

    public ArrayList<ItemClass_Tag> TagDataFile_CreateNewRecords(ArrayList<String> sNewTagNames, int iMediaCategory){

        //Get the tags file:

        Uri uriTagsFile = gUriCatalogTagsFiles[iMediaCategory];

        int iNextRecordId = getNewTagID(iMediaCategory);

        //Create an ArrayList to store the new tags:
        ArrayList<ItemClass_Tag> ictNewTags = new ArrayList<>();
        ItemClass_Tag ictNewTag;

        try {
            //Open the tags file write-mode append:

            OutputStream osNewTagsFile = gcrContentResolver.openOutputStream(uriTagsFile, "wa"); //Write-mode append.
            if(osNewTagsFile == null){
                return null;
            }
            StringBuilder sbNewTagsStringBuilder = new StringBuilder();

            for(String sNewTagName: sNewTagNames) {

                ictNewTag = new ItemClass_Tag(iNextRecordId, sNewTagName);
                gtmCatalogTagReferenceLists.get(iMediaCategory).put(iNextRecordId, ictNewTag);
                boolean bTagApprovedForCurrentUser = true;
                if(ictNewTag.alsTagApprovedUsers.size() > 0){
                    if(!ictNewTag.alsTagApprovedUsers.contains(gicuCurrentUser.sUserName)){
                        bTagApprovedForCurrentUser = false;
                    }
                }
                if(bTagApprovedForCurrentUser) {
                    gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).put(iNextRecordId, ictNewTag);
                    //Prep for return of new tag items to the caller:
                    ictNewTags.add(ictNewTag);
                }

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

        int iNextRecordId = getNewTagID(iMediaCategory);

        //Get the tags file:
        Uri uriTagsFile = gUriCatalogTagsFiles[iMediaCategory];
        try {
            //Open the tags file write-mode append.
            OutputStream osNewTagsFile = gcrContentResolver.openOutputStream(uriTagsFile, "wa"); //Open file in write-append mode.
            if(osNewTagsFile == null){
                return null;
            }
            StringBuilder sbNewTagsStringBuilder = new StringBuilder();

            boolean bTagAlreadyExists = false;
            if (gtmCatalogTagReferenceLists.get(iMediaCategory).size() > 0) {
                for (Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {

                    if (entry.getValue().sTagText.equalsIgnoreCase(ictNewTag.sTagText)) {
                        //If the tag already exists, abort adding this tag. //todo: unless it is a user-private tag or age-rating is set differently.
                        bTagAlreadyExists = true;
                        break;
                    }
                }
            }

            ItemClass_Tag ictNewNewTag = new ItemClass_Tag(iNextRecordId, ictNewTag.sTagText); //Tag ID in the tag item is final. Must update here with "newnew" item.
            if(!bTagAlreadyExists) {
                //Add the tag to memory
                ictNewNewTag.sTagDescription = ictNewTag.sTagDescription;
                ictNewNewTag.iMaturityRating = ictNewTag.iMaturityRating;
                ictNewNewTag.alsTagApprovedUsers = new ArrayList<>(ictNewTag.alsTagApprovedUsers);

                gtmCatalogTagReferenceLists.get(iMediaCategory).put(iNextRecordId, ictNewNewTag);

                boolean bTagApprovedForCurrentUser = true;
                if(ictNewNewTag.alsTagApprovedUsers.size() > 0){
                    if(!ictNewNewTag.alsTagApprovedUsers.contains(gicuCurrentUser.sUserName)){
                        bTagApprovedForCurrentUser = false;
                    }
                }
                if(bTagApprovedForCurrentUser) {
                    gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).put(iNextRecordId, ictNewNewTag);
                }

                //Add the new record to the catalog file:
                String sLine = getTagRecordString(ictNewNewTag);
                sbNewTagsStringBuilder.append(sLine);
                sbNewTagsStringBuilder.append("\n");
            } else {
                ictNewNewTag = null;
            }

            osNewTagsFile.write(sbNewTagsStringBuilder.toString().getBytes(StandardCharsets.UTF_8));
            osNewTagsFile.flush();
            osNewTagsFile.close();
            return ictNewNewTag;

        } catch (Exception e) {
            Toast.makeText(this, "Problem updating Tags.dat.\n" + uriTagsFile + "\n\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;

        }

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

            boolean bTagApprovedForCurrentUser = true;
            if(ict_TagToUpdate.alsTagApprovedUsers.size() > 0){
                if(!ict_TagToUpdate.alsTagApprovedUsers.contains(gicuCurrentUser.sUserName)){
                    bTagApprovedForCurrentUser = false;
                }
            }
            if(bTagApprovedForCurrentUser) {
                gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).replace(ict_TagToUpdate.iTagID, ict_TagToUpdate);
            } else {
                gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).remove(ict_TagToUpdate.iTagID);
            }

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
        for(Map.Entry<Integer, ItemClass_Tag> entry : gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            Integer iRefTag = entry.getValue().iTagID;
            if(iRefTag.equals(iTagID)){
                sTagText = entry.getValue().sTagText;
                break;
            }
        }


        return sTagText;
    }

    public static TreeMap<Integer, ItemClass_Tag> getApprovedTagsTreeMapCopy(int iMediaCategory){
        TreeMap<Integer, ItemClass_Tag> tmCopy = new TreeMap<>();
        for(Map.Entry<Integer, ItemClass_Tag> entry: GlobalClass.gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            String sTagCopy = GlobalClass.getTagRecordString(entry.getValue());
            ItemClass_Tag ictTagCopy = GlobalClass.ConvertFileLineToTagItem(sTagCopy); //This is the only way to get a true copy. Otherwise it is passed by reference.
            tmCopy.put(entry.getKey(), ictTagCopy);
        }
        return tmCopy;
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

        for(Map.Entry<Integer, ItemClass_Tag> entry : gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            Integer iRefTag = entry.getValue().iTagID;
            if(iRefTag.equals(iTagID)){
                return true;
            }
        }

        return false;
    }

    public void TagDataFileAddNewField(){
        //Execute these steps to add a new field (adding a new field) to the tags files.
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

    public static boolean WriteTagDataFile(int iMediaCategory){
        //This routine used to re-write the tag data file from reference memory.

        Uri uriTagsFile = gUriCatalogTagsFiles[iMediaCategory];

        try {
            StringBuilder sbBuffer = new StringBuilder();
            sbBuffer.append(getTagFileHeader()).append("\n");
            for(Map.Entry<Integer, ItemClass_Tag> tagEntry: gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
                sbBuffer.append(getTagRecordString(tagEntry.getValue())).append("\n");
            }

            //Write the data to the file:
            OutputStream osNewTagsFile = gcrContentResolver.openOutputStream(uriTagsFile, "wt"); //Open the tags file in write mode.
            if (osNewTagsFile == null) {
                return false;
            }
            osNewTagsFile.write(sbBuffer.toString().getBytes(StandardCharsets.UTF_8));
            osNewTagsFile.flush();
            osNewTagsFile.close();

        } catch (Exception e) {
            String sMessage =  "Problem updating Tags.dat.\n" + uriTagsFile + "\n" + e.getMessage();
            Log.d("TagDataFileAddNewField", sMessage);
        }

        return true;

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
        for(Map.Entry<Integer, ItemClass_Tag> entry: gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
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
                for(Map.Entry<Integer, ItemClass_Tag> entry: gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
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
                        if(gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).get(iCatalogItemTagID) != null) {
                            Objects.requireNonNull(gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).get(iCatalogItemTagID)).iHistogramCount++;
                        }
                    }
                }


            }
        }
    }

    /**
     *
     * @param iMediaCategory            Media category for the tags to be considered.
     * @param aliTagIDs                 Perform histogram around tags that occur alongside these identified tags. Leave empty for a basic tag histogram.
     * @param bIncludeZeroCountItems    Return all tag items, including ones that have zero occurrences alongside entries in aliTagIDs.
     * @return                          A treemap of TagID, and Tag. The Tag structure includes the histogram count.
     */
    public TreeMap<Integer, ItemClass_Tag> getXrefTagHistogram(int iMediaCategory, ArrayList<Integer> aliTagIDs, boolean bIncludeZeroCountItems){
        //Get a histogram counting the tags that occur alongside tags found in aliTagIDs.
        //  Suppose the user selects tag ID 7, and wants to know what other tag IDs are frequently
        //  found alongside tag ID 7. This routine returns that list with frequency.
        //  This is used in filtering. The user will select a tag, an xref list will be returned.
        //  Tags with an occurrence of Zero will be hidden. Therefore, the user will be able to
        //  filter further by selecting additional tags.

        TreeMap<Integer, ItemClass_Tag> tmXrefTagHistogram = new TreeMap<>();

        //Get a list of all tags not available to the current user because those tags are private to
        // another user:
        ArrayList<Integer> aliRestrictedTagIDs = new ArrayList<>();
        for (Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
            boolean bTagIsOutsideMaturityRating;
            boolean bTagIsPrivateToOtherUsers;
            if(gicuCurrentUser != null) {
                //If a user is logged-in:
                 bTagIsOutsideMaturityRating = gicuCurrentUser.iMaturityLevel < entry.getValue().iMaturityRating;
                 bTagIsPrivateToOtherUsers = entry.getValue().alsTagApprovedUsers.size() > 0;
                if(entry.getValue().alsTagApprovedUsers.size() > 0){
                    for(String sApprovedUser: entry.getValue().alsTagApprovedUsers){
                        if(sApprovedUser.equals(gicuCurrentUser.sUserName)){
                            bTagIsPrivateToOtherUsers = false;
                            break;
                        }
                    }
                }
            } else {
                //If no user is selected or current user is somehow null, follow guidelines for
                //  default user maturity rating.
                bTagIsOutsideMaturityRating = giDefaultUserMaturityRating < entry.getValue().iMaturityRating;
                bTagIsPrivateToOtherUsers = entry.getValue().alsTagApprovedUsers.size() > 0;
            }
            if (bTagIsOutsideMaturityRating ||
                    bTagIsPrivateToOtherUsers) {
                aliRestrictedTagIDs.add(entry.getValue().iTagID);
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
                            ItemClass_Tag ict = gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).get(iCatalogItemTagID);
                            if(ict != null){
                                String sTagCopy = GlobalClass.getTagRecordString(ict);
                                ItemClass_Tag ictTagCopy = GlobalClass.ConvertFileLineToTagItem(sTagCopy); //This is the only way to get a true copy. Otherwise it is passed by reference.
                                ictTagCopy.iHistogramCount = 1;
                                tmXrefTagHistogram.put(iCatalogItemTagID, ictTagCopy);
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

        if(bIncludeZeroCountItems){
            //Include tags that have not been found to have an occurence and set the histogram count for the item to zero.
            for (Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                boolean bTagIsOutsideMaturityRating;
                boolean bTagIsPrivateToOtherUsers;
                if(gicuCurrentUser != null) {
                    //If a user is logged-in:
                    bTagIsOutsideMaturityRating = gicuCurrentUser.iMaturityLevel < entry.getValue().iMaturityRating;
                    bTagIsPrivateToOtherUsers = entry.getValue().alsTagApprovedUsers.size() > 0;
                    if(entry.getValue().alsTagApprovedUsers.size() > 0){
                        for(String sApprovedUser: entry.getValue().alsTagApprovedUsers){
                            if(sApprovedUser.equals(gicuCurrentUser.sUserName)){
                                bTagIsPrivateToOtherUsers = false;
                                break;
                            }
                        }
                    }
                } else {
                    //If no user is selected or current user is somehow null, follow guidelines for
                    //  default user maturity rating.
                    bTagIsOutsideMaturityRating = giDefaultUserMaturityRating < entry.getValue().iMaturityRating;
                    bTagIsPrivateToOtherUsers = entry.getValue().alsTagApprovedUsers.size() > 0;
                }
                if (bTagIsOutsideMaturityRating ||
                        bTagIsPrivateToOtherUsers) {
                    continue;
                }
                if(!tmXrefTagHistogram.containsKey(entry.getValue().iTagID)){
                    String sTagCopy = GlobalClass.getTagRecordString(entry.getValue());
                    ItemClass_Tag ictTagCopy = GlobalClass.ConvertFileLineToTagItem(sTagCopy); //This is the only way to get a true copy. Otherwise it is passed by reference.
                    ictTagCopy.iHistogramCount = 0;
                    tmXrefTagHistogram.put(ictTagCopy.iTagID, ictTagCopy);
                }

            }

        }

        return tmXrefTagHistogram;
    }



    public static int getHighestTagMaturityRating(ArrayList<ItemClass_Tag> alict_Tags){
        int iHighestTagMaturityRating = AdapterMaturityRatings.MATURITY_RATING_EC;

        for(ItemClass_Tag ict: alict_Tags){
            if(iHighestTagMaturityRating < ict.iMaturityRating){
                iHighestTagMaturityRating = ict.iMaturityRating;
            }
        }

        return iHighestTagMaturityRating;
    }

    public static int getHighestTagMaturityRating(ArrayList<Integer> ali_TagIDs, int iMediaCategory){

        ArrayList<ItemClass_Tag> alict_Tags = new ArrayList<>();
        for(Integer iTagID: ali_TagIDs){

            ItemClass_Tag ict = gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).get(iTagID);
            if (ict != null) {
                alict_Tags.add(ict);
            }

        }

        return getHighestTagMaturityRating(alict_Tags);
    }


    public static ArrayList<String> getApprovedUsersForTagGrouping(ArrayList<Integer> aliTagIDs, int iMediaCategory){

        //If the group of tags has a single approved user, then only that user is approved for the
        // whole catalog item.
        //If the group of tags has two or more approved users, all users must be approved for
        //  each tag that is restricted to users.
        //Further, users must have a maturity rating equal-to or greater-than the highest
        //  maturity rating among the group of tags.

        //Start with a listing of all users:
        ArrayList<ItemClass_User> alicuApprovedUserPool = new ArrayList<>(galicu_Users);

        //Process each listed tag ID:
        for(Integer iTagID: aliTagIDs) {
            if(iTagID == -1) continue;//If an item has no tags, just move on without special approved users.
            ItemClass_Tag ict = gtmCatalogTagReferenceLists.get(iMediaCategory).get(iTagID);
            if(ict != null) {
                if (ict.alsTagApprovedUsers != null) {
                    if (ict.alsTagApprovedUsers.size() > 0) {
                        //Check to see if users are included:
                        ArrayList<ItemClass_User> alsApprovedUsers = new ArrayList<>();
                        for (ItemClass_User icu : alicuApprovedUserPool) {
                            boolean bUserApproved = false;
                            for (String sUserApprovedForTag : ict.alsTagApprovedUsers) {
                                if (sUserApprovedForTag.equals(icu.sUserName)) {
                                    bUserApproved = true;
                                    break;
                                }
                            }
                            if (bUserApproved) {
                                alsApprovedUsers.add(icu);
                            }
                        }
                        alicuApprovedUserPool = alsApprovedUsers;
                    }
                } else {
                    Log.d("GlobalClass:getApprovedUsersForTagGrouping()", "alsTagApprovedUsers is null");
                }
            } else {
                Log.d("GlobalClass:getApprovedUsersForTagGrouping()", "Tag item is null for tag ID " + iTagID);
            }
        }

        //Remove users not approved for the tag grouping based on maturity rating:
        int iHighestTagMaturityRating = getHighestTagMaturityRating(aliTagIDs, iMediaCategory);
        ArrayList<ItemClass_User> alicuPreApprovedUsers = new ArrayList<>(alicuApprovedUserPool);
        for(ItemClass_User icu_PreApprovedUser: alicuPreApprovedUsers){
            if(icu_PreApprovedUser.iMaturityLevel < iHighestTagMaturityRating){
                alicuApprovedUserPool.remove(icu_PreApprovedUser);
            }
        }

        ArrayList<String> alsApprovedUsers = new ArrayList<>();
        for(ItemClass_User icu: alicuApprovedUserPool){
            alsApprovedUsers.add(icu.sUserName);
        }
        return alsApprovedUsers;
    }

    public void populateApprovedTags(){
        if(!gabTagsLoaded.get()){
            return;
        }
        gtmApprovedCatalogTagReferenceLists.clear();
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++) {
            TreeMap<Integer, ItemClass_Tag> tmApprovedTags = new TreeMap<>();
            gtmApprovedCatalogTagReferenceLists.add(tmApprovedTags); //Add empty list.
            //Process all tags in this media category:
            for (Map.Entry<Integer, ItemClass_Tag> entry : gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                boolean bOkToAddTag = false;
                if(gicuCurrentUser == null){
                    //If no user has been selected, don't show tags that have been approved for users
                    //  and don't show tags above the default user's maturity rating.
                    if(entry.getValue().alsTagApprovedUsers.size() == 0 &&
                        entry.getValue().iMaturityRating <= giDefaultUserMaturityRating){
                        bOkToAddTag = true;
                    }
                } else {
                    //If a user has been selected...
                    if(entry.getValue().iMaturityRating <= gicuCurrentUser.iMaturityLevel){
                        if(entry.getValue().alsTagApprovedUsers.size() == 0){
                            bOkToAddTag = true;
                        } else {
                            for(String sUserName: entry.getValue().alsTagApprovedUsers){
                                if(sUserName.equals(gicuCurrentUser.sUserName)){
                                    bOkToAddTag = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if(bOkToAddTag) {
                    String sTagCopy = GlobalClass.getTagRecordString(entry.getValue());
                    ItemClass_Tag ictTagCopy = GlobalClass.ConvertFileLineToTagItem(sTagCopy); //This is the only way to get a true copy. Otherwise it is passed by reference.
                    gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).put(entry.getKey(), ictTagCopy);
                }
            }

            //Update the tags histogram. As of 7/29/2022, this is used to show the user
            //  how many tags are in use while they select tags to perform a tag filter.
            if(gtmCatalogLists.size() == 3) {
                //If the catalogs are loaded...
                for (Map.Entry<String, ItemClass_CatalogItem> icci : gtmCatalogLists.get(iMediaCategory).entrySet()) {
                    for (int iCatalogItemTagID : icci.getValue().aliTags) {
                        if (gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).get(iCatalogItemTagID) != null) {
                            Objects.requireNonNull(gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).get(iCatalogItemTagID)).iHistogramCount++;
                        }
                    }
                }
            }

        }



    }

    static public ItemClass_Tag validateTagData(ItemClass_Tag ict){

        //There are some characters and or character sequences that are not allowed in the
        //  tag file.
        //  -Double percent symbol. This is used to indicate start of text.
        //  -Newline character.
        //  -Carriage return character.

        //Check all string fields to ensure that there are no illegal strings.
        boolean bIllegalString = false;

        String[][] sFieldsAndData = {
                {"TagText"		    	,ict.sTagText			},
                {"TagDescription"		,ict.sTagDescription	}};

        StringBuilder sbDataIssueNarrative = new StringBuilder();

        for(String[] sIllegalStringSet: GlobalClass.gsIllegalRecordStrings) {
            for(int i = 0; i < sFieldsAndData.length; i++){
                bIllegalString = sFieldsAndData[i][GlobalClass.CHECKABLE].contains(sIllegalStringSet[GlobalClass.CHECKABLE]);
                if(bIllegalString) {
                    ict.bIllegalDataFound = true;
                    sbDataIssueNarrative.append("Illegal string sequence ").append(sIllegalStringSet[GlobalClass.PRINTABLE]).append(" found in ").append(sFieldsAndData[i][GlobalClass.PRINTABLE]).append(" field.\n");
                    sbDataIssueNarrative.append("Original data: ").append(sFieldsAndData[i][GlobalClass.CHECKABLE]).append("\n");
                    sFieldsAndData[i][GlobalClass.CHECKABLE] = sFieldsAndData[i][GlobalClass.CHECKABLE].replace(sIllegalStringSet[GlobalClass.CHECKABLE],"");
                    sbDataIssueNarrative.append("Modified data: ").append(sFieldsAndData[i][GlobalClass.CHECKABLE]).append("\n\n");
                    switch (i){
                        case 0: ict.sTagText            = sFieldsAndData[i][1]; break;
                        case 1: ict.sTagDescription     = sFieldsAndData[i][1]; break;
                    }
                }
            }
        }

        if(ict.bIllegalDataFound){
            ict.sIllegalDataNarrative = sbDataIssueNarrative.toString();
        }

        //Verify user names are acceptable.
        for (String[] sIllegalStringSet : GlobalClass.gsIllegalRecordStrings) {
            for (String sApprovedUser : ict.alsTagApprovedUsers) {
                bIllegalString = sApprovedUser.contains(sIllegalStringSet[GlobalClass.CHECKABLE]);
                if (bIllegalString) {
                    //If there is a user name containing an illegal character, null the ict as this
                    // is an unacceptable condition at any time. A corrected username would cause
                    // the item to disappear within the program as there would be no matching username.
                    ict = null;
                    break;
                }
            }
            if (bIllegalString) break;
        }

        return ict;
    }


    //==================================================================================================
    //=========  USER SUBROUTINES SECTION  ============================================================
    //==================================================================================================

    public static final String EXTRA_STRING_USERNAME = "com.agcurations.aggallerymanager.extra.string_username";

    public static String getUserAccountRecordString(ItemClass_User icu){
        String sUserRecord =
                        JumbleStorageText(icu.sUserName) + "\t" +
                        JumbleStorageText(icu.sPin) + "\t" +
                        JumbleStorageText(icu.iUserIconColor) + "\t" +
                        JumbleStorageText(icu.bAdmin) + "\t" +
                        JumbleStorageText(icu.iMaturityLevel) + "\t" +
                        JumbleStorageText(icu.bToBeDeleted);
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
        if(sRecordSplit.length == 6){
            icu.bToBeDeleted = Boolean.parseBoolean(JumbleStorageText(sRecordSplit[5]));
        }
        return icu;
    }

    public static boolean WriteUserDataFile(){

        StringBuilder sbBuffer = new StringBuilder();
        for(ItemClass_User icu: galicu_Users){
            sbBuffer.append(getUserAccountRecordString(icu)).append("\n");
        }

        try {
            //Write the data to the file:
            OutputStream osUserDataFile = gcrContentResolver.openOutputStream(gUriUserDataFile, "wt"); //Open the file in overwrite mode.
            if (osUserDataFile == null) {
                return false;
            }
            osUserDataFile.write(sbBuffer.toString().getBytes(StandardCharsets.UTF_8));
            osUserDataFile.flush();
            osUserDataFile.close();

        } catch (Exception e) {
            return false;
        }

        return true;
    }





    //==================================================================================================
    //=========  BROWSER  ==============================================================================
    //==================================================================================================

    public static final String EXTRA_WEBPAGE_TAB_DATA_TABID = "com.agcurations.webbrowser.extra.WEBPAGE_TAB_DATA_TABID";
    public static final String EXTRA_WEBPAGE_TAB_DATA_ADDRESS = "com.agcurations.webbrowser.extra.WEBPAGE_TAB_DATA_ADDRESS";
    public static final String EXTRA_WEBPAGE_TAB_DATA_TITLE = "com.agcurations.webbrowser.extra.WEBPAGE_TAB_DATA_TITLE";
    public static final String EXTRA_WEBPAGE_TAB_DATA_FAVICON_ADDRESS = "com.agcurations.webbrowser.extra.WEBPAGE_TAB_DATA_FAVICON_ADDRESS";
    public static final String EXTRA_RESULT_TYPE_WEB_PAGE_TAB_MESSAGE = "com.agcurations.webbrowser.extra.RESULT_TYPE";

    public static String gsBrowserAddressClipboard = "";

    public static String giWebViewSettings_UserAgentString;
    public static void ConfigureWebSettings(WebSettings webSettings){
        webSettings.setDomStorageEnabled(true); //Required to load all graphics on some webpages.
        if(giWebViewSettings_UserAgentString != null){
            if(!giWebViewSettings_UserAgentString.equals("")){
                //User agent can be changed to request the "desktop version" of a web page, but might
                //  throw off any server-side client authentication systems.
                webSettings.setUserAgentString(giWebViewSettings_UserAgentString);
            }
        }
    }

    public static final Queue<String> queueWebPageTabDataFileWriteRequests = new LinkedList<>();
    public static final int giMaxDelayForWriteRequestMS = 5000;

    public static final int giWebPageTabDataFileVersion = 1;
    public static String getWebPageTabDataFileHeader(){
        String sHeader = "";
        sHeader = sHeader + "ID";                       //Tab ID (unique).
        sHeader = sHeader + "\t" + "User";             //User who created the tab. Tabs are not shared between users
        sHeader = sHeader + "\t" + "Title";             //Tab title (don't reload the page to get the title).
        sHeader = sHeader + "\t" + "Address";           //Current address for the tab.
        sHeader = sHeader + "\t" + "Favicon Filename";  //Filename of bitmap for tab icon.
        sHeader = sHeader + "\t" + "BackStack";         //Address back stack (history). Top item is the current address.
        sHeader = sHeader + "\t" + "ForwardStack";      //Address forward stack (forward-history)
        sHeader = sHeader + "\t" + "Version:" + giWebPageTabDataFileVersion;

        return sHeader;
    }

    public static String ConvertWebPageTabDataToDataFileRecordString(ItemClass_WebPageTabData icwptd){

        String sRecord = "";  //To be used when writing the catalog file.

        String sTabIDClean = GlobalClass.JumbleStorageText(icwptd.sTabID);
        String sUserNameClean = GlobalClass.JumbleStorageText(icwptd.sUserName);
        String sTabTitleClean = GlobalClass.JumbleStorageText(icwptd.sTabTitle);
        String sAddressClean = GlobalClass.JumbleStorageText(icwptd.sAddress);
        String sFaviconAddressClean = GlobalClass.JumbleStorageText(icwptd.sFaviconAddress);

        //public static final String[][] gsIllegalRecordStrings = { //{Printable notification, actual illegal string/character}
        boolean bIllegalString = false;

        String[][] sFieldsAndData = {
                {"TabID"		    	,GlobalClass.JumbleStorageText(icwptd.sTabID)			},
                {"TabTitle"		    	,GlobalClass.JumbleStorageText(icwptd.sTabTitle)		},
                {"Address"		    	,GlobalClass.JumbleStorageText(icwptd.sAddress)			},
                {"FaviconAddress"		,GlobalClass.JumbleStorageText(icwptd.sFaviconAddress)	}};

        StringBuilder sbDataIssueNarrative = new StringBuilder();

        for(String[] sIllegalStringSet: GlobalClass.gsIllegalRecordStrings) {
            for(int i = 0; i < sFieldsAndData.length; i++){
                bIllegalString = sFieldsAndData[i][GlobalClass.CHECKABLE].contains(sIllegalStringSet[GlobalClass.CHECKABLE]);
                if(bIllegalString) {
                    icwptd.bIllegalDataFound = true;
                    sbDataIssueNarrative.append("Illegal string sequence ").append(sIllegalStringSet[GlobalClass.PRINTABLE]).append(" found in ").append(sFieldsAndData[i][GlobalClass.PRINTABLE]).append(" field.\n");
                    sbDataIssueNarrative.append("Original data: ").append(sFieldsAndData[i][GlobalClass.CHECKABLE]).append("\n");
                    sFieldsAndData[i][GlobalClass.CHECKABLE] = sFieldsAndData[i][GlobalClass.CHECKABLE].replace(sIllegalStringSet[GlobalClass.CHECKABLE],"");
                    sbDataIssueNarrative.append("Modified data: ").append(sFieldsAndData[i][GlobalClass.CHECKABLE]).append("\n\n");
                    switch (i){
                        case 0: sTabIDClean =           sFieldsAndData[i][1]; break;
                        case 2: sTabTitleClean =        sFieldsAndData[i][1]; break;
                        case 3: sAddressClean =         sFieldsAndData[i][1]; break;
                        case 4: sFaviconAddressClean =  sFieldsAndData[i][1]; break;
                    }
                }
            }
        }

        if(icwptd.bIllegalDataFound){
            icwptd.sIllegalDataNarrative = sbDataIssueNarrative.toString();
        }

        sRecord = sRecord + sTabIDClean;
        sRecord = sRecord + "\t" + sUserNameClean;  //No special processing for user name. Any change of that item could make an entry non-recoverable.
        sRecord = sRecord + "\t" + sTabTitleClean;
        sRecord = sRecord + "\t" + sAddressClean;
        sRecord = sRecord + "\t" + sFaviconAddressClean;

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
        String sBackStackClean = sb.toString();
        sBackStackClean = sBackStackClean.replace("\n", "");
        sBackStackClean = sBackStackClean.replace("\r", "");
        sBackStackClean = sBackStackClean.replace("\t", "");
        sRecord = sRecord + sBackStackClean + "%%" + "}";

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
        String sForwardStackClean = sb.toString();
        sForwardStackClean = sForwardStackClean.replace("\n", "");
        sForwardStackClean = sForwardStackClean.replace("\r", "");
        sForwardStackClean = sForwardStackClean.replace("\t", "");
        sRecord = sRecord + sForwardStackClean + "%%" + "}";

        return sRecord;
    }

    public static ItemClass_WebPageTabData ConvertStringToWebPageTabData(String[] sRecord){
        //Designed for interpreting a line as read from the WebPageTabData file.
        ItemClass_WebPageTabData icwptd =  new ItemClass_WebPageTabData();
        icwptd.sTabID = GlobalClass.JumbleStorageText(sRecord[0]);
        icwptd.sUserName = GlobalClass.JumbleStorageText(sRecord[1]);
        icwptd.sTabTitle = GlobalClass.JumbleStorageText(sRecord[2]);
        icwptd.sAddress = GlobalClass.JumbleStorageText(sRecord[3]);


        if(sRecord.length > 4) { //Length is 1-based
            //Favicon filename might be empty, and if it is the last item on the record,
            //  it will not be split-out via the split operation.
            icwptd.sFaviconAddress = GlobalClass.JumbleStorageText(sRecord[4]); //Array index is 0-based.
        }

        if(sRecord.length > 5) { //Length is 1-based
            //Get the back-stack:
            String sBackStackRaw = sRecord[5]; //Array index is 0-based.
            sBackStackRaw = sBackStackRaw.substring(1, sBackStackRaw.length() - 1); //Remove '{' and '}'.
            String[] sBackStackArray = sBackStackRaw.split("%%");
            for (int i = 0; i < sBackStackArray.length; i++) {
                sBackStackArray[i] = GlobalClass.JumbleStorageText(sBackStackArray[i]);
            }
            icwptd.stackBackHistory.addAll(Arrays.asList(sBackStackArray));
        }

        if(sRecord.length > 6) { //Length is 1-based
            //Get the forward-stack:
            String sForwardStackRaw = sRecord[6]; //Array index is 0-based.
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
        int iRequiredFieldCount = 7;
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

    static public ItemClass_WebPageTabData validateWebPageTabData(ItemClass_WebPageTabData icwptd){
        //This routine might not be necessary. The only code that could mess up the
        // .dat file is related to scraped data, such as a web page's title. The web addresses
        // cannot include illegal characters. Best to address issues involving the title at locations
        // where the title is acquired from the page.

        //There are some characters and or character sequences that are not allowed in the
        //  data file. Introductions of these characters must be elimnated at the source to allow
        //  user to be informed of data modification, etc.

        //Check all string fields to ensure that there are no illegal strings.
        boolean bIllegalString = false;

        String[][] sFieldsAndData = {
                {"TabTitle"		    ,icwptd.sTabTitle           }};

        StringBuilder sbDataIssueNarrative = new StringBuilder();

        for(String[] sIllegalStringSet: gsIllegalRecordStrings) {
            for(int i = 0; i < sFieldsAndData.length; i++){
                bIllegalString = sFieldsAndData[i][CHECKABLE].contains(sIllegalStringSet[CHECKABLE]);
                if(bIllegalString) {
                    icwptd.bIllegalDataFound = true;
                    sbDataIssueNarrative.append("Illegal string sequence ").append(sIllegalStringSet[PRINTABLE]).append(" found in ").append(sFieldsAndData[i][PRINTABLE]).append(" field.\n");
                    sbDataIssueNarrative.append("Original data: ").append(sFieldsAndData[i][CHECKABLE]).append("\n");
                    sFieldsAndData[i][CHECKABLE] = sFieldsAndData[i][CHECKABLE].replace(sIllegalStringSet[CHECKABLE],"");
                    sbDataIssueNarrative.append("Modified data: ").append(sFieldsAndData[i][CHECKABLE]).append("\n\n");
                    switch (i){
                        case 0:  icwptd.sTabTitle				= sFieldsAndData[i][1]; break;
                    }
                }
            }
        }

        //Don't worry about checking the back and forward history addresses. They should already be checked for illegal characters/sequences.

        if(icwptd.bIllegalDataFound){
            icwptd.sIllegalDataNarrative = sbDataIssueNarrative.toString();
        }

        return icwptd;
    }

    //==================================================================================================
    //=========  BROWSER OPTIONS  ======================================================================
    //==================================================================================================

    public static final String USR_MAX_BROWSER_TAB_COUNT_PREF_SUFFIX = "_MaxBrowserTabCnt";
    public static final int MAX_BROWSER_TAB_COUNT_DEFAULT = 15;
    public static int giMaxTabCount = 15;

    //====================================================================================
    //===== User Training Tracking =======================================================
    //====================================================================================

    //Variables for tracking user use of side bars. After the user has used them three times,
    //  stop automatically displaying them for a moment before hiding them. The temporary
    //  initial display of the side bars is to let the user know that they are there. That
    //  initial display makes use of the program by the user just a little bit slower,
    //  and also gets in the way of my own debugging.
    public static final String USR_DRAWER_USE_CNT_SUFX_CATALOG_BROWSER = "_DrawerUseCntCatBrowser";
    public static int giDrawerUseCntCatBrowser = 0;

    public static final String USR_DRAWER_USE_CNT_SUFX_IMAGE_VIEWER = "_DrawerUseCntImgViewer";
    public static int giDrawerUseCntImgViewer = 0;

    //=====================================================================================
    //===== File System Subroutines Section ===================================================
    //=====================================================================================

    public static String getUniqueFileName(Uri uriParent, String sOriginalFileName, boolean bReturnUniqueJumbledFileName){
        //This routine is used to check to see if a file name is in-use, and if so, return an alternate.
        ArrayList<String> alsFileNamesInUse = GetDirectoryFileNames(uriParent);
        return getUniqueFileName(alsFileNamesInUse, sOriginalFileName, bReturnUniqueJumbledFileName);
    }
    public static String getUniqueFileNameAppInternalTempStorage(File fileParent, String sOriginalFileName, boolean bReturnUniqueJumbledFileName) {
        //This routine is used to check to see if a file name is in-use, and if so, return an alternate.
        File[] fileFolderChildren = fileParent.listFiles();
        ArrayList<String> alsFileNamesInUse = new ArrayList<>();
        if (fileFolderChildren != null){
            for (File f : fileFolderChildren) {
                alsFileNamesInUse.add(f.getName());
            }
        }
        return getUniqueFileName(alsFileNamesInUse, sOriginalFileName, bReturnUniqueJumbledFileName);
    }

    public static String getUniqueFileName(ArrayList<String> alsFileNamesInUse, String sOriginalFileName, boolean bReturnUniqueJumbledFileName){
        //This routine is used to check to see if a file name is in-use, and if so, return an alternate.

        String sFinalFileName = sOriginalFileName;
        if(bReturnUniqueJumbledFileName) {
            //Jumble the file name before confirming there are no file duplicates.

            //Check to see if the file name is already jumbled. This may be the case particularly
            //  if the user is re-importing orphaned files. If it is already jumbled, don't jumble
            //  again:
            if(!isJumbled(sOriginalFileName)) {
                sFinalFileName = JumbleFileName(sOriginalFileName);
            }
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

            if(bReturnUniqueJumbledFileName) {
                sNewFileName = JumbleFileName(sNewFileName);
            }

            sFinalFileName = sNewFileName;
        }

        return sFinalFileName;
    }

    //Subfolder structure.
    // There are 3 catalogs at this time. All are media-based. If there are too many items in a folder
    // it slows down indexing, especially during debugging. Therefore a subfolder system has been
    // developed. As of 2023-06-13, videos and images were put into tag ID subfolders, but this
    // is unbalanced. The user is likely to have rarely-used tags that have only a few items in the
    // folder. The intention was that the user could retrieve their contents and they would
    // be sorted by the first tag assigned to the item. However, the files have been renamed in such
    // a way to prevent a global media app from finding the content and listing it in a gallery or
    // offer previews of the content. The content may be later encryped in a future version.
    // I have calculated that I'd like to have an approximate max of 250 items in a folder. The user
    // could have 62,500 items before the concept is surpassed.
    // The program must identify the current or next folder to fill with content..

    public static final TreeMap<Integer, ItemClass_StorageFolderAvailability> gtmFolderAvailability = new TreeMap<>(); //MediaCategory, Folder data.

    public static void getAGGMStorageFolderAvailability(int iMediaCategory){
        //Determine where files can be stored without over-loading folders at times when indexing actions are performed.
        //  "Indexing actions" is just "general indexing actions", whether it be initiated by the user browsing the
        //  file structure or other action.
        gtmFolderAvailability.remove(iMediaCategory);

        ArrayList<String> alsFolderNamesInUse = GetDirectorySubfolderNames(gUriCatalogFolders[iMediaCategory]);
        int iGreatestFolderID = -1;
        String sGreatestFolderID = "";
        for(String sFolderName:alsFolderNamesInUse){
            try{
                int iFolderID = Integer.parseInt(sFolderName);
                if(iFolderID > iGreatestFolderID){
                    iGreatestFolderID = iFolderID;
                    sGreatestFolderID = sFolderName; //To preserve any leading zeros that might be present.
                }

            } catch (Exception ignored){
                //Likely case is folder has non-numeric folder name.
            }
        }
        if(sGreatestFolderID.equals("") || iGreatestFolderID < 1000){
            //If there are no numeric folders of proper use, designate the first one.
            sGreatestFolderID = "1000";
            ItemClass_StorageFolderAvailability icsfa = new ItemClass_StorageFolderAvailability();
            icsfa.sFolderName = sGreatestFolderID;
            icsfa.iFileCount = 0;
            gtmFolderAvailability.put(iMediaCategory, icsfa);
            return;
        }
        //Greatest folder ID should now be found, query for content count:
        Uri uriFolder = FormChildUri(gUriCatalogFolders[iMediaCategory], sGreatestFolderID);
        //Count the number of items in the folder:
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriFolder,
                DocumentsContract.getDocumentId(uriFolder));
        Cursor c;
        try {
            c = gcrContentResolver.query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
            if (c != null) {
                int iItemCount = c.getCount();
                c.close();
                ItemClass_StorageFolderAvailability icsfa = new ItemClass_StorageFolderAvailability();
                icsfa.sFolderName = sGreatestFolderID;
                icsfa.iFileCount = iItemCount;

                if (icsfa.iFileCount >= 250) {
                    //Designate the next folder to hold content:
                    iGreatestFolderID++; //Should not yield an exception as it should have already been caught in a prior process.
                    icsfa.iFileCount = 0;
                    icsfa.sFolderName = "" + iGreatestFolderID;

                    Uri uriDestinationFolder = GlobalClass.FormChildUri(GlobalClass.gUriCatalogFolders[iMediaCategory], icsfa.sFolderName);
                    if (!GlobalClass.CheckIfFileExists(uriDestinationFolder)) {
                        try {
                            uriDestinationFolder = GlobalClass.CreateDirectory(uriDestinationFolder);

                            //If we are creating a folder here, perform the below action if necessary.
                            //If the user has been performing catalog analysis, that is, analyzing the catalog
                            // in this session, update the analysis index to include this new folder.
                            if(GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).size() > 0){
                                //If a folder was created record it
                                //  in the Catalog Analysis index variable if the index is in use.

                                String sKey = icsfa.sFolderName;
                                ItemClass_File icfCollectionFolder = new ItemClass_File(ItemClass_File.TYPE_FOLDER, icsfa.sFolderName);
                                icfCollectionFolder.sMimeType = DocumentsContract.Document.MIME_TYPE_DIR;
                                icfCollectionFolder.lSizeBytes = 0;
                                icfCollectionFolder.sUriParent = GlobalClass.gUriCatalogFolders[iMediaCategory].toString();
                                icfCollectionFolder.sMediaFolderRelativePath = icsfa.sFolderName;
                                icfCollectionFolder.sUriThumbnailFile = "";
                                icfCollectionFolder.sUri = uriDestinationFolder.toString();

                                try {
                                    Bundle bundle = DocumentsContract.getDocumentMetadata(GlobalClass.gcrContentResolver, uriDestinationFolder);
                                    if(bundle != null) {
                                        long lLastModified = bundle.getLong(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0);
                                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                                        cal.setTimeInMillis(lLastModified);
                                        icfCollectionFolder.dateLastModified = cal.getTime();

                                    }
                                } catch (FileNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                                GlobalClass.gtmicf_AllFileItemsInMediaFolder.get(iMediaCategory).put(sKey, icfCollectionFolder);

                            }


                        } catch (Exception e) {
                            Log.d("AGGalleryManager", "" + e.getMessage());
                        }
                    }
                }

                gtmFolderAvailability.put(iMediaCategory, icsfa);
            }
        } catch (Exception e) {
            Log.d("AGGalleryManager", "Problem querying folder.");
        }

    }

    public static void AssignDestinationFolders(ArrayList<ItemClass_File> alicf, int iMediaCategory){
        //This routine is used to assign folders to items being imported.
        //If there end up being more than X items per folder, it's really no big deal
        //  so long as the destination folder is in fact recorded.
        //  Item count limit per folder is really for human ability to browse objects.

        for(ItemClass_File fileItem: alicf){
            if(!fileItem.bMarkedForDeletion) {

                //Set the destination folder on each file item:
                ItemClass_StorageFolderAvailability icStorageFolderAvailability = GlobalClass.gtmFolderAvailability.get(iMediaCategory);
                if(icStorageFolderAvailability == null){
                    //Get the next folder:
                    GlobalClass.getAGGMStorageFolderAvailability(iMediaCategory);
                    icStorageFolderAvailability = GlobalClass.gtmFolderAvailability.get(iMediaCategory);
                }
                if(icStorageFolderAvailability != null) {
                    if(iMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                        if(fileItem.iTypeFileFolderURL == ItemClass_File.TYPE_FOLDER){
                            //If this is the comic folder fileItem, increase the file count by 1.
                            //  The comic will be stored in a folder inside the subfolder. It is the
                            //  subfolder count that is being increased.
                            icStorageFolderAvailability.iFileCount++;
                        }
                    } else {
                        icStorageFolderAvailability.iFileCount++;
                    }
                    if(icStorageFolderAvailability.iFileCount >= 250){
                        //Designate the next folder to hold content:
                        int iFolderID = Integer.parseInt(icStorageFolderAvailability.sFolderName); //Should not yield an exception as it should have already been caught in a prior process.
                        iFolderID++;
                        icStorageFolderAvailability.iFileCount = 0;
                        icStorageFolderAvailability.sFolderName = "" + iFolderID;
                    }

                    fileItem.sDestinationFolder = icStorageFolderAvailability.sFolderName;
                }
            }
        }

    }

    //=====================================================================================
    //===== Catalog Maintenance Subroutines Section ===================================================
    //=====================================================================================

    public static final String BROADCAST_CATALOG_FILES_MAINTENANCE = "com.agcurations.aggallerymanager.intent.action.CATALOG_FILES_MAINTENANCE";

    static AtomicInteger aiCatalogVerificationRunning = new AtomicInteger(0);
    static AtomicInteger aiCatalogUpdateRunning = new AtomicInteger(0);
    public static final int STOPPED = 0;
    public static final int START_REQUESTED = 1;
    public static final int RUNNING = 2;
    public static final int STOP_REQUESTED = 3;
    public static final int FINISHED = 4;


    public void deJumbleOrphanedFiles(int iMediaCategory){

        int iProgressNumerator = 0;
        int iProgressDenominator = gtmCatalogLists.get(MEDIA_CATEGORY_VIDEOS).size();
        int iProgressBarValue = 0;


        //Check folders:
        ArrayList<String> alsFolderNamesInUse = GetDirectorySubfolderNames(gUriCatalogFolders[iMediaCategory]);

        iProgressDenominator = alsFolderNamesInUse.size();

        for(String sFolderName: alsFolderNamesInUse){

            iProgressNumerator++;
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, "Checking folder " + sFolderName + "...",
                    BROADCAST_WRITE_CATALOG_FILE);

            try{
                //Check if this is an old-system folder:
                int iFolderID = Integer.parseInt(sFolderName);
                if(iFolderID >= 1000){
                    continue;
                }
            } catch (Exception e){
                Log.d("AGGalleryManager", e.getMessage() + "");
            }

            Uri uriFolder = FormChildUri(gUriCatalogFolders[iMediaCategory], sFolderName);
            //Count the number of items in the folder:
            final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriFolder,
                    DocumentsContract.getDocumentId(uriFolder));
            Cursor c;
            try {
                c = gcrContentResolver.query(childrenUri, new String[]{
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
                if (c != null) {
                    int iItemCount = c.getCount();
                    while (c.moveToNext()) {
                        final String sFileName = c.getString(0);
                        final String sMimeType = c.getString( 1);
                        if(!sMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)){
                            //If it is a file, de-jumble the filename so that it can be picked up and viewed.
                            if(isJumbled(sFileName)) {
                                String sNewFilename = JumbleFileName(sFileName);
                                Uri uriFile = FormChildUri(uriFolder, sFileName);
                                try {
                                    DocumentsContract.renameDocument(gcrContentResolver, uriFile, sNewFilename);
                                } catch (Exception e) {
                                    Log.d("AGGalleryManager", "Trouble with file dejumble of orphaned file.");
                                }
                            }
                        }

                    }
                    c.close();
                }
            } catch (Exception e) {
                Log.d("AGGalleryManager", "Problem querying folder.");
            }

        }

    }

    public static ArrayList<String> getListOfNonJumbledFileExtensions(){
        ArrayList<String> alsRecognizedNonJumbledFileExtensions = new ArrayList<>();

        alsRecognizedNonJumbledFileExtensions.add(".mp4");
        alsRecognizedNonJumbledFileExtensions.add(".webm");
        alsRecognizedNonJumbledFileExtensions.add(".wmv");
        alsRecognizedNonJumbledFileExtensions.add(".avi");
        alsRecognizedNonJumbledFileExtensions.add(".mov");
        alsRecognizedNonJumbledFileExtensions.add(".mkv");
        alsRecognizedNonJumbledFileExtensions.add(".flv");
        alsRecognizedNonJumbledFileExtensions.add(".ogg");
        alsRecognizedNonJumbledFileExtensions.add(".jpg");
        alsRecognizedNonJumbledFileExtensions.add(".jpeg");
        alsRecognizedNonJumbledFileExtensions.add(".gif");
        alsRecognizedNonJumbledFileExtensions.add(".tiff");
        alsRecognizedNonJumbledFileExtensions.add(".png");
        alsRecognizedNonJumbledFileExtensions.add(".bmp");
        alsRecognizedNonJumbledFileExtensions.add(".ts");

        return alsRecognizedNonJumbledFileExtensions;
    }

    public static boolean isJumbled(String sFilename){

        ArrayList<String> alsRecognizedFileExtensions = getListOfNonJumbledFileExtensions();
        boolean bExtensionRecognized = false;

        for(String sExtension: alsRecognizedFileExtensions){
            if(sFilename.toLowerCase(Locale.ROOT).endsWith(sExtension.toLowerCase(Locale.ROOT))){
                bExtensionRecognized = true;
                break;
            }
        }

        return !bExtensionRecognized;
    }




    //=====================================================================================
    //===== Other Subroutines Section ===================================================
    //=====================================================================================


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
                //todo: Add code to always have 3 significant digits
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

    public static final String COMIC_DETAILS_SUCCESS = "COMIC_DETAILS_SUCCESS";

    public static final String EXTRA_CALLER_ACTION_RESPONSE_FILTER = "com.agcurations.aggallerymanager.extra.EXTRA_CALLER_ACTION_RESPONSE_FILTER";

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

    public double gdWebComicDataExpirationDuration = 00000000.010000000d; //1 hour by gsDatePatternNumSort = "yyyyMMdd.HHmmssSSS". gsDatePatternNumSort used for code commonality with
                                                                          // other implementations of time stamps.

    public static String cleanHTMLCodedCharacters(String sInput){
        String sResult;
        sResult = Html.fromHtml(sInput,0).toString();
        try {
            sResult = URLDecoder.decode(sResult, "UTF-8");
        } catch (Exception ignored){

        }
        return sResult;

    }

    public void BroadcastProgress(boolean bUpdateLog, String sLogLine,
                                  boolean bUpdatePercentComplete, int iAmountComplete,
                                  boolean bUpdateProgressBarText, String sProgressBarText,
                                  String sIntentActionFilter){


        //Preserve data in memory for the event of a screen rotation, or activity loses focus. This
        //  is typically used when log data is given from a worker in a separate thread.
        switch (sIntentActionFilter) {
            case Worker_Import_ImportComicFolders.IMPORT_COMIC_FOLDERS_ACTION_RESPONSE:
            case Worker_Import_ImportFiles.IMPORT_FILES_ACTION_RESPONSE:
            case Worker_Import_ImportComicWebFiles.IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE:
            case Worker_Import_VideoDownload.IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE:
            case Worker_LocalFileTransfer.IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE:
            case GlobalClass.CATALOG_CREATE_NEW_RECORDS_ACTION_RESPONSE:
                if (bUpdatePercentComplete) {
                    giImportExecutionProgressBarPercent = iAmountComplete;
                }
                if (bUpdateProgressBarText) {
                    //Preserve progress bar text for the event of a screen rotation, or activity looses focus:
                    gsImportExecutionProgressBarText = sProgressBarText;
                }
                if (bUpdateLog) {
                    //Preserve the log for the event of a screen rotation, or activity looses focus:
                    gsbImportExecutionLog.append(sLogLine);
                }
                break;
            case Fragment_Import_1_StorageLocation.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE:
                if (bUpdatePercentComplete) {
                    giImportFolderAnalysisProgressBarPercent = iAmountComplete;
                }
                if (bUpdateProgressBarText) {
                    gsImportFolderAnalysisProgressBarText = sProgressBarText;
                }
                break;
            case Worker_DeleteFiles.DELETE_FILES_RESPONSE:
                if (bUpdatePercentComplete) {
                    giDeleteFilesExecutionProgressBarPercent = iAmountComplete;
                }
                if (bUpdateProgressBarText) {
                    //Preserve progress bar text for the event of a screen rotation, or activity looses focus:
                    gsDeleteFilesExecutionProgressBarText = sProgressBarText;
                }
                if (bUpdateLog) {
                    //Preserve the log for the event of a screen rotation, or activity looses focus:
                    gsbDeleteFilesExecutionLog.append(sLogLine);
                }
                break;
            case Worker_Catalog_Analysis.CATALOG_ANALYSIS_ACTION_RESPONSE:
                if (bUpdatePercentComplete) {
                    giCatalogAnalysis_ProgressBarPercent = iAmountComplete;
                }
                if (bUpdateProgressBarText) {
                    //Preserve progress bar text for the event of a screen rotation, or activity looses focus:
                    gsCatalogAnalysis_ProgressBarText = sProgressBarText;
                }
                if (bUpdateLog) {
                    //Preserve the log for the event of a screen rotation, or activity looses focus:
                    gsbCatalogAnalysis_ExecutionLog.append(sLogLine);
                }
                break;
        }

        //Broadcast a message to be picked-up by an Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(sIntentActionFilter);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        broadcastIntent.putExtra(UPDATE_LOG_BOOLEAN, bUpdateLog);
        broadcastIntent.putExtra(LOG_LINE_STRING, sLogLine);
        broadcastIntent.putExtra(UPDATE_PERCENT_COMPLETE_BOOLEAN, bUpdatePercentComplete);
        broadcastIntent.putExtra(PERCENT_COMPLETE_INT, iAmountComplete);
        broadcastIntent.putExtra(UPDATE_PROGRESS_BAR_TEXT_BOOLEAN, bUpdateProgressBarText);
        broadcastIntent.putExtra(PROGRESS_BAR_TEXT_STRING, sProgressBarText);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }

    public static Data DataErrorMessage(String sMessage){ //todo: Replace local instances of DataErrorMessage in other classes with a reference to this.
        return new Data.Builder()
                .putString(FAILURE_MESSAGE, sMessage)
                .build();
    }

    //=====================================================================================
    //===== XPath Testing  ================================================================
    //=====================================================================================
    public static String sWebPageHTML;
    public static AtomicBoolean gabHTMLHolderAvailable = new AtomicBoolean(true);

    //=====================================================================================
    //===== Import Options ================================================================
    //=====================================================================================

    public static final String IMPORT_REQUEST_FROM_INTERNAL_BROWSER = "com.agcurations.aggallerymanager.importurl";

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

    //Video import web html search strings (may change if the website changes)

    boolean gbLogM3U8Files = false;     //Writes M3U8 files as they are read and analyzed as part of an
                                        //  import interpretive analysis.

    public final boolean gbOptionSilenceActiveStreamListening = true;
        //This option for video import from web source. During detection, if the user plays the
        //  the video a stream may continually request .ts files. This attempts to filter them out.
        //  However, it may block content that the user wants. Thus, allow it to be an option.


    //Create an array of keys that allow program to locate video links:
    public static ArrayList<ItemClass_WebVideoDataLocator> galWebVideoDataLocators;

    //Array to allow passing of web comic series analysis data:
    public static AtomicBoolean gabComicSeriesArrayAvailable = new AtomicBoolean(true);
    public static ArrayList<ItemClass_File> galicf_ComicSeriesEntries;

    public static boolean gbComicAutoDetect = true; //Automatically Detect/Analyze webpage contents once page has loaded.

    public static final int DOWNLOAD_WAIT_TIMEOUT = 2 * 60 * 60 * 1000; //2 hours in milliseconds.

    public static int giAutoProcessingWaitTimeMS = 1000; //Wait time to allow a web page to load, after onPageFinished, before performing an analysis.
                                                         //  This is because sometimes the onPageFinished call happens before is really is truly fully loaded.
                                                         //todo: Move this to a setting in the browser section.

    public static boolean gbAutoDownloadGroupComics = false;
    public static boolean gbAutoNavigateNextChapter = false;

    public static ArrayList<ItemClass_WebComicDataLocator> getComicWebDataKeys(){

        //Tools:
        // https://regex101.com/
        // https://www.freeformatter.com/xpath-tester.html

        ArrayList<ItemClass_WebComicDataLocator> alWebComicDataLocators = new ArrayList<>();
        ItemClass_WebComicDataLocator itemClass_webComicDataLocator;

        itemClass_webComicDataLocator =
                FormWebImageSeriesDataLocator("^h%t%tps:\\/\\/n%he%n%ta%i\\.n%e%t\\/g\\/\\d{1,7}\\/$",
                        null);
        itemClass_webComicDataLocator.sComicSeriesIDStartString = "h%t%tps://n%he%n%ta%i.n%e%t/g/";
        itemClass_webComicDataLocator.sShortName = "nH"; //For hard-coded behavior differentiation
        alWebComicDataLocators.add(itemClass_webComicDataLocator);


        itemClass_webComicDataLocator =
                FormWebImageSeriesDataLocator("^h%t%tps:\\/\\/m%an%g%ap%ark%.%i%o\\/title\\/(.*)\\/(.*)$",
                        null);
        itemClass_webComicDataLocator.sComicSeriesIDStartString = "ht%tps://man%gapa%rk.io/title/";
        //https://mangapark.io/title/10049-en-hikaru-no-go/64697-vol-22-ch-175
        itemClass_webComicDataLocator.sShortName = "MP"; //For hard-coded behavior differentiation
        alWebComicDataLocators.add(itemClass_webComicDataLocator);

        return alWebComicDataLocators;
    }

    private static ItemClass_WebComicDataLocator FormWebImageSeriesDataLocator(String sNonExplicitAddress, String[][] sSearchKeys){
        //Include parenthesis in sNonExplicitAddress to obscure the web address so that searchboottss cannot find it.
        String sExplicitAddress = sNonExplicitAddress.replace("%","");
        ItemClass_WebComicDataLocator itemClass_webComicDataLocator = new ItemClass_WebComicDataLocator(sExplicitAddress);  //Re-create the data locator, clearing-out any found data.
        itemClass_webComicDataLocator.alComicDownloadSearchKeys = new ArrayList<>();

        if(sSearchKeys != null) {
            for (String[] sFields : sSearchKeys) {
                if (sFields.length == 2) {
                    //SxPathExpression Search Key
                    itemClass_webComicDataLocator.alComicDownloadSearchKeys.add(
                            new ItemClass_ComicDownloadSearchKey(
                                    sFields[0], sFields[1]));
                } else if (sFields.length == 3) {
                    //Text Search Key
                    itemClass_webComicDataLocator.alComicDownloadSearchKeys.add(
                            new ItemClass_ComicDownloadSearchKey(
                                    sFields[0], sFields[1], sFields[2]));
                }
            }
        }
        return itemClass_webComicDataLocator;
    }





    //=====================================================================================
    //===== Catalog Analysis Options ================================================================
    //=====================================================================================

    public static boolean bAllowCheckAndMoveOfComicFolders = false;

    //==============================================================================================
    //=========== Other Options ====================================================================

    public static boolean gbUseCatalogItemThumbnailDeepSearch = false;


    public static int giLogFileKeepDurationInDays = 30;

    public static boolean gbOptionUserAutoLogin = false;

    public static boolean gbOptionIndividualizeM3U8VideoSegmentPlayback = false;
    //If the option to individualize M3U8 video segment playback is selected,
    //  create an array of the individual video segment files and feed
    //  them into the ExoPlayer as a playlist.
    //  There was an issue during coding and testing an M3U8
    //  in which the program would freeze the entire tablet causing the
    //  need for a hard reset. If this happens again, a coder can change the
    //  buffer amount (in onCreate), or configure this boolean to be
    //  user-configurable.
    //  If the option to individualize M3U8 video segment playback is not selected,
    //  play an SAF-adapted M3U8 file. That is, a file with video listings
    //  of Android Storage Access Framework Uris.


    //Repair Orphaned File Items: This variable/function used to move files/folders associated
    // with orphaned files. The catalog analysis will first look for catalog items missing their
    // media, then will look for orphaned files in the catalog media storage location. If media
    // is found that matches the file name, and the file name is unique both among the 'storage'
    // and the 'database memory', thus there is a one-to-one relation, then the program will move
    // the orphaned file item into the location indicated by the catalog database entry.
    // The occurrence limit variable is to allow the user to check success of a repair operation
    // before continuing.
    public static boolean gbCatalogAnalysis_RepairOrphanedItems = false;
    public static int giCatalogAnalysis_RepairOrphanedItemLimit = 1;

    //Trim Missing Catalog Items: This variable is used to trim database entries for catalog items
    // for which the associated media cannot be found.
    public static boolean gbCatalogAnalysis_TrimMissingCatalogItems = false;
    public static int giCatalogAnalysis_TrimMissingCatalogItemLimit = 1; //Set to -1 to trim all applicable.


    //==============================================================================================
    //=========== Preferences ======================================================================

    //Data storage location:
    public static final String gsPreference_DataStorageLocationUri = "com.agcurations.aggallerymanager.preferences.DataStorageLocation";
    //Data storage location, user-friendly for display purposes:
    public static final String gsPreference_DataStorageLocationUriUF = "com.agcurations.aggallerymanager.preferences.DataStorageLocationUF";

    public static final String gsPreference_Import_IncludeGraphicsFileData = "com.agcurations.aggallerymanager.preferences.ImportIncludeGraphicsFileData";


    public static final String PREF_WEB_TAB_PREV_FOCUS_INDEX_PREFIX = "com.agcurations.aggallerymanager.preference.web_tab_prev_focus_index_"; //Current user name to be appended.

    public static String PREF_APPLICATION_LOG_PATH_FILENAME = "APPLICATION_LOG_PATH_FILENAME";
    public static String PREF_WRITE_APPLICATION_LOG_FILE = "WRITE_APPLICATION_LOG_FILE";

    public static String PREF_USE_FFMPEG_TO_MERGE_VIDEO_STREAMS = "USE_FFMPEG_TO_MERGE_VIDEO_STREAMS";

}

