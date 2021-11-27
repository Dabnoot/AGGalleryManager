package com.agcurations.aggallerymanager;

import java.util.ArrayList;

public class ItemClass_M3U8 {

    //An M3U8 file is one typically found online that contains data regarding streaming media, such as video.
    //  This class attempts to provide a location to capture data regarding entries in an M3U8 file.
    //  Sometimes an M3U8 file contains links to other M3U8 files that contain lists of .TS files, which
    //  are MPEG-2 video files, typically 10 seconds long.

    //String sTitle;
    String sName = "";
    String sHost = "";
    String sFileName = "";
    String sBandwidth = "";
    String sResolution = "";
    String sBaseURL = "";
    String sCODECs = "";
    String sProgramID = "";
    float fDurationInSeconds;

    ArrayList<String> als_TSDownloads; //This item captures the TS file addresses for a given M3U8 entry.
    long lTotalTSFileSetSize = -1; //This captures the total size of all of the TS files for a given M3U8 entry.

}
