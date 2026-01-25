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
     * Initialize the view model, set up preferences storage, and load saved settings into LiveData.
     *
     * @param application the Application instance used to obtain SharedPreferences and app resources
     */
    public SettingsViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE);
        loadSettings();
    }

    // ==================== Settings Loading/Saving ====================

    /**
     * Populate the ViewModel's LiveData fields from SharedPreferences, using defined defaults when keys are absent.
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
     * Persist a float value under the given preference key and mark settings as changed.
     *
     * @param key   the preference key to store the value under
     * @param value the float value to persist
     */
    private void saveFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
        settingsChangedLiveData.setValue(true);
    }

    /**
     * Save the boolean value under the given key in SharedPreferences and mark that settings have changed.
     */
    private void saveBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
        settingsChangedLiveData.setValue(true);
    }

    /**
     * Persist an integer preference and mark settings as changed.
     *
     * @param key   the SharedPreferences key under which to store the value
     * @param value the integer value to save
     */
    private void saveInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
        settingsChangedLiveData.setValue(true);
    }

    // ==================== LiveData Getters ====================

    /**
     * LiveData that exposes the star brightness multiplier.
     *
     * @return the current star brightness multiplier
     */
    @NonNull
    public LiveData<Float> getStarBrightness() {
        return starBrightnessLiveData;
    }

    /**
     * LiveData exposing the current maximum magnitude to display.
     *
     * @return the current maximum magnitude to display as a Float
     */
    @NonNull
    public LiveData<Float> getMagnitudeLimit() {
        return magnitudeLimitLiveData;
    }

    /**
         * Exposes the current maximum magnitude threshold for star labels as LiveData.
         *
         * @return the LiveData holding the maximum magnitude for which star labels are displayed
         */
    @NonNull
    public LiveData<Float> getLabelMagnitudeLimit() {
        return labelMagnitudeLimitLiveData;
    }

    /**
     * Provides the LiveData representing whether night mode is enabled.
     *
     * @return true if night mode is enabled, false otherwise.
     */
    @NonNull
    public LiveData<Boolean> getNightMode() {
        return nightModeLiveData;
    }

    /**
         * Provides LiveData that indicates whether star labels are shown.
         *
         * @return the LiveData whose value is `true` if star labels should be shown, `false` otherwise
         */
    @NonNull
    public LiveData<Boolean> getShowStarLabels() {
        return showStarLabelsLiveData;
    }

    /**
         * Provides the current setting for constellation lines visibility.
         *
         * @return true if constellation lines should be shown, false otherwise.
         */
    @NonNull
    public LiveData<Boolean> getShowConstellationLines() {
        return showConstellationLinesLiveData;
    }

    /**
     * Provides the LiveData that indicates whether constellation names are shown.
     *
     * @return `true` if constellation names should be shown, `false` otherwise.
     */
    @NonNull
    public LiveData<Boolean> getShowConstellationNames() {
        return showConstellationNamesLiveData;
    }

    /**
     * Provides observable state of whether the coordinate grid is shown.
     *
     * @return LiveData containing true if the coordinate grid should be shown, false otherwise.
     */
    @NonNull
    public LiveData<Boolean> getShowGrid() {
        return showGridLiveData;
    }

    /**
     * Get the current auto-rotate setting.
     *
     * @return LiveData containing {@code true} if auto-rotate is enabled, {@code false} otherwise.
     */
    @NonNull
    public LiveData<Boolean> getAutoRotate() {
        return autoRotateLiveData;
    }

    /**
     * Exposes the current sensor smoothing factor as observable LiveData.
     *
     * @return the smoothing factor in the range 0.0 to 1.0
     */
    @NonNull
    public LiveData<Float> getSensorSmoothing() {
        return sensorSmoothingLiveData;
    }

    /**
     * Provides the current field of view setting in degrees.
     *
     * @return LiveData containing the field of view in degrees.
     */
    @NonNull
    public LiveData<Float> getFieldOfView() {
        return fieldOfViewLiveData;
    }

    /**
         * Exposes the current constellation line color.
         *
         * @return the current constellation line color as an ARGB integer
         */
    @NonNull
    public LiveData<Integer> getConstellationLineColor() {
        return constellationLineColorLiveData;
    }

    /**
         * LiveData that indicates whether magnetic correction is enabled.
         *
         * @return `true` if magnetic correction should be used, `false` otherwise.
         */
    @NonNull
    public LiveData<Boolean> getUseMagneticCorrection() {
        return useMagneticCorrectionLiveData;
    }

    /**
     * Indicates whether settings have changed since the last clear.
     *
     * @return `true` if settings have changed, `false` otherwise.
     */
    @NonNull
    public LiveData<Boolean> getSettingsChanged() {
        return settingsChangedLiveData;
    }

    // ==================== Settings Setters ====================

    /**
     * Update the application's star brightness setting.
     *
     * Updates the observable LiveData and persists the clamped value to SharedPreferences.
     *
     * @param brightness desired brightness multiplier; values below 0.5 are set to 0.5 and values above 2.0 are set to 2.0
     */
    public void setStarBrightness(float brightness) {
        float clamped = Math.max(0.5f, Math.min(2.0f, brightness));
        starBrightnessLiveData.setValue(clamped);
        saveFloat(KEY_STAR_BRIGHTNESS, clamped);
        Log.d(TAG, "Star brightness set to: " + clamped);
    }

    /**
     * Update the maximum star magnitude shown in the sky view.
     *
     * The provided value is clamped to the range -2 to 12, persisted to preferences, and posted to observers.
     *
     * @param magnitude desired magnitude limit; values less than -2 are set to -2 and values greater than 12 are set to 12
     */
    public void setMagnitudeLimit(float magnitude) {
        float clamped = Math.max(-2f, Math.min(12f, magnitude));
        magnitudeLimitLiveData.setValue(clamped);
        saveFloat(KEY_MAGNITUDE_LIMIT, clamped);
        Log.d(TAG, "Magnitude limit set to: " + clamped);
    }

    /**
     * Update the maximum star magnitude for which labels are displayed.
     *
     * Values outside the range -2 to 8 are clamped to that range.
     *
     * @param magnitude the desired label magnitude limit; values < -2 are treated as -2 and values > 8 are treated as 8
     */
    public void setLabelMagnitudeLimit(float magnitude) {
        float clamped = Math.max(-2f, Math.min(8f, magnitude));
        labelMagnitudeLimitLiveData.setValue(clamped);
        saveFloat(KEY_LABEL_MAGNITUDE_LIMIT, clamped);
        Log.d(TAG, "Label magnitude limit set to: " + clamped);
    }

    /**
     * Enable or disable the app's night (low-light) display mode.
     *
     * @param enabled true to enable night mode, false to disable it
     */
    public void setNightMode(boolean enabled) {
        nightModeLiveData.setValue(enabled);
        saveBoolean(KEY_NIGHT_MODE, enabled);
        Log.d(TAG, "Night mode set to: " + enabled);
    }

    /**
     * Toggle the night mode setting between enabled and disabled.
     *
     * If the setting is currently unset, this enables night mode.
     */
    public void toggleNightMode() {
        Boolean current = nightModeLiveData.getValue();
        setNightMode(current == null || !current);
    }

    /**
     * Enable or disable the display of star labels.
     *
     * @param show `true` to show star labels, `false` to hide them
     */
    public void setShowStarLabels(boolean show) {
        showStarLabelsLiveData.setValue(show);
        saveBoolean(KEY_SHOW_STAR_LABELS, show);
        Log.d(TAG, "Show star labels set to: " + show);
    }

    /**
     * Enable or disable display of constellation lines.
     *
     * @param show true to display constellation lines, false to hide them
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
     * Enable or disable automatic rotation of the view.
     *
     * @param enabled true to enable automatic rotation, false to disable it
     */
    public void setAutoRotate(boolean enabled) {
        autoRotateLiveData.setValue(enabled);
        saveBoolean(KEY_AUTO_ROTATE, enabled);
        Log.d(TAG, "Auto-rotate set to: " + enabled);
    }

    /**
     * Updates the sensor smoothing factor, clamped to the range 0.0–1.0 and persisted to preferences.
     *
     * @param smoothing the desired smoothing factor; values less than 0.0 become 0.0 and values greater than 1.0 become 1.0
     */
    public void setSensorSmoothing(float smoothing) {
        float clamped = Math.max(0f, Math.min(1f, smoothing));
        sensorSmoothingLiveData.setValue(clamped);
        saveFloat(KEY_SENSOR_SMOOTHING, clamped);
        Log.d(TAG, "Sensor smoothing set to: " + clamped);
    }

    /**
     * Update the field of view used by the UI and persist the value.
     *
     * @param fov field of view in degrees; values are clamped to the range 10–120
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
     * Applies the current field-of-view and auto-rotate settings to the given AstronomerModel.
     *
     * If a setting is not present in the ViewModel's LiveData, the corresponding model property is left unchanged.
     *
     * @param model the AstronomerModel to configure
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
         * Apply current stars-related settings to the provided StarsLayer.
         *
         * <p>Specifically applies magnitude limit, label magnitude limit, and star label visibility,
         * then requests the layer to redraw.</p>
         *
         * @param layer the StarsLayer to configure and redraw
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
     * Configure a ConstellationsLayer using current settings and request a redraw.
     *
     * Updates visibility of constellation lines and names and the line color when the corresponding settings are available, then triggers a redraw of the layer.
     *
     * @param layer the ConstellationsLayer to configure
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
     */
    public void clearSettingsChangedFlag() {
        settingsChangedLiveData.setValue(false);
    }
}