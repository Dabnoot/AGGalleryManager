package com.agcurations.aggallerymanager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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

    AdapterUserList gAdapterApprovedUsers;

    private ArrayList<ItemClass_Tag> galNewTags;

    private EditText gEditText_TagText;
    private EditText gEditText_TagDescription;
    private Spinner gSpinner_AgeRating;
    private CheckBox gCheckBox_SetApprovedUsers;

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

            for(int i = 0; i < AdapterTagMaturityRatings.TAG_AGE_RATINGS.length; i++){
            //for(String[] sESRBRating: AdapterTagMaturityRatings.TAG_AGE_RATINGS){
                if(globalClass.gicuCurrentUser.iMaturityLevel >= i) {
                    //Don't let the user add a tag or modify a tag to a maturity level greater than their user level, or the tag will be lost
                    //  to them unless their user rating is modified.
                    String[] sESRBRating = AdapterTagMaturityRatings.TAG_AGE_RATINGS[i];
                    alsTemp.add(sESRBRating);
                }
            }
            AdapterTagMaturityRatings atarSpinnerAdapter = new AdapterTagMaturityRatings(getContext(), R.layout.spinner_item_maturity_rating, alsTemp);
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
                    ToggleRestrictToUserVisibility();
                }
            });

            TextView textView_labelRestrictTagToUserIDs = getView().findViewById(R.id.textView_labelRestrictToUsers);
            textView_labelRestrictTagToUserIDs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gCheckBox_SetApprovedUsers.setChecked(!gCheckBox_SetApprovedUsers.isChecked());
                    ToggleRestrictToUserVisibility();
                }
            });

            //Initialize the user list:
            //Initialize the displayed list of users:
            ListView listView_UserPool = getView().findViewById(R.id.listView_UserPool);
            AdapterUserList adapterUserList = new AdapterUserList(
                    getActivity().getApplicationContext(), R.layout.listview_useritem, globalClass.galicu_Users);
            adapterUserList.gbCompactMode = true;
            int[] iSelectedUnselectedBGColors = {
                    ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2),
                    ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBackgroundMain)};
            adapterUserList.giSelectedUnselectedBGColors = iSelectedUnselectedBGColors;
            listView_UserPool.setAdapter(adapterUserList);
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
            ArrayList<ItemClass_User> alicu_EmptyUserList = new ArrayList<>();
            gAdapterApprovedUsers = new AdapterUserList(
                    getActivity().getApplicationContext(), R.layout.listview_useritem, alicu_EmptyUserList);
            gAdapterApprovedUsers.gbCompactMode = true;
            gAdapterApprovedUsers.giSelectedUnselectedBGColors = iSelectedUnselectedBGColors;
            listView_ApprovedUsers.setAdapter(gAdapterApprovedUsers);
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
                    ArrayList<ItemClass_User> alicu_UsersToAdd = adapterUserList.GetSelectedUsers();
                    gAdapterApprovedUsers.AddUsers(alicu_UsersToAdd);
                    adapterUserList.RemoveUsersFromList(alicu_UsersToAdd);
                }
            });

            ImageButton imageButton_RemoveUser = getView().findViewById(R.id.imageButton_RemoveUser);
            imageButton_RemoveUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<ItemClass_User> alicu_UsersToMoveBack = gAdapterApprovedUsers.GetSelectedUsers();
                    adapterUserList.AddUsers(alicu_UsersToMoveBack);
                    gAdapterApprovedUsers.RemoveUsersFromList(alicu_UsersToMoveBack);
                }
            });



        }

        RefreshTagListView();
    }

    private void ToggleRestrictToUserVisibility(){
        if(getView() == null) return;
        boolean bCheckedState = gCheckBox_SetApprovedUsers.isChecked();
        if(bCheckedState){
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
        boolean bIsTagToBeRestricted = false;
        for (Map.Entry<Integer, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(gViewModelTagEditor.iTagEditorMediaCategory).entrySet()) {
            //if (entry.getValue().bIsRestricted) {
            if(globalClass.gicuCurrentUser != null) {
                if (globalClass.gicuCurrentUser.iMaturityLevel < entry.getValue().iMaturityRating) {
                    bIsTagToBeRestricted = true;
                }
            } else {
                //If no user is selected or current user is somehow null, follow guidelines for
                //  default user maturity rating.
                if (entry.getValue().iMaturityRating <= globalClass.giDefaultUserMaturityRating) {
                    bIsTagToBeRestricted = true;
                }
            }
            if(!bIsTagToBeRestricted){
                if(entry.getValue().alsTagApprovedUsers != null){
                    if(entry.getValue().alsTagApprovedUsers.size() > 0){
                        bIsTagToBeRestricted = true;
                        for(String sApprovedUser: entry.getValue().alsTagApprovedUsers){
                            if (globalClass.gicuCurrentUser.sUserName.equals(sApprovedUser)){
                                bIsTagToBeRestricted = false;
                                break;
                            }
                        }
                    }
                }
            }
            if (!bIsTagToBeRestricted) {
                String sTagTextForSort = entry.getValue().sTagText + entry.getValue().iTagID;
                tmTags.put(sTagTextForSort, entry.getValue());
            }

        }

        ArrayList<ItemClass_Tag> alict_TagsListTags = new ArrayList<>();

        for(Map.Entry<String, ItemClass_Tag> entry : tmTags.entrySet()){
            alict_TagsListTags.add(entry.getValue());
        }

        if(getActivity() != null) {
            ListViewTagsAdapter listViewTagsAdapter = new ListViewTagsAdapter(getActivity().getApplicationContext(), alict_TagsListTags);
            ListView listView_TagViewer = getView().findViewById(R.id.listView_TagViewer);
            listView_TagViewer.setAdapter(listViewTagsAdapter);
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

        //Update data in storage and memory, and notify the user:
        boolean bTagSuccess = false;
        if(gViewModelTagEditor.iTagAddOrEditMode == gViewModelTagEditor.TAG_EDIT_MODE) {
            //Attempt to update the record:
            if(globalClass.TagDataFile_UpdateRecord(ictNewTag, gViewModelTagEditor.iTagEditorMediaCategory)){
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
            ictNewTag = globalClass.TagDataFile_CreateNewRecord(ictNewTag, gViewModelTagEditor.iTagEditorMediaCategory);
            if (ictNewTag != null) {
                RefreshTagListView();
                galNewTags.add(ictNewTag);
                gViewModelTagEditor.alNewTags = galNewTags; //To allow new tags to be sent back to a possible calling activity.
                gViewModelTagEditor.bTagAdded = true;
                bTagSuccess = true;
                Toast.makeText(getActivity(), sTagName + " added successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), sTagName + " already exists in tag list.", Toast.LENGTH_SHORT).show();
            }
        }

        if(bTagSuccess){
            gEditText_TagText.setText("");
            gEditText_TagDescription.setText("");
            gCheckBox_SetApprovedUsers.setChecked(false);
        }
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

            if(gViewModelTagEditor.iTagAddOrEditMode == gViewModelTagEditor.TAG_EDIT_MODE) {
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