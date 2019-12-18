package com.knifeds.kdsclient.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.knifeds.kdsclient.data.DataContext;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloader extends AsyncTask<String, String, String> {
    private static final String TAG = "FileDownloader";

    private DataContext dataContext = null;

    String saveAsFilename;
    private OnFileDownloaded listener;

    public FileDownloader(String saveAsFilename, OnFileDownloaded listener, DataContext dataContext) {
        this.saveAsFilename = saveAsFilename;
        this.listener = listener;
        this.dataContext = dataContext;
    }

    @Override
    protected String doInBackground(String... urls) {
        int count;
        try {
            URL url = new URL(urls[0]);
            Log.d(TAG, "doInBackground: Downloading from url: " + url);

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            int resCode = urlConnection.getResponseCode();
            if (200 == resCode) {
                int lengthOfFile = urlConnection.getContentLength();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String fullFilename = this.saveAsFilename;
                OutputStream out = new FileOutputStream(fullFilename);
                byte data[] = new byte[1024];
                long total = 0;

                while ((count = in.read(data)) != -1) {
                    total += count;
                    int percentage = (int)((total * 100) / lengthOfFile);
                    publishProgress("" + percentage);
                    dataContext.percentage = percentage;
                    out.write(data, 0, count);
                }

                out.flush();
                out.close();
                in.close();

                // Something like: /data/user/0/com.knifeds.kdsclient/files/scepclient
                Log.d(TAG, "doInBackground: Received " + total + " bytes: " + fullFilename);
                return urls[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    protected void onProgressUpdate(String... progress) {
        EventBus.getDefault().post(new StatusMessage("Downloading content: " + Integer.parseInt(progress[0]) + "% done..."));
    }

    @Override
    protected void onPostExecute(String url) {
        EventBus.getDefault().post(new StatusMessage(""));
        dataContext.percentage = -1; // reset percentage
        listener.onFileDownloaded(url);
    }
}
