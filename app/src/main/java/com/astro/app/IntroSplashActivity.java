package com.astro.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.astro.app.ui.intro.StarFieldView;
import com.astro.app.ui.skymap.SkyMapActivity;

public class IntroSplashActivity extends AppCompatActivity {
    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private StarFieldView starFieldView;
    private boolean warpStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_splash);

        starFieldView = findViewById(R.id.starFieldView);
        ConstraintLayout centerContent = findViewById(R.id.centerContent);
        View rootView = findViewById(R.id.rootSplash);
        TextView tvTapHint = findViewById(R.id.tvTapHint);

        // Fade in the "Tap to explore" hint after 1.5s
        splashHandler.postDelayed(() -> {
            Animation fadeInPulse = AnimationUtils.loadAnimation(this, R.anim.fade_in_pulse);
            tvTapHint.setAlpha(1f);
            tvTapHint.startAnimation(fadeInPulse);
        }, 1500);

        // Tap anywhere to start warp transition
        rootView.setOnClickListener(v -> {
            if (warpStarted) return;
            warpStarted = true;

            // Phase 1: Zoom out center content + fade hint
            centerContent.animate()
                    .scaleX(3f).scaleY(3f)
                    .alpha(0f)
                    .setDuration(500)
                    .start();

            tvTapHint.clearAnimation();
            tvTapHint.animate().alpha(0f).setDuration(300).start();

            // Phase 2: Start warp tunnel
            starFieldView.setOnWarpCompleteListener(() -> {
                if (isFinishing() || isDestroyed()) return;
                // Phase 3: Transition to SkyMapActivity with no system animation
                Intent i = new Intent(IntroSplashActivity.this, SkyMapActivity.class);
                i.putExtra("from_splash", true);
                startActivity(i);
                overridePendingTransition(0, 0);  // suppress system fade for seamless reveal
                finish();
            });

            splashHandler.postDelayed(() -> starFieldView.startWarpMode(), 300);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        splashHandler.removeCallbacksAndMessages(null);
    }
}
