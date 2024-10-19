package com.agcurations.aggallerymanager;

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
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;


public class Fragment_LogViewer_0_FileList extends Fragment {


    GlobalClass globalClass;

    LogListCustomAdapter logListCustomAdapter;

    private ViewModel_Fragment_LogViewer viewModel_fragment_logViewer;

    Button gButton_Delete;
    CheckBox gCheckBox_SelectAll;

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

        gButton_Delete = getView().findViewById(R.id.button_Delete);

        gCheckBox_SelectAll = getView().findViewById(R.id.checkBox_SelectAll);

        initializeLogFileList();

        gCheckBox_SelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getActivity() == null){
                    return;
                }
                if(logListCustomAdapter != null) {
                    for (ItemClass_File icf: logListCustomAdapter.alicf_LogFiles) {
                        icf.bIsChecked = ((CheckBox) view).isChecked();
                    }
                    logListCustomAdapter.notifyDataSetChanged();
                    logListCustomAdapter.EvaluateDeleteButtonEnabled();
                }
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel_fragment_logViewer.alicf_LogFiles = logListCustomAdapter.alicf_LogFiles;
    }

    public void initializeLogFileList(){

        if(getActivity() == null || getView() == null){
            return;
        }

        //todo: handle screen rotation.
        ArrayList<ItemClass_File> alicfLogFiles = GlobalClass.GetDirectoryFileNamesData(GlobalClass.gUriLogsFolder);

        String[] sFileNames = new String[alicfLogFiles.size()];
        for(int i = 0; i < alicfLogFiles.size(); i++){
            sFileNames[i] = alicfLogFiles.get(i).sFileOrFolderName;
        }

        logListCustomAdapter = new LogListCustomAdapter(getActivity(), R.layout.listview_selectable_1line_btn_view, sFileNames, alicfLogFiles);
        ListView listView_LogFiles = getView().findViewById(R.id.listView_LogFiles);
        listView_LogFiles.setAdapter(logListCustomAdapter);

        TextView textView_NotificationNoLogFiles = getView().findViewById(R.id.textView_NotificationNoLogFiles);
        if(textView_NotificationNoLogFiles != null) {
            if (alicfLogFiles.size() > 0) {
                textView_NotificationNoLogFiles.setVisibility(View.INVISIBLE);
                gCheckBox_SelectAll.setVisibility(View.VISIBLE);
            } else {
                textView_NotificationNoLogFiles.setVisibility(View.VISIBLE);
                gCheckBox_SelectAll.setVisibility(View.INVISIBLE);
            }
        }

    }



    public class LogListCustomAdapter extends ArrayAdapter<String> {

        ArrayList<ItemClass_File> alicf_LogFiles;

        public LogListCustomAdapter(@NonNull Context context, int resource, @NonNull String[] objects, ArrayList<ItemClass_File> _alicf_LogFiles) {
            super(context, resource, objects);
            alicf_LogFiles = _alicf_LogFiles;
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
            RelativeLayout relativeLayout_Row = row.findViewById(R.id.relativeLayout_Row);

            checkBox_ItemSelect.setChecked(alicf_LogFiles.get(position).bIsChecked);

            checkBox_ItemSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alicf_LogFiles.get(position).bIsChecked = ((CheckBox) view).isChecked();
                    EvaluateDeleteButtonEnabled();
                }
            });

            textView_Line1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkBox_ItemSelect.setChecked(!checkBox_ItemSelect.isChecked());
                    alicf_LogFiles.get(position).bIsChecked = checkBox_ItemSelect.isChecked();
                    EvaluateDeleteButtonEnabled();
                }
            });

            relativeLayout_Row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkBox_ItemSelect.setChecked(!checkBox_ItemSelect.isChecked());
                    alicf_LogFiles.get(position).bIsChecked = checkBox_ItemSelect.isChecked();
                    EvaluateDeleteButtonEnabled();
                }
            });

            String sFileName = alicf_LogFiles.get(position).sFileOrFolderName;
            textView_Line1.setText(sFileName);

            button_View.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewModel_fragment_logViewer.sLogFileName = alicf_LogFiles.get(position).sFileOrFolderName;

                    Activity_LogViewer activity_logViewer = (Activity_LogViewer) getActivity();
                    if(activity_logViewer != null){
                        activity_logViewer.gotoViewer();
                    }

                }
            });

            return row;
        }

        @SuppressWarnings("unchecked")
        private void EvaluateDeleteButtonEnabled(){
            boolean bEnabled = false;
            viewModel_fragment_logViewer.alicf_LogFiles.clear();
            for(ItemClass_File icf: alicf_LogFiles){
                if(icf.bIsChecked){
                    bEnabled = true;
                    break;
                }
            }
            if(gCheckBox_SelectAll.isChecked()){
                for(ItemClass_File icf: alicf_LogFiles) {
                    if (!icf.bIsChecked) {
                        gCheckBox_SelectAll.setChecked(false);
                        break;
                    }

                }
            }
            viewModel_fragment_logViewer.alicf_LogFiles = (ArrayList<ItemClass_File>) alicf_LogFiles.clone();
            gButton_Delete.setEnabled(bEnabled);
        }

    }



}