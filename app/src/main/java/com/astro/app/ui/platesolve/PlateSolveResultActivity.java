package com.astro.app.ui.platesolve;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.astro.app.AstroApplication;
import com.astro.app.R;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.StarRepository;
import com.astro.app.ml.PlateSolveCallback;
import com.astro.app.ml.PlateSolveResult;
import com.astro.app.ml.PlateSolveService;
import com.astro.app.ml.model.DetectedStar;
import com.astro.app.ml.model.SolveStatus;
import com.astro.app.ui.starinfo.StarInfoActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Activity that displays the results of plate solving a captured sky image.
 *
 * <p>This activity receives a captured image path, runs the Tetra3 plate solve
 * algorithm on it, and displays the identified stars with an overlay.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Displays the captured photo with star overlay</li>
 *   <li>Shows solve status (success/failed)</li>
 *   <li>Displays celestial coordinates (RA, Dec, FOV, Roll)</li>
 *   <li>Lists detected stars with tap-to-view-details</li>
 * </ul>
 *
 * @see PhotoCaptureActivity
 * @see PlateSolveService
 * @see StarOverlayView
 */
public class PlateSolveResultActivity extends AppCompatActivity {

    private static final String TAG = "PlateSolveResultActivity";

    /** Default FOV hint for smartphone cameras */
    private static final float DEFAULT_FOV_HINT = 70f;

    // Injected dependencies
    @Inject
    PlateSolveService plateSolveService;

    @Inject
    StarRepository starRepository;

    // Views
    private ImageView ivCapturedPhoto;
    private StarOverlayView starOverlayView;
    private MaterialCardView resultPanel;
    private FrameLayout loadingOverlay;
    private LinearLayout errorState;
    private RecyclerView rvDetectedStars;
    private TextView tvDetectedStarsHeader;

    // Result panel views
    private ImageView ivStatusIcon;
    private TextView tvStatusText;
    private TextView tvMatchedStars;
    private TextView tvCenterRa;
    private TextView tvCenterDec;
    private TextView tvFov;
    private TextView tvRoll;

