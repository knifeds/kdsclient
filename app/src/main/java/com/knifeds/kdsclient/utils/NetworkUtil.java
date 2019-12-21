package com.knifeds.kdsclient.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;

        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo == null)
            return false;

        return netInfo.isConnected();
    }
}