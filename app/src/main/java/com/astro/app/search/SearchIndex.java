package com.astro.app.search;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.core.control.SolarSystemBody;
import com.astro.app.core.control.space.Universe;
import com.astro.app.core.math.RaDec;
import com.astro.app.data.model.ConstellationData;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.StarRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates search functionality across all celestial object types.
 *
 * <p>SearchIndex provides unified search across stars, planets, and constellations.
 * It uses a {@link PrefixStore} for efficient autocomplete and maintains a lookup
 * map for exact matches.</p>
 *
 * <h3>Indexed Object Types:</h3>
 * <ul>
 *   <li>Stars - From star catalog</li>
 *   <li>Planets - Sun, Moon, and planets</li>
 *   <li>Constellations - 88 modern constellations</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * SearchIndex index = new SearchIndex(starRepository, constellationRepository, universe);
 * index.buildIndex();
 *
 * // Autocomplete suggestions
 * Set<String> suggestions = index.getAutocompleteSuggestions("sir");  // Returns {"Sirius"}
 *
 * // Full search
 * List<SearchResult> results = index.search("Sirius");
 * }</pre>
 */
public class SearchIndex {

    private static final String TAG = "SearchIndex";

    /** Prefix store for autocomplete */
    private final PrefixStore prefixStore = new PrefixStore();

    /** Map of lowercase name to search result */
    private final Map<String, SearchResult> searchMap = new HashMap<>();

    /** Star repository for star data */
    @Nullable
    private final StarRepository starRepository;

    /** Constellation repository for constellation data */
    @Nullable
    private final ConstellationRepository constellationRepository;

    /** Universe for planet positions */
    @Nullable
    private final Universe universe;

    /** Whether the index has been built */
    private boolean isIndexBuilt = false;

    /**
     * Constructs a SearchIndex that optionally uses the provided data sources to populate the index.
     *
     * @param starRepository          source of star data; may be null to disable star indexing
     * @param constellationRepository source of constellation data; may be null to disable constellation indexing
     * @param universe                source for planet positions; may be null to disable planet indexing and updates
     */
    public SearchIndex(@Nullable StarRepository starRepository,
                       @Nullable ConstellationRepository constellationRepository,
                       @Nullable Universe universe) {
        this.starRepository = starRepository;
        this.constellationRepository = constellationRepository;
        this.universe = universe;
    }

    /**
     * Constructs the in-memory search index by clearing existing entries and indexing any available
     * stars, planets, and constellations from the provided data sources.
     *
     * <p>After this method completes, {@link #isIndexBuilt()} returns {@code true}. Any null data
     * sources are skipped.</p>
     */
    public void buildIndex() {
        Log.d(TAG, "Building search index...");
        long startTime = System.currentTimeMillis();

        prefixStore.clear();
        searchMap.clear();

        // Index stars
        if (starRepository != null) {
            indexStars();
        }

        // Index planets
        if (universe != null) {
            indexPlanets();
        }

        // Index constellations
        if (constellationRepository != null) {
            indexConstellations();
        }

        isIndexBuilt = true;
        long elapsed = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Search index built in " + elapsed + "ms with " + searchMap.size() + " entries");
    }

    /**
         * Indexes named stars from the star repository into the search index.
         *
         * For each StarData returned by the repository, if the star has a non-null, non-empty
         * name that does not start with "Star ", creates a STAR SearchResult using the star's
         * RA/Dec and id and adds it to the index. Logs the number of stars indexed.
         */
    private void indexStars() {
        List<StarData> stars = starRepository.getAllStars();
        int count = 0;

        for (StarData star : stars) {
            String name = star.getName();
            if (name != null && !name.isEmpty() && !name.startsWith("Star ")) {
                addToIndex(name, star.getRa(), star.getDec(),
                        SearchResult.ObjectType.STAR, star.getId());
                count++;
            }
        }

        Log.d(TAG, "Indexed " + count + " named stars");
    }

    /**
     * Adds entries for all solar system bodies (except Earth) to the search index by querying the Universe for their current RA/Dec.
     *
     * For each body this method obtains its RA/Dec for the current time and inserts a corresponding SearchResult into the index; bodies that cannot be resolved are skipped and logged. The method updates the internal index and logs the number of planets indexed.
     */
    private void indexPlanets() {
        Date now = new Date();
        int count = 0;

        for (SolarSystemBody body : SolarSystemBody.values()) {
            if (body == SolarSystemBody.Earth) {
                continue;
            }

            try {
                RaDec raDec = universe.getRaDec(body, now);
                SearchResult.ObjectType type = getObjectType(body);
                addToIndex(body.name(), raDec.getRa(), raDec.getDec(), type, body.name());
                count++;
            } catch (Exception e) {
                Log.e(TAG, "Error indexing planet " + body.name() + ": " + e.getMessage());
            }
        }

        Log.d(TAG, "Indexed " + count + " planets");
    }

