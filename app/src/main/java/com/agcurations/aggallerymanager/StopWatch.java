package com.agcurations.aggallerymanager;

import android.util.Log;

public class StopWatch {

    public long lStartTime = 0;
    public long lStopTime = 0;
    public long lElapsedTime = 0;
    public boolean bEnableDebug;

    public StopWatch(boolean EnableDebug){
        bEnableDebug = EnableDebug;
    }

    public void Start(){
        if(bEnableDebug) lStartTime = System.nanoTime();
    }

    public void Stop(){
        if(bEnableDebug) lStopTime = System.nanoTime();
    }

    public void Reset(){
        if(bEnableDebug) {
            lStartTime = System.nanoTime();
            Log.d("======StopWatch======", "Reset");
        }
    }

    public String GetElapsed(){
        if(bEnableDebug) {
            long lCurrentTime = System.nanoTime();
            lElapsedTime = lCurrentTime - lStartTime;
            double elapsedTimeInSeconds = (double) lElapsedTime / 1_000_000_000;
            return elapsedTimeInSeconds + "s";
        } else {
            return "";
        }
    }


    public void PostDebugLogAndRestart(String sMessageBase){
        if(bEnableDebug){
            String sMessage = sMessageBase + GetElapsed();
            Log.d("======StopWatch======", sMessage);
            Start();
        }
    }


}
