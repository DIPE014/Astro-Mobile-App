package com.astro.app.ui.skymap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.astro.app.AstroApplication;
import com.astro.app.R;
import com.astro.app.core.control.AstronomerModel;
import com.astro.app.core.control.LocationController;
import com.astro.app.core.control.SensorController;
import com.astro.app.core.control.SolarSystemBody;
import com.astro.app.core.control.TimeTravelClock;
import com.astro.app.core.control.space.Universe;
import com.astro.app.core.math.RaDec;
import com.astro.app.ui.timetravel.TimeTravelDialogFragment;
import com.astro.app.core.layers.ConstellationsLayer;
import com.astro.app.core.layers.PlanetsLayer;
import com.astro.app.core.layers.GridLayer;
import com.astro.app.core.layers.StarsLayer;
import com.astro.app.core.math.LatLong;
import com.astro.app.core.renderer.SkyCanvasView;
import com.astro.app.core.renderer.SkyGLSurfaceView;
import com.astro.app.core.renderer.SkyRenderer;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.StarRepository;
import com.astro.app.ui.search.SearchActivity;
import com.astro.app.ui.settings.SettingsActivity;
import com.astro.app.ui.settings.SettingsViewModel;
import com.astro.app.ui.starinfo.StarInfoActivity;
import com.astro.app.search.SearchArrowView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

/**
 * Main AR sky view activity.
 *
 * <p>This activity provides two viewing modes:
 * <ul>
 *   <li><b>AR Mode</b>: Camera preview with star overlay</li>
 *   <li><b>Map Mode</b>: Full-screen star map without camera</li>
 * </ul>
 * </p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>CameraX preview integration</li>
 *   <li>OpenGL sky overlay with stars and constellations</li>
 *   <li>Tap-to-select celestial objects</li>
 *   <li>Device sensor-based orientation tracking</li>
 *   <li>Toggle between AR and map modes</li>
 * </ul>
 */
public class SkyMapActivity extends AppCompatActivity {

    private static final String TAG = "SkyMapActivity";

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    // ViewModels
    private SkyMapViewModel viewModel;
    private SettingsViewModel settingsViewModel;

    // Injected dependencies
    @Inject
    StarRepository starRepository;

    @Inject
    ConstellationRepository constellationRepository;

    @Inject
    SensorController sensorController;

    @Inject
    AstronomerModel astronomerModel;

    @Inject
    LocationController locationController;

    @Inject
    TimeTravelClock timeTravelClock;

    @Inject
    Universe universe;

    // Camera components
    private CameraManager cameraManager;
    private CameraPermissionHandler permissionHandler;
    private AROverlayManager arOverlayManager;

    // Views
    private FrameLayout cameraPreviewContainer;
    private FrameLayout skyOverlayContainer;
    private PreviewView cameraPreview;
    private SkyGLSurfaceView skyGLSurfaceView;
    private SkyCanvasView skyCanvasView;
    private MaterialButton btnArToggle;
    private MaterialCardView infoPanel;
    private TextView tvInfoPanelName;
    private TextView tvInfoPanelType;
    private TextView tvInfoPanelMagnitude;
    private TextView tvInfoPanelRA;
    private TextView tvInfoPanelDec;
    private FrameLayout loadingOverlay;
    private View gpsIndicator;
    private ImageView ivGpsIcon;
    private TextView tvGpsStatus;

    // Layers
    private StarsLayer starsLayer;
    private ConstellationsLayer constellationsLayer;
    private GridLayer gridLayer;
    private PlanetsLayer planetsLayer;

    // Search
    private SearchArrowView searchArrowView;
    private String searchTargetName;
    private float searchTargetRa;
    private float searchTargetDec;

    // State
    // Default to MAP mode (AR disabled) for better emulator compatibility
    // On devices without camera or on emulator, this ensures stars are visible
    private boolean isARModeEnabled = false;
    private boolean isConstellationsEnabled = true;
    private boolean isGridEnabled = false;
    private boolean isPlanetsEnabled = false;
    @Nullable
    private StarData selectedStar;

    // GPS state
    private boolean isGpsEnabled = false;
    private float currentLatitude = 40.7128f;  // Default: New York
    private float currentLongitude = -74.0060f;
    private static final float DEFAULT_LATITUDE = 40.7128f;
    private static final float DEFAULT_LONGITUDE = -74.0060f;

