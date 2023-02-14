package com.agcurations.aggallerymanager;

import android.net.Uri;

public class ItemClass_DocFileData {
    //This class used to accelerate the identification of files in external storage.
    //  The need came about in order to adapt to the Android Storage Access Framework.
    //  Catalog data file contains Media Category (used to determine the "media folder"),
    //  Sub Folder (files are, and now a legacy continuance, split into subfolders based on
    //  the initial tag ID applied), and FileName. The combination of Media Category, SubFolder,
    //  and FileName will be used to identify a match with the uri.

    int iMediaCategory;
    Uri uriParentFolder;
    String sFileName;
    String sPath; //Videos/1/12345/image.jpg
    Uri uri;
    boolean bIsFolder;
    boolean bContentQueried;  //Is used to avoid requery during data load as all tree paths are explored.
}
