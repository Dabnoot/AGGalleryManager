
package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class Activity_Main extends AppCompatActivity {

    //Global Variables:
    GlobalClass globalClass;

    MainActivityDataServiceResponseReceiver mainActivityDataServiceResponseReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.MainTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        mainActivityDataServiceResponseReceiver = new MainActivityDataServiceResponseReceiver();
        registerReceiver(mainActivityDataServiceResponseReceiver, filter);
        //Call the MA Data Service, which will create a call to a service:
        Service_Main.startActionLoadData(this);


        //AlertDialogTest2();


    }

    private void AlertDialogTest2(){
        //Testing of AlertDialog style:
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustomStyle);

        // set the custom layout
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_layout_pin_code, null);
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
        button_PinCodeOK.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                String sPinEntered = editText_DialogInput.getText().toString();

                if(sPinEntered.equals(globalClass.gsPin)){
                    Toast.makeText(getApplicationContext(), "Correct pin entered.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                }

                adConfirmationDialog.dismiss();
            }
        });

        adConfirmationDialog.show();
    }

    private void AlertDialogTest1(){
        //Testing of AlertDialog style:
        String sConfirmationMessage = "Confirm item: Test test test test test test test";
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustomStyle);
        builder.setTitle("Delete Tag");
        builder.setMessage(sConfirmationMessage);
        // Set up the input
        final EditText editText_DialogInput = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        editText_DialogInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        builder.setView(editText_DialogInput);
        //builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog adConfirmationDialog = builder.create();
        adConfirmationDialog.show();
    }





    public static class MainActivityDataServiceResponseReceiver extends BroadcastReceiver {
        //MADataService = Main Activity Data Service
        public static final String MA_DATA_SERVICE_ACTION_RESPONSE = "com.agcurations.aggallerymanager.intent.action.FROM_MA_DATA_SERVICE";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean bError;

            //Get boolean indicating that an error may have occurred:
            bError = intent.getBooleanExtra(Service_Main.EXTRA_BOOL_DATA_LOAD_PROBLEM,false);
            if(bError) {
                String sMessage = intent.getStringExtra(Service_Main.EXTRA_STRING_DATA_LOAD_PROBLEM);
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mainActivityDataServiceResponseReceiver);
        super.onDestroy();
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

        //Configure the AlertDialog that will gather the pin code if necessary to begina particular behavior:
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustomStyle);

        // set the custom layout
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_layout_pin_code, null);
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


        if(item.getItemId() == R.id.menu_FlipView) {
            FlipObfuscation();
            return true;
        } else if(item.getItemId() == R.id.menu_import) {
            Intent intentImportGuided = new Intent(this, Activity_Import.class);
            startActivity(intentImportGuided);
            return true;
        } else  if(item.getItemId() == R.id.menu_Settings) {
            //Ask for pin code in order to allow access to Settings:

            button_PinCodeCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adConfirmationDialog.dismiss();
                }
            });

            //Code action for the OK button:
            button_PinCodeOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                    String sPinEntered = editText_DialogInput.getText().toString();

                    if (sPinEntered.equals(globalClass.gsPin)) {
                        Intent intentSettings = new Intent(getApplicationContext(), Activity_AppSettings.class);
                        startActivity(intentSettings);
                    } else {
                        Toast.makeText(getApplicationContext(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                    }

                    adConfirmationDialog.dismiss();
                }
            });

            adConfirmationDialog.show();

            return true;
        } else if(item.getItemId() == R.id.menu_TagEditor) {

            //Ask for pin code in order to allow access to the Tag Editor:

            button_PinCodeOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditText editText_DialogInput = customLayout.findViewById(R.id.editText_DialogInput);
                    String sPinEntered = editText_DialogInput.getText().toString();

                    if (sPinEntered.equals(globalClass.gsPin)) {
                        Intent intentTagEditor = new Intent(getApplicationContext(), Activity_TagEditor.class);
                        startActivity(intentTagEditor);
                    } else {
                        Toast.makeText(getApplicationContext(), "Incorrect pin entered.", Toast.LENGTH_SHORT).show();
                    }

                    adConfirmationDialog.dismiss();
                }
            });

            adConfirmationDialog.show();

            return true;
        } else if(item.getItemId() == R.id.menu_About) {


            return true;
        } else if(item.getItemId() == R.id.menu_Test) {

            //Testing WorkManager:
            //WorkManager workManager = WorkManager.getInstance(getApplicationContext());
            //workManager.enqueue(OneTimeWorkRequest.from(Worker_FileDownload.class));

            //Testing DownloadManager:
            //Use the download manager to download the file:
            String sFolderName = "0000";
            String sShortPath = globalClass.gfCatalogFolders[GlobalClass.MEDIA_CATEGORY_COMICS].getPath();
            sShortPath = sShortPath + File.separator + sFolderName;
            String sTestURL = "https://thumbs.dreamstime.com/z/tv-test-image-card-rainbow-multi-color-bars-geometric-signals-retro-hardware-s-minimal-pop-art-print-suitable-89603663.jpg";
            String sTestFileName = "1000.gpj";
            String sFullPathFileName = sShortPath + File.separator + sTestFileName;
            File fTestFile = new File(sFullPathFileName);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sTestURL));
            request.setTitle("AG Gallery+ File Download")
                    .setDescription("Download test file")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    //Set to equivalent of binary file so that Android MediaStore will not try to index it,
                    //  and the user can't easily open it. https://stackoverflow.com/questions/6783921/which-mime-type-to-use-for-a-binary-file-thats-specific-to-my-program
                    .setMimeType("application/octet-stream")
                    .setDestinationUri(Uri.fromFile(fTestFile));

            // get download service and enqueue file
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);


            return true;
        } else {
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
        ImageView ivPictures = findViewById(R.id.imageView_Images);
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
        ImageView ivPictures = findViewById(R.id.imageView_Images);
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
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_VIDEOS);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
        startActivity(intentCatalogActivity);
    }

    public void startPicturesCatalogActivity(View v){
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_IMAGES);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
        startActivity(intentCatalogActivity);
    }

    public void startComicsCatalogActivity(View v){
        /*Intent intentComicsCatalogActivity = new Intent(this, ComicsCatalogActivity.class);
        startActivity(intentComicsCatalogActivity);*/
        Intent intentCatalogActivity = new Intent(this, Activity_CatalogViewer.class);
        //intentCatalogActivity.putExtra("MEDIA_CATEGORY", GlobalClass.MEDIA_CATEGORY_COMICS);
        globalClass.giSelectedCatalogMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
        startActivity(intentCatalogActivity);
    }

    public void startImportVideos(View v){
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_VIDEOS);
        startActivity(intentImportGuided);
    }

    public void startImportImages(View v){
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_IMAGES);
        startActivity(intentImportGuided);
    }

    public void startImportComics(View v){
        Intent intentImportGuided = new Intent(this, Activity_Import.class);
        intentImportGuided.putExtra(Activity_Import.EXTRA_INT_MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_COMICS);
        startActivity(intentImportGuided);
    }



}