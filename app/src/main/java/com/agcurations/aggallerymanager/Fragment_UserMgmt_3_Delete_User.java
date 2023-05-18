package com.agcurations.aggallerymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;


public class Fragment_UserMgmt_3_Delete_User extends Fragment {

    AdapterUserList gAdapterUserList;

    Button gButton_DeleteUser;

    ProgressBar gProgressBar_Progress;
    TextView gTextView_ProgressBarText;

    UserDeleteResponseReceiver userDeleteResponseReceiver;

    public Fragment_UserMgmt_3_Delete_User() {
        // Required empty public constructor
    }

    public static Fragment_UserMgmt_3_Delete_User newInstance() {
        return new Fragment_UserMgmt_3_Delete_User();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if(getContext() == null) return;

        if(getView() != null) {
            gProgressBar_Progress = getView().findViewById(R.id.progressBar_Progress);
            gTextView_ProgressBarText = getView().findViewById(R.id.textView_ProgressBarText);
        }

        //Configure a response receiver to listen for updates user-delete related workers:
        IntentFilter filter = new IntentFilter(Worker_User_Delete.USER_DELETE_ACTION_RESPONSE);
        filter.addAction(Worker_Catalog_DeleteMultipleItems.DELETE_MULTIPLE_ITEMS_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        userDeleteResponseReceiver = new UserDeleteResponseReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(userDeleteResponseReceiver, filter);

    }

    @Override
    public void onDestroy() {
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(userDeleteResponseReceiver);
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_mgmt_3_delete_user, container, false);
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

        RefreshUserListView();

        if(getView() != null) {
            gButton_DeleteUser = getView().findViewById(R.id.button_DeleteUser);
            gButton_DeleteUser.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    button_DeleteUser_Click(v);
                }
            });
        }

        if(getView() != null) {
            Button button_Finish = getView().findViewById(R.id.button_Finish);
            button_Finish.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(getActivity() == null){
                        return;
                    }
                    getActivity().finish();
                }
            });
        }
    }

    private void RefreshUserListView(){
        if(getContext() == null){
            return;
        }
        if (getView() == null) {
            return;
        }

        //Populate the listView:
        ArrayList<ItemClass_User> alicuAllUserPool = new ArrayList<>(GlobalClass.galicu_Users);
        gAdapterUserList = new AdapterUserList(
                getContext(), R.layout.listview_useritem, alicuAllUserPool);
        //adapterUserList.gbSimplifiedView = true;  //Hides the maturity rating and admin status
        final ListView listView_UserDelete = getView().findViewById(R.id.listView_UserDelete);
        int[] iSelectedUnselectedBGColors = {
                ContextCompat.getColor(getContext(), R.color.colorFragmentBackgroundHighlight2),
                ContextCompat.getColor(getContext(), R.color.colorBackgroundMain)};
        gAdapterUserList.giSelectedUnselectedBGColors = iSelectedUnselectedBGColors;
        listView_UserDelete.setAdapter(gAdapterUserList);
        //listView_UserDelete.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView_UserDelete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ItemClass_User icu = (ItemClass_User) parent.getItemAtPosition(position);
                if(!icu.bIsChecked){
                    gAdapterUserList.uncheckAll();
                }
                icu.bIsChecked = !icu.bIsChecked;
                if(getActivity() == null) return;
                if(icu.bIsChecked){
                    gButton_DeleteUser.setEnabled(true);
                    view.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentBackgroundHighlight2));
                } else {
                    gButton_DeleteUser.setEnabled(false);
                    view.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBackgroundMain));
                }
            } //End if ListView.onItemClick().

        }); //End ListView.setOnItemClickListener()

        if(getView() != null) {
            Button button_DeleteUser = getView().findViewById(R.id.button_DeleteUser);
            button_DeleteUser.setEnabled(false);
        }



    }

    private void button_DeleteUser_Click(View v){
        if (getView() == null) {
            return;
        }

        ItemClass_User icuSelectedUser = gAdapterUserList.GetSelectedUsers().get(0);

        if(icuSelectedUser.bAdmin){
            int iAdminCount = 0;
            for(ItemClass_User icu: GlobalClass.galicu_Users){
                if(icu.bAdmin) iAdminCount++;
            }
            if(iAdminCount == 1){
                Toast.makeText(getContext(), "There must always be 1 admin account. Specify another account to be admin before deleting this account.", Toast.LENGTH_LONG).show();
                return;
            }

        }



        String sConfirmationMessage = "This action will delete the User from the database. A check" +
                " will be performed to verify that there are no catalog items or tags currently held" +
                " privately by this user. If there are catalog items or tags held privately by ONLY this user," +
                " a prompt will be given to approve deletion of those items.\n" +
                "Confirm user deletion: ";
        sConfirmationMessage = sConfirmationMessage + Objects.requireNonNull(icuSelectedUser.sUserName);

        if (getActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustomStyle);
        builder.setTitle("Delete User");
        builder.setMessage(sConfirmationMessage);
        //builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                DeleteUser_CheckItems();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog adConfirmationDialog = builder.create();
        adConfirmationDialog.show();
        //adConfirmationDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.YELLOW);
    }

    private void DeleteUser_CheckItems(){
        Toast.makeText(getContext(), "Examining catalog...", Toast.LENGTH_SHORT).show();
        final ItemClass_User icu_UserToDelete = gAdapterUserList.GetSelectedUsers().get(0);
        String sUserNameToBeRemoved = icu_UserToDelete.sUserName;

        //First check to see if there are catalog items private to only the selected user. If so,
        // inform the user that these catalog items will be deleted. Request permission to delete
        // these items and indicate that it may take time to do so. Request permission by asking
        // the user to enter a displayed code.
        boolean bPrivateCatalogItemsFound = false;
        boolean bPrivateTagItemsFound = false;
        for(int iMediaCategory = 0; iMediaCategory <= 2; iMediaCategory++){
            for(Map.Entry<String, ItemClass_CatalogItem> entryCatalogItem: GlobalClass.gtmCatalogLists.get(iMediaCategory).entrySet()){
                ItemClass_CatalogItem icci = entryCatalogItem.getValue();
                if(icci.alsApprovedUsers.size() == 1){
                    if(icci.alsApprovedUsers.get(0).equals(sUserNameToBeRemoved)){
                        bPrivateCatalogItemsFound = true;
                        bPrivateTagItemsFound = true; //Since private attribute upon a catalog item
                        // is inhereted from the tag, if there is a private catalog item, then there
                        // must also be at least one tag private to the user.
                    }
                }
            }
            if(bPrivateCatalogItemsFound){
                break;
            }
        }

        //Second, check for private tags. If there are private tags, inform the user that these tags
        // will be deleted. Request permission to delete these items by asking the user to enter a
        // displayed code.
        if(!bPrivateTagItemsFound){
            for(int iMediaCategory = 0; iMediaCategory <= 2; iMediaCategory++){
                for(Map.Entry<Integer, ItemClass_Tag> tagEntry: GlobalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
                    ItemClass_Tag ict = tagEntry.getValue();
                    if(ict.alsTagApprovedUsers.size() == 1) {
                        if (ict.alsTagApprovedUsers.get(0).equals(sUserNameToBeRemoved)) {
                            bPrivateTagItemsFound = true;
                            break;
                        }
                    }
                }
                if(bPrivateTagItemsFound){
                    break;
                }
            }
        }

        //If there are catalog items or tag items private to only the selected user, inform the user
        // that these catalog will be deleted. Request permission to delete these items and indicate
        // that it may take time to do so. Request permission by asking the user to enter a
        // displayed code.
        if(bPrivateCatalogItemsFound || bPrivateTagItemsFound){
            final boolean lbPrivateCatalogItemsFound = bPrivateCatalogItemsFound;
            final boolean lbPrivateTagItemsFound = bPrivateTagItemsFound;
            //Ask before proceeding with delete.
            if(getContext() == null){
                return;
            }
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext(), R.style.AlertDialogCustomStyle);

            // set the custom layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            final View customLayout = inflater.inflate(R.layout.dialog_layout_confirm_code, null);
            alertDialogBuilder.setView(customLayout);

            final AlertDialog adConfirmationDialog = alertDialogBuilder.create();

            //Code action for the Cancel button:
            Button button_ConfirmationCancel = customLayout.findViewById(R.id.button_ConfirmationCancel);
            button_ConfirmationCancel.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    adConfirmationDialog.dismiss();
                }
            });

            //Code action for the OK button:
            Button button_ConfirmCode = customLayout.findViewById(R.id.button_ConfirmCode);

            //Code action for the OK button:
            button_ConfirmCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    EditText editText_ConfirmationCode = customLayout.findViewById(R.id.editText_ConfirmationCode);
                    String sCodeEntered = editText_ConfirmationCode.getText().toString();

                    if (sCodeEntered.equals("2468")) {
                        adConfirmationDialog.dismiss();
                        //todo: Start a worker to execute the user delete operation to avoid blocking the UI thread.
                        //todo: Set the data load boolean to false.
                        ExecuteUserDeleteOperations(icu_UserToDelete, lbPrivateCatalogItemsFound, lbPrivateTagItemsFound);
                    } else {
                        Toast.makeText(getContext(), "Incorrect code entered. Delete aborted.", Toast.LENGTH_SHORT).show();
                        adConfirmationDialog.dismiss();
                    }

                }
            });

            adConfirmationDialog.show();

        } else {
            //If there are no catalog items and no tags to be deleted, just delete the user record:
            //todo: Start a worker to execute the user delete operation to avoid blocking the UI thread.
            //todo: Set the data load boolean to false.
            ExecuteUserDeleteOperations(icu_UserToDelete, false, false);
        }

    }


    private void ExecuteUserDeleteOperations(ItemClass_User icu_UserToDelete,
                                             boolean bPrivateCatalogItemsFound,
                                             boolean bPrivateTagsFound){

        //Either the user-to-be-deleted has no private tags (and thus no private catalog items) or
        // the user performing the deletion has approved deletion of that user's private data.

        //First, mark the user as "to be deleted" in the user data file and memory so that a
        // long-running file deletion process can be resumed if the program or worker unexpectedly
        // terminates before user-deletion is completed.
        for(ItemClass_User icu: GlobalClass.galicu_Users){
            if(icu.sUserName.equals(icu_UserToDelete.sUserName)){
                icu.bToBeDeleted = true;
                break;
            }
        }
        GlobalClass.WriteUserDataFile();

        RefreshUserListView();

        if(bPrivateCatalogItemsFound){
            //If a search of the catalog determined that the user had private catalog items, build
            // and start the file deletion worker...

            //Create a data file containing the catalog records that are to be deleted:
            StringBuilder sbBuffer = new StringBuilder();
            for(int iMediaCategory = 0; iMediaCategory <= 2; iMediaCategory++){
                for(Map.Entry<String, ItemClass_CatalogItem> entryCatalogItem: GlobalClass.gtmCatalogLists.get(iMediaCategory).entrySet()){
                    ItemClass_CatalogItem icci = entryCatalogItem.getValue();
                    if(icci.alsApprovedUsers.size() == 1){
                        if(icci.alsApprovedUsers.get(0).equals(icu_UserToDelete.sUserName)){
                            sbBuffer.append(GlobalClass.getCatalogRecordString(icci)); //Append the data.
                            sbBuffer.append("\n");
                        }
                    }
                }
            }
            try {
                //Write the catalog file:
                String sDateTimeStamp = GlobalClass.GetTimeStampFileSafe();
                String sFileName = "UserDeletionJobFile_" + sDateTimeStamp + ".dat";
                String sDataFileUriString = GlobalClass.gUriJobFilesFolder + GlobalClass.gsFileSeparator + sFileName;

                Uri uriDataFile = Uri.parse(sDataFileUriString);
                OutputStream osUserDeletionJobFile;

                osUserDeletionJobFile = GlobalClass.gcrContentResolver.openOutputStream(uriDataFile, "w"); //Mode w = write. See https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)
                if(osUserDeletionJobFile == null){
                    throw new Exception();
                }
                osUserDeletionJobFile.write(sbBuffer.toString().getBytes());
                osUserDeletionJobFile.flush();
                osUserDeletionJobFile.close();

            } catch (Exception e) {
                Toast.makeText(getContext(), "Problem writing file containing deletion job details.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                //todo: determine behavior at this point.
                return;
            }

            //Deletion job file should now be done.
            //Start deletion worker:
            if(getContext() == null) return;
            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            Data dataCatalogContentDeletion = new Data.Builder()
                    .putString(GlobalClass.EXTRA_CALLER_ID, "Fragment_UserMgmt_3_Delete_User:ExecuteUserDeleteOperations()")
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                    .putString(GlobalClass.EXTRA_STRING_USERNAME, icu_UserToDelete.sUserName)
                    .build();
            OneTimeWorkRequest otwrCatalogDeleteContent = new OneTimeWorkRequest.Builder(Worker_Catalog_DeleteMultipleItems.class)
                    .setInputData(dataCatalogContentDeletion)
                    .addTag(Worker_Catalog_DeleteMultipleItems.TAG_WORKER_CATALOG_DELETE_MULTIPLE_ITEMS) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getContext()).enqueue(otwrCatalogDeleteContent);

        } else {
            //If no private catalog items found, go ahead and search/remove private tags held by the
            // user. If there were private catalog items, this step is executed by the worker
            // responsible for removing those catalog items.
            if(getActivity() == null) return;
            Double dTimeStamp = GlobalClass.GetTimeStampDouble();
            Data dataDeleteUser = new Data.Builder()
                    .putString(GlobalClass.EXTRA_CALLER_ID, "Worker_Catalog_DeleteMultipleItems:doWork()")
                    .putDouble(GlobalClass.EXTRA_CALLER_TIMESTAMP, dTimeStamp)
                    .putString(GlobalClass.EXTRA_STRING_USERNAME, icu_UserToDelete.sUserName)
                    .build();
            OneTimeWorkRequest otwrUserDelete = new OneTimeWorkRequest.Builder(Worker_User_Delete.class)
                    .setInputData(dataDeleteUser)
                    .addTag(Worker_User_Delete.TAG_WORKER_USER_DELETE) //To allow finding the worker later.
                    .build();
            WorkManager.getInstance(getActivity().getApplicationContext()).enqueue(otwrUserDelete);


        }

        //todo: If the current user is the user to be deleted, log them out and into the "guest"
        // account, and return to the main activity.
        if(GlobalClass.gicuCurrentUser.sUserName.equals(icu_UserToDelete.sUserName)){

        }


    }

    public class UserDeleteResponseReceiver extends BroadcastReceiver {

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

            //Check to see if this is a completion notification about a user-delete operation:

            boolean bUserDeleteComplete = intent.getBooleanExtra(Worker_User_Delete.USER_DELETE_COMPLETE_NOTIFICATION_BOOLEAN,false);
            if(bUserDeleteComplete){
                Toast.makeText(getContext(), "User deletion complete.", Toast.LENGTH_SHORT).show();
                RefreshUserListView();
            }

        }
    }






}