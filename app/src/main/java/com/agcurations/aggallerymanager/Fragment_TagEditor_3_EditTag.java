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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_TagEditor_3_EditTag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_TagEditor_3_EditTag extends Fragment {

    private GlobalClass globalClass;
    private ViewModel_TagEditor viewModelTagEditor;

    private TextView gTextView_TagID;
    private EditText gEditText_TagText;
    private EditText gEditText_TagDescription;

    public Fragment_TagEditor_3_EditTag() {
        // Required empty public constructor
    }

    public static Fragment_TagEditor_3_EditTag newInstance() {
        return new Fragment_TagEditor_3_EditTag();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tag_editor_3_edit_tag, container, false);
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

        if(getView() != null) {
            Button button_Apply = getView().findViewById(R.id.button_Apply);
            button_Apply.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    button_Apply_Click(v);
                }
            });
        }

        if(getView() != null) {
            Button button_Finish = getView().findViewById(R.id.button_Finish);
            button_Finish.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(getActivity() != null) {
                        ((Activity_TagEditor) getActivity()).callForFinish();
                    }
                }
            });
        }

        setupUI(getView().findViewById(R.id.linearLayout_fragment_tag_editor_2_add_tag));

        RefreshTagListView();
    }

    private void RefreshTagListView(){
        //Populate the listView:
        if (getView() == null) {
            return;
        }

        gTextView_TagID = getView().findViewById(R.id.textView_TagID);
        gEditText_TagText = getView().findViewById(R.id.editText_TagText);
        gEditText_TagDescription = getView().findViewById(R.id.editText_TagDescription);
        gTextView_TagID.setText("");
        gEditText_TagText.setText("");
        gEditText_TagDescription.setText("");

        TreeMap<String, ItemClass_Tag> tmTags = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        boolean bIsTagToBeRestricted = false;
        for (Map.Entry<Integer, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(viewModelTagEditor.iTagEditorMediaCategory).entrySet()) {
            //if (entry.getValue().bIsRestricted) {
            if(globalClass.gicuCurrentUser != null) {
                if (globalClass.gicuCurrentUser.iMaturityLevel < entry.getValue().iTagAgeRating) {
                    bIsTagToBeRestricted = true;
                }
            } else {
                //If no user is selected or current user is somehow null, follow guidelines for
                //  default user maturity rating.
                if (entry.getValue().iTagAgeRating <= globalClass.giDefaultUserMaturityRating) {
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
            listView_TagViewer.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }

    }


    public void button_Apply_Click(View v){

        if(getView() == null){
            return;
        }
        EditText editText_TagText = getView().findViewById(R.id.editText_UserName);
        String sNewTagName = editText_TagText.getText().toString();

        if(sNewTagName.equals("")){
            Toast.makeText(getActivity(), "Tag text cannot be blank.", Toast.LENGTH_SHORT).show();
            return;
        }

        for(Map.Entry<Integer, ItemClass_Tag> entry: globalClass.gtmCatalogTagReferenceLists.get(viewModelTagEditor.iTagEditorMediaCategory).entrySet()){
            if(entry.getValue().sTagText.equals(sNewTagName)){
                Toast.makeText(getActivity(), "Cannot rename - tag already exists.", Toast.LENGTH_SHORT).show();
                return;
            }
        }


        TextView textView_TagID = getView().findViewById(R.id.textView_TagID);
        String sTagID = textView_TagID.getText().toString();

        if(globalClass.TagDataFile_UpdateRecord(sTagID, sNewTagName, viewModelTagEditor.iTagEditorMediaCategory)){
            RefreshTagListView();
            Toast.makeText(getActivity(), "Tag ID " + sTagID + " modified successfully.", Toast.LENGTH_SHORT).show();
            viewModelTagEditor.bTagRenamed = true;
        } else {
            Toast.makeText(getActivity(), "Could not alter tag.", Toast.LENGTH_SHORT).show();
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
                    if(tagItem.bIsChecked){
                        checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                        checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
                        String sTagID = "" + tagItem.iTagID;
                        gTextView_TagID.setText(sTagID);
                        gEditText_TagText.setText(tagItem.sTagText);
                        gEditText_TagDescription.setText(tagItem.sTagDescription);
                        //Go through and uncheck anything but this tag:
                        for(ItemClass_Tag ict: alictTagItems){
                            if(ict.bIsChecked && !ict.iTagID.equals(tagItem.iTagID)){
                                ict.bIsChecked = false;
                                bUpdateOtherItemsViews = true;
                            }
                        }
                    } else {
                        checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorUnfilledUnselected));
                        checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorTextColor));
                    }
                    if(bUpdateOtherItemsViews){
                        notifyDataSetChanged();
                    }


                }
            });

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
        if (!(view instanceof EditText) && view != null) {
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