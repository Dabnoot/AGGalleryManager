
package com.agcurations.aggallerymanager;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Activity_Main extends AppCompatActivity {

    //Global Variables:
    GlobalClass globalClass;

    MainActivityDataServiceResponseReceiver mainActivityDataServiceResponseReceiver;

    ProgressBar progressBar_WorkerTest;
    TextView textView_WorkerTest;
    Observer<WorkInfo> workInfoObserver_TrackingTest;

    ProgressBar gProgressBar_CatalogReadProgress;
    TextView gTextView_CatalogReadProgressBarText;

    boolean gbDataLoadComplete = false;

    boolean bSingleUserInUse = false;

    Activity_Main AM;



    //Configure the thing that asks the user to select a data folder:
    ActivityResultLauncher<Intent> garlPromptForDataFolder = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @SuppressLint("WrongConstant")
                @Override
                public void onActivityResult(ActivityResult result) {
                    // look for permissions before executing operations.
                    if(AM == null){
                        return;
                    }

                    //Check to make sure that we have read/write permission in the selected folder.
                    //If we don't have permission, request it.
                    if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) ||
                            (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED)) {

                        // Permission is not granted
                        // Should we show an explanation?
                        if ((ActivityCompat.shouldShowRequestPermissionRationale(AM,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                                (ActivityCompat.shouldShowRequestPermissionRationale(AM,
                                        Manifest.permission.READ_EXTERNAL_STORAGE))) {
                            // Show an explanation to the user *asynchronously* -- don't block
                            // this thread waiting for the user's response! After the user
                            // sees the explanation, try again to request the permission.
                            Toast.makeText(getApplicationContext(), "Permission required for read/write operation.", Toast.LENGTH_LONG).show();
                        } else {
                            // No explanation needed; request the permission
                            ActivityCompat.requestPermissions(AM,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_EXTERNAL_STORAGE},
                                    Fragment_Import_1_StorageLocation.MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE);

                            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
                        }
                        //} else {
                        // Permission has already been granted
                    }

                    //The above code checked for permission, and if not granted, requested it.
                    //  Check one more time to see if the permission was granted:

                    if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) &&
                            (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                                    == PackageManager.PERMISSION_GRANTED)) {
                        //If we now have permission...
                        //The result data contains a URI for the directory that
                        //the user selected.

                        //Put the import Uri into the intent (this could represent a folder OR a file:

                        if(result.getData() == null) {
                            return;
                        }
                        Intent data = result.getData();
                        Uri treeUri = data.getData();
                        if(treeUri == null) {
                            return;
                        }
                        final int takeFlags = data.getFlags() &
                                (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        //We must persist access to this folder or the user will be asked everytime to select a folder.
                        //  Even then, they well still have to re-access the location on device restart.
                        GlobalClass.gcrContentResolver.takePersistableUriPermission(treeUri, takeFlags);

                        //Call a routine to initialize the data folder:
                        Worker_Catalog_LoadData.initDataFolder(treeUri, getApplicationContext());


                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());

        //Return theme away from startup_screen
        setTheme(R.style.MainTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AM = this;

        GlobalClass.gcrContentResolver = getContentResolver();

        ActionBar AB = getSupportActionBar();
        if(AB != null) {
            AB.show();
        }

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        try {
            GlobalClass.gsFileSeparator = URLEncoder.encode(File.separator, StandardCharsets.UTF_8.toString());
        } catch (Exception e){
            String sMessage = "" + e.getMessage();
            Log.d("Activity_Main:onCreate()", sMessage);
        }


        gProgressBar_CatalogReadProgress = findViewById(R.id.progressBar_CatalogReadProgress);
        gTextView_CatalogReadProgressBarText = findViewById(R.id.textView_CatalogReadProgressBarText);

        //Configure a response receiver to listen for updates from the Main Activity (MA) Data Service:
        //  This will load the tags files for videos, pictures, and comics.
        IntentFilter filter = new IntentFilter(MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        mainActivityDataServiceResponseReceiver = new MainActivityDataServiceResponseReceiver();
        //registerReceiver(mainActivityDataServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mainActivityDataServiceResponseReceiver, filter);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Get user listing right away:
        //Clear the data (used during debugging):
        /*{
            Set<String> ssUserAccountData = new HashSet<>();
            sharedPreferences.edit()
                    .putStringSet(GlobalClass.gsPreferenceName_UserAccountData, ssUserAccountData)
                    .apply();
        }*/

        globalClass.galicu_Users = new ArrayList<>();
        Set<String> ssUserAccountData = sharedPreferences.getStringSet(GlobalClass.gsPreferenceName_UserAccountData, null);

        if(ssUserAccountData != null){
            if(ssUserAccountData.size() > 0) {
                //If user account data is ok, populate:
                for (String sUserAccountDataRecord : ssUserAccountData) {
                    ItemClass_User icu = GlobalClass.ConvertRecordStringToUserItem(sUserAccountDataRecord);
                    globalClass.galicu_Users.add(icu);
                }
                bSingleUserInUse = globalClass.galicu_Users.size() == 1;
                if(bSingleUserInUse && globalClass.galicu_Users.get(0).sPin.equals("")){
                    //If there is only one user and the pin is not set, log that user in.
                    globalClass.gicuCurrentUser = globalClass.galicu_Users.get(0);
                } else if(bSingleUserInUse && !globalClass.galicu_Users.get(0).sPin.equals("")){
                    Toast.makeText(this, "Welcome guest", Toast.LENGTH_SHORT).show();
                }
            } else {
                AddDefaultAdminUser(sharedPreferences);
            }
        } else {
            AddDefaultAdminUser(sharedPreferences);
        }




        //Get the data storage location Preference:
        String sDataStorageLocationURI = sharedPreferences.getString(GlobalClass.gsPreference_DataStorageLocationUri, null);
        if(sDataStorageLocationURI != null){
            if(!sDataStorageLocationURI.equals("")){
                //Confirm that the URI string points to a valid location.
                Uri uriDataStorageLocation = Uri.parse(sDataStorageLocationURI);

                //Examine to make sure that we have the proper folder.
                //  Testing showed that the system gave the data folder's parent folder, which is
                //  the folder that the user selected, rather than the folder that this program
                //  created within that folder AND saved to the DataStorageLocation preference.
                if(uriDataStorageLocation != null) {
                    String sDataStorageLocationFileName = GlobalClass.GetFileName(uriDataStorageLocation.toString());
                    if(sDataStorageLocationFileName.equals(GlobalClass.gsDataFolderBaseName)){
                        //If the folder has the proper name, use this folder.
                        GlobalClass.gUriDataFolder = uriDataStorageLocation;
                    } else {
                        if (GlobalClass.CheckIfFileExists(uriDataStorageLocation, GlobalClass.gsDataFolderBaseName)){
                            //If the DataFolderBaseName folder was found within the preCheck folder,
                            //  use the found folder.
                            GlobalClass.gUriDataFolder = globalClass.FormChildUri(uriDataStorageLocation.toString(), GlobalClass.gsDataFolderBaseName);
                        }
                    }

                }
            }
        }

        boolean bInitDataFolder = false;
        if(GlobalClass.gUriDataFolder == null){
            bInitDataFolder = true;
        } else {
            bInitDataFolder = !globalClass.CheckIfFileExists(GlobalClass.gUriDataFolder );
        }
        if(bInitDataFolder){
            //If the storage location data does not exist, such as when the user has not yet selected a location,
            //  prompt the user to select a location.

            //First present an Alert Dialog informing the user that they must select a storage location before continuing.
            //  The user must select a storage location to prevent the loss of data in the event that
            //  the application is uninstalled.
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustomStyle);
            builder.setTitle("First Time Use - Please Select Data Folder");
            String sMessage = "Please select a folder in which to store data.\n" +
                    "We suggest you create a new folder called 'Archive' on an SD card if available.\n" +
                    "A new storage location may be selected from the Settings menu.\n" +
                    "This app will not run properly without a selected external data storage location, \n" +
                    "otherwise data may be lost.";
            builder.setMessage(sMessage);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();

                    // Allow the user to choose a directory using the system's file picker.
                    Intent intent_DetermineDataFolder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                    // Provide write access to files and sub-directories in the user-selected directory:
                    intent_DetermineDataFolder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent_DetermineDataFolder.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent_DetermineDataFolder.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    //Start the activity:
                    garlPromptForDataFolder.launch(intent_DetermineDataFolder);
                }
            });
            AlertDialog adConfirmationDialog = builder.create();
            adConfirmationDialog.show();

        } else {
            //Call the MA Data Service, which will create a call to a service:
            Service_Main.startActionLoadData(this);
        }

        //AlertDialogTest2();

        //Put together a progressbar to indicate progress of a worker (originally incorporated to track video concatenation):
        progressBar_WorkerTest = findViewById(R.id.progressBar_WorkerTest);
        progressBar_WorkerTest.setMax(100);
        textView_WorkerTest = findViewById(R.id.textView_WorkerTest);


        ImageButton imageButton_DayNight = findViewById(R.id.imageButton_DayNight);
        imageButton_DayNight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!globalClass.gbIsDarkModeOn) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    globalClass.gbIsDarkModeOn = true;
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    globalClass.gbIsDarkModeOn = false;
                }
            }
        });

        ImageButton imageButton_Browser = findViewById(R.id.imageButton_Browser);
        imageButton_Browser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentBrowser = new Intent(getApplicationContext(), Activity_Browser.class);
                startActivity(intentBrowser);
            }
        });


        if(!GlobalClass.gbOptionUserAutoLogin && !bSingleUserInUse){
            //If the user has not set the option to auto-login a user, then show the user selection
            //  activity:

            Intent intentUserSelection = new Intent(getApplicationContext(), Activity_UserSelection.class);
            startActivity(intentUserSelection);

        }






    }



    private void AddDefaultAdminUser(SharedPreferences sharedPreferences){
        ItemClass_User icu_DefaultUser = new ItemClass_User();
        icu_DefaultUser.sUserName = "Admin";
        icu_DefaultUser.sPin = "";
        icu_DefaultUser.bAdmin = true;
        icu_DefaultUser.iUserIconColor = R.color.colorStatusBar;
        icu_DefaultUser.iMaturityLevel = AdapterMaturityRatings.MATURITY_RATING_X;
        globalClass.galicu_Users.add(icu_DefaultUser);
        globalClass.gicuCurrentUser = icu_DefaultUser;
        bSingleUserInUse = true;

        //Add data to preferences:
        String sUserRecord = GlobalClass.getUserAccountRecordString(icu_DefaultUser);
        Set<String> ssUserAccountDataDefault = new HashSet<>();
        ssUserAccountDataDefault.add(sUserRecord);
        sharedPreferences.edit()
                .putStringSet(GlobalClass.gsPreferenceName_UserAccountData, ssUserAccountDataDefault)
                .apply();
    }

    private void AlertDialogTest2(){
        //Testing of AlertDialog style:
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustomStyle);

        // set the custom layout
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_layout_pin_code, null);
        builder.setView(customLayout);

        final AlertDialog adConfirmationDialog = builder.create();

        //Code action for the Cancel button:
        Button button_PinCodeCancel = customLayout.findViewById(R.id.button_PinCodeCancel);
        button_PinCodeCancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                adConfirmationDialog.dismiss();
            }
        });

        //Code action for the OK button:
        Button button_PinCodeOK = customLayout.findViewById(R.id.button_PinCodeOK);
        button_PinCodeOK.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                String sPinEntered = editText_DialogInput.getText().toString();

                if(sPinEntered.equals(globalClass.gsPin)){
                    Toast.makeText(getApplicationContext(), "Correct pin entered.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                }

                adConfirmationDialog.dismiss();
            }
        });

        adConfirmationDialog.show();
    }

    private void AlertDialogTest1(){
        //Testing of AlertDialog style:
        String sConfirmationMessage = "Confirm item: Test test test test test test test";
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustomStyle);
        builder.setTitle("Delete Tag");
        builder.setMessage(sConfirmationMessage);
        // Set up the input
        final EditText editText_DialogInput = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        editText_DialogInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        builder.setView(editText_DialogInput);
        //builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog adConfirmationDialog = builder.create();
        adConfirmationDialog.show();
    }





    public class MainActivityDataServiceResponseReceiver extends BroadcastReceiver {
        //MADataService = Main Activity Data Service
        public static final String MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_MAIN_ACTIVITY_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {
                String sStatusMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_STATUS_MESSAGE);
                if(sStatusMessage != null){
                    Toast.makeText(context, sStatusMessage, Toast.LENGTH_LONG).show();
                }
            }

            //Check to see if this is a response to update log or progress bar:
            boolean 	bUpdatePercentComplete;
            boolean 	bUpdateProgressBarText;

            //Get booleans from the intent telling us what to update:
            bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
            bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

            if(gProgressBar_CatalogReadProgress != null && gTextView_CatalogReadProgressBarText != null) {
                if (bUpdatePercentComplete) {
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if (gProgressBar_CatalogReadProgress != null) {
                        gProgressBar_CatalogReadProgress.setProgress(iAmountComplete);
                    }
                    if (iAmountComplete == 100) {
                        gProgressBar_CatalogReadProgress.setVisibility(View.INVISIBLE);
                        gTextView_CatalogReadProgressBarText.setVisibility(View.INVISIBLE);
                        gbDataLoadComplete = true;
                    } else {
                        gProgressBar_CatalogReadProgress.setVisibility(View.VISIBLE);
                        gTextView_CatalogReadProgressBarText.setVisibility(View.VISIBLE);
                    }

                }
                if (bUpdateProgressBarText) {
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if (gTextView_CatalogReadProgressBarText != null) {
                        gTextView_CatalogReadProgressBarText.setText(sProgressBarText);
                    }
                }
            }

        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mainActivityDataServiceResponseReceiver);
        super.onDestroy();
    }

    //=====================================================================================
    //===== Menu Code =================================================================
    //=====================================================================================
    Menu optionsMenu;
    public boolean onCreateOptionsMenu(Menu menu) {
        optionsMenu = menu;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);

        if(globalClass.gicuCurrentUser != null){
            MenuItem menuItemLogin = menu.findItem(R.id.icon_login);
            if(menuItemLogin != null){
                setUserColor(menuItemLogin, globalClass.gicuCurrentUser.iUserIconColor);
            }
        }

        return true;
    }

    private Intent getMenuIntent(MenuItem item){
        if(item.getItemId() == R.id.menu_UserManagement) {
            return new Intent(getApplicationContext(), Activity_UserManagement.class);
        } else if(item.getItemId() == R.id.menu_Settings) {
            return new Intent(getApplicationContext(), Activity_AppSettings.class);
        } else if(item.getItemId() == R.id.menu_TagEditor) {
            return new Intent(getApplicationContext(), Activity_TagEditor.class);
        } else if (item.getItemId() == R.id.icon_login) {
            return new Intent(getApplicationContext(), Activity_UserSelection.class);
        }
        return null;
    }


    private void setUserColor(MenuItem item, int iColor){
        Drawable drawable = AppCompatResources.getDrawable(getApplicationContext(), R.drawable.login).mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(iColor, PorterDuff.Mode.SRC_IN));
        item.setIcon(drawable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if((item.getItemId() == R.id.menu_UserManagement)
            || (item.getItemId() == R.id.menu_Settings)
            || (item.getItemId() == R.id.menu_TagEditor)
            || (item.getItemId() == R.id.icon_login)){

            //Ask for pin code in order to allow access to feature if not admin:
            if (globalClass.gicuCurrentUser != null) {
                if (!globalClass.gicuCurrentUser.bAdmin) {
                    Toast.makeText(getApplicationContext(), "Feature requires admin credentials", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = getMenuIntent(item);
                    if (intent != null) {
                        startActivity(intent);
                    }
                }
            } else {
                if(item.getItemId() == R.id.icon_login){
                    //If the user has clicked the login icon, display the login screen:
                    Intent intentUserSelection = new Intent(getApplicationContext(), Activity_UserSelection.class);
                    startActivity(intentUserSelection);
                } else {
                    Toast.makeText(getApplicationContext(), "Feature requires admin credentials", Toast.LENGTH_SHORT).show();
                }
            }

        }

        if(item.getItemId() == R.id.menu_DatabaseBackup) {
            //Backup the database files (CatalogContents.dat):
            Toast.makeText(getApplicationContext(), "Initiating database backup.", Toast.LENGTH_SHORT).show();
            Service_Main.startActionCatalogBackup(this);
            return true;
        } else if(item.getItemId() == R.id.menu_WorkerConsole){
            Intent intentWorkerConsoleActivity = new Intent(this, Activity_WorkerConsole.class);
            startActivity(intentWorkerConsoleActivity);
            return true;
        } else if(item.getItemId() == R.id.menu_LogViewer) {
            Intent intentLogViewerActivity = new Intent(this, Activity_LogViewer.class);
            startActivity(intentLogViewerActivity);
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }


    }



    @Override
    public void onResume(){
        super.onResume();
        if(optionsMenu != null) {
            MenuItem menuItemLogin = optionsMenu.findItem(R.id.icon_login);
            if (menuItemLogin != null) {
                if (globalClass.gicuCurrentUser != null) {
                    setUserColor(menuItemLogin, globalClass.gicuCurrentUser.iUserIconColor);
                }
            }
        }


        //Create a generic observer to be assigned to any active video concatenation workers (shows the progress of the worker):
        workInfoObserver_TrackingTest = new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null) {
                    Data progress = workInfo.getProgress();
                    long lProgressNumerator = progress.getLong(Worker_TrackingTest.WORKER_BYTES_PROCESSED, 0);
                    long lProgressDenominator = progress.getLong(Worker_TrackingTest.WORKER_BYTES_TOTAL, 100);
                    int iProgressBarValue = Math.round((lProgressNumerator / (float) lProgressDenominator) * 100);
                    String sWorkerID = progress.getString(Worker_TrackingTest.WORKER_ID);

                    if(progressBar_WorkerTest != null && textView_WorkerTest != null)
                        if (workInfo.getState() == WorkInfo.State.RUNNING) {
                            progressBar_WorkerTest.setVisibility(View.VISIBLE);
                            progressBar_WorkerTest.setProgress(iProgressBarValue);
                            if (sWorkerID != null) {
                                textView_WorkerTest.setVisibility(View.VISIBLE);
                                textView_WorkerTest.setText(sWorkerID);
                            }
                        } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            progressBar_WorkerTest.setVisibility(View.INVISIBLE);
                            progressBar_WorkerTest.setProgress(0);
                            textView_WorkerTest.setVisibility(View.INVISIBLE);
                            textView_WorkerTest.setText("");
                        }
                }
            }
        };

        //Look to see if there are any workers out there processing data for AGGalleryManager,
        //  and if so, attempt to listen to their progress:
        ListenableFuture<List<WorkInfo>> lfListWorkInfo = WorkManager.getInstance(getApplicationContext()).getWorkInfosByTag(Worker_TrackingTest.WORKER_TRACKING_TEST_TAG);
        try {
            int iWorkerCount = lfListWorkInfo.get().size();
            for(int i = 0; i < iWorkerCount; i++) {
                WorkInfo.State stateWorkState = lfListWorkInfo.get().get(i).getState();
                UUID UUIDWorkID = lfListWorkInfo.get().get(i).getId();
                Log.d("Workstate", stateWorkState.toString() + ", ID " + UUIDWorkID.toString());
                if(stateWorkState == WorkInfo.State.RUNNING || stateWorkState == WorkInfo.State.ENQUEUED) {

                    WorkManager wm = WorkManager.getInstance(getApplicationContext());
                    LiveData<WorkInfo> ldWorkInfo = wm.getWorkInfoByIdLiveData(UUIDWorkID);
                    ldWorkInfo.observe(this, workInfoObserver_TrackingTest);
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }






    }

    //=====================================================================================
    //===== ImageView Click Code =================================================================
    //=====================================================================================

    public void startVideoCatalogActivity(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getApplicationContext(), "Opening Videos catalog...", Toast.LENGTH_SHORT).show();
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_VIDEOS);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        startActivity(intentCatalogActivity);
    }

    public void startPicturesCatalogActivity(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getApplicationContext(), "Opening Images catalog...", Toast.LENGTH_SHORT).show();
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        startActivity(intentCatalogActivity);
    }

    public void startComicsCatalogActivity(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getApplicationContext(), "Opening Comics catalog...", Toast.LENGTH_SHORT).show();
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        startActivity(intentCatalogActivity);
    }

    public void startImportVideos(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_VIDEOS); //todo: Redundant
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        startActivity(intentImportGuided);
    }

    public void startImportImages(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_IMAGES); //todo: Redundant
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        startActivity(intentImportGuided);
    }

    public void startImportComics(View v){
        if(!gbDataLoadComplete){
            Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_COMICS); //todo: Redundant
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        startActivity(intentImportGuided);
    }


    public void buttonTestClick_Test(View v){

    }


}