package com.agcurations.aggallerymanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class Fragment_WorkerConsole_1_Worker_Details extends Fragment {

    TextView textView_JobText;
    private ViewModel_Fragment_WorkerConsole viewModel_fragment_workerConsole;


    public Fragment_WorkerConsole_1_Worker_Details() {
        // Required empty public constructor
    }

    public static Fragment_WorkerConsole_1_Worker_Details newInstance() {
        return new Fragment_WorkerConsole_1_Worker_Details();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() == null){
            return;
        }

        //Instantiate the ViewModel sharing data between fragments:
        viewModel_fragment_workerConsole = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_WorkerConsole.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_worker_console_1_worker_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(getView() == null){
            return;
        }
        textView_JobText = getView().findViewById(R.id.textView_JobText);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(getActivity() == null){
            return;
        }

        if(viewModel_fragment_workerConsole.fJobFile != null) {
            String sFileName = viewModel_fragment_workerConsole.fJobFile.getName();
            getActivity().setTitle(sFileName);

            String sLogFileAbsolutePath = viewModel_fragment_workerConsole.fJobFile.getAbsolutePath();

            //Get data from file:
            BufferedReader brReader;
            try {
                brReader = new BufferedReader(new FileReader(sLogFileAbsolutePath));
                StringBuilder sb = new StringBuilder();
                String sLine = brReader.readLine();
                while (sLine != null) {
                    sb.append(sLine);
                    sb.append("\n");
                    sLine = brReader.readLine();
                }
                brReader.close();
                textView_JobText.setText(sb.toString());

            } catch (IOException e) {
                String sFailureMessage = "Problem reading log file: " + sLogFileAbsolutePath;
                Toast.makeText(getActivity(), sFailureMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}