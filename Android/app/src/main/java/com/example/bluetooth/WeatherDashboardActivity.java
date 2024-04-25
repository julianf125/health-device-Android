package com.example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class WeatherDashboardActivity extends AppCompatActivity {

    private Handler uiHandler;
    private Runnable uiUpdater;
    private boolean isRunning = false;
    private float currentValueTemp = 0.0f;
    private float currentValueHum = 0.0f;
    private float currentValuePressure = 0.0f;
    private TextView tempValue, pressureValue, humidityValue, airQuality;
    private GaugeView temperatureGaugeView;
    private pressureGaugeView pressureGaugeView;
    private humidityGaugeView humidityGaugeView;
    private ProgressBar progressBarGasVoc;
    private boolean isFirstUpdate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_dashboard);

        uiHandler = new Handler();

        uiUpdater = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                double currAirTemp = DataSingleton.getInstance().getTempData();
                double currHumidity = DataSingleton.getInstance().getHumidity();
                double currPressure = DataSingleton.getInstance().getPressData();
                double currGas = DataSingleton.getInstance().getGas();

                updateGauges(currAirTemp, currHumidity, currPressure, currGas);

                uiHandler.postDelayed(this, 1000);
            }
        };

        // Assuming gaugeView is your GaugeView instance
        temperatureGaugeView = findViewById(R.id.gaugeTemperature);
        pressureGaugeView = findViewById(R.id.gaugePressure);
        humidityGaugeView = findViewById(R.id.gaugeHumidity);

        tempValue = findViewById(R.id.valueTemperature);
        pressureValue = findViewById(R.id.valuePressure);
        humidityValue = findViewById(R.id.valueHumidity);
        progressBarGasVoc = findViewById(R.id.progressBarGasVoc);
        airQuality = findViewById(R.id.gasVocValue);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            finish();
        });

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

    // This function updates and animates the 3 gauges for temperature, pressure, and humidity,
    // and updates the progress bar for air quality.
    protected void updateGauges(Double temp, Double humidity, Double pressure, Double gas) {
        tempValue.setText(String.format("%.1f\u00B0 F", temp));
        pressureValue.setText(String.format("%.1f\nhPa", pressure/100.0));
        humidityValue.setText(String.format("%.1f %%", humidity));

        float tempStartValue = isFirstUpdate ? 0f : currentValueTemp;
        float humStartValue = isFirstUpdate ? 0f : currentValueHum;
        float pressStartValue = isFirstUpdate ? 0f : currentValuePressure;
        int airBarValue = (gas == 0.0) ? 0 : (int)(100 - ((Float.parseFloat(String.valueOf(gas))-5700)/50));
        isFirstUpdate = false; // Set flag to false after first update (start from 0 on opening activity)


        float pressGaugeValue = ((Float.parseFloat(String.valueOf(pressure))/100)-600)/8;

        // Animate progress bar and gauge motion
        ObjectAnimator tempGaugeAnimator = ObjectAnimator.ofFloat(temperatureGaugeView, "value", tempStartValue, temp.floatValue());
        tempGaugeAnimator.setDuration(1000); // Duration in milliseconds
        tempGaugeAnimator.setInterpolator(new DecelerateInterpolator());
        tempGaugeAnimator.start();

        ObjectAnimator humGaugeAnimator = ObjectAnimator.ofFloat(humidityGaugeView, "value", humStartValue, humidity.floatValue());
        humGaugeAnimator.setDuration(1000); // Duration in milliseconds
        humGaugeAnimator.setInterpolator(new DecelerateInterpolator());
        humGaugeAnimator.start();

        ObjectAnimator pressGaugeAnimator = ObjectAnimator.ofFloat(pressureGaugeView, "value", pressStartValue, pressGaugeValue);
        pressGaugeAnimator.setDuration(1000); // Duration in milliseconds
        pressGaugeAnimator.setInterpolator(new DecelerateInterpolator());
        pressGaugeAnimator.start();

        ObjectAnimator animation = ObjectAnimator.ofInt(progressBarGasVoc, "progress", progressBarGasVoc.getProgress(), airBarValue);
        animation.setDuration(750); // Duration in milliseconds
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();

        currentValueTemp = Float.parseFloat(String.valueOf(temp));
        currentValueHum = Float.parseFloat(String.valueOf(humidity));
        currentValuePressure = pressGaugeValue;

        // Calculate air quality
        if (gas != 0.0) {
            if (airBarValue <= 25) {
                airQuality.setText("Excellent");
                airQuality.setTextColor(Color.rgb(80, 200, 120));
            } else if (airBarValue <= 50) {
                airQuality.setText("Good");
                airQuality.setTextColor(Color.rgb(50, 205, 50));
            } else if (airBarValue <= 75) {
                airQuality.setText("Fair");
                airQuality.setTextColor(Color.rgb(255, 220, 30));
            } else if (airBarValue <= 90) {
                airQuality.setText("Poor");
                airQuality.setTextColor(Color.rgb(255, 165, 0));
            } else {
                airQuality.setText("Hazardous");
                airQuality.setTextColor(Color.rgb(255, 0, 0));
            }
        }
    }

}