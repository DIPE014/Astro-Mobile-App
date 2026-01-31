package com.astro.app.ui.platesolve;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.ml.model.DetectedStar;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom view that draws star markers and labels on top of the captured image.
 *
 * <p>This view is designed to be overlaid on an ImageView displaying the captured
 * night sky photo. It draws circles around detected stars and displays their names
 * or Hipparcos IDs.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Draws circular markers around detected star positions</li>
 *   <li>Shows star names or HIP IDs as labels</li>
 *   <li>Scales properly when the underlying image is resized</li>
 *   <li>Supports custom colors and marker sizes</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * StarOverlayView overlayView = findViewById(R.id.starOverlayView);
 * overlayView.setImageDimensions(imageWidth, imageHeight);
 * overlayView.setDetectedStars(result.getDetectedStars());
 * }</pre>
 */
public class StarOverlayView extends View {

    private static final String TAG = "StarOverlayView";

    /** Default marker radius in dp */
    private static final float DEFAULT_MARKER_RADIUS_DP = 16f;

    /** Default stroke width in dp */
    private static final float DEFAULT_STROKE_WIDTH_DP = 2f;

    /** Default label text size in sp */
    private static final float DEFAULT_LABEL_SIZE_SP = 12f;

    /** Default marker color */
    private static final int DEFAULT_MARKER_COLOR = 0xFFFFD700; // Gold

    /** Default label background color */
    private static final int DEFAULT_LABEL_BG_COLOR = 0xAA000000; // Semi-transparent black

    /** Default label text color */
    private static final int DEFAULT_LABEL_TEXT_COLOR = Color.WHITE;

    // Paints
    private final Paint markerPaint;
    private final Paint labelBackgroundPaint;
    private final Paint labelTextPaint;

    // Data
    private final List<DetectedStar> detectedStars = new ArrayList<>();

    // Image dimensions for coordinate scaling
    private int imageWidth = 0;
    private int imageHeight = 0;

    // Marker properties
    private float markerRadius;
    private float labelPadding;

    /**
     * Creates a new StarOverlayView.
     *
     * @param context The context
     */
    public StarOverlayView(@NonNull Context context) {
        super(context);
        markerPaint = new Paint();
        labelBackgroundPaint = new Paint();
        labelTextPaint = new Paint();
        init();
    }

    /**
     * Creates a new StarOverlayView with attributes.
     *
     * @param context The context
     * @param attrs   The attribute set
     */
    public StarOverlayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        markerPaint = new Paint();
        labelBackgroundPaint = new Paint();
        labelTextPaint = new Paint();
        init();
    }

    /**
     * Creates a new StarOverlayView with attributes and style.
     *
     * @param context      The context
     * @param attrs        The attribute set
     * @param defStyleAttr The default style attribute
     */
    public StarOverlayView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        markerPaint = new Paint();
        labelBackgroundPaint = new Paint();
        labelTextPaint = new Paint();
        init();
    }

    /**
     * Initializes paint objects and dimensions.
     */
    private void init() {
        float density = getResources().getDisplayMetrics().density;
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;

        markerRadius = DEFAULT_MARKER_RADIUS_DP * density;
        labelPadding = 4f * density;

        // Marker paint (circle around star)
        markerPaint.setAntiAlias(true);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH_DP * density);
        markerPaint.setColor(DEFAULT_MARKER_COLOR);

        // Label background paint
        labelBackgroundPaint.setAntiAlias(true);
        labelBackgroundPaint.setStyle(Paint.Style.FILL);
        labelBackgroundPaint.setColor(DEFAULT_LABEL_BG_COLOR);

        // Label text paint
        labelTextPaint.setAntiAlias(true);
        labelTextPaint.setTextSize(DEFAULT_LABEL_SIZE_SP * scaledDensity);
        labelTextPaint.setColor(DEFAULT_LABEL_TEXT_COLOR);
    }

    /**
     * Sets the original dimensions of the captured image.
     *
     * <p>This is required for proper coordinate scaling when the image is displayed
     * at a different size than its original resolution.</p>
     *
     * @param width  Original image width in pixels
     * @param height Original image height in pixels
     */
    public void setImageDimensions(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
        invalidate();
    }

    /**
     * Sets the list of detected stars to display.
     *
     * <p>Call this after {@link #setImageDimensions(int, int)} to ensure
     * proper coordinate scaling.</p>
     *
     * @param stars The list of detected stars
     */
    public void setDetectedStars(@NonNull List<DetectedStar> stars) {
        detectedStars.clear();
        detectedStars.addAll(stars);
        invalidate();
    }

    /**
     * Clears all detected stars from the overlay.
     */
    public void clearStars() {
        detectedStars.clear();
        invalidate();
    }

    /**
     * Sets the marker color.
     *
     * @param color The color as an ARGB integer
     */
    public void setMarkerColor(int color) {
        markerPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (detectedStars.isEmpty() || imageWidth == 0 || imageHeight == 0) {
            return;
        }

        // Calculate scaling factors to map image coordinates to view coordinates
        float scaleX = (float) getWidth() / imageWidth;
        float scaleY = (float) getHeight() / imageHeight;

        // Use the smaller scale to maintain aspect ratio (fitCenter behavior)
        float scale = Math.min(scaleX, scaleY);

        // Calculate offsets for centering
        float offsetX = (getWidth() - imageWidth * scale) / 2f;
        float offsetY = (getHeight() - imageHeight * scale) / 2f;

        for (DetectedStar star : detectedStars) {
            // Convert image coordinates to view coordinates
            float viewX = star.getPixelX() * scale + offsetX;
            float viewY = star.getPixelY() * scale + offsetY;

            // Draw marker circle
            canvas.drawCircle(viewX, viewY, markerRadius, markerPaint);

            // Draw label
            String label = star.getDisplayName();
            drawLabel(canvas, label, viewX, viewY);
        }
    }

    /**
     * Draws a label with a background near the star marker.
     *
     * @param canvas The canvas to draw on
     * @param text   The label text
     * @param x      X coordinate of the star
     * @param y      Y coordinate of the star
     */
    private void drawLabel(@NonNull Canvas canvas, @NonNull String text, float x, float y) {
        // Measure text
        float textWidth = labelTextPaint.measureText(text);
        Paint.FontMetrics fm = labelTextPaint.getFontMetrics();
        float textHeight = fm.bottom - fm.top;

        // Position label to the right and slightly above the marker
        float labelX = x + markerRadius + labelPadding;
        float labelY = y - markerRadius / 2f;

        // Ensure label stays within view bounds
        if (labelX + textWidth + labelPadding * 2 > getWidth()) {
            // Move label to the left of the marker
            labelX = x - markerRadius - labelPadding - textWidth - labelPadding * 2;
        }
        if (labelY - textHeight - labelPadding < 0) {
            labelY = y + markerRadius + textHeight + labelPadding;
        }

        // Draw background
        RectF bgRect = new RectF(
                labelX,
                labelY - textHeight - labelPadding,
                labelX + textWidth + labelPadding * 2,
                labelY + labelPadding
        );
        float cornerRadius = labelPadding * 2;
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, labelBackgroundPaint);

        // Draw text
        canvas.drawText(text, labelX + labelPadding, labelY - labelPadding, labelTextPaint);
    }
}
