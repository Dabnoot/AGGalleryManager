package com.agcurations.aggallerymanager;

import java.util.ArrayList;

import androidx.lifecycle.ViewModel;

public class ViewModel_ImportActivity extends ViewModel {
    public int iImportMediaCategory;

    public static final int COMIC_SOURCE_NH_COMIC_DOWNLOADER = 0;
    public static final int COMIC_SOURCE_FOLDER = 1;
    public static final int COMIC_SOURCE_WEBPAGE = 2;
    public int iComicImportSource = -1;  //Defines the types of files to be imported and guides the import process for comics.

    public static final int IMPORT_METHOD_MOVE = 0;
    public static final int IMPORT_METHOD_COPY = 1;
    public int iImportMethod;  //Move or copy.
    public ArrayList<ItemClass_File> alfiConfirmedFileImports;
    public String sWebAddress = "";
    public String sLPWebAddress = "";  //For detecting a change in the user-specified web address.
    public boolean bWebAddressChanged = false;  //Used to tell Fragment_Import_5a_WebConfirmation to gather the data.
    public boolean bOnlineDataGathered = false;  //Used to tell Fragment_Import_5a_WebConfirmation to not re-gather the data

    public boolean bImportInitiated = false;

    public boolean bUpdateImportSelectList = true; //Useful when the user goes back and selects a
    // different folder from which to import.

    public boolean bImportCategoryChange = true; //Useful to demand user re-select import folder
    // if they went back and changed the import type.

    public ItemClass_CatalogItem ci; //Used for import of comic web item, as the comic details
                                     // are gathered before the import takes place.
}
