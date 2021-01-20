package com.agcurations.aggallerymanager;

import java.util.ArrayList;

import androidx.lifecycle.ViewModel;

public class ViewModel_ImportActivity extends ViewModel {
    public int iImportMediaCategory;

    public static final int COMIC_SOURCE_NH_COMIC_DOWNLOADER = 0;
    public static final int COMIC_SOURCE_FOLDER = 1;
    public static final int COMIC_SOURCE_WEBPAGE = 2;
    public int iComicImportSource;  //Defines the types of files to be imported and guides the import process for comics.

    public static final int IMPORT_METHOD_MOVE = 0;
    public static final int IMPORT_METHOD_COPY = 1;
    public int iImportMethod;  //Move or copy.
    public ArrayList<ItemClass_File> alfiConfirmedFileImports;

}
