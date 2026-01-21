package com.astro.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.astro.app.ui.skymap.SkyMapActivity;

/**
 * Main entry point - launches SkyMapActivity.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Navigate to sky map
        findViewById(R.id.btnStartSkyMap).setOnClickListener(v -> {
            Intent intent = new Intent(this, SkyMapActivity.class);
            startActivity(intent);
        });
    }
}