    /**
     * Adds named constellations from the repository to the search index using each constellation's center coordinates.
     *
     * For each constellation with a non-null, non-empty name, creates an index entry with type CONSTELLATION and the constellation's id. Constellations without names are skipped.
     */
    private void indexConstellations() {
        List<ConstellationData> constellations = constellationRepository.getAllConstellations();
        int count = 0;

        for (ConstellationData constellation : constellations) {
            String name = constellation.getName();
            if (name != null && !name.isEmpty()) {
                // Use the center of the constellation's bounding box
                float centerRa = constellation.getCenterRa();
                float centerDec = constellation.getCenterDec();

                addToIndex(name, centerRa, centerDec,
                        SearchResult.ObjectType.CONSTELLATION, constellation.getId());
                count++;
            }
        }

        Log.d(TAG, "Indexed " + count + " constellations");
    }

    /**
     * Add a named object to the autocomplete store and exact-name lookup.
     *
     * @param name     display name used for autocomplete and exact matching
     * @param ra       right ascension in degrees for the object's position
     * @param dec      declination in degrees for the object's position
     * @param type     the object's SearchResult.ObjectType (e.g., STAR, PLANET, CONSTELLATION)
     * @param objectId unique identifier for the object (repository ID or canonical name)
     */
    private void addToIndex(String name, float ra, float dec,
                            SearchResult.ObjectType type, String objectId) {
        prefixStore.add(name);
        SearchResult result = new SearchResult(name, ra, dec, type, objectId);
        searchMap.put(name.toLowerCase(Locale.ROOT), result);
    }

    /**
     * Map a SolarSystemBody to the corresponding SearchResult.ObjectType.
     *
     * @param body the solar system body to classify
     * @return `SUN` for Sun, `MOON` for Moon, `PLANET` for any other body
     */
    private SearchResult.ObjectType getObjectType(SolarSystemBody body) {
        switch (body) {
            case Sun:
                return SearchResult.ObjectType.SUN;
            case Moon:
                return SearchResult.ObjectType.MOON;
            default:
                return SearchResult.ObjectType.PLANET;
        }
    }

    /**
     * Retrieve autocomplete suggestions that start with the given prefix.
     *
     * @param prefix the prefix to match
     * @return a set of matching names; empty if the index is not built or no matches exist
     */
    @NonNull
    public Set<String> getAutocompleteSuggestions(@NonNull String prefix) {
        if (!isIndexBuilt) {
            Log.w(TAG, "Search index not built, returning empty suggestions");
            return java.util.Collections.emptySet();
        }
        return prefixStore.queryByPrefix(prefix);
    }

    /**
     * Finds indexed objects whose names match the query.
     *
     * <p>Performs an exact-name lookup first; if an exact match exists it is returned as the sole item.
     * Otherwise performs a prefix search and returns all matching entries. If the index has not been built
     * or no matches are found, an empty list is returned.</p>
     *
     * @param query the search query string
     * @return a list of matching SearchResult objects (empty if none)
     */
    @NonNull
    public List<SearchResult> search(@NonNull String query) {
        if (!isIndexBuilt) {
            Log.w(TAG, "Search index not built, returning empty results");
            return new ArrayList<>();
        }

        List<SearchResult> results = new ArrayList<>();
        String queryLower = query.toLowerCase(Locale.ROOT);

        // Try exact match first
        SearchResult exactMatch = searchMap.get(queryLower);
        if (exactMatch != null) {
            results.add(exactMatch);
            return results;
        }

        // Fall back to prefix search
        Set<String> matches = prefixStore.queryByPrefix(query);
        for (String match : matches) {
            SearchResult result = searchMap.get(match.toLowerCase(Locale.ROOT));
            if (result != null) {
                results.add(result);
            }
        }

        return results;
    }

    /**
     * Retrieve the indexed SearchResult for an exact object name.
     *
     * @param name the exact name to look up; comparison is case-insensitive (uses {@code Locale.ROOT})
     * @return the SearchResult for the given name, or {@code null} if no matching entry exists
     */
    @Nullable
    public SearchResult getByName(@NonNull String name) {
        return searchMap.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Update indexed planet positions to those corresponding to the given observation time.
     *
     * Updates stored search entries for all solar system bodies except Earth to use RA/Dec
     * computed for the provided time while preserving each entry's object type and identifier.
     *
     * @param timeMillis the observation time in milliseconds since the Unix epoch
     */
    public void updatePlanetPositions(long timeMillis) {
        if (universe == null) {
            return;
        }

        Date date = new Date(timeMillis);
        for (SolarSystemBody body : SolarSystemBody.values()) {
            if (body == SolarSystemBody.Earth) {
                continue;
            }

            try {
                RaDec raDec = universe.getRaDec(body, date);
                String key = body.name().toLowerCase(Locale.ROOT);
                SearchResult existing = searchMap.get(key);
                if (existing != null) {
                    // Replace with updated position
                    SearchResult updated = new SearchResult(
                            body.name(), raDec.getRa(), raDec.getDec(),
                            existing.getObjectType(), body.name());
                    searchMap.put(key, updated);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating planet " + body.name() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Indicates whether the search index has been built.
     *
     * @return `true` if the index has been built, `false` otherwise.
     */
    public boolean isIndexBuilt() {
        return isIndexBuilt;
    }

    /**
     * Number of entries in the search index.
     *
     * @return the number of indexed entries
     */
    public int size() {
        return searchMap.size();
    }
}