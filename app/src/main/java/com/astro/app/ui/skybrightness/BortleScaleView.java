package com.astro.app.ui.skybrightness;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom view that draws a horizontal Bortle-scale gauge with 9 coloured
 * segments and a triangle marker indicating the current class.
 *
 * <p>Usage: call {@link #setBortleClass(int)} to move the marker. The view
 * invalidates itself and redraws.</p>
 */
public class BortleScaleView extends View {

    /** Segment colours for Bortle classes 1-9. */
    private static final int[] SEGMENT_COLORS = {
            0xFF1B5E20, // 1 - dark green
            0xFF2E7D32, // 2 - green
            0xFF4CAF50, // 3 - light green
            0xFFCDDC39, // 4 - lime
            0xFFFFEB3B, // 5 - yellow
            0xFFFFC107, // 6 - amber
            0xFFFF9800, // 7 - orange
            0xFFF44336, // 8 - red
            0xFFB71C1C, // 9 - dark red
    };

    private static final int SEGMENT_COUNT = 9;
    private static final float SEGMENT_CORNER_RADIUS_DP = 4f;
    private static final float SEGMENT_GAP_DP = 2f;
    private static final float MARKER_SIZE_DP = 10f;
    private static final float LABEL_TEXT_SIZE_SP = 11f;
    private static final float BAR_TOP_FRACTION = 0.0f;
    private static final float BAR_BOTTOM_FRACTION = 0.55f;
    private static final float LABEL_BASELINE_FRACTION = 0.85f;

    private int bortleClass = 0; // 0 = no marker shown

    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path markerPath = new Path();
    private final RectF segmentRect = new RectF();

    private float density;
    private float scaledDensity;

    public BortleScaleView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public BortleScaleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BortleScaleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        density = context.getResources().getDisplayMetrics().density;
        scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;

        segmentPaint.setStyle(Paint.Style.FILL);

        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setColor(0xFFFFFFFF);

        labelPaint.setStyle(Paint.Style.FILL);
        labelPaint.setColor(0xFFB3B3B3); // text_secondary equivalent
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(LABEL_TEXT_SIZE_SP * scaledDensity);
    }

    /**
     * Sets the Bortle class (1-9) and redraws the marker.
     * Pass 0 to hide the marker.
     */
    public void setBortleClass(int bortle) {
        this.bortleClass = Math.max(0, Math.min(9, bortle));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) (60 * density);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height;

        if (heightMode == MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, MeasureSpec.getSize(heightMeasureSpec));
        } else {
            height = desiredHeight;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth() - getPaddingLeft() - getPaddingRight();
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        if (w <= 0 || h <= 0) return;

        float left = getPaddingLeft();
        float top = getPaddingTop();

        float gap = SEGMENT_GAP_DP * density;
        float totalGaps = gap * (SEGMENT_COUNT - 1);
        float segWidth = (w - totalGaps) / SEGMENT_COUNT;
        float cornerRadius = SEGMENT_CORNER_RADIUS_DP * density;

        float barTop = top + h * BAR_TOP_FRACTION;
        float barBottom = top + h * BAR_BOTTOM_FRACTION;
        float labelY = top + h * LABEL_BASELINE_FRACTION;

        // Draw segments
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            float sx = left + i * (segWidth + gap);
            segmentRect.set(sx, barTop, sx + segWidth, barBottom);
            segmentPaint.setColor(SEGMENT_COLORS[i]);
            canvas.drawRoundRect(segmentRect, cornerRadius, cornerRadius, segmentPaint);

            // Label
            String label = String.valueOf(i + 1);
            float labelX = sx + segWidth / 2f;
            canvas.drawText(label, labelX, labelY, labelPaint);
        }

        // Draw triangle marker
        if (bortleClass >= 1 && bortleClass <= SEGMENT_COUNT) {
            int idx = bortleClass - 1;
            float markerCenterX = left + idx * (segWidth + gap) + segWidth / 2f;
            float markerSize = MARKER_SIZE_DP * density;
            float markerTop = barBottom + 2 * density;

            markerPath.reset();
            markerPath.moveTo(markerCenterX, markerTop);
            markerPath.lineTo(markerCenterX - markerSize / 2f, markerTop + markerSize);
            markerPath.lineTo(markerCenterX + markerSize / 2f, markerTop + markerSize);
            markerPath.close();

            markerPaint.setColor(0xFFFFFFFF);
            canvas.drawPath(markerPath, markerPaint);
        }
    }
}
