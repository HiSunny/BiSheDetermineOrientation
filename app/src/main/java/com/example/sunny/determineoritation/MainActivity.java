package com.example.sunny.determineoritation;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.HashMap;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static final String TAG = "DetermineOrientationActivity";
    private static final int RATE = SensorManager.SENSOR_DELAY_NORMAL;
    private static final double GRAVITY_THRESHOLD =
            SensorManager.STANDARD_GRAVITY / 2;

    private SensorManager sensorManager;
    private float[] accelerationValues;
    private float[] magneticValues;
    private boolean isFaceUp;
    private RadioGroup sensorSelector;
    private TextView selectedSensorValue;
    private TextView orientationValue;
    private TextView sensorXLabel;
    private TextView sensorXValue;
    private TextView sensorYLabel;
    private TextView sensorYValue;
    private TextView sensorZLabel;
    private TextView sensorZValue;
    private HashMap<String, String> ttsParams;
    private boolean ttsNotifications;
    private int selectedSensorId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Initialize references to the UI views that will be updated in the
        // code
        sensorSelector = (RadioGroup) findViewById(R.id.sensorSelector);
        selectedSensorValue = (TextView) findViewById(R.id.selectedSensorValue);
        orientationValue = (TextView) findViewById(R.id.orientationValue);
        sensorXLabel = (TextView) findViewById(R.id.sensorXLabel);
        sensorXValue = (TextView) findViewById(R.id.sensorXValue);
        sensorYLabel = (TextView) findViewById(R.id.sensorYLabel);
        sensorYValue = (TextView) findViewById(R.id.sensorYValue);
        sensorZLabel = (TextView) findViewById(R.id.sensorZLabel);
        sensorZValue = (TextView) findViewById(R.id.sensorZValue);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Unregister updates from sensors
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        float[] rotationMatrix;

        switch (event.sensor.getType())
        {
            case Sensor.TYPE_GRAVITY:
                sensorXLabel.setText("X Axis");
                sensorXValue.setText(String.valueOf(event.values[0]));

                sensorYLabel.setText("Y Axis");
                sensorYValue.setText(String.valueOf(event.values[1]));

                sensorZLabel.setText("Z Axis");
                sensorZValue.setText(String.valueOf(event.values[2]));

                sensorYLabel.setVisibility(View.VISIBLE);
                sensorYValue.setVisibility(View.VISIBLE);
                sensorZLabel.setVisibility(View.VISIBLE);
                sensorZValue.setVisibility(View.VISIBLE);

                if (selectedSensorId == R.id.gravitySensor)
                {
                    if (event.values[2] >= GRAVITY_THRESHOLD)
                    {
                        onFaceUp();
                    }
                    else if (event.values[2] <= (GRAVITY_THRESHOLD * -1))
                    {
                        onFaceDown();
                    }
                }
                else
                {//此时accelerationValues代表重力加速度
                    accelerationValues = event.values.clone();
                    rotationMatrix = generateRotationMatrix();

                    if (rotationMatrix != null)
                    {
                        determineOrientation(rotationMatrix);
                    }
                }

                break;
            case Sensor.TYPE_ACCELEROMETER:
                accelerationValues = event.values.clone();
                rotationMatrix = generateRotationMatrix();

                if (rotationMatrix != null)
                {
                    determineOrientation(rotationMatrix);
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticValues = event.values.clone();
                rotationMatrix = generateRotationMatrix();

                if (rotationMatrix != null)
                {
                    determineOrientation(rotationMatrix);
                }
                break;
            case Sensor.TYPE_ROTATION_VECTOR:

                rotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(rotationMatrix,
                        event.values);
                determineOrientation(rotationMatrix);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        //Log.d(TAG, String.format("Accuracy for sensor %s = %d", sensor.getName(), accuracy));
    }

    /**
     * Generates a rotation matrix using the member data stored in
     * accelerationValues and magneticValues.
     *
     * @return The rotation matrix returned from
     * {@link android.hardware.SensorManager#getRotationMatrix(float[], float[], float[], float[])}
     * or <code>null</code> if either <code>accelerationValues</code> or
     * <code>magneticValues</code> is null.
     */
    private float[] generateRotationMatrix()
    {
        float[] rotationMatrix = null;

        if (accelerationValues != null && magneticValues != null)
        {
            rotationMatrix = new float[16];
            boolean rotationMatrixGenerated;
            //android.hardware.SensorManager.getRotationMatrix(float[], float[], float[], float[])
            //加速度/重力+地磁 accelerationValues:当使用加速度传感器时代表加速度，使用重力代表重力加速度值
            //boolean android.hardware.SensorManager.getRotationMatrix(float[] R, float[] I, float[] gravity, float[] geomagnetic)
            rotationMatrixGenerated =
                    SensorManager.getRotationMatrix(rotationMatrix,
                            null,
                            accelerationValues,
                            magneticValues);

            if (!rotationMatrixGenerated)
            {
                //Log.w(TAG, getString(R.string.rotationMatrixGenFailureMessage));

                rotationMatrix = null;
            }
        }

        return rotationMatrix;
    }

    /**
     * Uses the last read accelerometer and gravity values to determine if the
     * device is face up or face down.
     *
     * @param rotationMatrix The rotation matrix to use if the orientation
     * calculation
     */
    private void determineOrientation(float[] rotationMatrix)
    {
        float[] orientationValues = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientationValues);

        double azimuth = Math.toDegrees(orientationValues[0]);
        double pitch = Math.toDegrees(orientationValues[1]);
        double roll = Math.toDegrees(orientationValues[2]);

        sensorXLabel.setText("azimuth:");
        sensorXValue.setText(String.valueOf(azimuth));

        sensorYLabel.setText("pitch:");
        sensorYValue.setText(String.valueOf(pitch));

        sensorZLabel.setText("roll:");
        sensorZValue.setText(String.valueOf(roll));

        sensorYLabel.setVisibility(View.VISIBLE);
        sensorYValue.setVisibility(View.VISIBLE);
        sensorZLabel.setVisibility(View.VISIBLE);
        sensorZValue.setVisibility(View.VISIBLE);

        if (pitch <= 10)
        {
            if (Math.abs(roll) >= 170)
            {
                onFaceDown();
            }
            else if (Math.abs(roll) <= 10)
            {
                onFaceUp();
            }
        }
    }


    /**
     * Handler for device being face up.
     */
    private void onFaceUp()
    {
        if (!isFaceUp)
        {

            orientationValue.setText("face up");
            isFaceUp = true;
        }
    }

    /**
     * Handler for device being face down.
     */
    private void onFaceDown()
    {
        if (isFaceUp)
        {
            orientationValue.setText("face down");
            isFaceUp = false;
        }
    }


    /**
     * Updates the views for when the selected sensor is changed
     */
    private void updateSelectedSensor()
    {
        // Clear any current registrations
        sensorManager.unregisterListener(this);

        // Determine which radio button is currently selected and enable the
        // appropriate sensors
        selectedSensorId = sensorSelector.getCheckedRadioButtonId();
        if (selectedSensorId == R.id.accelerometerMagnetometer)
        {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    RATE);

            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    RATE);
        }
        else if (selectedSensorId == R.id.gravityMagnetometer)
        {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                    RATE);

            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    RATE);
        }
        else if ((selectedSensorId == R.id.gravitySensor))
        {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                    RATE);
        }
        else
        {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                    RATE);
        }

        // Update the label with the currently selected sensor
        RadioButton selectedSensorRadioButton =
                (RadioButton) findViewById(selectedSensorId);
        selectedSensorValue.setText(selectedSensorRadioButton.getText());
    }

    /**
     * Handles click event for the sensor selector.
     *
     * @param view The view that was clicked
     */
    public void onSensorSelectorClick(View view)
    {
        updateSelectedSensor();
    }

}
