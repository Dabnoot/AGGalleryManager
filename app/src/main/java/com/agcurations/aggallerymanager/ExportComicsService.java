package com.agcurations.aggallerymanager;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportComicsService extends IntentService {
    public static final String ZIP_TOTAL_FILE_COUNT = "ZIP_TOTAL_FILE_COUNT";
    public static final String ZIP_LIST = "ZIP_LIST";
    public static final String ZIP_FILE = "ZIP_FILE";

    public static final String UPDATE_LOG_BOOLEAN = "UPDATE_LOG_BOOLEAN";
    public static final String LOG_LINE_STRING = "LOG_LINE_STRING";
    public static final String UPDATE_PERCENT_COMPLETE_BOOLEAN = "UPDATE_PERCENT_COMPLETE_BOOLEAN";
    public static final String PERCENT_COMPLETE_INT = "PERCENT_COMPLETE_INT";

    int giComicFileCount;
    int giProgressCounter;
    int giProgressBarValue;

    private GlobalClass globalClass;

    public ExportComicsService() {
        super("ExportComicsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        globalClass = (GlobalClass) getApplicationContext();

        //Get the list of files and folders to be zipped:
        if(intent == null) return;
        ArrayList<String> alZipList = intent.getStringArrayListExtra(ZIP_LIST);

        try {

            giComicFileCount = intent.getIntExtra(ZIP_TOTAL_FILE_COUNT,-1);

            //create ZipOutputStream to write to the zip file
            String sUriString = intent.getStringExtra(ZIP_FILE);
            Uri uriZipFile = Uri.parse(sUriString);
            ParcelFileDescriptor pfd = getContentResolver().
                    openFileDescriptor(uriZipFile, "w");

            assert pfd != null;
            FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
            ZipOutputStream zos = new ZipOutputStream(fos);
            zos.setLevel(Deflater.BEST_COMPRESSION);

            File fInputFile;
            String sTemp;

            BroadcastProgress(true, "Beginning export.",
                    true, giProgressBarValue);


            //Loop through the list of zip entries and add them to the zip file.
            assert alZipList != null;
            for(int i = 0; i < alZipList.size(); i++){

                //Get an entry from the list of files/folders to zip:
                sTemp = alZipList.get(i);
                fInputFile = new File(sTemp);


                if(alZipList.get(i).endsWith(File.separator)){
                    //If the entry ends with a file separator character, then
                    //  it is a folder.
                    BroadcastProgress(true, "Adding folder " + fInputFile + " to zip file.",
                            false, 0);
                    zipDirectory(fInputFile,  zos);
                } else {
                    //Otherwise it is a single file.

                    zipSingleFile(fInputFile,  zos);

                    //Provide user with progress update:
                    giProgressCounter++;
                    //Update with every comic page processed:
                    giProgressBarValue = Math.round((giProgressCounter / (float) giComicFileCount) * 100);
                    BroadcastProgress(true, "Added file " + fInputFile + " to zip file.",
                            true, giProgressBarValue);

                }

            }

            zos.close();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        BroadcastProgress(true, "Export complete.",
                true, 100);

    }

    public void BroadcastProgress(boolean bUpdateLog, String sLogLine,
                                  boolean bUpdatePercentComplete, int iAmountComplete){

        //Broadcast a message to be picked-up by the Import Activity:
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ExportComicsActivity.ExportResponseReceiver.EXPORT_ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

        broadcastIntent.putExtra(UPDATE_LOG_BOOLEAN, bUpdateLog);
        String s = globalClass.GetTimeStampReadReady() + ": " + sLogLine + "\n";
        broadcastIntent.putExtra(LOG_LINE_STRING, s);
        broadcastIntent.putExtra(UPDATE_PERCENT_COMPLETE_BOOLEAN, bUpdatePercentComplete);
        broadcastIntent.putExtra(PERCENT_COMPLETE_INT, iAmountComplete);

        sendBroadcast(broadcastIntent);
    }

    //Zip reference: https://www.journaldev.com/957/java-zip-file-folder-example

    private void zipDirectory(File dir, ZipOutputStream zos) {
        try {
            //Get the folder to create in the zip file:
            String sDirName = dir.getName();

            //Get all the file names from the directory to be included in the zip file:
            List<String> filesListInDir = populateFilesList(dir);

            //now zip files one by one:
            String sTemp;
            int i, j;
            for(String filePath : filesListInDir){
                System.out.println("Zipping "+filePath);

                //for ZipEntry we need to keep only relative file path, so we used substring on absolute path
                i = dir.getAbsolutePath().length()+1;
                j = filePath.length();
                sTemp = filePath.substring(i, j);
                sTemp = sDirName + File.separator + sTemp;
                ZipEntry ze = new ZipEntry(sTemp);
                zos.putNextEntry(ze);

                //read the file and write to ZipOutputStream
                FileInputStream fis = new FileInputStream(filePath);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                zos.closeEntry();
                fis.close();

                //Provide user with progress update:
                giProgressCounter++;
                //Update with every comic page processed:
                giProgressBarValue = Math.round((giProgressCounter / (float) giComicFileCount) * 100);
                BroadcastProgress(true, "Added file " + sTemp + " to zip file.",
                        true, giProgressBarValue);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private List<String> populateFilesList(File dir) {
        File[] files = dir.listFiles();
        List<String> filesListInDir = new ArrayList<>();
        assert files != null;
        for(File file : files){
            if(file.isFile()) filesListInDir.add(file.getAbsolutePath());
            else populateFilesList(file);
        }
        return filesListInDir;
    }


    private static void zipSingleFile(File file, ZipOutputStream zos) {
        try {

            //add a new Zip Entry to the ZipOutputStream
            ZipEntry ze = new ZipEntry(file.getName());
            zos.putNextEntry(ze);
            //read the file and write to ZipOutputStream
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            //Close the zip entry to write to zip file
            zos.closeEntry();
            //Close resources
            fis.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}


