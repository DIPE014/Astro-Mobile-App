package com.astro.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.astro.app.ui.skymap.SkyMapActivity;

/**
 * Intro splash screen.
 *
 * Scene: full-screen animated star field (StarFieldView) with twinkling
 * stars, shooting meteors, and nebula glows.  The scene plays until the
 * user taps "EXPLORE THE UNIVERSE".
 *
 * On tap:
 *   1. UI content fades out (400 ms)
 *   2. Warp mode activates — stars fly radially outward, accelerating
 *   3. Black overlay fades in (600 ms, starting at t = 1 200 ms)
 *   4. SkyMapActivity launches with no transition (seamless handoff)
 */
public class IntroSplashActivity extends AppCompatActivity {

    // Total warp duration before the black fade starts
    private static final long WARP_DURATION_MS  = 1200;
    // Duration of the black-overlay fade
    private static final long BLACK_FADE_MS      = 600;

    private final Handler splashHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Full-screen immersive + keep screen on for the splash
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_intro_splash);

        StarFieldView starField   = findViewById(R.id.starFieldView);
        View          centerContent = findViewById(R.id.centerContent);
        View          btnStart    = findViewById(R.id.btnStart);
        View          blackOverlay = findViewById(R.id.blackOverlay);

        // ── Entrance animation ────────────────────────────────────────────
        // Center content fades in gently
        splashHandler.postDelayed(() ->
                centerContent.animate()
                        .alpha(1f)
                        .setDuration(1400)
                        .setInterpolator(new DecelerateInterpolator())
                        .start(),
                300);

        // Button slides up from below and fades in
        btnStart.setTranslationY(50f);
        splashHandler.postDelayed(() ->
                btnStart.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(900)
                        .setInterpolator(new DecelerateInterpolator(1.5f))
                        .start(),
                1100);

        // ── Start button: warp → SkyMapActivity ──────────────────────────
        btnStart.setOnClickListener(v -> {
            btnStart.setEnabled(false);

            // 1. Fade out UI
            centerContent.animate().alpha(0f).setDuration(400).start();
            btnStart.animate().alpha(0f).setDuration(300).start();

            // 2. Engage warp — StarFieldView flies stars outward
            starField.startWarp();

            // 3. After warp builds up, fade to black then launch
            splashHandler.postDelayed(() ->
                    blackOverlay.animate()
                            .alpha(1f)
                            .setDuration(BLACK_FADE_MS)
                            .setInterpolator(new AccelerateInterpolator())
                            .withEndAction(this::launchSkyMap)
                            .start(),
                    WARP_DURATION_MS);
        });
    }

    private void launchSkyMap() {
        startActivity(new Intent(this, SkyMapActivity.class));
        overridePendingTransition(0, 0); // no default transition; we already faded to black
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        splashHandler.removeCallbacksAndMessages(null);
    }
}
