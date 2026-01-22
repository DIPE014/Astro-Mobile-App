package com.astro.app.ui.settings;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.astro.app.core.control.AstronomerModel;
import com.astro.app.core.layers.ConstellationsLayer;
import com.astro.app.core.layers.StarsLayer;

/**
 * ViewModel for the SettingsActivity.
 *
 * <p>Manages the application settings including:
 * <ul>
 *   <li>Display settings (star brightness, magnitude limit, night mode)</li>
 *   <li>Layer visibility settings</li>
 *   <li>Sensor settings</li>
 *   <li>Preference persistence via SharedPreferences</li>
 * </ul>
 * </p>
 *
 * <p>Settings are automatically persisted to SharedPreferences when changed
 * and can be applied to the AstronomerModel and layer instances.</p>
 */
public class SettingsViewModel extends AndroidViewModel {

    private static final String TAG = "SettingsViewModel";

    // SharedPreferences keys
    public static final String PREFS_NAME = "astro_settings";
    public static final String KEY_STAR_BRIGHTNESS = "star_brightness";
    public static final String KEY_MAGNITUDE_LIMIT = "magnitude_limit";
    public static final String KEY_LABEL_MAGNITUDE_LIMIT = "label_magnitude_limit";
    public static final String KEY_NIGHT_MODE = "night_mode";
    public static final String KEY_SHOW_STAR_LABELS = "show_star_labels";
    public static final String KEY_SHOW_CONSTELLATION_LINES = "show_constellation_lines";
    public static final String KEY_SHOW_CONSTELLATION_NAMES = "show_constellation_names";
    public static final String KEY_SHOW_GRID = "show_grid";
    public static final String KEY_AUTO_ROTATE = "auto_rotate";
    public static final String KEY_SENSOR_SMOOTHING = "sensor_smoothing";
    public static final String KEY_FIELD_OF_VIEW = "field_of_view";
    public static final String KEY_CONSTELLATION_LINE_COLOR = "constellation_line_color";
    public static final String KEY_USE_MAGNETIC_CORRECTION = "use_magnetic_correction";

    // Default values
    private static final float DEFAULT_STAR_BRIGHTNESS = 1.0f;
    private static final float DEFAULT_MAGNITUDE_LIMIT = 6.5f;
    private static final float DEFAULT_LABEL_MAGNITUDE_LIMIT = 3.0f;
    private static final boolean DEFAULT_NIGHT_MODE = false;
    private static final boolean DEFAULT_SHOW_STAR_LABELS = true;
    private static final boolean DEFAULT_SHOW_CONSTELLATION_LINES = true;
    private static final boolean DEFAULT_SHOW_CONSTELLATION_NAMES = true;
    private static final boolean DEFAULT_SHOW_GRID = false;
    private static final boolean DEFAULT_AUTO_ROTATE = true;
    private static final float DEFAULT_SENSOR_SMOOTHING = 0.5f;
    private static final float DEFAULT_FIELD_OF_VIEW = 45f;
    private static final int DEFAULT_CONSTELLATION_LINE_COLOR = 0x40FFFFFF;
    private static final boolean DEFAULT_USE_MAGNETIC_CORRECTION = true;

    // ==================== LiveData Fields ====================

    /** Star brightness multiplier (0.5 to 2.0) */
    private final MutableLiveData<Float> starBrightnessLiveData = new MutableLiveData<>();

    /** Maximum star magnitude to display */
    private final MutableLiveData<Float> magnitudeLimitLiveData = new MutableLiveData<>();

    /** Maximum magnitude for showing star labels */
    private final MutableLiveData<Float> labelMagnitudeLimitLiveData = new MutableLiveData<>();

    /** Night mode (red filter) enabled */
    private final MutableLiveData<Boolean> nightModeLiveData = new MutableLiveData<>();

    /** Show star name labels */
    private final MutableLiveData<Boolean> showStarLabelsLiveData = new MutableLiveData<>();

    /** Show constellation connection lines */
    private final MutableLiveData<Boolean> showConstellationLinesLiveData = new MutableLiveData<>();

    /** Show constellation names */
    private final MutableLiveData<Boolean> showConstellationNamesLiveData = new MutableLiveData<>();

