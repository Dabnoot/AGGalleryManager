package com.agcurations.aggallerymanager;

public class ItemClass_CatalogItem {

    public String sItemID;                         //Video, image, comic id
    public int[] iTags;                            //Tags given to the video, image, or comic
    public String sFilename;                       //Video or image filename
    public String sFolder_Name;                    //Name of the folder holding the video, image, or comic pages
    public String[] sCast;                         //For videos and images
    public Double dDatetime_Import;                //Date of import. Used for sorting if desired
    public Double dDatetime_Last_Viewed_by_User;   //Date of last read by user. Used for sorting if desired
    public int iHeight;                            //Video or image dimension/resolution
    public int iWidth;                             //Video or image dimension/resolution
    public long lDuration_Milliseconds;            //Duration of video in milliseconds
    public String sDuration_Text;                  //Duration of video text in 00:00:00 format
    public String sResolution;                     //Resolution for sorting at user request
    public long lSize;                             //Size of video, image, or size of all files in the comic, in Bytes
    public String sThumbnail_File;                 //Name of the file used as the thumbnail for a video or comic

    //Comic-related variables:
    public String[] sComicArtists;                 //Common comic tag category
    public String[] sComicCategories;              //Common comic tag category
    public String[] sComicCharacters;              //Common comic tag category
    public String[] sComicGroups;                  //Common comic tag category
    public String[] sComicLanguages;               //Language(s) found in the comic
    public String[] sComicParodies;                //Common comic tag category
    public String sComicName;                      //Comic name
    public int iComicPages;                        //Total number of pages as defined at the comic source
    public int iComic_Max_Page_ID;                 //Max comic page id extracted from file names
    public int[] iComic_Missing_Pages;             //String of comma-delimited missing page numbers
    public int iComic_File_Count;                  //Files included with the comic. Can be used for integrity check.
    public boolean bComic_Online_Data_Acquired;    //Typically used to gather tag data from an online comic source, if automatic.
    public String sComic_Source;                   //Website, if relevant. Intended for comics.
}
