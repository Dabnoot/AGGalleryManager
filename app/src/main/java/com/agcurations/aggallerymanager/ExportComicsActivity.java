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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ExportComicsActivity extends AppCompatActivity {

    //Global Constants:
    public static final int  MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE = 1002;
    public static final int REQUEST_CODE_GET_EXPORT_SAVE_AS_FILE = 1003;
    private static final String LOG_TAG = "ExportComicsActivity";

    //Global Variables:
    private ExportResponseReceiver exportResponseReceiver;
    Intent gIntentExport;
    TextView gtvExportLog;
    ProgressBar gpbExportDeterminateBar;
    TextView gtvExportPercent;
    int giComicFileCount;

    ArrayList<String> galZipList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_comics);

        //Get view references now for updates rather than over and over again for the
        //  broadcast receiver:
        TextView tvCatalogSize = findViewById(R.id.textView_CatalogSize);
        gpbExportDeterminateBar = findViewById(R.id.determinateBar_Export);
        gtvExportPercent = findViewById(R.id.textView_ExportPercent);
        gtvExportLog = findViewById(R.id.textView_ExportLog);
        gtvExportLog.setMovementMethod(new ScrollingMovementMethod());

        // Calling Application class (see application tag in AndroidManifest.xml)
        final GlobalClass globalVariable = (GlobalClass) getApplicationContext();

        //Determine the amount of space required for the export (backup):
        TreeMap<Integer, String[]> tmCatalogComicList = globalVariable.gtmCatalogComicList;
        String sComicFolderPath = GlobalClass.gfComicsFolder.getAbsolutePath();

        int iComicCatalogSize = 0;
        String sTemp;
        galZipList = new ArrayList<>();

        for (Map.Entry<Integer, String[]>
                entry : tmCatalogComicList.entrySet()) {

            //Get the data for the entry:
            String[] sFields;
            sFields = entry.getValue();

            if (sFields == null) {
                sFields = new String[GlobalClass.ComicRecordFields.length]; //To prevent possible null pointer exception later.
            }

            if (sFields[GlobalClass.COMIC_SIZE_KB_INDEX] != null) {
                iComicCatalogSize += Integer.parseInt(sFields[GlobalClass.COMIC_SIZE_KB_INDEX]);
            }

            if (sFields[GlobalClass.COMIC_FILE_COUNT_INDEX] != null) {
                giComicFileCount += Integer.parseInt(sFields[GlobalClass.COMIC_FILE_COUNT_INDEX]);
            }

            //Add the comic folder to the list of files to include in the zip file:
            sTemp = sComicFolderPath + File.separator + sFields[GlobalClass.COMIC_FOLDER_NAME_INDEX] + File.separator;
            galZipList.add(sTemp);
        }
        //Add the catalog contents file to the list of files to include in the zip file:
        File f = GlobalClass.gfComicCatalogContentsFile;
        galZipList.add(f.getAbsolutePath());
        giComicFileCount++;

        String s = NumberFormat.getNumberInstance(Locale.US).format(iComicCatalogSize);
        s = s + " KB";
        tvCatalogSize.setText(s);


        IntentFilter filter = new IntentFilter(ExportResponseReceiver.EXPORT_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        exportResponseReceiver = new ExportResponseReceiver();
        registerReceiver(exportResponseReceiver, filter);

    }


    public void selectOutputFile(View v){
        //https://developer.android.com/training/data-storage/shared/documents-files
        //https://developer.android.com/training/data-storage/shared/documents-files#create-file
        // Allow the user to choose a location to save the export using the system's file picker.
        Intent intent_GetExportSaveAsFile = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        intent_GetExportSaveAsFile.addCategory(Intent.CATEGORY_OPENABLE);
        intent_GetExportSaveAsFile.setType("application/zip");

        //Generate the name of the zip file:
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String sTemp = sdf.format(timestamp);
        String sZipFileName = "NHComicManager_Backup_" + sTemp + ".zip";

        intent_GetExportSaveAsFile.putExtra(Intent.EXTRA_TITLE, sZipFileName);

        //Start the activity:
        startActivityForResult(intent_GetExportSaveAsFile, REQUEST_CODE_GET_EXPORT_SAVE_AS_FILE);

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

            if (((requestCode == REQUEST_CODE_GET_EXPORT_SAVE_AS_FILE && resultCode == Activity.RESULT_OK))
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

                    gIntentExport = new Intent(this, ExportComicsService.class);
                    gIntentExport.putStringArrayListExtra(ExportComicsService.ZIP_LIST,galZipList);
                    gIntentExport.putExtra(ExportComicsService.ZIP_TOTAL_FILE_COUNT,giComicFileCount);

                    Uri uriZipFile = resultData.getData();
                    assert uriZipFile != null;
                    String sUriString = uriZipFile.toString();
                    gIntentExport.putExtra(ExportComicsService.ZIP_FILE, sUriString);

                    TextView tvExportFileAbsPath = findViewById(R.id.textView_ExportFileAbsPath);
                    tvExportFileAbsPath.setText(sUriString);

                    startService(gIntentExport);

                }

            }
        } catch (Exception ex) {
            Context context = getApplicationContext();
            Toast.makeText(context, ex.toString(),
                    Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, ex.toString());
        }

    }

    public class ExportResponseReceiver extends BroadcastReceiver {
        public static final String EXPORT_ACTION_RESPONSE = "com.dabnoot.intent.action.FROM_EXPORT";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean 	bUpdateLog;
            boolean 	bUpdatePercentComplete;

            //Get booleans from the intent telling us what to update:
            bUpdateLog = intent.getBooleanExtra(ExportComicsService.UPDATE_LOG_BOOLEAN,false);
            bUpdatePercentComplete = intent.getBooleanExtra(ExportComicsService.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);

            if(bUpdateLog){
                String 		sLogLine;
                sLogLine = intent.getStringExtra(ExportComicsService.LOG_LINE_STRING);
                gtvExportLog.append(sLogLine);
            }
            if(bUpdatePercentComplete){
                int 		iAmountComplete;
                iAmountComplete = intent.getIntExtra(ExportComicsService.PERCENT_COMPLETE_INT, -1);
                gpbExportDeterminateBar.setProgress(iAmountComplete);
                String s = iAmountComplete + "%";
                gtvExportPercent.setText(s);
            }
        }
    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(exportResponseReceiver);
        super.onDestroy();
    }

}