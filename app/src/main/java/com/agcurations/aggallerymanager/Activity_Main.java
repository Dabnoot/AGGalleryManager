
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
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.DocumentsContract;
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

import com.google.android.exoplayer2.util.MimeTypes;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

    boolean bSingleUserInUse = false;

    Activity_Main AM;

    boolean gbDataFolderSettingIgnored = true;


    //Configure the thing that asks the user to select a data folder:
    ActivityResultLauncher<Intent> garlPromptForDataFolder = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @SuppressLint("WrongConstant")
                @Override
                public void onActivityResult(ActivityResult result) {

                    //The result data contains a URI for the directory that
                    //the user selected.

                    //Put the import Uri into the intent (this could represent a folder OR a file:

                    if(result.getData() == null) {
                        Toast.makeText(getApplicationContext(),
                                "No data folder selected. A storage location may be selected from the Settings menu.",
                                Toast.LENGTH_LONG).show();
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
                    initDataFolder(treeUri, getApplicationContext());



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

        String sAndroidUserAgent = "Mozilla/5.0 (Linux; Android 13; SM-X200) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
        String sAndroidDesktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
        String sWindowsUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
        GlobalClass.giWebViewSettings_UserAgentString = "";

        gProgressBar_CatalogReadProgress = findViewById(R.id.progressBar_CatalogReadProgress);
        gTextView_CatalogReadProgressBarText = findViewById(R.id.textView_CatalogReadProgressBarText);

        //Configure a response receiver to listen for updates from the Main Activity (MA) Data Services:
        //  This will load the tags files for videos, pictures, and comics.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_LoadData.CATALOG_LOAD_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportComicFolders.IMPORT_COMIC_FOLDERS_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportFiles.IMPORT_FILES_ACTION_RESPONSE);
        filter.addAction(Worker_Import_ImportComicWebFiles.IMPORT_COMIC_WEB_FILES_ACTION_RESPONSE);
        filter.addAction(Worker_Import_VideoDownload.IMPORT_VIDEO_DOWNLOAD_ACTION_RESPONSE);
        filter.addAction(Worker_LocalFileTransfer.IMPORT_LOCAL_FILE_TRANSFER_ACTION_RESPONSE);
        filter.addAction(Worker_Catalog_BackupCatalogDBFiles.CATALOG_DATA_FILE_BACKUP_ACTION_RESPONSE);
        filter.addAction(Worker_Catalog_DeleteMultipleItems.DELETE_MULTIPLE_ITEMS_ACTION_RESPONSE);
        filter.addAction(GlobalClass.BROADCAST_WRITE_CATALOG_FILE);
        filter.addAction(Worker_User_Delete.USER_DELETE_ACTION_RESPONSE);
        filter.addAction(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);
        filter.addAction(Worker_DownloadPostProcessing.DOWNLOAD_POST_PROCESSING_ACTION_RESPONSE);
        filter.addAction(GlobalClass.BROADCAST_CATALOG_FILES_MAINTENANCE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        mainActivityDataServiceResponseReceiver = new MainActivityDataServiceResponseReceiver();
        //registerReceiver(mainActivityDataServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mainActivityDataServiceResponseReceiver, filter);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

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
                            GlobalClass.gUriDataFolder = GlobalClass.FormChildUri(uriDataStorageLocation.toString(), GlobalClass.gsDataFolderBaseName);
                        }
                    }

                }
            }
        }

        boolean bInitDataFolder;
        if(GlobalClass.gUriDataFolder == null){
            bInitDataFolder = true;
        } else {
            bInitDataFolder = !GlobalClass.CheckIfFileExists(GlobalClass.gUriDataFolder );
        }
        GlobalClass.galicu_Users = new ArrayList<>();
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
                    "A storage location may be selected from the Settings menu.\n" +
                    "This app will not run properly without a selected external data storage location, " +
                    "otherwise data may be lost.";
            builder.setMessage(sMessage);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    gbDataFolderSettingIgnored = false;
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
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if(gbDataFolderSettingIgnored) {
                        //If the user dismissed this dialog by not clicking the "ok" button, thus
                        //  did not select a storage location, remind them of the directions to
                        //  select a storage location.
                        Toast.makeText(getApplicationContext(),
                                "No data folder selected. A storage location may be selected from the Settings menu.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
            AlertDialog adConfirmationDialog = builder.create();
            adConfirmationDialog.show();

        } else {

            //Get user list immediately to allow login while the rest of the data loads.
            ReadUserData(getApplicationContext());
            if(GlobalClass.galicu_Users.size() == 1){
                bSingleUserInUse = true;
                if(GlobalClass.galicu_Users.get(0).sPin.equals("")){
                    //If there is only one user and the user does not have a pin assigned,
                    //  auto-login that user:
                    GlobalClass.gicuCurrentUser = GlobalClass.galicu_Users.get(0);
                }
            }

            if(!GlobalClass.gbOptionUserAutoLogin && !bSingleUserInUse){
                //If the user has not set the option to auto-login a user, then show the user selection
                //  activity:
                Intent intentUserSelection = new Intent(getApplicationContext(), Activity_UserSelection.class);
                startActivity(intentUserSelection);
            }

            //Call the MA Data Service, which will create a call to a service:
            startActionLoadData(getApplicationContext());
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




    }

    public static void initDataFolder(Uri treeUri, Context context){
        DocumentFile dfSelectedFolder = DocumentFile.fromTreeUri(context, treeUri);

        if(dfSelectedFolder == null){
            return;
        }

        String sUserFriendlyFolderPath = dfSelectedFolder.getUri().getPath();
        //Do what you gotta do to reflect back to the user a friendly path that they
        //  selected:
        //https://www.dev2qa.com/how-to-get-real-file-path-from-android-uri/
        Uri uriSelectedFolder = dfSelectedFolder.getUri();

        String uriAuthority = uriSelectedFolder.getAuthority();
        boolean isExternalStoreDoc = "com.android.externalstorage.documents".equals(uriAuthority);
        if(isExternalStoreDoc) {
            //Detect storage devices to determine if one is SD card:
            ArrayList<Worker_Catalog_LoadData.Storage> alsStorages = Worker_Catalog_LoadData.getStorages(context);

            String documentId = DocumentsContract.getDocumentId(uriSelectedFolder);

            String[] idArr = documentId.split(":");
            if(idArr.length == 2)
            {
                String type = idArr[0];
                String realDocId = idArr[1];

                String sStoragePrefix = "";
                if("primary".equalsIgnoreCase(type))
                {
                    sStoragePrefix = "Internal storage";
                } else {
                    for (Worker_Catalog_LoadData.Storage storage : alsStorages) {
                        if (storage.getName().contains(type)) {
                            sStoragePrefix = storage.sName;
                            break;
                        }
                    }
                }
                sUserFriendlyFolderPath = sStoragePrefix + "/" + realDocId;

            }
        }
        //With the user having specified a folder, identify/create the data folder within:
        Uri parentFolderUri = dfSelectedFolder.getUri();
        DocumentFile parentFolder = DocumentFile.fromTreeUri(context, parentFolderUri);
        if(parentFolder == null){
            return;
        }
        DocumentFile dfDataFolder = parentFolder.findFile(GlobalClass.gsDataFolderBaseName);
        if(dfDataFolder == null) {
            boolean bWeAreInTheGalleryFolder = false;
            if(parentFolder.getName() != null) {
                if (parentFolder.getName().equals(GlobalClass.gsDataFolderBaseName)){
                    bWeAreInTheGalleryFolder = true;
                }
            }
            if(!bWeAreInTheGalleryFolder) {
                dfDataFolder = parentFolder.createDirectory(GlobalClass.gsDataFolderBaseName);
            } else {
                dfDataFolder = parentFolder;
            }
        }

        sUserFriendlyFolderPath = sUserFriendlyFolderPath + File.separator + GlobalClass.gsDataFolderBaseName;

        if(dfDataFolder == null){
            Toast.makeText(context, "Unable to create working folder in selected directory.", Toast.LENGTH_LONG).show();
        } else {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            String sDataStorageLocationURI = dfDataFolder.getUri().toString();
            sharedPreferences.edit()
                    .putString(GlobalClass.gsPreference_DataStorageLocationUri, sDataStorageLocationURI)
                    .apply();
            sharedPreferences.edit()
                    .putString(GlobalClass.gsPreference_DataStorageLocationUriUF, sUserFriendlyFolderPath)
                    .apply();

            GlobalClass.gUriDataFolder = dfDataFolder.getUri();


            //Check to see if there is a user data file in the location:
            if(GlobalClass.CheckIfFileExists(GlobalClass.gUriDataFolder, GlobalClass.gsUserDataFileName)){

                GlobalClass.gUriUserDataFile = GlobalClass.FormChildUri(GlobalClass.gUriDataFolder.toString(), GlobalClass.gsUserDataFileName);

                //If the user data file was found in the location specified to be used for data,
                //  then there should be users already configured. Read in that data and then prompt
                //  for user login if necessary:

                ReadUserData(context);

                boolean bSingleUserInUse = false;

                if(GlobalClass.galicu_Users != null) {
                    if(GlobalClass.galicu_Users.size() == 1){
                        bSingleUserInUse = true;
                        if(GlobalClass.galicu_Users.get(0).sPin.equals("")){
                            //If there is only one user and the user does not have a pin assigned,
                            //  auto-login that user:
                            GlobalClass.gicuCurrentUser = GlobalClass.galicu_Users.get(0);
                        }
                    }
                }

                if(!bSingleUserInUse){
                    //show the user selection activity:
                    Intent intentUserSelection = new Intent(context, Activity_UserSelection.class);
                    context.startActivity(intentUserSelection);
                }
            } else {
                try {
                    GlobalClass.gUriUserDataFile = DocumentsContract.createDocument(
                            GlobalClass.gcrContentResolver,
                            GlobalClass.gUriDataFolder,
                            MimeTypes.BASE_TYPE_TEXT,
                            GlobalClass.gsUserDataFileName);
                    if (GlobalClass.gUriUserDataFile == null) {
                        Toast.makeText(context, "Could not create user data file.", Toast.LENGTH_LONG).show();
                    } else {
                        //If the user data file was just created, populate it with the default admin user:
                        ItemClass_User icu_DefaultUser = new ItemClass_User();
                        icu_DefaultUser.sUserName = "Admin";
                        icu_DefaultUser.sPin = "";
                        icu_DefaultUser.bAdmin = true;
                        icu_DefaultUser.iUserIconColor = R.color.colorStatusBar;
                        icu_DefaultUser.iMaturityLevel = AdapterMaturityRatings.MATURITY_RATING_X;
                        GlobalClass.galicu_Users.add(icu_DefaultUser);
                        GlobalClass.gicuCurrentUser = icu_DefaultUser;

                        if(!GlobalClass.WriteUserDataFile()){
                            Toast.makeText(context, "Unable to update user data file.", Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(context, "Could not create user data file.", Toast.LENGTH_LONG).show();
                }
            }

            startActionLoadData(context);
        }
    }

    public static void ReadUserData(Context context){
        BufferedReader brReader;
        InputStream isUserDataFile = null;
        try {
            GlobalClass.gUriUserDataFile = GlobalClass.FormChildUri(GlobalClass.gUriDataFolder.toString(), GlobalClass.gsUserDataFileName);
            isUserDataFile = GlobalClass.gcrContentResolver.openInputStream(GlobalClass.gUriUserDataFile);
            if (isUserDataFile != null) {
                brReader = new BufferedReader(new InputStreamReader(isUserDataFile));
                String sLine = brReader.readLine();
                while (sLine != null) {
                    ItemClass_User icu = GlobalClass.ConvertRecordStringToUserItem(sLine);
                    GlobalClass.galicu_Users.add(icu);
                    sLine = brReader.readLine();
                }
                brReader.close();
            }
        } catch (IOException e) {
            Toast.makeText(context,
                    "Trouble reading users from user data file at\n"
                            + GlobalClass.gUriUserDataFile + "\n\n" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            if(isUserDataFile != null){
                try {
                    isUserDataFile.close();
                } catch (Exception e){
                    Log.d("Activity_Main:onCreate()", "Could not close user data file.");
                }
            }
        }
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
                        assert gProgressBar_CatalogReadProgress != null;
                        gProgressBar_CatalogReadProgress.setVisibility(View.INVISIBLE);
                        gTextView_CatalogReadProgressBarText.setVisibility(View.INVISIBLE);
                    } else {
                        assert gProgressBar_CatalogReadProgress != null;
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

            boolean bCatalogLoadComplete = intent.getBooleanExtra(Worker_Catalog_LoadData.CATALOG_LOAD_COMPLETE_NOTIFICATION_BOOLEAN,false);
            if(bCatalogLoadComplete){
                GlobalClass.gabDataLoaded.set(true);
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

        if(GlobalClass.gicuCurrentUser != null){
            MenuItem menuItemLogin = menu.findItem(R.id.icon_login);
            if(menuItemLogin != null){
                setUserColor(menuItemLogin, GlobalClass.gicuCurrentUser.iUserIconColor);
            }
        }

        return true;
    }

    private void setUserColor(MenuItem item, int iColor){
        Drawable d1 = AppCompatResources.getDrawable(getApplicationContext(), R.drawable.login);
        if(d1 == null){
            return;
        }
        Drawable drawable = d1.mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(iColor, PorterDuff.Mode.SRC_IN));
        item.setIcon(drawable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if((item.getItemId() == R.id.menu_UserManagement)){
            if(!dataStorageAndLoadOk()){
                return true;
            }
            //Ask for pin code in order to allow access to feature if not admin:
            if (GlobalClass.gicuCurrentUser != null) {
                if (!GlobalClass.gicuCurrentUser.bAdmin) {
                    Toast.makeText(getApplicationContext(), "User must be logged-in and have admin privileges to configure users.", Toast.LENGTH_LONG).show();
                } else {
                    Intent intentUserManagement = new Intent(getApplicationContext(), Activity_UserManagement.class);
                    startActivity(intentUserManagement);
                }
            } else {
                Toast.makeText(getApplicationContext(), "User must be logged-in and have admin privileges to configure users.", Toast.LENGTH_LONG).show();
            }

        }

        if(item.getItemId() == R.id.menu_Settings){
            Intent intentSettings = new Intent(getApplicationContext(), Activity_AppSettings.class);
            startActivity(intentSettings);
            return true;
        } else if(item.getItemId() == R.id.icon_login){
            Intent intentUserSelection = new Intent(getApplicationContext(), Activity_UserSelection.class);
            startActivity(intentUserSelection);
            return true;
        } else if(item.getItemId() == R.id.menu_TagEditor) {
            if(!dataStorageAndLoadOk()){
                return true;
            }
            Intent intentTagEditor = new Intent(getApplicationContext(), Activity_TagEditor.class);
            startActivity(intentTagEditor);
            return true;
        } else if(item.getItemId() == R.id.menu_DatabaseBackup) {
            if(!dataStorageAndLoadOk()){
                return true;
            }
            Toast.makeText(getApplicationContext(), "Initiating database backup.", Toast.LENGTH_SHORT).show();
            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            Data dataCatalogBackupCatalogDBFiles = new Data.Builder()
                    .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_Main:onCreate()")
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                    .build();
            OneTimeWorkRequest otwrCatalogBackupCatalogDBFiles = new OneTimeWorkRequest.Builder(Worker_Catalog_BackupCatalogDBFiles.class)
                    .setInputData(dataCatalogBackupCatalogDBFiles)
                    .addTag(Worker_Catalog_BackupCatalogDBFiles.TAG_WORKER_CATALOG_BACKUPCATALOGDBFILES) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(otwrCatalogBackupCatalogDBFiles);

            return true;
        } else if(item.getItemId() == R.id.menu_LogViewer) {
            if(!dataStorageAndLoadOk()){
                return true;
            }
            Intent intentLogViewerActivity = new Intent(this, Activity_LogViewer.class);
            startActivity(intentLogViewerActivity);
            return true;

        } else if(item.getItemId() == R.id.menu_CatalogAnalysis){
            if(!dataStorageAndLoadOk()){
                return true;
            }
            Intent intentCatalogAnalysisActivity = new Intent(this, Activity_CatalogAnalysis.class);
            startActivity(intentCatalogAnalysisActivity);
            return true;

        } else if(item.getItemId() == R.id.menu_WorkerConsole){
            if(!dataStorageAndLoadOk()){
                return true;
            }
            Intent intentWorkerConsoleActivity = new Intent(this, Activity_WorkerConsole.class);
            startActivity(intentWorkerConsoleActivity);
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
                if (GlobalClass.gicuCurrentUser != null) {
                    setUserColor(menuItemLogin, GlobalClass.gicuCurrentUser.iUserIconColor);
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
                Log.d("Workstate", stateWorkState + ", ID " + UUIDWorkID);
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
        if(!dataStorageAndLoadOk()){
            return;
        }
        Toast.makeText(getApplicationContext(), "Opening Videos catalog...", Toast.LENGTH_SHORT).show();
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_VIDEOS);
        GlobalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        startActivity(intentCatalogActivity);
    }

    public void startPicturesCatalogActivity(View v){
        if(!dataStorageAndLoadOk()){
            return;
        }
        Toast.makeText(getApplicationContext(), "Opening Images catalog...", Toast.LENGTH_SHORT).show();
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        GlobalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        startActivity(intentCatalogActivity);
    }

    public void startComicsCatalogActivity(View v){
        if(!dataStorageAndLoadOk()){
            return;
        }
        Toast.makeText(getApplicationContext(), "Opening Comics catalog...", Toast.LENGTH_SHORT).show();
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        GlobalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        startActivity(intentCatalogActivity);
    }

    public void startImportVideos(View v){
        if(!dataStorageAndLoadOk()){
            return;
        }
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_VIDEOS); //todo: Redundant
        GlobalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        startActivity(intentImportGuided);
    }

    public void startImportImages(View v){
        if(!dataStorageAndLoadOk()){
            return;
        }
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_IMAGES); //todo: Redundant
        GlobalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        startActivity(intentImportGuided);
    }

    public void startImportComics(View v){
        if(!dataStorageAndLoadOk()){
            return;
        }
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_COMICS); //todo: Redundant
        GlobalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        startActivity(intentImportGuided);
    }

    private boolean dataStorageAndLoadOk(){
        if(!GlobalClass.gabDataLoaded.get()){
            if(GlobalClass.gUriDataFolder == null){
                Toast.makeText(getApplicationContext(),
                        "No data folder selected. A storage location may be selected from the Settings menu.",
                        Toast.LENGTH_LONG).show();
                return false;
            } else {
                Toast.makeText(getApplicationContext(), "Please wait for data to load.", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }


    public void buttonTestClick_Test(View v){

    }

    public static void startActionLoadData(Context context) {

        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataCatalogLoadData = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Activity_Main:onCreate()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .build();
        OneTimeWorkRequest otwrCatalogLoadData = new OneTimeWorkRequest.Builder(Worker_Catalog_LoadData.class)
                .setInputData(dataCatalogLoadData)
                .addTag(Worker_Catalog_LoadData.TAG_WORKER_CATALOG_LOADDATA) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(context).enqueue(otwrCatalogLoadData);
    }


}