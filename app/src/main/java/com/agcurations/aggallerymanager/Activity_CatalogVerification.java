package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_CatalogVerification extends AppCompatActivity {

    int giSpinnerPosition = -1;

    TextView gTextView_Log;
    ScrollView gScrollView_Log;

    ProgressBar gProgressBar_CatalogVerificationProgress;
    TextView gTextView_CatalogVerificationProgressBarText;


    private CatalogVerificationResponseReceiver catalogVerificationResponseReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog_verification);


        Spinner spinner_CatalogType = findViewById(R.id.spinner_CatalogType);
        String[] sSpinnerItems = new String[]{"Videos", "Images", "Comics"};
        ArrayAdapter<String> adapter=new ArrayAdapter<>(getApplicationContext(), R.layout.catalog_spinner_item_sort_search_filter, sSpinnerItems);
        //assign adapter to the Spinner
        spinner_CatalogType.setAdapter(adapter);
        spinner_CatalogType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                giSpinnerPosition = pos;
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        gTextView_Log = findViewById(R.id.textView_Log);
        gScrollView_Log = findViewById(R.id.scrollView_Log);

        gProgressBar_CatalogVerificationProgress = findViewById(R.id.progressBar_CatalogVerificationProgress);
        gTextView_CatalogVerificationProgressBarText = findViewById(R.id.textView_CatalogVerificationProgressBarText);

        Button button_AnalysisStartStop = findViewById(R.id.button_AnalysisStartStop);
        button_AnalysisStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sMode = button_AnalysisStartStop.getText().toString();
                if(sMode.equals("START")){
                    button_AnalysisStartStop.setText("STOP");
                } else {
                    button_AnalysisStartStop.setText("START");
                }
                Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                Data dataCatalogVerification = new Data.Builder()
                        .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_CatalogVerification:button_AnalysisStartStop.onClick()")
                        .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                        .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, giSpinnerPosition)
                        .build();
                OneTimeWorkRequest otwrCatalogVerification = new OneTimeWorkRequest.Builder(Worker_Catalog_Verification.class)
                        .setInputData(dataCatalogVerification)
                        .addTag(Worker_Catalog_Verification.TAG_WORKER_CATALOG_VERIFICATION) //To allow finding the worker later.
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueue(otwrCatalogVerification);
            }
        });

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_Verification.CATALOG_VERIFICATION_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        catalogVerificationResponseReceiver = new CatalogVerificationResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(catalogVerificationResponseReceiver,filter);

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(catalogVerificationResponseReceiver);
        super.onDestroy();
    }

    public class CatalogVerificationResponseReceiver extends BroadcastReceiver {

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
                boolean bUpdateLog;

                boolean	bUpdatePercentComplete;
                boolean	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:

                bUpdateLog = intent.getBooleanExtra(GlobalClass.UPDATE_LOG_BOOLEAN, false);

                bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdateLog){
                    String sLogLine = intent.getStringExtra(GlobalClass.LOG_LINE_STRING);
                    if (gTextView_Log != null) {
                        gTextView_Log.append(sLogLine);
                        if(gScrollView_Log != null){
                            gScrollView_Log.fullScroll(View.FOCUS_DOWN);
                        }
                    }
                }

                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_CatalogVerificationProgress != null) {
                        gProgressBar_CatalogVerificationProgress.setProgress(iAmountComplete);
                    }
                    if (iAmountComplete == 100) {
                        assert gProgressBar_CatalogVerificationProgress != null;
                        gProgressBar_CatalogVerificationProgress.setVisibility(View.INVISIBLE);
                        gTextView_CatalogVerificationProgressBarText.setVisibility(View.INVISIBLE);
                    } else {
                        assert gProgressBar_CatalogVerificationProgress != null;
                        gProgressBar_CatalogVerificationProgress.setVisibility(View.VISIBLE);
                        gTextView_CatalogVerificationProgressBarText.setVisibility(View.VISIBLE);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_CatalogVerificationProgressBarText != null) {
                        gTextView_CatalogVerificationProgressBarText.setText(sProgressBarText);
                    }
                }



            } //End if not an error message.

        } //End onReceive.

    } //End CatalogVerificationResponseReceiver.





}