package com.agcurations.aggallerymanager;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.TreeMap;

public class ComicDetailsActivity extends AppCompatActivity {
    //Global constants
    public static final String COMIC_FIELDS_STRING = "COMIC_FIELDS_STRING";

    //Global Variables:

    private GlobalClass globalClass;

    private String[] gsComicFields;
    private TreeMap<Integer, String> tmComicPages;
    private String gsComicName = "";
    private String gsComicFolder_AbsolutePath; //TODO

    private ImageView ivComicCoverPage;
    private TextView gtvComicTitle;
    private TextView gtvComicID;
    private TextView gtvParodies;
    private TextView gtvCharacters;
    private TextView gtvTags;
    private TextView gtvArtists;
    private TextView gtvGroups;
    private TextView gtvLanguages;
    private TextView gtvCategories;
    private TextView gtvPages;

    private TextView gtvLabelComicID;
    private TextView gtvLabelParodies;
    private TextView gtvLabelCharacters;
    private TextView gtvLabelTags;
    private TextView gtvLabelArtists;
    private TextView gtvLabelGroups;
    private TextView gtvLabelLanguages;
    private TextView gtvLabelCategories;
    private TextView gtvLabelPages;









    private File fComicCoverPage;

    private RecyclerView.Adapter<ComicDetailsActivity.RecyclerViewComicPagesAdapter.ViewHolder> gRecyclerViewComicPagesAdapter;

    private boolean gbDebugTouch = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_details);

        getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorDarkDarkOrange));
        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.colorDarkDarkOrange)));
        this.getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.startup_screen_background));


        // Calling Application class (see application tag in AndroidManifest.xml)
        globalClass = (GlobalClass) getApplicationContext();



        //Get the intent used to start this activity:
        Intent intentCaller = getIntent();
        //Get data from the intent:
        gsComicFields = intentCaller.getStringArrayExtra(COMIC_FIELDS_STRING);

        if( gsComicFields == null) return;
        gsComicName = gsComicFields[GlobalClass.COMIC_NAME_INDEX];

        gsComicFolder_AbsolutePath = globalClass.getCatalogComicsFolder().getAbsolutePath();
        String sComicFolderPath;
        sComicFolderPath = gsComicFolder_AbsolutePath + File.separator
                + gsComicFields[GlobalClass.COMIC_FOLDER_NAME_INDEX];

        //Load the full path to each comic page into tmComicPages:
        File fComicFolder = new File(sComicFolderPath);
        tmComicPages = new TreeMap<>();
        if(fComicFolder.exists()){
            File[] fComicPages = fComicFolder.listFiles();
            if(fComicPages != null) {
                for (int i = 0; i < fComicPages.length; i++) {
                    tmComicPages.put(i, fComicPages[i].getAbsolutePath());
                    if (fComicCoverPage == null){
                        fComicCoverPage = new File(fComicPages[i].getAbsolutePath());
                    }
                }
            }
        }


        ivComicCoverPage = findViewById(R.id.imageView_ComicCoverPage);
        if (fComicCoverPage.exists()) {
            Glide.with(getApplicationContext()).load(fComicCoverPage).into(ivComicCoverPage);
        }

        gtvComicTitle = findViewById(R.id.textView_ComicTitle);
        gtvComicID = findViewById(R.id.textView_ComicID);
        gtvParodies = findViewById(R.id.textView_Parodies);
        gtvCharacters = findViewById(R.id.textView_Characters);
        gtvTags = findViewById(R.id.textView_Tags);
        gtvArtists = findViewById(R.id.textView_Artists);
        gtvGroups = findViewById(R.id.textView_Groups);
        gtvLanguages = findViewById(R.id.textView_Languages);
        gtvCategories = findViewById(R.id.textView_Categories);
        gtvPages = findViewById(R.id.textView_Pages);

        gtvLabelComicID = findViewById(R.id.textView_LabelComicID);
        gtvLabelParodies  = findViewById(R.id.textView_LabelParodies);
        gtvLabelCharacters  = findViewById(R.id.textView_LabelCharacters);
        gtvLabelTags = findViewById(R.id.textView_LabelTags);
        gtvLabelArtists = findViewById(R.id.textView_LabelArtists);
        gtvLabelGroups  = findViewById(R.id.textView_LabelGroups);
        gtvLabelLanguages = findViewById(R.id.textView_LabelLanguages);
        gtvLabelCategories = findViewById(R.id.textView_LabelCategories);
        gtvLabelPages = findViewById(R.id.textView_LabelPages);

        gtvComicTitle.setText(gsComicFields[GlobalClass.COMIC_NAME_INDEX]);
        gtvComicID.setText(gsComicFields[GlobalClass.COMIC_ID_INDEX]);
        gtvParodies.setText(gsComicFields[GlobalClass.COMIC_PARODIES_INDEX]);
        gtvCharacters.setText(gsComicFields[GlobalClass.COMIC_CHARACTERS_INDEX]);
        gtvTags.setText(gsComicFields[GlobalClass.COMIC_TAGS_INDEX]);
        gtvArtists.setText(gsComicFields[GlobalClass.COMIC_ARTISTS_INDEX]);
        gtvGroups.setText(gsComicFields[GlobalClass.COMIC_GROUPS_INDEX]);
        gtvLanguages.setText(gsComicFields[GlobalClass.COMIC_LANGUAGES_INDEX]);
        gtvCategories.setText(gsComicFields[GlobalClass.COMIC_CATEGORIES_INDEX]);
        gtvPages.setText(gsComicFields[GlobalClass.COMIC_PAGES_INDEX]);

        populate_RecyclerViewComicPages();

        if(globalClass.ObfuscationOn) {
            Obfuscate();
        } else {
            RemoveObfuscation();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comic_pages_menu, menu);
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

            default:
                return super.onOptionsItemSelected(item);
        }
    }

