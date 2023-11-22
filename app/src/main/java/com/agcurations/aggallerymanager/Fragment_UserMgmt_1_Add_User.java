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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
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

    private ViewModel_UserManagement gViewModelUserManagement;

    String gsEditUser_OriginalUserName = "";

    public final MutableLiveData<Integer> mldiSelectedUserColor =
            new MutableLiveData<>();
    public Integer giSelectedColor = 0;

    ProgressBar gProgressBar_Progress;
    TextView gTextView_ProgressBarText;

    AddUserResponseReceiver gAddUserResponseReceiver;

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

        if(getContext() == null) return;
        //Configure a response receiver to listen for updates user-delete related workers:
        IntentFilter filter = new IntentFilter();
        filter.addAction(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.WORKER_CATALOG_RECALC_APPROVED_USERS_ACTION_RESPONSE);
        filter.addAction(GlobalClass.BROADCAST_WRITE_CATALOG_FILE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        gAddUserResponseReceiver = new AddUserResponseReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(gAddUserResponseReceiver, filter);

        if(getActivity() == null) return;
        //Instantiate the ViewModel sharing data between fragments:
        gViewModelUserManagement = new ViewModelProvider(getActivity()).get(ViewModel_UserManagement.class);

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

            gProgressBar_Progress = getView().findViewById(R.id.progressBar_Progress);
            gTextView_ProgressBarText = getView().findViewById(R.id.textView_ProgressBarText);

            //Call thing to hide the keyboard when somewhere other than an EditText is touched:
            setupUI(getView().findViewById(R.id.linerLayout_AddUser));

            final Button button_AddUser = getView().findViewById(R.id.button_AddUser);
            button_AddUser.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    button_AddUser_Click(v);
                }
            });
            TextView textView_AddUser = getView().findViewById(R.id.textView_AddUser);
            if(gViewModelUserManagement.iUserAddOrEditMode == ViewModel_UserManagement.USER_EDIT_MODE){
                button_AddUser.setText("Apply");
                textView_AddUser.setText("Edit User");
            } else {
                button_AddUser.setText("Add User");
                textView_AddUser.setText("New User");
            }

            EditText editText_UserName = getView().findViewById(R.id.editText_UserName);
            editText_UserName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    boolean bEnableAddApply = true;
                    String sUserName = "" + s;
                    if(sUserName.equals("")){
                        bEnableAddApply = false;
                    } else {
                        if(gViewModelUserManagement.iUserAddOrEditMode != ViewModel_UserManagement.USER_EDIT_MODE){
                            //If we are not in user-edit mode:
                            //Make sure that the user name does not already exist:
                            for (ItemClass_User icu : GlobalClass.galicu_Users) {
                                if (sUserName.equalsIgnoreCase(icu.sUserName)) {
                                    //If the user name already exists, don't let the
                                    //  user create this new user.
                                    //There is opportunity to allow user name change in edit mode here,
                                    //  but the user name would need to be changed on all of the associated
                                    //  private tags and catalog items as well. Easier to let the user
                                    //  create a new user with the corrected name, share tags with that user,
                                    //  then delete the old user.
                                    bEnableAddApply = false;
                                }
                            }
                        }
                    }

                    button_AddUser.setEnabled(bEnableAddApply);
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            if(gViewModelUserManagement.iUserAddOrEditMode == ViewModel_UserManagement.USER_EDIT_MODE){
                editText_UserName.setEnabled(false);
            } else {
                editText_UserName.setEnabled(true);
                //Set focus on the user name editText and pop open the keyboard:
                editText_UserName.requestFocus();
                if(getActivity() != null) {
                    InputMethodManager inputMethodManager =
                            (InputMethodManager) getActivity().getSystemService(
                                    Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }

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
                                    }
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
            AdapterMaturityRatings amrSpinnerAdapter = new AdapterMaturityRatings(getContext(), R.layout.spinner_item_maturity_rating, alsTemp);
            spinner_ContentMaturity.setAdapter(amrSpinnerAdapter);

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
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(gAddUserResponseReceiver);
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
        int[] iSelectedUnselectedBGColors = {
                ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2),
                ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBackgroundMain)};
        adapterUserList.giSelectedUnselectedBGColors = iSelectedUnselectedBGColors;
        listView_UserList.setAdapter(adapterUserList);
        int iWidthWidestUserItemView = (int)(getWidestView(requireActivity().getApplicationContext(), adapterUserList) * 1.05);

        if(gViewModelUserManagement.iUserAddOrEditMode == ViewModel_UserManagement.USER_EDIT_MODE) {
            //If we are in User Edit Mode, configure a click listener to listen for selection of a
            // user to edit.
            final EditText editText_UserName = getView().findViewById(R.id.editText_UserName);
            final EditText editText_AccessPinNumber = getView().findViewById(R.id.editText_AccessPinNumber);
            final CheckBox checkBox_AdminUser = getView().findViewById(R.id.checkBox_AdminUser);
            final Spinner spinner_ContentMaturity = getView().findViewById(R.id.spinner_ContentMaturity);

            listView_UserList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final ItemClass_User icu = (ItemClass_User) parent.getItemAtPosition(position);
                    icu.bIsChecked = !icu.bIsChecked;
                    if (getActivity() == null) return;
                    if (icu.bIsChecked) {

                        if(!icu.sPin.equals("")){
                            //If this user record requires a pin to log-in, display the pin code popup
                            // before allowing the user to edit this user. Otherwise the admin could
                            // gain access to the user and view their stored contents.

                            //Configure the AlertDialog that will gather the pin code:
                            final AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext(), R.style.AlertDialogCustomStyle);

                            // set the custom layout
                            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                            final View customLayout = inflater.inflate(R.layout.dialog_layout_pin_code, null);
                            builder.setView(customLayout);

                            final AlertDialog adConfirmationDialog = builder.create();

                            //Code action for the Cancel button:
                            Button button_PinCodeCancel = customLayout.findViewById(R.id.button_PinCodeCancel);
                            button_PinCodeCancel.setOnClickListener(view1 -> adConfirmationDialog.dismiss());

                            //Code action for the OK button:
                            Button button_PinCodeOK = customLayout.findViewById(R.id.button_PinCodeOK);

                            //Code action for the OK button:
                            button_PinCodeOK.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {

                                    EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                                    String sPinEntered = editText_DialogInput.getText().toString();

                                    if (sPinEntered.equals(icu.sPin)) {
                                        adapterUserList.uncheckAll(); //If the user is selecting this item, unselect the other items.
                                        icu.bIsChecked = true;
                                        gsEditUser_OriginalUserName = icu.sUserName;
                                        //view.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                                        editText_UserName.setText(icu.sUserName);
                                        editText_AccessPinNumber.setText(icu.sPin);
                                        checkBox_AdminUser.setChecked(icu.bAdmin);
                                        //Set Icon Color:
                                        mldiSelectedUserColor.setValue(icu.iUserIconColor);
                                        //Set maturity selection:
                                        int iAdjustedMaturityLevel = icu.iMaturityLevel;
                                        if(iAdjustedMaturityLevel > 6) {
                                            //User maturity level does not make use of level 6, which is "rating pending"
                                            iAdjustedMaturityLevel -= 1;
                                        }
                                        spinner_ContentMaturity.setSelection(iAdjustedMaturityLevel);
                                    } else {
                                        gsEditUser_OriginalUserName = "";
                                        Toast.makeText(getContext(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                                        adConfirmationDialog.dismiss();
                                    }

                                }
                            });

                            adConfirmationDialog.show();

                        } else {
                            adapterUserList.uncheckAll(); //If the user is selecting this item, unselect the other items.
                            icu.bIsChecked = true;
                                    //If this user record does not require a pin to log-in, merely populate the data in the fields:
                            gsEditUser_OriginalUserName = icu.sUserName;
                            //view.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                            editText_UserName.setText(icu.sUserName);
                            editText_AccessPinNumber.setText(icu.sPin);
                            checkBox_AdminUser.setChecked(icu.bAdmin);
                            //Set Icon Color:
                            mldiSelectedUserColor.setValue(icu.iUserIconColor);
                            //Set maturity selection:
                            int iAdjustedMaturityLevel = icu.iMaturityLevel;
                            if(iAdjustedMaturityLevel > 6) {
                                //User maturity level does not make use of level 6, which is "rating pending"
                                iAdjustedMaturityLevel -= 1;
                            }
                            spinner_ContentMaturity.setSelection(iAdjustedMaturityLevel);


                        }

                    } else {
                        //If the user is de-selecting a user from the user list while in edit-mode, un-highlight and clear data from the form:
                        //view.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBackgroundMain));
                        gsEditUser_OriginalUserName = "";
                        editText_UserName.setText("");
                        editText_AccessPinNumber.setText("");
                        checkBox_AdminUser.setChecked(false);
                        //Initialize Icon Color:
                        final int iAC = initColorIcon();
                        mldiSelectedUserColor.setValue(iAC);
                        //Reset maturity selection:
                        spinner_ContentMaturity.setSelection(0);
                    }
                    adapterUserList.notifyDataSetChanged();
                }
            });
        }


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



    public void button_AddUser_Click(View v){

        if(getView() != null) {

            //Get data:
            EditText editText_UserName = getView().findViewById(R.id.editText_UserName);
            String sUserName = editText_UserName.getText().toString();

            for(String[] sIllegalStringSet: GlobalClass.gsIllegalRecordStrings) {
                if(sUserName.contains(sIllegalStringSet[GlobalClass.CHECKABLE])){
                    String sMessage = "User name contains illegal character '" + sIllegalStringSet[GlobalClass.PRINTABLE] + "'.\n" +
                            "Remove any illegal characters and try again.";
                    Toast.makeText(getContext(), sMessage, Toast.LENGTH_LONG).show();
                    return;
                }
            }

            EditText editText_AccessPinNumber = getView().findViewById(R.id.editText_AccessPinNumber);
            String sPin = editText_AccessPinNumber.getText().toString();

            for(String[] sIllegalStringSet: GlobalClass.gsIllegalRecordStrings) {
                if(sPin.contains(sIllegalStringSet[GlobalClass.CHECKABLE])){
                    String sMessage = "Pin contains illegal character '" + sIllegalStringSet[GlobalClass.PRINTABLE] + "'.\n" +
                            "Remove any illegal characters and try again.";
                    Toast.makeText(getContext(), sMessage, Toast.LENGTH_LONG).show();
                    return;
                }
            }

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

            ItemClass_User icu = new ItemClass_User();
            icu.sUserName = sUserName;
            icu.sPin = sPin;
            icu.bAdmin = bAdmin;
            icu.iUserIconColor = iColorSelection;
            icu.iMaturityLevel = iMaturityLevel;

            //Modify data in memory:
            if(gViewModelUserManagement.iUserAddOrEditMode == ViewModel_UserManagement.USER_EDIT_MODE){
                //If we are editing an existing user, find the existing user for modification and
                // remove the data as part of replacement:
                for(int i = 0; i < GlobalClass.galicu_Users.size(); i++){
                    if(GlobalClass.galicu_Users.get(i).sUserName.equals(gsEditUser_OriginalUserName)){
                        GlobalClass.galicu_Users.remove(i);
                        break;
                    }
                }
            }
            //Add user to memory:
            GlobalClass.galicu_Users.add(icu);


            //Add data to file storage so that it can be picked-up by a new device if the data is moved:
            if(!GlobalClass.WriteUserDataFile()){
                Toast.makeText(getContext(), "Unable to update user data file.", Toast.LENGTH_LONG).show();
            }

            Toast.makeText(requireContext(), "User added successfully.", Toast.LENGTH_SHORT).show();


            //Reset data entries:
            gsEditUser_OriginalUserName = "";
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

            //Recalculate approved users for all items so that the new user is included. Recall that
            // the approved users are pre-calculated to reduce time for catalog display.
            if(getContext() == null) return;
            int iMediaCategoriesToProcessBitSet = 0;
            int[] iMediaCategoryBits = {1, 2, 4};
            for(int iMediaCategory = 0; iMediaCategory <= 2; iMediaCategory++){
                iMediaCategoriesToProcessBitSet |= iMediaCategoryBits[iMediaCategory]; //Set a bit to indicate processing of catalog files.
            }
            if(iMediaCategoriesToProcessBitSet != 0) {
                //Call a worker to go through this media category data file and recalc the maturity
                //  rating and assigned users:
                Double dTimeStamp = GlobalClass.GetTimeStampDouble();
                Data dataRecalcCatalogItemsMaturityAndUsers = new Data.Builder()
                        .putString(GlobalClass.EXTRA_CALLER_ID, "Fragment_UserMgmt_1_Add_User:button_AddUser_Click()")
                        .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                        .putInt(GlobalClass.EXTRA_MEDIA_CATEGORY_BIT_SET, iMediaCategoriesToProcessBitSet)
                        .build();
                OneTimeWorkRequest otwrRecalcCatalogItemsMaturityAndUsers = new OneTimeWorkRequest.Builder(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.class)
                        .setInputData(dataRecalcCatalogItemsMaturityAndUsers)
                        .addTag(Worker_Catalog_RecalcCatalogItemsMaturityAndUsers.TAG_WORKER_CATALOG_RECALC_APPROVED_USERS) //To allow finding the worker later.
                        .build();
                WorkManager.getInstance(getContext()).enqueue(otwrRecalcCatalogItemsMaturityAndUsers);
            }

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