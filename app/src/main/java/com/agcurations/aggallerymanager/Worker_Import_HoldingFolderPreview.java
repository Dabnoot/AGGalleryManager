package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Worker_Import_HoldingFolderPreview extends Worker {

    //Define string used to identify this worker type:
    public static final String TAG_WORKER_IMPORT_HOLDING_FOLDER_PREVIEW = "com.agcurations.aggallermanager.tag_worker_import_getholdingfolderpreview";

    public static final String IMPORT_HOLDING_FOLDER_PREVIEW_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.IMPORT_HOLDING_FOLDER_PREVIEW_ACTION_RESPONSE";

    public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";

    public static final String EXTRA_BOOL_HOLDING_FOLDER_FILES_APPROVED_RESULT = "com.agcurations.aggallermanager.extra_bool_holding_folder_files_approved_result";
    public static final String EXTRA_INT_HOLDING_FOLDER_FILES_APPROVED_FOR_USER = "com.agcurations.aggallermanager.extra_int_holding_folder_files_approved_for_user";

    //=========================
    // Define keys for arguments passed to this worker:
    public static final String KEY_ARG_JOB_REQUEST_DATETIME = "KEY_ARG_JOB_REQUEST_DATETIME";

    //=========================
    String gsJobRequestDateTime;    //Date/Time of job request for logging purposes.
    long glProgressNumerator = 0L;
    long glProgressDenominator;
    int giFileCount;
    int giFilesProcessed;

    GlobalClass globalClass;

    //=========================

    public Worker_Import_HoldingFolderPreview(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters) {
        super(appContext, workerParameters);

        gsJobRequestDateTime = getInputData().getString(KEY_ARG_JOB_REQUEST_DATETIME);
    }

    @NonNull
    @Override
    public Result doWork() {

        globalClass = (GlobalClass) getApplicationContext();

        String sMessage ="";

        globalClass.BroadcastProgress(true, sMessage,
                false, 0,
                false, "",
                IMPORT_HOLDING_FOLDER_PREVIEW_ACTION_RESPONSE);


        //Determine the number of files in the holding folder and adjust the radiobutton text to show file count.
        ArrayList<String> sImageHoldingFolderFiles = GlobalClass.GetDirectoryFileNames(GlobalClass.gUriImageDownloadHoldingFolder);

        int iFileCount;

        //Need to determine which files have associated .dat metadata files.
        //  If there is an associated metadata file, read which user downloaded the file and
        //  don't expose that file to any other users.

        //List all media files with associated .dat files:
        ArrayList<String> alsMediaFilesWithDatFiles = new ArrayList<>();
        for(String sFileName: sImageHoldingFolderFiles){
            String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sFileName);
            if(sBaseAndExtension.length == 2) {
                if (sBaseAndExtension[1].equals("tad")) { //.dat file obfuscated. .dat file would be something like 1egamI.gpj.tad.
                    if(sImageHoldingFolderFiles.contains(sBaseAndExtension[0])){ //sBaseAndExtension[0] will be the media file name AND its extension... such as 1egamI.gpj.
                        //If the media file exists, add it to the list of media files with matching .dat files:
                        alsMediaFilesWithDatFiles.add(sBaseAndExtension[0]);
                    }/* else {
                        //If this is a .dat file without a media file... do nothing for now. Todo: Consider deleting the .dat file?
                        //  This case would be if the download failed for some reason, or perhaps the media file was moved
                        //  or was imported and the .dat file deletion failed.
                        //  Suggest starting the "delete files" worker if orphaned .dat files are found.
                    }*/
                }
            }
        }

        //Now look to see if there are any files without metadata files.
        ArrayList<String> alsNoMetadataFileMediaFiles = new ArrayList<>();
        for(String sFileName: sImageHoldingFolderFiles){
            String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sFileName);
            if(sBaseAndExtension.length == 2) {
                if (!sBaseAndExtension[1].equals("tad")) {
                    //If this is not a .dat file then it is a media file. Check if this media file had a corresponding .dat file.
                    if(!alsMediaFilesWithDatFiles.contains(sFileName)){
                        //If there was no associated .dat file found for this media file in the previous operation, record this file.
                        alsNoMetadataFileMediaFiles.add(sFileName);
                    }
                }
            }

        }

        //Now check metadata files for user. If the user is specified and matches the current user,
        // include this entry in the file count for potential imports.
        TreeMap<String, String> tmFilenamesAndUsers = new TreeMap<>();
        for(String sMediaFileName: alsMediaFilesWithDatFiles) {
            String sMetaDataFileName = sMediaFileName + ".tad";
            String sMetadataFileUri = GlobalClass.FormChildUriString(GlobalClass.gUriImageDownloadHoldingFolder.toString(), sMetaDataFileName);
            try {
                //Read a metadata file.
                Uri uriMetadataFile = Uri.parse(sMetadataFileUri);
                InputStream isImageMetadataFile = GlobalClass.gcrContentResolver.openInputStream(uriMetadataFile);
                if (isImageMetadataFile == null) {
                    sMessage = "Could not open metadata file for analysis: " + sMetadataFileUri;
                    Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
                    continue;
                }
                BufferedReader brReader;
                brReader = new BufferedReader(new InputStreamReader(isImageMetadataFile));
                int iLine = 0;
                for (String sLine = brReader.readLine(); sLine != null && iLine < 3; sLine = brReader.readLine()) {
                    switch (iLine) {
                        case 0:
                        case 1:
                            //Do nothing
                            break;
                        case 2:
                            tmFilenamesAndUsers.put(sMediaFileName, sLine);
                            break;
                    }
                    iLine++;
                }

                brReader.close();
                isImageMetadataFile.close();

                if(!tmFilenamesAndUsers.containsKey(sMediaFileName)){
                    //If a username was not found for this entry, add the filename but exclude a user.
                    tmFilenamesAndUsers.put(sMediaFileName, "");
                }

            } catch (Exception e) {
                sMessage = "Could not open metadata file for analysis: " + sMetadataFileUri + "\n" + e.getMessage();
                Toast.makeText(getApplicationContext(), sMessage, Toast.LENGTH_SHORT).show();
            }
        }
        //tmFilenamesAndUsers now contains a list of all media filenames and the user as derived from an associated metadata file.

        //
        //Now combine file items that had metadata indicating that they are intended for the current
        //  user with any file items that were missing metadata files.
        ArrayList<String> alsApprovedFiles = new ArrayList<>();
        for(Map.Entry<String, String> entry: tmFilenamesAndUsers.entrySet()){
            if(entry.getValue().equals(GlobalClass.gicuCurrentUser.sUserName) ||
                    entry.getValue().equals("")){
                alsApprovedFiles.add(entry.getKey());
            }
        }
        alsApprovedFiles.addAll(alsNoMetadataFileMediaFiles);

        //All media files in holding folder now located that are approved for the current user
        //  or have no defined user or have no associated metadata file.

        iFileCount = alsApprovedFiles.size();

        //Broadcast a message to to indicate the result of this worker:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(IMPORT_HOLDING_FOLDER_PREVIEW_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(EXTRA_BOOL_HOLDING_FOLDER_FILES_APPROVED_RESULT, true);
        broadcastIntent.putExtra(EXTRA_INT_HOLDING_FOLDER_FILES_APPROVED_FOR_USER, iFileCount);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

        return Result.success();

    }






}
