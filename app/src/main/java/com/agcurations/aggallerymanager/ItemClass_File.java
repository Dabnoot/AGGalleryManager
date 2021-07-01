package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class ItemClass_File implements Serializable {

    public final int iTypeFileFolderURL; //folder or file
    public final static int TYPE_FOLDER = 1;
    public final static int TYPE_FILE = 2;
    public final static int TYPE_URL = 3;
    public final static int TYPE_M3U8 = 4;
    public final String sFileOrFolderName;
    public String sExtension = "";
    public long lSizeBytes = 0;
    public Date dateLastModified = null;
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

    //Items for comic folder import:
    public ArrayList<Integer[]> aliNumberBlocks = new ArrayList<>(); //Tells where to find numbers that could be comic page IDs.
    public int iNumberBlockPageIDIndex = -1; //Tells which number block in aliNumberBlocks is the page ID text substring.
    public int iNumberBlockStatus = 0;   //Used for a comic folder to report status of page numbering identification.
    public final static int COMIC_PAGE_ID_BLOCK_COUNT_OK = 0;
    public final static int COMIC_PAGE_ID_BLOCK_COUNT_INCONSISTENT = 1;
    public final static int COMIC_PAGE_ID_BLOCK_COUNT_SKIPS_PAGES = 2;
    public String sUriParent = "";       //Folder source for grouping comic pages during import.
    public String sUriThumbnailFile = ""; //For use by the comic folder ItemClass_File.

    //Items for video download:
    public String sURLVideoLink = "";
    public String sURLThumbnail = "";
    public ArrayList<Integer> aliDownloadRecognizedTags;  //For tags assigned by the source and found in our tags catalog.
    public ArrayList<String> alsDownloadUnidentifiedTags; //For tags assigned by the source but not found in our tags catalog. User approval required.
    ItemClass_M3U8 ic_M3U8; //Captures a streaming download, which consists of a set of TS files.
    //A video download will have either sURLVideoLink, or in the case of a streaming download
    //  an entry for ic_M3U8 which will contain many files to be downloaded.

    String sTitle = "";  //Particularly for facilitation of video title (could be, but is not used for comic title as of 6/29/2021 commit).

    public int iGrade = 3;  //Grade, to be specified by user.

    public ItemClass_File(int _iTypeFileFolderURL,
                          String _FileOrFolderName)
    {
        this.iTypeFileFolderURL = _iTypeFileFolderURL;
        this.sFileOrFolderName = _FileOrFolderName;
    }

}
