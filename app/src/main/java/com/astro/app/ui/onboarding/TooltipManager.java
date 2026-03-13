package com.astro.app.ui.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.astro.app.R;

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

        // Dismiss previous tooltip
        dismiss();

        TooltipConfig config = tooltips.get(index);

        // Execute onShowAction if present
        Runnable onShowAction = config.getOnShowAction();
        if (onShowAction != null) {
            onShowAction.run();
        }

        // Create tooltip view
        currentTooltipView = new TooltipView(activity);
        currentTooltipView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Add message text
        TextView messageText = new TextView(activity);
        messageText.setText(config.getMessage());
        messageText.setTextColor(activity.getResources().getColor(R.color.text_on_light));
        messageText.setTextSize(16);  // 16sp
        messageText.setPadding(
            dpToPx(16),
            dpToPx(16),
            dpToPx(16),
            dpToPx(48)  // Extra bottom padding for buttons
        );

        // Button container
        FrameLayout buttonContainer = new FrameLayout(activity);
        FrameLayout.LayoutParams buttonContainerParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonContainerParams.gravity = Gravity.BOTTOM;

        // Next button
        Button btnNext = new Button(activity);
        btnNext.setText(index == tooltips.size() - 1 ? "Got it" : "Next");
        btnNext.setOnClickListener(v -> showNext());
        FrameLayout.LayoutParams nextParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        nextParams.gravity = Gravity.END | Gravity.BOTTOM;
        nextParams.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        // Skip button
        Button btnSkip = new Button(activity);
        btnSkip.setText("Skip");
        btnSkip.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams skipParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        skipParams.gravity = Gravity.START | Gravity.BOTTOM;
        skipParams.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        buttonContainer.addView(btnSkip, skipParams);
        buttonContainer.addView(btnNext, nextParams);

        // Calculate tooltip bubble position
        RectF bubbleRect = calculateBubbleRect(config);
        RectF anchorRect = config.shouldHighlightAnchor() && config.getAnchorView() != null
            ? getViewRect(config.getAnchorView())
            : null;

        // Compute extra highlight rects
        List<RectF> extraHighlightRects = null;
        List<View> extraViews = config.getExtraHighlightViews();
        if (extraViews != null && !extraViews.isEmpty()) {
            extraHighlightRects = new ArrayList<>();
            for (View v : extraViews) {
                if (v != null && v.getVisibility() == View.VISIBLE) {
                    extraHighlightRects.add(getViewRect(v));
                }
            }
        }

        currentTooltipView.configure(bubbleRect, anchorRect, config.getPosition(),
            extraHighlightRects, config.isInteractive(), config.getAnchorView());

        // Position message text inside bubble
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            (int) bubbleRect.width(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.leftMargin = (int) bubbleRect.left;
        textParams.topMargin = (int) bubbleRect.top;

        currentTooltipView.addView(messageText, textParams);

        // Position buttons inside the bubble (not at screen bottom)
        FrameLayout.LayoutParams bubbleButtonParams = new FrameLayout.LayoutParams(
            (int) bubbleRect.width(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bubbleButtonParams.leftMargin = (int) bubbleRect.left;
        bubbleButtonParams.topMargin = (int) bubbleRect.bottom - dpToPx(48);
        currentTooltipView.addView(buttonContainer, bubbleButtonParams);

        // Add to root view (custom or activity default)
        ViewGroup target = rootView != null ? rootView
            : (ViewGroup) activity.findViewById(android.R.id.content);
        target.addView(currentTooltipView);
    }

    private RectF calculateBubbleRect(TooltipConfig config) {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        float bubbleWidth = dpToPx(280);
        float bubbleHeight = dpToPx(140);
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

    private void dismiss() {
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
