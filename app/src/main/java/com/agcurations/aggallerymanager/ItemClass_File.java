package com.agcurations.aggallerymanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class ItemClass_File implements Serializable {

    final public String type; //folder or file
    final public String name;
    final public String extension;
    final public long sizeBytes;
    final public Date dateLastModified;
    final public String width;
    final public String height;
    public Boolean isChecked;
    final public String uri;       //includes file source.
    final public String mimeType;
    public long videoTimeInMilliseconds;
    public String destinationFolder; //Used for moving/copying.

    public ArrayList<Integer> prospectiveTags; //"prospective" as in "about to be applied".
    public boolean bPreviewTagUpdate = false; //Flag used to reduce excess processing

    public String videoTimeText;

    public ItemClass_File(String _type,
                          String _name,
                          String _extension,
                          long _sizeBytes,
                          Date _dateLastModified,
                          String _width,
                          String _height,
                          Boolean _isChecked,
                          String _uri,
                          String _mime,
                          long _videoTimeInMilliseconds)
    {
        this.uri = _uri;
        this.type = _type;
        this.name = _name;
        this.extension = _extension;
        this.sizeBytes = _sizeBytes;
        this.dateLastModified = _dateLastModified;
        this.width = _width;
        this.height = _height;
        this.isChecked = _isChecked;
        this.mimeType = _mime;
        this.videoTimeInMilliseconds = _videoTimeInMilliseconds;
        this.videoTimeText = "";
        this.prospectiveTags = new ArrayList<>();
        this.destinationFolder = "";
    }
}
