package com.example.bluetooth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

public class pressureGaugeView extends View {
    private float value = 0;
    private Paint gaugePaint;
    private Paint backgroundPaint;
    private Paint needlePaint;
    private int minValue;
    private int maxValue;
    private int startAngle;
    private int sweepAngle;

    public pressureGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        minValue = 0;
        maxValue = 100;
        startAngle = 120;
        sweepAngle = 300;

        gaugePaint = new Paint();
        gaugePaint.setAntiAlias(true);
        gaugePaint.setStyle(Paint.Style.STROKE);
        gaugePaint.setStrokeWidth(45);

        backgroundPaint = new Paint(gaugePaint);
        backgroundPaint.setColor(0xFFCCCCCC); // Grey background

        needlePaint = new Paint();
        needlePaint.setColor(0xFFFF0000); // Red needle
        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setStrokeWidth(5);
        needlePaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background arc
        drawGaugeBackground(canvas);

        // Draw foreground arc with gradient
        drawGaugeForeground(canvas);

        // Draw the needle
        drawNeedle(canvas);
    }

    private void drawGaugeBackground(Canvas canvas) {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) - gaugePaint.getStrokeWidth();

        canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                startAngle, sweepAngle, false, backgroundPaint);
    }

    private void drawGaugeForeground(Canvas canvas) {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) - gaugePaint.getStrokeWidth();

        // Calculate the end angle
        float endAngle = startAngle + (value - minValue) / (maxValue - minValue) * sweepAngle;

        // Create a path for the filled-in portion
        Path filledPath = new Path();
        filledPath.addArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                startAngle, endAngle - startAngle);

        // Sweep gradient for the entire arc
        SweepGradient sweepGradient = new SweepGradient(centerX, centerY,
                new int[] {0xFFFF0000, 0xFFFFFF00, 0xFF86DC3D, 0xFF86DC3D, 0xFFFFFF00, 0xFFFF0000},
                new float[] {0.0f, 0.2f, 0.45f, 0.55f, 0.8f, 1f});

        // Rotate the gradient to start at the right spot
        Matrix gradientMatrix = new Matrix();
        gradientMatrix.preRotate(startAngle, centerX, centerY);
        sweepGradient.setLocalMatrix(gradientMatrix);

        gaugePaint.setShader(sweepGradient);

        // Draw only the filled portion of the gauge with the gradient
        canvas.drawPath(filledPath, gaugePaint);
    }




    public void setValue(float value) {
        this.value = Math.min(maxValue, Math.max(minValue, value));
        invalidate(); // Redraw the view
    }

    private void drawNeedle(Canvas canvas) {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) - gaugePaint.getStrokeWidth();

        // Calculate needle angle based on value
        float needleAngle = (value - minValue) / (maxValue - minValue) * sweepAngle + startAngle;
        float needleAngleRadians = (float) Math.toRadians(needleAngle);

        // Calculate the end points of the triangle
        float needleLength = radius * 0.85f;
        float needleWidth = needleLength * 0.05f;

        // Tip of the needle
        float needleTipX = centerX + needleLength * (float) Math.cos(needleAngleRadians);
        float needleTipY = centerY + needleLength * (float) Math.sin(needleAngleRadians);

        // Base points of the needle triangle
        float baseAngleRadians = (float) Math.toRadians(90);
        float baseX1 = centerX + needleWidth * (float) Math.cos(needleAngleRadians + baseAngleRadians);
        float baseY1 = centerY + needleWidth * (float) Math.sin(needleAngleRadians + baseAngleRadians);
        float baseX2 = centerX + needleWidth * (float) Math.cos(needleAngleRadians - baseAngleRadians);
        float baseY2 = centerY + needleWidth * (float) Math.sin(needleAngleRadians - baseAngleRadians);

        // Draw the needle
        Path needlePath = new Path();
        needlePath.moveTo(needleTipX, needleTipY); // Move to the tip
        needlePath.lineTo(baseX1, baseY1); // Line to one base point
        needlePath.lineTo(baseX2, baseY2); // Line to the other base point
        needlePath.close(); // Close the triangle

        needlePaint.setStrokeWidth(2); // Outline
        needlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawPath(needlePath, needlePaint);

        // Screw dot
        Paint centerDotPaint = new Paint();
        centerDotPaint.setColor(0xFF000000); // Black color
        centerDotPaint.setStyle(Paint.Style.FILL);
        float centerDotRadius = 10;
        canvas.drawCircle(centerX, centerY, centerDotRadius, centerDotPaint);
    }
}
