package com.agcurations.aggallerymanager;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.widget.Toast;

import java.io.FileNotFoundException;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class Activity_AppSettings extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "AG Gallery Manager Preferences";

    static GlobalClass globalClass;

    static ActivityResultLauncher<Intent> garlPromptForDataFolder;

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




        globalClass = (GlobalClass) getApplicationContext();

        // Configure the thing that will allow the user to choose a directory using the system's file picker.
        garlPromptForDataFolder = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onActivityResult(ActivityResult result) {

                        //The result data contains a URI for the directory that
                        //the user selected.

                        if(result.getData() == null) {
                            Toast.makeText(getApplicationContext(),
                                    "No data folder selected. A storage location may be selected from the Settings menu.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent data = result.getData();
                        Uri treeUri = data.getData();
                        if(treeUri == null) {
                            return;
                        }
                        final int takeFlags = data.getFlags() &
                                (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        //We must persist access to this folder or the user will be asked everytime to select a folder.
                        //  Even then, they well still have to re-access the location on device restart.
                        GlobalClass.gcrContentResolver.takePersistableUriPermission(treeUri, takeFlags);

                        //Call a routine to initialize the data folder:
                        Activity_Main.initDataFolder(treeUri, getApplicationContext());
                        finish();
                    }
                });


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

    public static class GeneralFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.general_preferences, rootKey);

            Preference preferenceDataFolderLocation = (Preference) findPreference("DATA_FOLDER_LOCATION");
            if(preferenceDataFolderLocation != null) {
                preferenceDataFolderLocation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        // Allow the user to choose a directory using the system's file picker.
                        Intent intent_DetermineDataFolder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                        // Provide write access to files and sub-directories in the user-selected directory:
                        intent_DetermineDataFolder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent_DetermineDataFolder.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        intent_DetermineDataFolder.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        //Start the activity:
                        garlPromptForDataFolder.launch(intent_DetermineDataFolder);

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

                    if(getContext() == null) return false;

                    //Confirm with the user that they are doing what they want to do:
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogCustomStyle);
                    builder.setTitle("AG Gallery Manager Web Browser");
                    builder.setMessage("Close all tabs?");

                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();

                            if(GlobalClass.CheckIfFileExists(GlobalClass.gUriWebpageTabDataFile)){
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

            Preference preference_clear_all_cookies = findPreference("clear_all_cookies");
            preference_clear_all_cookies.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    if(getContext() == null) return false;

                    //Confirm with the user that they are doing what they want to do:
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogCustomStyle);
                    builder.setTitle("AG Gallery Manager Web Browser");
                    builder.setMessage("Clear all Cookies?");

                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();

                            Toast.makeText(getContext(), "Clearing cookies...", Toast.LENGTH_SHORT).show();
                            android.webkit.CookieManager cookieManager = CookieManager.getInstance();

                            cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                                // a callback which is executed when the cookies have been removed

                                public void onReceiveValue(Boolean aBoolean) {
                                    Toast.makeText(getContext(), "Cookies cleared.", Toast.LENGTH_SHORT).show();
                                }
                            });

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

            EditTextPreference editTextPreference_max_tab_count = findPreference("max_tab_count");
            if(editTextPreference_max_tab_count != null) {
                String sValue = "" + GlobalClass.giMaxTabCount;
                editTextPreference_max_tab_count.setText(sValue);

                editTextPreference_max_tab_count.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {

                        if(getContext() == null) return false;

                        //Map this preference to a custom preference by the user name:
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

                        String sPreferenceNamePrefix = GlobalClass.gicuCurrentUser.sUserName;
                        String sMaxBrowserTabCountPref = sPreferenceNamePrefix + GlobalClass.USR_MAX_BROWSER_TAB_COUNT_PREF_SUFFIX;

                        String sNewValue = (String) newValue;

                        sharedPreferences.edit()
                                .putString(sMaxBrowserTabCountPref, sNewValue)
                                .apply();

                        return true;
                    }
                });
            }



        }
    }

    public static class ComicsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.comics_preferences, rootKey);
        }
    }


}