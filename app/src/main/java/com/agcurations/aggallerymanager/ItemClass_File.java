package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class ItemClass_File implements Serializable {

    public final String sType; //folder or file
    public final String sName;
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
    public ArrayList<Integer> aliProspectiveTags; //"prospective" as in "about to be applied".
    public boolean bPreviewTagUpdate = false; //Flag used to reduce excess processing
    public String sVideoTimeText;

    public ItemClass_File(String _type,
                          String _name)
    {
        this.sType = _type;
        this.sName = _name;
    }

}
