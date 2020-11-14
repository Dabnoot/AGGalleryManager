package com.agcurations.aggallerymanager;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class AGGallerySettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "AG Gallery Manager Preferences";

    private static File gfAppFolder;
    private static String gsPin;
    private static TreeMap<Integer, String> gtmTags;
    private static Set<String> gssSelectedTags;
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

        gsPin = globalClass.gsPin;

        //Get a list of comic tags to populate the multiSelect dropdown list:
        gtmTags = new TreeMap<>();
        for (Map.Entry<Integer, String[]>
                entry : globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()) {
            String sTag = entry.getValue()[GlobalClass.TAG_NAME_INDEX];
            gtmTags.put(entry.getKey(),sTag);
        }


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gssSelectedTags = sharedPreferences.getStringSet("multi_select_list_restricted_tags", null);


        if(!gsPin.isEmpty()) {
            final String[] sPinEntry = new String[1];
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Preferences");
            builder.setMessage("Enter pin to view/set preferences.");

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sPinEntry[0] = input.getText().toString();
                    boolean bExit = false;
                    if(sPinEntry[0].isEmpty()){
                        bExit = true;
                    } else {
                        if(!sPinEntry[0].equals(gsPin)){
                            bExit = true;
                        }
                    }
                    if(bExit){
                        finish();
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();


                }
            });
            builder.show();


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
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            gssSelectedTags = sharedPreferences.getStringSet("multi_select_list_restricted_tags", null);
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
                    findPreference("preferences_pin");

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

                    //Attempt to write to file a pin number set by the user:
                    File fAppConfigFile;

                    String sExternalStorageState;
                    sExternalStorageState = Environment.getExternalStorageState();
                    if (sExternalStorageState.equals(Environment.MEDIA_MOUNTED) ){

                        //Look for the AppConfig file:
                        fAppConfigFile = new File(gfAppFolder.getAbsolutePath() + File.separator + "AppConfig.dat");
                        if (!fAppConfigFile.exists()){
                            try {
                                if(!fAppConfigFile.createNewFile()){
                                    Toast.makeText(getContext(),"Could not create AppConfig.dat at " + fAppConfigFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                                }
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

            //CONFIGURE THE restricted TAGS LIST PREFERENCE:

            final MultiSelectListPreference pref_restricted_tags =
                    findPreference("multi_select_list_restricted_tags");

            //Populate the MultiSelectListPreference drop-down menu:
            CharSequence[] csEntries;
            ArrayList<String> alTags = new ArrayList<>();
            for (Map.Entry<Integer, String> entry : gtmTags.entrySet()) {
                alTags.add(entry.getValue());
            }
            csEntries = alTags.toArray(new CharSequence[0]);
            assert pref_restricted_tags != null;
            pref_restricted_tags.setEntries(csEntries);
            pref_restricted_tags.setEntryValues(csEntries);

            //Fill out the "selected tags" summary text:
            //Sort the strings:
            SortedSet<String> ssTemp = new TreeSet<>(gssSelectedTags);  //gssSelectedTags contains the tags selected by the user.

            //Format the strings:
            StringBuilder sb = new StringBuilder();
            Iterator<String> isIterator = ssTemp.iterator();
            sb.append(isIterator.next());
            while(isIterator.hasNext()){
                sb.append(", ");
                sb.append(isIterator.next());
            }
            String sTemp = sb.toString();

            //Apply the new data to the summary:
            if (!(sTemp.isEmpty())) {
                sTemp = "Restricted tags: " + sTemp;
                pref_restricted_tags.setSummary(sTemp);
            }


            //Configure the change listener for when the user modifies the selection:
            pref_restricted_tags.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    //The user has modified the tag selection.
                    //Build-out the summary text.

                    StringBuilder sb = new StringBuilder();
                    sb.append(newValue);
                    String sTemp = sb.toString();
                    if(sTemp.length() > 0) {
                        //Get rid of brackets:
                        sTemp = sTemp.substring(1, sTemp.length() - 1);

                        //Sort the strings:
                        String[] sTempArray = sTemp.split(",");
                        SortedSet<String> ssTemp = new TreeSet<>();
                        for(String s: sTempArray) {
                            ssTemp.add(s.trim());
                        }

                        //Format the strings:
                        sb = new StringBuilder();
                        Iterator<String> isIterator = ssTemp.iterator();
                        sb.append(isIterator.next());
                        while(isIterator.hasNext()){
                            sb.append(", ");
                            sb.append(isIterator.next());
                        }
                        sTemp = sb.toString();

                        //Apply the new data to the summary:
                        if (!(sTemp.isEmpty())) {
                            sTemp = "Restricted tags: " + sTemp;
                            pref_restricted_tags.setSummary(sTemp);
                        }
                    }

                    return true;
                }


            });

        }

    }



}