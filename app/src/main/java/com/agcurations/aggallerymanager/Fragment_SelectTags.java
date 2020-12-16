package com.agcurations.aggallerymanager;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Fragment_SelectTags extends Fragment {

    GlobalClass globalClass;

    private ViewModel_Fragment_SelectTags mViewModel;

    public static Fragment_SelectTags newInstance() {
        return new Fragment_SelectTags();
    }

    public final static String MEDIA_CATEGORY = "MEDIA_CATEGORY";
    public final static String PRESELECTED_TAG_ITEMS = "PRESELECTED_TAG_ITEMS";

    private ArrayList<Integer> galiPreselectedTags;

    public final static int RESULT_CODE_TAGS_MODIFIED = 202;

    private TreeMap<String, String[]> tmOneTimeTagsInUse; //Gather tags in use only once.
    // This is require for when the user switches between tabs. We will transfer selected tags here
    //  in addition to globally-used tags so that the user's choices are not wiped out when switching tabs.

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_tags, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(getActivity() == null) return;
        mViewModel = new ViewModelProvider(getActivity()).get(ViewModel_Fragment_SelectTags.class);

        globalClass = (GlobalClass) getActivity().getApplicationContext();

        //Get the Media Category argument passed to this fragment:
        Bundle args = getArguments();

        if(args == null) {
            mViewModel.iMediaCategory = 0;
            galiPreselectedTags = new ArrayList<>();
        } else {
            mViewModel.iMediaCategory = args.getInt(MEDIA_CATEGORY, 0);
            galiPreselectedTags = args.getIntegerArrayList(PRESELECTED_TAG_ITEMS);
        }

        if (getView() == null) {
            return;
        }

        if(getActivity() == null){
            return;
        }

        //Configure the tabLayout to change the ListView:
        TabLayout tabLayout_TagListings = getView().findViewById(R.id.tabLayout_TagListings);
        tabLayout_TagListings.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewModel.iTabLayoutListingSelection = tab.getPosition();
                initListViewData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        tmOneTimeTagsInUse = globalClass.GetTagsInUse(mViewModel.iMediaCategory);
        if(tmOneTimeTagsInUse.size() == 0){
            TabLayout.Tab tab = tabLayout_TagListings.getTabAt(1);
            if(tab != null) {
                tab.select();
            }
        }

        initListViewData();

        //Configure the button to start the tag editor:
        Button button_TagEditor = getView().findViewById(R.id.button_TagEditor);
        button_TagEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentTagEditor = new Intent(getActivity(), Activity_TagEditor.class);
                intentTagEditor.putExtra(Activity_TagEditor.EXTRA_INT_MEDIA_CATEGORY, mViewModel.iMediaCategory);
                startActivityForResult(intentTagEditor, RESULT_CODE_TAGS_MODIFIED);
            }
        });

    }

    private void initListViewData(){
        final ListView listView_ImportTagSelection = getView().findViewById(R.id.listView_ImportTagSelection);

        //Get tags to put in the ListView:
        int iPreSelectedTagsCount = 0;
        int iPreSelectedTagIterator = 0;
        int iSelectionOrder;

        mViewModel.alTagsAll.clear();

        TreeMap<String, String[]> tmTagPool = globalClass.gtmCatalogTagReferenceLists.get(mViewModel.iMediaCategory);

        if(mViewModel.iTabLayoutListingSelection == 0) {
            //Show only tags which are in-use.
            tmTagPool = tmOneTimeTagsInUse;
        }

        //Go through the tags treeMap and put the ListView together:
        for (Map.Entry<String, String[]> tmEntryTagReferenceItem : tmTagPool.entrySet()) {

            //Check to see if the list of preselected tags includes this tag from the reference list.
            //  If so, set the item as "checked":
            boolean bIsChecked = false;
            iSelectionOrder = 0;
            if (galiPreselectedTags != null) {
                iPreSelectedTagsCount = galiPreselectedTags.size();
                int iReferenceTagID = Integer.parseInt(tmEntryTagReferenceItem.getValue()[GlobalClass.TAG_ID_INDEX]);
                for (int iPreSelectedTagID : galiPreselectedTags) {
                    if (iReferenceTagID == iPreSelectedTagID) {
                        bIsChecked = true;
                        iPreSelectedTagIterator++;
                        iSelectionOrder = iPreSelectedTagIterator;
                    }
                }
            }

            ItemClass_Tag tiNew = new ItemClass_Tag(
                    bIsChecked,
                    Integer.parseInt(tmEntryTagReferenceItem.getValue()[GlobalClass.TAG_ID_INDEX]),
                    tmEntryTagReferenceItem.getValue()[GlobalClass.TAG_NAME_INDEX],
                    iSelectionOrder);


            mViewModel.alTagsAll.add(tiNew);
        }

        // Create the adapter for the ListView, and set the ListView adapter:
        ListViewTagsAdapter listViewTagsAdapter = new ListViewTagsAdapter(getActivity().getApplicationContext(), mViewModel.alTagsAll, iPreSelectedTagsCount);
        listView_ImportTagSelection.setAdapter(listViewTagsAdapter);
        listView_ImportTagSelection.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getActivity();
        if(requestCode == RESULT_CODE_TAGS_MODIFIED && resultCode == Activity.RESULT_OK) {
            //If we are coming back from the tag editor, rebuilt the listview contents:
            initListViewData();
        }
    }


    public class ListViewTagsAdapter extends ArrayAdapter<ItemClass_Tag> {
        int iOrderIterator;

        public ListViewTagsAdapter(Context context, ArrayList<ItemClass_Tag> tagItems, int iPreselectedCount) {
            super(context, 0, tagItems);
            iOrderIterator = iPreselectedCount;
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            // Get the data item for this position
            final ItemClass_Tag tagItem = getItem(position);
            if(tagItem == null){
                return v;
            }
            // Check if an existing view is being reused, otherwise inflate the view
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(R.layout.activity_import_listview_tag_item, parent, false);
            }
            // Lookup view for data population
            final CheckedTextView checkedTextView_TagText = v.findViewById(R.id.checkedTextView_TagText);
            // Populate the data into the template view using the data object
            String s = tagItem.TagText;
            checkedTextView_TagText.setText(s);


            //Set the selection state (needed as views are recycled).
            if(tagItem.isChecked){
                checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentImportBackgroundHighlight2));
                checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBlack));
            } else {
                checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentImportBackground));
                checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorGrey1));
            }

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Handle changing the checked state:
                    tagItem.isChecked = !tagItem.isChecked;
                    if(tagItem.isChecked){
                        iOrderIterator++;
                        tagItem.SelectionOrder = iOrderIterator; //Set the index for the order in which this item was selected.
                        checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentImportBackgroundHighlight2));
                        checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorGrey1));
                        if(galiPreselectedTags == null){
                            galiPreselectedTags = new ArrayList<>();
                        }
                        galiPreselectedTags.add(tagItem.TagID);
                    } else {
                        //iOrderIterator--; Never decrease the order iterator, because user may unselect a middle item, thus creating duplicate order nums.
                        tagItem.SelectionOrder = 0; //Remove the index showing the order in which this item was selected.
                        checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentImportBackground));
                        checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorGrey1));
                        if(galiPreselectedTags == null){
                            //Not a likely case, but just in case.
                            galiPreselectedTags = new ArrayList<>();
                        }
                        for(int i = 0; i < galiPreselectedTags.size(); i++) {
                            if(galiPreselectedTags.get(i) == tagItem.TagID){
                                galiPreselectedTags.remove(i);
                            }
                        }
                    }

                    //Reform the tags string listing all of the selected tags:

                    //Iterate through all of the items in this ArrayAdapter, gathering the items,
                    //  and using a TreeMap to automatically sort the items by selection order:
                    int iItemCount = getCount();
                    TreeMap<Integer, ItemClass_Tag> tmSelectedItems = new TreeMap<>();
                    for(int i=0; i< iItemCount; i++){
                        if(getItem(i) != null){
                            ItemClass_Tag tagItem1 = getItem(i);
                            assert tagItem1 != null;
                            if(tagItem1.isChecked) {
                                tmSelectedItems.put(tagItem1.SelectionOrder, tagItem1);
                            }
                        }
                    }

                    //Put the sorted TreeList items into an ArrayList and transfer to the ViewModel:
                    ArrayList<ItemClass_Tag> alTagItems = new ArrayList<>();

                    for (Map.Entry<Integer, ItemClass_Tag>
                            entry : tmSelectedItems.entrySet()) {
                        alTagItems.add(entry.getValue());
                    }

                    mViewModel.setSelectedTags(alTagItems);

                }
            });


            // Return the completed view to render on screen
            return v;
        }
    }

}