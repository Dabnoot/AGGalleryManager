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
    private static final String ACTION_ADD_TAGS = "com.agcurations.aggallerymanager.action.add_tags";

    // Parameters
    private static final String EXTRA_TAG_TO_BE_DELETED = "com.agcurations.aggallerymanager.extra.TAG_TO_BE_DELETED";
    private static final String EXTRA_MEDIA_CATEGORY = "com.agcurations.aggallerymanager.extra.MEDIA_CATEGORY";

    public static final String EXTRA_TAG_DELETE_COMPLETE = "com.agcurations.aggallerymanager.extra.TAG_DELETE_COMPLETE";

    public static final String EXTRA_ARRAYLIST_STRING_TAGS_TO_ADD = "com.agcurations.aggallerymanager.extra.TAGS_TO_ADD";
    public static final String EXTRA_ARRAYLIST_ITEMCLASSTAGS_ADDED_TAGS = "com.agcurations.aggallerymanager.extra.ADDED_TAGS";



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

    public static void startActionAddTags(Context context, ArrayList<String> alsTags, int iMediaCategory) {
        Intent intent = new Intent(context, Service_TagEditor.class);
        intent.setAction(ACTION_ADD_TAGS);
        intent.putExtra(EXTRA_ARRAYLIST_STRING_TAGS_TO_ADD, alsTags);
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
            } else if (ACTION_ADD_TAGS.equals(action)){
                final ArrayList<String> alsTags = intent.getStringArrayListExtra(EXTRA_ARRAYLIST_STRING_TAGS_TO_ADD);
                final int iMediaCategory = intent.getIntExtra(EXTRA_MEDIA_CATEGORY,0);
                handleActionAddTags(alsTags, iMediaCategory);
            }
        }
    }


    public static final String EXTRA_BOOL_PROBLEM = "EXTRA_BOOL_PROBLEM";
    public static final String EXTRA_STRING_PROBLEM = "EXTRA_BOOL_PROBLEM_MESSAGE";
    void problemNotificationConfig(String sMessage){

        //Todo: Set intent action via .setAction so that the intent is properly captured by a response receiver.
        Intent broadcastIntent_Notification = new Intent();
        broadcastIntent_Notification.putExtra(EXTRA_BOOL_PROBLEM, true);
        broadcastIntent_Notification.putExtra(EXTRA_STRING_PROBLEM, sMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent_Notification);
    }

    private void handleActionDeleteTag(ItemClass_Tag ict_TagToDelete, int iMediaCategory) {

        GlobalClass globalClass;
        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        String sTagFolderPath = globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                ict_TagToDelete.iTagID;

        //Loop through all catalog items and look for items that contain the tag to delete:
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntryCatalogRecord : globalClass.gtmCatalogLists.get(iMediaCategory).entrySet()){
            String sTags = tmEntryCatalogRecord.getValue().sTags;
            ArrayList<Integer> aliTags = GlobalClass.getIntegerArrayFromString(sTags, ",");

            if((tmEntryCatalogRecord.getValue().sFolder_Name.equals(String.valueOf(ict_TagToDelete.iTagID)))
                || (aliTags.contains(ict_TagToDelete.iTagID))){
                //If this catalog item is in the folder of the tag to be deleted or if this catalog
                // item contains the tag...

                String sNewTagFolderDestination;

                //Videos and images are sorted into folders based on their first tag.
                //If their first tag is deleted, move the file to the new first tag folder.
                if((iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                        iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES)){

                    if(tmEntryCatalogRecord.getValue().sFolder_Name.equals(String.valueOf(ict_TagToDelete.iTagID))) {
                        //If this catalog item is currently stored in the tag folder, move the file
                        // to it's next tag folder.

                        String sSourcePath = globalClass.gfCatalogFolders[iMediaCategory].getAbsolutePath() + File.separator +
                                tmEntryCatalogRecord.getValue().sFolder_Name + File.separator +
                                tmEntryCatalogRecord.getValue().sFilename;

                        //Get a default folder for the move (to be used if there are no other tags
                        //  assigned to this item):
                        sNewTagFolderDestination = GlobalClass.gsUnsortedFolderName;

                        //Get the tag ID of the first tag assigned to this item that is not the
                        //  tag to be deleted (if such a tag exists for this item). We will use this
                        //  as the item's new destination folder:
                        for(int i = 0; i < aliTags.size(); i++) {
                            if(!aliTags.get(i).equals(ict_TagToDelete.iTagID)) {
                                sNewTagFolderDestination = aliTags.get(i).toString();
                                break;
                            }
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
                                    } else {
                                        tmEntryCatalogRecord.getValue().sFolder_Name = sNewTagFolderDestination;
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
                    }

                } //End move if catalog item is 'video or image' and tag to delete is first tag.

                //Delete the tag from the record and move on.
                //Form the new Tag string:
                ArrayList<Integer> aliNewTags = new ArrayList<>();
                for (Integer iTagID : aliTags) {
                    if (!iTagID.equals(ict_TagToDelete.iTagID)) {
                        aliNewTags.add(iTagID);
                    }
                }
                tmEntryCatalogRecord.getValue().sTags = GlobalClass.formDelimitedString(aliNewTags, ",");
                //Update the record and the catalog file:
                globalClass.CatalogDataFile_UpdateRecord(tmEntryCatalogRecord.getValue());

            } //End if the record contains the tag

        } //End for loop through catalog.


        //If it was a video or image, now delete the tag folder (there should not be any more files in that folder:
        //(if the folder exists, as tag may have only been a secondary tag for items).
        if(iMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS ||
                iMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES){

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
            sbBuffer.append(brReader.readLine());  //Read the header. //todo: replace with getHeader().
            sbBuffer.append("\n");

            String sLine = brReader.readLine();
            while (sLine != null) {

                ItemClass_Tag ict = GlobalClass.ConvertFileLineToTagItem(sLine);

                if(!ict_TagToDelete.iTagID.equals(ict.iTagID)){
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
        globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).remove(ict_TagToDelete.sTagText);
        if(globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).containsKey(ict_TagToDelete.sTagText)){
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
                if(ict_TagToDelete.iTagID.equals(iRestrictedTag)){
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

    private void handleActionAddTags(ArrayList<String> alsTags, int iMediaCategory){

        ArrayList<ItemClass_Tag> itemClass_tags = null;

        GlobalClass globalClass = (GlobalClass) getApplicationContext();
        itemClass_tags = globalClass.TagDataFile_CreateNewRecords(alsTags, iMediaCategory);

        //Broadcast the completion of this task:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Fragment_Import_3a_ItemDownloadTagImport.AddTagsServiceResponseReceiver.ADD_TAGS_SERVICE_EXECUTE_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        if(itemClass_tags != null) {
            broadcastIntent.putExtra(EXTRA_ARRAYLIST_ITEMCLASSTAGS_ADDED_TAGS, itemClass_tags);
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }

}