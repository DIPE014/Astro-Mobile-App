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
     * Creates a search index with all data sources.
     *
     * @param starRepository          Star data source (can be null)
     * @param constellationRepository Constellation data source (can be null)
     * @param universe                Universe for planet positions (can be null)
     */
    public SearchIndex(@Nullable StarRepository starRepository,
                       @Nullable ConstellationRepository constellationRepository,
                       @Nullable Universe universe) {
        this.starRepository = starRepository;
        this.constellationRepository = constellationRepository;
        this.universe = universe;
    }

    /**
     * Builds the search index from all available data sources.
     *
     * <p>This should be called once after construction, ideally on a
     * background thread as it may take some time for large catalogs.</p>
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
     * Indexes all named stars from the star repository.
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
     * Indexes all planets from the Universe.
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
     * Indexes all constellations from the constellation repository.
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
     * Adds an entry to the search index.
     */
    private void addToIndex(String name, float ra, float dec,
                            SearchResult.ObjectType type, String objectId) {
        prefixStore.add(name);
        SearchResult result = new SearchResult(name, ra, dec, type, objectId);
        searchMap.put(name.toLowerCase(Locale.ROOT), result);
    }

    /**
     * Gets the object type for a solar system body.
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
     * Gets autocomplete suggestions for a prefix.
     *
     * @param prefix The prefix to search for
     * @return Set of matching names
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
     * Searches for objects matching the query.
     *
     * <p>First tries exact match, then falls back to prefix search.</p>
     *
     * @param query The search query
     * @return List of matching results
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
     * Gets a search result by exact name.
     *
     * @param name The exact name to look up
     * @return The search result, or null if not found
     */
    @Nullable
    public SearchResult getByName(@NonNull String name) {
        return searchMap.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Updates planet positions for a specific time.
     *
     * <p>Call this when using time travel to update planet positions.</p>
     *
     * @param timeMillis The observation time
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
     * Returns whether the index has been built.
     *
     * @return true if buildIndex() has been called
     */
    public boolean isIndexBuilt() {
        return isIndexBuilt;
    }

    /**
     * Returns the number of indexed entries.
     *
     * @return Entry count
     */
    public int size() {
        return searchMap.size();
    }
}
