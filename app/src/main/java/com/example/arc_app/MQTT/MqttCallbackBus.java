package com.example.arc_app.MQTT;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import static com.example.arc_app.MainActivity.handler_MQTT_message_receive;

public class MqttCallbackBus implements MqttCallback {
    public static JSONObject messageSource;
    private String TAG = "MqttCallbackBus";

    public MqttCallbackBus(){

    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d(TAG, "Connection lost :　" + cause );
        Log.e(TAG, cause.getMessage());
        Message message = new Message();
        message.what = 0;
        responseToHandler(message);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message_) {
        //將message_.getPayload()儲存的byte array轉成String
        String mes = new String(message_.getPayload());
        try {
            //將String 轉成 JSON格式
            messageSource = new JSONObject(mes);
            Log.d("mqtt", "messageArrived: gameInit");

            Message message = new Message();
            message.what = 0;

            Bundle bundle = new Bundle();

            message.setData(bundle);
            responseToHandler(message);

        }catch (JSONException e){
            Log.d(TAG, "messageArrived_ : " + e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG,"Callback Delivery Complete");
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
