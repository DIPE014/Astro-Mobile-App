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

    @Override
    @NonNull
    public synchronized List<ConstellationData> getAllConstellations() {
        ensureCacheLoaded();
        return cachedConstellations != null ? cachedConstellations : Collections.emptyList();
    }

    @Override
    @Nullable
    public ConstellationData getConstellationById(@NonNull String id) {
        ensureCacheLoaded();
        return constellationIdMap != null ? constellationIdMap.get(id) : null;
    }

    @Override
    @Nullable
    public ConstellationData getConstellationByName(@NonNull String name) {
        ensureCacheLoaded();
        return constellationNameMap != null ? constellationNameMap.get(name.toLowerCase(Locale.ROOT)) : null;
    }

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
     * Ensures that the constellation cache is loaded.
     * This method is synchronized to prevent multiple threads from loading simultaneously.
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
     * Converts an {@link AstronomicalSourceProto} to a {@link ConstellationData} object.
     *
     * @param proto The protobuf astronomical source
     * @return A ConstellationData object, or null if conversion fails
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

            // Extract star coordinates for direct rendering
            List<GeocentricCoords> starCoordinates = extractStarCoordinates(proto);

            // Extract line connections (index-based approach)
            List<int[]> lineIndices = extractLineIndices(proto, starIds.size());

            // Extract line segments directly from protobuf (coordinate-based approach)
            // This is more reliable as it doesn't depend on matching vertices to points
            List<float[]> lineSegments = extractLineSegments(proto);

            // Debug logging for constellation data extraction
            Log.d(TAG, "Constellation '" + name + "': points=" + proto.getPointCount() +
                    ", lines=" + proto.getLineCount() + ", starIds=" + starIds.size() +
                    ", starCoords=" + starCoordinates.size() +
                    ", lineIndices=" + lineIndices.size() +
                    ", lineSegments=" + lineSegments.size());

            // Extract center point from search location
            GeocentricCoords centerPoint = extractCenterPoint(proto);

            ConstellationData.Builder builder = ConstellationData.builder()
                    .setId(id)
                    .setName(name)
                    .setStarIds(starIds)
                    .setStarCoordinates(starCoordinates)
                    .setLineIndices(lineIndices)
                    .setLineSegments(lineSegments);

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
     * Extracts the constellation name from the protobuf data.
     *
     * <p>Tries multiple sources: labels, string IDs, and integer IDs.</p>
     *
     * @param proto The astronomical source proto
     * @return The constellation name, or null if not found
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
     * Extracts or generates a constellation ID.
     *
     * @param proto The astronomical source proto
     * @param name  The constellation name (fallback for ID generation)
     * @return A unique constellation ID
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
     * Extracts star IDs from the constellation's point elements.
     *
     * <p>Each point in the constellation represents a star position.
     * We generate unique IDs based on their coordinates.</p>
     *
     * @param proto The astronomical source proto
     * @return A list of generated star IDs
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
     * Extracts star coordinates from the constellation's point elements.
     *
     * <p>Each point in the constellation represents a star position.
     * The coordinates are stored for direct use in rendering.</p>
     *
     * @param proto The astronomical source proto
     * @return A list of GeocentricCoords for each star
     */
    @NonNull
    private List<GeocentricCoords> extractStarCoordinates(@NonNull AstronomicalSourceProto proto) {
        List<GeocentricCoords> coords = new ArrayList<>();

        for (int i = 0; i < proto.getPointCount(); i++) {
            PointElementProto point = proto.getPoint(i);
            if (point.hasLocation()) {
                GeocentricCoordinatesProto loc = point.getLocation();
                coords.add(GeocentricCoords.fromDegrees(
                        loc.getRightAscension(),
                        loc.getDeclination()
                ));
            }
        }

        return coords;
    }

    /**
     * Extracts line segments directly from the protobuf line elements.
     *
     * <p>Each line segment is stored as [startRa, startDec, endRa, endDec].
     * This approach uses the raw line vertices without trying to match to points.</p>
     *
     * @param proto The astronomical source proto
     * @return A list of line segments with coordinates
     */
    @NonNull
    private List<float[]> extractLineSegments(@NonNull AstronomicalSourceProto proto) {
        List<float[]> segments = new ArrayList<>();

        for (LineElementProto line : proto.getLineList()) {
            List<GeocentricCoordinatesProto> vertices = line.getVertexList();

            // Each pair of consecutive vertices forms a line segment
            for (int i = 0; i < vertices.size() - 1; i++) {
                GeocentricCoordinatesProto start = vertices.get(i);
                GeocentricCoordinatesProto end = vertices.get(i + 1);

                segments.add(new float[] {
                        start.getRightAscension(),
                        start.getDeclination(),
                        end.getRightAscension(),
                        end.getDeclination()
                });
            }
        }

        return segments;
    }

    /**
     * Extracts line connection indices from the constellation's line elements.
     *
     * <p>Lines in the protobuf contain vertex coordinates. We need to map these
     * to indices in our star list by finding the closest matching stars.</p>
     *
     * <p>If no points are available, we create a sequential list of all line vertices
     * and use sequential indices.</p>
     *
     * @param proto    The astronomical source proto
     * @param starCount The number of stars in the constellation
     * @return A list of index pairs representing line connections
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

        // If no points exist, use line vertices directly as star positions
        if (starCoords.isEmpty()) {
            Log.d(TAG, "No points in constellation, using line vertices directly");
            // This case is handled by building coordinates from lines in extractStarCoordinatesFromLines
            return lineIndices;
        }

        // For each line, find the star indices that match the vertices
        int matchedLines = 0;
        int unmatchedLines = 0;
        for (LineElementProto line : proto.getLineList()) {
            List<GeocentricCoordinatesProto> vertices = line.getVertexList();

            for (int i = 0; i < vertices.size() - 1; i++) {
                GeocentricCoordinatesProto start = vertices.get(i);
                GeocentricCoordinatesProto end = vertices.get(i + 1);

                int startIndex = findClosestStarIndex(starCoords, start);
                int endIndex = findClosestStarIndex(starCoords, end);

                if (startIndex >= 0 && endIndex >= 0 && startIndex != endIndex) {
                    lineIndices.add(new int[]{startIndex, endIndex});
                    matchedLines++;
                } else {
                    unmatchedLines++;
                }
            }
        }

        if (unmatchedLines > 0) {
            Log.d(TAG, "Line matching: " + matchedLines + " matched, " + unmatchedLines + " unmatched");
        }

        return lineIndices;
    }

    /**
     * Finds the index of the closest star to the given coordinates.
     *
     * @param starCoords List of star coordinates [ra, dec]
     * @param target     The target coordinates to match
     * @return The index of the closest star, or -1 if no match found
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

        // Use a generous threshold of 10 degrees since line vertices may not exactly match point coordinates
        // The protobuf line vertices should be close to the constellation stars
        return closestDist < 10.0f ? closestIndex : -1;
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
     * Extracts the center point from the search location.
     *
     * @param proto The astronomical source proto
     * @return The center coordinates, or null if not available
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
