package com.astro.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.ConstellationData;

import java.util.List;

/**
 * Repository interface for accessing constellation data.
 *
 * <p>Provides methods to query constellations from the catalog with various
 * filtering options including name search and ID lookup.</p>
 *
 * <p>Implementations should cache data in memory for efficient repeated access.</p>
 *
 * @see ConstellationData
 * @see ConstellationRepositoryImpl
 */
public interface ConstellationRepository {

    /**
     * Retrieve all constellations in the catalog.
     *
     * <p>Includes the 88 officially recognized constellations and any additional asterisms present in the catalog.</p>
     *
     * @return a non-null list of all constellations in the catalog; may be empty if loading fails
     */
    @NonNull
    List<ConstellationData> getAllConstellations();

    /**
     * Finds the constellation with the specified unique identifier.
     *
     * <p>Constellation IDs typically use the standard 3-letter IAU abbreviations (e.g., "Ori" for
     * Orion, "UMa" for Ursa Major).</p>
     *
     * @param id the constellation's unique identifier (typically a 3-letter IAU abbreviation)
     * @return the ConstellationData for the given ID, or null if no matching constellation exists
     */
    @Nullable
    ConstellationData getConstellationById(@NonNull String id);

    /**
     * Finds a constellation by its exact name.
     *
     * <p>Name matching is case-insensitive.</p>
     *
     * @param name The constellation's name (e.g., "Orion", "Ursa Major")
     * @return The constellation with the given name, or null if not found
     */
    @Nullable
    ConstellationData getConstellationByName(@NonNull String name);

    /**
         * Searches constellations whose names contain the given query using case-insensitive matching.
         *
         * <p>Partial name matches are supported (for example, "ori" matches "Orion").</p>
         *
         * @param query the partial or full name to match; matching is case-insensitive
         * @return a non-null list of constellations whose names match the query (case-insensitive, partial matches allowed)
         */
    @NonNull
    List<ConstellationData> searchConstellations(@NonNull String query);
}