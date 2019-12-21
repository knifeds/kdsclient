package com.knifeds.kdsclient.data;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import com.knifeds.kdsclient.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

public class ConfigX {
    @Expose
    public String type;
    @Expose
    public String action;
    @Expose
    public String packageName;
    @Expose
    public String serviceName;
    @Expose
    public String className;
    @Expose
    public String fragmentName;
    @Expose
    public List<String> configValues;

    private static final String TAG = "ConfigX";

    public static List<ConfigX> loadConfig(Context context) {
        Resources resources = context.getResources();

        try {
            InputStream rawResource = resources.openRawResource(R.raw.configx);
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(rawResource, "UTF-8"));
//            StringBuilder responseStrBuilder = new StringBuilder();
//
//            String inputStr;
//            while ((inputStr = streamReader.readLine()) != null)
//                responseStrBuilder.append(inputStr);

            Gson gson = new Gson();
            List<ConfigX> configXs = gson.fromJson(streamReader, new TypeToken<List<ConfigX>>() {}.getType());
            return configXs;
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "getConfigValue failed: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "getConfigValue: failed to open config file");
        }

        return null;
    }
}
