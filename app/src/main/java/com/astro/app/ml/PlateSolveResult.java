package com.astro.app.ml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.ml.model.DetectedStar;
import com.astro.app.ml.model.SolveStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains the result of a plate solve operation.
 *
 * <p>A plate solve determines the celestial coordinates (RA, Dec) of the center
 * of an image by matching detected star patterns against a catalog database.</p>
 *
 * <h3>Coordinate System:</h3>
 * <ul>
 *   <li><b>Right Ascension (RA)</b>: 0-360 degrees, measured eastward from the
 *       vernal equinox</li>
 *   <li><b>Declination (Dec)</b>: -90 to +90 degrees, measured from the
 *       celestial equator (positive north)</li>
 *   <li><b>Field of View (FOV)</b>: Angular width of the image in degrees</li>
 *   <li><b>Roll</b>: Rotation of the camera relative to celestial north</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * PlateSolveResult result = PlateSolveResult.builder()
 *     .setStatus(SolveStatus.SUCCESS)
 *     .setCenterRa(83.633f)     // Orion's Belt region
 *     .setCenterDec(-1.2f)
 *     .setFov(15.5f)
 *     .setRoll(45.0f)
 *     .setDetectedStars(stars)
 *     .build();
 *
 * if (result.isSuccess()) {
 *     Log.d("PlateSolve", "Center: RA=" + result.getCenterRa() +
 *                         ", Dec=" + result.getCenterDec());
 * }
 * }</pre>
 *
 * @see SolveStatus
 * @see DetectedStar
 * @see PlateSolveService
 */
public class PlateSolveResult {

    /** The status of the plate solve operation */
    @NonNull
    private final SolveStatus status;

    /** Right ascension of the image center in degrees (0-360) */
    private final float centerRa;

    /** Declination of the image center in degrees (-90 to +90) */
    private final float centerDec;

    /** Field of view of the image in degrees */
    private final float fov;

    /** Roll angle in degrees relative to celestial north */
    private final float roll;

    /** List of stars detected and matched in the image */
    @NonNull
    private final List<DetectedStar> detectedStars;

    /** Error message if status is ERROR */
    @Nullable
    private final String errorMessage;

    /** Total number of stars detected in the image (before matching) */
    private final int totalStarsDetected;

    /** Number of stars successfully matched to the catalog */
    private final int starsMatched;

    /**
     * Private constructor. Use {@link Builder} to create instances.
     *
     * @param builder The builder containing result properties
     */
    private PlateSolveResult(@NonNull Builder builder) {
        this.status = builder.status;
        this.centerRa = builder.centerRa;
        this.centerDec = builder.centerDec;
        this.fov = builder.fov;
        this.roll = builder.roll;
        this.detectedStars = Collections.unmodifiableList(new ArrayList<>(builder.detectedStars));
        this.errorMessage = builder.errorMessage;
        this.totalStarsDetected = builder.totalStarsDetected;
        this.starsMatched = builder.starsMatched;
    }

    /**
     * Returns the status of the plate solve operation.
     *
     * @return The solve status (never null)
     */
    @NonNull
    public SolveStatus getStatus() {
        return status;
    }

    /**
     * Checks if the plate solve was successful.
     *
     * @return true if status is {@link SolveStatus#SUCCESS}
     */
    public boolean isSuccess() {
        return status == SolveStatus.SUCCESS;
    }

    /**
     * Returns the right ascension of the image center.
     *
     * <p>Only valid when {@link #isSuccess()} returns true.</p>
     *
     * @return Right ascension in degrees (0-360)
     */
    public float getCenterRa() {
        return centerRa;
    }

    /**
     * Returns the declination of the image center.
     *
     * <p>Only valid when {@link #isSuccess()} returns true.</p>
     *
     * @return Declination in degrees (-90 to +90)
     */
    public float getCenterDec() {
        return centerDec;
    }

    /**
     * Returns the field of view of the image.
     *
     * <p>This represents the angular width of the image. Only valid when
     * {@link #isSuccess()} returns true.</p>
     *
     * @return Field of view in degrees
     */
    public float getFov() {
        return fov;
    }

    /**
     * Returns the roll angle of the camera.
     *
     * <p>This represents the rotation relative to celestial north (up in the
     * equatorial coordinate system). Only valid when {@link #isSuccess()}
     * returns true.</p>
     *
     * @return Roll angle in degrees
     */
    public float getRoll() {
        return roll;
    }

    /**
     * Returns the list of detected and matched stars.
     *
     * <p>Each star includes its Hipparcos ID and pixel coordinates in the
     * original image.</p>
     *
     * @return Unmodifiable list of detected stars (never null, may be empty)
     */
    @NonNull
    public List<DetectedStar> getDetectedStars() {
        return detectedStars;
    }

    /**
     * Returns the error message if an error occurred.
     *
     * @return The error message, or null if no error
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the total number of stars detected in the image.
     *
     * <p>This count includes stars that were detected but not successfully
     * matched to the catalog.</p>
     *
     * @return Total star count
     */
    public int getTotalStarsDetected() {
        return totalStarsDetected;
    }

