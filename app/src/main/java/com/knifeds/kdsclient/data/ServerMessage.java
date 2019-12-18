package com.knifeds.kdsclient.data;

import android.os.Build;
import android.util.Log;

import com.knifeds.kdsclient.upgrade.UpdateInfo;
import com.knifeds.kdsclient.upgrade.UpdateResponse;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

// Enroll response:
//  { "type": "enroll",
//	  "command", "enrollResult",
//	  "response": <localized prompt message>,
//	  "status", "OK|ERROR", // "ERROR"
//	}
// Activated response:
//  { "type": "notify",
//	  "command": "activebypin",
//	  "uuid": <uuid>,
//	  "status": "OK"
//  }
// Unenroll request:
// 	{ "type": "notify",
//	  "command": "unenroll"
//	  "uuid": <uuid>,
//  }
//
public class ServerMessage {
    private static final String TAG = "ServerMessage";
    public enum Result {
        None,
        EnrollSucceeded,
        ScheduleReceived,
        SchedulePlaylistReceived,
        PlaylistReceived,
        UnenrollReceived,
        ScreenShotReceived,
        ClientDownloadReceived,
        ClientUpgradeReceived,
        LicenseInstallReceived,
        ClientDownloadSucceeded,
        RestartReceived,
        ShutdownReceived,
        UnloadPlaylistReceived,
        ClearCacheReceived,
        DeviceControlReceived,
        SetOnOffReceived,
    }

    public ServerMessage.Result result = ServerMessage.Result.None;

    public String duration;
    public String url;
    public Schedule schedule;
    public String token;
    public UpdateResponse updateResponse;
    public License license;
    public String commandid;
    public String uuid;
    public String angle;
    public Date onDate;
    public Date offDate;

    private DataContext dataContext = null;

    public ServerMessage(DataContext dataContext) {
        this.dataContext = dataContext;
    }

    public static String assembleEnrollMessage(final DataContext dataContext, final String pin) {
        String deviceUuid = dataContext.getDeviceUuid();
        return "{\"command\":\"enroll\"," +
                "\"uuid\":\"" + deviceUuid + "\"," +
                "\"OS\":\"" + dataContext.getSystemVersion() + "\"," +
                "\"ip\":\"" + dataContext.getIpAddress() + "\"," +
                "\"name\":\"" + Build.MODEL + "(..." + deviceUuid.substring(deviceUuid.length()-3) + ")\"," +
                "\"model\":\"" + dataContext.getHardwareModel() + "\"," +
                "\"position\":\"" + dataContext.getLocation() + "\"," +
                "\"resolution\":\"" + dataContext.getResolution() + "\"," +
                "\"info\":\"NeoPlay Android client\"," +
                "\"agentversion\":\"" + UpdateInfo.localVersionName + "\"," +
                "\"pin\":\"" + pin + "\"," +
                "\"capabilities\":\"execute" + dataContext.capabilities + "\"}";
    }

    public static String assembleHeartbeatMessage(final DataContext dataContext) {
        return "{\"type\":\"notify\"," +
                "\"command\":\"heartbeat\"," +
                "\"status\":\"" + dataContext.getDisplayStatus() + "\"," +
                "\"stateHash\":" + JSONObject.quote(dataContext.getDeviceStateHash()) + "," +
                "\"percentage\":\"" + dataContext.percentage + "\"," +
                "\"uuid\":\"" + dataContext.getDeviceUuid() + "\"}";
    }

