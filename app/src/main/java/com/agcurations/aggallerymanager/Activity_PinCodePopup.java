package com.agcurations.aggallerymanager;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class Activity_PinCodePopup extends Activity {

    GlobalClass globalClass;

    public static final int PIN_CODE_MATCH = -1;
    public static final int PIN_CODE_NO_MATCH = 0;

    int giKeyboardAdjusted_y;
    boolean gbLPKeyboardVisibility = false;
    int giKbTick = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Source: https://www.youtube.com/watch?v=fn5OlqQuOCk
        setContentView(R.layout.activity_pin_code_popup);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        final int height = dm.heightPixels;

        float fSize = 0.35f;

        getWindow().setLayout((int)(width * fSize), (int)(height * fSize));

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        getWindow().setAttributes(lp);


        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        RelativeLayout relativeLayout_PinCode = findViewById(R.id.relativeLayout_PinCode);

        giKeyboardAdjusted_y = getActionBarHeight() + getStatusBarHeight() + 10;

        ViewCompat.setOnApplyWindowInsetsListener(relativeLayout_PinCode, new androidx.core.view.OnApplyWindowInsetsListener(){

            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                boolean bCurrentKeyboardIsVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

                if(bCurrentKeyboardIsVisible != gbLPKeyboardVisibility && giKbTick == 0) {
                    if (bCurrentKeyboardIsVisible) {
                        giKbTick = 1;
                        int iPinCodePopupHeight = v.getHeight();
                        int iHeightMarginTop = (height - iPinCodePopupHeight) / 2;
                        lp.y = giKeyboardAdjusted_y - iHeightMarginTop;
                    } else {
                        lp.y = 0;
                    }
                    getWindow().setAttributes(lp);
                    gbLPKeyboardVisibility = bCurrentKeyboardIsVisible;
                } else {
                    giKbTick = 0;
                }

                return insets;
            }

        });


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

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

/*    public int getNavigationBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }*/

    public int getActionBarHeight() {
        int result = 0;
        // Calculate ActionBar height
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
            result = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }
        return result;
    }

}
