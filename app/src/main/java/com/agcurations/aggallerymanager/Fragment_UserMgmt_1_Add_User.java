package com.agcurations.aggallerymanager;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.thebluealliance.spectrum.SpectrumDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class Fragment_UserMgmt_1_Add_User extends Fragment {

    public final MutableLiveData<Integer> mldiSelectedUserColor =
            new MutableLiveData<>();
    public Integer giSelectedColor = 0;

    public Fragment_UserMgmt_1_Add_User() {
        // Required empty public constructor
    }

    public static Fragment_UserMgmt_1_Add_User newInstance(String param1, String param2) {
        return new Fragment_UserMgmt_1_Add_User();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_mgmt_1_add_user, container, false);
    }

    private boolean gbSpinnerDropdownWidthSet = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Observer<Integer> observerColorSelection = new Observer<Integer>() {
            @Override
            public void onChanged(Integer iColorSelection) {
                giSelectedColor = iColorSelection;
                AppCompatImageView imageView_UserIcon = getView().findViewById(R.id.imageView_UserIcon);
                Drawable drawable = AppCompatResources.getDrawable(getActivity().getApplicationContext(), R.drawable.login).mutate();
                drawable.setColorFilter(new PorterDuffColorFilter(giSelectedColor, PorterDuff.Mode.SRC_IN));
                imageView_UserIcon.setImageDrawable(drawable);
            }
        };
        mldiSelectedUserColor.observe(requireActivity(), observerColorSelection);

        if (getView() != null) {

            EditText editText_UserName = getView().findViewById(R.id.editText_UserName);
            editText_UserName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateData(); //Look to see if ok to enable the Add User button.
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            EditText editText_AccessPinNumber = getView().findViewById(R.id.editText_AccessPinNumber);
            editText_AccessPinNumber.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateData(); //Look to see if ok to enable the Add User button.
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });


            //Initialize Icon Color:
            final int iAC = initColorIcon();
            mldiSelectedUserColor.setValue(iAC);

            AppCompatImageView imageView_UserIcon = getView().findViewById(R.id.imageView_UserIcon);
            imageView_UserIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new SpectrumDialog.Builder(getContext(), R.style.AlertDialogCustomStyle)
                            .setColors(R.array.colors_user_icon)
                            //.setSelectedColorRes(R.color.md_blue_500)
                            .setSelectedColor(iAC)

                            .setDismissOnColorSelected(true)
                            .setOutlineWidth(2)
                            .setOnColorSelectedListener(new SpectrumDialog.OnColorSelectedListener() {
                                @Override public void onColorSelected(boolean positiveResult, @ColorInt int color) {
                                    if (positiveResult) {
                                        //Toast.makeText(getContext(), "Color selected: #" + Integer.toHexString(color).toUpperCase(), Toast.LENGTH_SHORT).show();
                                        mldiSelectedUserColor.setValue(color);
                                    } /*else {
                                        Toast.makeText(getContext(), "Dialog cancelled", Toast.LENGTH_SHORT).show();
                                    }*/
                                }
                            }).build().show(getParentFragmentManager(), "dialog_demo_1");
                }
            });





            final Spinner spinner_ContentMaturity = getView().findViewById(R.id.spinner_ContentMaturity);
            ArrayList<String[]> alsTemp = new ArrayList<>();
            for(String[] sESRBRating: adapterTagMaturityRatings.TAG_AGE_RATINGS){
                if(!(sESRBRating[adapterTagMaturityRatings.TAG_AGE_RATING_CODE_INDEX].equals(
                        adapterTagMaturityRatings.TAG_AGE_RATINGS[adapterTagMaturityRatings.TAG_AGE_RATING_RP][adapterTagMaturityRatings.TAG_AGE_RATING_CODE_INDEX])
                || sESRBRating[adapterTagMaturityRatings.TAG_AGE_RATING_CODE_INDEX].equals(
                        adapterTagMaturityRatings.TAG_AGE_RATINGS[adapterTagMaturityRatings.TAG_AGE_RATING_UR][adapterTagMaturityRatings.TAG_AGE_RATING_CODE_INDEX]))) {
                    //Don't add "rating pending" or "user restricted" ratings to the drop-down. Those are for tags only.
                    alsTemp.add(sESRBRating);
                }
            }
            adapterTagMaturityRatings atarSpinnerAdapter = new adapterTagMaturityRatings(getContext(), R.layout.spinner_item_maturity_rating, alsTemp);
            spinner_ContentMaturity.setAdapter(atarSpinnerAdapter);

            spinner_ContentMaturity.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(!gbSpinnerDropdownWidthSet) {
                        int iSpinnerWidthPixels = spinner_ContentMaturity.getWidth();
                        if (iSpinnerWidthPixels > 0) {
                            spinner_ContentMaturity.setDropDownWidth(iSpinnerWidthPixels);
                            gbSpinnerDropdownWidthSet = true;
                        }
                    }
                    return false;
                }
            });

            Button button_AddUser = getView().findViewById(R.id.button_AddUser);
            button_AddUser.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    button_AddUser_Click(v);
                }
            });

            Button button_Finish = getView().findViewById(R.id.button_Finish);
            button_Finish.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(getActivity() != null) {
                        ((Activity_UserManagement) getActivity()).buttonClick_Cancel(v);
                    }
                }
            });

        }

    }

    private int initColorIcon(){
        int[] iColorsArray = getResources().getIntArray(R.array.colors_user_icon);
        ArrayList<Integer> aliColorOptions = new ArrayList<>();
        GlobalClass globalClass = (GlobalClass) requireActivity().getApplicationContext();
        for(int iColor: iColorsArray){
            boolean bColorIsTaken = false;
            for(ItemClass_User icuUser: globalClass.galicu_Users){
                if(icuUser.iUserIconColor == iColor){
                    bColorIsTaken = true;
                    break;
                }
            }
            if(!bColorIsTaken){
                aliColorOptions.add(iColor);
            }
        }
        int iAvailableColor = R.color.md_blue_500;
        if(aliColorOptions.size() > 0){
            int iRandom = new Random().nextInt(aliColorOptions.size());
            iAvailableColor = aliColorOptions.get(iRandom);
        }
        return iAvailableColor;
    }

    private void validateData(){
        EditText editText_UserName = getView().findViewById(R.id.editText_UserName);
        EditText editText_AccessPinNumber = getView().findViewById(R.id.editText_AccessPinNumber);
        String sUserName = editText_UserName.getText().toString();
        String sPin = editText_AccessPinNumber.getText().toString();
        Button button_AddUser = getView().findViewById(R.id.button_AddUser);
        if(button_AddUser != null) {
            if (sUserName != null && sPin != null) {
                if (!sUserName.equals("") && !sPin.equals("")) {
                    button_AddUser.setEnabled(true);
                } else {
                    button_AddUser.setEnabled(false);
                }
            } else {
                button_AddUser.setEnabled(false);
            }
        }


    }

    public void button_AddUser_Click(View v){

        if(getView() != null) {
            //Validate data:
            EditText editText_UserName = getView().findViewById(R.id.editText_UserName);
            String sUserName = editText_UserName.getText().toString();

            EditText editText_AccessPinNumber = getView().findViewById(R.id.editText_AccessPinNumber);
            String sPin = editText_AccessPinNumber.getText().toString();

            CheckBox checkBox_AdminUser = getView().findViewById(R.id.checkBox_AdminUser);
            boolean bAdmin = checkBox_AdminUser.isChecked();

            int iColorSelection = mldiSelectedUserColor.getValue();

            Spinner spinner_ContentMaturity = getView().findViewById(R.id.spinner_ContentMaturity);
            String sMaturityCode = "";
            int iMaturityLevel = -1;
            View viewSpinnerItemMaturityRating = spinner_ContentMaturity.getSelectedView();
            if(viewSpinnerItemMaturityRating != null){
                TextView tv = viewSpinnerItemMaturityRating.findViewById(R.id.textView_AgeRatingCode);
                if(tv != null){
                    sMaturityCode = tv.getText().toString();
                    //Maturity code lookup
                    for(int i = 0; i < adapterTagMaturityRatings.RATINGS_COUNT; i++){
                        String[] sMaturityRecord = adapterTagMaturityRatings.TAG_AGE_RATINGS[i];
                        if(sMaturityCode.equals(sMaturityRecord[adapterTagMaturityRatings.TAG_AGE_RATING_CODE_INDEX])){
                            iMaturityLevel = i;
                            break;
                        }
                    }
                }
            }
            if(iMaturityLevel == -1){
                Toast.makeText(requireContext(), "Unable to parse maturity rating. User addition aborted.", Toast.LENGTH_SHORT).show();
                return;
            }

            //Make sure that the user name does not already exist:
            GlobalClass globalClass = (GlobalClass) requireActivity().getApplicationContext();
            for(ItemClass_User icu: globalClass.galicu_Users){
                if(sUserName.toLowerCase().equals(icu.sUserName.toLowerCase())){
                    Toast.makeText(requireContext(), "User name already exists. User addition aborted.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            Set<String> ssUserAccountData = sharedPreferences.getStringSet(GlobalClass.gsPreferenceName_UserAccountData, null);
            if(ssUserAccountData == null){
                ssUserAccountData = new HashSet<>();
            }

            ItemClass_User icu = new ItemClass_User();
            icu.sUserName = sUserName;
            icu.iPin = Integer.parseInt(sPin);
            icu.bAdmin = bAdmin;
            icu.iUserIconColor = iColorSelection;
            icu.iMaturityLevel = iMaturityLevel;

            //Add data to memory:
            globalClass.galicu_Users.add(icu);

            //Add data to preferences:
            String sUserRecord = GlobalClass.getUserAccountRecordString(icu);
            ssUserAccountData.add(sUserRecord);
            SharedPreferences.Editor SPE = sharedPreferences.edit();
            SPE.putStringSet(GlobalClass.gsPreferenceName_UserAccountData, ssUserAccountData);
            SPE.apply();

            //Add data to file storage so that it can be picked-up by a new device if the data is moved:
            //todo.

            Toast.makeText(requireContext(), "User added successfully.", Toast.LENGTH_SHORT).show();

            //Reset text entries:
            editText_UserName.setText("");
            editText_AccessPinNumber.setText("");
            //Reset admin selection:
            checkBox_AdminUser.setChecked(false);
            //Initialize Icon Color:
            final int iAC = initColorIcon();
            mldiSelectedUserColor.setValue(iAC);
            //Reset maturity selection:
            spinner_ContentMaturity.setSelection(0);

            //Todo: update listview of users.

        }
    }



}