package com.astro.app.search;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

/**
 * Custom view that displays an arrow pointing toward a celestial target.
 *
 * <p>When the target is off-screen, the arrow points in the direction
 * the user should look. As the target comes into view, the arrow fades
 * and a circle indicator expands.</p>
 *
 * <h3>Visual States:</h3>
 * <ul>
 *   <li><b>Off-screen:</b> Arrow + circle indicator, arrow color indicates distance</li>
 *   <li><b>In view:</b> Expanding circle, arrow fades out</li>
 *   <li><b>Centered:</b> Full-size pulsing circle, no arrow</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * SearchArrowView arrowView = findViewById(R.id.searchArrow);
 * arrowView.setTarget(targetRa, targetDec, "Sirius");
 * arrowView.updatePointing(currentRa, currentDec);
 * }</pre>
 */
public class SearchArrowView extends View {

    private static final String TAG = "SearchArrowView";

    // Paints
    private Paint arrowPaint;
    private Paint circlePaint;
    private Paint labelPaint;
    private Paint backgroundPaint;
    private Paint closeButtonPaint;
    private Paint closeButtonIconPaint;

    // Colors
    private static final int COLOR_ARROW_NEAR = 0xFFFF5722;    // Orange-red when close
    private static final int COLOR_ARROW_FAR = 0xFF2196F3;     // Blue when far
    private static final int COLOR_CIRCLE = 0xFFBB86FC;        // Primary purple
    private static final int COLOR_LABEL = 0xFFFFFFFF;         // White
    private static final int COLOR_BACKGROUND = 0x80000000;    // Semi-transparent black
    private static final int COLOR_CLOSE_BUTTON = 0xFFCF6679;  // Red for close

    // Close button
    private static final float CLOSE_BUTTON_SIZE = 48f;
    private static final float CLOSE_BUTTON_MARGIN = 24f;
    private RectF closeButtonRect = new RectF();

    // Callback for auto-dismiss
    private OnTargetCenteredListener targetCenteredListener;

    // Target coordinates (RA/Dec in degrees)
    private float targetRa = 0f;
    private float targetDec = 0f;
    private String targetName = "";

    // Current view direction (RA/Dec in degrees)
    private float viewRa = 0f;
    private float viewDec = 45f;

    // State
    private boolean isActive = false;
    private float focusProgress = 0f;  // 0 = off-screen, 1 = centered
    private float arrowAngle = 0f;     // Angle in radians
    private float distance = 1f;        // 0 = very close, 1 = far away

    // Dimensions
    private static final float ARROW_SIZE = 80f;
    private static final float CIRCLE_RADIUS = 40f;
    private static final float MAX_CIRCLE_RADIUS = 200f;
    private static final float ARROW_OFFSET = 120f;

    // Animation
    private ValueAnimator pulseAnimator;
    private float pulseScale = 1f;

    // Path for arrow shape
    private Path arrowPath;

    public SearchArrowView(Context context) {
        super(context);
        init();
    }

    public SearchArrowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SearchArrowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Arrow paint
        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(COLOR_ARROW_FAR);

        // Circle paint
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(4f);
        circlePaint.setColor(COLOR_CIRCLE);

