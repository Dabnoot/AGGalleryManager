package com.agcurations.aggallerymanager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.thebluealliance.spectrum.SpectrumDialog;

import java.util.ArrayList;
import java.util.Random;


public class Fragment_UserMgmt_1_Add_User extends Fragment {

    GlobalClass globalClass;

    public final MutableLiveData<Integer> mldiSelectedUserColor =
            new MutableLiveData<>();
    public Integer giSelectedColor = 0;

    ProgressBar gProgressBar_Progress;
    TextView gTextView_ProgressBarText;

    AddUserResponseReceiver addUserResponseReceiver;

    public Fragment_UserMgmt_1_Add_User() {
        // Required empty public constructor
    }

    public static Fragment_UserMgmt_1_Add_User newInstance() {
        return new Fragment_UserMgmt_1_Add_User();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globalClass = (GlobalClass) requireActivity().getApplicationContext();


        if(getView() != null) {
            gProgressBar_Progress = getView().findViewById(R.id.progressBar_Progress);
            gTextView_ProgressBarText = getView().findViewById(R.id.textView_ProgressBarText);
        }

        if(getContext() == null) return;
        //Configure a response receiver to listen for updates user-delete related workers:
        IntentFilter filter = new IntentFilter(Worker_Catalog_RecalcCatalogItemsApprovedUsers.WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        addUserResponseReceiver = new AddUserResponseReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(addUserResponseReceiver, filter);
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
                if(getView() == null){
                    return;
                }
                if(getActivity() == null){
                    return;
                }
                giSelectedColor = iColorSelection;
                AppCompatImageView imageView_UserIcon = getView().findViewById(R.id.imageView_UserIcon);
                Drawable d1 = AppCompatResources.getDrawable(getActivity().getApplicationContext(), R.drawable.login);
                if(d1 == null){
                    return;
                }
                Drawable drawable = d1.mutate();
                drawable.setColorFilter(new PorterDuffColorFilter(giSelectedColor, PorterDuff.Mode.SRC_IN));
                imageView_UserIcon.setImageDrawable(drawable);
            }
        };
        mldiSelectedUserColor.observe(requireActivity(), observerColorSelection);

        if (getView() != null) {

            //Call thing to hide the keyboard when somewhere other than an EditText is touched:
            setupUI(getView().findViewById(R.id.linerLayout_AddUser));

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
            editText_UserName.requestFocus();
            if(getActivity() != null) {
                InputMethodManager inputMethodManager =
                        (InputMethodManager) getActivity().getSystemService(
                                Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }


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
            for(String[] sESRBRating: AdapterMaturityRatings.MATURITY_RATINGS){
                if(!(sESRBRating[AdapterMaturityRatings.MATURITY_RATING_CODE_INDEX].equals(
                        AdapterMaturityRatings.MATURITY_RATINGS[AdapterMaturityRatings.MATURITY_RATING_RP][AdapterMaturityRatings.MATURITY_RATING_CODE_INDEX]))) {
                    //Don't add "rating pending" to the drop-down. Those are for tags only.
                    alsTemp.add(sESRBRating);
                }
            }

            if(getContext() == null) {
                return;
            }
            AdapterMaturityRatings atarSpinnerAdapter = new AdapterMaturityRatings(getContext(), R.layout.spinner_item_maturity_rating, alsTemp);
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

            initUserList();

        }

    }

