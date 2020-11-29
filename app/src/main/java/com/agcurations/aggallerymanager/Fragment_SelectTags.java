package com.agcurations.aggallerymanager;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Fragment_SelectTags extends Fragment {

    private FragmentSelectTagsViewModel mViewModel;

    public static Fragment_SelectTags newInstance() {
        return new Fragment_SelectTags();
    }

    public final static String MEDIA_CATEGORY = "MEDIA_CATEGORY";
    public int iMediaCategory;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_tags_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this.getActivity()).get(FragmentSelectTagsViewModel.class);
        // TODO: Use the ViewModel

        //Get the Media Category argument passed to this fragment:
        Bundle args = getArguments();
        iMediaCategory = args.getInt(MEDIA_CATEGORY, 0);

        if (getView() == null) {
            return;
        }

        if(getActivity() == null){
            return;
        }

        final ListView listView_ImportTagSelection = getView().findViewById(R.id.listView_ImportTagSelection);

        //Get tags to put in the ListView:
        GlobalClass globalClass = (GlobalClass) getActivity().getApplicationContext();
        for (Map.Entry<Integer, String[]> entry : globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()){
            mViewModel.alTagsAll.add(new TagItem(
                    false,
                    Integer.parseInt(entry.getValue()[GlobalClass.TAG_ID_INDEX]),
                    entry.getValue()[GlobalClass.TAG_NAME_INDEX],
                    0));
        }

        // Create the adapter for the ListView, and set the ListView adapter:
        ListViewTagsAdapter listViewTagsAdapter = new ListViewTagsAdapter(getActivity().getApplicationContext(), mViewModel.alTagsAll);
        listView_ImportTagSelection.setAdapter(listViewTagsAdapter);
        listView_ImportTagSelection.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    public class ListViewTagsAdapter extends ArrayAdapter<TagItem> {
        int iOrderIterator = 0;

        public ListViewTagsAdapter(Context context, ArrayList<TagItem> tagItems) {
            super(context, 0, tagItems);
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            // Get the data item for this position
            final TagItem tagItem = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(R.layout.activity_import_listview_tag_item, parent, false);
            }
            // Lookup view for data population
            final CheckedTextView checkedTextView_TagText = (CheckedTextView) v.findViewById(R.id.checkedTextView_TagText);
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
                        checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorBlack));
                    } else {
                        //iOrderIterator--; Never decrease the order iterator, because user may unselect a middle item, thus creating duplicate order nums.
                        tagItem.SelectionOrder = 0; //Remove the index showing the order in which this item was selected.
                        checkedTextView_TagText.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorFragmentImportBackground));
                        checkedTextView_TagText.setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorGrey1));
                    }

                    //Reform the tags string listing all of the selected tags:

                    //Iterate through all of the items in this ArrayAdapter, gathering the items,
                    //  and using a TreeMap to automatically sort the items by selection order:
                    int iItemCount = getCount();
                    TreeMap<Integer, TagItem> tmSelectedItems = new TreeMap<>();
                    for(int i=0; i< iItemCount; i++){
                        if(getItem(i) != null){
                            TagItem tagItem1 = getItem(i);
                            assert tagItem1 != null;
                            if(tagItem1.isChecked) {
                                tmSelectedItems.put(tagItem1.SelectionOrder, tagItem1);
                            }
                        }
                    }

                    //Put the sorted TreeList items into an ArrayList and transfer to the ViewModel:
                    ArrayList<TagItem> alTagItems = new ArrayList<>();

                    for (Map.Entry<Integer, TagItem>
                            entry : tmSelectedItems.entrySet()) {
                        alTagItems.add(entry.getValue());
                    }

                    mViewModel.alTagsSelected.setValue(alTagItems);

                }
            });

            // Return the completed view to render on screen
            return v;
        }
    }

}