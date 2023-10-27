package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Catalog_BackupCatalogDBFiles extends Worker {

    public static final String TAG_WORKER_CATALOG_BACKUPCATALOGDBFILES = "com.agcurations.aggallermanager.tag_worker_catalog_backupcatalogdbfiles";

    public static final String CATALOG_DATA_FILE_BACKUP_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.CATALOG_DATA_FILE_BACKUP_ACTION_RESPONSE";

    String gsResponseActionFilter;
    GlobalClass globalClass;

    public Worker_Catalog_BackupCatalogDBFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gsResponseActionFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);


    }

    @NonNull
    @Override
    public Result doWork() {
        globalClass = (GlobalClass) getApplicationContext();

        Intent broadcastIntent = new Intent();
        boolean bProblem = false;


        int iProgressDenominator;
        int iProgressNumerator;
        int iProgressBarValue;

        Uri uriBackupFolder = GlobalClass.FormChildUri(GlobalClass.gUriDataFolder, "Backup");

        String sDateTimeStamp = GlobalClass.GetTimeStampFileSafe();

        //==========================================================================================
        //Backup the catalog text files:
        for(int iMediaCategory = 0; iMediaCategory < 3; iMediaCategory++){
            iProgressNumerator = 0;
            iProgressDenominator = GlobalClass.gtmCatalogLists.get(iMediaCategory).size();
            iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
            globalClass.BroadcastProgress(false, "",
                    true, iProgressBarValue,
                    true, "Backing up catalog data files...",
                    CATALOG_DATA_FILE_BACKUP_ACTION_RESPONSE);

            StringBuilder sbBuffer = new StringBuilder();
            boolean bHeaderWritten = false;
            for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: GlobalClass.gtmCatalogLists.get(iMediaCategory).entrySet()){

                if(!bHeaderWritten) {
                    sbBuffer.append(GlobalClass.getCatalogHeader()); //Append the header.
                    sbBuffer.append("\n");
                    bHeaderWritten = true;
                }

                sbBuffer.append(GlobalClass.getCatalogRecordString(tmEntry.getValue())); //Append the data.
                sbBuffer.append("\n");

                iProgressNumerator++;
                if(iProgressNumerator % 100 == 0) {
                    iProgressBarValue = Math.round((iProgressNumerator / (float) iProgressDenominator) * 100);
                    globalClass.BroadcastProgress(false, "",
                            true, iProgressBarValue,
                            true, "Backing up " + GlobalClass.gsCatalogFolderNames[iMediaCategory] + " catalog data file.",
                            CATALOG_DATA_FILE_BACKUP_ACTION_RESPONSE);
                }
            }

            try {
                //Write the catalog file:
                String sFileName = GlobalClass.gsCatalogFolderNames[iMediaCategory] + "_CatalogContents_" + sDateTimeStamp + ".dat";
                Uri uriBackupFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriBackupFolder, GlobalClass.BASE_TYPE_TEXT, sFileName);
                if(uriBackupFile != null){
                    OutputStream osBackupFile = GlobalClass.gcrContentResolver.openOutputStream(uriBackupFile);
                    if(osBackupFile != null) {
                        osBackupFile.write(sbBuffer.toString().getBytes(StandardCharsets.UTF_8));
                        osBackupFile.flush();
                        osBackupFile.close();
                    } else {
                        String sMessage = "Problem creating backup of CatalogContents.dat.";
                        LogThis("doWork()", sMessage, null);
                        broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                        broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                        bProblem = true;
                    }
                } else {
                    String sMessage = "Problem creating backup of CatalogContents.dat.";
                    LogThis("doWork()", sMessage, null);
                    broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                    broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                    bProblem = true;
                }

            } catch (Exception e) {
                String sMessage = "Problem creating backup of CatalogContents.dat.\n" + e.getMessage();
                LogThis("doWork()", sMessage, null);
                broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                bProblem = true;
            }
        }

        //==========================================================================================
        //Backup the tag files:
        for(int i = 0; i < 3; i++){
            StringBuilder sbBuffer = new StringBuilder();
            boolean bHeaderWritten = false;
            for(Map.Entry<Integer, ItemClass_Tag> tmEntry: GlobalClass.gtmCatalogTagReferenceLists.get(i).entrySet()){

                if(!bHeaderWritten) {
                    sbBuffer.append(GlobalClass.getTagFileHeader()); //Append the header.
                    sbBuffer.append("\n");
                    bHeaderWritten = true;
                }

                sbBuffer.append(GlobalClass.getTagRecordString(tmEntry.getValue())); //Append the data.
                sbBuffer.append("\n");
            }

            try {
                //Write the tag file:
                String sFileName = GlobalClass.gsCatalogFolderNames[i] + "_Tags_" + sDateTimeStamp + ".dat";
                Uri uriBackupFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriBackupFolder, GlobalClass.BASE_TYPE_TEXT, sFileName);
                if(uriBackupFile != null){
                    OutputStream osBackupFile = GlobalClass.gcrContentResolver.openOutputStream(uriBackupFile);
                    if(osBackupFile != null) {
                        osBackupFile.write(sbBuffer.toString().getBytes(StandardCharsets.UTF_8));
                        osBackupFile.flush();
                        osBackupFile.close();
                    } else {
                        String sMessage = "Problem creating backup of Tags.dat.";
                        broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                        broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                        bProblem = true;
                    }
                } else {
                    String sMessage = "Problem creating backup of Tags.dat.";
                    broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                    broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                    bProblem = true;
                }

            } catch (Exception e) {
                String sMessage = "Problem creating backup of Tags.dat.\n" + e.getMessage();
                broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                bProblem = true;
            }
        }

        //==========================================================================================
        //Backup the user data file:

        try {
            //Write the data to the file:
            String sBaseName = GlobalClass.gsUserDataFileName.substring(0, GlobalClass.gsUserDataFileName.lastIndexOf("."));
            String sExtension = GlobalClass.gsUserDataFileName.substring(GlobalClass.gsUserDataFileName.lastIndexOf("."));
            String sFileName = sBaseName + "_" + sDateTimeStamp + sExtension;
            Uri uriBackupFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriBackupFolder, GlobalClass.BASE_TYPE_TEXT, sFileName);
            if(uriBackupFile != null) {
                InputStream isSourceFile = null;
                OutputStream osDestinationFile = null;

                try {
                    isSourceFile = GlobalClass.gcrContentResolver.openInputStream(GlobalClass.gUriUserDataFile);
                    osDestinationFile = GlobalClass.gcrContentResolver.openOutputStream(uriBackupFile);

                    if (isSourceFile != null && osDestinationFile != null) {
                        byte[] bucket = new byte[32 * 1024];
                        int bytesRead = 0;
                        while (bytesRead != -1) {
                            bytesRead = isSourceFile.read(bucket); //-1, 0, or more
                            if (bytesRead > 0) {
                                osDestinationFile.write(bucket, 0, bytesRead);
                            }
                        }
                    }

                } catch (Exception e) {
                    String sMessage = "Problem creating backup of " + GlobalClass.gUriUserDataFile + " file.";
                    broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                    broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                    bProblem = true;

                } finally {
                    if (isSourceFile != null)
                        isSourceFile.close();
                    if (osDestinationFile != null)
                        osDestinationFile.close();
                }
            } else {
                String sMessage = "Problem creating backup of " + GlobalClass.gsUserDataFileName + ".";
                broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                bProblem = true;
            }
        } catch (Exception e) {
            String sMessage = "Problem creating backup of " + GlobalClass.gsUserDataFileName + ".\n" + e.getMessage();
            broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
            broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
            bProblem = true;
        }

        //==========================================================================================
        //Backup the browser data file:

        try {
            Uri uriWebPageTabDataFile = GlobalClass.gUriWebpageTabDataFile;
            if(!GlobalClass.CheckIfFileExists(uriWebPageTabDataFile)){
                String sMessage = "Problem creating backup of " + GlobalClass.gsBrowserDataFile + " file. Source file not found.";
                broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                bProblem = true;
            } else {

                String sBaseName = GlobalClass.gsBrowserDataFile.substring(0, GlobalClass.gsBrowserDataFile.lastIndexOf("."));
                String sExtension = GlobalClass.gsBrowserDataFile.substring(GlobalClass.gsBrowserDataFile.lastIndexOf("."));
                String sFileName = sBaseName + "_" + sDateTimeStamp + sExtension;

                Uri uriBackupFile = DocumentsContract.createDocument(GlobalClass.gcrContentResolver, uriBackupFolder, GlobalClass.BASE_TYPE_TEXT, sFileName);

                if(uriBackupFile != null) {

                    InputStream isSourceFile = null;
                    OutputStream osDestinationFile = null;

                    try {
                        isSourceFile = GlobalClass.gcrContentResolver.openInputStream(uriWebPageTabDataFile);
                        osDestinationFile = GlobalClass.gcrContentResolver.openOutputStream(uriBackupFile);

                        if (isSourceFile != null && osDestinationFile != null) {
                            byte[] bucket = new byte[32 * 1024];
                            int bytesRead = 0;
                            while (bytesRead != -1) {
                                bytesRead = isSourceFile.read(bucket); //-1, 0, or more
                                if (bytesRead > 0) {
                                    osDestinationFile.write(bucket, 0, bytesRead);
                                }
                            }
                        }

                    } catch (Exception e) {
                        String sMessage = "Problem creating backup of " + GlobalClass.gsBrowserDataFile + " file.";
                        broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                        broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                        bProblem = true;

                    } finally {
                        if (isSourceFile != null)
                            isSourceFile.close();
                        if (osDestinationFile != null)
                            osDestinationFile.close();
                    }

                } else { //End if uriBackupFile != null.
                    String sMessage = "Problem creating backup of " + GlobalClass.gsBrowserDataFile + " file.";
                    broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                    broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                    bProblem = true;
                }
            } //End if-else for finding the backup's source file.
        } catch (Exception e) {
            String sMessage = "Problem creating backup of " + GlobalClass.gsBrowserDataFile + " file.\n" + e.getMessage();
            broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
            broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
            bProblem = true;
        }

        //==========================================================================================
        //==========================================================================================



        if(!bProblem) {
            String sMessage = "Database files backup completed.";
            broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_STATUS_MESSAGE, sMessage);
        }

        //Send broadcast to the Main Activity:
        broadcastIntent.setAction(CATALOG_DATA_FILE_BACKUP_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

        globalClass.BroadcastProgress(false, "",
                true, 100,
                true, "Catalog Backup Complete",
                CATALOG_DATA_FILE_BACKUP_ACTION_RESPONSE);

        return Result.success();
    }

    private void LogThis(String sRoutine, String sMainMessage, String sExtraErrorMessage){
        String sMessage = sMainMessage;
        if(sExtraErrorMessage != null){
            sMessage = sMessage + " " + sExtraErrorMessage;
        }
        Log.d("Worker_Catalog_BackupCatalogDBFiles:" + sRoutine, sMessage);
    }

}
