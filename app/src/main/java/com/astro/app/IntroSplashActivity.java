package com.astro.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import com.astro.app.ui.skymap.SkyMapActivity;

public class IntroSplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent(this, SkyMapActivity.class);
        startActivity(i);
        overridePendingTransition(0, 0);
        finish();
    }
}
