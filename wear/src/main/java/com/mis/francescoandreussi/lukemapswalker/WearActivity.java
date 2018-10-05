package com.mis.francescoandreussi.lukemapswalker;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wear.widget.BoxInsetLayout;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;

public class WearActivity extends WearableActivity implements SensorEventListener {
    //Code strongly inspired from
    //https://github.com/pocmo/SensorDashboard

    private TextView accelerometerTextView;
    private TextView gyroscopeTextView;
    private TextView rotVecTextView;
    private TextView significantMotionText;
    private static final String TAG = "WearActivity";
    private final static int SENS_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    private final static int SENS_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    private final static int SENS_GYROSCOPE_UNCALIBRATED = Sensor.TYPE_GYROSCOPE_UNCALIBRATED;
    private final static int SENS_LINEAR_ACCELERATION = Sensor.TYPE_LINEAR_ACCELERATION;
    private final static int SENS_ROTATION_VECTOR = Sensor.TYPE_ROTATION_VECTOR;
    private final static int SENS_SIGNIFICANT_MOTION = Sensor.TYPE_SIGNIFICANT_MOTION;
    private ScheduledExecutorService mScheduler;
    private DataClient client;
    private PutDataMapRequest sensorData;
    private float[] rotMat = new float[16];
    private float[] orientation = new float[3];
    private boolean accData = false;
    private boolean gyroData = false;
    private boolean orientData = false;

    SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear);
        BoxInsetLayout wear_layout = findViewById(R.id.wear_layout);

        accelerometerTextView = (TextView) findViewById(R.id.accelerometer_text);
        gyroscopeTextView     = (TextView) findViewById(R.id.gyroscope_text);
        rotVecTextView        = (TextView) findViewById(R.id.rotation_text);
        significantMotionText = (TextView) findViewById(R.id.significant_motion_text);

        accelerometerTextView.setText("Accelerometer");
        gyroscopeTextView.setText("Gyroscope");

        client = Wearable.getDataClient(this);

        // Enables Always-on
        setAmbientEnabled();
        startMeasurement();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopMeasurement();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //client.sendSensorData(event.sensor.getType(), event.accuracy, event.timestamp, event.values);
        sensorData = PutDataMapRequest.create("/wear").setUrgent();
        switch (event.sensor.getType()) {
            case SENS_LINEAR_ACCELERATION:
                if(!accData) {
                    accelerometerTextView.setText("Accelerometer (no gravity):\n X " + new DecimalFormat("#.###").format(event.values[0]) +
                            " Y " + new DecimalFormat("#.###").format(event.values[1]) +
                            " Z " + new DecimalFormat("#.###").format(event.values[2]));
                    sensorData.getDataMap().putFloatArray("accVec", event.values.clone());
                    //sensorData.getDataMap().putInt("a", 1);
                    accData = true;
                    //PutDataRequest accVecSendable = accVec.asPutDataRequest();
                    //Task<DataItem> putAccVecTask = Wearable.getDataClient(this).putDataItem(accVecSendable);
                }
                break;
            case SENS_GYROSCOPE:
                if(!gyroData) {
                    gyroscopeTextView.setText("Gyroscope:\n X " + new DecimalFormat("#.###").format(event.values[0]) +
                            " Y " + new DecimalFormat("#.###").format(event.values[1]) +
                            " Z " + new DecimalFormat("#.###").format(event.values[2]));
                    //sensorData = PutDataMapRequest.create("/gyroVec").setUrgent();
                    sensorData.getDataMap().putFloatArray("gyroVec", event.values.clone());
                    //sensorData.getDataMap().putInt("b", 2);
                    gyroData = true;
                    //PutDataRequest gyroVecSendable = gyroVec.asPutDataRequest();
                    //Task<DataItem> putGyroVecTask = Wearable.getDataClient(this).putDataItem(gyroVecSendable);
                }
                break;
            case SENS_ROTATION_VECTOR:
                if(!orientData) {
                    SensorManager.getRotationMatrixFromVector(rotMat, event.values);
                    SensorManager.getOrientation(rotMat, orientation);
                    rotVecTextView.setText("Orientation:\n Azimuth " + new DecimalFormat("#.###").format(orientation[0]) +
                            " Pitch " + new DecimalFormat("#.###").format(orientation[1]) +
                            " Roll " + new DecimalFormat("#.###").format(orientation[2]));
                    //orientVec = PutDataMapRequest.create("/orientVec").setUrgent();
                    sensorData.getDataMap().putFloatArray("orientVec", orientation.clone());
                    //sensorData.getDataMap().putInt("c", 3);
                    orientData = true;
                    //PutDataRequest orientVecSendable = orientVec.asPutDataRequest();
                    //Task<DataItem> putOrientVecTask = Wearable.getDataClient(this).putDataItem(orientVecSendable);
                }
                break;
            case SENS_SIGNIFICANT_MOTION:
                significantMotionText.setText("Significant Motion: " + event.values);
        }
        sensorData.getDataMap().putLong("time", new Date().getTime());
        //Wearable.getDataClient(this).putDataItem(sensorData.asPutDataRequest());
        sendData(sensorData);
        //client.sendSensorData(event.sensor.getType(), event.accuracy, event.timestamp, event.values);
    }

    private void sendData(PutDataMapRequest data){
        if(accData && gyroData && orientData) {
            Task<DataItem> dataItemTask = client.putDataItem(data.asPutDataRequest());
            dataItemTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
                @Override
                public void onSuccess(DataItem dataItem) {
                    Log.d("SentData", "Sending Data was successful: " + dataItem);
                    accData = gyroData = orientData = false;
                }
            });
        }
    }

    protected void startMeasurement() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        //if(BuildConfig.DEBUG) { logAvailableSensors(); }

        Sensor accelerometerSensor = mSensorManager.getDefaultSensor((SENS_ACCELEROMETER));
        Sensor linearAccelerationSensor = mSensorManager.getDefaultSensor((SENS_LINEAR_ACCELERATION));
        Sensor gyroscopeSensor = mSensorManager.getDefaultSensor((SENS_GYROSCOPE));
        Sensor uncaibratedGyroscopeSensor = mSensorManager.getDefaultSensor((SENS_GYROSCOPE_UNCALIBRATED));
        Sensor rotationVectorSensor = mSensorManager.getDefaultSensor((SENS_ROTATION_VECTOR));

        //Register the listener
        if (mSensorManager != null) {
            if (accelerometerSensor != null) {
                mSensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Accelerometer found");
            }
            if (linearAccelerationSensor != null) {
                mSensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d(TAG, "No Linear Acceleration Sensor found");
            }
            if (gyroscopeSensor != null) {
                mSensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Gyroscope Sensor found");
            }
            if (uncaibratedGyroscopeSensor != null) {
                mSensorManager.registerListener(this, uncaibratedGyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Uncalibrated Gyroscope Sensor found");
            }
            if (rotationVectorSensor != null) {
                mSensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d(TAG, "No Rotation Vector Sensor found");
            }
        }
    }

    protected void stopMeasurement() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        if (mScheduler != null && !mScheduler.isTerminated()) {
            mScheduler.shutdown();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){}
}
