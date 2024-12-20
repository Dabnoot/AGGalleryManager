package com.agcurations.aggallerymanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class Activity_ComicPageEditor extends AppCompatActivity {

    public static final String EXTRA_STRING_COMIC_PAGE_FILE_URI = "com.agcurations.aggallermanager.EXTRA_STRING_COMIC_PAGE_FILE_URI";

    ImageView gImageView_ComicPage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_comic_page_editor);

        RelativeLayout relativeLayoutAll = findViewById(R.id.relativeLayout_All);
        if(relativeLayoutAll.getWindowInsetsController() != null) {
            relativeLayoutAll.getWindowInsetsController().hide(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        }

        // Initialize data

        //Get the intent used to start this activity:
        Intent intentCaller = getIntent();
        //Get data from the intent:
        String sComicPageFileUri = intentCaller.getStringExtra(EXTRA_STRING_COMIC_PAGE_FILE_URI);
        if( sComicPageFileUri == null) finish();
        Uri uriComicPageFile = Uri.parse(sComicPageFileUri);

        gImageView_ComicPage = findViewById(R.id.imageView_ComicPage);
        if(gImageView_ComicPage == null){
            finish();
        }

        //Get the width and height of the image:
        /*String sWidth = "";
        String sHeight = "";
        try {
            InputStream input = getApplicationContext().getContentResolver().openInputStream(uriComicPageFile);
            if (input != null) {
                BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                onlyBoundsOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
                input.close();
                sWidth = "" + onlyBoundsOptions.outWidth;
                sHeight = "" + onlyBoundsOptions.outHeight;
            }

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Trouble opening file at " + sComicPageFileUri, Toast.LENGTH_SHORT).show();
            finish();
        }*/

        Glide.with(getApplicationContext())
                .load(uriComicPageFile)
                .placeholder(R.drawable.baseline_image_white_18dp_wpagepad)
                .into(gImageView_ComicPage);

    }




}