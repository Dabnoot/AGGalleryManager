package com.agcurations.aggallerymanager;

import android.content.Context;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImportFragment_3_SelectTags#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImportFragment_3_SelectTags extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ImportFragment_3_SelectTags() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentImport_3_VideoApplyTags.
     */
    // TODO: Rename and change types and number of parameters
    public static ImportFragment_3_SelectTags newInstance(String param1, String param2) {
        ImportFragment_3_SelectTags fragment = new ImportFragment_3_SelectTags();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.import_fragment_3_select_tags, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        initComponents();
    }

    public void initComponents() {

        if (getView() == null) {
            return;
        }

        if(getActivity() == null){
            return;
        }

        final ListView listView_ImportTagSelection = getView().findViewById(R.id.listView_ImportTagSelection);

        // Construct the data source
        ArrayList<TagItem> alTagItems = new ArrayList<TagItem>();

        for(String s: ImportActivity.sDefaultTags){
            alTagItems.add(new TagItem(false, s, 0));
        }

        // Create the adapter to convert the array to views
        ListViewTagsAdapter listViewTagsAdapter = new ListViewTagsAdapter(getActivity().getApplicationContext(), alTagItems);

        listView_ImportTagSelection.setAdapter(listViewTagsAdapter);
        listView_ImportTagSelection.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);


    }


    //============================================
    //===  ListView Adapter ======================
    //============================================

    public class TagItem {
        public Boolean isChecked;
        public String Text;

        //Create a variable to be used to preserve the order in which items are selected.
        //  This is needed because the first tag will be used to select which folder to put
        //  the items in for storage purposes. Similar to first ingredient in ingredient lists.
        public int SelectionOrder;

        public TagItem(Boolean _isChecked, String _Text, int _SelectionOrder) {
            this.isChecked = _isChecked;
            this.Text = _Text;
            this.SelectionOrder = _SelectionOrder;
        }
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
            String s = tagItem.Text;
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
                    TextView tvTags = v.getRootView().findViewById(R.id.textView_ImportTags);

                    //Display the selected data:
                    //Reform the tags string listing all of the selected tags:
                    int iItemCount = getCount();
                    //Use a TreeMap to automatically sort the items:
                    TreeMap<Integer, String> tmSelectedItems = new TreeMap<>();
                    for(int i=0; i< iItemCount; i++){
                        if(getItem(i) != null){
                            TagItem ti = getItem(i);
                            assert ti != null;
                            if(ti.isChecked) {
                                tmSelectedItems.put(ti.SelectionOrder, ti.Text);
                            }
                        }
                    }
                    //Transfer the treemap items back to ImportActivity as well as to a string for display:
                    StringBuilder sb = new StringBuilder();
                    ImportActivity.alsImportTags.clear();
                    for (Map.Entry<Integer, String>
                            entry : tmSelectedItems.entrySet()) {
                        ImportActivity.alsImportTags.add(entry.getValue());
                        sb.append(entry.getValue());
                        sb.append(", ");
                    }

                    String s = sb.toString();
                    if(s.length() >= 2){
                        s = s.substring(0, s.length() - 2);
                    }

                    tvTags.setText(s);
                }
            });

            // Return the completed view to render on screen
            return v;
        }
    }





}