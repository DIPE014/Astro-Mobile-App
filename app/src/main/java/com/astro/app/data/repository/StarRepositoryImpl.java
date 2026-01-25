package com.astro.app.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.StarData;
import com.astro.app.data.parser.ProtobufParser;
import com.astro.app.data.proto.SourceProto.GeocentricCoordinatesProto;
import com.astro.app.data.proto.SourceProto.PointElementProto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link StarRepository} that loads star data from binary protobuf files.
 *
 * <p>This implementation uses {@link ProtobufParser} to load star data from the
 * stars.binary asset file. Stars are converted from {@link PointElementProto}
 * to {@link StarData} objects and cached in memory for efficient access.</p>
 *
 * <h3>Caching Strategy:</h3>
 * <p>Stars are loaded lazily on first access and cached for the lifetime of the
 * application. This avoids repeated disk I/O and protobuf parsing.</p>
 *
 * <h3>Thread Safety:</h3>
 * <p>This implementation uses synchronized methods to ensure thread-safe
 * initialization of the cache.</p>
 *
 * @see StarRepository
 * @see ProtobufParser
 */
@Singleton
public class StarRepositoryImpl implements StarRepository {

    private static final String TAG = "StarRepositoryImpl";

    /** Counter for generating unique star IDs */
    private static int starIdCounter = 0;

    private final ProtobufParser protobufParser;

    /** Cached list of all stars */
    @Nullable
    private List<StarData> cachedStars;

    /** Map from star ID to StarData for fast lookup */
    @Nullable
    private Map<String, StarData> starIdMap;

    /** Map from lowercase star name to StarData for fast lookup */
    @Nullable
    private Map<String, StarData> starNameMap;

    /**
     * Constructs a StarRepositoryImpl that loads and caches star data using the provided parser.
     *
     * @param protobufParser parser used to parse star protobuf records from binary assets
     */
    @Inject
    public StarRepositoryImpl(@NonNull ProtobufParser protobufParser) {
        this.protobufParser = protobufParser;
    }

    /**
     * Provide the complete list of known stars, loading and caching them if they have not yet been loaded.
     *
     * @return the in-memory (cached) unmodifiable list of all StarData objects; an empty list if no stars are available
     */
    @Override
    @NonNull
    public synchronized List<StarData> getAllStars() {
        ensureCacheLoaded();
        return cachedStars != null ? cachedStars : Collections.emptyList();
    }

    /**
     * Filter stars to those with apparent magnitude less than or equal to the given threshold.
     *
     * @param maxMagnitude the maximum apparent magnitude threshold; stars with magnitude less than or equal to this value are included
     * @return a list of stars whose magnitude is less than or equal to {@code maxMagnitude}; may be empty
     */
    @Override
    @NonNull
    public List<StarData> getStarsByMagnitude(float maxMagnitude) {
        List<StarData> allStars = getAllStars();
        List<StarData> filtered = new ArrayList<>();

        for (StarData star : allStars) {
            if (star.getMagnitude() <= maxMagnitude) {
                filtered.add(star);
            }
        }

        return filtered;
    }

    /**
     * Retrieve a star by its unique identifier.
     *
     * @param id the unique identifier of the star
     * @return the StarData for the given id, or null if no star matches
     */
    @Override
    @Nullable
    public StarData getStarById(@NonNull String id) {
        ensureCacheLoaded();
        return starIdMap != null ? starIdMap.get(id) : null;
    }

    /**
     * Retrieves a star by its name using a case-insensitive match.
     *
     * @param name the star's name to look up (matching is case-insensitive)
     * @return the matching {@link StarData} if found, or `null` if no star has that name
     */
    @Override
    @Nullable
    public StarData getStarByName(@NonNull String name) {
        ensureCacheLoaded();
        return starNameMap != null ? starNameMap.get(name.toLowerCase(Locale.ROOT)) : null;
    }

    /**
     * Searches stars by name using a case-insensitive substring match.
     *
     * The query is matched against each star's primary name and its alternate names.
     *
     * @param query the case-insensitive substring to match against primary and alternate star names
     * @return a list of stars whose primary name or any alternate name contains the query (case-insensitive)
     */
    @Override
    @NonNull
    public List<StarData> searchStars(@NonNull String query) {
        List<StarData> allStars = getAllStars();
        List<StarData> results = new ArrayList<>();

        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (StarData star : allStars) {
            // Check primary name
            if (star.getName().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                results.add(star);
                continue;
            }

            // Check alternate names
            for (String alternateName : star.getAlternateNames()) {
                if (alternateName.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    results.add(star);
                    break;
                }
            }
        }

        return results;
    }

    /**
     * Retrieves all stars that belong to the specified constellation.
     *
     * @param constellationId the identifier of the constellation to filter by
     * @return a list of matching StarData objects; empty if no stars are found for the given constellation
     */
    @Override
    @NonNull
    public List<StarData> getStarsInConstellation(@NonNull String constellationId) {
        List<StarData> allStars = getAllStars();
        List<StarData> results = new ArrayList<>();

        for (StarData star : allStars) {
            if (constellationId.equals(star.getConstellationId())) {
                results.add(star);
            }
        }

        return results;
    }

