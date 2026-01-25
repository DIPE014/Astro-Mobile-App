package com.astro.app.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.ConstellationData;
import com.astro.app.data.model.GeocentricCoords;
import com.astro.app.data.parser.ProtobufParser;
import com.astro.app.data.proto.SourceProto.AstronomicalSourceProto;
import com.astro.app.data.proto.SourceProto.GeocentricCoordinatesProto;
import com.astro.app.data.proto.SourceProto.LabelElementProto;
import com.astro.app.data.proto.SourceProto.LineElementProto;
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
 * Implementation of {@link ConstellationRepository} that loads constellation data
 * from binary protobuf files.
 *
 * <p>This implementation uses {@link ProtobufParser} to load constellation data from
 * the constellations.binary asset file. Constellations are converted from
 * {@link AstronomicalSourceProto} to {@link ConstellationData} objects and cached
 * in memory for efficient access.</p>
 *
 * <h3>Data Conversion:</h3>
 * <p>Each {@link AstronomicalSourceProto} contains:
 * <ul>
 *   <li>Points (stars) that make up the constellation</li>
 *   <li>Lines connecting the stars to form the constellation pattern</li>
 *   <li>Labels for the constellation name</li>
 *   <li>Search location for centering on the constellation</li>
 * </ul>
 * </p>
 *
 * <h3>Caching Strategy:</h3>
 * <p>Constellations are loaded lazily on first access and cached for the lifetime
 * of the application.</p>
 *
 * @see ConstellationRepository
 * @see ProtobufParser
 */
@Singleton
public class ConstellationRepositoryImpl implements ConstellationRepository {

    private static final String TAG = "ConstellationRepoImpl";

    /** Counter for generating unique constellation IDs when not available */
    private static int constellationIdCounter = 0;

    private final ProtobufParser protobufParser;

    /** Cached list of all constellations */
    @Nullable
    private List<ConstellationData> cachedConstellations;

    /** Map from constellation ID to ConstellationData for fast lookup */
    @Nullable
    private Map<String, ConstellationData> constellationIdMap;

    /** Map from lowercase constellation name to ConstellationData for fast lookup */
    @Nullable
    private Map<String, ConstellationData> constellationNameMap;

    /**
     * Creates a ConstellationRepositoryImpl with the provided parser.
     *
     * @param protobufParser The parser for reading constellation data from binary files
     */
    @Inject
    public ConstellationRepositoryImpl(@NonNull ProtobufParser protobufParser) {
        this.protobufParser = protobufParser;
    }

    /**
     * Provide the list of all known constellations, loading and caching data on first access.
     *
     * @return the unmodifiable list of ConstellationData sorted by name, or an empty list if no constellations are available
     */
    @Override
    @NonNull
    public synchronized List<ConstellationData> getAllConstellations() {
        ensureCacheLoaded();
        return cachedConstellations != null ? cachedConstellations : Collections.emptyList();
    }

    /**
     * Retrieve a constellation by its identifier.
     *
     * @param id the constellation identifier to look up
     * @return the matching {@code ConstellationData} if found, {@code null} otherwise
     */
    @Override
    @Nullable
    public ConstellationData getConstellationById(@NonNull String id) {
        ensureCacheLoaded();
        return constellationIdMap != null ? constellationIdMap.get(id) : null;
    }

    /**
     * Look up a constellation by name using a case-insensitive match.
     *
     * @param name the constellation name to look up; comparison is case-insensitive
     * @return the matching {@code ConstellationData}, or {@code null} if no constellation with that name is found
     */
    @Override
    @Nullable
    public ConstellationData getConstellationByName(@NonNull String name) {
        ensureCacheLoaded();
        return constellationNameMap != null ? constellationNameMap.get(name.toLowerCase(Locale.ROOT)) : null;
    }

