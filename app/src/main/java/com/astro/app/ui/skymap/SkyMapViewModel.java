package com.astro.app.ui.skymap;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.astro.app.common.model.Pointing;
import com.astro.app.core.control.AstronomerModel;
import com.astro.app.core.layers.ConstellationsLayer;
import com.astro.app.core.layers.Layer;
import com.astro.app.core.layers.StarsLayer;
import com.astro.app.data.model.CelestialObject;
import com.astro.app.data.model.ConstellationData;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.StarRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for the SkyMapActivity.
 *
 * <p>Manages the state for the sky map view including:
 * <ul>
 *   <li>Current pointing direction (where the device is aimed)</li>
 *   <li>Visible celestial objects (stars and constellations)</li>
 *   <li>Currently selected object for detail view</li>
 *   <li>Night mode state for display</li>
 *   <li>Field of view for zoom level</li>
 *   <li>Layer visibility toggles</li>
 * </ul>
 * </p>
 *
 * <p>This ViewModel integrates with the AstronomerModel to receive pointing updates
 * and with repositories to access star and constellation data.</p>
 */
public class SkyMapViewModel extends AndroidViewModel implements AstronomerModel.PointingListener {

    private static final String TAG = "SkyMapViewModel";

    // Default field of view in degrees
    private static final float DEFAULT_FOV = 45f;
    private static final float MIN_FOV = 10f;
    private static final float MAX_FOV = 120f;

    // ==================== LiveData Fields ====================

    /** Current pointing direction (where the device is aimed) */
    private final MutableLiveData<Pointing> pointingLiveData = new MutableLiveData<>();

    /** List of visible stars within the current field of view */
    private final MutableLiveData<List<StarData>> visibleStarsLiveData = new MutableLiveData<>(new ArrayList<>());

    /** List of visible constellations within the current field of view */
    private final MutableLiveData<List<ConstellationData>> visibleConstellationsLiveData = new MutableLiveData<>(new ArrayList<>());

    /** Currently selected celestial object (star or constellation) */
    private final MutableLiveData<CelestialObject> selectedObjectLiveData = new MutableLiveData<>();

    /** Night mode state (red filter for preserving night vision) */
    private final MutableLiveData<Boolean> nightModeLiveData = new MutableLiveData<>(false);

    /** Current field of view in degrees */
    private final MutableLiveData<Float> fieldOfViewLiveData = new MutableLiveData<>(DEFAULT_FOV);

    /** Layer visibility states (layer ID -> visible) */
    private final MutableLiveData<Map<String, Boolean>> layerVisibilityLiveData = new MutableLiveData<>(new HashMap<>());

    /** Search results for celestial objects */
    private final MutableLiveData<List<CelestialObject>> searchResultsLiveData = new MutableLiveData<>(new ArrayList<>());

    /** Error messages for UI feedback */
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    /** Loading state for async operations */
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    // ==================== Dependencies ====================

    @Nullable
    private AstronomerModel astronomerModel;

    @Nullable
    private StarRepository starRepository;

    @Nullable
    private ConstellationRepository constellationRepository;

    /** Map of layer ID to layer instance */
    private final Map<String, Layer> layers = new HashMap<>();

    /**
     * Creates a new SkyMapViewModel.
     *
     * @param application The application context
     */
    public SkyMapViewModel(@NonNull Application application) {
        super(application);
        initializeDefaultLayerVisibility();
    }

    /**
     * Initializes default layer visibility settings.
     */
    private void initializeDefaultLayerVisibility() {
        Map<String, Boolean> visibility = new HashMap<>();
        visibility.put(StarsLayer.LAYER_ID, true);
        visibility.put(ConstellationsLayer.LAYER_ID, true);
        visibility.put("layer_grid", false);  // Grid layer off by default
        layerVisibilityLiveData.setValue(visibility);
    }

    // ==================== Dependency Injection ====================

    /**
     * Sets the AstronomerModel for receiving pointing updates.
     *
     * @param astronomerModel The astronomer model instance
     */
    public void setAstronomerModel(@NonNull AstronomerModel astronomerModel) {
        // Remove listener from old model
        if (this.astronomerModel != null) {
            this.astronomerModel.removePointingListener(this);
        }

        this.astronomerModel = astronomerModel;
        this.astronomerModel.addPointingListener(this);

        // Initialize with current pointing
        pointingLiveData.postValue(astronomerModel.getPointing());
        fieldOfViewLiveData.postValue(astronomerModel.getFieldOfView());
    }

