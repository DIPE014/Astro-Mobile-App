package com.astro.app.ui.onboarding;

import android.view.View;

import java.util.List;

/**
 * Configuration for a single tooltip in the interactive tutorial.
 */
public class TooltipConfig {
    private final View anchorView;
    private final String message;
    private final TooltipPosition position;
    private final boolean highlightAnchor;
    private final boolean interactive;
    private final Runnable onShowAction;
    private final List<View> extraHighlightViews;

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
        this.interactive = false;
        this.onShowAction = null;
        this.extraHighlightViews = null;
    }

    private TooltipConfig(Builder builder) {
        this.anchorView = builder.anchorView;
        this.message = builder.message;
        this.position = builder.position;
        this.highlightAnchor = builder.highlightAnchor;
        this.interactive = builder.interactive;
        this.onShowAction = builder.onShowAction;
        this.extraHighlightViews = builder.extraHighlightViews;
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

    public boolean isInteractive() {
        return interactive;
    }

    public Runnable getOnShowAction() {
        return onShowAction;
    }

    public List<View> getExtraHighlightViews() {
        return extraHighlightViews;
    }

    public static class Builder {
        private View anchorView;
        private String message;
        private TooltipPosition position = TooltipPosition.CENTER;
        private boolean highlightAnchor = false;
        private boolean interactive = false;
        private Runnable onShowAction = null;
        private List<View> extraHighlightViews = null;

        public Builder(String message) {
            this.message = message;
        }

        public Builder anchorView(View anchorView) {
            this.anchorView = anchorView;
            return this;
        }

        public Builder position(TooltipPosition position) {
            this.position = position;
            return this;
        }

        public Builder highlightAnchor(boolean highlightAnchor) {
            this.highlightAnchor = highlightAnchor;
            return this;
        }

        public Builder interactive(boolean interactive) {
            this.interactive = interactive;
            return this;
        }

        public Builder onShowAction(Runnable onShowAction) {
            this.onShowAction = onShowAction;
            return this;
        }

        public Builder extraHighlightViews(List<View> extraHighlightViews) {
            this.extraHighlightViews = extraHighlightViews;
            return this;
        }

        public TooltipConfig build() {
            return new TooltipConfig(this);
        }
    }
}
