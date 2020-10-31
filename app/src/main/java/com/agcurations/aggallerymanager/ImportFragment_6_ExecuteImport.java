package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImportFragment_6_ExecuteImport#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImportFragment_6_ExecuteImport extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ImportFragment_6_ExecuteImport() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentImport_6_ExecuteImport.
     */
    // TODO: Rename and change types and number of parameters
    public static ImportFragment_6_ExecuteImport newInstance(String param1, String param2) {
        ImportFragment_6_ExecuteImport fragment = new ImportFragment_6_ExecuteImport();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.import_fragment_6_execute_import, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void initComponents(){

        //Init progress:
        ProgressBar progressBar_ImportProgress = getView().findViewById(R.id.progressBar_ImportProgress);
        progressBar_ImportProgress.setProgress(0);
        TextView textView_ImportProgress = getView().findViewById(R.id.textView_ImportProgress);
        textView_ImportProgress.setText("0%");

        //Set the log textView to be able to scroll vertically:
        TextView textView_ImportLog = getView().findViewById(R.id.textView_ImportLog);
        textView_ImportLog.setMovementMethod(new ScrollingMovementMethod());
        //Init log:
        textView_ImportLog.setText("");


    }

}