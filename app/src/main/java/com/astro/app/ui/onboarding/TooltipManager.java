package com.astro.app.ui.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.astro.app.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the in-app tooltip tutorial sequence.
 *
 * Features:
 * - Shows tooltips one at a time in sequence
 * - Highlights anchor views with circular punch-through
 * - Next/Skip buttons for navigation
 * - Saves completion state to SharedPreferences
 * - Per-tutorial preference keys for multiple screens
 * - Interactive mode for toggle buttons
 * - Extra highlight views for grouped controls
 */
public class TooltipManager {
    private static final String TAG = "TooltipManager";
    private static final String PREFS_NAME = "onboarding";

    // Per-tutorial keys
    public static final String KEY_SKYMAP_TUTORIAL = "tooltip_tutorial_completed"; // backward compat
    public static final String KEY_SETTINGS_TUTORIAL = "settings_tooltip_completed";
    public static final String KEY_SEARCH_TUTORIAL = "search_tooltip_completed";
    public static final String KEY_CHAT_TUTORIAL = "chat_tooltip_completed";
    public static final String KEY_DETECT_TUTORIAL = "detect_tooltip_completed";

    private final Activity activity;
    private final String tutorialKey;
    private final ViewGroup rootView;
    private final List<TooltipConfig> tooltips;
    private int currentTooltipIndex = 0;

    private TooltipView currentTooltipView;
    private OnTutorialCompleteListener completeListener;

    public interface OnTutorialCompleteListener {
        void onTutorialComplete();
    }

    public TooltipManager(Activity activity) {
        this(activity, KEY_SKYMAP_TUTORIAL);
    }

    public TooltipManager(Activity activity, String tutorialKey) {
        this(activity, tutorialKey, null);
    }

    public TooltipManager(Activity activity, String tutorialKey, ViewGroup rootView) {
        this.activity = activity;
        this.tutorialKey = tutorialKey;
        this.rootView = rootView;
        this.tooltips = new ArrayList<>();
    }

    /**
     * Add a tooltip to the sequence.
     */
    public TooltipManager addTooltip(TooltipConfig config) {
        tooltips.add(config);
        return this;
    }

    /**
     * Set the tutorial completion listener.
     */
    public TooltipManager setOnCompleteListener(OnTutorialCompleteListener listener) {
        this.completeListener = listener;
        return this;
    }

    /**
     * Check if the user has already completed the tutorial (default skymap key).
     */
    public static boolean hasCompletedTutorial(Context context) {
        return hasCompletedTutorial(context, KEY_SKYMAP_TUTORIAL);
    }

    /**
     * Check if the user has already completed a specific tutorial.
     */
    public static boolean hasCompletedTutorial(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    /**
     * Mark the tutorial as completed.
     */
    private void markTutorialCompleted() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(tutorialKey, true).apply();
    }

    /**
     * Reset tutorial completion state (default skymap key).
     */
    public static void resetTutorial(Context context) {
        resetTutorial(context, KEY_SKYMAP_TUTORIAL);
    }

