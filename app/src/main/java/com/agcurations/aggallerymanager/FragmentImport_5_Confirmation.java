package com.agcurations.aggallerymanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentImport_5_Confirmation#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentImport_5_Confirmation extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public FragmentImport_5_Confirmation() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentImport_4_Confirmation.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentImport_5_Confirmation newInstance(String param1, String param2) {
        FragmentImport_5_Confirmation fragment = new FragmentImport_5_Confirmation();
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
        return inflater.inflate(R.layout.fragment_import_5_confirmation, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        initComponents();
    }

    public void initComponents(){

        if(getView() == null)
            return;


        //Populate the ListView with selected file names from an earlier step:
        ListView listView_FilesToImport = getView().findViewById(R.id.listView_FilesToImport);
        // Construct the data source
        ArrayList<String> alImportFileNames = new ArrayList<String>();
        int iFileCount = 0;
        long lRequiredStorageSpaceKB = 0L;
        for(ImportActivity.fileModel fm: ImportActivity.fileListCustomAdapter.alFileList){
            if(fm.isChecked) {
                alImportFileNames.add(fm.name);
                iFileCount++;
                lRequiredStorageSpaceKB += fm.size;
            }
        }

        ArrayAdapter<String> aaImportFileNames = new ArrayAdapter<>(getActivity().getApplicationContext(),R.layout.listview_singleline, alImportFileNames);
        listView_FilesToImport.setAdapter(aaImportFileNames);

        //Build and display the tag listing:
        StringBuilder sbTags = new StringBuilder();
        for (String s: ImportActivity.alsImportTags){
            sbTags.append(s);
            sbTags.append(", ");
        }
        String sTags = sbTags.toString();
        if (sTags.length() > 2) {
            sTags = sTags.substring(0,sTags.length()-2); //remove the trailing ", ".
        }
        TextView textView_TagsToApply = getView().findViewById(R.id.textView_TagsToApply);
        textView_TagsToApply.setText(sTags);

        //Display the file count:
        TextView textView_FileCount = getView().findViewById(R.id.textView_FileCount);
        String s = Integer.toString(iFileCount);
        textView_FileCount.setText(s);

        //Display the required space:
        TextView textView_RequiredStorageSpace = getView().findViewById(R.id.textView_RequiredStorageSpace);
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);
        s = decimalFormat.format(lRequiredStorageSpaceKB) + " KB";
        textView_RequiredStorageSpace.setText(s);

        //Display the available space:
        long lAvailableStorageSpaceKB = 0L;
        GlobalClass globalClass;
        globalClass = (GlobalClass) getActivity().getApplicationContext();
        lAvailableStorageSpaceKB = globalClass.AvailableStorageSpace(getActivity().getApplicationContext(), 1);
        TextView textView_AvailableStorageSpace = getView().findViewById(R.id.textView_AvailableStorageSpace);
        s = decimalFormat.format(lAvailableStorageSpaceKB) + " KB";
        textView_AvailableStorageSpace.setText(s);

        //Display the destination folder:
        String sDestinationFolder;
        sDestinationFolder = GlobalClass.gvfAppFolder.getAbsolutePath() +
                File.separator + ImportActivity.gsMediaCategoryFolderName +
                File.separator + ImportActivity.gsImportDestinationFolder;
        TextView textView_DestinationFolder = getView().findViewById(R.id.textView_DestinationFolder);
        textView_DestinationFolder.setText(sDestinationFolder);

    }
}