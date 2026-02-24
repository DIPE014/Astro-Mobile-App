package com.astro.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import com.astro.app.ui.skymap.SkyMapActivity;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class IntroSplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_splash);

        ImageView gifBackground = findViewById(R.id.introScreen);
        ConstraintLayout centerContent = findViewById(R.id.centerContent);

        Animation titleZoom = AnimationUtils.loadAnimation(this, R.anim.title_zoom_exit);

        Glide.with(this)
                .asGif()
                .load(R.drawable.intro_star) // your gif filename here
                .into(gifBackground);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                centerContent.startAnimation(titleZoom);

                // 2. Wait a split second for the zoom to start, then switch screens
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(IntroSplashActivity.this, SkyMapActivity.class);
                        startActivity(i);
                        // Keep the smooth fade between activities
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                }, 800); // Wait 800ms after zoom starts to switch
            }
        }, 2000); // Start the whole sequence after 2 seconds
    }
}