    /**
     * Returns the number of stars successfully matched to the catalog.
     *
     * @return Matched star count
     */
    public int getStarsMatched() {
        return starsMatched;
    }

    @Override
    @NonNull
    public String toString() {
        if (isSuccess()) {
            return String.format("PlateSolveResult{SUCCESS, center=(%.4f, %.4f), fov=%.2f, " +
                            "roll=%.2f, matched=%d/%d}",
                    centerRa, centerDec, fov, roll, starsMatched, totalStarsDetected);
        } else {
            return String.format("PlateSolveResult{%s, detected=%d, message='%s'}",
                    status, totalStarsDetected, errorMessage);
        }
    }

    // ==================== Static Factory Methods ====================

    /**
     * Creates an error result with the specified message.
     *
     * @param message The error message describing what went wrong
     * @return A PlateSolveResult with status ERROR
     */
    @NonNull
    public static PlateSolveResult error(@NonNull String message) {
        return builder()
                .setStatus(SolveStatus.ERROR)
                .setErrorMessage(message)
                .build();
    }

    /**
     * Creates a result indicating no match was found.
     *
     * @return A PlateSolveResult with status NO_MATCH
     */
    @NonNull
    public static PlateSolveResult noMatch() {
        return builder()
                .setStatus(SolveStatus.NO_MATCH)
                .build();
    }

    /**
     * Creates a result indicating not enough stars were detected.
     *
     * @param starsDetected The number of stars that were detected
     * @return A PlateSolveResult with status NOT_ENOUGH_STARS
     */
    @NonNull
    public static PlateSolveResult notEnoughStars(int starsDetected) {
        return builder()
                .setStatus(SolveStatus.NOT_ENOUGH_STARS)
                .setTotalStarsDetected(starsDetected)
                .build();
    }

    // ==================== Builder ====================

    /**
     * Creates a new Builder instance.
     *
     * @return A new Builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating PlateSolveResult instances.
     *
     * <p>Example usage:
     * <pre>{@code
     * PlateSolveResult result = PlateSolveResult.builder()
     *     .setStatus(SolveStatus.SUCCESS)
     *     .setCenterRa(83.633f)
     *     .setCenterDec(-1.2f)
     *     .setFov(15.5f)
     *     .setRoll(45.0f)
     *     .setDetectedStars(starList)
     *     .setTotalStarsDetected(25)
     *     .setStarsMatched(18)
     *     .build();
     * }</pre>
     * </p>
     */
    public static class Builder {

        @NonNull
        private SolveStatus status = SolveStatus.ERROR;
        private float centerRa = 0.0f;
        private float centerDec = 0.0f;
        private float fov = 0.0f;
        private float roll = 0.0f;
        @NonNull
        private List<DetectedStar> detectedStars = new ArrayList<>();
        @Nullable
        private String errorMessage = null;
        private int totalStarsDetected = 0;
        private int starsMatched = 0;

        /**
         * Creates a new Builder instance.
         */
        public Builder() {
        }

        /**
         * Sets the solve status.
         *
         * @param status The status of the plate solve operation
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setStatus(@NonNull SolveStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the right ascension of the image center.
         *
         * @param centerRa Right ascension in degrees (0-360)
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setCenterRa(float centerRa) {
            this.centerRa = centerRa;
            return this;
        }

        /**
         * Sets the declination of the image center.
         *
         * @param centerDec Declination in degrees (-90 to +90)
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setCenterDec(float centerDec) {
            this.centerDec = centerDec;
            return this;
        }

        /**
         * Sets the field of view.
         *
         * @param fov Field of view in degrees
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setFov(float fov) {
            this.fov = fov;
            return this;
        }

        /**
         * Sets the roll angle.
         *
         * @param roll Roll angle in degrees relative to celestial north
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setRoll(float roll) {
            this.roll = roll;
            return this;
        }

        /**
         * Sets the list of detected stars.
         *
         * @param detectedStars List of stars detected in the image
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setDetectedStars(@NonNull List<DetectedStar> detectedStars) {
            this.detectedStars = new ArrayList<>(detectedStars);
            return this;
        }

        /**
         * Adds a single detected star to the list.
         *
         * @param star The detected star to add
         * @return This builder for method chaining
         */
        @NonNull
        public Builder addDetectedStar(@NonNull DetectedStar star) {
            this.detectedStars.add(star);
            return this;
        }

        /**
         * Sets the error message.
         *
         * @param errorMessage The error message, or null if no error
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setErrorMessage(@Nullable String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * Sets the total number of stars detected.
         *
         * @param totalStarsDetected Total count of detected stars
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setTotalStarsDetected(int totalStarsDetected) {
            this.totalStarsDetected = totalStarsDetected;
            return this;
        }

        /**
         * Sets the number of matched stars.
         *
         * @param starsMatched Count of stars matched to catalog
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setStarsMatched(int starsMatched) {
            this.starsMatched = starsMatched;
            return this;
        }

        /**
         * Builds the PlateSolveResult instance.
         *
         * @return A new PlateSolveResult with the configured properties
         */
        @NonNull
        public PlateSolveResult build() {
            return new PlateSolveResult(this);
        }
    }
}
