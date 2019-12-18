package com.knifeds.kdsclient.utils;

import android.util.Log;

import org.json.JSONObject;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FileUploader {
    private static final String TAG = "FileUploader";

    public void uploadFile(File file, String uploadUrl) {
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"), file)) .build();
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("Content-Type","multipart/form-data")
                    .post(requestBody)
                    .build();
            if (client == null)
                client = new OkHttpClient();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "uploadFile: File uploading failed with Response:" + response.code());
            } else {
                String jsonResponse = response.body().string();
                if (jsonResponse == null) {
                    Log.e(TAG, "uploadFile: Invalid response. File uploading considered to be failed!");
                } else {
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    String result  = jsonObject.getString("result");
                    String status = jsonObject.getString("status");
                    if(status.equals("ok")){
                        // Delete the file after successful upload
                        if (file.isFile() && file.exists()) {
                            file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "uploadFile: File uploading failed with Exception:");
            e.printStackTrace();
        }
    }

    private OkHttpClient client;
}

