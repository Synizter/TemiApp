package com.virachlabo.temiapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class MqttWrapper {
    public MqttAndroidClient mqttAndroidClient;

    final String serverUri = "tcp://babyai.org:1883";

    final String username = "wifimod";
    final String password = "PeEFc9Aq";

    private String clientID;
    private String TAG = ": mqtt-service :";
    private String topic;

    private enum LOG_LEVEL {
        VERBOSE, WARNING, ERROR, INFO
    }

    public MqttWrapper(Context context, String t, String id) {
        topic = t;
        clientID = id;
        TAG = clientID + TAG;

        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientID);
        connect();
    }

    //MQTT Callback
     public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
     }

     private void connect() {
         MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
         mqttConnectOptions.setCleanSession(true);
         mqttConnectOptions.setUserName(username);
         mqttConnectOptions.setPassword(password.toCharArray());
         try {
             mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                 @Override
                 public void onSuccess(IMqttToken asyncActionToken) {
                     DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                     disconnectedBufferOptions.setBufferEnabled(true);
                     disconnectedBufferOptions.setBufferSize(100);
                     disconnectedBufferOptions.setPersistBuffer(false);
                     disconnectedBufferOptions.setDeleteOldestMessages(false);
                     Log.w(TAG, "Connect to " + serverUri);
                     _log("The client is successfully connected to " + mqttConnectOptions.getServerURIs().toString(), LOG_LEVEL.INFO);

                     subscribeToTopic(topic);

                 }
                 @Override
                 public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    _log("Cannot connect " + serverUri + " with exception " + exception.toString(), LOG_LEVEL.ERROR);
                 }
             });
         } catch (MqttException e) {
             e.printStackTrace();
         }

     }
     private void subscribeToTopic(String subTopic) {
        try {
            mqttAndroidClient.subscribe(subTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    _log("Successfully subscribe to topic " + subTopic, LOG_LEVEL.INFO);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    _log("fail to subscribe exception" + exception.toString(), LOG_LEVEL.ERROR);
                }
            });
        }catch (MqttException ex) {
            ex.printStackTrace();
        }
     }
     public void reconnect() {
        connect();
     }

     public void publish(String topic, String msg) {
         MqttMessage mqttMessage = new MqttMessage(msg.getBytes());
        if(!mqttAndroidClient.isConnected()) {
            reconnect();
        }
        try {
            mqttAndroidClient.publish(topic, mqttMessage);
        }
        catch (MqttException e) {
            e.printStackTrace();
        }
     }

    public void _log(String msg, LOG_LEVEL level) {
        switch (level) {
            case ERROR:
                Log.e(this.TAG, msg);
                break;
            case WARNING:
                Log.w(this.TAG, msg);
                break;
            case VERBOSE:
                Log.v(this.TAG, msg);
                break;
            case INFO:
                Log.i(this.TAG, msg);
                break;
        }
    }
}
