package com.agcurations.aggallerymanager;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class ViewModel_Fragment_LogViewer extends ViewModel {
    public String sLogFileName;

    public ArrayList<ItemClass_File> alicf_LogFiles = new ArrayList<>(); //Selected ItemClass_File items are marked for deletion.
}
