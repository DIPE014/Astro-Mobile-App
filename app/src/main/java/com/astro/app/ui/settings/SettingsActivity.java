package com.astro.app.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.astro.app.R;
import com.google.android.material.button.MaterialButton;
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
    private SeekBar seekBarBrightness;
    private TextView tvBrightnessValue;
    private SeekBar seekBarMagnitude;
    private TextView tvMagnitudeValue;
    private SeekBar seekBarLabelMagnitude;
    private TextView tvLabelMagnitudeValue;
    private SwitchMaterial switchNightMode;

    // Layer settings views
    private SwitchMaterial switchStarLabels;
    private SwitchMaterial switchConstellationLines;
    private SwitchMaterial switchConstellationNames;
    private SwitchMaterial switchGrid;

    // Sensor settings views
    private SwitchMaterial switchAutoRotate;
    private SeekBar seekBarSensorSmoothing;
    private TextView tvSensorSmoothingValue;
    private SwitchMaterial switchMagneticCorrection;

    // Field of view
    private SeekBar seekBarFieldOfView;
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
        seekBarBrightness = findViewById(R.id.seekBarBrightness);
        tvBrightnessValue = findViewById(R.id.tvBrightnessValue);
        seekBarMagnitude = findViewById(R.id.seekBarMagnitude);
        tvMagnitudeValue = findViewById(R.id.tvMagnitudeValue);
        seekBarLabelMagnitude = findViewById(R.id.seekBarLabelMagnitude);
        tvLabelMagnitudeValue = findViewById(R.id.tvLabelMagnitudeValue);
        switchNightMode = findViewById(R.id.switchNightMode);

        // Layer settings
        switchStarLabels = findViewById(R.id.switchStarLabels);
        switchConstellationLines = findViewById(R.id.switchConstellationLines);
        switchConstellationNames = findViewById(R.id.switchConstellationNames);
        switchGrid = findViewById(R.id.switchGrid);

        // Sensor settings
        switchAutoRotate = findViewById(R.id.switchAutoRotate);
        seekBarSensorSmoothing = findViewById(R.id.seekBarSensorSmoothing);
        tvSensorSmoothingValue = findViewById(R.id.tvSensorSmoothingValue);
        switchMagneticCorrection = findViewById(R.id.switchMagneticCorrection);

        // Field of view
        seekBarFieldOfView = findViewById(R.id.seekBarFieldOfView);
        tvFieldOfViewValue = findViewById(R.id.tvFieldOfViewValue);

        // Configure SeekBar ranges
        if (seekBarBrightness != null) {
            seekBarBrightness.setMax(150); // 0.5 to 2.0 in 0.01 steps
        }
        if (seekBarMagnitude != null) {
            seekBarMagnitude.setMax(140); // -2 to 12 in 0.1 steps
        }
        if (seekBarLabelMagnitude != null) {
            seekBarLabelMagnitude.setMax(100); // -2 to 8 in 0.1 steps
        }
        if (seekBarSensorSmoothing != null) {
            seekBarSensorSmoothing.setMax(100); // 0.0 to 1.0 in 0.01 steps
        }
        if (seekBarFieldOfView != null) {
            seekBarFieldOfView.setMax(110); // 10 to 120 degrees
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

        // Brightness SeekBar
        if (seekBarBrightness != null) {
            seekBarBrightness.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && !isUpdatingUI) {
                        float brightness = 0.5f + (progress / 100f);
                        viewModel.setStarBrightness(brightness);
                    }
                }
            });
        }

        // Magnitude SeekBar
        if (seekBarMagnitude != null) {
            seekBarMagnitude.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && !isUpdatingUI) {
                        float magnitude = -2f + (progress / 10f);
                        viewModel.setMagnitudeLimit(magnitude);
                    }
                }
            });
        }

        // Label Magnitude SeekBar
        if (seekBarLabelMagnitude != null) {
            seekBarLabelMagnitude.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && !isUpdatingUI) {
                        float magnitude = -2f + (progress / 10f);
                        viewModel.setLabelMagnitudeLimit(magnitude);
                    }
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

        // Sensor Smoothing SeekBar
        if (seekBarSensorSmoothing != null) {
            seekBarSensorSmoothing.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && !isUpdatingUI) {
                        float smoothing = progress / 100f;
                        viewModel.setSensorSmoothing(smoothing);
                    }
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

        // Field of View SeekBar
        if (seekBarFieldOfView != null) {
            seekBarFieldOfView.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && !isUpdatingUI) {
                        float fov = 10f + progress;
                        viewModel.setFieldOfView(fov);
                    }
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
            if (seekBarBrightness != null && brightness != null) {
                int progress = (int) ((brightness - 0.5f) * 100);
                seekBarBrightness.setProgress(Math.max(0, Math.min(150, progress)));
            }
            if (tvBrightnessValue != null && brightness != null) {
                tvBrightnessValue.setText(String.format("%.1fx", brightness));
            }
            isUpdatingUI = false;
        });

        // Magnitude Limit
        viewModel.getMagnitudeLimit().observe(this, magnitude -> {
            isUpdatingUI = true;
            if (seekBarMagnitude != null && magnitude != null) {
                int progress = (int) ((magnitude + 2) * 10);
                seekBarMagnitude.setProgress(Math.max(0, Math.min(140, progress)));
            }
            if (tvMagnitudeValue != null && magnitude != null) {
                tvMagnitudeValue.setText(String.format("%.1f", magnitude));
            }
            isUpdatingUI = false;
        });

        // Label Magnitude Limit
        viewModel.getLabelMagnitudeLimit().observe(this, magnitude -> {
            isUpdatingUI = true;
            if (seekBarLabelMagnitude != null && magnitude != null) {
                int progress = (int) ((magnitude + 2) * 10);
                seekBarLabelMagnitude.setProgress(Math.max(0, Math.min(100, progress)));
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
            if (seekBarSensorSmoothing != null && smoothing != null) {
                seekBarSensorSmoothing.setProgress((int) (smoothing * 100));
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
            if (seekBarFieldOfView != null && fov != null) {
                seekBarFieldOfView.setProgress((int) (fov - 10));
            }
            if (tvFieldOfViewValue != null && fov != null) {
                tvFieldOfViewValue.setText(String.format("%.0f\u00b0", fov));
            }
            isUpdatingUI = false;
        });
    }

    /**
     * Simple SeekBar listener that only requires onProgressChanged to be implemented.
     */
    private abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Not needed
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Not needed
        }
    }
}
