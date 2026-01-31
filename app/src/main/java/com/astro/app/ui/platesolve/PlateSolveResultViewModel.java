package com.astro.app.ui.platesolve;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.StarRepository;
import com.astro.app.ml.PlateSolveCallback;
import com.astro.app.ml.PlateSolveResult;
import com.astro.app.ml.PlateSolveService;
import com.astro.app.ml.model.DetectedStar;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for PlateSolveResultActivity.
 *
 * <p>Manages the plate solving process and result data, surviving configuration
 * changes like screen rotation.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Exposes solve results via LiveData</li>
 *   <li>Manages loading state</li>
 *   <li>Enriches detected stars with repository data</li>
 * </ul>
 *
 * @see PlateSolveResultActivity
 * @see PlateSolveService
 */
public class PlateSolveResultViewModel extends ViewModel {

    private static final String TAG = "PlateSolveResultVM";

    /** Default FOV hint for smartphone cameras */
    private static final float DEFAULT_FOV_HINT = 70f;

    // LiveData
    private final MutableLiveData<PlateSolveResult> solveResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<DetectedStar>> enrichedStars = new MutableLiveData<>();

    // Dependencies (set by activity)
    @Nullable
    private PlateSolveService plateSolveService;

    @Nullable
    private StarRepository starRepository;

    /**
     * Returns the solve result as LiveData.
     *
     * @return LiveData containing the solve result
     */
    @NonNull
    public LiveData<PlateSolveResult> getSolveResult() {
        return solveResult;
    }

    /**
     * Returns the loading state as LiveData.
     *
     * @return LiveData containing true if solving is in progress
     */
    @NonNull
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    /**
     * Returns error messages as LiveData.
     *
     * @return LiveData containing error message, or null if no error
     */
    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns enriched detected stars as LiveData.
     *
     * <p>Enriched stars include StarData from the repository where available.</p>
     *
     * @return LiveData containing list of enriched detected stars
     */
    @NonNull
    public LiveData<List<DetectedStar>> getEnrichedStars() {
        return enrichedStars;
    }

    /**
     * Sets the PlateSolveService dependency.
     *
     * @param service The plate solve service
     */
    public void setPlateSolveService(@NonNull PlateSolveService service) {
        this.plateSolveService = service;
    }

    /**
     * Sets the StarRepository dependency.
     *
     * @param repository The star repository
     */
    public void setStarRepository(@NonNull StarRepository repository) {
        this.starRepository = repository;
    }

    /**
     * Solves an image for celestial coordinates.
     *
     * <p>Results are delivered via {@link #getSolveResult()}.</p>
     *
     * @param imageBytes The image data as a byte array
     */
    public void solveImage(@NonNull byte[] imageBytes) {
        solveImage(imageBytes, DEFAULT_FOV_HINT);
    }

    /**
     * Solves an image for celestial coordinates with a custom FOV hint.
     *
     * @param imageBytes The image data as a byte array
     * @param fovHint    Estimated field of view in degrees
     */
    public void solveImage(@NonNull byte[] imageBytes, float fovHint) {
        if (plateSolveService == null) {
            errorMessage.postValue("PlateSolveService not initialized");
            return;
        }

        isLoading.postValue(true);

        plateSolveService.solveImage(imageBytes, fovHint, new PlateSolveCallback() {
            @Override
            public void onSuccess(@NonNull PlateSolveResult result) {
                isLoading.postValue(false);
                solveResult.postValue(result);

                // Enrich stars with repository data
                if (result.isSuccess()) {
                    List<DetectedStar> enriched = enrichStarsWithData(result.getDetectedStars());
                    enrichedStars.postValue(enriched);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
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
            if (starRepository != null) {
                StarData starData = starRepository.getStarByHipparcosId(star.getHipId());
                if (starData != null) {
                    DetectedStar enrichedStar = DetectedStar.builder()
                            .setHipId(star.getHipId())
                            .setPixelX(star.getPixelX())
                            .setPixelY(star.getPixelY())
                            .setStarData(starData)
                            .build();
                    enriched.add(enrichedStar);
                    continue;
                }
            }
            enriched.add(star);
        }

        return enriched;
    }

    /**
     * Clears the current error message.
     */
    public void clearError() {
        errorMessage.postValue(null);
    }
}
