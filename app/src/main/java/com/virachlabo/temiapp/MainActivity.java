package com.virachlabo.temiapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.exception.OnSdkExceptionListener;
import com.robotemi.sdk.exception.SdkException;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnLocationsUpdatedListener;
import com.robotemi.sdk.listeners.OnRobotLiftedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;
import com.robotemi.sdk.permission.OnRequestPermissionResultListener;
import com.robotemi.sdk.permission.Permission;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MainActivity extends AppCompatActivity implements
        OnRobotReadyListener,
        OnRobotLiftedListener,
        Robot.TtsListener,
        OnGoToLocationStatusChangedListener,
        OnSdkExceptionListener,
        OnRequestPermissionResultListener {

    //Permission
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

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

    //Object and Variable
    private  Robot robot;
    private List<String> location;
    private FirebaseDatabase firebaseDatabase;

    //Verify permission function
    public static void verifyStroagePermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }
    //Robot Ability Fucntion
    public void TEST() {
        DatabaseReference test = firebaseDatabase.getReference();

        test.setValue("I'm READy");


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

        robot = Robot.getInstance();
        robot.addOnRequestPermissionResultListener(this);
        robot.addOnSdkExceptionListener(this);

        FirebaseApp.initializeApp(this);

    }

    @Override
    protected void onDestroy() {
        robot.removeOnRequestPermissionResultListener(this);
        robot.removeOnSdkExceptionListener(this);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        robot.addOnRobotReadyListener(this);
        robot.addOnGoToLocationStatusChangedListener(this);
        robot.addTtsListener(this);
        robot.addOnRobotLiftedListener(this);

        firebaseDatabase = FirebaseDatabase.getInstance();
    }

    //Robot callback -------------------------------------------------------
    @Override
    public void onRobotReady(boolean isReady) {
        if(isReady) {
            //Iniitializa robot
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                robot.onStart(activityInfo);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            TEST();

        }
    }

    @Override
    public void onTtsStatusChanged(@NotNull TtsRequest ttsRequest) {

        if(ttsRequest.getStatus() == TtsRequest.Status.COMPLETED) {
            Log.i("TtsStatus", "" + ttsRequest.getStatus().toString());
        }


    }

    @Override
    public void onSdkError(@NotNull SdkException sdkException) {

    }

    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, @NotNull String status, int descriptionId, @NotNull String description) {

    }

    @Override
    public void onRobotLifted(boolean isLifted, @NotNull String reason) {

    }


    @Override
    public void onRequestPermissionResult(@NotNull Permission permission, int grantResult, int requestCode) {

    }
}