    // Error state views
    private TextView tvErrorTitle;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    // Data
    private String imagePath;
    private Bitmap capturedBitmap;
    private DetectedStarsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plate_solve_result);

        // Inject dependencies
        ((AstroApplication) getApplication()).getAppComponent().inject(this);

        // Get image path from intent
        imagePath = getIntent().getStringExtra(PhotoCaptureActivity.EXTRA_IMAGE_PATH);
        if (imagePath == null || imagePath.isEmpty()) {
            Log.e(TAG, "No image path provided");
            Toast.makeText(this, R.string.error_file_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        setupRecyclerView();

        // Load and display the captured image
        loadCapturedImage();

        // Start plate solving
        startPlateSolve();
    }

    /**
     * Initializes view references.
     */
    private void initializeViews() {
        ivCapturedPhoto = findViewById(R.id.ivCapturedPhoto);
        starOverlayView = findViewById(R.id.starOverlayView);
        resultPanel = findViewById(R.id.resultPanel);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        errorState = findViewById(R.id.errorState);
        rvDetectedStars = findViewById(R.id.rvDetectedStars);
        tvDetectedStarsHeader = findViewById(R.id.tvDetectedStarsHeader);

        // Result panel
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        tvStatusText = findViewById(R.id.tvStatusText);
        tvMatchedStars = findViewById(R.id.tvMatchedStars);
        tvCenterRa = findViewById(R.id.tvCenterRa);
        tvCenterDec = findViewById(R.id.tvCenterDec);
        tvFov = findViewById(R.id.tvFov);
        tvRoll = findViewById(R.id.tvRoll);

        // Error state
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnRetry = findViewById(R.id.btnRetry);
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

        // Retry button
        btnRetry.setOnClickListener(v -> {
            // Go back to capture activity
            finish();
        });
    }

    /**
     * Sets up the RecyclerView for detected stars.
     */
    private void setupRecyclerView() {
        adapter = new DetectedStarsAdapter();
        rvDetectedStars.setLayoutManager(new LinearLayoutManager(this));
        rvDetectedStars.setAdapter(adapter);
    }

    /**
     * Loads the captured image from file and displays it.
     */
    private void loadCapturedImage() {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: " + imagePath);
                showError(getString(R.string.error_file_not_found));
                return;
            }

            capturedBitmap = BitmapFactory.decodeFile(imagePath);
            if (capturedBitmap == null) {
                Log.e(TAG, "Failed to decode image: " + imagePath);
                showError(getString(R.string.error_data_load));
                return;
            }

            ivCapturedPhoto.setImageBitmap(capturedBitmap);
            starOverlayView.setImageDimensions(capturedBitmap.getWidth(), capturedBitmap.getHeight());

            Log.d(TAG, "Image loaded: " + capturedBitmap.getWidth() + "x" + capturedBitmap.getHeight());

        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            showError(getString(R.string.error_data_load));
        }
    }

    /**
     * Starts the plate solving process.
     */
    private void startPlateSolve() {
        showLoading(true);

        // Check if service is initialized
        if (!plateSolveService.isInitialized()) {
            Log.d(TAG, "PlateSolveService not initialized, initializing now...");
            plateSolveService.initialize();
            // Wait a bit and try again
            // In production, you'd want a proper callback system
            loadingOverlay.postDelayed(this::performPlateSolve, 2000);
        } else {
            performPlateSolve();
        }
    }

    /**
     * Performs the actual plate solve operation.
     */
    private void performPlateSolve() {
        try {
            // Read image bytes
            byte[] imageBytes = readImageBytes(imagePath);
            if (imageBytes == null) {
                showError(getString(R.string.error_file_not_found));
                return;
            }

            Log.d(TAG, "Starting plate solve with " + imageBytes.length + " bytes");

            plateSolveService.solveImage(imageBytes, DEFAULT_FOV_HINT, new PlateSolveCallback() {
                @Override
                public void onSuccess(@NonNull PlateSolveResult result) {
                    Log.d(TAG, "Plate solve result: " + result);
                    runOnUiThread(() -> displayResult(result));
                }

                @Override
                public void onError(@NonNull String message) {
                    Log.e(TAG, "Plate solve error: " + message);
                    runOnUiThread(() -> showError(message));
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error during plate solve", e);
            showError(e.getMessage());
        }
    }

    /**
     * Reads an image file into a byte array.
     *
     * @param path The path to the image file
     * @return The image bytes, or null if reading failed
     */
    private byte[] readImageBytes(String path) {
        try (FileInputStream fis = new FileInputStream(path)) {
            File file = new File(path);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        } catch (IOException e) {
            Log.e(TAG, "Error reading image file", e);
            return null;
        }
    }

    /**
     * Displays the plate solve result.
     *
     * @param result The solve result
     */
    private void displayResult(@NonNull PlateSolveResult result) {
        showLoading(false);

        if (result.isSuccess()) {
            displaySuccessResult(result);
        } else {
            displayFailureResult(result);
        }
    }

    /**
     * Displays a successful solve result.
     *
     * @param result The successful result
     */
    private void displaySuccessResult(@NonNull PlateSolveResult result) {
        // Show result panel
        resultPanel.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
        rvDetectedStars.setVisibility(View.VISIBLE);
        tvDetectedStarsHeader.setVisibility(View.VISIBLE);

        // Update status
        ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info);
        ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.success));
        tvStatusText.setText(R.string.plate_solve_success);
        tvStatusText.setTextColor(ContextCompat.getColor(this, R.color.success));

        // Update matched stars count
        tvMatchedStars.setText(getString(R.string.plate_solve_matched_stars, result.getStarsMatched()));

        // Update coordinates
        tvCenterRa.setText(formatRa(result.getCenterRa()));
        tvCenterDec.setText(formatDec(result.getCenterDec()));
        tvFov.setText(String.format("%.1f\u00B0", result.getFov()));
        tvRoll.setText(String.format("%.1f\u00B0", result.getRoll()));

        // Enrich detected stars with star data from repository
        List<DetectedStar> enrichedStars = enrichStarsWithData(result.getDetectedStars());

        // Update overlay
        starOverlayView.setDetectedStars(enrichedStars);

        // Update list
        adapter.setStars(enrichedStars);
    }

    /**
     * Displays a failure result.
     *
     * @param result The failure result
     */
    private void displayFailureResult(@NonNull PlateSolveResult result) {
        // Show error state
        resultPanel.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        rvDetectedStars.setVisibility(View.GONE);
        tvDetectedStarsHeader.setVisibility(View.GONE);

        // Update error message based on status
        switch (result.getStatus()) {
            case NO_MATCH:
                tvErrorTitle.setText(R.string.plate_solve_no_match);
                tvErrorMessage.setText(R.string.plate_solve_no_match_message);
                break;

            case NOT_ENOUGH_STARS:
                tvErrorTitle.setText(R.string.plate_solve_not_enough_stars);
                tvErrorMessage.setText(getString(R.string.plate_solve_not_enough_stars_message,
                        result.getTotalStarsDetected()));
                break;

            case ERROR:
            default:
                tvErrorTitle.setText(R.string.plate_solve_error);
                String errorMsg = result.getErrorMessage();
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    tvErrorMessage.setText(getString(R.string.plate_solve_error_message, errorMsg));
                } else {
                    tvErrorMessage.setText(R.string.error_generic);
                }
                break;
        }
    }

    /**
     * Enriches detected stars with data from the star repository.
     *
     * @param stars The detected stars
     * @return Stars with added StarData where available
     */
    private List<DetectedStar> enrichStarsWithData(@NonNull List<DetectedStar> stars) {
        List<DetectedStar> enriched = new ArrayList<>();
        for (DetectedStar star : stars) {
            // Try to find star data by Hipparcos ID
            StarData starData = starRepository.getStarByHipparcosId(star.getHipId());
            if (starData != null) {
                // Create new DetectedStar with star data
                DetectedStar enrichedStar = DetectedStar.builder()
                        .setHipId(star.getHipId())
                        .setPixelX(star.getPixelX())
                        .setPixelY(star.getPixelY())
                        .setStarData(starData)
                        .build();
                enriched.add(enrichedStar);
            } else {
                enriched.add(star);
            }
        }
        return enriched;
    }

    /**
     * Shows an error message.
     *
     * @param message The error message
     */
    private void showError(@NonNull String message) {
        showLoading(false);
        resultPanel.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        rvDetectedStars.setVisibility(View.GONE);
        tvDetectedStarsHeader.setVisibility(View.GONE);

        tvErrorTitle.setText(R.string.plate_solve_failed);
        tvErrorMessage.setText(message);
    }

    /**
     * Shows or hides the loading overlay.
     *
     * @param show true to show loading, false to hide
     */
    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Formats right ascension for display.
     *
     * @param raDegrees RA in degrees
     * @return Formatted string (e.g., "5h 35m")
     */
    private String formatRa(float raDegrees) {
        float raHours = raDegrees / 15.0f;
        int hours = (int) raHours;
        int minutes = (int) ((raHours - hours) * 60);
        return String.format("%dh %dm", hours, minutes);
    }

    /**
     * Formats declination for display.
     *
     * @param decDegrees Dec in degrees
     * @return Formatted string (e.g., "-5° 23'")
     */
    private String formatDec(float decDegrees) {
        String sign = decDegrees >= 0 ? "+" : "";
        int degrees = (int) decDegrees;
        int minutes = (int) (Math.abs(decDegrees - degrees) * 60);
        return String.format("%s%d\u00B0 %d'", sign, degrees, minutes);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capturedBitmap != null && !capturedBitmap.isRecycled()) {
            capturedBitmap.recycle();
        }
    }

    // ===================================================================
    // RecyclerView Adapter for Detected Stars
    // ===================================================================

    /**
     * Adapter for displaying detected stars in a RecyclerView.
     */
    private class DetectedStarsAdapter extends RecyclerView.Adapter<DetectedStarsAdapter.StarViewHolder> {

        private final List<DetectedStar> stars = new ArrayList<>();

        void setStars(@NonNull List<DetectedStar> newStars) {
            stars.clear();
            stars.addAll(newStars);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public StarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_detected_star, parent, false);
            return new StarViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StarViewHolder holder, int position) {
            DetectedStar star = stars.get(position);
            holder.bind(star);
        }

        @Override
        public int getItemCount() {
            return stars.size();
        }

        /**
         * ViewHolder for detected star items.
         */
        class StarViewHolder extends RecyclerView.ViewHolder {

            private final TextView tvStarName;
            private final TextView tvHipId;
            private final TextView tvPixelCoords;

            StarViewHolder(@NonNull View itemView) {
                super(itemView);
                tvStarName = itemView.findViewById(R.id.tvStarName);
                tvHipId = itemView.findViewById(R.id.tvHipId);
                tvPixelCoords = itemView.findViewById(R.id.tvPixelCoords);
            }

            void bind(@NonNull DetectedStar star) {
                tvStarName.setText(star.getDisplayName());
                tvHipId.setText(getString(R.string.plate_solve_hip_id, star.getHipId()));
                tvPixelCoords.setText(getString(R.string.plate_solve_pixel_coords,
                        star.getPixelX(), star.getPixelY()));

                // Click to view star details if star data is available
                if (star.hasStarData()) {
                    itemView.setOnClickListener(v -> {
                        StarData data = star.getStarData();
                        if (data != null) {
                            Intent intent = new Intent(PlateSolveResultActivity.this,
                                    StarInfoActivity.class);
                            intent.putExtra(StarInfoActivity.EXTRA_STAR_ID, data.getId());
                            intent.putExtra(StarInfoActivity.EXTRA_STAR_NAME, data.getName());
                            intent.putExtra(StarInfoActivity.EXTRA_STAR_RA, data.getRa());
                            intent.putExtra(StarInfoActivity.EXTRA_STAR_DEC, data.getDec());
                            intent.putExtra(StarInfoActivity.EXTRA_STAR_MAGNITUDE, data.getMagnitude());
                            startActivity(intent);
                        }
                    });
                } else {
                    itemView.setOnClickListener(null);
                }
            }
        }
    }
}
