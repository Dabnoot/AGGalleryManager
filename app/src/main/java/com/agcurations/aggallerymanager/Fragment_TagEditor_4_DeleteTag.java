package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class Fragment_TagEditor_4_DeleteTag extends Fragment {

    private ViewModel_TagEditor gViewModelTagEditor;

    TagEditorServiceResponseReceiver tagEditorServiceResponseReceiver;

    Button gButton_DeleteTag = null;

    ProgressBar gProgressBar_Progress;
    TextView gTextView_ProgressBarText;

    TextView gTextView_DeleteTagSubText;
    static String gsDeleteTagSubTextDefault = "Select a tag to delete.";

    private ItemClass_Tag gictTagToBeDeleted;

    private Fragment_SelectTags gFragment_selectTags;
    ViewModel_Fragment_SelectTags gViewModel_fragment_selectTags;

    public Fragment_TagEditor_4_DeleteTag() {
        // Required empty public constructor
    }

    public static Fragment_TagEditor_4_DeleteTag newInstance() {
        return new Fragment_TagEditor_4_DeleteTag();
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Instantiate the ViewModel sharing data between fragments:
        gViewModelTagEditor = new ViewModelProvider(getActivity()).get(ViewModel_TagEditor.class);

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Tags_DeleteTag.DELETE_TAGS_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        tagEditorServiceResponseReceiver = new TagEditorServiceResponseReceiver();
        //registerReceiver(tagEditorServiceResponseReceiver, filter);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(tagEditorServiceResponseReceiver,filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tag_editor_4_delete_tag, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getView() != null) {
            gButton_DeleteTag = getView().findViewById(R.id.button_DeleteTag);
            gButton_DeleteTag.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    button_DeleteTag_Click(v);
                }
            });

            Button button_Finish = getView().findViewById(R.id.button_Finish);
            button_Finish.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(getActivity() != null) {
                        ((Activity_TagEditor) getActivity()).callForFinish();
                    }
                }
            });

            gProgressBar_Progress = getView().findViewById(R.id.progressBar_Progress);
            gTextView_ProgressBarText = getView().findViewById(R.id.textView_ProgressBarText);

            gTextView_DeleteTagSubText = getView().findViewById(R.id.textView_DeleteTagSubText);
        }

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        if(getActivity() == null){
            return;
        }
        gViewModel_fragment_selectTags = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_SelectTags.class);
        gViewModel_fragment_selectTags.altiTagsSelected.removeObservers(getViewLifecycleOwner());
        gViewModel_fragment_selectTags.bFilterOnXrefTags = false;

        //Populate the tags fragment:
        //Start the tag selection fragment:
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        gFragment_selectTags = new Fragment_SelectTags();
        Bundle fragment_selectTags_args = new Bundle();
        fragment_selectTags_args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, gViewModelTagEditor.iTagEditorMediaCategory);
        gFragment_selectTags.setArguments(fragment_selectTags_args);
        fragmentTransaction.replace(R.id.child_fragment_tag_selector, gFragment_selectTags);
        fragmentTransaction.commit();

        gFragment_selectTags.giSelectionMode = Fragment_SelectTags.SINGLE_SELECT;
        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<ItemClass_Tag>> observerSelectedTags = getNewTagObserver();
        gViewModel_fragment_selectTags.altiTagsSelected.observe(getViewLifecycleOwner(), observerSelectedTags);


    }

    private Observer<ArrayList<ItemClass_Tag>> getNewTagObserver() {
        return new Observer<ArrayList<ItemClass_Tag>>() {
            @Override
            public void onChanged(ArrayList<ItemClass_Tag> tagItems) {

                //In our case, there should only be one tag selected.
                if(tagItems.size() > 0) {
                    gictTagToBeDeleted = tagItems.get(0);
                    gButton_DeleteTag.setEnabled(true);
                    String sDeleteTagSubText = "Tag '" + gictTagToBeDeleted.sTagText + "' selected for deletion.";
                    String sMaturityCode = AdapterMaturityRatings.MATURITY_RATINGS[gictTagToBeDeleted.iMaturityRating][AdapterMaturityRatings.MATURITY_RATING_CODE_INDEX];
                    sDeleteTagSubText += "\nTag has rating '" + sMaturityCode + "'.";
                    sDeleteTagSubText += "\nTag is currently used by " + gictTagToBeDeleted.iHistogramCount + " items viewable by the current user's maturity limit.";
                    if(gictTagToBeDeleted.alsTagApprovedUsers.size() == 1){
                        if(gictTagToBeDeleted.alsTagApprovedUsers.get(0).equals(GlobalClass.gicuCurrentUser.sUserName)) {
                            sDeleteTagSubText += "\nTag is private to the current user.";
                        }
                    }
                    gTextView_DeleteTagSubText.setText(sDeleteTagSubText);
                } else {
                    gictTagToBeDeleted = null;
                    gButton_DeleteTag.setEnabled(false);
                    gTextView_DeleteTagSubText.setText(gsDeleteTagSubTextDefault);
                }

            }
        };
    }


    @Override
    public void onDestroy() {
        if(getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(tagEditorServiceResponseReceiver);
        }
        super.onDestroy();
    }


    private void button_DeleteTag_Click(View v){
        if (getActivity() == null || getView() == null || gictTagToBeDeleted == null) {
            return;
        }

        String sConfirmationMessage = "This action will delete the tag from the database. The tag will be" +
                " removed from all catalog items. The user permissions and maturity rating for each" +
                " catalog item previously holding the tag will be recalculated. If this tag has a" +
                " high maturity rating and all other tags applied to the catalog item have a lower" +
                " maturity, the lower maturity rating will be applied to the catalog item.\n" +
                "If there are no remaining tags to the catalog item, the default maturity will be applied. This could result in" +
                " mature content or content 'currently private to the current user' being exposed to" +
                " inappropriate users, or in the case of this tag being the last tag and of a lower" +
                " maturity than the default, the catalog item may \"disappear\" from view of some" +
                " low maturity users as the higher default maturity is applied. Use the filter feature" +
                " of the Catalog Viewer to determine the content to which this tag has been applied.\n" +
                "Confirm tag deletion: ";
        sConfirmationMessage = sConfirmationMessage + gictTagToBeDeleted.sTagText;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustomStyle);
        builder.setTitle("Delete Tag");
        builder.setMessage(sConfirmationMessage);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                DeleteTag();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog adConfirmationDialog = builder.create();
        adConfirmationDialog.show();
    }

    private void DeleteTag(){
        if(getContext() == null || gictTagToBeDeleted == null) {
            return;
        }
        GlobalClass.gabDataLoaded.set(false); //Don't let the user get into any catalog until processing is complete.
        String sTagRecord = GlobalClass.getTagRecordString(gictTagToBeDeleted);
        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
        Data dataDeleteTag = new Data.Builder()
                .putString(GlobalClass.EXTRA_CALLER_ID, "Fragment_TagEditor_4_DeleteTag:DeleteTag()")
                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY, gViewModelTagEditor.iTagEditorMediaCategory)
                .putString(GlobalClass.EXTRA_TAG_TO_BE_DELETED, sTagRecord)
                .build();
        OneTimeWorkRequest otwrDeleteTag = new OneTimeWorkRequest.Builder(Worker_Tags_DeleteTag.class)
                .setInputData(dataDeleteTag)
                .addTag(Worker_Tags_DeleteTag.TAG_WORKER_TAGS_DELETETAG) //To allow finding the worker later.
                .build();
        WorkManager.getInstance(getContext()).enqueue(otwrDeleteTag);

        gViewModelTagEditor.bTagDeleted = true;
    }



    public class TagEditorServiceResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //Errors are checked for in Activity_TagEditor.
            //Check to see if this is a message indicating that a tag deletion is complete:

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

            if(gProgressBar_Progress != null && gTextView_ProgressBarText != null) {
                if (bUpdatePercentComplete) {
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if (gProgressBar_Progress != null) {
                        gProgressBar_Progress.setProgress(iAmountComplete);
                    }
                    if (iAmountComplete == 100) {
                        assert gProgressBar_Progress != null;
                        gProgressBar_Progress.setVisibility(View.INVISIBLE);
                        gTextView_ProgressBarText.setVisibility(View.INVISIBLE);
                    } else {
                        assert gProgressBar_Progress != null;
                        gProgressBar_Progress.setVisibility(View.VISIBLE);
                        gTextView_ProgressBarText.setVisibility(View.VISIBLE);
                    }

                }
                if (bUpdateProgressBarText) {
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if (gTextView_ProgressBarText != null) {
                        gTextView_ProgressBarText.setText(sProgressBarText);
                    }
                }
            }

            //Check to see if this is a completion notification about a tag-delete operation:

            boolean bTagDeleteComplete = intent.getBooleanExtra(GlobalClass.EXTRA_TAG_DELETE_COMPLETE,false);
            if(bTagDeleteComplete){
                GlobalClass.gabDataLoaded.set(true); //Allow the user back into catalog viewers.
                gFragment_selectTags.initListViewData();
                Toast.makeText(context, "Tag deletion complete.", Toast.LENGTH_SHORT).show();
            }

        }
    }

}