    // Permission launcher
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (permissionHandler != null) {
                    permissionHandler.handlePermissionResult(isGranted);
                }
            });

    // Search activity launcher
    private final ActivityResultLauncher<Intent> searchActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleSearchResult(result.getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on while using the sky map
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_sky_map);

        // Diagnostic toast to verify activity is running
        android.widget.Toast.makeText(this, "SkyMap Started", android.widget.Toast.LENGTH_LONG).show();

        // Initialize ViewModels
        viewModel = new ViewModelProvider(this).get(SkyMapViewModel.class);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // Inject dependencies
        ((AstroApplication) getApplication()).getAppComponent().inject(this);

        initializeViews();
        initializeManagers();
        setupViewModel();
        setupSensorController();
        setupLocationController();
        observeViewModel();
        setupClickListeners();

        if (hasPermissions()) {
            initializeSkyMap();
        } else {
            requestPermissions();
        }
    }

    /**
     * Sets up the ViewModel with dependencies.
     */
    private void setupViewModel() {
        // Set dependencies on the ViewModel
        if (astronomerModel != null) {
            viewModel.setAstronomerModel(astronomerModel);
        }
        if (starRepository != null) {
            viewModel.setStarRepository(starRepository);
        }
        if (constellationRepository != null) {
            viewModel.setConstellationRepository(constellationRepository);
        }
    }

    /**
     * Sets up the SensorController to receive orientation updates.
     * Converts rotation vector to azimuth/altitude and updates the sky view.
     */
    private void setupSensorController() {
        if (sensorController == null) {
            Log.w(TAG, "SensorController is null, compass tracking unavailable");
            return;
        }

        sensorController.setListener(rotationVector -> {
            // Convert rotation vector to rotation matrix
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

            // Remap coordinate system for device orientation (phone held upright looking at sky)
            float[] remappedMatrix = new float[9];
            SensorManager.remapCoordinateSystem(rotationMatrix,
                    SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix);

            // Get orientation angles from remapped rotation matrix
            float[] orientationAngles = new float[3];
            SensorManager.getOrientation(remappedMatrix, orientationAngles);

            // Convert radians to degrees
            // orientationAngles[0] = azimuth (rotation around Z axis, -pi to pi)
            // orientationAngles[1] = pitch (rotation around X axis, -pi/2 to pi/2)
            // orientationAngles[2] = roll (rotation around Y axis, -pi to pi)
            float azimuth = (float) Math.toDegrees(orientationAngles[0]);
            float pitch = (float) Math.toDegrees(orientationAngles[1]);

            // Normalize azimuth to 0-360 range (0 = North, 90 = East)
            if (azimuth < 0) {
                azimuth += 360f;
            }

            // Convert pitch to altitude (0 = horizon, 90 = zenith)
            // When phone is held upright facing forward, pitch is ~0
            // When tilted up toward sky, pitch becomes negative
            // So altitude = -pitch, clamped to 0-90 range
            float altitude = -pitch;
            altitude = Math.max(0f, Math.min(90f, altitude));

            // Update the sky canvas view with new orientation
            final float finalAzimuth = azimuth;
            final float finalAltitude = altitude;
            runOnUiThread(() -> {
                if (skyCanvasView != null) {
                    skyCanvasView.setOrientation(finalAzimuth, finalAltitude);
                }

                // Update search arrow if active
                if (searchArrowView != null && searchArrowView.isActive()) {
                    updateSearchArrow();
                }
            });
        });

        Log.d(TAG, "SensorController listener set up for compass tracking");
    }

    /**
     * Sets up the LocationController to receive GPS updates.
     */
    private void setupLocationController() {
        if (locationController == null) {
            Log.w(TAG, "LocationController is null, GPS tracking unavailable");
            updateGpsIndicator(false, getString(R.string.gps_unavailable));
            return;
        }

        locationController.setListener((latitude, longitude) -> {
            Log.d(TAG, "GPS: Location updated - lat=" + latitude + ", lon=" + longitude);

            // Store current location
            currentLatitude = latitude;
            currentLongitude = longitude;
            isGpsEnabled = true;

            // Update the sky canvas view with new location
            runOnUiThread(() -> {
                if (skyCanvasView != null) {
                    skyCanvasView.setObserverLocation(latitude, longitude);
                }

                // Update astronomer model with real location
                if (astronomerModel != null) {
                    astronomerModel.setLocation(new LatLong(latitude, longitude));
                }

                // Update GPS indicator to show connected status
                updateGpsIndicator(true, String.format("%.2f°, %.2f°", latitude, longitude));
            });
        });

        // Initial GPS indicator state
        updateGpsIndicator(false, getString(R.string.gps_searching));
    }

    /**
     * Updates the GPS status indicator in the UI.
     *
     * @param connected Whether GPS has obtained a fix
     * @param statusText Text to display (coordinates or status message)
     */
    private void updateGpsIndicator(boolean connected, String statusText) {
        if (ivGpsIcon != null) {
            int iconColor = connected ? R.color.gps_connected : R.color.gps_searching;
            ivGpsIcon.setColorFilter(ContextCompat.getColor(this, iconColor));
        }
        if (tvGpsStatus != null) {
            tvGpsStatus.setText(statusText);
        }
    }

    /**
     * Observes LiveData from ViewModels.
     */
    private void observeViewModel() {
        // Observe night mode from settings
        settingsViewModel.getNightMode().observe(this, nightMode -> {
            if (nightMode != null) {
                viewModel.setNightMode(nightMode);
            }
        });

        // Observe settings changes
        settingsViewModel.getSettingsChanged().observe(this, changed -> {
            if (changed != null && changed) {
                applySettings();
                settingsViewModel.clearSettingsChangedFlag();
            }
        });

        // Observe magnitude limit changes to reload star data
        settingsViewModel.getMagnitudeLimit().observe(this, magnitude -> {
            if (magnitude != null && skyCanvasView != null) {
                Log.d(TAG, "Magnitude limit changed to: " + magnitude);
                loadStarDataForCanvas();
            }
        });

        // Observe ViewModel night mode state
        viewModel.getNightMode().observe(this, this::onNightModeChanged);

        // Observe selected object
        viewModel.getSelectedObject().observe(this, object -> {
            if (object instanceof StarData) {
                showStarInfo((StarData) object);
            } else if (object == null) {
                hideInfoPanel();
            }
        });

        // Observe errors
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        // Observe loading state
        viewModel.isLoading().observe(this, loading -> {
            showLoading(loading != null && loading);
        });
    }

    /**
     * Called when night mode state changes from ViewModel.
     */
    private void onNightModeChanged(@Nullable Boolean nightMode) {
        if (nightMode == null) return;

        if (skyGLSurfaceView != null) {
            skyGLSurfaceView.setNightMode(nightMode);
        }

        // Also update Canvas-based renderer
        if (skyCanvasView != null) {
            skyCanvasView.setNightMode(nightMode);
        }

        MaterialButton btnNightMode = findViewById(R.id.btnNightMode);
        if (btnNightMode != null) {
            int tintColor = nightMode ? R.color.night_mode_red : R.color.icon_primary;
            btnNightMode.setIconTint(ContextCompat.getColorStateList(this, tintColor));
        }
    }

    /**
     * Applies current settings from SettingsViewModel.
     */
    private void applySettings() {
        // Apply to StarsLayer
        if (starsLayer != null) {
            settingsViewModel.applyToStarsLayer(starsLayer);
        }

        // Apply to ConstellationsLayer
        if (constellationsLayer != null) {
            settingsViewModel.applyToConstellationsLayer(constellationsLayer);
        }

        // Apply to AstronomerModel
        if (astronomerModel != null) {
            settingsViewModel.applyToAstronomerModel(astronomerModel);
        }

        // Trigger layer redraw
        if (skyGLSurfaceView != null) {
            skyGLSurfaceView.requestLayerUpdate();
        }

        Log.d(TAG, "Settings applied");
    }

    /**
     * Initializes view references.
     */
    private void initializeViews() {
        cameraPreviewContainer = findViewById(R.id.cameraPreview);
        skyOverlayContainer = findViewById(R.id.skyOverlayContainer);
        infoPanel = findViewById(R.id.infoPanel);
        tvInfoPanelName = findViewById(R.id.tvInfoPanelName);
        tvInfoPanelType = findViewById(R.id.tvInfoPanelType);
        tvInfoPanelMagnitude = findViewById(R.id.tvInfoPanelMagnitude);
        tvInfoPanelRA = findViewById(R.id.tvInfoPanelRA);
        tvInfoPanelDec = findViewById(R.id.tvInfoPanelDec);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        btnArToggle = findViewById(R.id.btnArToggle);

        // GPS indicator views
        gpsIndicator = findViewById(R.id.gpsIndicator);
        ivGpsIcon = findViewById(R.id.ivGpsIcon);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);

        // Search arrow view
        searchArrowView = findViewById(R.id.searchArrow);

        // Create PreviewView programmatically for camera
        cameraPreview = new PreviewView(this);
        cameraPreview.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        cameraPreviewContainer.addView(cameraPreview);

        // ============================================================
        // Use Canvas-based renderer (works without OpenGL)
        // This avoids OpenGL issues on emulators
        // ============================================================
        skyCanvasView = new SkyCanvasView(this);
        FrameLayout container = findViewById(R.id.skyOverlayContainer);
        container.addView(skyCanvasView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));

        Log.d(TAG, "SkyCanvasView created - Canvas2D rendering enabled");

        // Set up star selection listener to open StarInfoActivity when a star is tapped
        skyCanvasView.setOnStarSelectedListener(star -> {
            // Debug toast to confirm star was tapped
            Toast.makeText(this, "Selected: " + star.getName(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Star selected: " + star.getName() + " (id=" + star.getId() + ")");

            Intent intent = new Intent(this, StarInfoActivity.class);
            intent.putExtra(StarInfoActivity.EXTRA_STAR_ID, star.getId());
            intent.putExtra(StarInfoActivity.EXTRA_STAR_NAME, star.getName());
            intent.putExtra(StarInfoActivity.EXTRA_STAR_RA, star.getRa());
            intent.putExtra(StarInfoActivity.EXTRA_STAR_DEC, star.getDec());
            intent.putExtra(StarInfoActivity.EXTRA_STAR_MAGNITUDE, star.getMagnitude());
            startActivity(intent);
        });

        // Create a dummy SkyGLSurfaceView for compatibility (won't be displayed)
        // This prevents null pointer exceptions in other parts of the code
        SkyRenderer renderer = new SkyRenderer();
        skyGLSurfaceView = new SkyGLSurfaceView(this, renderer);
    }

    /**
     * Initializes camera and AR managers.
     */
    private void initializeManagers() {
        // Initialize camera manager
        cameraManager = new CameraManager(this);
        cameraManager.setPreviewView(cameraPreview);

        // Initialize permission handler
        permissionHandler = new CameraPermissionHandler(this);
        permissionHandler.setCallback(new CameraPermissionHandler.PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                startCameraPreview();
            }

            @Override
            public void onPermissionDenied(boolean shouldShowRationale) {
                if (shouldShowRationale) {
                    permissionHandler.showRationaleDialog(cameraPermissionLauncher);
                } else {
                    // Permission permanently denied - show settings dialog
                    permissionHandler.showSettingsDialog();
                    // Fall back to map-only mode
                    setARModeEnabled(false);
                }
            }
        });

        // Initialize AR overlay manager
        arOverlayManager = new AROverlayManager(starRepository);
        arOverlayManager.setSkyView(skyGLSurfaceView);
        arOverlayManager.setObjectSelectionCallback((star, screenX, screenY) -> {
            if (star != null) {
                viewModel.selectObject(star);
            } else {
                viewModel.clearSelection();
            }
        });
    }

    /**
     * Sets up click listeners for UI elements.
     */
    private void setupClickListeners() {
        // Back button
        MaterialButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // AR toggle button
        if (btnArToggle != null) {
            btnArToggle.setOnClickListener(v -> toggleARMode());
        }

        // Settings button
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings());
        }

        // Night mode toggle
        MaterialButton btnNightMode = findViewById(R.id.btnNightMode);
        if (btnNightMode != null) {
            btnNightMode.setOnClickListener(v -> viewModel.toggleNightMode());
        }

        // Constellations toggle
        View btnConstellations = findViewById(R.id.btnConstellations);
        if (btnConstellations != null) {
            btnConstellations.setOnClickListener(v -> toggleConstellations());
        }

        // Grid toggle
        View btnGrid = findViewById(R.id.btnGrid);
        if (btnGrid != null) {
            btnGrid.setOnClickListener(v -> toggleGrid());
        }

        // Time Travel button
        View btnTimeTravel = findViewById(R.id.btnTimeTravel);
        if (btnTimeTravel != null) {
            btnTimeTravel.setOnClickListener(v -> showTimeTravelDialog());
        }

        // Planets toggle button
        View btnPlanets = findViewById(R.id.btnPlanets);
        if (btnPlanets != null) {
            btnPlanets.setOnClickListener(v -> togglePlanets());
        }

        // Search FAB
        View fabSearch = findViewById(R.id.fabSearch);
        if (fabSearch != null) {
            fabSearch.setOnClickListener(v -> openSearch());
        }

        // Close info panel
        View btnCloseInfoPanel = findViewById(R.id.btnCloseInfoPanel);
        if (btnCloseInfoPanel != null) {
            btnCloseInfoPanel.setOnClickListener(v -> viewModel.clearSelection());
        }

        // Info panel details button
        MaterialButton btnDetails = findViewById(R.id.btnInfoPanelDetails);
        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> {
                if (selectedStar != null) {
                    openStarDetails(selectedStar);
                }
            });
        }

        // Set up tap gesture on sky view
        skyGLSurfaceView.setGestureListener(new SkyGLSurfaceView.SimpleGestureListener() {
            @Override
            public void onTap(float x, float y) {
                arOverlayManager.handleTap(x, y);
            }

            @Override
            public void onOrientationChanged(float azimuth, float elevation) {
                arOverlayManager.setOrientation(azimuth, elevation);
            }
        });
    }

    /**
     * Checks if all required permissions are granted.
     */
    private boolean hasPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Requests required permissions.
     */
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check individual permission results
            boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
            boolean locationGranted = hasLocationPermission();

            // Handle location permission result
            if (!locationGranted) {
                // Location permission denied - use default location
                Toast.makeText(this, R.string.permission_location_denied, Toast.LENGTH_SHORT).show();
                currentLatitude = DEFAULT_LATITUDE;
                currentLongitude = DEFAULT_LONGITUDE;
                updateGpsIndicator(false, getString(R.string.gps_disabled));
                Log.d(TAG, "GPS: Permission denied, using default location");
            } else {
                // Start GPS tracking
                if (locationController != null) {
                    locationController.start();
                    updateGpsIndicator(false, getString(R.string.gps_searching));
                }
            }

            // Handle camera permission result
            if (cameraGranted || locationGranted) {
                // At least one useful permission granted, initialize sky map
                if (!cameraGranted) {
                    Toast.makeText(this, R.string.permission_camera_denied_fallback, Toast.LENGTH_LONG).show();
                    setARModeEnabled(false);
                    initializeSkyMapOnly();
                } else {
                    initializeSkyMap();
                }
            } else {
                // Neither permission granted - still show sky map in basic mode
                Toast.makeText(this, R.string.permission_camera_denied_fallback, Toast.LENGTH_LONG).show();
                setARModeEnabled(false);
                initializeSkyMapOnly();
            }
        }
    }

    /**
     * Initializes the sky map with all features.
     */
    private void initializeSkyMap() {
        showLoading(true);

        // Initialize layers
        initializeLayers();

        // Load real star data into Canvas view
        loadStarDataForCanvas();

        // Calibrate AR overlay
        arOverlayManager.calibrate(
                cameraManager.getHorizontalFov(),
                cameraManager.getVerticalFov()
        );

        // Start camera if in AR mode, otherwise set up map-only mode
        if (isARModeEnabled) {
            startCameraPreview();
        } else {
            arOverlayManager.setARModeEnabled(false);
            // Hide camera preview container for map-only mode
            cameraPreviewContainer.setVisibility(View.GONE);
        }

        // Update AR toggle button to reflect current state
        updateARToggleButton();

        showLoading(false);
    }

    /**
     * Initializes the sky map without camera (map-only mode).
     */
    private void initializeSkyMapOnly() {
        showLoading(true);

        // Initialize layers
        initializeLayers();

        // Load real star data into Canvas view
        loadStarDataForCanvas();

        // Set map-only mode
        arOverlayManager.setARModeEnabled(false);
        arOverlayManager.calibrateDefault();

        // Hide camera preview
        cameraPreviewContainer.setVisibility(View.GONE);

        showLoading(false);
    }

    /**
     * Loads star data from the repository and passes it to the Canvas view.
     */
    private void loadStarDataForCanvas() {
        if (skyCanvasView == null) {
            Log.e(TAG, "STARS: Cannot load star data - skyCanvasView is null");
            return;
        }
        if (starRepository == null) {
            Log.e(TAG, "STARS: Cannot load star data - starRepository is null");
            return;
        }

        // Get magnitude limit from settings (default 6.5 for naked eye visibility)
        Float magnitudeLimit = settingsViewModel.getMagnitudeLimit().getValue();
        if (magnitudeLimit == null) {
            magnitudeLimit = 6.5f;
        }

        // Load stars up to the configured magnitude limit
        List<StarData> visibleStars = starRepository.getStarsByMagnitude(magnitudeLimit);
        Log.d(TAG, "STARS: Loaded " + visibleStars.size() + " stars with magnitude <= " + magnitudeLimit);

        if (visibleStars.isEmpty()) {
            Log.e(TAG, "STARS: No stars loaded! Check protobuf parsing and assets.");
            // Try loading all stars as a fallback
            List<StarData> allStars = starRepository.getAllStars();
            Log.d(TAG, "STARS: Total stars in repository: " + allStars.size());
        }

        // Pass star data to the canvas view
        skyCanvasView.setStarData(visibleStars);
        Log.d(TAG, "STARS: Star data passed to canvas view");

        // Load and pass constellation data
        if (constellationRepository != null) {
            List<com.astro.app.data.model.ConstellationData> constellations =
                    constellationRepository.getAllConstellations();
            skyCanvasView.setConstellationData(constellations);
            skyCanvasView.setConstellationsVisible(isConstellationsEnabled);
            Log.d(TAG, "CONSTELLATIONS: Loaded " + constellations.size() + " constellations for canvas");
        }

        // Set observer location - use current GPS coordinates if available, otherwise default
        // GPS updates will automatically update this via the LocationListener
        skyCanvasView.setObserverLocation(currentLatitude, currentLongitude);
        Log.d(TAG, "STARS: Observer location set to " + currentLatitude + ", " + currentLongitude);

        // Set default view direction (looking north at 45 degrees altitude)
        skyCanvasView.setOrientation(0f, 45f);
    }

    /**
     * Initializes the rendering layers.
     */
    private void initializeLayers() {
        SkyRenderer renderer = skyGLSurfaceView.getSkyRenderer();

        // Create adapter for StarsLayer.StarRepository interface
        StarsLayer.StarRepository starsLayerRepository = new StarsLayer.StarRepository() {
            @NonNull
            @Override
            public List<StarData> getStars() {
                return starRepository.getAllStars();
            }

            @NonNull
            @Override
            public List<StarData> getStarsBrighterThan(float maxMagnitude) {
                return starRepository.getStarsByMagnitude(maxMagnitude);
            }

            @Nullable
            @Override
            public StarData findById(@NonNull String starId) {
                return starRepository.getStarById(starId);
            }

            @NonNull
            @Override
            public List<StarData> findByName(@NonNull String name) {
                return starRepository.searchStars(name);
            }
        };

        // Create adapter for ConstellationsLayer.ConstellationRepository interface
        ConstellationsLayer.ConstellationRepository constellationsLayerRepository =
                new ConstellationsLayer.ConstellationRepository() {
            @NonNull
            @Override
            public List<com.astro.app.data.model.ConstellationData> getConstellations() {
                return constellationRepository.getAllConstellations();
            }

            @Nullable
            @Override
            public com.astro.app.data.model.ConstellationData findById(@NonNull String constellationId) {
                return constellationRepository.getConstellationById(constellationId);
            }

            @NonNull
            @Override
            public List<com.astro.app.data.model.ConstellationData> findByName(@NonNull String name) {
                return constellationRepository.searchConstellations(name);
            }

            @NonNull
            @Override
            public java.util.Map<String, com.astro.app.data.model.GeocentricCoords> getStarCoordinates(
                    @NonNull com.astro.app.data.model.ConstellationData constellation) {
                java.util.Map<String, com.astro.app.data.model.GeocentricCoords> coords =
                        new java.util.HashMap<>();
                for (String starId : constellation.getStarIds()) {
                    StarData star = starRepository.getStarById(starId);
                    if (star != null) {
                        coords.put(starId, com.astro.app.data.model.GeocentricCoords.fromDegrees(
                                star.getRa(), star.getDec()));
                    }
                }
                return coords;
            }
        };

        // Create layers
        starsLayer = new StarsLayer(starsLayerRepository);
        constellationsLayer = new ConstellationsLayer(constellationsLayerRepository);
        gridLayer = new GridLayer();
        planetsLayer = new PlanetsLayer(universe);

        // Configure initial visibility
        starsLayer.setVisible(true);
        constellationsLayer.setVisible(isConstellationsEnabled);
        gridLayer.setVisible(isGridEnabled);
        planetsLayer.setVisible(isPlanetsEnabled);

        // Add layers to renderer
        List<com.astro.app.core.layers.Layer> layers = new ArrayList<>();
        layers.add(gridLayer);
        layers.add(constellationsLayer);
        layers.add(starsLayer);
        layers.add(planetsLayer);

        renderer.setLayers(layers);

        // Update layer data
        starsLayer.initialize();
        constellationsLayer.initialize();
        gridLayer.initialize();
        planetsLayer.initialize();

        renderer.requestLayerUpdate();

        // Debug logging to verify layer initialization
        Log.d(TAG, "Layers initialized - Stars: " + starsLayer.getPoints().size() +
                " points, Constellations: " + constellationsLayer.getLines().size() +
                " lines, Grid visible: " + gridLayer.isVisible());
    }

    /**
     * Starts the camera preview.
     */
    private void startCameraPreview() {
        if (!permissionHandler.hasCameraPermission()) {
            permissionHandler.checkAndRequestPermission(cameraPermissionLauncher);
            return;
        }

        cameraManager.startCamera(this, new CameraManager.CameraCallback() {
            @Override
            public void onCameraStarted() {
                Log.d(TAG, "Camera started successfully");
                cameraPreviewContainer.setVisibility(View.VISIBLE);
                arOverlayManager.setARModeEnabled(true);
                updateARToggleButton();
            }

            @Override
            public void onCameraError(String message) {
                Log.e(TAG, "Camera error: " + message);
                Toast.makeText(SkyMapActivity.this,
                        getString(R.string.camera_error, message),
                        Toast.LENGTH_SHORT).show();
                // Fall back to map-only mode
                setARModeEnabled(false);
            }
        });
    }

    /**
     * Stops the camera preview.
     */
    private void stopCameraPreview() {
        cameraManager.stopCamera();
        cameraPreviewContainer.setVisibility(View.GONE);
    }

    /**
     * Toggles between AR mode and map-only mode.
     */
    private void toggleARMode() {
        setARModeEnabled(!isARModeEnabled);
    }

    /**
     * Sets the AR mode state.
     *
     * @param enabled true for AR mode, false for map-only mode
     */
    private void setARModeEnabled(boolean enabled) {
        isARModeEnabled = enabled;

        // Update GL surface transparency based on mode
        if (skyGLSurfaceView != null) {
            if (enabled) {
                // AR mode: transparent surface to show camera behind
                skyGLSurfaceView.setZOrderOnTop(true);
                skyGLSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
            } else {
                // MAP mode: opaque surface to show dark blue background
                skyGLSurfaceView.setZOrderOnTop(false);
                skyGLSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.OPAQUE);
            }
        }

        if (enabled) {
            if (permissionHandler.hasCameraPermission()) {
                startCameraPreview();
            } else {
                permissionHandler.checkAndRequestPermission(cameraPermissionLauncher);
            }
        } else {
            stopCameraPreview();
            arOverlayManager.setARModeEnabled(false);
        }

        updateARToggleButton();
    }

    /**
     * Updates the AR toggle button appearance.
     */
    private void updateARToggleButton() {
        if (btnArToggle != null) {
            if (isARModeEnabled) {
                btnArToggle.setIconResource(android.R.drawable.ic_menu_camera);
                btnArToggle.setIconTint(ContextCompat.getColorStateList(this, R.color.icon_primary));
            } else {
                btnArToggle.setIconResource(android.R.drawable.ic_menu_gallery);
                btnArToggle.setIconTint(ContextCompat.getColorStateList(this, R.color.icon_inactive));
            }
        }
    }

    /**
     * Opens the settings activity.
     */
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Toggles constellation lines visibility.
     */
    private void toggleConstellations() {
        isConstellationsEnabled = !isConstellationsEnabled;
        if (constellationsLayer != null) {
            constellationsLayer.setVisible(isConstellationsEnabled);
            skyGLSurfaceView.requestLayerUpdate();
        }

        // Update Canvas-based view
        if (skyCanvasView != null) {
            skyCanvasView.setConstellationsVisible(isConstellationsEnabled);
        }

        ImageView ivConstellations = findViewById(R.id.ivConstellations);
        if (ivConstellations != null) {
            int tintColor = isConstellationsEnabled ? R.color.icon_primary : R.color.icon_inactive;
            ivConstellations.setColorFilter(ContextCompat.getColor(this, tintColor));
        }
    }

    /**
     * Toggles coordinate grid visibility.
     */
    private void toggleGrid() {
        isGridEnabled = !isGridEnabled;
        if (gridLayer != null) {
            gridLayer.setVisible(isGridEnabled);
            skyGLSurfaceView.requestLayerUpdate();
        }

        // Update Canvas-based view
        if (skyCanvasView != null) {
            skyCanvasView.setGridVisible(isGridEnabled);
        }

        ImageView ivGrid = findViewById(R.id.ivGrid);
        if (ivGrid != null) {
            int tintColor = isGridEnabled ? R.color.icon_primary : R.color.icon_inactive;
            ivGrid.setColorFilter(ContextCompat.getColor(this, tintColor));
        }
    }

    /**
     * Shows the time travel dialog to select a different date/time.
     */
    private void showTimeTravelDialog() {
        // Pass current time travel time (or current time if not in time travel mode)
        long currentTime = (timeTravelClock != null)
                ? timeTravelClock.getCurrentTimeMillis()
                : System.currentTimeMillis();
        TimeTravelDialogFragment dialog = TimeTravelDialogFragment.newInstance(currentTime);
        dialog.setCallback(new TimeTravelDialogFragment.TimeTravelCallback() {
            @Override
            public void onTimeTravelSelected(int year, int month, int day, int hour, int minute) {
                Log.d(TAG, "Time travel to: " + year + "-" + month + "-" + day + " " + hour + ":" + minute);
                if (timeTravelClock != null) {
                    timeTravelClock.travelToDateTime(year, month, day, hour, minute);
                    updateTimeTravelIndicator(true);
                    // Update sky view with new time
                    updateSkyForTime(timeTravelClock.getCurrentTimeMillis());
                }
            }

            @Override
            public void onReturnToRealTime() {
                Log.d(TAG, "Returning to real time");
                if (timeTravelClock != null) {
                    timeTravelClock.returnToRealTime();
                    updateTimeTravelIndicator(false);
                    // Update sky view with current time
                    updateSkyForTime(System.currentTimeMillis());
                }
            }
        });
        dialog.show(getSupportFragmentManager(), "time_travel");
    }

    /**
     * Updates the time travel indicator in the UI.
     *
     * @param active whether time travel is active
     */
    private void updateTimeTravelIndicator(boolean active) {
        ImageView ivTimeTravel = findViewById(R.id.ivTimeTravel);
        if (ivTimeTravel != null) {
            int tintColor = active ? R.color.time_travel_active : R.color.icon_primary;
            ivTimeTravel.setColorFilter(ContextCompat.getColor(this, tintColor));
        }
    }

    /**
     * Updates the sky view for a specific time.
     *
     * @param timeMillis the time in milliseconds since epoch
     */
    private void updateSkyForTime(long timeMillis) {
        // Update astronomer model time
        if (astronomerModel != null) {
            astronomerModel.setTime(timeMillis);
        }

        // Reload star data for canvas with new time
        if (skyCanvasView != null) {
            skyCanvasView.setTime(timeMillis);
            skyCanvasView.invalidate();
        }

        // Update planets layer for new time
        if (planetsLayer != null) {
            planetsLayer.setTime(timeMillis);
        }

        // Update planet positions in canvas if planets are enabled
        if (isPlanetsEnabled) {
            updatePlanetPositions();
        }

        // Update GL surface view
        if (skyGLSurfaceView != null) {
            skyGLSurfaceView.requestLayerUpdate();
        }

        Log.d(TAG, "Sky updated for time: " + new java.util.Date(timeMillis));
    }

    /**
     * Toggles visibility of planets in the sky view.
     */
    private void togglePlanets() {
        isPlanetsEnabled = !isPlanetsEnabled;

        // Update planets layer visibility
        if (planetsLayer != null) {
            planetsLayer.setVisible(isPlanetsEnabled);
            skyGLSurfaceView.requestLayerUpdate();
        }

        // Update SkyCanvasView planet visibility
        if (skyCanvasView != null) {
            skyCanvasView.setPlanetsVisible(isPlanetsEnabled);
            if (isPlanetsEnabled) {
                updatePlanetPositions();
            }
        }

        // Update icon color to indicate state
        ImageView ivPlanets = findViewById(R.id.ivPlanets);
        if (ivPlanets != null) {
            int tintColor = isPlanetsEnabled ? R.color.icon_primary : R.color.icon_inactive;
            ivPlanets.setColorFilter(ContextCompat.getColor(this, tintColor));
        }

        Log.d(TAG, "Planets visibility toggled to: " + isPlanetsEnabled);
    }

    /**
     * Updates planet positions in the SkyCanvasView based on current time.
     */
    private void updatePlanetPositions() {
        if (skyCanvasView == null || universe == null) {
            return;
        }

        // Get current observation time
        long timeMillis = (timeTravelClock != null)
                ? timeTravelClock.getCurrentTimeMillis()
                : System.currentTimeMillis();
        Date observationDate = new Date(timeMillis);

        // Update each planet position
        for (SolarSystemBody body : SolarSystemBody.values()) {
            if (body == SolarSystemBody.Earth) {
                continue; // Skip Earth
            }

            try {
                RaDec raDec = universe.getRaDec(body, observationDate);
                int color = getPlanetColor(body);
                float size = getPlanetSize(body);
                skyCanvasView.setPlanet(body.name(), raDec.getRa(), raDec.getDec(), color, size);
            } catch (Exception e) {
                Log.e(TAG, "Error updating planet " + body.name() + ": " + e.getMessage());
            }
        }

        Log.d(TAG, "Planet positions updated for time: " + observationDate);
    }

    /**
     * Gets the display color for a solar system body.
     */
    private int getPlanetColor(SolarSystemBody body) {
        switch (body) {
            case Sun:
                return 0xFFFFD700;      // Gold
            case Moon:
                return 0xFFF4F4F4;     // Near white
            case Mercury:
                return 0xFFB0B0B0;  // Gray
            case Venus:
                return 0xFFE6E6CC;    // Pale yellow
            case Mars:
                return 0xFFFF6347;     // Red-orange
            case Jupiter:
                return 0xFFD4A574;  // Tan/brown
            case Saturn:
                return 0xFFF4D59E;   // Pale gold
            case Uranus:
                return 0xFFAFDBF5;   // Pale blue
            case Neptune:
                return 0xFF5B5DDF; // Deep blue
            case Pluto:
                return 0xFFCCBBAA;    // Brownish gray
            default:
                return 0xFFFFFFFF;
        }
    }

    /**
     * Gets the display size for a solar system body.
     */
    private float getPlanetSize(SolarSystemBody body) {
        switch (body) {
            case Sun:
            case Moon:
                return 12f;
            case Venus:
            case Jupiter:
                return 9f;
            case Mars:
            case Saturn:
                return 7f;
            default:
                return 5f;
        }
    }

    /**
     * Opens the search activity to search for celestial objects.
     */
    private void openSearch() {
        Intent intent = new Intent(this, SearchActivity.class);
        searchActivityLauncher.launch(intent);
    }

    /**
     * Handles the result from SearchActivity.
     *
     * @param data The result intent containing search target info
     */
    private void handleSearchResult(Intent data) {
        searchTargetName = data.getStringExtra(SearchActivity.EXTRA_RESULT_NAME);
        searchTargetRa = data.getFloatExtra(SearchActivity.EXTRA_RESULT_RA, 0f);
        searchTargetDec = data.getFloatExtra(SearchActivity.EXTRA_RESULT_DEC, 0f);

        Log.d(TAG, "Search target: " + searchTargetName + " at RA=" + searchTargetRa + ", Dec=" + searchTargetDec);

        // Show search arrow
        if (searchArrowView != null) {
            searchArrowView.setTarget(searchTargetRa, searchTargetDec, searchTargetName);
            searchArrowView.setVisibility(View.VISIBLE);

            // Update arrow pointing based on current view direction
            updateSearchArrow();
        }

        // Navigate to target location
        if (skyCanvasView != null) {
            skyCanvasView.setOrientation(searchTargetRa, searchTargetDec);
        }

        Toast.makeText(this, getString(R.string.search_navigating_to, searchTargetName), Toast.LENGTH_SHORT).show();
    }

    /**
     * Updates the search arrow position based on current view direction.
     */
    private void updateSearchArrow() {
        if (searchArrowView == null || !searchArrowView.isActive()) {
            return;
        }

        // Get current view direction from skyCanvasView or sensor controller
        float viewRa = 0f;
        float viewDec = 45f;

        if (skyCanvasView != null) {
            // Assuming skyCanvasView has getOrientation method or similar
            viewRa = skyCanvasView.getViewRa();
            viewDec = skyCanvasView.getViewDec();
        }

        searchArrowView.updatePointing(viewRa, viewDec);

        // Check if we're close to the target (within 5 degrees)
        float dRa = Math.abs(searchTargetRa - viewRa);
        float dDec = Math.abs(searchTargetDec - viewDec);
        if (dRa > 180) dRa = 360 - dRa;  // Handle wraparound

        double distance = Math.sqrt(dRa * dRa + dDec * dDec);
        if (distance < 5.0) {
            // Close to target, hide arrow after a delay
            searchArrowView.postDelayed(this::clearSearchTarget, 3000);
        }
    }

    /**
     * Clears the current search target.
     */
    private void clearSearchTarget() {
        if (searchArrowView != null) {
            searchArrowView.clearTarget();
            searchArrowView.setVisibility(View.GONE);
        }
        searchTargetName = null;
    }

    /**
     * Shows information about a selected star.
     *
     * @param star The star to display info for
     */
    private void showStarInfo(@NonNull StarData star) {
        selectedStar = star;

        tvInfoPanelName.setText(star.getName());
        tvInfoPanelType.setText(getString(R.string.star_type_format,
                star.getConstellationId() != null ? star.getConstellationId() : getString(R.string.unknown)));
        tvInfoPanelMagnitude.setText(String.format("%.2f", star.getMagnitude()));
        tvInfoPanelRA.setText(formatRA(star.getRa()));
        tvInfoPanelDec.setText(formatDec(star.getDec()));

        infoPanel.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the info panel.
     */
    private void hideInfoPanel() {
        infoPanel.setVisibility(View.GONE);
        selectedStar = null;
    }

    /**
     * Opens the star details activity.
     *
     * @param star The star to show details for
     */
    private void openStarDetails(@NonNull StarData star) {
        Intent intent = new Intent(this, StarInfoActivity.class);
        intent.putExtra(StarInfoActivity.EXTRA_STAR_ID, star.getId());
        intent.putExtra(StarInfoActivity.EXTRA_STAR_NAME, star.getName());
        intent.putExtra(StarInfoActivity.EXTRA_STAR_RA, star.getRa());
        intent.putExtra(StarInfoActivity.EXTRA_STAR_DEC, star.getDec());
        intent.putExtra(StarInfoActivity.EXTRA_STAR_MAGNITUDE, star.getMagnitude());
        startActivity(intent);
    }

    /**
     * Formats Right Ascension for display.
     *
     * @param raDegrees RA in degrees
     * @return Formatted string (e.g., "2h 31m")
     */
    private String formatRA(float raDegrees) {
        float raHours = raDegrees / 15.0f;
        int hours = (int) raHours;
        int minutes = (int) ((raHours - hours) * 60);
        return String.format("%dh %dm", hours, minutes);
    }

    /**
     * Formats Declination for display.
     *
     * @param decDegrees Dec in degrees
     * @return Formatted string (e.g., "+89 15'")
     */
    private String formatDec(float decDegrees) {
        String sign = decDegrees >= 0 ? "+" : "";
        int degrees = (int) decDegrees;
        int minutes = (int) (Math.abs(decDegrees - degrees) * 60);
        return String.format("%s%d %d'", sign, degrees, minutes);
    }

    /**
     * Shows or hides the loading overlay.
     *
     * @param show true to show loading, false to hide
     */
    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        skyGLSurfaceView.onResume();

        // Start sensors
        if (sensorController != null) {
            sensorController.start();
        }

        // Start GPS location updates if permission is granted
        if (locationController != null && hasLocationPermission()) {
            locationController.start();
            updateGpsIndicator(false, getString(R.string.gps_searching));
        }

        // Resume camera if AR mode is enabled
        if (isARModeEnabled && permissionHandler != null && permissionHandler.hasCameraPermission()) {
            startCameraPreview();
        }
    }

    /**
     * Checks if location permission is granted.
     */
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onPause() {
        super.onPause();
        skyGLSurfaceView.onPause();

        // Stop sensors to save battery
        if (sensorController != null) {
            sensorController.stop();
        }

        // Stop GPS location updates to save battery
        if (locationController != null) {
            locationController.stop();
        }

        // Stop camera
        if (cameraManager != null) {
            cameraManager.stopCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Release camera resources
        if (cameraManager != null) {
            cameraManager.release();
        }
    }
}
