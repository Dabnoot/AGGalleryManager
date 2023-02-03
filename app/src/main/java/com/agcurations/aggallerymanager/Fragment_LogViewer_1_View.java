package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
    }

    @Override
    public void onResume() {
        super.onResume();

        if(getActivity() == null){
            return;
        }

        if(viewModel_fragment_logViewer.dfLogFile != null) {
            String sFileName = viewModel_fragment_logViewer.dfLogFile.getName();
            getActivity().setTitle(sFileName);

            String sMessage;

            //Get data from file:

            try {
                InputStream isLogFile = GlobalClass.gcrContentResolver.openInputStream(viewModel_fragment_logViewer.dfLogFile.getUri());
                if(isLogFile == null){
                    sMessage = "Problem opening log file: " + viewModel_fragment_logViewer.dfLogFile.getUri();
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
                textView_LogText.setText(sb.toString());

            } catch (IOException e) {
                sMessage = "Problem reading log file: " + viewModel_fragment_logViewer.dfLogFile.getUri();
                Toast.makeText(getActivity(), sMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}