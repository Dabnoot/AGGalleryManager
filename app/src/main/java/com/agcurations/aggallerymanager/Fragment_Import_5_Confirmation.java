package com.agcurations.aggallerymanager;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public Fragment_Import_5_Confirmation() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentImport_4_Confirmation.
     */
    // TODO: Rename and change types and number of parameters
    public static Fragment_Import_5_Confirmation newInstance(String param1, String param2) {
        Fragment_Import_5_Confirmation fragment = new Fragment_Import_5_Confirmation();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Calling Application class (see application tag in AndroidManifest.xml)
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
        return inflater.inflate(R.layout.fragment_import_5_confirmation, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        initComponents();
    }

    public void initComponents(){

        if(getView() == null)
            return;



        // Construct the data source
        long lRequiredStorageSpaceBytes = 0L;
        for(ItemClass_File fileItem: viewModelImportActivity.alfiConfirmedFileImports){
            lRequiredStorageSpaceBytes += fileItem.sizeBytes;

            //Set the destination folder on each file item:
            String sPrimaryTag;
            if(fileItem.prospectiveTags.size() > 0){
                sPrimaryTag = fileItem.prospectiveTags.get(0).toString();
            } else {
                sPrimaryTag = GlobalClass.gsUnsortedFolderName;
            }
            fileItem.destinationFolder = sPrimaryTag;
        }

        //Populate the ListView with selected file names from an earlier step:
        ListView listView_FilesToImport = getView().findViewById(R.id.listView_FilesToImport);
        if (getActivity() == null) {
            return;
        }
        confirmationFileListCustomAdapter = new ConfirmationFileListCustomAdapter(getActivity(), R.id.listView_FilesToImport, viewModelImportActivity.alfiConfirmedFileImports);
        if(listView_FilesToImport != null) {
            listView_FilesToImport.setAdapter(confirmationFileListCustomAdapter);
        }

        //Display the file count:
        TextView textView_FileCount = getView().findViewById(R.id.textView_FileCount);
        String s = Integer.toString(confirmationFileListCustomAdapter.getCount());
        textView_FileCount.setText(s);


        //Display the required space:
        TextView textView_RequiredStorageSpace = getView().findViewById(R.id.textView_RequiredStorageSpace);
        s = GlobalClass.CleanStorageSize(lRequiredStorageSpaceBytes);
        textView_RequiredStorageSpace.setText(s);

        //Display the available space:
        long lAvailableStorageSpaceBytes;
        GlobalClass globalClass;
        globalClass = (GlobalClass) getActivity().getApplicationContext();
        lAvailableStorageSpaceBytes = globalClass.AvailableStorageSpace(getActivity().getApplicationContext(), 1);
        TextView textView_AvailableStorageSpace = getView().findViewById(R.id.textView_AvailableStorageSpace);
        s = GlobalClass.CleanStorageSize(lAvailableStorageSpaceBytes);
        textView_AvailableStorageSpace.setText(s);


    }

    public class ConfirmationFileListCustomAdapter extends ArrayAdapter<ItemClass_File> {

        final public ArrayList<ItemClass_File> alFileItems;

        public ConfirmationFileListCustomAdapter(@NonNull Context context, int resource, ArrayList<ItemClass_File> alfi) {
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
                        Activity_Import.mediaMetadataRetriever.setDataSource(getContext(), docUri);
                        String time = Activity_Import.mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        durationInMilliseconds = Long.parseLong(time);
                    } else { //if it's not a video file, check to see if it's a gif:
                        if (alFileItems.get(position).extension.contentEquals(".gif")) {
                            //Get the duration of the gif image:
                            Uri docUri = Uri.parse(alFileItems.get(position).uri);
                            Context activityContext = getContext();
                            pl.droidsonroids.gif.GifDrawable gd = new pl.droidsonroids.gif.GifDrawable(activityContext.getContentResolver(), docUri);
                            durationInMilliseconds = gd.getDuration();
                        }
                    }
                    if(durationInMilliseconds != -1L) { //If time is now defined, get the text form of the time:
                        alFileItems.get(position).videoTimeText = GlobalClass.getDurationTextFromMilliseconds(durationInMilliseconds);
                        alFileItems.get(position).videoTimeInMilliseconds = durationInMilliseconds;
                    }
                }

                if(alFileItems.get(position).videoTimeText.length() > 0){
                    //If the video time text has been defined, recall and display the time:
                    sLine2 = sLine2 + "\tDuration: " + alFileItems.get(position).videoTimeText;
                }

                sLine2 = sLine2 + "\tFile size: " + GlobalClass.CleanStorageSize(alFileItems.get(position).sizeBytes);

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
                    sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(0), viewModelImportActivity.iImportMediaCategory));
                    for (int i = 1; i < aliTagIDs.size(); i++) {
                        sbTags.append(", ");
                        sbTags.append(globalClass.getTagTextFromID(aliTagIDs.get(i), viewModelImportActivity.iImportMediaCategory));
                    }
                }
            }

            String sLine3 = sbTags.toString();
            sLine3 = sLine3 + "\n";
            sLine3 = sLine3 + "Destination path: " + alFileItems.get(position).destinationFolder;
            tvLine3.setText(sLine3);

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
        public ItemClass_File getItem(int position) {
            return alFileItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

    }

}