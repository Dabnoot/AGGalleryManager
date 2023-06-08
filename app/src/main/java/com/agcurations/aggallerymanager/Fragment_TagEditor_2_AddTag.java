package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_TagEditor_2_AddTag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_TagEditor_2_AddTag extends Fragment {

    private GlobalClass globalClass;
    private ViewModel_TagEditor gViewModelTagEditor;

    RelativeLayout gRelativeLayout_UserSelection;
    RelativeLayout.LayoutParams gLayoutParams_UserSelection;

    AdapterUserList gAdapterUserPool;
    AdapterUserList gAdapterApprovedUsers;

    int giInitialMaturityRating = -1;
    ArrayList<String> galsInitialApprovedUsers;

    private ArrayList<ItemClass_Tag> galNewTags;

    private EditText gEditText_TagText;
    private EditText gEditText_TagDescription;
    private Spinner gSpinner_AgeRating;
    private CheckBox gCheckBox_SetApprovedUsers;

    ListViewTagsAdapter glistViewTagsAdapter = null;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tag_editor_2_add_tag, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        initComponents();
    }

    public void initComponents() {

        if (getView() == null) {
            return;
        }

        if(getActivity() == null){
            return;
        }



        if (getView() != null) {

            gEditText_TagText = getView().findViewById(R.id.editText_TagText);
            gEditText_TagDescription = getView().findViewById(R.id.editText_TagDescription);

            gSpinner_AgeRating = getView().findViewById(R.id.spinner_ContentMaturity);
            ArrayList<String[]> alsTemp = new ArrayList<>();

            for(int i = 0; i < AdapterMaturityRatings.MATURITY_RATINGS.length; i++){
            //for(String[] sESRBRating: AdapterTagMaturityRatings.TAG_AGE_RATINGS){
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
            button_AddTag.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    button_AddTag_Click(v);
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

            //Call thing to hide the keyboard when somewhere other than an EditText is touched:
            setupUI(getView().findViewById(R.id.linearLayout_fragment_tag_editor_2_add_tag));

            //Configure the Restrict-To-User elements:
            gRelativeLayout_UserSelection = getView().findViewById(R.id.relativeLayout_UserSelection);
            gLayoutParams_UserSelection = (RelativeLayout.LayoutParams) gRelativeLayout_UserSelection.getLayoutParams();
            gLayoutParams_UserSelection.height = 0;
            gRelativeLayout_UserSelection.setLayoutParams(gLayoutParams_UserSelection);

            gCheckBox_SetApprovedUsers = getView().findViewById(R.id.checkBox_SetApprovedUsers);
            gCheckBox_SetApprovedUsers.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ToggleRestrictToUserVisibility(gCheckBox_SetApprovedUsers.isChecked());
                }
            });

            TextView textView_labelRestrictTagToUserIDs = getView().findViewById(R.id.textView_labelRestrictToUsers);
            textView_labelRestrictTagToUserIDs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gCheckBox_SetApprovedUsers.setChecked(!gCheckBox_SetApprovedUsers.isChecked());
                    ToggleRestrictToUserVisibility(gCheckBox_SetApprovedUsers.isChecked());
                }
            });

            //Initialize the user list:
            //Initialize the displayed list of users:
            RefreshUserPools();
            ListView listView_UserPool = getView().findViewById(R.id.listView_UserPool);
            listView_UserPool.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final ItemClass_User icu = (ItemClass_User) parent.getItemAtPosition(position);
                    icu.bIsChecked = !icu.bIsChecked;
                    if(getActivity() == null) return;
                    if(icu.bIsChecked){
                        view.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                    } else {
                        view.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBackgroundMain));
                    }
                } //End if ListView.onItemClick().
            }); //End ListView.setOnItemClickListener()

            ListView listView_ApprovedUsers = getView().findViewById(R.id.listView_ApprovedUsers);
            listView_ApprovedUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final ItemClass_User icu = (ItemClass_User) parent.getItemAtPosition(position);
                    icu.bIsChecked = !icu.bIsChecked;
                    if(getActivity() == null) return;
                    if(icu.bIsChecked){
                        view.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                    } else {
                        view.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBackgroundMain));
                    }
                } //End if ListView.onItemClick().
            }); //End ListView.setOnItemClickListener()

            //Configure the buttons that transfer users from the "user pool" to "selected users" for tag restricted-to selection:
            ImageButton imageButton_AddUser = getView().findViewById(R.id.imageButton_AddUser);
            imageButton_AddUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<ItemClass_User> alicu_UsersToAdd = gAdapterUserPool.GetSelectedUsers();
                    gAdapterApprovedUsers.AddUsers(alicu_UsersToAdd);
                    gAdapterApprovedUsers.uncheckAll();
                    gAdapterUserPool.RemoveUsersFromList(alicu_UsersToAdd);
                }
            });

            ImageButton imageButton_RemoveUser = getView().findViewById(R.id.imageButton_RemoveUser);
            imageButton_RemoveUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<ItemClass_User> alicu_UsersToMoveBack = gAdapterApprovedUsers.GetSelectedUsers();
                    gAdapterUserPool.AddUsers(alicu_UsersToMoveBack);
                    gAdapterUserPool.uncheckAll();
                    gAdapterApprovedUsers.RemoveUsersFromList(alicu_UsersToMoveBack);
                }
            });



        }

        RefreshTagListView();
    }

    private void ToggleRestrictToUserVisibility(boolean bNewCheckedState){
        if(bNewCheckedState){
            gLayoutParams_UserSelection.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
        } else {
            gLayoutParams_UserSelection.height = 0;
        }
        gRelativeLayout_UserSelection.setLayoutParams(gLayoutParams_UserSelection);
    }

    private void RefreshTagListView(){
        //Populate the listView:
        if(getView() == null){
            return;
        }

        TreeMap<String, ItemClass_Tag> tmTags = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<Integer, ItemClass_Tag> entry : GlobalClass.gtmApprovedCatalogTagReferenceLists.get(gViewModelTagEditor.iTagEditorMediaCategory).entrySet()) {
            String sTagTextForSort = entry.getValue().sTagText + entry.getValue().iTagID;
            tmTags.put(sTagTextForSort, entry.getValue());
        }

        ArrayList<ItemClass_Tag> alict_TagsListTags = new ArrayList<>();

        for(Map.Entry<String, ItemClass_Tag> entry : tmTags.entrySet()){
            alict_TagsListTags.add(entry.getValue());
        }

        if(getActivity() != null) {
            glistViewTagsAdapter = new ListViewTagsAdapter(getActivity().getApplicationContext(), alict_TagsListTags);
            ListView listView_TagViewer = getView().findViewById(R.id.listView_TagViewer);
            listView_TagViewer.setAdapter(glistViewTagsAdapter);
            listView_TagViewer.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

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

        //Check and see if the user has added one or more approved users, and if so, if the list
        // of approved users does not include the current user, notify them of the circumstance
        // and ask for confirmation before continuing.
        if(ictNewTag.alsTagApprovedUsers.size() > 0 &&
                !ictNewTag.alsTagApprovedUsers.contains(GlobalClass.gicuCurrentUser.sUserName)){
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
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    continueWithTagAddOrEditFinalize(ictNewTag);
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog adConfirmationDialog = builder.create();
            adConfirmationDialog.show();
        } else {
            continueWithTagAddOrEditFinalize(ictNewTag);
        }

    }

    private void continueWithTagAddOrEditFinalize(ItemClass_Tag ictTagToAddOrEdit){
        //Update data in storage and memory, and notify the user:
        boolean bTagSuccess = false;
        if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_EDIT_MODE) {
            //Attempt to update the record:
            if(globalClass.TagDataFile_UpdateRecord(ictTagToAddOrEdit, gViewModelTagEditor.iTagEditorMediaCategory)){
                RefreshTagListView();
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
                RefreshTagListView();
                galNewTags.add(ictTagToAddOrEdit);
                gViewModelTagEditor.alNewTags = galNewTags; //To allow new tags to be sent back to a possible calling activity.
                gViewModelTagEditor.bTagAdded = true;
                bTagSuccess = true;
                Toast.makeText(getActivity(), ictTagToAddOrEdit.sTagText + " added successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), ictTagToAddOrEdit.sTagText + " already exists in tag list.", Toast.LENGTH_SHORT).show();
            }
        }

        if(bTagSuccess){

            if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_EDIT_MODE) {
                //If we are editing a tag, check to see if we need to go and recalculate maturity
                // ratings and/or approved users for catalog items.
                boolean bMaturityRatingChanged = ictTagToAddOrEdit.iMaturityRating != giInitialMaturityRating;
                boolean bApprovedUserChanged = false;
                if(galsInitialApprovedUsers.size() != ictTagToAddOrEdit.alsTagApprovedUsers.size()){
                    bApprovedUserChanged = true;
                }
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

                if(getActivity() != null){
                    Toast.makeText(getActivity().getApplicationContext(), "Updating catalog records...", Toast.LENGTH_SHORT).show();
                }

                if (bMaturityRatingChanged || bApprovedUserChanged) {
                    if(getContext() == null) return;
                    int iMediaCategoriesToProcessBitSet = 0;
                    int[] iMediaCategoryBits = {1, 2, 4};
                    iMediaCategoriesToProcessBitSet |= iMediaCategoryBits[gViewModelTagEditor.iTagEditorMediaCategory]; //Set a bit to indicate processing of catalog files.
                    if(iMediaCategoriesToProcessBitSet != 0) {
                        //Call a worker to go through this media category data file and recalc the maturity
                        //  rating and assigned users:
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
        //Clear any selected tag in the tag listview:
        if (glistViewTagsAdapter != null) {
            glistViewTagsAdapter.unselectAllTags();
        }
    }

    private void RefreshUserPools(){
        if(getActivity() == null) return;
        if(getView() == null) return;
        ListView listView_UserPool = getView().findViewById(R.id.listView_UserPool);
        ArrayList<ItemClass_User> alicuAllUserPool = new ArrayList<>(globalClass.galicu_Users);
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





    public class ListViewTagsAdapter extends ArrayAdapter<ItemClass_Tag> {

        ArrayList<ItemClass_Tag> alictTagItems; //Contains all tag items passed to the listviewTagsAdapter.

        public ListViewTagsAdapter(Context context, ArrayList<ItemClass_Tag> tagItems) {
            super(context, 0, tagItems);
            alictTagItems = tagItems;
        }

        @NonNull
        @Override
        public View getView(int position, View v, @NonNull ViewGroup parent) {
            // Get the data item for this position

            final ItemClass_Tag tagItem = alictTagItems.get(position);

            if(tagItem == null){
                return v;
            }
            // Check if an existing view is being reused, otherwise inflate the view
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(R.layout.listview_tag_item_select_tags_fragment, parent, false);
            }
            // Lookup view for data population
            final CheckedTextView checkedTextView_TagText = v.findViewById(R.id.checkedTextView_TagText);
            checkedTextView_TagText.setText(tagItem.sTagText);

            if(gViewModelTagEditor.iTagAddOrEditMode == ViewModel_TagEditor.TAG_EDIT_MODE) {
                //Only allow the user to select items if we are in edit mode.

                //Set the selection state (needed as views are recycled).
                if(getActivity() != null) {
                    if (tagItem.bIsChecked) {
                        checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                    } else {
                        checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorUnfilledUnselected));
                    }
                    checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
                }

                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Handle changing the checked state:
                        tagItem.bIsChecked = !tagItem.bIsChecked;
                        boolean bUpdateOtherItemsViews = false;
                        if (tagItem.bIsChecked) {
                            checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                            checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
                            gictTagIDInEditMode = tagItem;
                            gEditText_TagText.setText(tagItem.sTagText);
                            gEditText_TagDescription.setText(tagItem.sTagDescription);
                            gSpinner_AgeRating.setSelection(tagItem.iMaturityRating);
                            giInitialMaturityRating = tagItem.iMaturityRating;
                            if(tagItem.alsTagApprovedUsers.size() > 0){
                                gCheckBox_SetApprovedUsers.setChecked(true);
                                ToggleRestrictToUserVisibility(true);
                                ArrayList<ItemClass_User> alicuAllUserPool = new ArrayList<>(globalClass.galicu_Users);
                                ArrayList<ItemClass_User> alicuSelectedUsers = new ArrayList<>();
                                ArrayList<ItemClass_User> alicuRemainingUserPool = new ArrayList<>();
                                galsInitialApprovedUsers = new ArrayList<>();

                                for (ItemClass_User icu : alicuAllUserPool) {
                                    boolean bUserSelected = false;
                                    for(String sUserName: tagItem.alsTagApprovedUsers) {
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
                                ArrayList<ItemClass_User> alicuRemainingUserPool = new ArrayList<>(globalClass.galicu_Users);
                                galsInitialApprovedUsers = new ArrayList<>();

                                gAdapterUserPool.clear();
                                gAdapterApprovedUsers.clear();

                                gAdapterUserPool.AddUsers(alicuRemainingUserPool);
                                gAdapterApprovedUsers.AddUsers(alicuSelectedUsers);
                            }
                            //Go through and uncheck anything but this tag:
                            for (ItemClass_Tag ict : alictTagItems) {
                                if (ict.bIsChecked && !ict.iTagID.equals(tagItem.iTagID)) {
                                    ict.bIsChecked = false;
                                    bUpdateOtherItemsViews = true;
                                }
                            }
                        } else {
                            checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorUnfilledUnselected));
                            checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
                            ClearTagData();
                        }
                        if (bUpdateOtherItemsViews) {
                            notifyDataSetChanged();
                        }

                    }
                });
            }

            // Return the completed view to render on screen
            return v;
        }

        public void unselectAllTags(){
            for(ItemClass_Tag ict: alictTagItems){
                ict.bIsChecked = false;
            }
            notifyDataSetChanged();
        }


        @Override
        public int getCount() {
            return super.getCount();
        }
    }



    public void setupUI(View view) {
        //https://stackoverflow.com/questions/4165414/how-to-hide-soft-keyboard-on-android-after-clicking-outside-edittext/19828165
        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if(getActivity() != null) {
                        GlobalClass.hideSoftKeyboard(getActivity());
                    }
                    return false;
                }
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








}