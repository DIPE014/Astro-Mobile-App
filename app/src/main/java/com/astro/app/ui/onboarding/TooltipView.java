package com.astro.app.ui.onboarding;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Custom view for displaying a tooltip bubble with optional anchor highlighting.
 *
 * Features:
 * - Semi-transparent dark overlay (scrim)
 * - Circular highlight punch-through for anchor view
 * - Speech bubble with rounded corners
 * - Arrow pointing to anchor (triangle)
 */
public class TooltipView extends FrameLayout {
    private static final String TAG = "TooltipView";

    private Paint scrimPaint;
    private Paint bubblePaint;
    private Paint clearPaint;

    private RectF anchorHighlight;
    private RectF bubbleRect;
    private Path arrowPath;
    private TooltipConfig.TooltipPosition position;

    public TooltipView(Context context) {
        super(context);
        init();
    }

    public TooltipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // Semi-transparent dark overlay
        scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scrimPaint.setColor(Color.argb(200, 0, 0, 0));  // 78% opacity black

        // White rounded speech bubble
        bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setColor(Color.WHITE);
        bubblePaint.setStyle(Paint.Style.FILL);

        // Clear paint for punch-through effect (highlight anchor)
        clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    /**
     * Configure the tooltip display.
     *
     * @param bubbleRect    Bounds of the speech bubble
     * @param anchorRect    Bounds of the anchor view (for highlight), or null
     * @param position      Position of tooltip relative to anchor
     */
    public void configure(RectF bubbleRect, RectF anchorRect, TooltipConfig.TooltipPosition position) {
        this.bubbleRect = bubbleRect;
        this.anchorHighlight = anchorRect;
        this.position = position;

        // Build arrow path (triangle pointing to anchor)
        if (anchorRect != null && position != TooltipConfig.TooltipPosition.CENTER) {
            buildArrowPath();
        } else {
            arrowPath = null;
        }

        invalidate();
    }

    private void buildArrowPath() {
        if (bubbleRect == null || anchorHighlight == null) return;

        arrowPath = new Path();
        float arrowSize = 20f * getResources().getDisplayMetrics().density;  // 20dp

        switch (position) {
            case ABOVE:
                // Arrow points down from bottom of bubble to top of anchor
                float bottomCenterX = bubbleRect.centerX();
                float bottomY = bubbleRect.bottom;
                arrowPath.moveTo(bottomCenterX, bottomY);
                arrowPath.lineTo(bottomCenterX - arrowSize, bottomY + arrowSize);
                arrowPath.lineTo(bottomCenterX + arrowSize, bottomY + arrowSize);
                arrowPath.close();
                break;

            case BELOW:
                // Arrow points up from top of bubble to bottom of anchor
                float topCenterX = bubbleRect.centerX();
                float topY = bubbleRect.top;
                arrowPath.moveTo(topCenterX, topY);
                arrowPath.lineTo(topCenterX - arrowSize, topY - arrowSize);
                arrowPath.lineTo(topCenterX + arrowSize, topY - arrowSize);
                arrowPath.close();
                break;

            case LEFT:
                // Arrow points right from right edge of bubble
                float rightY = bubbleRect.centerY();
                float rightX = bubbleRect.right;
                arrowPath.moveTo(rightX, rightY);
                arrowPath.lineTo(rightX + arrowSize, rightY - arrowSize);
                arrowPath.lineTo(rightX + arrowSize, rightY + arrowSize);
                arrowPath.close();
                break;

            case RIGHT:
                // Arrow points left from left edge of bubble
                float leftY = bubbleRect.centerY();
                float leftX = bubbleRect.left;
                arrowPath.moveTo(leftX, leftY);
                arrowPath.lineTo(leftX - arrowSize, leftY - arrowSize);
                arrowPath.lineTo(leftX - arrowSize, leftY + arrowSize);
                arrowPath.close();
                break;

            case CENTER:
                // No arrow for center position
                break;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Draw scrim (dark overlay)
        canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);

        // Punch through scrim to highlight anchor (if specified)
        if (anchorHighlight != null) {
            float highlightPadding = 16f * getResources().getDisplayMetrics().density;
            canvas.drawCircle(
                anchorHighlight.centerX(),
                anchorHighlight.centerY(),
                Math.max(anchorHighlight.width(), anchorHighlight.height()) / 2f + highlightPadding,
                clearPaint
            );
        }

        // Draw speech bubble
        if (bubbleRect != null) {
            float cornerRadius = 12f * getResources().getDisplayMetrics().density;
            canvas.drawRoundRect(bubbleRect, cornerRadius, cornerRadius, bubblePaint);

            // Draw arrow
            if (arrowPath != null) {
                canvas.drawPath(arrowPath, bubblePaint);
            }
        }

        // Draw children (TextView with message)
        super.dispatchDraw(canvas);
    }
}