    @Override
    public void onDestroy() {
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(addUserResponseReceiver);
        }
        super.onDestroy();
    }

    private void initUserList(){
        //Initialize the displayed list of users:
        if(getView() == null){
            return;
        }
        ListView listView_UserList = getView().findViewById(R.id.listView_UserList);
        ArrayList<ItemClass_User> alicuAllUserPool = new ArrayList<>(GlobalClass.galicu_Users);
        AdapterUserList adapterUserList = new AdapterUserList(
                        requireActivity().getApplicationContext(), R.layout.listview_useritem, alicuAllUserPool);
        listView_UserList.setAdapter(adapterUserList);
        int iWidthWidestUserItemView = (int)(getWidestView(requireActivity().getApplicationContext(), adapterUserList) * 1.05);


        WindowMetrics windowMetrics = requireActivity().getWindowManager().getCurrentWindowMetrics();
        int iWidthScreen = windowMetrics.getBounds().width();
        int iMaxWidthToBeAllowed = (int)(iWidthScreen * 0.25); //Only allow the listview to take up a quarter of the screen.
        int iDesiredListViewWidth = Math.min(iMaxWidthToBeAllowed, iWidthWidestUserItemView);

        listView_UserList.getLayoutParams().width = iDesiredListViewWidth;

    }

    /**
     * https://stackoverflow.com/questions/6547154/wrap-content-for-a-listviews-width
     * Computes the widest view in an adapter, best used when you need to wrap_content on a ListView, please be careful
     * and don't use it on an adapter that is extremely numerous in items or it will take a long time.
     *
     * @param context Some context
     * @param adapter The adapter to process
     * @return The pixel width of the widest View
     */
    public static int getWidestView(Context context, Adapter adapter) {
        int maxWidth = 0;
        View view = null;
        FrameLayout fakeParent = new FrameLayout(context);
        for (int i=0, count=adapter.getCount(); i<count; i++) {
            view = adapter.getView(i, view, fakeParent);
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = view.getMeasuredWidth();
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
    }

    private int initColorIcon(){
        int[] iColorsArray = getResources().getIntArray(R.array.colors_user_icon);
        ArrayList<Integer> aliColorOptions = new ArrayList<>();
        for(int iColor: iColorsArray){
            boolean bColorIsTaken = false;
            for(ItemClass_User icuUser: GlobalClass.galicu_Users){
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
        if(getView() == null){
            return;
        }
        EditText editText_UserName = getView().findViewById(R.id.editText_UserName);
        EditText editText_AccessPinNumber = getView().findViewById(R.id.editText_AccessPinNumber);
        String sUserName = editText_UserName.getText().toString();
        String sPin = editText_AccessPinNumber.getText().toString();
        Button button_AddUser = getView().findViewById(R.id.button_AddUser);
        if(button_AddUser != null) {
            button_AddUser.setEnabled(!sUserName.equals(""));
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

            Integer iColorSelection = mldiSelectedUserColor.getValue();
            if(iColorSelection == null){
                return;
            }

            Spinner spinner_ContentMaturity = getView().findViewById(R.id.spinner_ContentMaturity);
            String sMaturityCode;
            int iMaturityLevel = -1;
            View viewSpinnerItemMaturityRating = spinner_ContentMaturity.getSelectedView();
            if(viewSpinnerItemMaturityRating != null){
                TextView tv = viewSpinnerItemMaturityRating.findViewById(R.id.textView_AgeRatingCode);
                if(tv != null){
                    sMaturityCode = tv.getText().toString();
                    //Maturity code lookup
                    for(int i = 0; i < AdapterMaturityRatings.RATINGS_COUNT; i++){
                        String[] sMaturityRecord = AdapterMaturityRatings.MATURITY_RATINGS[i];
                        if(sMaturityCode.equals(sMaturityRecord[AdapterMaturityRatings.MATURITY_RATING_CODE_INDEX])){
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
            for(ItemClass_User icu: GlobalClass.galicu_Users){
                if(sUserName.equalsIgnoreCase(icu.sUserName)){
                    Toast.makeText(requireContext(), "User name already exists. User addition aborted.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            Set<String> ssUserAccountData = sharedPreferences.getStringSet(GlobalClass.gsPreferenceName_UserAccountData, null);
            if(ssUserAccountData == null){
                ssUserAccountData = new HashSet<>();
            } else {
                ssUserAccountData = new HashSet<>(ssUserAccountData); //https://stackoverflow.com/questions/51001328/shared-preferences-not-saving-stringset-when-application-is-killed-its-a-featu
                //It is said that we must not modify the StringSet returned by getStringSet. Consistency is not guaranteed.
            }*/

            ItemClass_User icu = new ItemClass_User();
            icu.sUserName = sUserName;
            icu.sPin = sPin;
            icu.bAdmin = bAdmin;
            icu.iUserIconColor = iColorSelection;
            icu.iMaturityLevel = iMaturityLevel;

            //Add data to memory:
            GlobalClass.galicu_Users.add(icu);

            /*//Add data to preferences:
            String sUserRecord = GlobalClass.getUserAccountRecordString(icu);
            ssUserAccountData.add(sUserRecord);
            sharedPreferences.edit()
                    .putStringSet(GlobalClass.gsPreferenceName_UserAccountData, ssUserAccountData)
                    .apply();*/


            //Add data to file storage so that it can be picked-up by a new device if the data is moved:
            if(!GlobalClass.WriteUserDataFile()){
                Toast.makeText(getContext(), "Unable to update user data file.", Toast.LENGTH_LONG).show();
            }

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

            //Update listview of users:
            initUserList();

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

    public class AddUserResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(GlobalClass.EXTRA_BOOL_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            } else {
                String sStatusMessage = intent.getStringExtra(GlobalClass.EXTRA_STRING_STATUS_MESSAGE);
                if(sStatusMessage != null){
                    Toast.makeText(context, sStatusMessage, Toast.LENGTH_LONG).show();
                }
            }

            //Check to see if this is a response to update log or progress bar:
            boolean 	bUpdatePercentComplete;
            boolean 	bUpdateProgressBarText;

            //Get booleans from the intent telling us what to update:
            bUpdatePercentComplete = intent.getBooleanExtra(GlobalClass.UPDATE_PERCENT_COMPLETE_BOOLEAN,false);
            bUpdateProgressBarText = intent.getBooleanExtra(GlobalClass.UPDATE_PROGRESS_BAR_TEXT_BOOLEAN,false);

            if(gProgressBar_Progress != null && gTextView_ProgressBarText != null) {
                if (bUpdatePercentComplete) {
                    int iAmountComplete;
                    iAmountComplete = intent.getIntExtra(GlobalClass.PERCENT_COMPLETE_INT, -1);
                    if (gProgressBar_Progress != null) {
                        gProgressBar_Progress.setProgress(iAmountComplete);
                    }
                    if (iAmountComplete == 100) {
                        assert gProgressBar_Progress != null;
                        gProgressBar_Progress.setVisibility(View.INVISIBLE);
                        gTextView_ProgressBarText.setVisibility(View.INVISIBLE);
                    } else {
                        assert gProgressBar_Progress != null;
                        gProgressBar_Progress.setVisibility(View.VISIBLE);
                        gTextView_ProgressBarText.setVisibility(View.VISIBLE);
                    }

                }
                if (bUpdateProgressBarText) {
                    String sProgressBarText;
                    sProgressBarText = intent.getStringExtra(GlobalClass.PROGRESS_BAR_TEXT_STRING);
                    if (gTextView_ProgressBarText != null) {
                        gTextView_ProgressBarText.setText(sProgressBarText);
                    }
                }
            }

        }
    }



}