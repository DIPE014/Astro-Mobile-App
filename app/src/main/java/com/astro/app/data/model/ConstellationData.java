package com.astro.app.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a constellation with its stars and connection lines.
 *
 * <p>A constellation consists of:
 * <ul>
 *   <li>A set of stars identified by their IDs</li>
 *   <li>Line connections between stars that form the constellation pattern</li>
 *   <li>A center point for labeling and identification</li>
 * </ul>
 * </p>
 *
 * <h3>Line Indices:</h3>
 * <p>The {@code lineIndices} list contains pairs of indices that reference
 * positions in the {@code starIds} list. For example, if starIds is
 * ["star1", "star2", "star3"] and lineIndices contains [0, 1], [1, 2],
 * then lines will be drawn from star1 to star2, and from star2 to star3.</p>
 *
 * <h3>Standard Constellation Abbreviations:</h3>
 * <p>Constellation IDs typically use the standard 3-letter IAU abbreviations:
 * <ul>
 *   <li>Ori - Orion</li>
 *   <li>UMa - Ursa Major (Big Dipper)</li>
 *   <li>CMa - Canis Major</li>
 *   <li>etc.</li>
 * </ul>
 * </p>
 *
 * @see StarData
 * @see GeocentricCoords
 */
public class ConstellationData {

    /** Unique identifier (typically 3-letter IAU abbreviation) */
    @NonNull
    private final String id;

    /** Display name of the constellation */
    @NonNull
    private final String name;

    /** List of star IDs that make up this constellation */
    @NonNull
    private final List<String> starIds;

    /**
     * Pairs of indices into starIds defining the constellation lines.
     * Each int[] should have exactly 2 elements: [startIndex, endIndex]
     */
    @NonNull
    private final List<int[]> lineIndices;

    /**
     * List of star coordinates matching starIds order.
     * Each entry is the RA/Dec position of the corresponding star.
     */
    @NonNull
    private final List<GeocentricCoords> starCoordinates;

    /**
     * Direct line segments from the protobuf data.
     * Each entry is a pair of coordinates: [startRa, startDec, endRa, endDec].
     * This is used when line vertices don't match point coordinates.
     */
    @NonNull
    private final List<float[]> lineSegments;

    /** Center point of the constellation for labeling */
    @Nullable
    private final GeocentricCoords centerPoint;

    /**
     * Private constructor. Use {@link Builder} to create instances.
     *
     * @param builder The builder containing constellation properties
     */
    private ConstellationData(@NonNull Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.starIds = Collections.unmodifiableList(new ArrayList<>(builder.starIds));
        // Deep copy lineIndices to ensure immutability
        List<int[]> copiedLines = new ArrayList<>();
        for (int[] line : builder.lineIndices) {
            copiedLines.add(new int[] {line[0], line[1]});
        }
        this.lineIndices = Collections.unmodifiableList(copiedLines);
        this.starCoordinates = Collections.unmodifiableList(new ArrayList<>(builder.starCoordinates));
        // Deep copy lineSegments
        List<float[]> copiedSegments = new ArrayList<>();
        for (float[] segment : builder.lineSegments) {
            copiedSegments.add(new float[] {segment[0], segment[1], segment[2], segment[3]});
        }
        this.lineSegments = Collections.unmodifiableList(copiedSegments);
        this.centerPoint = builder.centerPoint;
    }

    /**
     * Returns the unique identifier for this constellation.
     *
     * <p>Typically the 3-letter IAU abbreviation (e.g., "Ori" for Orion).</p>
     *
     * @return The constellation ID
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Returns the display name of this constellation.
     *
     * @return The constellation name (e.g., "Orion")
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the list of star IDs in this constellation.
     *
     * @return An unmodifiable list of star IDs
     */
    @NonNull
    public List<String> getStarIds() {
        return starIds;
    }

    /**
     * Returns the number of stars in this constellation.
     *
     * @return The star count
     */
    public int getStarCount() {
        return starIds.size();
    }

    /**
     * Returns the line indices for drawing constellation lines.
     *
     * <p>Each int[] contains exactly 2 elements: the start and end indices
     * into the {@link #getStarIds()} list.</p>
     *
     * @return An unmodifiable list of index pairs
     */
    @NonNull
    public List<int[]> getLineIndices() {
        return lineIndices;
    }

    /**
     * Returns the number of lines in this constellation pattern.
     *
     * @return The line count
     */
    public int getLineCount() {
        return lineIndices.size();
    }

    /**
     * Returns the list of star coordinates in this constellation.
     *
     * <p>Coordinates are in the same order as {@link #getStarIds()}.</p>
     *
     * @return An unmodifiable list of star coordinates
     */
    @NonNull
    public List<GeocentricCoords> getStarCoordinates() {
        return starCoordinates;
    }

    /**
     * Returns the coordinates of a star at the specified index.
     *
     * @param index The index in the star list
     * @return The star coordinates, or null if index is out of bounds or coordinates not available
     */
    @Nullable
    public GeocentricCoords getStarCoordinatesAt(int index) {
        if (index >= 0 && index < starCoordinates.size()) {
            return starCoordinates.get(index);
        }
        return null;
    }

