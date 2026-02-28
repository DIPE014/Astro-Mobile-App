package com.astro.app.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.astro.app.R;
import com.astro.app.ui.onboarding.OnboardingActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Activity for managing application settings.
 *
 * <p>This activity provides controls for:
 * <ul>
 *   <li>Display settings (star brightness, magnitude limit, night mode)</li>
 *   <li>Layer visibility (stars, constellations, grid)</li>
 *   <li>Sensor settings (smoothing, auto-rotate)</li>
 *   <li>Reset to defaults</li>
 * </ul>
 * </p>
 *
 * <p>Uses SettingsViewModel for state management and automatic persistence
 * via SharedPreferences. All changes are immediately saved and applied.</p>
 */
public class SettingsActivity extends AppCompatActivity {

    private SettingsViewModel viewModel;

    // Display settings views
    private Slider sliderBrightness;
    private TextView tvBrightnessValue;
    private Slider sliderMagnitude;
    private TextView tvMagnitudeValue;
    private MaterialSwitch switchNightMode;

    // Layer settings views
    private MaterialSwitch switchStarLabels;
    private MaterialSwitch switchConstellationLines;
    private MaterialSwitch switchConstellationNames;
    private MaterialSwitch switchManualScroll;

    // Prevent recursive updates
    private boolean isUpdatingUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        initializeViews();
        setupClickListeners();
        observeViewModel();
    }

    /**
     * Initializes view references.
     */
    private void initializeViews() {
        // Display settings
        sliderBrightness = findViewById(R.id.sliderBrightness);
        tvBrightnessValue = findViewById(R.id.tvBrightnessValue);
        sliderMagnitude = findViewById(R.id.sliderMagnitude);
        tvMagnitudeValue = findViewById(R.id.tvMagnitudeValue);
        switchNightMode = findViewById(R.id.switchNightMode);

        // Layer settings
        switchStarLabels = findViewById(R.id.switchStarLabels);
        switchConstellationLines = findViewById(R.id.switchConstellationLines);
        switchConstellationNames = findViewById(R.id.switchConstellationNames);
        switchManualScroll = findViewById(R.id.switchManualScroll);

        // Configure Slider ranges (Sliders are configured via XML, but we can adjust programmatically if needed)
        if (sliderBrightness != null) {
            sliderBrightness.setValueFrom(0f);
            sliderBrightness.setValueTo(100f);
            sliderBrightness.setStepSize(5f);
        }
        if (sliderMagnitude != null) {
            sliderMagnitude.setValueFrom(1f);
            sliderMagnitude.setValueTo(8f);
            sliderMagnitude.setStepSize(0.5f);
        }
    }

    /**
     * Sets up click and change listeners for UI elements.
     */
    private void setupClickListeners() {
        // Setup toolbar navigation
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Brightness Slider
        if (sliderBrightness != null) {
            sliderBrightness.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser && !isUpdatingUI) {
                    float brightness = 0.5f + (value / 100f);
                    viewModel.setStarBrightness(brightness);
                }
            });
        }

        // Magnitude Slider
        if (sliderMagnitude != null) {
            sliderMagnitude.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser && !isUpdatingUI) {
                    viewModel.setMagnitudeLimit(value);
                }
            });
        }

        // Night Mode Switch
        if (switchNightMode != null) {
            switchNightMode.setOnCheckedChangeListener((button, isChecked) -> {
                if (!isUpdatingUI) {
                    viewModel.setNightMode(isChecked);
                }
            });
        }

        // Star Labels Switch
        if (switchStarLabels != null) {
            switchStarLabels.setOnCheckedChangeListener((button, isChecked) -> {
                if (!isUpdatingUI) {
                    viewModel.setShowStarLabels(isChecked);
                }
            });
        }

        // Constellation Lines Switch
        if (switchConstellationLines != null) {
            switchConstellationLines.setOnCheckedChangeListener((button, isChecked) -> {
                if (!isUpdatingUI) {
                    viewModel.setShowConstellationLines(isChecked);
                }
            });
        }

        // Constellation Names Switch
        if (switchConstellationNames != null) {
            switchConstellationNames.setOnCheckedChangeListener((button, isChecked) -> {
                if (!isUpdatingUI) {
                    viewModel.setShowConstellationNames(isChecked);
                }
            });
        }

        // Manual Scroll Switch
        if (switchManualScroll != null) {
            switchManualScroll.setOnCheckedChangeListener((button, isChecked) -> {
                if (!isUpdatingUI) {
                    viewModel.setEnableManualScroll(isChecked);
                }
            });
        }

        // Replay Tutorial Button
        MaterialButton btnReplayTutorial = findViewById(R.id.btnReplayTutorial);
        if (btnReplayTutorial != null) {
            btnReplayTutorial.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences(
                        SettingsViewModel.PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean(
                        OnboardingActivity.KEY_HAS_COMPLETED_ONBOARDING, false).apply();
                Toast.makeText(this, R.string.settings_replay_tutorial_toast,
                        Toast.LENGTH_SHORT).show();
            });
        }

        // API Key Save Button
        TextInputEditText etApiKey = findViewById(R.id.etApiKey);
        MaterialButton btnSaveApiKey = findViewById(R.id.btnSaveApiKey);
        if (btnSaveApiKey != null && etApiKey != null) {
            // Load existing key
            try {
                MasterKey masterKey = new MasterKey.Builder(this)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                SharedPreferences encryptedPrefs = EncryptedSharedPreferences.create(
                        this,
                        "astro_secure_prefs",
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                String existingKey = encryptedPrefs.getString("openai_api_key", "");
                if (!existingKey.isEmpty()) {
                    etApiKey.setText(existingKey);
                }
            } catch (Exception e) {
                // Ignore load failure
            }

            btnSaveApiKey.setOnClickListener(v -> {
                String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
                try {
                    MasterKey masterKey = new MasterKey.Builder(this)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build();
                    SharedPreferences encryptedPrefs = EncryptedSharedPreferences.create(
                            this,
                            "astro_secure_prefs",
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                    encryptedPrefs.edit().putString("openai_api_key", apiKey).apply();
                    Toast.makeText(this,
                            apiKey.isEmpty() ? R.string.settings_api_key_cleared : R.string.settings_api_key_saved,
                            Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Observes LiveData from the ViewModel.
     */
    private void observeViewModel() {
        // Star Brightness
        viewModel.getStarBrightness().observe(this, brightness -> {
            isUpdatingUI = true;
            if (sliderBrightness != null && brightness != null) {
                float value = (brightness - 0.5f) * 100;
                sliderBrightness.setValue(Math.max(0f, Math.min(100f, value)));
            }
            if (tvBrightnessValue != null && brightness != null) {
                tvBrightnessValue.setText(String.format("%.1fx", brightness));
            }
            isUpdatingUI = false;
        });

        // Magnitude Limit
        viewModel.getMagnitudeLimit().observe(this, magnitude -> {
            isUpdatingUI = true;
            if (sliderMagnitude != null && magnitude != null) {
                sliderMagnitude.setValue(Math.max(1f, Math.min(8f, magnitude)));
            }
            if (tvMagnitudeValue != null && magnitude != null) {
                tvMagnitudeValue.setText(String.format("%.1f", magnitude));
            }
            isUpdatingUI = false;
        });

        // Night Mode
        viewModel.getNightMode().observe(this, nightMode -> {
            isUpdatingUI = true;
            if (switchNightMode != null && nightMode != null) {
                switchNightMode.setChecked(nightMode);
            }
            isUpdatingUI = false;
        });

        // Star Labels
        viewModel.getShowStarLabels().observe(this, show -> {
            isUpdatingUI = true;
            if (switchStarLabels != null && show != null) {
                switchStarLabels.setChecked(show);
            }
            isUpdatingUI = false;
        });

        // Constellation Lines
        viewModel.getShowConstellationLines().observe(this, show -> {
            isUpdatingUI = true;
            if (switchConstellationLines != null && show != null) {
                switchConstellationLines.setChecked(show);
            }
            isUpdatingUI = false;
        });

        // Constellation Names
        viewModel.getShowConstellationNames().observe(this, show -> {
            isUpdatingUI = true;
            if (switchConstellationNames != null && show != null) {
                switchConstellationNames.setChecked(show);
            }
            isUpdatingUI = false;
        });

        // Manual Scroll Mode
        viewModel.getEnableManualScroll().observe(this, enabled -> {
            isUpdatingUI = true;
            if (switchManualScroll != null && enabled != null) {
                switchManualScroll.setChecked(enabled);
            }
            isUpdatingUI = false;
        });
    }
}
