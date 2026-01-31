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

    /** Map from Hipparcos ID to StarData for fast lookup */
    @Nullable
    private Map<Integer, StarData> hipparcosIdMap;

    /**
     * Creates a StarRepositoryImpl with the provided parser.
     *
     * @param protobufParser The parser for reading star data from binary files
     */
    @Inject
    public StarRepositoryImpl(@NonNull ProtobufParser protobufParser) {
        this.protobufParser = protobufParser;
    }

    @Override
    @NonNull
    public synchronized List<StarData> getAllStars() {
        ensureCacheLoaded();
        return cachedStars != null ? cachedStars : Collections.emptyList();
    }

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

    @Override
    @Nullable
    public StarData getStarById(@NonNull String id) {
        ensureCacheLoaded();
        return starIdMap != null ? starIdMap.get(id) : null;
    }

    @Override
    @Nullable
    public StarData getStarByName(@NonNull String name) {
        ensureCacheLoaded();
        return starNameMap != null ? starNameMap.get(name.toLowerCase(Locale.ROOT)) : null;
    }

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

    @Override
    @Nullable
    public StarData getStarByHipparcosId(int hipparcosId) {
        ensureCacheLoaded();
        return hipparcosIdMap != null ? hipparcosIdMap.get(hipparcosId) : null;
    }

    /**
     * Ensures that the star cache is loaded.
     * This method is synchronized to prevent multiple threads from loading simultaneously.
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
        Map<Integer, StarData> hipMap = new HashMap<>();

        for (PointElementProto proto : protos) {
            StarData star = convertProtoToStarData(proto);
            if (star != null) {
                stars.add(star);
                idMap.put(star.getId(), star);
                nameMap.put(star.getName().toLowerCase(Locale.ROOT), star);
                if (star.hasHipparcosId()) {
                    hipMap.put(star.getHipparcosId(), star);
                }
            }
        }

        // Sort stars by magnitude (brightest first)
        Collections.sort(stars, (s1, s2) -> Float.compare(s1.getMagnitude(), s2.getMagnitude()));

        cachedStars = Collections.unmodifiableList(stars);
        starIdMap = idMap;
        starNameMap = nameMap;
        hipparcosIdMap = hipMap;

        Log.d(TAG, "Loaded " + stars.size() + " stars");
    }

    /**
     * Converts a {@link PointElementProto} to a {@link StarData} object.
     *
     * @param proto The protobuf point element
     * @return A StarData object, or null if conversion fails
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
     * Generates a unique star ID from coordinates.
     *
     * @param ra  Right Ascension in degrees
     * @param dec Declination in degrees
     * @return A unique identifier string
     */
    @NonNull
    private String generateStarId(float ra, float dec) {
        // Use coordinate-based ID format for consistency
        return String.format(Locale.US, "cstar_%d_%d",
                Math.round(ra * 1000),
                Math.round(dec * 1000));
    }

    /**
     * Generates a star name from coordinates.
     *
     * <p>For stars without named designations, generates a name based on
     * the coordinate system.</p>
     *
     * @param ra  Right Ascension in degrees
     * @param dec Declination in degrees
     * @return A generated name string
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
     * Estimates apparent magnitude from rendered size.
     *
     * <p>Brighter stars have larger rendered sizes. This provides an
     * approximate inverse mapping.</p>
     *
     * @param size The rendered size in pixels
     * @return Estimated apparent magnitude
     */
    private float estimateMagnitudeFromSize(int size) {
        // Map size range [1, 8] to magnitude range [6.5, -1.5]
        // Larger size = brighter star = lower magnitude
        float normalizedSize = Math.max(1, Math.min(8, size));
        return 6.5f - ((normalizedSize - 1) / 7.0f) * 8.0f;
    }
}
