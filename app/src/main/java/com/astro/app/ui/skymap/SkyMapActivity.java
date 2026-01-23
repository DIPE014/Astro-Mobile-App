package com.astro.app.ui.skymap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.astro.app.core.control.SensorController;
import com.astro.app.core.layers.ConstellationsLayer;
import com.astro.app.core.layers.GridLayer;
import com.astro.app.core.layers.StarsLayer;
import com.astro.app.core.renderer.SkyGLSurfaceView;
import com.astro.app.core.renderer.SkyRenderer;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.StarRepository;
import com.astro.app.ui.settings.SettingsActivity;
import com.astro.app.ui.settings.SettingsViewModel;
import com.astro.app.ui.starinfo.StarInfoActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
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

    // Camera components
    private CameraManager cameraManager;
    private CameraPermissionHandler permissionHandler;
    private AROverlayManager arOverlayManager;

    // Views
    private FrameLayout cameraPreviewContainer;
    private FrameLayout skyOverlayContainer;
    private PreviewView cameraPreview;
    private SkyGLSurfaceView skyGLSurfaceView;
    private MaterialButton btnArToggle;
    private MaterialCardView infoPanel;
    private TextView tvInfoPanelName;
    private TextView tvInfoPanelType;
    private TextView tvInfoPanelMagnitude;
    private TextView tvInfoPanelRA;
    private TextView tvInfoPanelDec;
    private FrameLayout loadingOverlay;

    // Layers
    private StarsLayer starsLayer;
    private ConstellationsLayer constellationsLayer;
    private GridLayer gridLayer;

    // State
    // Default to MAP mode (AR disabled) for better emulator compatibility
    // On devices without camera or on emulator, this ensures stars are visible
    private boolean isARModeEnabled = false;
    private boolean isConstellationsEnabled = true;
    private boolean isGridEnabled = false;
    @Nullable
    private StarData selectedStar;

    // Permission launcher
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (permissionHandler != null) {
                    permissionHandler.handlePermissionResult(isGranted);
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

        // Create PreviewView programmatically for camera
        cameraPreview = new PreviewView(this);
        cameraPreview.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        cameraPreviewContainer.addView(cameraPreview);

        // ============================================================
        // MINIMAL OPENGL TEST - Replace custom SkyGLSurfaceView with
        // a basic GLSurfaceView to test if OpenGL works on this emulator
        // ============================================================
        // If this shows RED, the emulator supports OpenGL and the issue is in our custom SkyGLSurfaceView.
        // If this shows nothing/cyan, the emulator has OpenGL issues.

        android.opengl.GLSurfaceView testGLView = new android.opengl.GLSurfaceView(this);
        testGLView.setEGLContextClientVersion(2);
        testGLView.setRenderer(new android.opengl.GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
                android.util.Log.d("TEST_GL", "onSurfaceCreated called!");
            }

            @Override
            public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
                android.opengl.GLES20.glViewport(0, 0, width, height);
                android.util.Log.d("TEST_GL", "onSurfaceChanged: " + width + "x" + height);
            }

            @Override
            public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
                android.opengl.GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);  // RED
                android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT);
            }
        });

        // Set cyan background - if you see cyan, OpenGL rendering is NOT working
        testGLView.setBackgroundColor(0xFF00FFFF);  // Cyan diagnostic background

        // Add test GLSurfaceView to container
        FrameLayout container = findViewById(R.id.skyOverlayContainer);
        container.addView(testGLView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));

        Log.d(TAG, "MINIMAL TEST: Basic GLSurfaceView created - expecting RED screen");

        // ============================================================
        // COMMENTED OUT: Original SkyGLSurfaceView code
        // ============================================================
        /*
        // Create SkyGLSurfaceView programmatically using the renderer constructor
        // This ensures proper initialization order: EGL version -> EGL config -> setRenderer
        SkyRenderer renderer = new SkyRenderer();
        skyGLSurfaceView = new SkyGLSurfaceView(this, renderer);
        skyGLSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // DIAGNOSTIC: Set cyan background to detect if OpenGL isn't rendering
        // If you see cyan, OpenGL's onDrawFrame() isn't being called
        // If you see red, OpenGL IS rendering (diagnostic glClearColor in SkyRenderer)
        skyGLSurfaceView.setBackgroundColor(0xFF00FFFF);  // Cyan background as diagnostic

        // Configure GL surface for MAP mode (opaque) or AR mode (transparent)
        // In MAP mode (default), use opaque surface to show dark blue sky background
        // In AR mode, use transparent surface to show camera behind
        if (isARModeEnabled) {
            skyGLSurfaceView.setZOrderOnTop(true);
            skyGLSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        } else {
            // MAP mode: keep it simple with OPAQUE format
            skyGLSurfaceView.setZOrderOnTop(false);
            skyGLSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.OPAQUE);
        }

        Log.d(TAG, "SkyGLSurfaceView created and configured");
        skyOverlayContainer.addView(skyGLSurfaceView);
        */

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
            if (hasPermissions()) {
                initializeSkyMap();
            } else {
                // Check if camera permission was denied
                boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED;

                if (!cameraGranted) {
                    // Fall back to map-only mode
                    Toast.makeText(this, R.string.permission_camera_denied_fallback, Toast.LENGTH_LONG).show();
                    setARModeEnabled(false);
                    initializeSkyMapOnly();
                } else {
                    finish();
                }
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

        // Set map-only mode
        arOverlayManager.setARModeEnabled(false);
        arOverlayManager.calibrateDefault();

        // Hide camera preview
        cameraPreviewContainer.setVisibility(View.GONE);

        showLoading(false);
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

        // Configure initial visibility
        starsLayer.setVisible(true);
        constellationsLayer.setVisible(isConstellationsEnabled);
        gridLayer.setVisible(isGridEnabled);

        // Add layers to renderer
        List<com.astro.app.core.layers.Layer> layers = new ArrayList<>();
        layers.add(gridLayer);
        layers.add(constellationsLayer);
        layers.add(starsLayer);

        renderer.setLayers(layers);

        // Update layer data
        starsLayer.initialize();
        constellationsLayer.initialize();
        gridLayer.initialize();

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

        ImageView ivGrid = findViewById(R.id.ivGrid);
        if (ivGrid != null) {
            int tintColor = isGridEnabled ? R.color.icon_primary : R.color.icon_inactive;
            ivGrid.setColorFilter(ContextCompat.getColor(this, tintColor));
        }
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

        // Resume camera if AR mode is enabled
        if (isARModeEnabled && permissionHandler != null && permissionHandler.hasCameraPermission()) {
            startCameraPreview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        skyGLSurfaceView.onPause();

        // Stop sensors to save battery
        if (sensorController != null) {
            sensorController.stop();
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
