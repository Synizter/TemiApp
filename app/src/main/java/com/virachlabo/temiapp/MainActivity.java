package com.virachlabo.temiapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
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
import com.robotemi.sdk.model.CallEventModel;
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener;
import com.robotemi.sdk.navigation.model.Position;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity<RobotActionExecutorService> extends AppCompatActivity implements
        OnRobotReadyListener,
        Robot.TtsListener,
        OnSdkExceptionListener,
        OnCurrentPositionChangedListener,
        OnTelepresenceEventChangedListener,
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

    private SpeechRecognizer speechRecognizer; //STT
    Intent speechRecognizerIntent; //STT

    //MQTT Instance
    MqttWrapper mqttWrapper;

    //ActrionFlag
    Boolean isRobotActionComplete = true;

    public static void verifyStroagePermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    public static void verifyAudioRecordPermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO);
        if(permission != PackageManager.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity, PERMISSION_AUDIO, REQUEST_CODE_VOICE_RECOGNITION);
            }
        }
    }

    public void RobotSpeak(String s, boolean isOverlayed) {
        robot.speak(TtsRequest.create(s, isOverlayed));
    }

    //Androod App callback -------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStroagePermission(this);
        verifyAudioRecordPermission(this);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this); //STT
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); //STT
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH"); //STT


        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {
                printLog("Start speak");
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

            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });



        robot = Robot.getInstance(); // get an instance of the robot in order to begin using its features.
        robot.addOnSdkExceptionListener(this);
        robot.addOnCurrentPositionChangedListener(this);

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


    }

    // ------------------------------------------------------ Robot SDK Callvack -------------------------------------------------------------
    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
//                Robot.getInstance().onStart(); //method may change the visibility of top bar.

                robot.onStart(activityInfo);
                //Start MQTT Server
                StartMqtt();

            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    public void onTtsStatusChanged(@NotNull TtsRequest ttsRequest) {
        if (ttsRequest.getStatus() == TtsRequest.Status.COMPLETED) {
            mqttWrapper.publish("actionState/", "Done");
            isRobotActionComplete = true;
        }
    }

    @Override
    public void onSdkError(@NotNull SdkException sdkException) {

    }

    @Override
    public void onTelepresenceEventChanged(@NotNull CallEventModel callEventModel) {
        printLog("onTelepresenceEvent", callEventModel.toString());
        if (callEventModel.getType() == CallEventModel.TYPE_INCOMING) {
            Toast.makeText(this, "Incoming call", Toast.LENGTH_LONG).show();
        }
        else if  (callEventModel.getType() == CallEventModel.TYPE_OUTGOING) {
            Toast.makeText(this, "Outgoing call", Toast.LENGTH_LONG).show();
        }
        else if (callEventModel.getType() == CallEventModel.STATE_ENDED) {
            mqttWrapper.publish("actionState/", "Done");
            isRobotActionComplete = true;
        }
    }


    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, @NotNull String status, int descriptionId, @NotNull String description) {
        switch (status) {
            case OnGoToLocationStatusChangedListener.START:
                printLog("Start");
                break;
            case OnGoToLocationStatusChangedListener.CALCULATING:
                printLog("Calculating route");
                break;
            case OnGoToLocationStatusChangedListener.COMPLETE:
                printLog("Arrived");
                isRobotActionComplete = true;
                break;
        }
    }

    @Override
    public void onCurrentPositionChanged(@NotNull Position position) {
        Log.i("POSITION", "" + position.toString());

    }

    // ------------------------------------------------------ Robot Function  -------------------------------------------------------------
    public void speak(String tts) {
        TtsRequest ttsRequest = TtsRequest.create(tts, true);
        robot.speak(ttsRequest);
    }

    //TEST Function
//    public List<UserInfo> GetAllUser() {
//        List<UserInfo> userInfos = robot.getAllContact();
//        for (UserInfo userInfo : userInfos) {
//            printLog("Username", userInfo.getName());
//            printLog("UserID", userInfo.getUserId());
//            printLog("UserRole", String.valueOf(userInfo.getRole()));
//        }
//        return userInfos;
//    }

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

    public void goTo(String des) {
        for (String location : robot.getLocations()) {
            if (location.equals(des)) {
                robot.goTo(des);
            }
        }
    }

    void printLog(String msg) {
        Log.i("RobotAction", msg);
    }

    void printLog(String tag, String msg) {
        Log.i(tag, msg);
    }

    //----------------------------- MQTT --------------------------------------
    private void StartMqtt() {
        printLog("Inside MQTT START");
        mqttWrapper = new MqttWrapper(getApplicationContext());
        mqttWrapper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                printLog("Connect to " + serverURI.toString());
            }

            @Override
            public void connectionLost(Throwable cause) {
                mqttWrapper.reconnect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                printLog("From topic: " + topic.toString());
                printLog("Arrived message: " + message.toString());
                try {
                    JSONObject jsonObject = new JSONObject(message.toString());
                    if(isRobotActionComplete == true) {
                        isRobotActionComplete = false;
                        actionDecoder(jsonObject);
                    }
                }catch (JSONException e) {
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
                case "SPEAK":
                    speak(actionInfo.get("content").toString());
                    break;
                case "GOTO":
                    goTo(actionInfo.get("content").toString());
                    break;
                case "CALL":
                    startTelepresence(actionInfo.get("content").toString());
                    break;
                case "NLP_TH_START":
                    //start chatbot sequence
                    speechRecognizer.startListening(speechRecognizerIntent);
                    break;
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

//Async Task



