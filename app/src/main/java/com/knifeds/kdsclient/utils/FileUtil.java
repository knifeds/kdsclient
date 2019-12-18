package com.knifeds.kdsclient.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class FileUtil {

    public static void getFileFromAssets(Context context, String outputFolder, String filename) throws IOException {
        File outputFile = new File(outputFolder, filename);
        try {
            InputStream inputStream = context.getAssets().open(filename);
            try {
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new IOException("Could not open: " + filename, e);
        }
    }

    /**
     * Get the current base path from the jar package
     *
     * @return
     */
    public static String getBasePath() {
        String filePath = FileUtil.class.getProtectionDomain().getCodeSource().getLocation().getFile();


        if (filePath.endsWith(".jar")) {
            filePath = filePath.substring(0, filePath.lastIndexOf("/"));
            try {
                filePath = URLDecoder.decode(filePath, "UTF-8"); // Deal with space(%20) in the path
            } catch (UnsupportedEncodingException ex) {

            }

        }
        File file = new File(filePath);
        filePath = file.getAbsolutePath();
        return filePath;
    }
}
