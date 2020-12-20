package com.agcurations.aggallerymanager;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class Activity_PinCodePopup extends Activity {

    GlobalClass globalClass;

    public static final int START_ACTIVITY_FOR_RESULT_PIN_CODE_ACCESS_SETTINGS = 2100;
    public static final int START_ACTIVITY_FOR_RESULT_UNLOCK_RESTRICTED_TAGS = 2200;
    public static final int START_ACTIVITY_FOR_RESULT_EDIT_TAGS = 2300;

    public static final int PIN_CODE_MATCH = -1;
    public static final int PIN_CODE_NO_MATCH = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Source: https://www.youtube.com/watch?v=fn5OlqQuOCk
        setContentView(R.layout.activity_pin_code_popup);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        float fSize = 0.35f;

        getWindow().setLayout((int)(width * fSize), (int)(height * fSize));

    }


    public void buttonOkClick(View v){
        globalClass = (GlobalClass) getApplicationContext();
        EditText editText_NumberPassword = findViewById(R.id.editText_NumberPassword);
        String sPinCode = editText_NumberPassword.getText().toString();
        if(globalClass.gsPin != null) {
            if (sPinCode.contentEquals(globalClass.gsPin) || (globalClass.gsPin.contentEquals(""))) {
                setResult(PIN_CODE_MATCH);
            } else {
                setResult(PIN_CODE_NO_MATCH);
                Toast.makeText(this, "Incorrect pin.", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    public void buttonCancelClick(View v){
        setResult(PIN_CODE_NO_MATCH);
        finish();
    }


}
