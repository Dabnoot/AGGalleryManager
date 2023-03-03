package com.agcurations.aggallerymanager;

import android.os.Bundle;

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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_TagEditor_2_AddTag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_TagEditor_2_AddTag extends Fragment {

    private GlobalClass globalClass;
    private ViewModel_TagEditor viewModelTagEditor;

    RelativeLayout gRelativeLayout_UserSelection;
    RelativeLayout.LayoutParams gLayoutParams_UserSelection;

    AdapterUserList gAdapterApprovedUsers;

    private ArrayList<ItemClass_Tag> galNewTags;

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
        viewModelTagEditor = new ViewModelProvider(getActivity()).get(ViewModel_TagEditor.class);

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

            Spinner spinner_AgeRating = getView().findViewById(R.id.spinner_ContentMaturity);
            ArrayList<String[]> alsTemp = new ArrayList<>();
            for(String[] sESRBRating: AdapterTagMaturityRatings.TAG_AGE_RATINGS){
                alsTemp.add(sESRBRating);
            }
            AdapterTagMaturityRatings atarSpinnerAdapter = new AdapterTagMaturityRatings(getContext(), R.layout.spinner_item_maturity_rating, alsTemp);
            spinner_AgeRating.setAdapter(atarSpinnerAdapter);

            Button button_AddTag = getView().findViewById(R.id.button_AddTag);
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

            final CheckBox checkBox_SetApprovedUsers = getView().findViewById(R.id.checkBox_SetApprovedUsers);
            checkBox_SetApprovedUsers.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ToggleRestrictToUserVisibility();
                }
            });

            TextView textView_labelRestrictTagToUserIDs = getView().findViewById(R.id.textView_labelRestrictToUsers);
            textView_labelRestrictTagToUserIDs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox_SetApprovedUsers.setChecked(!checkBox_SetApprovedUsers.isChecked());
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
        final CheckBox checkBox_RestrictTagToUserIDs = getView().findViewById(R.id.checkBox_SetApprovedUsers);
        boolean bCheckedState = checkBox_RestrictTagToUserIDs.isChecked();
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
        final ListView listView_TagViewer = getView().findViewById(R.id.listView_TagViewer);

        ArrayList<String> alsTags = new ArrayList<>();
        for(Map.Entry<Integer, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(viewModelTagEditor.iTagEditorMediaCategory).entrySet()){
            alsTags.add(entry.getValue().sTagText);
        }

        String[] sTemp = new String[alsTags.size()];
        sTemp = alsTags.toArray(sTemp);
        if(getActivity() == null){
            return;
        }
        ArrayAdapter<String> aasTags = new ArrayAdapter<>(getActivity(), R.layout.listview_tageditor_tagtext, sTemp);
        listView_TagViewer.setAdapter(aasTags);

        EditText editText_TagText = getView().findViewById(R.id.editText_TagText);
        editText_TagText.setText("");
    }


    public void button_AddTag_Click(View v){

        if(getView() == null){
            return;
        }

        EditText editText_TagText = getView().findViewById(R.id.editText_TagText);
        String sTagName = editText_TagText.getText().toString();
        if(sTagName.equals("")){
            Toast.makeText(getActivity(), "Tag text cannot be blank.", Toast.LENGTH_SHORT).show();
            return;
        }
        ItemClass_Tag ictNewTag = new ItemClass_Tag(-1, sTagName);

        //Get the user-entered tag description:
        EditText editText_TagDescription = getView().findViewById(R.id.editText_TagDescription);
        ictNewTag.sTagDescription = editText_TagDescription.getText().toString();

        //Get the selected Age Rating:
        Spinner spinner_AgeRating = getView().findViewById(R.id.spinner_ContentMaturity);
        ictNewTag.iTagAgeRating = spinner_AgeRating.getSelectedItemPosition();

        //Get any users to whom the tag is to be restricted (approved users):
        ictNewTag.alsTagApprovedUsers = gAdapterApprovedUsers.getUserNamesInList();

        //Attempt to add the new record:
        ictNewTag = globalClass.TagDataFile_CreateNewRecord(ictNewTag, viewModelTagEditor.iTagEditorMediaCategory);
        if(ictNewTag != null){
            RefreshTagListView();
            galNewTags.add(ictNewTag);
            viewModelTagEditor.alNewTags = galNewTags; //To allow new tags to be sent back to a possible calling activity.
            viewModelTagEditor.bTagAdded = true;
            Toast.makeText(getActivity(), sTagName + " added successfully.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), sTagName + " already exists in tag list.", Toast.LENGTH_SHORT).show();
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