    /**
     * Searches cached constellations for entries whose name or id contains the given query, case-insensitive.
     *
     * @param query substring to match against a constellation's name or id (case-insensitive)
     * @return a list of ConstellationData whose name or id contains the query, preserving the order from getAllConstellations(); empty list if no matches
     */
    @Override
    @NonNull
    public List<ConstellationData> searchConstellations(@NonNull String query) {
        List<ConstellationData> allConstellations = getAllConstellations();
        List<ConstellationData> results = new ArrayList<>();

        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (ConstellationData constellation : allConstellations) {
            // Check name
            if (constellation.getName().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                results.add(constellation);
                continue;
            }

            // Check ID (abbreviation)
            if (constellation.getId().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                results.add(constellation);
            }
        }

        return results;
    }

    /**
     * Load and cache all constellation data from the protobuf asset if the cache is not already initialized.
     *
     * <p>Parses all AstronomicalSourceProto entries, converts each to ConstellationData, and populates
     * the in-memory caches: a sorted, unmodifiable list of constellations, a map from constellation ID
     * to ConstellationData, and a map from lowercased constellation name to ConstellationData.</p>
     */
    private synchronized void ensureCacheLoaded() {
        if (cachedConstellations != null) {
            return;
        }

        Log.d(TAG, "Loading constellations from binary file...");

        List<AstronomicalSourceProto> protos = protobufParser.parseConstellations();
        List<ConstellationData> constellations = new ArrayList<>(protos.size());
        Map<String, ConstellationData> idMap = new HashMap<>();
        Map<String, ConstellationData> nameMap = new HashMap<>();

        for (AstronomicalSourceProto proto : protos) {
            ConstellationData constellation = convertProtoToConstellationData(proto);
            if (constellation != null) {
                constellations.add(constellation);
                idMap.put(constellation.getId(), constellation);
                nameMap.put(constellation.getName().toLowerCase(Locale.ROOT), constellation);
            }
        }

        // Sort constellations alphabetically by name
        Collections.sort(constellations, (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

        cachedConstellations = Collections.unmodifiableList(constellations);
        constellationIdMap = idMap;
        constellationNameMap = nameMap;

        Log.d(TAG, "Loaded " + constellations.size() + " constellations");
    }

    /**
         * Builds a constellation model from the provided protobuf source.
         *
         * <p>Extracts a display name, a stable identifier, star identifiers, connection indices, and
         * an optional center point from the protobuf. Returns null when a required value (such as the
         * name) is missing or when an error occurs during conversion.</p>
         *
         * @param proto the protobuf astronomical source to convert
         * @return the resulting ConstellationData, or {@code null} if conversion fails or required data is missing
         */
    @Nullable
    private ConstellationData convertProtoToConstellationData(@NonNull AstronomicalSourceProto proto) {
        try {
            // Extract name from labels or string IDs
            String name = extractConstellationName(proto);
            if (name == null || name.isEmpty()) {
                Log.w(TAG, "Skipping constellation without name");
                return null;
            }

            // Generate ID from name or use string ID if available
            String id = extractConstellationId(proto, name);

            // Extract star positions as IDs for this constellation
            List<String> starIds = extractStarIds(proto);

            // Extract line connections
            List<int[]> lineIndices = extractLineIndices(proto, starIds.size());

            // Extract center point from search location
            GeocentricCoords centerPoint = extractCenterPoint(proto);

            ConstellationData.Builder builder = ConstellationData.builder()
                    .setId(id)
                    .setName(name)
                    .setStarIds(starIds)
                    .setLineIndices(lineIndices);

            if (centerPoint != null) {
                builder.setCenterPoint(centerPoint);
            }

            return builder.build();

        } catch (Exception e) {
            Log.e(TAG, "Error converting proto to ConstellationData", e);
            return null;
        }
    }

    /**
         * Determines the constellation display name from the given proto.
         *
         * <p>Checks sources in order: label string IDs, the proto's name string IDs, then a
         * fallback formatted from the search location coordinates.</p>
         *
         * @param proto the astronomical source proto to extract the name from
         * @return the formatted constellation name, or {@code null} if no name could be determined
         */
    @Nullable
    private String extractConstellationName(@NonNull AstronomicalSourceProto proto) {
        // Try to get name from labels first
        for (LabelElementProto label : proto.getLabelList()) {
            if (label.hasStringsStrId()) {
                String strId = label.getStringsStrId();
                // Convert snake_case to Title Case
                return formatConstellationName(strId);
            }
        }

        // Try string IDs from the source itself
        if (proto.getNameStrIdsCount() > 0) {
            return formatConstellationName(proto.getNameStrIds(0));
        }

        // Generate a name based on position if nothing else available
        if (proto.hasSearchLocation()) {
            GeocentricCoordinatesProto loc = proto.getSearchLocation();
            return String.format(Locale.US, "Constellation %.0f,%.0f",
                    loc.getRightAscension(), loc.getDeclination());
        }

        return null;
    }

    /**
         * Produce a stable, unique identifier for a constellation.
         *
         * <p>Prefer a canonical ID embedded in the proto when present; otherwise generate an ID
         * from the provided name and append a unique numeric suffix.</p>
         *
         * @param proto the astronomical source proto which may contain a canonical name string ID
         * @param name  fallback constellation name used to generate an ID when the proto has none
         * @return a lowercase identifier derived from the proto's name string ID when available;
         *         otherwise a generated identifier based on the provided name with an appended
         *         underscore and unique numeric suffix
         */
    @NonNull
    private String extractConstellationId(@NonNull AstronomicalSourceProto proto, @NonNull String name) {
        // Try to use string ID if available
        if (proto.getNameStrIdsCount() > 0) {
            String strId = proto.getNameStrIds(0);
            // Clean up the ID (remove special characters, convert to lowercase)
            return strId.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase(Locale.ROOT);
        }

        // Generate from name
        String generated = name.replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase(Locale.ROOT);

        if (generated.length() > 3) {
            // Use first 3 characters as abbreviation
            generated = generated.substring(0, 3);
        }

        // Ensure uniqueness
        return generated + "_" + (++constellationIdCounter);
    }

    /**
     * Generates star identifiers for points that include a location.
     *
     * @param proto the astronomical source proto containing point elements
     * @return a list of star IDs derived from each point's coordinates (e.g. "cstar_<ra*1000>_<dec*1000>")
     */
    @NonNull
    private List<String> extractStarIds(@NonNull AstronomicalSourceProto proto) {
        List<String> starIds = new ArrayList<>();

        for (int i = 0; i < proto.getPointCount(); i++) {
            PointElementProto point = proto.getPoint(i);
            if (point.hasLocation()) {
                GeocentricCoordinatesProto loc = point.getLocation();
                // Generate ID from coordinates for matching with star repository
                String starId = String.format(Locale.US, "cstar_%d_%d",
                        Math.round(loc.getRightAscension() * 1000),
                        Math.round(loc.getDeclination() * 1000));
                starIds.add(starId);
            }
        }

        return starIds;
    }

    /**
         * Builds a list of star-index pairs that represent line segments for the constellation.
         *
         * <p>Each element is an int[2] where the first entry is the start star index and the second is the end star index;
         * pairs are included only when both vertices map to distinct stars in the constellation.</p>
         *
         * @param proto the astronomical source proto containing line vertex coordinates
         * @return a list of index pairs ([startIndex, endIndex]) referencing stars that form each line segment
         */
    @NonNull
    private List<int[]> extractLineIndices(@NonNull AstronomicalSourceProto proto, int starCount) {
        List<int[]> lineIndices = new ArrayList<>();

        // Build a list of star coordinates from points
        List<float[]> starCoords = new ArrayList<>();
        for (PointElementProto point : proto.getPointList()) {
            if (point.hasLocation()) {
                GeocentricCoordinatesProto loc = point.getLocation();
                starCoords.add(new float[]{loc.getRightAscension(), loc.getDeclination()});
            }
        }

        // For each line, find the star indices that match the vertices
        for (LineElementProto line : proto.getLineList()) {
            List<GeocentricCoordinatesProto> vertices = line.getVertexList();

            for (int i = 0; i < vertices.size() - 1; i++) {
                GeocentricCoordinatesProto start = vertices.get(i);
                GeocentricCoordinatesProto end = vertices.get(i + 1);

                int startIndex = findClosestStarIndex(starCoords, start);
                int endIndex = findClosestStarIndex(starCoords, end);

                if (startIndex >= 0 && endIndex >= 0 && startIndex != endIndex) {
                    lineIndices.add(new int[]{startIndex, endIndex});
                }
            }
        }

        return lineIndices;
    }

    /**
         * Finds the index of the star whose coordinates are nearest to the given target coordinates.
         *
         * @param starCoords list of float[2] arrays where each entry is [rightAscension, declination] in degrees
         * @param target     target coordinates to match (right ascension and declination in degrees)
         * @return the index of the nearest star in {@code starCoords} if its angular distance to {@code target} is less than 1.0 degree, or -1 otherwise
         */
    private int findClosestStarIndex(@NonNull List<float[]> starCoords,
                                     @NonNull GeocentricCoordinatesProto target) {
        float targetRa = target.getRightAscension();
        float targetDec = target.getDeclination();

        int closestIndex = -1;
        float closestDist = Float.MAX_VALUE;

        for (int i = 0; i < starCoords.size(); i++) {
            float[] coords = starCoords.get(i);
            float dist = angularDistance(coords[0], coords[1], targetRa, targetDec);

            if (dist < closestDist) {
                closestDist = dist;
                closestIndex = i;
            }
        }

        // Only return if within a reasonable threshold (1 degree)
        return closestDist < 1.0f ? closestIndex : -1;
    }

    /**
     * Calculates the angular distance between two celestial coordinates.
     *
     * @param ra1  Right Ascension of first point
     * @param dec1 Declination of first point
     * @param ra2  Right Ascension of second point
     * @param dec2 Declination of second point
     * @return Angular distance in degrees
     */
    private float angularDistance(float ra1, float dec1, float ra2, float dec2) {
        double dRa = Math.toRadians(ra2 - ra1);
        double dDec = Math.toRadians(dec2 - dec1);
        double dec1Rad = Math.toRadians(dec1);
        double dec2Rad = Math.toRadians(dec2);

        double a = Math.sin(dDec / 2) * Math.sin(dDec / 2) +
                Math.cos(dec1Rad) * Math.cos(dec2Rad) *
                        Math.sin(dRa / 2) * Math.sin(dRa / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (float) Math.toDegrees(c);
    }

    /**
     * Obtain the center coordinates from the proto's search location.
     *
     * @param proto the astronomical source proto to read the search location from
     * @return the center geocentric coordinates, or null if the proto has no search location
     */
    @Nullable
    private GeocentricCoords extractCenterPoint(@NonNull AstronomicalSourceProto proto) {
        if (proto.hasSearchLocation()) {
            GeocentricCoordinatesProto loc = proto.getSearchLocation();
            return GeocentricCoords.fromDegrees(
                    loc.getRightAscension(),
                    loc.getDeclination()
            );
        }
        return null;
    }

    /**
     * Formats a snake_case string ID to Title Case name.
     *
     * @param snakeCaseId The snake_case ID (e.g., "ursa_major")
     * @return The formatted name (e.g., "Ursa Major")
     */
    @NonNull
    private String formatConstellationName(@NonNull String snakeCaseId) {
        String[] parts = snakeCaseId.split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (builder.length() > 0) {
                builder.append(" ");
            }

            // Capitalize first letter
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }

        return builder.toString();
    }
}