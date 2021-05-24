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
    public String sFilename = "";                         //Video or image filename, comic thumbnail image filename
    public String sFolder_Name = "";                      //Name of the folder holding the video, image, or comic pages
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
    public String sComicArtists = "";                     //Common comic tag category
    public String sComicCategories = "";                  //Common comic tag category
    public String sComicCharacters = "";                  //Common comic tag category
    public String sComicGroups = "";                      //Common comic tag category
    public String sComicLanguages = "";                   //Language(s) found in the comic
    public String sComicParodies = "";                    //Common comic tag category
    public String sComicName = "";                        //Comic name
    public int iComicPages = 0;                           //Total number of pages as defined at the comic source
    public int iComic_Max_Page_ID = 0;                    //Max comic page id extracted from file names
    public String sComic_Missing_Pages = "";              //String of comma-delimited missing page numbers
    public int iComic_File_Count = 0;                     //Files included with the comic. Can be used for integrity check.
    public boolean bComic_Online_Data_Acquired = false;   //Typically used to gather tag data from an online comic source, if automatic.
    public String sSource = "";                           //Website, if relevant. Originally intended for comics.

    public String sComicThumbnailURL = "";                //Used specifically for NH Comic import preview.
    public ArrayList<String[]> alsComicPageURLsAndDestFileNames; //Used specifically for NH Comic import.

    public static final int POST_PROCESSING_NONE = 0;
    public static final int POST_PROCESSING_COMIC_DLM_MOVE = 1; //DownloadIdleService will delete files
                                            // that have been downloaded and not touched after about a week.
                                            //  these files must be moved so that DIS can't find them.
    public int iPostProcessingCode = POST_PROCESSING_NONE; //Used to tell the app to that file requires post-processing of some sort after an operation.

    public int iGrade = 3;                                //Rating (grade) of the item, 1-5. Default to 3.
}
