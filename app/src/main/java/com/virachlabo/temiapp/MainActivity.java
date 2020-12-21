package com.virachlabo.temiapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.UserInfo;
import com.robotemi.sdk.exception.OnSdkExceptionListener;
import com.robotemi.sdk.exception.SdkException;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;
import com.robotemi.sdk.listeners.OnTelepresenceEventChangedListener;
import com.robotemi.sdk.listeners.OnTelepresenceStatusChangedListener;
import com.robotemi.sdk.model.CallEventModel;
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener;
import com.robotemi.sdk.navigation.model.Position;
import com.robotemi.sdk.telepresence.CallState;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.nio.charset.StandardCharsets;


/*
 * Contact List
 * Kobkrit:dade8fa3ac93fcf26340397be3d5840a
 * P'man:fe1090ed941db12ed1d350730031ea5b
 * พี่แพรSIIT:4990c18cea5e6604cc1adc384fe224e8
 * Aj Virach:67696f1ff709a3b0804ae43641ed8d85
 * */

public class MainActivity<RobotActionExecutorService> extends AppCompatActivity implements
        OnRobotReadyListener,
        Robot.TtsListener,
        Robot.WakeupWordListener,
        Robot.AsrListener,
        OnSdkExceptionListener,
        OnCurrentPositionChangedListener,
        OnTelepresenceEventChangedListener,
