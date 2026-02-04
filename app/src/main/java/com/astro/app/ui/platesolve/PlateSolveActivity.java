package com.astro.app.ui.platesolve;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.astro.app.R;
import com.astro.app.native_.AstrometryNative;
import com.astro.app.native_.NativePlateSolver;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test activity for plate solving functionality.
 * Allows picking an image from gallery and running star detection + plate solving.
 */
public class PlateSolveActivity extends AppCompatActivity {
    private static final String TAG = "PlateSolveActivity";

    private ImageView imageView;
    private TextView tvStatus;
    private TextView tvResults;
    private Button btnPickImage;
    private Button btnDetectStars;
    private Button btnSolve;
    private ProgressBar progressBar;

    private Bitmap currentBitmap;
    private Bitmap originalBitmap;
    private List<AstrometryNative.NativeStar> detectedStars;
    private NativePlateSolver solver;
    private ExecutorService executor;
    private CheckBox cbShowStars;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    loadImage(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plate_solve);

        imageView = findViewById(R.id.imageView);
        tvStatus = findViewById(R.id.tvStatus);
        tvResults = findViewById(R.id.tvResults);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnDetectStars = findViewById(R.id.btnDetectStars);
        btnSolve = findViewById(R.id.btnSolve);
        progressBar = findViewById(R.id.progressBar);
        cbShowStars = findViewById(R.id.cbShowStars);

        executor = Executors.newSingleThreadExecutor();
        solver = new NativePlateSolver(this);

