package com.astro.app.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class NightModeManager {
    private static NightModeManager instance;
    private boolean isNightMode = false;
    private View redTintView;

    private NightModeManager(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        isNightMode = prefs.getBoolean("night_mode", false);
    }

    public static NightModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new NightModeManager(context);
        }
        return instance;
    }

    public boolean isNightMode() {
        return isNightMode;
    }

    public void toggleNightMode(Activity activity) {
        isNightMode = !isNightMode;

        // Save preference
        SharedPreferences prefs = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("night_mode", isNightMode).apply();

        // Apply to current activity
        applyToActivity(activity);
    }

    public void applyToActivity(Activity activity) {
        // Get root view
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();

        if (isNightMode) {
            // Add red tint overlay
            if (redTintView == null) {
                redTintView = new View(activity);
                redTintView.setBackgroundColor(0x20800000); // Semi-transparent red
                redTintView.setClickable(false); // Allow clicks to pass through
            }

            // Remove if already attached elsewhere
            if (redTintView.getParent() != null) {
                ((ViewGroup) redTintView.getParent()).removeView(redTintView);
            }

            // Add to current activity
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            rootView.addView(redTintView, params);

        } else {
            // Remove red tint
            if (redTintView != null && redTintView.getParent() != null) {
                ((ViewGroup) redTintView.getParent()).removeView(redTintView);
            }
        }
    }
}
