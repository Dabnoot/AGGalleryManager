package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    //Global Variables:
    GlobalClass globalClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.MainTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        //Initialize the File objects with paths:
        globalClass.initFolderAndFileStructure();

        if(!globalClass.ObfuscationOn) {
            //Remove obfuscation:
            RemoveObfuscation();
        }

        //Read the user-set pin for feature access:
        globalClass.readPin(this);

        if(GlobalClass.gvfComicTagsFile.exists()) {
            //Get Comic Tags from file:
            //Read the list of comics and populate the catalog array:
            BufferedReader brReader;
            try {

                brReader = new BufferedReader(new FileReader(GlobalClass.gvfComicTagsFile.getAbsolutePath()));
                String sLine = brReader.readLine();
                if(sLine != null) {
                    String[] sTags;
                    sTags = sLine.split(",");
                    brReader.close();
                    SortedSet<String> ssTags = new TreeSet<>();
                    for (String sEntry : sTags) {
                        //Don't add duplicates
                        //The SortedSet<T> class does not accept duplicate elements.
                        //  If item is already in the set, this method returns false
                        //  and does not throw an exception.
                        ssTags.add(sEntry.trim());
                    }
                    globalClass.gssAllUniqueCatalogComicTags = ssTags;
                }
            } catch (IOException e) {
                Toast.makeText(this,
                        "Trouble reading ComicTags.dat at" + GlobalClass.gvfComicTagsFile.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
            }
        }

    }



    //=====================================================================================
    //===== Menu Code =================================================================
    //=====================================================================================

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Display a message showing the name of the item selected.
        //Toast.makeText(this, "Selected Item: " +item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {

            case R.id.menu_FlipView:
                FlipObfuscation();
                return true;

            case R.id.menu_import_guided:
                Intent intentImportGuided = new Intent(this, ImportActivity.class);
                startActivity(intentImportGuided);
                return true;

            case R.id.menu_Settings:
                Intent intentSettings = new Intent(this, AGGallerySettingsActivity.class);

                startActivity(intentSettings);
                return true;

            case R.id.menu_About:
                Intent intentAbout = new Intent(this, ScrollingAboutActivity.class);
                startActivity(intentAbout);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }



    //=====================================================================================
    //===== Obfuscation Code =================================================================
    //=====================================================================================

    @Override
    public void onResume(){
        super.onResume();
        if(globalClass.ObfuscationOn) {
            //Obfuscate data:
            Obfuscate();
        } else {
            //Remove obfuscation:
            RemoveObfuscation();
        }
    }

    public void FlipObfuscation() {
        //This routine primarily accessed via the toggle option on the menu.
        globalClass.ObfuscationOn = !globalClass.ObfuscationOn;
        if(globalClass.ObfuscationOn) {
            //Obfuscate data:
            Obfuscate();
        } else {
            //Remove obfuscation:
            RemoveObfuscation();
       }

    }

    public void Obfuscate() {
        //This routine is separate because it can be activated
        // by either a long-press or the toggle option on the menu.
        if(!globalClass.ObfuscationOn) {
            globalClass.ObfuscationOn = true;
        }

        ImageView ivVideos = findViewById(R.id.imageView_Video);
        ImageView ivPictures = findViewById(R.id.imageView_Pictures);
        ImageView ivComics = findViewById(R.id.imageView_Comics);

        ivVideos.setImageResource(R.drawable.ob_productivity_tools);
        ivPictures.setImageResource(R.drawable.ob_data_management);
        ivComics.setImageResource(R.drawable.ob_quality_analysis);

        TextView tvVideos = findViewById(R.id.textView_Videos);
        TextView tvPictures = findViewById(R.id.textView_Pictures);
        TextView tvComics = findViewById(R.id.textView_Comics);

        tvVideos.setText(R.string.productivity_tools);
        tvPictures.setText(R.string.data_management);
        tvComics.setText(R.string.quality_analysis);

    }

    public void RemoveObfuscation() {
        //Remove obfuscation:
        ImageView ivVideos = findViewById(R.id.imageView_Video);
        ImageView ivPictures = findViewById(R.id.imageView_Pictures);
        ImageView ivComics = findViewById(R.id.imageView_Comics);

        ivVideos.setImageResource(R.drawable.nob_videos);
        ivPictures.setImageResource(R.drawable.nob_pictures);
        ivComics.setImageResource(R.drawable.nob_comics);

        TextView tvVideos = findViewById(R.id.textView_Videos);
        TextView tvPictures = findViewById(R.id.textView_Pictures);
        TextView tvComics = findViewById(R.id.textView_Comics);

        tvVideos.setText(R.string.videos);
        tvPictures.setText(R.string.pictures);
        tvComics.setText(R.string.comics);
    }

    //=====================================================================================
    //===== ImageView Click Code =================================================================
    //=====================================================================================

    public void startComicsCatalogActivity(View v){
        Intent intentComicsCatalogActivity = new Intent(this, ComicsCatalogActivity.class);
        startActivity(intentComicsCatalogActivity);
    }

    public void startPicturesCatalogActivity(View v){
        Intent intentPicturesCatalogActivity = new Intent(this, PicturesCatalogActivity.class);
        startActivity(intentPicturesCatalogActivity);
    }

}