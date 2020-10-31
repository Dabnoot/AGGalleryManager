package com.agcurations.aggallerymanager;


import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class ImportComicsActivity_obsolete extends AppCompatActivity {

    //Global Constants:
    public static final int  MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE = 1002;
    public static final int REQUEST_CODE_GET_IMPORT_FOLDER = 1004;
    public static final int REQUEST_CODE_GET_IMPORT_FILE = 1005;
    private static final String LOG_TAG = "ImportComicsActivity";

    //Global Variables:
    private ImportComicsActivity_obsolete.ImportResponseReceiver importResponseReceiver;
    Intent gIntentImport;
    TextView gtvLog;
    ProgressBar gpbDeterminateBar;
    TextView gtvStage;

    private GlobalClass globalClass;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_comics);

        globalClass = (GlobalClass) getApplicationContext();

        //Get view references now for updates rather than over and over again for the
        //  broadcast receiver:
        gtvLog = findViewById(R.id.textView_ImportLog);
        gtvLog.setMovementMethod(new ScrollingMovementMethod());


        gpbDeterminateBar = findViewById(R.id.determinateBar_Import);
        gtvStage = findViewById(R.id.textView_ImportStage);

        IntentFilter filter = new IntentFilter(ImportComicsActivity_obsolete.ImportResponseReceiver.IMPORT_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        importResponseReceiver = new ImportComicsActivity_obsolete.ImportResponseReceiver();
        registerReceiver(importResponseReceiver, filter);


    }


    public void selectImportFile(View v){
        //Make sure there is space for copying
        long lSize;
        lSize = globalClass.AvailableStorageSpace(this, 1);

        //150 MB to begin import operation. This is about the size of 250 pages.
        //It is not uncommon for a comic listing to be a set of issues totaling neat 250 pages.
        boolean bEnoughSize = false;

        if (lSize >= 1024) {
            //contains at least 1 MB.
            lSize /= 1024;
            //size now in MB units.
            if (lSize > 150){
                bEnoughSize = true;
            }
        }

        //A
        if (!bEnoughSize) {
            Toast.makeText(this, "Storage space too full to begin import.(need > 150 MB).", Toast.LENGTH_LONG).show();
            return;
        }


        RadioButton radioButton_ImportFolder;
        radioButton_ImportFolder = findViewById(R.id.radioButton_ImportFolder);
        if(radioButton_ImportFolder.isChecked()) {

            //A minimum space to get started with the import has been confirmed.
            //https://developer.android.com/training/data-storage/shared/documents-files
            // Allow the user to choose a directory using the system's file picker.
            Intent intent_GetImportFromFolder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

            // Provide write access to files and sub-directories in the user-selected directory:
            intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            //Start the activity:
            startActivityForResult(intent_GetImportFromFolder, REQUEST_CODE_GET_IMPORT_FOLDER);
        } else {
            // Allow the user to choose a backup zip file using the system's file picker.
            Intent intent_GetExportSaveAsFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            intent_GetExportSaveAsFile.addCategory(Intent.CATEGORY_OPENABLE);
            intent_GetExportSaveAsFile.setType("application/zip");

            //Start the activity:
            startActivityForResult(intent_GetExportSaveAsFile, REQUEST_CODE_GET_IMPORT_FILE);

        }

    }

    @Override
    //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        //https://developer.android.com/training/data-storage/shared/documents-files
        try {
            super.onActivityResult(requestCode, resultCode, resultData);


            //If this is an EXPORT operation, and the data is not NULL,
            // look for permissions before executing operations.

            if (((requestCode == REQUEST_CODE_GET_IMPORT_FOLDER && resultCode == Activity.RESULT_OK)
            || (requestCode == REQUEST_CODE_GET_IMPORT_FILE && resultCode == Activity.RESULT_OK))
                    && (resultData != null)){
                //Check to make sure that we have read/write permission in the selected folder.
                //If we don't have permission, request it.
                if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) ||
                        (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED)) {

                    // Permission is not granted
                    // Should we show an explanation?
                    if ((ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                            (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE))) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        Toast.makeText(this, "Permission required for read/write operation.", Toast.LENGTH_LONG).show();
                    } else {
                        // No explanation needed; request the permission
                        ActivityCompat.requestPermissions(this,
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

                if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED)) {
                    //If we now have permission...
                    //The result data contains a URI for the document or directory that
                    //the user selected.

                    gIntentImport = new Intent(this, ImportComicsService_obsolete.class);

                    //Put the import method into the intent.
                    if(requestCode == REQUEST_CODE_GET_IMPORT_FOLDER) {
                        gIntentImport.putExtra(ImportComicsService_obsolete.IMPORT_METHOD, ImportComicsService_obsolete.IMPORT_METHOD_FOLDER);
                    } else {
                        gIntentImport.putExtra(ImportComicsService_obsolete.IMPORT_METHOD, ImportComicsService_obsolete.IMPORT_METHOD_FILE);
                    }
                    //Put the import Uri into the intent (this could represent a folder OR a file:
                    Uri uriImportURI = resultData.getData();
                    assert uriImportURI != null;
                    String sUriString = uriImportURI.toString();
                    gIntentImport.putExtra(ImportComicsService_obsolete.IMPORT_URI, sUriString);

                    TextView tvImportPath = findViewById(R.id.textView_ImportPath);
                    tvImportPath.setText(sUriString);

                    startService(gIntentImport);

                }

            }
        } catch (Exception ex) {
            Context context = getApplicationContext();
            Toast.makeText(context, ex.toString(),
                    Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, ex.toString());
        }

    }


    public class ImportResponseReceiver extends BroadcastReceiver {
        public static final String IMPORT_ACTION_RESPONSE = "com.dabnoot.intent.action.FROM_IMPORT";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean 	bUpdateLog;
            boolean 	bUpdatePercentComplete;
            boolean 	bUpdateStageIndication;

            //Get booleans from the intent telling us what to update:
            bUpdateLog = intent.getBooleanExtra(ImportComicsService_obsolete.UPDATE_LOG_BOOLEAN,false);
            bUpdatePercentComplete = intent.getBooleanExtra(ImportComicsService_obsolete.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
            bUpdateStageIndication = intent.getBooleanExtra(ImportComicsService_obsolete.UPDATE_STAGE_INDICATION_BOOLEAN,false);

            if(bUpdateLog){
                String 		sLogLine;
                sLogLine = intent.getStringExtra(ImportComicsService_obsolete.LOG_LINE_STRING);
                gtvLog.append(sLogLine);
            }
            if(bUpdatePercentComplete){
                int 		iAmountComplete;
                iAmountComplete = intent.getIntExtra(ImportComicsService_obsolete.PERCENT_COMPLETE_INT, -1);
                gpbDeterminateBar.setProgress(iAmountComplete);
            }
            if(bUpdateStageIndication){
                String 		sStage;
                sStage = intent.getStringExtra(ImportComicsService_obsolete.STAGE_STRING);
                gtvStage.setText(sStage);
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(importResponseReceiver);
        super.onDestroy();
    }

}