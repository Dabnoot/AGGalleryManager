
package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Activity_Main extends AppCompatActivity {

    //Global Variables:
    GlobalClass globalClass;

    MainActivityDataServiceResponseReceiver mainActivityDataServiceResponseReceiver;

    ProgressBar progressBar_WorkerTest;
    TextView textView_WorkerTest;
    Observer<WorkInfo> workInfoObserver_VideoConcatenator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.MainTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        if(!globalClass.ObfuscationOn) {
            //Remove obfuscation:
            RemoveObfuscation();
        }

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

        /*//Create a generic observer to be assigned to any active video concatenation workers (shows the progress of the worker):
        workInfoObserver_VideoConcatenator = new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null) {
                    Data progress = workInfo.getProgress();
                    int value = progress.getInt(Worker_VideoPostProcessing.PROGRESS, 0);
                    String sFileName = progress.getString(Worker_VideoPostProcessing.FILENAME);

                    if(progressBar_WorkerTest != null && textView_WorkerTest != null)
                    if (workInfo.getState() == WorkInfo.State.RUNNING) {
                        progressBar_WorkerTest.setVisibility(View.VISIBLE);
                        progressBar_WorkerTest.setProgress(value);
                        if (sFileName != null) {
                            textView_WorkerTest.setVisibility(View.VISIBLE);
                            textView_WorkerTest.setText(sFileName);
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
        ListenableFuture<List<WorkInfo>> lfListWorkInfo = WorkManager.getInstance(getApplicationContext()).getWorkInfosByTag(Worker_VideoPostProcessing.WORKER_VIDEO_POST_PROCESSING_TAG);
        try {
            int iWorkerCount = lfListWorkInfo.get().size();
            for(int i = 0; i < iWorkerCount; i++) {
                WorkInfo.State stateWorkState = lfListWorkInfo.get().get(i).getState();
                UUID UUIDWorkID = lfListWorkInfo.get().get(i).getId();
                Log.d("Workstate", stateWorkState.toString() + ", ID " + UUIDWorkID.toString());
                if(stateWorkState == WorkInfo.State.RUNNING || stateWorkState == WorkInfo.State.ENQUEUED) {

                    WorkManager wm = WorkManager.getInstance(getApplicationContext());
                    LiveData<WorkInfo> ldWorkInfo = wm.getWorkInfoByIdLiveData(UUIDWorkID);
                    ldWorkInfo.observe(this, workInfoObserver_VideoConcatenator);
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }*/





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





    public static class MainActivityDataServiceResponseReceiver extends BroadcastReceiver {
        //MADataService = Main Activity Data Service
        public static final String MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_MAIN_ACTIVITY_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_Main.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_Main.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {
                String sStatusMessage = intent.getStringExtra(Service_Main.EXTRA_STRING_STATUS_MESSAGE);
                if(sStatusMessage != null){
                    Toast.makeText(context, sStatusMessage, Toast.LENGTH_LONG).show();
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


        if(item.getItemId() == R.id.menu_FlipView) {
            FlipObfuscation();
            return true;
        } else if(item.getItemId() == R.id.menu_import) {
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
        } else if(item.getItemId() == R.id.menu_About) {


            return true;
        } else if(item.getItemId() == R.id.menu_Test) {

            //Testing WorkManager:
            //WorkManager workManager = WorkManager.getInstance(getApplicationContext());
            //workManager.enqueue(OneTimeWorkRequest.from(Worker_FileDownload.class));


            //Testing DownloadManager:
            //Use the download manager to download the file:
            /*String sFolderName = "0000";
            String sShortPath = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getPath();
            sShortPath = sShortPath + File.separator + sFolderName;
            String sTestURL = "https://thumbs.dreamstime.com/z/tv-test-image-card-rainbow-multi-color-bars-geometric-signals-retro-hardware-s-minimal-pop-art-print-suitable-89603663.jpg";
            String sTestFileName = "1000.gpj";
            String sFullPathFileName = sShortPath + File.separator + sTestFileName;
            File fTestFile = new File(sFullPathFileName);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sTestURL));
            request.setTitle("AG Gallery+ File Download")
                    .setDescription("Download test file")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                    //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                    .setMimeType("application/octet-stream")
                    .setDestinationUri(Uri.fromFile(fTestFile));

            // get download service and enqueue file
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);*/

            //Try to view status of downloads in the download manager:
            //http://android-er.blogspot.com/2011/07/check-downloadmanager-status-and-reason.html
            /*DownloadManager dm;
            dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query();

            Cursor c = dm.query(query);
            int iLoopCount = 0;
            if(c.moveToFirst()) {
                do {
                    int columnIndex_Status = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    String sTimeStampms = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP));

                    String sStatus = "Status: unknown. ";
                    switch (c.getInt((columnIndex_Status))) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            sStatus = "Status: successful. ";
                            break;
                        case DownloadManager.STATUS_FAILED:
                            sStatus = "Status: failed. ";
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            sStatus = "Status: failed. ";
                            break;
                        case DownloadManager.STATUS_PENDING:
                            sStatus = "Status: failed. ";
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            sStatus = "Status: failed. ";
                            break;

                    }
                    String sTimeStamp = getDate(Long.decode(sTimeStampms), "yyyy/MM/dd hh:mm:ss.SSS");
                    iLoopCount++;
                    String sMessage = iLoopCount + ", " + sStatus + sTimeStamp + ", " + uriString;
                    Log.d("Download Status Analysis", sMessage);

                    if(iLoopCount % 100 == 0){
                        Log.d("Download Status Analysis", "Entry count: " + iLoopCount);
                    }
                } while (c.moveToNext());
            }*/

            /*//Testing WorkManager for video concatenation:
            //https://developer.android.com/topic/libraries/architecture/workmanager/advanced
            Data dataVideoConcatenator = new Data.Builder()
                    .putString(Worker_VideoPostProcessing.KEY_ARG_VIDEO_SEGMENT_FOLDER, "Test")
                    .putString(Worker_VideoPostProcessing.KEY_ARG_VIDEO_OUTPUT_FILENAME, "TestOutputConcatenation.mpeg")
                    .build();
            OneTimeWorkRequest otwrVideoConcatenation = new OneTimeWorkRequest.Builder(Worker_VideoPostProcessing.class)
                    .setInputData(dataVideoConcatenator)
                    .addTag(Worker_VideoPostProcessing.WORKER_VIDEOCONCATENATOR_TAG) //To allow finding the worker later.
                    .build();
            UUID UUIDWorkID = otwrVideoConcatenation.getId();
            WorkManager.getInstance(getApplicationContext()).enqueue(otwrVideoConcatenation);

            //Next: configure worker to write progressbar on ActivityMain.

            WorkManager wm = WorkManager.getInstance(getApplicationContext());
            LiveData<WorkInfo> ldWorkInfo = wm.getWorkInfoByIdLiveData(UUIDWorkID);
            ldWorkInfo.observe(this, workInfoObserver_VideoConcatenator);*/



            String sFileName = /*GlobalClass.GetTimeStampFileSafe() +*/ "_1667_hls-720p-02c5c0_0.ts";
            /*DownloadManager downloadManager = null;
            downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse("https://hls-hw.xnxx-cdn.com/videos/hls/0e/01/24/0e0124dc524ebea496445e40d41288a9/hls-720p-02c5c0.ts?e=1624738656&l=0&h=e2636fe7b579aabaedfe4b411cea611b"));
            request.setTitle("AG Gallery+ File Download: " + "Video Download Test")
                    .setDescription("Video Download Test")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    //.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                    //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                    .setMimeType("application/octet-stream")
                    .setDestinationInExternalFilesDir(getApplicationContext(), "Videos", sFileName);
            downloadManager.enqueue(request);*/

            /*File[] fExternalFilesDirs = getExternalFilesDirs(null);
            String sSource = fExternalFilesDirs[0].getAbsolutePath() + File.separator +
                    "Videos" + File.separator + sFileName;
            String sDestination = fExternalFilesDirs[1].getAbsolutePath() + File.separator +
                    "Videos" + File.separator + sFileName;
            File fSource = new File(sSource);
            File fDestination = new File(sDestination);*/
            /*if(fSource.exists()) {
                if (!fSource.renameTo(fDestination)) {
                    Toast.makeText(getApplicationContext(), "File move unsuccessful.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "File does not exist.", Toast.LENGTH_SHORT).show();
            }*/

            /*if(fSource.exists() && !fDestination.exists()) {
                try {
                    InputStream inputStream;
                    OutputStream outputStream;
                    inputStream = new FileInputStream(fSource.getPath());
                    outputStream = new FileOutputStream(fDestination.getPath());
                    byte[] buffer = new byte[100000];
                    while ((inputStream.read(buffer, 0, buffer.length)) >= 0) {
                        outputStream.write(buffer, 0, buffer.length);
                    }
                    outputStream.flush();
                    outputStream.close();

                    if (!fSource.delete()) {
                        Toast.makeText(getApplicationContext(), "Could not delete source file after copy.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }*/

            /*if(fDestination.exists()){
                if (!fDestination.delete()) {
                    Toast.makeText(getApplicationContext(), "Could not delete destination file after test.", Toast.LENGTH_SHORT).show();
                }
            }*/

            String sPath = "/storage/3966-3438/Android/data/com.agcurations.aggallerymanager/files/Videos/148/1668/Output/FFMPEGLog.txt";
            File fFile = new File(sPath);
            if(fFile.exists()) {
                if (!fFile.delete()) {
                    Toast.makeText(getApplicationContext(), "Could not delete file after test.", Toast.LENGTH_SHORT).show();
                }
            }








            Toast.makeText(getApplicationContext(), "No developer test item configured.", Toast.LENGTH_SHORT).show();

            return true; //End Test Options item.
        } else {
            return super.onOptionsItemSelected(item);
        }


    }



    /**
     * Return date in specified format.
     * @param milliSeconds Date in milliseconds
     * @param dateFormat Date format
     * @return String representing date in specified format
     */
    public static String getDate(long milliSeconds, String dateFormat) {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }


    //=====================================================================================
    //===== Obfuscation Code =================================================================
    //=====================================================================================

    @Override
    public void onResume(){
        super.onResume();
        if(globalClass.ObfuscationOn) {
            //Obfuscate data:
            Obfuscate();
        } else {
            //Remove obfuscation:
            RemoveObfuscation();
        }
    }

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

        ImageView ivVideos = findViewById(R.id.imageView_Video);
        ImageView ivPictures = findViewById(R.id.imageView_Images);
        ImageView ivComics = findViewById(R.id.imageView_Comics);

        ivVideos.setImageResource(R.drawable.ob_productivity_tools);
        ivPictures.setImageResource(R.drawable.ob_data_management);
        ivComics.setImageResource(R.drawable.ob_quality_analysis);

        TextView tvVideos = findViewById(R.id.textView_Videos);
        TextView tvPictures = findViewById(R.id.textView_Pictures);
        TextView tvComics = findViewById(R.id.textView_Comics);

        tvVideos.setText(R.string.productivity_tools);
        tvPictures.setText(R.string.data_management);
        tvComics.setText(R.string.quality_analysis);

    }

    public void RemoveObfuscation() {
        //Remove obfuscation:
        ImageView ivVideos = findViewById(R.id.imageView_Video);
        ImageView ivPictures = findViewById(R.id.imageView_Images);
        ImageView ivComics = findViewById(R.id.imageView_Comics);

        ivVideos.setImageResource(R.drawable.nob_videos);
        ivPictures.setImageResource(R.drawable.nob_pictures);
        ivComics.setImageResource(R.drawable.nob_comics);

        TextView tvVideos = findViewById(R.id.textView_Videos);
        TextView tvPictures = findViewById(R.id.textView_Pictures);
        TextView tvComics = findViewById(R.id.textView_Comics);

        tvVideos.setText(R.string.videos);
        tvPictures.setText(R.string.Images);
        tvComics.setText(R.string.comics);
    }

    //=====================================================================================
    //===== ImageView Click Code =================================================================
    //=====================================================================================

    public void startVideoCatalogActivity(View v){
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_VIDEOS);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        startActivity(intentCatalogActivity);
    }

    public void startPicturesCatalogActivity(View v){
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_IMAGES);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        startActivity(intentCatalogActivity);
    }

    public void startComicsCatalogActivity(View v){
        /*Intent intentComicsCatalogActivity = new Intent(this, ComicsCatalogActivity.class);
        startActivity(intentComicsCatalogActivity);*/
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_COMICS);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        startActivity(intentCatalogActivity);
    }

    public void startImportVideos(View v){
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_VIDEOS);
        startActivity(intentImportGuided);
    }

    public void startImportImages(View v){
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_IMAGES);
        startActivity(intentImportGuided);
    }

    public void startImportComics(View v){
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_COMICS);
        startActivity(intentImportGuided);
    }




}