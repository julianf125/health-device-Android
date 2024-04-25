package com.example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ecgActivity extends AppCompatActivity {

    private ecgView heartRatePlot;
    private TextView heartRateView;

    private Handler uiHandler;
    private Runnable uiUpdater;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecg);

        heartRatePlot = findViewById(R.id.heartRatePlot);
        heartRateView = findViewById(R.id.heartRate);

        // EKG Plot
        heartRatePlot = findViewById(R.id.heartRatePlot);

        uiHandler = new Handler();

        uiUpdater = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                double currHR = DataSingleton.getInstance().getHeartRate();
                heartRatePlot.setBPM((int)currHR);
                heartRateView.setText("Heart Rate:   " + Double.toString(currHR) + " BPM");

                uiHandler.postDelayed(this, 1000);
            }
        };

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
}