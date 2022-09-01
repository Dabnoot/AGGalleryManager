package com.agcurations.aggallerymanager;

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

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.thebluealliance.spectrum.SpectrumDialog;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;


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

            //Initialize Icon Color:
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
            final int iAC = iAvailableColor;
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





            final Spinner spinner_AgeRating = getView().findViewById(R.id.spinner_AgeRating);
            ArrayList<String[]> alsTemp = new ArrayList<>();
            for(String[] sESRBRating: adapterTagAgeRatings.TAG_AGE_RATINGS){
                if(!(sESRBRating[adapterTagAgeRatings.TAG_AGE_RATING_CODE_INDEX].equals(
                        adapterTagAgeRatings.TAG_AGE_RATINGS[adapterTagAgeRatings.TAG_AGE_RATING_RP][adapterTagAgeRatings.TAG_AGE_RATING_CODE_INDEX])
                || sESRBRating[adapterTagAgeRatings.TAG_AGE_RATING_CODE_INDEX].equals(
                        adapterTagAgeRatings.TAG_AGE_RATINGS[adapterTagAgeRatings.TAG_AGE_RATING_UR][adapterTagAgeRatings.TAG_AGE_RATING_CODE_INDEX]))) {
                    //Don't add "rating pending" or "user restricted" ratings to the drop-down. Those are for tags only.
                    alsTemp.add(sESRBRating);
                }
            }
            adapterTagAgeRatings atarSpinnerAdapter = new adapterTagAgeRatings(getContext(), R.layout.spinner_item_age_rating, alsTemp);
            spinner_AgeRating.setAdapter(atarSpinnerAdapter);

            spinner_AgeRating.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(!gbSpinnerDropdownWidthSet) {
                        int iSpinnerWidthPixels = spinner_AgeRating.getWidth();
                        if (iSpinnerWidthPixels > 0) {
                            spinner_AgeRating.setDropDownWidth(iSpinnerWidthPixels);
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

    public void button_AddUser_Click(View v){

    }



}