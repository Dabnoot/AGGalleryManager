package com.agcurations.aggallerymanager;

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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
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

    public static final String PREFERENCE_COMICS_TAGS_RESTRICTED = "com.agcurations.aggallerymanager.preferences.comics.tags.restricted";

    private static File gfAppFolder;
    private static TreeMap<Integer, String> gtmComicTags;
    private static ArrayList<Integer> galiComicsRestrictedTags;

    static GlobalClass globalClass;

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

        //Get a list of comic tags to populate the multiSelect dropdown list:
        gtmComicTags = new TreeMap<>();
        for (Map.Entry<Integer, String[]>
                entry : globalClass.gtmCatalogTagReferenceLists.get(GlobalClass.MEDIA_CATEGORY_COMICS).entrySet()) {
            String sTagID = entry.getValue()[GlobalClass.TAG_ID_INDEX];
            String sTag = entry.getValue()[GlobalClass.TAG_NAME_INDEX];
            gtmComicTags.put(Integer.parseInt(sTagID),sTag);
        }


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //gssSelectedTags = sharedPreferences.getStringSet("multi_select_list_restricted_tags", null);

        //Get Comics' restricted tags:
        String gsComicsRestrictedTags = sharedPreferences.getString(PREFERENCE_COMICS_TAGS_RESTRICTED, null);
        galiComicsRestrictedTags = GlobalClass.getIntegerArrayFromString(gsComicsRestrictedTags, ",");

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
            //gssSelectedTags = sharedPreferences.getStringSet("multi_select_list_restricted_tags", null);
            //Get Comics' restricted tags:
            String gsComicsRestrictedTags = sharedPreferences.getString(PREFERENCE_COMICS_TAGS_RESTRICTED, null);
            galiComicsRestrictedTags = GlobalClass.getIntegerArrayFromString(gsComicsRestrictedTags, ",");
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
                    findPreference("multi_select_list_comics_restricted_tags");

            //Populate the MultiSelectListPreference drop-down menu:
            CharSequence[] csTagTexts, csTagIDs;
            ArrayList<String> alTagTexts = new ArrayList<>();
            ArrayList<String> alTagIDs = new ArrayList<>();
            for (Map.Entry<Integer, String> entry : gtmComicTags.entrySet()) {
                alTagIDs.add(entry.getKey().toString());
                alTagTexts.add(entry.getValue());
            }
            csTagIDs = alTagIDs.toArray(new CharSequence[0]);
            csTagTexts = alTagTexts.toArray(new CharSequence[0]);
            assert pref_restricted_tags != null;
            pref_restricted_tags.setEntries(csTagTexts);
            pref_restricted_tags.setEntryValues(csTagIDs);

            //Fill out the "selected tags" summary text:
            //Sort the strings:
/*            SortedSet<String> ssTemp = new TreeSet<>(gssSelectedTags);  //gssSelectedTags contains the tags selected by the user.

            //Format the strings:
            StringBuilder sb = new StringBuilder();
            Iterator<String> isIterator = ssTemp.iterator();
            sb.append(isIterator.next());
            while(isIterator.hasNext()){
                sb.append(", ");
                sb.append(isIterator.next());
            }
            String sTemp = sb.toString();*/

            String sTemp = GlobalClass.formDelimitedString(galiComicsRestrictedTags,", ");

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

                        //Get the tag text associated with each tag ID:
                        ArrayList<Integer> aliTagIDs = GlobalClass.getIntegerArrayFromString(sTemp, ", ");
                        ArrayList<String> alsTagTexts = globalClass.getTagTextsFromIDs(aliTagIDs, GlobalClass.MEDIA_CATEGORY_COMICS);

                        //Sort the strings:
                        SortedSet<String> ssTemp = new TreeSet<>(alsTagTexts);

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