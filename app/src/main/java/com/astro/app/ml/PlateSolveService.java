package com.astro.app.ml;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.astro.app.ml.model.DetectedStar;
import com.astro.app.ml.model.SolveStatus;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service for plate solving images using the Tetra3 algorithm.
 *
 * <p>Plate solving is the process of matching stars in an image to a star catalog
 * to determine the exact celestial coordinates the image represents. This service
 * uses the Tetra3 library (via Chaquopy) for star pattern recognition.</p>
 *
 * <h3>Initialization:</h3>
 * <p>Before using the service, call {@link #initialize()} to load the Python module
 * and Tetra3 database. This is a one-time operation that may take several seconds.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @Inject PlateSolveService plateSolveService;
 *
 * // Initialize once at app startup
 * plateSolveService.initialize();
 *
 * // Solve an image
 * byte[] imageBytes = readImageFile(photoFile);
 * plateSolveService.solveImage(imageBytes, 70f, new PlateSolveCallback() {
 *     @Override
 *     public void onSuccess(PlateSolveResult result) {
 *         if (result.isSuccess()) {
 *             Log.d(TAG, "Center: RA=" + result.getCenterRa() +
 *                        ", Dec=" + result.getCenterDec());
 *         }
 *     }
 *
 *     @Override
 *     public void onError(String message) {
 *         Log.e(TAG, "Plate solve failed: " + message);
 *     }
 * });
 * }</pre>
 *
 * <h3>Performance:</h3>
 * <p>Plate solving is CPU-intensive and runs on a background thread. Typical solve
 * times are 1-5 seconds depending on image size and star density.</p>
 *
 * @see PlateSolveResult
 * @see PlateSolveCallback
 */
@Singleton
public class PlateSolveService {

    private static final String TAG = "PlateSolveService";

    /** Name of the Tetra3 database file in assets */
    private static final String DATABASE_FILENAME = "hip_database_fov85.npz";

    /** Path to the database within assets folder */
    private static final String DATABASE_ASSET_PATH = "tetra3/" + DATABASE_FILENAME;

    /** Application context for accessing assets */
    @NonNull
    private final Context context;

    /** Executor for background work */
    @NonNull
    private final ExecutorService executor;

    /** Handler for posting results to main thread */
    @NonNull
    private final Handler mainHandler;

    /** Python tetra3_wrapper module */
    private PyObject tetra3Module;

    /** Whether the service has been initialized */
    private volatile boolean initialized = false;

    /** Path to the extracted database file */
    private String databasePath;

