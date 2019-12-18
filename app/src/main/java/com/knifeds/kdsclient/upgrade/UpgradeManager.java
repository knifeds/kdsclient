package com.knifeds.kdsclient.upgrade;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.knifeds.kdsclient.data.Config;
import com.knifeds.kdsclient.data.Consts;
import com.knifeds.kdsclient.data.DataContext;
import com.knifeds.kdsclient.data.License;
import com.knifeds.kdsclient.data.ServerMessage;
import com.knifeds.kdsclient.data.StateChanged;
import com.knifeds.kdsclient.mqtt.MqttManager;
import com.knifeds.kdsclient.schedule.TimerMessage;
import com.knifeds.kdsclient.utils.HostedFiles;
import com.knifeds.kdsclient.utils.LicenseExtractor;
import com.knifeds.kdsclient.utils.MessageUtil;
import com.knifeds.kdsclient.utils.StatusMessage;
import com.knifeds.kdsclient.utils.TimeUtil;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UpgradeManager implements Observer {
    private static final String TAG = "UpgradeManager";

    public static final String CLIENT_UPGRADE_COMMANDID = "client_upgrade_commandid";
    public static final String CLIENT_UPGRADE_UUID = "client_upgrade_uuid";
    public static final String UPDATE_UPGRADE_KEY = "updateUpgradeKey";

    public static final String CLIENT_DOWNLOAD_COMMANDID = "client_download_commandid";
    public static final String CLIENT_DOWNLOAD_UUID = "client_download_uuid";
    public static final String UPDATE_DOWNLOAD_KEY = "updateDownloadKey";

    public static final String LICENSE_COMMANDID = "license_commandid";
    public static final String LICENSE_UUID = "license_uuid";
    public static final String LICENSE_KEY = "licenseKey";

    public static final String ACTION_DOWNLOAD_UPGRADE = "download_upgrade";
    public static final String ACTION_UPGRADE = "upgrade";
    public static final String ACTION_DOWNLOAD = "download";
    public static final String ACTION_LICENSE_DOWNLOAD = "licenseDownload";

    private static long _DOWNLOAD_APK_DELAY = 10;// download apk delay
    private static long _UPGRADE_APK_DELAY = 10;// download apk period

    @Inject
    DataContext dataContext;

    @Inject
    MqttManager mqttManager;

    @Inject
    public UpgradeManager() {}

    @Override
    public void update(Observable observable, Object o) {
        int tag = (int)o;
        switch(tag) {
            case Consts.O_LICENSE:
                License licenseParam = new Gson().fromJson(dataContext.getPrivateConfig(LICENSE_KEY,""), License.class);
                if (licenseParam != null)
                    publicKeyDownload(licenseParam.getPublicKeyPath());
                break;
            case Consts.O_PUBLICKEY:
                License licenseParam1 = new Gson().fromJson(dataContext.getPrivateConfig(LICENSE_KEY,""),  License.class);
                String commandid = dataContext.getPrivateConfig(LICENSE_COMMANDID,"");
                String uuid = dataContext.getPrivateConfig(LICENSE_UUID, "");
                boolean isValidLicense = false;
                try {
                    isValidLicense = LicenseExtractor.generate(uuid, dataContext.mLicensePath, dataContext.mPublicKeyPath, licenseParam1.getRandomSecret());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if(isValidLicense){
                    EventBus.getDefault().post(new StateChanged(StateChanged.State.LicenseOk));
                    sendLicenseCommand(commandid, uuid, licenseParam1);
                } else {
                    EventBus.getDefault().post(new StateChanged(StateChanged.State.LicenseError));
                }
                break;
        }
    }

    public void handleClientUpgrade(ServerMessage serverMessage) {
        dataContext.setPrivateConfig(CLIENT_UPGRADE_COMMANDID, serverMessage.commandid);
        dataContext.setPrivateConfig(CLIENT_UPGRADE_UUID, serverMessage.uuid);
        UpdateResponse upgradeResponse = serverMessage.updateResponse;
        dataContext.setUpdateResponse(UPDATE_UPGRADE_KEY, upgradeResponse);

        if (upgradeResponse.url.length() > 0) {
            scheduleClientUpgrade(upgradeResponse.upgradeTime, ACTION_DOWNLOAD_UPGRADE);
        } else {
            scheduleClientUpgrade(upgradeResponse.upgradeTime, ACTION_UPGRADE);
        }
    }

    public void handleClientDownload(ServerMessage serverMessage) {
        dataContext.setPrivateConfig(CLIENT_DOWNLOAD_COMMANDID, serverMessage.commandid);
        dataContext.setPrivateConfig(CLIENT_DOWNLOAD_UUID, serverMessage.uuid);
        UpdateResponse updateResponse = serverMessage.updateResponse;
        dataContext.setUpdateResponse(UPDATE_DOWNLOAD_KEY, updateResponse);

        scheduleClientUpgrade(updateResponse.downloadTime, ACTION_DOWNLOAD);
    }

    public void setLicenseCommand(ServerMessage serverMessage) {
        dataContext.setPrivateConfig(LICENSE_COMMANDID, serverMessage.commandid);
        dataContext.setPrivateConfig(LICENSE_UUID, serverMessage.uuid);
        dataContext.setPrivateConfig(LICENSE_KEY, new Gson().toJson(serverMessage.license));
    }

    public void sendClientDownloadResult() {
        if (!dataContext.hasPrivateConfig(CLIENT_DOWNLOAD_COMMANDID)){
            Log.e(TAG, "sendClientDownloadResult: No client download commandid found! Bail out");
            return;
        }

        String commandid = dataContext.getPrivateConfig(CLIENT_DOWNLOAD_COMMANDID,"");
        String uuid = dataContext.getPrivateConfig(CLIENT_DOWNLOAD_UUID,"");
        UpdateResponse updateResponse = dataContext.getUpdateResponse(UPDATE_DOWNLOAD_KEY);

        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("commandid",commandid);
            jsonObject.put("command","client_download_result");
            jsonObject.put("uuid",uuid);
            jsonObject.put("type","config");
            jsonObject.put("status","OK");
            JSONObject childJson = new JSONObject();
            childJson.put("version",updateResponse.getVersion());
            jsonObject.put("response",childJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(new Runnable() {
            public void run() {
                boolean b = mqttManager.publish(Config.requestTopicPrefix + dataContext.getDeviceUuid(), 2, jsonObject.toString().getBytes());
                Log.d(TAG, "run: sending downloaded apk to server: " + b);
            }
        }, 0, TimeUnit.MINUTES);
    }

    public void sendClientUpgradResult() {
        if (!dataContext.hasPrivateConfig(CLIENT_UPGRADE_COMMANDID)){
            Log.e(TAG, "sendClientUpgradResult: No client upgrade commandid found! Bail out");
            return;
        }

        String commandid = dataContext.getPrivateConfig(CLIENT_UPGRADE_COMMANDID,"");
        String uuid = dataContext.getPrivateConfig(CLIENT_UPGRADE_UUID,"");
        UpdateResponse updateResponse = dataContext.getUpdateResponse(UPDATE_UPGRADE_KEY);

        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("commandid",commandid);
            jsonObject.put("command","client_upgrade_result");
            jsonObject.put("uuid",uuid);
            jsonObject.put("type","config");
            jsonObject.put("status","OK");
            JSONObject childJson = new JSONObject();
            childJson.put("version",updateResponse.getVersion());
            jsonObject.put("response",childJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(new Runnable() {
            public void run() {
                boolean b = mqttManager.publish(Config.requestTopicPrefix + dataContext.getDeviceUuid(), 2, jsonObject.toString().getBytes());
                Log.d(TAG, "run: sending upgraded apk to server: " + b);
            }
        }, 0, TimeUnit.MINUTES);
    }

    public void sendLicenseCommand(String commandid, String uuid, License licenseParam) {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("commandid",commandid);
            jsonObject.put("command","license_install_result");
            jsonObject.put("uuid",uuid);
            jsonObject.put("status","OK");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(new Runnable() {
            public void run() {
                boolean b = mqttManager.publish(Config.requestTopicPrefix + dataContext.getDeviceUuid(), 2, jsonObject.toString().getBytes());
                Log.d(TAG, "run: sending install license to server: " + b);
            }
        }, 0, TimeUnit.MINUTES);
    }

    public void licenseDownload() {
        License license = new Gson().fromJson(dataContext.getPrivateConfig(LICENSE_KEY, ""), License.class);
        if (license == null)
            return;

        String url = license.getLicensePath();
        EventBus.getDefault().post(new StatusMessage("Downloading license from: " + url));

        HostedFiles hostedFiles = new HostedFiles(dataContext);
        hostedFiles.addObserver(this);
        hostedFiles.add(dataContext.mLicensePath, url);
        hostedFiles.checkAndFetchAll(Consts.O_LICENSE, Consts.O_LICENSE_FAILED, true); // FIXME
    }

    public void scheduleClientUpgrade(final String timeToRun, final String actionName) {
        if (actionName.equals(ACTION_UPGRADE) || actionName.equals(ACTION_DOWNLOAD_UPGRADE)) {
            _UPGRADE_APK_DELAY = TimeUtil.beforeSchedulerTimeMill(timeToRun);
            if (_UPGRADE_APK_DELAY <= 0) {
                _UPGRADE_APK_DELAY = 10;
            }
            MessageUtil.postMessageDelayed(new TimerMessage(Consts.TIMER_UPGRADE_APK, actionName), _UPGRADE_APK_DELAY);
        } else if (actionName.equals(ACTION_DOWNLOAD)) {
            if (TextUtils.isEmpty(timeToRun)) { // Download immediately if downloadTime is missing in upgrade
                _DOWNLOAD_APK_DELAY = 10;
            } else {
                _DOWNLOAD_APK_DELAY = TimeUtil.beforeSchedulerTimeMill(timeToRun);
            }
            if (_DOWNLOAD_APK_DELAY <= 0) {
                _DOWNLOAD_APK_DELAY = 10;
            }
            MessageUtil.postMessageDelayed(new TimerMessage(Consts.TIMER_DOWNLOAD_APK, actionName), _DOWNLOAD_APK_DELAY);
        }
    }

    private void publicKeyDownload(String publicKeyPath) {
        String url = publicKeyPath;
        EventBus.getDefault().post(new StatusMessage("Downloading license key from: " + url));

        HostedFiles hostedFiles = new HostedFiles(dataContext);
        hostedFiles.addObserver(this);
        hostedFiles.add(dataContext.mPublicKeyPath, url);
        hostedFiles.checkAndFetchAll(Consts.O_PUBLICKEY, Consts.O_PUBLICKEY_FAILED, true); // FIXME
    }
}
