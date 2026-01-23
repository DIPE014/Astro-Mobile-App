package com.astro.app.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.astro.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

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
    private Slider sliderLabelMagnitude;
    private TextView tvLabelMagnitudeValue;
    private SwitchMaterial switchNightMode;

    // Layer settings views
    private SwitchMaterial switchStarLabels;
    private SwitchMaterial switchConstellationLines;
    private SwitchMaterial switchConstellationNames;
    private SwitchMaterial switchGrid;

    // Sensor settings views
    private SwitchMaterial switchAutoRotate;
    private Slider sliderSensorSmoothing;
    private TextView tvSensorSmoothingValue;
    private SwitchMaterial switchMagneticCorrection;

    // Field of view
    private Slider sliderFieldOfView;
    private TextView tvFieldOfViewValue;

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
        sliderLabelMagnitude = findViewById(R.id.sliderLabelMagnitude);
        tvLabelMagnitudeValue = findViewById(R.id.tvLabelMagnitudeValue);
        switchNightMode = findViewById(R.id.switchNightMode);

        // Layer settings
        switchStarLabels = findViewById(R.id.switchStarLabels);
        switchConstellationLines = findViewById(R.id.switchConstellationLines);
        switchConstellationNames = findViewById(R.id.switchConstellationNames);
        switchGrid = findViewById(R.id.switchGrid);

        // Sensor settings
        switchAutoRotate = findViewById(R.id.switchAutoRotate);
        sliderSensorSmoothing = findViewById(R.id.sliderSensorSmoothing);
        tvSensorSmoothingValue = findViewById(R.id.tvSensorSmoothingValue);
        switchMagneticCorrection = findViewById(R.id.switchMagneticCorrection);

        // Field of view
        sliderFieldOfView = findViewById(R.id.sliderFieldOfView);
        tvFieldOfViewValue = findViewById(R.id.tvFieldOfViewValue);

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
        if (sliderLabelMagnitude != null) {
            sliderLabelMagnitude.setValueFrom(1f);
            sliderLabelMagnitude.setValueTo(8f);
            sliderLabelMagnitude.setStepSize(0.5f);
        }
        if (sliderSensorSmoothing != null) {
            sliderSensorSmoothing.setValueFrom(0f);
            sliderSensorSmoothing.setValueTo(100f);
            sliderSensorSmoothing.setStepSize(1f);
        }
        if (sliderFieldOfView != null) {
            sliderFieldOfView.setValueFrom(10f);
            sliderFieldOfView.setValueTo(120f);
            sliderFieldOfView.setStepSize(1f);
        }
    }

    /**
     * Sets up click and change listeners for UI elements.
     */
    private void setupClickListeners() {
        // Back button
        MaterialButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Reset button
        MaterialButton btnReset = findViewById(R.id.btnResetDefaults);
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> viewModel.resetToDefaults());
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

        // Label Magnitude Slider
        if (sliderLabelMagnitude != null) {
            sliderLabelMagnitude.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser && !isUpdatingUI) {
                    viewModel.setLabelMagnitudeLimit(value);
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

        // Grid Switch
        if (switchGrid != null) {
            switchGrid.setOnCheckedChangeListener((button, isChecked) -> {
                if (!isUpdatingUI) {
                    viewModel.setShowGrid(isChecked);
                }
            });
        }

        // Auto Rotate Switch
        if (switchAutoRotate != null) {
            switchAutoRotate.setOnCheckedChangeListener((button, isChecked) -> {
                if (!isUpdatingUI) {
                    viewModel.setAutoRotate(isChecked);
                }
            });
        }

        // Sensor Smoothing Slider
        if (sliderSensorSmoothing != null) {
            sliderSensorSmoothing.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser && !isUpdatingUI) {
                    float smoothing = value / 100f;
                    viewModel.setSensorSmoothing(smoothing);
                }
            });
        }

        // Magnetic Correction Switch
        if (switchMagneticCorrection != null) {
            switchMagneticCorrection.setOnCheckedChangeListener((button, isChecked) -> {
                if (!isUpdatingUI) {
                    viewModel.setUseMagneticCorrection(isChecked);
                }
            });
        }

        // Field of View Slider
        if (sliderFieldOfView != null) {
            sliderFieldOfView.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser && !isUpdatingUI) {
                    viewModel.setFieldOfView(value);
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

        // Label Magnitude Limit
        viewModel.getLabelMagnitudeLimit().observe(this, magnitude -> {
            isUpdatingUI = true;
            if (sliderLabelMagnitude != null && magnitude != null) {
                sliderLabelMagnitude.setValue(Math.max(1f, Math.min(8f, magnitude)));
            }
            if (tvLabelMagnitudeValue != null && magnitude != null) {
                tvLabelMagnitudeValue.setText(String.format("%.1f", magnitude));
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

        // Grid
        viewModel.getShowGrid().observe(this, show -> {
            isUpdatingUI = true;
            if (switchGrid != null && show != null) {
                switchGrid.setChecked(show);
            }
            isUpdatingUI = false;
        });

        // Auto Rotate
        viewModel.getAutoRotate().observe(this, autoRotate -> {
            isUpdatingUI = true;
            if (switchAutoRotate != null && autoRotate != null) {
                switchAutoRotate.setChecked(autoRotate);
            }
            isUpdatingUI = false;
        });

        // Sensor Smoothing
        viewModel.getSensorSmoothing().observe(this, smoothing -> {
            isUpdatingUI = true;
            if (sliderSensorSmoothing != null && smoothing != null) {
                sliderSensorSmoothing.setValue(Math.max(0f, Math.min(100f, smoothing * 100)));
            }
            if (tvSensorSmoothingValue != null && smoothing != null) {
                tvSensorSmoothingValue.setText(String.format("%.0f%%", smoothing * 100));
            }
            isUpdatingUI = false;
        });

        // Magnetic Correction
        viewModel.getUseMagneticCorrection().observe(this, use -> {
            isUpdatingUI = true;
            if (switchMagneticCorrection != null && use != null) {
                switchMagneticCorrection.setChecked(use);
            }
            isUpdatingUI = false;
        });

        // Field of View
        viewModel.getFieldOfView().observe(this, fov -> {
            isUpdatingUI = true;
            if (sliderFieldOfView != null && fov != null) {
                sliderFieldOfView.setValue(Math.max(10f, Math.min(120f, fov)));
            }
            if (tvFieldOfViewValue != null && fov != null) {
                tvFieldOfViewValue.setText(String.format("%.0f\u00b0", fov));
            }
            isUpdatingUI = false;
        });
    }
}
