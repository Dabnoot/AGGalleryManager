package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


public class MainActivityDataService extends IntentService {

    // IntentService Actions:
    private static final String ACTION_LOAD_APP_DATA = "com.agcurations.aggallerymanager.action.LAD";
    //Parameters:
    public static final String EXTRA_BOOL_DATA_LOAD_PROBLEM = "com.agcurations.aggallerymanager.extra.BDLP";
    public static final String EXTRA_STRING_DATA_LOAD_PROBLEM = "com.agcurations.aggallerymanager.extra.SDLP";

    //Global Variables:
    private GlobalClass globalClass;


    public MainActivityDataService() {
        super("MainActivityDataService");
    }


    public static void startActionLoadData(Context context) {
        Intent intent = new Intent(context, MainActivityDataService.class);
        intent.setAction(ACTION_LOAD_APP_DATA);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        globalClass = (GlobalClass) getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LOAD_APP_DATA.equals(action)) {
                handleActionLoadAppData();
            }
        }
    }



    Intent broadcastIntent_LoadAppDataResponse; //Make global to allow for problem notification string extras.
    private void handleActionLoadAppData() {

        String sExternalStorageState;
        sExternalStorageState = Environment.getExternalStorageState();
        if (sExternalStorageState.equals(Environment.MEDIA_MOUNTED) ) {

            File[] fAvailableDirs = getExternalFilesDirs(null);
            if (fAvailableDirs.length == 2) {
                GlobalClass.gvfAppFolder = fAvailableDirs[1];
            } else {
                GlobalClass.gvfAppFolder = fAvailableDirs[0];
            }



            //--------------------------------------------------------------------------------
            //Videos Folder Structure:
            GlobalClass.gvfVideosFolder = new File(GlobalClass.gvfAppFolder
                    + File.separator + "Videos");
            obtainFolderStructureItem(GlobalClass.gvfVideosFolder);

            GlobalClass.gvfVideoCatalogContentsFile = new File(GlobalClass.gvfVideosFolder.getAbsolutePath()
                    + File.separator + "CatalogContents.dat");

            GlobalClass.gvfVideoLogsFolder = new File(GlobalClass.gvfVideosFolder
                    + File.separator + "Logs");
            obtainFolderStructureItem(GlobalClass.gvfVideoLogsFolder);

            GlobalClass.gvfVideoTagsFile = new File(GlobalClass.gvfVideosFolder.getAbsolutePath()
                    + File.separator + "VideoTags.dat");
            //--------------------------------------------------------------------------------

            //--------------------------------------------------------------------------------
            //Comics Folder Structure:
            GlobalClass.gfComicsFolder = new File(GlobalClass.gvfAppFolder
                    + File.separator + "Comics");
            obtainFolderStructureItem(GlobalClass.gfComicsFolder);

            GlobalClass.gvfComicCatalogContentsFile = new File(GlobalClass.gfComicsFolder.getAbsolutePath()
                    + File.separator + "CatalogContents.dat");

            GlobalClass.gfComicLogsFolder = new File(GlobalClass.gfComicsFolder
                    + File.separator + "Logs");
            obtainFolderStructureItem(GlobalClass.gfComicLogsFolder);

            GlobalClass.gvfComicTagsFile = new File(GlobalClass.gfComicsFolder.getAbsolutePath()
                    + File.separator + "ComicTags.dat");
            //--------------------------------------------------------------------------------

            //Attempt to read a pin number set by the user:
            GlobalClass.gvfAppConfigFile = new File(GlobalClass.gvfAppFolder.getAbsolutePath()
                    + File.separator + "AppConfig.dat");
            if (!GlobalClass.gvfAppConfigFile.exists()) {
                try {
                    if (!GlobalClass.gvfAppConfigFile.createNewFile()) {
                        problemNotificationConfig("Could not create AppConfig.dat at " + GlobalClass.gvfAppConfigFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    problemNotificationConfig("Could not create AppConfig.dat at " + GlobalClass.gvfAppConfigFile.getAbsolutePath());
                }
            } else {

                //Read the AppConfig data. This file, at the time of design, was only intended to
                //  hold 1 piece of data - a pin/password set by the user to unlock certain settings.
                //  Specifically, settings for restricted tags, and turning the restriction on and off.
                BufferedReader brReader;
                String sLine = "";
                try {
                    brReader = new BufferedReader(new FileReader(GlobalClass.gvfAppConfigFile.getAbsolutePath()));
                    sLine = brReader.readLine();
                    brReader.close();
                } catch (IOException e) {
                    problemNotificationConfig("Trouble reading AppConfig.dat at" + GlobalClass.gvfAppConfigFile.getAbsolutePath());
                }

                //Set the global variable holding the pin:
                if (sLine == null) {
                    GlobalClass.gsPin = "";
                } else {
                    GlobalClass.gsPin = sLine;
                }
            }
        }

        //Attempt to read or create the video tags file:
        globalClass.gtmAllUniqueCatalogVideoTags =
                InitTagData(GlobalClass.gvfVideoTagsFile,
                        getResources().getStringArray(R.array.default_video_tags));
        //Attempt to read or create the comic tags file:
        globalClass.gssAllUniqueCatalogComicTags =
                InitTagDataComics(GlobalClass.gvfComicTagsFile,
                        getResources().getStringArray(R.array.default_comic_tags));

    }

    private void obtainFolderStructureItem(File file){
        if(!file.exists()){
            if(!file.mkdir()){
                problemNotificationConfig("Could not create item at " + file.getAbsolutePath());
            }
        }
    }

    private TreeMap<Integer, String> InitTagData(File fTagsFile, String[] sDefaultTags ){
        TreeMap<Integer, String> tmTags = new TreeMap<>();
        if(fTagsFile.exists()) {
            //Get Tags from file:
            BufferedReader brReader;
            try {

                brReader = new BufferedReader(new FileReader(fTagsFile.getAbsolutePath()));
                String sLine = brReader.readLine();

                while(sLine != null) {
                    String[] sFields;
                    sFields = sLine.split(",");
                    tmTags.put(Integer.parseInt(sFields[0]), sFields[1]);
                    sLine = brReader.readLine();
                }

                brReader.close();

            } catch (IOException e) {
                problemNotificationConfig("Trouble reading tags file at" + fTagsFile.getAbsolutePath());
            }
        } else { //If the tags file does not exist, create it and populate it with default values:
            try {
                if(fTagsFile.createNewFile()) {
                    try {
                        FileWriter fwTagsFile = new FileWriter(fTagsFile, false);
                        Integer i = 0;
                        for (String sEntry : sDefaultTags) {
                            if(!sEntry.equals("")) {
                                fwTagsFile.write(i.toString() + "," + sEntry + "\n");
                                tmTags.put(i, sEntry);
                                i++;
                            }
                        }
                        fwTagsFile.flush();
                        fwTagsFile.close();

                    } catch (IOException e) {
                        problemNotificationConfig( "Trouble writing file at "
                                + fTagsFile.getAbsolutePath());
                    }
                } else {
                    problemNotificationConfig( "Could not create file at "
                            + fTagsFile.getAbsolutePath());
                }
            }catch (IOException e){
                problemNotificationConfig("Could not create file at "
                        + fTagsFile.getAbsolutePath());
            }
        }

        return tmTags;
    }

    private SortedSet<String> InitTagDataComics(File fTagsFile, String[] sDefaultTags ){
        //Comic tags are not to be changed.
        //  Comic tags can be added, but tags will be automatically grabbed from comic-hosting web
        //  pages. We will use SortedSet instead of TreeMap. SortedSet will prevent duplicates. A
        //  nice bonus. TreeMap is used on Video and Image tags to give each tag an ID so that the
        //  user can rename them.
        SortedSet<String> ssTags = new TreeSet<>();
        if(fTagsFile.exists()) {
            //Get Tags from file:
            BufferedReader brReader;
            try {

                brReader = new BufferedReader(new FileReader(fTagsFile.getAbsolutePath()));
                String sLine = brReader.readLine();

                if(sLine != null) {
                    String[] sTags;
                    sTags = sLine.split(",");
                    ssTags.addAll(Arrays.asList(sTags));
                }

                brReader.close();

            } catch (IOException e) {
                problemNotificationConfig("Trouble reading tags file at" + fTagsFile.getAbsolutePath());
            }
        } else { //If the tags file does not exist, create it and populate it with default values:
            try {
                if(fTagsFile.createNewFile()) {
                    try {
                        FileWriter fwTagsFile = new FileWriter(fTagsFile, false);
                        for (String sEntry : sDefaultTags) {
                            if(!sEntry.equals("")) {
                                fwTagsFile.write(sEntry + ",");
                                //Don't worry about the trailing comma. It will be ignored on
                                //  read/String.split operation.
                                ssTags.add(sEntry);
                            }
                        }
                        fwTagsFile.flush();
                        fwTagsFile.close();

                    } catch (IOException e) {
                        problemNotificationConfig( "Trouble writing file at "
                                + fTagsFile.getAbsolutePath());
                    }
                } else {
                    problemNotificationConfig( "Could not create file at "
                            + fTagsFile.getAbsolutePath());
                }
            }catch (IOException e){
                problemNotificationConfig("Could not create file at "
                        + fTagsFile.getAbsolutePath());
            }
        }

        return ssTags;
    }



    void problemNotificationConfig(String sMessage){
        broadcastIntent_LoadAppDataResponse.putExtra(EXTRA_BOOL_DATA_LOAD_PROBLEM, true);
        broadcastIntent_LoadAppDataResponse.putExtra(EXTRA_STRING_DATA_LOAD_PROBLEM, sMessage);

    }


}