    /**
     * Sets the star repository for accessing star data.
     *
     * @param starRepository The star repository instance
     */
    public void setStarRepository(@NonNull StarRepository starRepository) {
        this.starRepository = starRepository;
    }

    /**
     * Sets the constellation repository for accessing constellation data.
     *
     * @param constellationRepository The constellation repository instance
     */
    public void setConstellationRepository(@NonNull ConstellationRepository constellationRepository) {
        this.constellationRepository = constellationRepository;
    }

    /**
     * Registers a layer with this ViewModel.
     *
     * @param layer The layer to register
     */
    public void registerLayer(@NonNull Layer layer) {
        layers.put(layer.getLayerId(), layer);

        // Update layer visibility map
        Map<String, Boolean> visibility = layerVisibilityLiveData.getValue();
        if (visibility != null && !visibility.containsKey(layer.getLayerId())) {
            visibility.put(layer.getLayerId(), layer.isVisible());
            layerVisibilityLiveData.setValue(visibility);
        }
    }

    // ==================== LiveData Getters ====================

    /**
     * Returns the LiveData for the current pointing direction.
     *
     * @return LiveData containing the current pointing
     */
    @NonNull
    public LiveData<Pointing> getPointing() {
        return pointingLiveData;
    }

    /**
     * Returns the LiveData for visible stars.
     *
     * @return LiveData containing list of visible stars
     */
    @NonNull
    public LiveData<List<StarData>> getVisibleStars() {
        return visibleStarsLiveData;
    }

    /**
     * Returns the LiveData for visible constellations.
     *
     * @return LiveData containing list of visible constellations
     */
    @NonNull
    public LiveData<List<ConstellationData>> getVisibleConstellations() {
        return visibleConstellationsLiveData;
    }

