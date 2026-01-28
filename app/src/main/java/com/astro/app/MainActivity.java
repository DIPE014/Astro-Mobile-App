package com.astro.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.astro.app.databinding.ActivityMainBinding;
import com.astro.app.ui.settings.SettingsActivity;
import com.astro.app.ui.settings.SettingsViewModel;
import com.astro.app.ui.skymap.SkyMapActivity;

/**
 * Main entry point of the Astro app.
 *
 * <p>This activity serves as the launcher and provides navigation to:
 * <ul>
 *   <li>Sky Map - The main AR star viewing feature</li>
 *   <li>Settings - Configure app preferences</li>
 * </ul>
 * </p>
 *
 * <p>Uses ViewBinding for type-safe view access and observes SettingsViewModel
 * to apply night mode when returning from settings.</p>
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SettingsViewModel settingsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ViewModel for settings (shared across app)
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupUI();
        observeSettings();
    }

    /**
     * Sets up UI click listeners and initial state.
     */
    private void setupUI() {
        // Navigate to Sky Map
        binding.btnStartSkyMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, SkyMapActivity.class);
            startActivity(intent);
        });

        // Navigate to Settings (if settings button exists)
        View settingsButton = findViewById(R.id.btnSettings);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    /**
     * Observes settings changes and applies them to the UI.
     */
    private void observeSettings() {
        // Observe night mode setting
        settingsViewModel.getNightMode().observe(this, nightMode -> {
            if (nightMode != null && nightMode) {
                applyNightMode();
            } else {
                applyDayMode();
            }
        });
    }

    /**
     * Applies night mode (red filter) to preserve night vision.
     */
    private void applyNightMode() {
        View rootView = binding.getRoot();
        // Apply red tint overlay for night mode
        rootView.setBackgroundColor(0xFF1A0000); // Dark red background
    }

    /**
     * Applies normal day mode appearance.
     */
    private void applyDayMode() {
        View rootView = binding.getRoot();
        // Reset to normal background
        rootView.setBackgroundColor(0xFF000000); // Black background
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply current settings when returning from other activities
        Boolean nightMode = settingsViewModel.getNightMode().getValue();
        if (nightMode != null && nightMode) {
            applyNightMode();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