        // Toggle star overlay
        if (cbShowStars != null) {
            cbShowStars.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateImageDisplay();
            });
        }

        // Check native library and load index files
        if (!AstrometryNative.isLibraryLoaded()) {
            tvStatus.setText("ERROR: Native library not loaded!");
            btnDetectStars.setEnabled(false);
            btnSolve.setEnabled(false);
        } else {
            // Load index files from assets at startup
            int indexCount = 0;
            try {
                indexCount = solver.loadIndexesFromAssets("indexes");
                Log.i(TAG, "Loaded " + indexCount + " index files from assets");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load index files: " + e.getMessage());
            }

            if (indexCount > 0) {
                tvStatus.setText("Ready! " + indexCount + " index files loaded. Pick an image.");
            } else {
                tvStatus.setText("Warning: No index files found. Solve will not work.");
            }
        }

        btnPickImage.setOnClickListener(v -> pickImage());
        btnDetectStars.setOnClickListener(v -> detectStars());
        btnSolve.setOnClickListener(v -> solvePlate());

        btnDetectStars.setEnabled(false);
        btnSolve.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Star Field Image"));
    }

    private void loadImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (originalBitmap != null) {
                currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                imageView.setImageBitmap(currentBitmap);
                tvStatus.setText(String.format(Locale.US, "Image loaded: %dx%d",
                        currentBitmap.getWidth(), currentBitmap.getHeight()));
                tvResults.setText("");
                btnDetectStars.setEnabled(true);
                btnSolve.setEnabled(false);
                detectedStars = null;
                if (cbShowStars != null) {
                    cbShowStars.setChecked(false);
                    cbShowStars.setEnabled(false);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load image", e);
            tvStatus.setText("Failed to load image: " + e.getMessage());
        }
    }

    private void updateImageDisplay() {
        if (originalBitmap == null) return;

        currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

        if (cbShowStars != null && cbShowStars.isChecked() && detectedStars != null) {
            drawStarsOnBitmap(currentBitmap, detectedStars);
        }

        imageView.setImageBitmap(currentBitmap);
    }

    private void drawStarsOnBitmap(Bitmap bitmap, List<AstrometryNative.NativeStar> stars) {
        Canvas canvas = new Canvas(bitmap);

        // Find max flux for scaling
        float maxFlux = 0;
        for (AstrometryNative.NativeStar s : stars) {
            if (s.flux > maxFlux) maxFlux = s.flux;
        }

        Paint circlePaint = new Paint();
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(2);
        circlePaint.setAntiAlias(true);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(24);
        textPaint.setAntiAlias(true);

        // Sort by flux (brightest first)
        stars.sort((a, b) -> Float.compare(b.flux, a.flux));

        for (int i = 0; i < stars.size(); i++) {
            AstrometryNative.NativeStar s = stars.get(i);

            // Circle size based on flux (brighter = bigger)
            float radius = 5 + 15 * (s.flux / maxFlux);

            // Color: bright stars are yellow, dim stars are cyan
            if (i < 20) {
                circlePaint.setColor(Color.YELLOW);
            } else if (i < 100) {
                circlePaint.setColor(Color.GREEN);
            } else {
                circlePaint.setColor(Color.CYAN);
            }

            canvas.drawCircle(s.x, s.y, radius, circlePaint);

            // Label top 10 brightest stars
            if (i < 10) {
                canvas.drawText(String.valueOf(i + 1), s.x + radius + 2, s.y - radius, textPaint);
            }
        }

        // Draw legend
        textPaint.setTextSize(32);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(stars.size() + " stars detected", 20, 50, textPaint);
    }

    private void detectStars() {
        if (currentBitmap == null) {
            Toast.makeText(this, "No image loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        tvStatus.setText("Detecting stars...");

        executor.execute(() -> {
            long startTime = System.currentTimeMillis();

            List<AstrometryNative.NativeStar> stars = solver.detectStars(currentBitmap);

            long elapsed = System.currentTimeMillis() - startTime;

            runOnUiThread(() -> {
                setLoading(false);

                if (stars != null && !stars.isEmpty()) {
                    detectedStars = stars;

                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format(Locale.US, "Detected %d stars in %d ms\n\n",
                            stars.size(), elapsed));

                    sb.append("Top 10 brightest stars:\n");
                    // Sort by flux (brightness)
                    stars.sort((a, b) -> Float.compare(b.flux, a.flux));

                    for (int i = 0; i < Math.min(10, stars.size()); i++) {
                        AstrometryNative.NativeStar s = stars.get(i);
                        sb.append(String.format(Locale.US, "%2d. (%.1f, %.1f) flux=%.0f\n",
                                i + 1, s.x, s.y, s.flux));
                    }

                    tvResults.setText(sb.toString());
                    tvStatus.setText(String.format(Locale.US, "Found %d stars", stars.size()));
                    btnSolve.setEnabled(true);

                    // Enable star overlay checkbox
                    if (cbShowStars != null) {
                        cbShowStars.setEnabled(true);
                        cbShowStars.setChecked(true);  // Auto-show stars
                    }
                    updateImageDisplay();
                } else {
                    tvStatus.setText("No stars detected");
                    tvResults.setText("Try adjusting detection parameters or use a different image.");
                    btnSolve.setEnabled(false);
                }
            });
        });
    }

    private void solvePlate() {
        if (currentBitmap == null) {
            Toast.makeText(this, "No image loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        tvStatus.setText("Solving plate... (this requires index files)");

        executor.execute(() -> {
            long startTime = System.currentTimeMillis();

            solver.solve(currentBitmap, new NativePlateSolver.SolveCallback() {
                @Override
                public void onProgress(String message) {
                    runOnUiThread(() -> tvStatus.setText(message));
                }

                @Override
                public void onSuccess(AstrometryNative.SolveResult result) {
                    long elapsed = System.currentTimeMillis() - startTime;

                    runOnUiThread(() -> {
                        setLoading(false);

                        StringBuilder sb = new StringBuilder();
                        sb.append(String.format(Locale.US, "=== SOLVED in %d ms ===\n\n", elapsed));
                        sb.append(String.format(Locale.US, "RA:  %.4f°\n", result.ra));
                        sb.append(String.format(Locale.US, "Dec: %.4f°\n", result.dec));
                        sb.append(String.format(Locale.US, "Pixel scale: %.2f arcsec/pix\n", result.pixelScale));
                        sb.append(String.format(Locale.US, "Rotation: %.1f°\n", result.rotation));
                        sb.append(String.format(Locale.US, "Log-odds: %.1f\n", result.logOdds));
                        sb.append(String.format(Locale.US, "\nReference pixel: (%.1f, %.1f)\n",
                                result.crpixX, result.crpixY));
                        sb.append(String.format(Locale.US, "CD matrix:\n  [%.6f, %.6f]\n  [%.6f, %.6f]",
                                result.cd[0], result.cd[1], result.cd[2], result.cd[3]));

                        tvResults.setText(sb.toString());
                        tvStatus.setText("Plate solved successfully!");
                    });
                }

                @Override
                public void onFailure(String error) {
                    long elapsed = System.currentTimeMillis() - startTime;

                    runOnUiThread(() -> {
                        setLoading(false);
                        tvStatus.setText("Solve failed: " + error);
                        tvResults.setText(String.format(Locale.US,
                                "Failed after %d ms\n\n" +
                                "Possible reasons:\n" +
                                "- No index files installed\n" +
                                "- Wrong pixel scale range\n" +
                                "- Not enough stars detected\n" +
                                "- Image doesn't contain recognizable star field\n\n" +
                                "To install index files, place .fits files in:\n" +
                                "assets/indexes/ or /sdcard/astrometry/indexes/",
                                elapsed));
                    });
                }
            });
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPickImage.setEnabled(!loading);
        btnDetectStars.setEnabled(!loading && currentBitmap != null);
        btnSolve.setEnabled(!loading && detectedStars != null);
    }
}
