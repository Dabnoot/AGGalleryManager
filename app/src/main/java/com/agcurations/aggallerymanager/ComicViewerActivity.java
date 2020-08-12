package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Objects;
import java.util.TreeMap;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ComicViewerActivity extends AppCompatActivity {
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
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
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
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };


    //Global constants
    public static final String COMIC_FIELDS_STRING = "COMIC_FIELDS_STRING";
    public static final String SELECTED_COMIC_INDEX = "SELECTED_COMIC_INDEX";

    //Global variables
    public TreeMap<Integer, String> tmComicPages;
    public int giCurrentPageIndex;
    public boolean gbDebugSwiping = false;
    public GlobalClass globalClass;

    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;

    private String gsComicName = "";

    ImageView givComicPage;

    private int giSelectedComicSequenceNum;

    //Movement variables
    private float mLastTouchX = 0.0f;
    private float mLastTouchY = 0.0f;
    private float mPosX = 0.0f;
    private float mPosY = 0.0f;




    private static final String TAG = "Touch";

    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 5.0f;

    // These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;



    TextView gtvDebug;
    int giDebugLineCount;
    int giDebugMaxLines = 30;







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_viewer);

        globalClass = (GlobalClass) getApplicationContext();

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        gtvDebug = findViewById(R.id.textViewDebug);





        //Get the intent used to start this activity:
        Intent intentCaller = getIntent();

        //Get an integer representing which ImageView was selected on the caller.
        giSelectedComicSequenceNum = intentCaller.getIntExtra(SELECTED_COMIC_INDEX,-1);

        //Get data related to the selected comic:
        TreeMap<Integer, String[]> tmCatalogComicList;
        tmCatalogComicList = globalClass.getCatalogComicList();
        String[] sComicFields = tmCatalogComicList.get(giSelectedComicSequenceNum);
        if( sComicFields == null) return;
        gsComicName = sComicFields[GlobalClass.COMIC_NAME_INDEX];


        String sComicFolder_AbsolutePath = globalClass.getCatalogComicsFolder().getAbsolutePath();
        String sComicFolderPath;
        sComicFolderPath = sComicFolder_AbsolutePath + File.separator
                + sComicFields[GlobalClass.COMIC_FOLDER_NAME_INDEX];

        //Load the full path to each comic pate into tmComicPages:
        File fComicFolder = new File(sComicFolderPath);
        tmComicPages = new TreeMap<>();
        if(fComicFolder.exists()){
            File[] fComicPages = fComicFolder.listFiles();
            if(fComicPages != null) {
                for (int i = 0; i < fComicPages.length; i++) {
                    tmComicPages.put(i, fComicPages[i].getAbsolutePath());
                }
            }
        }

        //Post a runnable from onCreate(), that will be executed when the view has been created, in
        //  order to load the first comic page:
        mContentView.post(new Runnable(){
            public void run(){
                //https://stackoverflow.com/questions/12829653/content-view-width-and-height/21426049
                giCurrentPageIndex = 0;
                LoadComicPage(giCurrentPageIndex);
            }
        });

        // Set up the user interaction to manually show or hide the system UI.
        /*mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });*/

        mContentView.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeTop() {
                if(gbDebugSwiping) Toast.makeText(getApplicationContext(), "Swiped top", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeRight() {
                if(gbDebugSwiping) Toast.makeText(getApplicationContext(), "Swiped right", Toast.LENGTH_SHORT).show();
                if(mScaleFactor == 1.0f) gotoPreviousComicPage();
            }

            public void onSwipeLeft() {
                if(gbDebugSwiping) Toast.makeText(getApplicationContext(), "Swiped left", Toast.LENGTH_SHORT).show();
                if(mScaleFactor == 1.0f) gotoNextComicPage();
            }

            public void onSwipeBottom() {
                if(gbDebugSwiping) Toast.makeText(getApplicationContext(), "Swiped bottom", Toast.LENGTH_SHORT).show();
            }

        });


        givComicPage = findViewById(R.id.imageView_ComicPage);
        givComicPage.setScaleType(ImageView.ScaleType.MATRIX);
        //givComicPage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());


        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);



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
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
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

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    private void LoadComicPage(int iPageIndex){

        if(tmComicPages.containsKey(iPageIndex)){
            if(tmComicPages.get(iPageIndex) == null){
                return;
            }
        } else {
            return;
        }

        File fComicPage = new File(Objects.requireNonNull(tmComicPages.get(iPageIndex)));


        if (fComicPage.exists()) {
            //https://developer.android.com/topic/performance/graphics/load-bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fComicPage.getAbsolutePath(), options);

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
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //!!!!!   REFACTOR USING createImageThumbnail ONCE API LEVEL 29 IS COMMON    !!!!!!!
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            Bitmap myBitmap;

            if(globalClass.ObfuscationOn) {

                //Get the obfuscation image index:
                int i = (giSelectedComicSequenceNum % globalClass.getObfuscationImageCount());
                //Get the obfuscation image resource ID:
                int iObfuscatorResourceID = globalClass.getObfuscationImage(i);
                myBitmap = BitmapFactory.decodeResource(getResources(), iObfuscatorResourceID);

                //Set the title of the activity:
                setTitle(globalClass.getObfuscationCategoryName());
            } else {
                myBitmap = BitmapFactory.decodeFile(fComicPage.getAbsolutePath(), options);
                setTitle(gsComicName);
            }

            givComicPage.setImageBitmap(myBitmap);
            int h1 = givComicPage.getHeight();
            int h2 = givComicPage.getMaxHeight();
            CenterComicPage();
        }
    }

    private void CenterComicPage(){
        Drawable d = givComicPage.getDrawable();
        // TODO: check that d isn't null
        //Resolution of SM-T510 1920 x 1200 (WUXGA)
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        RectF imageRectF = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        RectF viewRectF = new RectF(0, 0, givComicPage.getWidth(), displayMetrics.heightPixels);
        matrix.setRectToRect(imageRectF, viewRectF, Matrix.ScaleToFit.CENTER);

        givComicPage.setImageMatrix(matrix);
    }

    private void gotoNextComicPage(){
        if(giCurrentPageIndex < tmComicPages.size()){
            giCurrentPageIndex++;
            LoadComicPage(giCurrentPageIndex);
        }
    }

    private void gotoPreviousComicPage(){
        if(giCurrentPageIndex > 0){
            giCurrentPageIndex--;
            LoadComicPage(giCurrentPageIndex);
        }
    }

    class OnSwipeTouchListener implements View.OnTouchListener {
        //https://www.journaldev.com/28900/android-gesture-detectors
        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context ctx) {
            gestureDetector = new GestureDetector(ctx, new GestureListener());
        }



        // The â€˜active pointerâ€™ is the one currently moving our object.
        private int INVALID_POINTER_ID = 0;
        private int mActivePointerId = INVALID_POINTER_ID;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            v.performClick();
            mScaleGestureDetector.onTouchEvent(event);






            //Code to process movement of the image:
            if(mScaleFactor > 1.0f) {
                //    if(false){






                /*final int action = event.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        final int pointerIndex = event.getActionIndex();
                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);

                        // Remember where we started (for dragging)
                        mLastTouchX = x;
                        mLastTouchY = y;
                        // Save the ID of this pointer (for dragging)
                        mActivePointerId = event.getPointerId(0);
                        break;
                    }

                    case MotionEvent.ACTION_MOVE: {
                        // Find the index of the active pointer and fetch its position
                        final int pointerIndex =
                                event.findPointerIndex(mActivePointerId);

                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);

                        // Calculate the distance moved
                        final float dx = x - mLastTouchX;
                        final float dy = y - mLastTouchY;

                        mPosX += dx;
                        mPosY += dy;

                        v.invalidate();

                        // Remember this touch position for the next move event
                        mLastTouchX = x;
                        mLastTouchY = y;

                        break;
                    }

                    case MotionEvent.ACTION_UP: {
                        mActivePointerId = INVALID_POINTER_ID;
                        break;
                    }

                    case MotionEvent.ACTION_CANCEL: {
                        mActivePointerId = INVALID_POINTER_ID;
                        break;
                    }

                    case MotionEvent.ACTION_POINTER_UP: {

                        final int pointerIndex = event.getActionIndex();
                        final int pointerId = event.getPointerId(pointerIndex);

                        if (pointerId == mActivePointerId) {
                            // This was our active pointer going up. Choose a new
                            // active pointer and adjust accordingly.
                            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                            mLastTouchX = event.getX(newPointerIndex);
                            mLastTouchY = event.getY(newPointerIndex);
                            mActivePointerId = event.getPointerId(newPointerIndex);
                        }
                        break;
                    }
                }*/


                //https://www.semicolonworld.com/question/48318/android-imageview-setting-drag-and-pinch-zoom-parameters
                float scale = 0.0f;
                String s="";
                // Handle touch events here...
                switch (event.getAction() & MotionEvent.ACTION_MASK) {

                    case MotionEvent.ACTION_DOWN: //first finger down only
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        //Log.d(TAG, "mode=DRAG" );
                        mode = DRAG;
                        break;
                    case MotionEvent.ACTION_UP: //first finger lifted
                    case MotionEvent.ACTION_POINTER_UP: //second finger lifted
                        mode = NONE;
                        //Log.d(TAG, "mode=NONE" );
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN: //second finger down
                        oldDist = spacing(event);
                        //Log.d(TAG, "oldDist=" + oldDist);
                        if (oldDist > 5f) {
                            savedMatrix.set(matrix);
                            midPoint(mid, event);
                            mode = ZOOM;
                            //Log.d(TAG, "mode=ZOOM" );
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:

                        if (mode == DRAG) { //movement of first finger

                            matrix.set(savedMatrix);
                            if (givComicPage.getLeft() >= -392){
                                matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                                s = "DRAG: NewX: " + event.getX() + ". OldX: " + start.x + ". NewY: " + event.getY() + ". OldY: " + start.y + "\n";
                            }

                        }
                        else if (mode == ZOOM) { //pinch zooming
                            float newDist = spacing(event);
                            //Log.d(TAG, "newDist=" + newDist);



                            if (newDist < 5f) {
                                matrix.set(savedMatrix);
                                scale = newDist / oldDist; //thinking i need to play around with this value to limit it**
                                matrix.postScale(scale, scale, mid.x, mid.y);
                            }
                            s = "ZOOM: newDist=" + newDist + ". oldDist: " + oldDist + ". Scale: " + scale + "\n";

                        }
                        //debugWriteLine(s);
                        break;
                }

                // Perform the transformation
                givComicPage.setImageMatrix(matrix);

                return true; // indicate event was handled

            } else {
                CenterComicPage();
            }


            return gestureDetector.onTouchEvent(event);
        }

        private void debugWriteLine(String s){
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
            private static final int SWIPE_VELOCITY_THRESHOLD = 300;

            @Override
            public boolean onDown(MotionEvent e) {

                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggle();
                if(gbDebugSwiping) Toast.makeText(getApplicationContext(), "Single Tap Detected", Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Obfuscate();
                if(gbDebugSwiping) Toast.makeText(getApplicationContext(), "Long Press Detected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if(gbDebugSwiping) Toast.makeText(getApplicationContext(), "Double Tap Detected", Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
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
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                        result = true;
                    }
                    if(mScaleFactor > 1.0f) {
                        //If we are zoomed-in, pan the image:
                        givComicPage.animate()
                                .x(e1.getX() + diffX)
                                .y(e1.getY() + diffY)
                                .setDuration(500)
                                .start();
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }
        }

        public void onSwipeRight() {
        }

        public void onSwipeLeft() {
        }

        public void onSwipeTop() {
        }

        public void onSwipeBottom() {
        }


    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        //https://medium.com/quick-code/pinch-to-zoom-with-multi-touch-gestures-in-android-d6392e4bf52d
        //https://stackoverflow.com/questions/10225851/how-do-i-define-max-and-min-zooming-lvl-when-scaling-imageview-by-matrix
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector){
            //updating the scalefactor after the current gesture
            mScaleFactor *= scaleGestureDetector.getScaleFactor();

            //making sure the scale is within the limits
            float gfMaxZoom = 3.0f;
            float gfMinZoom = 1.0f;
            mScaleFactor = Math.max(gfMinZoom, Math.min(mScaleFactor, gfMaxZoom));


            givComicPage.setScaleX(mScaleFactor);
            givComicPage.setScaleY(mScaleFactor);
            return true;
        }
    }


    //=====================================================================================
    //===== Obfuscation Code =================================================================
    //=====================================================================================

    public void FlipObfuscation() {
        globalClass.ObfuscationOn = !globalClass.ObfuscationOn;
        //LoadComicPage will automatically load the obfuscated/non-obfuscated image as required:
        LoadComicPage(giCurrentPageIndex);
    }

    public void Obfuscate() {
        //This routine is separate because it can be activated
        // by either a long-press or the toggle option on the menu.
        if(!globalClass.ObfuscationOn) {
            globalClass.ObfuscationOn = true;
            LoadComicPage(giCurrentPageIndex);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comic_viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Display a message showing the name of the item selected.
        //Toast.makeText(this, "Selected Item: " +item.getTitle(), Toast.LENGTH_SHORT).show();
        if (item.getItemId() == R.id.menu_FlipView) {
            FlipObfuscation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



}