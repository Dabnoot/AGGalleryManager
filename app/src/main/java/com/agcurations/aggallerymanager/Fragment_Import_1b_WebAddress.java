package com.agcurations.aggallerymanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class Fragment_Import_1b_WebAddress extends Fragment {

    ViewModel_ImportActivity viewModelImportActivity;


    public Fragment_Import_1b_WebAddress() {
        // Required empty public constructor
    }

    public static Fragment_Import_1b_WebAddress newInstance() {
        return new Fragment_Import_1b_WebAddress();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() != null) {
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_1b_comic_web_address, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Import");

        Button button_PasteAddress = getView().findViewById(R.id.button_PasteAddress);
        if(button_PasteAddress != null){

            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = clipboard.getPrimaryClip();
            ClipData.Item clipData_Item = clipData.getItemAt(0);
            if(clipData_Item != null){
                String sClipString = clipData_Item.getText().toString();
                if(sClipString != null){
                    button_PasteAddress.setEnabled(true);
                } else {
                    button_PasteAddress.setEnabled(false);
                }
            }


            button_PasteAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = clipboard.getPrimaryClip();
                    ClipData.Item clipData_Item = clipData.getItemAt(0);
                    if(clipData_Item != null){
                        String sClipString = clipData_Item.getText().toString();
                        if(sClipString != null){
                            EditText editText_WebAddress = getView().findViewById(R.id.editText_WebAddress);
                            if(editText_WebAddress != null){
                                editText_WebAddress.setText(sClipString);
                            }
                        }
                    }
                }
            });

        }

    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText editText_WebAddress = getView().findViewById(R.id.editText_WebAddress);
        if(editText_WebAddress != null){

            editText_WebAddress.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    viewModelImportActivity.sWebAddress = String.valueOf(charSequence);
                    Button button_NextStep = getView().findViewById(R.id.button_NextStep);
                    if(button_NextStep != null){
                        button_NextStep.setEnabled(viewModelImportActivity.sWebAddress.length() > 0);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });


        }

    }
}