    /**
     * Reset a specific tutorial's completion state.
     */
    public static void resetTutorial(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, false).apply();
    }

    /**
     * Reset all tutorial completion states.
     */
    public static void resetAllTutorials(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_SKYMAP_TUTORIAL, false)
            .putBoolean(KEY_SETTINGS_TUTORIAL, false)
            .putBoolean(KEY_SEARCH_TUTORIAL, false)
            .putBoolean(KEY_CHAT_TUTORIAL, false)
            .putBoolean(KEY_DETECT_TUTORIAL, false)
            .apply();
    }

    /**
     * Start the tutorial sequence.
     */
    public void start() {
        if (tooltips.isEmpty()) {
            // No tooltips added (views not ready?) — don't mark as completed
            return;
        }

        currentTooltipIndex = 0;
        showTooltip(currentTooltipIndex);
    }

    private void showTooltip(int index) {
        if (index >= tooltips.size()) {
            finish();
            return;
        }

        TooltipConfig config = tooltips.get(index);

        // Fade out previous tooltip before showing next
        if (currentTooltipView != null && currentTooltipView.getParent() != null) {
            final TooltipView oldView = currentTooltipView;
            currentTooltipView = null;
            oldView.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                if (oldView.getParent() != null) {
                    ((ViewGroup) oldView.getParent()).removeView(oldView);
                }
            }).start();
        }

        // Execute onShowAction if present (e.g., expand FAB menu)
        Runnable onShowAction = config.getOnShowAction();
        boolean needsAnimationDelay = onShowAction != null
            && config.getExtraHighlightViews() != null
            && !config.getExtraHighlightViews().isEmpty();

        if (onShowAction != null) {
            onShowAction.run();
        }

        // If onShowAction triggers an animation that moves extra highlight views,
        // delay tooltip layout to let the animation settle (e.g., MotionLayout ~500ms)
        if (needsAnimationDelay) {
            ViewGroup target = rootView != null ? rootView
                : (ViewGroup) activity.findViewById(android.R.id.content);
            target.postDelayed(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                buildAndShowTooltipView(index, config);
            }, 600);
        } else {
            buildAndShowTooltipView(index, config);
        }
    }

    private void buildAndShowTooltipView(int index, TooltipConfig config) {
        // Guard against activity finishing during delay
        if (activity.isFinishing() || activity.isDestroyed()) return;

        // Scroll anchor into view if it's inside a ScrollView/NestedScrollView
        View anchor = config.getAnchorView();
        if (anchor != null) {
            scrollAnchorIntoView(anchor);
            // Post to let the scroll settle before measuring positions
            anchor.post(() -> buildTooltipAfterScroll(index, config));
        } else {
            buildTooltipAfterScroll(index, config);
        }
    }

    private void scrollAnchorIntoView(View anchor) {
        // Try requestRectangleOnScreen first
        anchor.requestRectangleOnScreen(
            new Rect(0, 0, anchor.getWidth(), anchor.getHeight()), false);

        // Also manually scroll parent ScrollView/NestedScrollView as fallback.
        // Use screen coordinates to handle nested containers correctly —
        // anchor.getTop() is relative to immediate parent, not the ScrollView.
        View parent = (View) anchor.getParent();
        while (parent != null) {
            if (parent instanceof NestedScrollView) {
                NestedScrollView scrollView = (NestedScrollView) parent;
                int[] anchorLoc = new int[2];
                int[] scrollLoc = new int[2];
                anchor.getLocationOnScreen(anchorLoc);
                scrollView.getLocationOnScreen(scrollLoc);
                int relativeTop = anchorLoc[1] - scrollLoc[1] + scrollView.getScrollY();
                int scrollTarget = relativeTop - scrollView.getHeight() / 2 + anchor.getHeight() / 2;
                scrollView.smoothScrollTo(0, Math.max(0, scrollTarget));
                break;
            } else if (parent instanceof ScrollView) {
                ScrollView scrollView = (ScrollView) parent;
                int[] anchorLoc = new int[2];
                int[] scrollLoc = new int[2];
                anchor.getLocationOnScreen(anchorLoc);
                scrollView.getLocationOnScreen(scrollLoc);
                int relativeTop = anchorLoc[1] - scrollLoc[1] + scrollView.getScrollY();
                int scrollTarget = relativeTop - scrollView.getHeight() / 2 + anchor.getHeight() / 2;
                scrollView.smoothScrollTo(0, Math.max(0, scrollTarget));
                break;
            }
            if (parent.getParent() instanceof View) {
                parent = (View) parent.getParent();
            } else {
                break;
            }
        }
    }

    private void buildTooltipAfterScroll(int index, TooltipConfig config) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        // Create tooltip view
        currentTooltipView = new TooltipView(activity);
        currentTooltipView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Measure message text to compute dynamic bubble height
        int bubbleWidth = dpToPx(280);
        int textPaddingH = dpToPx(16);
        int textMaxWidth = bubbleWidth - 2 * textPaddingH;

        TextView messageText = new TextView(activity);
        messageText.setText(config.getMessage());
        messageText.setTextColor(activity.getResources().getColor(R.color.text_on_light));
        messageText.setTextSize(16);

        // Measure text at the constrained width
        int widthSpec = View.MeasureSpec.makeMeasureSpec(textMaxWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        messageText.measure(widthSpec, heightSpec);
        int measuredTextHeight = messageText.getMeasuredHeight();

        // Dynamic bubble height: top padding + text + spacing + step indicator + buttons + bottom padding
        int topPadding = dpToPx(12);
        int stepIndicatorHeight = dpToPx(18); // 12sp text + margins
        int spacing = dpToPx(8);
        int buttonHeight = dpToPx(40);
        int bottomPadding = dpToPx(8);
        float dynamicBubbleHeight = topPadding + stepIndicatorHeight + spacing
            + measuredTextHeight + spacing + buttonHeight + bottomPadding;

        // Set text padding (no extra bottom — buttons are positioned separately)
        messageText.setPadding(textPaddingH, 0, textPaddingH, 0);

        // Step indicator text
        TextView stepIndicator = new TextView(activity);
        stepIndicator.setText((index + 1) + " of " + tooltips.size());
        stepIndicator.setTextSize(12);
        stepIndicator.setTextColor(activity.getResources().getColor(R.color.text_secondary));

        // Button container
        FrameLayout buttonContainer = new FrameLayout(activity);

        // Next button — Material filled style
        MaterialButton btnNext = new MaterialButton(activity, null,
            com.google.android.material.R.attr.materialButtonStyle);
        btnNext.setText(index == tooltips.size() - 1 ? "Got it" : "Next");
        btnNext.setOnClickListener(v -> showNext());
        btnNext.setAllCaps(false);
        btnNext.setTextSize(14);
        btnNext.setCornerRadius(dpToPx(8));
        btnNext.setMinHeight(0);
        btnNext.setMinimumHeight(0);
        btnNext.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        FrameLayout.LayoutParams nextParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        nextParams.gravity = Gravity.END | Gravity.BOTTOM;
        nextParams.setMargins(dpToPx(4), 0, dpToPx(8), 0);

        // Skip button — text-only style
        MaterialButton btnSkip = new MaterialButton(activity, null,
            com.google.android.material.R.attr.borderlessButtonStyle);
        btnSkip.setText("Skip");
        btnSkip.setOnClickListener(v -> finish());
        btnSkip.setAllCaps(false);
        btnSkip.setTextSize(14);
        btnSkip.setMinHeight(0);
        btnSkip.setMinimumHeight(0);
        btnSkip.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        btnSkip.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams skipParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        skipParams.gravity = Gravity.START | Gravity.BOTTOM;
        skipParams.setMargins(dpToPx(8), 0, dpToPx(4), 0);

        buttonContainer.addView(btnSkip, skipParams);
        buttonContainer.addView(btnNext, nextParams);

        // Calculate tooltip bubble position with dynamic height
        RectF bubbleRect = calculateBubbleRect(config, dynamicBubbleHeight);
        RectF anchorRect = config.shouldHighlightAnchor() && config.getAnchorView() != null
            ? getViewRect(config.getAnchorView())
            : null;

        // Compute extra highlight rects and collect view references for interactive passthrough
        List<RectF> extraHighlightRects = null;
        List<View> visibleExtraViews = null;
        List<View> extraViews = config.getExtraHighlightViews();
        if (extraViews != null && !extraViews.isEmpty()) {
            extraHighlightRects = new ArrayList<>();
            visibleExtraViews = new ArrayList<>();
            for (View v : extraViews) {
                if (v != null && v.getVisibility() == View.VISIBLE) {
                    extraHighlightRects.add(getViewRect(v));
                    visibleExtraViews.add(v);
                }
            }
        }

        currentTooltipView.configure(bubbleRect, anchorRect, config.getPosition(),
            extraHighlightRects, visibleExtraViews,
            config.isInteractive(), config.getAnchorView());

        // Position step indicator at top-right of bubble
        FrameLayout.LayoutParams stepParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        stepParams.leftMargin = (int) bubbleRect.left + (int) bubbleRect.width() - dpToPx(60);
        stepParams.topMargin = (int) bubbleRect.top + dpToPx(8);
        currentTooltipView.addView(stepIndicator, stepParams);

        // Position message text inside bubble (below step indicator)
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            (int) bubbleRect.width(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.leftMargin = (int) bubbleRect.left;
        textParams.topMargin = (int) bubbleRect.top + topPadding + stepIndicatorHeight + spacing;
        currentTooltipView.addView(messageText, textParams);

        // Position buttons at bottom of bubble
        FrameLayout.LayoutParams bubbleButtonParams = new FrameLayout.LayoutParams(
            (int) bubbleRect.width(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bubbleButtonParams.leftMargin = (int) bubbleRect.left;
        bubbleButtonParams.topMargin = (int) bubbleRect.bottom - buttonHeight - bottomPadding;
        currentTooltipView.addView(buttonContainer, bubbleButtonParams);

        // Start invisible for fade-in
        currentTooltipView.setAlpha(0f);

        // Add to root view (custom or activity default)
        ViewGroup target = rootView != null ? rootView
            : (ViewGroup) activity.findViewById(android.R.id.content);
        target.addView(currentTooltipView);

        // Fade in
        currentTooltipView.animate().alpha(1f).setDuration(200).start();
    }

    private RectF calculateBubbleRect(TooltipConfig config, float bubbleHeight) {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        float bubbleWidth = dpToPx(280);
        float margin = dpToPx(16);

        if (config.getPosition() == TooltipConfig.TooltipPosition.CENTER || config.getAnchorView() == null) {
            // Center of screen
            float left = (screenWidth - bubbleWidth) / 2f;
            float top = (screenHeight - bubbleHeight) / 2f;
            return new RectF(left, top, left + bubbleWidth, top + bubbleHeight);
        }

        RectF anchorRect = getViewRect(config.getAnchorView());

        switch (config.getPosition()) {
            case ABOVE:
                // Position above anchor
                float aboveLeft = Math.max(margin, anchorRect.centerX() - bubbleWidth / 2f);
                aboveLeft = Math.min(aboveLeft, screenWidth - bubbleWidth - margin);
                float aboveTop = anchorRect.top - bubbleHeight - dpToPx(30);
                return new RectF(aboveLeft, aboveTop, aboveLeft + bubbleWidth, aboveTop + bubbleHeight);

            case BELOW:
                // Position below anchor
                float belowLeft = Math.max(margin, anchorRect.centerX() - bubbleWidth / 2f);
                belowLeft = Math.min(belowLeft, screenWidth - bubbleWidth - margin);
                float belowTop = anchorRect.bottom + dpToPx(30);
                return new RectF(belowLeft, belowTop, belowLeft + bubbleWidth, belowTop + bubbleHeight);

            case LEFT:
                // Position to the left of anchor
                float leftLeft = anchorRect.left - bubbleWidth - dpToPx(30);
                float leftTop = Math.max(margin, anchorRect.centerY() - bubbleHeight / 2f);
                return new RectF(leftLeft, leftTop, leftLeft + bubbleWidth, leftTop + bubbleHeight);

            case RIGHT:
                // Position to the right of anchor
                float rightLeft = anchorRect.right + dpToPx(30);
                float rightTop = Math.max(margin, anchorRect.centerY() - bubbleHeight / 2f);
                return new RectF(rightLeft, rightTop, rightLeft + bubbleWidth, rightTop + bubbleHeight);

            default:
                // Fallback to center
                float left = (screenWidth - bubbleWidth) / 2f;
                float top = (screenHeight - bubbleHeight) / 2f;
                return new RectF(left, top, left + bubbleWidth, top + bubbleHeight);
        }
    }

    private RectF getViewRect(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new RectF(
            location[0],
            location[1],
            location[0] + view.getWidth(),
            location[1] + view.getHeight()
        );
    }

    private int dpToPx(int dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density);
    }

    private void showNext() {
        currentTooltipIndex++;
        showTooltip(currentTooltipIndex);
    }

    /**
     * Dismiss the current tooltip and clean up. Call from onDestroy/onDestroyView
     * to prevent leaks if the tutorial is interrupted.
     */
    public void dismiss() {
        if (currentTooltipView != null && currentTooltipView.getParent() != null) {
            ((ViewGroup) currentTooltipView.getParent()).removeView(currentTooltipView);
            currentTooltipView = null;
        }
    }

    private void finish() {
        dismiss();
        markTutorialCompleted();
        if (completeListener != null) {
            completeListener.onTutorialComplete();
        }
    }
}
