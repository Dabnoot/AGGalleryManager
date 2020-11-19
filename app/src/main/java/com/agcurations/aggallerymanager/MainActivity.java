package com.agcurations.aggallerymanager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.TreeMap;

import javax.xml.transform.Result;

public class MainActivity extends AppCompatActivity {

    //Global Variables:
    GlobalClass globalClass;

    // a static variable to get a reference of our activity context
    public static Context contextOfActivity;
    public static Context getContextOfActivity(){
        return contextOfActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.MainTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contextOfActivity = this;

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        if(!globalClass.ObfuscationOn) {
            //Remove obfuscation:
            RemoveObfuscation();
        }

        //Configure a response receiver to listen for updates from the Main Activity (MA) Data Service:
        //  This will load the tags files for videos, pictures, and comics.
        IntentFilter filter = new IntentFilter(MainActivityDataServiceResponseReceiver.MA_DATA_SERVICE_ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        MainActivityDataServiceResponseReceiver mainActivityDataServiceResponseReceiver = new MainActivityDataServiceResponseReceiver();
        registerReceiver(mainActivityDataServiceResponseReceiver, filter);
        //Call the MA Data Service, which will create a call to a service:
        MainActivityDataService.startActionLoadData(this);


    }

    public static class MainActivityDataServiceResponseReceiver extends BroadcastReceiver {
        //MADataService = Main Activity Data Service
        public static final String MA_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_MA_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(MainActivityDataService.EXTRA_BOOL_DATA_LOAD_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(MainActivityDataService.EXTRA_STRING_DATA_LOAD_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
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
                //Ask for pin code in order to allow access to Settings:
                Intent intentPinCodeAccessSettings = new Intent(this, PinCodePopup.class);
                startActivityForResult(intentPinCodeAccessSettings, PinCodePopup.START_ACTIVITY_FOR_RESULT_PIN_CODE_ACCESS_SETTINGS);
                return true;

            case R.id.menu_About:
                Intent intentAbout = new Intent(this, ScrollingAboutActivity.class);
                startActivity(intentAbout);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PinCodePopup.START_ACTIVITY_FOR_RESULT_PIN_CODE_ACCESS_SETTINGS){
            if(resultCode == RESULT_OK){
                Intent intentAbout = new Intent(this, AGGallerySettingsActivity.class);
                startActivity(intentAbout);
            }
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
        tvPictures.setText(R.string.Images);
        tvComics.setText(R.string.comics);
    }

    //=====================================================================================
    //===== ImageView Click Code =================================================================
    //=====================================================================================

    public void startVideoCatalogActivity(View v){
        Intent intentCatalogActivity = new Intent(this, CatalogActivity.class);
        intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_VIDEOS);
        startActivity(intentCatalogActivity);
    }

    public void startPicturesCatalogActivity(View v){
        Intent intentCatalogActivity = new Intent(this, CatalogActivity.class);
        intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_IMAGES);
        startActivity(intentCatalogActivity);
    }

    public void startComicsCatalogActivity(View v){
        /*Intent intentComicsCatalogActivity = new Intent(this, ComicsCatalogActivity.class);
        startActivity(intentComicsCatalogActivity);*/
        Intent intentCatalogActivity = new Intent(this, CatalogActivity.class);
        intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_COMICS);
        startActivity(intentCatalogActivity);

    }

}