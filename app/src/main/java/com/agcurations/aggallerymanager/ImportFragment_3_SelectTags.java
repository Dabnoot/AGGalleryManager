package com.agcurations.aggallerymanager;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImportFragment_3_SelectTags#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImportFragment_3_SelectTags extends Fragment {

    GlobalClass globalClass;
    private ImportActivityViewModel importActivityViewModel;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

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

    private SelectedFileListCustomAdapter selectedFileListCustomAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        globalClass = (GlobalClass) getActivity().getApplicationContext();
        importActivityViewModel = new ViewModelProvider(getActivity()).get(ImportActivityViewModel.class);
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

    @Override
    public void onPause() {
        importActivityViewModel.alfiConfirmedFileImports = selectedFileListCustomAdapter.alFileItems;
        super.onPause();
    }

    public void initComponents() {

        if (getView() == null) {
            return;
        }

        if(getActivity() == null){
            return;
        }

        //Populate the listView:
        final ListView listView_FilesToImport = getView().findViewById(R.id.listView_FilesToImport);
        ArrayList<FileItem> alfi = new ArrayList<>();
        for(FileItem fi: ImportActivity.fileListCustomAdapter.alFileItems){
            if(fi.isChecked){
                alfi.add(fi);
            }
        }
        selectedFileListCustomAdapter = 
                new SelectedFileListCustomAdapter(getActivity(), R.id.listView_FilesToImport, alfi);
        if(listView_FilesToImport != null) {
            listView_FilesToImport.setAdapter(selectedFileListCustomAdapter);
        }



        //Start the tag selection fragment:
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        Fragment_SelectTags fst = new Fragment_SelectTags();
        Bundle args = new Bundle();
        args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, importActivityViewModel.iImportMediaCategory);
        fst.setArguments(args);
        ft.replace(R.id.child_fragment_tag_selector, fst);
        ft.commit();

        //React to changes in the selected tag data in the ViewModel initiated in ImportActivity:
        final Observer<ArrayList<TagItem>> selectedTagsObserver = new Observer<ArrayList<TagItem>>() {
            @Override
            public void onChanged(ArrayList<TagItem> tagItems) {

                //Get the text of the tags and display:
                StringBuilder sb = new StringBuilder();
                if(tagItems.size() > 0) {
                    sb.append(tagItems.get(0).TagText);
                    for (int i = 1; i < tagItems.size(); i++) {
                        sb.append(", ");
                        sb.append(tagItems.get(i).TagText);
                    }
                }
                //Display the tags:
                TextView tv = getView().findViewById(R.id.textView_ImportTags);
                if(tv != null){
                    tv.setText(sb.toString());
                }

                //Apply the selected tags to individual items:
                boolean bUpdateAdapter = false;
                TagItem tiAdded = ImportActivity.viewModelTags.tiTagItemAdded.getValue();
                if(tiAdded != null){
                    selectedFileListCustomAdapter.applyTagToItems(tiAdded.TagID);
                    bUpdateAdapter = true;
                }
                TagItem tiRemoved = ImportActivity.viewModelTags.tiTagItemRemoved.getValue();
                if(tiRemoved != null){
                    selectedFileListCustomAdapter.removeTagFromItems(tiRemoved.TagID);
                    bUpdateAdapter = true;
                }
                if(bUpdateAdapter){
                    selectedFileListCustomAdapter.notifyDataSetChanged();
                }

            }
        };
        ImportActivity.viewModelTags.altiTagsSelected.observe(this, selectedTagsObserver);

    }


    public class SelectedFileListCustomAdapter extends ArrayAdapter<FileItem> {

        final public ArrayList<FileItem> alFileItems;
        
        public SelectedFileListCustomAdapter(@NonNull Context context, int resource, ArrayList<FileItem> alfi) {
            super(context, resource);
            alFileItems = alfi;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View v, @NonNull ViewGroup parent) {

            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                //My custom list item design is here
                row = inflater.inflate(R.layout.listview_fileitem, parent, false);
            }

            ImageView ivFileType =  row.findViewById(R.id.imageView_StorageItemIcon);
            TextView tvLine1 =  row.findViewById(R.id.textView_Line1);
            TextView tvLine2 = row.findViewById(R.id.textView_Line2);
            TextView tvLine3 = row.findViewById(R.id.textView_Line3);

            tvLine1.setText(alFileItems.get(position).name);
            DateFormat dfDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.getDefault() );
            String sLine2 = dfDateFormat.format(alFileItems.get(position).dateLastModified);


            //If type is video or gif, get the duration:
            long durationInMilliseconds = -1L;
            //If mimeType is video or gif, get the duration:
            try {
                if(alFileItems.get(position).videoTimeInMilliseconds == -1L) { //If the time has not already been determined for the video file...
                    if (alFileItems.get(position).mimeType.startsWith("video")) {
                        Uri docUri = Uri.parse(alFileItems.get(position).uri);
                        ImportActivity.mediaMetadataRetriever.setDataSource(getContext(), docUri);
                        String time = ImportActivity.mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        durationInMilliseconds = Long.parseLong(time);
                    } else { //if it's not a video file, check to see if it's a gif:
                        if (alFileItems.get(position).extension.contentEquals("gif")) {
                            //Get the duration of the gif image:
                            Uri docUri = Uri.parse(alFileItems.get(position).uri);
                            Context activityContext = getContext();
                            pl.droidsonroids.gif.GifDrawable gd = new pl.droidsonroids.gif.GifDrawable(activityContext.getContentResolver(), docUri);
                            durationInMilliseconds = gd.getDuration();
                        }
                    }
                    if(durationInMilliseconds != -1L) { //If time is now defined, get the text form of the time:
                        alFileItems.get(position).videoTimeText = ImportActivity.getDurationTextFromMilliseconds(durationInMilliseconds);
                        alFileItems.get(position).videoTimeInMilliseconds = durationInMilliseconds;
                    }
                }

                if(alFileItems.get(position).videoTimeText.length() > 0){
                    //If the video time text has been defined, recall and display the time:
                    sLine2 = sLine2 + "\tDuration: " + alFileItems.get(position).videoTimeText;
                }

            }catch (Exception e){
                Context activityContext = getContext();
                Toast.makeText(activityContext, e.getMessage() + "; File: " + alFileItems.get(position).name, Toast.LENGTH_LONG).show();
            }

            tvLine2.setText(sLine2);

            //Get tag text to apply to list item if tags are assigned to the item:
            StringBuilder sbTags = new StringBuilder();
            sbTags.append("Tags: ");
            ArrayList<Integer> aliTagIDs = alFileItems.get(position).prospectiveTags;

            if(aliTagIDs != null){
                if(aliTagIDs.size() > 0) {
                    sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(0), importActivityViewModel.iImportMediaCategory));
                    for (int i = 1; i < aliTagIDs.size(); i++) {
                        sbTags.append(", ");
                        sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(i), importActivityViewModel.iImportMediaCategory));
                    }
                }
            }
            tvLine3.setText(sbTags.toString());

            //set the image type if folder or file
            if(alFileItems.get(position).type.equals("folder")) {
                ivFileType.setImageResource(R.drawable.baseline_folder_white_18dp);
            } else {
                //ivFileType.setImageResource(R.drawable.baseline_file_white_18dp);

                //Get the Uri of the file and create/display a thumbnail:
                String sUri = alFileItems.get(position).uri;
                Uri uri = Uri.parse(sUri);
                Glide.with(getContext()).
                        load(uri).
                        into(ivFileType);
            }
            
            
            //return super.getView(position, row, parent);
            return row;
        }

        @Override
        public int getCount() {
            return alFileItems.size();
        }

        @Override
        public FileItem getItem(int position) {
            return alFileItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void applyTagToItems(Integer iTagID){
            //Apply tag to items if the item does not already have the tag:
            for(FileItem fm: alFileItems){
                if(!fm.prospectiveTags.contains(iTagID)){
                    fm.prospectiveTags.add(iTagID);
                }
            }
        }

        public void removeTagFromItems(Integer iTagID){
            //Remove tag from items if the item has the tag:
            for(FileItem fm: alFileItems){
                fm.prospectiveTags.remove(iTagID);
            }
        }
    }



}