package com.agcurations.aggallerymanager;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImportFragment_1_StorageLocation#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImportFragment_1_StorageLocation extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int REQUEST_CODE_GET_IMPORT_FOLDER = 1000;
    public static final int MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE = 2002;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ProgressBar gProgressBar_FileAnalysisProgress;
    TextView gTextView_FileAnalysisProgressBarText;
    RelativeLayout gRelativeLayout_Progress;
    Button gbutton_FolderSelectComplete;

    public ImportFragment_1_StorageLocation() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentImportStorageLocation_1.
     */
    // TODO: Rename and change types and number of parameters
    public static ImportFragment_1_StorageLocation newInstance(String param1, String param2) {
        ImportFragment_1_StorageLocation fragment = new ImportFragment_1_StorageLocation();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(ImportActivity.ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();
        //requireActivity().registerReceiver(importDataServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(importDataServiceResponseReceiver, filter);

    }

    @Override
    public void onDestroy() {
        // unregister  like this
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(importDataServiceResponseReceiver);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.import_fragment_1_storage_location, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if(getView() == null){
            return;
        }
        Button button_SelectFolder = getView().findViewById(R.id.button_SelectFolder);
        button_SelectFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Allow the user to choose a directory using the system's file picker.
                Intent intent_GetImportFromFolder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                // Provide write access to files and sub-directories in the user-selected directory:
                intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                //Start the activity:
                startActivityForResult(intent_GetImportFromFolder, REQUEST_CODE_GET_IMPORT_FOLDER);
            }
        });

        //Init progress:
        gProgressBar_FileAnalysisProgress = getView().findViewById(R.id.progressBar_FileAnalysisProgress);
        gProgressBar_FileAnalysisProgress.setProgress(0);
        gProgressBar_FileAnalysisProgress.setMax(100);
        gTextView_FileAnalysisProgressBarText = getView().findViewById(R.id.textView_FileAnalysisProgressBarText);
        gTextView_FileAnalysisProgressBarText.setText("0/0");
        gbutton_FolderSelectComplete = getView().findViewById(R.id.button_FolderSelectComplete);
    }


    @Override
    //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        //https://developer.android.com/training/data-storage/shared/documents-files

        super.onActivityResult(requestCode, resultCode, resultData);


        //If this is an EXPORT operation, and the data is not NULL,
        // look for permissions before executing operations.
        if(getActivity() == null){
            return;
        }
        if ((requestCode == REQUEST_CODE_GET_IMPORT_FOLDER && resultCode == Activity.RESULT_OK)
                && (resultData != null)){
            //Check to make sure that we have read/write permission in the selected folder.
            //If we don't have permission, request it.
            if ((ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED)) {

                // Permission is not granted
                // Should we show an explanation?
                if ((ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                        (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                                Manifest.permission.READ_EXTERNAL_STORAGE))) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    Toast.makeText(getActivity(), "Permission required for read/write operation.", Toast.LENGTH_LONG).show();
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
                //} else {
                // Permission has already been granted
            }


            //The above code checked for permission, and if not granted, requested it.
            //  Check one more time to see if the permission was granted:

            if ((ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED)) {
                //If we now have permission...
                //The result data contains a URI for the document or directory that
                //the user selected.

                //Put the import Uri into the intent (this could represent a folder OR a file:

                ImportActivity.guriImportTreeURI = resultData.getData();

                assert ImportActivity.guriImportTreeURI != null;
                DocumentFile df1 = DocumentFile.fromTreeUri(getActivity(), ImportActivity.guriImportTreeURI);
                if(df1 == null){
                    return;
                }
                String sTreeUriSourceName = df1.getName(); //Get name of the selected folder for display purposes.

                //Display the source name:
                if(getView() == null){
                    return;
                }
                TextView textView_Selected_Import_Folder = getView().findViewById(R.id.textView_Selected_Import_Folder);
                textView_Selected_Import_Folder.setText(sTreeUriSourceName);
                TextView textView_Label_Selected_Folder = getView().findViewById(R.id.textView_Label_Selected_Folder);
                textView_Label_Selected_Folder.setVisibility(View.VISIBLE);
                textView_Selected_Import_Folder.setVisibility(View.VISIBLE);
                gRelativeLayout_Progress = getView().findViewById(R.id.relativeLayout_Progress);
                gRelativeLayout_Progress.setVisibility(View.VISIBLE);

                ImportActivityDataService.startActionGetDirectoryContents(getContext(), ImportActivity.guriImportTreeURI, ImportActivity.giImportMediaCategory);


            }

        }

    }

    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String IMPORT_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_IMPORT_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            String sReceiver = intent.getStringExtra(ImportActivityDataService.RECEIVER_STRING);
            if(!sReceiver.contentEquals(ImportActivityDataService.RECEIVER_STORAGE_LOCATION)){
                return;
            }

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(ImportActivityDataService.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(ImportActivityDataService.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {

                //Check to see if this is a response to request to get directory contents:
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdatePercentComplete = intent.getBooleanExtra(ImportActivityDataService.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(ImportActivityDataService.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(ImportActivityDataService.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_FileAnalysisProgress != null) {
                        gProgressBar_FileAnalysisProgress.setProgress(iAmountComplete);
                    }
                    if(iAmountComplete == 100){
                        if(gbutton_FolderSelectComplete != null) {
                            gbutton_FolderSelectComplete.setEnabled(true);
                        }
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(ImportActivityDataService.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_FileAnalysisProgressBarText != null) {
                        gTextView_FileAnalysisProgressBarText.setText(sProgressBarText);
                    }
                }

            }

        }
    }

}