package com.agcurations.aggallerymanager;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GlobalClassTest {

    @Test
    public void SplitFileNameIntoBaseAndExtension() {

        String[] sFileNameTests = {"hello.jpeg",
                                   "trythis.png"};
        String[][] sBaseAndExtensionExpected = {
                {"hello", "jpeg"},
                {"trythis", "png"}};
        String[] sBaseAndExtensionResult;

        for(String sFileNameTest: sFileNameTests){
            sBaseAndExtensionResult = GlobalClass.SplitFileNameIntoBaseAndExtension(sFileNameTest);
            assertEquals(sBaseAndExtensionResult.length, 2);
        }

    }

    @Test
    public void TestValidateCatalogItemData(){

        String sTags                  = "Test\n";
        String sFilename              = "Test\n";
        String sFolderRelativePath    = "Test\n";
        String sCast                  = "Test\n";
        String sDuration_Text         = "Test\n";
        String sResolution            = "Test\n";
        String sThumbnail_File        = "Test\n";
        String sComicArtists          = "Test\n";
        String sComicCategories       = "Test\n";
        String sComicCharacters       = "Test\n";
        String sComicGroups           = "Test\n";
        String sComicLanguages        = "Test\n";
        String sComicParodies         = "Test\n";
        String sTitle                 = "Test\n";
        String sComic_Missing_Pages   = "Test\n";
        String sSource                = "Test\n";
        String sVideoLink             = "Test\n";

        ItemClass_CatalogItem icci = new ItemClass_CatalogItem();
        icci.sTags					= sTags				  ;
        icci.sFilename              = sFilename           ;
        icci.sFolderRelativePath    = sFolderRelativePath ;
        icci.sCast                  = sCast               ;
        icci.sDuration_Text         = sDuration_Text      ;
        icci.sResolution            = sResolution         ;
        icci.sThumbnail_File        = sThumbnail_File     ;
        icci.sComicArtists          = sComicArtists       ;
        icci.sComicCategories       = sComicCategories    ;
        icci.sComicCharacters       = sComicCharacters    ;
        icci.sComicGroups           = sComicGroups        ;
        icci.sComicLanguages        = sComicLanguages     ;
        icci.sComicParodies         = sComicParodies      ;
        icci.sTitle                 = sTitle              ;
        icci.sComic_Missing_Pages   = sComic_Missing_Pages;
        icci.sSource                = sSource             ;
        icci.sVideoLink             = sVideoLink          ;

        icci = GlobalClass.validateCatalogItemData(icci);

        assertNotEquals(icci.sTags, sTags);

        icci.alsApprovedUsers.add("\n");
        icci = GlobalClass.validateCatalogItemData(icci);
        assertNull(icci);


    }

    public void TestValidateTagData(){

        String sTagText             = "Test\n";
        String sTagDescription      = "Test\n";

        ItemClass_Tag ict = new ItemClass_Tag(999, sTagText);

        ict = GlobalClass.validateTagData(ict);

        assertNotEquals(ict.sTagText, sTagText);
        assertNotEquals(ict.sTagDescription, sTagDescription);

        ict.alsTagApprovedUsers.add("\n");
        ict = GlobalClass.validateTagData(ict);
        assertNull(ict);


    }



}