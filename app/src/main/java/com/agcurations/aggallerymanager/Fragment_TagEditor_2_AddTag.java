package com.agcurations.aggallerymanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
            Button button_AddTag = getView().findViewById(R.id.button_AddTag);
            button_AddTag.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    button_AddTag_Click(v);
                }
            });
        }

        if (getView() != null) {
            Button button_Finish = getView().findViewById(R.id.button_Finish);
            button_Finish.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(getActivity() != null) {

                        //Send any new tags back to the calling activity so that
                        // the calling activity is aware of the new tags created by the user.
                        // The calling activity may want to automatically select these
                        // new tags.
                        Intent data = new Intent();
                        Bundle b = new Bundle();
                        b.putSerializable(Activity_TagEditor.NEW_TAGS, galNewTags);
                        data.putExtra(Activity_TagEditor.TAG_EDITOR_NEW_TAGS_RESULT_BUNDLE, b);
                        getActivity().setResult(Activity.RESULT_OK, data);

                        getActivity().finish();
                    }
                }
            });

            setupUI(getView().findViewById(R.id.linearLayout_fragment_tag_editor_2_add_tag));
        }


        RefreshTagListView();
    }

    private void RefreshTagListView(){
        //Populate the listView:
        if(getView() == null){
            return;
        }
        final ListView listView_TagViewer = getView().findViewById(R.id.listView_TagViewer);

        ArrayList<String> alsTags = new ArrayList<>();
        for(Map.Entry<String, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(viewModelTagEditor.iTagEditorMediaCategory).entrySet()){
            alsTags.add(entry.getValue().TagText);
        }

        String[] sTemp = new String[alsTags.size()];
        sTemp = alsTags.toArray(sTemp);
        if(getActivity() == null){
            return;
        }
        ArrayAdapter<String> aasTags = new ArrayAdapter<>(getActivity(), R.layout.listview_tageditor_tagtext, sTemp);
        listView_TagViewer.setAdapter(aasTags);

        EditText etNewTagName = getView().findViewById(R.id.editText_NewTagText);
        etNewTagName.setText("");
    }


    public void button_AddTag_Click(View v){

        if(getView() == null){
            return;
        }
        EditText etNewTagName = getView().findViewById(R.id.editText_NewTagText);

        String sTagName = etNewTagName.getText().toString();

        if(sTagName.equals("")){
            Toast.makeText(getActivity(), "Tag text cannot be blank.", Toast.LENGTH_SHORT).show();
            return;
        }

        ItemClass_Tag ictNewTagItem = globalClass.TagDataFile_CreateNewRecord(sTagName, viewModelTagEditor.iTagEditorMediaCategory);
        if(ictNewTagItem != null){
            RefreshTagListView();
            galNewTags.add(ictNewTagItem);
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