package com.agcurations.aggallerymanager;

import java.util.ArrayList;

import androidx.lifecycle.ViewModel;

public class ViewModel_ImportActivity extends ViewModel {
    public int iImportMediaCategory;

    public static final int VIDEO_SOURCE_FOLDER = 0;
    public static final int VIDEO_SOURCE_WEBPAGE = 1;
    public int iVideoImportSource = -1;  //Guides the import process for videos.

    public static final int COMIC_SOURCE_FOLDER = 1;
    public static final int COMIC_SOURCE_WEBPAGE = 2;
    public static final int COMIC_SOURCE_WEBPAGE_SERIES = 3;
    public int iComicImportSource = -1;  //Guides the import process for comics.

    public static final int IMAGE_SOURCE_FOLDER = 1;
    public static final int IMAGE_SOURCE_WEBPAGE = 2;
    public static final int IMAGE_SOURCE_HOLDING_FOLDER = 3;
    public int iImageImportSource = -1;  //Guides the import process for comics.

    public int iImportMethod;  //Move or copy.
    public ArrayList<ItemClass_File> alfiConfirmedFileImports;

    public ArrayList<String> alsUnidentifiedTags; //Used to import new tags, primarily due to comic xml file.

    public String sWebAddress = "";

    public boolean bUpdateImportSelectList = true; //Useful when the user goes back and selects a
    // different folder from which to import.

    public boolean bImportCategoryChange = true; //Useful to demand user re-select import folder
    // if they went back and changed the import type.

    public ItemClass_CatalogItem ci; //Used for import of web item, as the details
                                     // are gathered before the import takes place.

    public boolean bImportingOrphanedFiles = false;
    public boolean bImportingOrphanedFilesRepairMode = false;

    //Create an array of keys that allow program to locate image links:
    public ItemClass_WebComicDataLocator webComicDataLocator;
}
