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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_TagEditor_3_EditTag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_TagEditor_3_EditTag extends Fragment {

    private GlobalClass globalClass;
    private ViewModel_TagEditor viewModelTagEditor;

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

                        //Send back data to the caller that a tag may have been renamed.
                        //  If the user reached this stage while viewing a catalog item, the cat
                        //  item may need to have tags reloaded.
                        Intent data = new Intent();
                        data.putExtra(Activity_TagEditor.EXTRA_BOOL_REQUEST_RELOAD_OPEN_CATALOG_ITEM_TAGS, true);
                        getActivity().setResult(Activity.RESULT_OK, data);

                        getActivity().finish();

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
        final ListView listView_TagViewer = getView().findViewById(R.id.listView_TagViewer);

        ArrayList<String> alsTags = new ArrayList<>();
        for(Map.Entry<String, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(viewModelTagEditor.iTagEditorMediaCategory).entrySet()){
            String sItemText = entry.getValue().sTagText + " (ID:" + entry.getValue().iTagID + ")";
            alsTags.add(sItemText);
        }

        String[] sTemp = new String[alsTags.size()];
        sTemp = alsTags.toArray(sTemp);
        if (getActivity() == null) {
            return;
        }
        ArrayAdapter<String> aasTags = new ArrayAdapter<>(getActivity(), R.layout.listview_tageditor_tagtext, sTemp);
        listView_TagViewer.setAdapter(aasTags);

        final EditText editText_TagText = getView().findViewById(R.id.editText_TagText);
        final TextView textView_TagID = getView().findViewById(R.id.textView_TagID);
        editText_TagText.setText("");
        textView_TagID.setText("");

        listView_TagViewer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String sItemText = (String)adapterView.getItemAtPosition(position);
                String[] sFields = sItemText.split(" \\(ID:");
                String sTagName = sFields[0];
                String sTagID = sFields[1].substring(0, sFields[1].length()-1);
                textView_TagID.setText(sTagID);
                editText_TagText.setText(sTagName);
                Button button_Apply = getView().findViewById(R.id.button_Apply);
                if(button_Apply != null){
                    button_Apply.setEnabled(true);
                }
            }
        });
    }


    public void button_Apply_Click(View v){

        if(getView() == null){
            return;
        }
        EditText editText_TagText = getView().findViewById(R.id.editText_TagText);
        String sNewTagName = editText_TagText.getText().toString();

        if(sNewTagName.equals("")){
            Toast.makeText(getActivity(), "Tag text cannot be blank.", Toast.LENGTH_SHORT).show();
            return;
        }

        if(globalClass.gtmCatalogTagReferenceLists.get(viewModelTagEditor.iTagEditorMediaCategory).containsKey(sNewTagName)){
            Toast.makeText(getActivity(), "Cannot rename - tag already exists.", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView textView_TagID = getView().findViewById(R.id.textView_TagID);
        String sTagID = textView_TagID.getText().toString();

        if(globalClass.TagDataFile_UpdateRecord(sTagID, sNewTagName, viewModelTagEditor.iTagEditorMediaCategory)){
            RefreshTagListView();
            Toast.makeText(getActivity(), "Tag ID " + sTagID + " modified successfully.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Could not alter tag.", Toast.LENGTH_SHORT).show();
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