package com.agcurations.aggallerymanager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


public class Fragment_LogViewer_0_FileList extends Fragment {


    GlobalClass globalClass;

    LogListCustomAdapter logListCustomAdapter;

    private ViewModel_Fragment_LogViewer viewModel_fragment_logViewer;

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
                        for (int i = 0; i < logListCustomAdapter.fLogFiles.length; i++) {
                            if (logListCustomAdapter.bRowSelected[i]) {
                                if (!logListCustomAdapter.fLogFiles[i].delete()) {
                                    Toast.makeText(getActivity().getApplicationContext(), "Could not delete file: " + logListCustomAdapter.fLogFiles[i].getName(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        initializeLogFileList();
                    }
                }
            });
        }

    }

    public void initializeLogFileList(){

        if(getActivity() == null || getView() == null){
            return;
        }

        File[] fLogFiles = globalClass.gfLogsFolder.listFiles();
        if(fLogFiles != null) {
            logListCustomAdapter = new LogListCustomAdapter(getActivity(), R.layout.listview_selectable_1line_btn_view, fLogFiles);
            ListView listView_LogFiles = getView().findViewById(R.id.listView_LogFiles);
            listView_LogFiles.setAdapter(logListCustomAdapter);
        }
    }



    public class LogListCustomAdapter extends ArrayAdapter<File> {

        File[] fLogFiles;
        boolean[] bRowSelected;

        public LogListCustomAdapter(@NonNull Context context, int resource, @NonNull File[] objects) {
            super(context, resource, objects);
            fLogFiles = objects;
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

            checkBox_ItemSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bRowSelected[position] = ((CheckBox)view).isChecked();
                }
            });

            String sFileName = fLogFiles[position].getName();
            textView_Line1.setText(sFileName);

            button_View.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewModel_fragment_logViewer.fLogFile = fLogFiles[position];

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