        // Label paint
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(COLOR_LABEL);
        labelPaint.setTextSize(32f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Background paint
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(COLOR_BACKGROUND);
        backgroundPaint.setStyle(Paint.Style.FILL);

        // Close button paint
        closeButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        closeButtonPaint.setColor(COLOR_CLOSE_BUTTON);
        closeButtonPaint.setStyle(Paint.Style.FILL);

        // Close button icon paint (X)
        closeButtonIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        closeButtonIconPaint.setColor(Color.WHITE);
        closeButtonIconPaint.setStyle(Paint.Style.STROKE);
        closeButtonIconPaint.setStrokeWidth(4f);
        closeButtonIconPaint.setStrokeCap(Paint.Cap.ROUND);

        // Create arrow path
        arrowPath = new Path();
        createArrowPath();

        // Setup pulse animation
        pulseAnimator = ValueAnimator.ofFloat(0.9f, 1.1f);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulseScale = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    /**
     * Creates the arrow shape path.
     */
    private void createArrowPath() {
        arrowPath.reset();
        float size = ARROW_SIZE;

        // Arrow pointing up (will be rotated)
        arrowPath.moveTo(0, -size / 2);           // Top point
        arrowPath.lineTo(size / 3, size / 4);     // Bottom right
        arrowPath.lineTo(0, size / 8);            // Bottom center notch
        arrowPath.lineTo(-size / 3, size / 4);    // Bottom left
        arrowPath.close();
    }

    /**
     * Sets the search target.
     *
     * @param ra   Target Right Ascension in degrees
     * @param dec  Target Declination in degrees
     * @param name Target name for display
     */
    public void setTarget(float ra, float dec, String name) {
        this.targetRa = ra;
        this.targetDec = dec;
        this.targetName = name != null ? name : "";
        this.isActive = true;

        // Start pulse animation
        if (!pulseAnimator.isRunning()) {
            pulseAnimator.start();
        }

        updatePointing(viewRa, viewDec);
    }

    /**
     * Clears the search target.
     */
    public void clearTarget() {
        this.isActive = false;
        this.targetName = "";
        pulseAnimator.cancel();
        invalidate();
    }

    /**
     * Updates the current view direction and recalculates arrow position.
     *
     * @param viewRa  Current view Right Ascension in degrees
     * @param viewDec Current view Declination in degrees
     */
    public void updatePointing(float viewRa, float viewDec) {
        this.viewRa = viewRa;
        this.viewDec = viewDec;

        if (!isActive) {
            return;
        }

        // Calculate angular distance using proper spherical formula
        // This correctly handles the RA compression near the poles
        double angularDistance = calculateAngularDistance(viewRa, viewDec, targetRa, targetDec);
        this.distance = (float) Math.min(1.0, angularDistance / 90.0);

        // Calculate arrow angle (pointing direction) using planar approximation
        // This is fine for the arrow direction since we just need the general direction
        float dRa = normalizeAngle(targetRa - viewRa);
        float dDec = targetDec - viewDec;
        // Account for RA compression at current declination for arrow direction
        float raScale = (float) Math.cos(Math.toRadians((viewDec + targetDec) / 2.0));
        this.arrowAngle = (float) Math.atan2(dDec, dRa * raScale);

        // Calculate focus progress (0 = far, 1 = centered)
        // Consider "in focus" when within 10 degrees (more lenient threshold)
        float oldProgress = this.focusProgress;
        this.focusProgress = 1f - Math.min(1f, (float) angularDistance / 10f);

        // Debug logging for troubleshooting
        android.util.Log.d(TAG, String.format(
            "SEARCH: view(RA=%.1f, Dec=%.1f) target(RA=%.1f, Dec=%.1f) dist=%.2f° focus=%.2f",
            viewRa, viewDec, targetRa, targetDec, angularDistance, focusProgress));

        // Notify listener when target becomes centered (>95% focused)
        if (focusProgress >= 0.95f && oldProgress < 0.95f && targetCenteredListener != null) {
            targetCenteredListener.onTargetCentered();
        }

        invalidate();
    }

    /**
     * Calculates the angular distance between two points on the celestial sphere
     * using the haversine formula. This properly handles RA wraparound and
     * the compression of RA near the poles.
     *
     * @param ra1  First point RA in degrees
     * @param dec1 First point Dec in degrees
     * @param ra2  Second point RA in degrees
     * @param dec2 Second point Dec in degrees
     * @return Angular distance in degrees
     */
    private double calculateAngularDistance(float ra1, float dec1, float ra2, float dec2) {
        // Convert to radians
        double ra1Rad = Math.toRadians(ra1);
        double dec1Rad = Math.toRadians(dec1);
        double ra2Rad = Math.toRadians(ra2);
        double dec2Rad = Math.toRadians(dec2);

        // Haversine formula for angular distance on a sphere
        double dRa = ra2Rad - ra1Rad;
        double dDec = dec2Rad - dec1Rad;

        double a = Math.sin(dDec / 2) * Math.sin(dDec / 2) +
                   Math.cos(dec1Rad) * Math.cos(dec2Rad) *
                   Math.sin(dRa / 2) * Math.sin(dRa / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Convert back to degrees
        return Math.toDegrees(c);
    }

    /**
     * Normalizes an angle to the range [-180, 180].
     */
    private float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isActive) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Draw semi-transparent background
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // Calculate arrow color based on distance (blue -> red as closer)
        int arrowColor = interpolateColor(COLOR_ARROW_FAR, COLOR_ARROW_NEAR, 1f - distance);
        arrowPaint.setColor(arrowColor);

        // Draw based on focus state
        if (focusProgress < 0.9f) {
            // Target is off-center: draw arrow
            drawArrow(canvas, centerX, centerY);
        }

        // Draw circle indicator
        drawCircleIndicator(canvas, centerX, centerY);

        // Draw target name
        if (!targetName.isEmpty()) {
            float labelY = centerY + (focusProgress > 0.5f ? 0 : ARROW_OFFSET + ARROW_SIZE);
            canvas.drawText(targetName, centerX, labelY + 50, labelPaint);
        }

        // Draw close button (X) in top-right corner
        drawCloseButton(canvas, width, height);

        // Draw "Tap anywhere to dismiss" hint at bottom
        labelPaint.setTextSize(24f);
        canvas.drawText("Tap X to dismiss", centerX, height - 40, labelPaint);
        labelPaint.setTextSize(32f);  // Reset
    }

