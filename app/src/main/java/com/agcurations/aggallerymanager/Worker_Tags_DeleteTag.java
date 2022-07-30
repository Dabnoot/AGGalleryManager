package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_Tags_DeleteTag extends Worker {

    public static final String TAG_WORKER_TAGS_DELETETAG = "com.agcurations.aggallermanager.tag_worker_tags_deletetag";

    String gsResponseActionFilter;
    int giMediaCategory;
    ItemClass_Tag gict_TagToDelete;
    String gsIntentActionFilter = Fragment_TagEditor_4_DeleteTag.TagEditorServiceResponseReceiver.TAG_EDITOR_SERVICE_ACTION_RESPONSE;
    Context gContext;

    public Worker_Tags_DeleteTag(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        gContext = context;
        gsResponseActionFilter = getInputData().getString(GlobalClass.EXTRA_CALLER_ACTION_RESPONSE_FILTER);
        giMediaCategory = getInputData().getInt(GlobalClass.EXTRA_MEDIA_CATEGORY, -1);
        String sTagToDelete = getInputData().getString(GlobalClass.EXTRA_TAG_TO_BE_DELETED);
        if(sTagToDelete != null){
            gict_TagToDelete = GlobalClass.ConvertFileLineToTagItem(sTagToDelete);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        GlobalClass globalClass;
        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        String sTagFolderPath = globalClass.gfCatalogFolders[giMediaCategory].getAbsolutePath() + File.separator +
                gict_TagToDelete.iTagID;

        //Loop through all catalog items and look for items that contain the tag to delete:
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntryCatalogRecord : globalClass.gtmCatalogLists.get(giMediaCategory).entrySet()){
            String sTags = tmEntryCatalogRecord.getValue().sTags;
            ArrayList<Integer> aliTags = GlobalClass.getIntegerArrayFromString(sTags, ",");

            if((tmEntryCatalogRecord.getValue().sFolder_Name.equals(String.valueOf(gict_TagToDelete.iTagID)))
                    || (aliTags.contains(gict_TagToDelete.iTagID))){
                //If this catalog item is in the folder of the tag to be deleted or if this catalog
                // item contains the tag...

                String sNewTagFolderDestination;

                //Videos and images are sorted into folders based on their first tag.
                //If their first tag is deleted, move the file to the new first tag folder.
                if((giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                        giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES)){

                    if(tmEntryCatalogRecord.getValue().sFolder_Name.equals(String.valueOf(gict_TagToDelete.iTagID))) {
                        //If this catalog item is currently stored in the tag folder, move the file
                        // to it's next tag folder.

                        String sSourcePath = globalClass.gfCatalogFolders[giMediaCategory].getAbsolutePath() + File.separator +
                                tmEntryCatalogRecord.getValue().sFolder_Name + File.separator +
                                tmEntryCatalogRecord.getValue().sFilename;

                        //Get a default folder for the move (to be used if there are no other tags
                        //  assigned to this item):
                        sNewTagFolderDestination = GlobalClass.gsUnsortedFolderName;

                        //Get the tag ID of the first tag assigned to this item that is not the
                        //  tag to be deleted (if such a tag exists for this item). We will use this
                        //  as the item's new destination folder:
                        for(int i = 0; i < aliTags.size(); i++) {
                            if(!aliTags.get(i).equals(gict_TagToDelete.iTagID)) {
                                sNewTagFolderDestination = aliTags.get(i).toString();
                                break;
                            }
                        }

                        String sDestinationPath = globalClass.gfCatalogFolders[giMediaCategory].getAbsolutePath() + File.separator +
                                sNewTagFolderDestination + File.separator +
                                tmEntryCatalogRecord.getValue().sFilename;
                        String sDestinationFolder = globalClass.gfCatalogFolders[giMediaCategory].getAbsolutePath() + File.separator +
                                sNewTagFolderDestination;

                        File fInternalCatalogItem = new File(sSourcePath);
                        if (fInternalCatalogItem.exists()) {

                            File fDestinationFolder = new File(sDestinationFolder);
                            boolean bFolderOk = false;
                            if (!fDestinationFolder.exists()) {
                                if (fDestinationFolder.mkdirs()) {
                                    bFolderOk = true;
                                }
                            } else {
                                bFolderOk = true;
                            }

                            if (bFolderOk) {
                                //Move the file:
                                try {
                                    Path temp = Files.move(fInternalCatalogItem.toPath(), Paths.get(sDestinationPath));
                                    if (temp == null) {
                                        String sMessage = "Could not move file " + fInternalCatalogItem.toPath() + ".";
                                        globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
                                        return Result.failure();
                                    } else {
                                        tmEntryCatalogRecord.getValue().sFolder_Name = sNewTagFolderDestination;
                                    }
                                } catch (Exception e) {
                                    String sMessage = "Could not move file " + fInternalCatalogItem.toPath() + ".\n" + e.getMessage();
                                    globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
                                    return Result.failure();
                                }

                            } else {
                                String sMessage = "Could not create catalog data folder " + fDestinationFolder.getPath() + ".";
                                globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
                                return Result.failure();
                            }
                        } else {
                            String sMessage = "File source not found: " + sDestinationPath;
                            globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
                            return Result.failure();
                        }
                    }

                } //End move if catalog item is 'video or image' and tag to delete is first tag.

                //Delete the tag from the record and move on.
                //Form the new Tag string:
                ArrayList<Integer> aliNewTags = new ArrayList<>();
                for (Integer iTagID : aliTags) {
                    if (!iTagID.equals(gict_TagToDelete.iTagID)) {
                        aliNewTags.add(iTagID);
                    }
                }
                tmEntryCatalogRecord.getValue().sTags = GlobalClass.formDelimitedString(aliNewTags, ",");
                //Update the record and the catalog file:
                globalClass.CatalogDataFile_UpdateRecord(tmEntryCatalogRecord.getValue());

            } //End if the record contains the tag

        } //End for loop through catalog.

        //Inform program of a need to update the tags histogram:
        globalClass.gbTagHistogramRequiresUpdate[giMediaCategory] = true;

        //If it was a video or image, now delete the tag folder (there should not be any more files in that folder:
        //(if the folder exists, as tag may have only been a secondary tag for items).
        if(giMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                giMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES){

            File fTagFolderPath = new File(sTagFolderPath);
            if(fTagFolderPath.exists()){
                File[] fFileList = fTagFolderPath.listFiles();
                if(fFileList != null) {
                    if (fFileList.length == 0) {
                        try {
                            if (!fTagFolderPath.delete()) {
                                String sMessage = "Could not delete tag folder:\n" + fTagFolderPath.getPath();
                                globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
                                return Result.failure();
                            }
                        } catch (Exception e) {
                            String sMessage = "Could not delete tag folder:\n" + fTagFolderPath.getPath() + "\n" + e.getMessage();
                            globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
                            return Result.failure();
                        }
                    } else {
                        String sMessage = "Tag delete aborted. Tag folder not empty. Could not delete tag folder:\n" + fTagFolderPath.getPath();
                        globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
                        return Result.failure();
                    }
                }
            }
        }

        //Remove tag from reference list:
        File fCatalogTagsFile = globalClass.gfCatalogTagsFiles[giMediaCategory];
        try {
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fCatalogTagsFile.getAbsolutePath()));
            sbBuffer.append(brReader.readLine());  //Read the header. //todo: replace with getHeader().
            sbBuffer.append("\n");

            String sLine = brReader.readLine();
            while (sLine != null) {

                ItemClass_Tag ict = GlobalClass.ConvertFileLineToTagItem(sLine);

                if(!gict_TagToDelete.iTagID.equals(ict.iTagID)){
                    //If this is not the tag to be deleted:
                    sbBuffer.append(sLine);
                    sbBuffer.append("\n");
                }

                // read next line
                sLine = brReader.readLine();
            }
            brReader.close();

            //Write the data to the file:
            FileWriter fwNewCatalogContentsFile = new FileWriter(fCatalogTagsFile, false);
            fwNewCatalogContentsFile.write(sbBuffer.toString());
            fwNewCatalogContentsFile.flush();
            fwNewCatalogContentsFile.close();
        } catch (Exception e) {
            String sMessage = "Problem updating Tags.dat.\n" + fCatalogTagsFile.getPath() + "\n\n" + e.getMessage();
            globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
            return Result.failure();
        }

        //Remove the tag from memory:
        globalClass.gtmCatalogTagReferenceLists.get(giMediaCategory).remove(gict_TagToDelete.sTagText);
        if(globalClass.gtmCatalogTagReferenceLists.get(giMediaCategory).containsKey(gict_TagToDelete.sTagText)){
            String sMessage = "Unable to find tag in memory.";
            globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
            return Result.failure();
        }

        //Check to see if the tag is included in the "restricted tags" listing for the media category,
        //  and if so, remove it:
        //Get tag restrictions preferences:
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(gContext);
        Set<String> ssCatalogTagsRestricted = sharedPreferences.getStringSet(GlobalClass.gsRestrictedTagsPreferenceNames[giMediaCategory], null);
        //Attempt to match the restricted tag text IDs from the preferences to the Tag ID:
        boolean bUpdatePreference = false;
        Set<String> ssNewRestrictedTags = new HashSet<>();
        if(ssCatalogTagsRestricted != null) {
            for (String sRestrictedTag : ssCatalogTagsRestricted) {
                Integer iRestrictedTag = Integer.parseInt(sRestrictedTag);
                if(gict_TagToDelete.iTagID.equals(iRestrictedTag)){
                    //This tag to be deleted is one of the restricted tags. Remove this tag from the list of restricted tags:
                    bUpdatePreference = true;
                } else {
                    ssNewRestrictedTags.add(sRestrictedTag);
                }
            }
        }
        if(bUpdatePreference) {
            SharedPreferences.Editor sharedPreferencedEditor = sharedPreferences.edit();
            sharedPreferencedEditor.putStringSet(GlobalClass.gsRestrictedTagsPreferenceNames[giMediaCategory], ssNewRestrictedTags);
            sharedPreferencedEditor.apply();
        }


        //Send a broadcast that this process is complete.
        Intent broadcastIntent_NotifyTagDeleteComplete = new Intent();
        broadcastIntent_NotifyTagDeleteComplete.putExtra(GlobalClass.EXTRA_TAG_DELETE_COMPLETE, true);
        broadcastIntent_NotifyTagDeleteComplete.setAction(Fragment_TagEditor_4_DeleteTag.TagEditorServiceResponseReceiver.TAG_EDITOR_SERVICE_ACTION_RESPONSE);
        broadcastIntent_NotifyTagDeleteComplete.addCategory(Intent.CATEGORY_DEFAULT);
        //sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_NotifyTagDeleteComplete);

        return Result.success();
    }



}
