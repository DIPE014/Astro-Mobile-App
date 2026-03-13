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
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.List;

/**
 * Custom view for displaying a tooltip bubble with optional anchor highlighting.
 *
 * Features:
 * - Semi-transparent dark overlay (scrim)
 * - Circular highlight punch-through for anchor view
 * - Speech bubble with rounded corners
 * - Arrow pointing to anchor (triangle)
 * - Multi-highlight support for extra views
 * - Interactive mode for touch passthrough to anchor
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
    private List<RectF> extraHighlights;
    private List<View> extraHighlightViews;
    private boolean interactive;
    private View anchorView;

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

        // Semi-transparent dark overlay — 40% opacity for lighter scrim
        scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scrimPaint.setColor(Color.argb(100, 0, 0, 0));

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
     * @param bubbleRect      Bounds of the speech bubble
     * @param anchorRect      Bounds of the anchor view (for highlight), or null
     * @param position        Position of tooltip relative to anchor
     * @param extraHighlights Additional highlight rects to punch through scrim
     * @param interactive     If true, taps on anchor pass through
     * @param anchorView      The anchor view for interactive mode
     */
    public void configure(RectF bubbleRect, RectF anchorRect,
                          TooltipConfig.TooltipPosition position,
                          List<RectF> extraHighlights,
                          List<View> extraHighlightViews,
                          boolean interactive, View anchorView) {
        this.bubbleRect = bubbleRect;
        this.anchorHighlight = anchorRect;
        this.position = position;
        this.extraHighlights = extraHighlights;
        this.extraHighlightViews = extraHighlightViews;
        this.interactive = interactive;
        this.anchorView = anchorView;

        // Build arrow path (triangle pointing to anchor)
        if (anchorRect != null && position != TooltipConfig.TooltipPosition.CENTER) {
            buildArrowPath();
        } else {
            arrowPath = null;
        }

        invalidate();
    }

    /** Backward-compatible configure without extra features. */
    public void configure(RectF bubbleRect, RectF anchorRect, TooltipConfig.TooltipPosition position) {
        configure(bubbleRect, anchorRect, position, null, null, false, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (interactive && anchorHighlight != null && anchorView != null
                && event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            float highlightPadding = 16f * getResources().getDisplayMetrics().density;
            float radius = Math.max(anchorHighlight.width(), anchorHighlight.height()) / 2f + highlightPadding;
            float dx = x - anchorHighlight.centerX();
            float dy = y - anchorHighlight.centerY();
            if (dx * dx + dy * dy <= radius * radius) {
                anchorView.performClick();
                return true;
            }

            // Also check extra highlights for interactive passthrough
            if (extraHighlights != null && extraHighlightViews != null) {
                for (int i = 0; i < extraHighlights.size() && i < extraHighlightViews.size(); i++) {
                    RectF rect = extraHighlights.get(i);
                    float er = Math.max(rect.width(), rect.height()) / 2f + highlightPadding;
                    float edx = x - rect.centerX();
                    float edy = y - rect.centerY();
                    if (edx * edx + edy * edy <= er * er) {
                        extraHighlightViews.get(i).performClick();
                        return true;
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private void buildArrowPath() {
        if (bubbleRect == null || anchorHighlight == null) return;

        arrowPath = new Path();
        float arrowSize = 20f * getResources().getDisplayMetrics().density;  // 20dp

        switch (position) {
            case ABOVE:
                // Arrow: base flush with bubble bottom, tip points down toward anchor
                float bottomCenterX = bubbleRect.centerX();
                float bottomY = bubbleRect.bottom;
                arrowPath.moveTo(bottomCenterX - arrowSize, bottomY);
                arrowPath.lineTo(bottomCenterX + arrowSize, bottomY);
                arrowPath.lineTo(bottomCenterX, bottomY + arrowSize);
                arrowPath.close();
                break;

            case BELOW:
                // Arrow: base flush with bubble top, tip points up toward anchor
                float topCenterX = bubbleRect.centerX();
                float topY = bubbleRect.top;
                arrowPath.moveTo(topCenterX - arrowSize, topY);
                arrowPath.lineTo(topCenterX + arrowSize, topY);
                arrowPath.lineTo(topCenterX, topY - arrowSize);
                arrowPath.close();
                break;

            case LEFT:
                // Arrow: base flush with bubble right edge, tip points right toward anchor
                float rightY = bubbleRect.centerY();
                float rightX = bubbleRect.right;
                arrowPath.moveTo(rightX, rightY - arrowSize);
                arrowPath.lineTo(rightX, rightY + arrowSize);
                arrowPath.lineTo(rightX + arrowSize, rightY);
                arrowPath.close();
                break;

            case RIGHT:
                // Arrow: base flush with bubble left edge, tip points left toward anchor
                float leftY = bubbleRect.centerY();
                float leftX = bubbleRect.left;
                arrowPath.moveTo(leftX, leftY - arrowSize);
                arrowPath.lineTo(leftX, leftY + arrowSize);
                arrowPath.lineTo(leftX - arrowSize, leftY);
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

        float highlightPadding = 16f * getResources().getDisplayMetrics().density;

        // Punch through scrim to highlight anchor (if specified)
        if (anchorHighlight != null) {
            canvas.drawCircle(
                anchorHighlight.centerX(),
                anchorHighlight.centerY(),
                Math.max(anchorHighlight.width(), anchorHighlight.height()) / 2f + highlightPadding,
                clearPaint
            );
        }

        // Punch through scrim for extra highlighted views
        if (extraHighlights != null) {
            for (RectF rect : extraHighlights) {
                canvas.drawCircle(
                    rect.centerX(),
                    rect.centerY(),
                    Math.max(rect.width(), rect.height()) / 2f + highlightPadding,
                    clearPaint
                );
            }
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
