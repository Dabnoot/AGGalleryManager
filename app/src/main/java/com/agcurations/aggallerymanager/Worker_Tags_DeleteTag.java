package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
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

        //Loop through all catalog items and look for items that contain the tag to delete:
        for(Map.Entry<String, ItemClass_CatalogItem> tmEntryCatalogRecord : globalClass.gtmCatalogLists.get(giMediaCategory).entrySet()){
            String sTags = tmEntryCatalogRecord.getValue().sTags;
            ArrayList<Integer> aliTags = GlobalClass.getIntegerArrayFromString(sTags, ",");

            if(aliTags.contains(gict_TagToDelete.iTagID)){
                //If this catalog item contains the tag...
                //Delete the tag from the record.
                //Form the new Tag string:
                ArrayList<Integer> aliNewTags = new ArrayList<>();
                for (Integer iTagID : aliTags) {
                    if (!iTagID.equals(gict_TagToDelete.iTagID)) {
                        aliNewTags.add(iTagID);
                    }
                }
                tmEntryCatalogRecord.getValue().sTags = GlobalClass.formDelimitedString(aliNewTags, ",");
                tmEntryCatalogRecord.getValue().aliTags = new ArrayList<>(aliNewTags);
                //Update the record and the catalog file:
                globalClass.CatalogDataFile_UpdateRecord(tmEntryCatalogRecord.getValue());

            } //End if the record contains the tag

        } //End for loop through catalog.

        //Inform program of a need to update the tags histogram:
        globalClass.gbTagHistogramRequiresUpdate[giMediaCategory] = true;

        //Remove tag from reference list:
        Uri uriCatalogTagsFile = GlobalClass.gUriCatalogTagsFiles[giMediaCategory];
        try {
            StringBuilder sbBuffer = new StringBuilder();
            BufferedReader brReader;
            InputStream isCatalogTagsFile = GlobalClass.gcrContentResolver.openInputStream(uriCatalogTagsFile);
            if(isCatalogTagsFile == null){
                String sMessage = "Problem reading Tags.dat.\n" + uriCatalogTagsFile;
                globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
                return Result.failure();
            }
            brReader = new BufferedReader(new InputStreamReader(isCatalogTagsFile));
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

            isCatalogTagsFile.close();

            //Write the data to the file:
            OutputStream osCatalogTagsFile = GlobalClass.gcrContentResolver.openOutputStream(uriCatalogTagsFile, "wt");
            if (osCatalogTagsFile == null){
                String sMessage = "Problem updating Tags.dat. Cannot open output stream for file \n" + uriCatalogTagsFile;
                globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
                return Result.failure();
            }
            osCatalogTagsFile.write(sbBuffer.toString().getBytes(StandardCharsets.UTF_8));
            osCatalogTagsFile.flush();
            osCatalogTagsFile.close();
        } catch (Exception e) {
            String sMessage = "Problem updating Tags.dat.\n" + uriCatalogTagsFile + "\n\n" + e.getMessage();
            globalClass.problemNotificationConfig(sMessage, gsIntentActionFilter);
            return Result.failure();
        }

        //Remove the tag from memory:
        globalClass.gtmCatalogTagReferenceLists.get(giMediaCategory).remove(gict_TagToDelete.iTagID);
        if(globalClass.gtmCatalogTagReferenceLists.get(giMediaCategory).containsKey(gict_TagToDelete.iTagID)){
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
