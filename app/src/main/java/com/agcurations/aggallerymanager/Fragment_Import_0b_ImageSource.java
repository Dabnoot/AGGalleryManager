package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


public class Fragment_Import_0b_ImageSource extends Fragment {

    BroadcastReceiver_FI_0b_ImageSource broadcastReceiver_FI_0b_ImageSource;

    public Fragment_Import_0b_ImageSource() {
        // Required empty public constructor
    }

    public static Fragment_Import_0b_ImageSource newInstance() {
        return new Fragment_Import_0b_ImageSource();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Import_HoldingFolderPreview.IMPORT_HOLDING_FOLDER_PREVIEW_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastReceiver_FI_0b_ImageSource = new BroadcastReceiver_FI_0b_ImageSource();
        if(getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(broadcastReceiver_FI_0b_ImageSource, filter);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_0b_image_source, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();


        //Determine the number of files in the holding folder and adjust the radiobutton text to show file count.
        if(getActivity() != null) {
            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            Data dataHoldingFolderPreview = new Data.Builder()
                    .putString(GlobalClass.EXTRA_CALLER_ID, "Fragment_Import_0b_ImageSource:onResume()")
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                    .build();
            OneTimeWorkRequest otwrHoldingFolderPreview = new OneTimeWorkRequest.Builder(Worker_Import_HoldingFolderPreview.class)
                    .setInputData(dataHoldingFolderPreview)
                    .addTag(Worker_Import_HoldingFolderPreview.TAG_WORKER_IMPORT_HOLDING_FOLDER_PREVIEW) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getActivity().getApplicationContext()).enqueue(otwrHoldingFolderPreview);
        }



    }


    public class BroadcastReceiver_FI_0b_ImageSource extends BroadcastReceiver {

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

            //This receiver is used primarily to see if the holding folder contents have been checked
            // to see how many media items in the folder should be visible to the current user.

            boolean bHoldingFolderFilesResult = intent.getBooleanExtra(Worker_Import_HoldingFolderPreview.EXTRA_BOOL_HOLDING_FOLDER_FILES_APPROVED_RESULT,false);
            if(bHoldingFolderFilesResult){
                int iHoldingFolderFilesResult = intent.getIntExtra(Worker_Import_HoldingFolderPreview.EXTRA_INT_HOLDING_FOLDER_FILES_APPROVED_FOR_USER, 0);

                if(getView() != null && getActivity() != null) {
                    RadioButton radioButton_ImageSourceHoldingFolder = getView().findViewById(R.id.radioButton_ImageSourceHoldingFolder);
                    RadioGroup radioGroup_ImageSource = getView().findViewById(R.id.radioGroup_ImageSource);
                    GlobalClass globalClass = (GlobalClass) getActivity().getApplicationContext();
                    if (iHoldingFolderFilesResult > 0) {
                        String sText = "Internal holding folder (" + iHoldingFolderFilesResult;
                        if (iHoldingFolderFilesResult == 1) {
                            sText = sText + " file)";
                        } else {
                            sText = sText + " files)";
                        }
                        radioButton_ImageSourceHoldingFolder.setText(sText);
                        radioButton_ImageSourceHoldingFolder.setVisibility(View.VISIBLE);
                        radioGroup_ImageSource.getLayoutParams().height = globalClass.ConvertDPtoPX(140);
                        radioGroup_ImageSource.requestLayout();
                    } else {
                        //todo: does it matter if this code is hit?
                        radioButton_ImageSourceHoldingFolder.setVisibility(View.INVISIBLE);
                        radioGroup_ImageSource.getLayoutParams().height = globalClass.ConvertDPtoPX(90);
                        radioGroup_ImageSource.requestLayout();
                    }
                }

            }

        }
    }



}