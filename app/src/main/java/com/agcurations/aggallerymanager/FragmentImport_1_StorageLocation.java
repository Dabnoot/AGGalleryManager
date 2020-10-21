package com.agcurations.aggallerymanager;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentImport_1_StorageLocation#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentImport_1_StorageLocation extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int REQUEST_CODE_GET_IMPORT_FOLDER = 1000;
    public static final int MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE = 2002;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;



    public FragmentImport_1_StorageLocation() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentImportStorageLocation_1.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentImport_1_StorageLocation newInstance(String param1, String param2) {
        FragmentImport_1_StorageLocation fragment = new FragmentImport_1_StorageLocation();
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
        return inflater.inflate(R.layout.fragment_import_1_storage_location, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if(getView() == null){
            return;
        }
        Button button_SelectFolder = getView().findViewById(R.id.button_SelectFolder);
        button_SelectFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Allow the user to choose a directory using the system's file picker.
                Intent intent_GetImportFromFolder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                // Provide write access to files and sub-directories in the user-selected directory:
                intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                //Start the activity:
                startActivityForResult(intent_GetImportFromFolder, REQUEST_CODE_GET_IMPORT_FOLDER);
            }
        });

    }


    @Override
    //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        //https://developer.android.com/training/data-storage/shared/documents-files

        super.onActivityResult(requestCode, resultCode, resultData);


        //If this is an EXPORT operation, and the data is not NULL,
        // look for permissions before executing operations.
        if(getActivity() == null){
            return;
        }
        if ((requestCode == REQUEST_CODE_GET_IMPORT_FOLDER && resultCode == Activity.RESULT_OK)
                && (resultData != null)){
            //Check to make sure that we have read/write permission in the selected folder.
            //If we don't have permission, request it.
            if ((ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED)) {

                // Permission is not granted
                // Should we show an explanation?
                if ((ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                        (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                                Manifest.permission.READ_EXTERNAL_STORAGE))) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    Toast.makeText(getActivity(), "Permission required for read/write operation.", Toast.LENGTH_LONG).show();
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
                //} else {
                // Permission has already been granted
            }


            //The above code checked for permission, and if not granted, requested it.
            //  Check one more time to see if the permission was granted:

            if ((ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED)) {
                //If we now have permission...
                //The result data contains a URI for the document or directory that
                //the user selected.

                //Put the import Uri into the intent (this could represent a folder OR a file:

                ImportActivity.guriImportTreeURI = resultData.getData();

                assert ImportActivity.guriImportTreeURI != null;
                DocumentFile df1 = DocumentFile.fromTreeUri(getActivity(), ImportActivity.guriImportTreeURI);
                if(df1 == null){
                    return;
                }
                String sTreeUriSourceName = df1.getName(); //Get name of the selected folder for display purposes.

                //Display the source name:
                if(getView() == null){
                    return;
                }
                TextView textView_Selected_Import_Folder = getView().findViewById(R.id.textView_Selected_Import_Folder);
                textView_Selected_Import_Folder.setText(sTreeUriSourceName);
                TextView textView_Label_Selected_Folder = getView().findViewById(R.id.textView_Label_Selected_Folder);
                textView_Label_Selected_Folder.setVisibility(View.VISIBLE);
                textView_Selected_Import_Folder.setVisibility(View.VISIBLE);


                ImportActivityDataService.startActionGetDirectoryContents(getActivity().getApplicationContext(), ImportActivity.guriImportTreeURI);






            }

        }

    }

}