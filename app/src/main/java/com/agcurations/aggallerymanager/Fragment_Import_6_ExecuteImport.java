package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;


public class Fragment_Import_6_ExecuteImport extends Fragment {

    private ViewModel_ImportActivity viewModelImportActivity;

    ProgressBar gProgressBar_ImportProgress;
    TextView gTextView_ImportProgressBarText;
    TextView gtextView_ImportLog;
    ScrollView gScrollView_ImportLog;

    GlobalClass globalClass;

    public Fragment_Import_6_ExecuteImport() {
        // Required empty public constructor
    }

    public static Fragment_Import_6_ExecuteImport newInstance() {
        return new Fragment_Import_6_ExecuteImport();
    }


    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);

            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Import_ImportComicFolders.IMPORT_COMIC_FOLDERS_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportFiles.IMPORT_FILES_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportComicWebFiles.IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
        filter.addAction(Worker_Import_VideoDownload.IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
        filter.addAction(Worker_LocalFileTransfer.IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
        filter.addAction(GlobalClass.CATALOG_CREATE_NEW_RECORDS_ACTION_RESPONSE);
        filter.addAction(Worker_DownloadPostProcessing.DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();
        //requireActivity().registerReceiver(importDataServiceResponseReceiver, filter);
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(importDataServiceResponseReceiver, filter);
        }
    }

    @Override
    public void onDestroy() {
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(importDataServiceResponseReceiver);
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_6_execute_import, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() != null) {
            getActivity().setTitle("Import");
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }
        initComponents();
    }



    public void initComponents(){

        //Init progress:
        if(getView() == null){
            return;
        }
        gProgressBar_ImportProgress = getView().findViewById(R.id.progressBar_ImportProgress);
        gProgressBar_ImportProgress.setMax(100);
        gTextView_ImportProgressBarText = getView().findViewById(R.id.textView_ImportProgressBarText);

        //Set the log textView to be able to scroll vertically:
        gtextView_ImportLog = getView().findViewById(R.id.textView_ImportLog);
        gScrollView_ImportLog = getView().findViewById(R.id.scrollView_ImportLog);


        if(globalClass.gbImportExecutionStarted && !globalClass.gbImportExecutionRunning) {
            gProgressBar_ImportProgress.setProgress(0);
            gTextView_ImportProgressBarText.setText("0/0");
            gtextView_ImportLog.setText("");
            globalClass.gsbImportExecutionLog = new StringBuilder();
            globalClass.gbImportExecutionStarted = false;
            globalClass.gbImportExecutionRunning = true;//This prevents import from starting again
                                                             // if the activity/fragment is restarted due to an orientation change, etc.
            //Initiate the file import via ImportActivityDataService:
            globalClass.galImportFileList = viewModelImportActivity.alfiConfirmedFileImports; //Transfer to globalClass to avoid transaction limit.

            if(getContext() == null) return;
            String sCallerID = "Fragment_Import_6_ExecuteImport:initComponents()";
            double dTimeStamp = GlobalClass.GetTimeStampDouble();

            if (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                if(viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                    Data dataImportComicFolders = new Data.Builder()
                            .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                            .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                            .putInt(GlobalClass.EXTRA_IMPORT_FILES_MOVE_OR_COPY, viewModelImportActivity.iImportMethod)
                            .build();
                    OneTimeWorkRequest otwrImportComicFolders = new OneTimeWorkRequest.Builder(Worker_Import_ImportComicFolders.class)
                            .setInputData(dataImportComicFolders)
                            .addTag(Worker_Import_ImportComicFolders.TAG_WORKER_IMPORT_IMPORTCOMICFOLDERS) //To allow finding the worker later.
                            .build();
                    WorkManager.getInstance(getContext()).enqueue(otwrImportComicFolders);


                } else if(viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE) {
                    Data dataImportComicWebFiles = new Data.Builder()
                            .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                            .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                            .build();
                    OneTimeWorkRequest otwrImportComicWebFiles = new OneTimeWorkRequest.Builder(Worker_Import_ImportComicWebFiles.class)
                            .setInputData(dataImportComicWebFiles)
                            .addTag(Worker_Import_ImportComicWebFiles.TAG_WORKER_IMPORT_IMPORTCOMICWEBFILES) //To allow finding the worker later.
                            .build();
                    WorkManager.getInstance(getContext()).enqueue(otwrImportComicWebFiles);

                }
            } else if (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS
                    && viewModelImportActivity.iVideoImportSource == ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE) {
                //If this is a video download:
                Data dataVideoDownload = new Data.Builder()
                        .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                        .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                        .putString(GlobalClass.EXTRA_STRING_WEB_ADDRESS, viewModelImportActivity.sWebAddress)
                        .build();
                OneTimeWorkRequest otwrVideoDownload = new OneTimeWorkRequest.Builder(Worker_Import_VideoDownload.class)
                        .setInputData(dataVideoDownload)
                        .addTag(Worker_Import_VideoDownload.TAG_WORKER_IMPORT_VIDEODOWNLOAD) //To allow finding the worker later.
                        .build();
                WorkManager.getInstance(getContext()).enqueue(otwrVideoDownload);

            } else {
                Data dataImportFiles = new Data.Builder()
                        .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                        .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                        .putInt(GlobalClass.EXTRA_IMPORT_FILES_MOVE_OR_COPY, viewModelImportActivity.iImportMethod)
                        .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, viewModelImportActivity.iImportMediaCategory)
                        .build();
                OneTimeWorkRequest otwrImportFiles = new OneTimeWorkRequest.Builder(Worker_Import_ImportFiles.class)
                        .setInputData(dataImportFiles)
                        .addTag(Worker_Import_ImportFiles.TAG_WORKER_IMPORT_IMPORTFILES) //To allow finding the worker later.
                        .build();
                WorkManager.getInstance(getContext()).enqueue(otwrImportFiles);
            }
        } else {
            //If an import has been started...
            //Refresh the text and progress:
            gProgressBar_ImportProgress.setProgress(globalClass.giImportExecutionProgressBarPercent);
            gTextView_ImportProgressBarText.setText(globalClass.gsImportExecutionProgressBarText);
            gtextView_ImportLog.setText(globalClass.gsbImportExecutionLog.toString());


            if(globalClass.gbImportExecutionFinished){
                //If the user has returned to this fragment and the import is finished,
                //  enable the buttons:
                if(getView() != null) {
                    Button button_ImportFinish = getView().findViewById(R.id.button_ImportFinish);
                    if (button_ImportFinish != null) {
                        button_ImportFinish.setEnabled(true);
                    }
                    Button button_ImportRestart = getView().findViewById(R.id.button_ImportRestart);
                    if (button_ImportRestart != null) {
                        button_ImportRestart.setEnabled(true);
                    }
                }
            }
        }

    }


    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;
            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to update log or progress bar:
                boolean 	bUpdateLog;
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdateLog = intent.getBooleanExtra(GlobalClass.UPDATE_LOG_BOOLEAN,false);
                bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdateLog){
                    String sLogLine;
                    sLogLine = intent.getStringExtra(GlobalClass.LOG_LINE_STRING);
                    if(sLogLine != null) {
                        if (gtextView_ImportLog != null) {
                            gtextView_ImportLog.append(sLogLine);
                            //Execute delayed scroll down since this broadcast listener is not on the UI thread:
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Do something after 100ms
                                    if(gScrollView_ImportLog != null){
                                        gScrollView_ImportLog.fullScroll(View.FOCUS_DOWN);
                                    }
                                }
                            }, 100);
                        }
                        if (sLogLine.toLowerCase(Locale.ROOT).contains("operation complete.")) {
                            if(getView() != null) {
                                Button button_ImportFinish = getView().findViewById(R.id.button_ImportFinish);
                                if (button_ImportFinish != null) {
                                    button_ImportFinish.setEnabled(true);
                                }
                                Button button_ImportRestart = getView().findViewById(R.id.button_ImportRestart);
                                if (button_ImportRestart != null) {
                                    button_ImportRestart.setEnabled(true);
                                }
                            }
                        }
                    }
                }
                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_ImportProgress != null) {
                        gProgressBar_ImportProgress.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_ImportProgressBarText != null) {
                        gTextView_ImportProgressBarText.setText(sProgressBarText);
                    }
                }

            }

        }
    }




}