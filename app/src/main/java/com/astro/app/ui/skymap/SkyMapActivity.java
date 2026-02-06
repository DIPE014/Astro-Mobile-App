package com.astro.app.ui.skymap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
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
import com.astro.app.core.math.Vector3;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.StarRepository;
import com.astro.app.ui.education.EducationDetailActivity;
import com.astro.app.ui.search.SearchActivity;
import com.astro.app.ui.settings.SettingsActivity;
import com.astro.app.ui.settings.SettingsViewModel;
import com.astro.app.ui.starinfo.StarInfoActivity;
import com.astro.app.search.SearchArrowView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.ScrollView;

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
    private MaterialButton btnSearchDetails;
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
    private TextView tvSearchTapHint;
    private String searchTargetName;
    private float searchTargetRa;
    private float searchTargetDec;
    @Nullable
    private String searchTargetConstellation;
    @Nullable
    private String searchTargetType;
    @Nullable
    private String searchTargetId;
    private boolean searchTargetBelowHorizonNotified = false;
    private boolean searchTargetInViewNotified = false;
    private boolean isSearchModeActive = false;
    private float lastViewAzimuth = 0f;
    private float lastViewAltitude = 45f;
    @Nullable
    private Vector3 lastViewDirection = null;
    @Nullable
    private Vector3 lastViewUp = null;

    // Reticle selection
    private ExtendedFloatingActionButton fabSelect;
    private Handler reticleCheckHandler;
    private Runnable reticleCheckRunnable;
    private static final long RETICLE_CHECK_INTERVAL_MS = 500;  // Check every 500ms

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
     * Uses matrix-based transformation (matching stardroid's approach) to convert
     * sensor data to celestial coordinates, then to Alt/Az for display.
     *
     * The correct transformation flow is:
     * 1. Rotation vector sensor → pass to AstronomerModel
     * 2. AstronomerModel calculates celestial pointing using matrix math
     * 3. Convert celestial pointing (RA/Dec) to local coordinates (Alt/Az)
     * 4. Update SkyCanvasView with Alt/Az
     *
     * This approach avoids the errors introduced by early Euler angle conversion.
     */
    private void setupSensorController() {
        if (sensorController == null) {
            Log.w(TAG, "SensorController is null, compass tracking unavailable");
            return;
        }

        sensorController.setListener(rotationVector -> {
            // Pass rotation vector directly to AstronomerModel for matrix-based transformation
            // This matches stardroid's approach and avoids Euler angle conversion errors
            if (astronomerModel != null) {
                astronomerModel.setPhoneSensorValues(rotationVector);

                // Get the celestial pointing (RA/Dec) calculated via matrix transformation
                com.astro.app.common.model.Pointing pointing = astronomerModel.getPointing();
                float viewRa = pointing.getRightAscension();
                float viewDec = pointing.getDeclination();
                lastViewDirection = pointing.getLineOfSight();
                lastViewUp = pointing.getPerpendicular();

                // Convert celestial coordinates (RA/Dec) to local coordinates (Alt/Az)
                // This requires observer's location and Local Sidereal Time
                float[] altAz = raDecToAltAzForView(viewRa, viewDec);
                float altitude = altAz[0];
                float azimuth = altAz[1];

                // Update the sky canvas view with new orientation
                final float finalAzimuth = azimuth;
                final float finalAltitude = altitude;
                runOnUiThread(() -> {
                    if (skyCanvasView != null) {
                        skyCanvasView.setOrientation(finalAzimuth, finalAltitude);
                    }

                    lastViewAzimuth = finalAzimuth;
                    lastViewAltitude = finalAltitude;

                    // Update search arrow if active
                    if (searchArrowView != null && searchArrowView.isActive()) {
                        updateSearchArrow();
                    }
                });
            }
        });

        Log.d(TAG, "SensorController listener set up with matrix-based transformation (stardroid approach)");
    }

    /**
     * Converts Right Ascension/Declination to Altitude/Azimuth for the view direction.
     * This is the same formula used in SkyCanvasView for star positions.
     *
     * @param ra  Right Ascension in degrees (0-360)
     * @param dec Declination in degrees (-90 to +90)
     * @return float array [altitude, azimuth] in degrees
     */
    private float[] raDecToAltAzForView(float ra, float dec) {
        // Calculate Local Sidereal Time
        double lst = calculateLocalSiderealTimeForView();

        // Convert to radians
        double latRad = Math.toRadians(currentLatitude);
        double decRad = Math.toRadians(dec);

        // Hour Angle = LST - RA
        double ha = lst - ra;
        if (ha < 0) ha += 360;
        double haRad = Math.toRadians(ha);

        // Calculate altitude
        // sin(alt) = sin(dec) * sin(lat) + cos(dec) * cos(lat) * cos(HA)
        double sinAlt = Math.sin(decRad) * Math.sin(latRad) +
                Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad);
        double altitude = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, sinAlt))));

        // Calculate azimuth
        // cos(A) = (sin(dec) - sin(alt) * sin(lat)) / (cos(alt) * cos(lat))
        double cosAlt = Math.cos(Math.toRadians(altitude));
        double azimuth;
        if (Math.abs(cosAlt) < 0.0001 || Math.abs(Math.cos(latRad)) < 0.0001) {
            // At poles or looking at celestial pole - azimuth is undefined
            azimuth = 0;
        } else {
            double cosA = (Math.sin(decRad) - Math.sin(Math.toRadians(altitude)) * Math.sin(latRad)) /
                    (cosAlt * Math.cos(latRad));
            cosA = Math.max(-1, Math.min(1, cosA)); // Clamp to [-1, 1]
            azimuth = Math.toDegrees(Math.acos(cosA));

            // Determine the sign based on hour angle
            // If sin(HA) > 0, object is West of meridian, azimuth > 180
            if (Math.sin(haRad) > 0) {
                azimuth = 360 - azimuth;
            }
        }

        return new float[]{(float) altitude, (float) azimuth};
    }

    /**
     * Calculates the Local Sidereal Time for the view transformation.
     * Uses the same formula as SkyCanvasView for consistency.
     *
     * LST = GST + longitude
     * GST = 280.461 + 360.98564737 * (JD - 2451545.0)
     *
     * @return LST in degrees (0-360)
     */
    private double calculateLocalSiderealTimeForView() {
        // Get current observation time (or time travel time if active)
        long timeMillis = (timeTravelClock != null)
                ? timeTravelClock.getCurrentTimeMillis()
                : System.currentTimeMillis();

        // Use TimeUtils for consistent LST calculation
        Date observationDate = new Date(timeMillis);
        return com.astro.app.core.math.TimeUtilsKt.meanSiderealTime(observationDate, (float) currentLongitude);
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
        btnSearchDetails = findViewById(R.id.btnSearchDetails);

        // GPS indicator views
        gpsIndicator = findViewById(R.id.gpsIndicator);
        ivGpsIcon = findViewById(R.id.ivGpsIcon);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);

        // Search arrow view
        searchArrowView = findViewById(R.id.searchArrow);
        tvSearchTapHint = findViewById(R.id.tvSearchTapHint);

        // Select FAB for reticle selection
        fabSelect = findViewById(R.id.fabSelect);

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
        skyCanvasView.setEnabled(true);
        skyCanvasView.setOnSkyTapListener((x, y) -> {
            if (searchTargetName != null) {
                clearSearchTarget();
            }
        });
        setSearchModeActive(false);

        Log.d(TAG, "SkyCanvasView created - Canvas2D rendering enabled");

        skyCanvasView.setOnObjectSelectedListener(obj -> {
            if ("planet".equals(obj.type)) {
                openEducationDetail(EducationDetailActivity.TYPE_PLANET, obj.name, obj.id);
            } else if ("constellation".equals(obj.type)) {
                openEducationDetail(EducationDetailActivity.TYPE_CONSTELLATION, obj.name, obj.id);
            }
        });

        skyCanvasView.setOnTrajectoryListener((planetName, x, y) -> {
            if (universe == null) return;
            new Thread(() -> {
                try {
                    SolarSystemBody body = null;
                    for (SolarSystemBody b : SolarSystemBody.values()) {
                        if (b.name().equalsIgnoreCase(planetName)) {
                            body = b;
                            break;
                        }
                    }
                    if (body == null) return;

                    long now = (timeTravelClock != null) ? timeTravelClock.getCurrentTimeMillis() : System.currentTimeMillis();
                    long sixMonthsMs = 6L * 30 * 24 * 3600 * 1000;
                    long step = 2L * 24 * 3600 * 1000; // 2 days
                    List<SkyCanvasView.TrajectoryPoint> points = new ArrayList<>();
                    for (long t = now - sixMonthsMs; t <= now + sixMonthsMs; t += step) {
                        RaDec raDec = universe.getRaDec(body, new Date(t));
                        points.add(new SkyCanvasView.TrajectoryPoint(t, raDec.getRa(), raDec.getDec()));
                    }
                    runOnUiThread(() -> skyCanvasView.startTrajectory(planetName, points, now));
                } catch (Exception e) {
                    Log.e(TAG, "Error computing trajectory: " + e.getMessage());
                }
            }).start();
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

        // Initialize toggle button visual states
        initializeToggleButtonStates();

        // Search FAB
        View fabSearch = findViewById(R.id.fabSearch);
        if (fabSearch != null) {
            fabSearch.setOnClickListener(v -> openSearch());
        }

        if (btnSearchDetails != null) {
            btnSearchDetails.setOnClickListener(v -> openSearchTargetEducation());
        }

        // Select FAB (for reticle selection)
        if (fabSelect != null) {
            fabSelect.setOnClickListener(v -> showObjectSelectionDialog());
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
                if (isSearchModeActive) {
                    clearSearchTarget();
                    return;
                }
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
        starsLayer = new StarsLayer(starRepository);
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
     * Updates a toggle button's visual state with green (ON) or red (OFF) indicators.
     *
     * @param icon            The ImageView icon for the toggle
     * @param buttonContainer The parent View container for the button
     * @param isEnabled       Whether the toggle is enabled
     */
    private void updateToggleButtonVisual(ImageView icon, View buttonContainer, boolean isEnabled) {
        if (icon != null) {
            int iconColor = isEnabled ? R.color.icon_primary : R.color.icon_inactive;
            icon.setColorFilter(ContextCompat.getColor(this, iconColor));
        }

        if (buttonContainer != null) {
            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(12f);
            if (isEnabled) {
                // Green for ON state
                background.setColor(ContextCompat.getColor(this, R.color.toggle_on_bg));
                background.setStroke(3, ContextCompat.getColor(this, R.color.toggle_on));
            } else {
                // Red for OFF state
                background.setColor(ContextCompat.getColor(this, R.color.toggle_off_bg));
                background.setStroke(3, ContextCompat.getColor(this, R.color.toggle_off));
            }
            buttonContainer.setBackground(background);
        }
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

        // Update visual state with green/red indicator
        View btnConstellations = findViewById(R.id.btnConstellations);
        ImageView ivConstellations = findViewById(R.id.ivConstellations);
        updateToggleButtonVisual(ivConstellations, btnConstellations, isConstellationsEnabled);
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

        // Update visual state with green/red indicator
        View btnGrid = findViewById(R.id.btnGrid);
        ImageView ivGrid = findViewById(R.id.ivGrid);
        updateToggleButtonVisual(ivGrid, btnGrid, isGridEnabled);
    }

    /**
     * Initializes the visual state of all toggle buttons on startup.
     * This ensures buttons display correct green/red indicators based on initial settings.
     */
    private void initializeToggleButtonStates() {
        // Constellations toggle
        View btnConstellations = findViewById(R.id.btnConstellations);
        ImageView ivConstellations = findViewById(R.id.ivConstellations);
        updateToggleButtonVisual(ivConstellations, btnConstellations, isConstellationsEnabled);

        // Grid toggle
        View btnGrid = findViewById(R.id.btnGrid);
        ImageView ivGrid = findViewById(R.id.ivGrid);
        updateToggleButtonVisual(ivGrid, btnGrid, isGridEnabled);

        // Planets toggle
        View btnPlanets = findViewById(R.id.btnPlanets);
        ImageView ivPlanets = findViewById(R.id.ivPlanets);
        updateToggleButtonVisual(ivPlanets, btnPlanets, isPlanetsEnabled);
    }

    /**
     * Shows the time travel dialog to select a different date/time.
     */
    private void showTimeTravelDialog() {
        Log.d(TAG, "TIME_TRAVEL: showTimeTravelDialog called");
        // Pass current time travel time (or current time if not in time travel mode)
        long currentTime = (timeTravelClock != null)
                ? timeTravelClock.getCurrentTimeMillis()
                : System.currentTimeMillis();
        Log.d(TAG, "TIME_TRAVEL: Creating dialog with time " + new Date(currentTime));
        TimeTravelDialogFragment dialog = TimeTravelDialogFragment.newInstance(currentTime);
        Log.d(TAG, "TIME_TRAVEL: Setting callback on dialog");
        dialog.setCallback(new TimeTravelDialogFragment.TimeTravelCallback() {
            @Override
            public void onTimeTravelSelected(int year, int month, int day, int hour, int minute) {
                Log.d(TAG, "TIME_TRAVEL: Callback received: " + year + "-" + month + "-" + day + " " + hour + ":" + minute);
                if (timeTravelClock != null) {
                    timeTravelClock.travelToDateTime(year, month, day, hour, minute);
                    long newTime = timeTravelClock.getCurrentTimeMillis();
                    Log.d(TAG, "TIME_TRAVEL: Clock set to: " + new Date(newTime));
                    Toast.makeText(SkyMapActivity.this,
                            "Time travel to: " + year + "-" + month + "-" + day + " " + hour + ":" + minute,
                            Toast.LENGTH_SHORT).show();
                    updateTimeTravelIndicator(true);
                    // Update sky view with new time
                    updateSkyForTime(newTime);
                } else {
                    Log.e(TAG, "TIME_TRAVEL: timeTravelClock is NULL!");
                    Toast.makeText(SkyMapActivity.this, "Error: TimeTravelClock not initialized", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onReturnToRealTime() {
                Log.d(TAG, "Returning to real time");
                if (timeTravelClock != null) {
                    timeTravelClock.returnToRealTime();
                    updateTimeTravelIndicator(false);

                    // Reset AstronomerModel to use real-time clock (not a fixed time)
                    if (astronomerModel != null) {
                        astronomerModel.setClock(new com.astro.app.core.control.RealClock());
                    }

                    // Update sky view to refresh with current time
                    if (skyCanvasView != null) {
                        skyCanvasView.setTime(System.currentTimeMillis());
                        skyCanvasView.invalidate();
                    }

                    // Update planets layer
                    if (planetsLayer != null) {
                        planetsLayer.setTime(System.currentTimeMillis());
                    }

                    // Update GL surface
                    if (skyGLSurfaceView != null) {
                        skyGLSurfaceView.requestLayerUpdate();
                    }
                }
            }
        });
        Log.d(TAG, "TIME_TRAVEL: Callback set, now showing dialog");
        dialog.show(getSupportFragmentManager(), "time_travel");
        Log.d(TAG, "TIME_TRAVEL: Dialog show() called");
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
        Log.d(TAG, "TIME_TRAVEL: updateSkyForTime called with: " + new Date(timeMillis));

        // Update astronomer model time
        if (astronomerModel != null) {
            astronomerModel.setTime(timeMillis);
            Log.d(TAG, "TIME_TRAVEL: AstronomerModel time set");
        } else {
            Log.w(TAG, "TIME_TRAVEL: astronomerModel is null!");
        }

        // Reload star data for canvas with new time
        if (skyCanvasView != null) {
            Log.d(TAG, "TIME_TRAVEL: Calling skyCanvasView.setTime()");
            skyCanvasView.setTime(timeMillis);
            // Note: setTime() already calls invalidate(), but call it again to be sure
            skyCanvasView.invalidate();
            Log.d(TAG, "TIME_TRAVEL: skyCanvasView invalidated");
        } else {
            Log.w(TAG, "TIME_TRAVEL: skyCanvasView is null!");
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

        Log.d(TAG, "TIME_TRAVEL: Sky updated for time: " + new Date(timeMillis));
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

        // Update visual state with green/red indicator
        View btnPlanets = findViewById(R.id.btnPlanets);
        ImageView ivPlanets = findViewById(R.id.ivPlanets);
        updateToggleButtonVisual(ivPlanets, btnPlanets, isPlanetsEnabled);

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
                return 0xFF8B3A2E;     // Brown-red
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
        intent.putExtra(SearchActivity.EXTRA_OBSERVER_LAT, (double) currentLatitude);
        intent.putExtra(SearchActivity.EXTRA_OBSERVER_LON, (double) currentLongitude);
        long observationTime = (timeTravelClock != null)
                ? timeTravelClock.getCurrentTimeMillis()
                : System.currentTimeMillis();
        intent.putExtra(SearchActivity.EXTRA_OBSERVATION_TIME, observationTime);
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
        searchTargetConstellation = data.getStringExtra(SearchActivity.EXTRA_RESULT_CONSTELLATION);
        String searchTargetId = data.getStringExtra(SearchActivity.EXTRA_RESULT_ID);
        String resultType = data.getStringExtra(SearchActivity.EXTRA_RESULT_TYPE);
        searchTargetType = resultType;
        searchTargetId = data.getStringExtra(SearchActivity.EXTRA_RESULT_ID);
        searchTargetBelowHorizonNotified = false;
        searchTargetInViewNotified = false;
        setSearchModeActive(true);

        updateSearchDetailsButtonVisibility();

        // For planets, recalculate position for current time (or time travel time)
        // The search index stores positions at index build time which may be stale
        if (resultType != null && (resultType.equals("PLANET") || resultType.equals("SUN") || resultType.equals("MOON"))) {
            updatePlanetSearchTarget();
        }

        Log.d(TAG, "Search target: " + searchTargetName + " at RA=" + searchTargetRa + ", Dec=" + searchTargetDec);

        // Highlight selected object in the sky view (if applicable)
        if (skyCanvasView != null) {
            if (resultType != null && (resultType.equals("PLANET") || resultType.equals("SUN") || resultType.equals("MOON"))) {
                SolarSystemBody body = findSolarSystemBody(searchTargetName);
                if (body != null) {
                    int color = getPlanetColor(body);
                    float size = getPlanetSize(body);
                    skyCanvasView.setPlanet(body.name(), searchTargetRa, searchTargetDec, color, size);
                    skyCanvasView.setHighlightedPlanet(body.name());
                } else {
                    skyCanvasView.setPlanet(searchTargetName, searchTargetRa, searchTargetDec, 0xFFFF4444, 10f);
                    skyCanvasView.setHighlightedPlanet(searchTargetName);
                }
            } else if (resultType != null && resultType.equals("STAR")) {
                StarData star = null;
                if (searchTargetId != null && starRepository != null) {
                    star = starRepository.getStarById(searchTargetId);
                }
                if (star == null && starRepository != null && searchTargetName != null) {
                    star = starRepository.getStarByName(searchTargetName);
                }
                skyCanvasView.setHighlightedStar(star);
            } else {
                skyCanvasView.clearHighlight();
            }
        }

        // Show search arrow to guide user to target
        // NOTE: Don't immediately set orientation - let the arrow guide the user
        if (searchArrowView != null) {
            searchArrowView.setTarget(searchTargetRa, searchTargetDec, searchTargetName, searchTargetConstellation);
            searchArrowView.setVisibility(View.VISIBLE);
            searchArrowView.setOnClickListener(v -> clearSearchTarget());

            // Auto-dismiss when target is centered
            searchArrowView.setOnTargetCenteredListener(() -> {
                Toast.makeText(this, getString(R.string.search_target_found, searchTargetName), Toast.LENGTH_SHORT).show();
            });

            // Update arrow pointing based on current view direction
            updateSearchArrow();
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

        // Use AstronomerModel pointing when available (most direct sensor-derived RA/Dec)
        float viewRa = 0f;
        float viewDec = 45f;

        if (astronomerModel != null) {
            com.astro.app.common.model.Pointing pointing = astronomerModel.getPointing();
            viewRa = pointing.getRightAscension();
            viewDec = pointing.getDeclination();
            if (searchArrowView != null) {
                searchArrowView.updatePointing(pointing.getLineOfSight(), pointing.getPerpendicular());
            }
            Log.d(TAG, "SEARCH_ARROW: using AstronomerModel vectors");
        } else if (lastViewDirection != null && lastViewUp != null) {
            RaDec raDec = RaDec.fromGeocentricCoords(lastViewDirection);
            viewRa = raDec.getRa();
            viewDec = raDec.getDec();
            if (searchArrowView != null) {
                searchArrowView.updatePointing(lastViewDirection, lastViewUp);
            }
            Log.d(TAG, "SEARCH_ARROW: using cached sensor vectors");
        } else if (skyCanvasView != null) {
            skyCanvasView.setTime(System.currentTimeMillis());
            float viewAz = lastViewAzimuth;
            float viewAlt = lastViewAltitude;
            double[] raDec = altAzToRaDecForView(viewAlt, viewAz);
            viewRa = (float) raDec[0];
            viewDec = (float) raDec[1];
            Log.d(TAG, "SEARCH_ARROW: astronomerModel null, using alt/az fallback");
        }

        if (searchArrowView != null && astronomerModel == null) {
            searchArrowView.updatePointing(viewRa, viewDec);
        }

        // Log target visibility and notify if below horizon
        float targetAlt = 0f;
        float targetAz = 0f;
        double[] targetAltAz = raDecToAltAz(searchTargetRa, searchTargetDec);
        if (targetAltAz != null) {
            targetAlt = (float) targetAltAz[0];
            targetAz = (float) targetAltAz[1];
        }
        Log.d(TAG, "SEARCH_VISIBILITY: target=" + searchTargetName + " alt=" + targetAlt +
                " az=" + targetAz + " viewRa=" + viewRa + " viewDec=" + viewDec);
        if (targetAlt < 0 && !searchTargetBelowHorizonNotified) {
            searchTargetBelowHorizonNotified = true;
            Toast.makeText(this, getString(R.string.search_target_below_horizon, searchTargetName),
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "SEARCH_VISIBILITY: target below horizon");
        }

        // Notify when target is within the camera field of view
        float fov = skyCanvasView != null ? skyCanvasView.getFieldOfView() : 60f;
        double angularDistance = calculateAngularDistance(viewRa, viewDec, searchTargetRa, searchTargetDec);
        boolean targetInView = angularDistance <= (fov / 2f);
        if (targetInView && !searchTargetInViewNotified) {
            searchTargetInViewNotified = true;
            Toast.makeText(this, getString(R.string.search_target_in_view, searchTargetName),
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "SEARCH_VISIBILITY: target in view (distance=" + angularDistance + "°)");
        }

        // Hide only the arrow (keep overlay/search active) when target is in view.
        if (searchArrowView != null) {
            searchArrowView.setArrowVisible(!targetInView);
        }

        // Keep search active until user dismisses it.
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
        searchTargetConstellation = null;
        searchTargetType = null;
        searchTargetId = null;
        searchTargetBelowHorizonNotified = false;
        searchTargetInViewNotified = false;
        updateSearchDetailsButtonVisibility();
        if (skyCanvasView != null) {
            skyCanvasView.clearHighlight();
        }
        setSearchModeActive(false);
    }

    private void setSearchModeActive(boolean active) {
        isSearchModeActive = active;
        if (skyCanvasView != null) {
            skyCanvasView.setEnabled(true);
            skyCanvasView.setSearchModeActive(active);
        }
        if (tvSearchTapHint != null) {
            tvSearchTapHint.setVisibility(active ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Calculates angular distance between two RA/Dec points in degrees.
     */
    private double calculateAngularDistance(float ra1, float dec1, float ra2, float dec2) {
        double ra1Rad = Math.toRadians(ra1);
        double dec1Rad = Math.toRadians(dec1);
        double ra2Rad = Math.toRadians(ra2);
        double dec2Rad = Math.toRadians(dec2);

        double dRa = ra2Rad - ra1Rad;
        double dDec = dec2Rad - dec1Rad;

        double a = Math.sin(dDec / 2) * Math.sin(dDec / 2) +
                Math.cos(dec1Rad) * Math.cos(dec2Rad) *
                        Math.sin(dRa / 2) * Math.sin(dRa / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.toDegrees(c);
    }

    /**
     * Converts Altitude/Azimuth to Right Ascension/Declination using current
     * observer location and time.
     *
     * @param altitude Altitude in degrees (0 = horizon, 90 = zenith)
     * @param azimuth  Azimuth in degrees (0 = North, 90 = East)
     * @return double array [RA, Dec] in degrees
     */
    private double[] altAzToRaDecForView(double altitude, double azimuth) {
        double lst = calculateLocalSiderealTimeForView();

        double latRad = Math.toRadians(currentLatitude);
        double altRad = Math.toRadians(altitude);
        double azRad = Math.toRadians(azimuth);

        double sinDec = Math.sin(altRad) * Math.sin(latRad) +
                Math.cos(altRad) * Math.cos(latRad) * Math.cos(azRad);
        double dec = Math.toDegrees(Math.asin(sinDec));

        double cosHa = (Math.sin(altRad) - Math.sin(latRad) * Math.sin(Math.toRadians(dec))) /
                (Math.cos(latRad) * Math.cos(Math.toRadians(dec)));
        cosHa = Math.max(-1, Math.min(1, cosHa));

        double ha = Math.toDegrees(Math.acos(cosHa));
        if (Math.sin(azRad) > 0) {
            ha = 360 - ha;
        }

        double ra = lst - ha;
        while (ra < 0) ra += 360;
        while (ra >= 360) ra -= 360;

        return new double[]{ra, dec};
    }

    /**
     * Converts Right Ascension/Declination to Altitude/Azimuth for the observer.
     *
     * @param ra  Right Ascension in degrees (0-360)
     * @param dec Declination in degrees (-90 to +90)
     * @return double array [altitude, azimuth] in degrees
     */
    private double[] raDecToAltAz(float ra, float dec) {
        double lst = calculateLocalSiderealTimeForView();

        double latRad = Math.toRadians(currentLatitude);
        double decRad = Math.toRadians(dec);

        double ha = lst - ra;
        if (ha < 0) ha += 360;
        double haRad = Math.toRadians(ha);

        double sinAlt = Math.sin(decRad) * Math.sin(latRad) +
                Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad);
        double altitude = Math.toDegrees(Math.asin(sinAlt));

        double cosA = (Math.sin(decRad) - Math.sin(Math.toRadians(altitude)) * Math.sin(latRad)) /
                (Math.cos(Math.toRadians(altitude)) * Math.cos(latRad));
        cosA = Math.max(-1, Math.min(1, cosA));
        double azimuth = Math.toDegrees(Math.acos(cosA));

        if (Math.sin(haRad) > 0) {
            azimuth = 360 - azimuth;
        }

        return new double[]{altitude, azimuth};
    }

    // ===================================================================
    // RETICLE SELECTION METHODS
    // ===================================================================

    /**
     * Starts periodic checking for objects in the reticle.
     * Shows/hides the Select FAB based on whether objects are present.
     */
    private void startReticleChecking() {
        if (reticleCheckHandler == null) {
            reticleCheckHandler = new Handler(Looper.getMainLooper());
        }
        if (reticleCheckRunnable == null) {
            reticleCheckRunnable = new Runnable() {
                @Override
                public void run() {
                    updateSelectButtonVisibility();
                    reticleCheckHandler.postDelayed(this, RETICLE_CHECK_INTERVAL_MS);
                }
            };
        }
        reticleCheckHandler.post(reticleCheckRunnable);
    }

    /**
     * Stops periodic checking for objects in the reticle.
     */
    private void stopReticleChecking() {
        if (reticleCheckHandler != null && reticleCheckRunnable != null) {
            reticleCheckHandler.removeCallbacks(reticleCheckRunnable);
        }
    }

    /**
     * Updates the visibility of the Select FAB based on objects in reticle.
     */
    private void updateSelectButtonVisibility() {
        if (fabSelect == null || skyCanvasView == null) return;

        List<SkyCanvasView.SelectableObject> objects = skyCanvasView.getObjectsInReticle();
        boolean hasObjects = !objects.isEmpty();

        if (hasObjects && fabSelect.getVisibility() != View.VISIBLE) {
            fabSelect.setText(getString(R.string.select) + " (" + objects.size() + ")");
            fabSelect.setVisibility(View.VISIBLE);
            fabSelect.animate().alpha(1f).setDuration(200).start();
        } else if (hasObjects) {
            fabSelect.setText(getString(R.string.select) + " (" + objects.size() + ")");
        } else if (!hasObjects && fabSelect.getVisibility() == View.VISIBLE) {
            fabSelect.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                fabSelect.setVisibility(View.GONE);
            }).start();
            // Clear any highlight when no objects in reticle
            skyCanvasView.clearHighlight();
        }
    }

    /**
     * Shows a bottom sheet dialog listing objects in the reticle.
     */
    private void showObjectSelectionDialog() {
        if (skyCanvasView == null) return;

        List<SkyCanvasView.SelectableObject> objects = skyCanvasView.getObjectsInReticle();
        if (objects.isEmpty()) {
            Toast.makeText(this, R.string.no_objects_in_reticle, Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);

        // Create dialog layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(
                (int) getResources().getDimension(R.dimen.padding_medium),
                (int) getResources().getDimension(R.dimen.padding_medium),
                (int) getResources().getDimension(R.dimen.padding_medium),
                (int) getResources().getDimension(R.dimen.padding_medium)
        );
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.surface));

        // Title
        TextView title = new TextView(this);
        title.setText(R.string.objects_in_reticle);
        title.setTextSize(20);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setPadding(0, 0, 0, (int) getResources().getDimension(R.dimen.padding_medium));
        layout.addView(title);

        // Create scrollable list
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollView.setLayoutParams(scrollParams);
        LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);

        // Track currently selected item for highlight preview
        final SkyCanvasView.SelectableObject[] selectedObject = {null};

        for (SkyCanvasView.SelectableObject obj : objects) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(
                    (int) getResources().getDimension(R.dimen.padding_small),
                    (int) getResources().getDimension(R.dimen.padding_medium),
                    (int) getResources().getDimension(R.dimen.padding_small),
                    (int) getResources().getDimension(R.dimen.padding_medium)
            );
            itemLayout.setBackgroundResource(android.R.drawable.list_selector_background);

            // Object icon (placeholder - could be customized per type)
            ImageView icon = new ImageView(this);
            int iconRes = obj.type.equals("planet") ?
                    android.R.drawable.presence_online :
                    android.R.drawable.btn_star_big_on;
            icon.setImageResource(iconRes);
            icon.setColorFilter(ContextCompat.getColor(this,
                    obj.type.equals("planet") ? R.color.planet_color : R.color.star_color));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.icon_size_medium),
                    (int) getResources().getDimension(R.dimen.icon_size_medium)
            );
            iconParams.rightMargin = (int) getResources().getDimension(R.dimen.margin_medium);
            icon.setLayoutParams(iconParams);
            itemLayout.addView(icon);

            // Object info
            LinearLayout infoLayout = new LinearLayout(this);
            infoLayout.setOrientation(LinearLayout.VERTICAL);
            infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView nameText = new TextView(this);
            nameText.setText(obj.getDisplayName());
            nameText.setTextSize(16);
            nameText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            infoLayout.addView(nameText);

            TextView typeText = new TextView(this);
            String typeString;
            if (obj.type.equals("planet")) {
                typeString = getString(R.string.object_planet);
            } else if (obj.type.equals("constellation")) {
                typeString = getString(R.string.object_constellation);
            } else {
                typeString = getString(R.string.object_star);
            }
            if (obj.type.equals("star")) {
                typeString += " (mag " + String.format("%.1f", obj.magnitude) + ")";
            }
            typeText.setText(typeString);
            typeText.setTextSize(12);
            typeText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            infoLayout.addView(typeText);

            itemLayout.addView(infoLayout);

            // Click handler - highlight object on tap
            itemLayout.setOnClickListener(v -> {
                selectedObject[0] = obj;
                // Highlight the object on the sky view
                if (obj.type.equals("planet")) {
                    skyCanvasView.setHighlightedPlanet(obj.name);
                } else if (obj.type.equals("constellation")) {
                    skyCanvasView.clearHighlight();
                } else {
                    StarData star = skyCanvasView.getStarById(obj.id);
                    if (star != null) {
                        skyCanvasView.setHighlightedStar(star);
                    }
                }
                Toast.makeText(this, getString(R.string.object_highlighted, obj.getDisplayName()),
                        Toast.LENGTH_SHORT).show();
            });

            // Long press to confirm and open details
            itemLayout.setOnLongClickListener(v -> {
                dialog.dismiss();
                openObjectDetails(obj);
                return true;
            });

            listLayout.addView(itemLayout);

            // Add divider
            View divider = new View(this);
            divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) getResources().getDimension(R.dimen.divider_height)
            );
            listLayout.addView(divider, dividerParams);
        }

        scrollView.addView(listLayout);
        layout.addView(scrollView);

        // Confirm button
        MaterialButton confirmButton = new MaterialButton(this);
        confirmButton.setText(R.string.confirm_selection);
        confirmButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (selectedObject[0] != null) {
                openObjectDetails(selectedObject[0]);
            } else if (!objects.isEmpty()) {
                // If nothing selected, open details for first object
                openObjectDetails(objects.get(0));
            }
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = (int) getResources().getDimension(R.dimen.margin_medium);
        confirmButton.setLayoutParams(buttonParams);
        layout.addView(confirmButton);

        dialog.setContentView(layout);
        dialog.setOnDismissListener(d -> {
            // Clear highlight when dialog is dismissed without selection
            if (skyCanvasView != null) {
                skyCanvasView.clearHighlight();
            }
        });
        dialog.show();
    }

    /**
     * Opens the detail view for a selected object.
     *
     * @param obj The selectable object to show details for
     */
    private void openObjectDetails(SkyCanvasView.SelectableObject obj) {
        if (obj.type.equals("planet")) {
            openEducationDetail(EducationDetailActivity.TYPE_PLANET, obj.name, obj.id);
        } else if (obj.type.equals("constellation")) {
            openEducationDetail(EducationDetailActivity.TYPE_CONSTELLATION, obj.name, obj.id);
        } else {
            // For stars, open StarInfoActivity
            StarData star = skyCanvasView.getStarById(obj.id);
            if (star != null) {
                Intent intent = new Intent(this, StarInfoActivity.class);
                intent.putExtra(StarInfoActivity.EXTRA_STAR_ID, star.getId());
                intent.putExtra(StarInfoActivity.EXTRA_STAR_NAME, star.getName());
                intent.putExtra(StarInfoActivity.EXTRA_STAR_RA, star.getRa());
                intent.putExtra(StarInfoActivity.EXTRA_STAR_DEC, star.getDec());
                intent.putExtra(StarInfoActivity.EXTRA_STAR_MAGNITUDE, star.getMagnitude());
                startActivity(intent);
            }
        }
        // Clear highlight after opening details
        if (skyCanvasView != null) {
            skyCanvasView.clearHighlight();
        }
    }

    /**
     * Updates the search target coordinates for a planet based on current time.
     *
     * <p>Planet positions change over time, so we recalculate them here using
     * the current observation time (or time travel time if active).</p>
     */
    private void updatePlanetSearchTarget() {
        if (universe == null || searchTargetName == null) {
            return;
        }

        // Get current observation time
        long timeMillis = (timeTravelClock != null)
                ? timeTravelClock.getCurrentTimeMillis()
                : System.currentTimeMillis();
        Date observationDate = new Date(timeMillis);

        // Find the matching solar system body
        for (SolarSystemBody body : SolarSystemBody.values()) {
            if (body.name().equalsIgnoreCase(searchTargetName)) {
                try {
                    RaDec raDec = universe.getRaDec(body, observationDate);
                    searchTargetRa = raDec.getRa();
                    searchTargetDec = raDec.getDec();
                    Log.d(TAG, "Updated planet position for " + searchTargetName +
                            " at time " + observationDate + ": RA=" + searchTargetRa +
                            ", Dec=" + searchTargetDec);
                } catch (Exception e) {
                    Log.e(TAG, "Error calculating planet position: " + e.getMessage());
                }
                break;
            }
        }
    }

    @Nullable
    private SolarSystemBody findSolarSystemBody(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (SolarSystemBody body : SolarSystemBody.values()) {
            if (body.name().equalsIgnoreCase(name)) {
                return body;
            }
        }
        return null;
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

    private void openEducationDetail(@NonNull String type, @NonNull String name, @Nullable String id) {
        Intent intent = new Intent(this, EducationDetailActivity.class);
        intent.putExtra(EducationDetailActivity.EXTRA_OBJECT_TYPE, type);
        intent.putExtra(EducationDetailActivity.EXTRA_OBJECT_NAME, name);
        if (id != null) {
            intent.putExtra(EducationDetailActivity.EXTRA_OBJECT_ID, id);
        }
        startActivity(intent);
    }

    private void openSearchTargetEducation() {
        if (searchTargetName == null || searchTargetType == null) {
            return;
        }
        if (isConstellationType(searchTargetType)) {
            openEducationDetail(EducationDetailActivity.TYPE_CONSTELLATION, searchTargetName, searchTargetId);
        } else if (isPlanetType(searchTargetType)) {
            openEducationDetail(EducationDetailActivity.TYPE_PLANET, searchTargetName, searchTargetId);
        }
    }

    private void updateSearchDetailsButtonVisibility() {
        if (btnSearchDetails == null) return;
        boolean visible = searchTargetName != null && searchTargetType != null
                && (isConstellationType(searchTargetType) || isPlanetType(searchTargetType));
        btnSearchDetails.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean isConstellationType(@NonNull String type) {
        return "CONSTELLATION".equals(type);
    }

    private boolean isPlanetType(@NonNull String type) {
        return "PLANET".equals(type) || "SUN".equals(type) || "MOON".equals(type);
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
        setSearchModeActive(searchTargetName != null);

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

        // Start checking for objects in reticle
        startReticleChecking();
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

        // Stop checking for objects in reticle
        stopReticleChecking();

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
