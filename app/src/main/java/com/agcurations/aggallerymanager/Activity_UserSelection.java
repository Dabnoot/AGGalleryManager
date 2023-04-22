package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowMetrics;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class Activity_UserSelection extends AppCompatActivity {

    Activity_UserSelectionResponseReceiver activity_userSelectionResponseReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_selection);

        setTitle("Select User");

        IntentFilter filter = new IntentFilter(Activity_UserSelectionResponseReceiver.ACTIVITY_USERSELECTION_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        activity_userSelectionResponseReceiver = new Activity_UserSelectionResponseReceiver();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(activity_userSelectionResponseReceiver, filter);

        final GlobalClass globalClass = (GlobalClass) getApplicationContext();

        //Initialize the displayed list of users:
        ListView listView_UserList = findViewById(R.id.listView_UserList);
        ArrayList<ItemClass_User> alicuAllUserPool = new ArrayList<>(globalClass.galicu_Users);
        AdapterUserList adapterUserList = new AdapterUserList(
                getApplicationContext(), R.layout.listview_useritem, alicuAllUserPool);
        //adapterUserList.gbSimplifiedView = true;  //Hides the maturity rating and admin status
        listView_UserList.setAdapter(adapterUserList);
        listView_UserList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ItemClass_User icu = (ItemClass_User) parent.getItemAtPosition(position);
                final String sWelcomeMessage = "Welcome, " + icu.sUserName + ".";
                if(!icu.sPin.equals("")){
                    //If this user record requires a pin to log-in, display the pin code popup:

                    //Configure the AlertDialog that will gather the pin code if necessary to begin a particular behavior:
                    final AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext(), R.style.AlertDialogCustomStyle);

                    // set the custom layout
                    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                    final View customLayout = inflater.inflate(R.layout.dialog_layout_pin_code, null);
                    builder.setView(customLayout);

                    final AlertDialog adConfirmationDialog = builder.create();

                    //Code action for the Cancel button:
                    Button button_PinCodeCancel = customLayout.findViewById(R.id.button_PinCodeCancel);
                    button_PinCodeCancel.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            adConfirmationDialog.dismiss();
                        }
                    });

                    //Code action for the OK button:
                    Button button_PinCodeOK = customLayout.findViewById(R.id.button_PinCodeOK);

                    //Code action for the OK button:
                    button_PinCodeOK.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                            String sPinEntered = editText_DialogInput.getText().toString();

                            if (sPinEntered.equals(icu.sPin)) {
                                globalClass.gicuCurrentUser = icu;
                                Toast.makeText(getApplicationContext(), sWelcomeMessage, Toast.LENGTH_SHORT).show();
                                adConfirmationDialog.dismiss();
                                closeActivity(getApplicationContext());
                            } else {
                                Toast.makeText(getApplicationContext(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                                adConfirmationDialog.dismiss();
                            }

                        }
                    });

                    adConfirmationDialog.show();

                } else {
                    //If this user record does not require a pin to log-in, merely log-in.
                    globalClass.gicuCurrentUser = icu;
                    Toast.makeText(getApplicationContext(), sWelcomeMessage, Toast.LENGTH_SHORT).show();
                    closeActivity(getApplicationContext());
                }

            } //End if ListView.onItemClick().

        }); //End ListView.setOnItemClickListener()

    }

    public static void closeActivity(Context context){
        //Broadcast a message to be picked-up by the Activity to close the activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Activity_UserSelectionResponseReceiver.ACTIVITY_USERSELECTION_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(Activity_UserSelectionResponseReceiver.EXTRA_BOOL_CLOSE_ACTIVITY, true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
    }

    //Create a listener
    public class Activity_UserSelectionResponseReceiver extends BroadcastReceiver {

        public static final String ACTIVITY_USERSELECTION_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.ACTIVITY_USERSELECTION_ACTION_RESPONSE";
        public static final String EXTRA_BOOL_CLOSE_ACTIVITY = "EXTRA_BOOL_CLOSE_ACTIVITY";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bCloseActivity;

            //Get boolean indicating that an error may have occurred:
            bCloseActivity = intent.getBooleanExtra(EXTRA_BOOL_CLOSE_ACTIVITY,false);
            if(bCloseActivity) {
                finish();
            }


        }
    }



}