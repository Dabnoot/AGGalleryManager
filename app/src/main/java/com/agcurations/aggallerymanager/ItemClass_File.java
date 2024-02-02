package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class ItemClass_File implements Serializable {

    public final int iTypeFileFolderURL; //folder, file, url, etc.
    public final static int TYPE_FOLDER = 1;
    public final static int TYPE_FILE = 2;
    public final static int TYPE_URL = 3;
    public final static int TYPE_M3U8 = 4;
    public final static int TYPE_IMAGE_FROM_HOLDING_FOLDER = 5;
    public final String sFileOrFolderName;
    public String sExtension = "";
    public long lSizeBytes = 0;
    public Date dateLastModified = null;
    public boolean bMetadataDetected = false;
    public long lMetadataFileSizeBytes = 0;
    public String sWidth = "";
    public String sHeight = "";
    public Boolean bIsChecked = false;
    public String sUri = "";       //includes file source.
    public String sMimeType = "";
    public long lVideoTimeInMilliseconds = 0;
    public String sDestinationFolder = ""; //Used for moving/copying.
    public ArrayList<Integer> aliProspectiveTags = new ArrayList<>(); //"prospective" as in "about to be applied".
    public boolean bDataUpdateFlag = false; //Flag used to reduce excess processing
    public String sVideoTimeText = "";

    public String sUserName = ""; //Populated when bringing in data from the image holding folder. A specific user
                                  //  will have downloaded a folder via the browser, and we don't want other
                                  //  users to be able to import or view those files.

    public boolean bMarkedForDeletion = false;

    //Items for comic folder import:
    public ArrayList<Integer[]> aliNumberBlocks = new ArrayList<>(); //Tells where to find numbers that could be comic page IDs.
    public int iNumberBlockPageIDIndex = -1; //Tells which number block in aliNumberBlocks is the page ID text substring.
    public int iNumberBlockStatus = 0;   //Used for a comic folder to report status of page numbering identification.
    public final static int COMIC_PAGE_ID_BLOCK_COUNT_OK = 0;
    public final static int COMIC_PAGE_ID_BLOCK_COUNT_INCONSISTENT = 1;
    public final static int COMIC_PAGE_ID_BLOCK_COUNT_SKIPS_PAGES = 2;
    public String sUriParent = "";       //Folder source for grouping comic pages during import. Also supports DocumentsContract.Move operations.
    public String sUriThumbnailFile = ""; //For use by the comic folder ItemClass_File.
    public String sURL = "";              //For use during web-import.
    public String sPageCount = "";
    public String sComicArtists = "";
    public String sComicParodies = "";                    //Common comic tag category
    public String sComicCategories = "";                  //A "category" field sometimes included by some websites.
    public String sComicCharacters = "";                  //Comic characters, if relevant, particular to parodies.
    public String sComicGroups = "";                      //A "group" field sometimes included by some websites.
    public String sComicLanguages = "";                   //Language(s) found in the comic
    public String sComicVolume = "";                      //Comic "book number" or volume string
    public String sComicChapter = "";                     //Comic chapter string
    public String sComicChapterSubtitle = "";             //Comic chapter subtitle
    public int iComicPages = 0;
    int iThumbnailURLImageHeight = -1; //Used specifically for Comic Import Preview.
    int iThumbnailURLImageWidth = -1;  //Used specifically for Comic Import Preview.


    //Items for video download:
    public String sURLVideoLink = "";
    public String sURLThumbnail = ""; //Used both my comics and videos
    public ArrayList<Integer> aliRecognizedTags;  //For tags assigned by the source and found in our tags catalog.
    public ArrayList<String> alsUnidentifiedTags; //For tags assigned by the source but not found in our tags catalog. User approval required.
    ItemClass_M3U8 ic_M3U8; //Captures a streaming download, which consists of a set of TS files.
    //A video download will have either sURLVideoLink, or in the case of a streaming download
    //  an entry for ic_M3U8 which will contain many files to be downloaded.

    String sTitle = "";  //For facilitation of web download video and comic titles.

    public int iGrade = 3;  //Grade, to be specified by user.

    //Items for comic download:
    public ArrayList<String> alsImageURLs;

    public String sGroupID = "";        //Group ID to identify explict related items related much more closely than generic tags.
                                        //  Item is populated during import preview.

    public boolean bIllegalDataFound = false; //Used during import.
    public String sIllegalDataNarrative = "";

    public ItemClass_File(int _iTypeFileFolderURL,
                          String _FileOrFolderName)
    {
        this.iTypeFileFolderURL = _iTypeFileFolderURL;
        this.sFileOrFolderName = _FileOrFolderName;
    }

}
