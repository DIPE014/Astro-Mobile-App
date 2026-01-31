package com.astro.app.ml.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.StarData;

/**
 * Represents a star detected during plate solving.
 *
 * <p>Contains the star's Hipparcos catalog ID, its pixel coordinates in the
 * captured image, and optionally the full {@link StarData} if found in the
 * local star repository.</p>
 *
 * <h3>Coordinate System:</h3>
 * <p>Pixel coordinates use the image coordinate system where:
 * <ul>
 *   <li>(0, 0) is the top-left corner of the image</li>
 *   <li>X increases to the right</li>
 *   <li>Y increases downward</li>
 * </ul>
 * </p>
 *
 * @see com.astro.app.ml.PlateSolveResult
 */
public class DetectedStar {

    /** The Hipparcos catalog ID for this star */
    private final int hipId;

    /** X coordinate in the image (pixels from left edge) */
    private final float pixelX;

    /** Y coordinate in the image (pixels from top edge) */
    private final float pixelY;

    /** Full star data from the repository, if available */
    @Nullable
    private final StarData starData;

    /**
     * Private constructor. Use {@link Builder} to create instances.
     *
     * @param builder The builder containing star properties
     */
    private DetectedStar(@NonNull Builder builder) {
        this.hipId = builder.hipId;
        this.pixelX = builder.pixelX;
        this.pixelY = builder.pixelY;
        this.starData = builder.starData;
    }

    /**
     * Returns the Hipparcos catalog ID for this star.
     *
     * <p>The Hipparcos catalog contains over 100,000 stars with precise
     * position and brightness measurements.</p>
     *
     * @return The Hipparcos ID (HIP number)
     */
    public int getHipId() {
        return hipId;
    }

    /**
     * Returns the X coordinate of this star in the captured image.
     *
     * @return X position in pixels from the left edge of the image
     */
    public float getPixelX() {
        return pixelX;
    }

    /**
     * Returns the Y coordinate of this star in the captured image.
     *
     * @return Y position in pixels from the top edge of the image
     */
    public float getPixelY() {
        return pixelY;
    }

    /**
     * Returns the full star data from the local repository.
     *
     * <p>This may be null if the star was not found in the local repository
     * or if the repository lookup has not been performed yet.</p>
     *
     * @return The {@link StarData} object, or null if not available
     */
    @Nullable
    public StarData getStarData() {
        return starData;
    }

    /**
     * Checks if full star data is available for this detected star.
     *
     * @return true if {@link #getStarData()} returns non-null
     */
    public boolean hasStarData() {
        return starData != null;
    }

    /**
     * Returns the display name for this star.
     *
     * <p>Uses the star name from {@link StarData} if available,
     * otherwise returns "HIP {id}".</p>
     *
     * @return A human-readable name for the star
     */
    @NonNull
    public String getDisplayName() {
        if (starData != null && starData.getName() != null) {
            return starData.getName();
        }
        return "HIP " + hipId;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("DetectedStar{hipId=%d, pixel=(%.1f, %.1f), hasData=%b}",
                hipId, pixelX, pixelY, starData != null);
    }

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
     * Builder for creating DetectedStar instances.
     *
     * <p>Example usage:
     * <pre>{@code
     * DetectedStar star = DetectedStar.builder()
     *     .setHipId(32349)
     *     .setPixelX(512.5f)
     *     .setPixelY(384.2f)
     *     .setStarData(siriusData)
     *     .build();
     * }</pre>
     * </p>
     */
    public static class Builder {

        private int hipId = 0;
        private float pixelX = 0.0f;
        private float pixelY = 0.0f;
        @Nullable
        private StarData starData = null;

        /**
         * Creates a new Builder instance.
         */
        public Builder() {
        }

        /**
         * Sets the Hipparcos catalog ID.
         *
         * @param hipId The Hipparcos ID (HIP number)
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setHipId(int hipId) {
            this.hipId = hipId;
            return this;
        }

        /**
         * Sets the X coordinate in the image.
         *
         * @param pixelX X position in pixels from the left edge
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setPixelX(float pixelX) {
            this.pixelX = pixelX;
            return this;
        }

        /**
         * Sets the Y coordinate in the image.
         *
         * @param pixelY Y position in pixels from the top edge
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setPixelY(float pixelY) {
            this.pixelY = pixelY;
            return this;
        }

        /**
         * Sets the full star data from the repository.
         *
         * @param starData The StarData object, or null if not available
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setStarData(@Nullable StarData starData) {
            this.starData = starData;
            return this;
        }

        /**
         * Builds the DetectedStar instance.
         *
         * @return A new DetectedStar with the configured properties
         * @throws IllegalStateException if hipId is not set (zero)
         */
        @NonNull
        public DetectedStar build() {
            if (hipId <= 0) {
                throw new IllegalStateException("hipId must be a positive value");
            }
            return new DetectedStar(this);
        }
    }
}