    /**
     * Checks if this constellation has embedded star coordinates.
     *
     * @return true if star coordinates are available
     */
    public boolean hasStarCoordinates() {
        return !starCoordinates.isEmpty();
    }

    /**
     * Returns the direct line segments for this constellation.
     *
     * <p>Each float[] contains [startRa, startDec, endRa, endDec] in degrees.</p>
     *
     * @return An unmodifiable list of line segments
     */
    @NonNull
    public List<float[]> getLineSegments() {
        return lineSegments;
    }

    /**
     * Checks if this constellation has direct line segments.
     *
     * @return true if line segments are available
     */
    public boolean hasLineSegments() {
        return !lineSegments.isEmpty();
    }

    /**
     * Returns the center point of the constellation.
     *
     * <p>This is used for placing the constellation label and for
     * determining if the constellation is in view.</p>
     *
     * @return The center coordinates, or null if not set
     */
    @Nullable
    public GeocentricCoords getCenterPoint() {
        return centerPoint;
    }

    /**
     * Checks if the center point is defined.
     *
     * @return true if center point is available
     */
    public boolean hasCenterPoint() {
        return centerPoint != null;
    }

    /**
     * Returns the center Right Ascension in degrees.
     *
     * @return Center RA in degrees, or 0 if center point is not set
     */
    public float getCenterRa() {
        return centerPoint != null ? centerPoint.getRa() : 0f;
    }

    /**
     * Returns the center Declination in degrees.
     *
     * @return Center Dec in degrees, or 0 if center point is not set
     */
    public float getCenterDec() {
        return centerPoint != null ? centerPoint.getDec() : 0f;
    }

    /**
     * Checks if a star ID is part of this constellation.
     *
     * @param starId The star ID to check
     * @return true if the star is part of this constellation
     */
    public boolean containsStar(@NonNull String starId) {
        return starIds.contains(starId);
    }

    /**
     * Gets the index of a star in the constellation's star list.
     *
     * @param starId The star ID to find
     * @return The index in starIds, or -1 if not found
     */
    public int getStarIndex(@NonNull String starId) {
        return starIds.indexOf(starId);
    }

    /**
     * Returns the star ID at the specified index.
     *
     * @param index The index in the star list
     * @return The star ID
     * @throws IndexOutOfBoundsException if index is out of range
     */
    @NonNull
    public String getStarIdAt(int index) {
        return starIds.get(index);
    }

    /**
     * Gets all unique star pairs connected by lines.
     *
     * <p>Returns a list where each String[] contains [startStarId, endStarId].</p>
     *
     * @return List of star ID pairs for each line
     */
    @NonNull
    public List<String[]> getLinePairs() {
        List<String[]> pairs = new ArrayList<>();
        for (int[] indices : lineIndices) {
            if (indices.length >= 2 &&
                indices[0] >= 0 && indices[0] < starIds.size() &&
                indices[1] >= 0 && indices[1] < starIds.size()) {
                pairs.add(new String[] {starIds.get(indices[0]), starIds.get(indices[1])});
            }
        }
        return pairs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstellationData that = (ConstellationData) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("ConstellationData{id='%s', name='%s', stars=%d, lines=%d}",
                id, name, starIds.size(), lineIndices.size());
    }

    /**
     * Creates a new Builder instance.
     *
     * @return A new Builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating ConstellationData instances.
     *
     * <p>Example usage:
     * <pre>{@code
     * ConstellationData orion = ConstellationData.builder()
     *     .setId("Ori")
     *     .setName("Orion")
     *     .setStarIds(Arrays.asList("betelgeuse", "rigel", "bellatrix", "mintaka"))
     *     .addLine(0, 1)  // Betelgeuse to Rigel
     *     .addLine(0, 2)  // Betelgeuse to Bellatrix
     *     .addLine(2, 3)  // Bellatrix to Mintaka
     *     .setCenterPoint(GeocentricCoords.fromDegrees(85.0f, -1.2f))
     *     .build();
     * }</pre>
     * </p>
     */
    public static class Builder {

        @NonNull
        private String id = "";

        @NonNull
        private String name = "";

        @NonNull
        private List<String> starIds = new ArrayList<>();

        @NonNull
        private List<int[]> lineIndices = new ArrayList<>();

        @NonNull
        private List<GeocentricCoords> starCoordinates = new ArrayList<>();

        @NonNull
        private List<float[]> lineSegments = new ArrayList<>();

        @Nullable
        private GeocentricCoords centerPoint = null;

        /**
         * Creates a new Builder instance.
         */
        public Builder() {
        }

        /**
         * Sets the constellation ID.
         *
         * @param id The unique identifier (typically 3-letter IAU abbreviation)
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setId(@NonNull String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the constellation name.
         *
         * @param name The display name
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setName(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the list of star IDs in this constellation.
         *
         * @param starIds The list of star IDs
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setStarIds(@Nullable List<String> starIds) {
            this.starIds = starIds != null ? new ArrayList<>(starIds) : new ArrayList<>();
            return this;
        }

        /**
         * Adds a star ID to the constellation.
         *
         * @param starId The star ID to add
         * @return This builder for method chaining
         */
        @NonNull
        public Builder addStarId(@NonNull String starId) {
            this.starIds.add(starId);
            return this;
        }

