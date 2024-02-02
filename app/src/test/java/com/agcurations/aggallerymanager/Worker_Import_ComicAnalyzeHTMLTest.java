package com.agcurations.aggallerymanager;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Worker_Import_ComicAnalyzeHTMLTest {

    @Test
    public void ExtractVolumeAndChapter(){

        String[] sTestStrings = {
                "Ch.001",
                "Chapter 199",
                "Chapter 198: Side Story 19",
                "Volume 21 Chapter 183",
                "Vol.3 Ch.23.5",
                "Vol.0 Ch.1"
        };
        String[][] sExpectedResults = {
                {"", "001"},
                {"", "199"},
                {"", "198"},
                {"21", "183"},
                {"3", "23.5"},
                {"0", "1"}
        };

        for(int i = 0; i < sTestStrings.length; i++){
            String[] sResults = Worker_Import_ComicAnalyzeHTML.ExtractVolumeAndChapter(sTestStrings[i]);
            assertEquals(sResults[0], sExpectedResults[i][0]);
            assertEquals(sResults[1], sExpectedResults[i][1]);
        }

    }

}
