package com.agcurations.aggallerymanager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class Fragment_LogViewer_0_FileList extends Fragment {


    GlobalClass globalClass;

    LogListCustomAdapter logListCustomAdapter;

    private ViewModel_Fragment_LogViewer viewModel_fragment_logViewer;

    boolean bItemsDeletedFlag = false;

    public Fragment_LogViewer_0_FileList() {
        // Required empty public constructor
    }

    public static Fragment_LogViewer_0_FileList newInstance() {
        return new Fragment_LogViewer_0_FileList();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_log_viewer_0_file_list, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(getActivity() == null || getView() == null){
            return;
        }

        getActivity().setTitle("Log Viewer");

        //Instantiate the ViewModel sharing data between fragments:
        viewModel_fragment_logViewer = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_LogViewer.class);

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        initializeLogFileList();

        Button button_Delete = getView().findViewById(R.id.button_Delete);
        if(button_Delete != null){
            button_Delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(getActivity() == null){
                        return;
                    }

                    if(logListCustomAdapter != null) {
                        for (int i = 0; i < logListCustomAdapter.dfLogFiles.length; i++) {
                            if (logListCustomAdapter.bRowSelected[i]) {
                                if (!logListCustomAdapter.dfLogFiles[i].delete()) {
                                    Toast.makeText(getActivity().getApplicationContext(), "Could not delete file: " + logListCustomAdapter.dfLogFiles[i].getName(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        bItemsDeletedFlag = true;
                        initializeLogFileList();
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
                if(logListCustomAdapter != null) {
                    for (int i = 0; i < logListCustomAdapter.dfLogFiles.length; i++) {
                        logListCustomAdapter.bRowSelected[i] = ((CheckBox) view).isChecked();
                    }
                    logListCustomAdapter.notifyDataSetChanged();
                }
            }
        });

    }

    public void initializeLogFileList(){

        if(getActivity() == null || getView() == null){
            return;
        }

        DocumentFile[] dfLogFiles = globalClass.gdfLogsFolder.listFiles();

        logListCustomAdapter = new LogListCustomAdapter(getActivity(), R.layout.listview_selectable_1line_btn_view, dfLogFiles);
        ListView listView_LogFiles = getView().findViewById(R.id.listView_LogFiles);
        listView_LogFiles.setAdapter(logListCustomAdapter);

        TextView textView_NotificationNoLogFiles = getView().findViewById(R.id.textView_NotificationNoLogFiles);
        if(textView_NotificationNoLogFiles != null) {
            if (dfLogFiles.length > 0) {
                textView_NotificationNoLogFiles.setVisibility(View.INVISIBLE);
            } else {
                textView_NotificationNoLogFiles.setVisibility(View.VISIBLE);
                if (bItemsDeletedFlag){
                    //If items were just deleted and now the list of files is empty, wait a moment
                    // and then end this activity.
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(getActivity() instanceof Activity_LogViewer) {
                                getActivity().finish();
                            }
                        }
                    }, 2000);
                }

            }
        }


        bItemsDeletedFlag = false;
    }



    public class LogListCustomAdapter extends ArrayAdapter<DocumentFile> {

        DocumentFile[] dfLogFiles;
        boolean[] bRowSelected;

        public LogListCustomAdapter(@NonNull Context context, int resource, @NonNull DocumentFile[] objects) {
            super(context, resource, objects);
            dfLogFiles = objects;
            bRowSelected = new boolean[objects.length];
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View v, @NonNull ViewGroup parent) {

            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                //My custom list item design is here
                row = inflater.inflate(R.layout.listview_selectable_1line_btn_view, parent, false);
            }

            CheckBox checkBox_ItemSelect =  row.findViewById(R.id.checkBox_ItemSelect);
            TextView textView_Line1 = row.findViewById(R.id.textView_Line1);
            Button button_View = row.findViewById(R.id.button_View);

            checkBox_ItemSelect.setChecked(bRowSelected[position]);

            checkBox_ItemSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bRowSelected[position] = ((CheckBox)view).isChecked();
                }
            });

            String sFileName = dfLogFiles[position].getName();
            textView_Line1.setText(sFileName);

            button_View.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewModel_fragment_logViewer.dfLogFile = dfLogFiles[position];

                    Activity_LogViewer activity_logViewer = (Activity_LogViewer) getActivity();
                    if(activity_logViewer != null){
                        activity_logViewer.gotoViewer();
                    }

                }
            });

            return row;
        }

    }



}