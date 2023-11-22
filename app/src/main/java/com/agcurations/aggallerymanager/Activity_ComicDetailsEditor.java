package com.agcurations.aggallerymanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class Activity_ComicDetailsEditor extends AppCompatActivity {

    private ItemClass_CatalogItem gciCatalogItem;

    public static final String EXTRA_COMIC_CATALOG_ITEM = "com.agcurations.aggallerymanager.extra.COMIC_CATALOG_ITEM";

    private EditText editText_ComicTitle;
    private EditText editText_ComicSource;
    private EditText editText_Parodies;
    private EditText editText_Characters;
    private TextView textView_Tags;
    private EditText editText_Artists;
    private EditText editText_Groups;
    private EditText editText_Languages;
    private EditText editText_Categories;

    private GlobalClass globalClass;

    private Fragment_SelectTags gFragment_selectTags;

    private String gsNewTagIDs;

    private DrawerLayout gDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_details_editor);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        globalClass = (GlobalClass) getApplicationContext();

        editText_ComicTitle = findViewById(R.id.editText_ComicTitle);
        editText_ComicSource = findViewById(R.id.editText_ComicSource);
        editText_Parodies = findViewById(R.id.editText_Parodies);
        editText_Characters = findViewById(R.id.editText_Characters);
        textView_Tags = findViewById(R.id.textView_Tags);
        editText_Artists = findViewById(R.id.editText_Artists);
        editText_Groups = findViewById(R.id.editText_Groups);
        editText_Languages = findViewById(R.id.editText_Languages);
        editText_Categories = findViewById(R.id.editText_Categories);

        Intent intent = getIntent();
        gciCatalogItem = (ItemClass_CatalogItem) intent.getSerializableExtra(EXTRA_COMIC_CATALOG_ITEM);

        if( gciCatalogItem == null) {
            finish();
        }

        Button button_Cancel = findViewById(R.id.button_Cancel);
        button_Cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button button_Save = findViewById(R.id.button_Save);
        button_Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gciCatalogItem.sTitle = editText_ComicTitle.getText().toString();
                gciCatalogItem.sSource = editText_ComicSource.getText().toString();
                gciCatalogItem.sComicParodies = editText_Parodies.getText().toString();
                gciCatalogItem.sComicCharacters = editText_Characters.getText().toString();

                //Get tags and record to catalog item:
                if(!gciCatalogItem.sTags.equals(gsNewTagIDs)) {
                    gciCatalogItem.sTags = gsNewTagIDs;
                    gciCatalogItem.aliTags = GlobalClass.getTagIDsFromTagIDString(gsNewTagIDs);
                    //Recalc maturity rating and approved users for the item:
                    gciCatalogItem.iMaturityRating = GlobalClass.getHighestTagMaturityRating(gciCatalogItem.aliTags, gciCatalogItem.iMediaCategory);
                    gciCatalogItem.alsApprovedUsers = GlobalClass.getApprovedUsersForTagGrouping(gciCatalogItem.aliTags, gciCatalogItem.iMediaCategory);
                    //Inform program of a need to update the tags histogram:
                    globalClass.gbTagHistogramRequiresUpdate[gciCatalogItem.iMediaCategory] = true;
                }


                gciCatalogItem.sComicArtists = editText_Artists.getText().toString();
                gciCatalogItem.sComicGroups = editText_Groups.getText().toString();
                gciCatalogItem.sComicLanguages = editText_Languages.getText().toString();
                gciCatalogItem.sComicCategories = editText_Categories.getText().toString();


                ItemClass_CatalogItem icci_validated = GlobalClass.validateCatalogItemData(gciCatalogItem);

                //Update catalog item in memory and in storage:
                Toast.makeText(getApplicationContext(), "Saving catalog record...", Toast.LENGTH_SHORT).show();
                globalClass.CatalogDataFile_UpdateRecord(icci_validated);
                Toast.makeText(getApplicationContext(), "Catalog record saved.", Toast.LENGTH_SHORT).show();

                //Tell Activity_CatalogViewer to refresh its view:
                globalClass.gbCatalogViewerRefresh = true;

                finish();
            }
        });







        //Get tags for the item:
        ArrayList<Integer> aliTags = GlobalClass.getIntegerArrayFromString(gciCatalogItem.sTags, ",");

        //Instantiate the ViewModel tracking tag data from the tag selector fragment:
        ViewModel_Fragment_SelectTags viewModel_fragment_selectTags = new ViewModelProvider(this).get(ViewModel_Fragment_SelectTags.class);

        viewModel_fragment_selectTags.altiTagsSelected.removeObservers(this);

        ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();
        for(int i = 0; i < aliTags.size(); i++){
            alTagItems.add(i, new ItemClass_Tag(aliTags.get(i), globalClass.getTagTextFromID(aliTags.get(i), gciCatalogItem.iMediaCategory)));
        }
        viewModel_fragment_selectTags.altiTagsSelected.setValue(alTagItems);

        //Populate the TagSelector fragment:
        if(gFragment_selectTags == null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            gFragment_selectTags = new Fragment_SelectTags();

            Bundle args = new Bundle();
            args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, aliTags);
            args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, GlobalClass.MEDIA_CATEGORY_COMICS);
            gFragment_selectTags.setArguments(args);
            fragmentTransaction.replace(R.id.frameLayout_TagSelector, gFragment_selectTags);
            fragmentTransaction.commit();
        }


        //React to changes in the selected tag data in the ViewModel:
        final Observer<ArrayList<ItemClass_Tag>> observerSelectedTags = new Observer<ArrayList<ItemClass_Tag>>() {
            @Override
            public void onChanged(ArrayList<ItemClass_Tag> tagItems) {

                //Get the text of the tags and display:
                StringBuilder sb = new StringBuilder();
                //sb.append("Tags: ");
                if(tagItems.size() > 0) {
                    sb.append(tagItems.get(0).sTagText);
                    for (int i = 1; i < tagItems.size(); i++) {
                        sb.append(", ");
                        sb.append(tagItems.get(i).sTagText);
                    }
                }
                TextView textView_Tags = findViewById(R.id.textView_Tags);
                if(textView_Tags != null){
                    textView_Tags.setText(sb.toString());
                }

                //Get the tag IDs:
                ArrayList<Integer> aliTagIDs = new ArrayList<>();
                for(ItemClass_Tag ti : tagItems){
                    aliTagIDs.add(ti.iTagID);
                }

                gsNewTagIDs = GlobalClass.formDelimitedString(aliTagIDs,",");
                /*if(!gsNewTagIDs.equals(gsPreviousTagIDs)) {
                    //Enable the Save button:
                    enableSave();
                }*/
            }
        };

        viewModel_fragment_selectTags.altiTagsSelected.observe(this, observerSelectedTags);

        gDrawerLayout = findViewById(R.id.drawer_layout);

        ImageView imageView_EditTags = findViewById(R.id.imageView_EditTags);
        imageView_EditTags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gDrawerLayout.openDrawer(GravityCompat.END);
            }
        });







    }

    @Override
    protected void onResume() {
        super.onResume();

        editText_ComicTitle.setText(gciCatalogItem.sTitle);
        editText_ComicSource.setText(gciCatalogItem.sSource);
        editText_Parodies.setText(gciCatalogItem.sComicParodies);
        editText_Characters.setText(gciCatalogItem.sComicCharacters);

        String sTagText = globalClass.getTagTextsFromTagIDsString(gciCatalogItem.sTags, gciCatalogItem.iMediaCategory);
        textView_Tags.setText(sTagText);

        editText_Artists.setText(gciCatalogItem.sComicArtists);
        editText_Groups.setText(gciCatalogItem.sComicGroups);
        editText_Languages.setText(gciCatalogItem.sComicLanguages);
        editText_Categories.setText(gciCatalogItem.sComicCategories);



    }
}