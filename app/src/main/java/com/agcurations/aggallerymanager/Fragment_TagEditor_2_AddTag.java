package com.agcurations.aggallerymanager;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_TagEditor_2_AddTag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_TagEditor_2_AddTag extends Fragment {

    private GlobalClass globalClass;
    private ViewModel_TagEditor viewModelTagEditor;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public Fragment_TagEditor_2_AddTag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Fragment_TagEditor_2_AddTag.
     */
    // TODO: Rename and change types and number of parameters
    public static Fragment_TagEditor_2_AddTag newInstance(String param1, String param2) {
        Fragment_TagEditor_2_AddTag fragment = new Fragment_TagEditor_2_AddTag();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getActivity().getApplicationContext();
        //Instantiate the ViewModel sharing data between fragments:
        viewModelTagEditor = new ViewModelProvider(this).get(ViewModel_TagEditor.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tag_editor_2_add_tag, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        Button button_AddTag = getView().findViewById(R.id.button_AddTag);
        button_AddTag.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                button_AddTag_Click(v);
            }
        });

        Button button_Finish = getView().findViewById(R.id.button_Finish);
        button_Finish.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        setupUI(getView().findViewById(R.id.linearLayout_fragment_tag_editor_2_add_tag));

        RefreshTagListView();
    }

    private void RefreshTagListView(){
        //Populate the listView:
        final ListView listView_TagViewer = getView().findViewById(R.id.listView_TagViewer);

        ArrayList<String> alsTags = new ArrayList<>();
        for(Map.Entry<String, String[]> entry : globalClass.gtmCatalogTagReferenceLists.get(viewModelTagEditor.iTagEditorMediaCategory).entrySet()){
            alsTags.add(entry.getValue()[GlobalClass.TAG_NAME_INDEX]);
        }

        String[] sTemp = new String[alsTags.size()];
        sTemp = alsTags.toArray(sTemp);
        ArrayAdapter<String> aasTags = new ArrayAdapter<>(getActivity(), R.layout.listview_tagtext, sTemp);
        listView_TagViewer.setAdapter(aasTags);

        EditText etNewTagName = getView().findViewById(R.id.editText_NewTagText);
        etNewTagName.setText("");
    }


    public void button_AddTag_Click(View v){

        EditText etNewTagName = getView().findViewById(R.id.editText_NewTagText);

        String sTagName = etNewTagName.getText().toString();

        if(globalClass.TagDataFile_CreateNewRecord(sTagName, viewModelTagEditor.iTagEditorMediaCategory)){
            RefreshTagListView();
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
                    GlobalClass.hideSoftKeyboard(getActivity());
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