package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.TreeMap;

public class ComicsCatalogActivity extends AppCompatActivity {

    //Global Constants:
    private static final String LOG_TAG = "ComicsCatalogActivity";

    //Global Variables:
    private GlobalClass globalClass;
    private String gsComicFolder_AbsolutePath;
    private RecyclerView.Adapter<RecyclerViewComicsAdapter.ViewHolder> gRecyclerViewComicsAdapter;
    private boolean gbDebugTouch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_catalog);

        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();

        if(globalClass.ObfuscationOn) {
            setTitle(globalClass.getObfuscatedProgramName());
        } else {
            setTitle(globalClass.getNonObfuscatedProgramName());
        }

        File fCatalogComicsFolder;
        File fCatalogContentsFile;
        File fLogsFolder;

        String sExternalStorageState;
        sExternalStorageState = Environment.getExternalStorageState();
        if (sExternalStorageState.equals(Environment.MEDIA_MOUNTED) ){

            // Get the NHComicManager directory that's inside the app-specific directory on
            // external storage, or create it.
            boolean bFolderOk = false ;

            File[] fAvailableDirs = getExternalFilesDirs(null);
            if (fAvailableDirs.length == 2) {
                //Create the folder on the likely SDCard:
                fCatalogComicsFolder = new File(fAvailableDirs[1] + File.separator + "Comics");
                Toast.makeText(this, "Using SD Card.", Toast.LENGTH_SHORT).show();
            }else{
                //Create the folder on the likely Internal storage.
                fCatalogComicsFolder = new File(fAvailableDirs[1] + File.separator + "Comics");
                Toast.makeText(this, "Using internal storage.", Toast.LENGTH_SHORT).show();
            }

            if(!fCatalogComicsFolder.exists()) {
                if (fCatalogComicsFolder.mkdirs()) {
                    bFolderOk = true;
                }
            }else{
                bFolderOk = true;
            }

            if (!bFolderOk) {
                //If the catalog folder does not exist and cannot be created:
                Log.e(LOG_TAG, "Directory not created");
                TextView tvCatalogStatus = findViewById(R.id.textView_CatalogStatus);
                tvCatalogStatus.setText(R.string.no_catalog_location);
            } else {

                //Set the global variable holding the catalog comics folder:
                globalClass.setCatalogComicsFolder(fCatalogComicsFolder);

                //Look for the catalog status file:
                fCatalogContentsFile = new File(fCatalogComicsFolder.getAbsolutePath() + File.separator + "CatalogContents.dat");
                if (!fCatalogContentsFile.exists()){
                    try {
                        if(fCatalogContentsFile.createNewFile()) {
                            FileWriter fwCatalogContentsFile = null;
                            try {
                                fwCatalogContentsFile = new FileWriter(fCatalogContentsFile, true);

                                //Write the header line to the file:
                                fwCatalogContentsFile.append(GlobalClass.ComicRecordFields[0]);
                                for(int i = 1; i < GlobalClass.ComicRecordFields.length; i++) {
                                    fwCatalogContentsFile.append("\t");
                                    fwCatalogContentsFile.append("GlobalClass.ComicRecordFields[i]");
                                }
                                fwCatalogContentsFile.append("\n");

                            } catch (Exception e) {
                                Toast.makeText(this, "Problem during CatalogContentsFile write.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                            } finally {
                                try {
                                    if (fwCatalogContentsFile != null) {
                                        fwCatalogContentsFile.flush();
                                        fwCatalogContentsFile.close();
                                    }
                                } catch (IOException e) {
                                    Toast.makeText(this, "Problem during CatalogContentsFile flush/close.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            Toast.makeText(this, "Could not create CatalogContents.dat at" + fCatalogComicsFolder.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        }
                    }catch (IOException e){
                        Toast.makeText(this, "Could not create CatalogContents.dat at" + fCatalogComicsFolder.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    }
                }
                if(fCatalogContentsFile.exists()){
                    //If the catalog contents file exists, set the global variable:
                    globalClass.setCatalogContentsFile(fCatalogContentsFile);
                }

                //Look for the Logs folder. If it does not exist, create it.
                fLogsFolder = new File(fCatalogComicsFolder + File.separator + "Logs");
                if(!fLogsFolder.exists()) {
                    if(!fLogsFolder.mkdirs()){
                        Toast.makeText(this, "Could not create log folder at" + fLogsFolder.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    }
                }
                if(fLogsFolder.exists()) {
                    globalClass.setLogsFolder(fLogsFolder);
                }

                //Build the internal list of comics:
                TreeMap<Integer, String[]> tmCatalogComicList = new TreeMap<>();

                //Read the list of comics and populate the catalog array:
                BufferedReader brReader;
                try {
                    brReader = new BufferedReader(new FileReader(fCatalogContentsFile.getAbsolutePath()));
                    brReader.readLine(); //The first line is the header. Skip this line.
                    String sLine = brReader.readLine();
                    String[] sFields;
                    int iComicRID = 0;
                    while (sLine != null) {
                        //Split the line read from the contents file with the delimiter of TAB:
                        sFields = sLine.split("\t",-1);
                        tmCatalogComicList.put(iComicRID, sFields);

                        // read next line
                        sLine = brReader.readLine();
                        iComicRID++;
                    }
                    brReader.close();

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Trouble reading CatalogContents.dat at" + fCatalogComicsFolder.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }

                //Update main activity view to include the listing of the comics.
                TextView tvCatalogStatus = findViewById(R.id.textView_CatalogStatus);
                if (tmCatalogComicList.size() <= 0 ) {
                    tvCatalogStatus.setVisibility(View.VISIBLE);
                    String s = "Catalog contains " + tmCatalogComicList.size() + " comics.";
                    tvCatalogStatus.setText(s);
                } else {
                    tvCatalogStatus.setVisibility(View.INVISIBLE);
                }

                //Set the global variable holding the comic list:
                globalClass.setCatalogComicList(tmCatalogComicList);
            }
        }

        gsComicFolder_AbsolutePath = globalClass.getCatalogComicsFolder().getAbsolutePath();

        populate_RecyclerViewComicsCatalog();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comics_catalog_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Display a message showing the name of the item selected.
        //Toast.makeText(this, "Selected Item: " +item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {
            case R.id.menu_import:
                Intent intentImport = new Intent(this, ImportComicsActivity.class);
                startActivity(intentImport);
                return true;

            case R.id.menu_export:
                Intent intentExport = new Intent(this, ExportComicsActivity.class);
                startActivity(intentExport);
                return true;

            case R.id.menu_FlipView:
                FlipObfuscation();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        //https://developer.android.com/training/data-storage/shared/documents-files
        try {
            super.onActivityResult(requestCode, resultCode, resultData);

        } catch (Exception ex) {
            Context context = getApplicationContext();
            Toast.makeText(context, ex.toString(),
                    Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, ex.toString());
        }


    }



    //=====================================================================================
    //===== RecyclerView Code =================================================================
    //=====================================================================================

    public void populate_RecyclerViewComicsCatalog(){

        RecyclerView recyclerView = findViewById(R.id.RecyclerView_ComicsCatalog);
        // use this setting to
        // improve performance if you know that changes
        // in content do not change the layout size
        // of the RecyclerView
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager;
        GridLayoutManager gridLayoutManager;

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape
            // use a grid layout manager
            gridLayoutManager = new GridLayoutManager(this, 4, RecyclerView.VERTICAL, false);
            recyclerView.setLayoutManager(gridLayoutManager);
        } else {
            // In portrait
            // use a linear layout manager
            layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
        }


        //Build the internal list of comics:
        TreeMap<Integer, String[]> tmCatalogComicList;
        //Set the global variable holding the comic list:
        tmCatalogComicList = globalClass.getCatalogComicList();

        gRecyclerViewComicsAdapter = new RecyclerViewComicsAdapter(tmCatalogComicList);
        recyclerView.setAdapter(gRecyclerViewComicsAdapter);


    }


    public class RecyclerViewComicsAdapter extends RecyclerView.Adapter<RecyclerViewComicsAdapter.ViewHolder> {

        private final TreeMap<Integer, String[]> treeMap;
        private final Integer[] mapKeys;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public final ImageView ivThumbnail;
            public final TextView tvComicName;
            public final TextView tvComicDetails;

            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.ImageView_Thumbnail);
                tvComicName = v.findViewById(R.id.TextView_ComicName);
                tvComicDetails = v.findViewById(R.id.TextView_ComicDetails);
            }
        }

        public RecyclerViewComicsAdapter(TreeMap<Integer, String[]> data) {
            this.treeMap = data;
            mapKeys = treeMap.keySet().toArray(new Integer[getCount()]);
        }

        public int getCount() {
            return treeMap.size();
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public RecyclerViewComicsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                       int viewType) {
            // create a new view
            View v;

            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                v = inflater.inflate(R.layout.recycler_comics_grid, parent, false);
            } else {
                v = inflater.inflate(R.layout.recycler_comics_row, parent, false);
            }

            return new ViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, final int position) {
            // - get element from your data set at this position
            // - replace the contents of the view with that element

            //Get the data for the row:
            String[] sFields;
            sFields = treeMap.get(mapKeys[position]);
            if (sFields == null) {
                sFields = new String[GlobalClass.ComicRecordFields.length]; //To prevent possible null pointer exception later.
            }


            if(globalClass.ObfuscationOn) {

                //Get the obfuscation image index:
                int i = (position % globalClass.getObfuscationImageCount());
                //Get the obfuscation image resource ID:
                int iObfuscatorResourceID = globalClass.getObfuscationImage(i);

                Bitmap bmObfuscator = BitmapFactory.decodeResource(getResources(), iObfuscatorResourceID);
                holder.ivThumbnail.setImageBitmap(bmObfuscator);
                holder.tvComicName.setText(globalClass.getObfuscationCategoryName());
            } else {

                //Load the non-obfuscated image into the RecyclerView ViewHolder:
                String sThumbnailFilePath = gsComicFolder_AbsolutePath + File.separator
                        + sFields[GlobalClass.COMIC_FOLDER_NAME_INDEX] + File.separator
                        + sFields[GlobalClass.COMIC_THUMBNAIL_FILE_INDEX];
                File fThumbnail = new File(sThumbnailFilePath);

                if (fThumbnail.exists()) {
                    Glide.with(getApplicationContext()).load(fThumbnail).into(holder.ivThumbnail);
                }

                holder.tvComicName.setText(sFields[GlobalClass.COMIC_NAME_INDEX]);

            }


            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(gbDebugTouch) Toast.makeText(getApplicationContext(),"Click Item Number " + position, Toast.LENGTH_LONG).show();
                    StartComicViewerActivity(position);
                }
            });

            holder.ivThumbnail.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(gbDebugTouch) Toast.makeText(getApplicationContext(), "Long press detected", Toast.LENGTH_SHORT).show();
                    Obfuscate();
                    return true;// returning true instead of false, works for me
                }
            });

            if(holder.tvComicDetails != null) {
                //The landscape version (GridView) does not have a "Comic Details" TextView, so
                //  don't try to set it if this object is null.
                String s = "Comic ID: " + sFields[GlobalClass.COMIC_ID_INDEX];
                holder.tvComicDetails.setText(s);
            }
        }

        // Return the size of the data set (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return treeMap.size();
        }

    }

    //=====================================================================================
    //===== Comic Viewer Code =================================================================
    //=====================================================================================

    public void StartComicViewerActivity(int iComicSequence){
        Intent intentComicViewer = new Intent(this, ComicViewerActivity.class);
        TreeMap<Integer, String[]> tmCatalogComicList;
        tmCatalogComicList = globalClass.getCatalogComicList();
        String[] sFields = tmCatalogComicList.get(iComicSequence);

        intentComicViewer.putExtra(ComicViewerActivity.COMIC_FIELDS_STRING,sFields);
        intentComicViewer.putExtra(ComicViewerActivity.SELECTED_COMIC_INDEX,iComicSequence);

        startActivity(intentComicViewer);
    }


    //=====================================================================================
    //===== Obfuscation Code =================================================================
    //=====================================================================================

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
        setTitle(globalClass.getObfuscatedProgramName());
        //Update the RecyclerView:
        gRecyclerViewComicsAdapter.notifyDataSetChanged();
    }

    public void RemoveObfuscation(){
        //Remove obfuscation:
        setTitle(globalClass.getNonObfuscatedProgramName());
        //Update the RecyclerView:
        gRecyclerViewComicsAdapter.notifyDataSetChanged();
    }


}