//        OnTelepresenceStatusChangedListener,
        OnGoToLocationStatusChangedListener {

    //Permission
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CODE_VOICE_RECOGNITION = 1001;

    private static final int REQUEST_CODE_NORMAL = 0;
    private static final int REQUEST_CODE_FACE_START = 1;
    private static final int REQUEST_CODE_FACE_STOP = 2;
    private static final int REQUEST_CODE_MAP = 3;
    private static final int REQUEST_CODE_SEQUENCE_FETCH_ALL = 4;
    private static final int REQUEST_CODE_SEQUENCE_PLAY = 5;
    private static final int REQUEST_CODE_START_DETECTION_WITH_DISTANCE = 6;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static String[] PERMISSION_AUDIO = {
            Manifest.permission.RECORD_AUDIO
    };

    //Object and Variable
    private Robot robot;
    private String temi_serial = "01234";
    //TODO
    //[] How to get serial number

    private SpeechRecognizer speechRecognizer; //STT
    Intent speechRecognizerIntent; //STT

    //MQTT Instance
    MqttWrapper mqttCmdInterface;

    //ActrionFlag
    Boolean isRobotActionComplete = true;
    private Context context_main;


    public static void verifyStroagePermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    public static void verifyAudioRecordPermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity, PERMISSION_AUDIO, REQUEST_CODE_VOICE_RECOGNITION);
            }
        }
    }

    public void RobotSpeak(String s, boolean isOverlayed) {
        robot.speak(TtsRequest.create(s, isOverlayed));
    }

    public void RobotSpeak_TH(String word) {
        //start http request
        try {
            String th_encode = URLEncoder.encode(word, "utf-8");
            String DOWNLOAD_URL = "https://tts-kaitom2.iapp.co.th/tts?text=" + th_encode;
            try {
                PlayAudioManager.playAudio(getApplicationContext(), DOWNLOAD_URL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        RobotActionComplete();

    }

    public void RobotActionComplete() {
        try {
            mqttCmdInterface.publish("actionState/", "done");
            isRobotActionComplete = true;
        }catch (Exception e) {
            e.printStackTrace();
            printLog(e.toString());
        }

    }

    public void GoogleSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this); //STT
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); //STT
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH"); //STT

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {
                speechRecognizer.stopListening();
            }

            @Override
            public void onError(int error) {

            }

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                printLog(data.get(0));
                RobotSpeak_TH(data.get(0));
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });

    }

    public void BUTTON_TEST_FOR_FEATURE() {
        Button button = findViewById(R.id.button_test);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                speechRecognizer.startListening(speechRecognizerIntent); //Get built-in STT working
//                robot.startTelepresence("TEST", "fe1090ed941db12ed1d350730031ea5b");

//                RobotActionComplete();
                robot.startTelepresence("TEST", "fe1090ed941db12ed1d350730031ea5b");
            }
        });
    }
    //Androod App callback -------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStroagePermission(this);
        verifyAudioRecordPermission(this);

        GoogleSpeechRecognizer();
        StartMqttCmdInterface();

        robot = Robot.getInstance(); // get an instance of the robot in order to begin using its features.
        robot.addOnSdkExceptionListener(this);
        robot.addOnCurrentPositionChangedListener(this);
        robot.addOnTelepresenceEventChangedListener(this);

        //TEST unit
        BUTTON_TEST_FOR_FEATURE();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        robot.addOnRobotReadyListener(this);
        robot.addTtsListener(this);
        robot.addWakeupWordListener(this);
        robot.addAsrListener(this);
        robot.addOnGoToLocationStatusChangedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // ------------------------------------------------------ Robot SDK Callvack -------------------------------------------------------------
    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                robot.onStart(activityInfo);
                robot.requestToBeKioskApp();


            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    public void onTtsStatusChanged(@NotNull TtsRequest ttsRequest) {
        String topic = "temi/" + temi_serial + "/speak-status";

        if (ttsRequest.getStatus() == TtsRequest.Status.COMPLETED) {
            printLog("DONE SEPAKING");
            mqttCmdInterface.publish(topic, "COMPLETED");
        }
        else if (ttsRequest.getStatus() == TtsRequest.Status.STARTED) {
            mqttCmdInterface.publish(topic, "STARTED");
        }
        else if (ttsRequest.getStatus() == TtsRequest.Status.ERROR) {
            mqttCmdInterface.publish(topic, "ERROR");
        }
        else if (ttsRequest.getStatus() == TtsRequest.Status.NOT_ALLOWED) {
            mqttCmdInterface.publish(topic, "NOT_ALLOWED");
        }
    }

    @Override
    public void onSdkError(@NotNull SdkException sdkException) {

    }

    @Override
    public void onTelepresenceEventChanged(@NotNull CallEventModel callEventModel) {
        String topic = "temi/" + temi_serial + "/call-status";
        if (callEventModel.getType() == CallEventModel.TYPE_INCOMING) {
            mqttCmdInterface.publish(topic, "TYPE_INCOMMING");
        } else if (callEventModel.getType() == CallEventModel.TYPE_OUTGOING) {
            mqttCmdInterface.publish(topic, "TYPE_OUTGOING");
        } else if (callEventModel.getType() == CallEventModel.STATE_ENDED) {
            mqttCmdInterface.publish(topic, "STATE_ENDED");
            RobotActionComplete();
        }
    }

    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, @NotNull String status, int descriptionId, @NotNull String description) {
        String topic = "temi/" + temi_serial + "/goto-status";
        switch (status) {
            case OnGoToLocationStatusChangedListener.START:
                mqttCmdInterface.publish(topic, "START");
                break;
            case OnGoToLocationStatusChangedListener.CALCULATING:
                mqttCmdInterface.publish(topic, "CALCULATING");
                break;
            case OnGoToLocationStatusChangedListener.ABORT:
                mqttCmdInterface.publish(topic, "ABORT");
                break;
            case OnGoToLocationStatusChangedListener.GOING:
                mqttCmdInterface.publish(topic, "GOING");
                break;
            case OnGoToLocationStatusChangedListener.COMPLETE:
                mqttCmdInterface.publish(topic, "COMPLETE");
                break;
        }
    }

    @Override
    public void onCurrentPositionChanged(@NotNull Position position) {
        Log.i("POSITION", "" + position.toString());

    }

