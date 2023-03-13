package com.agcurations.aggallerymanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Worker_FFMPEG_Operation extends Worker {


    public Worker_FFMPEG_Operation(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {


        //This routine was created to preserve FFMPEG coding that was successful in concatenating
        //  video files. Work on this has been halted due to lack of use. It may be used again in
        //  the future. The code was moved here during major modifications to align with the
        //  Android Storage Access Framework. To get it to work again, coding will have to be added
        //  to move the files to be worked upon to the app's internal storage space where File
        //  objects and standard operating paths are valid, as opposed to using DocumentFile objects.
        //File name should not be "Jumbled" as if it is a .ts file download of videos, FFMPEG will
        //  not understand what to do with the files if the extension is unrecognized.



        /*if(giDownloadTypeSingleOrM3U8 != DOWNLOAD_TYPE_M3U8_LOCAL) {
            //Create a file listing the files which are to be concatenated:
            String sFFMPEGInputFilename = "FFMPEGInputFileName.txt";
            DocumentFile dfFFMPEGInputFile = dfOutputFolder.createFile(MimeTypes.BASE_TYPE_TEXT, sFFMPEGInputFilename);
            if(dfFFMPEGInputFile == null){
                sMessage = "Unable to create FFMPEG input file.";
                LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                return Result.failure(DataErrorMessage(sMessage));
            }


            //Sort the file names:
            bwLogFile.write("Sorting file names to be concatenated..." + "\n");
            bwLogFile.flush();
            TreeMap<Integer, String> tmDownloadedFiles = new TreeMap<>();
            for (DocumentFile df : dfDownloadedFiles) {
                for (int j = 0; j < gsFilenameSequence.length; j++) {
                    if (df.isFile()) {
                        if(df.getName() != null) {
                            if (!df.getName().contains(VIDEO_DLID_AND_SEQUENCE_FILE_NAME)) {
                                String sFilename = df.getName();
                                if (sFilename.contains(gsFilenameSequence[j])) {
                                    tmDownloadedFiles.put(j, df.getUri().toString());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            bwLogFile.write("File names sorted." + "\n");
            bwLogFile.write("Writing list of files to file to use as input to FFMPEG..." + "\n");
            bwLogFile.flush();
            StringBuilder sbBuffer = new StringBuilder();
            String sTestFileAbsolutePath = "";
            for (Map.Entry<Integer, String> entry : tmDownloadedFiles.entrySet()) {
                sbBuffer.append("file '");
                sbBuffer.append(entry.getValue());
                if(sTestFileAbsolutePath.equals("")){
                    sTestFileAbsolutePath = entry.getValue();
                }
                sbBuffer.append("'\n");
            }
            bwLogFile.write("Finished.\nWriting data to file: " + dfFFMPEGInputFile.getUri() + "\n");
            bwLogFile.flush();
            //Write the data to the file:
            try {
                BufferedWriter bwFFMPEGInputFile;
                OutputStream osFFMPEGInputFile = contentResolver.openOutputStream(dfFFMPEGInputFile.getUri(), "wt");
                if(osFFMPEGInputFile == null){
                    sMessage = "Unable to write FFMPEG input file: " + dfFFMPEGInputFile.getUri();
                    LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                    return Result.failure(DataErrorMessage(sMessage));
                }
                bwFFMPEGInputFile = new BufferedWriter(new OutputStreamWriter(osFFMPEGInputFile));
                bwFFMPEGInputFile.write(sbBuffer.toString());
                bwFFMPEGInputFile.flush();
                bwFFMPEGInputFile.close();
                osFFMPEGInputFile.flush();
                osFFMPEGInputFile.close();
            } catch (IOException e) {
                e.printStackTrace();
                sMessage = "Unable to create FFMPEG input file: " + dfFFMPEGInputFile.getUri();
                LogReturnWithFailure(sMessage, osLogFile, bwLogFile);
                return Result.failure(DataErrorMessage(sMessage));
            }
            bwLogFile.write("File write completed." + "\n");
            bwLogFile.flush();


            if(globalClass.gbUseFFMPEGToMerge) {

                        *//*final String sConcatIntermediateOutputFilePath = sOutputFolderPath + File.separator + gsVideoOutputFilename;
                        String sFFMPEGLogFileName = gsItemID + "_" + GlobalClass.GetTimeStampFileSafe() + "_Video_FFMPEGLog.txt";
                        final DocumentFile dfFFMPEGLogFile = globalClass.gdfLogsFolder.createFile(MimeTypes.BASE_TYPE_TEXT, sFFMPEGLogFileName);
                        if(dfFFMPEGLogFile == null){
                            Toast.makeText(getApplicationContext(), "Unable to create log file to monitor FFMPEG file merge operation", Toast.LENGTH_SHORT).show();
                            flush
                            bwLogFile.close();
                            flush
                            osLogFile.close();
                            return Result.failure();
                        }

                        String sCommand = "-f concat -safe 0 -i " + sFFMPEGInputFilePath + " -c copy \"" + sConcatIntermediateOutputFilePath + "\"";

                        bwLogFile.write("Starting FFMPEG operation asynchronously. See FFMPEG log for process-related data." + "\n");
                        bwLogFile.write("Issuing command:\n" + sCommand + "\n");
                        bwLogFile.flush();
                        FFmpegKit.executeAsync(sCommand, new ExecuteCallback() {

                            @Override
                            public void apply(Session session) {
                                // CALLED WHEN SESSION IS EXECUTED
                                SessionState state = session.getState();
                                ReturnCode returnCode = session.getReturnCode();
                                //Write the data to the log file:
                                try {
                                    OutputStream osFFMPEGLogFile = GlobalClass.gcrContentResolver.openOutputStream(dfFFMPEGLogFile.getUri(), "wa");
                                    if(osFFMPEGLogFile == null){
                                        Toast.makeText(getApplicationContext(), "Unable to create log file to monitor FFMPEG file merge execution operation", Toast.LENGTH_SHORT).show();
                                    }
                                    //Could end up with an issue here if osFFMPEGLogFile is null. At the moment, this part of
                                    // the routine is not used - I am not "dogfooding" the FFMPEG merge component.
                                    BufferedWriter bwFFMPEGLogFile;
                                    bwFFMPEGLogFile = new BufferedWriter(new OutputStreamWriter(osFFMPEGLogFile));
                                    bwFFMPEGLogFile.write(String.format("\nExec message: FFmpeg process exited with state %s and return code %s.\n", state, returnCode) + "\n");

                                    if (ReturnCode.isSuccess(returnCode)) {
                                        //Attempt to move the output file:
                                        DocumentFile fFFMPEGOutputFile = new File(sConcatIntermediateOutputFilePath);
                                        DocumentFile fFinalOutputFile = new File(sFinalOutputPath);
                                        if (!fFFMPEGOutputFile.renameTo(fFinalOutputFile)) {
                                            String sMessage = "Exec message: Could not rename FFMPEG output file to final file name: " + fFFMPEGOutputFile.getAbsolutePath() + " => " + fFinalOutputFile.getAbsolutePath();
                                            bwFFMPEGLogFile.write(sMessage + "\n");
                                        }
                                    } else {
                                        //Attempt to move the output file:
                                        DocumentFile fFFMPEGOutputFile = new File(sConcatIntermediateOutputFilePath);
                                        DocumentFile fFinalOutputFile = new File(sFinalOutputPath);
                                        if (!fFFMPEGOutputFile.renameTo(fFinalOutputFile)) {
                                            String sMessage = "Exec message: Could not rename FFMPEG output file to final file name: " + fFFMPEGOutputFile.getAbsolutePath() + " => " + fFinalOutputFile.getAbsolutePath();
                                            bwFFMPEGLogFile.write(sMessage + "\n");
                                        }
                                    }
                                    String sMessage = session.getFailStackTrace();
                                    if (sMessage != null) {
                                        bwFFMPEGLogFile.write("Exec message: " + sMessage + "\n");
                                    }
                                    bwFFMPEGLogFile.close();
                                    if(osFFMPEGLogFile != null) {
                                        flush
                                        osFFMPEGLogFile.close();
                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new LogCallback() {

                            @Override
                            public void apply(com.arthenica.ffmpegkit.Log log) {
                                // CALLED WHEN SESSION PRINTS LOGS
                                String sMessage = log.getMessage();

                                //Write the data to the log file and rename the output file so that the main application can find it:
                                try {
                                    OutputStream osFFMPEGLogFile = GlobalClass.gcrContentResolver.openOutputStream(dfFFMPEGLogFile.getUri(), "wa");
                                    if(osFFMPEGLogFile == null){
                                        Toast.makeText(getApplicationContext(), "Unable to create log file to monitor FFMPEG file merge execution operation", Toast.LENGTH_SHORT).show();
                                    }
                                    //Could end up with an issue here if osFFMPEGLogFile is null. At the moment, this part of
                                    // the routine is not used - I am not "dogfooding" the FFMPEG merge component.
                                    BufferedWriter bwFFMPEGLogFile;
                                    bwFFMPEGLogFile = new BufferedWriter(new OutputStreamWriter(osFFMPEGLogFile));
                                    bwFFMPEGLogFile.write("Log message: " + sMessage + "\n");
                                    bwFFMPEGLogFile.close();
                                    if(osFFMPEGLogFile != null) {
                                        flush
                                        osFFMPEGLogFile.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        }, new StatisticsCallback() {

                            @Override
                            public void apply(Statistics statistics) {
                                // CALLED WHEN SESSION GENERATES STATISTICS
                                String sMessage = "File size: " + statistics.getSize();

                                //Write the data to the log file:
                                try {
                                    OutputStream osFFMPEGLogFile = GlobalClass.gcrContentResolver.openOutputStream(dfFFMPEGLogFile.getUri(), "wa");
                                    if(osFFMPEGLogFile == null){
                                        Toast.makeText(getApplicationContext(), "Unable to create log file to monitor FFMPEG file merge execution operation", Toast.LENGTH_SHORT).show();
                                    }
                                    //Could end up with an issue here if osFFMPEGLogFile is null. At the moment, this part of
                                    // the routine is not used - I am not "dogfooding" the FFMPEG merge component.
                                    BufferedWriter bwFFMPEGLogFile;
                                    bwFFMPEGLogFile = new BufferedWriter(new OutputStreamWriter(osFFMPEGLogFile));
                                    bwFFMPEGLogFile.write("Stat message: " + sMessage + "\n");
                                    bwFFMPEGLogFile.close();
                                    if(osFFMPEGLogFile != null) {
                                        flush
                                        osFFMPEGLogFile.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            }
                        });

                        bwLogFile.write("Success calling FFMPEG to concatenate files." + "\n");
                        bwLogFile.flush();*//*
                bwLogFile.write("Calling FFMPEG to concatenate files is disabled until " +
                        "FFmpegKit is compatible with the Android Storage Access Framework." + "\n");
                bwLogFile.flush();
                //Execute no further processing as the FFMPEG call is asynchronous.
            }
        }
        */

        return Result.success();
    }


}
