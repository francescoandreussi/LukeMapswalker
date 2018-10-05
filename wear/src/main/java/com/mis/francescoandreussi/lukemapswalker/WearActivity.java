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

public class WearActivity extends WearableActivity implements SensorEventListener {
    //Code strongly inspired from
    //https://github.com/pocmo/SensorDashboard

    private TextView accelerometerTextView;
    private TextView gyroscopeTextView;
    private TextView rotVecTextView;
    private TextView significantMotionText;
    private static final String TAG = "WearActivity";
    private final static int SENS_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    private final static int SENS_LINEAR_ACCELERATION = Sensor.TYPE_LINEAR_ACCELERATION;
    private final static int SENS_ROTATION_VECTOR = Sensor.TYPE_ROTATION_VECTOR;
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

        // Initialize DataClient that will handle the communication between smartwatch and phone
        // (i.e.: open the connection
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
        // Setting up a request for putting a DataMap with path "/wear" and defining it urgent
        // in order to make the DataMapItems delivered right away
        sensorData = PutDataMapRequest.create("/wear").setUrgent();
        switch (event.sensor.getType()) {
            case SENS_LINEAR_ACCELERATION:
                if(!accData) {
                    // Initially accData is false (in our idea that should help to deliver one FloatArray
                    // per Sensor for each DataMapItem
                    accelerometerTextView.setText("Accelerometer (no gravity):\n X " + new DecimalFormat("#.###").format(event.values[0]) +
                            " Y " + new DecimalFormat("#.###").format(event.values[1]) +
                            " Z " + new DecimalFormat("#.###").format(event.values[2]));
                    // Putting the FloatArray of values of the Sensor in the DataMap of the PutDataMapRequest
                    sensorData.getDataMap().putFloatArray("accVec", event.values.clone());
                    //sensorData.getDataMap().putInt("a", 1); //TROUBLESHOOT
                    accData = true; // Avoiding that other data form this vector are added to the Put request
                }
                break;
            case SENS_GYROSCOPE:
                if(!gyroData) { // Read previous case
                    gyroscopeTextView.setText("Gyroscope:\n X " + new DecimalFormat("#.###").format(event.values[0]) +
                            " Y " + new DecimalFormat("#.###").format(event.values[1]) +
                            " Z " + new DecimalFormat("#.###").format(event.values[2]));
                    sensorData.getDataMap().putFloatArray("gyroVec", event.values.clone());
                    //sensorData.getDataMap().putInt("b", 2);
                    gyroData = true;
                }
                break;
            case SENS_ROTATION_VECTOR:
                if(!orientData) { // Read first case
                    SensorManager.getRotationMatrixFromVector(rotMat, event.values);
                    SensorManager.getOrientation(rotMat, orientation);
                    rotVecTextView.setText("Orientation:\n Azimuth " + new DecimalFormat("#.###").format(orientation[0]) +
                            " Pitch " + new DecimalFormat("#.###").format(orientation[1]) +
                            " Roll " + new DecimalFormat("#.###").format(orientation[2]));
                    sensorData.getDataMap().putFloatArray("orientVec", orientation.clone());
                    //sensorData.getDataMap().putInt("c", 3);
                    orientData = true;
                }
        }
        sensorData.getDataMap().putLong("time", new Date().getTime()); // Putting also a timestamp
        sendData(sensorData);
    }

    private void sendData(PutDataMapRequest data){
        if(accData && gyroData && orientData) { // Check if in the Put request there is exactly ONE array for analyzed Sensor
            // Putting DataItem encapsulating the DataMap with the senors data to the DataClient
            // and checking for the success of the operation
            Task<DataItem> dataItemTask = client.putDataItem(data.asPutDataRequest());
            dataItemTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
                @Override
                public void onSuccess(DataItem dataItem) {
                    Log.d("SentData", "Sending Data was successful: " + dataItem);
                    accData = gyroData = orientData = false; // This makes the onSensorChangedListener
                                                             // able to create another Put request with three arrays
                }
            });
        }
    }

    protected void startMeasurement() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        Sensor linearAccelerationSensor = mSensorManager.getDefaultSensor((SENS_LINEAR_ACCELERATION));
        Sensor gyroscopeSensor = mSensorManager.getDefaultSensor((SENS_GYROSCOPE));
        Sensor rotationVectorSensor = mSensorManager.getDefaultSensor((SENS_ROTATION_VECTOR));

        //Register the listener
        if (mSensorManager != null) {
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
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){}
}
