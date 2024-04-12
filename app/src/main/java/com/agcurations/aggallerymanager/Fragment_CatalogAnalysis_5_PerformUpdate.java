package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Locale;


public class Fragment_CatalogAnalysis_5_PerformUpdate extends Fragment {

    private ViewModel_CatalogAnalysis viewModel_catalogAnalysis;

    ProgressBar gProgressBar_UpdateProgress;
    TextView gTextView_UpdateProgressBarText;
    TextView gtextView_UpdateLog;
    ScrollView gScrollView_UpdateLog;

    GlobalClass globalClass;

    public Fragment_CatalogAnalysis_5_PerformUpdate() {
        // Required empty public constructor
    }

    public static Fragment_CatalogAnalysis_5_PerformUpdate newInstance() {
        return new Fragment_CatalogAnalysis_5_PerformUpdate();
    }


    UpdateDataServiceResponseReceiver updateDataServiceResponseReceiver;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            viewModel_catalogAnalysis = new ViewModelProvider(getActivity()).get(ViewModel_CatalogAnalysis.class);

            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_Maintenance.CATALOG_MAINTENANCE_ACTION_RESPONSE);
        filter.addAction(GlobalClass.BROADCAST_WRITE_CATALOG_FILE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        updateDataServiceResponseReceiver = new UpdateDataServiceResponseReceiver();

        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(updateDataServiceResponseReceiver, filter);
        }
    }

    @Override
    public void onDestroy() {
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(updateDataServiceResponseReceiver);
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_catalog_analysis_2_perform_analysis, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() != null) {
            getActivity().setTitle("Perform Analysis");
            if(((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().show();
            }
        }
        initComponents();
    }



    public void initComponents(){

        //Init progress:
        if(getView() == null){
            return;
        }
        gProgressBar_UpdateProgress = getView().findViewById(R.id.progressBar_AnalysisProgress);
        gProgressBar_UpdateProgress.setMax(100);
        gTextView_UpdateProgressBarText = getView().findViewById(R.id.textView_AnalysisProgressBarText);

        //Set the log textView to be able to scroll vertically:
        gtextView_UpdateLog = getView().findViewById(R.id.textView_AnalysisLog);
        gScrollView_UpdateLog = getView().findViewById(R.id.scrollView_AnalysisLog);

        if(GlobalClass.aiCatalogUpdateRunning.get() == GlobalClass.START_REQUESTED) {
            gProgressBar_UpdateProgress.setProgress(0);
            gTextView_UpdateProgressBarText.setText("0/0");
            gtextView_UpdateLog.setText("");
            globalClass.gsbUpdateExecutionLog = new StringBuilder();

            if(getContext() == null) return;
            String sCallerID = "Fragment_CatalogAnalysis_2_PerformAnalysis:initComponents()";
            double dTimeStamp = GlobalClass.GetTimeStampDouble();

            Data dataAnalysisFiles = new Data.Builder()
                    .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                    .putInt(Worker_Catalog_Analysis.EXTRA_ANALYSIS_TYPE, viewModel_catalogAnalysis.iAnalysisType)
                    .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, viewModel_catalogAnalysis.iMediaCategory)
                    .build();
            OneTimeWorkRequest otwrAnalysisFiles = new OneTimeWorkRequest.Builder(Worker_Catalog_Analysis.class)
                    .setInputData(dataAnalysisFiles)
                    .addTag(Worker_Catalog_Analysis.TAG_WORKER_CATALOG_VERIFICATION) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getContext()).enqueue(otwrAnalysisFiles);

        } else {
            //If an Analysis has been started...
            //Refresh the text and progress:
            gProgressBar_UpdateProgress.setProgress(globalClass.giUpdateExecutionProgressBarPercent);
            gTextView_UpdateProgressBarText.setText(globalClass.gsUpdateExecutionProgressBarText);
            gtextView_UpdateLog.setText(globalClass.gsbUpdateExecutionLog.toString());


            if(GlobalClass.aiCatalogUpdateRunning.get() == GlobalClass.FINISHED){
                //If the user has returned to this fragment and the Update is finished,
                //  enable the buttons:
                if(getView() != null) {
                    Button button_UpdateFinish = getView().findViewById(R.id.button_UpdateFinish);
                    if (button_UpdateFinish != null) {
                        button_UpdateFinish.setEnabled(true);
                    }
                    Button button_UpdateRestart = getView().findViewById(R.id.button_UpdateRestart);
                    if (button_UpdateRestart != null) {
                        button_UpdateRestart.setEnabled(true);
                    }
                }
            }
        }

    }


    public class UpdateDataServiceResponseReceiver extends BroadcastReceiver {


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
                        if (gtextView_UpdateLog != null) {
                            gtextView_UpdateLog.append(sLogLine);
                            //Execute delayed scroll down since this broadcast listener is not on the UI thread:
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Do something after 100ms
                                    if(gScrollView_UpdateLog != null){
                                        gScrollView_UpdateLog.fullScroll(View.FOCUS_DOWN);
                                    }
                                }
                            }, 100);
                        }
                        if (sLogLine.toLowerCase(Locale.ROOT).contains("operation complete.")) {
                            if(getView() != null) {
                                Button button_AnalysisFinish = getView().findViewById(R.id.button_AnalysisFinish);
                                if (button_AnalysisFinish != null) {
                                    button_AnalysisFinish.setEnabled(true);
                                }
                                Button button_AnalysisRestart = getView().findViewById(R.id.button_AnalysisRestart);
                                if (button_AnalysisRestart != null) {
                                    button_AnalysisRestart.setEnabled(true);
                                }
                            }
                        }
                    }
                }
                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_UpdateProgress != null) {
                        gProgressBar_UpdateProgress.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_UpdateProgressBarText != null) {
                        gTextView_UpdateProgressBarText.setText(sProgressBarText);
                    }
                }


            }

        }
    }




}