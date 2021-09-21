package com.agcurations.aggallerymanager;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

                    if(workerListCustomAdapter != null) {
                        for (int i = 0; i < workerListCustomAdapter.lftWorkerData.length; i++) {
                            if (workerListCustomAdapter.bRowSelected[i]) {
                                if (workerListCustomAdapter.lftWorkerData[i].fJobFile != null) {
                                    if (!workerListCustomAdapter.lftWorkerData[i].fJobFile.delete()) {
                                        Toast.makeText(getActivity().getApplicationContext(), "Could not delete file: " + workerListCustomAdapter.lftWorkerData[i].fJobFile.getName(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
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
                    for (int i = 0; i < workerListCustomAdapter.lftWorkerData.length; i++) {
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
        ArrayList<LFTWorkerData> alWorkerData = new ArrayList<>();
        ListenableFuture<List<WorkInfo>> lfListWorkInfo = WorkManager.getInstance(getActivity().getApplicationContext()).getWorkInfosByTag(Worker_LocalFileTransfer.WORKER_LOCAL_FILE_TRANSFER_TAG);
        try {
            int iWorkerCount = lfListWorkInfo.get().size();
            for(int i = 0; i < iWorkerCount; i++) {
                WorkInfo.State stateWorkState = lfListWorkInfo.get().get(i).getState();
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
                } else if( stateWorkState == WorkInfo.State.RUNNING) {
                    sState = "Running";
                } else if( stateWorkState == WorkInfo.State.SUCCEEDED) {
                    sState = "Succeeded";
                }
                Data progress = lfListWorkInfo.get().get(i).getProgress();
                LFTWorkerData lftWorkerData = new LFTWorkerData();
                lftWorkerData.uuidWorkerID = UUIDWorkerID;
                lftWorkerData.sJobRequestDateTime = "";//progress.getString(Worker_TrackingTest.WORKER_ID);  //todo:fix.
                lftWorkerData.sWorkerStatus = sState;
                alWorkerData.add(lftWorkerData);
                WorkManager wm = WorkManager.getInstance(getActivity().getApplicationContext());
                LiveData<WorkInfo> ldWorkInfo = wm.getWorkInfoByIdLiveData(UUIDWorkerID);
                ldWorkInfo.observe(this, workInfoObserver_TrackingTest);

            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        //Attempt to divine the existence of other past workers by examining job files:
        File[] fJobFiles = globalClass.gfJobFilesFolder.listFiles();
        for(File fJobFile: fJobFiles){
            String sJobFileName = fJobFile.getName();
            String sJobRequestDateTime = sJobFileName.substring(("Job_").length(),sJobFileName.length()-(".txt").length());
            boolean bDataFound = false;
            for(LFTWorkerData lftWorkerData: alWorkerData){
                if(lftWorkerData.sJobRequestDateTime.equals(sJobRequestDateTime)){
                    lftWorkerData.sJobRequestDateTime = sJobRequestDateTime;
                    lftWorkerData.fJobFile = fJobFile;
                    bDataFound = true;
                }
            }
            if(!bDataFound){
                LFTWorkerData lftWorkerDataNew = new LFTWorkerData();
                lftWorkerDataNew.sJobRequestDateTime = sJobRequestDateTime;
                lftWorkerDataNew.fJobFile = fJobFile;
                lftWorkerDataNew.sWorkerStatus = "Does not exist.";
                alWorkerData.add(lftWorkerDataNew);
            }

            //todo: check to make sure that the files were copied successfully (might not be able to confirm source exists/does not exist).
        }


        LFTWorkerData[] lftWorkerData = new LFTWorkerData[alWorkerData.size()];
        lftWorkerData = alWorkerData.toArray(lftWorkerData);

        workerListCustomAdapter = new WorkerListCustomAdapter(getActivity(), R.layout.listview_selectable_1line_btn_view, lftWorkerData);
        ListView listView_Workers = getView().findViewById(R.id.listView_Workers);
        listView_Workers.setAdapter(workerListCustomAdapter);

        TextView textView_NotificationNoWorkers = getView().findViewById(R.id.textView_NotificationNoWorkers);
        if(textView_NotificationNoWorkers != null) {
            if (lftWorkerData.length > 0) {
                textView_NotificationNoWorkers.setVisibility(View.INVISIBLE);
            } else {
                textView_NotificationNoWorkers.setVisibility(View.VISIBLE);
                if (bItemsDeletedFlag){
                    //If items were just deleted and now the list of files is empty, wait a moment
                    // and then end this activity.
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getActivity().finish();
                        }
                    }, 2000);
                }

            }
        }


        bItemsDeletedFlag = false;
    }



    public class WorkerListCustomAdapter extends ArrayAdapter<String> {

        LFTWorkerData[] lftWorkerData;
        boolean[] bRowSelected;

        public WorkerListCustomAdapter(@NonNull Context context, int resource, @NonNull LFTWorkerData[] objects) {
            super(context, resource);
            lftWorkerData = objects;
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


            final String sJobRequestDateTime = lftWorkerData[position].sJobRequestDateTime;
            String sWorkerStatus = lftWorkerData[position].sWorkerStatus;
            final String sJobFileName;
            if(lftWorkerData[position].fJobFile != null){
                sJobFileName = lftWorkerData[position].fJobFile.getName();
            } else {
                sJobFileName = "";
            }
            String sLine1 = "Job date/time: " + sJobRequestDateTime;
            String sLine2 = "Worker Status: " + sWorkerStatus;
            String sLine3 = "Job File: " + sJobFileName;
            textView_Line1.setText(sLine1);
            textView_Line2.setText(sLine2);
            textView_Line3.setText(sLine3);

            //Set button enable/disable based on presence of job file:
            button_View.setEnabled(!sJobFileName.equals(""));
            button_Restart.setEnabled(!sJobFileName.equals(""));

            button_View.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewModel_fragment_workerConsole.fJobFile = lftWorkerData[position].fJobFile;

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
                            .putInt(Worker_LocalFileTransfer.KEY_ARG_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_IMAGES)
                            .putInt(Worker_LocalFileTransfer.KEY_ARG_COPY_OR_MOVE, Worker_LocalFileTransfer.LOCAL_FILE_TRANSFER_MOVE)
                            .putLong(Worker_LocalFileTransfer.KEY_ARG_TOTAL_IMPORT_SIZE_BYTES, 0)
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
            return lftWorkerData.length;
        }
    }


    class LFTWorkerData{
        UUID uuidWorkerID;
        String sJobRequestDateTime;
        String sWorkerStatus;
        File fJobFile;
    }



}