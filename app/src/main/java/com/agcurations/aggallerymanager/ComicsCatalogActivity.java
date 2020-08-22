package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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

import java.util.Map;
import java.util.TreeMap;

public class ComicsCatalogActivity extends AppCompatActivity {

    //Global Constants:
    private static final String LOG_TAG = "ComicsCatalogActivity";

    //Global Variables:
    private GlobalClass globalClass;
    private String gsComicFolder_AbsolutePath;
    private RecyclerView.Adapter<RecyclerViewComicsAdapter.ViewHolder> gRecyclerViewComicsAdapter;
    private boolean gbDebugTouch = false;
    RecyclerView gRecyclerView;
    private boolean gbRecyclerViewFiltered;

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

                                //Write the activity_comic_details_header line to the file:
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

                    /*//Process any modifications to the CatalogContentsFile:
                    String[] sNewFields = new String[]{
                            "COMIC_SOURCE",
                            "COMIC_DATETIME_LAST_READ_BY_USER",
                            "COMIC_DATETIME_IMPORT"
                    };
                    Catalog_data_file_add_fields(sNewFields,1);
*/
                    /*int[] iFields = new int[]{
                            GlobalClass.COMIC_DATETIME_IMPORT
                    };

                    String[] sUpdateData = new String[]{
                            "0"
                    };
                    globalClass.CatalogDataFile_UpdateAllRecords(iFields,sUpdateData);*/

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
                    brReader.readLine(); //The first line is the activity_comic_details_header. Skip this line.
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

