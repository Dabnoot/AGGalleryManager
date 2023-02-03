package com.agcurations.aggallerymanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

        if(viewModel_fragment_workerConsole.dfJobFile != null) {
            String sFileName = viewModel_fragment_workerConsole.dfJobFile.getName();
            getActivity().setTitle(sFileName);

            String sMessage;

            //Get data from file:

            try {
                InputStream isLogFile = GlobalClass.gcrContentResolver.openInputStream(viewModel_fragment_workerConsole.dfJobFile.getUri());
                if(isLogFile == null){
                    sMessage = "Problem opening log file: " + viewModel_fragment_workerConsole.dfJobFile.getUri();
                    Toast.makeText(getActivity(), sMessage, Toast.LENGTH_LONG).show();
                    return;
                }
                BufferedReader brReader = new BufferedReader(new InputStreamReader(isLogFile));
                StringBuilder sb = new StringBuilder();
                String sLine = brReader.readLine();
                while (sLine != null) {
                    sb.append(sLine);
                    sb.append("\n");
                    sLine = brReader.readLine();
                }
                brReader.close();
                isLogFile.close();
                textView_JobText.setText(sb.toString());

            } catch (IOException e) {
                sMessage = "Problem reading log file: " + viewModel_fragment_workerConsole.dfJobFile.getUri();
                Toast.makeText(getActivity(), sMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}