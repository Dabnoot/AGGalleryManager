package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class ItemClass_File implements Serializable {

    public final int iTypeFileOrFolder; //folder or file
    public final static int TYPE_FOLDER = 1;
    public final static int TYPE_FILE = 2;
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
    public boolean bPreviewTagUpdate = false; //Flag used to reduce excess processing
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
    public String sURL = "";


    public int iGrade = 3;  //Grade, to be specified by user.

    public ItemClass_File(int _iTypeFileOrFolder,
                          String _FileOrFolderName)
    {
        this.iTypeFileOrFolder = _iTypeFileOrFolder;
        this.sFileOrFolderName = _FileOrFolderName;
    }

}