    /** Show coordinate grid */
    private final MutableLiveData<Boolean> showGridLiveData = new MutableLiveData<>();

    /** Auto-rotate view with device sensors */
    private final MutableLiveData<Boolean> autoRotateLiveData = new MutableLiveData<>();

    /** Sensor smoothing factor (0.0 to 1.0) */
    private final MutableLiveData<Float> sensorSmoothingLiveData = new MutableLiveData<>();

    /** Field of view in degrees */
    private final MutableLiveData<Float> fieldOfViewLiveData = new MutableLiveData<>();

    /** Constellation line color */
    private final MutableLiveData<Integer> constellationLineColorLiveData = new MutableLiveData<>();

    /** Use magnetic declination correction */
    private final MutableLiveData<Boolean> useMagneticCorrectionLiveData = new MutableLiveData<>();

    /** Settings changed flag for notifying other components */
    private final MutableLiveData<Boolean> settingsChangedLiveData = new MutableLiveData<>(false);

    // ==================== Dependencies ====================

    private SharedPreferences sharedPreferences;

    /**
     * Creates a new SettingsViewModel.
     *
     * @param application The application context
     */
    public SettingsViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE);
        loadSettings();
    }

    // ==================== Settings Loading/Saving ====================

    /**
     * Loads all settings from SharedPreferences.
     */
    private void loadSettings() {
        starBrightnessLiveData.setValue(
                sharedPreferences.getFloat(KEY_STAR_BRIGHTNESS, DEFAULT_STAR_BRIGHTNESS));
        magnitudeLimitLiveData.setValue(
                sharedPreferences.getFloat(KEY_MAGNITUDE_LIMIT, DEFAULT_MAGNITUDE_LIMIT));
        labelMagnitudeLimitLiveData.setValue(
                sharedPreferences.getFloat(KEY_LABEL_MAGNITUDE_LIMIT, DEFAULT_LABEL_MAGNITUDE_LIMIT));
        nightModeLiveData.setValue(
                sharedPreferences.getBoolean(KEY_NIGHT_MODE, DEFAULT_NIGHT_MODE));
        showStarLabelsLiveData.setValue(
                sharedPreferences.getBoolean(KEY_SHOW_STAR_LABELS, DEFAULT_SHOW_STAR_LABELS));
        showConstellationLinesLiveData.setValue(
                sharedPreferences.getBoolean(KEY_SHOW_CONSTELLATION_LINES, DEFAULT_SHOW_CONSTELLATION_LINES));
        showConstellationNamesLiveData.setValue(
                sharedPreferences.getBoolean(KEY_SHOW_CONSTELLATION_NAMES, DEFAULT_SHOW_CONSTELLATION_NAMES));
        showGridLiveData.setValue(
                sharedPreferences.getBoolean(KEY_SHOW_GRID, DEFAULT_SHOW_GRID));
        autoRotateLiveData.setValue(
                sharedPreferences.getBoolean(KEY_AUTO_ROTATE, DEFAULT_AUTO_ROTATE));
        sensorSmoothingLiveData.setValue(
                sharedPreferences.getFloat(KEY_SENSOR_SMOOTHING, DEFAULT_SENSOR_SMOOTHING));
        fieldOfViewLiveData.setValue(
                sharedPreferences.getFloat(KEY_FIELD_OF_VIEW, DEFAULT_FIELD_OF_VIEW));
        constellationLineColorLiveData.setValue(
                sharedPreferences.getInt(KEY_CONSTELLATION_LINE_COLOR, DEFAULT_CONSTELLATION_LINE_COLOR));
        useMagneticCorrectionLiveData.setValue(
                sharedPreferences.getBoolean(KEY_USE_MAGNETIC_CORRECTION, DEFAULT_USE_MAGNETIC_CORRECTION));

        Log.d(TAG, "Settings loaded from SharedPreferences");
    }

    /**
     * Saves a float setting to SharedPreferences.
     */
    private void saveFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
        settingsChangedLiveData.setValue(true);
    }

    /**
     * Saves a boolean setting to SharedPreferences.
     */
    private void saveBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
        settingsChangedLiveData.setValue(true);
    }

    /**
     * Saves an int setting to SharedPreferences.
     */
    private void saveInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
        settingsChangedLiveData.setValue(true);
    }

    // ==================== LiveData Getters ====================

    /**
     * Returns the LiveData for star brightness.
     *
     * @return LiveData containing the star brightness multiplier
     */
    @NonNull
    public LiveData<Float> getStarBrightness() {
        return starBrightnessLiveData;
    }

    /**
     * Returns the LiveData for magnitude limit.
     *
     * @return LiveData containing the maximum magnitude to display
     */
    @NonNull
    public LiveData<Float> getMagnitudeLimit() {
        return magnitudeLimitLiveData;
    }

    /**
     * Returns the LiveData for label magnitude limit.
     *
     * @return LiveData containing the maximum magnitude for labels
     */
    @NonNull
    public LiveData<Float> getLabelMagnitudeLimit() {
        return labelMagnitudeLimitLiveData;
    }

    /**
     * Returns the LiveData for night mode state.
     *
     * @return LiveData containing true if night mode is enabled
     */
    @NonNull
    public LiveData<Boolean> getNightMode() {
        return nightModeLiveData;
    }

    /**
     * Returns the LiveData for star labels visibility.
     *
     * @return LiveData containing true if star labels should be shown
     */
    @NonNull
    public LiveData<Boolean> getShowStarLabels() {
        return showStarLabelsLiveData;
    }

    /**
     * Returns the LiveData for constellation lines visibility.
     *
     * @return LiveData containing true if constellation lines should be shown
     */
    @NonNull
    public LiveData<Boolean> getShowConstellationLines() {
        return showConstellationLinesLiveData;
    }

    /**
     * Returns the LiveData for constellation names visibility.
     *
     * @return LiveData containing true if constellation names should be shown
     */
    @NonNull
    public LiveData<Boolean> getShowConstellationNames() {
        return showConstellationNamesLiveData;
    }

    /**
     * Returns the LiveData for grid visibility.
     *
     * @return LiveData containing true if the coordinate grid should be shown
     */
    @NonNull
    public LiveData<Boolean> getShowGrid() {
        return showGridLiveData;
    }

    /**
     * Returns the LiveData for auto-rotate setting.
     *
     * @return LiveData containing true if auto-rotate is enabled
     */
    @NonNull
    public LiveData<Boolean> getAutoRotate() {
        return autoRotateLiveData;
    }

    /**
     * Returns the LiveData for sensor smoothing.
     *
     * @return LiveData containing the smoothing factor (0.0 to 1.0)
     */
    @NonNull
    public LiveData<Float> getSensorSmoothing() {
        return sensorSmoothingLiveData;
    }

    /**
     * Returns the LiveData for field of view.
     *
     * @return LiveData containing the FOV in degrees
     */
    @NonNull
    public LiveData<Float> getFieldOfView() {
        return fieldOfViewLiveData;
    }

    /**
     * Returns the LiveData for constellation line color.
     *
     * @return LiveData containing the ARGB color value
     */
    @NonNull
    public LiveData<Integer> getConstellationLineColor() {
        return constellationLineColorLiveData;
    }

    /**
     * Returns the LiveData for magnetic correction setting.
     *
     * @return LiveData containing true if magnetic correction should be used
     */
    @NonNull
    public LiveData<Boolean> getUseMagneticCorrection() {
        return useMagneticCorrectionLiveData;
    }

    /**
     * Returns the LiveData for settings changed flag.
     *
     * @return LiveData containing true if settings have changed
     */
    @NonNull
    public LiveData<Boolean> getSettingsChanged() {
        return settingsChangedLiveData;
    }

    // ==================== Settings Setters ====================

    /**
     * Sets the star brightness multiplier.
     *
     * @param brightness The brightness multiplier (0.5 to 2.0)
     */
    public void setStarBrightness(float brightness) {
        float clamped = Math.max(0.5f, Math.min(2.0f, brightness));
        starBrightnessLiveData.setValue(clamped);
        saveFloat(KEY_STAR_BRIGHTNESS, clamped);
        Log.d(TAG, "Star brightness set to: " + clamped);
    }

    /**
     * Sets the maximum star magnitude to display.
     *
     * @param magnitude The magnitude limit (-2 to 12)
     */
    public void setMagnitudeLimit(float magnitude) {
        float clamped = Math.max(-2f, Math.min(12f, magnitude));
        magnitudeLimitLiveData.setValue(clamped);
        saveFloat(KEY_MAGNITUDE_LIMIT, clamped);
        Log.d(TAG, "Magnitude limit set to: " + clamped);
    }

    /**
     * Sets the maximum magnitude for showing star labels.
     *
     * @param magnitude The label magnitude limit (-2 to 8)
     */
    public void setLabelMagnitudeLimit(float magnitude) {
        float clamped = Math.max(-2f, Math.min(8f, magnitude));
        labelMagnitudeLimitLiveData.setValue(clamped);
        saveFloat(KEY_LABEL_MAGNITUDE_LIMIT, clamped);
        Log.d(TAG, "Label magnitude limit set to: " + clamped);
    }

    /**
     * Sets the night mode state.
     *
     * @param enabled true to enable night mode
     */
    public void setNightMode(boolean enabled) {
        nightModeLiveData.setValue(enabled);
        saveBoolean(KEY_NIGHT_MODE, enabled);
        Log.d(TAG, "Night mode set to: " + enabled);
    }

    /**
     * Toggles night mode on/off.
     */
    public void toggleNightMode() {
        Boolean current = nightModeLiveData.getValue();
        setNightMode(current == null || !current);
    }

    /**
     * Sets whether star labels should be shown.
     *
     * @param show true to show star labels
     */
    public void setShowStarLabels(boolean show) {
        showStarLabelsLiveData.setValue(show);
        saveBoolean(KEY_SHOW_STAR_LABELS, show);
        Log.d(TAG, "Show star labels set to: " + show);
    }

    /**
     * Sets whether constellation lines should be shown.
     *
     * @param show true to show constellation lines
     */
    public void setShowConstellationLines(boolean show) {
        showConstellationLinesLiveData.setValue(show);
        saveBoolean(KEY_SHOW_CONSTELLATION_LINES, show);
        Log.d(TAG, "Show constellation lines set to: " + show);
    }

    /**
     * Sets whether constellation names should be shown.
     *
     * @param show true to show constellation names
     */
    public void setShowConstellationNames(boolean show) {
        showConstellationNamesLiveData.setValue(show);
        saveBoolean(KEY_SHOW_CONSTELLATION_NAMES, show);
        Log.d(TAG, "Show constellation names set to: " + show);
    }

    /**
     * Sets whether the coordinate grid should be shown.
     *
     * @param show true to show the grid
     */
    public void setShowGrid(boolean show) {
        showGridLiveData.setValue(show);
        saveBoolean(KEY_SHOW_GRID, show);
        Log.d(TAG, "Show grid set to: " + show);
    }

    /**
     * Sets whether auto-rotate is enabled.
     *
     * @param enabled true to enable auto-rotate
     */
    public void setAutoRotate(boolean enabled) {
        autoRotateLiveData.setValue(enabled);
        saveBoolean(KEY_AUTO_ROTATE, enabled);
        Log.d(TAG, "Auto-rotate set to: " + enabled);
    }

    /**
     * Sets the sensor smoothing factor.
     *
     * @param smoothing The smoothing factor (0.0 to 1.0)
     */
    public void setSensorSmoothing(float smoothing) {
        float clamped = Math.max(0f, Math.min(1f, smoothing));
        sensorSmoothingLiveData.setValue(clamped);
        saveFloat(KEY_SENSOR_SMOOTHING, clamped);
        Log.d(TAG, "Sensor smoothing set to: " + clamped);
    }

    /**
     * Sets the field of view.
     *
     * @param fov The field of view in degrees (10 to 120)
     */
    public void setFieldOfView(float fov) {
        float clamped = Math.max(10f, Math.min(120f, fov));
        fieldOfViewLiveData.setValue(clamped);
        saveFloat(KEY_FIELD_OF_VIEW, clamped);
        Log.d(TAG, "Field of view set to: " + clamped);
    }

    /**
     * Sets the constellation line color.
     *
     * @param color The ARGB color value
     */
    public void setConstellationLineColor(int color) {
        constellationLineColorLiveData.setValue(color);
        saveInt(KEY_CONSTELLATION_LINE_COLOR, color);
        Log.d(TAG, "Constellation line color set to: " + Integer.toHexString(color));
    }

    /**
     * Sets whether magnetic correction should be used.
     *
     * @param use true to use magnetic correction
     */
    public void setUseMagneticCorrection(boolean use) {
        useMagneticCorrectionLiveData.setValue(use);
        saveBoolean(KEY_USE_MAGNETIC_CORRECTION, use);
        Log.d(TAG, "Use magnetic correction set to: " + use);
    }

    // ==================== Apply Settings to Components ====================

    /**
     * Applies current settings to the AstronomerModel.
     *
     * @param model The astronomer model to configure
     */
    public void applyToAstronomerModel(@NonNull AstronomerModel model) {
        Float fov = fieldOfViewLiveData.getValue();
        if (fov != null) {
            model.setFieldOfView(fov);
        }

        Boolean autoUpdate = autoRotateLiveData.getValue();
        if (autoUpdate != null) {
            model.setAutoUpdatePointing(autoUpdate);
        }

        Log.d(TAG, "Settings applied to AstronomerModel");
    }

    /**
     * Applies current settings to the StarsLayer.
     *
     * @param layer The stars layer to configure
     */
    public void applyToStarsLayer(@NonNull StarsLayer layer) {
        Float magnitudeLimit = magnitudeLimitLiveData.getValue();
        if (magnitudeLimit != null) {
            layer.setMagnitudeLimit(magnitudeLimit);
        }

        Float labelMagnitudeLimit = labelMagnitudeLimitLiveData.getValue();
        if (labelMagnitudeLimit != null) {
            layer.setLabelMagnitudeLimit(labelMagnitudeLimit);
        }

        Boolean showLabels = showStarLabelsLiveData.getValue();
        if (showLabels != null) {
            layer.setShowLabels(showLabels);
        }

        // Redraw layer to apply changes
        layer.redraw();

        Log.d(TAG, "Settings applied to StarsLayer");
    }

    /**
     * Applies current settings to the ConstellationsLayer.
     *
     * @param layer The constellations layer to configure
     */
    public void applyToConstellationsLayer(@NonNull ConstellationsLayer layer) {
        Boolean showLines = showConstellationLinesLiveData.getValue();
        if (showLines != null) {
            layer.setShowLines(showLines);
        }

        Boolean showNames = showConstellationNamesLiveData.getValue();
        if (showNames != null) {
            layer.setShowNames(showNames);
        }

        Integer lineColor = constellationLineColorLiveData.getValue();
        if (lineColor != null) {
            layer.setLineColor(lineColor);
        }

        // Redraw layer to apply changes
        layer.redraw();

        Log.d(TAG, "Settings applied to ConstellationsLayer");
    }

    // ==================== Reset Settings ====================

    /**
     * Resets all settings to their default values.
     */
    public void resetToDefaults() {
        setStarBrightness(DEFAULT_STAR_BRIGHTNESS);
        setMagnitudeLimit(DEFAULT_MAGNITUDE_LIMIT);
        setLabelMagnitudeLimit(DEFAULT_LABEL_MAGNITUDE_LIMIT);
        setNightMode(DEFAULT_NIGHT_MODE);
        setShowStarLabels(DEFAULT_SHOW_STAR_LABELS);
        setShowConstellationLines(DEFAULT_SHOW_CONSTELLATION_LINES);
        setShowConstellationNames(DEFAULT_SHOW_CONSTELLATION_NAMES);
        setShowGrid(DEFAULT_SHOW_GRID);
        setAutoRotate(DEFAULT_AUTO_ROTATE);
        setSensorSmoothing(DEFAULT_SENSOR_SMOOTHING);
        setFieldOfView(DEFAULT_FIELD_OF_VIEW);
        setConstellationLineColor(DEFAULT_CONSTELLATION_LINE_COLOR);
        setUseMagneticCorrection(DEFAULT_USE_MAGNETIC_CORRECTION);

        Log.d(TAG, "Settings reset to defaults");
    }

    /**
     * Clears the settings changed flag.
     *
     * <p>Call this after handling settings changes in the observer.</p>
     */
    public void clearSettingsChangedFlag() {
        settingsChangedLiveData.setValue(false);
    }
}
