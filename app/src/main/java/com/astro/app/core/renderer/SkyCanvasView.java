package com.astro.app.core.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Canvas-based sky renderer - works without OpenGL
 */
public class SkyCanvasView extends View {
    private static final String TAG = "SkyCanvasView";

    private Paint starPaint;
    private Paint linePaint;
    private Paint labelPaint;
    private Paint backgroundPaint;

    private boolean nightMode = false;
    private float fieldOfView = 60f;

    // Simple star data for rendering
    private List<float[]> stars = new CopyOnWriteArrayList<>();  // x, y, size, color
    private List<float[]> lines = new CopyOnWriteArrayList<>();  // x1, y1, x2, y2, color
    private List<Object[]> labels = new CopyOnWriteArrayList<>(); // x, y, text

    public SkyCanvasView(Context context) {
        super(context);
        init();
    }

    public SkyCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(24f);
        labelPaint.setColor(Color.WHITE);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.rgb(5, 5, 20)); // Dark blue

        // Add some demo stars for testing
        addDemoStars();
    }

    private void addDemoStars() {
        // Add random stars for demo
        for (int i = 0; i < 200; i++) {
            float x = (float) (Math.random());
            float y = (float) (Math.random());
            float size = (float) (Math.random() * 4 + 1);
            int color = Color.WHITE;
            stars.add(new float[]{x, y, size, color});
        }

        // Add some constellation lines
        lines.add(new float[]{0.1f, 0.2f, 0.15f, 0.25f, Color.argb(100, 100, 150, 255)});
        lines.add(new float[]{0.15f, 0.25f, 0.2f, 0.22f, Color.argb(100, 100, 150, 255)});

        // Add a label
        labels.add(new Object[]{0.5f, 0.1f, "Polaris"});
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Draw background
        if (nightMode) {
            backgroundPaint.setColor(Color.rgb(20, 0, 0)); // Dark red for night mode
        } else {
            backgroundPaint.setColor(Color.rgb(5, 5, 20)); // Dark blue
        }
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // Draw constellation lines
        for (float[] line : lines) {
            linePaint.setColor((int) line[4]);
            canvas.drawLine(
                line[0] * width, line[1] * height,
                line[2] * width, line[3] * height,
                linePaint
            );
        }

        // Draw stars
        for (float[] star : stars) {
            float x = star[0] * width;
            float y = star[1] * height;
            float size = star[2];
            starPaint.setColor((int) star[3]);
            if (nightMode) {
                starPaint.setColor(Color.rgb(255, 100, 100)); // Red tint
            }
            canvas.drawCircle(x, y, size, starPaint);
        }

        // Draw labels
        for (Object[] label : labels) {
            float x = (float) label[0] * width;
            float y = (float) label[1] * height;
            String text = (String) label[2];
            if (nightMode) {
                labelPaint.setColor(Color.rgb(255, 100, 100));
            } else {
                labelPaint.setColor(Color.WHITE);
            }
            canvas.drawText(text, x, y, labelPaint);
        }

        // Request next frame for animation
        postInvalidateOnAnimation();
    }

    public void setNightMode(boolean enabled) {
        this.nightMode = enabled;
        invalidate();
    }

    public void setFieldOfView(float fov) {
        this.fieldOfView = fov;
        invalidate();
    }

    public void setStars(List<float[]> newStars) {
        stars.clear();
        stars.addAll(newStars);
        invalidate();
    }

    public void setLines(List<float[]> newLines) {
        lines.clear();
        lines.addAll(newLines);
        invalidate();
    }

    public void setLabels(List<Object[]> newLabels) {
        labels.clear();
        labels.addAll(newLabels);
        invalidate();
    }
}
