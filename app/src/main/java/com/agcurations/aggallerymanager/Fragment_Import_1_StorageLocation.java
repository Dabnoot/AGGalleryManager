package com.agcurations.aggallerymanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Fragment_Import_1_StorageLocation extends Fragment {

    public static final int MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE = 2002;

    GlobalClass globalClass;

    ViewModel_ImportActivity viewModelImportActivity;

    ProgressBar gProgressBar_FileAnalysisProgress;
    TextView gTextView_FileAnalysisProgressBarText;
    TextView gTextView_FileAnalysisDebugLog;
    LinearLayout gLinearLayout_Progress;
    Button gbutton_FolderSelectComplete;

    boolean gbIncludeGraphicsAttributesInFileQuery = false;

    public Fragment_Import_1_StorageLocation() {
        // Required empty public constructor
    }

    public static Fragment_Import_1_StorageLocation newInstance() {
        return new Fragment_Import_1_StorageLocation();
    }

    ImportDataServiceResponseReceiver importDataServiceResponseReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);

            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter(ImportDataServiceResponseReceiver.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importDataServiceResponseReceiver = new ImportDataServiceResponseReceiver();
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(importDataServiceResponseReceiver, filter);
        }

    }

    @Override
    public void onDestroy() {
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(importDataServiceResponseReceiver);
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_import_1_storage_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        if(getView() == null){
            return;
        }

        //Determine Checkbox state for importing graphics file data (which increases the time of the folder processing):
        if(getActivity() != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            gbIncludeGraphicsAttributesInFileQuery = sharedPreferences.getBoolean(GlobalClass.gsPreference_Import_IncludeGraphicsFileData, false);
        }
        final CheckBox checkBox_IncludeGraphicsAttributes = getView().findViewById(R.id.checkBox_IncludeGraphicsAttributes);
        checkBox_IncludeGraphicsAttributes.setChecked(gbIncludeGraphicsAttributesInFileQuery);
        checkBox_IncludeGraphicsAttributes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gbIncludeGraphicsAttributesInFileQuery = ((CheckBox) v).isChecked();
                if(getActivity() != null) {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                    sharedPreferences.edit()
                            .putBoolean(GlobalClass.gsPreference_Import_IncludeGraphicsFileData, gbIncludeGraphicsAttributesInFileQuery)
                            .apply();
                }
            }
        });
        TextView textView_Label_IncludeGraphicsAttributes = getView().findViewById(R.id.textView_Label_IncludeGraphicsAttributes);
        textView_Label_IncludeGraphicsAttributes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipGraphicsCheckboxState(checkBox_IncludeGraphicsAttributes);
            }
        });
        TextView textView_Label_IncludeGraphicsAttributes_SubText = getView().findViewById(R.id.textView_Label_IncludeGraphicsAttributes_SubText);
        textView_Label_IncludeGraphicsAttributes_SubText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipGraphicsCheckboxState(checkBox_IncludeGraphicsAttributes);
            }
        });





        Button button_SelectFolder = getView().findViewById(R.id.button_SelectFolder);
        button_SelectFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(GlobalClass.gbImportFolderAnalysisRunning){
                    GlobalClass.gbImportFolderAnalysisStop = true;
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


        gProgressBar_FileAnalysisProgress = getView().findViewById(R.id.progressBar_FileAnalysisProgress);
        gProgressBar_FileAnalysisProgress.setMax(1000);
        gTextView_FileAnalysisProgressBarText = getView().findViewById(R.id.textView_FileAnalysisProgressBarText);
        gbutton_FolderSelectComplete = getView().findViewById(R.id.button_FolderSelectComplete);

        gTextView_FileAnalysisDebugLog = getView().findViewById(R.id.textView_FileAnalysisDebugLog);
        if (gTextView_FileAnalysisDebugLog != null) {
            gTextView_FileAnalysisDebugLog.setMovementMethod(new ScrollingMovementMethod());
        }



    }

    private void flipGraphicsCheckboxState(CheckBox checkBox_IncludeGraphicsAttributes){
        boolean bCheckedState = checkBox_IncludeGraphicsAttributes.isChecked();
        checkBox_IncludeGraphicsAttributes.setChecked(!bCheckedState);
        gbIncludeGraphicsAttributesInFileQuery = !bCheckedState;
        if(getActivity() != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            sharedPreferences.edit()
                    .putBoolean(GlobalClass.gsPreference_Import_IncludeGraphicsFileData, gbIncludeGraphicsAttributesInFileQuery)
                    .apply();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() == null || getView() == null) {
            return;
        }

        getActivity().setTitle("Import");
        ActionBar actionBar =((AppCompatActivity) getActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.show();
        }

        if(!GlobalClass.gbImportHoldingFolderAnalysisAutoStart) {

            if (viewModelImportActivity.bImportCategoryChange) {
                //Reset all the stuff so that it looks like time to select a folder:
                if (GlobalClass.gbImportFolderAnalysisRunning) {
                    GlobalClass.gbImportFolderAnalysisStop = true;
                }
                GlobalClass.gbImportFolderAnalysisFinished = false;

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
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) linearLayout_ButtonBar.getLayoutParams();
                lp.setMargins(0, 0, 0, 0); // left, top, right, bottom
                linearLayout_ButtonBar.setLayoutParams(lp);

                TextView textView_FileAnalysisDebugLog = getView().findViewById(R.id.textView_FileAnalysisDebugLog);
                if (textView_FileAnalysisDebugLog != null) {
                    textView_FileAnalysisDebugLog.setText("");
                    textView_FileAnalysisDebugLog.setVisibility(View.INVISIBLE);
                }

            } else {
                if (GlobalClass.gbImportFolderAnalysisRunning || GlobalClass.gbImportFolderAnalysisFinished) {

                    //Make some more space to show the progress bar:
                    LinearLayout linearLayout_ButtonBar = getView().findViewById(R.id.linearLayout_ButtonBar);
                    ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) linearLayout_ButtonBar.getLayoutParams();
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

                    if(globalClass.gsbImportFolderAnalysisLog.length() > 0) {
                        gTextView_FileAnalysisDebugLog.setVisibility(View.VISIBLE);
                        gTextView_FileAnalysisDebugLog.setText(globalClass.gsbImportFolderAnalysisLog.toString());
                    }

                    if (GlobalClass.gbImportFolderAnalysisFinished) {
                        if (gbutton_FolderSelectComplete != null) {
                            gbutton_FolderSelectComplete.setEnabled(true);
                            viewModelImportActivity.bUpdateImportSelectList = true;
                        }
                    }

                }
            }
        } else {
            GlobalClass.gbImportHoldingFolderAnalysisAutoStart = false;
            //If the user has selected to import from the holding folder, move forward with processing.
            String sHoldingFolderPath = GlobalClass.gUriImageDownloadHoldingFolder.toString();
            ShowFolderAnalysisViews(sHoldingFolderPath);

            globalClass.gsbImportFolderAnalysisLog = new StringBuilder();

            if(getContext() == null) return;
            String sCallerID = "Service_Import:startActionGetHoldingFolderDirectoryContents()";
            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            Data dataGetHoldingFolderDirectoryContents = new Data.Builder()
                    .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                    .build();
            OneTimeWorkRequest otwrGetHoldingFolderDirectoryContents = new OneTimeWorkRequest.Builder(Worker_Import_GetHoldingFolderDirectoryContents.class)
                    .setInputData(dataGetHoldingFolderDirectoryContents)
                    .addTag(Worker_Import_GetHoldingFolderDirectoryContents.TAG_WORKER_IMPORT_GETHOLDINGFOLDERDIRECTORYCONTENTS) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getContext()).enqueue(otwrGetHoldingFolderDirectoryContents);

        }


    }

    ActivityResultLauncher<Intent> garlGetImportFolder = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @SuppressLint("WrongConstant")
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
                            return; //todo: create message.
                        }
                        Intent data = result.getData();
                        Uri treeUri = data.getData();
                        Activity_Import.guriImportTreeURI = treeUri;

                        if(treeUri == null){
                            return; //todo: create message.
                        }

                        final int takeFlags = data.getFlags() & (
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getActivity().getContentResolver().takePersistableUriPermission(treeUri,
                                takeFlags);

                        assert Activity_Import.guriImportTreeURI != null;
                        DocumentFile df1 = DocumentFile.fromTreeUri(getActivity(), Activity_Import.guriImportTreeURI);
                        if(df1 == null){
                            return; //todo: create message.
                        }
                        String sTreeUriSourceName = df1.getName(); //Get name of the selected folder for display purposes.
                        ShowFolderAnalysisViews(sTreeUriSourceName);


                        int iFilesOrFolders = GlobalClass.FILES_ONLY;
                        if((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) &&
                                viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER){
                            iFilesOrFolders = GlobalClass.FOLDERS_ONLY;
                        }

                        GlobalClass.gbImportFolderAnalysisRunning = true;
                        globalClass.gsbImportFolderAnalysisLog = new StringBuilder();
                        GlobalClass.gbImportFolderAnalysisFinished = false;

                        if(getContext() == null) return;
                        String sCallerID = "Service_Import:startActionGetDirectoryContents()";
                        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                        Data dataGetDirectoryContents = new Data.Builder()
                                .putString(GlobalClass.EXTRA_CALLER_ID, sCallerID)
                                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                                .putString(GlobalClass.EXTRA_IMPORT_TREE_URI, Activity_Import.guriImportTreeURI.toString())
                                .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, viewModelImportActivity.iImportMediaCategory)
                                .putInt(GlobalClass.EXTRA_FILES_OR_FOLDERS, iFilesOrFolders)
                                .putInt(GlobalClass.EXTRA_COMIC_IMPORT_SOURCE, viewModelImportActivity.iComicImportSource)
                                .build();
                        OneTimeWorkRequest otwrGetDirectoryContents = new OneTimeWorkRequest.Builder(Worker_Import_GetDirectoryContents.class)
                                .setInputData(dataGetDirectoryContents)
                                .addTag(Worker_Import_GetDirectoryContents.TAG_WORKER_IMPORT_GETDIRECTORYCONTENTS) //To allow finding the worker later.
                                .build();
                        WorkManager.getInstance(getContext()).enqueue(otwrGetDirectoryContents);
                    }
                }
            });


    private void ShowFolderAnalysisViews(String sImportFolderName){
        //Display the source name:
        if(getView() == null){
            return; //todo: create message.
        }
        TextView textView_Selected_Import_Folder = getView().findViewById(R.id.textView_Selected_Import_Folder);
        textView_Selected_Import_Folder.setText(sImportFolderName);
        globalClass.gsImportFolderAnalysisSelectedFolder = sImportFolderName;
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
    }


    public class ImportDataServiceResponseReceiver extends BroadcastReceiver {
        public static final String IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.IMPORT_DATA_SERVICE_STORAGE_LOCATION_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
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
                bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
                bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

                if(bUpdatePercentComplete){
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if(gProgressBar_FileAnalysisProgress != null) {
                        gProgressBar_FileAnalysisProgress.setProgress(iAmountComplete);
                    }
                }
                if(bUpdateProgressBarText){
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if(gTextView_FileAnalysisProgressBarText != null) {
                        gTextView_FileAnalysisProgressBarText.setText(sProgressBarText);
                    }
                }
                if(GlobalClass.gbImportFolderAnalysisFinished){
                    GlobalClass.gbImportFolderAnalysisFinished = false; //Prevent this GlobalClass var from allowing a re-run of this case.
                    if(gbutton_FolderSelectComplete != null) {
                        gbutton_FolderSelectComplete.setEnabled(true);
                        viewModelImportActivity.bUpdateImportSelectList = true;

                        if(gTextView_FileAnalysisDebugLog.getVisibility() == View.INVISIBLE) {
                            //Go ahead and move to the next fragment if there is no log data for the user.
                            gbutton_FolderSelectComplete.performClick();
                        }
                    }
                }

            }

        }
    }

}