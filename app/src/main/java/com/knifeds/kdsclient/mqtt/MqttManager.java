package com.knifeds.kdsclient.mqtt;

import android.util.Log;

import com.knifeds.kdsclient.data.DataContext;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.SocketFactory;

@Singleton
public class MqttManager {
    private static final String TAG = "MqttManager";

    @Inject
    DataContext dataContext;

    private MqttCallback mCallback;

    // Private instance variables
    private MqttClient client;
    private MqttConnectOptions conOpt;
    private boolean clean = true;

    @Inject
    public MqttManager() {
        Log.i(TAG, "MqttManager: " + "++++ constructor.");
        mCallback = new MqttCallbackBus(true);
    }

//    public static void release() {
//        try {
//            if (mInstance != null) {
//                mInstance.disConnect();
//                mInstance = null;
//            }
//        } catch (Exception e) {
//
//        }
//    }

    public boolean createConnect(String brokerUrl, String userName, String password, String clientId, SocketFactory socketFactory) {
        boolean flag = false;
        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

        try {
            // Construct the connection options object that contains connection parameters
            // such as cleanSession and LWT
            conOpt = new MqttConnectOptions();
            conOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            conOpt.setCleanSession(clean);
            if (password != null) {
                conOpt.setPassword(password.toCharArray());
            }
            if (userName != null) {
                conOpt.setUserName(userName);
            }

            if (socketFactory != null) {
                conOpt.setSocketFactory(socketFactory);
                // Note: If we use Paho 1.2.1 then the following lines need to be uncommented
                // However, Paho 1.2.1 requires a newer Android SDK, probably > 8, otherwise:
                // Didn't find class "javax.net.ssl.SNIHostName"
                // See this: https://github.com/eclipse/paho.mqtt.java/issues/633
                //conOpt.setHttpsHostnameVerificationEnabled(false);
            }

            // Construct an MQTT blocking mode client
            client = new MqttClient(brokerUrl, clientId, dataStore);

            // Set this wrapper as the callback handler
            client.setCallback(mCallback);
            flag = doConnect();
        } catch (MqttException e) {
            Log.e(TAG, "createConnect: " + e.getMessage());
        }

        return flag;
    }

    public boolean doConnect() {
        boolean flag = false;
        if (client != null) {
            try {
                client.connect(conOpt);
                Log.d(TAG, "✅doConnect: " + client.getServerURI() + " with client ID " + client.getClientId());
                flag = true;
            } catch (Exception e) {
                Log.e(TAG, "❌doConnect: " + "Unable to connect to MQTT: " + e.getMessage());
            }
        }
        return flag;
    }

    public boolean publish(String topicName, int qos, byte[] payload) {

        boolean flag = false;

        if (client != null && client.isConnected()) {
            if (dataContext.debugMqtt) {
                Log.d(TAG, "publish: " + "Publishing to topic \"" + topicName + "\" qos " + qos);
            }

            // Create and configure a message
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);

            // Send the message to the server, control is not returned until
            // it has been delivered to the server meeting the specified
            // quality of service.
            try {
                client.publish(topicName, message);
                flag = true;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "???? - publish: " + "No connected client!");
        }

        return flag;
    }

    public boolean subscribe(String topicName, int qos) {

        boolean flag = false;

        if (client != null && client.isConnected()) {
            // Subscribe to the requested topic
            // The QoS specified is the maximum level that messages will be sent to the client at.
            // For instance if QoS 1 is specified, any messages originally published at QoS 2 will
            // be downgraded to 1 when delivering to the client but messages published at 1 and 0
            // will be received at the same level they were published at.
            Log.d(TAG, "subscribe: " + "Subscribing to topic \"" + topicName + "\" qos " + qos);
            try {
                client.subscribe(topicName, qos);
                flag = true;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "???? - subscribe: " + "No connected client!");
        }

        return flag;

    }

    public void disConnect() throws MqttException {
        Log.d(TAG, "disConnect: ");
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }
}
