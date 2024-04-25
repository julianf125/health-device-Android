package com.example.bluetooth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ecgView extends View {

    private Paint paint = new Paint();
    private float offsetX = 0f; // X-coordinate to start drawing from
    private int bpm = 80; // Beats per minute
    private float pixelsPerMillisecond; // How many pixels represent one millisecond

    // Keep track of the system time when the BPM was last updated
    private long lastUpdateTime = System.currentTimeMillis();

    public ecgView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(0xFF0000FF); // Blue color for plot line
        paint.setStrokeWidth(6f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Change scroll speed on screen size change
        pixelsPerMillisecond = w / (60000f / bpm);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float nwidth = getWidth();
        float nheight = getHeight();
        float nhalfHeight = nheight / 2f;

        if (bpm == 0) {
            canvas.drawLine(0, nhalfHeight, nwidth, nhalfHeight, paint);
        } else {

            // Elapsed time since last update
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastUpdateTime;

            // Update offsetX
            offsetX += elapsedTime * pixelsPerMillisecond;

            // Reset lastUpdateTime to the current time
            lastUpdateTime = currentTime;


            // Reset plot path
            float width = getWidth();
            float height = getHeight();
            float halfHeight = height / 2f;

            // Calculate beat positions from BPM
            float msPerBeat = 60000f / bpm;
            float beatWidth = pixelsPerMillisecond * msPerBeat;
            float spacing = width / 4f; // Num beats shown at once = 4

            // Start drawing from the left minus the offset to loop the drawing
            float startX = -offsetX % beatWidth;
            if (startX > 0) startX -= beatWidth;

            // Draw the ECG line
            for (float x = startX; x < width; x += spacing) {
                // Draw each beat
                canvas.drawLine(x, halfHeight, x + spacing * 0.2f, halfHeight, paint); // Flat line before spike
                canvas.drawLine(x + spacing * 0.2f, halfHeight, x + spacing * 0.25f, 0, paint); // Spike
                canvas.drawLine(x + spacing * 0.25f, 0, x + spacing * 0.3f, halfHeight, paint); // Back to baseline
                canvas.drawLine(x + spacing * 0.3f, halfHeight, x + spacing, halfHeight, paint); // Flat line after spike
            }


            // Update offsetX (scrolling)
            offsetX += pixelsPerMillisecond * 20f; // Adjust this value for speed
        }

        // Schedule a refresh
        postInvalidateDelayed(16); // About 60FPS
    }

    public void setBPM(int newBPM) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastUpdateTime;

        // Adjust offsetX
        offsetX += elapsedTime * pixelsPerMillisecond;

        bpm = (int)(newBPM/6.5);
        updateDrawingParameters();

        lastUpdateTime = currentTime;
    }

    private void updateDrawingParameters() {
        // Update params based on new BPM
        pixelsPerMillisecond = (getWidth() / (60f / bpm)) / 1000f;
    }
}
