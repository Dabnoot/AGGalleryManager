package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Activity_ComicViewer extends AppCompatActivity {

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };


    //Global constants
    public static final String EXTRA_COMIC_PAGE_START = "COMIC_PAGE_START";

    public static final String SAVED_STATE_ITEM_INDEX = "SAVED_STATE_ITEM_INDEX";

    //Global variables

    //Comic global variables
    private GlobalClass globalClass;
    private TreeMap<Integer, Uri> tmImagePaths;
    private TreeMap<Integer, String> tmImageFileNamesReadable;
    private String gsActivityTitleString = "";
    private int giCurrentCatalogItemIndex;
    private int giMaxFileCount;

    //Graphics global variables
    private ImageView givImageViewer;
    private Point gpDisplaySize;
    private Point gpLastDisplaySize;
    private int giImageWidth = 0;
    private int giImageHeight = 0;
    Display gDisplay;

    // Matrices for moving and zooming image:
    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();
    // Scaling
    private float gfScaleFactor = 1.0f;
    private float gfMinScale = 1.0f;
    private float gfScaleWidthMatch = 0.0f;
    private float gfScaleHeightMatch = 0.0f;
    private float gfJumpOutAxisScale = 0.0f;
    private boolean gbOkToZoomJumpOut = true;
    boolean gbOkToZoomJumpIn = true;
    // Image reset to original coords:
    private float gfImageViewOriginX = -1.0f;
    private float gfImageViewOriginY = -1.0f;

    //Touch data processing global variables
    // Touch actions - We can be in one of these 3 states
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    // Zooming:
    private final PointF gpTouchStart = new PointF();
    private final PointF gpMidPoint = new PointF();
    private float gfPreviousPinchDistance = 1f;

    //Debug assistance global variables
    private final boolean gbDebugSwiping = false;
    private TextView gtvDebug;
    private int giDebugLineCount;

    //Other globals
    private int iSwipeToExitCounter = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_page_viewer);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        globalClass = (GlobalClass) getApplicationContext();

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        gtvDebug = findViewById(R.id.textViewDebug);


        //Get the intent used to start this activity:
        Intent intentCaller = getIntent();
        //Get data from the intent:
        if (savedInstanceState != null) {
            giCurrentCatalogItemIndex = savedInstanceState.getInt(SAVED_STATE_ITEM_INDEX);
        } else {
            giCurrentCatalogItemIndex = intentCaller.getIntExtra(EXTRA_COMIC_PAGE_START, 0);
        }
        ItemClass_CatalogItem gciCatalogItem;
        gciCatalogItem = (ItemClass_CatalogItem) intentCaller.getSerializableExtra(GlobalClass.EXTRA_CATALOG_ITEM);


        if( gciCatalogItem == null) return;
        gsActivityTitleString = gciCatalogItem.sTitle;
        giMaxFileCount = gciCatalogItem.iFile_Count;

        String sComicFolderUri = GlobalClass.gsUriAppRootPrefix
                + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[gciCatalogItem.iMediaCategory]
                + GlobalClass.gsFileSeparator + gciCatalogItem.sFolderRelativePath;

        //Load the full path to each comic page into tmComicPages:
        if (gciCatalogItem.iSpecialFlag == ItemClass_CatalogItem.FLAG_COMIC_DLM_MOVE) {
            //If this is a downloaded comic and the files from DownloadManager have not been moved as
            //  part of download post-processing, look in the [comic]\download folder for the files:
            sComicFolderUri = GlobalClass.gsUriAppRootPrefix
                    + GlobalClass.gsFileSeparator + GlobalClass.gsCatalogFolderNames[gciCatalogItem.iMediaCategory]
                    + GlobalClass.gsFileSeparator + gciCatalogItem.sFolderRelativePath
                    + GlobalClass.gsFileSeparator + GlobalClass.gsDLTempFolderName;
        }



        Uri uriComicFolderUri = Uri.parse(sComicFolderUri);
        if(!GlobalClass.CheckIfFileExists(uriComicFolderUri)){
            Toast.makeText(getApplicationContext(), "Comic folder does not exist. Try deleting and re-import the comic.", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uriComicFilesChildUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriComicFolderUri,
                DocumentsContract.getDocumentId(uriComicFolderUri));
        Cursor cComicFiles = GlobalClass.gcrContentResolver.query(uriComicFilesChildUri,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        DocumentsContract.Document.COLUMN_SIZE,
                        DocumentsContract.Document.COLUMN_SUMMARY,
                        DocumentsContract.Document.COLUMN_FLAGS,
                        DocumentsContract.Document.COLUMN_ICON},
                null,
                null,
                null);

        tmImagePaths = new TreeMap<>();
        tmImageFileNamesReadable = new TreeMap<>();

        TreeMap<String, Uri> tmSortByFileName = new TreeMap<>();
        if(cComicFiles != null) {
            while(cComicFiles.moveToNext()){
                String sMimeType = cComicFiles.getString(2);
                if(sMimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)){
                    continue; //Don't add any folders, if there might be one.
                }
                String sFileName = cComicFiles.getString(1);
                String sFileUri = sComicFolderUri
                        + GlobalClass.gsFileSeparator + sFileName;
                Uri uriFileUri = Uri.parse(sFileUri);
                tmSortByFileName.put(GlobalClass.JumbleFileName(sFileName), uriFileUri);
            }
            cComicFiles.close();
        }

        int i = 0;
        for (Map.Entry<String, Uri> tmFiles : tmSortByFileName.entrySet()) {
            tmImagePaths.put(i, tmFiles.getValue());
            tmImageFileNamesReadable.put(i, tmFiles.getKey()); //For toast messages when changing the page.
            i++;
        }



        //Get the display size:
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        gDisplay = windowManager.getDefaultDisplay(); //Deprecated. Use GlobalClass.getScreenWidth(this); upon refactoring opportunity.
        gpDisplaySize = new Point();
        gpLastDisplaySize = new Point();
        gDisplay.getRealSize(gpDisplaySize); //Get the total size of the screen, assume that the navigation bar will be hidden.
        gDisplay.getRealSize(gpLastDisplaySize);
        //display.getSize(gpDisplaySize);   //Get the size of the screen with the navigation bar shown.

        //Post a runnable from onCreate(), that will be executed when the view has been created, in
        //  order to load the first comic page:
        mContentView.post(new Runnable() {
            public void run() {
                //https://stackoverflow.com/questions/12829653/content-view-width-and-height/21426049
                LoadComicPage(giCurrentCatalogItemIndex);
            }
        });

        mContentView.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeTop() {
                if (gbDebugSwiping) {
                    makeToast("Swiped top", Toast.LENGTH_SHORT);
                }
            }

            public void onSwipeBottom() {
                if (gbDebugSwiping) {
                    makeToast("Swiped bottom", Toast.LENGTH_SHORT);
                }
            }

            public void onSwipeRight() {
                if (gbDebugSwiping) {
                    makeToast("Swiped right", Toast.LENGTH_SHORT);
                }
                if (gfScaleFactor <= gfJumpOutAxisScale) {
                    if (giCurrentCatalogItemIndex == 0) {
                        if (iSwipeToExitCounter == 0) {
                            makeToast("Start of comic", Toast.LENGTH_SHORT);
                        } else if (iSwipeToExitCounter == 1) {
                            makeToast("Exiting...", Toast.LENGTH_SHORT);
                            finish();
                        }
                        iSwipeToExitCounter++;
                    } else {
                        gotoPreviousComicPage();
                        iSwipeToExitCounter = 0;
                    }
                }
            }

            public void onSwipeLeft() {
                if (gbDebugSwiping) {
                    makeToast("Swiped left", Toast.LENGTH_SHORT);
                }

                if (gfScaleFactor <= gfJumpOutAxisScale) {

                    if (giCurrentCatalogItemIndex == (giMaxFileCount - 1)) {
                        if (iSwipeToExitCounter == 0) {
                            makeToast("End of comic", Toast.LENGTH_SHORT);
                        } else if (iSwipeToExitCounter == 1) {
                            makeToast("Exiting...", Toast.LENGTH_SHORT);
                            finish();
                        }
                        iSwipeToExitCounter++;
                    } else {
                        gotoNextComicPage();
                        iSwipeToExitCounter = 0;
                    }
                }
            }
        });


        givImageViewer = findViewById(R.id.imageView_ComicPage);
        givImageViewer.setScaleType(ImageView.ScaleType.MATRIX);


    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(SAVED_STATE_ITEM_INDEX, giCurrentCatalogItemIndex);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    //=====================================================================================
    //===== Load Comic Page =================================================================
    //=====================================================================================

    private void LoadComicPage(int iPageIndex){


        Uri uriComicPage = null;
        if(tmImagePaths.containsKey(iPageIndex)){
            uriComicPage = tmImagePaths.get(iPageIndex);
            if(uriComicPage == null){
                return;
            }
        } else {
            return;
        }


        //https://developer.android.com/topic/performance/graphics/load-bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        //BitmapFactory.decodeFile(uriComicPage.getAbsolutePath(), options);

        try {
            Rect rect = new Rect(0,0,0,0);
            InputStream inputStream = GlobalClass.gcrContentResolver.openInputStream(uriComicPage);
            BitmapFactory.decodeStream(inputStream, rect, options);
        } catch (Exception e){
            Log.d("Activity_ComicViewer", "Could not open decode file.");
        }

        //Get the dimensions of the image in the file without loading the image:
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;

        //Find the required dimensions of the image:
        int reqHeight;
        int reqWidth;
        reqHeight = mContentView.getHeight();
        reqWidth = mContentView.getWidth();

        int inSS = 1; //Scalar to scale-down the image.

        if (imageHeight > reqHeight || imageWidth > reqWidth) {

            final int halfHeight = imageHeight / 2;
            final int halfWidth = imageWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSS) >= reqHeight
                    && (halfWidth / inSS) >= reqWidth) {
                inSS *= 2;
            }

            options.inSampleSize = inSS;

        }
        options.inJustDecodeBounds = false;

        //todo: REFACTOR USING createImageThumbnail ONCE API LEVEL 29 IS COMMON

        Bitmap myBitmap = null;
        //myBitmap = BitmapFactory.decodeFile(uriComicPage.getAbsolutePath(), options);
        try {
            Rect rect = new Rect(0,0,0,0);
            InputStream inputStream = GlobalClass.gcrContentResolver.openInputStream(uriComicPage);
            myBitmap = BitmapFactory.decodeStream(inputStream, rect, options);
        } catch (Exception e){
            Log.d("Activity_ComicViewer", "Could not open decode file.");
        }
        setTitle(gsActivityTitleString);
        if(globalClass.gbOptionComicViewerShowPageNumber) {
            makeToast(tmImageFileNamesReadable.get(iPageIndex), Toast.LENGTH_SHORT);
        }

        givImageViewer.setImageBitmap(myBitmap);
        setInitialZoom();

    }

    private void setInitialZoom(){
        //Set the zoom level for the image.
        int iInitialZoomOption;
        if(giCurrentCatalogItemIndex == 0) {
            iInitialZoomOption = globalClass.giOptionComicViewerCoverPageStartZoomConfiguration;
        } else {
            iInitialZoomOption = globalClass.giOptionComicViewerContentPageStartZoomConfiguration;
        }
        //Set start zoom for the first page (likely the cover page)
        if(iInitialZoomOption == GlobalClass.COMIC_VIEWER_PAGE_FIT_SCREEN){
            //If the user has set option to have the entire cover page to be fit into the screen
            if(gpDisplaySize.x > gpDisplaySize.y) {
                //Screen is landscape orientation
                ComicPageZoomToMatchHeight();
            } else {
                //Screen is portrait orientation
                ComicPageZoomToMatchWidth();
            }
        } else if (iInitialZoomOption == GlobalClass.COMIC_VIEWER_PAGE_MATCH_HEIGHT){
            ComicPageZoomToMatchHeight();
        } else if (iInitialZoomOption == GlobalClass.COMIC_VIEWER_PAGE_MATCH_WIDTH){
            ComicPageZoomToMatchWidth();
        }
    }

    private void ComicPageZoomToMatchHeight(){
        //This routine gets called after a page is loaded, and when an orientation change has
        //  been detected.
        //Get the dimensions of the image loaded into the ImageView:
        Drawable d = givImageViewer.getDrawable();
        if( d == null) return;

        giImageWidth = d.getIntrinsicWidth();
        giImageHeight = d.getIntrinsicHeight();

        RectF imageRectF = new RectF(0, 0, giImageWidth, giImageHeight);
        RectF viewRectF = new RectF(0, 0, givImageViewer.getWidth(), gpDisplaySize.y);
        matrix.setRectToRect(imageRectF, viewRectF, Matrix.ScaleToFit.CENTER);

        //Get the values from the matrix:
        float[] values = new float[9];
        matrix.getValues(values);

        //Calculate scaling reference points:
        gfScaleWidthMatch = gpDisplaySize.x / (float) giImageWidth;
        gfScaleHeightMatch = gpDisplaySize.y / (float) giImageHeight;
        gfMinScale = Math.min(gfScaleHeightMatch, gfScaleWidthMatch);

        //Depending on the dimensions of the image and the screen,
        //  the jump direction could be horizontal or vertical. Either way, it
        //  will be in the direction of the larger scaling point. Grab that
        //  scale:
        gfJumpOutAxisScale = Math.max(gfScaleHeightMatch, gfScaleWidthMatch);

        gfScaleFactor = gfJumpOutAxisScale; //Track the current scale.

        //Get the new translated X and Y coordinates.
        gfImageViewOriginX = values[Matrix.MTRANS_X];
        gfImageViewOriginY = values[Matrix.MTRANS_Y];

        givImageViewer.setImageMatrix(matrix);
    }

    private void ComicPageZoomToMatchWidth(){
        //This routine gets called after a page is loaded, and when an orientation change has
        //  been detected.
        //Get the dimensions of the image loaded into the ImageView:
        Drawable d = givImageViewer.getDrawable();
        if( d == null) return;

        giImageWidth = d.getIntrinsicWidth();
        giImageHeight = d.getIntrinsicHeight();

        RectF imageRectF = new RectF(0, 0, giImageWidth, giImageHeight);
        RectF viewRectF = new RectF(0, 0, gpDisplaySize.x, givImageViewer.getHeight());
        matrix.setRectToRect(imageRectF, viewRectF, Matrix.ScaleToFit.CENTER);

        //Get the values from the matrix:
        float[] values = new float[9];
        matrix.getValues(values);

        //Calculate scaling reference points:
        gfScaleWidthMatch = gpDisplaySize.x / (float) giImageWidth;
        gfScaleHeightMatch = gpDisplaySize.y / (float) giImageHeight;
        gfMinScale = Math.min(gfScaleHeightMatch, gfScaleWidthMatch);

        //Depending on the dimensions of the image and the screen,
        //  the jump direction could be horizontal or vertical. Either way, it
        //  will be in the direction of the larger scaling point. Grab that
        //  scale:
        gfJumpOutAxisScale = Math.max(gfScaleHeightMatch, gfScaleWidthMatch);

        gfScaleFactor = gfJumpOutAxisScale; //Track the current scale.

        //Get the new translated X and Y coordinates.
        gfImageViewOriginX = values[Matrix.MTRANS_X];
        gfImageViewOriginY = values[Matrix.MTRANS_Y];





        //Set the translated x-value to the left edge of the screen:
        values[Matrix.MTRANS_X] = 0;
        //Set the translated y-value to the top edge of the screen:
        values[Matrix.MTRANS_Y] = 0;

        //Set the zoom level to that required to match the width of the screen:
        values[Matrix.MSCALE_X] = gfJumpOutAxisScale;
        values[Matrix.MSCALE_Y] = gfJumpOutAxisScale;

        //Place the values in the matrix:
        matrix.setValues(values);




        givImageViewer.setImageMatrix(matrix);
    }

    private void gotoNextComicPage(){
        if(giCurrentCatalogItemIndex < tmImagePaths.size() - 1){
            giCurrentCatalogItemIndex++;
            LoadComicPage(giCurrentCatalogItemIndex);
        }
    }

    private void gotoPreviousComicPage(){
        if(giCurrentCatalogItemIndex > 0){
            giCurrentCatalogItemIndex--;
            LoadComicPage(giCurrentCatalogItemIndex);
        }
    }

    //=====================================================================================
    //===== Touch Listener =================================================================
    //=====================================================================================

    class OnSwipeTouchListener implements View.OnTouchListener {
        //https://www.journaldev.com/28900/android-gesture-detectors
        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context ctx) {
            gestureDetector = new GestureDetector(ctx, new GestureListener());
        }

        private int giOnTouchProcessCount = 0; //Used to limit writes to the debug window on the screen.

        @SuppressLint("DefaultLocale")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            v.performClick();

            //Code to process movement of the image:

            float scale;
            float[] values = new float[9];

            String s="";
            // Handle touch events here...
            switch (event.getAction() & MotionEvent.ACTION_MASK) {

                case MotionEvent.ACTION_DOWN: //first finger down only
                    //First check to see if the orientation has changed:
                    gDisplay.getRealSize(gpDisplaySize); //Get the total size of the screen, assume that the navigation bar will be hidden.
                    if(gpDisplaySize.x != gpLastDisplaySize.x){
                        //Display has rotated!
                        ComicPageZoomToMatchHeight();
                        gDisplay.getRealSize(gpLastDisplaySize);
                    }
                    if(gfScaleFactor == gfMinScale){
                        gbOkToZoomJumpOut = true;
                    }
                    if(gfScaleFactor < gfJumpOutAxisScale){
                        //Reset ZoomJumpIn if the scale is within the JumpOutAxis.
                        //  We don't allow ZoomJumpIn if we are outside this limit
                        //  because the user is probably interested in zooming in just a little
                        //  bit.
                        gbOkToZoomJumpIn = true;
                    }




                    savedMatrix.set(matrix);
                    gpTouchStart.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_UP: //first finger lifted
                case MotionEvent.ACTION_POINTER_UP: //second finger lifted

                    savedMatrix.set(givImageViewer.getImageMatrix());
                    mode = NONE;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN: //second finger down
                    gfPreviousPinchDistance = spacing(event);
                    if (gfPreviousPinchDistance > 5f) {
                        savedMatrix.set(matrix);
                        midPoint(gpMidPoint, event);
                        mode = ZOOM;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    //Recall the matrix from when the user touched the screen:
                    matrix.set(savedMatrix);
                    //Get the values from the matrix for evaluation:
                    matrix.getValues(values);

                    float fVerticalScrollValue;
                    float fHorizontalScrollValue;


                    //========================================
                    //======  PANNING  =======================
                    //========================================
                    if (mode == DRAG) { //movement of first finger

                        //Get the drag/pan distance:
                        fVerticalScrollValue = event.getY() - gpTouchStart.y;
                        fHorizontalScrollValue = event.getX() - gpTouchStart.x;

                        //Accelerate the panning if desired by settings:
                        if(globalClass.bCPV_PanAcceleration) {
                            if(globalClass.iCPV_PanSpeedMethod == GlobalClass.CPV_PAN_SPEED_FIXED) {
                                fVerticalScrollValue *= globalClass.fCPV_VerticalPanScalar;
                                fHorizontalScrollValue *= globalClass.fCPV_HorizontalPanScalar;
                            } else {
                                fVerticalScrollValue *= gfScaleFactor;
                                fHorizontalScrollValue *= gfScaleFactor;
                            }
                        }

                        //Translate the matrix:
                        matrix.postTranslate(fHorizontalScrollValue, fVerticalScrollValue);

                        //Check to see if we are out of bounds.
                        //  If not, update the ImageView matrix:
                        //Get the values from the matrix for evaluation:
                        matrix.getValues(values);
                        float fImageX = values[Matrix.MTRANS_X]; //The left edge of the image, translated.
                        float fImageY = values[Matrix.MTRANS_Y]; //The top edge of the image, translated.
                        float fScaledWidth = values[Matrix.MSCALE_X] * giImageWidth;
                        float fScaledHeight = values[Matrix.MSCALE_Y] * giImageHeight;
                        float fImageEndTRANS_X = values[Matrix.MTRANS_X] + fScaledWidth; //X-coord of the end of the translated image:
                        float fImageEndTRANS_Y = values[Matrix.MTRANS_Y] + fScaledHeight; //Y-coord of the end of the translated image.

                        //Stop pan-down if at the top:
                        if((fImageY >= 0.0f) && (fVerticalScrollValue > 0.0f)) {
                            if(fScaledHeight < gpDisplaySize.y) {
                                //if the height of the image is less than the height of the screen,
                                //  center the image:
                                float fTotalMargin = gpDisplaySize.y - fScaledHeight;
                                values[Matrix.MTRANS_Y] = fTotalMargin / 2.0f;
                            } else {
                                //Stop the pan at the top edge of the screen.
                                values[Matrix.MTRANS_Y] = 0.0f;
                            }
                            matrix.setValues(values);
                        }

                        //Stop pan-up if at the bottom:
                        if((fImageEndTRANS_Y <= gpDisplaySize.y) && (fVerticalScrollValue < 0.0f)){
                            if(fScaledHeight < gpDisplaySize.y) {
                                //if the height of the image is less than the height of the screen,
                                //  center the image:
                                float fTotalMargin = gpDisplaySize.y - fScaledHeight;
                                values[Matrix.MTRANS_Y] = fTotalMargin / 2.0f;
                            } else {
                                //Set the y-coord so that the translated end of the image is at the bottom of the screen:
                                values[Matrix.MTRANS_Y] = gpDisplaySize.y - fScaledHeight;
                            }
                            matrix.setValues(values);
                        }


                        //Stop pan-right if left edge is at the left of the screen:
                        if((fImageX >= 0.0f) && (fHorizontalScrollValue > 0.0f)) {
                            if(fScaledWidth < gpDisplaySize.x) {
                                //if the width of the image is less than the width of the screen,
                                //  center the image:
                                float fTotalMargin = gpDisplaySize.x - fScaledWidth;
                                values[Matrix.MTRANS_X] = fTotalMargin / 2.0f;
                            } else {
                                //Stop the pan at the left edge of the screen.
                                values[Matrix.MTRANS_X] = 0.0f;
                            }
                            matrix.setValues(values);
                        }

                        //Stop pan-left if right edge is at the right of the screen:
                        if((fImageEndTRANS_X <= gpDisplaySize.x) && (fHorizontalScrollValue < 0.0f)){
                            if(fScaledWidth < gpDisplaySize.x) {
                                //if the width of the image is less than the width of the screen,
                                //  center the image:
                                float fTotalMargin = gpDisplaySize.x - fScaledWidth;
                                values[Matrix.MTRANS_X] = fTotalMargin / 2.0f;
                            } else {
                                //Set the x-coord so that the translated end of the image is at the end of the screen:
                                values[Matrix.MTRANS_X] = gpDisplaySize.x - fScaledWidth;
                            }
                            matrix.setValues(values);
                        }

                        // Perform the transformation
                        givImageViewer.setImageMatrix(matrix);
                    }

                    //========================================
                    //======  ZOOMING  =======================
                    //========================================
                    else if (mode == ZOOM) { //pinch zooming
                        iSwipeToExitCounter = 0; //Reset the swipe-to-exit counter.

                        //Determine the ZoomJumpOut direction:
                        boolean bJumpDirectionHorizontal = (gfScaleWidthMatch > gfScaleHeightMatch);

                        float fNewPinchDistance = spacing(event);

                        //Determine the new scale:
                        scale = fNewPinchDistance / gfPreviousPinchDistance;
                        float fScaleDistance = fNewPinchDistance - gfPreviousPinchDistance;

                        s = String.format("%3.3f\n", fScaleDistance);


                        //Define the center point about which the image zoom is translated:
                        float fMidPointX = gpMidPoint.x;
                        float fMidPointY = gpMidPoint.y;
                        float[] fImageMatrixValues = new float[9];
                        givImageViewer.getImageMatrix().getValues(fImageMatrixValues);
                        float fScaledWidth = fImageMatrixValues[Matrix.MSCALE_X] * giImageWidth;
                        if(fScaledWidth < gpDisplaySize.x) {
                            //if the width of the image is less than the width of the screen,
                            //  scale about the center of the screen:
                            fMidPointX = gpDisplaySize.x / 2.0f;
                        }
                        float fScaledHeight = fImageMatrixValues[Matrix.MSCALE_Y] * giImageHeight;
                        if(fScaledHeight < gpDisplaySize.y) {
                            //if the height of the image is less than the height of the screen,
                            //  scale about the center of the screen:
                            fMidPointY = gpDisplaySize.y / 2.0f;
                        }

                        //Translate the matrix to be provided to the ImageView:
                        matrix.postScale(scale, scale, fMidPointX, fMidPointY);

                        //Get the values from the matrix for evaluation:
                        matrix.getValues(values);

                        if(values[Matrix.MSCALE_X] < gfMinScale){ //If the scale is below the minimum, reset to the minimum:
                            values[Matrix.MSCALE_X] = gfMinScale;
                            values[Matrix.MSCALE_Y] = gfMinScale;
                            //Re-center the image on the screen:
                            values[Matrix.MTRANS_X] = gfImageViewOriginX;
                            values[Matrix.MTRANS_Y] = gfImageViewOriginY;
                            //Place the values in the matrix:
                            matrix.setValues(values);
                            //Reset/Allow the user to use the Jump-To-Zoom feature for zoom-out:
                            gbOkToZoomJumpOut = true;
                        } else if (values[Matrix.MSCALE_X] > globalClass.bCPV_MaxScale){ //If the scale is above the max, reset to max:
                            //Set the scale to max:
                            values[Matrix.MSCALE_X] = globalClass.bCPV_MaxScale;
                            values[Matrix.MSCALE_Y] = globalClass.bCPV_MaxScale;
                            //Restore the translated X & Y coordinates to the values array:
                            givImageViewer.getImageMatrix().getValues(fImageMatrixValues);
                            values[Matrix.MTRANS_X] = fImageMatrixValues[Matrix.MTRANS_X];
                            values[Matrix.MTRANS_Y] = fImageMatrixValues[Matrix.MTRANS_Y];
                            //Place the values in the matrix:
                            matrix.setValues(values);
                        } else if (globalClass.bCPV_AllowZoomJump) {
                            //If the settings are set to allow zoom jump...

                            float fCurrentMatrixScaleValue = values[Matrix.MSCALE_X]; //When we use values[Matrix.MSCALE_X], know that it is the same value as MSCALE_Y - we keep the aspect ratio.

                            if(fCurrentMatrixScaleValue > gfJumpOutAxisScale){
                                //If the user has zoomed-in, expanding the image beyond the jump
                                //  distance (outside the window), don't allow ZoomJumpIn when the
                                //  user zooms back out into within normal bounds. We already don't
                                //  jump back in while outside, but we also don't want a sudden jump
                                //  when the user zooms back out and hits the window edge threshold.
                                gbOkToZoomJumpIn = false;
                            }

                            if(gbOkToZoomJumpOut && (fScaleDistance > globalClass.fCPV_ZoomJumpOutThreshold) &&
                                    (fCurrentMatrixScaleValue < gfJumpOutAxisScale)) {

                                //  If the gbOkToZoomJumpOut flag is true,
                                //  and the user is zooming out (past a threshold value),
                                //  jump the zoom to the screen edges,
                                //  but not if the image is already zoomed outside the edges of the screen.

                                if( bJumpDirectionHorizontal) {
                                    //Set the translated x-value to the left edge of the screen:
                                    values[Matrix.MTRANS_X] = 0;
                                    //Retain the translated y-value of the image:
                                    givImageViewer.getImageMatrix().getValues(fImageMatrixValues);
                                    values[Matrix.MTRANS_Y] = fImageMatrixValues[Matrix.MTRANS_Y];
                                } else {
                                    //Set the translated y-value to the left edge of the screen:
                                    values[Matrix.MTRANS_Y] = 0;
                                    //Retain the translated x-value of the image:
                                    givImageViewer.getImageMatrix().getValues(fImageMatrixValues);
                                    values[Matrix.MTRANS_X] = fImageMatrixValues[Matrix.MTRANS_X];
                                }

                                //Set the zoom level to that required to match the width of the screen:
                                values[Matrix.MSCALE_X] = gfJumpOutAxisScale;
                                values[Matrix.MSCALE_Y] = gfJumpOutAxisScale;

                                //Place the values in the matrix:
                                matrix.setValues(values);

                                //Turn off jumpToZoomOut until the user zooms all the way back in:
                                gbOkToZoomJumpOut = false;

                                //Terminate the pinch processing by mimicking the
                                //  MotionEvent.ACTION_POINTER_UP case behaviors:
                                savedMatrix.set(matrix);
                                mode = NONE;
                            } else if(gbOkToZoomJumpIn
                                    && (fCurrentMatrixScaleValue < gfJumpOutAxisScale)
                                    && (fScaleDistance < globalClass.fCPV_ZoomJumpInThreshold)) {

                                //  If the gbOkToZoomJumpIn flag is true,
                                //  and the user is zooming in (past a threshold value),
                                //  jump the zoom to the minimum value and center the translation.

                                //Set the zoom level to that required to match the width of the screen:
                                values[Matrix.MSCALE_X] = gfMinScale;
                                values[Matrix.MSCALE_Y] = gfMinScale;
                                //Re-center the image on the screen:
                                values[Matrix.MTRANS_X] = gfImageViewOriginX;
                                values[Matrix.MTRANS_Y] = gfImageViewOriginY;

                                //Place the values in the matrix:
                                matrix.setValues(values);

                                //Terminate the pinch processing by mimicking the
                                //  MotionEvent.ACTION_POINTER_UP case behaviors:
                                savedMatrix.set(matrix);
                                mode = NONE;
                            }
                        }



                        //Check to see if the zoom has moved the matrix edge inside the screen
                        //  bounds while the opposite edge is outside the screen. If so, translate
                        //  the image so that the inside-edge is at the screen edge:
                        matrix.getValues(values);
                        float fImageX = values[Matrix.MTRANS_X]; //The left edge of the image, translated.
                        float fImageY = values[Matrix.MTRANS_Y]; //The top edge of the image, translated.
                        float fScaledImageWidth = values[Matrix.MSCALE_X] * giImageWidth;
                        float fScaledImageHeight = values[Matrix.MSCALE_Y] * giImageHeight;
                        float fImageEndTRANS_X = fImageX + fScaledImageWidth;
                        float fImageEndTRANS_Y = fImageY + fScaledImageHeight;


                        if(fScaledImageHeight > gpDisplaySize.y) { //If the image matrix height is bigger than the screen:

                            if (fImageEndTRANS_Y < gpDisplaySize.y) { //If the bottom of the image matrix is above the bottom of the screen:
                                //Translate the image matrix to put the bottom to the bottom of the screen:
                                values[Matrix.MTRANS_Y] = gpDisplaySize.y - fScaledImageHeight;
                                //Place the values in the matrix:
                                matrix.setValues(values);
                            } else if (fImageY > 0.0f) { //If the top of the image matrix is below the top of the screen:
                                //Translate the image matrix to put the top to the top of the screen:
                                values[Matrix.MTRANS_Y] = 0.0f;
                                //Place the values in the matrix:
                                matrix.setValues(values);
                            }

                        } else {
                            //If the image matrix height is smaller than height of the screen,
                            //  move the image to the horizontal mid-point of the screen.
                            //  This should only occur in odd scenarios in which the scaled width
                            //  of the image is the same as the width of the screen, but the scaled
                            //  height of the image is less than the height of the screen.
                            values[Matrix.MTRANS_Y] = (gpDisplaySize.y - fScaledImageHeight) / 2.0f;
                            //Place the values in the matrix:
                            matrix.setValues(values);
                        }

                        if(fScaledImageWidth > gpDisplaySize.x) { //If the image matrix width is bigger than the screen:

                            if (fImageEndTRANS_X < gpDisplaySize.x) { //If the end of the image matrix is inside the end of the screen:
                                //Translate the image matrix to put the end to the end of the screen:
                                values[Matrix.MTRANS_X] = gpDisplaySize.x - fScaledImageWidth;
                                //Place the values in the matrix:
                                matrix.setValues(values);
                            } else if (fImageX > 0.0f) { //If the start of the image matrix is inside the start of the screen:
                                //Translate the image matrix to put the start to the start of the screen:
                                values[Matrix.MTRANS_X] = 0.0f;
                                //Place the values in the matrix:
                                matrix.setValues(values);
                            }

                        } else {
                            //If the image matrix width is smaller than the width of the screen,
                            //  center the image on the screen and preserve the translated y-coord.
                            values[Matrix.MTRANS_X] = (gpDisplaySize.x - fScaledImageWidth) / 2.0f;
                            //Place the values in the matrix:
                            matrix.setValues(values);

                        }

                        //Track the current scale.
                        gfScaleFactor = values[Matrix.MSCALE_X];

                        // Perform the transformation
                        givImageViewer.setImageMatrix(matrix);

                    }

                    break;
            }

            if(giOnTouchProcessCount >= 10) {
                debugWriteLine(s);
                giOnTouchProcessCount = 0;

            }
            giOnTouchProcessCount++;

            return gestureDetector.onTouchEvent(event);
        }

        private void debugWriteLine(String s){
            int giDebugMaxLines = 30;
            if(giDebugLineCount >= giDebugMaxLines){
                gtvDebug.setText(s);
                giDebugLineCount = 0;
            } else {
                gtvDebug.append(s);
            }
            giDebugLineCount++;
        }

        private float spacing(MotionEvent event) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }

        private void midPoint(PointF point, MotionEvent event) {
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_THRESHOLD = 300;
            private static final int SWIPE_VELOCITY_THRESHOLD = 800; //pixels per second

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                //Go back a page, forward a page, or show the title and navigation bars,
                //  depending on the tap location.
                float fXMidPoint = gpDisplaySize.x / 2f;
                float fTouchDeadband = gpDisplaySize.x * .10f;
                float fNavigateBackTapXLocation = fXMidPoint - fTouchDeadband;
                float fNavigateNextTapXLocation = fXMidPoint + fTouchDeadband;
                float fTapXLocation = e.getRawX();
                if(fTapXLocation < fNavigateBackTapXLocation){
                    if(giCurrentCatalogItemIndex == 0){
                        if(iSwipeToExitCounter == 0) {
                            makeToast("Start of comic", Toast.LENGTH_SHORT);
                        } else if (iSwipeToExitCounter == 1){
                            //right/left - it feels like a "right-swipe" in the comic reference frame.
                            makeToast("Tap left again to exit", Toast.LENGTH_SHORT);
                        } else if (iSwipeToExitCounter == 2){
                            if(toastLastToastMessage != null){
                                toastLastToastMessage.cancel();
                            }
                            finish();
                        }
                        iSwipeToExitCounter++;
                    } else {
                        gotoPreviousComicPage();
                        iSwipeToExitCounter = 0;
                    }
                } else if (fTapXLocation > fNavigateNextTapXLocation){
                    if(giCurrentCatalogItemIndex == (giMaxFileCount - 1)){
                        if(iSwipeToExitCounter == 0) {
                            makeToast("End of comic", Toast.LENGTH_SHORT);
                        } else if (iSwipeToExitCounter == 1){
                            //right/left - it feels like a "right-swipe" in the comic reference frame.
                            makeToast("Tap right again to exit", Toast.LENGTH_SHORT);
                        } else if (iSwipeToExitCounter == 2){
                            if(toastLastToastMessage != null){
                                toastLastToastMessage.cancel();
                            }
                            finish();
                        }
                        iSwipeToExitCounter++;
                    } else {
                        gotoNextComicPage();
                        iSwipeToExitCounter = 0;
                    }
                } else {
                    toggle();
                    if (mVisible && AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                }


                if(gbDebugSwiping){
                    makeToast("Single Tap Detected", Toast.LENGTH_SHORT);
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if(gbDebugSwiping){
                    makeToast("Double Tap Detected", Toast.LENGTH_SHORT);
                }
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                //Detect swipe actions:
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight();
                            } else {
                                onSwipeLeft();
                            }
                            result = true;
                        }
                    } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeBottom(velocityY);
                        } else {
                            onSwipeTop();
                        }
                        result = true;
                    }

                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }
        }

        public void onSwipeRight() {}
        public void onSwipeLeft() {}
        public void onSwipeTop() {}
        public void onSwipeBottom(float velocityY) {

            //givImageViewer.animate().y(velocityY / 10).setDuration(1000);



        }
    }


    Toast toastLastToastMessage;
    private void makeToast(String sMessage, int iLength){
        if(toastLastToastMessage != null){
            toastLastToastMessage.cancel();
        }
        toastLastToastMessage = Toast.makeText(getApplicationContext(), sMessage, iLength);
        toastLastToastMessage.show();
    }


}