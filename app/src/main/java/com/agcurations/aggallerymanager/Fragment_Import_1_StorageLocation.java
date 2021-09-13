package com.agcurations.aggallerymanager;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_Import_1_StorageLocation#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_Import_1_StorageLocation extends Fragment {

    private static final int REQUEST_CODE_GET_IMPORT_FOLDER = 1000;
    public static final int MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE = 2002;

    GlobalClass globalClass;

    ViewModel_ImportActivity viewModelImportActivity;

    ProgressBar gProgressBar_FileAnalysisProgress;
    TextView gTextView_FileAnalysisProgressBarText;
    TextView gTextView_FileAnalysisDebugLog;
    LinearLayout gLinearLayout_Progress;
    Button gbutton_FolderSelectComplete;

    public Fragment_Import_1_StorageLocation() {
        // Required empty public constructor
    }

    public static Fragment_Import_1_StorageLocation newInstance() {
        return new Fragment_Import_1_StorageLocation();
    }

    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();
        //requireActivity().registerReceiver(importDataServiceResponseReceiver, filter);
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(importDataServiceResponseReceiver, filter);
        }
        if(getActivity() != null) {
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
        }

        if(getActivity()!=null) {
            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }
    }

    @Override
    public void onDestroy() {
        // unregister  like this
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(importDataServiceResponseReceiver);
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_1_storage_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        if(getView() == null){
            return;
        }
        Button button_SelectFolder = getView().findViewById(R.id.button_SelectFolder);
        button_SelectFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(globalClass.gbImportFolderAnalysisRunning){
                    globalClass.gbImportFolderAnalysisStop = true;
                }


                if(gTextView_FileAnalysisDebugLog != null){
                    gTextView_FileAnalysisDebugLog.setText("");
                }

                // Allow the user to choose a directory using the system's file picker.
                Intent intent_GetImportFromFolder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                // Provide write access to files and sub-directories in the user-selected directory:
                intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                //Start the activity:
                garlGetImportFolder.launch(intent_GetImportFromFolder);

            }
        });

        if(getView() != null) {
            gProgressBar_FileAnalysisProgress = getView().findViewById(R.id.progressBar_FileAnalysisProgress);
            gProgressBar_FileAnalysisProgress.setMax(1000);
            gTextView_FileAnalysisProgressBarText = getView().findViewById(R.id.textView_FileAnalysisProgressBarText);
            gbutton_FolderSelectComplete = getView().findViewById(R.id.button_FolderSelectComplete);

            gTextView_FileAnalysisDebugLog = getView().findViewById(R.id.textView_FileAnalysisDebugLog);
            if (gTextView_FileAnalysisDebugLog != null) {
                gTextView_FileAnalysisDebugLog.setMovementMethod(new ScrollingMovementMethod());
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() == null || getView() == null) {
            return;
        }

        getActivity().setTitle("Import");

        if(viewModelImportActivity.bImportCategoryChange){
            //Reset all the stuff so that it looks like time to select a folder:
            if(globalClass.gbImportFolderAnalysisRunning){
                globalClass.gbImportFolderAnalysisStop = true;
            }
            globalClass.gbImportFolderAnalysisFinished = false;

            viewModelImportActivity.bImportCategoryChange = false;
            gProgressBar_FileAnalysisProgress.setProgress(0);
            gTextView_FileAnalysisProgressBarText.setText("0/0");

            TextView textView_Selected_Import_Folder = getView().findViewById(R.id.textView_Selected_Import_Folder);
            textView_Selected_Import_Folder.setText("");
            TextView textView_Label_Selected_Folder = getView().findViewById(R.id.textView_Label_Selected_Folder);
            textView_Label_Selected_Folder.setVisibility(View.INVISIBLE);
            textView_Selected_Import_Folder.setVisibility(View.INVISIBLE);
            gLinearLayout_Progress = getView().findViewById(R.id.linearLayout_Progress);
            gLinearLayout_Progress.setVisibility(View.INVISIBLE);

            gbutton_FolderSelectComplete.setEnabled(false);

            //Make less space to cover the hidden progress bar:
            LinearLayout linearLayout_ButtonBar = getView().findViewById(R.id.linearLayout_ButtonBar);
            ConstraintLayout.LayoutParams lp =  (ConstraintLayout.LayoutParams) linearLayout_ButtonBar.getLayoutParams();
            lp.setMargins(0, 0, 0, 0); // left, top, right, bottom
            linearLayout_ButtonBar.setLayoutParams(lp);

            TextView textView_FileAnalysisDebugLog = getView().findViewById(R.id.textView_FileAnalysisDebugLog);
            if(textView_FileAnalysisDebugLog != null){
                textView_FileAnalysisDebugLog.setText("");
                textView_FileAnalysisDebugLog.setVisibility(View.INVISIBLE);
            }

        } else {
            if(globalClass.gbImportFolderAnalysisRunning || globalClass.gbImportFolderAnalysisFinished){

                //Make some more space to show the progress bar:
                LinearLayout linearLayout_ButtonBar = getView().findViewById(R.id.linearLayout_ButtonBar);
                ConstraintLayout.LayoutParams lp =  (ConstraintLayout.LayoutParams) linearLayout_ButtonBar.getLayoutParams();
                lp.setMargins(0, 130, 0, 0); // left, top, right, bottom
                linearLayout_ButtonBar.setLayoutParams(lp);

                TextView textView_Label_Selected_Folder = getView().findViewById(R.id.textView_Label_Selected_Folder);
                textView_Label_Selected_Folder.setVisibility(View.VISIBLE);

                gLinearLayout_Progress = getView().findViewById(R.id.linearLayout_Progress);
                gLinearLayout_Progress.setVisibility(View.VISIBLE);

                TextView textView_Selected_Import_Folder = getView().findViewById(R.id.textView_Selected_Import_Folder);
                textView_Selected_Import_Folder.setVisibility(View.VISIBLE);
                textView_Selected_Import_Folder.setText(globalClass.gsImportFolderAnalysisSelectedFolder);

                gProgressBar_FileAnalysisProgress.setVisibility(View.VISIBLE);
                gProgressBar_FileAnalysisProgress.setProgress(globalClass.giImportFolderAnalysisProgressBarPercent);

                gTextView_FileAnalysisProgressBarText.setVisibility(View.VISIBLE);
                gTextView_FileAnalysisProgressBarText.setText(globalClass.gsImportFolderAnalysisProgressBarText);

                gTextView_FileAnalysisDebugLog.setVisibility(View.VISIBLE);
                gTextView_FileAnalysisDebugLog.setText(globalClass.gsbImportFolderAnalysisLog.toString());

                if(globalClass.gbImportFolderAnalysisFinished){
                    if(gbutton_FolderSelectComplete != null) {
                        gbutton_FolderSelectComplete.setEnabled(true);
                        viewModelImportActivity.bUpdateImportSelectList = true;
                    }
                }

            }
        }
    }

    ActivityResultLauncher<Intent> garlGetImportFolder = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    // look for permissions before executing operations.
                    if(getActivity() == null){
                        return;
                    }

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

                        if(result.getData() == null) {
                            return;
                        }
                        Intent data = result.getData();
                        Uri treeUri = data.getData();
                        Activity_Import.guriImportTreeURI = treeUri;
                        List<UriPermission> uriPermissionList;
                        uriPermissionList = getActivity().getContentResolver().getPersistedUriPermissions();
                        final int takeFlags = data.getFlags() & (
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getActivity().getContentResolver().takePersistableUriPermission(treeUri,
                                takeFlags);
                        uriPermissionList = getActivity().getContentResolver().getPersistedUriPermissions();

                        assert Activity_Import.guriImportTreeURI != null;
                        DocumentFile df1 = DocumentFile.fromTreeUri(getActivity(), Activity_Import.guriImportTreeURI);
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
                        globalClass.gsImportFolderAnalysisSelectedFolder = sTreeUriSourceName;
                        TextView textView_Label_Selected_Folder = getView().findViewById(R.id.textView_Label_Selected_Folder);
                        textView_Label_Selected_Folder.setVisibility(View.VISIBLE);
                        textView_Selected_Import_Folder.setVisibility(View.VISIBLE);
                        gLinearLayout_Progress = getView().findViewById(R.id.linearLayout_Progress);
                        gLinearLayout_Progress.setVisibility(View.VISIBLE);

                        //Make some more space to show the progress bar:
                        LinearLayout linearLayout_ButtonBar = getView().findViewById(R.id.linearLayout_ButtonBar);
                        ConstraintLayout.LayoutParams lp =  (ConstraintLayout.LayoutParams) linearLayout_ButtonBar.getLayoutParams();
                        lp.setMargins(0, 130, 0, 0); // left, top, right, bottom
                        linearLayout_ButtonBar.setLayoutParams(lp);

                        int iFilesOrFolders = Service_Import.FILES_ONLY;
                        if((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) &&
                                viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER){
                            iFilesOrFolders = Service_Import.FOLDERS_ONLY;
                        }

                        globalClass.gbImportFolderAnalysisRunning = true;
                        globalClass.gsbImportFolderAnalysisLog = new StringBuilder();
                        Service_Import.startActionGetDirectoryContents(getContext(),
                                Activity_Import.guriImportTreeURI,
                                viewModelImportActivity.iImportMediaCategory,
                                iFilesOrFolders,
                                viewModelImportActivity.iComicImportSource);


                    }
                }
            });


    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_Import.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_Import.EXTRA_STRING_PROBLEM);
                if(getView() != null) {
                    TextView textView_FileAnalysisDebugLog = getView().findViewById(R.id.textView_FileAnalysisDebugLog);
                    if (textView_FileAnalysisDebugLog != null) {
                        textView_FileAnalysisDebugLog.setVisibility(View.VISIBLE);
                        textView_FileAnalysisDebugLog.setText(globalClass.gsbImportFolderAnalysisLog.toString());
                    } else {
                        Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
                    }
                }
            } else {

                //Check to see if this is a response to request to get directory contents:
                boolean 	bUpdatePercentComplete;
                boolean 	bUpdateProgressBarText;

                //Get booleans from the intent telling us what to update:
                bUpdatePercentComplete = intent.getBooleanExtra(Service_Import.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(Service_Import.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(Service_Import.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_FileAnalysisProgress != null) {
                        gProgressBar_FileAnalysisProgress.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(Service_Import.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_FileAnalysisProgressBarText != null) {
                        gTextView_FileAnalysisProgressBarText.setText(sProgressBarText);
                    }
                }
                if(globalClass.gbImportFolderAnalysisFinished){
                    if(gbutton_FolderSelectComplete != null) {
                        gbutton_FolderSelectComplete.setEnabled(true);
                        viewModelImportActivity.bUpdateImportSelectList = true;
                    }
                }

            }

        }
    }

}