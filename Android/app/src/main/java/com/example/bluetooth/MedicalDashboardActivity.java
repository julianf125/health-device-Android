package com.example.bluetooth;

import static java.lang.Double.parseDouble;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import java.util.ArrayList;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.widget.ProgressBar;

import android.os.Bundle;
import android.app.Activity;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MedicalDashboardActivity extends Activity {

    private ProgressBar progressBarTemperature, progressBarHeartRate, circularProgressBarOxygenSaturation;
    private TextView temperatureLabel, heartRateLabel, oxygenSaturationLabel, oxygenSaturationVal;
    private TextView heartRateValue;

    private Handler uiHandler;
    private Runnable uiUpdater;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_dashboard);

        // UI elements initialization
        heartRateValue = findViewById(R.id.heartRateValue);
        progressBarTemperature = findViewById(R.id.progressBarTemperature);
        progressBarHeartRate = findViewById(R.id.progressBarHeartRate);
        circularProgressBarOxygenSaturation = findViewById(R.id.circularProgressBarOxygenSaturation);
        temperatureLabel = findViewById(R.id.temperatureValue);
        heartRateLabel = findViewById(R.id.heartRateLabel);
        oxygenSaturationLabel = findViewById(R.id.oxygenSaturationLabel);
        oxygenSaturationVal = findViewById(R.id.bloodOxValue);

        uiHandler = new Handler();

        uiUpdater = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                int currHR = (int)DataSingleton.getInstance().getHeartRate();
                double currBodyTemp = DataSingleton.getInstance().getBodyTemp();
                double currSpO2 = DataSingleton.getInstance().getBloodOx();

                // Update data
                updateHealthData(currBodyTemp, currHR, currSpO2);

                uiHandler.postDelayed(this, 1000);
            }
        };

        // Implement back button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            onBackPressed();
        });
    }

    // Update progress bars
    public void updateHealthData(double temperature, int heartRate, double oxygenSaturation) {
        updateTemperatureProgressBar((float)temperature);
        updateHeartRateProgressBar(heartRate);
        updateOxygenSaturationProgressBar(oxygenSaturation);
    }

    // Update/animate temperature progress bar
    private void updateTemperatureProgressBar(float temperature) {
        int progress = (int) temperature;
        ObjectAnimator animation1 = ObjectAnimator.ofInt(progressBarTemperature, "progress", progressBarTemperature.getProgress(), progress);
        animation1.setInterpolator(new DecelerateInterpolator());
        animation1.setDuration(500);
        animation1.start();
        temperatureLabel.setText(String.format("%.1f", temperature));
    }

    // Update/animate heart rate progress bar
    private void updateHeartRateProgressBar(int heartRate) {
        int progress = (int) ((heartRate*100)/200);
        ObjectAnimator animation2 = ObjectAnimator.ofInt(progressBarHeartRate, "progress", progressBarHeartRate.getProgress(), progress);
        animation2.setInterpolator(new DecelerateInterpolator());
        animation2.setDuration(500); // Duration in milliseconds
        animation2.start();
        heartRateValue.setText(String.valueOf(heartRate));
    }

    // Update oxygen saturation progress bar
    private void updateOxygenSaturationProgressBar(double oxygenSaturation) {
        int bloodOxProgress = (int)((oxygenSaturation - 90)*10);
        if (bloodOxProgress < 0) {
            bloodOxProgress = 0;
        }
        ObjectAnimator animation3 = ObjectAnimator.ofInt(circularProgressBarOxygenSaturation, "progress", circularProgressBarOxygenSaturation.getProgress(), bloodOxProgress);
        animation3.setDuration(500); // Duration in milliseconds
        animation3.setInterpolator(new DecelerateInterpolator());
        animation3.start();
        oxygenSaturationVal.setText(Double.toString(oxygenSaturation) + "%");
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
        uiUpdater.run(); // Start the UI update loop
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
        uiHandler.removeCallbacks(uiUpdater); // Stop the loop
    }
}