        gRecyclerView = findViewById(R.id.RecyclerView_ComicsCatalog);
        configure_RecyclerViewComicsCatalog();
        SetComicSortOrderDefault(); //This routine also populates the RecyclerView Adapter.

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comics_catalog_menu, menu);
        getMenuInflater().inflate(R.menu.comics_catalog_action_bar, menu);


        // Initialise menu item search bar with id and take its object
        //https://www.geeksforgeeks.org/android-searchview-with-example/
        MenuItem searchViewItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) searchViewItem.getActionView();
        // attach setOnQueryTextListener to search view defined above
        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    // Override onQueryTextSubmit method
                    @Override
                    public boolean onQueryTextSubmit(String query)
                    {
                        ComicsCatalogFilter(true, query);
                        gbRecyclerViewFiltered = true;
                        return false;
                    }
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                });
        //Set a listener for the "cancel search" button:
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                if(gbRecyclerViewFiltered) {
                    SetComicSortOrderDefault();
                }
                return false;
            }
        });





        return super.onCreateOptionsMenu(menu);
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

    public void configure_RecyclerViewComicsCatalog(){

        // use this setting to
        // improve performance if you know that changes
        // in content do not change the layout size
        // of the RecyclerView
        gRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager;
        GridLayoutManager gridLayoutManager;

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape
            // use a grid layout manager
            gridLayoutManager = new GridLayoutManager(this, 4, RecyclerView.VERTICAL, false);
            gRecyclerView.setLayoutManager(gridLayoutManager);
        } else {
            // In portrait
            // use a linear layout manager
            layoutManager = new LinearLayoutManager(this);
            gRecyclerView.setLayoutManager(layoutManager);
        }

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
            public final TextView tvThumbnailText;
            public final TextView tvComicDetails;

            public ViewHolder(View v) {
                super(v);
                ivThumbnail = v.findViewById(R.id.ImageView_Thumbnail);
                tvThumbnailText = v.findViewById(R.id.TextView_ThumbnailText);
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
            final String[] sFields_final = sFields;


            if(globalClass.ObfuscationOn) {

                //Get the obfuscation image index:
                int i = (position % globalClass.getObfuscationImageCount());
                //Get the obfuscation image resource ID:
                int iObfuscatorResourceID = globalClass.getObfuscationImage(i);

                Bitmap bmObfuscator = BitmapFactory.decodeResource(getResources(), iObfuscatorResourceID);
                holder.ivThumbnail.setImageBitmap(bmObfuscator);
                holder.tvThumbnailText.setText(globalClass.getObfuscationImageText(i));
            } else {

                //Load the non-obfuscated image into the RecyclerView ViewHolder:
                String sThumbnailFilePath = gsComicFolder_AbsolutePath + File.separator
                        + sFields[GlobalClass.COMIC_FOLDER_NAME_INDEX] + File.separator
                        + sFields[GlobalClass.COMIC_THUMBNAIL_FILE_INDEX];
                File fThumbnail = new File(sThumbnailFilePath);

                if (fThumbnail.exists()) {
                    Glide.with(getApplicationContext()).load(fThumbnail).into(holder.ivThumbnail);
                }

                holder.tvThumbnailText.setText(sFields[GlobalClass.COMIC_NAME_INDEX]);

            }


            holder.ivThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(gbDebugTouch) Toast.makeText(getApplicationContext(),"Click Item Number " + position, Toast.LENGTH_LONG).show();
                    StartComicViewerActivity(sFields_final);
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

    public void populate_RecyclerViewComicsCatalog(TreeMap<Integer, String[]> tmCatalogComicList){
        gRecyclerViewComicsAdapter = new RecyclerViewComicsAdapter(tmCatalogComicList);
        gRecyclerView.setAdapter(gRecyclerViewComicsAdapter);
    }

    public final int SORT_ORDER_ASCENDING = 0;
    public final int SORT_ORDER_DESCENDING = 1;

    public void SetComicSortOrderDefault(){
        ChangeComicSortOrder(GlobalClass.COMIC_TAGS_INDEX, SORT_ORDER_ASCENDING); //TODO, return to default sort.
        gbRecyclerViewFiltered = false;
    }

    public void ChangeComicSortOrder(int iField, int iOrder){

        //Create new TreeMap to presort the comics:
        TreeMap<String, String[]> treeMapPreSort; //String = field being sorted, String = Comic data
        treeMapPreSort = new TreeMap<>();

        //Get existing data and load elements into the presorter:
        TreeMap<Integer, String[]> tmCatalogComicList;
        tmCatalogComicList = globalClass.getCatalogComicList();
        String[] sComicListRecord;
        String sKey;
        for (Map.Entry<Integer, String[]>
                entry : tmCatalogComicList.entrySet()) {
            sComicListRecord = entry.getValue();
            //Append the ComicID to the key field to ensure that the key is always unique.
            //  The user could choose to sort by "LAST_READ_DATE", and the LAST_READ_DATE
            //  could be 0, duplicated. There cannot be duplicate keys.
            //  The user might also decide to sort by # of pages, for which there might
            //  be duplicates.
            sKey = sComicListRecord[iField] + sComicListRecord[GlobalClass.COMIC_ID_INDEX];
            treeMapPreSort.put(sKey, sComicListRecord);
        }

        //Treemap presort will auto-sort itself.

        //Delete everything out of the old TreeMap, and re-populate it with the new sort order:
        TreeMap<Integer, String[]> tmNewOrderCatalogComicList = new TreeMap<>();
        int iComicRID, iIterator;
        if(iOrder == SORT_ORDER_DESCENDING){
            iComicRID = treeMapPreSort.size();
            iIterator = -1;
        } else {
            iComicRID = 0;
            iIterator = 1;
        }

        for (Map.Entry<String, String[]>
                entry : treeMapPreSort.entrySet()) {
            sComicListRecord = entry.getValue();
            tmNewOrderCatalogComicList.put(iComicRID, sComicListRecord);
            iComicRID += iIterator;
        }

        //Re-populate the RecyclerVeiw adapter and tell the RecyclerView to update itself:
        populate_RecyclerViewComicsCatalog(tmNewOrderCatalogComicList);
    }

    public void ComicsCatalogFilter(boolean bFilterOn, String sFilterText){

        if(bFilterOn){
            //Create new TreeMap to presort the comics:
            TreeMap<Integer, String[]> treeMapFiltered;
            treeMapFiltered = new TreeMap<>();

            //Get existing data and load elements into the presorter:
            TreeMap<Integer, String[]> tmCatalogComicList;
            tmCatalogComicList = globalClass.getCatalogComicList();
            String[] sComicListRecord;
            StringBuilder sbKey;
            int iComicRID = 0;
            String sFilterText_LowerCase = sFilterText.toLowerCase();
            String sKey_LowerCase;
            for (Map.Entry<Integer, String[]>
                    entry : tmCatalogComicList.entrySet()) {
                sComicListRecord = entry.getValue();
                sbKey = new StringBuilder();
                sbKey.append(sComicListRecord[GlobalClass.COMIC_ID_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_NAME_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_PARODIES_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_CHARACTERS_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_TAGS_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_ARTISTS_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_GROUPS_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_CATEGORIES_INDEX]);
                sbKey.append(sComicListRecord[GlobalClass.COMIC_SOURCE]);
                sKey_LowerCase = sbKey.toString().toLowerCase();

                if(sKey_LowerCase.contains(sFilterText_LowerCase)){
                     treeMapFiltered.put(iComicRID, sComicListRecord);
                     iComicRID++;
                 }
            }
            populate_RecyclerViewComicsCatalog(treeMapFiltered);

        } else {

            SetComicSortOrderDefault();

        }
    }

    //=====================================================================================
    //===== Comic Viewer Code =================================================================
    //=====================================================================================

    public void StartComicViewerActivity(String[] sFields){

        Intent intentComicViewer = new Intent(this, ComicDetailsActivity.class);

        //intentComicViewer.putExtra(ComicDetailsActivity.COMIC_FIELDS_STRING,sFields);
        globalClass.gvSelectedComic = sFields; //Don't bother with using the intent to pass this data.

        startActivity(intentComicViewer);
    }


    //=====================================================================================
    //===== Obfuscation Code =================================================================
    //=====================================================================================

    @Override
    public void onResume(){
        super.onResume();

        //If we are returning from ComicDetailsActivity after deleting the comic,
        //  return the sort:
        if(globalClass.gbComicJustDeleted){
            SetComicSortOrderDefault();
            globalClass.gbComicJustDeleted = false;
        }


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

    //=====================================================================================
    //===== Catalog.dat Revision Routine(S) ===============================================
    //=====================================================================================

    public void Catalog_data_file_add_fields(String[] sNewFields, int iToVersion) {

        File fCatalogContentsFile = globalClass.getCatalogContentsFile();

        try {
            //Read the list of comics and populate the catalog array:
            BufferedReader brReader;
            brReader = new BufferedReader(new FileReader(fCatalogContentsFile.getAbsolutePath()));

            //Get the version of the current .dat file.
            String sLine = brReader.readLine();
            String[] sFields = sLine.split("\t");
            String[] sVersionData = sFields[sFields.length - 1].split(".");
            int iFromVersion = 0;
            if (sVersionData.length == 2) {
                iFromVersion = Integer.parseInt(sVersionData[1]);
            }
            //Quit this routine if the version of the .dat file to be written
            //  is the same or older:
            if (iToVersion <= iFromVersion) {
                brReader.close();
                return;
            }

            //Create the new catalog contents file:
            File fCatalogComicsFolder = globalClass.getCatalogComicsFolder();
            File fNewCatalogContentsFile;

            //Create a new catalog status file:
            fNewCatalogContentsFile = new File(fCatalogComicsFolder.getAbsolutePath() + File.separator + "CatalogContents_new.dat");

            if (!fNewCatalogContentsFile.exists()) {
                try {
                    if (fNewCatalogContentsFile.createNewFile()) {
                        FileWriter fwNewCatalogContentsFile = null;
                        try {
                            fwNewCatalogContentsFile = new FileWriter(fNewCatalogContentsFile, true);

                            //Write the activity_comic_details_header line to the file:
                            fwNewCatalogContentsFile.append(GlobalClass.ComicRecordFields[0]);
                            for (int i = 1; i < GlobalClass.ComicRecordFields.length; i++) {
                                fwNewCatalogContentsFile.append("\t");
                                fwNewCatalogContentsFile.append(GlobalClass.ComicRecordFields[i]);
                            }
                            for (String sNewField : sNewFields) {
                                fwNewCatalogContentsFile.append("\t");
                                fwNewCatalogContentsFile.append(sNewField);
                            }
                            fwNewCatalogContentsFile.append("\t");
                            fwNewCatalogContentsFile.append("DataFileVersion." + iToVersion);
                            fwNewCatalogContentsFile.append("\n");


                            sLine = brReader.readLine();
                            while (sLine != null) {
                                fwNewCatalogContentsFile.append(sLine);
                                for (int i = 0; i < sNewFields.length - 1; i++) {
                                    fwNewCatalogContentsFile.append("\t");
                                }
                                fwNewCatalogContentsFile.append("\n");
                                // read next line
                                sLine = brReader.readLine();
                            }
                            brReader.close();

                            fwNewCatalogContentsFile.flush();
                            fwNewCatalogContentsFile.close();

                            File fRenameCurrentDatFile = new File(fCatalogComicsFolder.getAbsolutePath() + File.separator + "CatalogContents_v" + iFromVersion + "_bak.dat");
                            if (!fRenameCurrentDatFile.exists()) {
                                if (!fCatalogContentsFile.renameTo(fRenameCurrentDatFile)) {
                                    Toast.makeText(this, "Could not rename CatalogContentsFile.", Toast.LENGTH_LONG).show();
                                } else {
                                    if (!fNewCatalogContentsFile.renameTo(fCatalogContentsFile)) {
                                        Toast.makeText(this, "Could not rename new CatalogContentsFile.", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }

                        } catch (Exception e) {
                            Toast.makeText(this, "Problem during CatalogContentsFile re-write.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Could not write new CatalogContents.dat at" + fNewCatalogContentsFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "Could not create new CatalogContents.dat at" + fNewCatalogContentsFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e){
            Toast.makeText(this, "Could not open CatalogContents.dat at" + fCatalogContentsFile.getAbsolutePath()+ "\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }











}