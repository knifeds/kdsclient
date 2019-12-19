package com.knifeds.kdsclient.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.knifeds.kdsclient.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigHelper {
    private static final String TAG = "ConfigHelper";

    public static String getConfigValue(Context context, String name) {
        Resources resources = context.getResources();

        try {
            InputStream rawResource = resources.openRawResource(R.raw.config);
            Properties properties = new Properties();
            properties.load(rawResource);
            return properties.getProperty(name);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "getConfigValue failed: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "getConfigValue: failed to open config file");
        }

        return null;
    }
}
