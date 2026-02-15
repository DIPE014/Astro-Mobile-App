package com.astro.app.ui.onboarding;

import android.view.View;

/**
 * Configuration for a single tooltip in the interactive tutorial.
 */
public class TooltipConfig {
    private final View anchorView;
    private final String message;
    private final TooltipPosition position;
    private final boolean highlightAnchor;

    public enum TooltipPosition {
        ABOVE,  // Tooltip appears above the anchor view
        BELOW,  // Tooltip appears below the anchor view
        LEFT,   // Tooltip appears to the left
        RIGHT,  // Tooltip appears to the right
        CENTER  // Tooltip appears in the center of the screen (no anchor)
    }

    public TooltipConfig(View anchorView, String message, TooltipPosition position, boolean highlightAnchor) {
        this.anchorView = anchorView;
        this.message = message;
        this.position = position;
        this.highlightAnchor = highlightAnchor;
    }

    public View getAnchorView() {
        return anchorView;
    }

    public String getMessage() {
        return message;
    }

    public TooltipPosition getPosition() {
        return position;
    }

    public boolean shouldHighlightAnchor() {
        return highlightAnchor;
    }
}