//=====================================================================================
    //===== RecyclerView Code =================================================================
    //=====================================================================================

    public void populate_RecyclerViewComicPages(){

        RecyclerView recyclerView = findViewById(R.id.RecyclerView_ComicPages);
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


        gRecyclerViewComicPagesAdapter = new ComicDetailsActivity.RecyclerViewComicPagesAdapter(tmComicPages);
        recyclerView.setAdapter(gRecyclerViewComicPagesAdapter);


    }


    public class RecyclerViewComicPagesAdapter extends RecyclerView.Adapter<ComicDetailsActivity.RecyclerViewComicPagesAdapter.ViewHolder> {

        private final TreeMap<Integer, String> treeMap;
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

        public RecyclerViewComicPagesAdapter(TreeMap<Integer, String> data) {
            this.treeMap = data;
            mapKeys = treeMap.keySet().toArray(new Integer[getCount()]);
        }

        public int getCount() {
            return treeMap.size();
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public ComicDetailsActivity.RecyclerViewComicPagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
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

            return new ComicDetailsActivity.RecyclerViewComicPagesAdapter.ViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ComicDetailsActivity.RecyclerViewComicPagesAdapter.ViewHolder holder, final int position) {
            // - get element from your data set at this position
            // - replace the contents of the view with that element


            if(globalClass.ObfuscationOn) {

                //Get the obfuscation image index:
                int i = (position % globalClass.getObfuscationImageCount());
                //Get the obfuscation image resource ID:
                int iObfuscatorResourceID = globalClass.getObfuscationImage(i);

                Bitmap bmObfuscator = BitmapFactory.decodeResource(getResources(), iObfuscatorResourceID);
                holder.ivThumbnail.setImageBitmap(bmObfuscator);
                holder.tvComicName.setText(globalClass.getObfuscationImageText(i));
            } else {

                //Load the non-obfuscated image into the RecyclerView ViewHolder:
                String sThumbnailFilePath = tmComicPages.get(position);
                File fThumbnail = new File(sThumbnailFilePath);

                if (fThumbnail.exists()) {
                    Glide.with(getApplicationContext()).load(fThumbnail).into(holder.ivThumbnail);
                }

                String s = String.format("Page %d", position + 1);
                holder.tvComicName.setText(s);

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

    public void StartComicViewerActivity(int iComicPage){//TODO
        Intent intentComicViewer = new Intent(this, ComicPageViewerActivity.class);

        intentComicViewer.putExtra(ComicPageViewerActivity.COMIC_FIELDS_STRING,gsComicFields);
        intentComicViewer.putExtra(ComicPageViewerActivity.COMIC_PAGE_START,iComicPage);

        //Record the COMIC_DATETIME_LAST_READ_BY_USER:
        String sTest = globalClass.GetTimeStampFileSafe(); //TODO
        Double dTimeStamp = globalClass.GetTimeStampFloat();
        String[] sDateTime = new String[]{dTimeStamp.toString()};
        int[] iFields = new int[]{GlobalClass.COMIC_DATETIME_LAST_READ_BY_USER};
        globalClass.CatalogDataFile_UpdateRecord(
                gsComicFields[GlobalClass.COMIC_ID_INDEX],
                iFields,
                sDateTime);

        startActivity(intentComicViewer);
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
        setTitle(globalClass.getObfuscationCategoryName());

        gtvComicTitle.setVisibility(View.INVISIBLE);
        gtvComicID.setVisibility(View.INVISIBLE);
        gtvParodies.setVisibility(View.INVISIBLE);
        gtvCharacters.setVisibility(View.INVISIBLE);
        gtvTags.setVisibility(View.INVISIBLE);
        gtvArtists.setVisibility(View.INVISIBLE);
        gtvGroups.setVisibility(View.INVISIBLE);
        gtvLanguages.setVisibility(View.INVISIBLE);
        gtvCategories.setVisibility(View.INVISIBLE);
        gtvPages.setVisibility(View.INVISIBLE);

        gtvLabelComicID.setVisibility(View.INVISIBLE);
        gtvLabelParodies.setVisibility(View.INVISIBLE);
        gtvLabelCharacters.setVisibility(View.INVISIBLE);
        gtvLabelTags.setVisibility(View.INVISIBLE);
        gtvLabelArtists.setVisibility(View.INVISIBLE);
        gtvLabelGroups.setVisibility(View.INVISIBLE);
        gtvLabelLanguages.setVisibility(View.INVISIBLE);
        gtvLabelCategories.setVisibility(View.INVISIBLE);
        gtvLabelPages.setVisibility(View.INVISIBLE);

        //Hide the cover page:
        int iObfuscatorResourceID = globalClass.getObfuscationImage(0);
        Bitmap bmObfuscator = BitmapFactory.decodeResource(getResources(), iObfuscatorResourceID);
        Glide.with(getApplicationContext()).load(bmObfuscator).into(ivComicCoverPage);
        //tvComicName.setText(globalClass.getObfuscationImageText(i));

        //Update the RecyclerView:
        gRecyclerViewComicPagesAdapter.notifyDataSetChanged();
    }

    public void RemoveObfuscation(){
        //Remove obfuscation:
        setTitle(gsComicName);

        gtvComicTitle.setVisibility(View.VISIBLE);
        gtvComicID.setVisibility(View.VISIBLE);
        gtvParodies.setVisibility(View.VISIBLE);
        gtvTags.setVisibility(View.VISIBLE);
        gtvArtists.setVisibility(View.VISIBLE);
        gtvLanguages.setVisibility(View.VISIBLE);
        gtvCategories.setVisibility(View.VISIBLE);
        gtvPages.setVisibility(View.VISIBLE);

        gtvLabelComicID.setVisibility(View.VISIBLE);
        gtvLabelParodies .setVisibility(View.VISIBLE);
        gtvLabelTags.setVisibility(View.VISIBLE);
        gtvLabelArtists.setVisibility(View.VISIBLE);
        gtvLabelLanguages.setVisibility(View.VISIBLE);
        gtvLabelCategories.setVisibility(View.VISIBLE);
        gtvLabelPages.setVisibility(View.VISIBLE);

        //Show the cover page:
        if (fComicCoverPage.exists()) {
            Glide.with(getApplicationContext()).load(fComicCoverPage).into(ivComicCoverPage);
        }

        //Update the RecyclerView:
        gRecyclerViewComicPagesAdapter.notifyDataSetChanged();
    }



}