package com.knifeds.kdsclient.data;

public class Config {

    // Default config
    public static String mqttUrl = "ssl://paas.neods.tech:10001"; // Public MQTT URL secured
    // FIXME: Must use ip address due to Go lib issue resolving hostname (always use ipv6 DNS)
    public static String scepServerUrl = "http://47.244.241.96:10004/scep";
    public static String screenShotPrefix = "https://api.neods.tech/dss/device/uploadImage/";
    public static String hostedFileBaseUrl = "https://api.neods.tech/scepclient/";

    public static String envText = "";
    public static boolean changeWwwToStaging = false;

    public static boolean useScep = false;
    public static boolean loadPlugins = false;

    // Fix values
    public static final String enrollTopic = "newdevice/enroll";
    public static final String requestTopicPrefix = "server/device/request/";
    public static final String responseTopicPrefix = "server/device/response/";
    public static final String imageUrlPrefix = "https://api.neods.tech"; // Note: no trailing /
    public static final String scepClientFilename = "scepclient";
    public static final String mqttCaFilename = "mqtt_ca.crt"; // See: https://gist.github.com/sharonbn/4104301
    public static final String mqttUsername = "device";
    public static final String mqttPassword = "Kds1213";
}
