package com.agcurations.aggallerymanager;



import android.app.Application;

import java.io.File;
import java.util.TreeMap;

public class GlobalClass extends Application {
    //GlobalClass built using this guide:
    //https://androidexample.com/Global_Variable_Or_Application_Context_Variable_-_Android_Example/index.php?view=article_discription&aid=114&aaid=136

    //Global Variables:
    private File gvfCatalogComicsFolder;
    private File gvfLogsFolder;
    private File gvfCatalogContentsFile; //CatalogContentsFile record fields: ComicID, ComicName, MaxPageID, MissingPages, ComicSize (in kB)
    TreeMap<Integer, String[]> gvtmCatalogComicList;

    public static final int COMIC_ID_INDEX = 0;                 //Comic ID
    public static final int COMIC_NAME_INDEX = 1;               //Comic Name
    public static final int COMIC_FILE_COUNT_INDEX = 2;         //Files included with the comic
    public static final int COMIC_MAX_PAGE_ID_INDEX = 3;        //Max page ID extracted from file names
    public static final int COMIC_MISSING_PAGES_INDEX = 4;      //String of comma-delimited missing page numbers
    public static final int COMIC_SIZE_KB_INDEX = 5;            //Total size of all files in the comic
    public static final int COMIC_FOLDER_NAME_INDEX = 6;        //Name of the folder holding the comic pages
    public static final int COMIC_THUMBNAIL_FILE_INDEX = 7;     //Name of the file used as the thumbnail for the comic
    //public static final int COMIC_PARODIES_INDEX = 8;
    //public static final int COMIC_CHARACTERS_INDEX = 9;
    //public static final int COMIC_TAGS_INDEX = 10;              //Tags given to the comic
    //public static final int COMIC_ARTISTS_INDEX = 11;
    //public static final int COMIC_GROUPS_INDEX = 12;
    //public static final int COMIC_LANGUAGES_INDEX = 13;         //Language(s) found in the comic
    //public static final int COMIC_CATEGORIES_INDEX = 14;
    //public static final int COMIC_PAGES_INDEX = 15;             //Total number of pages as defined at the comic source

    public static final String[] ComicRecordFields = new String[]{
            "COMIC_ID",
            "COMIC_NAME",
            "FILE_COUNT",
            "MAX_PAGE_ID",
            "MISSING_PAGES",
            "COMIC_SIZE_KB",
            "FOLDER_NAME",
            "THUMBNAIL_FILE",
            "PARODIES",
            "CHARACTERS",
            "TAGS",
            "ARTISTS",
            "GROUPS",
            "LANGUAGES",
            "CATEGORIES",
            "PAGES"};




    public File getCatalogComicsFolder() {
        return gvfCatalogComicsFolder;
    }
    public void setCatalogComicsFolder(File fCatalogComicsFolder){
        gvfCatalogComicsFolder = fCatalogComicsFolder;
    }

    public File getLogsFolder() {
        return gvfLogsFolder;
    }
    public void setLogsFolder(File fLogsFolder){
        gvfLogsFolder = fLogsFolder;
    }

    public File getCatalogContentsFile() {
        return gvfCatalogContentsFile;
    }
    public void setCatalogContentsFile(File fCatalogContentsFile){
        gvfCatalogContentsFile = fCatalogContentsFile;
    }

    public TreeMap<Integer, String[]> getCatalogComicList() {
        return gvtmCatalogComicList;
    }
    public void setCatalogComicList(TreeMap<Integer, String[]> tmCatalogComicList){
        gvtmCatalogComicList = tmCatalogComicList;
    }


    //Begin Obfuscation section:

    public boolean ObfuscationOn = false;
    public int iObfuscationIndex;
    //public int OBFUSCATION_SUBJECT_VIDEOGAMES = 0;
    public int OBFUSCATION_SUBJECT_QUALITY = 1;
    public int iObfuscationSubjectSelection = OBFUSCATION_SUBJECT_QUALITY;

    int[][] iImageList = new int[][]{
            {
                    R.drawable.ovg_death_stranding,
                    R.drawable.ovg_doom_eternal,
                    R.drawable.ovg_fallout_new_vegas,
                    R.drawable.ovg_horizon_zero_dawn,
                    R.drawable.ovg_resident_evil_2
            },
            {
                    R.drawable.oq_cost_of_poor_quality,
                    R.drawable.oq_five_why,
                    R.drawable.oq_ishikawa_diagram,
                    R.drawable.oq_ishikawa_diagram2,
                    R.drawable.oq_kanban_method_principles,
                    R.drawable.oq_kanban_method,
                    R.drawable.oq_mccalls_quality_factors
            }
    };
    String[][] sObfuscationCategoryNames = new String[][]{
            {
                    "Top 10 PS4 Games 2015",
                    "Top 10 PS4 Games 2016",
                    "Top 10 PS4 Games 2017",
                    "Top 10 PS4 Games 2018",
                    "Top 10 PS4 Games 2019"
            },
            {
                    "Cost of Poor Quality",
                    "Five Why Diagram",
                    "Ishikawa Diagram 1",
                    "Ishikawa Diagram 2",
                    "Kanban Method Principles",
                    "Kanban Method",
                    "McCall's Quality Factors"
            }
    };
    String[] sObfuscatedProgramNames = new String[]{
            "Top Titles",
            "Quality Operations"
    };
    String sNonObfustatedProgramName = "Comic Catalog";

    public int getObfuscationImageCount(){
        return iImageList[iObfuscationSubjectSelection].length;
    }

    public int getObfuscationImage(int index) {
        if(index >= iImageList[iObfuscationSubjectSelection].length - 1){
            index = 0;
        }
        return iImageList[iObfuscationSubjectSelection][index];
    }

    public String getObfuscationCategoryName(){
        return sObfuscationCategoryNames[iObfuscationSubjectSelection][iObfuscationIndex];
    }

    public String getObfuscatedProgramName() {
        return sObfuscatedProgramNames[iObfuscationSubjectSelection];
    }

    public String getNonObfuscatedProgramName(){
        return sNonObfustatedProgramName;
    }

    //End obfuscation section.

    //=====================================================================================
    //===== Comic Page Viewer Options =====================================================
    //=====================================================================================
    //CPV = "Comic Page Viewer"
    public float bCPV_MaxScale = 4.0f; //Max zoom.
    public boolean bCPV_AllowZoomJump = true;
    public float fCPV_ZoomJumpOutThreshold = 100.0f;
    public float fCPV_ZoomJumpInThreshold = -200.0f;

    //When image is zoomed, options for pan speed:
    public boolean bCPV_PanAcceleration = true;
    public static final int CPV_PAN_SPEED_SCALED = 1; //Pan based on zoom level.
    public static final int CPV_PAN_SPEED_FIXED = 2;  //Pan based on user-selected speed.
    public int iCPV_PanSpeedMethod = CPV_PAN_SPEED_SCALED;
    public float fCPV_VerticalPanScalar = 1.5f;
    public float fCPV_HorizontalPanScalar = 1.5f;

}