    /**
     * Lazily loads star entries from the protobuf asset and populates in-memory caches.
     *
     * Ensures {@code cachedStars}, {@code starIdMap}, and {@code starNameMap} are initialized
     * with {@code StarData} parsed from the {@code ProtobufParser}; stars are sorted by
     * magnitude with the brightest first. If the cache is already initialized, the method
     * returns immediately.
     */
    private synchronized void ensureCacheLoaded() {
        if (cachedStars != null) {
            return;
        }

        Log.d(TAG, "Loading stars from binary file...");

        List<PointElementProto> protos = protobufParser.parseStars();
        List<StarData> stars = new ArrayList<>(protos.size());
        Map<String, StarData> idMap = new HashMap<>();
        Map<String, StarData> nameMap = new HashMap<>();

        for (PointElementProto proto : protos) {
            StarData star = convertProtoToStarData(proto);
            if (star != null) {
                stars.add(star);
                idMap.put(star.getId(), star);
                nameMap.put(star.getName().toLowerCase(Locale.ROOT), star);
            }
        }

        // Sort stars by magnitude (brightest first)
        Collections.sort(stars, (s1, s2) -> Float.compare(s1.getMagnitude(), s2.getMagnitude()));

        cachedStars = Collections.unmodifiableList(stars);
        starIdMap = idMap;
        starNameMap = nameMap;

        Log.d(TAG, "Loaded " + stars.size() + " stars");
    }

    /**
         * Create a StarData instance from a PointElementProto.
         *
         * @param proto the protobuf point element to convert; its location must be present to produce a StarData
         * @return the StarData populated from the proto, or `null` if the proto has no location or conversion fails
         */
    @Nullable
    private StarData convertProtoToStarData(@NonNull PointElementProto proto) {
        try {
            GeocentricCoordinatesProto location = proto.getLocation();
            if (location == null) {
                return null;
            }

            float ra = location.getRightAscension();
            float dec = location.getDeclination();

            // Generate a unique ID based on coordinates
            String id = generateStarId(ra, dec);

            // Generate a name based on coordinates (since protobuf doesn't include names)
            String name = generateStarName(ra, dec);

            // Calculate size based on proto size or default
            int size = proto.hasSize() ? proto.getSize() : 3;

            // Estimate magnitude from size (inverse relationship)
            float magnitude = estimateMagnitudeFromSize(size);

            return StarData.builder()
                    .setId(id)
                    .setName(name)
                    .setRa(ra)
                    .setDec(dec)
                    .setColor(proto.getColor())
                    .setSize(size)
                    .setMagnitude(magnitude)
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "Error converting proto to StarData", e);
            return null;
        }
    }

    /**
         * Create a unique identifier for a star based on its coordinates.
         *
         * @param ra  Right Ascension in degrees
         * @param dec Declination in degrees
         * @return the identifier string in the form "star_<counter>_<raRounded>_<decRounded>",
         *         where RA and DEC are multiplied by 1000 and rounded to integers and <counter> is a monotonic counter
         */
    @NonNull
    private String generateStarId(float ra, float dec) {
        // Use counter with coordinate hash for uniqueness
        return String.format(Locale.US, "star_%d_%d_%d",
                ++starIdCounter,
                Math.round(ra * 1000),
                Math.round(dec * 1000));
    }

    /**
         * Generates a synthetic star name from celestial coordinates for untitled stars.
         *
         * <p>The generated name encodes Right Ascension as hours and minutes and Declination with sign.</p>
         *
         * @param ra  Right Ascension in degrees
         * @param dec Declination in degrees
         * @return A generated name string in the form "Star HHhMMm ±DD.D"
         */
    @NonNull
    private String generateStarName(float ra, float dec) {
        // Convert RA to hours, minutes, seconds format
        float raHours = ra / 15.0f;
        int hours = (int) raHours;
        int minutes = (int) ((raHours - hours) * 60);

        // Format dec with sign
        String decSign = dec >= 0 ? "+" : "";

        return String.format(Locale.US, "Star %02dh%02dm %s%.1f",
                hours, minutes, decSign, dec);
    }

    /**
         * Estimate an apparent magnitude corresponding to a rendered star size.
         *
         * <p>Values outside the expected rendered size range are clamped to [1, 8]. Larger sizes map to
         * brighter magnitudes (numerically lower).</p>
         *
         * @param size the rendered size in pixels (values outside 1–8 are clamped)
         * @return the estimated apparent magnitude, between -1.5 (brightest) and 6.5 (dimmest)
         */
    private float estimateMagnitudeFromSize(int size) {
        // Map size range [1, 8] to magnitude range [6.5, -1.5]
        // Larger size = brighter star = lower magnitude
        float normalizedSize = Math.max(1, Math.min(8, size));
        return 6.5f - ((normalizedSize - 1) / 7.0f) * 8.0f;
    }
}