    /**
     * Draws the close button (X) in the top-right corner.
     */
    private void drawCloseButton(Canvas canvas, int width, int height) {
        float right = width - CLOSE_BUTTON_MARGIN;
        float top = CLOSE_BUTTON_MARGIN;
        float left = right - CLOSE_BUTTON_SIZE;
        float bottom = top + CLOSE_BUTTON_SIZE;

        closeButtonRect.set(left, top, right, bottom);

        // Draw circle background
        float centerX = left + CLOSE_BUTTON_SIZE / 2;
        float centerY = top + CLOSE_BUTTON_SIZE / 2;
        canvas.drawCircle(centerX, centerY, CLOSE_BUTTON_SIZE / 2, closeButtonPaint);

        // Draw X icon
        float inset = CLOSE_BUTTON_SIZE / 4;
        canvas.drawLine(left + inset, top + inset, right - inset, bottom - inset, closeButtonIconPaint);
        canvas.drawLine(right - inset, top + inset, left + inset, bottom - inset, closeButtonIconPaint);
    }

    /**
     * Draws the directional arrow.
     */
    private void drawArrow(Canvas canvas, float centerX, float centerY) {
        canvas.save();

        // Position arrow around the center
        float offsetX = (float) Math.cos(arrowAngle) * ARROW_OFFSET;
        float offsetY = (float) -Math.sin(arrowAngle) * ARROW_OFFSET;  // Flip Y

        canvas.translate(centerX + offsetX, centerY + offsetY);

        // Rotate arrow to point OUTWARD toward target direction
        // arrowAngle points from view center toward target
        // Arrow path points UP by default, so we rotate to point outward
        float rotationDegrees = (float) Math.toDegrees(-arrowAngle) + 90;
        canvas.rotate(rotationDegrees);

        // Fade arrow as target comes into view
        int alpha = (int) (255 * (1f - focusProgress));
        arrowPaint.setAlpha(alpha);

        canvas.drawPath(arrowPath, arrowPaint);
        canvas.restore();
    }

    /**
     * Draws the circle indicator around the target.
     */
    private void drawCircleIndicator(Canvas canvas, float centerX, float centerY) {
        // Calculate circle radius based on focus state
        float radius = CIRCLE_RADIUS + (MAX_CIRCLE_RADIUS - CIRCLE_RADIUS) * focusProgress;
        radius *= pulseScale;

        // Draw circle
        int alpha = focusProgress > 0.5f ? 255 : (int) (255 * focusProgress * 2);
        circlePaint.setAlpha(alpha);

        canvas.drawCircle(centerX, centerY, radius, circlePaint);

        // Draw inner filled circle when centered
        if (focusProgress > 0.8f) {
            Paint fillPaint = new Paint(circlePaint);
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setAlpha((int) (80 * (focusProgress - 0.8f) / 0.2f));
            canvas.drawCircle(centerX, centerY, radius * 0.3f, fillPaint);
        }
    }

    /**
     * Interpolates between two colors.
     */
    private int interpolateColor(int color1, int color2, float fraction) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * fraction);
        int r = (int) (r1 + (r2 - r1) * fraction);
        int g = (int) (g1 + (g2 - g1) * fraction);
        int b = (int) (b1 + (b2 - b1) * fraction);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Returns whether a search target is active.
     *
     * @return true if target is set
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Returns the current focus progress.
     *
     * @return 0 (off-screen) to 1 (centered)
     */
    public float getFocusProgress() {
        return focusProgress;
    }

    /**
     * Sets a listener for when the target becomes centered.
     */
    public void setOnTargetCenteredListener(OnTargetCenteredListener listener) {
        this.targetCenteredListener = listener;
    }

    /**
     * Callback interface for when target is centered.
     */
    public interface OnTargetCenteredListener {
        void onTargetCentered();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && isActive) {
            float x = event.getX();
            float y = event.getY();

            // Check if close button was tapped
            if (closeButtonRect.contains(x, y)) {
                performClick();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pulseAnimator.cancel();
    }
}
