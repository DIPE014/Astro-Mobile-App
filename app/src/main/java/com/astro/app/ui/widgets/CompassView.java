package com.astro.app.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Custom 3D rotating compass view with smooth animation.
 *
 * Displays a circular compass with cardinal directions (N, E, S, W).
 * Rotates smoothly to match device orientation with 0/360 degree wraparound handling.
 */
public class CompassView extends View {
    private static final String TAG = "CompassView";

    // Paint objects
    private Paint circlePaint;
    private Paint needlePaint;
    private Paint textPaint;
    private Paint tickPaint;

    // Current rotation (0-360 degrees, 0 = North)
    private float currentRotation = 0f;
    private float targetRotation = 0f;

    // Smooth rotation animator
    private ValueAnimator rotationAnimator;
    private static final long ANIMATION_DURATION_MS = 300;

    // Size and style
    private float compassRadius;
    private RectF compassBounds;

    public CompassView(Context context) {
        super(context);
        init();
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Outer circle (compass body)
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.argb(180, 40, 40, 40));  // Semi-transparent dark gray
        circlePaint.setStyle(Paint.Style.FILL);

        // Compass needle (points north)
        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setColor(Color.argb(255, 255, 80, 80));  // Bright red
        needlePaint.setStyle(Paint.Style.FILL);

        // Cardinal direction labels
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(16f * getResources().getDisplayMetrics().density);  // 16sp
        textPaint.setFakeBoldText(true);

        // Tick marks
        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(Color.argb(200, 200, 200, 200));  // Light gray
        tickPaint.setStrokeWidth(2f * getResources().getDisplayMetrics().density);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        compassBounds = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Calculate compass dimensions
        float size = Math.min(w, h);
        float padding = size * 0.1f;
        compassRadius = (size - padding * 2) / 2f;

        float centerX = w / 2f;
        float centerY = h / 2f;

        compassBounds.set(
            centerX - compassRadius,
            centerY - compassRadius,
            centerX + compassRadius,
            centerY + compassRadius
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float centerX = width / 2f;
        float centerY = height / 2f;

        // Draw outer circle
        canvas.drawCircle(centerX, centerY, compassRadius, circlePaint);

        // Save canvas state and rotate around center
        canvas.save();
        canvas.rotate(-currentRotation, centerX, centerY);

        // Draw tick marks (every 30 degrees)
        for (int i = 0; i < 12; i++) {
            float angle = i * 30f;
            float startRadius = compassRadius * 0.85f;
            float endRadius = compassRadius * 0.95f;

            float startX = centerX + startRadius * (float) Math.sin(Math.toRadians(angle));
            float startY = centerY - startRadius * (float) Math.cos(Math.toRadians(angle));
            float endX = centerX + endRadius * (float) Math.sin(Math.toRadians(angle));
            float endY = centerY - endRadius * (float) Math.cos(Math.toRadians(angle));

            canvas.drawLine(startX, startY, endX, endY, tickPaint);
        }

        // Draw cardinal directions
        drawCardinalDirection(canvas, centerX, centerY, 0f, "N", true);    // North (red)
        drawCardinalDirection(canvas, centerX, centerY, 90f, "E", false);  // East
        drawCardinalDirection(canvas, centerX, centerY, 180f, "S", false); // South
        drawCardinalDirection(canvas, centerX, centerY, 270f, "W", false); // West

        // Draw north needle (triangle pointing up)
        Path needlePath = new Path();
        float needleLength = compassRadius * 0.6f;
        float needleWidth = compassRadius * 0.15f;

        needlePath.moveTo(centerX, centerY - needleLength);  // Tip
        needlePath.lineTo(centerX - needleWidth, centerY);    // Left base
        needlePath.lineTo(centerX + needleWidth, centerY);    // Right base
        needlePath.close();

        canvas.drawPath(needlePath, needlePaint);

        // Restore canvas
        canvas.restore();
    }

    private void drawCardinalDirection(Canvas canvas, float centerX, float centerY,
                                        float angleDegrees, String label, boolean isNorth) {
        float textRadius = compassRadius * 0.7f;
        float angleRadians = (float) Math.toRadians(angleDegrees);

        float x = centerX + textRadius * (float) Math.sin(angleRadians);
        float y = centerY - textRadius * (float) Math.cos(angleRadians);

        // Adjust Y position to account for text baseline
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        y -= (metrics.ascent + metrics.descent) / 2f;

        // Use red for North, white for others
        int originalColor = textPaint.getColor();
        if (isNorth) {
            textPaint.setColor(Color.argb(255, 255, 80, 80));
        }

        canvas.drawText(label, x, y, textPaint);

        // Restore original color
        textPaint.setColor(originalColor);
    }

    /**
     * Set the compass rotation (0-360 degrees, 0 = North).
     * Animates smoothly to the target rotation with 0/360 wraparound handling.
     *
     * @param azimuthDegrees Device azimuth (0 = North, 90 = East, etc.)
     */
    public void setAzimuthRotation(float azimuthDegrees) {
        // Normalize to 0-360 range
        targetRotation = ((azimuthDegrees % 360f) + 360f) % 360f;

        // Cancel existing animation
        if (rotationAnimator != null && rotationAnimator.isRunning()) {
            rotationAnimator.cancel();
        }

        // Handle 0/360 wraparound: choose shortest path
        float delta = targetRotation - currentRotation;
        if (delta > 180f) {
            currentRotation += 360f;  // Cross 360 going down
        } else if (delta < -180f) {
            currentRotation -= 360f;  // Cross 0 going up
        }

        // Animate to target
        rotationAnimator = ValueAnimator.ofFloat(currentRotation, targetRotation);
        rotationAnimator.setDuration(ANIMATION_DURATION_MS);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.addUpdateListener(animation -> {
            currentRotation = (float) animation.getAnimatedValue();
            invalidate();
        });
        rotationAnimator.start();
    }

    /**
     * Get the current displayed rotation.
     *
     * @return Rotation in degrees (0-360)
     */
    public float getAzimuthRotation() {
        return ((currentRotation % 360f) + 360f) % 360f;
    }
}
