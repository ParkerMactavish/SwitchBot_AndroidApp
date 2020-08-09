package com.example.arc_app.MQTT;

import android.os.Message;
import android.util.Log;


import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import static com.example.arc_app.MainActivity.handler_MQTT_message_receive;

public class MqttManager {
    // 單例
    private static MqttManager mInstance = null;
    // 回調
    private MqttCallback mCallback;

    // Private instance variables
    private MqttClient client;
    private MqttConnectOptions conOpt;
    private boolean clean = true;
    //判斷有無連線
    public boolean isConnect=false;
    public boolean isSubscribe=false;
    public String TAG = "MqttManager";

    private MqttManager() {
        mCallback = new MqttCallbackBus();
    }

    public static MqttManager getInstance() {
        if (null == mInstance) {
            mInstance = new MqttManager();
        }
        return mInstance;
    }

    //釋放單例
    public static void release() {
        try {
            if (mInstance != null) {
                mInstance.disConnect();
                mInstance = null;
            }
        } catch (Exception e) {

        }
    }

    public boolean createConnection(String brokerUrl, String userName, String password, String clientId) {
        boolean flag = false;
        //Android預設暫存資料夾
        String tmpDir = System.getProperty("java.io.tmpdir");
        //設定MQTT資料儲存位置
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

        try {
            conOpt = new MqttConnectOptions();
            conOpt.setMaxInflight(100);
            conOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            conOpt.setCleanSession(clean);

            if (password != null) {
                conOpt.setPassword(password.toCharArray());
            }
            if (userName != null) {
                conOpt.setUserName(userName);
            }

            // Construct an handler_MQTT_message_receive blocking mode client
            client = new MqttClient(brokerUrl, clientId, dataStore);

            // Set this wrapper as the callback handler
            client.setCallback(mCallback);
            flag = doConnect();
            isConnect=true;

            Log.d(TAG, "Connection begin");
            Message message = new Message();
            message.what = 1;
            responseToHandler(message);

        } catch (MqttException e) {
            Log.d("debug", "exception " + e.getMessage() );
        }
        return flag;
    }

    public boolean doConnect() {
        boolean flag = false;
        if (client != null) {
            try {
                client.connect(conOpt);
                Log.d(TAG,"Connected to " + client.getServerURI() + " with client ID " + client.getClientId());
                flag = true;
            } catch (Exception e) {
                Log.d("debug", "exception" + e.getMessage() );
            }
        }
        return flag;
    }

    public boolean publish(String topicName, int qos, byte[] payload) {

        boolean flag = false;

        if (client != null && client.isConnected()) {
            Log.d(TAG,"Publishing to topic \"" + topicName + "\" qos " + qos);

            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            try {
                client.publish(topicName, message);
                flag = true;
                Log.d("mqtt","publish : " + message);
            } catch (MqttException e) {
                Log.d("mqtt","exception : "+ e.getMessage() );
            }
        }
        return flag;
    }

    public boolean subscribe(String topicName, int qos) {
        boolean flag = false;

        if (client != null && client.isConnected()) {
            Log.d(TAG, "Subscribing to topic \"" + topicName + "\" qos " + qos);
            try {
                client.subscribe(topicName, qos);
                isSubscribe=true;
                flag = true;
                Log.d("mqtt","subscribe : " + topicName);
            } catch (MqttException e) {
                Log.d("mqtt","exception : " + e);
            }
        }
        return flag;
    }

    public boolean unsubscribe(String topicName) {
        boolean flag = false;

        if (client != null && client.isConnected()) {
            Log.d(TAG, "Unsubscribing to topic \"" + topicName );
            try {
                client.unsubscribe(topicName);
                isSubscribe=false;
                flag = true;
                Log.d("mqtt","unsubscribe");
            } catch (MqttException e) {
                Log.d("mqtt","exception : " + e.getMessage() );
            }
        }
        return flag;
    }

    public void disConnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            isConnect=false;
            Log.d("mqtt", "disConnect: ");
        }
    }

    public void responseToHandler(final Message message){
        new Thread(new Runnable(){
            @Override
            public void run() {
                handler_MQTT_message_receive.sendMessage(message);
            }
        }).start();
    }

}


