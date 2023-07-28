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
}