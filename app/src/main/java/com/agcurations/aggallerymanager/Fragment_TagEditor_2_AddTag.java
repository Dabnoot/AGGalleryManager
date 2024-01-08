package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class Fragment_TagEditor_2_AddTag extends Fragment {

    private GlobalClass globalClass;
    private ViewModel_TagEditor gViewModelTagEditor;

    RelativeLayout gRelativeLayout_UserSelection;
    RelativeLayout.LayoutParams gLayoutParams_UserSelection;

    ProgressBar gProgressBar_Progress;
    TextView gTextView_ProgressBarText;

    TagEditorServiceResponseReceiver tagEditorServiceResponseReceiver;

    AdapterUserList gAdapterUserPool;
    AdapterUserList gAdapterApprovedUsers;

    int giInitialMaturityRating = -1;
    ArrayList<String> galsInitialApprovedUsers;

    private ArrayList<ItemClass_Tag> galNewTags;

    private EditText gEditText_TagText;
    private EditText gEditText_TagDescription;
    private Spinner gSpinner_AgeRating;
    private CheckBox gCheckBox_SetApprovedUsers;

    private Fragment_SelectTags gFragment_selectTags;
    ViewModel_Fragment_SelectTags gViewModel_fragment_selectTags;

    private ItemClass_Tag gictTagIDInEditMode;

    public Fragment_TagEditor_2_AddTag() {
        // Required empty public constructor
    }

    public static Fragment_TagEditor_2_AddTag newInstance() {
        return new Fragment_TagEditor_2_AddTag();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Calling Application class (see application tag in AndroidManifest.xml)
        if(getActivity() != null) {
            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }
        //Instantiate the ViewModel sharing data between fragments:
        gViewModelTagEditor = new ViewModelProvider(getActivity()).get(ViewModel_TagEditor.class);

        galNewTags = new ArrayList<>();
        galsInitialApprovedUsers = new ArrayList<>();

        //Configure a response receiver to listen for updates from the Data Service:
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        tagEditorServiceResponseReceiver = new TagEditorServiceResponseReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(tagEditorServiceResponseReceiver,filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tag_editor_2_add_tag, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        if (getView() == null || getActivity() == null) {
            return;
        }

        gEditText_TagText = getView().findViewById(R.id.editText_TagText);
        gEditText_TagDescription = getView().findViewById(R.id.editText_TagDescription);

        gSpinner_AgeRating = getView().findViewById(R.id.spinner_ContentMaturity);
        ArrayList<String[]> alsTemp = new ArrayList<>();

        for(int i = 0; i < AdapterMaturityRatings.MATURITY_RATINGS.length; i++){
            if(GlobalClass.gicuCurrentUser.iMaturityLevel >= i) {
                //Don't let the user add a tag or modify a tag to a maturity level greater than their user level, or the tag will be lost
                //  to them unless their user rating is modified.
                String[] sESRBRating = AdapterMaturityRatings.MATURITY_RATINGS[i];
                alsTemp.add(sESRBRating);
            }
        }
        if(getContext() == null) return;
        AdapterMaturityRatings atarSpinnerAdapter = new AdapterMaturityRatings(getContext(), R.layout.spinner_item_maturity_rating, alsTemp);
        gSpinner_AgeRating.setAdapter(atarSpinnerAdapter);

        Button button_AddTag = getView().findViewById(R.id.button_AddTag);
        TextView textView_NewTagTitle = getView().findViewById(R.id.textView_NewTagTitle);
        if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_EDIT_MODE){
            button_AddTag.setText("Apply");
            textView_NewTagTitle.setText("Edit Tag");
        } else {
            button_AddTag.setText("Add Tag");
            textView_NewTagTitle.setText("New Tag");
        }
        button_AddTag.setOnClickListener(this::button_AddTag_Click);

        Button button_Finish = getView().findViewById(R.id.button_Finish);
        button_Finish.setOnClickListener(v -> {
            //Uncheck tags that might be in the viewmodel.
            if(gFragment_selectTags.gListViewTagsAdapter != null) {
                gFragment_selectTags.gListViewTagsAdapter.uncheckAll();
            }
            if(getActivity() != null) {
                ((Activity_TagEditor) getActivity()).callForFinish();
            }
        });

        //Call thing to hide the keyboard when somewhere other than an EditText is touched:
        setupUI(getView().findViewById(R.id.linearLayout_fragment_tag_editor_2_add_tag));

        //Configure the Restrict-To-User elements:
        gRelativeLayout_UserSelection = getView().findViewById(R.id.relativeLayout_UserSelection);
        gLayoutParams_UserSelection = (RelativeLayout.LayoutParams) gRelativeLayout_UserSelection.getLayoutParams();
        gLayoutParams_UserSelection.height = 0;
        gRelativeLayout_UserSelection.setLayoutParams(gLayoutParams_UserSelection);

        gCheckBox_SetApprovedUsers = getView().findViewById(R.id.checkBox_SetApprovedUsers);
        gCheckBox_SetApprovedUsers.setOnClickListener(v -> ToggleRestrictToUserVisibility(gCheckBox_SetApprovedUsers.isChecked()));

        TextView textView_labelRestrictTagToUserIDs = getView().findViewById(R.id.textView_labelRestrictToUsers);
        textView_labelRestrictTagToUserIDs.setOnClickListener(v -> {
            gCheckBox_SetApprovedUsers.setChecked(!gCheckBox_SetApprovedUsers.isChecked());
            ToggleRestrictToUserVisibility(gCheckBox_SetApprovedUsers.isChecked());
        });

        //Initialize the user list:
        //Initialize the displayed list of users:
        RefreshUserPools();
        ListView listView_UserPool = getView().findViewById(R.id.listView_UserPool);
        //End if ListView.onItemClick().
        listView_UserPool.setOnItemClickListener((parent, listView, position, id) -> {
            final ItemClass_User icu = (ItemClass_User) parent.getItemAtPosition(position);
            icu.bIsChecked = !icu.bIsChecked;
            if(getActivity() == null) return;
            if(icu.bIsChecked){
                listView.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
            } else {
                listView.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBackgroundMain));
            }
        }); //End ListView.setOnItemClickListener()

        ListView listView_ApprovedUsers = getView().findViewById(R.id.listView_ApprovedUsers);
        //End if ListView.onItemClick().
        listView_ApprovedUsers.setOnItemClickListener((parent, listView, position, id) -> {
            final ItemClass_User icu = (ItemClass_User) parent.getItemAtPosition(position);
            icu.bIsChecked = !icu.bIsChecked;
            if(getActivity() == null) return;
            if(icu.bIsChecked){
                listView.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
            } else {
                listView.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBackgroundMain));
            }
        }); //End ListView.setOnItemClickListener()

        //Configure the buttons that transfer users from the "user pool" to "selected users" for tag restricted-to selection:
        ImageButton imageButton_AddUser = getView().findViewById(R.id.imageButton_AddUser);
        imageButton_AddUser.setOnClickListener(v -> {
            ArrayList<ItemClass_User> alicu_UsersToAdd = gAdapterUserPool.GetSelectedUsers();
            gAdapterApprovedUsers.AddUsers(alicu_UsersToAdd);
            gAdapterApprovedUsers.uncheckAll();
            gAdapterUserPool.RemoveUsersFromList(alicu_UsersToAdd);
        });

        ImageButton imageButton_RemoveUser = getView().findViewById(R.id.imageButton_RemoveUser);
        imageButton_RemoveUser.setOnClickListener(v -> {
            ArrayList<ItemClass_User> alicu_UsersToMoveBack = gAdapterApprovedUsers.GetSelectedUsers();
            gAdapterUserPool.AddUsers(alicu_UsersToMoveBack);
            gAdapterUserPool.uncheckAll();
            gAdapterApprovedUsers.RemoveUsersFromList(alicu_UsersToMoveBack);
        });

        gProgressBar_Progress = getView().findViewById(R.id.progressBar_Progress);
        gTextView_ProgressBarText = getView().findViewById(R.id.textView_ProgressBarText);



        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
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

        if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_ADD_MODE) {
            gFragment_selectTags.gbOptionViewOnly = true;
            gFragment_selectTags.giSelectionMode = Fragment_SelectTags.NO_SELECT;
        } else {
            //Tag Edit mode:
            gFragment_selectTags.giSelectionMode = Fragment_SelectTags.SINGLE_SELECT;
            //React to changes in the selected tag data in the ViewModel:
            final Observer<ArrayList<ItemClass_Tag>> observerSelectedTags = getNewTagObserver();
            gViewModel_fragment_selectTags.altiTagsSelected.observe(getViewLifecycleOwner(), observerSelectedTags);
        }

        super.onResume();
    }

    @Override
    public void onDestroy() {
        if(getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(tagEditorServiceResponseReceiver);
        }
        super.onDestroy();
    }

    private Observer<ArrayList<ItemClass_Tag>> getNewTagObserver() {
        return new Observer<ArrayList<ItemClass_Tag>>() {
            @Override
            public void onChanged(ArrayList<ItemClass_Tag> tagItems) {

                //In our case, there should only be one tag selected.
                if(tagItems.size() > 0) {
                    gictTagIDInEditMode = tagItems.get(0);
                    gEditText_TagText.setText(gictTagIDInEditMode.sTagText);
                    gEditText_TagDescription.setText(gictTagIDInEditMode.sTagDescription);
                    gSpinner_AgeRating.setSelection(gictTagIDInEditMode.iMaturityRating);
                    giInitialMaturityRating = gictTagIDInEditMode.iMaturityRating;
                    if(gictTagIDInEditMode.alsTagApprovedUsers.size() > 0){
                        gCheckBox_SetApprovedUsers.setChecked(true);
                        ToggleRestrictToUserVisibility(true);
                        ArrayList<ItemClass_User> alicuAllUserPool = new ArrayList<>(GlobalClass.galicu_Users);
                        ArrayList<ItemClass_User> alicuSelectedUsers = new ArrayList<>();
                        ArrayList<ItemClass_User> alicuRemainingUserPool = new ArrayList<>();
                        galsInitialApprovedUsers = new ArrayList<>();

                        for (ItemClass_User icu : alicuAllUserPool) {
                            boolean bUserSelected = false;
                            for(String sUserName: gictTagIDInEditMode.alsTagApprovedUsers) {
                                if(sUserName.equals(icu.sUserName)){
                                    galsInitialApprovedUsers.add(sUserName);
                                    bUserSelected = true;
                                    break;
                                }
                            }
                            if(bUserSelected){
                                alicuSelectedUsers.add(icu);
                            } else {
                                alicuRemainingUserPool.add(icu);
                            }
                        }

                        gAdapterUserPool.clear();
                        gAdapterApprovedUsers.clear();

                        gAdapterUserPool.AddUsers(alicuRemainingUserPool);
                        gAdapterApprovedUsers.AddUsers(alicuSelectedUsers);

                    } else {

                        gCheckBox_SetApprovedUsers.setChecked(false);
                        ToggleRestrictToUserVisibility(false);
                        ArrayList<ItemClass_User> alicuSelectedUsers = new ArrayList<>();
                        ArrayList<ItemClass_User> alicuRemainingUserPool = new ArrayList<>(GlobalClass.galicu_Users);
                        galsInitialApprovedUsers = new ArrayList<>();

                        gAdapterUserPool.clear();
                        gAdapterApprovedUsers.clear();

                        gAdapterUserPool.AddUsers(alicuRemainingUserPool);
                        gAdapterApprovedUsers.AddUsers(alicuSelectedUsers);
                    }
                } else {
                    if(gictTagIDInEditMode != null) { //null ==> the observer will fire when count zero is set to zero again, so accommodate.
                        //If the tag to be edited has been de-selected, recalculate.
                        ClearTagData();
                        gictTagIDInEditMode = null;
                    }
                }

            }
        };
    }

    private void ToggleRestrictToUserVisibility(boolean bNewCheckedState){
        if(bNewCheckedState){
            gLayoutParams_UserSelection.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
        } else {
            gLayoutParams_UserSelection.height = 0;
        }
        gRelativeLayout_UserSelection.setLayoutParams(gLayoutParams_UserSelection);
    }


    public void button_AddTag_Click(View v){

        if(getView() == null){
            return;
        }

        String sTagName = gEditText_TagText.getText().toString();
        if(sTagName.equals("")){
            Toast.makeText(getActivity(), "Tag text cannot be blank.", Toast.LENGTH_SHORT).show();
            return;
        }

        //Gather tag data from the UI:
        ItemClass_Tag ictNewTag;
        if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_EDIT_MODE) {
            if(gictTagIDInEditMode == null){
                Toast.makeText(getActivity(), "Select a tag to modify first from the tag reference list.", Toast.LENGTH_SHORT).show();
                return;
            }
            ictNewTag = gictTagIDInEditMode;
            ictNewTag.sTagText = sTagName;
        } else {
            ictNewTag = new ItemClass_Tag(-1, sTagName);
        }
        //Get the user-entered tag description:
        EditText editText_TagDescription = getView().findViewById(R.id.editText_TagDescription);
        ictNewTag.sTagDescription = editText_TagDescription.getText().toString();
        //Get the selected Age Rating:
        ictNewTag.iMaturityRating = gSpinner_AgeRating.getSelectedItemPosition();
        //Get any users to whom the tag is to be restricted (approved users):
        ictNewTag.alsTagApprovedUsers = gAdapterApprovedUsers.getUserNamesInList();

        //Ensure data has no illegal characters:
        final ItemClass_Tag ict_ValidatedNewTag = GlobalClass.validateTagData(ictNewTag);

        if(ict_ValidatedNewTag == null){
            Toast.makeText(getContext(), "Critical error with formation of data record. Record creation aborted.", Toast.LENGTH_LONG).show();
            return;
        }
        if(ict_ValidatedNewTag.bIllegalDataFound){
            //The illegal data should have been corrected by the validation routine. Notify the user:
            Toast.makeText(getContext(), ict_ValidatedNewTag.sIllegalDataNarrative, Toast.LENGTH_LONG).show();
        }

        //Check and see if the user has added one or more approved users, and if so, if the list
        // of approved users does not include the current user, notify them of the circumstance
        // and ask for confirmation before continuing.
        if(ict_ValidatedNewTag.alsTagApprovedUsers.size() > 0 &&
                !ict_ValidatedNewTag.alsTagApprovedUsers.contains(GlobalClass.gicuCurrentUser.sUserName)){
            String sConfirmationMessage = "You have selected to";
            if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_EDIT_MODE){
                sConfirmationMessage += " edit";
            } else {
                sConfirmationMessage += " add";
            }
            sConfirmationMessage += " a tag and have set a group of approved users which does not" +
                    " contain your username. This will render the tag invisible to you. Any items" +
                    " to which this tag is applied will be hidden from you as well.";
            if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_EDIT_MODE){
                sConfirmationMessage += " You may wish to halt this activity, return to the " +
                        " catalog viewer, and filter by this tag to see what items will be affected.";
            }
            sConfirmationMessage += " Do you want to continue?";

            if (getActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustomStyle);
            builder.setTitle("Tag Does Not Contain Current User");
            builder.setMessage(sConfirmationMessage);
            builder.setPositiveButton("Yes", (dialog, id) -> {
                dialog.dismiss();
                continueWithTagAddOrEditFinalize(ict_ValidatedNewTag);
            });
            builder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());
            AlertDialog adConfirmationDialog = builder.create();
            adConfirmationDialog.show();
        } else {
            continueWithTagAddOrEditFinalize(ict_ValidatedNewTag);
        }

    }

    private void continueWithTagAddOrEditFinalize(ItemClass_Tag ictTagToAddOrEdit){
        //Update data in storage and memory, and notify the user:
        boolean bTagSuccess = false;
        if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_EDIT_MODE) {
            //Attempt to update the record:
            if(globalClass.TagDataFile_UpdateRecord(ictTagToAddOrEdit, gViewModelTagEditor.iTagEditorMediaCategory)){
                gFragment_selectTags.initListViewData();
                Toast.makeText(getActivity(), "Tag modified successfully.", Toast.LENGTH_SHORT).show();
                gictTagIDInEditMode = null;
                gViewModelTagEditor.bTagDataUpdated = true;
                bTagSuccess = true;
            } else {
                Toast.makeText(getActivity(), "Could not alter tag.", Toast.LENGTH_SHORT).show();
            }
        } else {
            //Attempt to add the new record:
            ictTagToAddOrEdit = globalClass.TagDataFile_CreateNewRecord(ictTagToAddOrEdit, gViewModelTagEditor.iTagEditorMediaCategory);
            if (ictTagToAddOrEdit != null) {
                gFragment_selectTags.initListViewData();
                galNewTags.add(ictTagToAddOrEdit);
                gViewModelTagEditor.alNewTags = galNewTags; //To allow new tags to be sent back to a possible calling activity.
                gViewModelTagEditor.bTagAdded = true;
                bTagSuccess = true;
                Toast.makeText(getActivity(), ictTagToAddOrEdit.sTagText + " added successfully.", Toast.LENGTH_SHORT).show();
            }
        }

        if(bTagSuccess){

            if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_EDIT_MODE) {
                //If we are editing a tag, check to see if we need to go and recalculate maturity
                // ratings and/or approved users for catalog items.
                boolean bMaturityRatingChanged = ictTagToAddOrEdit.iMaturityRating != giInitialMaturityRating;
                boolean bApprovedUserChanged = false;

                for(String sOriginalUserEntry: galsInitialApprovedUsers){
                    boolean bUserFound = false;
                    for(String sNewUserEntry: ictTagToAddOrEdit.alsTagApprovedUsers){
                        if(sNewUserEntry.equals(sOriginalUserEntry)){
                            bUserFound = true;
                            break;
                        }
                    }
                    if(!bUserFound){
                        bApprovedUserChanged = true;
                        break;
                    }
                }


                if (bMaturityRatingChanged || bApprovedUserChanged) {
                    if(getActivity() != null){
                        Toast.makeText(getActivity().getApplicationContext(), "Updating catalog records...", Toast.LENGTH_SHORT).show();
                    }
                    if(getContext() == null) return;
                    int iMediaCategoriesToProcessBitSet = 0;
                    int[] iMediaCategoryBits = {1, 2, 4};
                    iMediaCategoriesToProcessBitSet |= iMediaCategoryBits[gViewModelTagEditor.iTagEditorMediaCategory]; //Set a bit to indicate processing of catalog files.
                    if(iMediaCategoriesToProcessBitSet != 0) {
                        //Call a worker to go through this media category data file and recalc the maturity
                        //  rating and assigned users:
                        GlobalClass.gabDataLoaded.set(false); //Don't let the user get into any catalog until processing is complete.
                        Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                        Data dataRecalcCatalogItemsMaturityAndUsers = new Data.Builder()
                                .putString(GlobalClass.EXTRA_CALLER_ID, "Fragment_TagEditor_2_AddTag:continueWithTagAddOrEditFinalize()")
                                .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                                .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY_BIT_SET, iMediaCategoriesToProcessBitSet)
                                .build();
                        OneTimeWorkRequest otwrRecalcCatalogItemsMaturityAndUsers = new OneTimeWorkRequest.Builder(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.class)
                                .setInputData(dataRecalcCatalogItemsMaturityAndUsers)
                                .addTag(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.TAG_WORKER_CATALOG_RECALC_APPROVED_USERS) //To allow finding the worker later.
                                .build();
                        WorkManager.getInstance(getContext()).enqueue(otwrRecalcCatalogItemsMaturityAndUsers);
                    }

                }
            }

            ClearTagData();
        }

    }


    private void ClearTagData(){
        gEditText_TagText.setText("");
        gEditText_TagDescription.setText("");
        gCheckBox_SetApprovedUsers.setChecked(false);
        ToggleRestrictToUserVisibility(false);
        giInitialMaturityRating = -1;
        galsInitialApprovedUsers = new ArrayList<>();
        RefreshUserPools();

        if(gFragment_selectTags.gListViewTagsAdapter != null) {
            gFragment_selectTags.gListViewTagsAdapter.uncheckAll();
        }
        if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_EDIT_MODE) {
            gSpinner_AgeRating.setSelection(0);
        }

    }

    private void RefreshUserPools(){
        if(getActivity() == null) return;
        if(getView() == null) return;
        ListView listView_UserPool = getView().findViewById(R.id.listView_UserPool);
        ArrayList<ItemClass_User> alicuAllUserPool = new ArrayList<>(GlobalClass.galicu_Users);
        gAdapterUserPool = new AdapterUserList(
                getActivity().getApplicationContext(), R.layout.listview_useritem, alicuAllUserPool);
        gAdapterUserPool.gbCompactMode = true;
        int[] iSelectedUnselectedBGColors = {
                ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2),
                ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBackgroundMain)};
        gAdapterUserPool.giSelectedUnselectedBGColors = iSelectedUnselectedBGColors;
        listView_UserPool.setAdapter(gAdapterUserPool);

        ListView listView_ApprovedUsers = getView().findViewById(R.id.listView_ApprovedUsers);
        ArrayList<ItemClass_User> alicu_EmptyUserList = new ArrayList<>();
        gAdapterApprovedUsers = new AdapterUserList(
                getActivity().getApplicationContext(), R.layout.listview_useritem, alicu_EmptyUserList);
        gAdapterApprovedUsers.gbCompactMode = true;
        gAdapterApprovedUsers.giSelectedUnselectedBGColors = iSelectedUnselectedBGColors;
        listView_ApprovedUsers.setAdapter(gAdapterApprovedUsers);
    }


    @SuppressLint("ClickableViewAccessibility") //This is to suppress a warning that the click capture should call v.perform click.
        // But if that is done, it causes an erroneous click to be performed on the Add Tag button in certain circumstances.
    public void setupUI(View view) {
        //https://stackoverflow.com/questions/4165414/how-to-hide-soft-keyboard-on-android-after-clicking-outside-edittext/19828165
        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                if (getActivity() != null) {
                    GlobalClass.hideSoftKeyboard(getActivity());
                }
                return false;
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
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

            boolean bCatalogRecalcComplete = intent.getBooleanExtra(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.EXTRA_CATALOG_RECALC_COMPLETE,false);
            if(bCatalogRecalcComplete){
                GlobalClass.gabDataLoaded.set(true); //Allow the user back into catalog viewers.
                gFragment_selectTags.initListViewData();
                Toast.makeText(context, "Catalog recalc complete.", Toast.LENGTH_SHORT).show();
            }

        }
    }



}