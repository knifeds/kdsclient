package com.knifeds.kdsclient.data;

import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.knifeds.kdsclient.upgrade.UpdateResponse;
import com.knifeds.kdsclient.utils.WifiUtils;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DataContext {
    private static final String TAG = "DataContext";
    public void init(SharedPreferences pref, SharedPreferences private_pref, final File filesDir) {

        maker = Build.MANUFACTURER;
        model = Build.MODEL;
        hardware = Build.HARDWARE;
        if (debugLevel == 4) {
            Log.i(TAG, "maker=" + maker + "; model=" + model + "; hardware=" + hardware);
        }

        this.pref = pref;
        this.private_perf = private_pref;
        setFilesDir(filesDir);

        if (pref.contains("env")) {
            env = pref.getString("env", "prod");
        } else {
            env = "prod";
        }

        if (pref.contains("content_downloaded")) {
            contentDownloaded = pref.getBoolean("content_downloaded", false);
        }

        if (pref.contains("uuid")) {
            deviceUuid = pref.getString("uuid", FALLBACK_DEVICE_ID);
            if (hasSavedDeviceUuid()) {
                configureNamesAndPaths();
            }
        }

        if (pref.contains("schedule_list")) {
            String val = pref.getString("schedule_list","[]");
            if (debugLevel == 4) {
                Log.i(TAG, "schedule_list" + ": " + val);
            }

            scheduleList = new Gson().fromJson(pref.getString("schedule_list","[]"),new TypeToken<List<Schedule>>() {}.getType());
        } else {
            scheduleList = new ArrayList<>();
        }

        if (pref.contains(SPK_IDLE_PLAYLIST)) {
            String val = pref.getString(SPK_IDLE_PLAYLIST,"");
            if (debugLevel == 4) {
                Log.i(TAG, SPK_IDLE_PLAYLIST + ": " + val);
            }

            idlePlaylist = new Gson().fromJson(pref.getString(SPK_IDLE_PLAYLIST,""),new TypeToken<Playlist>() {}.getType());
        } else {
            idlePlaylist = null;
        }

        if (pref.contains("state_hash")) {
            String val = pref.getString("state_hash","{}");
            if (debugLevel == 4) {
                Log.i(TAG, "state_hash" + ": " + val);
            }

            deviceStateHash = pref.getString("state_hash", "{}");
        } else {
            deviceStateHash = "{}";
        }

        if (pref.contains("activated")){
            activated = pref.getBoolean("activated", false);
        } else {
            activated = false;
        }

        if (pref.contains("angle")) {
            deviceAngle = pref.getString("angle", "0");
        } else {
            deviceAngle = "0";
        }
    }

    public String getDeviceUuid() {
        if (deviceUuid == null) {
            return FALLBACK_DEVICE_ID;
        }
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
        configureNamesAndPaths();

        SharedPreferences.Editor editor = pref.edit();
        editor.putString("uuid", this.deviceUuid);
        editor.commit();
    }

    public boolean hasSavedDeviceUuid() {
        return (deviceUuid != null && !deviceUuid.equals(FALLBACK_DEVICE_ID));
    }

    public String getDeviceStateHash() {
        return deviceStateHash;
    }

    public void saveDeviceState(final JSONObject jsonObject) {
        try {
            String command = jsonObject.getString("command");
            if (command.equals("playlist") || command.equals("devicecontrol")) {
                String parameter = jsonObject.getString("parameter");
                String type = jsonObject.getString("type");

                JsonObject stateObject = new JsonObject();
                stateObject.add("command", new JsonPrimitive(command));
                stateObject.add("parameter", new JsonPrimitive(parameter));
                stateObject.add("type", new JsonPrimitive(type));

                String stateJson = stateObject.toString();
                // Logger.d("stateJson = " + stateJson);

                Type hmType = new TypeToken<HashMap<String, String>>(){}.getType();
                HashMap<String, String> command2hash = new Gson().fromJson(deviceStateHash, hmType);
                command2hash.put(command, md5(stateJson));
                deviceStateHash = new Gson().toJson(command2hash);
            }
        } catch (Exception ex) {
            deviceStateHash = "{}";
        }
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("state_hash", deviceStateHash);
        editor.commit();
    }

    public void resetDeviceStateHash() {
        try {
            Type hmType = new TypeToken<HashMap<String, String>>(){}.getType();
            HashMap<String, String> command2hash = new Gson().fromJson(deviceStateHash, hmType);
            command2hash.put("playlist", "0");
            command2hash.put("devicecontrol", "0");
            deviceStateHash = new Gson().toJson(command2hash);
        } catch (Exception ex) {
            deviceStateHash = "{}";
        }

        SharedPreferences.Editor editor = pref.edit();
        editor.putString("state_hash", deviceStateHash);
        editor.commit();
    }

    String getDisplayStatus() {
        // 2: Idle，3: Playing
        if (scheduleList.size() == 0) {
            // No schedule
            return "2";
        }

        if (currentPlaylist == null || currentPlaylist.isIdlePlaylist()) {
            // No playlist or playlist is idlePlaylist
            return "2";
        }

        return "3";
    }

    String getIpAddress() {
        return WifiUtils.getIPAddress(true);
    }

    String getResolution() {
        return "" + width + "x" + height;
    }

    public void setResolution(int w, int h) {
        width = w;
        height = h;
    }

    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLocation() {
        return "" + latitude + "," + longitude;
    }

    public String getSystemVersion() {
        double release=Double.parseDouble(Build.VERSION.RELEASE.replaceAll("(\\d+[.]\\d+)(.*)","$1"));
        String codeName="Unsupported";//below Jelly bean OR above Oreo
        if(release>=4.1 && release<4.4)codeName="Jelly Bean";
        else if(release<5)codeName="Kit Kat";
        else if(release<6)codeName="Lollipop";
        else if(release<7)codeName="Marshmallow";
        else if(release<8)codeName="Nougat";
        else if(release<9)codeName="Oreo";
        else if(release<10)codeName="Pie";
        return "Android " + release; // + ", API Level: "+Build.VERSION.SDK_INT;
    }

    public List<Schedule> getScheduleList() {
        return scheduleList;
    }

    public void saveScheduleList() {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("schedule_list", getGson().toJson(scheduleList));
        editor.commit();
    }

    public void setIdlePlaylist(Playlist playlist) {
        idlePlaylist = playlist;
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(SPK_IDLE_PLAYLIST, getGson().toJson(idlePlaylist));
        editor.commit();
    }

    public Playlist getIdlePlaylist() {
        return idlePlaylist;
    }

    public void setContentDownloaded(boolean set) {
        contentDownloaded = set;
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("content_downloaded", contentDownloaded);
        editor.commit();
    }

    public boolean getContentDownloaded() {
        return contentDownloaded;
    }

    public String getScepServerUrl() {
        return Config.scepServerUrl;
    }

    public String getMqttUrl() {
        return Config.mqttUrl;
    }

    public String getScreenShotPrefix() {
        return Config.screenShotPrefix;
    }

    public boolean getActivated() { return activated; }

    public void setActivated(boolean activated) {
        this.activated = activated;
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("activated", this.activated);
        editor.commit();
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        if (this.env.equals(env))
            return;

        this.env = env;
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("env", this.env);
        editor.commit();
    }

    public void flipEnv() {
        if (env.equals("prod")) {
            setEnv("test");
        } else {
            setEnv("prod");
        }
    }

    public void setCurrentPlaylist(Playlist playlist) {
        currentPlaylist = playlist;
    }

    public Playlist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public void setPlaylistMd5(final String playlistId, final String plyalistMd5) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("playlist-" + playlistId, plyalistMd5);
        editor.commit();
    }

    public final String getPlaylistMd5(final String playlistId) {
        if (!pref.contains("playlist-" + playlistId)) {
            return "";
        }

        return pref.getString("playlist-" + playlistId, "");
    }

    public final String getDeviceAngle() {
        return deviceAngle;
    }

    public void setDeviceAngle(final String angle) {
        this.deviceAngle = angle;
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("angle", this.deviceAngle);
        editor.commit();
    }

    public boolean hasPrivateConfig(final String key) {
        return private_perf.contains(key);
    }

    public String getPrivateConfig(final String key, final String defaultVal) {
        return private_perf.getString(key, defaultVal);
    }

    public void setPrivateConfig(final String key, final String val) {
        SharedPreferences.Editor editor = private_perf.edit();
        editor.putString(key, val);
        editor.commit();
    }

    public UpdateResponse getUpdateResponse(final String key) {
        return new Gson().fromJson(private_perf.getString(key,""), UpdateResponse.class);
    }

    public void setUpdateResponse(final String key, UpdateResponse res) {
        SharedPreferences.Editor editor = private_perf.edit();
        editor.putString(key, new Gson().toJson(res));
        editor.commit();
    }

    public File getUpdateFile() {
        return new Gson().fromJson(private_perf.getString("updateFile",""), File.class);
    }

    public void setUpdateFile(File file) {
        SharedPreferences.Editor editor = private_perf.edit();
        editor.putString("updateFile", new Gson().toJson(file));
        editor.commit();
    }

    public String getHardwareModel() {
        return Build.MANUFACTURER + "-" + Build.MODEL + "-" + Build.HARDWARE;
    }

    public String getMqttCaFilePath() {
        return mqttCaFilePath;
    }

    public String getClientCertName() {
        return clientCertName;
    }

    public String getClientCertFilePath() {
        if (!Config.useScep) {
            return filesDir + "/mqtt_client.crt";
        }
        return clientCertFilePath;
    }

    public String getClientKeyFilePath() {
        if (!Config.useScep) {
            return filesDir + "/mqtt_client.key";
        }
        return clientKeyFilePath;
    }


    public volatile int percentage = -1;
    public String mqttClientId = "DsAndroidMQTTClient";
    public String scepClientFilePath;
    public String mLicensePath;
    public String mPublicKeyPath;
    public String filesDir;
    public String maker;
    public String model;
    public String hardware;
    public String capabilities;
    public boolean debugMqtt = true;
    public int debugLevel = 4;  // 0: Error only, 1: Warning, 2: Info, 3. Verbose, 4. All

    // Private
    private static final String SPK_IDLE_PLAYLIST="idle_playlist";
    private volatile String deviceUuid = null;
    private static final String FALLBACK_DEVICE_ID = "Fallback_Device_Id";
    private String deviceStateHash = null;
    private SharedPreferences pref = null;
    private SharedPreferences private_perf = null;
    private int width = 1920;
    private int height = 1080;
    private double latitude = 0;
    private double longitude = 0;
    private boolean contentDownloaded = false;
    private List<Schedule> scheduleList = null;
    private Playlist idlePlaylist = null;
    private Playlist currentPlaylist = null;
    private String env;
    private boolean activated = false;
    private String deviceAngle = "0";
    private String mqttCaFilePath;
    private String clientCertName;
    private String clientCertFilePath;
    private String clientKeyFilePath;

    //private constructor to avoid client applications to use constructor
    @Inject
    public DataContext(){}

    private void setFilesDir(final File filesDir) {
        this.filesDir = filesDir.toString();

        scepClientFilePath = filesDir + "/" + Config.scepClientFilename;
        mqttCaFilePath = filesDir + "/" + Config.mqttCaFilename;
    }

    private void configureNamesAndPaths() {
        mqttClientId = deviceUuid;
        clientCertName = deviceUuid;// + "_Cert";
        String certPathNoExt = filesDir + "/" + deviceUuid;
        clientCertFilePath = certPathNoExt + ".crt";
        clientKeyFilePath = certPathNoExt + ".key";
        mLicensePath = certPathNoExt + "_license.dat";
        mPublicKeyPath = certPathNoExt + "_publickey.key";
    }

    private static final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private Gson getGson() {
        GsonBuilder b = new GsonBuilder();
        return b.excludeFieldsWithoutExposeAnnotation().create();
    }
}

