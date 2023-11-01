package com.agcurations.aggallerymanager;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


public class Fragment_Import_0b_ImageSource extends Fragment {


    public Fragment_Import_0b_ImageSource() {
        // Required empty public constructor
    }

    public static Fragment_Import_0b_ImageSource newInstance() {
        return new Fragment_Import_0b_ImageSource();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            //Instantiate the ViewModel sharing data between fragments:
            //viewModelImportActivity = new ViewModelProvider(getActivity()).get(ViewModel_ImportActivity.class);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_0b_image_source, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity() != null) {
            getActivity().setTitle("Import");
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }


        //todo: Determine the number of files in the holding folder and adjust the radiobutton text to show file count.
        GlobalClass globalClass = (GlobalClass) getActivity().getApplicationContext();
        ArrayList<String> sImageHoldingFolderFiles = GlobalClass.GetDirectoryFileNames(GlobalClass.gUriImageDownloadHoldingFolder);

        int iFileCount = 0;

        //Need to determine which files have associated .dat metadata files.
        //  If there is an associated metadata file, read which user downloaded the file and
        //  don't expose that file to any other users.

        //List all metadata files by base name:
        ArrayList<String> alsMediaFilesWithDatFiles = new ArrayList<>();
        for(String sFileName: sImageHoldingFolderFiles){
            String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sFileName);
            if(sBaseAndExtension.length == 2) {
                if (sBaseAndExtension[1].equals("tad")) {
                    if(sImageHoldingFolderFiles.contains(sBaseAndExtension[0])){
                        //If the media file exists, add it to the list of media files with matching .dat files:
                        alsMediaFilesWithDatFiles.add(sBaseAndExtension[0]);
                    }/* else {
                        //If this is a .dat file without a media file... do nothing for now. Todo: Consider deleting the .dat file?
                        //  This case would be if the download failed for some reason, or perhaps the media file was moved
                        //  or was imported and the .dat file deletion failed.
                        //  Suggest starting the "delete files" worker if orphaned .dat files are found.
                    }*/
                }
            }
        }

        //Now look to see if there are any files without metadata files.
        ArrayList<String> alsNoMetadataFileMediaFiles = new ArrayList<>();
        boolean bMetadataFileFound;
        for(String sFileName: sImageHoldingFolderFiles){
            String[] sBaseAndExtension = GlobalClass.SplitFileNameIntoBaseAndExtension(sFileName);
            if(sBaseAndExtension.length == 2) {
                if (!sBaseAndExtension[1].equals("tad")) {
                    //If this is not a .dat file then it is a media file. Check if this media file had a corresponding .dat file.
                    if(!alsMediaFilesWithDatFiles.contains(sFileName)){
                        //If there was no associated .dat file found for this media file in the previous operation, record this file.
                        alsNoMetadataFileMediaFiles.add(sFileName);
                    }
                }
            }

        }

        //Now check metadata files for user. If the user is specified and matches the current user,
        // include this entry in the file count for potential imports.
        TreeMap<String, String> tmFilenamesAndUsers = new TreeMap<>();
        for(String sMediaFileName: alsMediaFilesWithDatFiles) {
            String sMetaDataFileName = sMediaFileName + ".tad";
            String sMetadataFileUri = GlobalClass.FormChildUriString(GlobalClass.gUriImageDownloadHoldingFolder.toString(), sMetaDataFileName);
            try {
                //Read a metadata file.
                Uri uriMetadataFile = Uri.parse(sMetadataFileUri);
                InputStream isImageMetadataFile = GlobalClass.gcrContentResolver.openInputStream(uriMetadataFile);
                if (isImageMetadataFile == null) {
                    String sMessage = "Could not open metadata file for analysis: " + sMetadataFileUri;
                    Toast.makeText(getContext(), sMessage, Toast.LENGTH_SHORT).show();
                    continue;
                }
                BufferedReader brReader;
                brReader = new BufferedReader(new InputStreamReader(isImageMetadataFile));
                int iLine = 0;
                for (String sLine = brReader.readLine(); sLine != null && iLine < 3; sLine = brReader.readLine()) {
                    switch (iLine) {
                        case 0:
                            //Do nothing
                            break;
                        case 1:
                            //Do nothing
                            break;
                        case 2:
                            tmFilenamesAndUsers.put(sMediaFileName, sLine);
                            break;
                    }
                    iLine++;
                }

                brReader.close();
                isImageMetadataFile.close();

                if(!tmFilenamesAndUsers.containsKey(sMediaFileName)){
                    //If a username was not found for this entry, add the filename but exclude a user.
                    tmFilenamesAndUsers.put(sMediaFileName, "");
                }

            } catch (Exception e) {
                String sMessage = "Could not open metadata file for analysis: " + sMetadataFileUri + "\n" + e.getMessage();
                Toast.makeText(getContext(), sMessage, Toast.LENGTH_SHORT).show();
            }
        }
        //tmFilenamesAndUsers now contains a list of all media filenames and the user as derived from an associated metadata file.

        //
        //Now combine file items that had metadata indicating that they are intended for the current
        //  user with any file items that were missing metadata files.
        ArrayList<String> alsApprovedFiles = new ArrayList<>();
        for(Map.Entry<String, String> entry: tmFilenamesAndUsers.entrySet()){
            if(entry.getValue().equals(GlobalClass.gicuCurrentUser.sUserName) ||
                    entry.getValue().equals("")){
                alsApprovedFiles.add(entry.getKey());
            }
        }
        alsApprovedFiles.addAll(alsNoMetadataFileMediaFiles);

        //All media files in holding folder now located that are approved for the current user
        //  or have no defined user or have no associated metadata file.

        iFileCount = alsApprovedFiles.size();
        /*for(String sFileName: sImageHoldingFolderFiles){
            String sExtension = sFileName.substring(sFileName.lastIndexOf("."));
            if(!sExtension.equals(".tad")){
                iFileCount++;
            }
        }*/

        RadioButton radioButton_ImageSourceHoldingFolder = getView().findViewById(R.id.radioButton_ImageSourceHoldingFolder);
        RadioGroup radioGroup_ImageSource = getView().findViewById(R.id.radioGroup_ImageSource);
        ViewGroup.LayoutParams vglp = radioGroup_ImageSource.getLayoutParams();
        if(iFileCount > 0){
            String sText = "Internal holding folder (" + iFileCount;
            if(iFileCount == 1) {
                sText = sText + " file)";
            } else {
                sText = sText + " files)";
            }
            radioButton_ImageSourceHoldingFolder.setText(sText);
            radioButton_ImageSourceHoldingFolder.setVisibility(View.VISIBLE);
            int iNewHeight = globalClass.ConvertDPtoPX(140);
            radioGroup_ImageSource.getLayoutParams().height = iNewHeight;
            radioGroup_ImageSource.requestLayout();
        } else {
            radioButton_ImageSourceHoldingFolder.setVisibility(View.INVISIBLE);
            int iNewHeight = globalClass.ConvertDPtoPX(90);
            radioGroup_ImageSource.getLayoutParams().height = iNewHeight;
            radioGroup_ImageSource.requestLayout();
        }





    }
}