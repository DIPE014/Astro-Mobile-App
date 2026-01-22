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
     * Returns all constellations in the catalog.
     *
     * <p>This returns the 88 officially recognized constellations along
     * with any additional asterisms included in the catalog.</p>
     *
     * @return A list of all constellations, or an empty list if loading fails
     */
    @NonNull
    List<ConstellationData> getAllConstellations();

    /**
     * Finds a constellation by its unique identifier.
     *
     * <p>Constellation IDs typically use the standard 3-letter IAU
     * abbreviations (e.g., "Ori" for Orion, "UMa" for Ursa Major).</p>
     *
     * @param id The constellation's unique identifier
     * @return The constellation with the given ID, or null if not found
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
     * Searches for constellations matching the given query.
     *
     * <p>Search is case-insensitive and matches partial names.
     * For example, "ori" would match "Orion".</p>
     *
     * @param query The search query (partial name match)
     * @return A list of constellations matching the query
     */
    @NonNull
    List<ConstellationData> searchConstellations(@NonNull String query);
}
