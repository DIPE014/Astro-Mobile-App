package com.astro.app.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
    private Paint globeOutlinePaint;   // Globe silhouette ring
    private Paint globeRingPaint;      // Equatorial ring on globe (inside tilt)

    // Current rotation (0-360 degrees, 0 = North)
    private float currentRotation = 0f;
    private float targetRotation = 0f;

    // Current pitch/altitude for 3D tilt (0 = flat/horizon, 90 = zenith)
    private float currentPitch = 0f;

    // Reusable Camera for 3D tilt transform
    private final Camera tiltCamera = new Camera();
    private final Matrix tiltMatrix = new Matrix();

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

        // Globe silhouette (outer ring, no tilt applied — always a perfect circle)
        globeOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        globeOutlinePaint.setColor(Color.argb(120, 160, 200, 255));  // Faint blue-white
        globeOutlinePaint.setStyle(Paint.Style.STROKE);
        globeOutlinePaint.setStrokeWidth(1.5f * getResources().getDisplayMetrics().density);

        // Equatorial ring drawn inside the tilt (appears as ellipse when device is tilted)
        globeRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        globeRingPaint.setColor(Color.argb(70, 160, 200, 255));  // Dimmer blue-white
        globeRingPaint.setStyle(Paint.Style.STROKE);
        globeRingPaint.setStrokeWidth(1f * getResources().getDisplayMetrics().density);
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
        float globeRadius = compassRadius * 1.18f;

        // Build 3D tilt matrix.
        // Altitude semantics: 0 = horizon (device upright), 90 = zenith (device flat).
        // When device is flat (alt=90) the compass face is viewed top-down → face-on circle → 0° tilt.
        // When device points at horizon (alt=0) the compass face is edge-on → 90° tilt.
        // Therefore tilt angle = 90 - altitude.
        tiltCamera.save();
        tiltCamera.rotateX(90f - currentPitch);
        tiltCamera.getMatrix(tiltMatrix);
        tiltCamera.restore();
        tiltMatrix.preTranslate(-centerX, -centerY);
        tiltMatrix.postTranslate(centerX, centerY);

        // --- Globe silhouette (NO tilt — always a perfect circle) ---
        canvas.drawCircle(centerX, centerY, globeRadius, globeOutlinePaint);

        // --- Everything inside the 3D tilt ---
        canvas.save();
        canvas.concat(tiltMatrix);

        // Equatorial ring of the globe (same plane as compass disc, globe radius)
        RectF globeOval = new RectF(centerX - globeRadius, centerY - globeRadius,
                                    centerX + globeRadius, centerY + globeRadius);
        canvas.drawOval(globeOval, globeRingPaint);

        // Compass background disc
        canvas.drawCircle(centerX, centerY, compassRadius, circlePaint);

        // Compass interior — apply heading rotation on top of the tilt
        canvas.save();
        canvas.rotate(-currentRotation, centerX, centerY);

        // Tick marks (every 30 degrees)
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

        // Cardinal directions
        drawCardinalDirection(canvas, centerX, centerY, 0f, "N", true);
        drawCardinalDirection(canvas, centerX, centerY, 90f, "E", false);
        drawCardinalDirection(canvas, centerX, centerY, 180f, "S", false);
        drawCardinalDirection(canvas, centerX, centerY, 270f, "W", false);

        // North needle (red triangle)
        Path needlePath = new Path();
        float needleLength = compassRadius * 0.6f;
        float needleWidth = compassRadius * 0.15f;
        needlePath.moveTo(centerX, centerY - needleLength);
        needlePath.lineTo(centerX - needleWidth, centerY);
        needlePath.lineTo(centerX + needleWidth, centerY);
        needlePath.close();
        canvas.drawPath(needlePath, needlePaint);

        canvas.restore(); // end heading rotation
        canvas.restore(); // end 3D tilt
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

    /**
     * Set the device pitch/altitude for 3D tilt rendering.
     * 0 = device flat / pointing at horizon (compass appears as circle),
     * 90 = device pointing straight up (compass tilts fully away, edge-on).
     *
     * @param pitchDegrees Altitude/tilt in degrees (-90 to +90)
     */
    public void setPitch(float pitchDegrees) {
        this.currentPitch = pitchDegrees;
        invalidate();
    }
}
