package com.agcurations.aggallerymanager;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class Activity_AppSettings extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "AG Gallery Manager Preferences";


    static GlobalClass globalClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        //setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);

        //Make it so that the thumbnail of the app in the app switcher hides the last-viewed screen:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_a_g_gallery_settings);
                        }
                    }

                });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }




        globalClass = (GlobalClass) getApplicationContext();

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                int iBackStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
                if (iBackStackEntryCount == 0) {
                    this.finish();
                } else {
                    getSupportFragmentManager().popBackStack();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        //fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    public class GeneralFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.general_preferences, rootKey);

            Preference preferenceDataFolderLocation = (Preference) findPreference("DATA_FOLDER_LOCATION");
            if(preferenceDataFolderLocation != null) {
                preferenceDataFolderLocation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        //Configure a thing to allow a response to the user selecting a folder:
                        ActivityResultLauncher<Intent> garlPromptForDataFolder = registerForActivityResult(
                                new ActivityResultContracts.StartActivityForResult(),
                                new ActivityResultCallback<ActivityResult>() {
                                    @Override
                                    public void onActivityResult(ActivityResult result) {
                                        // look for permissions before executing operations.
                                        if(getParent() == null){
                                            return;
                                        }

                                        //Check to make sure that we have read/write permission in the selected folder.
                                        //If we don't have permission, request it.
                                        if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                != PackageManager.PERMISSION_GRANTED) ||
                                                (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                                                        != PackageManager.PERMISSION_GRANTED)) {

                                            // Permission is not granted
                                            // Should we show an explanation?
                                            if ((ActivityCompat.shouldShowRequestPermissionRationale(getParent(),
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                                                    (ActivityCompat.shouldShowRequestPermissionRationale(getParent(),
                                                            Manifest.permission.READ_EXTERNAL_STORAGE))) {
                                                // Show an explanation to the user *asynchronously* -- don't block
                                                // this thread waiting for the user's response! After the user
                                                // sees the explanation, try again to request the permission.
                                                Toast.makeText(getApplicationContext(), "Permission required for read/write operation.", Toast.LENGTH_LONG).show();
                                            } else {
                                                // No explanation needed; request the permission
                                                ActivityCompat.requestPermissions(getParent(),
                                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                                Manifest.permission.READ_EXTERNAL_STORAGE},
                                                        Fragment_Import_1_StorageLocation.MY_PERMISSIONS_READWRITE_EXTERNAL_STORAGE);

                                                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                                                // app-defined int constant. The callback method gets the
                                                // result of the request.
                                            }
                                            //} else {
                                            // Permission has already been granted
                                        }

                                        //The above code checked for permission, and if not granted, requested it.
                                        //  Check one more time to see if the permission was granted:

                                        if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                == PackageManager.PERMISSION_GRANTED) &&
                                                (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                                                        == PackageManager.PERMISSION_GRANTED)) {
                                            //If we now have permission...
                                            //The result data contains a URI for the directory that
                                            //the user selected.

                                            //Put the import Uri into the intent (this could represent a folder OR a file:

                                            if(result.getData() == null) {
                                                return;
                                            }
                                            Intent data = result.getData();
                                            Uri treeUri = data.getData();
                                            if(treeUri == null) {
                                                return;
                                            }

                                            //Call a routine to validate the location and read data from it:
                                            Worker_Catalog_LoadData.initDataFolder(treeUri, getApplicationContext());

                                        }
                                    }
                                });

                        // Allow the user to choose a directory using the system's file picker.
                        Intent intent_GetImportFromFolder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                        // Provide write access to files and sub-directories in the user-selected directory:
                        intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent_GetImportFromFolder.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        //Start the activity:
                        garlPromptForDataFolder.launch(intent_GetImportFromFolder);
                        return true;
                    }
                });
            }

        }
    }

    public static class BrowserFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.browser_preferences, rootKey);

            Preference preference_close_all_open_tabs = findPreference("browser_close_all_open_tabs");
            preference_close_all_open_tabs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    //Confirm with the user that they are doing what they want to do:
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogCustomStyle);
                    builder.setTitle("AG Gallery Manager: Web Browser");
                    builder.setMessage("Close all tabs?");

                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();

                            if(globalClass.CheckIfFileExists(GlobalClass.gUriWebpageTabDataFile)){
                                try {
                                    if(DocumentsContract.deleteDocument(GlobalClass.gcrContentResolver, GlobalClass.gUriWebpageTabDataFile)){
                                        Toast.makeText(getContext(), "Success deleting file maintaining browser open tabs.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), "Could not delete file maintaining browser open tabs.", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (FileNotFoundException e) {
                                    Toast.makeText(getContext(), "Could not delete file maintaining browser open tabs.", Toast.LENGTH_SHORT).show();
                                }
                            }

                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog adConfirmationDialog = builder.create();
                    adConfirmationDialog.show();

                    return false;
                }
            });


        }
    }

    public static class ComicsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.comics_preferences, rootKey);
        }
    }

    public static class RestrictedFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            setPreferencesFromResource(R.xml.restricted_preferences, rootKey);

            //CONFIGURE THE PIN PREFERENCE:
            final EditTextPreference pref_preferences_pin =
                    findPreference(GlobalClass.gsPinPreference);

            //Set keyboard to be numeric:
            assert pref_preferences_pin != null;
            pref_preferences_pin.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                }
            });

            pref_preferences_pin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    globalClass.gsPin = newValue.toString();

                    return true;
                }
            });

            //CONFIGURE THE RESTRICTED TAGS LIST PREFERENCES:

            MultiSelectListPreference[] pref_restricted_tags =
                    {findPreference("multi_select_list_videos_restricted_tags"),
                            findPreference("multi_select_list_images_restricted_tags"),
                            findPreference("multi_select_list_comics_restricted_tags")};

            for(int i = 0; i < 3; i++) {

                if(pref_restricted_tags != null) {  //This line is the lesser evil of two misleading code checks.

                    //Populate the MultiSelectListPreference drop-down menu:
                    CharSequence[] csTagTexts, csTagIDs;
                    ArrayList<String> alTagTexts = new ArrayList<>();
                    ArrayList<String> alTagIDs = new ArrayList<>();

                    //Get a list of comic tags to populate the multiSelect dropdown list:
                    for (Map.Entry<Integer, ItemClass_Tag>
                            entry : globalClass.gtmApprovedCatalogTagReferenceLists.get(i).entrySet()) {
                        alTagIDs.add(entry.getValue().iTagID.toString());
                        alTagTexts.add(entry.getValue().sTagText);
                    }

                    csTagIDs = alTagIDs.toArray(new CharSequence[0]);
                    csTagTexts = alTagTexts.toArray(new CharSequence[0]);

                    pref_restricted_tags[i].setEntries(csTagTexts);
                    pref_restricted_tags[i].setEntryValues(csTagIDs);


                    StringBuilder sbRestrictedTagTextInit = new StringBuilder();
                    for (Map.Entry<Integer, ItemClass_Tag> entry : globalClass.gtmApprovedCatalogTagReferenceLists.get(i).entrySet()) {
                        if (entry.getValue().bIsRestricted) {
                            sbRestrictedTagTextInit.append(entry.getValue().sTagText);
                            sbRestrictedTagTextInit.append(", ");
                        }
                    }
                    String sRestrictedTagsTextInit = sbRestrictedTagTextInit.toString();

                    if (sRestrictedTagsTextInit.length() > 2) {
                        //Apply the new data to the summary:
                        sRestrictedTagsTextInit = sRestrictedTagsTextInit.substring(0, sRestrictedTagsTextInit.lastIndexOf(", "));
                        String sTemp = "Restricted tags: " + sRestrictedTagsTextInit;
                        pref_restricted_tags[i].setSummary(sTemp);
                    }

                    //Configure the change listener for when the user modifies the selection:
                    pref_restricted_tags[i].setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                        //The user has modified the tag selection.
                        //Build-out the summary text.

                        int iMediaCategory = 0;
                        switch (preference.getKey()) {
                            case "multi_select_list_videos_restricted_tags":
                                iMediaCategory = GlobalClass.MEDIA_CATEGORY_VIDEOS;
                                break;
                            case "multi_select_list_images_restricted_tags":
                                iMediaCategory = GlobalClass.MEDIA_CATEGORY_IMAGES;
                                break;
                            case "multi_select_list_comics_restricted_tags":
                                iMediaCategory = GlobalClass.MEDIA_CATEGORY_COMICS;
                                break;
                        }

                        //First turn off all restricted tags, and then turn back on based on newValue:
                        for (Map.Entry<Integer, ItemClass_Tag> entry : globalClass.gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                            entry.getValue().bIsRestricted = false;
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append(newValue);
                        String sTemp = sb.toString();
                        if (sTemp.length() > 0) {
                            //Get rid of brackets:
                            sTemp = sTemp.substring(1, sTemp.length() - 1);

                            if (sTemp.length() > 0) {
                                //Get the tag text associated with each tag ID:
                                ArrayList<Integer> aliTagIDs = GlobalClass.getIntegerArrayFromString(sTemp, ", ");
                                ArrayList<String> alsTagTexts = globalClass.getTagTextsFromIDs(aliTagIDs, iMediaCategory);

                                //Sort the strings:
                                SortedSet<String> ssTemp = new TreeSet<>(alsTagTexts);

                                //Format the strings:
                                sb = new StringBuilder();
                                Iterator<String> isIterator = ssTemp.iterator();
                                sb.append(isIterator.next());
                                while (isIterator.hasNext()) {
                                    sb.append(", ");
                                    sb.append(isIterator.next());
                                }
                                sTemp = sb.toString();

                                //Apply the new data to the summary:
                                if (!(sTemp.isEmpty())) {
                                    sTemp = "Restricted tags: " + sTemp;
                                    preference.setSummary(sTemp);
                                }

                                //Update the globalClass restricted tag listings:
                                for (Integer iRestrictedTag : aliTagIDs) {
                                    for (Map.Entry<Integer, ItemClass_Tag> entry : globalClass.gtmApprovedCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
                                        if (entry.getValue().iTagID.equals(iRestrictedTag)) {
                                            //If the restricted tag has been found, mark it as restricted:
                                            entry.getValue().bIsRestricted = true;
                                        }
                                    }
                                }
                            } else {
                                preference.setSummary("Select tags you wish to be restricted.");
                            }
                        }

                        return true;
                        }

                    });
                }
            } //End for loop setting the 3 restricted tags listings.

        }

    }



}