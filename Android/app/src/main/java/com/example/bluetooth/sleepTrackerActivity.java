package com.example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import android.util.Log;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayDeque;
import java.util.Queue;


public class sleepTrackerActivity extends AppCompatActivity implements SensorEventListener {

    private LocalDateTime sleepStart, sleepEnd;
    private TextView sleepDurationTextView, fellAsleepTextView, wokeUpTextView, timeAwakeTextView;
    private Button toggleSleepButton;
    private boolean isSleeping = false;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isSensorRegistered = false;
    private float movementSum = 0;
    private int movementCount = 0;
    private long lastUpdate = 0;
    private final float[] gravity = new float[3];
    private final float alpha = 0.8f;
    private int awakeMinutes = 0;
    private float movementThreshold;
    private long secondLastUpdate = 0;
    private float secondMovementSum = 0;
    private int secondMovementCount = 0;
    private Queue<Float> movementQueue = new ArrayDeque<>();
    private static final int MOVEMENT_QUEUE_SIZE = 10; // Size of the moving average window
    private boolean useLinearAccelerationSensor = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_tracker);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            finish();
        });

        toggleSleepButton = findViewById(R.id.toggleSleepButton);
        sleepDurationTextView = findViewById(R.id.sleepDurationTextView);
        fellAsleepTextView = findViewById(R.id.fellAsleepTextView);
        wokeUpTextView = findViewById(R.id.wokeUpTextView);
        timeAwakeTextView = findViewById(R.id.timeAwakeTextView);

        sleepDurationTextView.setVisibility(View.GONE);
        wokeUpTextView.setVisibility(View.GONE);
        timeAwakeTextView.setVisibility(View.GONE);
        fellAsleepTextView.setVisibility(View.GONE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        if (accelerometer == null) {
            Log.e("SensorError", "Linear acceleration sensor not available, using standard accelerometer");
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            useLinearAccelerationSensor = false;
        } else {
            Log.d("SensorStatus", "Using linear acceleration sensor");
        }

        toggleSleepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSleeping) {
                    sleepStart = LocalDateTime.now();
                    fellAsleepTextView.setText("Fell Asleep At: " + formatTime(sleepStart));
                    toggleSleepButton.setText("Stop Sleep");
                    isSleeping = true;

                    fellAsleepTextView.setText("");
                    sleepDurationTextView.setText("");
                    wokeUpTextView.setText("");
                    timeAwakeTextView.setText("");

                    sleepDurationTextView.setVisibility(View.GONE);
                    wokeUpTextView.setVisibility(View.GONE);
                    timeAwakeTextView.setVisibility(View.GONE);
                    fellAsleepTextView.setVisibility(View.GONE);

                    movementSum = 0;
                    movementCount = 0;
                    if (!isSensorRegistered) {
                        sensorManager.registerListener(sleepTrackerActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                        isSensorRegistered = true;
                    }

                } else {
                    sleepEnd = LocalDateTime.now();
                    fellAsleepTextView.setText("Fell Asleep At:  " + formatTime(sleepStart));
                    wokeUpTextView.setText("Woke Up At:  " + formatTime(sleepEnd));

                    awakeMinutes = awakeMinutes / 6;
                    long totalSleepMinutes = java.time.Duration.between(sleepStart, sleepEnd).toMinutes();
                    long adjustedSleepMinutes = totalSleepMinutes - awakeMinutes;
                    long adjustedSleepHours = adjustedSleepMinutes / 60;
                    adjustedSleepMinutes %= 60;

                    sleepDurationTextView.setText("Slept for:  " + adjustedSleepHours + " hours and " + adjustedSleepMinutes + " minutes");
                    timeAwakeTextView.setText("Time Spent Awake:  " + awakeMinutes / 60 + " hours and " + awakeMinutes % 60 + " minutes");
                    awakeMinutes = 0;
                    toggleSleepButton.setText("Start Sleep");
                    isSleeping = false;

                    sleepDurationTextView.setVisibility(View.VISIBLE);
                    wokeUpTextView.setVisibility(View.VISIBLE);
                    timeAwakeTextView.setVisibility(View.VISIBLE);
                    fellAsleepTextView.setVisibility(View.VISIBLE);

                    if (isSensorRegistered) {
                        sensorManager.unregisterListener(sleepTrackerActivity.this);
                        isSensorRegistered = false;
                    }
                }
            }
        });
    }

    private String formatTime(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return time.format(formatter);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        float x, y, z;
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            // If linear acceleration sensor is used
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
        } else {
            // If standard accelerometer is used
            // Apply low-pass filter to isolate gravity
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            x = event.values[0] - gravity[0];
            y = event.values[1] - gravity[1];
            z = event.values[2] - gravity[2];
        }

        float tempmagnitude = (float) Math.sqrt(x * x + y * y + z * z);

        // Add to queue and remove oldest if exceeded size
        movementQueue.add(tempmagnitude);
        if (movementQueue.size() > MOVEMENT_QUEUE_SIZE) {
            movementQueue.poll();
        }

        float sum = 0;
        for (float val : movementQueue) {
            sum += val;
        }
        float magnitude = sum / movementQueue.size();
        secondMovementSum += magnitude;
        secondMovementCount++;
        movementSum += magnitude;
        movementCount++;

        long currentTime = System.currentTimeMillis();
        if (currentTime - secondLastUpdate >= 1000) { // One second has passed
            float secondAverageMovement = secondMovementSum / secondMovementCount;
            Log.d("SleepTracker", "Average movement per second: " + secondAverageMovement);
            secondMovementSum = 0;
            secondMovementCount = 0;
            secondLastUpdate = currentTime;
        }

        if (currentTime - lastUpdate >= 10000) { // Ten seconds have passed
            float tenSecondAverageMovement = movementSum / movementCount;
            Log.d("SleepTracker", "Average movement per 10 seconds: " + tenSecondAverageMovement);

            // Define a threshold for movement to determine awake state
            if (useLinearAccelerationSensor) {
                movementThreshold = 0.01f; // This value may need adjustment
            }
            else{
                movementThreshold = 0.075f;
            }
            String sleepState = tenSecondAverageMovement > movementThreshold ? "Awake" : "Asleep";
            Log.d("SleepTracker", "Sleep state for the past 10 seconds: " + sleepState);

            if (tenSecondAverageMovement > movementThreshold) {
                awakeMinutes++;
            }

            movementSum = 0;
            movementCount = 0;
            lastUpdate = currentTime;
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Can be left empty for this use case
    }
}
