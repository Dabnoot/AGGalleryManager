package com.agcurations.aggallerymanager;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;


public class Fragment_WorkerConsole_0_WorkerList extends Fragment {


    GlobalClass globalClass;

    WorkerListCustomAdapter workerListCustomAdapter;

    private ViewModel_Fragment_WorkerConsole viewModel_fragment_workerConsole;

    boolean bItemsDeletedFlag = false;

    Observer<WorkInfo> workInfoObserver_TrackingTest;

    public Fragment_WorkerConsole_0_WorkerList() {
        // Required empty public constructor
    }

    public static Fragment_WorkerConsole_0_WorkerList newInstance() {
        return new Fragment_WorkerConsole_0_WorkerList();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_worker_console_0_worker_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(getView() == null){
            return;
        }
        Button button_Return = getView().findViewById(R.id.button_Return);
        button_Return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getActivity() == null){
                    return;
                }
                getActivity().finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if(getActivity() == null || getView() == null){
            return;
        }

        getActivity().setTitle("Worker Console");

        //Instantiate the ViewModel sharing data between fragments:
        viewModel_fragment_workerConsole = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_WorkerConsole.class);

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        initializeWorkerList();

        Button button_Delete = getView().findViewById(R.id.button_Delete);
        if(button_Delete != null){
            button_Delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(getActivity() == null){
                        return;
                    }

                    boolean bDeleteAll = true;
                    if(workerListCustomAdapter != null) {
                        for (int i = 0; i < workerListCustomAdapter.customWorkerData.length; i++) {
                            if (workerListCustomAdapter.bRowSelected[i]) {
                                if (workerListCustomAdapter.customWorkerData[i].dfJobFile != null) {
                                    if (!workerListCustomAdapter.customWorkerData[i].dfJobFile.delete()) {
                                        Toast.makeText(getActivity().getApplicationContext(), "Could not delete file: " + workerListCustomAdapter.customWorkerData[i].dfJobFile.getName(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                bDeleteAll = false;
                            }
                        }
                        if(bDeleteAll){
                            //If the user is trying to delete all items from the list, issue a command to prune workers.
                            //  This command tells WorkManager to clear all with SUCCESS, FAILED, or CANCELLED.
                            //  No way as of 9/22/2021 to prune individual workers.
                            WorkManager.getInstance(getActivity().getApplicationContext()).pruneWork();
                        }

                        bItemsDeletedFlag = true;
                        initializeWorkerList();
                    }
                }
            });
        }

        CheckBox checkBox_SelectAll = getView().findViewById(R.id.checkBox_SelectAll);
        checkBox_SelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getActivity() == null){
                    return;
                }
                if(workerListCustomAdapter != null) {
                    for (int i = 0; i < workerListCustomAdapter.customWorkerData.length; i++) {
                        workerListCustomAdapter.bRowSelected[i] = ((CheckBox) view).isChecked();
                    }
                    workerListCustomAdapter.notifyDataSetChanged();
                }
            }
        });

    }

    public void initializeWorkerList(){

        if(getActivity() == null || getView() == null){
            return;
        }

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


/*                    if (workInfo.getState() == WorkInfo.State.RUNNING) {

                    } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {

                    }*/
                }
            }
        };

        //Look to see if there are any workers out there processing data for AGGalleryManager,
        //  and if so, attempt to listen to their progress:
        ArrayList<CustomWorkerData> alWorkerData = new ArrayList<>();
        ListenableFuture<List<WorkInfo>> lfListWorkInfo = WorkManager.getInstance(getActivity().getApplicationContext()).getWorkInfosByTag(Worker_LocalFileTransfer.WORKER_LOCAL_FILE_TRANSFER_TAG);
        try {
            int iWorkerCount = lfListWorkInfo.get().size();
            for(int i = 0; i < iWorkerCount; i++) {
                WorkInfo.State stateWorkState = lfListWorkInfo.get().get(i).getState();
                Data dataOutput = lfListWorkInfo.get().get(i).getOutputData();
                CustomWorkerData newWorkerData = new CustomWorkerData();
                UUID UUIDWorkerID = lfListWorkInfo.get().get(i).getId();
                Log.d("Workstate", stateWorkState.toString() + ", ID " + UUIDWorkerID.toString());
                String sState = "Unknown";
                if( stateWorkState == WorkInfo.State.BLOCKED) {
                    sState = "Blocked";
                } else if( stateWorkState == WorkInfo.State.ENQUEUED) {
                    sState = "Enqueued";
                } else if( stateWorkState == WorkInfo.State.CANCELLED) {
                    sState = "Cancelled";
                } else if( stateWorkState == WorkInfo.State.FAILED) {
                    sState = "Failed";
                    newWorkerData.sFailureMessage = dataOutput.getString(Worker_LocalFileTransfer.FAILURE_MESSAGE);
                    if(newWorkerData.sFailureMessage == null){
                        newWorkerData.sFailureMessage = "No failure message data.";
                    }
                } else if( stateWorkState == WorkInfo.State.RUNNING) {
                    sState = "Running";
                } else if( stateWorkState == WorkInfo.State.SUCCEEDED) {
                    sState = "Succeeded";
                }
                newWorkerData.uuidWorkerID = UUIDWorkerID;

                newWorkerData.sWorkerStatus = sState;
                if(stateWorkState == WorkInfo.State.SUCCEEDED ||
                        stateWorkState == WorkInfo.State.FAILED ||
                        stateWorkState == WorkInfo.State.CANCELLED){
                    //If the job is permanently stopped, get data from the output data:
                    newWorkerData.sJobRequestDateTime = dataOutput.getString(Worker_LocalFileTransfer.JOB_DATETIME);
                    newWorkerData.lProgressNumerator = dataOutput.getLong(Worker_LocalFileTransfer.JOB_BYTES_PROCESSED, 0);
                    newWorkerData.lProgressDenominator = dataOutput.getLong(Worker_LocalFileTransfer.JOB_BYTES_TOTAL, 100);
                } else {
                    //If the job is ongoing, get the data from the progress data:
                    Data progress = lfListWorkInfo.get().get(i).getProgress();
                    newWorkerData.lProgressNumerator = progress.getLong(Worker_TrackingTest.WORKER_BYTES_PROCESSED, 0);
                    newWorkerData.lProgressDenominator = progress.getLong(Worker_TrackingTest.WORKER_BYTES_TOTAL, 100);
                }
                alWorkerData.add(newWorkerData);
                WorkManager wm = WorkManager.getInstance(getActivity().getApplicationContext());
                LiveData<WorkInfo> ldWorkInfo = wm.getWorkInfoByIdLiveData(UUIDWorkerID);
                ldWorkInfo.observe(this, workInfoObserver_TrackingTest);

            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        //Attempt to divine the existence of other past workers by examining job files:
        DocumentFile[] dfJobFiles = globalClass.gdfJobFilesFolder.listFiles();
        for(DocumentFile dfJobFile: dfJobFiles){
            String sJobFileName = dfJobFile.getName();
            String sJobRequestDateTime = sJobFileName.substring(("Job_").length(),sJobFileName.length()-(".txt").length());
            boolean bDataFound = false;
            for(CustomWorkerData customWorkerData : alWorkerData){
                if(customWorkerData.sJobRequestDateTime != null) {
                    if (customWorkerData.sJobRequestDateTime.equals(sJobRequestDateTime)) {
                        customWorkerData.sJobRequestDateTime = sJobRequestDateTime;
                        customWorkerData.dfJobFile = dfJobFile;
                        bDataFound = true;
                    }
                }
            }
            if(!bDataFound){
                CustomWorkerData customWorkerDataNew = new CustomWorkerData();
                customWorkerDataNew.sJobRequestDateTime = sJobRequestDateTime;
                customWorkerDataNew.dfJobFile = dfJobFile;
                customWorkerDataNew.sWorkerStatus = "[worker not found]";
                alWorkerData.add(customWorkerDataNew);
            }

            //todo: check to make sure that the files were copied successfully (might not be able to confirm source exists/does not exist).
        }


        CustomWorkerData[] customWorkerData = new CustomWorkerData[alWorkerData.size()];
        customWorkerData = alWorkerData.toArray(customWorkerData);

        workerListCustomAdapter = new WorkerListCustomAdapter(getActivity(), R.layout.listview_selectable_1line_btn_view, customWorkerData);
        ListView listView_Workers = getView().findViewById(R.id.listView_Workers);
        listView_Workers.setAdapter(workerListCustomAdapter);

        TextView textView_NotificationNoWorkers = getView().findViewById(R.id.textView_NotificationNoWorkers);
        if(textView_NotificationNoWorkers != null) {
            if (customWorkerData.length > 0) {
                textView_NotificationNoWorkers.setVisibility(View.INVISIBLE);
            } else {
                textView_NotificationNoWorkers.setVisibility(View.VISIBLE);
                /* Don't automatically close the workers console when all items are deleted -
                     there's a lot to take in with this activity.
                if (bItemsDeletedFlag){
                    //If items were just deleted and now the list of files is empty, wait a moment
                    // and then end this activity.
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(getActivity() instanceof Activity_WorkerConsole) {
                                getActivity().finish();
                            }
                        }
                    }, 2000);
                }*/

            }
        }


        bItemsDeletedFlag = false;
    }



    public class WorkerListCustomAdapter extends ArrayAdapter<String> {

        CustomWorkerData[] customWorkerData;
        boolean[] bRowSelected;

        public WorkerListCustomAdapter(@NonNull Context context, int resource, @NonNull CustomWorkerData[] objects) {
            super(context, resource);
            customWorkerData = objects;
            bRowSelected = new boolean[objects.length];
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View v, @NonNull ViewGroup parent) {

            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                //My custom list item design is here
                row = inflater.inflate(R.layout.listview_selectable_3line_2btn_view_restart, parent, false);
            }

            CheckBox checkBox_ItemSelect =  row.findViewById(R.id.checkBox_ItemSelect);
            TextView textView_Line1 = row.findViewById(R.id.textView_Line1);
            TextView textView_Line2 = row.findViewById(R.id.textView_Line2);
            TextView textView_Line3 = row.findViewById(R.id.textView_Line3);
            Button button_View = row.findViewById(R.id.button_View);
            Button button_Restart = row.findViewById(R.id.button_Restart);

            checkBox_ItemSelect.setChecked(bRowSelected[position]);

            checkBox_ItemSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bRowSelected[position] = ((CheckBox)view).isChecked();
                }
            });



            final String sJobFileName;
            if(customWorkerData[position].dfJobFile != null){
                sJobFileName = customWorkerData[position].dfJobFile.getName();
            } else {
                sJobFileName = "";
            }
            String sLine1;
            if(customWorkerData[position].uuidWorkerID != null) {
                sLine1 = "Worker UUID: " + customWorkerData[position].uuidWorkerID.toString();
            } else {
                sLine1 = "Job file only - no matching worker found. Worker may have died, suggest restart.";
            }
            textView_Line1.setText(sLine1);

            String sProgress;
            String sWorkerStatus = customWorkerData[position].sWorkerStatus;
            if(sWorkerStatus.equals("Failed")){
                sProgress = customWorkerData[position].sFailureMessage;
            } else {
                long lProgressNumerator = customWorkerData[position].lProgressNumerator;
                long lProgressDenominator = customWorkerData[position].lProgressDenominator;
                int iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                if (iProgressBarValue == 0) {
                    sProgress = "Progress: 0% / Unknown";
                } else {
                    sProgress = "Progress: " + iProgressBarValue + "%";
                }
            }
            String sLine2 = "Worker Status: " + sWorkerStatus + ".\t" + sProgress;
            textView_Line2.setText(sLine2);

            final String sJobRequestDateTime = customWorkerData[position].sJobRequestDateTime;
            String sLine3 = "Job date/time: " + sJobRequestDateTime + "\tJob File: " + sJobFileName;
            textView_Line3.setText(sLine3);

            //Set button enable/disable based on presence of job file:
            button_View.setEnabled(!sJobFileName.equals(""));
            button_Restart.setEnabled(!sJobFileName.equals(""));

            button_View.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewModel_fragment_workerConsole.dfJobFile = customWorkerData[position].dfJobFile;

                    Activity_WorkerConsole activity_workerConsole = (Activity_WorkerConsole) getActivity();
                    if(activity_workerConsole != null){
                        activity_workerConsole.gotoWorkerDetails();
                    }

                }
            });

            button_Restart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Build-out data to send to the worker:
                    Data dataLocalFileTransfer = new Data.Builder()
                            .putString(Worker_LocalFileTransfer.KEY_ARG_JOB_REQUEST_DATETIME, sJobRequestDateTime)
                            .putString(Worker_LocalFileTransfer.KEY_ARG_JOB_FILE, sJobFileName)
                            .build();
                    OneTimeWorkRequest otwrLocalFileTransfer = new OneTimeWorkRequest.Builder(Worker_LocalFileTransfer.class)
                            .setInputData(dataLocalFileTransfer)
                            .addTag(Worker_LocalFileTransfer.WORKER_LOCAL_FILE_TRANSFER_TAG) //To allow finding the worker later.
                            .build();
                    UUID UUIDWorkID = otwrLocalFileTransfer.getId();
                    WorkManager.getInstance(getActivity().getApplicationContext()).enqueue(otwrLocalFileTransfer);

                }
            });

            return row;
        }

        @Override
        public int getCount() {
            return customWorkerData.length;
        }
    }


    class CustomWorkerData {
        UUID uuidWorkerID;
        String sJobRequestDateTime;
        String sWorkerStatus;
        String sFailureMessage;
        DocumentFile dfJobFile;
        long lProgressNumerator;
        long lProgressDenominator;
    }



}