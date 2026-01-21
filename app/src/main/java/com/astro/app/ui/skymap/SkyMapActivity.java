package com.astro.app.ui.skymap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.astro.app.R;

/**
 * FRONTEND - Person A
 *
 * Main AR sky view activity.
 * Shows camera preview with star overlay.
 */
public class SkyMapActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    // TODO: Add reference to SkyDataProvider (from Backend)
    // private SkyDataProvider skyDataProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sky_map);

        if (hasPermissions()) {
            initializeSkyMap();
        } else {
            requestPermissions();
        }
    }

    private boolean hasPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasPermissions()) {
                initializeSkyMap();
            } else {
                // TODO: Show message that permissions are required
                finish();
            }
        }
    }

    private void initializeSkyMap() {
        // TODO: Initialize camera preview
        // TODO: Initialize star overlay renderer
        // TODO: Connect to SkyDataProvider (Backend)
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: Start sensor updates
        // skyDataProvider.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO: Stop sensor updates to save battery
        // skyDataProvider.stop();
    }
}
