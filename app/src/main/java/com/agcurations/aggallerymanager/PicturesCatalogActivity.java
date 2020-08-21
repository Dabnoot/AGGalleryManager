package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class PicturesCatalogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Return theme away from startup_screen
        setTheme(R.style.AppTheme);

        setContentView(R.layout.activity_pictures_catalog);
    }
}