package com.agcurations.aggallerymanager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

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

public class Activity_AppSettings extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "AG Gallery Manager Preferences";

    public static final String PREFERENCE_COMICS_TAGS_RESTRICTED = "com.agcurations.aggallerymanager.preferences.comics.tags.restricted";

    private static TreeMap<Integer, String> gtmComicTags;
    private static ArrayList<Integer> galiComicsRestrictedTags;

    static GlobalClass globalClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Return theme away from startup_screen
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);
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


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //gssSelectedTags = sharedPreferences.getStringSet("multi_select_list_restricted_tags", null);

        //Get Comics' restricted tags:
        /*String gsComicsRestrictedTags = sharedPreferences.getString(PREFERENCE_COMICS_TAGS_RESTRICTED, null);
        galiComicsRestrictedTags = GlobalClass.getIntegerArrayFromString(gsComicsRestrictedTags, ",");*/

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
                    for (Map.Entry<String, ItemClass_Tag>
                            entry : globalClass.gtmCatalogTagReferenceLists.get(i).entrySet()) {
                        alTagIDs.add(entry.getValue().iTagID.toString());
                        alTagTexts.add(entry.getValue().sTagText);
                    }

                    csTagIDs = alTagIDs.toArray(new CharSequence[0]);
                    csTagTexts = alTagTexts.toArray(new CharSequence[0]);

                    pref_restricted_tags[i].setEntries(csTagTexts);
                    pref_restricted_tags[i].setEntryValues(csTagIDs);


                    StringBuilder sbRestrictedTagTextInit = new StringBuilder();
                    for (Map.Entry<String, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(i).entrySet()) {
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
                        for (Map.Entry<String, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
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
                                    for (Map.Entry<String, ItemClass_Tag> entry : globalClass.gtmCatalogTagReferenceLists.get(iMediaCategory).entrySet()) {
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