package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Catalog_BackupCatalogDBFiles extends Worker {

    public static final String TAG_WORKER_CATALOG_BACKUPCATALOGDBFILES = "com.agcurations.aggallermanager.tag_worker_catalog_backupcatalogdbfiles";

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

        //Backup the catalog text files:
        for(int i = 0; i < 3; i++){
            StringBuilder sbBuffer = new StringBuilder();
            boolean bHeaderWritten = false;
            for(Map.Entry<String, ItemClass_CatalogItem> tmEntry: globalClass.gtmCatalogLists.get(i).entrySet()){

                if(!bHeaderWritten) {
                    sbBuffer.append(globalClass.getCatalogHeader()); //Append the header.
                    sbBuffer.append("\n");
                    bHeaderWritten = true;
                }

                sbBuffer.append(GlobalClass.getCatalogRecordString(tmEntry.getValue())); //Append the data.
                sbBuffer.append("\n");
            }

            try {
                //Write the catalog file:
                String sDateTimeStamp = GlobalClass.GetTimeStampFileSafe();
                /*File fBackup = new File(globalClass.gfCatalogFolders[i].getAbsolutePath()
                        + File.separator + "CatalogContents_" + sDateTimeStamp + ".dat");*/
                File fBackup = new File(globalClass.gdfDataFolder + File.separator
                        + "Backup" + File.separator
                        + GlobalClass.gsCatalogFolderNames[i] + "_CatalogContents_" + sDateTimeStamp + ".dat");

                FileWriter fwNewCatalogContentsFile = new FileWriter(fBackup, false);

                fwNewCatalogContentsFile.write(sbBuffer.toString());
                fwNewCatalogContentsFile.flush();
                fwNewCatalogContentsFile.close();

            } catch (Exception e) {
                String sMessage = "Problem creating backup of CatalogContents.dat.\n" + e.getMessage();
                broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                bProblem = true;
            }
        }

        //Backup the tag files:
        for(int i = 0; i < 3; i++){
            StringBuilder sbBuffer = new StringBuilder();
            boolean bHeaderWritten = false;
            for(Map.Entry<Integer, ItemClass_Tag> tmEntry: globalClass.gtmCatalogTagReferenceLists.get(i).entrySet()){

                if(!bHeaderWritten) {
                    sbBuffer.append(GlobalClass.getTagFileHeader()); //Append the header.
                    sbBuffer.append("\n");
                    bHeaderWritten = true;
                }

                sbBuffer.append(globalClass.getTagRecordString(tmEntry.getValue())); //Append the data.
                sbBuffer.append("\n");
            }

            try {
                //Write the tag file:
                String sDateTimeStamp = GlobalClass.GetTimeStampFileSafe();
                /*File fBackup = new File(globalClass.gfCatalogFolders[i].getAbsolutePath()
                        + File.separator + "Tags_" + sDateTimeStamp + ".dat");*/
                File fBackup = new File(globalClass.gdfDataFolder + File.separator
                        + "Backup" + File.separator
                        + GlobalClass.gsCatalogFolderNames[i] + "_Tags_" + sDateTimeStamp + ".dat");
                FileWriter fwNewTagsFile = new FileWriter(fBackup, false);

                fwNewTagsFile.write(sbBuffer.toString());
                fwNewTagsFile.flush();
                fwNewTagsFile.close();

            } catch (Exception e) {
                String sMessage = "Problem creating backup of Tags.dat.\n" + e.getMessage();
                broadcastIntent.putExtra(GlobalClass.EXTRA_BOOL_PROBLEM, true);
                broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_PROBLEM, sMessage);
                bProblem = true;
            }
        }
        if(!bProblem) {
            String sMessage = "Database files backup completed.";
            broadcastIntent.putExtra(GlobalClass.EXTRA_STRING_STATUS_MESSAGE, sMessage);
        }

        //Send broadcast to the Import Activity:
        broadcastIntent.setAction(Activity_Main.MainActivityDataServiceResponseReceiver.MAIN_ACTIVITY_DATA_SERVICE_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        return Result.success();
    }



}
