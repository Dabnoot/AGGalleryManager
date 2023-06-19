package com.agcurations.aggallerymanager;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
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

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_Import_5_Confirmation#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_Import_5_Confirmation extends Fragment {

    private GlobalClass globalClass;
    private ViewModel_ImportActivity viewModelImportActivity;
    ConfirmationFileListCustomAdapter confirmationFileListCustomAdapter;

    public Fragment_Import_5_Confirmation() {
        // Required empty public constructor
    }

    public static Fragment_Import_5_Confirmation newInstance() {
        return new Fragment_Import_5_Confirmation();
    }

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
        return inflater.inflate(R.layout.fragment_import_5_confirmation, container, false);
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

    public void initComponents(){

        if(getView() == null)
            return;



        // Construct the data source
        long lRequiredStorageSpaceBytes = 0L;
        ArrayList<ItemClass_File> alicf_FileImports = new ArrayList<>();
        for(ItemClass_File fileItem: viewModelImportActivity.alfiConfirmedFileImports){
            if(!fileItem.bMarkedForDeletion) {
                lRequiredStorageSpaceBytes += fileItem.lSizeBytes;

                //Set the destination folder on each file item:
                ItemClass_StorageFolderAvailability icsfa = GlobalClass.gtmFolderAvailability.get(viewModelImportActivity.iImportMediaCategory);
                if(icsfa == null){
                    //Get the next folder:
                    GlobalClass.getAGGMStorageFolderAvailability(viewModelImportActivity.iImportMediaCategory);
                    icsfa = GlobalClass.gtmFolderAvailability.get(viewModelImportActivity.iImportMediaCategory);
                }
                if(icsfa != null) {
                    if(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS){
                        if(fileItem.iTypeFileFolderURL == ItemClass_File.TYPE_FOLDER){
                            //If this is the comic folder fileItem, increase the file count by 1.
                            //  The comic will be stored in a folder inside the subfolder. It is the
                            //  subfolder count that is being increased.
                            icsfa.iFileCount++;
                        }
                    } else {
                        icsfa.iFileCount++;
                    }
                    if(icsfa.iFileCount >= 250){
                        //Designate the next folder to hold content:
                        int iFolderID = Integer.parseInt(icsfa.sFolderName); //Should not yield an exception as it should have already been caught in a prior process.
                        iFolderID++;
                        icsfa.iFileCount = 0;
                        icsfa.sFolderName = "" + iFolderID;
                    }

                    fileItem.sDestinationFolder = icsfa.sFolderName;
                    alicf_FileImports.add(fileItem);
                }
            }
        }

        //Populate the ListView with selected file names from an earlier step:
        ListView listView_FilesToImport = getView().findViewById(R.id.listView_GlobalTagsSelection);
        if (getActivity() == null) {
            return;
        }
        confirmationFileListCustomAdapter = new ConfirmationFileListCustomAdapter(getActivity(), R.id.listView_GlobalTagsSelection, alicf_FileImports);
        if(listView_FilesToImport != null) {
            listView_FilesToImport.setAdapter(confirmationFileListCustomAdapter);
        }

        //Display the file count:
        TextView textView_FileCount = getView().findViewById(R.id.textView_FileCount);
        String s = Integer.toString(confirmationFileListCustomAdapter.getCount());
        textView_FileCount.setText(s);


        //Display the required space:
        TextView textView_RequiredStorageSpace = getView().findViewById(R.id.textView_RequiredStorageSpace);
        s = GlobalClass.CleanStorageSize(lRequiredStorageSpaceBytes, GlobalClass.STORAGE_SIZE_NO_PREFERENCE);
        textView_RequiredStorageSpace.setText(s);

        //Get the units from 'required space' to match with 'available space':
        String[] sData = s.split(" ");
        String sStorageUnit = GlobalClass.STORAGE_SIZE_NO_PREFERENCE;
        if(sData.length == 2) {
            sStorageUnit = sData[1];
        }

        //Display the available space:
        long lAvailableStorageSpaceBytes;
        GlobalClass globalClass;
        globalClass = (GlobalClass) getActivity().getApplicationContext();
        lAvailableStorageSpaceBytes = globalClass.AvailableStorageSpace(getActivity().getApplicationContext(), 1);
        TextView textView_AvailableStorageSpace = getView().findViewById(R.id.textView_AvailableStorageSpace);
        s = GlobalClass.CleanStorageSize(lAvailableStorageSpaceBytes, sStorageUnit);
        textView_AvailableStorageSpace.setText(s);




    }

    public class ConfirmationFileListCustomAdapter extends ArrayAdapter<ItemClass_File> {

        final public ArrayList<ItemClass_File> alFileItems;
        final public ArrayList<ItemClass_File> alFileItemsDisplay;

        public ConfirmationFileListCustomAdapter(@NonNull Context context, int resource, ArrayList<ItemClass_File> alfi) {
            super(context, resource);
            alFileItems = alfi;
            alFileItemsDisplay = new ArrayList<>();
            for(ItemClass_File icf: alfi){
                if(icf.iTypeFileFolderURL != ItemClass_File.TYPE_FOLDER){
                    alFileItemsDisplay.add(icf);
                }
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

            ImageView ivFileType = row.findViewById(R.id.imageView_StorageItemIcon);
            TextView tvLine1 = row.findViewById(R.id.textView_Line1);
            TextView tvLine2 = row.findViewById(R.id.textView_Line2);
            TextView tvLine3 = row.findViewById(R.id.textView_Line3);

            tvLine1.setText(alFileItemsDisplay.get(position).sFileOrFolderName);
            DateFormat dfDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.getDefault() );
            String sLine2 = "";
            if (!((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS
                        && viewModelImportActivity.iVideoImportSource == ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE)
                    || (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS
                        && viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE))) {
                //If this is not a video download, display the datetime of the file:
                sLine2 = dfDateFormat.format(alFileItemsDisplay.get(position).dateLastModified);
            }

            boolean bIsVideoOrGif = (alFileItemsDisplay.get(position).sMimeType.startsWith("video")) ||
                    (alFileItemsDisplay.get(position).sExtension.contentEquals(".gif")) ||
                    (alFileItemsDisplay.get(position).sMimeType.equals("application/octet-stream") && alFileItemsDisplay.get(position).sExtension.equals(".mp4"));
            if(bIsVideoOrGif) {
                //If type is video or gif, get the duration:
                long durationInMilliseconds = -1L;
                //If mimeType is video or gif, get the duration:
                try {
                    if (alFileItemsDisplay.get(position).lVideoTimeInMilliseconds == -1L) { //If the time has not already been determined for the video file...
                        if (alFileItemsDisplay.get(position).sMimeType.startsWith("video")) {
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
                        sLine2 = sLine2 + "\tDuration: " + alFileItemsDisplay.get(position).sVideoTimeText;
                    }
                } catch (Exception e) {
                    Context activityContext = getContext();
                    Toast.makeText(activityContext, e.getMessage() + "; File: " + alFileItemsDisplay.get(position).sFileOrFolderName, Toast.LENGTH_LONG).show();
                }
            }
            if(!sLine2.equals("")){
                sLine2 = sLine2 + "\t";
            }
            sLine2 = sLine2 + "File size: " + GlobalClass.CleanStorageSize(
                    alFileItemsDisplay.get(position).lSizeBytes,
                    GlobalClass.STORAGE_SIZE_NO_PREFERENCE);

            if(alFileItemsDisplay.get(position).bMarkedForDeletion){
                sLine2 = "Marked for deletion.";
            }

            tvLine2.setText(sLine2);

            String sLine3 = "";
            if (!(viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS
                    && viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE)) {
                //Get tag text to apply to list item if tags are assigned to the item, but not if it is a web comic item.
                //  A comic item would have tags for every single page because they are assigned when the html is analyzed.
                //  This is done because the file items carry the tag data, and a future feature might have it such that
                //  the user can choose not to import a "detected" page, or first page - such a page might actually
                //  merely be a jpeg Advertisement not associated with the comic.
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
                sLine3 = sbTags.toString();
            }

            if(viewModelImportActivity.iImportMediaCategory != GlobalClass.MEDIA_CATEGORY_COMICS) {
                //Don't put the destination for each comic page because it is the same for every page.
                if(!sLine3.equals("")) {
                    sLine3 = sLine3 + "\n";
                }
                sLine3 = sLine3 + "Destination path: " +
                        globalClass.gUriCatalogFolders[viewModelImportActivity.iImportMediaCategory] +
                        File.separator +
                        alFileItemsDisplay.get(position).sDestinationFolder;
            }
            if(alFileItemsDisplay.get(position).bMarkedForDeletion){
                sLine3 = "";
            }
            tvLine3.setText(sLine3);



            //set the image type if folder or file
            if(alFileItemsDisplay.get(position).iTypeFileFolderURL == ItemClass_File.TYPE_FOLDER) {
                ivFileType.setImageResource(R.drawable.baseline_folder_white_18dp);
            } else {
                if ((viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_VIDEOS
                        && viewModelImportActivity.iVideoImportSource == ViewModel_ImportActivity.VIDEO_SOURCE_WEBPAGE)
                    || (viewModelImportActivity.iImportMediaCategory == GlobalClass.MEDIA_CATEGORY_COMICS
                        && viewModelImportActivity.iComicImportSource == ViewModel_ImportActivity.COMIC_SOURCE_WEBPAGE)){
                    //If this is a video download, get the thumbnail from the IRL and display it:
                    String sURLThumbnail = alFileItems.get(position).sURLThumbnail;
                    if(!sURLThumbnail.equals("")) {
                        Glide.with(getContext()).
                                load(sURLThumbnail).
                                into(ivFileType);
                    }

                } else {
                    //Get the Uri of the file and create/display a thumbnail:
                    String sUri = alFileItemsDisplay.get(position).sUri;
                    Uri uri = Uri.parse(sUri);
                    Glide.with(getContext()).
                            load(uri).
                            into(ivFileType);
                }
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

    }

}