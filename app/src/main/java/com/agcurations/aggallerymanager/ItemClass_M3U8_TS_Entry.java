package com.agcurations.aggallerymanager;

public class ItemClass_M3U8_TS_Entry {

    //File entries in an M3U8 look like this:
    //#EXTINF:<DURATION> [<KEY>="<VALUE>"]*,<TITLE>
    //Uri string

    //Sequence of the entries should be maintained by a TreeMap of this class.

    float fDuration = -1;
    String sUri = "";


}
