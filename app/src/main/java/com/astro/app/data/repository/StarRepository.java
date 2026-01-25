package com.astro.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.StarData;

import java.util.List;

/**
 * Repository interface for accessing star data.
 *
 * <p>Provides methods to query stars from the star catalog with various
 * filtering options including magnitude, name search, and constellation.</p>
 *
 * <p>Implementations should cache data in memory for efficient repeated access.</p>
 *
 * @see StarData
 * @see StarRepositoryImpl
 */
public interface StarRepository {

    /**
     * Retrieve the complete catalog of stars.
     *
     * <p>The returned list may contain thousands of entries; prefer filtered retrieval methods
     * for UI or other performance-sensitive scenarios.</p>
     *
     * @return a non-null list containing all stars in the catalog; may be empty
     */
    @NonNull
    List<StarData> getAllStars();

    /**
         * Filter stars by maximum apparent magnitude.
         *
         * <p>The magnitude scale is inverted: smaller values are brighter. This method returns
         * stars whose apparent magnitude is less than or equal to the provided threshold.
         * Typical thresholds:
         * <ul>
         *   <li>1.0 — only the brightest stars (≈20 stars)</li>
         *   <li>3.0 — bright stars visible in light-polluted areas (≈200 stars)</li>
         *   <li>6.0 — approximate naked-eye visibility limit (≈5000 stars)</li>
         * </ul>
         * </p>
         *
         * @param maxMagnitude the maximum apparent magnitude (inclusive); lower values indicate brighter stars
         * @return a list of stars with apparent magnitude less than or equal to {@code maxMagnitude}
         */
    @NonNull
    List<StarData> getStarsByMagnitude(float maxMagnitude);

    /**
         * Locate the star with the specified unique identifier.
         *
         * @param id the star's unique identifier (non-null)
         * @return the StarData matching the identifier, or null if no matching star is found
         */
    @Nullable
    StarData getStarById(@NonNull String id);

    /**
     * Locate a star by its primary name using case-insensitive exact matching.
     *
     * @param name the star's primary name (e.g., "Sirius", "Betelgeuse"); alternate names are not matched
     * @return the matching StarData, or `null` if no star with that name exists
     */
    @Nullable
    StarData getStarByName(@NonNull String name);

    /**
     * Finds stars whose primary or alternate names match the given query.
     *
     * <p>Matches are case-insensitive and support partial (substring) matches.</p>
     *
     * @param query the substring to match against primary and alternate names (case-insensitive)
     * @return a non-null list of StarData objects matching the query; empty if no matches
     */
    @NonNull
    List<StarData> searchStars(@NonNull String query);

    /**
     * Get all stars that belong to a specific constellation.
     *
     * @param constellationId the constellation's unique identifier (typically a 3-letter IAU abbreviation such as "Ori")
     * @return a non-null list of StarData objects in the specified constellation; an empty list if no matching stars are found
     */
    @NonNull
    List<StarData> getStarsInConstellation(@NonNull String constellationId);
}