package com.astro.app.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.StarData;
import com.astro.app.data.parser.AssetDataSource;
import com.astro.app.data.parser.ProtobufParser;
import com.astro.app.data.proto.SourceProto.GeocentricCoordinatesProto;
import com.astro.app.data.proto.SourceProto.PointElementProto;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
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
    private static final String EDUCATION_ASSET = "education_content.json";
    private static final String JSON_KEY_BRIGHTEST_STARS = "brightest_stars";
    private static final float NAMED_STAR_MATCH_TOLERANCE_DEG = 0.1f;
    private static final int COLOR_NAMED_STAR_ORANGE = 0xFFFFA500;
    private static final String BOUNDARIES_ASSET = "bound_18.json";
    private static final String CONSTELLATION_NAMES_ASSET = "constellations.json";
    private static final float RA_WRAP_THRESHOLD_HOURS = 12.0f;

    private final ProtobufParser protobufParser;
    private final AssetDataSource assetDataSource;

    /** Cached list of all stars */
    @Nullable
    private List<StarData> cachedStars;

    /** Map from star ID to StarData for fast lookup */
    @Nullable
    private Map<String, StarData> starIdMap;

    /** Map from lowercase star name to StarData for fast lookup */
    @Nullable
    private Map<String, StarData> starNameMap;
    @Nullable
    private List<ConstellationBoundary> constellationBoundaries;
    private boolean constellationBoundariesLoaded = false;
    @Nullable
    private Map<String, String> constellationIdByAbbr;

    /**
     * Creates a StarRepositoryImpl with the provided parser.
     *
     * @param protobufParser The parser for reading star data from binary files
     * @param assetDataSource The data source for reading JSON assets
     */
    @Inject
    public StarRepositoryImpl(@NonNull ProtobufParser protobufParser,
                              @NonNull AssetDataSource assetDataSource) {
        this.protobufParser = protobufParser;
        this.assetDataSource = assetDataSource;
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

        List<NamedStar> namedStars = loadNamedStars();
        java.util.Set<String> matchedNamedStars = new java.util.HashSet<>();

        for (PointElementProto proto : protos) {
            NamedStar namedStar = null;
            if (proto.hasLocation()) {
                float ra = proto.getLocation().getRightAscension();
                float dec = proto.getLocation().getDeclination();
                namedStar = findNamedStar(ra, dec, namedStars);
                if (namedStar != null) {
                    matchedNamedStars.add(namedStar.displayName);
                }
            }

            StarData star = convertProtoToStarData(proto, namedStar);
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
        if (!namedStars.isEmpty()) {
            Log.d(TAG, "Matched " + matchedNamedStars.size() + " named stars (of " + namedStars.size() + " with coordinates)");
        }
    }

    /**
     * Converts a {@link PointElementProto} to a {@link StarData} object.
     *
     * @param proto The protobuf point element
     * @return A StarData object, or null if conversion fails
     */
    @Nullable
    private StarData convertProtoToStarData(@NonNull PointElementProto proto,
                                            @Nullable NamedStar namedStar) {
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
            String generatedName = generateStarName(ra, dec);

            // Calculate size based on proto size or default
            int size = proto.hasSize() ? proto.getSize() : 3;

            // Estimate magnitude from size (inverse relationship)
            float magnitude = estimateMagnitudeFromSize(size);

            StarData.Builder builder = StarData.builder()
                    .setId(id)
                    .setRa(ra)
                    .setDec(dec)
                    .setColor(proto.getColor())
                    .setSize(size)
                    .setMagnitude(magnitude);

            String constellationId = getConstellationIdForPosition(ra, dec);
            if (constellationId != null) {
                builder.setConstellationId(constellationId);
            }

            if (namedStar != null) {
                builder.setName(namedStar.displayName);
                builder.setColor(COLOR_NAMED_STAR_ORANGE);
                builder.addAlternateName(generatedName);
                for (String alternate : namedStar.alternateNames) {
                    builder.addAlternateName(alternate);
                }
            } else {
                builder.setName(generatedName);
            }

            return builder.build();

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

    @NonNull
    private List<NamedStar> loadNamedStars() {
        List<NamedStar> namedStars = new ArrayList<>();
        int skippedNoCoords = 0;

        try (java.io.InputStream inputStream = assetDataSource.openAsset(EDUCATION_ASSET)) {
            String json = readStreamToString(inputStream);
            JSONObject root = new JSONObject(json);
            JSONArray stars = root.optJSONArray(JSON_KEY_BRIGHTEST_STARS);
            if (stars == null) {
                Log.w(TAG, "No '" + JSON_KEY_BRIGHTEST_STARS + "' array in " + EDUCATION_ASSET);
                return namedStars;
            }

            for (int i = 0; i < stars.length(); i++) {
                JSONObject star = stars.optJSONObject(i);
                if (star == null) {
                    continue;
                }
                if (!star.has("ra") || !star.has("dec")) {
                    skippedNoCoords++;
                    continue;
                }

                String displayName = star.optString("displayName", "").trim();
                if (displayName.isEmpty()) {
                    continue;
                }

                float ra = (float) star.optDouble("ra", Float.NaN);
                float dec = (float) star.optDouble("dec", Float.NaN);
                if (Float.isNaN(ra) || Float.isNaN(dec)) {
                    skippedNoCoords++;
                    continue;
                }

                List<String> alternates = new ArrayList<>();
                JSONArray altArray = star.optJSONArray("alternateNames");
                if (altArray != null) {
                    for (int j = 0; j < altArray.length(); j++) {
                        String alt = altArray.optString(j, "").trim();
                        if (!alt.isEmpty()) {
                            alternates.add(alt);
                        }
                    }
                }

                namedStars.add(new NamedStar(displayName, ra, dec, alternates));
            }

            Log.d(TAG, "Loaded " + namedStars.size() + " named stars from " + EDUCATION_ASSET);
            if (skippedNoCoords > 0) {
                Log.d(TAG, "Skipped " + skippedNoCoords + " named stars without coordinates");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load named stars from " + EDUCATION_ASSET, e);
        }

        return namedStars;
    }

    @NonNull
    private String readStreamToString(@NonNull java.io.InputStream inputStream) throws java.io.IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString(java.nio.charset.StandardCharsets.UTF_8.name());
    }

    @Nullable
    private NamedStar findNamedStar(float ra, float dec, @NonNull List<NamedStar> namedStars) {
        NamedStar bestMatch = null;
        float bestDistance = Float.MAX_VALUE;

        for (NamedStar candidate : namedStars) {
            float raDelta = angularDeltaDegrees(ra, candidate.ra);
            float decDelta = Math.abs(dec - candidate.dec);

            if (raDelta <= NAMED_STAR_MATCH_TOLERANCE_DEG && decDelta <= NAMED_STAR_MATCH_TOLERANCE_DEG) {
                float distance = raDelta * raDelta + decDelta * decDelta;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = candidate;
                }
            }
        }

        return bestMatch;
    }

    private float angularDeltaDegrees(float a, float b) {
        float delta = Math.abs(a - b);
        return delta > 180.0f ? 360.0f - delta : delta;
    }

    @Nullable
    private String getConstellationIdForPosition(float raDegrees, float decDegrees) {
        List<ConstellationBoundary> boundaries = getConstellationBoundaries();
        if (boundaries == null || boundaries.isEmpty()) {
            return null;
        }
        float raHours = raDegrees / 15.0f;
        for (ConstellationBoundary boundary : boundaries) {
            if (isPointInPolygon(raHours, decDegrees, boundary)) {
                return boundary.id;
            }
        }
        return null;
    }

    private boolean isPointInPolygon(float raHours, float decDegrees, @NonNull ConstellationBoundary boundary) {
        float adjustedRa = raHours;
        if (boundary.crossesZero && adjustedRa < RA_WRAP_THRESHOLD_HOURS) {
            adjustedRa += 24.0f;
        }

        boolean inside = false;
        int count = boundary.points.size();
        for (int i = 0, j = count - 1; i < count; j = i++) {
            BoundaryPoint pi = boundary.points.get(i);
            BoundaryPoint pj = boundary.points.get(j);

            float xi = pi.raHours;
            float xj = pj.raHours;
            if (boundary.crossesZero) {
                if (xi < RA_WRAP_THRESHOLD_HOURS) xi += 24.0f;
                if (xj < RA_WRAP_THRESHOLD_HOURS) xj += 24.0f;
            }

            float yi = pi.decDegrees;
            float yj = pj.decDegrees;

            boolean intersects = ((yi > decDegrees) != (yj > decDegrees)) &&
                    (adjustedRa < (xj - xi) * (decDegrees - yi) / (yj - yi) + xi);
            if (intersects) {
                inside = !inside;
            }
        }

        return inside;
    }

    @Nullable
    private List<ConstellationBoundary> getConstellationBoundaries() {
        if (constellationBoundariesLoaded) {
            return constellationBoundaries;
        }
        constellationBoundariesLoaded = true;
        constellationBoundaries = loadConstellationBoundaries();
        return constellationBoundaries;
    }

    @Nullable
    private List<ConstellationBoundary> loadConstellationBoundaries() {
        Map<String, String> idByAbbr = getConstellationIdByAbbr();
        if (idByAbbr == null || idByAbbr.isEmpty()) {
            Log.w(TAG, "Constellation name map missing; boundaries will not be assigned");
            return null;
        }

        try (java.io.InputStream inputStream = assetDataSource.openAsset(BOUNDARIES_ASSET)) {
            String json = readStreamToString(inputStream);
            JSONObject root = new JSONObject(json);
            JSONArray polygons = root.optJSONArray("polygons");
            if (polygons == null || polygons.length() == 0) {
                Log.w(TAG, "No 'polygons' array in " + BOUNDARIES_ASSET);
                return null;
            }

            List<ConstellationBoundary> boundaries = new ArrayList<>(polygons.length());
            for (int i = 0; i < polygons.length(); i++) {
                JSONObject polygon = polygons.optJSONObject(i);
                if (polygon == null) continue;
                String abbr = polygon.optString("constellation", "").trim().toUpperCase(Locale.ROOT);
                if (abbr.isEmpty()) continue;

                String id = idByAbbr.get(abbr);
                if (id == null || id.isEmpty()) {
                    continue;
                }

                JSONArray points = polygon.optJSONArray("points");
                if (points == null || points.length() == 0) {
                    continue;
                }

                List<BoundaryPoint> boundaryPoints = new ArrayList<>(points.length());
                float minRa = Float.MAX_VALUE;
                float maxRa = -Float.MAX_VALUE;
                for (int j = 0; j < points.length(); j++) {
                    JSONObject point = points.optJSONObject(j);
                    if (point == null) continue;
                    float raHours = (float) point.optDouble("ra_h", Float.NaN);
                    float decDegrees = (float) point.optDouble("dec_deg", Float.NaN);
                    if (Float.isNaN(raHours) || Float.isNaN(decDegrees)) {
                        continue;
                    }
                    minRa = Math.min(minRa, raHours);
                    maxRa = Math.max(maxRa, raHours);
                    boundaryPoints.add(new BoundaryPoint(raHours, decDegrees));
                }

                if (boundaryPoints.isEmpty()) {
                    continue;
                }

                boolean crossesZero = (maxRa - minRa) > RA_WRAP_THRESHOLD_HOURS;
                boundaries.add(new ConstellationBoundary(abbr, id, boundaryPoints, crossesZero));
            }

            if (boundaries.isEmpty()) {
                Log.w(TAG, "No valid constellation boundaries in " + BOUNDARIES_ASSET);
                return null;
            }

            Log.d(TAG, "Loaded " + boundaries.size() + " constellation boundaries");
            return boundaries;
        } catch (Exception e) {
            Log.w(TAG, "Failed to load constellation boundaries from " + BOUNDARIES_ASSET, e);
            return null;
        }
    }

    @Nullable
    private Map<String, String> getConstellationIdByAbbr() {
        if (constellationIdByAbbr != null) {
            return constellationIdByAbbr;
        }
        constellationIdByAbbr = loadConstellationIdByAbbr();
        return constellationIdByAbbr;
    }

    @Nullable
    private Map<String, String> loadConstellationIdByAbbr() {
        try (java.io.InputStream inputStream = assetDataSource.openAsset(CONSTELLATION_NAMES_ASSET)) {
            String json = readStreamToString(inputStream);
            JSONObject root = new JSONObject(json);
            Map<String, String> map = new HashMap<>();
            JSONArray names = root.names();
            if (names == null) {
                return null;
            }
            for (int i = 0; i < names.length(); i++) {
                String abbr = names.optString(i, "").trim();
                if (abbr.isEmpty()) continue;
                String fullName = root.optString(abbr, "").trim();
                if (fullName.isEmpty()) continue;
                String id = toSnakeCaseId(fullName);
                if (!id.isEmpty()) {
                    map.put(abbr.toUpperCase(Locale.ROOT), id);
                }
            }
            return map;
        } catch (Exception e) {
            Log.w(TAG, "Failed to load constellation names from " + CONSTELLATION_NAMES_ASSET, e);
            return null;
        }
    }

    @NonNull
    private String toSnakeCaseId(@NonNull String name) {
        String normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String cleaned = normalized.replaceAll("[^a-zA-Z0-9]+", "_")
                .toLowerCase(Locale.ROOT)
                .replaceAll("^_+|_+$", "");
        return cleaned;
    }

    private static final class ConstellationBoundary {
        private final String abbr;
        private final String id;
        private final List<BoundaryPoint> points;
        private final boolean crossesZero;

        private ConstellationBoundary(String abbr, String id,
                                      List<BoundaryPoint> points,
                                      boolean crossesZero) {
            this.abbr = abbr;
            this.id = id;
            this.points = points;
            this.crossesZero = crossesZero;
        }
    }

    private static final class BoundaryPoint {
        private final float raHours;
        private final float decDegrees;

        private BoundaryPoint(float raHours, float decDegrees) {
            this.raHours = raHours;
            this.decDegrees = decDegrees;
        }
    }

    private static class NamedStar {
        private final String displayName;
        private final float ra;
        private final float dec;
        private final List<String> alternateNames;

        private NamedStar(@NonNull String displayName,
                          float ra,
                          float dec,
                          @NonNull List<String> alternateNames) {
            this.displayName = displayName;
            this.ra = ra;
            this.dec = dec;
            this.alternateNames = alternateNames;
        }
    }
}


