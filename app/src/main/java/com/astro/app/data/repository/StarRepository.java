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
     * Returns all stars in the catalog.
     *
     * <p>This may include thousands of stars. Consider using filtered
     * methods for better performance when displaying.</p>
     *
     * @return A list of all stars, or an empty list if loading fails
     */
    @NonNull
    List<StarData> getAllStars();

    /**
     * Returns stars filtered by maximum apparent magnitude.
     *
     * <p>The magnitude scale is inverted - lower values are brighter.
     * Typical thresholds:
     * <ul>
     *   <li>1.0 - Only the brightest stars (~20 stars)</li>
     *   <li>3.0 - Bright stars visible in light-polluted areas (~200 stars)</li>
     *   <li>6.0 - Naked eye visibility limit (~5000 stars)</li>
     * </ul>
     * </p>
     *
     * @param maxMagnitude The maximum magnitude (inclusive)
     * @return A list of stars with magnitude <= maxMagnitude
     */
    @NonNull
    List<StarData> getStarsByMagnitude(float maxMagnitude);

    /**
     * Finds a star by its unique identifier.
     *
     * @param id The star's unique identifier
     * @return The star with the given ID, or null if not found
     */
    @Nullable
    StarData getStarById(@NonNull String id);

    /**
     * Finds a star by its exact name.
     *
     * <p>Name matching is case-insensitive.</p>
     *
     * @param name The star's name (e.g., "Sirius", "Betelgeuse")
     * @return The star with the given name, or null if not found
     */
    @Nullable
    StarData getStarByName(@NonNull String name);

    /**
     * Searches for stars matching the given query.
     *
     * <p>Searches both the primary name and alternate names.
     * Search is case-insensitive and matches partial names.</p>
     *
     * @param query The search query (partial name match)
     * @return A list of stars matching the query
     */
    @NonNull
    List<StarData> searchStars(@NonNull String query);

    /**
     * Returns all stars belonging to a specific constellation.
     *
     * @param constellationId The constellation's unique identifier
     *                        (typically 3-letter IAU abbreviation like "Ori")
     * @return A list of stars in the constellation
     */
    @NonNull
    List<StarData> getStarsInConstellation(@NonNull String constellationId);
}
