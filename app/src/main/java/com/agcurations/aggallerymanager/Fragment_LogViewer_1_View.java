package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Fragment_LogViewer_1_View extends Fragment {

    TextView textView_LogText;
    private ViewModel_Fragment_LogViewer viewModel_fragment_logViewer;


    public Fragment_LogViewer_1_View() {
        // Required empty public constructor
    }

    public static Fragment_LogViewer_1_View newInstance() {
        return new Fragment_LogViewer_1_View();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() == null){
            return;
        }

        //Instantiate the ViewModel sharing data between fragments:
        viewModel_fragment_logViewer = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_LogViewer.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_log_viewer_1_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(getView() == null){
            return;
        }
        textView_LogText = getView().findViewById(R.id.textView_LogText);
        textView_LogText.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public void onResume() {
        super.onResume();

        if(getActivity() == null){
            return;
        }

        if(viewModel_fragment_logViewer.fLogFile != null) {
            String sFileName = viewModel_fragment_logViewer.fLogFile.getName();
            getActivity().setTitle(sFileName);

            String sLogFileAbsolutePath = viewModel_fragment_logViewer.fLogFile.getAbsolutePath();

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
                textView_LogText.setText(sb.toString());

            } catch (IOException e) {
                String sFailureMessage = "Problem reading log file: " + sLogFileAbsolutePath;
                Toast.makeText(getActivity(), sFailureMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}