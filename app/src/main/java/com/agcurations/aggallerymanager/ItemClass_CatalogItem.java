package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.ArrayList;

public class ItemClass_CatalogItem implements Serializable {
    public static final int MEDIA_CATEGORY_VIDEOS = 0;
    public static final int MEDIA_CATEGORY_IMAGES = 1;
    public static final int MEDIA_CATEGORY_COMICS = 2;

    public int iMediaCategory = -1;                       //Video, image, or comic.
    public String sItemID = "";                           //Video, image, comic id
    public String sTags = "";                             //Tags given to the video, image, or comic
    public ArrayList<Integer> aliTags;                    //Tags passed around as integer to increase filter speed. Todo: Look to replace sTags.
    public String sFilename = "";                         //Video or image filename, comic thumbnail image filename
    public String sFolderRelativePath = "";               //Relative path of the folder holding the video, image, or comic pages, relative to the catalog folder.
    public String sCast = "";                             //For videos and images
    public Double dDatetime_Import = 0d;                  //Date of import. Used for sorting if desired
    public Double dDatetime_Last_Viewed_by_User = 0d;     //Date of last read by user. Used for sorting if desired
    public int iHeight = 0;                               //Video or image dimension/resolution
    public int iWidth = 0;                                //Video or image dimension/resolution
    public long lDuration_Milliseconds = 0;               //Duration of video in milliseconds
    public String sDuration_Text = "";                    //Duration of video text in 00:00:00 format
    public String sResolution = "";                       //Resolution for sorting at user request
    public long lSize = 0;                                //Size of video, image, or size of all files in the comic, in Bytes
    public String sThumbnail_File = "";                   //Name of the file used as the thumbnail for a video (no longer for comic)

    //Comic-related variables:
    public String sComicArtists = "";                     //Comic artist(s).
    public String sComicCategories = "";                  //A "category" field sometimes included by some websites.
    public String sComicCharacters = "";                  //Comic characters, if relevant, particular to parodies.
    public String sComicGroups = "";                      //A "group" field sometimes included by some websites.
    public String sComicLanguages = "";                   //Language(s) found in the comic
    public String sComicParodies = "";                    //Common comic tag category
    public String sTitle = "";                            //Comic name or Video title
    public int iComicPages = 0;                           //Total number of pages as defined at the comic source
    public int iComic_Max_Page_ID = 0;                    //Max comic page id extracted from file names
    public String sComic_Missing_Pages = "";              //String of comma-delimited missing page numbers
    public int iFile_Count = 0;                           //Files included with the comic. Can be used for integrity check.
                                                          //  Also used for post-processing of M3U8 video file download completion check for post-processing.
    public boolean bComic_Online_Data_Acquired = false;   //Typically used to gather tag data from an online comic source, if automatic.
    public static final String FOLDER_SOURCE = "Folder Import";
    public String sSource = "";                           //Website, if relevant. Originally intended for comics.
    //Todo: Above sSource, how is it being used? Can I rename it to a string holding a URL? ComicURL?
    public String sVideoLink = "";                        //Link to the .mp4, .m3u8, etc, if it is a video download.


    public static final int FLAG_NO_CODE = 0;
    public static final int FLAG_COMIC_DLM_MOVE = 1; //DownloadIdleService will delete files
                                            // that have been downloaded and not touched after about a week.
                                            //  these files must be moved so that DIS can't find them.
    public static final int FLAG_VIDEO_DLM_SINGLE = 2; //Move a single video file to avoid DIS deletion.
    public static final int FLAG_VIDEO_DLM_CONCAT = 3; //Concatenate multiple video files and move the result. Worker handles the concat.
    public static final int FLAG_VIDEO_M3U8 = 4;  //Video item was downloaded and consists of
                                                            //  an m3u8 text file and multiple .ts files in
                                                            //  a folder. At some point in the future, a feature
                                                            //  may be devised to allow the user to abbreviate or trim
                                                            //  the video, concat the video, etc.
    public int iSpecialFlag = FLAG_NO_CODE; //Used to tell the app to that file requires post-processing of some sort after an operation.

    public int iGrade = 3;                                //Rating (grade) of the item, 1-5. Default to 3.

    public int iAllVideoSegmentFilesDetected = VIDEO_SEGMENT_FILES_UNDETERMINED; //Only used for m3u8 video downloads in which a video comprises many .ts files.
    public static final int VIDEO_SEGMENT_FILES_UNDETERMINED = 0;     //Video segment files have not been checked for complete set.
    public static final int VIDEO_SEGMENT_FILES_KNOWN_COMPLETE = 1;   //All video segment files detected and in-place.
    public static final int VIDEO_SEGMENT_FILES_KNOWN_INCOMPLETE = 2; //Some video segment files are missing, don't attempt to process.

    public int iMaturityRating = AdapterMaturityRatings.MATURITY_RATING_RP;
    public ArrayList<String> alsApprovedUsers = new ArrayList<>();


}
