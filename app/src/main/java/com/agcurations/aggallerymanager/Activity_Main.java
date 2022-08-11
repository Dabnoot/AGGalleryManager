
package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Activity_Main extends AppCompatActivity {

    //Global Variables:
    GlobalClass globalClass;

    MainActivityDataServiceResponseReceiver mainActivityDataServiceResponseReceiver;

    ProgressBar progressBar_WorkerTest;
    TextView textView_WorkerTest;
    Observer<WorkInfo> workInfoObserver_TrackingTest;

    ProgressBar gProgressBar_CatalogReadProgress;
    TextView gTextView_CatalogReadProgressBarText;

    boolean gbDataLoadComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.MainTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().show();

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        gProgressBar_CatalogReadProgress = findViewById(R.id.progressBar_CatalogReadProgress);
        gTextView_CatalogReadProgressBarText = findViewById(R.id.textView_CatalogReadProgressBarText);

        //Configure a response receiver to listen for updates from the Main Activity (MA) Data Service:
        //  This will load the tags files for videos, pictures, and comics.
        IntentFilter filter = new IntentFilter(MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        mainActivityDataServiceResponseReceiver = new MainActivityDataServiceResponseReceiver();
        //registerReceiver(mainActivityDataServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mainActivityDataServiceResponseReceiver, filter);
        //Call the MA Data Service, which will create a call to a service:
        Service_Main.startActionLoadData(this);


        //AlertDialogTest2();

        //Put together a progressbar to indicate progress of a worker (originally incorporated to track video concatenation):
        progressBar_WorkerTest = findViewById(R.id.progressBar_WorkerTest);
        progressBar_WorkerTest.setMax(100);
        textView_WorkerTest = findViewById(R.id.textView_WorkerTest);


        ImageButton imageButton_DayNight = findViewById(R.id.imageButton_DayNight);
        imageButton_DayNight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!globalClass.gbIsDarkModeOn) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    globalClass.gbIsDarkModeOn = true;
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    globalClass.gbIsDarkModeOn = false;
                }
            }
        });

        ImageButton imageButton_Browser = findViewById(R.id.imageButton_Browser);
        imageButton_Browser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentBrowser = new Intent(getApplicationContext(), Activity_Browser.class);
                startActivity(intentBrowser);
            }
        });

    }

    private void AlertDialogTest2(){
        //Testing of AlertDialog style:
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
                    Toast.makeText(getApplicationContext(), "Correct pin entered.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                }

                adConfirmationDialog.dismiss();
            }
        });

        adConfirmationDialog.show();
    }

    private void AlertDialogTest1(){
        //Testing of AlertDialog style:
        String sConfirmationMessage = "Confirm item: Test test test test test test test";
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustomStyle);
        builder.setTitle("Delete Tag");
        builder.setMessage(sConfirmationMessage);
        // Set up the input
        final EditText editText_DialogInput = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        editText_DialogInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        builder.setView(editText_DialogInput);
        //builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog adConfirmationDialog = builder.create();
        adConfirmationDialog.show();
    }





    public class MainActivityDataServiceResponseReceiver extends BroadcastReceiver {
        //MADataService = Main Activity Data Service
        public static final String MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_MAIN_ACTIVITY_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {
                String sStatusMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_STATUS_MESSAGE);
                if(sStatusMessage != null){
                    Toast.makeText(context, sStatusMessage, Toast.LENGTH_LONG).show();
                }
            }

            //Check to see if this is a response to update log or progress bar:
            boolean 	bUpdatePercentComplete;
            boolean 	bUpdateProgressBarText;

            //Get booleans from the intent telling us what to update:
            bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
            bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

            if(gProgressBar_CatalogReadProgress != null && gTextView_CatalogReadProgressBarText != null) {
                if (bUpdatePercentComplete) {
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if (gProgressBar_CatalogReadProgress != null) {
                        gProgressBar_CatalogReadProgress.setProgress(iAmountComplete);
                    }
                    if (iAmountComplete == 100) {
                        gProgressBar_CatalogReadProgress.setVisibility(View.INVISIBLE);
                        gTextView_CatalogReadProgressBarText.setVisibility(View.INVISIBLE);
                        gbDataLoadComplete = true;
                    } else {
                        gProgressBar_CatalogReadProgress.setVisibility(View.VISIBLE);
                        gTextView_CatalogReadProgressBarText.setVisibility(View.VISIBLE);
                    }

                }
                if (bUpdateProgressBarText) {
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if (gTextView_CatalogReadProgressBarText != null) {
                        gTextView_CatalogReadProgressBarText.setText(sProgressBarText);
                    }
                }
            }

        }
    }

    @Override
    protected void onDestroy() {
        //unregisterReceiver(mainActivityDataServiceResponseReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mainActivityDataServiceResponseReceiver);
        super.onDestroy();
    }

    //=====================================================================================
    //===== Menu Code =================================================================
    //=====================================================================================

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //Configure the AlertDialog that will gather the pin code if necessary to begina particular behavior:
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


        if(item.getItemId() == R.id.menu_import) {
            Intent intentImportGuided = new Intent(this, Activity_Import.class);
            startActivity(intentImportGuided);
            return true;
        } else  if(item.getItemId() == R.id.menu_Settings) {
            //Ask for pin code in order to allow access to Settings:

            button_PinCodeCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adConfirmationDialog.dismiss();
                }
            });

            //Code action for the OK button:
            button_PinCodeOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                    String sPinEntered = editText_DialogInput.getText().toString();

                    if (sPinEntered.equals(globalClass.gsPin)) {
                        Intent intentSettings = new Intent(getApplicationContext(), Activity_AppSettings.class);
                        startActivity(intentSettings);
                    } else {
                        Toast.makeText(getApplicationContext(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                    }

                    adConfirmationDialog.dismiss();
                }
            });

            adConfirmationDialog.show();

            return true;
        } else if(item.getItemId() == R.id.menu_TagEditor) {

            //Ask for pin code in order to allow access to the Tag Editor:

            button_PinCodeOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                    String sPinEntered = editText_DialogInput.getText().toString();

                    if (sPinEntered.equals(globalClass.gsPin)) {
                        Intent intentTagEditor = new Intent(getApplicationContext(), Activity_TagEditor.class);
                        startActivity(intentTagEditor);
                    } else {
                        Toast.makeText(getApplicationContext(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                    }

                    adConfirmationDialog.dismiss();
                }
            });

            adConfirmationDialog.show();

            return true;
        } else if(item.getItemId() == R.id.menu_DatabaseBackup) {

            //Backup the database files (CatalogContents.dat):
            Service_Main.startActionCatalogBackup(this);

            return true;
        } else if(item.getItemId() == R.id.menu_WorkerConsole){
            Intent intentWorkerConsoleActivity = new Intent(this, Activity_WorkerConsole.class);
            startActivity(intentWorkerConsoleActivity);
            return true;
        } else if(item.getItemId() == R.id.menu_LogViewer){
            Intent intentLogViewerActivity = new Intent(this, Activity_LogViewer.class);
            startActivity(intentLogViewerActivity);
            return true;
        } else if(item.getItemId() == R.id.menu_About) {


            return true;
        } else if(item.getItemId() == R.id.menu_Test) {

            //Testing WorkManager for video concatenation:
            //https://developer.android.com/topic/libraries/architecture/workmanager/advanced
            String sJobDateTime = GlobalClass.GetTimeStampFileSafe();
            Data dataTrackingTest = new Data.Builder()
                    .putString(Worker_TrackingTest.KEY_ARG_JOB_REQUEST_DATETIME, sJobDateTime)
                    .build();
            OneTimeWorkRequest otwrWorkerTrackingTest = new OneTimeWorkRequest.Builder(Worker_TrackingTest.class)
                    .setInputData(dataTrackingTest)
                    .addTag(Worker_TrackingTest.WORKER_TRACKING_TEST_TAG) //To allow finding the worker later.
                    .build();
            UUID UUIDWorkID = otwrWorkerTrackingTest.getId();
            WorkManager.getInstance(getApplicationContext()).enqueue(otwrWorkerTrackingTest);

            //Next: configure worker to write progressbar on ActivityMain.

            WorkManager wm = WorkManager.getInstance(getApplicationContext());
            LiveData<WorkInfo> ldWorkInfo = wm.getWorkInfoByIdLiveData(UUIDWorkID);
            ldWorkInfo.observe(this, workInfoObserver_TrackingTest);

            Toast.makeText(getApplicationContext(), "Worker started with job datetime " + sJobDateTime, Toast.LENGTH_SHORT).show();

            //Toast.makeText(getApplicationContext(), "No developer test item configured.", Toast.LENGTH_SHORT).show();

            return true; //End Test Options item.
        } else {
            return super.onOptionsItemSelected(item);
        }


    }


    //=====================================================================================
    //===== Obfuscation Code =================================================================
    //=====================================================================================

    @Override
    public void onResume(){
        super.onResume();

        //Create a generic observer to be assigned to any active video concatenation workers (shows the progress of the worker):
        workInfoObserver_TrackingTest = new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null) {
                    Data progress = workInfo.getProgress();
                    long lProgressNumerator = progress.getLong(Worker_TrackingTest.WORKER_BYTES_PROCESSED, 0);
                    long lProgressDenominator = progress.getLong(Worker_TrackingTest.WORKER_BYTES_TOTAL, 100);
                    int iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    String sWorkerID = progress.getString(Worker_TrackingTest.WORKER_ID);

                    if(progressBar_WorkerTest != null && textView_WorkerTest != null)
                        if (workInfo.getState() == WorkInfo.State.RUNNING) {
                            progressBar_WorkerTest.setVisibility(View.VISIBLE);
                            progressBar_WorkerTest.setProgress(iProgressBarValue);
                            if (sWorkerID != null) {
                                textView_WorkerTest.setVisibility(View.VISIBLE);
                                textView_WorkerTest.setText(sWorkerID);
                            }
                        } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            progressBar_WorkerTest.setVisibility(View.INVISIBLE);
                            progressBar_WorkerTest.setProgress(0);
                            textView_WorkerTest.setVisibility(View.INVISIBLE);
                            textView_WorkerTest.setText("");
                        }
                }
            }
        };

        //Look to see if there are any workers out there processing data for AGGalleryManager,
        //  and if so, attempt to listen to their progress:
        ListenableFuture<List<WorkInfo>> lfListWorkInfo = WorkManager.getInstance(getApplicationContext()).getWorkInfosByTag(Worker_TrackingTest.WORKER_TRACKING_TEST_TAG);
        try {
            int iWorkerCount = lfListWorkInfo.get().size();
            for(int i = 0; i < iWorkerCount; i++) {
                WorkInfo.State stateWorkState = lfListWorkInfo.get().get(i).getState();
                UUID UUIDWorkID = lfListWorkInfo.get().get(i).getId();
                Log.d("Workstate", stateWorkState.toString() + ", ID " + UUIDWorkID.toString());
                if(stateWorkState == WorkInfo.State.RUNNING || stateWorkState == WorkInfo.State.ENQUEUED) {

                    WorkManager wm = WorkManager.getInstance(getApplicationContext());
                    LiveData<WorkInfo> ldWorkInfo = wm.getWorkInfoByIdLiveData(UUIDWorkID);
                    ldWorkInfo.observe(this, workInfoObserver_TrackingTest);
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }






    }

    //=====================================================================================
    //===== ImageView Click Code =================================================================
    //=====================================================================================

    public void startVideoCatalogActivity(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_VIDEOS);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        startActivity(intentCatalogActivity);
    }

    public void startPicturesCatalogActivity(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_IMAGES);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        startActivity(intentCatalogActivity);
    }

    public void startComicsCatalogActivity(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        /*Intent intentComicsCatalogActivity = new Intent(this, ComicsCatalogActivity.class);
        startActivity(intentComicsCatalogActivity);*/
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_COMICS);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        startActivity(intentCatalogActivity);
    }

    public void startImportVideos(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_VIDEOS); //todo: Redundant
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        startActivity(intentImportGuided);
    }

    public void startImportImages(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_IMAGES); //todo: Redundant
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        startActivity(intentImportGuided);
    }

    public void startImportComics(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_COMICS); //todo: Redundant
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        startActivity(intentImportGuided);
    }


    public void buttonTestClick_Test(View v){

    }


}