        /**
         * Sets the line indices that define constellation lines.
         *
         * @param lineIndices List of index pairs
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setLineIndices(@Nullable List<int[]> lineIndices) {
            this.lineIndices = new ArrayList<>();
            if (lineIndices != null) {
                for (int[] line : lineIndices) {
                    if (line != null && line.length >= 2) {
                        this.lineIndices.add(new int[] {line[0], line[1]});
                    }
                }
            }
            return this;
        }

        /**
         * Adds a line connecting two stars by their indices.
         *
         * @param startIndex Index of the start star in starIds
         * @param endIndex   Index of the end star in starIds
         * @return This builder for method chaining
         */
        @NonNull
        public Builder addLine(int startIndex, int endIndex) {
            this.lineIndices.add(new int[] {startIndex, endIndex});
            return this;
        }

        /**
         * Sets the star coordinates list.
         *
         * @param coordinates List of star coordinates matching starIds order
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setStarCoordinates(@Nullable List<GeocentricCoords> coordinates) {
            this.starCoordinates = coordinates != null ? new ArrayList<>(coordinates) : new ArrayList<>();
            return this;
        }

        /**
         * Adds a star coordinate to the constellation.
         *
         * @param ra  Right Ascension in degrees
         * @param dec Declination in degrees
         * @return This builder for method chaining
         */
        @NonNull
        public Builder addStarCoordinate(float ra, float dec) {
            this.starCoordinates.add(GeocentricCoords.fromDegrees(ra, dec));
            return this;
        }

        /**
         * Sets the line segments list.
         *
         * @param segments List of line segments [startRa, startDec, endRa, endDec]
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setLineSegments(@Nullable List<float[]> segments) {
            this.lineSegments = new ArrayList<>();
            if (segments != null) {
                for (float[] segment : segments) {
                    if (segment != null && segment.length >= 4) {
                        this.lineSegments.add(new float[] {segment[0], segment[1], segment[2], segment[3]});
                    }
                }
            }
            return this;
        }

        /**
         * Adds a line segment to the constellation.
         *
         * @param startRa  Start Right Ascension in degrees
         * @param startDec Start Declination in degrees
         * @param endRa    End Right Ascension in degrees
         * @param endDec   End Declination in degrees
         * @return This builder for method chaining
         */
        @NonNull
        public Builder addLineSegment(float startRa, float startDec, float endRa, float endDec) {
            this.lineSegments.add(new float[] {startRa, startDec, endRa, endDec});
            return this;
        }

        /**
         * Sets the center point of the constellation.
         *
         * @param centerPoint The center coordinates
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setCenterPoint(@Nullable GeocentricCoords centerPoint) {
            this.centerPoint = centerPoint;
            return this;
        }

        /**
         * Sets the center point from RA/Dec values.
         *
         * @param ra  Right Ascension in degrees
         * @param dec Declination in degrees
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setCenterPoint(float ra, float dec) {
            this.centerPoint = GeocentricCoords.fromDegrees(ra, dec);
            return this;
        }

        /**
         * Calculates the center point as the average of all star positions.
         *
         * <p>Note: This requires that the stars are already resolved. This
         * method should be called after building when star coordinates are
         * available.</p>
         *
         * @param starCoords List of star coordinates matching starIds order
         * @return This builder for method chaining
         */
        @NonNull
        public Builder calculateCenterFromStars(@NonNull List<GeocentricCoords> starCoords) {
            if (starCoords.isEmpty()) {
                return this;
            }

            float sumRa = 0;
            float sumDec = 0;
            for (GeocentricCoords coord : starCoords) {
                sumRa += coord.getRa();
                sumDec += coord.getDec();
            }

            float avgRa = sumRa / starCoords.size();
            float avgDec = sumDec / starCoords.size();
            this.centerPoint = GeocentricCoords.fromDegrees(avgRa, avgDec);
            return this;
        }

        /**
         * Builds the ConstellationData instance.
         *
         * @return A new ConstellationData instance
         * @throws IllegalStateException if required fields are not set
         */
        @NonNull
        public ConstellationData build() {
            validate();
            return new ConstellationData(this);
        }

        /**
         * Validates that required fields are properly set.
         *
         * @throws IllegalStateException if validation fails
         */
        private void validate() {
            if (id.isEmpty()) {
                throw new IllegalStateException("ConstellationData requires a non-empty id");
            }
            if (name.isEmpty()) {
                throw new IllegalStateException("ConstellationData requires a non-empty name");
            }
            // Validate line indices are within bounds
            for (int[] line : lineIndices) {
                if (line[0] < 0 || line[0] >= starIds.size() ||
                    line[1] < 0 || line[1] >= starIds.size()) {
                    throw new IllegalStateException(
                            String.format("Line index out of bounds: [%d, %d] for %d stars",
                                    line[0], line[1], starIds.size()));
                }
            }
        }
    }
}
