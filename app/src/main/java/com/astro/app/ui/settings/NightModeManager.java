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
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();

        // Remove any existing overlay from this activity to avoid duplicates
        View existing = rootView.findViewWithTag("night_mode_overlay");
        if (existing != null) {
            rootView.removeView(existing);
        }

        if (isNightMode) {
            // Create a fresh view scoped to this activity to avoid context leaks
            View overlay = new View(activity);
            overlay.setTag("night_mode_overlay");
            overlay.setBackgroundColor(0x20800000); // Semi-transparent red
            overlay.setClickable(false);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            rootView.addView(overlay, params);
        }
    }
}
