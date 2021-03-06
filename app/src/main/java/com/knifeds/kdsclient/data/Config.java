package com.knifeds.kdsclient.data;

import android.content.Context;
import android.content.Intent;

import com.knifeds.kdsclient.utils.ConfigHelper;

import java.util.List;

public class Config {
    public static String mqttUrl = "";
    public static String scepServerUrl = "";
    public static String screenShotPrefix = "";
    public static String hostedFileBaseUrl = "";

    public static String envText = "";
    public static boolean changeWwwToStaging = false;

    public static boolean useScep = false;
    public static boolean loadPlugins = false;

    public static String enrollTopic = "";
    public static String requestTopicPrefix = "";
    public static String responseTopicPrefix = "";
    public static String imageUrlPrefix = "";
    public static String scepClientFilename = "";
    public static String mqttCaFilename = "";
    public static String mqttUsername = "";
    public static String mqttPassword = "";

    public static List<ConfigX> configXs = null;

    public static void loadConfig(Context context) {
        mqttUrl = ConfigHelper.getConfigValue(context, "mqttUrl");
        scepServerUrl = ConfigHelper.getConfigValue(context, "scepServerUrl");
        screenShotPrefix = ConfigHelper.getConfigValue(context, "screenShotPrefix");
        hostedFileBaseUrl = ConfigHelper.getConfigValue(context, "hostedFileBaseUrl");

        enrollTopic = ConfigHelper.getConfigValue(context, "enrollTopic");
        requestTopicPrefix = ConfigHelper.getConfigValue(context, "requestTopicPrefix");
        responseTopicPrefix = ConfigHelper.getConfigValue(context, "responseTopicPrefix");
        imageUrlPrefix = ConfigHelper.getConfigValue(context, "imageUrlPrefix");
        scepClientFilename = ConfigHelper.getConfigValue(context, "scepClientFilename");
        mqttCaFilename = ConfigHelper.getConfigValue(context, "mqttCaFilename");
        mqttUsername = ConfigHelper.getConfigValue(context, "mqttUsername");
        mqttPassword = ConfigHelper.getConfigValue(context, "mqttPassword");

        configXs = ConfigX.loadConfig(context);
    }

    public static ConfigX getConfigSerivce() {
        for (ConfigX c: configXs) {
            if (c.type.equals("config")) return c;
        }
        return null;
    }

    public static void applyConfig(Intent intent) {
        Config.envText = intent.getStringExtra("envText");
        Config.mqttUrl = intent.getStringExtra("mqttUrl");
        Config.scepServerUrl = intent.getStringExtra("scepServerUrl");
        Config.screenShotPrefix = intent.getStringExtra("screenShotPrefix");
        Config.hostedFileBaseUrl = intent.getStringExtra("hostedFileBaseUrl");
        Config.changeWwwToStaging = intent.getBooleanExtra("changeWwwToStaging", false);
    }
}
