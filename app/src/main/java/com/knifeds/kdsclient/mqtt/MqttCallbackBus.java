package com.knifeds.kdsclient.mqtt;

import android.util.Log;

import com.knifeds.kdsclient.data.StateChanged;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

public class MqttCallbackBus implements MqttCallback {
    private static final String TAG = "MqttCallbackBus";

    boolean debugMqtt = false;

    public MqttCallbackBus(boolean debugMqtt) {
        this.debugMqtt = debugMqtt;
    }

    @Override
    public void connectionLost(Throwable cause) {
        if (debugMqtt) {
            Log.e(TAG, "connectionLost: " + cause.getMessage());
        }
        EventBus.getDefault().post(new StateChanged(StateChanged.State.MqttConnectionLost));
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        if (debugMqtt) {
            Log.d(TAG, "messageArrived: " + " ==> " + message.toString());
        }
        EventBus.getDefault().post(message);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        if (debugMqtt) {
            Log.d(TAG, "deliveryComplete: " + "  ");
        }
    }

}
