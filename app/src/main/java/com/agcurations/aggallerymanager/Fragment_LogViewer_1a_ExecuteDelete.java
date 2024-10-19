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

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


public class Fragment_LogViewer_1a_ExecuteDelete extends Fragment {

    CustomResponseReceiver gCustomResponseReceiver;

    ProgressBar gProgressBar_Progress;
    TextView gTextView_ProgressBarText;
    TextView gtextView_Log;
    ScrollView gScrollView_Log;

    public Fragment_LogViewer_1a_ExecuteDelete() {
        // Required empty public constructor
    }

    public static Fragment_LogViewer_1a_ExecuteDelete newInstance() {
        return new Fragment_LogViewer_1a_ExecuteDelete();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_log_viewer_1a_execute_delete, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(getActivity() == null || getView() == null){
            return;
        }

        gProgressBar_Progress = getView().findViewById(R.id.progressBar_Progress);
        gTextView_ProgressBarText = getView().findViewById(R.id.textView_ProgressBarText);
        gtextView_Log = getView().findViewById(R.id.textView_Log);
        gScrollView_Log = getView().findViewById(R.id.scrollView_Log);

        //Configure the Cancel button:
        Button button_Cancel = getView().findViewById(R.id.button_Cancel);
        button_Cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GlobalClass.gabGeneralFileDeletionCancel.set(true); //Worker will set back to false, or this Activity on destroy.
            }
        });

        //Configure a response receiver to listen for updates from a Worker:
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_DeleteFiles.DELETE_FILES_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        gCustomResponseReceiver = new CustomResponseReceiver();
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(gCustomResponseReceiver, filter);
        }

        if(GlobalClass.gabGeneralFileDeletionStart.get()){
            GlobalClass.gsbDeleteFilesExecutionLog.setLength(0); //Clear the StringBuilder. This is supposedly faster than creating a new StringBuilder.
            GlobalClass.gabGeneralFileDeletionStart.set(false);
            if(getContext() == null) return;
            String sCallerID = "Fragment_LogViewer_1a_ExecuteDelete:onResume()";
            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            Data dataExecuteDelete = new Data.Builder()
                    .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                    .build();
            OneTimeWorkRequest otwrExecuteDelete = new OneTimeWorkRequest.Builder(Worker_DeleteFiles.class)
                    .setInputData(dataExecuteDelete)
                    .addTag(Worker_DeleteFiles.TAG_WORKER_DELETEFILES) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getContext()).enqueue(otwrExecuteDelete);
        } else if (GlobalClass.gabGeneralFileDeletionRunning.get()) {
            //If the file deletion is running, restore the log to the textview and the progressbar data.
            gProgressBar_Progress.setProgress(GlobalClass.giDeleteFilesExecutionProgressBarPercent);
            gTextView_ProgressBarText.setText(GlobalClass.gsDeleteFilesExecutionProgressBarText);
            gtextView_Log.setText(GlobalClass.gsbDeleteFilesExecutionLog.toString());
        }

    }

    @Override
    public void onDestroy() {
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(gCustomResponseReceiver);
        }
        super.onDestroy();

    }

    public class CustomResponseReceiver extends BroadcastReceiver {

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
                        if (gtextView_Log != null) {
                            gtextView_Log.append(sLogLine);
                            //Execute delayed scroll down since this broadcast listener is not on the UI thread:
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Do something after 100ms
                                    if(gScrollView_Log != null){
                                        gScrollView_Log.fullScroll(View.FOCUS_DOWN);
                                    }
                                }
                            }, 100);
                        }
                    }
                }
                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_Progress != null) {
                        gProgressBar_Progress.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_ProgressBarText != null) {
                        gTextView_ProgressBarText.setText(sProgressBarText);
                    }
                }

            }

        }
    }




}