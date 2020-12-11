package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_Import_6_ExecuteImport#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_Import_6_ExecuteImport extends Fragment {

    private ViewModel_ImportActivity viewModelImportActivity;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    ProgressBar gProgressBar_ImportProgress;
    TextView gTextView_ImportProgressBarText;
    TextView gtextView_ImportLog;

    public Fragment_Import_6_ExecuteImport() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentImport_6_ExecuteImport.
     */
    // TODO: Rename and change types and number of parameters
    public static Fragment_Import_6_ExecuteImport newInstance(String param1, String param2) {
        Fragment_Import_6_ExecuteImport fragment = new Fragment_Import_6_ExecuteImport();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(Activity_Import.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();
        //requireActivity().registerReceiver(importDataServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(importDataServiceResponseReceiver,filter);
        viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
    }

    @Override
    public void onDestroy() {
        //requireActivity().unregisterReceiver(importDataServiceResponseReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(importDataServiceResponseReceiver);
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
        initComponents();
    }

    public void initComponents(){

        //Init progress:
        gProgressBar_ImportProgress = getView().findViewById(R.id.progressBar_ImportProgress);
        gProgressBar_ImportProgress.setProgress(0);
        gProgressBar_ImportProgress.setMax(100);
        gTextView_ImportProgressBarText = getView().findViewById(R.id.textView_ImportProgressBarText);
        gTextView_ImportProgressBarText.setText("0/0");

        //Set the log textView to be able to scroll vertically:
        gtextView_ImportLog = getView().findViewById(R.id.textView_ImportLog);
        gtextView_ImportLog.setMovementMethod(new ScrollingMovementMethod());
        //Init log:
        gtextView_ImportLog.setText("");

        //Initiate the file import via ImportActivityDataService:
        Service_Import.startActionImportFiles(getContext(),
                viewModelImportActivity.alfiConfirmedFileImports,
                viewModelImportActivity.iImportMethod,
                viewModelImportActivity.iImportMediaCategory);


    }


    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String IMPORT_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_IMPORT_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            String sReceiver = intent.getStringExtra(Service_Import.RECEIVER_STRING);
            if(!sReceiver.contentEquals(Service_Import.RECEIVER_EXECUTE_IMPORT)){
                return;
            }

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to request to get directory contents:
                boolean 	bUpdateLog;
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdateLog = intent.getBooleanExtra(Service_Import.UPDATE_LOG_BOOLEAN,false);
                bUpdatePercentComplete = intent.getBooleanExtra(Service_Import.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(Service_Import.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdateLog){
                    String sLogLine;
                    sLogLine = intent.getStringExtra(Service_Import.LOG_LINE_STRING);
                    if(gtextView_ImportLog != null) {
                        gtextView_ImportLog.append(sLogLine);
                    }
                    if(sLogLine.contains("Operation complete.")){
                        Button button_ImportFinish = getView().findViewById(R.id.button_ImportFinish);
                        if(button_ImportFinish != null){
                            button_ImportFinish.setEnabled(true);
                        }
                    }
                }
                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(Service_Import.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_ImportProgress != null) {
                        gProgressBar_ImportProgress.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(Service_Import.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_ImportProgressBarText != null) {
                        gTextView_ImportProgressBarText.setText(sProgressBarText);
                    }
                }

            }

        }
    }




}