//    @Override
//    public void onTelepresenceStatusChanged(@NotNull CallState callState) {
//        printLog(callState.toString());
//    }

    // ------------------------------------------------------ Robot Function  -------------------------------------------------------------
    public void speak(String tts) {
        TtsRequest ttsRequest = TtsRequest.create(tts, true);
        robot.speak(ttsRequest);
    }


    public void startTelepresence(String name) {
        //Find peerID by name
        List<UserInfo> userInfos = robot.getAllContact();

        for (UserInfo userInfo : userInfos) {
            if (userInfo.getName().contains(name)) {
                printLog("Start Telepresence");
                robot.startTelepresence(userInfo.getName(), userInfo.getUserId());
            }
        }
        speak("No user found on contact list");
    }

    public void goToPosition(float xp, float yp, float yawnp) {
        try {
            float x = xp;
            float y = yp;
            float yaw = yawnp;
            robot.goToPosition(new Position(x, y, yaw, 0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Boolean goTo(String des) {
        for (String location : robot.getLocations()) {
            printLog(location);
            if (location.equalsIgnoreCase(des)) {
                robot.goTo(des);
                return true;
            }
        }
        return  false;
    }

    void printLog(String msg) {
        Log.i("RobotAction", msg);
    }

    void printLog(String tag, String msg) {
        Log.i(tag, msg);
    }

    //----------------------------- MQTT --------------------------------------
    private void StartMqttCmdInterface() {
        String cmd_topic = "temi/" + temi_serial + "/temi-cmd";
        printLog(cmd_topic);

        mqttCmdInterface = new MqttWrapper(getApplicationContext(), cmd_topic, "VirachTemiRobotClient");
        mqttCmdInterface.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                printLog("Connected to " + serverURI.toString());
            }

            @Override
            public void connectionLost(Throwable cause) {
                mqttCmdInterface.reconnect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                printLog("From topic: " + topic.toString());
                printLog("Arrived message: " + message.toString());
                try {
                    JSONObject jsonObject = new JSONObject(message.toString());
//                    if ((isRobotActionComplete)) {
//                        printLog("Action Done");
//                    } else {
//                        printLog("Action Not Done");
//                    }
//                    if(isRobotActionComplete) {
//                        isRobotActionComplete = false;
//                        actionDecoder(jsonObject);
//                    }
                    actionDecoder(jsonObject);

                } catch (JSONException e) {
                    Log.d("Error", e.toString());
                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
    }

    /*---------------- ACTION DECODEER -----------------*/
    public void actionDecoder(JSONObject actionInfo) {
        try {
            switch (actionInfo.get("action").toString()) {
                case "speak":
//                    speak(actionInfo.get("content").toString());
                    if(actionInfo.get("language").equals("th")) {
                        RobotSpeak_TH(actionInfo.get("content").toString());
                    }
                    else if(actionInfo.get("language").equals("en")) {
                        speak(actionInfo.get("content").toString());
                    }
                    break;
                case "goto":
                    if(goTo(actionInfo.get("content").toString())) {}
                    break;
                case "call":
                    printLog("Call to " + actionInfo.get("content").toString());
//                    startTelepresence("Blockly-agent", actionInfo.get("content").toString());
                    robot.startTelepresence("TEST", "fe1090ed941db12ed1d350730031ea5b");
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWakeupWord(@NotNull String wakeupWord, int direction) {
        printLog("onWakeupWord", wakeupWord + ", " + direction);
        speechRecognizer.startListening(speechRecognizerIntent); //Get built-in STT working
    }

    @Override
    public void onAsrResult(@NotNull String asrResult) {
        printLog("onAsrResult", "asrResult = " + asrResult);

        if (asrResult.equalsIgnoreCase("Hello")) {
            robot.askQuestion("Hello, I'm temi, what can I do for you?");
        } else if (asrResult.equalsIgnoreCase("Play music")) {
            robot.speak(TtsRequest.create("Okay, please enjoy.", false));
            robot.finishConversation();
        } else if (asrResult.equalsIgnoreCase("Play movie")) {
            robot.speak(TtsRequest.create("Okay, please enjoy.", false));
            robot.finishConversation();
        } else if (asrResult.toLowerCase().contains("follow me")) {
            robot.finishConversation();
            robot.beWithMe();
        } else if (asrResult.toLowerCase().contains("go to home base")) {
            robot.finishConversation();
            robot.goTo("home base");
        } else {
            robot.askQuestion("Sorry I can't understand you, could you please ask something else?");
        }
    }

}

//Async Task