    public ServerMessage build(MqttMessage message) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(message.toString());
            if (jsonObject != null) {
                // Logger.d("ServerMessageObject: " + serverMessageObject.toString());
                dataContext.saveDeviceState(jsonObject);
                handleCommand(jsonObject.getString("command"), jsonObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return this;
    }

    private void handleCommand(String command, JSONObject jsonObject) {
        try {
            if (command.equals("enrollResult")) {
                String status = jsonObject.getString("status");
                if (status.equals("OK")) {
                    result = ServerMessage.Result.EnrollSucceeded;
                } else {
                    if (status.equals("ERROR")) {
                        String response = jsonObject.getString("response");
                        if (response.equals("10001")) {
                            // Typically means need to activate by pin
                            result = ServerMessage.Result.None;
                        }
                    }
                }
                Log.d(TAG, "handleCommand: Command Result: Device Activation: " + result);
                return;
            }
            if (command.equals("play-schedule")) {
                String commandid = jsonObject.getString("commandid");
                JSONObject parameter = new JSONObject(jsonObject.getString("parameter"));

                String type = jsonObject.getString("type");
                String uuid = jsonObject.getString("uuid");

                JSONObject scheduler = parameter.getJSONObject("scheduler");
                JSONObject playList = scheduler.getJSONObject("playList");
                JSONArray mediaList = playList.getJSONArray("mediaList");

                for (int i = 0, size = mediaList.length(); i < size; i++) {
                    JSONObject media = mediaList.getJSONObject(i);
                    duration = media.getString("duration");
                    url = media.getString("url");
                }

                Log.d(TAG, "handleCommand: play-schedule: " + url);

                result = ServerMessage.Result.ScheduleReceived;
                return;
            }
            if (command.equals("playlist")) {
                String commandid = jsonObject.getString("commandid");
//                JSONObject parameter = new JSONObject(jsonObject.getString("parameter"));
//                String type = jsonObject.getString("type");
//                String uuid = jsonObject.getString("uuid");
//                JSONObject scheduler = parameter.getJSONObject("scheduler");
                schedule = Schedule.parse(jsonObject.getString("parameter"));
                result = ServerMessage.Result.SchedulePlaylistReceived;
                return;
            }
            if (command.equals("activebypin")) {
                String type = jsonObject.getString("type");
                if (type.equals("notify")) {
                    result = ServerMessage.Result.EnrollSucceeded;
                }
                return;
            }
//            if (command.equals("playlist")) {
//                Playlist = Playlist.parse(jsonObject.getString("parameter"));
//                result = Result.PlaylistReceived;
//            }
            if (command.equals("device_screenshot")) {
                JSONObject parameter = new JSONObject(jsonObject.getString("parameter"));
                token = parameter.getString("token");
                result = ServerMessage.Result.ScreenShotReceived;
                return;
            }
            if (command.equals("client_download")) {
                commandid = jsonObject.getString("commandid");
                uuid = jsonObject.getString("uuid");
                JSONObject parameter = new JSONObject(jsonObject.getString("parameter"));
                updateResponse = UpdateResponse.parse( jsonObject.getString("parameter"));
                result = ServerMessage.Result.ClientDownloadReceived;
                return;
            }
            if (command.equals("client_download_result")) {
                commandid = jsonObject.getString("commandid");
                uuid = jsonObject.getString("uuid");
                result = ServerMessage.Result.ClientDownloadSucceeded;
                return;
            }
            if (command.equals("client_upgrade")) {
                commandid = jsonObject.getString("commandid");
                uuid = jsonObject.getString("uuid");
                JSONObject parameter = new JSONObject(jsonObject.getString("parameter"));
                updateResponse = UpdateResponse.parse( jsonObject.getString("parameter"));
                result = ServerMessage.Result.ClientUpgradeReceived;
                return;
            }
            if (command.equals("license_install")) {
                commandid = jsonObject.getString("commandid");
                uuid = jsonObject.getString("uuid");
                license = License.parse( jsonObject.getString("parameter"));
                result = ServerMessage.Result.LicenseInstallReceived;
                return;
            }
            if (command.equals("restart")) {
                result = ServerMessage.Result.RestartReceived;
                return;
            }
            if (command.equals("shutdown")) {
                result = ServerMessage.Result.ShutdownReceived;
                return;
            }
            if (command.equals("unloadPlaylist")) {
                result = ServerMessage.Result.UnloadPlaylistReceived;
                return;
            }
            if (command.equals("heartbeat")) {
                result = ServerMessage.Result.None;
                return;
            }
            if (command.equals("clearcache")) {
                result = ServerMessage.Result.ClearCacheReceived;
                return;
            }
            if (command.equals("devicecontrol")) {
                JSONObject parameter = new JSONObject(jsonObject.getString("parameter"));
                angle = parameter.getString("angle");
                result = ServerMessage.Result.DeviceControlReceived;
                return;
            }
            if (command.equals("setonoff")) {
                JSONObject parameter = new JSONObject(jsonObject.getString("parameter"));
                // Format:\"2019-11-03T00:00:00\"
                String onDateStr = parameter.getString("on");
                String offDateStr = parameter.getString("off");

                try {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    onDate = df.parse(onDateStr);
                    offDate = df.parse(offDateStr);

                    result = ServerMessage.Result.SetOnOffReceived;
                } catch (ParseException parseEx) {
                    parseEx.printStackTrace();
                }
                return;
            }
            // TODO: Handle other commands
            Log.w(TAG, "handleCommand: Unhandled command: " + command);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
