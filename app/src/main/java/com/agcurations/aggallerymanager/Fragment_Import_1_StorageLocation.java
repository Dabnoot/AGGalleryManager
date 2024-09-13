package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Fragment_Import_1_StorageLocation extends Fragment {

    GlobalClass globalClass;

    ViewModel_ImportActivity viewModelImportActivity;

    ProgressBar gProgressBar_FileAnalysisProgress;
    TextView gTextView_FileAnalysisProgressBarText;
    TextView gTextView_FileAnalysisDebugLog;
    LinearLayout gLinearLayout_Progress;
    Button gButton_SelectFolder;
    Button gbutton_FolderSelectComplete;

    RelativeLayout gRelativeLayout_GraphicsAttributesInclusion;

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
        checkBox_IncludeGraphicsAttributes.setOnClickListener(v -> {
            gbIncludeGraphicsAttributesInFileQuery = ((CheckBox) v).isChecked();
            if(getActivity() != null) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                sharedPreferences.edit()
                        .putBoolean(GlobalClass.gsPreference_Import_IncludeGraphicsFileData, gbIncludeGraphicsAttributesInFileQuery)
                        .apply();
            }
        });
        TextView textView_Label_IncludeGraphicsAttributes = getView().findViewById(R.id.textView_Label_IncludeGraphicsAttributes);
        textView_Label_IncludeGraphicsAttributes.setOnClickListener(v -> flipGraphicsCheckboxState(checkBox_IncludeGraphicsAttributes));
        TextView textView_Label_IncludeGraphicsAttributes_SubText = getView().findViewById(R.id.textView_Label_IncludeGraphicsAttributes_SubText);
        textView_Label_IncludeGraphicsAttributes_SubText.setOnClickListener(v -> flipGraphicsCheckboxState(checkBox_IncludeGraphicsAttributes));





        gButton_SelectFolder = getView().findViewById(R.id.button_SelectFolder);
        gButton_SelectFolder.setOnClickListener(v -> {

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

        });


        gProgressBar_FileAnalysisProgress = getView().findViewById(R.id.progressBar_FileAnalysisProgress);
        gProgressBar_FileAnalysisProgress.setMax(1000);
        gTextView_FileAnalysisProgressBarText = getView().findViewById(R.id.textView_FileAnalysisProgressBarText);
        gbutton_FolderSelectComplete = getView().findViewById(R.id.button_FolderSelectComplete);

        gRelativeLayout_GraphicsAttributesInclusion = getView().findViewById(R.id.relativeLayout_GraphicsAttributesInclusion);

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

                //Reduce space occupied by the "Folder Results" section to improve visible balance:
                LinearLayout linearLayout_SelectedFolderResults = getView().findViewById(R.id.linearLayout_SelectedFolderResults);
                linearLayout_SelectedFolderResults.getLayoutParams().height = 0;

                TextView textView_FileAnalysisDebugLog = getView().findViewById(R.id.textView_FileAnalysisDebugLog);
                if (textView_FileAnalysisDebugLog != null) {
                    textView_FileAnalysisDebugLog.setText("");
                    textView_FileAnalysisDebugLog.setVisibility(View.INVISIBLE);
                }

            } else {
                if (GlobalClass.gbImportFolderAnalysisRunning || GlobalClass.gbImportFolderAnalysisFinished) {

                    //Show the "Folder Results" section previously reduced to improve visible balance:
                    LinearLayout linearLayout_SelectedFolderResults = getView().findViewById(R.id.linearLayout_SelectedFolderResults);
                    linearLayout_SelectedFolderResults.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;

                    TextView textView_Label_Selected_Folder = getView().findViewById(R.id.textView_Label_Selected_Folder);
                    textView_Label_Selected_Folder.setVisibility(View.VISIBLE);

                    gLinearLayout_Progress = getView().findViewById(R.id.linearLayout_Progress);
                    gLinearLayout_Progress.setVisibility(View.VISIBLE);

                    TextView textView_Selected_Import_Folder = getView().findViewById(R.id.textView_Selected_Import_Folder);
                    textView_Selected_Import_Folder.setVisibility(View.VISIBLE);
                    textView_Selected_Import_Folder.setText(GlobalClass.cleanHTMLCodedCharacters(globalClass.gsImportFolderAnalysisSelectedFolder));

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


            //Hide irrelevant UI items:
            gButton_SelectFolder.setLayoutParams(new LinearLayout.LayoutParams(0,0));
            gRelativeLayout_GraphicsAttributesInclusion.setLayoutParams(new LinearLayout.LayoutParams(0,0));
            /*gButton_SelectFolder.setVisibility(View.VISIBLE);

            gRelativeLayout_GraphicsAttributesInclusion.setVisibility(View.VISIBLE);
            gRelativeLayout_GraphicsAttributesInclusion.getLayoutParams().height = 0;
            gRelativeLayout_GraphicsAttributesInclusion.requestLayout();*/



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
            new ActivityResultCallback<>() {
                @SuppressLint("WrongConstant")
                @Override
                public void onActivityResult(ActivityResult result) {

                    if (getContext() == null) {
                        return;
                    }

                    //The result data contains a URI for the directory that
                    //the user selected.

                    //Put the import Uri into the intent (this could represent a folder OR a file:

                    if (result.getData() == null) {
                        Toast.makeText(getContext(),
                                "No data folder selected. A storage location may be selected from the Settings menu.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent data = result.getData();
                    Uri treeUri = data.getData();
                    if (treeUri == null) {
                        return;
                    }
                    final int takeFlags = data.getFlags() &
                            (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    //We must persist access to this folder or the user will be asked everytime to select a folder.
                    //  Even then, they well still have to re-access the location on device restart.
                    GlobalClass.gcrContentResolver.takePersistableUriPermission(treeUri, takeFlags);


                    Activity_Import.guriImportTreeURI = treeUri;

                    DocumentFile df1 = DocumentFile.fromTreeUri(getContext(), Activity_Import.guriImportTreeURI);
                    if (df1 == null) {
                        return; //todo: create message.
                    }
                    String sTreeUriSourceName = df1.getName(); //Get name of the selected folder for display purposes.
                    ShowFolderAnalysisViews(sTreeUriSourceName);


                    int iFilesOrFolders = GlobalClass.FILES_ONLY;
                    if ((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) &&
                            viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                        iFilesOrFolders = GlobalClass.FOLDERS_ONLY;
                    }

                    GlobalClass.gbImportFolderAnalysisRunning = true;
                    globalClass.gsbImportFolderAnalysisLog = new StringBuilder();
                    GlobalClass.gbImportFolderAnalysisFinished = false;

                    if (getContext() == null) return;
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
            });


    private void ShowFolderAnalysisViews(String sImportFolderName){
        //Display the source name:
        if(getView() == null){
            return; //todo: create message.
        }
        TextView textView_Selected_Import_Folder = getView().findViewById(R.id.textView_Selected_Import_Folder);
        textView_Selected_Import_Folder.setText(GlobalClass.cleanHTMLCodedCharacters(sImportFolderName));
        globalClass.gsImportFolderAnalysisSelectedFolder = sImportFolderName;
        TextView textView_Label_Selected_Folder = getView().findViewById(R.id.textView_Label_Selected_Folder);
        textView_Label_Selected_Folder.setVisibility(View.VISIBLE);
        textView_Selected_Import_Folder.setVisibility(View.VISIBLE);
        gLinearLayout_Progress = getView().findViewById(R.id.linearLayout_Progress);
        gLinearLayout_Progress.setVisibility(View.VISIBLE);

        //Show the "Folder Results" section previously reduced to improve visible balance:
        LinearLayout linearLayout_SelectedFolderResults = getView().findViewById(R.id.linearLayout_SelectedFolderResults);
        linearLayout_SelectedFolderResults.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
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