    /**
     * Creates a new PlateSolveService.
     *
     * <p>The service is not ready to use until {@link #initialize()} is called.</p>
     *
     * @param context The application context (not an Activity context)
     */
    @Inject
    public PlateSolveService(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Initializes the plate solve service.
     *
     * <p>This method:
     * <ol>
     *   <li>Extracts the Tetra3 database from assets to internal storage</li>
     *   <li>Initializes the Python runtime (Chaquopy)</li>
     *   <li>Loads the tetra3_wrapper Python module</li>
     *   <li>Initializes Tetra3 with the database</li>
     * </ol>
     * </p>
     *
     * <p>This operation may take several seconds and should be called during
     * app startup (e.g., in Application.onCreate() or a splash screen).</p>
     *
     * <p>If already initialized, this method returns immediately.</p>
     */
    public void initialize() {
        if (initialized) {
            Log.d(TAG, "Already initialized");
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting initialization...");

                // Extract database from assets
                databasePath = extractDatabase();
                Log.d(TAG, "Database extracted to: " + databasePath);

                // Initialize Python
                Python py = Python.getInstance();
                tetra3Module = py.getModule("tetra3_wrapper");
                Log.d(TAG, "Python module loaded");

                // Initialize Tetra3 with database
                PyObject result = tetra3Module.callAttr("initialize", databasePath);
                boolean success = result.toBoolean();

                if (success) {
                    initialized = true;
                    Log.i(TAG, "PlateSolveService initialized successfully");
                } else {
                    Log.e(TAG, "Tetra3 initialization returned false");
                }

            } catch (Exception e) {
                Log.e(TAG, "Initialization failed", e);
            }
        });
    }

    /**
     * Checks if the service has been initialized and is ready to use.
     *
     * @return true if {@link #initialize()} has completed successfully
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Solves an image to determine its celestial coordinates.
     *
     * <p>The image is processed asynchronously and results are delivered via
     * the callback on the main thread.</p>
     *
     * @param imageBytes JPEG or PNG image data as a byte array
     * @param fovHint    Estimated field of view in degrees (typically 30-90 for
     *                   smartphone cameras). A value around 70 is a good default.
     * @param callback   Callback to receive the result or error
     * @throws IllegalStateException if the service has not been initialized
     */
    public void solveImage(@NonNull byte[] imageBytes, float fovHint,
                           @NonNull PlateSolveCallback callback) {
        if (!initialized) {
            mainHandler.post(() -> callback.onError("PlateSolveService not initialized"));
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting plate solve, image size: " + imageBytes.length +
                        " bytes, FOV hint: " + fovHint);

                // Call Python solver
                PyObject result = tetra3Module.callAttr("solve_image", imageBytes, fovHint);
                String jsonResult = result.toString();
                Log.d(TAG, "Python result: " + jsonResult);

                // Parse result
                PlateSolveResult solveResult = parseResult(jsonResult);

                // Deliver result on main thread
                mainHandler.post(() -> callback.onSuccess(solveResult));

            } catch (Exception e) {
                Log.e(TAG, "Plate solve failed", e);
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
                mainHandler.post(() -> callback.onError(errorMessage));
            }
        });
    }

    /**
     * Parses the JSON result from the Python solver.
     *
     * @param jsonString JSON string from tetra3_wrapper
     * @return Parsed PlateSolveResult
     * @throws JSONException if JSON parsing fails
     */
    @NonNull
    private PlateSolveResult parseResult(@NonNull String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);

        String statusStr = json.getString("status");
        SolveStatus status = parseStatus(statusStr);

        PlateSolveResult.Builder builder = PlateSolveResult.builder()
                .setStatus(status);

        // Parse fields based on status
        switch (status) {
            case SUCCESS:
                builder.setCenterRa((float) json.getDouble("centerRa"))
                        .setCenterDec((float) json.getDouble("centerDec"))
                        .setFov((float) json.getDouble("fov"))
                        .setRoll((float) json.getDouble("roll"));

                // Parse matched stars
                if (json.has("matchedStars")) {
                    JSONArray starsArray = json.getJSONArray("matchedStars");
                    List<DetectedStar> stars = new ArrayList<>();
                    for (int i = 0; i < starsArray.length(); i++) {
                        JSONObject starJson = starsArray.getJSONObject(i);
                        DetectedStar star = DetectedStar.builder()
                                .setHipId(starJson.getInt("hipId"))
                                .setPixelX((float) starJson.getDouble("pixelX"))
                                .setPixelY((float) starJson.getDouble("pixelY"))
                                .build();
                        stars.add(star);
                    }
                    builder.setDetectedStars(stars)
                            .setStarsMatched(stars.size());
                }
                break;

            case NOT_ENOUGH_STARS:
                if (json.has("starsDetected")) {
                    builder.setTotalStarsDetected(json.getInt("starsDetected"));
                }
                break;

            case ERROR:
                if (json.has("message")) {
                    builder.setErrorMessage(json.getString("message"));
                }
                break;

            case NO_MATCH:
                // No additional fields needed
                break;
        }

        return builder.build();
    }

    /**
     * Converts a status string to a SolveStatus enum value.
     *
     * @param statusStr Status string from Python
     * @return Corresponding SolveStatus
     */
    @NonNull
    private SolveStatus parseStatus(@NonNull String statusStr) {
        switch (statusStr) {
            case "SUCCESS":
                return SolveStatus.SUCCESS;
            case "NO_MATCH":
                return SolveStatus.NO_MATCH;
            case "NOT_ENOUGH_STARS":
                return SolveStatus.NOT_ENOUGH_STARS;
            default:
                return SolveStatus.ERROR;
        }
    }

    /**
     * Extracts the Tetra3 database from assets to internal storage.
     *
     * <p>The database is only extracted once; subsequent calls return the
     * existing path if the file exists.</p>
     *
     * @return Absolute path to the extracted database file
     * @throws IOException if extraction fails
     */
    @NonNull
    private String extractDatabase() throws IOException {
        File outputFile = new File(context.getFilesDir(), DATABASE_FILENAME);

        // Skip extraction if file already exists
        if (outputFile.exists()) {
            Log.d(TAG, "Database already extracted");
            return outputFile.getAbsolutePath();
        }

        Log.d(TAG, "Extracting database from assets...");

        try (InputStream inputStream = context.getAssets().open(DATABASE_ASSET_PATH);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        Log.d(TAG, "Database extraction complete, size: " + outputFile.length() + " bytes");
        return outputFile.getAbsolutePath();
    }

    /**
     * Shuts down the executor service.
     *
     * <p>Call this when the service is no longer needed (e.g., in
     * Application.onTerminate() or Activity.onDestroy()).</p>
     */
    public void shutdown() {
        executor.shutdown();
        Log.d(TAG, "PlateSolveService shutdown");
    }
}
