package com.knifeds.kdsclient.mqtt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.knifeds.kdsclient.data.Config;
import com.knifeds.kdsclient.data.DataContext;
import com.knifeds.kdsclient.data.StateChanged;
import com.knifeds.kdsclient.utils.SslUtil;
import com.knifeds.kdsclient.utils.StatusMessage;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.net.SocketFactory;

import dagger.android.AndroidInjection;

public class MqttService extends Service {
    private static final String TAG = "MqttService";

    @Inject
    DataContext dataContext;

    @Inject MqttManager mqttManager;

    volatile boolean connected = false;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //mqttUrl = intent.getStringExtra("mqttUrl");
        if (connected) {
            EventBus.getDefault().post(new StateChanged(StateChanged.State.MqttServiceReady));
            Log.d(TAG, "onStartCommand: Connected to MQTT.");
        }
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);

        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                connected = initMqtt();
                if (connected) {
                    EventBus.getDefault().post(new StateChanged(StateChanged.State.MqttServiceReady));
                    Log.d(TAG, "run: Connected to MQTT.");
                    scheduler.shutdown();
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private boolean initMqtt() {
        try {
            boolean b;

            String mqttUrl = dataContext.getMqttUrl();

            Log.d(TAG, "initMqtt: Trying to connect to MQTT at: " + mqttUrl);

            if (mqttUrl.startsWith("ssl://")) {
                SocketFactory socketFactory = SslUtil.getSocketFactory(dataContext.getMqttCaFilePath(), dataContext.getClientCertFilePath(), dataContext.getClientKeyFilePath(),"");
                b = mqttManager.createConnect(mqttUrl, Config.mqttUsername, Config.mqttPassword, dataContext.mqttClientId, socketFactory);
            } else {
                b = mqttManager.createConnect(mqttUrl, Config.mqttUsername, Config.mqttPassword, dataContext.mqttClientId, null);
            }

            if (!b)
                return false;

            b = mqttManager.subscribe(Config.requestTopicPrefix + dataContext.getDeviceUuid(), 2);
            Log.d(TAG, "initMqtt: Subscribed to requestTopic: " + b);

            b = mqttManager.subscribe(Config.responseTopicPrefix + dataContext.getDeviceUuid(), 2);
            Log.d(TAG, "initMqtt: Subscribed to responseTopic: " + b);

        } catch(Exception e) {
            Log.d(TAG, "initMqtt: Error: " + e.getMessage());
            EventBus.getDefault().post(new StatusMessage(e.getMessage()));
            return false;
        }

        return true;
    }

    @Override
    public void onDestroy() {
        try {
            Log.d(TAG, "onDestroy: Disconnecting MQTT...");
            mqttManager.disConnect();
        } catch (MqttException e) {
            Log.d(TAG, "onDestroy: Unable to disconnect: " + e.getMessage());
        }

    }
}
