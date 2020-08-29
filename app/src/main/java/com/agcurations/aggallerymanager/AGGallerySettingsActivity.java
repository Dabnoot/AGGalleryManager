package com.agcurations.aggallerymanager;

import android.app.Application;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class AGGallerySettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "AG Gallery Manager Preferences";

    private static File gfAppFolder;
    private static String gsPin;
    GlobalClass globalClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
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

        // Get the app directory:
        File[] fAvailableDirs = getExternalFilesDirs(null);
        if (fAvailableDirs.length == 2) {
            //Create the folder on the likely SDCard:
            gfAppFolder = new File(fAvailableDirs[1].toString());
        }else{
            //Create the folder on the likely Internal storage.
            gfAppFolder = new File(fAvailableDirs[0].toString());
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
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

    public static class MessagesFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.messages_preferences, rootKey);
        }
    }

    public static class SyncFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.sync_preferences, rootKey);
        }
    }

    public static class ComicsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.comics_preferences, rootKey);
        }
    }

    public static class LockedFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.locked_preferences, rootKey);

            final EditTextPreference pref_locked_preferences_pin = (EditTextPreference) findPreference("locked_preferences_pin");

            //Set keyboard to be numeric:
            pref_locked_preferences_pin.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);

                }
            });



            assert pref_locked_preferences_pin != null;
            pref_locked_preferences_pin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    //Attempt to write to file a pin number set by the user:
                    File fAppFolder;
                    File fAppConfigFile;

                    String sExternalStorageState;
                    sExternalStorageState = Environment.getExternalStorageState();
                    if (sExternalStorageState.equals(Environment.MEDIA_MOUNTED) ){



                        //Look for the AppConfig file:
                        fAppConfigFile = new File(gfAppFolder.getAbsolutePath() + File.separator + "AppConfig.dat");
                        if (!fAppConfigFile.exists()){
                            try {
                                fAppConfigFile.createNewFile();
                            }catch (IOException e){
                                Toast.makeText(getContext(),"Could not create AppConfig.dat at " + fAppConfigFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (fAppConfigFile.exists()){


                            try {
                                FileWriter fwAppConfigFile = new FileWriter(fAppConfigFile, false);
                                fwAppConfigFile.write(newValue.toString());
                                fwAppConfigFile.flush();
                                fwAppConfigFile.close();
                                gsPin = newValue.toString();
                            } catch (IOException e) {
                                Toast.makeText(getContext(),"Trouble writing AppConfig.dat at" + fAppConfigFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                            }

                        }

                    }


                    return true;
                }
            });

        }

    }



}