package com.agcurations.aggallerymanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


public class Fragment_Import_0b_ImageSource extends Fragment {


    public Fragment_Import_0b_ImageSource() {
        // Required empty public constructor
    }

    public static Fragment_Import_0b_ImageSource newInstance() {
        return new Fragment_Import_0b_ImageSource();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            //viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_0b_image_source, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() != null) {
            getActivity().setTitle("Import");
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }


        //todo: Determine the number of files in the holding folder and adjust the radiobutton text to show file count.
        GlobalClass globalClass = (GlobalClass) getActivity().getApplicationContext();
        ArrayList<String> sImageHoldingFolderFiles = GlobalClass.GetDirectoryFileNames(GlobalClass.gUriImageDownloadHoldingFolder);

        int iFileCount = 0;
        int iTotalFileCount = 0;

        for(String sFileName: sImageHoldingFolderFiles){
            String sExtension = sFileName.substring(sFileName.lastIndexOf("."));
            if(!sExtension.equals(".tad")){
                iFileCount++;
            }
            iTotalFileCount++;
        }

        RadioButton radioButton_ImageSourceHoldingFolder = getView().findViewById(R.id.radioButton_ImageSourceHoldingFolder);
        RadioGroup radioGroup_ImageSource = getView().findViewById(R.id.radioGroup_ImageSource);
        ViewGroup.LayoutParams vglp = radioGroup_ImageSource.getLayoutParams();
        if(iFileCount > 0){
            String sText = "Internal holding folder (" + iFileCount;
            if(iFileCount == 1) {
                sText = sText + " file)";
            } else {
                sText = sText + " files)";
            }
            radioButton_ImageSourceHoldingFolder.setText(sText);
            radioButton_ImageSourceHoldingFolder.setVisibility(View.VISIBLE);
            int iNewHeight = globalClass.ConvertDPtoPX(140);
            radioGroup_ImageSource.getLayoutParams().height = iNewHeight;
            radioGroup_ImageSource.requestLayout();
        } else {
            radioButton_ImageSourceHoldingFolder.setVisibility(View.INVISIBLE);
            int iNewHeight = globalClass.ConvertDPtoPX(90);
            radioGroup_ImageSource.getLayoutParams().height = iNewHeight;
            radioGroup_ImageSource.requestLayout();
        }





    }
}