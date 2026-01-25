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

    /**
     * Initializes the activity by inflating the view binding, setting the content view,
     * obtaining the shared SettingsViewModel, and configuring UI actions and observers.
     *
     * @param savedInstanceState bundle containing saved instance state, or null if none
     */
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
     * Configures UI actions for the main screen.
     *
     * Attaches a click listener to the bound start button to launch SkyMapActivity and, if a view
     * with ID R.id.btnSettings exists, attaches a click listener to launch SettingsActivity.
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
     * Switches the activity UI to a night-optimized red theme to preserve night vision.
     *
     * Sets the activity's root view background to a dark red tint (ARGB 0xFF1A0000).
     */
    private void applyNightMode() {
        View rootView = binding.getRoot();
        // Apply red tint overlay for night mode
        rootView.setBackgroundColor(0xFF1A0000); // Dark red background
    }

    /**
     * Set the activity UI to day mode.
     *
     * Changes the root view background color to solid black.
     */
    private void applyDayMode() {
        View rootView = binding.getRoot();
        // Reset to normal background
        rootView.setBackgroundColor(0xFF000000); // Black background
    }

    /**
     * Re-applies the current night mode setting when the activity resumes.
     *
     * If the stored night mode value is `true`, applies night mode; otherwise leaves the UI state unchanged.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply current settings when returning from other activities
        Boolean nightMode = settingsViewModel.getNightMode().getValue();
        if (nightMode != null && nightMode) {
            applyNightMode();
        }
    }

    /**
     * Releases the activity's view binding when the activity is destroyed.
     *
     * Clears the binding reference so the view hierarchy can be garbage-collected and avoid leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}