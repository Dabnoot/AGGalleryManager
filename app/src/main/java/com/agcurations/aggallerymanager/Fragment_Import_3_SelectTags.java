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
 * Use the {@link Fragment_Import_3_SelectTags#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_Import_3_SelectTags extends Fragment {

    GlobalClass globalClass;
    private ViewModel_ImportActivity viewModelImportActivity;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public Fragment_Import_3_SelectTags() {
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
    public static Fragment_Import_3_SelectTags newInstance(String param1, String param2) {
        Fragment_Import_3_SelectTags fragment = new Fragment_Import_3_SelectTags();
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

        if (getActivity() == null) {
            return;
        }
        globalClass = (GlobalClass) getActivity().getApplicationContext();
        viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_3_select_tags, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        initComponents();
    }

    @Override
    public void onPause() {
        viewModelImportActivity.alfiConfirmedFileImports = selectedFileListCustomAdapter.alFileItems;
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
        ArrayList<ItemClass_File> alfi = new ArrayList<>();
        for(ItemClass_File fi: Activity_Import.fileListCustomAdapter.alFileItems){
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
        args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, viewModelImportActivity.iImportMediaCategory);
        fst.setArguments(args);
        ft.replace(R.id.child_fragment_tag_selector, fst);
        ft.commit();

        //React to changes in the selected tag data in the ViewModel initiated in ImportActivity:
        final Observer<ArrayList<ItemClass_Tag>> selectedTagsObserver = new Observer<ArrayList<ItemClass_Tag>>() {
            @Override
            public void onChanged(ArrayList<ItemClass_Tag> tagItems) {

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
                if(getView() != null) {
                    TextView tv = getView().findViewById(R.id.textView_ImportTags);
                    if (tv != null) {
                        tv.setText(sb.toString());
                    }
                }

                //Apply the selected tags to individual items:
                boolean bUpdateAdapter = false;
                ItemClass_Tag tiAdded = Activity_Import.viewModelTags.tiTagItemAdded.getValue();
                if(tiAdded != null){
                    selectedFileListCustomAdapter.applyTagToItems(tiAdded.TagID);
                    bUpdateAdapter = true;
                }
                ItemClass_Tag tiRemoved = Activity_Import.viewModelTags.tiTagItemRemoved.getValue();
                if(tiRemoved != null){
                    selectedFileListCustomAdapter.removeTagFromItems(tiRemoved.TagID);
                    bUpdateAdapter = true;
                }
                if(bUpdateAdapter){
                    selectedFileListCustomAdapter.notifyDataSetChanged();
                }

            }
        };
        Activity_Import.viewModelTags.altiTagsSelected.observe(this, selectedTagsObserver);

    }


    public class SelectedFileListCustomAdapter extends ArrayAdapter<ItemClass_File> {

        final public ArrayList<ItemClass_File> alFileItems;
        private ArrayList<ItemClass_File> alFileItemsDisplay;
        
        public SelectedFileListCustomAdapter(@NonNull Context context, int resource, ArrayList<ItemClass_File> alfi) {
            super(context, resource);
            alFileItems = new ArrayList<>(alfi);

            if((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) &&
                    (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_NH_COMIC_DOWNLOADER)) {
                //If importing comics and importing NHComicDownloader files as the source, filter on the cover pages:
                alFileItemsDisplay = new ArrayList<>(); //initialize.
                applyFilter("^\\d{5,6}_Cover.+");
            } else {
                alFileItemsDisplay = new ArrayList<>(alfi);
            }

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
            TextView tvLine1 =  row.findViewById(R.id.textView_Line1);  //For Name
            TextView tvLine2 = row.findViewById(R.id.textView_Line2);  //For item details
            TextView tvLine3 = row.findViewById(R.id.textView_Line3);  //For Tags


            String sLine1 = "";  //For Name
            if((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) ||
                    (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES)){
                sLine1 = alFileItemsDisplay.get(position).name;
            } else {
                //If import type is comic...
                if(viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_NH_COMIC_DOWNLOADER){
                    //Get the title of the comic from the file name:
                    sLine1 = Service_Import.GetNHComicNameFromCoverFile(alFileItemsDisplay.get(position).name);
                }
            }
            tvLine1.setText(sLine1);


            String sLine2 = "";//For item details, including video duration, file size, comic page count, comic page file set size.
            /*DateFormat dfDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.getDefault() );
            sLine2 = sLine2 + dfDateFormat.format(alFileItemsDisplay.get(position).dateLastModified);*/
            String sRequiredStorageSize = "Size: " + GlobalClass.CleanStorageSize(alFileItemsDisplay.get(position).sizeBytes, GlobalClass.STORAGE_SIZE_NO_PREFERENCE);

            if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                //If type is video or gif, get the duration:
                String sDuration = "";
                long durationInMilliseconds = -1L;
                //If mimeType is video or gif, get the duration:
                try {
                    if (alFileItemsDisplay.get(position).videoTimeInMilliseconds == -1L) { //If the time has not already been determined for the video file...
                        if (alFileItemsDisplay.get(position).mimeType.startsWith("video") ||
                                (alFileItemsDisplay.get(position).mimeType.equals("application/octet-stream") &&
                                        alFileItemsDisplay.get(position).extension.equals(".mp4"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid){
                            Uri docUri = Uri.parse(alFileItemsDisplay.get(position).uri);
                            Activity_Import.mediaMetadataRetriever.setDataSource(getContext(), docUri);
                            String time = Activity_Import.mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            durationInMilliseconds = Long.parseLong(time);
                        } else { //if it's not a video file, check to see if it's a gif:
                            if (alFileItemsDisplay.get(position).extension.contentEquals(".gif")) {
                                //Get the duration of the gif image:
                                Uri docUri = Uri.parse(alFileItemsDisplay.get(position).uri);
                                Context activityContext = getContext();
                                pl.droidsonroids.gif.GifDrawable gd = new pl.droidsonroids.gif.GifDrawable(activityContext.getContentResolver(), docUri);
                                durationInMilliseconds = gd.getDuration();
                            }
                        }
                        if (durationInMilliseconds != -1L) { //If time is now defined, get the text form of the time:
                            alFileItemsDisplay.get(position).videoTimeText = GlobalClass.getDurationTextFromMilliseconds(durationInMilliseconds);
                            alFileItemsDisplay.get(position).videoTimeInMilliseconds = durationInMilliseconds;
                        }
                    }

                    if (alFileItemsDisplay.get(position).videoTimeText.length() > 0) {
                        //If the video time text has been defined, recall and display the time:
                        sDuration = "Duration: " + alFileItemsDisplay.get(position).videoTimeText;
                    }

                } catch (Exception e) {
                    Context activityContext = getContext();
                    Toast.makeText(activityContext, e.getMessage() + "; File: " + alFileItemsDisplay.get(position).name, Toast.LENGTH_LONG).show();
                }
                sLine2 = sDuration + "\t" + sRequiredStorageSize;

            } else if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                sLine2 = sRequiredStorageSize;

            } else if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                //If import type is comic...
                //Get the file count for the comic:
                String sComicPageCount = "";
                if(viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_NH_COMIC_DOWNLOADER){
                    String sNHComicID = Service_Import.GetNHComicID(alFileItemsDisplay.get(position).name);
                    int iComicFileCount = getFilterMatchCount(sNHComicID + ".+");
                    sComicPageCount = "File count: " + iComicFileCount + ".";
                    long lCombinedSize = getFilterMatchCombinedSize(sNHComicID + ".+");
                    sRequiredStorageSize = "Comic size: " + GlobalClass.CleanStorageSize(lCombinedSize, GlobalClass.STORAGE_SIZE_NO_PREFERENCE) + ".";
                }

                sLine2 = sRequiredStorageSize + "\t" + sComicPageCount;
            }

            tvLine2.setText(sLine2);




            //Get tag text to apply to list item if tags are assigned to the item:
            StringBuilder sbTags = new StringBuilder();
            sbTags.append("Tags: ");
            ArrayList<Integer> aliTagIDs = alFileItemsDisplay.get(position).prospectiveTags;

            if(aliTagIDs != null){
                if(aliTagIDs.size() > 0) {
                    sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(0), viewModelImportActivity.iImportMediaCategory));
                    for (int i = 1; i < aliTagIDs.size(); i++) {
                        sbTags.append(", ");
                        sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(i), viewModelImportActivity.iImportMediaCategory));
                    }
                }
            }
            tvLine3.setText(sbTags.toString());

            //set the image type if folder or file
            if(alFileItemsDisplay.get(position).type.equals("folder")) {
                ivFileType.setImageResource(R.drawable.baseline_folder_white_18dp);
            } else {
                //ivFileType.setImageResource(R.drawable.baseline_file_white_18dp);

                //Get the Uri of the file and create/display a thumbnail:
                String sUri = alFileItemsDisplay.get(position).uri;
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
            return alFileItemsDisplay.size();
        }

        @Override
        public ItemClass_File getItem(int position) {
            return alFileItemsDisplay.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void applyTagToItems(Integer iTagID){
            //Apply tag to items if the item does not already have the tag:
            for(ItemClass_File fm: alFileItemsDisplay){
                if(!fm.prospectiveTags.contains(iTagID)){
                    fm.prospectiveTags.add(iTagID);
                }
            }
        }

        public void removeTagFromItems(Integer iTagID){
            //Remove tag from items if the item has the tag:
            for(ItemClass_File fm: alFileItemsDisplay){
                fm.prospectiveTags.remove(iTagID);
            }
        }

        public void applyFilter(String sFilter){
            alFileItemsDisplay.clear();
            for(ItemClass_File fm : alFileItems){
                if(fm.name.matches(sFilter)){
                    alFileItemsDisplay.add(fm);
                }
            }
        }

        public int getFilterMatchCount(String sFilter){
            int iPageCount = 0;
            for(ItemClass_File fm : alFileItems){
                if(fm.name.matches(sFilter)){
                    iPageCount++;
                }
            }
            return iPageCount;
        }

        public long getFilterMatchCombinedSize(String sFilter){
            long lCombinedSize = 0;
            for(ItemClass_File fm : alFileItems){
                if(fm.name.matches(sFilter)){
                    lCombinedSize += fm.sizeBytes;
                }
            }
            return lCombinedSize;
        }

    }



}