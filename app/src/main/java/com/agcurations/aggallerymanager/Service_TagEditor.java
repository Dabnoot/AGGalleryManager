package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
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

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;


public class Service_TagEditor extends IntentService {

    // Action names that describe tasks that this IntentService can perform:
    private static final String ACTION_DELETE_TAG = "com.agcurations.aggallerymanager.action.delete_tag";

    // Parameters
    private static final String EXTRA_TAG_TO_BE_DELETED = "com.agcurations.aggallerymanager.extra.TAG_TO_BE_DELETED";
    private static final String EXTRA_MEDIA_CATEGORY = "com.agcurations.aggallerymanager.extra.MEDIA_CATEGORY";

    public static final String EXTRA_TAG_DELETE_COMPLETE = "com.agcurations.aggallerymanager.extra.TAG_DELETE_COMPLETE";

    public Service_TagEditor() {
        super("Service_TagEditor");
    }

    public static void startActionDeleteTag(Context context, ItemClass_Tag ict_TagToDelete, int iMediaCategory) {
        Intent intent = new Intent(context, Service_TagEditor.class);
        intent.setAction(ACTION_DELETE_TAG);
        intent.putExtra(EXTRA_TAG_TO_BE_DELETED, ict_TagToDelete);
        intent.putExtra(EXTRA_MEDIA_CATEGORY, iMediaCategory);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DELETE_TAG.equals(action)) {
                final ItemClass_Tag ict_TagToDelete = (ItemClass_Tag) intent.getSerializableExtra(EXTRA_TAG_TO_BE_DELETED);
                final int iMediaCategory = intent.getIntExtra(EXTRA_MEDIA_CATEGORY,0);
                handleActionDeleteTag(ict_TagToDelete, iMediaCategory);
            }
        }
    }


    public static final String EXTRA_BOOL_PROBLEM = "EXTRA_BOOL_PROBLEM";
    public static final String EXTRA_BOOL_PROBLEM_MESSAGE = "EXTRA_BOOL_PROBLEM_MESSAGE";
    void problemNotificationConfig(String sMessage){
        Intent broadcastIntent_Notification = new Intent();
        broadcastIntent_Notification.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Notification.putExtra(EXTRA_BOOL_PROBLEM_MESSAGE, sMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Notification);
    }

    private void handleActionDeleteTag(ItemClass_Tag ict_TagToDelete, int iMediaCategory) {

        GlobalClass globalClass;
        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();


        //Loop through all catalog items and look for items that contain the tag to delete:
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntryCatalogRecord : globalClass.gtmCatalogLists.get(iMediaCategory).entrySet()){
            String sTags = tmEntryCatalogRecord.getValue().sTags;
            ArrayList<Integer> aliTags = GlobalClass.getIntegerArrayFromString(sTags, ",");

            if(aliTags.contains(ict_TagToDelete.TagID)){
                //If this item contains the tag:

                String sNewTagFolderDestination;


                //Videos and images are sorted into folders based on their first tag.
                //If their first tag is deleted, move the file to the new first tag folder.
                if((iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                        iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) &&
                        aliTags.get(0).equals(ict_TagToDelete.TagID)) {

                    sNewTagFolderDestination = GlobalClass.gsUnsortedFolderName;


                    String sSourcePath = globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                            tmEntryCatalogRecord.getValue().sFolder_Name + File.separator +
                            tmEntryCatalogRecord.getValue().sFilename;

                    if (aliTags.size() > 1) {
                        sNewTagFolderDestination = aliTags.get(1).toString();
                    }

                    String sDestinationPath = globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                            sNewTagFolderDestination + File.separator +
                            tmEntryCatalogRecord.getValue().sFilename;
                    String sDestinationFolder = globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
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
                                    problemNotificationConfig(sMessage);
                                    return;
                                }
                            } catch (Exception e) {
                                String sMessage = "Could not move file " + fInternalCatalogItem.toPath() + ".\n" + e.getMessage();
                                problemNotificationConfig(sMessage);
                                return;
                            }

                        } else {
                            String sMessage = "Could not create catalog data folder " + fDestinationFolder.getPath() + ".";
                            problemNotificationConfig(sMessage);
                            return;
                        }
                    } else {
                        String sMessage = "File source not found: " + sDestinationPath;
                        problemNotificationConfig(sMessage);
                        return;
                    }


                } //File move (if video or image and tag to delete is first tag.

                //Delete the tag from the record and move on.
                //Form the new Tag string:
                ArrayList<Integer> aliNewTags = new ArrayList<>();
                for (Integer iTagID : aliTags) {
                    if (!iTagID.equals(ict_TagToDelete.TagID)) {
                        aliNewTags.add(iTagID);
                    }
                }
                String sNewTags = GlobalClass.formDelimitedString(aliNewTags, ",");
                //Update the record and the catalog file:
                globalClass.CatalogDataFile_UpdateRecord(tmEntryCatalogRecord.getValue());

            } //End if the record contains the tag

        } //End for loop through catalog.


        //If it was a video or image, now delete the tag folder (there should not be any more files in that folder:
        //(if the folder exists, as tag may have only been a secondary tag for items).
        if(iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES){
            String sTagFolderPath = globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                    ict_TagToDelete.TagID;
            File fTagFolderPath = new File(sTagFolderPath);
            if(fTagFolderPath.exists()){
                File[] fFileList = fTagFolderPath.listFiles();
                if(fFileList != null) {
                    if (fFileList.length == 0) {
                        try {
                            if (!fTagFolderPath.delete()) {
                                String sMessage = "Could not delete tag folder:\n" + fTagFolderPath.getPath();
                                problemNotificationConfig(sMessage);
                                return;
                            }
                        } catch (Exception e) {
                            String sMessage = "Could not delete tag folder:\n" + fTagFolderPath.getPath() + "\n" + e.getMessage();
                            problemNotificationConfig(sMessage);
                            return;
                        }
                    } else {
                        String sMessage = "Tag delete aborted. Tag folder not empty. Could not delete tag folder:\n" + fTagFolderPath.getPath();
                        problemNotificationConfig(sMessage);
                        return;
                    }
                }
            }
        }

        //Remove tag from reference list:
        File fCatalogTagsFile = globalClass.gfCatalogTagsFiles[iMediaCategory];
        try {
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fCatalogTagsFile.getAbsolutePath()));
            sbBuffer.append(brReader.readLine());  //Read the header.
            sbBuffer.append("\n");

            String[] sFields;
            String sLine = brReader.readLine();
            while (sLine != null) {

                sFields = sLine.split("\t",-1);
                //De-jumble the data read from the file:

                String sTagID = GlobalClass.JumbleStorageText(sFields[GlobalClass.TAG_ID_INDEX]);
                Integer iTagID = Integer.parseInt(sTagID);

                if(!ict_TagToDelete.TagID.equals(iTagID)){
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
            problemNotificationConfig(sMessage);
            return;
        }

        //Remove the tag from memory:
        globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).remove(ict_TagToDelete.TagText);
        if(globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).containsKey(ict_TagToDelete.TagText)){
            String sMessage = "Unable to find tag in memory.";
            problemNotificationConfig(sMessage);
            return;
        }

        //Check to see if the tag is included in the "restricted tags" listing for the media category,
        //  and if so, remove it:
        //Get tag restrictions preferences:
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> ssCatalogTagsRestricted = sharedPreferences.getStringSet(GlobalClass.gsRestrictedTagsPreferenceNames[iMediaCategory], null);
        //Attempt to match the restricted tag text IDs from the preferences to the Tag ID:
        boolean bUpdatePreference = false;
        Set<String> ssNewRestrictedTags = new HashSet<>();
        if(ssCatalogTagsRestricted != null) {
            for (String sRestrictedTag : ssCatalogTagsRestricted) {
                Integer iRestrictedTag = Integer.parseInt(sRestrictedTag);
                if(ict_TagToDelete.TagID.equals(iRestrictedTag)){
                    //This tag to be deleted is one of the restricted tags. Remove this tag from the list of restricted tags:
                    bUpdatePreference = true;
                } else {
                    ssNewRestrictedTags.add(sRestrictedTag);
                }
            }
        }
        if(bUpdatePreference) {
            SharedPreferences.Editor sharedPreferencedEditor = sharedPreferences.edit();
            sharedPreferencedEditor.putStringSet(GlobalClass.gsRestrictedTagsPreferenceNames[iMediaCategory], ssNewRestrictedTags);
            sharedPreferencedEditor.apply();
        }


        //Send a broadcast that this process is complete.
        Intent broadcastIntent_NotifyTagDeleteComplete = new Intent();
        broadcastIntent_NotifyTagDeleteComplete.putExtra(EXTRA_TAG_DELETE_COMPLETE, true);
        broadcastIntent_NotifyTagDeleteComplete.setAction(Fragment_TagEditor_4_DeleteTag.TagEditorServiceResponseReceiver.TAG_EDITOR_SERVICE_ACTION_RESPONSE);
        broadcastIntent_NotifyTagDeleteComplete.addCategory(Intent.CATEGORY_DEFAULT);
        //sendBroadcast(broadcastIntent_GetDirectoryContentsResponse);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_NotifyTagDeleteComplete);

    }



}