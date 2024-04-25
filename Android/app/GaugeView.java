import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GaugeView extends View {
    private float value = 0;
    private Paint paint;
    private int minValue;
    private int maxValue;
    private int startAngle;
    private int sweepAngle;

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        // Set other paint properties like color as needed

        // Initialize gauge properties
        minValue = 0;
        maxValue = 100;
        startAngle = 180;
        sweepAngle = 180;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGauge(canvas);
    }

    private void drawGauge(Canvas canvas) {
        // Center and radius for the gauge
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) - paint.getStrokeWidth();

        // Calculate end angle based on value
        float angle = (value - minValue) / (maxValue - minValue) * sweepAngle + startAngle;

        // Draw the gauge
        canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                startAngle, angle - startAngle, false, paint);
    }

    public void setValue(float value) {
        this.value = value;
        invalidate(); // Redraw the view
    }
}
