package com.agcurations.aggallerymanager;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_Import_3_SelectTags#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_Import_3_SelectTags extends Fragment {

    GlobalClass globalClass;
    private ViewModel_ImportActivity viewModelImportActivity;

    public Fragment_Import_3_SelectTags() {
        // Required empty public constructor
    }

    public static Fragment_Import_3_SelectTags newInstance() {
        return new Fragment_Import_3_SelectTags();
    }

    private SelectedFileListCustomAdapter selectedFileListCustomAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);

            globalClass = (GlobalClass) getActivity().getApplicationContext();
        }

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
        if(getActivity() != null) {
            getActivity().setTitle("Import");
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }
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

        selectedFileListCustomAdapter = new SelectedFileListCustomAdapter(
                        getActivity(),
                        R.id.listView_FilesToImport,
                        viewModelImportActivity.alfiConfirmedFileImports);
        if(listView_FilesToImport != null) {
            listView_FilesToImport.setAdapter(selectedFileListCustomAdapter);
        }


        //If this is a single download item, preselect the tags.
        //  We do this here because the previous fragment allowed the user to choose whether or
        //  not to import any new tags associated with this download. Downloads are always single
        //  videos or complete comics, so tags apply to one item.
        //Get the text of the tags and display:
        /*if (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS
                && viewModelImportActivity.iVideoImportSource == ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE) {
            ArrayList<Integer> aliPreSelectedTags = viewModelImportActivity.alfiConfirmedFileImports.get(0).aliRecognizedTags;
            ArrayList<ItemClass_Tag> alict = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            if (aliPreSelectedTags.size() > 0) {
                int iTagID = aliPreSelectedTags.get(0);
                String sTagText = globalClass.getTagTextFromID(iTagID, viewModelImportActivity.iImportMediaCategory);
                ItemClass_Tag ict = new ItemClass_Tag(iTagID, sTagText);
                alict.add(ict);
                sb.append(sTagText);
                for (int i = 1; i < aliPreSelectedTags.size(); i++) {
                    sb.append(", ");
                    iTagID = aliPreSelectedTags.get(i);
                    sTagText = globalClass.getTagTextFromID(iTagID, viewModelImportActivity.iImportMediaCategory);
                    ict = new ItemClass_Tag(iTagID, sTagText);
                    alict.add(ict);
                    sb.append(sTagText);
                }
            }
            Activity_Import.viewModelTags.altiTagsSelected.setValue(alict);

            //Display the tags:
            if (getView() != null) {
                TextView textView_ImportTags = getView().findViewById(R.id.textView_ImportTags);
                if (textView_ImportTags != null) {
                    textView_ImportTags.setText(sb.toString());
                }
            }
        }*/
        //If the user is importing a single item or we are importing a video or comic from the web,
        // don't show the global tags listing line. Video from web should satisfy the "<2" condition.
        // Comics from the web will appear to have more than one item in the background.
        if ((viewModelImportActivity.alfiConfirmedFileImports.size() < 2)
            || ((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS
                && viewModelImportActivity.iVideoImportSource == ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE)
                || (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS
                && viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE))){
            TextView textView_LabelImportTags = getView().findViewById(R.id.textView_LabelImportTags);
            if (textView_LabelImportTags != null) {
                textView_LabelImportTags.setVisibility(View.INVISIBLE);
                textView_LabelImportTags.getLayoutParams().height = 0;
                textView_LabelImportTags.requestLayout();
            }
            TextView textView_ImportTags = getView().findViewById(R.id.textView_ImportTags);
            if (textView_ImportTags != null) {
                textView_ImportTags.setVisibility(View.INVISIBLE);
                textView_ImportTags.getLayoutParams().height = 0;
                textView_ImportTags.requestLayout();
            }
        }

        //Start the tag selection fragment:
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment_SelectTags fragment_selectTags = new Fragment_SelectTags();
        Bundle args = new Bundle();
        args.putInt(Fragment_SelectTags.MEDIA_CATEGORY, viewModelImportActivity.iImportMediaCategory);
        //If this is a webo download import, seed the tag selection fragment with the tags from
        // the web item that also exist in the tags catalog:
        if ((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS
                && viewModelImportActivity.iVideoImportSource == ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE)
                || (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS
                && viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE)){
            ArrayList<Integer> aliPreSelectedTags = viewModelImportActivity.alfiConfirmedFileImports.get(0).aliRecognizedTags;
            args.putIntegerArrayList(Fragment_SelectTags.PRESELECTED_TAG_ITEMS, aliPreSelectedTags);

            //Also modify the subject line text:
            TextView textView_Label_Subtext = getView().findViewById(R.id.textView_Label_Subtext);
            if(textView_Label_Subtext != null){
                textView_Label_Subtext.setText("Add or remove tags to be assigned to the imported item. Select next to continue.");

                //todo: An imported item with tags pulled from the web could have restricted tags.
                //  Determine the behavior on how to handle this. Currently the locked tags will
                //  be carried-in with the import. Restricted tags have funny behavior here when the
                //  user selects the "clear" icon on the tag selection bar.
            }
        }
        fragment_selectTags.setArguments(args);
        fragmentTransaction.replace(R.id.child_fragment_tag_selector, fragment_selectTags);
        fragmentTransaction.commit();

        fragment_selectTags.gbHistogramFreeze = true;


        //React to changes in the selected tag data in the ViewModel initiated in ImportActivity:
        final Observer<ArrayList<ItemClass_Tag>> selectedTagsObserver = new Observer<ArrayList<ItemClass_Tag>>() {
            @Override
            public void onChanged(ArrayList<ItemClass_Tag> tagItems) {

                //Get the text of the tags and display:
                StringBuilder sb = new StringBuilder();
                if(tagItems.size() > 0) {
                    sb.append(tagItems.get(0).sTagText);
                    for (int i = 1; i < tagItems.size(); i++) {
                        sb.append(", ");
                        sb.append(tagItems.get(i).sTagText);
                    }
                }
                //Display the tags:
                if(getView() != null) {
                    TextView textView_ImportTags = getView().findViewById(R.id.textView_ImportTags);
                    if (textView_ImportTags != null) {
                        textView_ImportTags.setText(sb.toString());
                    }
                }

                //Apply the selected tags to individual items:
                boolean bUpdateAdapter = false;
                ItemClass_Tag tiAdded = Activity_Import.viewModelTags.tiTagItemAdded.getValue();
                if(tiAdded != null){
                    selectedFileListCustomAdapter.applyTagToItems(tiAdded.iTagID);
                    bUpdateAdapter = true;
                }
                ArrayList<ItemClass_Tag> alictTagsRemoved = Activity_Import.viewModelTags.tiTagItemsRemoved.getValue();
                if(alictTagsRemoved != null){
                    selectedFileListCustomAdapter.removeTagsFromItems(alictTagsRemoved);
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

            if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                    //If importing comics and importing folders as the source, filter on the cover pages:
                    alFileItemsDisplay = new ArrayList<>(); //initialize.
                    applyFilterByType(ItemClass_File.TYPE_FOLDER);
                } else {
                    //If importing a comic from the web, it should be a single comic. Show only the first page.
                    alFileItemsDisplay = new ArrayList<>();
                    if(alfi.size() > 0) {
                        alFileItemsDisplay.add(alfi.get(0));
                    }
                }
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

            ImageView imageView_StorageItemIcon = row.findViewById(R.id.imageView_StorageItemIcon);
            TextView textView_Line1 = row.findViewById(R.id.textView_Line1);  //For Name
            TextView textView_Line2 = row.findViewById(R.id.textView_Line2);  //For item details
            TextView textView_Line3 = row.findViewById(R.id.textView_Line3);  //For Tags


            String sLine1 = "";  //For Name
            if ((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) ||
                    (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES)) {
                sLine1 = alFileItemsDisplay.get(position).sFileOrFolderName;
            } else {
                //If import type is comic...
                if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                    //Get the title of the comic from the file name:
                    sLine1 = alFileItemsDisplay.get(position).sFileOrFolderName;
                } else if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE) {
                    sLine1 = alFileItemsDisplay.get(position).sTitle;
                }
            }
            textView_Line1.setText(sLine1);


            String sLine2 = "";//For item details, including video duration, file size, comic page count, comic page file set size.
            /*DateFormat dfDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.getDefault() );
            sLine2 = sLine2 + dfDateFormat.format(alFileItemsDisplay.get(position).dateLastModified);*/
            String sRequiredStorageSize = "Size: " + GlobalClass.CleanStorageSize(alFileItemsDisplay.get(position).lSizeBytes, GlobalClass.STORAGE_SIZE_NO_PREFERENCE);

            if (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS) {
                //If type is video or gif, get the duration:
                String sDuration = "";
                long durationInMilliseconds = -1L;
                //If mimeType is video or gif, get the duration:
                try {
                    if (alFileItemsDisplay.get(position).lVideoTimeInMilliseconds == -1L) { //If the time has not already been determined for the video file...
                        if (alFileItemsDisplay.get(position).sMimeType.startsWith("video") ||
                                (alFileItemsDisplay.get(position).sMimeType.equals("application/octet-stream") &&
                                        alFileItemsDisplay.get(position).sExtension.equals(".mp4"))) { //https://stackoverflow.com/questions/51059736/why-some-of-the-mp4-files-mime-type-are-application-octet-stream-instead-of-vid){
                            Uri docUri = Uri.parse(alFileItemsDisplay.get(position).sUri);
                            Activity_Import.mediaMetadataRetriever.setDataSource(getContext(), docUri);
                            String time = Activity_Import.mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            durationInMilliseconds = Long.parseLong(time);
                        } else { //if it's not a video file, check to see if it's a gif:
                            if (alFileItemsDisplay.get(position).sExtension.contentEquals(".gif")) {
                                //Get the duration of the gif image:
                                Uri docUri = Uri.parse(alFileItemsDisplay.get(position).sUri);
                                Context activityContext = getContext();
                                pl.droidsonroids.gif.GifDrawable gd = new pl.droidsonroids.gif.GifDrawable(activityContext.getContentResolver(), docUri);
                                durationInMilliseconds = gd.getDuration();
                            }
                        }
                        if (durationInMilliseconds != -1L) { //If time is now defined, get the text form of the time:
                            alFileItemsDisplay.get(position).sVideoTimeText = GlobalClass.getDurationTextFromMilliseconds(durationInMilliseconds);
                            alFileItemsDisplay.get(position).lVideoTimeInMilliseconds = durationInMilliseconds;
                        }
                    }

                    if (alFileItemsDisplay.get(position).sVideoTimeText.length() > 0) {
                        //If the video time text has been defined, recall and display the time:
                        sDuration = "Duration: " + alFileItemsDisplay.get(position).sVideoTimeText;
                    }

                } catch (Exception e) {
                    Context activityContext = getContext();
                    Toast.makeText(activityContext, e.getMessage() + "; File: " + alFileItemsDisplay.get(position).sFileOrFolderName, Toast.LENGTH_LONG).show();
                }
                sLine2 = sDuration + "\t" + sRequiredStorageSize;

            } else if (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_IMAGES) {
                sLine2 = sRequiredStorageSize;

            } else if (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) {
                //If import type is comic...
                //Get the file count for the comic:
                String sComicPageCount = "";
                if (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER) {
                    String sUriParent = alFileItemsDisplay.get(position).sUri;
                    int iComicFileCount = getParentUriChildCount(sUriParent);
                    sComicPageCount = "File count: " + iComicFileCount + ".";
                    long lCombinedSize = getParentUriChildCombinedSize(sUriParent);
                    sRequiredStorageSize = "Comic size: " + GlobalClass.CleanStorageSize(lCombinedSize, GlobalClass.STORAGE_SIZE_NO_PREFERENCE) + ".";
                }

                sLine2 = sRequiredStorageSize + "\t" + sComicPageCount;
            }

            if(alFileItemsDisplay.get(position).bMarkedForDeletion){
                sLine2 = "Marked for deletion.";
            }

            textView_Line2.setText(sLine2);


            //Get tag text to apply to list item if tags are assigned to the item:
            StringBuilder sbTags = new StringBuilder();
            sbTags.append("Tags: ");
            ArrayList<Integer> aliTagIDs = alFileItemsDisplay.get(position).aliProspectiveTags;

            if (aliTagIDs != null) {
                if (aliTagIDs.size() > 0) {
                    sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(0), viewModelImportActivity.iImportMediaCategory));
                    for (int i = 1; i < aliTagIDs.size(); i++) {
                        sbTags.append(", ");
                        sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(i), viewModelImportActivity.iImportMediaCategory));
                    }
                }
            }
            String sLine3 = sbTags.toString();
            if(alFileItemsDisplay.get(position).bMarkedForDeletion){
                sLine3 = "";
            }
            textView_Line3.setText(sLine3);

            //set the image type if folder or file
            if (alFileItemsDisplay.get(position).iTypeFileFolderURL == ItemClass_File.TYPE_FOLDER) {
                if ((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS) &&
                        (viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_FOLDER)) {
                    //Get the Uri of the file and create/display a thumbnail:
                    String sUri = alFileItemsDisplay.get(position).sUriThumbnailFile;
                    Uri uri = Uri.parse(sUri);
                    Glide.with(getContext()).
                            load(uri).
                            into(imageView_StorageItemIcon);
                } else {
                    //If this is a folder item and we are not importing comics by the folder, display a simple folder icon.
                    //  If we are here, I think there is an mistake in logic somewhere.
                    imageView_StorageItemIcon.setImageResource(R.drawable.baseline_folder_white_18dp);
                }
            } else if ((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS
                        && viewModelImportActivity.iVideoImportSource == ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE)
                    || (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS
                        && viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE)){
                String sURLThumbnail = alFileItems.get(position).sURLThumbnail;
                if(!sURLThumbnail.equals("")) {
                    Glide.with(getContext()).
                            load(sURLThumbnail).
                            into(imageView_StorageItemIcon);
                }

            } else {

                //Get the Uri of the file and create/display a thumbnail:
                String sUri = alFileItemsDisplay.get(position).sUri;
                Uri uri = Uri.parse(sUri);
                Glide.with(getContext()).
                        load(uri).
                        into(imageView_StorageItemIcon);
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
                if(!fm.aliProspectiveTags.contains(iTagID)){
                    fm.aliProspectiveTags.add(iTagID);
                }
            }
        }

        public void removeTagsFromItems(ArrayList<ItemClass_Tag> alict_TagsToRemove){
            //Remove tags from items if the item has the tag:
            for(ItemClass_Tag ictTagToRemoveEntry: alict_TagsToRemove) {
                for (ItemClass_File fm : alFileItemsDisplay) {
                    fm.aliProspectiveTags.remove(ictTagToRemoveEntry.iTagID);
                }
            }
        }

        public void applyFilter(String sFilter){
            alFileItemsDisplay.clear();
            for(ItemClass_File fm : alFileItems){
                if(fm.sFileOrFolderName.matches(sFilter)){
                    alFileItemsDisplay.add(fm);
                }
            }
        }

        public void applyFilterByType(int iTypeFileOrFolder){
            alFileItemsDisplay.clear();
            for(ItemClass_File fi : alFileItems){
                if(fi.iTypeFileFolderURL ==iTypeFileOrFolder){
                    if(!fi.bMarkedForDeletion) {
                        alFileItemsDisplay.add(fi);
                    }
                }
            }
        }

        public int getFilterMatchCount(String sFilter){
            int iPageCount = 0;
            for(ItemClass_File fm : alFileItems){
                if(fm.sFileOrFolderName.matches(sFilter)){
                    iPageCount++;
                }
            }
            return iPageCount;
        }

        public long getFilterMatchCombinedSize(String sFilter){
            long lCombinedSize = 0;
            for(ItemClass_File fm : alFileItems){
                if(fm.sFileOrFolderName.matches(sFilter)){
                    lCombinedSize += fm.lSizeBytes;
                }
            }
            return lCombinedSize;
        }

        public int getParentUriChildCount(String sUriParent){
            int iChildCount = 0;
            for(ItemClass_File fm : alFileItems){
                if(fm.sUriParent.equals(sUriParent)){
                    iChildCount++;
                }
            }
            return iChildCount;
        }

        public long getParentUriChildCombinedSize(String sUriParent){
            long lCombinedSize = 0;
            for(ItemClass_File fm : alFileItems){
                if(fm.sUriParent.equals(sUriParent)){
                    lCombinedSize += fm.lSizeBytes;
                }
            }
            return lCombinedSize;
        }

    }



}