    /**
     * Returns the LiveData for the selected celestial object.
     *
     * @return LiveData containing the selected object, or null if none selected
     */
    @NonNull
    public LiveData<CelestialObject> getSelectedObject() {
        return selectedObjectLiveData;
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
     * Returns the LiveData for the current field of view.
     *
     * @return LiveData containing the FOV in degrees
     */
    @NonNull
    public LiveData<Float> getFieldOfView() {
        return fieldOfViewLiveData;
    }

    /**
     * Returns the LiveData for layer visibility states.
     *
     * @return LiveData containing map of layer ID to visibility state
     */
    @NonNull
    public LiveData<Map<String, Boolean>> getLayerVisibility() {
        return layerVisibilityLiveData;
    }

    /**
     * Returns the LiveData for search results.
     *
     * @return LiveData containing list of matching celestial objects
     */
    @NonNull
    public LiveData<List<CelestialObject>> getSearchResults() {
        return searchResultsLiveData;
    }

    /**
     * Returns the LiveData for error messages.
     *
     * @return LiveData containing error message, or null if no error
     */
    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    /**
     * Returns the LiveData for loading state.
     *
     * @return LiveData containing true if loading
     */
    @NonNull
    public LiveData<Boolean> isLoading() {
        return loadingLiveData;
    }

    // ==================== User Actions ====================

    /**
     * Selects a celestial object for detailed viewing.
     *
     * @param object The object to select, or null to clear selection
     */
    public void selectObject(@Nullable CelestialObject object) {
        selectedObjectLiveData.setValue(object);
        Log.d(TAG, "Selected object: " + (object != null ? object.getName() : "none"));
    }

    /**
     * Selects a star by its ID.
     *
     * @param starId The ID of the star to select
     */
    public void selectStarById(@NonNull String starId) {
        if (starRepository != null) {
            StarData star = starRepository.getStarById(starId);
            if (star != null) {
                selectObject(star);
            } else {
                errorMessageLiveData.setValue("Star not found: " + starId);
            }
        }
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        selectedObjectLiveData.setValue(null);
    }

    /**
     * Searches for celestial objects matching the query.
     *
     * <p>Searches both stars and constellations by name.</p>
     *
     * @param query The search query
     */
    public void search(@NonNull String query) {
        if (query.trim().isEmpty()) {
            searchResultsLiveData.setValue(new ArrayList<>());
            return;
        }

        loadingLiveData.setValue(true);
        List<CelestialObject> results = new ArrayList<>();

        try {
            // Search stars
            if (starRepository != null) {
                List<StarData> stars = starRepository.searchStars(query);
                results.addAll(stars);
                Log.d(TAG, "Found " + stars.size() + " stars matching: " + query);
            }

            // Search constellations
            if (constellationRepository != null) {
                List<ConstellationData> constellations = constellationRepository.searchConstellations(query);
                // Convert constellation data to celestial objects for unified results
                for (ConstellationData constellation : constellations) {
                    // Create a simple wrapper object for the constellation
                    results.add(createConstellationWrapper(constellation));
                }
                Log.d(TAG, "Found " + constellations.size() + " constellations matching: " + query);
            }

            searchResultsLiveData.setValue(results);
        } catch (Exception e) {
            Log.e(TAG, "Search failed", e);
            errorMessageLiveData.setValue("Search failed: " + e.getMessage());
        } finally {
            loadingLiveData.setValue(false);
        }
    }

    /**
     * Creates a CelestialObject wrapper for a constellation.
     */
    @NonNull
    private CelestialObject createConstellationWrapper(@NonNull ConstellationData constellation) {
        float ra = 0f;
        float dec = 0f;
        if (constellation.getCenterPoint() != null) {
            ra = constellation.getCenterPoint().getRa();
            dec = constellation.getCenterPoint().getDec();
        }

        return new CelestialObject.Builder<>()
                .setId("constellation_" + constellation.getId())
                .setName(constellation.getName())
                .setRa(ra)
                .setDec(dec)
                .build();
    }

    /**
     * Toggles night mode on/off.
     *
     * <p>Night mode applies a red filter to preserve night vision
     * when viewing the sky.</p>
     */
    public void toggleNightMode() {
        Boolean current = nightModeLiveData.getValue();
        boolean newValue = current == null || !current;
        nightModeLiveData.setValue(newValue);
        Log.d(TAG, "Night mode toggled: " + newValue);
    }

    /**
     * Sets the night mode state.
     *
     * @param enabled true to enable night mode
     */
    public void setNightMode(boolean enabled) {
        nightModeLiveData.setValue(enabled);
    }

    /**
     * Toggles the visibility of a layer.
     *
     * @param layerId The ID of the layer to toggle
     */
    public void toggleLayer(@NonNull String layerId) {
        Map<String, Boolean> visibility = layerVisibilityLiveData.getValue();
        if (visibility == null) {
            visibility = new HashMap<>();
        }

        Boolean currentState = visibility.get(layerId);
        boolean newState = currentState == null || !currentState;
        visibility.put(layerId, newState);
        layerVisibilityLiveData.setValue(visibility);

        // Update the actual layer
        Layer layer = layers.get(layerId);
        if (layer != null) {
            layer.setVisible(newState);
            Log.d(TAG, "Layer " + layerId + " visibility: " + newState);
        }
    }

    /**
     * Sets the visibility of a specific layer.
     *
     * @param layerId The ID of the layer
     * @param visible true to show the layer
     */
    public void setLayerVisible(@NonNull String layerId, boolean visible) {
        Map<String, Boolean> visibility = layerVisibilityLiveData.getValue();
        if (visibility == null) {
            visibility = new HashMap<>();
        }

        visibility.put(layerId, visible);
        layerVisibilityLiveData.setValue(visibility);

        Layer layer = layers.get(layerId);
        if (layer != null) {
            layer.setVisible(visible);
        }
    }

    /**
     * Sets the field of view (zoom level).
     *
     * @param fov Field of view in degrees (clamped to valid range)
     */
    public void setFieldOfView(float fov) {
        float clampedFov = Math.max(MIN_FOV, Math.min(MAX_FOV, fov));
        fieldOfViewLiveData.setValue(clampedFov);

        if (astronomerModel != null) {
            astronomerModel.setFieldOfView(clampedFov);
        }
    }

    /**
     * Zooms in by reducing the field of view.
     *
     * @param factor The zoom factor (e.g., 0.8 for 20% zoom in)
     */
    public void zoomIn(float factor) {
        Float currentFov = fieldOfViewLiveData.getValue();
        if (currentFov != null) {
            setFieldOfView(currentFov * factor);
        }
    }

    /**
     * Zooms out by increasing the field of view.
     *
     * @param factor The zoom factor (e.g., 1.2 for 20% zoom out)
     */
    public void zoomOut(float factor) {
        Float currentFov = fieldOfViewLiveData.getValue();
        if (currentFov != null) {
            setFieldOfView(currentFov * factor);
        }
    }

    /**
     * Centers the view on a specific celestial object.
     *
     * @param object The object to center on
     */
    public void centerOnObject(@NonNull CelestialObject object) {
        if (astronomerModel != null) {
            // This would require AstronomerModel to support manual pointing
            // For now, just select the object
            selectObject(object);
            Log.d(TAG, "Centering on: " + object.getName());
        }
    }

    // ==================== Pointing Listener Implementation ====================

    @Override
    public void onPointingChanged(Pointing pointing) {
        pointingLiveData.postValue(pointing);
        updateVisibleObjects(pointing);
    }

    /**
     * Updates the lists of visible stars and constellations based on current pointing.
     *
     * @param pointing The current pointing direction
     */
    private void updateVisibleObjects(@NonNull Pointing pointing) {
        // This would filter objects based on the current field of view
        // For now, we'll use a simple approach - in a real implementation,
        // this would use angular distance calculations

        Float fov = fieldOfViewLiveData.getValue();
        if (fov == null) {
            fov = DEFAULT_FOV;
        }

        // Update visible stars
        if (starRepository != null) {
            List<StarData> allStars = starRepository.getAllStars();
            List<StarData> visible = filterVisibleStars(allStars, pointing, fov);
            visibleStarsLiveData.postValue(visible);
        }

        // Update visible constellations
        if (constellationRepository != null) {
            List<ConstellationData> allConstellations = constellationRepository.getAllConstellations();
            List<ConstellationData> visible = filterVisibleConstellations(allConstellations, pointing, fov);
            visibleConstellationsLiveData.postValue(visible);
        }
    }

    /**
     * Filters stars to those visible in the current field of view.
     */
    @NonNull
    private List<StarData> filterVisibleStars(@NonNull List<StarData> stars,
                                               @NonNull Pointing pointing,
                                               float fov) {
        List<StarData> visible = new ArrayList<>();
        float halfFov = fov / 2f;

        for (StarData star : stars) {
            float angularDistance = calculateAngularDistance(
                    pointing.getRightAscension(), pointing.getDeclination(),
                    star.getRa(), star.getDec());

            if (angularDistance <= halfFov) {
                visible.add(star);
            }
        }

        return visible;
    }

    /**
     * Filters constellations to those visible in the current field of view.
     */
    @NonNull
    private List<ConstellationData> filterVisibleConstellations(
            @NonNull List<ConstellationData> constellations,
            @NonNull Pointing pointing,
            float fov) {
        List<ConstellationData> visible = new ArrayList<>();
        float halfFov = fov / 2f;

        for (ConstellationData constellation : constellations) {
            if (constellation.getCenterPoint() != null) {
                float angularDistance = calculateAngularDistance(
                        pointing.getRightAscension(), pointing.getDeclination(),
                        constellation.getCenterPoint().getRa(),
                        constellation.getCenterPoint().getDec());

                // Use a larger radius for constellations as they span multiple degrees
                if (angularDistance <= halfFov + 30f) {
                    visible.add(constellation);
                }
            }
        }

        return visible;
    }

    /**
     * Calculates the angular distance between two celestial coordinates.
     *
     * @param ra1  Right Ascension of first point (degrees)
     * @param dec1 Declination of first point (degrees)
     * @param ra2  Right Ascension of second point (degrees)
     * @param dec2 Declination of second point (degrees)
     * @return Angular distance in degrees
     */
    private float calculateAngularDistance(float ra1, float dec1, float ra2, float dec2) {
        double ra1Rad = Math.toRadians(ra1);
        double dec1Rad = Math.toRadians(dec1);
        double ra2Rad = Math.toRadians(ra2);
        double dec2Rad = Math.toRadians(dec2);

        double cosAngle = Math.sin(dec1Rad) * Math.sin(dec2Rad) +
                Math.cos(dec1Rad) * Math.cos(dec2Rad) * Math.cos(ra1Rad - ra2Rad);

        // Clamp to [-1, 1] to handle floating point errors
        cosAngle = Math.max(-1.0, Math.min(1.0, cosAngle));

        return (float) Math.toDegrees(Math.acos(cosAngle));
    }

    // ==================== Lifecycle ====================

    @Override
    protected void onCleared() {
        super.onCleared();

        // Remove pointing listener
        if (astronomerModel != null) {
            astronomerModel.removePointingListener(this);
        }

        // Clear layer references
        layers.clear();

        Log.d(TAG, "SkyMapViewModel cleared");
    }

    /**
     * Clears any displayed error message.
     */
    public void clearError() {
        errorMessageLiveData.setValue(null);
    }
}
