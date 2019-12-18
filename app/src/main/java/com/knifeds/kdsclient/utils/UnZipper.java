package com.knifeds.kdsclient.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.knifeds.kdsclient.data.Consts;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UnZipper extends Observable {
    private static final String TAG = "UnZip";
    private String zipFile, destinationFolder;

    public UnZipper (String zipFile, String destinationFoler) {
        this.zipFile = zipFile;
        this.destinationFolder = destinationFoler;
    }

    public void unzip () {
        Log.d(TAG, "Unzipping " + zipFile + " to " + destinationFolder);
        new UnZipTask().execute(zipFile, destinationFolder);
    }

    private class UnZipTask extends AsyncTask<String, Void, Boolean> {

        @SuppressWarnings("rawtypes")
        @Override
        protected Boolean doInBackground(String... params) {
            String filePath = params[0];
            String destinationPath = params[1];

            File archive = new File(filePath);
            try {
                ZipFile zipfile = new ZipFile(archive);
                deleteDirectory(destinationPath);
                for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    unzipEntry(zipfile, entry, destinationPath);
                }
                zipfile.close();
                if(archive.exists())archive.delete();
            } catch (Exception e) {
                Log.e(TAG, "Error while extracting file " + archive, e);
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                setChanged();
                notifyObservers(Consts.O_UNZIPPED);
            } else {
                setChanged();
                notifyObservers(Consts.O_UNZIPPED_FAILED);
            }
        }

        private void unzipEntry(ZipFile zipfile, ZipEntry entry,
                                String outputDir) throws IOException {

            if (entry.isDirectory()) {
                createDir(new File(outputDir, entry.getName()));
                return;
            }

            File outputFile = new File(outputDir, entry.getName());
            if (!outputFile.getParentFile().exists()) {
                createDir(outputFile.getParentFile());
            }

//            Log.v(TAG, "Extracting: " + entry);
            BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

            try {
                IOUtils.copy(inputStream, outputStream);
            } finally {
                outputStream.close();
                inputStream.close();
            }
        }

        private void createDir(File dir) {
            if (dir.exists()) {
                return;
            }
//            Log.v(TAG, "Creating dir " + dir.getName());
            if (!dir.mkdirs()) {
                throw new RuntimeException("Can not create dir " + dir);
            }
        }
    }

    /**
     * Delete folder and files in the folder
     *
     * @param sPath folder path to be deleted
     * @return return true on success, otherwise return false
     */
    public static boolean deleteDirectory(String sPath) {
        // if sPath doesn't end with file separator, append it
        if (!sPath.endsWith(File.separator)) {
            sPath = sPath + File.separator;
        }
        File dirFile = new File(sPath);
        // if the dir doesn't exist or if not a folder, bail out.
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        // delete all files in the folder (including sub-folders)
        File[] files = dirFile.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
//            if (files[i].getName().equals("YJHtml.zip")){
//                continue;
//            }
            if (files[i].isFile()) { // delete files
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag)
                    break;
            } else { // delete sub-folders
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag)
                    break;
            }
        }
        if (!flag)
            return false;
        // delete current folder
        if (dirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }
    /**
     * delete single file
     *
     * @param sPath name of the file to be deleted
     * @return return true on success, otherewise return false
     */
    public static boolean deleteFile(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }
}
