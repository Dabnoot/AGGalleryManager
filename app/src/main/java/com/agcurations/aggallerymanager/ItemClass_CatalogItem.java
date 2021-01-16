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

    public String[] sArtists;                      //Common comic tag category
    public String[] sCategories;                   //Common comic tag category
    public String[] sCharacters;                   //Common comic tag category
    public String[] sGroups;                       //Common comic tag category
    public String[] sLanguages;                    //Language(s) found in the comic

    public String[] sParodies;                     //Common comic tag category
    public String sComicName;                      //Comic name
    public int iPages;                             //Total number of pages as defined at the comic source
    public int iMax_Page_ID;                       //Max comic page id extracted from file names
    public int[] iMissing_Pages;                   //String of comma-delimited missing page numbers
    public int iFile_Count;                        //Files included with the comic. Can be used for integrity check.
    public boolean bOnline_Data_Acquired;          //Typically used to gather tag data from an online comic source, if automatic.
    public String sSource;                         //Website, if relevant. Intended for comics.
}
