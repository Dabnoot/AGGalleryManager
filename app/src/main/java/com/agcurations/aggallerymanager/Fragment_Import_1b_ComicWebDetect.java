package com.agcurations.aggallerymanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;

public class Fragment_Import_1b_ComicWebDetect extends Fragment {

    ViewModel_ImportActivity viewModelImportActivity;

    public Fragment_Import_1b_ComicWebDetect() {
        // Required empty public constructor
    }

    public static Fragment_Import_1b_ComicWebDetect newInstance() {
        return new Fragment_Import_1b_ComicWebDetect();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);

            //globalClass = (GlobalClass) getActivity().getApplicationContext();
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

        if(getActivity() == null || getView() == null) {
            return;
        }

        getActivity().setTitle("Import");
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();

        Button button_PasteAddress = getView().findViewById(R.id.button_PasteAddress);
        if(button_PasteAddress != null){

            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = clipboard.getPrimaryClip();
            if(clipData != null) {
                ClipData.Item clipData_Item = clipData.getItemAt(0);
                if (clipData_Item != null) {
                    String sClipString = clipData_Item.getText().toString();
                    button_PasteAddress.setEnabled(sClipString != null);
                }
            } else {
                button_PasteAddress.setEnabled(false);
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
                    boolean bAddressOK = false;
                    String sAddressCandidate = String.valueOf(charSequence);
                    if(sAddressCandidate.length() > 0) {
                        //Evaluate if the address matches a pattern:
                        String sNHRegexExpression = "https:\\/\\/nhentai.net\\/g\\/\\d{1,6}\\/?$";
                        ArrayList<String> alsComicSiteRegexExpressions = new ArrayList<>();
                        alsComicSiteRegexExpressions.add(sNHRegexExpression);

                        for(String sRegex: alsComicSiteRegexExpressions){
                            if(sAddressCandidate.matches(sRegex)){
                                bAddressOK = true;
                                break;
                            }
                        }
                    }

                    if(bAddressOK) {
                        viewModelImportActivity.sWebAddress = sAddressCandidate;
                    } else {
                        viewModelImportActivity.sWebAddress = "";
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });


            //Check to see if we got here because the user wants to import something that they found on the internal
            // browser:
            if (getContext() != null) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = clipboard.getPrimaryClip();
                if (clipData != null) {
                    String sClipLabel = clipData.getDescription().getLabel().toString();
                    if (sClipLabel != null){
                        if(sClipLabel.equals(Service_WebPageTabs.IMPORT_REQUEST_FROM_INTERNAL_BROWSER)){
                            ClipData.Item clipItem = clipData.getItemAt(0);
                            if(clipItem != null){
                                if(clipItem.getText() != null){
                                    String sWebAddress = clipItem.coerceToHtmlText(getActivity().getApplicationContext());
                                    if( sWebAddress != null){
                                        editText_WebAddress.setText(sWebAddress);
                                        clipboard.clearPrimaryClip();
                                    }
                                }
                            }

                        }
                    }
                }
            }


        }

    }
}